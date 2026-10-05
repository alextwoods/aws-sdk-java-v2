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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * The body of an {@link SdkHttpRequest} or {@link SdkHttpResponse}: absent, fully buffered bytes, or a stream.
 *
 * <p>A {@link Kind#STREAMING} body carries a {@link StreamingBody}: its data is never buffered by this type, and is
 * read through the stream's blocking or demand-driven view. Which party closes a streaming body's stream or
 * subscription is defined by {@link SyncHttpExchange} and {@link SdkAsyncHttpClient}.
 *
 * <p>Instances are immutable: bytes are copied in and exposed as read-only buffers. Streaming bodies compare by
 * identity of their {@link StreamingBody}.
 */
public final class HttpBody {
    private static final HttpBody EMPTY = new HttpBody(Kind.EMPTY, new byte[0], 0);

    private final Kind kind;
    private final byte[] bytes;
    private final long contentLength;
    private final StreamingBody stream;

    /** What a body holds. */
    public enum Kind {
        /** No body. */
        EMPTY,
        /** Fully buffered bytes. */
        BYTES,
        /** A {@link StreamingBody}, consumed incrementally. */
        STREAMING
    }

    private HttpBody(Kind kind, byte[] bytes, long contentLength) {
        this(kind, bytes, contentLength, null);
    }

    private HttpBody(Kind kind, byte[] bytes, long contentLength, StreamingBody stream) {
        this.kind = kind;
        this.bytes = bytes;
        this.contentLength = contentLength;
        this.stream = stream;
    }

    /** No body. */
    public static HttpBody empty() {
        return EMPTY;
    }

    /** A body of a copy of {@code bytes}; empty if {@code bytes} is empty. */
    public static HttpBody fromBytes(byte[] bytes) {
        return bytes.length == 0 ? EMPTY : new HttpBody(Kind.BYTES, bytes.clone(), bytes.length);
    }

    /** A body of the remaining bytes of {@code buffer}, copied; the buffer's position is not changed. */
    public static HttpBody fromBytes(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] copy = new byte[view.remaining()];
        view.get(copy);
        return copy.length == 0 ? EMPTY : new HttpBody(Kind.BYTES, copy, copy.length);
    }

    /** A body of the UTF-8 encoding of {@code text}. */
    public static HttpBody fromUtf8(String text) {
        byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
        return encoded.length == 0 ? EMPTY : new HttpBody(Kind.BYTES, encoded, encoded.length);
    }

    /** A streaming body of {@code stream}; its length is the stream's, if known. */
    public static HttpBody streaming(StreamingBody stream) {
        Objects.requireNonNull(stream, "stream");
        return new HttpBody(Kind.STREAMING, null, stream.contentLength().orElse(-1), stream);
    }

    /** What this body holds. */
    public Kind kind() {
        return kind;
    }

    /**
     * The stream of a {@link Kind#STREAMING} body.
     *
     * @throws IllegalStateException for a buffered or empty body
     */
    public StreamingBody streamingBody() {
        if (stream == null) {
            throw new IllegalStateException("A " + kind + " body is not streaming");
        }
        return stream;
    }

    /**
     * This body, buffered: a streaming body is read completely (and so consumed) into a {@link Kind#BYTES} or
     * {@link Kind#EMPTY} body; any other body is returned as is. Only for finite messages, such as error responses
     * and responses of operations without a streaming member; streaming payloads are never buffered.
     *
     * @throws java.io.UncheckedIOException if reading the stream fails
     */
    public HttpBody buffered() {
        if (kind != Kind.STREAMING) {
            return this;
        }
        byte[] data = StreamingBodies.readAll(stream);
        return data.length == 0 ? EMPTY : new HttpBody(Kind.BYTES, data, data.length);
    }

    /** The length in bytes, if known. */
    public OptionalLong contentLength() {
        return contentLength < 0 ? OptionalLong.empty() : OptionalLong.of(contentLength);
    }

    /**
     * The bytes, as a read-only buffer; empty for {@link Kind#EMPTY}.
     *
     * @throws IllegalStateException for a {@link Kind#STREAMING} body
     */
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(requireBuffered()).asReadOnlyBuffer();
    }

    /**
     * A copy of the bytes; empty for {@link Kind#EMPTY}.
     *
     * @throws IllegalStateException for a {@link Kind#STREAMING} body
     */
    public byte[] asByteArray() {
        return requireBuffered().clone();
    }

    /**
     * The bytes decoded as UTF-8.
     *
     * @throws IllegalStateException for a {@link Kind#STREAMING} body
     */
    public String asUtf8String() {
        return new String(requireBuffered(), StandardCharsets.UTF_8);
    }

    private byte[] requireBuffered() {
        if (kind == Kind.STREAMING) {
            throw new IllegalStateException("A streaming body is not buffered; read it through streamingBody() or "
                                            + "buffer a finite one with buffered()");
        }
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HttpBody other && kind == other.kind && contentLength == other.contentLength
                            && Arrays.equals(bytes, other.bytes) && stream == other.stream;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * kind.hashCode() + Long.hashCode(contentLength)) + Arrays.hashCode(bytes))
               + System.identityHashCode(stream);
    }

    /** Renders the kind and length only; content may be sensitive. */
    @Override
    public String toString() {
        return "HttpBody[" + kind + (contentLength < 0 ? "" : ", " + contentLength + " bytes") + "]";
    }
}
