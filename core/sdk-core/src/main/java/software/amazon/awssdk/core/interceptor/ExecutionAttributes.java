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

package software.amazon.awssdk.core.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A mutable collection of {@link ExecutionAttribute}s that can be modified by {@link ExecutionInterceptor}s in order to save and
 * retrieve information specific to the current execution.
 *
 * This is useful for sharing data between {@link ExecutionInterceptor} method calls specific to a particular execution.
 */
@SdkPublicApi
@NotThreadSafe
public class ExecutionAttributes implements ToCopyableBuilder<ExecutionAttributes.Builder, ExecutionAttributes> {
    /**
     * Attribute values, indexed by {@link ExecutionAttribute} id. Every {@code ExecutionAttribute} is a static
     * final constant with a dense id, so a flat array replaces the per-execution hash map: reads and writes are
     * a bounds check and an array access, and construction is a single allocation.
     */
    private Object[] values;

    public ExecutionAttributes() {
        this.values = new Object[ExecutionAttribute.idCapacity()];
    }

    protected ExecutionAttributes(Map<? extends ExecutionAttribute<?>, ?> attributes) {
        this();
        attributes.forEach((key, value) -> rawSet(key.id(), value));
    }

    ExecutionAttributes(ExecutionAttributes source) {
        this.values = source.values.clone();
    }

    /**
     * Retrieve the current value of the provided attribute in this collection of attributes. This will return null if the value
     * is not set.
     */
    public <U> U getAttribute(ExecutionAttribute<U> attribute) {
        return attribute.storage().get(this);
    }

    /**
     * Retrieve the collection of attributes.
     */
    public Map<ExecutionAttribute<?>, Object> getAttributes() {
        Map<ExecutionAttribute<?>, Object> result = new IdentityHashMap<>(values.length);
        Object[] currentValues = values;
        for (int i = 0; i < currentValues.length; i++) {
            if (currentValues[i] != null) {
                result.put(ExecutionAttribute.forId(i), currentValues[i]);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Retrieve the Optional current value of the provided attribute in this collection of attributes.
     * This will return Optional Value.
     */
    public <U> Optional<U> getOptionalAttribute(ExecutionAttribute<U> attribute) {
        return Optional.ofNullable(getAttribute(attribute));
    }

    /**
     * Update or set the provided attribute in this collection of attributes.
     */
    public <U> ExecutionAttributes putAttribute(ExecutionAttribute<U> attribute, U value) {
        attribute.storage().set(this, value);
        return this;
    }

    /**
     * Set the provided attribute in this collection of attributes if it does not already exist in the collection.
     */
    public <U> ExecutionAttributes putAttributeIfAbsent(ExecutionAttribute<U> attribute, U value) {
        attribute.storage().setIfAbsent(this, value);
        return this;
    }

    /**
     * Merge attributes of a higher precedence into the current lower precedence collection.
     */
    public ExecutionAttributes merge(ExecutionAttributes lowerPrecedenceExecutionAttributes) {
        ExecutionAttributes result = new ExecutionAttributes(this);
        result.putAbsentAttributes(lowerPrecedenceExecutionAttributes);
        return result;
    }

    /**
     * Add the provided attributes to this attribute, if the provided attribute does not exist.
     */
    public void putAbsentAttributes(ExecutionAttributes lowerPrecedenceExecutionAttributes) {
        if (lowerPrecedenceExecutionAttributes != null) {
            Object[] lower = lowerPrecedenceExecutionAttributes.values;
            for (int i = 0; i < lower.length; i++) {
                if (lower[i] != null && rawGet(i) == null) {
                    rawSet(i, lower[i]);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Raw id-indexed storage, used by ExecutionAttribute.ValueStorage implementations.
    // ------------------------------------------------------------------

    Object rawGet(int id) {
        Object[] currentValues = values;
        return id < currentValues.length ? currentValues[id] : null;
    }

    void rawSet(int id, Object value) {
        ensureCapacity(id);
        values[id] = value;
    }

    void rawSetIfAbsent(int id, Object value) {
        if (rawGet(id) == null) {
            rawSet(id, value);
        }
    }

    void rawCompute(int id, UnaryOperator<Object> update) {
        rawSet(id, update.apply(rawGet(id)));
    }

    private void ensureCapacity(int id) {
        if (id >= values.length) {
            // An attribute registered after this instance was created; grow to cover all current ids.
            values = Arrays.copyOf(values, Math.max(ExecutionAttribute.idCapacity(), id + 1));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new ExecutionAttributes.Builder(this);
    }

    public ExecutionAttributes copy() {
        return toBuilder().build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(o instanceof ExecutionAttributes)) {
            return false;
        }

        ExecutionAttributes that = (ExecutionAttributes) o;
        Object[] a = this.values;
        Object[] b = that.values;
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++) {
            Object left = i < a.length ? a[i] : null;
            Object right = i < b.length ? b[i] : null;
            if (left == null ? right != null : !left.equals(right)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        Object[] currentValues = values;
        for (int i = 0; i < currentValues.length; i++) {
            if (currentValues[i] != null) {
                // Mirrors Map.Entry hash contract: key hash ^ value hash, summed.
                result += ExecutionAttribute.forId(i).hashCode() ^ currentValues[i].hashCode();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        List<ExecutionAttribute<?>> keys = new ArrayList<>();
        Object[] currentValues = values;
        for (int i = 0; i < currentValues.length; i++) {
            if (currentValues[i] != null) {
                keys.add(ExecutionAttribute.forId(i));
            }
        }
        return ToString.builder("ExecutionAttributes")
                       .add("attributes", keys)
                       .build();
    }

    public static ExecutionAttributes unmodifiableExecutionAttributes(ExecutionAttributes attributes) {
        return new UnmodifiableExecutionAttributes(attributes);
    }

    private static class UnmodifiableExecutionAttributes extends ExecutionAttributes {
        UnmodifiableExecutionAttributes(ExecutionAttributes executionAttributes) {
            super(executionAttributes);
        }

        @Override
        public <U> ExecutionAttributes putAttribute(ExecutionAttribute<U> attribute, U value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U> ExecutionAttributes putAttributeIfAbsent(ExecutionAttribute<U> attribute, U value) {
            throw new UnsupportedOperationException();
        }
    }

    /*
     * TODO: We should deprecate this builder - execution attributes are mutable - why do we need a builder? We can just use
     * copy() if it's because of {@link #unmodifiableExecutionAttributes(ExecutionAttributes)}.
     */
    public static final class Builder implements CopyableBuilder<ExecutionAttributes.Builder, ExecutionAttributes> {
        private final ExecutionAttributes executionAttributes;

        private Builder() {
            this.executionAttributes = new ExecutionAttributes();
        }

        private Builder(ExecutionAttributes source) {
            this.executionAttributes = new ExecutionAttributes(source);
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> ExecutionAttributes.Builder put(ExecutionAttribute<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            key.storage().set(executionAttributes, value);
            return this;
        }

        /**
         * Adds all the attributes from the map provided.
         */
        public ExecutionAttributes.Builder putAll(Map<? extends ExecutionAttribute<?>, ?> attributes) {
            attributes.forEach(this::unsafePut);
            return this;
        }

        /**
         * There is no way to make this safe without runtime checks, which we can't do because we don't have the class of T.
         * This will just throw an exception at runtime if the types don't match up.
         */
        @SuppressWarnings("unchecked")
        private <T> void unsafePut(ExecutionAttribute<T> key, Object value) {
            key.storage().set(executionAttributes, (T) value);
        }

        @Override
        public ExecutionAttributes build() {
            return new ExecutionAttributes(executionAttributes);
        }
    }
}
