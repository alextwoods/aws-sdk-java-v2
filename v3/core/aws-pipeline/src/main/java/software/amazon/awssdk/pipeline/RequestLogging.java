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

package software.amazon.awssdk.pipeline;

import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Redaction;

/**
 * The per-attempt request log of the pipeline at DEBUG under {@value #LOGGER_NAME}, the V2 request logger name
 * (Requirements 21.8-21.11): the request line, header names and redacted query of every transmitted request, and the
 * status and request IDs of every response. Header values, bodies and query values of sensitive parameters are never
 * rendered. Nothing is logged at INFO, WARN or ERROR (a failed response reaches the caller as an exception), and no
 * message is built while DEBUG is disabled.
 */
final class RequestLogging {
    /** The V2 {@code SdkStandardLogger.REQUEST_LOGGER} name. */
    static final String LOGGER_NAME = "software.amazon.awssdk.request";

    private static final Logger REQUEST_LOGGER = Logger.loggerFor(LOGGER_NAME);
    private static final String[] REQUEST_ID_HEADERS = {"x-amzn-RequestId", "x-amz-request-id", "x-amzn-request-id"};
    private static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";

    private RequestLogging() {
    }

    /** Logs the signed request an attempt is about to transmit. */
    static void sending(CallState<?, ?> call, AttemptState attempt, SdkHttpRequest request) {
        if (!REQUEST_LOGGER.isDebugEnabled()) {
            return;
        }
        REQUEST_LOGGER.debug(() -> "Sending Request: " + request.method() + " " + request.scheme() + "://"
                                   + request.authority() + request.encodedPath() + query(request) + " [operation="
                                   + call.plan().operation().id() + ", attempt=" + attempt.number() + ", headers="
                                   + headerNames(request.headers()) + "]");
    }

    /** Logs the response an attempt received, successful or not. */
    static void received(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        if (!REQUEST_LOGGER.isDebugEnabled()) {
            return;
        }
        REQUEST_LOGGER.debug(() -> {
            int status = response.statusCode();
            HttpHeaders headers = response.headers();
            return (status >= 200 && status < 300 ? "Received successful response: " : "Received failed response: ")
                   + status + ", Request ID: " + requestId(headers) + ", Extended Request ID: "
                   + headers.firstValue(EXTENDED_REQUEST_ID_HEADER).orElse("not available") + " [operation="
                   + call.plan().operation().id() + ", attempt=" + attempt.number() + "]";
        });
    }

    private static String query(SdkHttpRequest request) {
        String encoded = request.encodedQuery();
        return encoded == null || encoded.isEmpty() ? "" : "?" + Redaction.query(encoded);
    }

    private static String requestId(HttpHeaders headers) {
        for (String name : REQUEST_ID_HEADERS) {
            String value = headers.firstValue(name).orElse(null);
            if (value != null) {
                return value;
            }
        }
        return "not available";
    }

    private static String headerNames(HttpHeaders headers) {
        StringBuilder names = new StringBuilder("[");
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(headers.name(i));
        }
        return names.append(']').toString();
    }
}
