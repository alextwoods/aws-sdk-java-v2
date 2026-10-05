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

package software.amazon.awssdk.http.crt.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * One blocking CRT exchange. {@link #execute()} waits for the status and headers; the response body is a one-shot
 * stream over the chunks the CRT delivers on its event loop. The CRT stream uses manual window management: the window
 * starts at the client's {@code readBufferSizeInBytes} and is reopened by exactly the bytes the reader consumed, so at
 * most that many bytes are ever buffered ahead of the reader. {@link #abort()} fails a blocked or later
 * {@code execute}/{@code read} at once with {@link HttpFailureCategory#CANCELLED} and cancels the native stream.
 */
public final class CrtSyncExchange implements SyncHttpExchange, HttpStreamResponseHandler {
    private final CrtClientCore core;
    private final SdkHttpRequest request;
    private final HttpExecutionOptions options;
    private final CrtStreamState nativeStream = new CrtStreamState();
    private final CompletableFuture<SdkHttpResponse> headers = new CompletableFuture<>();
    private final HttpHeaders.Builder responseHeaders = HttpHeaders.builder();
    private final Body body = new Body();
    private final Runnable abortOnClientClose = this::abort;
    private volatile HttpPhase phase = HttpPhase.CONNECTION_ACQUISITION;
    private volatile CrtRequestBodies.FromInputStream requestStream;
    private boolean executed;
    private boolean closed;
    private int status;

    public CrtSyncExchange(CrtClientCore core, SdkHttpRequest request, HttpExecutionOptions options) {
        this.core = core;
        this.request = request;
        this.options = options;
    }

    @Override
    public SdkHttpResponse execute() {
        synchronized (this) {
            if (executed || closed) {
                throw new IllegalStateException("execute() was already called or the exchange is closed");
            }
            executed = true;
        }
        if (nativeStream.isAbandoned()) {
            throw cancelled();
        }
        core.track(abortOnClientClose);
        core.acquire(request, options.connectionAcquisitionTimeout()).whenComplete((lease, failure) -> {
            if (failure != null) {
                fail(failure);
                return;
            }
            if (!nativeStream.leased(lease)) {
                return;
            }
            start(lease);
        });
        try {
            return headers.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            abort();
            throw cancelled();
        } catch (ExecutionException e) {
            throw CrtFailureMapping.toTransportException(e.getCause(), phase, core.clientName());
        }
    }

    private void start(CrtClientCore.Lease lease) {
        phase = HttpPhase.REQUEST_TRANSMISSION;
        try {
            // Makes and activates the stream under the stream state's lock; discards the connection if this throws.
            nativeStream.start(() -> {
                HttpRequestBodyStream bodyStream = null;
                HttpBody requestBody = request.body();
                if (requestBody.kind() == HttpBody.Kind.BYTES) {
                    bodyStream = CrtRequestBodies.bytes(requestBody);
                } else if (requestBody.kind() == HttpBody.Kind.STREAMING) {
                    requestStream = new CrtRequestBodies.FromInputStream(requestBody.streamingBody().openStream(),
                                                                         requestBody.contentLength().orElse(-1),
                                                                         this::requestBodyFailed);
                    bodyStream = requestStream;
                }
                return lease.connection().makeRequest(CrtClientCore.toCrtRequest(request, bodyStream), this);
            });
        } catch (RuntimeException e) {
            closeRequestStream();
            fail(e);
        }
    }

    private void requestBodyFailed(Throwable t) {
        fail(CrtFailureMapping.toTransportException(t, HttpPhase.REQUEST_TRANSMISSION, core.clientName()));
        nativeStream.abandon();
    }

    @Override
    public void onResponseHeaders(HttpStream stream, int statusCode, int blockType, HttpHeader[] nextHeaders) {
        if (blockType == HttpHeaderBlock.MAIN.getValue()) {
            phase = HttpPhase.RESPONSE_CONSUMPTION;
            status = statusCode;
            for (HttpHeader h : nextHeaders) {
                responseHeaders.add(h.getName(), h.getValue());
            }
        }
    }

    @Override
    public void onResponseHeadersDone(HttpStream stream, int blockType) {
        if (blockType != HttpHeaderBlock.MAIN.getValue()) {
            return;
        }
        HttpHeaders fields = responseHeaders.build();
        long length = fields.firstValue("Content-Length").map(Long::parseLong).orElse(-1L);
        boolean empty = length == 0 || "HEAD".equals(request.method()) || status == 204 || status == 304;
        HttpBody responseBody = empty ? HttpBody.empty()
                                      : HttpBody.streaming(StreamingBodies.fromInputStream(
                                          body, length, fields.firstValue("Content-Type").orElse(null)));
        if (empty) {
            // Nothing for the caller to consume: the connection may be reused once the stream completes.
            body.emptyResponse = true;
            nativeStream.consumed();
        }
        headers.complete(SdkHttpResponse.builder().statusCode(status).headers(fields).body(responseBody).build());
    }

    @Override
    public int onResponseBody(HttpStream stream, byte[] bodyBytesIn) {
        body.offer(bodyBytesIn);
        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        closeRequestStream();
        if (errorCode == 0) {
            body.end();
            nativeStream.completed(true);
        } else {
            HttpTransportException failure = nativeStream.isAbandoned()
                                             ? cancelled()
                                             : CrtFailureMapping.fromErrorCode(errorCode, phase, core.clientName());
            fail(failure);
            nativeStream.completed(false);
        }
    }

    private void fail(Throwable failure) {
        HttpTransportException f = CrtFailureMapping.toTransportException(failure, phase, core.clientName());
        headers.completeExceptionally(f);
        body.fail(f);
    }

    private HttpTransportException cancelled() {
        return HttpTransportException.builder().phase(phase).category(HttpFailureCategory.CANCELLED)
                                     .clientName(core.clientName())
                                     .message("The HTTP exchange was aborted during " + phase + ".").build();
    }

    @Override
    public void abort() {
        HttpTransportException failure = cancelled();
        headers.completeExceptionally(failure);
        body.fail(failure);
        nativeStream.abandon();
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        if (body.isFullyRead()) {
            // A body read to its end without seeing -1: the connection may be reused (no-op otherwise).
            nativeStream.consumed();
        } else {
            abort();
        }
        closeRequestStream();
        core.untrack(abortOnClientClose);
    }

    private void closeRequestStream() {
        CrtRequestBodies.FromInputStream s = requestStream;
        if (s != null) {
            s.close();
        }
    }

    /** Test view: the most bytes ever buffered ahead of the reader. */
    public long maxBufferedBytes() {
        return body.highWater;
    }

    /** The response body stream: chunks queued by the event loop, read by the owning thread. */
    private final class Body extends InputStream {
        private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();
        private byte[] current;
        private int offset;
        private long buffered;
        private long highWater;
        private boolean ended;
        private HttpTransportException failure;
        private boolean closed;
        private boolean opened;
        private volatile boolean emptyResponse;

        synchronized void offer(byte[] chunk) {
            if (failure != null || closed) {
                return;
            }
            chunks.add(chunk);
            buffered += chunk.length;
            highWater = Math.max(highWater, buffered);
            notifyAll();
        }

        synchronized void end() {
            ended = true;
            notifyAll();
        }

        synchronized void fail(HttpTransportException f) {
            if (failure == null && !(ended && chunks.isEmpty() && current == null)) {
                failure = f;
                chunks.clear();
                current = null;
                notifyAll();
            }
        }

        /**
         * True if closing the exchange need not abandon it: the caller consumed the body to its end, the response
         * has no body (the stream completes by itself and returns the connection), or the exchange already failed
         * (the connection was discarded). A body the CRT received completely but the caller did not read is not
         * fully read: closing then discards the connection.
         */
        synchronized boolean isFullyRead() {
            return consumedToEnd() || failure != null || emptyResponse;
        }

        private boolean consumedToEnd() {
            return ended && chunks.isEmpty() && current == null;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            java.util.Objects.checkFromIndexSize(off, len, b.length);
            int consumed = 0;
            int n;
            synchronized (this) {
                opened = true;
                while (true) {
                    if (closed) {
                        throw new IOException("The response stream is closed");
                    }
                    if (failure != null) {
                        throw failure;
                    }
                    if (current == null && !chunks.isEmpty()) {
                        current = chunks.poll();
                        offset = 0;
                    }
                    if (current != null) {
                        break;
                    }
                    if (ended) {
                        consumed = -1;
                        break;
                    }
                    if (len == 0) {
                        return 0;
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new java.io.InterruptedIOException("Interrupted while reading the response");
                    }
                }
                if (consumed < 0) {
                    n = -1;
                } else {
                    n = Math.min(len, current.length - offset);
                    System.arraycopy(current, offset, b, off, n);
                    offset += n;
                    buffered -= n;
                    consumed = n;
                    if (offset == current.length) {
                        current = null;
                    }
                }
            }
            if (n < 0) {
                // The caller reached the end of the body: the connection may be reused.
                nativeStream.consumed();
                return -1;
            }
            nativeStream.incrementWindow(consumed);
            return n;
        }

        @Override
        public synchronized int available() {
            return (int) Math.min(Integer.MAX_VALUE, buffered);
        }

        @Override
        public void close() {
            boolean complete;
            synchronized (this) {
                if (closed) {
                    return;
                }
                complete = consumedToEnd();
                closed = true;
                chunks.clear();
                current = null;
                notifyAll();
            }
            if (complete) {
                nativeStream.consumed();
            } else {
                // An early close never returns the connection to the pool, even if the CRT received it all.
                abort();
            }
        }
    }
}
