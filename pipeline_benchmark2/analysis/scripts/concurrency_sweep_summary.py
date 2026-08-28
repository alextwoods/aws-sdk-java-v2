#!/usr/bin/env python3
"""Summarize a concurrency sweep and flag where the measurement stops being about the client.

Concurrency is worth having: it produces more samples per second of wall clock and exercises a more
realistic workload. But it is only free while the *per-operation cost* stays flat. Once client and
server together outgrow the host's cores, per-operation CPU inflates for reasons that have nothing
to do with the SDK, and a phase-to-phase comparison at that level measures scheduling.

Columns:
  ops/s           throughput (iterations / wall)
  scale           throughput relative to concurrency 1
  cpu us/op       client CPU per operation — should be flat; this is the efficiency metric
  cpu drift       cpu us/op relative to concurrency 1
  srv us/op       server CPU per operation
  cores           (client CPU + server CPU) / wall — total demand in cores
  p50/p99 us      latency percentiles
  flags           SERVER-SATURATED, OVERSUBSCRIBED (cores > host cores), CPU-DRIFT (>10%)

Usage: concurrency_sweep_summary.py RESULTS.csv [--cores N] [--drift-pct P]
"""
import csv
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path


def main(argv):
    if not argv:
        raise SystemExit(__doc__)
    path = Path(argv[0])
    cores = 0
    drift_pct = 10.0
    i = 1
    while i < len(argv):
        if argv[i] == "--cores":
            cores = int(argv[i + 1]); i += 2
        elif argv[i] == "--drift-pct":
            drift_pct = float(argv[i + 1]); i += 2
        else:
            raise SystemExit(f"unexpected argument: {argv[i]}")

    if not path.exists():
        raise SystemExit(f"no such file: {path}")
    rows = list(csv.DictReader(path.open()))
    if not rows:
        raise SystemExit(f"{path} has no data rows")

    # client -> concurrency -> row
    data = defaultdict(OrderedDict)
    for r in rows:
        data[(r["client"], r.get("transport", ""), r["scenario"])][int(r["concurrency"])] = r

    print("# Concurrency sweep\n")
    if cores:
        print(f"Host: {cores} logical cores. `cores` is total CPU demand (client + server) divided "
              f"by wall time; above {cores} the run is oversubscribed and per-operation cost "
              f"inflates for scheduling reasons.\n")

    recommendations = []
    for (client, transport, scenario), by_conc in data.items():
        print(f"## {client} ({transport}) / {scenario}\n")
        levels = sorted(by_conc)
        base = by_conc[levels[0]]
        base_ops = float(base["ops_per_wall_sec"])
        base_cpu = float(base["cpu_us_per_op"])

        print("| conc | ops/s | scale | cpu us/op | cpu drift | srv us/op | cores | p50 us | "
              "p99 us | flags |")
        print("|-----:|------:|------:|----------:|----------:|----------:|------:|-------:|"
              "-------:|-------|")
        healthy = []
        for c in levels:
            r = by_conc[c]
            ops = float(r["ops_per_wall_sec"])
            cpu_op = float(r["cpu_us_per_op"])
            iters = float(r["iterations"])
            wall_ms = float(r["wall_ms"])
            cpu_ms = float(r["cpu_ms"])
            srv_ms = float(r["server_cpu_ms"]) if r.get("server_cpu_ms") else 0.0
            srv_op = srv_ms * 1000.0 / iters if iters else 0.0
            demand = (cpu_ms + srv_ms) / wall_ms if wall_ms else 0.0
            drift = (cpu_op - base_cpu) / base_cpu * 100 if base_cpu else 0.0

            flags = []
            if str(r.get("server_saturated", "")).lower() == "true":
                flags.append("SERVER-SATURATED")
            if cores and demand > cores:
                flags.append("OVERSUBSCRIBED")
            if drift > drift_pct:
                flags.append("CPU-DRIFT")
            if not flags:
                healthy.append(c)

            print(f"| {c} | {ops:,.0f} | {ops / base_ops:.2f}x | {cpu_op:,.1f} | {drift:+.1f}% | "
                  f"{srv_op:,.1f} | {demand:.1f} | {float(r['p50_us']):,.0f} | "
                  f"{float(r['p99_us']):,.0f} | {' '.join(flags)} |")
        print()
        best = max(healthy) if healthy else levels[0]
        speedup = float(by_conc[best]["ops_per_wall_sec"]) / base_ops
        recommendations.append((client, transport, best, speedup))
        print(f"Highest clean level: **{best}** ({speedup:.1f}x the throughput of concurrency "
              f"{levels[0]}, per-operation CPU still within {drift_pct:.0f}%).\n")

    if len(recommendations) > 1:
        print("## Recommended default\n")
        limit = min(r[2] for r in recommendations)
        print("| client | transport | highest clean | speedup |")
        print("|--------|-----------|--------------:|--------:|")
        for client, transport, best, speedup in recommendations:
            print(f"| {client} | {transport} | {best} | {speedup:.1f}x |")
        print()
        print(f"Use **{limit}**: the lowest of these, since a phase comparison has to run every "
              f"client at the same concurrency, and the level has to be clean for all of them.\n")


if __name__ == "__main__":
    main(sys.argv[1:])
