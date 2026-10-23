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

package software.amazon.awssdk.codecs.commons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * A growable in-memory {@link OutputStream} whose written bytes can be read without copying, as a {@link ByteBuffer}
 * ({@link #toByteBuffer()}) or as a request body ({@link #toContentStreamProvider()}).
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.io.ByteBufferOutputStream}. The SDK adds
 * {@link #toContentStreamProvider()}, so a serialized body reaches the HTTP client through
 * {@link ContentStreamProvider#contentAsByteBufferOrNull()} with no copy, and {@link #reserve(int)} and
 * {@link #advance(int)}, so a serializer can write straight into the internal array instead of copying from a buffer
 * of its own.
 *
 * <p><b>Sharing.</b> {@link #toByteBuffer()} and {@link #toContentStreamProvider()} share this stream's internal array
 * instead of copying it. Each one captures the bytes written so far. Bytes written afterwards are appended past the
 * captured range and are not part of the captured content. {@link #reset()} after either call moves this stream to a
 * new array of the same capacity, so writing after a reset never changes content that was handed out. {@link #array()}
 * exposes the internal array without capturing it; callers must not hold on to it.
 *
 * <p>This class is not thread-safe.
 */
@SdkPublicApi
@NotThreadSafe
public final class ByteBufferOutputStream extends OutputStream {
    private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
    private static final int DEFAULT_INITIAL_LENGTH = 32;

    private byte[] buf;
    private int count;

    /**
     * True when the bytes in {@code buf[0, count)} have been handed out through {@link #toByteBuffer()} or
     * {@link #toContentStreamProvider()}, so {@link #reset()} must not reuse {@code buf}.
     */
    private boolean shared;

    /**
     * Creates a stream with a 32-byte initial capacity.
     */
    public ByteBufferOutputStream() {
        this(DEFAULT_INITIAL_LENGTH);
    }

    /**
     * Creates a stream with the given initial capacity.
     *
     * @param initialLength the initial capacity in bytes
     * @throws IllegalArgumentException if {@code initialLength} is negative
     */
    public ByteBufferOutputStream(int initialLength) {
        if (initialLength < 0) {
            throw new IllegalArgumentException("initialLength must not be negative: " + initialLength);
        }
        this.buf = new byte[initialLength];
    }

    /**
     * Writes {@code len} bytes from {@code b}, starting at {@code off}.
     *
     * @param b the bytes to write
     * @param off the start offset in {@code b}
     * @param len the number of bytes to write
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is outside {@code b}
     */
    @Override
    public void write(byte[] b, int off, int len) {
        Objects.requireNonNull(b, "b must not be null");
        if ((off | len) < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length "
                                                + b.length);
        }
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes one byte, the low eight bits of {@code b}.
     *
     * @param b the byte to write
     */
    @Override
    public void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    /**
     * Returns a {@link ByteBuffer} over the bytes written so far, sharing this stream's internal array (see the class
     * documentation for what later writes and {@link #reset()} do).
     *
     * <p>The returned buffer always {@linkplain ByteBuffer#hasArray() has an accessible backing array}. Read it with
     * this idiom, which does not assume that the data starts at {@linkplain ByteBuffer#arrayOffset() offset 0} or spans
     * the whole array:
     *
     * <pre>{@code
     * int pos = buffer.arrayOffset() + buffer.position();
     * int len = buffer.remaining();
     * doSomethingWithBuffer(buffer.array(), pos, len);
     * }</pre>
     *
     * @return a buffer positioned at the first written byte, with its limit after the last one
     */
    public ByteBuffer toByteBuffer() {
        shared = true;
        return ByteBuffer.wrap(buf, 0, count);
    }

    /**
     * Returns a {@link ContentStreamProvider} for the bytes written so far, sharing this stream's internal array (see
     * the class documentation for what later writes and {@link #reset()} do).
     *
     * <p>{@link ContentStreamProvider#contentAsByteBufferOrNull()} returns a new array-backed, writable view over the
     * shared array on each call, positioned at the first byte with its limit after the last one, so an HTTP client can
     * send it without copying. The view is not read-only, because a read-only view hides its backing array and HTTP
     * clients then fall back to {@link ContentStreamProvider#newStream()}. As that method's contract says, callers must
     * not modify the content. {@link ContentStreamProvider#newStream()} returns a new stream over the same bytes on each
     * call.
     *
     * @return a provider of the bytes written so far
     */
    public ContentStreamProvider toContentStreamProvider() {
        shared = true;
        return new SharedArrayContentStreamProvider(buf, count);
    }

    /**
     * Writes the bytes written so far to {@code out}.
     *
     * @param out where to write
     * @throws IOException if {@code out} throws
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Returns the number of bytes written so far.
     *
     * @return the number of bytes written
     */
    public int size() {
        return count;
    }

    /**
     * Returns the internal array, without copying. The written bytes are at indexes {@code 0} to {@code size() - 1}; the
     * array may be longer. Later writes may replace the array, so do not hold on to it.
     *
     * @return the internal array
     */
    public byte[] array() {
        return buf;
    }

    /**
     * Makes room for at least {@code length} more bytes and returns the internal array, so a writer can put them at
     * indexes {@code size()} to {@code size() + length - 1} directly and then add them with {@link #advance(int)},
     * instead of filling a buffer of its own and copying it in with {@link #write(byte[], int, int)}. The array may be
     * a new one, holding the bytes written so far, and may have more room than requested; the contents past
     * {@code size()} are undefined. A later {@code reserve}, write or {@link #reset()} may replace the array, so call
     * {@code reserve} again before writing after any of them. Bytes put into the array are not part of the stream until
     * {@code advance} adds them: a write or {@link #reset()} before that may write over them or leave them in the old
     * array, so a writer must not let the stream be written while it holds bytes it has not added. SDK addition.
     *
     * @param length the number of bytes the caller will write
     * @return the internal array, with at least {@code size() + length} bytes
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public byte[] reserve(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative: " + length);
        }
        if (length > Integer.MAX_VALUE - count) {
            throw new OutOfMemoryError("Required array length " + count + " + " + length + " is too large");
        }
        ensureCapacity(count + length);
        return buf;
    }

    /**
     * Adds {@code length} bytes that the caller wrote into the array returned by {@link #reserve(int)}, at indexes
     * {@code size()} to {@code size() + length - 1}, to the stream. SDK addition.
     *
     * @param length the number of bytes written
     * @throws IndexOutOfBoundsException if {@code length} is negative or more than the array holds after
     *     {@code size()}
     */
    public void advance(int length) {
        if (length < 0 || length > buf.length - count) {
            throw new IndexOutOfBoundsException("Cannot advance " + length + " bytes: size " + count + ", capacity "
                                                + buf.length);
        }
        count += length;
    }

    /**
     * Writes an ASCII string, one byte per character. Each character is truncated to its low eight bits, so this is only
     * correct for ASCII (or Latin-1) text.
     *
     * @param s the string to write
     */
    @SuppressWarnings("deprecation")
    public void writeAscii(String s) {
        int len = s.length();
        if (len == 0) {
            return;
        }
        ensureCapacity(count + len);
        // String.getBytes(int, int, byte[], int) copies the low byte of each char without an intermediate array.
        s.getBytes(0, len, buf, count);
        count += len;
    }

    /**
     * Empties the stream. The capacity is kept: the internal array is reused, or, if it has been shared through
     * {@link #toByteBuffer()} or {@link #toContentStreamProvider()}, replaced with a new array of the same length so the
     * shared content is not overwritten.
     */
    public void reset() {
        if (shared) {
            buf = new byte[buf.length];
            shared = false;
        }
        count = 0;
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int minGrowth = minCapacity - oldCapacity;
        if (minGrowth > 0) {
            // The copy is a new array, so content shared from the old one stays as it was.
            buf = Arrays.copyOf(buf, newLength(oldCapacity, minGrowth, oldCapacity /* preferred growth */));
            shared = false;
        }
    }

    private static int newLength(int oldLength, int minGrowth, int prefGrowth) {
        int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
        if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
            return prefLength;
        }
        // put code cold in a separate method
        return hugeLength(oldLength, minGrowth);
    }

    private static int hugeLength(int oldLength, int minGrowth) {
        int minLength = oldLength + minGrowth;
        if (minLength < 0) { // overflow
            throw new OutOfMemoryError("Required array length " + oldLength + " + " + minGrowth + " is too large");
        }
        if (minLength <= SOFT_MAX_ARRAY_LENGTH) {
            return SOFT_MAX_ARRAY_LENGTH;
        }
        return minLength;
    }

    /**
     * A provider over {@code bytes[0, length)}, captured when the provider is created.
     */
    private static final class SharedArrayContentStreamProvider implements ContentStreamProvider {
        private final byte[] bytes;
        private final int length;

        private SharedArrayContentStreamProvider(byte[] bytes, int length) {
            this.bytes = bytes;
            this.length = length;
        }

        @Override
        public InputStream newStream() {
            return new ByteArrayInputStream(bytes, 0, length);
        }

        @Override
        public ByteBuffer contentAsByteBufferOrNull() {
            // A new writable, array-backed view per call (see toContentStreamProvider).
            return ByteBuffer.wrap(bytes, 0, length);
        }

        @Override
        public String name() {
            return ProviderType.BYTES.getName();
        }
    }
}
