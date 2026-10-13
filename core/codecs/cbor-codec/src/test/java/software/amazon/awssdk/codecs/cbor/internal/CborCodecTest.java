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

package software.amazon.awssdk.codecs.cbor.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.cbor.CborCodecTest}. Static fixtures of the nested classes
 * live in this outer class, because Java 8 does not allow static fields in inner classes.
 */
class CborCodecTest {

    private static final CborSettings SETTINGS = CborSettings.defaultSettings();
    private static final DefaultCborSerdeProvider CODEC = new DefaultCborSerdeProvider();

    private static final Schema LOOKUP_STRUCT = Schema.structureBuilder(ShapeId.from("smithy.test#TestStruct"))
                                                      .putMember("alpha", PreludeSchemas.STRING)
                                                      .putMember("beta", PreludeSchemas.INTEGER)
                                                      .putMember("gamma", PreludeSchemas.BOOLEAN)
                                                      .build();

    private static final Schema EXT_STRUCT = Schema.structureBuilder(ShapeId.from("smithy.test#ExtStruct"))
                                                   .putMember("foo", PreludeSchemas.STRING)
                                                   .putMember("bar", PreludeSchemas.INTEGER)
                                                   .build();

    // CBOR major type 0 (unsigned int) value 1 -> 0x01.
    private static final byte[] POS_INT_ONE = {0x01};
    // CBOR major type 0 value 0 -> 0x00.
    private static final byte[] POS_INT_ZERO = {0x00};
    // CBOR major type 1 (negative int) value -1 -> 0x20.
    private static final byte[] NEG_INT_ONE = {0x20};
    // CBOR uint8 value 42 -> 0x18 0x2A.
    private static final byte[] POS_INT_FORTY_TWO = {0x18, 0x2A};
    // CBOR uint64 Long.MAX_VALUE -> 0x1B 7FFFFFFFFFFFFFFF (largest value that still fits a long).
    private static final byte[] POS_INT_LONG_MAX = {0x1B, 0x7F, -1, -1, -1, -1, -1, -1, -1};
    // CBOR uint64 2^64-1 -> 0x1B FFFFFFFFFFFFFFFF (overflows long: reads back as -1).
    private static final byte[] POS_INT_OVERFLOW = {0x1B, -1, -1, -1, -1, -1, -1, -1, -1};
    // CBOR negint64 -2^64 -> 0x3B FFFFFFFFFFFFFFFF (overflows long: -val-1 reads back positive).
    private static final byte[] NEG_INT_OVERFLOW = {0x3B, -1, -1, -1, -1, -1, -1, -1, -1};

    private static ByteBuffer roundTrip(SerializableStruct shape) {
        return CODEC.serialize(shape, SETTINGS);
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    @Nested
    class SerializerPoolTest {
        @Test
        void acquireAndReleaseReturnsToPool() {
            CborSerializer s1 = CborSerializer.acquire();
            CborSerializer.release(s1);

            CborSerializer s2 = CborSerializer.acquire();
            // Pool may or may not return the same instance depending on thread, but it should not throw.
            assertNotNull(s2);
            CborSerializer.release(s2);
        }

        @Test
        void acquireProducesCleanState() {
            CborSerializer s = CborSerializer.acquire();
            assertEquals(0, s.pos);
            CborSerializer.release(s);
        }

        @Test
        void largeBufferIsDownsizedOnRelease() {
            CborSerializer s = CborSerializer.acquire();
            s.buf = new byte[1024 * 1024];
            CborSerializer.release(s);

            CborSerializer s2 = CborSerializer.acquire();
            // The same pooled instance has a downsized buffer; a new instance has the default size.
            assertNotNull(s2);
            assertTrue(s2.buf.length < 1024 * 1024);
            CborSerializer.release(s2);
        }

        @Test
        void streamingSerializerNotPooled() {
            CborSerializer s = new CborSerializer(new ByteArrayOutputStream());
            CborSerializer.release(s);
            // Should not throw, and the streaming serializer should not be returned from acquire
            CborSerializer s2 = CborSerializer.acquire();
            assertNotSame(s, s2);
            CborSerializer.release(s2);
        }
    }

    @Nested
    class Utf8EncodingTest {
        @ParameterizedTest
        @ValueSource(strings = {"hello", "ASCII only 12345!@#", "", "a"})
        void asciiStrings(String input) {
            byte[] result = serializeString(input);
            ShapeDeserializer de = CODEC.newDeserializer(result, SETTINGS);
            assertEquals(input, de.readString(PreludeSchemas.STRING));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "\u00e9", // e-acute (2-byte UTF-8)
            "\u00fc\u00f6", // u-umlaut, o-umlaut
            "\u4e16\u754c", // Chinese: "world"
            "caf\u00e9", // mixed ASCII + 2-byte
            "\ud83d\ude00", // emoji (surrogate pair, 4-byte UTF-8)
            "a\ud83d\ude00z" // ASCII + emoji + ASCII
        })
        void nonAsciiStrings(String input) {
            byte[] result = serializeString(input);
            ShapeDeserializer de = CODEC.newDeserializer(result, SETTINGS);
            assertEquals(input, de.readString(PreludeSchemas.STRING));
        }

        @Test
        void unpairedHighSurrogate() {
            String broken = "a\uD800b"; // unpaired high surrogate
            byte[] result = serializeString(broken);
            ShapeDeserializer de = CODEC.newDeserializer(result, SETTINGS);
            assertEquals("a\uFFFDb", de.readString(PreludeSchemas.STRING)); // replacement character
        }

        @Test
        void unpairedLowSurrogate() {
            String broken = "a\uDC00b"; // unpaired low surrogate
            byte[] result = serializeString(broken);
            ShapeDeserializer de = CODEC.newDeserializer(result, SETTINGS);
            assertEquals("a\uFFFDb", de.readString(PreludeSchemas.STRING)); // replacement character
        }

        private byte[] serializeString(String value) {
            CborSerializer s = CborSerializer.acquire();
            try {
                s.writeString(PreludeSchemas.STRING, value);
                return Arrays.copyOf(s.buf, s.pos);
            } finally {
                CborSerializer.release(s);
            }
        }
    }

    @Nested
    class CollectionDepthTest {
        @Test
        void deeplyNestedCollections() {
            int depth = 100;
            CborSerializer s = CborSerializer.acquire();
            try {
                for (int i = 0; i < depth; i++) {
                    s.writeList(PreludeSchemas.DOCUMENT, null, -1, (ignored, inner) -> {
                    });
                }
            } finally {
                CborSerializer.release(s);
            }
        }

        @Test
        void collectionDepthExceeds64() {
            int depth = 70;
            CborSerializer s = CborSerializer.acquire();
            try {
                for (int i = 0; i < depth; i++) {
                    boolean indefinite = i % 2 == 0;
                    s.writeList(PreludeSchemas.DOCUMENT, null, indefinite ? -1 : 1, (ignored, inner) -> {
                    });
                }
            } finally {
                CborSerializer.release(s);
            }
        }
    }

    @Nested
    class MemberLookupTest {
        @Test
        void lookupInDefinitionOrder() {
            CborMemberLookup lookup = new CborMemberLookup(LOOKUP_STRUCT.members());
            byte[] alpha = "alpha".getBytes(StandardCharsets.UTF_8);
            byte[] beta = "beta".getBytes(StandardCharsets.UTF_8);
            byte[] gamma = "gamma".getBytes(StandardCharsets.UTF_8);

            Schema found = lookup.lookup(alpha, 0, alpha.length, 0);
            assertEquals("alpha", found.memberName());

            found = lookup.lookup(beta, 0, beta.length, 1);
            assertEquals("beta", found.memberName());

            found = lookup.lookup(gamma, 0, gamma.length, 2);
            assertEquals("gamma", found.memberName());
        }

        @Test
        void lookupOutOfOrder() {
            CborMemberLookup lookup = new CborMemberLookup(LOOKUP_STRUCT.members());
            byte[] gamma = "gamma".getBytes(StandardCharsets.UTF_8);

            Schema found = lookup.lookup(gamma, 0, gamma.length, 0);
            assertEquals("gamma", found.memberName());
        }

        @Test
        void lookupDisabledSpeculative() {
            CborMemberLookup lookup = new CborMemberLookup(LOOKUP_STRUCT.members());
            byte[] beta = "beta".getBytes(StandardCharsets.UTF_8);

            Schema found = lookup.lookup(beta, 0, beta.length, -1);
            assertEquals("beta", found.memberName());
        }

        @Test
        void lookupUnknownMemberReturnsNull() {
            CborMemberLookup lookup = new CborMemberLookup(LOOKUP_STRUCT.members());
            byte[] unknown = "unknown".getBytes(StandardCharsets.UTF_8);

            Schema found = lookup.lookup(unknown, 0, unknown.length, 0);
            assertNull(found);
        }

        @Test
        void lookupWithOffset() {
            CborMemberLookup lookup = new CborMemberLookup(LOOKUP_STRUCT.members());
            byte[] padded = "XXalphaYY".getBytes(StandardCharsets.UTF_8);

            Schema found = lookup.lookup(padded, 2, 7, -1);
            assertEquals("alpha", found.memberName());
        }

        @Test
        void emptyMemberList() {
            CborMemberLookup lookup = new CborMemberLookup(Collections.<Schema>emptyList());
            byte[] any = "any".getBytes(StandardCharsets.UTF_8);

            Schema found = lookup.lookup(any, 0, any.length, -1);
            assertNull(found);
        }
    }

    @Nested
    class SchemaExtensionsTest {
        @Test
        void memberExtensionHasEncodedNameBytes() {
            CborSchemaExtensions provider = new CborSchemaExtensions();
            CborSchemaExtensions.CborExtension ext = provider.provide(EXT_STRUCT.member("foo"));

            assertNotNull(ext);
            assertNotNull(ext.memberNameBytes());
            assertNull(ext.memberLookup());
            assertNull(ext.fieldNameTable());

            byte[] expected = CborSerializer.encodeMemberName("foo");
            assertArrayEquals(expected, ext.memberNameBytes());
        }

        @Test
        void structExtensionHasLookupAndFieldNameTable() {
            CborSchemaExtensions provider = new CborSchemaExtensions();
            CborSchemaExtensions.CborExtension ext = provider.provide(EXT_STRUCT);

            assertNotNull(ext);
            assertNull(ext.memberNameBytes());
            assertNotNull(ext.memberLookup());
            assertNotNull(ext.fieldNameTable());

            assertEquals(2, ext.fieldNameTable().length);
            assertNotNull(ext.fieldNameTable()[0]);
            assertNotNull(ext.fieldNameTable()[1]);
        }

        @Test
        void nonStructNonMemberReturnsNull() {
            CborSchemaExtensions provider = new CborSchemaExtensions();
            CborSchemaExtensions.CborExtension ext = provider.provide(PreludeSchemas.STRING);
            assertNull(ext);
        }

        @Test
        void emptyStructReturnsEmptyExtension() {
            Schema emptyStruct = Schema.structureBuilder(ShapeId.from("smithy.test#Empty")).build();
            CborSchemaExtensions provider = new CborSchemaExtensions();
            CborSchemaExtensions.CborExtension ext = provider.provide(emptyStruct);

            assertNotNull(ext);
            assertNull(ext.memberNameBytes());
            assertNull(ext.memberLookup());
            assertNull(ext.fieldNameTable());
        }
    }

    @Nested
    class EncodeMemberNameTest {
        @Test
        void shortName() {
            byte[] result = CborSerializer.encodeMemberName("id");
            // CBOR text string header for length 2: 0x62, then 'i', 'd'
            assertEquals(3, result.length);
            assertEquals(0x62, result[0] & 0xFF);
            assertEquals('i', result[1]);
            assertEquals('d', result[2]);
        }

        @Test
        void longerName() {
            String name = repeat('a', 200);
            byte[] result = CborSerializer.encodeMemberName(name);
            // Length 200 needs 1-byte arg: header = 0x78 (text string, 1-byte length), 0xC8 (200)
            assertEquals(202, result.length);
            assertEquals(0x78, result[0] & 0xFF);
            assertEquals(200, result[1] & 0xFF);
        }
    }

    @Nested
    class StructRoundTripTest {
        @Test
        void serializeDeserializeInOrder() {
            CborTestData.Bird bird = new CborTestData.BirdBuilder()
                .name("falcon")
                .flightRange(BigInteger.valueOf(42))
                .build();

            ByteBuffer ser = roundTrip(bird);
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();

            assertEquals("falcon", de.name);
            assertEquals(BigInteger.valueOf(42), de.flightRange);
        }

        @Test
        void serializeDeserializeAllFields() {
            CborTestData.Bird bird = new CborTestData.BirdBuilder()
                .name("hawk")
                .bytes(ByteBuffer.wrap(new byte[] {1, 2, 3}))
                .lastSquawkAt(Instant.ofEpochSecond(1000))
                .flightRange(BigInteger.TEN)
                .wingspan(new BigDecimal("3.14"))
                .build();

            ByteBuffer ser = roundTrip(bird);
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();

            assertEquals("hawk", de.name);
            assertEquals(Instant.ofEpochSecond(1000), de.lastSquawkAt);
            assertEquals(BigInteger.TEN, de.flightRange);
            assertEquals(new BigDecimal("3.14"), de.wingspan);
        }

        // Regression: the original readTimestamp passed the tagged token (EPOCH_IPOS=16) to readLong,
        // which rejected any token > NEG_INT(1), so integer epoch timestamps always threw.
        @Test
        void timestampIntegerEpoch() {
            // tag(1) + uint8(100) - simplest valid integer epoch
            byte[] payload = {(byte) 0xC1, 0x18, 0x64};
            ShapeDeserializer de = CODEC.newDeserializer(payload, SETTINGS);
            assertEquals(Instant.ofEpochSecond(100), de.readTimestamp(PreludeSchemas.TIMESTAMP));
        }

        @Test
        void timestampNegativeIntegerEpoch() {
            // tag(1) + negint8(99) = -1 - 99 = -100 seconds
            byte[] payload = {(byte) 0xC1, 0x38, 0x63};
            ShapeDeserializer de = CODEC.newDeserializer(payload, SETTINGS);
            assertEquals(Instant.ofEpochSecond(-100), de.readTimestamp(PreludeSchemas.TIMESTAMP));
        }

        @Test
        void timestampWholeSeconds() {
            Instant wholeSecond = Instant.ofEpochSecond(1700000000);
            CborTestData.Bird bird = new CborTestData.BirdBuilder().lastSquawkAt(wholeSecond).build();

            ByteBuffer ser = roundTrip(bird);
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();

            assertEquals(wholeSecond, de.lastSquawkAt);
        }

        @Test
        void timestampWithMillis() {
            Instant withMillis = Instant.ofEpochMilli(1700000000123L);
            CborTestData.Bird bird = new CborTestData.BirdBuilder().lastSquawkAt(withMillis).build();

            ByteBuffer ser = roundTrip(bird);
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();

            assertEquals(withMillis, de.lastSquawkAt);
        }
    }

    // Regression: readDouble rejected any non-FLOAT token, so a whole-valued double/float written as a
    // CBOR integer (e.g. 1.0 -> major-type-0 int 1) threw instead of coercing.
    @Nested
    class IntegerEncodedFloatingPointTest {
        @Test
        void doubleFromPositiveInteger() {
            ShapeDeserializer de = CODEC.newDeserializer(POS_INT_ONE, SETTINGS);
            assertEquals(1.0d, de.readDouble(PreludeSchemas.DOUBLE));
        }

        @Test
        void doubleFromZeroInteger() {
            ShapeDeserializer de = CODEC.newDeserializer(POS_INT_ZERO, SETTINGS);
            assertEquals(0.0d, de.readDouble(PreludeSchemas.DOUBLE));
        }

        @Test
        void doubleFromNegativeInteger() {
            ShapeDeserializer de = CODEC.newDeserializer(NEG_INT_ONE, SETTINGS);
            assertEquals(-1.0d, de.readDouble(PreludeSchemas.DOUBLE));
        }

        @Test
        void doubleFromMultiByteInteger() {
            ShapeDeserializer de = CODEC.newDeserializer(POS_INT_FORTY_TWO, SETTINGS);
            assertEquals(42.0d, de.readDouble(PreludeSchemas.DOUBLE));
        }

        @Test
        void floatFromInteger() {
            ShapeDeserializer de = CODEC.newDeserializer(POS_INT_FORTY_TWO, SETTINGS);
            assertEquals(42.0f, de.readFloat(PreludeSchemas.FLOAT));
        }

        @Test
        void doubleFromLongMaxInteger() {
            ShapeDeserializer de = CODEC.newDeserializer(POS_INT_LONG_MAX, SETTINGS);
            assertEquals((double) Long.MAX_VALUE, de.readDouble(PreludeSchemas.DOUBLE));
        }

        // A 64-bit magnitude that overflows long must throw rather than silently saturate.
        @Test
        void doubleFromOverflowingPositiveIntegerThrows() {
            ShapeDeserializer de = CODEC.newDeserializer(POS_INT_OVERFLOW, SETTINGS);
            assertThrows(SerializationException.class, () -> de.readDouble(PreludeSchemas.DOUBLE));
        }

        @Test
        void doubleFromOverflowingNegativeIntegerThrows() {
            ShapeDeserializer de = CODEC.newDeserializer(NEG_INT_OVERFLOW, SETTINGS);
            assertThrows(SerializationException.class, () -> de.readDouble(PreludeSchemas.DOUBLE));
        }

        // A non-numeric token (here a text string) must still be rejected.
        @Test
        void doubleFromStringStillThrows() {
            // CBOR text string "hi"
            byte[] textHi = {0x62, 'h', 'i'};
            ShapeDeserializer de = CODEC.newDeserializer(textHi, SETTINGS);
            assertThrows(SerializationException.class, () -> de.readDouble(PreludeSchemas.DOUBLE));
        }
    }

    @Nested
    class BigIntegerEdgeCaseTest {
        @ParameterizedTest
        @ValueSource(strings = {
            "9223372036854775808", // Long.MAX_VALUE + 1 (exactly 64 bits positive)
            "-9223372036854775809", // Long.MIN_VALUE - 1 (exactly 64 bits negative)
            "18446744073709551615" // max unsigned 64-bit
        })
        void bigInteger64BitBoundary(String value) {
            BigInteger bi = new BigInteger(value);
            CborTestData.Bird bird = new CborTestData.BirdBuilder().flightRange(bi).build();
            ByteBuffer ser = roundTrip(bird);
            CborTestData.Bird de = new CborTestData.BirdBuilder().deserialize(CODEC.newDeserializer(ser, SETTINGS))
                                                                 .build();
            assertEquals(bi, de.flightRange);
        }
    }

    @Nested
    class MalformedCborTest {

        @Test
        void bigDecimalLongExponent() {
            byte[][] payloads = {
                {-60, -126, 27, 0, 0, 0, 7, -1, -1, -1, -1, 1},
                {-60, -126, 59, 0, 0, 0, 7, -1, -1, -1, -1, 1},
                // 1^-2147483648
                {-60, -126, 58, 127, -1, -1, -1, 1},
                // 1^-2147483649
                {-60, -126, 58, -128, 0, 0, 0, 1}
            };
            for (byte[] payload : payloads) {
                ShapeDeserializer de = CODEC.newDeserializer(payload, SETTINGS);
                assertThrows(BadCborException.class, () -> de.readBigDecimal(PreludeSchemas.BIG_DECIMAL));
            }
        }

        // Adapted: the deserializer now rejects a byte string longer than the payload when it reaches the token,
        // so the exception comes from creating the deserializer rather than from readBigInteger.
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void bigIntegerWithExcessiveLength(boolean negative) {
            byte[] payload = {
                (byte) (negative ? 0xC3 : 0xC2),
                (byte) 0x5A,
                (byte) 0x7F,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0x01,
                (byte) 0x02
            };
            assertThrows(BadCborException.class,
                         () -> CODEC.newDeserializer(payload, SETTINGS).readBigInteger(PreludeSchemas.BIG_INTEGER));
        }

        // Adapted: smithy-java threw ArrayIndexOutOfBoundsException here (a TODO in its test); the port checks chunk
        // lengths against the payload and throws BadCborException.
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void bigIntegerWithIndefiniteLengthExcessiveChunk(boolean negative) {
            byte[] payload = {
                (byte) (negative ? 0xC3 : 0xC2),
                (byte) 0x5F,
                (byte) 0x5A,
                (byte) 0x7F,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0x01,
                (byte) 0x02
            };
            assertThrows(BadCborException.class, () -> CODEC.newDeserializer(payload, SETTINGS));
        }

        @Test
        void bigIntegerWithIndefiniteLengthExcessiveTotalSize() {
            byte[] payload = {
                (byte) 0xC2,
                (byte) 0x5F,
                (byte) 0x5A,
                (byte) 0x1D,
                (byte) 0xCD,
                (byte) 0x65,
                (byte) 0x00,
                (byte) 0x01,
                (byte) 0x02,
                (byte) 0x5A,
                (byte) 0x1D,
                (byte) 0xCD,
                (byte) 0x65,
                (byte) 0x00,
                (byte) 0x03,
                (byte) 0x04
            };
            assertThrows(BadCborException.class, () -> CODEC.newDeserializer(payload, SETTINGS));
        }

        @Test
        void incompleteImmediate() {
            byte[] cbor = write(os -> writeList(os, Integer.MAX_VALUE, list -> list.writeString(null, "stop")));
            BadCborException e = assertThrows(BadCborException.class, () -> {
                ShapeDeserializer de = CODEC.newDeserializer(cbor, SETTINGS);
                de.readDocument();
            });
            assertTrue(e.getMessage().contains("incomplete array"), e.getMessage());
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void incompleteCollection(boolean map) {
            byte[] cbor = write(os -> {
                if (map) {
                    writeMap(os, 2, c -> c.entry("hi", value -> value.writeString(null, "hi")));
                } else {
                    writeList(os, 2, c -> c.writeString(null, "hi"));
                }
            });
            BadCborException e = assertThrows(BadCborException.class, () -> {
                ShapeDeserializer de = CODEC.newDeserializer(cbor, SETTINGS);
                de.readDocument();
            });
            assertTrue(e.getMessage().contains("incomplete " + (map ? "map" : "array")), e.getMessage());
        }

        @Test
        void timestampOutOfRange() {
            // tag(1) + uint64(Long.MAX_VALUE) - epoch seconds far exceeding Instant.MAX
            byte[] payload = {
                (byte) 0xC1, // tag 1 (epoch timestamp)
                (byte) 0x1B, // 8-byte positive integer follows
                (byte) 0x7F,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF
            };
            ShapeDeserializer de = CODEC.newDeserializer(payload, SETTINGS);
            assertThrows(SerializationException.class, () -> de.readTimestamp(PreludeSchemas.TIMESTAMP));
        }

        @Test
        void missingMapValue() {
            byte[] cbor = write(os -> writeMap(os, 1, m -> m.entry("hi", v -> {
                throw new StopWritingException();
            })));
            BadCborException e = assertThrows(BadCborException.class, () -> {
                ShapeDeserializer de = CODEC.newDeserializer(cbor, SETTINGS);
                de.readDocument();
            });
            assertTrue(e.getMessage().contains("incomplete map"), e.getMessage());
        }
    }

    private static final class StopWritingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static byte[] write(Consumer<CborSerializer> consumer) {
        ByteBufferOutputStream stream = new ByteBufferOutputStream();
        try (CborSerializer ser = new CborSerializer(stream)) {
            try {
                consumer.accept(ser);
            } catch (StopWritingException ignored) {
                // Leaves the output incomplete on purpose.
            }
            ser.flush();
        }
        return ByteBufferUtils.getBytes(stream.toByteBuffer());
    }

    private static void writeList(ShapeSerializer s, int len, Consumer<ShapeSerializer> listHandler) {
        s.writeList(null, null, len, (ignored, l) -> listHandler.accept(l));
    }

    private static void writeMap(ShapeSerializer s, int len, Consumer<WriteEntry> mapHandler) {
        s.writeMap(null, null, len, (ignored, l) -> mapHandler.accept(new WriteEntry(l)));
    }

    private static final class WriteEntry {
        private final MapSerializer m;

        private WriteEntry(MapSerializer m) {
            this.m = m;
        }

        void entry(String key, Consumer<ShapeSerializer> val) {
            m.writeEntry(null, key, null, (ignored, s) -> val.accept(s));
        }
    }
}
