#!/usr/bin/env python3
"""Build every table for a cross-SDK (v1 / v2-sync / v2-async / smithy) comparison report.

Unlike phase_compare.py and paired_ab_summary.py, which diff one client against itself across two
builds, this joins *different* clients whose metric names, phase boundaries and even units differ.
It therefore reads three sources and keeps them separate rather than trying to unify them:

  results.csv   per-rep timing/throughput, averaged over reps here
  metrics.txt   each SDK's own instrumentation, reported under that SDK's own metric names
  profiles-*.txt  output of collection_profile_report.py, i.e. CPU and allocation by category

Expected layout of BASE (a superset of what collect.sh + fetch produce):

  BASE/
    small/<timestamp>/{results.csv,<client>_<scenario>/metrics.txt}
    batch/<timestamp>/{results.csv,<client>_<scenario>/metrics.txt}
    profiles-small.txt
    profiles-batch.txt

"small" and "batch" are separate collections because a batch operation costs ~20x a small one, so a
single iteration count either starves the small scenarios of steady state or makes the batch runs
absurdly long. Add or rename groups via GROUPS/SCENARIOS below.

Usage: crosssdk_tables.py BASE
"""
import csv
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path

CLIENTS = ["v1", "v2-sync", "v2-async", "smithy"]
GROUPS = ["small", "batch"]
SCENARIOS = [("small-get", "small"), ("small-put", "small"),
             ("batch-get", "batch"), ("batch-put", "batch")]

# Category print order: transport-ish first, then the SDK's own layers, then noise.
CATEGORY_ORDER = ["socket-syscall", "http-client", "pipeline-framework", "signing", "marshall",
                  "unmarshall", "json", "crypto", "retry", "endpoint-rules", "thread-sync", "other"]

# Each SDK names its phases differently and draws the boundaries differently. Read the report's
# phase table notes before treating any row as comparable across columns -- notably V2's
# ApiCallDuration excludes marshalling, and smithy's serialization sits outside attempt_duration.
PHASE_METRICS = {
    "v1": {"marshall": "RequestMarshallTime", "sign": "RequestSigningTime",
           "http": "HttpRequestTime", "response": "ResponseProcessingTime",
           "total": "ClientExecuteTime"},
    "v2-sync": {"marshall": "MarshallingDuration", "sign": "SigningDuration",
                "endpoint": "EndpointResolveDuration", "svc call": "ServiceCallDuration",
                "unmarshall": "UnmarshallingDuration", "api call": "ApiCallDuration",
                "ttfb": "TimeToFirstByte", "ttlb": "TimeToLastByte"},
    "smithy": {"serialize": "smithy.client.call.serialization_duration",
               "sign": "smithy.client.call.auth.signing_duration",
               "endpoint": "smithy.client.call.resolve_endpoint_duration",
               "deserialize": "smithy.client.call.deserialization_duration",
               "attempt": "smithy.client.call.attempt_duration",
               "call": "smithy.client.call.duration"},
}
PHASE_METRICS["v2-async"] = PHASE_METRICS["v2-sync"]

# Cases whose hot frames are quoted in the report: the two clients that bracket each gap.
FRAME_CASES = ["v2-sync_small-get", "smithy_small-get", "v2-sync_batch-put", "smithy_batch-put",
               "v2-sync_batch-get", "v1_batch-get"]

CAT_RX = re.compile(r"^\s+([\d.]+)%\s+([a-z-]+)(?:\s+([\d,]+) B/op)?$", re.M)
FRAME_RX = re.compile(r"^\s+([\d.]+)%(?:\s+([\d,]+) B/op)?\s+(.+)$", re.M)
PER_OP_RX = re.compile(r"= ([\d,]+) bytes/op")


def num(s):
    return int(s.replace(",", ""))


def find_run(base, group):
    """The single timestamped collection directory under BASE/<group>/."""
    cands = sorted(p for p in (base / group).glob("*") if (p / "results.csv").exists())
    if not cands:
        sys.exit(f"no collection with a results.csv under {base / group}")
    return cands[-1]


def load_timings(runs):
    rows = defaultdict(list)
    for run in runs.values():
        with open(run / "results.csv") as fh:
            for r in csv.DictReader(fh):
                rows[(r["client"], r["scenario"])].append(r)
    return rows


def load_profiles(base):
    """(case, mode) -> (bytes_per_op or None, {cat: (pct, bytes_per_op)}, [(pct, bytes, frame)])."""
    prof = {}
    for group in GROUPS:
        path = base / f"profiles-{group}.txt"
        if not path.exists():
            continue
        txt = path.read_text()
        for mode in ("CPU", "ALLOC"):
            if mode not in txt:
                continue
            section = txt.split(f"## {mode}")[1]
            if mode == "CPU":
                # The ALLOC section follows, preceded by its banner rule.
                section = re.split(r"={10,}\n## ALLOC", section)[0]
            for block in re.split(r"\n### ", section)[1:]:
                case = block.split("\n")[0].strip()
                m = PER_OP_RX.search(block)
                cats = {c: (float(p), num(b) if b else None) for p, c, b in CAT_RX.findall(block)}
                marker = "top self frames:" if mode == "CPU" else "top allocation sites:"
                tail = block.split(marker)
                frames = [(float(p), num(b) if b else None, f.strip())
                          for p, b, f in FRAME_RX.findall(tail[1])] if len(tail) > 1 else []
                prof[(case, mode)] = (num(m.group(1)) if m else None, cats, frames)
    return prof


def load_phases(run, client, scenario):
    path = run / f"{client}_{scenario}" / "metrics.txt"
    if not path.exists():
        return {}
    out = {}
    for line in path.read_text().splitlines():
        if not line.startswith("METRIC"):
            continue
        parts = line.split()
        avg = [x for x in parts if x.startswith("avgUs=")]
        if avg:
            out[parts[1]] = float(avg[0].split("=")[1].replace(",", ""))
    return out


def banner(title):
    print("\n" + "=" * 100)
    print(title)
    print("=" * 100)


def header(first):
    return f'  {first:20}' + "".join(f'{c:>12}' for c in CLIENTS)


def main(argv):
    if len(argv) != 2:
        sys.exit(__doc__)
    base = Path(argv[1]).expanduser().resolve()
    runs = {g: find_run(base, g) for g in GROUPS}
    rows = load_timings(runs)
    prof = load_profiles(base)

    def mean(client, scenario, field):
        vals = [float(r[field]) for r in rows.get((client, scenario), []) if r.get(field)]
        return statistics.fmean(vals) if vals else None

    def ratio_row(label, vals, fmt):
        cells = "".join(fmt.format(v).rjust(12) if v else "-".rjust(12) for v in vals)
        r = vals[1] / vals[3] if vals[1] and vals[3] else 0
        return f'  {label:20}{cells}{r:>11.2f}x'

    banner("TABLE 1 - headline")
    for field, label, fmt in (("app_cpu_us_per_op", "app CPU us/op", "{:.1f}"),
                              ("mean_lat_us", "mean latency us", "{:.1f}"),
                              ("ops_per_wall_sec", "ops/wall-sec", "{:,.0f}")):
        print(f"\n{label}")
        print(header("scenario") + f'{"v2s/smithy":>12}')
        for scenario, _ in SCENARIOS:
            print(ratio_row(scenario, [mean(c, scenario, field) for c in CLIENTS], fmt))

    print("\nallocation bytes/op (client code)")
    print(header("scenario") + f'{"v2s/smithy":>12}')
    for scenario, _ in SCENARIOS:
        vals = [prof.get((f"{c}_{scenario}", "ALLOC"), (None,))[0] for c in CLIENTS]
        print(ratio_row(scenario, vals, "{:,.0f}"))

    for mode, title, pick, floor in (
            ("CPU", "TABLE 2 - CPU by category (% of client-code samples)",
             lambda v: f'{v[0]:.1f}%', lambda v: v[0] >= 0.5),
            ("ALLOC", "TABLE 3 - allocation by category (bytes/op)",
             lambda v: f'{v[1]:,}' if v[1] is not None else "-",
             lambda v: v[1] and v[1] > 300)):
        banner(title)
        for scenario, _ in SCENARIOS:
            print(f"\n{scenario}")
            print(header("category"))
            for cat in CATEGORY_ORDER:
                found = [prof.get((f"{c}_{scenario}", mode), (None, {}, []))[1].get(cat)
                         for c in CLIENTS]
                if not any(v and floor(v) for v in found):
                    continue
                print(f'  {cat:20}' + "".join((pick(v) if v else "-").rjust(12) for v in found))

    banner("TABLE 4 - SDK-reported phases (avg us/op, each SDK's own metric names)")
    for scenario, group in SCENARIOS:
        print(f"\n{scenario}")
        for client in CLIENTS:
            m = load_phases(runs[group], client, scenario)
            got = [f'{lab}={m[k]:.1f}' for lab, k in PHASE_METRICS[client].items() if k in m]
            print(f'  {client:9} ' + "  ".join(got))

    banner("TABLE 5 - top frames for the decisive cases")
    for case in FRAME_CASES:
        for mode in ("CPU", "ALLOC"):
            frames = prof.get((case, mode), (None, {}, []))[2]
            if not frames:
                continue
            print(f"\n{case} / {mode}")
            for pct, byt, frame in frames[:9]:
                extra = f"{byt:>9,} B/op  " if byt is not None else ""
                print(f'  {pct:5.2f}%  {extra}{frame[:88]}')


if __name__ == "__main__":
    main(sys.argv)
