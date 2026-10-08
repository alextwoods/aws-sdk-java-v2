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
import java.util.Objects;

/**
 * The documented counts of one concurrency suite (Requirement 19.10): {@code simultaneous} callers (at least two) run
 * {@code total} operations (more than {@code simultaneous}, and a multiple of it) in {@link #rounds()} rounds of one
 * operation per caller, and the whole run completes within {@code timeout} or fails.
 *
 * <p>Each round starts with every caller released together by a start barrier ({@link ConcurrentRun}); the suite proves
 * the configured overlap inside the component with an {@link OverlapBarrier} (blocking components) or an
 * {@link AsyncOverlapGate} (asynchronous components) of {@code simultaneous} parties.
 *
 * @param component what the suite exercises, used in thread names and failure messages
 * @param simultaneous the number of callers inside the component at the same time
 * @param total the number of operations of the whole run
 * @param timeout the bound on the whole run, at most {@link #MAX_TIMEOUT}
 */
public record ConcurrencyPlan(String component, int simultaneous, int total, Duration timeout) {
    /** The largest timeout a plan may declare, so that no suite can wait unboundedly long. */
    public static final Duration MAX_TIMEOUT = Duration.ofMinutes(2);

    public ConcurrencyPlan {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(timeout, "timeout");
        if (simultaneous < 2) {
            throw new IllegalArgumentException(component + ": the simultaneous-call count must be at least 2, was "
                                               + simultaneous);
        }
        if (total <= simultaneous) {
            throw new IllegalArgumentException(component + ": the total-operation count " + total
                                               + " must exceed the simultaneous-call count " + simultaneous);
        }
        if (total % simultaneous != 0) {
            throw new IllegalArgumentException(component + ": the total-operation count " + total
                                               + " must be a multiple of the simultaneous-call count " + simultaneous);
        }
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException(component + ": the timeout must be positive and at most " + MAX_TIMEOUT
                                               + ", was " + timeout);
        }
    }

    public static ConcurrencyPlan of(String component, int simultaneous, int total, Duration timeout) {
        return new ConcurrencyPlan(component, simultaneous, total, timeout);
    }

    /** The number of rounds: one operation per caller per round. */
    public int rounds() {
        return total / simultaneous;
    }

    /** A blocking overlap barrier of {@link #simultaneous()} parties that waits at most {@link #timeout()}. */
    public OverlapBarrier overlapBarrier() {
        return new OverlapBarrier(component, simultaneous, timeout);
    }

    /** A non-blocking overlap gate that releases arrivals in waves of {@link #simultaneous()}. */
    public AsyncOverlapGate overlapGate() {
        return new AsyncOverlapGate(component, simultaneous);
    }

    @Override
    public String toString() {
        return component + " (" + simultaneous + " simultaneous, " + total + " total in " + rounds() + " rounds, timeout "
               + timeout + ")";
    }
}
