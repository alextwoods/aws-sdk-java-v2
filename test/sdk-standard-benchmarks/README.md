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
accepted difference. Three work-boundary differences remain, and the profiling pass
measures each one (see below):

- A1-objects: the Baseline_Build builds its legacy marshaller objects (marshaller,
  builder, context, endpoint lookup) inside the timed method on every call; the feature
  resolves its `ClientProtocol` once per trial.
- A2: the feature's unmarshall also checks the status and attaches the `SdkHttpResponse`
  and the response metadata, which the legacy client does in its response handler. The
  awsQuery request-id read is in both timed methods (the legacy unmarshaller's
  `parseMetadata`, the feature's `readRequestId`), so it is not A2.
- A5: the feature's Query marshall includes form encoding and Content-Length, which the
  legacy client does after its marshaller.

The legacy marshaller constructor also does request work, recorded as A1-request: the
`SdkHttpFullRequest` builder, URI, static headers and query parameters, and the JSON
generator or XML writer. The feature does the same work inside `serializeRequest`, so
A1-request is not a boundary difference and stays in the baseline.

The script builds each commit in its own worktree (`$TMPDIR/aws-sdk-serde-ab-<sha12>`,
Maven repository `<worktree>-m2` in front of `~/.m2/repository`) after overlaying
`src/main/resources/codegen-resources` and `serde-tests` from `--bench-src` (default
`HEAD`), so every arm reads the same models and payloads (`--no-overlay` builds them as
committed). `--sdk-pin feature=<sha>` fails the run unless the feature commit differs from
that SDK commit only under `test/sdk-standard-benchmarks/`, `scripts/`, `docs/` or `.kiro/`.
For a prebuilt jar whose commit is not in a local repository (a benchmark host), the check
made when the jar was built is used: its provenance must record an `ok` pin of the same SDK
commit.

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
`taskset -c` and exits with status 2 where `taskset` is not available (Linux only); JMH
forks inherit the affinity, and verify checks that the JMH runner and a fork run on exactly
those CPUs (`verify/pin-affinity-<arm>.txt`). The summary
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
the work-boundary items A1-objects, A1-request, A2 and A5 per combination to
`boundary.tsv` (format 2; the frame rules are in the `serde_ab_profile.py` docstring).
`compare --out-dir DIR` writes these files to DIR instead of the run directory, to
recompute a fetched run without changing it. Given that file,
`serde_ab_summary.py summarize --boundary` names the rows where an item is at least 10%
of either arm and adds a comparable estimate: each arm's ns/op and B/op scaled by one
minus its CPU and allocation share of A1-objects, A2 and A5 (A1-request stays in the
baseline), per row and as per-class and per-protocol geometric means (a protocol is the
geometric mean of its marshall and unmarshall class geomeans). A reference table with
every item removed (serde-only, A1 fully removed) follows; it overstates the baseline's
removal. A format-1 `boundary.tsv` from earlier versions (columns `A1_*`, `A2_*`, `A5_*`)
still gives the old serde-only estimate. With `--profile` after timing, `summary.md`
includes it. The estimate is approximate (the shares come from the profiling run, and the
A5 rule covers percent-encoding only).

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

### Param subsets, abbreviated null experiments and steady state

`--param-subset ends` runs, per class, only the smallest and the largest test case by
payload size; `--params 'Class=v1,v2;Class2=v3'` names the cases of a class explicitly
(and overrides `ends` for it). Each class's JMH runs get `-p <param name>=<cases>`, so the
subset of one class never affects another. Payload size
(`serde_ab_checks.py payload-sizes`, from the jar alone): the test-case file a class reads
is the `serde-tests/...json` literal in its `.class` file; a marshall case's size is the
UTF-8 length of its `params` object as compact JSON with sorted keys, an unmarshall case's
size is the UTF-8 length of `response.body` plus every response header name and value. On a
tie, the smallest is the earliest case in `@Param` order and the largest the latest. The
script prints the chosen cases with their sizes and records them in `params.tsv`
(`param_subset`, `params_selected`, `combos`) and every case's size in `payload-sizes.tsv`.
Over the 10 serde classes `ends` selects 20 of the 71 combinations. The verify code-path
runs and the profiling pass use the selected cases too.

A comparison summarized with `--null` from such an abbreviated null experiment gives each
row without a null row of its own an inferred floor: the largest floor of the null rows of
its class, or of all null rows when its class has none, marked `inferred (class max)` or
`inferred (all max)`. A null summary also checks warmup: from the JMH JSON per-iteration
data, per fork, the change from the first to the last measurement iteration and the
least-squares trend over the iterations; a row is flagged `DRIFT` when the mean over its
forks of either exceeds max(its ns/op floor, 5%).

### Running on a remote metal host

`scripts/schema-serde/serde-ab-remote.sh` runs the A/B on a Linux benchmark host (an EC2
metal instance with Amazon Linux 2023) from jars built locally; the host needs no checkout
and no Maven. Build the jars on the laptop (the overlay and `--sdk-pin` are evaluated here
and recorded in each jar's provenance), then drive the host from bash:

```bash
# Local: build both jars and run the checks
scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature=<feature sha> \
  --sdk-pin feature=<SDK sha> --verify-only --out ~/.cache/aws-sdk-java-v2/serde-ab-metal
JARS=~/.cache/aws-sdk-java-v2/serde-ab-metal/jars

export SERDE_AB_REMOTE_TARGET=ec2-user@<host> SERDE_AB_REMOTE_KEY=<key.pem>
R=scripts/schema-serde/serde-ab-remote.sh
$R setup      # JDK 25, python3, async-profiler 4.4, perf_event_paranoid=1; host-info.txt
$R deploy --jar "$JARS/baseline.jar" --jar "$JARS/feature.jar"
$R start /tmp/serde-null.sh   # a local bash script, run detached on the host
$R status; $R log 20
$R wait 1500                  # exit 1 while still running: repeat under your timeout
$R fetch latest               # <main checkout>/.agents/tasks/serde-benchmark-metal/runs/<run-id>/
$R stop
```

`deploy` copies the scripts to `~/serde-ab/repo/scripts/schema-serde/` and the jars with
their `.provenance.properties` and `.resources.tsv` to `~/serde-ab/jars/`, then runs the
Python self-tests on the host. `start` refuses while a session, `serde-ab.sh` or a
benchmark JVM is running. `fetch` copies the whole run directory, `host-info.txt` and the
session logs. Use absolute jar paths in session scripts (`status` and `stop` find the
benchmark JVMs by them). Nothing the run starts listens on a non-loopback address: the
only listener is JMH's runner-to-fork link on `127.0.0.1`.

Recommended parameters on a 64-core Graviton metal host (the null experiment's floors apply
to an A/B only with the same options):

```bash
# serde-null.sh: abbreviated null experiment, 20 combos (ends of each class), about 2 h
J=$HOME/serde-ab/jars
OPTS=(--reps 4 --pin 32-47 --jmh-opts "-f 2 -wi 5 -w 2s -i 5 -r 2s"
      --jvm-args "-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
      --out "$HOME/serde-ab/out")
"$HOME/serde-ab/repo/scripts/schema-serde/serde-ab.sh" --null "feature=$J/feature.jar" \
  --sdk-pin feature=<SDK sha> --param-subset ends "${OPTS[@]}"

# Full A/B: all 71 combos, both arms, the same options, about 7 h
"$HOME/serde-ab/repo/scripts/schema-serde/serde-ab.sh" --jar "baseline=$J/baseline.jar" \
  --jar "feature=$J/feature.jar" --sdk-pin feature=<SDK sha> "${OPTS[@]}"

# Profiling pass: cpu + alloc, both arms, all params, default -f 1 -wi 2 -w 2s -i 3 -r 3s,
# about 1.3 h. Verify alone (--verify-only) takes about 10 min.
"$HOME/serde-ab/repo/scripts/schema-serde/serde-ab.sh" --jar "baseline=$J/baseline.jar" \
  --jar "feature=$J/feature.jar" --sdk-pin feature=<SDK sha> --profile-only --pin 32-47 \
  --jvm-args "-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4" \
  --out "$HOME/serde-ab/out"

# Locally, after fetch: the A/B with the null's floors (inferred for the rows it skipped)
python3 scripts/schema-serde/serde_ab_summary.py summarize <runs>/<ab>/serde-ab.json \
  --null <runs>/<null>/serde-ab.json --boundary <runs>/<profile>/boundary.tsv
```

Each JMH invocation is one class: `-f 2 -wi 5 -w 2s -i 5 -r 2s -prof gc -rf json -foe true`
in the classes' SampleTime mode, arms alternating per repetition (4 repetitions). The JVM
flags cap the GC and JIT threads (the JVM defaults on 64 cores are about 43 GC and 18
compiler threads), and the pinning keeps every JVM on the same 16 cores.

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
