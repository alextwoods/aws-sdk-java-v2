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

package software.amazon.awssdk.core.async;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;

class AsyncRequestBodyTest {
    private static final int CHUNK = AsyncRequestBody.DEFAULT_CHUNK_SIZE;

    private static byte[] pattern(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    @Test
    void bytesAreCopiedChunkedAndReplayedForEverySubscriber() {
        byte[] bytes = pattern(2 * CHUNK + 5);
        AsyncRequestBody body = AsyncRequestBody.fromBytes(bytes);
        byte[] expected = bytes.clone();
        bytes[0] = 42;

        assertEquals(Optional.of((long) expected.length), body.contentLength());
        assertEquals("application/octet-stream", body.contentType());
        for (int subscriber = 0; subscriber < 2; subscriber++) {
            RecordingSubscriber recorder = RecordingSubscriber.drain(body);
            assertTrue(recorder.completed);
            assertEquals(3, recorder.chunks.size());
            assertTrue(recorder.chunks.stream().allMatch(ByteBuffer::isReadOnly));
            assertArrayEquals(expected, recorder.bytes());
        }
    }

    @Test
    void demandIsExact() {
        RecordingSubscriber recorder = new RecordingSubscriber();
        AsyncRequestBody.fromBytes(pattern(3 * CHUNK)).subscribe(recorder);
        assertTrue(recorder.chunks.isEmpty());

        recorder.subscription.request(1);
        assertEquals(1, recorder.chunks.size());
        recorder.subscription.request(1);
        assertEquals(2, recorder.chunks.size());
        assertFalse(recorder.completed);

        recorder.subscription.request(5);
        assertEquals(3, recorder.chunks.size());
        assertTrue(recorder.completed);
    }

    @Test
    void nonPositiveRequestFailsAndCancelStopsDelivery() {
        RecordingSubscriber invalid = new RecordingSubscriber();
        AsyncRequestBody.fromString("abc").subscribe(invalid);
        invalid.subscription.request(0);
        assertInstanceOf(IllegalArgumentException.class, invalid.error);
        invalid.subscription.request(1);
        assertTrue(invalid.chunks.isEmpty());

        RecordingSubscriber cancelled = new RecordingSubscriber();
        AsyncRequestBody.fromBytes(pattern(2 * CHUNK)).subscribe(cancelled);
        cancelled.subscription.request(1);
        cancelled.subscription.cancel();
        cancelled.subscription.request(1);
        assertEquals(1, cancelled.chunks.size());
        assertFalse(cancelled.completed);
        assertNull(cancelled.error);
    }

    @Test
    void stringsCarryTheirCharset() {
        AsyncRequestBody utf8 = AsyncRequestBody.fromString("h\u00e9");
        assertEquals("text/plain; charset=UTF-8", utf8.contentType());
        assertArrayEquals("h\u00e9".getBytes(StandardCharsets.UTF_8), RecordingSubscriber.drain(utf8).bytes());
        assertEquals("text/plain; charset=ISO-8859-1",
                     AsyncRequestBody.fromString("x", StandardCharsets.ISO_8859_1).contentType());
    }

    @Test
    void byteBuffersFollowV2PositionSemantics() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
        buffer.position(2);

        assertArrayEquals(new byte[] {1, 2, 3, 4}, RecordingSubscriber.drain(AsyncRequestBody.fromByteBuffer(buffer))
                                                                     .bytes());
        assertArrayEquals(new byte[] {3, 4},
                          RecordingSubscriber.drain(AsyncRequestBody.fromRemainingByteBuffer(buffer)).bytes());
        assertEquals(2, buffer.position());
    }

    @Test
    void unsafeBytesAreNotCopied() {
        byte[] bytes = {1, 2};
        AsyncRequestBody body = AsyncRequestBody.fromBytesUnsafe(bytes);
        bytes[0] = 9;
        assertArrayEquals(new byte[] {9, 2}, RecordingSubscriber.drain(body).bytes());
    }

    @Test
    void emptyCompletesWithoutData() {
        AsyncRequestBody body = AsyncRequestBody.empty();
        assertEquals(Optional.of(0L), body.contentLength());
        RecordingSubscriber recorder = RecordingSubscriber.drain(body);
        assertTrue(recorder.completed);
        assertTrue(recorder.chunks.isEmpty());
    }

    @Test
    void filesAreReopenedPerSubscriberAndSizeChangesFail(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("body.bin");
        byte[] bytes = pattern(CHUNK + 1);
        Files.write(file, bytes);
        AsyncRequestBody body = AsyncRequestBody.fromFile(file);
        assertEquals(Optional.of((long) bytes.length), body.contentLength());
        assertArrayEquals(bytes, RecordingSubscriber.drain(body).bytes());
        assertArrayEquals(bytes, RecordingSubscriber.drain(AsyncRequestBody.fromFile(file.toFile())).bytes());

        Files.write(file, new byte[3]);
        RecordingSubscriber shrunk = RecordingSubscriber.drain(body);
        assertInstanceOf(IOException.class, shrunk.error);
        assertFalse(shrunk.completed);

        Files.write(file, pattern(CHUNK + 2));
        assertInstanceOf(IOException.class, RecordingSubscriber.drain(body).error);

        assertThrows(UncheckedIOException.class, () -> AsyncRequestBody.fromFile(dir.resolve("missing")));
    }

    @Test
    void publishersPassThroughWithUnknownLength() {
        AsyncRequestBody source = AsyncRequestBody.fromString("abc");
        Publisher<ByteBuffer> publisher = source::subscribe;
        AsyncRequestBody body = AsyncRequestBody.fromPublisher(publisher);
        assertEquals(Optional.empty(), body.contentLength());
        assertEquals("application/octet-stream", body.contentType());
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), RecordingSubscriber.drain(body).bytes());
    }

    @Test
    void isReplayable_factoryBodies_areReplayableButPublishersAndCustomBodiesAreNot(@TempDir Path dir)
        throws IOException {
        Path file = dir.resolve("body.bin");
        Files.write(file, pattern(3));
        for (AsyncRequestBody body : new AsyncRequestBody[] {
            AsyncRequestBody.fromBytes(pattern(3)), AsyncRequestBody.fromBytesUnsafe(pattern(3)),
            AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(pattern(3))),
            AsyncRequestBody.fromRemainingByteBuffer(ByteBuffer.wrap(pattern(3))), AsyncRequestBody.fromString("x"),
            AsyncRequestBody.fromFile(file), AsyncRequestBody.empty()}) {
            assertTrue(body.isReplayable(), body.toString());
        }

        assertFalse(AsyncRequestBody.fromPublisher(AsyncRequestBody.fromString("x")).isReplayable(),
                    "a publisher's replayability is unknown");
        AsyncRequestBody custom = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(org.reactivestreams.Subscriber<? super ByteBuffer> subscriber) {
                AsyncRequestBody.empty().subscribe(subscriber);
            }
        };
        assertFalse(custom.isReplayable(), "a body that does not declare itself replayable is one-shot");
    }

    @Test
    void nullSubscriberIsRejected() {
        assertThrows(NullPointerException.class, () -> AsyncRequestBody.empty().subscribe(null));
    }
}
