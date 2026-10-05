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

package software.amazon.awssdk.protocols.core.eventstream;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The demand-driven async view of an output event stream: subscribes to the response body and publishes its typed
 * events (union values).
 *
 * <p>Backpressure and bounds: an event is published only against outstanding downstream demand; the body is asked for
 * one chunk at a time, and only when demand is outstanding and the current chunk holds no further complete message,
 * so at most one body chunk and one partial message ({@link MessageDecoder#maxMessageSize()}) are buffered, and no
 * decoded event is queued. Terminal signals: exactly one of {@code onComplete} (the body completed on a message
 * boundary) or {@code onError} (a body error, a malformed or truncated frame, a modeled exception event mapped to its
 * modeled error, an {@code error} event mapped to a generic error); the body subscription is cancelled on every
 * failure raised here and on downstream cancellation. A non-positive request fails with
 * {@link IllegalArgumentException} (Reactive Streams rule 3.9). One subscriber only.
 *
 * <p>With an initial-message consumer (an RPC output, see {@link OutputEventStream}), the first message is read as
 * soon as the publisher is subscribed, without downstream demand (at most one chunk and one partial message, as for
 * any message), and handed to the consumer instead of being published; a failure of the consumer fails the
 * publisher, and a body that ends before the first message fails it with an {@code SdkProtocolException}.
 */
public final class EventStreamPublisher implements Publisher<SerializableStruct> {
    private final Publisher<ByteBuffer> body;
    private final EventStreamCodec codec;
    private final SdkHttpResponse response;
    private final int maxMessageSize;
    private final Consumer<Message> initialMessage;
    private final AtomicBoolean subscribed = new AtomicBoolean();

    EventStreamPublisher(Publisher<ByteBuffer> body, EventStreamCodec codec, SdkHttpResponse response,
                         int maxMessageSize) {
        this(body, codec, response, maxMessageSize, null);
    }

    EventStreamPublisher(Publisher<ByteBuffer> body, EventStreamCodec codec, SdkHttpResponse response,
                         int maxMessageSize, Consumer<Message> initialMessage) {
        this.body = Objects.requireNonNull(body, "body");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.response = response;
        this.maxMessageSize = maxMessageSize;
        this.initialMessage = initialMessage;
    }

    @Override
    public void subscribe(Subscriber<? super SerializableStruct> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (!subscribed.compareAndSet(false, true)) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onError(new IllegalStateException("An event stream publisher has a single subscriber"));
            return;
        }
        Bridge bridge = new Bridge(subscriber);
        subscriber.onSubscribe(bridge);
        body.subscribe(bridge);
    }

    /** Body subscriber and downstream subscription, serialized through a work-in-progress counter. */
    private final class Bridge implements Subscriber<ByteBuffer>, Subscription {
        private final Subscriber<? super SerializableStruct> downstream;
        private final MessageDecoder decoder = new MessageDecoder(maxMessageSize);
        private final AtomicInteger wip = new AtomicInteger();
        private volatile Subscription upstream;
        private volatile long demand;
        private volatile ByteBuffer chunk;
        private volatile boolean chunkRequested;
        private volatile boolean upstreamDone;
        private volatile Throwable upstreamError;
        private volatile boolean cancelled;
        private volatile Throwable invalidRequest;
        /** Set once the body subscription must be cancelled, even if it has not arrived yet. */
        private volatile boolean cancelUpstream;
        /** Whether the initial message is still to be read, without downstream demand. */
        private boolean initialPending = initialMessage != null;
        private boolean terminated;

        Bridge(Subscriber<? super SerializableStruct> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                invalidRequest = new IllegalArgumentException("Non-positive request " + n + " (rule 3.9)");
            } else {
                synchronized (this) {
                    long next = demand + n;
                    demand = next < 0 ? Long.MAX_VALUE : next;
                }
            }
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            drain();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            upstream = subscription;
            if (cancelUpstream) {
                // Cancelled or failed before the body subscribed (for example, cancel inside onSubscribe).
                subscription.cancel();
                return;
            }
            drain();
        }

        @Override
        public void onNext(ByteBuffer item) {
            chunk = item;
            chunkRequested = false;
            drain();
        }

        @Override
        public void onError(Throwable throwable) {
            upstreamError = throwable;
            upstreamDone = true;
            drain();
        }

        @Override
        public void onComplete() {
            upstreamDone = true;
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
                    terminated = true;
                    cancelUpstream();
                    return;
                }
                if (invalidRequest != null) {
                    fail(invalidRequest);
                    return;
                }
                if (upstreamError != null) {
                    terminated = true;
                    downstream.onError(upstreamError);
                    return;
                }
                ByteBuffer current = chunk;
                boolean wanted = initialPending || demand > 0;
                if (wanted && current != null && current.hasRemaining()) {
                    SerializableStruct event;
                    try {
                        Message message = decoder.decodeNext(current);
                        if (message == null) {
                            continue;
                        }
                        if (initialPending) {
                            initialPending = false;
                            initialMessage.accept(message);
                            continue;
                        }
                        event = codec.decodeEvent(message, response);
                    } catch (RuntimeException e) {
                        fail(e);
                        return;
                    }
                    synchronized (this) {
                        demand--;
                    }
                    downstream.onNext(event);
                    continue;
                }
                if (current != null && !current.hasRemaining()) {
                    chunk = null;
                    continue;
                }
                if (upstreamDone && current == null) {
                    terminated = true;
                    try {
                        decoder.endOfStream();
                        if (initialPending) {
                            throw codec.missingInitialResponse();
                        }
                    } catch (RuntimeException e) {
                        downstream.onError(e);
                        return;
                    }
                    downstream.onComplete();
                    return;
                }
                Subscription up = upstream;
                if (wanted && current == null && !chunkRequested && up != null && !upstreamDone) {
                    chunkRequested = true;
                    up.request(1);
                    continue;
                }
                return;
            }
        }

        private void fail(Throwable error) {
            terminated = true;
            cancelUpstream();
            downstream.onError(error);
        }

        private void cancelUpstream() {
            // Volatile write then read here, volatile write then read in onSubscribe: at least one side sees the
            // other, so a body that subscribes concurrently is cancelled (possibly twice, which rule 3.7 allows).
            cancelUpstream = true;
            Subscription up = upstream;
            if (up != null) {
                up.cancel();
            }
        }
    }
}
