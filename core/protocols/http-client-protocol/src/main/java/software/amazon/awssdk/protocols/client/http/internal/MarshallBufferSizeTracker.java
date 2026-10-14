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

package software.amazon.awssdk.protocols.client.http.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.schema.ApiOperation;

/**
 * Remembers, per operation, how large the last serialized request body was, so the next body buffer of that operation
 * starts large enough and does not grow (racecar phase E1, re-implemented).
 *
 * <p>Each operation gets an ordinal, the index of its slot in an identity hash table owned by one protocol instance,
 * and its hint is the {@code int} at that index of the table's hint array. Lookups are lock-free reads of a
 * {@code volatile} table. A new operation is added by copying the table and publishing the copy with a compare-and-set,
 * so readers never see a half-built table. The table stops taking new operations at {@link #MAX_OPERATIONS}, after
 * which unknown operations use {@link #DEFAULT_CAPACITY}; this bounds memory if a caller creates operation instances per
 * call.
 *
 * <p>Hint updates are plain, racy {@code int} writes: two threads recording the same operation may overwrite each
 * other, a reader may see an older value, and a write into a table that is being replaced may be lost. Each of these
 * only changes the starting capacity of a buffer, which grows as needed, so the race is benign.
 */
@SdkInternalApi
@ThreadSafe
public final class MarshallBufferSizeTracker {
    /**
     * The capacity of a body buffer for an operation with no recorded size.
     */
    public static final int DEFAULT_CAPACITY = 256;

    /**
     * The smallest hint; smaller bodies still start with this capacity.
     */
    static final int MIN_CAPACITY = 32;

    /**
     * The largest hint. Larger bodies start at this capacity and grow, so one very large request does not make every
     * later request of the operation allocate that much up front.
     */
    static final int MAX_CAPACITY = 1 << 20;

    /**
     * The largest number of operations the table tracks.
     */
    static final int MAX_OPERATIONS = 4096;

    private static final int INITIAL_SLOTS = 16;

    private static final AtomicReferenceFieldUpdater<MarshallBufferSizeTracker, Table> TABLE =
        AtomicReferenceFieldUpdater.newUpdater(MarshallBufferSizeTracker.class, Table.class, "table");

    private volatile Table table = new Table(new ApiOperation<?, ?>[INITIAL_SLOTS], new int[INITIAL_SLOTS], 0);

    /**
     * Returns the capacity to create the next body buffer of an operation with.
     *
     * @param operation the operation
     * @return the hint recorded for the operation, or {@link #DEFAULT_CAPACITY} if none has been recorded
     */
    public int initialCapacity(ApiOperation<?, ?> operation) {
        Table current = table;
        int slot = current.find(operation);
        if (slot < 0) {
            return DEFAULT_CAPACITY;
        }
        int hint = current.hints[slot];
        return hint == 0 ? DEFAULT_CAPACITY : hint;
    }

    /**
     * Records the size of a body serialized for an operation. The next buffer starts at that size plus one eighth, so a
     * body of the same or a slightly larger size fits without growing, clamped to [{@link #MIN_CAPACITY},
     * {@link #MAX_CAPACITY}].
     *
     * @param operation the operation
     * @param size the number of bytes serialized
     */
    public void record(ApiOperation<?, ?> operation, int size) {
        int hint = hintFor(size);
        while (true) {
            Table current = table;
            int slot = current.find(operation);
            if (slot >= 0) {
                current.hints[slot] = hint;
                return;
            }
            Table added = current.with(operation, hint);
            if (added == null || TABLE.compareAndSet(this, current, added)) {
                return;
            }
        }
    }

    static int hintFor(int size) {
        long withHeadroom = (long) Math.max(size, 0) + (Math.max(size, 0) >>> 3);
        return (int) Math.max(MIN_CAPACITY, Math.min(MAX_CAPACITY, withHeadroom));
    }

    /**
     * An immutable-shape open-addressing identity table; only the hint values change after publication.
     */
    private static final class Table {
        private final ApiOperation<?, ?>[] keys;
        private final int[] hints;
        private final int size;

        private Table(ApiOperation<?, ?>[] keys, int[] hints, int size) {
            this.keys = keys;
            this.hints = hints;
            this.size = size;
        }

        private int find(ApiOperation<?, ?> operation) {
            int mask = keys.length - 1;
            int slot = System.identityHashCode(operation) & mask;
            while (true) {
                ApiOperation<?, ?> key = keys[slot];
                if (key == operation) {
                    return slot;
                }
                if (key == null) {
                    return -1;
                }
                slot = (slot + 1) & mask;
            }
        }

        /**
         * Returns a copy with the operation added, resized to keep the load factor at most one half, or null if the
         * table is full.
         */
        private Table with(ApiOperation<?, ?> operation, int hint) {
            if (size >= MAX_OPERATIONS) {
                return null;
            }
            int length = keys.length;
            if ((size + 1) * 2 > length) {
                length *= 2;
            }
            ApiOperation<?, ?>[] newKeys = new ApiOperation<?, ?>[length];
            int[] newHints = new int[length];
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != null) {
                    insert(newKeys, newHints, keys[i], hints[i]);
                }
            }
            insert(newKeys, newHints, operation, hint);
            return new Table(newKeys, newHints, size + 1);
        }

        private static void insert(ApiOperation<?, ?>[] keys, int[] hints, ApiOperation<?, ?> operation, int hint) {
            int mask = keys.length - 1;
            int slot = System.identityHashCode(operation) & mask;
            while (keys[slot] != null) {
                slot = (slot + 1) & mask;
            }
            keys[slot] = operation;
            hints[slot] = hint;
        }
    }
}
