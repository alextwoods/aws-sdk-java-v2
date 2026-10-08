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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Runs a {@link ConcurrencyPlan}: {@link ConcurrencyPlan#simultaneous()} caller threads ("lanes") each perform one
 * operation per round for {@link ConcurrencyPlan#rounds()} rounds. A start barrier releases all lanes of a round
 * together, so every round has exactly {@code simultaneous} operations in flight, and the whole run is bounded by the
 * plan's timeout: a run that does not finish in time interrupts its lanes, runs the given failure actions (which
 * break the suite's overlap barriers or gates) and fails with an {@link AssertionError} naming the unfinished calls.
 */
public final class ConcurrentRun {

    /** One operation of a run: {@code index = round * simultaneous + lane}, unique in {@code [0, total)}. */
    public record Call(int lane, int round, int index) {
    }

    /** A blocking operation. */
    @FunctionalInterface
    public interface Operation<T> {
        T call(Call call) throws Exception;
    }

    /** An asynchronous operation: returns a future without blocking. */
    @FunctionalInterface
    public interface AsyncOperation<T> {
        CompletableFuture<T> start(Call call) throws Exception;
    }

    private ConcurrentRun() {
    }

    /**
     * Runs {@code operation} {@link ConcurrencyPlan#total()} times; returns the results by {@link Call#index()}.
     *
     * @param onFailure run (once) when the run fails or times out, before the failure is thrown
     */
    public static <T> List<T> run(ConcurrencyPlan plan, Operation<T> operation, Runnable... onFailure) {
        return run(plan, round -> { }, operation, onFailure);
    }

    /**
     * Runs {@code operation} {@link ConcurrencyPlan#total()} times, calling {@code beforeRound} with the round number
     * once all lanes reached the round's start barrier and before any of them is released (for example to advance a
     * test clock past an expiry); returns the results by {@link Call#index()}.
     *
     * @param onFailure run (once) when the run fails or times out, before the failure is thrown
     */
    public static <T> List<T> run(ConcurrencyPlan plan, IntConsumer beforeRound, Operation<T> operation,
                                  Runnable... onFailure) {
        long deadline = System.nanoTime() + plan.timeout().toNanos();
        int lanes = plan.simultaneous();
        AtomicInteger rounds = new AtomicInteger();
        OverlapBarrier start = new OverlapBarrier(plan.component() + " start", lanes, plan.timeout(),
                                                  () -> beforeRound.accept(rounds.getAndIncrement()));
        Object[] results = new Object[plan.total()];
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger threadIds = new AtomicInteger();
        AtomicBoolean failed = new AtomicBoolean();
        Runnable fail = () -> {
            if (failed.compareAndSet(false, true)) {
                // Release the lanes waiting at the start barrier, and inside the component for a caller that will
                // never arrive.
                start.breakBarrier();
                runAll(onFailure);
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(lanes, r -> {
            Thread t = new Thread(r, plan.component() + "-lane-" + threadIds.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int lane = 0; lane < lanes; lane++) {
                int laneIndex = lane;
                futures.add(executor.submit(() -> {
                    for (int round = 0; round < plan.rounds(); round++) {
                        Call call = new Call(laneIndex, round, round * lanes + laneIndex);
                        try {
                            start.arrive(remaining(deadline));
                            results[call.index()] = operation.call(call);
                            completed.incrementAndGet();
                        } catch (Throwable t) {
                            fail.run();
                            throw new CallFailure(plan, call, t);
                        }
                    }
                    return null;
                }));
            }
            awaitLanes(plan, futures, deadline, completed, executor, fail);
            @SuppressWarnings("unchecked")
            List<T> list = (List<T>) Arrays.asList(results);
            return list;
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Runs {@code operation} {@link ConcurrencyPlan#total()} times; each lane waits (bounded by the plan's deadline) for
     * its call's future before its next round, so each round has exactly {@code simultaneous} calls in flight. A call
     * still running at the deadline is cancelled.
     */
    public static <T> List<T> runAsync(ConcurrencyPlan plan, AsyncOperation<T> operation, Runnable... onFailure) {
        long deadline = System.nanoTime() + plan.timeout().toNanos();
        return run(plan, call -> {
            CompletableFuture<T> future = operation.start(call);
            try {
                return future.get(remaining(deadline), TimeUnit.NANOSECONDS);
            } catch (TimeoutException | InterruptedException e) {
                // The plan's deadline passed (in this lane, or in the run, which interrupts the lanes).
                future.cancel(true);
                throw e;
            } catch (ExecutionException e) {
                throw e.getCause() instanceof Exception cause ? cause : e;
            }
        }, onFailure);
    }

    private static void awaitLanes(ConcurrencyPlan plan, List<Future<?>> futures, long deadline,
                                   AtomicInteger completed, ExecutorService executor, Runnable fail) {
        List<Throwable> failures = new ArrayList<>();
        boolean timedOut = false;
        for (Future<?> future : futures) {
            try {
                future.get(remaining(deadline), TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                timedOut = true;
                break;
            } catch (ExecutionException e) {
                failures.add(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(plan + ": interrupted", e);
            }
        }
        if (timedOut) {
            fail.run();
            executor.shutdownNow();
            AssertionError error = new AssertionError(plan + ": did not complete within " + plan.timeout() + "; "
                                                      + completed.get() + " of " + plan.total()
                                                      + " operations completed");
            failures.forEach(error::addSuppressed);
            throw error;
        }
        if (!failures.isEmpty()) {
            Throwable primary = failures.stream().filter(f -> !isSecondary(f)).findFirst().orElse(failures.get(0));
            AssertionError error = new AssertionError(primary.getMessage(), primary.getCause());
            failures.stream().filter(f -> f != primary).limit(5).forEach(error::addSuppressed);
            throw error;
        }
    }

    private static boolean isSecondary(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof BrokenBarrierException || t instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    private static void runAll(Runnable[] actions) {
        for (Runnable action : actions) {
            try {
                action.run();
            } catch (RuntimeException e) {
                // A teardown action failing must not hide the run's failure.
            }
        }
    }

    private static long remaining(long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }

    /** The failure of one call; its message names the plan and the call. */
    private static final class CallFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CallFailure(ConcurrencyPlan plan, Call call, Throwable cause) {
            super(plan.component() + ": " + call + " failed: " + cause, cause);
        }
    }
}
