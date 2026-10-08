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

package software.amazon.awssdk.test.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.id;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.s;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryClassification;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;

/**
 * Concurrency suite of the retry strategies' shared per-scope state (Requirements 13.x via 19.9, 19.10): the retry
 * quota (token bucket) and the adaptive client-side rate limiter. Every test uses {@value #SIMULTANEOUS} simultaneous
 * callers, {@value #TOTAL} total operations and a bounded {@value #TIMEOUT_SECONDS}-second run.
 *
 * <p>Strategy level: an {@link OverlapBarrier} inside the {@linkplain StandardRetryStrategy.Builder#classifier
 * classifier} holds every {@code refreshRetryToken} until {@value #SIMULTANEOUS} refreshes are inside the strategy, so
 * their quota draws race; a second barrier after the decisions holds every caller until all have drawn, where the
 * available quota must be exactly the capacity minus the drawn costs, and every success must return its cost. The
 * adaptive limiter's {@code reserve} is a short monitor section that cannot be entered together: its callers are
 * released together by the round's start barrier and must receive exactly the multiset of delays a sequential oracle
 * produces with the same frozen clock.
 *
 * <p>Client level: one generated client and one strategy instance shared by every call; the first attempt of every
 * call fails with a retryable error, and the transport's overlap barrier (sync) or gate (async) proves that
 * {@value #SIMULTANEOUS} first attempts and then {@value #SIMULTANEOUS} retries are in flight together.
 */
class RetryConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;
    static final int COST = 5;
    static final String SCOPE = "concurrency-scope";

    private final List<FakeDynamoDb> fakes = new ArrayList<>();
    private final ExecutorService completion = Clients.completionExecutor("retry-completion", 3);

    @AfterEach
    void shutdown() {
        fakes.forEach(FakeDynamoDb::shutdown);
        Clients.shutdown(completion);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void standardStrategy_simultaneousRefreshes_conserveTheRetryQuota() {
        ConcurrencyPlan plan = plan("standard-quota");
        OverlapBarrier insideRefresh = plan.overlapBarrier();
        OverlapBarrier allDrawn = plan.overlapBarrier();
        int capacity = 500;
        StandardRetryStrategy strategy = Clients.retries(3).retryQuotaCapacity(capacity).transientFailureCost(COST)
                                                .classifier(f -> {
                                                    insideRefresh.arrive();
                                                    return RetryClassification.TRANSIENT;
                                                }).build();

        List<Integer> availableWhileDrawn = ConcurrentRun.run(plan, call -> {
            RetryToken first = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
            RetryDecision decision = strategy.refreshRetryToken(first, AttemptFailure.of(new RuntimeException("e")));
            allDrawn.arrive();
            int available = strategy.availableRetryQuota(SCOPE);
            // Nobody returns quota until every caller of the round has sampled it.
            allDrawn.arrive();
            RetryToken retry = assertInstanceOf(RetryDecision.Retry.class, decision).token();
            assertEquals(COST, retry.quotaAcquired());
            assertEquals(2, retry.attempt());
            strategy.recordSuccess(retry);
            return available;
        }, insideRefresh::breakBarrier, allDrawn::breakBarrier);

        availableWhileDrawn.forEach(a -> assertEquals(capacity - SIMULTANEOUS * COST, a, "quota while all drew"));
        assertEquals(capacity, strategy.availableRetryQuota(SCOPE), "every success returned its draw");
        assertEquals(plan.rounds(), insideRefresh.trips());
        assertEquals(SIMULTANEOUS, insideRefresh.maxInside());
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void standardStrategy_simultaneousRefreshesBeyondTheQuota_grantExactlyTheAffordableRetries() {
        ConcurrencyPlan plan = plan("standard-quota-exhaustion");
        OverlapBarrier insideRefresh = plan.overlapBarrier();
        OverlapBarrier decided = plan.overlapBarrier();
        int affordable = 3;
        StandardRetryStrategy strategy = Clients.retries(3).retryQuotaCapacity(affordable * COST)
                                                .transientFailureCost(COST)
                                                .classifier(f -> {
                                                    insideRefresh.arrive();
                                                    return RetryClassification.TRANSIENT;
                                                }).build();

        List<RetryDecision> decisions = ConcurrentRun.run(plan, call -> {
            RetryToken first = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
            RetryDecision decision = strategy.refreshRetryToken(first, AttemptFailure.of(new RuntimeException("e")));
            decided.arrive();
            if (decision instanceof RetryDecision.Retry retry) {
                strategy.recordSuccess(retry.token());
            }
            return decision;
        }, insideRefresh::breakBarrier, decided::breakBarrier);

        for (int round = 0; round < plan.rounds(); round++) {
            List<RetryDecision> ofRound = decisions.subList(round * SIMULTANEOUS, (round + 1) * SIMULTANEOUS);
            long retries = ofRound.stream().filter(RetryDecision.Retry.class::isInstance).count();
            assertEquals(affordable, retries, "round " + round + " granted exactly the affordable retries");
            ofRound.stream().filter(RetryDecision.Stop.class::isInstance)
                   .forEach(d -> assertEquals(RetryDecision.StopReason.QUOTA_EXHAUSTED,
                                              ((RetryDecision.Stop) d).reason()));
        }
        assertEquals(affordable * COST, strategy.availableRetryQuota(SCOPE));
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void adaptiveStrategy_simultaneousThrottledRefreshes_conserveTheQuotaAndEnableTheLimiter() {
        ConcurrencyPlan plan = plan("adaptive-quota");
        OverlapBarrier insideRefresh = plan.overlapBarrier();
        OverlapBarrier allDrawn = plan.overlapBarrier();
        int capacity = 500;
        AtomicLong nanos = new AtomicLong();
        AdaptiveRetryStrategy strategy = AdaptiveRetryStrategy.builder().maxAttempts(3)
                                                              .backoffStrategy(BackoffStrategy.none())
                                                              .throttlingBackoffStrategy(BackoffStrategy.none())
                                                              .retryQuotaCapacity(capacity)
                                                              .throttlingFailureCost(COST)
                                                              .nanoTimeSource(nanos::get)
                                                              .classifier(f -> {
                                                                  insideRefresh.arrive();
                                                                  return RetryClassification.THROTTLING;
                                                              }).build();

        List<Integer> availableWhileDrawn = ConcurrentRun.run(plan, call -> {
            RetryToken first = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
            RetryDecision decision = strategy.refreshRetryToken(first, AttemptFailure.of(new RuntimeException("e")));
            allDrawn.arrive();
            int available = strategy.availableRetryQuota(SCOPE);
            allDrawn.arrive();
            RetryDecision.Retry retry = assertInstanceOf(RetryDecision.Retry.class, decision);
            assertTrue(!retry.delay().isNegative());
            strategy.recordSuccess(retry.token());
            return available;
        }, insideRefresh::breakBarrier, allDrawn::breakBarrier);

        availableWhileDrawn.forEach(a -> assertEquals(capacity - SIMULTANEOUS * COST, a));
        assertEquals(capacity, strategy.availableRetryQuota(SCOPE));
        assertEquals(plan.rounds(), insideRefresh.trips());
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void adaptiveRateLimiter_simultaneousReservations_matchTheSequentialDebt() {
        ConcurrencyPlan plan = plan("adaptive-rate-limiter");
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        AdaptiveRetryStrategy subject = enabledLimiter(nanos);
        AdaptiveRetryStrategy oracle = enabledLimiter(nanos);
        List<Duration> expected = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) {
            AcquireInitialTokenResponse response = oracle.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE));
            expected.add(response.delay());
            oracle.recordFailure(response.token());
        }

        List<Duration> delays = ConcurrentRun.run(plan, call -> {
            AcquireInitialTokenResponse response = subject.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE));
            subject.recordFailure(response.token());
            return response.delay();
        });

        assertTrue(expected.stream().anyMatch(d -> !d.isZero()), "the limiter queued callers behind its debt");
        assertEquals(sorted(expected), sorted(delays), "each reservation took exactly one place in the debt queue");
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedSyncClient_overlappingRetriedCalls_conserveTheSharedQuota() {
        ConcurrencyPlan plan = plan("sync-client-retries");
        OverlapBarrier overlap = plan.overlapBarrier();
        FakeDynamoDb http = fake(FakeDynamoDb.sync().failFirstAttempts(1, "InternalServerError")
                                             .onRequest(r -> overlap.arrive()));
        RecordingStrategy strategy = new RecordingStrategy(Clients.retries(3).retryQuotaCapacity(500)
                                                                  .transientFailureCost(COST).build());

        try (DynamoDbClient client = Clients.sync(http).retryStrategy(strategy).build()) {
            List<String> echoes = ConcurrentRun.run(plan, call -> client.getItem(
                r -> r.tableName("t").key(Map.of("id", s(id(call))))).item().get("echo").s(), overlap::breakBarrier);
            for (int i = 0; i < TOTAL; i++) {
                assertEquals("t/call-" + i, echoes.get(i));
            }
        }
        // The first attempts of a round fill the barrier once, their retries once more.
        assertEquals(2 * plan.rounds(), overlap.trips());
        strategy.assertConserved(500);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedAsyncClient_overlappingRetriedCalls_conserveTheSharedQuota() {
        ConcurrencyPlan plan = plan("async-client-retries");
        AsyncOverlapGate gate = plan.overlapGate();
        FakeDynamoDb http = fake(FakeDynamoDb.async().failFirstAttempts(1, "InternalServerError")
                                             .gate(r -> gate.arrive()));
        RecordingStrategy strategy = new RecordingStrategy(Clients.retries(3).retryQuotaCapacity(500)
                                                                  .transientFailureCost(COST).build());

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion)
                                                 .retryStrategy(strategy).build()) {
            List<String> echoes = ConcurrentRun.runAsync(plan, call -> client.getItem(
                r -> r.tableName("t").key(Map.of("id", s(id(call))))).thenApply(g -> g.item().get("echo").s()),
                                                         gate::failPending);
            for (int i = 0; i < TOTAL; i++) {
                assertEquals("t/call-" + i, echoes.get(i));
            }
        }
        assertEquals(2 * plan.rounds(), gate.trips());
        strategy.assertConserved(500);
    }

    private static ConcurrencyPlan plan(String component) {
        return ConcurrencyPlan.of(component, SIMULTANEOUS, TOTAL, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    private FakeDynamoDb fake(FakeDynamoDb fake) {
        fakes.add(fake);
        return fake;
    }

    /** An adaptive strategy whose limiter one throttling response has enabled, at the frozen time of {@code nanos}. */
    private static AdaptiveRetryStrategy enabledLimiter(AtomicLong nanos) {
        AdaptiveRetryStrategy strategy = AdaptiveRetryStrategy.builder().maxAttempts(2)
                                                              .backoffStrategy(BackoffStrategy.none())
                                                              .throttlingBackoffStrategy(BackoffStrategy.none())
                                                              .nanoTimeSource(nanos::get)
                                                              .classifier(f -> RetryClassification.THROTTLING)
                                                              .build();
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create(SCOPE)).token();
        RetryDecision decision = strategy.refreshRetryToken(token, AttemptFailure.of(new RuntimeException("throttled")));
        strategy.recordFailure(assertInstanceOf(RetryDecision.Retry.class, decision).token());
        return strategy;
    }

    private static List<Duration> sorted(List<Duration> delays) {
        return delays.stream().sorted().toList();
    }

    /** Delegates to a standard strategy and counts its calls, recording the scopes the pipeline used. */
    private static final class RecordingStrategy implements RetryStrategy {
        private final StandardRetryStrategy delegate;
        private final Set<String> scopes = ConcurrentHashMap.newKeySet();
        private final LongAdder acquired = new LongAdder();
        private final LongAdder retried = new LongAdder();
        private final LongAdder succeeded = new LongAdder();
        private final LongAdder failed = new LongAdder();

        RecordingStrategy(StandardRetryStrategy delegate) {
            this.delegate = delegate;
        }

        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            scopes.add(request.scope());
            acquired.increment();
            return delegate.acquireInitialToken(request);
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            RetryDecision decision = delegate.refreshRetryToken(token, failure);
            if (decision instanceof RetryDecision.Retry) {
                retried.increment();
            }
            return decision;
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            succeeded.increment();
            return delegate.recordSuccess(token);
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            failed.increment();
            return delegate.recordFailure(token);
        }

        void assertConserved(int capacity) {
            assertEquals(1, scopes.size(), "one client, one retry scope: " + scopes);
            assertEquals(TOTAL, acquired.sum(), "one initial token per call");
            assertEquals(TOTAL, retried.sum(), "one retry per call");
            assertEquals(TOTAL, succeeded.sum(), "one success per call");
            assertEquals(0, failed.sum());
            assertEquals(capacity, delegate.availableRetryQuota(scopes.iterator().next()),
                         "every retry's draw was returned by its success");
        }
    }
}
