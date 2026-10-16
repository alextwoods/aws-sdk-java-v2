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


package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.codecs.json.TestCollections.listOf;
import static software.amazon.awssdk.codecs.json.TestCollections.mapOf;
import static software.amazon.awssdk.codecs.json.TestCollections.repeat;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.Trait;

public class JsonDeserializerTest extends ProviderTestBase {
    @PerProvider
    public void detectsUnclosedStructureObject(JsonSerdeProvider provider) {
        Set<String> members = new LinkedHashSet<>();

        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
                ShapeDeserializer de = codec.createDeserializer("{\"name\":\"Sam\"".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                    memberResult.add(member.memberName());
                });
            }
        });

        assertThat(members).containsExactly("name");
    }

    @PerProvider
    public void deserializesByte(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readByte(PreludeSchemas.BYTE)).isEqualTo((byte) 1);
        }
    }

    @PerProvider
    public void deserializesShort(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readShort(PreludeSchemas.SHORT)).isEqualTo((short) 1);
        }
    }

    @PerProvider
    public void deserializesInteger(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readInteger(PreludeSchemas.INTEGER)).isEqualTo(1);
        }
    }

    @PerProvider
    public void deserializesLong(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readLong(PreludeSchemas.LONG)).isEqualTo(1L);
        }
    }

    @PerProvider
    public void deserializesFloat(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readFloat(PreludeSchemas.FLOAT)).isEqualTo(1.0f);
            de = codec.createDeserializer("\"NaN\"".getBytes(StandardCharsets.UTF_8));
            assertTrue(Float.isNaN(de.readFloat(PreludeSchemas.FLOAT)));
            de = codec.createDeserializer("\"Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readFloat(PreludeSchemas.FLOAT)).isEqualTo(Float.POSITIVE_INFINITY);
            de = codec.createDeserializer("\"-Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readFloat(PreludeSchemas.FLOAT)).isEqualTo(Float.NEGATIVE_INFINITY);
        }
    }

    @PerProvider
    public void normalFloatsCannotBeStrings(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> {
                de.readFloat(PreludeSchemas.FLOAT);
            });
        }
    }

    @PerProvider
    public void deserializesDouble(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readDouble(PreludeSchemas.DOUBLE)).isEqualTo(1.0);
            de = codec.createDeserializer("\"NaN\"".getBytes(StandardCharsets.UTF_8));
            assertTrue(Double.isNaN(de.readDouble(PreludeSchemas.DOUBLE)));
            de = codec.createDeserializer("\"Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readDouble(PreludeSchemas.DOUBLE)).isEqualTo(Double.POSITIVE_INFINITY);
            de = codec.createDeserializer("\"-Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readDouble(PreludeSchemas.DOUBLE)).isEqualTo(Double.NEGATIVE_INFINITY);
        }
    }

    @ParameterizedTest
    @MethodSource("wholeNumberDoubles")
    public void deserializesWholeNumberDoubleExactly(JsonSerdeProvider provider, String json) {
        long expected = Double.doubleToRawLongBits(Double.parseDouble(json));

        try (JsonCodec codec = codec(provider)) {
            double actual = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8))
                    .readDouble(PreludeSchemas.DOUBLE);

            assertThat(Double.doubleToRawLongBits(actual)).isEqualTo(expected);
        }
    }

    static List<Arguments> wholeNumberDoubles() {
        ArrayList<Arguments> result = new ArrayList<Arguments>();
        List<String> values = listOf(
                "0",
                "1",
                "-1",
                "9007199254740991",
                "9007199254740992",
                "9007199254740993",
                "123456789012345678",
                "999999999999999999",
                "-999999999999999999",
                "1000000000000000000",
                "9223372036854775807",
                "9223372036854775808",
                "123456789012345678901234567890");
        for (JsonSerdeProvider provider : providerInstances()) {
            for (String value : values) {
                result.add(Arguments.of(provider, value));
            }
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void preservesNegativeWholeNumberZero(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            double actual = codec.createDeserializer("-0".getBytes(StandardCharsets.UTF_8))
                    .readDouble(PreludeSchemas.DOUBLE);

            assertThat(Double.doubleToRawLongBits(actual)).isEqualTo(Double.doubleToRawLongBits(-0.0d));
        }
    }

    @PerProvider
    public void normalDoublesCannotBeStrings(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> {
                de.readDouble(PreludeSchemas.DOUBLE);
            });
        }
    }

    @PerProvider
    public void deserializesBigInteger(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBigInteger(PreludeSchemas.BIG_INTEGER)).isEqualTo(BigInteger.ONE);
        }
    }

    @PerProvider
    public void deserializesBigIntegerOnlyFromRawNumbersByDefault(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readBigInteger(PreludeSchemas.BIG_INTEGER));
        }
    }

    @PerProvider
    public void deserializesBigIntegerFromStringWhenConfigured(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useStringForArbitraryPrecision(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBigInteger(PreludeSchemas.BIG_INTEGER)).isEqualTo(BigInteger.ONE);
        }
    }

    @PerProvider
    public void deserializesBigIntegerOnlyFromStringWhenConfigured(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useStringForArbitraryPrecision(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readBigInteger(PreludeSchemas.BIG_INTEGER));
        }
    }

    @PerProvider
    public void deserializesBigDecimal(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBigDecimal(PreludeSchemas.BIG_DECIMAL)).isEqualTo(BigDecimal.ONE);
        }
    }

    @PerProvider
    public void deserializesBigDecimalOnlyFromRawNumbersByDefault(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readBigDecimal(PreludeSchemas.BIG_DECIMAL));
        }
    }

    @PerProvider
    public void deserializesBigDecimalFromStringWhenConfigured(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useStringForArbitraryPrecision(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBigDecimal(PreludeSchemas.BIG_DECIMAL)).isEqualTo(BigDecimal.ONE);
        }
    }

    @PerProvider
    public void deserializesBigDecimalOnlyFromStringWhenConfigured(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useStringForArbitraryPrecision(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readBigDecimal(PreludeSchemas.BIG_DECIMAL));
        }
    }

    @PerProvider
    public void deserializesTimestamp(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            try (ShapeSerializer ser = codec.createSerializer(sink)) {
                ser.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.EPOCH);
            }

            ShapeDeserializer de = codec.createDeserializer(sink.toByteArray());
            assertThat(de.readTimestamp(PreludeSchemas.TIMESTAMP)).isEqualTo(Instant.EPOCH);
        }
    }

    @PerProvider
    public void deserializesBlob(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            String str = "foo";
            String expected = Base64.getEncoder().encodeToString(str.getBytes());
            ShapeDeserializer de = codec.createDeserializer(("\"" + expected + "\"").getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBlob(PreludeSchemas.BLOB).array()).isEqualTo(str.getBytes(StandardCharsets.UTF_8));
        }
    }

    @PerProvider
    public void deserializesBoolean(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBoolean(PreludeSchemas.BOOLEAN)).isEqualTo(true);
        }
    }

    @PerProvider
    public void deserializesString(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("\"foo\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo("foo");
        }
    }

    @PerProvider
    public void deserializesList(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("[\"foo\",\"bar\"]".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            de.readList(PreludeSchemas.DOCUMENT, null, (ignore, firstList) -> {
                values.add(firstList.readString(PreludeSchemas.STRING));
            });

            assertThat(values).isEqualTo(listOf("foo", "bar"));
        }
    }

    @PerProvider
    public void deserializesEmptyList(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("[]".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            de.readList(PreludeSchemas.DOCUMENT, null, (ignore, listDe) -> {
                values.add(listDe.readString(PreludeSchemas.STRING));
            });

            assertThat(values).hasSize(0);
        }
    }

    @PerProvider
    public void throwsWhenReadListGetsObject(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> {
                de.readList(PreludeSchemas.DOCUMENT, values, (list, listDe) -> {
                    list.add(listDe.readString(PreludeSchemas.STRING));
                });
            });

            assertThat(e.getMessage()).contains("Expected a list");
        }
    }

    @PerProvider
    public void throwsWhenReadListGetsString(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("\"not a list\"".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> {
                de.readList(PreludeSchemas.DOCUMENT, values, (list, listDe) -> {
                    list.add(listDe.readString(PreludeSchemas.STRING));
                });
            });

            assertThat(e.getMessage()).contains("Expected a list");
        }
    }

    @PerProvider
    public void throwsWhenReadListGetsEmptyObject(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("{}".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> {
                de.readList(PreludeSchemas.DOCUMENT, values, (list, listDe) -> {
                    list.add(listDe.readString(PreludeSchemas.STRING));
                });
            });

            assertThat(e.getMessage()).contains("Expected a list");
        }
    }

    @PerProvider
    public void throwsOnUnfinishedList(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("[{}".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> {
                de.readList(PreludeSchemas.DOCUMENT, values, (list, listDe) -> {
                    list.add(listDe.readString(PreludeSchemas.STRING));
                });
            });

            assertThat(e.getMessage()).satisfiesAnyOf(
                    m -> assertThat(m).isEqualTo("Expected end of list, but found Object value"),
                    m -> assertThat(m).contains("Expected"));
        }
    }

    @PerProvider
    public void deserializesMap(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("{\"foo\":\"bar\",\"baz\":\"bam\"}".getBytes(StandardCharsets.UTF_8));
            Map<String, String> result = new LinkedHashMap<>();

            de.readStringMap(PreludeSchemas.DOCUMENT, result, (map, key, mapde) -> {
                map.put(key, mapde.readString(PreludeSchemas.STRING));
            });

            assertThat(result.values()).hasSize(2);
            assertThat(result).containsKey("foo");
            assertThat(result).containsKey("baz");
            assertThat(result.get("foo")).isEqualTo("bar");
            assertThat(result.get("baz")).isEqualTo("bam");
        }
    }

    @PerProvider
    public void deserializesStruct(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("{\"name\":\"Sam\",\"Color\":\"red\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();

            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                switch (member.memberName()) {
                    case "name":
                        assertThat(deser.readString(JsonTestData.BIRD.member("name"))).isEqualTo("Sam");
                        break;
                    case "color":
                        assertThat(deser.readString(JsonTestData.BIRD.member("color"))).isEqualTo("red");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected member: " + member);
                }
            });

            assertThat(members).containsExactly("name", "color");
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void deserializesFieldNamesAtWordProbeBoundaries(JsonSerdeProvider provider) {
        Schema schema = Schema.structureBuilder(ShapeId.from("smithy.test#ProbeBoundaries"))
                .putMember("shortPrefix", PreludeSchemas.STRING, new JsonNameTrait("a"))
                .putMember("sevenBytes", PreludeSchemas.STRING, new JsonNameTrait("aaaaaaa"))
                .putMember("eightBytes", PreludeSchemas.STRING, new JsonNameTrait("aaaaaaaa"))
                .putMember("sixUtf8Bytes", PreludeSchemas.STRING, new JsonNameTrait("ééé"))
                .putMember("eightUtf8Bytes", PreludeSchemas.STRING, new JsonNameTrait("éééé"))
                .build();
        String json = "{\"aaaaaaaa\":\"8\",\"a\":\"1\",\"aaaaaaa\":\"7\","
                + "\"ééé\":\"6\",\"éééé\":\"8u\"}";
        Map<String, String> values = new LinkedHashMap<>();

        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8))
                    .readStruct(schema,
                            values,
                            (state, member, deser) -> state.put(member.memberName(), deser.readString(member)));
        }

        assertThat(values).isEqualTo(mapOf(
                        "shortPrefix",
                        "1",
                        "sevenBytes",
                        "7",
                        "eightBytes",
                        "8",
                        "sixUtf8Bytes",
                        "6",
                        "eightUtf8Bytes",
                        "8u"));
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void fallsBackWhenFieldNameIsTooCloseToBufferEndForWordRead(JsonSerdeProvider provider) {
        Schema schema = Schema.structureBuilder(ShapeId.from("smithy.test#ShortBuffer"))
                .putMember("a", PreludeSchemas.INTEGER)
                .build();
        AtomicReference<Integer> value = new AtomicReference<>();

        try (JsonCodec codec = codec(provider)) {
            codec.createDeserializer("{\"a\":0}".getBytes(StandardCharsets.UTF_8))
                    .readStruct(schema, value, (state, member, deser) -> state.set(deser.readInteger(member)));
        }

        assertThat(value.get()).isEqualTo(0);
    }

    @PerProvider
    public void deserializesUnion(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("{\"booleanValue\":true}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();

            de.readStruct(JsonTestData.UNION, members, new ShapeDeserializer.StructMemberConsumer<Set<String>>() {
                @Override
                public void accept(Set<String> memberResult, Schema member, ShapeDeserializer deser) {
                    memberResult.add(member.memberName());
                    if (member.memberName().equals("booleanValue")) {
                        assertThat(deser.readBoolean(JsonTestData.UNION.member("booleanValue"))).isEqualTo(true);
                    } else {
                        throw new IllegalStateException("Unexpected member: " + member);
                    }
                }

                @Override
                public void unknownMember(Set<String> state, String memberName) {
                    Assertions.fail("Should not have detected an unknown member: " + memberName);
                }
            });

            assertThat(members).containsExactly("booleanValue");
        }
    }

    @PerProvider
    public void deserializesUnknownUnion(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer("{\"totallyUnknown!\":3.14}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();

            AtomicReference<String> unknownSet = new AtomicReference<>();
            de.readStruct(JsonTestData.UNION, members, new ShapeDeserializer.StructMemberConsumer<Set<String>>() {
                @Override
                public void accept(Set<String> state, Schema memberSchema, ShapeDeserializer memberDeserializer) {
                    Assertions.fail("Unexpected member: " + memberSchema);
                }

                @Override
                public void unknownMember(Set<String> state, String memberName) {
                    unknownSet.set(memberName);
                }
            });

            assertThat(unknownSet.get()).isEqualTo("totallyUnknown!");
        }
    }

    @PerProvider
    public void skipsUnknownMembers(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"name\":\"Sam\",\"Ignore\":[1,2,3],\"Color\":\"rainbow\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();

            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                switch (member.memberName()) {
                    case "name":
                        assertThat(deser.readString(JsonTestData.BIRD.member("name"))).isEqualTo("Sam");
                        break;
                    case "color":
                        assertThat(deser.readString(JsonTestData.BIRD.member("color"))).isEqualTo("rainbow");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected member: " + member);
                }
            });

            assertThat(members).containsExactly("name", "color");
        }
    }

    @ParameterizedTest
    @MethodSource("deserializesBirdWithJsonNameOrNotSource")
    public void deserializesBirdWithJsonNameOrNot(JsonSerdeProvider provider, boolean useJsonName, String input) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(useJsonName).build()) {
            ShapeDeserializer de = codec.createDeserializer(input.getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                switch (member.memberName()) {
                    case "name":
                        assertThat(deser.readString(JsonTestData.BIRD.member("name"))).isEqualTo("Sam");
                        break;
                    case "color":
                        assertThat(deser.readString(JsonTestData.BIRD.member("color"))).isEqualTo("red");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected member: " + member);
                }
            });
            assertThat(members).containsExactly("name", "color");
        }
    }

    public static List<Arguments> deserializesBirdWithJsonNameOrNotSource() {
        List<Arguments> testCases = listOf(
                Arguments.of(true, "{\"name\":\"Sam\",\"Color\":\"red\"}"),
                Arguments.of(false, "{\"name\":\"Sam\",\"color\":\"red\"}"));

        // Cross-product with providers
        ArrayList<Arguments> result = new ArrayList<Arguments>();
        for (Arguments provider : providers()) {
            for (Arguments testCase : testCases) {
                result.add(Arguments.of(
                        provider.get()[0],
                        testCase.get()[0],
                        testCase.get()[1]));
            }
        }
        return result;
    }

    @PerProvider
    public void readsDocuments(JsonSerdeProvider provider) {
        byte[] json = "{\"name\":\"Sam\",\"color\":\"red\"}".getBytes(StandardCharsets.UTF_8);

        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer(json);
            Document document = de.readDocument();

            assertThat(document.type()).isEqualTo(ShapeType.MAP);
            Map<String, Document> map = document.asMap();
            assertThat(map.values()).hasSize(2);
            assertThat(map.get("name").asString()).isEqualTo("Sam");
            assertThat(map.get("color").asString()).isEqualTo("red");
        }
    }

    @ParameterizedTest
    @MethodSource("deserializesWithTimestampFormatSource")
    public void deserializesWithTimestampFormat(
            JsonSerdeProvider provider,
            boolean useTrait,
            TimestampFormatTrait trait,
            TimestampFormatter defaultFormat,
            String json
    ) {
        Trait[] traits = trait == null ? new Trait[0] : new Trait[] {trait};
        Schema schema = Schema.createTimestamp(ShapeId.from("smithy.foo#Time"), traits);

        JsonCodec.Builder builder = codecBuilder(provider).useTimestampFormat(useTrait);
        if (defaultFormat != null) {
            // Adapted: the SDK settings take a TimestampFormatTrait.Format (CodecSettings, task 3.5).
            builder.defaultTimestampFormat(defaultFormat.format());
        }

        try (JsonCodec codec = builder.build()) {
            ShapeDeserializer de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
            assertThat(de.readTimestamp(schema)).isEqualTo(Instant.EPOCH);
        }
    }

    public static List<Arguments> deserializesWithTimestampFormatSource() {
        String epochSeconds = Double.toString(((double) Instant.EPOCH.toEpochMilli()) / 1000);

        List<Arguments> testCases = listOf(
                // boolean useTrait, TimestampFormatTrait trait, TimestampFormatter defaultFormat, String json
                Arguments.of(false, null, null, epochSeconds),
                Arguments.of(false, new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS), null, epochSeconds),
                Arguments.of(
                        false,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.EPOCH_SECONDS,
                        epochSeconds),
                Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.EPOCH_SECONDS,
                        epochSeconds),
                Arguments.of(true, new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS), null, epochSeconds),
                Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.DATE_TIME,
                        epochSeconds),
                Arguments.of(
                        false,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.DATE_TIME,
                        "\"" + Instant.EPOCH + "\""),
                Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME),
                        TimestampFormatter.Prelude.EPOCH_SECONDS,
                        "\"" + Instant.EPOCH + "\""));

        // Cross-product with providers
        ArrayList<Arguments> result = new ArrayList<Arguments>();
        for (Arguments provider : providers()) {
            for (Arguments testCase : testCases) {
                result.add(Arguments.of(
                        provider.get()[0],
                        testCase.get()[0],
                        testCase.get()[1],
                        testCase.get()[2],
                        testCase.get()[3]));
            }
        }
        return result;
    }

    @PerProvider
    public void throwsWhenTimestampIsWrongType(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(ShapeId.from("smithy.foo#Time"));

        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> de.readTimestamp(schema));
            assertThat(e.getMessage()).contains("Expected a timestamp");
        }
    }

    @PerProvider
    public void ignoresTypeOnUnions(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"__type\":\"foo\", \"booleanValue\":true}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();

            de.readStruct(JsonTestData.UNION, members, new ShapeDeserializer.StructMemberConsumer<Set<String>>() {
                @Override
                public void accept(Set<String> memberResult, Schema member, ShapeDeserializer deser) {
                    memberResult.add(member.memberName());
                    if (member.memberName().equals("booleanValue")) {
                        assertThat(deser.readBoolean(JsonTestData.UNION.member("booleanValue"))).isEqualTo(true);
                    } else {
                        throw new IllegalStateException("Unexpected member: " + member);
                    }
                }

                @Override
                public void unknownMember(Set<String> state, String memberName) {
                    Assertions.fail("Should not have detected an unknown member: " + memberName);
                }
            });

            assertThat(members).containsExactly("booleanValue");
        }
    }

    @PerProvider
    public void rejectsInvalidEscapeInSkippedValue(JsonSerdeProvider provider) {
        // Unknown field "x" has value with invalid escape \e
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"x\":\"hello\\eworld\",\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> {
                    deser.readString(member);
                });
            }
        });
    }

    @PerProvider
    public void rejectsControlCharInSkippedValue(JsonSerdeProvider provider) {
        // Unknown field "x" has value with unescaped null byte
        byte[] input = "{\"x\":\"ab\u0000cd\",\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8);
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(input);
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> deser.readString(member));
            }
        });
    }

    @PerProvider
    public void rejectsLeadingZeroInSkippedNumber(JsonSerdeProvider provider) {
        // Unknown field "x" has value with leading zero (00.5)
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"x\":00.5,\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> deser.readString(member));
            }
        });
    }

    @PerProvider
    public void rejectsDoubleExponentInSkippedNumber(JsonSerdeProvider provider) {
        // Unknown field "x" has malformed number with double exponent
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"x\":1e2e3,\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> deser.readString(member));
            }
        });
    }

    @PerProvider
    public void rejectsControlCharInUnknownFieldName(JsonSerdeProvider provider) {
        // Field name contains unescaped null byte
        byte[] input = new byte[] {
                '{',
                '"',
                'a',
                0x00,
                'b',
                '"',
                ':',
                '1',
                ',',
                '"',
                'n',
                'a',
                'm',
                'e',
                '"',
                ':',
                '"',
                'S',
                '"',
                '}'
        };
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(input);
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> deser.readString(member));
            }
        });
    }

    @PerProvider
    public void rejectsInvalidEscapeInUnknownFieldName(JsonSerdeProvider provider) {
        // Field name contains invalid escape \e
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"a\\eb\":1,\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> deser.readString(member));
            }
        });
    }

    @PerProvider
    public void resolvesEscapedFieldNamesOutOfOrder(JsonSerdeProvider provider) {
        String json = "{"
                + "\"héllo\u00e9\":\"e\","
                + "\"has\\u0001control\":\"d\","
                + "\"has\\\\slash\":\"b\","
                + "\"has\\ttab\":\"c\","
                + "\"has\\\"quote\":\"a\""
                + "}";
        Map<String, String> values = new LinkedHashMap<>();

        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8))
                    .readStruct(JsonTestData.ESCAPED_FIELD_NAMES,
                            values,
                            (state, member, deser) -> state.put(member.memberName(), deser.readString(member)));
        }

        assertThat(values).isEqualTo(mapOf(
                        "quoted",
                        "a",
                        "slashed",
                        "b",
                        "tabbed",
                        "c",
                        "controlled",
                        "d",
                        "unicode",
                        "e"));
    }

    @PerProvider
    public void resolvesGratuitouslyEscapedFieldName(JsonSerdeProvider provider) {
        String json = "{\"\\u0068as\\\"\\u0071uote\":\"a\"}";
        Map<String, String> values = new LinkedHashMap<>();

        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8))
                    .readStruct(JsonTestData.ESCAPED_FIELD_NAMES, values, (state, member, deser) -> {
                        state.put(member.memberName(), deser.readString(member));
                    });
        }

        assertThat(values.get("quoted")).isEqualTo("a");
    }

    @PerProvider
    public void reportsUnknownMemberByDecodedName(JsonSerdeProvider provider) {
        String json = "{\"un\\tknown\":\"x\",\"has\\\"quote\":\"a\"}";
        List<String> unknownNames = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();

        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8))
                    .readStruct(
                            JsonTestData.ESCAPED_FIELD_NAMES,
                            values,
                            new ShapeDeserializer.StructMemberConsumer<Map<String, String>>() {
                                @Override
                                public void accept(
                                        Map<String, String> state,
                                        Schema member,
                                        ShapeDeserializer deser
                                ) {
                                    state.put(member.memberName(), deser.readString(member));
                                }

                                @Override
                                public void unknownMember(Map<String, String> state, String memberName) {
                                    unknownNames.add(memberName);
                                }
                            });
        }

        assertThat(unknownNames).containsExactly("un\tknown");
        assertThat(values.get("quoted")).isEqualTo("a");
    }

    @PerProvider
    public void rejectsInvalidEscapeInSkippedObjectKey(JsonSerdeProvider provider) {
        // Unknown field "x" has an object value whose key contains invalid escape
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"x\":{\"a\\eb\":1},\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> {
                    deser.readString(member);
                });
            }
        });
    }

    @PerProvider
    public void stillSkipsValidUnknownFields(JsonSerdeProvider provider) {
        // Valid unknown fields should still be skipped successfully
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"unknown\":{\"a\":1,\"b\":[true,null,\"x\"]},\"name\":\"Sam\",\"extra\":1.5e10}"
                            .getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                if (member.memberName().equals("name")) {
                    assertThat(deser.readString(JsonTestData.BIRD.member("name"))).isEqualTo("Sam");
                }
            });
            assertThat(members).containsExactly("name");
        }
    }

    @PerProvider
    public void deserializesLongMinValue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("-9223372036854775808".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readLong(PreludeSchemas.LONG)).isEqualTo(Long.MIN_VALUE);
        }
    }

    @PerProvider
    public void deserializesLongMaxValue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("9223372036854775807".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readLong(PreludeSchemas.LONG)).isEqualTo(Long.MAX_VALUE);
        }
    }

    @PerProvider
    public void rejectsLongOverflowPositive(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("9223372036854775808".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readLong(PreludeSchemas.LONG));
        }
    }

    @PerProvider
    public void rejectsLongOverflowNegative(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("-9223372036854775809".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readLong(PreludeSchemas.LONG));
        }
    }

    @PerProvider
    public void deserializesIntegerMinMaxValues(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("-2147483648".getBytes(StandardCharsets.UTF_8))
                    .readInteger(PreludeSchemas.INTEGER)).isEqualTo(Integer.MIN_VALUE);
            assertThat(codec.createDeserializer("2147483647".getBytes(StandardCharsets.UTF_8))
                    .readInteger(PreludeSchemas.INTEGER)).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @PerProvider
    public void rejectsIntegerOverflow(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("2147483648".getBytes(StandardCharsets.UTF_8))
                            .readInteger(PreludeSchemas.INTEGER));
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("-2147483649".getBytes(StandardCharsets.UTF_8))
                            .readInteger(PreludeSchemas.INTEGER));
        }
    }

    @PerProvider
    public void rejectsLeadingZerosInLong(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("01".getBytes(StandardCharsets.UTF_8))
                            .readLong(PreludeSchemas.LONG));
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("007".getBytes(StandardCharsets.UTF_8))
                            .readLong(PreludeSchemas.LONG));
        }
    }

    @PerProvider
    public void parsesNegativeZero(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("-0".getBytes(StandardCharsets.UTF_8))
                    .readLong(PreludeSchemas.LONG)).isEqualTo(0L);
        }
    }

    @PerProvider
    public void rejectsBareMinusSign(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("-".getBytes(StandardCharsets.UTF_8))
                            .readLong(PreludeSchemas.LONG));
        }
    }

    @PerProvider
    public void deserializesByteMinMax(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("-128".getBytes(StandardCharsets.UTF_8))
                    .readByte(PreludeSchemas.BYTE)).isEqualTo(Byte.MIN_VALUE);
            assertThat(codec.createDeserializer("127".getBytes(StandardCharsets.UTF_8))
                    .readByte(PreludeSchemas.BYTE)).isEqualTo(Byte.MAX_VALUE);
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsByteOverflow(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("128".getBytes(StandardCharsets.UTF_8))
                            .readByte(PreludeSchemas.BYTE));
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("-129".getBytes(StandardCharsets.UTF_8))
                            .readByte(PreludeSchemas.BYTE));
        }
    }

    @PerProvider
    public void deserializesShortMinMax(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("-32768".getBytes(StandardCharsets.UTF_8))
                    .readShort(PreludeSchemas.SHORT)).isEqualTo(Short.MIN_VALUE);
            assertThat(codec.createDeserializer("32767".getBytes(StandardCharsets.UTF_8))
                    .readShort(PreludeSchemas.SHORT)).isEqualTo(Short.MAX_VALUE);
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsShortOverflow(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("32768".getBytes(StandardCharsets.UTF_8))
                            .readShort(PreludeSchemas.SHORT));
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("-32769".getBytes(StandardCharsets.UTF_8))
                            .readShort(PreludeSchemas.SHORT));
        }
    }

    @PerProvider
    public void parsesDoubleWithLeadingZeroBeforeDecimal(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("0.5".getBytes(StandardCharsets.UTF_8))
                    .readDouble(PreludeSchemas.DOUBLE)).isEqualTo(0.5);
        }
    }

    @PerProvider
    public void rejectsDoubleWithDoubleLeadingZero(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("00.5".getBytes(StandardCharsets.UTF_8))
                            .readDouble(PreludeSchemas.DOUBLE));
        }
    }

    @PerProvider
    public void parsesDoubleWithExponent(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("1e10".getBytes(StandardCharsets.UTF_8))
                    .readDouble(PreludeSchemas.DOUBLE)).isEqualTo(1e10);
            assertThat(codec.createDeserializer("1E-10".getBytes(StandardCharsets.UTF_8))
                    .readDouble(PreludeSchemas.DOUBLE)).isEqualTo(1E-10);
            assertThat(codec.createDeserializer("1.5e+3".getBytes(StandardCharsets.UTF_8))
                    .readDouble(PreludeSchemas.DOUBLE)).isEqualTo(1.5e+3);
        }
    }

    @PerProvider
    public void parsesAllEscapeSequences(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer(
                    "\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo("\" \\ / \b \f \n \r \t");
        }
    }

    @PerProvider
    public void parsesUnicodeEscapes(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            // Basic unicode escape
            assertThat(codec.createDeserializer("\"\\u0041\"".getBytes(StandardCharsets.UTF_8))
                    .readString(PreludeSchemas.STRING)).isEqualTo("A");
            // Lowercase hex
            assertThat(codec.createDeserializer("\"\\u00e9\"".getBytes(StandardCharsets.UTF_8))
                    .readString(PreludeSchemas.STRING)).isEqualTo("\u00e9");
            // Uppercase hex
            assertThat(codec.createDeserializer("\"\\u00E9\"".getBytes(StandardCharsets.UTF_8))
                    .readString(PreludeSchemas.STRING)).isEqualTo("\u00e9");
        }
    }

    @PerProvider
    public void parsesSurrogatePair(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            // U+1F600 (grinning face emoji)
            ShapeDeserializer de = codec.createDeserializer("\"\\uD83D\\uDE00\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo("\uD83D\uDE00");
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsLoneSurrogateHigh(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"\\uD83D\"".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsLoneSurrogateLow(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"\\uDE00\"".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsHighSurrogateFollowedByNonLow(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"\\uD83D\\u0041\"".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    static List<Arguments> smithyOnly() {
        return listOf(Arguments.of(SMITHY));
    }

    @PerProvider
    public void rejectsInvalidEscapeSequence(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"\\x\"".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    @PerProvider
    public void rejectsControlCharInString(JsonSerdeProvider provider) {
        byte[] input = new byte[] {'"', 0x01, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @PerProvider
    public void rejectsUnterminatedString(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"hello".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    @PerProvider
    public void rejectsIncompleteUnicodeEscape(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"\\u00\"".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    @PerProvider
    public void rejectsInvalidHexInUnicodeEscape(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("\"\\uGGGG\"".getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING));
        }
    }

    @PerProvider
    public void parsesAsciiAtStringScanBoundaries(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            for (int length : listOf(0, 6, 7, 8, 14, 15, 16, 23, 24)) {
                String value = repeat("a", length);
                assertThat(codec.createDeserializer(("\"" + value + "\"").getBytes(StandardCharsets.UTF_8))
                        .readString(PreludeSchemas.STRING)).isEqualTo(value);
            }

            for (char value : listOf(' ', '!', '#', (char) 0x7F)) {
                for (int position : listOf(0, 6, 7, 8, 14, 15, 16)) {
                    String expected = repeat("a", position) + value + "z";
                    assertThat(codec.createDeserializer(("\"" + expected + "\"").getBytes(StandardCharsets.UTF_8))
                            .readString(PreludeSchemas.STRING)).isEqualTo(expected);
                }
            }
        }
    }

    @PerProvider
    public void parsesEscapesAtStringScanBoundaries(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            for (int position : listOf(0, 6, 7, 8, 14, 15, 16)) {
                String prefix = repeat("a", position);
                String input = "\"" + prefix + "\\nz\"";
                assertThat(codec.createDeserializer(input.getBytes(StandardCharsets.UTF_8))
                        .readString(PreludeSchemas.STRING)).isEqualTo(prefix + "\nz");
            }
        }
    }

    @PerProvider
    public void parsesUnescapedUtf8AtStringScanBoundaries(JsonSerdeProvider provider) {
        List<String> values = listOf(
                "é",
                "aaaaaé",
                "aaaaaaé",
                "aaaaaaaé",
                "aaaaaaaaé",
                "aaaaaaa中z",
                "aaaaaaa😀z",
                "中文字符串测试内容值中文字符串测试内容值");
        try (JsonCodec codec = codec(provider)) {
            for (String value : values) {
                assertThat(codec.createDeserializer(("\"" + value + "\"").getBytes(StandardCharsets.UTF_8))
                        .readString(PreludeSchemas.STRING)).isEqualTo(value);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsControlCharactersAfterUnescapedUtf8(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            for (int position : listOf(0, 5, 6, 7, 8, 13, 14, 15, 16)) {
                ByteArrayOutputStream input = new ByteArrayOutputStream();
                input.write('"');
                byte[] prefix = ("é" + repeat("a", position)).getBytes(StandardCharsets.UTF_8);
                input.write(prefix, 0, prefix.length);
                input.write(0x1F);
                input.write('"');

                SerializationException exception = Assertions.assertThrows(
                        SerializationException.class,
                        () -> codec.createDeserializer(input.toByteArray()).readString(PreludeSchemas.STRING));
                assertThat(exception.getMessage()).contains("control character 0x1f");
            }
        }
    }

    @PerProvider
    public void rejectsTruncatedTrue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("tru".getBytes(StandardCharsets.UTF_8))
                            .readBoolean(PreludeSchemas.BOOLEAN));
        }
    }

    @PerProvider
    public void rejectsTruncatedFalse(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("fals".getBytes(StandardCharsets.UTF_8))
                            .readBoolean(PreludeSchemas.BOOLEAN));
        }
    }

    @PerProvider
    public void rejectsCorruptLiteral(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("trux".getBytes(StandardCharsets.UTF_8))
                            .readBoolean(PreludeSchemas.BOOLEAN));
        }
    }

    @PerProvider
    public void readsNullLiteral(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("null".getBytes(StandardCharsets.UTF_8));
            assertTrue(de.isNull());
            assertNull(de.readNull());
        }
    }

    @PerProvider
    public void rejectsTruncatedNull(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer("nul".getBytes(StandardCharsets.UTF_8));
                de.readNull();
            }
        });
    }

    @PerProvider
    public void handlesLeadingWhitespace(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            assertThat(codec.createDeserializer("  \t\n\r 42".getBytes(StandardCharsets.UTF_8))
                    .readInteger(PreludeSchemas.INTEGER)).isEqualTo(42);
        }
    }

    @PerProvider
    public void handlesWhitespaceBetweenTokens(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "  {  \"name\"  :  \"Sam\"  ,  \"Color\"  :  \"red\"  }  ".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                deser.readString(member);
            });
            assertThat(members).containsExactly("name", "color");
        }
    }

    @PerProvider
    public void documentParsesLargeInteger(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            // Long range (exceeds int)
            ShapeDeserializer de = codec.createDeserializer("3000000000".getBytes(StandardCharsets.UTF_8));
            Document doc = de.readDocument();
            assertThat(doc.type()).isEqualTo(ShapeType.LONG);
            assertThat(doc.asLong()).isEqualTo(3000000000L);
        }
    }

    @PerProvider
    public void documentParsesBigInteger(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("99999999999999999999".getBytes(StandardCharsets.UTF_8));
            Document doc = de.readDocument();
            // Should be BigInteger since it exceeds Long range
            assertThat(doc.asBigInteger()).isEqualTo(new BigInteger("99999999999999999999"));
        }
    }

    @PerProvider
    public void documentParsesExponentAsDouble(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("1e5".getBytes(StandardCharsets.UTF_8));
            Document doc = de.readDocument();
            assertThat(doc.type()).isEqualTo(ShapeType.DOUBLE);
            assertThat(doc.asDouble()).isEqualTo(1e5);
        }
    }

    @PerProvider
    public void documentParsesNegativeNumber(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("-42".getBytes(StandardCharsets.UTF_8));
            Document doc = de.readDocument();
            assertThat(doc.type()).isEqualTo(ShapeType.INTEGER);
            assertThat(doc.asInteger()).isEqualTo(-42);
        }
    }

    @PerProvider
    public void rejectsTrailingContentAfterClose(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("42 extra".getBytes(StandardCharsets.UTF_8));
            de.readInteger(PreludeSchemas.INTEGER);
            Assertions.assertThrows(SerializationException.class, () -> de.close());
        }
    }

    @PerProvider
    public void skipsDeepNestedUnknownFields(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    ("{\"unknown\":{\"a\":{\"b\":[1,{\"c\":true}]},"
                            + "\"d\":\"test\"},\"name\":\"Sam\"}")
                            .getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                if (member.memberName().equals("name")) {
                    assertThat(deser.readString(JsonTestData.BIRD.member("name"))).isEqualTo("Sam");
                }
            });
            assertThat(members).containsExactly("name");
        }
    }

    @PerProvider
    public void rejectsInvalidEscapeInSkippedNestedObjectKey(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"x\":{\"a\\qb\":1},\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
                de.readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, member, deser) -> {
                    deser.readString(member);
                });
            }
        });
    }

    @PerProvider
    public void skipsStringWithValidEscapesInUnknownValue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            // Use JSON escapes that are valid: \n, \t, \\, \", \u0041
            String json = "{\"x\":\"hello\\nworld\\t\\u0041\",\"name\":\"Sam\"}";
            ShapeDeserializer de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                deser.readString(member);
            });
            assertThat(members).containsExactly("name");
        }
    }

    @PerProvider
    public void rejectsCorruptFalse(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("faxxx".getBytes(StandardCharsets.UTF_8))
                        .readBoolean(PreludeSchemas.BOOLEAN);
            }
        });
    }

    @PerProvider
    public void rejectsInvalidBase64InBlob(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("\"!!!not-base64!!!\"".getBytes(StandardCharsets.UTF_8))
                        .readBlob(PreludeSchemas.BLOB);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void readsMixedEpochSecondsAndMaintainsTheCursor(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS));
        String json = "[1000000000, 1, 1712345678.25, 999999999, 9999999999]";

        try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
            ArrayList<Instant> read = new ArrayList<Instant>();
            codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8))
                    .readList(PreludeSchemas.DOCUMENT, read, (sink, de) -> sink.add(de.readTimestamp(schema)));

            assertThat(read).containsExactly(
                            Instant.ofEpochSecond(1_000_000_000L),
                            Instant.ofEpochSecond(1),
                            Instant.ofEpochSecond(1_712_345_678L, 250_000_000),
                            Instant.ofEpochSecond(999_999_999L),
                            Instant.ofEpochSecond(9_999_999_999L));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsEpochSecondsOutOfRange(JsonSerdeProvider provider) {
        // Instant.MAX.getEpochSecond() is 31556889864403199; one beyond that overflows
        long outOfRange = Instant.MAX.getEpochSecond() + 1;
        Schema schema = Schema.createTimestamp(ShapeId.from("smithy.foo#Time"));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer(
                        Long.toString(outOfRange).getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("epochSecondsWithExponentSource")
    public void parsesEpochSecondsWithExponent(JsonSerdeProvider provider, String json, Instant expected) {
        // Regression: a fractional epoch-seconds value that also carries an exponent (e.g. "1.5e3")
        // used to take the nanosecond-precision fast path, which ignored the exponent — decoding the
        // fraction as nanos and leaving the cursor before the 'e'. That both produced the wrong Instant
        // and corrupted subsequent parsing. Such values must fall through to full double parsing.
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS));
        try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
            assertThat(de.readTimestamp(schema)).isEqualTo(expected);
        }
    }

    public static List<Arguments> epochSecondsWithExponentSource() {
        List<Arguments> args = new ArrayList<>();
        for (JsonSerdeProvider provider : providerInstances()) {
            // Fraction + exponent: 1.5e3 == 1500s. The buggy path returned Instant(1, 500_000_000).
            args.add(Arguments.of(provider, "1.5e3", Instant.ofEpochSecond(1500)));
            // Uppercase exponent.
            args.add(Arguments.of(provider, "1.5E3", Instant.ofEpochSecond(1500)));
            // Fraction that survives into the result: 1.5e1 == 15s.
            args.add(Arguments.of(provider, "1.5e1", Instant.ofEpochSecond(15)));
            // Negative exponent keeps a sub-second fraction: 1500e-3 == 1.5s.
            args.add(Arguments.of(provider, "1500e-3", Instant.ofEpochSecond(1, 500_000_000)));
            // No fractional part, exponent only: 1e3 == 1000s (already worked; guards against regression).
            args.add(Arguments.of(provider, "1e3", Instant.ofEpochSecond(1000)));
        }
        return args;
    }

    @PerProvider
    public void timestampFallbackForOffsetTimezone(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME));
        try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
            // Offset timezone causes fast-path parseIso8601 to return null, falling back to DateTimeFormatter
            ShapeDeserializer de = codec.createDeserializer(
                    "\"2025-01-15T10:30:00+00:00\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readTimestamp(schema)).isEqualTo(Instant.parse("2025-01-15T10:30:00Z"));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsNonObjectForStruct(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("123".getBytes(StandardCharsets.UTF_8))
                        .readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, m, d) -> {});
            }
        });
    }

    @PerProvider
    public void rejectsNonQuotedFieldName(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("{123:1}".getBytes(StandardCharsets.UTF_8))
                        .readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, m, d) -> {});
            }
        });
    }

    @PerProvider
    public void rejectsUnterminatedFieldName(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("{\"unterminated".getBytes(StandardCharsets.UTF_8))
                        .readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, m, d) -> {});
            }
        });
    }

    @PerProvider
    public void rejectsMissingColonInStruct(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("{\"name\" \"Sam\"}".getBytes(StandardCharsets.UTF_8))
                        .readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, m, d) -> {});
            }
        });
    }

    @PerProvider
    public void nullMemberValueSkippedInStruct(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"name\":null,\"Color\":\"red\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                deser.readString(member);
            });
            // "name" was null so consumer was never called for it
            assertThat(members).containsExactly("color");
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsNonObjectForMap(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("[1]".getBytes(StandardCharsets.UTF_8))
                        .readStringMap(PreludeSchemas.DOCUMENT, null, (s, k, d) -> {});
            }
        });
    }

    @PerProvider
    public void rejectsMissingCommaInMap(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                Map<String, String> result = new LinkedHashMap<>();
                codec.createDeserializer("{\"a\":\"1\" \"b\":\"2\"}".getBytes(StandardCharsets.UTF_8))
                        .readStringMap(PreludeSchemas.DOCUMENT,
                                result,
                                (m, k, d) -> m.put(k, d.readString(PreludeSchemas.STRING)));
            }
        });
    }

    @PerProvider
    public void rejectsMissingColonInMap(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                Map<String, String> result = new LinkedHashMap<>();
                codec.createDeserializer("{\"a\" \"1\"}".getBytes(StandardCharsets.UTF_8))
                        .readStringMap(PreludeSchemas.DOCUMENT,
                                result,
                                (m, k, d) -> m.put(k, d.readString(PreludeSchemas.STRING)));
            }
        });
    }

    @PerProvider
    public void skipsFalseLiteralInUnknownValue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"x\":false,\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                deser.readString(member);
            });
            assertThat(members).containsExactly("name");
        }
    }

    @PerProvider
    public void skipsEmptyObjectInUnknownValue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"x\":{},\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                deser.readString(member);
            });
            assertThat(members).containsExactly("name");
        }
    }

    @PerProvider
    public void skipsEmptyArrayInUnknownValue(JsonSerdeProvider provider) {
        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            ShapeDeserializer de = codec.createDeserializer(
                    "{\"x\":[],\"name\":\"Sam\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                deser.readString(member);
            });
            assertThat(members).containsExactly("name");
        }
    }

    @PerProvider
    public void rejectsUnterminatedStringInSkippedValue(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("{\"x\":\"unterminated".getBytes(StandardCharsets.UTF_8))
                        .readStruct(JsonTestData.BIRD, new LinkedHashSet<>(), (s, m, d) -> {});
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsInvalidMonthInHttpDate(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu, 01 Xyz 2025 00:00:00 GMT\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @PerProvider
    public void rejectsMissingCommaInList(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("[1 2]".getBytes(StandardCharsets.UTF_8))
                        .readList(PreludeSchemas.DOCUMENT, null, (s, d) -> d.readInteger(PreludeSchemas.INTEGER));
            }
        });
    }

    @PerProvider
    public void rejectsNullAsTimestamp(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(ShapeId.from("smithy.foo#Time"));
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("null".getBytes(StandardCharsets.UTF_8));
            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> de.readTimestamp(schema));
            assertThat(e.getMessage()).contains("Expected a timestamp");
        }
    }

    @PerProvider
    public void rejectsArrayAsTimestamp(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(ShapeId.from("smithy.foo#Time"));
        try (JsonCodec codec = codec(provider)) {
            ShapeDeserializer de = codec.createDeserializer("[1]".getBytes(StandardCharsets.UTF_8));
            SerializationException e = Assertions.assertThrows(SerializationException.class, () -> de.readTimestamp(schema));
            assertThat(e.getMessage()).contains("Expected a timestamp");
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void parseLongRejectsEmptyInput(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(new byte[0]).readLong(PreludeSchemas.LONG));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void parseLongRejectsNonDigitInput(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("abc".getBytes(StandardCharsets.UTF_8))
                            .readLong(PreludeSchemas.LONG));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void parseDoubleRejectsBareMinusSign(JsonSerdeProvider provider) {
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer("-".getBytes(StandardCharsets.UTF_8))
                            .readDouble(PreludeSchemas.DOUBLE));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsOverlong2ByteUtf8InString(JsonSerdeProvider provider) {
        // 0xC0 0x80 is an overlong encoding of U+0000 (RFC 3629 forbids this)
        byte[] input = {'"', '\\', 'n', (byte) 0xC0, (byte) 0x80, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsTruncated2ByteUtf8InString(JsonSerdeProvider provider) {
        // 0xC2 is a 2-byte sequence lead but the continuation byte is missing
        byte[] input = {'"', '\\', 'n', (byte) 0xC2, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsOverlong3ByteUtf8InString(JsonSerdeProvider provider) {
        // 0xE0 0x80 0x80 is an overlong encoding of U+0000
        byte[] input = {'"', '\\', 'n', (byte) 0xE0, (byte) 0x80, (byte) 0x80, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsTruncated3ByteUtf8InString(JsonSerdeProvider provider) {
        // 0xE4 0xB8 is a 3-byte lead + first continuation, missing the third byte
        byte[] input = {'"', '\\', 'n', (byte) 0xE4, (byte) 0xB8, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsTruncated4ByteUtf8InString(JsonSerdeProvider provider) {
        // 0xF0 0x9F 0x98 is a 4-byte sequence missing the last continuation byte
        byte[] input = {'"', '\\', 'n', (byte) 0xF0, (byte) 0x9F, (byte) 0x98, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsInvalidUtf8StartByteInString(JsonSerdeProvider provider) {
        // 0xFF is never a valid UTF-8 start byte
        byte[] input = {'"', '\\', 'n', (byte) 0xFF, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsControlCharAfterEscapeInString(JsonSerdeProvider provider) {
        // RFC 8259 requires control chars (U+0000..U+001F) to be escaped
        byte[] input = {'"', '\\', 'n', 0x01, '"'};
        try (JsonCodec codec = codec(provider)) {
            Assertions.assertThrows(SerializationException.class,
                    () -> codec.createDeserializer(input).readString(PreludeSchemas.STRING));
        }
    }

    @Disabled("DateTimeFormatter fallback accepts trailing garbage after Z")
    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void iso8601WithExtraCharsAfterZShouldBeRejected(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-01-15T10:30:00Zextra\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @Disabled("DateTimeFormatter fallback accepts missing space after comma")
    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void httpDateShouldRejectMissingSpaceAfterComma(JsonSerdeProvider provider) {
        // RFC 7231 requires "day-name, SP" format
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu,01 Jan 2025 00:00:00 GMT\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @Disabled("DateTimeFormatter fallback accepts UTC instead of GMT")
    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void httpDateShouldRejectNonGmtTimezone(JsonSerdeProvider provider) {
        // RFC 7231 HTTP-date requires "GMT"
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu, 01 Jan 2025 00:00:00 UTC\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @Disabled("DateTimeFormatter fallback accepts dash delimiters")
    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void httpDateShouldRejectDashInsteadOfSpace(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu, 01-Jan 2025 00:00:00 GMT\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @Disabled("DateTimeFormatter fallback accepts dash instead of colon in time")
    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void httpDateShouldRejectDashInsteadOfColonInTime(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu, 01 Jan 2025 00-00:00 GMT\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void httpDateRejectsInvalidMonthStartingWithMa(JsonSerdeProvider provider) {
        // "Max" is neither "Mar" nor "May"
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu, 01 Max 2025 00:00:00 GMT\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void httpDateRejectsInvalidMonthStartingWithJu(JsonSerdeProvider provider) {
        // "Jux" is neither "Jun" nor "Jul"
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE));
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"Thu, 01 Jux 2025 00:00:00 GMT\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsInvalidMonthInIso8601(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME));
        // Month 13
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-13-01T00:00:00Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
        // Month 0
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-00-01T00:00:00Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsInvalidDayInIso8601(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME));
        // Day 32
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-01-32T00:00:00Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
        // Day 0
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-01-00T00:00:00Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsInvalidTimeInIso8601(JsonSerdeProvider provider) {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.foo#Time"),
                new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME));
        // Hour 25
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-01-01T25:00:00Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
        // Minute 60
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-01-01T00:60:00Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
        // Second 60
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build()) {
                codec.createDeserializer(
                        "\"2025-01-01T00:00:60Z\"".getBytes(StandardCharsets.UTF_8))
                        .readTimestamp(schema);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsInvalidContinuationByteInUtf8(JsonSerdeProvider provider) {
        // 2-byte lead 0xC2 followed by ASCII 'A' (0x41) instead of valid continuation byte.
        // Prefix with "\n" to force entry into the slow path (escape triggers decodeUtf8Char for following bytes).
        byte[] json = new byte[] {'"', '\\', 'n', (byte) 0xC2, 0x41, '"'};
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer(json).readString(PreludeSchemas.STRING);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void rejectsSurrogateCodePointInUtf8(JsonSerdeProvider provider) {
        // 3-byte sequence [0xED, 0xA0, 0x80] encodes U+D800 (high surrogate) as UTF-8.
        // Prefix with "\n" to force slow path.
        byte[] json = new byte[] {'"', '\\', 'n', (byte) 0xED, (byte) 0xA0, (byte) 0x80, '"'};
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer(json).readString(PreludeSchemas.STRING);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void readBigIntegerRejectsDecimalInput(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("1.5".getBytes(StandardCharsets.UTF_8))
                        .readBigInteger(PreludeSchemas.BIG_INTEGER);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void readBigIntegerRejectsExponentInput(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                codec.createDeserializer("1e5".getBytes(StandardCharsets.UTF_8))
                        .readBigInteger(PreludeSchemas.BIG_INTEGER);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("smithyOnly")
    public void readBigDecimalRejectsInvalidFormat(JsonSerdeProvider provider) {
        // findNumberEnd accepts "++1" but BigDecimal constructor rejects it
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codec(provider)) {
                // Start with valid digit prefix so findNumberEnd is entered, but overall format is invalid
                codec.createDeserializer("1+1".getBytes(StandardCharsets.UTF_8))
                        .readBigDecimal(PreludeSchemas.BIG_DECIMAL);
            }
        });
    }

    @PerProvider
    public void forbidUnknownUnionMembersThrows(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).forbidUnknownUnionMembers(true).build()) {
                ShapeDeserializer de = codec.createDeserializer(
                        "{\"totallyUnknown\":42}".getBytes(StandardCharsets.UTF_8));
                Set<String> members = new LinkedHashSet<>();
                de.readStruct(JsonTestData.UNION, members, new ShapeDeserializer.StructMemberConsumer<Set<String>>() {
                    @Override
                    public void accept(Set<String> state, Schema member, ShapeDeserializer deser) {}

                    @Override
                    public void unknownMember(Set<String> state, String memberName) {}
                });
            }
        });
    }

    @PerProvider
    public void rejectsUnterminatedObject(JsonSerdeProvider provider) {
        Assertions.assertThrows(SerializationException.class, () -> {
            try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
                // After reading first field, no '}' or ',' -> unterminated
                ShapeDeserializer de = codec.createDeserializer("{\"name\":\"Sam\"".getBytes(StandardCharsets.UTF_8));
                Set<String> members = new LinkedHashSet<>();
                de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                    memberResult.add(member.memberName());
                    deser.readString(member);
                });
            }
        });
    }

}
