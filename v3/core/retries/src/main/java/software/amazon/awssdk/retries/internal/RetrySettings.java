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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryClassifier;

/**
 * Mutable builder state shared by the standard and adaptive strategy builders. The defaults are the AWS Modern Retry
 * ("retries 2.1") constants.
 */
public final class RetrySettings {
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final Duration DEFAULT_BASE_DELAY = Duration.ofMillis(50);
    public static final Duration DEFAULT_THROTTLING_BASE_DELAY = Duration.ofSeconds(1);
    public static final Duration DEFAULT_MAX_BACKOFF = Duration.ofSeconds(20);
    public static final int DEFAULT_QUOTA_CAPACITY = 500;
    public static final int DEFAULT_TRANSIENT_COST = 14;
    public static final int DEFAULT_THROTTLING_COST = 5;
    /** Quota returned by a success whose token acquired none, so the quota refills over successful calls. */
    public static final int SUCCESS_INCREMENT = 1;

    int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    BackoffStrategy backoff = BackoffStrategy.exponentialDelay(DEFAULT_BASE_DELAY, DEFAULT_MAX_BACKOFF);
    BackoffStrategy throttlingBackoff = BackoffStrategy.exponentialDelay(DEFAULT_THROTTLING_BASE_DELAY,
                                                                         DEFAULT_MAX_BACKOFF);
    boolean circuitBreakerEnabled = true;
    int quotaCapacity = DEFAULT_QUOTA_CAPACITY;
    int transientCost = DEFAULT_TRANSIENT_COST;
    int throttlingCost = DEFAULT_THROTTLING_COST;
    RetryClassifier classifier = RetryClassifier.defaults();
    List<Predicate<Throwable>> retryPredicates = new ArrayList<>();
    LongSupplier nanoClock = System::nanoTime;
    /**
     * Per-scope state inherited from the strategy this builder was copied from, or null for fresh state. It is reused
     * only while the quota capacity and mode are unchanged.
     */
    ScopeStore sharedScopes;

    public RetrySettings() {
    }

    public void maxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1: " + maxAttempts);
        }
        this.maxAttempts = maxAttempts;
    }

    public void backoff(BackoffStrategy backoff) {
        this.backoff = Objects.requireNonNull(backoff, "backoffStrategy");
    }

    public void throttlingBackoff(BackoffStrategy throttlingBackoff) {
        this.throttlingBackoff = Objects.requireNonNull(throttlingBackoff, "throttlingBackoffStrategy");
    }

    public void circuitBreakerEnabled(boolean enabled) {
        this.circuitBreakerEnabled = enabled;
    }

    public void quotaCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("retryQuotaCapacity must not be negative: " + capacity);
        }
        this.quotaCapacity = capacity;
    }

    public void transientCost(int cost) {
        this.transientCost = requireCost(cost, "transientFailureCost");
    }

    public void throttlingCost(int cost) {
        this.throttlingCost = requireCost(cost, "throttlingFailureCost");
    }

    public void classifier(RetryClassifier classifier) {
        this.classifier = Objects.requireNonNull(classifier, "classifier");
    }

    public void addRetryPredicate(Predicate<Throwable> predicate) {
        retryPredicates.add(Objects.requireNonNull(predicate, "predicate"));
    }

    public void nanoClock(LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    private static int requireCost(int cost, String name) {
        if (cost < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + cost);
        }
        return cost;
    }
}
