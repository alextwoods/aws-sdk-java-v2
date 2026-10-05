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

import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.core.context.ContextKey;

/**
 * The execution-scoped typed context of one call (Requirement 11.6): values stored under
 * {@link ContextKey.Scope#EXECUTION} keys, visible to every later stage and hook of the call across all of its
 * attempts. One instance is created per call from the call's request-override execution attributes and is never
 * shared with another call.
 *
 * <p>Storage is one array indexed by the plan's {@link ContextLayout}; undeclared keys go to an overflow map that is
 * only allocated when used. Reads enforce the key's declared type. Attempt-scoped keys are rejected: they live in the
 * {@link AttemptContext} of one attempt. Not thread safe; confined to the call's driver.
 */
public final class ExecutionContext extends ContextValues {
    ExecutionContext(ContextLayout layout, Map<ContextKey<?>, Object> initialValues) {
        super(layout, ContextKey.Scope.EXECUTION);
        initialValues.forEach(this::putInitial);
    }

    /**
     * The value stored under {@code key}, or {@code null}.
     *
     * @throws IllegalArgumentException if {@code key} is attempt scoped
     */
    public <T> T get(ContextKey<T> key) {
        requireScope(Objects.requireNonNull(key, "key"));
        return read(key);
    }

    /**
     * Whether a value is stored under {@code key}.
     *
     * @throws IllegalArgumentException if {@code key} is attempt scoped
     */
    public boolean contains(ContextKey<?> key) {
        requireScope(Objects.requireNonNull(key, "key"));
        return has(key);
    }

    /**
     * Stores {@code value} under {@code key}; {@code null} removes it. Later stages of this call, in this and every
     * later attempt, observe the value.
     *
     * @throws IllegalArgumentException if {@code key} is attempt scoped
     * @throws ClassCastException if {@code value} is not of the key's value type
     */
    public <T> void put(ContextKey<T> key, T value) {
        requireScope(Objects.requireNonNull(key, "key"));
        write(key, value);
    }

    /**
     * The value in {@code slot}, or {@code null}.
     *
     * @throws IllegalArgumentException if the slot belongs to another plan or is attempt scoped
     */
    public <T> T get(ContextSlot<T> slot) {
        return read(Objects.requireNonNull(slot, "slot"));
    }

    /**
     * Stores {@code value} in {@code slot}; {@code null} clears it.
     *
     * @throws IllegalArgumentException if the slot belongs to another plan or is attempt scoped
     * @throws ClassCastException if {@code value} is not of the key's value type
     */
    public <T> void put(ContextSlot<T> slot, T value) {
        write(Objects.requireNonNull(slot, "slot"), value);
    }

    // Captures the wildcard so the value is checked against, and stored as, the key's own type.
    private <T> void putInitial(ContextKey<T> key, Object value) {
        requireScope(key);
        write(key, key.cast(value));
    }

    @Override
    public String toString() {
        return "ExecutionContext(" + layout().executionKeys().size() + " declared slots)";
    }
}
