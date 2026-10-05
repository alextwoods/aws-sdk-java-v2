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

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The async response body: a single-subscriber Reactive Streams publisher of read-only chunks with exact demand
 * (Requirement 12.5). The connection offers chunks only within the credit this publisher grants: at most
 * {@code window} bytes buffered ahead of demand when the engine asks for capacity, plus one grant per delivered chunk
 * of its size, so buffering stays within the protocol's initial window plus {@code window}.
 *
 * <p>All subscriber signals run in one serial drain loop, so they are never concurrent or recursive, whichever
 * thread offers data, requests, cancels or fails.
 */
final class ResponseBodyPublisher implements Publisher<ByteBuffer> {
    /** The connection side. */
    interface Upstream {
        /** Grants the connection {@code bytes} more body bytes. */
        void request(int bytes);

        /** The subscriber cancelled: discard the connection. */
        void cancel();

        /** The body ended and was delivered; true if normal completion of the exchange won. */
        boolean complete();
    }

    private static final Subscription NOOP = new Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    };

    private final int window;
    private final Upstream upstream;
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong demand = new AtomicLong();
    private final AtomicInteger wip = new AtomicInteger();
    private final AtomicBoolean subscribed = new AtomicBoolean();
    private final AtomicLong buffered = new AtomicLong();
    private final AtomicLong highWater = new AtomicLong();
    private volatile Subscriber<? super ByteBuffer> subscriber;
    private volatile boolean ended;
    private volatile boolean cancelled;
    private volatile Throwable error;
    private boolean terminated;

    ResponseBodyPublisher(int window, Upstream upstream) {
        this.window = window;
        this.upstream = upstream;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Objects.requireNonNull(s, "subscriber");
        if (!subscribed.compareAndSet(false, true)) {
            s.onSubscribe(NOOP);
            s.onError(new IllegalStateException("The response body accepts one subscriber"));
            return;
        }
        s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    fail(new IllegalArgumentException("Non-positive request (Reactive Streams rule 3.9): " + n));
                    upstream.cancel();
                    return;
                }
                demand.getAndUpdate(d -> d + n < 0 ? Long.MAX_VALUE : d + n);
                drain();
            }

            @Override
            public void cancel() {
                if (!cancelled) {
                    cancelled = true;
                    upstream.cancel();
                    drain();
                }
            }
        });
        subscriber = s;
        drain();
    }

    /** The engine asks for capacity: grant up to the window beyond what is buffered. */
    void onCapacityRequested() {
        long grant = window - buffered.get();
        if (grant > 0) {
            upstream.request((int) grant);
        }
    }

    /** A body chunk from the connection; {@code chunk} is owned by this publisher. */
    void offer(ByteBuffer chunk) {
        queue.add(chunk);
        long now = buffered.addAndGet(chunk.remaining());
        highWater.accumulateAndGet(now, Math::max);
        drain();
    }

    /** The body ended. */
    void end() {
        ended = true;
        drain();
    }

    /** The body failed; the first failure wins and is signalled without waiting for demand. */
    void fail(Throwable failure) {
        if (error == null) {
            error = failure;
        }
        drain();
    }

    /** The largest number of body bytes buffered ahead of demand so far. */
    long highWaterBytes() {
        return highWater.get();
    }

    private void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }
        int missed = 1;
        do {
            Subscriber<? super ByteBuffer> s = subscriber;
            if (s != null && !terminated) {
                if (cancelled) {
                    terminated = true;
                    queue.clear();
                    subscriber = null;
                } else if (error != null) {
                    terminated = true;
                    queue.clear();
                    s.onError(error);
                    subscriber = null;
                } else {
                    emit(s);
                }
            }
            missed = wip.addAndGet(-missed);
        } while (missed != 0);
    }

    private void emit(Subscriber<? super ByteBuffer> s) {
        long d = demand.get();
        long e = 0;
        while (e != d && !cancelled && error == null) {
            ByteBuffer chunk = queue.poll();
            if (chunk == null) {
                break;
            }
            int n = chunk.remaining();
            buffered.addAndGet(-n);
            s.onNext(chunk);
            e++;
            if (n > 0 && !cancelled) {
                upstream.request(n);
            }
        }
        if (e != 0 && d != Long.MAX_VALUE) {
            demand.addAndGet(-e);
        }
        if (!cancelled && error == null && ended && queue.isEmpty() && upstream.complete()) {
            terminated = true;
            s.onComplete();
            subscriber = null;
        }
    }

    @Override
    public String toString() {
        return "Apache5ResponseBodyPublisher[buffered " + buffered.get() + " bytes" + (ended ? ", ended" : "")
               + (cancelled ? ", cancelled" : "") + "]";
    }
}
