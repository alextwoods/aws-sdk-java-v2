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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * {@link PublisherEventStream}: a publisher's events become the request body's event frames under one-at-a-time
 * demand, subscribed only when the body is consumed; the publisher's error fails the body, and cancelling the body
 * (closing its stream, or {@link EventStreamRequestBody#cancel()}) cancels the subscription at once.
 */
class PublisherEventStreamTest {
    static final EventStreamCodec CODEC = codec();

    @Test
    void thePublishersEventsAreTheBodysFramesInOrder() throws Exception {
        List<SerializableStruct> events = List.of(event("a"), event("bb"), event("ccc"));
        ListPublisher publisher = new ListPublisher(events, null);
        EventStream<SerializableStruct> stream = PublisherEventStream.of(publisher);
        EventStreamRequestBody body = EventStreams.requestBody(stream, CODEC, null);
        byte[] bytes;
        try (InputStream in = body.openStream()) {
            bytes = in.readAllBytes();
        }
        assertTrue(publisher.subscribed.get());
        assertEquals(1, publisher.maxOutstanding.get(), "events are requested one at a time");
        List<Message> frames = new MessageDecoder().decode(ByteBuffer.wrap(bytes));
        List<Message> expected = new ArrayList<>();
        events.forEach(event -> expected.add(CODEC.encodeEvent(event)));
        assertEquals(expected, frames.subList(0, Math.min(frames.size(), expected.size())));
        assertTrue(frames.size() <= expected.size() + 1, "at most the closing frame follows the events");
    }

    @Test
    void thePublishersErrorFailsTheBody() throws Exception {
        IllegalStateException failure = new IllegalStateException("boom");
        EventStream<SerializableStruct> stream = PublisherEventStream.of(new ListPublisher(List.of(event("a")),
                                                                                         failure));
        EventStreamRequestBody body = EventStreams.requestBody(stream, CODEC, null);
        try (InputStream in = body.openStream()) {
            assertThrows(IOException.class, in::readAllBytes);
        }
    }

    @Test
    void closingTheBodyCancelsTheSubscription() throws Exception {
        List<SerializableStruct> events = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            events.add(event("e" + i));
        }
        ListPublisher publisher = new ListPublisher(events, null);
        publisher.hold = true;
        EventStream<SerializableStruct> stream = PublisherEventStream.of(publisher);
        EventStreamRequestBody body = EventStreams.requestBody(stream, CODEC, null);
        InputStream in = body.openStream();
        assertTrue(publisher.requested.await(5, TimeUnit.SECONDS));
        in.close();
        assertTrue(publisher.cancelled.await(5, TimeUnit.SECONDS));
    }

    @Test
    void thePublisherIsSubscribedOnlyWhenTheBodyIsConsumed() throws Exception {
        ListPublisher publisher = new ListPublisher(List.of(event("a")), null);
        EventStreamRequestBody body = EventStreams.requestBody(PublisherEventStream.of(publisher), CODEC, null);
        EventStreamRequestBody signed = body.withFrameTransformer(software.amazon.awssdk.eventstream.FrameTransformer.identity());
        assertFalse(publisher.subscribed.get(), "not subscribed by of() or by building the body");
        try (InputStream in = signed.openStream()) {
            assertTrue(publisher.subscribed.get());
            in.readAllBytes();
        }
    }

    @Test
    void cancellingABodyBeforeItIsConsumedNeverSubscribesThePublisher() {
        ListPublisher publisher = new ListPublisher(List.of(event("a")), null);
        EventStreamRequestBody body = EventStreams.requestBody(PublisherEventStream.of(publisher), CODEC, null);
        body.cancel();
        assertThrows(IllegalStateException.class, body::openStream);
        assertFalse(publisher.subscribed.get());
    }

    @Test
    void cancellingTheBodyCancelsASilentPublisherAtOnce() throws Exception {
        ListPublisher publisher = new ListPublisher(List.of(), null);
        publisher.hold = true;
        EventStreamRequestBody body = EventStreams.requestBody(PublisherEventStream.of(publisher), CODEC, null);
        InputStream in = body.openStream();
        assertTrue(publisher.requested.await(5, TimeUnit.SECONDS));
        long start = System.nanoTime();
        body.cancel();
        assertTrue(publisher.cancelled.await(1, TimeUnit.SECONDS), "the cancel reaches the silent publisher");
        assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1));
        assertThrows(IOException.class, in::read);
    }

    /** Emits {@code events} under demand, then completes (or fails with {@code error}); {@code hold} never ends. */
    private static final class ListPublisher implements Publisher<SerializableStruct> {
        private final List<SerializableStruct> events;
        private final Throwable error;
        private final AtomicBoolean subscribed = new AtomicBoolean();
        private final AtomicLong maxOutstanding = new AtomicLong();
        private final CountDownLatch requested = new CountDownLatch(1);
        private final CountDownLatch cancelled = new CountDownLatch(1);
        private volatile boolean hold;

        ListPublisher(List<SerializableStruct> events, Throwable error) {
            this.events = events;
            this.error = error;
        }

        @Override
        public void subscribe(Subscriber<? super SerializableStruct> subscriber) {
            subscribed.set(true);
            AtomicLong outstanding = new AtomicLong();
            subscriber.onSubscribe(new Subscription() {
                private int next;
                private boolean done;

                @Override
                public synchronized void request(long n) {
                    requested.countDown();
                    maxOutstanding.accumulateAndGet(outstanding.addAndGet(n), Math::max);
                    while (!done && outstanding.get() > 0 && next < events.size()) {
                        outstanding.decrementAndGet();
                        subscriber.onNext(events.get(next++));
                    }
                    if (!done && !hold && next == events.size()) {
                        done = true;
                        if (error == null) {
                            subscriber.onComplete();
                        } else {
                            subscriber.onError(error);
                        }
                    }
                }

                @Override
                public void cancel() {
                    cancelled.countDown();
                }
            });
        }
    }

    static SerializableStruct event(String payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(EventStreamCodec.MESSAGE_TYPE, HeaderValue.ofString("event"));
        headers.put(EventStreamCodec.EVENT_TYPE, HeaderValue.ofString("data"));
        headers.put(EventStreamCodec.CONTENT_TYPE, HeaderValue.ofString("application/octet-stream"));
        return CODEC.decodeEvent(new Message(headers, payload.getBytes(StandardCharsets.UTF_8)), null);
    }

    private static EventStreamCodec codec() {
        Model model = Model.assembler().addUnparsedModel("events.smithy", String.join("\n",
            "$version: \"2\"",
            "namespace test.flow",
            "@streaming",
            "union Events { data: DataEvent }",
            "structure DataEvent {",
            "    @eventPayload",
            "    payload: Blob",
            "}")).assemble().unwrap();
        return EventStreamCodec.of(new WireContext(ShapeId.from("aws.protocols#restJson1"),
                                                   ShapeId.from("test.flow#PublishEvents")),
                                   new SchemaConverter(model).getSchema(
                                       model.expectShape(ShapeId.from("test.flow#Events"))),
                                   EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
    }
}
