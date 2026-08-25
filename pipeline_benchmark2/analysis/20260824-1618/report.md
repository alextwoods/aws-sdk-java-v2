# DynamoDB Client Performance Deep Dive — Collection 20260824-1618

Comparison of AWS SDK for Java **V1** (1.12.797), **V2 sync** (2.54.4-SNAPSHOT, Apache HttpClient 5),
**V2 async** (2.54.4-SNAPSHOT, CRT), and **smithy-java** (1.5.1) DynamoDB clients against an
out-of-process canned-response mock server.

- Raw data: `pipeline_benchmark2/raw/20260824-1618/` (manifest.md records commit, environment, and every command)
- Derived tables and per-case profile summaries: `analysis/20260824-1618/data/`
- Analysis scripts: `analysis/scripts/`
- Environment: Apple M4 Pro (14 cores, 48 GiB), JDK 25.0.2, commit `ac80cf35ab0`
- Parameters: 200,000 measured ops + 20,000 warmup per run, 3 timing reps per case (interleaved),
  single-threaded closed loop, standard 3-attempt retries on every client, no metrics/profilers on timing runs

---

## 1. Executive summary

**smithy-java is the efficiency leader in every scenario, and the gap is mostly attributable to
four specific V2 subsystems, each with a concrete fix path.**

Headline numbers (mean of 3 reps; CPU is whole-process):

| | small-get | small-put | batch-get (25×2 KB) | batch-put (25×2 KB) |
|---|---|---|---|---|
| **User-CPU µs/op** — v1 / v2-sync / v2-async / smithy | 30 / **42** / 55 / **16** | 28 / **41** / 49 / **16** | 342 / **165** / 168 / **100** | 192 / **199** / 197 / **83** |
| **Total-CPU µs/op** | 65 / 79 / 115 / 51 | 64 / 78 / 106 / 52 | 412 / 210 / 265 / 146 | 280 / 287 / 273 / 142 |
| **Wall ops/s** | 9.3k / 8.9k / 6.5k / 11.5k | 9.4k / 9.0k / 6.9k / 10.9k | 2.0k / 3.9k / 2.9k / 5.1k | 2.8k / 2.8k / 2.8k / 5.3k |
| **Allocation KB/op** (client code) | 40 / 61 / 70 / **10** | 37 / 54 / 61 / **7** | 264 / 534 / 744 / **220** | 190 / 205 / 372 / **122** |

Key findings:

1. **V2 sync burns 2.6× the user CPU of smithy-java on small ops and 2.4× on batch-put.**
   The four dominant causes, in order of impact: request **marshalling** (metadata-driven field loop +
   1 KB-initial buffer that doubles its way to ~115 KB of allocation per 50 KB body), **signing**
   (~20.6 KB/op allocated vs smithy's 1.2 KB), **pipeline framework** overhead (11–18% of client CPU;
   per-call `ExecutionAttributes` IdentityHashMap, header-map deep copies, always-on metric stages),
   and (async only) a **full request-body re-copy** (`IoUtils.toByteArray`, 164 KB/op — the single
   largest allocation site in the dataset).
2. **V2 allocates 5–8× more than smithy-java on small ops** (61 KB vs 10 KB per small-get).
   Allocation is the best predictor of the CPU gap: the raw crypto (SHA-256) sample counts are nearly
   identical across clients, confirming the workload itself is fair — the difference is framework churn.
3. **V1 is bimodal**: it beats V2 sync on small-op CPU (30 vs 42 user-µs/op — its pipeline is
   primitive but thin), then collapses on batch-get (3.4× smithy, 2.1× V2) inside its legacy
   `Stack`/`Vector`-based JSON unmarshaller (`Stack.peek` + `Vector.isEmpty` = 25% of client CPU).
4. **V2 async is the slowest client for single-threaded small ops** (55 user-µs/op): every call pays
   CompletableFuture thread hops (12–14% of client CPU in `__psynch_cvwait`) plus the CRT body copy.
   This is an artifact of the closed-loop workload shape as much as the client; it would amortize
   under concurrency, but the per-op allocation (372 KB/op on batch-put) would not.
5. **Metric-semantics discoveries** (relevant to anyone consuming SDK metrics):
   V2's `ApiCallDuration` **excludes marshalling** (measured in `BaseClientHandler` before the
   pipeline timer starts, contradicting the `CoreMetric` javadoc formula); smithy's
   `serialization_duration` happens outside `attempt_duration`; V2 async's `UnmarshallingDuration`
   overlaps `ServiceCallDuration`. Cross-SDK phase tables below correct for these.

Estimated opportunity if the four V2 subsystems adopt smithy-java-style implementations
(sections 6–7): roughly **35–50% of V2 sync's user CPU** on these workloads, with the marshalling
buffer fix alone worth ~15–25% on large writes.

---

## 2. Methodology and fairness

Improvements over the previous round (`pipeline_benchmark/`):

- **Out-of-process server** (Jetty, canned byte-identical responses per operation, routed on
  `X-Amz-Target`): no server CPU or allocation in client profiles.
- **Retry parity**: all four clients run standard token-bucket retries, 3 max attempts
  (V2 `RetryMode.STANDARD`, V1 `maxErrorRetry=2`, smithy `StandardRetryStrategy.maxAttempts(3)`).
  No retries actually occur (server always returns 200), so the measured cost is per-call accounting.
- **No payload-wrapper asymmetry**: DynamoDB has no raw-payload API; each SDK marshals the same
  prebuilt item graph per call. Marshalling is part of the measurement by design.
- Timing runs are clean (no profilers/metrics); CPU/alloc/metrics runs are separate JVM executions.
- 3 interleaved timing reps per case; spread was tight (worst case ±6% wall, mostly ±2%).
- CPU from OSHI `OSProcess` (user/kernel split, whole process — includes event loops, GC, JIT).

Scenario shapes: small item = 12 mixed-type attributes (~0.5 KB); batches = 25 medium (~2 KB) items,
~38 KB batch-get response / ~50 KB batch-put request. Server responses: GetItem 470 B,
PutItem 2 B, BatchGetItem 37,808 B, BatchWriteItem 23 B.

---

## 3. Timing results (detailed)

Mean of 3 reps, [min..max]. Full table: `data/timing-tables.md`.

### small-get

| client | ops/wall-sec | ops/cpu-sec | ops/user-cpu-sec | avg µs/op | cpu_ms (user/sys) |
|--------|-------------:|------------:|-----------------:|----------:|------------------:|
| v1 | 9,345 [9,117..9,547] | 15,468 [15,308..15,699] | 33,010 | 107.0 | 6,059/6,872 |
| v2-sync | 8,915 [8,412..9,201] | 12,613 [12,259..13,105] | 23,631 | 112.4 | 8,476/7,394 |
| v2-async | 6,501 [5,809..6,910] | 8,707 [7,865..9,546] | 18,221 | 154.7 | 11,075/12,039 |
| smithy | 11,469 [11,085..11,901] | 19,700 [19,427..20,008] | 62,948 | 87.2 | 3,178/6,976 |

### small-put

| client | ops/wall-sec | ops/cpu-sec | ops/user-cpu-sec | avg µs/op | cpu_ms (user/sys) |
|--------|-------------:|------------:|-----------------:|----------:|------------------:|
| v1 | 9,442 [8,911..9,902] | 15,564 [14,914..16,660] | 35,387 | 106.1 | 5,675/7,207 |
| v2-sync | 9,030 [8,655..9,409] | 12,789 [11,705..13,543] | 24,595 | 110.9 | 8,189/7,511 |
| v2-async | 6,880 [6,804..7,024] | 9,468 [9,123..9,832] | 20,424 | 145.4 | 9,824/11,319 |
| smithy | 10,910 [10,091..11,383] | 19,077 [17,651..19,970] | 64,271 | 91.9 | 3,119/7,396 |

### batch-get

| client | ops/wall-sec | ops/cpu-sec | ops/user-cpu-sec | avg µs/op | cpu_ms (user/sys) |
|--------|-------------:|------------:|-----------------:|----------:|------------------:|
| v1 | 1,969 [1,943..2,006] | 2,426 [2,393..2,491] | 2,928 | 507.8 | 68,343/14,113 |
| v2-sync | 3,932 [3,873..4,020] | 4,752 [4,594..4,986] | 6,043 | 254.4 | 33,151/8,987 |
| v2-async | 2,863 [2,816..2,926] | 3,779 [3,726..3,835] | 5,940 | 349.4 | 33,688/19,248 |
| smithy | 5,057 [4,904..5,197] | 6,847 [6,624..7,049] | 9,979 | 197.8 | 20,047/9,183 |

### batch-put

| client | ops/wall-sec | ops/cpu-sec | ops/user-cpu-sec | avg µs/op | cpu_ms (user/sys) |
|--------|-------------:|------------:|-----------------:|----------:|------------------:|
| v1 | 2,795 [2,741..2,832] | 3,568 [3,485..3,634] | 5,208 | 357.8 | 38,439/17,627 |
| v2-sync | 2,802 [2,707..2,851] | 3,488 [3,291..3,617] | 5,013 | 357.1 | 40,013/17,417 |
| v2-async | 2,756 [2,704..2,795] | 3,659 [3,602..3,770] | 5,085 | 362.9 | 39,359/15,321 |
| smithy | 5,324 [5,276..5,355] | 7,061 [6,966..7,138] | 11,998 | 187.9 | 16,673/11,653 |

### Ratios vs smithy-java (>1.0 = slower / more CPU)

| scenario | client | wall × | total cpu × | user cpu × |
|----------|--------|-------:|------------:|-----------:|
| small-get | v1 | 1.23 | 1.27 | 1.91 |
| small-get | v2-sync | 1.29 | 1.56 | **2.66** |
| small-get | v2-async | 1.76 | 2.26 | **3.45** |
| small-put | v1 | 1.16 | 1.23 | 1.82 |
| small-put | v2-sync | 1.21 | 1.49 | **2.61** |
| small-put | v2-async | 1.59 | 2.01 | 3.15 |
| batch-get | v1 | 2.57 | 2.82 | **3.41** |
| batch-get | v2-sync | 1.29 | 1.44 | 1.65 |
| batch-get | v2-async | 1.77 | 1.81 | 1.68 |
| batch-put | v1 | 1.90 | 1.98 | 2.30 |
| batch-put | v2-sync | 1.90 | 2.02 | **2.39** |
| batch-put | v2-async | 1.93 | 1.93 | 2.36 |

Observations:

- User CPU is the sharpest discriminator. System CPU (socket syscalls against loopback) is similar
  across clients (6.9–7.5 s for 200k small ops) — the wire work is identical; the *user-code* work
  differs by 2–3.5×.
- Small ops are transport-latency-bound on wall clock (~64 µs of the ~110 µs is HTTP round trip),
  which compresses wall-clock ratios. CPU ratios show the real efficiency picture.
- V1's small-op numbers are *better than V2 sync* — worth internalizing: V2's abstraction layers,
  not "being older/newer", drive the difference.

---

## 4. Component-level (pipeline phase) timings

From each SDK's native metrics facility (separate runs; adds per-call overhead so totals exceed the
timing runs slightly). All values avg µs/op. Full table: `data/metrics-tables.md`.

Metric-semantics corrections applied (verified in code):
- **V2 `ApiCallDuration` excludes marshalling** — `BaseClientHandler.finalizeSdkHttpFullRequest`
  measures `MarshallingDuration` *before* invoking the pipeline that hosts the ApiCall timer
  (`core/sdk-core/.../handler/BaseClientHandler.java:73-76`), contradicting the additivity formula
  in `CoreMetric.API_CALL_DURATION`'s javadoc. The "total incl marshall" row adds it back.
- **smithy `serialization_duration` is outside `attempt_duration`** (the call−attempt gap equals
  serialization across all scenarios), so smithy transport is derived as attempt − sign − deserialize.

### small-get

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| marshall | 0.19 | 0.86 | 1.09 | 0.49 |
| sign | 3.99 | 3.75 | 4.46 | 2.17 |
| endpoint resolve | – | 0.83 | 0.87 | 0.42 |
| http transport | 64.37 | 64.30 | 99.59 | 59.17* |
| unmarshall | 6.90 | 3.68 | 3.57 | 2.31 |
| unattributed | 5.85 | 4.31 | 0.90 | ~0 |
| **total incl marshall** | **81.3** | **77.8** | **110.5** | **64.6** |

### batch-get

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| marshall | 4.29 | 4.53 | 5.67 | 2.30 |
| sign | 4.24 | 4.08 | 5.18 | 3.00 |
| http transport | 139.00 | 78.93 | 302.31† | 78.53* |
| unmarshall | 312.25 | 121.10 | 108.64 | 87.45 |
| **total incl marshall** | **465.8** | **220.6** | **321.3** | **171.8** |

### batch-put

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| marshall | 153.01 | 150.38 | 112.92 | 53.73 |
| sign | 17.17 | 16.89 | 17.02 | 14.33 |
| http transport | 153.61 | 154.81 | 162.96 | 88.35* |
| unmarshall | 0.08 | 0.68 | 0.66 | 0.44 |
| **total incl marshall** | **328.1** | **332.5** | **304.0** | **157.3** |

\* smithy transport derived (attempt − sign − deserialize).
† V2 async's `UnmarshallingDuration` overlaps `ServiceCallDuration` (response unmarshalled inside
the service-call window), so async phases double-count on response-heavy scenarios.

Phase-level takeaways:

- **Marshall, batch-put: V2 150 µs vs smithy 54 µs (2.8×)** — the single biggest attributable phase
  gap in the dataset. Section 6.1.
- **Sign: V2 3.75–4.5 µs vs smithy 2.2 µs on small ops** — nearly 2×, entirely client-side work on
  identical inputs (SHA-256 sample counts match). Section 6.2.
- **Unmarshall, batch-get: V2 121 µs vs smithy 87 µs; V1 312 µs** — V2's streaming
  `JsonUnmarshallingParser` is respectable; the remaining 40% gap is builder/copier churn
  (see allocation table). V1's legacy unmarshaller is the outlier.
- **Transport, batch-put: V1/V2 ~154 µs vs smithy 88 µs.** Same syscalls, so this is user-code
  inside the "transport" window: for V2 the window includes streaming the marshalled body through
  `InputStreamEntity.writeTo` (a stream copy) and the signer's extra body traversal; smithy writes
  the already-materialized ByteBuffer once.
- smithy's own unattributed time is ~0 — its metrics decompose almost perfectly.

---

## 5. Profile deep dives

### 5.1 CPU by category

% of client-code samples (JIT/GC/VM threads and harness excluded from the base; their share of all
samples shown separately). Whole-process asprof, includes warmup. Full: `data/profile-tables.md`.

**small-get**

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| socket-syscall | 57.8% | 58.8% | 64.2% | 72.7% |
| http-client | 18.4% | 4.4% | 5.9% | 13.6% |
| pipeline-framework | 0.1% | **17.8%** | 4.3% | 1.7% |
| signing | 7.6% | 6.7% | 2.8% | 2.5% |
| marshall | 8.2% | 4.6% | 4.1% | 1.8% |
| thread-sync | ~0 | ~0 | **11.8%** | ~0 |
| json | 4.0% | 3.0% | 2.1% | 5.1% |
| retry | 2.5% | 1.0% | 0.9% | 0.5% |
| endpoint-rules | 0.0% | 1.6% | 0.8% | 1.4% |
| _jit-compiler (% of all)_ | 24.8% | 31.6% | 26.4% | 22.3% |
| _client-code samples_ | 1,483 | 1,514 | 2,221 | 1,062 |

Small ops are syscall-bound for everyone (the loopback round trip), but the *absolute* sample counts
matter: v2-sync has 1.4× smithy's client-code samples and v2-async 2.1×. smithy's higher syscall
*percentage* reflects a smaller everything-else, not more syscalls. V2 sync's standout is
**pipeline-framework at 17.8%** (metric collection stages, `ExecutionAttributes`, interceptor
plumbing) vs smithy's 1.7% — pure per-call orchestration cost.

**batch-get**

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| unmarshall | **58.1%** | 30.0% | 10.9% | 9.8% |
| socket-syscall | 17.9% | 24.1% | 52.8% | 34.6% |
| json | 17.3% | 26.9% | 9.0% | **47.9%** |
| pipeline-framework | 0.0% | 10.5% | 4.1% | 0.6% |
| thread-sync | 0.0% | 0.1% | 11.7% | 0.1% |
| _client-code samples_ | 9,634 | 4,546 | 6,272 | 3,209 |

The unmarshall-vs-json split is architectural: smithy's deserialization *is* the Jackson parse loop
(47.9% json, 9.8% deserializer glue); V2 splits the same work between its unmarshaller layer (30%)
and the shaded Jackson parser (26.9%); V1 does most of the work in its own unmarshaller framework
(58.1%) on top of Jackson. V1's hot frames tell the story: `java/util/Stack.peek` 12.9%,
`java/util/Vector.isEmpty` 12.0%, `String.equals` 8.0%, `JsonUnmarshallerContextImpl.testExpression`
4.1% — synchronized legacy collections in the token loop.

**batch-put**

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| marshall | 30.7% | **39.4%** | 42.1% | **3.1%** |
| socket-syscall | 34.4% | 32.2% | 24.5% | 40.2% |
| json | 22.8% | 13.3% | 21.6% | 38.4% |
| crypto | 4.3% | 4.5% | 2.0% | 9.9% |
| pipeline-framework | 0.0% | 5.3% | 2.2% | 0.9% |
| _client-code samples_ | 7,330 | 7,331 | 8,161 | 3,731 |

Crypto sanity check: absolute SHA-256 samples are nearly equal (v2-sync 284, smithy 319
`sha256_implCompressMB` self samples on ~2× the base) — identical signing work, so the category
percentages differ only because smithy's denominator is smaller. **The fairness fixes worked.**

V2 sync batch-put hot self frames (the marshalling story):

| % | frame |
|---:|---|
| 22.2% | `write` (syscall) |
| 4.9% | `java/util/Arrays$ArrayItr.next` |
| 4.8% | `jackson UTF8JsonGenerator._writeStringSegment` |
| 4.6% | `java/util/Arrays$ArrayItr.hasNext` |
| 3.9% | `sha256_implCompressMB` |
| 3.6% | `JsonProtocolMarshaller.marshallPayloadField` |
| 2.7% | `itable stub` (megamorphic dispatch) |
| 2.5% | `SdkField.getTrait` |
| 1.9% | `SdkField.get` |

~10% of all client CPU is *iterator allocation/advance* over `sdkFields()` lists, plus ~4.4% in
`SdkField` trait/value reflection — none of which exists in smithy's generated straight-line
serializers (its top frames are the Jackson generator itself: `_writeStringSegment` 12.8%,
`writeName` 3.9%).

### 5.2 Allocation by category (bytes/op, client code)

**small-get**

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| signing | 14,949 | **20,569** | 22,628 | **1,218** |
| pipeline-framework | 160 | **25,271** | 17,993 | 1,289 |
| http-client | 14,844 | 2 | 8,539 | 1,728 |
| unmarshall | 3,491 | 7,183 | 11,334 | 2,057 |
| json | 3,487 | 3,446 | 3,265 | 3,208 |
| retry | 2,412 | 1,578 | 1,959 | 64 |
| marshall | 715 | 1,237 | 1,856 | 2 |
| endpoint-rules | 0 | 1,337 | 1,356 | 393 |
| **total** | **40,289** | **60,963** | **69,509** | **10,062** |

**batch-put**

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| json | 154,124 | 118,110 | 118,127 | 116,654 |
| pipeline-framework | 186 | 25,018 | **176,494** | 1,277 |
| marshall | 2,240 | **35,902** | 37,432 | 5 |
| signing | 15,109 | 20,538 | 22,885 | 1,425 |
| **total** | **190,083** | **205,292** | **372,456** | **121,708** |

(batch-get totals: v1 264 KB, v2-sync 534 KB, v2-async 744 KB, smithy 220 KB/op — dominated by
unmarshalled model builders + response-map copiers for V2: `BatchGetResponseMapCopier` 22%,
`AttributeValue.builder`+`build` 30%, i.e. the response is materialized, then *copied again* by the
generated copier.)

Top V2-sync small-get allocation sites (of 61 KB/op):

| bytes/op | site | meaning |
|---:|---|---|
| ~4,075 | `ChecksumUtil.lambda$readAll$0` | fresh 4 KB buffer per request to drain the body for SHA-256 |
| ~4,070 | `org/apache/hc/core5 InputStreamEntity.writeTo` | body streamed through another 4 KB copy buffer |
| ~2,509 | `CollectionUtils.lambda$deepCopyMap$1` | header-map (TreeMap) deep copy on builder mutation |
| ~2,338 | `V4CanonicalRequest.getCanonicalHeadersString` | 2 KB StringBuilder + String per request |
| ~1,723 | `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | header restatement for Apache |
| ~1,673 | `AttributeMapCopier.lambda$copy$0` | generated map copier (response item copy) |
| ~1,120 | `ExecutionAttributes.<init>` | `new IdentityHashMap<>(64)` per call |
| ~1,084 | `SdkByteArrayOutputStream.<init>` | fresh marshalling buffer per request |
| ~1,714 | `DefaultAuthSchemeOption` builder + instance | constant auth scheme option rebuilt per call |

smithy small-get, for contrast: total 10 KB/op, led by Jackson `TextBuffer`/`ByteArrayBuilder`
(parser scratch, ~1.3 KB), `ArrayHttpHeaders` (~0.45 KB), context chunks (~0.6 KB). There is no
signing, copying, or framework line item above ~1 KB.

**The V2 async outlier**: `IoUtils.toByteArray` re-copies the already-marshalled body into a new
`byte[]` per request — 164 KB/op on batch-put (43% of all its allocation), via a default-sized JDK
`ByteArrayOutputStream` growing from 32 bytes.

---

## 6. V2 optimization opportunities (code-level, vs smithy-java)

Each item cites the responsible V2 code in this repo and the smithy-java counterpart in
`.kiro/reference/smithy-java` that demonstrates the cheaper design. Ordered by expected impact on
these workloads. (Note: this branch already has two marshalling optimizations — the
`marshallPayloadField` hot-path switch and the zero-copy `contentStreamProvider()` — the items below
are the *remaining* costs.)

### 6.1 Marshalling: buffer sizing + field-loop overhead
**Evidence:** batch-put 39.4% of client CPU, 35.9 KB/op "marshall" allocation + the dominant
`SdkByteArrayOutputStream.write` site (115 KB allocated per ~50 KB body); ~10% CPU in
`Arrays$ArrayItr`, 4.4% in `SdkField.getTrait/get`.

- **Buffer**: `SdkJsonGenerator` creates `new SdkByteArrayOutputStream(1024)` per request
  (`core/protocols/aws-json-protocol/.../SdkJsonGenerator.java:41`); a 50 KB body doubles through
  1→2→4→…→64 KB ≈ 127 KB of cumulative garbage. *Fix*: size from a per-operation hint (e.g. rolling
  average of recent body sizes per operation, or `contentLength` when re-marshalling), and/or reuse
  a per-client pooled buffer. Smithy counterpart: `Codec.serialize` →
  `ByteBufferOutputStream` → `ByteBuffer.wrap(buf, 0, count)` — one growth chain, zero final copy
  (`.kiro/reference/smithy-java/core/.../serde/Codec.java:65`, `io/.../ByteBufferOutputStream.java`).
- **Field loop**: `JsonProtocolMarshaller.doMarshall` iterates `pojo.sdkFields()` — generated as
  `Collections.unmodifiableList(Arrays.asList(...))`, so every nested `AttributeValue` (thousands
  per batch) allocates two iterator objects and probes `DefaultValueTrait` via `getValueOrDefault`
  for each of its ~10 mostly-null fields, plus up to 3 `PayloadTrait` probes
  (`JsonProtocolMarshaller.java:213-248,255-267`; `SdkField.java:274`). *Fixes*: indexed iteration
  over a `List` with cheap `get` (or codegen an array), precompute `isExplicitPayload`/`hasDefault`
  booleans on `SdkField` at class-init, skip the trait probe when the field has no default trait.
  Smithy counterpart: generated `serializeMembers` is straight-line `if (member != null)
  serializer.writeX(SCHEMA, member)` — no metadata loop at all
  (`codecs/json-codec/.../JacksonJsonSerializer.java:214`).

### 6.2 Signing: per-call object graph and triple body traversal
**Evidence:** 20.6 KB/op allocated in signing vs smithy 1.2 KB; signing phase 3.75 µs vs 2.17 µs.

- `ChecksumUtil.readAll` allocates a fresh 4 KB buffer per request just to drain the body through
  the SHA-256 stream (`core/http-auth-aws/.../util/ChecksumUtil.java:85`). The body is then read
  *again* by Apache's `InputStreamEntity.writeTo` (another 4 KB buffer) — and on async, copied a
  third time (6.3). *Fix*: hash the marshalled bytes directly (they exist contiguously inside
  `SdkByteArrayOutputStream`) — `MessageDigest.update(byte[], off, len)` with no drain loop.
- Per-call graph: `DefaultAwsV4HttpSigner.sign` builds `Checksummer` + `V4Properties` +
  `CredentialScope` + capturing-lambda `V4RequestSigner` + `V4PayloadSigner` per request
  (`DefaultAwsV4HttpSigner.java:61-68`); `V4CanonicalRequest` allocates header pair lists, a 2 KB
  StringBuilder, and result strings (`V4CanonicalRequest.java:141-197`); `CredentialScope.scope`
  rebuilds the `date/region/service/aws4_request` string every call.
- The generated `DefaultDynamoDbAuthSchemeProvider.resolveAuthScheme` rebuilds a constant
  `AuthSchemeOption` (builder + property map) per call — trivially cacheable per client/region.
- Smithy counterpart (the template): `SigV4Signer` + pooled `SigningResources` — reusable
  `StringBuilder`/`MessageDigest`/`Mac`/byte-array scratch, strided header array sorted in place,
  hand-rolled date formatting, per-day signing-key cache (`SigningCache`), ASCII narrowing instead
  of `String.getBytes` (`.kiro/reference/smithy-java/aws/aws-sigv4/.../SigV4Signer.java`). The
  same techniques port directly to `V4RequestSigner`/`V4CanonicalRequest`.

### 6.3 Async body re-copy (`SimpleHttpContentPublisher`)
**Evidence:** `IoUtils.toByteArray` = 164 KB/op on batch-put (43% of v2-async allocation), the
largest single site in the dataset.

For every non-streaming async call, `MakeAsyncHttpRequestStage` wraps the request in
`new SimpleHttpContentPublisher(request)` whose constructor does
`IoUtils.toByteArray(p.newStream())` (`core/sdk-core/.../async/SimpleHttpContentPublisher.java:39`)
— re-materializing bytes that already exist in the marshalling buffer, through a 32-byte-initial JDK
`ByteArrayOutputStream` with a final `toByteArray()` copy. *Fixes* (increasing ambition): presize
from the Content-Length header; expose the marshalled buffer from the generator as a
`ByteBuffer` and wrap it (what smithy's `DataStream.ofByteBuffer(codec.serialize(input))` does —
`aws/client/aws-client-awsjson/.../AwsJsonProtocol.java:97`); unify so hash (6.2), publish, and
write all share one materialized body instead of traversing it three times.

### 6.4 Pipeline framework: per-call maps, copies, and always-on metric stages
**Evidence:** 17.8% of v2-sync small-get client CPU and 25 KB/op vs smithy 1.7% / 1.3 KB.

- `ExecutionAttributes` = `new IdentityHashMap<>(64)` per call plus ~20 puts and copies
  (`core/sdk-core/.../interceptor/ExecutionAttributes.java:39`). Smithy counterpart:
  `ChunkedArrayStorageContext` — interned int keys into lazily-allocated 32-slot `Object[]` chunks;
  get = two array indexes (`context/.../ChunkedArrayStorageContext.java`). A typed-array attribute
  store for V2 execution attributes would remove both the map nodes and the hashing.
- Header maps: `SdkHttpFullRequest` builders use `TreeMap(CASE_INSENSITIVE_ORDER)` with
  copy-on-write; the signer's `request.toBuilder()` + `putHeader` triggers a full
  `CollectionUtils.deepCopyMap` (TreeMap + per-entry ArrayList) per request
  (`http-client-spi/.../LowCopyListMap.java:104`, `utils/.../CollectionUtils.java:92`). Smithy
  mutates a modifiable header structure key-by-key during signing — no wholesale copies.
- `ApiCallMetricCollectionStage`/`ApiCallAttemptMetricCollectionStage` always run timers, allocate
  `Duration`s and report into `NoOpMetricCollector` even when no publisher is configured; only some
  `MetricUtils` paths check for NoOp. A collector-is-NoOp fast path (or eliding the stages at
  client construction) is free CPU; the stage wrappers appear on every request stack in the
  profiles.
- Generated response copiers: `GetItemResponse.Builder.item()` runs `AttributeMapCopier.copy`
  (LinkedHashMap rebuild + `unmodifiableMap`) on the just-unmarshalled map — the parser output
  could be adopted directly by the builder (it's never aliased). Same for
  `BatchGetResponseMapCopier` (22% of batch-get allocation).

### 6.5 Not-recommended / lower-value observations

- **V1 batch-get unmarshaller** (58% CPU in `Stack`/`Vector`/`testExpression`): confirms the known
  cost of the legacy design; V1 is maintenance-mode, listed for completeness.
- **V2 async thread hops** (12–14% thread-sync, +27 µs/op vs sync): inherent to completing futures
  across the CRT event loop under a closed loop; concurrency amortizes it. The allocation issues
  (6.3) matter more.
- **Retry accounting** is now comparable and cheap everywhere (0.1–2.5% CPU, ≤2.5 KB/op) — the
  previous round's noRetries-vs-default asymmetry is gone.
- **Metric-definition bugs worth fixing independently**: `ApiCallDuration` excluding marshalling
  (javadoc contradiction), async `UnmarshallingDuration` overlapping `ServiceCallDuration`.

### 6.6 Rough sizing of the opportunity (v2-sync)

| fix | scenario | basis | est. user-CPU saving |
|---|---|---|---|
| 6.1 buffer sizing | batch-put | removes ~2/3 of 115 KB/op marshall-buffer churn + GC | 8–15% |
| 6.1 field loop | batch-put | ~10% CPU iterators + ~4% trait/field reflection | 10–14% |
| 6.2 signing | small ops | 19 KB/op alloc + ~4% CPU + double traversal | 8–12% |
| 6.4 framework | small ops | 17.8% CPU, 25 KB/op | 10–15% |
| 6.3 async copy | batch-put async | 164 KB/op | 15–25% (async only) |

These overlap (GC savings compound), but 35–50% total user-CPU reduction on these workloads is a
defensible target — which would put v2-sync within ~1.3–1.7× of smithy-java rather than 2.4–2.7×.

---

## 7. Caveats and limitations

- **Single-threaded closed loop**: measures per-call pipeline cost, not peak throughput or
  concurrency scaling; v2-async is structurally penalized on wall/latency here.
- **JIT compiler share**: 22–32% of all CPU samples are C2 compiler threads (whole run, spare cores
  available). It inflates whole-process `cpu_ms` for all clients roughly proportionally to code
  size; user-CPU ratios between clients are stable across the 3 reps regardless.
- **Profiles include warmup** (20k of 220k ops, ~9%) and one-time client setup.
- **macOS/M4, JDK 25, loopback HTTP**: absolute numbers will differ on Linux/x86 with real
  networks; the *relative* structure (allocation sites, category splits) is what to act on. The
  procfs cross-check source exists for the first Linux run.
- **Mock server parses nothing**: correctness of each SDK's requests is not validated per-call
  (all four SDKs were verified to round-trip real shapes; a DynamoDB Local pass can re-verify).
- **v2-sync used Apache HttpClient 5** (`apache5-client` on classpath, `org.apache.hc.core5` in
  profiles) — the current default for this SNAPSHOT; earlier rounds used Apache 4.
- **Metrics runs perturb timing** — never compare metric-run RESULT lines with timing-run CSV rows.
- Alloc profiles measure allocation *pressure* (bytes allocated), not live heap.

## 8. Data inventory

| artifact | path |
|---|---|
| Timing rows (48 = 16 cases × 3 reps) | `raw/20260824-1618/results.csv` |
| Per-case CPU/alloc JFR + collapsed | `raw/20260824-1618/<caseid>/{cpu,alloc}.{jfr,collapsed}` |
| Per-case SDK metrics | `raw/20260824-1618/<caseid>/metrics.txt` |
| Run manifest (commit, env, commands) | `raw/20260824-1618/manifest.md` |
| Aggregated tables | `analysis/20260824-1618/data/{timing,metrics,profile}-tables.md` |
| Per-case profile summaries | `analysis/20260824-1618/data/{cpu,alloc}-<caseid>.txt` |
| Analysis scripts | `analysis/scripts/*.py` |
