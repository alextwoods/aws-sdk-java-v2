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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Blob helpers shared by the codecs and HTTP binding: base64 encoding and byte access for {@link ByteBuffer} blob values.
 *
 * <p>No method changes the position, limit or mark of a buffer it is given; each reads the bytes from the buffer's
 * position to its limit.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.io.ByteBufferUtils}.
 */
@SdkProtectedApi
public final class ByteBufferUtils {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    private ByteBufferUtils() {
    }

    /**
     * Base64-encodes the remaining bytes of {@code buffer}.
     *
     * @param buffer the bytes to encode
     * @return the base64 text
     */
    public static String base64Encode(ByteBuffer buffer) {
        byte[] encoded = base64EncodeToBytes(buffer);
        return new String(encoded, StandardCharsets.ISO_8859_1);
    }

    /**
     * Base64-encodes the remaining bytes of {@code buffer} into a new array of ASCII bytes.
     *
     * @param buffer the bytes to encode
     * @return the base64 bytes
     */
    public static byte[] base64EncodeToBytes(ByteBuffer buffer) {
        // Base64.Encoder.encode(ByteBuffer) returns a buffer over a new, exactly sized array.
        return isExact(buffer)
               ? BASE64_ENCODER.encode(buffer.array())
               : BASE64_ENCODER.encode(buffer.duplicate()).array();
    }

    /**
     * Returns the number of bytes {@link #base64EncodeTo} writes for {@code dataLen} bytes of input.
     *
     * @param dataLen the input length
     * @return the encoded length
     */
    public static int base64EncodedSize(int dataLen) {
        return ((dataLen + 2) / 3) * 4;
    }

    /**
     * Base64-encodes the remaining bytes of {@code buffer} into {@code scratch}, starting at index 0.
     *
     * @param buffer the bytes to encode
     * @param scratch where to write; at least {@link #base64EncodedSize} of {@code buffer.remaining()} long
     * @return the number of bytes written
     */
    public static int base64EncodeInto(ByteBuffer buffer, byte[] scratch) {
        if (isExact(buffer)) {
            return BASE64_ENCODER.encode(buffer.array(), scratch);
        }
        byte[] encoded = BASE64_ENCODER.encode(buffer.duplicate()).array();
        System.arraycopy(encoded, 0, scratch, 0, encoded.length);
        return encoded.length;
    }

    /**
     * Base64-encodes the remaining bytes of {@code buffer} into {@code dst}, starting at {@code dstOffset}.
     *
     * @param buffer the bytes to encode
     * @param dst where to write the encoded bytes
     * @param dstOffset the index in {@code dst} of the first encoded byte
     * @param scratch a work array, as for {@link #base64EncodeInto(ByteBuffer, byte[])}
     * @return the number of bytes written
     */
    public static int base64EncodeTo(ByteBuffer buffer, byte[] dst, int dstOffset, byte[] scratch) {
        int written = base64EncodeInto(buffer, scratch);
        System.arraycopy(scratch, 0, dst, dstOffset, written);
        return written;
    }

    /**
     * Decodes the remaining bytes of {@code buffer} as UTF-8.
     *
     * @param buffer the bytes to decode
     * @return the decoded text
     */
    public static String getUTF8String(ByteBuffer buffer) {
        return new String(getBytes(buffer), StandardCharsets.UTF_8);
    }

    /**
     * Returns the remaining bytes of {@code buffer} as an array. If the buffer wraps a whole array from its start, that
     * array is returned without copying, so callers must not modify the result; otherwise the bytes are copied.
     *
     * @param buffer the bytes to return
     * @return the bytes
     */
    public static byte[] getBytes(ByteBuffer buffer) {
        if (isExact(buffer)) {
            return buffer.array();
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.asReadOnlyBuffer().get(bytes);
        return bytes;
    }

    /**
     * Returns an input stream that reads the remaining bytes of {@code buffer}. Unlike the other methods of this class,
     * reading the stream advances the buffer's position.
     *
     * @param buffer the bytes to read
     * @return the input stream
     */
    public static InputStream byteBufferInputStream(ByteBuffer buffer) {
        return new ByteBufferBackedInputStream(buffer);
    }

    private static boolean isExact(ByteBuffer buffer) {
        return buffer.hasArray() && buffer.arrayOffset() == 0 && buffer.remaining() == buffer.array().length;
    }

    // Adapted from jackson-databind's ByteBufferBackedInputStream.
    private static final class ByteBufferBackedInputStream extends InputStream {
        private final ByteBuffer b;

        private ByteBufferBackedInputStream(ByteBuffer buf) {
            b = buf;
        }

        @Override
        public int available() {
            return b.remaining();
        }

        @Override
        public int read() {
            return b.hasRemaining() ? (b.get() & 0xFF) : -1;
        }

        @Override
        public int read(byte[] bytes, int off, int len) {
            if (!b.hasRemaining()) {
                return -1;
            }
            int n = Math.min(len, b.remaining());
            b.get(bytes, off, n);
            return n;
        }
    }
}
