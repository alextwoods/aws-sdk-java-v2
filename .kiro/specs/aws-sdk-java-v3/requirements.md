# Requirements Document

## Introduction

This document defines requirements for a new major version of the AWS SDK for Java. AWS SDK for Java V3 is a JDK 21-or-later, Maven-built SDK generated directly from canonical Smithy models. V3 uses Smithy-native schemas and runtime components, contains no SDK-specific intermediate model, provides generated and dynamic clients over one AWS request pipeline, and prioritizes correctness, migration compatibility, and maximum measured performance.

This document does not define implementation tasks or an implementation design. The **Verified Landscape Facts** section records observations from the inspected V2 repository, proof-of-concept branches, and reference material. The **Requirements** section is normative.

## Glossary

- **AWS Codegen Package**: The single V3 Maven artifact that contains AWS-specific Smithy code-generation integrations, naming rules, documentation generation, service customizations, and generated-client emitters.
- **Backpressure**: A streaming contract in which a consumer controls the quantity of data requested from a producer.
- **BOM**: A Maven bill of materials that aligns dependency versions without adding runtime classes.
- **C2J**: The legacy AWS service JSON model and sidecar formats consumed by V2 code generation.
- **Codec**: A runtime encoder and decoder for a concrete data format such as JSON, XML, or CBOR.
- **Common Fork-Join Pool**: The JVM-wide default `ForkJoinPool` used by asynchronous Java APIs when no executor is supplied.
- **Event Loop**: An HTTP Implementation thread responsible for non-blocking network progress.
- **Event Stream**: A modeled bidirectional or unidirectional sequence of framed events carried by a streaming operation.
- **JDK**: A Java Development Kit used to compile and run V3.
- **JIT**: The JVM just-in-time compiler that optimizes code during execution.
- **JVM**: The Java Virtual Machine running a V3 application.
- **Migration Tooling**: The V3 Maven-delivered source transformation and diagnostic system for V2 applications.
- **Schema Fidelity Test**: An automated comparison between a generated Schema and the source Canonical Smithy Model shape.
- **Telemetry SPI**: The optional service-provider contract for receiving V3 metrics and traces.
- **AWS Feature Plugin**: A runtime component that derives behavior from Smithy schemas and traits rather than from generated service classes.
- **AWS Pipeline**: The single SDK-owned request execution pipeline used by generated and Dynamic Clients for serialization, endpoint resolution, authentication, transmission, retries, deserialization, interception, telemetry, and error mapping.
- **Async Client**: A generated non-blocking service client whose operation methods return `CompletableFuture` values and whose streaming operations use asynchronous body and response abstractions.
- **Canonical Smithy Model**: The authoritative Smithy model for an AWS service, including service, operation, shape, trait, endpoint, auth, pagination, waiter, example, and customization information required by V3.
- **Client Configuration**: Immutable resolved settings shared by calls from one client, including region, endpoint, identity, auth, retry, timeout, HTTP, interceptor, telemetry, protocol, and service settings.
- **Compatibility Classification**: One of `PRESERVED`, `ADAPTED`, or `EXCLUDED`, assigned to each V2 public API or behavior in the Parity Manifest.
- **Compatibility Layer**: V3 APIs, adapters, and migration tooling that preserve or translate supported V2 source, binary, configuration, and behavioral contracts.
- **Document**: A runtime representation of any finite Smithy data-model value, including blob and timestamp values, with optional shape-discriminator and protocol context.
- **Dynamic Client**: A client that accepts a Smithy model at runtime and invokes operations by name without generated service classes.
- **Execution Context**: Per-call and per-attempt typed state shared by AWS Pipeline components and Interceptors.
- **Generated Service Package**: A Maven artifact containing generated service clients, models, schemas, operation descriptors, exceptions, paginators, waiters, utilities, and registries for one AWS service.
- **Generated Type**: A generated immutable Java representation of a Smithy structure, union, enum, or error shape.
- **HTTP Implementation**: A V3 transport artifact backed by a specific HTTP engine, including Apache HTTP Client 4, Apache HTTP Client 5, JDK URLConnection, Netty NIO, or AWS Common Runtime when the corresponding V2 artifact is supported at the V3 feature freeze.
- **HTTP SPI**: The V3 public service-provider contract between the AWS Pipeline and HTTP Implementations.
- **Intermediate Model**: An SDK-specific whole-service object graph or serialized artifact that duplicates, flattens, or translates the Smithy semantic model for downstream code generators. Local symbols, indexes, and short-lived views over a Smithy `Model` are not an Intermediate Model when the local objects do not become a second semantic source of truth.
- **Interceptor**: A user-supplied or SDK-supplied component that observes or modifies modeled requests, transport requests, transport responses, modeled responses, exceptions, and Execution Context at documented AWS Pipeline lifecycle points.
- **Launch Parity Baseline**: The latest stable AWS SDK for Java V2 release selected at V3 feature freeze.
- **Maven Build**: The supported V3 contributor and release build driven by Maven reactor projects and Maven plugins.
- **Migration Corpus**: A versioned set of representative real-world and synthetic V2 applications used to evaluate V3 source, binary, and behavioral migration.
- **Modern Auth**: Smithy Reference Architecture auth-scheme resolution and signing, including currently supported SigV4, SigV4a, S3 Express, event-stream, bearer-token, and modeled anonymous-auth behaviors.
- **Modern Retry**: Token-based standard and adaptive retry strategies conforming to the current Smithy Reference Architecture behavior.
- **Parity Manifest**: A versioned inventory of V2 public APIs, supported behaviors, services, high-level libraries, HTTP Implementations, configuration sources, and extension points, with a Compatibility Classification, rationale, migration action, and verification evidence for each entry.
- **Performance Harness**: The reproducible benchmark system used to measure application CPU time, allocation, latency, throughput, cold start, artifact size, class loading, and streaming memory.
- **Performance Reference**: For each benchmark scenario, the best valid result among the Launch Parity Baseline, the inspected `feature/poc/racecar` implementation, and the pinned Smithy Java reference implementation, measured by the same Performance Harness and JDK.
- **Protocol Runtime**: Schema-driven runtime logic for HTTP bindings, codecs, event framing, request serialization, response deserialization, and protocol error handling.
- **Request Override**: Immutable settings applied to one operation invocation with higher precedence than Client Configuration.
- **Schema**: A lightweight runtime descriptor of a Smithy shape, member graph, shape identifier, shape type, and runtime-relevant traits.
- **Schema SERDE**: Serialization and deserialization driven by Schema and protocol runtime components rather than generated protocol-specific marshallers or reflection.
- **SerializableStruct**: The Smithy Java runtime contract implemented by generated structure, union, request, response, and modeled-error values so a schema-driven serializer can read members without reflection.
- **Statistically Valid Result**: A benchmark comparison with at least five interleaved repetitions, a same-artifact null experiment, steady-state verification, no failed operations, and a 95% confidence interval that does not cross the applicable regression boundary.
- **Sync Client**: A generated blocking service client whose ordinary operation methods return modeled responses and whose streaming operations use synchronous body and response abstractions.
- **Type Registry**: A composable mapping from Smithy shape identifiers to schemas, generated Java types, and factories for deserialization.
- **V2-Compatible Surface**: A V2 public type, method, builder method, overload, exception, configuration behavior, or extension point classified as `PRESERVED` or `ADAPTED` in the Parity Manifest.
- **V3 SDK**: The complete AWS SDK for Java V3 product, including generated artifacts, runtime artifacts, high-level libraries, HTTP Implementations, migration tooling, tests, documentation, and release automation.
- **Wire Equivalent**: Identical protocol-significant method, URI, headers, body, event frames, status interpretation, and modeled values after excluding documented nondeterministic metadata such as dates, invocation identifiers, signatures, and user-agent version strings.

## Verified Landscape Facts

The following facts are observations, not V3 requirements. Branch-specific facts describe the inspected revisions and do not imply production readiness.

| ID | Verified observation | Evidence inspected |
|---|---|---|
| LF-1 | The current V2 reactor is Maven-built, reports version `2.55.6-SNAPSHOT`, and targets Java 8 through `jre.version`. | `pom.xml` |
| LF-2 | V2 code generation uses JavaPoet generators over an SDK-specific `IntermediateModel` containing processed operations, shapes, endpoint data, paginators, waiters, retry configuration, and customizations. | `docs/guidelines/code-generation-guidelines.md`; `codegen/.../IntermediateModel.java` |
| LF-3 | V2 generated clients expose separate sync and async interfaces. Sync methods return modeled values; async methods return `CompletableFuture`; both provide request-builder consumer overloads; streaming APIs use sync or async body/response abstractions. | Generated `services/s3/target/generated-sources/.../S3Client.java` and `S3AsyncClient.java` |
| LF-4 | V2 exposes separate sync and async HTTP SPIs and a multi-stage `ExecutionInterceptor` API with request, HTTP message, response, exception, and failure hooks. | `http-client-spi/.../SdkHttpClient.java`; `SdkAsyncHttpClient.java`; `core/sdk-core/.../ExecutionInterceptor.java` |
| LF-5 | V2 Client Configuration is an immutable typed option map with merge, lazy option, resource ownership, client override, and request override behavior. | `core/sdk-core/.../SdkClientConfiguration.java`; V2 generated clients and builders |
| LF-6 | V2 runtime modules separately implement auth, identity, profiles, regions, protocols, metrics, endpoints, retries, and checksums. V2 ships Apache 4, Apache 5, URLConnection, Netty NIO, and AWS Common Runtime HTTP artifacts. | `core/pom.xml`; `http-clients/pom.xml` |
| LF-7 | V2 custom libraries include DynamoDB Enhanced sync and async clients, S3 Transfer Manager, IAM policy builder, S3 event notifications, and SNS message manager. | `services-custom/pom.xml`; module READMEs |
| LF-8 | The inspected AWS models repository stores a `smithy/model.json` beside current service-specific JSON artifacts for service directories. | `.kiro/reference/aws-models` target repository tree |
| LF-9 | Smithy Java requires Java 21 or later. Its code generator consumes Smithy `Model` and directed generation contexts directly, while runtime modules provide schemas, `SerializableStruct`, codecs, protocols, plugins, retries, endpoints, auth, HTTP transports, waiters, metrics, and a Dynamic Client. | `.kiro/reference/smithy-java/README.md`; `settings.gradle.kts`; codegen and runtime source |
| LF-10 | Smithy Java generated structures implement `SerializableStruct`, expose a Schema, serialize members through `ShapeSerializer`, and support member access by Schema. Smithy Java Schemas expose member indexes, target schemas, traits, builders, shape classes, and extension storage. | Smithy Java `SerializableStruct.java` and `Schema.java` |
| LF-11 | The Smithy Java Dynamic Client accepts a runtime Smithy model, resolves a service, invokes operations by string name with Document values, supports request overrides, and supports streaming blob and event-stream values without buffering streams into finite Documents. | Smithy Java `DynamicClient.java`; `.kiro/reference/Dynamic-Client.md` |
| LF-12 | The schema-decoupling reference requires generated schemas, format-neutral shape serialization, runtime-selectable protocols, no required DOM or reflection path, and round-trip testing. The paired Document reference requires blob and timestamp Documents, discriminators, coercion, package-level Type Registries, and registry composition. | `.kiro/reference/serialization-schema-decoupling.md`; `.kiro/reference/document-types-and-type-registries.md` |
| LF-13 | The Dynamic Client reference defines generated, trimmed, and dynamic projections over shared protocol, auth, endpoint, retry, telemetry, plugin, and request-pipeline behavior. | `.kiro/reference/Dynamic-Client.md` |
| LF-14 | The Racecar branch demonstrated that straight-line pipeline execution, generated direct JSON SERDE, reduced copying, compact headers and attributes, cached client-constant work, and optimized signing can materially reduce V2 CPU and allocation. Its summary reports representative DynamoDB application CPU reductions of approximately 55% to 69% from stock V2. | `feature/poc/racecar:pipeline_benchmark2/project_racecar_summary.md` |
| LF-15 | Racecar measurements identify the HTTP engine and asynchronous transport/future machinery as dominant remaining costs after pipeline optimization, and show that per-client caching and inactive-interceptor filtering avoid measurable per-call work. | Racecar summary, async execution report, and bridge comparison report |
| LF-16 | The full Smithy Java bridge prototype showed schema-driven performance potential, but reconstructing Smithy traits from C2J-derived metadata caused multiple silent wire differences across only two exercised services. | `smithy-java-bridge-alexwoo-full:alexwoo-smithy-java-runtime.md`; `compatability_issues.md` |
| LF-17 | The full bridge prototype reported incomplete parity for metrics, timeouts, checksums, compression, configuration options, successful-response metadata, paginators, waiters, utilities, async clients, event streams, presigning, and multiple interceptor hooks. The prototype also loaded build-time model classes at runtime and increased client-construction time. | Full bridge report and compatibility ledger |
| LF-18 | The SPI bridge demonstrated runtime pipeline replacement through service-provider discovery, but its inspected implementation remained sync-focused, resolved operation classes reflectively, carried two pipeline paths, and did not prove full V2 parity. A later Racecar comparison measured repeated V2 request adaptation as a major overhead. | `smithy-java-spi-bridge-poc:.kiro/specs/smithy-java-spi-pipeline-poc.md`; `smithy-bridge-current-state.md`; Racecar summary |
| LF-19 | The bridge investigation found that retry ownership and lifecycle placement affect transport-failure correctness, and that interceptor bridges can change endpoint visibility, mutation semantics, attempt frequency, and shared attributes. | Full bridge report and compatibility ledger; Smithy Java `ClientPipeline.java` |
| LF-20 | V2 already uses compile tests, protocol tests, endpoint tests, migration tests, backward-compatibility tests, stability tests, native-image tests, and benchmarks as separate reactor modules. | Root `pom.xml` |

## Requirements

### Requirement 1: JDK 21+ and Maven Product Build

**User Story:** As a Java developer, I want a current Java baseline and a Maven-native SDK, so that V3 can use modern Java capabilities without adding another build system to SDK consumption or contribution.

#### Acceptance Criteria

1. THE Maven Build SHALL compile every V3 production class as Java release 21 bytecode.
2. WHEN the release reactor runs on each supported JDK version newer than 21, THE Maven Build SHALL complete without source changes.
3. WHEN a contributor starts from a clean checkout on JDK 21 and runs the single documented Maven build command, THE Maven Build SHALL compile, test, and package all code-generation, runtime, service, HTTP, high-level-library, migration, and test artifacts.
4. WHEN the Maven source-generation phase runs, THE Maven Build SHALL generate every Generated Service Package source required by later compilation phases.
5. THE V3 SDK SHALL complete build, test, code generation, packaging, and release without invoking or requiring a Gradle executable.
6. IF the Maven Build runs on a JDK older than 21, THEN THE Maven Build SHALL fail before Java compilation with a diagnostic that identifies JDK 21 as the minimum version.
7. IF the Maven Build rejects a JDK older than 21, THEN THE Maven Build SHALL produce zero V3 class artifacts.

### Requirement 2: Canonical Smithy Model Source

**User Story:** As an SDK maintainer, I want canonical Smithy models to be the sole semantic source, so that generated schemas preserve modeled behavior without lossy reconstruction.

#### Acceptance Criteria

1. THE AWS Codegen Package SHALL use Canonical Smithy Models represented as Smithy `Model` values as the exclusive semantic input for every Generated Service Package.
2. THE AWS Codegen Package SHALL preserve service, operation, shape, member, trait, endpoint, auth, pagination, waiter, example, protocol, documentation, and AWS customization semantics exactly as represented in each Canonical Smithy Model.
3. IF generation receives C2J `service-2.json` input, THEN THE AWS Codegen Package SHALL reject the input with a diagnostic identifying the affected service and C2J format.
4. IF generation receives a V2 paginator sidecar, THEN THE AWS Codegen Package SHALL reject the input with a diagnostic identifying the affected service and paginator sidecar format.
5. IF generation receives a V2 waiter sidecar, THEN THE AWS Codegen Package SHALL reject the input with a diagnostic identifying the affected service and waiter sidecar format.
6. IF generation receives a V2 endpoint sidecar, THEN THE AWS Codegen Package SHALL reject the input with a diagnostic identifying the affected service and endpoint sidecar format.
7. IF generation receives a V2 example sidecar, THEN THE AWS Codegen Package SHALL reject the input with a diagnostic identifying the affected service and example sidecar format.
8. IF generation receives a V2 customization sidecar, THEN THE AWS Codegen Package SHALL reject the input with a diagnostic identifying the affected service and customization sidecar format.
9. IF the AWS Codegen Package rejects a C2J or V2 sidecar input, THEN THE AWS Codegen Package SHALL produce no Generated Service Package artifact for the affected service.
10. IF a required AWS behavior lacks a Canonical Smithy Model representation, THEN THE AWS Codegen Package SHALL fail generation with a diagnostic identifying the affected service, operation when applicable, and missing semantic element.
11. WHEN a Canonical Smithy Model contains an unknown trait, THE AWS Codegen Package SHALL retain the exact trait identifier, trait target, and trait value for runtime Schema access.
12. THE V3 SDK SHALL publish model provenance for each Generated Service Package containing the untruncated immutable source revision and exact service shape identifier.

### Requirement 3: Smithy-Native Code Generation Without an Intermediate Model

**User Story:** As an SDK maintainer, I want one Smithy-native AWS code generator, so that V3 avoids duplicate semantic models and fragmented generation logic.

#### Acceptance Criteria

1. THE V3 SDK SHALL publish exactly one AWS Codegen Package.
2. THE AWS Codegen Package SHALL generate AWS service clients by traversing Smithy `Model`, shapes, traits, indexes, symbols, and directed-generation contexts directly.
3. THE AWS Codegen Package SHALL create zero duplicate whole-service semantic object graphs that restate a Canonical Smithy Model.
4. THE AWS Codegen Package SHALL create zero serialized whole-service semantic representations for a downstream V3 generator.
5. THE AWS Codegen Package SHALL contain every AWS-specific integration required by every Generated Service Package.
6. WHEN identical model inputs, generator configuration, JDK, generator version, and output root are used, THE AWS Codegen Package SHALL produce generated source and resource trees with byte-identical relative paths and byte-identical file contents.
7. IF two AWS code-generation integrations produce conflicting output or settings, THEN THE AWS Codegen Package SHALL fail generation with a diagnostic identifying both integration identifiers and the conflicting Smithy shape identifier.
8. THE AWS Codegen Package SHALL resolve every generated operation descriptor through a generated static reference.
9. THE AWS Pipeline SHALL perform zero runtime class-name lookups to resolve a generated operation descriptor.

### Requirement 4: Generated Types and Schemas

**User Story:** As a service client user, I want familiar immutable model types backed by Smithy schemas, so that V3 remains ergonomic while enabling generic runtime behavior.

#### Acceptance Criteria

1. THE AWS Codegen Package SHALL generate an immutable public Generated Type for every structure, union, enum, and error in the transitive public model closure of each client-supported service.
2. THE AWS Codegen Package SHALL generate V2-compatible builder, accessor, copy-builder, equality, hash, and string-rendering behavior for each Generated Type classified as `PRESERVED`.
3. WHERE a Generated Type API is classified as `PRESERVED`, THE AWS Codegen Package SHALL preserve the API name, parameter type, return type, null behavior, equality result, hash result, and string-rendering contract recorded in the Parity Manifest.
4. THE AWS Codegen Package SHALL generate every structure, union, request, response, and modeled-error Generated Type as a SerializableStruct exposing the complete modeled state.
5. THE AWS Codegen Package SHALL generate exactly one Schema for each exported Generated Type.
6. THE AWS Codegen Package SHALL generate exactly one service Schema for each generated service and exactly one operation Schema for each generated operation, with exact links to the request, response, error, and streaming Schemas required by the AWS Pipeline and Dynamic Client.
7. THE AWS Codegen Package SHALL preserve exact shape identifiers, member identifiers, member order, member targets, recursive references, requiredness, defaults, constraints, and runtime-relevant traits in generated Schemas.
8. WHEN a serializer reads a generated SerializableStruct, THE Generated Type SHALL expose every modeled member value without Java reflection.
9. WHEN a deserializer constructs a Generated Type, THE Generated Type SHALL accept every modeled member assignment without Java reflection.
10. WHEN a Generated Type exposes a modeled collection, THE Generated Type SHALL prevent mutation of the collection and every nested collection reachable through the exposed value.
11. IF a generated builder is missing one or more required members at build time, THEN THE Generated Type SHALL throw one validation error listing every missing required modeled member identifier.
12. WHEN a Schema Fidelity Test compares a Generated Type Schema with the source Canonical Smithy Model shape, THE Schema Fidelity Test SHALL find exactly one matching Schema with identical shape identity, member graph, and runtime-relevant traits.

### Requirement 5: Schema-Decoupled Protocol SERDE

**User Story:** As an AWS service team, I want protocol behavior in reusable runtime libraries, so that protocol changes do not multiply generated code or require regenerated model types.

#### Acceptance Criteria

1. THE Protocol Runtime SHALL serialize every generated request and modeled value by using Schema SERDE conforming to the selected modeled protocol.
2. THE Protocol Runtime SHALL deserialize every transport response and modeled value by using Schema SERDE conforming to the selected modeled protocol.
3. THE AWS Codegen Package SHALL generate zero per-operation protocol-specific marshaller classes.
4. THE AWS Codegen Package SHALL generate zero per-operation protocol-specific unmarshaller classes.
5. THE Protocol Runtime SHALL support AWS JSON 1.0, AWS JSON 1.1, REST JSON 1, REST XML, AWS Query, EC2 Query, and Smithy RPC V2 CBOR at launch.
6. WHEN a client is constructed, THE Client Configuration SHALL select exactly one protocol declared by the Canonical Smithy Model and supported by the Protocol Runtime.
7. WHEN protocol behavior changes without a public model change, THE Protocol Runtime SHALL reuse the same Generated Types without regenerating type code.
8. WHEN a valid Generated Type is serialized and deserialized through identical supported codec settings, THE Protocol Runtime SHALL preserve modeled member presence, member values, modeled member order where order is significant, blob bytes, and event content.
9. IF wire input violates the selected protocol or Schema, THEN THE Protocol Runtime SHALL return an error identifying the operation identifier, shape identifier, and failing member path.
10. IF wire input violates the selected protocol or Schema, THEN THE Protocol Runtime SHALL return no partial modeled value to the caller.
11. WHEN the Protocol Runtime serializes or deserializes a streaming blob or Event Stream, THE Protocol Runtime SHALL process stream data incrementally with bounded buffering and Backpressure.
12. IF Client Configuration selects a protocol absent from the Canonical Smithy Model or unsupported by the Protocol Runtime, THEN THE Client Configuration SHALL reject the selection before returning the client.

### Requirement 6: Documents and Type Registries

**User Story:** As a framework author, I want complete Smithy Documents and discoverable type registries, so that applications can exchange modeled values without compile-time coupling to every type.

#### Acceptance Criteria

1. THE V3 SDK SHALL provide Document values for finite Smithy boolean, byte, short, integer, long, float, double, big-integer, big-decimal, string, enum, int-enum, blob, timestamp, document, list, set, map, structure, and union values.
2. WHEN a Document originates from a value with a specific modeled shape, THE Document SHALL retain the exact normalized absolute shape identifier as the shape discriminator.
3. WHEN a Document originates from protocol input, THE Document SHALL retain the protocol representation context required for valid coercion.
4. WHEN a SerializableStruct is converted to a Document and converted back through the matching Schema, THE V3 SDK SHALL preserve member presence, member values, shape discriminator, blob bytes, timestamp value, and nested collection order where order is significant.
5. THE AWS Codegen Package SHALL generate exactly one publicly accessible primary Type Registry for every Generated Service Package.
6. THE Type Registry SHALL register every public structure, union, request, response, and modeled-error Generated Type in the Generated Service Package.
7. THE Type Registry SHALL map each exact registered shape identifier to the matching Schema, generated Java type, and builder factory.
8. WHEN Type Registries are composed, THE Type Registry SHALL preserve registered entry identity without copying registered entries.
9. WHEN a discriminated Document matches a composed Type Registry entry, THE Type Registry SHALL construct the registered Generated Type through the matching builder factory.
10. IF a Document value is incompatible with a requested Smithy type, THEN THE Document SHALL return a deterministic coercion error identifying the source kind and requested shape identifier.
11. IF a Document lacks a discriminator required for typed construction, THEN THE Type Registry SHALL return a deterministic error identifying the absent discriminator.
12. IF a Document discriminator has no matching Type Registry entry, THEN THE Type Registry SHALL return a deterministic error identifying the unknown shape identifier.
13. IF composed Type Registries contain conflicting entries for one shape identifier, THEN THE Type Registry SHALL reject composition with a deterministic error identifying the shape identifier and both conflicting generated Java types.
14. IF a Document discriminator conflicts with an explicitly requested Schema, THEN THE Document SHALL return a deterministic error identifying both shape identifiers.

### Requirement 7: Generated Sync Client Surface

**User Story:** As a V2 sync-client user, I want a familiar V3 sync API, so that migration preserves common application structure.

#### Acceptance Criteria

1. THE AWS Codegen Package SHALL generate exactly one public Sync Client interface for every client-supported AWS service.
2. WHERE the corresponding V2 Sync Client provides `create()`, THE Sync Client SHALL provide a V2-compatible `create()` construction entry point.
3. WHERE the corresponding V2 Sync Client provides `builder()`, THE Sync Client SHALL provide a V2-compatible `builder()` construction entry point.
4. THE Sync Client SHALL provide a typed request method for every modeled operation available to synchronous callers.
5. WHERE an operation consumer overload is classified as `PRESERVED`, THE Sync Client SHALL provide the request-builder consumer overload with the preserved signature and behavior.
6. WHERE an operation has no required input members and a no-argument overload is classified as `PRESERVED`, THE Sync Client SHALL provide the no-argument overload with the preserved signature and behavior.
7. WHERE a streaming overload is classified as `PRESERVED`, THE Sync Client SHALL provide the corresponding synchronous request-body or response-transformer overload with the preserved signature and behavior.
8. WHEN an operation is paginated, THE Sync Client SHALL provide a synchronous paginator derived from the Canonical Smithy Model pagination semantics.
9. WHEN a service defines waiters, THE Sync Client SHALL provide a waiter sharing the exact resolved Client Configuration of the Sync Client.
10. WHERE the Parity Manifest classifies service presigning or utility behavior as `PRESERVED` or `ADAPTED`, THE Sync Client SHALL expose the documented V3 presigner or utility entry point.
11. THE Sync Client SHALL expose an immutable accessor returning the exact resolved Client Configuration used by calls from that client.
12. WHEN multiple threads invoke overlapping operations on one Sync Client, THE Sync Client SHALL complete calls without cross-call state corruption.

### Requirement 8: Generated Async Client Surface

**User Story:** As a V2 async-client user, I want a non-blocking V3 async API, so that migration retains asynchronous composition, streaming, and cancellation.

#### Acceptance Criteria

1. WHERE the Launch Parity Baseline provides an async client for a client-supported AWS service, THE AWS Codegen Package SHALL generate exactly one public Async Client interface for that service.
2. WHEN a non-streaming Async Client operation is invoked, THE Async Client SHALL return a `CompletableFuture` before terminal operation completion.
3. WHERE an operation consumer overload is classified as `PRESERVED`, THE Async Client SHALL provide the request-builder consumer overload with the preserved signature and behavior.
4. WHEN an operation has streaming input, THE Async Client SHALL request input elements only in response to downstream demand and SHALL consume no more elements than requested.
5. WHEN an operation has streaming output, THE Async Client SHALL deliver output elements only in response to downstream demand and SHALL deliver no more elements than requested.
6. WHEN an operation is paginated, THE Async Client SHALL request pages according to downstream demand and SHALL request no page after demand reaches zero.
7. WHEN a service defines waiters, THE Async Client SHALL provide a waiter that performs waiting without blocking the caller thread.
8. WHILE code executes on an Event Loop, THE Async Client SHALL perform zero blocking credential, endpoint, retry-delay, request-body, response-body, or HTTP operations.
9. WHEN a caller successfully cancels a returned future, THE Async Client SHALL cancel active body subscriptions, pending retry delays, and in-flight HTTP work.
10. WHEN a caller successfully cancels a returned future, THE Async Client SHALL schedule no later attempt or body-consumption work for the cancelled call.
11. IF a child asynchronous stage fails, THEN THE Async Client SHALL complete the returned future exceptionally with the causal SDK exception preserved in the exception chain.
12. WHEN the Async Client performs SDK-owned asynchronous work, THE Async Client SHALL use a configured executor and SHALL use zero Common Fork-Join Pool tasks.
13. WHEN multiple threads invoke overlapping operations on one Async Client, THE Async Client SHALL complete calls without cross-call state corruption.

### Requirement 9: Configuration and Resource Ownership

**User Story:** As a V2 customer, I want V3 configuration to preserve common names, precedence, and ownership behavior, so that upgrades are predictable.

#### Acceptance Criteria

1. THE Client Configuration SHALL expose typed settings for region, endpoint, identity, auth, retry, timeout, HTTP, Interceptor, telemetry, protocol, and service-specific options.
2. WHERE a V2 configuration API is classified as `PRESERVED`, THE Client Configuration SHALL preserve the builder method name and value type recorded in the Parity Manifest.
3. WHEN multiple configuration sources provide one setting, THE Client Configuration SHALL apply precedence in the order Request Override, explicit builder value, JVM property, environment variable, profile value, and SDK default.
4. WHEN one Request Override is applied to a call, THE Client Configuration SHALL isolate the overridden value from every other call.
5. WHEN a client is built, THE Client Configuration SHALL resolve each client-constant derived value once and retain the resolved value in immutable state.
6. WHEN a closeable resource is configured, THE Client Configuration SHALL record whether the V3 SDK or the caller owns the resource.
7. WHEN a client closes one or more times, THE Client Configuration SHALL close each SDK-owned resource exactly once.
8. WHEN a client closes, THE Client Configuration SHALL leave every caller-owned resource open.
9. IF a supplied V2 configuration option has no V3 behavior, THEN THE Client Configuration SHALL reject client construction with a diagnostic identifying the unsupported option.
10. WHEN the Migration Tooling encounters a V2 configuration option without V3 behavior, THE Migration Tooling SHALL report the source location, unsupported option, and Parity Manifest disposition.
11. WHEN the Client Configuration accepts a configuration option, THE V3 SDK SHALL apply the option to every call within the documented scope.
12. THE Request Override SHALL support per-call headers, query parameters, identity, timeout, retry, Interceptor, telemetry, and typed Execution Context values classified as `PRESERVED` or `ADAPTED`.
13. IF closing one SDK-owned resource fails, THEN THE Client Configuration SHALL continue closing every remaining SDK-owned resource.
14. IF closing any SDK-owned resource fails, THEN THE Client Configuration SHALL report each close failure after all SDK-owned resources have received one close attempt.

### Requirement 10: SDK-Owned AWS Pipeline

**User Story:** As an SDK maintainer, I want one SDK-owned AWS request pipeline, so that generated and dynamic clients share AWS correctness while V3 controls compatibility-sensitive ordering and performance.

#### Acceptance Criteria

1. THE V3 SDK SHALL provide exactly one production AWS Pipeline path from operation invocation through terminal completion.
2. THE AWS Pipeline SHALL order request validation, request interception, endpoint resolution, identity resolution, serialization, signing, HTTP transmission, response interception, deserialization, retry classification, telemetry, response metadata, and exception mapping according to one documented lifecycle.
3. THE AWS Pipeline SHALL use only Smithy Java Schema, Codec, Protocol Runtime, and extensibility components that pass V3 conformance and performance gates.
4. THE AWS Pipeline SHALL apply the same lifecycle ordering to Sync Client, Async Client, and Dynamic Client calls except for documented synchronous and asynchronous transport mechanics.
5. THE AWS Pipeline SHALL contain zero fallback paths to a V2 execution pipeline.
6. WHEN a retryable failure occurs and the Modern Retry strategy grants a retry token, THE AWS Pipeline SHALL submit exactly one subsequent attempt for that token.
7. IF the Modern Retry strategy grants no retry token, THEN THE AWS Pipeline SHALL submit no subsequent attempt.
8. WHEN an attempt begins, THE AWS Pipeline SHALL expose a one-based attempt number through the attempt-scoped Execution Context.
9. WHEN an attempt resolves an endpoint or identity, THE AWS Pipeline SHALL expose the resolved value only to later stages and Interceptors in that attempt.
10. WHEN no telemetry publisher and no Interceptor implementing the current hook are configured, THE AWS Pipeline SHALL perform zero dispatch, timing, or counting work solely for the inactive extension point.
11. THE AWS Pipeline SHALL reuse client-constant stage graphs, Interceptor dispatch plans, auth metadata, endpoint metadata, and user-agent components across calls.
12. THE AWS Pipeline SHALL perform zero runtime class-name lookups to invoke a generated operation.
13. IF a configured Protocol Runtime and HTTP SPI use incompatible message types, THEN THE Client Configuration SHALL reject construction before returning a client and identify both component identifiers.
14. IF an AWS Pipeline stage fails, THEN THE AWS Pipeline SHALL skip every dependent later stage.
15. IF an AWS Pipeline stage fails, THEN THE AWS Pipeline SHALL complete the call exactly once through one exception-mapping path.

### Requirement 11: Interceptors and Plugins

**User Story:** As an SDK extension author, I want complete and efficient lifecycle hooks, so that V3 supports observability and mutation without service-specific generated coupling.

#### Acceptance Criteria

1. WHEN the AWS Pipeline invokes an Interceptor hook, THE Interceptor SHALL receive the operation Schema and typed Execution Context for the documented execution or attempt scope.
2. THE Interceptor SHALL provide documented modeled-request, serialized-request, pre-signing, post-signing, pre-transmit, post-transmit, transport-response, deserialization, modeled-response, exception, attempt, and execution hook boundaries.
3. WHEN an applicable lifecycle boundary occurs once, THE AWS Pipeline SHALL invoke each registered Interceptor for that boundary exactly once within the documented scope.
4. WHEN an Interceptor modifies a transport request before signing, THE AWS Pipeline SHALL sign the modified transport request.
5. WHEN an Interceptor observes a resolved endpoint, THE Interceptor SHALL receive the exact endpoint transmitted by the current attempt.
6. WHEN an Interceptor stores a typed execution-scoped Execution Context value, THE AWS Pipeline SHALL expose the value to later execution-scoped hooks across subsequent attempts of the same call.
7. WHEN an Interceptor stores a typed attempt-scoped Execution Context value, THE AWS Pipeline SHALL expose the value only to later hooks in the same attempt.
8. WHEN an Interceptor replaces a request body or response body, THE AWS Pipeline SHALL use the complete replacement body as the next-stage input.
9. WHEN a call performs multiple attempts, THE AWS Pipeline SHALL invoke each per-attempt hook once per applicable attempt and each per-execution hook once per call.
10. WHEN multiple Interceptors are configured, THE AWS Pipeline SHALL invoke Interceptors in documented registration order at each hook.
11. THE AWS Feature Plugin SHALL depend only on Schemas and runtime abstractions shared by Generated Service Packages and the Dynamic Client.
12. WHERE V2 Interceptor compatibility is enabled, THE Compatibility Layer SHALL reproduce every `ADAPTED` hook semantic recorded in the Parity Manifest.
13. IF an Interceptor returns an incompatible modeled or transport value, THEN THE AWS Pipeline SHALL fail the call with an error identifying the Interceptor class and lifecycle hook.
14. IF an Interceptor throws an exception, THEN THE AWS Pipeline SHALL fail the call with an error identifying the Interceptor class and lifecycle hook while preserving the thrown exception as the cause.

### Requirement 12: HTTP SPI and Implementations

**User Story:** As an SDK customer, I want portable HTTP contracts and launch-time implementation choice, so that V3 works with existing deployment constraints and optimized transports.

#### Acceptance Criteria

1. THE HTTP SPI SHALL define request, response, header, body-stream, timeout, cancellation, connection, TLS, proxy, and resource-ownership lifecycles.
2. WHEN a Canonical Smithy Model requires HTTP/1.1 behavior, THE HTTP SPI SHALL represent every protocol-significant method, target, header, body, and connection behavior required by the model.
3. WHEN a Canonical Smithy Model requires HTTP/2 behavior, THE HTTP SPI SHALL represent every protocol-significant pseudo-header, header, stream, flow-control, and connection behavior required by the model.
4. THE HTTP SPI SHALL preserve repeated header values in insertion order wherever protocol semantics require order preservation.
5. WHEN a body stream is transmitted or received, THE HTTP SPI SHALL move data according to downstream demand and keep buffered body data within the configured HTTP Implementation buffer bound.
6. THE HTTP Implementation SHALL document the configurable buffer bound or the test-derived fixed bound used to verify bounded streaming behavior.
7. WHEN an HTTP Implementation reports a transport failure, THE HTTP SPI SHALL map the failure to a stable V3 category for connection acquisition, connection establishment, TLS, proxy, timeout, cancellation, request transmission, or response consumption.
8. WHEN a caller successfully cancels an in-flight async request, THE HTTP SPI SHALL terminate pending acquisition, transmission, and response consumption within the documented cancellation timeout and SHALL report terminal completion exactly once.
9. THE V3 SDK SHALL provide native V3 synchronous and asynchronous HTTP Implementations for every non-deprecated corresponding implementation in the Launch Parity Baseline.
10. THE Parity Manifest SHALL classify Apache HTTP Client 4, Apache HTTP Client 5, JDK URLConnection, Netty NIO, and AWS Common Runtime artifacts separately with supported synchronous or asynchronous modes.
11. WHEN multiple default HTTP Implementations are present, THE Client Configuration SHALL select the implementation by one documented deterministic priority order.
12. IF multiple default HTTP Implementations share the highest priority, THEN THE Client Configuration SHALL fail construction with a diagnostic identifying each tied implementation.
13. IF an HTTP Implementation cannot support a configured option, THEN THE Client Configuration SHALL fail construction with a diagnostic identifying the implementation and option.
14. IF HTTP client construction fails because of an unsupported option or tied priority, THEN THE Client Configuration SHALL release every SDK-owned HTTP resource acquired during construction.
15. THE AWS Pipeline SHALL invoke and require zero V2 HTTP adapter classes on the native generated-client hot path.

### Requirement 13: AWS Auth, Endpoints, Resilience, and Data Integrity

**User Story:** As an AWS customer, I want V3 to retain current AWS correctness features, so that a major-version upgrade does not weaken authentication, routing, resilience, or payload integrity.

#### Acceptance Criteria

1. THE V3 SDK SHALL select Modern Auth schemes from Canonical Smithy Model service and operation traits.
2. THE V3 SDK SHALL implement every Modern Auth scheme required by a service or operation in the Launch Parity Baseline.
3. WHEN multiple default AWS identity sources are eligible, THE V3 SDK SHALL evaluate the sources in the exact precedence recorded in the Parity Manifest.
4. THE V3 SDK SHALL preserve the Launch Parity Baseline behavior recorded in the Parity Manifest for supported profile, environment, JVM property, container, web identity, SSO, role, and instance-metadata identity sources.
5. THE V3 SDK SHALL resolve regional, partition, FIPS, dual-stack, account-based, host-prefix, endpoint-discovery, and endpoint-auth dimensions from Canonical Smithy Model rules and traits.
6. WHEN a Canonical Smithy Model requires a request checksum, THE V3 SDK SHALL calculate and transmit the checksum over the exact modeled payload scope and algorithm.
7. WHEN a Canonical Smithy Model requires response checksum validation, THE V3 SDK SHALL validate the received checksum before returning the modeled response.
8. WHEN a Canonical Smithy Model requires request compression or content encoding, THE V3 SDK SHALL apply the exact modeled algorithm, threshold, and protocol fields.
9. WHEN a Canonical Smithy Model requires an idempotency token and the request omits the token, THE V3 SDK SHALL inject one token according to the modeled token contract.
10. WHEN a Canonical Smithy Model requires Event Stream signing, THE V3 SDK SHALL sign each required frame according to the selected Modern Auth scheme.
11. THE V3 SDK SHALL implement every Modern Retry mode required by the Launch Parity Baseline with the documented token, attempt, quota, and delay accounting.
12. WHEN a service response supplies a syntactically valid supported retry-delay hint within the documented mode bound, THE Modern Retry SHALL use the parsed hint as the attempt-delay input.
13. WHEN a service response supplies a syntactically valid supported retry-delay hint above the documented mode bound, THE Modern Retry SHALL cap the attempt-delay input at the documented mode bound.
14. IF a service response supplies an invalid retry-delay hint, THEN THE Modern Retry SHALL ignore the hint and use the strategy delay calculation.
15. WHEN a modeled error contains retry traits, THE Modern Retry SHALL classify retryability from both Schema traits and response metadata.
16. WHEN temporary identity material expires or is rejected and the identity resolver contract permits refresh, THE V3 SDK SHALL request refreshed identity before an allowed subsequent attempt or call.
17. WHEN identity refresh succeeds, THE V3 SDK SHALL use the refreshed identity for the subsequent signing operation.
18. IF endpoint, auth, checksum, compression, or content-encoding configuration conflicts with the Canonical Smithy Model, THEN THE V3 SDK SHALL block transmission and identify the operation and conflicting setting.
19. IF required identity refresh fails, THEN THE V3 SDK SHALL block subsequent transmission and return an error preserving the refresh failure as the cause.

### Requirement 14: Dynamic Client

**User Story:** As a tool or framework author, I want a model-driven client with full AWS behavior, so that runtime-discovered services do not require hand-written request pipelines.

#### Acceptance Criteria

1. WHEN the Dynamic Client loads a Smithy JSON AST model, THE Dynamic Client SHALL preserve every service, operation, shape, member, trait identifier, trait target, and trait value represented by the runtime model.
2. WHEN a runtime model contains exactly one service and no explicit service identifier is supplied, THE Dynamic Client SHALL select that service.
3. WHEN a runtime model contains multiple services and an exact service shape identifier is supplied, THE Dynamic Client SHALL select the identified service.
4. IF a runtime model contains multiple services without an explicit service shape identifier, THEN THE Dynamic Client SHALL reject construction with an error listing every available service shape identifier in lexical order.
5. IF a runtime model contains no service, THEN THE Dynamic Client SHALL reject construction with a no-service error.
6. WHEN a caller supplies an exact modeled operation name, THE Dynamic Client SHALL resolve the matching operation in the selected service.
7. IF a caller supplies an unknown operation name, THEN THE Dynamic Client SHALL reject the invocation with an error identifying the selected service, unknown name, and available operation names.
8. WHEN a synchronous dynamic invocation terminates successfully, THE Dynamic Client SHALL return the modeled finite output or streaming output terminal result to the caller.
9. WHEN an asynchronous dynamic invocation starts, THE Dynamic Client SHALL return a `CompletableFuture` before terminal operation completion.
10. WHEN an asynchronous dynamic invocation terminates, THE Dynamic Client SHALL complete the returned `CompletableFuture` exactly once with the modeled result, cancellation, or causal SDK exception.
11. WHEN an operation has finite input or output, THE Dynamic Client SHALL accept or return Document values conforming to the exact operation input or output Schema.
12. WHEN an operation contains a streaming blob or Event Stream, THE Dynamic Client SHALL use the same demand, Backpressure, cancellation, and terminal lifecycle as the corresponding Generated Service Package operation.
13. WHEN a caller successfully cancels an asynchronous dynamic invocation, THE Dynamic Client SHALL propagate cancellation through the AWS Pipeline.
14. THE Dynamic Client SHALL use the same AWS Pipeline used by Generated Service Packages.
15. THE Dynamic Client SHALL support every applicable Client Configuration, Request Override, Interceptor, AWS Feature Plugin, Modern Auth, endpoint, Modern Retry, telemetry, paginator, and waiter behavior supported by the runtime model.
16. WHEN generated and dynamic calls use equivalent input, configuration, protocol, and nondeterministic metadata, THE Dynamic Client SHALL produce Wire Equivalent HTTP method, URI, headers, body, event frames, and status interpretation.
17. WHEN a Dynamic Client loads an unknown trait, THE Dynamic Client SHALL preserve the exact trait identifier, target, and value as a runtime object.
18. THE Dynamic Client SHALL isolate runtime Smithy model parser dependencies in optional artifacts absent from every Generated Service Package runtime dependency tree.
19. IF the selected service, operation, or finite input is invalid, THEN THE Dynamic Client SHALL reject the call before HTTP transmission with an error identifying the invalid element.

### Requirement 15: Core Libraries and Dependency Boundaries

**User Story:** As an application owner, I want modular V3 dependencies, so that service clients remain small, conflict-resistant, and fast to initialize.

#### Acceptance Criteria

1. THE V3 SDK SHALL publish a BOM that manages the exact release version of every V3 release artifact.
2. THE V3 SDK SHALL publish a documented map from public API, Schema SERDE, Protocol Runtime, AWS Pipeline, auth, identity, endpoint, retry, checksum, telemetry, HTTP SPI, HTTP Implementation, dynamic-model, codegen, service, and high-level-library concerns to owning Maven artifacts and allowed dependencies.
3. THE Generated Service Package SHALL have a runtime dependency tree containing zero V2 runtime artifacts.
4. THE Generated Service Package SHALL have a runtime dependency tree containing zero code-generation artifacts.
5. THE Generated Service Package SHALL have a runtime dependency tree containing zero full Smithy model parser artifacts.
6. THE Generated Service Package SHALL have a runtime dependency tree containing zero generated protocol-marshaller artifacts.
7. THE V3 SDK SHALL make HTTP Implementations and telemetry exporters opt-in dependencies.
8. THE V3 SDK SHALL expose zero third-party logging implementation types in public or protected APIs.
9. WHEN a minimal Generated Service Package is resolved with no optional component, THE V3 SDK SHALL produce a Maven dependency tree that exactly matches the documented service-and-required-runtime dependency set.
10. IF build validation detects incompatible V3 runtime versions, THEN THE Maven Build SHALL fail with a diagnostic identifying every conflicting module and version.
11. IF client construction detects incompatible V3 runtime versions, THEN THE Client Configuration SHALL fail construction with a diagnostic identifying every conflicting module and version.

### Requirement 16: Launch-Time V2 Feature Parity

**User Story:** As a V2 customer, I want supported V2 capabilities available when V3 launches, so that migration does not require waiting for missing services or libraries.

#### Acceptance Criteria

1. THE Parity Manifest SHALL identify the single V2 general-availability release, feature-freeze cutoff date, and support or deprecation status used as the Launch Parity Baseline.
2. THE Parity Manifest SHALL contain one entry for every public artifact, public API, and documented customer behavior in the Launch Parity Baseline.
3. THE Parity Manifest SHALL assign exactly one Compatibility Classification of `PRESERVED`, `ADAPTED`, or `EXCLUDED` to every entry.
4. THE V3 SDK SHALL launch every non-deprecated AWS service in the Launch Parity Baseline at V3 general availability.
5. THE V3 SDK SHALL launch generated Sync Client and Async Client surfaces, modeled exceptions, paginators, waiters, presigners, utilities, streaming operations, Event Stream operations, endpoint providers, and service customizations classified as `PRESERVED` or `ADAPTED`.
6. THE V3 SDK SHALL launch DynamoDB Enhanced synchronous and asynchronous clients with mapping, table, index, batch, transaction, pagination, extension, and Document behaviors classified as `PRESERVED` or `ADAPTED`.
7. THE V3 SDK SHALL launch S3 Transfer Manager upload, download, directory, multipart, progress, cancellation, resume, and failure-cleanup behaviors classified as `PRESERVED` or `ADAPTED`.
8. THE V3 SDK SHALL launch IAM policy builder, S3 event-notification model, and SNS message-manager behaviors classified as `PRESERVED` or `ADAPTED`.
9. THE V3 SDK SHALL launch every non-deprecated HTTP Implementation classified as `PRESERVED` or `ADAPTED`.
10. THE V3 SDK SHALL launch telemetry, logging, profile, region, credential, endpoint, checksum, compression, native-image, module-path, and service-default behaviors classified as `PRESERVED` or `ADAPTED`.
11. IF a Parity Manifest entry is classified as `ADAPTED`, THEN THE Parity Manifest SHALL list every behavioral and API difference, V3 replacement, migration action, and verification test.
12. IF a Parity Manifest entry is classified as `ADAPTED`, THEN THE V3 SDK SHALL ship the listed replacement at general availability.
13. IF a Parity Manifest entry is classified as `EXCLUDED`, THEN THE Parity Manifest SHALL include deprecation or support evidence, exclusion rationale, migration replacement when one exists, and compile-time or Migration Tooling diagnostic.
14. WHEN a release candidate is evaluated, THE Parity Manifest SHALL record candidate-specific expected outcome, actual outcome, and passing verification evidence for every non-deprecated entry.
15. IF any non-deprecated Parity Manifest entry lacks a shipped `PRESERVED` or `ADAPTED` capability and passing candidate-specific evidence, THEN THE V3 SDK SHALL block general availability.

### Requirement 17: V2 Migration Compatibility

**User Story:** As a V2 application owner, I want measurable compatibility and automated migration, so that upgrading to V3 has bounded effort and no silent semantic loss.

#### Acceptance Criteria

1. WHERE a V2 API is classified as `PRESERVED`, THE Compatibility Layer SHALL preserve the fully qualified type name, method name, parameter signature, return signature, overload set, builder pattern, and exception inheritance relationship recorded in the Parity Manifest.
2. WHERE a V2 API is classified as binary-compatible in the Parity Manifest, THE Compatibility Layer SHALL load and invoke compiled V2 caller bytecode without a linkage failure.
3. WHEN a Migration Corpus project uses only `PRESERVED` APIs, THE V3 SDK SHALL compile the project after only JDK and Maven dependency-version changes.
4. WHERE a Parity Manifest entry is classified as `ADAPTED`, THE Migration Tooling SHALL provide an automated recipe or a source-located diagnostic describing the required manual adaptation.
5. THE V3 SDK SHALL publish Maven-delivered automated migration recipes from V2 to V3.
6. THE Migration Tooling SHALL cover dependencies, BOM coordinates, imports, builders, Client Configuration, Interceptors, HTTP clients, retries, signers, streaming types, and high-level-library APIs represented in the Parity Manifest.
7. WHEN the Migration Tooling transforms a Migration Corpus project without unresolved diagnostics, THE Migration Tooling SHALL produce JDK 21-compatible source that compiles.
8. WHEN the Migration Tooling transforms a Migration Corpus project without unresolved diagnostics, THE Migration Tooling SHALL verify that the transformed application passes the corpus behavioral tests.
9. IF the Migration Tooling cannot preserve or safely transform a source construct, THEN THE Migration Tooling SHALL leave the construct unchanged and emit an unresolved diagnostic containing file, line, affected construct, V3 replacement when one exists, and classification rationale.
10. WHEN the Migration Tooling encounters a migration item, THE Migration Tooling SHALL record exactly one disposition of preserved, transformed, or unresolved.
11. THE Migration Tooling SHALL assign a disposition to every encountered dependency, configuration option, Interceptor hook, HTTP option, retry behavior, signer behavior, streaming behavior, and high-level-library behavior.
12. WHEN a release candidate is produced, THE V3 SDK SHALL publish a machine-readable compatibility report covering every Compatibility Classification, migration recipe, Migration Corpus compile result, Migration Corpus behavioral result, and unresolved diagnostic.

### Requirement 18: Deprecated and Legacy Facility Exclusions

**User Story:** As a V3 user, I want obsolete facilities removed with actionable replacements, so that the new major version has one modern path for security and resilience behavior.

#### Acceptance Criteria

1. THE V3 SDK SHALL expose zero pre-Smithy-Reference-Architecture signer APIs listed as `EXCLUDED` in the Parity Manifest.
2. THE V3 SDK SHALL expose zero V2 `Signer` or legacy `Presigner` extension contracts listed as `EXCLUDED` in the Parity Manifest.
3. WHEN a service supports presigning, THE V3 SDK SHALL provide the named service-presigning replacement through the Modern Auth scheme recorded in the Parity Manifest.
4. THE V3 SDK SHALL expose zero legacy retry strategies or retry-policy APIs listed as `EXCLUDED` in the Parity Manifest.
5. THE V3 SDK SHALL expose only Modern Retry extension contracts designated by the Parity Manifest.
6. THE V3 SDK SHALL expose zero public APIs deprecated before the Parity Manifest feature-freeze date and classified as `EXCLUDED`.
7. WHEN the Migration Tooling encounters an excluded signer API with a replacement, THE Migration Tooling SHALL emit a diagnostic naming the Modern Auth replacement.
8. WHEN the Migration Tooling encounters an excluded retry API with a replacement, THE Migration Tooling SHALL emit a diagnostic naming the Modern Retry replacement.
9. IF no safe replacement exists for an excluded facility, THEN THE Migration Tooling SHALL leave the source unchanged, halt migration of that use, and emit a blocking diagnostic containing the exclusion rationale and documentation identifier.

### Requirement 19: Correctness and Conformance Verification

**User Story:** As an AWS customer, I want V3 behavior validated at model, protocol, pipeline, and service levels, so that architectural changes do not create silent wire or lifecycle defects.

#### Acceptance Criteria

1. WHEN the release reactor runs, THE V3 SDK SHALL execute every applicable Smithy protocol conformance case for every supported Protocol Runtime.
2. WHEN the release reactor runs, THE V3 SDK SHALL execute every applicable endpoint rule and endpoint test case from the pinned Canonical Smithy Models.
3. WHEN a Schema Fidelity Test examines a source shape, THE Schema Fidelity Test SHALL find exactly one generated Schema with the exact shape identifier, shape type, recursive member graph, requiredness, defaults, constraints, and runtime-relevant traits.
4. THE V3 SDK SHALL run wire-differential tests covering request serialization, successful-response deserialization, modeled-error deserialization, and unmodeled-error handling for every supported protocol and modeled binding-trait category.
5. WHEN a wire-differential expectation intentionally differs from V2, THE V3 SDK SHALL require the wire-differential test to reference the corresponding `ADAPTED` or `EXCLUDED` Parity Manifest entry.
6. THE V3 SDK SHALL run generated-versus-dynamic differential tests that compare modeled results and Wire Equivalent requests for every supported protocol.
7. THE V3 SDK SHALL run deterministic fault-injection cases for connection failure, partial request body, partial response body, timeout, throttling, retry-delay hint, malformed success, modeled error, unmodeled error, identity failure, endpoint failure, and cancellation.
8. WHEN a deterministic fault-injection case runs, THE V3 SDK SHALL verify terminal outcome, attempt count, applied retry delay, cancellation propagation, and resource release applicable to the injected fault.
9. THE V3 SDK SHALL run documented concurrency tests for clients, identity resolvers, HTTP Implementations, Interceptors, retries, paginators, waiters, and high-level libraries.
10. WHEN a concurrency test runs, THE V3 SDK SHALL use a documented simultaneous-call count of at least two, a documented total-operation count greater than the simultaneous-call count, a synchronization barrier that produces the configured overlap, and a documented bounded completion timeout.
11. WHEN a bounded-memory streaming test runs, THE V3 SDK SHALL transfer a payload larger than the configured JVM heap and verify complete byte-for-byte ordered output.
12. THE V3 SDK SHALL run source and binary compatibility tests for every `PRESERVED`, `ADAPTED`, and `EXCLUDED` behavior applicable to the test scope.
13. WHEN a module-path or supported native-image smoke test runs, THE V3 SDK SHALL construct a client and complete one modeled call.
14. IF an applicable conformance, endpoint, schema-fidelity, differential, fault-injection, concurrency, bounded-memory, compatibility, module-path, or native-image test fails or exceeds the test timeout, THEN THE Maven Build SHALL fail the release reactor.
15. IF an applicable release-gate test is skipped without an approved omission recorded in release evidence, THEN THE Maven Build SHALL fail the release reactor.

### Requirement 20: Maximum Measured Performance

**User Story:** As a high-throughput Java customer, I want V3 to minimize CPU, allocation, latency, startup, and memory, so that SDK overhead does not dominate AWS calls.

#### Acceptance Criteria

1. THE Performance Harness SHALL measure generated Sync Client, generated Async Client, and Dynamic Client paths for every supported protocol applicable to each client path.
2. THE Performance Harness SHALL define small, medium, and large finite payload classes by non-overlapping documented encoded-byte ranges.
3. THE Performance Harness SHALL include finite small, medium, and large success, retry, and modeled-error workloads and SHALL include streaming and Event Stream workloads.
4. THE Performance Harness SHALL run non-streaming success and retry workloads at concurrency levels 1, 8, and 32.
5. WHEN a release-gate collection runs, THE Performance Harness SHALL run client and mock-service processes separately on dedicated pinned compute with identical host model, processor configuration, operating system, JDK, JVM flags, power settings, network path, and background-service policy across compared arms.
6. WHEN a benchmark cell is compared, THE Performance Harness SHALL execute at least five and at most ten interleaved repetitions per arm.
7. WHEN a benchmark repetition is measured, THE Performance Harness SHALL collect steady-state samples for at least 30 seconds after the JIT steady-state gate passes.
8. WHEN a release-gate collection runs, THE Performance Harness SHALL include a same-artifact null experiment for every measured benchmark cell.
9. WHEN steady-state samples are considered for acceptance, THE Performance Harness SHALL require the documented rolling compilation, throughput, CPU, and allocation stability thresholds to hold for the documented steady-state window.
10. THE Performance Harness SHALL publish per-repetition application CPU time, allocated bytes, p50 latency, p99 latency, throughput, cold client-construction time, first-call time, loaded-class count, artifact size, peak streaming heap usage, maximum heap setting, host metadata, JDK metadata, JVM flags, and benchmark configuration.
11. WHEN generated non-streaming benchmark cells are aggregated, THE Performance Harness SHALL calculate equal-weight geometric means across every valid protocol, client mode, payload-size, outcome, and concurrency cell in the aggregation.
12. WHEN generated non-streaming application CPU is aggregated, THE V3 SDK SHALL use no more than 70% of Launch Parity Baseline application CPU per operation by equal-weight geometric mean.
13. WHEN generated non-streaming allocation is aggregated, THE V3 SDK SHALL allocate no more than 60% of Launch Parity Baseline bytes per operation by equal-weight geometric mean.
14. WHEN a generated benchmark cell is compared with the Performance Reference, THE V3 SDK SHALL exceed the best valid Performance Reference application CPU result for that cell by no more than 10% as a Statistically Valid Result.
15. WHEN a generated benchmark cell is compared with the Performance Reference, THE V3 SDK SHALL exceed the best valid Performance Reference allocation result for that cell by no more than 10% as a Statistically Valid Result.
16. WHEN an Async Client benchmark uses an in-memory request and response, THE Async Client SHALL consume no more than 35% additional application CPU per operation over the corresponding Sync Client benchmark cell.
17. WHEN a Dynamic Client benchmark uses an equivalent finite request and response, THE Dynamic Client SHALL consume no more than 25% additional application CPU per operation over the corresponding generated-client benchmark cell.
18. WHEN a Dynamic Client benchmark uses an equivalent finite request and response, THE Dynamic Client SHALL allocate no more than 25% additional bytes per operation over the corresponding generated-client benchmark cell.
19. WHEN a minimal generated client is constructed and performs a first call, THE V3 SDK SHALL exceed the Launch Parity Baseline combined construction-and-first-call time by no more than 5% as a Statistically Valid Result.
20. WHEN a 1 GiB streaming transfer runs with a 256 MiB maximum heap, THE V3 SDK SHALL complete within the documented release-gate timeout and preserve the exact byte count, byte values, and byte order without an out-of-memory error.
21. WHEN no Interceptor, telemetry publisher, or Request Override is configured, THE AWS Pipeline SHALL allocate zero objects whose sole purpose is inactive extension dispatch.
22. WHEN a generated operation executes after warmup, THE AWS Pipeline SHALL perform zero reflective member access, runtime model parsing, runtime operation lookup, or per-call pipeline construction.
23. WHEN a benchmark comparison is classified as a Statistically Valid Result, THE Performance Harness SHALL report a 95% confidence interval with relative half-width no greater than 5% for the compared metric.
24. WHEN a same-artifact null experiment is evaluated, THE Performance Harness SHALL require each compared CPU, allocation, latency, and throughput ratio to fall within 0.95 through 1.05.
25. IF any same-artifact null metric ratio falls outside 0.95 through 1.05, THEN THE Performance Harness SHALL invalidate the release-gate collection.
26. IF the JIT steady-state gate does not pass within the documented warmup timeout, THEN THE Performance Harness SHALL invalidate the affected benchmark cell.
27. IF a benchmark repetition contains a failed operation, incomplete sample, or host-policy violation, THEN THE Performance Harness SHALL invalidate the repetition.
28. IF fewer than five valid interleaved repetitions remain for any compared arm, THEN THE Performance Harness SHALL invalidate the affected benchmark cell.
29. IF a release candidate misses a performance boundary or contains an invalid required release-gate collection, THEN THE V3 SDK SHALL block general availability.

### Requirement 21: Observability, Metadata, and Safe Diagnostics

**User Story:** As an operator, I want complete low-overhead diagnostics, so that V3 calls can be investigated without exposing sensitive data or charging inactive features.

#### Acceptance Criteria

1. WHEN a response supplies request identifier, extended request identifier, HTTP status, service error code, attempt count, or transport metadata, THE V3 SDK SHALL expose each supplied value through the modeled response or SDK exception metadata API.
2. WHERE a telemetry implementation is configured, THE Telemetry SPI SHALL emit exactly one terminal call record for each call.
3. WHERE a telemetry implementation is configured, THE Telemetry SPI SHALL emit exactly one terminal attempt record for each attempt.
4. WHEN the Telemetry SPI emits a call or attempt record, THE Telemetry SPI SHALL include service, operation, outcome, attempt number when applicable, HTTP Implementation, retry mode, and duration dimensions.
5. WHEN no telemetry implementation is configured, THE AWS Pipeline SHALL perform zero telemetry callbacks and SHALL create zero timers or byte counters used only by telemetry.
6. WHEN a component contributes to a call, THE V3 SDK SHALL include the service, operation, SDK version, HTTP Implementation, retry mode, and contributing high-level-library feature identifiers in the user agent.
7. WHEN a component does not contribute to a call, THE V3 SDK SHALL omit that component identifier from the user agent.
8. THE V3 SDK SHALL route runtime logging through a facade that requires no application logging implementation.
9. THE V3 SDK SHALL emit zero INFO-level log events from runtime artifacts.
10. WHEN the V3 SDK writes logs, exception messages, telemetry records, or diagnostics, THE V3 SDK SHALL redact credential values, authorization values, security tokens, cookies, sensitive headers, and members carrying the Smithy sensitive trait.
11. WHEN an exception is returned to the caller, THE V3 SDK SHALL emit zero WARN-level or ERROR-level log events for the same failure.
12. WHEN a successful fallback or ignored background cleanup failure produces a benign warning, THE V3 SDK SHALL emit at most one equivalent warning for each category, operation, and configured suppression scope.
13. IF a response-metadata field is unavailable, THEN THE V3 SDK SHALL return the documented absent-value representation without a null dereference.
14. IF a telemetry callback fails, THEN THE AWS Pipeline SHALL preserve the call result or causal SDK exception as the terminal result.
15. IF a telemetry callback fails, THEN THE AWS Pipeline SHALL report the telemetry failure through the documented telemetry-failure channel without replacing the call result.

### Requirement 22: Release Readiness and Evidence

**User Story:** As an SDK release owner, I want traceable readiness evidence, so that V3 general availability is based on complete parity, correctness, migration, and performance results.

#### Acceptance Criteria

1. WHEN a release candidate is published, THE V3 SDK SHALL publish one uniquely associated Parity Manifest identifying that release candidate.
2. WHEN a release candidate is published, THE V3 SDK SHALL publish the exact untruncated immutable Canonical Smithy Model revision associated with that release candidate.
3. WHEN a release candidate is published, THE V3 SDK SHALL publish protocol, endpoint, Schema Fidelity Test, wire-differential, generated-versus-dynamic differential, fault-injection, concurrency, compatibility, migration, and performance evidence summaries.
4. WHEN an evidence summary is published, THE V3 SDK SHALL identify the release candidate, Canonical Smithy Model revision, test scope, execution date, executed count, passed count, failed count, omitted cases with reasons, and overall outcome.
5. WHEN a release candidate is published, THE V3 SDK SHALL publish a complete artifact dependency inventory and license report.
6. WHERE a Parity Manifest entry is classified as `ADAPTED`, THE V3 SDK SHALL publish documentation identifying the rationale, replacement, behavioral differences, and migration action.
7. WHERE a Parity Manifest entry is classified as `EXCLUDED`, THE V3 SDK SHALL publish documentation identifying the rationale, replacement when one exists, and migration action.
8. WHEN a Canonical Smithy Model update changes generated public API, THE V3 SDK SHALL identify every affected shape identifier, API delta, and Compatibility Classification before release.
9. IF release evidence contains an unclassified V2 parity gap, THEN THE V3 SDK SHALL block general availability.
10. IF release evidence contains an unapproved silent configuration drop or Wire Equivalent failure, THEN THE V3 SDK SHALL block general availability.
11. IF required release evidence is missing, incomplete, or unretrievable, THEN THE V3 SDK SHALL block general availability.
12. WHILE a release candidate remains eligible for general availability, THE V3 SDK SHALL keep every associated evidence artifact immutable and retrievable by release-candidate identifier.

## Requirements Traceability Summary

| Requested concern | Primary requirements |
|---|---|
| JDK 21+ and Maven | 1 |
| Smithy-native codegen from scratch | 2, 3 |
| No intermediate model | 3 |
| One AWS codegen package | 3 |
| Generated Schemas and SerializableStructs | 4 |
| Schema-decoupled SERDE | 5, 6 |
| Generated sync and async interfaces | 7, 8 |
| Configuration | 9 |
| SDK-owned pipeline | 10 |
| Interceptors and plugins | 11 |
| HTTP clients and implementations | 12 |
| Core libraries and dependencies | 13, 15 |
| Dynamic clients | 14 |
| Launch-time V2 parity and high-level libraries | 16 |
| V2 migration compatibility | 17 |
| Deprecated and legacy exclusions | 18 |
| Testable correctness outcomes | 19 |
| Maximum performance | 20 |
| Operational parity | 21 |
| Launch evidence | 22 |

## Investigated Sources

- Current V2 repository: root, core, codegen, HTTP clients, generated S3 clients, custom service libraries, tests, and migration tooling.
- Git branch `feature/poc/racecar`, especially `pipeline_benchmark2/project_racecar_summary.md`, async execution analysis, and bridge comparisons.
- Git branch `smithy-java-bridge-alexwoo-full`, especially `alexwoo-smithy-java-runtime.md`, `RFC-smithy-java-runtime.md`, `compatability_issues.md`, and `codegen-c2j-to-smithy/IR-REMOVAL-PLAN.md`.
- Git branch `smithy-java-spi-bridge-poc`, especially `.kiro/specs/smithy-java-spi-pipeline-poc.md` and `smithy-bridge-current-state.md`.
- `.kiro/reference/smithy-java`, including core Schema/SERDE, client pipeline, Dynamic Client, codegen, protocol, AWS, retry, endpoint, and HTTP modules.
- `.kiro/reference/aws-models` and its per-service Smithy model layout.
- `.kiro/reference/serialization-schema-decoupling.md`.
- `.kiro/reference/document-types-and-type-registries.md`.
- `.kiro/reference/Dynamic-Client.md`.
