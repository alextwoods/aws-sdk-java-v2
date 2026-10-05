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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;

class StandardRetryStrategyTest {
    private static final String SCOPE = "test-scope";
    private static final Duration TRANSIENT_BACKOFF = Duration.ofMillis(100);
    private static final Duration THROTTLING_BACKOFF = Duration.ofMillis(1000);

    private static StandardRetryStrategy.Builder deterministic() {
        return StandardRetryStrategy.builder()
                                    .backoffStrategy(BackoffStrategy.fixedDelay(TRANSIENT_BACKOFF))
                                    .throttlingBackoffStrategy(BackoffStrategy.fixedDelay(THROTTLING_BACKOFF));
    }

    private static AttemptFailure transientFailure() {
        return AttemptFailure.of(SdkServiceException.builder().statusCode(503).build());
    }

    private static AttemptFailure throttlingFailure() {
        return AttemptFailure.of(SdkServiceException.builder().statusCode(429).build());
    }

    private static RetryToken initial(RetryStrategy strategy) {
        AcquireInitialTokenResponse response = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE));
        assertEquals(Duration.ZERO, response.delay());
        return response.token();
    }

    @Test
    void defaultsAreTheModernRetryConstants() {
        StandardRetryStrategy strategy = StandardRetryStrategy.create();
        assertEquals(3, strategy.maxAttempts());
        assertEquals(500, strategy.availableRetryQuota(SCOPE));
        RetryToken token = initial(strategy);
        RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class,
                                                     strategy.refreshRetryToken(token, transientFailure()));
        assertEquals(14, retry.token().quotaAcquired());
        assertTrue(retry.delay().compareTo(Duration.ofMillis(50)) < 0, retry.delay().toString());
        RetryDecision.Retry throttled = assertInstanceOf(RetryDecision.Retry.class,
                                                         strategy.refreshRetryToken(retry.token(),
                                                                                    throttlingFailure()));
        assertEquals(5, throttled.token().quotaAcquired());
        // Throttling ceiling for failed attempt 2: 1 s * 2 = 2 s.
        assertTrue(throttled.delay().compareTo(Duration.ofSeconds(2)) < 0, throttled.delay().toString());
        assertEquals(500 - 14 - 5, strategy.availableRetryQuota(SCOPE));
        assertEquals(1, StandardRetryStrategy.doNotRetry().maxAttempts());
    }

    @Test
    void attemptsAreNumberedFromOneAndEachGrantAuthorizesOneMore() {
        StandardRetryStrategy strategy = deterministic().maxAttempts(4).build();
        RetryToken token = initial(strategy);
        assertEquals(1, token.attempt());
        assertEquals(RetryToken.State.ACTIVE, token.state());
        for (int expected = 2; expected <= 4; expected++) {
            RetryDecision decision = strategy.refreshRetryToken(token, transientFailure());
            RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class, decision);
            assertEquals(RetryToken.State.RETRIED, token.state());
            assertEquals(expected, retry.token().attempt());
            assertEquals(RetryToken.State.ACTIVE, retry.token().state());
            assertEquals(TRANSIENT_BACKOFF, retry.delay());
            assertEquals(RetryClassification.TRANSIENT, retry.classification());
            token = retry.token();
        }
        RetryDecision.Stop stop = assertInstanceOf(RetryDecision.Stop.class,
                                                   strategy.refreshRetryToken(token, transientFailure()));
        assertEquals(RetryDecision.StopReason.MAX_ATTEMPTS_REACHED, stop.reason());
        assertEquals(Duration.ZERO, stop.delay());
        assertSame(token, stop.token());
        assertEquals(RetryToken.State.FAILED, token.state());
    }

    @Test
    void nonRetryableFailuresStopWithoutDrawingQuota() {
        StandardRetryStrategy strategy = deterministic().build();
        RetryToken token = initial(strategy);
        RetryDecision.Stop stop = assertInstanceOf(RetryDecision.Stop.class, strategy.refreshRetryToken(
            token, AttemptFailure.of(SdkServiceException.builder().statusCode(400).build())));
        assertEquals(RetryDecision.StopReason.NON_RETRYABLE, stop.reason());
        assertEquals(RetryClassification.NON_RETRYABLE, stop.classification());
        assertEquals(500, strategy.availableRetryQuota(SCOPE));
    }

    @Test
    void nonReplayableBodiesAreNeverRetried() {
        StandardRetryStrategy strategy = deterministic().classifier(f -> RetryClassification.TRANSIENT)
                                                        .retryOnException(t -> true).build();
        RetryDecision decision = strategy.refreshRetryToken(
            initial(strategy), AttemptFailure.builder().failure(new RuntimeException()).requestReplayable(false).build());
        assertEquals(RetryDecision.StopReason.NON_RETRYABLE, assertInstanceOf(RetryDecision.Stop.class, decision)
            .reason());
    }

    @Test
    void quotaIsDrawnPerRetryAndRefusesWhenExhausted() {
        StandardRetryStrategy strategy = deterministic().maxAttempts(100).retryQuotaCapacity(30)
                                                        .transientFailureCost(10).throttlingFailureCost(4).build();
        RetryToken token = initial(strategy);
        token = assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(token, transientFailure()))
            .token();
        assertEquals(20, token.quotaRemaining());
        token = assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(token, throttlingFailure()))
            .token();
        assertEquals(16, strategy.availableRetryQuota(SCOPE));
        token = assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(token, transientFailure()))
            .token();
        assertEquals(6, strategy.availableRetryQuota(SCOPE));
        RetryDecision.Stop stop = assertInstanceOf(RetryDecision.Stop.class,
                                                   strategy.refreshRetryToken(token, transientFailure()));
        assertEquals(RetryDecision.StopReason.QUOTA_EXHAUSTED, stop.reason());
        assertEquals(Duration.ZERO, stop.delay());
        assertEquals(6, stop.token().quotaRemaining());
        // A cheaper throttling retry still fits in a fresh call.
        RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class,
                                                     strategy.refreshRetryToken(initial(strategy),
                                                                                throttlingFailure()));
        assertEquals(THROTTLING_BACKOFF, retry.delay());
        assertEquals(2, strategy.availableRetryQuota(SCOPE));
    }

    @Test
    void successReturnsTheTokensCostOrOne() {
        StandardRetryStrategy strategy = deterministic().retryQuotaCapacity(100).build();
        RetryToken retried = assertInstanceOf(RetryDecision.Retry.class,
                                              strategy.refreshRetryToken(initial(strategy), transientFailure()))
            .token();
        RetryToken other = assertInstanceOf(RetryDecision.Retry.class,
                                            strategy.refreshRetryToken(initial(strategy), transientFailure())).token();
        assertEquals(72, strategy.availableRetryQuota(SCOPE));
        RetryToken succeeded = strategy.recordSuccess(retried);
        assertSame(retried, succeeded);
        assertEquals(RetryToken.State.SUCCEEDED, succeeded.state());
        assertEquals(86, succeeded.quotaRemaining());
        strategy.recordFailure(other);
        assertEquals(RetryToken.State.FAILED, other.state());
        assertEquals(86, strategy.availableRetryQuota(SCOPE));
        // A first-attempt success refills one unit, never beyond the capacity.
        for (int i = 0; i < 20; i++) {
            strategy.recordSuccess(initial(strategy));
        }
        assertEquals(100, strategy.availableRetryQuota(SCOPE));
    }

    @Test
    void scopesHaveIndependentQuotas() {
        StandardRetryStrategy strategy = deterministic().build();
        strategy.refreshRetryToken(initial(strategy), transientFailure());
        assertEquals(486, strategy.availableRetryQuota(SCOPE));
        assertEquals(500, strategy.availableRetryQuota("other"));
    }

    @Test
    void circuitBreakerDisabledRetriesForFree() {
        StandardRetryStrategy strategy = deterministic().circuitBreakerEnabled(false).retryQuotaCapacity(0)
                                                        .maxAttempts(5).build();
        RetryToken token = initial(strategy);
        for (int i = 0; i < 4; i++) {
            RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class,
                                                         strategy.refreshRetryToken(token, transientFailure()));
            assertEquals(0, retry.token().quotaAcquired());
            token = retry.token();
        }
        assertEquals(RetryDecision.StopReason.MAX_ATTEMPTS_REACHED,
                     assertInstanceOf(RetryDecision.Stop.class, strategy.refreshRetryToken(token, transientFailure()))
                         .reason());
    }

    @Test
    void longPollingOperationsBackOffEvenWhenTheQuotaIsExhausted() {
        StandardRetryStrategy strategy = deterministic().retryQuotaCapacity(0).build();
        RetryToken token = strategy.acquireInitialToken(new AcquireInitialTokenRequest(SCOPE, true)).token();
        RetryDecision.Stop stop = assertInstanceOf(RetryDecision.Stop.class,
                                                   strategy.refreshRetryToken(token, throttlingFailure()));
        assertEquals(RetryDecision.StopReason.QUOTA_EXHAUSTED, stop.reason());
        assertEquals(THROTTLING_BACKOFF, stop.delay());
    }

    @Test
    void responseHintsAreBoundedByTheBackoff() {
        StandardRetryStrategy strategy = deterministic().maxAttempts(10).build();
        RetryToken token = initial(strategy);
        List<Duration> delays = new ArrayList<>();
        for (String hint : new String[] {"2500", "60000", "not-a-number", "5"}) {
            RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(
                token, AttemptFailure.builder().failure(SdkServiceException.builder().statusCode(503).build())
                                     .retryAfterHint(hint).build()));
            delays.add(retry.delay());
            token = retry.token();
        }
        assertEquals(List.of(Duration.ofMillis(2500), TRANSIENT_BACKOFF.plus(RetryHints.HINT_EXTENSION),
                             TRANSIENT_BACKOFF, TRANSIENT_BACKOFF), delays);
    }

    @Test
    void retryPredicatesExtendButNeverOverrideTerminalFailures() {
        StandardRetryStrategy strategy = deterministic().retryOnException(IllegalStateException.class::isInstance)
                                                        .build();
        assertInstanceOf(RetryDecision.Retry.class, strategy.refreshRetryToken(
            initial(strategy), AttemptFailure.of(new IllegalStateException())));
        assertInstanceOf(RetryDecision.Stop.class, strategy.refreshRetryToken(
            initial(strategy), AttemptFailure.of(new IllegalArgumentException())));
        StandardRetryStrategy always = deterministic().retryOnException(t -> true).build();
        assertInstanceOf(RetryDecision.Stop.class, always.refreshRetryToken(
            initial(always), AttemptFailure.of(new CancellationException())));
    }

    @Test
    void tokensAreSingleUseAndOwnedByTheirStrategy() {
        StandardRetryStrategy strategy = deterministic().build();
        RetryToken token = initial(strategy);
        strategy.refreshRetryToken(token, transientFailure());
        assertThrows(IllegalStateException.class, () -> strategy.refreshRetryToken(token, transientFailure()));
        assertThrows(IllegalStateException.class, () -> strategy.recordSuccess(token));
        assertThrows(IllegalStateException.class, () -> strategy.recordFailure(token));

        RetryToken succeeded = strategy.recordSuccess(initial(strategy));
        assertThrows(IllegalStateException.class, () -> strategy.recordSuccess(succeeded));

        StandardRetryStrategy other = deterministic().build();
        assertThrows(IllegalArgumentException.class, () -> other.recordSuccess(initial(strategy)));
        assertThrows(IllegalArgumentException.class, () -> strategy.recordSuccess(new RetryToken() {
            @Override
            public String scope() {
                return SCOPE;
            }

            @Override
            public int attempt() {
                return 1;
            }

            @Override
            public State state() {
                return State.ACTIVE;
            }

            @Override
            public int quotaAcquired() {
                return 0;
            }

            @Override
            public int quotaRemaining() {
                return 0;
            }
        }));
    }

    @Test
    void concurrentRefreshesOfOneTokenGrantAtMostOneAttempt() throws Exception {
        StandardRetryStrategy strategy = deterministic().build();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            for (int round = 0; round < 50; round++) {
                RetryToken token = initial(strategy);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<RetryDecision>> results = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    results.add(executor.submit(() -> {
                        start.await();
                        try {
                            return strategy.refreshRetryToken(token, transientFailure());
                        } catch (IllegalStateException e) {
                            return null;
                        }
                    }));
                }
                start.countDown();
                List<RetryDecision.Retry> granted = new ArrayList<>();
                for (Future<RetryDecision> result : results) {
                    if (result.get() instanceof RetryDecision.Retry retry) {
                        granted.add(retry);
                    }
                }
                assertEquals(1, granted.size());
                // The granted attempt succeeds and returns its cost, so the losers leaked no quota.
                strategy.recordSuccess(granted.get(0).token());
                assertEquals(500, strategy.availableRetryQuota(SCOPE));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void toBuilderCopiesSettingsAndSharesTheQuota() {
        StandardRetryStrategy strategy = deterministic().maxAttempts(5).build();
        strategy.refreshRetryToken(initial(strategy), transientFailure());
        StandardRetryStrategy copy = strategy.toBuilder().maxAttempts(2).build();
        assertNotSame(strategy, copy);
        assertEquals(5, strategy.maxAttempts());
        assertEquals(2, copy.maxAttempts());
        assertEquals(486, copy.availableRetryQuota(SCOPE));
        StandardRetryStrategy resized = strategy.toBuilder().retryQuotaCapacity(50).build();
        assertEquals(50, resized.availableRetryQuota(SCOPE));
        assertTrue(strategy.toString().contains("maxAttempts=5"), strategy.toString());
    }

    @Test
    void builderValidation() {
        assertThrows(IllegalArgumentException.class, () -> StandardRetryStrategy.builder().maxAttempts(0));
        assertThrows(IllegalArgumentException.class, () -> StandardRetryStrategy.builder().retryQuotaCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> StandardRetryStrategy.builder().transientFailureCost(-1));
        assertThrows(NullPointerException.class, () -> StandardRetryStrategy.builder().classifier(null));
        StandardRetryStrategy negative = StandardRetryStrategy.builder()
                                                              .backoffStrategy(a -> Duration.ofMillis(-1)).build();
        assertThrows(IllegalStateException.class,
                     () -> negative.refreshRetryToken(initial(negative), transientFailure()));
    }

    @Test
    void retryModes() {
        assertEquals(RetryMode.STANDARD, RetryMode.fromValue("Standard"));
        assertEquals(RetryMode.ADAPTIVE, RetryMode.fromValue(" adaptive "));
        assertInstanceOf(StandardRetryStrategy.class, RetryMode.STANDARD.strategyBuilder().build());
        assertInstanceOf(AdaptiveRetryStrategy.class, RetryMode.ADAPTIVE.strategyBuilder().build());
        IllegalArgumentException legacy = assertThrows(IllegalArgumentException.class,
                                                       () -> RetryMode.fromValue("legacy"));
        assertTrue(legacy.getMessage().contains("standard"), legacy.getMessage());
        assertThrows(IllegalArgumentException.class, () -> RetryMode.fromValue("fast"));
    }
}
