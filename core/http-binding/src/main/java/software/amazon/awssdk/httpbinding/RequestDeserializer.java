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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.httpbinding.internal.HttpBindingDeserializer;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * Deserializes an HTTP request into an input builder. Used by mocks and round-trip tests; clients only write requests.
 *
 * <p>The caller matches the operation and supplies the percent-decoded label values through
 * {@link #pathLabelValues(Map)}. Query parameters come from the request's raw (decoded) parameters; a parameter
 * without a value reads as the empty string.
 */
@SdkPublicApi
public final class RequestDeserializer {
    private final HttpBindingDeserializer.Builder deserBuilder = HttpBindingDeserializer.builder().isResponse(false);
    private ShapeBuilder<?> inputShapeBuilder;
    private SdkHttpFullRequest request;

    RequestDeserializer() {
    }

    /**
     * Sets the codec for the body and structured payloads.
     *
     * @param payloadCodec the payload codec
     * @return this deserializer
     */
    public RequestDeserializer payloadCodec(Codec payloadCodec) {
        deserBuilder.payloadCodec(payloadCodec);
        return this;
    }

    /**
     * Sets the expected media type of a codec body; null skips the check.
     *
     * @param payloadMediaType the media type
     * @return this deserializer
     */
    public RequestDeserializer payloadMediaType(String payloadMediaType) {
        deserBuilder.payloadMediaType(payloadMediaType);
        return this;
    }

    /**
     * Sets the HTTP request to deserialize.
     *
     * @param request the request
     * @return this deserializer
     */
    public RequestDeserializer request(SdkHttpFullRequest request) {
        this.request = request;
        return this;
    }

    /**
     * Sets the input builder to populate.
     *
     * @param inputShapeBuilder the input builder
     * @return this deserializer
     */
    public RequestDeserializer inputShapeBuilder(ShapeBuilder<?> inputShapeBuilder) {
        this.inputShapeBuilder = inputShapeBuilder;
        return this;
    }

    /**
     * Sets the percent-decoded label values of the matched path, by member name.
     *
     * @param labelValues the label values
     * @return this deserializer
     */
    public RequestDeserializer pathLabelValues(Map<String, String> labelValues) {
        deserBuilder.requestPathLabels(labelValues);
        return this;
    }

    /**
     * Deserializes the request into the builder. The builder is not built.
     *
     * @throws IllegalStateException if the input builder is not set
     * @throws NullPointerException if the request or codec is not set
     */
    public void deserialize() {
        if (inputShapeBuilder == null) {
            throw new IllegalStateException("inputShapeBuilder must be set");
        }
        Validate.paramNotNull(request, "request");
        InputStream content = request.contentStreamProvider().map(p -> p.newStream()).orElse(null);
        deserBuilder.headers(request)
                    .queryParameters(queryParameters(request))
                    .bodyStream(content);
        inputShapeBuilder.deserialize(deserBuilder.build());
    }

    private static Map<String, List<String>> queryParameters(SdkHttpFullRequest request) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        request.forEachRawQueryParameter((name, values) -> {
            List<String> copy = new ArrayList<>(values.size());
            for (String value : values) {
                copy.add(value == null ? "" : value);
            }
            result.put(name, copy);
        });
        return result;
    }
}
