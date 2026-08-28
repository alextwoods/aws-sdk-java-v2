#!/usr/bin/env python3
"""Summarize a paired A/B timing run from test/standalone-e2e-benchmarks/scripts/paired-ab.sh.

The point of pairing is that the per-repetition *ratio* between arms is far more stable than either
arm's absolute number, because both arms see the same machine conditions within a repetition. So the
headline statistic here is the mean of the per-rep ratios (with its spread and a sign count), not the
ratio of the means.

Arms are identified by the `phase` column, which comes from each jar's embedded provenance, and the
`commit` column is checked to confirm both arms used the same harness build — comparing jars is only
sound if the only difference is the SDK inside them.

Usage: paired_ab_summary.py RUNDIR
"""
import csv
import statistics
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

# (csv field, label, per-op conversion, lower_is_better)
METRICS = [
    ("avg_us_per_op", "wall µs/op", lambda r: float(r["avg_us_per_op"]), True),
    ("cpu_us_per_op", "cpu µs/op",
     lambda r: float(r["cpu_ms"]) * 1000.0 / float(r["iterations"]), True),
    ("user_cpu_us_per_op", "user cpu µs/op",
     lambda r: float(r["cpu_user_ms"]) * 1000.0 / float(r["iterations"])
     if r.get("cpu_user_ms") else None, True),
]


def load(rundir):
    path = Path(rundir) / "results.csv"
    if not path.exists():
        raise SystemExit(f"no results.csv under {rundir}")
    rows = []
    with path.open() as f:
        for r in csv.DictReader(f):
            rows.append(r)
    if not rows:
        raise SystemExit(f"results.csv under {rundir} has no data rows")
    return rows


def main(argv):
    if len(argv) != 1:
        raise SystemExit(__doc__)
    rundir = Path(argv[0])
    rows = load(rundir)

    if "phase" not in rows[0] or "commit" not in rows[0]:
        raise SystemExit("results.csv has no phase/commit columns; rebuild the jars with provenance")

    # Arms in order of first appearance; rep 1 runs them in the order given on the command line, so
    # the first one is the baseline.
    arms = list(OrderedDict.fromkeys(r["phase"] for r in rows))
    if len(arms) < 2:
        raise SystemExit(f"only one arm ({arms[0]!r}) present — arms are identified by the jar's "
                         f"stamped phase label, so each jar needs a distinct one")

    harness = {r["commit"] for r in rows}
    sdk = {r["phase"]: r.get("sdk_commit", "?") for r in rows}

    # phase -> (client, scenario) -> [values in rep order]
    data = defaultdict(lambda: defaultdict(list))
    for r in rows:
        data[r["phase"]][(r["client"], r["scenario"])].append(r)

    print(f"# Paired A/B summary — `{rundir.name}`\n")
    print(f"Arms (baseline first): {', '.join(f'`{a}`' for a in arms)}\n")
    for a in arms:
        n = len(next(iter(data[a].values()), []))
        print(f"- `{a}`: sdk_commit `{sdk[a]}`, {n} reps per case")
    print()
    if len(harness) == 1:
        print(f"Harness build identical across arms (commit `{harness.pop()}`), so the SDK is the "
              f"only difference.\n")
    else:
        print("**WARNING: arms were built from different harness commits "
              f"({', '.join(sorted(harness))}). The comparison is not clean — rebuild both jars "
              "from the same harness before drawing conclusions.**\n")

    base = arms[0]
    cases = list(OrderedDict.fromkeys(
        k for a in arms for k in data[a].keys()))

    for field, label, extract, lower_better in METRICS:
        # Skip a metric the CPU source didn't provide.
        sample = extract(rows[0])
        if sample is None:
            continue
        print(f"## {label}\n")
        header = "| client | scenario | " + f"{base} mean |"
        divider = "|--------|----------|" + "----:|"
        for a in arms[1:]:
            header += f" {a} mean | paired delta | spread of pairs | wins |"
            divider += "----:|----:|----:|----:|"
        print(header)
        print(divider)

        for client, scenario in cases:
            base_rows = data[base].get((client, scenario), [])
            base_vals = [extract(r) for r in base_rows]
            if not base_vals or any(v is None for v in base_vals):
                continue
            cells = [f"{statistics.fmean(base_vals):,.1f}"]
            for a in arms[1:]:
                cand_rows = data[a].get((client, scenario), [])
                cand_vals = [extract(r) for r in cand_rows]
                if not cand_vals or any(v is None for v in cand_vals):
                    cells += ["-", "-", "-", "-"]
                    continue
                n = min(len(base_vals), len(cand_vals))
                # Per-rep ratios: both arms ran back to back inside rep k, so conditions match.
                ratios = [cand_vals[k] / base_vals[k] for k in range(n) if base_vals[k]]
                mean_delta = (statistics.fmean(ratios) - 1.0) * 100
                spread = (statistics.stdev(ratios) * 100) if len(ratios) > 1 else 0.0
                better = sum(1 for x in ratios if (x < 1.0) == lower_better and x != 1.0)
                cells += [f"{statistics.fmean(cand_vals):,.1f}",
                          f"{mean_delta:+.1f}%",
                          f"±{spread:.1f}%",
                          f"{better}/{len(ratios)}"]
            print(f"| {client} | {scenario} | " + " | ".join(cells) + " |")
        print()

    print("## Per-arm run-to-run spread\n")
    print("How noisy each arm was on its own. Where this is much larger than the paired spread")
    print("above, pairing is doing real work and unpaired numbers from this machine can't be")
    print("trusted at that resolution.\n")
    print("| client | scenario | arm | min | mean | max | spread |")
    print("|--------|----------|-----|----:|----:|----:|-------:|")
    for client, scenario in cases:
        for a in arms:
            vals = [float(r["avg_us_per_op"]) for r in data[a].get((client, scenario), [])]
            if not vals:
                continue
            lo, hi = min(vals), max(vals)
            spread = (hi - lo) / lo * 100 if lo else 0.0
            print(f"| {client} | {scenario} | `{a}` | {lo:,.1f} | {statistics.fmean(vals):,.1f} "
                  f"| {hi:,.1f} | {spread:.1f}% |")
    print()


if __name__ == "__main__":
    main(sys.argv[1:])
