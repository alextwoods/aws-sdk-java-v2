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

package software.amazon.awssdk.test.fault;

import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/**
 * The deterministic standard retry strategy of a trace: fixed delays (no jitter) and no retry quota, so the delay
 * before each retry is a pure function of the failure (Requirement 19.8): {@code backoff} after a transient failure,
 * {@code throttlingBackoff} after throttling, either replaced by a valid {@code x-amz-retry-after} hint between it and
 * itself plus {@link software.amazon.awssdk.retries.RetryHints#HINT_EXTENSION}.
 *
 * @param maxAttempts the most attempts of one call, at least 1
 * @param backoff the delay before a retry of a transient failure
 * @param throttlingBackoff the delay before a retry of a throttling failure
 */
public record RetrySettings(int maxAttempts, Duration backoff, Duration throttlingBackoff) {
    /** Three attempts, 10 ms after a transient failure and 30 ms after throttling. */
    public static final RetrySettings DEFAULT = new RetrySettings(3, Duration.ofMillis(10), Duration.ofMillis(30));

    public RetrySettings {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1: " + maxAttempts);
        }
        Objects.requireNonNull(backoff, "backoff");
        Objects.requireNonNull(throttlingBackoff, "throttlingBackoff");
        if (backoff.isNegative() || throttlingBackoff.isNegative()) {
            throw new IllegalArgumentException("Delays must not be negative");
        }
    }

    /** These settings with {@code maxAttempts}. */
    public RetrySettings withMaxAttempts(int attempts) {
        return new RetrySettings(attempts, backoff, throttlingBackoff);
    }

    /** A new strategy of these settings, with its own (empty) per-scope state. */
    public StandardRetryStrategy strategy() {
        return StandardRetryStrategy.builder()
                                    .maxAttempts(maxAttempts)
                                    .backoffStrategy(backoff.isZero() ? BackoffStrategy.none()
                                                                      : BackoffStrategy.fixedDelay(backoff))
                                    .throttlingBackoffStrategy(throttlingBackoff.isZero()
                                                               ? BackoffStrategy.none()
                                                               : BackoffStrategy.fixedDelay(throttlingBackoff))
                                    .circuitBreakerEnabled(false)
                                    .build();
    }
}
