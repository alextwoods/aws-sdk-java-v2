# SDK Standard Benchmarks

This module contains JMH microbenchmarks for the AWS SDK for Java v2, covering
endpoint resolution and serialization/deserialization (serde) across all major
AWS protocol types.

## Building

```bash
mvn clean install -P quick -pl :sdk-standard-benchmarks --am
```

The `--am` flag builds all upstream dependencies (codegen plugin, protocol modules,
etc.). You only need it on the first build or after upstream changes. Subsequent
builds can omit it:

```bash
mvn install -P quick -pl :sdk-standard-benchmarks
```

## Endpoint Resolution Benchmarks

Benchmarks for the standard endpoint resolution pipeline (`ruleParams()` →
`resolveEndpoint()`) for S3 and Lambda. These exercise the same code path that
runs during a real SDK API call.

| Class | Service | Test Cases |
|---|---|---|
| `S3EndpointResolverBenchmark` | S3 | 5 (virtual host, path style, S3 Express, access point ARN, outposts ARN) |
| `LambdaEndpointResolverBenchmark` | Lambda | 2 (standard region, GovCloud with FIPS + DualStack) |

```bash
java -jar target/benchmarks.jar S3EndpointResolverBenchmark
java -jar target/benchmarks.jar LambdaEndpointResolverBenchmark
```

## Serde Benchmarks

Benchmarks for serialization (marshaling) and deserialization (unmarshalling)
across five AWS protocol types. Each protocol has a pair of benchmark classes
parameterized by test case ID via JMH `@Param`.

| Protocol | Marshall Class | Unmarshall Class | Input Cases | Output Cases |
|---|---|---|---|---|
| JSON RPC 1.0 | `JsonRpc10MarshallBenchmark` | `JsonRpc10UnmarshallBenchmark` | 33 | 18 |
| AWS Query | `QueryMarshallBenchmark` | `QueryUnmarshallBenchmark` | 33 | 18 |
| REST JSON | `RestJsonMarshallBenchmark` | `RestJsonUnmarshallBenchmark` | 18 | 14 |
| REST XML | `RestXmlMarshallBenchmark` | `RestXmlUnmarshallBenchmark` | 18 | 14 |
| RPC v2 CBOR | `RpcV2CborMarshallBenchmark` | `RpcV2CborUnmarshallBenchmark` | 33 | 18 |

Serde benchmarks use `@BenchmarkMode(Mode.SampleTime)` instead of `AverageTime`.
SampleTime collects per-invocation latency samples, which gives us percentile
data (p50, p90, p95, p99) needed by the cross-language output schema.

### Running serde benchmarks

```bash
# All serde benchmarks (all protocols, all test cases)
java -jar target/benchmarks.jar ".*serde.*"

# Single protocol — marshall only
java -jar target/benchmarks.jar JsonRpc10MarshallBenchmark

# Single protocol — unmarshall only
java -jar target/benchmarks.jar QueryUnmarshallBenchmark

# Quick smoke test: 1 fork, short warmup/measurement
java -jar target/benchmarks.jar JsonRpc10MarshallBenchmark -f 1 -wi 1 -w 1s -i 1 -r 3s -foe true
```

### Producing the cross-language output JSON

The benchmarks produce standard JMH output. To convert it to the cross-language
`output_schema.json` format used for comparison with other SDK implementations
(Ruby, TypeScript, etc.):

```bash
# 1. Run benchmarks and write JMH results as JSON
java -jar target/benchmarks.jar ".*serde.*" -rf json -rff results.json

# 2. Convert to cross-language format (produces output.json and output.md)
java -cp target/benchmarks.jar \
  software.amazon.awssdk.benchmark.serde.JmhResultConverter \
  results.json output
```

This produces two files:
- `output.json` — the cross-language JSON schema format
- `output.md` — a rendered Markdown table for easy viewing

The output JSON has this structure:

```json
{
  "metadata": {
    "lang": "Java",
    "software": [["smithy-java", "TODO"], ["AWS SDK for Java", "TODO"]],
    "os": "TODO",
    "instance": "TODO",
    "precision": "-9"
  },
  "serde_benchmarks": [
    {
      "id": "awsJson1_0_GetItemInput_Baseline",
      "n": 5,
      "mean": 1234,
      "p50": 1200,
      "p90": 1500,
      "p95": 1600,
      "p99": 1800,
      "std_dev": 150
    }
  ]
}
```

The `metadata` fields marked `TODO` should be filled in for the target environment
before publishing results.

## Serde A/B against the Baseline_Build

`scripts/schema-serde/serde-ab.sh` compares the `benchmark.serde.*` classes (10 classes,
71 benchmark/param combinations) between builds made from git refs, typically the
Baseline_Build (`fc8b74e9fe21`) and the feature branch. Both jars have the same class
names, `@Param` lists and JMH annotations, so every result has the same key on both arms.

What the timed method runs differs by design. On the Baseline_Build it calls the legacy
marshaller or unmarshaller (`JsonProtocolMarshaller`, `XmlProtocolMarshaller`,
`QueryProtocolUnmarshaller`, ...) over the `SdkField` POJOs; marshallers are built per
call. On the feature branch it calls `ClientProtocol.serializeRequest` /
`deserializeResponse` (`AwsJson1Protocol`, `RestJsonClientProtocol`,
`RestXmlClientProtocol`, `AwsQueryClientProtocol`, `RpcV2CborProtocol`) over the ported
codecs, with the protocol and operation resolved once per trial. That is the path feature
clients use when `stagedClientProtocols` is enabled; clients generated without it (four of
the five serde services here, and services such as S3) still run the legacy marshallers.
The legacy protocol modules are identical in both builds, so benchmarking them would not
measure the new codecs. Inputs and responses are built exactly as on the Baseline_Build.
`SerdeBenchmarkEquivalenceTest` checks, for all 71 combinations, that the schema path
produces the legacy path's output on the same input; its allowlist documents every
accepted difference. Three work-boundary differences remain: the Baseline_Build builds
its legacy marshaller inside the timed method on every call (A1), the feature's unmarshall
also checks the status and attaches the `SdkHttpResponse` and the response metadata (A2),
and the feature's Query marshall includes form encoding and Content-Length, which the
legacy client does after its marshaller (A5). The profiling pass measures their share,
and the summary can subtract it (see below).

The script builds each commit in its own worktree (`$TMPDIR/aws-sdk-serde-ab-<sha12>`,
Maven repository `<worktree>-m2` in front of `~/.m2/repository`) after overlaying
`src/main/resources/codegen-resources` and `serde-tests` from `--bench-src` (default
`HEAD`), so every arm reads the same models and payloads (`--no-overlay` builds them as
committed). `--sdk-pin feature=<sha>` fails the run unless the feature commit differs from
that SDK commit only under `test/sdk-standard-benchmarks/`, `scripts/` or `docs/`.

Before timing, it verifies (`scripts/schema-serde/serde_ab_checks.py`) that the resources
and the intermediate models of the jars are identical, that `SerdeWireDump.java` (the
legacy path over each jar) prints the same requests and responses, and that each arm runs
the expected code path: legacy on the first arm, schema on the others, from an
async-profiler profile when async-profiler is installed and always from a
`-Xlog:class+load` log. `KNOWN` lines in the check outputs are value-checked differences
documented in the script (a greedy `{Key+}` label and two unused rest-xml namespaces in
the feature's intermediate models). Any other difference fails the run.

```bash
# Build both arms and run the checks only
scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature=HEAD \
  --sdk-pin feature=cabc26b41aa --verify-only

# Smoke run (reuses the jars), then its null experiment
scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature=HEAD --skip-build \
  --sdk-pin feature=cabc26b41aa --reps 2 --jmh-opts "-wi 1 -i 1 -f 1 -w 1s -r 1s"
scripts/schema-serde/serde-ab.sh --null feature=HEAD --skip-build \
  --sdk-pin feature=cabc26b41aa --reps 2 --jmh-opts "-wi 1 -i 1 -f 1 -w 1s -r 1s"

# Full run (class annotations: 5 x 2 s warmup, 10 x 5 s measurement, 3 forks), pinned on Linux
scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature=HEAD --skip-build \
  --sdk-pin feature=cabc26b41aa --reps 2 --pin 2-7

# Apply a null experiment's floors and a profiling pass's work-boundary shares to a comparison
python3 scripts/schema-serde/serde_ab_summary.py summarize <run>/serde-ab.json \
  --null <null-run>/serde-ab.json --boundary <profile-run>/boundary.tsv
```

Arm order alternates per repetition (A,B then B,A; `--order fixed` keeps A,B). Every JMH
run uses `-foe true -prof gc -rf json` and the same `--jmh-opts` and `--jvm-args`
(default `-Xms2g -Xmx2g -XX:+AlwaysPreTouch`). `--pin CPUS` wraps every `java` in
`taskset -c` and exits with status 2 where `taskset` is not available. The summary
reports mean ns/op and `gc.alloc.rate.norm` per arm with 99.9% errors, the paired ratio,
the floor from a null experiment (`--null`: one jar as `LABEL-a` and `LABEL-b`) and a
verdict, plus geometric-mean ratios per class.

`--profile` (after timing) or `--profile-only` runs a separate async-profiler pass over
every class, param, arm and event (`--profile-events`, default `cpu,alloc`;
`--profile-jmh-opts`, default `-f 1 -wi 2 -w 2s -i 3 -r 3s`) with
`-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` on both arms.
`scripts/schema-serde/serde_ab_profile.py` then writes `profile-comparison.md`: per class
and combination, the CPU and allocation share, ns/op and B/op of each logical-work
category (codec write, codec read, member traversal, HTTP request construction, ...) side
by side, the top frames, and flags for categories or frames present in one arm only or
more than 2x apart. It also checks the runtime code path of every cpu profile (every class,
param and arm; `profile-path.tsv`, and a failure fails the run) and writes the share of
the work-boundary items A1, A2 and A5 per combination to `boundary.tsv`. Given that file,
`serde_ab_summary.py summarize --boundary` names the rows where an item is at least 10%
of either arm and adds a serde-only estimate: each arm's ns/op and B/op scaled by one
minus the items' CPU and allocation share, per row and as geometric means. With
`--profile` after timing, `summary.md` includes it. The estimate is approximate (the
shares come from the profiling run, and the A5 rule covers percent-encoding only).

```bash
scripts/schema-serde/serde-ab.sh --jar baseline=<out>/jars/baseline.jar \
  --jar feature=<out>/jars/feature.jar --profile-only --classes RestJsonMarshallBenchmark
```

Output, under `--out` (default `~/.cache/aws-sdk-java-v2/serde-ab`): `jars/<label>.jar`
with `<label>.jar.provenance.properties` (ref, commit, bench-src, overlay, SDK pin of the
build, jar sha256, JDK, Maven, host; not edited after the build) and
`<label>.jar.resources.tsv`; and per run `<run-id>/` with `arms.tsv`, `params.tsv`,
`runs.tsv`, `provenance/<label>.properties` (the jar's record with this run's SDK pin
result), `verify/verify.md`, `jmh/`, `logs/`, `serde-ab.json`, `summary.md`,
`profile/<arm>/<Class>/<param>/<event>/` (collapsed stacks and flame graphs),
`profile-comparison.md`, `profile-path.tsv` and `boundary.tsv`.

## Schema Serde Benchmarks
Benchmarks for the schema-based serde path (`software.amazon.awssdk.benchmark.schema`).
They exist only on the feature branch, so they have no Baseline_Build counterpart; the
`benchmark.serde.*` A/B above is the comparison with the Baseline_Build.
`SchemaMarshallBenchmark` runs `ClientProtocol.serializeRequest` from the generated input
to the `SdkHttpFullRequest`; `SchemaUnmarshallBenchmark` runs
`ClientProtocol.deserializeResponse` from the response bytes (built by the
Response_Generator of `client-mock-plugin`) to the generated output. Neither signs nor
sends. Both are parameterized by `protocol` (`RPCV2_CBOR`, `AWS_JSON_1_0`, `AWS_JSON_1_1`,
`REST_JSON_1`, `REST_XML`, `AWS_QUERY`, `EC2_QUERY`) and `payload` (`SMALL`, at most 1 KB;
`LARGE`, at least 100 KB), and report µs/op (`AverageTime`).
The services are generated from the C2J models in
`src/main/resources/schema-codegen-resources` (one per protocol, same shapes,
`stagedClientProtocols`). The payload has a timestamp, a blob, a nested structure, a list
of structures and a map of structures; each list item has a string map, a double list, a
timestamp, a blob and a nested structure with a string list. `SchemaSerdeFixtureTest`
checks the body sizes and that each protocol reads the payload back.
`TraitLookupBenchmark` (`Schema.getTrait(TraitKey)`) and `SchemaExtensionBenchmark`
(`Schema.getExtension` after the first computation) must allocate 0 B/op.
`ZeroAllocationBenchmarkTest` runs them with the GC profiler in the module build (about
15 seconds) and fails above 1 B/op.
```bash
# Full run with bytes per op (class defaults: 5 warmup and 5 measurement iterations, 2 forks)
java -jar target/benchmarks.jar 'benchmark\.schema\.' -prof gc -rf json -rff schema-serde.json
# One protocol and size
java -jar target/benchmarks.jar SchemaMarshallBenchmark -p protocol=AWS_JSON_1_0 -p payload=LARGE -prof gc
# Smoke run
java -jar target/benchmarks.jar 'benchmark\.schema\.' -wi 1 -i 1 -f 1 -w 1s -r 1s -prof gc
```

### S3 Event Notifications

`S3EventNotificationBenchmark` compares the S3 Event Notifications library on schemas
(`S3EventNotification.fromJson(String)` and `toJson()`, task 11.19) with the `JsonNode` reader
and `JsonWriter` writer it replaced (`implementation`: `LEGACY`, `SCHEMA`). The previous
classes are in the library's test sources, so the build copies them from
`services-custom/s3-event-notifications/src/test/java` unchanged, together with the valid JSON
samples of its test suite (`sampleSet`: `ALL_SAMPLES`, the 50 samples per operation, or
`OBJECT_CREATED_PUT`, one typical event). Setup fails if the two implementations read a sample
differently; `S3EventNotificationBenchmarkTest` checks the fixture in the module build.

The result feeds the Library_Integration_Analysis
(`scripts/schema-serde/library_integration_analysis.py`):

```bash
# Full run (class defaults), written where the analysis reads it; then regenerate the analysis
java -jar target/benchmarks.jar S3EventNotificationBenchmark -prof gc -rf json \
  -rff ../../docs/design/schema-serde/library-integration-jmh.json
# Smoke run (the analysis marks it PRELIMINARY)
java -jar target/benchmarks.jar S3EventNotificationBenchmark -wi 1 -i 2 -f 1 -w 1s -r 1s -prof gc
```
## General JMH options

```bash
# List all available benchmarks
java -jar target/benchmarks.jar -l

# Custom warmup/measurement: 3 warmup iterations, 5 measurement iterations, 2 forks
java -jar target/benchmarks.jar -wi 3 -i 5 -f 2

# Custom warmup/measurement times: 
java -jar target/benchmarks.jar -f 1 -w 1s -r 3s 

# Fail on error (useful for CI)
java -jar target/benchmarks.jar -foe true
```

Each benchmark class has default JMH annotations (`@Warmup`, `@Measurement`, `@Fork`)
tailored for stable results. Override them on the command line as needed for your
environment.
