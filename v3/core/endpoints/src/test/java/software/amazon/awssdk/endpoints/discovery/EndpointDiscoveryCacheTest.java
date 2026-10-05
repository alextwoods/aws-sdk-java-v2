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

package software.amazon.awssdk.endpoints.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class EndpointDiscoveryCacheTest {
    private static final EndpointDiscoveryKey KEY = EndpointDiscoveryKey.of("AKID", "Query", Map.of());

    /** A clock the test advances. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2025-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** A discovery call counter whose calls complete when the test completes {@link #pending}. */
    private static final class Discovery implements Supplier<CompletableFuture<DiscoveredEndpoint>> {
        final AtomicInteger calls = new AtomicInteger();
        CompletableFuture<DiscoveredEndpoint> pending = new CompletableFuture<>();

        @Override
        public CompletableFuture<DiscoveredEndpoint> get() {
            calls.incrementAndGet();
            return pending;
        }
    }

    @Test
    void requiredDiscoveryWaitsForOneSharedCallAndCachesItsResult() {
        TestClock clock = new TestClock();
        EndpointDiscoveryCache cache = EndpointDiscoveryCache.create(clock, 10);
        Discovery discovery = new Discovery();
        CompletableFuture<Optional<URI>> first = cache.discover(KEY, true, discovery);
        CompletableFuture<Optional<URI>> second = cache.discover(KEY, true, discovery);
        assertFalse(first.isDone());
        assertEquals(1, discovery.calls.get());
        discovery.pending.complete(DiscoveredEndpoint.of("discovered.test", 1));
        assertEquals(Optional.of(URI.create("https://discovered.test")), first.join());
        assertEquals(first.join(), second.join());

        // Cached until the cache period ends.
        assertTrue(cache.discover(KEY, true, discovery).isDone());
        assertEquals(1, discovery.calls.get());
        clock.advance(Duration.ofMinutes(1));
        discovery.pending = new CompletableFuture<>();
        assertFalse(cache.discover(KEY, true, discovery).isDone());
        assertEquals(2, discovery.calls.get());
    }

    @Test
    void requiredDiscoveryFailsWithTheDiscoveryFailure() {
        EndpointDiscoveryCache cache = EndpointDiscoveryCache.create();
        IllegalStateException failure = new IllegalStateException("boom");
        CompletableFuture<Optional<URI>> result = cache.discover(KEY, true, () -> CompletableFuture.failedFuture(
            failure));
        CompletionException e = assertThrows(CompletionException.class, result::join);
        assertSame(failure, e.getCause());
        assertEquals(0, cache.size());
    }

    @Test
    void optionalDiscoveryNeverWaitsAndUsesStaleEndpointsWhileRefreshing() {
        TestClock clock = new TestClock();
        EndpointDiscoveryCache cache = EndpointDiscoveryCache.create(clock, 10);
        Discovery discovery = new Discovery();
        assertEquals(Optional.empty(), cache.discover(KEY, false, discovery).join());
        assertEquals(1, discovery.calls.get());
        discovery.pending.complete(DiscoveredEndpoint.of("a.test", 5));
        assertEquals(Optional.of(URI.create("https://a.test")), cache.discover(KEY, false, discovery).join());

        clock.advance(Duration.ofMinutes(5));
        discovery.pending = CompletableFuture.failedFuture(new IllegalStateException("refresh failed"));
        assertEquals(Optional.of(URI.create("https://a.test")), cache.discover(KEY, false, discovery).join());
        assertEquals(2, discovery.calls.get());
        assertEquals(Optional.empty(), cache.cached(KEY));
    }

    @Test
    void invalidationAndEviction() {
        EndpointDiscoveryCache cache = EndpointDiscoveryCache.create(new TestClock(), 2);
        for (String table : new String[] {"a", "b", "c"}) {
            EndpointDiscoveryKey key = EndpointDiscoveryKey.of("AKID", "Query", Map.of("Table", table));
            cache.discover(key, true, () -> CompletableFuture.completedFuture(DiscoveredEndpoint.of(table, 10))).join();
        }
        assertEquals(2, cache.size());
        EndpointDiscoveryKey first = EndpointDiscoveryKey.of("AKID", "Query", Map.of("Table", "a"));
        EndpointDiscoveryKey last = EndpointDiscoveryKey.of("AKID", "Query", Map.of("Table", "c"));
        assertEquals(Optional.empty(), cache.cached(first));
        assertEquals(Optional.of(URI.create("https://c")), cache.cached(last));
        cache.invalidate(last);
        assertEquals(Optional.empty(), cache.cached(last));
    }

    @Test
    void keysAreOperationSpecificOnlyWithIdentifiers() {
        assertEquals(EndpointDiscoveryKey.of("AKID", "A", Map.of()), EndpointDiscoveryKey.of("AKID", "B", Map.of()));
        assertFalse(EndpointDiscoveryKey.of("AKID", "A", Map.of("T", "x"))
                                        .equals(EndpointDiscoveryKey.of("AKID", "B", Map.of("T", "x"))));
        assertEquals(URI.create("http://h:1"), DiscoveredEndpoint.of("http://h:1", 1).uri());
        assertThrows(IllegalArgumentException.class, () -> DiscoveredEndpoint.of(" ", 1));
    }
}
