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

package software.amazon.awssdk.test.fault;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.retries.RetryScheduler;

/**
 * A retry scheduler on a virtual clock: every requested delay is recorded exactly and, unless the scheduler holds
 * timers, elapses at once on the requesting thread while the virtual clock advances by it. Async retry delays are
 * thus asserted exactly (Requirement 19.8) without real waiting. A held timer elapses only when the test calls
 * {@link VirtualTimer#elapse()}; cancelling it is counted. Thread-safe.
 */
public final class VirtualRetryScheduler implements RetryScheduler {
    private final boolean hold;
    private final List<VirtualTimer> timers = new CopyOnWriteArrayList<>();
    private final BlockingQueue<VirtualTimer> pending = new LinkedBlockingQueue<>();
    private final AtomicLong virtualNanos = new AtomicLong();

    private VirtualRetryScheduler(boolean hold) {
        this.hold = hold;
    }

    /** A scheduler whose timers elapse as soon as they are requested. */
    public static VirtualRetryScheduler elapsing() {
        return new VirtualRetryScheduler(false);
    }

    /** A scheduler whose timers wait until the test elapses (or the pipeline cancels) them. */
    public static VirtualRetryScheduler holding() {
        return new VirtualRetryScheduler(true);
    }

    @Override
    public CompletableFuture<Void> delay(Duration delay) {
        Objects.requireNonNull(delay, "delay");
        VirtualTimer timer = new VirtualTimer(delay, virtualNanos);
        timers.add(timer);
        if (hold) {
            pending.add(timer);
        } else {
            timer.elapse();
        }
        return timer;
    }

    /** Every delay requested so far, in order. */
    public List<Duration> requested() {
        return timers.stream().map(VirtualTimer::delay).toList();
    }

    /** Every timer so far, in order. */
    public List<VirtualTimer> timers() {
        return List.copyOf(timers);
    }

    /** The virtual time that elapsed: the sum of the elapsed timers' delays. */
    public Duration virtualElapsed() {
        return Duration.ofNanos(virtualNanos.get());
    }

    /** Timers that neither elapsed nor were cancelled: a pending delay the pipeline abandoned. */
    public List<VirtualTimer> unresolved() {
        return timers.stream().filter(t -> !t.isDone()).toList();
    }

    /** Waits for the next held timer. */
    public VirtualTimer awaitPending(Duration timeout) {
        try {
            VirtualTimer timer = pending.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
            if (timer == null) {
                throw new IllegalStateException("No retry delay was requested within " + timeout);
            }
            return timer;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a retry delay", e);
        }
    }

    /** One requested delay. */
    public static final class VirtualTimer extends CompletableFuture<Void> {
        private final Duration delay;
        private final AtomicLong clock;
        private final AtomicInteger cancels = new AtomicInteger();

        private VirtualTimer(Duration delay, AtomicLong clock) {
            this.delay = delay;
            this.clock = clock;
        }

        public Duration delay() {
            return delay;
        }

        /** Advances the virtual clock by this delay and completes the timer, unless it was cancelled. */
        public boolean elapse() {
            if (isDone()) {
                return false;
            }
            clock.addAndGet(delay.toNanos());
            return complete(null);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancels.incrementAndGet();
            return super.cancel(mayInterruptIfRunning);
        }

        /** How many times the pipeline cancelled this timer. */
        public int cancels() {
            return cancels.get();
        }
    }
}
