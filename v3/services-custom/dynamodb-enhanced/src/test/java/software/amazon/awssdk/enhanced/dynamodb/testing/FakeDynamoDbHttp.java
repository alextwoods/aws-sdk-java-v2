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

package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNodeParser;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * A sync and async HTTP transport that serves awsJson1.0 DynamoDB requests from an {@link InMemoryDynamoDb}, so the
 * enhanced clients run over the real generated {@link DynamoDbClient} and {@link DynamoDbAsyncClient} pipelines
 * (serialization, signing, retries, error mapping, cancellation). Nothing leaves the JVM.
 *
 * <p>Faults are scripted per request with {@link #inject}: each queued {@link Fault} replaces the service's answer to
 * the next request. Every received request and every exchange cancellation or abort is recorded.
 */
public final class FakeDynamoDbHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
    public static final URI ENDPOINT = URI.create("https://dynamodb.test");

    /** Static test credentials, so the SigV4 clients never consult the default chain. */
    public static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS = new IdentityProvider<>() {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(
                AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"));
        }
    };

    private final InMemoryDynamoDb service;
    private final HttpTransportMode mode;
    private final Queue<Fault> faults = new ConcurrentLinkedQueue<>();
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private final List<String> userAgents = new CopyOnWriteArrayList<>();
    private final List<CancellationReason> cancellations = new CopyOnWriteArrayList<>();
    private final AtomicInteger aborts = new AtomicInteger();
    private final AtomicInteger openExchanges = new AtomicInteger();
    private final CountDownLatch hanging = new CountDownLatch(1);

    private FakeDynamoDbHttp(InMemoryDynamoDb service, HttpTransportMode mode) {
        this.service = service;
        this.mode = mode;
    }

    public static FakeDynamoDbHttp sync(InMemoryDynamoDb service) {
        return new FakeDynamoDbHttp(service, HttpTransportMode.SYNC);
    }

    public static FakeDynamoDbHttp async(InMemoryDynamoDb service) {
        return new FakeDynamoDbHttp(service, HttpTransportMode.ASYNC);
    }

    /** A sync client over {@code http} that retries up to {@code maxAttempts} times without backoff. */
    public static DynamoDbClient syncClient(FakeDynamoDbHttp http, int maxAttempts) {
        return DynamoDbClient.builder().region(Region.of("us-east-1")).credentialsProvider(CREDENTIALS)
                             .endpointOverride(ENDPOINT).httpClient(http).retryStrategy(retries(maxAttempts))
                             .build();
    }

    /** An async client over {@code http} that retries up to {@code maxAttempts} times without backoff. */
    public static DynamoDbAsyncClient asyncClient(FakeDynamoDbHttp http, int maxAttempts) {
        return DynamoDbAsyncClient.builder().region(Region.of("us-east-1")).credentialsProvider(CREDENTIALS)
                                  .endpointOverride(ENDPOINT).httpClient(http).retryStrategy(retries(maxAttempts))
                                  .build();
    }

    /** A standard retry strategy of {@code maxAttempts} attempts without backoff or circuit breaking. */
    public static StandardRetryStrategy retries(int maxAttempts) {
        return StandardRetryStrategy.builder().maxAttempts(maxAttempts).backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none())
                                    .circuitBreakerEnabled(false).build();
    }

    /** Queues faults answering the next requests, in order. */
    public FakeDynamoDbHttp inject(Fault... next) {
        faults.addAll(List.of(next));
        return this;
    }

    public List<Recorded> requests() {
        return List.copyOf(requests);
    }

    /** The User-Agent header of each received request, in order ({@code "<none>"} without one). */
    public List<String> userAgents() {
        return List.copyOf(userAgents);
    }

    /** Operations of the received requests, in order. */
    public List<String> operations() {
        return requests.stream().map(Recorded::operation).toList();
    }

    public List<CancellationReason> cancellations() {
        return List.copyOf(cancellations);
    }

    public int aborts() {
        return aborts.get();
    }

    /** Exchanges that were started and have not yet completed, failed, been cancelled or been closed. */
    public int openExchanges() {
        return openExchanges.get();
    }

    /** Waits until a {@link Fault#hang()} request has been received. */
    public boolean awaitHangingRequest(Duration timeout) throws InterruptedException {
        return hanging.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private Recorded record(SdkHttpRequest request) {
        ByteBuffer buffer = request.body().buffered().asByteBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String target = request.headers().firstValue("X-Amz-Target").orElse("");
        Recorded recorded = new Recorded(target.substring(target.indexOf('.') + 1),
                                         new String(bytes, StandardCharsets.UTF_8), System.nanoTime());
        requests.add(recorded);
        userAgents.add(request.headers().firstValue("User-Agent").orElse("<none>"));
        return recorded;
    }

    private SdkHttpResponse serve(Recorded recorded) {
        try {
            String body = service.execute(recorded.operation(), JsonNodeParser.create().parse(recorded.body()));
            return json(200, body);
        } catch (DynamoDbError e) {
            return json(e.status(), e.body());
        }
    }

    private static SdkHttpResponse json(int status, String body) {
        return SdkHttpResponse.builder().statusCode(status)
                              .addHeader("Content-Type", "application/x-amz-json-1.0")
                              .body(HttpBody.fromUtf8(body)).build();
    }

    // ---- sync ------------------------------------------------------------------------------------------------------

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        openExchanges.incrementAndGet();
        CountDownLatch aborted = new CountDownLatch(1);
        return new SyncHttpExchange() {
            private boolean closed;

            @Override
            public SdkHttpResponse execute() {
                Recorded recorded = record(request);
                Fault fault = faults.poll();
                if (fault == null) {
                    return serve(recorded);
                }
                return switch (fault.kind) {
                    case RESPONSE -> fault.response();
                    case MALFORMED -> json(200, fault.body);
                    case TRANSPORT -> throw fault.transportFailure();
                    case PARTIAL_RESPONSE -> partial(serve(recorded));
                    case HANG -> {
                        hanging.countDown();
                        try {
                            aborted.await(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        throw HttpTransportException.create(HttpPhase.RESPONSE_CONSUMPTION,
                                                            HttpFailureCategory.Kind.CANCELLED, "aborted", null);
                    }
                };
            }

            @Override
            public void abort() {
                aborts.incrementAndGet();
                aborted.countDown();
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    openExchanges.decrementAndGet();
                }
            }
        };
    }

    /** {@code response} with a streamed body that fails after half of its declared bytes. */
    private static SdkHttpResponse partial(SdkHttpResponse response) {
        byte[] full = response.body().asByteArray();
        int half = full.length / 2;
        InputStream truncated = new InputStream() {
            private final ByteArrayInputStream delegate = new ByteArrayInputStream(full, 0, half);

            @Override
            public int read() throws IOException {
                int b = delegate.read();
                if (b < 0) {
                    throw new IOException("Connection reset while reading the response body");
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int n = delegate.read(b, off, len);
                if (n < 0) {
                    throw new IOException("Connection reset while reading the response body");
                }
                return n;
            }
        };
        return response.toBuilder()
                       .body(HttpBody.streaming(StreamingBodies.fromInputStream(truncated, full.length,
                                                                                "application/x-amz-json-1.0")))
                       .build();
    }

    // ---- async -----------------------------------------------------------------------------------------------------

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        openExchanges.incrementAndGet();
        AtomicInteger open = new AtomicInteger(1);
        Runnable release = () -> {
            if (open.getAndSet(0) == 1) {
                openExchanges.decrementAndGet();
            }
        };
        AsyncExchangeCompletion exchange = new AsyncExchangeCompletion(clientName(), (reason, failure) -> {
            cancellations.add(reason);
            release.run();
            handler.onError(failure);
        });
        exchange.completion().whenComplete((r, t) -> release.run());
        Recorded recorded = record(request);
        Fault fault = faults.poll();
        if (fault != null && fault.kind == Fault.Kind.HANG) {
            exchange.enter(HttpPhase.RESPONSE_CONSUMPTION);
            hanging.countDown();
            return exchange;
        }
        if (fault != null && fault.kind == Fault.Kind.TRANSPORT) {
            HttpTransportException failure = fault.transportFailure();
            exchange.fail(failure);
            handler.onError(failure);
            return exchange;
        }
        SdkHttpResponse response;
        if (fault == null || fault.kind == Fault.Kind.PARTIAL_RESPONSE) {
            response = serve(recorded);
        } else if (fault.kind == Fault.Kind.MALFORMED) {
            response = json(200, fault.body);
        } else {
            response = fault.response();
        }
        exchange.enter(HttpPhase.RESPONSE_CONSUMPTION);
        byte[] body = response.body().asByteArray();
        handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
        if (fault != null && fault.kind == Fault.Kind.PARTIAL_RESPONSE) {
            byte[] half = java.util.Arrays.copyOf(body, body.length / 2);
            IOException reset = new IOException("Connection reset while reading the response body");
            handler.onStream(publisherOf(half, reset));
            exchange.fail(reset);
        } else {
            handler.onStream(publisherOf(body, null));
            exchange.complete();
        }
        return exchange;
    }

    private static Publisher<ByteBuffer> publisherOf(byte[] bytes, Throwable failure) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes).asReadOnlyBuffer());
                }
                if (failure == null) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(failure);
                }
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(1 << 20)
                                     .cancellationTimeout(Duration.ofSeconds(1)).build();
    }

    @Override
    public String clientName() {
        return "FakeDynamoDbHttp";
    }

    @Override
    public void close() {
    }

    /** A request the transport received: its operation name, JSON body and arrival time. */
    public record Recorded(String operation, String body, long nanoTime) {
    }

    /** A scripted answer to one request. */
    public static final class Fault {
        enum Kind { RESPONSE, MALFORMED, TRANSPORT, PARTIAL_RESPONSE, HANG }

        private final Kind kind;
        private final int status;
        private final String body;
        private final String header;
        private final HttpPhase phase;
        private final HttpFailureCategory.Kind failureKind;

        private Fault(Kind kind, int status, String body, String header, HttpPhase phase,
                      HttpFailureCategory.Kind failureKind) {
            this.kind = kind;
            this.status = status;
            this.body = body;
            this.header = header;
            this.phase = phase;
            this.failureKind = failureKind;
        }

        /** An awsJson1.0 error response of {@code type} (a DynamoDB error shape name, or any other code). */
        public static Fault error(int status, String type, String message) {
            return new Fault(Kind.RESPONSE, status, new DynamoDbError(status, type, message, null).body(), null, null,
                             null);
        }

        /** A {@code 400 ProvisionedThroughputExceededException} throttling response. */
        public static Fault throttling() {
            return error(400, "ProvisionedThroughputExceededException", "The level of configured provisioned "
                                                                        + "throughput for the table was exceeded.");
        }

        /** A throttling response carrying an {@code x-amz-retry-after} hint of {@code millis}. */
        public static Fault throttlingWithRetryAfter(long millis) {
            Fault throttling = throttling();
            return new Fault(Kind.RESPONSE, throttling.status, throttling.body, Long.toString(millis), null, null);
        }

        /** A {@code 500 InternalServerError} response. */
        public static Fault internalServerError() {
            return error(500, "InternalServerError", "Internal server error");
        }

        /** A {@code 200} response whose body is not a valid awsJson document. */
        public static Fault malformedSuccess() {
            return new Fault(Kind.MALFORMED, 200, "{\"Item\": {\"id\": ", null, null, null);
        }

        /** A connection failure before the request was sent. */
        public static Fault connectionFailure() {
            return new Fault(Kind.TRANSPORT, 0, null, null, HttpPhase.CONNECT, HttpFailureCategory.Kind.IO);
        }

        /** A failure while the request body was being sent. */
        public static Fault partialRequestBody() {
            return new Fault(Kind.TRANSPORT, 0, null, null, HttpPhase.REQUEST_TRANSMISSION,
                             HttpFailureCategory.Kind.IO);
        }

        /** A transport read timeout while waiting for the response. */
        public static Fault timeout() {
            return new Fault(Kind.TRANSPORT, 0, null, null, HttpPhase.RESPONSE_CONSUMPTION,
                             HttpFailureCategory.Kind.TIMEOUT);
        }

        /** The service's real answer, whose body fails after half of its bytes. */
        public static Fault partialResponseBody() {
            return new Fault(Kind.PARTIAL_RESPONSE, 0, null, null, null, null);
        }

        /** No answer until the exchange is cancelled (async) or aborted (sync). */
        public static Fault hang() {
            return new Fault(Kind.HANG, 0, null, null, null, null);
        }

        SdkHttpResponse response() {
            SdkHttpResponse response = json(status, body);
            return header == null ? response : response.toBuilder().addHeader("x-amz-retry-after", header).build();
        }

        HttpTransportException transportFailure() {
            return HttpTransportException.create(phase, failureKind, "Injected " + failureKind + " failure in "
                                                                     + phase, new IOException("injected"));
        }
    }
}
