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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2ServerBootstrap;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

/** Behaviour of the isolated async {@link Apache5AsyncHttpClient}. */
class Apache5AsyncHttpClientTest {
    private RawHttpServer server;
    private Apache5AsyncHttpClient client;

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

    /** Records handler signals; its subscriber requests {@code initialDemand} chunks. */
    static final class Recorder implements AsyncHttpResponseHandler {
        final CompletableFuture<SdkHttpResponse> headers = new CompletableFuture<>();
        final CompletableFuture<Throwable> error = new CompletableFuture<>();
        final CompletableFuture<byte[]> body = new CompletableFuture<>();
        final CompletableFuture<Publisher<ByteBuffer>> stream = new CompletableFuture<>();
        final CompletableFuture<Void> firstChunk = new CompletableFuture<>();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final long initialDemand;
        volatile Subscription subscription;

        Recorder(long initialDemand) {
            this.initialDemand = initialDemand;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            headers.complete(response);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            stream.complete(publisher);
            publisher.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    s.request(initialDemand);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    assertTrue(chunk.isReadOnly());
                    byte[] b = new byte[chunk.remaining()];
                    chunk.get(b);
                    synchronized (bytes) {
                        bytes.write(b, 0, b.length);
                    }
                    firstChunk.complete(null);
                }

                @Override
                public void onError(Throwable t) {
                    body.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    synchronized (bytes) {
                        body.complete(bytes.toByteArray());
                    }
                }
            });
        }

        @Override
        public void onError(Throwable t) {
            error.complete(t);
        }
    }

    private static HttpTransportException cause(CompletableFuture<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("expected a failure");
        } catch (ExecutionException e) {
            return assertInstanceOf(HttpTransportException.class, e.getCause());
        }
    }

    @Test
    void asyncCapabilitiesAreSeparateAndTheProviderIsNotDiscoverable() {
        HttpClientCapabilities c = Apache5AsyncHttpClient.CAPABILITIES;
        assertEquals(HttpTransportMode.ASYNC, c.mode());
        assertEquals(Set.of(Protocol.HTTP1_1, Protocol.HTTP2), c.protocols());
        assertTrue(c.supports(HttpClientOption.PROTOCOL_VERSION));
        assertFalse(c.supports(HttpClientOption.EXPECT_CONTINUE));
        assertFalse(Apache5HttpClient.CAPABILITIES.supports(HttpClientOption.PROTOCOL_VERSION));
        assertEquals(256 * 1024, c.maxBufferedBytesPerExchange());
        Apache5SdkAsyncHttpService service = new Apache5SdkAsyncHttpService();
        assertEquals("apache5-async-client", service.implementationName());
        assertEquals(50, service.priority());
        assertEquals(HttpTransportMode.ASYNC, service.mode());
        assertEquals(c, service.capabilities());
        assertTrue(getClass().getClassLoader()
                             .getResource("META-INF/services/software.amazon.awssdk.http.provider.SdkAsyncHttpService")
                   == null);
    }

    @Test
    void http11ExchangeStreamsBothBodiesWithOrderedHeaders() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.respond(out, 200, r.body(), "X-R: 1", "X-R: 2"));
        client = Apache5AsyncHttpClient.builder().build();
        byte[] data = new byte[300_000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 7);
        }
        SdkHttpRequest request = get(server.port(), "/e%20cho").method("POST").addRawQueryParameter("q", "%2F")
            .addHeader("X-B", "1").addHeader("X-A", "v").addHeader("X-B", "2")
            .body(HttpBody.streaming(StreamingBodies.fromInputStream(new ByteArrayInputStream(data), -1, null)))
            .build();
        Recorder recorder = new Recorder(Long.MAX_VALUE);
        AsyncHttpExchange exchange = client.execute(request, recorder, HttpExecutionOptions.defaults());
        assertArrayEquals(data, recorder.body.get(10, TimeUnit.SECONDS));
        exchange.completion().get(5, TimeUnit.SECONDS);
        assertFalse(exchange.cancel(CancellationReason.CALLER_CANCELLED));
        assertEquals(List.of("1", "2"), recorder.headers.get().headers().allValues("X-R"));
        RawHttpServer.Request received = server.take();
        assertEquals("POST /e%20cho?q=%2F HTTP/1.1", received.requestLine());
        assertEquals(List.of("X-B", "X-A", "X-B"), received.headerNames().subList(0, 3));
        assertEquals(List.of("chunked"), received.values("Transfer-Encoding"));
        assertEquals(0, client.inFlightCount());
    }

    @Test
    void http2IsNegotiatedByPriorKnowledge() throws Exception {
        HttpAsyncServer h2 = H2ServerBootstrap.bootstrap().setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2)
            .setCanonicalHostName("127.0.0.1")
            .register("*", new AsyncServerRequestHandler<Message<HttpRequest, String>>() {
                @Override
                public AsyncRequestConsumer<Message<HttpRequest, String>> prepare(
                    HttpRequest request, org.apache.hc.core5.http.EntityDetails details, HttpContext context) {
                    return new BasicRequestConsumer<>(new StringAsyncEntityConsumer());
                }

                @Override
                public void handle(Message<HttpRequest, String> message, ResponseTrigger trigger,
                                   HttpContext context) throws org.apache.hc.core5.http.HttpException, IOException {
                    HttpRequest head = message.getHead();
                    trigger.submitResponse(AsyncResponseBuilder.create(200)
                                                               .addHeader("x-path", head.getPath())
                                                               .setEntity(context.getProtocolVersion() + ":"
                                                                          + message.getBody())
                                                               .build(), context);
                }
            }).create();
        h2.start();
        try {
            ListenerEndpoint endpoint = h2.listen(new InetSocketAddress("127.0.0.1", 0), URIScheme.HTTP).get();
            int port = ((InetSocketAddress) endpoint.getAddress()).getPort();
            client = Apache5AsyncHttpClient.builder().protocol(Protocol.HTTP2).build();
            SdkHttpRequest request = get(port, "/a%2Fb").method("PUT").addRawQueryParameter("x", "1")
                                                       .body(HttpBody.fromUtf8("h2 body")).build();
            Recorder recorder = new Recorder(Long.MAX_VALUE);
            client.execute(request, recorder, HttpExecutionOptions.defaults());
            assertEquals("HTTP/2.0:h2 body", new String(recorder.body.get(10, TimeUnit.SECONDS),
                                                         StandardCharsets.UTF_8));
            assertEquals("/a%2Fb?x=1", recorder.headers.get().headers().firstValue("x-path").orElseThrow());
        } finally {
            h2.close(CloseMode.IMMEDIATE);
        }
    }

    @Test
    void cancelBeforeTheBodyFailsTheHandlerOnceWithinTheTimeout() throws Exception {
        server = new RawHttpServer((r, out, s) -> s.awaitRelease());
        client = Apache5AsyncHttpClient.builder().build();
        Recorder recorder = new Recorder(1);
        AsyncHttpExchange exchange = client.execute(get(server.port(), "/").build(), recorder,
                                                    HttpExecutionOptions.defaults());
        server.take();
        long start = System.nanoTime();
        assertTrue(exchange.cancel(CancellationReason.CALLER_CANCELLED));
        assertFalse(exchange.cancel(CancellationReason.CALLER_CANCELLED));
        HttpTransportException e = assertInstanceOf(HttpTransportException.class, recorder.error.get(2,
                                                                                                    TimeUnit.SECONDS));
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(client.capabilities().cancellationTimeout())
                   < 0);
        assertEquals(HttpFailureCategory.CANCELLED, e.category());
        assertEquals(HttpFailureCategory.CANCELLED, cause(exchange.completion()).category());
        assertFalse(recorder.headers.isDone());
        assertEquals(0, client.inFlightCount());
    }

    @Test
    void cancelDuringTheBodyFailsTheSubscriber() throws Exception {
        server = new RawHttpServer((r, out, s) -> {
            out.write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\nabc".getBytes(StandardCharsets.US_ASCII));
            out.flush();
            s.awaitRelease();
        });
        client = Apache5AsyncHttpClient.builder().build();
        Recorder recorder = new Recorder(1);
        AsyncHttpExchange exchange = client.execute(get(server.port(), "/").build(), recorder,
                                                    HttpExecutionOptions.defaults());
        recorder.firstChunk.get(5, TimeUnit.SECONDS);
        recorder.subscription.request(5);
        assertTrue(exchange.cancel(CancellationReason.API_CALL_TIMEOUT));
        HttpTransportException e = cause(recorder.body);
        assertEquals(HttpFailureCategory.CANCELLED, e.category());
        assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        assertFalse(recorder.error.isDone());
        assertEquals(HttpFailureCategory.CANCELLED, cause(exchange.completion()).category());
    }

    @Test
    void responseBufferingStaysWithinTheDeclaredBoundAndSubscriberCancelDiscards() throws Exception {
        byte[] big = new byte[4 * 1024 * 1024];
        server = new RawHttpServer((r, out, s) -> RawHttpServer.respond(out, 200, big));
        client = Apache5AsyncHttpClient.builder().build();
        Recorder recorder = new Recorder(1);
        AsyncHttpExchange exchange = client.execute(get(server.port(), "/").build(), recorder,
                                                    HttpExecutionOptions.defaults());
        recorder.firstChunk.get(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        ResponseBodyPublisher publisher = (ResponseBodyPublisher) recorder.stream.get();
        assertTrue(publisher.highWaterBytes() <= client.capabilities().maxBufferedBytesPerExchange(),
                   "buffered " + publisher.highWaterBytes());
        recorder.subscription.cancel();
        assertEquals(HttpFailureCategory.CANCELLED, cause(exchange.completion()).category());
        assertFalse(recorder.body.isDone());
    }

    @Test
    void readTimeoutIsReportedToTheHandler() throws Exception {
        server = new RawHttpServer((r, out, s) -> s.awaitRelease());
        client = Apache5AsyncHttpClient.builder().build();
        Recorder recorder = new Recorder(1);
        client.execute(get(server.port(), "/").build(), recorder,
                       HttpExecutionOptions.builder().readTimeout(Duration.ofMillis(300)).build());
        HttpTransportException e = assertInstanceOf(HttpTransportException.class,
                                                    recorder.error.get(5, TimeUnit.SECONDS));
        assertEquals(HttpFailureCategory.TIMEOUT, e.category(), String.valueOf(e.getCause()));
        assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
    }

    @Test
    void closeIsIdempotentAndCancelsExchangesInFlight() throws Exception {
        server = new RawHttpServer((r, out, s) -> s.awaitRelease());
        client = Apache5AsyncHttpClient.builder().build();
        Recorder recorder = new Recorder(1);
        AsyncHttpExchange exchange = client.execute(get(server.port(), "/").build(), recorder,
                                                    HttpExecutionOptions.defaults());
        server.take();
        client.close();
        client.close();
        assertEquals(HttpFailureCategory.CANCELLED, cause(exchange.completion()).category());
        assertTrue(recorder.error.get(2, TimeUnit.SECONDS).getMessage().contains("CLIENT_CLOSED"));
        try {
            client.execute(get(server.port(), "/").build(), recorder, HttpExecutionOptions.defaults());
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // closed
        }
    }

    @Test
    void explicitAsyncProviderResolvesAndRejectsUnsupportedOptions() throws Exception {
        server = new RawHttpServer((r, out, s) -> RawHttpServer.ok(out, "async"));
        ResolvedHttpClient<SdkAsyncHttpClient> resolved =
            HttpClientResolver.async().implementation(new Apache5SdkAsyncHttpService())
                              .configuration(HttpClientConfiguration.builder()
                                                                    .put(HttpClientOption.PROTOCOL_VERSION,
                                                                         Protocol.HTTP1_1).build())
                              .resolve();
        Recorder recorder = new Recorder(Long.MAX_VALUE);
        resolved.client().execute(get(server.port(), "/").build(), recorder, HttpExecutionOptions.defaults());
        assertEquals("async", new String(recorder.body.get(5, TimeUnit.SECONDS), StandardCharsets.UTF_8));
        resolved.close();
        try {
            HttpClientResolver.async().implementation(new Apache5SdkAsyncHttpService())
                              .configuration(HttpClientConfiguration.builder()
                                                                    .put(HttpClientOption.EXPECT_CONTINUE, true).build())
                              .resolve();
            throw new AssertionError("expected rejection");
        } catch (software.amazon.awssdk.http.provider.HttpImplementationSelectionException e) {
            assertEquals(List.of(HttpClientOption.EXPECT_CONTINUE), e.unsupportedOptions());
            assertTrue(e.getMessage().contains("expectContinueEnabled"), e.getMessage());
        }
    }
}
