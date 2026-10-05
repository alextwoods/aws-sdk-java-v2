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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.eventstream.FrameTransformer;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.ProtocolEventStreamWriter;

/**
 * One consumption of an input event stream: the activated Smithy Java writer and the byte stream of its encoded (and
 * transformed, for example signed) frames, with the terminal rules of the request body.
 *
 * <ul>
 *   <li>Transform failure (for example {@code EventStreamSigningException}): the failing {@code write} throws, the
 *       writer is closed with the failure, and the transport receives the frames queued before it followed by an
 *       {@link IOException} whose cause is the failure. A failure signing the closing frame fails the transport the
 *       same way instead of ending the stream normally.</li>
 *   <li>Cancellation ({@link #cancel()}, or {@link #close()} before the end of the stream): no later frame is
 *       encoded or transformed (no signature advances, no closing frame), later writes fail with a
 *       {@link CancellationException} cause, and the transport's later reads fail. Releasing the writer's bounded
 *       pipe can block (a full queue, a producer blocked in {@code write}), so it runs on two short-lived virtual
 *       threads and never blocks the cancelling (possibly event-loop) thread.</li>
 * </ul>
 *
 * <p>Reads come from one consumer thread (the transport); cancellation may come from any thread.
 */
final class OutgoingEventStream extends InputStream {
    private static final String RELEASE_THREAD = "aws-sdk-event-stream-release";

    private final ProtocolEventStreamWriter<SerializableStruct, SerializableStruct, MessageFrames.MessageFrame> writer;
    private final AtomicReference<Exception> terminal = new AtomicReference<>();
    private volatile boolean cancelled;
    private volatile boolean pipeTerminated;
    /** The caller's publisher feeding the writer, or {@code null} for a writer the caller writes directly. */
    private volatile PublisherEventStream.Pump<?> pump;
    private final InputStream pipe;

    private OutgoingEventStream(EventStream<? extends SerializableStruct> events) {
        this.writer = ProtocolEventStreamWriter.of(events);
        this.pipe = writer.toDataStream().asInputStream();
    }

    /**
     * Binds {@code events} to {@code codec} and {@code transformer}, activates the writer (encoding the
     * {@code initialRequest} event, if any, on this thread) and returns the frame stream.
     *
     * @throws RuntimeException if the initial event cannot be encoded or transformed; the stream is failed
     */
    static OutgoingEventStream open(EventStream<? extends SerializableStruct> events, EventStreamCodec codec,
                                    SerializableStruct initialRequest, FrameTransformer transformer) {
        OutgoingEventStream stream = new OutgoingEventStream(events);
        stream.writer.setEventEncoderFactory(MessageFrames.encoders(codec, transformer, stream));
        if (initialRequest != null) {
            stream.writer.setInitialEvent(initialRequest);
        }
        try {
            stream.writer.activate();
        } catch (RuntimeException e) {
            // Nothing is sent: a caller's publisher is never subscribed.
            PublisherEventStream.discard(events);
            throw e;
        }
        // A caller's publisher (PublisherEventStream) is subscribed only now, when the transport consumes the body.
        stream.pump = PublisherEventStream.start(events);
        if (!stream.isActive()) {
            // Cancelled while the publisher subscribed (cancel() ran before pump was published).
            stream.cancelPump();
        }
        return stream;
    }

    private void cancelPump() {
        PublisherEventStream.Pump<?> p = pump;
        if (p != null) {
            p.cancel();
        }
    }

    /** True until the stream is cancelled or a frame fails to transform; encoders check it before each frame. */
    boolean isActive() {
        return terminal.get() == null;
    }

    /**
     * Throws if the stream is no longer active, so that no frame is encoded or transformed after cancellation or
     * failure.
     */
    void checkActive() {
        Exception cause = terminal.get();
        if (cause != null) {
            throw new IllegalStateException(cancelled ? "The event stream request was cancelled"
                                                      : "The event stream request failed", cause);
        }
    }

    /** Records that transforming a frame failed with {@code failure} and fails the writer (and thus the transport). */
    void failed(RuntimeException failure) {
        if (terminal.compareAndSet(null, failure)) {
            // On the producer thread: blocks at most like a write (the transport is still draining the pipe). A
            // no-op if the writer is already closing (a failed closing frame); read() then fails at the end.
            writer.closeWithError(failure);
            cancelPump();
        }
    }

    /** Cancels the stream (idempotent; a no-op after its end or failure). Never blocks. */
    void cancel() {
        if (pipeTerminated) {
            return;
        }
        CancellationException cancellation = new CancellationException("The event stream request body was cancelled");
        if (!terminal.compareAndSet(null, cancellation)) {
            return;
        }
        cancelled = true;
        // Reaches the caller's publisher at once, even while the pump waits for its next event (Reactive Streams 3.5).
        cancelPump();
        InputStream frames = pipe;
        // closeWithError enqueues the end-of-stream marker (blocks while the bounded queue is full); the drainer
        // empties the queue so that it, and a producer blocked in write, can proceed, then releases the pipe.
        Thread.ofVirtual().name(RELEASE_THREAD).start(() -> writer.closeWithError(cancellation));
        Thread.ofVirtual().name(RELEASE_THREAD).start(() -> drain(frames));
    }

    private static void drain(InputStream frames) {
        byte[] discard = new byte[8192];
        try {
            while (frames.read(discard, 0, discard.length) >= 0) {
                // Discard frames queued before or during cancellation.
            }
        } catch (IOException | RuntimeException e) {
            // The end-of-stream marker of a failed or closed pipe; nothing is left to release.
        } finally {
            closeQuietly(frames);
        }
    }

    private static void closeQuietly(InputStream frames) {
        try {
            frames.close();
        } catch (IOException | RuntimeException e) {
            // Best-effort release of a terminated pipe.
        }
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        return n < 0 ? -1 : one[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (cancelled) {
            throw new IOException("The event stream request body was cancelled", terminal.get());
        }
        if (len == 0) {
            return 0;
        }
        int n;
        try {
            n = pipe.read(b, off, len);
        } catch (IOException e) {
            pipeTerminated = true;
            throw e;
        }
        if (n < 0) {
            pipeTerminated = true;
            Exception failure = terminal.get();
            if (failure != null && !cancelled) {
                // The closing frame failed to transform: the stream must not end as if it were complete.
                throw new IOException("The event stream request failed", failure);
            }
        }
        return n;
    }

    /** Releases the stream at its end; closing before the end cancels it. */
    @Override
    public void close() {
        if (pipeTerminated) {
            closeQuietly(pipe);
        } else {
            cancel();
        }
    }
}
