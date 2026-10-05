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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;

/** Typed reads of token fields with token-specific errors. Not a public API. */
final class TokenFields {
    private TokenFields() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String field) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw invalid(field);
    }

    static String string(Object value, String field) {
        if (value instanceof String s) {
            return s;
        }
        throw invalid(field);
    }

    /** The long value, or {@code null} if absent. */
    static Long longValue(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal d) {
            try {
                return d.longValueExact();
            } catch (ArithmeticException e) {
                throw invalid(field);
            }
        }
        throw invalid(field);
    }

    /** The instant, or {@code null} if absent. */
    static Instant instant(Object value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(string(value, field));
        } catch (DateTimeParseException e) {
            throw invalid(field);
        }
    }

    static SdkClientException invalid(String field) {
        return SdkClientException.create("Invalid resumable transfer token: missing or malformed field " + field);
    }
}
