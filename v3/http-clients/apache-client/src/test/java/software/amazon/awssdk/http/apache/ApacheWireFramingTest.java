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

package software.amazon.awssdk.http.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;

/** Apache 4 specific framing, {@code Expect: 100-continue}, and abort of a connection-lease wait. */
class ApacheWireFramingTest {
    private RawHttpServer server;
    private ApacheHttpClient client;

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
    }

    private SdkHttpRequest.Builder request(String method, String path) {
        return SdkHttpRequest.builder().method(method).scheme("http").host("127.0.0.1").port(server.port())
                             .encodedPath(path);
    }

    private static String read(SdkHttpResponse response) throws IOException {
        try (InputStream in = response.body().streamingBody().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String send(SdkHttpRequest request) throws IOException {
        try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            return read(exchange.execute());
        }
    }

    @Test
    void callerFramingHeadersAreKeptAndNeverDuplicated() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "ok"));
        client = ApacheHttpClient.create();
        send(request("POST", "/h").addHeader("host", "custom.example").addHeader("content-length", "3")
                                  .body(HttpBody.fromUtf8("abc")).build());
        RawHttpServer.Request received = server.take();
        assertEquals(List.of("host", "content-length"), received.headerNames());
        assertEquals(List.of("custom.example"), received.values("Host"));
        assertEquals("abc", new String(received.body(), StandardCharsets.UTF_8));

        send(request("PUT", "/empty").build());
        received = server.take();
        assertEquals(List.of("0"), received.values("Content-Length"));
        assertTrue(received.values("Transfer-Encoding").isEmpty());

        send(request("GET", "/get").build());
        received = server.take();
        assertEquals(List.of("Host"), received.headerNames());

        assertThrows(IllegalArgumentException.class,
                     () -> client.prepare(request("POST", "/bad").addHeader("Content-Length", "9")
                                                                  .body(HttpBody.fromUtf8("abc")).build(),
                                          HttpExecutionOptions.defaults()));
    }

    @Test
    void expectContinueIsSentForNonEmptyBodiesWhenEnabled() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "ok"));
        client = ApacheHttpClient.builder().expectContinueEnabled(true).build();
        assertEquals("ok", send(request("PUT", "/e").body(HttpBody.fromUtf8("data")).build()));
        RawHttpServer.Request received = server.take();
        assertEquals(List.of("100-continue"), received.values("Expect"));
        assertEquals("data", new String(received.body(), StandardCharsets.UTF_8));

        send(request("GET", "/no-body").build());
        assertTrue(server.take().values("Expect").isEmpty());
    }

    @Test
    void chunkedAndBodilessResponsesReuseTheConnection() throws Exception {
        server = new RawHttpServer((r, out, s) -> {
            if (r.target().equals("/chunked")) {
                out.write(("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n")
                              .getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } else {
                RawHttpServer.respond(out, 204, new byte[0]);
            }
        });
        client = ApacheHttpClient.create();
        assertEquals("abcde", send(request("GET", "/chunked").build()));
        try (SyncHttpExchange exchange = client.prepare(request("DELETE", "/none").build(),
                                                        HttpExecutionOptions.defaults())) {
            SdkHttpResponse response = exchange.execute();
            assertEquals(204, response.statusCode());
            assertEquals(HttpBody.Kind.EMPTY, response.body().kind());
        }
        assertEquals("abcde", send(request("GET", "/chunked").build()));
        int first = server.take().connection();
        assertEquals(first, server.take().connection());
        assertEquals(first, server.take().connection());
        assertEquals(1, server.connectionCount());
        assertEquals(0, client.poolStats()[0]);
    }

    @Test
    void aFailingRequestBodySourceIsReportedAsTransmission() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "never"));
        client = ApacheHttpClient.create();
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("source failed");
            }
        };
        SdkHttpRequest request = request("PUT", "/fail")
            .body(HttpBody.streaming(StreamingBodies.fromInputStream(failing, -1, null)))
            .build();
        try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpPhase.REQUEST_TRANSMISSION, e.phase());
            assertEquals(HttpFailureCategory.REQUEST_TRANSMISSION, e.category());
        }
        assertEquals(0, client.poolStats()[0]);
    }

    @Test
    void abortCancelsAConnectionLeaseWait() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "held"));
        client = ApacheHttpClient.builder().maxConnections(1).connectionAcquisitionTimeout(Duration.ofSeconds(30))
                                 .build();
        try (SyncHttpExchange held = client.prepare(request("GET", "/1").build(), HttpExecutionOptions.defaults())) {
            held.execute();
            SyncHttpExchange waiting = client.prepare(request("GET", "/2").build(), HttpExecutionOptions.defaults());
            CompletableFuture<HttpTransportException> result = CompletableFuture.supplyAsync(
                () -> assertThrows(HttpTransportException.class, waiting::execute));
            Thread.sleep(200);
            long start = System.nanoTime();
            waiting.abort();
            HttpTransportException e = result.get();
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(client.capabilities()
                                                                                   .cancellationTimeout()) < 0);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.CONNECTION_ACQUISITION, e.phase());
            waiting.close();
        }
        assertEquals(0, client.poolStats()[0]);
    }
}
