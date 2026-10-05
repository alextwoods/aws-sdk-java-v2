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

package software.amazon.awssdk.retries.internal;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryClassification;
import software.amazon.awssdk.retries.RetryClassifier;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryHints;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.utils.ToString;

/**
 * The token, attempt, quota and delay accounting shared by the standard and adaptive strategies.
 *
 * <p>Refresh algorithm for a failure of attempt {@code n}:
 * <ol>
 *   <li>classify: a non-replayable request is {@code NON_RETRYABLE}; otherwise the classifier decides, and a
 *       non-retryable, non-terminal failure matching a retry predicate becomes {@code TRANSIENT};</li>
 *   <li>stop with {@code NON_RETRYABLE} if not retryable, or {@code MAX_ATTEMPTS_REACHED} if {@code n >=
 *       maxAttempts};</li>
 *   <li>draw the classification's quota cost (zero with the circuit breaker disabled); stop with
 *       {@code QUOTA_EXHAUSTED} if unavailable (long-polling operations still receive the backoff delay);</li>
 *   <li>retry with a token for attempt {@code n + 1} holding the drawn cost, after the classification's backoff
 *       bounded by the response hint ({@link RetryHints}) and, in the adaptive mode, no earlier than the rate
 *       limiter's reservation.</li>
 * </ol>
 * In the adaptive mode every throttling classification lowers, and every success raises, the scope's send rate. A
 * success returns the token's drawn cost, or {@link RetrySettings#SUCCESS_INCREMENT} if it drew none.
 */
public final class TokenRetryCore {
    private final boolean adaptive;
    private final int maxAttempts;
    private final BackoffStrategy backoff;
    private final BackoffStrategy throttlingBackoff;
    private final boolean circuitBreakerEnabled;
    private final int quotaCapacity;
    private final int transientCost;
    private final int throttlingCost;
    private final RetryClassifier classifier;
    private final List<Predicate<Throwable>> retryPredicates;
    private final LongSupplier nanoClock;
    private final ScopeStore scopes;

    public TokenRetryCore(RetrySettings settings, boolean adaptive) {
        this.adaptive = adaptive;
        this.maxAttempts = settings.maxAttempts;
        this.backoff = settings.backoff;
        this.throttlingBackoff = settings.throttlingBackoff;
        this.circuitBreakerEnabled = settings.circuitBreakerEnabled;
        this.quotaCapacity = settings.quotaCapacity;
        this.transientCost = settings.transientCost;
        this.throttlingCost = settings.throttlingCost;
        this.classifier = settings.classifier;
        this.retryPredicates = List.copyOf(settings.retryPredicates);
        this.nanoClock = settings.nanoClock;
        ScopeStore shared = settings.sharedScopes;
        this.scopes = shared != null && shared.compatibleWith(quotaCapacity, adaptive)
                      ? shared : new ScopeStore(quotaCapacity, adaptive, nanoClock);
    }

    /** Builder state reproducing this core; strategies built from it share this core's per-scope state. */
    public RetrySettings toSettings() {
        RetrySettings settings = new RetrySettings();
        settings.maxAttempts = maxAttempts;
        settings.backoff = backoff;
        settings.throttlingBackoff = throttlingBackoff;
        settings.circuitBreakerEnabled = circuitBreakerEnabled;
        settings.quotaCapacity = quotaCapacity;
        settings.transientCost = transientCost;
        settings.throttlingCost = throttlingCost;
        settings.classifier = classifier;
        settings.retryPredicates.addAll(retryPredicates);
        settings.nanoClock = nanoClock;
        settings.sharedScopes = scopes;
        return settings;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    /** The per-scope state of {@code scope}; for accounting inspection. */
    public ScopeStore.State scopeState(String scope) {
        return scopes.get(Objects.requireNonNull(scope, "scope"));
    }

    public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        Objects.requireNonNull(request, "request");
        ScopeStore.State state = scopes.get(request.scope());
        Duration delay = adaptive ? state.rateLimiter().reserve() : Duration.ZERO;
        DefaultRetryToken token = new DefaultRetryToken(this, state, request.scope(), request.longPolling(), 1, 0,
                                                        state.quota().available());
        return new AcquireInitialTokenResponse(token, delay);
    }

    public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
        DefaultRetryToken current = own(token);
        Objects.requireNonNull(failure, "failure");
        requireActive(current);

        RetryClassification classification = classify(failure);
        if (!classification.isRetryable()) {
            return stop(current, RetryDecision.StopReason.NON_RETRYABLE, Duration.ZERO, classification);
        }
        if (current.attempt() >= maxAttempts) {
            return stop(current, RetryDecision.StopReason.MAX_ATTEMPTS_REACHED, Duration.ZERO, classification);
        }
        RetryQuota quota = current.scopeState.quota();
        int cost = !circuitBreakerEnabled ? 0
                 : classification == RetryClassification.THROTTLING ? throttlingCost : transientCost;
        if (!quota.tryAcquire(cost)) {
            Duration delay = current.longPolling ? attemptDelay(classification, current.attempt(), failure)
                                                 : Duration.ZERO;
            return stop(current, RetryDecision.StopReason.QUOTA_EXHAUSTED, delay, classification);
        }
        if (!current.consume(RetryToken.State.RETRIED)) {
            quota.release(cost);
            throw notActive(current);
        }
        int remaining = quota.available();
        current.quotaRemaining(remaining);
        updateRateLimiter(current, classification);

        Duration delay = attemptDelay(classification, current.attempt(), failure);
        if (adaptive) {
            Duration reservation = current.scopeState.rateLimiter().reserve();
            if (reservation.compareTo(delay) > 0) {
                delay = reservation;
            }
        }
        DefaultRetryToken next = new DefaultRetryToken(this, current.scopeState, current.scope(),
                                                       current.longPolling, current.attempt() + 1, cost, remaining);
        return new RetryDecision.Retry(next, delay, classification);
    }

    public RetryToken recordSuccess(RetryToken token) {
        DefaultRetryToken current = own(token);
        if (!current.consume(RetryToken.State.SUCCEEDED)) {
            throw notActive(current);
        }
        current.quotaRemaining(current.scopeState.quota().release(Math.max(current.quotaAcquired(),
                                                                            RetrySettings.SUCCESS_INCREMENT)));
        if (adaptive) {
            current.scopeState.rateLimiter().updateAfterSuccess();
        }
        return current;
    }

    public RetryToken recordFailure(RetryToken token) {
        DefaultRetryToken current = own(token);
        if (!current.consume(RetryToken.State.FAILED)) {
            throw notActive(current);
        }
        current.quotaRemaining(current.scopeState.quota().available());
        return current;
    }

    private RetryClassification classify(AttemptFailure failure) {
        if (!failure.requestReplayable()) {
            return RetryClassification.NON_RETRYABLE;
        }
        RetryClassification classification = Objects.requireNonNull(classifier.classify(failure),
                                                                     "RetryClassifier returned null");
        if (classification == RetryClassification.NON_RETRYABLE && !retryPredicates.isEmpty()
            && !DefaultRetryClassifier.isTerminal(failure.failure())) {
            for (Predicate<Throwable> predicate : retryPredicates) {
                if (predicate.test(failure.failure())) {
                    return RetryClassification.TRANSIENT;
                }
            }
        }
        return classification;
    }

    private Duration attemptDelay(RetryClassification classification, int failedAttempt, AttemptFailure failure) {
        BackoffStrategy strategy = classification == RetryClassification.THROTTLING ? throttlingBackoff : backoff;
        Duration computed = strategy.computeDelay(failedAttempt);
        if (computed == null || computed.isNegative()) {
            throw new IllegalStateException("BackoffStrategy returned an invalid delay: " + computed);
        }
        return RetryHints.boundedDelay(computed, RetryHints.parseRetryAfter(failure.retryAfterHint().orElse(null)),
                                       RetryHints.HINT_EXTENSION);
    }

    private RetryDecision.Stop stop(DefaultRetryToken current, RetryDecision.StopReason reason, Duration delay,
                                    RetryClassification classification) {
        if (!current.consume(RetryToken.State.FAILED)) {
            throw notActive(current);
        }
        current.quotaRemaining(current.scopeState.quota().available());
        updateRateLimiter(current, classification);
        return new RetryDecision.Stop(current, reason, delay, classification);
    }

    private void updateRateLimiter(DefaultRetryToken token, RetryClassification classification) {
        if (adaptive && classification == RetryClassification.THROTTLING) {
            token.scopeState.rateLimiter().updateAfterThrottling();
        }
    }

    private DefaultRetryToken own(RetryToken token) {
        Objects.requireNonNull(token, "token");
        if (token instanceof DefaultRetryToken t && t.owner == this) {
            return t;
        }
        throw new IllegalArgumentException("The retry token was not issued by this retry strategy: " + token);
    }

    private static void requireActive(DefaultRetryToken token) {
        if (token.state() != RetryToken.State.ACTIVE) {
            throw notActive(token);
        }
    }

    private static IllegalStateException notActive(DefaultRetryToken token) {
        return new IllegalStateException("The retry token of attempt " + token.attempt() + " was already used ("
                                         + token.state() + "); each token authorizes exactly one attempt.");
    }

    /** Renders the settings of a strategy named {@code name}. */
    public String toString(String name) {
        return ToString.builder(name)
                       .add("maxAttempts", maxAttempts)
                       .add("backoffStrategy", backoff)
                       .add("throttlingBackoffStrategy", throttlingBackoff)
                       .add("circuitBreakerEnabled", circuitBreakerEnabled)
                       .add("retryQuotaCapacity", quotaCapacity)
                       .add("transientFailureCost", transientCost)
                       .add("throttlingFailureCost", throttlingCost)
                       .add("classifier", classifier)
                       .add("retryPredicates", retryPredicates.size())
                       .build();
    }
}
