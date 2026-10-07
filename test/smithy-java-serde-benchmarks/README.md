# Stock v2 serde vs smithy-java serde

JMH benchmarks for the question this branch exists to answer: **what does replacing only the marshaller and
the response unmarshaller with smithy-java's codecs buy?** Everything else about the call is the same in both
arms — one jar, one client configuration, the stock v2 pipeline (endpoint rules, SigV4, retries, interceptors,
CRC32 validation, metrics).

The four cases are the project's standard DynamoDB ones, with the same items and the same canned response
bytes the end-to-end harness uses (`BenchmarkItems` is a copy of
`test/standalone-e2e-benchmarks`'s, trimmed to the v2 model):

| case | request | response |
|---|---|---|
| `smallGet` | `GetItem`, one key | 12 mixed-type attributes, ~0.4 KB |
| `smallPut` | `PutItem`, 12 mixed-type attributes | `{}` |
| `batchGet` | `BatchGetItem`, 25 keys | 25 × ~2 KB "user profile" rows, ~50 KB |
| `batchPut` | `BatchWriteItem`, 25 × ~2 KB rows | `{"UnprocessedItems":{}}` |

Two benchmark classes:

- **`DynamoDbClientBenchmark`** — a whole `DynamoDbClient` call against an in-process
  `CannedDynamoDbHttpClient`. No sockets and no server, so the number is client CPU per call rather than
  latency: it is what the end-to-end harness measures minus the transport, which costs both arms the same.
- **`DynamoDbSerdeBenchmark`** — the marshaller and the success response handler on their own, built exactly
  as the generated client builds them (a new marshaller per call, as the generated code does). This bounds
  what the client benchmark can show; the rest is pipeline cost common to both arms.

Three arms, selected by the `serde` parameter. JMH forks each separately, and a client reads the switch once
at construction, so a fork's JIT only ever sees one serde:

| `serde` | what runs |
|---|---|
| `v2` | v2's own marshallers and response handlers (`aws.smithyJavaSerde=false`) |
| `smithy` | smithy-java's codecs on their default JSON provider (a shaded Jackson 3) |
| `smithy-native` | smithy-java's codecs on its own JSON reader/writer (`smithy-java.json-provider=smithy`) |

`SerdeSelection.verify` fails the run if a client is not on the serde its arm claims, so an arm cannot
silently measure the same thing twice.

## Running

Install the SDK first (the benchmark resolves `2.46.11-SNAPSHOT` from `~/.m2`), then build and run here:

```bash
# from the repo root
./mvnw -q install -pl core/protocols/smithy-java-serde,codegen,codegen-maven-plugin -P quick -DskipTests
./mvnw -q clean install -pl services/dynamodb -P quick -DskipTests

cd test/smithy-java-serde-benchmarks
../../mvnw -q package -DskipTests

# everything, with allocation profiling, pinned off core 0-1 so the harness does not fight the OS
taskset -c 2-7 java -jar target/benchmarks.jar 'DynamoDb(Client|Serde)Benchmark' -prof gc \
    -rf json -rff results/$(date -u +%Y%m%d-%H%M)/jmh.json

# one case, quickly
java -jar target/benchmarks.jar DynamoDbSerdeBenchmark.marshallSmallPut -f 1 -wi 3 -i 3
```

Warmup is deliberately long (10 iterations × 2 s for the client benchmark): the first measured iteration of
`smallGet` was still 10× the steady-state number with JMH's default 5.

## Results

`results/<runid>/summary.md` holds the tables for a run; `jmh.json` the raw data, `host.txt` and
`base-commit.txt` what produced it. The headline from `results/20261007-1717` (8-core Xeon, JDK 21.0.9,
3 forks × 5 × 2 s):

- **Serialization is 33–54% cheaper** on smithy-java, 60–69% on its native JSON provider.
- **Deserialization is 55–62% cheaper** for responses with content; see the caveat below for empty ones.
- **A whole call** is 2–12% cheaper for the small cases and 37% for `batchPut`, because serde is a small
  share of a call that also resolves endpoints, signs, and runs interceptors. `batchGet` barely moves
  (−1%) for the reason in the next point.

Two results that run the other way, both worth knowing before reading the win:

- **Allocation per call goes up, not down** (+9% small, +24% `batchGet`), even though serde itself allocates
  *less*. smithy-java's `Codec` deserializes from a `byte[]`/`ByteBuffer` and has no `InputStream` entry
  point, so the response body is read whole into memory before parsing where v2 streams it. On `batchGet`
  that is a ~50 KB array per call. It is also why `batchGet`'s wall-clock win is small: the extra copy eats
  the parsing gain.
- **An empty or near-empty response body is slower** (`unmarshallSmallPut`, a `{}` body: +87%, and 3× the
  allocation). The per-response cost of standing up a smithy-java deserializer does not amortize over two
  bytes. Operations whose responses are mostly empty are the one shape where v2's serde wins.
