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

package software.amazon.awssdk.core.internal.util;

import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A one-entry cache keyed by object identity, for values that are a pure function of a key which is, in practice,
 * the same instance on nearly every call — a client's {@code SdkClientConfiguration}, for example, which is one
 * instance for the life of the client unless a request carries plugins.
 *
 * <p>Hits cost one volatile read and one reference comparison. A miss computes the value and retains it, unless the
 * cache has missed {@link #MAX_CONSECUTIVE_MISSES} times in a row: a caller whose key is fresh on every call (a client
 * whose every request has plugins) would otherwise pay a contended write per call for a cache that never hits, so it
 * stops storing and simply computes. A hit resets the count.
 *
 * <p>Racing threads compute equal values for the same key, so a lost write costs one recomputation.
 */
@SdkInternalApi
public final class SingleEntryIdentityCache<K, V> {
    static final int MAX_CONSECUTIVE_MISSES = 8;

    private volatile Entry<K, V> entry;

    /**
     * Written only on a miss (at most {@link #MAX_CONSECUTIVE_MISSES} times in a row) and on the first hit after a
     * miss, so the steady-state hit path does one volatile read of it and never writes.
     */
    private volatile int consecutiveMisses;

    /**
     * The value for {@code key}: the retained one if it was computed for this same instance, otherwise
     * {@code compute} is applied and its result retained (subject to the miss guard).
     */
    public V get(K key, Function<? super K, ? extends V> compute) {
        Entry<K, V> cached = entry;
        if (cached != null && cached.key == key) {
            if (consecutiveMisses != 0) {
                consecutiveMisses = 0;
            }
            return cached.value;
        }
        V value = compute.apply(key);
        int misses = consecutiveMisses;
        if (misses < MAX_CONSECUTIVE_MISSES) {
            consecutiveMisses = misses + 1;
            entry = new Entry<>(key, value);
        }
        return value;
    }

    /**
     * Test hook: whether an entry is currently retained.
     */
    boolean hasEntry() {
        return entry != null;
    }

    private static final class Entry<K, V> {
        private final K key;
        private final V value;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
