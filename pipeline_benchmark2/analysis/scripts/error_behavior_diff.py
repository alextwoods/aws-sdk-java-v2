#!/usr/bin/env python3
"""Diff error and retry behavior between arms of a test/standalone-e2e-benchmarks error sweep.

Unlike the timing summaries, this reports agreement rather than magnitude. Every field the probe records
is something a customer can observe — the exception class they catch, the error code they switch on, the
request ID they paste into a ticket, whether the call was retried at all — so a mismatch is a
compatibility finding, not a percentage.

Two fields are treated as approximate rather than exact. `wall_ms` is a timing and never matches
exactly; it is compared as a ratio and only reported when the arms differ by more than 2x, which is the
scale at which a *missing* backoff shows up rather than a slow machine. `message` is compared for
equality but reported separately from the rest, because SDKs routinely reword messages without any
behavioral difference and burying a real finding under wording churn would defeat the point.

Usage: error_behavior_diff.py RUNDIR
"""
import csv
import statistics
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

# Fields where any difference is a behavioral difference.
STRICT = ["outcome", "attempts", "exception", "cause", "errorCode", "statusCode", "requestId",
          "extendedRequestId", "retryable", "throttling", "clockSkew", "serviceName", "rawResponse"]
# Compared, but reported apart: wording churn is not a behavior change.
SOFT = ["message"]
# Compared as a ratio, not for equality.
TIMING = "wall_ms"
TIMING_RATIO_THRESHOLD = 2.0


def load(rundir):
    path = rundir / "errors.csv"
    if not path.exists():
        raise SystemExit(f"no errors.csv under {rundir}")
    with path.open() as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise SystemExit("errors.csv has no data rows")
    return rows


def main(argv):
    if len(argv) != 1:
        raise SystemExit(__doc__)
    rundir = Path(argv[0])
    rows = load(rundir)

    arms = list(OrderedDict.fromkeys(r["jar"] for r in rows))
    if len(arms) < 2:
        raise SystemExit(f"need at least two arms to diff; found {arms}")
    base, *others = arms

    # (arm, fault, case) -> rows across reps
    data = defaultdict(list)
    for r in rows:
        data[(r["jar"], r["fault"], r["case"])].append(r)

    cases = list(OrderedDict.fromkeys((r["fault"], r["case"]) for r in rows))

    print(f"# Error behavior diff — `{rundir.name}`\n")
    print(f"Baseline arm: `{base}`. Compared against: {', '.join(f'`{a}`' for a in others)}.")
    print(f"{len(cases)} cases, {len(rows)} rows.\n")

    # A case is only comparable if each arm agreed with itself across reps. Report that first: a
    # nondeterministic arm makes every downstream comparison unreliable, and silently picking rep 1
    # would hide it.
    unstable = []
    for arm in arms:
        for fault, case in cases:
            group = data[(arm, fault, case)]
            for field in STRICT:
                values = {r[field] for r in group}
                if len(values) > 1:
                    unstable.append((arm, fault, case, field, sorted(values)))
    if unstable:
        print("## Nondeterministic within an arm\n")
        print("Same arm, same fault, different answers across reps. Everything below is suspect for")
        print("these cases.\n")
        print("| arm | fault | case | field | values |")
        print("|---|---|---|---|---|")
        for arm, fault, case, field, values in unstable:
            print(f"| `{arm}` | `{fault}` | {case} | `{field}` | {', '.join(values)} |")
        print()
    else:
        print("Every arm gave the same answer on every rep, so each case below is a single "
              "deterministic outcome.\n")

    def first(arm, fault, case):
        group = data[(arm, fault, case)]
        return group[0] if group else None

    for arm in others:
        print(f"## `{base}` vs `{arm}`\n")

        diffs = []
        soft_diffs = []
        timing_diffs = []
        missing = []
        for fault, case in cases:
            a, b = first(base, fault, case), first(arm, fault, case)
            if a is None or b is None:
                missing.append((fault, case))
                continue
            for field in STRICT:
                if a[field] != b[field]:
                    diffs.append((fault, case, field, a[field], b[field]))
            for field in SOFT:
                if a[field] != b[field]:
                    soft_diffs.append((fault, case, field, a[field], b[field]))
            try:
                x, y = float(a[TIMING]), float(b[TIMING])
                ratio = max(x, y) / min(x, y) if min(x, y) > 0 else None
                if ratio and ratio >= TIMING_RATIO_THRESHOLD:
                    timing_diffs.append((fault, case, x, y))
            except (ValueError, ZeroDivisionError):
                pass

        if missing:
            print("Cases missing from one arm: "
                  + ", ".join(f"`{f}`/{c}" for f, c in missing) + "\n")

        if diffs:
            print(f"**{len(diffs)} behavioral difference(s).**\n")
            print(f"| fault | case | field | `{base}` | `{arm}` |")
            print("|---|---|---|---|---|")
            for fault, case, field, x, y in diffs:
                print(f"| `{fault}` | {case} | `{field}` | {x} | {y} |")
            print()
        else:
            print("**No behavioral differences** on any strictly compared field "
                  f"({', '.join('`' + f + '`' for f in STRICT)}).\n")

        if timing_diffs:
            print(f"Wall time differing by {TIMING_RATIO_THRESHOLD}x or more — read as a backoff "
                  "signal, not a latency:\n")
            print(f"| fault | case | `{base}` ms | `{arm}` ms |")
            print("|---|---|---:|---:|")
            for fault, case, x, y in timing_diffs:
                print(f"| `{fault}` | {case} | {x:,.0f} | {y:,.0f} |")
            print()

        if soft_diffs:
            print("Message wording differences (not behavioral on their own):\n")
            print(f"| fault | case | `{base}` | `{arm}` |")
            print("|---|---|---|---|")
            for fault, case, _field, x, y in soft_diffs:
                print(f"| `{fault}` | {case} | `{x}` | `{y}` |")
            print()

    print("## What each arm did\n")
    print("| fault | case | arm | attempts | outcome | exception | errorCode | status | requestId "
          "| retryable | throttling | wall ms |")
    print("|---|---|---|---:|---|---|---|---:|---|---|---|---:|")
    for fault, case in cases:
        for arm in arms:
            r = first(arm, fault, case)
            if r is None:
                continue
            exc = r["exception"].rsplit(".", 1)[-1] if r["exception"] != "-" else "-"
            print(f"| `{fault}` | {case} | `{arm}` | {r['attempts']} | {r['outcome']} | `{exc}` "
                  f"| {r['errorCode']} | {r['statusCode']} | {r['requestId']} | {r['retryable']} "
                  f"| {r['throttling']} | {r['wall_ms']} |")
    print()

    # Median wall time per arm, as a coarse check that the sweep as a whole was not run on a machine
    # doing something else at the time.
    print("Median wall time per arm across all cases: "
          + ", ".join(f"`{a}` {statistics.median(float(r['wall_ms']) for r in rows if r['jar'] == a):,.0f} ms"
                      for a in arms) + "\n")


if __name__ == "__main__":
    main(sys.argv[1:])
