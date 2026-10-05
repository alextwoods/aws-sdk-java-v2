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

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Adapts an upstream publisher of buffers to the {@link StreamingBody} publisher contract: it requests one upstream
 * buffer at a time and only while downstream demand is positive, delivers it as zero-copy read-only slices of at most
 * {@code maxChunkSize} bytes, drops empty buffers, enforces a known content length, and signals exactly one
 * terminal. It holds at most one upstream buffer and never copies data.
 */
final class BoundedChunkPublisher implements Publisher<ByteBuffer> {
    private final Publisher<? extends ByteBuffer> upstream;
    private final int maxChunkSize;
    private final long contentLength;

    BoundedChunkPublisher(Publisher<? extends ByteBuffer> upstream, int maxChunkSize, long contentLength) {
        this.upstream = upstream;
        this.maxChunkSize = maxChunkSize;
        this.contentLength = contentLength;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        upstream.subscribe(new Relay(subscriber, maxChunkSize, contentLength));
    }

    private static final class Relay implements Subscriber<ByteBuffer>, Subscription {
        private final Subscriber<? super ByteBuffer> downstream;
        private final int maxChunkSize;
        private final long contentLength;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private volatile Subscription upstream;
        private volatile ByteBuffer pending;
        private volatile boolean upstreamDone;
        private volatile Throwable upstreamError;
        private volatile boolean cancelled;
        private volatile Throwable invalidRequest;

        // Owned by the draining thread.
        private boolean requested;
        private boolean upstreamCancelled;
        private boolean done;
        private long delivered;

        Relay(Subscriber<? super ByteBuffer> downstream, int maxChunkSize, long contentLength) {
            this.downstream = downstream;
            this.maxChunkSize = maxChunkSize;
            this.contentLength = contentLength;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream != null) {
                subscription.cancel(); // rule 2.5
                return;
            }
            upstream = subscription;
            downstream.onSubscribe(this);
            drain();
        }

        @Override
        public void onNext(ByteBuffer item) {
            pending = Objects.requireNonNull(item, "item").duplicate();
            drain();
        }

        @Override
        public void onError(Throwable throwable) {
            upstreamError = Objects.requireNonNull(throwable, "throwable");
            upstreamDone = true;
            drain();
        }

        @Override
        public void onComplete() {
            upstreamDone = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                invalidRequest = new IllegalArgumentException("Reactive Streams rule 3.9: request(" + n + ")");
            } else {
                Demand.add(demand, n);
            }
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            drain();
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            do {
                if (!done) {
                    step();
                }
                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        private void step() {
            while (true) {
                if (cancelled) {
                    done = true;
                    pending = null;
                    cancelUpstream();
                    return;
                }
                Throwable invalid = invalidRequest;
                if (invalid != null) {
                    cancelUpstream();
                    fail(invalid);
                    return;
                }
                ByteBuffer buffer = pending;
                if (buffer != null) {
                    requested = false;
                    if (!buffer.hasRemaining()) {
                        pending = null;
                        continue;
                    }
                    if (demand.get() == 0) {
                        return;
                    }
                    int n = Math.min(maxChunkSize, buffer.remaining());
                    if (contentLength >= 0 && delivered + n > contentLength) {
                        cancelUpstream();
                        fail(new ContentLengthMismatchException(contentLength, delivered + n));
                        return;
                    }
                    ByteBuffer chunk = buffer.slice(buffer.position(), n).asReadOnlyBuffer();
                    buffer.position(buffer.position() + n);
                    if (!buffer.hasRemaining()) {
                        pending = null;
                    }
                    delivered += n;
                    demand.decrementAndGet();
                    try {
                        downstream.onNext(chunk);
                    } catch (Throwable t) {
                        // Rule 2.13: a throwing subscriber is treated as having cancelled.
                        done = true;
                        pending = null;
                        cancelUpstream();
                        return;
                    }
                    continue;
                }
                if (upstreamDone) {
                    done = true;
                    Throwable error = upstreamError;
                    if (error == null && contentLength >= 0 && delivered != contentLength) {
                        error = new ContentLengthMismatchException(contentLength, delivered);
                    }
                    if (error != null) {
                        downstream.onError(error);
                    } else {
                        downstream.onComplete();
                    }
                    return;
                }
                if (demand.get() > 0 && !requested) {
                    requested = true;
                    upstream.request(1);
                    continue; // the upstream may have delivered synchronously
                }
                return;
            }
        }

        private void fail(Throwable t) {
            done = true;
            pending = null;
            downstream.onError(t);
        }

        private void cancelUpstream() {
            if (!upstreamCancelled) {
                upstreamCancelled = true;
                upstream.cancel();
            }
        }
    }
}
