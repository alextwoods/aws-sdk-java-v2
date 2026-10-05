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

package software.amazon.awssdk.core.waiters;

import java.time.Duration;
import java.util.Optional;

/**
 * Per-waiter or per-wait overrides of a modeled waiter: the maximum wait time and attempts, and the minimum and
 * maximum delay between polls. Immutable and thread safe.
 */
public final class WaiterOverrideConfiguration {
    private final Integer maxAttempts;
    private final Duration waitTimeout;
    private final Duration minDelay;
    private final Duration maxDelay;

    private WaiterOverrideConfiguration(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.waitTimeout = builder.waitTimeout;
        this.minDelay = builder.minDelay;
        this.maxDelay = builder.maxDelay;
        if (minDelay != null && maxDelay != null && minDelay.compareTo(maxDelay) > 0) {
            throw new IllegalArgumentException("minDelay " + minDelay + " exceeds maxDelay " + maxDelay);
        }
    }

    /** A builder with no overrides. */
    public static Builder builder() {
        return new Builder();
    }

    /** The maximum number of polls. */
    public Optional<Integer> maxAttempts() {
        return Optional.ofNullable(maxAttempts);
    }

    /** The maximum total wait time, instead of the default. */
    public Optional<Duration> waitTimeout() {
        return Optional.ofNullable(waitTimeout);
    }

    /** The minimum delay between polls, instead of the modeled {@code minDelay}. */
    public Optional<Duration> minDelay() {
        return Optional.ofNullable(minDelay);
    }

    /** The maximum delay between polls, instead of the modeled {@code maxDelay}. */
    public Optional<Duration> maxDelay() {
        return Optional.ofNullable(maxDelay);
    }

    /** A builder with this configuration's overrides. */
    public Builder toBuilder() {
        return new Builder().maxAttempts(maxAttempts).waitTimeout(waitTimeout).minDelay(minDelay).maxDelay(maxDelay);
    }

    @Override
    public String toString() {
        return "WaiterOverrideConfiguration(maxAttempts=" + maxAttempts + ", waitTimeout=" + waitTimeout
               + ", minDelay=" + minDelay + ", maxDelay=" + maxDelay + ")";
    }

    /** Builder of a {@link WaiterOverrideConfiguration}; not thread safe. */
    public static final class Builder {
        private Integer maxAttempts;
        private Duration waitTimeout;
        private Duration minDelay;
        private Duration maxDelay;

        private Builder() {
        }

        /** The maximum number of polls, at least one; {@code null} for no limit beyond the wait time. */
        public Builder maxAttempts(Integer maxAttempts) {
            if (maxAttempts != null && maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be at least 1: " + maxAttempts);
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /** The maximum total wait time, positive; {@code null} for the default. */
        public Builder waitTimeout(Duration waitTimeout) {
            this.waitTimeout = positive(waitTimeout, "waitTimeout");
            return this;
        }

        /** The minimum delay between polls, positive; {@code null} for the modeled value. */
        public Builder minDelay(Duration minDelay) {
            this.minDelay = positive(minDelay, "minDelay");
            return this;
        }

        /** The maximum delay between polls, positive; {@code null} for the modeled value. */
        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = positive(maxDelay, "maxDelay");
            return this;
        }

        /** The configuration. */
        public WaiterOverrideConfiguration build() {
            return new WaiterOverrideConfiguration(this);
        }

        private static Duration positive(Duration value, String name) {
            if (value != null && (value.isNegative() || value.isZero())) {
                throw new IllegalArgumentException(name + " must be positive: " + value);
            }
            return value;
        }
    }
}
