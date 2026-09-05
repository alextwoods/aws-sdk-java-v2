#!/usr/bin/env python3
"""Summarize the *timing* of retried calls from an error sweep run with a deterministic backoff.

This is the companion to error_behavior_diff.py and answers a different question. That script asks
whether the arms *behave* the same and deliberately treats wall_ms as a 2x signal, because under v2's
default backoff wall_ms is close to worthless: the delay is exponential with full jitter, so a single
sample of a throttled 3-attempt call is uniform on roughly [0, 3 s) and two arms running byte-identical
policy routinely differ by 4x. Averaging that needs hundreds of reps to resolve anything.

Run the sweep with `--backoff fixed:MS` or `--backoff immediate` (and `--warmup`) and the jitter is gone,
at which point wall_ms is a measurement and this script reports it as one. Two readings:

  * With `fixed:MS`, `implied delay` should be `(attempts - 1) * MS` in every arm. It is a falsifiable
    check that the customer's configured backoff survives bridging -- a dropped, halved or defaulted
    backoff is a flat offset, not something to be teased out of a distribution.
  * With `immediate`, the delay is zero and wall time *is* the cost of retrying: token bookkeeping,
    classification, and re-running serialization, signing and endpoint resolution once per attempt. That
    number is structurally absent from the timing benchmarks, where nothing fails.

`per extra attempt` divides the excess over the 1-attempt control by the number of retries, which is the
comparable figure across faults that retry a different number of times.

Refuses to run on a `standard`-backoff sweep, because the output would look like a measurement and would
not be one.

Usage: retry_cost_summary.py RUNDIR
"""
import csv
import statistics as stats
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path


def load(rundir):
    path = rundir / "errors.csv"
    if not path.exists():
        raise SystemExit(f"no errors.csv under {rundir}")
    with path.open() as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise SystemExit("errors.csv has no data rows")
    if "backoff" not in rows[0]:
        raise SystemExit("errors.csv has no `backoff` column; re-run the sweep with a newer script")
    unit = "us" if "wall_us" in rows[0] else "ms"
    modes = {r["backoff"] for r in rows}
    if modes == {"standard"}:
        raise SystemExit(
            "this run used the default jittered backoff, where wall_ms cannot be read as a timing.\n"
            "Re-run with --backoff immediate --warmup (retry cost) or --backoff fixed:MS (backoff "
            "fidelity), or use error_behavior_diff.py for behavior.")
    if len(modes) > 1:
        raise SystemExit(f"run mixes backoff modes {sorted(modes)}; cannot compare")
    return rows, modes.pop(), unit


def describe(values):
    return {
        "n": len(values),
        "median": stats.median(values),
        "mean": stats.fmean(values),
        "sd": stats.stdev(values) if len(values) > 1 else 0.0,
        "min": min(values),
        "max": max(values),
    }


def main(argv):
    if len(argv) != 1:
        raise SystemExit(__doc__)
    rundir = Path(argv[0])
    rows, mode, unit = load(rundir)

    arms = list(OrderedDict.fromkeys(r["jar"] for r in rows))
    walls = defaultdict(list)
    attempts = defaultdict(set)
    for r in rows:
        key = (r["fault"], r["case"], r["jar"])
        walls[key].append(int(r["wall_us" if unit == "us" else "wall_ms"]))
        attempts[key].add(int(r["attempts"]))

    cases = list(OrderedDict.fromkeys((r["fault"], r["case"]) for r in rows))

    # The 1-attempt control is the floor: everything above it is retry work plus extra round trips.
    control = {}
    for arm in arms:
        key = ("none", "control", arm)
        if key in walls:
            control[arm] = stats.median(walls[key])

    out = []
    out.append(f"# Retry timing — `{rundir.name}`")
    out.append("")
    out.append(f"Backoff mode: **`{mode}`**. Arms: {', '.join(f'`{a}`' for a in arms)}. "
               f"Times in **{'microseconds' if unit == 'us' else 'milliseconds'}**.")
    out.append("")
    if mode == "immediate":
        out.append("Delay is zero, so wall time is the cost of *retrying* — no sleep in it.")
    else:
        out.append(f"Delay is fixed and unjittered, so `implied delay` must be `(attempts-1) x "
                   f"{mode.split(':')[1]} ms` in every arm.")
    if control:
        out.append("")
        out.append("Warm 1-attempt control (the floor every row below stands on): "
                   + ", ".join(f"`{a}` {control[a]:.0f} {unit}" for a in arms if a in control))
    out.append("")

    header = (f"| fault | case | arm | attempts | n | median {unit} | mean {unit} | sd | min | max "
              f"| over control | per extra attempt |")
    out.append(header)
    out.append("|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    for fault, case in cases:
        for arm in arms:
            key = (fault, case, arm)
            if key not in walls:
                continue
            d = describe(walls[key])
            att = sorted(attempts[key])
            att_s = str(att[0]) if len(att) == 1 else ",".join(map(str, att))
            over = per = "-"
            if arm in control:
                excess = d["median"] - control[arm]
                over = f"{excess:.0f}"
                retries = att[0] - 1 if len(att) == 1 else 0
                if retries > 0:
                    per = f"{excess / retries:.1f}"
            out.append(f"| `{fault}` | {case} | `{arm}` | {att_s} | {d['n']} | {d['median']:.0f} "
                       f"| {d['mean']:.1f} | {d['sd']:.1f} | {d['min']} | {d['max']} | {over} | {per} |")

    # Arm-vs-arm. Rank-based on purpose: these samples are heavy-tailed -- a GC pause or a scheduler
    # hiccup puts a single rep 20x above the median -- so anything built on the standard deviation
    # reports "indistinguishable" for an effect that is plainly there in every rep. `P(faster)` and a
    # bootstrap interval on the median difference are both immune to that.
    base, *others = arms
    out.append("")
    out.append(f"## `{base}` vs each other arm")
    out.append("")
    out.append(f"`P(faster)` is the chance a random sample from the arm beats a random `{base}` sample; "
               "0.5 is indistinguishable, 1.0 is total separation. `95% CI` is a bootstrap interval on "
               "the difference of medians — it excludes 0 exactly when the difference is real at this "
               "sample size. Both are rank-based, because a handful of GC outliers make the standard "
               "deviation useless here while barely moving the median.")
    out.append("")
    out.append(f"| fault | case | arm | median {unit} | vs base | ratio | P(faster) | 95% CI |")
    out.append("|---|---|---|---:|---:|---:|---:|---:|")
    signs = defaultdict(int)
    real = defaultdict(int)
    for fault, case in cases:
        bk = (fault, case, base)
        if bk not in walls:
            continue
        bs = walls[bk]
        bmed = stats.median(bs)
        for arm in others:
            k = (fault, case, arm)
            if k not in walls:
                continue
            os_ = walls[k]
            omed = stats.median(os_)
            delta = omed - bmed
            ratio = omed / bmed if bmed else float("nan")
            faster = sum(1 for a in os_ for b in bs if a < b)
            ties = sum(1 for a in os_ for b in bs if a == b)
            p_faster = (faster + 0.5 * ties) / (len(os_) * len(bs))
            lo, hi = bootstrap_median_diff(os_, bs)
            signs[arm] += 1 if delta < 0 else (-1 if delta > 0 else 0)
            if hi < 0 or lo > 0:
                real[arm] += 1
            out.append(f"| `{fault}` | {case} | `{arm}` | {omed:.0f} | {delta:+.0f} "
                       f"| {ratio:.2f}x | {p_faster:.2f} | [{lo:+.0f}, {hi:+.0f}] |")

    # Counting the significant rows rather than just the sign, because the sign alone tells the wrong
    # story on a fidelity run: `fixed:MS` differences are a millisecond of call work, so an arm can win
    # every case while the finding is that the arms *agree*. A CI that excludes 0 is the thing worth
    # counting, and a run where none do is a real result, not a failed one.
    out.append("")
    n_cases = len([c for c in cases if (c[0], c[1], base) in walls])
    for arm in others:
        lower = (n_cases + signs[arm]) // 2
        sig = real[arm]
        line = (f"`{arm}` has the lower median in {lower} of {n_cases} cases, and the difference clears "
                f"the bootstrap interval in {sig} of {n_cases}.")
        if sig == 0:
            line += (" Nothing here separates the arms — which is the expected result for a backoff "
                     "fidelity run, where the delay dominates and both arms take it from the same code.")
        elif lower == n_cases and sig == n_cases:
            line += (" One direction in every case with every interval clear: if the arms were "
                     "equivalent each case would be a coin flip, so no single row has to carry this.")
        out.append(line)

    print("\n".join(out))


def bootstrap_median_diff(a, b, reps=2000, seed=17):
    """Percentile bootstrap 95% CI for median(a) - median(b). Seeded, so reruns agree."""
    import random
    rng = random.Random(seed)
    diffs = []
    for _ in range(reps):
        ra = [a[rng.randrange(len(a))] for _ in range(len(a))]
        rb = [b[rng.randrange(len(b))] for _ in range(len(b))]
        diffs.append(stats.median(ra) - stats.median(rb))
    diffs.sort()
    return diffs[int(0.025 * reps)], diffs[int(0.975 * reps) - 1]


if __name__ == "__main__":
    main(sys.argv[1:])
