# Requirements Document

## Introduction

This feature brings Serialization and Schema Decoupling (schema-based serde), the enhanced Document type with Type Registries, and the Dynamic Client to the AWS SDK for Java v2. It is phase 3 of the Smithy migration. The cross-SDK contracts are defined by three SEPs in `.kiro/reference/`:

1. `serialization-schema-decoupling.md`: runtime `Schema`, `ShapeSerializer`/`ShapeDeserializer`, `Codec`, `ClientProtocol`, `ClientTransport`.
2. `document-types-and-type-registries.md`: full-Smithy-data-model `Document`, protocol-specific documents, `TypeRegistry`.
3. `Dynamic-Client.md`: a model-driven client that shares one schema-driven pipeline with generated clients and requires schema extensions.

smithy-java (`.kiro/reference/smithy-java`) implements all three, but it targets JDK 21+. The SDK targets Java 8, so smithy-java cannot be used as a dependency. Instead, its runtime schema, serde, codec, protocol, document, type registry, schema extension, dynamic client, and mock designs are ported as close to 1:1 as Java 8 allows, under SDK v2 namespaces.

Codegen changes from C2J-driven to Smithy-driven. C2J models are converted to Smithy before code generation, using the converter from `mtdowling/aws-sdk-java-v2@smithy-java-bridge:codegen-c2j-to-smithy`. The codegen order becomes: C2J → C2J customizations → C2J-to-Smithy conversion → Smithy model → IntermediateModel → generated code. The IntermediateModel drops every C2J concept and uses Smithy concepts (ShapeId, traits, a retained `Model`). This branch already has a Smithy-backed `SmithyIntermediateModelBuilder` and a Smithy build plugin from phase 2. Phase 3 builds on them.

The requirements are organized in four parts, in dependency order:

- Part A: schema decoupling and schema-based serde (Requirements 1–17)
- Part B: Document types and Type Registries (Requirements 18–24)
- Part C: Dynamic Client (Requirements 25–31)
- Part D: cross-cutting requirements (Requirements 32–42). These cover protocol tests, the prototype service set, performance, generating HTTP responses for mocking, artifact size, the pipeline rewrite informed by project racecar, and document/type registry uses in higher-level libraries.

### Scope assumptions

- Customizations from `customization.config` do not drive the design. If a customization conflicts with schema codegen for a prototype service, the customization is disabled for the prototype.
- Generated output does not need to be byte-identical to the current SDK. The public API of generated clients and models must stay source-compatible (Requirement 16).
- Server-side frameworks, a standard `ServerProtocol` API, and the Sparrowhawk protocol are out of scope. Server-side response serialization is in scope only to generate HTTP responses for testing and mocking (Requirement 35).
- The prototype covers a limited service set (Requirement 33). It does not generate the full service fleet.

## Glossary

- **SDK**: The AWS SDK for Java v2 in this repository.
- **smithy-java**: The reference implementation at `.kiro/reference/smithy-java` (JDK 21+).
- **Porting_Map**: The design artifact that maps each ported smithy-java type to its SDK module, package, and class name, and records each deviation and the reason for it.
- **Runtime_Schema_Module**: The new SDK runtime module that contains the ported schema, trait, serde, document, and type registry types.
- **Schema**: A runtime, immutable description of a Smithy shape. It holds the ShapeId, ShapeType, serde-relevant traits, and member schemas. It is a port of smithy-java `Schema`.
- **Member_Schema**: A Schema for a structure, union, list, or map member. It presents its target's type, members, and merged traits.
- **Root_Schema**: A Schema for a non-member shape.
- **Deferred_Schema**: A Schema whose members are resolved lazily so that recursive shape graphs can be built.
- **ShapeId_Runtime**: The SDK runtime representation of a Smithy shape ID (`namespace#Name$member`).
- **Runtime_Trait**: The runtime object form of a Smithy trait, keyed by the trait's ShapeId.
- **Generic_Trait**: A Runtime_Trait that holds the raw value of a trait that has no typed runtime class, as a Document.
- **Trait_Key**: A dense integer key, resolved once, that indexes Runtime_Trait storage on a Schema (port of smithy-java `TraitKey`/`TraitMap`).
- **Schema_Extension**: Derived data computed for, and cached on, a Schema by a runtime component (port of smithy-java `SchemaExtensionKey`/`SchemaExtensionProvider`).
- **Schema_Extension_Provider**: A discoverable component that computes one Schema_Extension value for a Schema.
- **Serializable_Shape**: A value that can write itself to a Shape_Serializer (port of `SerializableShape`).
- **Serializable_Struct**: A Serializable_Shape for structures and unions. It exposes `schema()`, `serializeMembers(ShapeSerializer)`, and `getMemberValue(Schema)` (port of `SerializableStruct`).
- **Shape_Builder**: A builder that populates itself from a Shape_Deserializer and builds a typed value (port of `ShapeBuilder`).
- **Shape_Serializer**: The format-agnostic write interface for the Smithy data model (port of `ShapeSerializer`).
- **Shape_Deserializer**: The format-agnostic read interface for the Smithy data model, using member consumers (port of `ShapeDeserializer`).
- **Codec**: A factory for a paired Shape_Serializer and Shape_Deserializer over bytes for one format and its settings (JSON, CBOR, XML).
- **Codec_Settings**: The per-protocol settings for a Codec, such as use of `jsonName`, use of `timestampFormat`, the default timestamp format, and the default namespace.
- **Client_Protocol**: A runtime component that serializes an operation input into a transport request and deserializes a transport response into an output or an error (port of `ClientProtocol`).
- **Supported_Protocols**: `aws.protocols#awsJson1_0`, `aws.protocols#awsJson1_1`, `aws.protocols#restJson1`, `aws.protocols#restXml`, `aws.protocols#awsQuery`, `aws.protocols#ec2Query`, and `smithy.protocols#rpcv2Cbor`.
- **Supported_Protocol**: Any one protocol in Supported_Protocols.
- **Client_Transport**: A runtime component that sends a transport request and returns a transport response. For HTTP, it adapts the existing `SdkHttpClient` and `SdkAsyncHttpClient`.
- **Http_Binding_Serde**: The shared runtime serializers and deserializers for HTTP labels, query, headers, prefix headers, status code, and payload (port of smithy-java `http-binding`).
- **Api_Operation**: The runtime operation abstraction. It exposes the operation Schema, input and output Schemas, error Schemas, the error Type_Registry, effective auth schemes, and input/output Shape_Builder factories (port of `ApiOperation`).
- **Api_Service**: The runtime service abstraction that exposes the service Schema (port of `ApiService`).
- **Generated_Model_Class**: A generated POJO for a structure, union, or modeled exception, including its builder.
- **Codegen**: The SDK code generator in `codegen/` and its Maven and Smithy-build entry points.
- **Intermediate_Model**: The `IntermediateModel` and its model classes (`ShapeModel`, `MemberModel`, `OperationModel`, `Metadata`, and related types) that Codegen consumes.
- **Smithy_Model**: A `software.amazon.smithy.model.Model` loaded at codegen time, or at runtime by the Dynamic_Client.
- **C2J_Converter**: The C2J-to-Smithy converter ported from `codegen-c2j-to-smithy` on the `smithy-java-bridge` fork branch.
- **C2J_Marker_Trait**: A trait in the `com.amazonaws.c2j` namespace that the C2J_Converter emits to carry C2J-only information.
- **Codegen_Pipeline**: The ordered codegen stages: C2J load → C2J customizations → C2J_Converter → Smithy_Model → Intermediate_Model → generated code.
- **Schema_Generator**: The Codegen component that emits Schemas, the schema index, and trait initialization code.
- **Trait_Inclusion_Policy**: The Codegen rules that select which traits are emitted into generated Schemas.
- **Trait_Initializer**: A Codegen extension that renders the Java expression for one trait's runtime value (port of smithy-java `TraitInitializer`).
- **Document**: The enhanced SDK document type that covers the full Smithy data model, including blob and timestamp (port of smithy-java `Document`).
- **Legacy_Document**: The existing `software.amazon.awssdk.core.document.Document`.
- **Protocol_Document**: A Document created by deserialization that keeps the Codec_Settings of the protocol it was read from (for example, a JSON document).
- **Discriminator**: The ShapeId that identifies the concrete shape of a structure Document.
- **Type_Registry**: A runtime map from ShapeId to a way of building that shape's typed value (port of `TypeRegistry`).
- **Primary_Type_Registry**: The public, statically accessible Type_Registry for a generated package.
- **Error_Registry**: The Type_Registry of every shape with the `error` trait in a service, used during error deserialization.
- **Dynamic_Client**: The SDK client that runs against a Smithy_Model loaded at runtime, with Document input and output (port of smithy-java `DynamicClient`).
- **Schema_Converter**: The runtime component that converts Smithy_Model shapes into Schemas (port of smithy-java `SchemaConverter`).
- **Request_Pipeline**: The SDK runtime path that runs one client call: interceptors, serialization, endpoint resolution, auth, signing, retries, transport, and deserialization.
- **Protocol_Test_Generator**: The Codegen component that generates JUnit 5 tests from Smithy `httpRequestTests` and `httpResponseTests` traits.
- **Protocol_Test_Services**: The Smithy protocol test services under `.kiro/reference/smithy/smithy-aws-protocol-tests/model` and `.kiro/reference/smithy/smithy-protocol-tests/model`.
- **Legacy_Protocol_Tests**: The existing C2J-based suites in `test/protocol-tests` and `test/protocol-tests-core`.
- **Prototype_Service_Set**: The limited set of AWS services generated with schema-based serde for the prototype (Requirement 33).
- **Response_Generator**: The runtime component that serializes an operation output or a modeled exception into the transport response a service would send for a given protocol.
- **Mock_Plugin**: The test utility that returns queued output shapes, documents, or raw responses to a client through the Response_Generator (port of smithy-java `MockPlugin`/`MockQueue`).
- **Benchmark_Harness**: The JMH component benchmarks in `test/sdk-standard-benchmarks`, plus the end-to-end harness and methodology from branch `feature/poc/racecar` (`test/standalone-e2e-benchmarks`).
- **Baseline_Build**: The SDK built from the merge base of this feature branch, with the existing generated marshallers and unmarshallers.
- **Size_Report**: The artifact-size comparison between Baseline_Build and the feature build for each Prototype_Service_Set service.
- **Library_Integration_Analysis**: The design deliverable that identifies where Documents and Type_Registries can improve higher-level SDK libraries and utilities.

---

## Requirements

**Part A: Schema Decoupling and Schema-Based Serde**

### Requirement 1: Porting fidelity and Java 8 compatibility

**User Story:** As an SDK maintainer, I want the smithy-java runtime designs ported as directly as possible onto Java 8, so that the SDK gets proven designs while keeping its language-level contract.

#### Acceptance Criteria

1. THE Porting_Map SHALL name each smithy-java source module in porting scope. For every public top-level type in those modules that the SDK ports, it SHALL give exactly one entry with the source fully qualified name, the target SDK module, the target package and the target class name. Every target package SHALL start with `software.amazon.awssdk.`.
2. THE Porting_Map SHALL record a deviation entry for each ported type whose SDK form differs from smithy-java beyond the package rename. A difference means any of: a changed type name, a changed kind of type (for example a record becoming a final class, or a sealed interface becoming a plain interface), an added, removed or renamed public method, a changed public method signature, or changed observable behavior. Each entry SHALL state the Java 8 language or library feature that is unavailable, or the SDK compatibility rule, that requires the deviation.
3. WHEN the SDK Maven build compiles the Runtime_Schema_Module, every codec module, every Client_Protocol module, the Dynamic_Client module and the Mock_Plugin module, THE SDK Maven build SHALL compile each one with `--release 8` (or `source` and `target` set to 8). Main and test sources SHALL compile with zero errors.
4. THE ported code SHALL contain no records, sealed or permitted types, pattern matching for `instanceof` or `switch`, switch expressions, text blocks or `var`. It SHALL not reference `java.util.concurrent.Flow`, `java.net.http`, `java.lang.invoke.VarHandle`, or any type or method first added in JDK 9 or later (for example `List.of`, `Optional.isEmpty`, `String.isBlank`). Each one SHALL be replaced by a Java 8 equivalent.
5. WHERE smithy-java uses `java.util.concurrent.Flow.Publisher`, `Flow.Subscriber`, `Flow.Subscription` or `Flow.Processor`, THE ported code SHALL use `org.reactivestreams.Publisher`, `Subscriber`, `Subscription` or `Processor` respectively.
6. THE resolved Maven compile-scope and runtime-scope dependency tree of each generated Prototype_Service_Set client SHALL contain no `software.amazon.smithy:smithy-model` artifact, whether direct or transitive.
7. THE Dynamic_Client module SHALL be the only SDK runtime module, meaning any published module other than the codegen modules, the codegen Maven plugin and test modules, that declares a compile-scope or runtime-scope dependency on `software.amazon.smithy:smithy-model`.
8. THE ported public types SHALL each carry exactly one SDK visibility annotation, using this mapping: smithy-java `public` without a stability annotation maps to `@SdkPublicApi`, `@SmithyUnstableApi` maps to `@SdkProtectedApi`, and `@SmithyInternalApi` maps to `@SdkInternalApi`. Any type that departs from this mapping SHALL have a Porting_Map deviation entry under criterion 2.
9. WHEN the unit tests of the modules listed in criterion 3 run on a Java 8 runtime, THE tests SHALL complete with zero failures caused by `UnsupportedClassVersionError`, `NoSuchMethodError` or `NoClassDefFoundError`.

### Requirement 2: Runtime ShapeId, ShapeType, and Runtime_Traits

**User Story:** As an SDK runtime developer, I want SDK-native shape identifiers, shape types, and trait objects, so that schemas can be inspected at runtime without the Smithy model library.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide an immutable ShapeId_Runtime type that parses an absolute shape ID string of the form `namespace#Name` or `namespace#Name$member` (where `namespace` is one or more dot-separated Smithy identifiers) and exposes the namespace, the shape name, and the member name, which is absent for non-member shape IDs.
2. FOR ALL valid absolute shape ID strings, parsing into a ShapeId_Runtime and then formatting SHALL produce the original string (round-trip property), and two ShapeId_Runtime values parsed from equal strings SHALL be equal and have equal hash codes.
3. THE Runtime_Schema_Module SHALL provide a ShapeType enumeration with exactly one constant for each Smithy IDL 2.0 simple type (`blob`, `boolean`, `string`, `enum`, `timestamp`, `byte`, `short`, `integer`, `intEnum`, `long`, `float`, `double`, `bigInteger`, `bigDecimal`, `document`), each aggregate type (`list`, `map`, `structure`, `union`), and `member`, `operation`, `resource`, and `service`.
4. THE Runtime_Schema_Module SHALL provide a typed Runtime_Trait class for every trait that the Trait_Inclusion_Policy includes by default (Requirement 9), and each Runtime_Trait SHALL expose the ShapeId_Runtime of the trait it represents.
5. THE Runtime_Schema_Module SHALL provide a Generic_Trait that holds a trait ShapeId_Runtime and the trait value as a Document, and returns a Document equal to the one it was constructed with.
6. THE Runtime_Schema_Module SHALL provide a trait factory registry, discoverable at runtime, that maps a trait ShapeId_Runtime to the constructor of its typed Runtime_Trait and builds a Runtime_Trait from a trait ShapeId_Runtime and a Document value.
7. WHEN a third party registers a trait factory through the discovery mechanism for a trait ShapeId_Runtime that has no SDK-provided factory, THE Runtime_Schema_Module SHALL produce instances of the third party's Runtime_Trait class for that trait ShapeId_Runtime.
8. IF a string passed to ShapeId_Runtime parsing is not a valid absolute shape ID (empty, missing `#`, missing the namespace or shape name, more than one `$`, an empty member name, or characters outside the Smithy identifier grammar), THEN THE Runtime_Schema_Module SHALL reject the input with an error that includes the offending string, and SHALL NOT return a ShapeId_Runtime.
9. IF the trait factory registry is asked to build a trait whose ShapeId_Runtime has no registered factory, THEN THE Runtime_Schema_Module SHALL return a Generic_Trait that holds that ShapeId_Runtime and the given Document value, without raising an error.
10. IF more than one trait factory is discovered for the same trait ShapeId_Runtime, or a factory is given a Document value that does not match the trait's modeled shape, THEN THE Runtime_Schema_Module SHALL raise an error identifying the trait ShapeId_Runtime.

### Requirement 3: Schema type hierarchy

**User Story:** As an SDK runtime developer, I want a Schema hierarchy equivalent to smithy-java's, so that serializers, protocols, and pipeline components can walk a shape graph at runtime.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide `Schema`, Root_Schema, Member_Schema, and Deferred_Schema types, a `SchemaBuilder`, and a `PreludeSchemas` holder, each compiling at Java 8 language level and each having a Porting_Map entry that names the smithy-java type it ports and lists every public smithy-java method of that type that is omitted, renamed, or given a different signature, with the reason for each deviation.
2. THE Schema SHALL expose its ShapeId_Runtime, its ShapeType, a boolean indicating whether the Schema is a Member_Schema, its member name, and its member index; for a Root_Schema the member indicator SHALL be false, the member name SHALL be null, and the member index SHALL be -1.
3. THE Schema SHALL expose its members as an unmodifiable list in Smithy_Model declaration order, member lookup by name, the list member, the map key member, the map value member, the string enum values as an unmodifiable set, and the int enum values as an unmodifiable set; IF a caller attempts to modify any returned list or set, THEN THE Schema SHALL throw an exception and leave its members and enum values unchanged.
4. THE Schema SHALL hold no reference to a Generated_Model_Class type parameter, and the Schema type and its subtypes SHALL declare no generic type parameters.
5. WHERE a Root_Schema corresponds to a Generated_Model_Class, THE Root_Schema SHALL expose a Shape_Builder supplier whose every invocation returns a new, independent Shape_Builder for that class, and SHALL expose that Generated_Model_Class, matching smithy-java `Schema.shapeBuilder()` and `Schema.shapeClass()`; for a Root_Schema that does not correspond to a Generated_Model_Class, both SHALL return null.
6. WHEN a Member_Schema is queried for its ShapeType, members, member by name, list member, map key member, map value member, or enum values, THE Member_Schema SHALL return the same result as the same query on its target shape's Schema, and SHALL never report a ShapeType of `MEMBER`.
7. WHEN a Member_Schema is queried for a trait, THE Member_Schema SHALL return the member's trait if the member declares it (including when the target also declares it), the target's trait if only the target declares it, and null if neither declares it.
8. THE Member_Schema SHALL provide a direct-trait lookup that returns a trait only when that trait is declared on the member itself and returns null for a trait declared only on the target; for a Root_Schema the direct-trait lookup SHALL return the same result as the trait lookup.
9. WHEN a SchemaBuilder builds a structure or union whose member graph refers back to itself directly or through intermediate shapes, THE SchemaBuilder SHALL produce, through Deferred_Schema resolution, a Schema graph in which following member targets from the recursive shape reaches a Schema with the same ShapeId_Runtime and the same member names as the original, and each such member access returns without unbounded recursion.
10. THE Schema_Generator SHALL generate Schemas such that, for every operation, a recursive member walk with a visited set keyed by ShapeId_Runtime, starting from the operation's input, output, and error Schemas, visits every shape in the operation closure (all shapes transitively targeted by members of those Schemas) exactly once and terminates.
11. THE member index of each structure or union member SHALL be an integer from 0 to N-1, where N is the container's member count, unique within that container, identical on every access within one generated build, and accepted by the container's index-based member lookup to return that member; THE member index accessor and index-based member lookup SHALL be annotated as SDK internal API, not for use by callers outside generated code and SDK runtime modules.
12. IF a SchemaBuilder is given a member name already present on the shape, a member for a ShapeType that does not take members, a list without exactly one member, or a map without exactly one key member and one value member, THEN THE SchemaBuilder SHALL throw an exception identifying the shape's ShapeId_Runtime and the invalid member, and SHALL NOT produce a Schema.
13. IF a Deferred_Schema member's target is accessed before the referenced builder has been built, THEN THE Deferred_Schema SHALL throw an exception identifying the unresolved target's ShapeId_Runtime.

### Requirement 4: Trait lookup performance

**User Story:** As a performance-focused maintainer, I want trait lookups that avoid hashing and allocation, so that schema-driven serde is competitive with generated marshallers.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide a Trait_Key type, obtained through a static factory that takes a Runtime_Trait class, whose instances each hold an integer index that is unique among all Trait_Keys, with indices assigned contiguously from 0 in Trait_Key creation order.
2. WHEN the Trait_Key factory is called more than once for the same Runtime_Trait class, including concurrently from multiple threads, THE Runtime_Schema_Module SHALL return the same Trait_Key instance and index for every call.
3. WHEN `Schema.getTrait(TraitKey)` is called for a trait present on the Schema, THE Schema SHALL return that Runtime_Trait instance through an array index lookup on the Trait_Key index, allocating 0 bytes per call as measured by the Benchmark_Harness allocation profiler, with no hashing and with lookup time independent of the number of traits on the Schema.
4. IF `Schema.getTrait(TraitKey)` is called with a Trait_Key whose trait is absent from the Schema, including a Trait_Key whose index exceeds the Schema's trait storage size, THEN THE Schema SHALL return null without throwing an exception and without allocating.
5. THE Runtime_Schema_Module SHALL provide a predefined Trait_Key constant, as a public static final field, for every Runtime_Trait class that any SDK runtime module reads.
6. WHEN the trait collection view of a Schema is requested, THE Schema SHALL return an unmodifiable collection containing every Runtime_Trait on the Schema exactly once, including Generic_Traits, and SHALL reject any modification attempt by throwing an exception that leaves the Schema's traits unchanged.
7. THE Schema SHALL expose traits only through Trait_Key lookups and the trait collection view. Schema SHALL NOT expose a per-trait getter method.

### Requirement 5: Schema extensions

**User Story:** As a protocol implementer, I want to attach and cache derived data on any Schema, so that per-request hot paths read precomputed metadata instead of re-interpreting traits.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide a `SchemaExtensionKey<T>` type that identifies one Schema_Extension whose value type is `T`, and a `SchemaExtensionProvider<T>` type that returns its `SchemaExtensionKey<T>` and computes a value of type `T` from a given Schema, matching the smithy-java types of the same names; THE Porting_Map SHALL record every deviation from the smithy-java signatures and the reason for each deviation.
2. THE Runtime_Schema_Module SHALL discover Schema_Extension_Providers through `java.util.ServiceLoader` at most once per class loader, and SHALL complete discovery before the first `Schema.getExtension(key)` call returns.
3. WHEN `Schema.getExtension(key)` is first called for a Schema instance and key, THE Schema SHALL invoke the Schema_Extension_Provider registered for that key exactly once with that Schema instance as the argument, cache the returned value on that Schema instance, and return the value.
4. WHEN `Schema.getExtension(key)` is called again for the same Schema instance and key, THE Schema SHALL return the same instance (reference equality) as the cached value without invoking the Schema_Extension_Provider.
5. WHILE 2 to 64 threads call `Schema.getExtension(key)` concurrently for the same Schema instance and key, THE Schema SHALL return to every caller a value equal (per `equals`) to the cached value, and after any call has returned, THE Schema SHALL return that same instance to all later calls.
6. THE Schema_Extension mechanism SHALL apply the same key lookup, provider invocation, and caching behavior defined in criteria 3, 4, 5, and 8 to generated Root_Schemas, Member_Schemas, and Deferred_Schemas, and to Schemas produced by the Schema_Converter, with no API that differs based on a Schema's origin.
7. THE JSON, CBOR, XML, Query, and Http_Binding_Serde implementations SHALL obtain pre-encoded wire names, member lookup tables, resolved timestamp formats, and binding locations through Schema_Extensions, such that serializing or deserializing values of the same Schema 1,000 times invokes each Schema_Extension_Provider at most once per Schema instance.
8. IF a Schema_Extension_Provider returns null for a Schema, THEN THE Schema SHALL cache the null result, return null for that key, and not invoke the Schema_Extension_Provider again for that Schema instance and key.
9. IF `Schema.getExtension(key)` is called with a key for which no Schema_Extension_Provider is registered, THEN THE Schema SHALL return null without throwing an exception.
10. IF a Schema_Extension_Provider throws an exception while computing a value, THEN THE Schema SHALL propagate the exception to the caller unchanged, not cache any value for that Schema instance and key, and invoke the Schema_Extension_Provider again on the next `Schema.getExtension(key)` call for that Schema instance and key.
11. IF ServiceLoader discovery finds more than one Schema_Extension_Provider for the same `SchemaExtensionKey`, THEN THE Runtime_Schema_Module SHALL fail discovery with an exception that identifies the duplicated key and the conflicting provider classes.

### Requirement 6: Serializable shapes and builders in generated code

**User Story:** As an SDK user, I want generated model classes to plug into any serializer or deserializer, so that I can serialize SDK types to any supported format without regenerating code.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide Serializable_Shape, Serializable_Struct, and Shape_Builder interfaces whose methods match the smithy-java interfaces of the same names in method name, parameter types, and return types, including at minimum Serializable_Shape `serialize(ShapeSerializer)`, Serializable_Struct `schema()`, `serializeMembers(ShapeSerializer)`, and `getMemberValue(Schema)`, and Shape_Builder `build()` and `deserialize(ShapeDeserializer)`, with every Java 8 deviation recorded in the Porting_Map.
2. THE Codegen SHALL generate every structure, union, and modeled exception Generated_Model_Class so that the class implements Serializable_Struct, and so that calling `serialize(ShapeSerializer)` on an instance calls `writeStruct` on that Shape_Serializer exactly once, passing the class's `SCHEMA` and the instance itself.
3. THE Codegen SHALL generate on every Generated_Model_Class a `public static final` field named `SCHEMA`, of type Schema, whose ShapeId_Runtime equals the ShapeId of the corresponding Smithy_Model shape and which is the same instance returned by the class's `schema()` method.
4. THE Codegen SHALL generate every Generated_Model_Class builder so that the builder implements Shape_Builder for its Generated_Model_Class, including a `deserialize(ShapeDeserializer)` method that returns the same builder instance and leaves any value already set on the builder unchanged for each member absent from the deserialized data.
5. WHEN the generated `serializeMembers` method is called, THE method SHALL write each member whose value is set to the Shape_Serializer exactly once, using that member's Member_Schema, in ascending member index order, and SHALL write nothing for any member whose value is not set. A list or map member the caller never set SHALL count as not set. A list or map member explicitly set to an empty collection SHALL be written as an empty collection. A string-enum member SHALL be written as its wire string, including values not defined in the enum.
6. WHEN the generated `getMemberValue(Schema)` method is called with a Member_Schema of the Generated_Model_Class's Schema, THE method SHALL return the same value that the member's public getter returns. For a string-enum member, it SHALL return the wire string value.
7. THE generated builder `deserialize` method SHALL select the member to populate by the Member_Schema member index, SHALL NOT compare member names, and SHALL pass the Shape_Deserializer a consumer that captures no local or instance state (a non-capturing lambda or a static consumer instance), so that repeated `deserialize` calls pass the identical consumer instance.
8. FOR ALL Generated_Model_Class instances in the Prototype_Service_Set, serializing with each Codec provided by the Runtime_Schema_Module (JSON, CBOR, XML) and then deserializing the output with the same Codec and Codec_Settings SHALL produce a value that `equals` the original (round-trip property). This SHALL be verified with at least 100 generated instances per Generated_Model_Class. Together, those instances SHALL cover every member both set and unset, empty and non-empty collections of up to 10 elements, nested structures to a depth of 3, every union variant, the minimum and maximum value of each numeric member type, and timestamps at millisecond precision.
9. THE Codegen SHALL generate every string enum class so that it implements the runtime equivalent of smithy-java `SmithyEnum`, and every int enum so that it implements the runtime equivalent of smithy-java `SmithyIntEnum`. Each SHALL expose the enum's Schema and the modeled wire value of each defined constant.
10. IF the generated `getMemberValue(Schema)` method is called with a Schema that is not a member of the Generated_Model_Class's Schema, THEN THE method SHALL throw an exception indicating that the Schema is not a member of the shape, and SHALL leave the instance unchanged.
11. IF the data read by a generated builder `deserialize` call contains a member that the Generated_Model_Class's Schema does not define, THEN THE builder SHALL skip that member, populate every defined member present in the data, and complete without throwing an exception. For a union Generated_Model_Class, `build()` SHALL then return a value whose type is the unknown-to-SDK-version variant.
12. IF the Shape_Deserializer throws an exception during a generated builder `deserialize` call, THEN THE builder SHALL propagate that exception to the caller without returning normally.

### Requirement 7: Shape_Serializer and Shape_Deserializer

**User Story:** As a codec implementer, I want format-agnostic serializer and deserializer interfaces, so that one generated shape implementation works with every format.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide a Java 8 compatible Shape_Serializer interface with one write method for each of struct (taking a Serializable_Struct), list, map, boolean, byte, short, integer, long, float, double, bigInteger, bigDecimal, string, blob, timestamp, document, null, data stream, and event stream, where each write method takes the Schema of the value being written, the list and map write methods also take the number of elements or entries to be written, and the event stream write method accepts a publisher type available on Java 8.
2. THE Runtime_Schema_Module SHALL provide a Java 8 compatible Shape_Deserializer interface with a read method for every type listed in criterion 1 (struct, list, and map through consumer-based `readStruct`, `readList`, and `readMap`), plus `isNull`, which returns true only when the next value is null, `readNull`, which consumes the null value and returns null, and a container size method, which returns the element or entry count of the current list or map when the count is known and -1 when it is not known.
3. THE Runtime_Schema_Module SHALL provide ports of `SpecificShapeSerializer`, `SpecificShapeDeserializer`, `InterceptingSerializer`, `ListSerializer`, `MapSerializer`, `NullSerializer`, `ToStringSerializer`, `TimestampFormatter`, and `SerializationException`, each with the same public method names, parameter types, and behavior as the smithy-java type, except for deviations recorded in the Porting_Map.
4. THE Shape_Serializer and Shape_Deserializer SHALL implement `AutoCloseable`, and WHEN `flush` is called on a Shape_Serializer, THE Shape_Serializer SHALL make every value written before the call available in its output before `flush` returns.
5. WHEN a Generated_Model_Class is passed to `ToStringSerializer`, THE ToStringSerializer SHALL produce a string that contains the shape name and the Smithy member name of every non-null member, and SHALL replace the value of every member whose Member_Schema has the `sensitive` trait (whether applied to the member or to its target shape) with a fixed redaction placeholder that contains no part of the original value, at every nesting depth, including list elements and map values.
6. THE Runtime_Schema_Module SHALL provide a port of smithy-java `ShapeTranscoder` that converts one Serializable_Shape into another Shape_Builder by Smithy member name, with no intermediate wire format or Document tree, where each source member is set on the target member that has the same Smithy member name, nested structures, lists, and maps are converted recursively, and source members with no same-named target member are skipped without error.
7. IF a Shape_Deserializer (including the `ShapeTranscoder` port acting as one) reads input that is malformed or whose value type does not match the type of the read method called, THEN THE Shape_Deserializer SHALL throw a `SerializationException` whose message identifies the ShapeId of the Schema being read, and SHALL NOT return a partially populated value.
8. WHEN `readStruct` encounters a member name that is not defined in the structure Schema, THE Shape_Deserializer SHALL pass that member name to the consumer's unknown-member callback, skip the member's value, and continue reading the remaining members.

### Requirement 8: Codecs

**User Story:** As an SDK user, I want JSON, CBOR, and XML codecs with per-protocol settings, so that I can serialize and deserialize SDK types outside an operation call.

#### Acceptance Criteria

1. THE SDK SHALL provide a JSON Codec, a CBOR Codec, and an XML Codec. Each one SHALL create a new Shape_Serializer that writes bytes and a new Shape_Deserializer that reads bytes. A Codec SHALL produce only the encoded payload bytes, with no HTTP message, header, query string, or other protocol framing.
2. THE SDK SHALL provide an AWS Query Shape_Serializer that writes the form-encoded request body for the `awsQuery` and `ec2Query` protocols. THE SDK SHALL NOT provide an AWS Query Shape_Deserializer, because responses for those protocols are read with the XML Codec. IF an AWS Query Codec is provided, THEN its deserializer factory method SHALL return `null`.
3. THE JSON, CBOR, and XML Codecs SHALL accept Codec_Settings. The settings SHALL cover: whether the `jsonName` or `xmlName` trait is honored, whether the `timestampFormat` trait is honored, the default timestamp format (`epoch-seconds`, `date-time`, or `http-date`), the default XML namespace, and whether an unknown union member is kept as an unknown variant or rejected. The SDK SHALL provide the following preset Codec_Settings, one per protocol:
   - restJson1: honors `jsonName`; honors `timestampFormat` with a default of `epoch-seconds`.
   - awsJson1_0 and awsJson1_1: ignore `jsonName`; honor `timestampFormat` with a default of `epoch-seconds`.
   - restXml and the XML response side of awsQuery/ec2Query: honor `xmlName`; honor `timestampFormat` with a default of `date-time`.
   - rpcv2Cbor: ignores `timestampFormat`; always uses `epoch-seconds`.
4. THE JSON Codec SHALL have no compile-time or runtime dependency on Jackson databind, so the `jackson-databind` artifact SHALL NOT appear in the JSON Codec module's Maven dependency tree. THE JSON Codec SHALL use either a hand-written byte-level reader and writer ported from smithy-java `SmithyJsonSerializer`/`SmithyJsonDeserializer`, or the SDK's shaded `third-party-jackson-core`.
5. THE CBOR Codec SHALL encode and decode every Smithy data model type as defined by the Smithy RPCv2 CBOR specification. That covers boolean, byte, short, integer, long, float, double, bigInteger, bigDecimal, string, blob, timestamp, document, list, map, structure, union, and null. Timestamps SHALL be encoded as CBOR tag 1 epoch-seconds values.
6. FOR ALL Generated_Model_Class instances, the following round trip SHALL return a value that is `equals()` to the original: serialize with the JSON, CBOR, or XML Codec, then deserialize the output with the same Codec and the same Codec_Settings. The instances include ones with unset optional members, empty lists and maps, nested and recursive structures, unions, blobs, timestamps, and boundary numeric values.
7. IF a Codec deserializes input that is malformed for its format or does not conform to the target Schema, THEN THE Codec SHALL throw a `SerializationException` and return no partially built value. Non-conforming input includes a value whose type does not match the member's ShapeType and a union with more than one member set. The exception message SHALL name the target Schema's ShapeId and the member path where the failure was detected.
8. THE Codec interface, its factory methods, and Codec_Settings SHALL be annotated as public SDK API. Every public method SHALL have Javadoc, so that third parties can add codecs for other formats.
9. WHEN a Codec deserializes a structure that contains a member name that is not in the target Schema, THE Codec SHALL skip that member's value without throwing. It SHALL still populate every member that is in the Schema.
10. THE JSON, CBOR, and XML Codec instances SHALL be immutable after construction. WHEN two or more threads use one Codec instance at the same time to serialize or deserialize different values, each thread SHALL get the same bytes or value it would get using the Codec alone.

### Requirement 9: Trait inclusion policy

**User Story:** As a Codegen maintainer, I want a controllable trait inclusion policy, so that generated Schemas carry only serde-relevant traits, keeping artifacts small.

#### Acceptance Criteria

1. WHEN the Schema_Generator generates Schemas for a service, THE Trait_Inclusion_Policy SHALL include, by default, (a) every trait whose ShapeId is listed in the `traits` property of each protocol definition trait or auth definition trait applied to the service shape, and (b) each trait applied to the service shape that is itself marked with the `protocolDefinition` or `authDefinition` meta-trait.
2. THE Trait_Inclusion_Policy SHALL include, by default, the `sensitive`, `required`, `default`, `sparse`, `idempotencyToken`, `error`, `httpError`, `retryable`, `streaming`, `eventHeader`, `eventPayload`, `mediaType`, `hostLabel`, `endpoint`, `http`, `httpChecksum`, `httpChecksumRequired`, `requestCompression`, `paginated`, `awsQueryError`, `awsQueryCompatible`, `ec2QueryName`, `xmlName`, `xmlNamespace`, `xmlAttribute`, `xmlFlattened`, `jsonName`, `timestampFormat`, and `httpResponseCode` traits, whether or not the service's protocol definition lists them.
3. THE Trait_Inclusion_Policy SHALL include each of the constraint traits `length`, `range`, `pattern`, `uniqueItems`, and `enum` if and only if the SDK runtime validates that constraint on client input or output, and SHALL exclude each of these constraint traits that the SDK runtime does not validate.
4. THE Trait_Inclusion_Policy SHALL exclude every C2J_Marker_Trait and the `documentation`, `examples`, `externalDocumentation`, `since`, and `tags` traits from every generated Schema, regardless of criteria 1, 2, and 5.
5. THE Codegen SHALL expose an API, and a Codegen configuration setting, that (a) adds traits to the Trait_Inclusion_Policy by trait ShapeId, (b) removes traits that the same API or configuration added, and (c) returns the effective set of included trait ShapeIds for a service after defaults, additions, and removals are applied.
6. IF a configuration or API call attempts to remove a trait that the SDK Codegen added to the Trait_Inclusion_Policy under criterion 1, 2, or 3, THEN THE Codegen SHALL fail generation without emitting generated code and report an error naming the trait.
7. WHEN an included trait has no registered Trait_Initializer, THE Schema_Generator SHALL emit the trait as a Generic_Trait keyed by the trait's ShapeId and carrying a Document equal to the trait's model value.
8. THE Codegen SHALL provide a Trait_Initializer extension point, equivalent to smithy-java `TraitInitializer`, through which a Codegen plugin registers, per trait ShapeId, the rendering of that trait's runtime value. If exactly one Trait_Initializer is registered for an included trait, the Schema_Generator SHALL use it instead of the Generic_Trait form. If more than one is registered for the same trait ShapeId, the Codegen SHALL fail generation and report an error naming the trait.
9. WHEN the Schema_Generator emits a Root_Schema or Member_Schema, THE Schema_Generator SHALL attach exactly the traits that the shape or member carries (including traits merged from a member's target) and that are in the effective inclusion set from criterion 5(c), and SHALL attach no other traits.
10. IF a configuration or API call attempts to add a trait that criterion 4 excludes, THEN THE Codegen SHALL fail generation without emitting generated code and report an error naming the trait.
11. IF a configuration or API call supplies a trait identifier that is not a syntactically valid Smithy ShapeId, THEN THE Codegen SHALL fail generation without emitting generated code and report an error naming the invalid identifier.

### Requirement 10: Schema generation

**User Story:** As a Codegen maintainer, I want schemas generated from Smithy shapes, so that every exported type has a matching runtime Schema.

#### Acceptance Criteria

1. THE Schema_Generator SHALL generate exactly one Root_Schema for every non-prelude shape in the service closure. The service closure is the input, output, and error shapes of every operation bound to the service, the service-level error shapes, and every shape reachable from those shapes through structure, union, list, and map members. Members that target Smithy prelude shapes SHALL reference the `PreludeSchemas` instances instead of generated duplicates.
2. THE Schema_Generator SHALL generate one Schema for each operation bound to the service and one Schema for the service. Each generated Api_Operation SHALL return its operation's input Schema, output Schema, and error Schemas, and the generated Api_Service SHALL return the service Schema.
3. THE Schema_Generator SHALL generate one Schema index per service, equivalent to smithy-java `SchemaIndex`. The index SHALL return the generated Schema for every ShapeId_Runtime in the service closure, and the number of indexed Schemas SHALL equal the number of Root_Schemas generated under criterion 1.
4. THE Schema_Generator SHALL emit each member's directly declared traits apart from its target shape's traits. A Member_Schema's direct-trait lookup (Requirement 3, criterion 8) SHALL return only the member-declared included traits, and trait merging (Requirement 3, criterion 7) SHALL happen at runtime.
5. WHEN the service closure contains a recursive shape, THE Schema_Generator SHALL emit Deferred_Schema construction only for member edges that close a cycle back to a shape already on the construction path. All other member edges SHALL use non-deferred Schema construction, and a recursive member walk over the generated Schemas SHALL terminate (Requirement 3, criterion 10).
6. WHEN the Smithy_Model was transformed to synthesize or rename operation input or output shapes, THE Schema_Generator SHALL set the generated Schema's ShapeId_Runtime, and every wire-facing name derived from it, to the ShapeId the shape had in the Smithy_Model before the transform.
7. THE Schema_Generator SHALL order and partition static Schema initializers so that every generated method's bytecode stays within the JVM limit of 65,535 bytes for every Prototype_Service_Set service. WHEN any single generated Schema-holding class is the first one initialized, every Schema reachable from it SHALL be non-null, with no deadlock and no class-initialization error.
8. WHEN only the default protocol trait of a generated service is changed from one Supported_Protocol to another, THE regenerated client source SHALL differ only in the default protocol configuration entry and in the Schema trait entries that the Trait_Inclusion_Policy adds or removes because of the protocol definition's `traits` property (Requirement 9, criterion 1).
9. WHEN the Schema_Generator runs twice on the same Smithy_Model with the same Trait_Inclusion_Policy, THE generated Schema source SHALL be identical, including member order and member indexes.
10. IF the Schema index is asked for a ShapeId_Runtime that is not in the service closure, THEN THE Schema index SHALL return an empty result without throwing an exception.

### Requirement 11: Client protocols

**User Story:** As an SDK user, I want each wire protocol implemented once as a schema-driven runtime component, so that generated and dynamic clients share protocol behavior and protocols can be swapped at runtime.

#### Acceptance Criteria

1. THE SDK SHALL provide exactly one Client_Protocol implementation for each of the 7 Supported_Protocols, and generated clients and the Dynamic_Client SHALL use the same Client_Protocol implementation for a given protocol ShapeId.
2. THE Client_Protocol interface SHALL be transport-agnostic and generic over request and response message types: its method signatures SHALL NOT reference any HTTP-specific type, and HTTP-specific behavior SHALL be confined to implementations whose request and response message types are HTTP messages.
3. THE Client_Protocol SHALL expose its protocol ShapeId (equal to the ShapeId of the protocol definition trait it implements), its payload Codec, and its request and response message types, and the same values SHALL be returned on every call for the lifetime of the instance.
4. THE `awsQuery` and `ec2Query` Client_Protocols SHALL return null for the payload Codec, whether or not they use a Codec internally. The `awsJson1_0`, `awsJson1_1`, `restJson1`, `restXml`, and `rpcv2Cbor` Client_Protocols SHALL return a non-null payload Codec configured with that protocol's Codec_Settings.
5. WHEN `serializeRequest` is called with an Api_Operation, an input, a call context, and an endpoint, THE Client_Protocol SHALL produce a transport request that passes every `httpRequestTests` case defined for that protocol in the Protocol_Test_Services (Requirement 32), except cases listed with a recorded reason in the protocol test skip list.
6. WHEN `deserializeResponse` is called with a transport response whose status code the protocol specification classifies as success, THE Client_Protocol SHALL produce an operation output, built through the Api_Operation's output Shape_Builder, that passes every `httpResponseTests` case defined for that protocol's operation outputs in the Protocol_Test_Services, except cases listed with a recorded reason in the protocol test skip list.
7. WHEN `deserializeResponse` is called with a transport response whose status code the protocol specification classifies as an error, THE Client_Protocol SHALL resolve the error Discriminator from the response location the protocol specification defines, look up the Discriminator in the Error_Registry (Requirement 23), and produce the modeled exception populated from the response. The exception SHALL carry the response status code.
8. WHEN `updateServiceEndpoint` is called with a serialized request and an endpoint, THE Client_Protocol SHALL return a new request whose scheme, host, and port are taken from the new endpoint, whose path is the new endpoint's path followed by the original request path, and whose query, headers, and body are unchanged. The original request SHALL NOT be modified.
9. THE `restJson1` and `restXml` Client_Protocols SHALL use Http_Binding_Serde for the `httpLabel`, `httpQuery`, `httpQueryParams`, `httpHeader`, `httpPrefixHeaders`, and `httpResponseCode` bindings, and the payload Codec for members bound to the body or `httpPayload`.
10. THE `awsJson1_0`, `awsJson1_1`, and `rpcv2Cbor` Client_Protocols SHALL ignore HTTP binding traits and `jsonName`, so that a member carrying any of those traits is serialized and deserialized identically to the same member without them, as each protocol specification defines.
11. THE Client_Protocol interface SHALL be publicly documented so that a third party can implement a custom protocol and use it with any generated client or the Dynamic_Client without modifying the Codegen or regenerating the client.
12. THE SDK SHALL discover Client_Protocol factories through `java.util.ServiceLoader`, keyed by protocol ShapeId, and WHEN a factory is requested for a ShapeId that has a registered factory, THE SDK SHALL return a factory that creates a Client_Protocol whose protocol ShapeId equals the requested ShapeId.
13. IF `deserializeResponse` resolves an error Discriminator that is absent from the Error_Registry, or no Discriminator can be resolved, THEN THE Client_Protocol SHALL produce a generic service exception carrying the response status code, the raw error code (when present), and the error message (when present), instead of failing with a deserialization error.
14. IF `serializeRequest` is called with an input that cannot be serialized under the protocol (for example, a null or empty value for a required `httpLabel` member), or `deserializeResponse` receives a body that cannot be parsed by the protocol's format, THEN THE Client_Protocol SHALL throw an SDK client exception naming the operation and the cause, and SHALL NOT return a partial request or output.
15. IF a Client_Protocol factory is requested for a protocol ShapeId that has no factory registered through `java.util.ServiceLoader`, THEN THE SDK SHALL throw an SDK client exception naming the requested ShapeId.

### Requirement 12: Runtime protocol selection

**User Story:** As an SDK user, I want to choose a protocol at runtime, so that I can pin to an earlier protocol when a service migrates protocols.

#### Acceptance Criteria

1. THE generated client builders SHALL expose an optional setting that accepts one Client_Protocol instance as an override. Setting it SHALL NOT require regenerating or recompiling generated code. Setting it to null SHALL count as "no override configured".
2. WHEN a Client_Protocol override is configured, THE generated client SHALL use that Client_Protocol to serialize every operation input and deserialize every operation output and error for that client instance. This applies even when the service's Smithy_Model does not carry that protocol's trait.
3. WHEN no Client_Protocol override is configured, THE generated client SHALL use the Client_Protocol for the first protocol the service declares, checked in this priority order: `smithy.protocols#rpcv2Cbor`, `aws.protocols#awsJson1_0`, `aws.protocols#awsJson1_1`, `aws.protocols#restJson1`, `aws.protocols#restXml`, `aws.protocols#awsQuery`, `aws.protocols#ec2Query`.
4. IF a service's Smithy_Model has none of the Supported_Protocols traits, THEN Codegen SHALL fail generation for that service with an error that names the service ShapeId and the protocol traits found on the service. No client SHALL be generated for that service.
5. IF a configured Client_Protocol has request or response message types that are incompatible with the configured Client_Transport, THEN the client builder's build call SHALL throw an exception that names the Client_Protocol's request and response message types and the Client_Transport's request and response message types. In that case the build call SHALL NOT return a client.
6. WHEN a CloudWatch client from the Prototype_Service_Set is configured in turn with the `rpcv2Cbor`, `awsJson1_0`, and `awsQuery` Client_Protocols and calls the same operation with the same input, THE client SHALL return an output without throwing for each protocol. The three outputs SHALL be equal by `equals()`.
7. WHEN a CloudWatch client from the Prototype_Service_Set is configured in turn with the `rpcv2Cbor`, `awsJson1_0`, and `awsQuery` Client_Protocols and the called operation returns a modeled error, THE client SHALL throw the same modeled exception type with the same error code for each protocol.

### Requirement 13: Client transports

**User Story:** As an SDK user, I want existing HTTP clients to keep working under schema-based serde, so that adoption does not require a new HTTP stack.

#### Acceptance Criteria

1. THE SDK SHALL provide one Client_Transport adapter that wraps any `SdkHttpClient` implementation and one Client_Transport adapter that wraps any `SdkAsyncHttpClient` implementation, including every `SdkHttpClient` and `SdkAsyncHttpClient` implementation shipped in the SDK.
2. THE Client_Transport SHALL report at runtime the request message type it accepts and the response message type it returns.
3. IF a client is built with a Client_Protocol whose request or response message type does not match the reported types of the configured Client_Transport, THEN THE SDK SHALL fail the client build with an exception that names the mismatched types, and SHALL NOT send any request.
4. WHEN the Client_Transport adapter sends a request, THE Client_Transport adapter SHALL deliver to the wrapped HTTP client the HTTP method, URI, headers, and body bytes produced by the Client_Protocol without adding, removing, or changing any of them, and SHALL return to the Client_Protocol the status code, headers, and body bytes received from the wrapped HTTP client without changing them.
5. IF the wrapped HTTP client throws an exception or completes its response future exceptionally, THEN THE Client_Transport SHALL raise the same SDK exception type, with the same retryable classification, that the Baseline_Build raises for the same failure, and SHALL set the original exception as the cause.
6. WHEN a non-streaming request body is serialized into a single byte buffer, THE Client_Transport adapter SHALL give the wrapped HTTP client that same buffer instance with no intermediate copy of its bytes. The SDK SHALL implement this zero-copy body contract on this branch, modeled on the one prototyped on branch `feature/poc/racecar`, which is reference-only and not merged.
7. WHEN a user configures a client with an existing `SdkHttpClient` or `SdkAsyncHttpClient` instance, THE SDK SHALL send every request of that client through that instance via the Client_Transport adapter.
8. WHEN a client that uses a Client_Transport adapter is closed, THE SDK SHALL close the wrapped HTTP client only if the SDK created it, and SHALL leave a user-provided HTTP client open, matching the Baseline_Build behavior.

### Requirement 14: Streaming and event streams

**User Story:** As an SDK user, I want streaming blobs and event streams to work under schema-based serde, so that streaming operations keep their current behavior.

#### Acceptance Criteria

1. WHEN an input member targets a blob with the `streaming` trait, THE Client_Protocol SHALL bind the caller's `RequestBody` or `AsyncRequestBody` as the transport request body. It SHALL pass the body's content length to the Client_Transport when the body declares one, and it SHALL NOT read the body's content into memory before the Client_Transport starts sending it.
2. WHEN a successful response is received for an operation whose output member targets a blob with the `streaming` trait, THE Client_Protocol SHALL first deserialize the output's non-streaming members (headers, prefix headers, status code) into the output object. It SHALL then pass that output object and the unconsumed response body to the caller's `ResponseTransformer` or `AsyncResponseTransformer`, and return the transformer's result as the operation result.
3. IF a response for an operation with a streaming output member is an error response, THEN THE Client_Protocol SHALL deserialize the error through the operation's Error_Registry and throw the resulting modeled exception (or the service's generic exception if no entry matches). It SHALL NOT pass the error body to the caller's `ResponseTransformer` or `AsyncResponseTransformer`.
4. WHEN the SDK serializes an outbound event stream message, THE SDK SHALL use the event member's Schema with Shape_Serializer. It SHALL write each member that has the `eventHeader` trait as a message header and the member that has the `eventPayload` trait as the message body. If no member has `eventPayload`, it SHALL write all non-header members as the message body using the protocol's Codec, and it SHALL set the message's event type to the name of the event stream union member.
5. WHEN the SDK receives an inbound event stream message whose event type matches a member of the event stream union, THE SDK SHALL deserialize the message into that member's event type through its Schema and Shape_Deserializer. It SHALL populate `eventHeader` members from the message headers and the `eventPayload` member, or the remaining members, from the message body, and it SHALL deliver events to the subscriber in the order they arrived.
6. IF the SDK receives an inbound event stream message that is a modeled exception event, THEN THE SDK SHALL deserialize it through the matching error member's Schema, signal the resulting exception to the subscriber and the event stream response handler's exception callback, and deliver no further events from that stream.
7. WHERE the operation's protocol sends non-stream input or output members in an initial event stream message, THE SDK SHALL serialize the input's non-stream members as the first outbound message and deserialize the output's non-stream members from the first inbound message. It SHALL do this through the input and output Schemas, before any event is delivered.
8. THE SDK SHALL keep the current generated public event stream API under schema-based serde, in source-compatible form (Requirement 16). That covers `org.reactivestreams.Publisher` for event streams, the event stream response handler interfaces, and the event visitor interfaces. Inbound events with an unrecognized event type SHALL keep being delivered through the visitor's existing default (unknown-event) path.

### Requirement 15: HTTP binding serde

**User Story:** As a protocol implementer, I want shared HTTP binding serializers and deserializers, so that REST protocols reuse one binding implementation regardless of payload codec.

#### Acceptance Criteria

1. WHEN an input Serializable_Struct is serialized into a transport request, THE Http_Binding_Serde SHALL write each member with a non-null value to the location named by its binding trait:
   - an `httpLabel` member goes into the URI path segment whose label name matches the member name.
   - an `httpQuery` member becomes a query parameter named by the trait value.
   - each entry of an `httpQueryParams` map becomes a query parameter named by the entry key.
   - an `httpHeader` member becomes a header named by the trait value.
   - each entry of an `httpPrefixHeaders` map becomes a header named by the prefix followed by the entry key.
   - an `httpPayload` member becomes the entire body. Blob and string targets are written as raw bytes. Structure, union, and document targets are written through the protocol's Codec.
   - every member without a binding trait (unbound body members) is written into the body through the protocol's Codec.

   Members with a null value SHALL be left out of every location. The body SHALL be empty when the input has neither a non-null `httpPayload` member nor any non-null unbound body member.
2. WHEN a transport response is deserialized into an output Shape_Builder, THE Http_Binding_Serde SHALL populate the output as follows:
   - each `httpHeader` member is read from the header whose name matches the trait value, compared case-insensitively.
   - each `httpPrefixHeaders` map member gets one entry for every header whose name starts with the prefix, compared case-insensitively. The entry key is the header name with the prefix removed.
   - the `httpResponseCode` member gets the HTTP status code.
   - the `httpPayload` member is read from the entire body. Blob and string targets are read as raw bytes. Structure, union, and document targets are read through the Codec.
   - unbound body members are read through the Codec.

   A member whose header is absent from the response SHALL be left unset in the Shape_Builder.
3. THE Http_Binding_Serde SHALL serialize and deserialize timestamp members in headers in the `http-date` format, and timestamp members in labels and query parameters in the `date-time` format. When a member has a `timestampFormat` trait with value `epoch-seconds`, `date-time`, or `http-date`, that format SHALL override the location default in both directions. Timestamps in the body SHALL follow the Codec_Settings of the protocol's Codec.
4. THE Http_Binding_Serde SHALL compute the binding plan for each input and output Schema at most once per Schema instance, store it as a Schema_Extension (Requirement 5), and reuse the stored plan for every later serialization or deserialization that uses that Schema.
5. WHEN `httpLabel` values are serialized, THE Http_Binding_Serde SHALL percent-encode every byte of the value's UTF-8 encoding that falls outside the RFC 3986 unreserved set (`A–Z`, `a–z`, `0–9`, `-`, `.`, `_`, `~`). The only exception is `/`, which SHALL be left unencoded for greedy labels (`{name+}`) and nowhere else.
6. WHEN list-valued or conflicting query and header members are serialized:
   - THE Http_Binding_Serde SHALL write a list-valued `httpQuery` member as one query parameter per element, in list order.
   - It SHALL write a list-valued `httpHeader` member as one header whose value is the elements joined by `, ` in list order.
   - It SHALL percent-encode query parameter names and values using the unreserved set from criterion 5, with `/` encoded.
   - It SHALL drop any `httpQueryParams` entry whose key matches an `httpQuery` parameter name, so the `httpQuery` value wins.
7. IF an `httpLabel` member is null or an empty string at serialization time, THEN THE Http_Binding_Serde SHALL fail with a serialization error that names the member, and SHALL not pass any request to the Client_Transport.
8. IF a header value cannot be converted to the target type of its `httpHeader` or `httpPrefixHeaders` member (for example, a non-numeric value for an integer member, a value other than `true` or `false` for a boolean member, or a timestamp that does not match the expected format), THEN THE Http_Binding_Serde SHALL fail with a deserialization error that names the member, and SHALL not return a built output.

### Requirement 16: Public API compatibility of generated code

**User Story:** As an existing SDK customer, I want my code to keep compiling against regenerated clients, so that adopting schema-based serde is not a breaking change.

#### Acceptance Criteria

1. THE Generated_Model_Class for each Prototype_Service_Set service SHALL keep every public fluent getter, static `builder()` and `serializableBuilderClass()` method, builder interface method, `toBuilder`, `equals`, `hashCode`, `toString`, and `SdkPojo.sdkFields()` that exists in the Baseline_Build for the same shape. Each kept member SHALL have the same name, parameter types, return type, and declared checked exceptions as in the Baseline_Build.
2. THE generated sync and async client interfaces for each Prototype_Service_Set service SHALL keep every public method that exists in the Baseline_Build, with the same name, parameter types (including `Consumer<Builder>` overloads), return type (including generic type arguments such as `CompletableFuture<T>`), and declared exceptions.
3. WHEN Codegen adds a new public method to a generated client interface or to a generated builder interface, THE Codegen SHALL emit that method as a `default` method, so that existing customer implementations of the interface still compile.
4. THE generated modeled exception classes for each Prototype_Service_Set service SHALL keep the same direct superclass (the service base exception class) and the same public builder methods as in the Baseline_Build.
5. THE Legacy_Document SHALL keep every public type, method, and static factory that exists in the Baseline_Build, with the same signatures and the same return values for the same inputs.
6. WHEN the SDK's japicmp API compatibility check runs on a Prototype_Service_Set module and on the module that contains Legacy_Document, comparing against the Baseline_Build, THE check SHALL report zero removed public members and zero changed public signatures, apart from deviations that the Porting_Map lists by fully qualified signature together with a reason.
7. IF the japicmp API compatibility check reports a removed or changed public signature that the Porting_Map does not list, THEN THE SDK build for that module SHALL fail and SHALL name the incompatible signature in its output.
8. WHEN a source-compatibility test fixture, written against the Baseline_Build public API of a Prototype_Service_Set service (building requests, calling sync and async client methods, reading response getters, catching modeled exceptions, and calling `toBuilder`), is compiled against the regenerated module, THE compilation SHALL succeed with zero errors.

### Requirement 17: Smithy-based IntermediateModel and Codegen_Pipeline

**User Story:** As a Codegen maintainer, I want an IntermediateModel built only from Smithy concepts, and C2J converted to Smithy before code generation, so that schema codegen has one model source.

#### Acceptance Criteria

1. THE Codegen_Pipeline SHALL run these stages in this order for each service: (1) C2J load, (2) C2J customizations, (3) C2J_Converter, (4) Smithy_Model assembly and validation, (5) Intermediate_Model construction, (6) code generation. No stage starts before the previous stage has finished.
2. THE SDK SHALL include the C2J_Converter as a codegen module ported from `codegen-c2j-to-smithy` on the `mtdowling/aws-sdk-java-v2` `smithy-java-bridge` branch. The module compiles to Java 8 bytecode and is the only component in the Codegen_Pipeline that reads C2J model files.
3. WHEN a service has a C2J model and no native Smithy model, THE Codegen SHALL convert the C2J model, with customizations applied, through the C2J_Converter and generate the service from the resulting Smithy-based Intermediate_Model.
4. WHEN a service has a native Smithy model, THE Codegen SHALL generate the service from that Smithy model without running the C2J_Converter, even if a C2J model for the same service is also present.
5. THE Intermediate_Model SHALL hold a reference to the assembled, validated Smithy_Model and to the absolute ShapeId of the service shape being generated. That ShapeId resolves to a service shape in the referenced Smithy_Model.
6. THE `ShapeModel`, `OperationModel`, and `MemberModel` types SHALL each expose the absolute Smithy ShapeId of the shape, operation, or member they represent. Member ShapeIds use the `namespace#Shape$member` form, and every exposed ShapeId resolves to a shape in the referenced Smithy_Model.
7. THE Intermediate_Model SHALL contain no field, accessor, or lookup method that takes or returns C2J concepts. This includes the `c2jName` and `c2jShape` fields and accessors, the `getShapeByNameAndC2jName` lookup, and the C2J `location`/`locationName` mappings.
8. THE Intermediate_Model SHALL derive member protocol binding information only from the Smithy traits `httpHeader`, `httpPrefixHeaders`, `httpQuery`, `httpQueryParams`, `httpLabel`, `httpPayload`, `httpResponseCode`, `jsonName`, `xmlName`, `xmlAttribute`, `xmlFlattened`, `xmlNamespace`, and `ec2QueryName`. IF a member has no applicable name trait, THEN THE Intermediate_Model SHALL use the Smithy member name as the serialized name.
9. THE Intermediate_Model construction SHALL read no C2J_Marker_Trait. For any service, a Smithy_Model from which all `com.amazonaws.c2j` traits have been removed yields the same Intermediate_Model as the original Smithy_Model.
10. WHERE the C2J_Converter has no standard Smithy or AWS trait for a piece of C2J information, THE C2J_Converter SHALL write a conversion gap report entry for each unmapped C2J property. Each entry lists the C2J property name, every affected service, and the generated-code impact. The report is produced on every C2J_Converter run and contains zero entries when no gaps exist.
11. IF a customization from `customization.config` fails to apply, or conflicts with schema codegen, for a Prototype_Service_Set service, THEN THE Codegen SHALL skip that customization, log the customization name and the service name, and continue generating that service with all remaining customizations applied.
12. WHERE a service is generated with schema-based serde, THE Codegen SHALL generate no classes in the per-protocol marshaller and unmarshaller `transform` package for that service. No other generated class for that service references a `transform` package class.
13. IF Smithy_Model assembly or validation reports one or more validation events of severity ERROR or DANGER, THEN THE Codegen SHALL stop generating that service before Intermediate_Model construction, write no generated sources for that service, and report each event's shape ID and message.
14. IF the C2J_Converter cannot convert a C2J shape, operation, or service property into the Smithy_Model, THEN THE Codegen SHALL stop generating that service, write no generated sources for that service, and report the service name and the C2J shape or property name that failed.

---

**Part B: Document Types and Type Registries**

### Requirement 18: Enhanced Document interface

**User Story:** As an SDK user, I want a Document type covering the full Smithy data model, so that I can carry and convert any modeled data, including blobs and timestamps.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide a Document interface that is equivalent to smithy-java `Document`, that implements Serializable_Shape, and whose `serialize(ShapeSerializer)` method writes the Document through exactly one call to `ShapeSerializer.writeDocument`.
2. THE Document SHALL provide one coercion method, named `as<Type>`, for each of these 14 types: boolean, byte, short, integer, long, float, double, bigInteger, bigDecimal, string, blob, timestamp, list of Document, and map from String to Document (`asMap()`).
3. THE Document SHALL provide `getMember(String)` returning a Document, `getMemberNames()` returning the member names, `getDiscriminator()` returning a ShapeId_Runtime, `type()` returning the Smithy shape type, and `asShape(ShapeBuilder<T>)` returning a `T`.
4. WHEN a numeric coercion method (byte, short, integer, long, float, double, bigInteger, or bigDecimal) is called on a Document whose `type()` is a numeric type, THE Document SHALL return the value converted using Java widening (JLS 5.1.2) and narrowing (JLS 5.1.3) primitive conversion rules, without throwing on overflow or loss of precision (for example, `asInteger()` on a long Document holding 4294967297 returns 1, and `asInteger()` on a double Document holding 1.9 returns 1).
5. WHEN `type()` is called on a numeric Document that has no explicit Schema, THE Document SHALL report the first of integer, long, bigInteger, double, and bigDecimal that holds the value without loss of precision, and SHALL never report byte, short, float, or intEnum (for example, 0 reports integer, 2147483648 reports long, 9223372036854775808 reports bigInteger, 1.5 reports double, and 1.00000000000000000001 reports bigDecimal).
6. WHEN `asMap()` is called on a structure Document, THE Document SHALL return a map whose keys are the Smithy member names (not `jsonName` or `xmlName` values) of the members present in the Document, each mapped to that member's Document value.
7. IF a coercion method is called for a type that differs from the Document's `type()` and the call is not a numeric-to-numeric coercion (criterion 4), a structure-to-map coercion (criterion 6), or a Protocol_Document coercion defined in Requirement 19, THEN THE Document SHALL throw a `SerializationException` that names the requested type and the Document's actual type, and SHALL return no value.
8. THE map and structure Documents created from serialized data SHALL return entries from `asMap()` and names from `getMemberNames()` in the order the entries appear in the source data.
9. THE Document SHALL declare no `asStructure` or `asUnion` method, so that structure and union values are produced only through `asShape(ShapeBuilder)`.
10. IF `getMember(String)` is called with a name that is not a member or key present in the Document, THEN THE Document SHALL return null without throwing.
11. WHEN `getDiscriminator()` is called on a structure Document created from a Generated_Model_Class, THE Document SHALL return the ShapeId_Runtime of that model's shape, and WHEN it is called on a Document that holds no Discriminator, THE Document SHALL return null.
12. IF `asShape(ShapeBuilder)` is called on a Document whose data does not match the shape type of the builder's Schema (for example, a string Document with a structure builder), THEN THE Document SHALL throw a `SerializationException` and SHALL return no built value.

### Requirement 19: Document implementations

**User Story:** As a codec implementer, I want protocol-agnostic Documents for serialization and protocol-specific Documents for deserialization, so that documents keep the context needed to read protocol-renamed members.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide protocol-agnostic Document implementations, equivalent to smithy-java `Documents`, for boolean, byte, short, integer, long, float, double, bigInteger, bigDecimal, string, blob, timestamp, list, string-keyed map, and structure values. Each implementation SHALL hold a Schema, and `type()` SHALL return that Schema's ShapeType.
2. THE JSON Codec SHALL provide Protocol_Document implementations, equivalent to smithy-java `JsonDocuments`, for JSON boolean, number, string, array, and object values. Each one SHALL hold the Codec_Settings of the JSON Codec that read it.
3. THE CBOR Codec SHALL provide Protocol_Document implementations, equivalent to smithy-java `CborDocuments`, for every CBOR value type the CBOR Codec decodes. Each one SHALL hold the Codec_Settings of the CBOR Codec that read it.
4. WHEN `asBlob()` is called on a JSON string Protocol_Document that holds valid base64, THE JSON Protocol_Document SHALL return the base64-decoded bytes of the string.
5. WHEN `asTimestamp()` is called on a JSON string Protocol_Document whose Codec_Settings default timestamp format is `date-time` or `http-date`, or on a JSON number Protocol_Document whose Codec_Settings default timestamp format is `epoch-seconds`, THE JSON Protocol_Document SHALL return the value parsed with that default timestamp format.
6. WHEN `asShape` is called on a Protocol_Document, THE Protocol_Document SHALL match members by `jsonName` if the Codec_Settings enable `jsonName`, and by Smithy member name if they do not. It SHALL read timestamp members with the member's `timestampFormat` trait if the Codec_Settings enable `timestampFormat`, and with the Codec_Settings default timestamp format if they do not.
7. WHEN `Document.of` is called with a Serializable_Shape, THE Runtime_Schema_Module SHALL use a port of smithy-java `DocumentParser` to return a Document whose `type()` equals the Serializable_Shape's Schema ShapeType. For a Serializable_Struct, the returned Document SHALL report the Serializable_Struct Schema's ShapeId as its Discriminator and SHALL return null from `getMember` for every member that has no value set.
8. FOR ALL Generated_Model_Class instances, converting to a Document with `Document.of` and back with `asShape` SHALL produce a value equal to the original (round-trip property).
9. IF `asBlob()` is called on a JSON string Protocol_Document whose value is not valid base64, THEN THE JSON Protocol_Document SHALL throw a `SerializationException` indicating that a base64-encoded blob was expected.
10. IF `asTimestamp()` is called on a JSON string Protocol_Document whose Codec_Settings default timestamp format is `epoch-seconds`, on a JSON number Protocol_Document whose default timestamp format is `date-time` or `http-date`, or on a value that the default timestamp format cannot parse, THEN THE JSON Protocol_Document SHALL throw a `SerializationException` that names the default timestamp format.
11. FOR ALL Generated_Model_Class instances, serializing with the JSON Codec, reading the bytes back as a Protocol_Document with the same Codec_Settings, and calling `asShape` SHALL produce a value equal to the original, whether `jsonName` is enabled or disabled in the Codec_Settings (round-trip property).

### Requirement 20: Document serialization and discriminators

**User Story:** As an SDK user, I want documents with typed structures to serialize correctly in every protocol that supports documents, so that type information survives protocol changes.

#### Acceptance Criteria

1. THE Client_Protocols for `aws.protocols#awsJson1_0`, `aws.protocols#awsJson1_1`, `aws.protocols#restJson1`, and `smithy.protocols#rpcv2Cbor` SHALL serialize every Document-targeting member through `ShapeSerializer.writeDocument` and deserialize every Document-targeting member through `ShapeDeserializer.readDocument`.
2. WHEN a structure Document that has a Discriminator is serialized by a Client_Protocol listed in criterion 1, THE Shape_Serializer SHALL write the Discriminator as an absolute ShapeId (`namespace#Name`) in the `__type` member of the serialized structure. This applies even when the Discriminator was read in relative form or was read by a different Client_Protocol.
3. WHEN a Protocol_Document is deserialized from data whose `__type` member holds a relative ShapeId (a shape name with no namespace), THE Protocol_Document SHALL report a Discriminator whose namespace is the default namespace in its Codec_Settings and whose name is the `__type` value. For a generated client, the default namespace is the namespace of the service's ShapeId. WHEN `__type` holds an absolute ShapeId, THE Protocol_Document SHALL report that ShapeId unchanged.
4. WHEN a Document wrapping a typed structure is serialized, THE Shape_Serializer SHALL apply the serializing protocol's Codec_Settings to the typed structure's members. This covers `jsonName` member naming, `timestampFormat` trait use, and the default timestamp format. It applies regardless of which protocol, if any, the Document was originally read from.
5. THE SDK SHALL run every test case in the `serdeTests` array of `new-document-type-test-cases.json` against `new-document-type-test-model.json`, using the codec and Codec_Settings each case specifies. Each test case SHALL pass all six assertions defined by the Document Type and Type Registries SEP:
   - data object from the canonical serialization
   - data object from the document built from the data object
   - data object from the document built from the serialization
   - serialization of the data object
   - serialization of the document built from the serialization, using `writeDocument`
   - serialization of the document built from the data object
6. IF a structure Document has no Discriminator, THEN THE Shape_Serializer SHALL serialize it without a `__type` member.
7. IF a deserialized Protocol_Document has no `__type` member, THEN THE Protocol_Document SHALL report a null Discriminator and SHALL NOT raise an error.
8. IF the `__type` value of a deserialized Protocol_Document is not a syntactically valid ShapeId, is not a string, or is a relative ShapeId while the Codec_Settings has no default namespace, THEN THE Protocol_Document SHALL raise an error indicating an invalid Discriminator. The error SHALL occur no later than the first call that reads the Discriminator, and THE Protocol_Document SHALL NOT report a Discriminator value.

### Requirement 21: Legacy_Document interoperability

**User Story:** As an existing SDK customer, I want to convert between the existing Document and the new Document, so that existing document-typed members keep working.

#### Acceptance Criteria

1. THE SDK SHALL provide a public converter, equivalent to smithy-java `aws-sdkv2-shapes` `DocumentConverter`, with one operation that converts a Legacy_Document to a Document and one operation that converts a Document to a Legacy_Document. The Legacy_Document-to-Document operation SHALL map each value as follows. A string becomes a string Document. A boolean becomes a boolean Document. A number becomes a numeric Document with the same `SdkNumber` string representation. A list becomes a list Document with the same element count and order. A map becomes a map Document with the same key set. A null becomes a null Document value. List elements and map values SHALL be converted recursively.
2. FOR ALL Legacy_Document values, including nested lists and maps and null values, converting to Document and back SHALL produce a Legacy_Document equal to the original under `Legacy_Document.equals`, and every number SHALL keep its `SdkNumber` string representation (round-trip property).
3. IF a Document holding a blob or timestamp is converted to a Legacy_Document, THEN THE converter SHALL encode a blob as a Legacy_Document string with standard base64 encoding and padding, and SHALL encode a timestamp as a Legacy_Document number of epoch seconds with at most 3 fractional digits (millisecond precision), matching the `aws.protocols#awsJson1_0`, `aws.protocols#awsJson1_1`, and `aws.protocols#restJson1` epoch-seconds encoding.
4. THE generated accessors for members that target `document` shapes SHALL keep returning Legacy_Document, and THE generated builder setters for those members SHALL keep accepting Legacy_Document.
5. WHEN a Document that holds no blob or timestamp is converted to a Legacy_Document, THE converter SHALL map each value as follows. A string becomes a string. A boolean becomes a boolean. A byte, short, integer, long, float, double, bigInteger, or bigDecimal becomes a number with the same numeric value. A list becomes a list with the same element count and order. A map becomes a map with the same keys. A structure or union becomes a map whose keys are the member names of the set members only, with no entry for the Discriminator. Nested values SHALL be converted recursively.
6. IF either converter operation receives a Java `null` argument, THEN THE converter SHALL throw an exception indicating a null argument and SHALL return no value.
7. WHEN a Generated_Model_Class instance with a `document`-targeting member set to a Legacy_Document is serialized and then deserialized with schema-based serde under any of the Supported_Protocols, THE deserialized instance's accessor for that member SHALL return a Legacy_Document equal to the original under `Legacy_Document.equals`.

### Requirement 22: Type registries

**User Story:** As an SDK user, I want to build typed objects from a ShapeId at runtime, so that documents and errors can become concrete SDK types on demand.

#### Acceptance Criteria

1. THE Runtime_Schema_Module SHALL provide a Type_Registry interface and builder equivalent to smithy-java `TypeRegistry`, with the instance methods `getShapeClass(ShapeId)`, `createBuilder(ShapeId)`, `contains(ShapeId)`, and `deserialize(Document)`, the static methods `compose(TypeRegistry, TypeRegistry)` and `empty()`, and a builder that registers a ShapeId together with its Generated_Model_Class and Shape_Builder supplier, all compiling against Java 8.
2. WHEN `deserialize(Document)` is called with a Document whose Discriminator is registered, THE Type_Registry SHALL return an instance of the Generated_Model_Class registered for that Discriminator, built through a newly created Shape_Builder, with member values equal to those the same Document produces through `Document.asShape` with that Shape_Builder.
3. IF `deserialize(Document)` is called with a Document that has no Discriminator, THEN THE Type_Registry SHALL throw a `DiscriminatorException` and SHALL NOT create a Shape_Builder.
4. IF `deserialize(Document)` is called with a Document whose Discriminator is not registered, THEN THE Type_Registry SHALL throw an `UnsupportedOperationException` whose message contains the full Discriminator ShapeId (`namespace#Name`).
5. WHEN two Type_Registries are composed, THE composed Type_Registry SHALL resolve a ShapeId through `getShapeClass`, `createBuilder`, and `deserialize` from the first registry if the first registry contains it, and otherwise from the second registry. `contains` SHALL return true when either registry contains the ShapeId, and neither input registry SHALL be modified.
6. THE Codegen SHALL generate a Primary_Type_Registry for each generated package. The registry SHALL contain every shape of type `structure` listed in the service's `shapes` property and SHALL NOT contain shapes of type `union`, shapes of any other type, or structures that are not listed in that property. It SHALL be exposed as a public static member that takes no arguments, needs no client instance, and returns the same instance on every access.
7. THE generated Primary_Type_Registry SHALL NOT initialize any Generated_Model_Class while the registry is created or while `contains` is called. WHEN a ShapeId is first resolved through `getShapeClass`, `createBuilder`, or `deserialize`, THE Primary_Type_Registry SHALL load the Generated_Model_Class registered for that ShapeId at that time.
8. IF `getShapeClass` or `createBuilder` is called with a ShapeId that is not registered, THEN THE Type_Registry SHALL return null without throwing, and `contains` SHALL return false for that ShapeId.
9. THE Type_Registry returned by `empty()` SHALL return false from `contains` for every ShapeId, SHALL return null from `getShapeClass` and `createBuilder`, and SHALL act as an identity element under `compose`, so that `compose(empty(), r)` and `compose(r, empty())` resolve every ShapeId the same way `r` does.

### Requirement 23: Error registry and error deserialization

**User Story:** As an SDK maintainer, I want errors resolved through a type registry, so that per-operation exception mapping code is removed from generated clients.

#### Acceptance Criteria

1. THE Codegen SHALL generate exactly one Error_Registry per service. The Error_Registry SHALL contain an entry for every shape with the `error` trait in the service closure, covering both operation-level and service-level errors, and SHALL contain no shape that lacks the `error` trait. Each entry SHALL map the error's ShapeId to a Shape_Builder for its Generated_Model_Class.
2. WHEN a Client_Protocol deserializes an error response, THE Client_Protocol SHALL locate the error Discriminator from these locations, checked in the listed order, using the first one present:
   - `awsJson1_0`, `awsJson1_1`, and `restJson1`: the `X-Amzn-Errortype` header, then the body `__type` member, then the body `code` member.
   - `rpcv2Cbor`: the body `__type` member.
   - `restXml`, `awsQuery`, and `ec2Query`: the body error `Code` element.
   - Where the service has the `aws.protocols#awsQueryCompatible` trait: the error-code part of the `x-amzn-query-error` header, before any other location for that protocol.
3. WHEN the Error_Registry resolves a Discriminator, THE Error_Registry SHALL first drop any text from the first `:` onward. It SHALL then resolve an absolute value (`namespace#Name`) by exact ShapeId match and a relative value (`Name`) as `<service namespace>#Name`. For `awsQuery`, `ec2Query`, and `awsQueryCompatible` services, it SHALL resolve an error shape that has the `aws.protocols#awsQueryError` trait by that trait's custom code.
4. WHEN the Discriminator resolves to an entry in the Error_Registry, THE Client_Protocol SHALL throw an instance of that error's Generated_Model_Class. Every modeled member present in the response SHALL be deserialized into it, and it SHALL carry the error code, the message, the HTTP status code, and the request ID.
5. IF no Discriminator can be located, or the Discriminator is not found in the Error_Registry, or the error body cannot be parsed in the protocol's format, THEN THE Client_Protocol SHALL throw the service base exception populated with the located error code (null if none was located), the message (null if absent), the HTTP status code, and the request ID. It SHALL NOT throw a deserialization exception in place of the base exception.
6. THE generated sync and async clients SHALL contain no per-operation or per-client exception metadata mapping or registration code. Error resolution for every operation SHALL go through the service's single Error_Registry.
7. WHEN an operation receives an error whose Discriminator matches an error shape in the Error_Registry that is not listed in that operation's `errors`, THE Client_Protocol SHALL throw that error's Generated_Model_Class instance, which matches the Baseline_Build's service-wide exception resolution.
8. WHEN any Legacy_Protocol_Tests or Protocol_Test_Services error test case runs against a Prototype_Service_Set or protocol test service client, THE resolved exception class, error code, HTTP status code, message, request ID, and modeled member values SHALL equal those the Baseline_Build produces for that case, for 100% of error test cases.

### Requirement 24: Typed document Codegen exclusion

**User Story:** As a size-conscious maintainer, I want document conversion to reuse generated schemas, so that documents add no generated code.

#### Acceptance Criteria

1. WHEN Codegen runs for a Prototype_Service_Set service, THE generated source SHALL contain no class, method, or field that converts a specific shape to or from a Document. This includes per-shape `toDocument` or `fromDocument` methods, per-shape Document serializers or deserializers, and per-shape Document converter classes. Three things are exempt: the Primary_Type_Registry (Requirement 22), the Error_Registry (Requirement 23), and the Legacy_Document handling for members that target `document` shapes (Requirement 21).
2. WHEN `Document.of` is called with a Generated_Model_Class instance, THE SDK SHALL build the Document through the shared `DocumentParser` port (Requirement 19), calling only the instance's `schema()` and `serializeMembers(ShapeSerializer)` methods. THE SDK SHALL also set the Document's Discriminator to the ShapeId of the instance's Schema.
3. WHEN `asShape(ShapeBuilder)` is called on a Document with the builder of a Generated_Model_Class, THE Document SHALL fill the builder only by calling the builder's `deserialize(ShapeDeserializer)` method, and SHALL match members using the builder's Schema.
4. THE SDK SHALL support the Document conversions in criteria 2 and 3 for every Generated_Model_Class without requiring any trait, `customization.config` entry, or Codegen option to turn them on.

---

**Part C: Dynamic Client**

### Requirement 25: Runtime model to schema conversion

**User Story:** As a dynamic-client user, I want Smithy models converted to runtime schemas, so that a client can run against a model it was not generated from.

#### Acceptance Criteria

1. WHEN a caller requests the Schema for a ShapeId present in a Smithy_Model, THE Schema_Converter SHALL return a Schema whose ShapeId, ShapeType, and member names equal those of the model shape, with members in model declaration order, for every shape type in the ShapeType enumeration, equivalent to smithy-java `SchemaConverter`.
2. WHEN a caller requests the Schema for the same ShapeId from the same Smithy_Model instance more than once, THE Schema_Converter SHALL return the same Schema instance (reference equality) for each request, and SHALL NOT return a Schema converted from a different Smithy_Model instance.
3. THE Schema_Converter SHALL attach to each converted Schema and Member_Schema exactly the traits that the default Trait_Inclusion_Policy (Requirement 9) includes, and SHALL omit every trait that the policy excludes, including every C2J_Marker_Trait.
4. WHEN an included trait has a typed Runtime_Trait registered in the trait factory registry, THE Schema_Converter SHALL materialize that trait as the typed Runtime_Trait. Otherwise, THE Schema_Converter SHALL materialize the trait as a Generic_Trait whose Document value equals the trait's value in the Smithy_Model.
5. WHEN the Smithy_Model contains a recursive shape, whether self-referencing, mutually recursive (A → B → A), or recursive through a list or map, THE Schema_Converter SHALL complete conversion without unbounded recursion. A Member_Schema that targets an enclosing shape SHALL resolve to the same Schema instance that the Schema_Converter returns for that enclosing shape.
6. WHEN a shape carries the original-shape-ID marker from a model transform, THE Schema_Converter SHALL produce wire-facing names derived from the ShapeId, such as the XML root element name, from the original ShapeId rather than the transformed ShapeId. For shapes without the marker, it SHALL derive these names from the shape's own ShapeId.
7. FOR ALL shapes in the Prototype_Service_Set models, the Schema produced by the Schema_Converter and the generated Schema SHALL have equal ShapeIds, ShapeTypes, member names, member order, member target ShapeIds, and included trait values (model-based property).
8. IF the requested ShapeId is not present in the Smithy_Model, or a member of the shape's closure targets a ShapeId not present in the Smithy_Model, THEN THE Schema_Converter SHALL fail with an error naming the missing ShapeId and SHALL NOT cache a Schema for the requested ShapeId.
9. WHEN multiple threads request the Schema for the same ShapeId from one Schema_Converter at the same time, THE Schema_Converter SHALL return the same Schema instance to every thread.

### Requirement 26: Dynamic client construction

**User Story:** As a tool developer, I want to build a client from a Smithy JSON AST model, so that I can call services without a code generation step.

#### Acceptance Criteria

1. THE Dynamic_Client builder SHALL accept a Smithy_Model loaded from a single-file Smithy JSON AST.
2. WHEN the Dynamic_Client builder builds a client, the Smithy_Model contains exactly one service shape, and no service ShapeId is configured, THE Dynamic_Client builder SHALL resolve the target service to that service shape.
3. IF the Smithy_Model contains two or more service shapes and no service ShapeId is configured when the Dynamic_Client builder builds a client, THEN THE Dynamic_Client builder SHALL throw an exception whose message lists the ShapeId of every service shape in the Smithy_Model, and SHALL NOT return a Dynamic_Client.
4. IF the Smithy_Model contains zero service shapes when the Dynamic_Client builder builds a client, THEN THE Dynamic_Client builder SHALL throw an exception indicating that the model contains no service, and SHALL NOT return a Dynamic_Client.
5. THE Dynamic_Client builder SHALL accept the same client configuration options as a generated client builder (region, credentials provider, HTTP client, override configuration, interceptors, and plugins), and WHEN one of these options is not configured, THE Dynamic_Client SHALL resolve it to the same default that a generated client builder resolves for that option.
6. WHEN the Dynamic_Client builder resolves the target service, THE Dynamic_Client SHALL expose an Api_Service whose Schema is a Root_Schema with ShapeType service and with ShapeId equal to the resolved service's ShapeId.
7. IF no Smithy_Model is configured when the Dynamic_Client builder builds a client, THEN THE Dynamic_Client builder SHALL throw an exception indicating that a model is required, and SHALL NOT return a Dynamic_Client.
8. WHEN a service ShapeId is configured on the Dynamic_Client builder and the Smithy_Model contains a service shape with that ShapeId, THE Dynamic_Client builder SHALL resolve the target service to that service shape, whatever the number of service shapes in the Smithy_Model.
9. IF a configured service ShapeId does not identify a service shape in the Smithy_Model (the ShapeId is absent, or names a shape whose type is not service), THEN THE Dynamic_Client builder SHALL throw an exception whose message includes the configured ShapeId, and SHALL NOT return a Dynamic_Client.

### Requirement 27: Dynamic resolution of protocol, transport, auth, and endpoint

**User Story:** As a tool developer, I want the dynamic client to work out protocol, auth, and endpoint from the model, so that a call works with minimal configuration.

#### Acceptance Criteria

1. WHEN the Dynamic_Client builder builds a client with no Client_Protocol configured, THE Dynamic_Client builder SHALL select the first Client_Protocol in the SDK's priority-ordered protocol list that meets two conditions: its ShapeId matches a protocol trait on the service, and its transport type (for example, HTTP) is compatible with the configured Client_Transport, if one is configured. The order in which Client_Protocol implementations are discovered at runtime SHALL NOT affect the selection.
2. IF no protocol trait on the service matches an available Client_Protocol, THEN THE Dynamic_Client builder SHALL throw an exception and SHALL NOT return a client. The exception SHALL list the ShapeId of every protocol trait found on the service, or SHALL state that none were found if the service has no protocol trait.
3. WHEN no auth scheme resolver is configured, THE Dynamic_Client SHALL derive each operation's ordered effective auth schemes from the `auth`, `optionalAuth`, and auth definition traits, using the same rules as generated clients. An operation with no modeled auth schemes SHALL resolve to `smithy.api#noAuth` only.
4. IF an effective auth scheme has no runtime implementation, THEN THE Dynamic_Client SHALL do all of the following:
   - The builder SHALL build the client without throwing.
   - The builder SHALL log one warning per missing auth scheme, naming its ShapeId.
   - The client SHALL allow calls to every operation whose ordered effective auth schemes include at least one scheme with a runtime implementation or `smithy.api#noAuth`.
   - The client SHALL fail calls to any other operation with an exception naming the missing auth schemes, before any request is sent.
5. WHEN no endpoint provider or static endpoint is configured, THE Dynamic_Client SHALL evaluate the service's `endpointRuleSet` or `endpointBdd` trait at runtime for each call. Parameters SHALL be bound from the client configuration and from the call's operation and input Document, and the request SHALL be sent to the resulting endpoint.
6. WHERE a Client_Protocol, auth scheme resolver, endpoint provider, or static endpoint override is configured, THE Dynamic_Client SHALL use the configured component for every call instead of the model-derived one. If a static endpoint override is configured, every request SHALL be sent to the static endpoint without evaluating `endpointRuleSet` or `endpointBdd`.
7. WHEN the Dynamic_Client builder builds a client with no Client_Transport configured, THE Dynamic_Client builder SHALL resolve a Client_Transport that adapts the `SdkHttpClient` or `SdkAsyncHttpClient` chosen by the same HTTP client resolution rules generated clients use.
8. IF the transport type of the resolved Client_Protocol is incompatible with the configured or resolved Client_Transport, THEN THE Dynamic_Client builder SHALL throw an exception naming both the Client_Protocol and the Client_Transport, and SHALL NOT return a client.
9. IF endpoint resolution cannot produce an endpoint for a call, THEN THE Dynamic_Client SHALL fail the call with an exception indicating the cause and SHALL NOT send a request. This applies when no endpoint provider or static endpoint is configured and the service has neither an `endpointRuleSet` nor an `endpointBdd` trait. It also applies when rule evaluation reaches an error rule, and the exception SHALL then include that rule's error message.

### Requirement 28: Calling operations with documents

**User Story:** As a tool developer, I want to call operations by name with document input and output, so that I can work with any operation without generated types.

#### Acceptance Criteria

1. THE Dynamic_Client SHALL provide `call(operationName)`, `call(operationName, Document)`, and `call(operationName, Document, overrideConfiguration)` in a sync form that returns a Document and an async form that returns a `CompletableFuture` of a Document. `call(operationName)` SHALL behave the same as `call(operationName, Document)` with an empty map Document, and `overrideConfiguration` SHALL accept the same per-request override options as a generated client and apply them only to that call.
2. IF the operation name does not case-sensitively match the shape name of an operation in the service's closure (operations bound directly to the service or through its resources), THEN THE Dynamic_Client SHALL throw an exception that names the operation and the service ShapeId, and SHALL NOT send a request.
3. WHEN an operation is called, THE Dynamic_Client SHALL coerce the input Document to a schema-backed structure Document whose Discriminator is the operation's input ShapeId and whose members are keyed by Smithy member names (not protocol-specific names such as `jsonName` or `xmlName`). It SHALL do this before the Request_Pipeline runs any interceptor or serialization.
4. WHEN an input Document supplies a number for a timestamp member, THE Dynamic_Client SHALL read the number as epoch-seconds and keep any fractional part as sub-second precision down to milliseconds.
5. WHEN an input Document supplies a string for a timestamp member, THE Dynamic_Client SHALL parse the string as a Smithy `date-time` value (RFC 3339 section 5.6).
6. IF an input Document that is not a Protocol_Document supplies a string for a blob member, THEN THE Dynamic_Client SHALL throw a `SerializationException` naming the member, and SHALL NOT send a request.
7. WHEN a call succeeds, THE Dynamic_Client SHALL return a schema-backed structure Document whose Discriminator is the operation's output ShapeId. Its members SHALL be readable by Smithy member name, and modeled members missing from the response SHALL be absent from the Document.
8. WHEN `createStruct(ShapeId, Document)` is called with the ShapeId of a structure shape in the Smithy_Model, THE Dynamic_Client SHALL return a schema-backed structure Document with that ShapeId as its Discriminator, applying the coercion rules of criteria 3–6 and 10–11. IF the ShapeId does not identify a structure shape in the Smithy_Model, THEN `createStruct` SHALL throw an exception naming the ShapeId.
9. WHEN the same operation name is called more than once on one Dynamic_Client instance, THE Dynamic_Client SHALL reuse the Api_Operation, Schemas, Error_Registry, and effective auth schemes resolved on the first call. As a result, the Api_Operation exposed in the execution context SHALL be the same instance on each call.
10. IF an input Document contains a member name that is not a member of the target structure Schema, THEN THE Dynamic_Client SHALL throw a `SerializationException` naming the member and the structure ShapeId, and SHALL NOT send a request.
11. IF an input Document value cannot be coerced to its member's ShapeType, THEN THE Dynamic_Client SHALL throw a `SerializationException` naming the member, and SHALL NOT send a request. This covers a non-map value for a structure, a non-list value for a list, a string for a numeric member, a number outside the range of a byte, short, integer, or long member, and a string that is not a valid `date-time` for a timestamp member.
12. IF an async-form call fails for any reason in criteria 2, 6, 10, or 11, THEN THE Dynamic_Client SHALL return a `CompletableFuture` that completes exceptionally with that exception, and SHALL NOT throw from the method call.

### Requirement 29: Dynamic client errors

**User Story:** As a tool developer, I want modeled errors exposed as documents, so that I can read error members without generated exception classes.

#### Acceptance Criteria

1. WHEN a call returns an error response whose Discriminator resolves to an error shape in the operation's Error_Registry, THE Dynamic_Client SHALL throw a document-bearing modeled exception whose contents accessor returns a structure Document whose Discriminator equals that error shape's ShapeId and which contains every error member present in the response, using modeled member names and types from the error shape's Schema.
2. THE Dynamic_Client document-bearing modeled exception SHALL be a subtype of the SDK's generic service exception and SHALL expose the error code, message, HTTP status code, and request ID from the response, so that callers that catch the generic service exception also catch it.
3. THE Dynamic_Client Error_Registry for an operation SHALL contain every error shape bound to that operation and every error shape bound to the service, so that a call can resolve an error from either level.
4. IF an operation-level error shape and a service-level error shape resolve from the same Discriminator, THEN THE Dynamic_Client SHALL resolve it to the operation-level error shape.
5. WHEN the Dynamic_Client resolves an error Discriminator, THE Dynamic_Client SHALL accept both an absolute ShapeId (`namespace#Name`) and a relative name (`Name`), and SHALL resolve a relative name against the service's namespace.
6. IF a call returns an error response that has no Discriminator, or whose Discriminator does not resolve to a shape in the operation's Error_Registry, THEN THE Dynamic_Client SHALL throw the SDK's generic service exception (not a document-bearing modeled exception) carrying the error code, message, HTTP status code, and request ID that the response provides.

### Requirement 30: Operations as first-class objects and the shared pipeline

**User Story:** As a plugin author, I want dynamic and generated clients to run the same pipeline over the same operation abstraction, so that one interceptor or plugin works for both.

#### Acceptance Criteria

1. THE Dynamic_Client SHALL provide `getOperation(operationName)`, which returns an Api_Operation exposing the operation Schema, input Schema, output Schema, error Schemas, Error_Registry, and effective auth schemes, whose operation, input, output, and error ShapeIds and ordered effective auth scheme IDs equal those of the generated Api_Operation for the same operation in the same Smithy_Model.
2. THE Dynamic_Client and generated clients SHALL run calls through the same Request_Pipeline implementation, such that for the same operation, equivalent input, and the same client configuration, a registered `ExecutionInterceptor` receives the same sequence of hook invocations in the same order.
3. WHEN the Request_Pipeline invokes an interceptor hook or plugin for a call, from `beforeExecution` through `afterExecution` or `onExecutionFailure`, THE Request_Pipeline SHALL expose through the execution context the same Api_Operation instance that the Client_Protocol uses to serialize that call.
4. THE SDK runtime features that read operation or member metadata (idempotency token injection, request compression, checksums, host prefix, auth scheme resolution, and pagination) SHALL read that metadata only from Api_Operation Schemas and Runtime_Traits, and SHALL NOT inspect or type-check Generated_Model_Class types.
5. WHERE a feature needs an auxiliary operation (such as S3 Express `CreateSession`), THE feature SHALL take the means of reaching that operation as a required constructor or builder input, and SHALL NOT require that the auxiliary operation be generated or that the feature module depend on a generated client.
6. IF the operation name passed to `getOperation` is not an operation of the service, THEN THE Dynamic_Client SHALL throw an exception naming the operation and the service.
7. WHEN the Dynamic_Client and a generated client call the same operation with equivalent input and the same client configuration, THE SDK runtime features in criterion 4 SHALL produce the same observable request effects: an idempotency token populated when the input omits it, the same compression encoding of the payload, the same checksum values, the same host prefix on the endpoint, and the same selected auth scheme.
8. IF a feature that needs an auxiliary operation is constructed without its required dependency, THEN THE feature SHALL fail construction with an exception naming the missing dependency, before any client is built with it and before any request is sent.

### Requirement 31: Dynamic client streaming

**User Story:** As a tool developer, I want streaming operations to work through the dynamic client, so that it covers the same surface as generated clients.

#### Acceptance Criteria

1. WHEN an operation is called whose input Schema has a member targeting a blob with the `streaming` trait, THE Dynamic_Client SHALL accept a data-stream handle at that member of the input Document (`RequestBody` for the sync form, `AsyncRequestBody` for the async form) and bind it as the transport request body. It SHALL start passing bytes to the Client_Transport before the stream has been fully read.
2. WHEN an operation's output Schema has a member targeting a union with the `streaming` trait (an event stream), THE Dynamic_Client SHALL deliver each received event to the caller in wire order, as a schema-backed Document whose Schema is the target of the matching event-union member. It SHALL deliver each event without waiting for the event stream to end.
3. THE Dynamic_Client SHALL use the same Client_Protocol, Client_Transport, event-stream message framing, event signing, `org.reactivestreams.Publisher` backpressure, stream lifecycle, and stream error behavior as a generated client for the selected Client_Protocol. For the same operation and equivalent input, the request headers, body bytes, and event message frames that the Dynamic_Client produces SHALL equal those of the generated client, except for non-semantic metadata (user agent, invocation ID, timestamps, and signatures).
4. WHEN an operation is called whose output Schema has a member targeting a blob with the `streaming` trait, THE Dynamic_Client SHALL deliver the response body through the caller's `ResponseTransformer` (sync) or `AsyncResponseTransformer` (async), along with the non-streaming output members as a schema-backed Document for the output Schema. The transformer SHALL receive the first body bytes before the Client_Transport has read the whole response body.
5. WHEN an operation is called whose input Schema has an event stream member, including a bidirectional event stream where criterion 2 also applies, THE Dynamic_Client SHALL accept a `Publisher` of Documents at that member. It SHALL serialize each published Document as one event message, using the Schema of the event-union member identified by the Document's Discriminator, and SHALL send each event as the `Publisher` emits it, without waiting for the `Publisher` to complete.
6. IF a required streaming blob or event stream member is absent from the input Document, a streaming member holds a value that is not a data-stream handle or `Publisher` of the kind criterion 1 or 5 requires, or an operation with an event stream member is called through the sync form, THEN THE Dynamic_Client SHALL throw an exception naming the operation and the member, and SHALL NOT send a request.

---

**Part D: Cross-Cutting Requirements**

### Requirement 32: Smithy-driven protocol tests

**User Story:** As an SDK maintainer, I want protocol compliance tests generated from the Smithy protocol test models, so that every Client_Protocol is verified against the shared cross-SDK fixtures.

#### Acceptance Criteria

1. THE Protocol_Test_Generator SHALL generate exactly one JUnit 5 test for each `httpRequestTests` and `httpResponseTests` case in the Protocol_Test_Services whose `protocol` is a Supported_Protocol and whose `appliesTo` is `client` or unset. Each generated test SHALL have a display name that contains the test case `id`.
2. THE Protocol_Test_Generator SHALL generate the Protocol_Test_Services clients through the same Codegen_Pipeline as Prototype_Service_Set services.
3. WHEN a generated request test runs, THE test SHALL configure the client endpoint from the case `host`, invoke the operation with the case `params`, and capture the serialized request at the Client_Transport boundary without opening a network connection. It SHALL then assert that: the method equals `method`; the URI path equals `uri`; every `queryParams` entry is present; no `forbidQueryParams` key is present; every `requireQueryParams` key is present; the request host equals `resolvedHost` when `resolvedHost` is set; every `headers` entry is present with an equal value, with header names compared case-insensitively; no `forbidHeaders` name is present; and every `requireHeaders` name is present.
4. WHEN a generated request test case specifies `body`, THE test SHALL compare the captured request body against `body` according to `bodyMediaType`: JSON semantically (member order ignored), XML semantically (whitespace between elements ignored), CBOR by decoded data-model value, `application/x-www-form-urlencoded` by the set of decoded key-value pairs, and every other or unset media type byte for byte. An empty `body` SHALL match only an empty request body.
5. WHEN a generated response test runs, THE test SHALL give the client a response with the case `code`, `headers`, and `body` without opening a network connection. For a case on an operation output, it SHALL assert that the deserialized output equals the case `params` member by member. For a case on an error shape, it SHALL assert that the thrown exception is the Generated_Model_Class of that error shape and that its members equal the case `params`. Blobs SHALL be compared by bytes, timestamps by instant, and a NaN value SHALL equal NaN.
6. THE Protocol_Test_Generator SHALL read a skip list in which each entry names one test case `id` and a non-empty reason. It SHALL generate each listed case as a JUnit 5 test that is reported as skipped with that reason, not left out of the generated output.
7. IF a skip list entry names a test case `id` that does not exist in the Protocol_Test_Services, or has an empty reason, THEN THE Protocol_Test_Generator SHALL fail code generation with an error that names the offending entry.
8. IF a test case selected by criterion 1 is not in the skip list and the Protocol_Test_Generator cannot generate a test for it, THEN THE Protocol_Test_Generator SHALL fail code generation with an error that names the test case `id`, and SHALL NOT silently omit the case.
9. WHEN the SDK build runs its protocol test suite, THE SDK build SHALL run the generated protocol tests and SHALL NOT run Legacy_Protocol_Tests.
10. THE design SHALL list every Legacy_Protocol_Tests case that has no Smithy equivalent. A Smithy equivalent is a Protocol_Test_Services case for the same Supported_Protocol that uses equivalent operation input or response and asserts the same request or response properties. Each listed case SHALL have exactly one disposition: ported to a Smithy test trait, with the new test case `id` recorded, or retired, with a reason recorded.
11. WHEN the generated protocol tests run on the feature branch, THE test results SHALL report the pass, fail, and skip counts per Supported_Protocol, and SHALL count each generated test exactly once, under the Supported_Protocol named by its `protocol`.

### Requirement 33: Prototype service set

**User Story:** As a prototype owner, I want a small, protocol-diverse service set with minimal customizations, so that the prototype validates the design without generating the whole fleet.

#### Acceptance Criteria

1. THE Prototype_Service_Set SHALL consist of exactly these six services, each generated with schema-based serde: Amazon SQS (`awsJson1_0`), AWS Secrets Manager (`awsJson1_1`), AWS Account (`restJson1`), Amazon Route 53 (`restXml`), Amazon SNS (`awsQuery`), and Amazon CloudWatch (`rpcv2Cbor`). The only exceptions are replacements made under criterion 6 and the addition allowed by criterion 4.
2. THE generated Amazon CloudWatch client SHALL use `rpcv2Cbor` as its Client_Protocol. CloudWatch's `awsJson1_0` and `awsQuery` protocol traits SHALL be kept in its Smithy_Model.
3. THE Protocol_Test_Services SHALL cover the `ec2Query` Supported_Protocol through the generated protocol tests of Requirement 32. Amazon EC2 SHALL NOT be generated with schema-based serde as part of the prototype.
4. WHERE end-to-end performance benchmarks run, THE Prototype_Service_Set SHALL also include Amazon DynamoDB, generated with schema-based serde. The design SHALL compare the DynamoDB benchmark results with the results recorded on branch `feature/poc/racecar`.
5. WHEN a Prototype_Service_Set service is generated, THE Codegen_Pipeline SHALL take its input from the Smithy model at `.kiro/reference/aws-models/<service>/smithy/model.json`, or from the service's C2J model converted by the C2J_Converter. The design SHALL record which of the two sources each service uses.
6. IF Codegen fails for a Prototype_Service_Set service, or the generated code fails to compile on Java 8, THEN THE design SHALL record the blocking issue and name one replacement service. The replacement SHALL use the same Supported_Protocol, SHALL have no `customization.config` or one of 1,024 bytes or less, SHALL NOT be Amazon EC2, and SHALL meet criteria 5 and 8.
7. IF a `customization.config` entry of a Prototype_Service_Set service conflicts with schema codegen, THEN THE Codegen SHALL generate the service with that entry disabled. The design SHALL list each disabled entry by service, with the reason.
8. WHEN a Prototype_Service_Set service is generated, THE unit and functional tests that exist for that service's SDK module in Baseline_Build SHALL run with zero failures and zero errors. The only exceptions are tests that the design lists by test name, each with a reason.
9. THE Prototype_Service_Set together with the Protocol_Test_Services SHALL cover every Supported_Protocol. Each Supported_Protocol other than `ec2Query` SHALL be the Client_Protocol of at least one generated Prototype_Service_Set service.

### Requirement 34: Performance

**User Story:** As an SDK user, I want schema-based serde and the new pipeline to be at least as fast as today's generated code, so that the architecture change does not cost me latency or memory.

#### Acceptance Criteria

1. THE Benchmark_Harness SHALL measure request marshalling and response unmarshalling for each Supported_Protocol used by the Prototype_Service_Set.
   - Marshalling runs from the input POJO to the HTTP request and excludes signing and transport. Unmarshalling runs from the HTTP response bytes to the output POJO and excludes transport.
   - Each is measured in µs/op and in bytes allocated per op.
   - Each protocol needs at least one small-payload operation (serialized body of 1 KB or less) and one large-payload operation (serialized body of 100 KB or more).
   - Where the protocol supports them, each payload includes nested structure, list, map, timestamp, and blob members.
2. THE Benchmark_Harness SHALL measure end-to-end DynamoDB GetItem, PutItem, BatchGetItem, and BatchWriteItem using the paired A/B methodology from `pipeline_benchmark2/project_racecar_summary.md` on branch `feature/poc/racecar`. For any of these operations with a request body of 100 KB or more, it SHALL report request-marshalling time separately from SigV4 payload SHA-256 time, so the serde comparison covers serialization cost only.
3. THE schema-based marshalling and unmarshalling SHALL allocate, for each benchmarked operation in criteria 1 and 2, no more bytes per op than the Baseline_Build mean plus that operation's allocation resolution floor from the null experiment in criterion 9. The comparison uses the mean across all repetitions.
4. THE schema-based marshalling and unmarshalling SHALL take, for each benchmarked operation in criteria 1 and 2, no more than 1.05 times the Baseline_Build mean µs/op. The comparison uses the mean across all repetitions.
5. THE performance report SHALL compare the feature build with the Baseline_Build, the `feature/poc/racecar` optimized build, and smithy-java for each benchmarked operation in criteria 1 and 2. Each entry SHALL state:
   - the mean, the JMH error bound, and the repetition count
   - the ratio to the Baseline_Build
   - the null-experiment resolution floor
   - the JDK version and host used, with all arms of one comparison run on the same JDK version and host
6. WHEN a Shape_Serializer or Shape_Deserializer processes a Serializable_Struct whose Schema is already initialized, THE serde hot path SHALL:
   - make no calls to `java.lang.reflect` APIs
   - do no trait lookups that hash a trait class or ShapeId (Trait_Key-indexed lookups only)
   - allocate no capturing lambdas per member

   This is verified by a profiler allocation and call trace of the criterion 1 operations that shows none of these calls or allocations.
7. THE Benchmark_Harness SHALL measure client construction time and first-call latency for each Prototype_Service_Set service.
   - Client construction time runs from the start of the client builder's `build()` call to its return.
   - First-call latency is the first operation call against a local stub endpoint that returns a fixed response.
   - Each sample runs in a fresh JVM process, with at least 10 samples per build.
   - THE performance report SHALL compare the mean of each measurement with the Baseline_Build mean and give the ratio.
8. THE design SHALL evaluate smithy-java's runtime bytecode generation of codecs (`RuntimeCodegenMode`) as an optional Java 8-compatible optimization. It SHALL record the decision (adopt, defer, or reject) together with the µs/op and bytes/op measured on Java 8 for the criterion 1 operations, or the reason those measurements could not be taken.
9. THE Benchmark_Harness SHALL run every A/B comparison in criteria 2, 3, 4, 5, and 7 as follows:
   - at least 3 interleaved repetitions of each arm (A, B, A, B order)
   - all arms on the same host and JDK version
   - a null experiment that runs the same build in both arms, to set each operation's µs/op and bytes/op resolution floor
10. IF the difference between two arms for an operation is no larger than that operation's null-experiment resolution floor, THEN THE performance report SHALL record the result as "no measurable difference". IF a feature-build result exceeds the threshold in criterion 3 or criterion 4, THEN THE performance report SHALL flag the operation as a regression with the measured ratio, and that criterion SHALL be counted as failed for that operation.
11. IF an operation's null-experiment µs/op resolution floor is greater than 5%, THEN THE performance report SHALL mark that operation's criterion 4 result as inconclusive rather than passed, and record the floor value.

### Requirement 35: Generating HTTP responses from output types

**User Story:** As an SDK user writing tests, I want to turn an output object or modeled exception into the HTTP response a service would send, so that I can mock SDK clients without hand-writing wire bytes.

#### Acceptance Criteria

1. WHEN the Response_Generator is given an operation output Generated_Model_Class and one of the Supported_Protocols, THE Response_Generator SHALL produce a transport response whose status code, headers, and body follow that protocol's rules and the output's HTTP binding traits. It SHALL build the response using only Schemas and Http_Binding_Serde, without per-shape generated marshallers.
2. WHEN the Response_Generator is given a modeled exception and one of the Supported_Protocols, THE Response_Generator SHALL produce the protocol's error response. That response SHALL contain (a) the status code from the exception's `httpError` trait, or the protocol's default status for the exception's fault when the trait is absent, (b) the exception's Discriminator in the location the protocol defines, and (c) a body that carries the exception's modeled members.
3. FOR ALL operation output values, schema-backed Document output values, and modeled exception values in the Prototype_Service_Set, and for each Supported_Protocol the owning service declares, deserializing the Response_Generator output with the matching Client_Protocol SHALL produce a value equal to the input (round-trip property). For modeled exceptions, this means the deserialized exception is of the same type as the original and has equal member values.
4. THE SDK SHALL provide a Mock_Plugin that accepts queued items of four kinds: output Generated_Model_Class values, schema-backed Documents, modeled exceptions, and raw HTTP responses. It SHALL return them to client calls in first-in, first-out order, and each item SHALL be consumed by exactly one call.
5. WHEN a client configured with the Mock_Plugin makes a call and the next queued item is an output value, a schema-backed Document, or a modeled exception, THE Mock_Plugin SHALL turn the item into a response with the Response_Generator, using the client's resolved Client_Protocol. It SHALL then return that response through the Client_Protocol deserialization path of the Request_Pipeline without sending a network request. A queued modeled exception SHALL reach the caller as a thrown exception of the same modeled type.
6. WHEN a client configured with the Mock_Plugin makes a call and the next queued item is a raw HTTP response, THE Mock_Plugin SHALL pass that response unchanged, without calling the Response_Generator, to the Client_Protocol deserialization path. It SHALL not send a network request.
7. IF a client configured with the Mock_Plugin makes a call while the queue is empty, THEN THE Mock_Plugin SHALL fail the call with an exception indicating that the mock queue is exhausted, and it SHALL not send a network request.
8. IF the next queued output value, schema-backed Document, or modeled exception has a Schema that is neither the called operation's output Schema nor one of the operation's error Schemas, THEN THE Mock_Plugin SHALL fail the call with an exception that names both the expected ShapeIds and the queued item's ShapeId. The mismatched item SHALL be removed from the queue.
9. THE Mock_Plugin SHALL work with generated sync clients, generated async clients, and the Dynamic_Client. For the same queued item and operation, all three client kinds SHALL return equal output values, or throw exceptions of the same modeled type with equal member values.
10. THE Response_Generator and Mock_Plugin SHALL be published in a test-scoped module that generated clients do not depend on at runtime. A generated client module's runtime dependency tree SHALL not contain this module.

### Requirement 36: Artifact size

**User Story:** As an SDK user who deploys to size-constrained environments, I want schema generation to offset its size cost by removing duplicated code, so that client JARs do not grow.

#### Acceptance Criteria

1. THE Size_Report SHALL record these values for each Prototype_Service_Set service, once for the Baseline_Build and once for the feature build:
   - the size in bytes of the compressed main JAR file, with sources and javadoc JARs excluded
   - the total uncompressed class-file bytes
   - the number of `.class` entries, with nested, anonymous, and synthetic classes included
   - the uncompressed class-file bytes for each Java package
2. THE Size_Report SHALL break down each Prototype_Service_Set service's change in uncompressed class-file bytes into seven categories: Schemas, Type_Registries, Generated_Model_Class classes, client classes, removed marshallers and unmarshallers, removed exception-mapping code, and other. Each class SHALL be assigned to exactly one category, and the category deltas SHALL add up to that service's total change in uncompressed class-file bytes.
3. THE feature build's compressed main JAR file for each Prototype_Service_Set service SHALL be no larger in bytes than the Baseline_Build's compressed main JAR file for that service.
4. THE generated sync client and the generated async client of each Prototype_Service_Set service SHALL get operation metadata (Api_Operation instances) and error resolution (Error_Registry) from the same single generated per-service class. Neither client class SHALL contain its own per-operation metadata or exception-mapping code.
5. THE design SHALL list each further size-reduction opportunity found during the prototype. For each one, the list SHALL give the category from criterion 2 that it affects, the Prototype_Service_Set services it applies to, and its estimated change in compressed main JAR bytes per affected service.
6. THE Size_Report SHALL record, for both builds, the size in bytes of the compressed JAR of each runtime module the feature build adds or changes, including the Runtime_Schema_Module. A module that is absent from the Baseline_Build SHALL be recorded as 0 bytes for that build. These module sizes SHALL be reported separately from the per-service totals.
7. THE Size_Report SHALL record the JDK version, the compiler options, and the Codegen customizations disabled for each Prototype_Service_Set service in the feature build. THE Baseline_Build and the feature build SHALL be compiled with the same JDK version and the same compiler options.
8. IF the feature build's compressed main JAR file for a Prototype_Service_Set service is larger than the Baseline_Build's, THEN THE Size_Report SHALL mark that service as failing criterion 3 and SHALL list the criterion 2 categories that have a net byte increase for that service.

### Requirement 37: Request pipeline rewrite

**User Story:** As a performance-focused maintainer, I want the request pipeline rewritten around Api_Operation and Client_Protocol using the racecar findings, so that dynamic clients are supported and per-call framework cost goes down.

#### Acceptance Criteria

1. THE design SHALL list every optimization recorded on branch `feature/poc/racecar` (phases A through H11, including the straight-line pipeline of phase G). For each one it SHALL record exactly one decision (adopted, superseded, or rejected) and a reason. For adopted items it SHALL also name the Request_Pipeline component that implements the optimization. For superseded items it SHALL name the replacing mechanism.
2. THE Request_Pipeline SHALL serialize requests only through Api_Operation and Client_Protocol, send them only through Client_Transport, and deserialize responses only through Client_Protocol. It SHALL have no compile-time or runtime dependency on generated marshaller or unmarshaller types.
3. WHEN a Dynamic_Client call is made, THE SDK SHALL run it through the same Request_Pipeline implementation that generated clients use, with an Api_Operation built from the Smithy_Model at runtime.
4. WHEN a client is built, THE Request_Pipeline SHALL construct its stage structure exactly once for that client configuration. WHEN a call is made on that client, THE Request_Pipeline SHALL reuse that stage structure without constructing any stage objects.
5. THE Request_Pipeline SHALL invoke `ExecutionInterceptor` hooks in the same order as the Baseline_Build, for both successful and failed calls. Each hook's `Context` SHALL expose the same request, HTTP request, HTTP response, and response objects, and the same `ExecutionAttributes`, as in the Baseline_Build. The existing interceptor ordering and interceptor functional tests SHALL pass unmodified.
6. THE sync and async Request_Pipelines SHALL call the same stage implementations for serialization, endpoint resolution, auth resolution, signing, retry decisions, and deserialization. Async-only code SHALL be limited to scheduling and future completion.
7. WHEN the sync and async Request_Pipelines get the same input and the same transport response or failure sequence, THEY SHALL produce equal outputs, or exceptions of the same type, and SHALL make the same number of attempts.
8. IF Client_Protocol serialization, Client_Transport, or Client_Protocol deserialization throws during a call, THEN THE Request_Pipeline SHALL pass the exception to retry decisions. Once retries are exhausted or the exception is non-retryable, it SHALL invoke `onExecutionFailure` once and raise the same SDK exception type to the caller as the Baseline_Build, with the original exception kept as the cause.
9. WHEN the Benchmark_Harness runs end-to-end DynamoDB GetItem, PutItem, BatchGetItem, and BatchWriteItem with the paired A/B methodology of Requirement 34, THE rewritten Request_Pipeline SHALL allocate no more bytes per op than the `feature/poc/racecar` optimized build for each operation. Both builds SHALL use the same stubbed transport, payloads, JDK, and JVM options, so that the bytes measured come only from framework code.
10. THE design SHALL compare the rewritten Request_Pipeline with the smithy-java bridge results in `pipeline_benchmark3/results.md` on branch `feature/poc/racecar`. The comparison SHALL report µs/op and bytes allocated per op for each operation in that file, and SHALL give a cause for each metric that differs by more than 5%.

### Requirement 38: Documents and type registries in higher-level libraries

**User Story:** As an SDK user of higher-level libraries, I want libraries built on schemas, documents, and type registries, so that user-defined message payloads and event formats get first-class typed serde.

#### Acceptance Criteria

1. THE Library_Integration_Analysis SHALL contain exactly one entry for each of these seven subjects: the S3 Event Notifications library, the SNS Message Manager, SQS message bodies with user-defined schemas, AWS Lambda event handler payloads, DynamoDB Enhanced Client documents and table schemas, the IAM Policy Builder, and modeled string members with `mediaType` `application/json`. Each entry SHALL state one recommendation from the set {prototype in phase 3, defer to a later phase, do not adopt}, with a rationale.
2. FOR EACH entry in the Library_Integration_Analysis, THE Library_Integration_Analysis SHALL state: (a) the current serde approach, naming the parser or writer the library uses today; (b) the proposed use of Schemas, Documents, or Type_Registries; (c) the compatibility impact, classified as none, additive, or breaking for the library's public API, and listing every affected public type; (d) the expected artifact-size effect and (e) the expected performance effect. Items (d) and (e) SHALL each give a direction (increase, decrease, or neutral) plus either an estimate or the Size_Report or Benchmark_Harness measurement that will confirm it.
3. THE design SHALL include a typed-payload API for SQS and SNS message bodies. The design SHALL define (a) a serialize operation that takes a Serializable_Shape value and a Codec and returns the message body string, and (b) a deserialize operation that takes a message body string, a Codec, and either a Shape_Builder or a Type_Registry, and returns the typed value. When a Type_Registry is supplied, the design SHALL state how the concrete shape's ShapeId is determined: caller-supplied, or read from a Discriminator in the body. The typed-payload API SHALL be additive only. It SHALL NOT remove or change the signature of any existing public SQS, SNS, or SNS Message Manager API that takes or returns a message body as a string.
4. WHERE the S3 Event Notifications library is prototyped on schemas, THE library SHALL pass every test in its existing test suite without modifying the tests. Every S3 event notification JSON sample in that suite SHALL parse into an `S3EventNotification` that is equal, per `equals`, to the value the current implementation produces. This includes samples with null, empty, and absent `Records`, unknown extra fields, and `awsGeneratedTags` entries with null or empty values.
5. IF a message body passed to the typed-payload deserialize operation is not well-formed for the supplied Codec, does not conform to the target Schema, or names a Discriminator that is not registered in the supplied Type_Registry, THEN THE design SHALL specify that the operation throws an exception identifying which of the three causes occurred and returns no partially populated value.
6. WHERE the S3 Event Notifications library is prototyped on schemas, THE library SHALL keep its existing public API (`fromJson` from String, byte array, and InputStream; `toJson`; `toJsonPretty`) unchanged in signature. IF the input is malformed JSON or an `awsGeneratedTags` value has the wrong type, THEN THE library SHALL throw the same exception type as the current implementation.

### Requirement 39: Shape serde outside operations

**User Story:** As an SDK user, I want to serialize any SDK model type to any codec outside an operation call, so that I can persist and exchange SDK types in standard formats.

#### Acceptance Criteria

1. WHEN a caller passes a Generated_Model_Class instance (a structure, union, or modeled exception) to the serialize convenience method of the JSON, CBOR, or XML Codec, THE Codec SHALL return the serialized bytes. These bytes SHALL be byte-for-byte identical to the bytes produced by writing the same instance through a Shape_Serializer created by the same Codec instance with the same Codec_Settings.
2. WHEN a caller passes bytes and a Shape_Builder to the deserialize convenience method of the JSON, CBOR, or XML Codec, THE Codec SHALL return the typed value built by that Shape_Builder. This value SHALL be equal to the value produced by reading the same bytes through a Shape_Deserializer created by the same Codec instance into a new Shape_Builder of the same type.
3. THE Codec convenience methods SHALL complete using only the Codec and its Codec_Settings, with no client instance, Client_Protocol, Client_Transport, or Request_Pipeline, and with no network access.
4. IF the bytes passed to the deserialize convenience method are truncated, are malformed for the Codec's format, or do not conform to the Shape_Builder's Schema, THEN THE Codec SHALL throw a `SerializationException` that names the Schema ShapeId, consistent with Requirement 8 criterion 7, and SHALL return no value.
5. IF a null value, null bytes, or a null Shape_Builder is passed to a Codec convenience method, THEN THE Codec SHALL throw a `NullPointerException` that names the null parameter and SHALL write or build no output.
6. WHILE 2 or more threads call the convenience methods on one Codec instance at the same time, THE Codec SHALL give each caller the same result that a single-threaded call with the same arguments returns.

### Requirement 40: Observability and debugging

**User Story:** As an SDK user debugging a call, I want schema-aware logging that redacts sensitive data, so that logs are useful and safe.

#### Acceptance Criteria

1. WHEN `toString` is called on a Generated_Model_Class instance, THE Generated_Model_Class SHALL return a string produced by writing the instance through `ToStringSerializer` using the instance's Schema. The string SHALL contain the shape name followed by each set member's name and value, in Schema member order.
2. WHEN `ToStringSerializer` writes a set member whose Member_Schema has the `sensitive` trait, either on the member or on its target shape, THE `ToStringSerializer` SHALL write the redaction placeholder used by Baseline_Build in place of the value, and the returned string SHALL contain no part of that value. This applies at every nesting depth, including structure and union members, list elements, and map keys and values.
3. IF a Generated_Model_Class member is unset (null) or targets a streaming blob or event stream, THEN THE `ToStringSerializer` SHALL leave that member out of the returned string and SHALL NOT read from or consume any stream.
4. WHILE DEBUG level is enabled for the SDK request logger, WHEN the Request_Pipeline selects the Client_Protocol for a call, THE Request_Pipeline SHALL log exactly one DEBUG entry for that call. The entry SHALL contain the operation's ShapeId_Runtime and the selected Client_Protocol ShapeId (for example, `aws.protocols#restJson1`), and SHALL be written through `software.amazon.awssdk.utils.Logger`.
5. THE Request_Pipeline SHALL NOT include any operation input or output member values in the log entry required by criterion 4.

### Requirement 41: Testing strategy

**User Story:** As an SDK maintainer, I want each layer verified with the appropriate kind of test, so that correctness is established independently of the SDK's own serde round trips.

#### Acceptance Criteria

1. THE ported Runtime_Schema_Module, codec, and Client_Protocol types SHALL have a ported counterpart, compiled for Java 8 and run with JUnit 5, for every test method in each smithy-java unit test class whose subject type is listed in the Porting_Map. The Porting_Map SHALL list each smithy-java test method that is not ported, with the reason it was not ported (for example, it depends on a JDK 9+ API with no Java 8 equivalent).
2. THE round-trip properties in Requirements 2.2, 6.8, 8.6, 19.8, 21.2, and 35.3 SHALL be verified with jqwik property-based tests, each over the value domain that its source criterion states. The minimum number of generated values per run SHALL be:
   - Requirements 2.2 and 21.2: at least 1,000 values for each property.
   - Requirements 6.8, 8.6, and 19.8: at least 100 values for each structure, union, and modeled exception Generated_Model_Class in the Prototype_Service_Set, for each of the JSON, CBOR, and XML Codecs where the source criterion is per Codec.
   - Requirement 35.3: at least 100 values for each output and modeled exception Generated_Model_Class in the Prototype_Service_Set, for each Supported_Protocol that the owning service models.
3. THE wire-format correctness of each Client_Protocol SHALL be verified by the generated protocol tests (Requirement 32). Each protocol test SHALL assert against the expected request, response, and `params` values stated in the Smithy test case, not against values produced by the SDK's own serializer or deserializer. Round-trip property tests and Mock_Plugin tests SHALL NOT count as evidence of wire-format correctness.
4. THE Dynamic_Client SHALL have at least one test, with the case ID in its name, for each behavioral case DC-1 through DC-30 in the Testing section of the Dynamic Client SEP. Each test SHALL use that SEP's reference model with the case's "Given" traits applied, and SHALL assert the case's "Expect" outcome. The design SHALL list any case that is not implemented, with its ID and the reason.
5. WHEN the SDK build runs on the feature branch, THE tests in criteria 1–4 and the existing test suites of every SDK module whose main or test sources differ from the Baseline_Build SHALL complete with zero failures and zero errors. Two kinds of case are excepted. The first is Legacy_Protocol_Tests cases replaced under Requirement 32. The second is test cases that the design lists by test class, test method, and reason.
6. THE property-based test generators in criterion 2 SHALL include the following in their value domain:
   - absent and present values for each optional member, and exactly one set member for each union
   - empty and non-empty strings, blobs, lists, and maps, with collection sizes from 0 to 8 elements
   - strings of 0 to 256 characters, including characters outside ASCII
   - the minimum, maximum, and zero values of each integral type, plus NaN and positive and negative infinity for float and double
   - timestamps from 1970-01-01T00:00:00Z through 9999-12-31T23:59:59Z, at the precision of the member's effective timestamp format
   - recursive shapes nested to at most 4 levels
7. IF a property-based test in criterion 2 fails, THEN THE test report SHALL include the random seed and the shrunk failing value, so that rerunning the property with that seed reproduces the failure.

### Requirement 42: Design document structure

**User Story:** As a reviewer, I want the design organized in the same dependency order as these requirements, so that each layer can be reviewed and built in sequence.

#### Acceptance Criteria

1. THE design document SHALL present exactly four top-level parts in this order: schema decoupling and schema-based serde (Requirements 1–17), Document types and Type_Registries (Requirements 18–24), Dynamic_Client (Requirements 25–31), and cross-cutting concerns (Requirements 32–42).
2. THE design document SHALL include the Porting_Map as a table with one row per ported smithy-java type. Each row SHALL contain the source fully qualified name, target SDK module, target package, target class name, SDK visibility annotation (Requirement 1), and the deviation with its reason, or "none". The rows SHALL be grouped under one heading per target SDK module, and each ported type SHALL appear in exactly one row.
3. THE design document SHALL include a numbered, phased implementation order that keeps the relative order of steps 1–9 of the suggested implementation order in the Serialization and Schema Decoupling SEP. In this order, the first phase SHALL deliver the Schema, Schema_Generator output for the Protocol_Test_Services, and the Protocol_Test_Generator.
4. THE design document SHALL list, for each design element (each section, component, module, and Porting_Map row), the numbers of the requirements in the range 1–42 that the element satisfies.
5. THE design document SHALL define each SDK module, runtime type, and Codegen component in or before the first section that references it, so no section depends on a definition in a later section.
6. THE design document SHALL state, for each phase of the implementation order, the requirement numbers it delivers, the Supported_Protocols and Prototype_Service_Set services it covers, and the tests that must pass before the next phase starts. IF a phase departs from the SEP order, THEN the design document SHALL state the departure and its reason in that phase.
7. THE design document SHALL trace each of Requirements 1–42 to at least one design element. IF a requirement has no tracing design element, THEN the design document SHALL list that requirement as untraced in a traceability summary.
