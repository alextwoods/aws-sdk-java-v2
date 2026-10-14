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

package software.amazon.awssdk.protocols.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.transport.OwnedResponseBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Reads a buffered HTTP response body into one {@link ByteBuffer} for a {@link Codec} to parse.
 *
 * <p>SDK addition with no smithy-java counterpart (smithy-java reads bodies through {@code DataStream.asByteBuffer()}).
 */
@SdkProtectedApi
public final class ResponseBodies {
    /**
     * The longest array the JVM reliably allocates, as in {@code java.util.ArrayList}.
     */
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    /**
     * Initial capacity when the response has no usable {@code Content-Length}.
     */
    private static final int UNKNOWN_LENGTH_INITIAL_CAPACITY = 1024;

    private static final byte[] EMPTY = new byte[0];

    private ResponseBodies() {
    }

    /**
     * Returns the whole body of a response as a buffer with an accessible backing array, copying it at most once.
     *
     * <ul>
     *     <li>If the asynchronous transport aggregated the body into an array it owns and the response still carries
     *     that unread body, the buffer wraps that array, with no copy.</li>
     *     <li>Otherwise, if the response has a valid {@code Content-Length}, the body is read into an array of exactly
     *     that length. A body that ends early is returned as read; one that is longer continues in a growable
     *     array.</li>
     *     <li>Otherwise the body is read into an array that doubles as needed; the returned buffer covers the bytes read
     *     and is not trimmed.</li>
     * </ul>
     *
     * <p>The response content stream is read to its end but not closed; the caller owns the response. The caller must
     * not modify the returned bytes, which may be shared with the transport.
     *
     * @param context the attributes of the call, used to find the transport's owned body; may be null
     * @param response the response
     * @return the body, positioned at its first byte; an empty buffer if the response has no content
     * @throws UncheckedIOException if reading the content fails
     */
    public static ByteBuffer asByteBuffer(ExecutionAttributes context, SdkHttpFullResponse response) {
        if (context != null) {
            ByteBuffer owned = OwnedResponseBody.ownedBuffer(context, response);
            if (owned != null) {
                return owned;
            }
        }
        Optional<AbortableInputStream> content = response.content();
        if (!content.isPresent()) {
            return ByteBuffer.wrap(EMPTY);
        }
        long contentLength = contentLength(response);
        try {
            InputStream in = content.get();
            if (contentLength >= 0 && contentLength <= MAX_ARRAY_LENGTH) {
                return readKnownLength(in, (int) contentLength);
            }
            return readRemaining(in, new byte[UNKNOWN_LENGTH_INITIAL_CAPACITY], 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the HTTP response body", e);
        }
    }

    private static long contentLength(SdkHttpFullResponse response) {
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
        long doubled = Math.max((long) length * 2, UNKNOWN_LENGTH_INITIAL_CAPACITY);
        return (int) Math.min(Math.max(doubled, minLength), MAX_ARRAY_LENGTH);
    }
}
