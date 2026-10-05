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
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The API-call and API-call-attempt timeouts, with the V2 names {@code apiCallTimeout} and
 * {@code apiCallAttemptTimeout}. An absent timeout means no SDK-enforced limit at that scope.
 *
 * <p>Immutable; equal when both timeouts are equal.
 */
public final class TimeoutConfiguration implements ToCopyableBuilder<TimeoutConfiguration.Builder,
                                                                     TimeoutConfiguration> {
    private static final TimeoutConfiguration NONE = new TimeoutConfiguration(new Builder());

    private final Duration apiCallTimeout;
    private final Duration apiCallAttemptTimeout;

    private TimeoutConfiguration(Builder builder) {
        this.apiCallTimeout = builder.apiCallTimeout;
        this.apiCallAttemptTimeout = builder.apiCallAttemptTimeout;
    }

    /** The configuration with neither timeout. */
    public static TimeoutConfiguration none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Limit on a whole call, across every attempt and retry delay. */
    public Optional<Duration> apiCallTimeout() {
        return Optional.ofNullable(apiCallTimeout);
    }

    /** Limit on one attempt. */
    public Optional<Duration> apiCallAttemptTimeout() {
        return Optional.ofNullable(apiCallAttemptTimeout);
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TimeoutConfiguration that
               && Objects.equals(apiCallTimeout, that.apiCallTimeout)
               && Objects.equals(apiCallAttemptTimeout, that.apiCallAttemptTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiCallTimeout, apiCallAttemptTimeout);
    }

    @Override
    public String toString() {
        return ToString.builder("TimeoutConfiguration")
                       .add("apiCallTimeout", apiCallTimeout)
                       .add("apiCallAttemptTimeout", apiCallAttemptTimeout)
                       .build();
    }

    /** Validates that a timeout, when present, is positive. */
    static Duration requirePositive(Duration timeout, String name) {
        if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
            throw new IllegalArgumentException(name + " must be positive, but was " + timeout + ".");
        }
        return timeout;
    }

    /** Builder of a {@link TimeoutConfiguration}. */
    public static final class Builder implements CopyableBuilder<Builder, TimeoutConfiguration> {
        private Duration apiCallTimeout;
        private Duration apiCallAttemptTimeout;

        private Builder() {
        }

        private Builder(TimeoutConfiguration configuration) {
            this.apiCallTimeout = configuration.apiCallTimeout;
            this.apiCallAttemptTimeout = configuration.apiCallAttemptTimeout;
        }

        /**
         * The whole-call limit; {@code null} removes it.
         *
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        public Builder apiCallTimeout(Duration apiCallTimeout) {
            this.apiCallTimeout = requirePositive(apiCallTimeout, "apiCallTimeout");
            return this;
        }

        /**
         * The per-attempt limit; {@code null} removes it.
         *
         * @throws IllegalArgumentException if the timeout is zero or negative
         */
        public Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            this.apiCallAttemptTimeout = requirePositive(apiCallAttemptTimeout, "apiCallAttemptTimeout");
            return this;
        }

        @Override
        public TimeoutConfiguration build() {
            return apiCallTimeout == null && apiCallAttemptTimeout == null ? NONE : new TimeoutConfiguration(this);
        }
    }
}
