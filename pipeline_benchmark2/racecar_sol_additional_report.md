# Project Racecar: Independent Cross-SDK Analysis and Additional Opportunities

**Date:** 2026-09-02  
**Scope:** AWS SDK for Java v2, V1, and smithy-java DynamoDB benchmark data; SDK v2 implementation; smithy-java reference implementation  
**Primary data:** `pipeline_benchmark2/raw/crosssdk-254/`  
**Existing analysis reviewed:** `pipeline_benchmark2/analysis/crosssdk-254/report.md` and `pipeline_benchmark2/project_racecar_summary.md`

## Executive summary

The cross-SDK data supports Project Racecar's central diagnosis: unmodified SDK v2 2.54.0 paid substantial fixed per-call framework and signing costs on small operations, metadata-driven serde costs on large writes and reads, and additional body-copy and coordination costs on the asynchronous path. Racecar has already addressed most of the largest gaps: the fast signer, materialized request bodies, adaptive marshalling buffers, straight-line pipelines, generated serde, dense execution attributes, completed-future fast paths, retry-header work, immutable collection adoption, and a faster JSON string loop.

My independent review found one high-confidence, high-value residual that Racecar had not directly addressed,
and it has now been validated as phase E10:

1. **E10 collapses asynchronous non-streaming response aggregation to one SDK-owned array.** In the
   predecessor async batch-get profile, three named SDK copy sites totaled approximately 165 KB/op on
   the current Racecar stack. E10 removes about 128 KB/op, reduces total client allocation by 21.3%,
   and improves async batch-get by 2.4% application CPU / 2.3% latency with 7/7 paired wins. The byte
   reader still copies the owned response stream into another ~36 KB array, making direct parser handoff
   the clearest next prototype.

I also found several smaller or broader opportunities:

2. **Build CRT request headers directly into a pre-sized `HttpHeader[]` and avoid constructing a full `URI` merely to obtain the host.**
3. **Make remaining auth-resolution diagnostics and interceptor-property rebuilds lazy.**
4. **Replace the pre-interceptor full-URI snapshot with a compact endpoint-comparison snapshot.**
5. **Reuse immutable interceptor chains and replace hook-dispatch lambdas with indexed loops.**
6. **Precompute the client-constant portion of per-call execution attributes.**
7. **Prototype compact flat-array HTTP headers with lazy public map views.** This was already suggested by Racecar; smithy-java confirms that it is a coherent architectural direction.
8. **Measure lazy response metadata before implementation.** It is a new residual, but E4 already removed the expensive collection-copy consequence of the response rebuilds.
9. **Use pooled flat query pairs and an already-canonical query bypass in the fast signer when a query-heavy workload justifies it.** This does not help the DynamoDB corpus.

The following should not be prioritized without new evidence: broad endpoint-result caching, replacing dense execution attributes with sparse chunks, treating `FunctionalUtils.invokeSafely` as a demonstrated performance issue, or expanding generated serde APIs solely to fuse capacity checks.

## 1. Method and evidence boundaries

### 1.1 What was inspected

This analysis used:

- `pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`
- `pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`
- `pipeline_benchmark2/raw/crosssdk-254/profiles-small.txt`
- `pipeline_benchmark2/raw/crosssdk-254/profiles-batch.txt`
- All retained cross-SDK metrics files and manifests
- The generated summary from `analysis/scripts/crosssdk_tables.py`
- Relevant SDK v2 request, pipeline, auth, endpoint, HTTP, async-response, protocol, codegen, and metadata sources
- Analogous code under `.kiro/reference/smithy-java`
- Racecar's phase-by-phase measurements, including negative experiments and later corrections to initial diagnoses

The existing `crosssdk_tables.py` output reproduces the headline timing, throughput, allocation, category, metric, and frame tables from the committed aggregates.

### 1.2 Important limitation: the retained raw tree is aggregated

The manifests reference per-invocation logs and JFR recordings, but those primary artifacts are not retained under `pipeline_benchmark2/raw/crosssdk-254/`. Case directories contain metrics files, while `profiles-small.txt` and `profiles-batch.txt` contain already-aggregated profiler output.

Consequences:

- Timing CSV means and flags can be independently recomputed.
- Allocation totals and category arithmetic can be checked against the aggregate profile reports.
- CPU/allocation categories and top frames cannot be regenerated from primary JFR recordings.
- Causal source conclusions require code inspection; a self frame alone does not prove its full call context.
- Allocation profiles include warmup and one-time setup. Per-case divisors are correct, but warmup length differs substantially by client.

The copy under `pipeline_benchmark2/analysis/crosssdk-254/data/` is a preservation copy of the same artifacts, not an independent measurement.

### 1.3 Baseline-versus-current provenance

The cross-SDK collection measures published, unmodified SDK v2 2.54.0 and smithy-java 1.5.1. The current Racecar branch and the checked-in smithy-java reference are newer. Therefore:

- Baseline profile sizes establish why a mechanism mattered in 2.54.0.
- Racecar phase comparisons establish which mechanisms were removed and their measured effects.
- Current source proves a residual path still exists.
- Old baseline bytes/op must not be presented as the exact current cost after Racecar.
- A current Racecar profile or paired experiment is required before claiming a residual end-to-end result.

## 2. Independent audit of the headline data

### 2.1 Recomputed timing means

Arithmetic means of the three retained timing repetitions:

| Client and scenario | App CPU µs/op | Mean latency µs | Ops/wall-second | Steady repetitions |
|---|---:|---:|---:|---:|
| V1 small-get | 114.5 | 155.4 | 6,429 | 3/3 |
| V2 sync small-get | 151.6 | 193.2 | 5,171 | 3/3 |
| V2 async small-get | 204.3 | 222.2 | 4,356 | **1/3** |
| smithy-java small-get | 46.0 | 79.0 | 12,655 | 3/3 |
| V1 small-put | 96.6 | 137.8 | 7,252 | 3/3 |
| V2 sync small-put | 143.4 | 184.5 | 5,418 | 3/3 |
| V2 async small-put | 197.1 | 215.7 | 4,472 | **1/3** |
| smithy-java small-put | 49.8 | 82.9 | 12,051 | 3/3 |
| V1 batch-get | 1,496.6 | 1,546.8 | 646 | 3/3 |
| V2 sync batch-get | 664.7 | 713.8 | 1,399 | 3/3 |
| V2 async batch-get | 740.6 | 756.5 | 1,307 | 3/3 |
| smithy-java batch-get | 343.3 | 381.7 | 2,617 | 3/3 |
| V1 batch-put | 731.2 | 785.4 | 1,272 | 3/3 |
| V2 sync batch-put | 798.5 | 847.3 | 1,179 | 3/3 |
| V2 async batch-put | 784.1 | 823.3 | 1,201 | 3/3 |
| smithy-java batch-put | 304.3 | 354.1 | 2,820 | 3/3 |

The central ranking is valid. Smithy-java is substantially cheaper than unmodified SDK v2, while V1 is competitive on small calls and writes but pathological on batch reads.

### 2.2 Corrections and qualifications to the existing report

#### Steady-state count

The retained timing data contains **44 steady repetitions out of 48**, not “23 of 24 timing cases.” Both V2 async small scenarios are only 1/3 steady, producing four false flags total. The report's later caveat describes the per-case issue correctly; its aggregate count does not.

#### Async is not slower on every scenario

V2 async is slightly better than V2 sync on batch-put in all retained clean timing measures:

- App CPU: 784.1 versus 798.5 µs/op
- Mean latency: 823.3 versus 847.3 µs
- Throughput: 1,201 versus 1,179 ops/s

The difference is small, but it disproves the blanket statement that async is slower on every scenario.

#### Server CPU parity

Server CPU per operation is not within 2% across V1, V2 sync, and V2 async on every scenario. Recomputed ranges are approximately:

- Small-get: 2.4%
- Small-put: 7.0%
- Batch-get: 7.1%
- Batch-put: 7.8%

This does not invalidate the client conclusions, but the server-parity claim should be narrowed.

#### Smithy small-get transport arithmetic

Under the stated formula, smithy small-get transport is:

```text
attempt - sign - deserialize
81.41 - 11.39 - 8.88 = 61.14 µs
```

It is not approximately 59 µs unless endpoint time is also subtracted, which was not consistently done for the other scenarios.

#### Payload size terminology

Smithy metrics report approximately 37.8 KB for the batch-get response and 38.4 KB for the batch-put request. The “25×2 KB” label is a workload description, not the actual measured wire-body size. Allocation-copy estimates should use the measured body size when available.

### 2.3 Allocation totals

The retained aggregate profiles reproduce these client-code allocation totals:

| Scenario | V1 | V2 sync | V2 async | smithy-java |
|---|---:|---:|---:|---:|
| Small-get | 40,554 | 60,335 | 68,246 | 10,551 |
| Small-put | 36,535 | 54,315 | 61,007 | 6,839 |
| Batch-get | 266,199 | 535,507 | 747,959 | 218,304 |
| Batch-put | 191,155 | 209,089 | 371,304 | 123,184 |

These are useful directional values, but they combine warmup/setup and measured-loop allocation. Smithy warmed substantially longer than V2 in several profile runs. Dividing by actual per-case operations prevents a nominal-divisor error, but does not turn a whole-JVM warmup profile into a pure steady-state allocation measurement.

## 3. What Racecar already solved

Before adding opportunities, it is important not to rediscover completed work.

| Baseline gap | Racecar response | Status |
|---|---|---|
| SigV4 object graph and body-hash buffer | Fast header signer, pooled scratch/crypto, signing-key cache, direct body-buffer hashing | Implemented and measured |
| Deep header/list copying | Shallow map-only copy-on-write for non-list-mutating operations | Implemented |
| Request body copied through wrappers/transports | Materialized-body capability propagated through wrappers; Apache5 and async consumers use it | Implemented |
| JSON output buffer growth | Per-operation adaptive size hints | Implemented |
| User-agent and Apache5 ignore-header allocation | Precomputed/sized user agent and indexed ignore-header loop | Implemented |
| Per-request pipeline composition graph | Straight-line sync and async pipelines | Implemented |
| Metadata-driven marshalling | Fast generator and generated straight-line marshalling | Implemented |
| Metadata-driven response parsing | Generated reader and byte-level JSON parser | Implemented |
| `ExecutionAttributes` map lookup | Dense ID-indexed array | Implemented, with documented behavior caveats |
| No-op auth option rebuild | Property-absence fast path and shared empty identity request | Implemented |
| Completed-future coordination | Inline completed identity/interceptor/request stages | Implemented |
| First-attempt retry header rebuild | Pre-stamp and exact-match reuse | Implemented |
| Response collection recopy during `toBuilder` | Generated copy constructors adopt immutable model collections | Implemented |
| Union type bitmask | Measured as no benefit and reverted | Do not repeat |
| JSON ASCII branch density | Four-character grouped fast path | Implemented and measured |
| Async non-streaming response aggregation | One SDK-owned, length-hinted buffer with no per-chunk arrays or final clone | **Implemented in E10; −128 KB/op aggregation allocation, −2.4% async batch-get CPU** |

Racecar's cumulative measurements show that the dominant write-side gap has largely closed, while small-call and response-side work remain the better areas for additional investigation.

## 4. Highest-priority opportunity and E10 result

### 4.1 Baseline evidence

In unmodified V2 async batch-get, these named SDK allocation sites account for:

| Site | Baseline bytes/op |
|---|---:|
| `AsyncResponseHandler$BaosSubscriber.onNext` | 93,503 |
| `BinaryUtils.copyBytesFrom` | 36,760 |
| `AsyncResponseHandler.lambda$prepare$0` | 36,685 |
| **Total named SDK aggregation copies** | **166,948** |

A separate CRT response callback site accounts for another 38,018 B/op. That transport-to-Java transfer may not be avoidable at the SDK layer, so it should not be included in the SDK-owned-copy target without further CRT investigation.

The response body itself is approximately 37.8 KB. The three SDK sites therefore represent multiple body equivalents, growth overhead, or both.

### 4.2 Pre-E10 code path

Before E10, `core/sdk-core/.../async/AsyncResponseHandler`:

1. Receives a `ByteBuffer` from the HTTP publisher.
2. Calls `BinaryUtils.copyBytesFrom`, allocating one temporary byte array per chunk.
3. Writes that temporary array into a growing `ByteArrayOutputStream`.
4. Calls `baos.toByteArray()` after completion, cloning the full accumulated body.
5. Wraps the clone in `ByteArrayInputStream` and `AbortableInputStream`.
6. Passes the stream to the synchronous protocol response handler.

Racecar's byte-level JSON path then checks `Content-Length`, allocates another exact `byte[]`, and reads the already-buffered async stream into that array before invoking `FastJsonStructuredReader.parseDocument`.

This means the current async+byte-reader path can pay an additional full-body copy after the baseline async aggregation copies.

### 4.3 Proposed design

Implement this in two independently measurable steps.

#### Step A: one SDK-owned aggregation array

When `Content-Length` is present, valid, and below a bounded in-memory limit:

- Allocate one exact SDK-owned `byte[]`.
- Copy each incoming `ByteBuffer` directly into that array during `onNext`.
- Do not retain transport buffers after `onNext` returns.
- Track actual bytes written.
- On short, long, or invalid bodies, fail or use the existing safe fallback according to current semantics.

For unknown length:

- Reuse the existing direct-access output-stream pattern already used by `ByteArrayAsyncResponseTransformer`.
- Write array-backed buffers directly without `BinaryUtils.copyBytesFrom`.
- For direct buffers, copy straight into the output stream's owned backing array.
- Expose the backing array and count without `toByteArray()`.

The implementation should carry an internal `(byte[] array, int offset, int length)` body object rather than a `ByteArrayOutputStream`.

#### Step B: direct parser handoff

Add an internal response-body capability that allows `JsonProtocolUnmarshaller` to obtain the SDK-owned array and invoke:

```java
FastJsonStructuredReader.parseDocument(array, offset, length, ...)
```

without allocating and rereading another array.

The generic `InputStream` view must remain available because:

- Other protocols still consume streams.
- Mixed-location and explicit-payload shapes retain fallback paths.
- CRC validation may need to read the body before parsing.
- Error handlers and custom response handlers expect the existing interface.

A repeatable stream over the owned array can preserve those paths.

### 4.4 Why not retain transport buffers

Smithy-java's JDK HTTP client includes a `ZeroCopyBodySubscriber` that retains response chunks because the JDK `BodySubscriber` contract explicitly transfers ownership of those buffers. SDK v2's HTTP publisher SPI does not provide the same guarantee for every transport. CRT or Netty may reuse pooled/direct buffers after `onNext` returns.

The safe SDK v2 design is therefore **one copy into SDK-owned storage**, not literal zero-copy retention of transport buffers.

### 4.5 Correctness and lifecycle requirements

Required coverage:

- Exact, shorter, longer, absent, invalid, negative, and oversized `Content-Length`
- Array-backed, sliced, read-only, and direct `ByteBuffer`s
- Multiple chunks and empty chunks
- Empty response body
- Cancellation during aggregation
- Error after partial aggregation
- Retry reuse and per-attempt cleanup
- CRC validation before protocol parsing
- Error response handling
- JSON byte-reader direct path and Jackson/other-protocol fallback
- No use of transport buffers after `onNext` returns
- Resource release on success, cancellation, and failure

Because this changes a Reactive Streams subscriber, TCK verification and focused backpressure/cancellation/error tests are required.

### 4.6 Expected value

The 166,948 B/op baseline total is an upper sizing clue, not a promised current saving. A successful implementation should eliminate several response-body equivalents from async non-streaming calls and should reduce the persistent async-versus-sync batch-get gap after E3/E4.

This was the highest-priority prototype because it had:

- Strong named profile evidence
- A still-present source mechanism
- A natural connection to Racecar's byte reader
- Broad applicability to non-streaming async responses
- A measurable sync-control arm

### 4.7 E10 implementation and measured result

Phase E10 (`40187bba428`) implemented Step A as a `sdk-core`-only change. It uses a bounded
`Content-Length` sizing hint, copies each transport buffer directly into one lazily allocated
SDK-owned array, and passes a count-bounded stream over that array to the unchanged protocol handler.
It preserves inaccurate-length, retry, CRC/gzip, absent/empty, cancellation, and generic handler
behavior, with focused unit tests and a new whitebox Reactive Streams TCK.

Equal 35,000-operation allocation profiles on the dedicated host confirmed the mechanism:

| allocation | predecessor | E10 | delta |
|---|---:|---:|---:|
| client code | 630,350 B/op | 495,953 B/op | **−21.3%** |
| `byte[]` | 294,447 B/op | 164,304 B/op | **−44.2%** |
| async aggregation | 165,090 B/op | 36,790 B/op | **−128,301 B/op** |

`BaosSubscriber.onNext`, `BinaryUtils.copyBytesFrom`, and the final `prepare()` clone all fell to zero.
The only aggregation allocation is the one 36.8 KB owned array. The byte reader still allocates another
36.3 KB body array, and CRT's response callback remains approximately 36.2 KB.

Dedicated-host timing (`paired/host-20260903-0404`, seven paired reps) showed async batch-get
application CPU **−2.4%** with ±0.8% paired spread and 7/7 wins; latency was **−2.3%** with ±0.7%
spread and 7/7 wins. Sync controls were noise. Async small-get was non-steady for CPU and flat in
latency, consistent with its 470-byte response.

**Status:** Step A is implemented and kept. Step B—capability-preserving direct handoff of the owned
array to `FastJsonStructuredReader`—is now the highest-priority follow-up and has a directly measured
~36 KB/op allocation target.

## 5. Near-term contained opportunities

### 5.1 Direct CRT header-array construction

#### Current path

`CrtRequestAdapter` currently:

1. Creates an `ArrayList<HttpHeader>`.
2. Adds synthetic Host, Connection, and Content-Length headers.
3. Iterates each SDK header through nested streams and lambdas.
4. Allocates one `HttpHeader` per value.
5. Calls `toArray(new HttpHeader[0])`, allocating/copying into another array.
6. Calls `sdkRequest.getUri()` even though the async adapter only uses `uri.getHost()`.

The unmodified async small-call profiles show roughly:

- 2.17–2.27 KB/op in `CrtRequestAdapter.createAsyncHttpHeaderList`
- 1.27–1.29 KB/op in `HttpHeader.<init>`
- Approximately 1.05 KB/op in CRT request JNI marshalling

The `HttpHeader` and JNI allocations remain necessary under the current CRT API, but the list, stream, lambda, and `toArray` work are avoidable.

#### Proposed change

- Count header values, not only distinct names.
- Allocate `HttpHeader[]` directly with room for synthetic headers.
- Fill it with imperative indexed loops.
- If exact counting requires an undesirable preliminary materialization, use a small growable array rather than `ArrayList` plus `toArray`.
- Use `sdkRequest.host()` for the Host header because the current code also discards the URI port.
- Keep encoded path and query handling unchanged.

A future entry-level header iteration API could avoid wrapping each `List<String>` during transport adaptation, but the direct-array change does not require a new public interface.

#### Risk and tests

Preserve:

- Synthetic header ordering
- Multi-value and duplicate ordering
- Header name casing
- Empty values
- HTTP/1.1 versus HTTP/2 Connection behavior
- Content-Length versus Transfer-Encoding exclusivity
- IPv4, IPv6, explicit ports, and standard ports
- Sync and async CRT paths

**Recommendation:** Implement as a small dedicated change and benchmark current CRT small-get/small-put allocation. Keep claims limited to adapter allocation; this does not remove per-value `HttpHeader` objects.

### 5.2 Lazy auth diagnostics and endpoint-property reapplication

Racecar already avoids rebuilding an auth option when the pre-existing option contributes no missing properties. Two smaller residuals remain in `AuthSchemeResolver`.

#### Lazy discarded reasons

`selectAuthScheme` eagerly allocates an `ArrayList<Supplier<String>>` even when the first auth option succeeds. Make the list lazy and allocate it only on the first discarded option.

Preserve:

- Complete ordered diagnostics when all options fail
- Existing debug message ordering
- Lazy supplier evaluation
- First-success behavior

#### Lazy interceptor-property builder

`doApplyInterceptorModifiedProperties` currently calls `currentScheme.authSchemeOption().toBuilder()` and allocates a one-element `boolean[]` before establishing that an interceptor changed any signer property.

Use a consumer that:

- Detects the first actual modification.
- Creates the builder only at that point.
- Applies subsequent modifications directly.
- Leaves `SELECTED_AUTH_SCHEME` untouched when no property changed.

**Recommendation:** Treat these as small allocation changes. Add exact diagnostics and property-precedence tests. Do not attribute the full D2 2–4% result to them; D2 measured dense attributes and a larger auth optimization together.

### 5.3 Compact endpoint-modification snapshot

Before `modifyHttpRequest` interceptors run, `BaseClientHandler` stores:

```java
httpRequest.getUri()
```

`SdkHttpRequest.getUri()` encodes the query string, builds a URI string, and parses it into a `URI`. Later, `EndpointResolutionStage.interceptorModifiedEndpoint` reads only:

- Host
- Protocol
- Port

Path and query changes intentionally do not count as endpoint replacement.

The old small-get profile samples `SdkHttpRequest.getUri`, and endpoint work accounts for approximately 3–4% of CPU overall. The profile does not isolate this snapshot's exact share, so measure it directly.

Proposed approach:

- Add a new internal snapshot carrying protocol, host, and normalized port, or retain the immutable pre-interceptor `SdkHttpRequest` reference.
- Compare only the fields used today.
- Keep the existing URI execution attribute as compatibility fallback if necessary.

Required edge cases:

- Null and explicit ports
- Explicit standard port versus normalized `-1`
- Protocol changes
- Host casing
- IPv6
- Path-only and query-only changes
- Customer interceptor replacement of the entire request

**Recommendation:** Prototype with a microbenchmark around snapshot and comparison. This is potentially a low-risk fixed-per-call win, but the old aggregate endpoint category is not sufficient evidence to claim a percentage.

## 6. Framework opportunities that require measurement

### 6.1 Reuse immutable interceptor chains

Every request constructs a new `ExecutionInterceptorChain`, which copies the effective interceptor list into a new `ArrayList`. The chain itself has no mutable per-request fields; request state is passed in `InterceptorContext` and `ExecutionAttributes`.

Potential design:

- Build or cache the chain per effective immutable `SdkClientConfiguration`.
- Reuse it when request plugins do not produce a different configuration.
- For request-specific configurations, create or cache a chain associated with that effective configuration rather than globally.

Caveats:

- Preserve interceptor ordering and constructor snapshot semantics.
- Interceptors may themselves be stateful, but their instances are already reused; chain reuse does not introduce that property.
- Debug logging currently happens on every chain construction and would change frequency.
- Request plugins and override configuration can change the effective list.

### 6.2 Replace interceptor hook lambdas with indexed loops

Several `ExecutionInterceptorChain` methods use `List.forEach` with capturing lambdas, and reverse-order hooks pass another capturing `Consumer` into `reverseForEach`. Mutating hooks already use direct loops.

Convert void hooks to explicit indexed loops. This:

- Avoids wrapper lambdas/consumers.
- Gives the JIT simpler control flow.
- Preserves exact order.
- Requires no public interface change.

The old framework category and `itable stub` samples motivate measuring interceptor dispatch, but they do not prove a specific gain. Use a component benchmark with realistic SDK interceptor counts and both empty/custom chains.

### 6.3 Precompute client-constant execution attributes

`AwsExecutionContextBuilder` performs dozens of `SdkClientConfiguration.option` lookups and individual `putAttribute` calls on every API call. Racecar's dense `ExecutionAttributes` makes the resulting lookups cheaper, but does not remove setup work.

A qualified fast path could precompute a client template containing constants such as:

- Service name/signing name
- Region/signing region
- Endpoint prefix and client endpoint provider
- FIPS/dual-stack flags
- Protocol-independent client settings
- Identity/auth scheme registries
- Retry and checksum policy constants

Per-call construction would then:

1. Copy the dense template array.
2. Overlay operation-specific values.
3. Apply request and client override attributes with existing precedence.
4. Resolve request-specific endpoint/auth providers and business metrics.

Constraints:

- Request plugins may create a different effective client configuration.
- Profile-file suppliers and credentials can be dynamic.
- Some attributes contain mutable per-call objects and must never be shared.
- Override precedence must remain request > client > generated defaults.
- Arrays containing mutable values need explicit copy semantics.

**Recommendation:** Build an instrumented prototype that classifies current attributes as immutable-client, operation, request, or attempt scoped. Benchmark construction separately before changing runtime code.

### 6.4 Metric-stage elision remains valid but is not newly identified

Generated clients already use `NoOpMetricCollector` when no publishers are configured. However, the pipeline still:

- Reads timers.
- Constructs `Duration` values.
- Traverses metric wrappers.
- Creates or resets per-attempt byte counters and request-body metrics.
- Adds async completion stages for API metrics.

Racecar already identified metric-stage elision as a follow-up. A safe design should specialize the pipeline when the collector is `NoOpMetricCollector`, while retaining any counters required for retry or internal behavior. This report confirms the mechanism but does not claim it as a new opportunity.

## 7. Strategic representation work

### 7.1 Compact flat-array HTTP headers

Smithy-java stores alternating canonical name/value entries in one `String[]`, uses identity-first name comparison, returns lazy multi-value views, iterates entries without materializing a map, and creates a map only when compatibility APIs request one.

SDK v2 still stores a case-insensitive `TreeMap<String, List<String>>`. Racecar's shallow copy-on-write change removes per-list copies for replacement operations, but a map clone still allocates a tree and entry nodes. The Racecar summary explicitly identifies a strided-array representation as the next structural step.

Potential SDK v2 design:

- Flat alternating name/value storage for internal request and response headers.
- Canonical lowercase token for matching, with preserved original casing where required.
- In-place mutation while exclusively owned.
- Explicit freeze/share transition for built requests.
- Lazy immutable `Map<String, List<String>>` materialization for public APIs and interceptors.
- Allocation-free entry iteration for signers and transports.
- Specialized direct copy/merge between array-backed builders.

Required compatibility matrix:

- Case-insensitive lookup
- Original key casing exposed by maps and iteration
- Sorted or insertion iteration behavior currently observed by callers
- Duplicate names and multi-value order
- Append versus replace semantics
- External caller map ownership
- Builder/buildable aliasing and two-builders-from-one-object behavior
- Equality and hash code
- Concurrent external reads of built objects
- Signer canonical ordering
- Apache5 and CRT transport adaptation

**Recommendation:** Prototype rather than directly replace. Benchmark 0, 4, 8, 16, 32, and unusually large header sets. Compare linear scan, binary search, and small index options. This is strategically promising, but broad.

### 7.2 Builder-aware fast signing

The fast signer still performs:

```text
source.toBuilder() -> managed header puts -> build()
```

once per attempt. Racecar removed the first-attempt retry-header rebuild, but the signer remains another header-map clone. Eliminating it requires an internal builder-aware signing path or an SPI design change that makes request ownership explicit.

A safe design must preserve:

- Each retry beginning from unsigned state
- Re-signing idempotency
- No mutation visible through an already-built request
- Custom signer compatibility
- Presigning, streaming, checksum, and event-stream fallbacks

This was already identified by Racecar. It remains a valid strategic follow-up, not a newly discovered item.

### 7.3 One-object generated response construction

After E4, a large part of batch-get work is genuine value construction rather than collection-spine recopying. The Racecar summary estimates that a smithy-like generated object shape could remove 30–40% of remaining unmarshalling allocation by avoiding a builder-to-immutable-object transition for every nested value.

This needs a fundamentally different generated construction path, for example:

- A package-private generated parsed-fields carrier passed to one model constructor.
- A generated static factory taking final member values.
- A parser-owned mutable state object whose fields are consumed once into the final model.

The E5 negative result is a critical warning: replacing union `EnumSet` bookkeeping produced exactly zero allocation improvement because escape analysis had already removed the confined builder and iterator. Any direct-construction experiment must use a thread-allocation probe and JMH before expanding codegen broadly.

This opportunity was already recorded by Racecar; it is included here because it is likely the largest remaining payload-scaled model change.

## 8. New but lower-confidence opportunities

### 8.1 Lazy response metadata

AWS JSON, XML, and Query response handlers eagerly:

1. Create or augment a `Map<String, String>`.
2. Copy the request ID and first value of every response header.
3. Wrap that map in `AwsResponseMetadata`.
4. Rebuild the modeled response to attach metadata.

Later, `BaseClientHandler` rebuilds the modeled response again to attach `SdkHttpResponse` after response interceptors run.

Racecar E4 made model-to-builder collection adoption cheap: its isolated `toBuilder().build()` mechanism fell from 14,849 to 80 B/op for the batch-get shape. Therefore the old copier profile cannot size the remaining metadata map and two small response/builder objects.

A safe first step is lazy metadata materialization:

- Preserve an immutable response-header backing object.
- Store the request ID directly or resolve it lazily.
- Materialize the complete map only when metadata lookup, equality, hash code, or `toString` requires it.
- Merge Query body metadata without changing precedence.
- Preserve generated service-specific metadata subclasses.

Fusing metadata and raw-HTTP response attachment is harder because it can change interceptor-visible ordering. Today metadata exists before `afterUnmarshalling` and `modifyResponse`, while `sdkHttpResponse` is attached afterward. That ordering must remain unless explicitly redesigned.

**Recommendation:** Add an isolated allocation probe for small JSON/XML/Query responses on current Racecar. Defer implementation until the residual is shown to matter.

### 8.2 Canonical-query fast signing

The fast signer returns immediately for requests without query parameters. For non-empty queries it still creates a `TreeMap` and lists, URL-encodes every name and value, sorts values, and flattens the result.

Smithy-java uses:

- A pooled strided query-pair array.
- In-place pair sorting.
- A fast check for already-canonical RFC 3986 text with uppercase `%XX` escapes.
- A direct single-pair output path.

Potential value is limited to query-bearing signed operations such as S3 and REST/Query services. It does not improve the DynamoDB benchmark.

Required differential tests:

- Empty names and values
- Duplicate names
- Multiple values
- Existing uppercase and lowercase percent escapes
- Encoded slash, space, plus, and Unicode
- Sort by encoded key and then encoded value
- Presigning and query-auth fallback boundaries

**Recommendation:** Profile a query-heavy service first. If material, reuse `V4SigningResources`; do not add unbounded thread-local state.

### 8.3 Generated endpoint base parameters

Generated endpoint resolution builds a new service-specific endpoint-parameter builder and immutable parameter object on every call. Many built-ins are client constants, while others depend on operation, request members, identity/account ID, or overrides.

A qualified codegen optimization could precompute stable base values and only overlay operation/request-dependent fields. General endpoint-result caching is unsafe because endpoint providers are user-replaceable and may have side effects or dynamic behavior.

Possible qualification:

- Generated default endpoint provider only.
- No request or operation context parameters, or an explicit bounded key covering them.
- No dynamic identity/account ID dependency.
- No request endpoint override.
- Immutable endpoint result.

**Recommendation:** Prototype stable base-parameter reuse only. Do not cache arbitrary provider results.

### 8.4 Fused generated serde capacity checks

Smithy-java's serializer can reserve field-name, comma, and fixed-width scalar bytes with one capacity check. Racecar already has pre-encoded field tokens and generated straight-line writes, but some name and value calls may still perform adjacent checks.

There is no current profile identifying capacity checks as a residual hotspot. E2 and E6 have already removed most serde overhead.

**Recommendation:** Defer. If revisited, start with fixed-width boolean/integer/long overloads in component JMH and require exact wire-identity coverage. Avoid a large generated API expansion without repeatable evidence.

## 9. Ideas not recommended

### 9.1 Broad endpoint-result caching

Reject a general cache around arbitrary endpoint providers. It could:

- Suppress user-provider invocations and side effects.
- Retain failures.
- Reuse stale identity/account information.
- Ignore request/operation context parameters.
- Reuse incorrect endpoint headers, auth properties, or business metrics.

Only generated, statically qualified default-provider caching should be considered.

### 9.2 Replace dense execution attributes with sparse chunks

Smithy-java uses lazily allocated 32-element context chunks. Racecar measured approximately 53 densely populated execution attributes per SDK call and implemented a flat ID-indexed array. For the normal dense low-ID case, chunking adds an outer array and multiple objects without reducing occupied slots.

Revisit only if telemetry demonstrates realistic processes with a very high global attribute ID and sparse per-call use.

### 9.3 Claim `invokeSafely` as a measured optimization

`FunctionalUtils.invokeSafely` creates a safe wrapper and immediately invokes it. Direct try/catch can preserve behavior and is reasonable cleanup. However, the JIT may inline and scalar-replace the wrapper, and the retained allocation profiles do not show it as an allocation site. A CPU self frame named for the lambda may contain work attributed through that frame rather than wrapper overhead.

Do not claim a performance win without a targeted microbenchmark across supported JDKs.

### 9.4 Copy smithy-java's transport-buffer retention

Smithy-java's zero-copy JDK response subscriber relies on an explicit JDK ownership contract. SDK v2's pluggable transports do not universally transfer buffer ownership. Retaining CRT or Netty buffers after `onNext` can corrupt data or hold pooled native memory.

Copy once into SDK-owned storage unless a specific HTTP SPI explicitly guarantees ownership transfer.

### 9.5 JDK-internal compact-string access

Smithy-java's widest JSON ASCII path accesses compact-string internals with newer JDK mechanisms and trusted lookup/Unsafe fallback. SDK v2 targets Java 8 source compatibility and broader module, native-image, and security environments. Racecar's four-character grouping is the appropriate safe current compromise.

## 10. Recommended work queue

### Tier 1: prototype now

1. **Direct async response-array handoff to the byte-level JSON reader**
   - E10 delivered the one-copy owned aggregator and removed approximately 128 KB/op.
   - Preserve generic `InputStream`, CRC/gzip, error, and non-JSON fallback behavior.
   - Forward the internal capability through response-handler wrappers without adding an `sdk-core` → protocol dependency.
   - Eliminate the measured remaining ~36 KB/op parser body copy.
   - Pair current Racecar against E10 on async batch-get with sync and non-eligible protocol controls.

### Tier 2: contained implementation candidates

2. **Direct CRT header array and host-field use**
3. **Lazy auth discarded reasons and interceptor-property builder**
4. **Compact endpoint-modification snapshot**, if a microbenchmark confirms signal
5. **Indexed interceptor loops**, if a component benchmark confirms signal

### Tier 3: framework prototypes

6. **Reusable interceptor chain per effective configuration**
7. **Precomputed client execution-attribute template**
8. **Generated endpoint base-parameter reuse**

### Tier 4: strategic architecture

9. **Compact flat-array HTTP headers with lazy map compatibility**
10. **Builder-aware fast signer path**
11. **One-object generated response construction**

### Measure before acting

12. Lazy response metadata
13. Query-pair signer pooling/canonical bypass
14. Fused generated serde capacity checks

### Do not prioritize

15. Broad endpoint caching
16. Sparse chunked execution attributes without telemetry
17. `invokeSafely` as an asserted performance project
18. Transport-buffer retention without ownership guarantees
19. JDK-internal compact-string access

## 11. Validation strategy for new performance work

The Racecar campaign demonstrates that mechanism probes and matched controls are essential. For each candidate:

1. **Prove the mechanism directly.** Use exact thread-allocation probes or component JMH before e2e runs.
2. **Use paired artifacts built from one dependency state.** Alternate arms within a session.
3. **Run a matched null at the same settings** for changes expected below 3%.
4. **Keep a built-in control scenario** that cannot exercise the changed path.
5. **Use actual operation counts** for profile normalization.
6. **Reject non-steady CPU windows.** Prefer latency only when CPU windows fail the compilation check.
7. **Do not compare independently sampled allocation profiles as exact arm-to-arm totals.** Use allocation probes or JMH for small changes.
8. **Retain primary logs/JFRs** if future reports need independent regeneration.
9. **Separate current-Racecar measurements from old 2.54.0 profile sizing.**
10. **Validate semantics at the actual risk boundary:** aliasing, cancellation, retries, buffer ownership, interceptor order, wire identity, or generated compatibility.

## 12. Final assessment

Racecar has already harvested most of the large smithy-java-inspired gains visible in the original cross-SDK profile. The remaining performance work is no longer dominated by one generic framework abstraction. It is concentrated in narrower ownership and representation boundaries:

- Who owns an async response buffer, and how many times is it copied?
- When are HTTP headers forced into maps, lists, transport objects, and signer copies?
- Which request state is truly client constant versus reconstructed per call?
- Which response metadata is eagerly materialized even when callers never inspect it?
- Which internal rebuilds exist solely because public interfaces expose immutable objects rather than owned builders?

E10 validated the ownership diagnosis: replacing the old async aggregator with one SDK-owned array removed
approximately 128 KB/op and improved large async reads by 2.4% CPU / 2.3% latency. The strongest next
move is to carry that owned array through the existing response-handler wrappers to the byte-level JSON
reader, eliminating its measured remaining ~36 KB/op copy without retaining transport buffers or changing
non-JSON behavior. After that, prioritize contained CRT/auth changes and measure the remaining framework
setup before undertaking another broad architecture change.

## E11 measured update

Phase E11 (`6774e92dd1e`) completed the direct handoff proposed after E10. An internal bounded stream
exposes the SDK-owned array/range to eligible generated JSON parsing; CRC/gzip and generic streams
retain the old fallback automatically.

Dedicated-host allocation profiles (`raw/host-e11-alloc-20260903-160054`) remove the
`JsonProtocolUnmarshaller.byteUnmarshallFromJson` body array completely: approximately 37.8 KB/op.
Total `byte[]` allocation falls about 36.3 KB/op and total allocation about 34.9 KB/op (7.0%).

Paired timing (`paired/host-20260903-1502`, seven reps) shows async batch-get CPU −0.9% (6/7,
±0.9%) and latency −1.2% (7/7, ±0.7%); the sync control is zero. This is retained as an exact
allocation win, while the sub-floor CPU value is not claimed as independently significant.

## E12 measured update

Phase E12 (`79687b4e359`) completed the direct CRT header-array change proposed in section 5.1. The
adapter now fills a growable `HttpHeader[]` in one pass, avoids the intermediate list, value streams,
lambdas, and `toArray`, and derives synthetic Host directly from `SdkHttpRequest.host()`. Existing
header ordering, casing, multi-value, empty-list, HTTP/2, Content-Length/Transfer-Encoding, and host
semantics are covered by fifteen focused tests.

Dedicated-host allocation profiles (`raw/host-e12-alloc-20260903-191748`) used 35,000 equal async
small-get operations per arm. The former `createAsyncHttpHeaderList` allocation site falls from
approximately 2,247 B/op to zero. The replacement builder/array sites are approximately 479 B/op,
the HTTP-client category falls about 1,303 B/op (15.4%), and total sampled allocation falls from
44,242 to 42,564 B/op: approximately **−1,678 B/op (−3.8%)**. Per-value `HttpHeader` and CRT JNI
allocations remain, as expected.

Paired timing (`paired/host-20260903-1743`, seven reps, 56/56 successful runs) is neutral within the
measurement floor. Async small-get is +0.3% application CPU / +0.4% latency, and async small-put is
+0.2% CPU / −0.0% latency; paired spreads are 1.8–3.5%. All async timing runs had residual JIT, so no
CPU effect is claimed. Apache5 sync controls also moved within noise. E12 is retained strictly as an
exact allocation-mechanism win.

The current profile also chooses between the two auth follow-ups in section 5.2. The no-change
`doApplyInterceptorModifiedProperties` path samples at approximately 494 B/op, dominated by copied
`HashMap` nodes/table storage, while the eager discarded-reasons `ArrayList` is approximately 30
B/op. Therefore the next isolated phase should make the interceptor-property builder lazy and defer
copying until the first real modification. Lazy discarded diagnostics remain valid but lower priority.

## E13 measured update

Phase E13 (`6d81ac812ae`) makes interceptor signer-property reapplication lazy. Reference-identical
pre/post interceptor options return before traversal; distinct options allocate a copy of the
endpoint-resolved option only after the first value that actually changed. Null, equality,
multi-property, fail-fast, signer, identity, and current-option preservation semantics are covered
by focused tests and the existing S3 sync/async compatibility suite.

Equal 35,000-operation profiles (`raw/host-e13-alloc-20260903-204915`) show the exact
`doApplyInterceptorModifiedProperties` subtree falling from approximately 360 B/op to zero for
sync and 225 B/op to zero for async. Whole-process profile totals disagree across the isolated JVMs,
so no total-allocation delta is claimed. The exact mechanism removal is the retention signal.

Paired timing (`paired/host-20260903-2014`, seven reps, 42/42 successful runs) is neutral: sync
small-get is +0.1% application CPU / +0.2% latency; async is −1.1% CPU / −0.8% latency but every
async run retained JIT activity; the smithy control is +0.3% CPU / +0.9% latency with broad spread.
E13 is retained as an allocation-only improvement.

The candidate profile changes the next priority. Checksum-free requests currently copy the
compatibility auth option to insert an explicit null `CHECKSUM_ALGORITHM`; that otherwise invisible
entry then forces `mergePreExistingAuthSchemeProperties` to copy/build the generated option again.
The next phase should suppress that write only when both the requested checksum and existing signer
property are null, preserving null-as-clear when an existing checksum is non-null. This proven
cascade is larger than the approximately 30 B/op eager discarded-reasons list and should precede it.
