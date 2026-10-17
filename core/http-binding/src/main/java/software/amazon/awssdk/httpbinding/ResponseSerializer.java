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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSerializer;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.utils.Validate;

/**
 * Serializes an HTTP response from an output or error shape that uses HTTP binding traits. Used by the
 * Response_Generator, mocks and round-trip tests; clients only read responses.
 *
 * <p>The status code is the {@code @httpResponseCode} member when set, else {@code @httpError}/{@code @error} for an
 * error, else the {@code smithy.api#http} code. Event streams are not supported yet (task 7.20).
 */
@SdkPublicApi
public final class ResponseSerializer {
    private static final String CONTENT_LENGTH = "Content-Length";

    private Codec payloadCodec;
    private String payloadMediaType;
    private ApiOperation<?, ?> operation;
    private SerializableShape shapeValue;
    private boolean error;
    private boolean omitEmptyPayload;
    private HeaderErrorSerializer headerErrorSerializer = HeaderErrorSerializer.AMZN_ERROR_HEADER;

    ResponseSerializer() {
    }

    /**
     * Sets the operation whose output or error is serialized.
     *
     * @param operation the operation
     * @return this serializer
     */
    public ResponseSerializer operation(ApiOperation<?, ?> operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Sets the codec for the body and structured payloads.
     *
     * @param payloadCodec the payload codec
     * @return this serializer
     */
    public ResponseSerializer payloadCodec(Codec payloadCodec) {
        this.payloadCodec = payloadCodec;
        return this;
    }

    /**
     * Sets the media type of codec output, sent as {@code Content-Type}.
     *
     * @param payloadMediaType the media type
     * @return this serializer
     */
    public ResponseSerializer payloadMediaType(String payloadMediaType) {
        this.payloadMediaType = payloadMediaType;
        return this;
    }

    /**
     * Sets the output or error shape to serialize.
     *
     * @param shapeValue the shape
     * @return this serializer
     */
    public ResponseSerializer shapeValue(SerializableShape shapeValue) {
        this.shapeValue = shapeValue;
        return this;
    }

    /**
     * When true, no body is written for a shape without body members or payload member.
     *
     * @param omitEmptyPayload true to omit an empty body
     * @return this serializer
     */
    public ResponseSerializer omitEmptyPayload(boolean omitEmptyPayload) {
        this.omitEmptyPayload = omitEmptyPayload;
        return this;
    }

    /**
     * Sets how the error type is written into the headers of an error response; the default writes
     * {@code X-Amzn-Errortype}.
     *
     * @param headerErrorSerializer the error header serializer
     * @return this serializer
     */
    public ResponseSerializer headerErrorSerializer(HeaderErrorSerializer headerErrorSerializer) {
        this.headerErrorSerializer = Validate.paramNotNull(headerErrorSerializer, "headerErrorSerializer");
        return this;
    }

    /**
     * Marks the shape as a modeled error. smithy-java takes the error schema; the SDK reads it from the shape value.
     *
     * @param error true if the shape is an error
     * @return this serializer
     */
    public ResponseSerializer error(boolean error) {
        this.error = error;
        return this;
    }

    /**
     * Serializes the response.
     *
     * @return the response
     * @throws NullPointerException if the operation, codec, media type or shape value is not set
     */
    public SdkHttpFullResponse serializeResponse() {
        Validate.paramNotNull(shapeValue, "shapeValue");
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(payloadCodec, "payloadCodec");
        Validate.paramNotNull(payloadMediaType, "payloadMediaType");

        HttpBindingSerializer serializer =
            new HttpBindingSerializer(operation.schema(), payloadCodec, payloadMediaType, true)
                .omitEmptyPayload(omitEmptyPayload);
        if (error) {
            serializer.error(headerErrorSerializer);
        }
        shapeValue.serialize(serializer);

        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder()
                                                                 .statusCode(serializer.responseStatus())
                                                                 .headers(serializer.headers());
        ContentStreamProvider body = serializer.body();
        if (body != null) {
            builder.content(AbortableInputStream.create(body.newStream()))
                   .putHeader(CONTENT_LENGTH, Long.toString(serializer.bodyLength()));
        }
        return builder.build();
    }
}
