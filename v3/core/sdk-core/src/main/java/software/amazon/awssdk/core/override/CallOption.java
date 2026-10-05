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

package software.amazon.awssdk.core.override;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import software.amazon.awssdk.core.client.config.ClientOption;

/**
 * A client option that one call may override, and how the call's value combines with the client's (Requirements 9.4,
 * 9.11, 9.12).
 *
 * <p>A call option is the key of one sparse per-call delta. It names the {@link ClientOption} it overrides and a merge
 * rule; {@link CallOptions} applies the rule to the client value and the delta when a call starts, without copying the
 * rest of the client configuration. A client option with no call option is client scoped: no request override can
 * change it.
 *
 * <p>The module that owns an option's value type declares its call option next to its client option, for example a
 * retry strategy {@link #replacing(ClientOption)} or an interceptor list {@link #appending(ClientOption, Class)}. Keys
 * are compared by identity. Merge functions must be pure and must not mutate their arguments.
 *
 * @param <T> the value type, the same as the overridden client option's
 */
public final class CallOption<T> {
    private final ClientOption<T> clientOption;
    private final BinaryOperator<T> merge;
    private final Class<?> elementType;

    private CallOption(ClientOption<T> clientOption, BinaryOperator<T> merge, Class<?> elementType) {
        this.clientOption = Objects.requireNonNull(clientOption, "clientOption");
        this.merge = Objects.requireNonNull(merge, "merge");
        this.elementType = elementType;
    }

    /** A call option whose per-call value replaces the client's {@code clientOption} value. */
    public static <T> CallOption<T> replacing(ClientOption<T> clientOption) {
        return new CallOption<>(clientOption, (clientValue, callValue) -> callValue, null);
    }

    /**
     * A call option whose per-call elements are added after the client's {@code clientOption} elements, as V2
     * {@code addExecutionInterceptor} does. Lists are immutable and elements must be {@code elementType} instances.
     */
    public static <E> CallOption<List<E>> appending(ClientOption<List<E>> clientOption, Class<E> elementType) {
        Objects.requireNonNull(elementType, "elementType");
        return new CallOption<>(clientOption, CallOption::concat, elementType);
    }

    /**
     * A call option combined with the client's value by {@code merge}. {@code merge} receives the client's value (or
     * {@code null} when the client has none) and the non-null per-call value, and returns the value the call uses.
     */
    public static <T> CallOption<T> merging(ClientOption<T> clientOption, BinaryOperator<T> merge) {
        return new CallOption<>(clientOption, merge, null);
    }

    /** The client option this call option overrides. */
    public ClientOption<T> clientOption() {
        return clientOption;
    }

    /** Diagnostic name, the same as the client option's. */
    public String name() {
        return clientOption.name();
    }

    /** Whether per-call values are added to the client's rather than replacing or merging with it. */
    public boolean isAppending() {
        return elementType != null;
    }

    /**
     * The value a call uses given the current value ({@code null} when unset) and the non-null per-call delta.
     *
     * @throws ClassCastException if the merge produced a value that is not of the option's type
     */
    public T merge(T current, T delta) {
        Objects.requireNonNull(delta, "delta");
        return current == null && !isAppending() ? delta : validate(merge.apply(current, delta));
    }

    /**
     * {@code value} checked against the option's type (and element type for appending options), with lists copied to
     * immutable lists so no caller-held reference can change a built delta.
     *
     * @throws ClassCastException if {@code value} or one of its elements has the wrong type
     */
    public T validate(Object value) {
        Objects.requireNonNull(value, "value");
        T typed = clientOption.cast(value);
        if (!isAppending()) {
            return typed;
        }
        List<?> list = (List<?>) typed;
        for (Object element : list) {
            if (!elementType.isInstance(element)) {
                throw new ClassCastException(this + " requires " + elementType.getName() + " elements, but was given "
                                             + (element == null ? "null" : "a " + element.getClass().getName()) + ".");
            }
        }
        return clientOption.cast(List.copyOf(list));
    }

    private static <E> List<E> concat(List<E> current, List<E> delta) {
        if (current == null || current.isEmpty()) {
            return delta;
        }
        if (delta.isEmpty()) {
            return current;
        }
        List<E> merged = new ArrayList<>(current.size() + delta.size());
        merged.addAll(current);
        merged.addAll(delta);
        return List.copyOf(merged);
    }

    @Override
    public String toString() {
        return "CallOption(" + clientOption.name() + ")";
    }
}
