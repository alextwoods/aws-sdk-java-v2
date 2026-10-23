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

package software.amazon.awssdk.core.client.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Reads an HTTP response body stream to its end into one heap array, copying each byte once when the length is known
 * or the caller's starting capacity is large enough.
 *
 * <p>The returned buffer is positioned at 0 and its limit is the number of bytes read. Its array is new and owned by the
 * caller; it may be longer than the body. The stream is read to its end and never closed.
 *
 * <p>SDK addition, shared by {@code ResponseBodies} in {@code http-client-protocol} and the new path's CRC32 validation.
 * smithy-java reads bodies with {@code DataStream.asByteBuffer()}.
 */
@SdkProtectedApi
public final class ResponseBodyReader {
    /**
     * The starting capacity for a body of unknown length when the caller has no better estimate.
     */
    public static final int DEFAULT_UNKNOWN_LENGTH_CAPACITY = 1024;

    /**
     * The longest array the JVM reliably allocates, as in {@code java.util.ArrayList}.
     */
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private ResponseBodyReader() {
    }

    /**
     * Returns the declared length of a response body.
     *
     * @param response the response
     * @return the first {@code Content-Length} value, trimmed and parsed; or -1 if there is none or it is not a number.
     *     A negative value is returned as parsed, and {@link #read} treats it as unknown
     */
    public static long declaredLength(SdkHttpResponse response) {
        Optional<String> header = response.firstMatchingHeader("Content-Length");
        if (!header.isPresent()) {
            return -1;
        }
        try {
            return Long.parseLong(header.get().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Reads a stream to its end.
     *
     * <ul>
     *     <li>If {@code declaredLength} is between 0 and the maximum array length, the body is read into an array of
     *     exactly that length. A body that ends early is returned as read; one that is longer continues in a growable
     *     array.</li>
     *     <li>Otherwise the body is read into an array of {@code unknownLengthCapacity} bytes that doubles (to at least
     *     1 KiB) as needed. The returned buffer covers the bytes read and is not trimmed.</li>
     * </ul>
     *
     * @param in the stream, which is not closed
     * @param declaredLength the declared body length, or a negative value if it is unknown
     * @param unknownLengthCapacity the starting capacity when the length is unknown
     * @return a heap buffer over a new array, positioned at the first byte of the body
     * @throws IOException if reading fails
     * @throws IllegalArgumentException if {@code unknownLengthCapacity} is negative
     * @throws OutOfMemoryError if the body is longer than the maximum array length
     */
    public static ByteBuffer read(InputStream in, long declaredLength, int unknownLengthCapacity) throws IOException {
        Validate.isNotNegative(unknownLengthCapacity, "unknownLengthCapacity");
        if (declaredLength >= 0 && declaredLength <= MAX_ARRAY_LENGTH) {
            return readKnownLength(in, (int) declaredLength);
        }
        return readRemaining(in, new byte[unknownLengthCapacity], 0);
    }

    private static ByteBuffer readKnownLength(InputStream in, int length) throws IOException {
        byte[] bytes = new byte[length];
        int read = readFully(in, bytes, 0);
        if (read < length) {
            return ByteBuffer.wrap(bytes, 0, read);
        }
        int next = in.read();
        if (next < 0) {
            return ByteBuffer.wrap(bytes);
        }
        // The body is longer than its Content-Length (for example, the client decoded a content encoding).
        byte[] grown = Arrays.copyOf(bytes, grownLength(length, length + 1));
        grown[length] = (byte) next;
        return readRemaining(in, grown, length + 1);
    }

    /**
     * Reads the rest of {@code in} into {@code bytes}, after the first {@code count} bytes, growing the array as needed.
     */
    private static ByteBuffer readRemaining(InputStream in, byte[] bytes, int count) throws IOException {
        while (true) {
            if (count == bytes.length) {
                int next = in.read();
                if (next < 0) {
                    break;
                }
                bytes = Arrays.copyOf(bytes, grownLength(bytes.length, count + 1));
                bytes[count++] = (byte) next;
            }
            count = readFully(in, bytes, count);
            if (count < bytes.length) {
                break;
            }
        }
        return ByteBuffer.wrap(bytes, 0, count);
    }

    /**
     * Fills {@code bytes} from {@code offset} until it is full or {@code in} ends.
     *
     * @return the index after the last byte read
     */
    private static int readFully(InputStream in, byte[] bytes, int offset) throws IOException {
        int count = offset;
        while (count < bytes.length) {
            int read = in.read(bytes, count, bytes.length - count);
            if (read < 0) {
                break;
            }
            count += read;
        }
        return count;
    }

    private static int grownLength(int length, int minLength) {
        if (minLength < 0 || minLength > MAX_ARRAY_LENGTH) {
            throw new OutOfMemoryError("The HTTP response body is larger than the maximum array length");
        }
        long doubled = Math.max((long) length * 2, DEFAULT_UNKNOWN_LENGTH_CAPACITY);
        return (int) Math.min(Math.max(doubled, minLength), MAX_ARRAY_LENGTH);
    }
}
