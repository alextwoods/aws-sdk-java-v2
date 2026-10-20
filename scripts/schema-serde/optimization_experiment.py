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
"""Verdict and record of one racecar optimization experiment (task 9.19; Requirements 37.1, 37.9).

Run by scripts/schema-serde/optimization_experiment.sh. Python 3 standard library only; reuses the statistics of
paired_ab_summary.py (same directory).

Usage:
  optimization_experiment.py plan ID [--targets TARGETS_JSON] [--yaml DECISIONS_YAML]
      Checks that ID is a racecar id in the decision YAML and in the targets mapping, and prints the plan as
      tab-separated lines for the shell: "decision", "task", "jmh_module", "jmh_include" and one "jmh_param" line
      per JMH -p value (no jmh_* lines when the id has no JMH benchmarks).
  optimization_experiment.py verdict ID --null NULL_JSON --ab AB_JSON --out RECORD_MD
                        [--jmh-null-a JSON --jmh-null-b JSON --jmh-candidate JSON]
                        [--feature-ref REF --candidate-ref REF] [--targets TARGETS_JSON] [--yaml DECISIONS_YAML]
                        [--preliminary]
      Applies the keep rule to the e2e runs (paired-ab.json of the null experiment and of the A/B) and, if given,
      the JMH runs (-rf json results of the feature jar, the candidate jar and the feature jar again), writes the
      experiment record and prints the YAML change for a human to apply. Exit status 0 for KEEP, 3 for REVERT.
  optimization_experiment.py --self-test
      Runs the verdict-rule tests on fixture data.

Keep rule (lower is better for every metric):
  target  a (case, metric) the targets mapping names for ID. It gains when its change is below -floor.
  guard   every other measured (case, metric) of the guard metrics. It regresses when its change is above +floor.
  KEEP    iff at least one target was measured, every measured target gains, no guard regresses, every measured
          (case, metric) has a floor, and neither run reports a problem (failed runs, mixed harness commits).
  Floors: e2e from the null experiment (paired_ab_summary.floor_pct: max(|ratio - 1|, spread of pairs)); JMH from
  the feature jar run twice: max(|b/a - 1|, each run's relative 99.9% error). A JMH B/op baseline below 1 B/op is
  compared absolutely: the candidate regresses if it allocates 1 B/op or more.
"""
import json
import math
import os
import re
import sys
from collections import OrderedDict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import paired_ab_summary as pab  # noqa: E402
import racecar_decisions as rd  # noqa: E402

REPO_ROOT = os.path.normpath(os.path.join(SCRIPT_DIR, "..", ".."))
DEFAULT_TARGETS = os.path.join(REPO_ROOT, "docs", "design", "schema-serde", "optimizations", "targets.json")
DEFAULT_YAML = rd.DEFAULT_YAML
FEATURE = "feature"
CANDIDATE = "candidate"
WIDE_FLOOR = pab.INCONCLUSIVE_FLOOR  # a target floor above this is too wide to reject on
ZERO_ALLOC = 1.0                     # B/op below this counts as no allocation (ZeroAllocationBenchmarkTest)
KEEP, REVERT = "KEEP", "REVERT"


class ExperimentError(Exception):
    pass


# ==================== inputs ====================

def load_targets(path, racecar_id):
    with open(path) as f:
        doc = json.load(f)
    if racecar_id not in doc["targets"]:
        raise ExperimentError(f"{racecar_id!r} has no entry in {path} (known: {', '.join(doc['targets'])})")
    return doc["targets"][racecar_id], doc["guard_metrics"]


def decision_entry(yaml_path, racecar_id):
    for entry in rd.parse(yaml_path):
        if entry.get("id") == racecar_id:
            return entry
    raise ExperimentError(f"{racecar_id!r} is not a racecar id in {yaml_path}")


def _num(value):
    try:
        number = float(value)
    except (TypeError, ValueError):
        return None
    return None if math.isnan(number) else number


def load_jmh(path):
    """{(benchmark, params): {metric: (score, error)}} from a JMH -rf json file; metric "score" is the primary."""
    with open(path) as f:
        rows = json.load(f)
    out = OrderedDict()
    for row in rows:
        params = ",".join(f"{k}={v}" for k, v in sorted((row.get("params") or {}).items()))
        metrics = {"score": (_num(row["primaryMetric"]["score"]), _num(row["primaryMetric"].get("scoreError")))}
        for name, m in (row.get("secondaryMetrics") or {}).items():
            metrics[name.lstrip("\u00b7")] = (_num(m["score"]), _num(m.get("scoreError")))
        out[(row["benchmark"], params)] = metrics
    return out


# ==================== measurements ====================

def _item(source, case, metric, base, cand, delta, floor, target):
    return {"source": source, "case": case, "metric": metric, "feature": base, "candidate": cand,
            "delta_pct": delta, "floor_pct": floor, "target": target}


def e2e_is_target(spec, client, scenario, metric):
    for t in spec.get("e2e") or []:
        if (scenario in t["scenarios"] and metric in t["metrics"]
                and (not t.get("clients") or client in t["clients"])):
            return True
    return False


def e2e_items(spec, guards, null_doc, ab_doc):
    labels = pab.arm_labels(ab_doc)
    if labels[:2] != [FEATURE, CANDIDATE]:
        raise ExperimentError(f"A/B arms must be [{FEATURE}, {CANDIDATE}], got {labels}")
    items = []
    for (client, scenario), per_metric in pab.analyze(ab_doc, null_doc).items():
        for metric, entry in per_metric.items():
            target = e2e_is_target(spec, client, scenario, metric)
            if not target and metric not in guards:
                continue
            comparison = entry["vs_base"].get(CANDIDATE)
            base, cand = entry["arms"].get(FEATURE), entry["arms"].get(CANDIDATE)
            items.append(_item("e2e", f"{client} {scenario}", metric, base and base["mean"], cand and cand["mean"],
                               comparison and comparison["delta_pct"], entry.get("floor_pct"), target))
    return items


def jmh_is_target(spec, benchmark, metric):
    jmh = spec.get("jmh") or {}
    return any(re.search(t["benchmark"], benchmark) and metric in t["metrics"] for t in jmh.get("targets", []))


def _rel_err(score, error):
    return 0.0 if not score or error is None else abs(error / score) * 100


def jmh_items(spec, guards, null_a, null_b, candidate):
    items = []
    for key, metrics in null_a.items():
        benchmark, params = key
        for metric, (a, a_err) in metrics.items():
            target = jmh_is_target(spec, benchmark, metric)
            if not target and metric not in guards:
                continue
            b, b_err = null_b.get(key, {}).get(metric, (None, None))
            c = candidate.get(key, {}).get(metric, (None, None))[0]
            floor = delta = None
            if a is not None and b is not None:
                if metric == "gc.alloc.rate.norm" and a < ZERO_ALLOC:
                    floor = 0.0
                elif a:
                    floor = max(abs(b / a - 1) * 100, _rel_err(a, a_err), _rel_err(b, b_err))
            if a is not None and c is not None:
                if metric == "gc.alloc.rate.norm" and a < ZERO_ALLOC:
                    delta = 0.0 if c < ZERO_ALLOC else math.inf
                elif a:
                    delta = (c / a - 1) * 100
            case = benchmark.split("benchmark.", 1)[-1] + (f" [{params}]" if params else "")
            items.append(_item("jmh", case, metric, a, c, delta, floor, target))
    return items


# ==================== verdict ====================

def outcome(item):
    if item["delta_pct"] is None:
        return "not measured"
    if item["floor_pct"] is None:
        return "no floor"
    if item["target"]:
        return "gain" if item["delta_pct"] < -item["floor_pct"] else "no gain"
    return "regression" if item["delta_pct"] > item["floor_pct"] else "ok"


def decide(items, problems=()):
    """(verdict, reasons, suggested decision) for a list of measurement items."""
    targets = [i for i in items if i["target"]]
    reasons = [f"run problem: {p}" for p in problems]
    if not targets:
        raise ExperimentError("no target metric was measured; run the target scenarios or benchmarks")
    undecided = [i for i in items if outcome(i) in ("no floor", "not measured")]
    no_gain = [i for i in targets if outcome(i) == "no gain"]
    regressions = [i for i in items if not i["target"] and outcome(i) == "regression"]
    reasons += [f"{_name(i)}: {outcome(i)}" for i in undecided]
    reasons += [f"{_name(i)}: {_pct(i['delta_pct'])} is within the {i['floor_pct']:.1f}% floor or worse"
                for i in no_gain]
    reasons += [f"{_name(i)}: regressed {_pct(i['delta_pct'])}, beyond the {i['floor_pct']:.1f}% floor"
                for i in regressions]
    if not reasons:
        return KEEP, [f"{_name(i)}: {_pct(i['delta_pct'])}, beyond the {i['floor_pct']:.1f}% floor"
                      for i in targets], rd.APPLIED
    gained = [i for i in targets if outcome(i) == "gain"]
    wide = [i for i in targets if i["floor_pct"] is not None and i["floor_pct"] > WIDE_FLOOR]
    if problems or undecided or wide:
        suggestion = rd.DEFERRED + " (undecided: rerun on a quiet host)"
    elif gained and regressions:
        suggestion = rd.DEFERRED + " (gain with a regression elsewhere: rework, then rerun)"
    else:
        suggestion = rd.REJECTED
    return REVERT, reasons, suggestion


def _name(item):
    return f"{item['source']} {item['case']} {item['metric']}"


def _pct(value):
    if value is None:
        return "-"
    return "+inf%" if value == math.inf else f"{value:+.1f}%"


# ==================== record ====================

def _table(items):
    lines = ["| source | case | metric | feature | candidate | change | floor | result |",
             "|---|---|---|---:|---:|---:|---:|---|"]
    for i in items:
        floor = "-" if i["floor_pct"] is None else f"{i['floor_pct']:.1f}%"
        lines.append(f"| {i['source']} | {i['case']} | {i['metric']} | {pab._fmt(i['feature'], 2)} "
                     f"| {pab._fmt(i['candidate'], 2)} | {_pct(i['delta_pct'])} | {floor} | {outcome(i)} |")
    return lines


def _jmh_table(null_a, null_b, candidate):
    """Every JMH result of the three runs: score ± error (unit-free) per benchmark, parameters and metric."""
    lines = ["## JMH runs (feature, candidate, feature again)", "",
             "| benchmark | params | metric | feature-a | candidate | feature-b |", "|---|---|---|---:|---:|---:|"]

    def cell(run, key, metric):
        score, error = run.get(key, {}).get(metric, (None, None))
        return "-" if score is None else f"{score:,.3f} ± {'-' if error is None else f'{error:,.3f}'}"

    for key, metrics in null_a.items():
        for metric in metrics:
            lines.append(f"| {key[0]} | {key[1] or '-'} | {metric} | {cell(null_a, key, metric)} "
                         f"| {cell(candidate, key, metric)} | {cell(null_b, key, metric)} |")
    return lines + [""]


def _demote(markdown):
    return "\n".join("##" + line if line.startswith("#") else line for line in markdown.splitlines())


def yaml_change(entry, verdict, suggestion, record_rel, items):
    numbers = "; ".join(f"{i['case']} {i['metric']} {_pct(i['delta_pct'])} (floor "
                        f"{'-' if i['floor_pct'] is None else '%.1f%%' % i['floor_pct']})"
                        for i in items if i["target"])
    lines = [f'- id: "{entry["id"]}"', "  # keep optimization, component and task as they are"]
    if verdict == KEEP:
        lines += [f'  decision: "{rd.APPLIED}"',
                  '  classes: ["<fully qualified class that implements it>"]',
                  "  applied_in_phase: 5"]
    else:
        decision = rd.REJECTED if suggestion == rd.REJECTED else rd.DEFERRED
        lines.append(f'  decision: "{decision}"')
        if decision == rd.REJECTED:
            lines.append('  component: "—"')
    lines += [f'  reason: "<existing reason>; 9.19 experiment: {numbers}"',
              f'  ab_record: "{record_rel}"']
    return lines


def render_record(racecar_id, entry, spec, verdict, reasons, suggestion, items, null_doc, ab_doc, record_rel,
                  jmh_files=None, preliminary=False, refs=None, jmh_runs=None):
    labels = {a["label"]: a for a in ab_doc["arms"]}
    params = ab_doc["params"]
    lines = [f"# Optimization experiment: racecar {racecar_id}", ""]
    if preliminary:
        lines += ["> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict "
                  "below only shows that the procedure runs end to end. Do not apply its YAML change.", ""]
    lines += [f"Racecar {racecar_id}: {entry['optimization']}. Task {spec.get('task', entry.get('task', '?'))}. "
              f"Current decision: {entry['decision']}.", "",
              "| arm | ref | commit | SDK version |", "|---|---|---|---|"]
    for label in (FEATURE, CANDIDATE):
        a = labels.get(label, {})
        ref = (refs or {}).get(label) or a.get("ref", "?")
        lines.append(f"| `{label}` | `{ref}` | `{a.get('commit', '?')[:12]}` "
                     f"| {a.get('sdk_version', '?')} |")
    lines += ["", f"E2E: reps {params.get('reps')}, iterations {params.get('iterations')}, warmup "
              f"{params.get('warmup')}, clients {params.get('clients')}, scenarios {params.get('scenarios')}, "
              f"{params.get('java', '?')}, {params.get('host', '?')}, {params.get('hardware', '?')}. "
              f"Null run `{null_doc['params'].get('run_id')}`, A/B run `{params.get('run_id')}`.", ""]
    if jmh_files:
        lines += ["JMH: " + ", ".join(f"{k} `{os.path.basename(v)}`" for k, v in jmh_files.items()) + ".", ""]
    if spec.get("note"):
        lines += [f"Note: {spec['note']}", ""]
    lines += [f"## Verdict: {verdict}", "", f"Suggested decision: **{suggestion}**.", ""]
    lines += [f"- {r}" for r in reasons] + [""]
    lines += ["## Targets", ""] + _table([i for i in items if i["target"]]) + [""]
    lines += ["## Other scenarios and benchmarks (guards)", ""] + _table([i for i in items if not i["target"]])
    lines += ["", "## YAML change", "",
              "Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run "
              "`python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).", "",
              "```yaml"] + yaml_change(entry, verdict, suggestion, record_rel, items) + ["```", ""]
    if verdict == REVERT:
        lines += ["The candidate is not kept: do not merge its commit (or `git revert` it if it is already on the "
                  "branch) before the next optimization is tried.", ""]
    if jmh_runs:
        lines += _jmh_table(*jmh_runs)
    lines += ["## Null experiment (e2e)", "", _demote(pab.render_summary(null_doc)), "",
              "## Paired A/B (e2e)", "", _demote(pab.render_summary(ab_doc, null_doc)), ""]
    return "\n".join(lines)


# ==================== CLI ====================

def _opts(argv, names, flags=()):
    opts, found = {n: None for n in names}, set()
    i = 0
    while i < len(argv):
        if argv[i] in flags:
            found.add(argv[i])
            i += 1
        elif argv[i] in opts and i + 1 < len(argv):
            opts[argv[i]] = argv[i + 1]
            i += 2
        else:
            raise ExperimentError(f"unknown or incomplete option {argv[i]}\n{__doc__}")
    return opts, found


def plan_main(racecar_id, argv):
    opts, _ = _opts(argv, ("--targets", "--yaml"))
    entry = decision_entry(opts["--yaml"] or DEFAULT_YAML, racecar_id)
    spec, _ = load_targets(opts["--targets"] or DEFAULT_TARGETS, racecar_id)
    print(f"decision\t{entry['decision']}")
    print(f"task\t{spec.get('task', '')}")
    jmh = spec.get("jmh")
    if jmh:
        print(f"jmh_module\t{jmh['module']}")
        print(f"jmh_include\t{jmh['include']}")
        for p in jmh.get("params", []):
            print(f"jmh_param\t{p}")
    return 0


def verdict_main(racecar_id, argv):
    opts, flags = _opts(argv, ("--null", "--ab", "--out", "--jmh-null-a", "--jmh-null-b", "--jmh-candidate",
                               "--targets", "--yaml", "--feature-ref", "--candidate-ref"),
                        ("--preliminary",))
    if not opts["--null"] or not os.path.isfile(opts["--null"]):
        raise ExperimentError("verdict needs the null experiment's paired-ab.json (--null)")
    for required in ("--ab", "--out"):
        if not opts[required]:
            raise ExperimentError(f"verdict needs {required}")
    yaml_path = opts["--yaml"] or DEFAULT_YAML
    entry = decision_entry(yaml_path, racecar_id)
    spec, guards = load_targets(opts["--targets"] or DEFAULT_TARGETS, racecar_id)
    null_doc, ab_doc = pab.load(opts["--null"]), pab.load(opts["--ab"])
    if not pab.is_null_experiment(null_doc):
        raise ExperimentError(f"{opts['--null']} is not a null experiment (paired-ab.sh --null)")
    items = e2e_items(spec, guards["e2e"], null_doc, ab_doc)
    jmh_keys = ("--jmh-null-a", "--jmh-null-b", "--jmh-candidate")
    jmh_files = OrderedDict((k[2:], opts[k]) for k in jmh_keys if opts[k])
    if jmh_files and len(jmh_files) != 3:
        raise ExperimentError("JMH needs all of --jmh-null-a, --jmh-null-b and --jmh-candidate")
    jmh_runs = None
    if jmh_files:
        jmh_runs = [load_jmh(opts[k]) for k in jmh_keys]
        items += jmh_items(spec, guards["jmh"], *jmh_runs)
        jmh_runs = (jmh_runs[0], jmh_runs[1], jmh_runs[2])
    problems = [f"null run: {p}" for p in pab.problems(null_doc)] + [f"A/B run: {p}" for p in pab.problems(ab_doc)]
    verdict, reasons, suggestion = decide(items, problems)
    record_rel = os.path.relpath(os.path.abspath(opts["--out"]), os.path.dirname(os.path.abspath(yaml_path)))
    text = render_record(racecar_id, entry, spec, verdict, reasons, suggestion, items, null_doc, ab_doc,
                         record_rel, jmh_files, "--preliminary" in flags,
                         {FEATURE: opts["--feature-ref"], CANDIDATE: opts["--candidate-ref"]}, jmh_runs)
    os.makedirs(os.path.dirname(os.path.abspath(opts["--out"])), exist_ok=True)
    with open(opts["--out"], "w") as f:
        f.write(text)
    print(f"Wrote {opts['--out']}")
    print(f"Verdict: {verdict}. Suggested decision: {suggestion}.")
    for r in reasons:
        print(f"  - {r}")
    print("\nYAML change to apply by hand to " + os.path.relpath(yaml_path, REPO_ROOT) +
          ", then run: python3 scripts/schema-serde/racecar_decisions.py && "
          "python3 scripts/schema-serde/racecar_decisions.py --check")
    print("\n".join(yaml_change(entry, verdict, suggestion, record_rel, items)))
    return 0 if verdict == KEEP else 3


def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    try:
        if len(argv) >= 2 and argv[0] == "plan":
            return plan_main(argv[1], argv[2:])
        if len(argv) >= 2 and argv[0] == "verdict":
            return verdict_main(argv[1], argv[2:])
    except (ExperimentError, rd.InvalidDecisions) as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 2
    print(__doc__, file=sys.stderr)
    return 2


# ==================== self-test ====================

def self_test():
    import tempfile
    import unittest

    def run(n, rep, arm, scenario, cpu, alloc, client="v2-sync"):
        return {"run": n, "rep": rep, "arm": arm, "client": client, "scenario": scenario, "status": "ok",
                "started": "t", "result": {"app_cpu_us_per_op": cpu, "alloc_bytes_per_op": alloc,
                                           "pipeline": "schema", "commit": "h"}}

    def doc(arms, runs, null=False):
        return {"schema_version": 1, "params": {"run_id": "fixture", "null_experiment": str(null).lower()},
                "arms": [{"label": a, "ref": "r", "commit": "c" * 40, "sdk_version": "v", "jar": "j"} for a in arms],
                "runs": runs}

    # Null experiment: small-get cpu ratios 1.02, 0.98 -> floor max(0, 2.83)%; batch-get cpu 1.01, 0.99.
    null_doc = doc(["feature-a", "feature-b"], [
        run(1, 1, "feature-a", "small-get", 100.0, 1000), run(2, 1, "feature-b", "small-get", 102.0, 1000),
        run(3, 2, "feature-b", "small-get", 98.0, 1000), run(4, 2, "feature-a", "small-get", 100.0, 1000),
        run(5, 1, "feature-a", "batch-get", 200.0, 5000), run(6, 1, "feature-b", "batch-get", 202.0, 5010),
        run(7, 2, "feature-b", "batch-get", 198.0, 4990), run(8, 2, "feature-a", "batch-get", 200.0, 5000),
    ], null=True)

    def ab(small_cpu, batch_cpu):
        return doc([FEATURE, CANDIDATE], [
            run(1, 1, FEATURE, "small-get", 100.0, 1000), run(2, 1, CANDIDATE, "small-get", small_cpu, 1000),
            run(3, 2, CANDIDATE, "small-get", small_cpu, 1000), run(4, 2, FEATURE, "small-get", 100.0, 1000),
            run(5, 1, FEATURE, "batch-get", 200.0, 5000), run(6, 1, CANDIDATE, "batch-get", batch_cpu, 5000),
            run(7, 2, CANDIDATE, "batch-get", batch_cpu, 5000), run(8, 2, FEATURE, "batch-get", 200.0, 5000),
        ])

    spec = {"task": "9.x", "e2e": [{"scenarios": ["small-get"], "metrics": ["app_cpu_us_per_op"]}],
            "jmh": {"module": "m", "include": "x", "targets": [{"benchmark": r"Marshall\.", "metrics": ["score"]}]}}
    guards = {"e2e": ["app_cpu_us_per_op", "alloc_bytes_per_op"], "jmh": ["score", "gc.alloc.rate.norm"]}

    def items(small_cpu, batch_cpu):
        return e2e_items(spec, guards["e2e"], null_doc, ab(small_cpu, batch_cpu))

    def jmh(score, alloc, err=0.5):
        return {("b.Marshall.run", "protocol=AWS_JSON_1_0"): {"score": (score, err), "gc.alloc.rate.norm": (alloc, 0.0)},
                ("b.Lookup.get", ""): {"score": (10.0, 0.1), "gc.alloc.rate.norm": (alloc and 0.0001, 0.0)}}

    class VerdictRulesTest(unittest.TestCase):
        def test_decide_whenTargetGainWithinFloor_reverts(self):
            # small-get -2% is within its 2.8% floor.
            verdict, reasons, suggestion = decide(items(98.0, 200.0))
            self.assertEqual(verdict, REVERT)
            self.assertTrue(any("within the 2.8% floor" in r for r in reasons))
            self.assertEqual(suggestion, rd.REJECTED)

        def test_decide_whenGainBeyondFloorAndRegressionElsewhere_reverts(self):
            # small-get -10% gains; batch-get +5% is beyond its 2.8% floor.
            verdict, reasons, suggestion = decide(items(90.0, 210.0))
            self.assertEqual(verdict, REVERT)
            self.assertTrue(any("v2-sync batch-get app_cpu_us_per_op: regressed +5.0%" in r for r in reasons))
            self.assertTrue(suggestion.startswith(rd.DEFERRED))

        def test_decide_whenGainBeyondFloorAndNoRegression_keeps(self):
            verdict, reasons, suggestion = decide(items(90.0, 201.0))
            self.assertEqual(verdict, KEEP)
            self.assertEqual(suggestion, rd.APPLIED)
            self.assertEqual(reasons, ["e2e v2-sync small-get app_cpu_us_per_op: -10.0%, beyond the 2.8% floor"])

        def test_decide_whenNoTargetMeasuredOrRunFailed_errorsOrReverts(self):
            guards_only = [i for i in items(90.0, 200.0) if not i["target"]]
            with self.assertRaises(ExperimentError):
                decide(guards_only)
            verdict, reasons, suggestion = decide(items(90.0, 200.0), ["A/B run: 1 of 8 runs failed"])
            self.assertEqual(verdict, REVERT)
            self.assertIn("undecided", suggestion)

        def test_jmhItems_floorFromFeatureTwiceAndZeroAllocationIsAbsolute(self):
            found = jmh_items(spec, guards["jmh"], jmh(100.0, 2000.0), jmh(101.0, 2000.0), jmh(90.0, 2000.0))
            by = {(i["case"].split(" ")[0], i["metric"]): i for i in found}
            marshall = by[("b.Marshall.run", "score")]
            self.assertTrue(marshall["target"])
            self.assertAlmostEqual(marshall["floor_pct"], 1.0)  # max(|101/100 - 1|, 0.5%, 0.495%)
            self.assertAlmostEqual(marshall["delta_pct"], -10.0)
            self.assertEqual(outcome(marshall), "gain")
            zero = by[("b.Lookup.get", "gc.alloc.rate.norm")]
            self.assertEqual((zero["floor_pct"], zero["delta_pct"]), (0.0, 0.0))
            allocating = jmh_items(spec, guards["jmh"], jmh(100.0, 2000.0), jmh(100.0, 2000.0),
                                   {("b.Lookup.get", ""): {"gc.alloc.rate.norm": (24.0, 0.0)}})
            lookup = [i for i in allocating if i["case"] == "b.Lookup.get" and i["metric"] == "gc.alloc.rate.norm"]
            self.assertEqual(outcome(lookup[0]), "regression")

        def test_verdictMain_whenNullRunMissing_errors(self):
            with tempfile.TemporaryDirectory() as d:
                ab_path = os.path.join(d, "ab.json")
                with open(ab_path, "w") as f:
                    json.dump(ab(90.0, 200.0), f)
                with self.assertRaises(ExperimentError):
                    verdict_main("E6", ["--null", os.path.join(d, "missing.json"), "--ab", ab_path,
                                        "--out", os.path.join(d, "E6.md")])
                with self.assertRaises(ExperimentError):  # an A/B run passed as the null run
                    verdict_main("E6", ["--null", ab_path, "--ab", ab_path, "--out", os.path.join(d, "E6.md")])

        def test_verdictMain_writesRecordWithVerdictAndYamlChange(self):
            with tempfile.TemporaryDirectory() as d:
                paths = {}
                for name, content in (("null", null_doc), ("ab", ab(90.0, 200.0))):
                    paths[name] = os.path.join(d, name + ".json")
                    with open(paths[name], "w") as f:
                        json.dump(content, f)
                targets = os.path.join(d, "targets.json")
                with open(targets, "w") as f:
                    json.dump({"guard_metrics": guards, "targets": {"E6": dict(spec, jmh=None)}}, f)
                out = os.path.join(d, "optimizations", "E6.md")
                status = verdict_main("E6", ["--null", paths["null"], "--ab", paths["ab"], "--out", out,
                                             "--targets", targets, "--preliminary"])
                with open(out) as f:
                    text = f.read()
            self.assertEqual(status, 0)
            self.assertIn("## Verdict: KEEP", text)
            self.assertIn("PRELIMINARY", text)
            self.assertIn(f'decision: "{rd.APPLIED}"', text)
            self.assertIn("applied_in_phase: 5", text)
            self.assertIn("## Null experiment (e2e)", text)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(VerdictRulesTest)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
