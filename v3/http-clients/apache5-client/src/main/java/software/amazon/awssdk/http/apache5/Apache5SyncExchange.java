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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.CancellationException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.net.URIAuthority;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.apache5.internal.Apache5FailureMapping;

/** One exchange of {@link Apache5HttpClient}; see {@link SyncHttpExchange} for the ownership rules it implements. */
final class Apache5SyncExchange implements SyncHttpExchange {
    private static final URI PLACEHOLDER = URI.create("/");

    private final Apache5HttpClient owner;
    private final SdkHttpRequest request;
    private final RequestConfig requestConfig;
    private final PhaseTracker tracker = new PhaseTracker();
    private volatile boolean aborted;
    private volatile HttpUriRequestBase engineRequest;
    private boolean executed;
    private boolean closed;
    private ClassicHttpResponse response;
    private ResponseStream responseStream;
    private InputStream requestStream;

    Apache5SyncExchange(Apache5HttpClient owner, SdkHttpRequest request, RequestConfig requestConfig) {
        this.owner = owner;
        this.request = request;
        this.requestConfig = requestConfig;
    }

    @Override
    public SdkHttpResponse execute() {
        if (executed || closed) {
            throw new IllegalStateException(closed ? "The exchange is closed" : "The exchange was already executed");
        }
        executed = true;
        if (!owner.track(this)) {
            throw new IllegalStateException("The " + Apache5HttpClient.CLIENT_NAME + " HTTP client is closed");
        }
        try {
            HttpUriRequestBase engine = engineRequest();
            engineRequest = engine;
            if (aborted) {
                engine.cancel();
                throw failure(null);
            }
            HttpClientContext context = HttpClientContext.create();
            context.setRequestConfig(requestConfig);
            context.setAttribute(Apache5HttpClient.PHASE_ATTRIBUTE, tracker);
            HttpHost target = new HttpHost(request.scheme(), request.host(), request.port());
            response = owner.engine().executeOpen(target, engine, context);
            tracker.enter(HttpPhase.RESPONSE_CONSUMPTION);
            closeRequestStream();
            if (aborted) {
                throw failure(null);
            }
            return toResponse(response);
        } catch (HttpTransportException e) {
            release(true);
            throw e;
        } catch (IOException | RuntimeException e) {
            HttpTransportException mapped = failure(e);
            release(true);
            throw mapped;
        }
    }

    private HttpUriRequestBase engineRequest() {
        HttpUriRequestBase engine = new HttpUriRequestBase(request.method(), PLACEHOLDER);
        engine.setScheme(request.scheme());
        engine.setAuthority(new URIAuthority(request.host(), request.port()));
        engine.setPath(request.rawTarget());
        HttpHeaders headers = request.headers();
        String declaredLength = null;
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            if (name.equalsIgnoreCase("Content-Length")) {
                declaredLength = headers.value(i);
            } else if (!name.equalsIgnoreCase("Transfer-Encoding")) {
                engine.addHeader(name, headers.value(i));
            }
        }
        HttpEntity entity = entity(request.body(), declaredLength);
        if (entity != null) {
            engine.setEntity(entity);
        }
        return engine;
    }

    private HttpEntity entity(HttpBody body, String declaredLength) {
        long length = declaredLength == null ? body.contentLength().orElse(-1) : parseLength(declaredLength);
        return switch (body.kind()) {
            case EMPTY -> declaredLength == null ? null : new BodyEntity(null, new byte[0], 0);
            case BYTES -> new BodyEntity(null, body.asByteArray(), length);
            case STREAMING -> new BodyEntity(body.streamingBody(), null, length);
        };
    }

    private static long parseLength(String value) {
        try {
            long length = Long.parseLong(value.trim());
            if (length < 0) {
                throw new NumberFormatException();
            }
            return length;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The Content-Length header is not a non-negative integer");
        }
    }

    private SdkHttpResponse toResponse(ClassicHttpResponse engineResponse) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(engineResponse.getCode());
        for (Header header : engineResponse.getHeaders()) {
            builder.addHeader(header.getName(), header.getValue());
        }
        HttpEntity entity = engineResponse.getEntity();
        if (entity == null) {
            release(false);
            return builder.body(HttpBody.empty()).build();
        }
        InputStream content;
        try {
            content = entity.getContent();
        } catch (IOException e) {
            throw failure(e);
        }
        responseStream = new ResponseStream(content);
        long length = entity.getContentLength();
        StreamingBody body = StreamingBodies.fromInputStream(responseStream, length, entity.getContentType());
        return builder.body(HttpBody.streaming(body)).build();
    }

    private HttpTransportException failure(Throwable cause) {
        boolean cancelled = aborted || owner.isClosed();
        if (cause == null) {
            return Apache5FailureMapping.toTransportException(new CancellationException("The exchange was aborted"),
                                                              tracker.phase, true, Apache5HttpClient.CLIENT_NAME);
        }
        return Apache5FailureMapping.toTransportException(cause, tracker.phase, cancelled,
                                                          Apache5HttpClient.CLIENT_NAME);
    }

    @Override
    public void abort() {
        aborted = true;
        HttpUriRequestBase engine = engineRequest;
        if (engine != null) {
            engine.cancel();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        boolean consumed = responseStream != null && responseStream.eof && responseStream.closed;
        release(!consumed && response != null && response.getEntity() != null);
    }

    /** Ends the exchange: discards the connection (abort, then close) or returns it to the pool. */
    private void release(boolean discard) {
        try {
            if (discard) {
                abort();
            }
            closeRequestStream();
            if (responseStream != null) {
                responseStream.closeQuietly();
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException | RuntimeException ignored) {
                    // The connection is being discarded or already released.
                }
            }
        } finally {
            owner.untrack(this);
        }
    }

    private void closeRequestStream() {
        InputStream stream = requestStream;
        requestStream = null;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException | RuntimeException ignored) {
                // The request body source failed to close; the exchange result stands.
            }
        }
    }

    @Override
    public String toString() {
        return "Apache5SyncExchange[" + request.method() + " " + request.scheme() + "://" + request.authority()
               + request.encodedPath() + ", " + tracker.phase + (aborted ? ", aborted" : "")
               + (closed ? ", closed" : "") + "]";
    }

    /** The phase of the exchange, recorded by the client's request executor and the body entity. */
    static final class PhaseTracker {
        volatile HttpPhase phase = HttpPhase.CONNECTION_ACQUISITION;

        void enter(HttpPhase phase) {
            this.phase = phase;
        }
    }

    /** The request body: buffered bytes, or one {@link StreamingBody#openStream()} closed when writing ends. */
    private final class BodyEntity extends AbstractHttpEntity {
        private final StreamingBody streaming;
        private final byte[] bytes;
        private final long length;

        BodyEntity(StreamingBody streaming, byte[] bytes, long length) {
            super((String) null, null, length < 0);
            this.streaming = streaming;
            this.bytes = bytes;
            this.length = length;
        }

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public long getContentLength() {
            return length;
        }

        @Override
        public InputStream getContent() {
            throw new UnsupportedOperationException("The request body is written, not read");
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            tracker.enter(HttpPhase.REQUEST_TRANSMISSION);
            if (bytes != null) {
                out.write(bytes);
            } else {
                InputStream in = streaming.openStream();
                requestStream = in;
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                }
                closeRequestStream();
            }
            out.flush();
            tracker.enter(HttpPhase.RESPONSE_CONSUMPTION);
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public void close() {
            closeRequestStream();
        }
    }

    /**
     * The response body stream: failures surface as {@link HttpTransportException} of the response phase (or
     * {@code CANCELLED} after an abort); closing it after its end returns the connection, closing it early discards
     * the connection without draining.
     */
    private final class ResponseStream extends InputStream {
        private final InputStream delegate;
        volatile boolean eof;
        volatile boolean closed;

        ResponseStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (closed) {
                throw new IllegalStateException("The response body stream is closed");
            }
            if (aborted) {
                throw failure(null);
            }
            try {
                int n = delegate.read(b, off, len);
                if (n < 0) {
                    eof = true;
                }
                return n;
            } catch (IOException | RuntimeException e) {
                throw failure(e);
            }
        }

        @Override
        public int available() throws IOException {
            return closed || aborted ? 0 : delegate.available();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (!eof) {
                abort();
            }
            closeQuietly();
        }

        void closeQuietly() {
            closed = true;
            try {
                delegate.close();
            } catch (IOException | RuntimeException ignored) {
                // Discarded connection or already released.
            }
        }

        @Override
        public String toString() {
            return "Apache5ResponseStream" + (eof ? "[eof]" : "") + (closed ? "[closed]" : "");
        }
    }
}
