#!/usr/bin/env python3
"""Summarize a concurrency sweep and flag where the measurement stops being about the client.

Concurrency is worth having: it produces more samples per second of wall clock and exercises a more
realistic workload. But it is only free while the *per-operation cost* stays flat. Once client and
server together outgrow the host's cores, per-operation CPU inflates for reasons that have nothing
to do with the SDK, and a phase-to-phase comparison at that level measures scheduling.

Columns:
  ops/s           throughput (iterations / wall)
  scale           throughput relative to concurrency 1
  app us/op       APPLICATION CPU per operation — the efficiency metric. Excludes the JIT compiler,
                  VM and GC threads, whose cost is fixed per JVM rather than per operation; using
                  whole-process CPU here produced a number that fell 114 -> 49 us/op purely by
                  lengthening the window.
  drift           app us/op relative to concurrency 1
  srv us/op       server CPU per operation
  cores           (client CPU + server CPU) / wall — total demand in cores
  p50/p99 us      latency percentiles
  flags           SERVER-SATURATED, OVERSUBSCRIBED (cores > host cores), CPU-DRIFT (>10%),
                  NOT-STEADY (JIT still compiling inside the window, so per-op CPU is unreliable)

Flags divide into two kinds, and only one of them disqualifies a level:

  HARD  SERVER-SATURATED, OVERSUBSCRIBED, NOT-STEADY. Each means the run is no longer measuring the
        client: the server is the limit, the host is out of cores, or the JIT is still changing the
        code. A level with any of these cannot be the basis for a comparison.
  SOFT  CPU-DRIFT. Per-operation CPU rising with concurrency is real contention cost, but a phase
        comparison holds concurrency fixed, so drift is a constant offset shared by both arms rather
        than an error. It is reported because it says how much of the measurement is contention, not
        because it invalidates anything.

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

    if "app_cpu_us_per_op" not in rows[0]:
        raise SystemExit("results.csv has no app_cpu_us_per_op column; this sweep predates "
                         "application-CPU accounting and its per-operation CPU cannot be trusted")

    print("# Concurrency sweep\n")
    if cores:
        print(f"Host: {cores} logical cores. `cores` is total CPU demand (client + server) divided "
              f"by wall time; above {cores} the run is oversubscribed and per-operation cost "
              f"inflates for scheduling reasons.\n")
    print("`app us/op` is application CPU only — compiler, VM and GC threads excluded. Rows flagged "
          "NOT-STEADY had JIT compilation running inside the measured window, so their per-operation "
          "figures are not steady-state regardless of the accounting.\n")

    recommendations = []
    for (client, transport, scenario), by_conc in data.items():
        print(f"## {client} ({transport}) / {scenario}\n")
        levels = sorted(by_conc)
        base = by_conc[levels[0]]
        base_ops = float(base["ops_per_wall_sec"])
        base_cpu = float(base["app_cpu_us_per_op"])

        print("| conc | ops/s | scale | app us/op | drift | srv us/op | cores | p50 us | "
              "p99 us | flags |")
        print("|-----:|------:|------:|----------:|------:|----------:|------:|-------:|"
              "-------:|-------|")
        healthy = []
        for c in levels:
            r = by_conc[c]
            ops = float(r["ops_per_wall_sec"])
            app_op = float(r["app_cpu_us_per_op"])
            iters = float(r["iterations"])
            wall_ms = float(r["wall_ms"])
            cpu_ms = float(r["cpu_ms"])
            srv_ms = float(r["server_cpu_ms"]) if r.get("server_cpu_ms") else 0.0
            srv_op = srv_ms * 1000.0 / iters if iters else 0.0
            demand = (cpu_ms + srv_ms) / wall_ms if wall_ms else 0.0
            drift = (app_op - base_cpu) / base_cpu * 100 if base_cpu else 0.0

            hard = []
            soft = []
            if str(r.get("server_saturated", "")).lower() == "true":
                hard.append("SERVER-SATURATED")
            if cores and demand > cores:
                hard.append("OVERSUBSCRIBED")
            if str(r.get("steady_state", "true")).lower() == "false":
                hard.append("NOT-STEADY")
            if drift > drift_pct:
                soft.append("CPU-DRIFT")
            if not hard:
                healthy.append(c)

            print(f"| {c} | {ops:,.0f} | {ops / base_ops:.2f}x | {app_op:,.1f} | {drift:+.1f}% | "
                  f"{srv_op:,.1f} | {demand:.1f} | {float(r['p50_us']):,.0f} | "
                  f"{float(r['p99_us']):,.0f} | {' '.join(hard + soft)} |")
        print()
        best = max(healthy) if healthy else levels[0]
        speedup = float(by_conc[best]["ops_per_wall_sec"]) / base_ops
        best_drift = ((float(by_conc[best]["app_cpu_us_per_op"]) - base_cpu) / base_cpu * 100
                      if base_cpu else 0.0)
        recommendations.append((client, transport, best, speedup, best_drift))
        print(f"Highest level with no hard flag: **{best}** ({speedup:.1f}x the throughput of "
              f"concurrency {levels[0]}, per-operation CPU {best_drift:+.0f}% from contention).\n")

    if len(recommendations) > 1:
        print("## Recommended default\n")
        limit = min(r[2] for r in recommendations)
        print("| client | transport | highest without a hard flag | speedup | contention cost |")
        print("|--------|-----------|----------------------------:|--------:|----------------:|")
        for client, transport, best, speedup, best_drift in recommendations:
            print(f"| {client} | {transport} | {best} | {speedup:.1f}x | {best_drift:+.0f}% |")
        print()
        print(f"Use **{limit}**: the lowest of these, because a phase comparison has to run every "
              f"client at the same concurrency and the level has to be usable for all of them. The "
              f"contention cost column is the price paid for the extra samples — it applies equally "
              f"to both arms of a comparison, so it shifts absolute numbers without biasing a "
              f"delta.\n")


if __name__ == "__main__":
    main(sys.argv[1:])
