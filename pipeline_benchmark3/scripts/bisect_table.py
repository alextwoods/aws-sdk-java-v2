#!/usr/bin/env python3
"""Per-version table for the 2.46 -> 2.54 stock-V2 bisection: app CPU us/op by (version, client, scenario)."""
import csv, statistics, sys
from collections import defaultdict

rows = defaultdict(list)
for r in csv.DictReader(open(sys.argv[1])):
    v = r["phase"].replace("bisect-", "").replace("_", ".")
    rows[(v, r["client"], r["scenario"])].append(r)

def vkey(v):
    return tuple(int(x) for x in v.split("."))

versions = sorted({k[0] for k in rows}, key=vkey)
for client, scenario in [("v2-sync", "small-get"), ("v2-async", "small-get"), ("v2-sync", "batch-put")]:
    print(f"\n--- {client} {scenario}: app CPU us/op (mean of reps, spread%, steady, n)   delta vs previous")
    prev = None
    for v in versions:
        rs = rows.get((v, client, scenario), [])
        if not rs:
            continue
        cpu = [float(r["app_cpu_us_per_op"]) for r in rs]
        lat = [float(r["p50_us"]) for r in rs]
        m = statistics.fmean(cpu)
        sp = (max(cpu) - min(cpu)) / m * 100 if len(cpu) > 1 else 0
        steady = sum(1 for r in rs if r["steady_state"] == "true")
        d = f"{(m - prev) / prev * 100:+6.1f}%" if prev else "      "
        print(f"  {v:8s} {m:8.1f}  ±{sp:4.1f}%  {steady}/{len(rs)}  p50 {statistics.fmean(lat):7.1f}   {d}")
        prev = m
