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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codecs.json.TestCollections.listOf;
import static software.amazon.awssdk.codecs.json.TestCollections.mapOf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.json.internal.JsonSchemaExtensions;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSchemaExtensions;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSerdeProvider;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * SDK tests of {@link JsonCodec} and {@link JsonSettings} beyond the ported smithy-java tests: protocol presets,
 * settings values, the lenient number-into-string read, ShapeId-naming errors, pretty printing, the wire format of
 * numbers and timestamps, and concurrent use.
 */
class JsonCodecTest {

    private static final Schema STRUCT = Schema.structureBuilder(ShapeId.from("test#Struct"))
                                               .putMember("stringValue", PreludeSchemas.STRING)
                                               .putMember("intValue", PreludeSchemas.INTEGER, new JsonNameTrait("int"))
                                               .putMember("when", PreludeSchemas.TIMESTAMP,
                                                          new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
                                               .putMember("list", BenchModel.STRING_LIST)
                                               .putMember("map", BenchModel.INTEGER_MAP)
                                               .build();

    private static final Schema UNION = Schema.unionBuilder(ShapeId.from("test#Union"))
                                              .putMember("a", PreludeSchemas.STRING)
                                              .build();

    private static final JsonCodec REST_JSON = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();
    private static final JsonCodec AWS_JSON = JsonCodec.builder().settings(JsonSettings.AWS_JSON).build();

    @Test
    void restJson1Preset_honorsJsonNameAndTimestampFormatWithEpochSecondsDefault() {
        JsonSettings settings = JsonSettings.REST_JSON_1;
        assertThat(settings.useJsonName()).isTrue();
        assertThat(settings.useTimestampFormat()).isTrue();
        assertThat(settings.defaultTimestampFormat()).isEqualTo(TimestampFormatTrait.Format.EPOCH_SECONDS);
        assertThat(settings.fieldMapper()).isSameAs(JsonFieldMapper.UseJsonNameTrait.INSTANCE);
        assertThat(settings.timestampResolver()).isInstanceOf(TimestampResolver.UseTimestampFormatTrait.class);
        assertThat(settings.prettyPrint()).isFalse();
        assertThat(settings.serializeTypeInDocuments()).isTrue();
        assertThat(settings.useStringForArbitraryPrecision()).isFalse();
        assertThat(settings.forbidUnknownUnionMembers()).isFalse();

        SchemaStruct value = SchemaStruct.builder(STRUCT).set("intValue", 1).set("when", Instant.EPOCH).build();
        assertThat(REST_JSON.serializeToString(value)).isEqualTo("{\"int\":1,\"when\":\"1970-01-01T00:00:00Z\"}");
    }

    @Test
    void awsJsonPreset_ignoresJsonNameAndHonorsTimestampFormat() {
        JsonSettings settings = JsonSettings.AWS_JSON;
        assertThat(settings.useJsonName()).isFalse();
        assertThat(settings.useTimestampFormat()).isTrue();
        assertThat(settings.defaultTimestampFormat()).isEqualTo(TimestampFormatTrait.Format.EPOCH_SECONDS);
        assertThat(settings.fieldMapper()).isSameAs(JsonFieldMapper.UseMemberName.INSTANCE);

        SchemaStruct value = SchemaStruct.builder(STRUCT).set("intValue", 1).set("when", Instant.EPOCH).build();
        assertThat(AWS_JSON.serializeToString(value)).isEqualTo("{\"intValue\":1,\"when\":\"1970-01-01T00:00:00Z\"}");
    }

    @Test
    void defaultSettings_ignoreBothTraits() {
        JsonSettings settings = JsonSettings.builder().build();
        assertThat(settings.useJsonName()).isFalse();
        assertThat(settings.useTimestampFormat()).isFalse();
        assertThat(settings.timestampResolver()).isInstanceOf(TimestampResolver.StaticFormat.class);
        assertThat(settings.provider()).isInstanceOf(SmithyJsonSerdeProvider.class);
        assertThat(settings.toString()).contains("useJsonName=false").contains("provider=default");
    }

    @Test
    void settings_toBuilderAndUpdateBuilderCopyEveryValue() {
        JsonSettings settings = JsonSettings.builder()
                                            .useJsonName(true)
                                            .useTimestampFormat(true)
                                            .defaultTimestampFormat(TimestampFormatTrait.Format.HTTP_DATE)
                                            .defaultNamespace("com.example")
                                            .forbidUnknownUnionMembers(true)
                                            .serializeTypeInDocuments(false)
                                            .prettyPrint(true)
                                            .useStringForArbitraryPrecision(true)
                                            .build();
        assertThat(settings.toBuilder().build()).isEqualTo(settings).hasSameHashCodeAs(settings);
        assertThat(JsonSettings.builder().updateBuilder(settings).build()).isEqualTo(settings);
        assertThat(JsonCodec.builder().settings(settings).build().settings()).isEqualTo(settings);
        assertThat(settings).isNotEqualTo(JsonSettings.REST_JSON_1);
        assertThat(settings.timestampResolver().defaultFormat()).isEqualTo(TimestampFormatter.Prelude.HTTP_DATE);
    }

    @Test
    void codecBuilder_settersOverrideCopiedSettings() {
        JsonCodec codec = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).useJsonName(false).build();
        assertThat(codec.settings().useJsonName()).isFalse();
        assertThat(codec.settings().useTimestampFormat()).isTrue();
        assertThat(codec.toString()).startsWith("JsonCodec(JsonSettings(");
    }

    @Test
    void overrideSerdeProvider_isUsedByTheCodec() {
        CountingProvider provider = new CountingProvider();
        JsonCodec codec = JsonCodec.builder().overrideSerdeProvider(provider).build();
        assertThat(codec.settings().provider()).isSameAs(provider);
        codec.serializeToString(Document.of(1));
        codec.createDeserializer(new byte[] {'1'}).readInteger(PreludeSchemas.INTEGER);
        assertThat(provider.serializers).isEqualTo(1);
        assertThat(provider.deserializers).isEqualTo(1);
        assertThatThrownBy(() -> JsonCodec.builder().overrideSerdeProvider(null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("provider");
    }

    @Test
    void nullArguments_throwNpeNamingTheParameter() {
        assertThatThrownBy(() -> REST_JSON.serialize(null)).isInstanceOf(NullPointerException.class)
                                                          .hasMessage("shape must not be null");
        assertThatThrownBy(() -> REST_JSON.deserializeShape((byte[]) null, SchemaStruct.builder(STRUCT)))
            .isInstanceOf(NullPointerException.class).hasMessage("source must not be null");
        assertThatThrownBy(() -> REST_JSON.deserializeShape(ByteBuffer.allocate(0), null))
            .isInstanceOf(NullPointerException.class).hasMessage("builder must not be null");
    }

    @Test
    void schemaExtensions_areRegistered() {
        List<Class<?>> providers = new ArrayList<>();
        for (SchemaExtensionProvider<?> provider : ServiceLoader.load(SchemaExtensionProvider.class)) {
            providers.add(provider.getClass());
        }
        assertThat(providers).contains(JsonSchemaExtensions.class, SmithyJsonSchemaExtensions.class);
        assertThat(STRUCT.getExtension(SmithyJsonSchemaExtensions.KEY)).isNotNull();
        assertThat(STRUCT.getExtension(JsonSchemaExtensions.KEY).jsonMemberLookup().member("int"))
            .isSameAs(STRUCT.member("intValue"));
    }

    @ParameterizedTest
    @CsvSource({
        "123456789012345678901.000000000000000001",
        "-0.5e-7",
        "42",
        "1E400"
    })
    void readString_ofAJsonNumber_returnsTheLiteralText(String number) {
        SchemaStruct value = REST_JSON.deserializeShape("{\"stringValue\": " + number + "}", SchemaStruct.builder(STRUCT));
        assertThat(value.get("stringValue")).isEqualTo(number);
    }

    @Test
    void readString_ofAMalformedNumber_throws() {
        assertThatThrownBy(() -> REST_JSON.deserializeShape("{\"stringValue\": 01}", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class).hasMessageContaining("Leading zeros");
    }

    @Test
    void typeMismatch_namesTheShapeIdAndMember() {
        assertThatThrownBy(() -> REST_JSON.deserializeShape("{\"int\": \"x\"}", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("test#Struct$intValue");
        assertThatThrownBy(() -> REST_JSON.deserializeShape("{\"stringValue\": true}", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected string for test#Struct$stringValue, found: 't'");
        assertThatThrownBy(() -> REST_JSON.deserializeShape("{\"list\": {}}", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("test#Struct$list");
        assertThatThrownBy(() -> REST_JSON.deserializeShape("[]", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected '{' for test#Struct, found: '['");
    }

    @Test
    void integerOutOfRange_namesTheShapeId() {
        assertThatThrownBy(() -> REST_JSON.deserializeShape("{\"int\": 2147483648}", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Value out of integer range for test#Struct$intValue: 2147483648");
    }

    @Test
    void deserializeShape_rejectsTrailingContentAndBuildsNothing() {
        assertThatThrownBy(() -> REST_JSON.deserializeShape("{} {}", SchemaStruct.builder(STRUCT)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Unexpected JSON content");
        assertThat(REST_JSON.deserializeShape("  {}  \n", SchemaStruct.builder(STRUCT)))
            .isEqualTo(SchemaStruct.builder(STRUCT).build());
    }

    @Test
    void unknownUnionMember_isReportedOrRejectedPerSettings() {
        SchemaStruct.Builder lenient = SchemaStruct.builder(UNION);
        REST_JSON.createDeserializer("{\"__type\":\"x#Y\",\"b\":1,\"a\":\"v\"}".getBytes(StandardCharsets.UTF_8))
                 .readStruct(UNION, lenient, unionConsumer());
        assertThat(lenient.unknownMembers()).containsExactly("b");

        JsonCodec strict = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).forbidUnknownUnionMembers(true).build();
        assertThatThrownBy(() -> strict.deserializeShape("{\"b\":1}", SchemaStruct.builder(UNION)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unknown member b encountered for union test#Union");
        assertThat(strict.deserializeShape("{\"__type\":\"x#Y\",\"a\":\"v\"}", SchemaStruct.builder(UNION)).get("a"))
            .isEqualTo("v");
    }

    @Test
    void prettyPrint_usesTheJacksonDefaultPrettyPrinterLayout() {
        JsonCodec pretty = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).prettyPrint(true).build();
        SchemaStruct value = SchemaStruct.builder(STRUCT)
                                         .set("stringValue", "a{b}[c], \"d\": e")
                                         .set("list", listOf("x", "y"))
                                         .set("map", mapOf("k", 1))
                                         .build();
        String expected = "{\n"
                          + "  \"stringValue\" : \"a{b}[c], \\\"d\\\": e\",\n"
                          + "  \"list\" : [ \"x\", \"y\" ],\n"
                          + "  \"map\" : {\n"
                          + "    \"k\" : 1\n"
                          + "  }\n"
                          + "}";
        assertThat(pretty.serializeToString(value)).isEqualTo(expected);
        ByteBuffer pooled = pretty.serialize(value);
        assertThat(StandardCharsets.UTF_8.decode(pooled).toString()).isEqualTo(expected);

        Document nested = Document.of(listOf(Document.of(TestCollections.<String, Document>mapOf()),
                                             Document.of(TestCollections.<Document>listOf()),
                                             Document.of(mapOf("a", Document.of(listOf(Document.of(1)))))));
        assertThat(pretty.serializeToString(nested)).isEqualTo("[ { }, [ ], {\n  \"a\" : [ 1 ]\n} ]");
    }

    @Test
    void prettyPrint_outputReadsBackToTheSameValue() {
        JsonCodec pretty = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).prettyPrint(true).build();
        SchemaStruct value = SchemaStruct.builder(STRUCT)
                                         .set("stringValue", "tab\there \u00e9\uD83D\uDE00")
                                         .set("intValue", -7)
                                         .set("when", Instant.parse("2025-01-15T10:30:00.123Z"))
                                         .set("list", listOf("", "x"))
                                         .set("map", mapOf("a", 1, "b", 2))
                                         .build();
        assertThat(REST_JSON.deserializeShape(pretty.serializeToString(value), SchemaStruct.builder(STRUCT)))
            .isEqualTo(value);
    }

    /**
     * Records the number and timestamp spellings, including where they differ from the legacy SDK
     * ({@code SdkJsonGenerator} over Jackson): integral doubles are written without {@code .0}, epoch seconds keep up
     * to nine fraction digits and drop trailing zeros, and big decimals are plain JSON numbers.
     */
    @Test
    void numbersAndTimestamps_haveTheDocumentedWireFormat() {
        assertThat(write(s -> s.writeDouble(PreludeSchemas.DOUBLE, 1.0))).isEqualTo("1");
        assertThat(write(s -> s.writeDouble(PreludeSchemas.DOUBLE, 1.0e10))).isEqualTo("10000000000");
        assertThat(write(s -> s.writeDouble(PreludeSchemas.DOUBLE, 1.0e300))).isEqualTo("1.0E300");
        assertThat(write(s -> s.writeDouble(PreludeSchemas.DOUBLE, 0.1))).isEqualTo("0.1");
        assertThat(write(s -> s.writeDouble(PreludeSchemas.DOUBLE, -0.0))).isEqualTo("0");
        assertThat(write(s -> s.writeFloat(PreludeSchemas.FLOAT, 1.5f))).isEqualTo("1.5");
        assertThat(write(s -> s.writeDouble(PreludeSchemas.DOUBLE, Double.NaN))).isEqualTo("\"NaN\"");
        assertThat(write(s -> s.writeFloat(PreludeSchemas.FLOAT, Float.NEGATIVE_INFINITY))).isEqualTo("\"-Infinity\"");
        assertThat(write(s -> s.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, new BigDecimal("1E+3")))).isEqualTo("1000");
        assertThat(write(s -> s.writeBigInteger(PreludeSchemas.BIG_INTEGER, BigInteger.TEN.pow(30))))
            .isEqualTo("1000000000000000000000000000000");
        assertThat(write(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.ofEpochSecond(1398796238))))
            .isEqualTo("1398796238");
        assertThat(write(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.ofEpochMilli(1398796238100L))))
            .isEqualTo("1398796238.1");
        assertThat(write(s -> s.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.ofEpochSecond(1, 123_456_789))))
            .isEqualTo("1.123456789");
        assertThat(write(s -> s.writeBlob(PreludeSchemas.BLOB, "foo".getBytes(StandardCharsets.UTF_8))))
            .isEqualTo("\"Zm9v\"");
    }

    @Test
    void documents_areWrittenByType() {
        assertThat(REST_JSON.serializeToString(Document.of(Instant.EPOCH))).isEqualTo("0");
        assertThat(REST_JSON.serializeToString(Document.of("foo".getBytes(StandardCharsets.UTF_8)))).isEqualTo("\"Zm9v\"");
        List<Document> withNull = new ArrayList<>();
        withNull.add(Document.of(1));
        withNull.add(null);
        assertThat(REST_JSON.serializeToString(Document.of(withNull))).isEqualTo("[1,null]");
        assertThat(write(s -> s.writeDocument(PreludeSchemas.DOCUMENT, null))).isEqualTo("null");
    }

    @Test
    void readDocument_typesNumbersByMagnitude() {
        assertThat(readDocument("2147483647").type()).isEqualTo(software.amazon.awssdk.schema.ShapeType.INTEGER);
        assertThat(readDocument("2147483648").type()).isEqualTo(software.amazon.awssdk.schema.ShapeType.LONG);
        assertThat(readDocument("-9223372036854775808").asLong()).isEqualTo(Long.MIN_VALUE);
        assertThat(readDocument("9223372036854775808").asBigInteger()).isEqualTo(new BigInteger("9223372036854775808"));
        assertThat(readDocument("1e2").type()).isEqualTo(software.amazon.awssdk.schema.ShapeType.DOUBLE);
        assertThat(readDocument("[1,null]").asList()).hasSize(2).containsNull();
    }

    @Test
    void oneCodec_usedConcurrently_givesEachThreadItsOwnResult() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                int seed = t;
                results.add(executor.submit((Callable<Boolean>) () -> {
                    for (int i = 0; i < 200; i++) {
                        SchemaStruct value = SchemaStruct.builder(STRUCT)
                                                         .set("stringValue", "thread-" + seed + "-" + i)
                                                         .set("list", listOf("a" + i, "b"))
                                                         .build();
                        ByteBuffer bytes = REST_JSON.serialize(value);
                        if (!REST_JSON.deserializeShape(bytes, SchemaStruct.builder(STRUCT)).equals(value)) {
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

    @Test
    void createSerializer_writesOnFlushAndCloseWithoutClosingTheSink() {
        boolean[] closed = {false};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream sink = new OutputStream() {
            @Override
            public void write(int b) {
                bytes.write(b);
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };
        ShapeSerializer serializer = REST_JSON.createSerializer(sink);
        serializer.writeInteger(PreludeSchemas.INTEGER, 1);
        assertThat(bytes.size()).isZero();
        serializer.flush();
        assertThat(bytes.toString()).isEqualTo("1");
        serializer.close();
        assertThat(closed[0]).isFalse();
    }

    private static Document readDocument(String json) {
        return REST_JSON.createDeserializer(json.getBytes(StandardCharsets.UTF_8)).readDocument();
    }

    private static String write(java.util.function.Consumer<ShapeSerializer> writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = AWS_JSON.createSerializer(out)) {
            writer.accept(serializer);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static ShapeDeserializer.StructMemberConsumer<SchemaStruct.Builder> unionConsumer() {
        return new ShapeDeserializer.StructMemberConsumer<SchemaStruct.Builder>() {
            @Override
            public void accept(SchemaStruct.Builder builder, Schema member, ShapeDeserializer de) {
                builder.set(member.memberName(), de.readString(member));
            }

            @Override
            public void unknownMember(SchemaStruct.Builder builder, String memberName) {
                builder.unknownMembers().add(memberName);
            }
        };
    }

    private static final class CountingProvider implements JsonSerdeProvider {
        private final SmithyJsonSerdeProvider delegate = new SmithyJsonSerdeProvider();
        private int serializers;
        private int deserializers;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public String getName() {
            return "counting";
        }

        @Override
        public ShapeDeserializer newDeserializer(byte[] source, JsonSettings settings) {
            deserializers++;
            return delegate.newDeserializer(source, settings);
        }

        @Override
        public ShapeDeserializer newDeserializer(ByteBuffer source, JsonSettings settings) {
            deserializers++;
            return delegate.newDeserializer(source, settings);
        }

        @Override
        public ShapeSerializer newSerializer(OutputStream sink, JsonSettings settings) {
            serializers++;
            return delegate.newSerializer(sink, settings);
        }
    }
}
