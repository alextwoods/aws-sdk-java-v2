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

package software.amazon.awssdk.http.tck;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBody;

/**
 * Recording handler, response-body subscriber and request-body publisher of the async suite. They record every
 * signal and every protocol violation (a signal out of order, a chunk without demand, a second terminal) so the
 * suite can assert the universal async rules: signal order, demand, and exactly one terminal signal per exchange.
 */
final class AsyncProbes {

    private AsyncProbes() {
    }

    /**
     * An {@link AsyncHttpResponseHandler} that subscribes a {@link BodyProbe} to the body as soon as it arrives. With
     * {@code autoDemand} the probe requests one chunk at a time as chunks arrive; otherwise it requests nothing until
     * the check calls {@link BodyProbe#request(long)}.
     */
    static final class Recorder implements AsyncHttpResponseHandler {
        final BodyProbe body;
        final List<String> violations = new CopyOnWriteArrayList<>();
        final CompletableFuture<SdkHttpResponse> headers = new CompletableFuture<>();
        final CompletableFuture<Throwable> handlerError = new CompletableFuture<>();
        final AtomicInteger terminals = new AtomicInteger();
        private final AtomicInteger headerSignals = new AtomicInteger();
        private final AtomicInteger streamSignals = new AtomicInteger();
        private final AtomicInteger errorSignals = new AtomicInteger();
        private final AtomicBoolean inSignal = new AtomicBoolean();

        Recorder(boolean autoDemand) {
            this.body = new BodyProbe(autoDemand, this);
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            enter("onHeaders");
            try {
                if (headerSignals.incrementAndGet() > 1 || streamSignals.get() > 0 || errorSignals.get() > 0) {
                    violations.add("onHeaders out of order");
                }
                headers.complete(response);
            } finally {
                inSignal.set(false);
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            enter("onStream");
            try {
                if (headerSignals.get() != 1 || streamSignals.incrementAndGet() > 1 || errorSignals.get() > 0) {
                    violations.add("onStream out of order");
                }
                publisher.subscribe(body);
            } finally {
                inSignal.set(false);
            }
        }

        @Override
        public void onError(Throwable error) {
            enter("onError");
            try {
                if (errorSignals.incrementAndGet() > 1 || streamSignals.get() > 0) {
                    violations.add("handler onError after onStream or twice");
                }
                terminal();
                handlerError.complete(error);
            } finally {
                inSignal.set(false);
            }
        }

        void terminal() {
            if (terminals.incrementAndGet() > 1) {
                violations.add("more than one terminal signal");
            }
        }

        private void enter(String signal) {
            if (!inSignal.compareAndSet(false, true)) {
                violations.add("concurrent handler signal " + signal);
            }
        }

        /** The failure delivered by the handler or the body subscriber, waiting up to {@code millis}. */
        Throwable failure(long millis) throws Exception {
            CompletableFuture<Object> any = CompletableFuture.anyOf(handlerError, body.error);
            return (Throwable) any.get(millis, TimeUnit.MILLISECONDS);
        }
    }

    /** A response-body subscriber that records chunks, demand violations and terminal signals. */
    static final class BodyProbe implements Subscriber<ByteBuffer> {
        final CompletableFuture<Void> complete = new CompletableFuture<>();
        final CompletableFuture<Throwable> error = new CompletableFuture<>();
        final CountDownLatch subscribed = new CountDownLatch(1);
        final AtomicInteger chunks = new AtomicInteger();
        final AtomicLong received = new AtomicLong();
        private final boolean autoDemand;
        private final Recorder recorder;
        private final AtomicLong outstanding = new AtomicLong();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private volatile Subscription subscription;

        BodyProbe(boolean autoDemand, Recorder recorder) {
            this.autoDemand = autoDemand;
            this.recorder = recorder;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription != null) {
                recorder.violations.add("second onSubscribe");
                s.cancel();
                return;
            }
            subscription = s;
            subscribed.countDown();
            if (autoDemand) {
                request(1);
            }
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            if (outstanding.getAndDecrement() <= 0) {
                recorder.violations.add("onNext without demand");
            }
            if (complete.isDone() || error.isDone()) {
                recorder.violations.add("onNext after a terminal signal");
            }
            int n = chunk.remaining();
            byte[] copy = new byte[n];
            chunk.get(copy);
            synchronized (bytes) {
                bytes.write(copy, 0, n);
            }
            received.addAndGet(n);
            chunks.incrementAndGet();
            if (autoDemand) {
                request(1);
            }
        }

        @Override
        public void onError(Throwable t) {
            recorder.terminal();
            error.complete(t);
        }

        @Override
        public void onComplete() {
            recorder.terminal();
            complete.complete(null);
        }

        void request(long n) {
            outstanding.addAndGet(n);
            subscription.request(n);
        }

        void cancel() {
            subscription.cancel();
        }

        byte[] bytes() {
            synchronized (bytes) {
                return bytes.toByteArray();
            }
        }
    }

    /**
     * A one-shot request body of {@code length} bytes that emits {@code chunkSize} slices strictly on demand and
     * records whether its subscription was cancelled.
     */
    static final class TrackingBody implements StreamingBody {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicInteger subscriptions = new AtomicInteger();
        private final long length;
        private final boolean declareLength;
        private final ByteBuffer chunk;

        TrackingBody(long length, int chunkSize, boolean declareLength) {
            this.length = length;
            this.declareLength = declareLength;
            this.chunk = ByteBuffer.wrap(TckSupport.bytes(chunkSize)).asReadOnlyBuffer();
        }

        @Override
        public OptionalLong contentLength() {
            return declareLength ? OptionalLong.of(length) : OptionalLong.empty();
        }

        @Override
        public Optional<String> contentType() {
            return Optional.of("application/x-tck");
        }

        @Override
        public boolean isReplayable() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return subscriptions.get() == 0;
        }

        @Override
        public int maxChunkSize() {
            return chunk.capacity();
        }

        @Override
        public InputStream openStream() {
            throw new UnsupportedOperationException("the async TCK body is consumed through its publisher only");
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            if (subscriptions.incrementAndGet() > 1) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                    }
                });
                subscriber.onError(new IllegalStateException("one-shot TCK body subscribed twice"));
                return;
            }
            subscriber.onSubscribe(new Emitter(subscriber));
        }

        /** Trampolined emission: chunks are sent only against outstanding demand, never re-entrantly. */
        private final class Emitter implements Subscription {
            private final Subscriber<? super ByteBuffer> subscriber;
            private final AtomicLong demand = new AtomicLong();
            private final AtomicInteger wip = new AtomicInteger();
            private long remaining = length;
            private boolean done;

            Emitter(Subscriber<? super ByteBuffer> subscriber) {
                this.subscriber = subscriber;
            }

            @Override
            public void request(long n) {
                if (n <= 0) {
                    cancelled.set(true);
                    subscriber.onError(new IllegalArgumentException("non-positive request " + n));
                    return;
                }
                demand.accumulateAndGet(n, (a, b) -> a + b < 0 ? Long.MAX_VALUE : a + b);
                drain();
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }

            private void drain() {
                if (wip.getAndIncrement() != 0) {
                    return;
                }
                do {
                    while (!done && !cancelled.get() && demand.get() > 0 && remaining > 0) {
                        int n = (int) Math.min(chunk.capacity(), remaining);
                        remaining -= n;
                        demand.decrementAndGet();
                        ByteBuffer slice = chunk.duplicate();
                        slice.limit(n);
                        subscriber.onNext(slice.slice().asReadOnlyBuffer());
                    }
                    if (!done && !cancelled.get() && remaining == 0) {
                        done = true;
                        subscriber.onComplete();
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
