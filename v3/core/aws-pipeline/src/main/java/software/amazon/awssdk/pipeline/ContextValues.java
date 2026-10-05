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

import java.util.IdentityHashMap;
import java.util.Map;
import software.amazon.awssdk.core.context.ContextKey;

/**
 * The compact storage of one scope's context values: an array indexed by the plan's {@link ContextLayout}, plus an
 * overflow map allocated only when an undeclared key is written. Reads enforce the key's declared type.
 *
 * <p>Not thread safe: a context is confined to its call's driver, which hands it between threads only through the
 * happens-before edges of its executor and transport callbacks.
 */
abstract class ContextValues {
    private static final Object[] EMPTY = new Object[0];

    private final ContextLayout layout;
    private final ContextKey.Scope scope;
    private final Object[] values;
    private Map<ContextKey<?>, Object> overflow;

    ContextValues(ContextLayout layout, ContextKey.Scope scope) {
        this.layout = layout;
        this.scope = scope;
        int size = layout.size(scope);
        this.values = size == 0 ? EMPTY : new Object[size];
    }

    /** The plan's slot assignment. */
    final ContextLayout layout() {
        return layout;
    }

    final <T> T read(ContextKey<T> key) {
        int index = layout.index(key);
        if (index >= 0) {
            return key.cast(values[index]);
        }
        return overflow == null ? null : key.cast(overflow.get(key));
    }

    final boolean has(ContextKey<?> key) {
        int index = layout.index(key);
        if (index >= 0) {
            return values[index] != null;
        }
        return overflow != null && overflow.containsKey(key);
    }

    final <T> void write(ContextKey<T> key, T value) {
        T typed = key.cast(value);
        int index = layout.index(key);
        if (index >= 0) {
            values[index] = typed;
        } else if (typed != null) {
            if (overflow == null) {
                overflow = new IdentityHashMap<>(4);
            }
            overflow.put(key, typed);
        } else if (overflow != null) {
            overflow.remove(key);
        }
    }

    final <T> T read(ContextSlot<T> slot) {
        requireScope(slot.key());
        return slot.key().cast(values[layout.index(slot)]);
    }

    final <T> void write(ContextSlot<T> slot, T value) {
        requireScope(slot.key());
        values[layout.index(slot)] = slot.key().cast(value);
    }

    /** Direct access to a fixed slot of this scope, for pipeline-produced values. */
    final Object readIndex(int index) {
        return values[index];
    }

    final void writeIndex(int index, Object value) {
        values[index] = value;
    }

    final void requireScope(ContextKey<?> key) {
        if (key.scope() != scope) {
            throw new IllegalArgumentException(key + " is " + key.scope() + "-scoped and cannot be stored in the "
                                               + scope + " context");
        }
    }
}
