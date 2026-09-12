#!/usr/bin/env python3
"""Aggregate the pipeline_benchmark3 collection into the report's tables.

An arm here is a (jar, client) pair, not a client: `v2-sync` runs on both the optimized jar and the
published-2.54.0 jar, and `v2-async` runs on three. The jar is the `phase` column, so arms are keyed on
(phase, client) and mapped to the labels the report uses.

Reports, per scenario: application CPU per operation, latency, throughput, plus the repetition spread and
how many runs reached steady state -- because a per-operation CPU number from a run that was still
compiling is not one.

Usage: bench3_tables.py <results.csv>
"""
import csv
import statistics
import sys
from collections import defaultdict

# (phase, client) -> (report label, ordering key)
ARMS = {
    ("bench3-racecar", "v1"): ("v1 (1.12.797)", 0),
    ("bench3-stock254", "v2-sync"): ("v2-sync stock 2.54.0", 1),
    ("bench3-stock254", "v2-async"): ("v2-async stock 2.54.0", 2),
    ("bench3-racecar", "v2-sync"): ("v2-sync OPTIMIZED", 3),
    ("bench3-racecar", "v2-async"): ("v2-async OPTIMIZED", 4),
    ("bench3-bridged", "v2-bridged"): ("v2-bridged (smithy-java)", 5),
    ("bench3-racecar", "smithy"): ("smithy-java 1.5.1", 6),
    # references / controls, printed separately
    ("bench3-bridged", "v2-async"): ("[ref] v2-async stock 2.46.11", 7),
    ("bench3-bridged", "v1"): ("[null] v1 on bridged jar", 8),
}
CONTROL_LABELS = {"[ref] v2-async stock 2.46.11", "[null] v1 on bridged jar"}
SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put", "describe-table"]


def load(path):
    rows = defaultdict(list)
    with open(path) as fh:
        for r in csv.DictReader(fh):
            key = (r.get("phase"), r.get("client"))
            if key not in ARMS:
                continue
            rows[(ARMS[key][0], r["scenario"])].append(r)
    return rows


def mean(v):
    return statistics.fmean(v) if v else float("nan")


def spread(v):
    return (max(v) - min(v)) / mean(v) * 100 if len(v) > 1 else 0.0


def table(rows, scenario, metric, label, fmt="{:9.1f}"):
    present = [(k[0], v) for k, v in rows.items() if k[1] == scenario]
    if not present:
        return
    order = {lab: idx for (lab, idx) in ARMS.values()}
    present.sort(key=lambda kv: order.get(kv[0], 99))
    print(f"\n--- {scenario}: {label}")
    print(f"{'arm':28s} {'mean':>9s} {'spread%':>8s} {'steady':>8s} {'n':>3s}")
    base = None
    for lab, runs in present:
        vals = [float(r[metric]) for r in runs if r.get(metric)]
        if not vals:
            continue
        steady = sum(1 for r in runs if r.get("steady_state") == "true")
        m = mean(vals)
        if lab == "v2-sync OPTIMIZED":
            base = m
        rel = ""
        if base and lab not in CONTROL_LABELS:
            rel = f"  {m / base:5.2f}x vs optimized-sync"
        print(f"{lab:28s} {fmt.format(m):>9s} {spread(vals):8.1f} {steady:4d}/{len(runs):<3d} {len(vals):3d}{rel}")


def headlines(rows):
    print("\n\n===== headline comparisons (application CPU per operation)")
    print(f"{'scenario':16s} {'bridged':>10s} {'opt-sync':>10s} {'stock254':>10s} "
          f"{'bridged/opt':>12s} {'opt/stock':>10s} {'bridged/stock':>14s}")
    for s in SCENARIOS:
        def m(lab):
            runs = rows.get((lab, s), [])
            vals = [float(r["app_cpu_us_per_op"]) for r in runs if r.get("app_cpu_us_per_op")]
            return mean(vals) if vals else None
        b, o, st = m("v2-bridged (smithy-java)"), m("v2-sync OPTIMIZED"), m("v2-sync stock 2.54.0")
        if not (b and o):
            continue
        bo = f"{b / o:.2f}x"
        os_ = f"{o / st:.2f}x" if st else "-"
        bs = f"{b / st:.2f}x" if st else "-"
        stv = f"{st:10.1f}" if st else f"{'-':>10s}"
        print(f"{s:16s} {b:10.1f} {o:10.1f} {stv} {bo:>12s} {os_:>10s} {bs:>14s}")

    print("\n===== cross-jar null control: v1 is identical code and version in every jar")
    for s in SCENARIOS:
        a = rows.get(("v1 (1.12.797)", s), [])
        b = rows.get(("[null] v1 on bridged jar", s), [])
        if not a or not b:
            continue
        av = mean([float(r["app_cpu_us_per_op"]) for r in a])
        bv = mean([float(r["app_cpu_us_per_op"]) for r in b])
        print(f"  {s:16s} racecar jar {av:8.1f}   bridged jar {bv:8.1f}   delta {(bv - av) / av * 100:+6.1f}%")


def main():
    rows = load(sys.argv[1])
    for s in SCENARIOS:
        table(rows, s, "app_cpu_us_per_op", "application CPU us/op")
    for s in SCENARIOS:
        table(rows, s, "p50_us", "p50 latency us")
    for s in SCENARIOS:
        table(rows, s, "ops_per_wall_sec", "throughput ops/wall-sec", fmt="{:9.0f}")
    headlines(rows)


if __name__ == "__main__":
    main()
