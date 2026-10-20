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

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(PairedAbSummaryTest)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
