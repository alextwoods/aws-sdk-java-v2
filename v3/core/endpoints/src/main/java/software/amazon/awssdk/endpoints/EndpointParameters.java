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

package software.amazon.awssdk.endpoints;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The values bound to the parameters of an endpoint rule set for one resolution, by parameter name. A parameter that
 * is absent is unset (the rules' empty value), so its rule-set default, if any, applies.
 *
 * <p>Values have the rule-set parameter types: {@link String} ({@code string}), {@link Boolean} ({@code boolean}) and
 * {@code List<String>} ({@code stringArray}). Instances are immutable and compare by value.
 */
public final class EndpointParameters {
    private static final EndpointParameters EMPTY = new EndpointParameters(Map.of());

    private final Map<String, Object> values;

    private EndpointParameters(Map<String, Object> values) {
        this.values = values;
    }

    /** No parameter set. */
    public static EndpointParameters empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.values.putAll(values);
        return builder;
    }

    /** The value of parameter {@code name}, if it is set. */
    public Optional<Object> get(String name) {
        return Optional.ofNullable(values.get(name));
    }

    /** The value of parameter {@code name}, or {@code null} if it is unset. */
    public Object value(String name) {
        return values.get(name);
    }

    /** Every set parameter value, by name, in the order they were set. Immutable. */
    public Map<String, Object> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EndpointParameters that && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return "EndpointParameters" + values;
    }

    /**
     * {@code value} if it has a rule-set parameter type.
     *
     * @throws IllegalArgumentException otherwise
     */
    static Object requireParameterValue(String name, Object value) {
        if (value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof List<?> list) {
            for (Object element : list) {
                if (!(element instanceof String)) {
                    throw new IllegalArgumentException("Endpoint parameter " + name + " is a list, so every element "
                                                       + "must be a String: " + list);
                }
            }
            return List.copyOf(list);
        }
        throw new IllegalArgumentException("Endpoint parameter " + name + " must be a String, Boolean or "
                                           + "List<String>, not " + value.getClass().getName());
    }

    /** Builder of {@link EndpointParameters}. Not thread-safe. */
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Sets parameter {@code name} to {@code value}, replacing any earlier value; a {@code null} value unsets it.
         *
         * @throws IllegalArgumentException if {@code value} is not a String, Boolean or List of Strings
         */
        public Builder put(String name, Object value) {
            Objects.requireNonNull(name, "name");
            if (value == null) {
                values.remove(name);
            } else {
                values.put(name, requireParameterValue(name, value));
            }
            return this;
        }

        /** Sets {@code name} only if it is not set yet and {@code value} is not {@code null}. */
        public Builder putIfAbsent(String name, Object value) {
            if (value != null && !values.containsKey(name)) {
                put(name, value);
            }
            return this;
        }

        /** Whether parameter {@code name} is set. */
        public boolean isSet(String name) {
            return values.containsKey(name);
        }

        public EndpointParameters build() {
            return values.isEmpty() ? EMPTY
                                    : new EndpointParameters(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
    }
}
