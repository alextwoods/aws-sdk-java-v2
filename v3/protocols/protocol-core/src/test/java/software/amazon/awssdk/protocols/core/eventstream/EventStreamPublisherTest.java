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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Regression tests for {@link EventStreamPublisher} (found by Property 14): a cancellation or failure that happens
 * before the response body subscribes, such as a {@code cancel()} inside {@code onSubscribe}, still cancels the body
 * subscription when it arrives.
 */
class EventStreamPublisherTest {
    private static final EventStreamCodec CODEC = codec();

    @Test
    void cancelInsideOnSubscribeCancelsTheBodySubscription() {
        RecordingBody body = new RecordingBody();
        AtomicInteger signals = new AtomicInteger();
        EventStreams.publisher(body, CODEC, null, 1024).subscribe(new Subscriber<SerializableStruct>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.cancel();
            }

            @Override
            public void onNext(SerializableStruct item) {
                signals.incrementAndGet();
            }

            @Override
            public void onError(Throwable throwable) {
                signals.incrementAndGet();
            }

            @Override
            public void onComplete() {
                signals.incrementAndGet();
            }
        });
        assertEquals(1, body.cancels.get(), "the body subscription is cancelled");
        assertEquals(0, body.requests.get(), "nothing is requested from a cancelled stream");
        assertEquals(0, signals.get(), "no signal after cancel");
    }

    @Test
    void invalidRequestInsideOnSubscribeFailsOnceAndCancelsTheBodySubscription() {
        RecordingBody body = new RecordingBody();
        AtomicInteger errors = new AtomicInteger();
        EventStreams.publisher(body, CODEC, null, 1024).subscribe(new Subscriber<SerializableStruct>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(0);
            }

            @Override
            public void onNext(SerializableStruct item) {
            }

            @Override
            public void onError(Throwable throwable) {
                assertTrue(throwable instanceof IllegalArgumentException, throwable.toString());
                errors.incrementAndGet();
            }

            @Override
            public void onComplete() {
            }
        });
        assertEquals(1, errors.get());
        assertEquals(1, body.cancels.get(), "the body subscription is cancelled");
        assertEquals(0, body.requests.get());
    }

    /** A body whose subscription only records requests and cancellations. */
    private static final class RecordingBody implements StreamingBody {
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicInteger cancels = new AtomicInteger();

        @Override
        public OptionalLong contentLength() {
            return OptionalLong.empty();
        }

        @Override
        public Optional<String> contentType() {
            return Optional.of(EventStreams.CONTENT_TYPE);
        }

        @Override
        public boolean isReplayable() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public int maxChunkSize() {
            return StreamingBodies.DEFAULT_MAX_CHUNK_SIZE;
        }

        @Override
        public InputStream openStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    requests.incrementAndGet();
                }

                @Override
                public void cancel() {
                    cancels.incrementAndGet();
                }
            });
        }
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
                                                   ShapeId.from("test.flow#SubscribeToEvents")),
                                   new SchemaConverter(model).getSchema(
                                       model.expectShape(ShapeId.from("test.flow#Events"))),
                                   EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
    }
}
