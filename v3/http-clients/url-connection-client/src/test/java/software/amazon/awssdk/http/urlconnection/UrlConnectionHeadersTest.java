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

package software.amazon.awssdk.http.urlconnection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.http.urlconnection.TestRequests.get;
import static software.amazon.awssdk.http.urlconnection.TestRequests.readAll;
import static software.amazon.awssdk.http.urlconnection.TestRequests.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;

/** Exact wire order of request headers, response header order, raw targets, framing and the documented limits. */
class UrlConnectionHeadersTest {
    private static final String JAVA_UA = "Java/" + System.getProperty("java.version");
    private RawHttpServer server;
    private final UrlConnectionHttpClient client = UrlConnectionHttpClient.create();

    @BeforeEach
    void setUp() throws IOException {
        server = new RawHttpServer((request, out) -> {
            out.write(("HTTP/1.1 200 OK\r\nX-B: 1\r\nx-a: 2\r\nX-B: 3\r\nX-Empty: \r\nContent-Length: 2\r\n\r\nok")
                          .getBytes(StandardCharsets.ISO_8859_1));
            return true;
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    private SdkHttpResponse send(SdkHttpRequest request) {
        try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            SdkHttpResponse response = exchange.execute();
            readAll(response);
            return response;
        }
    }

    @Test
    void repeatedAndInterleavedHeadersAreSentAsSeparateLinesInInsertionOrder() throws Exception {
        send(get(server.port(), "/").addHeader("X-Rep", "1").addHeader("x-Other", "a").addHeader("X-Rep", "2, 3")
                                    .addHeader("x-rep", "4").addHeader("Accept", "application/json").build());
        RawHttpServer.Request received = server.take();
        assertEquals("GET / HTTP/1.1", received.requestLine());
        assertEquals(List.of("X-Rep: 1", "x-Other: a", "X-Rep: 2, 3", "x-rep: 4", "Accept: application/json",
                             "User-Agent: " + JAVA_UA, "Host: 127.0.0.1:" + server.port(),
                             "Connection: keep-alive"),
                     received.headerLines());
    }

    @Test
    void responseHeadersKeepWireOrderCaseAndRepeats() {
        SdkHttpResponse response = send(get(server.port(), "/").build());
        HttpHeaders headers = response.headers();
        assertEquals("X-B", headers.name(0));
        assertEquals("1", headers.value(0));
        assertEquals("x-a", headers.name(1));
        assertEquals("X-B", headers.name(2));
        assertEquals("3", headers.value(2));
        assertEquals(List.of("1", "3"), headers.allValues("x-b"));
        assertEquals("", headers.firstValue("X-Empty").orElseThrow());
    }

    @Test
    void rawTargetIsSentWithoutReEncoding() throws Exception {
        send(get(server.port(), "/a%2Fb/%7Ec/~d%20e").addRawQueryParameter("x", "%20y")
                                                    .addRawQueryParameter("x", "")
                                                    .addRawQueryParameter("flag", null)
                                                    .addRawQueryParameter("k%3D", "v%26w").build());
        assertEquals("GET /a%2Fb/%7Ec/~d%20e?x=%20y&x=&flag&k%3D=v%26w HTTP/1.1", server.take().requestLine());
    }

    @Test
    void jdkOwnedHeadersAreAcceptedWhenExactAndSentAfterCallerHeaders() throws Exception {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        send(request("PUT", "http", server.port(), "/put").addHeader("Host", "127.0.0.1:" + server.port())
                                                          .addHeader("Content-Length", "5")
                                                          .addHeader("Content-Type", "text/plain")
                                                          .addHeader("Connection", "close")
                                                          .body(HttpBody.fromBytes(body)).build());
        RawHttpServer.Request received = server.take();
        assertEquals(List.of("Content-Type: text/plain", "Connection: close", "User-Agent: " + JAVA_UA,
                             "Host: 127.0.0.1:" + server.port(), "Accept: */*", "Content-Length: 5"),
                     received.headerLines());
        assertArrayEquals(body, received.body());
    }

    @Test
    void jdkAddsAFormContentTypeToANonPutBodyWithoutOne() throws Exception {
        send(request("POST", "http", server.port(), "/").body(HttpBody.fromUtf8("a=b")).build());
        RawHttpServer.Request received = server.take();
        assertEquals("application/x-www-form-urlencoded", received.header("Content-type"));
        assertEquals("3", received.header("Content-Length"));
    }

    @Test
    void unknownLengthBodiesAreChunkedAndKnownLengthBodiesAreFixed() throws Exception {
        byte[] data = new byte[20_000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        send(request("PUT", "http", server.port(), "/chunked")
                 .body(HttpBody.streaming(StreamingBodies.fromInputStream(new ByteArrayInputStream(data), -1, null)))
                 .build());
        RawHttpServer.Request chunked = server.take();
        assertEquals("chunked", chunked.header("Transfer-Encoding"));
        assertNull(chunked.header("Content-Length"));
        assertArrayEquals(data, chunked.body());

        send(request("PUT", "http", server.port(), "/fixed")
                 .body(HttpBody.streaming(StreamingBodies.fromInputStream(new ByteArrayInputStream(data),
                                                                          data.length, null)))
                 .build());
        RawHttpServer.Request fixed = server.take();
        assertEquals(String.valueOf(data.length), fixed.header("Content-Length"));
        assertNull(fixed.header("Transfer-Encoding"));
        assertArrayEquals(data, fixed.body());
    }

    @Test
    void explicitChunkedTransferEncodingIsHonoured() throws Exception {
        send(request("POST", "http", server.port(), "/").addHeader("Transfer-Encoding", "chunked")
                                                        .body(HttpBody.fromUtf8("abc")).build());
        RawHttpServer.Request received = server.take();
        assertEquals("chunked", received.header("Transfer-Encoding"));
        assertEquals("abc", new String(received.body(), StandardCharsets.UTF_8));
    }

    @Test
    void emptyBodyWithContentLengthZeroIsSent() throws Exception {
        send(request("POST", "http", server.port(), "/").addHeader("Content-Length", "0").build());
        assertEquals("0", server.take().header("Content-Length"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Connection: keep-alive", "Keep-Alive: timeout=5", "Via: 1.1 proxy", "Origin: x",
                            "Upgrade: h2c", "Trailer: X", "Content-Transfer-Encoding: binary",
                            "Access-Control-Request-Method: GET", "Access-Control-Request-Headers: x",
                            "Sec-Fetch-Mode: cors"})
    void restrictedHeadersTheJdkWouldDropFailInsteadOfBeingSilentlyRemoved(String header) {
        String[] parts = header.split(": ", 2);
        SdkClientException e = assertThrows(SdkClientException.class, () -> client.prepare(
            get(server.port(), "/").addHeader(parts[0], parts[1]).build(), HttpExecutionOptions.defaults()));
        assertTrue(e.getMessage().contains(parts[0]), e.getMessage());
        assertTrue(e.getMessage().contains("sun.net.http.allowRestrictedHeaders"), e.getMessage());
        assertEquals(0, server.connections());
    }

    @Test
    void requestsTheJdkCannotTransmitExactlyFailBeforeAnyIo() {
        int port = server.port();
        List<SdkHttpRequest> unsendable = List.of(
            get(port, "/").addHeader("Host", "other.example.com").build(),
            get(port, "/").addHeader("Host", "127.0.0.1:" + port).addHeader("Host", "127.0.0.1:" + port).build(),
            request("PUT", "http", port, "/").addHeader("Content-Length", "4").body(HttpBody.fromUtf8("abc")).build(),
            request("PUT", "http", port, "/").addHeader("Content-Length", "3").addHeader("Content-Length", "3")
                                             .body(HttpBody.fromUtf8("abc")).build(),
            request("PUT", "http", port, "/").addHeader("Content-Length", "-1").build(),
            request("PUT", "http", port, "/").addHeader("Transfer-Encoding", "gzip, chunked")
                                             .body(HttpBody.fromUtf8("abc")).build(),
            request("PUT", "http", port, "/").addHeader("Transfer-Encoding", "chunked")
                                             .addHeader("Content-Length", "3").body(HttpBody.fromUtf8("abc")).build(),
            get(port, "/").body(HttpBody.fromUtf8("a GET body would be sent as POST")).build(),
            request("HEAD", "http", port, "/").addHeader("Content-Length", "0").build(),
            request("PATCH", "http", port, "/").body(HttpBody.fromUtf8("x")).build());
        for (SdkHttpRequest r : unsendable) {
            assertThrows(SdkClientException.class, () -> client.prepare(r, HttpExecutionOptions.defaults()),
                         r::toString);
        }
        assertEquals(0, server.connections());
    }

    @Test
    void jdkHostOmitsTheDefaultPort() {
        assertEquals("example.com", UrlConnectionRequestPlan.jdkHost(
            SdkHttpRequest.builder().method("GET").scheme("https").host("example.com").port(443).encodedPath("/")
                          .build()));
        assertEquals("example.com:8443", UrlConnectionRequestPlan.jdkHost(
            SdkHttpRequest.builder().method("GET").scheme("https").host("example.com").port(8443).encodedPath("/")
                          .build()));
    }
}
