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

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Client-wide overrides set through a client builder's {@code overrideConfiguration(c -> ...)}, with the V2
 * {@code ClientOverrideConfiguration} builder names {@code putHeader}, {@code headers}, {@code apiCallTimeout},
 * {@code apiCallAttemptTimeout} and {@code putExecutionAttribute} (design, "Client builders").
 *
 * <p>Every accessor reports only what the caller set; resolving these values against other configuration sources is
 * the client builder's job (task 6.2). Header names are case-insensitive (the first-supplied spelling is kept) and
 * hold immutable value lists. Execution attributes are {@link ContextKey.Scope#EXECUTION} context values every call
 * of the client starts with.
 *
 * <p>ADAPTED from V2: execution attributes are keyed by {@link ContextKey} (V2 {@code ExecutionAttribute}). The retry
 * strategy, execution interceptor, metric publisher, profile, scheduled executor, compression and app-id options are
 * added with their owning types (tasks 8.3, 11.x, 16.2 and 6.2); the legacy {@code retryPolicy} is not carried over.
 */
public final class ClientOverrideConfiguration
    implements ToCopyableBuilder<ClientOverrideConfiguration.Builder, ClientOverrideConfiguration> {
    private final Map<String, List<String>> headers;
    private final TimeoutConfiguration timeouts;
    private final Map<ContextKey<?>, Object> executionAttributes;

    private ClientOverrideConfiguration(DefaultBuilder builder) {
        this.headers = builder.headers.isEmpty() ? Map.of() : Collections.unmodifiableMap(copyHeaders(builder.headers));
        this.timeouts = builder.timeouts.build();
        this.executionAttributes = builder.executionAttributes.isEmpty()
                                   ? Map.of()
                                   : Collections.unmodifiableMap(new LinkedHashMap<>(builder.executionAttributes));
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /** Headers added to (or replacing those on) every request of the client, case-insensitive by name. */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** The client-wide whole-call timeout, if set. */
    public Optional<Duration> apiCallTimeout() {
        return timeouts.apiCallTimeout();
    }

    /** The client-wide attempt timeout, if set. */
    public Optional<Duration> apiCallAttemptTimeout() {
        return timeouts.apiCallAttemptTimeout();
    }

    /** Both timeouts. */
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

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClientOverrideConfiguration other && headers.equals(other.headers)
                            && timeouts.equals(other.timeouts)
                            && executionAttributes.equals(other.executionAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, timeouts, executionAttributes);
    }

    /** Renders header names, timeouts and attribute keys; header and attribute values may be sensitive. */
    @Override
    public String toString() {
        return "ClientOverrideConfiguration(headers=" + headers.keySet() + ", " + timeouts
               + ", executionAttributes=" + executionAttributes.keySet() + ")";
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(source);
        return copy;
    }

    /** Builder of a {@link ClientOverrideConfiguration}. */
    public interface Builder extends CopyableBuilder<Builder, ClientOverrideConfiguration> {
        /** Sets header {@code name} to the single value {@code value}, replacing earlier values. */
        Builder putHeader(String name, String value);

        /** Sets header {@code name} to {@code values}, replacing earlier values. */
        Builder putHeader(String name, List<String> values);

        /** Replaces every header with {@code headers}. */
        Builder headers(Map<String, List<String>> headers);

        Map<String, List<String>> headers();

        /**
         * The whole-call timeout; {@code null} clears it.
         *
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        Builder apiCallTimeout(Duration apiCallTimeout);

        Duration apiCallTimeout();

        /**
         * The attempt timeout; {@code null} clears it.
         *
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout);

        Duration apiCallAttemptTimeout();

        /**
         * Sets the execution attribute {@code key} to {@code value}; {@code null} removes it.
         *
         * @throws IllegalArgumentException if {@code key} is not {@link ContextKey.Scope#EXECUTION} scoped
         * @throws ClassCastException if {@code value} is not of the key's type
         */
        <T> Builder putExecutionAttribute(ContextKey<T> key, T value);

        Map<ContextKey<?>, Object> executionAttributes();
    }

    private static final class DefaultBuilder implements Builder {
        private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final TimeoutConfiguration.Builder timeouts;
        private final Map<ContextKey<?>, Object> executionAttributes = new LinkedHashMap<>();

        private DefaultBuilder() {
            this.timeouts = TimeoutConfiguration.builder();
        }

        private DefaultBuilder(ClientOverrideConfiguration configuration) {
            this.headers.putAll(configuration.headers);
            this.timeouts = configuration.timeouts.toBuilder();
            this.executionAttributes.putAll(configuration.executionAttributes);
        }

        @Override
        public Builder putHeader(String name, String value) {
            return putHeader(name, List.of(Objects.requireNonNull(value, "value")));
        }

        @Override
        public Builder putHeader(String name, List<String> values) {
            Objects.requireNonNull(name, "name");
            headers.remove(name);
            headers.put(name, List.copyOf(values));
            return this;
        }

        @Override
        public Builder headers(Map<String, List<String>> headers) {
            this.headers.clear();
            headers.forEach(this::putHeader);
            return this;
        }

        @Override
        public Map<String, List<String>> headers() {
            return Collections.unmodifiableMap(headers);
        }

        @Override
        public Builder apiCallTimeout(Duration apiCallTimeout) {
            timeouts.apiCallTimeout(apiCallTimeout);
            return this;
        }

        @Override
        public Duration apiCallTimeout() {
            return timeouts.build().apiCallTimeout().orElse(null);
        }

        @Override
        public Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            timeouts.apiCallAttemptTimeout(apiCallAttemptTimeout);
            return this;
        }

        @Override
        public Duration apiCallAttemptTimeout() {
            return timeouts.build().apiCallAttemptTimeout().orElse(null);
        }

        @Override
        public <T> Builder putExecutionAttribute(ContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            if (key.scope() != ContextKey.Scope.EXECUTION) {
                throw new IllegalArgumentException("Client overrides accept only execution-scoped keys, not " + key
                                                   + ".");
            }
            if (value == null) {
                executionAttributes.remove(key);
            } else {
                executionAttributes.put(key, key.cast(value));
            }
            return this;
        }

        @Override
        public Map<ContextKey<?>, Object> executionAttributes() {
            return Collections.unmodifiableMap(executionAttributes);
        }

        @Override
        public ClientOverrideConfiguration build() {
            return new ClientOverrideConfiguration(this);
        }
    }
}
