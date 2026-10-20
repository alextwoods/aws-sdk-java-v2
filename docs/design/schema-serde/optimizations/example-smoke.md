# Optimization experiment: racecar E6

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar E6: String writer, 4 characters per iteration. Task 9.21. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `fed6d903057d` | 2.49.7-SNAPSHOT |
| `candidate` | `HEAD` | `fed6d903057d` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-sync, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-081030`, A/B run `20261020-081258`.

JMH: jmh-null-a `feature-a.json`, jmh-null-b `feature-b.json`, jmh-candidate `candidate.json`.

Note: Racecar measured -4 to -7% on its own writer; measured here against the ported JSON writer.

## Verdict: REVERT

Suggested decision: **Rejected**.

- jmh schema.SchemaMarshallBenchmark.marshall [payload=SMALL,protocol=AWS_JSON_1_0] score: +1.1% is within the 1.6% floor or worse
- e2e v2-sync small-get alloc_bytes_per_op: regressed +0.2%, beyond the 0.1% floor

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| jmh | schema.SchemaMarshallBenchmark.marshall [payload=SMALL,protocol=AWS_JSON_1_0] | score | 0.75 | 0.76 | +1.1% | 1.6% | no gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 57.30 | 56.05 | -2.2% | 14.3% | ok |
| e2e | v2-sync small-get | alloc_bytes_per_op | 63,965.50 | 64,069.00 | +0.2% | 0.1% | regression |
| jmh | schema.SchemaMarshallBenchmark.marshall [payload=SMALL,protocol=AWS_JSON_1_0] | gc.alloc.rate.norm | 10,360.01 | 10,360.01 | +0.0% | 0.0% | ok |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "E6"
  # keep optimization, component and task as they are
  decision: "Rejected"
  component: "—"
  reason: "<existing reason>; 9.19 experiment: schema.SchemaMarshallBenchmark.marshall [payload=SMALL,protocol=AWS_JSON_1_0] score +1.1% (floor 1.6%)"
  ab_record: "optimizations/example-smoke.md"
```

The candidate is not kept: do not merge its commit (or `git revert` it if it is already on the branch) before the next optimization is tried.

## JMH runs (feature, candidate, feature again)

| benchmark | params | metric | feature-a | candidate | feature-b |
|---|---|---|---:|---:|---:|
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=SMALL,protocol=AWS_JSON_1_0 | score | 0.751 ± - | 0.759 ± - | 0.763 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=SMALL,protocol=AWS_JSON_1_0 | gc.alloc.rate | 13,155.421 ± - | 13,013.460 ± - | 12,950.104 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=SMALL,protocol=AWS_JSON_1_0 | gc.alloc.rate.norm | 10,360.005 ± - | 10,360.005 ± - | 10,360.027 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=SMALL,protocol=AWS_JSON_1_0 | gc.count | 37.000 ± - | 35.000 ± - | 37.000 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaMarshallBenchmark.marshall | payload=SMALL,protocol=AWS_JSON_1_0 | gc.time | 18.000 ± - | 17.000 ± - | 18.000 ± - |

## Null experiment (e2e)

### Paired A/B summary `20261020-081030`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `fed6d903057d` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `fed6d903057d` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 58.8 ± 954.9 (2) | 60.4 ± 2,801.1 (2) | 1.030 | ±14.3% | 1/2 | 14.3% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 128.4 ± 2,419.2 (2) | 128.4 ± 5,920.6 (2) | 1.003 | ±14.4% | 1/2 | 14.4% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 127.2 ± 2,100.8 (2) | 127.7 ± 5,952.4 (2) | 1.006 | ±14.1% | 1/2 | 14.1% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63,861.5 ± 17,507.0 (2) | 63,918.0 ± 22,918.3 (2) | 1.001 | ±0.0% | 0/2 | 0.1% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 111.7 ± 1,050.4 (2) | 115.0 ± 3,183.1 (2) | 1.031 | ±8.5% | 1/2 | 8.5% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-081258`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `fed6d903057d` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `(prebuilt jar)` | `fed6d903057d` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 57.3 ± 445.6 (2) | 56.0 ± 222.8 (2) | 0.978 | ±0.8% | 2/2 | 14.3% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 122.9 ± 2,673.8 (2) | 124.3 ± 636.6 (2) | 1.012 | ±3.7% | 1/2 | 14.4% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 122.0 ± 2,610.1 (2) | 123.5 ± 668.4 (2) | 1.014 | ±3.6% | 1/2 | 14.1% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63,965.5 ± 42,971.8 (2) | 64,069.0 ± 7,639.4 (2) | 1.002 | ±0.1% | 0/2 | 0.1% | +0.2% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 110.0 ± 2,100.8 (2) | 110.0 ± 2,100.8 (2) | 1.000 | ±0.0% | 0/2 | 8.5% | no measurable difference |
