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

package software.amazon.awssdk.http.apache5;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.HttpImplementationDiscovery;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException;
import software.amazon.awssdk.http.provider.InstalledHttpImplementations;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

/** Behaviour of the sync {@link Apache5HttpClient} against byte-exact and TLS test servers. */
class Apache5HttpClientTest {
    private RawHttpServer server;
    private Apache5HttpClient client;

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
    }

    private static SdkHttpRequest.Builder get(int port, String path) {
        return SdkHttpRequest.builder().method("GET").scheme("http").host("127.0.0.1").port(port).encodedPath(path);
    }

    private static String read(SdkHttpResponse response) throws IOException {
        try (InputStream in = response.body().streamingBody().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static HttpTransportException failure(SyncHttpExchange exchange) {
        return assertThrows(HttpTransportException.class, exchange::execute);
    }

    @Test
    void syncCapabilitiesAndProviderMetadata() {
        HttpClientCapabilities c = Apache5HttpClient.CAPABILITIES;
        assertEquals(HttpTransportMode.SYNC, c.mode());
        assertEquals(Set.of(Protocol.HTTP1_1), c.protocols());
        assertTrue(c.supports(HttpClientOption.READ_TIMEOUT) && c.supports(HttpClientOption.PROXY)
                   && c.supports(HttpClientOption.EXPECT_CONTINUE));
        assertEquals(List.of(HttpClientOption.WRITE_TIMEOUT, HttpClientOption.TLS_NEGOTIATION_TIMEOUT,
                             HttpClientOption.TRUST_ALL_CERTIFICATES, HttpClientOption.PROXY_FROM_ENVIRONMENT,
                             HttpClientOption.MAX_PENDING_CONNECTION_ACQUIRES, HttpClientOption.PROTOCOL_VERSION,
                             HttpClientOption.HTTP2_MAX_STREAMS, HttpClientOption.HTTP2_INITIAL_WINDOW_SIZE),
                     c.unsupported(List.of(HttpClientOption.values())));
        assertEquals(64 * 1024, c.maxBufferedBytesPerExchange());
        assertEquals(Duration.ofSeconds(2), c.cancellationTimeout());
        Apache5SdkHttpService service = new Apache5SdkHttpService();
        assertEquals("apache5-client", service.implementationName());
        assertEquals(300, service.priority());
        assertEquals(c, service.capabilities());
        assertEquals(HttpTransportMode.SYNC, service.mode());
        client = Apache5HttpClient.create();
        assertEquals(c, client.capabilities());
        assertEquals("Apache5", client.clientName());
    }

    @Test
    void requestIsSentExactlyWithOrderedRepeatedHeadersAndRawTarget() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.respond(
            out, 201, "done".getBytes(StandardCharsets.UTF_8), "X-R: 1", "Set-Thing: b", "X-R: 2"));
        client = Apache5HttpClient.create();
        SdkHttpRequest request =
            SdkHttpRequest.builder().method("PUT").scheme("http").host("127.0.0.1").port(server.port())
                          .encodedPath("/a%2Fb/c%20d").addRawQueryParameter("x", "%2F")
                          .addRawQueryParameter("x", "2").addRawQueryParameter("flag", null)
                          .addHeader("X-B", "first").addHeader("X-A", "secret-value").addHeader("X-B", "second")
                          .addHeader("Content-Type", "text/plain").body(HttpBody.fromUtf8("hello")).build();
        try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            SdkHttpResponse response = exchange.execute();
            assertEquals(201, response.statusCode());
            assertEquals(List.of("1", "2"), response.headers().allValues("X-R"));
            assertEquals("X-R", response.headers().name(0));
            assertEquals("Set-Thing", response.headers().name(1));
            assertEquals("done", read(response));
            assertFalse(exchange.toString().contains("secret-value"));
        }
        RawHttpServer.Request received = server.take();
        assertEquals("PUT /a%2Fb/c%20d?x=%2F&x=2&flag HTTP/1.1", received.requestLine());
        List<String> names = received.headerNames();
        assertEquals(List.of("X-B", "X-A", "X-B", "Content-Type"), names.subList(0, 4));
        assertEquals(List.of("first", "second"), received.values("X-B"));
        assertEquals(List.of("5"), received.values("Content-Length"));
        assertEquals(List.of("127.0.0.1:" + server.port()), received.values("Host"));
        assertTrue(received.values("User-Agent").isEmpty());
        assertTrue(received.values("Accept-Encoding").isEmpty());
        assertEquals("hello", new String(received.body(), StandardCharsets.UTF_8));
    }

    @Test
    void streamingRequestBodiesAreFramedAndClosed() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "ok"));
        client = Apache5HttpClient.create();
        byte[] data = new byte[100_000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        AtomicBoolean closed = new AtomicBoolean();
        InputStream source = new ByteArrayInputStream(data) {
            @Override
            public void close() {
                closed.set(true);
            }
        };
        SdkHttpRequest chunked = get(server.port(), "/up").method("POST")
            .body(HttpBody.streaming(StreamingBodies.fromInputStream(source, -1, null))).build();
        try (SyncHttpExchange exchange = client.prepare(chunked, HttpExecutionOptions.defaults())) {
            assertEquals("ok", read(exchange.execute()));
        }
        RawHttpServer.Request received = server.take();
        assertEquals(List.of("chunked"), received.values("Transfer-Encoding"));
        assertArrayEquals(data, received.body());
        assertTrue(closed.get());

        SdkHttpRequest sized = get(server.port(), "/up").method("POST")
            .body(HttpBody.streaming(StreamingBodies.fromBytes(data, null))).build();
        try (SyncHttpExchange exchange = client.prepare(sized, HttpExecutionOptions.defaults())) {
            assertEquals("ok", read(exchange.execute()));
        }
        received = server.take();
        assertEquals(List.of(String.valueOf(data.length)), received.values("Content-Length"));
        assertArrayEquals(data, received.body());
    }

    @Test
    void consumedResponsesReuseTheConnectionAndEarlyClosesDiscardIt() throws Exception {
        server = new RawHttpServer((r, out, s) -> {
            if (r.target().equals("/big")) {
                out.write(("HTTP/1.1 200 OK\r\nContent-Length: 10000000\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(new byte[100]);
                out.flush();
                s.awaitRelease();
            } else {
                RawHttpServer.ok(out, "small");
            }
        });
        client = Apache5HttpClient.create();
        for (int i = 0; i < 2; i++) {
            try (SyncHttpExchange exchange = client.prepare(get(server.port(), "/s").build(),
                                                            HttpExecutionOptions.defaults())) {
                assertEquals("small", read(exchange.execute()));
            }
        }
        int first = server.take().connection();
        assertEquals(first, server.take().connection());
        assertEquals(0, client.poolStats()[0]);
        assertEquals(1, client.poolStats()[1]);

        long start = System.nanoTime();
        try (SyncHttpExchange exchange = client.prepare(get(server.port(), "/big").build(),
                                                        HttpExecutionOptions.defaults())) {
            InputStream in = exchange.execute().body().streamingBody().openStream();
            assertEquals(10, in.readNBytes(10).length);
            in.close();
        }
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(2)) < 0,
                   "an early close must not drain the body");
        assertEquals(first, server.take().connection());
        assertEquals(0, client.poolStats()[0]);

        try (SyncHttpExchange exchange = client.prepare(get(server.port(), "/s").build(),
                                                        HttpExecutionOptions.defaults())) {
            assertEquals("small", read(exchange.execute()));
        }
        assertNotEquals(first, server.take().connection());
    }

    @Test
    void timeoutsAreReportedByPhase() throws Exception {
        server = new RawHttpServer((r, out, s) -> {
            if (r.target().equals("/slow-body")) {
                out.write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nabc".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } else if (r.target().equals("/fast")) {
                RawHttpServer.ok(out, "fast");
                return;
            }
            s.awaitRelease();
        });
        client = Apache5HttpClient.builder().socketTimeout(Duration.ofMillis(300)).maxConnections(1).build();

        HttpTransportException headers =
            failure(client.prepare(get(server.port(), "/slow-headers").build(), HttpExecutionOptions.defaults()));
        assertEquals(HttpFailureCategory.TIMEOUT, headers.category());
        assertEquals(HttpPhase.RESPONSE_CONSUMPTION, headers.phase());

        try (SyncHttpExchange exchange = client.prepare(get(server.port(), "/slow-body").build(),
                                                        HttpExecutionOptions.defaults())) {
            InputStream in = exchange.execute().body().streamingBody().openStream();
            HttpTransportException body = assertThrows(HttpTransportException.class, in::readAllBytes);
            assertEquals(HttpFailureCategory.TIMEOUT, body.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, body.phase());
        }

        try (SyncHttpExchange held = client.prepare(get(server.port(), "/fast").build(),
                                                    HttpExecutionOptions.defaults())) {
            held.execute();
            HttpExecutionOptions shortLease =
                HttpExecutionOptions.builder().connectionAcquisitionTimeout(Duration.ofMillis(200)).build();
            HttpTransportException lease = failure(client.prepare(get(server.port(), "/fast").build(), shortLease));
            assertEquals(HttpFailureCategory.TIMEOUT, lease.category());
            assertEquals(HttpPhase.CONNECTION_ACQUISITION, lease.phase());
        }
    }

    @Test
    void connectFailuresAreReportedInTheConnectPhase() throws Exception {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        client = Apache5HttpClient.builder().connectionTimeout(Duration.ofMillis(300)).build();
        HttpTransportException refused =
            failure(client.prepare(get(closedPort, "/").build(), HttpExecutionOptions.defaults()));
        assertEquals(HttpFailureCategory.CONNECT, refused.category());
        assertEquals(HttpPhase.CONNECT, refused.phase());
        assertTrue(refused.retryable());
    }

    @Test
    void abortCancelsBlockedExecutesAndReadsWithinTheCancellationTimeout() throws Exception {
        server = new RawHttpServer((r, out, s) -> {
            if (r.target().equals("/body")) {
                out.write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nab".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }
            s.awaitRelease();
        });
        client = Apache5HttpClient.create();
        Duration limit = client.capabilities().cancellationTimeout();

        SyncHttpExchange before = client.prepare(get(server.port(), "/").build(), HttpExecutionOptions.defaults());
        before.abort();
        assertEquals(HttpFailureCategory.CANCELLED, failure(before).category());
        before.close();

        try (SyncHttpExchange blocked = client.prepare(get(server.port(), "/").build(),
                                                       HttpExecutionOptions.defaults())) {
            CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> failure(blocked));
            server.take();
            long start = System.nanoTime();
            blocked.abort();
            HttpTransportException e = (HttpTransportException) result.get();
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(limit) < 0);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertFalse(e.retryable());
        }

        try (SyncHttpExchange reading = client.prepare(get(server.port(), "/body").build(),
                                                       HttpExecutionOptions.defaults())) {
            InputStream in = reading.execute().body().streamingBody().openStream();
            assertEquals(2, in.readNBytes(2).length);
            CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(
                () -> assertThrows(HttpTransportException.class, in::readAllBytes));
            Thread.sleep(200);
            long start = System.nanoTime();
            reading.abort();
            reading.abort();
            HttpTransportException e = (HttpTransportException) result.get();
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(limit) < 0);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        }
        assertEquals(0, client.poolStats()[0]);
    }

    @Test
    void closeIsIdempotentAndCancelsExchangesInFlight() throws Exception {
        server = new RawHttpServer((r, out, s) -> s.awaitRelease());
        client = Apache5HttpClient.create();
        SyncHttpExchange exchange = client.prepare(get(server.port(), "/").build(), HttpExecutionOptions.defaults());
        CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> failure(exchange));
        server.take();
        client.close();
        client.close();
        assertEquals(HttpFailureCategory.CANCELLED, ((HttpTransportException) result.get()).category());
        exchange.close();
        exchange.close();
        assertThrows(IllegalStateException.class, exchange::execute);
        assertThrows(IllegalStateException.class,
                     () -> client.prepare(get(server.port(), "/").build(), HttpExecutionOptions.defaults()));
    }

    @Test
    void tlsUsesTheConfiguredTrustManagersAndReportsHandshakeFailures() throws Exception {
        TestTls tls = TestTls.get();
        HttpsServer https = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(tls.serverContext));
        https.createContext("/", e -> {
            byte[] body = "secure".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200, body.length);
            try (OutputStream out = e.getResponseBody()) {
                out.write(body);
            }
        });
        https.start();
        try {
            SdkHttpRequest request = SdkHttpRequest.builder().method("GET").scheme("https").host("127.0.0.1")
                                                   .port(https.getAddress().getPort()).encodedPath("/").build();
            client = Apache5HttpClient.builder().tlsTrustManagersProvider(() -> tls.trustManagers).build();
            try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
                assertEquals("secure", read(exchange.execute()));
            }
            try (Apache5HttpClient untrusting = Apache5HttpClient.create()) {
                HttpTransportException e = failure(untrusting.prepare(request, HttpExecutionOptions.defaults()));
                assertEquals(HttpFailureCategory.TLS, e.category(), String.valueOf(e.getCause()));
                assertEquals(HttpPhase.TLS_HANDSHAKE, e.phase());
            }
        } finally {
            https.stop(0);
        }
    }

    @Test
    void explicitProxyReceivesAbsoluteTargetsWithCredentialsAndNonProxyHostsBypassIt() throws Exception {
        server = new RawHttpServer((r, out, s) -> {
            if (r.values("Proxy-Authorization").isEmpty() && r.target().startsWith("http://")) {
                out.write(("HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=\"p\"\r\n"
                           + "Content-Length: 0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } else {
                RawHttpServer.ok(out, "via");
            }
        });
        ProxyConfiguration proxy = ProxyConfiguration.builder().endpoint(URI.create("http://127.0.0.1:" + server.port()))
                                                     .username("user").password("pw")
                                                     .addNonProxyHost("localhost").build();
        assertFalse(proxy.toString().contains("pw"));
        client = Apache5HttpClient.builder().proxyConfiguration(proxy).build();
        SdkHttpRequest viaProxy = SdkHttpRequest.builder().method("GET").scheme("http").host("example.invalid")
                                                .port(8080).encodedPath("/p%20q").build();
        try (SyncHttpExchange exchange = client.prepare(viaProxy, HttpExecutionOptions.defaults())) {
            assertEquals("via", read(exchange.execute()));
        }
        RawHttpServer.Request challenged = server.take();
        assertEquals("http://example.invalid:8080/p%20q", challenged.target());
        RawHttpServer.Request authenticated = server.take();
        assertTrue(authenticated.values("Proxy-Authorization").get(0).startsWith("Basic "));

        SdkHttpRequest direct = SdkHttpRequest.builder().method("GET").scheme("http").host("localhost")
                                              .port(server.port()).encodedPath("/direct").build();
        try (SyncHttpExchange exchange = client.prepare(direct, HttpExecutionOptions.defaults())) {
            assertEquals("via", read(exchange.execute()));
        }
        assertEquals("/direct", server.take().target());
    }

    @Test
    void providerIsDiscoveredAndResolvedWithSdkOwnedResources() throws Exception {
        InstalledHttpImplementations installed = HttpImplementationDiscovery.discover(getClass().getClassLoader());
        assertTrue(installed.names(HttpTransportMode.SYNC).contains("apache5-client"));
        assertFalse(installed.names(HttpTransportMode.ASYNC).contains("apache5-async-client"));

        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "resolved"));
        ResolvedHttpClient<SdkSyncHttpClient> resolved =
            HttpClientResolver.sync().installed(() -> InstalledHttpImplementations.of(new Apache5SdkHttpService()))
                              .configuration(HttpClientConfiguration.builder()
                                                                    .put(HttpClientOption.READ_TIMEOUT,
                                                                         Duration.ofSeconds(5))
                                                                    .put(HttpClientOption.MAX_CONNECTIONS, 3).build())
                              .resolve();
        SdkSyncHttpClient sync = resolved.client();
        assertEquals("apache5-client", resolved.implementationName());
        try (SyncHttpExchange exchange = sync.prepare(get(server.port(), "/").build(), HttpExecutionOptions.defaults())) {
            assertEquals("resolved", read(exchange.execute()));
        }
        resolved.close();
        resolved.close();
        assertThrows(IllegalStateException.class,
                     () -> sync.prepare(get(server.port(), "/").build(), HttpExecutionOptions.defaults()));
    }

    @Test
    void unsupportedOrInvalidOptionsFailConstructionByV2Name() {
        HttpImplementationSelectionException unsupported = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().installed(() -> InstalledHttpImplementations.of(new Apache5SdkHttpService()))
                                    .configuration(HttpClientConfiguration.builder()
                                                                          .put(HttpClientOption.WRITE_TIMEOUT,
                                                                               Duration.ofSeconds(1))
                                                                          .put(HttpClientOption.PROXY_FROM_ENVIRONMENT,
                                                                               true)
                                                                          .put(HttpClientOption.PROTOCOL_VERSION,
                                                                               Protocol.HTTP2)
                                                                          .build())
                                    .resolve());
        assertEquals(List.of(HttpClientOption.WRITE_TIMEOUT, HttpClientOption.PROXY_FROM_ENVIRONMENT,
                             HttpClientOption.PROTOCOL_VERSION), unsupported.unsupportedOptions());
        String message = unsupported.getMessage();
        assertTrue(message.contains("apache5-client") && message.contains("writeTimeout")
                   && message.contains("proxyConfiguration.useSystemPropertyValues") && message.contains("protocol"),
                   message);

        SdkClientConfigurationException invalid = assertThrows(
            SdkClientConfigurationException.class,
            () -> HttpClientResolver.sync().installed(() -> InstalledHttpImplementations.of(new Apache5SdkHttpService()))
                                    .configuration(HttpClientConfiguration.builder()
                                                                          .put(HttpClientOption.MAX_CONNECTIONS, "ten")
                                                                          .build())
                                    .resolve());
        assertTrue(invalid.getMessage().contains("maxConnections"), invalid.getMessage());
        assertThrows(SdkClientConfigurationException.class,
                     () -> Apache5HttpClient.builder().socketTimeout(Duration.ZERO));
    }
}
