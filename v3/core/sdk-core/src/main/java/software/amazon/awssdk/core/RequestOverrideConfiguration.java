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

package software.amazon.awssdk.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.override.CallOption;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.core.override.SdkCallOption;

/**
 * Sparse per-call configuration that applies to one request only (Requirements 9.4, 9.12), with the V2 names
 * {@code putHeader}, {@code putRawQueryParameter}, {@code apiCallTimeout}, {@code apiCallAttemptTimeout} and
 * {@code putExecutionAttribute}.
 *
 * <p>An override is an immutable delta: every accessor reports only what the caller set, and an unset value means
 * "use the client's value". Header names are case-insensitive (the first-supplied spelling is kept); query parameter
 * names are case-sensitive; both keep insertion order and hold immutable value lists. Execution attributes are
 * {@link ContextKey.Scope#EXECUTION} context values the call starts with.
 *
 * <p>Every other per-call setting is a sparse {@link CallOption} delta: timeouts are the
 * {@link SdkCallOption#TIMEOUT_CONFIGURATION} entry, and identity, retry, interceptor and telemetry overrides are
 * entries whose keys the owning modules declare. Options without a call option are client scoped and cannot be
 * overridden here. {@link CallOptions} layers an override over the client configuration for one call; this type never
 * copies client configuration, and it is not serializable modeled state.
 *
 * <p>{@link SdkRequestOverrideConfiguration} is the protocol-agnostic concrete type; AWS clients extend this class with
 * AWS options (identity, signing).
 */
public abstract class RequestOverrideConfiguration {
    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> rawQueryParameters;
    private final TimeoutConfiguration timeouts;
    private final Map<ContextKey<?>, Object> executionAttributes;
    private final Map<CallOption<?>, Object> callOptions;

    protected RequestOverrideConfiguration(Builder<?> builder) {
        BuilderImpl<?> impl = (BuilderImpl<?>) builder;
        this.headers = freeze(impl.headers, true);
        this.rawQueryParameters = freeze(impl.rawQueryParameters, false);
        this.timeouts = impl.timeouts.build();
        this.executionAttributes = impl.executionAttributes.isEmpty()
                                   ? Map.of()
                                   : Collections.unmodifiableMap(new LinkedHashMap<>(impl.executionAttributes));
        this.callOptions = impl.callOptionsWithTimeouts(timeouts);
    }

    /** Headers to add to (or replace on) the request, case-insensitive by name. */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** Raw query parameters to add to (or replace on) the request. */
    public Map<String, List<String>> rawQueryParameters() {
        return rawQueryParameters;
    }

    /** The per-call whole-call timeout, if overridden. */
    public Optional<Duration> apiCallTimeout() {
        return timeouts.apiCallTimeout();
    }

    /** The per-call attempt timeout, if overridden. */
    public Optional<Duration> apiCallAttemptTimeout() {
        return timeouts.apiCallAttemptTimeout();
    }

    /** Both overridden timeouts. */
    public TimeoutConfiguration timeouts() {
        return timeouts;
    }

    /** Every execution attribute set, in insertion order. */
    public Map<ContextKey<?>, Object> executionAttributes() {
        return executionAttributes;
    }

    /** The value set for {@code key}, if any. */
    public <T> Optional<T> executionAttribute(ContextKey<T> key) {
        return Optional.ofNullable(key.cast(executionAttributes.get(key)));
    }

    /**
     * Every per-call option delta set, in insertion order, including {@link SdkCallOption#TIMEOUT_CONFIGURATION} when
     * a timeout is overridden. Values are the deltas, not the merged values a call uses.
     */
    public Map<CallOption<?>, Object> callOptions() {
        return callOptions;
    }

    /** The delta set for {@code option}, if any. */
    public <T> Optional<T> callOption(CallOption<T> option) {
        Objects.requireNonNull(option, "option");
        return Optional.ofNullable(option.clientOption().cast(callOptions.get(option)));
    }

    /** Whether this override sets nothing, so a call using it behaves as if it had none. */
    public boolean isEmpty() {
        return headers.isEmpty() && rawQueryParameters.isEmpty() && executionAttributes.isEmpty()
               && callOptions.isEmpty();
    }

    /** A builder initialized with this override's state. */
    public abstract Builder<? extends Builder<?>> toBuilder();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestOverrideConfiguration that = (RequestOverrideConfiguration) o;
        return headers.equals(that.headers)
               && rawQueryParameters.equals(that.rawQueryParameters)
               && executionAttributes.equals(that.executionAttributes)
               && callOptions.equals(that.callOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, rawQueryParameters, executionAttributes, callOptions);
    }

    /** Names of the call options set, for {@code toString}; values may be secret and are never rendered. */
    protected final List<String> callOptionNames() {
        return callOptions.keySet().stream().map(CallOption::name).toList();
    }

    private static Map<String, List<String>> freeze(Map<String, List<String>> source, boolean caseInsensitive) {
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = caseInsensitive ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                                                         : new LinkedHashMap<>();
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Builder of a {@link RequestOverrideConfiguration}.
     *
     * @param <B> the concrete builder type
     */
    public interface Builder<B extends Builder<B>> {
        /** Sets header {@code name} to the single value {@code value}, replacing earlier values. */
        B putHeader(String name, String value);

        /** Sets header {@code name} to {@code values}, replacing earlier values. */
        B putHeader(String name, List<String> values);

        /** Replaces every header with {@code headers}. */
        B headers(Map<String, List<String>> headers);

        Map<String, List<String>> headers();

        /** Sets query parameter {@code name} to the single value {@code value}, replacing earlier values. */
        B putRawQueryParameter(String name, String value);

        /** Sets query parameter {@code name} to {@code values}, replacing earlier values. */
        B putRawQueryParameter(String name, List<String> values);

        /** Replaces every query parameter with {@code rawQueryParameters}. */
        B rawQueryParameters(Map<String, List<String>> rawQueryParameters);

        Map<String, List<String>> rawQueryParameters();

        /**
         * The whole-call timeout for this call; {@code null} clears the override.
         *
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        B apiCallTimeout(Duration apiCallTimeout);

        Duration apiCallTimeout();

        /**
         * The attempt timeout for this call; {@code null} clears the override.
         *
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        B apiCallAttemptTimeout(Duration apiCallAttemptTimeout);

        Duration apiCallAttemptTimeout();

        /**
         * Sets the execution attribute {@code key} to {@code value}; {@code null} removes it.
         *
         * @throws IllegalArgumentException if {@code key} is not {@link ContextKey.Scope#EXECUTION} scoped
         * @throws ClassCastException if {@code value} is not of the key's type
         */
        <T> B putExecutionAttribute(ContextKey<T> key, T value);

        Map<ContextKey<?>, Object> executionAttributes();

        /**
         * Sets the per-call delta of {@code option} to {@code value}, replacing an earlier delta for the same client
         * option; {@code null} removes it. Setting {@link SdkCallOption#TIMEOUT_CONFIGURATION} sets both timeouts.
         *
         * @throws ClassCastException if {@code value} (or, for an appending option, one of its elements) is not of the
         *                            option's type
         */
        <T> B putCallOption(CallOption<T> option, T value);

        /**
         * Adds {@code element} to the per-call delta of the appending {@code option}, as V2
         * {@code addExecutionInterceptor} does.
         *
         * @throws IllegalArgumentException if {@code option} is not {@link CallOption#isAppending() appending}
         * @throws ClassCastException if {@code element} is not of the option's element type
         */
        <E> B addCallOptionElement(CallOption<List<E>> option, E element);

        /** The delta set for {@code option}, or {@code null}. */
        <T> T callOption(CallOption<T> option);

        /** Every delta set, including timeouts, in insertion order. */
        Map<CallOption<?>, Object> callOptions();

        RequestOverrideConfiguration build();
    }

    /**
     * Shared state of every {@link Builder} implementation.
     *
     * @param <B> the concrete builder type
     */
    protected abstract static class BuilderImpl<B extends Builder<B>> implements Builder<B> {
        private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, List<String>> rawQueryParameters = new LinkedHashMap<>();
        private final TimeoutConfiguration.Builder timeouts = TimeoutConfiguration.builder();
        private Duration apiCallTimeout;
        private Duration apiCallAttemptTimeout;
        private final Map<ContextKey<?>, Object> executionAttributes = new LinkedHashMap<>();
        private final Map<CallOption<?>, Object> callOptions = new LinkedHashMap<>();

        protected BuilderImpl() {
        }

        protected BuilderImpl(RequestOverrideConfiguration configuration) {
            headers.putAll(configuration.headers);
            rawQueryParameters.putAll(configuration.rawQueryParameters);
            apiCallTimeout(configuration.apiCallTimeout().orElse(null));
            apiCallAttemptTimeout(configuration.apiCallAttemptTimeout().orElse(null));
            executionAttributes.putAll(configuration.executionAttributes);
            configuration.callOptions.forEach((option, value) -> {
                if (option != SdkCallOption.TIMEOUT_CONFIGURATION) {
                    callOptions.put(option, value);
                }
            });
        }

        private Map<CallOption<?>, Object> callOptionsWithTimeouts(TimeoutConfiguration builtTimeouts) {
            boolean hasTimeouts = !builtTimeouts.equals(TimeoutConfiguration.none());
            if (callOptions.isEmpty() && !hasTimeouts) {
                return Map.of();
            }
            Map<CallOption<?>, Object> copy = new LinkedHashMap<>();
            if (hasTimeouts) {
                copy.put(SdkCallOption.TIMEOUT_CONFIGURATION, builtTimeouts);
            }
            copy.putAll(callOptions);
            return Collections.unmodifiableMap(copy);
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }

        @Override
        public B putHeader(String name, String value) {
            return putHeader(name, List.of(Objects.requireNonNull(value, "value")));
        }

        @Override
        public B putHeader(String name, List<String> values) {
            Objects.requireNonNull(name, "name");
            headers.remove(name);
            headers.put(name, List.copyOf(values));
            return self();
        }

        @Override
        public B headers(Map<String, List<String>> headers) {
            this.headers.clear();
            headers.forEach(this::putHeader);
            return self();
        }

        @Override
        public Map<String, List<String>> headers() {
            return Collections.unmodifiableMap(headers);
        }

        @Override
        public B putRawQueryParameter(String name, String value) {
            return putRawQueryParameter(name, List.of(Objects.requireNonNull(value, "value")));
        }

        @Override
        public B putRawQueryParameter(String name, List<String> values) {
            rawQueryParameters.put(Objects.requireNonNull(name, "name"), List.copyOf(values));
            return self();
        }

        @Override
        public B rawQueryParameters(Map<String, List<String>> rawQueryParameters) {
            this.rawQueryParameters.clear();
            rawQueryParameters.forEach(this::putRawQueryParameter);
            return self();
        }

        @Override
        public Map<String, List<String>> rawQueryParameters() {
            return Collections.unmodifiableMap(rawQueryParameters);
        }

        @Override
        public B apiCallTimeout(Duration apiCallTimeout) {
            timeouts.apiCallTimeout(apiCallTimeout);
            this.apiCallTimeout = apiCallTimeout;
            return self();
        }

        @Override
        public Duration apiCallTimeout() {
            return apiCallTimeout;
        }

        @Override
        public B apiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            timeouts.apiCallAttemptTimeout(apiCallAttemptTimeout);
            this.apiCallAttemptTimeout = apiCallAttemptTimeout;
            return self();
        }

        @Override
        public Duration apiCallAttemptTimeout() {
            return apiCallAttemptTimeout;
        }

        @Override
        public <T> B putExecutionAttribute(ContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            if (key.scope() != ContextKey.Scope.EXECUTION) {
                throw new IllegalArgumentException("Request overrides accept only execution-scoped keys, not " + key
                                                   + ".");
            }
            if (value == null) {
                executionAttributes.remove(key);
            } else {
                executionAttributes.put(key, key.cast(value));
            }
            return self();
        }

        @Override
        public Map<ContextKey<?>, Object> executionAttributes() {
            return Collections.unmodifiableMap(executionAttributes);
        }

        @Override
        public <T> B putCallOption(CallOption<T> option, T value) {
            Objects.requireNonNull(option, "option");
            if (option == SdkCallOption.TIMEOUT_CONFIGURATION) {
                TimeoutConfiguration timeouts = (TimeoutConfiguration) value;
                apiCallTimeout(timeouts == null ? null : timeouts.apiCallTimeout().orElse(null));
                return apiCallAttemptTimeout(timeouts == null ? null : timeouts.apiCallAttemptTimeout().orElse(null));
            }
            if (option.clientOption() == SdkClientOption.TIMEOUT_CONFIGURATION) {
                throw new IllegalArgumentException("Per-call timeouts are set through apiCallTimeout, "
                                                   + "apiCallAttemptTimeout or " + SdkCallOption.TIMEOUT_CONFIGURATION
                                                   + ", not " + option + ".");
            }
            T validated = value == null ? null : option.validate(value);
            callOptions.keySet().removeIf(existing -> existing.clientOption() == option.clientOption());
            if (validated != null) {
                callOptions.put(option, validated);
            }
            return self();
        }

        @Override
        public <E> B addCallOptionElement(CallOption<List<E>> option, E element) {
            Objects.requireNonNull(option, "option");
            Objects.requireNonNull(element, "element");
            if (!option.isAppending()) {
                throw new IllegalArgumentException(option + " replaces the client's value; set it with putCallOption.");
            }
            List<E> current = callOption(option);
            List<E> next = new ArrayList<>(current == null ? List.of() : current);
            next.add(element);
            return putCallOption(option, next);
        }

        @Override
        public <T> T callOption(CallOption<T> option) {
            Objects.requireNonNull(option, "option");
            if (option == SdkCallOption.TIMEOUT_CONFIGURATION) {
                TimeoutConfiguration built = timeouts.build();
                return built.equals(TimeoutConfiguration.none()) ? null : option.clientOption().cast(built);
            }
            return option.clientOption().cast(callOptions.get(option));
        }

        @Override
        public Map<CallOption<?>, Object> callOptions() {
            return callOptionsWithTimeouts(timeouts.build());
        }
    }
}
