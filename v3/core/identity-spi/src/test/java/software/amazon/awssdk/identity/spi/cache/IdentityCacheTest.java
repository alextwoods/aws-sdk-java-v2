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

package software.amazon.awssdk.identity.spi.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.identity.spi.IdentityRefreshException;

/** The refresh contract of {@link IdentityCache} (Requirements 13.16, 13.17, 13.19). */
class IdentityCacheTest {
    private static final Instant T0 = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void freshValueIsReusedAndStaleValueIsRefreshed() {
        MutableClock clock = new MutableClock(T0);
        Loader loader = new Loader(() -> RefreshResult.of("v", clock.instant().plusSeconds(60), null));
        IdentityCache<String> cache = cache(loader, clock, IdentityCache.StaleValueBehavior.STRICT);

        assertEquals("v1", cache.get());
        assertEquals("v1", cache.get());
        assertEquals(1, loader.calls.get());
        clock.now = T0.plusSeconds(60);
        assertEquals("v2", cache.get());
        assertEquals(2, loader.calls.get());
    }

    @Test
    void failedPrefetchKeepsTheValueButFailedRequiredRefreshFailsWithTheCause() {
        MutableClock clock = new MutableClock(T0);
        RuntimeException outage = new IllegalStateException("outage");
        Deque<Supplier<RefreshResult<String>>> script = new ArrayDeque<>(List.of(
            () -> RefreshResult.of("first", T0.plusSeconds(100), T0.plusSeconds(50)),
            () -> { throw outage; },
            () -> { throw outage; }));
        IdentityCache<String> cache = IdentityCache.builder("test", () -> script.pop().get())
                                                   .clock(clock).executor(Runnable::run).build();

        assertEquals("first", cache.get());
        clock.now = T0.plusSeconds(60);
        assertEquals("first", cache.get()); // prefetch failed, value kept
        clock.now = T0.plusSeconds(100);
        IdentityRefreshException e = assertThrows(IdentityRefreshException.class, cache::get);
        assertSame(outage, e.getCause());
        assertEquals("test", e.providerName());
    }

    @Test
    void invalidatedValueIsNeverReturnedAgain() {
        MutableClock clock = new MutableClock(T0);
        AtomicInteger calls = new AtomicInteger();
        IdentityCache<String> cache = IdentityCache.builder("test", () -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("refresh failed");
            }
            return RefreshResult.of("v" + calls.get());
        }).clock(clock).executor(Runnable::run).build();

        assertEquals("v1", cache.get());
        assertTrue(cache.invalidate("v1"));
        IdentityRefreshException e = assertThrows(IdentityRefreshException.class, cache::get);
        assertEquals("refresh failed", e.getCause().getMessage());
        assertEquals("v3", cache.get());
        assertEquals(false, cache.invalidate("v1"));
    }

    @Test
    void allowKeepsAStaleValueWhenRefreshFailsUnlessInvalidated() {
        MutableClock clock = new MutableClock(T0);
        AtomicInteger calls = new AtomicInteger();
        IdentityCache<String> cache = IdentityCache.builder("imds", () -> {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("imds down");
            }
            return RefreshResult.of("v1", T0.plusSeconds(10), null);
        }).clock(clock).executor(Runnable::run).staleValueBehavior(IdentityCache.StaleValueBehavior.ALLOW).build();

        assertEquals("v1", cache.get());
        clock.now = T0.plusSeconds(20);
        assertEquals("v1", cache.get());
        cache.invalidate("v1");
        assertThrows(IdentityRefreshException.class, cache::get);
    }

    @Test
    void concurrentCallersShareOneLoadAndAsyncNeverBlocks() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        IdentityCache<String> cache = IdentityCache.builder("test", () -> {
            calls.incrementAndGet();
            try {
                release.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return RefreshResult.of("v");
        }).build();

        CompletableFuture<String> first = cache.getAsync();
        CompletableFuture<String> second = cache.getAsync();
        assertTrue(!first.isDone() && !second.isDone());
        release.countDown();
        assertEquals("v", first.get());
        assertEquals("v", second.get());
        assertEquals(1, calls.get());
    }

    /**
     * Reproducer of a duplicate refresh found by the task 18.7 concurrency suites: a caller that read the stale value,
     * then lost the CPU while another caller's load ran to completion, used to start a second load for the same expiry
     * instead of returning the value that load had just cached.
     */
    @Test
    void aCallerThatSawTheStaleValueBeforeAConcurrentLoadFinishedReusesThatLoadsValue() throws Exception {
        GatedClock clock = new GatedClock(T0);
        Loader loader = new Loader(() -> RefreshResult.of("v", clock.instant().plusSeconds(60), null));
        IdentityCache<String> cache = cache(loader, clock, IdentityCache.StaleValueBehavior.STRICT);
        assertEquals("v1", cache.get());
        clock.now = T0.plusSeconds(120);

        // A reads the stale v1, then stops at its first clock read, before it decides to refresh.
        CompletableFuture<String> a = new CompletableFuture<>();
        Thread caller = new Thread(() -> {
            try {
                a.complete(cache.get());
            } catch (RuntimeException e) {
                a.completeExceptionally(e);
            }
        });
        clock.gate(caller);
        caller.start();
        assertTrue(clock.reached.await(10, TimeUnit.SECONDS), "the caller reached the clock");

        // Meanwhile another caller refreshes the expired value to completion.
        assertEquals("v2", cache.get());
        clock.release.countDown();

        assertEquals("v2", a.get(10, TimeUnit.SECONDS), "the caller returns the value the concurrent load cached");
        assertEquals(2, loader.calls.get(), "exactly one load for the expiry");
    }

    /** A settable clock whose first read on one gated thread stops until {@link #release} opens. */
    private static final class GatedClock extends Clock {
        private final CountDownLatch reached = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean armed = new AtomicBoolean();
        private volatile Thread gated;
        private volatile Instant now;

        GatedClock(Instant now) {
            this.now = now;
        }

        void gate(Thread thread) {
            gated = thread;
            armed.set(true);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            Instant reading = now;
            if (Thread.currentThread() == gated && armed.compareAndSet(true, false)) {
                reached.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return reading;
        }
    }

    private static IdentityCache<String> cache(Loader loader, Clock clock, IdentityCache.StaleValueBehavior behavior) {
        return IdentityCache.builder("test", loader).clock(clock).executor(Runnable::run).staleValueBehavior(behavior)
                            .build();
    }

    private static final class Loader implements Supplier<RefreshResult<String>> {
        private final Supplier<RefreshResult<String>> template;
        private final AtomicInteger calls = new AtomicInteger();

        Loader(Supplier<RefreshResult<String>> template) {
            this.template = template;
        }

        @Override
        public RefreshResult<String> get() {
            RefreshResult<String> result = template.get();
            int n = calls.incrementAndGet();
            return RefreshResult.of(result.value() + n, result.staleTime().orElse(null),
                                    result.prefetchTime().orElse(null));
        }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
