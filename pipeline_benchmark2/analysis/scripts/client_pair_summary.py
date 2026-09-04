#!/usr/bin/env python3
"""Summarize a paired A/B whose axis is the *client arm* rather than the jar.

paired_ab_summary.py pairs arms that differ by jar, which is right for an SDK change. When two client
arms in one jar differ only in transport, the pairing axis is the client name instead, and the arms are
distinguished by the `client=` field of each RESULT line rather than by the jar's stamped phase label.

Input is the `all-results.txt` produced by the phase runner: RESULT lines with a `rep=N` field prepended.
Each pair is compared repetition by repetition, so a drift across the collection cancels instead of being
attributed to the transport.

Usage:
  client_pair_summary.py RESULTS_FILE --pair baseline=candidate [--pair ...] [--metric FIELD]
"""
import argparse
import re
import statistics
import sys
from collections import defaultdict

# Metrics worth reporting by default: application CPU is the cost the SDK controls, and mean latency is
# what a caller experiences. Both are reported because a transport change can move them differently.
DEFAULT_METRICS = ["app_cpu_us_per_op", "mean_lat_us"]


def parse(path):
    """Returns {(client, scenario, rep): {field: value}}."""
    rows = {}
    with open(path) as handle:
        for line in handle:
            if not line.startswith("RESULT"):
                continue
            fields = dict(
                match.groups()
                for match in re.finditer(r"(\w+)=([^\s]+)", line)
            )
            key = (fields.get("client"), fields.get("scenario"), fields.get("rep"))
            if None in key:
                continue
            rows[key] = fields
    return rows


def numeric(fields, name):
    try:
        return float(fields[name])
    except (KeyError, ValueError):
        return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("results")
    parser.add_argument("--pair", action="append", required=True,
                        help="baseline=candidate, e.g. v2-sync=v2-sync-smithy")
    parser.add_argument("--metric", action="append", default=None)
    args = parser.parse_args()

    metrics = args.metric or DEFAULT_METRICS
    rows = parse(args.results)
    if not rows:
        print(f"no RESULT lines found in {args.results}", file=sys.stderr)
        return 1

    reps = sorted({key[2] for key in rows}, key=lambda r: int(r))
    scenarios = []
    for key in rows:
        if key[1] not in scenarios:
            scenarios.append(key[1])

    for spec in args.pair:
        base_client, cand_client = spec.split("=", 1)
        print()
        print(f"================ {cand_client} vs {base_client} ================")
        print(f"repetitions present: {', '.join(reps)}")

        for metric in metrics:
            print()
            print(f"-- {metric} (negative delta = candidate faster) --")
            header = f"{'scenario':<12} {'base':>10} {'cand':>10} {'delta':>9} {'spread':>8} {'wins':>6}"
            print(header)
            for scenario in scenarios:
                deltas = []
                base_values = []
                cand_values = []
                for rep in reps:
                    base = rows.get((base_client, scenario, rep))
                    cand = rows.get((cand_client, scenario, rep))
                    if not base or not cand:
                        continue
                    b = numeric(base, metric)
                    c = numeric(cand, metric)
                    if b is None or c is None or b == 0:
                        continue
                    base_values.append(b)
                    cand_values.append(c)
                    deltas.append((c - b) / b * 100.0)

                if not deltas:
                    print(f"{scenario:<12} {'-':>10} {'-':>10} {'no pairs':>9}")
                    continue

                wins = sum(1 for d in deltas if d < 0)
                # Spread of the per-repetition deltas shows whether the effect is stable or an artifact
                # of one noisy repetition.
                spread = (max(deltas) - min(deltas)) / 2.0
                print(f"{scenario:<12} "
                      f"{statistics.mean(base_values):>10.1f} "
                      f"{statistics.mean(cand_values):>10.1f} "
                      f"{statistics.mean(deltas):>8.1f}% "
                      f"{spread:>7.1f}% "
                      f"{wins:>3}/{len(deltas)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
