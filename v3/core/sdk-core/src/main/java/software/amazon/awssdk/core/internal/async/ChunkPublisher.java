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

package software.amazon.awssdk.core.internal.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A Reactive Streams publisher of the chunks of a {@link ChunkSource}, opened afresh for every subscriber, with exact
 * demand: a chunk is read only under outstanding demand and never more chunks than requested are delivered.
 *
 * <p>Signals are serialized by a work-in-progress counter, so a {@code request} from inside {@code onNext} does not
 * recurse (rule 3.3). A non-positive {@code request} fails the subscription with {@link IllegalArgumentException}
 * (rule 3.9); cumulative demand saturates at {@link Long#MAX_VALUE} (rule 3.17). The source is read on the thread that
 * calls {@code request}; the pipeline moves blocking sources off event loops (task 7.1).
 */
public final class ChunkPublisher implements Publisher<ByteBuffer> {
    private final ChunkSource.Factory sources;

    public ChunkPublisher(ChunkSource.Factory sources) {
        this.sources = Objects.requireNonNull(sources, "sources");
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        new ChunkSubscription(subscriber, sources).start();
    }

    private static final class ChunkSubscription implements Subscription {
        private final Subscriber<? super ByteBuffer> downstream;
        private final ChunkSource.Factory sources;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private volatile boolean cancelled;
        private volatile Throwable invalidRequest;
        private boolean done;
        private ChunkSource source;

        private ChunkSubscription(Subscriber<? super ByteBuffer> downstream, ChunkSource.Factory sources) {
            this.downstream = downstream;
            this.sources = sources;
        }

        private void start() {
            downstream.onSubscribe(this);
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                invalidRequest = new IllegalArgumentException(
                    "A request must be positive (Reactive Streams rule 3.9), but was " + n + ".");
            } else {
                demand.accumulateAndGet(n, (current, delta) -> {
                    long sum = current + delta;
                    return sum < 0 ? Long.MAX_VALUE : sum;
                });
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
                    emit();
                }
                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        /** Delivers chunks while there is demand; runs only inside the serialized drain loop. */
        private void emit() {
            while (true) {
                if (cancelled) {
                    finish();
                    return;
                }
                Throwable invalid = invalidRequest;
                if (invalid != null) {
                    finish();
                    downstream.onError(invalid);
                    return;
                }
                if (demand.get() == 0) {
                    return;
                }
                ByteBuffer chunk;
                try {
                    if (source == null) {
                        source = sources.open();
                    }
                    chunk = source.next();
                } catch (IOException | RuntimeException e) {
                    finish();
                    downstream.onError(e);
                    return;
                }
                if (chunk == null) {
                    finish();
                    downstream.onComplete();
                    return;
                }
                if (demand.get() != Long.MAX_VALUE) {
                    demand.decrementAndGet();
                }
                downstream.onNext(chunk);
            }
        }

        private void finish() {
            done = true;
            if (source != null) {
                try {
                    source.close();
                } catch (IOException | RuntimeException ignored) {
                    // The terminal signal (or cancellation) is already decided; a close failure has nowhere to go.
                }
                source = null;
            }
        }
    }
}
