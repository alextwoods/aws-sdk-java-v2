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

package software.amazon.awssdk.http.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkCloseException;
import software.amazon.awssdk.core.resource.ResourceOwnership;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException.Reason;

/**
 * HTTP client resolution (Requirements 12.11-12.14): explicit precedence, highest-priority default selection
 * independent of order, tied-priority and missing-mode failures, option validation before construction, and rollback
 * of the SDK-owned resources a failed construction acquired.
 */
class HttpClientResolverTest {
    private static final Supplier<InstalledHttpImplementations> NO_DISCOVERY = () -> {
        throw new AssertionError("discovery must not run");
    };

    // Explicit precedence.

    @Test
    void anExplicitClientWinsIsCallerOwnedAndSkipsDiscovery() {
        FakeSyncClient explicit = new FakeSyncClient("explicit", caps(HttpTransportMode.SYNC));
        ResolvedHttpClient<SdkSyncHttpClient> resolved = HttpClientResolver.sync().client(explicit)
                                                                           .installed(NO_DISCOVERY).resolve();
        assertSame(explicit, resolved.client());
        assertEquals("explicit", resolved.implementationName());
        assertEquals(ResourceOwnership.CALLER_OWNED, resolved.ownership());
        resolved.close();
        assertEquals(0, explicit.closed.get(), "a caller-owned client is never closed by the SDK");
    }

    @Test
    void anExplicitImplementationWinsOverAHigherPriorityInstalledOne() {
        FakeSyncService installed = new FakeSyncService("installed", 100, caps(HttpTransportMode.SYNC));
        FakeSyncService explicit = new FakeSyncService("explicit", 1, caps(HttpTransportMode.SYNC));
        AtomicInteger discoveries = new AtomicInteger();
        ResolvedHttpClient<SdkSyncHttpClient> resolved =
            HttpClientResolver.sync().implementation(explicit)
                              .installed(() -> {
                                  discoveries.incrementAndGet();
                                  return InstalledHttpImplementations.of(installed);
                              })
                              .resolve();
        assertEquals("explicit", resolved.implementationName());
        assertEquals(ResourceOwnership.SDK_OWNED, resolved.ownership());
        assertEquals(0, discoveries.get());
        assertEquals(0, installed.creates.get());
    }

    @Test
    void configuringBothAClientAndAnImplementationFails() {
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().client(new FakeSyncClient("c", caps(HttpTransportMode.SYNC)))
                                    .implementation(new FakeSyncService("s", 0, caps(HttpTransportMode.SYNC)))
                                    .installed(NO_DISCOVERY).resolve());
        assertEquals(Reason.EXPLICIT_CONFLICT, e.reason());
        assertEquals(List.of("c", "s"), e.implementations());
        assertTrue(e.getMessage().contains("httpClient and httpClientBuilder"), e.getMessage());
    }

    // Default selection.

    @Test
    void theHighestPriorityOfTheModeIsSelectedInEveryOrder() {
        List<HttpImplementation<?>> implementations = List.of(
            new FakeSyncService("low", 1, caps(HttpTransportMode.SYNC)),
            new FakeSyncService("high", 5, caps(HttpTransportMode.SYNC)),
            new FakeSyncService("middle", 3, caps(HttpTransportMode.SYNC)),
            new FakeAsyncService("async-highest", 100, caps(HttpTransportMode.ASYNC)));
        for (List<HttpImplementation<?>> order : permutations(implementations)) {
            InstalledHttpImplementations installed = InstalledHttpImplementations.of(order);
            assertEquals(List.of("high", "low", "middle"), installed.names(HttpTransportMode.SYNC), order::toString);
            assertEquals("high", installed.defaultSync().implementationName(), order::toString);
            assertEquals("async-highest", installed.defaultAsync().implementationName(), order::toString);
            assertEquals("high", HttpClientResolver.sync().installed(() -> installed).resolve().implementationName());
        }
    }

    @Test
    void aTopPriorityTieFailsNamingEveryTiedImplementationSortedInEveryOrder() {
        List<HttpImplementation<?>> implementations = List.of(
            new FakeSyncService("c", 5, caps(HttpTransportMode.SYNC)),
            new FakeSyncService("a", 5, caps(HttpTransportMode.SYNC)),
            new FakeSyncService("b", 1, caps(HttpTransportMode.SYNC)),
            new FakeSyncService("d", 5, caps(HttpTransportMode.SYNC)));
        for (List<HttpImplementation<?>> order : permutations(implementations)) {
            HttpImplementationSelectionException e = assertThrows(
                HttpImplementationSelectionException.class,
                () -> HttpClientResolver.sync().installed(() -> InstalledHttpImplementations.of(order)).resolve());
            assertEquals(Reason.TIED_PRIORITY, e.reason());
            assertEquals(Optional.of(HttpTransportMode.SYNC), e.mode());
            assertEquals(List.of("a", "c", "d"), e.implementations());
            assertEquals(Optional.of("httpClient"), e.option());
            assertTrue(e.getMessage().startsWith("3 installed sync HTTP implementations share the highest priority 5: "
                                                 + "a (" + FakeSyncService.class.getName() + "), c ("),
                       e.getMessage());
            assertTrue(!e.getMessage().contains("b ("), e.getMessage());
        }
        implementations.forEach(i -> assertEquals(0, ((FakeSyncService) i).creates.get(), "nothing is created"));
    }

    @Test
    void noImplementationOfTheModeFailsWithTheOtherModesImplementations() {
        InstalledHttpImplementations installed =
            InstalledHttpImplementations.of(new FakeAsyncService("netty", 1, caps(HttpTransportMode.ASYNC)));
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().installed(() -> installed).resolve());
        assertEquals(Reason.NO_IMPLEMENTATION, e.reason());
        assertEquals(Optional.of(HttpTransportMode.SYNC), e.mode());
        assertEquals(Optional.of("httpClient"), e.option());
        assertEquals("No sync HTTP implementation is installed (installed async implementations: [netty]). Add a "
                     + "sync HTTP client module to the class path, or configure httpClient or httpClientBuilder "
                     + "explicitly.", e.getMessage());

        HttpImplementationSelectionException none = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.async().installed(InstalledHttpImplementations::none).resolve());
        assertEquals(Reason.NO_IMPLEMENTATION, none.reason());
        assertEquals(Optional.of("asyncHttpClient"), none.option());
    }

    @Test
    void asyncResolutionUsesTheAsyncSpi() {
        FakeAsyncService netty = new FakeAsyncService("netty", 1, caps(HttpTransportMode.ASYNC));
        ResolvedHttpClient<SdkAsyncHttpClient> resolved =
            HttpClientResolver.async().installed(() -> InstalledHttpImplementations.of(netty)).resolve();
        assertEquals("netty", resolved.client().clientName());
        FakeAsyncClient explicit = new FakeAsyncClient("explicit", caps(HttpTransportMode.ASYNC));
        assertSame(explicit, HttpClientResolver.async().client(explicit).installed(NO_DISCOVERY).resolve().client());
    }

    // Metadata validation.

    @Test
    void invalidMetadataFailsInstallation() {
        HttpImplementationSelectionException wrongMode = assertThrows(
            HttpImplementationSelectionException.class,
            () -> InstalledHttpImplementations.of(new FakeSyncService("x", 0, caps(HttpTransportMode.ASYNC))));
        assertEquals(Reason.INVALID_IMPLEMENTATION, wrongMode.reason());
        assertTrue(wrongMode.getMessage().contains("installed as sync but declares mode SYNC with ASYNC"),
                   wrongMode.getMessage());

        HttpImplementationSelectionException duplicate = assertThrows(
            HttpImplementationSelectionException.class,
            () -> InstalledHttpImplementations.of(new FakeSyncService("x", 0, caps(HttpTransportMode.SYNC)),
                                                  new FakeSyncService("x", 1, caps(HttpTransportMode.SYNC))));
        assertEquals(Reason.INVALID_IMPLEMENTATION, duplicate.reason());
        assertEquals(List.of("x"), duplicate.implementations());

        HttpImplementationSelectionException blank = assertThrows(
            HttpImplementationSelectionException.class,
            () -> InstalledHttpImplementations.of(new FakeSyncService(" ", 0, caps(HttpTransportMode.SYNC))));
        assertEquals(Reason.INVALID_IMPLEMENTATION, blank.reason());
    }

    // Option validation.

    @Test
    void everyUnsupportedOptionFailsBeforeCreateNamingOptionImplementationAndMode() {
        FakeSyncService service = new FakeSyncService("url-connection", 0,
                                                      caps(HttpTransportMode.SYNC, HttpClientOption.CONNECTION_TIMEOUT));
        HttpClientConfiguration configuration = HttpClientConfiguration.builder()
            .put(HttpClientOption.MAX_CONNECTIONS, 10)
            .put(HttpClientOption.CONNECTION_TIMEOUT, Duration.ofSeconds(1))
            .put(HttpClientOption.PROXY, "proxy")
            .build();
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().implementation(service).configuration(configuration).resolve());
        assertEquals(Reason.UNSUPPORTED_OPTION, e.reason());
        assertEquals(List.of(HttpClientOption.PROXY, HttpClientOption.MAX_CONNECTIONS), e.unsupportedOptions());
        assertEquals(List.of("url-connection"), e.implementations());
        assertEquals(Optional.empty(), e.option(), "no single option is at fault");
        assertEquals("The sync HTTP implementation url-connection does not support the configured options "
                     + "proxyConfiguration, maxConnections. Remove them or configure an HTTP client that supports "
                     + "them.", e.getMessage());
        assertEquals(0, service.creates.get(), "nothing is constructed or acquired");

        HttpImplementationSelectionException one = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().implementation(service)
                                    .configuration(HttpClientConfiguration.builder()
                                                                          .put(HttpClientOption.PROXY, "p").build())
                                    .resolve());
        assertEquals(Optional.of("proxyConfiguration"), one.option());
        assertTrue(one.getMessage().contains("option proxyConfiguration. Remove it"), one.getMessage());
    }

    @Test
    void anExplicitClientIsCheckedAndNotClosedWhenItFails() {
        FakeAsyncClient explicit = new FakeAsyncClient("explicit", caps(HttpTransportMode.ASYNC));
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.async().client(explicit)
                                    .configuration(HttpClientConfiguration.builder()
                                                                          .put(HttpClientOption.HTTP2_MAX_STREAMS, 4)
                                                                          .build())
                                    .installed(NO_DISCOVERY).resolve());
        assertEquals(Reason.UNSUPPORTED_OPTION, e.reason());
        assertEquals(Optional.of(HttpTransportMode.ASYNC), e.mode());
        assertTrue(e.getMessage().startsWith("The async HTTP client explicit does not support the "
                                             + "configured option http2Configuration.maxStreams."), e.getMessage());
        assertEquals(0, explicit.closed.get());
    }

    @Test
    void theConfiguredValuesReachTheImplementation() {
        AtomicReference<HttpClientConstruction> seen = new AtomicReference<>();
        FakeSyncService service = new FakeSyncService("apache", 0,
                                                      caps(HttpTransportMode.SYNC, HttpClientOption.READ_TIMEOUT),
                                                      c -> {
                                                          seen.set(c);
                                                          return new FakeSyncClient("apache", caps(HttpTransportMode.SYNC,
                                                                                                   HttpClientOption.READ_TIMEOUT));
                                                      });
        HttpClientResolver.sync().implementation(service)
                          .configuration(HttpClientConfiguration.builder()
                                                                .put(HttpClientOption.READ_TIMEOUT, Duration.ofSeconds(2))
                                                                .build())
                          .resolve();
        assertEquals(Optional.of(Duration.ofSeconds(2)),
                     seen.get().configuration().value(HttpClientOption.READ_TIMEOUT, Duration.class));
        assertEquals("apache", seen.get().implementationName());
        assertEquals(HttpTransportMode.SYNC, seen.get().mode());
        assertThrows(IllegalArgumentException.class,
                     () -> seen.get().configuration().value(HttpClientOption.READ_TIMEOUT, String.class));
    }

    // Construction rollback.

    @Test
    void aFailedCreateReleasesSdkOwnedResourcesInReverseAndSuppressesCloseFailures() {
        List<String> closes = new ArrayList<>();
        RuntimeException closeFailure = new RuntimeException("event loop close failed");
        IllegalStateException createFailure = new IllegalStateException("native library missing");
        Tracked callerExecutor = new Tracked("caller-executor", closes, null);
        FakeSyncService service = new FakeSyncService("crt", 0, caps(HttpTransportMode.SYNC), c -> {
            c.sdkOwned(new Tracked("thread-pool", closes, null));
            c.callerOwned(callerExecutor);
            c.sdkOwned(new Tracked("event-loop", closes, closeFailure));
            c.sdkOwned(new Tracked("connection-manager", closes, null));
            throw createFailure;
        });
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                                                     () -> HttpClientResolver.sync().implementation(service).resolve());
        assertSame(createFailure, thrown);
        assertEquals(List.of("connection-manager", "event-loop", "thread-pool"), closes);
        assertEquals(1, thrown.getSuppressed().length);
        SdkCloseException aggregated = assertInstanceOf(SdkCloseException.class, thrown.getSuppressed()[0]);
        assertEquals(List.of(closeFailure), aggregated.failures());
    }

    @Test
    void aBuiltClientThatDoesNotSupportAConfiguredOptionIsRolledBackWithItsResources() {
        List<String> closes = new ArrayList<>();
        Tracked callerProxy = new Tracked("caller-proxy", closes, null);
        AtomicReference<FakeSyncClient> built = new AtomicReference<>();
        // The metadata claims proxy support; the client built on this platform lacks it.
        FakeSyncService service = new FakeSyncService("crt", 0, caps(HttpTransportMode.SYNC, HttpClientOption.PROXY),
                                                      c -> {
                                                          c.sdkOwned(new Tracked("native-handle", closes, null));
                                                          c.callerOwned(callerProxy);
                                                          FakeSyncClient client = new FakeSyncClient(
                                                              "crt", caps(HttpTransportMode.SYNC), closes);
                                                          built.set(client);
                                                          return client;
                                                      });
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().implementation(service)
                                    .configuration(HttpClientConfiguration.builder()
                                                                          .put(HttpClientOption.PROXY, "p").build())
                                    .resolve());
        assertEquals(Reason.UNSUPPORTED_OPTION, e.reason());
        assertTrue(e.getMessage().contains("HTTP client crt"), e.getMessage());
        assertEquals(List.of("client:crt", "native-handle"), closes, "the client first, then its resources");
        assertEquals(1, built.get().closed.get());
        assertEquals(0, e.getSuppressed().length);
    }

    @Test
    void aSuccessfulConstructionKeepsResourcesUntilCloseAndRejectsLateRegistration() {
        List<String> closes = new ArrayList<>();
        AtomicReference<HttpClientConstruction> construction = new AtomicReference<>();
        FakeSyncService service = new FakeSyncService("apache", 0, caps(HttpTransportMode.SYNC), c -> {
            construction.set(c);
            c.sdkOwned(new Tracked("pool", closes, null));
            c.sdkOwned(new Tracked("reaper", closes, null));
            return new FakeSyncClient("apache", caps(HttpTransportMode.SYNC), closes);
        });
        ResolvedHttpClient<SdkSyncHttpClient> resolved = HttpClientResolver.sync().implementation(service).resolve();
        assertEquals(List.of(), closes);
        assertThrows(IllegalStateException.class,
                     () -> construction.get().sdkOwned(new Tracked("late", closes, null)));
        resolved.close();
        resolved.close();
        assertEquals(List.of("client:apache", "reaper", "pool"), closes);
    }

    @Test
    void aNullClientIsAnInvalidImplementationAndStillRollsBack() {
        List<String> closes = new ArrayList<>();
        FakeSyncService service = new FakeSyncService("broken", 0, caps(HttpTransportMode.SYNC), c -> {
            c.sdkOwned(new Tracked("pool", closes, null));
            return null;
        });
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class, () -> HttpClientResolver.sync().implementation(service).resolve());
        assertEquals(Reason.INVALID_IMPLEMENTATION, e.reason());
        assertEquals(List.of("pool"), closes);
    }

    // Fixtures.

    static HttpClientCapabilities caps(HttpTransportMode mode, HttpClientOption... options) {
        return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1).addOption(options)
                                     .maxBufferedBytesPerExchange(8192).cancellationTimeout(Duration.ofSeconds(1))
                                     .build();
    }

    static <T> List<List<T>> permutations(List<T> items) {
        if (items.isEmpty()) {
            return List.of(List.of());
        }
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<T> rest = new ArrayList<>(items);
            T head = rest.remove(i);
            for (List<T> tail : permutations(rest)) {
                List<T> permutation = new ArrayList<>();
                permutation.add(head);
                permutation.addAll(tail);
                out.add(permutation);
            }
        }
        return out;
    }

    /** A resource that records its close, optionally failing it. */
    record Tracked(String name, List<String> closes, RuntimeException failure) implements AutoCloseable {
        @Override
        public void close() {
            closes.add(name);
            if (failure != null) {
                throw failure;
            }
        }
    }

    static final class FakeSyncService implements SdkHttpService {
        final AtomicInteger creates = new AtomicInteger();
        private final String name;
        private final int priority;
        private final HttpClientCapabilities capabilities;
        private final Function<HttpClientConstruction, SdkSyncHttpClient> factory;

        FakeSyncService(String name, int priority, HttpClientCapabilities capabilities) {
            this(name, priority, capabilities, c -> new FakeSyncClient(name, capabilities));
        }

        FakeSyncService(String name, int priority, HttpClientCapabilities capabilities,
                        Function<HttpClientConstruction, SdkSyncHttpClient> factory) {
            this.name = name;
            this.priority = priority;
            this.capabilities = capabilities;
            this.factory = factory;
        }

        @Override
        public String implementationName() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public SdkSyncHttpClient create(HttpClientConstruction construction) {
            creates.incrementAndGet();
            return factory.apply(construction);
        }

        @Override
        public String toString() {
            return name + "@" + priority;
        }
    }

    static final class FakeAsyncService implements SdkAsyncHttpService {
        private final String name;
        private final int priority;
        private final HttpClientCapabilities capabilities;

        FakeAsyncService(String name, int priority, HttpClientCapabilities capabilities) {
            this.name = name;
            this.priority = priority;
            this.capabilities = capabilities;
        }

        @Override
        public String implementationName() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public SdkAsyncHttpClient create(HttpClientConstruction construction) {
            return new FakeAsyncClient(name, capabilities);
        }

        @Override
        public String toString() {
            return name + "@" + priority;
        }
    }

    static final class FakeSyncClient implements SdkSyncHttpClient {
        final AtomicInteger closed = new AtomicInteger();
        private final String name;
        private final HttpClientCapabilities capabilities;
        private final List<String> closes;

        FakeSyncClient(String name, HttpClientCapabilities capabilities) {
            this(name, capabilities, new ArrayList<>());
        }

        FakeSyncClient(String name, HttpClientCapabilities capabilities, List<String> closes) {
            this.name = name;
            this.capabilities = capabilities;
            this.closes = closes;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public String clientName() {
            return name;
        }

        @Override
        public void close() {
            closed.incrementAndGet();
            closes.add("client:" + name);
        }
    }

    static final class FakeAsyncClient implements SdkAsyncHttpClient {
        final AtomicInteger closed = new AtomicInteger();
        private final String name;
        private final HttpClientCapabilities capabilities;

        FakeAsyncClient(String name, HttpClientCapabilities capabilities) {
            this.name = name;
            this.capabilities = capabilities;
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public String clientName() {
            return name;
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }
}
