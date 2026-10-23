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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.HttpBinding;
import software.amazon.awssdk.httpbinding.RequestSerializer;
import software.amazon.awssdk.httpbinding.ResponseDeserializer;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventDecoderFactory;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventEncoderFactory;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * An HTTP protocol that uses the HTTP binding traits ({@code @http}, {@code @httpLabel}, {@code @httpQuery},
 * {@code @httpHeader}, {@code @httpPayload} and the others) to place input and output members, and the payload codec for
 * the body. Requests are serialized by the http-binding {@link RequestSerializer}, and responses deserialized by its
 * {@link ResponseDeserializer}; error responses are resolved by {@link #errorDeserializer()}.
 *
 * <p>Subclasses provide the {@link #payloadCodec() codec}, its {@link #payloadMediaType() media type} and the error
 * deserializer, and may change how empty bodies are written ({@link #omitEmptyPayload()},
 * {@link #allowEmptyStructPayload(Schema)}) and how greedy labels are encoded
 * ({@link #trimGreedyLabelLeadingSlash()}).
 *
 * <p>A {@code @streaming} blob payload member is skipped in both directions: the request has no body or
 * {@code Content-Type} for it, because the request pipeline binds the caller's {@code RequestBody} or
 * {@code AsyncRequestBody} with its own headers, and a successful response's body is left unread for the caller's
 * response transformer. An error response of such an operation is read as for any other operation (Requirement 14.3).
 *
 * <p>An event stream {@code @httpPayload} member is skipped the same way: the other input members are bound to the
 * request line and headers, the request gets {@code Content-Type: application/vnd.amazon.eventstream} and
 * {@code Transfer-Encoding: chunked}, and the request pipeline sends the events encoded by
 * {@link #inputEventEncoderFactory} as the body. A successful response's output members are read from the HTTP
 * response and its body is left for {@link #outputEventDecoderFactory}'s decoders.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.binding.HttpBindingClientProtocol}, without the
 * event stream frame type parameter: the frames are always {@code AwsEventFrame}s.
 */
@SdkPublicApi
@ThreadSafe
public abstract class HttpBindingClientProtocol extends HttpClientProtocol {
    private final HttpBinding httpBinding = HttpBinding.create();

    /**
     * Creates a protocol.
     *
     * @param id the shape ID of the protocol definition trait the protocol implements
     * @throws NullPointerException if {@code id} is null
     */
    protected HttpBindingClientProtocol(ShapeId id) {
        super(id);
    }

    /**
     * Returns the media type of the codec's output, sent as {@code Content-Type} of a codec-serialized body.
     *
     * @return the media type, for example {@code application/json}
     */
    protected abstract String payloadMediaType();

    /**
     * Returns the deserializer that creates the exception for an error response.
     *
     * @return the error deserializer
     */
    protected abstract HttpErrorDeserializer errorDeserializer();

    /**
     * Returns whether no body is sent for an input without body members and without a payload member. The default is
     * false: such an input is sent with the codec's empty structure.
     *
     * @return true to send no body
     */
    protected boolean omitEmptyPayload() {
        return false;
    }

    /**
     * Returns whether the codec's empty structure is sent when the input's structure {@code @httpPayload} member is
     * null. The default is false: no body is sent.
     *
     * @param inputSchema the operation input schema
     * @return true to send an empty structure
     */
    protected boolean allowEmptyStructPayload(Schema inputSchema) {
        return false;
    }

    /**
     * Returns whether one leading {@code /} of a greedy label value is dropped before it is encoded. The default is
     * false: {@code /a/{key+}} with {@code key = "/b"} is sent as {@code /a//b}.
     *
     * @return true to drop one leading {@code /}
     */
    protected boolean trimGreedyLabelLeadingSlash() {
        return false;
    }

    /**
     * Returns the HTTP binding instance the protocol serializes with.
     *
     * @return the HTTP binding
     */
    protected final HttpBinding httpBinding() {
        return httpBinding;
    }

    @Override
    protected final <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest createRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
        ByteBufferOutputStream body = newRequestBodyBuffer(operation);
        SdkHttpFullRequest request = httpBinding.requestSerializer()
                                                .operation(operation)
                                                .payloadCodec(payloadCodec())
                                                .payloadMediaType(payloadMediaType())
                                                .shapeValue(input)
                                                .endpoint(endpoint)
                                                .omitEmptyPayload(omitEmptyPayload())
                                                .allowEmptyStructPayload(allowEmptyStructPayload(input.sdkSchema()))
                                                .trimGreedyLabelLeadingSlash(trimGreedyLabelLeadingSlash())
                                                .bodyBuffer(body)
                                                .serializeRequest();
        // The request already holds the body when the codec wrote one into the buffer; this only records its size as
        // the next request's starting capacity.
        finishRequestBody(operation, body);
        if (isEventStream(operation.inputStreamMember())) {
            // The event stream is the @httpPayload, which the serializer skips; the other members are bound to the
            // request line and headers. The request pipeline sends the encoded events as the body.
            return putEventStreamHeaders(request.toBuilder()).build();
        }
        return request;
    }

    @Override
    public final AwsEventEncoderFactory inputEventEncoderFactory(ApiOperation<?, ?> operation) {
        return AwsEventEncoderFactory.forInputStream(operation, payloadCodec(), payloadMediaType(),
                                                     HttpClientProtocol::eventStreamInternalError);
    }

    @Override
    public final AwsEventDecoderFactory outputEventDecoderFactory(ApiOperation<?, ?> operation) {
        return AwsEventDecoderFactory.forOutputStream(operation, payloadCodec());
    }

    @Override
    protected final <I extends SerializableStruct, O extends SerializableStruct> O deserializeOutput(
        ApiOperation<I, O> operation, ExecutionAttributes context, SdkHttpFullRequest request,
        SdkHttpFullResponse response) {
        ShapeBuilder<O> builder = operation.outputBuilder();
        ResponseDeserializer deserializer = httpBinding.responseDeserializer()
                                                       .payloadCodec(payloadCodec())
                                                       .outputShapeBuilder(builder)
                                                       .response(response);
        if (!httpBinding.hasStreamingPayload(operation.outputSchema(), true)) {
            // The async transport's buffer is read without a copy, and a chunked body starts at the operation's last
            // body size. The body is parsed only when the output has body or payload members, so an output without
            // members ignores it.
            deserializer.body(readResponseBody(operation, context, response));
        }
        deserializer.deserialize();
        builder.errorCorrection();
        return buildOutput(builder, response);
    }

    @Override
    protected final SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                                    TypeRegistry errorRegistry, SdkHttpFullRequest request,
                                                    SdkHttpFullResponse response) {
        return errorDeserializer().createError(operation, context, errorRegistry, response);
    }
}
