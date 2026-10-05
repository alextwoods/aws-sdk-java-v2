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

package software.amazon.awssdk.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * The sync SPI contract (Requirements 12.1, 12.7) exercised through a minimal in-memory transport: request body
 * streams are opened once and closed by the transport, the response stream releases or discards the connection
 * exactly once, abort fails a blocked read with {@link HttpFailureCategory#CANCELLED}, and close is idempotent.
 */
class SyncHttpSpiTest {
    /** An in-memory sync transport whose "connection" is counted; the response body blocks until released. */
    static final class InMemorySyncHttpClient implements SdkSyncHttpClient {
        final AtomicInteger released = new AtomicInteger();
        final AtomicInteger discarded = new AtomicInteger();
        final CountDownLatch bodyGate = new CountDownLatch(1);
        final byte[] responseBody;
        volatile byte[] lastRequestBody;
        private volatile boolean closed;

        InMemorySyncHttpClient(byte[] responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            if (closed) {
                throw new IllegalStateException("closed");
            }
            return new Exchange(request);
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(8192).cancellationTimeout(Duration.ofSeconds(1))
                                         .build();
        }

        @Override
        public String clientName() {
            return "InMemorySync";
        }

        @Override
        public void close() {
            closed = true;
        }

        private final class Exchange implements SyncHttpExchange {
            private final SdkHttpRequest request;
            private final AtomicInteger connection = new AtomicInteger(); // 0 open, 1 released/discarded
            private volatile boolean aborted;
            private boolean executed;
            private InputStream response;

            Exchange(SdkHttpRequest request) {
                this.request = request;
            }

            @Override
            public SdkHttpResponse execute() {
                if (executed) {
                    throw new IllegalStateException("execute() was already called");
                }
                executed = true;
                requireNotAborted(HttpPhase.REQUEST_TRANSMISSION);
                HttpBody body = request.body();
                if (body.kind() == HttpBody.Kind.STREAMING) {
                    try (InputStream in = body.streamingBody().openStream()) {
                        lastRequestBody = in.readAllBytes();
                    } catch (IOException e) {
                        throw HttpTransportException.create(HttpPhase.REQUEST_TRANSMISSION,
                                                            HttpFailureCategory.Kind.IO, null, e);
                    }
                } else {
                    lastRequestBody = body.asByteArray();
                }
                response = new ConnectionStream();
                return SdkHttpResponse.builder().statusCode(200).addHeader("Content-Length", "" + responseBody.length)
                                      .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                          response, responseBody.length, null)))
                                      .build();
            }

            @Override
            public void abort() {
                aborted = true;
                finish(false);
            }

            @Override
            public void close() {
                finish(false);
            }

            private void requireNotAborted(HttpPhase phase) {
                if (aborted) {
                    throw HttpTransportException.builder().phase(phase).category(HttpFailureCategory.CANCELLED)
                                                .clientName(clientName()).build();
                }
            }

            /** Releases (fully read) or discards the connection, once. */
            private void finish(boolean fullyRead) {
                if (connection.compareAndSet(0, 1)) {
                    (fullyRead ? released : discarded).incrementAndGet();
                }
            }

            private final class ConnectionStream extends InputStream {
                private final ByteArrayInputStream data = new ByteArrayInputStream(responseBody);

                @Override
                public int read() {
                    byte[] one = new byte[1];
                    return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    try {
                        while (!bodyGate.await(10, TimeUnit.MILLISECONDS)) {
                            requireNotAborted(HttpPhase.RESPONSE_CONSUMPTION);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new UncheckedIOException(new IOException(e));
                    }
                    requireNotAborted(HttpPhase.RESPONSE_CONSUMPTION);
                    return data.read(b, off, len);
                }

                @Override
                public void close() {
                    finish(data.available() == 0);
                }
            }
        }
    }

    @Test
    void responseStreamReadToTheEndReleasesTheConnectionOnce() throws IOException {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        InMemorySyncHttpClient client = new InMemorySyncHttpClient(body);
        client.bodyGate.countDown();
        SdkHttpRequest request = SdkHttpRequest.builder().method("PUT").host("h")
                                               .body(HttpBody.streaming(StreamingBodies.fromBytes(new byte[] {1, 2},
                                                                                                  null)))
                                               .build();
        try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            SdkHttpResponse response = exchange.execute();
            assertThrows(IllegalStateException.class, exchange::execute);
            try (InputStream in = response.body().streamingBody().openStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                assertArrayEquals(body, out.toByteArray());
            }
        }
        assertArrayEquals(new byte[] {1, 2}, client.lastRequestBody);
        assertEquals(1, client.released.get());
        assertEquals(0, client.discarded.get());
        client.close();
        client.close();
        assertThrows(IllegalStateException.class, () -> client.prepare(
            SdkHttpRequest.builder().method("GET").host("h").build(), HttpExecutionOptions.defaults()));
    }

    @Test
    void closingBeforeTheBodyIsConsumedDiscardsTheConnection() {
        InMemorySyncHttpClient client = new InMemorySyncHttpClient(new byte[100]);
        try (SyncHttpExchange exchange = client.prepare(SdkHttpRequest.builder().method("GET").host("h").build(),
                                                        HttpExecutionOptions.defaults())) {
            exchange.execute();
        }
        assertEquals(0, client.released.get());
        assertEquals(1, client.discarded.get());
    }

    @Test
    void abortFromAnotherThreadFailsABlockedReadAsCancelled() throws Exception {
        InMemorySyncHttpClient client = new InMemorySyncHttpClient(new byte[10]);
        SyncHttpExchange exchange = client.prepare(SdkHttpRequest.builder().method("GET").host("h").build(),
                                                   HttpExecutionOptions.defaults());
        SdkHttpResponse response = exchange.execute();
        Thread aborter = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.abort();
            exchange.abort();
        });
        aborter.start();
        long start = System.nanoTime();
        try (InputStream in = response.body().streamingBody().openStream()) {
            HttpTransportException e = assertThrows(HttpTransportException.class, in::read);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        }
        aborter.join();
        exchange.close();
        long elapsed = System.nanoTime() - start;
        assertEquals(true, elapsed < client.capabilities().cancellationTimeout().toNanos() + 50_000_000L);
        assertEquals(1, client.discarded.get());
        assertEquals(0, client.released.get());
    }
}
