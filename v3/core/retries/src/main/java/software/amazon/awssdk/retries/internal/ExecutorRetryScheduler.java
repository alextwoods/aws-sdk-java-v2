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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.retries.RetryScheduler;

/** {@link RetryScheduler} over a {@link ScheduledExecutorService}, optionally completing on another executor. */
public final class ExecutorRetryScheduler implements RetryScheduler {
    private static final CompletableFuture<Void> DONE = CompletableFuture.completedFuture(null);

    private final ScheduledExecutorService scheduler;
    private final Executor completionExecutor;

    public ExecutorRetryScheduler(ScheduledExecutorService scheduler, Executor completionExecutor) {
        this.scheduler = scheduler;
        this.completionExecutor = completionExecutor;
    }

    @Override
    public CompletableFuture<Void> delay(Duration delay) {
        Objects.requireNonNull(delay, "delay");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative: " + delay);
        }
        if (delay.isZero()) {
            // A fresh completed future so that a caller cancelling or obtruding it never affects another caller.
            return DONE.copy();
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        ScheduledFuture<?> scheduled;
        try {
            scheduled = scheduler.schedule(() -> complete(result), delay.toNanos(), TimeUnit.NANOSECONDS);
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
            return result;
        }
        result.whenComplete((r, t) -> {
            if (result.isCancelled()) {
                scheduled.cancel(false);
            }
        });
        return result;
    }

    private void complete(CompletableFuture<Void> result) {
        if (completionExecutor == null) {
            result.complete(null);
            return;
        }
        try {
            completionExecutor.execute(() -> result.complete(null));
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
        }
    }

    @Override
    public String toString() {
        return "RetryScheduler(" + scheduler + ")";
    }
}
