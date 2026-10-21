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
import static software.amazon.awssdk.codecs.json.TestCollections.listOf;
import static software.amazon.awssdk.codecs.json.TestCollections.mapOf;
import static software.amazon.awssdk.codecs.json.TestCollections.repeat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSerdeProvider;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

public class JsonSerializerTest extends ProviderTestBase {

    // The initial buffer of a SmithyJsonSerializer created with a sink (SmithyJsonSerializer.SINK_BUF_SIZE).
    private static final int SINK_BUFFER_SIZE = 1024;

    @PerProvider
    public void writesNull(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ShapeSerializer serializer = codec.createSerializer(output);
        serializer.writeNull(PreludeSchemas.STRING);
        serializer.flush();
        String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertThat(result).isEqualTo("null");
    }

    @PerProvider
    public void writesDocumentsInline(JsonSerdeProvider provider) throws Exception {
        Document document = Document.of(listOf(Document.of("a")));

        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeDocument(PreludeSchemas.DOCUMENT, document);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("[\"a\"]");
        }
    }

    static List<Arguments> serializesJsonValuesProvider() {
        return listOf(
                Arguments.of(Document.of("a"), "\"a\""),
                Arguments.of(Document.of("a".getBytes(StandardCharsets.UTF_8)), "\"YQ==\""),
                Arguments.of(Document.of((byte) 1), "1"),
                Arguments.of(Document.of((short) 1), "1"),
                Arguments.of(Document.of(1), "1"),
                Arguments.of(Document.of(1L), "1"),
                Arguments.of(Document.of(1.1f), "1.1"),
                Arguments.of(Document.of(Float.NaN), "\"NaN\""),
                Arguments.of(Document.of(Float.POSITIVE_INFINITY), "\"Infinity\""),
                Arguments.of(Document.of(Float.NEGATIVE_INFINITY), "\"-Infinity\""),
                Arguments.of(Document.of(1.1), "1.1"),
                Arguments.of(Document.of(Double.NaN), "\"NaN\""),
                Arguments.of(Document.of(Double.POSITIVE_INFINITY), "\"Infinity\""),
                Arguments.of(Document.of(Double.NEGATIVE_INFINITY), "\"-Infinity\""),
                Arguments.of(Document.of(BigInteger.ZERO), "0"),
                Arguments.of(Document.of(BigDecimal.ONE), "1"),
                Arguments.of(Document.of(true), "true"),
                Arguments.of(Document.of(Instant.EPOCH), "0"),
                Arguments.of(Document.of(listOf(Document.of("a"))), "[\"a\"]"),
                Arguments.of(
                        Document.of(
                                listOf(
                                        Document.of(listOf(Document.of("a"), Document.of("b"))),
                                        Document.of("c"))),
                        "[[\"a\",\"b\"],\"c\"]"),
                Arguments.of(
                        Document.of(listOf(Document.of("a"), Document.of("b"))),
                        "[\"a\",\"b\"]"),
                Arguments.of(Document.of(mapOf("a", Document.of("av"))), "{\"a\":\"av\"}"),
                Arguments.of(Document.of(new LinkedHashMap<String, Document>() {
                    {
                        this.put("a", Document.of("av"));
                        this.put("b", Document.of("bv"));
                        this.put("c", Document.of(1));
                        this.put(
                                "d",
                                Document.of(listOf(Document.of(1), Document.of(2))));
                        this.put("e", Document.of(mapOf("ek", Document.of("ek1"))));
                    }
                }), "{\"a\":\"av\",\"b\":\"bv\",\"c\":1,\"d\":[1,2],\"e\":{\"ek\":\"ek1\"}}"));
    }

    static List<Arguments> serializesJsonValuesWithProvider() {
        List<Arguments> values = serializesJsonValuesProvider();
        List<Arguments> provs = providers();
        List<Arguments> result = new ArrayList<>();
        for (Arguments prov : provs) {
            for (Arguments val : values) {
                result.add(Arguments.of(prov.get()[0], val.get()[0], val.get()[1]));
            }
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("serializesJsonValuesWithProvider")
    public void serializesJsonValues(JsonSerdeProvider provider, Document value, String expected) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                value.serializeContents(serializer);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo(expected);
        }
    }

    static List<Arguments> configurableTimestampFormatWithProvider() {
        List<Arguments> configs = listOf(
                Arguments.of(true, "\"1970-01-01T00:00:00Z\""),
                Arguments.of(false, "0"));
        List<Arguments> provs = providers();
        List<Arguments> result = new ArrayList<>();
        for (Arguments prov : provs) {
            for (Arguments cfg : configs) {
                result.add(Arguments.of(prov.get()[0], cfg.get()[0], cfg.get()[1]));
            }
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("configurableTimestampFormatWithProvider")
    public void configurableTimestampFormat(
            JsonSerdeProvider provider,
            boolean useTimestampFormat,
            String json
    ) throws Exception {
        Schema schema = Schema.createTimestamp(
                ShapeId.from("smithy.example#foo"),
                new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME));
        try (
                JsonCodec codec = codecBuilder(provider)
                        .useTimestampFormat(useTimestampFormat)
                        .build();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeTimestamp(schema, Instant.EPOCH);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo(json);
        }
    }

    static List<Arguments> configurableJsonNameWithProvider() {
        List<Arguments> configs = listOf(
                Arguments.of(true, "{\"name\":\"Toucan\",\"Color\":\"red\"}"),
                Arguments.of(false, "{\"name\":\"Toucan\",\"color\":\"red\"}"));
        List<Arguments> provs = providers();
        List<Arguments> result = new ArrayList<>();
        for (Arguments prov : provs) {
            for (Arguments cfg : configs) {
                result.add(Arguments.of(prov.get()[0], cfg.get()[0], cfg.get()[1]));
            }
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("configurableJsonNameWithProvider")
    public void configurableJsonName(JsonSerdeProvider provider, boolean useJsonName, String json) throws Exception {
        try (
                JsonCodec codec = codecBuilder(provider).useJsonName(useJsonName).build();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeStruct(
                        JsonTestData.BIRD,
                        new SerializableStruct() {
                            @Override
                            public Schema sdkSchema() {
                                return JsonTestData.BIRD;
                            }

                            @Override
                            public void serializeMembers(ShapeSerializer ser) {
                                ser.writeString(sdkSchema().member("name"), "Toucan");
                                ser.writeString(sdkSchema().member("color"), "red");
                            }

                            @Override
                            public <T> T getMemberValue(Schema member) {
                                return null;
                            }
                        });
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo(json);
        }
    }

    @PerProvider
    public void escapesSpecialCharactersInFieldNames(JsonSerdeProvider provider) {
        SerializableStruct value = new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return JsonTestData.ESCAPED_FIELD_NAMES;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                serializer.writeString(sdkSchema().member("quoted"), "a");
                serializer.writeString(sdkSchema().member("slashed"), "b");
                serializer.writeString(sdkSchema().member("tabbed"), "c");
                serializer.writeString(sdkSchema().member("controlled"), "d");
                serializer.writeString(sdkSchema().member("unicode"), "e");
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getMemberValue(Schema member) {
                switch (member.memberName()) {
                    case "quoted":
                        return (T) "a";
                    case "slashed":
                        return (T) "b";
                    case "tabbed":
                        return (T) "c";
                    case "controlled":
                        return (T) "d";
                    case "unicode":
                        return (T) "e";
                    default:
                        return (T) null;
                }
            }
        };

        try (JsonCodec codec = codecBuilder(provider).useJsonName(true).build()) {
            String json = StandardCharsets.UTF_8.decode(codec.serialize(value)).toString();
            assertThat(json).isEqualTo("{\"has\\\"quote\":\"a\",\"has\\\\slash\":\"b\",\"has\\ttab\":\"c\","
                            + "\"has\\u0001control\":\"d\",\"héllo\u00e9\":\"e\"}");
        }
    }

    @PerProvider
    public void writesNestedStructures(JsonSerdeProvider provider) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeStruct(
                        JsonTestData.BIRD,
                        new SerializableStruct() {
                            @Override
                            public Schema sdkSchema() {
                                return JsonTestData.BIRD;
                            }

                            @Override
                            public void serializeMembers(ShapeSerializer ser) {
                                ser.writeStruct(sdkSchema().member("nested"), new NestedStruct());
                            }

                            @Override
                            public <T> T getMemberValue(Schema member) {
                                return null;
                            }
                        });
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("{\"nested\":{\"number\":10}}");
        }
    }

    @PerProvider
    public void writesStructureUsingSerializableStruct(JsonSerdeProvider provider) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeStruct(JsonTestData.NESTED, new NestedStruct());
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("{\"number\":10}");
        }
    }

    @PerProvider
    public void writesDunderTypeAndMoreMembers(JsonSerdeProvider provider) throws Exception {
        Document document = Document.of(new NestedStruct());
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                document.serialize(serializer);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("{\"__type\":\"smithy.example#Nested\",\"number\":10}");
        }
    }

    @PerProvider
    public void writesNestedDunderType(JsonSerdeProvider provider) throws Exception {
        Document document = Document.of(new NestedStruct());
        Document map = Document.of(mapOf("a", document));
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                map.serialize(serializer);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("{\"a\":{\"__type\":\"smithy.example#Nested\",\"number\":10}}");
        }
    }

    @PerProvider
    public void writesModeledStructureDocumentInsideStructure(JsonSerdeProvider provider) throws Exception {
        Schema schema = Schema.structureBuilder(ShapeId.from("smithy.example#Outer"))
                              .putMember("payload", PreludeSchemas.DOCUMENT)
                              .build();
        SerializableStruct value = new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return schema;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                serializer.writeDocument(schema.member("payload"), Document.of(new NestedStruct()));
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                return null;
            }
        };
        try (JsonCodec codec = codecBuilder(provider).serializeTypeInDocuments(false).build();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeStruct(schema, value);
            }
            assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isEqualTo("{\"payload\":{\"number\":10}}");
        }
    }

    @PerProvider
    public void writesDunderTypeForEmptyStruct(JsonSerdeProvider provider) throws Exception {
        Document document = Document.of(new EmptyStruct());
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                document.serialize(serializer);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("{\"__type\":\"smithy.example#Nested\"}");
        }
    }

    @Test
    public void testPrettyPrinting() throws Exception {
        // Adapted: the SDK pretty-prints with JsonPrettyPrinter (smithy-java delegates to Jackson); same layout.
        try (JsonCodec codec = JsonCodec.builder().prettyPrint(true).build();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeStruct(
                        JsonTestData.BIRD,
                        new SerializableStruct() {
                            @Override
                            public Schema sdkSchema() {
                                return JsonTestData.BIRD;
                            }

                            @Override
                            public void serializeMembers(ShapeSerializer ser) {
                                ser.writeString(sdkSchema().member("name"), "Toucan");
                                ser.writeString(sdkSchema().member("color"), "red");
                            }

                            @Override
                            public <T> T getMemberValue(Schema member) {
                                return null;
                            }
                        });
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            String expectedFormat = "{\n"
                                    + "  \"name\" : \"Toucan\",\n"
                                    + "  \"color\" : \"red\"\n"
                                    + "}";
            assertThat(result.replace("\r", "")).isEqualTo(expectedFormat);
        }
    }

    private static final class NestedStruct implements SerializableStruct {
        @Override
        public Schema sdkSchema() {
            return JsonTestData.NESTED;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeInteger(JsonTestData.NESTED.member("number"), 10);
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    private static final class EmptyStruct implements SerializableStruct {
        @Override
        public Schema sdkSchema() {
            return JsonTestData.NESTED;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {}

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    @PerProvider
    public void writesIntegerBoundaryValues(JsonSerdeProvider provider) throws Exception {
        Map<Integer, String> testCases = mapOf(
                0,
                "0",
                1,
                "1",
                -1,
                "-1",
                Integer.MIN_VALUE,
                "-2147483648",
                Integer.MAX_VALUE,
                "2147483647",
                12,
                "12",
                123,
                "123",
                1234,
                "1234",
                12345,
                "12345",
                1234567890,
                "1234567890");

        for (Map.Entry<?, String> entry : testCases.entrySet()) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeInteger(PreludeSchemas.INTEGER, (Integer) entry.getKey());
                }
                assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(entry.getValue());
            }
        }
    }

    @PerProvider
    public void writesLongBoundaryValues(JsonSerdeProvider provider) throws Exception {
        Map<Long, String> testCases = mapOf(
                0L,
                "0",
                1L,
                "1",
                -1L,
                "-1",
                Long.MIN_VALUE,
                "-9223372036854775808",
                Long.MAX_VALUE,
                "9223372036854775807",
                1000000L,
                "1000000", // fits in int range
                3000000000L,
                "3000000000", // exceeds int range
                10000000000L,
                "10000000000", // 11 digits
                1000000000000000000L,
                "1000000000000000000"); // 19 digits

        for (Map.Entry<?, String> entry : testCases.entrySet()) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeLong(PreludeSchemas.LONG, (Long) entry.getKey());
                }
                assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(entry.getValue());
            }
        }
    }

    @PerProvider
    public void writesStringWithControlChars(JsonSerdeProvider provider) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeString(PreludeSchemas.STRING, "a\u0000b\u001Fc");
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            // Verify control chars are escaped (case may vary between providers)
            String lower = result.toLowerCase();
            assertThat(lower).contains("\\u0000");
            assertThat(lower).contains("\\u001f");
        }
    }

    @PerProvider
    public void writesStringWithUnicode(JsonSerdeProvider provider) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeString(PreludeSchemas.STRING, "\u00e9\u4e2d\uD83D\uDE00");
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            ShapeDeserializer de = codec.createDeserializer(result.getBytes(StandardCharsets.UTF_8));
            assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo("\u00e9\u4e2d\uD83D\uDE00");
        }
    }

    @PerProvider
    public void writesEmptyString(JsonSerdeProvider provider) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeString(PreludeSchemas.STRING, "");
            }
            assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("\"\"");
        }
    }

    @PerProvider
    public void writesBigDecimalVariousScales(JsonSerdeProvider provider) throws Exception {
        List<BigDecimal> testCases = listOf(
                new BigDecimal("42"), // scale=0
                new BigDecimal("99999.99999"), // positive scale, long unscaled
                new BigDecimal("-123.45"), // negative unscaled
                new BigDecimal("1.005"), // leading zeros in fraction
                new BigDecimal("1E+10")); // negative scale

        for (BigDecimal bd : testCases) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, bd);
                }
                String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
                // Parse back and verify equivalence
                assertThat(new BigDecimal(result).compareTo(bd) == 0).isEqualTo(true);
            }
        }
    }

    @PerProvider
    public void handlesLargeStringsWithBufferGrowth(JsonSerdeProvider provider) throws Exception {
        // String > 8192 bytes to trigger buffer growth
        String large = repeat("x", 10000);
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeString(PreludeSchemas.STRING, large);
            }
            ShapeDeserializer de = codec.createDeserializer(output.toByteArray());
            assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo(large);
        }
    }

    @Test
    public void createSerializer_withBodyLargerThanBuffer_writesFullBuffersToSinkWithoutGrowing() throws Exception {
        // About 120 KB of compact JSON: nested maps and lists of short ASCII and multi-byte strings, none near 8 KB.
        Map<String, Document> items = new LinkedHashMap<>();
        for (int i = 0; i < 400; i++) {
            Map<String, Document> item = new LinkedHashMap<>();
            item.put("id", Document.of(i));
            item.put("text", Document.of(repeat("abcdefghij", 20)));
            item.put("unicode", Document.of(repeat("\u00e9\u4e2d", 10)));
            item.put("tags", Document.of(listOf(Document.of("a"), Document.of(true), Document.of(1.5))));
            items.put("item" + i, Document.of(item));
        }
        Document document = Document.of(items);
        WriteRecordingStream output = new WriteRecordingStream();

        try (JsonCodec codec = codec(SMITHY)) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeDocument(PreludeSchemas.DOCUMENT, document);
            }
            byte[] expected = toArray(codec.serialize(document));

            assertThat(output.toByteArray()).isEqualTo(expected);
            assertThat(expected.length).isGreaterThan(100_000);
            assertThat(output.writes).isGreaterThan(1);
            // The serializer's buffer is drained when full rather than grown to the document size.
            assertThat(output.largestWrite).isLessThanOrEqualTo(SINK_BUFFER_SIZE);
        }
    }

    @Test
    public void createSerializer_withBodyLargerThanBuffer_matchesSerializeThroughByteBufferOutputStream() {
        Map<String, Document> items = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            items.put("item" + i, Document.of(repeat("\u00e9x\"", 20)));
        }
        Document document = Document.of(items);
        ByteBufferOutputStream output = new ByteBufferOutputStream(16);

        try (JsonCodec codec = codec(SMITHY)) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeDocument(PreludeSchemas.DOCUMENT, document);
            }

            assertThat(toArray(output.toByteBuffer())).isEqualTo(toArray(codec.serialize(document)));
            assertThat(output.size()).isGreaterThan(SINK_BUFFER_SIZE * 4);
        }
    }

    @Test
    public void createSerializer_withSmallDocument_writesToSinkOnceOnClose() {
        SerializableShape shape = boundaryShape(10, "value");
        WriteRecordingStream output = new WriteRecordingStream();

        try (JsonCodec codec = codec(SMITHY)) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                shape.serialize(serializer);
                assertThat(output.writes).isZero();
            }

            assertThat(output.toByteArray()).isEqualTo(toArray(codec.serialize(shape)));
            assertThat(output.writes).isEqualTo(1);
        }
    }

    /**
     * Moves a structure, a numeric structure, a string of each length below and a blob across the end of the sink
     * buffer one byte at a time, so that every write starts at, ends at and straddles the point where the buffer is
     * drained. The string lengths cover values that just fit the buffer, just do not, and need it to grow.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sinkBufferStringKinds")
    public void createSerializer_atEveryBufferBoundary_matchesSerialize(String kind, String unit) {
        int[] valueLengths = {0, 1, SINK_BUFFER_SIZE / 6 - 1, SINK_BUFFER_SIZE / 6, SINK_BUFFER_SIZE / 6 + 1,
                              SINK_BUFFER_SIZE - 3, SINK_BUFFER_SIZE - 2, SINK_BUFFER_SIZE - 1, SINK_BUFFER_SIZE,
                              SINK_BUFFER_SIZE + 1, SINK_BUFFER_SIZE * 2 + 1};
        try (JsonCodec codec = codec(SMITHY)) {
            for (int valueLength : valueLengths) {
                String value = repeat(unit, valueLength);
                // The structures are under 320 bytes, so the drain point passes through every write after the pad.
                for (int pad = SINK_BUFFER_SIZE - 320; pad <= SINK_BUFFER_SIZE + 2; pad++) {
                    SerializableShape shape = boundaryShape(pad, value);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try (ShapeSerializer serializer = codec.createSerializer(output)) {
                        shape.serialize(serializer);
                    }

                    assertThat(output.toByteArray())
                        .as("%s value of %d chars after %d pad chars", kind, valueLength, pad)
                        .isEqualTo(toArray(codec.serialize(shape)));
                }
            }
        }
    }

    static List<Arguments> sinkBufferStringKinds() {
        return listOf(Arguments.of("ascii", "a"),
                      Arguments.of("escaped", "\""),
                      Arguments.of("two-byte", "\u00e9"),
                      Arguments.of("surrogate pair", "\ud83d\ude00"));
    }

    private static SerializableShape boundaryShape(int padLength, String value) {
        SchemaStruct simple = SchemaStruct.builder(BenchModel.SIMPLE_STRUCT)
                                          .set("name", "n")
                                          .set("age", 42)
                                          .set("active", true)
                                          .set("score", 1.5)
                                          .set("createdAt", Instant.ofEpochSecond(1_700_000_000L, 5_000_000))
                                          .build();
        SchemaStruct numeric = SchemaStruct.builder(BenchModel.NUMERIC_STRUCT)
                                           .set("byteVal", (byte) -7)
                                           .set("shortVal", (short) 300)
                                           .set("intVal", Integer.MIN_VALUE)
                                           .set("longVal", Long.MIN_VALUE)
                                           .set("floatVal", 1.25f)
                                           .set("doubleVal", -0.1)
                                           .set("bigIntVal", BigInteger.TEN.pow(30))
                                           .set("bigDecVal", new BigDecimal("1.5E-20"))
                                           .build();
        String pad = repeat("p", padLength);
        return s -> s.writeList(PreludeSchemas.DOCUMENT, value, 6, (v, list) -> {
            list.writeString(PreludeSchemas.STRING, pad);
            list.writeStruct(BenchModel.SIMPLE_STRUCT, simple);
            list.writeStruct(BenchModel.NUMERIC_STRUCT, numeric);
            list.writeString(PreludeSchemas.STRING, v);
            list.writeBlob(PreludeSchemas.BLOB, new byte[] {1, 2, 3, 4, 5});
            list.writeString(PreludeSchemas.STRING, "tail");
        });
    }

    @Test
    public void createSerializer_withValueLargerThanBuffer_drainsThenGrowsForThatValue() throws Exception {
        String large = repeat("y", 20_000);
        try (JsonCodec codec = codec(SMITHY); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeList(PreludeSchemas.DOCUMENT, large, 3, (value, s) -> {
                    s.writeString(PreludeSchemas.STRING, repeat("z", 8000));
                    s.writeString(PreludeSchemas.STRING, value);
                    s.writeString(PreludeSchemas.STRING, "tail");
                });
            }
            String expected = "[\"" + repeat("z", 8000) + "\",\"" + large + "\",\"tail\"]";
            assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(expected);
        }
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
    public void smithySerializerHandlesExactStringCapacityBoundary() throws Exception {
        String value = repeat("x", 8191);
        try (JsonCodec codec = codec(SMITHY); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeString(PreludeSchemas.STRING, value);
            }
            ShapeDeserializer de = codec.createDeserializer(output.toByteArray());
            assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo(value);
        }
    }

    @Test
    public void smithySerializerReservesColonAfterWidenedMapKey() throws Exception {
        String key = repeat("\u0000", 5000);
        Document value = Document.of(mapOf(key, Document.of("value")));

        try (JsonCodec codec = codec(SMITHY); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeDocument(PreludeSchemas.DOCUMENT, value);
            }
            Document result = codec.createDeserializer(output.toByteArray()).readDocument();
            assertThat(result.asMap().get(key).asString()).isEqualTo("value");
        }
    }

    @PerProvider
    public void doubleSerializationRoundtrips(JsonSerdeProvider provider) throws Exception {
        for (double v : new double[] {0.0, 1.0, -1.0, 3.14, 1e100, Double.MIN_VALUE, Double.MAX_VALUE}) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeDouble(PreludeSchemas.DOUBLE, v);
                }
                String json = new String(output.toByteArray(), StandardCharsets.UTF_8);
                assertThat(Double.parseDouble(json)).isEqualTo(v);
            }
        }
    }

    @PerProvider
    public void floatSerializationRoundtrips(JsonSerdeProvider provider) throws Exception {
        for (float v : new float[] {0.0f, 1.0f, -1.0f, 3.14f, Float.MIN_VALUE, Float.MAX_VALUE}) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeFloat(PreludeSchemas.FLOAT, v);
                }
                String json = new String(output.toByteArray(), StandardCharsets.UTF_8);
                assertThat(Float.parseFloat(json)).isEqualTo(v);
            }
        }
    }

    @Test
    public void smithyProviderNameAndPriority() {
        SmithyJsonSerdeProvider provider = new SmithyJsonSerdeProvider();
        assertThat(provider.getName()).isEqualTo("smithy");
        assertThat(provider.getPriority()).isEqualTo(10);
    }

    @Test
    public void defaultsToSmithyProvider() {
        JsonSerdeProvider provider = JsonSettings.builder().build().provider();
        Assertions.assertInstanceOf(SmithyJsonSerdeProvider.class, provider);
    }

    @Test
    public void smithyProviderDirectSerializeReturnsBuffer() {
        SmithyJsonSerdeProvider provider = new SmithyJsonSerdeProvider();
        NestedStruct struct = new NestedStruct();
        ByteBuffer result = provider.serialize(struct, JsonSettings.builder().build());
        assertThat(result != null).isEqualTo(true);
        assertThat(result.remaining() > 0).isEqualTo(true);
    }

    @Test
    public void smithyProviderPrettyPrintUsesThePrettyPrinter() throws Exception {
        SmithyJsonSerdeProvider provider = new SmithyJsonSerdeProvider();
        NestedStruct struct = new NestedStruct();
        JsonSettings settings = JsonSettings.builder().prettyPrint(true).build();
        // serialize(ByteBuffer) path
        ByteBuffer result = provider.serialize(struct, settings);
        assertThat(result.remaining() > 0).isEqualTo(true);
        // newSerializer path
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ShapeSerializer ser = provider.newSerializer(output, settings);
        ser.writeInteger(PreludeSchemas.INTEGER, 42);
        ser.close();
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("42");
    }

    @Test
    public void smithyProviderNonArrayBackedByteBuffer() {
        SmithyJsonSerdeProvider provider = new SmithyJsonSerdeProvider();
        JsonSettings settings = JsonSettings.builder().build();
        // Direct ByteBuffer is not array-backed
        ByteBuffer direct = ByteBuffer.allocateDirect(4);
        direct.put("\"hi\"".getBytes(StandardCharsets.UTF_8));
        direct.flip();
        ShapeDeserializer de = provider.newDeserializer(direct, settings);
        assertThat(de.readString(PreludeSchemas.STRING)).isEqualTo("hi");
    }

    @PerProvider
    public void writesIntegersOfAllDigitCounts(JsonSerdeProvider provider) throws Exception {
        // Cover digitCount branches for 6 and 8 digits
        int[] values = {100000, 999999, 10000000, 99999999};
        for (int v : values) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeInteger(PreludeSchemas.INTEGER, v);
                }
                assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(String.valueOf(v));
            }
        }
    }

    @PerProvider
    public void writesLongsOfAllDigitCounts(JsonSerdeProvider provider) throws Exception {
        // Cover digitCountLong branches for 12-18 digits
        long[] values = {
                100000000000L, // 12 digits
                1000000000000L, // 13 digits
                10000000000000L, // 14 digits
                100000000000000L, // 15 digits
                1000000000000000L, // 16 digits
                10000000000000000L, // 17 digits
                100000000000000000L // 18 digits
        };
        for (long v : values) {
            try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                try (ShapeSerializer serializer = codec.createSerializer(output)) {
                    serializer.writeLong(PreludeSchemas.LONG, v);
                }
                assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(String.valueOf(v));
            }
        }
    }

    @PerProvider
    public void writesStringWithLoneSurrogate(JsonSerdeProvider provider) throws Exception {
        String loneSurrogate = "a\uD800b";
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeString(PreludeSchemas.STRING, loneSurrogate);
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            // Verify the lone surrogate is escaped, not written as raw bytes
            assertThat(result.toLowerCase().contains("\\ud800")).isEqualTo(true);
        }
    }

    @PerProvider
    public void writesBlobFromByteArray(JsonSerdeProvider provider) throws Exception {
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeBlob(PreludeSchemas.BLOB, "hello".getBytes(StandardCharsets.UTF_8));
            }
            String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
            // Should be base64 of "hello" = "aGVsbG8="
            assertThat(result).isEqualTo("\"aGVsbG8=\"");
        }
    }

    @PerProvider
    public void writesBlobFromDirectByteBuffer(JsonSerdeProvider provider) throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        direct.flip();
        try (JsonCodec codec = codec(provider); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeBlob(PreludeSchemas.BLOB, direct);
            }
            assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("\"aGVsbG8=\"");
        }
    }

    @Test
    public void documentStructAtDepthLimitThrowsSerializationException() {
        // Adapted: SDK documents have no structure documents yet, so the nesting is built from list documents; the
        // serializer's depth limit (64) applies to every container the same way.
        Document nested = Document.of(1);
        for (int i = 0; i < 64; i++) {
            nested = Document.of(listOf(nested));
        }
        Document deepDoc = nested;
        Assertions.assertThrows(
                software.amazon.awssdk.schema.serde.SerializationException.class,
                () -> {
                    try (JsonCodec codec = JsonCodec.builder()
                            .overrideSerdeProvider(
                                    new SmithyJsonSerdeProvider())
                            .build();
                            ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        try (ShapeSerializer serializer = codec.createSerializer(output)) {
                            deepDoc.serialize(serializer);
                        }
                    }
                });
    }

    @Test
    public void smithySerializerHandlesIOExceptionOnFlush() {
        JsonSettings settings = JsonSettings.builder().build();
        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("simulated");
            }
        };
        SmithyJsonSerdeProvider provider = new SmithyJsonSerdeProvider();
        ShapeSerializer serializer = provider.newSerializer(failingStream, settings);
        serializer.writeInteger(PreludeSchemas.INTEGER, 42);
        Assertions.assertThrows(
                software.amazon.awssdk.schema.serde.SerializationException.class,
                serializer::flush);
    }

    @Test
    public void smithySerializerHandlesIOExceptionOnClose() {
        JsonSettings settings = JsonSettings.builder().build();
        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("simulated");
            }
        };
        SmithyJsonSerdeProvider provider = new SmithyJsonSerdeProvider();
        ShapeSerializer serializer = provider.newSerializer(failingStream, settings);
        serializer.writeInteger(PreludeSchemas.INTEGER, 42);
        Assertions.assertThrows(
                software.amazon.awssdk.schema.serde.SerializationException.class,
                serializer::close);
    }
}
