#!/usr/bin/env python3
"""Summarize a V3 paired run (scripts/paired.sh) into a markdown report.

Ported from feature/poc/racecar:
  - pipeline_benchmark2/analysis/scripts/paired_ab_summary.py: the headline statistic is the per-repetition
    ratio against a reference arm (both arms ran back to back inside repetition k, so machine conditions
    match), reported with racecar's "paired delta" (mean of per-rep ratios - 1), "spread of pairs"
    (stdev of the ratios) and "wins" (sign count). Application CPU leads; process CPU is secondary.
  - pipeline_benchmark3/scripts/bench3_tables.py: per scenario x arm tables (app CPU, p50, throughput) with
    the repetition spread and a steady-state count, plus a same-artifact null control reported as a ratio.

Added here (not in racecar): a 95% confidence interval for each paired ratio, computed as a Student-t
interval on the per-repetition log ratios (geometric-mean ratio exp(mean(log r)), bounds
exp(mean +- t(0.975, n-1) * sd / sqrt(n))). Racecar reported only mean +- stdev of the ratios and a sign
count; neither of its summary scripts computed a confidence interval. The log transform makes the interval
symmetric for A/B and B/A and is the usual choice for ratio metrics. Req 20.23's "Statistically Valid
Result" test (relative half-width <= 5%) and Req 20.24's null band (0.95..1.05) are evaluated on it.

Exclusions (counted, never silent): a row is excluded if valid != true, verified != true, or steady_state
!= true (the last one is waived with --include-non-steady, for smoke runs). Rows from an external harness
(racecar's runner, the optional V2 reference arm) carry no V3 verification column; they are marked
"external", included, and listed separately with their scope.

Usage: summarize.py RUNDIR_OR_RESULTS_CSV [--reference ARM] [--include-non-steady] [--out FILE]
"""
import argparse
import csv
import math
import statistics
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

NULL_SUFFIX = "#null"
# Two-sided 95% Student-t critical values, df 1..30; df > 30 falls back to 1.96 (conservative enough at the
# 5..10 repetitions Req 20.6 allows).
T975 = [12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228, 2.201, 2.179, 2.160, 2.145,
        2.131, 2.120, 2.110, 2.101, 2.093, 2.086, 2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048,
        2.045, 2.042]
SCENARIO_ORDER = ["small-get", "small-put", "batch-get", "batch-put", "describe-table"]
# (csv field, label, lower_is_better). Application CPU leads, as in racecar.
METRICS = [
    ("app_cpu_us_per_op", "application CPU µs/op", True),
    ("alloc_bytes_per_op", "allocated bytes/op", True),
    ("p50_us", "p50 latency µs", True),
    ("p99_us", "p99 latency µs", True),
    ("ops_per_wall_sec", "throughput ops/s", False),
    ("avg_us_per_op", "wall µs/op (1/throughput)", True),
    ("cpu_us_per_op", "process CPU µs/op (incl. JIT/GC/VM)", True),
]
# Req 20.24: every compared CPU, allocation, latency and throughput ratio of the null experiment.
NULL_METRICS = ["app_cpu_us_per_op", "alloc_bytes_per_op", "p50_us", "p99_us", "ops_per_wall_sec"]
EXTERNAL_SCOPE = ("full client (racecar harness): signing, endpoint resolution, retries (3 attempts), "
                  "interceptors, metrics plumbing + serde + transport")


def t975(df):
    return T975[df - 1] if 1 <= df <= len(T975) else 1.96


def fnum(r, field):
    v = r.get(field)
    if v is None or v == "":
        return None
    try:
        return float(v)
    except ValueError:
        return None


def is_true(r, field):
    return str(r.get(field, "")).strip().lower() == "true"


def load(path):
    path = Path(path)
    rundir = path if path.is_dir() else path.parent
    csv_path = path / "results.csv" if path.is_dir() else path
    rows = []
    if csv_path.exists():
        with csv_path.open() as f:
            rows.extend(csv.DictReader(f))
    # External-harness rows (paired.sh --v2-reference-jar): one CSV per run, named ext_<label>_<scenario>_rep<k>.csv,
    # so the label and repetition come from the file name rather than from racecar's own provenance.
    for ext in sorted(rundir.glob("ext_*_rep*.csv")):
        stem = ext.stem[len("ext_"):]
        head, rep = stem.rsplit("_rep", 1)
        label, _scenario = head.rsplit("_", 1)
        with ext.open() as f:
            for r in csv.DictReader(f):
                r["arm"] = r.get("client", "")
                r["client"] = label
                r["repetition"] = rep
                r["verified"] = "external"
                requests = fnum(r, "server_requests")
                iterations = fnum(r, "iterations")
                r["valid"] = "true" if requests is not None and requests == iterations else "false"
                r["arm_scope"] = EXTERNAL_SCOPE
                r["_external"] = "1"
                rows.append(r)
    if not rows:
        raise SystemExit(f"no result rows under {path}")
    return rundir, rows


def exclusion_reason(r, include_non_steady):
    reasons = []
    if not is_true(r, "valid"):
        reasons.append("invalid")
    if not r.get("_external") and not is_true(r, "verified"):
        reasons.append("unverified")
    if not include_non_steady and not is_true(r, "steady_state"):
        reasons.append("not-steady")
    return reasons


def paired(base_rows, cand_rows, field):
    """Per-repetition ratios cand/base for reps where both arms have an included row."""
    b = {r["repetition"]: fnum(r, field) for r in base_rows}
    c = {r["repetition"]: fnum(r, field) for r in cand_rows}
    reps = sorted(set(b) & set(c), key=lambda k: int(k) if k.isdigit() else k)
    return [c[k] / b[k] for k in reps if b[k] and c[k] is not None and c[k] > 0]


def ratio_stats(ratios):
    n = len(ratios)
    if n == 0:
        return None
    logs = [math.log(x) for x in ratios]
    m = statistics.fmean(logs)
    gm = math.exp(m)
    if n > 1:
        sd = statistics.stdev(logs)
        h = t975(n - 1) * sd / math.sqrt(n)
        lo, hi = math.exp(m - h), math.exp(m + h)
        spread = statistics.stdev(ratios) * 100
    else:
        lo = hi = float("nan")
        spread = 0.0
    return {"n": n, "gm": gm, "lo": lo, "hi": hi, "mean_delta": (statistics.fmean(ratios) - 1) * 100,
            "spread": spread, "rel_half": (hi - lo) / 2 / gm if n > 1 else float("nan")}


def fmt(v, digits=1):
    if v is None or (isinstance(v, float) and math.isnan(v)):
        return "-"
    if abs(v) >= 1000:
        return f"{v:,.0f}"
    return f"{v:,.{digits}f}"


def ci(s):
    return "-" if s is None or s["n"] < 2 else f"[{s['lo']:.3f}, {s['hi']:.3f}]"


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("path")
    ap.add_argument("--reference", default="v3-sync-apache5")
    ap.add_argument("--include-non-steady", action="store_true",
                    help="keep rows whose window still contained JIT compilation (smoke runs only)")
    ap.add_argument("--out", help="write the report here (default: stdout)")
    a = ap.parse_args(argv)
    rundir, rows = load(a.path)
    out = []
    p = out.append

    labels = list(OrderedDict.fromkeys(r["client"] for r in rows))
    scen_seen = list(OrderedDict.fromkeys(r["scenario"] for r in rows))
    scenarios = [s for s in SCENARIO_ORDER if s in scen_seen] + [s for s in scen_seen if s not in SCENARIO_ORDER]
    included = defaultdict(list)
    excluded = defaultdict(int)
    excluded_by_cell = defaultdict(list)
    for r in rows:
        reasons = exclusion_reason(r, a.include_non_steady)
        if reasons:
            for reason in reasons:
                excluded[reason] += 1
            excluded_by_cell[(r["client"], r["scenario"])].append("+".join(reasons))
        else:
            included[(r["client"], r["scenario"])].append(r)
    all_by_cell = defaultdict(list)
    for r in rows:
        all_by_cell[(r["client"], r["scenario"])].append(r)

    p(f"# V3 paired benchmark report — `{rundir.name}`\n")
    manifest = rundir / "manifest.md"
    if manifest.exists():
        text = manifest.read_text().splitlines()
        cfg = [ln for ln in text if ln.startswith("- ") and any(
            ln.startswith(f"- {k}") for k in ("arms", "scenarios", "repetitions", "iterations", "extra runner",
                                              "phase", "null arm", "v2 reference", "load average", "power:",
                                              "jdk", "model", "top-cpu", "processes"))]
        if cfg:
            p("## Configuration (from manifest.md)\n")
            seen = set()
            for ln in cfg:
                if ln not in seen:
                    p(ln)
                    seen.add(ln)
            p("")
    commits = sorted({r.get("commit", "") for r in rows if not r.get("_external")})
    harness = sorted({r.get("harness", "") for r in rows if not r.get("_external")})
    sdks = sorted({r.get("sdk_commit", "") for r in rows if not r.get("_external")})
    p(f"- rows: {len(rows)}, included: {sum(len(v) for v in included.values())}, excluded: "
      f"{sum(1 for r in rows if exclusion_reason(r, a.include_non_steady))} "
      f"(reasons: {', '.join(f'{k}={v}' for k, v in sorted(excluded.items())) or 'none'})")
    p(f"- V3 harness `{', '.join(harness)}`, repo commit `{', '.join(commits)}`, SDK jars `{', '.join(sdks)}`"
      + ("" if len(harness) == 1 and len(sdks) == 1 else " — **WARNING: arms differ in build; not a clean comparison**"))
    p(f"- reference arm for paired ratios: `{a.reference}`")
    if a.include_non_steady:
        p("- **--include-non-steady: rows whose window contained JIT compilation are INCLUDED. Per-op CPU and "
          "allocation from those rows are not steady-state numbers; use this for smoke runs only.**")
    p("")

    scopes = OrderedDict()
    for lab in labels:
        r0 = next(r for r in rows if r["client"] == lab)
        scopes[lab] = (r0.get("arm", "") or lab, r0.get("arm_label", ""), r0.get("arm_scope", ""),
                       r0.get("transport_class", "") or r0.get("transport", ""))
    p("## Arms and scope\n")
    p("| label | arm | honest label | scope | transport |")
    p("|---|---|---|---|---|")
    for lab, (arm, honest, scope, transport) in scopes.items():
        p(f"| `{lab}` | {arm} | {honest or '-'} | {scope or '-'} | {transport or '-'} |")
    if any(r.get("_external") for r in rows):
        p("\n**Scope difference:** V3 arms measure serde+transport only (no signer, endpoint resolution, retries, "
          "interceptors — V3 has none yet). The external reference arms run racecar's full clients, which do all of "
          "those per call. A ratio against them is an upper bound on V3's remaining budget, not a like-for-like "
          "comparison. External rows are not response-verified by the V3 harness (racecar's runner records no "
          "digest); their `valid` is server_requests == iterations only.")
    p("")

    for s in scenarios:
        p(f"## {s}\n")
        p("| arm | n incl/total | excl | app CPU µs/op median (min–max) | wall µs/op median | ops/s median "
          "| p50 µs | p99 µs | alloc B/op | steady | valid |")
        p("|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|")
        for lab in labels:
            allr = all_by_cell.get((lab, s), [])
            if not allr:
                continue
            inc = included.get((lab, s), [])
            steady = sum(1 for r in allr if is_true(r, "steady_state"))
            valid = sum(1 for r in allr if is_true(r, "valid"))
            excl = excluded_by_cell.get((lab, s), [])
            excl_s = ", ".join(f"{k}×{excl.count(k)}" for k in OrderedDict.fromkeys(excl)) or "-"

            def med(field):
                vals = [v for v in (fnum(r, field) for r in inc) if v is not None]
                return statistics.median(vals) if vals else None

            cpu = [v for v in (fnum(r, "app_cpu_us_per_op") for r in inc) if v is not None]
            cpu_s = f"{fmt(statistics.median(cpu))} ({fmt(min(cpu))}–{fmt(max(cpu))})" if cpu else "-"
            few = " ⚠<5" if len(inc) < 5 else ""
            p(f"| `{lab}` | {len(inc)}/{len(allr)}{few} | {excl_s} | {cpu_s} | {fmt(med('avg_us_per_op'))} "
              f"| {fmt(med('ops_per_wall_sec'), 0)} | {fmt(med('p50_us'))} | {fmt(med('p99_us'))} "
              f"| {fmt(med('alloc_bytes_per_op'), 0)} | {steady}/{len(allr)} | {valid}/{len(allr)} |")
        p("")
        base = included.get((a.reference, s), [])
        if not base:
            p(f"_No included `{a.reference}` rows for {s}; no paired ratios._\n")
            continue
        p(f"Paired ratios vs `{a.reference}` (arm/reference per repetition; GM = geometric mean of the per-rep "
          "ratios, CI = 95% t-interval on log ratios; racecar's paired delta / spread / wins alongside):\n")
        p("| arm | metric | n pairs | GM ratio | 95% CI | rel. half-width | racecar paired delta | spread of pairs "
          "| wins |")
        p("|---|---|---:|---:|---|---:|---:|---:|---:|")
        for lab in labels:
            if lab == a.reference or lab.endswith(NULL_SUFFIX):
                continue
            cand = included.get((lab, s), [])
            if not cand:
                continue
            for field, mlabel, lower_better in METRICS[:5]:
                ratios = paired(base, cand, field)
                st = ratio_stats(ratios)
                if not st:
                    continue
                wins = sum(1 for x in ratios if (x < 1.0) == lower_better and x != 1.0)
                rh = "-" if math.isnan(st["rel_half"]) else f"{st['rel_half'] * 100:.1f}%" + (
                    "" if st["rel_half"] <= 0.05 else " ✗")
                p(f"| `{lab}` | {mlabel} | {st['n']} | {st['gm']:.3f} | {ci(st)} | {rh} | {st['mean_delta']:+.1f}% "
                  f"| ±{st['spread']:.1f}% | {wins}/{st['n']} |")
        p("")

    p("## Same-artifact null experiment (Req 20.8 / 20.24)\n")
    nulls = [lab for lab in labels if lab.endswith(NULL_SUFFIX)]
    if not nulls:
        p("_No null arm in this run (paired.sh --null-arm ARM adds one)._\n")
    else:
        p("The same arm, same artifact and flags, run twice per repetition under two labels in different order "
          "slots. Pass requires, per metric: 1.0 inside the 95% CI (no detectable difference) AND the GM ratio "
          "inside 0.95–1.05 (Req 20.24). Req 20.25: any failure invalidates a release-gate collection.\n")
        p("| null arm | scenario | metric | n pairs | GM ratio | 95% CI | 1.0 in CI | in 0.95–1.05 | verdict |")
        p("|---|---|---|---:|---:|---|---|---|---|")
        overall = True
        for nl in nulls:
            base_lab = nl[:-len(NULL_SUFFIX)]
            for s in scenarios:
                b = included.get((base_lab, s), [])
                c = included.get((nl, s), [])
                for field in NULL_METRICS:
                    st = ratio_stats(paired(b, c, field))
                    if not st:
                        p(f"| `{nl}` | {s} | {field} | 0 | - | - | - | - | NO DATA |")
                        overall = False
                        continue
                    in_ci = st["n"] > 1 and st["lo"] <= 1.0 <= st["hi"]
                    in_band = 0.95 <= st["gm"] <= 1.05
                    ok = in_ci and in_band and st["n"] >= 5
                    overall &= ok
                    verdict = "pass" if ok else ("FAIL (<5 pairs)" if st["n"] < 5 else "FAIL")
                    p(f"| `{nl}` | {s} | {field} | {st['n']} | {st['gm']:.3f} | {ci(st)} | {'yes' if in_ci else 'no'} "
                      f"| {'yes' if in_band else 'no'} | {verdict} |")
        p(f"\n**Null experiment overall: {'PASS' if overall else 'FAIL'}**\n")

    p("## Per-arm run-to-run spread (racecar)\n")
    p("Where this is much larger than the paired spread above, pairing is doing real work.\n")
    p("| arm | scenario | app CPU min | mean | max | spread |")
    p("|---|---|---:|---:|---:|---:|")
    for s in scenarios:
        for lab in labels:
            vals = [v for v in (fnum(r, "app_cpu_us_per_op") for r in included.get((lab, s), [])) if v is not None]
            if not vals:
                continue
            lo, hi = min(vals), max(vals)
            p(f"| `{lab}` | {s} | {fmt(lo)} | {fmt(statistics.fmean(vals))} | {fmt(hi)} "
              f"| {((hi - lo) / lo * 100 if lo else 0):.1f}% |")
    p("")
    text = "\n".join(out) + "\n"
    if a.out:
        Path(a.out).write_text(text)
        print(f"report: {a.out}")
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    main(sys.argv[1:])
