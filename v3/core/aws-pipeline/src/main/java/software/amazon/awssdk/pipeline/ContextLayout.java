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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.context.ContextKey;

/**
 * The immutable slot assignment of one {@link PipelinePlan}'s typed contexts (design "Typed contexts"): each declared
 * {@link ContextKey} gets a dense index in the value array of its scope, assigned once when the plan is built.
 *
 * <p>Every layout starts with the {@link PipelineKeys} in fixed attempt slots. Execution and attempt keys index
 * separate arrays, so an {@link ExecutionContext} and each {@link AttemptContext} are one compact array each; a key
 * that was never declared (for example an interceptor's own key) is kept in a per-context overflow map that is only
 * allocated when such a key is written. Immutable and shared by every call of the plan.
 */
public final class ContextLayout {
    private final Object token;
    private final List<ContextKey<?>> executionKeys;
    private final List<ContextKey<?>> attemptKeys;
    private final Map<ContextKey<?>, Integer> indexes;

    private ContextLayout(Builder builder) {
        this.token = builder.token;
        this.executionKeys = List.copyOf(builder.executionKeys);
        this.attemptKeys = List.copyOf(builder.attemptKeys);
        this.indexes = new IdentityHashMap<>(builder.indexes);
    }

    /** A builder whose attempt slots start with the {@link PipelineKeys}. */
    static Builder builder() {
        return new Builder();
    }

    /** The declared execution-scoped keys, in slot order. Immutable. */
    public List<ContextKey<?>> executionKeys() {
        return executionKeys;
    }

    /** The declared attempt-scoped keys, in slot order. Immutable. */
    public List<ContextKey<?>> attemptKeys() {
        return attemptKeys;
    }

    /** The slot of {@code key} in this layout, or empty if it was not declared. */
    public <T> Optional<ContextSlot<T>> slot(ContextKey<T> key) {
        int index = index(Objects.requireNonNull(key, "key"));
        return index < 0 ? Optional.empty() : Optional.of(new ContextSlot<>(key, index, token));
    }

    /** The index of {@code key} in its scope's array, or {@code -1} if it was not declared. */
    int index(ContextKey<?> key) {
        Integer index = indexes.get(key);
        return index == null ? -1 : index;
    }

    /** The index of {@code slot}, checked to belong to this layout. */
    int index(ContextSlot<?> slot) {
        if (slot.layoutToken() != token) {
            throw new IllegalArgumentException(slot + " was assigned by another plan's context layout");
        }
        return slot.index();
    }

    int size(ContextKey.Scope scope) {
        return scope == ContextKey.Scope.EXECUTION ? executionKeys.size() : attemptKeys.size();
    }

    @Override
    public String toString() {
        return "ContextLayout(execution=" + executionKeys + ", attempt=" + attemptKeys + ")";
    }

    /** Assigns slots while a plan is built; not thread safe. */
    static final class Builder {
        private final Object token = new Object();
        private final List<ContextKey<?>> executionKeys = new ArrayList<>();
        private final List<ContextKey<?>> attemptKeys = new ArrayList<>();
        private final Map<ContextKey<?>, Integer> indexes = new IdentityHashMap<>();

        private Builder() {
            PipelineKeys.BUILT_IN.forEach(this::declare);
        }

        /** The slot of {@code key}, assigning the next index of its scope on first declaration. */
        <T> ContextSlot<T> declare(ContextKey<T> key) {
            Objects.requireNonNull(key, "key");
            Integer index = indexes.get(key);
            if (index == null) {
                List<ContextKey<?>> keys = key.scope() == ContextKey.Scope.EXECUTION ? executionKeys : attemptKeys;
                index = keys.size();
                keys.add(key);
                indexes.put(key, index);
            }
            return new ContextSlot<>(key, index, token);
        }

        ContextLayout build() {
            return new ContextLayout(this);
        }
    }
}
