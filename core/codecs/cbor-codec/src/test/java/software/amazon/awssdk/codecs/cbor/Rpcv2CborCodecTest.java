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

package software.amazon.awssdk.codecs.cbor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.cbor.internal.CborSchemaExtensions;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * SDK tests for {@link Rpcv2CborCodec} and {@link CborSettings}: the wire encoding of each Smithy type against the
 * RPCv2 CBOR specification (independent of the codec's own reader), round trips, tolerance of unknown and null
 * members, union handling, malformed input, and thread safety.
 */
class Rpcv2CborCodecTest {
    private static final Rpcv2CborCodec CODEC = Rpcv2CborCodec.builder().build();
    // The initial buffer of a CborSerializer created with a sink (CborSerializer.SINK_BUF_SIZE).
    private static final int SINK_BUFFER_SIZE = 1024;

    private static final Schema STRING_LIST = Schema.listBuilder(ShapeId.from("test#StringList"))
                                                    .putMember("member", PreludeSchemas.STRING)
                                                    .build();
    private static final Schema INT_MAP = Schema.mapBuilder(ShapeId.from("test#IntMap"))
                                                .putMember("key", PreludeSchemas.STRING)
                                                .putMember("value", PreludeSchemas.INTEGER)
                                                .build();
    private static final Schema NESTED = Schema.structureBuilder(ShapeId.from("test#Nested"))
                                               .putMember("name", PreludeSchemas.STRING)
                                               .build();
    private static final Schema CHOICE = Schema.unionBuilder(ShapeId.from("test#Choice"))
                                               .putMember("s", PreludeSchemas.STRING)
                                               .putMember("i", PreludeSchemas.INTEGER)
                                               .build();
    private static final Schema ALL = Schema.structureBuilder(ShapeId.from("test#AllTypes"))
                                            .putMember("boolean", PreludeSchemas.BOOLEAN)
                                            .putMember("byte", PreludeSchemas.BYTE)
                                            .putMember("short", PreludeSchemas.SHORT)
                                            .putMember("integer", PreludeSchemas.INTEGER)
                                            .putMember("long", PreludeSchemas.LONG)
                                            .putMember("float", PreludeSchemas.FLOAT)
                                            .putMember("double", PreludeSchemas.DOUBLE)
                                            .putMember("bigInteger", PreludeSchemas.BIG_INTEGER)
                                            .putMember("bigDecimal", PreludeSchemas.BIG_DECIMAL)
                                            .putMember("string", PreludeSchemas.STRING)
                                            .putMember("blob", PreludeSchemas.BLOB)
                                            .putMember("timestamp", PreludeSchemas.TIMESTAMP,
                                                       new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
                                            .putMember("document", PreludeSchemas.DOCUMENT)
                                            .putMember("list", STRING_LIST)
                                            .putMember("map", INT_MAP)
                                            .putMember("nested", NESTED)
                                            .putMember("choice", CHOICE)
                                            .build();

    private static byte[] hex(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return result;
    }

    private static String repeat(String value, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(value);
        }
        return sb.toString();
    }

    private static String toHex(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        ByteBuffer view = buffer.duplicate();
        while (view.hasRemaining()) {
            sb.append(String.format("%02x", view.get() & 0xFF));
        }
        return sb.toString();
    }

    private static String encode(Consumer<ShapeSerializer> writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            writer.accept(serializer);
        }
        return toHex(ByteBuffer.wrap(out.toByteArray()));
    }

    private static SchemaStruct allTypes() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("max", Integer.MAX_VALUE);
        return SchemaStruct.builder(ALL)
                           .set("boolean", true)
                           .set("byte", Byte.MIN_VALUE)
                           .set("short", Short.MAX_VALUE)
                           .set("integer", Integer.MIN_VALUE)
                           .set("long", Long.MAX_VALUE)
                           .set("float", Float.NaN)
                           .set("double", Double.NEGATIVE_INFINITY)
                           .set("bigInteger", new BigInteger("-123456789012345678901234567890"))
                           .set("bigDecimal", new BigDecimal("-1234567890.0987654321"))
                           .set("string", "h\u00e9llo \u4e16\u754c") // non-ASCII text
                           .set("blob", ByteBuffer.wrap(new byte[] {0, 1, (byte) 0xFF}))
                           .set("timestamp", Instant.ofEpochMilli(-1500))
                           .set("document", Document.of(Collections.singletonMap("k", Document.of(Arrays.asList(
                               Document.of("v"), Document.of(2L))))))
                           .set("list", Arrays.asList("a", "", "c"))
                           .set("map", map)
                           .set("nested", SchemaStruct.builder(NESTED).set("name", "inner").build())
                           .set("choice", SchemaStruct.builder(CHOICE).set("i", 7).build())
                           .build();
    }

    // ---- Settings and builder ----

    @Test
    void rpcv2Preset_hasTheSharedDefaults() {
        CborSettings settings = CborSettings.RPCV2;

        assertThat(CborSettings.defaultSettings()).isSameAs(settings);
        assertThat(settings.useTimestampFormat()).isFalse();
        assertThat(settings.defaultTimestampFormat()).isEqualTo(TimestampFormatTrait.Format.EPOCH_SECONDS);
        assertThat(settings.defaultNamespace()).isNull();
        assertThat(settings.forbidUnknownUnionMembers()).isFalse();
        assertThat(settings.provider()).isNotNull();
        assertThat(settings.provider().getName()).isEqualTo("cbor");
        assertThat(CborSettings.builder().build()).isEqualTo(settings).hasSameHashCodeAs(settings);
        assertThat(settings.toString()).isEqualTo(
            "CborSettings(useTimestampFormat=false, defaultTimestampFormat=epoch-seconds, defaultNamespace=null, "
            + "forbidUnknownUnionMembers=false, provider=default)");
    }

    @Test
    void settings_toBuilderAndUpdateBuilderCopyEveryValue() {
        CborSettings settings = CborSettings.builder()
                                            .defaultNamespace("com.example")
                                            .forbidUnknownUnionMembers(true)
                                            .useTimestampFormat(true)
                                            .defaultTimestampFormat(TimestampFormatTrait.Format.DATE_TIME)
                                            .build();

        assertThat(settings.toBuilder().build()).isEqualTo(settings);
        assertThat(CborSettings.builder().updateBuilder(settings).build()).isEqualTo(settings);
        assertThat(settings).isNotEqualTo(CborSettings.RPCV2);
        assertThatThrownBy(() -> CborSettings.builder().updateBuilder(null))
            .isInstanceOf(NullPointerException.class).hasMessage("settings must not be null");
    }

    @Test
    void codecBuilder_appliesSettings() {
        Rpcv2CborCodec codec = Rpcv2CborCodec.builder()
                                             .settings(CborSettings.builder().defaultNamespace("a.b").build())
                                             .forbidUnknownUnionMembers(true)
                                             .build();

        assertThat(Rpcv2CborCodec.builder().build().settings()).isEqualTo(CborSettings.RPCV2);
        assertThat(codec.settings().defaultNamespace()).isEqualTo("a.b");
        assertThat(codec.settings().forbidUnknownUnionMembers()).isTrue();
        assertThat(Rpcv2CborCodec.builder().defaultNamespace("x.y").build().settings().defaultNamespace())
            .isEqualTo("x.y");
        assertThatThrownBy(() -> Rpcv2CborCodec.builder().overrideSerdeProvider(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void overrideSerdeProvider_isUsedByTheCodec() {
        List<String> calls = new ArrayList<>();
        CborSerdeProvider delegate = CborSettings.RPCV2.provider();
        CborSerdeProvider recording = new CborSerdeProvider() {
            @Override
            public int getPriority() {
                return 1;
            }

            @Override
            public String getName() {
                return "recording";
            }

            @Override
            public ShapeDeserializer newDeserializer(byte[] source, CborSettings settings) {
                calls.add("bytes");
                return delegate.newDeserializer(source, settings);
            }

            @Override
            public ShapeDeserializer newDeserializer(ByteBuffer source, CborSettings settings) {
                calls.add("buffer");
                return delegate.newDeserializer(source, settings);
            }

            @Override
            public ShapeSerializer newSerializer(OutputStream sink, CborSettings settings) {
                calls.add("serializer");
                return delegate.newSerializer(sink, settings);
            }

            @Override
            public ByteBuffer serialize(SerializableShape shape, CborSettings settings) {
                calls.add("serialize");
                return delegate.serialize(shape, settings);
            }
        };
        Rpcv2CborCodec codec = Rpcv2CborCodec.builder().overrideSerdeProvider(recording).build();

        ByteBuffer bytes = codec.serialize(Document.of(1L));
        codec.createDeserializer(bytes).readDocument();
        codec.createDeserializer(new byte[] {1}).readDocument();
        codec.createSerializer(new ByteArrayOutputStream()).close();

        assertThat(calls).containsExactly("serialize", "buffer", "bytes", "serializer");
        assertThat(codec.settings().provider()).isSameAs(recording);
        assertThat(codec.settings()).isNotEqualTo(CborSettings.RPCV2);
    }

    @Test
    void serialize_nullShape_throwsNpe() {
        assertThatThrownBy(() -> CODEC.serialize(null))
            .isInstanceOf(NullPointerException.class).hasMessage("shape must not be null");
    }

    @Test
    void schemaExtension_isRegistered() {
        assertThat(ALL.getExtension(CborSchemaExtensions.KEY)).isNotNull();
        assertThat(ALL.member("string").getExtension(CborSchemaExtensions.KEY)).isNotNull();
    }

    // ---- Wire encoding (RPCv2 CBOR shape serialization; expected bytes from RFC 8949) ----

    static List<Arguments> integerEncodings() {
        return Arrays.asList(
            Arguments.of(0L, "00"),
            Arguments.of(23L, "17"),
            Arguments.of(24L, "1818"),
            Arguments.of(255L, "18ff"),
            Arguments.of(256L, "190100"),
            Arguments.of(65535L, "19ffff"),
            Arguments.of(65536L, "1a00010000"),
            Arguments.of(4294967295L, "1affffffff"),
            Arguments.of(4294967296L, "1b0000000100000000"),
            Arguments.of(Long.MAX_VALUE, "1b7fffffffffffffff"),
            Arguments.of(-1L, "20"),
            Arguments.of(-24L, "37"),
            Arguments.of(-25L, "3818"),
            Arguments.of(-1000L, "3903e7"),
            Arguments.of(Long.MIN_VALUE, "3b7fffffffffffffff"));
    }

    @ParameterizedTest
    @MethodSource("integerEncodings")
    void integers_useTheShortestEncoding(long value, String expected) {
        assertThat(encode(s -> s.writeLong(PreludeSchemas.LONG, value))).isEqualTo(expected);
        assertThat(CODEC.createDeserializer(hex(expected)).readLong(PreludeSchemas.LONG)).isEqualTo(value);
    }

    @Test
    void scalars_matchTheSpecification() {
        assertThat(encode(s -> s.writeBoolean(PreludeSchemas.BOOLEAN, true))).isEqualTo("f5");
        assertThat(encode(s -> s.writeBoolean(PreludeSchemas.BOOLEAN, false))).isEqualTo("f4");
        assertThat(encode(s -> s.writeNull(PreludeSchemas.STRING))).isEqualTo("f6");
        assertThat(encode(s -> s.writeFloat(PreludeSchemas.FLOAT, 1.5f))).isEqualTo("fa3fc00000");
        assertThat(encode(s -> s.writeDouble(PreludeSchemas.DOUBLE, 1.1))).isEqualTo("fb3ff199999999999a");
        assertThat(encode(s -> s.writeString(PreludeSchemas.STRING, ""))).isEqualTo("60");
        assertThat(encode(s -> s.writeString(PreludeSchemas.STRING, "IETF"))).isEqualTo("6449455446");
        assertThat(encode(s -> s.writeString(PreludeSchemas.STRING, "\u00fc"))).isEqualTo("62c3bc"); // u-umlaut
        assertThat(encode(s -> s.writeString(PreludeSchemas.STRING, repeat("a", 24))))
            .isEqualTo("7818" + repeat("61", 24));
        assertThat(encode(s -> s.writeBlob(PreludeSchemas.BLOB, new byte[] {1, 2, 3, 4}))).isEqualTo("4401020304");
    }

    @Test
    void timestamps_areTag1EpochSecondsAsDoubles() {
        // Whole seconds are doubles too, as the legacy smithy-rpcv2-protocol marshallers write them.
        assertThat(encode(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.ofEpochSecond(1363896240))))
            .isEqualTo("c1fb41d452d9ec000000");
        assertThat(encode(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.ofEpochMilli(1500))))
            .isEqualTo("c1fb3ff8000000000000");
        assertThat(encode(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.ofEpochSecond(-1))))
            .isEqualTo("c1fbbff0000000000000");
        // The timestampFormat trait is ignored.
        assertThat(encode(s -> s.writeTimestamp(ALL.member("timestamp"), Instant.ofEpochSecond(1))))
            .isEqualTo("c1fb3ff0000000000000");
    }

    @Test
    void timestamps_readIntegerAndFloatEpochSeconds() {
        assertThat(CODEC.createDeserializer(hex("c11a514b67b0")).readTimestamp(PreludeSchemas.TIMESTAMP))
            .isEqualTo(Instant.ofEpochSecond(1363896240));
        assertThat(CODEC.createDeserializer(hex("c1fb41d452d9ec000000")).readTimestamp(PreludeSchemas.TIMESTAMP))
            .isEqualTo(Instant.ofEpochSecond(1363896240));
    }

    @Test
    void timestampOutsideEpochMillisRange_throwsSerializationException() {
        assertThatThrownBy(() -> encode(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.MAX)))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void bigNumbers_matchTheSpecification() {
        // 2^64: tag 2 bignum
        assertThat(encode(s -> s.writeBigInteger(PreludeSchemas.BIG_INTEGER, BigInteger.ONE.shiftLeft(64))))
            .isEqualTo("c249010000000000000000");
        // -2^64 - 1: tag 3 bignum of 2^64
        assertThat(encode(s -> s.writeBigInteger(PreludeSchemas.BIG_INTEGER,
                                                 BigInteger.ONE.shiftLeft(64).negate().subtract(BigInteger.ONE))))
            .isEqualTo("c349010000000000000000");
        // Fits in a long: a plain integer
        assertThat(encode(s -> s.writeBigInteger(PreludeSchemas.BIG_INTEGER, BigInteger.valueOf(-500))))
            .isEqualTo("3901f3");
        // 273.15 = 27315 * 10^-2: tag 4 [-2, 27315] (RFC 8949 3.4.4)
        assertThat(encode(s -> s.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, new BigDecimal("273.15"))))
            .isEqualTo("c48221196ab3");
    }

    @Test
    void containers_matchTheSpecification() {
        // Structures: indefinite-length maps keyed by member name
        assertThat(encode(s -> s.writeStruct(NESTED, SchemaStruct.builder(NESTED).set("name", "x").build())))
            .isEqualTo("bf646e616d656178ff");
        // Lists and maps: definite length when the size is known, indefinite when it is not
        assertThat(encode(s -> SchemaStruct.write(s, STRING_LIST, Arrays.asList("a", "b")))).isEqualTo("8261616162");
        assertThat(encode(s -> s.writeList(STRING_LIST, null, -1, (st, l) -> l.writeString(STRING_LIST, "a"))))
            .isEqualTo("9f6161ff");
        assertThat(encode(s -> SchemaStruct.write(s, INT_MAP, Collections.singletonMap("a", 1)))).isEqualTo("a1616101");
        assertThat(encode(s -> s.writeMap(INT_MAP, null, -1, (st, m) -> {
        }))).isEqualTo("bfff");
    }

    @Test
    void serialize_isByteIdenticalToTheSerializerPath() {
        SchemaStruct value = allTypes();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            value.serialize(serializer);
        }

        assertThat(toHex(CODEC.serialize(value))).isEqualTo(toHex(ByteBuffer.wrap(out.toByteArray())));
    }

    @Test
    void createSerializer_writesOnFlushAndCloseWithoutClosingTheSink() {
        List<String> events = new ArrayList<>();
        ByteArrayOutputStream sink = new ByteArrayOutputStream() {
            @Override
            public void close() {
                events.add("close");
            }
        };
        ShapeSerializer serializer = CODEC.createSerializer(sink);
        serializer.writeInteger(PreludeSchemas.INTEGER, 1);
        assertThat(sink.size()).isZero();

        serializer.flush();
        assertThat(sink.toByteArray()).containsExactly(1);

        serializer.writeInteger(PreludeSchemas.INTEGER, 2);
        serializer.close();
        assertThat(sink.toByteArray()).containsExactly(1, 2);
        assertThat(events).isEmpty();
    }

    // ---- Round trips ----

    @Test
    void roundTrip_everyType() {
        SchemaStruct value = allTypes();

        SchemaStruct result = CODEC.deserializeShape(CODEC.serialize(value), SchemaStruct.builder(ALL));

        assertThat(result).isEqualTo(value);
    }

    @Test
    void roundTrip_emptyStructAndEmptyCollections() {
        SchemaStruct empty = SchemaStruct.builder(ALL).build();
        SchemaStruct emptyCollections = SchemaStruct.builder(ALL)
                                                    .set("list", Collections.emptyList())
                                                    .set("map", Collections.emptyMap())
                                                    .set("string", "")
                                                    .set("blob", ByteBuffer.allocate(0))
                                                    .build();

        assertThat(toHex(CODEC.serialize(empty))).isEqualTo("bfff");
        assertThat(CODEC.deserializeShape(CODEC.serialize(empty), SchemaStruct.builder(ALL))).isEqualTo(empty);
        assertThat(CODEC.deserializeShape(CODEC.serialize(emptyCollections), SchemaStruct.builder(ALL)))
            .isEqualTo(emptyCollections);
    }

    @Test
    void roundTrip_deeplyNestedCollections() {
        // More than 64 levels exercises the serializer's overflow stack of indefinite-length flags.
        Document document = Document.of(1L);
        for (int i = 0; i < 100; i++) {
            document = Document.of(Collections.singletonList(document));
        }

        assertThat(CODEC.createDeserializer(CODEC.serialize(document)).readDocument()).isEqualTo(document);
    }

    @Test
    void createDeserializer_readsTheRemainingBytesOfSlicedAndDirectBuffers() {
        byte[] encoded = hex("bf646e616d656178ff");
        byte[] padded = new byte[encoded.length + 4];
        System.arraycopy(encoded, 0, padded, 2, encoded.length);
        padded[0] = (byte) 0xFF;
        padded[padded.length - 1] = (byte) 0xFF;
        ByteBuffer sliced = ByteBuffer.wrap(padded, 2, encoded.length);
        ByteBuffer direct = ByteBuffer.allocateDirect(encoded.length);
        direct.put(encoded);
        direct.flip();

        SchemaStruct expected = SchemaStruct.builder(NESTED).set("name", "x").build();
        assertThat(CODEC.deserializeShape(sliced, SchemaStruct.builder(NESTED))).isEqualTo(expected);
        assertThat(CODEC.deserializeShape(direct, SchemaStruct.builder(NESTED))).isEqualTo(expected);
        assertThat(sliced.position()).isEqualTo(2);
        assertThat(direct.position()).isZero();
    }

    // ---- Reading what other encoders write ----

    @Test
    void read_acceptsDefiniteLengthStructsAndIndefiniteLengthStrings() {
        // {"name": "ab"} with a definite-length map, an indefinite-length key and a chunked value
        SchemaStruct result = CODEC.deserializeShape(hex("a17f626e61626d65ff7f61616162ff"),
                                                     SchemaStruct.builder(NESTED));
        assertThat(result.get("name")).isEqualTo("ab");

        // Chunked byte string
        ByteBuffer blob = CODEC.createDeserializer(hex("5f42010243030405ff")).readBlob(PreludeSchemas.BLOB);
        assertThat(blob).isEqualTo(ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5}));
    }

    @Test
    void read_acceptsHalfPrecisionFloatsAndUndefined() {
        assertThat(CODEC.createDeserializer(hex("f93c00")).readDouble(PreludeSchemas.DOUBLE)).isEqualTo(1.0);
        assertThat(CODEC.createDeserializer(hex("f97c00")).readFloat(PreludeSchemas.FLOAT))
            .isEqualTo(Float.POSITIVE_INFINITY);
        assertThat(CODEC.createDeserializer(hex("f97e00")).readFloat(PreludeSchemas.FLOAT)).isNaN();
        assertThat(CODEC.createDeserializer(hex("fa47c35000")).readFloat(PreludeSchemas.FLOAT)).isEqualTo(100000.0f);
        assertThat(CODEC.createDeserializer(hex("f7")).isNull()).isTrue();
    }

    @Test
    void read_nullMembersAreAbsent() {
        // {"name": null}
        SchemaStruct result = CODEC.deserializeShape(hex("bf646e616d65f6ff"), SchemaStruct.builder(NESTED));
        assertThat(result.get("name")).isNull();
    }

    @Test
    void read_unknownMembersAreSkippedAndReported() {
        // {"x": [1, {"y": 2}], "name": "n", "z": 3}
        SchemaStruct.Builder builder = SchemaStruct.builder(NESTED);
        builder.deserialize(CODEC.createDeserializer(hex("bf61788201a1617902646e616d65616e617a03ff")));

        assertThat(builder.build().get("name")).isEqualTo("n");
        assertThat(builder.unknownMembers()).containsExactly("x", "z");
    }

    @Test
    void read_unionIgnoresTypeMember() {
        // {"__type": "test#Choice", "s": "v"}
        SchemaStruct.Builder builder = SchemaStruct.builder(CHOICE);
        builder.deserialize(CODEC.createDeserializer(hex("bf665f5f747970656b746573742343686f69636561736176ff")));

        assertThat(builder.build().get("s")).isEqualTo("v");
        assertThat(builder.unknownMembers()).isEmpty();
    }

    @Test
    void read_unknownUnionMember_isReportedOrRejectedPerSettings() {
        // {"other": 1}
        byte[] payload = hex("bf656f7468657201ff");
        SchemaStruct.Builder builder = SchemaStruct.builder(CHOICE);
        builder.deserialize(CODEC.createDeserializer(payload));
        assertThat(builder.unknownMembers()).containsExactly("other");

        Rpcv2CborCodec strict = Rpcv2CborCodec.builder().forbidUnknownUnionMembers(true).build();
        assertThatThrownBy(() -> strict.deserializeShape(payload, SchemaStruct.builder(CHOICE)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("other")
            .hasMessageContaining("test#Choice");
        // Structures are not affected by the setting.
        assertThat(strict.deserializeShape(hex("bf656f7468657201ff"), SchemaStruct.builder(NESTED)).get("name"))
            .isNull();
    }

    // ---- Malformed and non-conforming input ----

    static List<Arguments> malformedPayloads() {
        return Arrays.asList(
            Arguments.of("truncated integer", "19 01"),
            Arguments.of("truncated string", "63 61 62"),
            Arguments.of("truncated struct", "bf 64 6e 61 6d 65"),
            Arguments.of("missing break", "bf 64 6e 61 6d 65 61 78"),
            Arguments.of("non-string key", "a1 01 01"),
            Arguments.of("unexpected break", "ff"),
            Arguments.of("reserved additional information", "1c"),
            Arguments.of("indefinite-length integer", "1f"),
            Arguments.of("nested tags", "c1 c1 01"),
            Arguments.of("unsupported tag", "c5 82 01 01"),
            Arguments.of("tag without a value", "c1"),
            Arguments.of("timestamp tag on a string", "c1 61 61"),
            Arguments.of("chunk of another major type", "7f 41 61 ff"),
            Arguments.of("chunk longer than the payload", "7f 7a 7f ff ff ff 61 ff"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedPayloads")
    void malformedInput_throwsSerializationException(String description, String payload) {
        byte[] bytes = hex(payload.replace(" ", ""));

        assertThatThrownBy(() -> CODEC.deserializeShape(bytes, SchemaStruct.builder(NESTED)))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> CODEC.createDeserializer(bytes).readDocument())
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void typeMismatch_namesTheShapeId() {
        // {"name": 1}
        assertThatThrownBy(() -> CODEC.deserializeShape(hex("bf646e616d6501ff"), SchemaStruct.builder(NESTED)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Can't read POS_INT as a string for test#Nested$name");
        assertThatThrownBy(() -> CODEC.createDeserializer(hex("01")).readStruct(NESTED, null, (s, m, d) -> {
        })).isInstanceOf(SerializationException.class).hasMessageContaining("test#Nested");
        // An untagged number is not a timestamp
        assertThatThrownBy(() -> CODEC.createDeserializer(hex("01")).readTimestamp(PreludeSchemas.TIMESTAMP))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> CODEC.createDeserializer(new byte[0]).readInteger(PreludeSchemas.INTEGER))
            .isInstanceOf(SerializationException.class).hasMessageContaining("FINISHED");
    }

    @Test
    void integersOutOfRange_throwSerializationException() {
        assertThatThrownBy(() -> CODEC.createDeserializer(hex("1880")).readByte(PreludeSchemas.BYTE))
            .isInstanceOf(SerializationException.class).hasMessageContaining("out of range");
        assertThatThrownBy(() -> CODEC.createDeserializer(hex("398000")).readShort(PreludeSchemas.SHORT))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> CODEC.createDeserializer(hex("1a80000000")).readInteger(PreludeSchemas.INTEGER))
            .isInstanceOf(SerializationException.class);
        assertThat(CODEC.createDeserializer(hex("387f")).readByte(PreludeSchemas.BYTE)).isEqualTo(Byte.MIN_VALUE);
    }

    @Test
    void nonFiniteTimestamp_throwsSerializationException() {
        ShapeDeserializer nan = CODEC.createDeserializer(hex("c1fb7ff8000000000000"));
        assertThatThrownBy(() -> nan.readTimestamp(PreludeSchemas.TIMESTAMP))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void close_rejectsTrailingContentOnly() {
        ShapeDeserializer complete = CODEC.createDeserializer(hex("bf646e616d656178ff"));
        SchemaStruct.builder(NESTED).deserialize(complete);
        complete.close();

        ShapeDeserializer scalar = CODEC.createDeserializer(hex("01"));
        scalar.readInteger(PreludeSchemas.INTEGER);
        scalar.close();

        ShapeDeserializer trailing = CODEC.createDeserializer(hex("0102"));
        trailing.readInteger(PreludeSchemas.INTEGER);
        assertThatThrownBy(trailing::close).isInstanceOf(SerializationException.class);
    }

    // ---- Thread safety ----

    @Test
    void oneCodec_usedConcurrently_givesEachThreadItsOwnResult() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                int seed = t;
                results.add(executor.submit((Callable<Boolean>) () -> {
                    for (int i = 0; i < 200; i++) {
                        SchemaStruct value = SchemaStruct.builder(NESTED).set("name", "thread-" + seed + "-" + i)
                                                         .build();
                        ByteBuffer bytes = CODEC.serialize(value);
                        if (!CODEC.deserializeShape(bytes, SchemaStruct.builder(NESTED)).equals(value)) {
                            return false;
                        }
                    }
                    return true;
                }));
            }
            for (Future<Boolean> result : results) {
                assertThat(result.get()).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // ---- Writing through a sink ----

    @Test
    void createSerializer_withBodyLargerThanBuffer_writesFullBuffersToSinkWithoutGrowing() {
        Map<String, Document> items = new LinkedHashMap<>();
        for (int i = 0; i < 400; i++) {
            Map<String, Document> item = new LinkedHashMap<>();
            item.put("id", Document.of(i));
            item.put("text", Document.of(repeat("abcdefghij", 20)));
            item.put("unicode", Document.of(repeat("\u00e9\u4e2d", 10)));
            item.put("tags", Document.of(Arrays.asList(Document.of("a"), Document.of(true), Document.of(1.5))));
            items.put("item" + i, Document.of(item));
        }
        Document document = Document.of(items);
        WriteRecordingStream output = new WriteRecordingStream();

        try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
            serializer.writeDocument(PreludeSchemas.DOCUMENT, document);
        }

        byte[] expected = toArray(CODEC.serialize(document));
        assertThat(output.toByteArray()).isEqualTo(expected);
        assertThat(expected.length).isGreaterThan(100_000);
        assertThat(output.writes).isGreaterThan(1);
        // The serializer's buffer is drained when full rather than grown to the document size.
        assertThat(output.largestWrite).isLessThanOrEqualTo(SINK_BUFFER_SIZE);
    }

    @Test
    void createSerializer_withValueLargerThanBuffer_drainsThenGrowsForThatValue() {
        String large = repeat("y", 20_000);
        byte[] blob = new byte[5_000];
        Arrays.fill(blob, (byte) 7);
        SerializableShape shape = s -> s.writeList(STRING_LIST, large, 3, (value, list) -> {
            list.writeString(PreludeSchemas.STRING, repeat("z", SINK_BUFFER_SIZE - 10));
            list.writeString(PreludeSchemas.STRING, value);
            list.writeBlob(PreludeSchemas.BLOB, blob);
        });
        ByteBufferOutputStream output = new ByteBufferOutputStream(16);

        try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
            shape.serialize(serializer);
        }

        assertThat(toArray(output.toByteBuffer())).isEqualTo(toArray(CODEC.serialize(shape)));
    }

    @Test
    void createSerializer_withSmallDocument_writesToSinkOnceOnClose() {
        SchemaStruct value = allTypes();
        WriteRecordingStream output = new WriteRecordingStream();

        try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
            serializer.writeStruct(ALL, value);
            assertThat(output.writes).isZero();
        }

        assertThat(output.toByteArray()).isEqualTo(toArray(CODEC.serialize(value)));
        assertThat(output.writes).isEqualTo(1);
    }

    /**
     * Moves a structure of every member type, a string of each length below and a blob across the end of the sink
     * buffer one byte at a time, so that every write starts at, ends at and straddles the point where the buffer is
     * drained. The string lengths cover values whose reservation ({@code 5 + 3 * length}) just fits the buffer, just
     * does not, and needs it to grow; the non-ASCII kinds also take the path that rewrites the length header.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sinkBufferStringKinds")
    void createSerializer_atEveryBufferBoundary_matchesSerialize(String kind, String unit) {
        int third = (SINK_BUFFER_SIZE - 5) / 3;
        int[] valueLengths = {0, 1, 23, 24, third - 1, third, third + 1, SINK_BUFFER_SIZE - 1, SINK_BUFFER_SIZE,
                              SINK_BUFFER_SIZE + 1, SINK_BUFFER_SIZE * 2 + 1};
        SchemaStruct struct = allTypes();
        for (int valueLength : valueLengths) {
            String value = repeat(unit, valueLength);
            // The structure is under 400 bytes, so the drain point passes through every write after the pad.
            for (int pad = SINK_BUFFER_SIZE - 400; pad <= SINK_BUFFER_SIZE + 2; pad++) {
                String padding = repeat("p", pad);
                SerializableShape shape = s -> s.writeList(STRING_LIST, value, 5, (v, list) -> {
                    list.writeString(PreludeSchemas.STRING, padding);
                    list.writeStruct(ALL, struct);
                    list.writeString(PreludeSchemas.STRING, v);
                    list.writeBlob(PreludeSchemas.BLOB, new byte[] {1, 2, 3, 4, 5});
                    list.writeString(PreludeSchemas.STRING, "tail");
                });
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (ShapeSerializer serializer = CODEC.createSerializer(output)) {
                    shape.serialize(serializer);
                }

                assertThat(output.toByteArray())
                    .as("%s value of %d chars after %d pad chars", kind, valueLength, pad)
                    .isEqualTo(toArray(CODEC.serialize(shape)));
            }
        }
    }

    static List<Arguments> sinkBufferStringKinds() {
        return Arrays.asList(Arguments.of("ascii", "a"),
                             Arguments.of("two-byte", "\u00e9"),
                             Arguments.of("three-byte", "\u4e2d"),
                             Arguments.of("surrogate pair", "\ud83d\ude00"));
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    /** Records the number and largest length of the writes it receives. */
    private static final class WriteRecordingStream extends ByteArrayOutputStream {
        private int writes;
        private int largestWrite;

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            writes++;
            largestWrite = Math.max(largestWrite, len);
            super.write(b, off, len);
        }
    }

    @Test
    void stringsOfEveryHeaderSize_roundTrip() {
        for (int length : new int[] {0, 1, 23, 24, 255, 256, 65535, 65536}) {
            char[] ascii = new char[length];
            Arrays.fill(ascii, 'x');
            char[] wide = new char[length];
            Arrays.fill(wide, '\u00e9'); // 2-byte UTF-8
            for (String value : new String[] {new String(ascii), new String(wide)}) {
                ByteBuffer bytes = CODEC.serialize(Document.of(value));
                assertThat(CODEC.createDeserializer(bytes).readString(PreludeSchemas.STRING)).isEqualTo(value);
                assertThat(bytes.remaining()).isGreaterThanOrEqualTo(value.getBytes(StandardCharsets.UTF_8).length);
            }
        }
    }
}
