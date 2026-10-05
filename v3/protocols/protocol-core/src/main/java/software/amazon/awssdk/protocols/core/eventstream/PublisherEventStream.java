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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamWriter;

/**
 * Bridges a Reactive Streams {@link Publisher} of input events to the Smithy Java {@code EventStream.newWriter()} the
 * protocol reads as an input's event stream member ({@link EventStreams#eventStream}): the value a generated client
 * sets on the input through {@code ShapeBuilder.setMemberValue}, exactly as a dynamic-client caller sets a writer.
 *
 * <p>Lazy, as in V2: {@link #of} only records the publisher against the writer it returns (Smithy Java's writer types
 * are sealed, so the association is an identity-keyed weak registry, never a subclass). The publisher is subscribed
 * when the transport first consumes the request body ({@link EventStreamRequestBody#subscribe} or
 * {@link EventStreamRequestBody#openStream()}), after the writer is activated. A call that ends before then (an
 * identity, endpoint or serialization failure, or a cancellation) never subscribes it, and a writer that is never
 * consumed is garbage collected with its registration.
 *
 * <p>Once subscribed, one event is requested at a time and written to the writer on a dedicated virtual thread, so a
 * write that waits for the transport (the writer's bounded queue) never blocks the publisher's thread: the
 * transport's consumption of the frames is the publisher's demand. The publisher's completion closes the writer (the
 * end of the stream) and its error fails it. Cancelling the body (the transport cancels its subscription or closes
 * the stream early, or the call's attempt is cancelled, {@link EventStreamRequestBody#cancel()}) cancels the
 * subscription at once, from the cancelling thread, even while the pump waits for a silent publisher (Reactive
 * Streams 3.5, 3.7); a failing write (a transform failure) cancels it too. Calls to the subscription are serialized
 * (rule 2.7).
 */
public final class PublisherEventStream {
    private static final String PUMP_THREAD = "aws-sdk-event-stream-publisher";
    private static final Object COMPLETE = new Object();
    private static final Object CANCELLED = new Object();
    /** Writers created by {@link #of} whose publisher was not yet subscribed; identity keys (writers keep equals). */
    private static final Map<EventStream<?>, Publisher<?>> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    private PublisherEventStream() {
    }

    /**
     * A Smithy Java event stream writer fed by the events {@code publisher} emits, subscribed when the request body
     * is first consumed.
     *
     * @param publisher the caller's events; subscribed at most once, when the request body is opened
     * @param <T> the event (union) type
     * @return the writer the protocol encodes as the request body
     */
    public static <T extends SerializableStruct> EventStream<T> of(Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher, "publisher");
        EventStream<T> stream = EventStream.newWriter();
        // The value must not reference the key, or the weak entry would never be collected.
        PENDING.put(stream, publisher);
        return stream;
    }

    /**
     * Subscribes the publisher registered for {@code events}, if any, now that its writer is active.
     *
     * @return the pump to cancel with the body, or {@code null} if {@code events} is not a publisher's writer
     */
    static Pump<?> start(EventStream<? extends SerializableStruct> events) {
        Publisher<?> publisher = PENDING.remove(events);
        if (publisher == null) {
            return null;
        }
        return subscribe(publisher, events);
    }

    /** Forgets the publisher registered for {@code events} without subscribing it: the body was never sent. */
    static void discard(EventStream<? extends SerializableStruct> events) {
        PENDING.remove(events);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Pump<?> subscribe(Publisher publisher, EventStream events) {
        Pump pump = new Pump<>(events.asWriter());
        publisher.subscribe(pump);
        return pump;
    }

    /** Writes the publisher's events, one requested at a time, on a virtual thread. Thread safe. */
    static final class Pump<T extends SerializableStruct> implements Subscriber<T> {
        private final EventStreamWriter<T> writer;
        /** At most one event (demand is one), one terminal signal and the cancellation marker. */
        private final LinkedBlockingQueue<Object> signals = new LinkedBlockingQueue<>();
        /** Guards {@link #subscription} and serializes request and cancel (rule 2.7). */
        private final Object lock = new Object();
        private Subscription subscription;
        private boolean cancelled;

        Pump(EventStreamWriter<T> writer) {
            this.writer = writer;
        }

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s, "subscription");
            synchronized (lock) {
                if (subscription != null || cancelled) {
                    // Rule 2.5 (a second subscription), or the body was cancelled before the publisher subscribed.
                    s.cancel();
                    return;
                }
                subscription = s;
            }
            Thread.ofVirtual().name(PUMP_THREAD).start(this::run);
        }

        @Override
        public void onNext(T event) {
            signals.offer(Objects.requireNonNull(event, "event"));
        }

        @Override
        public void onError(Throwable t) {
            signals.offer(Objects.requireNonNull(t, "throwable"));
        }

        @Override
        public void onComplete() {
            signals.offer(COMPLETE);
        }

        /** Cancels the subscription at once (idempotent, never blocks on the publisher's silence) and stops the pump. */
        void cancel() {
            synchronized (lock) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                if (subscription != null) {
                    subscription.cancel();
                }
            }
            signals.offer(CANCELLED);
        }

        /** Requests the next event, unless cancelled. */
        private boolean requestNext() {
            synchronized (lock) {
                if (cancelled) {
                    return false;
                }
                subscription.request(1);
                return true;
            }
        }

        @SuppressWarnings("unchecked")
        private void run() {
            try {
                while (requestNext()) {
                    Object signal = signals.take();
                    if (signal == CANCELLED) {
                        return;
                    }
                    if (signal == COMPLETE) {
                        writer.close();
                        return;
                    }
                    if (signal instanceof Throwable failure) {
                        writer.closeWithError(failure instanceof Exception e ? e : new RuntimeException(failure));
                        return;
                    }
                    writer.write((T) signal);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancel();
                writer.closeWithError(e);
            } catch (RuntimeException e) {
                // The writer was cancelled or failed by the protocol: stop the publisher.
                cancel();
            }
        }
    }
}
