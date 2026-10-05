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
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A demand-driven publisher over a blocking source: each subscription opens the source lazily and reads at most one
 * chunk of at most {@code maxChunkSize} bytes per unit of demand, so it buffers nothing beyond the chunk being
 * delivered. Reads happen on the thread that calls {@code request} (task 7.1 moves blocking sources off event
 * loops). Signals are serialized by a work-in-progress counter, so a {@code request} from inside {@code onNext} does
 * not recurse.
 */
final class InputStreamPublisher implements Publisher<ByteBuffer> {
    /** Opens the blocking source of one subscription. */
    @FunctionalInterface
    interface Source {
        InputStream open() throws IOException;
    }

    private final Source source;
    private final int maxChunkSize;
    private final long contentLength;

    InputStreamPublisher(Source source, int maxChunkSize, long contentLength) {
        this.source = source;
        this.maxChunkSize = maxChunkSize;
        this.contentLength = contentLength;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        subscriber.onSubscribe(new PullSubscription(subscriber, source, maxChunkSize, contentLength));
    }

    private static final class PullSubscription implements Subscription {
        private final Subscriber<? super ByteBuffer> downstream;
        private final Source source;
        private final int maxChunkSize;
        private final long contentLength;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private volatile boolean cancelled;
        private volatile Throwable invalidRequest;

        // Owned by the draining thread.
        private InputStream in;
        private boolean done;

        PullSubscription(Subscriber<? super ByteBuffer> downstream, Source source, int maxChunkSize,
                         long contentLength) {
            this.downstream = downstream;
            this.source = source;
            this.maxChunkSize = maxChunkSize;
            this.contentLength = contentLength;
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
                    finish();
                    return;
                }
                Throwable invalid = invalidRequest;
                if (invalid != null) {
                    fail(invalid);
                    return;
                }
                if (demand.get() == 0) {
                    return;
                }
                ByteBuffer chunk;
                try {
                    chunk = readChunk();
                } catch (Throwable t) {
                    fail(t);
                    return;
                }
                if (chunk == null) {
                    finish();
                    downstream.onComplete();
                    return;
                }
                demand.decrementAndGet();
                try {
                    downstream.onNext(chunk);
                } catch (Throwable t) {
                    // Rule 2.13: a throwing subscriber is treated as having cancelled.
                    finish();
                    return;
                }
            }
        }

        /** The next non-empty chunk, or {@code null} at the end of the source. */
        private ByteBuffer readChunk() throws IOException {
            if (in == null) {
                in = LengthCheckedInputStream.of(Objects.requireNonNull(source.open(), "source stream"),
                                                 contentLength);
            }
            byte[] buffer = new byte[maxChunkSize];
            while (true) {
                int n = in.read(buffer, 0, buffer.length);
                if (n < 0) {
                    return null;
                }
                if (n > 0) {
                    return ByteBuffer.wrap(buffer, 0, n).asReadOnlyBuffer();
                }
            }
        }

        private void fail(Throwable t) {
            finish();
            downstream.onError(t);
        }

        private void finish() {
            done = true;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException | RuntimeException ignored) {
                    // Closing releases the source; a failure to close has nowhere to go after the terminal signal.
                }
                in = null;
            }
        }
    }
}
