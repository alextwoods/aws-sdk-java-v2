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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SingleEntryIdentityCacheTest {

    @Test
    void sameKeyInstance_hits_withoutRecomputing() {
        SingleEntryIdentityCache<Object, Object> cache = new SingleEntryIdentityCache<>();
        AtomicInteger computes = new AtomicInteger();
        Object key = new Object();
        Object first = cache.get(key, k -> new Object() {
            final int n = computes.incrementAndGet();
        });
        assertThat(cache.get(key, k -> new Object())).isSameAs(first);
        assertThat(computes.get()).isEqualTo(1);
    }

    @Test
    void equalButDistinctKey_misses() {
        SingleEntryIdentityCache<String, Object> cache = new SingleEntryIdentityCache<>();
        Object first = cache.get(new String("k"), k -> new Object());
        assertThat(cache.get(new String("k"), k -> new Object())).isNotSameAs(first);
    }

    @Test
    void consistentMisses_stopRetaining() {
        SingleEntryIdentityCache<Object, Object> cache = new SingleEntryIdentityCache<>();
        for (int i = 0; i < SingleEntryIdentityCache.MAX_CONSECUTIVE_MISSES; i++) {
            cache.get(new Object(), k -> new Object());
        }
        assertThat(cache.hasEntry()).isTrue();
        Object tooMany = new Object();
        Object value = cache.get(tooMany, k -> new Object());
        // Not retained: the same key again is a miss.
        assertThat(cache.get(tooMany, k -> new Object())).isNotSameAs(value);
    }

    @Test
    void hitAfterMisses_resetsTheMissCount() {
        SingleEntryIdentityCache<Object, Object> cache = new SingleEntryIdentityCache<>();
        for (int i = 0; i < SingleEntryIdentityCache.MAX_CONSECUTIVE_MISSES - 1; i++) {
            cache.get(new Object(), k -> new Object());
        }
        Object steady = new Object();
        Object retained = cache.get(steady, k -> new Object());
        assertThat(cache.get(steady, k -> new Object())).isSameAs(retained);
        for (int i = 0; i < SingleEntryIdentityCache.MAX_CONSECUTIVE_MISSES - 1; i++) {
            cache.get(new Object(), k -> new Object());
        }
        Object next = new Object();
        assertThat(cache.get(next, k -> new Object())).isSameAs(cache.get(next, k -> new Object()));
    }
}
