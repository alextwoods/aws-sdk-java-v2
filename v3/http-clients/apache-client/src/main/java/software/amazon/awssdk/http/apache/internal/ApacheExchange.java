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

package software.amazon.awssdk.http.apache.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * One exchange over a pooled Apache connection; see {@link SyncHttpExchange} for the contract.
 *
 * <p>Wire representation: the request line is {@code METHOD rawTarget HTTP/1.1} ({@code scheme://authority} is
 * prefixed only for a plain-HTTP proxy), never parsed into or rebuilt from a {@link java.net.URI}. The request's
 * headers are written in order, with repeats and case. The transport adds only what is absent: {@code Host} first,
 * then after the caller's headers {@code Content-Length} (known length) or {@code Transfer-Encoding: chunked}
 * (unknown length), {@code Content-Length: 0} for an empty POST/PUT/PATCH, {@code Expect: 100-continue} when enabled
 * for a non-empty body, and {@code Proxy-Authorization} for a plain proxy with credentials.
 *
 * <p>Connection release happens exactly once: reusable (with its keep-alive capped by {@code connectionMaxIdleTime})
 * when the response has no body or its body is read to the end, otherwise the connection is shut down and discarded.
 *
 * <p>Threading: {@link #abort()} may run on any thread and only closes sockets and cancels the pending lease under
 * {@link #lock}; the owning thread then observes the failure, reports {@code CANCELLED} and releases the connection.
 */
final class ApacheExchange implements SyncHttpExchange, SocketFactories.SocketListener {
    private static final int WAIT_FOR_CONTINUE_MILLIS = 3000;

    private final ApacheEngine engine;
    private final SdkHttpRequest request;
    private final HttpExecutionOptions options;
    private final HttpContext context = new BasicHttpContext();
    private final Object lock = new Object();

    // Guarded by lock.
    private boolean executed;
    private boolean closed;
    private ConnectionRequest pendingLease;
    private Socket socket;
    private HttpClientConnection connection;

    private volatile boolean aborted;
    private volatile CancellationReason cancellationReason;
    private volatile HttpPhase phase = HttpPhase.CONNECTION_ACQUISITION;

    // Owner thread only.
    private HttpResponse response;
    private boolean bodyNotSent;
    private boolean tracked;

    ApacheExchange(ApacheEngine engine, SdkHttpRequest request, HttpExecutionOptions options) {
        this.engine = engine;
        this.request = request;
        this.options = options;
        context.setAttribute(SocketFactories.SOCKET_LISTENER, this);
    }

    @Override
    public SdkHttpResponse execute() {
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("The exchange is closed");
            }
            if (executed) {
                throw new IllegalStateException("The exchange was already executed");
            }
            executed = true;
        }
        tracked = engine.track(this);
        if (!tracked) {
            abort(CancellationReason.CLIENT_CLOSED);
        }
        try {
            return doExecute();
        } catch (IOException | HttpException | RuntimeException e) {
            HttpTransportException failure = e instanceof HttpTransportException t ? t : fail(e);
            discard();
            throw failure;
        }
    }

    private SdkHttpResponse doExecute() throws IOException, HttpException {
        EngineSettings settings = engine.settings();
        boolean secure = "https".equalsIgnoreCase(request.scheme());
        int port = request.port() >= 0 ? request.port() : secure ? 443 : 80;
        HttpHost target = new HttpHost(request.host(), port, secure ? "https" : "http");
        ProxySettings proxy = settings.proxyFor(request.scheme(), request.host());
        HttpRoute route = proxy == null ? new HttpRoute(target, null, secure)
                                        : new HttpRoute(target, null, new HttpHost(proxy.host(), proxy.port()), secure);
        HttpClientConnection conn = lease(route);
        int readTimeout = ApacheEngine.millis(options.readTimeout().orElse(settings.socketTimeout()).toMillis());
        if (!conn.isOpen()) {
            phase = proxy == null ? HttpPhase.CONNECT : HttpPhase.PROXY;
            engine.manager().connect(conn, route, ApacheEngine.millis(settings.connectionTimeout().toMillis()),
                                     context);
            if (route.isTunnelled()) {
                conn.setSocketTimeout(readTimeout);
                tunnel(conn, target, proxy);
                phase = HttpPhase.TLS_HANDSHAKE;
                engine.manager().upgrade(conn, route, context);
            }
            engine.manager().routeComplete(conn, route, context);
        }
        checkAborted();
        conn.setSocketTimeout(readTimeout);

        phase = HttpPhase.REQUEST_TRANSMISSION;
        boolean absoluteForm = proxy != null && !secure;
        BasicHttpEntityEnclosingRequest wire = toWire(absoluteForm ? proxy : null, settings.expectContinueEnabled());
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, wire);
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        HttpResponse early = send(conn, wire);

        phase = HttpPhase.RESPONSE_CONSUMPTION;
        HttpResponse r = early;
        while (r == null) {
            HttpResponse candidate = conn.receiveResponseHeader();
            int status = candidate.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK || status == HttpStatus.SC_SWITCHING_PROTOCOLS) {
                r = candidate;
            }
        }
        int status = r.getStatusLine().getStatusCode();
        if (canHaveBody(request.method(), status)) {
            conn.receiveResponseEntity(r);
        }
        this.response = r;
        checkAborted();
        return toSdkResponse(r);
    }

    private HttpClientConnection lease(HttpRoute route) throws IOException {
        phase = HttpPhase.CONNECTION_ACQUISITION;
        ConnectionRequest lease = engine.manager().requestConnection(route, null);
        synchronized (lock) {
            if (aborted) {
                lease.cancel();
            }
            pendingLease = lease;
        }
        checkAborted();
        HttpClientConnection conn;
        long timeout = options.connectionAcquisitionTimeout().orElse(engine.settings().connectionAcquisitionTimeout())
                              .toMillis();
        try {
            conn = lease.get(Math.max(1, timeout), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while acquiring a connection");
        } catch (ExecutionException e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getCause());
        } catch (java.util.concurrent.CancellationException e) {
            throw new InterruptedIOException("The connection request was cancelled");
        }
        synchronized (lock) {
            pendingLease = null;
            connection = conn;
        }
        checkAborted();
        return conn;
    }

    private void tunnel(HttpClientConnection conn, HttpHost target, ProxySettings proxy)
        throws IOException, HttpException {
        String authority = target.getHostName() + ":" + target.getPort();
        BasicHttpRequest connect = new BasicHttpRequest("CONNECT", authority, HttpVersion.HTTP_1_1);
        connect.addHeader("Host", authority);
        String authorization = proxy.basicAuthorization();
        if (authorization != null) {
            connect.addHeader("Proxy-Authorization", authorization);
        }
        conn.sendRequestHeader(connect);
        conn.flush();
        HttpResponse r;
        do {
            r = conn.receiveResponseHeader();
        } while (r.getStatusLine().getStatusCode() < HttpStatus.SC_OK);
        int status = r.getStatusLine().getStatusCode();
        if (status >= 300) {
            throw new ProxyTunnelException("The proxy refused the tunnel with status " + status);
        }
    }

    private BasicHttpEntityEnclosingRequest toWire(ProxySettings plainProxy, boolean expectContinueEnabled) {
        String target = plainProxy == null ? request.rawTarget()
                                           : request.scheme() + "://" + request.authority() + request.rawTarget();
        BasicHttpEntityEnclosingRequest wire =
            new BasicHttpEntityEnclosingRequest(new BasicRequestLine(request.method(), target, HttpVersion.HTTP_1_1));
        HttpHeaders headers = request.headers();
        boolean host = false;
        boolean length = false;
        boolean transferEncoding = false;
        boolean expect = false;
        boolean proxyAuthorization = false;
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            host |= name.equalsIgnoreCase("Host");
            length |= name.equalsIgnoreCase("Content-Length");
            transferEncoding |= name.equalsIgnoreCase("Transfer-Encoding");
            expect |= name.equalsIgnoreCase("Expect");
            proxyAuthorization |= name.equalsIgnoreCase("Proxy-Authorization");
        }
        if (!host) {
            wire.addHeader("Host", request.authority());
        }
        for (int i = 0; i < headers.size(); i++) {
            wire.addHeader(headers.name(i), headers.value(i));
        }
        HttpBody body = request.body();
        if (body.kind() != HttpBody.Kind.EMPTY) {
            long framed;
            if (transferEncoding) {
                framed = -1;
            } else if (length) {
                framed = Long.parseLong(headers.firstValue("Content-Length").orElseThrow().trim());
            } else if (body.contentLength().isPresent()) {
                framed = body.contentLength().getAsLong();
                wire.addHeader("Content-Length", Long.toString(framed));
            } else {
                framed = -1;
                wire.addHeader("Transfer-Encoding", "chunked");
            }
            if (expectContinueEnabled && !expect && framed != 0) {
                wire.addHeader("Expect", "100-continue");
            }
            wire.setEntity(new RequestEntity(body, framed));
        } else if (!length && !transferEncoding && expectsBody(request.method())) {
            wire.addHeader("Content-Length", "0");
        }
        if (plainProxy != null && !proxyAuthorization && plainProxy.basicAuthorization() != null) {
            wire.addHeader("Proxy-Authorization", plainProxy.basicAuthorization());
        }
        return wire;
    }

    /** Writes the request; returns a final response the server sent instead of {@code 100 Continue}, if any. */
    private HttpResponse send(HttpClientConnection conn, BasicHttpEntityEnclosingRequest wire)
        throws IOException, HttpException {
        conn.sendRequestHeader(wire);
        HttpResponse early = null;
        if (wire.getEntity() != null) {
            boolean sendBody = true;
            Header expect = wire.getFirstHeader("Expect");
            if (expect != null && "100-continue".equalsIgnoreCase(expect.getValue().trim())) {
                conn.flush();
                if (conn.isResponseAvailable(WAIT_FOR_CONTINUE_MILLIS)) {
                    phase = HttpPhase.RESPONSE_CONSUMPTION;
                    HttpResponse interim = conn.receiveResponseHeader();
                    int status = interim.getStatusLine().getStatusCode();
                    if (status < HttpStatus.SC_OK) {
                        if (status != HttpStatus.SC_CONTINUE) {
                            throw new ProtocolException("Unexpected interim response " + status);
                        }
                        phase = HttpPhase.REQUEST_TRANSMISSION;
                    } else {
                        early = interim;
                        sendBody = false;
                        bodyNotSent = true;
                    }
                }
            }
            if (sendBody) {
                conn.sendRequestEntity(wire);
            }
        }
        conn.flush();
        return early;
    }

    private SdkHttpResponse toSdkResponse(HttpResponse r) throws IOException, HttpException {
        SdkHttpResponse.Builder out = SdkHttpResponse.builder().statusCode(r.getStatusLine().getStatusCode());
        try {
            for (Header header : r.getAllHeaders()) {
                out.addHeader(header.getName(), header.getValue());
            }
        } catch (IllegalArgumentException e) {
            throw new ProtocolViolation("The response has a header the SDK cannot represent", e);
        }
        HttpEntity entity = r.getEntity();
        if (entity == null || entity.getContentLength() == 0) {
            release();
            return out.body(HttpBody.empty()).build();
        }
        Header type = r.getFirstHeader("Content-Type");
        long length = entity.isChunked() ? -1 : Math.max(-1, entity.getContentLength());
        ResponseBodyStream stream = new ResponseBodyStream(entity.getContent());
        return out.body(HttpBody.streaming(StreamingBodies.fromInputStream(stream, length,
                                                                           type == null ? null : type.getValue())))
                  .build();
    }

    @Override
    public void abort() {
        abort(CancellationReason.CALLER_CANCELLED);
    }

    void abort(CancellationReason reason) {
        ConnectionRequest lease;
        Socket s;
        HttpClientConnection conn;
        synchronized (lock) {
            if (aborted) {
                return;
            }
            aborted = true;
            cancellationReason = reason;
            lease = pendingLease;
            s = socket;
            conn = connection;
        }
        if (lease != null) {
            lease.cancel();
        }
        if (s != null) {
            SocketFactories.closeQuietly(s);
        }
        if (conn != null) {
            try {
                conn.shutdown();
            } catch (IOException ignored) {
                // The owning thread observes the closed connection.
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        discard();
    }

    @Override
    public void socketCreated(Socket created) throws IOException {
        synchronized (lock) {
            if (!aborted) {
                socket = created;
                return;
            }
        }
        SocketFactories.closeQuietly(created);
        throw new InterruptedIOException("The exchange was aborted");
    }

    /** The reason of an abort, if aborted. */
    CancellationReason cancellationReason() {
        return cancellationReason;
    }

    private void checkAborted() throws InterruptedIOException {
        if (aborted) {
            throw new InterruptedIOException("The exchange was aborted");
        }
    }

    private HttpTransportException fail(Throwable e) {
        return ApacheFailureMapper.map(phase, e, aborted, engine.clientName());
    }

    /** Returns the connection to the pool for reuse if the protocol allows it, else discards it. */
    private void release() {
        HttpClientConnection conn;
        synchronized (lock) {
            conn = aborted ? null : connection;
            if (conn != null) {
                connection = null;
                // Hand the socket over with the connection: a racing abort() must not close a socket that is
                // being returned to the pool as reusable (the next lease would then write to a closed socket).
                socket = null;
            }
        }
        if (conn == null) {
            discard();
            return;
        }
        boolean reusable = !bodyNotSent && conn.isOpen()
                           && DefaultConnectionReuseStrategy.INSTANCE.keepAlive(response, context);
        long maxIdle = engine.settings().connectionMaxIdleTime().toMillis();
        long keepAlive = DefaultConnectionKeepAliveStrategy.INSTANCE.getKeepAliveDuration(response, context);
        long duration = keepAlive > 0 ? Math.min(keepAlive, maxIdle) : maxIdle;
        if (!reusable) {
            shutdownQuietly(conn);
        }
        engine.manager().releaseConnection(conn, null, reusable ? duration : 0, TimeUnit.MILLISECONDS);
        untrack();
    }

    /** Shuts down and releases the connection, if still held; cancels a pending lease. Idempotent. */
    private void discard() {
        HttpClientConnection conn;
        ConnectionRequest lease;
        synchronized (lock) {
            conn = connection;
            connection = null;
            lease = pendingLease;
            pendingLease = null;
            socket = null;
        }
        if (lease != null) {
            lease.cancel();
        }
        if (conn != null) {
            shutdownQuietly(conn);
            engine.manager().releaseConnection(conn, null, 0, TimeUnit.MILLISECONDS);
        }
        untrack();
    }

    private void untrack() {
        if (tracked) {
            tracked = false;
            engine.untrack(this);
        }
    }

    private static void shutdownQuietly(HttpClientConnection conn) {
        try {
            conn.shutdown();
        } catch (IOException ignored) {
            // Discarded either way.
        }
    }

    private static boolean canHaveBody(String method, int status) {
        return !"HEAD".equalsIgnoreCase(method) && status >= HttpStatus.SC_OK && status != HttpStatus.SC_NO_CONTENT
               && status != HttpStatus.SC_NOT_MODIFIED && status != HttpStatus.SC_RESET_CONTENT;
    }

    private static boolean expectsBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    @Override
    public String toString() {
        return "ApacheExchange[" + request.method() + " " + request.host() + ", phase=" + phase + ", aborted="
               + aborted + "]";
    }

    /** The proxy answered CONNECT with a non-2xx status. */
    static final class ProxyTunnelException extends IOException {
        private static final long serialVersionUID = 1L;

        ProxyTunnelException(String message) {
            super(message);
        }
    }

    /** The response cannot be represented (for example an invalid header name). */
    static final class ProtocolViolation extends ProtocolException {
        private static final long serialVersionUID = 1L;

        ProtocolViolation(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * The response body over the connection. Reads fail with {@link HttpTransportException} (phase
     * RESPONSE_CONSUMPTION; CANCELLED after an abort). The end of the body releases the connection for reuse;
     * closing before the end discards it; after the end nothing more is read from the connection.
     */
    private final class ResponseBodyStream extends InputStream {
        private final InputStream in;
        private boolean eof;
        private boolean streamClosed;

        ResponseBodyStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (eof) {
                return -1;
            }
            if (streamClosed) {
                throw new IOException("The response stream is closed");
            }
            if (len == 0) {
                return 0;
            }
            int n;
            try {
                checkAborted();
                n = in.read(b, off, len);
            } catch (IOException | RuntimeException e) {
                HttpTransportException failure = fail(e);
                streamClosed = true;
                discard();
                throw failure;
            }
            if (n < 0) {
                eof = true;
                release();
            }
            return n;
        }

        @Override
        public int available() throws IOException {
            if (eof || streamClosed) {
                return 0;
            }
            try {
                return in.available();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public void close() {
            if (streamClosed || eof) {
                streamClosed = true;
                return;
            }
            streamClosed = true;
            discard();
        }
    }
}
