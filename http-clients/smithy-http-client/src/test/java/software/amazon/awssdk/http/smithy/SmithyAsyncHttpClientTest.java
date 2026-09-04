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

package software.amazon.awssdk.http.smithy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Behavioural tests for {@link SmithyAsyncHttpClient} over plaintext HTTP.
 *
 * <p>The SDK's shared {@code SdkAsyncHttpClientH1TestSuite} cannot be used here: it drives every request over
 * HTTPS against a self-signed certificate and relies on {@code TRUST_ALL_CERTIFICATES}, which this client
 * cannot fully honour because smithy always enables hostname verification (see {@link SmithyClientOptions}).
 * These tests therefore cover the same behaviours — status handling, body integrity, HEAD, connection reuse,
 * and failure propagation — over HTTP, which is also the configuration this client is benchmarked in.
 */
public class SmithyAsyncHttpClientTest {

    private static WireMockServer server;
    private SdkAsyncHttpClient client;

    @BeforeAll
    public static void startServer() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @AfterEach
    public void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
        server.resetAll();
    }

    private SdkAsyncHttpClient client() {
        client = SmithyAsyncHttpClient.builder().build();
        return client;
    }

    private static SdkHttpFullRequest request(String path, SdkHttpMethod method) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:" + server.port() + path))
                                 .method(method)
                                 .putHeader("Host", "localhost:" + server.port())
                                 .build();
    }

    @Test
    public void deliversStatusHeadersAndBody() throws Exception {
        server.stubFor(any(urlPathEqualTo("/ok")).willReturn(aResponse()
                                                                .withStatus(200)
                                                                .withHeader("x-amzn-RequestId", "abc123")
                                                                .withBody("hello")));

        RecordingHandler handler = new RecordingHandler();
        CompletableFuture<Void> future = client().execute(AsyncExecuteRequest.builder()
                                                                            .request(request("/ok", SdkHttpMethod.GET))
                                                                            .requestContentPublisher(new EmptyBody())
                                                                            .responseHandler(handler)
                                                                            .build());

        future.get(30, TimeUnit.SECONDS);

        assertThat(handler.statusCode).isEqualTo(200);
        assertThat(handler.requestId).isEqualTo("abc123");
        assertThat(handler.body()).isEqualTo("hello");
        assertThat(handler.error).isNull();
    }

    /**
     * An error status is a normal response at this layer: the future must succeed and the body must be
     * readable, because the SDK parses it to build the exception.
     */
    @Test
    public void errorStatusCompletesNormallyWithBody() throws Exception {
        server.stubFor(any(urlPathEqualTo("/err")).willReturn(aResponse()
                                                                 .withStatus(500)
                                                                 .withBody("{\"__type\":\"Broken\"}")));

        RecordingHandler handler = new RecordingHandler();
        client().execute(AsyncExecuteRequest.builder()
                                            .request(request("/err", SdkHttpMethod.GET))
                                            .requestContentPublisher(new EmptyBody())
                                            .responseHandler(handler)
                                            .build())
                .get(30, TimeUnit.SECONDS);

        assertThat(handler.statusCode).isEqualTo(500);
        assertThat(handler.body()).contains("Broken");
        assertThat(handler.error).isNull();
    }

    @Test
    public void headResponseHasNoPayload() throws Exception {
        server.stubFor(any(urlPathEqualTo("/head")).willReturn(aResponse()
                                                                  .withStatus(200)
                                                                  .withHeader("Content-Length", "1234")));

        RecordingHandler handler = new RecordingHandler();
        client().execute(AsyncExecuteRequest.builder()
                                            .request(request("/head", SdkHttpMethod.HEAD))
                                            .requestContentPublisher(new EmptyBody())
                                            .responseHandler(handler)
                                            .build())
                .get(30, TimeUnit.SECONDS);

        assertThat(handler.statusCode).isEqualTo(200);
        assertThat(handler.body()).isEmpty();
        assertThat(handler.streamCalls).isEqualTo(1);
    }

    /**
     * The request body must reach the server intact. This exercises the reactive-streams to {@code Flow}
     * bridge that hands the SDK's publisher to smithy.
     */
    @Test
    public void sendsRequestBody() throws Exception {
        server.stubFor(any(urlPathEqualTo("/put")).willReturn(aResponse().withStatus(200).withBody("ok")));

        String payload = "{\"TableName\":\"racecar\",\"Item\":{}}";
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        SdkHttpFullRequest req = SdkHttpFullRequest.builder()
                                                   .uri(URI.create("http://localhost:" + server.port() + "/put"))
                                                   .method(SdkHttpMethod.POST)
                                                   .putHeader("Host", "localhost:" + server.port())
                                                   .putHeader("Content-Length", String.valueOf(bytes.length))
                                                   .build();

        RecordingHandler handler = new RecordingHandler();
        client().execute(AsyncExecuteRequest.builder()
                                            .request(req)
                                            .requestContentPublisher(new FixedBody(bytes))
                                            .responseHandler(handler)
                                            .build())
                .get(30, TimeUnit.SECONDS);

        assertThat(handler.statusCode).isEqualTo(200);
        server.verify(1, postRequestedFor(urlPathEqualTo("/put")).withRequestBody(equalTo(payload)));
    }

    /**
     * A body larger than a single socket read must be reassembled exactly.
     */
    @Test
    public void readsLargeBodyIntact() throws Exception {
        byte[] body = new byte[512 * 1024];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) ('A' + (i % 26));
        }
        server.stubFor(any(urlPathEqualTo("/big")).willReturn(aResponse().withStatus(200).withBody(body)));

        RecordingHandler handler = new RecordingHandler();
        client().execute(AsyncExecuteRequest.builder()
                                            .request(request("/big", SdkHttpMethod.GET))
                                            .requestContentPublisher(new EmptyBody())
                                            .responseHandler(handler)
                                            .build())
                .get(30, TimeUnit.SECONDS);

        assertThat(handler.bodyBytes()).isEqualTo(body);
    }

    /**
     * Sequential requests through one client must reuse the pooled connection rather than opening a new one
     * each time; that pooling is a large part of why this client is interesting.
     */
    @Test
    public void reusesConnectionAcrossRequests() throws Exception {
        server.stubFor(any(urlPathEqualTo("/reuse")).willReturn(aResponse().withStatus(200).withBody("x")));

        SdkAsyncHttpClient c = client();
        for (int i = 0; i < 5; i++) {
            RecordingHandler handler = new RecordingHandler();
            c.execute(AsyncExecuteRequest.builder()
                                         .request(request("/reuse", SdkHttpMethod.GET))
                                         .requestContentPublisher(new EmptyBody())
                                         .responseHandler(handler)
                                         .build())
             .get(30, TimeUnit.SECONDS);
            assertThat(handler.statusCode).isEqualTo(200);
        }

        server.verify(5, com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlPathEqualTo("/reuse")));
    }

    /**
     * A connection failure must both fail the returned future and be reported to the handler, matching what
     * the SDK's own async clients do.
     */
    @Test
    public void connectionFailureFailsFutureAndHandler() {
        RecordingHandler handler = new RecordingHandler();
        // Port 1 is not listening.
        SdkHttpFullRequest req = SdkHttpFullRequest.builder()
                                                   .uri(URI.create("http://localhost:1/nope"))
                                                   .method(SdkHttpMethod.GET)
                                                   .putHeader("Host", "localhost:1")
                                                   .build();

        CompletableFuture<Void> future = client().execute(AsyncExecuteRequest.builder()
                                                                            .request(req)
                                                                            .requestContentPublisher(new EmptyBody())
                                                                            .responseHandler(handler)
                                                                            .build());

        assertThatThrownBy(() -> future.get(30, TimeUnit.SECONDS)).isInstanceOf(java.util.concurrent.ExecutionException.class);
        assertThat(handler.error).isNotNull();
    }

    @Test
    public void reportsClientName() {
        assertThat(client().clientName()).isEqualTo("SmithyAsync");
    }

    /**
     * Closing the client must stop it serving new requests rather than silently hanging.
     */
    @Test
    public void closedClientRejectsNewRequests() {
        SdkAsyncHttpClient c = SmithyAsyncHttpClient.builder().build();
        c.close();

        CompletableFuture<Void> future = c.execute(AsyncExecuteRequest.builder()
                                                                     .request(request("/ok", SdkHttpMethod.GET))
                                                                     .requestContentPublisher(new EmptyBody())
                                                                     .responseHandler(new RecordingHandler())
                                                                     .build());

        assertThatThrownBy(() -> future.get(30, TimeUnit.SECONDS))
            .isInstanceOfAny(java.util.concurrent.ExecutionException.class,
                             java.util.concurrent.RejectedExecutionException.class,
                             CompletionException.class);
    }

    private static final class EmptyBody implements SdkHttpContentPublisher {
        @Override
        public java.util.Optional<Long> contentLength() {
            return java.util.Optional.of(0L);
        }

        @Override
        public void subscribe(org.reactivestreams.Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new org.reactivestreams.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onComplete();
        }
    }

    private static final class FixedBody implements SdkHttpContentPublisher {
        private final byte[] payload;

        private FixedBody(byte[] payload) {
            this.payload = payload;
        }

        @Override
        public java.util.Optional<Long> contentLength() {
            return java.util.Optional.of((long) payload.length);
        }

        @Override
        public void subscribe(org.reactivestreams.Subscriber<? super ByteBuffer> subscriber) {
            SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
            publisher.subscribe(subscriber);
            publisher.send(ByteBuffer.wrap(payload));
            publisher.complete();
        }
    }

    private static final class RecordingHandler implements software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler {
        private volatile int statusCode = -1;
        private volatile String requestId;
        private volatile Throwable error;
        private volatile int streamCalls;
        private final java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
        private final CompletableFuture<Void> streamDone = new CompletableFuture<>();

        @Override
        public void onHeaders(software.amazon.awssdk.http.SdkHttpResponse headers) {
            statusCode = headers.statusCode();
            requestId = headers.firstMatchingHeader("x-amzn-RequestId").orElse(null);
        }

        @Override
        public void onStream(org.reactivestreams.Publisher<ByteBuffer> stream) {
            streamCalls++;
            stream.subscribe(new org.reactivestreams.Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(org.reactivestreams.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    byte[] chunk = new byte[byteBuffer.remaining()];
                    byteBuffer.get(chunk);
                    synchronized (collected) {
                        collected.write(chunk, 0, chunk.length);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    error = throwable;
                    streamDone.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    streamDone.complete(null);
                }
            });
        }

        @Override
        public void onError(Throwable e) {
            error = e;
        }

        private String body() {
            return new String(bodyBytes(), StandardCharsets.UTF_8);
        }

        private byte[] bodyBytes() {
            synchronized (collected) {
                return collected.toByteArray();
            }
        }
    }
}
