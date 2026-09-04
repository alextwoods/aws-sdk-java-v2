# Requirements Document

## Introduction

Expand the smithy-java SPI pipeline proof-of-concept into a full execution pipeline that matches the smithy-java `Client.call` method's lifecycle. The current POC only handles serialization, transport, and deserialization for `awsJson1_0`. This feature adds identity resolution (credentials), SigV4 request signing, auth scheme resolution, the full interceptor pipeline, endpoint resolution, retry, multi-protocol support, and proper error mapping — making the smithy-java bridge a production-ready drop-in replacement for the v2 internal pipeline.

## Glossary

- **SmithyJavaPipeline**: The `SdkPipeline` implementation in `core/smithy-java-bridge` that delegates execution to the smithy-java runtime.
- **SdkPipeline**: The SPI interface in `sdk-core` (`software.amazon.awssdk.core.client.handler.SdkPipeline`) that abstracts the execution pipeline. Discovered via `SdkPipelineProvider` and `ServiceLoader`.
- **IdentityResolver**: smithy-java interface (`software.amazon.smithy.java.auth.api.identity.IdentityResolver`) that resolves caller identity (credentials) for a request.
- **AwsCredentialsResolver**: smithy-java specialization of `IdentityResolver` for AWS credentials (`software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver`).
- **AwsCredentialsProvider**: v2 SDK interface (`software.amazon.awssdk.auth.credentials.AwsCredentialsProvider`) that supplies AWS credentials.
- **V2CredentialsBridge**: Adapter that wraps a v2 `AwsCredentialsProvider` as a smithy-java `AwsCredentialsResolver` for per-call credential resolution.
- **AuthScheme**: A combination of an identity type, a signer, and configuration that defines how a request is authenticated.
- **Signer**: smithy-java component that signs an HTTP request given an identity (e.g., SigV4 signing with AWS credentials).
- **ClientInterceptor**: smithy-java interceptor interface (`software.amazon.smithy.java.client.core.interceptors.ClientInterceptor`) with lifecycle hooks spanning the full call.
- **ExecutionInterceptor**: v2 SDK interceptor interface (`software.amazon.awssdk.core.interceptor.ExecutionInterceptor`) with 13 hooks.
- **FullV2InterceptorBridge**: Adapter that wraps a v2 `ExecutionInterceptor` as a smithy-java `ClientInterceptor`, mapping all 13 v2 hooks to the corresponding smithy-java hooks.
- **EndpointResolver**: smithy-java interface that resolves the endpoint URL for a request based on endpoint rules.
- **ClientProtocol**: smithy-java interface that handles request serialization and response deserialization for a specific wire protocol.
- **RetryStrategy**: smithy-java retry mechanism that wraps attempt execution with configurable retry logic.
- **V2TransportBridge**: Existing adapter that wraps a v2 `SdkHttpClient` as a smithy-java `ClientTransport`.
- **ApiOperation**: smithy-java schema object representing a service operation's input, output, and error types.
- **CallException**: smithy-java exception wrapping errors from a service call.

## Requirements

### Requirement 1: Credential Bridge (Identity Resolution)

**User Story:** As a developer using the v2 SDK with the smithy-java bridge on the classpath, I want my existing v2 `AwsCredentialsProvider` to be used for signing requests, so that I do not need to change my credential configuration.

#### Acceptance Criteria

1. WHEN the SmithyJavaPipeline is constructed with a `SdkClientConfiguration` containing an `AwsCredentialsProvider`, THE V2CredentialsBridge SHALL wrap the v2 provider as a smithy-java `AwsCredentialsResolver` whose `identityType()` method returns the `AwsCredentialsIdentity` interface class.
2. WHEN a request is executed, THE V2CredentialsBridge SHALL invoke the v2 `AwsCredentialsProvider.resolveCredentials()` and return the result as an `AwsCredentialsIdentity` with the same `accessKeyId` and `secretAccessKey` values.
3. WHEN the v2 `AwsCredentialsProvider` returns `AwsSessionCredentials` (with a session token), THE V2CredentialsBridge SHALL produce an `AwsSessionCredentialsIdentity` that includes the `accessKeyId`, `secretAccessKey`, and `sessionToken` from the v2 credentials.
4. IF the v2 `AwsCredentialsProvider` throws an exception during credential resolution, THEN THE V2CredentialsBridge SHALL propagate the exception wrapped as an `SdkClientException` with the original exception preserved as the cause.
5. IF the `SdkClientConfiguration` does not contain an `AwsCredentialsProvider` (null), THEN THE SmithyJavaPipeline SHALL throw an `SdkClientException` with a message indicating that no credential provider was configured.

### Requirement 2: Request Signing (SigV4)

**User Story:** As a developer, I want my requests to be signed with SigV4 using the resolved credentials, so that they are authenticated by AWS services.

#### Acceptance Criteria

1. WHEN the SmithyJavaPipeline executes a request, THE SmithyJavaPipeline SHALL resolve credentials via the `AwsCredentialsResolver` and sign the HTTP request using the smithy-java `aws-sigv4` signer before transmitting to the transport.
2. WHEN signing a request, THE SmithyJavaPipeline SHALL use the signing service name (from `SdkClientOption.SERVICE_SIGNING_NAME`) and the signing region (from the v2 client configuration `Region`) to compute the SigV4 signature.
3. WHEN the resolved endpoint's auth scheme metadata specifies a different signing name or signing region override, THE SmithyJavaPipeline SHALL use the override values instead of the client-level defaults for signing.
4. WHEN the resolved `AwsCredentialsIdentity` includes a session token, THE SmithyJavaPipeline SHALL include the session token in the SigV4-signed request.
5. IF signing fails due to an invalid credential state or signer error, THEN THE SmithyJavaPipeline SHALL throw an `SdkClientException` with a message indicating the signing failure cause.

### Requirement 3: Auth Scheme Resolution

**User Story:** As a developer, I want the pipeline to resolve the correct authentication scheme per-operation, so that operations with different auth requirements are handled correctly.

#### Acceptance Criteria

1. WHEN an operation is executed, THE SmithyJavaPipeline SHALL resolve the effective auth scheme list by calling `effectiveAuthSchemes()` on the operation's `ApiOperation` instance, which returns a `List<ShapeId>` of scheme identifiers (e.g., `aws.auth#sigv4`, `smithy.api#noAuth`).
2. WHEN the resolved auth scheme list contains `aws.auth#sigv4` as the selected scheme, THE SmithyJavaPipeline SHALL select the SigV4 signer and the `AwsCredentialsResolver` to sign the HTTP request before transmitting it via the transport.
3. IF the resolved auth scheme list contains only `smithy.api#noAuth` (anonymous auth), THEN THE SmithyJavaPipeline SHALL skip signing and transmit the HTTP request unsigned without attaching credentials.
4. WHEN multiple auth schemes are present in the list returned by `effectiveAuthSchemes()`, THE SmithyJavaPipeline SHALL iterate the list in order and select the first scheme that has a registered signer implementation available in the pipeline's configuration.
5. IF no scheme in the `effectiveAuthSchemes()` list has a registered signer available, THEN THE SmithyJavaPipeline SHALL throw an exception indicating that no supported auth scheme could be resolved for the operation.

### Requirement 4: Full Interceptor Pipeline

**User Story:** As a developer, I want the smithy-java pipeline to execute the full smithy-java interceptor lifecycle, so that registered interceptors observe and modify the request/response at every stage.

#### Acceptance Criteria

1. THE SmithyJavaPipeline SHALL execute smithy-java `ClientInterceptor` hooks in the following order: readBeforeExecution, modifyBeforeSerialization, readBeforeSerialization, readAfterSerialization, modifyBeforeSigning, readBeforeSigning, readAfterSigning, modifyBeforeTransmit, readBeforeTransmit, readAfterTransmit, modifyBeforeDeserialization, readBeforeDeserialization, readAfterDeserialization, modifyBeforeAttemptCompletion, readAfterAttemptCompletion, modifyBeforeCompletion, readAfterExecution.
2. WHEN multiple `ClientInterceptor` instances are registered, THE SmithyJavaPipeline SHALL execute each hook in interceptor-registration order for forward hooks (readBeforeExecution through readAfterTransmit) and in reverse-registration order for completion hooks (modifyBeforeDeserialization through readAfterExecution).
3. WHEN a `ClientInterceptor` modifies a request in a `modify*` hook, THE SmithyJavaPipeline SHALL pass the modified request object to all subsequently invoked hooks and pipeline stages, replacing the prior request value.
4. IF a `ClientInterceptor` throws an exception in any hook prior to modifyBeforeAttemptCompletion, THEN THE SmithyJavaPipeline SHALL skip remaining forward hooks, record the exception as the call failure, and execute the completion hooks (modifyBeforeAttemptCompletion, readAfterAttemptCompletion, modifyBeforeCompletion, readAfterExecution) with the failure context.
5. IF a `ClientInterceptor` throws an exception in a completion hook during error propagation, THEN THE SmithyJavaPipeline SHALL suppress the completion-hook exception, attach it to the original failure as a suppressed exception, and continue executing subsequent completion hooks.

### Requirement 5: V2 ExecutionInterceptor Bridge

**User Story:** As a developer with existing v2 `ExecutionInterceptor` implementations, I want them to run in the smithy-java pipeline, so that my observability and request-modification logic continues to work without changes.

#### Acceptance Criteria

1. WHEN the SmithyJavaPipeline is constructed with a `SdkClientConfiguration` containing `ExecutionInterceptor` instances (retrieved via `SdkClientOption.EXECUTION_INTERCEPTORS`), THE SmithyJavaPipeline SHALL wrap each v2 interceptor as a smithy-java `ClientInterceptor` using the FullV2InterceptorBridge, preserving the registration order of the interceptors.
2. THE FullV2InterceptorBridge SHALL map v2 `beforeExecution` to smithy-java `readBeforeExecution`, providing a `Context.BeforeExecution` containing the `SdkRequest` and populating `ExecutionAttributes` with at minimum the operation name and service name.
3. THE FullV2InterceptorBridge SHALL map v2 `modifyRequest` to smithy-java `modifyBeforeSerialization`, passing a `Context.ModifyRequest` containing the current `SdkRequest` and returning the (potentially modified) `SdkRequest` to the pipeline.
4. THE FullV2InterceptorBridge SHALL map v2 `beforeMarshalling` to smithy-java `readBeforeSerialization`, passing a `Context.BeforeMarshalling` containing the current (possibly modified) `SdkRequest`.
5. THE FullV2InterceptorBridge SHALL map v2 `afterMarshalling` to smithy-java `readAfterSerialization`, passing a `Context.AfterMarshalling` containing both the `SdkRequest` and the serialized `SdkHttpRequest`.
6. THE FullV2InterceptorBridge SHALL map v2 `modifyHttpRequest` to smithy-java `modifyBeforeSigning`, passing a `Context.ModifyHttpRequest` and returning the (potentially modified) `SdkHttpRequest` so that mutations are included in the signed request.
7. THE FullV2InterceptorBridge SHALL map v2 `beforeTransmission` to smithy-java `readBeforeTransmit`, passing a `Context.BeforeTransmission` containing the final signed `SdkHttpRequest`.
8. THE FullV2InterceptorBridge SHALL map v2 `afterTransmission` to smithy-java `readAfterTransmit`, passing a `Context.AfterTransmission` containing the `SdkHttpResponse` received from the service.
9. THE FullV2InterceptorBridge SHALL map v2 `modifyHttpResponse` to smithy-java `modifyBeforeDeserialization`, passing a `Context.ModifyHttpResponse` and returning the (potentially modified) `SdkHttpResponse`.
10. THE FullV2InterceptorBridge SHALL map v2 `beforeUnmarshalling` to smithy-java `readBeforeDeserialization`, passing a `Context.BeforeUnmarshalling` containing the current (possibly modified) `SdkHttpResponse`.
11. THE FullV2InterceptorBridge SHALL map v2 `afterUnmarshalling` to smithy-java `readAfterDeserialization`, passing a `Context.AfterUnmarshalling` containing the deserialized `SdkResponse`.
12. THE FullV2InterceptorBridge SHALL map v2 `afterExecution` to smithy-java `readAfterExecution` on success (passing a `Context.AfterExecution` with the final `SdkResponse`), and map v2 `onExecutionFailure` to smithy-java `readAfterExecution` with error (passing a `Context.FailedExecution` containing the exception and whatever request/response state was available at the point of failure).
13. THE FullV2InterceptorBridge SHALL map v2 `modifyResponse` to smithy-java `modifyBeforeCompletion`, passing a `Context.ModifyResponse` and returning the (potentially modified) `SdkResponse`.
14. WHEN bridging v2 interceptor hooks, THE FullV2InterceptorBridge SHALL adapt the smithy-java hook context (InputHook, RequestHook, ResponseHook, OutputHook) into the corresponding v2 `Context.*` objects and provide an `ExecutionAttributes` instance that is shared across all hooks within a single execution, preserving attributes set by earlier hooks for use by later hooks.
15. THE FullV2InterceptorBridge SHALL map v2 `modifyException` to smithy-java `modifyBeforeAttemptCompletion` (or the equivalent error-path modify hook), passing a `Context.FailedExecution` and returning the (potentially modified) `Throwable` to the pipeline.
16. IF a v2 `ExecutionInterceptor` hook throws an exception, THEN THE FullV2InterceptorBridge SHALL propagate that exception to the smithy-java pipeline's error handling without wrapping it in an additional exception type.

### Requirement 6: Endpoint Resolution

**User Story:** As a developer, I want endpoints to be resolved dynamically using smithy-java's endpoint rules engine, so that requests are sent to the correct service endpoint based on region and other parameters.

#### Acceptance Criteria

1. WHEN a request is executed, THE SmithyJavaPipeline SHALL resolve the endpoint by invoking the smithy-java `EndpointResolver` configured from the service's endpoint ruleset, supplying the resolution result (URI, headers, and auth scheme properties) to the protocol layer before HTTP request construction.
2. WHEN the v2 client configuration specifies an explicit endpoint override via `SdkClientOption.CLIENT_ENDPOINT_PROVIDER`, THE SmithyJavaPipeline SHALL bypass the endpoint rules engine and use the override URI as the static endpoint for all requests made through that client instance.
3. WHEN the endpoint resolution produces endpoint-specific auth schemes or headers, THE SmithyJavaPipeline SHALL apply those auth schemes and headers to the HTTP request before signing, such that the signing step uses the endpoint-resolved signing name and signing region rather than the client-level defaults.
4. THE SmithyJavaPipeline SHALL pass the following parameters to the endpoint rules engine for every request: the configured AWS region, the service name (derived from the service's Smithy shape ID), and any operation-specific endpoint parameters defined in the service's endpoint ruleset (e.g., bucket name for S3, account ID for account-based routing).
5. IF the endpoint rules engine fails to resolve an endpoint (e.g., due to missing required parameters or an unsupported region), THEN THE SmithyJavaPipeline SHALL throw an exception indicating the resolution failure reason without sending the request over the network.

### Requirement 7: Retry

**User Story:** As a developer, I want failed requests to be retried according to my configured retry policy, so that transient errors are handled transparently.

#### Acceptance Criteria

1. WHEN a request fails with an exception that the configured retry strategy classifies as retryable, THE SmithyJavaPipeline SHALL retry the request up to the maximum number of attempts defined by the retry strategy (default: 3 for standard mode, 4 for legacy mode).
2. WHEN the v2 client configuration contains a `RetryPolicy` or `RetryStrategy`, THE SmithyJavaPipeline SHALL bridge the v2 retry configuration (including max attempts, backoff strategy, throttling backoff strategy, and retry predicates) to smithy-java's retry mechanism.
3. IF the v2 client configuration contains neither a `RetryPolicy` nor a `RetryStrategy`, THEN THE SmithyJavaPipeline SHALL apply the default `StandardRetryStrategy` with 3 maximum attempts.
4. WHILE retrying a request, THE SmithyJavaPipeline SHALL re-execute the attempt-level pipeline stages (serialization, signing, transmission, and deserialization) and invoke the per-attempt interceptor hooks (`beforeMarshalling` through `afterUnmarshalling`) for each retry attempt.
5. WHEN the retry strategy's `refreshRetryToken` throws `TokenAcquisitionFailedException` (indicating maximum attempts reached or circuit breaker open), THE SmithyJavaPipeline SHALL throw the last encountered exception to the caller without further retry attempts.
6. WHEN a response indicates a throttling error (HTTP 429 or an exception where `isThrottlingException()` returns true), THE SmithyJavaPipeline SHALL classify the error as retryable and apply the throttling backoff strategy for delay computation.
7. WHEN a response indicates a transient server error (HTTP 500, 502, 503, or 504), THE SmithyJavaPipeline SHALL classify the error as retryable and apply the standard backoff strategy for delay computation.
8. WHILE retrying a request, THE SmithyJavaPipeline SHALL wait for the backoff delay duration returned by the retry strategy before making each subsequent attempt.

### Requirement 8: Multi-Protocol Support

**User Story:** As a developer, I want the smithy-java pipeline to support all AWS protocols, so that any AWS service can use the smithy-java execution path.

#### Acceptance Criteria

1. WHEN the `ApiOperation`'s service schema carries the `awsJson1_0` protocol trait, THE SmithyJavaPipeline SHALL select the `AwsJson1Protocol` for serialization and deserialization.
2. WHEN the `ApiOperation`'s service schema carries the `awsJson1_1` protocol trait, THE SmithyJavaPipeline SHALL select the `AwsJson1_1Protocol` for serialization and deserialization.
3. WHEN the `ApiOperation`'s service schema carries the `restJson1` protocol trait, THE SmithyJavaPipeline SHALL select the `RestJson1Protocol` for serialization and deserialization.
4. WHEN the `ApiOperation`'s service schema carries the `restXml` protocol trait, THE SmithyJavaPipeline SHALL select the `RestXmlProtocol` for serialization and deserialization.
5. WHEN the `ApiOperation`'s service schema carries the `awsQuery` protocol trait, THE SmithyJavaPipeline SHALL select the `AwsQueryProtocol` for serialization and deserialization.
6. WHEN the `ApiOperation`'s service schema carries the `rpcV2Cbor` protocol trait, THE SmithyJavaPipeline SHALL select the `RpcV2CborProtocol` for serialization and deserialization.
7. THE SmithyJavaPipeline SHALL resolve the protocol by reading the protocol trait from the `ApiOperation`'s service schema and selecting the corresponding `ClientProtocol` implementation.
8. IF the `ApiOperation`'s service schema does not carry any recognized protocol trait, THEN THE SmithyJavaPipeline SHALL throw an exception indicating the unsupported or missing protocol.
9. THE SmithyJavaPipeline SHALL resolve the protocol once per service and reuse the same `ClientProtocol` instance for all subsequent operations on that service.

### Requirement 9: Error Handling and Exception Mapping

**User Story:** As a developer, I want service errors from the smithy-java pipeline to surface as v2 SDK exceptions, so that my existing error-handling code continues to work.

#### Acceptance Criteria

1. WHEN the smithy-java protocol deserializes a service error response into a modeled exception type that extends `AwsServiceException`, THE SmithyJavaPipeline SHALL throw that modeled exception directly without wrapping.
2. WHEN the smithy-java runtime throws a `CallException` whose cause is an instance of `SdkServiceException` (or a subtype), THE SmithyJavaPipeline SHALL unwrap and throw the cause exception.
3. WHEN the smithy-java runtime throws a `CallException` whose cause is not an instance of `SdkServiceException`, THE SmithyJavaPipeline SHALL create and throw an `AwsServiceException` populated with: the `CallException` message as the error message, the HTTP status code from the error response, and the request ID extracted from the response headers, with the `CallException` set as the cause.
4. IF a transport-level failure occurs (the smithy-java transport throws a `TransportException` or an `IOException` propagates from the underlying HTTP client), THEN THE SmithyJavaPipeline SHALL throw an `SdkClientException` whose message contains the transport error's message and whose cause is the original transport exception.
5. WHEN an error response contains the `x-amzn-RequestId` header (or the request ID is available from the `CallException` or deserialized error), THE SmithyJavaPipeline SHALL set the `requestId` field on the thrown `AwsServiceException` or `SdkServiceException`.
6. WHEN an error response contains the `x-amz-id-2` header, THE SmithyJavaPipeline SHALL set the `extendedRequestId` field on the thrown `AwsServiceException` or `SdkServiceException`.
7. IF the smithy-java runtime throws an exception that is neither a `CallException` nor a transport-level exception (unexpected runtime error), THEN THE SmithyJavaPipeline SHALL throw an `SdkClientException` whose message indicates an unexpected error during request execution and whose cause is the original exception.

### Requirement 10: SPI Backward Compatibility

**User Story:** As a developer, I want the smithy-java bridge to be an optional runtime dependency with no impact when absent, so that the v2 SDK continues to work without it.

#### Acceptance Criteria

1. THE SmithyJavaPipeline SHALL implement the existing `SdkPipeline` interface and be registered via a `SdkPipelineProvider` declared in `META-INF/services/software.amazon.awssdk.core.client.handler.SdkPipelineProvider`, discovered by `ServiceLoader` through the existing `SdkPipelineLoader` logic without modification to that loader.
2. WHILE the `smithy-java-bridge` jar is absent from the classpath, THE `SdkPipelineLoader` SHALL return `Optional.empty()` and THE generated v2 client SHALL use the default v2 pipeline, producing identical request/response behavior as a build without the bridge module.
3. WHEN the `SmithyJavaPipeline` determines that an operation is unsupported (the input does not implement `SerializableStruct`, the operation is a streaming operation, or the corresponding `ApiOperation` class cannot be loaded), THE SmithyJavaPipeline SHALL return `supportsOperation() == false` so that the generated client executes that specific operation through the default v2 pipeline.
4. THE SmithyJavaPipeline SHALL expose no new public method or class that customers must invoke to activate the bridge — adding the `smithy-java-bridge` jar to the classpath is the sole opt-in signal, and all wiring occurs internally via the `SdkPipelineProvider` SPI.
5. IF the `SmithyJavaPipelineProvider.createPipeline()` call throws an exception, THEN THE `SdkPipelineLoader` SHALL log the failure and fall back to the next available provider or the default v2 pipeline, so that a misconfigured bridge does not prevent the client from operating.
6. THE `SmithyJavaPipelineProvider` SHALL declare a priority value lower than the default v2 pipeline's priority (`Integer.MAX_VALUE`), so that when the bridge jar is present and available it is selected over the default pipeline by the `SdkPipelineLoader` priority-sort logic.

### Requirement 11: Pipeline Execution Order

**User Story:** As a developer, I want the smithy-java bridge pipeline to follow the same execution order as smithy-java's `Client.call` method, so that behavior is consistent with a native smithy-java client.

#### Acceptance Criteria

1. THE SmithyJavaPipeline SHALL execute pipeline stages in the following order: (1) resolve endpoint, (2) run pre-serialization interceptor hooks, (3) serialize request, (4) run post-serialization interceptor hooks, (5) resolve identity, (6) sign request, (7) run post-signing interceptor hooks, (8) transmit via transport, (9) run post-transmit interceptor hooks, (10) deserialize response, (11) run post-deserialization interceptor hooks, (12) run completion hooks.
2. THE SmithyJavaPipeline SHALL wrap stages 3 through 10 in the retry loop, re-executing them for each retry attempt up to the maximum number of attempts specified by the configured retry strategy.
3. WHEN a retry occurs, THE SmithyJavaPipeline SHALL re-resolve identity (credentials may have refreshed) and re-sign the request on each attempt.
4. IF the configured retry strategy determines a response is retriable (based on the exception type or HTTP status code), THEN THE SmithyJavaPipeline SHALL re-execute stages 3 through 10 as a new attempt.
5. IF all retry attempts are exhausted without a successful response, THEN THE SmithyJavaPipeline SHALL propagate the last exception from the final attempt to the caller and invoke the completion hooks (stage 12).
6. IF an exception occurs in a stage outside the retry loop (stages 1, 2, 11, or 12), THEN THE SmithyJavaPipeline SHALL propagate the exception to the caller without retrying.
