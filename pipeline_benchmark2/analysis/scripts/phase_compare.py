#!/usr/bin/env python3
"""Compare timing results across optimization phases.

Each phase is a collection run directory (containing results.csv) plus a label. The first phase
given is treated as the baseline; every later phase is reported as a delta against it.

Usage:
    phase_compare.py LABEL=RUNDIR [LABEL=RUNDIR ...]

Example:
    phase_compare.py baseline=raw/phase0-baseline/20260826-1750 \
                     signer=raw/phaseF-signer/20260826-1900
"""
import csv
import statistics
import sys
from collections import defaultdict
from pathlib import Path

SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put"]

# (csv column, human label, higher_is_better)
METRICS = [
    ("ops_per_user_cpu_sec", "user-CPU ops/s", True),
    ("ops_per_cpu_sec", "total-CPU ops/s", True),
    ("ops_per_wall_sec", "wall ops/s", True),
    ("avg_us_per_op", "avg us/op", False),
]


def load(rundir):
    """(client, scenario) -> {field: [values]} from a run's results.csv."""
    path = Path(rundir) / "results.csv"
    if not path.exists():
        raise SystemExit(f"no results.csv under {rundir}")
    out = defaultdict(lambda: defaultdict(list))
    with open(path) as f:
        for row in csv.DictReader(f):
            for field, _, _ in METRICS:
                value = row.get(field)
                if value:
                    out[(row["client"], row["scenario"])][field].append(float(value))
    return out


def mean(data, key, field):
    vals = data.get(key, {}).get(field)
    return statistics.mean(vals) if vals else None


def spread(data, key, field):
    """Percent spread of the reps around the mean, as a rough noise floor."""
    vals = data.get(key, {}).get(field)
    if not vals or len(vals) < 2:
        return None
    m = statistics.mean(vals)
    return (max(vals) - min(vals)) / m * 100 if m else None


def main(args):
    if not args:
        raise SystemExit(__doc__)

    phases = []
    for arg in args:
        if "=" not in arg:
            raise SystemExit(f"expected LABEL=RUNDIR, got: {arg}")
        label, rundir = arg.split("=", 1)
        phases.append((label, load(rundir), rundir))

    base_label, base_data, _ = phases[0]
    clients = sorted({c for _, data, _ in phases for (c, _s) in data})

    print("# Phase comparison\n")
    print("Runs compared (first is the baseline):\n")
    for label, _, rundir in phases:
        print(f"- `{label}`: `{rundir}`")
    print()
    print("Deltas are vs the baseline. Positive % always means *better* (more throughput, less")
    print("time). The `spread` column is the rep-to-rep spread of the baseline as a noise")
    print("reference: treat deltas smaller than it as inconclusive.\n")

    for client in clients:
        print(f"## {client}\n")
        for metric_field, metric_label, higher_better in METRICS:
            print(f"### {metric_label}\n")
            header = "| scenario | " + f"{base_label} |"
            divider = "|----------|" + "----:|"
            for label, _, _ in phases[1:]:
                header += f" {label} | delta |"
                divider += "----:|----:|"
            header += " baseline spread |"
            divider += "----:|"
            print(header)
            print(divider)

            for scenario in SCENARIOS:
                key = (client, scenario)
                base = mean(base_data, key, metric_field)
                if base is None:
                    continue
                cells = [f"{base:,.1f}"]
                for _, data, _ in phases[1:]:
                    cur = mean(data, key, metric_field)
                    if cur is None:
                        cells += ["-", "-"]
                        continue
                    raw = (cur - base) / base * 100
                    improvement = raw if higher_better else -raw
                    cells += [f"{cur:,.1f}", f"{improvement:+.1f}%"]
                sp = spread(base_data, key, metric_field)
                cells.append(f"{sp:.1f}%" if sp is not None else "-")
                print(f"| {scenario} | " + " | ".join(cells) + " |")
            print()


if __name__ == "__main__":
    main(sys.argv[1:])
