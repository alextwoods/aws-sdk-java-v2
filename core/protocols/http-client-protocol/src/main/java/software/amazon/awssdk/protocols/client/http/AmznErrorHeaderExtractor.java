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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Reads the Amazon-specific {@code X-Amzn-Errortype} error header, and the {@code X-Amzn-Error-Message} header that
 * services without an error body (for example in reply to a {@code HEAD} request) send the message in.
 *
 * <p>The awsJson1_0, awsJson1_1 and restJson1 protocols check this header before the error body.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.AmznErrorHeaderExtractor}. Resolving the header
 * value to a shape ID is done by {@link ErrorTypeUtils#resolve}, so the extractor only reads headers; the SDK adds
 * {@link #errorMessage(SdkHttpFullResponse)}.
 */
@SdkPublicApi
@ThreadSafe
public final class AmznErrorHeaderExtractor implements HttpErrorDeserializer.HeaderErrorExtractor {
    /**
     * The shared instance.
     */
    public static final AmznErrorHeaderExtractor INSTANCE = new AmznErrorHeaderExtractor();

    private static final String ERROR_TYPE_HEADER = "x-amzn-errortype";
    private static final String ERROR_MESSAGE_HEADER = "x-amzn-error-message";

    /**
     * Creates an extractor. {@link #INSTANCE} can be used instead.
     */
    public AmznErrorHeaderExtractor() {
    }

    /**
     * Returns the value of the {@code X-Amzn-Errortype} header, matched case-insensitively.
     *
     * @param response the error response
     * @return the raw header value, for example {@code FooError:http://internal.amazon.com/coral/}, or null if the
     *     header is absent
     */
    @Override
    public String errorType(SdkHttpFullResponse response) {
        return response.firstMatchingHeader(ERROR_TYPE_HEADER).orElse(null);
    }

    /**
     * Returns the value of the {@code X-Amzn-Error-Message} header, matched case-insensitively.
     *
     * @param response the error response
     * @return the header value, or null if the header is absent
     */
    @Override
    public String errorMessage(SdkHttpFullResponse response) {
        return response.firstMatchingHeader(ERROR_MESSAGE_HEADER).orElse(null);
    }

    @Override
    public String toString() {
        return "AmznErrorHeaderExtractor";
    }
}
