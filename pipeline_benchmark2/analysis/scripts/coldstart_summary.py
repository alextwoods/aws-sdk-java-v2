#!/usr/bin/env python3
"""Summarize a cold-start sweep from test/standalone-e2e-benchmarks/scripts/cold-start.sh.

Cold numbers are one-shot: each row is a whole JVM's single first call, so a scheduler hiccup lands
entirely in one row instead of being averaged over 60,000 operations. The median is therefore the
headline rather than the mean, and the full min/max is printed next to it — with ten reps, a summary
that hides the spread hides most of what there is to know.

Where a sweep compared jars (`cold-start.sh --jars`), arms are also paired by rep index, since the arms
alternate within each repetition.

Usage: coldstart_summary.py RUNDIR
"""
import csv
import statistics
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

# (csv field, label) — build_plus_first is the attributable one; see the runner's runColdStart javadoc.
METRICS = [
    ("build_plus_first_ms", "client build + first call (ms)"),
    ("client_build_ms", "client construction (ms)"),
    ("first_call_ms", "first call (ms)"),
    ("jvm_to_first_response_ms", "jvm start to first response (ms)"),
    ("jit_ms", "jit compilation at exit (ms)"),
]


def main(argv):
    if len(argv) != 1:
        raise SystemExit(__doc__)
    rundir = Path(argv[0])
    path = rundir / "coldstart.csv"
    if not path.exists():
        raise SystemExit(f"no coldstart.csv under {rundir}")
    with path.open() as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise SystemExit("coldstart.csv has no data rows")

    arms = list(OrderedDict.fromkeys(r["jar"] for r in rows))
    clients = list(OrderedDict.fromkeys(r["client"] for r in rows))
    scenario = rows[0]["scenario"]
    reps = max(int(r["rep"]) for r in rows)

    # (client, arm) -> rows in rep order
    data = defaultdict(list)
    for r in rows:
        data[(r["client"], r["jar"])].append(r)

    print(f"# Cold-start summary — `{rundir.name}`\n")
    print(f"Scenario `{scenario}`, {reps} JVMs per (arm, client), {len(rows)} JVMs total. "
          f"Arms: {', '.join(f'`{a}`' for a in arms)}.\n")
    print("One JVM per row of data. Medians, because a single outlier is a whole data point here.\n")

    base = arms[0]
    for field, label in METRICS:
        print(f"## {label}\n")
        header = "| client |"
        divider = "|--------|"
        for a in arms:
            header += f" {a} median | min–max |"
            divider += "----:|----:|"
        if len(arms) > 1:
            header += f" paired vs {base} | wins |"
            divider += "----:|----:|"
        print(header)
        print(divider)
        for client in clients:
            cells = []
            for a in arms:
                vals = [float(r[field]) for r in data[(client, a)]]
                if not vals:
                    cells += ["-", "-"]
                    continue
                cells += [f"{statistics.median(vals):,.1f}", f"{min(vals):,.0f}–{max(vals):,.0f}"]
            if len(arms) > 1:
                base_vals = [float(r[field]) for r in data[(client, base)]]
                cand_vals = [float(r[field]) for r in data[(client, arms[-1])]]
                n = min(len(base_vals), len(cand_vals))
                ratios = [cand_vals[k] / base_vals[k] for k in range(n) if base_vals[k]]
                if ratios:
                    delta = (statistics.median(ratios) - 1.0) * 100
                    wins = sum(1 for x in ratios if x < 1.0)
                    cells += [f"{delta:+.1f}%", f"{wins}/{len(ratios)}"]
                else:
                    cells += ["-", "-"]
            print(f"| `{client}` | " + " | ".join(cells) + " |")
        print()

    print("## Later calls in the same JVM (µs)\n")
    print("How fast calls 2..N were, as the JVM warms. Not steady state — these are the first")
    print("handful of operations ever executed, so they are still hundreds of times slower than the")
    print("numbers a timing collection reports.\n")
    print("| client | arm | median of call 2 | median of last call |")
    print("|--------|-----|----:|----:|")
    for client in clients:
        for a in arms:
            series = [r["later_calls_us"].split("/") for r in data[(client, a)] if r["later_calls_us"]]
            series = [[float(x) for x in s if x] for s in series]
            series = [s for s in series if s]
            if not series:
                continue
            print(f"| `{client}` | `{a}` | {statistics.median(s[0] for s in series):,.0f} "
                  f"| {statistics.median(s[-1] for s in series):,.0f} |")
    print()


if __name__ == "__main__":
    main(sys.argv[1:])
