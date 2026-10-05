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

package software.amazon.awssdk.transfer.s3.internal.directory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import software.amazon.awssdk.transfer.s3.internal.TransferScope;

/**
 * Starts one transfer per item of an {@link ItemSource} with at most {@code maxConcurrency} in flight (the directory
 * transfer's explicit concurrency). Items are pulled only when a slot is free, so a directory with millions of files
 * or keys holds at most one listing page (or one directory iterator) plus the transfers in flight. Pulling may block
 * (walking a file tree), so the pull loop runs on the I/O executor, serialized. The returned future completes when the
 * source is exhausted and every started transfer has finished (whatever their outcomes), or fails if the source fails
 * or the scope ends.
 */
final class BoundedDispatcher<T> {
    private final ItemSource<T> source;
    private final Function<T, CompletableFuture<?>> start;
    private final int maxConcurrency;
    private final Executor executor;
    private final TransferScope scope;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger wip = new AtomicInteger();
    private final CompletableFuture<Void> done = new CompletableFuture<>();

    BoundedDispatcher(ItemSource<T> source, Function<T, CompletableFuture<?>> start, int maxConcurrency,
                      Executor executor, TransferScope scope) {
        this.source = source;
        this.start = start;
        this.maxConcurrency = maxConcurrency;
        this.executor = executor;
        this.scope = scope;
    }

    CompletableFuture<Void> run() {
        scope.onEnd(this::schedule);
        source.onAvailable(this::schedule);
        schedule();
        return done;
    }

    void schedule() {
        if (wip.getAndIncrement() != 0) {
            return;
        }
        try {
            executor.execute(this::drainLoop);
        } catch (RejectedExecutionException e) {
            wip.set(0);
            finish(e);
        }
    }

    private void drainLoop() {
        int missed = 1;
        do {
            drain();
            missed = wip.addAndGet(-missed);
        } while (missed != 0);
    }

    private void drain() {
        if (done.isDone()) {
            return;
        }
        if (scope.isEnded()) {
            if (inFlight.get() == 0) {
                Throwable cause = scope.cause();
                finish(cause != null ? cause : new CancellationException("The directory transfer was cancelled"));
            }
            return;
        }
        try {
            while (inFlight.get() < maxConcurrency && !scope.isEnded()) {
                T item = source.poll();
                if (item == null) {
                    break;
                }
                inFlight.incrementAndGet();
                CompletableFuture<?> transfer;
                try {
                    transfer = start.apply(item);
                } catch (RuntimeException e) {
                    transfer = CompletableFuture.failedFuture(e);
                }
                transfer.whenComplete((r, t) -> {
                    inFlight.decrementAndGet();
                    schedule();
                });
            }
            if (source.exhausted() && inFlight.get() == 0) {
                finish(null);
            }
        } catch (Exception e) {
            scope.end(TransferScope.Reason.FAILED, e);
            if (inFlight.get() == 0) {
                finish(e);
            }
        }
    }

    private void finish(Throwable failure) {
        source.close();
        if (failure == null) {
            done.complete(null);
        } else {
            done.completeExceptionally(failure);
        }
    }
}
