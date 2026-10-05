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

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * One modeled waiter ({@code smithy.waiters#waitable}): its acceptors, in model order, and its delays. It polls an
 * operation until an acceptor transitions to {@link WaiterState#SUCCESS} or {@link WaiterState#FAILURE}, or until
 * the maximum wait time or attempts are exhausted.
 *
 * <p>A poll result is matched against the acceptors in order and the first match decides; a response no acceptor
 * matches is retried and an exception no acceptor matches fails the wait. Between polls the waiter delays by the
 * Smithy waiter backoff: exponential from {@code minDelay}, capped at {@code maxDelay}, with full jitter above
 * {@code minDelay}, and shortened so that the last poll happens within the maximum wait time.
 *
 * <p>{@link #run} polls and sleeps on the calling thread: it is the blocking waiter operation. {@link #runAsync}
 * never blocks: every delay is a future of the caller-supplied scheduler, such as the client's retry scheduler, and
 * cancelling the returned future cancels the pending poll or delay. Immutable and thread safe.
 *
 * @param <T> the polled operation's response type
 */
public final class Waiter<T> {
    /** The wait time when neither the waiter nor the wait overrides it. */
    public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(5);

    private final String name;
    private final List<WaiterAcceptor<T>> acceptors;
    private final Duration minDelay;
    private final Duration maxDelay;
    private final WaiterOverrideConfiguration overrides;

    private Waiter(String name, List<WaiterAcceptor<T>> acceptors, Duration minDelay, Duration maxDelay,
                   WaiterOverrideConfiguration overrides) {
        this.name = Objects.requireNonNull(name, "name");
        this.acceptors = List.copyOf(acceptors);
        this.minDelay = Objects.requireNonNull(minDelay, "minDelay");
        this.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay");
        this.overrides = overrides == null ? WaiterOverrideConfiguration.builder().build() : overrides;
    }

    /**
     * A waiter of the modeled acceptors and delays.
     *
     * @param name the waiter's modeled name, for messages
     * @param acceptors the modeled acceptors, in model order
     * @param minDelay the modeled {@code minDelay}
     * @param maxDelay the modeled {@code maxDelay}
     * @param overrides the waiter-level overrides, or {@code null}
     */
    public static <T> Waiter<T> of(String name, List<WaiterAcceptor<T>> acceptors, Duration minDelay,
                                   Duration maxDelay, WaiterOverrideConfiguration overrides) {
        return new Waiter<>(name, acceptors, minDelay, maxDelay, overrides);
    }

    /** Polls on the calling thread until the wait ends; {@code overrides} (or {@code null}) apply to this wait. */
    public WaiterResponse<T> run(Supplier<T> poll, WaiterOverrideConfiguration overrides) {
        Objects.requireNonNull(poll, "poll");
        Run run = new Run(overrides);
        while (true) {
            int attempt = run.attempts + 1;
            run.attempts = attempt;
            T response = null;
            Throwable failure = null;
            try {
                response = poll.get();
            } catch (RuntimeException e) {
                failure = e;
            }
            WaiterResponse<T> done = run.decide(response, failure);
            if (done != null) {
                return done;
            }
            Duration delay = run.nextDelay();
            try {
                TimeUnit.NANOSECONDS.sleep(delay.toNanos());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw SdkClientException.create("Waiter " + name + " was interrupted", e);
            }
        }
    }

    /**
     * Polls without blocking until the wait ends. Each delay is {@code scheduler}'s future; {@code poll} is invoked
     * on the thread completing the previous delay. {@code overrides} (or {@code null}) apply to this wait.
     */
    public CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> poll,
                                                         Function<Duration, CompletableFuture<Void>> scheduler,
                                                         WaiterOverrideConfiguration overrides) {
        Objects.requireNonNull(poll, "poll");
        Objects.requireNonNull(scheduler, "scheduler");
        CompletableFuture<WaiterResponse<T>> result = new CompletableFuture<>();
        AtomicReference<CompletableFuture<?>> pending = new AtomicReference<>();
        result.whenComplete((r, e) -> {
            CompletableFuture<?> current = pending.get();
            if (result.isCancelled() && current != null) {
                current.cancel(false);
            }
        });
        new AsyncRun(new Run(overrides), poll, scheduler, result, pending).poll();
        return result;
    }

    /** The state of one wait. */
    private final class Run {
        private final long startNanos = System.nanoTime();
        private final long timeoutNanos;
        private final Integer maxAttempts;
        private final Duration min;
        private final Duration max;
        private int attempts;

        Run(WaiterOverrideConfiguration perWait) {
            WaiterOverrideConfiguration call = perWait == null ? WaiterOverrideConfiguration.builder().build()
                                                               : perWait;
            this.timeoutNanos = call.waitTimeout().or(overrides::waitTimeout).orElse(DEFAULT_WAIT_TIMEOUT).toNanos();
            this.maxAttempts = call.maxAttempts().or(overrides::maxAttempts).orElse(null);
            this.min = call.minDelay().or(overrides::minDelay).orElse(minDelay);
            this.max = call.maxDelay().or(overrides::maxDelay).orElse(maxDelay);
            if (min.compareTo(max) > 0) {
                throw new IllegalArgumentException("Waiter " + name + ": minDelay " + min + " exceeds maxDelay "
                                                   + max);
            }
        }

        /** The ended wait's response, {@code null} to retry; throws when the wait fails. */
        WaiterResponse<T> decide(T response, Throwable failure) {
            for (WaiterAcceptor<T> acceptor : acceptors) {
                boolean matched = failure == null ? acceptor.matches(response) : acceptor.matchesException(failure);
                if (!matched) {
                    continue;
                }
                switch (acceptor.state()) {
                    case SUCCESS:
                        return new WaiterResponse<>(failure == null ? ResponseOrException.response(response)
                                                                    : ResponseOrException.exception(failure),
                                                    attempts);
                    case FAILURE:
                        throw SdkClientException.create("Waiter " + name + " transitioned to the failure state "
                                                        + "after " + attempts + " attempt(s)", failure);
                    default:
                        return null;
                }
            }
            if (failure != null) {
                throw SdkClientException.create("Waiter " + name + " polled an exception no acceptor matched",
                                                failure);
            }
            return null;
        }

        /** The delay before the next poll; throws when the wait time or attempts are exhausted. */
        Duration nextDelay() {
            long remaining = timeoutNanos - (System.nanoTime() - startNanos);
            if (maxAttempts != null && attempts >= maxAttempts) {
                throw SdkClientException.create("Waiter " + name + " exceeded its " + maxAttempts
                                                + " attempt(s)");
            }
            Duration delay = WaiterDelays.delay(attempts, min, max, remaining,
                                                ThreadLocalRandom.current().nextDouble());
            if (delay == null) {
                throw SdkClientException.create("Waiter " + name + " exceeded its maximum wait time after "
                                                + attempts + " attempt(s)");
            }
            return delay;
        }
    }

    /** One non-blocking wait: poll, decide, schedule the delay, poll again. */
    private final class AsyncRun {
        private final Run run;
        private final Supplier<CompletableFuture<T>> poll;
        private final Function<Duration, CompletableFuture<Void>> scheduler;
        private final CompletableFuture<WaiterResponse<T>> result;
        private final AtomicReference<CompletableFuture<?>> pending;

        AsyncRun(Run run, Supplier<CompletableFuture<T>> poll, Function<Duration, CompletableFuture<Void>> scheduler,
                 CompletableFuture<WaiterResponse<T>> result, AtomicReference<CompletableFuture<?>> pending) {
            this.run = run;
            this.poll = poll;
            this.scheduler = scheduler;
            this.result = result;
            this.pending = pending;
        }

        void poll() {
            if (result.isDone()) {
                return;
            }
            run.attempts++;
            CompletableFuture<T> call;
            try {
                call = Objects.requireNonNull(poll.get(), "poll future");
            } catch (RuntimeException e) {
                call = CompletableFuture.failedFuture(e);
            }
            track(call);
            call.whenComplete(this::polled);
        }

        private void polled(T response, Throwable failure) {
            if (result.isDone()) {
                return;
            }
            try {
                WaiterResponse<T> done = run.decide(response, unwrap(failure));
                if (done != null) {
                    result.complete(done);
                    return;
                }
                CompletableFuture<Void> delay = Objects.requireNonNull(scheduler.apply(run.nextDelay()),
                                                                       "delay future");
                track(delay);
                delay.whenComplete((ignored, delayFailure) -> {
                    if (delayFailure != null) {
                        result.completeExceptionally(unwrap(delayFailure));
                    } else {
                        poll();
                    }
                });
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        }

        private void track(CompletableFuture<?> current) {
            pending.set(current);
            if (result.isCancelled()) {
                current.cancel(false);
            }
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
               && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Override
    public String toString() {
        return "Waiter(" + name + ", acceptors=" + acceptors.size() + ", minDelay=" + minDelay + ", maxDelay="
               + maxDelay + ")";
    }
}
