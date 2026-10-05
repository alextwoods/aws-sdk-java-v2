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

/**
 * Outcome of {@link RetryStrategy#refreshRetryToken}: either exactly one more attempt ({@link Retry}) or none
 * ({@link Stop}) (Requirements 10.6, 10.7).
 */
public sealed interface RetryDecision permits RetryDecision.Retry, RetryDecision.Stop {
    /** How the failed attempt was classified. */
    RetryClassification classification();

    /**
     * The non-negative wait before continuing: before sending the next attempt for {@link Retry}, and before
     * completing the call with the failure for {@link Stop} (non-zero only for long-polling operations whose retry
     * quota is exhausted).
     */
    Duration delay();

    /** Why a strategy made no further attempt. */
    enum StopReason {
        /** The failure is not retryable. */
        NON_RETRYABLE,
        /** The call already made {@link RetryStrategy#maxAttempts()} attempts. */
        MAX_ATTEMPTS_REACHED,
        /** The scope's retry quota cannot pay for another attempt. */
        QUOTA_EXHAUSTED
    }

    /**
     * Another attempt is authorized.
     *
     * @param token the {@link RetryToken.State#ACTIVE} token of the next attempt; its {@link RetryToken#attempt()} is
     *              one more than the refreshed token's
     * @param delay the bounded wait before sending the next attempt
     * @param classification a retryable classification of the failed attempt
     */
    record Retry(RetryToken token, Duration delay, RetryClassification classification) implements RetryDecision {
        public Retry {
            Objects.requireNonNull(token, "token");
            Delays.requireNonNegative(delay, "delay");
            Objects.requireNonNull(classification, "classification");
            if (!classification.isRetryable()) {
                throw new IllegalArgumentException("A retry requires a retryable classification: " + classification);
            }
        }
    }

    /**
     * No further attempt is authorized; the call completes with the failure.
     *
     * @param token the consumed token, {@link RetryToken.State#FAILED}
     * @param reason why no further attempt is made
     * @param delay the wait before completing the call
     * @param classification the classification of the failed attempt
     */
    record Stop(RetryToken token, StopReason reason, Duration delay, RetryClassification classification)
        implements RetryDecision {
        public Stop {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(reason, "reason");
            Delays.requireNonNegative(delay, "delay");
            Objects.requireNonNull(classification, "classification");
        }
    }
}
