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

package software.amazon.awssdk.core.internal.http;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkProtocolMetadata;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.CompressRequestStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.HttpChecksumStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.QueryParametersToBodyStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * The request mutations of the schema-based request pipeline, shared unchanged by the sync and async pipelines. One
 * instance is created per client configuration and client type, and every call reuses it.
 *
 * <p>The legacy pipeline mutates a request in ten nested stages ({@code MakeRequestMutableStage} through
 * {@code MakeRequestImmutableStage}) and then copies it once more per attempt for the retry-info header. Here the same
 * mutations run as direct calls, in the legacy order, in one pass over a single request builder (racecar's flattened
 * mutation stages, re-implemented):
 * <ol>
 *     <li>invocation ID ({@link ApplyTransactionIdStage});</li>
 *     <li>client and request custom headers ({@link MergeCustomHeadersStage});</li>
 *     <li>request custom query parameters ({@link MergeCustomQueryParamsStage}), skipped when the request adds none;</li>
 *     <li>query parameters moved to the body for the query protocols ({@link QueryParametersToBodyStage}), without
 *     building the request just to check the condition;</li>
 *     <li>request compression ({@link CompressRequestStage}, the legacy stage, used unchanged);</li>
 *     <li>auth scheme resolution ({@link AuthSchemeResolutionCache});</li>
 *     <li>endpoint resolution ({@link EndpointResolution}, applied through {@link ClientProtocol#updateServiceEndpoint});
 *     </li>
 *     <li>flexible checksums ({@link HttpChecksumStage}, the legacy stage, used unchanged);</li>
 *     <li>{@code User-Agent} ({@link UserAgentHeaderCache}, racecar H9);</li>
 *     <li>the first attempt's retry-info header {@code amz-sdk-request} (racecar B2.b; {@link RetryDecisions} stamps
 *     the header of later attempts).</li>
 * </ol>
 * Each produces the headers, query parameters, body and URI of the legacy stage it replaces.
 *
 * <p>{@link #marshall} runs the steps the legacy client handler runs between {@code beforeMarshalling} and the
 * pipeline: serialization through the protocol, addressing the request to the client endpoint with the host prefix
 * (before {@code afterMarshalling}, so interceptors see the same host as with a legacy marshaller), the URI snapshot,
 * {@code modifyHttpRequest}/{@code modifyHttpContent}, the signer check and the request body re-attach.
 */
@SdkInternalApi
@ThreadSafe
public final class RequestMutationStages {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset="
                                                    + lowerCase(StandardCharsets.UTF_8.toString());

    private final ClientType clientType;
    private final Map<String, List<String>> additionalHeaders;
    private final boolean hostPrefixInjectionDisabled;
    private final ClientEndpointProvider clientEndpointProvider;
    private final int maxAttempts;
    private final UserAgentHeaderCache userAgentHeaderCache;
    private final AuthSchemeResolutionCache authSchemeResolutionCache;
    private final CompressRequestStage compressRequestStage;
    private final HttpChecksumStage httpChecksumStage;
    private volatile ClientEndpoint clientEndpoint;

    private RequestMutationStages(HttpClientDependencies dependencies, ClientType clientType) {
        SdkClientConfiguration config = dependencies.clientConfiguration();
        this.clientType = clientType;
        this.additionalHeaders = config.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS);
        this.hostPrefixInjectionDisabled =
            Boolean.TRUE.equals(config.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION));
        this.clientEndpointProvider = config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        this.maxAttempts = RetryDecisions.maxAttempts(config);
        this.userAgentHeaderCache = UserAgentHeaderCache.create(config);
        this.authSchemeResolutionCache = AuthSchemeResolutionCache.create(config);
        this.compressRequestStage = new CompressRequestStage(dependencies);
        this.httpChecksumStage = new HttpChecksumStage(clientType);
    }

    /**
     * Creates the mutation stages of a client.
     *
     * @param dependencies the client's dependencies, whose configuration the stages read once
     * @param clientType {@link ClientType#SYNC} or {@link ClientType#ASYNC}; it selects the legacy behaviors that
     *     differ between the two (checksum trailers, request body re-attach)
     */
    public static RequestMutationStages create(HttpClientDependencies dependencies, ClientType clientType) {
        return new RequestMutationStages(dependencies, clientType);
    }

    /**
     * The client's auth scheme resolution cache, also used to seed the placeholder auth scheme of a call.
     */
    public AuthSchemeResolutionCache authSchemeResolutionCache() {
        return authSchemeResolutionCache;
    }

    /**
     * Serializes the call's input and runs the HTTP request interceptors, as the legacy client handler does before it
     * calls the pipeline ({@code BaseClientHandler.finalizeSdkHttpFullRequest} and the handlers' request body
     * re-attach):
     * <ol>
     *     <li>{@link ProtocolMarshalling#marshall(ExecutionContext, java.util.function.UnaryOperator)}:
     *     {@code beforeMarshalling}, serialization, then, before {@code afterMarshalling}, the request is addressed to
     *     the client endpoint through {@link ClientProtocol#updateServiceEndpoint}, with the discovered endpoint's host
     *     and port, or with the host prefix unless host prefix injection is disabled;</li>
     *     <li>the {@code HTTP_REQUEST_URI_BEFORE_MODIFY} snapshot, used by endpoint resolution to detect an interceptor
     *     that moved the request;</li>
     *     <li>{@code modifyHttpRequest}, {@code modifyHttpContent} and {@code modifyAsyncHttpContent};</li>
     *     <li>the bearer-token signer check (plain HTTP is rejected);</li>
     *     <li>the interceptors' request body put back on the request: for the sync pipeline when there is one, for the
     *     async pipeline when there is one and no asynchronous body; the sync pipeline also sets the result on the
     *     interceptor context, as {@code AmazonSyncHttpClient} does.</li>
     * </ol>
     *
     * @param executionContext the call's execution context, after {@code modifyRequest}
     * @param hostPrefix the operation's resolved host prefix, or null
     * @param discoveredEndpoint the endpoint from endpoint discovery, or null
     * @return the request to prepare with {@link #prepare}
     */
    public SdkHttpFullRequest marshall(ExecutionContext executionContext, String hostPrefix, URI discoveredEndpoint) {
        ExecutionAttributes attributes = executionContext.executionAttributes();
        ProtocolMarshalling.marshall(executionContext,
                                     request -> address(request, attributes, hostPrefix, discoveredEndpoint));

        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY,
                                executionContext.interceptorContext().httpRequest().getUri());
        InterceptorContext modified = executionContext.interceptorChain()
                                                      .modifyHttpRequestAndHttpContent(executionContext.interceptorContext(),
                                                                                       attributes);
        executionContext.interceptorContext(modified);

        SdkHttpFullRequest marshalled = (SdkHttpFullRequest) modified.httpRequest();
        validateSigningConfiguration(marshalled, executionContext.signer());

        Optional<RequestBody> requestBody = modified.requestBody();
        boolean reattach = requestBody.isPresent()
                           && (clientType != ClientType.ASYNC || !modified.asyncRequestBody().isPresent());
        if (reattach) {
            marshalled = marshalled.toBuilder().contentStreamProvider(requestBody.get().contentStreamProvider()).build();
        }
        if (clientType != ClientType.ASYNC) {
            SdkHttpFullRequest finalRequest = marshalled;
            executionContext.interceptorContext(executionContext.interceptorContext()
                                                                .copy(b -> b.httpRequest(finalRequest)));
        }
        return marshalled;
    }

    /**
     * Runs the request mutations in one pass and returns the first attempt's unsigned request.
     *
     * @param request the request {@link #marshall} returned
     * @param context the call's context
     * @return the request with every mutation applied, addressed to the resolved endpoint, with the retry-info header
     *     of attempt 1
     * @throws Exception as the legacy stages throw (an {@code InterruptedException} when the thread is interrupted, the
     *     checksum and compression stages' failures)
     */
    public SdkHttpFullRequest prepare(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        SdkHttpFullRequest.Builder builder = request.toBuilder();

        InterruptMonitor.checkInterrupted();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        builder.putHeader(ApplyTransactionIdStage.HEADER_SDK_TRANSACTION_ID,
                          new UUID(random.nextLong(), random.nextLong()).toString());

        mergeCustomHeaders(builder, context);
        mergeCustomQueryParameters(builder, context);
        builder = queryParametersToBody(builder, context);
        builder = compressRequestStage.execute(builder, context);
        authSchemeResolutionCache.resolve(context);
        Endpoint endpoint = EndpointResolution.resolve(builder, context);
        builder = httpChecksumStage.execute(builder, context);

        String userAgent = userAgentHeaderCache.headerValue(context);
        if (userAgent != null) {
            builder.putHeader(ApplyUserAgentStage.HEADER_USER_AGENT, userAgent);
        }
        builder.putHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER, RetryDecisions.retryInfo(1, maxAttempts));

        return EndpointResolution.apply(builder.build(), endpoint, context.executionAttributes());
    }

    /**
     * Addresses the serialized request to the client endpoint, as legacy marshallers do, and then applies the
     * discovered endpoint or the host prefix as {@code BaseClientHandler.modifyEndpointHostIfNeeded} does. Without a
     * client endpoint the request keeps the host of {@link ProtocolMarshalling#UNRESOLVED_URI}; the discovered endpoint
     * and the host prefix are still applied, as the legacy handler applies them to whatever the marshaller produced.
     */
    private SdkHttpFullRequest address(SdkHttpFullRequest request,
                                       ExecutionAttributes attributes,
                                       String hostPrefix,
                                       URI discoveredEndpoint) {
        Endpoint endpoint = clientEndpoint();
        SdkHttpFullRequest addressed = request;
        if (endpoint != null) {
            ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol = ProtocolMarshalling.httpProtocol(attributes);
            addressed = protocol.updateServiceEndpoint(request, endpoint);
        }

        if (discoveredEndpoint != null) {
            attributes.putAttribute(SdkInternalExecutionAttribute.SKIP_ENDPOINT_RESOLUTION, true);
            attributes.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
            return addressed.toBuilder().host(discoveredEndpoint.getHost()).port(discoveredEndpoint.getPort()).build();
        }
        if (hostPrefixInjectionDisabled || StringUtils.isEmpty(hostPrefix)) {
            return addressed;
        }
        return addressed.toBuilder().host(hostPrefix + addressed.host()).build();
    }

    /**
     * The client endpoint as an {@link Endpoint}, created once per client endpoint URI.
     */
    private Endpoint clientEndpoint() {
        if (clientEndpointProvider == null) {
            return null;
        }
        URI uri = clientEndpointProvider.clientEndpoint();
        ClientEndpoint cached = clientEndpoint;
        if (cached == null || cached.uri != uri) {
            cached = new ClientEndpoint(uri, Endpoint.builder().url(uri).build());
            clientEndpoint = cached;
        }
        return cached.endpoint;
    }

    /**
     * Same as {@code BaseClientHandler.validateSigningConfiguration}.
     */
    private static void validateSigningConfiguration(SdkHttpFullRequest request, Signer signer) {
        if (signer == null || signer.credentialType() != CredentialType.TOKEN) {
            return;
        }
        URI endpoint = request.getUri();
        if (!"https".equals(endpoint.getScheme())) {
            throw SdkClientException.create("Cannot use bearer token signer with a plaintext HTTP endpoint: " + endpoint);
        }
    }

    /**
     * Same as {@link MergeCustomHeadersStage}.
     */
    private void mergeCustomHeaders(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        addOverrideHeaders(request, additionalHeaders);
        addOverrideHeaders(request, context.requestConfig().headers());
    }

    private static void addOverrideHeaders(SdkHttpFullRequest.Builder request, Map<String, List<String>> overrideHeaders) {
        if (overrideHeaders == null || overrideHeaders.isEmpty()) {
            return;
        }
        overrideHeaders.forEach((headerName, headerValues) -> {
            if (SdkHttpUtils.isSingleHeader(headerName)) {
                request.removeHeader(headerName);
            }
            headerValues.forEach(v -> request.appendHeader(headerName, v));
        });
    }

    /**
     * Same result as {@link MergeCustomQueryParamsStage}, which rewrites the query parameters with an unchanged copy
     * when the request adds none.
     */
    private static void mergeCustomQueryParameters(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        Map<String, List<String>> custom = context.requestConfig().rawQueryParameters();
        if (custom.isEmpty()) {
            return;
        }
        Map<String, List<String>> merged = new LinkedHashMap<>(request.numRawQueryParameters());
        request.forEachRawQueryParameter(merged::put);
        custom.forEach((key, val) -> merged.put(key, CollectionUtils.mergeLists(merged.get(key), val)));
        request.rawQueryParameters(merged);
    }

    /**
     * Same as {@link QueryParametersToBodyStage}; the request is built only when the parameters are moved.
     */
    private static SdkHttpFullRequest.Builder queryParametersToBody(SdkHttpFullRequest.Builder request,
                                                                    RequestExecutionContext context) {
        SdkProtocolMetadata protocolMetadata =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.PROTOCOL_METADATA);
        if (protocolMetadata == null) {
            return request;
        }
        String protocol = protocolMetadata.serviceProtocol();
        boolean isQueryProtocol = "query".equalsIgnoreCase(protocol) || "ec2".equalsIgnoreCase(protocol);
        if (!isQueryProtocol
            || request.method() != SdkHttpMethod.POST
            || request.contentStreamProvider() != null
            || request.numRawQueryParameters() == 0) {
            return request;
        }
        SdkHttpFullRequest built = request.build();
        byte[] params = built.encodedQueryParametersAsFormData().orElse("").getBytes(StandardCharsets.UTF_8);
        return built.toBuilder()
                    .clearQueryParameters()
                    .contentStreamProvider(() -> new ByteArrayInputStream(params))
                    .putHeader("Content-Length", singletonList(String.valueOf(params.length)))
                    .putHeader("Content-Type", singletonList(FORM_CONTENT_TYPE));
    }

    private static final class ClientEndpoint {
        private final URI uri;
        private final Endpoint endpoint;

        private ClientEndpoint(URI uri, Endpoint endpoint) {
            this.uri = uri;
            this.endpoint = endpoint;
        }
    }
}
