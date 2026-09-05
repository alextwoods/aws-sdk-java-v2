# Retry cost — `20260905-1532`

**Result: retrying costs the bridge slightly less per attempt than v2, but its advantage is
front-loaded — most of the per-call saving does not recur on retries.** See `retry_cost.md`.

`--faults internal-error,throughput-exceeded,unavailable,empty-500,malformed-body,resource-not-found
--reps 60 --backoff immediate --warmup --pin-client 0,1 --pin-server 4,5,6`. 1,560 rows, 0 failures,
7 behavioral differences (all previously catalogued; see `diff.md`).

With the delay set to zero, wall time *is* the cost of retrying — token bookkeeping, classification, and
re-running serialization, signing and endpoint resolution once per attempt. That number is structurally
absent from the timing benchmarks, where nothing fails.

## The decomposition

| quantity | `baseline` | `bridge` | ratio |
|---|---:|---:|---:|
| warm 1-attempt call (control) | 1,110 µs | 669 µs | **0.60x** |
| non-retryable error, 1 attempt (`resource-not-found`) | 1,176 µs | 764 µs | **0.65x** |
| marginal cost of one extra attempt (median of 10 cases) | 675 µs | 578 µs | **0.86x** |
| total, 3-attempt cases | 2,253–2,550 µs | 1,710–1,984 µs | 0.72–0.80x |

A two-parameter model — a fixed per-execution cost plus a per-attempt cost — predicts the totals to
within the noise, which is the check that the two rates above are real and not an artifact of how the
excess was divided:

* `baseline`: 1,110 + 2 x 675 = **2,460 µs** predicted, ~2,470 µs observed (median of the 10 cases).
* `bridge`: 669 + 2 x 578 = **1,825 µs** predicted, ~1,830 µs observed.

So the bridge saves **~441 µs once per execution** and only **~97 µs on each attempt**. Roughly a fifth
of its per-call advantage recurs per retry; four fifths is one-time work. The consequence is that the
bridge's *relative* advantage decays as attempts grow: 0.60x at one attempt, 0.72–0.80x at three, and it
would keep drifting toward parity at higher attempt counts.

**The leading candidate is the endpoint bridge (§3.3), which runs the V2 rules engine per *attempt*
rather than per execution.** That is exactly the shape this measurement has — work that should be
once-per-execution being paid per attempt, eroding the advantage in proportion to retries. This is a
hypothesis consistent with the numbers, not something this run isolates; the run measures the aggregate.
Attributing it needs the memoization change, which conveniently makes this its before-measurement.

## Why the statistics are rank-based

`retry_cost_summary.py` originally reported a `separation` figure built on pooled standard deviation. It
said the arms were indistinguishable (all values under 1) while the bridge's median was lower in 13 of
13 cases by 20–40%. The sd was wrong, not the medians: these samples are heavy-tailed — a GC pause or a
scheduler hiccup puts a single rep 20x above the median, and one `unavailable` rep reached 44,056 µs
against a 1,828 µs median — so the sd is set by a handful of outliers that barely move the median.

It now reports `P(faster)` and a bootstrap interval on the difference of medians. **All 13 intervals
exclude zero**, and the direction is the same in all 13. `P(faster)` sits at 0.65–0.85 rather than near
1.0, which is the honest picture: the distributions overlap in their tails even though their centers are
firmly separated. Both statistics are immune to the outliers; the discarded one was not.

## What this does not measure

Wall time on loopback with an immediate backoff. It excludes the sleep a real retry spends (measured
separately in `20260905-1527`), network RTT, and any server-side cost, so the per-attempt figure is the
client's own work and nothing else — which is what makes it comparable between arms, and what makes it
useless as an absolute prediction of production latency.
