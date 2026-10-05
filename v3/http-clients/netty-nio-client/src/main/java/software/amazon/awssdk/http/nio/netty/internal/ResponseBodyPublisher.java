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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.util.concurrent.EventExecutor;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The response body of one exchange: a single-subscriber Reactive Streams publisher of read-only chunks, confined to
 * the channel's event loop.
 *
 * <p>Flow control: the channel has {@code autoRead} off; the publisher asks its {@link Source} for one more read only
 * when a subscriber has outstanding demand, the queue of received chunks is empty and no read is pending. The queue
 * therefore holds at most what one read decodes (bounded by the channel's receive allocator, see
 * {@link ChannelSetup#MAX_READ_BYTES}), which is how the transport keeps
 * {@code maxBufferedBytesPerExchange}.
 *
 * <p>Threading: every signal to the subscriber runs on {@code loop}, serially; {@code request}/{@code cancel} from
 * other threads are handed to the loop. Re-entrant {@code request} from {@code onNext} is trampolined. Only the given
 * subscriber is called on the loop.
 */
public final class ResponseBodyPublisher implements Publisher<ByteBuffer> {
    /** The channel side, called on the loop. */
    public interface Source {
        /** Read more data; results arrive through {@link #offer}, {@link #complete} or {@link #fail}. */
        void read();

        /** The subscriber cancelled: discard the connection. */
        void cancelled();

        /**
         * About to deliver {@code onComplete}; returns false if the exchange already terminated otherwise (for
         * example a racing cancellation), in which case the terminal signal is left to that path.
         */
        boolean completing();
    }

    private static final Subscription NOOP = new Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    };

    private final EventExecutor loop;
    private final Source source;
    private final AtomicBoolean subscribed = new AtomicBoolean();
    private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();

    // Loop-confined state.
    private Subscriber<? super ByteBuffer> subscriber;
    private long demand;
    private long queuedBytes;
    private long maxQueuedBytes;
    private boolean readPending;
    private boolean upstreamDone;
    private Throwable pendingError;
    private boolean terminated;
    private boolean draining;

    public ResponseBodyPublisher(EventExecutor loop, Source source) {
        this.loop = Objects.requireNonNull(loop, "loop");
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Objects.requireNonNull(s, "subscriber");
        if (!subscribed.compareAndSet(false, true)) {
            s.onSubscribe(NOOP);
            s.onError(new IllegalStateException("The response body accepts one subscriber"));
            return;
        }
        onLoop(() -> {
            subscriber = s;
            s.onSubscribe(new BodySubscription());
            drain();
        });
    }

    /** A decoded chunk (loop only). */
    public void offer(ByteBuffer chunk) {
        readPending = false;
        if (terminated || !chunk.hasRemaining()) {
            return;
        }
        queue.add(chunk);
        queuedBytes += chunk.remaining();
        maxQueuedBytes = Math.max(maxQueuedBytes, queuedBytes);
        drain();
    }

    /** The read issued by {@link Source#read} finished, possibly without a chunk (loop only). */
    public void readComplete() {
        readPending = false;
        drain();
    }

    /** The body ended (loop only); {@code onComplete} follows the queued chunks. */
    public void complete() {
        readPending = false;
        upstreamDone = true;
        drain();
    }

    /** The exchange failed or was cancelled (loop only); delivered at once, dropping queued chunks. */
    public void fail(Throwable error) {
        if (terminated) {
            return;
        }
        pendingError = error;
        upstreamDone = true;
        queue.clear();
        queuedBytes = 0;
        drain();
    }

    /** The largest number of bytes queued ahead of demand so far (loop only; for the buffer-bound tests). */
    public long maxQueuedBytes() {
        return maxQueuedBytes;
    }

    /** Forgets the subscriber after its terminal signal or cancellation (rule 3.13) and returns it. */
    private Subscriber<? super ByteBuffer> detach() {
        Subscriber<? super ByteBuffer> s = subscriber;
        subscriber = null;
        return s;
    }

    private void onLoop(Runnable task) {
        if (loop.inEventLoop()) {
            task.run();
        } else {
            loop.execute(task);
        }
    }

    private void drain() {
        if (draining || subscriber == null || terminated) {
            return;
        }
        draining = true;
        try {
            while (!terminated) {
                if (pendingError != null) {
                    terminated = true;
                    detach().onError(pendingError);
                    break;
                }
                if (demand > 0 && !queue.isEmpty()) {
                    ByteBuffer chunk = queue.poll();
                    queuedBytes -= chunk.remaining();
                    demand--;
                    subscriber.onNext(chunk);
                    continue;
                }
                if (queue.isEmpty() && upstreamDone) {
                    if (!source.completing()) {
                        // The exchange already terminated (a cancellation won the race with the end of the body):
                        // stay attached so the exchange's teardown delivers its failure through fail().
                        break;
                    }
                    terminated = true;
                    detach().onComplete();
                    break;
                }
                if (demand > 0 && queue.isEmpty() && !readPending) {
                    readPending = true;
                    source.read();
                    continue;
                }
                break;
            }
        } finally {
            draining = false;
        }
    }

    private final class BodySubscription implements Subscription {
        @Override
        public void request(long n) {
            onLoop(() -> {
                if (terminated) {
                    return;
                }
                if (n <= 0) {
                    terminated = true;
                    queue.clear();
                    Subscriber<? super ByteBuffer> s = detach();
                    source.cancelled();
                    s.onError(new IllegalArgumentException("Rule 3.9: request " + n + " is not positive"));
                    return;
                }
                demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                drain();
            });
        }

        @Override
        public void cancel() {
            onLoop(() -> {
                if (terminated) {
                    return;
                }
                terminated = true;
                queue.clear();
                queuedBytes = 0;
                detach();
                source.cancelled();
            });
        }
    }
}
