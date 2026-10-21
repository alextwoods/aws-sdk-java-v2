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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.document.DocumentEqualsFlags;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Tests of writing documents with the CBOR codec: the {@code __type} member of structure documents, and documents read
 * by the JSON codec written as CBOR and the reverse, each written with the writing codec's settings.
 */
class CborDocumentSerializationTest {
    private static final Instant ONE_SECOND = Instant.ofEpochSecond(1);

    private static final Rpcv2CborCodec CBOR = Rpcv2CborCodec.builder().build();
    private static final JsonCodec REST_JSON = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();

    private static final Schema INNER = Schema.structureBuilder(ShapeId.from("test#Inner"))
                                              .putMember("value", PreludeSchemas.STRING, new JsonNameTrait("VALUE"))
                                              .build();
    private static final Schema CHOICE = Schema.unionBuilder(ShapeId.from("test#Choice"))
                                               .putMember("text", PreludeSchemas.STRING)
                                               .build();
    private static final Schema PAYLOAD =
        Schema.structureBuilder(ShapeId.from("test#Payload"))
              .putMember("name", PreludeSchemas.STRING, new JsonNameTrait("Name"))
              .putMember("created", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
              .putMember("data", PreludeSchemas.BLOB)
              .putMember("inner", INNER)
              .putMember("document", PreludeSchemas.DOCUMENT)
              .build();

    private static SchemaStruct inner(String value) {
        return SchemaStruct.builder(INNER).set("value", value).build();
    }

    private static SchemaStruct payload(Document document) {
        return SchemaStruct.builder(PAYLOAD)
                           .set("name", "n")
                           .set("created", ONE_SECOND)
                           .set("data", ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)))
                           .set("inner", inner("v"))
                           .set("document", document)
                           .build();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }

    private static String utf8(ByteBuffer buffer) {
        return new String(bytes(buffer), StandardCharsets.UTF_8);
    }

    private static Document readCbor(Rpcv2CborCodec codec, ByteBuffer bytes) {
        return codec.createDeserializer(bytes).readDocument();
    }

    private static Document readJson(JsonCodec codec, String json) {
        return codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8)).readDocument();
    }

    /**
     * Concatenates CBOR items: an Integer is one initial byte, a String a short text string (under 24 bytes).
     */
    private static byte[] cbor(Object... items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Object item : items) {
            if (item instanceof Integer) {
                out.write((Integer) item);
            } else {
                byte[] text = ((String) item).getBytes(StandardCharsets.UTF_8);
                out.write(0x60 | text.length);
                out.write(text, 0, text.length);
            }
        }
        return out.toByteArray();
    }

    @Test
    void writeDocument_typedStructure_writesTypeFirst() {
        assertThat(bytes(CBOR.serialize(Document.of(inner("v")))))
            .isEqualTo(cbor(0xbf, "__type", "test#Inner", "value", "v", 0xff));
    }

    @Test
    void writeDocument_typedUnion_writesNoTypeBecauseUnionDocumentsHaveNoDiscriminator() {
        Document document = Document.of(SchemaStruct.builder(CHOICE).set("text", "t").build());

        assertThat(bytes(CBOR.serialize(document))).isEqualTo(cbor(0xbf, "text", "t", 0xff));
    }

    @Test
    void writeDocument_typedStructureReadBack_hasTheTypeOnlyOnTheDocumentsOwnStructure() {
        Document nested = Document.of(Arrays.asList(Document.of(inner("a")), Document.of(1)));

        Document read = readCbor(CBOR, CBOR.serialize(Document.of(payload(nested))));

        assertThat(read.getMemberNames()).containsExactly("__type", "name", "created", "data", "inner", "document");
        assertThat(read.getDiscriminator()).isEqualTo(ShapeId.from("test#Payload"));
        // CBOR writes tag 1 timestamps and byte strings, whatever the timestampFormat trait says.
        assertThat(read.getMember("created").type()).isEqualTo(ShapeType.TIMESTAMP);
        assertThat(read.getMember("created").asTimestamp()).isEqualTo(ONE_SECOND);
        assertThat(read.getMember("data").type()).isEqualTo(ShapeType.BLOB);
        assertThat(read.getMember("inner").getMemberNames()).containsExactly("value");
        Document element = read.getMember("document").asList().get(0);
        assertThat(element.getMemberNames()).containsExactly("__type", "value");
        assertThat(element.getDiscriminator()).isEqualTo(ShapeId.from("test#Inner"));
        assertThat(read.asShape(SchemaStruct.builder(PAYLOAD)).get("inner")).isEqualTo(inner("v"));
    }

    @Test
    void writeDocument_jsonDocumentWrittenAsCbor_writesTheJsonValuesAndReadsWithTheCborNamespace() {
        JsonCodec json = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).defaultNamespace("com.json").build();
        Rpcv2CborCodec cbor = Rpcv2CborCodec.builder().defaultNamespace("com.cbor").build();
        Document document = readJson(json, "{\"__type\":\"Payload\",\"Name\":\"n\",\"data\":\"Zm9v\","
                                           + "\"created\":\"1970-01-01T00:00:01Z\",\"n\":1,\"list\":[1.5,true]}");
        Map<String, Document> expected = new LinkedHashMap<>();
        expected.put("__type", Document.of("Payload"));
        expected.put("Name", Document.of("n"));
        expected.put("data", Document.of("Zm9v"));
        expected.put("created", Document.of("1970-01-01T00:00:01Z"));
        expected.put("n", Document.of(1L));
        expected.put("list", Document.of(Arrays.asList(Document.of(1.5), Document.of(true))));

        ByteBuffer written = cbor.serialize(document);
        Document read = readCbor(cbor, written);

        // The JSON document is a map: its keys, strings (the base64 text and the timestamp string), numbers and
        // relative __type are written as they were read, without the JSON settings.
        assertThat(bytes(written)).isEqualTo(bytes(CBOR.serialize(Document.of(expected))));
        assertThat(Document.equals(read, document, DocumentEqualsFlags.NUMBER_PROMOTION)).isTrue();
        assertThat(document.getDiscriminator()).isEqualTo(ShapeId.from("com.json#Payload"));
        assertThat(read.getDiscriminator()).isEqualTo(ShapeId.from("com.cbor#Payload"));
    }

    @Test
    void writeDocument_cborDocumentWrittenAsJson_writesBlobsAndTimestampsWithTheJsonSettings() {
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("__type", Document.of("Payload"));
        values.put("b", Document.of("foo".getBytes(StandardCharsets.UTF_8)));
        values.put("t", Document.of(ONE_SECOND));
        values.put("n", Document.of(1L));
        Document document = readCbor(CBOR, CBOR.serialize(Document.of(values)));
        JsonCodec dateTime = JsonCodec.builder()
                                      .settings(JsonSettings.REST_JSON_1)
                                      .defaultTimestampFormat(TimestampFormatTrait.Format.DATE_TIME)
                                      .build();

        assertThat(utf8(REST_JSON.serialize(document)))
            .isEqualTo("{\"__type\":\"Payload\",\"b\":\"Zm9v\",\"t\":1,\"n\":1}");
        assertThat(utf8(dateTime.serialize(document)))
            .isEqualTo("{\"__type\":\"Payload\",\"b\":\"Zm9v\",\"t\":\"1970-01-01T00:00:01Z\",\"n\":1}");
    }

    @Test
    void writeDocument_typedStructureFromACborDocument_writesJsonNamesAndAnAbsoluteType() {
        Rpcv2CborCodec cbor = Rpcv2CborCodec.builder().defaultNamespace("test").build();
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("__type", Document.of("Payload"));
        values.put("name", Document.of("n"));
        values.put("created", Document.of(ONE_SECOND));
        Document read = readCbor(cbor, CBOR.serialize(Document.of(values)));

        SchemaStruct shape = read.asShape(SchemaStruct.builder(PAYLOAD));

        assertThat(Document.of(shape).getDiscriminator()).isEqualTo(read.getDiscriminator());
        assertThat(utf8(REST_JSON.serialize(Document.of(shape))))
            .isEqualTo("{\"__type\":\"test#Payload\",\"Name\":\"n\",\"created\":\"1970-01-01T00:00:01Z\"}");
    }

    @Test
    void writeDocument_typedStructureFromAJsonDocument_writesAnAbsoluteTypeAsCbor() {
        JsonCodec json = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).defaultNamespace("test").build();
        Document read = readJson(json, "{\"__type\":\"Inner\",\"VALUE\":\"v\"}");

        Document typed = Document.of(read.asShape(SchemaStruct.builder(INNER)));

        assertThat(bytes(CBOR.serialize(typed))).isEqualTo(cbor(0xbf, "__type", "test#Inner", "value", "v", 0xff));
    }

    @Test
    void writeDocument_structureDocumentThatDoesNotWriteAStructure_throwsSerializationException() {
        Document document = new Document() {
            @Override
            public ShapeType type() {
                return ShapeType.STRUCTURE;
            }

            @Override
            public void serializeContents(ShapeSerializer serializer) {
                serializer.writeInteger(PreludeSchemas.INTEGER, 1);
            }
        };

        assertThatThrownBy(() -> CBOR.serialize(document))
            .isInstanceOf(SerializationException.class)
            .hasMessage("A structure document must write a structure, but wrote a value of type integer with schema "
                        + "smithy.api#Integer");
    }
}
