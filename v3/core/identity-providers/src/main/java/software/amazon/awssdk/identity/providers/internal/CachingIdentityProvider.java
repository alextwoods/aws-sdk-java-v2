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

package software.amazon.awssdk.identity.providers.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;

/**
 * Base of the providers that load temporary identities and cache them with an {@link IdentityCache}: resolution returns
 * the cached identity or waits for a refresh, {@link #invalidate} discards a rejected identity, {@link #close} releases
 * the provider's transport. Not a public API.
 *
 * @param <T> the identity type
 */
public abstract class CachingIdentityProvider<T extends Identity>
    implements RefreshableIdentityProvider<T>, AutoCloseable {
    private final IdentityCache<T> cache;
    private final Class<T> identityType;
    protected final Clock clock;

    protected CachingIdentityProvider(Class<T> identityType, String name, Clock clock, Executor executor,
                                      IdentityCache.StaleValueBehavior staleValueBehavior) {
        this.identityType = identityType;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        IdentityCache.Builder<T> builder = IdentityCache.builder(name, this::load)
                                                        .clock(this.clock)
                                                        .staleValueBehavior(staleValueBehavior);
        if (executor != null) {
            builder.executor(executor);
        }
        this.cache = builder.build();
    }

    /** Loads a new identity with its stale and prefetch times; blocking. */
    protected abstract RefreshResult<T> load();

    @Override
    public final Class<T> identityType() {
        return identityType;
    }

    @Override
    public CompletableFuture<T> resolveIdentity(ResolveIdentityRequest request) {
        return cache.getAsync();
    }

    @Override
    public void invalidate(Identity identity) {
        cache.invalidate(identity);
    }

    /** Releases the provider's resources. Idempotent. */
    @Override
    public void close() {
    }

    /** Stale one minute and prefetched five minutes before {@code expiration} (V2 STS and SSO). */
    public static <T> RefreshResult<T> stsStyle(T value, Instant expiration) {
        if (expiration == null) {
            return RefreshResult.of(value);
        }
        return RefreshResult.of(value, expiration.minus(Duration.ofMinutes(1)), expiration.minus(Duration.ofMinutes(5)));
    }
}
