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

package software.amazon.awssdk.test.fault;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
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

/**
 * A scripted HTTP transport for both drivers: each transmission runs the next {@link TransportFault} of its script
 * and is recorded as an {@link ExchangeRecord}. {@link #sync()} and {@link #async()} are the two transport views of
 * one script, so the same trace can run on either driver.
 *
 * <p>The fakes keep the transport contracts the pipeline relies on: the async view signals each exchange's terminal
 * event exactly once (the handler's {@code onError}, or its response body's {@code onComplete}/{@code onError}),
 * honours body demand and cancellation, and reacts to a winning {@code cancel} through
 * {@link AsyncExchangeCompletion}; the sync view blocks a hanging exchange until it is aborted, closed or its thread
 * is interrupted. Async deliveries run on the {@code deliveries} executor (inline by default, which makes a whole
 * async call run deterministically on the calling thread). A transmission beyond the end of the script fails with an
 * {@link IllegalStateException} and records a violation.
 *
 * <p>Thread-safe. Not a real network transport: the request is never sent anywhere.
 */
public final class FaultyHttpClient {
    /** The {@code clientName()} of both views, and the client name recorded in injected transport failures. */
    public static final String CLIENT_NAME = "FaultyHttpClient";

    private static final long ALL = Long.MAX_VALUE;

    private final Deque<TransportFault> script;
    private final Executor deliveries;
    private final Violations violations;
    private final List<ExchangeRecord> exchanges = new CopyOnWriteArrayList<>();
    private final BlockingQueue<ExchangeRecord> parked = new LinkedBlockingQueue<>();
    private final SdkSyncHttpClient sync = new SyncView();
    private final SdkAsyncHttpClient async = new AsyncView();

    /** A transport of {@code script}, delivering async events inline. */
    public FaultyHttpClient(List<TransportFault> script, Violations violations) {
        this(script, Runnable::run, violations);
    }

    /** A transport of {@code script}, delivering async events on {@code deliveries} (a fake event loop). */
    public FaultyHttpClient(List<TransportFault> script, Executor deliveries, Violations violations) {
        this.script = new ArrayDeque<>(script);
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
        this.violations = Objects.requireNonNull(violations, "violations");
    }

    /** The sync transport view. */
    public SdkSyncHttpClient sync() {
        return sync;
    }

    /** The async transport view. */
    public SdkAsyncHttpClient async() {
        return async;
    }

    /** Every exchange so far, in transmission order. */
    public List<ExchangeRecord> exchanges() {
        return List.copyOf(exchanges);
    }

    /** How many transmissions ran a scripted fault. */
    public int transmissions() {
        return (int) exchanges.stream().filter(e -> e.fault() != null).count();
    }

    /** The scripted faults no transmission has used yet. */
    public synchronized List<TransportFault> unused() {
        return List.copyOf(script);
    }

    /** Why each exchange that is not cleanly released leaks, empty if none does. */
    public List<String> leaks() {
        return exchanges.stream().map(ExchangeRecord::leak).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Waits for the next exchange to park on a {@link TransportFault.Hang} or {@link TransportFault.HangBody}.
     *
     * @throws IllegalStateException if none parks within {@code timeout}
     */
    public ExchangeRecord awaitParked(Duration timeout) {
        try {
            ExchangeRecord record = parked.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
            if (record == null) {
                throw new IllegalStateException("No exchange parked within " + timeout + ": " + exchanges);
            }
            return record;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an exchange to park", e);
        }
    }

    private synchronized TransportFault next(ExchangeRecord record) {
        TransportFault fault = script.poll();
        if (fault == null) {
            violations.record("transport: unscripted transmission " + record.index() + " ("
                              + record.request().method() + " " + record.request().encodedPath() + ")");
            return null;
        }
        record.fault(fault);
        return fault;
    }

    private ExchangeRecord newRecord(HttpTransportMode mode, SdkHttpRequest request) {
        ExchangeRecord record;
        synchronized (this) {
            record = new ExchangeRecord(exchanges.size(), mode, request);
            exchanges.add(record);
        }
        return record;
    }

    private void park(ExchangeRecord record) {
        record.park();
        parked.add(record);
    }

    private static HttpTransportException transport(HttpPhase phase, HttpFailureCategory.Kind kind, String message,
                                                    Throwable cause) {
        return HttpTransportException.builder()
                                     .phase(phase)
                                     .category(HttpFailureCategory.of(phase, kind))
                                     .clientName(CLIENT_NAME)
                                     .message(message)
                                     .cause(cause)
                                     .build();
    }

    private static IllegalStateException unscripted() {
        return new IllegalStateException("FaultyHttpClient: the trace scripts no further transmission");
    }

    private static HttpHeaders headers(Map<String, List<String>> values) {
        HttpHeaders.Builder headers = HttpHeaders.builder();
        values.forEach((name, list) -> list.forEach(value -> headers.add(name, value)));
        return headers.build();
    }

    private static HttpClientCapabilities capabilities(HttpTransportMode mode) {
        return HttpClientCapabilities.builder()
                                     .mode(mode)
                                     .addProtocol(Protocol.HTTP1_1, Protocol.HTTP2)
                                     .maxBufferedBytesPerExchange(64 * 1024)
                                     .cancellationTimeout(Duration.ofSeconds(1))
                                     .build();
    }

    /** How a response body ends after its delivered prefix. */
    private enum BodyEnd {
        COMPLETE, FAIL, HANG
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Sync view

    private final class SyncView implements SdkSyncHttpClient {
        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncExchange(newRecord(HttpTransportMode.SYNC, request), request);
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return FaultyHttpClient.capabilities(HttpTransportMode.SYNC);
        }

        @Override
        public String clientName() {
            return CLIENT_NAME;
        }

        @Override
        public void close() {
        }
    }

    private final class SyncExchange implements SyncHttpExchange {
        private final ExchangeRecord record;
        private final SdkHttpRequest request;
        private final CountDownLatch released = new CountDownLatch(1);
        private volatile ResponseStream response;

        private SyncExchange(ExchangeRecord record, SdkHttpRequest request) {
            this.record = record;
            this.request = request;
        }

        @Override
        public SdkHttpResponse execute() {
            TransportFault fault = next(record);
            if (fault == null) {
                throw unscripted();
            }
            if (fault instanceof TransportFault.Respond respond) {
                readRequest(ALL);
                return response(respond, respond.body().length, BodyEnd.COMPLETE);
            }
            if (fault instanceof TransportFault.ConnectFailure) {
                throw transport(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Injected connection failure",
                                new ConnectException("Connection refused (injected)"));
            }
            if (fault instanceof TransportFault.Timeout timeout) {
                if (timeout.phase().compareTo(HttpPhase.REQUEST_TRANSMISSION) >= 0) {
                    readRequest(ALL);
                }
                throw transport(timeout.phase(), HttpFailureCategory.Kind.TIMEOUT, "Injected timeout",
                                new SocketTimeoutException("Read timed out (injected)"));
            }
            if (fault instanceof TransportFault.PartialRequest partial) {
                readRequest(partial.bytes());
                throw transport(HttpPhase.REQUEST_TRANSMISSION, HttpFailureCategory.Kind.IO,
                                "Injected failure after " + partial.bytes() + " request body bytes",
                                new IOException("Broken pipe (injected)"));
            }
            if (fault instanceof TransportFault.PartialResponse partial) {
                readRequest(ALL);
                return response(partial.response(), partial.bytes(), BodyEnd.FAIL);
            }
            if (fault instanceof TransportFault.Hang) {
                readRequest(ALL);
                park(record);
                awaitRelease();
                throw transport(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.CANCELLED,
                                "The hanging exchange was aborted", null);
            }
            TransportFault.HangBody hang = (TransportFault.HangBody) fault;
            readRequest(ALL);
            return response(hang.response(), hang.bytes(), BodyEnd.HANG);
        }

        /** Blocks until the exchange is aborted or closed; an interrupt ends the wait as a cancelled exchange. */
        private void awaitRelease() {
            try {
                released.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                InterruptedIOException cause = new InterruptedIOException("Interrupted while waiting (injected hang)");
                cause.initCause(e);
                throw transport(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.CANCELLED,
                                "The hanging exchange's thread was interrupted", cause);
            }
        }

        /** Reads at most {@code limit} bytes of the request body, as a transport streams it, and closes it. */
        private void readRequest(long limit) {
            HttpBody body = request.body();
            switch (body.kind()) {
                case EMPTY -> {
                }
                case BYTES -> record.addRequestBytes(Math.min(limit, body.asByteArray().length));
                case STREAMING -> {
                    record.requestBodyOpen(true);
                    try (InputStream in = body.streamingBody().openStream()) {
                        byte[] buffer = new byte[8192];
                        long remaining = limit;
                        while (remaining > 0) {
                            int n = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                            if (n < 0) {
                                break;
                            }
                            record.addRequestBytes(n);
                            remaining -= n;
                        }
                    } catch (IOException | RuntimeException e) {
                        throw transport(HttpPhase.REQUEST_TRANSMISSION, HttpFailureCategory.Kind.IO,
                                        "The request body failed while it was sent", e);
                    } finally {
                        record.requestBodyOpen(false);
                    }
                }
                default -> throw new IllegalStateException("Unknown body kind " + body.kind());
            }
        }

        private SdkHttpResponse response(TransportFault.Respond respond, int deliver, BodyEnd end) {
            ResponseStream stream = new ResponseStream(respond.body(), deliver, end, record);
            response = stream;
            record.responseBodyOpen(true);
            String contentType = respond.headers().getOrDefault("Content-Type", List.of()).stream().findFirst()
                                        .orElse(null);
            return SdkHttpResponse.builder()
                                  .statusCode(respond.status())
                                  .headers(headers(respond.headers()))
                                  .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                      stream, respond.body().length, contentType)))
                                  .build();
        }

        @Override
        public void abort() {
            record.aborted();
            released.countDown();
            ResponseStream stream = response;
            if (stream != null) {
                stream.release();
            }
        }

        @Override
        public void close() {
            record.closed();
            released.countDown();
            ResponseStream stream = response;
            if (stream != null) {
                stream.close();
            }
        }
    }

    /** A sync response body: {@code deliver} bytes, then the end, a failure, or a hang until released. */
    private final class ResponseStream extends InputStream {
        private final byte[] body;
        private final int deliver;
        private final BodyEnd end;
        private final ExchangeRecord record;
        private final CountDownLatch released = new CountDownLatch(1);
        private int position;
        private volatile boolean closed;

        private ResponseStream(byte[] body, int deliver, BodyEnd end, ExchangeRecord record) {
            this.body = body;
            this.deliver = deliver;
            this.end = end;
            this.record = record;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (closed) {
                throw new IOException("The response body is closed");
            }
            if (length == 0) {
                return 0;
            }
            if (position < deliver) {
                int n = Math.min(length, deliver - position);
                System.arraycopy(body, position, buffer, offset, n);
                position += n;
                return n;
            }
            switch (end) {
                case COMPLETE -> {
                    return -1;
                }
                case FAIL -> throw new IOException("Premature end of the response body after " + deliver + " of "
                                                   + body.length + " bytes (injected)");
                default -> {
                    park(record);
                    try {
                        released.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        InterruptedIOException failure = new InterruptedIOException("Interrupted (injected hang)");
                        failure.initCause(e);
                        throw failure;
                    }
                    throw new IOException("The hanging response body was aborted");
                }
            }
        }

        void release() {
            released.countDown();
        }

        @Override
        public void close() {
            closed = true;
            released.countDown();
            record.responseBodyOpen(false);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Async view

    private final class AsyncView implements SdkAsyncHttpClient {
        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            ExchangeRecord record = newRecord(HttpTransportMode.ASYNC, request);
            AsyncExchange exchange = new AsyncExchange(record, request, handler);
            TransportFault fault = next(record);
            deliveries.execute(() -> exchange.run(fault));
            return exchange.completion;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return FaultyHttpClient.capabilities(HttpTransportMode.ASYNC);
        }

        @Override
        public String clientName() {
            return CLIENT_NAME;
        }

        @Override
        public void close() {
        }
    }

    private final class AsyncExchange {
        private final ExchangeRecord record;
        private final SdkHttpRequest request;
        private final AsyncHttpResponseHandler handler;
        private final AsyncExchangeCompletion completion;
        private volatile Subscription requestSubscription;
        private volatile ResponsePublisher body;

        private AsyncExchange(ExchangeRecord record, SdkHttpRequest request, AsyncHttpResponseHandler handler) {
            this.record = record;
            this.request = request;
            this.handler = handler;
            this.completion = new AsyncExchangeCompletion(CLIENT_NAME, (reason, failure) -> {
                record.cancelled(reason);
                Subscription upload = requestSubscription;
                if (upload != null) {
                    requestSubscription = null;
                    upload.cancel();
                    record.requestBodyOpen(false);
                }
                ResponsePublisher download = body;
                if (download != null) {
                    download.fail(failure);
                } else {
                    signalError(failure);
                }
            });
            record.asyncDone(completion::isDone);
        }

        private void run(TransportFault fault) {
            if (fault == null) {
                fail(transport(HttpPhase.CONNECTION_ACQUISITION, HttpFailureCategory.Kind.IO,
                               "The trace scripts no further transmission", unscripted()));
                return;
            }
            if (completion.isDone()) {
                return;
            }
            if (fault instanceof TransportFault.Respond respond) {
                sendRequest(ALL, () -> respond(respond, respond.body().length, BodyEnd.COMPLETE));
            } else if (fault instanceof TransportFault.ConnectFailure) {
                completion.enter(HttpPhase.CONNECT);
                fail(transport(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Injected connection failure",
                               new ConnectException("Connection refused (injected)")));
            } else if (fault instanceof TransportFault.Timeout timeout) {
                Runnable timedOut = () -> fail(transport(timeout.phase(), HttpFailureCategory.Kind.TIMEOUT,
                                                         "Injected timeout",
                                                         new SocketTimeoutException("Read timed out (injected)")));
                if (timeout.phase().compareTo(HttpPhase.REQUEST_TRANSMISSION) >= 0) {
                    sendRequest(ALL, timedOut);
                } else {
                    completion.enter(timeout.phase());
                    timedOut.run();
                }
            } else if (fault instanceof TransportFault.PartialRequest partial) {
                sendRequest(partial.bytes(), () -> fail(transport(
                    HttpPhase.REQUEST_TRANSMISSION, HttpFailureCategory.Kind.IO,
                    "Injected failure after " + partial.bytes() + " request body bytes",
                    new IOException("Broken pipe (injected)"))));
            } else if (fault instanceof TransportFault.PartialResponse partial) {
                sendRequest(ALL, () -> respond(partial.response(), partial.bytes(), BodyEnd.FAIL));
            } else if (fault instanceof TransportFault.Hang) {
                sendRequest(ALL, () -> {
                    completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
                    park(record);
                });
            } else {
                TransportFault.HangBody hang = (TransportFault.HangBody) fault;
                sendRequest(ALL, () -> respond(hang.response(), hang.bytes(), BodyEnd.HANG));
            }
        }

        /** Fails the exchange and signals the handler, unless the exchange already terminated. */
        private void fail(HttpTransportException failure) {
            if (completion.fail(failure)) {
                signalError(failure);
            }
        }

        private void signalError(Throwable failure) {
            record.terminalSignal();
            handler.onError(failure);
        }

        /** Streams at most {@code limit} request body bytes under demand, then runs {@code then}. */
        private void sendRequest(long limit, Runnable then) {
            completion.enter(HttpPhase.REQUEST_TRANSMISSION);
            HttpBody payload = request.body();
            if (payload.kind() != HttpBody.Kind.STREAMING) {
                record.addRequestBytes(Math.min(limit, payload.kind() == HttpBody.Kind.EMPTY
                                                       ? 0 : payload.asByteArray().length));
                then.run();
                return;
            }
            record.requestBodyOpen(true);
            payload.streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                private long read;
                private boolean done;

                @Override
                public void onSubscribe(Subscription subscription) {
                    if (completion.isDone()) {
                        subscription.cancel();
                        record.requestBodyOpen(false);
                        return;
                    }
                    requestSubscription = subscription;
                    if (limit == 0) {
                        finish(subscription);
                        return;
                    }
                    subscription.request(1);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    if (done) {
                        return;
                    }
                    long n = Math.min(chunk.remaining(), limit - read);
                    read += n;
                    record.addRequestBytes(n);
                    Subscription subscription = requestSubscription;
                    if (read >= limit) {
                        finish(subscription);
                    } else if (subscription != null) {
                        subscription.request(1);
                    }
                }

                /** The limit was reached before the body's end: stop reading it. */
                private void finish(Subscription subscription) {
                    done = true;
                    requestSubscription = null;
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    record.requestBodyOpen(false);
                    then.run();
                }

                @Override
                public void onError(Throwable error) {
                    if (done) {
                        return;
                    }
                    done = true;
                    requestSubscription = null;
                    record.requestBodyOpen(false);
                    fail(transport(HttpPhase.REQUEST_TRANSMISSION, HttpFailureCategory.Kind.IO,
                                   "The request body failed while it was sent", error));
                }

                @Override
                public void onComplete() {
                    if (done) {
                        return;
                    }
                    done = true;
                    requestSubscription = null;
                    record.requestBodyOpen(false);
                    if (!completion.isDone()) {
                        then.run();
                    }
                }
            });
        }

        private void respond(TransportFault.Respond respond, int deliver, BodyEnd end) {
            completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
            if (completion.isDone()) {
                return;
            }
            ResponsePublisher publisher = new ResponsePublisher(this, respond.body(), deliver, end);
            body = publisher;
            record.responseBodyOpen(true);
            handler.onHeaders(SdkHttpResponse.builder()
                                             .statusCode(respond.status())
                                             .headers(headers(respond.headers()))
                                             .body(HttpBody.empty())
                                             .build());
            handler.onStream(publisher);
        }
    }

    /**
     * An async response body for one subscriber: on demand, the delivered prefix as one chunk, then completion, a
     * failure, or nothing until cancelled. Each terminal signal also terminates the exchange; a subscriber's cancel
     * terminates it silently, as a transport discards a connection whose body is abandoned.
     */
    private final class ResponsePublisher implements Publisher<ByteBuffer> {
        private final AsyncExchange exchange;
        private final byte[] bytes;
        private final int deliver;
        private final BodyEnd end;
        private Subscriber<? super ByteBuffer> subscriber;
        private boolean delivered;
        private boolean terminated;
        private Throwable pendingFailure;

        private ResponsePublisher(AsyncExchange exchange, byte[] bytes, int deliver, BodyEnd end) {
            this.exchange = exchange;
            this.bytes = bytes;
            this.deliver = deliver;
            this.end = end;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            Objects.requireNonNull(s, "subscriber");
            Throwable failure;
            synchronized (this) {
                if (subscriber != null) {
                    violations.record("transport: exchange " + exchange.record.index()
                                      + " response body subscribed twice");
                    s.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                    s.onError(new IllegalStateException("The response body allows one subscriber"));
                    return;
                }
                subscriber = s;
                failure = pendingFailure;
            }
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        terminate(false, new IllegalArgumentException("Reactive Streams 3.9: demand " + n));
                        return;
                    }
                    deliver();
                }

                @Override
                public void cancel() {
                    synchronized (ResponsePublisher.this) {
                        if (terminated) {
                            return;
                        }
                        terminated = true;
                    }
                    exchange.record.bodyCancelled();
                    exchange.record.responseBodyOpen(false);
                    exchange.completion.fail(transport(HttpPhase.RESPONSE_CONSUMPTION,
                                                       HttpFailureCategory.Kind.CANCELLED,
                                                       "The response body subscription was cancelled", null));
                }
            });
            if (failure != null) {
                terminate(true, failure);
            }
        }

        private void deliver() {
            synchronized (this) {
                if (terminated || delivered) {
                    return;
                }
                delivered = true;
            }
            if (deliver > 0) {
                subscriber.onNext(ByteBuffer.wrap(bytes, 0, deliver).slice().asReadOnlyBuffer());
            }
            switch (end) {
                case COMPLETE -> terminate(false, null);
                case FAIL -> terminate(false, transport(
                    HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.IO,
                    "Premature end of the response body after " + deliver + " of " + bytes.length + " bytes",
                    new IOException("Connection reset (injected)")));
                default -> park(exchange.record);
            }
        }

        /** A winning exchange cancellation: the subscriber's {@code onError} now, or once it subscribes. */
        private void fail(Throwable failure) {
            synchronized (this) {
                if (subscriber == null) {
                    pendingFailure = failure;
                    return;
                }
            }
            terminate(true, failure);
        }

        /**
         * Sends the body's one terminal signal ({@code onComplete} without {@code failure}) unless it already ended,
         * and terminates the exchange with it unless {@code exchangeDone} (a winning cancellation already did).
         */
        private void terminate(boolean exchangeDone, Throwable failure) {
            synchronized (this) {
                if (terminated) {
                    return;
                }
                terminated = true;
            }
            exchange.record.responseBodyOpen(false);
            if (failure == null) {
                if (exchange.completion.complete()) {
                    exchange.record.terminalSignal();
                    subscriber.onComplete();
                }
                return;
            }
            if (exchangeDone || exchange.completion.fail(failure)) {
                exchange.record.terminalSignal();
                subscriber.onError(failure);
            }
        }
    }
}
