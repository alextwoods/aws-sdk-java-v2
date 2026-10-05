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

package software.amazon.awssdk.protocols.core.error;

import java.util.Optional;
import software.amazon.awssdk.http.HttpHeaders;

/**
 * Error-code extraction rules shared by the protocol runtimes.
 */
public final class ErrorCodes {
    /** The header carrying the error type in {@code awsJson1_*} and {@code restJson1} responses. */
    public static final String ERROR_TYPE_HEADER = "X-Amzn-Errortype";

    /** The {@code awsQueryCompatible} response header, {@code <awsQueryError code>;<Sender|Receiver>}. */
    public static final String QUERY_ERROR_HEADER = "x-amzn-query-error";

    /** The {@code awsQueryCompatible} request header announcing that the client understands query error codes. */
    public static final String QUERY_MODE_HEADER = "x-amzn-query-mode";

    private static final String[] REQUEST_ID_HEADERS = {"x-amzn-RequestId", "x-amz-request-id", "x-amzn-request-id"};
    private static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";

    private ErrorCodes() {
    }

    /**
     * The error shape name in a wire error type, which may carry a namespace ({@code aws.example#FooError}) and a
     * {@code :}-separated suffix ({@code FooError:http://internal.amazon.com/...}); both are removed, as are
     * surrounding spaces. Returns {@code null} for a {@code null} or blank value.
     */
    public static String sanitizeErrorType(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw;
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        int hash = value.lastIndexOf('#');
        if (hash >= 0) {
            value = value.substring(hash + 1);
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * The parsed {@value #QUERY_ERROR_HEADER} header, if present and well formed.
     */
    public static Optional<QueryError> queryError(HttpHeaders headers) {
        return headers.firstValue(QUERY_ERROR_HEADER).flatMap(ErrorCodes::parseQueryError);
    }

    /** Parses {@code <code>;<fault>}; the fault part is optional. */
    public static Optional<QueryError> parseQueryError(String value) {
        int semicolon = value.indexOf(';');
        String code = (semicolon < 0 ? value : value.substring(0, semicolon)).trim();
        if (code.isEmpty()) {
            return Optional.empty();
        }
        String fault = semicolon < 0 ? null : value.substring(semicolon + 1).trim();
        return Optional.of(new QueryError(code, fault == null || fault.isEmpty() ? null : fault));
    }

    /** The AWS request ID of a response, from the first request-ID header present. */
    public static Optional<String> requestId(HttpHeaders headers) {
        for (String name : REQUEST_ID_HEADERS) {
            Optional<String> value = headers.firstValue(name);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    /** The extended request ID ({@code x-amz-id-2}) of a response. */
    public static Optional<String> extendedRequestId(HttpHeaders headers) {
        return headers.firstValue(EXTENDED_REQUEST_ID_HEADER);
    }

    /**
     * An {@code awsQueryCompatible} error code.
     *
     * @param code the awsQuery error code
     * @param fault {@code Sender} or {@code Receiver}, or {@code null} if absent
     */
    public record QueryError(String code, String fault) {
    }
}
