# Implementation Plan: AWS SDK for Java V3

## Overview

Build V3 as a JDK 21 Maven reactor under `v3/`, integrated with the repository root build while retaining the existing V2 modules as compatibility, differential, and migration fixtures. Production modules follow the artifact boundaries in the design; generated service sources live under `v3/services/`; automated verification lives under `v3/test/`; migration and release tooling live under `v3/tools/`. Every step produces compiling code or executable validation, builds on prior steps, and ends with all generated, dynamic, compatibility, parity, and release paths wired into one release reactor.

All tasks are required for general availability. No task is marked optional because the requirements make the listed implementation and release gates mandatory.

## Tasks

- [x] 1. Establish the JDK 21 Maven reactor and dependency boundaries
  - [x] 1.1 Create the V3 parent and module skeleton
    - Create `v3/pom.xml`, V3 parent/plugin-management POMs, and empty compiling modules for the artifacts defined by the design; integrate the V3 aggregate into the root Maven entry point.
    - Configure `maven.compiler.release=21`, reproducible archives, exact dependency/plugin versions, and the JDK enforcer before compilation; ensure all production artifacts carry `3.x` coordinates without invoking Gradle.
    - _Requirements: 1.1, 1.3, 1.5, 1.6, 1.7, 15.2_
  - [x] 1.2 Wire source generation, packaging, and the clean release reactor
    - Configure `aws-codegen-maven-plugin` lifecycle hooks so generated service sources exist before compilation, and add aggregate build profiles for runtime, HTTP, services, high-level libraries, migration tools, tests, benchmarks, and release evidence.
    - Add an automated clean-checkout reactor test that runs the single supported command and asserts every required artifact is compiled, tested, and packaged.
    - _Requirements: 1.2, 1.3, 1.4_
  - [x] 1.3 Establish the lightweight Smithy Java runtime boundary
    - Add pinned Smithy Java dependencies to `v3/smithy-runtime` and isolate shape identity, runtime traits, `Schema`, `SerializableStruct`, `Document`, codecs, and builders from Smithy model parsing, assembly, and code generation.
    - Add classpath and class-loading tests proving generated service clients do not resolve parser/assembler classes.
    - _Requirements: 2.1, 4.4, 5.1, 6.1, 14.18, 15.3, 15.4, 15.5_
  - [x] 1.4 Implement the V3 BOM, dependency policy, and runtime version handshake
    - Create `v3/bom`, `v3/bom-internal`, generated dependency-policy metadata, and runtime module version resources; validate exact release alignment at build time and client construction.
    - Encode allowed and forbidden module edges as machine-readable rules consumed by architecture tests.
    - _Requirements: 15.1, 15.2, 15.9, 15.10, 15.11_
  - [x] 1.5 Add early architecture-enforcement tests for runtime dependency purity
    - In `v3/test/architecture-tests`, inspect Maven dependency trees and bytecode references for service modules; fail on V2 runtime, codegen, Smithy parser/model, generated protocol marshaller, concrete HTTP, telemetry exporter, or third-party logging implementation leakage.
    - Verify HTTP implementations and telemetry exporters remain opt-in and public/protected APIs expose no logging implementation types.
    - _Requirements: 15.3, 15.4, 15.5, 15.6, 15.7, 15.8, 15.9_
  - [x] 1.6 Add JDK and reactor failure-mode integration tests
    - Add Maven Invoker fixtures for JDK 21, supported newer JDKs, and an older-JDK simulation; assert older JDK rejection occurs before compilation and produces no class files.
    - Add a build audit that rejects Gradle executable invocation and verifies generated-source/package ordering.
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6, 1.7_
  - [x] 1.7 Write the property test for BOM and runtime version coherence
    - **Property 55: BOM and runtime versions are coherent.** Create `RuntimeVersionCoherencePropertyTest` with generated artifact/version inventories and conflicting module sets; compare BOM resolution and diagnostics with an independent map oracle.
    - **Validates: Requirements 15.1, 15.11**

- [x] 2. Implement canonical Smithy code generation and enforce one generator
  - [x] 2.1 Create the single AWS directed code generator and Maven wrapper
    - Implement `v3/codegen/aws-codegen` as the only AWS `DirectedCodegen` integration and `v3/codegen/aws-codegen-maven-plugin` as a lifecycle-only wrapper.
    - Define ordered integration discovery, generation settings, file manifests, symbol providers, and atomic staging/publication without a downstream semantic IR.
    - _Requirements: 2.1, 3.1, 3.2, 3.3, 3.4, 3.5_
  - [x] 2.2 Implement canonical model validation and atomic legacy-input rejection
    - Add model/source validators under `aws-codegen/.../validation` for C2J service files and V2 paginator, waiter, endpoint, example, and customization sidecars.
    - Emit service/operation/semantic diagnostics, retain unknown traits, and prevent affected service artifacts from being published on any validation failure.
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11_
  - [x] 2.3 Implement direct model traversal and static operation descriptor emission
    - Traverse Smithy `Model`, indexes, traits, symbols, and directed contexts directly; implement shape/file-local render plans only.
    - Generate static descriptor references for every operation and reject any generated runtime class-name lookup or duplicate whole-service object graph.
    - _Requirements: 3.2, 3.3, 3.4, 3.8, 3.9, 10.12_
  - [x] 2.4 Implement deterministic writers and integration conflict detection
    - Sort shapes, members, integrations, maps, and sets deterministically; remove timestamps and absolute paths; detect output/setting conflicts before publication and identify both integrations and the shape.
    - Add byte-tree comparison and conflict fixture tests in `v3/test/codegen-tests`.
    - _Requirements: 3.6, 3.7_
  - [x] 2.5 Generate exact model provenance and generation manifests
    - Emit untruncated immutable model revision, service shape ID, generator/JDK/config identity, generated file digests, and required semantic inventory into each service artifact.
    - Add schema validation and reproducibility tests for provenance resources.
    - _Requirements: 2.12, 22.2_
  - [x] 2.6 Add architecture tests for forbidden generation paths
    - Scan codegen classes, serialized resources, generated trees, and hot-path bytecode for an SDK intermediate model, C2J/sidecar consumers, duplicate generator entry points, per-operation protocol transformers, reflective operation lookup, and runtime model loading.
    - Fail the V3 reactor when any forbidden path is introduced.
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.1, 3.3, 3.4, 3.8, 3.9, 5.3, 5.4, 10.5, 10.12, 12.15, 20.22_
  - [x] 2.7 Write the property test for unknown trait preservation
    - **Property 2: Unknown trait preservation.** Generate arbitrary absolute trait IDs, targets, and Smithy node trees for canonical and runtime models; use structural Smithy-node equality as the oracle.
    - **Validates: Requirements 2.11, 14.1, 14.17**
  - [x] 2.8 Write the property test for atomic legacy input rejection
    - **Property 3: Legacy input rejection is atomic.** Generate combinations of C2J and sidecar fixtures across services; assert exact diagnostics and an empty published artifact set for every affected service.
    - **Validates: Requirements 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9**
  - [x] 2.9 Write the property test for exact provenance
    - **Property 4: Provenance is exact.** Generate long revisions and normalized/non-normalized service IDs; round-trip resources and compare exact strings and IDs without truncation.
    - **Validates: Requirements 2.12, 22.2**
  - [x] 2.10 Write the property test for deterministic generation
    - **Property 5: Generation is deterministic.** Randomize model insertion order, integration enumeration, and equivalent output roots; compare relative path sets and file bytes.
    - **Validates: Requirements 3.6**
  - [x] 2.11 Write the property test for complete integration conflict diagnostics
    - **Property 6: Integration conflicts are complete diagnostics.** Generate paired integrations with colliding settings/files and an independent collision oracle; assert both IDs and the shape ID are reported.
    - **Validates: Requirements 3.7**

- [x] 3. Generate immutable model types, schemas, service metadata, and registries
  - [x] 3.1 Implement recursive schema and operation/service schema generation
    - Add schema emitters and shared recursive holders under `aws-codegen/.../schema`; preserve exact IDs, shape types, members, targets, requiredness, defaults, constraints, traits, and operation links.
    - Generate exactly one exported schema object per public type plus one service and one operation schema per modeled entity.
    - Expose unknown traits on generated schemas keyed by exact trait shape ID, with multiple unknown traits per shape and member schemas for enum/intEnum members. Then enable the runtime-schema view of the Property 2 test (task 2.7, `UnknownTraitPreservationPropertyTest`) against generated schemas. That view is currently disabled as a known failure against the Smithy Java 1.6.1 `SchemaConverter` stand-in: it keeps one `DynamicTrait` per schema and builds no enum member schemas.
    - _Requirements: 2.11, 4.1, 4.5, 4.6, 4.7, 4.12, 14.17_
  - [x] 3.2 Implement generated immutable classes and builders
    - Add templates for final structures, unions, enums, errors, builders, copy-builders, V2-compatible accessors/equality/hash/string behavior, direct `SerializableStruct` member switches, and reflection-free builder deserialization.
    - Deeply snapshot nested collections and aggregate all missing required members into one validation exception.
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.8, 4.9, 4.10, 4.11_
  - [x] 3.3 Implement service metadata and primary type-registry generation
    - Generate `*ServiceMetadata`, static operation tables, lazy registry partitions, and exact shape-to-schema/type/builder entries for every eligible aggregate and error.
    - Add compile/golden tests for recursive models, unions, unknown enums, errors, request overrides excluded from modeled state, and registry lookup.
    - _Requirements: 4.6, 6.5, 6.6, 6.7, 6.9_
  - [x] 3.4 Write the property test for complete generated state exposure
    - **Property 8: Generated values expose complete modeled state.** Generate valid model instances and compare direct serializer events/member lookup with a canonical member-map oracle; deserialize and assert generated equality.
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.8, 4.9**
  - [x] 3.5 Write the property test for deep collection immutability
    - **Property 9: Generated collection state is deeply immutable.** Generate nested list/set/map values, retain all mutable input aliases, and attempt mutations at every reachable level; compare to a pre-build deep snapshot.
    - **Validates: Requirements 4.10**
  - [x] 3.6 Write the property test for missing-required-member aggregation
    - **Property 10: Missing required members are aggregated.** Generate nonempty subsets of required members and compare the single exception's member-ID set to the omitted subset.
    - **Validates: Requirements 4.11**
  - [x] 3.7 Write the property test for canonical model-to-schema bijection and fidelity
    - **Property 1: Canonical model-to-schema bijection and fidelity.** Generate recursive synthetic Smithy graphs with traits, defaults, constraints, and ordered members; compare generated type/schema inventories to a direct canonical-model oracle.
    - **Validates: Requirements 2.1, 2.2, 4.1, 4.5, 4.6, 4.7, 4.12, 19.3**

- [x] 4. Implement schema-driven protocols, Documents, registries, and bounded streaming
  - [x] 4.1 Implement protocol selection and shared HTTP binding plans
    - Create `v3/protocols/protocol-core` with modeled protocol discovery, singular selection, schema HTTP bindings, structured errors, and protocol/transport compatibility metadata.
    - Reject undeclared or unavailable protocols during client construction.
    - _Requirements: 5.1, 5.2, 5.6, 5.7, 5.9, 5.10, 5.12_
  - [x] 4.2 Implement all seven launch protocol runtimes
    - Implement Schema SERDE and codecs in `aws-json-protocol` (1.0/1.1), `rest-json-protocol`, `rest-xml-protocol`, `aws-query-protocol`, `ec2-query-protocol`, and `rpcv2-cbor-protocol`.
    - Cover request/response/error binding without generated per-operation marshaller or unmarshaller classes.
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.7, 5.8, 5.9, 5.10_
  - [x] 4.3 Implement incremental streaming blob and Event Stream SERDE
    - Add demand-aware encoders/decoders, bounded chunk buffers, terminal/error handling, and replay metadata in protocol/event-stream runtime areas.
    - Validate fragmented input, cancellation, malformed frames, and no whole-stream buffering.
    - _Requirements: 5.8, 5.11, 8.4, 8.5, 12.5, 12.6_
  - [x] 4.4 Implement complete Document conversion and composable registries
    - Extend/wire Smithy Java `Document` support for all finite Smithy kinds, exact discriminators, protocol context, schema coercion, generated builders, identity-preserving registry composition, and deterministic conflict errors.
    - Add focused unit tests for blob/timestamp/numeric boundaries, nested structures/unions, absent/unknown/mismatched discriminators, and conflicting entries.
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.8, 6.9, 6.10, 6.11, 6.12, 6.13, 6.14_
  - [x] 4.5 Write the property test for singular valid protocol selection
    - **Property 11: Protocol selection is singular and valid.** Generate declared/installed/explicit protocol sets; compare selection or rejection to an independent set-intersection/precedence oracle.
    - **Validates: Requirements 5.6, 5.12**
  - [x] 4.6 Write the property test for Schema SERDE round trips
    - **Property 12: Schema SERDE round trip.** Generate schema-valid values across every protocol and codec setting; compare presence, values, significant order, blobs, timestamps, unions, and events after round trip.
    - **Validates: Requirements 5.1, 5.2, 5.8**
  - [x] 4.7 Write the property test for atomic located wire errors
    - **Property 13: Invalid wire input is atomic and located.** Mutate valid encoded values at generated member paths; assert no modeled output and exact operation/shape/path diagnostics using a mutation-location oracle.
    - **Validates: Requirements 5.9, 5.10**
  - [x] 4.8 Write the property test for demand and buffering bounds
    - **Property 14: Demand and buffering bounds.** Generate chunk/page/event traces, demand deltas, and cancellation points; use a reference demand ledger and peak-buffer counter oracle.
    - **Validates: Requirements 5.11, 8.4, 8.5, 8.6, 12.5**
  - [x] 4.9 Write the property test for complete Document round trips
    - **Property 15: Complete Document round trip.** Generate every finite Smithy kind and nested combination; compare kind, exact value, discriminator, context, and significant order after schema conversion.
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
  - [x] 4.10 Write the property test for registry completeness and exact lookup
    - **Property 16: Registry completeness and exact lookup.** Generate service type closures and registry queries; compare entries to generated metadata and invoke each builder factory with generated values.
    - **Validates: Requirements 6.5, 6.6, 6.7, 6.9**
  - [x] 4.11 Write the property test for identity-preserving registry composition
    - **Property 17: Registry composition preserves identity.** Generate disjoint/nonconflicting registries and assert union lookup plus object identity of schemas, classes, and factories.
    - **Validates: Requirements 6.8**
  - [x] 4.12 Write the property test for deterministic registry/coercion failures
    - **Property 18: Registry and coercion failures are deterministic.** Generate incompatible kinds, missing/unknown discriminators, entry conflicts, and schema mismatches; compare repeated errors to a structured diagnostic oracle.
    - **Validates: Requirements 6.10, 6.11, 6.12, 6.13, 6.14**

- [x] 5. Add automated foundation and protocol aggregate validation
  - [x] 5.1 Configure and execute the foundation verification reactor profile
    - Create or configure a Maven aggregate verification profile that includes the JDK/reactor checks, dependency and architecture enforcement, canonical codegen tests, generated model/schema tests, all seven protocol suites, streaming tests, and Document/registry tests completed in tasks 1–4.
    - Execute the aggregated modules in Maven dependency order and fail the profile on any compilation, generation, unit, property, architecture, conformance, classpath, reproducibility, or packaging gate failure.
    - _Requirements: 1.1–1.7, 2.1–2.12, 3.1–3.9, 4.1–4.12, 5.1–5.12, 6.1–6.14, 15.1–15.11_

- [x] 6. Implement typed configuration, overrides, version checks, and resource ownership
  - [x] 6.1 Implement core public configuration and metadata APIs
    - Create `v3/core/sdk-core`, `aws-core`, identity, region, timeout, request/response metadata, typed option/context key, request override, exception, body, and ownership APIs.
    - Preserve manifest-classified builder names/value types and expose immutable resolved client/service configuration.
    - _Requirements: 7.11, 9.1, 9.2, 12.1, 21.1, 21.13_
  - [x] 6.2 Implement configuration resolution, provenance, close aggregation, and runtime checks
    - Resolve request override, builder, JVM property, environment, profile, and default precedence; cache client constants once; validate unsupported options/runtime versions.
    - Implement identity-deduplicated ownership and idempotent close that attempts every SDK-owned resource and aggregates all failures.
    - _Requirements: 9.3, 9.5, 9.6, 9.7, 9.8, 9.9, 9.13, 9.14, 15.10, 15.11_
  - [x] 6.3 Implement sparse per-call override merging and isolation
    - Add immutable header/query/identity/timeout/retry/interceptor/telemetry/context deltas and operation overload plumbing without copying full client configuration or serializing override state.
    - Add concurrent unit tests proving call isolation and documented option scope.
    - _Requirements: 9.4, 9.11, 9.12_
  - [x] 6.4 Write the property test for total configuration precedence
    - **Property 24: Configuration precedence is total.** Generate every subset of six configuration sources and compare selected value/provenance with a fixed precedence oracle.
    - **Validates: Requirements 9.3**
  - [x] 6.5 Write the property test for isolated scoped overrides
    - **Property 25: Overrides are isolated and scoped.** Generate concurrent calls with distinct sparse option maps; compare observed values to independent base-plus-delta maps and assert the base remains unchanged.
    - **Validates: Requirements 9.4, 9.11, 9.12**
  - [x] 6.6 Write the property test for once-per-client constant resolution
    - **Property 26: Client-constant resolution is once per client.** Generate positive call counts and instrument resolvers/plan factories; use per-client/operation invocation counters as the oracle.
    - **Validates: Requirements 9.5, 10.11**
  - [x] 6.7 Write the property test for resource ownership and close aggregation
    - **Property 27: Resource ownership and close aggregation.** Generate owned/borrowed resources, repeated closes, identity aliases, and close-failure subsets; compare attempts and suppressed failures to an ownership-set oracle.
    - **Validates: Requirements 9.6, 9.7, 9.8, 9.13, 9.14, 12.14**
  - [x] 6.8 Write the property test for unsupported-option diagnostics
    - **Property 28: Unsupported options never disappear silently.** Generate manifest dispositions and source locations; assert construction rejection and migration diagnostic equality for options lacking V3 behavior.
    - **Validates: Requirements 9.9, 9.10**

- [x] 7. Implement the HTTP SPI, provider selection, and native transports
  - [x] 7.1 Implement shared HTTP values and separate sync/async SPIs
    - Create `v3/http-client-spi` with immutable ordered headers/query entries, raw targets, sync streams/exchanges, demand-driven async bodies/exchanges, timeout/TLS/proxy/connection capabilities, cancellation, failure categories, and ownership contracts.
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8_
  - [x] 7.2 Implement deterministic HTTP provider selection and capability validation
    - Add service-provider metadata with mode/capability/priority, explicit-provider precedence, tied-priority failure, option validation, and construction rollback of acquired resources.
    - _Requirements: 12.11, 12.12, 12.13, 12.14_
  - [x] 7.3 Implement the native Apache HTTP Client 4 sync transport
    - Build `v3/http-clients/apache-client` directly on the V3 sync SPI; implement connection, TLS, proxy, timeout, ordered headers, streaming, abort, failure mapping, and ownership without V2 adapters.
    - Add transport-specific capability and lifecycle tests.
    - _Requirements: 12.1, 12.2, 12.4, 12.5, 12.6, 12.7, 12.9, 12.10, 12.13, 12.15, 16.9_
  - [x] 7.4 Implement the native Apache HTTP Client 5 transport and mode validation
    - Build `v3/http-clients/apache5-client` with native sync support and an isolated async provider only when enabled by the generated parity manifest; expose and test sync/async capabilities separately.
    - Validate HTTP/1.1 and supported HTTP/2 behavior, cancellation, buffers, TLS/proxy/timeouts, and unsupported-option diagnostics.
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10, 12.13, 12.15, 16.9_
  - [x] 7.5 Implement the native JDK URLConnection sync transport
    - Build `v3/http-clients/url-connection-client`; implement supported sync behavior and fail fast for pool, HTTP/2, or other unsupported options.
    - Add exact stream-close, timeout, proxy, ordered-header, and failure-category tests.
    - _Requirements: 12.1, 12.2, 12.4, 12.5, 12.6, 12.7, 12.9, 12.10, 12.13, 12.15, 16.9_
  - [x] 7.6 Implement the native Netty NIO async transport
    - Build `v3/http-clients/netty-nio-client`; implement event-loop-safe HTTP/1.1/HTTP/2, bounded flow control, demand-driven `ByteBuffer` handoff, acquisition/write/read cancellation, TLS/proxy/timeouts, and stable failures.
    - Add event-loop blocking and successful-cancellation deadline tests.
    - _Requirements: 8.4, 8.5, 8.8, 8.9, 8.10, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10, 12.15, 16.9_
  - [x] 7.7 Implement native CRT sync and async transports
    - Build `v3/http-clients/aws-crt-client` with separate sync/async providers, JNI resource ownership, ordered headers, bounded streams, cancellation, TLS/proxy/timeouts, and stable failure mapping without V2 adapters.
    - Add native-availability, missing-native-library, capability, and cleanup tests for both modes.
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10, 12.13, 12.14, 12.15, 16.9_
  - [x] 7.8 Implement the HTTP SPI TCK and run it for every capability mode
    - Create reusable sync and async TCK suites under `v3/test/http-client-tests`; cover request representation, repeated headers, body bounds, demand, cancellation, terminal uniqueness, errors, TLS, proxy, timeout, and ownership.
    - Register separate Apache 4 sync, Apache 5 sync/async, URLConnection sync, Netty async, and CRT sync/async executions.
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10, 12.13, 12.14_
  - [x] 7.9 Write the property test for HTTP representation fidelity
    - **Property 40: HTTP representation preserves protocol-significant data.** Generate valid HTTP/1.1 and HTTP/2 messages and round-trip them through builders/adapters; compare with a raw-component oracle.
    - **Validates: Requirements 12.2, 12.3, 12.4**
  - [x] 7.10 Write the property test for stable transport failure categories
    - **Property 41: Transport failure categories are stable.** Generate engine-specific failures for every phase across providers; compare mapped categories with a provider-independent phase table.
    - **Validates: Requirements 12.7**
  - [x] 7.11 Write the property test for deterministic default HTTP selection
    - **Property 42: Default HTTP selection is deterministic.** Generate provider sets and classpath permutations; use maximum-priority/tie-set logic as the oracle.
    - **Validates: Requirements 12.11, 12.12**
  - [x] 7.12 Write the property test for HTTP capability checking
    - **Property 43: HTTP options are capability checked.** Generate provider capability matrices and option sets; assert every unsupported pair fails before client return and names both values.
    - **Validates: Requirements 12.13**

- [x] 8. Implement AWS auth, identity, endpoints, retries, integrity, compression, and Event Stream
  - [x] 8.1 Implement Modern Auth and launch identity/configuration providers
    - Build `auth-api`, `auth-aws`, `identity-spi`, `identity-providers`, profiles, regions, container, web identity, SSO, role, and instance-metadata providers with manifest-defined precedence and refresh semantics.
    - Implement SigV4, SigV4a, S3 Express, bearer, anonymous, and modeled auth-scheme selection required by the baseline.
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.16, 13.17, 13.19, 16.10_
  - [x] 8.2 Implement canonical endpoint rule evaluation
    - Build `regions`/`endpoints` runtime support for regional, partition, FIPS, dual-stack, account, host-prefix, discovery, and endpoint-auth dimensions from canonical rules and traits.
    - Add generated parameter binding and endpoint test-vector runners.
    - _Requirements: 13.5, 19.2_
  - [x] 8.3 Implement token-based standard and adaptive retry strategies
    - Build `retries` with attempt/quota/token accounting, modeled/transport classification, bounded delay hints, schedulable async delay, and success/failure token recording.
    - _Requirements: 10.6, 10.7, 10.8, 13.11, 13.12, 13.13, 13.14, 13.15_
  - [x] 8.4 Implement checksums, compression, content encoding, and idempotency
    - Build `checksums` and pipeline feature plugins for exact modeled request/response scopes, algorithms, thresholds, validation, compression/content encoding, and one-per-call idempotency injection.
    - Reject conflicts before transmission.
    - _Requirements: 13.6, 13.7, 13.8, 13.9, 13.18_
  - [x] 8.5 Implement Event Stream framing and signing
    - Build `eventstream` framing, schema encode/decode, incremental demand, signature chaining, errors, and cancellation for sync/async transports.
    - _Requirements: 5.11, 13.10, 14.12_
  - [x] 8.6 Write the property test for modeled auth and identity precedence
    - **Property 44: Auth and identity precedence follows model and manifest.** Generate auth trait sets and eligible identity subsets; compare selection order to canonical traits and manifest tables.
    - **Validates: Requirements 13.1, 13.3**
  - [x] 8.7 Write the property test for canonical endpoint dimensions
    - **Property 45: Endpoint dimensions follow canonical rules.** Generate valid endpoint parameter combinations and compare runtime outputs with the canonical endpoint-rules evaluator.
    - **Validates: Requirements 13.5**
  - [x] 8.8 Write the property test for exact integrity and compression transformations
    - **Property 46: Integrity and compression transformations are exact.** Generate payloads, scopes, algorithms, and thresholds; use independent digest/compression implementations and corrupted responses as oracles.
    - **Validates: Requirements 13.6, 13.7, 13.8**
  - [x] 8.9 Write the property test for stable idempotency values
    - **Property 47: Idempotency values are stable across retries.** Generate requests with/without tokens and attempt counts; assert one generated value or unchanged supplied value across all serialized attempts.
    - **Validates: Requirements 13.9**
  - [x] 8.10 Write the property test for Event Stream signature chains
    - **Property 48: Event Stream signatures form the modeled chain.** Generate frame sequences and identities; verify every signature with an independent chain verifier and prior-signature state.
    - **Validates: Requirements 13.10**
  - [x] 8.11 Write the property test for bounded retry hints and classification
    - **Property 49: Retry hints and classification are bounded.** Generate valid, over-bound, and invalid hints with traits/status metadata; compare delay/classification to a table-driven oracle.
    - **Validates: Requirements 13.12, 13.13, 13.14, 13.15**
  - [x] 8.12 Write the property test for identity refresh and subsequent signing
    - **Property 50: Identity refresh changes subsequent signing identity.** Generate expiry/rejection/refresh traces; inspect signer inputs and transmission counts against a refresh-state oracle.
    - **Validates: Requirements 13.16, 13.17, 13.19**
  - [x] 8.13 Write the property test that modeled conflicts block transmission
    - **Property 51: Model conflicts block transmission.** Generate endpoint/auth/integrity/encoding conflicts; assert zero mock transport invocations and exact operation/setting diagnostics.
    - **Validates: Requirements 13.18**

- [x] 9. Implement the single V3 AWS pipeline and interceptor/plugin lifecycle
  - [x] 9.1 Implement immutable runtime plans, call state, attempt state, and typed contexts
    - Create `v3/core/aws-pipeline` `ClientRuntime`, `PipelinePlan`, compact execution/attempt contexts, call/attempt state, cancellation scope, terminal state, and cached stage metadata.
    - Keep customer data out of plans and isolate all mutable state per call/attempt.
    - _Requirements: 9.4, 9.5, 10.1, 10.8, 10.9, 10.11, 11.1, 11.6, 11.7_
  - [x] 9.2 Implement shared sync and async engine drivers
    - Implement `AwsCallEngine.executeSync` as a direct loop and `executeAsync` as a compact state machine with one caller future, configured executors/scheduler, active exchange, delay, and subscriptions.
    - Use shared semantic stages and no per-stage future graph.
    - _Requirements: 8.2, 8.8, 8.12, 10.1, 10.3, 10.4, 10.5_
  - [x] 9.3 Implement lifecycle ordering, retry ownership, and single exception mapping
    - Place validation, hooks, endpoint/identity, serialization, features, signing, HTTP acquisition/transmit/consume, checksums, deserialization, retry classification, metadata, and terminal mapping in the documented order.
    - Ensure one token authorizes one subsequent attempt, failed prerequisites skip dependents, and terminal completion follows one mapping path.
    - _Requirements: 10.2, 10.6, 10.7, 10.8, 10.9, 10.14, 10.15, 13.11_
  - [x] 9.4 Implement native interceptors and schema-driven feature plugins
    - Create typed hook contexts, modeled/transport/body replacement validation, registration-order dispatch arrays, execution/attempt scopes, thrown-cause preservation, and plan-time `AwsFeaturePlugin` contributions.
    - Do not introduce generated service coupling or a runtime V2 interceptor bridge.
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 11.10, 11.11, 11.12, 11.13, 11.14_
  - [x] 9.5 Implement comprehensive cancellation and async terminal races
    - Cancel HTTP acquisition/transmission/consumption, body subscriptions, retry delays, identity/endpoint work, and queued completion tasks after successful user cancellation; prevent later attempts/work and duplicate terminal signals.
    - _Requirements: 8.9, 8.10, 8.11, 10.15, 12.8, 14.10, 14.13_
  - [x] 9.6 Add pipeline architecture and hot-path enforcement tests
    - Scan production bytecode and instrument execution to prove exactly one pipeline, no V2 fallback/adapters, no operation class lookup, no reflection/model parsing/per-call plan construction, and no inactive interceptor/telemetry dispatch objects, timers, or counters.
    - _Requirements: 3.9, 10.1, 10.5, 10.10, 10.11, 10.12, 12.15, 20.21, 20.22, 21.5_
  - [x] 9.7 Write the property test for pipeline lifecycle partial order
    - **Property 29: Pipeline lifecycle partial order.** Generate success/failure/retry/cancellation stage traces and compare observed events with a reference directed-acyclic lifecycle oracle.
    - **Validates: Requirements 10.2, 10.14**
  - [x] 9.8 Write the property test for retry-token/attempt correspondence
    - **Property 31: Retry tokens and attempts correspond one-to-one.** Generate retry-decision sequences; compare attempt numbers and transmissions to a token-ledger state machine.
    - **Validates: Requirements 10.6, 10.7, 10.8, 13.11**
  - [x] 9.9 Write the property test for forward-only isolated attempt data
    - **Property 32: Attempt data visibility is forward-only and isolated.** Generate attempts and stage reads/writes; compare visibility with separate execution/attempt reference maps and stage positions.
    - **Validates: Requirements 10.9, 11.7**
  - [x] 9.10 Write the property test for zero inactive-extension work
    - **Property 33: Inactive extensions cost no extension work.** Generate plans without applicable hooks/publishers and use allocation/callback/timer instrumentation as a zero oracle.
    - **Validates: Requirements 10.10, 20.21, 21.5**
  - [x] 9.11 Write the property test for fail-fast protocol/transport validation
    - **Property 34: Protocol/transport capability validation is fail-fast.** Generate incompatible component pairs; assert construction failure, both IDs, and zero returned clients/resources leaked.
    - **Validates: Requirements 10.13**
  - [x] 9.12 Write the property test for exact interceptor contexts
    - **Property 35: Interceptor contexts are exact.** Generate operations, attempts, endpoints, identities, and hook points; compare received schema/context values by identity with stage outputs.
    - **Validates: Requirements 11.1, 11.5**
  - [x] 9.13 Write the property test for interceptor invocation counts and order
    - **Property 36: Interceptor invocation counts and order.** Generate ordered interceptor sets, overridden hook subsets, and attempt counts; compare event logs with a scope/frequency oracle.
    - **Validates: Requirements 11.3, 11.9, 11.10**
  - [x] 9.14 Write the property test for interceptor mutation composition
    - **Property 37: Interceptor mutation composes into the next stage.** Generate compatible replacements at each mutable hook; capture next-stage inputs and independently verify pre-sign mutations are signed.
    - **Validates: Requirements 11.4, 11.8**
  - [x] 9.15 Write the property test for execution/attempt context lifetimes
    - **Property 38: Execution context survives and attempt context does not.** Generate typed writes/reads over multi-attempt traces; compare to nested-scope reference storage.
    - **Validates: Requirements 11.6, 11.7**
  - [x] 9.16 Write the property test for interceptor failure diagnostics
    - **Property 39: Interceptor failures identify source and preserve cause.** Generate incompatible returns and thrown exceptions for every hook; assert class/hook fields and throwable identity in the cause chain.
    - **Validates: Requirements 11.13, 11.14**

- [x] 10. Generate and implement sync/async clients, paginators, waiters, presigners, and utilities
  - [x] 10.1 Generate typed sync/async client APIs and implementations
    - Add client/interface/builder/default-implementation templates for `create`, `builder`, typed operations, consumer/no-arg/streaming/override overloads, resolved configuration access, thread-safe plan fields, and direct static operation calls.
    - Preserve manifest-classified FQCNs, signatures, exceptions, and behavior.
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.11, 7.12, 8.1, 8.2, 8.3, 8.13, 17.1, 17.2_
  - [x] 10.2 Generate sync/async paginators and waiters
    - Generate token-driven sync iterables and demand-driven async publishers; generate sync/async waiters using exact client configuration, configured scheduler, ownership, modeled acceptors, and no caller-thread blocking.
    - _Requirements: 7.8, 7.9, 8.6, 8.7, 16.5_
  - [x] 10.3 Generate Modern Auth presigners and service utilities
    - Emit manifest-classified presigner/utility entry points, service-specific schema/plugin metadata, and signing-without-transmission plans; exclude legacy signer/presigner injection.
    - _Requirements: 7.10, 16.5, 18.1, 18.2, 18.3_
  - [x] 10.4 Add async execution, backpressure, event-loop, cancellation, and common-pool tests
    - Use controllable transports/executors to prove futures return incomplete before terminal completion, exact demand, no event-loop blocking, configured-executor use, causal failures, and comprehensive cancellation.
    - _Requirements: 8.2, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10, 8.11, 8.12, 8.13_
  - [x] 10.5 Add concrete generated API compile and compatibility fixtures
    - Generate synthetic, DynamoDB, and S3 APIs into `v3/test/codegen-generated-classes-test`; compile preserved builders/clients/overloads/streams/paginators/waiters/presigners/utilities and assert adapted/excluded calls produce manifest-linked diagnostics.
    - Execute selected compiled V2 caller bytecode against V3 where binary preservation is declared.
    - _Requirements: 4.2, 4.3, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 8.1, 8.3, 16.5, 17.1, 17.2, 19.12_
  - [x] 10.6 Write the property test for modeled paginator termination
    - **Property 19: Paginator termination follows modeled tokens.** Generate finite page/token graphs and demand traces; compare emissions and stop points to the canonical pagination-expression oracle.
    - **Validates: Requirements 7.8, 8.6**
  - [x] 10.7 Write the property test for exact derived-client configuration
    - **Property 20: Derived clients share exact configuration.** Generate client configurations/ownership sets; assert waiter, paginator calls, presigner, and utility observe identical configuration objects and decisions.
    - **Validates: Requirements 7.9, 7.11**
  - [x] 10.8 Write the property test for early async return
    - **Property 21: Async calls return before terminal completion.** Generate blocked transport stages for generated and dynamic calls; use a latch oracle proving an incomplete future was returned before release.
    - **Validates: Requirements 8.2, 14.9**
  - [x] 10.9 Write the property test for reachable async causes
    - **Property 22: Async causal failures remain reachable.** Generate failures at every async child stage; traverse exception chains and assert original SDK throwable identity remains reachable.
    - **Validates: Requirements 8.11**
  - [x] 10.10 Write the property test for comprehensive terminal cancellation
    - **Property 23: Cancellation is terminal and comprehensive.** Generate cancellation points and racing callbacks; compare handle cancellation, scheduled work, and terminal transitions with a single-winner state-machine oracle.
    - **Validates: Requirements 8.9, 8.10, 10.15, 14.10, 14.13**

- [ ] 11. Add automated core-runtime and generated-client aggregate validation
  - [x] 11.1 Configure and execute the core-runtime verification reactor profile
    - Create or configure a Maven aggregate verification profile that extends the foundation profile with configuration/resource tests, every HTTP implementation TCK mode, AWS auth/endpoint/retry/integrity suites, pipeline/interceptor architecture and lifecycle suites, and generated sync/async client compile, compatibility, streaming, backpressure, event-loop, executor, and cancellation tests completed in tasks 6–10.
    - Execute all included modules in Maven dependency order and fail the profile on any test, capability, lifecycle, ownership, compatibility, architecture, bounded-streaming, or generated-client gate failure.
    - _Requirements: 3.9, 4.2–4.3, 5.11, 7.1–7.12, 8.1–8.13, 9.1–9.14, 10.1–10.15, 11.1–11.14, 12.1–12.15, 13.1–13.19, 14.10, 14.12–14.13, 15.10–15.11, 16.5, 16.9–16.10, 17.1–17.2, 19.12, 20.21–20.22, 21.1, 21.5, 21.13_

- [x] 12. Implement the optional-dependency dynamic model loader and Dynamic Client
  - [x] 12.1 Implement isolated runtime model parsing and dynamic descriptors
    - Create `v3/dynamic/dynamic-model` for Smithy JSON AST parsing/model assembly/schema conversion and `DynamicAwsOperation` creation with exact unknown traits.
    - Keep parser/model dependencies absent from generated service runtime trees.
    - Replace the `SchemaConverter` stand-in in the Property 2 runtime-schema view with `dynamic-model` schemas, which must expose unknown traits by exact trait ID, including on enum members.
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 14.17, 14.18_
  - [x] 12.2 Implement sync/async Dynamic Client invocation on `AwsCallEngine`
    - Create `v3/dynamic/dynamic-client` with lexical service/operation diagnostics, finite `Document` input/output, exact validation, shared plans/configuration/overrides/plugins, and sync/async terminal semantics.
    - _Requirements: 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 14.8, 14.9, 14.10, 14.11, 14.14, 14.15, 14.19_
  - [x] 12.3 Implement dynamic streaming, pagination, waiters, and cancellation
    - Route streaming blobs/Event Streams through native V3 bodies, expose modeled dynamic pagination/waiting, preserve demand and terminal behavior, and propagate cancellation to the shared pipeline.
    - _Requirements: 14.8, 14.10, 14.12, 14.13, 14.15_
  - [x] 12.4 Add dynamic dependency and shared-pipeline architecture tests
    - Verify dynamic parser classes exist only in optional dynamic artifacts, generated services do not load them, dynamic calls enter the same engine/stages, and no alternate pipeline or V2 path is reachable.
    - _Requirements: 10.1, 10.4, 10.5, 14.14, 14.18, 15.5_
  - [x] 12.5 Write the property test for equivalent client-projection lifecycles
    - **Property 30: Client projections have equivalent lifecycle.** Generate equivalent sync, async, and dynamic finite calls; normalize transport-only events and compare semantic traces.
    - **Validates: Requirements 10.4, 14.14**
  - [x] 12.6 Write the property test for exact dynamic service/operation selection
    - **Property 52: Dynamic service and operation selection is exact.** Generate zero/single/multiple-service models and operation names; compare choices and lexical diagnostics with a sorted model inventory oracle.
    - **Validates: Requirements 14.2, 14.3, 14.4, 14.5, 14.6, 14.7**
  - [x] 12.7 Write the property test for pre-transport dynamic input validation
    - **Property 53: Dynamic finite input validates before transport.** Generate conforming and schema-mutated Documents; compare validation to schema traversal and assert zero transport calls for invalid input.
    - **Validates: Requirements 14.11, 14.19**
  - [x] 12.8 Write the property test for generated/dynamic wire equivalence
    - **Property 54: Generated and dynamic calls are wire equivalent.** Generate equivalent modeled inputs/configuration and deterministic nondeterministic metadata; compare method, URI, ordered headers, body, frames, status interpretation, and modeled outputs byte-for-byte.
    - **Validates: Requirements 14.16, 19.6**

- [x] 13. Implement fleet generation, the Parity Manifest, and API compatibility gates
  - [x] 13.1 Implement machine-readable V2 baseline inventory and Parity Manifest generation
    - Create `v3/tools/parity-manifest` scanners for public artifacts/APIs/behaviors/configuration/HTTP modes and emit exactly one classification with compatibility dimensions, rationale, replacement, migration action, and evidence IDs.
    - _Requirements: 12.10, 16.1, 16.2, 16.3, 16.11, 16.13, 17.12_
  - [x] 13.2 Generate every non-deprecated AWS service from canonical models
    - Wire canonical model revision inputs into `v3/services/pom.xml`, generate/package all baseline services, and fail the fleet build for missing semantics, missing clients/features, or provenance mismatch.
    - _Requirements: 2.1, 2.2, 2.10, 4.1, 7.1, 8.1, 16.4, 16.5_
  - [x] 13.3 Implement launch-surface and behavior inventory gates
    - Compare generated clients, models, exceptions, streams, Event Streams, endpoint providers, customizations, paginators, waiters, presigners, utilities, HTTP modes, configuration, telemetry, logging, profile, region, credentials, checksums, compression, module path, and native-image capabilities with non-deprecated manifest entries.
    - Block packaging for missing preserved/adapted replacements or evidence hooks.
    - _Requirements: 16.4, 16.5, 16.9, 16.10, 16.12, 16.14, 16.15_
  - [x] 13.4 Implement source, descriptor, hierarchy, and behavioral compatibility test generation
    - Generate compile fixtures, binary linkage invocations, signature/hierarchy comparisons, and behavior fixtures per manifest dimension for preserved, adapted, and excluded entries.
    - _Requirements: 17.1, 17.2, 17.3, 19.12_
  - [x] 13.5 Write the property test for complete manifest classification
    - **Property 56: Manifest classifications form a complete partition.** Generate inventories and classification records; compare uniqueness/completeness and required adapted/excluded fields to a schema oracle.
    - **Validates: Requirements 16.2, 16.3, 16.11, 16.13**
  - [x] 13.6 Write the property test that adapted replacements exist
    - **Property 57: Adapted replacements exist in the candidate.** Generate adapted entries and candidate artifact/API indexes; resolve every replacement and compare unresolved IDs with an independent index.
    - **Validates: Requirements 16.12**
  - [x] 13.7 Write the property test for declared preserved API descriptors
    - **Property 58: Preserved API descriptors match their declared dimension.** Generate/source baseline signatures and compare FQCN, member name, JVM descriptor, overload set, builder shape, and exception hierarchy by compatibility dimension.
    - **Validates: Requirements 17.1, 17.2**
  - [x] 13.8 Write the property test for generated surface closure
    - **Property 7: Generated surface matches modeled and parity closures.** Generate service/public closures and parity entries; compare emitted types, operations, clients, overloads, utilities, and static descriptor references with a set-based oracle.
    - **Validates: Requirements 3.8, 7.1, 7.4, 7.5, 7.6, 8.1, 8.3, 16.5**

- [x] 14. Implement required high-level libraries and their launch-parity suites
  - [x] 14.1 Implement DynamoDB Enhanced sync and async clients
    - Create V3 mapper/schema/table/index/expression, batch, transaction, pagination, extension, and Document implementations over public DynamoDB V3 APIs; add sync/async parity, concurrency, and fault tests.
    - _Requirements: 16.6, 19.7, 19.9_
  - [x] 14.2 Implement S3 Transfer Manager
    - Create upload/download/directory/multipart/progress/cancellation/resume/failure-cleanup flows over `S3AsyncClient`, replayable body factories, paginators, and cancellation scopes; add bounded-memory and injected-failure tests.
    - _Requirements: 16.7, 19.7, 19.8, 19.9, 19.11_
  - [x] 14.3 Implement the IAM policy builder
    - Port manifest-classified policy construction, validation, equality, and serialization behavior to V3 public/core APIs with compatibility fixtures.
    - _Requirements: 16.8, 17.1, 19.12_
  - [x] 14.4 Implement S3 event-notification models and registry integration
    - Generate/implement event-notification values, schemas, primary registry composition, parsing/serialization, unknown-field behavior, and compatibility tests.
    - _Requirements: 6.8, 16.8, 17.1, 19.12_
  - [x] 14.5 Implement SNS message manager
    - Port message parsing, signature verification integration, safe diagnostics, and manifest-classified behavior with deterministic fixtures.
    - _Requirements: 16.8, 17.1, 19.12, 21.10_
  - [x] 14.6 Add high-level-library parity, fault, concurrency, and compatibility gates
    - Generate manifest-driven test matrices across DynamoDB Enhanced, Transfer Manager, IAM, S3 notifications, and SNS; assert preserved/adapted behavior, cancellation, cleanup, and source/binary claims.
    - _Requirements: 16.6, 16.7, 16.8, 16.12, 16.14, 16.15, 19.7, 19.8, 19.9, 19.12_

- [x] 15. Implement Maven-delivered V2 migration recipes, diagnostics, and corpus validation
  - [x] 15.1 Implement the migration engine, Maven plugin, and manifest reader
    - Build `v3/tools/v2-migration` with source parsing, effective dependency/BOM inventory, manifest-backed dispositions, deterministic edits, source locations, and machine-readable reports.
    - _Requirements: 17.4, 17.5, 17.10, 17.11, 17.12_
  - [x] 15.2 Implement dependency, import, builder, client, and configuration recipes
    - Transform Maven/JDK settings, BOM/dependencies, imports, model/client builders, request overrides, region/identity/configuration, and supported option changes; leave uncertain nodes unchanged with diagnostics.
    - _Requirements: 9.10, 17.3, 17.4, 17.5, 17.6, 17.7, 17.9_
  - [x] 15.3 Implement interceptor, HTTP, streaming, and high-level-library recipes
    - Convert typed interceptor contexts/scopes, HTTP clients/options, async body/response types, paginators/waiters, DynamoDB Enhanced, Transfer Manager, IAM, event notification, and SNS usages according to manifest classifications.
    - _Requirements: 11.12, 17.4, 17.5, 17.6, 17.7, 17.9, 17.11_
  - [x] 15.4 Implement Modern Auth/Retry replacements and blocking exclusion diagnostics
    - Transform safe modern APIs, diagnose excluded legacy signer/presigner/retry/deprecated APIs, preserve unsafe source unchanged, and include rationale/replacement/documentation IDs.
    - Add public API scans proving excluded contracts are absent from V3.
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 18.7, 18.8, 18.9_
  - [x] 15.5 Build and execute the V2 migration corpus
    - Add representative and synthetic V2 applications under `v3/test/v2-migration-tests`; test preserved-only dependency/JDK upgrades, adapted transformations, excluded diagnostics, transformed JDK 21 compilation, and behavioral suites.
    - Require every encountered item to have exactly one disposition and publish corpus results into the compatibility report.
    - _Requirements: 17.3, 17.7, 17.8, 17.9, 17.10, 17.11, 17.12_
  - [x] 15.6 Write the property test for total exclusive migration dispositions
    - **Property 59: Migration dispositions are total and exclusive.** Generate source inventories across all required categories; compare reported preserved/transformed/unresolved partitions with manifest lookups.
    - **Validates: Requirements 17.4, 17.10, 17.11**
  - [x] 15.7 Write the property test for non-destructive unsafe migration
    - **Property 60: Unsafe migration is non-destructive.** Generate unsupported AST constructs; compare pre/post source subtrees byte-for-byte and validate all required diagnostic fields.
    - **Validates: Requirements 17.9, 18.9**
  - [x] 15.8 Write the property test for modern exclusion diagnostics
    - **Property 61: Excluded facilities receive modern diagnostics.** Generate excluded signer/retry usages and manifest replacements; assert exact Modern Auth/Retry replacement IDs and source locations.
    - **Validates: Requirements 18.7, 18.8**

- [x] 16. Implement response metadata, telemetry, logging, user-agent, and safe diagnostics
  - [x] 16.1 Implement response/exception metadata propagation
    - Add request IDs, extended IDs, status, service code, attempts, transport metadata, and documented absent values to modeled responses and SDK exceptions through the terminal pipeline.
    - _Requirements: 21.1, 21.13_
  - [x] 16.2 Implement opt-in telemetry SPI and exporters
    - Build `telemetry-spi`, terminal call/attempt records, required dimensions, isolated callback failure handling, and separate exporter artifacts; allocate no telemetry state when absent.
    - _Requirements: 15.7, 21.2, 21.3, 21.4, 21.5, 21.14, 21.15_
  - [x] 16.3 Implement logging facade, redaction, user-agent contribution, and warning suppression
    - Route runtime logs through a facade, prohibit runtime INFO, avoid duplicate caller-visible WARN/ERROR logs, lazily redact credentials/auth/tokens/cookies/sensitive members, and include exactly contributing user-agent feature IDs.
    - _Requirements: 15.8, 21.6, 21.7, 21.8, 21.9, 21.10, 21.11, 21.12_
  - [x] 16.4 Write the property test for response metadata presence
    - **Property 66: Response metadata preserves supplied presence.** Generate every metadata-field subset; compare response/exception accessors and absent representations with the input presence map.
    - **Validates: Requirements 21.1, 21.13**
  - [x] 16.5 Write the property test for telemetry/lifecycle correspondence
    - **Property 67: Telemetry records correspond to lifecycle.** Generate calls with arbitrary positive attempt counts/outcomes; compare terminal record counts and dimensions to lifecycle traces.
    - **Validates: Requirements 21.2, 21.3, 21.4**
  - [x] 16.6 Write the property test for exact user-agent contribution
    - **Property 68: User-agent contribution is exact.** Generate contributing component sets; compare parsed user-agent tokens to required base dimensions plus exactly that set.
    - **Validates: Requirements 21.6, 21.7**
  - [x] 16.7 Write the property test for diagnostic redaction
    - **Property 69: Sensitive data is absent from diagnostics.** Generate nested sensitive values, headers, credentials, logs, exceptions, telemetry, and diagnostics with unique canaries; assert no canary appears in rendered output.
    - **Validates: Requirements 21.10**
  - [x] 16.8 Write the property test for nonduplicative logging
    - **Property 70: Logging does not duplicate caller-visible failures.** Generate returned failures and repeated benign warning keys; compare captured log counts to zero/semaphore suppression rules.
    - **Validates: Requirements 21.11, 21.12**
  - [x] 16.9 Write the property test for isolated telemetry failures
    - **Property 71: Telemetry failures are isolated.** Generate callback failures over successful and failed calls; assert terminal result identity and exact telemetry-failure-channel delivery.
    - **Validates: Requirements 21.14, 21.15**

- [x] 17. Add automated dynamic, parity, migration, and observability aggregate validation
  - [x] 17.1 Configure and execute the product-parity verification reactor profile
    - Create or configure a Maven aggregate verification profile that extends the core-runtime profile with dynamic-model isolation and differential suites, fleet generation and Parity Manifest gates, high-level-library parity/fault/concurrency suites, migration corpus transformation/compile/behavior tests, excluded-API scans, and metadata/telemetry/logging/redaction suites completed in tasks 12–16.
    - Execute all included modules in Maven dependency order and fail the profile on any dependency-isolation, generated/dynamic equivalence, fleet completeness, parity, compatibility, migration, high-level-library, telemetry, logging, or safe-diagnostics gate failure.
    - _Requirements: 2.1–2.2, 2.10–2.11, 4.1–4.3, 6.8, 7.1–7.10, 8.1, 8.3, 9.9–9.10, 10.1, 10.4–10.5, 11.12, 12.10, 14.1–14.19, 15.5, 16.1–16.15, 17.1–17.12, 18.1–18.9, 19.6–19.12, 21.1–21.15_

- [ ] 18. Implement cross-cutting conformance and release-gate test systems
  - [x] 18.1 Implement protocol and endpoint conformance runners
    - Build `v3/test/protocol-tests` and endpoint test modules that discover and execute every applicable pinned Smithy protocol case and canonical endpoint case across all seven runtimes and generated services.
    - Fail on test timeouts, failures, or unapproved omissions.
    - _Requirements: 19.1, 19.2, 19.14, 19.15_
  - [x] 18.2 Implement fleet schema-fidelity and wire-differential suites
    - Compare every generated schema to canonical shapes and compare V3 requests/success/errors with canonical expectations and the frozen V2 baseline across every protocol/binding category.
    - Require intentional differences to carry an adapted/excluded manifest ID.
    - _Requirements: 19.3, 19.4, 19.5, 19.14, 19.15_
  - [x] 18.3 Write the property test for classified intentional wire differences
    - **Property 62: Intentional wire differences are classified.** Generate differential expectation records and manifests; assert every intentional delta resolves to an adapted/excluded entry.
    - **Validates: Requirements 19.5**
  - [x] 18.4 Implement generated-versus-dynamic differential matrices
    - Execute equivalent generated/dynamic finite and streaming calls for every protocol; compare modeled results and normalized wire components with deterministic credentials/time.
    - _Requirements: 14.16, 19.6, 19.14_
  - [x] 18.5 Implement deterministic pipeline fault injection
    - Build controllable endpoint, identity, signer, scheduler, body, and transport fakes for connection failure, partial request/response, timeout, throttling, retry hints, malformed success, modeled/unmodeled errors, cancellation, and resolution failures.
    - Assert terminal outcome, attempts, delays, cancellation, cleanup, metadata, and no duplicate completion.
    - _Requirements: 19.7, 19.8, 19.14_
  - [ ] 18.6 Write the property test for model-predicted fault outcomes
    - **Property 63: Fault outcomes follow pipeline state.** Generate bounded fault/retry/cancellation traces; compare execution results with an independent pipeline state-machine oracle.
    - **Validates: Requirements 19.7, 19.8**
  - [x] 18.7 Implement barrier-controlled concurrency suites
    - Test clients, identity resolvers, transports, interceptors, retries, paginators, waiters, and high-level libraries with documented simultaneous/total counts, overlap barriers, and bounded timeouts.
    - _Requirements: 7.12, 8.13, 19.9, 19.10, 19.14_
  - [x] 18.8 Implement bounded-memory streaming and Reactive Streams suites
    - Run demand/cancellation TCKs and transfer payloads larger than heap, including the 1 GiB/256 MiB case, with exact byte/order checks, bounded buffers, early close, and no out-of-memory failure.
    - _Requirements: 5.11, 8.4, 8.5, 12.5, 19.11, 19.14, 20.20_
  - [x] 18.9 Implement module-path and supported native-image smoke modules
    - Build dedicated JPMS and native-image fixtures that resolve minimal generated clients, construct each supported transport mode, and complete a modeled call without forbidden dependencies.
    - _Requirements: 16.10, 19.13, 19.14_
  - [ ] 18.10 Wire release-test discovery, timeout, and omission enforcement
    - Generate the expected release-gate test inventory from protocols, models, manifest, transports, services, and deployment modes; fail the Maven release reactor for missing, failed, timed-out, or unapproved-skipped cases.
    - _Requirements: 16.14, 16.15, 19.14, 19.15_

- [ ] 19. Implement the reproducible performance harness and performance gates
  - [ ] 19.1 Implement forked client/mock-service benchmark orchestration
    - Extend `v3/test/sdk-benchmarks` from the repository benchmark infrastructure with separate pinned processes, host/JDK/JVM/power/background metadata, explicit transports, all protocols, sync/async/dynamic paths, payload/outcome/stream/Event Stream scenarios, and concurrency 1/8/32.
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.10_
  - [ ] 19.2 Implement interleaving, steady-state, null experiments, and validity checks
    - Run 5–10 interleaved repetitions, JIT stability gates, at least 30-second samples, same-artifact null cells, operation/host-policy validation, and cell invalidation.
    - _Requirements: 20.6, 20.7, 20.8, 20.9, 20.23, 20.24, 20.25, 20.26, 20.27, 20.28_
  - [ ] 19.3 Implement metrics, statistics, references, and machine-readable reports
    - Collect all required CPU/allocation/latency/throughput/startup/class/artifact/heap metrics; compute equal-weight geometric means and confidence intervals against V2, Racecar, and pinned Smithy Java references.
    - _Requirements: 20.10, 20.11, 20.12, 20.13, 20.14, 20.15, 20.16, 20.17, 20.18, 20.19, 20.23_
  - [ ] 19.4 Implement automated performance and hot-path release gates
    - Enforce every CPU/allocation/async/dynamic/startup/streaming boundary and invalid-collection rule; add profiled assertions for zero inactive-extension allocations and no reflection/model lookup/per-call plan construction.
    - _Requirements: 20.12, 20.13, 20.14, 20.15, 20.16, 20.17, 20.18, 20.19, 20.20, 20.21, 20.22, 20.29_
  - [ ] 19.5 Write the property test for benchmark partitioning and scheduling
    - **Property 64: Performance harness partitions and schedules cells correctly.** Generate scenarios/ranges/plans; compare unique size classification, repetition counts, interleaving, and null-cell coverage with a combinatorial oracle.
    - **Validates: Requirements 20.2, 20.6, 20.8**
  - [ ] 19.6 Write the property test for performance statistics and gating
    - **Property 65: Performance statistics are computed and gated correctly.** Generate positive measurement sets, invalid samples, and known ratios; compare means, stability, confidence intervals, half-widths, null ratios, validity, and release decisions with an independent statistics oracle.
    - **Validates: Requirements 20.9, 20.11, 20.23, 20.24, 20.25, 20.26, 20.27, 20.28, 20.29**

- [ ] 20. Implement machine-generated release evidence and fail-closed GA decisions
  - [ ] 20.1 Implement release-candidate evidence schemas and immutable artifact assembly
    - Create `v3/tools/release-evidence` schemas and generators for candidate identity, exact model revision, parity manifest, test/performance summaries, artifact inventory, licenses, and immutable content-addressed output.
    - _Requirements: 22.1, 22.2, 22.3, 22.4, 22.5, 22.12_
  - [ ] 20.2 Generate all candidate test and migration evidence summaries
    - Ingest protocol, endpoint, schema, wire, generated/dynamic, fault, concurrency, compatibility, migration, and benchmark machine reports; emit scope/date/count/pass/fail/omission/outcome fields joined to one candidate.
    - _Requirements: 16.14, 17.12, 22.1, 22.3, 22.4_
  - [ ] 20.3 Generate adapted/excluded records, dependency/license inventory, and model API deltas
    - Produce machine-readable rationale/replacement/difference/migration fields, exact dependency/license inventories, and shape/API/classification deltas for canonical model updates.
    - _Requirements: 16.11, 16.13, 22.5, 22.6, 22.7, 22.8_
  - [ ] 20.4 Implement the fail-closed release decision engine
    - Join manifest entries, shipped APIs, configuration/wire outcomes, all required evidence, omission approvals, migration results, and performance validity; block GA on every required gap and emit machine-readable causes.
    - _Requirements: 16.15, 19.15, 20.29, 22.9, 22.10, 22.11_
  - [ ] 20.5 Write the property test for uniquely associated complete evidence
    - **Property 72: Release evidence is uniquely associated and complete.** Generate candidate/evidence record sets; compare uniqueness, required categories, identity consistency, fields, and counts with an evidence-schema oracle.
    - **Validates: Requirements 22.1, 22.3, 22.4**
  - [ ] 20.6 Write the property test for complete adapted/excluded release records
    - **Property 73: Adapted/excluded documentation is complete.** Generate classified entries and machine-published records; validate required rationale, replacements, differences, and migration actions by classification.
    - **Validates: Requirements 22.6, 22.7**
  - [ ] 20.7 Write the property test for complete model API-delta classification
    - **Property 74: Model API deltas are fully classified.** Generate before/after model/API inventories; compare emitted affected shape IDs and deltas to an independent set difference and classification oracle.
    - **Validates: Requirements 22.8**
  - [ ] 20.8 Write the property test for fail-closed GA decisions
    - **Property 75: General-availability decisions fail closed.** Generate candidates with every combination of parity, configuration, wire, evidence, omission, collection, and performance defects; compare the decision with an all-required-predicates oracle.
    - **Validates: Requirements 16.15, 19.15, 20.25, 20.26, 20.27, 20.28, 20.29, 22.9, 22.10, 22.11**

- [ ] 21. Wire and execute the complete release-gate reactor
  - [ ] 21.1 Configure and run the fail-closed general-availability reactor profile
    - Create or configure the top-level Maven release-gate profile to compose the foundation, core-runtime, product-parity, cross-cutting conformance, benchmark, and release-evidence profiles; bind the complete generated service fleet, all required test inventories, approved-omission checks, performance validity/boundary checks, evidence assembly, and the general-availability decision engine into dependency-ordered reactor phases.
    - Execute the complete clean release-gate reactor and fail the build on any missing, skipped without approval, timed-out, invalid, failed, incomplete, or unretrievable required gate or evidence artifact.
    - _Requirements: 1.1–1.7, 2.1–2.12, 3.1–3.9, 4.1–4.12, 5.1–5.12, 6.1–6.14, 7.1–7.12, 8.1–8.13, 9.1–9.14, 10.1–10.15, 11.1–11.14, 12.1–12.15, 13.1–13.19, 14.1–14.19, 15.1–15.11, 16.1–16.15, 17.1–17.12, 18.1–18.9, 19.1–19.15, 20.1–20.29, 21.1–21.15, 22.1–22.12_

## Notes

- Every leaf task is required; none is optional for the specified general-availability scope.
- Each correctness property has one primary jqwik property-test task with explicit generators and an independent oracle. Shared generator libraries may be reused, but each property remains independently executable and reported.
- Existing V2 code, Racecar benchmarks, Smithy Java references, and current test modules are fixtures/oracles; production V3 code must remain under the V3 reactor and must not depend on V2 runtime paths.
- Aggregate validation subtasks appear in the dependency graph as serialized phase boundaries after the implementation and test tasks they verify.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4"] },
    { "id": 2, "tasks": ["1.5", "1.6", "1.7"] },
    { "id": 3, "tasks": ["2.1"] },
    { "id": 4, "tasks": ["2.2", "2.3"] },
    { "id": 5, "tasks": ["2.4", "2.5", "2.6"] },
    { "id": 6, "tasks": ["2.7", "2.8", "2.9", "2.10", "2.11"] },
    { "id": 7, "tasks": ["3.1"] },
    { "id": 8, "tasks": ["3.2"] },
    { "id": 9, "tasks": ["3.3"] },
    { "id": 10, "tasks": ["3.4", "3.5", "3.6", "3.7"] },
    { "id": 11, "tasks": ["4.1"] },
    { "id": 12, "tasks": ["4.2", "4.4"] },
    { "id": 13, "tasks": ["4.3"] },
    { "id": 14, "tasks": ["4.5", "4.6", "4.7", "4.8", "4.9", "4.10", "4.11", "4.12"] },
    { "id": 15, "tasks": ["5.1"] },
    { "id": 16, "tasks": ["6.1"] },
    { "id": 17, "tasks": ["6.2", "6.3"] },
    { "id": 18, "tasks": ["6.4", "6.5", "6.6", "6.7", "6.8"] },
    { "id": 19, "tasks": ["7.1"] },
    { "id": 20, "tasks": ["7.2"] },
    { "id": 21, "tasks": ["7.3", "7.4", "7.5", "7.6", "7.7"] },
    { "id": 22, "tasks": ["7.8"] },
    { "id": 23, "tasks": ["7.9", "7.10", "7.11", "7.12"] },
    { "id": 24, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5"] },
    { "id": 25, "tasks": ["8.6", "8.7", "8.8", "8.9", "8.10", "8.11", "8.12", "8.13"] },
    { "id": 26, "tasks": ["9.1"] },
    { "id": 27, "tasks": ["9.2"] },
    { "id": 28, "tasks": ["9.3"] },
    { "id": 29, "tasks": ["9.4", "9.5"] },
    { "id": 30, "tasks": ["9.6"] },
    { "id": 31, "tasks": ["9.7", "9.8", "9.9", "9.10", "9.11", "9.12", "9.13", "9.14", "9.15", "9.16"] },
    { "id": 32, "tasks": ["10.1"] },
    { "id": 33, "tasks": ["10.2", "10.3"] },
    { "id": 34, "tasks": ["10.4", "10.5"] },
    { "id": 35, "tasks": ["10.6", "10.7", "10.8", "10.9", "10.10"] },
    { "id": 36, "tasks": ["11.1"] },
    { "id": 37, "tasks": ["12.1"] },
    { "id": 38, "tasks": ["12.2"] },
    { "id": 39, "tasks": ["12.3", "12.4"] },
    { "id": 40, "tasks": ["12.5", "12.6", "12.7", "12.8"] },
    { "id": 41, "tasks": ["13.1"] },
    { "id": 42, "tasks": ["13.2", "13.4"] },
    { "id": 43, "tasks": ["13.3"] },
    { "id": 44, "tasks": ["13.5", "13.6", "13.7", "13.8"] },
    { "id": 45, "tasks": ["14.1", "14.2", "14.3", "14.4", "14.5"] },
    { "id": 46, "tasks": ["14.6"] },
    { "id": 47, "tasks": ["15.1"] },
    { "id": 48, "tasks": ["15.2", "15.3", "15.4"] },
    { "id": 49, "tasks": ["15.5"] },
    { "id": 50, "tasks": ["15.6", "15.7", "15.8"] },
    { "id": 51, "tasks": ["16.1", "16.2", "16.3"] },
    { "id": 52, "tasks": ["16.4", "16.5", "16.6", "16.7", "16.8", "16.9"] },
    { "id": 53, "tasks": ["17.1"] },
    { "id": 54, "tasks": ["18.1", "18.2", "18.4", "18.5", "18.7", "18.8", "18.9"] },
    { "id": 55, "tasks": ["18.3", "18.6", "18.10"] },
    { "id": 56, "tasks": ["19.1"] },
    { "id": 57, "tasks": ["19.2", "19.3"] },
    { "id": 58, "tasks": ["19.4"] },
    { "id": 59, "tasks": ["19.5", "19.6"] },
    { "id": 60, "tasks": ["20.1"] },
    { "id": 61, "tasks": ["20.2", "20.3"] },
    { "id": 62, "tasks": ["20.4"] },
    { "id": 63, "tasks": ["20.5", "20.6", "20.7", "20.8"] },
    { "id": 64, "tasks": ["21.1"] }
  ]
}
```
