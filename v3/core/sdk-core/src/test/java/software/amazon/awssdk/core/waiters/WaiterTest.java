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

package software.amazon.awssdk.core.waiters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

/** The waiter runtime: acceptor order, terminal states, the Smithy backoff, and the non-blocking async loop. */
class WaiterTest {
    private static final WaiterOverrideConfiguration FAST = WaiterOverrideConfiguration.builder()
        .minDelay(Duration.ofMillis(1)).maxDelay(Duration.ofMillis(2)).build();

    private static Waiter<String> waiter() {
        return Waiter.of("Ready", List.of(
            WaiterAcceptor.onResponse(WaiterState.SUCCESS, "READY"::equals),
            WaiterAcceptor.onResponse(WaiterState.FAILURE, "BROKEN"::equals),
            WaiterAcceptor.onException(WaiterState.RETRY, e -> e instanceof IllegalStateException)),
                         Duration.ofSeconds(2), Duration.ofSeconds(120), FAST);
    }

    private static <T> java.util.function.Supplier<T> script(List<Object> results, AtomicInteger calls) {
        Iterator<Object> next = results.iterator();
        return () -> {
            calls.incrementAndGet();
            Object result = next.next();
            if (result instanceof RuntimeException e) {
                throw e;
            }
            @SuppressWarnings("unchecked")
            T value = (T) result;
            return value;
        };
    }

    @Test
    void retriesUntilASuccessAcceptorMatches() {
        AtomicInteger calls = new AtomicInteger();
        WaiterResponse<String> response = waiter().run(script(List.of(new IllegalStateException(), "PENDING",
                                                                      "READY"), calls), null);
        assertEquals("READY", response.matched().response().orElseThrow());
        assertEquals(3, response.attemptsExecuted());
        assertEquals(3, calls.get());
    }

    @Test
    void aFailureAcceptorOrAnUnmatchedExceptionFailsTheWait() {
        AtomicInteger calls = new AtomicInteger();
        SdkClientException failed = assertThrows(SdkClientException.class,
                                                 () -> waiter().run(script(List.of("BROKEN"), calls), null));
        assertTrue(failed.getMessage().contains("failure state"), failed.getMessage());
        IllegalArgumentException cause = new IllegalArgumentException("unmatched");
        SdkClientException unmatched = assertThrows(SdkClientException.class,
                                                    () -> waiter().run(script(List.of(cause), calls), null));
        assertEquals(cause, unmatched.getCause());
    }

    @Test
    void theMaxAttemptsOverrideEndsTheWait() {
        AtomicInteger calls = new AtomicInteger();
        WaiterOverrideConfiguration twice = FAST.toBuilder().maxAttempts(2).build();
        assertThrows(SdkClientException.class,
                     () -> waiter().run(script(List.of("PENDING", "PENDING", "READY"), calls), twice));
        assertEquals(2, calls.get());
    }

    @Test
    void theAsyncWaiterDelaysOnlyThroughTheScheduler() {
        List<CompletableFuture<Void>> delays = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        java.util.function.Supplier<String> polls = script(List.of("PENDING", "PENDING", "READY"), calls);
        CompletableFuture<WaiterResponse<String>> wait = waiter().runAsync(
            () -> CompletableFuture.completedFuture(polls.get()),
            delay -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                delays.add(future);
                return future;
            }, null);
        // Returned while a delay is pending: nothing slept on this thread.
        assertFalse(wait.isDone());
        assertEquals(1, delays.size());
        delays.get(0).complete(null);
        assertFalse(wait.isDone());
        delays.get(1).complete(null);
        assertEquals(3, wait.join().attemptsExecuted());
    }

    @Test
    void cancellingTheAsyncWaitCancelsThePendingDelay() {
        List<CompletableFuture<Void>> delays = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<WaiterResponse<String>> wait = waiter().runAsync(
            () -> {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture("PENDING");
            },
            delay -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                delays.add(future);
                return future;
            }, null);
        wait.cancel(false);
        assertTrue(delays.get(0).isCancelled());
        assertEquals(1, calls.get());
    }

    @Test
    void anAsyncFailureCompletesExceptionally() {
        CompletableFuture<WaiterResponse<String>> wait = waiter().runAsync(
            () -> CompletableFuture.completedFuture("BROKEN"), delay -> new CompletableFuture<>(), null);
        CompletionException failed = assertThrows(CompletionException.class, wait::join);
        assertTrue(failed.getCause() instanceof SdkClientException);
    }

    @Test
    void theDelayIsTrimmedAndStopsAtTheWaitTime() {
        Duration min = Duration.ofSeconds(2);
        Duration max = Duration.ofSeconds(120);
        assertEquals(min, WaiterDelays.delay(1, min, max, Duration.ofMinutes(5).toNanos(), 0.999));
        assertEquals(max, WaiterDelays.delay(20, min, max, Duration.ofMinutes(5).toNanos(), 1.0));
        assertEquals(Duration.ofSeconds(1), WaiterDelays.delay(5, min, max, Duration.ofSeconds(3).toNanos(), 0.5));
        assertNull(WaiterDelays.delay(5, min, max, min.toNanos(), 0.5));
    }

    /** The Smithy backoff stays within [minDelay, maxDelay] and leaves at least minDelay of the wait time. */
    @Property
    void theDelayIsBoundedByTheModeledDelays(@ForAll @IntRange(min = 1, max = 64) int attempt,
                                             @ForAll @IntRange(min = 1, max = 30) int minSeconds,
                                             @ForAll @IntRange(min = 0, max = 300) int extraSeconds,
                                             @ForAll @LongRange(min = 1, max = 3_600_000) long remainingMillis,
                                             @ForAll @DoubleRange(min = 0, max = 1, maxIncluded = false)
                                             double random) {
        Duration min = Duration.ofSeconds(minSeconds);
        Duration max = min.plusSeconds(extraSeconds);
        long remaining = Duration.ofMillis(remainingMillis).toNanos();
        Duration delay = WaiterDelays.delay(attempt, min, max, remaining, random);
        if (remaining <= min.toNanos()) {
            assertNull(delay);
            return;
        }
        assertTrue(delay.compareTo(max) <= 0, delay::toString);
        assertTrue(delay.isPositive(), delay::toString);
        assertTrue(remaining - delay.toNanos() >= min.toNanos(), delay::toString);
    }
}
