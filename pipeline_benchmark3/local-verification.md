# pipeline_benchmark3 — local verification, before any numbers are collected

What this records: that the four arms run, that each one really runs the pipeline it is labelled with,
and that they return the same data — checked locally on the laptop. No timing here; the laptop cannot
resolve the effects this comparison is about. Timing comes from the dedicated host.

## Why this verification is not optional

The smithy-java bridge replaces the generated client's execution path but keeps **every class name and
Maven coordinate that stock V2 uses**. A bridged and a stock `DynamoDbClient` therefore cannot coexist
on one classpath, and are distinguishable only by which jar the JVM loaded. Nothing about a successful
run would look wrong if the jars were swapped: an arm would report one pipeline's numbers under the
other's name, and every table downstream would be quietly wrong.

## Setup

Four jars, **one harness commit** (`ac977512cbe`), so the SDK under test is the only difference between
arms. Each is process-isolated — the harness already runs one JVM per (client, scenario, repetition).

| jar | SDK | arms it serves |
|---|---|---|
| `bench3-racecar` | this lineage's optimized SDK, `2.54.4-SNAPSHOT` local | `v2-sync`, `v2-async`, `v1`, `smithy` |
| `bench3-bridged` | bridge prototype `c1d88972e18`, installed as **`2.46.11-SMITHY-BRIDGED`** | `v2-bridged` |
| `bench3-stock246` | published **2.46.10** — the bridge's own baseline | `v2-sync` |
| `bench3-stock254` | published **2.54.0** — the optimization work's baseline | `v2-sync`, `v2-async` |

The bridge SDK was built in a disposable `git worktree` with `versions:set 2.46.11-SMITHY-BRIDGED`, so
the main checkout is untouched and `~/.m2` is unambiguous. That mattered: `~/.m2` already contained a
plain `2.46.11-SNAPSHOT` from earlier unrelated work, which a same-version install would have collided
with, and a SNAPSHOT could in principle be re-resolved from a remote.

**Both baselines are included deliberately.** The bridge prototype is based on 2.46.11 and the
optimization work on 2.54.4 — eight minor versions apart. A bare bridged-vs-optimized number would
conflate the bridge with whatever changed in stock V2 across those releases. With both published
baselines measured, that drift is a quantity in the report rather than a confound in it.

## Check 1 — the pipeline each arm runs (`PipelineCheck`)

Two independent checks per arm, before it is measured, because they fail differently:

1. **Wiring** — does the loaded generated client hold a `SmithyBridgeClient` field? Definitive about
   which build was loaded; needs no request.
2. **Execution** — a probe call with a stack-capturing credentials provider, asked which framework
   frames are on the stack inside the call. Definitive about which path a call actually takes, which is
   the claim a benchmark makes. Codegen routes operations through the bridge unconditionally
   (`return smithyClient.invoke(...)`, no fallback), so wiring implies execution today; the probe is
   what would catch that ceasing to be true.

The probe uses a throwaway client, so nothing is added to the measured path.

Results — 21 positive cases and 4 negative cases, all as intended:

```
### positive: each arm on its own jar
OK    bridged    v2-bridged  small-get      pipeline verified: SYNC BRIDGED
OK    bridged    v2-bridged  small-put      pipeline verified: SYNC BRIDGED
OK    bridged    v2-bridged  batch-get      pipeline verified: SYNC BRIDGED
OK    bridged    v2-bridged  batch-put      pipeline verified: SYNC BRIDGED
OK    bridged    v2-bridged  describe-table pipeline verified: SYNC BRIDGED
OK    racecar    v2-sync     small-get      pipeline verified: SYNC STOCK
OK    racecar    v2-async    small-get      pipeline verified: ASYNC STOCK
OK    racecar    v1          small-get      (no pipeline check: non-V2 arm)
OK    racecar    smithy      small-get      (no pipeline check: non-V2 arm)
OK    stock246   v2-sync     small-get      pipeline verified: SYNC STOCK
OK    stock254   v2-sync     small-get      pipeline verified: SYNC STOCK
OK    stock254   v2-async    small-get      pipeline verified: ASYNC STOCK
### negative: wrong jar for the arm must fail
OK    racecar    v2-bridged  rejected as designed
OK    stock246   v2-bridged  rejected as designed
OK    stock254   v2-bridged  rejected as designed
OK    bridged    v2-sync     rejected as designed
```

Two things this established along the way:

- **The bridged build's asynchronous client is stock.** The bridge is sync-only, so `v2-async` on the
  bridged jar verifies `ASYNC STOCK`. That makes it available as a same-baseline async control if one
  is wanted later.
- **The check had a defect of its own, now fixed.** It inspected the synchronous client regardless of
  which client the arm used, so an async arm was verified against the wrong object — failing with a
  misleading message on a bridged build and passing for the wrong reason on a stock one. It is now
  parameterized by client kind. A verification that is not itself tested is not a verification.

## Check 2 — do the pipelines return the same data (`ResponseDigest`)

Timing is only comparable if the arms produce the same result: a path that dropped members or returned
a partially materialized response would simply look fast, and the timing harness would never notice
because it discards responses. `ResponseDigest` renders GetItem, BatchGetItem and DescribeTable field
by field with map keys sorted, taken from every jar against one shared canned server.

**Three stock builds agree exactly.** `racecar` vs `stock246` vs `stock254` — identical digests across
eight minor versions of stock V2 *and* the whole optimization stack. That is an independent
confirmation that the optimization work did not change observable results.

**The bridge differs, on DynamoDB NULL attributes:**

```
stock  : nullField=AttributeValue(NUL=true) ... listField=[..., AttributeValue(NUL=true)]
bridged: nullField=AttributeValue()         ... listField=[..., AttributeValue()]
```

A `NULL` attribute comes back with **no member set at all** instead of `NUL=true`, both as a top-level
attribute and nested inside a list. For a caller that means `av.nul()` returns `null` rather than
`TRUE`, and `av.type()` reports the value as unset rather than as `NUL`. Everything else in all three
responses — binary, bool, number, string, sets, nested maps, 25 batch items, and all 45 DescribeTable
structures including timestamps and enums — is identical.

This is a compatibility defect in the bridge's handling of the `AttributeValue` union's `NULL` member,
not a measurement problem, and it is small enough not to move any timing. It is recorded here because
it belongs in the bridge's compatibility ledger, and because it is the kind of difference that a
timing-only comparison would have shipped without noticing.

## What is still to do

Timing, allocation and CPU profiles on the dedicated host, written up as `results.md` in the shape of
`pipeline_benchmark2/analysis/crosssdk-254/report.md`.
