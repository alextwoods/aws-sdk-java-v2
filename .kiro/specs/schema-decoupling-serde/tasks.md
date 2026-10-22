# Implementation Plan: Schema Decoupling and Schema-Based Serde

## Overview

The plan follows the phased order in design section D.13. Each phase ends with a checkpoint that runs that phase's exit tests. All code is Java 8 (`--release 8`), built with Maven. Module builds use the workspace convention:

- first build of a module and its dependencies: `mvn clean install -pl :<module> -P quick --am`
- module-only build (skips tests and checkstyle): `mvn clean install -pl :<module> -P quick`
- module build with tests: `mvn clean install -pl :<module>`

Property-based tests use jqwik 1.8.5. Each property sub-task names its design property and the requirements it validates. Property sub-tasks are optional (`*`). Generated protocol test suites, ported test suites, phase exit tests, and checkpoints are required, because they are phase exit criteria.

Phase 1 generates Schemas only; generated model classes are otherwise unchanged. Phase 2 makes them implement `SerializableStruct`/`ShapeBuilder` in the same task that generates their serde members, and builds the new `ApiOperation`-driven pipeline path (D.6) as new classes. The legacy pipeline stays unchanged for non-schema clients until phase 6 (OD-20). Racecar (`feature/poc/racecar`) is a reference proof of concept and benchmark arm: its concepts are re-implemented here, and no racecar runtime code is brought onto this branch (OD-16).

## Tasks

- [x] 1. Phase 1: Schema core and schema codegen
  - [x] 1.1 Restrict the `services` reactor to the Prototype_Service_Set (OD-7)
    - Edit `services/pom.xml` so `<modules>` lists only `sqs`, `secretsmanager`, `account`, `route53`, `sns`, `cloudwatch` and `dynamodb`; move every other service into a `fleet-services` profile that is off by default
    - Add a `bom-internal`/`aws-sdk-java` aggregator exclusion for the same profile so the root build resolves without non-prototype services
    - Add `services/README-schema-serde.md` noting that non-prototype services cannot regenerate on this branch until phase 6
    - _Requirements: 17.7, 33.1_

  - [x] 1.2 Create the new module skeletons and shared test dependencies
    - Add `pom.xml` and empty source roots for `core/schema`, `core/codecs/codec-commons`, `core/codecs/json-codec`, `core/codecs/cbor-codec`, `core/codecs/xml-codec`, `core/http-binding`, `core/protocols/http-client-protocol`, the five `*-client-protocol` modules, `core/dynamic-client`, `test/client-mock-plugin`, `services-custom/message-payloads` and `codegen-c2j-to-smithy`, with the artifactIds, root packages and dependencies from the design's module layout table
    - Register each module in its parent aggregator (`core/pom.xml`, `core/codecs/pom.xml` (new), `core/protocols/pom.xml`, `test/pom.xml`, `services-custom/pom.xml`, root `pom.xml`) and in `bom/pom.xml` and `bom-internal/pom.xml`
    - Add `net.jqwik:jqwik:1.8.5` (test scope) to root `dependencyManagement`; add a shared `jqwik.properties` (`jqwik.reporting.onlyfailures=false`, seed reporting on failure) under `test/test-utils` resources
    - Confirm each new module inherits `jre.version=1.8` and the `jdk-11-plus` `maven.compiler.release=8` setting
    - _Requirements: 1.3, 41.2, 41.6, 41.7_

  - [x] 1.3 Replace `test/protocol-tests` with `test/smithy-protocol-tests` and `test/smithy-protocol-tests-core`
    - Remove `protocol-tests` and `protocol-tests-core` from `test/pom.xml` `<modules>` and add `smithy-protocol-tests-core` and `smithy-protocol-tests`; leave the legacy sources on disk for the disposition tool in 1.34
    - In `smithy-protocol-tests-core` (`software.amazon.awssdk.protocol.smithytests`): `ProtocolTestHarness` API (`captureRequest`, `respondWith`), `CapturedRequest`, `ProtocolAsserts` (method, uri, query, forbid/require query, resolvedHost, case-insensitive headers, forbid/require headers, `outputEquals`), and body comparators: JSON via `json-utils` `JsonNodeParser`, XML via XMLUnit `similar()`, CBOR via shaded `jackson-dataformat-cbor` trees, form-urlencoded as a decoded-pair multiset, bytes otherwise; an empty expected body matches only an empty body
    - `ProtocolTestSkips` loader for `protocol-test-skips.json` (`[{id, reason, protocol?}]`) and `ProtocolTestSummaryExtension` (JUnit `TestExecutionListener`) that prints pass/fail/skip counts per `@Tag("protocol:<id>")`
    - The transport-backed harness implementation is added in 3.33 once the `ClientTransport` SPI exists
    - _Requirements: 32.4, 32.5, 32.6, 32.9, 32.11_

  - [x] 1.4 Port `ShapeId` and `ShapeType` into `core/schema`
    - `software.amazon.awssdk.schema.ShapeId` with `from`, `fromParts`, `fromOptionalNamespace`, `withMember`, `withoutMember`, `getNamespace`/`getName`/`getMember`, `toString`, `equals`/`hashCode`, `compareTo`; validation errors throw `IllegalArgumentException` naming the input
    - `ShapeType` enum with the Smithy IDL 2 types and `isShapeType`/category helpers
    - Apply the porting conventions (no records, no `Set.of`, one SDK visibility annotation per public type)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.8_

  - [ ]* 1.5 Write property test for ShapeId parsing
    - **Property 1: ShapeId round trip**
    - **Validates: Requirements 2.1, 2.2**

  - [ ]* 1.6 Write property test for invalid ShapeIds
    - **Property 2: Invalid ShapeId rejection**
    - **Validates: Requirements 2.8**

  - [x] 1.7 Port runtime traits and the trait factory registry
    - `Trait` interface, `GenericTrait(ShapeId, Document)`, and SDK-native classes for the default trait set from the design's A.16 list (`RequiredTrait`, `SensitiveTrait`, `DefaultTrait`, `HttpTrait`, `HttpErrorTrait`, `XmlNamespaceTrait`, `EndpointTrait`, `RetryableTrait`, `RequestCompressionTrait`, `PaginatedTrait`, `AwsQueryErrorTrait`, `HttpChecksumTrait`, `JsonNameTrait`, `TimestampFormatTrait`, protocol traits such as `Rpcv2CborTrait`, `AwsJson1_0Trait`, `RestJson1Trait`, ...)
    - `TraitFactory` SPI and `TraitFactories.create(ShapeId, Document)` registry (ServiceLoader plus built-ins); unknown IDs yield `GenericTrait`
    - Includes the minimal `Document` value type the trait factories need (ported smithy-java `Document` core, enhanced in phase 7)
    - _Requirements: 2.5, 2.6, 2.7, 2.9, 2.10_

  - [x] 1.8 Port `TraitKey`
    - `TraitKey.get(Class<T>)` returns one identity-stable key per trait class with a dense integer `id`; `Trait[]` storage indexed by key id
    - Generic-trait lookup by `ShapeId` through a separate `TraitKey`-free path
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [ ]* 1.9 Write property test for trait keys
    - **Property 6: Trait key identity**
    - **Validates: Requirements 4.1, 4.2**

  - [ ]* 1.10 Write property test for trait lookups
    - **Property 7: Trait lookup exactness**
    - **Validates: Requirements 4.3, 4.4, 4.6, 2.5**

  - [x] 1.11 Port the Schema hierarchy
    - `Schema`, `RootSchema`, `MemberSchema`, `DeferredRootSchema`, `SchemaBuilder` (`structureBuilder`, `unionBuilder`, `listBuilder`, `mapBuilder`, `putMember`, `builderSupplier`, `shapeClass`), `Schema.createEnum`, `createIntEnum`, `createOperation`, `createService`, `PreludeSchemas`, and `SchemaIndex` (`getSchema` returns `null` for unknown IDs)
    - Member schemas delegate to their target, and member traits take precedence over target traits; member order is declaration order with a required-member bitmask (OD-2)
    - Recursive shapes through `SchemaBuilder` plus `build().resolve()`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 3.12, 3.13_

  - [ ]* 1.12 Write property test for member schemas
    - **Property 3: Member delegation and trait precedence**
    - **Validates: Requirements 3.6, 3.7, 3.8, 10.4**

  - [ ]* 1.13 Write property test for member indexes
    - **Property 5: Member index invariant**
    - **Validates: Requirements 3.2, 3.3, 3.11**

  - [x] 1.14 Port schema extensions
    - `SchemaExtensionKey<T>`, `SchemaExtensionProvider<T>` (ServiceLoader), `Schema.getExtension(key)` with a plain-read fast path and a per-schema lock on first publish so every caller sees one instance
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 5.11_

  - [ ]* 1.15 Write property test for extension memoization
    - **Property 8: Schema extension memoization**
    - **Validates: Requirements 5.3, 5.4, 5.6, 5.7, 5.8, 5.9, 5.10**

  - [ ]* 1.16 Write property test for concurrent extension access
    - **Property 9: Schema extension concurrent identity**
    - **Validates: Requirements 5.5**

  - [x] 1.17 Define the serde interfaces in `core/schema`
    - Runtime interfaces only; no generated class implements them until 3.7
    - `SerializableShape`, `SerializableStruct` with `sdkSchema()` (OD-1, replacing smithy-java `schema()`), `ShapeBuilder<T>` with `sdkSchema()`, `SmithyEnum`/`SmithyIntEnum` with `sdkSchema()`
    - `ShapeSerializer` (one write method per Smithy type plus `writeStruct`, `writeList`, `writeMap`, `writeNull`, `writeDocument`, `flush`, `close`) and `ShapeDeserializer` (one read method per type plus `readStruct`, `readList`, `readStringMap`, `isNull`, `readNull`, `containerSize`)
    - Record the `sdkSchema()` deviation in the porting map deviations file `docs/design/schema-serde/porting-deviations.md`
    - _Requirements: 6.1, 6.3, 7.1, 7.2, 7.4, 1.2_

  - [x] 1.18 Port the smithy-java `core.schema` unit tests
    - Port the `ShapeId`, `TraitKey`, `Schema`, `SchemaBuilder`, `MemberSchema`, `PreludeSchemas` and extension tests into `core/schema/src/test/java`, adapted to JUnit 5 and Java 8
    - Record each test not ported, with a reason, in `docs/design/schema-serde/porting-deviations.md`
    - _Requirements: 41.1, 1.1_

  - [x] 1.19 Add a Java 8 port-compliance check
    - `PortComplianceTest` in `test/architecture-tests` (the module list grows as codec and protocol modules land) that scans compiled classes with ASM and fails on records, sealed/permitted types, `java.util.concurrent.Flow`, Java 9+ API calls, and public types with zero or more than one SDK visibility annotation
    - Add the module list to a Surefire run under the `jdk8` toolchain profile so the unit tests also run on a Java 8 runtime
    - _Requirements: 1.3, 1.4, 1.5, 1.8, 1.9_

  - [x] 1.20 Port the C2J_Converter into `codegen-c2j-to-smithy`
    - Port `C2jToSmithyConverter` to package `software.amazon.awssdk.codegen.c2j2smithy`; replace `Set.of`; take `C2jModels` as input; depend only on `smithy-model`, `smithy-aws-traits`, `smithy-rules-engine`, `smithy-waiters` and Jackson
    - `ConversionResult(Model, ConversionGapReport)`, `C2jConversionException(service, c2jName)`, and the bundled `c2j-markers.smithy` trait definitions
    - `ConversionGapReport` serialized to `target/c2j-conversion-gaps/<service>.json`, each entry listing property, services and generated-code impact
    - Do not port `SmithyToServiceModel` or the parity runners
    - _Requirements: 17.2, 17.10, 17.14_

  - [x] 1.21 Extend the converter with standard traits
    - Implement every row of the design's "Ported converter adds" table: one protocol trait per `metadata.protocols` entry (including `rpcv2Cbor` and `ec2Query`), `aws.api#service`, sigv4/sigv4a/`@auth`, per-operation auth and `unsignedPayload`, `awsQueryCompatible`, `@httpError` and `awsQueryError`, URI label normalization, `httpChecksum`, `requestCompression`, `@endpoint` and `@hostLabel`, rules-engine context params, endpoint discovery traits, event-stream unions, `ec2QueryName`, IDL 2 enums, `@clientOptional`, `@pattern`, `@mediaType`, reserved-header unbinding, `@paginated`, `waitable`, `endpointRuleSet` and `endpointTests`
    - Keep markers plus gap-report entries for `wrapper`, nonstandard `resultWrapper`, `synthetic`, `#operationName`, `deprecatedName`, `alternateBeanPropertyName`, `authorizers` (OD-19)
    - Unit tests per row using small C2J fixtures under `codegen-c2j-to-smithy/src/test/resources`
    - _Requirements: 17.3, 17.9, 17.10, 17.14, 12.6, 33.2_

  - [x] 1.22 Implement `CodegenPipeline` and wire it into `GenerationMojo`
    - `CodegenPipeline.run(ServiceInputs)` with the six stages from A.14: C2J load (skipped for `smithy-build.json` or `model/*` native models), C2J customizations with per-service `disabledCustomizations` and `WARN customization <name> skipped for <service>: <reason>`, conversion, assembly and validation (stop on `ERROR`/`DANGER`, reporting shape ID and message), Intermediate_Model, generation
    - Generate into a staging directory and move it into place only on success; a failed stage writes nothing
    - `codegen-maven-plugin` gains dependencies on `codegen-c2j-to-smithy`, `smithy-protocol-test-traits` and `smithy-aws-protocol-tests`; the `AwsSdkJavaCodegenPlugin` Smithy build plugin calls the same entry point
    - _Requirements: 17.1, 17.2, 17.4, 17.11, 17.13, 17.14_

  - [x] 1.23 Make the IntermediateModel Smithy-only
    - `IntermediateModel` holds the validated `Model` (`@JsonIgnore`) and `serviceId`; remove C2J-typed fields and views
    - `SmithyIntermediateModelBuilder` populates operation, shape, member, auth, paginator and waiter views from standard traits only (no `com.amazonaws.c2j` reads); `SmithyIntermediateModelPostprocessor` runs IM-level customizations such as `shapeModifiers.emitPropertyName`
    - _Requirements: 17.5, 17.6, 17.7, 17.8, 17.9_

  - [ ]* 1.24 Write property test for marker independence
    - **Property 46: Marker-trait independence** (`MarkerTraitIndependenceTest`: strip all `com.amazonaws.c2j` traits and compare IntermediateModels)
    - **Validates: Requirements 17.9**

  - [x] 1.25 Implement the Trait_Inclusion_Policy and Trait_Initializer SPI
    - `software.amazon.awssdk.codegen.schema.TraitInclusionPolicy.forService`, `TraitInclusionConfig` from `customization.config` `schemaTraits`, `TraitInitializer<T>`, `SchemaCodegenExtension` (ServiceLoader)
    - Effective set: defaults, protocol and auth definition traits plus their `traits` lists, constraint traits excluded (OD-13), config additions and removals, hard exclusions; fail before writing code on removing a default/protocol/auth trait, adding a hard-excluded trait, an invalid ShapeId, or two initializers for one trait
    - Default initializers ported from smithy-java `CoreIntegration` plus `GenericTraitInitializer`; generate a shared constants file of the default trait list for reuse by the Dynamic_Client in 13.1
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10, 9.11_

  - [ ]* 1.26 Write property test for trait inclusion
    - **Property 10: Trait inclusion exactness**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.9, 25.3**

  - [x] 1.27 Implement the Schema_Generator
    - JavaPoet ports of `SchemaFieldOrder`, `SchemasGenerator` and `SchemaIndexGenerator`: closure over operations and service errors, `PreludeSchemas` references, `TopologicalIndex` ordering, greedy partitioning into `<Svc>Schemas`, `<Svc>Schemas1`, … under a 48 KB bytecode estimate without splitting recursive SCCs, `X_BUILDER` emission only for recursive SCCs, separate member and target traits, `OriginalShapeIdTrait`, deterministic iteration
    - `<Svc>SchemaIndex` with per-partition `_initN()` and its `META-INF/services/software.amazon.awssdk.schema.SchemaIndex` entry
    - `<Svc>ServiceSchemas` (`@SdkInternalApi`, `.internal`) with operation schemas and `SERVICE` carrying protocol, auth and `awsQueryCompatible` traits
    - The generator emits Schema classes only; it does not change generated model classes
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.9, 10.10, 3.10_

  - [ ]* 1.28 Write property test for schema graph closure
    - **Property 4: Schema graph closure**
    - **Validates: Requirements 3.9, 3.10, 10.1, 10.3, 10.5, 10.10, 25.5**

  - [ ]* 1.29 Write property test for generator determinism
    - **Property 11: Schema generation determinism**
    - **Validates: Requirements 10.9**

  - [ ]* 1.30 Write property test for partition initialization
    - **Property 12: Partition initialization independence** (fresh class loader per partition)
    - **Validates: Requirements 10.7**

  - [x] 1.31 Generate Schemas for the Prototype_Service_Set
    - Add native Smithy model sources for Secrets Manager and Account (`model/` from `aws-models/<svc>/smithy/model.json`) so they bypass the converter; SQS, Route 53, SNS, CloudWatch and DynamoDB go through the converter (OD-15)
    - Run the pipeline for all seven services; generated model classes are otherwise unchanged (no `SerializableStruct` until 3.7), and the existing codegen golden files for model classes still match
    - Commit the gap reports under `docs/design/schema-serde/c2j-gaps/` and create `docs/design/schema-serde/disabled-customizations.md` (expected empty)
    - _Requirements: 33.5, 33.7, 17.4, 17.10_

  - [x] 1.32 Implement the Protocol_Test_Generator
    - `ProtocolTestGenerator` in `codegen` (enabled by `"protocolTests": true`) following `EndpointProviderTestSpec`: one `@Test` per supported-protocol `HttpRequestTestCase`/`HttpResponseTestCase` with `appliesTo` `client` or unset, `@DisplayName` containing the case id, `@Tag("protocol:<id>")`, classes split at 150 tests
    - `ShapeValueLiteralGenerator` builds inputs and expected outputs from `params` via `Node` walking (never via the SDK deserializer)
    - Request tests use endpoint override, anonymous or fixed credentials, `RetryMode.NONE` and a fixed idempotency token; response tests use the fixed-response harness and `ProtocolAsserts.outputEquals`; error cases assert the generated error class
    - Listed skips get `@Disabled("<reason>")`; an unknown id, empty reason, or an ungeneratable non-skipped case fails codegen naming the id
    - _Requirements: 32.1, 32.2, 32.3, 32.4, 32.5, 32.6, 32.7, 32.8, 41.3_

  - [x] 1.33 Generate protocol test sources for every Protocol_Test_Service
    - `test/smithy-protocol-tests/smithy-build.json` with one projection per Protocol_Test_Service (JsonRpc10, JsonProtocol, RestJson plus validation services, RestXml, RestXmlWithNamespace, AwsQuery, AwsEc2, RpcV2Protocol and `aws.protocoltests.rpcv2cbor#*`); models from `smithy-aws-protocol-tests` and `smithy-protocol-tests` at `${smithy.version}` 1.71.0
    - Generated sources compile in phase 1; Surefire runs only protocols listed in the `protocolTests.enabledProtocols` property (empty in phase 1, extended in 3.34 and 7.21)
    - _Requirements: 32.2, 32.9, 32.11, 33.3, 33.9_

  - [x] 1.34 Generate the legacy protocol test disposition
    - `LegacyDispositionTool` (main class in `test/smithy-protocol-tests/src/tool/java`) that reads the 26 legacy case files, matches each case by protocol, operation and input/response against the Smithy cases, applies the D.1 category rules, and writes `test/smithy-protocol-tests/legacy-disposition.csv` (`legacy file, description, protocol, smithy id|none, disposition, reason`)
    - Commit the CSV after hand review (OD-12); add new SDK-owned Smithy cases (for example `software.amazon.awssdk.protocoltests#RestJsonContentType`) for cases marked ported
    - _Requirements: 32.10_

- [x] 2. Checkpoint: Phase 1 exit tests
  - Build once with dependencies: `mvn clean install -pl :schema,:codegen-c2j-to-smithy,:codegen,:codegen-maven-plugin,:smithy-protocol-tests-core -P quick --am`
  - Run tests: `mvn clean install -pl :schema`, `mvn clean install -pl :codegen-c2j-to-smithy`, `mvn clean install -pl :codegen`, `mvn clean install -pl :architecture-tests`
  - Generate and compile: `mvn clean install -pl :smithy-protocol-tests -P quick`, then `mvn clean install -pl :sqs,:secretsmanager,:account,:route53,:sns,:cloudwatch,:dynamodb -P quick`
  - Exit criteria: ported `core.schema` tests and Properties 1–12 and 46 pass; gap reports exist; prototype Schemas and protocol test sources compile; generated model classes are otherwise unchanged (no `SerializableStruct` yet, model-class golden files match); `legacy-disposition.csv` is committed
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Phase 2: rpcv2Cbor end to end on CloudWatch, and the new pipeline path core
  - [x] 3.1 Port the serde helpers into `core/schema`
    - `SpecificShapeSerializer`, `SpecificShapeDeserializer`, `InterceptingSerializer`, `ListSerializer`, `MapSerializer`, `SerializationException`, `ShapeTranscoder`, and `ToStringSerializer` (sensitive members as `*** Sensitive Data Redacted ***`, unset members omitted, streaming and event-stream members not read)
    - Unknown members in `readStruct` are skipped and reported to the `unknownMember` callback; malformed input throws `SerializationException`
    - _Requirements: 7.3, 7.5, 7.6, 7.7, 7.8, 40.1, 40.2, 40.3_

  - [ ]* 3.2 Write property test for `toString`
    - **Property 20: `toString` redaction**
    - **Validates: Requirements 7.5, 40.1, 40.2, 40.3**

  - [ ]* 3.3 Write property test for the transcoder
    - **Property 21: Transcoder equivalence**
    - **Validates: Requirements 7.6**

  - [x] 3.4 Add the zero-copy body contract to `http-client-spi`
    - Add `default ByteBuffer contentAsByteBufferOrNull()` to `ContentStreamProvider`, returning `null`; Javadoc states that a non-null result is the complete body positioned at its start and that callers must not mutate it (racecar phase A concept, re-implemented here)
    - Override it in the byte-array and `ByteBuffer`-backed providers created by the `ContentStreamProvider` and `RequestBody` factories; SDK wrapper providers that do not change the body bytes return their delegate's buffer, and wrappers that transform the body (for example chunked encoding or compression) return `null`
    - `apache5-client` sends a non-null buffer that has a backing array as a `ByteArrayEntity` over that array, offset and length; `SimpleHttpContentPublisher` in `sdk-core` publishes the buffer without copying; other HTTP clients keep reading the stream
    - Unit tests: the default returns `null`; wrappers propagate or drop the buffer as specified; Apache5 and `SimpleHttpContentPublisher` send the same bytes with no intermediate copy
    - _Requirements: 13.6_

  - [x] 3.5 Implement `codec-commons`
    - `Codec` interface (`createSerializer(OutputStream)`, `createDeserializer(byte[] | ByteBuffer)`), `CodecSettings` base, and shared timestamp and blob helpers; codecs are immutable and thread-safe
    - `ByteBufferOutputStream` exposes its written bytes as a `ContentStreamProvider` whose `contentAsByteBufferOrNull()` (3.4) returns the internal buffer without copying
    - _Requirements: 8.1, 8.3, 8.8, 8.10, 13.6_

  - [x] 3.6 Implement the CBOR codec
    - Port `Rpcv2CborCodec`, `CborSerializer`, `CborDeserializer`, `CborSchemaExtensions` (pre-encoded member names, member lookup tables) and `CborSettings.RPCV2` into `cbor-codec`
    - Encode every Smithy type per the RPCv2 CBOR specification; tolerate unknown members; reject malformed input with `SerializationException`
    - Port the smithy-java CBOR codec tests
    - _Requirements: 8.1, 8.3, 8.5, 8.7, 8.9, 8.10, 41.1_

  - [x] 3.7 Make generated model classes implement `SerializableStruct` with their serde members
    - Interface declarations and their implementations land in this one generator change: generated structures, unions and modeled exceptions declare `implements SerializableStruct` with `sdkSchema()` returning their Root_Schema (OD-1); enums declare `SmithyEnum`/`SmithyIntEnum` with `sdkSchema()`; generated builders declare `ShapeBuilder<T>`
    - Emit per-member `Schema` constants and `static ShapeBuilder<T> newShapeBuilder()` on each class, plus `serializeMembers(ShapeSerializer)`, `getMemberValue(Schema)`, `setMemberValue` on builders, the builder's `deserialize` with an `InnerDeserializer` using `switch(memberIndex)`, and `<Svc>SharedSerde` with static list and map writers (no capturing lambdas per member)
    - `toString()` delegates to `ToStringSerializer`; `sdkFields()` and `sdkFieldNameToField()` are derived lazily from the Schema by `SdkFieldsFromSchema` (OD-14)
    - Add `SchemaSerdeSupport` in `sdk-core` (`SdkBytes` adapters); response metadata and `sdkHttpResponse` are set on the builder before `build()` (replaces racecar E4's `toBuilder` copy)
    - Keep every existing public getter, builder method and static factory (verified in 3.35 and 9.28)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10, 6.11, 6.12, 16.1_

  - [ ]* 3.8 Write property test for `serializeMembers`
    - **Property 13: `serializeMembers` contract**
    - **Validates: Requirements 6.2, 6.5**

  - [ ]* 3.9 Write property test for `getMemberValue`
    - **Property 17: `getMemberValue` consistency**
    - **Validates: Requirements 6.6, 6.10**

  - [ ]* 3.10 Write property test for preset members
    - **Property 18: Deserialize preserves preset members**
    - **Validates: Requirements 6.4**

  - [ ]* 3.11 Write property test for unknown members
    - **Property 19: Unknown-member tolerance**
    - **Validates: Requirements 6.11, 7.8, 8.9**

  - [ ]* 3.12 Write property test for CBOR round trips
    - **Property 14: Codec round trip** (CBOR, CloudWatch generated classes)
    - **Validates: Requirements 6.8, 8.5, 8.6, 41.2**

  - [ ]* 3.13 Write property test for malformed CBOR
    - **Property 15: Malformed input rejection** (CBOR)
    - **Validates: Requirements 7.7, 8.7, 39.4**

  - [x] 3.14 Add operation, service and registry types
    - `ApiOperation<I, O>`, `ApiService`, `SimpleApiOperation` in `sdk-core` (schema, input/output schemas, error schemas, effective auth schemes, error registry)
    - `TypeRegistry` interface (`getShapeClass`, `createBuilder`, `contains`, `empty`, `compose`, `builder` with `putType` and `putLazyType`) in `core/schema`, and `ErrorRegistry` with `resolveCode(rawCode)` for `awsQueryError` codes
    - _Requirements: 10.2, 22.1, 23.1, 23.3, 30.3_

  - [x] 3.15 Generate `<Svc>Operations` and the Error_Registry
    - `<Svc>Operations` (shared by sync and async clients) with one `ApiOperation` field per operation using lazy `Supplier<Schema>` (OD-8)
    - Service-wide `<Svc>ErrorRegistry` with exactly the closure shapes that have the `error` trait, plus the `awsQueryError` code index
    - _Requirements: 23.1, 23.6, 23.7, 36.4_

  - [x] 3.16 Add the ClientProtocol and ClientTransport SPI to `sdk-core`
    - Package `software.amazon.awssdk.core.client.protocol`: `MessageExchange`, `HttpMessageExchange.INSTANCE` (with request and response types), `ClientProtocol`, `ClientProtocolFactory`, `ClientProtocolFactories.forId` (throws `SdkClientException("No ClientProtocolFactory registered for <id>")`), `ProtocolSettings` (service, version, service schema, `baseExceptionBuilder` supplier), `ClientTransport`, `AsyncClientTransport` (OD-17)
    - `SdkClientOption.CLIENT_PROTOCOL`, `CLIENT_PROTOCOL_FACTORY`, `CLIENT_TRANSPORT`; `SdkExecutionAttribute.API_OPERATION`, `CLIENT_PROTOCOL`, `ERROR_REGISTRY` (OD-3, OD-4)
    - Javadoc on all `@SdkPublicApi` types
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.11, 11.12, 11.15, 13.2_

  - [x] 3.17 Implement the SDK HTTP client transport adapters
    - `SdkHttpClientTransport` and `SdkAsyncHttpClientTransport` in `sdk-core` internals: pass method, URI, headers and body through unchanged; send single-buffer bodies without copying via `contentAsByteBufferOrNull()` (3.4); wrap client failures in the SDK's existing exception types; close the wrapped client only when the SDK created it
    - `SdkAsyncHttpClientTransport` aggregates the async response body into one owned buffer with a single copy and marks it owned so `ResponseBodies.asByteBuffer` (3.19) parses it directly (racecar E10, re-implemented here)
    - _Requirements: 13.1, 13.2, 13.4, 13.5, 13.6, 13.7, 13.8_

  - [ ]* 3.18 Write property test for transports
    - **Property 28: Transport pass-through**
    - **Validates: Requirements 13.4, 13.6**

  - [x] 3.19 Implement `http-client-protocol`
    - `HttpClientProtocol` base: `updateServiceEndpoint`, `HttpErrorDeserializer`, `AmznErrorHeaderExtractor`, `ErrorTypeUtils.resolve` with normalization (drop from `:`, absolute vs relative IDs, `resolveCode` first for query and awsQueryCompatible), `AwsServiceException` population (code, message, service name, status, request ID headers), and base-exception fallback that never throws a deserialization exception instead
    - Client-side failures become `SdkClientException("Unable to (un)marshall <op>: <cause>", cause)`
    - Per-`ApiOperation` marshall buffer size hint (an `int[]` indexed by operation ordinal in the protocol instance) for `ByteBufferOutputStream` (racecar E1, re-implemented here)
    - `ResponseBodies.asByteBuffer` reads into an exact-size array when Content-Length is known and a growable buffer otherwise (racecar E3.3 gate), and recognizes the transport's owned buffer so `Codec.createDeserializer(ByteBuffer)` parses it with no copy (racecar E11)
    - _Requirements: 11.7, 11.8, 11.13, 11.14, 23.2, 23.3, 23.4, 23.5, 23.7, 13.6, 34.6_

  - [ ]* 3.20 Write property test for endpoint updates
    - **Property 22: `updateServiceEndpoint`**
    - **Validates: Requirements 11.8**

  - [x] 3.21 Implement `rpcv2-cbor-client-protocol`
    - `AbstractRpcV2ClientProtocol` and `RpcV2CborProtocol` with nested `Factory`, registered in `META-INF/services/software.amazon.awssdk.core.client.protocol.ClientProtocolFactory`
    - `POST /service/<svc>/operation/<op>`, `smithy-protocol: rpc-v2-cbor`, CBOR content type and accept headers, body `__type` error discriminator, `x-amzn-query-mode`/`x-amzn-query-error` for awsQueryCompatible; binding traits ignored
    - _Requirements: 11.1, 11.10, 23.2_

  - [ ]* 3.22 Write property test for rpcv2Cbor error resolution
    - **Property 26: Error resolution** (rpcv2Cbor)
    - **Validates: Requirements 11.7, 11.13, 23.3, 23.4, 23.5, 23.7, 29.1, 29.4, 29.5, 29.6**

  - [x] 3.23 Generate runtime protocol selection
    - `<Svc>BaseClientBuilder` gains `default B protocol(ClientProtocol<?, ?> protocol)` throwing `UnsupportedOperationException`; `Default<Svc>BaseClientBuilder` overrides it into `SdkClientOption.CLIENT_PROTOCOL`
    - Codegen picks the default with priority rpcv2Cbor, awsJson1_0, awsJson1_1, restJson1, restXml, awsQuery, ec2Query and emits one `CLIENT_PROTOCOL_FACTORY` line; fail with `"Service <id> declares no supported protocol; found: [...]"`
    - `build()` throws `IllegalStateException` naming both type pairs when `protocol.messageExchange()` differs from the transport's
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 13.3, 16.3, 10.8_

  - [x] 3.24 Implement the new path's protocol serialization and deserialization steps
    - New `ProtocolMarshalling` and `ProtocolResponseHandling` in `core/sdk-core/.../internal/http/`; only the new pipeline path calls them
    - `ProtocolMarshalling.marshall(params)` runs `protocol.serializeRequest(op, input, attrs, UNRESOLVED_URI)` and, after protocol selection, logs `Using protocol <protocol id> for operation <operation id>` once per call at DEBUG on the `software.amazon.awssdk.request` logger through `software.amazon.awssdk.utils.Logger`, with no member values
    - `ProtocolResponseHandling.handle(op, attrs, errorRegistry, request, response)` invokes `protocol.deserializeResponse`, sets `sdkHttpResponse` on the builder, turns a thrown `SdkServiceException` into `Response.failure` so the retry decision sees a service error, runs `afterUnmarshalling`/`modifyResponse` as today, and hands streaming outputs to the transformer
    - Neither class references generated marshaller or unmarshaller types
    - _Requirements: 37.2, 30.3, 40.4, 40.5_

  - [x] 3.25 Implement `RequestMutationStages`, the shared stages and per-client caches
    - `RequestMutationStages` applies user agent (through a new `UserAgentHeaderCache`, racecar H9), the retry-info header (B2.b), host prefix and the other request mutations of the legacy pipeline in one pass over a single request builder, producing the same headers as the legacy stages
    - Shared stage classes that sync and async both use unchanged: mutation, endpoint resolution applying `protocol.updateServiceEndpoint`, auth resolution through a new per-client `AuthSchemeResolutionCache` (H7) with the placeholder auth scheme built once per client internally (H4; no public `InterceptorContext` change), signing of each attempt from the unsigned request, retry decisions reusing `RetryableStageHelper` logic, and the 3.24 protocol steps
    - New `ExecutionAttributesTemplate` (H10) on the existing `ExecutionAttributes` type, built once per client and also seeding `CLIENT_PROTOCOL` and `ERROR_REGISTRY`; metric steps skip measurement when `MetricUtils.collectsMetrics` is false (H8)
    - Unit tests compare each flattened mutation's output with the legacy stage it replaces
    - _Requirements: 37.6, 37.7, 30.2, 11.8_

  - [x] 3.26 Implement `SyncApiCallPipeline`
    - Straight-line composition (racecar G1 concept, re-implemented): each call is a fixed sequence of direct calls to the 3.25 stages, not a chain of nested `RequestPipeline` wrappers
    - Invokes every `ExecutionInterceptor` hook in the Baseline_Build order from D.6 (`beforeExecution` through `afterExecution`), with the same `Context` types and the same `ExecutionAttributes` keys populated at each hook as on the legacy pipeline; interceptor list membership and order are the legacy ones
    - Transport through the configured `ClientTransport` (default `SdkHttpClientTransport`)
    - Failure reporting: exceptions from `serializeRequest`, the transport or `deserializeResponse` go to the shared retry decision; when retries end, `onExecutionFailure` runs once and the Baseline_Build SDK exception type is rethrown with the cause kept
    - _Requirements: 37.2, 37.5, 37.6, 37.8, 13.1_

  - [x] 3.27 Implement `AsyncApiCallPipeline`
    - Same stage classes, hook order and failure reporting as 3.26 (racecar G2 concept, re-implemented); async-only code is limited to scheduling and future completion (the async retry timer, `CompletableFutureUtils.forwardExceptionTo`)
    - Signing runs inline when the identity future is already complete (racecar C), and the call chain uses fewer future links (H9)
    - Transport through the configured `AsyncClientTransport` (default `SdkAsyncHttpClientTransport`)
    - _Requirements: 37.5, 37.6, 37.7, 37.8, 13.1_

  - [x] 3.28 Add `PipelineCache` and route schema operations to the new path
    - `PipelineCache` builds the sync and async stage graphs once per client configuration, and every call reuses them (racecar H11, re-implemented)
    - Add `ClientExecutionParams.withOperation(ApiOperation)`; the sync and async client handlers select `SyncApiCallPipeline`/`AsyncApiCallPipeline` when `params.getOperation() != null` and the legacy `RequestPipelineBuilder` path otherwise
    - The legacy `RequestPipelineBuilder` stage graph and its stages are not modified; non-schema clients keep using them (OD-20)
    - _Requirements: 37.4, 37.2, 30.3_

  - [x] 3.29 Add interceptor hook-order tests for the new path
    - `ApiCallPipelineInterceptorOrderTest` in `sdk-core` runs the same call through the legacy pipeline and the new path (stub `SimpleApiOperation`, `ClientProtocol` and `ClientTransport`), sync and async, for success, service-error, client-error and retry-then-success cases; asserts identical hook sequences, `Context` types and `ExecutionAttributes` keys at each hook, and exactly one `onExecutionFailure` on failure
    - `CloudWatchInterceptorOrderTest` in `services/cloudwatch` asserts the same sequence for generated sync and async CloudWatch clients; existing interceptor tests run unmodified
    - _Requirements: 37.5, 37.8, 30.2_

  - [x] 3.30 Add stage-graph-built-once tests
    - `PipelineCacheTest` in `sdk-core` asserts one stage graph per client configuration across many sequential and concurrent calls, sync and async, and a separate graph for a client built with a different configuration; a construction counter on the stage graph confirms that no call builds one
    - _Requirements: 37.4_

  - [ ]* 3.31 Write property test for sync and async pipelines on CloudWatch
    - **Property 50: Sync and async pipeline equivalence** (CloudWatch on the new path)
    - **Validates: Requirements 37.7, 37.8**

  - [x] 3.32 Route generated client methods through `<Svc>Operations`
    - Sync and async client generators call `clientHandler.execute(new ClientExecutionParams<>().withOperation(<Svc>Operations.OP)...)` for schema-enabled services (CloudWatch in this phase), with no per-operation marshaller, response handler or exception-mapper code
    - CloudWatch module depends on `rpcv2-cbor-client-protocol` and defaults to rpcv2Cbor
    - `CloudWatchNewPathTest` asserts sync and async CloudWatch calls run on `SyncApiCallPipeline`/`AsyncApiCallPipeline` and never reach the legacy `RequestPipelineBuilder`
    - _Requirements: 23.6, 33.2, 30.3, 37.2_

  - [x] 3.33 Implement the transport-backed protocol test harness
    - `CapturingClientTransport` (records the request and throws `RequestCaptured`, no network) and `FixedResponseTransport` (case `code`, `headers`, body base64-decoded for CBOR) in `smithy-protocol-tests-core`, plus a fixed `IdempotentUtils` generator hook
    - _Requirements: 32.3, 32.5_

  - [x] 3.34 Run the rpcv2Cbor protocol tests
    - Add `smithy.protocols#rpcv2Cbor` to `protocolTests.enabledProtocols`; the tests run on the new pipeline path; fix runtime defects until the suite passes; add every remaining failure to `protocol-test-skips.json` with a reason
    - Add the hand-written JUnit test `UnknownEnumValueProtocolTest` for rpcv2Cbor: through `ProtocolTestHarness` `respondWith` against an SDK-owned client, unknown enum values in a member, a list element, a map key and a map value are kept as the raw wire string (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10, retired `[moved-to-handwritten]`; the legacy rows are awsJson1_1 and restJson1, and rpcv2Cbor gets the same coverage)
    - Add the hand-written JUnit test class `NullDocumentProtocolTest`: through `ProtocolTestHarness` `respondWith`, a null document member (`{"documentValue": null}`) reads as `Document.fromNull()`, not unset (legacy-disposition.csv row json-core-output.json#35, retired `[moved-to-handwritten]`). rpcv2Cbor has no document types (`rpcv2Cbor.NoDocuments`), so the class has no rpcv2Cbor case here; its JSON protocol cases are enabled in 7.21
    - _Requirements: 32.6, 32.7, 32.9, 32.11, 41.3_

  - [x] 3.35 Add japicmp checking for CloudWatch
    - Configure `japicmp-maven-plugin` in `services/cloudwatch/pom.xml` against the Baseline_Build artifact; allow only additions documented in `porting-deviations.md`
    - _Requirements: 16.1, 16.2, 16.4, 16.6, 16.7_

- [x] 4. Checkpoint: Phase 2 exit tests
  - Build once: `mvn clean install -pl :http-client-spi,:apache5-client,:rpcv2-cbor-client-protocol,:cloudwatch,:smithy-protocol-tests -P quick --am`
  - Run tests: `mvn clean install -pl :http-client-spi`, `mvn clean install -pl :apache5-client`, `mvn clean install -pl :schema`, `mvn clean install -pl :codec-commons`, `mvn clean install -pl :cbor-codec`, `mvn clean install -pl :sdk-core`, `mvn clean install -pl :http-client-protocol`, `mvn clean install -pl :rpcv2-cbor-client-protocol`, `mvn clean install -pl :smithy-protocol-tests`, `mvn clean install -pl :cloudwatch`
  - Exit criteria: rpcv2Cbor protocol tests pass (minus recorded skips); Properties 13–20, 22, 26 (rpcv2Cbor) and 28 pass; CloudWatch module tests pass with CloudWatch calls on the new path (3.32); interceptor hook-order tests pass (3.29, Requirement 37.5); the stage graph is built once per client (3.30, Requirement 37.4); japicmp for CloudWatch passes; non-schema clients still run on the unmodified legacy pipeline
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Phase 3: Switch codegen and remove old per-operation components
  - [x] 5.1 Delete the legacy per-operation serde generators
    - Remove the `transform` marshaller generators, `ProtocolSpec` exception-mapper code, per-operation `JsonOperationMetadata`/response-handler setup, per-member `SdkField` constant generation, generated `ToString.builder()` chains, and the C2J IntermediateModel builders from `codegen`
    - Update codegen golden-file tests (`codegen/src/test/resources/.../poet`) to the new output
    - This phase removes per-operation components only: the new pipeline path from phase 2, the legacy `RequestPipelineBuilder` path and the published legacy protocol modules are not changed
    - _Requirements: 17.12, 23.6, 36.4_

  - [x] 5.2 Add `NoTransformPackageTest`
    - Codegen test that fails if any generated source for a prototype service contains a `transform` package, a `*Marshaller`, or a per-operation exception-mapper `switch`
    - _Requirements: 36.4, 37.2_

  - [x] 5.3 Remove CloudWatch's dependency on marshaller types
    - Drop the legacy protocol module dependencies (`aws-json-protocol`, `smithy-rpcv2-protocol`) from `services/cloudwatch/pom.xml`
    - `NoMarshallerDependencyTest` in `test/architecture-tests` scans the CloudWatch JAR and the new-path classes in `sdk-core` (`SyncApiCallPipeline`, `AsyncApiCallPipeline`, `RequestMutationStages`, `ProtocolMarshalling`, `ProtocolResponseHandling`, `PipelineCache`) with ASM and fails on any reference to generated or legacy marshaller and unmarshaller types (`*Marshaller`, `*Unmarshaller`, legacy protocol factories and per-operation response handlers)
    - _Requirements: 37.2, 36.4_

  - [x] 5.4 Create the `test/size-report` module (initial)
    - Builds Baseline_Build and feature jars with the same JDK and flags; records compressed JAR bytes, uncompressed class bytes, class count, bytes per package, and ASM-based attribution into the seven categories (including removed marshallers and removed exception-mapping methods)
    - Writes `docs/design/schema-serde/size-report.md`; CloudWatch is the first row; extended in 9.27
    - _Requirements: 36.1, 36.2, 36.7_

- [x] 6. Checkpoint: Phase 3 exit tests
  - Build once: `mvn clean install -pl :codegen,:cloudwatch,:architecture-tests,:size-report -P quick --am`
  - Run tests: `mvn clean install -pl :codegen`, `mvn clean install -pl :sdk-core`, `mvn clean install -pl :cloudwatch`, `mvn clean install -pl :codegen-generated-classes-test`, `mvn clean install -pl :architecture-tests`, `mvn clean install -pl :size-report`
  - Exit criteria: `NoTransformPackageTest` passes; the CloudWatch size report shows the removed categories; CloudWatch has no dependency on generated marshaller or unmarshaller types (`NoMarshallerDependencyTest`, Requirement 37.2); the 3.29 hook-order tests and the existing interceptor tests pass unmodified
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Phase 4: Remaining codecs and protocols
  - [x] 7.1 Implement the JSON codec
    - Hand-written byte-level port of `SmithyJsonSerializer`, `SmithyJsonDeserializer`, `SmithyMemberLookup` (packed-long and pre-hashed names), `JsonSchemaExtensions` and `JsonCodec` into `json-codec`, with `JsonSettings.AWS_JSON` and `REST_JSON_1` presets (`useJsonName`, timestamp format, `prettyPrint`) and the `JsonSerdeProvider` SPI kept for a future provider (OD-5)
    - Java 8 replacements for `VarHandle`/`Unsafe` paths using plain array loops; racecar's E6 and E7 loop optimizations are measured separately in 9.21 and 9.22; no Jackson databind dependency, enforced with `maven-enforcer` `bannedDependencies`
    - Port the smithy-java JSON codec tests
    - A JSON number read for a string member returns the number's literal text, not a reformatted number (lenient parsing; SDK-owned protocol test `SdkRestJsonNumberIntoStringMember`, legacy-disposition.csv row json-core-output.json#1 restJson1)
    - _Requirements: 8.1, 8.3, 8.4, 8.7, 8.9, 8.10, 41.1_

  - [ ]* 7.2 Write property test for JSON round trips
    - **Property 14: Codec round trip** (JSON)
    - **Validates: Requirements 6.8, 8.5, 8.6, 41.2**

  - [ ]* 7.3 Write property test for malformed JSON
    - **Property 15: Malformed input rejection** (JSON)
    - **Validates: Requirements 7.7, 8.7, 39.4**

  - [x] 7.4 Implement the XML codec
    - Port `XmlCodec`, `XmlSerializer`, `XmlDeserializer` and XML schema extensions into `xml-codec` with `XmlSettings.REST_XML`; honor `xmlName`, `xmlNamespace`, `xmlAttribute`, `xmlFlattened`
    - Port the smithy-java XML codec tests
    - _Requirements: 8.1, 8.3, 8.7, 8.9, 8.10, 41.1_

  - [x] 7.5 Implement the AWS Query form serializer
    - `QueryFormSerializer` in `aws-query-client-protocol` writing `Action`, `Version` and form-encoded members for awsQuery, with an ec2Query variant that applies `ec2QueryName` and ec2 list rules
    - _Requirements: 8.2_

  - [ ]* 7.6 Write property test for XML round trips
    - **Property 14: Codec round trip** (XML)
    - **Validates: Requirements 6.8, 8.5, 8.6, 41.2**

  - [ ]* 7.7 Write property test for malformed XML
    - **Property 15: Malformed input rejection** (XML)
    - **Validates: Requirements 7.7, 8.7, 39.4**

  - [x] 7.8 Implement `http-binding`
    - Port `RequestSerializer`, `ResponseDeserializer`, `BindingMatcher` and `HttpBindingSchemaExtensions` into `software.amazon.awssdk.httpbinding`: labels (including greedy), query params and query maps, headers, prefix headers, payload members, `httpResponseCode`, header timestamp and list formats, and RFC 3986 percent-encoding
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 15.8_

  - [ ]* 7.9 Write property test for HTTP bindings
    - **Property 23: HTTP binding round trip**
    - **Validates: Requirements 15.1, 15.2, 15.3**

  - [ ]* 7.10 Write property test for percent-encoding
    - **Property 24: Percent-encoding**
    - **Validates: Requirements 15.5, 15.6**

  - [x] 7.11 Implement `aws-json-client-protocol`
    - `AwsJsonProtocol`, `AwsJson1Protocol` and `AwsJson11Protocol` with factories: `POST /`, `X-Amz-Target: <service>.<op>`, `application/x-amz-json-1.0|1.1`, `useJsonName=false`, error discriminator from `X-Amzn-Errortype`, `__type`, `code`; awsQueryCompatible headers
    - _Requirements: 11.1, 11.10, 23.2_

  - [ ]* 7.12 Write property test for RPC protocols
    - **Property 25: RPC protocols ignore binding traits** (awsJson1_0, awsJson1_1, rpcv2Cbor)
    - **Validates: Requirements 11.10**

  - [x] 7.13 Implement `rest-json-client-protocol`
    - `HttpBindingClientProtocol` base in `http-client-protocol` delegating to `RequestSerializer`/`ResponseDeserializer`, and `RestJsonClientProtocol` with `JsonSettings.REST_JSON_1`
    - _Requirements: 11.1, 11.9, 23.2_

  - [x] 7.14 Implement `rest-xml-client-protocol`
    - `RestXmlClientProtocol` on `HttpBindingClientProtocol` with `XmlCodec`; error discriminator from `<Error><Code>` or `<ErrorResponse><Error><Code>`
    - _Requirements: 11.1, 11.9, 23.2_

  - [x] 7.15 Implement `aws-query-client-protocol`
    - `AwsQueryClientProtocol` and `Ec2QueryClientProtocol` (null payload codec, `QueryFormSerializer` requests, `XmlCodec` responses with `<OpResult>` unwrapping); errors from `<ErrorResponse><Error><Code>` and `<Response><Errors><Error><Code>` through `ErrorRegistry.resolveCode`
    - _Requirements: 11.1, 23.2, 23.3_

  - [ ]* 7.16 Write property test for error resolution across protocols
    - **Property 26: Error resolution** (awsJson1_0, awsJson1_1, restJson1, restXml, awsQuery, ec2Query)
    - **Validates: Requirements 11.7, 11.13, 23.3, 23.4, 23.5, 23.7, 29.1, 29.4, 29.5, 29.6**

  - [x] 7.17 Support document members in JSON and CBOR protocols
    - `writeDocument`/`readDocument` in the JSON and CBOR codecs using the ported `Document` from 1.7
    - `SchemaSerdeSupport.wrapLegacy`/`unwrapLegacy` lazy views so generated `document` accessors keep returning Legacy_Document without deep copies
    - _Requirements: 20.1, 21.4, 21.7_

  - [ ]* 7.18 Write property test for legacy document members
    - **Property 39: Legacy document members through protocols**
    - **Validates: Requirements 21.7**

  - [x] 7.19 Implement streaming blob members
    - Bind streaming inputs to the HTTP body without buffering; hand streaming outputs to the `ResponseTransformer` from the new path's deserialization step; deserialize error responses for streaming operations as modeled errors instead of streaming them
    - _Requirements: 14.1, 14.2, 14.3_

  - [x] 7.20 Implement event streams
    - Port smithy-java `aws-event-streams` encoders and decoders into `http-client-protocol` on top of the `eventstream` module: outbound events through the event member Schema with `eventHeader`/`eventPayload`, inbound event dispatch by type, modeled exception events, and initial request/response messages where the protocol uses them
    - Keep the generated public event stream API source compatible
    - _Requirements: 14.4, 14.5, 14.6, 14.7, 14.8_

  - [x] 7.21 Run all protocol test suites
    - Add every Supported_Protocol to `protocolTests.enabledProtocols`; fix defects until each suite passes, recording remaining cases in `protocol-test-skips.json` with reasons; `ProtocolTestSummaryExtension` reports pass/fail/skip counts per protocol
    - Extend the hand-written JUnit test `UnknownEnumValueProtocolTest` (from 3.34) to each other Supported_Protocol: through `ProtocolTestHarness` `respondWith` against an SDK-owned client, unknown enum values in a member, a list element, a map key and a map value are kept as the raw wire string (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10, retired `[moved-to-handwritten]` for awsJson1_1 and restJson1)
    - Enable `NullDocumentProtocolTest` (from 3.34) for the protocols with document types, awsJson1_0, awsJson1_1 and restJson1: a JSON null document member (`{"documentValue": null}`) reads as `Document.fromNull()`, not unset (legacy-disposition.csv row json-core-output.json#35, retired `[moved-to-handwritten]` for awsJson1_1 and restJson1)
    - _Requirements: 32.6, 32.7, 32.9, 32.11, 33.3, 33.9, 41.3, 23.8_

- [x] 8. Checkpoint: Phase 4 exit tests
  - Build once: `mvn clean install -pl :aws-json-client-protocol,:rest-json-client-protocol,:rest-xml-client-protocol,:aws-query-client-protocol,:smithy-protocol-tests -P quick --am`
  - Run tests: `mvn clean install -pl :json-codec`, `mvn clean install -pl :xml-codec`, `mvn clean install -pl :http-binding`, `mvn clean install -pl :http-client-protocol`, then each `*-client-protocol` module, then `mvn clean install -pl :smithy-protocol-tests`
  - Exit criteria: all generated protocol tests pass per protocol (minus recorded skips) with counts reported; Properties 21, 23–25 and 39 pass; streaming and event-stream tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Phase 5: Prototype clients, pipeline optimizations, performance and size
  - [x] 9.1 Generate the racecar decision table from a committed YAML
    - `docs/design/schema-serde/racecar-decisions.yaml` has one entry per racecar phase id from D.6 (A, B1, B2.a, B2.b, C, D1, D2, E1, E2.1–E2.4, E2.6, E3.1–E3.3, the E3.3 gate, E4–E16, F, G1, G2, H1–H11; racecar records no E2.5) with optimization, decision, component or replacing mechanism, and reason
    - Decisions use only the D.6 vocabulary: `Applied (re-implemented here)`, `Superseded`, `Deferred (later individual optimization)`, `Rejected`; D2, H1 and H5 are recorded as Deferred pending their own API review
    - `scripts/schema-serde/racecar_decisions.py` writes `docs/design/schema-serde/racecar-decisions.md` from the YAML
    - `RacecarDecisionTableTest` in `test/architecture-tests` fails if the committed Markdown differs from the generated output, a phase id is missing, a decision is outside the vocabulary, an Applied or Superseded component class is not on the classpath, or a row changed to Applied in this phase has no link to its paired A/B record (9.19)
    - _Requirements: 37.1_

  - [x] 9.2 Generate SQS and DynamoDB on the schema path
    - SQS (awsJson1_0 with awsQueryCompatible) and DynamoDB (awsJson1_0, benchmark baseline) through the converter, running on the new pipeline path; module dependencies on `aws-json-client-protocol`; existing module tests pass, with exclusions listed in `docs/design/schema-serde/excluded-tests.md`
    - If any prototype service in 9.2–9.5 blocks, switch it to its D.2 fallback replacement and record the swap in `excluded-tests.md`
    - _Requirements: 33.1, 33.4, 33.5, 33.6, 33.8, 23.2_

  - [x] 9.3 Generate Secrets Manager and Account on the schema path
    - Native Smithy models (awsJson1_1 and restJson1); existing module tests pass, with exclusions recorded
    - _Requirements: 33.1, 33.5, 33.8, 17.4_

  - [x] 9.4 Generate Route 53 and SNS on the schema path
    - Route 53 (restXml) keeps `Route53IdInterceptor` and `deprecatedShapes`; SNS (awsQuery) keeps `shapeModifiers.emitPropertyName` as an IM postprocess customization; existing module tests pass, with exclusions recorded
    - _Requirements: 33.1, 33.5, 33.7, 33.8_

  - [x] 9.5 Enable CloudWatch protocol overrides
    - CloudWatch depends on `rpcv2-cbor-client-protocol`, `aws-json-client-protocol` and `aws-query-client-protocol`; `protocol(...)` overrides to awsJson1_0 and awsQuery work with no regeneration
    - _Requirements: 12.2, 12.6, 12.7, 33.2_

  - [ ]* 9.6 Write property test for protocol swaps
    - **Property 27: Protocol swap equivalence** (CloudWatch rpcv2Cbor vs awsJson1_0 vs awsQuery)
    - **Validates: Requirements 12.2, 12.6, 12.7**

  - [ ]* 9.7 Write property test for sync and async pipelines on all prototype services
    - **Property 50: Sync and async pipeline equivalence** (all seven prototype services on the new path)
    - **Validates: Requirements 37.7, 37.8**

  - [x] 9.8 Enforce prototype dependency trees
    - `maven-enforcer` rule on every prototype service module banning `software.amazon.smithy:*` from compile and runtime scopes, and `DependencyTreeTest` in `test/architecture-tests` asserting `dynamic-client` is the only published module depending on `smithy-model`
    - _Requirements: 1.6, 1.7_

  - [x] 9.9 Add codec convenience methods
    - `Codec.serialize(SerializableShape)` to bytes and `Codec.deserialize(byte[] | ByteBuffer, ShapeBuilder<T>)` on JSON, CBOR and XML codecs, using only the codec and its settings; null arguments throw `NullPointerException` naming the parameter; failures throw `SerializationException`
    - _Requirements: 39.1, 39.2, 39.3, 39.4, 39.5, 39.6_

  - [ ]* 9.10 Write property test for codec convenience methods
    - **Property 16: Codec convenience equivalence and thread safety**
    - **Validates: Requirements 8.10, 39.1, 39.2, 39.6**

  - [ ]* 9.11 Write property tests for codec round trips on all prototype services
    - **Property 14: Codec round trip** (generated classes of all seven prototype services through each applicable codec, with the 41.6 value domain)
    - **Validates: Requirements 6.8, 8.5, 8.6, 41.2**

  - [x] 9.12 Implement the Response_Generator
    - `ResponseGenerator` (`forProtocol` via ServiceLoader) in `client-mock-plugin` with one implementation per Supported_Protocol, using only Schemas, codecs and Http_Binding_Serde: ported REST `ResponseSerializer` and `HeaderErrorSerializer`, awsJson `__type` and `X-Amzn-Errortype`, rpcv2Cbor, query and ec2 envelopes; error status from `httpError` else 400/500; `x-amzn-query-error` for awsQueryCompatible
    - _Requirements: 35.1, 35.2, 35.3_

  - [ ]* 9.13 Write property test for generated responses
    - **Property 48: Response_Generator round trip**
    - **Validates: Requirements 35.1, 35.2, 35.3**

  - [x] 9.14 Implement the Mock_Plugin for generated clients
    - Port `MockPlugin`, `MockQueue` and `MockedResult`; `MockClientTransport`/`MockAsyncClientTransport` installed as `CLIENT_TRANSPORT`; FIFO polling, schema-ID check with `IllegalStateException("Expected one of [..] but queued <id>")`, raw responses returned unchanged, `"Mock queue exhausted"` before any network activity
    - Publish as `software.amazon.awssdk:client-mock-plugin` from a new `test-utils` aggregator (OD-9); `DependencyScopeTest` asserts no generated module depends on it
    - _Requirements: 35.4, 35.5, 35.6, 35.7, 35.8, 35.10_

  - [ ]* 9.15 Write property test for mock queues on generated clients
    - **Property 49: Mock queue semantics across client kinds** (sync and async generated clients)
    - **Validates: Requirements 35.4, 35.5, 35.6, 35.7, 35.9**

  - [x] 9.16 Add the schema-serde JMH benchmarks
    - `SchemaMarshallBenchmark`/`SchemaUnmarshallBenchmark` per protocol in `test/sdk-standard-benchmarks/.../benchmark/schema/` (one ≤ 1 KB and one ≥ 100 KB case with nested struct, list, map, timestamp and blob members), `TraitLookupBenchmark` and `SchemaExtensionBenchmark` (0 B/op assertions with the GC profiler)
    - _Requirements: 34.1, 34.3, 34.4, 34.6, 4.3, 5.7_

  - [x] 9.17 Port racecar's benchmark tooling
    - Copy and adapt racecar's e2e benchmark harness into `test/standalone-e2e-benchmarks` (out-of-process Jetty `MockDdbServer` over loopback; DynamoDB small-get, small-put, describe-table, batch-get and batch-put scenarios), plus `paired-ab.sh` and `paired_ab_summary.py` under `scripts/schema-serde/`; this is benchmark tooling only, and no racecar runtime code comes with it
    - `paired-ab.sh` builds each arm from a named git ref in its own worktree (Baseline_Build, the racecar reference arm from `feature/poc/racecar`, the feature build, or a candidate build) with the same JDK and JVM options, interleaves runs, and supports a null experiment (the same ref as both arms) that reports the noise floor per scenario
    - _Requirements: 37.9, 37.10_

  - [x] 9.18 Add E2E benchmarks and the performance report tooling
    - `ClientColdStartBenchmark` and the DynamoDB `small-get`/`small-put`/`batch-get`/`batch-put` paired A/B runs on the 9.17 harness, reporting `MARSHALLING_DURATION` separately for ≥ 100 KB batch bodies
    - Extend `paired_ab_summary.py` to write `docs/design/schema-serde/perf-report.md` comparing Baseline_Build, the racecar reference arm, smithy-java and the feature build (plus the D.6 bridged column), with mean, error, repetitions, ratio, floor and pass/fail (time ≤ 1.05×, B/op ≤ baseline + floor; B/op ≤ racecar final for DynamoDB) and an expected-cause note for each difference above 5% vs. bridged
    - `scripts/schema-serde/hot_path_check.sh` runs async-profiler on the 34.1 benchmarks and fails on `java.lang.reflect` frames or `HashMap<ShapeId,…>`/`Class`-keyed lookups after warm-up
    - Record the `RuntimeCodegenMode` deferral (OD-6) in the report template
    - _Requirements: 34.2, 34.5, 34.6, 34.7, 34.8, 34.9, 34.10, 34.11, 37.9, 37.10_

  - [x] 9.19 Add the optimization experiment procedure
    - `scripts/schema-serde/optimization_experiment.sh <racecar-id> <candidate-ref>` runs a null experiment on the current feature build, then a paired A/B of the feature build against the candidate on the e2e scenarios and the relevant 9.16 JMH benchmarks
    - A candidate is kept only if its gain on its target scenarios exceeds the null-experiment noise floor and no other scenario regresses beyond the floor; the script writes `docs/design/schema-serde/optimizations/<id>.md` with both runs and the verdict
    - Kept: the YAML row becomes `Applied (re-implemented here)` with a link to the record. Not kept: the change is reverted and the row stays `Deferred (later individual optimization)` or becomes `Rejected`, with the measured numbers
    - Each of 9.20–9.26 is applied on its own, after the previous one is kept or reverted, so every measurement isolates one change
    - _Requirements: 37.1, 37.9_

  - [x] 9.20 Re-implement racecar F (SigV4 fast-path header signer)
    - Re-implement the fast-path header signer, signing-resource reuse and signing-key cache concepts in `core/http-auth-aws` behind the existing signer interfaces; a differential test signs randomized requests with the current and new signers and asserts identical `Authorization` and `X-Amz-*` headers; existing signer tests run unmodified
    - Keep it only per the 9.19 procedure
    - _Requirements: 37.1, 37.9_

  - [x] 9.21 Re-implement racecar E6 (JSON string writer, 4 characters per iteration)
    - Apply the 4-characters-per-iteration loop to the ported JSON writer's string-escape path (Java 8, no `Unsafe`); JSON codec tests and the JSON protocol tests pass
    - Keep it only per the 9.19 procedure, measured against the ported writer
    - _Requirements: 37.1, 37.9_

  - [x] 9.22 Re-implement racecar E7 (JSON whitespace-skip fast path)
    - Add the whitespace-skip fast path to the ported JSON reader; JSON codec tests and the JSON protocol tests pass
    - Keep it only per the 9.19 procedure, measured against the ported reader
    - _Requirements: 37.1, 37.9_

  - [x] 9.23 Re-implement racecar B1 (shallow header copy-on-write)
    - Request and response builders in `http-client-spi` share header storage until the first write; `headers()` and `Builder.headers()` keep today's behavior (H1's snapshot change stays deferred); `http-client-spi` tests run unmodified
    - Keep it only per the 9.19 procedure
    - _Requirements: 37.1, 37.9_

  - [x] 9.24 Re-implement racecar D1 (Apache5 header loop and user-agent prefix)
    - In `Apache5HttpRequestFactory`, filter headers in one loop and build the constant user-agent prefix once; `apache5-client` tests run unmodified
    - Keep it only per the 9.19 procedure
    - _Requirements: 37.1, 37.9_

  - [x] 9.25 Re-implement racecar E12 (direct CRT header array)
    - Build the CRT request header array directly from the SDK request headers in `aws-crt-client` without intermediate collections; `aws-crt-client` tests run unmodified
    - Keep it only per the 9.19 procedure
    - _Requirements: 37.1, 37.9_

  - [x] 9.26 Re-implement racecar E13 (lazy auth-property rebuild)
    - Rebuild auth-scheme properties only when a resolved value differs from the cached one, on a miss of the 3.25 `AuthSchemeResolutionCache`; auth resolution tests run unmodified
    - Keep it only per the 9.19 procedure
    - _Requirements: 37.1, 37.9_

  - [x] 9.27 Complete the Size_Report
    - Extend `test/size-report` to all seven prototype services, separate rows for the `schema`, codec, `http-binding` and protocol module JARs (0 B baseline), JDK version, compiler options and disabled customizations, and a failing marker listing grown categories for any service whose JAR grows
    - Append the reduction-opportunity table with measured deltas
    - _Requirements: 36.1, 36.2, 36.3, 36.5, 36.6, 36.7, 36.8_

  - [x] 9.28 Add API compatibility checks for all prototype services
    - japicmp on all seven service modules and `http-client-spi` against the Baseline_Build; source-compatibility fixtures in `test/schema-serde-source-compat` (Baseline_Build-era code using each service's clients, builders, getters, exceptions and Legacy_Document) compiled against the feature build
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8_

- [x] 10. Checkpoint: Phase 5 exit tests
  - Build once: `mvn clean install -pl :sqs,:secretsmanager,:account,:route53,:sns,:cloudwatch,:dynamodb,:client-mock-plugin,:standalone-e2e-benchmarks -P quick --am`
  - Run tests: `mvn clean install -pl :<svc>` for each of the seven services, `mvn clean install -pl :sdk-core`, `mvn clean install -pl :http-client-spi`, `mvn clean install -pl :http-auth-aws`, `mvn clean install -pl :apache5-client`, `mvn clean install -pl :aws-crt-client`, `mvn clean install -pl :json-codec`, `mvn clean install -pl :client-mock-plugin`, `mvn clean install -pl :architecture-tests`, `mvn clean install -pl :schema-serde-source-compat`, `mvn clean install -pl :size-report`
  - Benchmarks: `mvn clean install -pl :sdk-standard-benchmarks -P quick` and `paired-ab.sh` (including a null experiment); regenerate `perf-report.md` and `racecar-decisions.md`
  - Exit criteria: module tests, japicmp and source-compat fixtures pass; Properties 14–16, 27 and 48–50 pass; `RacecarDecisionTableTest` passes; each of 9.20–9.26 has a committed record with its null experiment and paired A/B result, and only kept changes remain in the code; the perf report meets the 34.3, 34.4 and 37.9 thresholds; the size report meets 36.3
  - Ensure all tests pass, ask the user if questions arise.

> Phase 6 (generate all clients) is post-prototype fleet work and has no tasks in this plan. Replacing the legacy pipeline for all clients happens there (OD-20).

- [x] 11. Phase 7: Documents and type registries
  - [x] 11.1 Implement the enhanced Document interface
    - Extend `software.amazon.awssdk.schema.serde.document.Document` and `Documents`: `type()`, `isType`, numeric `asByte`…`asBigDecimal` with exact coercion rules, `asString`, `asBlob`, `asTimestamp`, `asList`, `asStringMap`, `getMember`, `getMemberNames`, `size`, `discriminator()`, `asShape(ShapeBuilder)`, `Document.of(SerializableShape)`, and `equals(other, NUMBER_PROMOTION)`; coercion failures throw `SerializationException` naming source and target types
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 18.7, 18.8, 18.9, 18.10, 18.11, 18.12_

  - [ ]* 11.2 Write property test for numeric coercion
    - **Property 29: Document numeric coercion**
    - **Validates: Requirements 18.4, 18.5**

  - [ ]* 11.3 Write property test for coercion errors
    - **Property 30: Document coercion errors**
    - **Validates: Requirements 18.7, 18.12**

  - [ ]* 11.4 Write property test for structure documents
    - **Property 31: Structure document views**
    - **Validates: Requirements 18.6, 18.8, 18.10, 18.11, 19.7**

  - [x] 11.5 Implement the Document implementations
    - Schema-backed typed documents over `SerializableStruct` (no copying); JSON and CBOR Protocol_Documents (`JsonDocuments`, `CborDocuments`) with blob base64 and timestamp coercion per settings
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7, 19.8, 19.9, 19.10, 19.11_

  - [ ]* 11.6 Write property test for typed documents
    - **Property 32: Typed document round trip**
    - **Validates: Requirements 19.8, 24.2, 24.3**

  - [ ]* 11.7 Write property test for protocol documents
    - **Property 33: Protocol document round trip**
    - **Validates: Requirements 19.6, 19.11**

  - [ ]* 11.8 Write property test for JSON document coercion
    - **Property 34: JSON document blob and timestamp coercion**
    - **Validates: Requirements 19.4, 19.5, 19.9, 19.10**

  - [x] 11.9 Implement discriminator serialization
    - `__type` writing when `serializeTypeInDocuments` is set, reading and normalization of absolute and relative discriminators, `DiscriminatorException`; serializing a document uses the serializing protocol's settings
    - _Requirements: 20.2, 20.3, 20.4, 20.6, 20.7, 20.8_

  - [ ]* 11.10 Write property test for discriminators
    - **Property 35: Discriminator normalization**
    - **Validates: Requirements 20.2, 20.3, 20.6, 20.7, 20.8, 23.3**

  - [ ]* 11.11 Write property test for serializer settings
    - **Property 36: Serializing protocol's settings apply**
    - **Validates: Requirements 20.4**

  - [x] 11.12 Add the `serdeTests` suite
    - Copy `new-document-type-test-model.json` and `new-document-type-test-cases.json` from the SEP repository into `core/codecs/json-codec/src/test/resources` (OD-11) and implement `DocumentSerdeTestSuite` that runs every case
    - _Requirements: 20.5_

  - [x] 11.13 Implement `DocumentConverter`
    - `software.amazon.awssdk.core.document.DocumentConverter.toSchemaDocument` (lazy view keeping `SdkNumber`) and `toLegacy` (structures and unions as maps, base64 blobs, epoch-second timestamps with ≤ 3 fraction digits); null arguments throw `NullPointerException("document")`; Legacy_Document unchanged
    - _Requirements: 21.1, 21.2, 21.3, 21.5, 21.6, 16.5_

  - [ ]* 11.14 Write property test for legacy round trips
    - **Property 37: Legacy document round trip**
    - **Validates: Requirements 21.1, 21.2**

  - [ ]* 11.15 Write property test for Document to Legacy mapping
    - **Property 38: Document to Legacy mapping**
    - **Validates: Requirements 21.3, 21.5**

  - [x] 11.16 Complete type registries
    - `TypeRegistry.deserialize(Document)` (null discriminator → `DiscriminatorException`, unknown → `UnsupportedOperationException` with the absolute ShapeId), first-wins `compose` with empty-identity, immutability; generate `<Svc>TypeRegistry` (Primary_Type_Registry) from the service `shapes` property (OD-18)
    - _Requirements: 22.2, 22.3, 22.4, 22.5, 22.6, 22.7, 22.8, 22.9_

  - [ ]* 11.17 Write property test for type registries
    - **Property 41: Type registry laws**
    - **Validates: Requirements 22.2, 22.3, 22.4, 22.5, 22.8, 22.9**

  - [x] 11.18 Add `NoPerShapeDocumentCodeTest`
    - Codegen test scanning generated sources for `toDocument`/`fromDocument` and `*DocumentConverter`/`*DocumentSerializer` types outside the exempt registries
    - _Requirements: 24.1, 24.2, 24.3, 24.4_

  - [x] 11.19 Prototype schema serde in S3 Event Notifications
    - Author `s3-event-notification.smithy`; add a types-only codegen mode that generates Schemas; existing model classes implement `SerializableStruct` with `ShapeBuilder`s; `fromJson`/`toJson`/`toJsonPretty` call `JsonCodec`, mapping `SerializationException` to today's exception types
    - Keep the old reader in test scope and add `S3EventNotificationParityTest` over all JSON samples; existing reader, writer, bucket and equals tests run unmodified
    - _Requirements: 38.4, 38.6_

  - [x] 11.20 Implement `message-payloads`
    - `MessagePayloads.serialize`/`deserialize` (builder, registry with `__type`, registry with caller ShapeId) and `MessagePayloadException` with `Cause` (`MALFORMED`, `NON_CONFORMING`, `UNKNOWN_DISCRIMINATOR`); binary codecs use base64 text
    - _Requirements: 38.3, 38.5_

  - [ ]* 11.21 Write property test for typed payloads
    - **Property 51: Typed payload round trip and error classification**
    - **Validates: Requirements 38.3, 38.5**

  - [x] 11.22 Add a typed-payload accessor to the SNS Message Manager
    - Additive method on the parsed SNS message that decodes `Message` through `MessagePayloads`; envelope parsing and signature validation unchanged
    - _Requirements: 38.3_

  - [x] 11.23 Generate the Library_Integration_Analysis
    - `scripts/schema-serde/library_integration_analysis.py` reads `docs/design/schema-serde/library-integration.yaml` (the seven D.7 subjects) plus measured rows from `size-report.md` and an S3 Event Notifications JMH result, and writes `docs/design/schema-serde/library-integration-analysis.md`
    - Add `S3EventNotificationBenchmark` in `test/sdk-standard-benchmarks` comparing the old and new paths on the test samples
    - _Requirements: 38.1, 38.2_

- [x] 12. Checkpoint: Phase 7 exit tests
  - Build once: `mvn clean install -pl :s3-event-notifications,:message-payloads,:sns-message-manager -P quick --am`
  - Run tests: `mvn clean install -pl :schema`, `mvn clean install -pl :json-codec`, `mvn clean install -pl :cbor-codec`, `mvn clean install -pl :sdk-core`, `mvn clean install -pl :codegen`, `mvn clean install -pl :s3-event-notifications`, `mvn clean install -pl :message-payloads`, `mvn clean install -pl :sns-message-manager`
  - Exit criteria: the `serdeTests` suite passes; Properties 29–38, 41 and 51 pass; the S3 Event Notifications suite and parity test pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Phase 8: Dynamic client and mocking completion
  - [x] 13.1 Implement the Schema_Converter in `dynamic-client`
    - Port `SchemaConverter`, `StructDocument`, `ContentDocument` and `SchemaGuidedDocumentBuilder` into `software.amazon.awssdk.dynamicclient.schema`; `RuntimeTraitInclusion` uses the default-trait constants generated in 1.25; traits materialized through `TraitFactories.create`; per-converter `ConcurrentHashMap` cache with no caching on failure; recursion via `recursiveBuilders` under a converter lock; `OriginalShapeIdTrait` honored
    - _Requirements: 25.1, 25.2, 25.3, 25.4, 25.5, 25.6, 25.7, 25.8, 25.9, 1.7_

  - [ ]* 13.2 Write property test for converter equivalence
    - **Property 42: Schema_Converter equivalence** (converted vs generated Schemas for prototype services)
    - **Validates: Requirements 25.1, 25.4, 25.7**

  - [ ]* 13.3 Write property test for converter identity
    - **Property 43: Schema_Converter identity and concurrency**
    - **Validates: Requirements 25.2, 25.9**

  - [x] 13.4 Implement `DynamicClient` construction
    - `DynamicClient.builder()` extending `AwsClientBuilder`, `model`, `serviceId`, `protocol`, HTTP clients, `authSchemeResolver`, `endpointProvider`; service resolution errors (`"A Smithy model is required"`, none, several listed, unknown ID); `service()` returns an `ApiService`
    - _Requirements: 26.1, 26.2, 26.3, 26.4, 26.5, 26.6, 26.7, 26.8, 26.9_

  - [x] 13.5 Implement dynamic protocol, auth, endpoint and transport resolution
    - Fixed-priority protocol selection independent of discovery order, with message-exchange matching and a build error listing traits or `"none found"`
    - Auth from `ServiceIndex.getEffectiveAuthSchemes` mapped to sigv4, sigv4a, bearer and noAuth; one WARN per missing scheme; calls with no usable scheme fail before sending
    - `DynamicEndpointProvider` over `endpointRuleSet` with built-ins and context params (JMESPath `operationContextParams`); `endpointBdd` only if the pinned `smithy-rules-engine` supports it on Java 8 (OD-10)
    - Transport resolution shared with generated clients; mismatches fail the build naming both components
    - _Requirements: 27.1, 27.2, 27.3, 27.4, 27.5, 27.6, 27.7, 27.8, 27.9_

  - [ ]* 13.6 Write property test for protocol selection order
    - **Property 44: Protocol selection order independence**
    - **Validates: Requirements 27.1**

  - [x] 13.7 Implement dynamic operations and document calls
    - `DynamicOperation` cached per name, `DynamicRequest`/`DynamicResponse` (`AwsRequest`/`AwsResponse` implementing `SerializableStruct`), `call`/`callAsync` overloads, `createStruct`
    - Coercion before interceptors with the SDK's tightened rules (unknown members, timestamp numbers and strings, blob strings, integer ranges); `callAsync` failures return failed futures; outputs are `StructDocument`s with the output ShapeId discriminator
    - _Requirements: 28.1, 28.2, 28.3, 28.4, 28.5, 28.6, 28.7, 28.8, 28.9, 28.10, 28.11, 28.12_

  - [ ]* 13.8 Write property test for dynamic input coercion
    - **Property 45: Dynamic input coercion**
    - **Validates: Requirements 28.3, 28.4, 28.5, 28.6, 28.8, 28.10, 28.11**

  - [x] 13.9 Implement dynamic client errors
    - `DocumentException extends AwsServiceException` with `getContents()`; per-operation `compose(operationErrors, serviceErrors)` registries of `SchemaGuidedExceptionBuilder` entries; unresolved errors throw plain `AwsServiceException`
    - _Requirements: 29.1, 29.2, 29.3, 29.4, 29.5, 29.6_

  - [x] 13.10 Run dynamic calls through the shared pipeline
    - `AwsSyncClientHandler`/`AwsAsyncClientHandler` with `withOperation(dynamicOperation)`, so dynamic calls take the same `SyncApiCallPipeline`/`AsyncApiCallPipeline` path as generated clients and only the `ApiOperation` differs; `getOperation` metadata matches generated `<Svc>Operations`; idempotency tokens through `InterceptingSerializer`, compression, checksums, host prefix, auth and pagination from schemas only; auxiliary operations as required constructor arguments (`NullPointerException("createSession")`)
    - _Requirements: 30.1, 30.2, 30.3, 30.4, 30.5, 30.6, 30.7, 30.8, 37.3_

  - [ ]* 13.11 Write property test for operation metadata
    - **Property 40: Dynamic and generated operation metadata**
    - **Validates: Requirements 30.1**

  - [ ]* 13.12 Write property test for wire equivalence
    - **Property 47: Dynamic and generated wire equivalence**
    - **Validates: Requirements 30.2, 30.7, 31.3**

  - [x] 13.13 Implement dynamic streaming
    - `DataStream` documents wrapping `RequestBody`/`AsyncRequestBody`, `Publisher<Document>` event-stream inputs, `ResponseTransformer` overloads for blob outputs, `asEventStream()` outputs; missing or mistyped stream members and sync event-stream calls fail before sending
    - _Requirements: 31.1, 31.2, 31.3, 31.4, 31.5, 31.6_

  - [x] 13.14 Complete the Mock_Plugin for the dynamic client
    - `MockPlugin` installs through the dynamic client's transport seam; schema-backed `StructDocument` outputs and `DocumentException` contents are accepted by `MockQueue`
    - _Requirements: 35.9_

  - [ ]* 13.15 Write property test for mock queues on dynamic clients
    - **Property 49: Mock queue semantics across client kinds** (dynamic client)
    - **Validates: Requirements 35.4, 35.5, 35.6, 35.7, 35.9**

  - [x] 13.16 Add the DC-1…DC-30 behavioral tests
    - One test per case in `core/dynamic-client/src/test/java/.../DynamicClientBehaviorTest`, each with its DC id in the method name, across all Supported_Protocols through prototype and protocol-test models
    - _Requirements: 41.4, 41.5_

- [x] 14. Final checkpoint: Phase 8 exit tests
  - Build once: `mvn clean install -pl :dynamic-client,:client-mock-plugin -P quick --am`
  - Run tests: `mvn clean install -pl :dynamic-client`, `mvn clean install -pl :client-mock-plugin`, `mvn clean install -pl :smithy-protocol-tests`, then the seven prototype service modules
  - Exit criteria: DC-1…DC-30 pass; Properties 40, 42–45, 47 and 49 pass; existing suites of the touched modules pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional property-based tests (jqwik 1.8.5) and can be skipped for a faster MVP. Properties 14, 15, 26, 49 and 50 appear more than once, scoped per codec, protocol, service set or client kind.
- Generated protocol test suites, ported smithy-java tests, the interceptor hook-order tests (3.29), the stage-graph-built-once tests (3.30), `NoMarshallerDependencyTest` (5.3), the `serdeTests` suite, the S3 Event Notifications parity test, DC-1…DC-30, and every checkpoint are required.
- Generated model classes gain `implements SerializableStruct`/`ShapeBuilder`/`SmithyEnum`/`SmithyIntEnum`, `sdkSchema()`, per-member Schema constants and `newShapeBuilder()` only in 3.7, together with their implementations. Phase 1 generates Schemas only.
- The new pipeline path (3.24–3.30) is built as new classes; the legacy `RequestPipelineBuilder` path is not modified and serves non-schema clients until phase 6 (OD-20). Phase 3 removes old per-operation components only.
- Racecar (`feature/poc/racecar`) is a reference proof of concept and benchmark arm, and its runtime code is not brought onto this branch. Phase 2 re-implements the D.6 "Applied (re-implemented here)" concepts as part of the new path. Phase 5 re-implements selected "Deferred" ones one at a time (9.20–9.26), each kept only after a null experiment and a paired A/B measurement (9.19). Racecar's e2e harness, `paired-ab.sh` and `paired_ab_summary.py` are ported as benchmark tooling only (9.17).
- Racecar's public-behavior changes (D2, H1, H5), H2 (depends on H1), H6 and B2.a stay Deferred; this plan has no tasks that apply them. D2, H1 and H5 need their own API review first.
- The size report tool starts in phase 3 (5.4), because the phase 3 exit criterion needs a CloudWatch size report. Phase 5 (9.27) completes it.
- Requirement 42 constrains the design document and has no coding tasks. Requirements 1.1 and 1.2 (Porting_Map) are kept current through `porting-deviations.md` as types are ported.
- Open decisions that need confirmation before the dependent task starts: OD-1 (`sdkSchema()`, tasks 1.17 and 3.7) and OD-7 (reactor restriction, task 1.1).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4", "1.20"] },
    { "id": 2, "tasks": ["1.5", "1.6", "1.7", "1.21"] },
    { "id": 3, "tasks": ["1.8", "1.22"] },
    { "id": 4, "tasks": ["1.9", "1.10", "1.11", "1.23"] },
    { "id": 5, "tasks": ["1.12", "1.13", "1.14", "1.24"] },
    { "id": 6, "tasks": ["1.15", "1.16", "1.17", "1.25"] },
    { "id": 7, "tasks": ["1.18", "1.26", "1.27"] },
    { "id": 8, "tasks": ["1.19", "1.28", "1.29", "1.30", "1.31", "1.32"] },
    { "id": 9, "tasks": ["1.33", "1.34"] },
    { "id": 10, "tasks": ["3.1", "3.4", "3.14"] },
    { "id": 11, "tasks": ["3.2", "3.3", "3.5", "3.15", "3.16"] },
    { "id": 12, "tasks": ["3.6", "3.17", "3.19", "3.33"] },
    { "id": 13, "tasks": ["3.7", "3.18", "3.20", "3.21", "3.24"] },
    { "id": 14, "tasks": ["3.8", "3.9", "3.10", "3.11", "3.12", "3.13", "3.22", "3.23", "3.25"] },
    { "id": 15, "tasks": ["3.26", "3.27"] },
    { "id": 16, "tasks": ["3.28"] },
    { "id": 17, "tasks": ["3.30", "3.32"] },
    { "id": 18, "tasks": ["3.29", "3.31", "3.34", "3.35"] },
    { "id": 19, "tasks": ["5.1"] },
    { "id": 20, "tasks": ["5.2", "5.3", "5.4"] },
    { "id": 21, "tasks": ["7.1", "7.4", "7.8"] },
    { "id": 22, "tasks": ["7.2", "7.3", "7.5", "7.6", "7.9", "7.10", "7.17"] },
    { "id": 23, "tasks": ["7.7", "7.11", "7.13", "7.14", "7.15"] },
    { "id": 24, "tasks": ["7.12", "7.16", "7.18", "7.19"] },
    { "id": 25, "tasks": ["7.20"] },
    { "id": 26, "tasks": ["7.21"] },
    { "id": 27, "tasks": ["9.1", "9.2", "9.9", "9.16", "9.17"] },
    { "id": 28, "tasks": ["9.3", "9.5", "9.10", "9.12", "9.18"] },
    { "id": 29, "tasks": ["9.4", "9.6", "9.13", "9.14", "9.19"] },
    { "id": 30, "tasks": ["9.7", "9.8", "9.11", "9.15"] },
    { "id": 31, "tasks": ["9.20"] },
    { "id": 32, "tasks": ["9.21"] },
    { "id": 33, "tasks": ["9.22"] },
    { "id": 34, "tasks": ["9.23"] },
    { "id": 35, "tasks": ["9.24"] },
    { "id": 36, "tasks": ["9.25"] },
    { "id": 37, "tasks": ["9.26"] },
    { "id": 38, "tasks": ["9.27", "9.28"] },
    { "id": 39, "tasks": ["11.1", "11.16"] },
    { "id": 40, "tasks": ["11.2", "11.3", "11.4", "11.5", "11.17"] },
    { "id": 41, "tasks": ["11.6", "11.7", "11.8", "11.9", "11.13"] },
    { "id": 42, "tasks": ["11.10", "11.11", "11.12", "11.14", "11.15", "11.18", "11.19"] },
    { "id": 43, "tasks": ["11.20"] },
    { "id": 44, "tasks": ["11.21", "11.22", "11.23"] },
    { "id": 45, "tasks": ["13.1"] },
    { "id": 46, "tasks": ["13.2", "13.3", "13.4"] },
    { "id": 47, "tasks": ["13.5"] },
    { "id": 48, "tasks": ["13.6", "13.7"] },
    { "id": 49, "tasks": ["13.8", "13.9"] },
    { "id": 50, "tasks": ["13.10"] },
    { "id": 51, "tasks": ["13.11", "13.12", "13.13", "13.14"] },
    { "id": 52, "tasks": ["13.15", "13.16"] }
  ]
}
```
