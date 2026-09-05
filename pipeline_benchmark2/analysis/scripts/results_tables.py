#!/usr/bin/env python3
"""Aggregate a collection run's results.csv (N timing reps per case) into markdown tables."""
import csv
import statistics
import sys
from collections import defaultdict

CLIENTS = ["v1", "v2-sync", "v2-async", "smithy"]
SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put"]


def main(path):
    rows = defaultdict(list)  # (client, scenario) -> list of dict
    with open(path) as f:
        for row in csv.DictReader(f):
            rows[(row["client"], row["scenario"])].append(row)

    def agg(client, scenario, field):
        vals = [float(r[field]) for r in rows[(client, scenario)]]
        return statistics.mean(vals), min(vals), max(vals)

    print("## Aggregated timing (mean of reps, [min..max])\n")
    for scenario in SCENARIOS:
        print(f"### {scenario}\n")
        print("| client | ops/wall-sec | ops/cpu-sec | ops/user-cpu-sec | avg us/op | cpu_ms (user/sys) | reps |")
        print("|--------|-------------:|------------:|-----------------:|----------:|------------------:|-----:|")
        for client in CLIENTS:
            if (client, scenario) not in rows:
                continue
            w, wlo, whi = agg(client, scenario, "ops_per_wall_sec")
            c, clo, chi = agg(client, scenario, "ops_per_cpu_sec")
            u, _, _ = agg(client, scenario, "ops_per_user_cpu_sec")
            a, _, _ = agg(client, scenario, "avg_us_per_op")
            cu, _, _ = agg(client, scenario, "cpu_user_ms")
            cs, _, _ = agg(client, scenario, "cpu_sys_ms")
            n = len(rows[(client, scenario)])
            print(f"| {client} | {w:,.0f} [{wlo:,.0f}..{whi:,.0f}] | {c:,.0f} [{clo:,.0f}..{chi:,.0f}] "
                  f"| {u:,.0f} | {a:.1f} | {cu:,.0f}/{cs:,.0f} | {n} |")
        print()

    print("## Ratios vs smithy-java (mean values; >1.0 = slower / more CPU than smithy)\n")
    print("| scenario | client | wall time x | total cpu x | user cpu x |")
    print("|----------|--------|------------:|------------:|-----------:|")
    for scenario in SCENARIOS:
        s_w, _, _ = agg("smithy", scenario, "ops_per_wall_sec")
        s_c, _, _ = agg("smithy", scenario, "ops_per_cpu_sec")
        s_u, _, _ = agg("smithy", scenario, "ops_per_user_cpu_sec")
        for client in CLIENTS:
            if client == "smithy" or (client, scenario) not in rows:
                continue
            w, _, _ = agg(client, scenario, "ops_per_wall_sec")
            c, _, _ = agg(client, scenario, "ops_per_cpu_sec")
            u, _, _ = agg(client, scenario, "ops_per_user_cpu_sec")
            print(f"| {scenario} | {client} | {s_w / w:.2f} | {s_c / c:.2f} | {s_u / u:.2f} |")
    print()

    print("## Per-op CPU microseconds (derived: 1e6 / ops_per_cpu_sec)\n")
    print("| scenario | " + " | ".join(CLIENTS) + " |")
    print("|----------|" + "---:|" * len(CLIENTS))
    for scenario in SCENARIOS:
        cells = []
        for client in CLIENTS:
            c, _, _ = agg(client, scenario, "ops_per_cpu_sec")
            u, _, _ = agg(client, scenario, "ops_per_user_cpu_sec")
            cells.append(f"{1e6 / c:.0f} total / {1e6 / u:.0f} user")
        print(f"| {scenario} | " + " | ".join(cells) + " |")


if __name__ == "__main__":
    main(sys.argv[1])
