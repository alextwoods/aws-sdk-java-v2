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

package software.amazon.awssdk.retries;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/** The strategy delay calculation: the backoff before retrying after a failed attempt. */
@FunctionalInterface
public interface BackoffStrategy {
    /**
     * The non-negative backoff after the failure of attempt {@code failedAttempt} (one-based), before hints are
     * applied.
     */
    Duration computeDelay(int failedAttempt);

    /** No backoff. */
    static BackoffStrategy none() {
        return attempt -> Duration.ZERO;
    }

    /** A constant backoff of {@code delay}. */
    static BackoffStrategy fixedDelay(Duration delay) {
        Delays.requireNonNegative(delay, "delay");
        return attempt -> delay;
    }

    /**
     * Exponential backoff with full jitter: a uniformly random delay in {@code [0, min(maxDelay, baseDelay *
     * 2^(failedAttempt - 1)))}.
     */
    static BackoffStrategy exponentialDelay(Duration baseDelay, Duration maxDelay) {
        return exponentialDelay(baseDelay, maxDelay, () -> ThreadLocalRandom.current().nextDouble());
    }

    /**
     * {@link #exponentialDelay(Duration, Duration)} drawing its jitter factor in {@code [0, 1)} from {@code jitter},
     * for example to make delays deterministic in tests.
     */
    static BackoffStrategy exponentialDelay(Duration baseDelay, Duration maxDelay, DoubleSupplier jitter) {
        Delays.requireNonNegative(baseDelay, "baseDelay");
        Delays.requireNonNegative(maxDelay, "maxDelay");
        Objects.requireNonNull(jitter, "jitter");
        long baseNanos = baseDelay.toNanos();
        long maxNanos = maxDelay.toNanos();
        return failedAttempt -> {
            if (failedAttempt < 1) {
                throw new IllegalArgumentException("Attempts are numbered from one: " + failedAttempt);
            }
            // Saturate the doubling instead of overflowing it; 62 doublings exceed any Duration in nanos.
            int doublings = Math.min(failedAttempt - 1, 62);
            long ceiling = baseNanos > (maxNanos >> doublings) ? maxNanos : Math.min(maxNanos, baseNanos << doublings);
            double factor = jitter.getAsDouble();
            if (!(factor >= 0.0 && factor < 1.0)) {
                throw new IllegalStateException("A jitter factor must be in [0, 1): " + factor);
            }
            return Duration.ofNanos((long) (ceiling * factor));
        };
    }
}
