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

package software.amazon.awssdk.core.client.protocol;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.ToString;

/**
 * The HTTP message exchange: requests are {@link SdkHttpFullRequest}s and responses are {@link SdkHttpFullResponse}s.
 *
 * <p>Every HTTP {@link ClientProtocol} shipped with the SDK and the transports that wrap an {@code SdkHttpClient} or an
 * {@code SdkAsyncHttpClient} use {@link #INSTANCE}. A third-party protocol or transport that exchanges the same SDK HTTP
 * types should return it too, so that it can be combined with the SDK's own components.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.HttpMessageExchange}, moved to {@code sdk-core}
 * and with {@link #requestType()} and {@link #responseType()} added so that a protocol and transport mismatch can name
 * the types.
 */
@SdkPublicApi
public final class HttpMessageExchange implements MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> {
    /**
     * The only instance.
     */
    public static final HttpMessageExchange INSTANCE = new HttpMessageExchange();

    private HttpMessageExchange() {
    }

    /**
     * Returns the request message type.
     *
     * @return {@code SdkHttpFullRequest.class}
     */
    public Class<SdkHttpFullRequest> requestType() {
        return SdkHttpFullRequest.class;
    }

    /**
     * Returns the response message type.
     *
     * @return {@code SdkHttpFullResponse.class}
     */
    public Class<SdkHttpFullResponse> responseType() {
        return SdkHttpFullResponse.class;
    }

    /**
     * Names the request and response types.
     *
     * @return {@code HttpMessageExchange(request=software.amazon.awssdk.http.SdkHttpFullRequest,
     *     response=software.amazon.awssdk.http.SdkHttpFullResponse)}
     */
    @Override
    public String toString() {
        return ToString.builder("HttpMessageExchange")
                       .add("request", requestType().getName())
                       .add("response", responseType().getName())
                       .build();
    }
}
