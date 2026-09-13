#!/usr/bin/env python3
"""Summarize the adjacent-version regression ladder: one paired run per (version N -> N+1) pair.

usage: ladder_table.py PAIRED_DIR... [--client v2-sync] [--scenario small-get]

Each PAIRED_DIR is a paired-ab run whose two phases are labelled bisect-<a> / bisect-<b>. Prints, per
step: means, paired delta (mean of per-rep ratios), the spread of the per-rep deltas, wins, and the
per-rep deltas themselves so a single-rep outlier is visible. Ends with the cumulative walk.
"""
import argparse
import csv
import os
import re
import statistics


def load(d, client, scenario):
    rows = list(csv.DictReader(open(os.path.join(d, "results.csv"))))
    phases = []
    for r in rows:
        if r["phase"] not in phases:
            phases.append(r["phase"])
    if len(phases) != 2:
        return None
    seq = {p: [] for p in phases}
    for r in rows:
        if r["client"] == client and r["scenario"] == scenario:
            seq[r["phase"]].append(float(r["app_cpu_us_per_op"]))
    a, b = phases
    return a, b, seq[a], seq[b]


def version_of(label):
    m = re.search(r"(\d+)_(\d+)_(\d+)", label)
    return tuple(int(x) for x in m.groups()) if m else label


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dirs", nargs="+")
    ap.add_argument("--client", default="v2-sync")
    ap.add_argument("--scenario", default="small-get")
    a = ap.parse_args()

    steps = []
    for d in a.dirs:
        loaded = load(d, a.client, a.scenario)
        if not loaded:
            continue
        pa, pb, xs, ys = loaded
        if version_of(pa) > version_of(pb):
            pa, pb, xs, ys = pb, pa, ys, xs
        n = min(len(xs), len(ys))
        deltas = [(ys[i] / xs[i] - 1) * 100 for i in range(n)]
        steps.append((version_of(pa), pa, pb, xs[:n], ys[:n], deltas, os.path.basename(d)))
    steps.sort()

    print(f"{a.client} {a.scenario}: application CPU us/op, paired by rep\n")
    print("| step | base | cand | Δ mean | Δ median | pair spread | wins | per-rep Δ | run |")
    print("|---|---:|---:|---:|---:|---:|---:|---|---|")
    cum = 0.0
    for _, pa, pb, xs, ys, deltas, run in steps:
        wins = sum(1 for d in deltas if d < 0)
        spread = (max(deltas) - min(deltas)) / 2
        cum += statistics.mean(deltas)
        pv = lambda p: p.replace("bisect-", "").replace("_", ".")
        print(f"| {pv(pa)} → {pv(pb)} | {statistics.mean(xs):.1f} | {statistics.mean(ys):.1f} | "
              f"{statistics.mean(deltas):+.1f}% | {statistics.median(deltas):+.1f}% | ±{spread:.1f}% | "
              f"{wins}/{len(deltas)} | {' '.join(f'{d:+.1f}' for d in deltas)} | {run} |")
    if steps:
        first = statistics.mean(steps[0][3])
        last = statistics.mean(steps[-1][4])
        print(f"\nsum of step deltas: {cum:+.1f}%   end-to-end from arm means: {first:.1f} -> {last:.1f} "
              f"({(last / first - 1) * 100:+.1f}%)")


if __name__ == "__main__":
    main()
