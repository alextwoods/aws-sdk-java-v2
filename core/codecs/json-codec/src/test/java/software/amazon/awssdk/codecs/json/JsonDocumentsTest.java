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
import static software.amazon.awssdk.codecs.json.TestCollections.setOf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.document.DocumentEqualsFlags;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Tests of the JSON Protocol_Documents ({@link JsonDocuments}) that the JSON codec reads: the settings they hold, the
 * blob, timestamp and numeric coercions, discriminators, equality, and {@code asShape} with each setting.
 */
class JsonDocumentsTest {

    private static final JsonCodec DEFAULT = JsonCodec.builder().build();

    private static final Schema INNER = Schema.structureBuilder(ShapeId.from("test#Inner"))
                                              .putMember("value", PreludeSchemas.STRING, new JsonNameTrait("VALUE"))
                                              .build();

    private static final Schema UNION = Schema.unionBuilder(ShapeId.from("test#Choice"))
                                              .putMember("text", PreludeSchemas.STRING)
                                              .putMember("number", PreludeSchemas.INTEGER, new JsonNameTrait("NUM"))
                                              .build();

    private static final Schema STRING_LIST = Schema.listBuilder(ShapeId.from("test#StringList"))
                                                    .putMember("member", PreludeSchemas.STRING)
                                                    .build();

    private static final Schema INTEGER_MAP = Schema.mapBuilder(ShapeId.from("test#IntegerMap"))
                                                    .putMember("key", PreludeSchemas.STRING)
                                                    .putMember("value", PreludeSchemas.INTEGER)
                                                    .build();

    private static final Schema COLOR = Schema.createEnum(ShapeId.from("test#Color"), setOf("RED", "GREEN"));

    private static final Schema PRIORITY = Schema.createIntEnum(ShapeId.from("test#Priority"), setOf(1, 100));

    private static final Schema ALL_TYPES =
        Schema.structureBuilder(ShapeId.from("test#AllTypes"))
              .putMember("string", PreludeSchemas.STRING, new JsonNameTrait("STRING"))
              .putMember("boolean", PreludeSchemas.BOOLEAN)
              .putMember("byte", PreludeSchemas.BYTE)
              .putMember("short", PreludeSchemas.SHORT)
              .putMember("integer", PreludeSchemas.INTEGER, new JsonNameTrait("int"))
              .putMember("long", PreludeSchemas.LONG)
              .putMember("float", PreludeSchemas.FLOAT)
              .putMember("double", PreludeSchemas.DOUBLE)
              .putMember("nan", PreludeSchemas.DOUBLE)
              .putMember("negativeInfinity", PreludeSchemas.FLOAT)
              .putMember("bigInteger", PreludeSchemas.BIG_INTEGER)
              .putMember("bigDecimal", PreludeSchemas.BIG_DECIMAL)
              .putMember("blob", PreludeSchemas.BLOB, new JsonNameTrait("data"))
              .putMember("epoch", PreludeSchemas.TIMESTAMP)
              .putMember("dateTime", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
              .putMember("httpDate", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
              .putMember("list", STRING_LIST)
              .putMember("map", INTEGER_MAP)
              .putMember("inner", INNER, new JsonNameTrait("nested"))
              .putMember("choice", UNION)
              .putMember("color", COLOR)
              .putMember("priority", PRIORITY)
              .putMember("document", PreludeSchemas.DOCUMENT)
              .build();

    private static Document read(String json) {
        return read(DEFAULT, json);
    }

    private static Document read(JsonCodec codec, String json) {
        return codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8)).readDocument();
    }

    private static JsonCodec codecWithDefaultFormat(TimestampFormatTrait.Format format) {
        return JsonCodec.builder().defaultTimestampFormat(format).build();
    }

    private static String utf8(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    void readDocument_everyValueAndNestedValue_holdsTheCodecSettings() {
        JsonCodec codec = JsonCodec.builder().useJsonName(true).defaultNamespace("com.example").build();
        Document document = read(codec, "{\"a\":[true,\"s\",1,4294967296,1.5,18446744073709551616,1e400],\"b\":{}}");

        List<Document> values = document.getMember("a").asList();
        assertThat(values).extracting(Document::type)
                          .containsExactly(ShapeType.BOOLEAN, ShapeType.STRING, ShapeType.INTEGER, ShapeType.LONG,
                                           ShapeType.DOUBLE, ShapeType.BIG_INTEGER, ShapeType.BIG_DECIMAL);
        assertThat(settingsOf(document)).isSameAs(codec.settings());
        assertThat(settingsOf(document.getMember("a"))).isSameAs(codec.settings());
        assertThat(settingsOf(document.getMember("b"))).isSameAs(codec.settings());
        for (Document value : values) {
            assertThat(settingsOf(value)).isSameAs(codec.settings());
        }
    }

    private static JsonSettings settingsOf(Document document) {
        assertThat(document).isInstanceOf(JsonDocuments.JsonDocument.class);
        return ((JsonDocuments.JsonDocument) document).settings();
    }

    @Test
    void asBlob_onBase64String_returnsTheDecodedBytes() {
        assertThat(read("\"aGVsbG8=\"").asBlob()).isEqualTo(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        assertThat(read("\"\"").asBlob().remaining()).isZero();
    }

    @Test
    void asBlob_onInvalidBase64_throwsSerializationException() {
        assertThatThrownBy(() -> read("\"not base64!\"").asBlob())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a base64 encoded blob value");
    }

    @Test
    void asBlob_onNumber_throwsTypeMismatch() {
        assertThatThrownBy(() -> read("1").asBlob())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a blob document, but found integer");
    }

    static Stream<Arguments> epochSecondsNumbers() {
        return Stream.of(
            Arguments.of("1398796238", Instant.ofEpochSecond(1398796238L)),
            Arguments.of("4294967296", Instant.ofEpochSecond(4294967296L)),
            Arguments.of("1398796238.123", Instant.ofEpochSecond(1398796238L, 123_000_000)),
            Arguments.of("1.123456789", Instant.ofEpochSecond(1, 123_456_789)),
            Arguments.of("1398796238.123456789", Instant.ofEpochSecond(1398796238L, 123_456_789)),
            Arguments.of("1398796238.1234567891", Instant.ofEpochSecond(1398796238L, 123_456_789)),
            Arguments.of("-1.5", Instant.ofEpochSecond(-2, 500_000_000)),
            Arguments.of("1.5e3", Instant.ofEpochSecond(1500)),
            Arguments.of("0", Instant.EPOCH));
    }

    @ParameterizedTest
    @MethodSource("epochSecondsNumbers")
    void asTimestamp_onNumberWithEpochSecondsDefault_returnsTheExactInstant(String json, Instant expected) {
        assertThat(read(json).asTimestamp()).isEqualTo(expected);
    }

    @Test
    void asTimestamp_onNumberOutOfRange_throwsSerializationException() {
        assertThatThrownBy(() -> read("18446744073709551616").asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Epoch seconds out of range: 18446744073709551616");
        assertThatThrownBy(() -> read("9223372036854775807").asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Epoch seconds out of range: 9223372036854775807");
        assertThatThrownBy(() -> read("1e400").asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Epoch seconds out of range: 1E+400");
    }

    @Test
    void asTimestamp_onStringWithEpochSecondsDefault_throwsNamingTheFormat() {
        assertThatThrownBy(() -> read("\"1398796238\"").asTimestamp())
            .isInstanceOf(TimestampFormatter.TimestampSyntaxError.class)
            .hasMessage("Expected a NUMBER value for a EPOCH_SECONDS timestamp, but found String");
    }

    @Test
    void asTimestamp_onStringWithDateTimeDefault_parsesTheString() {
        JsonCodec codec = codecWithDefaultFormat(TimestampFormatTrait.Format.DATE_TIME);

        assertThat(read(codec, "\"2014-04-29T18:30:38.123456789Z\"").asTimestamp())
            .isEqualTo(Instant.ofEpochSecond(1398796238L, 123_456_789));
    }

    @Test
    void asTimestamp_onStringWithHttpDateDefault_parsesTheString() {
        JsonCodec codec = codecWithDefaultFormat(TimestampFormatTrait.Format.HTTP_DATE);

        assertThat(read(codec, "\"Tue, 29 Apr 2014 18:30:38 GMT\"").asTimestamp())
            .isEqualTo(Instant.ofEpochSecond(1398796238L));
    }

    @Test
    void asTimestamp_onUnparseableString_throwsNamingTheFormat() {
        assertThatThrownBy(() -> read(codecWithDefaultFormat(TimestampFormatTrait.Format.DATE_TIME), "\"nope\"")
            .asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a date-time timestamp, but found \"nope\"");
        assertThatThrownBy(() -> read(codecWithDefaultFormat(TimestampFormatTrait.Format.HTTP_DATE), "\"nope\"")
            .asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a http-date timestamp, but found \"nope\"");
    }

    @Test
    void asTimestamp_onNumberWithStringDefault_throwsNamingTheFormat() {
        assertThatThrownBy(() -> read(codecWithDefaultFormat(TimestampFormatTrait.Format.DATE_TIME), "1")
            .asTimestamp())
            .isInstanceOf(TimestampFormatter.TimestampSyntaxError.class)
            .hasMessage("Expected a STRING value for a DATE_TIME timestamp, but found Integer");
        assertThatThrownBy(() -> read(codecWithDefaultFormat(TimestampFormatTrait.Format.HTTP_DATE), "1.5")
            .asTimestamp())
            .isInstanceOf(TimestampFormatter.TimestampSyntaxError.class)
            .hasMessage("Expected a STRING value for a HTTP_DATE timestamp, but found Double");
    }

    @Test
    void asTimestamp_onBooleanListOrMap_throwsTypeMismatch() {
        assertThatThrownBy(() -> read("true").asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a timestamp document, but found boolean");
        assertThatThrownBy(() -> read("[]").asTimestamp())
            .hasMessage("Expected a timestamp document, but found list");
        assertThatThrownBy(() -> read("{}").asTimestamp())
            .hasMessage("Expected a timestamp document, but found map");
    }

    @Test
    void numericCoercions_followJlsNarrowingWithoutThrowing() {
        assertThat(read("4294967297").asInteger()).isEqualTo(1);
        assertThat(read("1.9").asInteger()).isEqualTo(1);
        assertThat(read("-1.9").asLong()).isEqualTo(-1L);
        assertThat(read("128").asByte()).isEqualTo((byte) -128);
        assertThat(read("32768").asShort()).isEqualTo((short) -32768);
        assertThat(read("1e20").asLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(read("18446744073709551617").asLong()).isEqualTo(1L);
        assertThat(read("1.00000000000000000001").asDouble()).isEqualTo(1.0);
        assertThat(read("1e400").asDouble()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(read("3.5").asBigInteger()).isEqualTo(BigInteger.valueOf(3));
        assertThat(read("1").asFloat()).isEqualTo(1f);
    }

    @Test
    void asBigDecimal_isExactForIntegralAndDecimalNumbers() {
        // 2^53 + 1 is not a double; the long is converted exactly.
        assertThat(read("9007199254740993").asBigDecimal()).isEqualTo(new BigDecimal("9007199254740993"));
        assertThat(read("18446744073709551616").asBigDecimal()).isEqualTo(new BigDecimal("18446744073709551616"));
        assertThat(read("0.1").asBigDecimal()).isEqualTo(new BigDecimal("0.1"));
        assertThat(read("1.00000000000000000001").asBigDecimal())
            .isEqualTo(new BigDecimal("1.00000000000000000001"));
    }

    @Test
    void ofNumber_keepsTheTypeOfTheNumberClass() {
        JsonSettings settings = DEFAULT.settings();

        assertThat(JsonDocuments.of((Number) (byte) 1, settings).type()).isEqualTo(ShapeType.BYTE);
        assertThat(JsonDocuments.of((Number) (short) 1, settings).type()).isEqualTo(ShapeType.SHORT);
        assertThat(JsonDocuments.of((Number) 1, settings).type()).isEqualTo(ShapeType.INTEGER);
        assertThat(JsonDocuments.of((Number) 1L, settings).type()).isEqualTo(ShapeType.LONG);
        assertThat(JsonDocuments.of((Number) 1.5f, settings).type()).isEqualTo(ShapeType.FLOAT);
        assertThat(JsonDocuments.of((Number) 1.5, settings).type()).isEqualTo(ShapeType.DOUBLE);
        assertThat(JsonDocuments.of((Number) BigInteger.ONE, settings).type()).isEqualTo(ShapeType.BIG_INTEGER);
        assertThat(JsonDocuments.of((Number) BigDecimal.ONE, settings).type()).isEqualTo(ShapeType.BIG_DECIMAL);
        assertThat(JsonDocuments.of((Number) 1.5f, settings).asFloat()).isEqualTo(1.5f);
        assertThatThrownBy(() -> JsonDocuments.of(new java.util.concurrent.atomic.AtomicInteger(), settings))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported Number");
    }

    @Test
    void asBigDecimal_onNonFiniteDouble_throws() {
        assertThatThrownBy(() -> JsonDocuments.of(Double.NaN, DEFAULT.settings()).asBigDecimal())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot convert a non-finite double document (NaN) to a bigDecimal");
    }

    @Test
    void coercions_toOtherTypes_throwNamingBothTypes() {
        assertThatThrownBy(() -> read("\"12\"").asInteger())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected an integer document, but found string");
        assertThatThrownBy(() -> read("12").asString())
            .hasMessage("Expected a string document, but found integer");
        assertThatThrownBy(() -> read("true").asList())
            .hasMessage("Expected a list document, but found boolean");
        assertThatThrownBy(() -> read("[1]").asMap())
            .hasMessage("Expected a map document, but found list");
        assertThatThrownBy(() -> read("{}").asBoolean())
            .hasMessage("Expected a boolean document, but found map");
    }

    @Test
    void equals_matchesUntypedDocumentsOfTheSameTypeAndValue() {
        assertThat(read("1")).isEqualTo(Document.of(1)).hasSameHashCodeAs(Document.of(1));
        assertThat(read("4294967296")).isEqualTo(Document.of(4294967296L));
        assertThat(read("1.5")).isEqualTo(Document.of(1.5));
        assertThat(read("\"a\"")).isEqualTo(Document.of("a")).hasSameHashCodeAs(Document.of("a"));
        assertThat(read("[1,null]")).isEqualTo(Document.of(Arrays.asList(Document.of(1), null)));
        Map<String, Document> map = mapOf("a", Document.of(true));
        assertThat(read("{\"a\":true}")).isEqualTo(Document.of(map)).hasSameHashCodeAs(Document.of(map));
        // A JSON blob is a string; it does not equal a blob document.
        assertThat(read("\"Zm9v\"")).isNotEqualTo(Document.of("foo".getBytes(StandardCharsets.UTF_8)));
        assertThat(read("1")).isNotEqualTo(Document.of(1L));
        assertThat(Document.equals(read("1"), Document.of(1L), DocumentEqualsFlags.NUMBER_PROMOTION)).isTrue();
    }

    @Test
    void equals_ignoresTheSettings() {
        JsonCodec other = JsonCodec.builder().useJsonName(true).defaultTimestampFormat(
            TimestampFormatTrait.Format.DATE_TIME).build();

        assertThat(read("{\"a\":[1]}")).isEqualTo(read(other, "{\"a\":[1]}"));
    }

    @Test
    void toString_showsTypeAndValue() {
        assertThat(read("\"a\"")).hasToString("JsonDocument(string: a)");
        assertThat(read("[1]")).hasToString("JsonDocument(list: [JsonDocument(integer: 1)])");
    }

    @Test
    void getDiscriminator_isParsedOnceAndCached() {
        Document document = read(JsonCodec.builder().defaultNamespace("com.foo").build(), "{\"__type\":\"Foo\"}");

        ShapeId first = document.getDiscriminator();

        assertThat(first).isEqualTo(ShapeId.from("com.foo#Foo"));
        assertThat(document.getDiscriminator()).isSameAs(first);
        assertThat(document.expectDiscriminator()).isSameAs(first);
    }

    @Test
    void getDiscriminator_withoutStringTypeMember_returnsNull() {
        assertThat(read("{}").getDiscriminator()).isNull();
        assertThat(read("{\"__type\":1}").getDiscriminator()).isNull();
        assertThat(read("{\"__type\":null}").getDiscriminator()).isNull();
        assertThat(read("[]").getDiscriminator()).isNull();
        assertThatThrownBy(() -> read("{}").expectDiscriminator())
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("map document has no discriminator");
    }

    @Test
    void getDiscriminator_invalidShapeId_throwsOnEveryCall() {
        Document document = read("{\"__type\":\"com.example#\"}");

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(document::getDiscriminator)
                .isInstanceOf(DiscriminatorException.class)
                .hasMessage("Unable to parse the document discriminator into a valid shape ID: com.example#");
        }
    }

    @Test
    void getDiscriminator_ofNestedObject_readsItsOwnTypeMember() {
        Document document = read("{\"__type\":\"a#A\",\"b\":{\"__type\":\"b#B\"}}");

        assertThat(document.getDiscriminator()).isEqualTo(ShapeId.from("a#A"));
        assertThat(document.getMember("b").getDiscriminator()).isEqualTo(ShapeId.from("b#B"));
    }

    static Stream<Arguments> settings() {
        return Stream.of(
            Arguments.of("REST_JSON_1", JsonSettings.REST_JSON_1),
            Arguments.of("AWS_JSON", JsonSettings.AWS_JSON),
            Arguments.of("defaults", JsonSettings.builder().build()),
            Arguments.of("timestampFormat off", JsonSettings.builder().useJsonName(true).useTimestampFormat(false)
                                                            .build()),
            Arguments.of("date-time default", JsonSettings.builder().useTimestampFormat(true)
                                                          .defaultTimestampFormat(TimestampFormatTrait.Format.DATE_TIME)
                                                          .build()),
            Arguments.of("http-date default, no timestampFormat",
                         JsonSettings.builder().defaultTimestampFormat(TimestampFormatTrait.Format.HTTP_DATE).build()),
            Arguments.of("strings for arbitrary precision", JsonSettings.builder().useJsonName(true)
                                                                        .useStringForArbitraryPrecision(true)
                                                                        .build()));
    }

    private static SchemaStruct allTypes(boolean secondsOnly) {
        return SchemaStruct.builder(ALL_TYPES)
                           .set("string", "hello")
                           .set("boolean", true)
                           .set("byte", (byte) -7)
                           .set("short", (short) 300)
                           .set("integer", Integer.MIN_VALUE)
                           .set("long", Long.MAX_VALUE)
                           .set("float", 1.1f)
                           .set("double", 0.1)
                           .set("nan", Double.NaN)
                           .set("negativeInfinity", Float.NEGATIVE_INFINITY)
                           .set("bigInteger", BigInteger.ONE.shiftLeft(70))
                           .set("bigDecimal", new BigDecimal("1.00000000000000000001"))
                           .set("blob", ByteBuffer.wrap(new byte[] {0, 1, 2, (byte) 0xff}))
                           .set("epoch", secondsOnly ? Instant.ofEpochSecond(1398796238L)
                                                     : Instant.ofEpochSecond(1398796238L, 123_456_789))
                           .set("dateTime", secondsOnly ? Instant.ofEpochSecond(1398796238L)
                                                        : Instant.ofEpochSecond(1398796238L, 120_000_000))
                           .set("httpDate", Instant.ofEpochSecond(1398796238L))
                           .set("list", listOf("a", "b"))
                           .set("map", mapOf("k", 1))
                           .set("inner", SchemaStruct.builder(INNER).set("value", "v").build())
                           .set("choice", SchemaStruct.builder(UNION).set("number", 5).build())
                           .set("color", "RED")
                           .set("priority", 100)
                           .set("document", Document.of(mapOf("x", Document.of(1))))
                           .build();
    }

    private static boolean httpDateDefault(JsonSettings settings) {
        return settings.defaultTimestampFormat() == TimestampFormatTrait.Format.HTTP_DATE;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("settings")
    void asShape_afterSerializingWithTheSameSettings_returnsAnEqualShape(String name, JsonSettings settings) {
        JsonCodec codec = JsonCodec.builder().settings(settings).build();
        // http-date has whole seconds only.
        SchemaStruct original = allTypes(httpDateDefault(settings));

        Document document = codec.createDeserializer(codec.serialize(original)).readDocument();
        SchemaStruct.Builder builder = SchemaStruct.builder(ALL_TYPES);
        SchemaStruct roundTripped = document.asShape(builder);

        assertThat(roundTripped).isEqualTo(original);
        assertThat(builder.unknownMembers()).isEmpty();
    }

    static Stream<Arguments> generatedClientSettings() {
        return Stream.of(Arguments.of("REST_JSON_1", JsonSettings.REST_JSON_1),
                         Arguments.of("AWS_JSON", JsonSettings.AWS_JSON));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedClientSettings")
    void serialize_ofReadDocument_writesTheSameJson(String name, JsonSettings settings) {
        JsonCodec codec = JsonCodec.builder().settings(settings).build();
        ByteBuffer serialized = codec.serialize(allTypes(false));

        Document document = codec.createDeserializer(serialized).readDocument();

        assertThat(utf8(codec.serialize(document))).isEqualTo(utf8(serialized));
    }

    static Stream<Arguments> settingsWithNumbersForArbitraryPrecision() {
        return settings().filter(arguments -> !((JsonSettings) arguments.get()[1]).useStringForArbitraryPrecision());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("settingsWithNumbersForArbitraryPrecision")
    void serialize_ofReadDocument_writesTheSameJsonValues(String name, JsonSettings settings) {
        JsonCodec codec = JsonCodec.builder().settings(settings).build();
        ByteBuffer serialized = codec.serialize(allTypes(httpDateDefault(settings)));
        Document document = codec.createDeserializer(serialized).readDocument();

        Document reread = codec.createDeserializer(codec.serialize(document)).readDocument();

        assertThat(reread).isEqualTo(document);
    }

    @Test
    void serialize_ofReadDocument_writesDoublesInTheSerializersForm() {
        // 1398796238.12 is a double; the serializer writes doubles of 1e7 and above with an exponent.
        assertThat(utf8(DEFAULT.serialize(read("[1398796238.12,0.5]")))).isEqualTo("[1.39879623812E9,0.5]");
    }

    @Test
    void serialize_withStringsForArbitraryPrecision_writesBigNumbersAsStrings() {
        JsonCodec codec = JsonCodec.builder().useStringForArbitraryPrecision(true).build();

        // Numbers too precise for a double are big decimal documents, which these settings write as strings.
        assertThat(utf8(codec.serialize(read(codec, "[1398796238.123456789,18446744073709551616,1.5]"))))
            .isEqualTo("[\"1398796238.123456789\",\"18446744073709551616\",1.5]");
    }

    @Test
    void asShape_matchesMembersByJsonNameOnlyWhenTheSettingsUseIt() {
        String json = "{\"STRING\":\"a\",\"string\":\"b\",\"nested\":{\"VALUE\":\"c\",\"value\":\"d\"}}";

        SchemaStruct withJsonName = read(JsonCodec.builder().useJsonName(true).build(), json)
            .asShape(SchemaStruct.builder(ALL_TYPES));
        SchemaStruct.Builder memberNameBuilder = SchemaStruct.builder(ALL_TYPES);
        SchemaStruct withMemberName = read(json).asShape(memberNameBuilder);

        assertThat(withJsonName.get("string")).isEqualTo("a");
        assertThat(((SchemaStruct) withJsonName.get("inner")).get("value")).isEqualTo("c");
        assertThat(withMemberName.get("string")).isEqualTo("b");
        assertThat(withMemberName.get("inner")).isNull();
        assertThat(memberNameBuilder.unknownMembers()).containsExactly("STRING", "nested");
    }

    @Test
    void asShape_readsTimestampMembersWithTheirTraitOnlyWhenTheSettingsUseIt() {
        String json = "{\"epoch\":1398796238,\"dateTime\":\"2014-04-29T18:30:38Z\"}";
        Instant expected = Instant.ofEpochSecond(1398796238L);

        SchemaStruct withTrait = read(JsonCodec.builder().useTimestampFormat(true).build(), json)
            .asShape(SchemaStruct.builder(ALL_TYPES));

        assertThat(withTrait.get("epoch")).isEqualTo(expected);
        assertThat(withTrait.get("dateTime")).isEqualTo(expected);
        assertThatThrownBy(() -> read(JsonCodec.builder().useTimestampFormat(false).build(), json)
            .asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(TimestampFormatter.TimestampSyntaxError.class)
            .hasMessageContaining("EPOCH_SECONDS");
    }

    @Test
    void asShape_union_skipsTypeAndReportsOrRejectsUnknownMembers() {
        String json = "{\"__type\":\"test#Choice\",\"text\":\"t\",\"other\":1}";
        SchemaStruct.Builder builder = SchemaStruct.builder(UNION);

        SchemaStruct union = read(json).asShape(builder);

        assertThat(union.get("text")).isEqualTo("t");
        assertThat(builder.unknownMembers()).containsExactly("other");
        assertThatThrownBy(() -> read(JsonCodec.builder().forbidUnknownUnionMembers(true).build(), json)
            .asShape(SchemaStruct.builder(UNION)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unknown member other encountered for union test#Choice");
    }

    @Test
    void asShape_structure_reportsTypeAsUnknownAndSkipsNullMembers() {
        SchemaStruct.Builder builder = SchemaStruct.builder(INNER);

        SchemaStruct inner = read("{\"__type\":\"test#Inner\",\"value\":null}").asShape(builder);

        assertThat(inner.get("value")).isNull();
        assertThat(builder.unknownMembers()).containsExactly("__type");
    }

    @Test
    void asShape_readsNonFiniteStringsOnlyForFloatingPointMembers() {
        SchemaStruct shape = read("{\"double\":\"Infinity\",\"float\":\"-Infinity\",\"nan\":\"NaN\"}")
            .asShape(SchemaStruct.builder(ALL_TYPES));

        assertThat(shape.get("double")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(shape.get("float")).isEqualTo(Float.NEGATIVE_INFINITY);
        assertThat(shape.get("nan")).isEqualTo(Double.NaN);
        assertThatThrownBy(() -> read("{\"double\":\"1.5\"}").asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a double document, but found string");
        assertThatThrownBy(() -> read("{\"integer\":\"NaN\"}").asShape(SchemaStruct.builder(ALL_TYPES)))
            .hasMessage("Expected an integer document, but found string");
    }

    @Test
    void asShape_readsArbitraryPrecisionStringsOnlyWhenTheSettingsUseThem() {
        String json = "{\"bigInteger\":\"12345678901234567890123\",\"bigDecimal\":\"1.5\"}";
        JsonCodec strings = JsonCodec.builder().useStringForArbitraryPrecision(true).build();

        SchemaStruct shape = read(strings, json).asShape(SchemaStruct.builder(ALL_TYPES));
        SchemaStruct fromNumbers = read(strings, "{\"bigInteger\":1,\"bigDecimal\":2.5}")
            .asShape(SchemaStruct.builder(ALL_TYPES));

        assertThat(shape.get("bigInteger")).isEqualTo(new BigInteger("12345678901234567890123"));
        assertThat(shape.get("bigDecimal")).isEqualTo(new BigDecimal("1.5"));
        assertThat(fromNumbers.get("bigInteger")).isEqualTo(BigInteger.ONE);
        assertThat(fromNumbers.get("bigDecimal")).isEqualTo(new BigDecimal("2.5"));
        assertThatThrownBy(() -> read(json).asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a bigInteger document, but found string");
        assertThatThrownBy(() -> read(strings, "{\"bigDecimal\":\"x\"}").asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Invalid BigDecimal value for test#AllTypes$bigDecimal: x");
    }

    @Test
    void asShape_documentMemberKeepsTheJsonDocument() {
        SchemaStruct shape = read("{\"document\":{\"blob\":\"Zm9v\"}}").asShape(SchemaStruct.builder(ALL_TYPES));

        Document member = (Document) shape.get("document");
        assertThat(member).isInstanceOf(JsonDocuments.JsonDocument.class);
        assertThat(member.getMember("blob").asBlob()).isEqualTo(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void asShape_withMismatchedDocument_throwsSerializationException() {
        assertThatThrownBy(() -> read("\"a\"").asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a map, structure, or union document, but found string");
        assertThatThrownBy(() -> read("{\"list\":{}}").asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a list document, but found map");
    }

    @Test
    void of_nullArguments_throwNullPointerException() {
        assertThatThrownBy(() -> JsonDocuments.of((String) null, DEFAULT.settings()))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("value must not be null");
        assertThatThrownBy(() -> JsonDocuments.of("a", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("settings must not be null");
    }
}
