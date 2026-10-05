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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * One {@link HttpURLConnection} exchange, under the {@link SyncHttpExchange} ownership rules.
 *
 * <p>Connection release, exactly one of:
 * <ul>
 *   <li>the response stream is read to its end (EOF, or all {@code Content-Length} bytes) and closed: the JDK stream
 *       is closed normally and the JDK returns the connection to its keep-alive cache for reuse;</li>
 *   <li>the stream is closed early, or {@link #abort()} or {@link #close()} comes first: the connection is discarded
 *       with {@link HttpURLConnection#disconnect()}. JDK limit: {@code disconnect()} first closes the JDK stream, which
 *       skips an unread remainder that is already buffered and then removes an idle connection to the same server
 *       from the keep-alive cache (the JDK documents this as an approximation), and hands an unread remainder that is
 *       not yet received but at most {@code http.KeepAlive.remainingData} (default 512 KB) to its background cleaner,
 *       which may drain it and reuse the connection. A larger or chunked unreceived remainder is never drained.</li>
 * </ul>
 * A response without a body ({@code HEAD}, 1xx, 204, 304, {@code Content-Length: 0}) is {@link HttpBody#empty()} and its
 * JDK stream is read to its end at once, so the connection is reusable immediately.
 *
 * <p>Abort (any thread, idempotent): an {@link #execute()} blocked writing the request or waiting for the status line
 * is released at once ({@code disconnect()} closes the socket), and fails with {@code CANCELLED}. JDK limits: an
 * in-progress TCP connect cannot be interrupted and ends within the connection timeout, then fails with
 * {@code CANCELLED}; a response-body read blocked inside the JDK holds the JDK stream lock, so the abort only marks the
 * exchange and the reading thread discards the connection and fails with {@code CANCELLED} as soon as its read returns
 * (data arrives or the socket timeout elapses). {@link #abort()} itself never blocks on a read.
 */
final class UrlConnectionExchange implements SyncHttpExchange {
    private final UrlConnectionHttpClient client;
    private final SdkHttpRequest request;
    private final UrlConnectionRequestPlan plan;
    private final Duration readTimeout;
    private final boolean proxied;
    private final boolean https;

    private final Object lock = new Object();
    private final AtomicBoolean executed = new AtomicBoolean();
    private final AtomicBoolean released = new AtomicBoolean();
    private volatile boolean aborted;
    private volatile boolean reusable;
    private volatile boolean closed;
    private volatile HttpURLConnection connection;
    private boolean reading;
    private ResponseStream responseStream;

    UrlConnectionExchange(UrlConnectionHttpClient client, SdkHttpRequest request, UrlConnectionRequestPlan plan,
                          Duration readTimeout) {
        this.client = client;
        this.request = request;
        this.plan = plan;
        this.readTimeout = readTimeout;
        this.proxied = client.proxied(request);
        this.https = "https".equalsIgnoreCase(request.scheme());
    }

    @Override
    public SdkHttpResponse execute() {
        if (closed) {
            throw new IllegalStateException("The exchange is closed");
        }
        if (!executed.compareAndSet(false, true)) {
            throw new IllegalStateException("execute() is called at most once per exchange");
        }
        client.register(this);
        HttpPhase step = HttpPhase.CONNECT;
        try {
            if (proxied && client.tunnelNeedsCredentials(request)) {
                throw HttpTransportException
                    .builder().phase(HttpPhase.PROXY).category(HttpFailureCategory.PROXY)
                    .clientName(UrlConnectionHttpClient.CLIENT_NAME)
                    .message(UrlConnectionHttpClient.CLIENT_NAME + " cannot send proxy credentials in the CONNECT "
                             + "tunnel of an https request; HttpURLConnection builds the tunnel itself.")
                    .build();
            }
            HttpURLConnection c = client.open(request, plan, readTimeout, proxied);
            synchronized (lock) {
                connection = c;
            }
            checkAborted(step);
            c.connect();
            checkAborted(step);
            if (plan.framing != UrlConnectionRequestPlan.Framing.NONE) {
                step = HttpPhase.REQUEST_TRANSMISSION;
                writeBody(c);
                checkAborted(step);
            }
            step = HttpPhase.RESPONSE_CONSUMPTION;
            int status = c.getResponseCode();
            checkAborted(step);
            if (status < 0) {
                throw new java.net.ProtocolException("Invalid Http response");
            }
            HttpHeaders headers = readHeaders(c);
            return SdkHttpResponse.builder().statusCode(status).headers(headers).body(body(c, status, headers))
                                  .build();
        } catch (HttpTransportException e) {
            discard();
            throw e;
        } catch (IOException | RuntimeException e) {
            discard();
            throw failure(step, e);
        }
    }

    private void writeBody(HttpURLConnection c) throws IOException {
        HttpBody body = request.body();
        try (OutputStream out = c.getOutputStream()) {
            switch (body.kind()) {
                case BYTES -> out.write(body.asByteArray());
                case STREAMING -> {
                    try (InputStream source = body.streamingBody().openStream()) {
                        byte[] buffer = new byte[UrlConnectionHttpClient.COPY_BUFFER_SIZE];
                        int n;
                        while ((n = source.read(buffer)) != -1) {
                            checkAborted(HttpPhase.REQUEST_TRANSMISSION);
                            out.write(buffer, 0, n);
                        }
                    }
                }
                case EMPTY -> {
                }
            }
        }
    }

    private static HttpHeaders readHeaders(HttpURLConnection c) {
        HttpHeaders.Builder headers = HttpHeaders.builder();
        for (int i = 0; ; i++) {
            String value = c.getHeaderField(i);
            if (value == null) {
                break;
            }
            String name = c.getHeaderFieldKey(i);
            if (name != null) {
                headers.add(name, value);
            }
        }
        return headers.build();
    }

    private HttpBody body(HttpURLConnection c, int status, HttpHeaders headers) throws IOException {
        long length = headers.firstValue("Content-Length").map(v -> {
            try {
                return Long.parseLong(v.trim());
            } catch (NumberFormatException e) {
                return -1L;
            }
        }).orElse(-1L);
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        boolean noBody = "HEAD".equals(plan.method) || status < 200 || status == 204 || status == 304 || length == 0;
        if (in == null) {
            releaseReusable();
            return HttpBody.empty();
        }
        if (noBody) {
            in.transferTo(OutputStream.nullOutputStream());
            in.close();
            releaseReusable();
            return HttpBody.empty();
        }
        ResponseStream stream = new ResponseStream(in, length);
        synchronized (lock) {
            responseStream = stream;
        }
        String contentType = headers.firstValue("Content-Type").orElse(null);
        return HttpBody.streaming(StreamingBodies.fromInputStream(stream, length, contentType));
    }

    private void checkAborted(HttpPhase step) {
        if (aborted || client.isClosed()) {
            throw UrlConnectionFailures.cancelled(UrlConnectionHttpClient.CLIENT_NAME, step);
        }
    }

    private HttpTransportException failure(HttpPhase step, Throwable e) {
        return UrlConnectionFailures.toException(UrlConnectionHttpClient.CLIENT_NAME, step, e,
                                                 aborted || client.isClosed(), proxied, https);
    }

    @Override
    public void abort() {
        aborted = true;
        synchronized (lock) {
            if (reading) {
                return; // the reading thread discards once its JDK read returns
            }
        }
        discard();
    }

    @Override
    public void close() {
        closed = true;
        ResponseStream stream;
        synchronized (lock) {
            stream = responseStream;
        }
        if (stream != null) {
            stream.close();
        } else {
            discard();
        }
    }

    /**
     * Discards the connection, never after a reusable release. {@code disconnect()} is repeated on every call (it is a
     * no-op once the JDK has dropped the connection), so a connect that completes after a concurrent abort is still
     * closed by the failure path of {@link #execute()}.
     */
    private void discard() {
        if (reusable) {
            return;
        }
        if (released.compareAndSet(false, true)) {
            client.unregister(this);
        }
        HttpURLConnection c = connection;
        if (c != null) {
            c.disconnect();
        }
    }

    /** Marks the connection as released to the JDK keep-alive cache. */
    private void releaseReusable() {
        if (released.compareAndSet(false, true)) {
            reusable = true;
            client.unregister(this);
        }
    }

    /** The response body stream: counts bytes, reuses the connection after EOF, discards it otherwise. */
    private final class ResponseStream extends InputStream {
        private final InputStream in;
        private final long length;
        private long count;
        private boolean eof;
        private boolean streamClosed;

        ResponseStream(InputStream in, long length) {
            this.in = in;
            this.length = length;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n == -1 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            java.util.Objects.checkFromIndexSize(off, len, b.length);
            synchronized (lock) {
                if (streamClosed) {
                    throw new IOException("The response stream is closed");
                }
                if (aborted || client.isClosed()) {
                    discardAndFail();
                }
                if (eof || len == 0) {
                    return eof ? -1 : 0;
                }
                reading = true;
            }
            int n;
            try {
                n = in.read(b, off, len);
            } catch (IOException | RuntimeException e) {
                synchronized (lock) {
                    reading = false;
                }
                discard();
                throw failure(HttpPhase.RESPONSE_CONSUMPTION, e);
            }
            synchronized (lock) {
                reading = false;
                if (aborted || client.isClosed()) {
                    discardAndFail();
                }
                if (n == -1) {
                    if (length >= 0 && count < length) {
                        // The JDK's KeepAliveStream reports a connection closed before Content-Length as EOF.
                        discard();
                        throw failure(HttpPhase.RESPONSE_CONSUMPTION,
                                      new IOException("Premature EOF: the connection closed after " + count + " of "
                                                      + length + " response bytes"));
                    }
                    eof = true;
                } else {
                    count += n;
                    if (length >= 0 && count >= length) {
                        eof = true;
                    }
                }
            }
            return n;
        }

        private void discardAndFail() {
            discard();
            throw UrlConnectionFailures.cancelled(UrlConnectionHttpClient.CLIENT_NAME, HttpPhase.RESPONSE_CONSUMPTION);
        }

        @Override
        public int available() throws IOException {
            synchronized (lock) {
                if (streamClosed || eof) {
                    return 0;
                }
            }
            return in.available();
        }

        @Override
        public void close() {
            boolean reuse;
            synchronized (lock) {
                if (streamClosed) {
                    return;
                }
                streamClosed = true;
                reuse = eof && !aborted && !client.isClosed();
            }
            if (reuse) {
                try {
                    in.close();
                    releaseReusable();
                } catch (IOException e) {
                    discard();
                }
            } else {
                discard();
            }
        }
    }
}
