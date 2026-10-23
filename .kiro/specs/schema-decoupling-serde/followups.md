# Follow-ups and open questions: schema decoupling branch

Tracks follow-up work and unresolved questions found on `feature/pov/schema-decoupling` after the spec was written. Design-time open decisions are in `design.md` §D.15 (OD-1 to OD-20); this file references them instead of repeating them. Per-task porting choices are in `docs/design/schema-serde/porting-deviations.md`.

Each item has an ID (`FU-n`), a status (`Open`, `Needs decision`, `Needs benchmark`, `Done`), the evidence, and a proposed next step. Add new items at the end; don't renumber.

## Size and performance

### FU-1 Generic collection serde instead of per-shape `<Svc>SharedSerde` classes
- **Status:** Needs benchmark
- **Context:** Codegen emits one nested class per list or map shape (writer, reader and `read()` pre-sizing helper), ported from smithy-java's `SharedSerdeGenerator`. A single runtime helper could get the element writer from `listMember().type()` and the element builder from `memberTarget().shapeBuilder()`. The `writeList`/`readList` callbacks don't pass the list schema to the consumer, so the helper has to be cached per list or map schema (for example as a schema extension) to avoid a capturing lambda per call (design D.3, Requirement 6.7).
- **Evidence:** `size-report.md` "Size-reduction opportunities" row 1: compressed savings of −5,306 B (Account) to −162,192 B (DynamoDB), the largest single item. The recorded reason for per-shape classes is racecar E2.3 (`racecar-decisions.md`: "elements are not type-tested per call"), which has not been measured against a per-schema cached helper.
- **Next step:** JMH run on list-heavy cases (DynamoDB `BatchWriteItem`, `ListAttributeValue`, SQS attribute lists), generated helpers vs. a per-schema cached generic helper. Also useful to the dynamic client, which needs a generic version anyway.

### FU-2 Size goal fails on every prototype service
- **Status:** Open
- **Context:** Requirement 36.3 (no growth in generated service JARs) fails today. `size-report.md`: +13.0% (Secrets Manager) to +17.5% (SNS) compressed; CloudWatch +263,968 B (+15.5%). The report's 11 size-reduction opportunities together would meet the goal for all seven services; none is applied.
- **Next step:** Decide which opportunities to apply on the branch (FU-1 is row 1; rows 3 and 6 are next largest) and re-run `test/size-report`. The design doc's size goal wording should match whatever is decided.

### FU-3 Performance goal vs. the gate the branch measures
- **Status:** Needs decision
- **Context:** The design doc's goal says SERDE "must be better" than today. `perf-report.md` (marked PRELIMINARY) fails a case only when the µs/op ratio exceeds 1.05.
- **Next step:** Pick one bar, use it in both the doc and the perf gate, and refresh `perf-report.md` before it's cited in round 2.

### FU-4 No serializer pool for `QueryFormSerializer`
- **Status:** Done (`e708e4b55f3`, branch `fix/marshall-allocation`). Sized instead of pooled: `QueryClientProtocol` keeps a `MarshallBufferSizeTracker` of the capacity each operation's last form body needed (`QueryFormSerializer.requiredCapacity()`, which includes the worst-case reservation of each value, so the next body does not grow) and takes the body with `size()`/`finishArray()`, without the two `ByteBuffer` views of `finish()`. Pooling would still need `StripedPool` across the `codecs`→`protocols` boundary and a copy at `finish()`; it is no longer needed. Laptop JMH (JDK 25, PRELIMINARY, all marshall fixes together): `PutMetricDataRequest_Baseline` 2,344 → 1,112 B/op, `_M` 13,761 → 8,793, `_L` 184,398 → 105,708, `GetMetricDataRequest_L` 37,009 → 14,057 (no 1.5x growth chain); `GetMetricDataRequest_S` (a ~0.9 KB body, which 1 KiB already fit) 2,784 → 2,536. Report: `.agents/tasks/fix-marshall-allocation/report.md`.
- **Context:** smithy-java pools `QueryFormSerializer` and its buffer (`StripedPool`). The port allocates a new serializer and a 1 KiB buffer per request, because `StripedPool` is `@SdkInternalApi` in `codecs.commons.internal` and `InternalApiBoundaryTest` forbids using it from `protocols` (`porting-deviations.md`, task 7.5). In exchange, `finish()` doesn't copy.
- **Next step:** Measure with the awsQuery serialize benchmark. If pooling matters, move `StripedPool` to a protected API or a shared internal module.

## Protocols

### FU-5 `payloadCodec()` returns `null` for awsQuery and ec2Query
- **Status:** Needs decision
- **Context:** smithy-java's `AwsQueryClientProtocol.payloadCodec()` returns the response `XmlCodec`. The port returns `null`, per Requirement 11.4 and the `ClientProtocol` Javadoc (a payload codec is a single format used in both directions) (`porting-deviations.md`, task 7.15). smithy-java reviewers are likely to question this.
- **Next step:** Decide whether `payloadCodec()` means "the request and response format" (keep `null`) or "the codec responses and errors are read with" (return `XmlCodec`). Update the round 2 doc's Codecs and Protocols sections either way.

### FU-6 Legacy `QueryParametersToBodyStage` double conversion
- **Status:** Open (legacy path only)
- **Context:** On the legacy path, `QueryProtocolMarshaller` writes members as raw query parameters, then `QueryParametersToBodyStage` (`sdk-core`, unchanged on the branch) re-encodes them through `encodedQueryParametersAsFormData()` into a `String`, then `byte[]`, and moves them to the body. The new path writes the form body directly with `QueryFormSerializer`, so the stage (and its copy in `RequestMutationStages.queryParametersToBody`) is a no-op there because the body is already set.
- **Next step:** Remove both once no query-protocol service uses the legacy path (OD-20). Until then, consider skipping the check entirely on the new path.

## Codegen and models

### FU-7 C2J-converted services don't target prelude shapes
- **Status:** Open
- **Context:** The C2J converter turns every C2J shape, including a plain `String`, into a service-local Smithy shape. So SQS emits `SqsSchemas.STRING` (`com.amazonaws.sqs#String`) instead of referencing `PreludeSchemas.STRING`; only native Smithy models (Account, Secrets Manager) use `PreludeSchemas`. Five of the seven prototype services are C2J-converted (OD-15). The design doc's Context section assumes there are no C2J paths.
- **Next step:** Decide whether the converter should map trait-free C2J simple shapes to prelude targets (smaller `<Svc>Schemas`, shared identity) or whether this goes away once the fleet uses native Smithy models (Smithy migration phases 1–2). Either way, the design doc should say which.

### FU-8 Required-trait validation is a codegen setting, off by default
- **Status:** Needs decision
- **Context:** `requiredTraitValidationEnabled` is a `CustomizationConfig` field, default `false`; it controls whether generated code validates `@required` members. It is not a runtime switch.
- **Next step:** Confirm the default matches today's V2 behavior for every protocol, and document it in the round 3 compatibility section.

### FU-9 `GetQueueUrlRequest` worked example is edited
- **Status:** Open (doc)
- **Context:** The design doc's SQS example shows `PreludeSchemas.STRING`; real SQS output targets the service-local `STRING` (FU-7). The `SchemasGenerator` Javadoc (lines 56–58) uses the same edited example.
- **Next step:** Resolve with FU-7, or keep the edit and say so in the doc.

## Testing

### FU-10 `Java8ApiGuardTest` is planned but not implemented
- **Status:** Open
- **Context:** `design.md` (L179–203, D.12, D.16) plans a per-module Java 8 API guard. No class with that name exists on the branch, and `core/schema/pom.xml` has no animal-sniffer configuration.
- **Next step:** Add the guard to the ported modules (`schema`, `codec-commons`, the codecs, `http-binding`, the client protocols), or drop it from the spec and rely on the Java 8 compile.

## Bugs

### FU-11 Greedy `httpLabel` members lose `GREEDY_PATH` on the legacy path
- **Status:** Open
- **Context:** `SdkFieldsFromSchema.location()` (`core/sdk-core/src/main/java/software/amazon/awssdk/core/schema/SdkFieldsFromSchema.java`, around line 263) maps every `httpLabel` member to `MarshallLocation.PATH` and never to `GREEDY_PATH`. The feature codegen's intermediate model also drops `greedy` for the same members (`http.greedy` true → false, `marshallLocation` `GREEDY_PATH` → `PATH`). A client that has no `stagedClientProtocols`, and so still runs the legacy marshallers over schema-derived `SdkField`s, sends a greedy label such as S3's `{Key+}` unexpanded. The schema path expands it correctly.
- **Evidence:** Found by the serde A/B wire dump (`.agents/tasks/serde-benchmark-comparison/`, `comparability-matrix.md` items M1 and W1): the 4 `CopyObject` marshall rows of `RestJsonMarshallBenchmark` and `RestXmlMarshallBenchmark` differ only in the `{Key+}` URI when the legacy reference runs on the feature classpath. The measured benchmark paths are not affected.
- **Next step:** Return `GREEDY_PATH` when the member's URI label ends in `+` (from the operation's `@http` URI pattern), keep `greedy` in the Smithy-built intermediate model, and add a legacy-path test with a greedy label.

### FU-12 Base64 encoding copies read-only blob members
- **Status:** Done (`cf3dff2dff5`, `cca1bca22d1`, `976f12c8e66`). Generated models write blob members, list elements and map values with `writeBlob(Schema, byte[])` from `asByteArrayUnsafe()`; `InterceptingSerializer`, `ListSerializer` and the JSON, XML, http-binding and awsQuery serializers read the array, and the `ShapeSerializer` default passes a read-only view to serializers that don't override it. Wire bytes unchanged (`SerdeBenchmarkEquivalenceTest` 142/142, protocol tests 923/0/16 before and after). Laptop JMH, JDK 25, preliminary: JsonRpc10 marshall `BinaryData_S` 3,152 → 2,736 B/op, `_M` 32,620 → 14,376, `_L` 274,676 → 112,034; RpcV2Cbor `BinaryData_*` unchanged (CBOR copied no extra bytes before); non-blob cases unchanged within fork-to-fork variation. Remaining: read-only, sliced and direct `ByteBuffer`s that don't come from generated code (dynamic client, `Document` blobs, user buffers) still take the JDK's copying path, and awsQuery still builds the encoded bytes before percent-encoding them. Report: `.agents/tasks/fix-blob-member-base64/report.md`.
- **Context:** Generated models hand blob members over as `SdkBytes.asByteBuffer()`, a read-only view. In the JSON and XML codecs, `ByteBufferUtils.base64EncodeInto` takes its non-exact branch for those buffers, and Java 8's `Base64.Encoder.encode(ByteBuffer)` copies a source that has no accessible array. So every blob member is copied once before it is encoded. This is the same root cause as the `@httpPayload` copy fixed in `1f5f257a7f2`, but that fix (sharing the buffer) doesn't apply to encoded members.
- **Evidence:** `.agents/tasks/fix-blob-payload-copy/report.md` ("Other call sites"). It is the likely source of the JsonRpc10 marshall `format` allocation (about 13 KB/op) and of the feature's higher B/op on the `BinaryData` cases (1.07–1.23x the baseline in the laptop smoke).
- **Next step:** Encode in chunks from the read-only buffer into the output, or have codegen pass the `SdkBytes` array without a copy. Measure with `JsonRpc10MarshallBenchmark` and `RpcV2CborMarshallBenchmark` `BinaryData_*`.

### FU-13 `PayloadSerializer.writeString` copies the encoded string twice
- **Status:** Open
- **Context:** A string `@httpPayload` is encoded to bytes and then copied once more into the body. The encoding is required; the second copy is not.
- **Evidence:** `.agents/tasks/fix-blob-payload-copy/report.md` ("Other call sites"). No benchmark case covers it.
- **Next step:** Write the encoded bytes into the body buffer directly. Add a benchmark case if this shape matters for a prototype service.

### FU-14 Response body reads allocate the declared `Content-Length` up front
- **Status:** Open
- **Context:** Since `4acbd0b03ae`, `Crc32ResponseValidation` (every DynamoDB response) reads through `ResponseBodyReader` into `new byte[Content-Length]`, capped only near `Integer.MAX_VALUE`. A broken endpoint that declares a huge `Content-Length` and sends a short body now fails with `OutOfMemoryError`, where the old `IoUtils.toByteArray` read failed with `UncheckedIOException`. The `ResponseBodies` known-length path and smithy-java's `readAllBytes` already behave this way. Separately, the per-operation size hint for chunked bodies can leave the next small body of the same operation in an untrimmed array of up to 1 MiB; nothing retains it today.
- **Evidence:** `.agents/tasks/fix-response-body-copy/review.md` (findings 1 and 2).
- **Next step:** Cap the up-front exact allocation (a few MiB) and read past the cap through the growth path. Any future zero-copy change for response blobs must wrap only exact arrays, or trim them.

### FU-15 Remaining response-path copies and parses
- **Status:** Open
- **Context:** Left by `4acbd0b03ae`: (a) the async `ResponseBodyAggregator` still grows from a fixed size for bodies of unknown length, because the transport has no operation to key the size hint on; (b) awsQuery parses the response body a second time to extract the request id, 36.5–37.2% of the feature's `QueryUnmarshallBenchmark` CPU in the metal profile `20261023-095831` (about 36% in the laptop profile); the serde report's comparable view counts this pass against the feature, because the legacy unmarshaller also extracts the request id in its timed method (`parseMetadata`); (c) the `MarshallBufferSizeTracker.record` Javadoc still describes only serialized request bodies, though it now also records response sizes.
- **Evidence:** `.agents/tasks/fix-response-body-copy/report.md` ("Remaining copies and why"); `.agents/tasks/serde-benchmark-comparison/comparability-matrix.md` item A2; `docs/design/schema-serde/serde-perf-report.md` §2.4 and §4.1.
- **Next step:** (b) is the largest: extract the request id during the main parse. Pass the operation's size hint into the async aggregator for (a). Fix the Javadoc for (c).

### FU-16 RpcV2Cbor `BinaryData_S` marshall is slower with the FU-12 change on metal
- **Status:** Open
- **Context:** The FU-12 change (`cf3dff2dff5`, `cca1bca22d1`) moves generated blob member writes from `writeBlob(Schema, ByteBuffer)` to `writeBlob(Schema, byte[])`. CBOR copied no extra bytes before, and the fix report expected no CBOR change.
- **Evidence:** m6g.metal run `20261023-111214` (`docs/design/schema-serde/serde-perf-report.md` §3.1): `rpcv2Cbor_PutItemRequest_BinaryData_S` feature-fu12 / feature is 1.086 ns/op (per repetition 1.075, 1.052, 1.112, 1.103; floor 3.8%*), 1,022–1,074 vs 951–979 ns/op. `_M` and `_L` are 0.987 and 0.974, and B/op is unchanged within floor on all three. The serde report's set 2 keeps the pre-FU-12 CBOR rows.
- **Next step:** Profile `rpcv2Cbor_PutItemRequest_BinaryData_S` on both jars (CPU, `-XX:+DebugNonSafepoints`) and compare the `CborSerializer` / `CborStructSerializer` `byte[]` blob path with the `ByteBuffer` path it replaced.
## Marshall allocation (from `fix/marshall-allocation`)

### FU-17 Codecs write straight into `ByteBufferOutputStream`
- **Status:** Done (`2bcbde49aa0`, branch `fix/marshall-allocation`). `ByteBufferOutputStream.reserve(int)`/`advance(int)` (new `@SdkPublicApi` methods) let the JSON and CBOR serializers write a `ByteBufferOutputStream` sink in place: no scratch buffer and no drain copy, at any body size. Other sinks keep a 1 KiB buffer lent by `SinkBufferPool` (`fbcbaa44120`). Laptop JMH (PRELIMINARY): about −1.0 KB/op on every awsJson and rpcv2Cbor case (for example `PutItemRequest_Baseline` 2,456 → 1,040 and 2,816 → 1,208 B/op with the other fixes), and −5 to −6 KB/op on `BinaryData_L`, where the scratch buffer used to grow. A caller that writes or resets the stream between serializer flushes gets the bytes a buffering sink gives (`14eb252b531`); one whose write lands on bytes the serializer has not added gets `IllegalStateException` (`porting-deviations.md`, `SmithyJsonSerializer` and `CborSerializer` rows).
- **Remaining:** the event stream encoder (`AwsEventShapeEncoder`) serializes payloads into a `ByteArrayOutputStream`, so it takes the pooled-buffer path and copies once; switching it to `ByteBufferOutputStream` would remove that copy. `SmithyXmlSerializer` still has its own buffer (restXml request bodies; no benchmark case has one).

### FU-18 `LowCopyListMap` creates its empty maps eagerly
- **Status:** Open
- **Context:** `SdkHttpFullRequest.builder()` creates an empty `TreeMap` for headers and a `LinkedHashMap` for query parameters (about 110 B/request) even when no query parameter is ever set, and `build()` adds two `ForBuildable` wrappers and a `Lazy` (about 175 B). The legacy path pays the same. It is an `http-client-spi` framework change shared with the legacy path.
- **Next step:** Create the maps on first write, with racecar B1 (task 9.23).

### FU-19 Small awsQuery requests stay above the comparable baseline
- **Status:** Open (boundary item)
- **Context:** `awsQuery_PutMetricDataRequest_Baseline` is 1,112 B/op on the fix (laptop) against a comparable baseline of 902 (raw 952, serde-only 285; metal profile split). The remainder is the form body (about 115 B) and the `QueryFormSerializer` objects (serializer 63 B, prefix buffer 144, prefix stack 48, four inner serializers 115). The legacy client builds the form body after the timed method, in `QueryParametersToBodyStage` (FU-6, about three times the body). Every other awsQuery case is 0.45–0.70x comparable.
- **Next step:** If the remaining 200 B matters: create the map and list inner serializers and the prefix stack on first use (−60 to −120 B on requests without maps), and size the prefix buffer from the operation's last request. Measuring end to end (including `QueryParametersToBodyStage`) would show the legacy client's later cost.

### FU-20 Per-operation request constants in the RPC protocols
- **Status:** Open
- **Context:** Every awsJson request builds `targetPrefix + operation.name()` for `X-Amz-Target`, every rpcv2Cbor request builds `targetPathPrefix + operation.name()` for its path, and each static header value is wrapped in a new `singletonList`. The legacy marshallers use static constants. About 70–185 B/op.
- **Evidence:** `.agents/tasks/fix-marshall-allocation/plan.md` (inventory, F8); metal profile run `serde-benchmark-metal/runs/20261023-095831`.
- **Next step:** Cache them per operation in each protocol, as `QueryClientProtocol.responseCodecs` caches response codecs (bounded map keyed by operation name).

### FU-21 Check `awsQuery_GetMetricDataRequest_S` and `rpcv2Cbor_PutItemRequest_BinaryData_S` marshall time on metal
- **Status:** Needs benchmark
- **Context:** On the laptop, both single-fork rounds had `awsQuery_GetMetricDataRequest_S` 1.45–1.53x slower on `fix/marshall-allocation` (p50 542 → 875 and 834 ns), while the 5-fork run had equal p50s on both jars (541–625 ns) and one 2,200 ns fork on the fix. No change in F2 explains a slowdown (the form array is about 1,080 B instead of 1,024). FU-16 (rpcv2Cbor `BinaryData_S` slower after FU-12) is on the CBOR write path that F1(b) rewrote.
- **Evidence:** `.agents/tasks/fix-marshall-allocation/verification.md` (laptop timing flags); `/tmp/fma-jmh/{base,fix}-gmd-f5.log`.
- **Next step:** Report both cases, before and after this branch, in the metal run. If the awsQuery slowdown reproduces there, profile it (CPU, `-XX:+DebugNonSafepoints`) on both jars.

## Still open from the spec (`design.md` §D.15)

Not re-checked against the branch; listed so they aren't lost.

- **OD-6** Runtime bytecode codegen (`RuntimeCodegenMode`): deferred until phase 5 benchmarks.
- **OD-8** Eager vs. lazy operation schemas in `<Svc>Operations`: lazy today; switch to eager if `ClientColdStartBenchmark` shows no difference.
- **OD-17** `AsyncClientTransport` shape: revisit when a non-HTTP async transport is needed.
- **OD-18** Primary type registry contents (usually empty for AWS models).
- **OD-19** Gap-report items with no standard trait (`wrapper`, `deprecatedName`, ...): resolve fleet-wide later.
- **OD-20** Whether the new pipeline replaces the legacy pipeline for all clients (yes, in phase 6).
