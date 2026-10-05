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
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.HttpBody;

/**
 * Request bodies as CRT {@link HttpRequestBodyStream}s, which the CRT pulls on its event loop whenever the connection
 * can write. None throws into the JNI callback: a source failure is reported to {@code onError} once, and the
 * exchange then cancels the native stream. None is reset or reopened within an exchange (resetPosition is false for a
 * streaming body), so a one-shot body is never partially retransmitted.
 */
public final class CrtRequestBodies {
    private CrtRequestBodies() {
    }

    /** A buffered body. */
    public static HttpRequestBodyStream bytes(HttpBody body) {
        ByteBuffer data = body.asByteBuffer();
        return new HttpRequestBodyStream() {
            private ByteBuffer remaining = data.duplicate();

            @Override
            public boolean sendRequestBody(ByteBuffer out) {
                int n = Math.min(out.remaining(), remaining.remaining());
                ByteBuffer slice = remaining.slice(remaining.position(), n);
                out.put(slice);
                remaining.position(remaining.position() + n);
                return !remaining.hasRemaining();
            }

            @Override
            public boolean resetPosition() {
                remaining = data.duplicate();
                return true;
            }

            @Override
            public long getLength() {
                return data.remaining();
            }
        };
    }

    /** A blocking stream (sync clients): read on the event loop, as V2's CRT sync client does. */
    public static final class FromInputStream implements HttpRequestBodyStream, AutoCloseable {
        private final InputStream stream;
        private final long length;
        private final Consumer<Throwable> onError;
        private final AtomicBoolean failed = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final byte[] buffer = new byte[16 * 1024];

        public FromInputStream(InputStream stream, long length, Consumer<Throwable> onError) {
            this.stream = stream;
            this.length = length;
            this.onError = onError;
        }

        @Override
        public boolean sendRequestBody(ByteBuffer out) {
            if (failed.get() || closed.get()) {
                return false;
            }
            try {
                int read = stream.read(buffer, 0, Math.min(buffer.length, out.remaining()));
                if (read > 0) {
                    out.put(buffer, 0, read);
                }
                return read < 0;
            } catch (IOException | RuntimeException e) {
                if (failed.compareAndSet(false, true)) {
                    onError.accept(e);
                }
                return false;
            }
        }

        @Override
        public boolean resetPosition() {
            return false;
        }

        @Override
        public long getLength() {
            return length;
        }

        /** Closes the stream the exchange opened; idempotent. */
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                try {
                    stream.close();
                } catch (IOException | RuntimeException e) {
                    // The body was already sent or the exchange already failed; nothing to report.
                }
            }
        }
    }

    /**
     * A {@link Publisher} body (async clients). The publisher is subscribed on the first pull, and exactly one chunk
     * is requested at a time, only when the CRT has drained the previous one, so at most one chunk is buffered and
     * demand follows what the connection can write (Requirement 12.5). The CRT polls again when a pull yields no
     * bytes.
     */
    public static final class FromPublisher implements HttpRequestBodyStream, Subscriber<ByteBuffer> {
        private final Publisher<ByteBuffer> publisher;
        private final long length;
        private final Consumer<Throwable> onError;
        private Subscription subscription;
        private ByteBuffer current;
        private boolean subscribed;
        private boolean requested;
        private boolean done;
        private boolean failed;
        private boolean cancelled;

        public FromPublisher(Publisher<ByteBuffer> publisher, long length, Consumer<Throwable> onError) {
            this.publisher = publisher;
            this.length = length;
            this.onError = onError;
        }

        @Override
        public boolean sendRequestBody(ByteBuffer out) {
            boolean subscribeNow = false;
            Subscription toRequest = null;
            boolean finished;
            synchronized (this) {
                if (failed || cancelled) {
                    return false;
                }
                if (!subscribed) {
                    subscribed = true;
                    requested = true;
                    subscribeNow = true;
                } else {
                    if (current != null) {
                        int n = Math.min(out.remaining(), current.remaining());
                        ByteBuffer slice = current.slice(current.position(), n);
                        out.put(slice);
                        current.position(current.position() + n);
                        if (!current.hasRemaining()) {
                            current = null;
                        }
                    }
                    if (current == null && !requested && !done && subscription != null) {
                        requested = true;
                        toRequest = subscription;
                    }
                }
                finished = done && current == null;
            }
            if (subscribeNow) {
                publisher.subscribe(this);
            } else if (toRequest != null) {
                toRequest.request(1);
            }
            return finished;
        }

        @Override
        public boolean resetPosition() {
            return false;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public void onSubscribe(Subscription s) {
            boolean cancelNow;
            synchronized (this) {
                cancelNow = subscription != null || cancelled;
                if (!cancelNow) {
                    subscription = s;
                }
            }
            if (cancelNow) {
                s.cancel();
            } else {
                s.request(1);
            }
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            synchronized (this) {
                requested = false;
                if (!cancelled) {
                    current = chunk.hasRemaining() ? chunk.duplicate() : null;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            synchronized (this) {
                if (cancelled || failed) {
                    return;
                }
                failed = true;
                current = null;
            }
            onError.accept(t);
        }

        @Override
        public void onComplete() {
            synchronized (this) {
                done = true;
            }
        }

        /** Cancels the body subscription (the exchange failed or was cancelled); idempotent. */
        public void cancel() {
            Subscription s;
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                current = null;
                s = subscription;
            }
            if (s != null) {
                s.cancel();
            }
        }
    }
}
