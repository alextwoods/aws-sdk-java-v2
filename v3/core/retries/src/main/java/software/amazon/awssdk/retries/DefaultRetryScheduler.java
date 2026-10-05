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

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.retries.internal.ExecutorRetryScheduler;

/**
 * The SDK-owned retry scheduler of an async client built without a caller-supplied one: a single daemon timer
 * thread that completes each wait on the client's completion executor. The client that creates it owns it and
 * {@linkplain #close() closes} it when the client closes; closing stops the timer thread and cancels pending waits.
 *
 * <p>Creating the timer here keeps the call pipeline free of scheduling types: the pipeline only ever sees the
 * {@link RetryScheduler} interface.
 */
public final class DefaultRetryScheduler implements RetryScheduler, AutoCloseable {
    private final ScheduledExecutorService timer;
    private final RetryScheduler delegate;

    private DefaultRetryScheduler(ScheduledExecutorService timer, Executor completionExecutor) {
        this.timer = timer;
        this.delegate = new ExecutorRetryScheduler(timer, completionExecutor);
    }

    /**
     * A new scheduler whose daemon timer thread is named {@code threadNamePrefix + "-" + n} and whose waits complete
     * on {@code completionExecutor}. The caller owns the result and must {@link #close()} it.
     */
    public static DefaultRetryScheduler create(String threadNamePrefix, Executor completionExecutor) {
        Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");
        Objects.requireNonNull(completionExecutor, "completionExecutor");
        AtomicInteger count = new AtomicInteger();
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadNamePrefix + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        return new DefaultRetryScheduler(timer, completionExecutor);
    }

    @Override
    public CompletableFuture<Void> delay(Duration delay) {
        return delegate.delay(delay);
    }

    /** Stops the timer thread; pending waits never complete and later waits complete exceptionally. */
    @Override
    public void close() {
        timer.shutdownNow();
    }

    /** Whether {@link #close()} was called. */
    public boolean isClosed() {
        return timer.isShutdown();
    }

    @Override
    public String toString() {
        return "DefaultRetryScheduler(" + timer + ")";
    }
}
