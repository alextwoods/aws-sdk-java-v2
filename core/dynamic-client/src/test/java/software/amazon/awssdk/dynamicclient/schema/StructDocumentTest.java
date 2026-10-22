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

package software.amazon.awssdk.dynamicclient.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.schema.TestModels.id;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.shapes.ServiceShape;

class StructDocumentTest {
    private static final SchemaConverter CONVERTER =
        new SchemaConverter(TestModels.converter(), TestModels.converter().expectShape(id("DynService"),
                                                                                       ServiceShape.class));
    private static final Schema OUTPUT = CONVERTER.getSchema(id("GetThingOutput"));
    private static final Schema NESTED = CONVERTER.getSchema(id("Nested"));
    private static final Schema CHOICE = CONVERTER.getSchema(id("Choice"));
    private static final JsonCodec JSON = JsonCodec.builder().build();

    @Test
    void of_mapDocument_convertsEachMemberToItsSchemaType() {
        StructDocument doc = StructDocument.of(OUTPUT, Document.of(fullOutput()));

        assertThat(doc.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(doc.sdkSchema()).isSameAs(OUTPUT);
        assertThat(doc.getDiscriminator()).hasToString("test.dyn#GetThingOutput");
        assertThat(doc.getMember("b").type()).isEqualTo(ShapeType.BYTE);
        assertThat(doc.getMember("b").asNumber()).isEqualTo((byte) 7);
        assertThat(doc.getMember("f").asNumber()).isEqualTo(1.5f);
        assertThat(doc.getMember("bi").asBigInteger()).isEqualTo(BigInteger.valueOf(12));
        assertThat(doc.getMember("bd").asBigDecimal()).isEqualTo(new BigDecimal("1.25"));
        assertThat(doc.getMember("blob").asBlob()).isEqualTo(ByteBuffer.wrap(new byte[] {1, 2, 3}));
        // Enum and intEnum documents report their data model type, as other documents do.
        assertThat(doc.getMember("color").type()).isEqualTo(ShapeType.STRING);
        assertThat(doc.getMember("level").type()).isEqualTo(ShapeType.INTEGER);
        assertThat(doc.getMember("level").asNumber()).isEqualTo(10);
        assertThat(doc.getMember("list").asList()).containsExactly(Document.of("x"), Document.of("y"));
        assertThat(doc.getMember("map").getMember("one").asInteger()).isEqualTo(1);
        assertThat(doc.getMember("doc").type()).isEqualTo(ShapeType.MAP);
        assertThat(doc.getMember("doc").getMember("k").asString()).isEqualTo("v");
        // A nested structure's discriminator is the structure's ShapeId, not the member's.
        assertThat(doc.getMember("nested").getDiscriminator()).hasToString("test.dyn#Nested");
        assertThat(doc.getMember("nested")).isInstanceOf(StructDocument.class);
    }

    @Test
    void of_numbers_convertWithJavaPrimitiveConversions() {
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("i", Document.of(4294967297L));
        values.put("l", Document.of(1.9));
        values.put("b", Document.of(300));
        values.put("d", Document.of(new BigDecimal("2.5")));
        StructDocument doc = StructDocument.of(OUTPUT, Document.of(values));

        assertThat(doc.getMember("i").asInteger()).isEqualTo(1);
        assertThat(doc.getMember("i").asNumber()).isInstanceOf(Integer.class);
        assertThat(doc.getMember("l").asLong()).isEqualTo(1L);
        assertThat(doc.getMember("b").asByte()).isEqualTo((byte) 44);
        assertThat(doc.getMember("d").asDouble()).isEqualTo(2.5);
        assertThat(doc.getMember("i").asBigDecimal()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void of_timestamps_acceptTimestampsEpochSecondsAndDateTimeStrings() {
        Instant expected = Instant.parse("2024-01-02T03:04:05Z");

        assertThat(timestamp(Document.of(expected))).isEqualTo(expected);
        assertThat(timestamp(Document.of(expected.getEpochSecond()))).isEqualTo(expected);
        assertThat(timestamp(Document.of("2024-01-02T03:04:05Z"))).isEqualTo(expected);
    }

    @Test
    void of_unknownMembers_areIgnoredAndUnsetMembersAreNull() {
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("str", Document.of("s"));
        values.put("notAMember", Document.of(1));
        StructDocument doc = StructDocument.of(OUTPUT, Document.of(values));

        assertThat(doc.getMemberNames()).containsExactly("str");
        assertThat(doc.getMember("notAMember")).isNull();
        assertThat(doc.getMember("i")).isNull();
        assertThat(doc.size()).isEqualTo(1);
    }

    @Test
    void of_invalidArguments_throw() {
        assertThatThrownBy(() -> StructDocument.of(CONVERTER.getSchema(id("StringList")), Document.EMPTY_MAP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Schema must be a structure or union, got list");
        assertThatThrownBy(() -> StructDocument.of(NESTED, Document.of("text")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Document must be a map, structure, or union, but got string");
        assertThatThrownBy(() -> StructDocument.of(NESTED, Document.of(Collections.singletonMap("name",
                                                                                                Document.of(1)))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a string document, but found integer");
    }

    @Test
    void of_streamingMemberWithABlob_fails() {
        Schema download = CONVERTER.getSchema(id("DownloadOutput"));

        assertThatThrownBy(() -> StructDocument.of(download, Document.of(Collections.singletonMap(
            "body", Document.of(new byte[] {1})))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot coerce member test.dyn#DownloadOutput$body: a streaming blob member needs a data stream "
                        + "document, but found a blob document. Create one with "
                        + "DynamicStreams.dataStream(RequestBody) or DynamicStreams.dataStream(AsyncRequestBody).");
    }

    @Test
    void of_streamingMemberWithADataStream_keepsTheStreamWithTheMemberSchema() {
        Schema download = CONVERTER.getSchema(id("DownloadOutput"));
        DataStream stream = new TestDataStream();

        StructDocument document = StructDocument.of(download, Document.of(Collections.singletonMap(
            "body", Document.of(PreludeSchemas.BLOB, stream))));

        Document body = document.getMember("body");
        assertThat(body.type()).isEqualTo(ShapeType.BLOB);
        assertThat(body.asDataStream()).isSameAs(stream);
        assertThat(body).isEqualTo(Document.of(download.member("body"), stream));
        assertThat(document.<Object>getMemberValue(download.member("body"))).isNull();
        RecordingSerializer serializer = new RecordingSerializer();
        document.serializeMembers(serializer);
        assertThat(serializer.events).isEmpty();
    }

    /**
     * A data stream that publishes nothing; the documents never subscribe to it.
     */
    private static final class TestDataStream implements DataStream {
        @Override
        public long contentLength() {
            return 0;
        }

        @Override
        public String contentType() {
            return null;
        }

        @Override
        public boolean isReplayable() {
            return false;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void union_isAStructureDocumentWithOneMemberAndNoDiscriminator() {
        StructDocument union = StructDocument.of(CHOICE, Document.of(Collections.singletonMap("number",
                                                                                              Document.of(5))));

        assertThat(union.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(union.getDiscriminator()).isNull();
        assertThat(union.getMemberNames()).containsExactly("number");
        assertThat(union.getMember("number").asInteger()).isEqualTo(5);
        assertThat(union.getMember("text")).isNull();
        assertThat(union.size()).isEqualTo(1);
        assertThat(union.asObject()).isEqualTo(Collections.singletonMap("number", 5));
        assertThat(union.<Integer>getMemberValue(CHOICE.member("number"))).isEqualTo(5);
        assertThat(union.<Object>getMemberValue(CHOICE.member("text"))).isNull();
    }

    @Test
    void asMap_isUnmodifiableAndInSchemaOrder() {
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("tags", Document.of(Collections.singletonList(Document.of("t"))));
        values.put("name", Document.of("n"));
        StructDocument doc = StructDocument.of(NESTED, Document.of(values));

        assertThat(doc.asMap().keySet()).containsExactly("name", "tags");
        assertThatThrownBy(() -> doc.asMap().put("x", null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> doc.getMember("tags").asList().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void contentDocument_wrongType_throwsTheDocumentCoercionError() {
        StructDocument doc = StructDocument.of(OUTPUT, Document.of(fullOutput()));

        assertThatThrownBy(() -> doc.getMember("str").asInteger())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected an integer document, but found string");
        assertThatThrownBy(() -> doc.getMember("i").asString())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a string document, but found integer");
        assertThatThrownBy(() -> doc.getMember("list").asMap())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a map document, but found list");
    }

    @Test
    void equalsAndHashCode_compareTypeAndMembersOnly() {
        StructDocument first = StructDocument.of(OUTPUT, Document.of(fullOutput()));
        StructDocument second = StructDocument.of(OUTPUT, Document.of(fullOutput()));

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.getMember("str")).isEqualTo(Document.of("text"));
        assertThat(first.getMember("str").hashCode()).isEqualTo(Document.of("text").hashCode());
        assertThat(first.getMember("color")).isEqualTo(Document.of("red"));
        assertThat(first.getMember("list")).isEqualTo(Document.of(Arrays.asList(Document.of("x"), Document.of("y"))));
        // A map document has type MAP, a structure document STRUCTURE.
        assertThat(first).isNotEqualTo(Document.of(first.asMap()));
    }

    @Test
    void toString_redactsSensitiveValues() {
        StructDocument doc = StructDocument.of(OUTPUT, Document.of(fullOutput()));

        assertThat(doc.getMember("secret")).hasToString("Document(string: *** Sensitive Data Redacted ***)");
        assertThat(doc.toString()).contains("secret=Document(string: *** Sensitive Data Redacted ***)")
                                  .contains("str=Document(string: text)")
                                  .doesNotContain("hunter2");
    }

    @Test
    void getMemberValue_returnsStandardLibraryValuesAndRejectsOtherSchemas() {
        StructDocument doc = StructDocument.of(OUTPUT, Document.of(fullOutput()));

        assertThat(doc.<String>getMemberValue(OUTPUT.member("str"))).isEqualTo("text");
        assertThat(doc.<Object>getMemberValue(OUTPUT.member("nested")))
            .isEqualTo(Collections.singletonMap("name", "inner"));
        assertThat(doc.<Object>getMemberValue(OUTPUT.member("patterned"))).isNull();
        assertThatThrownBy(() -> doc.getMemberValue(NESTED.member("name")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serialize_writesOneDocumentAndSerializeContentsWritesTheStructure() {
        StructDocument doc = StructDocument.of(NESTED, Document.of(Collections.singletonMap("name", Document.of("n"))));

        RecordingSerializer asDocument = new RecordingSerializer();
        doc.serialize(asDocument);
        assertThat(asDocument.events).containsExactly("writeDocument(smithy.api#Document)=structure");

        RecordingSerializer contents = new RecordingSerializer();
        doc.serializeContents(contents);
        assertThat(contents.events).containsExactly("writeStruct(test.dyn#Nested)",
                                                    "writeString(test.dyn#Nested$name)=n");
    }

    @Test
    void serializeMembers_writesEachMemberWithItsMemberSchema() {
        RecordingSerializer serializer = new RecordingSerializer();
        StructDocument.of(OUTPUT, Document.of(fullOutput())).serializeMembers(serializer);

        assertThat(serializer.events).contains(
            "writeBlob(test.dyn#GetThingOutput$blob)=3",
            "writeInteger(test.dyn#GetThingOutput$level)=10",
            "writeString(test.dyn#GetThingOutput$color)=red",
            "writeList(test.dyn#GetThingOutput$list)=2",
            "writeString(test.dyn#StringList$member)=x",
            "writeMap(test.dyn#GetThingOutput$map)=1",
            "entry(test.dyn#IntMap$key)=one",
            "writeInteger(test.dyn#IntMap$value)=1",
            "writeDocument(test.dyn#GetThingOutput$doc)=map",
            "writeStruct(test.dyn#GetThingOutput$choice)",
            "writeString(test.dyn#Choice$text)=t",
            "writeStruct(test.dyn#GetThingOutput$nested)",
            "writeString(test.dyn#Nested$name)=inner");
    }

    @Test
    void codecRoundTrip_throughTheDocumentBuilder_givesAnEqualDocument() {
        StructDocument original = StructDocument.of(OUTPUT, Document.of(fullOutput()));
        String json = JSON.serializeToString(asStruct(original));

        StructDocument read = JSON.deserializeShape(json, SchemaConverter.createDocumentBuilder(OUTPUT));

        assertThat(read).isEqualTo(original);
        assertThat(read.getMember("choice").getMemberNames()).containsExactly("text");
        assertThat(JSON.serializeToString(asStruct(read))).isEqualTo(json);
    }

    @Test
    void codecRoundTrip_unionWithScalarAndAggregateMembers() {
        for (Map.Entry<String, Document> member : unionValues().entrySet()) {
            StructDocument union = StructDocument.of(CHOICE, Document.of(Collections.singletonMap(member.getKey(),
                                                                                                  member.getValue())));
            String json = JSON.serializeToString(asStruct(union));

            StructDocument read = JSON.deserializeShape(json, SchemaConverter.createDocumentBuilder(CHOICE));

            assertThat(read).as(member.getKey()).isEqualTo(union);
            assertThat(read.getMember(member.getKey())).isEqualTo(union.getMember(member.getKey()));
        }
    }

    @Test
    void documentBuilder_sparseAndDenseLists() {
        String json = "{\"sparse\":[\"a\",null],\"list\":[\"b\"]}";
        StructDocument read = JSON.deserializeShape(json, SchemaConverter.createDocumentBuilder(OUTPUT));
        assertThat(read.getMember("sparse").asList()).containsExactly(Document.of("a"), null);

        assertThatThrownBy(() -> JSON.deserializeShape("{\"list\":[\"b\",null]}",
                                                       SchemaConverter.createDocumentBuilder(OUTPUT)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Null value found in dense list: test.dyn#StringList$member");
    }

    @Test
    void asShape_withTheDocumentBuilder_copiesTheDocument() {
        StructDocument original = StructDocument.of(OUTPUT, Document.of(fullOutput()));

        assertThat(original.asShape(SchemaConverter.createDocumentBuilder(OUTPUT))).isEqualTo(original);
    }

    @Test
    void documentBuilder_setMemberValue_convertsValuesAndNullClears() {
        ShapeBuilder<StructDocument> builder = SchemaConverter.createDocumentBuilder(NESTED);
        builder.setMemberValue(NESTED.member("name"), "n");
        builder.setMemberValue(NESTED.member("tags"), Arrays.asList("a", "b"));
        StructDocument built = builder.build();

        assertThat(built.getMember("name").asString()).isEqualTo("n");
        assertThat(built.getMember("tags").asList()).containsExactly(Document.of("a"), Document.of("b"));

        builder.setMemberValue(NESTED.member("name"), null);
        assertThat(builder.build().getMemberNames()).containsExactly("tags");
        // The built document does not change with the builder.
        assertThat(built.getMemberNames()).containsExactly("name", "tags");
        assertThatThrownBy(() -> builder.setMemberValue(OUTPUT.member("str"), "x"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void documentBuilder_setMemberValue_acceptsSerializableShapes() {
        ShapeBuilder<StructDocument> builder = SchemaConverter.createDocumentBuilder(CHOICE);
        StructDocument nested = StructDocument.of(NESTED, Document.of(Collections.singletonMap("name",
                                                                                               Document.of("n"))));
        builder.setMemberValue(CHOICE.member("nested"), nested);

        assertThat(builder.build().getMember("nested")).isEqualTo(nested);
    }

    @Test
    void documentBuilder_unionWithoutValue_failsToBuild() {
        assertThatThrownBy(() -> SchemaConverter.createDocumentBuilder(CHOICE).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No value set for union document: test.dyn#Choice");
    }

    private static Instant timestamp(Document value) {
        return StructDocument.of(OUTPUT, Document.of(Collections.singletonMap("time", value)))
                             .getMember("time").asTimestamp();
    }

    private static SerializableShape asStruct(StructDocument doc) {
        return serializer -> serializer.writeStruct(doc.sdkSchema(), doc);
    }

    private static Map<String, Document> unionValues() {
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("text", Document.of("t"));
        values.put("number", Document.of(42));
        values.put("nested", Document.of(Collections.singletonMap("name", Document.of("n"))));
        return values;
    }

    private static Map<String, Document> fullOutput() {
        Map<String, Document> values = new LinkedHashMap<>();
        values.put("blob", Document.of(new byte[] {1, 2, 3}));
        values.put("bool", Document.of(true));
        values.put("str", Document.of("text"));
        values.put("time", Document.of(Instant.parse("2024-01-02T03:04:05.123Z")));
        values.put("epoch", Document.of(Instant.ofEpochSecond(1700000000)));
        values.put("b", Document.of((byte) 7));
        values.put("s", Document.of((short) 8));
        values.put("i", Document.of(9));
        values.put("l", Document.of(10L));
        values.put("f", Document.of(1.5f));
        values.put("d", Document.of(2.25));
        values.put("bi", Document.of(BigInteger.valueOf(12)));
        values.put("bd", Document.of(new BigDecimal("1.25")));
        values.put("doc", Document.of(Collections.singletonMap("k", Document.of("v"))));
        values.put("color", Document.of("red"));
        values.put("level", Document.of(10));
        values.put("legacy", Document.of("a"));
        values.put("list", Document.of(Arrays.asList(Document.of("x"), Document.of("y"))));
        values.put("sparse", Document.of(Arrays.asList(Document.of("z"), null)));
        values.put("map", Document.of(Collections.singletonMap("one", Document.of(1))));
        values.put("choice", Document.of(Collections.singletonMap("text", Document.of("t"))));
        values.put("nested", Document.of(Collections.singletonMap("name", Document.of("inner"))));
        values.put("secret", Document.of("hunter2"));
        return values;
    }
}
