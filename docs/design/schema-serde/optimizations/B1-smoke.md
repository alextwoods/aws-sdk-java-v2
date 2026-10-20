# Optimization experiment: racecar B1

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar B1: Shallow header copy-on-write (`LowCopyListMap`). Task 9.23. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `3981a4c73e30` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/B1` | `fe81b1f809f8` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-sync, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-100641`, A/B run `20261020-100914`.

JMH: none (`targets.json` has no JMH target for B1); run with `--skip-jmh`.

## Candidate

One commit on `opt/B1`, on top of the feature build: `perf(http-client-spi): Copy only the header map on the first write after toBuilder()`. It lives in the worktree `/tmp/aws-sdk-opt-B1` and is not merged.

- `LowCopyListMap.ForBuilder` now tracks whether the value lists are shared separately from whether the map is shared. Put, remove and clear copy only the map. Append (`forInternalWriteWithListMutation`) also copies the lists. Builder reads that hand out list views (`forListViewRead`, used by `headers()`, `matchingHeaders`, `forEachHeader` and the query parameter equivalents) copy the lists first when the map is private but the lists are still shared. Racecar does not have this last step. Without it, a view taken after the first write stops seeing the builder's later appends.
- Correctness: `SdkHttpHeaderCopyOnWriteDifferentialTest` runs 1,500 random sequences of 60 operations for requests (headers and query parameters) and 1,500 for responses. It runs each operation on the real objects and on a copy of the previous deep-copy storage, then compares the built objects, the builders and up to 40 retained views. `LowCopyListMapTest` (8 tests) checks which storage is copied and when. Six mutations each fail the new tests: append on the shallow path (request headers, request query parameters, response headers), no list copy in `forListViewRead`, no list copy on append after a shallow map copy, and `forBuildable` not marking the lists shared. The unchanged `SdkHttpRequestResponseTest` passes under all six. http-client-spi: 84 tests, 0 failures; checkstyle, SpotBugs and japicmp pass. sdk-core: 10 header-heavy test classes (265 tests, 0 failures) against the candidate http-client-spi in the same reactor.

## Reading this smoke run

The row stays `Deferred (later individual optimization)`, and measurement is pending (user decision). Ignore the suggested decision below. The CPU numbers are noise at 2 × 300 operations (null floor 18.5%). The allocation result is more likely to hold: both pairs were lower, the two null arms differed by 43 B/op, and the candidate saved 815 B/op. Racecar measured a similar net saving for the same change: 1,756 B/op of per-header `ArrayList` copies removed, 925 B/op added for the map-only copy, −1.1% on sync small-get and −4.4% on async small-get. A 0.2% floor from 2 repetitions is still not a measurement. The full run covers v2-async and small-put as well, the other targets in `targets.json`: `scripts/schema-serde/optimization_experiment.sh B1 opt/B1 --pin-client 0-15 --pin-server 16-31 --skip-jmh`.

## Verdict: KEEP

Suggested decision: **Applied (re-implemented here)**.

- e2e v2-sync small-get alloc_bytes_per_op: -1.3%, beyond the 0.2% floor

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | alloc_bytes_per_op | 64,514.00 | 63,699.00 | -1.3% | 0.2% | gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 59.50 | 63.25 | +6.4% | 18.5% | ok |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "B1"
  # keep optimization, component and task as they are
  decision: "Applied (re-implemented here)"
  classes: ["<fully qualified class that implements it>"]
  applied_in_phase: 5
  reason: "<existing reason>; 9.19 experiment: v2-sync small-get alloc_bytes_per_op -1.3% (floor 0.2%)"
  ab_record: "optimizations/B1-smoke.md"
```

## Null experiment (e2e)

### Paired A/B summary `20261020-100641`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `3981a4c73e30` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `3981a4c73e30` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63.5 ± 2,896.6 (2) | 60.7 ± 2,482.8 (2) | 0.966 | ±18.5% | 1/2 | 18.5% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 134.1 ± 4,233.5 (2) | 128.8 ± 4,774.6 (2) | 0.966 | ±14.7% | 1/2 | 14.7% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 133.2 ± 4,201.7 (2) | 128.1 ± 4,742.8 (2) | 0.966 | ±14.7% | 1/2 | 14.7% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 64,335.0 ± 257,830.7 (2) | 64,378.0 ± 187,802.6 (2) | 1.001 | ±0.2% | 1/2 | 0.2% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 118.3 ± 5,315.8 (2) | 111.7 ± 3,183.1 (2) | 0.952 | ±15.5% | 1/2 | 15.5% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-100914`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `3981a4c73e30` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `opt/B1` | `fe81b1f809f8` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 59.5 ± 382.0 (2) | 63.2 ± 1,432.4 (2) | 1.064 | ±6.9% | 0/2 | 18.5% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 128.1 ± 127.3 (2) | 137.3 ± 1,114.1 (2) | 1.072 | ±2.2% | 0/2 | 14.7% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 127.2 ± 222.8 (2) | 136.4 ± 1,050.4 (2) | 1.072 | ±2.3% | 0/2 | 14.7% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 64,514.0 ± 45,836.6 (2) | 63,699.0 ± 40,743.6 (2) | 0.987 | ±0.3% | 2/2 | 0.2% | -1.3% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 91.7 ± 13,782.8 (2) | 135.0 ± 7,448.4 (2) | 1.528 | ±33.0% | 0/2 | 15.5% | +52.8% (beyond the floor) |
