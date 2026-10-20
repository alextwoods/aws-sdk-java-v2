# Optimization experiment: racecar E13

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar E13: Lazy auth-property rebuild. Task 9.26. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `06add6cadd29` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/E13` | `d96199de2142` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-sync,v2-async, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-120245`, A/B run `20261020-120600`.

Note: The rebuild happens on an AuthSchemeResolutionCache miss; if the warm cache hides it, the gain is within the floor and the row becomes Rejected.

JMH: none (`targets.json` has no JMH target for E13); run with `--skip-jmh`.

## Candidate

One commit on `opt/E13`, on top of the feature build: `perf(sdk-core): Copy the auth option only for interceptor-modified properties`. It lives in the worktree `/tmp/aws-sdk-opt-E13` and is not merged.

- `AuthSchemeResolver.applyInterceptorModifiedProperties` re-applies the signer properties that interceptors changed after endpoint resolution. It copied the endpoint-resolved option with `toBuilder()` and walked the post-interceptor signer properties with a new anonymous consumer and a `boolean[]`, even when nothing had changed. It now returns before reading any property when there is no pre-interceptor snapshot or both snapshots hold the same option. For distinct snapshots it walks the properties as before, but copies the current option only at the first property whose value differs (`Objects.equals`, as before). This is the method racecar changed (`6d81ac812ae`).
- Where it runs: the targets.json note above is wrong about the cache. `AuthSchemeResolutionCache` caches the auth scheme, identity provider and signer per scheme ID, and the options are resolved on every call. The rebuild is not tied to a cache miss: `EndpointResolution` calls the method on every call, after `AuthSchemeResolutionCache.resolve`. A probe client built like the harness's (DynamoDB GetItem and PutItem, static credentials, `us-east-1`, the standard retry strategy) showed both snapshots holding the same placeholder option on every call: scheme ID `unset` with `ServiceSigningName`, `RegionName` and a null `ChecksumAlgorithm`. So every measured call takes the new early return, and the removed copy is the 3-property option with its two `HashMap`s.
- Correctness: `AuthSchemeResolverLazyRebuildDifferentialTest` compares the method with a copy of the previous code over 2,000 random sequences of 1 to 5 calls (seed 20261026). The inputs vary regions, signing names, SigV4a region sets, and the double-encode, normalize-path and checksum properties; snapshots are absent, the same option, equal copies or modified copies; and each call starts from the previous result. Four mutations each fail the test. sdk-core: 1,940 JUnit tests (6 skipped) and 651 TestNG tests (272 skipped), 0 failures; existing tests unchanged; checkstyle, SpotBugs and japicmp pass. The auth tests in `codegen-generated-classes-test` (218 tests, 1 skipped) pass.

## Reading this smoke run

The row stays `Deferred (later individual optimization)`, and measurement is pending (user decision). Ignore the verdict and the YAML change below. The target metric in `targets.json` is allocation. Allocation was lower in both pairs on both clients. v2-sync saved 356 B/op (−0.6%), but its null arms differed by 409 B/op (floor 0.9%), so the script reads it as no gain and suggests REVERT. v2-async saved 296 B/op (−0.4%) against a 181 B/op null difference (floor 0.4%). Racecar's allocation profile put the removed `doApplyInterceptorModifiedProperties` subtree at 360 B/op (sync) and 225 B/op (async), so the size of the saving matches. Two repetitions of 300 operations cannot resolve a change of about 0.5% against a floor of the same size, so this run neither confirms nor rules out the saving. CPU is noise: application CPU −2.7% (sync) and +3.9% (async) against floors of 12.2% and 13.1%. The full run needs more repetitions and small-put as well, the other target in `targets.json`: `scripts/schema-serde/optimization_experiment.sh E13 opt/E13 --pin-client 0-15 --pin-server 16-31 --skip-jmh`. If the e2e floor still hides about 300 B/op, an allocation profile of the method, as racecar used, is the direct check.

## Verdict: REVERT

Suggested decision: **Rejected**.

- e2e v2-sync small-get alloc_bytes_per_op: -0.6% is within the 0.9% floor or worse

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | alloc_bytes_per_op | 64,306.50 | 63,950.50 | -0.6% | 0.9% | no gain |
| e2e | v2-async small-get | alloc_bytes_per_op | 67,160.50 | 66,865.00 | -0.4% | 0.4% | gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 55.25 | 53.70 | -2.7% | 12.2% | ok |
| e2e | v2-async small-get | app_cpu_us_per_op | 89.25 | 92.75 | +3.9% | 13.1% | ok |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "E13"
  # keep optimization, component and task as they are
  decision: "Rejected"
  component: "—"
  reason: "<existing reason>; 9.19 experiment: v2-sync small-get alloc_bytes_per_op -0.6% (floor 0.9%); v2-async small-get alloc_bytes_per_op -0.4% (floor 0.4%)"
  ab_record: "optimizations/E13-smoke.md"
```

The candidate is not kept: do not merge its commit (or `git revert` it if it is already on the branch) before the next optimization is tried.

## Null experiment (e2e)

### Paired A/B summary `20261020-120245`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `06add6cadd29` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `06add6cadd29` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 57.1 ± 1,527.9 (2) | 55.6 ± 1,655.2 (2) | 0.977 | ±12.2% | 1/2 | 12.2% | noise floor |
| v2-async | small-get | 100.4 ± 7,098.3 (2) | 90.1 ± 509.3 (2) | 0.907 | ±13.1% | 1/2 | 13.1% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 123.4 ± 891.3 (2) | 124.4 ± 636.6 (2) | 1.008 | ±2.8% | 1/2 | 2.8% | noise floor |
| v2-async | small-get | 166.3 ± 5,634.1 (2) | 165.6 ± 318.3 (2) | 0.998 | ±7.9% | 1/2 | 7.9% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 122.7 ± 859.4 (2) | 123.8 ± 700.3 (2) | 1.010 | ±2.8% | 1/2 | 2.8% | noise floor |
| v2-async | small-get | 161.6 ± 5,379.4 (2) | 160.9 ± 413.8 (2) | 0.999 | ±8.0% | 1/2 | 8.0% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 64,310.0 ± 262,287.0 (2) | 63,901.5 ± 12,414.1 (2) | 0.994 | ±0.9% | 2/2 | 0.9% | noise floor |
| v2-async | small-get | 66,895.0 ± 161,701.2 (2) | 66,714.0 ± 34,377.4 (2) | 0.997 | ±0.4% | 1/2 | 0.4% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 141.7 ± 18,048.1 (2) | 106.7 ± 2,132.7 (2) | 0.789 | ±25.7% | 2/2 | 25.7% | noise floor |
| v2-async | small-get | 198.3 ± 13,782.8 (2) | 175.0 ± 13,814.6 (2) | 0.905 | ±29.4% | 1/2 | 29.4% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-120600`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `06add6cadd29` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `opt/E13` | `d96199de2142` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 55.2 ± 1,241.4 (2) | 53.7 ± 63.7 (2) | 0.973 | ±4.6% | 1/2 | 12.2% | no measurable difference |
| v2-async | small-get | 89.2 ± 477.5 (2) | 92.8 ± 1,050.4 (2) | 1.039 | ±1.4% | 0/2 | 13.1% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 120.9 ± 2,291.8 (2) | 122.9 ± 445.6 (2) | 1.017 | ±3.5% | 1/2 | 2.8% | no measurable difference |
| v2-async | small-get | 159.8 ± 2,833.0 (2) | 168.6 ± 2,737.5 (2) | 1.055 | ±0.4% | 0/2 | 7.9% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 120.1 ± 2,419.2 (2) | 122.3 ± 445.6 (2) | 1.019 | ±3.7% | 1/2 | 2.8% | no measurable difference |
| v2-async | small-get | 155.4 ± 2,801.1 (2) | 163.8 ± 2,673.8 (2) | 1.054 | ±0.4% | 0/2 | 8.0% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 64,306.5 ± 234,594.1 (2) | 63,950.5 ± 328,177.1 (2) | 0.994 | ±0.3% | 2/2 | 0.9% | no measurable difference |
| v2-async | small-get | 67,160.5 ± 61,433.7 (2) | 66,865.0 ± 48,383.0 (2) | 0.996 | ±0.0% | 2/2 | 0.4% | -0.4% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 110.0 ± 0.0 (2) | 106.7 ± 0.0 (2) | 0.970 | ±0.0% | 2/2 | 25.7% | no measurable difference |
| v2-async | small-get | 183.3 ± 16,965.9 (2) | 200.0 ± 12,732.4 (2) | 1.098 | ±7.1% | 0/2 | 29.4% | no measurable difference |
