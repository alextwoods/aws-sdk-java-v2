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

package software.amazon.awssdk.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * A streaming response read fully into memory together with its unmarshalled response, as returned by the
 * {@code toBytes()} response transformers. Accessor names match V2 ({@code response}, {@code asByteArray},
 * {@code asByteArrayUnsafe}, {@code asByteBuffer}, {@code asString}, {@code asUtf8String}, {@code asInputStream}).
 *
 * <p>ADAPTED from V2: V3 has no shared {@code BytesWrapper} supertype (and so no {@code asContentStreamProvider}); the
 * byte accessors are declared here directly.
 *
 * @param <ResponseT> the unmarshalled response type
 */
public final class ResponseBytes<ResponseT> {
    private final ResponseT response;
    private final byte[] bytes;

    private ResponseBytes(ResponseT response, byte[] bytes) {
        this.response = Objects.requireNonNull(response, "response");
        this.bytes = Objects.requireNonNull(bytes, "bytes");
    }

    /**
     * The bytes of {@code stream}, read to its end; the stream is not closed.
     *
     * @throws UncheckedIOException if reading fails
     */
    public static <ResponseT> ResponseBytes<ResponseT> fromInputStream(ResponseT response, InputStream stream) {
        try {
            return new ResponseBytes<>(response, stream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A copy of {@code bytes}. */
    public static <ResponseT> ResponseBytes<ResponseT> fromByteArray(ResponseT response, byte[] bytes) {
        return new ResponseBytes<>(response, bytes.clone());
    }

    /** {@code bytes} without copying; the caller must not modify the array afterwards. */
    public static <ResponseT> ResponseBytes<ResponseT> fromByteArrayUnsafe(ResponseT response, byte[] bytes) {
        return new ResponseBytes<>(response, bytes);
    }

    /** The unmarshalled response. */
    public ResponseT response() {
        return response;
    }

    /** A read-only view of the bytes. */
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
    }

    /** A copy of the bytes. */
    public byte[] asByteArray() {
        return bytes.clone();
    }

    /** The bytes without copying; the caller must not modify them. */
    public byte[] asByteArrayUnsafe() {
        return bytes;
    }

    /** The bytes decoded with {@code charset}. */
    public String asString(Charset charset) {
        return new String(bytes, charset);
    }

    /** The bytes decoded as UTF-8. */
    public String asUtf8String() {
        return asString(StandardCharsets.UTF_8);
    }

    /** A new stream of the bytes. */
    public InputStream asInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ResponseBytes<?> other && response.equals(other.response)
                            && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return 31 * response.hashCode() + Arrays.hashCode(bytes);
    }

    /** Renders the length only; content may be sensitive. */
    @Override
    public String toString() {
        return "ResponseBytes(" + bytes.length + " bytes, response=" + response + ")";
    }
}
