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

package software.amazon.awssdk.pipeline;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The output event stream of one async call between its decoder (a protocol-core {@code EventStreamPublisher} over
 * the live response body) and the caller's event subscriber (Requirements 5.11, 8.5, 8.9, 14.12).
 *
 * <p>The driver subscribes the relay to the decoder as soon as the successful response arrives. The relay passes the
 * caller's demand and events straight through, so the decoder reads the body only under the caller's demand (at most
 * one body chunk and one partial message buffered, no decoded event queued); for an RPC output the decoder first
 * reads the {@code initial-response} message without demand, which the relay records for the driver.
 *
 * <p>The decoder's terminal signal is not forwarded: the relay records it and wakes the driver, which ends the call
 * through its one terminal path and then calls {@link #complete()} or {@link #fail(Throwable)}, so the subscriber's
 * one terminal signal carries the call's mapped outcome. The subscriber cancelling its subscription cancels the call
 * ({@link Listener#cancelled()}); cancelling the call {@linkplain #abandon() abandons} the decoder, which cancels its
 * body subscription. One subscriber only.
 */
final class EventStreamRelay implements Subscriber<SerializableStruct>, SdkPublisher<Object> {
    /** The driver's view of the relay; called on the decoder's or the subscriber's thread. */
    interface Listener {
        /** The decoder recorded the initial message or its terminal signal; the driver reads it from the relay. */
        void wake();

        /** The caller cancelled its event subscription. */
        void cancelled();
    }

    private static final Object COMPLETED = new Object();

    private final Listener listener;
    private final AtomicBoolean subscribed = new AtomicBoolean();
    private final AtomicBoolean delivered = new AtomicBoolean();
    private volatile Subscription upstream;
    private volatile boolean abandoned;
    private volatile OutputEventStream.InitialResponse initialMessage;
    /** The decoder's terminal signal: {@link #COMPLETED} or its failure. */
    private volatile Object upstreamTerminal;
    private volatile Subscriber<? super Object> downstream;
    /** Whether the subscriber has returned from {@code onSubscribe}, so a terminal signal may follow. */
    private volatile boolean ready;
    private volatile boolean downstreamCancelled;
    /** The call's terminal signal for the subscriber: {@link #COMPLETED} or the call's failure. */
    private volatile Object terminal;

    EventStreamRelay(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /** Records the RPC {@code initial-response} message; the decoder's initial-message consumer. */
    void initialMessage(OutputEventStream.InitialResponse message) {
        initialMessage = message;
        listener.wake();
    }

    /** The {@code initial-response} message, once the decoder read it; else {@code null}. */
    OutputEventStream.InitialResponse initial() {
        return initialMessage;
    }

    /** True once the decoder ended. */
    boolean upstreamDone() {
        return upstreamTerminal != null;
    }

    /** The decoder's failure, or {@code null} if it completed (or has not ended). */
    Throwable upstreamFailure() {
        Object end = upstreamTerminal;
        return end instanceof Throwable failure ? failure : null;
    }

    /** Cancels the decoder, and so its body subscription; idempotent. No further event is decoded. */
    void abandon() {
        abandoned = true;
        Subscription up = upstream;
        if (up != null) {
            up.cancel();
        }
    }

    /** The call succeeded: the subscriber completes, once, unless it cancelled. */
    void complete() {
        terminal = COMPLETED;
        deliverTerminal();
    }

    /** The call failed or was cancelled: the subscriber fails with {@code failure}, once, unless it cancelled. */
    void fail(Throwable failure) {
        terminal = Objects.requireNonNull(failure, "failure");
        deliverTerminal();
    }

    // Decoder side.

    @Override
    public void onSubscribe(Subscription subscription) {
        upstream = subscription;
        if (abandoned) {
            subscription.cancel();
        }
    }

    @Override
    public void onNext(SerializableStruct event) {
        Subscriber<? super Object> subscriber = downstream;
        // Events flow only against the subscriber's demand, so it is present; a cancelled one receives nothing.
        if (subscriber != null && !downstreamCancelled) {
            subscriber.onNext(event);
        }
    }

    @Override
    public void onError(Throwable error) {
        upstreamTerminal = Objects.requireNonNull(error, "error");
        listener.wake();
    }

    @Override
    public void onComplete() {
        upstreamTerminal = COMPLETED;
        listener.wake();
    }

    // Subscriber side.

    @Override
    public void subscribe(Subscriber<? super Object> subscriber) {
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
        downstream = subscriber;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (downstreamCancelled) {
                    return;
                }
                Subscription up = upstream;
                if (up != null) {
                    // The decoder validates n (rule 3.9) and fails the stream for a non-positive one.
                    up.request(n);
                }
            }

            @Override
            public void cancel() {
                if (downstreamCancelled) {
                    return;
                }
                downstreamCancelled = true;
                abandon();
                listener.cancelled();
            }
        });
        ready = true;
        deliverTerminal();
    }

    private void deliverTerminal() {
        Object end = terminal;
        Subscriber<? super Object> subscriber = downstream;
        if (end == null || subscriber == null || !ready || downstreamCancelled) {
            return;
        }
        if (!delivered.compareAndSet(false, true)) {
            return;
        }
        if (end == COMPLETED) {
            subscriber.onComplete();
        } else {
            subscriber.onError((Throwable) end);
        }
    }
}
