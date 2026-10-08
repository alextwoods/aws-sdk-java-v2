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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** The concurrency harness itself: documented-count validation, overlap, bounded failure and timeouts. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ConcurrencyHarnessTest {

    @Test
    void plan_withoutTheDocumentedMinimums_isRejected() {
        Duration timeout = Duration.ofSeconds(1);
        assertThrows(IllegalArgumentException.class, () -> ConcurrencyPlan.of("c", 1, 4, timeout));
        assertThrows(IllegalArgumentException.class, () -> ConcurrencyPlan.of("c", 4, 4, timeout));
        assertThrows(IllegalArgumentException.class, () -> ConcurrencyPlan.of("c", 4, 10, timeout));
        assertThrows(IllegalArgumentException.class, () -> ConcurrencyPlan.of("c", 2, 4, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> ConcurrencyPlan.of("c", 2, 4, Duration.ofMinutes(3)));
        ConcurrencyPlan plan = ConcurrencyPlan.of("c", 2, 6, timeout);
        assertEquals(3, plan.rounds());
        assertEquals("c (2 simultaneous, 6 total in 3 rounds, timeout PT1S)", plan.toString());
    }

    @Test
    void run_overlappingCalls_returnsEveryResultByIndexAndTripsOncePerRound() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("harness", 4, 12, Duration.ofSeconds(10));
        OverlapBarrier overlap = plan.overlapBarrier();
        List<Integer> rounds = new ArrayList<>();

        List<String> results = ConcurrentRun.run(plan, rounds::add, call -> {
            overlap.arrive();
            return call.lane() + "/" + call.round();
        });

        assertEquals(IntStream.range(0, 12).mapToObj(i -> i % 4 + "/" + i / 4).toList(), results);
        assertEquals(List.of(0, 1, 2), rounds);
        assertEquals(3, overlap.trips());
        assertEquals(4, overlap.maxInside());
    }

    @Test
    void run_aCallerThatNeverReachesTheOverlapBarrier_failsWithinTheBarrierTimeout() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("missing-party", 3, 6, Duration.ofSeconds(1));
        OverlapBarrier overlap = plan.overlapBarrier();
        long start = System.nanoTime();

        AssertionError failure = assertThrows(AssertionError.class, () -> ConcurrentRun.run(plan, call -> {
            if (call.lane() != 0) {
                overlap.arrive();
            }
            return null;
        }, overlap::breakBarrier));

        assertTrue(failure.getMessage().contains("missing-party"), failure.getMessage());
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(5)) < 0);
    }

    @Test
    void run_aFailingCall_reportsItAndReleasesTheCallersWaitingForIt() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("failing-call", 3, 9, Duration.ofSeconds(20));
        OverlapBarrier overlap = plan.overlapBarrier();
        IllegalStateException cause = new IllegalStateException("boom");
        long start = System.nanoTime();

        AssertionError failure = assertThrows(AssertionError.class, () -> ConcurrentRun.run(plan, call -> {
            if (call.index() == 4) {
                throw cause;
            }
            overlap.arrive();
            return null;
        }, overlap::breakBarrier));

        assertTrue(failure.getMessage().contains("index=4"), failure.getMessage());
        assertEquals(cause, failure.getCause());
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(10)) < 0,
                   "the waiting callers were released rather than left until the plan's timeout");
    }

    @Test
    void run_aHungCall_failsAtThePlanTimeout() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("hung-call", 2, 4, Duration.ofMillis(500));
        long start = System.nanoTime();

        AssertionError failure = assertThrows(AssertionError.class, () -> ConcurrentRun.run(plan, call -> {
            Thread.sleep(60_000);
            return null;
        }));

        assertTrue(failure.getMessage().contains("did not complete within PT0.5S; 0 of 4"), failure.getMessage());
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(5)) < 0);
    }

    @Test
    void runAsync_aFutureThatNeverCompletes_isCancelledAtThePlanTimeout() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("hung-future", 2, 4, Duration.ofMillis(500));
        List<CompletableFuture<Void>> started = new ArrayList<>();

        assertThrows(AssertionError.class, () -> ConcurrentRun.runAsync(plan, call -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            synchronized (started) {
                started.add(future);
            }
            return future;
        }));

        synchronized (started) {
            assertFalse(started.isEmpty());
            started.forEach(f -> assertTrue(f.isCancelled(), "cancelled at the deadline"));
        }
    }

    @Test
    void asyncOverlapGate_releasesArrivalsInWavesOfTheConfiguredParties() {
        AsyncOverlapGate gate = ConcurrencyPlan.of("gate", 3, 6, Duration.ofSeconds(1)).overlapGate();
        CompletableFuture<Void> first = gate.arrive();
        CompletableFuture<Void> second = gate.arrive();
        assertFalse(first.isDone());
        CompletableFuture<Void> third = gate.arrive();
        assertTrue(first.isDone() && second.isDone() && third.isDone());
        CompletableFuture<Void> fourth = gate.arrive();
        assertFalse(fourth.isDone());
        assertEquals(1, gate.trips());
        assertEquals(3, gate.maxPending());

        gate.failPending();
        assertInstanceOf(AssertionError.class, fourth.handle((r, t) -> t).join());
    }
}
