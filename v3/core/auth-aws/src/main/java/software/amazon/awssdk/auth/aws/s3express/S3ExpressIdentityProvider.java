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

package software.amazon.awssdk.auth.aws.s3express;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;

/**
 * Resolves and caches S3 Express session credentials per bucket and base credentials, with the V2 cache semantics:
 * sessions are created with the S3 {@code CreateSession} call the S3 service artifact supplies as a
 * {@link SessionLoader}; a session without an expiration is valid 5 minutes; it is prefetched 60 seconds and goes stale
 * 15 seconds before expiry; the 25 most recently used bucket sessions are kept.
 *
 * <p>The bucket is the {@link #BUCKET} identity property of the auth scheme option. A session the service rejects is
 * {@linkplain #invalidate invalidated} and recreated on the next attempt (Requirement 13.16).
 */
public final class S3ExpressIdentityProvider implements RefreshableIdentityProvider<S3ExpressSessionCredentials> {
    /** The bucket whose session is resolved. Required. */
    public static final IdentityProperty<String> BUCKET = IdentityProperty.create(S3ExpressIdentityProvider.class,
                                                                                  "Bucket");

    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(5);
    private static final Duration STALE = Duration.ofSeconds(15);
    private static final Duration PREFETCH = Duration.ofSeconds(60);
    private static final int MAX_SESSIONS = 25;

    private final SessionLoader sessionLoader;
    private final IdentityProvider<? extends AwsCredentialsIdentity> baseCredentials;
    private final Clock clock;
    private final Executor executor;
    private final Map<SessionKey, IdentityCache<S3ExpressSessionCredentials>> sessions =
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<SessionKey, IdentityCache<S3ExpressSessionCredentials>> e) {
                return size() > MAX_SESSIONS;
            }
        };

    private S3ExpressIdentityProvider(Builder builder) {
        this.sessionLoader = Objects.requireNonNull(builder.sessionLoader, "sessionLoader");
        this.baseCredentials = Objects.requireNonNull(builder.baseCredentials, "baseCredentials");
        this.clock = builder.clock;
        this.executor = builder.executor;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Class<S3ExpressSessionCredentials> identityType() {
        return S3ExpressSessionCredentials.class;
    }

    @Override
    public CompletableFuture<S3ExpressSessionCredentials> resolveIdentity(ResolveIdentityRequest request) {
        String bucket = request.property(BUCKET);
        if (bucket == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("S3 Express identity resolution requires the Bucket identity property."));
        }
        return baseCredentials.resolveIdentity(request)
                              .thenCompose(base -> session(bucket, base).getAsync());
    }

    @Override
    public void invalidate(Identity identity) {
        synchronized (sessions) {
            sessions.values().forEach(cache -> cache.invalidate(identity));
        }
    }

    private IdentityCache<S3ExpressSessionCredentials> session(String bucket, AwsCredentialsIdentity base) {
        SessionKey key = new SessionKey(bucket, base);
        synchronized (sessions) {
            return sessions.computeIfAbsent(key, k -> {
                IdentityCache.Builder<S3ExpressSessionCredentials> cache =
                    IdentityCache.builder("S3ExpressIdentityProvider(" + bucket + ")", () -> load(bucket, base))
                                 .clock(clock);
                if (executor != null) {
                    cache.executor(executor);
                }
                return cache.build();
            });
        }
    }

    private RefreshResult<S3ExpressSessionCredentials> load(String bucket, AwsCredentialsIdentity base) {
        S3ExpressSessionCredentials credentials = Objects.requireNonNull(sessionLoader.createSession(bucket, base),
                                                                         "createSession returned null");
        Instant expiration = credentials.expirationTime().orElseGet(() -> clock.instant().plus(DEFAULT_EXPIRATION));
        return RefreshResult.of(credentials, expiration.minus(STALE), expiration.minus(PREFETCH));
    }

    @Override
    public String toString() {
        return "S3ExpressIdentityProvider";
    }

    /** Creates an S3 Express session: the S3 {@code CreateSession} call, blocking. */
    @FunctionalInterface
    public interface SessionLoader {
        S3ExpressSessionCredentials createSession(String bucket, AwsCredentialsIdentity baseCredentials);
    }

    /** A session cache key: holds credentials, so it is never rendered. */
    private record SessionKey(String bucket, AwsCredentialsIdentity base) {
        @Override
        public String toString() {
            return "SessionKey[" + bucket + "]";
        }
    }

    /** Builder of an {@link S3ExpressIdentityProvider}. */
    public static final class Builder {
        private SessionLoader sessionLoader;
        private IdentityProvider<? extends AwsCredentialsIdentity> baseCredentials;
        private Clock clock = Clock.systemUTC();
        private Executor executor;

        private Builder() {
        }

        /** The {@code CreateSession} call. Required. */
        public Builder sessionLoader(SessionLoader sessionLoader) {
            this.sessionLoader = sessionLoader;
            return this;
        }

        /** The provider of the credentials {@code CreateSession} is signed with. Required. */
        public Builder baseCredentials(IdentityProvider<? extends AwsCredentialsIdentity> baseCredentials) {
            this.baseCredentials = baseCredentials;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /** Where session loads run; a virtual thread per load by default. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public S3ExpressIdentityProvider build() {
            return new S3ExpressIdentityProvider(this);
        }
    }
}
