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

import java.util.function.LongSupplier;
import java.util.function.Predicate;
import software.amazon.awssdk.retries.internal.RetrySettings;
import software.amazon.awssdk.retries.internal.TokenRetryCore;

/**
 * The adaptive Modern Retry mode: the {@link StandardRetryStrategy standard} accounting and defaults plus a per-scope
 * client-side rate limiter (Requirement 13.11).
 *
 * <p>Throttling responses lower the scope's send rate (CUBIC) and successes raise it. Once a throttling response has
 * enabled the limiter, every attempt, including attempt 1, reserves a send slot: the reservation's wait is returned
 * as {@link AcquireInitialTokenResponse#delay()} and folded into {@link RetryDecision.Retry#delay()} (the later of the
 * backoff and the reservation). The strategy never blocks; the AWS pipeline honors the delays without blocking an
 * event loop.
 *
 * <p>The adaptive mode suits clients whose calls are mostly throttled by a single resource; use one client (or one
 * scope) per resource.
 */
public final class AdaptiveRetryStrategy implements RetryStrategy {
    private final TokenRetryCore core;

    private AdaptiveRetryStrategy(RetrySettings settings) {
        this.core = new TokenRetryCore(settings, true);
    }

    /** A builder with the adaptive defaults (the standard ones). */
    public static Builder builder() {
        return new Builder(new RetrySettings());
    }

    /** An adaptive strategy with every default. */
    public static AdaptiveRetryStrategy create() {
        return builder().build();
    }

    /**
     * A builder with this strategy's settings. Strategies built from it share this strategy's per-scope retry quota
     * and rate limiters unless the quota capacity is changed.
     */
    public Builder toBuilder() {
        return new Builder(core.toSettings());
    }

    @Override
    public int maxAttempts() {
        return core.maxAttempts();
    }

    @Override
    public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        return core.acquireInitialToken(request);
    }

    @Override
    public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
        return core.refreshRetryToken(token, failure);
    }

    @Override
    public RetryToken recordSuccess(RetryToken token) {
        return core.recordSuccess(token);
    }

    @Override
    public RetryToken recordFailure(RetryToken token) {
        return core.recordFailure(token);
    }

    /** The currently available retry quota of {@code scope}. */
    public int availableRetryQuota(String scope) {
        return core.scopeState(scope).quota().available();
    }

    @Override
    public String toString() {
        return core.toString("AdaptiveRetryStrategy");
    }

    /** Builder of an {@link AdaptiveRetryStrategy}. */
    public static final class Builder implements RetryStrategy.Builder<Builder, AdaptiveRetryStrategy> {
        private final RetrySettings settings;

        private Builder(RetrySettings settings) {
            this.settings = settings;
        }

        @Override
        public Builder maxAttempts(int maxAttempts) {
            settings.maxAttempts(maxAttempts);
            return this;
        }

        @Override
        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            settings.backoff(backoffStrategy);
            return this;
        }

        @Override
        public Builder throttlingBackoffStrategy(BackoffStrategy throttlingBackoffStrategy) {
            settings.throttlingBackoff(throttlingBackoffStrategy);
            return this;
        }

        @Override
        public Builder circuitBreakerEnabled(boolean circuitBreakerEnabled) {
            settings.circuitBreakerEnabled(circuitBreakerEnabled);
            return this;
        }

        @Override
        public Builder retryQuotaCapacity(int capacity) {
            settings.quotaCapacity(capacity);
            return this;
        }

        @Override
        public Builder transientFailureCost(int cost) {
            settings.transientCost(cost);
            return this;
        }

        @Override
        public Builder throttlingFailureCost(int cost) {
            settings.throttlingCost(cost);
            return this;
        }

        @Override
        public Builder classifier(RetryClassifier classifier) {
            settings.classifier(classifier);
            return this;
        }

        @Override
        public Builder retryOnException(Predicate<Throwable> predicate) {
            settings.addRetryPredicate(predicate);
            return this;
        }

        /**
         * The monotonic nanosecond source of the rate limiter; default {@code System::nanoTime}. Replace it only to
         * make rate limiting deterministic in tests.
         */
        public Builder nanoTimeSource(LongSupplier nanoTimeSource) {
            settings.nanoClock(nanoTimeSource);
            return this;
        }

        @Override
        public AdaptiveRetryStrategy build() {
            return new AdaptiveRetryStrategy(settings);
        }
    }
}
