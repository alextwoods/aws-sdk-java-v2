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

package software.amazon.awssdk.test.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.id;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.s;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.providers.ContainerCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;

/**
 * Concurrency suite of identity resolution under simultaneous refresh (Requirements 13.16, 13.17 via 19.9, 19.10):
 * {@value #SIMULTANEOUS} simultaneous resolutions, {@value #TOTAL} in total, one expiry per round, a bounded
 * {@value #TIMEOUT_SECONDS}-second run. Before each round the test clock moves past the cached identity's stale (or
 * prefetch) time; the round's load is then held until all {@value #SIMULTANEOUS} callers hold a pending resolution
 * (non-blocking {@code getAsync}/{@code resolveIdentity} returned), which is the overlap the suite requires. Exactly
 * one load must run per expiry, and every caller must receive that load's identity.
 */
class IdentityResolverConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;
    static final Instant BASE = Instant.parse("2030-01-01T00:00:00Z");

    private final MutableClock clock = new MutableClock(BASE);
    private final ExecutorService loads = Clients.completionExecutor("identity-load", 2);
    private final AtomicInteger loadCount = new AtomicInteger();
    private final AtomicReference<CountDownLatch> allPending = new AtomicReference<>();
    private final Queue<String> problems = new ConcurrentLinkedQueue<>();

    @AfterEach
    void shutdown() {
        Clients.shutdown(loads);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void identityCache_simultaneousCallersAtEachExpiry_shareExactlyOneLoad() {
        ConcurrencyPlan plan = plan("identity-cache-refresh");
        IdentityCache<String> cache = IdentityCache.<String>builder("concurrency", () -> {
            int load = loadCount.getAndIncrement();
            awaitAllPending();
            // Stale half an hour after loading; each round starts an hour after the previous one.
            return RefreshResult.of("identity-" + load, clock.instant().plus(Duration.ofMinutes(30)), null);
        }).clock(clock).executor(loads).build();

        List<String> identities = ConcurrentRun.run(plan, this::startRound, call -> {
            CompletableFuture<String> identity = cache.getAsync();
            allPending.get().countDown();
            return identity.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }, this::releaseLoads);

        assertEquals(plan.rounds(), loadCount.get(), "exactly one load per expiry");
        for (int i = 0; i < TOTAL; i++) {
            assertEquals("identity-" + i / SIMULTANEOUS, identities.get(i), "call " + i);
        }
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void identityCache_simultaneousCallersInThePrefetchWindow_startExactlyOneBackgroundRefresh() {
        ConcurrencyPlan plan = plan("identity-cache-prefetch");
        // Completed once a load task (including the cache's bookkeeping after the loader returns) has finished.
        AtomicReference<CompletableFuture<Void>> loaded = new AtomicReference<>(new CompletableFuture<>());
        IdentityCache<String> cache = IdentityCache.<String>builder("concurrency", () -> {
            int load = loadCount.getAndIncrement();
            awaitAllPending();
            Instant now = clock.instant();
            // Prefetched 20 minutes and stale 2 hours after loading: a round an hour later is in the prefetch window.
            return RefreshResult.of("identity-" + load, now.plus(Duration.ofHours(2)), now.plus(Duration.ofMinutes(20)));
        }).clock(clock).executor(task -> {
            CompletableFuture<Void> ofThisRound = loaded.get();
            loads.execute(() -> {
                try {
                    task.run();
                } finally {
                    ofThisRound.complete(null);
                }
            });
        }).build();

        List<String> identities = ConcurrentRun.run(plan, round -> {
            if (round > 0) {
                // The previous round's background refresh must have finished, so the next window is its value's.
                awaitQuietly(loaded.get());
            }
            loaded.set(new CompletableFuture<>());
            startRound(round);
        }, call -> {
            CompletableFuture<String> identity = cache.getAsync();
            allPending.get().countDown();
            return identity.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }, this::releaseLoads);
        // The last window's background refresh may still be finishing.
        awaitQuietly(loaded.get());

        assertEquals(plan.rounds(), loadCount.get(), "the first required load, then one prefetch per window");
        for (int i = 0; i < TOTAL; i++) {
            int round = i / SIMULTANEOUS;
            // Round 0 waits for the first load; later rounds keep the cached value while it is prefetched.
            assertEquals("identity-" + Math.max(0, round - 1), identities.get(i), "call " + i);
        }
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void cachingProviderOfASharedClient_simultaneousCallsAtEachExpiry_fetchAndSignWithOneIdentity() {
        ConcurrencyPlan plan = plan("container-provider-refresh");
        ContainerEndpoint endpoint = new ContainerEndpoint();
        SystemSettings settings = new SystemSettings(SettingSource.fromMap(Map.of()), SettingSource.fromMap(
            Map.of("AWS_CONTAINER_CREDENTIALS_FULL_URI", "http://127.0.0.1/credentials")));
        FakeDynamoDb http = FakeDynamoDb.sync();
        try (ContainerCredentialsProvider provider = ContainerCredentialsProvider.builder().settings(settings)
                                                                                  .httpClient(endpoint).clock(clock)
                                                                                  .executor(loads).build();
             DynamoDbClient client = Clients.sync(http).credentialsProvider(new PendingCounter(provider)).build()) {
            ConcurrentRun.run(plan, this::startRound, call -> {
                client.getItem(r -> r.tableName("t").key(Map.of("id", s(id(call)))));
                return null;
            }, this::releaseLoads);
        } finally {
            http.shutdown();
        }

        assertEquals(plan.rounds(), endpoint.fetches.get(), "one credentials fetch per expiry");
        assertEquals(TOTAL, http.count("GetItem"));
        http.received().forEach(r -> {
            int round = Integer.parseInt(r.id().substring("call-".length())) / SIMULTANEOUS;
            String authorization = r.header("Authorization");
            if (!authorization.contains("Credential=AKID" + round + "/")) {
                problems.add(r.id() + " (round " + round + ") was signed with " + authorization);
            }
            if (!("token-" + round).equals(r.header("X-Amz-Security-Token"))) {
                problems.add(r.id() + " (round " + round + ") carried token " + r.header("X-Amz-Security-Token"));
            }
        });
        assertEquals(List.of(), List.copyOf(problems));
    }

    private static ConcurrencyPlan plan(String component) {
        return ConcurrencyPlan.of(component, SIMULTANEOUS, TOTAL, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    /** Moves the clock to the round's time (an hour per round) and arms the round's overlap latch. */
    private void startRound(int round) {
        clock.set(BASE.plus(Duration.ofHours(round)));
        allPending.set(new CountDownLatch(SIMULTANEOUS));
    }

    /** Holds a load until every caller of the round holds a pending resolution. */
    private void awaitAllPending() {
        try {
            if (!allPending.get().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("not every caller of the round reached the pending resolution");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void releaseLoads() {
        CountDownLatch latch = allPending.get();
        while (latch != null && latch.getCount() > 0) {
            latch.countDown();
        }
    }

    private static void awaitQuietly(CompletableFuture<Void> future) {
        try {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("the background refresh did not finish", e);
        }
    }

    /** Counts a caller as pending once the provider handed it the (possibly still loading) identity future. */
    private final class PendingCounter implements IdentityProvider<AwsCredentialsIdentity> {
        private final IdentityProvider<AwsCredentialsIdentity> delegate;

        PendingCounter(IdentityProvider<AwsCredentialsIdentity> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return delegate.identityType();
        }

        @Override
        public CompletableFuture<? extends AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            CompletableFuture<? extends AwsCredentialsIdentity> identity = delegate.resolveIdentity(request);
            allPending.get().countDown();
            return identity;
        }
    }

    /** The container credentials endpoint: round-numbered credentials expiring an hour after the clock's time. */
    private final class ContainerEndpoint implements SdkSyncHttpClient {
        final AtomicInteger fetches = new AtomicInteger();

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    int fetch = fetches.getAndIncrement();
                    awaitAllPending();
                    String body = "{\"AccessKeyId\":\"AKID" + fetch + "\",\"SecretAccessKey\":\"secret-" + fetch
                                  + "\",\"Token\":\"token-" + fetch + "\",\"Expiration\":\""
                                  + clock.instant().plus(Duration.ofMinutes(50)) + "\"}";
                    return SdkHttpResponse.builder().statusCode(200)
                                          .body(HttpBody.fromBytes(body.getBytes(StandardCharsets.UTF_8))).build();
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 16)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "ContainerEndpoint";
        }

        @Override
        public void close() {
        }
    }

    /** A settable clock shared by the cache, the provider and the credentials endpoint. */
    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        MutableClock(Instant start) {
            this.now = new AtomicReference<>(start);
        }

        void set(Instant instant) {
            now.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
