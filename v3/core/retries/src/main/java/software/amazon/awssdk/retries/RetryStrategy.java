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

import java.util.function.Predicate;

/**
 * A Modern Retry strategy: the token, attempt, quota and delay accounting that the AWS pipeline's single retry owner
 * consults between attempts (Requirements 10.6, 10.7, 13.11).
 *
 * <p>Tokens are single use. Every token returned by this strategy is {@link RetryToken.State#ACTIVE} and must be
 * passed to exactly one of {@link #refreshRetryToken}, {@link #recordSuccess} or {@link #recordFailure}; each of those
 * rejects a token that is no longer active or was issued by another strategy instance with an
 * {@link IllegalStateException} or {@link IllegalArgumentException}. A {@link RetryDecision.Retry} therefore
 * authorizes exactly one subsequent attempt, and a {@link RetryDecision.Stop} authorizes none.
 *
 * <p>Implementations are thread safe and shared by every call of the clients configured with them; the retry quota is
 * shared per {@link AcquireInitialTokenRequest#scope() scope}. No method blocks or sleeps: waits are returned as
 * delays for the caller to honor.
 */
public interface RetryStrategy {
    /** The maximum number of attempts of one call, including the first; at least one. */
    int maxAttempts();

    /** A token authorizing attempt 1 of a call, and the delay to wait before sending it. */
    AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request);

    /**
     * Classifies {@code failure} of the attempt authorized by {@code token} and decides whether another attempt is
     * made. {@code token} is consumed either way.
     *
     * @throws IllegalStateException if {@code token} is not {@link RetryToken.State#ACTIVE}
     * @throws IllegalArgumentException if {@code token} was not issued by this strategy
     */
    RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure);

    /**
     * Records that the attempt authorized by {@code token} succeeded, releasing its retry quota.
     *
     * @return the consumed token, now {@link RetryToken.State#SUCCEEDED}
     * @throws IllegalStateException if {@code token} is not {@link RetryToken.State#ACTIVE}
     * @throws IllegalArgumentException if {@code token} was not issued by this strategy
     */
    RetryToken recordSuccess(RetryToken token);

    /**
     * Records that the call ends with the attempt authorized by {@code token} failing without a refresh (for example
     * after cancellation or the whole-call timeout). Held retry quota is not returned, as for any failed call.
     *
     * @return the consumed token, now {@link RetryToken.State#FAILED}
     * @throws IllegalStateException if {@code token} is not {@link RetryToken.State#ACTIVE}
     * @throws IllegalArgumentException if {@code token} was not issued by this strategy
     */
    RetryToken recordFailure(RetryToken token);

    /**
     * Builder of the token-based strategies.
     *
     * @param <B> the builder type
     * @param <S> the strategy type
     */
    interface Builder<B extends Builder<B, S>, S extends RetryStrategy> {
        /** Maximum attempts per call, including the first; at least one. */
        B maxAttempts(int maxAttempts);

        /** Delay before retrying a {@link RetryClassification#TRANSIENT} failure. */
        B backoffStrategy(BackoffStrategy backoffStrategy);

        /** Delay before retrying a {@link RetryClassification#THROTTLING} failure. */
        B throttlingBackoffStrategy(BackoffStrategy throttlingBackoffStrategy);

        /**
         * Whether retries draw on the shared retry quota (the "circuit breaker"); default {@code true}. When
         * {@code false}, retries cost nothing and are bounded only by {@link #maxAttempts(int)}.
         */
        B circuitBreakerEnabled(boolean circuitBreakerEnabled);

        /** Capacity of each scope's retry quota; default 500. */
        B retryQuotaCapacity(int capacity);

        /** Quota cost of retrying a {@link RetryClassification#TRANSIENT} failure; default 14. */
        B transientFailureCost(int cost);

        /** Quota cost of retrying a {@link RetryClassification#THROTTLING} failure; default 5. */
        B throttlingFailureCost(int cost);

        /**
         * Replaces the default AWS classification table ({@link RetryClassifier#defaults()}). Whatever the
         * classifier, a failure whose request body cannot be replayed is never retried.
         */
        B classifier(RetryClassifier classifier);

        /**
         * Additionally treats failures matching {@code predicate} as {@link RetryClassification#TRANSIENT} when the
         * classifier finds them non-retryable. Cancellation, the whole-call timeout and non-replayable request bodies
         * stay non-retryable.
         */
        B retryOnException(Predicate<Throwable> predicate);

        /** A strategy with this builder's settings. */
        S build();
    }
}
