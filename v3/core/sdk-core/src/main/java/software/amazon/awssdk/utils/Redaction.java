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

package software.amazon.awssdk.utils;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The renderings runtime modules use for values that may be sensitive in log messages, exception messages, telemetry
 * records and diagnostics (Requirement 21.10): credential values, authorization values, security tokens, cookies,
 * sensitive headers and query parameters. {@code @sensitive} modeled members are already rendered as {@link #REDACTED}
 * by the {@code toString} of generated and dynamic modeled values.
 *
 * <p>A name is sensitive when, ignoring case, it contains one of {@code authorization}, {@code cookie},
 * {@code token}, {@code secret}, {@code password}, {@code credential}, {@code signature}, {@code session},
 * {@code customer-key}, {@code api-key}, {@code apikey} or {@code private}. That covers, among others,
 * {@code Authorization}, {@code Proxy-Authorization}, {@code Cookie}, {@code Set-Cookie},
 * {@code X-Amz-Security-Token}, {@code x-amz-server-side-encryption-customer-key}, {@code x-aws-ec2-metadata-token},
 * {@code x-amz-sso_bearer_token} and the {@code X-Amz-Signature}, {@code X-Amz-Credential} and
 * {@code X-Amz-Security-Token} query parameters of presigned URLs. Over-redaction is intended: a value is rendered
 * only when its name is known not to be sensitive.
 *
 * <p>These methods do the rendering work, so call them inside the message supplier of a {@link Logger} call:
 * nothing is rendered when the level is disabled. Stateless and thread safe.
 */
public final class Redaction {
    /** The rendering of a redacted value, as in V2 {@code toString}. */
    public static final String REDACTED = "*** Sensitive Data Redacted ***";

    private static final String[] SENSITIVE_NAME_PARTS = {
        "authorization", "cookie", "token", "secret", "password", "credential", "signature", "session",
        "customer-key", "api-key", "apikey", "private"
    };

    private Redaction() {
    }

    /** Whether a header, query parameter, property or field named {@code name} may carry a sensitive value. */
    public static boolean isSensitiveName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String part : SENSITIVE_NAME_PARTS) {
            if (lower.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /** {@link #REDACTED} for a non-null value known to be sensitive (a credential, token or sensitive member). */
    public static String secret(Object value) {
        return value == null ? null : REDACTED;
    }

    /** {@code value} rendered, or {@link #REDACTED} when {@code name} {@linkplain #isSensitiveName is sensitive}. */
    public static String value(String name, Object value) {
        if (value == null) {
            return null;
        }
        return isSensitiveName(name) ? REDACTED : String.valueOf(value);
    }

    /**
     * {@code headers} rendered as {@code {name=[value, ...], ...}}, with the values of sensitive headers replaced by
     * {@link #REDACTED}.
     */
    public static String headers(Map<String, List<String>> headers) {
        return render(headers);
    }

    /** Query parameters rendered like {@link #headers}, with the values of sensitive parameters redacted. */
    public static String queryParameters(Map<String, List<String>> parameters) {
        return render(parameters);
    }

    /**
     * {@code uri} with its user information and the values of its sensitive query parameters (for example the
     * signature, credential and security token of a presigned URL) replaced by {@link #REDACTED}.
     */
    public static String uri(URI uri) {
        if (uri == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        if (uri.getScheme() != null) {
            out.append(uri.getScheme()).append(':');
        }
        if (uri.getRawAuthority() != null) {
            out.append("//");
            if (uri.getRawUserInfo() != null) {
                out.append(REDACTED).append('@');
            }
            out.append(uri.getHost() == null ? uri.getRawAuthority() : uri.getHost());
            if (uri.getHost() != null && uri.getPort() >= 0) {
                out.append(':').append(uri.getPort());
            }
        }
        if (uri.getRawPath() != null) {
            out.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            out.append('?').append(query(uri.getRawQuery()));
        }
        return out.toString();
    }

    /** A raw (encoded) query string with the values of its sensitive parameters replaced by {@link #REDACTED}. */
    public static String query(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return rawQuery;
        }
        StringBuilder out = new StringBuilder(rawQuery.length());
        int start = 0;
        while (start <= rawQuery.length()) {
            int end = rawQuery.indexOf('&', start);
            if (end < 0) {
                end = rawQuery.length();
            }
            String pair = rawQuery.substring(start, end);
            int equals = pair.indexOf('=');
            if (start > 0) {
                out.append('&');
            }
            if (equals >= 0 && isSensitiveName(pair.substring(0, equals))) {
                out.append(pair, 0, equals + 1).append(REDACTED);
            } else {
                out.append(pair);
            }
            start = end + 1;
        }
        return out.toString();
    }

    private static String render(Map<String, List<String>> values) {
        if (values == null) {
            return null;
        }
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(entry.getKey()).append('=');
            if (isSensitiveName(entry.getKey())) {
                out.append(REDACTED);
            } else {
                out.append(entry.getValue());
            }
        }
        return out.append('}').toString();
    }
}
