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

package software.amazon.awssdk.core.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * {@link DocumentConverter}: Legacy_Document to schema document and back (Requirements 21.1, 21.2, 21.3, 21.5, 21.6).
 */
class DocumentConverterTest {
    private static final String NS = "com.example";

    private static final Schema INNER = Schema.structureBuilder(ShapeId.fromParts(NS, "Inner"))
                                              .putMember("value", PreludeSchemas.STRING)
                                              .build();
    private static final Schema TAGS = Schema.listBuilder(ShapeId.fromParts(NS, "Tags"))
                                             .putMember("member", PreludeSchemas.STRING)
                                             .build();
    private static final Schema SCORES = Schema.mapBuilder(ShapeId.fromParts(NS, "Scores"))
                                               .putMember("key", PreludeSchemas.STRING)
                                               .putMember("value", PreludeSchemas.INTEGER)
                                               .build();
    private static final Schema OUTER =
        Schema.structureBuilder(ShapeId.fromParts(NS, "Outer"))
              .putMember("name", PreludeSchemas.STRING)
              .putMember("count", PreludeSchemas.INTEGER)
              .putMember("unset", PreludeSchemas.STRING)
              .putMember("created", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
              .putMember("data", PreludeSchemas.BLOB)
              .putMember("child", INNER)
              .putMember("tags", TAGS)
              .putMember("scores", SCORES)
              .putMember("extra", PreludeSchemas.DOCUMENT)
              .build();
    private static final Schema CHOICE = Schema.unionBuilder(ShapeId.fromParts(NS, "Choice"))
                                               .putMember("text", PreludeSchemas.STRING)
                                               .putMember("number", PreludeSchemas.INTEGER)
                                               .build();

    @Test
    void toSchemaDocument_nullArgument_throwsNullPointerException() {
        assertThatThrownBy(() -> DocumentConverter.toSchemaDocument(null))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessage("document");
    }

    @Test
    void toLegacy_nullArgument_throwsNullPointerException() {
        assertThatThrownBy(() -> DocumentConverter.toLegacy(null))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessage("document");
    }

    @Test
    void toSchemaDocument_nullDocument_isANullValuedDocumentThatConvertsBack() {
        software.amazon.awssdk.schema.serde.document.Document converted =
            DocumentConverter.toSchemaDocument(Document.fromNull());

        assertThat(converted).isNotNull();
        assertThat(converted.type()).isEqualTo(ShapeType.DOCUMENT);
        assertThat(converted.asObject()).isNull();
        assertThat(converted.getMember("any")).isNull();
        assertThat(converted).isEqualTo(DocumentConverter.toSchemaDocument(Document.fromNull()));
        assertThat(converted.hashCode()).isEqualTo(DocumentConverter.toSchemaDocument(Document.fromNull()).hashCode());
        assertThat(converted).hasToString("Document(null)");
        assertThatThrownBy(converted::asString)
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("string");
        assertThat(DocumentConverter.toLegacy(converted)).isEqualTo(Document.fromNull());
    }

    @Test
    void toSchemaDocument_nullDocument_isWrittenAsNull() {
        List<String> writes = new ArrayList<>();
        ShapeSerializer recorder = new SpecificShapeSerializer() {
            @Override
            public void writeNull(Schema schema) {
                writes.add("null " + schema.type().name());
            }

            @Override
            public void writeDocument(Schema schema, software.amazon.awssdk.schema.serde.document.Document value) {
                writes.add("document");
                value.serializeContents(this);
            }
        };

        DocumentConverter.toSchemaDocument(Document.fromNull()).serialize(recorder);

        assertThat(writes).containsExactly("document", "null DOCUMENT");
    }

    @Test
    void toSchemaDocument_stringAndBoolean_becomeStringAndBooleanDocuments() {
        software.amazon.awssdk.schema.serde.document.Document string =
            DocumentConverter.toSchemaDocument(Document.fromString("text"));
        software.amazon.awssdk.schema.serde.document.Document bool =
            DocumentConverter.toSchemaDocument(Document.fromBoolean(false));

        assertThat(string.type()).isEqualTo(ShapeType.STRING);
        assertThat(string.asString()).isEqualTo("text");
        assertThat(bool.type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(bool.asBoolean()).isFalse();
    }

    static Stream<Arguments> legacyNumbers() {
        return Stream.of(
            Arguments.of(Document.fromNumber(-7), ShapeType.INTEGER, "-7"),
            Arguments.of(Document.fromNumber(Long.MIN_VALUE), ShapeType.LONG, "-9223372036854775808"),
            Arguments.of(Document.fromNumber(new BigInteger("123456789012345678901234567890")), ShapeType.BIG_INTEGER,
                         "123456789012345678901234567890"),
            Arguments.of(Document.fromNumber(0.25), ShapeType.DOUBLE, "0.25"),
            Arguments.of(Document.fromNumber("1.50"), ShapeType.DOUBLE, "1.5"),
            Arguments.of(Document.fromNumber("2.0"), ShapeType.INTEGER, "2"),
            Arguments.of(Document.fromNumber("1E+3"), ShapeType.INTEGER, "1000"),
            Arguments.of(Document.fromNumber("3.14159265358979323846264338327950288"), ShapeType.BIG_DECIMAL,
                         "3.14159265358979323846264338327950288"),
            Arguments.of(Document.fromNumber(new BigDecimal("-0.000000000000000000000001")), ShapeType.DOUBLE,
                         "-1E-24"));
    }

    @ParameterizedTest
    @MethodSource("legacyNumbers")
    void toSchemaDocument_number_hasTheNarrowestExactTypeAndConvertsBackToTheSameSdkNumber(
        Document legacy, ShapeType type, String value) {
        software.amazon.awssdk.schema.serde.document.Document converted = DocumentConverter.toSchemaDocument(legacy);

        assertThat(converted.type()).isEqualTo(type);
        assertThat(converted.asBigDecimal()).isEqualByComparingTo(value);
        assertThat(converted.toString()).contains(legacy.asNumber().stringValue());
        Document back = DocumentConverter.toLegacy(converted);
        assertThat(back).isSameAs(legacy);
        assertThat(back.asNumber().stringValue()).isEqualTo(legacy.asNumber().stringValue());
    }

    @Test
    void toSchemaDocument_number_coercesWithJavaPrimitiveConversions() {
        software.amazon.awssdk.schema.serde.document.Document converted =
            DocumentConverter.toSchemaDocument(Document.fromNumber("4294967297.9"));
        software.amazon.awssdk.schema.serde.document.Document integral =
            DocumentConverter.toSchemaDocument(Document.fromNumber(4294967297L));

        assertThat(converted.type()).isEqualTo(ShapeType.DOUBLE);
        assertThat(converted.asLong()).isEqualTo(4294967297L);
        assertThat(converted.asInteger()).isEqualTo(Integer.MAX_VALUE);
        assertThat(integral.asInteger()).isEqualTo(1);
    }

    @Test
    void toSchemaDocument_listAndMap_keepOrderSizeKeysAndNulls() {
        software.amazon.awssdk.schema.serde.document.Document converted = DocumentConverter.toSchemaDocument(tree());

        assertThat(converted.type()).isEqualTo(ShapeType.MAP);
        assertThat(converted.getMemberNames()).containsExactly("name", "items", "none");
        assertThat(converted.getMember("none")).isNull();
        software.amazon.awssdk.schema.serde.document.Document items = converted.getMember("items");
        assertThat(items.type()).isEqualTo(ShapeType.LIST);
        assertThat(items.size()).isEqualTo(4);
        assertThat(items.asList().get(0).asString()).isEqualTo("a");
        assertThat(items.asList().get(1).type()).isEqualTo(ShapeType.BIG_DECIMAL);
        assertThat(items.asList().get(2).getMember("flag").asBoolean()).isTrue();
        assertThat(items.asList().get(3)).isNull();
    }

    @Test
    void toSchemaDocument_isAViewOverTheLegacyDocument() {
        List<Document> elements = new ArrayList<>();
        software.amazon.awssdk.schema.serde.document.Document converted =
            DocumentConverter.toSchemaDocument(Document.fromList(elements));

        elements.add(Document.fromString("late"));

        assertThat(converted.asList()).hasSize(1);
        assertThat(converted.asList().get(0).asString()).isEqualTo("late");
    }

    @Test
    void roundTrip_legacyTree_returnsTheOriginalDocument() {
        Document legacy = tree();

        Document back = DocumentConverter.toLegacy(DocumentConverter.toSchemaDocument(legacy));

        assertThat(back).isSameAs(legacy);
    }

    @Test
    void roundTrip_throughCopiedSchemaContainers_keepsEqualityAndNumberStrings() {
        Document legacy = tree();
        software.amazon.awssdk.schema.serde.document.Document converted = DocumentConverter.toSchemaDocument(legacy);
        Map<String, software.amazon.awssdk.schema.serde.document.Document> copy = new LinkedHashMap<>(converted.asMap());
        copy.put("items", software.amazon.awssdk.schema.serde.document.Document.of(
            new ArrayList<>(converted.getMember("items").asList())));

        Document back = DocumentConverter.toLegacy(software.amazon.awssdk.schema.serde.document.Document.of(copy));

        assertThat(back).isNotSameAs(legacy).isEqualTo(legacy);
        assertThat(back.hashCode()).isEqualTo(legacy.hashCode());
        assertThat(back.toString()).isEqualTo(legacy.toString());
        assertThat(back.asMap().get("items").asList().get(1).asNumber().stringValue())
            .isEqualTo("1.00000000000000000000000000001");
    }

    @Test
    void toLegacy_stringEnumAndBoolean_becomeStringAndBoolean() {
        assertThat(DocumentConverter.toLegacy(software.amazon.awssdk.schema.serde.document.Document.of("s")))
            .isEqualTo(Document.fromString("s"));
        assertThat(DocumentConverter.toLegacy(software.amazon.awssdk.schema.serde.document.Document.of(true)))
            .isEqualTo(Document.fromBoolean(true));
    }

    static Stream<Arguments> schemaNumbers() {
        return Stream.of(
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of((byte) -8), "-8"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of((short) 300), "300"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(Integer.MAX_VALUE), "2147483647"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(Long.MAX_VALUE),
                         "9223372036854775807"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(0.5f), "0.5"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(1.0), "1.0"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(1e20), "1.0E20"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(BigInteger.TEN.pow(30)),
                         "1000000000000000000000000000000"),
            Arguments.of(software.amazon.awssdk.schema.serde.document.Document.of(
                new BigDecimal("0.10000000000000000000001")), "0.10000000000000000000001"));
    }

    @ParameterizedTest
    @MethodSource("schemaNumbers")
    void toLegacy_number_usesTheFactoryOfTheValueType(software.amazon.awssdk.schema.serde.document.Document number,
                                                     String expected) {
        Document legacy = DocumentConverter.toLegacy(number);

        assertThat(legacy.isNumber()).isTrue();
        assertThat(legacy.asNumber().stringValue()).isEqualTo(expected);
        assertThat(legacy.asNumber().bigDecimalValue()).isEqualByComparingTo(number.asBigDecimal());
    }

    @Test
    void toLegacy_blob_becomesPaddedStandardBase64() {
        assertThat(DocumentConverter.toLegacy(blob(new byte[0]))).isEqualTo(Document.fromString(""));
        assertThat(DocumentConverter.toLegacy(blob(new byte[] {0}))).isEqualTo(Document.fromString("AA=="));
        assertThat(DocumentConverter.toLegacy(blob(new byte[] {(byte) 0xfb, (byte) 0xff})))
            .isEqualTo(Document.fromString("+/8="));
        assertThat(DocumentConverter.toLegacy(blob("hi!".getBytes(StandardCharsets.UTF_8))))
            .isEqualTo(Document.fromString("aGkh"));
    }

    @Test
    void toLegacy_blob_doesNotMoveTheDocumentsBuffer() {
        software.amazon.awssdk.schema.serde.document.Document blob =
            software.amazon.awssdk.schema.serde.document.Document.of(ByteBuffer.wrap(new byte[] {1, 2, 3}));

        DocumentConverter.toLegacy(blob);

        assertThat(blob.asBlob().remaining()).isEqualTo(3);
    }

    static Stream<Arguments> timestamps() {
        return Stream.of(
            Arguments.of(Instant.EPOCH, "0"),
            Arguments.of(Instant.ofEpochSecond(1398796238), "1398796238"),
            Arguments.of(Instant.ofEpochMilli(1398796238123L), "1398796238.123"),
            Arguments.of(Instant.ofEpochMilli(1500), "1.5"),
            Arguments.of(Instant.ofEpochSecond(1, 999_999_999), "1.999"),
            Arguments.of(Instant.ofEpochSecond(0, 999_999), "0"),
            Arguments.of(Instant.ofEpochMilli(-500), "-0.5"),
            Arguments.of(Instant.ofEpochSecond(-1, 999_999_999), "-0.001"),
            Arguments.of(Instant.ofEpochSecond(-1, 1), "-1"),
            Arguments.of(Instant.MAX, "31556889864403199.999"),
            Arguments.of(Instant.MIN, "-31557014167219200"));
    }

    @ParameterizedTest
    @MethodSource("timestamps")
    void toLegacy_timestamp_becomesEpochSecondsWithAtMostThreeFractionDigits(Instant timestamp, String expected) {
        Document legacy = DocumentConverter.toLegacy(software.amazon.awssdk.schema.serde.document.Document.of(timestamp));

        assertThat(legacy.isNumber()).isTrue();
        assertThat(legacy.asNumber().stringValue()).isEqualTo(expected);
    }

    @Test
    void toLegacy_listAndMap_convertRecursivelyWithNullsAsNullDocuments() {
        Map<String, software.amazon.awssdk.schema.serde.document.Document> inner = new LinkedHashMap<>();
        inner.put("when", software.amazon.awssdk.schema.serde.document.Document.of(Instant.ofEpochSecond(5)));
        inner.put("nothing", null);
        software.amazon.awssdk.schema.serde.document.Document list =
            software.amazon.awssdk.schema.serde.document.Document.of(Arrays.asList(
                software.amazon.awssdk.schema.serde.document.Document.of(inner),
                null,
                blob(new byte[] {0})));

        Document legacy = DocumentConverter.toLegacy(list);

        Map<String, Document> expectedInner = new LinkedHashMap<>();
        expectedInner.put("when", Document.fromNumber(5));
        expectedInner.put("nothing", Document.fromNull());
        assertThat(legacy).isEqualTo(Document.fromList(Arrays.asList(
            Document.fromMap(expectedInner), Document.fromNull(), Document.fromString("AA=="))));
        assertThat(legacy.asList().get(0).asMap().keySet()).containsExactly("when", "nothing");
    }

    @Test
    void toLegacy_emptyContainers_becomeEmptyContainers() {
        assertThat(DocumentConverter.toLegacy(software.amazon.awssdk.schema.serde.document.Document.EMPTY_MAP))
            .isEqualTo(Document.fromMap(Collections.emptyMap()));
        assertThat(DocumentConverter.toLegacy(
            software.amazon.awssdk.schema.serde.document.Document.of(Collections.emptyList())))
            .isEqualTo(Document.fromList(Collections.emptyList()));
    }

    @Test
    void toLegacy_structure_becomesAMapOfTheSetMembersByMemberNameWithoutDiscriminator() {
        Document extra = Document.mapBuilder().putNumber("n", "1.50").build();
        SerializableStruct outer = new TestStruct(OUTER, s -> {
            s.writeInteger(OUTER.member("count"), 3);
            s.writeString(OUTER.member("name"), "widget");
            s.writeTimestamp(OUTER.member("created"), Instant.ofEpochMilli(1000123));
            s.writeBlob(OUTER.member("data"), ByteBuffer.wrap("hi".getBytes(StandardCharsets.UTF_8)));
            s.writeStruct(OUTER.member("child"), new TestStruct(INNER, c -> c.writeString(INNER.member("value"), "v")));
            s.writeList(OUTER.member("tags"), Arrays.asList("x", "y"), 2, (tags, ls) -> {
                for (String tag : tags) {
                    ls.writeString(TAGS.listMember(), tag);
                }
            });
            s.writeMap(OUTER.member("scores"), Collections.singletonMap("k", 9), 1, (scores, ms) -> {
                for (Map.Entry<String, Integer> e : scores.entrySet()) {
                    ms.writeEntry(SCORES.mapKeyMember(), e.getKey(), e.getValue(),
                                  (v, vs) -> vs.writeInteger(SCORES.mapValueMember(), v));
                }
            });
            s.writeDocument(OUTER.member("extra"), DocumentConverter.toSchemaDocument(extra));
        });
        software.amazon.awssdk.schema.serde.document.Document document =
            software.amazon.awssdk.schema.serde.document.Document.of(outer);
        assertThat(document.getDiscriminator()).isEqualTo(OUTER.id());

        Document legacy = DocumentConverter.toLegacy(document);

        Document expected = Document.mapBuilder()
                                    .putNumber("count", 3)
                                    .putString("name", "widget")
                                    .putNumber("created", new BigDecimal("1000.123"))
                                    .putString("data", "aGk=")
                                    .putMap("child", c -> c.putString("value", "v"))
                                    .putList("tags", l -> l.addString("x").addString("y"))
                                    .putMap("scores", m -> m.putNumber("k", 9))
                                    // Document.of(struct) copies a document member through serializeContents, so the
                                    // number keeps its value (a double) but not its SdkNumber spelling "1.50".
                                    .putMap("extra", e -> e.putNumber("n", 1.5))
                                    .build();
        assertThat(legacy).isEqualTo(expected);
        assertThat(legacy.asMap().keySet()).containsExactly("count", "name", "created", "data", "child", "tags", "scores",
                                                            "extra");
    }

    @Test
    void toLegacy_union_becomesAMapOfTheSetMember() {
        software.amazon.awssdk.schema.serde.document.Document union =
            software.amazon.awssdk.schema.serde.document.Document.of(
                new TestStruct(CHOICE, s -> s.writeInteger(CHOICE.member("number"), 4)));

        Document legacy = DocumentConverter.toLegacy(union);

        assertThat(legacy).isEqualTo(Document.mapBuilder().putNumber("number", 4).build());
    }

    @Test
    void toLegacy_typeWithNoLegacyForm_throwsSerializationException() {
        software.amazon.awssdk.schema.serde.document.Document other = () -> ShapeType.DOCUMENT;

        assertThatThrownBy(() -> DocumentConverter.toLegacy(other))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unsupported document document");
    }

    private static software.amazon.awssdk.schema.serde.document.Document blob(byte[] bytes) {
        return software.amazon.awssdk.schema.serde.document.Document.of(bytes);
    }

    /**
     * {"name": "n", "items": ["a", 1.00000000000000000000000000001, {"flag": true, "num": 1E+2}, null], "none": null}
     */
    private static Document tree() {
        return Document.mapBuilder()
                       .putString("name", "n")
                       .putList("items", l -> l.addString("a")
                                               .addNumber("1.00000000000000000000000000001")
                                               .addMap(m -> m.putBoolean("flag", true).putNumber("num", "1E+2"))
                                               .addNull())
                       .putNull("none")
                       .build();
    }

    /**
     * A structure or union shaped like a generated class: the members are written by a callback.
     */
    private static final class TestStruct implements SerializableStruct {
        private final Schema schema;
        private final Consumer<ShapeSerializer> members;

        private TestStruct(Schema schema, Consumer<ShapeSerializer> members) {
            this.schema = schema;
            this.members = members;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            members.accept(serializer);
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new UnsupportedOperationException();
        }
    }
}
