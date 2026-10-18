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

package software.amazon.awssdk.protocols.client.http;

import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.awscore.util.AwsHeader;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.client.http.internal.MarshallBufferSizeTracker;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Base class for client protocols whose messages are SDK HTTP requests and responses ({@link HttpMessageExchange}).
 *
 * <p>Subclasses implement {@link #createRequest}, {@link #deserializeOutput} and {@link #createError}. This class
 * applies the contract of {@link ClientProtocol} around them:
 * <ul>
 *     <li>A runtime exception from {@link #createRequest} or {@link #deserializeOutput} that is not an
 *     {@link SdkException} becomes {@code SdkClientException("Unable to marshall <operation>: <cause>")} or
 *     {@code SdkClientException("Unable to unmarshall <operation>: <cause>")}, with the original exception as the cause,
 *     so no partial request or output is returned.</li>
 *     <li>A response that {@link #isErrorResponse} classifies as an error is passed to {@link #createError}, whose
 *     result is thrown.</li>
 *     <li>{@link #updateServiceEndpoint} moves a request to a new endpoint.</li>
 * </ul>
 *
 * <p>Subclasses that serialize a body into a {@link ByteBufferOutputStream} create it with
 * {@link #newRequestBodyBuffer} and finish it with {@link #finishRequestBody}, so each operation's next buffer starts at
 * the size of its last body. They build outputs with {@link #buildOutput}, which attaches the HTTP response and the
 * response metadata.
 *
 * <p>Instances are created once per client and must be immutable after construction, apart from the internal buffer
 * size hints, and safe to use from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.HttpClientProtocol}. The SDK adds the template
 * methods above, the client-side failure wrapping, the buffer size hints and {@link #buildOutput}.
 * {@link #updateServiceEndpoint} keeps the request headers unchanged; it does not add endpoint headers as smithy-java's
 * {@code setServiceEndpoint} does, because the SDK request pipeline applies them.
 */
@SdkPublicApi
@ThreadSafe
public abstract class HttpClientProtocol implements ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> {
    private final ShapeId id;
    private final MarshallBufferSizeTracker bufferSizeHints = new MarshallBufferSizeTracker();

    /**
     * Creates a protocol.
     *
     * @param id the shape ID of the protocol definition trait the protocol implements
     * @throws NullPointerException if {@code id} is null
     */
    protected HttpClientProtocol(ShapeId id) {
        this.id = Validate.paramNotNull(id, "id");
    }

    @Override
    public final ShapeId id() {
        return id;
    }

    /**
     * Returns {@link HttpMessageExchange#INSTANCE}.
     *
     * @return the HTTP message exchange
     */
    @Override
    public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
        return HttpMessageExchange.INSTANCE;
    }

    /**
     * Serializes an operation input with {@link #createRequest}.
     *
     * @throws SdkClientException if {@link #createRequest} throws an exception that is not an {@link SdkException};
     *     the message is {@code Unable to marshall <operation name>: <cause message>}
     */
    @Override
    public final <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest serializeRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
        try {
            return createRequest(operation, input, context, endpoint);
        } catch (SdkException e) {
            throw e;
        } catch (RuntimeException e) {
            throw clientFailure("marshall", operation, e);
        }
    }

    /**
     * Returns a copy of the request whose scheme, host and port are the endpoint's, and whose encoded path is the
     * endpoint's raw path followed by the request's encoded path, joined with one {@code /}. The query parameters,
     * headers and body are unchanged, and the request passed in is not modified.
     *
     * <p>For example, a request for {@code http://placeholder/service/Foo/operation/Bar} sent to
     * {@code https://example.com:8443/prefix} becomes {@code https://example.com:8443/prefix/service/Foo/operation/Bar}.
     *
     * @param request the serialized request
     * @param endpoint the endpoint to send it to
     * @return the updated request
     */
    @Override
    public SdkHttpFullRequest updateServiceEndpoint(SdkHttpFullRequest request, Endpoint endpoint) {
        URI url = Validate.paramNotNull(endpoint, "endpoint").url();
        String endpointPath = url.getRawPath() == null ? "" : url.getRawPath();
        return request.toBuilder()
                      .protocol(url.getScheme())
                      .host(url.getHost())
                      .port(url.getPort())
                      .encodedPath(SdkHttpUtils.appendUri(endpointPath, request.encodedPath()))
                      .build();
    }

    /**
     * Deserializes a response with {@link #deserializeOutput}, or throws the exception {@link #createError} returns for
     * an error response.
     *
     * @throws SdkServiceException for an error response
     * @throws SdkClientException if {@link #deserializeOutput} throws an exception that is not an {@link SdkException};
     *     the message is {@code Unable to unmarshall <operation name>: <cause message>}
     */
    @Override
    public final <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
        ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry, SdkHttpFullRequest request,
        SdkHttpFullResponse response) {
        if (isErrorResponse(response)) {
            throw createError(operation, context, errorRegistry, request, response);
        }
        try {
            return deserializeOutput(operation, context, request, response);
        } catch (SdkException e) {
            throw e;
        } catch (RuntimeException e) {
            throw clientFailure("unmarshall", operation, e);
        }
    }

    /**
     * Serializes an operation input into an HTTP request. Called by {@link #serializeRequest}.
     *
     * @param operation the operation being called
     * @param input the operation input
     * @param context the attributes of the current call
     * @param endpoint the endpoint the request is created for
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @return the request
     */
    protected abstract <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest createRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint);

    /**
     * Deserializes a successful HTTP response into the operation output. Called by {@link #deserializeResponse}.
     *
     * @param operation the operation being called
     * @param context the attributes of the current call
     * @param request the request that was sent
     * @param response the response
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @return the output
     */
    protected abstract <I extends SerializableStruct, O extends SerializableStruct> O deserializeOutput(
        ApiOperation<I, O> operation, ExecutionAttributes context, SdkHttpFullRequest request, SdkHttpFullResponse response);

    /**
     * Creates the exception for an error response, usually with an {@link HttpErrorDeserializer}. Called by
     * {@link #deserializeResponse}, which throws the result. Implementations must return the service's base exception
     * rather than throw when the response cannot be deserialized.
     *
     * @param operation the operation being called
     * @param context the attributes of the current call
     * @param errorRegistry the registry used to create modeled errors
     * @param request the request that was sent
     * @param response the error response
     * @return the exception to throw
     */
    protected abstract SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                                       TypeRegistry errorRegistry, SdkHttpFullRequest request,
                                                       SdkHttpFullResponse response);

    /**
     * Returns whether a response is an error response. The default treats any status code outside 2xx as an error.
     *
     * @param response the response
     * @return true if the response is an error
     */
    protected boolean isErrorResponse(SdkHttpFullResponse response) {
        return !response.isSuccessful();
    }

    /**
     * Creates a buffer for an operation's request body, starting at the size of the operation's last body.
     *
     * @param operation the operation being serialized
     * @return a new, empty buffer
     */
    protected final ByteBufferOutputStream newRequestBodyBuffer(ApiOperation<?, ?> operation) {
        return new ByteBufferOutputStream(bufferSizeHints.initialCapacity(operation));
    }

    /**
     * Records the size of a serialized request body as the starting size of the operation's next buffer, and returns
     * the body as a request content provider that shares the buffer's array (see
     * {@link ByteBufferOutputStream#toContentStreamProvider()}).
     *
     * @param operation the operation being serialized
     * @param body the buffer holding the serialized body
     * @return the request content
     */
    protected final ContentStreamProvider finishRequestBody(ApiOperation<?, ?> operation, ByteBufferOutputStream body) {
        bufferSizeHints.record(operation, body.size());
        return body.toContentStreamProvider();
    }

    /**
     * Builds an operation output from its deserialized builder, attaching the HTTP response the way earlier SDK clients
     * attach it to every response:
     * <ul>
     *     <li>an {@link AwsResponse.Builder} receives {@link AwsResponseMetadata} holding the first value of every
     *     response header, and the request ID ({@code x-amzn-RequestId} or {@code x-amz-request-id}) under
     *     {@link AwsHeader#AWS_REQUEST_ID};</li>
     *     <li>an {@link SdkResponse.Builder} receives the {@link SdkResponse#sdkHttpResponse() HTTP response}, set on
     *     the builder before it is built so the output is not copied to attach it;</li>
     *     <li>any other builder, such as a document builder of the Dynamic_Client, is built as is.</li>
     * </ul>
     *
     * @param builder the output builder, already deserialized
     * @param response the HTTP response
     * @param <O> the output type
     * @return the built output
     */
    protected final <O extends SerializableStruct> O buildOutput(ShapeBuilder<O> builder, SdkHttpFullResponse response) {
        return buildOutput(builder, response, null);
    }

    /**
     * Like {@link #buildOutput(ShapeBuilder, SdkHttpFullResponse)}, for protocols that also send the request ID in the
     * response body, such as awsQuery's {@code <ResponseMetadata><RequestId>}: a non-null {@code bodyRequestId} is the
     * {@link AwsHeader#AWS_REQUEST_ID} of the response metadata instead of the request ID headers, as in
     * {@code AwsQueryResponseHandler}.
     *
     * @param builder the output builder, already deserialized
     * @param response the HTTP response
     * @param bodyRequestId the request ID read from the body, or null to use the request ID headers
     * @param <O> the output type
     * @return the built output
     */
    protected final <O extends SerializableStruct> O buildOutput(ShapeBuilder<O> builder, SdkHttpFullResponse response,
                                                                 String bodyRequestId) {
        if (builder instanceof AwsResponse.Builder) {
            ((AwsResponse.Builder) builder).responseMetadata(responseMetadata(response, bodyRequestId));
        }
        if (builder instanceof SdkResponse.Builder) {
            return SchemaSerdeSupport.buildResponse(builder, response);
        }
        return builder.build();
    }

    /**
     * Same metadata as {@code AwsJsonResponseHandler}, {@code AwsXmlResponseHandler} and {@code AwsQueryResponseHandler}.
     */
    private static AwsResponseMetadata responseMetadata(SdkHttpResponse response, String bodyRequestId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(AwsHeader.AWS_REQUEST_ID,
                     bodyRequestId != null
                     ? bodyRequestId
                     : response.firstMatchingHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADERS).orElse(null));
        response.forEachHeader((key, value) -> metadata.put(key, value.get(0)));
        return DefaultAwsResponseMetadata.create(metadata);
    }

    private static SdkClientException clientFailure(String action, ApiOperation<?, ?> operation, RuntimeException e) {
        Throwable cause = e instanceof UncheckedIOException && e.getCause() != null ? e.getCause() : e;
        String causeMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
        return SdkClientException.builder()
                                 .message("Unable to " + action + " " + operation.name() + ": " + causeMessage)
                                 .cause(cause)
                                 .build();
    }
}
