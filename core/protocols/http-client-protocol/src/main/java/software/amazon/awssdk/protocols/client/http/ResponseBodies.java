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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.client.protocol.OwnedResponseBody;
import software.amazon.awssdk.core.client.protocol.ResponseBodyReader;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Reads a buffered HTTP response body into one {@link ByteBuffer} for a {@link Codec} to parse.
 *
 * <p>SDK addition with no smithy-java counterpart (smithy-java reads bodies through {@code DataStream.asByteBuffer()}).
 */
@SdkProtectedApi
public final class ResponseBodies {
    private static final byte[] EMPTY = new byte[0];

    private ResponseBodies() {
    }

    /**
     * Returns the whole body of a response, as {@link #asByteBuffer(ExecutionAttributes, SdkHttpFullResponse, int)}
     * does with a starting capacity of {@link ResponseBodyReader#DEFAULT_UNKNOWN_LENGTH_CAPACITY} (1 KiB) for a body of
     * unknown length.
     *
     * @param context the attributes of the call, used to find the transport's owned body; may be null
     * @param response the response
     * @return the body, positioned at its first byte; an empty buffer if the response has no content
     * @throws UncheckedIOException if reading the content fails
     */
    public static ByteBuffer asByteBuffer(ExecutionAttributes context, SdkHttpFullResponse response) {
        return asByteBuffer(context, response, ResponseBodyReader.DEFAULT_UNKNOWN_LENGTH_CAPACITY);
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
     *     <li>Otherwise the body is read into an array of {@code unknownLengthCapacity} bytes that doubles as needed;
     *     the returned buffer covers the bytes read and is not trimmed.</li>
     * </ul>
     *
     * <p>The response content stream is read to its end but not closed; the caller owns the response. The caller must
     * not modify the returned bytes, which may be shared with the transport.
     *
     * @param context the attributes of the call, used to find the transport's owned body; may be null
     * @param response the response
     * @param unknownLengthCapacity the starting capacity for a body without a valid {@code Content-Length}
     * @return the body, positioned at its first byte; an empty buffer if the response has no content
     * @throws UncheckedIOException if reading the content fails
     * @throws IllegalArgumentException if {@code unknownLengthCapacity} is negative and the body is read from the stream
     */
    public static ByteBuffer asByteBuffer(ExecutionAttributes context, SdkHttpFullResponse response,
                                          int unknownLengthCapacity) {
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
        try {
            return ResponseBodyReader.read(content.get(), ResponseBodyReader.declaredLength(response),
                                           unknownLengthCapacity);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the HTTP response body", e);
        }
    }
}
