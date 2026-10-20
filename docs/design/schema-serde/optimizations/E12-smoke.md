# Optimization experiment: racecar E12

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar E12: Direct CRT header array. Task 9.25. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `8c6ee8f24d14` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/E12` | `d5afded9407a` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-async, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-104924`, A/B run `20261020-105117`.

Note: CRT is the v2-async client of the harness.

JMH: none (`targets.json` has no JMH target for E12); run with `--skip-jmh`.

## Candidate

One commit on `opt/E12`, on top of the feature build: `perf(aws-crt-client): Build CRT request headers directly into an array`. It lives in the worktree `/tmp/aws-sdk-opt-E12` and is not merged.

- `CrtRequestAdapter` built the CRT headers in an `ArrayList` sized for the worst case, added the values of each header name through a stream pipeline with two lambdas, and copied the list into an `HttpHeader[]` with `toArray`. Both the async path (`toAsyncCrtRequest`) and the sync path (`toCrtRequest`) now fill the `HttpHeader[]` directly. The added `Host`, `Connection` and `Content-Length` headers are decided first, the array gets one slot for each of them and one per header name, and a `BiConsumer` passed to `forEachHeader` loops over each value list. With one value per name, the array is filled exactly and returned without a copy. Headers, order, values, and the added and skipped headers are unchanged.
- Unlike racecar, the `Host` value still comes from `getUri().getHost()`, called as before. Racecar used `SdkHttpRequest.host()`, which drops the per-request `URI` but changes behavior when `URI` cannot parse the host (see porting-deviations.md).
- Correctness: `CrtRequestAdapterHeaderDifferentialTest` compares both paths with a copy of the previous header code over 5,000 random requests (seed 20261025). The requests use mixed-case `Host`, `Connection`, `Content-Length` and `Transfer-Encoding` names and look-alikes, multi-valued, empty and non-ASCII values, empty value lists, HTTP/1.1 and HTTP/2, known, zero and unknown content lengths, IPv4, IPv6, underscore and invalid hosts, and value lists without random access. Five mutations each fail the test, including racecar's `host()` derivation. aws-crt-client: 281 tests, 0 failures, 2 skipped (existing tests unchanged); checkstyle, SpotBugs and japicmp pass.

## Reading this smoke run

The row stays `Deferred (later individual optimization)`, and measurement is pending (user decision). Ignore the verdict and the YAML change below. The v2-async arm is the CRT async client, so this run exercises `toAsyncCrtRequest`. No harness arm uses the CRT sync client, so the `toCrtRequest` change is covered only by the tests. The target metric in `targets.json` is allocation. Both pairs were lower, the two null arms differed by 285 B/op (0.4%), and the candidate saved 2,236 B/op (−3.3%). That size fits the removal of the stream pipeline and two lambdas for each header name, plus the list and the `toArray` copy (an estimate; the run does not count headers). Racecar's profile put the old list, stream and lambda site at 2,247 B/op, and its replacement builder at about 479 B/op. The candidate's builder allocates only the builder object and the exact array, and it keeps the `URI`. CPU is noise: application CPU −1.6% against a 14.1% floor. Process CPU (−10.1% against a 3.5% floor) is not credible from 2 repetitions of 300 operations, because the null arms' application CPU alone varied by 14%. A 0.7% allocation floor from 2 repetitions is not a measurement either. The full run covers small-put as well, the other target in `targets.json`: `scripts/schema-serde/optimization_experiment.sh E12 opt/E12 --pin-client 0-15 --pin-server 16-31 --skip-jmh`.

## Verdict: KEEP

Suggested decision: **Applied (re-implemented here)**.

- e2e v2-async small-get alloc_bytes_per_op: -3.3%, beyond the 0.7% floor

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-async small-get | alloc_bytes_per_op | 66,921.50 | 64,685.00 | -3.3% | 0.7% | gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-async small-get | app_cpu_us_per_op | 93.70 | 92.15 | -1.6% | 14.1% | ok |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "E12"
  # keep optimization, component and task as they are
  decision: "Applied (re-implemented here)"
  classes: ["<fully qualified class that implements it>"]
  applied_in_phase: 5
  reason: "<existing reason>; 9.19 experiment: v2-async small-get alloc_bytes_per_op -3.3% (floor 0.7%)"
  ab_record: "optimizations/E12-smoke.md"
```

## Null experiment (e2e)

### Paired A/B summary `20261020-104924`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `8c6ee8f24d14` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `8c6ee8f24d14` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 107.6 ± 6,238.9 (2) | 114.6 ± 13,432.7 (2) | 1.056 | ±14.1% | 1/2 | 14.1% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 200.7 ± 21,517.7 (2) | 200.5 ± 30,239.4 (2) | 0.987 | ±10.0% | 1/2 | 10.0% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 194.9 ± 21,103.9 (2) | 194.9 ± 29,825.6 (2) | 0.988 | ±10.2% | 1/2 | 10.2% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 66,907.5 ± 149,287.2 (2) | 67,192.5 ± 56,977.4 (2) | 1.004 | ±0.7% | 1/2 | 0.7% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 253.3 ± 21,231.2 (2) | 256.7 ± 25,464.8 (2) | 1.010 | ±3.5% | 1/2 | 3.5% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-105117`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `8c6ee8f24d14` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `opt/E12` | `d5afded9407a` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 93.7 ± 827.6 (2) | 92.2 ± 477.5 (2) | 0.984 | ±3.1% | 1/2 | 14.1% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 164.3 ± 1,432.4 (2) | 161.1 ± 1,496.1 (2) | 0.980 | ±0.1% | 2/2 | 10.0% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 159.6 ± 1,496.1 (2) | 156.2 ± 1,527.9 (2) | 0.979 | ±0.1% | 2/2 | 10.2% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 66,921.5 ± 221,861.7 (2) | 64,685.0 ± 30,557.7 (2) | 0.967 | ±0.8% | 2/2 | 0.7% | -3.3% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-async | small-get | 233.3 ± 6,366.2 (2) | 210.0 ± 10,631.5 (2) | 0.899 | ±4.7% | 2/2 | 3.5% | -10.1% (beyond the floor) |
