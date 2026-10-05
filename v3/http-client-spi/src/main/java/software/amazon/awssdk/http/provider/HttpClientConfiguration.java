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

package software.amazon.awssdk.http.provider;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The HTTP options a caller configured, each with its value, passed to the implementation that constructs the client.
 * Only configured options are present: an absent option means the implementation's default. Selection checks
 * {@link #configuredOptions()} against the implementation's capabilities before construction (Requirement 12.13).
 *
 * <p>Values are opaque to selection; each option's implementation defines the value types it accepts (for example a
 * {@link java.time.Duration} for a timeout) and rejects others with a configuration failure. Instances are immutable
 * and iterate options in declaration order.
 */
public final class HttpClientConfiguration
    implements ToCopyableBuilder<HttpClientConfiguration.Builder, HttpClientConfiguration> {
    private static final HttpClientConfiguration EMPTY = builder().build();

    private final Map<HttpClientOption, Object> values;

    private HttpClientConfiguration(Builder builder) {
        this.values = builder.values.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(builder.values));
    }

    /** No configured option. */
    public static HttpClientConfiguration empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The configured options, in declaration order. */
    public Set<HttpClientOption> configuredOptions() {
        return values.keySet();
    }

    /** Whether {@code option} is configured. */
    public boolean isConfigured(HttpClientOption option) {
        return values.containsKey(option);
    }

    /** The value of {@code option}, if configured. */
    public Optional<Object> value(HttpClientOption option) {
        return Optional.ofNullable(values.get(Objects.requireNonNull(option, "option")));
    }

    /**
     * The value of {@code option} as a {@code type}, if configured.
     *
     * @throws IllegalArgumentException if the configured value is not a {@code type}
     */
    public <T> Optional<T> value(HttpClientOption option, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return value(option).map(v -> {
            if (!type.isInstance(v)) {
                throw new IllegalArgumentException(option.configurationName() + " is a " + v.getClass().getName()
                                                   + ", not a " + type.getName());
            }
            return type.cast(v);
        });
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.values.putAll(values);
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HttpClientConfiguration other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /** Names the configured options only; values may be secrets (proxy credentials, key managers). */
    @Override
    public String toString() {
        return "HttpClientConfiguration" + configuredOptions();
    }

    /** Builder of an {@link HttpClientConfiguration}. */
    public static final class Builder implements CopyableBuilder<Builder, HttpClientConfiguration> {
        private final Map<HttpClientOption, Object> values =
            new EnumMap<>(HttpClientOption.class);

        private Builder() {
        }

        /** Configures {@code option} with {@code value}, replacing an earlier value. */
        public Builder put(HttpClientOption option, Object value) {
            values.put(Objects.requireNonNull(option, "option"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Removes {@code option}, restoring the implementation's default. */
        public Builder remove(HttpClientOption option) {
            values.remove(Objects.requireNonNull(option, "option"));
            return this;
        }

        @Override
        public HttpClientConfiguration build() {
            return new HttpClientConfiguration(this);
        }
    }
}
