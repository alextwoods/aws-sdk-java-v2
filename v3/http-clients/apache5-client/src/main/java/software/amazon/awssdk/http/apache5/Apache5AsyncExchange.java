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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.apache5.internal.Apache5FailureMapping;

/**
 * One exchange of {@link Apache5AsyncHttpClient}. Terminal uniqueness comes from {@link AsyncExchangeCompletion}:
 * a signal to the handler or body subscriber is sent only by the call that terminated the exchange. Handler signals
 * run through a serial queue, and whether a failure goes to the handler ({@code onError}) or to the body subscriber
 * is decided under this exchange's lock together with the hand-off of the body ({@code onStream}), so the handler
 * never sees {@code onStream} after {@code onError}.
 */
final class Apache5AsyncExchange {
    private final Apache5AsyncHttpClient owner;
    private final SdkHttpRequest request;
    private final AsyncHttpResponseHandler handler;
    private final RequestConfig requestConfig;
    private final AsyncExchangeCompletion completion;
    private final Queue<Runnable> signals = new ConcurrentLinkedQueue<>();
    private final AtomicInteger signalling = new AtomicInteger();
    private ResponseBodyPublisher publisher;
    private volatile Future<Void> future;
    private volatile PublisherEntityProducer entity;
    private volatile CapacityChannel capacity;

    Apache5AsyncExchange(Apache5AsyncHttpClient owner, SdkHttpRequest request, AsyncHttpResponseHandler handler,
                         RequestConfig requestConfig) {
        this.owner = owner;
        this.request = request;
        this.handler = handler;
        this.requestConfig = requestConfig;
        this.completion = new AsyncExchangeCompletion(Apache5AsyncHttpClient.CLIENT_NAME, this::cancelled);
    }

    AsyncExchangeCompletion completion() {
        return completion;
    }

    void start() {
        BasicHttpRequest engineRequest = new BasicHttpRequest(request.method(),
                                                              new HttpHost(request.scheme(), request.host(),
                                                                           request.port()),
                                                              request.rawTarget());
        HttpHeaders headers = request.headers();
        String declaredLength = null;
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            if (name.equalsIgnoreCase("Content-Length")) {
                declaredLength = headers.value(i);
            } else if (!name.equalsIgnoreCase("Transfer-Encoding")) {
                engineRequest.addHeader(name, headers.value(i));
            }
        }
        AsyncEntityProducer producer = entity(request.body(), declaredLength);
        HttpClientContext context = HttpClientContext.create();
        context.setRequestConfig(requestConfig);
        BasicRequestProducer requestProducer = new BasicRequestProducer(engineRequest, producer) {
            @Override
            public void sendRequest(RequestChannel channel, HttpContext httpContext)
                throws HttpException, IOException {
                completion.enter(HttpPhase.REQUEST_TRANSMISSION);
                super.sendRequest(channel, httpContext);
                if (producer == null) {
                    completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
                }
            }
        };
        future = owner.engine().execute(requestProducer, new Consumer(), null, context, new FutureCallback<>() {
            @Override
            public void completed(Void result) {
            }

            @Override
            public void failed(Exception ex) {
                onFailure(ex);
            }

            @Override
            public void cancelled() {
                onFailure(new java.util.concurrent.CancellationException("The engine cancelled the exchange"));
            }
        });
        if (completion.isDone()) {
            future.cancel(true);
        }
    }

    private AsyncEntityProducer entity(HttpBody body, String declaredLength) {
        long length = declaredLength == null ? body.contentLength().orElse(-1) : Long.parseLong(declaredLength.trim());
        Runnable written = () -> completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
        // In-memory bodies are written straight from the body's read-only view, as the Netty transport does. Routing
        // them through a StreamingBodies publisher cost a copy plus two 64 KiB chunk buffers per exchange (one for
        // the data, one for the EOF probe): ~128 KiB/op of garbage on a 470-byte GetItem.
        PublisherEntityProducer producer = switch (body.kind()) {
            case EMPTY -> declaredLength == null ? null
                          : PublisherEntityProducer.ofBuffer(ByteBuffer.allocate(0), length, written);
            case BYTES -> PublisherEntityProducer.ofBuffer(body.asByteBuffer(), length, written);
            case STREAMING -> new PublisherEntityProducer(body.streamingBody(), length, written);
        };
        if (producer == null) {
            return null;
        }
        entity = producer;
        return producer;
    }

    /** An engine failure: terminates the exchange with its mapped category unless it already terminated. */
    void onFailure(Throwable failure) {
        HttpTransportException mapped =
            Apache5FailureMapping.toTransportException(failure, completion.phase(), owner.isClosed(),
                                                       Apache5AsyncHttpClient.CLIENT_NAME);
        if (completion.fail(mapped)) {
            stop();
            route(mapped);
        }
    }

    /** The {@link AsyncExchangeCompletion.CancelAction}: stop the engine, then signal the failure once. */
    private void cancelled(CancellationReason reason, HttpTransportException failure) {
        stop();
        route(failure);
    }

    private void stop() {
        Future<Void> f = future;
        if (f != null) {
            f.cancel(true);
        }
        PublisherEntityProducer e = entity;
        if (e != null) {
            e.cancel();
        }
        owner.untrack(this);
    }

    private void route(HttpTransportException failure) {
        ResponseBodyPublisher body;
        synchronized (this) {
            body = publisher;
        }
        if (body != null) {
            body.fail(failure);
        } else {
            signal(() -> handler.onError(failure));
        }
    }

    private void signal(Runnable signal) {
        signals.add(signal);
        if (signalling.getAndIncrement() != 0) {
            return;
        }
        int missed = 1;
        do {
            Runnable next;
            while ((next = signals.poll()) != null) {
                next.run();
            }
            missed = signalling.addAndGet(-missed);
        } while (missed != 0);
    }

    @Override
    public String toString() {
        return "Apache5AsyncExchange[" + request.method() + " " + request.scheme() + "://" + request.authority()
               + request.encodedPath() + ", " + completion + "]";
    }

    /** Receives the response from the engine. */
    private final class Consumer implements AsyncResponseConsumer<Void>, ResponseBodyPublisher.Upstream {
        private FutureCallback<Void> result;

        @Override
        public void consumeResponse(HttpResponse response, EntityDetails entityDetails, HttpContext context,
                                    FutureCallback<Void> resultCallback) {
            completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
            result = resultCallback;
            SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(response.getCode());
            for (Header header : response.getHeaders()) {
                builder.addHeader(header.getName(), header.getValue());
            }
            SdkHttpResponse headers = builder.body(HttpBody.empty()).build();
            ResponseBodyPublisher body = new ResponseBodyPublisher(Apache5AsyncHttpClient.RESPONSE_WINDOW, this);
            synchronized (Apache5AsyncExchange.this) {
                if (completion.isDone()) {
                    return;
                }
                publisher = body;
            }
            signal(() -> {
                handler.onHeaders(headers);
                handler.onStream(body);
            });
            if (entityDetails == null) {
                body.end();
                resultCallback.completed(null);
            }
        }

        @Override
        public void informationResponse(HttpResponse response, HttpContext context) {
        }

        @Override
        public void updateCapacity(CapacityChannel capacityChannel) {
            capacity = capacityChannel;
            ResponseBodyPublisher body;
            synchronized (Apache5AsyncExchange.this) {
                body = publisher;
            }
            if (body != null) {
                body.onCapacityRequested();
            }
        }

        @Override
        public void consume(ByteBuffer src) {
            ResponseBodyPublisher body;
            synchronized (Apache5AsyncExchange.this) {
                body = publisher;
            }
            if (body != null && src.hasRemaining()) {
                ByteBuffer copy = ByteBuffer.allocate(src.remaining());
                copy.put(src).flip();
                body.offer(copy.asReadOnlyBuffer());
            }
        }

        @Override
        public void streamEnd(List<? extends Header> trailers) {
            ResponseBodyPublisher body;
            synchronized (Apache5AsyncExchange.this) {
                body = publisher;
            }
            if (body != null) {
                body.end();
            }
            if (result != null) {
                result.completed(null);
            }
        }

        @Override
        public void failed(Exception cause) {
            onFailure(cause);
        }

        @Override
        public void releaseResources() {
        }

        @Override
        public void request(int bytes) {
            CapacityChannel channel = capacity;
            if (channel != null) {
                try {
                    channel.update(bytes);
                } catch (IOException | RuntimeException e) {
                    onFailure(e);
                }
            }
        }

        @Override
        public void cancel() {
            completion.cancel(CancellationReason.RESPONSE_ABANDONED);
        }

        @Override
        public boolean complete() {
            if (completion.complete()) {
                owner.untrack(Apache5AsyncExchange.this);
                return true;
            }
            return false;
        }
    }
}
