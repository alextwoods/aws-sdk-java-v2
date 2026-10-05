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
import java.util.Objects;
import java.util.Set;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The async request body: subscribes to the body publisher once, on the connection's first write opportunity, and
 * requests one chunk at a time, only after the previous chunk is fully written (Requirement 12.5). At most one chunk
 * is buffered. The subscription is cancelled if the exchange fails or is cancelled.
 *
 * <p>An in-memory body ({@link #ofBuffer}) has no publisher: its single buffer is the one chunk, already complete, and
 * is written as the connection accepts it.
 */
final class PublisherEntityProducer implements AsyncEntityProducer, Subscriber<ByteBuffer> {
    private final Publisher<ByteBuffer> body;
    private final long length;
    private final Runnable written;
    private Subscription subscription;
    private ByteBuffer current;
    private boolean started;
    private boolean complete;
    private boolean cancelled;
    private Throwable error;
    private DataStreamChannel channel;

    /**
     * @param length the declared length, or -1 for chunked transfer
     * @param written run once when the body is fully written
     */
    PublisherEntityProducer(Publisher<ByteBuffer> body, long length, Runnable written) {
        this.body = Objects.requireNonNull(body, "body");
        this.length = length;
        this.written = written;
    }

    private PublisherEntityProducer(ByteBuffer content, long length, Runnable written) {
        this.body = null;
        this.length = length;
        this.written = written;
        this.current = content;
        this.complete = true;
    }

    /**
     * An in-memory body, written from {@code content} itself (no copy, no chunk buffer).
     *
     * @param content the bytes, consumed from its position to its limit; must not be modified until written
     * @param length the declared length, or -1 for chunked transfer
     * @param written run once when the body is fully written
     */
    static PublisherEntityProducer ofBuffer(ByteBuffer content, long length, Runnable written) {
        return new PublisherEntityProducer(Objects.requireNonNull(content, "content"), length, written);
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
    public String getContentType() {
        return null;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public boolean isChunked() {
        return length < 0;
    }

    @Override
    public Set<String> getTrailerNames() {
        return null;
    }

    @Override
    public synchronized int available() {
        if (!started || complete || error != null) {
            return 1;
        }
        return current == null ? 0 : Math.max(1, current.remaining());
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        boolean subscribe;
        synchronized (this) {
            this.channel = channel;
            subscribe = !started;
            started = true;
        }
        if (subscribe && body != null) {
            body.subscribe(this);
        }
        while (true) {
            Subscription toRequest = null;
            synchronized (this) {
                if (error != null) {
                    throw error instanceof IOException io ? io : new IOException("The request body failed", error);
                }
                if (current != null) {
                    channel.write(current);
                    if (current.hasRemaining()) {
                        return;
                    }
                    current = null;
                    if (!complete) {
                        toRequest = subscription;
                    }
                }
                if (current == null && complete) {
                    channel.endStream();
                    complete = false;
                    cancelled = true;
                    written.run();
                    return;
                }
            }
            if (toRequest == null) {
                return;
            }
            toRequest.request(1);
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        Objects.requireNonNull(s, "subscription");
        synchronized (this) {
            if (subscription != null || cancelled) {
                s.cancel();
                return;
            }
            subscription = s;
        }
        s.request(1);
    }

    @Override
    public void onNext(ByteBuffer chunk) {
        Objects.requireNonNull(chunk, "chunk");
        DataStreamChannel ch;
        synchronized (this) {
            current = chunk;
            ch = channel;
        }
        ch.requestOutput();
    }

    @Override
    public void onError(Throwable t) {
        Objects.requireNonNull(t, "error");
        DataStreamChannel ch;
        synchronized (this) {
            error = t;
            ch = channel;
        }
        ch.requestOutput();
    }

    @Override
    public void onComplete() {
        DataStreamChannel ch;
        synchronized (this) {
            complete = true;
            ch = channel;
        }
        ch.requestOutput();
    }

    /** Cancels the body subscription; idempotent. */
    void cancel() {
        Subscription s;
        synchronized (this) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            s = subscription;
        }
        if (s != null) {
            s.cancel();
        }
    }

    @Override
    public void failed(Exception cause) {
        cancel();
    }

    @Override
    public void releaseResources() {
        cancel();
    }
}
