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

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.httpbinding.HttpBinding;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * Populates a modeled error through its HTTP binding traits: {@code @httpHeader}, {@code @httpPrefixHeaders} and
 * {@code @httpResponseCode} members from the response, the {@code @httpPayload} member or the unbound members from the
 * body. REST protocols use it in place of {@link HttpErrorDeserializer#DEFAULT_KNOWN_ERROR_FACTORY}, which reads only
 * the body.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.binding.HttpBindingErrorFactory}. As every
 * {@link HttpErrorDeserializer.KnownErrorFactory}, it only populates the builder; {@link HttpErrorDeserializer} sets the
 * message, status, request IDs and error details and builds the exception.
 */
@SdkPublicApi
@ThreadSafe
public final class HttpBindingErrorFactory implements HttpErrorDeserializer.KnownErrorFactory {
    private final HttpBinding httpBinding;

    /**
     * Creates a factory with its own {@link HttpBinding}.
     */
    public HttpBindingErrorFactory() {
        this(HttpBinding.create());
    }

    /**
     * Creates a factory.
     *
     * @param httpBinding the HTTP binding to deserialize with
     * @throws NullPointerException if {@code httpBinding} is null
     */
    public HttpBindingErrorFactory(HttpBinding httpBinding) {
        this.httpBinding = Validate.paramNotNull(httpBinding, "httpBinding");
    }

    @Override
    public void populateError(ExecutionAttributes context, Codec codec, SdkHttpFullResponse response, ByteBuffer body,
                              ShapeBuilder<?> builder) {
        httpBinding.responseDeserializer()
                   .payloadCodec(codec)
                   .errorShapeBuilder(builder)
                   .response(response)
                   .body(body)
                   .deserialize();
    }

    @Override
    public String toString() {
        return "HttpBindingErrorFactory";
    }
}
