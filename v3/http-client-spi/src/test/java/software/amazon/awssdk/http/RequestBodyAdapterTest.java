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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;

class RequestBodyAdapterTest {
    private static final int MAX = StreamingBodies.DEFAULT_MAX_CHUNK_SIZE;

    private static byte[] pattern(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i * 7);
        }
        return bytes;
    }

    /** Requests one chunk at a time and records everything. */
    private static final class Collector implements Subscriber<ByteBuffer> {
        private final List<ByteBuffer> chunks = new ArrayList<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Subscription subscription;
        private Throwable error;
        private boolean completed;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            chunks.add(chunk);
            ByteBuffer view = chunk.duplicate();
            byte[] b = new byte[view.remaining()];
            view.get(b);
            bytes.writeBytes(b);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onComplete() {
            completed = true;
        }
    }

    private static Collector collect(StreamingBody body) {
        Collector collector = new Collector();
        body.subscribe(collector);
        return collector;
    }

    private static byte[] readStream(StreamingBody body) throws IOException {
        try (InputStream in = body.openStream()) {
            return in.readAllBytes();
        }
    }

    @Test
    void replayableRequestBodyKeepsMetadataAndBothViews() throws IOException {
        byte[] bytes = pattern(2 * MAX + 3);
        StreamingBody body = RequestBodyAdapter.toStreamingBody(RequestBody.fromBytes(bytes));

        assertEquals(OptionalLong.of(bytes.length), body.contentLength());
        assertEquals(Optional.of("application/octet-stream"), body.contentType());
        assertTrue(body.isReplayable());
        assertArrayEquals(bytes, readStream(body));
        Collector collector = collect(body);
        assertTrue(collector.completed);
        assertEquals(3, collector.chunks.size());
        assertTrue(collector.chunks.stream().allMatch(c -> c.remaining() <= MAX && c.isReadOnly()));
        assertArrayEquals(bytes, collector.bytes.toByteArray());
        assertArrayEquals(bytes, readStream(body), "a replayable body can be read again");
    }

    @Test
    void oneShotRequestBodyIsConsumedOnce() throws IOException {
        RequestBody source = RequestBody.fromInputStream(new ByteArrayInputStream(pattern(10)), 10);
        HttpBody http = RequestBodyAdapter.toHttpBody(source);
        assertEquals(HttpBody.Kind.STREAMING, http.kind());
        StreamingBody body = http.streamingBody();

        assertFalse(body.isReplayable());
        assertArrayEquals(pattern(10), readStream(body));
        assertFalse(body.isAvailable());
        assertThrows(IllegalStateException.class, body::openStream);
        assertInstanceOf(IllegalStateException.class, collect(body).error);
    }

    @Test
    void requestBodyLengthIsEnforced() {
        RequestBody lying = RequestBody.fromContentProvider(() -> new ByteArrayInputStream(new byte[3]), 5,
                                                            "text/plain");
        StreamingBody body = RequestBodyAdapter.toStreamingBody(lying);
        assertEquals(Optional.of("text/plain"), body.contentType());
        assertInstanceOf(ContentLengthMismatchException.class, collect(body).error);
        assertThrows(IOException.class, () -> readStream(body));
    }

    @Test
    void toStreamingBody_replayableAsyncBody_isReChunkedAndReplayableThroughBothViews() throws IOException {
        byte[] bytes = pattern(MAX + 9);
        StreamingBody body = RequestBodyAdapter.toStreamingBody(AsyncRequestBody.fromBytes(bytes));
        assertEquals(OptionalLong.of(bytes.length), body.contentLength());
        assertTrue(body.isReplayable());

        Collector collector = collect(body);
        assertTrue(collector.completed);
        assertTrue(collector.chunks.stream().allMatch(c -> c.remaining() <= MAX && c.isReadOnly()));
        assertArrayEquals(bytes, collector.bytes.toByteArray());
        // A payload hash read through the blocking view, then transmission and a retry through the publisher.
        assertArrayEquals(bytes, readStream(body));
        assertArrayEquals(bytes, collect(body).bytes.toByteArray(), "a replayable body is read again");
        assertTrue(body.isAvailable());

        StreamingBody sync = RequestBodyAdapter.toHttpBody(AsyncRequestBody.fromString("h\u00e9llo")).streamingBody();
        assertEquals(Optional.of("text/plain; charset=UTF-8"), sync.contentType());
        assertArrayEquals("h\u00e9llo".getBytes(StandardCharsets.UTF_8), readStream(sync));
    }

    @Test
    void toStreamingBody_publisherAsyncBody_isConsumedOnce() {
        byte[] bytes = pattern(MAX + 9);
        StreamingBody body = RequestBodyAdapter.toStreamingBody(
            AsyncRequestBody.fromPublisher(AsyncRequestBody.fromBytes(bytes)));
        assertFalse(body.isReplayable());

        Collector collector = collect(body);
        assertTrue(collector.completed);
        assertArrayEquals(bytes, collector.bytes.toByteArray());
        assertFalse(body.isAvailable());
        assertInstanceOf(IllegalStateException.class, collect(body).error, "a second consumption is refused");
        assertThrows(IllegalStateException.class, body::openStream);
    }

    @Test
    void asyncRequestBodyOfUnknownLengthStreams() throws IOException {
        AsyncRequestBody source = AsyncRequestBody.fromPublisher(AsyncRequestBody.fromBytes(pattern(3 * MAX)));
        StreamingBody body = RequestBodyAdapter.toStreamingBody(source);
        assertEquals(OptionalLong.empty(), body.contentLength());
        assertArrayEquals(pattern(3 * MAX), readStream(body));
    }

    @Test
    void blockingViewSurfacesPublisherErrors() {
        IOException failure = new IOException("upstream failed");
        AsyncRequestBody failing = AsyncRequestBody.fromPublisher(s -> {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    s.onError(failure);
                }

                @Override
                public void cancel() {
                }
            });
        });
        StreamingBody body = RequestBodyAdapter.toStreamingBody(failing);
        IOException e = assertThrows(IOException.class, () -> readStream(body));
        assertEquals("upstream failed", e.getMessage());
    }
}
