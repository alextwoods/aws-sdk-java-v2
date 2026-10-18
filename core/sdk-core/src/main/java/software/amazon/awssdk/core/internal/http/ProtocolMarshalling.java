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

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.utils.io.LengthAwareInputStream;

/**
 * The serialization step of the schema-based request pipeline (design D.6, seam 1). Only the new pipeline path
 * ({@code SyncApiCallPipeline}/{@code AsyncApiCallPipeline}) calls it; the legacy path keeps using generated
 * marshallers through {@code BaseClientHandler}.
 *
 * <p>{@link #marshall(ExecutionContext)} is the counterpart of the marshalling part of the legacy
 * {@code BaseClientHandler.finalizeSdkHttpFullRequest}: it runs {@code beforeMarshalling}, serializes the input
 * through the call's {@link ClientProtocol}, records the request in the interceptor context and runs
 * {@code afterMarshalling}. Addressing the request to the client endpoint and host prefix before {@code afterMarshalling}
 * is passed in by the caller ({@link #marshall(ExecutionContext, UnaryOperator)}), and the steps the legacy method runs
 * afterwards ({@code modifyHttpRequest}/{@code modifyHttpContent}, the URI snapshot for endpoint resolution) are in
 * {@link RequestMutationStages#marshall}.
 *
 * <p>The class references no generated marshaller type (Requirement 37.2): the operation comes from
 * {@link SdkExecutionAttribute#API_OPERATION}, so the protocol serializes the same {@link ApiOperation} instance that
 * interceptors see (Requirement 30.3).
 */
@SdkInternalApi
public final class ProtocolMarshalling {
    /**
     * The endpoint passed to {@link ClientProtocol#serializeRequest}. The endpoint step replaces it through
     * {@link ClientProtocol#updateServiceEndpoint} with the resolved endpoint. The host is in the reserved
     * {@code .invalid} top-level domain (RFC 6761), so a request that skipped endpoint resolution can never reach a
     * real host.
     */
    public static final URI UNRESOLVED_URI = URI.create("https://unresolved.invalid");

    private ProtocolMarshalling() {
    }

    /**
     * Serializes the call's input into the HTTP request, running the marshalling interceptor hooks around it.
     *
     * <ol>
     *     <li>the protocol is selected: {@link SdkExecutionAttribute#CLIENT_PROTOCOL}, for the operation in
     *     {@link SdkExecutionAttribute#API_OPERATION};</li>
     *     <li>one DEBUG entry {@code Using protocol <protocol id> for operation <operation id>} on the
     *     {@code software.amazon.awssdk.request} logger, with no member values (Requirements 40.4, 40.5);</li>
     *     <li>{@code beforeMarshalling} with the current interceptor context;</li>
     *     <li>{@code protocol.serializeRequest(operation, input, attributes, UNRESOLVED_URI)}, where the input is the
     *     interceptor context's request (after {@code modifyRequest}), with an unset {@code @idempotencyToken} member
     *     filled from {@code IdempotentUtils.getGenerator()} in a view of the input (the request itself is not
     *     modified, see {@link IdempotencyTokens}); the time it takes is reported as
     *     {@link CoreMetric#MARSHALLING_DURATION};</li>
     *     <li>for an input with a streaming blob member, the call's {@link RequestBody} or async request body from the
     *     interceptor context is bound to the request without being read, with the legacy streaming marshallers'
     *     headers ({@link StreamingRequestBodies}), within the measured marshalling;</li>
     *     <li>the request, and its body as a {@link RequestBody} when it has one, are set on the interceptor context, as
     *     the legacy handler does;</li>
     *     <li>{@code afterMarshalling}.</li>
     * </ol>
     *
     * <p>Exceptions from the protocol propagate unchanged: the protocol already reports a serialization failure as
     * {@code SdkClientException("Unable to marshall <op>: <cause>")}.
     *
     * @param executionContext the call's execution context, whose execution attributes hold the operation and the
     *     protocol
     * @return the serialized request, addressed to {@link #UNRESOLVED_URI}
     * @throws IllegalStateException if the operation or the protocol is not set, or the protocol does not exchange
     *     {@link SdkHttpFullRequest}/{@link SdkHttpFullResponse} messages
     * @throws SdkClientException if the request is not a schema-based ({@link SerializableStruct}) request, or the
     *     protocol cannot serialize it
     */
    public static SdkHttpFullRequest marshall(ExecutionContext executionContext) {
        return marshall(executionContext, UnaryOperator.identity());
    }

    /**
     * Same as {@link #marshall(ExecutionContext)}, with the serialized request passed through {@code addressing} before
     * it is set on the interceptor context and {@code afterMarshalling} runs. The new pipeline path uses it to address
     * the request to the client endpoint (with the host prefix), so {@code afterMarshalling} and
     * {@code modifyHttpRequest} see the same host as with a legacy marshaller ({@link RequestMutationStages#marshall}).
     *
     * @param executionContext the call's execution context
     * @param addressing applied to the request {@link ClientProtocol#serializeRequest} returns
     * @return the serialized and addressed request
     */
    public static SdkHttpFullRequest marshall(ExecutionContext executionContext,
                                              UnaryOperator<SdkHttpFullRequest> addressing) {
        ExecutionAttributes attributes = executionContext.executionAttributes();
        ApiOperation<?, ?> operation = operation(attributes);
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol = httpProtocol(attributes);
        // Logged as soon as the protocol is selected, so the entry is there even when serialization fails. Only the two
        // shape IDs are logged, never input values (Requirement 40.5).
        SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Using protocol " + protocol.id()
                                                      + " for operation " + operation.schema().id());

        executionContext.interceptorChain().beforeMarshalling(executionContext.interceptorContext(), attributes);

        InterceptorContext beforeMarshalling = executionContext.interceptorContext();
        SdkRequest input = beforeMarshalling.request();
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean measure = MetricUtils.collectsMetrics(metricCollector);
        long start = measure ? System.nanoTime() : 0L;
        SdkHttpFullRequest request = serialize(protocol, operation, input, attributes);
        // As the legacy streaming marshallers wrap the generated marshaller, inside the measured marshalling.
        request = StreamingRequestBodies.bind(operation, request, beforeMarshalling);
        if (measure) {
            metricCollector.reportMetric(CoreMetric.MARSHALLING_DURATION, Duration.ofNanos(System.nanoTime() - start));
        }
        request = addressing.apply(request);

        executionContext.interceptorContext(withHttpRequest(executionContext.interceptorContext(), request));
        executionContext.interceptorChain().afterMarshalling(executionContext.interceptorContext(), attributes);
        return request;
    }

    /**
     * Returns the call's {@link SdkExecutionAttribute#API_OPERATION}.
     *
     * @throws IllegalStateException if it is not set
     */
    static ApiOperation<?, ?> operation(ExecutionAttributes attributes) {
        ApiOperation<?, ?> operation = attributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
        if (operation == null) {
            throw new IllegalStateException("The schema-based request pipeline requires the "
                                            + "SdkExecutionAttribute.API_OPERATION execution attribute");
        }
        return operation;
    }

    /**
     * Returns the call's {@link SdkExecutionAttribute#CLIENT_PROTOCOL} as an HTTP protocol. Client builders already
     * reject a protocol whose message exchange differs from the transport's; this check keeps the cast safe for a
     * protocol set some other way.
     *
     * @throws IllegalStateException if it is not set or does not exchange HTTP messages
     */
    @SuppressWarnings("unchecked")
    static ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> httpProtocol(ExecutionAttributes attributes) {
        ClientProtocol<?, ?> protocol = attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL);
        if (protocol == null) {
            throw new IllegalStateException("The schema-based request pipeline requires the "
                                            + "SdkExecutionAttribute.CLIENT_PROTOCOL execution attribute");
        }
        if (!HttpMessageExchange.INSTANCE.equals(protocol.messageExchange())) {
            throw new IllegalStateException("Protocol " + protocol.id() + " exchanges " + protocol.messageExchange()
                                            + ", but the request pipeline sends " + HttpMessageExchange.INSTANCE);
        }
        return (ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse>) protocol;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SdkHttpFullRequest serialize(ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol,
                                                ApiOperation operation,
                                                SdkRequest input,
                                                ExecutionAttributes attributes) {
        if (!(input instanceof SerializableStruct)) {
            throw SdkClientException.create("Unable to marshall " + operation.name() + ": the request "
                                            + (input == null ? "is null" : input.getClass().getName()
                                                                           + " is not a schema-based request"));
        }
        SerializableStruct struct = IdempotencyTokens.fill(operation, (SerializableStruct) input);
        return protocol.serializeRequest(operation, struct, attributes, UNRESOLVED_URI);
    }

    /**
     * Same as the legacy {@code BaseClientHandler.addHttpRequest}: interceptors see the request, and its content as a
     * {@link RequestBody} limited to the {@code Content-Length} header when there is one.
     */
    private static InterceptorContext withHttpRequest(InterceptorContext context, SdkHttpFullRequest request) {
        Optional<ContentStreamProvider> content = request.contentStreamProvider();
        if (!content.isPresent()) {
            return context.copy(b -> b.httpRequest(request));
        }
        RequestBody body = requestBody(request, content.get());
        return context.copy(b -> b.httpRequest(request).requestBody(body));
    }

    private static RequestBody requestBody(SdkHttpFullRequest request, ContentStreamProvider content) {
        Optional<String> contentLengthHeader = request.firstMatchingHeader("Content-Length");
        long contentLength = Long.parseLong(contentLengthHeader.orElse("0"));
        String contentType = request.firstMatchingHeader("Content-Type").orElse("");
        ContentStreamProvider provider = contentLengthHeader.isPresent()
                                         ? () -> new LengthAwareInputStream(content.newStream(), contentLength)
                                         : content;
        return new InterceptorRequestBody(provider, contentLength, contentType);
    }

    /**
     * Exposes the protected {@link RequestBody} constructor, which does not buffer the content, as the legacy
     * {@code SdkInternalOnlyRequestBody} does (that class is package-private in the handler package).
     */
    private static final class InterceptorRequestBody extends RequestBody {
        private InterceptorRequestBody(ContentStreamProvider contentStreamProvider, long contentLength, String contentType) {
            super(contentStreamProvider, contentLength, contentType);
        }
    }
}
