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

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.internal.HttpBindingDeserializer;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * Deserializes the HTTP response of an operation that uses HTTP bindings into an output or error builder.
 *
 * <ul>
 *     <li>{@code @httpHeader} members are read from the header with the declared name, compared case-insensitively;
 *     an absent header leaves the member unset. A value that does not convert throws
 *     {@link software.amazon.awssdk.schema.serde.SerializationException} naming the member.</li>
 *     <li>{@code @httpPrefixHeaders} gets one entry per header whose name starts with the prefix.</li>
 *     <li>{@code @httpResponseCode} gets the status code.</li>
 *     <li>{@code @httpPayload} is read from the whole body (raw for blobs and strings, else through the codec); an
 *     empty body leaves it unset. A {@code @streaming} payload is left to the protocol and the body is not read.</li>
 *     <li>Unbound members are read from the body through the codec.</li>
 * </ul>
 *
 * <p>The body is read on first use from {@link #body(ByteBuffer)}, else from the response content, which is read to
 * the end but not closed.
 */
@SdkPublicApi
public final class ResponseDeserializer {
    private final HttpBindingDeserializer.Builder deserBuilder = HttpBindingDeserializer.builder().isResponse(true);
    private ShapeBuilder<?> outputShapeBuilder;
    private ShapeBuilder<?> errorShapeBuilder;
    private ByteBuffer body;
    private SdkHttpFullResponse response;

    ResponseDeserializer() {
    }

    /**
     * Sets the codec for the body and structured payloads.
     *
     * @param payloadCodec the payload codec
     * @return this deserializer
     */
    public ResponseDeserializer payloadCodec(Codec payloadCodec) {
        deserBuilder.payloadCodec(payloadCodec);
        return this;
    }

    /**
     * Sets the expected media type of a codec body. When set, a {@code Content-Type} that does not match throws
     * {@link software.amazon.awssdk.schema.serde.SerializationException}; a missing {@code Content-Type} is accepted.
     * When unset, the body is parsed without a check.
     *
     * @param payloadMediaType the media type
     * @return this deserializer
     */
    public ResponseDeserializer payloadMediaType(String payloadMediaType) {
        deserBuilder.payloadMediaType(payloadMediaType);
        return this;
    }

    /**
     * Sets the HTTP response to deserialize.
     *
     * @param response the response
     * @return this deserializer
     */
    public ResponseDeserializer response(SdkHttpFullResponse response) {
        this.response = response;
        return this;
    }

    /**
     * Sets the whole response body, for example the transport's buffer, so the response content is not read. SDK
     * addition.
     *
     * @param body the body
     * @return this deserializer
     */
    public ResponseDeserializer body(ByteBuffer body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the output builder to populate; clears any error builder.
     *
     * @param outputShapeBuilder the output builder
     * @return this deserializer
     */
    public ResponseDeserializer outputShapeBuilder(ShapeBuilder<?> outputShapeBuilder) {
        this.outputShapeBuilder = outputShapeBuilder;
        this.errorShapeBuilder = null;
        return this;
    }

    /**
     * Sets the error builder to populate; clears any output builder.
     *
     * @param errorShapeBuilder the error builder
     * @return this deserializer
     */
    public ResponseDeserializer errorShapeBuilder(ShapeBuilder<?> errorShapeBuilder) {
        this.errorShapeBuilder = errorShapeBuilder;
        this.outputShapeBuilder = null;
        return this;
    }

    /**
     * Deserializes the response into the builder. The builder is not built.
     *
     * @throws IllegalStateException if neither builder is set
     * @throws NullPointerException if the response or codec is not set
     * @throws software.amazon.awssdk.schema.serde.SerializationException if a member cannot be read
     */
    public void deserialize() {
        if (outputShapeBuilder == null && errorShapeBuilder == null) {
            throw new IllegalStateException("Either errorShapeBuilder or outputShapeBuilder must be set");
        }
        Validate.paramNotNull(response, "response");
        ShapeBuilder<?> target = outputShapeBuilder != null ? outputShapeBuilder : errorShapeBuilder;
        deserBuilder.headers(response)
                    .responseStatus(response.statusCode())
                    .body(body)
                    .bodyStream(response.content().orElse(null));
        target.deserialize(deserBuilder.build());
    }
}
