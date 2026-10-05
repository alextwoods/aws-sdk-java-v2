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

package software.amazon.awssdk.eventstream;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Encodes {@link Message}s in the AWS event stream binary format:
 *
 * <pre>
 * [total length u32][headers length u32][prelude CRC32 u32][headers][payload][message CRC32 u32]
 * header: [name length u8][name UTF-8][type u8][value]
 * </pre>
 *
 * <p>All integers are big-endian; both CRCs are the JDK {@link CRC32}, the prelude CRC over the first 8 bytes and the
 * message CRC over every preceding byte. Stateless and thread safe.
 */
public final class MessageEncoder {
    /** Prelude (12) plus message CRC (4). */
    public static final int MIN_MESSAGE_LENGTH = 16;
    /** The largest header section. */
    public static final int MAX_HEADERS_LENGTH = 128 * 1024;
    /** The largest payload. */
    public static final int MAX_PAYLOAD_LENGTH = 16 * 1024 * 1024;
    /** The largest header name, in UTF-8 bytes. */
    public static final int MAX_HEADER_NAME_LENGTH = 255;

    private MessageEncoder() {
    }

    /**
     * The encoded form of {@code message}, as a buffer positioned at 0.
     *
     * @throws IllegalArgumentException if the header section exceeds {@link #MAX_HEADERS_LENGTH} or the payload
     *         {@link #MAX_PAYLOAD_LENGTH}
     */
    public static ByteBuffer encode(Message message) {
        Objects.requireNonNull(message, "message");
        int headersLength = headersLength(message.headers());
        if (headersLength > MAX_HEADERS_LENGTH) {
            throw new IllegalArgumentException("The event stream header section is limited to " + MAX_HEADERS_LENGTH
                                               + " bytes, not " + headersLength);
        }
        byte[] payload = message.rawPayload();
        if (payload.length > MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("The event stream payload is limited to " + MAX_PAYLOAD_LENGTH
                                               + " bytes, not " + payload.length);
        }
        int total = MIN_MESSAGE_LENGTH + headersLength + payload.length;
        ByteBuffer out = ByteBuffer.allocate(total);
        out.putInt(total).putInt(headersLength);
        out.putInt((int) crc(out.array(), 0, 8));
        message.headers().forEach((name, value) -> writeHeader(out, name, value));
        out.put(payload);
        out.putInt((int) crc(out.array(), 0, total - 4));
        return out.flip();
    }

    /** The encoded length of {@code message}. */
    public static int encodedLength(Message message) {
        return MIN_MESSAGE_LENGTH + headersLength(message.headers()) + message.payloadLength();
    }

    /**
     * The encoded header section of {@code headers} (in iteration order), exactly as {@link #encode} writes it; the
     * input of event stream signatures, which sign the encoded non-signature headers of a frame.
     *
     * @throws IllegalArgumentException if a header name is empty or longer than {@link #MAX_HEADER_NAME_LENGTH}
     *         UTF-8 bytes, or the section exceeds {@link #MAX_HEADERS_LENGTH}
     */
    public static byte[] encodeHeaders(Map<String, HeaderValue> headers) {
        Objects.requireNonNull(headers, "headers");
        headers.forEach((name, value) -> {
            Objects.requireNonNull(name, "header name");
            Objects.requireNonNull(value, "header value");
            int length = Utf8.length(name);
            if (length == 0 || length > MAX_HEADER_NAME_LENGTH) {
                throw new IllegalArgumentException("An event stream header name must have 1 to "
                                                   + MAX_HEADER_NAME_LENGTH + " UTF-8 bytes, not " + length);
            }
        });
        int headersLength = headersLength(headers);
        if (headersLength > MAX_HEADERS_LENGTH) {
            throw new IllegalArgumentException("The event stream header section is limited to " + MAX_HEADERS_LENGTH
                                               + " bytes, not " + headersLength);
        }
        ByteBuffer out = ByteBuffer.allocate(headersLength);
        headers.forEach((name, value) -> writeHeader(out, name, value));
        return out.array();
    }

    static long crc(byte[] bytes, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return crc.getValue();
    }

    private static int headersLength(Map<String, HeaderValue> headers) {
        int length = 0;
        for (Map.Entry<String, HeaderValue> header : headers.entrySet()) {
            length += 2 + Utf8.length(header.getKey()) + valueLength(header.getValue());
        }
        return length;
    }

    private static int valueLength(HeaderValue value) {
        return switch (value.type()) {
            case BOOLEAN_TRUE, BOOLEAN_FALSE -> 0;
            case BYTE -> 1;
            case SHORT -> 2;
            case INTEGER -> 4;
            case LONG, TIMESTAMP -> 8;
            case BYTE_ARRAY -> 2 + value.rawBytes().length;
            case STRING -> 2 + Utf8.length(value.asString());
            case UUID -> 16;
        };
    }

    private static void writeHeader(ByteBuffer out, String name, HeaderValue value) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        out.put((byte) nameBytes.length).put(nameBytes).put((byte) value.type().code());
        switch (value.type()) {
            case BOOLEAN_TRUE, BOOLEAN_FALSE -> {
            }
            case BYTE -> out.put(value.asByte());
            case SHORT -> out.putShort(value.asShort());
            case INTEGER -> out.putInt(value.asInteger());
            case LONG -> out.putLong(value.asLong());
            case BYTE_ARRAY -> {
                byte[] bytes = value.rawBytes();
                out.putShort((short) bytes.length).put(bytes);
            }
            case STRING -> {
                byte[] bytes = value.asString().getBytes(StandardCharsets.UTF_8);
                out.putShort((short) bytes.length).put(bytes);
            }
            case TIMESTAMP -> out.putLong(value.asTimestamp().toEpochMilli());
            case UUID -> {
                UUID uuid = value.asUuid();
                out.putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
            }
            default -> throw new IllegalStateException("Unknown header type " + value.type());
        }
    }
}
