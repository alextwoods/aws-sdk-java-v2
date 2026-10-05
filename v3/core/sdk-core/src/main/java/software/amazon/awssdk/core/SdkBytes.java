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
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import software.amazon.awssdk.utils.ToString;

/**
 * An immutable sequence of bytes: the value of a Smithy {@code blob} member of a generated type.
 *
 * <p>Source compatible with the V2 {@code SdkBytes}: the same factories and views, equality and hash code by content,
 * and the same {@code toString}. Every factory except {@link #fromByteArrayUnsafe} copies its input, and every view
 * except {@link #asByteArrayUnsafe} is a copy or read-only, so a value cannot change after it is created unless its
 * creator or reader explicitly opts out of copying.
 */
public final class SdkBytes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] bytes;

    private SdkBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /** The remaining bytes of {@code byteBuffer}, copied; the buffer's position is not changed. */
    public static SdkBytes fromByteBuffer(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "byteBuffer");
        ByteBuffer view = byteBuffer.duplicate();
        byte[] copy = new byte[view.remaining()];
        view.get(copy);
        return new SdkBytes(copy);
    }

    /** A copy of {@code bytes}. */
    public static SdkBytes fromByteArray(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return new SdkBytes(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * {@code bytes} without copying. The caller must not modify the array afterwards, or the value (and every model
     * object holding it) changes.
     */
    public static SdkBytes fromByteArrayUnsafe(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return new SdkBytes(bytes);
    }

    /** {@code string} encoded with {@code charset}. */
    public static SdkBytes fromString(String string, Charset charset) {
        Objects.requireNonNull(string, "string");
        Objects.requireNonNull(charset, "charset");
        return new SdkBytes(string.getBytes(charset));
    }

    /** {@code string} encoded as UTF-8. */
    public static SdkBytes fromUtf8String(String string) {
        return fromString(string, StandardCharsets.UTF_8);
    }

    /**
     * Every remaining byte of {@code inputStream}, which is read to its end but not closed.
     *
     * @throws UncheckedIOException if the stream cannot be read
     */
    public static SdkBytes fromInputStream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try {
            return new SdkBytes(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A read-only buffer over the bytes. */
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
    }

    /** A copy of the bytes. */
    public byte[] asByteArray() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    /** The bytes without copying. The caller must not modify the returned array. */
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

    /** A new stream over the bytes. */
    public InputStream asInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SdkBytes other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    /** {@code SdkBytes(bytes=0x<hex>)}, as in V2. */
    @Override
    public String toString() {
        return ToString.builder("SdkBytes").add("bytes", bytes).build();
    }
}
