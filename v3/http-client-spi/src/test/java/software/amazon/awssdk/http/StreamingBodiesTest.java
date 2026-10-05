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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The {@link StreamingBody} contract of {@link StreamingBodies}: fragmented input, demand, cancellation, exactly one
 * terminal signal, bounded buffering, content-length enforcement and replay (Requirements 5.11, 8.4, 8.5, 12.5, 12.6).
 */
class StreamingBodiesTest {
    private static final byte[] DATA = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    @Test
    void fragmentedStreamsAreReadCompletelyInOrderThroughBothViews() throws IOException {
        StreamingBody sync = StreamingBodies.fromInputStream(new FragmentingStream(DATA), DATA.length, null);
        try (InputStream in = sync.openStream()) {
            assertArrayEquals(DATA, in.readAllBytes());
        }
        StreamingBody async = StreamingBodies.fromInputStream(new FragmentingStream(DATA), DATA.length, null, 4);
        Recorder recorder = new Recorder(Long.MAX_VALUE);
        async.subscribe(recorder);
        assertArrayEquals(DATA, recorder.bytes());
        assertEquals(DATA.length, recorder.chunks.size(), "one chunk per 1-byte fragment");
        assertEquals(1, recorder.completions);
        assertEquals(0, recorder.errors.size());
    }

    @Test
    void boundedChunksDropsEmptyBuffersSlicesLargeOnesAndPreservesOrder() {
        List<ByteBuffer> upstream = List.of(ByteBuffer.allocate(0), ByteBuffer.wrap(DATA, 0, 1),
                                            ByteBuffer.allocate(0), ByteBuffer.wrap(DATA, 1, DATA.length - 1),
                                            ByteBuffer.allocate(0));
        Scripted source = new Scripted(upstream, null);
        Recorder recorder = new Recorder(Long.MAX_VALUE);
        StreamingBodies.boundedChunks(source, 5, DATA.length).subscribe(recorder);
        assertArrayEquals(DATA, recorder.bytes());
        for (ByteBuffer chunk : recorder.chunks) {
            assertTrue(chunk.remaining() > 0 && chunk.remaining() <= 5, "chunk size " + chunk.remaining());
            assertTrue(chunk.isReadOnly());
        }
        assertEquals(1, recorder.completions);
        assertEquals(1, source.maxOutstanding, "one upstream buffer requested at a time");
    }

    @Test
    void deliversNothingWithoutDemandAndNoMoreThanRequested() {
        Recorder recorder = new Recorder(0);
        StreamingBody body = StreamingBodies.of(DATA.length, null, true, () -> null,
                                                new Scripted(List.of(ByteBuffer.wrap(DATA)), null), 4);
        body.subscribe(recorder);
        assertEquals(0, recorder.chunks.size());
        recorder.subscription.request(2);
        assertEquals(2, recorder.chunks.size());
        recorder.subscription.request(1);
        assertEquals(3, recorder.chunks.size());
        assertEquals(0, recorder.completions);
        recorder.subscription.request(Long.MAX_VALUE);
        assertArrayEquals(DATA, recorder.bytes());
        assertEquals(1, recorder.completions);
    }

    @Test
    void reentrantRequestsFromOnNextDoNotRecurse() {
        byte[] large = new byte[200_000];
        StreamingBody body = StreamingBodies.fromInputStream(new FragmentingStream(large), large.length, null, 1);
        Recorder recorder = new Recorder(1) {
            @Override
            public void onNext(ByteBuffer item) {
                super.onNext(item);
                subscription.request(1);
            }
        };
        body.subscribe(recorder);
        assertEquals(large.length, recorder.bytes().length);
        assertEquals(1, recorder.completions);
    }

    @Test
    void cancellationMidStreamStopsDeliveryAndReleasesTheSource() {
        Scripted source = new Scripted(List.of(ByteBuffer.wrap(DATA, 0, 4), ByteBuffer.wrap(DATA, 4, 4),
                                               ByteBuffer.wrap(DATA, 8, 4)), null);
        Recorder recorder = new Recorder(1);
        StreamingBodies.boundedChunks(source, 64, -1).subscribe(recorder);
        recorder.subscription.cancel();
        recorder.subscription.request(10);
        assertEquals(1, recorder.chunks.size());
        assertTrue(source.cancelled);
        assertEquals(0, recorder.completions + recorder.errors.size(), "no terminal signal after cancel");

        FragmentingStream stream = new FragmentingStream(DATA);
        Recorder pull = new Recorder(2);
        StreamingBodies.fromInputStream(stream, DATA.length, null).subscribe(pull);
        pull.subscription.cancel();
        pull.subscription.request(5);
        assertEquals(2, pull.chunks.size());
        assertTrue(stream.closed, "the source stream is closed on cancel");
        assertEquals(0, pull.completions + pull.errors.size());
    }

    @Test
    void anUpstreamErrorMidStreamIsSignalledExactlyOnce() {
        IOException failure = new IOException("connection reset");
        Scripted source = new Scripted(List.of(ByteBuffer.wrap(DATA, 0, 4), ByteBuffer.wrap(DATA, 4, 4)), failure);
        Recorder recorder = new Recorder(Long.MAX_VALUE);
        StreamingBodies.boundedChunks(source, 64, -1).subscribe(recorder);
        source.misbehave(); // a late onComplete/onNext after onError must be ignored
        assertEquals(2, recorder.chunks.size());
        assertEquals(List.of(failure), recorder.errors);
        assertEquals(0, recorder.completions);

        StreamingBody failing = StreamingBodies.fromInputStream(new FailingStream(3), -1, null, 1);
        Recorder pull = new Recorder(Long.MAX_VALUE);
        failing.subscribe(pull);
        assertEquals(3, pull.chunks.size());
        assertEquals(1, pull.errors.size());
        assertInstanceOf(IOException.class, pull.errors.get(0));
        assertEquals(0, pull.completions);
    }

    @Test
    void nonPositiveRequestsAreErrors() {
        Recorder recorder = new Recorder(0);
        StreamingBodies.fromBytes(DATA, null).subscribe(recorder);
        recorder.subscription.request(0);
        recorder.subscription.request(1);
        assertEquals(1, recorder.errors.size());
        assertInstanceOf(IllegalArgumentException.class, recorder.errors.get(0));
        assertEquals(0, recorder.chunks.size());
    }

    @Test
    void bufferingStaysWithinOneChunkForASlowConsumer() {
        int upstreamChunk = 8 * 1024;
        int chunks = 64;
        CountingPublisher source = new CountingPublisher(upstreamChunk, chunks);
        Recorder slow = new Recorder(1);
        StreamingBodies.boundedChunks(source, 1024, (long) upstreamChunk * chunks).subscribe(slow);
        long maxInFlight = 0;
        while (slow.completions == 0) {
            maxInFlight = Math.max(maxInFlight, source.produced - slow.byteCount);
            slow.subscription.request(1);
        }
        assertEquals((long) upstreamChunk * chunks, slow.byteCount);
        assertTrue(maxInFlight <= upstreamChunk, "in-flight bytes " + maxInFlight);

        CountingStream stream = new CountingStream(1 << 20);
        Recorder pull = new Recorder(1);
        StreamingBodies.fromInputStream(stream, 1 << 20, null, 4096).subscribe(pull);
        long maxRead = 0;
        while (pull.completions == 0) {
            maxRead = Math.max(maxRead, stream.read - pull.byteCount);
            pull.subscription.request(1);
        }
        assertEquals(1 << 20, pull.byteCount);
        assertTrue(maxRead <= 4096, "read ahead " + maxRead);
    }

    @Test
    void contentLengthMismatchesFailBothViews() {
        IOException shortRead = assertThrows(IOException.class, () -> {
            try (InputStream in = StreamingBodies.fromInputStream(new FragmentingStream(DATA), DATA.length + 1, null)
                                                 .openStream()) {
                in.readAllBytes();
            }
        });
        assertInstanceOf(ContentLengthMismatchException.class, shortRead);
        assertThrows(ContentLengthMismatchException.class, () -> {
            try (InputStream in = StreamingBodies.fromInputStream(new FragmentingStream(DATA), 3, null).openStream()) {
                in.readAllBytes();
            }
        });

        Recorder tooShort = new Recorder(Long.MAX_VALUE);
        StreamingBodies.boundedChunks(new Scripted(List.of(ByteBuffer.wrap(DATA)), null), 64, DATA.length + 5)
                       .subscribe(tooShort);
        ContentLengthMismatchException e = assertInstanceOf(ContentLengthMismatchException.class,
                                                            tooShort.errors.get(0));
        assertEquals(DATA.length + 5, e.expected());
        assertEquals(DATA.length, e.actual());
        assertEquals(0, tooShort.completions);

        Scripted longSource = new Scripted(List.of(ByteBuffer.wrap(DATA)), null);
        Recorder tooLong = new Recorder(Long.MAX_VALUE);
        StreamingBodies.boundedChunks(longSource, 8, 10).subscribe(tooLong);
        assertEquals(1, tooLong.errors.size());
        assertInstanceOf(ContentLengthMismatchException.class, tooLong.errors.get(0));
        assertEquals(8, tooLong.byteCount, "only whole chunks within the declared length are delivered");
        assertTrue(longSource.cancelled);
    }

    @Test
    void replayableBodiesReplayFromTheFirstByteAndOneShotBodiesAreConsumedOnce(@TempDir Path dir)
        throws IOException {
        Path file = Files.write(dir.resolve("body.bin"), DATA);
        for (StreamingBody replayable : List.of(StreamingBodies.fromBytes(DATA, "text/plain"),
                                                StreamingBodies.fromFile(file, null),
                                                StreamingBodies.fromInputStreamSupplier(
                                                    () -> new FragmentingStream(DATA), DATA.length, null))) {
            assertTrue(replayable.isReplayable());
            for (int attempt = 0; attempt < 2; attempt++) {
                try (InputStream in = replayable.openStream()) {
                    assertArrayEquals(DATA, in.readAllBytes());
                }
                Recorder recorder = new Recorder(Long.MAX_VALUE);
                replayable.subscribe(recorder);
                assertArrayEquals(DATA, recorder.bytes());
                assertTrue(replayable.isAvailable());
            }
            assertEquals(DATA.length, replayable.contentLength().getAsLong());
        }

        StreamingBody oneShot = StreamingBodies.fromInputStream(new FragmentingStream(DATA), DATA.length, null);
        assertFalse(oneShot.isReplayable());
        assertTrue(oneShot.isAvailable());
        Recorder first = new Recorder(Long.MAX_VALUE);
        oneShot.subscribe(first);
        assertArrayEquals(DATA, first.bytes());
        assertFalse(oneShot.isAvailable());
        assertThrows(IllegalStateException.class, oneShot::openStream);
        Recorder second = new Recorder(Long.MAX_VALUE);
        oneShot.subscribe(second);
        assertEquals(0, second.chunks.size());
        assertInstanceOf(IllegalStateException.class, second.errors.get(0));
    }

    @Test
    void httpBodyHoldsTheStreamWithoutReadingItAndBuffersOnlyOnRequest() {
        FragmentingStream source = new FragmentingStream(DATA);
        StreamingBody stream = StreamingBodies.fromInputStream(source, DATA.length, "text/plain");
        HttpBody body = HttpBody.streaming(stream);
        assertEquals(HttpBody.Kind.STREAMING, body.kind());
        assertSame(stream, body.streamingBody());
        assertEquals(DATA.length, body.contentLength().getAsLong());
        assertThrows(IllegalStateException.class, body::asByteArray);
        assertEquals(0, source.reads, "holding a streaming body reads nothing");
        HttpBody buffered = body.buffered();
        assertEquals(HttpBody.Kind.BYTES, buffered.kind());
        assertArrayEquals(DATA, buffered.asByteArray());
        assertThrows(IllegalStateException.class, () -> HttpBody.fromUtf8("x").streamingBody());
        assertThrows(UncheckedIOException.class, () -> HttpBody.streaming(
            StreamingBodies.fromInputStream(new FragmentingStream(DATA), 1, null)).buffered());
    }

    /** Records every signal; requests {@code initial} on subscribe. */
    private static class Recorder implements Subscriber<ByteBuffer> {
        final List<ByteBuffer> chunks = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        final long initial;
        Subscription subscription;
        int completions;
        long byteCount;

        Recorder(long initial) {
            this.initial = initial;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            if (initial > 0) {
                s.request(initial);
            }
        }

        @Override
        public void onNext(ByteBuffer item) {
            assertTrue(item.hasRemaining(), "no empty chunks");
            assertEquals(0, completions + errors.size(), "no onNext after a terminal signal");
            chunks.add(item);
            byteCount += item.remaining();
        }

        @Override
        public void onError(Throwable t) {
            assertEquals(0, completions + errors.size(), "exactly one terminal signal");
            errors.add(t);
        }

        @Override
        public void onComplete() {
            assertEquals(0, completions + errors.size(), "exactly one terminal signal");
            completions++;
        }

        byte[] bytes() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (ByteBuffer chunk : chunks) {
                byte[] b = new byte[chunk.remaining()];
                chunk.duplicate().get(b);
                out.writeBytes(b);
            }
            return out.toByteArray();
        }
    }

    /** A publisher of fixed buffers, then complete or error; delivers synchronously on request. */
    private static final class Scripted implements Publisher<ByteBuffer> {
        private final List<ByteBuffer> items;
        private final Throwable error;
        private Subscriber<? super ByteBuffer> subscriber;
        private int next;
        private long outstanding;
        int maxOutstanding;
        boolean cancelled;
        boolean terminated;

        Scripted(List<ByteBuffer> items, Throwable error) {
            this.items = items;
            this.error = error;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    outstanding += n;
                    maxOutstanding = (int) Math.max(maxOutstanding, outstanding);
                    while (outstanding > 0 && next < items.size() && !cancelled) {
                        outstanding--;
                        s.onNext(items.get(next++).duplicate());
                    }
                    if (next == items.size() && !terminated && !cancelled) {
                        terminated = true;
                        if (error != null) {
                            s.onError(error);
                        } else {
                            s.onComplete();
                        }
                    }
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }

        /** Signals after termination, violating the protocol. */
        void misbehave() {
            subscriber.onNext(ByteBuffer.wrap(DATA));
            subscriber.onComplete();
        }
    }

    /** Produces {@code count} buffers of {@code size} bytes on demand, counting produced bytes. */
    private static final class CountingPublisher implements Publisher<ByteBuffer> {
        private final int size;
        private final int count;
        long produced;

        CountingPublisher(int size, int count) {
            this.size = size;
            this.count = count;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            AtomicInteger emitted = new AtomicInteger();
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    for (long i = 0; i < n && emitted.get() < count; i++) {
                        emitted.incrementAndGet();
                        produced += size;
                        s.onNext(ByteBuffer.allocate(size));
                    }
                    if (emitted.get() == count) {
                        emitted.incrementAndGet();
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    /** Returns one byte per read, with an empty read before each; records reads and close. */
    private static final class FragmentingStream extends InputStream {
        private final byte[] data;
        private int position;
        private boolean empty = true;
        int reads;
        boolean closed;

        FragmentingStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            reads++;
            if (position == data.length) {
                return -1;
            }
            empty = !empty;
            if (empty) {
                return 0;
            }
            b[off] = data[position++];
            return 1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** Supplies {@code good} bytes, then fails. */
    private static final class FailingStream extends InputStream {
        private int remaining;

        FailingStream(int good) {
            this.remaining = good;
        }

        @Override
        public int read() throws IOException {
            if (remaining-- > 0) {
                return 'x';
            }
            throw new IOException("stream failed");
        }
    }

    /** A zero-filled stream of {@code length} bytes counting bytes read from it. */
    private static final class CountingStream extends InputStream {
        private final long length;
        long read;

        CountingStream(long length) {
            this.length = length;
        }

        @Override
        public int read() {
            if (read == length) {
                return -1;
            }
            read++;
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (read == length) {
                return -1;
            }
            int n = (int) Math.min(len, length - read);
            read += n;
            return n;
        }
    }
}
