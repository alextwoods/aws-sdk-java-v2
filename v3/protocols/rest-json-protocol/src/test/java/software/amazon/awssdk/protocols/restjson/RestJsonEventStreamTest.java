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

package software.amazon.awssdk.protocols.restjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.eventstream.EventStreamFormatException;
import software.amazon.awssdk.eventstream.FrameTransformer;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamRequestBody;
import software.amazon.awssdk.protocols.core.eventstream.EventStreams;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamReader;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Event stream request bodies, the sync reader and the async publisher over the official restJson1
 * {@code aws.protocoltests.restjson#EventStream} union: demand, bounded buffering, terminal signals, errors and
 * cancellation.
 */
class RestJsonEventStreamTest {
    private static final ShapeId UNION = ShapeId.from("aws.protocoltests.restjson#EventStream");
    private static DynamicService dynamic;
    private static EventStreamCodec codec;
    private static Shape union;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(RestJsonEventStreamTest.class.getClassLoader())
                           .discoverModels(RestJsonEventStreamTest.class.getClassLoader())
                           .assemble()
                           .unwrap();
        dynamic = new DynamicService(model, ShapeId.from("aws.protocoltests.restjson#RestJson"));
        union = model.expectShape(UNION);
        codec = EventStreamCodec.of(new WireContext(ProtocolIds.REST_JSON_1, ShapeId.from("test#Op")),
                                    dynamic.schema(union),
                                    EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
    }

    private static SerializableStruct event(String json) {
        return dynamic.value(union, Node.parse(json));
    }

    private static SerializableStruct text(int i) {
        return event("{\"stringPayload\":{\"payload\":\"event " + i + "\"}}");
    }

    private static byte[] frames(SerializableStruct... events) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (SerializableStruct e : events) {
            out.writeBytes(bytes(MessageEncoder.encode(codec.encodeEvent(e))));
        }
        return out.toByteArray();
    }

    private static byte[] errorFrame() {
        return bytes(MessageEncoder.encode(codec.encodeEvent(event("{\"error\":{\"message\":\"boom\"}}"))));
    }

    @Test
    void requestBodyEncodesWrittenEventsAndIsOneShot() throws Exception {
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        AtomicInteger transformed = new AtomicInteger();
        EventStreamRequestBody body = EventStreams.requestBody(writer, codec, null).withFrameTransformer(
            new FrameTransformer() {
                @Override
                public Message transform(Message message) {
                    transformed.incrementAndGet();
                    return message;
                }

                @Override
                public Message closingFrame() {
                    return new Message(Map.of("closing", HeaderValue.ofBoolean(true)), new byte[0]);
                }
            });
        assertFalse(body.isReplayable());
        assertEquals(OptionalLong.empty(), body.contentLength());
        assertEquals(Optional.of(EventStreams.CONTENT_TYPE), body.contentType());
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                writer.asWriter().write(text(i));
            }
            writer.close();
        });
        producer.start();
        List<Message> sent;
        try (InputStream in = body.openStream()) {
            sent = new MessageDecoder().decode(ByteBuffer.wrap(in.readAllBytes()));
        }
        producer.join();
        assertEquals(4, sent.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(codec.encodeEvent(text(i)), sent.get(i));
        }
        assertTrue(sent.get(3).header("closing").asBoolean(), "closing frame last");
        assertEquals(3, transformed.get());
        assertThrows(IllegalStateException.class, body::openStream);
        assertThrows(IllegalStateException.class, () -> body.withFrameTransformer(FrameTransformer.identity()));
    }

    @Test
    void syncReaderSignalsAModeledExceptionOnceAndReleasesTheBody() {
        byte[] bytes = concat(frames(text(0), text(1)), errorFrame(), frames(text(2)));
        TrackingStream source = new TrackingStream(bytes);
        EventStreamReader<? extends SerializableStruct> reader = EventStreams.reader(
            response(StreamingBodies.fromInputStream(source, -1, EventStreams.CONTENT_TYPE)), codec,
            EventStreams.DEFAULT_MAX_MESSAGE_SIZE).asReader();
        assertEquals(DynamicService.canonical(text(0)), DynamicService.canonical(reader.read()));
        assertEquals(DynamicService.canonical(text(1)), DynamicService.canonical(reader.read()));
        AwsServiceException e = assertThrows(AwsServiceException.class, reader::read);
        assertEquals("error", e.awsErrorDetails().errorCode());
        assertEquals("boom", e.awsErrorDetails().errorMessage());
        assertNull(reader.read(), "exactly one terminal signal");
        assertTrue(source.closed, "the body is released");
    }

    @Test
    void syncReaderReportsTruncatedStreams() {
        byte[] frame = frames(text(0));
        byte[] truncated = java.util.Arrays.copyOf(frame, frame.length - 1);
        EventStreamReader<? extends SerializableStruct> reader = EventStreams.reader(
            response(StreamingBodies.fromBytes(truncated, null)), codec, 1024).asReader();
        RuntimeException e = assertThrows(RuntimeException.class, reader::read);
        assertInstanceOf(EventStreamFormatException.class, e.getCause().getCause());
    }

    @Test
    void publisherDeliversOnlyAgainstDemandWithoutReadAhead() {
        byte[] first = frames(text(0));
        byte[] bytes = concat(first, frames(text(1), text(2)));
        CountingBody body = new CountingBody(StreamingBodies.fromInputStream(new ByteArrayInputStream(bytes), -1,
                                                                              null, 1));
        Recorder recorder = new Recorder();
        EventStreams.publisher(body, codec, null, 1024).subscribe(recorder);
        assertEquals(0, body.chunks.get(), "nothing is read without demand");
        recorder.subscription.request(1);
        assertEquals(1, recorder.events.size());
        assertEquals(first.length, body.chunks.get(), "one-byte chunks: exactly the first message was read");
        recorder.subscription.request(5);
        assertEquals(3, recorder.events.size());
        assertEquals(1, recorder.completions.get());
        assertNull(recorder.error);
        for (int i = 0; i < 3; i++) {
            assertEquals(DynamicService.canonical(text(i)), DynamicService.canonical(recorder.events.get(i)));
        }
    }

    @Test
    void publisherFailsOnceOnExceptionEventsAndMalformedFramesAndCancelsTheBody() {
        CountingBody modeled = new CountingBody(StreamingBodies.fromBytes(concat(errorFrame(), frames(text(0))),
                                                                          null));
        Recorder recorder = new Recorder();
        EventStreams.publisher(modeled, codec, null, 1024).subscribe(recorder);
        recorder.subscription.request(10);
        assertInstanceOf(AwsServiceException.class, recorder.error);
        assertTrue(recorder.events.isEmpty());
        assertEquals(0, recorder.completions.get());
        assertTrue(modeled.cancelled.get() || modeled.completed.get());

        byte[] corrupt = frames(text(0));
        corrupt[corrupt.length - 1] ^= 1;
        Recorder malformed = new Recorder();
        EventStreams.publisher(StreamingBodies.fromBytes(corrupt, null), codec, null, 1024).subscribe(malformed);
        malformed.subscription.request(1);
        EventStreamFormatException e = assertInstanceOf(EventStreamFormatException.class, malformed.error);
        assertEquals(EventStreamFormatException.Reason.MESSAGE_CRC_MISMATCH, e.reason());
        assertEquals(1, malformed.terminals());

        Recorder generic = new Recorder();
        EventStreams.publisher(StreamingBodies.fromBytes(bytes(MessageEncoder.encode(
            codec.encodeError("internal-error", "unknown"))), null), codec, null, 1024).subscribe(generic);
        generic.subscription.request(1);
        AwsServiceException error = assertInstanceOf(AwsServiceException.class, generic.error);
        assertEquals("internal-error", error.awsErrorDetails().errorCode());
        assertEquals(1, generic.terminals());
    }

    @Test
    void cancellationMidStreamStopsDeliveryAndCancelsTheBody() {
        CountingBody body = new CountingBody(StreamingBodies.fromInputStream(
            new ByteArrayInputStream(frames(text(0), text(1), text(2))), -1, null, 4));
        Recorder recorder = new Recorder();
        EventStreams.publisher(body, codec, null, 1024).subscribe(recorder);
        recorder.subscription.request(1);
        recorder.subscription.cancel();
        recorder.subscription.request(5);
        assertEquals(1, recorder.events.size());
        assertTrue(body.cancelled.get());
        assertEquals(0, recorder.terminals());
        Recorder invalid = new Recorder();
        EventStreams.publisher(StreamingBodies.fromBytes(frames(text(0)), null), codec, null, 1024)
                    .subscribe(invalid);
        invalid.subscription.request(0);
        assertInstanceOf(IllegalArgumentException.class, invalid.error);
    }

    private static SdkHttpResponse response(StreamingBody body) {
        return SdkHttpResponse.builder().statusCode(200).body(HttpBody.streaming(body)).build();
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }

    private static final class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        TrackingStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /** Counts delivered chunks and records cancellation of a body. */
    private static final class CountingBody implements StreamingBody {
        private final StreamingBody delegate;
        private final AtomicInteger chunks = new AtomicInteger();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean completed = new AtomicBoolean();

        CountingBody(StreamingBody delegate) {
            this.delegate = delegate;
        }

        @Override
        public OptionalLong contentLength() {
            return delegate.contentLength();
        }

        @Override
        public Optional<String> contentType() {
            return delegate.contentType();
        }

        @Override
        public boolean isReplayable() {
            return delegate.isReplayable();
        }

        @Override
        public boolean isAvailable() {
            return delegate.isAvailable();
        }

        @Override
        public int maxChunkSize() {
            return delegate.maxChunkSize();
        }

        @Override
        public InputStream openStream() {
            return delegate.openStream();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                            subscription.request(n);
                        }

                        @Override
                        public void cancel() {
                            cancelled.set(true);
                            subscription.cancel();
                        }
                    });
                }

                @Override
                public void onNext(ByteBuffer item) {
                    chunks.addAndGet(item.remaining());
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                    subscriber.onComplete();
                }
            });
        }
    }

    private static final class Recorder implements Subscriber<SerializableStruct> {
        private final List<SerializableStruct> events = new ArrayList<>();
        private final AtomicInteger completions = new AtomicInteger();
        private final AtomicInteger errors = new AtomicInteger();
        private Subscription subscription;
        private Throwable error;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(SerializableStruct item) {
            events.add(item);
        }

        @Override
        public void onError(Throwable throwable) {
            errors.incrementAndGet();
            error = throwable;
        }

        @Override
        public void onComplete() {
            completions.incrementAndGet();
        }

        int terminals() {
            return errors.get() + completions.get();
        }
    }
}
