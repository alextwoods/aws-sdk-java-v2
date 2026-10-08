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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import software.amazon.awssdk.identity.spi.IdentityRefreshException;
import software.amazon.awssdk.utils.ToString;

/**
 * A single-value cache of refreshable identity material with the V2 {@code CachedSupplier} stale/prefetch semantics
 * and the V3 refresh contract (Requirements 13.16, 13.17, 13.19).
 *
 * <ul>
 *   <li>While the cached value is fresh it is returned without calling the loader.</li>
 *   <li>From its {@linkplain RefreshResult#prefetchTime() prefetch time} a call still returns it, and starts at most one
 *       background refresh; a failed prefetch keeps the value.</li>
 *   <li>With no value, from its {@linkplain RefreshResult#staleTime() stale time}, or after {@link #invalidate} discarded
 *       it, a call waits for a refresh. Concurrent callers share one load. The next value is the refreshed one.</li>
 *   <li>If a required refresh fails, the call fails with an {@link IdentityRefreshException} whose cause is the load
 *       failure, and the old value is not returned. The exception is {@link StaleValueBehavior#ALLOW}, used by the
 *       instance-metadata provider for V2 static stability: a stale (but not invalidated) value is then kept for a
 *       short jittered period after a failed refresh.</li>
 * </ul>
 *
 * <p>Loads are blocking and run on the configured executor (by default a virtual thread per load), so
 * {@link #getAsync()} never blocks its caller, including on an event loop. Thread-safe.
 *
 * @param <T> the cached value type
 */
public final class IdentityCache<T> {
    private static final ExecutorService DEFAULT_EXECUTOR =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("aws-sdk-identity-refresh-", 0).factory());

    private final String name;
    private final Supplier<RefreshResult<T>> loader;
    private final Clock clock;
    private final Executor executor;
    private final StaleValueBehavior staleValueBehavior;
    private final RandomGenerator random;
    private final AtomicReference<Cached<T>> cached = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<RefreshResult<T>>> inFlight = new AtomicReference<>();
    private final AtomicInteger consecutiveStaleFailures = new AtomicInteger();

    private IdentityCache(Builder<T> builder) {
        this.name = Objects.requireNonNull(builder.name, "name");
        this.loader = Objects.requireNonNull(builder.loader, "loader");
        this.clock = builder.clock;
        this.executor = builder.executor;
        this.staleValueBehavior = builder.staleValueBehavior;
        this.random = builder.random;
    }

    /** A builder of a cache named {@code name} (used in diagnostics) that loads values with {@code loader}. */
    public static <T> Builder<T> builder(String name, Supplier<RefreshResult<T>> loader) {
        return new Builder<T>().name(name).loader(loader);
    }

    /** The current value, refreshing it first when required; never blocks the caller. */
    public CompletableFuture<T> getAsync() {
        Cached<T> current = cached.get();
        Instant now = clock.instant();
        if (current != null && !current.invalidated && !isStale(current.result, now)) {
            if (current.result.prefetchTime().map(p -> !now.isBefore(p)).orElse(false)) {
                refresh(current);
            }
            return CompletableFuture.completedFuture(current.result.value());
        }
        return refresh(current).thenApply(RefreshResult::value);
    }

    /**
     * The current value, refreshing it first when required; blocks while a required refresh runs.
     *
     * @throws IdentityRefreshException if a required refresh fails
     */
    public T get() {
        try {
            return getAsync().get();
        } catch (ExecutionException e) {
            throw unwrap(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw IdentityRefreshException.create(name, e);
        }
    }

    /**
     * Discards the cached value if it equals {@code value}, so the next call refreshes and never returns it again.
     *
     * @return whether the cached value was discarded
     */
    public boolean invalidate(Object value) {
        while (true) {
            Cached<T> current = cached.get();
            if (current == null || current.invalidated || !current.result.value().equals(value)) {
                return false;
            }
            if (cached.compareAndSet(current, new Cached<>(current.result, true))) {
                return true;
            }
        }
    }

    private boolean isStale(RefreshResult<T> result, Instant now) {
        return result.staleTime().map(stale -> !now.isBefore(stale)).orElse(false);
    }

    /**
     * Starts one load unless one is running, and returns the running load. {@code observed} is the cached entry the
     * caller decided on: if a load finished after the caller read it and cached a usable value, that value is
     * returned instead of loading again, so one expiry (or prefetch window) causes one load.
     */
    private CompletableFuture<RefreshResult<T>> refresh(Cached<T> observed) {
        CompletableFuture<RefreshResult<T>> load;
        while (true) {
            CompletableFuture<RefreshResult<T>> running = inFlight.get();
            if (running != null) {
                return running;
            }
            load = new CompletableFuture<>();
            if (inFlight.compareAndSet(null, load)) {
                break;
            }
        }
        CompletableFuture<RefreshResult<T>> started = load;
        Cached<T> latest = cached.get();
        if (latest != observed && latest != null && !latest.invalidated && !isStale(latest.result, clock.instant())) {
            inFlight.compareAndSet(started, null);
            started.complete(latest.result);
            return started;
        }
        try {
            executor.execute(() -> runLoad(started));
        } catch (RuntimeException e) {
            inFlight.compareAndSet(started, null);
            started.completeExceptionally(IdentityRefreshException.create(name, e));
        }
        return started;
    }

    private void runLoad(CompletableFuture<RefreshResult<T>> load) {
        try {
            RefreshResult<T> result = handleSuccess(Objects.requireNonNull(loader.get(), "The loader returned null"));
            cached.set(new Cached<>(result, false));
            inFlight.compareAndSet(load, null);
            load.complete(result);
        } catch (RuntimeException | Error e) {
            RefreshResult<T> kept = handleFailure(e);
            inFlight.compareAndSet(load, null);
            if (kept != null) {
                load.complete(kept);
            } else {
                load.completeExceptionally(e instanceof IdentityRefreshException ? e
                                                                                 : IdentityRefreshException.create(name, e));
            }
        }
    }

    private RefreshResult<T> handleSuccess(RefreshResult<T> result) {
        consecutiveStaleFailures.set(0);
        Instant now = clock.instant();
        if (!isStale(result, now)) {
            return result;
        }
        // The source returned a value that is already stale.
        return switch (staleValueBehavior) {
            case STRICT -> result.withStaleTime(now.plusSeconds(1));
            case ALLOW -> result.withStaleTime(jitter(now, Duration.ofMinutes(1), Duration.ofMinutes(10)));
        };
    }

    /** The value to keep after a failed load, or null if the load failure must be reported. */
    private RefreshResult<T> handleFailure(Throwable failure) {
        Cached<T> current = cached.get();
        if (current == null || current.invalidated) {
            return null;
        }
        Instant now = clock.instant();
        if (!isStale(current.result, now)) {
            return current.result;
        }
        if (staleValueBehavior == StaleValueBehavior.STRICT || failure instanceof Error) {
            return null;
        }
        int failures = consecutiveStaleFailures.incrementAndGet();
        long maxMillis = Math.min(Duration.ofSeconds(10).toMillis(), 100L << Math.min(failures, 10));
        RefreshResult<T> extended = current.result.withStaleTime(jitter(now, Duration.ofMillis(1),
                                                                          Duration.ofMillis(Math.max(2, maxMillis))));
        cached.compareAndSet(current, new Cached<>(extended, false));
        return extended;
    }

    private Instant jitter(Instant from, Duration min, Duration max) {
        long range = Math.max(1, max.minus(min).toMillis());
        return from.plus(min).plusMillis(random.nextLong(range));
    }

    private RuntimeException unwrap(Throwable cause) {
        Throwable t = cause instanceof CompletionException && cause.getCause() != null ? cause.getCause() : cause;
        if (t instanceof RuntimeException e) {
            return e;
        }
        if (t instanceof Error e) {
            throw e;
        }
        return IdentityRefreshException.create(name, t);
    }

    @Override
    public String toString() {
        return ToString.builder("IdentityCache").add("name", name).add("staleValueBehavior", staleValueBehavior).build();
    }

    /** What a cache does when a refresh of a stale value fails. */
    public enum StaleValueBehavior {
        /** Fail: a stale value is never returned. The default. */
        STRICT,
        /** Keep the stale value for a short jittered period (V2 IMDS static stability). */
        ALLOW
    }

    private record Cached<T>(RefreshResult<T> result, boolean invalidated) {
    }

    /** Builder of an {@link IdentityCache}. */
    public static final class Builder<T> {
        private String name;
        private Supplier<RefreshResult<T>> loader;
        private Clock clock = Clock.systemUTC();
        private Executor executor = DEFAULT_EXECUTOR;
        private StaleValueBehavior staleValueBehavior = StaleValueBehavior.STRICT;
        private RandomGenerator random = RandomGenerator.getDefault();

        private Builder() {
        }

        /** The name diagnostics identify the cache by. */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /** The blocking loader of a new value. */
        public Builder<T> loader(Supplier<RefreshResult<T>> loader) {
            this.loader = loader;
            return this;
        }

        /** The clock stale and prefetch times are compared with; the system UTC clock by default. */
        public Builder<T> clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /** Where loads run; a new virtual thread per load by default. */
        public Builder<T> executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        /** What to do when refreshing a stale value fails; {@link StaleValueBehavior#STRICT} by default. */
        public Builder<T> staleValueBehavior(StaleValueBehavior staleValueBehavior) {
            this.staleValueBehavior = Objects.requireNonNull(staleValueBehavior, "staleValueBehavior");
            return this;
        }

        /** The jitter source; for deterministic tests. */
        public Builder<T> random(RandomGenerator random) {
            this.random = Objects.requireNonNull(random, "random");
            return this;
        }

        public IdentityCache<T> build() {
            return new IdentityCache<>(this);
        }
    }
}
