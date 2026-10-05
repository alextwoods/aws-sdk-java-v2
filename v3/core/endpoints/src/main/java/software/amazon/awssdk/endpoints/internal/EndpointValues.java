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

package software.amazon.awssdk.endpoints.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The value types of the rules language as Java objects: {@link String}, {@link Boolean}, {@link Integer},
 * {@code List<Object>} (arrays and tuples) and {@code Map<String, Object>} (records). The rules language's empty value
 * is Java {@code null}.
 */
public final class EndpointValues {
    private EndpointValues() {
    }

    /**
     * {@code value} if it is a rule-set value (recursively), for use as an endpoint property.
     *
     * @throws IllegalArgumentException if it, or anything it contains, is not
     */
    public static Object requireValue(Object value) {
        if (value instanceof String || value instanceof Boolean || value instanceof Integer) {
            return value;
        }
        if (value instanceof List<?> list) {
            list.forEach(EndpointValues::requireValue);
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (!(k instanceof String)) {
                    throw new IllegalArgumentException("Endpoint record keys must be strings: " + k);
                }
                requireValue(v);
            });
            return value;
        }
        throw new IllegalArgumentException("Not an endpoint rules value: " + value
                                           + (value == null ? "" : " (" + value.getClass().getName() + ")"));
    }

    /** An immutable deep copy of rule-set value {@code value}. */
    public static Object immutable(Object value) {
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(v -> copy.add(immutable(v)));
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put((String) k, immutable(v)));
            return Collections.unmodifiableMap(copy);
        }
        return requireValue(value);
    }

    /** A JSON-like rendering of a rules value, for diagnostics. */
    public static String render(Object value) {
        if (value == null) {
            return "<empty>";
        }
        if (value instanceof String s) {
            return '"' + s + '"';
        }
        return String.valueOf(value);
    }
}
