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

package software.amazon.awssdk.http.crt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.crt.internal.CrtClientCore;
import software.amazon.awssdk.http.crt.internal.CrtNativeAvailability;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.HttpImplementationDiscovery;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException;
import software.amazon.awssdk.http.provider.InstalledHttpImplementations;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

class CrtProvidersAndLifecycleTest {
    private static final Set<HttpClientOption> EXPECTED_OPTIONS =
        EnumSet.of(HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
                   HttpClientOption.TLS_NEGOTIATION_TIMEOUT, HttpClientOption.TRUST_ALL_CERTIFICATES,
                   HttpClientOption.PROXY, HttpClientOption.PROXY_CREDENTIALS, HttpClientOption.MAX_CONNECTIONS,
                   HttpClientOption.CONNECTION_MAX_IDLE_TIME, HttpClientOption.TCP_KEEP_ALIVE);
    private static final CrtNativeAvailability.Probe MISSING = () -> {
        throw new UnsatisfiedLinkError("no aws-crt-jni in java.library.path (simulated)");
    };

    private static TestServers.Server server;
    private static TestServers.Tls tls;
    private static TestServers.Server httpsServer;

    @BeforeAll
    static void start() throws Exception {
        server = TestServers.Server.http(exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().add("X-Via", exchange.getRequestURI().toString());
                exchange.sendResponseHeaders(200, 2);
                exchange.getResponseBody().write("ok".getBytes());
            } catch (IOException e) {
                // client went away
            }
        });
        tls = TestServers.Tls.generate();
        httpsServer = TestServers.Server.https(exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, 3);
                exchange.getResponseBody().write("tls".getBytes());
            } catch (IOException e) {
                // client went away
            }
        }, tls);
    }

    @AfterAll
    static void stop() {
        server.close();
        httpsServer.close();
    }

    private static void assertCapabilities(HttpClientCapabilities c, HttpTransportMode mode) {
        assertEquals(mode, c.mode());
        assertEquals(Set.of(Protocol.HTTP1_1), c.protocols());
        assertEquals(EXPECTED_OPTIONS, c.options());
        assertEquals(16L * 1024 * 1024, c.maxBufferedBytesPerExchange());
        assertEquals(Duration.ofSeconds(2), c.cancellationTimeout());
    }

    @Test
    void capabilitiesArePerModeAndMatchTheProviders() {
        try (AwsCrtHttpClient sync = AwsCrtHttpClient.create();
             AwsCrtAsyncHttpClient async = AwsCrtAsyncHttpClient.create()) {
            assertCapabilities(sync.capabilities(), HttpTransportMode.SYNC);
            assertCapabilities(async.capabilities(), HttpTransportMode.ASYNC);
            assertEquals(new AwsCrtSdkHttpService().capabilities(), sync.capabilities());
            assertEquals(new AwsCrtSdkAsyncHttpService().capabilities(), async.capabilities());
            assertEquals("AwsCommonRuntime", sync.clientName());
            assertEquals("AwsCommonRuntime", async.clientName());
        }
    }

    @Test
    void providersAreDiscoveredSeparatelyPerMode() {
        InstalledHttpImplementations installed =
            HttpImplementationDiscovery.discover(CrtProvidersAndLifecycleTest.class.getClassLoader());
        assertTrue(installed.names(HttpTransportMode.SYNC).contains(AwsCrtSdkHttpService.NAME));
        assertTrue(installed.names(HttpTransportMode.ASYNC).contains(AwsCrtSdkAsyncHttpService.NAME));
        assertFalse(installed.names(HttpTransportMode.SYNC).contains(AwsCrtSdkAsyncHttpService.NAME));
        assertEquals(20, AwsCrtSdkHttpService.PRIORITY);
        assertEquals(20, AwsCrtSdkAsyncHttpService.PRIORITY);
        assertNotEquals(AwsCrtSdkHttpService.NAME, AwsCrtSdkAsyncHttpService.NAME);
    }

    @Test
    void providersResolveAndConfigureClients() throws Exception {
        HttpClientConfiguration configuration =
            HttpClientConfiguration.builder().put(HttpClientOption.MAX_CONNECTIONS, 3)
                                   .put(HttpClientOption.CONNECTION_TIMEOUT, Duration.ofSeconds(1)).build();
        try (ResolvedHttpClient<SdkSyncHttpClient> resolved =
                 HttpClientResolver.sync().implementation(new AwsCrtSdkHttpService()).configuration(configuration)
                                   .resolve();
             SyncHttpExchange exchange = resolved.client().prepare(server.get("/p").build(),
                                                                   HttpExecutionOptions.defaults())) {
            assertEquals(200, exchange.execute().statusCode());
        }
        try (ResolvedHttpClient<SdkAsyncHttpClient> resolved =
                 HttpClientResolver.async().implementation(new AwsCrtSdkAsyncHttpService()).resolve()) {
            AwsCrtAsyncHttpClientTest.Recorder recorder = new AwsCrtAsyncHttpClientTest.Recorder(true);
            resolved.client().execute(server.get("/p").build(), recorder, HttpExecutionOptions.defaults())
                    .completion().get(5, TimeUnit.SECONDS);
            assertEquals("ok", new String(recorder.body.get()));
        }
    }

    @Test
    void unsupportedOptionsAreRejectedBeforeAnyNativeResourceIsAcquired() throws Exception {
        Set<CrtResource> before = TestServers.liveNativeResources();
        HttpClientConfiguration configuration =
            HttpClientConfiguration.builder().put(HttpClientOption.READ_TIMEOUT, Duration.ofSeconds(1))
                                   .put(HttpClientOption.PROTOCOL_VERSION, Protocol.HTTP2).build();
        HttpImplementationSelectionException sync = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().implementation(new AwsCrtSdkHttpService()).configuration(configuration)
                                    .resolve());
        assertEquals(HttpImplementationSelectionException.Reason.UNSUPPORTED_OPTION, sync.reason());
        assertEquals(List.of(HttpClientOption.READ_TIMEOUT, HttpClientOption.PROTOCOL_VERSION),
                     sync.unsupportedOptions());
        assertThrows(HttpImplementationSelectionException.class,
                     () -> HttpClientResolver.async().implementation(new AwsCrtSdkAsyncHttpService())
                                             .configuration(configuration).resolve());
        assertEquals(Set.of(), TestServers.awaitNoNewNativeResources(before, Duration.ofSeconds(5)));
    }

    @Test
    void nativeLibraryIsAvailableHere() {
        assertTrue(CrtNativeAvailability.isAvailable(CrtNativeAvailability.DEFAULT_PROBE));
    }

    @Test
    void missingNativeLibraryFailsConstructionWithAConfigurationDiagnostic() {
        SdkClientConfigurationException sync = assertThrows(
            SdkClientConfigurationException.class, () -> AwsCrtHttpClient.builder().nativeProbe(MISSING).build());
        assertTrue(sync.getMessage().contains("aws-crt-client (sync)"), sync.getMessage());
        assertTrue(sync.getMessage().contains(System.getProperty("os.name")), sync.getMessage());
        assertTrue(sync.getCause() instanceof UnsatisfiedLinkError);
        SdkClientConfigurationException async = assertThrows(
            SdkClientConfigurationException.class,
            () -> AwsCrtAsyncHttpClient.builder().nativeProbe(MISSING).build());
        assertTrue(async.getMessage().contains("aws-crt-client (async)"), async.getMessage());
        assertFalse(CrtNativeAvailability.isAvailable(MISSING));

        // Metadata stays readable, and the failure surfaces only from create, through the resolver.
        AwsCrtSdkHttpService service = new AwsCrtSdkHttpService(MISSING);
        assertEquals(HttpTransportMode.SYNC, service.capabilities().mode());
        assertThrows(SdkClientConfigurationException.class,
                     () -> HttpClientResolver.sync().implementation(service).resolve());
        AwsCrtSdkAsyncHttpService asyncService = new AwsCrtSdkAsyncHttpService(MISSING);
        assertEquals(AwsCrtSdkAsyncHttpService.PRIORITY, asyncService.priority());
        assertThrows(SdkClientConfigurationException.class,
                     () -> HttpClientResolver.async().implementation(asyncService).resolve());
    }

    @Test
    void failedConstructionReleasesEveryAcquiredNativeResource() throws Exception {
        Set<CrtResource> before = TestServers.liveNativeResources();
        // The invalid trust store fails after the event loop, resolver, bootstrap, socket and TLS options exist.
        assertThrows(RuntimeException.class,
                     () -> AwsCrtHttpClient.builder().numEventLoopThreads(1).tlsTrustedCertificatesPem("not a pem")
                                           .build());
        List<AutoCloseable> registered = new ArrayList<>();
        assertThrows(RuntimeException.class,
                     () -> AwsCrtAsyncHttpClient.builder().numEventLoopThreads(1)
                                                .tlsTrustedCertificatesPem("not a pem").build(registered::add));
        assertTrue(registered.size() >= 4, "registered " + registered);
        for (AutoCloseable r : registered) {
            assertEquals(1, ((CrtClientCore.Owned) r).closeCount(), r.toString());
            r.close();
            assertEquals(1, ((CrtClientCore.Owned) r).closeCount(), r.toString());
        }
        assertEquals(Set.of(), TestServers.awaitNoNewNativeResources(before, Duration.ofSeconds(10)));
    }

    @Test
    void closeReleasesEveryNativeResourceExactlyOnceInBothModes() throws Exception {
        Set<CrtResource> before = TestServers.liveNativeResources();
        List<CrtClientCore.Owned> all = new ArrayList<>();
        AwsCrtHttpClient sync = AwsCrtHttpClient.builder().numEventLoopThreads(1).build();
        try (SyncHttpExchange exchange = sync.prepare(server.get("/a").build(), HttpExecutionOptions.defaults())) {
            assertEquals("ok", new String(TestServers.readAll(exchange.execute().body())));
        }
        AwsCrtAsyncHttpClient async = AwsCrtAsyncHttpClient.builder().numEventLoopThreads(1).build();
        AwsCrtAsyncHttpClientTest.Recorder recorder = new AwsCrtAsyncHttpClientTest.Recorder(true);
        async.execute(server.get("/b").build(), recorder, HttpExecutionOptions.defaults()).completion()
             .get(5, TimeUnit.SECONDS);
        all.addAll(sync.core().ownedResources());
        all.addAll(sync.core().poolResources());
        all.addAll(async.core().ownedResources());
        all.addAll(async.core().poolResources());
        assertEquals(List.of("EventLoopGroup", "HostResolver", "ClientBootstrap", "SocketOptions", "TlsContextOptions",
                             "TlsContext"),
                     sync.core().ownedResources().stream().map(CrtClientCore.Owned::name).toList());
        assertTrue(TestServers.liveNativeResources().size() > before.size());
        sync.close();
        async.close();
        sync.close();
        async.close();
        for (CrtClientCore.Owned o : all) {
            assertEquals(1, o.closeCount(), o.name());
        }
        assertEquals(Set.of(), TestServers.awaitNoNewNativeResources(before, Duration.ofSeconds(10)));
    }

    @Test
    void resolverRollbackAndCloseReleaseRegistrationsOnce() throws Exception {
        Set<CrtResource> before = TestServers.liveNativeResources();
        ResolvedHttpClient<SdkSyncHttpClient> resolved =
            HttpClientResolver.sync().implementation(new AwsCrtSdkHttpService()).resolve();
        CrtClientCore core = ((AwsCrtHttpClient) resolved.client()).core();
        List<CrtClientCore.Owned> owned = core.ownedResources();
        resolved.close();
        resolved.close();
        for (CrtClientCore.Owned o : owned) {
            assertEquals(1, o.closeCount(), o.name());
        }
        assertEquals(Set.of(), TestServers.awaitNoNewNativeResources(before, Duration.ofSeconds(10)));
    }

    @Test
    void tlsTrustAllCertificates() throws Exception {
        SdkHttpRequest request = httpsServer.request("https", "localhost", "GET", "/").build();
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().trustAllCertificates(true).build();
             SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            assertEquals("tls", new String(TestServers.readAll(exchange.execute().body())));
        }
    }

    @Test
    void tlsWithATestTrustStore() throws Exception {
        SdkHttpRequest request = httpsServer.request("https", "localhost", "GET", "/").build();
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().tlsTrustedCertificatesPem(tls.pem).build();
             SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            assertEquals("tls", new String(TestServers.readAll(exchange.execute().body())));
        }
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().tlsTrustedCertificatesPem(tls.pem)
                                                                 .build()) {
            AwsCrtAsyncHttpClientTest.Recorder recorder = new AwsCrtAsyncHttpClientTest.Recorder(true);
            client.execute(request, recorder, HttpExecutionOptions.defaults()).completion().get(10, TimeUnit.SECONDS);
            assertEquals("tls", new String(recorder.body.get()));
        }
    }

    @Test
    void untrustedCertificateIsATlsFailure() {
        SdkHttpRequest request = httpsServer.request("https", "localhost", "GET", "/").build();
        try (AwsCrtHttpClient client = AwsCrtHttpClient.create();
             SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpFailureCategory.TLS, e.category(), e.getMessage());
        }
    }

    @Test
    void forwardingProxyReceivesTheAbsoluteTarget() throws Exception {
        ProxyConfiguration proxy = ProxyConfiguration.builder().host("127.0.0.1").port(server.port()).build();
        SdkHttpRequest request = SdkHttpRequest.builder().method("GET").scheme("http").host("example.invalid")
                                               .port(8080).encodedPath("/via-proxy").build();
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().proxyConfiguration(proxy).build();
             SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            assertEquals("http://example.invalid:8080/via-proxy",
                         exchange.execute().headers().firstValue("X-Via").orElseThrow());
        }
        assertFalse(proxy.toString().contains("password"));
    }
}
