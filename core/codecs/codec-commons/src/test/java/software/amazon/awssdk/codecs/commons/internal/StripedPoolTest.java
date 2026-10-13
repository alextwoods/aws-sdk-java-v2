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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * SDK tests for {@link StripedPool}; smithy-java has no unit tests for it.
 */
class StripedPoolTest {

    @Test
    void acquire_emptyPool_createsNewItem() {
        TestPool pool = new TestPool();

        Item item = pool.acquire("a");

        assertThat(item.context).isEqualTo("a");
        assertThat(pool.created.get()).isEqualTo(1);
    }

    @Test
    void acquire_afterReleaseOnSameThread_returnsPooledItemReset() {
        TestPool pool = new TestPool();
        Item item = pool.acquire("a");

        pool.release(item);
        Item again = pool.acquire("b");

        assertThat(again).isSameAs(item);
        assertThat(again.context).isEqualTo("b");
        assertThat(item.cleanedUp).isTrue();
        assertThat(item.prepared).isTrue();
        assertThat(pool.created.get()).isEqualTo(1);
    }

    @Test
    void acquire_resetRejectsItem_createsNewItemAndKeepsRejectedOne() {
        TestPool pool = new TestPool();
        Item item = pool.acquire("a");
        pool.release(item);

        Item other = pool.acquire(TestPool.REJECT);
        Item again = pool.acquire("c");

        assertThat(other).isNotSameAs(item);
        assertThat(again).isSameAs(item);
        assertThat(pool.created.get()).isEqualTo(2);
    }

    @Test
    void release_itemThatCannotBePooled_isCleanedUpButDropped() {
        TestPool pool = new TestPool();
        Item item = pool.acquire("a");
        item.poolable = false;

        pool.release(item);
        Item next = pool.acquire("b");

        assertThat(item.cleanedUp).isTrue();
        assertThat(item.prepared).isFalse();
        assertThat(next).isNotSameAs(item);
    }

    @Test
    void release_moreItemsThanProbedSlots_dropsExtras() {
        TestPool pool = new TestPool();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(pool.acquire("x"));
        }
        for (Item item : items) {
            pool.release(item);
        }

        // One thread probes at most four slots, so at most four items come back.
        int reused = 0;
        for (int i = 0; i < 10; i++) {
            if (items.contains(pool.acquire("y"))) {
                reused++;
            }
        }
        assertThat(reused).isBetween(1, 4);
    }

    @Test
    void acquire_concurrentThreads_neverHandsOneItemToTwoUsers() throws Exception {
        TestPool pool = new TestPool();
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 2_000; i++) {
                        Item item = pool.acquire("c");
                        if (!item.inUse.compareAndSet(false, true)) {
                            failed.set(true);
                        }
                        item.inUse.set(false);
                        pool.release(item);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(failed).isFalse();
    }

    private static final class Item {
        private final AtomicBoolean inUse = new AtomicBoolean();
        private String context;
        private boolean poolable = true;
        private boolean cleanedUp;
        private boolean prepared;
    }

    private static final class TestPool extends StripedPool<Item, String> {
        private static final String REJECT = "reject";
        private final AtomicInteger created = new AtomicInteger();

        @Override
        protected Item create(String context) {
            created.incrementAndGet();
            Item item = new Item();
            item.context = context;
            return item;
        }

        @Override
        protected void cleanup(Item item) {
            item.cleanedUp = true;
        }

        @Override
        protected boolean canPool(Item item) {
            return item.poolable;
        }

        @Override
        protected void prepareForPool(Item item) {
            item.prepared = true;
        }

        @Override
        protected boolean reset(Item item, String context) {
            if (REJECT.equals(context)) {
                return false;
            }
            item.context = context;
            return true;
        }
    }
}
