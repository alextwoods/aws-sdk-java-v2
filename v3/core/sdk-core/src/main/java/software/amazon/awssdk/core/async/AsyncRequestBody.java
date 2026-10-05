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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.internal.async.ChunkSource;
import software.amazon.awssdk.core.internal.async.ChunkedAsyncRequestBody;

/**
 * The body of a streaming-input operation of an async client: a Reactive Streams {@link Publisher} of
 * {@link ByteBuffer} chunks with a known or unknown {@link #contentLength()}, source compatible with the V2
 * {@code AsyncRequestBody} factories {@code fromBytes}, {@code fromBytesUnsafe}, {@code fromByteBuffer},
 * {@code fromRemainingByteBuffer}, {@code fromString}, {@code fromFile}, {@code fromPublisher} and {@code empty}.
 *
 * <p>Every body created here enforces demand exactly (design, "Generated async client"): a chunk is read only under
 * outstanding demand, never more chunks than requested are delivered, delivered buffers are read-only, a non-positive
 * {@code request} fails the subscription with {@link IllegalArgumentException}, and each subscriber reads the body
 * again from its first byte (so they are {@linkplain #isReplayable() replayable}). Chunks are at most {@link #DEFAULT_CHUNK_SIZE} bytes. File bodies are read on the thread
 * that calls {@code request}; the pipeline keeps that off HTTP event loops. {@link #fromPublisher} bodies keep the
 * demand behavior of the wrapped publisher.
 *
 * <p>ADAPTED from V2: this type extends {@link Publisher} directly (V2's {@code SdkPublisher} operator surface is not
 * part of V3 {@code sdk-core}), and the split, blocking-stream and executor-backed input-stream factories are not
 * provided yet.
 */
public interface AsyncRequestBody extends Publisher<ByteBuffer> {
    /** The largest chunk, in bytes, that a body created by this interface delivers: 64 KiB. */
    int DEFAULT_CHUNK_SIZE = 64 * 1024;

    /** The length of the body in bytes, if known. */
    Optional<Long> contentLength();

    /** The media type of the body; {@code application/octet-stream} unless the factory knows better. */
    default String contentType() {
        return "application/octet-stream";
    }

    /**
     * True if every subscription delivers the same bytes again from the first byte, so the SDK may read the body
     * before transmission (to compute a payload hash or a header checksum) and send it again on a retry (design
     * "Body ownership": an async body's replay factory). The bodies of the factories of this interface are
     * replayable, except {@link #fromPublisher}, whose source's replayability is unknown.
     *
     * <p>False by default: a body that does not declare itself replayable is subscribed to at most once, so it is
     * never retried after transmission began, and an operation that needs its bytes before transmission must use a
     * protocol alternative that does not (an unsigned payload with a trailing checksum) or fails before
     * transmission. An implementation that returns true must honor it for every subscription; a body that delivers
     * different bytes to a second subscriber would retransmit corrupt data.
     *
     * <p>ADAPTED from V2, where every async body is re-subscribed on retry regardless of its source.
     */
    default boolean isReplayable() {
        return false;
    }

    /**
     * A body of the chunks {@code publisher} delivers, of unknown length. Demand and cancellation are passed through
     * to {@code publisher} unchanged.
     */
    static AsyncRequestBody fromPublisher(Publisher<ByteBuffer> publisher) {
        Objects.requireNonNull(publisher, "publisher");
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                publisher.subscribe(subscriber);
            }

            @Override
            public String toString() {
                return "AsyncRequestBody(Publisher)";
            }
        };
    }

    /**
     * A body of the file at {@code path}, whose length is read now; each subscriber reopens the file. A file whose
     * size changes after this call fails the subscription with {@link IOException}.
     *
     * @throws UncheckedIOException if the file's size cannot be read
     */
    static AsyncRequestBody fromFile(Path path) {
        Objects.requireNonNull(path, "path");
        long length;
        try {
            length = Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the size of " + path, e);
        }
        return new ChunkedAsyncRequestBody(ChunkSource.ofFile(path, length, DEFAULT_CHUNK_SIZE), length,
                                           "application/octet-stream", "File");
    }

    /** {@link #fromFile(Path)} of {@code file}. */
    static AsyncRequestBody fromFile(File file) {
        return fromFile(file.toPath());
    }

    /** A body of {@code string} encoded with {@code cs}, of type {@code text/plain; charset=<cs>}. */
    static AsyncRequestBody fromString(String string, Charset cs) {
        byte[] bytes = string.getBytes(cs);
        return new ChunkedAsyncRequestBody(ChunkSource.ofBuffer(ByteBuffer.wrap(bytes), DEFAULT_CHUNK_SIZE),
                                           bytes.length, "text/plain; charset=" + cs.name(), "String");
    }

    /** {@link #fromString(String, Charset)} in UTF-8. */
    static AsyncRequestBody fromString(String string) {
        return fromString(string, StandardCharsets.UTF_8);
    }

    /** A body of a copy of {@code bytes}. */
    static AsyncRequestBody fromBytes(byte[] bytes) {
        return fromBytesUnsafe(bytes.clone());
    }

    /**
     * A body of {@code bytes} without copying them; the caller must not modify the array afterwards, or the bytes sent
     * are undefined.
     */
    static AsyncRequestBody fromBytesUnsafe(byte[] bytes) {
        return bytes(ByteBuffer.wrap(bytes), "Bytes");
    }

    /**
     * A body of a copy of the whole of {@code byteBuffer}, from index zero to its limit regardless of its position (as
     * in V2). The buffer's position is not changed.
     */
    static AsyncRequestBody fromByteBuffer(ByteBuffer byteBuffer) {
        ByteBuffer view = byteBuffer.duplicate().rewind();
        return bytes(copyOf(view), "ByteBuffer");
    }

    /** A body of a copy of the remaining bytes of {@code byteBuffer}; the buffer's position is not changed. */
    static AsyncRequestBody fromRemainingByteBuffer(ByteBuffer byteBuffer) {
        return bytes(copyOf(byteBuffer.duplicate()), "ByteBuffer");
    }

    /** An empty body of length zero, which completes on the first request. */
    static AsyncRequestBody empty() {
        return bytes(ByteBuffer.allocate(0), "Empty");
    }

    private static AsyncRequestBody bytes(ByteBuffer data, String name) {
        return new ChunkedAsyncRequestBody(ChunkSource.ofBuffer(data, DEFAULT_CHUNK_SIZE), data.remaining(),
                                           "application/octet-stream", name);
    }

    private static ByteBuffer copyOf(ByteBuffer view) {
        ByteBuffer copy = ByteBuffer.allocate(view.remaining());
        copy.put(view).flip();
        return copy;
    }

}
