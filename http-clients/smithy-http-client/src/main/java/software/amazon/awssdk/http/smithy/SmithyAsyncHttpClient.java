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

import static software.amazon.awssdk.http.HttpMetric.HTTP_CLIENT_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.smithy.internal.ReactiveToFlowPublisher;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.io.uri.SmithyUri;

/**
 * An asynchronous {@link SdkAsyncHttpClient} backed by smithy-java's HTTP client.
 *
 * <p>This is not one of the SDK's default HTTP clients and is not discovered by {@code ServiceLoader}; it is
 * used only when passed explicitly to a client builder.
 *
 * <p><b>This is a bridge, not a port.</b> smithy's HTTP client has no asynchronous API at all — it exposes a
 * single blocking {@code send}, and is designed to be run on virtual threads rather than to hand back futures.
 * So each request here is dispatched to a virtual thread, which is smithy's intended execution model but is
 * still a thread hand-off that a natively asynchronous client such as Netty or CRT does not pay. Treat its
 * numbers as a bridge's numbers. The synchronous {@link SmithyHttpClient} is the faithful mapping.
 *
 * <p>The response body is read to completion on the worker thread before being published, which releases the
 * pooled connection promptly but costs one copy and rules out incremental delivery of a large streamed
 * response. That is a deliberate simplification for a measurement client.
 *
 * <p>Requires Java 21, and reaches smithy's native epoll transport only on Linux. See {@link SmithyHttpClient}
 * for the full set of caveats.
 */
@SdkPublicApi
public final class SmithyAsyncHttpClient implements SdkAsyncHttpClient {

    private static final String CLIENT_NAME = "SmithyAsync";
    private static final Logger log = Logger.loggerFor(SmithyAsyncHttpClient.class);

    private final software.amazon.smithy.java.http.client.HttpClient smithyClient;
    private final ExecutorService executor;

    private SmithyAsyncHttpClient(DefaultBuilder builder, AttributeMap options) {
        this.smithyClient = SmithyClientOptions.apply(software.amazon.smithy.java.http.client.HttpClient.builder(),
                                                      builder.http2Enabled,
                                                      options)
                                               .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Creates a client with default settings.
     */
    public static SdkAsyncHttpClient create() {
        return new DefaultBuilder().build();
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        MetricCollector metricCollector = request.metricCollector().orElseGet(NoOpMetricCollector::create);
        metricCollector.reportMetric(HTTP_CLIENT_NAME, clientName());

        CompletableFuture<Void> future = new CompletableFuture<>();
        SdkAsyncHttpResponseHandler handler = request.responseHandler();

        Runnable task = () -> {
            try {
                boolean headRequest = request.request().method() == SdkHttpMethod.HEAD;
                HttpResponse response = smithyClient.send(toSmithyRequest(request));
                byte[] body;
                try {
                    body = headRequest ? new byte[0] : readFully(response);
                } finally {
                    // Release the connection before handing anything to the handler, so a slow or misbehaving
                    // subscriber cannot hold a pooled connection open.
                    closeQuietly(response);
                }

                handler.onHeaders(toSdkHeaders(response));
                handler.onStream(new SingleBufferPublisher(body, future));
            } catch (Throwable t) {
                // Both, in the order the SDK's own clients use: the handler learns why, and the caller's
                // future fails.
                safeOnError(handler, t);
                future.completeExceptionally(t);
            }
        };

        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            // A closed client must report through the future like any other failure. Letting this propagate
            // synchronously would bypass the pipeline's error handling, which only watches the future.
            safeOnError(handler, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    @Override
    public void close() {
        executor.shutdownNow();
        IoUtils.closeQuietly(smithyClient, null);
    }

    private static HttpRequest toSmithyRequest(AsyncExecuteRequest request) {
        SdkHttpRequest sdkRequest = request.request();
        ModifiableHttpRequest smithyRequest = HttpRequest.create()
                                                        .setMethod(sdkRequest.method().name())
                                                        .setUri(SmithyUri.of(sdkRequest.getUri()));
        // forEachHeader rather than headers(): the latter deep-copies every header on every request, which is
        // exactly the per-request cost this client exists to avoid.
        sdkRequest.forEachHeader(smithyRequest.headers()::setHeader);
        smithyRequest.setBody(toDataStream(request));
        return smithyRequest;
    }

    /**
     * Streams the request body straight from the SDK's publisher rather than buffering it, so a large write
     * does not get copied on its way to the socket.
     */
    private static DataStream toDataStream(AsyncExecuteRequest request) {
        long contentLength = request.requestContentPublisher().contentLength().orElse(-1L);
        if (contentLength == 0) {
            return DataStream.ofEmpty();
        }
        return DataStream.ofPublisher(new ReactiveToFlowPublisher<>(request.requestContentPublisher()),
                                      null,
                                      contentLength);
    }

    private static SdkHttpResponse toSdkHeaders(HttpResponse response) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(response.statusCode());
        response.headers().forEachEntry(builder::appendHeader);
        return builder.build();
    }

    private static byte[] readFully(HttpResponse response) throws IOException {
        DataStream body = response.body();
        if (body.contentLength() == 0) {
            return new byte[0];
        }
        try (InputStream stream = body.asInputStream()) {
            return IoUtils.toByteArray(stream);
        }
    }

    private static void safeOnError(SdkAsyncHttpResponseHandler handler, Throwable t) {
        try {
            handler.onError(t);
        } catch (RuntimeException e) {
            // A throwing handler must not stop the future from being failed.
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        IoUtils.closeQuietlyV2(closeable, log);
    }

    /**
     * Publishes an already-read body as at most one element, then completes the execute future.
     *
     * <p>Single-use: a second subscriber is completed immediately rather than replaying, matching how the
     * SDK's own clients treat their response publishers.
     */
    private static final class SingleBufferPublisher implements Publisher<ByteBuffer> {
        private final byte[] body;
        private final CompletableFuture<Void> future;
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        private SingleBufferPublisher(byte[] body, CompletableFuture<Void> future) {
            this.body = body;
            this.future = future;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            if (!subscribed.compareAndSet(false, true)) {
                subscriber.onSubscribe(new NoopSubscription());
                subscriber.onComplete();
                return;
            }

            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    if (done) {
                        return;
                    }
                    if (n <= 0) {
                        done = true;
                        subscriber.onError(new IllegalArgumentException("Demand must be positive, but was " + n));
                        future.completeExceptionally(new IllegalArgumentException("Demand must be positive"));
                        return;
                    }
                    done = true;
                    if (body.length > 0) {
                        subscriber.onNext(ByteBuffer.wrap(body));
                    }
                    subscriber.onComplete();
                    future.complete(null);
                }

                @Override
                public void cancel() {
                    if (!done) {
                        done = true;
                        // A cancelled body still ends the exchange: the connection was already released.
                        future.complete(null);
                    }
                }
            });
        }
    }

    private static final class NoopSubscription implements Subscription {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    }

    /**
     * Builder for {@link SmithyAsyncHttpClient}.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<SmithyAsyncHttpClient.Builder> {

        /**
         * The amount of time to wait for data on an established connection before timing out.
         */
        Builder readTimeout(Duration readTimeout);

        /**
         * The amount of time to wait when establishing a connection before timing out.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * The amount of time to wait for a connection from the pool before timing out.
         */
        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        /**
         * The maximum number of connections allowed in the pool.
         */
        Builder maxConcurrency(Integer maxConcurrency);

        /**
         * The maximum amount of time a connection may sit idle in the pool.
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

        /**
         * Forces HTTP/1.1 or allows HTTP/2 negotiation. Defaults to smithy's own policy.
         */
        Builder http2Enabled(Boolean http2Enabled);
    }

    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private Boolean http2Enabled;

        private DefaultBuilder() {
        }

        @Override
        public Builder readTimeout(Duration readTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.READ_TIMEOUT, readTimeout);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT, connectionAcquisitionTimeout);
            return this;
        }

        @Override
        public Builder maxConcurrency(Integer maxConcurrency) {
            standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConcurrency);
            return this;
        }

        @Override
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, connectionMaxIdleTime);
            return this;
        }

        @Override
        public Builder http2Enabled(Boolean http2Enabled) {
            this.http2Enabled = http2Enabled;
            return this;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            AttributeMap resolvedOptions = standardOptions.build()
                                                          .merge(serviceDefaults)
                                                          .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);
            return new SmithyAsyncHttpClient(this, resolvedOptions);
        }
    }
}
