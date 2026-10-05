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

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The client-scoped cache of endpoints returned by a service's endpoint discovery operation
 * ({@code aws.api#clientEndpointDiscovery}), shared by the operations with {@code aws.api#clientDiscoveredEndpoint}.
 *
 * <p>{@link #discover} never blocks: it returns an already completed future when a usable endpoint is cached, and
 * otherwise the future of one shared discovery call per key.
 * <ul>
 *   <li>A cached endpoint is used until its cache period ends.</li>
 *   <li>When discovery is <em>required</em> for the operation and no unexpired endpoint is cached, the result
 *       completes with the newly discovered endpoint, or exceptionally with the discovery failure (so the call is not
 *       transmitted to an undiscovered endpoint).</li>
 *   <li>When discovery is <em>optional</em>, the result is the cached endpoint, even if expired, or empty (use the
 *       rules-resolved endpoint); a missing or expired entry starts a background refresh whose failure is ignored.</li>
 * </ul>
 * Concurrent requests for one key share one discovery call. {@link #invalidate} drops an entry, for example after
 * the service's modeled invalid-endpoint error. At most {@code maxEntries} keys are kept (least recently written
 * evicted first). Thread-safe.
 */
public final class EndpointDiscoveryCache {
    private final Clock clock;
    private final int maxEntries;
    private final Map<EndpointDiscoveryKey, Entry> entries;
    private final Map<EndpointDiscoveryKey, CompletableFuture<DiscoveredEndpoint>> inFlight = new LinkedHashMap<>();

    private EndpointDiscoveryCache(Clock clock, int maxEntries) {
        this.clock = Objects.requireNonNull(clock, "clock");
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<>(16, 0.75f, false);
    }

    /** A cache of at most {@code maxEntries} keys, reading time from {@code clock}. */
    public static EndpointDiscoveryCache create(Clock clock, int maxEntries) {
        return new EndpointDiscoveryCache(clock, maxEntries);
    }

    /** A cache of at most 1000 keys on the system UTC clock. */
    public static EndpointDiscoveryCache create() {
        return create(Clock.systemUTC(), 1000);
    }

    /**
     * The discovered endpoint to use for a call with cache key {@code key}.
     *
     * @param required whether the operation requires a discovered endpoint
     * @param discovery starts one call of the discovery operation for {@code key}; invoked at most once per
     *        concurrent miss, never while a lock is held
     * @return the endpoint URI, or empty (optional discovery only) to use the rules-resolved endpoint
     */
    public CompletableFuture<Optional<URI>> discover(EndpointDiscoveryKey key, boolean required,
                                                     Supplier<CompletableFuture<DiscoveredEndpoint>> discovery) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(discovery, "discovery");
        Instant now = clock.instant();
        Entry cached;
        synchronized (this) {
            cached = entries.get(key);
        }
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return CompletableFuture.completedFuture(Optional.of(cached.uri()));
        }
        CompletableFuture<DiscoveredEndpoint> refresh = refresh(key, discovery);
        if (required) {
            return refresh.thenApply(endpoint -> Optional.of(endpoint.uri()));
        }
        return CompletableFuture.completedFuture(cached == null ? Optional.empty() : Optional.of(cached.uri()));
    }

    /** Drops the cached endpoint of {@code key}, if any. */
    public synchronized void invalidate(EndpointDiscoveryKey key) {
        entries.remove(key);
    }

    /** The unexpired cached endpoint of {@code key}, if any. */
    public Optional<URI> cached(EndpointDiscoveryKey key) {
        Entry entry;
        synchronized (this) {
            entry = entries.get(key);
        }
        return entry != null && clock.instant().isBefore(entry.expiresAt()) ? Optional.of(entry.uri())
                                                                             : Optional.empty();
    }

    /** The number of cached keys. */
    public synchronized int size() {
        return entries.size();
    }

    private CompletableFuture<DiscoveredEndpoint> refresh(EndpointDiscoveryKey key,
                                                          Supplier<CompletableFuture<DiscoveredEndpoint>> discovery) {
        CompletableFuture<DiscoveredEndpoint> shared;
        boolean start = false;
        synchronized (this) {
            shared = inFlight.get(key);
            if (shared == null) {
                shared = new CompletableFuture<>();
                inFlight.put(key, shared);
                start = true;
            }
        }
        if (!start) {
            return shared;
        }
        CompletableFuture<DiscoveredEndpoint> result = shared;
        CompletableFuture<DiscoveredEndpoint> call;
        try {
            call = Objects.requireNonNull(discovery.get(), "The endpoint discovery call returned no future.");
        } catch (RuntimeException e) {
            call = CompletableFuture.failedFuture(e);
        }
        call.whenComplete((endpoint, failure) -> {
            synchronized (this) {
                inFlight.remove(key);
                if (failure == null && endpoint != null) {
                    entries.remove(key);
                    entries.put(key, new Entry(endpoint.uri(), clock.instant().plus(endpoint.cachePeriod())));
                    while (entries.size() > maxEntries) {
                        entries.remove(entries.keySet().iterator().next());
                    }
                }
            }
            if (failure != null) {
                result.completeExceptionally(failure);
            } else if (endpoint == null) {
                result.completeExceptionally(new IllegalStateException("The endpoint discovery call returned no "
                                                                       + "endpoint."));
            } else {
                result.complete(endpoint);
            }
        });
        return result;
    }

    private record Entry(URI uri, Instant expiresAt) {
    }
}
