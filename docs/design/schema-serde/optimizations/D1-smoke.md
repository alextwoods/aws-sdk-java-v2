# Optimization experiment: racecar D1

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar D1: User-agent prefix built once; Apache5 header filter loop. Task 9.24. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `a18b73b12c30` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/D1` | `90d38512d280` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-sync, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-102008`, A/B run `20261020-102245`.

Note: Apache5 is the v2-sync client of the harness.

JMH: none (`targets.json` has no JMH target for D1); run with `--skip-jmh`.

## Candidate

One commit on `opt/D1`, on top of the feature build: `perf(apache5-client): Filter request headers with a loop instead of a stream`. It lives in the worktree `/tmp/aws-sdk-opt-D1` and is not merged.

- `Apache5HttpRequestFactory.addHeadersToRequest` checked each header name with `IGNORE_HEADERS.stream().noneMatch(name::equalsIgnoreCase)`, which created a stream pipeline and a method reference for every header of every request. The three skipped names (`Content-Length`, `Host`, `Transfer-Encoding`) are now a static array, and `isIgnoredHeader` loops over it with the same `name.equalsIgnoreCase` comparison. Headers, order and values are unchanged.
- The user-agent half of racecar D1 is not in this class. Racecar changed sdk-core's `ApplyUserAgentStage` to build the constant prefix once per client. The schema pipeline that the harness runs (`pipeline: schema` in the tables below) already does this in `UserAgentHeaderCache` (H9), so the candidate does not change the user agent.
- Correctness: `Apache5HeaderFilterDifferentialTest` compares the Apache request headers with a copy of the previous header code over 5,000 random requests (seed 20261024). The requests mix the skipped names in mixed case and with non-ASCII letters that `equalsIgnoreCase` matches, names that only look like them, multi-valued and empty values, a user `Host` header, and standard and non-standard ports. Three mutations each fail the test: dropping `Host` from the array, comparing case-sensitively, and also skipping empty names. apache5-client: 262 tests, 0 failures, 6 skipped (existing tests unchanged); checkstyle, SpotBugs and japicmp pass.

## Reading this smoke run

The row stays `Deferred (later individual optimization)`, and measurement is pending (user decision). Ignore the verdict and the YAML change below. The v2-sync arm is the Apache5 client, so this run exercises the changed code. The target metric in `targets.json` is application CPU, and at 2 × 300 operations it is noise (−1.5% against a 6.1% floor). Process CPU (−43%) is not credible: the candidate's 61.6 µs/op is barely above its 55.9 µs/op application CPU, although process CPU also counts JIT, GC and VM threads, and the null arms were 130.0 and 116.7 µs/op. Allocation is more likely to hold. Both pairs were lower, the two null arms differed by 22 B/op, and the candidate saved 1,111 B/op (−1.7%). That size fits the removal of one stream pipeline (spliterator, pipeline head, match operation and sink) and one method reference for each header of the request (an estimate; the run does not count headers). Racecar recorded the change as an allocation win too. The full run should therefore add `alloc_bytes_per_op` as a D1 target in `targets.json`, which needs the user's agreement because it changes the 9.19 mapping, and add small-put: `scripts/schema-serde/optimization_experiment.sh D1 opt/D1 --pin-client 0-15 --pin-server 16-31 --skip-jmh`.

## Verdict: REVERT

Suggested decision: **Deferred (later individual optimization) (undecided: rerun on a quiet host)**.

- e2e v2-sync small-get app_cpu_us_per_op: -1.5% is within the 6.1% floor or worse

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 56.75 | 55.90 | -1.5% | 6.1% | no gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | alloc_bytes_per_op | 63,986.00 | 62,874.50 | -1.7% | 0.1% | ok |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "D1"
  # keep optimization, component and task as they are
  decision: "Deferred (later individual optimization)"
  reason: "<existing reason>; 9.19 experiment: v2-sync small-get app_cpu_us_per_op -1.5% (floor 6.1%)"
  ab_record: "optimizations/D1-smoke.md"
```

The candidate is not kept: do not merge its commit (or `git revert` it if it is already on the branch) before the next optimization is tried.

## Null experiment (e2e)

### Paired A/B summary `20261020-102008`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `a18b73b12c30` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `a18b73b12c30` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 62.4 ± 2,928.4 (2) | 66.1 ± 2,037.2 (2) | 1.061 | ±3.8% | 0/2 | 6.1% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 129.8 ± 5,761.4 (2) | 140.9 ± 5,793.2 (2) | 1.086 | ±0.8% | 0/2 | 8.6% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 128.9 ± 5,697.7 (2) | 140.0 ± 5,793.2 (2) | 1.087 | ±0.7% | 0/2 | 8.7% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63,995.0 ± 35,014.0 (2) | 63,973.0 ± 17,825.3 (2) | 1.000 | ±0.1% | 1/2 | 0.1% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 130.0 ± 12,732.4 (2) | 116.7 ± 2,132.7 (2) | 0.915 | ±16.3% | 1/2 | 16.3% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-102245`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `a18b73b12c30` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `opt/D1` | `90d38512d280` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 56.8 ± 668.4 (2) | 55.9 ± 573.0 (2) | 0.985 | ±0.3% | 2/2 | 6.1% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 123.5 ± 191.0 (2) | 124.5 ± 1,082.3 (2) | 1.008 | ±1.6% | 1/2 | 8.6% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 122.8 ± 222.8 (2) | 123.8 ± 1,114.1 (2) | 1.008 | ±1.6% | 1/2 | 8.7% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63,986.0 ± 636.6 (2) | 62,874.5 ± 239,687.1 (2) | 0.983 | ±0.8% | 2/2 | 0.1% | -1.7% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 108.3 ± 1,050.4 (2) | 61.6 ± 1,050.4 (2) | 0.569 | ±3.4% | 2/2 | 16.3% | -43.1% (beyond the floor) |
