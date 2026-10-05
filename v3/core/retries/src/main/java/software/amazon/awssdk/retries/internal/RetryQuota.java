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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The retry quota (token bucket) of one scope: retries draw capacity, successes return it, and a retry that cannot be
 * paid for is refused. Lock free.
 */
public final class RetryQuota {
    private final int capacity;
    private final AtomicInteger available;

    public RetryQuota(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative: " + capacity);
        }
        this.capacity = capacity;
        this.available = new AtomicInteger(capacity);
    }

    /** Draws {@code cost} if available; a zero cost always succeeds. */
    public boolean tryAcquire(int cost) {
        if (cost == 0) {
            return true;
        }
        while (true) {
            int current = available.get();
            if (current < cost) {
                return false;
            }
            if (available.compareAndSet(current, current - cost)) {
                return true;
            }
        }
    }

    /** Returns {@code amount}, never exceeding the capacity; the resulting available quota. */
    public int release(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        while (true) {
            int current = available.get();
            int next = (int) Math.min((long) current + amount, capacity);
            if (available.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    /** The currently available quota. */
    public int available() {
        return available.get();
    }

    /** The maximum quota. */
    public int capacity() {
        return capacity;
    }
}
