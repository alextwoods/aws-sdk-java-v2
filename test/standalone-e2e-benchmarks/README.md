# Standalone E2E DynamoDB benchmarks

Fixed-iteration DynamoDB benchmarks for the V2 SDK against an out-of-process Jetty `MockDdbServer` on
`127.0.0.1`. Ported from racecar (`feature/poc/racecar`, commit `dc26e4aee93`) as benchmark tooling only: no
racecar runtime code comes with it. See `docs/design/schema-serde/porting-deviations.md` ("task 9.17") for what
was copied and what changed.

## Scenarios and clients

| Scenario | Operation | Payload |
|---|---|---|
| `small-get` | GetItem | 12 mixed-type attributes, ~0.4 KB response |
| `small-put` | PutItem | the same item in the request |
| `describe-table` | DescribeTable | structure-heavy response (46 nested structures); not part of `all` |
| `batch-get` | BatchGetItem | 25 ~2 KB items (~38 KB response) |
| `batch-put` | BatchWriteItem | 25 ~2 KB items in the request |

Clients: `v2-sync` (Apache5) and `v2-async` (CRT). Each run first makes a probe call and reports which request
pipeline the SDK on the classpath ran (`stock` = the legacy pipeline, `schema` = the schema path), in the
`RESULT` line and the `pipeline` results column.

Every results row also has `alloc_bytes_per_op`: heap bytes allocated by all client-JVM threads in the measured
window divided by the operation count (Requirement 37.9). The mock server runs in its own JVM, so its allocation
is not counted.

## Build

This module is not part of the root reactor and does not inherit the root pom, because each arm of a paired run
builds it against a different SDK build. It uses Java 21 language features, as racecar's harness does.

```
# against the SDK artifacts in the local repository (default: this branch's version)
mvn -o -f test/standalone-e2e-benchmarks/pom.xml package -Dbenchmark.phase=feature
# against another SDK version or local repository
mvn -o -f test/standalone-e2e-benchmarks/pom.xml package -Daws.sdk.v2.version=2.54.18-SNAPSHOT \
    -Dmaven.repo.local=<arm repository>
```

The shaded jar is `target/e2e-<phase>.jar`; `benchmark-provenance.properties` inside it records the phase label,
harness commit, SDK commit and SDK version.

## Run one scenario

```
test/standalone-e2e-benchmarks/scripts/benchmark.sh --jar target/e2e-feature.jar --client v2-sync \
    --scenario small-get --iterations 50000 --warmup 10000 --append-to-results-file /tmp/results.csv
```

`benchmark.sh` starts the mock server, waits for its `READY` line, runs `BenchmarkRunner` and stops the server.
`BenchmarkRunner --help` lists the runner options; `benchmark.sh` adds `--port`, `--tls`, `--profile`,
`--jvm-args`, `--server-jvm-args`, `--pin-client` and `--pin-server`. `scripts/server.sh` runs the server alone.
`ResponseDigest <endpoint>` prints every scenario's response field by field, so two arms can be diffed as text.

## Paired A/B runs

Use `scripts/schema-serde/paired-ab.sh`, which builds each arm from a git ref, interleaves the runs and writes
`paired-ab.json` for `scripts/schema-serde/paired_ab_summary.py`. See the header of each script.
