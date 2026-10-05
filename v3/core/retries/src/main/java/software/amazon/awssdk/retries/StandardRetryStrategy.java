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
import software.amazon.awssdk.retries.internal.RetrySettings;
import software.amazon.awssdk.retries.internal.TokenRetryCore;

/**
 * The standard Modern Retry mode: token-bucket retry quota, exponential backoff with full jitter and bounded service
 * hints (Requirement 13.11).
 *
 * <table>
 *   <caption>Defaults</caption>
 *   <tr><th>Setting</th><th>Default</th></tr>
 *   <tr><td>max attempts (including the first)</td><td>3</td></tr>
 *   <tr><td>transient backoff</td><td>exponential full jitter, base 50 ms, max 20 s</td></tr>
 *   <tr><td>throttling backoff</td><td>exponential full jitter, base 1 s, max 20 s</td></tr>
 *   <tr><td>retry quota per scope</td><td>500</td></tr>
 *   <tr><td>quota cost of a transient / throttling retry</td><td>14 / 5</td></tr>
 *   <tr><td>quota returned by a success</td><td>the token's cost, or 1 if it had none</td></tr>
 *   <tr><td>hint bound</td><td>computed backoff + 5 s ({@link RetryHints})</td></tr>
 *   <tr><td>classification</td><td>{@link RetryClassifier#defaults()}</td></tr>
 * </table>
 *
 * <pre>{@code
 * RetryStrategy strategy = StandardRetryStrategy.builder().maxAttempts(4).build();
 * }</pre>
 */
public final class StandardRetryStrategy implements RetryStrategy {
    private final TokenRetryCore core;

    private StandardRetryStrategy(RetrySettings settings) {
        this.core = new TokenRetryCore(settings, false);
    }

    /** A builder with the standard defaults. */
    public static Builder builder() {
        return new Builder(new RetrySettings());
    }

    /** A standard strategy with every default. */
    public static StandardRetryStrategy create() {
        return builder().build();
    }

    /** A strategy that never retries: one attempt per call. */
    public static StandardRetryStrategy doNotRetry() {
        return builder().maxAttempts(1).build();
    }

    /**
     * A builder with this strategy's settings. Strategies built from it share this strategy's per-scope retry quota
     * unless the quota capacity is changed.
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
        return core.toString("StandardRetryStrategy");
    }

    /** Builder of a {@link StandardRetryStrategy}. */
    public static final class Builder implements RetryStrategy.Builder<Builder, StandardRetryStrategy> {
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

        @Override
        public StandardRetryStrategy build() {
            return new StandardRetryStrategy(settings);
        }
    }
}
