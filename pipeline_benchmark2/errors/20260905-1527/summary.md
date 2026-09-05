# Backoff fidelity — `20260905-1527`

**Result: the customer's configured backoff survives bridging exactly.** With `--backoff fixed:200`,
`per extra attempt` is **203.0 ms in every arm and every case** — 200 ms of configured delay plus ~3 ms
of call work — and no arm-vs-arm difference clears its bootstrap interval. See `retry_cost.md`.

`--faults internal-error,throughput-exceeded --reps 5 --backoff fixed:200 --warmup`. Two faults on
purpose, one throttling and one not, because they take *different backoff paths*: `treatAsThrottling` is
the one classification decision `SdkRetryStrategy.of` genuinely rewires, so it selects between
`throttlingBackoffStrategy` and `backoffStrategy`. Both are pinned to the same fixed 200 ms here, so if
the rewiring dropped a strategy or fell back to a default the two faults would disagree with each other
or with 203.0. They don't.

## Why this run exists, and why it is not a statistical one

An earlier sweep (`20260905-0255`) showed `throughput-exceeded` at 2,785 ms in `baseline` against 729 ms
in `bridge` on the persistent case, and the *reverse* on the transient one (346 vs 2,632 ms). A 4x
spread that changes sign looks like a policy difference and was flagged as unresolved.

It is not one, and the resolution came from reading the code rather than from more samples. `javap` on
`aws-sdkv2-retries-1.6.1.jar` shows `SdkRetryStrategy.refreshRetryToken` to be pure delegation: it
rebuilds a v2 `RefreshRetryTokenRequest` from (token, failure, suggestedDelay), calls the wrapped v2
`RetryStrategy.refreshRetryToken`, and returns v2's `delay()` verbatim. `acquireInitialToken` and
`maxAttempts` are the same shape. **Delay computation is v2's in both arms and cannot differ by
construction** — the only rewiring is `retryOnException` and `treatAsThrottling`.

So the 4x was v2's own full jitter. v2's default backoff draws uniformly from `[0, base * 2^(n-1))`,
which for a throttled 3-attempt call at a 1 s base makes the total roughly uniform on `[0, 3 s)`. Two
arms running byte-identical policy will routinely differ by 4x on a single sample, in either direction.
Averaging that to a useful precision needs hundreds of reps per cell.

That makes statistics the wrong instrument. Removing the jitter turns the same question into a
falsifiable check that five reps settle: a dropped, halved, or defaulted backoff is a flat offset from
`(attempts-1) x 200 ms`, not something to be teased out of a distribution. Hence `--backoff fixed:MS`.

## Reading it

The cost is the flip side: this measures a configuration nobody runs in production. It says the
configured backoff is honored, and deliberately says nothing about the *default* backoff's behavior
beyond what the delegation argument above already establishes. Retry *cost* with the sleep removed is
the companion run, `20260905-1532`.

The 1 ms control difference between arms is the ordinary warm per-call gap, resolved properly in
`20260905-1532` where the unit is microseconds. At millisecond resolution on a 1–2 ms quantity it is
two adjacent buckets and means nothing; the bootstrap intervals correctly decline to call it.
