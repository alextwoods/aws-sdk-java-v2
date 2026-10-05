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
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.retries.internal.ExecutorRetryScheduler;

/**
 * Schedules the waits of async calls (retry backoff, adaptive rate limiting) without blocking a thread: the async
 * pipeline continues the next attempt when {@link #delay} completes, and cancels the returned future to cancel the
 * pending wait when the call is cancelled.
 */
@FunctionalInterface
public interface RetryScheduler {
    /**
     * A future completed after {@code delay}. A zero delay returns an already completed future. Cancelling the
     * future cancels the scheduled wait. If the wait cannot be scheduled, the future completes exceptionally with the
     * scheduler's rejection.
     *
     * @throws IllegalArgumentException if {@code delay} is negative
     */
    CompletableFuture<Void> delay(Duration delay);

    /**
     * A scheduler on {@code scheduler}. The future completes on the scheduler's thread, so dependents should be
     * short or move to another executor; see {@link #of(ScheduledExecutorService, Executor)}.
     */
    static RetryScheduler of(ScheduledExecutorService scheduler) {
        return new ExecutorRetryScheduler(Objects.requireNonNull(scheduler, "scheduler"), null);
    }

    /**
     * A scheduler timing waits on {@code scheduler} and completing the futures on {@code completionExecutor}, so that
     * the next attempt never runs on the timer thread.
     */
    static RetryScheduler of(ScheduledExecutorService scheduler, Executor completionExecutor) {
        return new ExecutorRetryScheduler(Objects.requireNonNull(scheduler, "scheduler"),
                                          Objects.requireNonNull(completionExecutor, "completionExecutor"));
    }
}
