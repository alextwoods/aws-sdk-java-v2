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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * An incremental decoder of the AWS event stream binary format (see {@link MessageEncoder}) that accepts input split
 * at arbitrary points, down to single bytes.
 *
 * <p>Bounded buffering: the decoder holds at most the bytes of one partial message, never more than
 * {@link #maxMessageSize()}. {@link #decodeNext} returns at most one message and leaves the rest of its input unread,
 * so a demand-driven caller holds at most one input chunk and one partial message; {@link #decode} is the eager
 * convenience that returns every complete message of a chunk.
 *
 * <p>Validation order: the prelude CRC is checked before its lengths are trusted, then the total length (minimum,
 * maximum message size), then the headers length; the message CRC is checked before headers are parsed. The first
 * violation throws an {@link EventStreamFormatException} and fails the decoder permanently (every later call throws
 * {@link IllegalStateException}). Failure messages never contain header or payload values.
 *
 * <p>Not thread safe; one instance decodes one stream.
 */
public final class MessageDecoder {
    /** The default and largest {@link #maxMessageSize()}: the largest well-formed message. */
    public static final int DEFAULT_MAX_MESSAGE_SIZE = MessageEncoder.MIN_MESSAGE_LENGTH
                                                       + MessageEncoder.MAX_HEADERS_LENGTH
                                                       + MessageEncoder.MAX_PAYLOAD_LENGTH;
    private static final int PRELUDE_LENGTH = 12;

    private final int maxMessageSize;
    private final byte[] prelude = new byte[PRELUDE_LENGTH];
    private int preludeFill;
    private byte[] message;
    private int messageFill;
    private long offset;
    private boolean failed;

    /** A decoder with {@link #DEFAULT_MAX_MESSAGE_SIZE}. */
    public MessageDecoder() {
        this(DEFAULT_MAX_MESSAGE_SIZE);
    }

    /**
     * A decoder that rejects messages longer than {@code maxMessageSize} bytes before buffering them.
     *
     * @throws IllegalArgumentException if {@code maxMessageSize} is outside 16..{@link #DEFAULT_MAX_MESSAGE_SIZE}
     */
    public MessageDecoder(int maxMessageSize) {
        if (maxMessageSize < MessageEncoder.MIN_MESSAGE_LENGTH || maxMessageSize > DEFAULT_MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException("maxMessageSize must be within " + MessageEncoder.MIN_MESSAGE_LENGTH
                                               + ".." + DEFAULT_MAX_MESSAGE_SIZE + ", not " + maxMessageSize);
        }
        this.maxMessageSize = maxMessageSize;
    }

    /** The largest message this decoder accepts, in bytes. */
    public int maxMessageSize() {
        return maxMessageSize;
    }

    /** The bytes of the partial message currently buffered; at most {@link #maxMessageSize()}. */
    public int bufferedBytes() {
        return message != null ? messageFill : preludeFill;
    }

    /** True if part of a message has been read but not yet completed. */
    public boolean hasPartialMessage() {
        return bufferedBytes() > 0;
    }

    /**
     * Reads {@code input} up to the end of the next message and returns it, or reads all of {@code input} and returns
     * {@code null} if no message completed. Bytes after the returned message stay unread in {@code input}.
     *
     * @throws EventStreamFormatException if the input is malformed
     */
    public Message decodeNext(ByteBuffer input) {
        checkUsable();
        try {
            while (input.hasRemaining()) {
                if (message == null) {
                    int n = Math.min(PRELUDE_LENGTH - preludeFill, input.remaining());
                    input.get(prelude, preludeFill, n);
                    preludeFill += n;
                    if (preludeFill < PRELUDE_LENGTH) {
                        return null;
                    }
                    startMessage();
                } else {
                    int n = Math.min(message.length - messageFill, input.remaining());
                    input.get(message, messageFill, n);
                    messageFill += n;
                }
                if (message.length == messageFill) {
                    return completeMessage();
                }
            }
            return null;
        } catch (EventStreamFormatException e) {
            failed = true;
            release();
            throw e;
        }
    }

    /**
     * Every message completed by {@code input}, in order; a trailing partial message stays buffered.
     *
     * @throws EventStreamFormatException if the input is malformed
     */
    public List<Message> decode(ByteBuffer input) {
        List<Message> out = new ArrayList<>(1);
        while (input.hasRemaining()) {
            Message next = decodeNext(input);
            if (next != null) {
                out.add(next);
            }
        }
        return out;
    }

    /**
     * Signals the end of the input.
     *
     * @throws EventStreamFormatException with {@link EventStreamFormatException.Reason#TRUNCATED_MESSAGE} if a partial
     *         message is buffered
     */
    public void endOfStream() {
        checkUsable();
        if (hasPartialMessage()) {
            failed = true;
            int buffered = bufferedBytes();
            String expected = message != null ? " of " + message.length : "";
            release();
            throw EventStreamFormatException.create(EventStreamFormatException.Reason.TRUNCATED_MESSAGE,
                                                    "the stream ended after " + buffered + expected
                                                    + " bytes of the message at stream offset " + offset);
        }
    }

    /** Drops the buffered partial message. */
    private void release() {
        message = null;
        messageFill = 0;
        preludeFill = 0;
    }

    private void checkUsable() {
        if (failed) {
            throw new IllegalStateException("The event stream decoder failed on malformed input and cannot continue");
        }
    }

    private void startMessage() {
        ByteBuffer p = ByteBuffer.wrap(prelude);
        long total = Integer.toUnsignedLong(p.getInt(0));
        long headers = Integer.toUnsignedLong(p.getInt(4));
        long expectedCrc = Integer.toUnsignedLong(p.getInt(8));
        long crc = MessageEncoder.crc(prelude, 0, 8);
        if (crc != expectedCrc) {
            throw fail(EventStreamFormatException.Reason.PRELUDE_CRC_MISMATCH,
                       String.format("prelude CRC 0x%08x does not match the computed 0x%08x", expectedCrc, crc));
        }
        if (total < MessageEncoder.MIN_MESSAGE_LENGTH) {
            throw fail(EventStreamFormatException.Reason.INVALID_TOTAL_LENGTH,
                       "total length " + total + " is below the minimum " + MessageEncoder.MIN_MESSAGE_LENGTH);
        }
        if (total > maxMessageSize) {
            throw fail(EventStreamFormatException.Reason.MESSAGE_TOO_LARGE,
                       "total length " + total + " exceeds the maximum message size " + maxMessageSize);
        }
        if (headers > MessageEncoder.MAX_HEADERS_LENGTH || headers > total - MessageEncoder.MIN_MESSAGE_LENGTH) {
            throw fail(EventStreamFormatException.Reason.INVALID_HEADERS_LENGTH,
                       "headers length " + headers + " does not fit total length " + total
                       + " (header limit " + MessageEncoder.MAX_HEADERS_LENGTH + ")");
        }
        message = new byte[(int) total];
        System.arraycopy(prelude, 0, message, 0, PRELUDE_LENGTH);
        messageFill = PRELUDE_LENGTH;
    }

    private Message completeMessage() {
        byte[] bytes = message;
        int total = bytes.length;
        ByteBuffer in = ByteBuffer.wrap(bytes);
        long expectedCrc = Integer.toUnsignedLong(in.getInt(total - 4));
        long crc = MessageEncoder.crc(bytes, 0, total - 4);
        if (crc != expectedCrc) {
            throw fail(EventStreamFormatException.Reason.MESSAGE_CRC_MISMATCH,
                       String.format("message CRC 0x%08x does not match the computed 0x%08x", expectedCrc, crc));
        }
        int headersEnd = PRELUDE_LENGTH + in.getInt(4);
        LinkedHashMap<String, HeaderValue> headers = new LinkedHashMap<>();
        int pos = PRELUDE_LENGTH;
        int index = 0;
        while (pos < headersEnd) {
            pos = readHeader(bytes, pos, headersEnd, index++, headers);
        }
        byte[] payload = new byte[total - 4 - headersEnd];
        System.arraycopy(bytes, headersEnd, payload, 0, payload.length);
        offset += total;
        message = null;
        messageFill = 0;
        preludeFill = 0;
        return Message.wrap(headers, payload);
    }

    private int readHeader(byte[] bytes, int pos, int end, int index, LinkedHashMap<String, HeaderValue> headers) {
        int nameLength = bytes[pos] & 0xFF;
        if (nameLength == 0) {
            throw fail(EventStreamFormatException.Reason.INVALID_HEADER_NAME,
                       "header " + index + " has an empty name");
        }
        require(pos + 1 + nameLength + 1, end, index);
        String name = Utf8.decode(bytes, pos + 1, nameLength);
        if (name == null) {
            throw fail(EventStreamFormatException.Reason.INVALID_HEADER_NAME,
                       "the name of header " + index + " is not valid UTF-8");
        }
        pos += 1 + nameLength;
        int code = bytes[pos++] & 0xFF;
        HeaderType type = HeaderType.fromCode(code);
        if (type == null) {
            throw fail(EventStreamFormatException.Reason.INVALID_HEADER_TYPE,
                       "header " + index + " has the unknown type code " + code);
        }
        ByteBuffer in = ByteBuffer.wrap(bytes);
        HeaderValue value;
        switch (type) {
            case BOOLEAN_TRUE -> value = HeaderValue.ofBoolean(true);
            case BOOLEAN_FALSE -> value = HeaderValue.ofBoolean(false);
            case BYTE -> {
                require(pos + 1, end, index);
                value = HeaderValue.ofByte(bytes[pos]);
                pos += 1;
            }
            case SHORT -> {
                require(pos + 2, end, index);
                value = HeaderValue.ofShort(in.getShort(pos));
                pos += 2;
            }
            case INTEGER -> {
                require(pos + 4, end, index);
                value = HeaderValue.ofInteger(in.getInt(pos));
                pos += 4;
            }
            case LONG -> {
                require(pos + 8, end, index);
                value = HeaderValue.ofLong(in.getLong(pos));
                pos += 8;
            }
            case TIMESTAMP -> {
                require(pos + 8, end, index);
                value = HeaderValue.ofTimestamp(Instant.ofEpochMilli(in.getLong(pos)));
                pos += 8;
            }
            case UUID -> {
                require(pos + 16, end, index);
                value = HeaderValue.ofUuid(new UUID(in.getLong(pos), in.getLong(pos + 8)));
                pos += 16;
            }
            case BYTE_ARRAY, STRING -> {
                require(pos + 2, end, index);
                int length = in.getShort(pos) & 0xFFFF;
                pos += 2;
                require(pos + length, end, index);
                if (type == HeaderType.BYTE_ARRAY) {
                    byte[] data = new byte[length];
                    System.arraycopy(bytes, pos, data, 0, length);
                    value = HeaderValue.wrapByteArray(data);
                } else {
                    String text = Utf8.decode(bytes, pos, length);
                    if (text == null) {
                        throw fail(EventStreamFormatException.Reason.INVALID_HEADER_VALUE,
                                   "the string value of header " + index + " is not valid UTF-8");
                    }
                    value = HeaderValue.ofString(text);
                }
                pos += length;
            }
            default -> throw new IllegalStateException("Unknown header type " + type);
        }
        if (headers.putIfAbsent(name, value) != null) {
            throw fail(EventStreamFormatException.Reason.DUPLICATE_HEADER,
                       "header " + index + " repeats the name of an earlier header");
        }
        return pos;
    }

    private void require(int needed, int end, int index) {
        if (needed > end) {
            throw fail(EventStreamFormatException.Reason.TRUNCATED_HEADER,
                       "header " + index + " runs past the end of the header section");
        }
    }

    private EventStreamFormatException fail(EventStreamFormatException.Reason reason, String detail) {
        return EventStreamFormatException.create(reason, detail + " (message at stream offset " + offset + ")");
    }
}
