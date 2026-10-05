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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.net.URI;

/**
 * Redaction of the SNS message values that act as credentials in rendered output (Requirement 21.10): a subscription
 * token, and the {@code Token} query parameter of a subscribe URL, each confirm a subscription by themselves.
 *
 * <p>Internal to the SNS Message Manager; not a customer API.
 */
public final class SensitiveValues {
    /** The rendering of a redacted value, as in generated model types. */
    public static final String REDACTED = "*** Sensitive Data Redacted ***";

    private static final String TOKEN_PARAMETER = "Token";

    private SensitiveValues() {
    }

    /** {@link #REDACTED}, or {@code null} for a {@code null} value (which {@code ToString} omits). */
    public static String redact(String value) {
        return value == null ? null : REDACTED;
    }

    /**
     * The string form of {@code uri} with the value of every {@code Token} query parameter (any case) replaced by
     * {@link #REDACTED}; {@code null} for {@code null}. Every other character of the URI is kept.
     */
    public static String redactTokenQuery(URI uri) {
        if (uri == null) {
            return null;
        }
        String text = uri.toString();
        String query = uri.getRawQuery();
        if (query == null) {
            return text;
        }
        int queryStart = text.indexOf('?') + 1;
        int queryEnd = queryStart + query.length();
        StringBuilder result = new StringBuilder(text.length()).append(text, 0, queryStart);
        String[] parameters = query.split("&", -1);
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                result.append('&');
            }
            String parameter = parameters[i];
            int equals = parameter.indexOf('=');
            String name = equals < 0 ? parameter : parameter.substring(0, equals);
            result.append(name.equalsIgnoreCase(TOKEN_PARAMETER) ? name + "=" + REDACTED : parameter);
        }
        return result.append(text, queryEnd, text.length()).toString();
    }
}
