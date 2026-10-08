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

import java.time.Duration;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A blocking barrier placed inside the component under test (a transport exchange, an interceptor hook, a classifier,
 * an identity load): a caller that {@linkplain #arrive() arrives} waits until {@link #parties()} callers are inside
 * at the same time, which proves the configured overlap. The barrier is cyclic, so each round trips it once.
 *
 * <p>Waits are bounded and failures are sticky: if the other parties do not arrive within the timeout, a waiting caller
 * is interrupted, or {@link #breakBarrier()} is called (by {@link ConcurrentRun} when any call fails), the barrier
 * breaks for good, and every waiting and later caller fails at once with an {@link AssertionError}. A missing overlap
 * therefore fails the suite instead of hanging it, and a failed caller never leaves the others waiting for it.
 */
public final class OverlapBarrier {
    private final String component;
    private final int parties;
    private final Duration timeout;
    private final Runnable onTrip;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition tripped = lock.newCondition();
    private int waiting;
    private long generation;
    private boolean broken;
    private int trips;
    private int maxInside;

    OverlapBarrier(String component, int parties, Duration timeout) {
        this(component, parties, timeout, () -> { });
    }

    /** {@code onTrip} runs on the last arriving caller before the waiting callers are released. */
    OverlapBarrier(String component, int parties, Duration timeout, Runnable onTrip) {
        this.component = component;
        this.parties = parties;
        this.timeout = timeout;
        this.onTrip = onTrip;
    }

    /** Waits, at most the barrier's timeout, until {@link #parties()} callers are inside the component together. */
    public void arrive() {
        arrive(timeout.toNanos());
    }

    /** Waits, at most {@code timeoutNanos}, until {@link #parties()} callers are inside together. */
    void arrive(long timeoutNanos) {
        lock.lock();
        try {
            if (broken) {
                throw brokenError();
            }
            long arrivedGeneration = generation;
            maxInside = Math.max(maxInside, ++waiting);
            if (waiting == parties) {
                try {
                    onTrip.run();
                } catch (RuntimeException | Error e) {
                    breakLocked();
                    throw e;
                }
                waiting = 0;
                generation++;
                trips++;
                tripped.signalAll();
                return;
            }
            long nanos = timeoutNanos;
            while (generation == arrivedGeneration && !broken) {
                if (nanos <= 0) {
                    int arrived = waiting;
                    breakLocked();
                    throw new AssertionError(component + ": only " + arrived + " of " + parties
                                             + " callers reached the overlap barrier within "
                                             + Duration.ofNanos(timeoutNanos), new TimeoutException());
                }
                try {
                    nanos = tripped.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    breakLocked();
                    Thread.currentThread().interrupt();
                    throw new AssertionError(component + ": interrupted at the overlap barrier", e);
                }
            }
            if (generation == arrivedGeneration) {
                throw brokenError();
            }
        } finally {
            lock.unlock();
        }
    }

    /** The number of callers that must be inside together. */
    public int parties() {
        return parties;
    }

    /** How many times all parties were inside together. */
    public int trips() {
        lock.lock();
        try {
            return trips;
        } finally {
            lock.unlock();
        }
    }

    /** The largest number of callers seen inside one wave of the barrier at the same time. */
    public int maxInside() {
        lock.lock();
        try {
            return maxInside;
        } finally {
            lock.unlock();
        }
    }

    /** Breaks the barrier for good, failing every waiting and later caller; for teardown after a failure. */
    public void breakBarrier() {
        lock.lock();
        try {
            breakLocked();
        } finally {
            lock.unlock();
        }
    }

    private void breakLocked() {
        broken = true;
        tripped.signalAll();
    }

    private AssertionError brokenError() {
        return new AssertionError(component + ": the overlap barrier broke (another caller failed or timed out)",
                                  new BrokenBarrierException());
    }
}
