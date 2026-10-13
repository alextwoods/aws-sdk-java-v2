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

package software.amazon.awssdk.codecs.commons.internal;

import java.util.concurrent.atomic.AtomicReferenceArray;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * A striped, lock-free object pool for reusing serializer and deserializer instances across requests.
 *
 * <p>Declare each concrete pool as a {@code private static final} subclass, so the JIT can devirtualize the callback
 * methods at each call site.
 *
 * <p>Slots are probed starting from a Fibonacci hash of the thread ID, with cache-line-sized strides to avoid false
 * sharing under contention. The pool has four slots per available processor by default to absorb probe collisions. An
 * item that cannot be pooled (no free slot within the probe limit) is dropped for the garbage collector.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.codecs.commons.StripedPool}.
 *
 * @param <T> the pooled type
 * @param <C> the context type passed to {@link #acquire} and forwarded to {@link #create} and {@link #reset}
 */
@SdkInternalApi
@ThreadSafe
public abstract class StripedPool<T, C> {

    private static final int N_PROCS = Runtime.getRuntime().availableProcessors();

    // 64-byte cache line / 4-byte compressed oop = 16 refs per line
    private static final int STRIDE_SHIFT = 4;
    private static final int MAX_PROBE = 4;
    private static final int DEFAULT_MULTIPLIER = 4;

    private final int slotMask;
    private final AtomicReferenceArray<T> pool;

    /**
     * Creates a pool with four slots per available processor.
     */
    protected StripedPool() {
        this(DEFAULT_MULTIPLIER);
    }

    /**
     * Creates a pool with {@code multiplier} slots per available processor, rounded up to a power of two.
     *
     * @param multiplier the number of slots per processor
     */
    protected StripedPool(int multiplier) {
        int raw = Math.max(2, N_PROCS * multiplier);
        int logicalSlots = Integer.highestOneBit(raw - 1) << 1;
        this.slotMask = logicalSlots - 1;
        this.pool = new AtomicReferenceArray<>(logicalSlots << STRIDE_SHIFT);
    }

    /**
     * Creates a new instance when no pooled instance is available.
     */
    protected abstract T create(C context);

    /**
     * Cleans up an item on every release, before {@link #canPool}. Use it to clear references that must not outlive the
     * request, such as an output sink. The default does nothing.
     */
    protected void cleanup(T item) {
    }

    /**
     * Returns whether {@code item} may be pooled. Called after {@link #cleanup}. Return false to drop the item, for
     * example when its internal buffer is gone.
     */
    protected abstract boolean canPool(T item);

    /**
     * Prepares an item for the pool. Called only when {@link #canPool} returned true; use it, for example, to shrink an
     * oversized buffer. The default does nothing.
     */
    protected void prepareForPool(T item) {
    }

    /**
     * Reinitializes a pooled item for reuse with {@code context}. Return true to accept the item, or false if it does
     * not suit this acquire (for example, different settings); a rejected item is put back and probing continues.
     */
    protected abstract boolean reset(T item, C context);

    /**
     * Returns a pooled item reset for {@code context}, or a new item from {@link #create}.
     *
     * @param context the context for this use
     * @return an item ready to use
     */
    public final T acquire(C context) {
        AtomicReferenceArray<T> p = this.pool;
        int mask = this.slotMask;
        int base = baseSlot(mask);
        for (int i = 0; i < MAX_PROBE; i++) {
            int idx = ((base + i) & mask) << STRIDE_SHIFT;
            T s = p.get(idx);
            if (s != null && p.compareAndSet(idx, s, null)) {
                if (reset(s, context)) {
                    return s;
                }
                p.compareAndSet(idx, null, s);
            }
        }
        return create(context);
    }

    /**
     * Returns {@code item} to the pool, if it can be pooled and a slot is free.
     *
     * @param item the item to release; it must not be used after this call
     */
    public final void release(T item) {
        cleanup(item);
        if (!canPool(item)) {
            return;
        }
        prepareForPool(item);
        AtomicReferenceArray<T> p = this.pool;
        int mask = this.slotMask;
        int base = baseSlot(mask);
        for (int i = 0; i < MAX_PROBE; i++) {
            int idx = ((base + i) & mask) << STRIDE_SHIFT;
            if (p.get(idx) == null && p.compareAndSet(idx, null, item)) {
                return;
            }
        }
    }

    private static int baseSlot(int mask) {
        long id = Thread.currentThread().getId();
        return (int) (id * 0x9E3779B97F4A7C15L >>> 32) & mask;
    }
}
