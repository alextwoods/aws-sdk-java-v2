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

package software.amazon.awssdk.core.context;

import java.util.Objects;

/**
 * A typed key of one execution- or attempt-scoped context value, the V3 replacement of the V2
 * {@code ExecutionAttribute}.
 *
 * <p>Keys are compared by identity: each key is one constant, and two keys with the same name are distinct. The
 * {@link #scope()} states where a value lives: {@link Scope#EXECUTION} values survive every attempt of one call, and
 * {@link Scope#ATTEMPT} values exist only for one attempt. Reads through {@link #cast(Object)} enforce the declared
 * value type.
 *
 * @param <T> the value type
 */
public final class ContextKey<T> {
    /** Lifetime of values stored under a key. */
    public enum Scope {
        /** One value per call, visible to every attempt of that call. */
        EXECUTION,
        /** One value per attempt, discarded when the attempt ends. */
        ATTEMPT
    }

    private final String name;
    private final Class<T> valueType;
    private final Scope scope;

    private ContextKey(String name, Class<T> valueType, Scope scope) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.scope = Objects.requireNonNull(scope, "scope");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A context key name must not be empty.");
        }
    }

    /** A new {@link Scope#EXECUTION} key named {@code name} whose values are {@code valueType} instances. */
    public static <T> ContextKey<T> execution(String name, Class<T> valueType) {
        return new ContextKey<>(name, valueType, Scope.EXECUTION);
    }

    /** A new {@link Scope#ATTEMPT} key named {@code name} whose values are {@code valueType} instances. */
    public static <T> ContextKey<T> attempt(String name, Class<T> valueType) {
        return new ContextKey<>(name, valueType, Scope.ATTEMPT);
    }

    /** Diagnostic name of the key; not an identity. */
    public String name() {
        return name;
    }

    /** Declared type of the key's values. */
    public Class<T> valueType() {
        return valueType;
    }

    /** Lifetime of the key's values. */
    public Scope scope() {
        return scope;
    }

    /**
     * {@code value} as this key's value type.
     *
     * @throws ClassCastException if {@code value} is not null and not a {@link #valueType()} instance
     */
    public T cast(Object value) {
        return valueType.cast(value);
    }

    @Override
    public String toString() {
        return "ContextKey(" + name + ": " + valueType.getSimpleName() + ", " + scope + ")";
    }
}
