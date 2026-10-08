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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The non-blocking counterpart of {@link OverlapBarrier} for asynchronous components, which must never block the thread
 * that calls them: each {@linkplain #arrive() arrival} gets a future that completes only when {@link #parties()}
 * arrivals of its wave are pending together, so a held exchange is answered only after the configured number of
 * asynchronous calls are in flight inside the component at the same time.
 *
 * <p>The gate itself never times out (nothing blocks on it); the suite's bounded wait on the calls' futures does, and
 * {@link #failPending} releases what is still held. The future of a wave is completed on the thread of its last
 * arrival, so callers continue on their own executor ({@code thenRunAsync(action, executor)}).
 */
public final class AsyncOverlapGate {
    private final String component;
    private final int parties;
    private final List<CompletableFuture<Void>> waves = new ArrayList<>();
    private int arrived;
    private int maxPending;

    AsyncOverlapGate(String component, int parties) {
        this.component = component;
        this.parties = parties;
    }

    /** Registers one in-flight call; the future completes once {@link #parties()} calls of this wave are pending. */
    public CompletableFuture<Void> arrive() {
        CompletableFuture<Void> wave;
        boolean last;
        synchronized (this) {
            int n = arrived++;
            int index = n / parties;
            while (waves.size() <= index) {
                waves.add(new CompletableFuture<>());
            }
            maxPending = Math.max(maxPending, n % parties + 1);
            wave = waves.get(index);
            last = n % parties == parties - 1;
        }
        if (last) {
            wave.complete(null);
        }
        return wave;
    }

    /** The number of calls that must be in flight together. */
    public int parties() {
        return parties;
    }

    /** How many waves had all parties in flight together. */
    public synchronized int trips() {
        return arrived / parties;
    }

    /** All arrivals so far. */
    public synchronized int arrivals() {
        return arrived;
    }

    /** The largest number of calls seen pending at the same time. */
    public synchronized int maxPending() {
        return maxPending;
    }

    /** Fails every wave that is still pending; for teardown after a failure. */
    public void failPending() {
        List<CompletableFuture<Void>> pending;
        synchronized (this) {
            pending = List.copyOf(waves);
        }
        AssertionError failure = new AssertionError(component + ": released by teardown before " + parties
                                                    + " calls were in flight together");
        pending.forEach(w -> w.completeExceptionally(failure));
    }
}
