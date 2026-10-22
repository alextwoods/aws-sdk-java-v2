#!/usr/bin/env python3
#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
#
"""Summarizes a serde A/B run of scripts/schema-serde/serde-ab.sh (serde benchmark plan, D6 and D7).

Python 3 standard library only; reuses paired_ab_summary.py (statistics, formatting) and optimization_experiment.py
(JMH result loading, zero-allocation rule) from the same directory.

Usage:
  serde_ab_summary.py collect RUNDIR
      Reads RUNDIR/arms.tsv, params.tsv, runs.tsv and the JMH -rf json files runs.tsv names (jmh/*.json) and writes
      RUNDIR/serde-ab.json (schema_version 1), the input of summarize.
  serde_ab_summary.py summarize RUN_JSON [--null NULL_JSON] [--boundary BOUNDARY_TSV] [--preliminary]
      Prints a Markdown summary: provenance, then per (benchmark, params) the baseline (first arm) and every other
      arm's mean ns/op ± error, the paired ratio and its spread, gc.alloc.rate.norm B/op and its ratio, the floor
      from the null experiment and a verdict; then per-class and overall geometric-mean ratios and the problems
      (failed runs, rows missing on an arm). A null-experiment run reports its floor per row instead. --preliminary
      adds a smoke-data banner. --boundary takes the boundary.tsv of serde_ab_profile.py compare (a profiling pass of
      the same jars): the table then names, per row, the work-boundary items (A1 per-call legacy marshaller
      construction, A2 response metadata and status check, A5 Query form percent-encoding) at >= 10% of either
      arm's CPU or B/op, and a serde-only estimate follows: per row and as geometric means, each arm's values
      scaled by 1 - the items' share (CPU share for ns/op, alloc share for B/op), with the same floor.
  serde_ab_summary.py --self-test
      Runs the fixture tests below.

Statistics (per benchmark, params and arm; one value per repetition = that repetition's JMH score):
  mean    the mean over repetitions of the JMH score (SampleTime mean, ns/op) or of gc.alloc.rate.norm (B/op).
  error   the 99.9% Student-t half-width over repetitions (paired_ab_summary.arm_stats); with one repetition, JMH's
          own scoreError.
  ratio   the mean of the per-repetition ratios arm/baseline (paired_ab_summary.paired); spread is their standard
          deviation in percent. Arms run back to back inside a repetition, so the pairing removes slow drift.
  floor   from a null experiment (the same jar as LABEL-a and LABEL-b): max(|ratio - 1|, spread, the relative JMH
          scoreError of every null run), in percent, as optimization_experiment.jmh_items does. B/op values below
          1 B/op (optimization_experiment.ZERO_ALLOC) are compared absolutely: floor 0, and an arm regresses only if
          it allocates 1 B/op or more.
  verdict faster / slower when |ratio - 1| exceeds the floor, "within floor" otherwise, "no floor" without a null
          experiment row.
  geomean the geometric mean of the per-row ratios of a class (and of all rows); B/op rows below 1 B/op on either
          arm are left out of the B/op geomean.

The serde-ab.json format (schema_version 1):
  {"schema_version": 1, "params": {key: value}, "arms": [{"label", "ref", "commit", ...}],
   "runs": [{"run", "rep", "arm", "class", "status", "started", "json",
             "results": [{"benchmark", "params", "score", "score_error", "alloc", "alloc_error"}]}]}
"""
import csv
import json
import math
import os
import statistics
import sys
from collections import OrderedDict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import optimization_experiment as oe  # noqa: E402
import paired_ab_summary as pab  # noqa: E402

SCHEMA_VERSION = 1
ALLOC = "gc.alloc.rate.norm"
BOUNDARY_FLAG_PCT = 10.0  # a work-boundary item at or above this share of either arm is flagged per row


# ==================== collect ====================

def _read_tsv(path):
    with open(path, newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def collect(rundir):
    """Builds the serde-ab.json document from the files serde-ab.sh wrote under rundir."""
    params = OrderedDict((row["key"], row["value"]) for row in _read_tsv(os.path.join(rundir, "params.tsv")))
    arms = _read_tsv(os.path.join(rundir, "arms.tsv"))
    runs = []
    for row in _read_tsv(os.path.join(rundir, "runs.tsv")):
        path = os.path.join(rundir, row["json"]) if row.get("json") else None
        results = []
        if path and os.path.isfile(path) and os.path.getsize(path) > 0:
            for (benchmark, p), metrics in oe.load_jmh(path).items():
                score, score_error = metrics["score"]
                alloc, alloc_error = metrics.get(ALLOC, (None, None))
                results.append({"benchmark": benchmark, "params": p, "score": score, "score_error": score_error,
                                "alloc": alloc, "alloc_error": alloc_error})
        runs.append({"run": int(row["run"]), "rep": int(row["rep"]), "arm": row["arm"], "class": row["class"],
                     "status": row["status"], "started": row["started"], "json": row.get("json"),
                     "results": results})
    return {"schema_version": SCHEMA_VERSION, "params": params, "arms": arms, "runs": runs}


# ==================== analysis ====================

def load(path):
    with open(path) as f:
        doc = json.load(f)
    if doc.get("schema_version") != SCHEMA_VERSION:
        raise SystemExit(f"{path}: unsupported schema_version {doc.get('schema_version')!r}")
    return doc


def is_null(doc):
    return str(doc["params"].get("null_experiment", "false")).lower() == "true"


def labels(doc):
    return [a["label"] for a in doc["arms"]]


def keys(doc):
    """(benchmark, params) in run order."""
    return list(OrderedDict.fromkeys((r["benchmark"], r["params"]) for run in doc["runs"] for r in run["results"]))


def samples(doc, arm, key, metric):
    """{rep: (value, jmh error)} of one arm and row; metric is "score" or "alloc"."""
    out = {}
    for run in doc["runs"]:
        if run["arm"] != arm:
            continue
        for r in run["results"]:
            if (r["benchmark"], r["params"]) == key and isinstance(r.get(metric), (int, float)):
                out[run["rep"]] = (float(r[metric]), r.get(metric + "_error"))
    return out


def stats(by_rep):
    """Mean, error and n: the 99.9% t half-width over repetitions, or JMH's error for one repetition."""
    s = pab.arm_stats([v for v, _ in by_rep.values()])
    if s is not None and s["n"] == 1:
        s["error"] = next(iter(by_rep.values()))[1]
    return s


def _rel_err(by_rep):
    return [oe._rel_err(v, e) for v, e in by_rep.values()]


def compare(base, cand, metric):
    """Paired comparison of two {rep: (value, error)} maps; B/op baselines below 1 B/op compare absolutely."""
    if metric == "alloc" and base and all(v < oe.ZERO_ALLOC for v, _ in base.values()):
        if not cand:
            return None
        zero = all(v < oe.ZERO_ALLOC for v, _ in cand.values())
        return {"ratio": None, "delta_pct": 0.0 if zero else math.inf, "spread_pct": 0.0,
                "pairs": len(set(base) & set(cand)), "zero_alloc": True}
    c = pab.paired({k: v for k, (v, _) in base.items()}, {k: v for k, (v, _) in cand.items()})
    if c is not None:
        c["zero_alloc"] = False
    return c


def null_floor(a, b, metric):
    """Floor in percent from the two arms of a null experiment, or None."""
    c = compare(a, b, metric)
    if c is None:
        return None
    if c["zero_alloc"]:
        return 0.0
    return max([abs(c["delta_pct"]), c["spread_pct"]] + _rel_err(a) + _rel_err(b))


def floors(null_doc):
    """{(key, metric): floor_pct} from a null-experiment document."""
    if not is_null(null_doc):
        raise SystemExit("--null needs a null-experiment run (serde-ab.sh --null)")
    a, b = labels(null_doc)[:2]
    out = {}
    for key in keys(null_doc):
        for metric in ("score", "alloc"):
            f = null_floor(samples(null_doc, a, key, metric), samples(null_doc, b, key, metric), metric)
            if f is not None:
                out[(key, metric)] = f
    return out


def verdict(comparison, floor):
    if comparison is None:
        return "missing"
    if floor is None:
        return "no floor"
    if comparison["delta_pct"] == math.inf:
        return "slower (allocates)"
    if comparison["delta_pct"] < -floor:
        return "faster"
    if comparison["delta_pct"] > floor:
        return "slower"
    return "within floor"


def analyze(doc, null_doc=None):
    """{key: {"arms": {label: {"score": stats, "alloc": stats}}, "vs_base": {label: {metric: comparison}},
    "floor": {metric: pct}}}."""
    arm_labels = labels(doc)
    base = arm_labels[0]
    null_floors = floors(null_doc) if null_doc else {}
    out = OrderedDict()
    for key in keys(doc):
        values = {a: {m: samples(doc, a, key, m) for m in ("score", "alloc")} for a in arm_labels}
        entry = {"arms": {a: {m: stats(values[a][m]) for m in values[a]} for a in arm_labels},
                 "vs_base": {a: {m: compare(values[base][m], values[a][m], m) for m in ("score", "alloc")}
                             for a in arm_labels[1:]},
                 "floor": {}}
        for m in ("score", "alloc"):
            if is_null(doc) and len(arm_labels) > 1:
                f = null_floor(values[base][m], values[arm_labels[1]][m], m)
            else:
                f = null_floors.get((key, m))
            if f is not None:
                entry["floor"][m] = f
        out[key] = entry
    return out


def geomean(ratios):
    ratios = [r for r in ratios if r and r > 0 and math.isfinite(r)]
    return math.exp(statistics.fmean(math.log(r) for r in ratios)) if ratios else None


def problems(doc):
    found = []
    failed = [r for r in doc["runs"] if r["status"] != "ok"]
    if failed:
        found.append(f"{len(failed)} of {len(doc['runs'])} JMH runs failed: "
                     + ", ".join(f"rep{r['rep']} {r['class']} {r['arm']}" for r in failed[:10]))
    arm_labels = labels(doc)
    for key in keys(doc):
        missing = [a for a in arm_labels if not samples(doc, a, key, "score")]
        if missing:
            found.append(f"{_name(key)}: no result on {', '.join(missing)}")
    return found


# ==================== Markdown ====================

def _name(key):
    benchmark, params = key
    parts = benchmark.split(".")
    case = params.split("=", 1)[-1] if params else "-"
    return f"{'.'.join(parts[-2:])} [{case}]"


def _class(key):
    return key[0].split(".")[-2]


def _case(key):
    return key[1].split("=", 1)[-1] if key[1] else "-"


def _mean_err(s, digits=1):
    if s is None:
        return "-"
    return f"{pab._fmt(s['mean'], digits)} ± {pab._fmt(s['error'], digits) or '-'} ({s['n']})"


def _ratio(c):
    if c is None:
        return "-"
    if c["zero_alloc"]:
        return "<1 B" if c["delta_pct"] == 0 else "≥1 B"
    return f"{c['ratio']:.3f} ±{c['spread_pct']:.1f}%"


def _floor(f):
    return "-" if f is None else f"{f:.1f}%"


def render_summary(doc, null_doc=None, preliminary=False, boundary=None):
    arm_labels = labels(doc)
    p = doc["params"]
    lines = [f"# Serde A/B summary `{p.get('run_id', '?')}`", ""]
    if preliminary:
        lines += ["> **PRELIMINARY — smoke data, not measurements.** Short JMH settings on an unpinned machine; the "
                  "numbers show that the tooling works end to end, not how the arms compare.", ""]
    kind = "null experiment (the same jar as both arms)" if is_null(doc) else "comparison"
    lines += [f"Kind: {kind}. Baseline arm: `{arm_labels[0]}`."
              + (f" Floors from null run `{null_doc['params'].get('run_id', '?')}`." if null_doc else ""), ""]
    lines += ["| arm | ref | commit | bench src | overlay | sdk pin | expected path | jar sha256 |",
              "|---|---|---|---|---|---|---|---|"]
    for a in doc["arms"]:
        lines.append(f"| `{a['label']}` | `{a.get('ref', '')}` | `{(a.get('commit') or '')[:12]}` "
                     f"| `{(a.get('bench_src') or '')[:12]}` | {a.get('overlay', '')} | {a.get('sdk_pin', '')} "
                     f"| {a.get('expect_path', '')} | `{(a.get('jar_sha256') or '')[:12]}` |")
    lines += ["", f"Parameters: include `{p.get('include', '')}`, classes {p.get('classes', '')}, JMH options "
              f"`{p.get('jmh_opts', '') or '(class annotations)'}`, JVM args `{p.get('jvm_args', '')}`, reps "
              f"{p.get('reps')}, order {p.get('order')}, pin `{p.get('pin', '') or 'none'}`, verify "
              f"{p.get('verify', '?')}.", f"Host: {p.get('host', '?')} ({p.get('hardware', '?')}); "
              f"{p.get('java', '?')}.", ""]
    for prob in problems(doc):
        lines += [f"**WARNING: {prob}.**", ""]
    analysis = analyze(doc, null_doc)
    if is_null(doc):
        a, b = arm_labels[:2]
        lines += ["## Noise floor per row", "",
                  f"| class | case | {a} ns/op | {b} ns/op | ratio | floor | {a} B/op | {b} B/op | B/op floor |",
                  "|---|---|---:|---:|---:|---:|---:|---:|---:|"]
        for key, e in analysis.items():
            lines.append(f"| {_class(key)} | {_case(key)} | {_mean_err(e['arms'][a]['score'])} "
                         f"| {_mean_err(e['arms'][b]['score'])} | {_ratio(e['vs_base'][b]['score'])} "
                         f"| {_floor(e['floor'].get('score'))} | {_mean_err(e['arms'][a]['alloc'], 0)} "
                         f"| {_mean_err(e['arms'][b]['alloc'], 0)} | {_floor(e['floor'].get('alloc'))} |")
        floor_values = [e["floor"]["score"] for e in analysis.values() if "score" in e["floor"]]
        if floor_values:
            lines += ["", f"ns/op floor: median {statistics.median(floor_values):.1f}%, "
                          f"max {max(floor_values):.1f}% over {len(floor_values)} rows."]
        return "\n".join(lines) + "\n"
    base = arm_labels[0]
    for arm in arm_labels[1:]:
        lines += [f"## `{arm}` vs `{base}`", ""]
        if boundary is not None:
            lines += [f"What each arm's timed method costs, work-boundary items included. Rows where an item (A1, A2, "
                      f"A5) is at least {BOUNDARY_FLAG_PCT:g}% of either arm's CPU or B/op are named in the last "
                      f"column; the serde-only estimate follows the geometric means.", ""]
        lines += [f"| class | case | {base} ns/op | {arm} ns/op | ratio | floor | verdict "
                  f"| {base} B/op | {arm} B/op | B/op ratio | B/op floor | B/op verdict |"
                  + (f" boundary ≥{BOUNDARY_FLAG_PCT:g}% |" if boundary is not None else ""),
                  "|---|---|---:|---:|---:|---:|---|---:|---:|---:|---:|---|" + ("---|" if boundary is not None else "")]
        per_class = OrderedDict()
        for key, e in analysis.items():
            sc, al = e["vs_base"][arm]["score"], e["vs_base"][arm]["alloc"]
            fs, fa = e["floor"].get("score"), e["floor"].get("alloc")
            lines.append(f"| {_class(key)} | {_case(key)} | {_mean_err(e['arms'][base]['score'])} "
                         f"| {_mean_err(e['arms'][arm]['score'])} | {_ratio(sc)} | {_floor(fs)} | {verdict(sc, fs)} "
                         f"| {_mean_err(e['arms'][base]['alloc'], 0)} | {_mean_err(e['arms'][arm]['alloc'], 0)} "
                         f"| {_ratio(al)} | {_floor(fa)} | {verdict(al, fa)} |"
                         + (f" {boundary_flag(boundary, key, (base, arm)) or '-'} |" if boundary is not None else ""))
            bucket = per_class.setdefault(_class(key), {"score": [], "alloc": []})
            if sc is not None:
                bucket["score"].append(sc["ratio"])
            if al is not None and not al["zero_alloc"]:
                bucket["alloc"].append(al["ratio"])
        lines += ["", f"### Geometric-mean ratios, `{arm}` / `{base}`", "",
                  "| class | rows | ns/op ratio | B/op ratio |", "|---|---:|---:|---:|"]
        all_score, all_alloc = [], []
        for cls, bucket in per_class.items():
            all_score += bucket["score"]
            all_alloc += bucket["alloc"]
            lines.append(f"| {cls} | {len(bucket['score'])} | {_geo(bucket['score'])} | {_geo(bucket['alloc'])} |")
        lines += [f"| **all** | {len(all_score)} | {_geo(all_score)} | {_geo(all_alloc)} |", ""]
        if boundary is not None:
            lines += render_boundary(analysis, boundary, base, arm)
    return "\n".join(lines)


def _geo(ratios):
    g = geomean(ratios)
    return "-" if g is None else f"{g:.3f}"


# ==================== work boundary (serde-only estimate) ====================

def load_boundary(path):
    """{(class, case, arm): {"cpu": total %, "alloc": total %, "items": {item: (cpu %, alloc %)}}} from the
    boundary.tsv of serde_ab_profile.py compare; a share is None where that profile is missing."""
    out = {}
    for row in _read_tsv(path):
        items = OrderedDict()
        for col in row:
            if col.endswith("_cpu_pct"):
                item = col[:-len("_cpu_pct")]
                cpu, alloc = row[col], row.get(f"{item}_alloc_pct", "")
                items[item] = (float(cpu) if cpu else None, float(alloc) if alloc else None)
        total = {k: (None if any(v[i] is None for v in items.values()) else sum(v[i] for v in items.values()))
                 for i, k in enumerate(("cpu", "alloc"))}
        out[(row["class"], row["param"], row["arm"])] = dict(total, items=items)
    return out


def _boundary_of(boundary, key, arm):
    return boundary.get((_class(key), _case(key), arm))


def boundary_flag(boundary, key, arms):
    """Text naming the work-boundary items at >= BOUNDARY_FLAG_PCT of CPU or B/op on any of the arms, or ""."""
    parts = []
    for arm in arms:
        b = _boundary_of(boundary, key, arm)
        for item, (cpu, alloc) in (b["items"].items() if b else []):
            if (cpu or 0) >= BOUNDARY_FLAG_PCT or (alloc or 0) >= BOUNDARY_FLAG_PCT:
                parts.append(f"{arm} {item} {_pct(cpu)} CPU, {_pct(alloc)} B")
    return "; ".join(parts)


def _pct(value):
    return "-" if value is None else f"{value:.0f}%"


def _keep(b, metric):
    """The fraction of an arm's measurement that is serde work: 1 - its boundary share, or None."""
    if b is None or b[metric] is None or b[metric] >= 100.0:
        return None
    return 1.0 - b[metric] / 100.0


def adjust_stats(s, keep):
    if s is None or keep is None:
        return None
    return {"mean": s["mean"] * keep, "error": None if s["error"] is None else s["error"] * keep, "n": s["n"]}


def adjust_comparison(c, keep_base, keep_arm):
    """A paired comparison with each arm's per-rep values scaled by its serde fraction (the same factor every rep)."""
    if c is None:
        return None
    if c["zero_alloc"]:
        return c
    if keep_base is None or keep_arm is None:
        return None
    k = keep_arm / keep_base
    ratio = c["ratio"] * k
    return {"ratio": ratio, "delta_pct": (ratio - 1.0) * 100, "spread_pct": c["spread_pct"] * k,
            "pairs": c["pairs"], "zero_alloc": False}


def render_boundary(analysis, boundary, base, arm):
    """The serde-only (boundary-adjusted) estimate of one arm against the baseline."""
    metric_of = {"score": "cpu", "alloc": "alloc"}
    lines = [f"### Serde-only estimate (boundary-adjusted), `{arm}` / `{base}`", "",
             "Estimate, not a measurement: each arm's mean ns/op (B/op) is scaled by 1 - the CPU (alloc) share of the "
             "work-boundary items A1 (per-call legacy marshaller construction), A2 (response metadata and status "
             "check) and A5 (Query form percent-encoding) in the profiling pass (`boundary.tsv`; rules in "
             "`serde_ab_profile.py`). Shares come from a different run than the timing, the same floor is applied, "
             "and A5 is a lower bound. The table above is what each arm's timed method costs; this one is the serde "
             "work alone.", "",
             f"| class | case | {base} ns/op | {arm} ns/op | ratio | floor | verdict | {base} B/op | {arm} B/op "
             f"| B/op ratio | B/op verdict | removed |",
             "|---|---|---:|---:|---:|---:|---|---:|---:|---:|---|---|"]
    per_class, missing = OrderedDict(), 0
    for key, e in analysis.items():
        bb, ba = _boundary_of(boundary, key, base), _boundary_of(boundary, key, arm)
        if bb is None or ba is None:
            missing += 1
            lines.append(f"| {_class(key)} | {_case(key)} | - | - | - | - | no profile row | - | - | - | - | - |")
            continue
        cells, adjusted = [], {}
        for m in ("score", "alloc"):
            kb, ka = _keep(bb, metric_of[m]), _keep(ba, metric_of[m])
            c = adjust_comparison(e["vs_base"][arm][m], kb, ka)
            adjusted[m] = c
            f = e["floor"].get(m)
            digits = 1 if m == "score" else 0
            cells += [_mean_err(adjust_stats(e["arms"][base][m], kb), digits),
                      _mean_err(adjust_stats(e["arms"][arm][m], ka), digits), _ratio(c)]
            cells += [_floor(f), verdict(c, f)] if m == "score" else [verdict(c, f)]
        removed = "; ".join(f"{a} {_pct(b['cpu'])} CPU, {_pct(b['alloc'])} B" for a, b in ((base, bb), (arm, ba))
                            if (b["cpu"] or 0) >= 1.0 or (b["alloc"] or 0) >= 1.0)
        lines.append(f"| {_class(key)} | {_case(key)} | " + " | ".join(cells) + f" | {removed or '-'} |")
        bucket = per_class.setdefault(_class(key), {"score": [], "alloc": []})
        if adjusted["score"] is not None:
            bucket["score"].append(adjusted["score"]["ratio"])
        if adjusted["alloc"] is not None and not adjusted["alloc"]["zero_alloc"]:
            bucket["alloc"].append(adjusted["alloc"]["ratio"])
    if missing:
        lines += ["", f"**WARNING: {missing} rows have no boundary row for both arms; left out of the geomeans.**"]
    lines += ["", f"#### Serde-only geometric-mean ratios (boundary-adjusted), `{arm}` / `{base}`", "",
              "| class | rows | ns/op ratio | B/op ratio |", "|---|---:|---:|---:|"]
    all_score, all_alloc = [], []
    for cls, bucket in per_class.items():
        all_score += bucket["score"]
        all_alloc += bucket["alloc"]
        lines.append(f"| {cls} | {len(bucket['score'])} | {_geo(bucket['score'])} | {_geo(bucket['alloc'])} |")
    return lines + [f"| **all** | {len(all_score)} | {_geo(all_score)} | {_geo(all_alloc)} |", ""]


# ==================== CLI ====================

def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    if len(argv) == 2 and argv[0] == "collect":
        doc = collect(argv[1])
        path = os.path.join(argv[1], "serde-ab.json")
        with open(path, "w") as f:
            json.dump(doc, f, indent=1)
            f.write("\n")
        print(f"Wrote {path}", file=sys.stderr)
        return 0
    if len(argv) >= 2 and argv[0] == "summarize":
        rest = argv[2:]
        preliminary = "--preliminary" in rest
        rest = [x for x in rest if x != "--preliminary"]
        opts = {"--null": None, "--boundary": None}
        while rest:
            if len(rest) < 2 or rest[0] not in opts:
                raise SystemExit(__doc__)
            opts[rest[0]] = rest[1]
            rest = rest[2:]
        null_doc = load(opts["--null"]) if opts["--null"] else None
        boundary = load_boundary(opts["--boundary"]) if opts["--boundary"] else None
        sys.stdout.write(render_summary(load(argv[1]), null_doc, preliminary, boundary))
        return 0
    raise SystemExit(__doc__)


# ==================== self-test ====================

def self_test():
    import tempfile
    import unittest

    bench = "software.amazon.awssdk.benchmark.serde.RestJsonMarshallBenchmark.marshall"
    unbench = "software.amazon.awssdk.benchmark.serde.QueryUnmarshallBenchmark.unmarshall"

    def result(benchmark, case, score, alloc, err=1.0):
        return {"benchmark": benchmark, "params": f"testCaseId={case}", "score": score, "score_error": err,
                "alloc": alloc, "alloc_error": 0.0}

    def run(n, rep, arm, cls, results, status="ok"):
        return {"run": n, "rep": rep, "arm": arm, "class": cls, "status": status, "started": "t", "json": "j",
                "results": results}

    def doc(arms, runs, null=False):
        return {"schema_version": 1, "params": {"run_id": "fixture", "null_experiment": str(null).lower()},
                "arms": [{"label": a, "ref": "r", "commit": "c" * 40} for a in arms], "runs": runs}

    # Null: per-rep ratios 1.02 and 0.98 -> spread 2.83%; JMH errors 1/100 = 1%.
    null_doc = doc(["f-a", "f-b"], [
        run(1, 1, "f-a", "RestJsonMarshallBenchmark", [result(bench, "S", 100.0, 500.0), result(bench, "Z", 50.0, 0.0)]),
        run(2, 1, "f-b", "RestJsonMarshallBenchmark", [result(bench, "S", 102.0, 500.0), result(bench, "Z", 50.0, 0.0)]),
        run(3, 2, "f-b", "RestJsonMarshallBenchmark", [result(bench, "S", 98.0, 500.0), result(bench, "Z", 50.0, 0.0)]),
        run(4, 2, "f-a", "RestJsonMarshallBenchmark", [result(bench, "S", 100.0, 500.0), result(bench, "Z", 50.0, 0.0)]),
    ], null=True)
    # A/B: feature 20% faster and 40% less allocation on S; zero-alloc row Z stays at 0; a row missing on feature.
    ab_doc = doc(["baseline", "feature"], [
        run(1, 1, "baseline", "RestJsonMarshallBenchmark",
            [result(bench, "S", 100.0, 500.0), result(bench, "Z", 50.0, 0.0)]),
        run(2, 1, "feature", "RestJsonMarshallBenchmark",
            [result(bench, "S", 80.0, 300.0), result(bench, "Z", 50.5, 0.0)]),
        run(3, 2, "feature", "RestJsonMarshallBenchmark",
            [result(bench, "S", 80.0, 300.0), result(bench, "Z", 50.5, 0.0)]),
        run(4, 2, "baseline", "RestJsonMarshallBenchmark",
            [result(bench, "S", 100.0, 500.0), result(bench, "Z", 50.0, 0.0)]),
        run(5, 1, "baseline", "QueryUnmarshallBenchmark", [result(unbench, "M", 1000.0, 9000.0, err=40.0)]),
        run(6, 1, "feature", "QueryUnmarshallBenchmark", [], status="failed"),
    ])

    class SerdeAbSummaryTest(unittest.TestCase):
        def test_stats_whenOneRep_usesJmhError(self):
            s = stats(samples(ab_doc, "baseline", (unbench, "testCaseId=M"), "score"))
            self.assertEqual((s["mean"], s["error"], s["n"]), (1000.0, 40.0, 1))
            two = stats(samples(ab_doc, "baseline", (bench, "testCaseId=S"), "score"))
            self.assertEqual(two["n"], 2)
            self.assertAlmostEqual(two["error"], 0.0)

        def test_floors_whenNullRun_takeMaxOfDeltaSpreadAndJmhError(self):
            f = floors(null_doc)
            self.assertAlmostEqual(f[((bench, "testCaseId=S"), "score")], statistics.stdev([1.02, 0.98]) * 100)
            self.assertAlmostEqual(f[((bench, "testCaseId=S"), "alloc")], 0.0)
            self.assertEqual(f[((bench, "testCaseId=Z"), "alloc")], 0.0)
            self.assertAlmostEqual(f[((bench, "testCaseId=Z"), "score")], 2.0)  # 1/50 relative JMH error

        def test_analyze_whenNullApplied_givesVerdicts(self):
            e = analyze(ab_doc, null_doc)[(bench, "testCaseId=S")]
            self.assertAlmostEqual(e["vs_base"]["feature"]["score"]["ratio"], 0.8)
            self.assertEqual(verdict(e["vs_base"]["feature"]["score"], e["floor"]["score"]), "faster")
            self.assertEqual(verdict(e["vs_base"]["feature"]["alloc"], e["floor"]["alloc"]), "faster")
            z = analyze(ab_doc, null_doc)[(bench, "testCaseId=Z")]
            self.assertEqual(verdict(z["vs_base"]["feature"]["score"], z["floor"]["score"]), "within floor")
            self.assertTrue(z["vs_base"]["feature"]["alloc"]["zero_alloc"])
            self.assertEqual(verdict(z["vs_base"]["feature"]["alloc"], 0.0), "within floor")
            self.assertEqual(verdict(z["vs_base"]["feature"]["score"], None), "no floor")

        def test_compare_whenZeroAllocBaselineAndCandidateAllocates_isSlower(self):
            c = compare({1: (0.0, 0.0)}, {1: (24.0, 0.0)}, "alloc")
            self.assertEqual(verdict(c, 0.0), "slower (allocates)")

        def test_render_whenRowMissingOnOneArm_warnsAndShowsGeomean(self):
            text = render_summary(ab_doc, null_doc, preliminary=True)
            self.assertIn("PRELIMINARY", text)
            self.assertIn("1 of 6 JMH runs failed: rep1 QueryUnmarshallBenchmark feature", text)
            self.assertIn("QueryUnmarshallBenchmark.unmarshall [M]: no result on feature", text)
            self.assertIn("| RestJsonMarshallBenchmark | S | 100.0 ± 0.0 (2) | 80.0 ± 0.0 (2) | 0.800 ±0.0% "
                          "| 2.8% | faster |", text)
            self.assertIn("| QueryUnmarshallBenchmark | M | 1,000.0 ± 40.0 (1) | - | - | - | missing |", text)
            # Geomean of 0.8 and 1.01 over the two RestJson rows; B/op leaves out the zero-alloc row.
            self.assertIn(f"| RestJsonMarshallBenchmark | 2 | {math.sqrt(0.8 * 1.01):.3f} | 0.600 |", text)

        def test_render_whenBoundaryGiven_flagsRowsAndAddsSerdeOnlyEstimate(self):
            # Row S: baseline A1 50% CPU / 40% B, feature A2 20% CPU / 25% B; row Z has no boundary row.
            with tempfile.TemporaryDirectory() as d:
                path = os.path.join(d, "boundary.tsv")
                with open(path, "w") as f:
                    f.write("benchmark\tclass\tparam\tarm\tcpu_samples\talloc_bytes\tA1_cpu_pct\tA1_alloc_pct"
                            "\tA2_cpu_pct\tA2_alloc_pct\tA5_cpu_pct\tA5_alloc_pct\n")
                    f.write(f"{bench}\tRestJsonMarshallBenchmark\tS\tbaseline\t100\t100\t50.0\t40.0\t0\t0\t0\t0\n")
                    f.write(f"{bench}\tRestJsonMarshallBenchmark\tS\tfeature\t100\t100\t0\t0\t20.0\t25.0\t0\t0\n")
                boundary = load_boundary(path)
            self.assertEqual(boundary[("RestJsonMarshallBenchmark", "S", "baseline")]["cpu"], 50.0)
            text = render_summary(ab_doc, null_doc, boundary=boundary)
            self.assertIn("| B/op verdict | boundary ≥10% |", text)
            self.assertIn("| 0.600 ±0.0% | 0.0% | faster | baseline A1 50% CPU, 40% B; feature A2 20% CPU, 25% B |",
                          text)
            self.assertIn("### Serde-only estimate (boundary-adjusted), `feature` / `baseline`", text)
            # ns/op: 100 x 0.5 = 50 vs 80 x 0.8 = 64 -> 1.28, slower; B/op: 500 x 0.6 = 300 vs 300 x 0.75 = 225.
            self.assertIn("| RestJsonMarshallBenchmark | S | 50.0 ± 0.0 (2) | 64.0 ± 0.0 (2) | 1.280 ±0.0% | 2.8% "
                          "| slower | 300 ± 0 (2) | 225 ± 0 (2) | 0.750 ±0.0% | faster "
                          "| baseline 50% CPU, 40% B; feature 20% CPU, 25% B |", text)
            self.assertIn("| RestJsonMarshallBenchmark | Z | - | - | - | - | no profile row |", text)
            self.assertIn("WARNING: 2 rows have no boundary row for both arms", text)  # Z and the Query row
            self.assertIn("| RestJsonMarshallBenchmark | 1 | 1.280 | 0.750 |", text)
            self.assertNotIn("boundary ≥10%", render_summary(ab_doc, null_doc))

        def test_render_whenNullRun_reportsFloors(self):
            text = render_summary(null_doc)
            self.assertIn("null experiment", text)
            self.assertIn("## Noise floor per row", text)
            self.assertIn("| RestJsonMarshallBenchmark | S |", text)
            self.assertIn("ns/op floor: median", text)
            with self.assertRaises(SystemExit):
                floors(ab_doc)

        def test_collect_whenRunFilesWritten_readsJmhJson(self):
            jmh = [{"benchmark": bench, "params": {"testCaseId": "S"},
                    "primaryMetric": {"score": 123.0, "scoreError": 4.0},
                    "secondaryMetrics": {"\u00b7gc.alloc.rate.norm": {"score": 640.0, "scoreError": 0.01}}}]
            with tempfile.TemporaryDirectory() as d:
                os.makedirs(os.path.join(d, "jmh"))
                with open(os.path.join(d, "params.tsv"), "w") as f:
                    f.write("key\tvalue\nrun_id\tx\nnull_experiment\tfalse\n")
                with open(os.path.join(d, "arms.tsv"), "w") as f:
                    f.write("label\tref\tcommit\tjar\nbaseline\tfc8b\t" + "a" * 40 + "\t/j\n")
                with open(os.path.join(d, "runs.tsv"), "w") as f:
                    f.write("run\trep\tarm\tclass\tstatus\tstarted\tjson\n")
                    f.write("1\t1\tbaseline\tRestJsonMarshallBenchmark\tok\tt\tjmh/a.json\n")
                    f.write("2\t1\tbaseline\tQueryMarshallBenchmark\tfailed\tt\tjmh/missing.json\n")
                with open(os.path.join(d, "jmh", "a.json"), "w") as f:
                    json.dump(jmh, f)
                built = collect(d)
            self.assertEqual(built["runs"][0]["results"], [{"benchmark": bench, "params": "testCaseId=S",
                                                            "score": 123.0, "score_error": 4.0, "alloc": 640.0,
                                                            "alloc_error": 0.01}])
            self.assertEqual(built["runs"][1]["results"], [])
            self.assertIn("1 of 2 JMH runs failed: rep1 QueryMarshallBenchmark baseline", render_summary(built))

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(SerdeAbSummaryTest)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
