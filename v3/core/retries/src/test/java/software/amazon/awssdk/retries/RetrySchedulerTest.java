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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RetrySchedulerTest {
    private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, "retry-timer");
        t.setDaemon(true);
        return t;
    });

    {
        timer.setRemoveOnCancelPolicy(true);
    }

    @AfterEach
    void shutdown() {
        timer.shutdownNow();
    }

    @Test
    void completesAfterTheDelayWithoutBlockingTheCaller() throws Exception {
        RetryScheduler scheduler = RetryScheduler.of(timer);
        long start = System.nanoTime();
        CompletableFuture<Void> delay = scheduler.delay(Duration.ofMillis(50));
        assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(50));
        delay.get(5, TimeUnit.SECONDS);
        assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(50));
    }

    @Test
    void zeroDelayIsAlreadyComplete() {
        CompletableFuture<Void> first = RetryScheduler.of(timer).delay(Duration.ZERO);
        assertTrue(first.isDone());
        first.cancel(true);
        assertFalse(RetryScheduler.of(timer).delay(Duration.ZERO).isCancelled());
        assertThrows(IllegalArgumentException.class, () -> RetryScheduler.of(timer).delay(Duration.ofMillis(-1)));
    }

    @Test
    void cancellingTheFutureCancelsTheScheduledWait() {
        CompletableFuture<Void> delay = RetryScheduler.of(timer).delay(Duration.ofHours(1));
        assertEquals(1, timer.getQueue().size());
        assertTrue(delay.cancel(true));
        assertEquals(0, timer.getQueue().size());
    }

    @Test
    void completesOnTheCompletionExecutor() throws Exception {
        ExecutorService completion = Executors.newSingleThreadExecutor(r -> new Thread(r, "completion"));
        try {
            AtomicReference<String> thread = new AtomicReference<>();
            RetryScheduler.of(timer, completion).delay(Duration.ofMillis(5))
                          .thenRun(() -> thread.set(Thread.currentThread().getName()))
                          .get(5, TimeUnit.SECONDS);
            assertEquals("completion", thread.get());
        } finally {
            completion.shutdownNow();
        }
    }

    @Test
    void rejectedSchedulingFailsTheFuture() {
        timer.shutdown();
        CompletableFuture<Void> delay = RetryScheduler.of(timer).delay(Duration.ofMillis(5));
        ExecutionException e = assertThrows(ExecutionException.class, () -> delay.get(5, TimeUnit.SECONDS));
        assertInstanceOf(RejectedExecutionException.class, e.getCause());
    }

    @Test
    void theDefaultSchedulerCompletesOnTheCompletionExecutorAndStopsItsDaemonTimerOnClose() throws Exception {
        ExecutorService completion = Executors.newSingleThreadExecutor(r -> new Thread(r, "completion"));
        try {
            DefaultRetryScheduler scheduler = DefaultRetryScheduler.create("svc-retry", completion);
            AtomicReference<Thread> completedOn = new AtomicReference<>();
            scheduler.delay(Duration.ofMillis(100))
                     .thenRun(() -> completedOn.set(Thread.currentThread()))
                     .get(5, TimeUnit.SECONDS);
            assertEquals("completion", completedOn.get().getName());
            assertFalse(scheduler.isClosed());

            scheduler.close();
            assertTrue(scheduler.isClosed());
            CompletableFuture<Void> afterClose = scheduler.delay(Duration.ofMillis(5));
            ExecutionException e = assertThrows(ExecutionException.class, () -> afterClose.get(5, TimeUnit.SECONDS));
            assertInstanceOf(RejectedExecutionException.class, e.getCause());
        } finally {
            completion.shutdownNow();
        }
    }
}
