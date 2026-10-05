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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;

class AdaptiveRetryStrategyTest {
    private static final String SCOPE = "scope";

    private final AtomicLong nanos = new AtomicLong();

    private AdaptiveRetryStrategy strategy() {
        return AdaptiveRetryStrategy.builder()
                                    .nanoTimeSource(nanos::get)
                                    .backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none())
                                    .circuitBreakerEnabled(false)
                                    .maxAttempts(1_000)
                                    .build();
    }

    private void advance(Duration d) {
        nanos.addAndGet(d.toNanos());
    }

    private static AttemptFailure throttled() {
        return AttemptFailure.of(SdkServiceException.builder().statusCode(429).build());
    }

    @Test
    void limiterIsFreeUntilTheFirstThrottle() {
        AdaptiveRetryStrategy strategy = strategy();
        for (int i = 0; i < 100; i++) {
            AcquireInitialTokenResponse response = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE));
            assertEquals(Duration.ZERO, response.delay());
            strategy.recordSuccess(response.token());
        }
        // Transient (non-throttling) failures do not enable the limiter.
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
        RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(
            token, AttemptFailure.of(SdkServiceException.builder().statusCode(503).build())));
        assertEquals(Duration.ZERO, retry.delay());
    }

    @Test
    void throttlingSlowsSubsequentAttemptsWithoutBlocking() {
        AdaptiveRetryStrategy strategy = strategy();
        // Send at ~10 requests per second for a few seconds so the measured rate is known.
        for (int i = 0; i < 40; i++) {
            advance(Duration.ofMillis(100));
            strategy.recordSuccess(strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token());
        }
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
        long before = System.nanoTime();
        RetryDecision decision = strategy.refreshRetryToken(token, throttled());
        RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class, decision);
        assertEquals(RetryClassification.THROTTLING, retry.classification());
        // Once the burst capacity is spent, back-to-back reservations at the same instant queue behind each other.
        Duration previous = Duration.ZERO;
        int free = 0;
        for (int i = 0; i < 50; i++) {
            Duration delay = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).delay();
            if (delay.isZero()) {
                assertEquals(Duration.ZERO, previous, "a free reservation after a delayed one");
                free++;
            } else {
                assertTrue(delay.compareTo(previous) > 0, previous + " then " + delay);
                previous = delay;
            }
        }
        assertTrue(free < 50 && !previous.isZero(), "free=" + free);
        // The strategy computed the waits instead of sleeping.
        assertTrue(Duration.ofNanos(System.nanoTime() - before).compareTo(Duration.ofSeconds(1)) < 0);
        // Once the waited time has passed, capacity is available again.
        advance(Duration.ofSeconds(30));
        assertEquals(Duration.ZERO, strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).delay());
    }

    @Test
    void retryDelayIsTheLaterOfBackoffAndReservation() {
        AdaptiveRetryStrategy strategy = AdaptiveRetryStrategy.builder()
                                                              .nanoTimeSource(nanos::get)
                                                              .throttlingBackoffStrategy(
                                                                  BackoffStrategy.fixedDelay(Duration.ofMinutes(1)))
                                                              .circuitBreakerEnabled(false)
                                                              .maxAttempts(10)
                                                              .build();
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
        RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class,
                                                     strategy.refreshRetryToken(token, throttled()));
        assertEquals(Duration.ofMinutes(1), retry.delay());
    }
}
