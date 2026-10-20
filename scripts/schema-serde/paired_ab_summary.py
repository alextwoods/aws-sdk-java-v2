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
"""Summarizes a paired A/B run of scripts/schema-serde/paired-ab.sh (Requirements 34.9-34.11, 37.9, 37.10).

Ported from racecar's pipeline_benchmark2/analysis/scripts/paired_ab_summary.py (feature/poc/racecar). Python 3
standard library only.

Usage:
  paired_ab_summary.py collect RUNDIR
      Reads RUNDIR/arms.tsv, params.tsv, runs.tsv and runs/<n>.csv (written by paired-ab.sh) and writes
      RUNDIR/paired-ab.json, the stable input of every other command.
  paired_ab_summary.py summarize RUN_JSON [--null NULL_JSON]
      Prints a Markdown summary. For every client/scenario and metric: each arm's mean, error and repetitions,
      and for each later arm the paired ratio to the first arm (the baseline). A null-experiment run (the same
      build as both arms) reports its noise floor per scenario instead; --null applies such a floor to a real
      comparison and labels differences within it "no measurable difference".
  paired_ab_summary.py report RUN_JSON --null NULL_JSON --reference REF_JSON --notes NOTES_JSON --out OUT_MD
                        [--roles baseline=L,racecar=L,feature=L] [--cold-start LABEL=CSV ...]
                        [--metrics-run METRICS_JSON] [--preliminary] [--strict]
      Writes the performance report (docs/design/schema-serde/perf-report.md, task 9.18): per operation the
      baseline, racecar and feature means with error and repetitions, the ratio, the floor and the pass/fail of
      Requirements 34.3/34.4/34.10/34.11 and 37.9; marshalling vs signing for request bodies >= 100 KB (34.2, from
      --metrics-run if given: a separate paired-ab.sh --metrics run, since the publisher perturbs timing); the
      recorded reference arms (perf-reference.json) with the vs-bridged difference and its expected-cause note
      (perf-notes.json, 37.10); cold start (34.7); and the RuntimeCodegenMode deferral (34.8, OD-6). Roles default
      to baseline = first arm, feature = last arm, racecar = an arm labelled "racecar". --strict exits 1 on any
      warning (a failed or undecidable criterion, or a missing note).
  paired_ab_summary.py --self-test
      Runs the unit tests below against fixture data.

Statistics:
  mean   the mean over repetitions of the per-run value.
  error  the 99.9% confidence half-width of that mean (Student t, as JMH reports "Error"); empty for one repetition.
  ratio  the mean of the per-repetition ratios arm/baseline. Both arms run back to back inside a repetition, so the
         per-repetition ratio is more stable than either arm's absolute number (racecar's pairing argument).
  spread the standard deviation of those ratios, in percent.
  floor  from a null experiment: max(|ratio - 1|, spread), in percent, per client/scenario and metric.

The paired-ab.json format (schema_version 1):
  {"schema_version": 1, "params": {key: value, ...}, "arms": [{"label", "ref", "commit", "sdk_version", "jar"}],
   "runs": [{"run", "rep", "arm", "client", "scenario", "status", "started", "result": {csv column: value}}]}
"""
import csv
import json
import math
import os
import statistics
import sys
from collections import OrderedDict

SCHEMA_VERSION = 1

# (results column, label). Application CPU leads, as in racecar: process CPU includes compiler, VM and GC threads.
METRICS = [
    ("app_cpu_us_per_op", "application CPU µs/op"),
    ("avg_us_per_op", "wall µs/op (1/throughput)"),
    ("mean_lat_us", "mean latency µs"),
    ("alloc_bytes_per_op", "allocated B/op (client JVM)"),
    ("cpu_us_per_op", "process CPU µs/op (includes JIT/GC/VM)"),
]

# Two-sided 99.9% Student t quantiles (t_0.9995) by degrees of freedom; JMH's "Error" uses the same level.
_T_999 = [636.619, 31.599, 12.924, 8.610, 6.869, 5.959, 5.408, 5.041, 4.781, 4.587, 4.437, 4.318, 4.221, 4.140,
          4.073, 4.015, 3.965, 3.922, 3.883, 3.850, 3.819, 3.792, 3.768, 3.745, 3.725, 3.707, 3.690, 3.674, 3.659,
          3.646]


def t_999(df):
    if df < 1:
        raise ValueError("degrees of freedom must be at least 1")
    if df <= len(_T_999):
        return _T_999[df - 1]
    return statistics.NormalDist().inv_cdf(0.9995)


# ==================== collect ====================

def _read_tsv(path):
    with open(path, newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def _number(value):
    if value is None or value == "":
        return None
    try:
        number = float(value)
    except ValueError:
        return value
    return int(number) if number.is_integer() and "." not in value else number


def collect(rundir):
    """Builds the paired-ab.json document from the files paired-ab.sh wrote under rundir."""
    params = {row["key"]: row["value"] for row in _read_tsv(os.path.join(rundir, "params.tsv"))}
    arms = _read_tsv(os.path.join(rundir, "arms.tsv"))
    runs = []
    for row in _read_tsv(os.path.join(rundir, "runs.tsv")):
        result_path = os.path.join(rundir, "runs", row["run"] + ".csv")
        result = None
        if row["status"] == "ok" and os.path.exists(result_path):
            with open(result_path, newline="") as f:
                rows = list(csv.DictReader(f))
            if len(rows) != 1:
                raise SystemExit(f"{result_path}: expected one results row, found {len(rows)}")
            result = {k: _number(v) for k, v in rows[0].items()}
        runs.append({"run": int(row["run"]), "rep": int(row["rep"]), "arm": row["arm"], "client": row["client"],
                     "scenario": row["scenario"], "status": row["status"] if result else "failed",
                     "started": row["started"], "result": result})
    return {"schema_version": SCHEMA_VERSION, "params": params, "arms": arms, "runs": runs}


# ==================== analysis ====================

def load(path):
    with open(path) as f:
        doc = json.load(f)
    if doc.get("schema_version") != SCHEMA_VERSION:
        raise SystemExit(f"{path}: unsupported schema_version {doc.get('schema_version')!r}")
    return doc


def is_null_experiment(doc):
    return str(doc["params"].get("null_experiment", "false")).lower() == "true"


def arm_labels(doc):
    return [a["label"] for a in doc["arms"]]


def cases(doc):
    """(client, scenario) pairs in run order."""
    return list(OrderedDict.fromkeys((r["client"], r["scenario"]) for r in doc["runs"]))


def values_by_rep(doc, arm, case, metric):
    """{rep: value} for successful runs of one arm and case that report the metric."""
    out = {}
    for r in doc["runs"]:
        if r["arm"] == arm and (r["client"], r["scenario"]) == case and r["status"] == "ok":
            value = r["result"].get(metric)
            if isinstance(value, (int, float)):
                out[r["rep"]] = float(value)
    return out


def arm_stats(values):
    """Mean, 99.9% error and repetitions of a list of per-run values."""
    n = len(values)
    if n == 0:
        return None
    mean = statistics.fmean(values)
    error = t_999(n - 1) * statistics.stdev(values) / math.sqrt(n) if n > 1 else None
    return {"mean": mean, "error": error, "n": n}


def paired(base, cand):
    """Paired comparison of two {rep: value} maps over the repetitions both arms completed."""
    reps = sorted(set(base) & set(cand))
    ratios = [cand[k] / base[k] for k in reps if base[k]]
    if not ratios:
        return None
    ratio = statistics.fmean(ratios)
    spread = statistics.stdev(ratios) * 100 if len(ratios) > 1 else 0.0
    return {"ratio": ratio, "delta_pct": (ratio - 1.0) * 100, "spread_pct": spread, "pairs": len(ratios),
            "lower": sum(1 for x in ratios if x < 1.0)}


def floor_pct(comparison):
    """The noise floor a null-experiment comparison implies: max(|ratio - 1|, spread), in percent."""
    return max(abs(comparison["delta_pct"]), comparison["spread_pct"])


def analyze(doc, null_doc=None):
    """Per case and metric: arm stats, comparisons to the baseline (first) arm and, where known, the floor.

    The returned structure is what the Markdown renderers below (and 9.18's perf report) read:
      {(client, scenario): {metric: {"arms": {label: stats}, "vs_base": {label: comparison}, "floor_pct": float}}}
    """
    labels = arm_labels(doc)
    base = labels[0]
    null_floors = floors(null_doc) if null_doc else {}
    out = OrderedDict()
    for case in cases(doc):
        per_metric = OrderedDict()
        for metric, _ in METRICS:
            values = {a: values_by_rep(doc, a, case, metric) for a in labels}
            if not any(values.values()):
                continue
            entry = {"arms": {a: arm_stats(list(v.values())) for a, v in values.items()}, "vs_base": {}}
            for a in labels[1:]:
                entry["vs_base"][a] = paired(values[base], values[a])
            if is_null_experiment(doc) and entry["vs_base"].get(labels[1]):
                entry["floor_pct"] = floor_pct(entry["vs_base"][labels[1]])
            elif (case, metric) in null_floors:
                entry["floor_pct"] = null_floors[(case, metric)]
            per_metric[metric] = entry
        out[case] = per_metric
    return out


def floors(null_doc):
    """{((client, scenario), metric): floor_pct} from a null-experiment document."""
    if not is_null_experiment(null_doc):
        raise SystemExit("--null needs a null-experiment run (paired-ab.sh --null)")
    out = {}
    for case, per_metric in analyze(null_doc).items():
        for metric, entry in per_metric.items():
            if "floor_pct" in entry:
                out[(case, metric)] = entry["floor_pct"]
    return out


def pipelines(doc):
    """{arm: set of pipelines its runs reported}."""
    out = OrderedDict((a, set()) for a in arm_labels(doc))
    for r in doc["runs"]:
        if r["status"] == "ok":
            out[r["arm"]].add(str(r["result"].get("pipeline", "unknown")))
    return out


def problems(doc):
    """Reasons the comparison is not clean. A null experiment must run one pipeline and one harness build."""
    found = []
    harness = {str(r["result"].get("commit")) for r in doc["runs"] if r["status"] == "ok"}
    if len(harness) > 1:
        found.append(f"arms were built from different harness commits ({', '.join(sorted(harness))})")
    failed = [r for r in doc["runs"] if r["status"] != "ok"]
    if failed:
        found.append(f"{len(failed)} of {len(doc['runs'])} runs failed")
    if is_null_experiment(doc):
        seen = {frozenset(p) for p in pipelines(doc).values()}
        if len(seen) > 1:
            found.append("the null-experiment arms ran different request pipelines")
    return found


# ==================== Markdown ====================

def _fmt(value, digits=1):
    return "" if value is None else f"{value:,.{digits}f}"


def render_summary(doc, null_doc=None):
    labels = arm_labels(doc)
    params = doc["params"]
    lines = [f"# Paired A/B summary `{params.get('run_id', '?')}`", ""]
    kind = "null experiment (the same build as both arms)" if is_null_experiment(doc) else "comparison"
    lines += [f"Kind: {kind}. Baseline arm: `{labels[0]}`.", ""]
    lines += ["| arm | ref | commit | SDK version | pipeline |", "|---|---|---|---|---|"]
    pipes = pipelines(doc)
    for a in doc["arms"]:
        lines.append(f"| `{a['label']}` | `{a['ref']}` | `{a['commit'][:12]}` | {a['sdk_version']} "
                     f"| {', '.join(sorted(pipes.get(a['label'], ()))) or '-'} |")
    lines += ["", f"Parameters: iterations {params.get('iterations')}, warmup {params.get('warmup')}, "
              f"reps {params.get('reps')}, concurrency {params.get('concurrency')}, "
              f"JVM options `{params.get('jvm_args', '')}`, {params.get('java', '?')}, {params.get('host', '?')}.", ""]
    for p in problems(doc):
        lines += [f"**WARNING: {p}.**", ""]
    analysis = analyze(doc, null_doc)
    for metric, label in METRICS:
        rows = [(case, per[metric]) for case, per in analysis.items() if metric in per]
        if not rows:
            continue
        lines += [f"## {label}", ""]
        header = "| client | scenario | " + " | ".join(f"{a} mean ± error (n)" for a in labels)
        divider = "|---|---|" + "---:|" * len(labels)
        for a in labels[1:]:
            header += f" | {a} ratio | spread | lower/pairs"
            divider += "---:|---:|---:|"
        header += " | floor | verdict |"
        divider += "---:|---|"
        lines += [header, divider]
        for (client, scenario), entry in rows:
            cells = []
            for a in labels:
                s = entry["arms"][a]
                cells.append("-" if s is None else f"{_fmt(s['mean'])} ± {_fmt(s['error']) or '-'} ({s['n']})")
            verdicts = []
            for a in labels[1:]:
                c = entry["vs_base"][a]
                if c is None:
                    cells += ["-", "-", "-"]
                    continue
                cells += [f"{c['ratio']:.3f}", f"±{c['spread_pct']:.1f}%", f"{c['lower']}/{c['pairs']}"]
                verdicts.append(verdict(c, entry.get("floor_pct"), is_null_experiment(doc)))
            floor = entry.get("floor_pct")
            cells += ["-" if floor is None else f"{floor:.1f}%", "; ".join(v for v in verdicts if v)]
            lines.append(f"| {client} | {scenario} | " + " | ".join(cells) + " |")
        lines.append("")
    return "\n".join(lines)


def verdict(comparison, floor, null_experiment):
    if null_experiment:
        return "noise floor"
    if floor is None:
        return "no floor (run a null experiment)"
    if abs(comparison["delta_pct"]) <= floor:
        return "no measurable difference"
    return f"{comparison['delta_pct']:+.1f}% (beyond the floor)"


# ==================== perf report (task 9.18) ====================

TIME_METRIC = "app_cpu_us_per_op"
BYTES_METRIC = "alloc_bytes_per_op"
TIME_LIMIT = 1.05          # Requirement 34.4: feature mean <= 1.05 x Baseline_Build mean
INCONCLUSIVE_FLOOR = 5.0   # Requirement 34.11: a µs floor above 5% makes criterion 4 inconclusive
NOTE_THRESHOLD = 5.0       # Requirement 37.10: a cause for every metric more than 5% from the bridged build
LARGE_BODY_BYTES = 100 * 1024  # Requirement 34.2: report marshalling apart from payload hashing at >= 100 KB
REFERENCE_ARMS = [("baseline", "Stock 2.54 (rec.)"), ("racecar", "Racecar (rec.)"), ("bridged", "Bridged (rec.)"),
                  ("smithy-java", "smithy-java (rec.)")]


def time_verdict(base_mean, feat_mean, floor):
    """Criterion 4 (34.4, 34.10, 34.11) for one operation, from the means over all repetitions.

    A ratio above 1.05 is a regression whatever the floor (34.10). Otherwise a floor above 5% makes the result
    inconclusive (34.11), a difference within the floor is "no measurable difference" (34.10), and anything else
    passes. Without a floor nothing can be decided.
    """
    ratio = feat_mean / base_mean
    if floor is None:
        return "NO FLOOR", ratio
    if ratio > TIME_LIMIT:
        return f"FAIL: regression, ratio {ratio:.3f}", ratio
    if floor > INCONCLUSIVE_FLOOR:
        return f"INCONCLUSIVE: floor {floor:.1f}% > 5%", ratio
    if abs(ratio - 1.0) * 100 <= floor:
        return "PASS: no measurable difference", ratio
    return "PASS", ratio


def bytes_verdict(base_mean, feat_mean, floor):
    """Criterion 3 (34.3): feature B/op <= Baseline_Build B/op + the operation's floor (floor in % of baseline)."""
    if floor is None:
        return "NO FLOOR"
    floor_bytes = base_mean * floor / 100.0
    if feat_mean > base_mean + floor_bytes:
        return f"FAIL: regression, {feat_mean - base_mean:+,.0f} B/op > floor {floor_bytes:,.0f}"
    if abs(feat_mean - base_mean) <= floor_bytes:
        return "PASS: no measurable difference"
    return "PASS"


def racecar_verdict(racecar_mean, feat_mean):
    """Requirement 37.9: feature B/op <= the racecar final build's B/op, measured in the same run (no floor)."""
    if racecar_mean is None:
        return "NOT RUN (no racecar arm)"
    if feat_mean <= racecar_mean:
        return "PASS"
    return f"FAIL: {feat_mean - racecar_mean:+,.0f} B/op over racecar"


def vs_bridged_pct(feat_mean, base_mean, ref):
    """The feature build against the recorded bridged build, normalized through the Baseline_Build.

    The reference numbers were recorded on another host and JDK (racecar's c6g.metal, JDK 25), so the feature mean
    is first scaled by recorded-baseline / measured-baseline. None when either reference number is missing.
    """
    bridged = (ref or {}).get("bridged")
    recorded_base = (ref or {}).get("baseline")
    if bridged is None or recorded_base is None or not base_mean:
        return None
    return (feat_mean / base_mean * recorded_base / bridged - 1.0) * 100


def note_for(notes, scenario, metric):
    return notes.get(f"{scenario}/{metric}")


def default_roles(doc):
    """baseline = the first arm, feature = the last, racecar = an arm labelled "racecar" if there is one."""
    labels = arm_labels(doc)
    roles = {"baseline": labels[0], "feature": labels[-1]}
    if "racecar" in labels:
        roles["racecar"] = "racecar"
    return roles


def _role_labels(doc, roles):
    labels = arm_labels(doc)
    out = {}
    for role, label in roles.items():
        if label not in labels:
            raise SystemExit(f"--roles: arm {label!r} ({role}) is not in the run (arms: {', '.join(labels)})")
        out[role] = label
    if "baseline" not in out or "feature" not in out:
        raise SystemExit("--roles needs baseline=LABEL and feature=LABEL")
    return out


def _ref_value(reference, scenario, metric, arm):
    entry = reference.get("scenarios", {}).get(scenario, {}).get(metric, {}).get(arm)
    return None if entry is None else entry["mean"]


def _mean_err(stats, digits=1):
    if stats is None:
        return "-"
    return f"{_fmt(stats['mean'], digits)} ± {_fmt(stats['error'], digits) or '-'} ({stats['n']})"


def cold_start_stats(rows, column):
    values = [float(r[column]) for r in rows if r.get(column) not in (None, "")]
    return arm_stats(values)


def read_cold_start(path):
    with open(path, newline="") as f:
        return list(csv.DictReader(f))


def render_report(doc, null_doc, roles, reference, notes, cold_starts=(), preliminary=False, metrics_doc=None):
    """Markdown for docs/design/schema-serde/perf-report.md, and the warnings it raised.

    doc/null_doc: paired-ab.json documents (the comparison and its null experiment). roles: {"baseline", "feature",
    optionally "racecar"} -> arm label. reference: perf-reference.json. notes: perf-notes.json "notes".
    cold_starts: [(label, rows)] from ClientColdStartBenchmark CSVs, the first being the baseline. metrics_doc: a
    separate --metrics run for the marshalling/signing split (default: doc); its arms are matched by role label.
    """
    roles = _role_labels(doc, roles)
    warnings = list(problems(doc))
    if null_doc is None:
        warnings.append("no null experiment: every floor is missing, so no criterion can pass")
    analysis = analyze(doc, null_doc)
    params = doc["params"]
    lines = ["# Schema-serde performance report", ""]
    if preliminary:
        lines += ["> **PRELIMINARY — smoke data, not measurements.** Tiny operation counts on an unpinned "
                  "development machine with the JIT still compiling. The verdicts below show that the tooling "
                  "applies the rules; they say nothing about the build. Replace this file with a full run on a "
                  "quiet host (see `porting-deviations.md`, task 9.18).", ""]
    lines += ["Generated by `scripts/schema-serde/paired_ab_summary.py report` (task 9.18; Requirements 34.2–34.11, "
              "37.9, 37.10; design D.3, D.6).", "",
              "## Run", "",
              f"- Comparison run `{params.get('run_id', '?')}`; null experiment "
              f"`{null_doc['params'].get('run_id', '?') if null_doc else 'none'}`",
              f"- Host: {params.get('host', '?')} ({params.get('hardware', '?')}); JDK: {params.get('java', '?')}. "
              "Every measured arm ran on this host and JDK (Requirement 34.5)",
              f"- Iterations {params.get('iterations')}, warmup {params.get('warmup')}, repetitions "
              f"{params.get('reps')}, concurrency {params.get('concurrency')}, metrics {params.get('metrics')}, "
              f"JVM options `{params.get('jvm_args', '')}`", ""]
    lines += ["| role | arm | ref | commit | SDK version |", "|---|---|---|---|---|"]
    by_label = {a["label"]: a for a in doc["arms"]}
    if by_label[roles["baseline"]]["jar"] == by_label[roles["feature"]]["jar"]:
        warnings.append("the baseline and feature roles are the same build, so every ratio is noise and the "
                        "normalized feature column equals the recorded baseline by construction")
    for role, label in roles.items():
        a = by_label[label]
        lines.append(f"| {role} | `{label}` | `{a['ref']}` | `{a['commit'][:12]}` | {a['sdk_version']} |")
    lines += ["",
              "## Rules", "",
              "- Time is application CPU µs/op (`app_cpu_us_per_op`); memory is client-JVM B/op "
              "(`alloc_bytes_per_op`). Means are over all repetitions; error is the 99.9% half-width; ratios are "
              "feature mean / Baseline_Build mean",
              "- Floor: the null experiment's max(|ratio − 1|, paired spread) for the operation and metric",
              "- Time (34.4): ratio > 1.05 fails as a regression; otherwise a floor > 5% is inconclusive (34.11); "
              "a difference within the floor is \"no measurable difference\" (34.10)",
              "- B/op (34.3): feature ≤ baseline + floor (floor as a share of the baseline mean)",
              "- B/op vs racecar (37.9): feature ≤ the racecar final build measured in the same run",
              "- Vs bridged (37.10): the recorded numbers come from racecar's run (another host and JDK), so the "
              "feature mean is scaled by recorded baseline / measured baseline first. Every difference above 5% "
              "needs an expected-cause note in `perf-notes.json`", ""]

    racecar_label = roles.get("racecar")
    for client in OrderedDict.fromkeys(c for c, _ in analysis):
        lines += [f"## E2E DynamoDB, `{client}` (Requirements 34.2–34.5, 37.9)", "",
                  "| scenario | metric | baseline | racecar | feature | ratio | floor | 34.3/34.4 | 37.9 |",
                  "|---|---|---:|---:|---:|---:|---:|---|---|"]
        for (c, scenario), per in analysis.items():
            if c != client:
                continue
            for metric in (TIME_METRIC, BYTES_METRIC):
                entry = per.get(metric)
                if entry is None:
                    continue
                base, feat = entry["arms"][roles["baseline"]], entry["arms"][roles["feature"]]
                race = entry["arms"].get(racecar_label) if racecar_label else None
                floor = entry.get("floor_pct")
                if base is None or feat is None:
                    lines.append(f"| {scenario} | {metric} | {_mean_err(base)} | {_mean_err(race)} | "
                                 f"{_mean_err(feat)} | - | - | MISSING ARM | - |")
                    continue
                if metric == TIME_METRIC:
                    v, ratio = time_verdict(base["mean"], feat["mean"], floor)
                    rv = "-"
                else:
                    ratio = feat["mean"] / base["mean"]
                    v = bytes_verdict(base["mean"], feat["mean"], floor)
                    rv = racecar_verdict(race["mean"] if race else None, feat["mean"])
                for verdict_text in (v, rv):
                    if verdict_text.startswith(("FAIL", "NO FLOOR", "INCONCLUSIVE")):
                        warnings.append(f"{client} {scenario} {metric}: {verdict_text}")
                d = 0 if metric == BYTES_METRIC else 1
                lines.append(f"| {scenario} | {metric} | {_mean_err(base, d)} | {_mean_err(race, d)} "
                             f"| {_mean_err(feat, d)} "
                             f"| {ratio:.3f} | {'-' if floor is None else f'{floor:.1f}%'} | {v} | {rv} |")
        lines.append("")

        # Marshalling apart from payload hashing for large request bodies (34.2).
        split_doc = metrics_doc or doc
        split_roles = {r: label for r, label in roles.items() if label in arm_labels(split_doc)}
        large = []
        for c, scenario in cases(split_doc):
            body = values_by_rep(split_doc, roles["feature"], (c, scenario), "request_body_bytes")
            if c == client and body and max(body.values()) >= LARGE_BODY_BYTES:
                large.append((scenario, int(max(body.values()))))
        lines += [f"### Marshalling vs signing for request bodies ≥ 100 KB, `{client}` (Requirement 34.2)", ""]
        if not large:
            lines += ["No scenario in this run had a request body of 100 KB or more with `--metrics` on. Run "
                      "`batch-put-large` with `paired-ab.sh --metrics`.", ""]
        else:
            lines += ["`MarshallingDuration` is request marshalling only; `SigningDuration` contains SigV4's payload "
                      "SHA-256. Both are means in µs from the SDK metric publisher.", "",
                      "| scenario | body bytes | arm | marshalling µs | signing µs |", "|---|---:|---|---:|---:|"]
            for scenario, body in large:
                for role, label in split_roles.items():
                    m = arm_stats(list(values_by_rep(split_doc, label, (client, scenario), "marshalling_us").values()))
                    s = arm_stats(list(values_by_rep(split_doc, label, (client, scenario), "signing_us").values()))
                    lines.append(f"| {scenario} | {body:,} | {role} | {_mean_err(m)} | {_mean_err(s)} |")
            lines.append("")

    # Recorded reference arms and the bridged comparison (34.5, 37.10).
    src = reference.get("source", {})
    lines += ["## Comparison with the recorded reference builds (Requirements 34.5, 37.10; design D.6)", "",
              f"Recorded columns: `{src.get('file', '?')}` on `{src.get('branch', '?')}` at `{src.get('commit', '?')}`"
              f" ({src.get('date', '?')}; {src.get('host', '?')}; {src.get('jdk', '?')}). smithy-java and the "
              "bridged build cannot be built by `paired-ab.sh`, so they appear only as recorded numbers.", "",
              "| scenario | metric | " + " | ".join(h for _, h in REFERENCE_ARMS)
              + " | feature (normalized) | vs bridged | expected cause (> 5%) |",
              "|---|---|" + "---:|" * (len(REFERENCE_ARMS) + 2) + "---|"]
    for (client, scenario), per in analysis.items():
        if client != "v2-sync":  # the recorded numbers are v2-sync (the bridge was sync only)
            continue
        for metric in (TIME_METRIC, BYTES_METRIC):
            entry = per.get(metric)
            ref = {arm: _ref_value(reference, scenario, metric, arm) for arm, _ in REFERENCE_ARMS}
            if entry is None or all(v is None for v in ref.values()):
                continue
            base, feat = entry["arms"][roles["baseline"]], entry["arms"][roles["feature"]]
            diff = vs_bridged_pct(feat["mean"], base["mean"], ref) if base and feat else None
            normalized = (feat["mean"] / base["mean"] * ref["baseline"]
                          if base and feat and ref.get("baseline") else None)
            note = note_for(notes, scenario, metric)
            if diff is not None and abs(diff) > NOTE_THRESHOLD and not note:
                note = "**MISSING NOTE**"
                warnings.append(f"{scenario} {metric}: {diff:+.1f}% vs bridged has no expected-cause note")
            digits = 0 if metric == BYTES_METRIC else 1
            cells = ["-" if ref[a] is None else _fmt(ref[a], digits) for a, _ in REFERENCE_ARMS]
            lines.append(f"| {scenario} | {metric} | " + " | ".join(cells)
                         + f" | {_fmt(normalized, digits) or '-'} | {'-' if diff is None else f'{diff:+.1f}%'} "
                         f"| {note or ''} |")
    lines.append("")

    # Cold start (34.7).
    lines += ["## Client construction and first call (Requirement 34.7)", ""]
    if not cold_starts:
        lines += ["No cold-start samples. Run `ClientColdStartBenchmark` with `benchmark.sh --main "
                  "ClientColdStartBenchmark --repeat 10` per arm and pass each CSV with `--cold-start LABEL=CSV`.", ""]
    else:
        lines += ["One fresh JVM per sample; DynamoDB `v2-sync` against the mock server.", "",
                  "| arm | build() ms | ratio | first call ms | ratio | samples |", "|---|---:|---:|---:|---:|---|"]
        base_build = cold_start_stats(cold_starts[0][1], "build_ms")
        base_first = cold_start_stats(cold_starts[0][1], "first_call_ms")
        for label, rows in cold_starts:
            b, f = cold_start_stats(rows, "build_ms"), cold_start_stats(rows, "first_call_ms")
            n = b["n"] if b else 0
            if n < 10:
                warnings.append(f"cold start {label}: {n} samples, fewer than the 10 Requirement 34.7 needs")
            lines.append(f"| `{label}` | {_mean_err(b, 2)} | "
                         f"{b['mean'] / base_build['mean']:.3f} | {_mean_err(f, 2)} | "
                         f"{f['mean'] / base_first['mean']:.3f} | {n}{'' if n >= 10 else ' (< 10)'} |")
        lines.append("")

    lines += ["## Runtime codec bytecode generation (`RuntimeCodegenMode`, Requirement 34.8, OD-6)", "",
              "Decision: **defer**. smithy-java's runtime codegen defines classes with `Lookup.defineHiddenClass` "
              "(JDK 15+) and emits them through a JDK 25 class-file path. A Java 8 version needs a custom "
              "`ClassLoader`-based definer and class-version-52 emission, which do not exist yet, so the Java 8 "
              "µs/op and B/op for the Requirement 34.1 operations could not be measured. Revisit after the phase 5 "
              "numbers are in (design D.3, OD-6).", ""]
    lines += ["## Warnings", ""]
    lines += [f"- {w}" for w in warnings] if warnings else ["None."]
    lines.append("")
    return "\n".join(lines), warnings


def report_main(argv):
    """report RUN_JSON --null NULL_JSON --reference REF --notes NOTES --out OUT [--roles ...] [--cold-start L=CSV]
    [--preliminary] [--strict]"""
    if not argv:
        raise SystemExit(__doc__)
    run_path, rest = argv[0], argv[1:]
    opts = {"--null": None, "--reference": None, "--notes": None, "--out": None, "--roles": None,
            "--metrics-run": None}
    cold = []
    flags = set()
    i = 0
    while i < len(rest):
        key = rest[i]
        if key in ("--preliminary", "--strict"):
            flags.add(key)
            i += 1
        elif key == "--cold-start":
            label, _, path = rest[i + 1].partition("=")
            cold.append((label, read_cold_start(path)))
            i += 2
        elif key in opts:
            opts[key] = rest[i + 1]
            i += 2
        else:
            raise SystemExit(f"unknown report option {key}\n{__doc__}")
    for required in ("--reference", "--notes", "--out"):
        if opts[required] is None:
            raise SystemExit(f"report needs {required}")
    doc = load(run_path)
    roles = (dict(item.split("=", 1) for item in opts["--roles"].split(",") if item) if opts["--roles"]
             else default_roles(doc))
    with open(opts["--reference"]) as f:
        reference = json.load(f)
    with open(opts["--notes"]) as f:
        notes = json.load(f)["notes"]
    text, warnings = render_report(doc, load(opts["--null"]) if opts["--null"] else None, roles,
                                   reference, notes, cold, "--preliminary" in flags,
                                   load(opts["--metrics-run"]) if opts["--metrics-run"] else None)
    with open(opts["--out"], "w") as f:
        f.write(text)
    print(f"Wrote {opts['--out']} ({len(warnings)} warnings)", file=sys.stderr)
    for w in warnings:
        print(f"WARNING: {w}", file=sys.stderr)
    return 1 if warnings and "--strict" in flags else 0


# ==================== CLI ====================

def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    if len(argv) == 2 and argv[0] == "collect":
        doc = collect(argv[1])
        path = os.path.join(argv[1], "paired-ab.json")
        with open(path, "w") as f:
            json.dump(doc, f, indent=1, sort_keys=False)
            f.write("\n")
        print(f"Wrote {path}", file=sys.stderr)
        return 0
    if len(argv) in (2, 4) and argv[0] == "summarize":
        null_doc = None
        if len(argv) == 4:
            if argv[2] != "--null":
                raise SystemExit(__doc__)
            null_doc = load(argv[3])
        print(render_summary(load(argv[1]), null_doc))
        return 0
    if argv[:1] == ["report"]:
        return report_main(argv[1:])
    raise SystemExit(__doc__)


# ==================== self-test ====================

def self_test():
    import tempfile
    import unittest

    def run(n, rep, arm, scenario, app_cpu, alloc, pipeline="schema", commit="h1", status="ok"):
        result = {"app_cpu_us_per_op": app_cpu, "avg_us_per_op": app_cpu * 1.5, "alloc_bytes_per_op": alloc,
                  "pipeline": pipeline, "commit": commit} if status == "ok" else None
        return {"run": n, "rep": rep, "arm": arm, "client": "v2-sync", "scenario": scenario, "status": status,
                "started": "t", "result": result}

    def doc(arms, runs, null=False):
        return {"schema_version": 1, "params": {"run_id": "fixture", "null_experiment": str(null).lower()},
                "arms": [{"label": a, "ref": "r", "commit": "c" * 40, "sdk_version": "v", "jar": "j"} for a in arms],
                "runs": runs}

    # Null experiment: per-rep ratios b/a = 1.02, 0.98, 1.00 -> ratio 1.0, spread 2.0%.
    null_doc = doc(["f-a", "f-b"], [
        run(1, 1, "f-a", "small-get", 100.0, 1000), run(2, 1, "f-b", "small-get", 102.0, 1000),
        run(3, 2, "f-b", "small-get", 98.0, 1000), run(4, 2, "f-a", "small-get", 100.0, 1000),
        run(5, 3, "f-a", "small-get", 100.0, 1000), run(6, 3, "f-b", "small-get", 100.0, 1000),
    ], null=True)
    # Comparison: candidate is 10% slower on CPU and allocates 0.5% more.
    ab_doc = doc(["base", "cand"], [
        run(1, 1, "base", "small-get", 100.0, 1000), run(2, 1, "cand", "small-get", 110.0, 1005),
        run(3, 2, "cand", "small-get", 110.0, 1005), run(4, 2, "base", "small-get", 100.0, 1000),
    ])

    class PairedAbSummaryTest(unittest.TestCase):
        def test_armStats_whenTwoValues_reportsMeanAnd999Error(self):
            s = arm_stats([10.0, 12.0])
            self.assertAlmostEqual(s["mean"], 11.0)
            self.assertAlmostEqual(s["error"], 636.619 * math.sqrt(2) / math.sqrt(2))
            self.assertEqual(s["n"], 2)
            self.assertIsNone(arm_stats([5.0])["error"])

        def test_analyze_whenNullExperiment_reportsFloorFromPairedSpread(self):
            entry = analyze(null_doc)[("v2-sync", "small-get")]["app_cpu_us_per_op"]
            self.assertAlmostEqual(entry["vs_base"]["f-b"]["ratio"], 1.0)
            self.assertAlmostEqual(entry["floor_pct"], 2.0)
            self.assertEqual(entry["arms"]["f-a"]["n"], 3)
            # Identical allocation in both arms: zero floor.
            self.assertAlmostEqual(analyze(null_doc)[("v2-sync", "small-get")]["alloc_bytes_per_op"]["floor_pct"],
                                   0.0)

        def test_verdict_whenNullFloorApplied_separatesNoiseFromChange(self):
            entry = analyze(ab_doc, null_doc)[("v2-sync", "small-get")]
            cpu = entry["app_cpu_us_per_op"]
            self.assertAlmostEqual(cpu["vs_base"]["cand"]["ratio"], 1.1)
            self.assertEqual(verdict(cpu["vs_base"]["cand"], cpu["floor_pct"], False), "+10.0% (beyond the floor)")
            alloc = entry["alloc_bytes_per_op"]
            self.assertEqual(verdict(alloc["vs_base"]["cand"], 1.0, False), "no measurable difference")
            self.assertIn("no floor", verdict(alloc["vs_base"]["cand"], None, False))

        def test_problems_whenNullArmsRanDifferentPipelines_flagsIt(self):
            mixed = doc(["f-a", "f-b"], [run(1, 1, "f-a", "small-get", 1.0, 1, pipeline="stock"),
                                         run(2, 1, "f-b", "small-get", 1.0, 1, pipeline="schema"),
                                         run(3, 2, "f-b", "small-get", 1.0, 1, status="failed")], null=True)
            found = problems(mixed)
            self.assertTrue(any("different request pipelines" in p for p in found))
            self.assertTrue(any("1 of 3 runs failed" in p for p in found))
            self.assertEqual(problems(null_doc), [])

        def test_collect_whenRunFilesWritten_buildsDocumentAndSummary(self):
            with tempfile.TemporaryDirectory() as d:
                os.makedirs(os.path.join(d, "runs"))
                with open(os.path.join(d, "params.tsv"), "w") as f:
                    f.write("key\tvalue\nrun_id\tx\nnull_experiment\ttrue\njvm_args\t-Xmx2g\n")
                with open(os.path.join(d, "arms.tsv"), "w") as f:
                    f.write("label\tref\tcommit\tsdk_version\tjar\n")
                    f.write("f-a\tHEAD\t" + "a" * 40 + "\t2.49.7-SNAPSHOT\t/j\n")
                    f.write("f-b\tHEAD\t" + "a" * 40 + "\t2.49.7-SNAPSHOT\t/j\n")
                with open(os.path.join(d, "runs.tsv"), "w") as f:
                    f.write("run\trep\tarm\tclient\tscenario\tstatus\tstarted\n")
                    f.write("1\t1\tf-a\tv2-sync\tsmall-get\tok\tt\n2\t1\tf-b\tv2-sync\tsmall-get\tok\tt\n")
                for n, cpu in ((1, "100.0"), (2, "101.0")):
                    with open(os.path.join(d, "runs", f"{n}.csv"), "w") as f:
                        f.write("client,scenario,app_cpu_us_per_op,alloc_bytes_per_op,pipeline,commit\n")
                        f.write(f"v2-sync,small-get,{cpu},72044,schema,abc\n")
                built = collect(d)
            self.assertEqual(built["runs"][1]["result"]["app_cpu_us_per_op"], 101.0)
            self.assertEqual(built["runs"][1]["result"]["alloc_bytes_per_op"], 72044)
            text = render_summary(built)
            self.assertIn("null experiment", text)
            self.assertIn("| v2-sync | small-get | 100.0 ± - (1) | 101.0 ± - (1) | 1.010 |", text)

        # ---- perf report (task 9.18) ----
        def test_timeVerdict_appliesThresholdFloorAndInconclusive(self):
            self.assertEqual(time_verdict(100.0, 105.0, 2.0), ("PASS", 1.05))
            self.assertTrue(time_verdict(100.0, 105.1, 2.0)[0].startswith("FAIL: regression, ratio 1.051"))
            self.assertEqual(time_verdict(100.0, 101.0, 2.0)[0], "PASS: no measurable difference")
            self.assertEqual(time_verdict(100.0, 99.0, 6.0)[0], "INCONCLUSIVE: floor 6.0% > 5%")
            # Above 1.05 is a regression even when the floor is wide (34.10 before 34.11).
            self.assertTrue(time_verdict(100.0, 106.0, 8.0)[0].startswith("FAIL"))
            self.assertEqual(time_verdict(100.0, 90.0, None)[0], "NO FLOOR")

        def test_bytesVerdicts_applyBaselineFloorAndRacecarLimit(self):
            self.assertEqual(bytes_verdict(1000.0, 1010.0, 1.0), "PASS: no measurable difference")
            self.assertTrue(bytes_verdict(1000.0, 1011.0, 1.0).startswith("FAIL: regression, +11 B/op > floor 10"))
            self.assertEqual(bytes_verdict(1000.0, 900.0, 1.0), "PASS")
            self.assertEqual(bytes_verdict(1000.0, 900.0, None), "NO FLOOR")
            self.assertEqual(racecar_verdict(900.0, 900.0), "PASS")
            self.assertTrue(racecar_verdict(900.0, 901.0).startswith("FAIL: +1 B/op over racecar"))
            self.assertTrue(racecar_verdict(None, 1.0).startswith("NOT RUN"))

        def test_vsBridged_normalizesThroughTheBaseline(self):
            # Measured feature is half the measured baseline; recorded baseline 200, bridged 90 -> 100/90 - 1.
            self.assertAlmostEqual(vs_bridged_pct(50.0, 100.0, {"baseline": 200.0, "bridged": 90.0}), 100 / 0.9 - 100)
            self.assertIsNone(vs_bridged_pct(50.0, 100.0, {"baseline": 200.0}))

        def test_renderReport_whenNoteMissing_warnsAndMarksIt(self):
            reference = {"source": {"file": "f"}, "scenarios": {"small-get": {
                "app_cpu_us_per_op": {"baseline": {"mean": 100.0}, "bridged": {"mean": 80.0}},
                "alloc_bytes_per_op": {"baseline": {"mean": 1000.0}, "bridged": {"mean": 1004.0}}}}}
            text, warnings = render_report(ab_doc, null_doc, {"baseline": "base", "feature": "cand"}, reference,
                                           {}, preliminary=True)
            self.assertIn("PRELIMINARY — smoke data, not measurements", text)
            self.assertIn("RuntimeCodegenMode", text)
            self.assertIn("OD-6", text)
            self.assertIn("**MISSING NOTE**", text)
            # CPU: 110/100 * 100/80 - 1 = +37.5% needs a note; B/op: 1005/1000 * 1000/1004 - 1 = +0.1% does not.
            self.assertTrue(any("app_cpu_us_per_op: +37.5% vs bridged has no expected-cause note" in w
                                for w in warnings))
            self.assertFalse(any("alloc_bytes_per_op" in w and "note" in w for w in warnings))
            self.assertTrue(any("FAIL: regression, ratio 1.100" in w for w in warnings))
            noted, warnings = render_report(ab_doc, null_doc, {"baseline": "base", "feature": "cand"}, reference,
                                            {"small-get/app_cpu_us_per_op": "expected cause"})
            self.assertIn("| expected cause |", noted)
            self.assertNotIn("PRELIMINARY", noted)
            self.assertFalse(any("note" in w for w in warnings))

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(PairedAbSummaryTest)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
