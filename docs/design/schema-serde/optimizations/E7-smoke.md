# Optimization experiment: racecar E7

> **PRELIMINARY — smoke data, not measurements.** Tiny settings on an unquiet host; the verdict below only shows that the procedure runs end to end. Do not apply its YAML change.

Racecar E7: Whitespace-skip fast path. Task 9.22. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` | `42e61724e43f` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/E7` | `49205e395d85` | 2.49.7-SNAPSHOT |

E2E: reps 2, iterations 300, warmup 200, clients v2-sync, scenarios small-get, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64, Apple M4 Pro, 14 logical cores. Null run `20261020-093520`, A/B run `20261020-093748`.

JMH: jmh-null-a `feature-a.json`, jmh-null-b `feature-b.json`, jmh-candidate `candidate.json`.

Note: Racecar measured -3.4% on batch-get; measured here against the ported JSON reader.

## Candidate

One commit on `opt/E7`, on top of the feature build: `perf(codecs): Move the JSON whitespace-skip loop out of line`. It lives in the worktree `/tmp/aws-sdk-opt-E7` and is not merged.

- Half of racecar E7 was already in the ported reader. smithy-java's `skipWhitespace`, ported in task 7.1, returns at once when the next byte is `> ' '`, which is racecar's one-comparison check. The candidate adds the other half: the loop moves to `JsonReadUtils.skipWhitespaceSlow`, so `skipWhitespace` shrinks from 55 to 22 bytecodes, under HotSpot's `MaxInlineSize` (35). Behavior is unchanged.
- Correctness: `JsonReadUtilsWhitespaceDifferentialTest` compares the new method with the previous one (copied into the test) for every byte value after whitespace runs of length 0–40, at every `end` including ends that cut a run short, and on 2,000 random buffers at every position. `JsonWhitespacePaddingTest` pads every gap between tokens (randomly, with each whitespace byte, with 64-byte runs, and alternately). It requires the same result as the compact text through the structure, list, map, unknown-member, document and scalar readers, from a byte array and from a `ByteBuffer` slice. Other control bytes, and U+00A0, are still rejected in every gap. json-codec: 953 tests, 0 failures, 5 skipped. Protocol tests: awsJson1_0 68/0/6, awsJson1_1 132/0/0, restJson1 264/0/7 (passed/failed/skipped).

## Reading this smoke run

The row stays `Deferred (later individual optimization)`, and measurement is pending (user decision). Ignore the suggested decision below. The e2e numbers are noise at 2 × 300 operations. The one JMH case does show a slowdown: the candidate was 4.8% slower (204.7 µs/op) than the two feature runs that bracket it (195.3 and 194.1 µs/op), with 1 fork and 2 × 1 s iterations. To rule out a run-order effect, the same two jars were run again by hand in the order candidate, feature, candidate, feature, with the same settings: 200.3, 191.1, 200.2 and 188.5 µs/op, so the candidate was 4.8–6.2% slower in every pair. E6 showed the same direction (+7.1%). One possible cause, not checked: when every call site inlines the stub, C2's inlining budget for the large readers (`readStruct`, `readList`, `readStringMap`) runs out sooner, and something else stops being inlined. `-XX:+PrintInlining` on both builds would show it. Full measurement command, on a quiet host: `scripts/schema-serde/optimization_experiment.sh E7 opt/E7 --pin-client 0-15 --pin-server 16-31`.

## Verdict: REVERT

Suggested decision: **Rejected**.

- jmh schema.SchemaUnmarshallBenchmark.unmarshall [payload=LARGE,protocol=AWS_JSON_1_0] score: +4.8% is within the 0.6% floor or worse
- jmh schema.SchemaUnmarshallBenchmark.unmarshall [payload=LARGE,protocol=AWS_JSON_1_0] gc.alloc.rate.norm: regressed +0.0%, beyond the 0.0% floor

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| jmh | schema.SchemaUnmarshallBenchmark.unmarshall [payload=LARGE,protocol=AWS_JSON_1_0] | score | 195.28 | 204.71 | +4.8% | 0.6% | no gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 58.90 | 58.55 | -0.6% | 19.0% | ok |
| e2e | v2-sync small-get | alloc_bytes_per_op | 63,898.50 | 63,954.50 | +0.1% | 0.3% | ok |
| jmh | schema.SchemaUnmarshallBenchmark.unmarshall [payload=LARGE,protocol=AWS_JSON_1_0] | gc.alloc.rate.norm | 804,710.47 | 804,747.78 | +0.0% | 0.0% | regression |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "E7"
  # keep optimization, component and task as they are
  decision: "Rejected"
  component: "—"
  reason: "<existing reason>; 9.19 experiment: schema.SchemaUnmarshallBenchmark.unmarshall [payload=LARGE,protocol=AWS_JSON_1_0] score +4.8% (floor 0.6%)"
  ab_record: "optimizations/E7-smoke.md"
```

The candidate is not kept: do not merge its commit (or `git revert` it if it is already on the branch) before the next optimization is tried.

## JMH runs (feature, candidate, feature again)

| benchmark | params | metric | feature-a | candidate | feature-b |
|---|---|---|---:|---:|---:|
| software.amazon.awssdk.benchmark.schema.SchemaUnmarshallBenchmark.unmarshall | payload=LARGE,protocol=AWS_JSON_1_0 | score | 195.283 ± - | 204.706 ± - | 194.135 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaUnmarshallBenchmark.unmarshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.alloc.rate | 3,929.933 ± - | 3,748.461 ± - | 3,952.421 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaUnmarshallBenchmark.unmarshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.alloc.rate.norm | 804,710.474 ± - | 804,747.777 ± - | 804,695.431 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaUnmarshallBenchmark.unmarshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.count | 18.000 ± - | 17.000 ± - | 18.000 ± - |
| software.amazon.awssdk.benchmark.schema.SchemaUnmarshallBenchmark.unmarshall | payload=LARGE,protocol=AWS_JSON_1_0 | gc.time | 10.000 ± - | 9.000 ± - | 9.000 ± - |

## Null experiment (e2e)

### Paired A/B summary `20261020-093520`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `HEAD` | `42e61724e43f` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `HEAD` | `42e61724e43f` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 70.3 ± 5,220.3 (2) | 56.4 ± 1,177.7 (2) | 0.810 | ±9.6% | 2/2 | 19.0% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 153.8 ± 12,987.0 (2) | 113.3 ± 4,583.7 (2) | 0.744 | ±7.3% | 2/2 | 25.6% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 152.8 ± 12,859.7 (2) | 112.6 ± 4,551.8 (2) | 0.743 | ±7.3% | 2/2 | 25.7% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 64,066.5 ± 6,047.9 (2) | 63,942.0 ± 73,847.8 (2) | 0.998 | ±0.3% | 1/2 | 0.3% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 143.3 ± 2,132.7 (2) | 108.3 ± 3,183.1 (2) | 0.757 | ±7.4% | 2/2 | 24.3% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261020-093748`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `42e61724e43f` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `opt/E7` | `49205e395d85` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 300, warmup 200, reps 2, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "21.0.2" 2024-01-16, 50f265ec2e1e, Darwin arm64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 58.9 ± 63.7 (2) | 58.5 ± 222.8 (2) | 0.994 | ±0.6% | 2/2 | 19.0% | no measurable difference |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 128.4 ± 1,591.5 (2) | 125.9 ± 636.6 (2) | 0.981 | ±1.6% | 2/2 | 25.6% | no measurable difference |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 127.6 ± 1,623.4 (2) | 125.1 ± 636.6 (2) | 0.980 | ±1.7% | 2/2 | 25.7% | no measurable difference |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 63,898.5 ± 25,146.5 (2) | 63,954.5 ± 9,867.6 (2) | 1.001 | ±0.1% | 0/2 | 0.3% | no measurable difference |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 106.7 ± 2,132.7 (2) | 111.7 ± 1,050.4 (2) | 1.047 | ±2.5% | 0/2 | 24.3% | no measurable difference |
