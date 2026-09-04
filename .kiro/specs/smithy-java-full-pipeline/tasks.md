# Implementation Plan: smithy-java Full Pipeline

## Overview

Expand the existing `SmithyJavaPipeline` proof-of-concept into a production-ready execution pipeline by implementing credential bridging, SigV4 signing, auth scheme resolution, full interceptor pipeline, endpoint resolution, retry, multi-protocol support, and exception mapping. The core architectural approach is to delegate to smithy-java's `Client.call()` rather than re-implementing pipeline stages.

## Tasks

- [x] 1. Add Maven dependencies and set up project structure
  - [x] 1.1 Add new smithy-java Maven dependencies to `core/smithy-java-bridge/pom.xml`
    - Add `software.amazon.smithy.java:aws-sigv4` (SigV4 signing)
    - Add `software.amazon.smithy.java:aws-auth-api` (AwsCredentialsIdentity, AwsCredentialsResolver)
    - Add `software.amazon.smithy.java:aws-client-core` (AWS client settings, RegionSetting)
    - Add `software.amazon.smithy.java:aws-client-restjson` (RestJsonClientProtocol)
    - Add `software.amazon.smithy.java:aws-client-restxml` (RestXmlClientProtocol)
    - Add `software.amazon.smithy.java:client-rpcv2` (RpcV2CborProtocol)
    - Add `software.amazon.smithy.java:client-rulesengine` (endpoint rules)
    - Add `software.amazon.smithy.java:aws-client-rulesengine` (AWS endpoint rules)
    - Add `net.jqwik:jqwik` as a test dependency for property-based tests
    - _Requirements: 2.1, 6.1, 7.2, 8.1–8.6_

  - [x] 1.2 Create package directories for new components
    - Create `auth` sub-package: `software.amazon.awssdk.bridge.smithyjava.auth`
    - Create `retry` sub-package: `software.amazon.awssdk.bridge.smithyjava.retry`
    - Create `protocol` sub-package: `software.amazon.awssdk.bridge.smithyjava.protocol`
    - Verify existing `interceptors` and `transport` packages are present
    - _Requirements: 1.1, 3.1, 7.2, 8.7_

- [ ] 2. Implement V2CredentialsBridge (Identity Resolution)
  - [ ] 2.1 Create `V2CredentialsBridge` class in `auth` package
    - Implement `AwsCredentialsResolver` interface
    - Implement `identityType()` returning `AwsCredentialsIdentity.class`
    - Implement `resolveIdentity(Context)` that calls `v2Provider.resolveCredentials()`
    - Map `AwsCredentials` to `AwsCredentialsIdentity` (basic credentials)
    - Map `AwsSessionCredentials` to `AwsSessionCredentialsIdentity` (with session token)
    - Wrap provider exceptions in `SdkClientException` with cause preserved
    - Throw `SdkClientException` if provider is null at construction time
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.2 Write property test for credential round-trip preservation
    - **Property 1: Credential Round-Trip Preservation**
    - Generate random (accessKeyId, secretAccessKey, sessionToken?) tuples
    - Verify `V2CredentialsBridge.resolveIdentity()` preserves all values exactly
    - **Validates: Requirements 1.2, 1.3**

  - [x] 2.3 Write property test for credential resolution error wrapping
    - **Property 2: Credential Resolution Error Wrapping**
    - Generate arbitrary exceptions from mock `AwsCredentialsProvider`
    - Verify `SdkClientException` is thrown with the original as cause and non-empty message
    - **Validates: Requirements 1.4**

  - [x] 2.4 Write unit tests for V2CredentialsBridge
    - Test basic credential mapping (accessKeyId + secretAccessKey)
    - Test session credential mapping (includes sessionToken)
    - Test null provider throws SdkClientException at construction
    - Test provider exception wrapping
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 3. Implement ProtocolResolver (Multi-Protocol Support)
  - [ ] 3.1 Create `ProtocolResolver` class in `protocol` package
    - Define `ShapeId` constants for all six protocol traits (awsJson1_0, awsJson1_1, restJson1, restXml, awsQuery, rpcV2Cbor)
    - Implement `resolve(ApiOperation<?, ?>)` that examines the service schema traits
    - Return `AwsJson1Protocol` for `aws.protocols#awsJson1_0`
    - Return `AwsJson1_1Protocol` for `aws.protocols#awsJson1_1`
    - Return `RestJson1Protocol` for `aws.protocols#restJson1`
    - Return `RestXmlProtocol` for `aws.protocols#restXml`
    - Return `AwsQueryProtocol` for `aws.protocols#awsQuery`
    - Return `RpcV2CborProtocol` for `smithy.protocols#rpcv2Cbor`
    - Cache protocol instances per service shape ID for reuse
    - Throw `SdkClientException` if no recognized protocol trait is found
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_

  - [x] 3.2 Write property test for protocol trait resolution
    - **Property 10: Protocol Trait Resolution**
    - Generate service schemas with each of the six protocol traits
    - Verify the correct `ClientProtocol` implementation is returned
    - Verify same instance returned for same service (caching)
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.9**

  - [x] 3.3 Write unit tests for ProtocolResolver
    - Test each protocol trait individually
    - Test unsupported protocol throws exception
    - Test caching returns same instance for same service
    - _Requirements: 8.1–8.9_

- [ ] 4. Implement AuthSchemeResolver
  - [ ] 4.1 Create `AuthSchemeResolver` class in `auth` package
    - Implement `resolve(ApiOperation<?, ?>, Map<ShapeId, SignerConfig>)` static method
    - Iterate `operation.effectiveAuthSchemes()` in order
    - Select the first scheme with a registered signer
    - Return an `AuthSchemeConfig` record containing the scheme ID and signer config
    - Throw `SdkClientException` if no supported scheme is found
    - Handle `smithy.api#noAuth` (anonymous) as a special case skipping signing
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 4.2 Write property test for auth scheme selection order
    - **Property 4: Auth Scheme Selection Order**
    - Generate random ordered lists of ShapeIds with random signer availability
    - Verify the first scheme with a registered signer is always selected
    - **Validates: Requirements 3.4**

  - [x] 4.3 Write unit tests for AuthSchemeResolver
    - Test single sigv4 scheme selection
    - Test noAuth scheme skips signing
    - Test multiple schemes, first-match wins
    - Test no supported scheme throws exception
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 5. Implement V2RetryBridge
  - [ ] 5.1 Create `V2RetryBridge` class in `retry` package
    - Implement `fromV2Config(SdkClientConfiguration)` static factory method
    - Bridge v2 `RetryStrategy` to smithy-java retry: map max attempts, backoff, predicates
    - Bridge v2 `RetryPolicy` (legacy) to smithy-java retry as fallback
    - Default to standard retry (3 max attempts) when no explicit config present
    - Map throttling classification (HTTP 429, isThrottlingException) to smithy-java
    - Map transient server error classification (HTTP 500, 502, 503, 504) to smithy-java
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_

  - [x] 5.2 Write property test for retry configuration bridge preservation
    - **Property 9: Retry Configuration Bridge Preservation**
    - Generate v2 RetryStrategy instances with random maxAttempts values
    - Verify smithy-java retry config has same maximum attempt count
    - **Validates: Requirements 7.2**

  - [x] 5.3 Write unit tests for V2RetryBridge
    - Test default strategy (3 max attempts) when no config
    - Test bridging explicit v2 RetryStrategy with custom max attempts
    - Test bridging legacy RetryPolicy
    - Test throttling error classification
    - Test transient server error classification
    - _Requirements: 7.1, 7.2, 7.3, 7.6, 7.7_

- [x] 6. Checkpoint - Ensure all tests pass for independent components
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement FullV2InterceptorBridge
  - [x] 7.1 Create `FullV2InterceptorBridge` class in `interceptors` package
    - Implement `ClientInterceptor` interface with all 17 hooks
    - Maintain a shared `ExecutionAttributes` instance across all hooks in one execution
    - Pre-populate `ExecutionAttributes` with operation name and service name
    - Implement context adaptation: convert smithy-java hook contexts (InputHook, RequestHook, ResponseHook, OutputHook) to v2 `Context.*` objects
    - _Requirements: 4.1, 5.1, 5.14_

  - [x] 7.2 Implement forward hook mappings in FullV2InterceptorBridge
    - Map `readBeforeExecution` → v2 `beforeExecution` (Context.BeforeExecution)
    - Map `modifyBeforeSerialization` → v2 `modifyRequest` (Context.ModifyRequest), return modified SdkRequest
    - Map `readBeforeSerialization` → v2 `beforeMarshalling` (Context.BeforeMarshalling)
    - Map `readAfterSerialization` → v2 `afterMarshalling` (Context.AfterMarshalling)
    - Map `modifyBeforeSigning` → v2 `modifyHttpRequest` (Context.ModifyHttpRequest), return modified SdkHttpRequest
    - Map `readBeforeTransmit` → v2 `beforeTransmission` (Context.BeforeTransmission)
    - Map `readAfterTransmit` → v2 `afterTransmission` (Context.AfterTransmission)
    - _Requirements: 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_

  - [x] 7.3 Implement completion hook mappings in FullV2InterceptorBridge
    - Map `modifyBeforeDeserialization` → v2 `modifyHttpResponse` (Context.ModifyHttpResponse), return modified SdkHttpResponse
    - Map `readBeforeDeserialization` → v2 `beforeUnmarshalling` (Context.BeforeUnmarshalling)
    - Map `readAfterDeserialization` → v2 `afterUnmarshalling` (Context.AfterUnmarshalling)
    - Map `modifyBeforeCompletion` → v2 `modifyResponse` (Context.ModifyResponse), return modified SdkResponse
    - Map `readAfterExecution` → v2 `afterExecution` (success) or `onExecutionFailure` (error)
    - Map `modifyBeforeAttemptCompletion` → v2 `modifyException` (Context.FailedExecution), return modified Throwable
    - Propagate v2 interceptor exceptions without additional wrapping
    - _Requirements: 5.9, 5.10, 5.11, 5.12, 5.13, 5.15, 5.16_

  - [x] 7.4 Write property test for interceptor hook dispatch fidelity
    - **Property 5: V2 Interceptor Hook Dispatch Fidelity**
    - Generate mock v2 ExecutionInterceptor instances
    - Invoke each smithy-java hook and verify the corresponding v2 hook is called with correct context
    - Verify modify hook mutations flow through to subsequent stages
    - **Validates: Requirements 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 5.11, 5.12, 5.13, 5.15**

  - [x] 7.5 Write property test for shared ExecutionAttributes
    - **Property 6: ExecutionAttributes Shared Across Hooks**
    - Set attributes in early hooks, verify visible in all subsequent hooks
    - **Validates: Requirements 5.14**

  - [x] 7.6 Write property test for interceptor exception transparency
    - **Property 7: V2 Interceptor Exception Transparency**
    - Generate exceptions from v2 interceptor hooks
    - Verify exact exception propagates without wrapping
    - **Validates: Requirements 5.16**

  - [x] 7.7 Write unit tests for FullV2InterceptorBridge
    - Test each of the 13 hook mappings individually
    - Test shared ExecutionAttributes across hooks
    - Test exception propagation without wrapping
    - Test modify hooks returning mutated objects
    - _Requirements: 5.1–5.16_

- [x] 8. Checkpoint - Ensure all tests pass for interceptor bridge
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Expand SmithyJavaPipeline to delegate to smithy-java Client
  - [x] 9.1 Rewrite `SmithyJavaPipeline` constructor to build a smithy-java `Client`
    - Extract v2 config: SYNC_HTTP_CLIENT, CREDENTIALS_PROVIDER, EXECUTION_INTERCEPTORS, RETRY_STRATEGY/RETRY_POLICY, CLIENT_ENDPOINT_PROVIDER, AWS_REGION, SERVICE_SIGNING_NAME
    - Construct `V2TransportBridge` from v2 `SdkHttpClient`
    - Construct `V2CredentialsBridge` from v2 `AwsCredentialsProvider`
    - Construct `FullV2InterceptorBridge` for each v2 `ExecutionInterceptor` (preserving order)
    - Construct retry config via `V2RetryBridge.fromV2Config()`
    - Configure auth scheme (SigV4 with credentials resolver + region + signing name)
    - Configure endpoint resolver (static override or rules engine)
    - Build smithy-java `Client` instance with all bridged components
    - _Requirements: 1.1, 2.1, 2.2, 3.2, 4.1, 5.1, 6.1, 6.2, 7.2, 7.3, 10.1, 11.1_

  - [x] 9.2 Rewrite `SmithyJavaPipeline.execute()` to delegate to `Client.call()`
    - Resolve `ApiOperation` (keep existing `resolveOperation` logic)
    - Resolve protocol via `ProtocolResolver.resolve(operation)`
    - Delegate to `smithyClient.call(operation, input)` — smithy-java handles the full lifecycle
    - Map result back to `OutputT`
    - _Requirements: 8.7, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [x] 9.3 Implement exception mapping in `SmithyJavaPipeline.execute()`
    - Catch `CallException`: if cause is `SdkServiceException` or subtype, throw cause directly
    - Catch `CallException`: if cause is other, create `AwsServiceException` with message, status code, request ID, cause
    - Catch `TransportException`/`IOException`: wrap in `SdkClientException`
    - Catch unexpected `RuntimeException`: wrap in `SdkClientException` with "unexpected error" message
    - Extract `x-amzn-RequestId` and `x-amz-id-2` headers, set on thrown exception
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [x] 9.4 Implement endpoint resolution logic
    - If `CLIENT_ENDPOINT_PROVIDER` is set, use static endpoint (bypass rules engine)
    - Otherwise, configure smithy-java `EndpointResolver` from the service's endpoint ruleset
    - Pass region, service name, and operation-specific parameters to the rules engine
    - Apply endpoint-resolved auth schemes and headers before signing
    - Throw exception on endpoint resolution failure
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 9.5 Ensure `supportsOperation()` returns false for unsupported operations
    - Keep check: input must implement `SerializableStruct`
    - Keep check: ApiOperation class must be loadable
    - Add check: streaming operations return false
    - _Requirements: 10.3_

  - [ ] 9.6 Write property test for CallException unwrapping
    - **Property 11: CallException Unwrapping**
    - Generate `CallException` instances with `SdkServiceException` causes
    - Verify the cause is thrown directly without wrapping
    - **Validates: Requirements 9.1, 9.2**

  - [ ] 9.7 Write property test for non-SDK CallException mapping
    - **Property 12: Non-SDK CallException Mapping**
    - Generate `CallException` instances with non-SdkServiceException causes
    - Verify `AwsServiceException` is thrown with correct message, status, and cause
    - **Validates: Requirements 9.3**

  - [ ] 9.8 Write property test for transport exception wrapping
    - **Property 13: Transport Exception Wrapping**
    - Generate `TransportException`/`IOException` instances
    - Verify `SdkClientException` is thrown with original as cause and message preserved
    - **Validates: Requirements 9.4**

  - [ ] 9.9 Write property test for request ID extraction
    - **Property 14: Request ID Extraction**
    - Generate responses with random `x-amzn-RequestId` and `x-amz-id-2` header values
    - Verify these appear on the thrown `AwsServiceException`
    - **Validates: Requirements 9.5, 9.6**

  - [ ] 9.10 Write property test for unsupported operation detection
    - **Property 15: Unsupported Operation Detection**
    - Generate SdkRequest inputs that do NOT implement SerializableStruct
    - Verify `supportsOperation()` returns false
    - **Validates: Requirements 10.3**

  - [ ] 9.11 Write property test for endpoint override bypass
    - **Property 8: Endpoint Override Bypass**
    - Configure static endpoint override URIs
    - Verify the rules engine is bypassed and the override URI is used
    - **Validates: Requirements 6.2**

  - [ ] 9.12 Write property test for signing parameter resolution
    - **Property 3: Signing Parameter Resolution (Most-Specific Wins)**
    - Configure client-level and endpoint-override signing params
    - Verify endpoint overrides take precedence when present
    - **Validates: Requirements 2.2, 2.3**

- [ ] 10. Checkpoint - Ensure all tests pass for expanded pipeline
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Verify SPI backward compatibility
  - [ ] 11.1 Verify `SmithyJavaPipelineProvider` registration and priority
    - Ensure `META-INF/services` file is correct for ServiceLoader discovery
    - Ensure priority value is lower than `Integer.MAX_VALUE` (default v2 pipeline)
    - Verify `createPipeline()` failure results in graceful fallback (logging + fall through)
    - _Requirements: 10.1, 10.2, 10.4, 10.5, 10.6_

  - [ ] 11.2 Write property test for interceptor registration order preservation
    - **Property 16: Interceptor Registration Order Preservation**
    - Generate random-length interceptor lists
    - Verify `FullV2InterceptorBridge` instances are registered in same order
    - **Validates: Requirements 5.1**

  - [ ] 11.3 Write unit tests for SPI compatibility
    - Test ServiceLoader discovers SmithyJavaPipelineProvider
    - Test priority is lower than default pipeline
    - Test createPipeline failure does not throw (graceful fallback)
    - Test absent bridge jar results in default pipeline usage
    - _Requirements: 10.1, 10.2, 10.4, 10.5, 10.6_

- [ ] 12. Integration tests
  - [ ] 12.1 Write integration test for full pipeline execution with DynamoDB Local
    - Configure v2 DynamoDB client with smithy-java bridge on classpath
    - Execute a PutItem + GetItem round-trip through the full pipeline
    - Verify credentials → signing → serialize → transport → deserialize → response works end-to-end
    - _Requirements: 1.1, 2.1, 8.1, 9.1, 11.1_

  - [ ] 12.2 Write integration test verifying full interceptor hook sequence
    - Register a recording v2 `ExecutionInterceptor` that logs all hook invocations
    - Execute a request and verify all 13 hooks fire in the correct order
    - Verify modify hooks can mutate request/response and mutations are visible downstream
    - _Requirements: 4.1, 5.1, 5.2–5.16_

  - [ ] 12.3 Write integration test for retry behavior
    - Configure a mock server returning transient errors (HTTP 500) then success
    - Verify the request is retried the correct number of times
    - Verify backoff delays are applied between attempts
    - _Requirements: 7.1, 7.4, 7.7, 7.8_

- [ ] 13. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using jqwik (minimum 100 iterations)
- Unit tests validate specific examples and edge cases
- The existing `V2InterceptorBridge` (4-hook POC) is superseded by `FullV2InterceptorBridge` — the old file can be removed or deprecated once the new bridge is validated
- Build command: `mvn clean install -pl :smithy-java-bridge -P quick --am`
- The design delegates to smithy-java's `Client.call()` — most pipeline ordering/interceptor/retry behavior is handled by smithy-java itself, reducing the bridge's responsibilities to config wiring and exception mapping

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1", "5.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "3.2", "3.3", "4.2", "4.3", "5.2", "5.3"] },
    { "id": 3, "tasks": ["7.1"] },
    { "id": 4, "tasks": ["7.2", "7.3"] },
    { "id": 5, "tasks": ["7.4", "7.5", "7.6", "7.7"] },
    { "id": 6, "tasks": ["9.1"] },
    { "id": 7, "tasks": ["9.2", "9.3", "9.4", "9.5"] },
    { "id": 8, "tasks": ["9.6", "9.7", "9.8", "9.9", "9.10", "9.11", "9.12"] },
    { "id": 9, "tasks": ["11.1"] },
    { "id": 10, "tasks": ["11.2", "11.3"] },
    { "id": 11, "tasks": ["12.1"] },
    { "id": 12, "tasks": ["12.2", "12.3"] }
  ]
}
```
