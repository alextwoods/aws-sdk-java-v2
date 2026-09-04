# smithy-java bridge work: current state

**Written:** 2026-09-04
**Purpose:** context recovery. Several parallel prototypes explored improving AWS SDK for Java v2
performance by leveraging `smithy-java`. This records what exists, on which branch, how to build it,
what it covers, and where it stops.

---

## 1. Why any of this exists

Two measurements motivate the whole effort. Both are in the tree.

**`output/profile_report.md`** (2026-06-16) — DynamoDB PutItem/GetItem through a mock HTTP client,
so no socket or connection-pool noise. Per request, single thread: ~12.8 µs CPU / ~47 KB allocated
for `getItem`, ~16.0 µs / ~53 KB for `putItem`. Bundled Jackson is the largest single SDK cost
(~10% CPU), then collection/util work, then the request pipeline and interceptor chain.

**`pipeline_benchmark2/analysis/crosssdk-254/report.md`** (2026-08-31, on `feature/poc/racecar`) —
the sharper number, and the one to quote. Published V2 2.54.0 from Maven Central versus smithy-java
1.5.1, on `c6g.metal` (64 physical cores, no SMT), JDK 25, out-of-process Jetty with canned
byte-identical responses, 96 JVM invocations, 0 failures.

Application CPU µs/op and client allocation B/op:

| scenario | V2 sync CPU | smithy CPU | ratio | V2 sync alloc | smithy alloc | ratio |
|---|---:|---:|---:|---:|---:|---:|
| small-get | 151.6 | 46.0 | **3.30×** | 60,335 | 10,551 | **5.7×** |
| small-put | 143.4 | 49.8 | **2.88×** | 54,315 | 6,839 | **7.9×** |
| batch-get | 664.7 | 343.3 | 1.94× | 535,507 | 218,304 | 2.5× |
| batch-put | 798.5 | 304.3 | 2.62× | 209,089 | 123,184 | 1.7× |

Attribution from that report: marshalling is 51% of V2's client CPU on batch-put (6.2% for smithy);
per-call framework orchestration is 23.7% on small-get (7.8%); signing costs 15× the allocation
(20.3 KB/op vs 1.4 KB/op); V2 batch-get materializes the response map twice via
`BatchGetResponseMapCopier` (119 KB/op).

That is the size of the prize. Everything below is an attempt to capture some of it.

---

## 2. Branch map

```
master  a06a1a60841  (2.54.x, 2026-08-25)
│
│  ── Approach A: re-host v2 on smithy-java ──
├── dowling/smithy-java-bridge   c6cc6d7de39  Dowling, 11 commits, Jun 14-16
│   └── smithy-java-bridge       43eab544718  + revert restoring the Gradle demo
│       └── smithy-java-bridge-alexwoo   f5ccd90216b  + Phase 1 & 2
│           └── smithy-java-spi-bridge-poc  22f9c2f828c  + SPI reframing   <-- most mature
│
│  ── Approach B: port the techniques into v2 natively ──
├── benchmark_smithy_signer  741ead3f91f   (ancestor of the next; no unique content)
│   └── fork_smithy_java     4aa652decab
├── alexwoo/sigv4_smithy-java-opts  1fee04ca245  (signer half, rebased onto 2.48)
│
│  ── Approach C: incremental v2 optimization, smithy-java as target only ──
└── feature/poc/racecar     b21313586ac  (2026-09-03, still active)
```

All the Approach A branches form one linear chain — no merges, no rebases. `dowling/smithy-java-bridge`
is a direct ancestor of `smithy-java-spi-bridge-poc`. `benchmark_smithy_signer` is a strict ancestor
of `fork_smithy_java` and can be ignored.

---

## 3. Approach A — re-host v2 on smithy-java

### 3.1 The thesis

`RFC-smithy-java-runtime.md` (on every branch in the chain) is the design document and still the best
statement of intent. Core argument: schema-based serde is architecturally incompatible with C2J. C2J
is a lossy, protocol-coupled down-projection that discards member-level trait granularity, and a
generic schema-driven codec has no per-shape escape hatch, so any wrong trait inference produces
silently wire-incorrect output. Conclusion: adopt canonical Smithy models, retarget codegen, preserve
the v2 public surface with a shim, delete the redundant runtime.

§10 records ten locked decisions: JDK 21 baseline (done), canonical Smithy models from Maven Central,
straight major-version replacement rather than a parallel artifact line, reuse the existing v2
presigner, do not preserve the five v2 HTTP clients, `services-custom/` out of scope.

Two further scoping documents: `codegen-c2j-to-smithy/IR-REMOVAL-PLAN.md` (measures the blast radius
of deleting `IntermediateModel` — 274 references, 109 of 157 generators IR-coupled) and
`test/sdk-standard-benchmarks/BRIDGE_BENCHMARK_DESIGN.md`.

### 3.2 `dowling/smithy-java-bridge` — the foundation

Three separable pieces:

- **`codegen-c2j-to-smithy/`** — a C2J → Smithy front-end reaching **422/422 byte-identical IR parity**
  across all services. Notable because it de-risks the model-source question without changing any
  generated output. `codegen-diff.sh` supports legacy-vs-smithy generated-code diffing.
- **`core/smithy-java-bridge/`** — the adapter set. `SdkSchemaFactory` builds a smithy-java `Schema`
  from a shape's existing `SDK_FIELDS`; `SdkPojoSerializer`/`SdkPojoDeserializer` drive serde from a
  precompiled plan; `V2TransportBridge` wraps a v2 `SdkHttpClient` as a smithy `ClientTransport`;
  `V2InterceptorBridge` maps a representative 4-hook subset of v2's `ExecutionInterceptor`.
- **Benchmarks** — bridge-vs-v2 serde JMH benchmarks across all five protocols in
  `test/sdk-standard-benchmarks/.../serde/bridge/`.

Root pom moves to `<jre.version>21</jre.version>`.

### 3.3 `smithy-java-bridge-alexwoo` — Phases 1 and 2

Two substantive commits (the third is just the revert restoring `smithy-java-demo/`, a standalone
Gradle project unrelated to the Maven build).

- `3ed8986a803` — codegen emits `$SCHEMA` per shape plus `ApiOperation` and `ApiService` classes
  (`ApiOperationSpec`, `ApiServiceSpec`); generated POJOs implement `SerializableStruct` and their
  builders implement `ShapeBuilder`. DynamoDB now builds from a canonical Smithy model
  (`services/dynamodb/.../codegen-resources/dynamodb/smithy-model.json`, 23k lines).
- `f5ccd90216b` — `DefaultDynamoDbClient` calls the smithy-java protocol directly:
  `smithyProtocol.createRequest` → `smithyTransport.send` → `smithyProtocol.deserializeResponse`.

`output/smithy-port-issues.md` is the honest ledger for this stage: 11 issues with resolutions and
remaining gaps. Prototype shortcuts in force: `ALLOW_UNKNOWN_TRAITS` on the model assembler,
`-Dawssdk.codegen.skipValidation=true` to bypass `SharedModelsValidator` (DynamoDB Streams still
loads from C2J while DynamoDB loads from Smithy), and endpoint rules / paginators / waiters still read
from C2J-era sidecar files rather than the model's traits.

Issue 11 in that document — renamed members serializing with the Java name instead of the wire name —
**was subsequently fixed**: `SdkSchemaFactory` now emits `@jsonName`/`@xmlName` from
`LocationTrait.locationName()` when it diverges from the member name.

### 3.4 `smithy-java-spi-bridge-poc` — the most complete work in the repo

`8c79b2d7923` reframes the whole thing as a runtime SPI, plus `22f9c2f828c` (see §6).

**Design.** Three new types in `core/sdk-core` with **zero** smithy-java dependencies:

| type | role |
|---|---|
| `SdkPipeline` | execution interface: `execute(params, config)`, `supportsOperation(params)` |
| `SdkPipelineProvider` | `ServiceLoader`-discovered factory: `priority()`, `isAvailable()`, `createPipeline(config)` |
| `SdkPipelineLoader` | discovery: sorts providers by priority (lowest wins), returns the first available |

Plus `SdkClientOption.SDK_PIPELINE`, an explicit-pipeline escape hatch that bypasses `ServiceLoader`
discovery entirely — intended for testing and benchmarking.

`core/smithy-java-bridge` gains `SmithyJavaPipelineProvider` (priority 0, registered in
`META-INF/services`) and `SmithyJavaPipeline`, which **delegates to smithy-java's `Client.call()`**
rather than reimplementing pipeline stages, so it inherits the full interceptor ordering, the retry
loop wrapping the correct stages, identity resolution and signing coordination, auth scheme
resolution and endpoint resolution. Supporting components: `V2CredentialsBridge`, `AuthSchemeResolver`,
`ProtocolResolver`, `V2RetryBridge`, and `FullV2InterceptorBridge` (411 lines — the complete hook
mapping, replacing the 4-hook subset).

23 main + 18 test files, with heavy jqwik property-based testing.
`.kiro/specs/smithy-java-full-pipeline/tasks.meta.json` records property tests passing through task
11.2. The `tasks.md` checkboxes are stale — they show tasks 2–5 and 9–13 unchecked even though the
corresponding code and passing property tests exist. Trust `tasks.meta.json` and the code, not the
checkboxes.

The SPI framing is the real conceptual advance over the earlier branches: it decouples the execution
engine from the generated client, so adoption is opt-in by classpath rather than baked in at build
time by a codegen flag.

**Caveat on that claim.** `.kiro/specs/smithy-java-spi-pipeline-poc.md` says the SPI approach leaves
the generated client with no smithy-java dependency, only `sdk-core`. On this branch that is not true.
Because the SPI commit was layered on top of the Phase-2 hard-wired path rather than replacing it,
`DefaultDynamoDbClient` carries `smithyProtocol`, `smithyTransport` and `smithyEndpoint` fields
unconditionally. The clean separation the spec describes is the intent, not the state.

### 3.5 What the generated client actually does — important

Every non-streaming operation on `DefaultDynamoDbClient` (this branch) reads:

```java
if (sdkPipeline != null && sdkPipeline.supportsOperation(params)) {
    return sdkPipeline.execute(params, clientConfiguration);       // SPI: smithy-java Client.call()
}
// the fallback is NOT v2 -- it is the Phase-2 hard-wired smithy protocol path
HttpRequest httpRequest  = smithyProtocol.createRequest(op, request, ctx, smithyEndpoint);
HttpResponse httpResponse = smithyTransport.send(ctx, httpRequest);
return smithyProtocol.deserializeResponse(op, ctx, op.errorRegistry(), httpRequest, httpResponse);
```

**`clientHandler.execute` appears zero times in the entire 9,427-line generated client.**
`clientHandler` is only constructed and closed. The sync DynamoDB client on this branch has no v2
execution path left at all.

Consequences worth remembering:

- There is no way to A/B the bridge against v2 on this branch without a code change (see §7).
- The v2 machinery is nonetheless still *present* in the artifact — all 57 request marshallers are
  still generated, and each method still builds `operationMetadata`, `responseHandler`,
  `errorResponseHandler`, `protocolMetadata` and the metric collector. Only the call site was dropped,
  because `JsonProtocolSpec.executionHandler` early-returns the smithy block before emitting the v2 one.
- `DefaultDynamoDbAsyncClient` was **not** rewired — it still has 60 `clientHandler` references and
  remains pure v2. The SPI work is sync-only, matching the spec's stated limitation.

### 3.6 Build

Requires JDK 21+ and a locally published smithy-java. The branch pins
`<smithy.java.version>1.4.2-rebased</smithy.java.version>`, which **does not exist in any remote
repository**.

```bash
# 1. Publish smithy-java locally. reference/smithy-java is a local checkout at c823f9281 with
#    VERSION overwritten to 1.4.2-rebased. It is untracked -- see Limitations.
cd reference/smithy-java && ./gradlew publishToMavenLocal -x test -x javadoc

# 2. Build the SDK. skipValidation is required (shared-model validator, DynamoDB/DynamoDB Streams).
mvn clean install -pl :dynamodb -P quick --am -Dawssdk.codegen.skipValidation=true

# 3. E2E tests and the benchmark
mvn clean install -pl :smithy-java-pipeline-tests
mvn clean install -pl :smithy-bridge-benchmark
java -jar test/smithy-bridge-benchmark/target/smithy-bridge-benchmarks.jar
```

DynamoDB opts in via `"generateSmithyJavaSerde": true` in its `customization.config` and a compile
dependency on `smithy-java-bridge` in `services/dynamodb/pom.xml`.

### 3.7 What is verified

`test/smithy-java-pipeline-tests` — `SmithyJavaPipelineE2ETest`, **6/6 passing** (re-run 2026-09-04,
matching the recorded 2026-07-29 surefire report). A real `DynamoDbClient` against a mock DynamoDB
server, with no configuration selecting the smithy path — the bridge jar on the classpath is what
activates it:

- `getItem_roundTrip_deserializesResponseCorrectly`
- `putItem_roundTrip_deserializesResponseCorrectly`
- `errorHandling_serviceError_throwsException`
- `retryBehavior_transientError_retriesAndSucceeds` (mock fails attempt 1 with a 500)
- `interceptorExecution_v2InterceptorFires` (v2 `beforeExecution` / `afterExecution`)
- `pipelineDiscovery_smithyJavaPipelineProviderIsDiscovered` (priority 0, available)

This is the strongest evidence for Approach A: the SPI bridge genuinely works end to end on DynamoDB,
including retry, error mapping and v2 interceptor compatibility.

### 3.8 Limitations

- **JDK 21 baseline** — breaking for the SDK's Java 8 contract. Already applied to core modules.
- **Sync only.** No `SdkPipeline.executeAsync`; `AsyncClientClass` untouched.
- **Streaming and event-stream operations excluded** — always fall through.
- **awsJson1 wired in practice.** `ProtocolResolver` exists for restJson/restXml/rpcv2, but DynamoDB
  is the only service exercised.
- **Reflection-based operation resolution.** `SmithyJavaPipeline` resolves `<Op>Operation` by class
  name convention via `Class.forName`, cached in a `ConcurrentHashMap`.
- **Metrics not wired** on the smithy path.
- **Model-source shortcuts still in force** — `ALLOW_UNKNOWN_TRAITS`, `skipValidation`, C2J sidecars
  for endpoint rules / paginators / waiters, `customization.config` still required with no Smithy
  equivalent.
- **`smithy.rules#endpointBdd`** in the DynamoDB model is silently ignored.
- **`1.4.2-rebased` is not reproducible** without the local `reference/smithy-java` checkout, which is
  untracked. This is the single biggest reproducibility hazard in the whole effort.
- **No end-to-end v2-vs-bridge number exists.** See §7.

---

## 4. Approach B — port the techniques into v2 natively

Deliberately the opposite bet: no smithy-java dependency, no JDK 21 (`fork_smithy_java` stays on
`<jre.version>1.8</jre.version>`), no public API change. Reimplement the *ideas* inside v2's own
modules, each behind a fast-path check with automatic fallback.

### 4.1 `fork_smithy_java`

Three commits on top of 2.46.12-SNAPSHOT. Brief in `reference/fork_smithy_prompt.md`: fork the
optimized serde from smithy-java, prototype only, shortcuts acceptable.

- `dd70a519e24` + `741ead3f91f` — `FastV4HeaderSigner`, `Pool`, `V4SigningResources`,
  `V4SigningKeyCache`, `SignedSdkHttpFullRequest`, `ByteBufferContentProvider`. Ports smithy-java's
  pooled-scratch-state signer design into a v2-shaped fast path, dispatched from
  `DefaultAwsV4HttpSigner.sign()` on a narrow eligibility check, with byte-equivalence tests.
- `4aa652decab` — `FastJsonSerializer`/`FastJsonDeserializer` plus a hand-rolled runtime `Schema`
  under `aws-json-protocol/internal/fast/`, bypassing Jackson entirely. Enabled by default with
  `-Daws.sdk.fastJson=false` to opt out, falling back per shape for anything unhandled.

**Results** (`output/smithy_java_fork/03_results.md`): all 720 protocol tests pass, plus marshall and
unmarshall parity tests against Jackson. Marshall side 20–66% faster on medium/large payloads (up to
2.9× on binary blobs, where Jackson's character-based base64 emission is the bottleneck), but
**25–41% slower on tiny payloads** (<~100 B) from per-call adapter allocation. Unmarshall gains a
more modest 8–25%, because the baseline already uses streaming Jackson tokens rather than a `JsonNode`
tree — not a straw man.

That document's own top recommendation for closing the small-payload gap — codegen-emitted
`serializeMembers` to eliminate the reflective adapter — is exactly what Approach A's
`smithy-java-bridge-alexwoo` went on to build. The two lines converge there.

Build: `mvn clean install -pl :aws-json-protocol,:http-auth-aws -P quick --am`. No local smithy-java
needed. Benchmarks in `test/sdk-standard-benchmarks` (`serde-tests/json-rpc-1-0` cases) and
`test/sdk-benchmarks`.

### 4.2 `alexwoo/sigv4_smithy-java-opts`

The signer half only, productionized and rebased onto 2.48. Three commits, +1558/-17 across 7 files
all under `core/http-auth-aws`. Write-up: `output/sigv4_fast_path_pr_summary.md`, review notes in
`output/sigv4_fast_path_review.md`.

**This is the only piece of any of this work with production-grade evidence.** Anticanary,
`c7g.8xlarge`, 1000 threads, 7500 target TPS, 20 minutes:

- SystemCpuLoad p50/p90: **10.2% lower** (p=4.32e-58)
- cpu_usage_active p50/p90: **10.3% lower** (p=5.46e-58)
- OpsPerCpuSecond p90: **8.7% higher** (p=8.54e-45)

JMH, `AwsV4HttpSignerBenchmark`: M7i 2825 vs 4823 ns/op, M7g 3388 vs 6759 ns/op, fast vs legacy path.

Byte-for-byte identical `Authorization`, `x-amz-date`, `x-amz-content-sha256` and
`x-amz-security-token` on every accepted request, asserted by 11 new `FastV4HeaderSignerTest` cases,
with all pre-existing SigV4 tests still passing.

**Not on `master`.** It did carry forward into `feature/poc/racecar`.

Build: `mvn clean install -pl :http-auth-aws -P quick --am`.

### 4.3 Limitations

- The fast JSON path is prototype quality by design: adapter allocations on the hot path, a fresh 8 KB
  buffer per marshall, `Double.toString` rather than a Schubfach formatter, and a tiny-payload
  regression. `03_results.md` §"What's left on the table" lists six specific items.
- The fast signer covers header signing only, with a narrow eligibility check; everything else falls
  back to the composed `Checksummer` → `V4RequestSigner` → `V4PayloadSigner` stack.
- Approach B's ceiling is bounded: it can address serde and signing, but not the per-call framework
  orchestration that the crosssdk-254 report attributes 23.7% of small-get CPU to.

---

## 5. Approach C — `feature/poc/racecar`

Not a bridge, but the direct successor to all of it and the only line still active. It treats
smithy-java as a *performance target* rather than an implementation.

- `test/benchmark-smithy-java/` — head-to-head native smithy-java (`sj_*`) versus V2 (`v2_*`) clients
  for DynamoDB, CloudWatch and S3, with committed JFR profiles. This is the harness that produced the
  crosssdk-254 numbers in §1.
- Carried the fast SigV4 signer over from Approach B, but **not** the fast JSON fork. Instead it
  optimized the existing Jackson path incrementally (phases E5–E8: four-characters-per-iteration
  string writing, a one-comparison whitespace fast path, owned async response buffers).
- Method is rigorous: every `perf(...)` commit is paired with a `docs(benchmarks): Record ...` commit,
  and negative results are kept in the record and reverted rather than deleted (E5, E8, E9).
- `output/racecar_do_now.md` ranks six changes by risk for near-term release, with the two parts of D1
  (Apache5 header filtering, user-agent construction) as the lowest-risk candidates.

For measurement work, this branch's harness is the one to trust: out-of-process server, retry parity,
reconciled request counts, bare metal.

---

## 6. What was recovered on 2026-09-04

Commit `22f9c2f828c` on `smithy-java-spi-bridge-poc`.

`8c79b2d7923` added two modules to the root pom's module list whose sources were **never committed**,
so the reactor could not resolve either and a full-reactor build failed immediately. Both had
surviving `target/` directories. Sources were reconstructed by decompiling the compiled classes (CFR
0.152 from Maven Central, pointed only at local class files — the classes carry debug info, so the
decompilation was clean), with the poms rebuilt from `target/maven-archiver/pom.properties`, the shade
plugin's dependency list in `build-benchmark.log`, and `test/sdk-benchmarks` conventions.

Three inferred build settings are load-bearing:

- The parent pom sets `-proc:none`, so `smithy-bridge-benchmark` needs a `combine.self="override"`
  compiler configuration to re-enable JMH's `BenchmarkProcessor`. Without it the uber-jar ships no
  `META-INF/BenchmarkList` and JMH refuses to start. The regenerated `BenchmarkList` is byte-identical
  to the original (1244 B, same four entries, same encoded `@Warmup(5)/@Measurement(5)/@Fork(2)`),
  which corroborates the recovered annotations.
- spotbugs is skipped in the benchmark module (`SE_NO_SERIALVERSIONID` on the canned-response servlet),
  matching `test/sdk-benchmarks`.
- `maven-dependency-plugin` is skipped in both modules. Version 3.1.1 bundles an ASM that cannot read
  the Java 21 bytecode these modules emit ("Unsupported class file major version 65"). This affects
  any Java-21 module on this branch, not just these two.

Original `target/` directories were backed up to `/tmp/smithy-recovery-backup` before the first
`mvn clean`, as they held the only surviving originals.

---

## 7. `test/smithy-bridge-benchmark` — what it measures, and what it does not

The benchmark builds two `DynamoDbClient`s with identical configuration (same endpoint, credentials,
`Apache5HttpClient` transport) against the same in-process canned-response Jetty server, in the same
JVM. The only difference is pipeline selection:

- `smithyJavaPipeline*` leaves `SDK_PIPELINE` unset, so `SdkPipelineLoader` discovers
  `SmithyJavaPipelineProvider` from the bridge jar → smithy-java `Client.call()`.
- `standardPipeline*` has a `NoOpPipeline` reflectively injected into `SDK_PIPELINE` whose
  `supportsOperation()` always returns `false`.

Keeping the classpath identical for both arms is a genuinely good design choice. But per §3.5, the
fallback is not v2 — it is the hard-wired bare smithy protocol path. **`standardPipeline*` is
misnamed and does not measure v2.**

What it actually measures is smithy-java's full pipeline versus its own bare serde. Full-fidelity run,
2026-09-04, JDK 25.0.2, single thread, 5 warmup + 5 measurement iterations × 10 s, 2 forks:

| benchmark | ops/s | what it is |
|---|---:|---|
| `smithyJavaPipelineGetItem` | 10,959 ± 1,186 | smithy-java `Client.call()` — interceptors, retry, auth, endpoints |
| `smithyJavaPipelinePutItem` | 11,051 ± 819 | same |
| `standardPipelineGetItem` | 14,508 ± 571 | bare `createRequest` → `send` → `deserializeResponse` |
| `standardPipelinePutItem` | 13,312 ± 2,454 | same |

Read as pipeline overhead: smithy-java's orchestration layer costs roughly **20–25% throughput** over
raw serde on small DynamoDB calls. That is useful — it bounds how much of smithy-java's 2–3×
advantage survives running a full client rather than just a codec. It is not a v2 comparison.

**Two further defects in the harness:**

1. Only `getitem-response.json` is ever loaded. `putitem-response.json` exists but is never read, so
   the PutItem arms are served a `{"Item":...}` body instead of `{"Attributes":...}`. Symmetric across
   arms, so the comparison is not invalid, but it under-counts response deserialization for both.
2. The Jetty server shares the JVM with the client. `-prof stack` comes back ~43% `sun.nio.ch.KQueue.poll`
   and cannot attribute client CPU at all. `pipeline_benchmark2` deliberately uses an out-of-process
   server for exactly this reason.

### Adding a real v2 arm

Two routes. The v2 machinery is all still present in the artifact (§3.5), so neither is large.

**Route 1 — codegen three-way dispatch,** ~65 lines across 4 files. Add
`SdkClientOption<Boolean> USE_LEGACY_SERDE` to `sdk-core` (~10 lines, mirroring how `SDK_PIPELINE` was
added in the same commit); extract the existing v2 body in `JsonProtocolSpec.executionHandler` into a
private method and compose `if (useLegacySerde) { <v2> }` followed by `<smithy>` (the v2 block is a
single `return clientHandler.execute(...)` statement, so nesting is trivially valid); add a
`private final boolean useLegacySerde` field and constructor init to `SyncClientClass`, guarded by the
same `isGenerateSmithyJavaSerde()` check that already guards `smithyProtocol`; add a third client and
two `@Benchmark` methods. Then rebuild `dynamodb`, `smithy-java-bridge` and the benchmark.

The risk is measurement validity, not effort. All three paths land in one compiled method per
operation, and `getItem` is already enormous because of its ~100-case `exceptionMetadataMapper` switch.
Adding the v2 block pushes it further past HotSpot's inlining thresholds, and the branch is decided by
a `final` instance field that differs between two live client instances, so the profile is shared and
neither path gets the constant-fold and dead-code elimination it would get in a real single-path
build. Mitigation: make the toggle a `static final boolean` from a system property so each fork folds
it, and drive the arms as separate JMH invocations — at the cost of carrying both a client option (for
the E2E tests) and a system property (for the benchmark).

**Route 2 — two DynamoDB builds, no codegen change,** ~1–2 hours. Build DynamoDB a second time with
`generateSmithyJavaSerde` off into a second benchmark module and compare across runs. Each artifact is
exactly what it claims, with no cross-path JIT contamination. The cost is a cross-JVM comparison,
which given the in-process server and ±5–20% error bars is nowhere near the dominant error term.

**Recommended:** Route 2 for the number. But note that `feature/poc/racecar`'s
`test/benchmark-smithy-java` already answers a cleaner version of the question (out-of-process server,
JFR profiles, bare metal, reconciled request counts). If the question is "how much of smithy-java's
2–3× does the bridge keep," porting the bridge into *that* harness produces a defensible answer where
upgrading this one produces a laptop number.

---

## 8. Comparison

| | A: bridge / re-host | B: native port | C: incremental |
|---|---|---|---|
| smithy-java dependency | yes, at runtime | no | no |
| JDK baseline | 21 (breaking) | 8 | 8 |
| Public API impact | shim, "preserve to an extent" | none | none |
| Ceiling | full 2–3× CPU / 5–8× alloc | serde + signing only | single-digit % per change |
| Maintenance payoff | large — deletes ~47 runtime modules + C2J | none; adds a parallel codec | none |
| Risk | very high, whole-runtime cutover | medium, per-subsystem with fallback | low, individually reviewable |
| Shippable today | no | signer: yes | yes, ranked |
| Evidence | serde microbenchmarks + 6/6 E2E; **no e2e v2 comparison** | JMH + 720 protocol tests | anticanary + bare metal + JFR |

---

## 9. Open items, in rough priority order

1. **`alexwoo/sigv4_smithy-java-opts` has never been raised as a PR.** 10% CPU with anticanary
   backing, byte-equivalence tested. This is the clearest unrealized value in the repo.
2. **Get a real v2-vs-bridge end-to-end number** (§7). Until then Approach A's central claim is
   unmeasured at the client level.
3. **Fix the two harness defects** — load `putitem-response.json`, move the server out of process.
4. **Resolve `1.4.2-rebased`.** Publish to an internal repository or align on a real smithy-java
   release. Currently unreproducible without an untracked local checkout.
5. **Reconcile the two smithy paths on `smithy-java-spi-bridge-poc`.** The hard-wired Phase-2 path and
   the SPI path coexist; the spec intends the SPI path to win and the generated client to carry no
   smithy-java imports.
6. **Update the stale `tasks.md` checkboxes** in `.kiro/specs/smithy-java-full-pipeline/` to match
   `tasks.meta.json` and the committed code.
7. Async support, streaming support, and multi-protocol coverage beyond DynamoDB/awsJson1.

---

## 10. Reference index

| path | what |
|---|---|
| `RFC-smithy-java-runtime.md` | Approach A design document and locked decisions |
| `codegen-c2j-to-smithy/IR-REMOVAL-PLAN.md` | blast radius of removing `IntermediateModel` |
| `test/sdk-standard-benchmarks/BRIDGE_BENCHMARK_DESIGN.md` | bridge serde benchmark design |
| `output/profile_report.md` | DynamoDB hot-spot profile that started it |
| `output/smithy-port-issues.md` | 11 issues from the DynamoDB Smithy-model port |
| `output/smithy_java_fork/03_results.md` | fast JSON codec correctness + performance |
| `output/sigv4_fast_path_pr_summary.md` | signer PR write-up + anticanary data |
| `output/racecar_do_now.md` | Racecar changes ranked by release risk |
| `pipeline_benchmark2/analysis/crosssdk-254/report.md` | V1 vs V2 vs smithy-java on bare metal |
| `.kiro/specs/smithy-java-dynamodb-bridge/` | Phase 1 spec |
| `.kiro/specs/smithy-java-client-pipeline/` | Phase 2 spec |
| `.kiro/specs/smithy-java-full-pipeline/` | full pipeline spec (checkboxes stale) |
| `.kiro/specs/smithy-java-spi-pipeline-poc.md` | SPI design + limitations table |

### Tracking status of the referenced material

Most of the documentation above lives in the working tree rather than in git. Verified 2026-09-04:

| path | status |
|---|---|
| `output/`, `reference/`, `pipeline_benchmark/` | **untracked on every branch** |
| `pipeline_benchmark2/` | tracked on `feature/poc/racecar`; untracked here |
| `.kiro/steering/`, `.kiro/settings/` | tracked |
| `.kiro/reference/` | **untracked, deliberately** — contains symlinks to directories this repo does not own |
| `.kiro/specs/` | committed 2026-09-04, per branch (below) |

The specs are now carried by the branch whose code they describe, so switching branches adds and
removes them from the working tree:

| spec | branch |
|---|---|
| `smithy-java-dynamodb-bridge/` (Phase 1) | `smithy-java-bridge-alexwoo`, also on `smithy-java-spi-bridge-poc` |
| `smithy-java-client-pipeline/` (Phase 2) | `smithy-java-bridge-alexwoo`, also on `smithy-java-spi-bridge-poc` |
| `smithy-java-full-pipeline/` | `smithy-java-spi-bridge-poc` |
| `smithy-java-spi-pipeline-poc.md` | `smithy-java-spi-bridge-poc` |
| `disable-request-override/` | `alexwoo/no-request-override-poc` |
| `sso-token-refresh-bug/` | still untracked — the fix merged to `master` as #7097, so it has no obvious home |

Still untracked and existing only on this machine: the port-issue ledger, the fast-JSON results, the
signer PR write-up, the profile report, `output/` generally, and the `reference/smithy-java` checkout
that `1.4.2-rebased` is built from. Back those up.
