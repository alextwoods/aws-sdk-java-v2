# Standalone E2E DynamoDB Benchmarks

Fixed-iteration DynamoDB benchmarks comparing four client stacks against an **out-of-process**
mock HTTP server:

| Client     | SDK                              | HTTP transport            | Call model              |
|------------|----------------------------------|---------------------------|-------------------------|
| `v1`       | AWS SDK for Java V1 (1.12.797)   | Apache HttpClient 4.x     | Blocking                |
| `v2-sync`  | AWS SDK for Java V2 (2.54.2)     | Apache HttpClient 4.x (default) | Blocking          |
| `v2-async` | AWS SDK for Java V2 (2.54.2)     | AWS CRT async client      | CompletableFuture + `join()` per call |
| `smithy`   | smithy-java (1.5.1), generated client | SmithyHttpClient (HTTP/1.1) | Blocking          |

This package supersedes the profiling setup in `test/benchmark-smithy-java` /
`pipeline_benchmark/` for DynamoDB, fixing its fairness issues:

1. **Retries** — previously smithy-java ran `RetryStrategy.noRetries()` while V2 ran its default
   strategy (per-call token acquisition and attempt accounting). Now every client is explicitly
   configured for standard token-bucket retries with **3 max attempts**: V2 uses
   `RetryMode.STANDARD`, smithy-java uses `StandardRetryStrategy.builder().maxAttempts(3)`, and
   V1 caps its default DynamoDB retry policy at `maxErrorRetry=2`. The server always returns 200,
   so no client actually retries — but all of them pay comparable retry bookkeeping per call.
2. **Payload wrapping** — the old S3 workload copied a 1 MB payload per call via
   `RequestBody.fromBytes` while smithy-java's `DataStream.ofBytes` only wrapped. This package is
   DynamoDB-only, where no raw-payload wrapper exists: request bodies are marshalled from model
   objects by each SDK on every call, and that marshalling is part of what we measure. The
   asymmetry does not apply here (see caveats).
3. **In-JVM mock server** — previously Jetty ran inside the benchmark JVM and contributed 32–60%
   of small-op allocation samples. The server now runs in its own JVM (`MockDdbServer`), launched
   and torn down by `scripts/benchmark.sh`, so client profiles contain only client work.

## Mock server vs. DynamoDB Local

Both were evaluated as the out-of-process target:

| | Canned-response mock server (chosen) | DynamoDB Local |
|---|---|---|
| Response bytes | Byte-identical for every SDK and every run, precomputed at startup | Depend on stored state; require a seeding step (create table, put items) |
| Server-side cost | ~zero (header lookup + fixed byte write), Jetty easily sustains the small-op request rate | Real parsing, SQLite storage engine (JNI), response synthesis — competes for cores with the client on the same box and adds run-to-run variance |
| Protocol fidelity | Hand-written wire JSON; does not validate requests | Real request validation and real error behavior |
| Setup | Same jar as the runner, `READY` line + `/ping` probe | Separate artifact with native libraries, table setup before each run |

**Recommendation: the canned-response mock server.** The goal is to isolate and compare
*client-side* pipeline cost (marshalling, signing, HTTP client, unmarshalling); a server that does
real database work adds noise without adding information about the clients. DynamoDB Local remains
useful as an occasional *correctness* cross-check that every SDK round-trips real responses: the
runner takes `--endpoint`, so you can point it at a running DynamoDB Local instance (create
`benchmark-table` with hash key `pk` first; note the response sizes will then differ from the
canned ones, so don't compare those numbers against mock-server runs).

## Prerequisites

- **JDK 21+** (smithy-java requires 21; tested on 25)
- **Maven**
- The generated smithy-java DynamoDB client published to `~/.m2` (one-time):

  ```bash
  cd ../benchmark-smithy-java/smithy-java-dynamodb-client
  ./gradlew publishToMavenLocal
  ```
- For `--profile cpu|alloc|wall`: async-profiler (`brew install async-profiler`, or set
  `ASYNC_PROFILER_LIB` to the library path)

## Quick start

```bash
cd test/standalone-e2e-benchmarks

# Smoke test (~2 s): all four scenarios, small iteration count
./scripts/benchmark.sh --client v2-sync --iterations 200 --warmup 50

# Robust run of one scenario
./scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000

# All clients, for a comparison table
for c in v1 v2-sync v2-async smithy; do
  ./scripts/benchmark.sh --client $c --iterations 100000 | grep RESULT
done
```

`scripts/benchmark.sh` builds on first use, starts the mock server as a child process, waits for its
`/ping` readiness probe, runs the client JVM, and kills the server on exit.

## Scenarios

| Scenario    | Operation      | Request                             | Canned response                 |
|-------------|----------------|-------------------------------------|---------------------------------|
| `small-get` | GetItem        | 1 key (~66 B body)                  | `{"Item": ...}`, 470 B          |
| `small-put` | PutItem        | 12-attribute mixed item (~0.5 KB)   | `{}`, 2 B                       |
| `batch-get` | BatchGetItem   | 25 keys (~1.5 KB)                   | 25 × ~2 KB items, ~38 KB        |
| `batch-put` | BatchWriteItem | 25 × ~2 KB items (~50 KB)           | `{"UnprocessedItems":{}}`, 23 B |

Item content is deterministic (seeded), defined once in `BenchmarkItems` and converted to each
SDK's model types plus the wire-format JSON the server returns — so all SDKs marshal/unmarshal
structurally identical data.

## CLI reference

### Runner options (passed through by `scripts/benchmark.sh`)

```
--client X            SDK under test: v1, v2-sync, v2-async, smithy (required)
--scenario X[,Y...]   small-get, small-put, batch-get, batch-put, or all (default: all)
--iterations N        measured operations per scenario (default: 10000)
--warmup N            unmeasured warmup operations per scenario (default: min(2000, iterations))
--endpoint URL        server endpoint (default: http://127.0.0.1:19080)
--metrics             collect SDK-internal metrics, print per-scenario summary to stdout
--metrics-file PATH   write metric summaries to PATH instead of stdout (implies --metrics)
--progress-seconds N  progress/ETA print interval; 0 disables (default: 5)
```

### Launcher-only options (`scripts/benchmark.sh`)

```
--port N            port for the auto-launched mock server (default: 19080)
--no-server         don't launch a server (requires --endpoint)
--profile MODE      jfr | cpu | alloc | wall
--profile-out DIR   profiler output directory (default: ./profiles)
--jvm-args "..."    extra JVM args for the client JVM
```

### Standalone server

```bash
./scripts/server.sh [--port N]     # foreground; prints "READY port=N pid=..." when up
```

Useful for running the server on a separate machine/core set, or keeping one server up across
many client runs (`benchmark.sh --no-server --endpoint http://host:port`).

## Output

Progress lines print at the configured interval:

```
progress small-get 9,938/20,000 (49.7%) 4969 ops/s eta 2s
```

One `RESULT` line per scenario:

```
RESULT client=v2-sync scenario=small-get iterations=100000 wall_ms=12000 ops_per_wall_sec=8333.3 \
       cpu_ms=9500 cpu_user_ms=9000 cpu_sys_ms=500 ops_per_cpu_sec=10526.3 \
       ops_per_user_cpu_sec=11111.1 avg_us_per_op=120.0
```

- `ops_per_wall_sec` — iterations / elapsed wall clock.
- `ops_per_cpu_sec` — iterations / **whole-process** CPU seconds (user + system, all threads
  including event loops, GC and JIT), which is the right measure when SDKs differ in thread
  usage. `ops_per_user_cpu_sec` uses user time only.
- CPU time comes from [OSHI](https://github.com/oshi/oshi) (`OSProcess` user/kernel time), which
  provides the user/system split on both Linux (`/proc/self/stat` with the real `USER_HZ`) and
  macOS (`proc_pidinfo`), at millisecond resolution. If OSHI's native path fails, the runner
  falls back to a direct `/proc/self/stat` parse (Linux) and then to
  `OperatingSystemMXBean.getProcessCpuTime()` (no split). The source in use is printed in the
  run header.

With `--metrics`, each SDK's native metric facility reports per-phase timings as `METRIC` lines
(V2 `MetricPublisher` CoreMetrics, V1 `AWSRequestMetrics`, smithy-java OTel
`OperationMetricsPlugin`). Metrics collection itself costs a little per call, so leave it off for
profiling runs.

## Profiling

```bash
# JFR (built into the JDK)
./scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 100000 --profile jfr

# async-profiler: CPU, allocation, or wall-clock flame graphs (HTML)
./scripts/benchmark.sh --client smithy --scenario batch-put --iterations 100000 --profile cpu
./scripts/benchmark.sh --client v1 --scenario small-put --iterations 100000 --profile alloc
```

Output lands in `./profiles/<client>-[mode-]<timestamp>.{jfr,html}`. Because the server is a
separate process, profiles contain only client-side work. Profile one client and one scenario per
run for clean recordings. For anything the flags don't cover, attach manually: the run header
prints the client `pid`, and `--jvm-args` accepts arbitrary JVM options.

Inspect JFR recordings with `jfr print`, JDK Mission Control, or IntelliJ.

## Implementation details, assumptions, caveats

- **Single-threaded, closed-loop.** One caller thread issues operations back-to-back; `v2-async`
  awaits each future with `join()`. This measures per-call pipeline cost, not maximum concurrent
  throughput. Async stacks pay thread-hop overhead here that a pipelined workload would amortize.
- **Request objects are prebuilt** once per workload and reused every iteration (identical policy
  for all SDKs). Marshalling model → wire bytes still happens per call in every SDK; request
  *builder* allocation does not.
- **Retry parity, not retry absence.** All clients run standard token-bucket retries capped at 3
  attempts (see table above). Since the server never fails, the measured cost is the per-call
  accounting only.
- **No raw payload wrappers.** DynamoDB has no `RequestBody`-style streaming input, so fairness
  issue #2 from the previous round doesn't arise. Every SDK serializes the same item graph per
  call; per-SDK differences in serializer allocation behavior are part of the measurement, by
  design.
- **The server doesn't parse requests.** It reads and discards the body, routes on the
  `X-Amz-Target` header, and writes precomputed bytes. Malformed-but-signed requests would still
  get a 200; use a DynamoDB Local run if you need a correctness check. Unknown targets get a 400
  so misrouted calls fail loudly.
- **No `x-amz-crc32` response header.** V1 validates it when present but V2/smithy-java do not,
  so omitting it keeps response handling symmetric.
- **CPU-time measurement.** OSHI snapshots are taken only at scenario boundaries (twice per
  scenario), so measurement overhead is negligible, but resolution is milliseconds — use runs of
  at least several seconds. Process CPU includes GC/JIT during the measured window; use generous
  warmup and iteration counts for stable numbers. The direct-procfs fallback assumes 100 ticks/s
  (override with `--jvm-args "-DclkTck=N"`).
- **Localhost transport.** All traffic is plain HTTP over loopback: no DNS, TLS, or real network.
  Numbers are pipeline overhead, not end-to-end AWS latency. Client and server share the host's
  cores; on a small machine consider running the server on separate cores (`taskset`/separate
  host with `--no-server --endpoint`).
- **Dependency pins.** V2 2.54.2 and smithy-java 1.5.1 from Maven Central; V1 via the monolithic
  `com.amazonaws:aws-java-sdk:1.12.797` artifact (as requested — the first build downloads the
  full V1 module set; swap to `aws-java-sdk-dynamodb` for a leaner tree, runtime behavior is
  identical). The smithy-java client comes from mavenLocal (see prerequisites). Netty is pinned
  to 4.2.0.Final for smithy-java compatibility.
- **No uber-jar.** Because of the V1 monolith, the build writes the resolved classpath to
  `target/classpath.txt` instead of shading; the `scripts/` files consume it. Run classes manually
  with `java -cp "target/classes:$(cat target/classpath.txt)" ...`.
- **JDK 24+ warnings.** Netty (pulled in by smithy-java) triggers a `sun.misc.Unsafe` deprecation
  warning; harmless. `--enable-native-access=ALL-UNNAMED` is always passed for the CRT client and
  async-profiler.
- **This module is not part of the repo's Maven reactor** — it benchmarks released artifacts, not
  the working tree.
