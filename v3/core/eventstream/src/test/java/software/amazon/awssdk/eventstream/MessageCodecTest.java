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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.eventstream.EventStreamFormatException.Reason;

class MessageCodecTest {
    private static final String SECRET = "s3cr3t-payload-value";

    /** The official restJson1 BooleanHeaderInput vector of smithy-aws-protocol-tests 1.73.0. */
    private static final String BOOLEAN_HEADER_VECTOR =
        "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws";
    /** The official restJson1 ServerErrorInput vector (an exception event with a JSON payload). */
    private static final String EXCEPTION_VECTOR =
        "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxp"
        + "Y2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8=";

    static Message allTypes(byte[] payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put("t", HeaderValue.ofBoolean(true));
        headers.put("f", HeaderValue.ofBoolean(false));
        headers.put("byte", HeaderValue.ofByte((byte) -7));
        headers.put("short", HeaderValue.ofShort((short) -300));
        headers.put("int", HeaderValue.ofInteger(Integer.MIN_VALUE));
        headers.put("long", HeaderValue.ofLong(Long.MAX_VALUE));
        headers.put("bytes", HeaderValue.ofByteArray(new byte[] {0, 1, (byte) 0xFF}));
        headers.put("string", HeaderValue.ofString("h\u00e9llo \u2603"));
        headers.put("time", HeaderValue.ofTimestamp(Instant.ofEpochMilli(1_700_000_000_123L)));
        headers.put("uuid", HeaderValue.ofUuid(new UUID(0x0123456789abcdefL, 0xfedcba9876543210L)));
        return new Message(headers, payload);
    }

    @Test
    void roundTripsEveryHeaderType() {
        Message message = allTypes("payload".getBytes(StandardCharsets.UTF_8));
        ByteBuffer encoded = MessageEncoder.encode(message);
        assertEquals(MessageEncoder.encodedLength(message), encoded.remaining());
        List<Message> decoded = new MessageDecoder().decode(encoded);
        assertEquals(List.of(message), decoded);
        assertEquals(List.copyOf(message.headers().keySet()), List.copyOf(decoded.get(0).headers().keySet()));
    }

    @Test
    void decodesAndReencodesTheOfficialVectorsByteForByte() {
        for (String vector : List.of(BOOLEAN_HEADER_VECTOR, EXCEPTION_VECTOR)) {
            byte[] bytes = Base64.getDecoder().decode(vector);
            List<Message> decoded = new MessageDecoder().decode(ByteBuffer.wrap(bytes));
            assertEquals(1, decoded.size());
            assertArrayEquals(bytes, toArray(MessageEncoder.encode(decoded.get(0))));
        }
        Message event = new MessageDecoder().decode(ByteBuffer.wrap(Base64.getDecoder().decode(BOOLEAN_HEADER_VECTOR)))
                                            .get(0);
        assertEquals("event", event.header(":message-type").asString());
        assertEquals("headers", event.header(":event-type").asString());
        assertTrue(event.header("booleanHeader").asBoolean());
        assertEquals(0, event.payloadLength());
    }

    @Test
    void toleratesArbitraryFragmentationIncludingSingleBytes() {
        Random random = new Random(0x5eed);
        for (int trial = 0; trial < 200; trial++) {
            List<Message> messages = new ArrayList<>();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            int count = 1 + random.nextInt(5);
            for (int i = 0; i < count; i++) {
                byte[] payload = new byte[random.nextInt(300)];
                random.nextBytes(payload);
                Message message = random.nextBoolean() ? allTypes(payload) : new Message(Map.of(), payload);
                messages.add(message);
                stream.writeBytes(toArray(MessageEncoder.encode(message)));
            }
            byte[] bytes = stream.toByteArray();
            boolean singleBytes = trial % 4 == 0;
            MessageDecoder decoder = new MessageDecoder();
            List<Message> decoded = new ArrayList<>();
            int pos = 0;
            int largestMessage = messages.stream().mapToInt(MessageEncoder::encodedLength).max().orElseThrow();
            while (pos < bytes.length) {
                int n = singleBytes ? 1 : Math.min(bytes.length - pos, 1 + random.nextInt(64));
                ByteBuffer chunk = ByteBuffer.wrap(bytes, pos, n);
                while (chunk.hasRemaining()) {
                    Message next = decoder.decodeNext(chunk);
                    if (next != null) {
                        decoded.add(next);
                    }
                    assertTrue(decoder.bufferedBytes() <= largestMessage, "bounded to one message");
                }
                pos += n;
            }
            decoder.endOfStream();
            assertEquals(messages, decoded, "trial " + trial);
        }
    }

    @Test
    void decodeNextReturnsOneMessageAndLeavesTheRestUnread() {
        byte[] one = toArray(MessageEncoder.encode(new Message(Map.of(), new byte[] {1})));
        ByteBuffer both = ByteBuffer.allocate(one.length * 2).put(one).put(one).flip();
        MessageDecoder decoder = new MessageDecoder();
        assertEquals(1, decoder.decodeNext(both).payloadLength());
        assertEquals(one.length, both.remaining());
        assertFalse(decoder.hasPartialMessage());
    }

    @Test
    void rejectsEachMalformedFrameWithItsReason() {
        byte[] valid = toArray(MessageEncoder.encode(allTypes(SECRET.getBytes(StandardCharsets.UTF_8))));

        byte[] preludeCrc = valid.clone();
        preludeCrc[9] ^= 1;
        assertReason(Reason.PRELUDE_CRC_MISMATCH, preludeCrc);

        byte[] messageCrc = valid.clone();
        messageCrc[valid.length - 20] ^= 1;
        assertReason(Reason.MESSAGE_CRC_MISMATCH, messageCrc);

        assertReason(Reason.INVALID_TOTAL_LENGTH, prelude(15, 0));
        assertReason(Reason.INVALID_HEADERS_LENGTH, prelude(32, 17));
        assertReason(Reason.INVALID_HEADERS_LENGTH, prelude(MessageDecoder.DEFAULT_MAX_MESSAGE_SIZE,
                                                            MessageEncoder.MAX_HEADERS_LENGTH + 1));
        assertReason(Reason.INVALID_HEADER_TYPE, frame(new byte[] {1, 'a', 10}));
        assertReason(Reason.INVALID_HEADER_NAME, frame(new byte[] {0, 0}));
        assertReason(Reason.INVALID_HEADER_NAME, frame(new byte[] {1, (byte) 0xC3, 0}));
        assertReason(Reason.INVALID_HEADER_VALUE, frame(new byte[] {1, 'a', 7, 0, 1, (byte) 0xFF}));
        assertReason(Reason.TRUNCATED_HEADER, frame(new byte[] {1, 'a', 4, 0, 0}));
        assertReason(Reason.TRUNCATED_HEADER, frame(new byte[] {1, 'a', 6, 0, 9, 1}));
        assertReason(Reason.TRUNCATED_HEADER, frame(new byte[] {5, 'a'}));
        assertReason(Reason.DUPLICATE_HEADER, frame(new byte[] {1, 'a', 0, 1, 'a', 1}));
    }

    @Test
    void rejectsOversizedMessagesBeforeBufferingThem() {
        MessageDecoder decoder = new MessageDecoder(64);
        byte[] big = toArray(MessageEncoder.encode(new Message(Map.of(), new byte[100])));
        EventStreamFormatException e = assertThrows(EventStreamFormatException.class,
                                                    () -> decoder.decodeNext(ByteBuffer.wrap(big, 0, 12)));
        assertEquals(Reason.MESSAGE_TOO_LARGE, e.reason());
        assertEquals(0, decoder.bufferedBytes());
        assertThrows(IllegalStateException.class, () -> decoder.decodeNext(ByteBuffer.wrap(big, 12, 4)));
        assertThrows(IllegalArgumentException.class, () -> new MessageDecoder(15));
        assertThrows(IllegalArgumentException.class,
                     () -> new MessageDecoder(MessageDecoder.DEFAULT_MAX_MESSAGE_SIZE + 1));
    }

    @Test
    void reportsATruncatedStream() {
        byte[] valid = toArray(MessageEncoder.encode(new Message(Map.of(), new byte[10])));
        MessageDecoder decoder = new MessageDecoder();
        assertNull(decoder.decodeNext(ByteBuffer.wrap(valid, 0, valid.length - 1)));
        EventStreamFormatException e = assertThrows(EventStreamFormatException.class, decoder::endOfStream);
        assertEquals(Reason.TRUNCATED_MESSAGE, e.reason());
    }

    @Test
    void encoderEnforcesTheFormatLimits() {
        assertThrows(IllegalArgumentException.class, () -> new Message(Map.of("", HeaderValue.ofInteger(1)),
                                                                       new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new Message(Map.of("x".repeat(256),
                                                                              HeaderValue.ofInteger(1)),
                                                                       new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> HeaderValue.ofString("x".repeat(32768)));
        assertThrows(IllegalArgumentException.class, () -> HeaderValue.ofByteArray(new byte[32768]));
        assertThrows(IllegalArgumentException.class, () -> MessageEncoder.encode(
            new Message(Map.of(), new byte[MessageEncoder.MAX_PAYLOAD_LENGTH + 1])));
    }

    @Test
    void valuesAreNeverRendered() {
        Message message = new Message(Map.of("h", HeaderValue.ofString(SECRET)), SECRET.getBytes());
        assertFalse(message.toString().contains(SECRET));
        assertFalse(message.header("h").toString().contains(SECRET));
        assertThrows(IllegalStateException.class, () -> message.header("h").asInteger());
    }

    @Test
    void frameTransformersComposeInOrder() {
        FrameTransformer tag = m -> new Message(Map.of("n", HeaderValue.ofInteger(
            m.header("n") == null ? 1 : m.header("n").asInteger() + 1)), m.payloadBytes());
        FrameTransformer closing = new FrameTransformer() {
            @Override
            public Message transform(Message message) {
                return message;
            }

            @Override
            public Message closingFrame() {
                return new Message(Map.of(), new byte[0]);
            }
        };
        FrameTransformer chain = tag.andThen(tag);
        assertEquals(2, chain.transform(new Message(Map.of(), new byte[0])).header("n").asInteger());
        assertNull(chain.closingFrame());
        assertEquals(1, closing.andThen(tag).closingFrame().header("n").asInteger());
        Message m = new Message(Map.of(), new byte[] {1});
        assertEquals(m, FrameTransformer.identity().transform(m));
    }

    private static void assertReason(Reason reason, byte[] bytes) {
        // Whole and byte-by-byte input fail the same way, and the message never quotes values.
        for (boolean singleBytes : new boolean[] {false, true}) {
            MessageDecoder decoder = new MessageDecoder();
            EventStreamFormatException e = assertThrows(EventStreamFormatException.class, () -> {
                if (singleBytes) {
                    for (int i = 0; i < bytes.length; i++) {
                        decoder.decodeNext(ByteBuffer.wrap(bytes, i, 1));
                    }
                } else {
                    decoder.decode(ByteBuffer.wrap(bytes));
                }
            });
            assertEquals(reason, e.reason(), e.getMessage());
            assertFalse(e.getMessage().contains(SECRET), e.getMessage());
            assertThrows(IllegalStateException.class, () -> decoder.decode(ByteBuffer.wrap(bytes)));
        }
    }

    /** A 12-byte prelude with a valid CRC. */
    private static byte[] prelude(int total, int headers) {
        ByteBuffer b = ByteBuffer.allocate(12).putInt(total).putInt(headers);
        b.putInt((int) crc(b.array(), 8));
        return b.array();
    }

    /** A frame of raw header bytes and no payload, with valid CRCs. */
    private static byte[] frame(byte[] headers) {
        int total = 16 + headers.length;
        ByteBuffer b = ByteBuffer.allocate(total).putInt(total).putInt(headers.length);
        b.putInt((int) crc(b.array(), 8)).put(headers);
        b.putInt((int) crc(b.array(), total - 4));
        return b.array();
    }

    private static long crc(byte[] bytes, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, length);
        return crc.getValue();
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }
}
