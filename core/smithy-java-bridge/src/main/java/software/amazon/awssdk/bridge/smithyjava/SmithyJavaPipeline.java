/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.bridge.smithyjava;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.bridge.smithyjava.auth.V2CredentialsBridge;
import software.amazon.awssdk.bridge.smithyjava.interceptors.FullV2InterceptorBridge;
import software.amazon.awssdk.bridge.smithyjava.protocol.ProtocolResolver;
import software.amazon.awssdk.bridge.smithyjava.retry.V2RetryBridge;
import software.amazon.awssdk.bridge.smithyjava.transport.V2TransportBridge;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkPipeline;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4Settings;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.retries.StandardRetryStrategy;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * smithy-java-backed implementation of {@link SdkPipeline}. Delegates to a smithy-java
 * {@link Client} instance configured with bridged v2 components (transport, credentials,
 * interceptors, retry, auth, endpoint resolution).
 *
 * <p>This pipeline supports operations where:
 * <ul>
 *   <li>The input request implements {@link SerializableStruct} (smithy-java serde generated)</li>
 *   <li>A corresponding {@code <OpName>Operation} class exists in the service's operations package</li>
 *   <li>The operation is non-streaming</li>
 * </ul>
 *
 * <p>Operations that don't meet these criteria will report {@code supportsOperation() == false},
 * causing the generated client to fall back to the default v2 pipeline.
 *
 * <p>The smithy-java Client internally handles:
 * <ul>
 *   <li>17-hook interceptor pipeline with correct ordering</li>
 *   <li>Retry loop wrapping the correct stages</li>
 *   <li>Identity resolution + signing coordination</li>
 *   <li>Auth scheme resolution per-operation</li>
 *   <li>Endpoint resolution</li>
 *   <li>Error propagation with suppressed exceptions in completion hooks</li>
 * </ul>
 */
@SdkInternalApi
final class SmithyJavaPipeline implements SdkPipeline {

    private static final Logger log = Logger.loggerFor(SmithyJavaPipeline.class);

    private final BridgeClient smithyClient;
    private final SdkClientConfiguration clientConfiguration;
    private final ProtocolResolver protocolResolver;
    private final ConcurrentMap<String, ApiOperation<SerializableStruct, SerializableStruct>> operationCache;

    SmithyJavaPipeline(SdkClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        this.protocolResolver = new ProtocolResolver();
        this.operationCache = new ConcurrentHashMap<>();
        this.smithyClient = buildSmithyClient(clientConfiguration);
    }

    /**
     * Builds a smithy-java {@link Client} instance configured with all bridged v2 components.
     *
     * <p>Extracts the following from the v2 {@link SdkClientConfiguration}:
     * <ul>
     *   <li>{@code SYNC_HTTP_CLIENT} → {@link V2TransportBridge}</li>
     *   <li>{@code CREDENTIALS_PROVIDER} → {@link V2CredentialsBridge}</li>
     *   <li>{@code EXECUTION_INTERCEPTORS} → {@link FullV2InterceptorBridge} per interceptor</li>
     *   <li>{@code RETRY_STRATEGY}/{@code RETRY_POLICY} → smithy-java retry config</li>
     *   <li>{@code CLIENT_ENDPOINT_PROVIDER} → static endpoint or endpoint resolver</li>
     *   <li>{@code AWS_REGION} → signing region</li>
     *   <li>{@code SERVICE_SIGNING_NAME} → SigV4 signing scope</li>
     * </ul>
     */
    private BridgeClient buildSmithyClient(SdkClientConfiguration config) {
        // -- Transport --
        SdkHttpClient httpClient = config.option(SdkClientOption.SYNC_HTTP_CLIENT);
        if (httpClient == null) {
            throw SdkClientException.builder()
                .message("No sync HTTP client configured. The smithy-java pipeline requires a "
                         + "SYNC_HTTP_CLIENT in the client configuration.")
                .build();
        }
        ClientTransport<HttpRequest, HttpResponse> transport = new V2TransportBridge(httpClient);

        // -- Credentials --
        AwsCredentialsProvider credentialsProvider = config.option(AwsClientOption.CREDENTIALS_PROVIDER);
        V2CredentialsBridge credentialsBridge = new V2CredentialsBridge(credentialsProvider);

        // -- Region and signing name --
        Region region = config.option(AwsClientOption.AWS_REGION);
        String signingName = config.option(AwsClientOption.SERVICE_SIGNING_NAME);

        // -- Auth scheme: SigV4 with credentials resolver + region + signing name --
        SigV4AuthScheme sigV4AuthScheme = new SigV4AuthScheme(signingName != null ? signingName : "service");

        // -- Endpoint resolution --
        EndpointResolver endpointResolver = resolveEndpoint(config);

        // -- Retry --
        V2RetryBridge retryBridge = V2RetryBridge.fromV2Config(config);
        software.amazon.smithy.java.retries.api.RetryStrategy smithyRetryStrategy =
            StandardRetryStrategy.builder()
                .maxAttempts(retryBridge.maxAttempts())
                .build();

        // -- Interceptors (preserving registration order) --
        List<ExecutionInterceptor> v2Interceptors = config.option(SdkClientOption.EXECUTION_INTERCEPTORS);

        // -- Build BridgeClient --
        BridgeClient.BridgeBuilder builder = BridgeClient.builder();
        builder.transport(transport);
        builder.addIdentityResolver(credentialsBridge);
        builder.putSupportedAuthSchemes(sigV4AuthScheme);
        builder.endpointResolver(endpointResolver);
        builder.retryStrategy(smithyRetryStrategy);

        // Set region in the client config context so SigV4AuthScheme.getSignerProperties() can read it
        if (region != null) {
            builder.putConfig(RegionSetting.REGION, region.id());
            builder.putConfig(SigV4Settings.SIGNING_NAME, signingName != null ? signingName : "service");
        }

        // Register bridged interceptors in order, filtering out internal SDK interceptors.
        // Internal interceptors (from .internal. packages) handle concerns like auth scheme
        // resolution and endpoint setting that smithy-java handles natively. Bridging them
        // would fail because the ExecutionAttributes won't contain the internal attributes
        // they expect (AUTH_SCHEME_RESOLVER, AUTH_SCHEMES, IDENTITY_PROVIDERS, etc.).
        if (v2Interceptors != null) {
            for (ExecutionInterceptor v2Interceptor : v2Interceptors) {
                if (!isInternalInterceptor(v2Interceptor)) {
                    builder.addInterceptor(new FullV2InterceptorBridge(v2Interceptor));
                } else {
                    log.debug(() -> "Skipping internal interceptor: " + v2Interceptor.getClass().getName());
                }
            }
        }

        // Disable auto plugins — we control all configuration explicitly
        builder.disableAutoPlugins();

        return builder.build();
    }

    /**
     * Resolves the endpoint configuration from the v2 client config.
     * If an explicit endpoint override is set, use a static endpoint resolver.
     * Otherwise, use a static endpoint from the client endpoint provider's URI.
     */
    private EndpointResolver resolveEndpoint(SdkClientConfiguration config) {
        ClientEndpointProvider endpointProvider = config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (endpointProvider != null) {
            URI endpointUri = endpointProvider.clientEndpoint();
            if (endpointUri != null) {
                // Use static endpoint — bypass rules engine
                return EndpointResolver.staticEndpoint(Endpoint.builder().uri(endpointUri).build());
            }
        }
        // Fallback: no endpoint configured (will fail at request time if not resolved)
        throw SdkClientException.builder()
            .message("No endpoint could be resolved. CLIENT_ENDPOINT_PROVIDER is not configured.")
            .build();
    }

    /**
     * Returns true if the interceptor is an internal SDK interceptor that should not be bridged
     * to the smithy-java pipeline. Internal interceptors handle concerns (auth scheme resolution,
     * endpoint setting, request metrics) that smithy-java handles natively.
     *
     * <p>Detected by checking if the interceptor's class is in a package containing ".internal."
     * (e.g., {@code ...auth.scheme.internal.DynamoDbAuthSchemeInterceptor}).
     */
    private static boolean isInternalInterceptor(ExecutionInterceptor interceptor) {
        String className = interceptor.getClass().getName();
        return className.contains(".internal.");
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> boolean supportsOperation(
        ClientExecutionParams<InputT, OutputT> executionParams) {

        InputT input = executionParams.getInput();
        if (input == null) {
            return false;
        }
        // The operation is only supported if the input POJO has smithy-java serde generated
        // (implements SerializableStruct).
        if (!(input instanceof SerializableStruct)) {
            return false;
        }
        // Verify we can resolve the ApiOperation class for this operation
        return resolveOperation(input, executionParams.getOperationName()) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        SdkClientConfiguration clientConfiguration) {

        InputT input = executionParams.getInput();
        String operationName = executionParams.getOperationName();
        ApiOperation<SerializableStruct, SerializableStruct> operation = resolveOperation(input, operationName);

        if (operation == null) {
            throw new IllegalStateException(
                "Cannot resolve ApiOperation for " + operationName + ". "
                + "supportsOperation() should have returned false.");
        }

        try {
            // Resolve the real protocol from the operation's service schema traits.
            // The BridgeClient is built with a PlaceholderProtocol; we override it per-request
            // so the smithy-java pipeline uses the correct serialization/deserialization logic.
            ClientProtocol<HttpRequest, HttpResponse> resolvedProtocol =
                protocolResolver.resolve(operation, executionParams.getProtocolMetadata());
            RequestOverrideConfig overrideConfig = RequestOverrideConfig.builder()
                .protocol(resolvedProtocol)
                .build();

            // Delegate to smithy-java Client.call() — the runtime handles the full lifecycle:
            // endpoint resolution, interceptors, serialization, signing, transport, deserialization, retry
            SerializableStruct result = smithyClient.callOperation(
                (SerializableStruct) input, operation, overrideConfig);
            return (OutputT) result;
        } catch (CallException e) {
            // smithy-java wraps modeled errors in CallException; unwrap for v2 compatibility
            if (e.getCause() instanceof SdkServiceException) {
                throw (SdkServiceException) e.getCause();
            }
            throw SdkServiceException.builder()
                                     .message(e.getMessage())
                                     .cause(e)
                                     .build();
        }
    }

    @Override
    public void close() {
        smithyClient.close();
    }

    /**
     * Resolve the generated ApiOperation for the given operation name. The convention is:
     * the operation class lives in a sibling {@code .operations} package relative to the
     * input's model package and is named {@code <OperationName>Operation}.
     *
     * <p>For example, if the input is {@code software.amazon.awssdk.services.dynamodb.model.PutItemRequest},
     * the operation class is {@code software.amazon.awssdk.services.dynamodb.operations.PutItemOperation}.
     */
    @SuppressWarnings("unchecked")
    private ApiOperation<SerializableStruct, SerializableStruct> resolveOperation(
        SdkRequest input, String operationName) {

        String cacheKey = input.getClass().getPackageName() + "#" + operationName;
        return operationCache.computeIfAbsent(cacheKey, k -> {
            try {
                // Derive operations package from the input class's package
                // e.g., "software.amazon.awssdk.services.dynamodb.model" -> "...dynamodb.operations"
                String inputPackage = input.getClass().getPackageName();
                String servicePackage = inputPackage.endsWith(".model")
                    ? inputPackage.substring(0, inputPackage.length() - ".model".length())
                    : inputPackage;
                String operationClassName = servicePackage + ".operations." + operationName + "Operation";

                Class<?> opClass = Class.forName(operationClassName, true, input.getClass().getClassLoader());
                Method instanceMethod = opClass.getMethod("instance");
                Object opInstance = instanceMethod.invoke(null);
                return (ApiOperation<SerializableStruct, SerializableStruct>) opInstance;
            } catch (ClassNotFoundException e) {
                // Operation class not generated — this service hasn't opted in
                log.debug(() -> "ApiOperation class not found for " + operationName
                                + " — falling back to v2 pipeline.");
                return null;
            } catch (Exception e) {
                log.warn(() -> "Failed to resolve ApiOperation for " + operationName, e);
                return null;
            }
        });
    }

    /**
     * Concrete subclass of smithy-java {@link Client} used solely to expose the
     * {@code protected call()} method to our pipeline. The {@code Client} class is
     * abstract; this thin shell provides a concrete builder and invokes the parent's
     * lifecycle (interceptor pipeline, retry, auth, transport).
     *
     * <p>The service schema and protocol are resolved lazily per-operation because
     * the pipeline must support multiple services in theory, though in practice a
     * pipeline instance is per-client (one service).
     */
    static final class BridgeClient extends Client {

        private BridgeClient(BridgeBuilder builder) {
            super(builder);
        }

        /**
         * Public entry point for the pipeline to invoke the smithy-java lifecycle.
         * Delegates to the {@code protected} {@link Client#call(SerializableStruct, ApiOperation, RequestOverrideConfig)}.
         */
        <I extends SerializableStruct, O extends SerializableStruct> O callOperation(
                I input,
                ApiOperation<I, O> operation,
                RequestOverrideConfig overrideConfig) {
            return call(input, operation, overrideConfig);
        }

        @Override
        protected TypeRegistry typeRegistry() {
            return TypeRegistry.empty();
        }

        static BridgeBuilder builder() {
            return new BridgeBuilder();
        }

        /**
         * Builder for the BridgeClient. Unlike generated client builders, this does not
         * set a protocol or service upfront — those are configured lazily based on the
         * first operation that passes through.
         */
        static final class BridgeBuilder extends Client.Builder<BridgeClient, BridgeBuilder> {

            private BridgeBuilder() {
            }

            @Override
            public BridgeClient build() {
                // Set a placeholder service and protocol — these are required by ClientConfig
                // but the bridge resolves the real protocol per-operation.
                // The service schema is required; use a minimal placeholder that will be
                // overridden at call time via RequestOverrideConfig if needed.
                configBuilder().service(PlaceholderService.INSTANCE);
                if (configBuilder().protocol() == null) {
                    configBuilder().protocol(PlaceholderProtocol.INSTANCE);
                }
                return new BridgeClient(this);
            }
        }
    }

    /**
     * A minimal placeholder {@link ApiService} to satisfy ClientConfig's non-null requirement.
     * The actual service is resolved from each operation at call time.
     */
    private static final class PlaceholderService implements ApiService {
        static final PlaceholderService INSTANCE = new PlaceholderService();

        private static final Schema SCHEMA = Schema.createService(
            ShapeId.from("aws.sdk#BridgeService"));

        @Override
        public Schema schema() {
            return SCHEMA;
        }
    }

    /**
     * A minimal placeholder {@link ClientProtocol} to satisfy ClientConfig's non-null requirement.
     * The actual protocol is determined per-operation via ProtocolResolver and passed
     * through RequestOverrideConfig.
     */
    @SuppressWarnings("rawtypes")
    private enum PlaceholderProtocol implements ClientProtocol<HttpRequest, HttpResponse> {
        INSTANCE;

        private static final software.amazon.smithy.java.client.core.MessageExchange<HttpRequest, HttpResponse>
            HTTP_EXCHANGE = software.amazon.smithy.java.client.http.HttpMessageExchange.INSTANCE;

        @Override
        public ShapeId id() {
            return ShapeId.from("aws.protocols#placeholder");
        }

        @Override
        public software.amazon.smithy.java.core.serde.Codec payloadCodec() {
            return null;
        }

        @Override
        public software.amazon.smithy.java.client.core.MessageExchange<HttpRequest, HttpResponse> messageExchange() {
            return HTTP_EXCHANGE;
        }

        @Override
        public HttpRequest createRequest(
                ApiOperation operation,
                SerializableStruct input,
                Context context,
                software.amazon.smithy.java.io.uri.SmithyUri endpoint) {
            throw new UnsupportedOperationException(
                "PlaceholderProtocol should not be used for serialization. "
                + "Ensure the correct protocol is set via RequestOverrideConfig.");
        }

        @Override
        public SerializableStruct deserializeResponse(
                ApiOperation operation,
                Context context,
                software.amazon.smithy.java.core.serde.TypeRegistry typeRegistry,
                HttpRequest request,
                HttpResponse response) {
            throw new UnsupportedOperationException(
                "PlaceholderProtocol should not be used for deserialization. "
                + "Ensure the correct protocol is set via RequestOverrideConfig.");
        }

        @Override
        public HttpRequest setServiceEndpoint(HttpRequest request, software.amazon.smithy.java.endpoints.Endpoint endpoint) {
            throw new UnsupportedOperationException(
                "PlaceholderProtocol should not be used for endpoint setting. "
                + "Ensure the correct protocol is set via RequestOverrideConfig.");
        }
    }
}
