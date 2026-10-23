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

package software.amazon.awssdk.httpbinding;

import java.net.URI;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSerializer;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Serializes an HTTP request from an input shape that uses HTTP binding traits.
 *
 * <ul>
 *     <li>Labels are percent-encoded with the RFC 3986 unreserved set; {@code /} is kept only in a greedy label. A null
 *     or empty label throws {@link software.amazon.awssdk.schema.serde.SerializationException}.</li>
 *     <li>Query parameters are set as raw parameters; the {@link SdkHttpFullRequest} percent-encodes them with the same
 *     unreserved set. A list gives one parameter per element; an {@code @httpQueryParams} entry whose name an
 *     {@code @httpQuery} member or a query literal of the URI pattern also writes is dropped.</li>
 *     <li>Headers use the name the model declares. A list is one header joined with {@code ", "}; timestamps default to
 *     {@code http-date}, query and label timestamps to {@code date-time}.</li>
 *     <li>The unbound members are the body, written by the codec, with {@code Content-Type} set to the payload media
 *     type and {@code Content-Length} set. A blob or string {@code @httpPayload} is the raw body.</li>
 * </ul>
 *
 * <p>A {@code @streaming} payload member is not written (no body is set); the protocol binds the stream (task 7.19).
 * Event streams are not supported yet (task 7.20).
 */
@SdkPublicApi
public final class RequestSerializer {
    private static final String CONTENT_LENGTH = "Content-Length";

    private Codec payloadCodec;
    private ApiOperation<?, ?> operation;
    private String payloadMediaType;
    private URI endpoint;
    private SerializableShape shapeValue;
    private ByteBufferOutputStream bodyBuffer;
    private boolean omitEmptyPayload;
    private boolean allowEmptyStructPayload;
    private boolean trimGreedyLabelLeadingSlash;

    RequestSerializer() {
    }

    /**
     * Sets the operation to serialize; its schema carries {@code smithy.api#http}.
     *
     * @param operation the operation
     * @return this serializer
     */
    public RequestSerializer operation(ApiOperation<?, ?> operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Sets the codec for the body and structured payloads.
     *
     * @param payloadCodec the payload codec
     * @return this serializer
     */
    public RequestSerializer payloadCodec(Codec payloadCodec) {
        this.payloadCodec = payloadCodec;
        return this;
    }

    /**
     * Sets the media type of codec output, sent as {@code Content-Type}.
     *
     * @param payloadMediaType the media type
     * @return this serializer
     */
    public RequestSerializer payloadMediaType(String payloadMediaType) {
        this.payloadMediaType = payloadMediaType;
        return this;
    }

    /**
     * Sets the endpoint. The request path is the endpoint's raw path joined with the serialized path.
     *
     * @param endpoint the endpoint
     * @return this serializer
     */
    public RequestSerializer endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Sets the input shape to serialize.
     *
     * @param shapeValue the input shape
     * @return this serializer
     */
    public RequestSerializer shapeValue(SerializableShape shapeValue) {
        this.shapeValue = shapeValue;
        return this;
    }

    /**
     * When true, no body is written for an input without body members or payload member.
     *
     * @param omitEmptyPayload true to omit an empty body
     * @return this serializer
     */
    public RequestSerializer omitEmptyPayload(boolean omitEmptyPayload) {
        this.omitEmptyPayload = omitEmptyPayload;
        return this;
    }

    /**
     * When true, the codec's empty structure is the body when an input with a payload member leaves it null.
     *
     * @param allowEmptyStructPayload true to write an empty structure body
     * @return this serializer
     */
    public RequestSerializer allowEmptyStructPayload(boolean allowEmptyStructPayload) {
        this.allowEmptyStructPayload = allowEmptyStructPayload;
        return this;
    }

    /**
     * When true, one leading {@code /} of a greedy label value is dropped before it is encoded, so {@code /a/{key+}}
     * with {@code key = "/b/c"} is sent as {@code /a/b/c} instead of {@code /a//b/c}. The earlier restJson1 marshallers
     * of the SDK do this; restXml keeps the slash. SDK addition; the default is false.
     *
     * @param trimGreedyLabelLeadingSlash true to drop one leading {@code /}
     * @return this serializer
     */
    public RequestSerializer trimGreedyLabelLeadingSlash(boolean trimGreedyLabelLeadingSlash) {
        this.trimGreedyLabelLeadingSlash = trimGreedyLabelLeadingSlash;
        return this;
    }

    /**
     * Sets the buffer the codec writes the body members into, for example one sized from earlier requests. SDK
     * addition.
     *
     * @param bodyBuffer the buffer
     * @return this serializer
     */
    public RequestSerializer bodyBuffer(ByteBufferOutputStream bodyBuffer) {
        this.bodyBuffer = bodyBuffer;
        return this;
    }

    /**
     * Serializes the request.
     *
     * @return the request
     * @throws NullPointerException if the operation, codec, media type, endpoint or shape value is not set
     * @throws software.amazon.awssdk.schema.serde.SerializationException if a member cannot be serialized
     */
    public SdkHttpFullRequest serializeRequest() {
        Validate.paramNotNull(shapeValue, "shapeValue");
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(payloadCodec, "payloadCodec");
        Validate.paramNotNull(endpoint, "endpoint");
        Validate.paramNotNull(payloadMediaType, "payloadMediaType");

        HttpBindingSerializer serializer =
            new HttpBindingSerializer(operation.schema(), payloadCodec, payloadMediaType, false)
                .omitEmptyPayload(omitEmptyPayload)
                .allowEmptyStructPayload(allowEmptyStructPayload)
                .trimGreedyLabelLeadingSlash(trimGreedyLabelLeadingSlash)
                .bodyBuffer(bodyBuffer);
        shapeValue.serialize(serializer);

        String endpointPath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                               .method(serializer.sdkHttpMethod())
                                                               .uri(endpoint)
                                                               .encodedPath(SdkHttpUtils.appendUri(endpointPath,
                                                                                                   serializer.path()))
                                                               .headers(serializer.headers());
        Map<String, List<String>> query = serializer.rawQueryParameters();
        if (!query.isEmpty()) {
            builder.rawQueryParameters(query);
        }
        ContentStreamProvider body = serializer.body();
        if (body != null) {
            builder.contentStreamProvider(body)
                   .putHeader(CONTENT_LENGTH, Long.toString(serializer.bodyLength()));
        }
        return builder.build();
    }
}
