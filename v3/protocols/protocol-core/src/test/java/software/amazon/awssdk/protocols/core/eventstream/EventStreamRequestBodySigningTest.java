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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.eventstream.EventStreamSigner;
import software.amazon.awssdk.eventstream.EventStreamSigningException;
import software.amazon.awssdk.eventstream.FrameSignatureAlgorithm;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Event stream signing through {@link EventStreamRequestBody} (task 8.5): identical signature chains for sync and
 * async transports, demand-driven delivery, cancellation that stops signing and releases a blocked producer without
 * blocking the canceller, and signing failures that fail the write and then the transport.
 */
class EventStreamRequestBodySigningTest {
    private static final EventStreamCodec CODEC = codec();
    private static final byte[] SEED = {1, 2, 3, 4};
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-05-01T12:00:00Z"), ZoneOffset.UTC);
    /** The capacity of the Smithy Java writer's frame queue. */
    private static final int QUEUE_CAPACITY = 16;

    @Test
    void syncAndAsyncTransportsSendTheSameVerifiableSignedChain() throws Exception {
        List<SerializableStruct> events = List.of(event("a"), event("bb"), event("ccc"));

        HashChain syncAlgorithm = new HashChain(-1);
        EventStream<SerializableStruct> syncWriter = EventStream.newWriter();
        EventStreamRequestBody syncBody = signed(syncWriter, syncAlgorithm);
        byte[] syncBytes;
        try (InputStream in = syncBody.openStream()) {
            writeAll(syncWriter, events);
            syncBytes = in.readAllBytes();
        }

        HashChain asyncAlgorithm = new HashChain(-1);
        EventStream<SerializableStruct> asyncWriter = EventStream.newWriter();
        EventStreamRequestBody asyncBody = signed(asyncWriter, asyncAlgorithm);
        Collector collector = new Collector();
        asyncBody.subscribe(collector);
        writeAll(asyncWriter, events);
        assertTrue(collector.chunks.isEmpty(), "nothing is delivered without demand");
        for (int i = 1; i <= events.size() + 1; i++) {
            collector.subscription.request(1);
            assertEquals(i, collector.chunks.size(), "exactly one frame per demanded chunk");
        }
        collector.subscription.request(1);
        assertTrue(collector.completed.await(5, TimeUnit.SECONDS));
        assertNull(collector.error.get());

        assertArrayEquals(syncBytes, collector.bytes(), "both transports send identical frames");
        verifyChain(syncBytes, events);
        assertEquals(events.size() + 1, syncAlgorithm.calls.get());
        assertEquals(events.size() + 1, asyncAlgorithm.calls.get());
    }

    @Test
    void sigV4SignedBodyCarriesDateAndSignatureEnvelopes() throws Exception {
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        EventStreamSigner signer = EventStreamSigner.sigV4("0a0b", "us-west-2", "transcribe",
                                                           date -> new byte[32], CLOCK);
        EventStreamRequestBody body = EventStreams.requestBody(writer, CODEC, null).withFrameTransformer(signer);
        byte[] bytes;
        try (InputStream in = body.openStream()) {
            writeAll(writer, List.of(event("x")));
            bytes = in.readAllBytes();
        }
        List<Message> frames = new MessageDecoder().decode(ByteBuffer.wrap(bytes));
        assertEquals(2, frames.size());
        for (Message frame : frames) {
            assertEquals(List.of(EventStreamSigner.DATE_HEADER, EventStreamSigner.CHUNK_SIGNATURE_HEADER),
                         List.copyOf(frame.headers().keySet()));
            assertEquals(CLOCK.instant(), frame.header(EventStreamSigner.DATE_HEADER).asTimestamp());
            assertEquals(32, frame.header(EventStreamSigner.CHUNK_SIGNATURE_HEADER).asByteArray().length);
        }
        assertEquals(CODEC.encodeEvent(event("x")), new MessageDecoder().decodeNext(frames.get(0).payload()));
        assertEquals(0, frames.get(1).payloadLength());
        assertArrayEquals(frames.get(1).header(EventStreamSigner.CHUNK_SIGNATURE_HEADER).asByteArray(),
                          signer.priorSignature());
        assertTrue(signer.isClosed());
    }

    @Test
    void cancellingTheSubscriptionReleasesABlockedProducerAndStopsSigning() throws Exception {
        HashChain algorithm = new HashChain(-1);
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        Collector collector = new Collector();
        signed(writer, algorithm).subscribe(collector);

        AtomicReference<RuntimeException> writeFailure = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 3 * QUEUE_CAPACITY; i++) {
                    writer.asWriter().write(event("e" + i));
                }
            } catch (RuntimeException e) {
                writeFailure.set(e);
            }
        });
        producer.start();
        // With no demand the queue fills; the producer signs one more frame and blocks enqueueing it.
        awaitBlocked(producer);
        assertEquals(QUEUE_CAPACITY + 1, algorithm.calls.get());

        long start = System.nanoTime();
        collector.subscription.cancel();
        assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1), "cancel does not block");

        producer.join(5_000);
        assertFalse(producer.isAlive(), "the blocked producer is released");
        assertInstanceOf(IllegalStateException.class, writeFailure.get(), "later writes fail");
        writer.close();
        assertEquals(QUEUE_CAPACITY + 1, algorithm.calls.get(), "no frame and no closing frame signed after cancel");
        assertTrue(collector.chunks.isEmpty());
        assertNull(collector.error.get(), "no signal after cancel");
        assertEquals(1, collector.completed.getCount(), "no signal after cancel");
    }

    @Test
    void cancellingBeforeTheFirstRequestCancelsTheEventStream() {
        HashChain algorithm = new HashChain(-1);
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        Collector collector = new Collector();
        signed(writer, algorithm).subscribe(collector);
        collector.subscription.cancel();
        assertThrows(IllegalStateException.class, () -> writer.asWriter().write(event("late")));
        writer.close();
        assertEquals(0, algorithm.calls.get());
    }

    @Test
    void closingTheSyncStreamBeforeItsEndCancelsTheEventStream() throws Exception {
        HashChain algorithm = new HashChain(-1);
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        InputStream in = signed(writer, algorithm).openStream();
        writeAll(writer, List.of(event("a"), event("b")), false);
        assertTrue(in.read() >= 0);
        in.close();

        assertThrows(IllegalStateException.class, () -> writer.asWriter().write(event("c")));
        writer.close();
        assertEquals(2, algorithm.calls.get(), "no frame and no closing frame signed after cancel");
        assertThrows(IOException.class, in::read, "a cancelled stream cannot be read");
    }

    @Test
    void aSigningFailureFailsTheWriteThenTheTransportAfterTheQueuedFrames() throws Exception {
        HashChain algorithm = new HashChain(3);
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        InputStream in = signed(writer, algorithm).openStream();
        writer.asWriter().write(event("a"));
        writer.asWriter().write(event("b"));
        EventStreamSigningException failure = assertThrows(EventStreamSigningException.class,
                                                           () -> writer.asWriter().write(event("c")));
        assertThrows(IllegalStateException.class, () -> writer.asWriter().write(event("d")));

        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        IOException transportFailure = assertThrows(IOException.class, () -> copy(in, sent));
        assertTrue(causes(transportFailure, failure), "the transport fails with the signing failure");
        assertEquals(2, new MessageDecoder().decode(ByteBuffer.wrap(sent.toByteArray())).size(),
                     "the frames signed before the failure were sent");
        in.close();
    }

    @Test
    void aClosingFrameSigningFailureFailsTheTransportInsteadOfEndingNormally() throws Exception {
        HashChain algorithm = new HashChain(3);
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        InputStream in = signed(writer, algorithm).openStream();
        writeAll(writer, List.of(event("a"), event("b")));

        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        IOException transportFailure = assertThrows(IOException.class, () -> copy(in, sent));
        assertTrue(causes(transportFailure, EventStreamSigningException.class), transportFailure.toString());
        assertEquals(2, new MessageDecoder().decode(ByteBuffer.wrap(sent.toByteArray())).size());
        in.close();
    }

    @Test
    void anInitialEventSigningFailureRejectsBothViews() {
        EventStreamRequestBody sync = EventStreams.requestBody(EventStream.newWriter(), CODEC, event("initial"))
                                                  .withFrameTransformer(signer(new HashChain(1)));
        assertThrows(EventStreamSigningException.class, sync::openStream);

        EventStreamRequestBody async = EventStreams.requestBody(EventStream.newWriter(), CODEC, event("initial"))
                                                   .withFrameTransformer(signer(new HashChain(1)));
        Collector collector = new Collector();
        async.subscribe(collector);
        assertNotNull(collector.subscription);
        assertInstanceOf(EventStreamSigningException.class, collector.error.get());
    }

    private static EventStreamRequestBody signed(EventStream<SerializableStruct> writer, HashChain algorithm) {
        return EventStreams.requestBody(writer, CODEC, null).withFrameTransformer(signer(algorithm));
    }

    private static EventStreamSigner signer(HashChain algorithm) {
        return EventStreamSigner.create(SEED, algorithm, CLOCK);
    }

    private static void writeAll(EventStream<SerializableStruct> writer, List<SerializableStruct> events) {
        writeAll(writer, events, true);
    }

    private static void writeAll(EventStream<SerializableStruct> writer, List<SerializableStruct> events,
                                 boolean close) {
        for (SerializableStruct e : events) {
            writer.asWriter().write(e);
        }
        if (close) {
            writer.close();
        }
    }

    /** Decodes the signed envelopes and verifies order, payloads, the closing frame and every chained signature. */
    private static void verifyChain(byte[] bytes, List<SerializableStruct> events) {
        List<Message> frames = new MessageDecoder().decode(ByteBuffer.wrap(bytes));
        assertEquals(events.size() + 1, frames.size());
        byte[] prior = SEED;
        for (int i = 0; i < frames.size(); i++) {
            Message frame = frames.get(i);
            byte[] signature = frame.header(EventStreamSigner.CHUNK_SIGNATURE_HEADER).asByteArray();
            LinkedHashMap<String, HeaderValue> dateHeader = new LinkedHashMap<>();
            dateHeader.put(EventStreamSigner.DATE_HEADER, frame.header(EventStreamSigner.DATE_HEADER));
            assertArrayEquals(HashChain.expected(prior, dateHeader, frame.payloadBytes()), signature, "frame " + i);
            if (i < events.size()) {
                assertEquals(CODEC.encodeEvent(events.get(i)), new MessageDecoder().decodeNext(frame.payload()));
            } else {
                assertEquals(0, frame.payloadLength(), "the closing frame is empty");
            }
            prior = signature;
        }
    }

    private static void copy(InputStream in, ByteArrayOutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
    }

    private static boolean causes(Throwable t, Object cause) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c == cause || (cause instanceof Class<?> type && type.isInstance(c))) {
                return true;
            }
        }
        return false;
    }

    private static void awaitBlocked(Thread producer) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (producer.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertEquals(Thread.State.WAITING, producer.getState(), "the producer blocks on the full queue");
    }

    private static SerializableStruct event(String payload) {
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

    /**
     * A verifiable test signature: SHA-256(prior || headers || payload). Fails with an {@link IllegalStateException}
     * on call {@code failAt} (1-based; -1 never).
     */
    private static final class HashChain implements FrameSignatureAlgorithm {
        private final int failAt;
        private final AtomicInteger calls = new AtomicInteger();

        HashChain(int failAt) {
            this.failAt = failAt;
        }

        @Override
        public byte[] sign(byte[] priorSignature, Instant signingTime, byte[] headers, ByteBuffer payload) {
            if (calls.incrementAndGet() == failAt) {
                throw new IllegalStateException("The signing identity expired");
            }
            byte[] bytes = new byte[payload.remaining()];
            payload.get(bytes);
            return hash(priorSignature, headers, bytes);
        }

        static byte[] expected(byte[] prior, Map<String, HeaderValue> headers, byte[] payload) {
            return hash(prior, MessageEncoder.encodeHeaders(headers), payload);
        }

        private static byte[] hash(byte[] prior, byte[] headers, byte[] payload) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(prior);
                digest.update(headers);
                digest.update(payload);
                return digest.digest();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    /** Records chunks, the terminal signal and the subscription; requests nothing by itself. */
    private static final class Collector implements Subscriber<ByteBuffer> {
        private final List<byte[]> chunks = new ArrayList<>();
        private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final CountDownLatch completed = new CountDownLatch(1);
        private volatile Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            chunks.add(bytes);
        }

        @Override
        public void onError(Throwable throwable) {
            error.set(throwable);
        }

        @Override
        public void onComplete() {
            completed.countDown();
        }

        byte[] bytes() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            chunks.forEach(out::writeBytes);
            return out.toByteArray();
        }
    }
}
