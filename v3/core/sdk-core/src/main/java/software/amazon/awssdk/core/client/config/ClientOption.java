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

package software.amazon.awssdk.core.client.config;

import java.util.Objects;

/**
 * A typed key of one resolved client configuration value (Requirement 9.1).
 *
 * <p>Keys are compared by identity: each option is one constant, and two keys with the same name are distinct options.
 * Reads through {@link #cast(Object)} enforce the declared value type, so a value stored under a key is always of the
 * key's type.
 *
 * @param <T> the value type
 */
public final class ClientOption<T> {
    private final String name;
    private final Class<T> valueType;

    private ClientOption(String name, Class<T> valueType) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A client option name must not be empty.");
        }
    }

    /** A new option named {@code name} whose values are {@code valueType} instances. */
    public static <T> ClientOption<T> create(String name, Class<T> valueType) {
        return new ClientOption<>(name, valueType);
    }

    /** Diagnostic name of the option; not an identity. */
    public String name() {
        return name;
    }

    /** Declared type of the option's values. */
    public Class<T> valueType() {
        return valueType;
    }

    /**
     * {@code value} as this option's value type.
     *
     * @throws ClassCastException if {@code value} is not null and not a {@link #valueType()} instance
     */
    public T cast(Object value) {
        return valueType.cast(value);
    }

    @Override
    public String toString() {
        return "ClientOption(" + name + ": " + valueType.getSimpleName() + ")";
    }
}
