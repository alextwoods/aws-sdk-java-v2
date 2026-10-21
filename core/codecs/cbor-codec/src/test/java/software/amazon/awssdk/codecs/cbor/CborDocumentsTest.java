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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Tests of the CBOR Protocol_Documents ({@link CborDocuments}) that the CBOR codec reads: the document type of each
 * CBOR value, the settings they hold, discriminators, equality, and {@code asShape} round trips.
 */
class CborDocumentsTest {

    private static final Rpcv2CborCodec CODEC = Rpcv2CborCodec.builder().build();

    private static final Schema INNER = Schema.structureBuilder(ShapeId.from("test#Inner"))
                                              .putMember("value", PreludeSchemas.STRING)
                                              .build();

    private static final Schema UNION = Schema.unionBuilder(ShapeId.from("test#Choice"))
                                              .putMember("text", PreludeSchemas.STRING)
                                              .putMember("number", PreludeSchemas.INTEGER)
                                              .build();

    private static final Schema STRING_LIST = Schema.listBuilder(ShapeId.from("test#StringList"))
                                                    .putMember("member", PreludeSchemas.STRING)
                                                    .build();

    private static final Schema INTEGER_MAP = Schema.mapBuilder(ShapeId.from("test#IntegerMap"))
                                                    .putMember("key", PreludeSchemas.STRING)
                                                    .putMember("value", PreludeSchemas.INTEGER)
                                                    .build();

    private static final Schema ALL_TYPES =
        Schema.structureBuilder(ShapeId.from("test#AllTypes"))
              .putMember("string", PreludeSchemas.STRING)
              .putMember("boolean", PreludeSchemas.BOOLEAN)
              .putMember("byte", PreludeSchemas.BYTE)
              .putMember("short", PreludeSchemas.SHORT)
              .putMember("integer", PreludeSchemas.INTEGER)
              .putMember("long", PreludeSchemas.LONG)
              .putMember("float", PreludeSchemas.FLOAT)
              .putMember("double", PreludeSchemas.DOUBLE)
              .putMember("nan", PreludeSchemas.DOUBLE)
              .putMember("bigInteger", PreludeSchemas.BIG_INTEGER)
              .putMember("bigDecimal", PreludeSchemas.BIG_DECIMAL)
              .putMember("blob", PreludeSchemas.BLOB)
              .putMember("timestamp", PreludeSchemas.TIMESTAMP)
              .putMember("fractionalTimestamp", PreludeSchemas.TIMESTAMP)
              .putMember("list", STRING_LIST)
              .putMember("map", INTEGER_MAP)
              .putMember("inner", INNER)
              .putMember("choice", UNION)
              .putMember("document", PreludeSchemas.DOCUMENT)
              .build();

    private static Document read(Document value) {
        return read(CODEC, value);
    }

    private static Document read(Rpcv2CborCodec codec, Document value) {
        return codec.createDeserializer(codec.serialize(value)).readDocument();
    }

    private static Document read(byte... cbor) {
        return CODEC.createDeserializer(cbor).readDocument();
    }

    private static Map<String, Document> map(Object... keysAndValues) {
        Map<String, Document> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            result.put((String) keysAndValues[i], (Document) keysAndValues[i + 1]);
        }
        return result;
    }

    private static CborSettings settingsOf(Document document) {
        assertThat(document).isInstanceOf(CborDocuments.CborDocument.class);
        return ((CborDocuments.CborDocument) document).settings();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }

    @Test
    void readDocument_everyValueType_isACborDocumentHoldingTheCodecSettings() {
        Rpcv2CborCodec codec = Rpcv2CborCodec.builder().defaultNamespace("com.example").build();
        List<Document> values = Arrays.asList(
            Document.of(true), Document.of(-5L), Document.of(BigInteger.ONE.shiftLeft(70)), Document.of(1.1),
            Document.of(new BigDecimal("1.25")), Document.of("s"), Document.of(new byte[] {1, 2}),
            Document.of(Instant.ofEpochSecond(10)), Document.of(Arrays.asList(Document.of(1L))),
            Document.of(map("a", Document.of(1L))));

        Document document = read(codec, Document.of(values));

        assertThat(settingsOf(document)).isSameAs(codec.settings());
        assertThat(document.asList()).extracting(Document::type)
                                     .containsExactly(ShapeType.BOOLEAN, ShapeType.LONG, ShapeType.BIG_INTEGER,
                                                      ShapeType.DOUBLE, ShapeType.BIG_DECIMAL, ShapeType.STRING,
                                                      ShapeType.BLOB, ShapeType.TIMESTAMP, ShapeType.LIST,
                                                      ShapeType.MAP);
        for (Document value : document.asList()) {
            assertThat(settingsOf(value)).isSameAs(codec.settings());
        }
        assertThat(settingsOf(document.asList().get(8).asList().get(0))).isSameAs(codec.settings());
        assertThat(settingsOf(document.asList().get(9).getMember("a"))).isSameAs(codec.settings());
    }

    @Test
    void readDocument_floats_keepTheirEncodedWidth() {
        // f9 3c00: half-precision 1.0; fa 3fc00000: single-precision 1.5; fb ...: double-precision 1.1.
        Document half = read((byte) 0xf9, (byte) 0x3c, (byte) 0x00);
        Document single = read((byte) 0xfa, (byte) 0x3f, (byte) 0xc0, (byte) 0x00, (byte) 0x00);
        Document dbl = read(Document.of(1.1));

        assertThat(half.type()).isEqualTo(ShapeType.FLOAT);
        assertThat(half.asFloat()).isEqualTo(1.0f);
        assertThat(single.type()).isEqualTo(ShapeType.FLOAT);
        assertThat(single.asDouble()).isEqualTo(1.5);
        assertThat(dbl.type()).isEqualTo(ShapeType.DOUBLE);
        assertThat(dbl.asDouble()).isEqualTo(1.1);
        assertThat(settingsOf(half)).isSameAs(CODEC.settings());
    }

    @Test
    void readDocument_byteStringAndTag1_areBlobAndTimestampDocuments() {
        Document blob = read(Document.of("foo".getBytes(StandardCharsets.UTF_8)));
        Document timestamp = read(Document.of(Instant.ofEpochSecond(1398796238L, 123_000_000)));
        // c1 1a 535fefce: tag 1 over the integer 1398796238.
        Document integerTimestamp = read((byte) 0xc1, (byte) 0x1a, (byte) 0x53, (byte) 0x5f, (byte) 0xef,
                                         (byte) 0xce);

        assertThat(blob.type()).isEqualTo(ShapeType.BLOB);
        assertThat(blob.asBlob()).isEqualTo(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
        assertThat(timestamp.type()).isEqualTo(ShapeType.TIMESTAMP);
        assertThat(timestamp.asTimestamp()).isEqualTo(Instant.ofEpochSecond(1398796238L, 123_000_000));
        assertThat(integerTimestamp.asTimestamp()).isEqualTo(Instant.ofEpochSecond(1398796238L));
    }

    @Test
    void asBlob_returnsAnIndependentBuffer() {
        Document blob = read(Document.of(new byte[] {1, 2, 3}));

        blob.asBlob().get();

        assertThat(blob.asBlob().remaining()).isEqualTo(3);
    }

    @Test
    void coercions_doNotConvertBetweenStringsBlobsNumbersAndTimestamps() {
        assertThatThrownBy(() -> read(Document.of(1398796238L)).asTimestamp())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a timestamp document, but found long");
        assertThatThrownBy(() -> read(Document.of("Zm9v")).asBlob())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a blob document, but found string");
        assertThatThrownBy(() -> read(Document.of(new byte[] {1})).asString())
            .hasMessage("Expected a string document, but found blob");
        assertThatThrownBy(() -> read(Document.of(Instant.EPOCH)).asLong())
            .hasMessage("Expected a long document, but found timestamp");
    }

    @Test
    void numericCoercions_followJlsNarrowingWithoutThrowing() {
        assertThat(read(Document.of(4294967297L)).asInteger()).isEqualTo(1);
        assertThat(read(Document.of(1.9)).asInteger()).isEqualTo(1);
        assertThat(read(Document.of(128L)).asByte()).isEqualTo((byte) -128);
        assertThat(read(Document.of(BigInteger.ONE.shiftLeft(64).add(BigInteger.ONE))).asLong()).isEqualTo(1L);
        assertThat(read(Document.of(new BigDecimal("2.75"))).asInteger()).isEqualTo(2);
        assertThat(read(Document.of(Long.MAX_VALUE)).asBigDecimal()).isEqualTo(BigDecimal.valueOf(Long.MAX_VALUE));
        assertThatThrownBy(() -> read(Document.of(Double.NaN)).asBigDecimal())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot convert a non-finite double document (NaN) to a bigDecimal");
    }

    @Test
    void equals_matchesUntypedDocumentsOfTheSameTypeAndValue() {
        Document blob = Document.of(new byte[] {1, 2});
        Document timestamp = Document.of(Instant.ofEpochSecond(5));
        Document map = Document.of(map("a", Document.of(1L), "b", null));

        assertThat(read(blob)).isEqualTo(blob).hasSameHashCodeAs(blob);
        assertThat(read(timestamp)).isEqualTo(timestamp).hasSameHashCodeAs(timestamp);
        assertThat(read(map)).isEqualTo(map).hasSameHashCodeAs(map);
        assertThat(read(Document.of(1L))).isNotEqualTo(Document.of(1));
        assertThat(read(Rpcv2CborCodec.builder().defaultNamespace("x").build(), map)).isEqualTo(read(map));
    }

    @Test
    void toString_showsTypeAndValue() {
        assertThat(read(Document.of("a"))).hasToString("CborDocument(string: a)");
        assertThat(read(Document.of(new byte[] {1, 2}))).hasToString("CborDocument(blob: 2 bytes)");
        assertThat(read(Document.of(Arrays.asList(Document.of(1L)))))
            .hasToString("CborDocument(list: [CborDocument(long: 1)])");
    }

    @Test
    void getDiscriminator_isParsedOnceAndCached() {
        Rpcv2CborCodec codec = Rpcv2CborCodec.builder().defaultNamespace("com.foo").build();
        Document document = read(codec, Document.of(map("__type", Document.of("Foo"))));

        ShapeId first = document.getDiscriminator();

        assertThat(first).isEqualTo(ShapeId.from("com.foo#Foo"));
        assertThat(document.getDiscriminator()).isSameAs(first);
    }

    @Test
    void getDiscriminator_withoutTextTypeMember_returnsNull() {
        assertThat(read(Document.of(map())).getDiscriminator()).isNull();
        assertThat(read(Document.of(map("__type", Document.of(1L)))).getDiscriminator()).isNull();
        assertThat(read(Document.of(map("__type", null))).getDiscriminator()).isNull();
    }

    @Test
    void getDiscriminator_invalidShapeId_throwsOnEveryCall() {
        Document document = read(Document.of(map("__type", Document.of("com.example#"))));

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(document::getDiscriminator)
                .isInstanceOf(DiscriminatorException.class)
                .hasMessage("Unable to parse the document discriminator into a valid shape ID: com.example#");
        }
    }

    private static SchemaStruct allTypes() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
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
                           .set("bigInteger", BigInteger.ONE.shiftLeft(70))
                           .set("bigDecimal", new BigDecimal("1.00000000000000000001"))
                           .set("blob", ByteBuffer.wrap(new byte[] {0, 1, 2, (byte) 0xff}))
                           .set("timestamp", Instant.ofEpochSecond(1398796238L))
                           .set("fractionalTimestamp", Instant.ofEpochSecond(1398796238L, 123_000_000))
                           .set("list", Arrays.asList("a", "b"))
                           .set("map", map)
                           .set("inner", SchemaStruct.builder(INNER).set("value", "v").build())
                           .set("choice", SchemaStruct.builder(UNION).set("number", 5).build())
                           .set("document", Document.of(map("x", Document.of(1L))))
                           .build();
    }

    @Test
    void asShape_afterSerializing_returnsAnEqualShape() {
        SchemaStruct original = allTypes();

        Document document = CODEC.createDeserializer(CODEC.serialize(original)).readDocument();
        SchemaStruct.Builder builder = SchemaStruct.builder(ALL_TYPES);
        SchemaStruct roundTripped = document.asShape(builder);

        assertThat(roundTripped).isEqualTo(original);
        assertThat(builder.unknownMembers()).isEmpty();
    }

    @Test
    void serialize_ofReadStructure_writesTheSameValuesWithDefiniteLengthMaps() {
        ByteBuffer serialized = CODEC.serialize(allTypes());
        Document document = CODEC.createDeserializer(serialized).readDocument();

        ByteBuffer reserialized = CODEC.serialize(document);

        // The serializer writes structures as indefinite-length maps (0xbf) and map documents with their length.
        assertThat(bytes(serialized)[0]).isEqualTo((byte) 0xbf);
        assertThat(bytes(reserialized)[0]).isEqualTo((byte) 0xb3);
        assertThat(CODEC.createDeserializer(reserialized).readDocument()).isEqualTo(document);
    }

    @Test
    void serialize_ofReadDocument_writesTheSameCbor() {
        ByteBuffer serialized = CODEC.serialize(CODEC.createDeserializer(CODEC.serialize(allTypes())).readDocument());

        Document document = CODEC.createDeserializer(serialized).readDocument();

        assertThat(bytes(CODEC.serialize(document))).isEqualTo(bytes(serialized));
    }

    @Test
    void asShape_union_skipsTypeAndReportsOrRejectsUnknownMembers() {
        Document value = Document.of(map("__type", Document.of("test#Choice"), "text", Document.of("t"),
                                         "other", Document.of(1L)));
        SchemaStruct.Builder builder = SchemaStruct.builder(UNION);

        SchemaStruct union = read(value).asShape(builder);

        assertThat(union.get("text")).isEqualTo("t");
        assertThat(builder.unknownMembers()).containsExactly("other");
        assertThatThrownBy(() -> read(Rpcv2CborCodec.builder().forbidUnknownUnionMembers(true).build(), value)
            .asShape(SchemaStruct.builder(UNION)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Unknown member other encountered for union test#Choice");
    }

    @Test
    void asShape_structure_reportsUnknownMembersAndSkipsNullMembers() {
        SchemaStruct.Builder builder = SchemaStruct.builder(INNER);

        SchemaStruct inner = read(Document.of(map("__type", Document.of("test#Inner"), "value", null)))
            .asShape(builder);

        assertThat(inner.get("value")).isNull();
        assertThat(builder.unknownMembers()).containsExactly("__type");
    }

    @Test
    void asShape_withMismatchedDocument_throwsSerializationException() {
        assertThatThrownBy(() -> read(Document.of("a")).asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a map, structure, or union document, but found string");
        assertThatThrownBy(() -> read(Document.of(map("timestamp", Document.of(1L))))
            .asShape(SchemaStruct.builder(ALL_TYPES)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a timestamp document, but found long");
    }

    @Test
    void of_nullArguments_throwNullPointerException() {
        assertThatThrownBy(() -> CborDocuments.of((String) null, CODEC.settings()))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("value must not be null");
        assertThatThrownBy(() -> CborDocuments.of(true, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("settings must not be null");
    }
}
