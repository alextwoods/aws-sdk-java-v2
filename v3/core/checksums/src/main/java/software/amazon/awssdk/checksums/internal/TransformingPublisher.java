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

package software.amazon.awssdk.checksums.internal;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The demand-driven view of a {@link ChunkTransform} over a source publisher.
 *
 * <p>Each subscription creates one transform and subscribes to the source once. The relay requests one source chunk
 * at a time, and only while the subscriber has unmet demand and no transformed chunk is waiting, so at most one source
 * chunk and its transformed output are buffered. A chunk whose transformation is empty is not delivered; the next one
 * is requested instead. The transform's final output is delivered under demand before {@code onComplete}. Every
 * transform call, delivery and terminal signal runs in one serialized drain loop, so the transform is never called
 * concurrently with its {@link ChunkTransform#release()}, which runs exactly once on completion, failure or
 * cancellation. A non-positive {@code request} fails the subscription (Reactive Streams rule 3.9).
 */
public final class TransformingPublisher implements Publisher<ByteBuffer> {
    private final Publisher<ByteBuffer> source;
    private final Supplier<? extends ChunkTransform> transforms;

    public TransformingPublisher(Publisher<ByteBuffer> source, Supplier<? extends ChunkTransform> transforms) {
        this.source = Objects.requireNonNull(source, "source");
        this.transforms = Objects.requireNonNull(transforms, "transforms");
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        ChunkTransform transform;
        try {
            transform = Objects.requireNonNull(transforms.get(), "transform");
        } catch (RuntimeException e) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onError(e);
            return;
        }
        source.subscribe(new Relay(subscriber, transform));
    }

    private static final class Relay implements Subscriber<ByteBuffer>, Subscription {
        private final Subscriber<? super ByteBuffer> downstream;
        private final ChunkTransform transform;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();

        private volatile Subscription upstream;
        private volatile ByteBuffer received;
        private volatile boolean upstreamInFlight;
        private volatile boolean upstreamDone;
        private volatile Throwable failure;
        private volatile boolean cancelled;

        // Owned by the drain loop.
        private ByteBuffer pending;
        private boolean finalProduced;
        private boolean terminated;

        Relay(Subscriber<? super ByteBuffer> downstream, ChunkTransform transform) {
            this.downstream = downstream;
            this.transform = transform;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            Objects.requireNonNull(subscription, "subscription");
            if (upstream != null) {
                subscription.cancel();
                return;
            }
            upstream = subscription;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            Objects.requireNonNull(chunk, "chunk");
            received = chunk;
            upstreamInFlight = false;
            drain();
        }

        @Override
        public void onError(Throwable error) {
            Objects.requireNonNull(error, "error");
            if (failure == null) {
                failure = error;
            }
            upstreamDone = true;
            upstreamInFlight = false;
            drain();
        }

        @Override
        public void onComplete() {
            upstreamDone = true;
            upstreamInFlight = false;
            drain();
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                if (failure == null) {
                    failure = new IllegalArgumentException("A request must be positive (Reactive Streams rule 3.9), "
                                                           + "but was " + n);
                }
            } else {
                long current;
                long next;
                do {
                    current = requested.get();
                    next = current + n < 0 ? Long.MAX_VALUE : current + n;
                } while (!requested.compareAndSet(current, next));
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
                drainLoop();
                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        private void drainLoop() {
            while (!terminated) {
                if (cancelled) {
                    terminate();
                    if (!upstreamDone) {
                        upstream.cancel();
                    }
                    return;
                }
                Throwable error = failure;
                if (error != null) {
                    terminate();
                    if (!upstreamDone) {
                        upstream.cancel();
                    }
                    downstream.onError(error);
                    return;
                }
                if (pending != null) {
                    if (requested.get() == 0) {
                        return;
                    }
                    ByteBuffer chunk = pending;
                    pending = null;
                    if (requested.get() != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    downstream.onNext(chunk);
                    continue;
                }
                ByteBuffer chunk = received;
                if (chunk != null) {
                    received = null;
                    transformChunk(chunk);
                    continue;
                }
                if (upstreamDone) {
                    if (!finalProduced) {
                        finalProduced = true;
                        produceFinal();
                        continue;
                    }
                    terminate();
                    downstream.onComplete();
                    return;
                }
                if (!upstreamInFlight && requested.get() > 0) {
                    upstreamInFlight = true;
                    upstream.request(1);
                    continue;
                }
                return;
            }
        }

        private void transformChunk(ByteBuffer chunk) {
            try {
                accept(transform.onChunk(chunk));
            } catch (Throwable t) {
                fail(t);
            }
        }

        private void produceFinal() {
            try {
                accept(transform.onComplete());
            } catch (Throwable t) {
                fail(t);
            }
        }

        private void accept(ByteBuffer output) {
            if (output != null && output.hasRemaining()) {
                pending = output.asReadOnlyBuffer();
            }
        }

        private void fail(Throwable t) {
            if (failure == null) {
                failure = t;
            }
        }

        private void terminate() {
            terminated = true;
            pending = null;
            received = null;
            transform.release();
        }
    }
}
