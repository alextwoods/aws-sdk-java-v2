# Optimization experiment: racecar E6

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar E6: String writer, 4 characters per iteration. Task 9.21. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `0eb128b8d649` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/E6` | `b5b8def9cdfa` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-sync, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-090438`, A/B run `20261020-090709`.

JMH: jmh-null-a `feature-a.json`, jmh-null-b `feature-b.json`, jmh-candidate `candidate.json`.

Note: Racecar measured -4 to -7% on its own writer; measured here against the ported JSON writer.
## Candidate
One commit on `opt/E6`, on top of the feature build: `perf(codecs): Escape JSON strings 4 characters per iteration`. It lives in the worktree `/tmp/aws-sdk-opt-E6` and is not merged.
- `JsonWriteUtils.copyUnescapedAscii` copies the leading run of unescaped ASCII characters four at a time: the OR of the four characters against `0xFF80`, then the OR of their `NEEDS_ESCAPE` entries, and the group is stored only if both pass. A per-character loop finishes the tail and finds the exact rejection point. `tryWriteQuotedAscii` and the prefix of `writeQuotedStringGeneral` share it; the escape slow path is unchanged.
- Correctness: `JsonWriteUtilsDifferentialTest` compares every entry point with the previous per-character writer (copied into the test) on whole buffers. It covers 2,000 mixed random strings of length 0–1000, every special character at every position for lengths 1–24, and unescaped ASCII of every length from 0 to 1000. json-codec: 950 tests, 0 failures, 5 skipped. Protocol tests: awsJson1_0 68/0/6, awsJson1_1 132/0/0, restJson1 264/0/7 (passed/failed/skipped).
## Reading this smoke run
The row stays `Deferred (later individual optimization)`, and measurement is pending (user decision). Ignore the suggested decision below. The e2e numbers are noise at 2 × 300 operations. The one JMH case does hint at a slowdown, though: the candidate was 7.1% slower (157.4 µs/op) than two feature runs that bracket it and agree within 0.1% (147.0 and 147.1 µs/op), with 1 fork and 2 × 1 s iterations. The full measurement should check this before any decision. One possible cause, not checked: C2 already unrolls the plain per-character loop, and the grouped loop adds work. Full measurement command, on a quiet host: `scripts/schema-serde/optimization_experiment.sh E6 opt/E6 --pin-client 0-15 --pin-server 16-31`.

## Verdict: REVERT

Suggested decision: **Rejected**.

- jmh schema.SchemaMarshallBenchmark.marshall [payload=LARGE,protocol=AWS_JSON_1_0] score: +7.1% is within the 0.1% floor or worse
- e2e v2-sync small-get alloc_bytes_per_op: regressed +0.5%, beyond the 0.2% floor
- jmh schema.SchemaMarshallBenchmark.marshall [payload=LARGE,protocol=AWS_JSON_1_0] gc.alloc.rate.norm: regressed +0.0%, beyond the 0.0% floor

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| jmh | schema.SchemaMarshallBenchmark.marshall [payload=LARGE,protocol=AWS_JSON_1_0] | score | 147.01 | 157.45 | +7.1% | 0.1% | no gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 73.75 | 73.20 | +6.4% | 13.9% | ok |
| e2e | v2-sync small-get | alloc_bytes_per_op | 63,983.50 | 64,290.50 | +0.5% | 0.2% | regression |
| jmh | schema.SchemaMarshallBenchmark.marshall [payload=LARGE,protocol=AWS_JSON_1_0] | gc.alloc.rate.norm | 871,499.50 | 871,660.60 | +0.0% | 0.0% | regression |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "E6"
  # keep optimization, component and task as they are
  decision: "Rejected"
  component: "—"
  reason: "<existing reason>; 9.19 experiment: schema.SchemaMarshallBenchmark.marshall [payload=LARGE,protocol=AWS_JSON_1_0] score +7.1% (floor 0.1%)"
  ab_record: "optimizations/E6-smoke.md"
```

The candidate is not kept: do not merge its commit (or `git revert` it if it is already on the branch) before the next optimization is tried.

## JMH runs (feature, candidate, feature again)

| benchmark | params | metric | feature-a | candidate | feature-b |
|---|---|---|---:|---:|---:|
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=LARGE,protocol=AWS_JSON_1_0 | score | 147.011 ± - | 157.449 ± - | 147.126 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.alloc.rate | 5,652.633 ± - | 5,278.849 ± - | 5,649.170 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.alloc.rate.norm | 871,499.495 ± - | 871,660.600 ± - | 871,651.263 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.count | 26.000 ± - | 24.000 ± - | 26.000 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.time | 13.000 ± - | 12.000 ± - | 13.000 ± - |

## Null experiment (e2e)

### Paired A/B summary `20261020-090438`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `0eb128b8d649` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `0eb128b8d649` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 70.8 ± 1,623.4 (2) | 61.1 ± 3,469.6 (2) | 0.861 | ±6.5% | 2/2 | 13.9% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 150.7 ± 4,138.0 (2) | 126.9 ± 7,162.0 (2) | 0.841 | ±5.4% | 2/2 | 15.9% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 149.5 ± 4,010.7 (2) | 126.1 ± 7,066.5 (2) | 0.842 | ±5.5% | 2/2 | 15.8% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 64,475.5 ± 198,306.8 (2) | 64,378.5 ± 234,594.1 (2) | 0.998 | ±0.1% | 2/2 | 0.2% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 131.7 ± 5,315.8 (2) | 100.0 ± 19,098.6 (2) | 0.748 | ±25.5% | 2/2 | 25.5% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-090709`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `0eb128b8d649` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `opt/E6` | `b5b8def9cdfa` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 73.8 ± 8,944.5 (2) | 73.2 ± 8,021.4 (2) | 1.064 | ±52.8% | 1/2 | 13.9% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 156.2 ± 17,188.7 (2) | 158.8 ± 17,825.3 (2) | 1.080 | ±51.7% | 1/2 | 15.9% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 155.2 ± 17,125.1 (2) | 157.8 ± 17,761.7 (2) | 1.080 | ±51.9% | 1/2 | 15.8% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63,983.5 ± 18,780.3 (2) | 64,290.5 ± 219,315.2 (2) | 1.005 | ±0.8% | 1/2 | 0.2% | +0.5% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 143.3 ± 0.0 (2) | 141.7 ± 15,915.5 (2) | 0.989 | ±24.7% | 1/2 | 25.5% | no measurable difference |
