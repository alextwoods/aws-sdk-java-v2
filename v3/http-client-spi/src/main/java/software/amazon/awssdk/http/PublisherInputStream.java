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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A blocking view of a publisher: subscribes on the first read and requests one chunk at a time, only once the reader
 * has consumed the previous one, so at most one chunk (plus a terminal signal) is ever queued. Closing cancels the
 * subscription. Not thread safe: one reader at a time, like any {@link InputStream}.
 */
final class PublisherInputStream extends InputStream {
    private static final Object COMPLETE = new Object();

    private final Publisher<? extends ByteBuffer> publisher;
    private final BlockingQueue<Object> signals = new LinkedBlockingQueue<>();
    private volatile Subscription subscription;
    private boolean subscribed;
    private boolean requestNext;
    private boolean ended;
    private volatile boolean closed;
    private ByteBuffer current;

    PublisherInputStream(Publisher<? extends ByteBuffer> publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public int read() throws IOException {
        if (!fill()) {
            return -1;
        }
        return current.get() & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (!fill()) {
            return -1;
        }
        int n = Math.min(len, current.remaining());
        current.get(b, off, n);
        return n;
    }

    /** Makes {@link #current} non-empty, waiting for the next chunk; false at the end of the stream. */
    private boolean fill() throws IOException {
        if (closed) {
            throw new IOException("The stream is closed");
        }
        while (current == null || !current.hasRemaining()) {
            if (ended) {
                return false;
            }
            if (!subscribed) {
                subscribed = true;
                publisher.subscribe(new Relay());
            } else if (requestNext) {
                requestNext = false;
                subscription.request(1);
            }
            Object signal;
            try {
                signal = signals.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                close();
                throw new InterruptedIOException("Interrupted while waiting for body data");
            }
            if (signal == COMPLETE) {
                ended = true;
            } else if (signal instanceof Throwable t) {
                ended = true;
                throw t instanceof IOException io ? io : new IOException(t.getMessage(), t);
            } else {
                current = (ByteBuffer) signal;
                requestNext = true;
            }
        }
        return true;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Subscription s = subscription;
            if (s != null && !ended) {
                s.cancel();
            }
        }
    }

    private final class Relay implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            if (closed) {
                s.cancel();
                return;
            }
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            signals.add(chunk);
        }

        @Override
        public void onError(Throwable t) {
            signals.add(t);
        }

        @Override
        public void onComplete() {
            signals.add(COMPLETE);
        }
    }
}
