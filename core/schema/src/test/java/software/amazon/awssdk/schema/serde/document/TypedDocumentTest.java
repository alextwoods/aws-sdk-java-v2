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

package software.amazon.awssdk.schema.serde.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeIdSyntaxException;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.RecordingShapeSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.DocumentTestShapes.Choice;
import software.amazon.awssdk.schema.serde.document.DocumentTestShapes.Inner;
import software.amazon.awssdk.schema.serde.document.DocumentTestShapes.Outer;

/**
 * Tests for documents created from modeled shapes ({@link Document#of(SerializableShape)}), {@link DocumentParser},
 * {@link DocumentDeserializer} and {@link Document#asShape}.
 */
class TypedDocumentTest {

    private static Outer fullOuter() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("b", 2);
        scores.put("a", 1);
        return Outer.builder()
                    .name("n")
                    .count(3)
                    .total(4294967297L)
                    .tags(Arrays.asList("x", null, "y"))
                    .scores(scores)
                    .child(new Inner("v"))
                    .created(Instant.ofEpochSecond(1, 5))
                    .data(ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8)))
                    .extra(Document.of(Collections.singletonMap("k", Document.of(1))))
                    .secret("hunter2")
                    .build();
    }

    @Test
    void of_structure_reportsStructureTypeAndShapeIdDiscriminator() {
        Document document = Document.of(fullOuter());

        assertThat(document.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(document.isType(ShapeType.STRUCTURE)).isTrue();
        assertThat(document.getDiscriminator()).isEqualTo(Outer.ID);
        assertThat(document.expectDiscriminator()).isEqualTo(Outer.ID);
    }

    @Test
    void of_structure_keysMembersBySmithyNameInWrittenOrder() {
        Document document = Document.of(fullOuter());

        List<String> written = Arrays.asList("count", "name", "total", "tags", "scores", "child", "created", "data",
                                             "extra", "secret");
        assertThat(document.getMemberNames()).containsExactlyElementsOf(written);
        assertThat(document.asMap().keySet()).containsExactlyElementsOf(written);
        assertThat(document.size()).isEqualTo(written.size());
    }

    @Test
    void of_structure_withUnsetMembers_holdsOnlySetMembers() {
        Document document = Document.of(Outer.builder().name("n").build());

        assertThat(document.getMemberNames()).containsExactly("name");
        assertThat(document.getMember("count")).isNull();
        assertThat(document.getMember("notAMember")).isNull();
    }

    @Test
    void of_structure_memberDocumentsHoldTheMemberValues() {
        Document document = Document.of(fullOuter());

        assertThat(document.getMember("name")).isEqualTo(Document.of("n"));
        assertThat(document.getMember("count")).isEqualTo(Document.of(3));
        assertThat(document.getMember("total").type()).isEqualTo(ShapeType.LONG);
        assertThat(document.getMember("total").asInteger()).isEqualTo(1);
        assertThat(document.getMember("tags").asList()).containsExactly(Document.of("x"), null, Document.of("y"));
        assertThat(document.getMember("scores").getMemberNames()).containsExactly("b", "a");
        assertThat(document.getMember("scores").getMember("a")).isEqualTo(Document.of(1));
        assertThat(document.getMember("created").asTimestamp()).isEqualTo(Instant.ofEpochSecond(1, 5));
        assertThat(document.getMember("data").asBlob())
            .isEqualTo(ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8)));
        assertThat(document.getMember("extra")).isEqualTo(Document.of(Collections.singletonMap("k", Document.of(1))));
    }

    @Test
    void of_structure_nestedStructureReportsItsShapeIdNotTheMemberId() {
        Document child = Document.of(fullOuter()).getMember("child");

        assertThat(child.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(child.getDiscriminator()).isEqualTo(Inner.ID);
        assertThat(child.getMember("value")).isEqualTo(Document.of("v"));
    }

    @Test
    void of_structure_readsMembersOnlyWhenFirstNeededAndOnce() {
        Outer outer = fullOuter();
        Document document = Document.of(outer);
        assertThat(outer.serializeMembersCalls.get()).isZero();

        document.getMember("name");
        document.getMemberNames();
        document.asMap();

        assertThat(outer.serializeMembersCalls.get()).isEqualTo(1);
    }

    @Test
    void of_union_holdsSetMemberWithoutDiscriminator() {
        Document document = Document.of(Choice.number(7));

        assertThat(document.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(document.getDiscriminator()).isNull();
        assertThat(document.getMemberNames()).containsExactly("number");
        assertThat(document.getMember("number")).isEqualTo(Document.of(7));
        assertThat(document.getMember("text")).isNull();
    }

    @Test
    void of_scalarShape_returnsDocumentOfThatType() {
        SerializableShape string = s -> s.writeString(PreludeSchemas.STRING, "a");
        SerializableShape nullValue = s -> s.writeNull(PreludeSchemas.STRING);

        assertThat(Document.of(string)).isEqualTo(Document.of("a"));
        assertThat(Document.of(nullValue)).isNull();
    }

    @Test
    void of_document_copiesTheDocumentFromItsContents() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("list", Document.of(Arrays.asList(Document.of(1), null, Document.of("s"))));
        members.put("null", null);
        members.put("blob", Document.of(new byte[] {1, 2}));
        members.put("decimal", Document.of(new BigDecimal("1.5")));
        Document original = Document.of(members);

        Document copy = Document.of(original);

        assertThat(copy).isEqualTo(original).isNotSameAs(original);
        assertThat(copy.getMemberNames()).containsExactly("list", "null", "blob", "decimal");
        assertThat(Document.of(Document.of(Collections.<Document>emptyList())).asList()).isEmpty();
    }

    @Test
    void of_nullShape_throwsNullPointerException() {
        assertThatThrownBy(() -> Document.of((SerializableShape) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("shape");
    }

    @Test
    void asShape_roundTripsTheStructure() {
        Outer original = fullOuter();
        Outer.Builder builder = Outer.builder();

        Outer copy = Document.of(original).asShape(builder);

        assertThat(copy).isEqualTo(original);
        assertThat(builder.errorCorrections()).isEqualTo(1);
    }

    @Test
    void asShape_roundTripsUnions() {
        assertThat(Document.of(Choice.text("t")).asShape(new Choice.Builder())).isEqualTo(Choice.text("t"));
        assertThat(Document.of(Choice.number(1)).asShape(new Choice.Builder())).isEqualTo(Choice.number(1));
        Choice any = Choice.any(Document.of(Arrays.asList(Document.of(true))));
        assertThat(Document.of(any).asShape(new Choice.Builder())).isEqualTo(any);
    }

    @Test
    void asShape_fromMapDocument_matchesMembersByNameAndIgnoresUnknownMembers() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("name", Document.of("n"));
        members.put("unknown", Document.of(1));
        members.put("count", Document.of(5L));
        members.put("child", null);

        Outer outer = Document.of(members).asShape(Outer.builder());

        assertThat(outer).isEqualTo(Outer.builder().name("n").count(5).build());
    }

    @Test
    void asShape_onStringDocumentWithStructureBuilder_throwsSerializationException() {
        assertThatThrownBy(() -> Document.of("a").asShape(Outer.builder()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a map, structure, or union document, but found string");
    }

    @Test
    void asShape_withMistypedMember_throwsNamingBothTypes() {
        Document document = Document.of(Collections.singletonMap("count", Document.of("5")));

        assertThatThrownBy(() -> document.asShape(Outer.builder()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected an integer document, but found string");
    }

    @Test
    void serialize_structureDocument_writesOneDocument() {
        Document document = Document.of(fullOuter());
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        document.serialize(serializer);

        assertThat(serializer.calls()).hasSize(1);
        assertThat(serializer.calls().get(0).method()).isEqualTo("writeDocument");
        assertThat(serializer.calls().get(0).schema()).isSameAs(PreludeSchemas.DOCUMENT);
        assertThat(serializer.calls().get(0).value()).isSameAs(document);
    }

    @Test
    void serializeContents_structureDocument_writesTheOriginalStructure() {
        Outer outer = fullOuter();
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        Document.of(outer).serializeContents(serializer);

        assertThat(serializer.calls()).hasSize(1);
        assertThat(serializer.calls().get(0).method()).isEqualTo("writeStruct");
        assertThat(serializer.calls().get(0).schema()).isSameAs(Outer.SCHEMA);
        assertThat(serializer.calls().get(0).value()).isSameAs(outer);
    }

    @Test
    void serializeContents_nestedStructure_usesTheMemberSchema() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        Document.of(fullOuter()).getMember("child").serializeContents(serializer);

        assertThat(serializer.calls().get(0).method()).isEqualTo("writeStruct");
        assertThat(serializer.calls().get(0).schema()).isSameAs(Outer.CHILD);
    }

    @Test
    void serializeMembers_unionDocument_writesMembersWithTheirMemberSchemas() {
        Document text = Document.of(Choice.text("t"));
        RecordingShapeSerializer contents = new RecordingShapeSerializer();
        text.serializeContents(contents);
        assertThat(contents.calls().get(0).method()).isEqualTo("writeStruct");
        assertThat(contents.calls().get(0).schema()).isSameAs(Choice.SCHEMA);

        RecordingShapeSerializer members = new RecordingShapeSerializer();
        ((SerializableStruct) contents.calls().get(0).value()).serializeMembers(members);
        assertThat(members.calls()).hasSize(1);
        assertThat(members.calls().get(0).method()).isEqualTo("writeString");
        assertThat(members.calls().get(0).schema()).isSameAs(Choice.TEXT);

        Document any = Document.of(Choice.any(Document.of("d")));
        RecordingShapeSerializer anyMembers = new RecordingShapeSerializer();
        ((SerializableStruct) any).serializeMembers(anyMembers);
        assertThat(anyMembers.calls().get(0).method()).isEqualTo("writeDocument");
        assertThat(anyMembers.calls().get(0).schema()).isSameAs(Choice.ANY);
        assertThat(anyMembers.calls().get(0).value()).isEqualTo(Document.of("d"));
    }

    @Test
    void serializeContents_memberDocument_usesTheMemberSchema() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        Document.of(fullOuter()).getMember("count").serializeContents(serializer);

        assertThat(serializer.calls().get(0).method()).isEqualTo("writeInteger");
        assertThat(serializer.calls().get(0).schema()).isSameAs(Outer.COUNT);
        assertThat(serializer.calls().get(0).value()).isEqualTo(3);
    }

    @Test
    void equals_comparesStructureDocumentsByMembers() {
        Document first = Document.of(fullOuter());
        Document second = Document.of(fullOuter());

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first).isNotEqualTo(Document.of(Outer.builder().name("other").build()));
        assertThat(first).isNotEqualTo(Document.of(first.asMap()));
        assertThat(Document.of(Choice.number(1))).isEqualTo(Document.of(Choice.number(1)));
    }

    @Test
    void equals_withNumberPromotion_comparesNestedNumbersAcrossTypes() {
        Document typed = Document.of(Outer.builder().count(3).build());
        Document untyped = Document.of(Collections.singletonMap("count", Document.of(3L)));

        assertThat(Document.equals(Document.of(typed.asMap()), untyped)).isFalse();
        assertThat(Document.equals(Document.of(typed.asMap()), untyped, DocumentEqualsFlags.NUMBER_PROMOTION)).isTrue();
    }

    @Test
    void getMemberValue_onStructureDocuments() {
        Outer outer = fullOuter();
        SerializableStruct structure = (SerializableStruct) Document.of(outer);
        SerializableStruct union = (SerializableStruct) Document.of(Choice.number(9));

        assertThat(structure.sdkSchema()).isSameAs(Outer.SCHEMA);
        assertThat((Inner) structure.getMemberValue(Outer.CHILD)).isSameAs(outer.child);
        assertThat((Integer) union.getMemberValue(Choice.NUMBER)).isEqualTo(9);
        assertThat((String) union.getMemberValue(Choice.TEXT)).isNull();
        assertThatThrownBy(() -> union.getMemberValue(Outer.NAME)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toString_redactsSensitiveMembers() {
        Document document = Document.of(fullOuter());

        assertThat(document.getMember("secret").toString()).doesNotContain("hunter2").contains("Redacted");
        assertThat(document.toString()).doesNotContain("hunter2").contains("Document(string: n)");
        assertThat(Document.of(Choice.text("t")).toString()).contains("Document(string: t)");
    }

    @Test
    void parser_writeMap_withNonStringKeys_throwsSerializationException() {
        Schema intKeyed = Schema.mapBuilder(ShapeId.fromParts("com.example", "IntMap"))
                                .putMember("key", PreludeSchemas.INTEGER)
                                .putMember("value", PreludeSchemas.STRING)
                                .build();

        assertThatThrownBy(() -> new DocumentParser().writeMap(intKeyed, null, 0, (s, m) -> { }))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Unexpected map key schema");
    }

    @Test
    void parser_clearResult_returnsAndClearsTheResult() {
        DocumentParser parser = new DocumentParser();
        parser.writeString(PreludeSchemas.STRING, "a");

        assertThat(parser.clearResult()).isEqualTo(Document.of("a"));
        assertThat(parser.getResult()).isNull();
    }

    @Test
    void deserializer_readsTheDocumentWithItsCoercions() {
        DocumentDeserializer deserializer = new DocumentDeserializer(Document.of(4294967297L));

        assertThat(deserializer.readInteger(PreludeSchemas.INTEGER)).isEqualTo(1);
        assertThat(deserializer.readLong(PreludeSchemas.LONG)).isEqualTo(4294967297L);
        assertThat(deserializer.isNull()).isFalse();
        assertThat(deserializer.containerSize()).isEqualTo(-1);
        assertThat(Document.of(Arrays.asList(Document.of(1))).createDeserializer().containerSize()).isEqualTo(1);
        assertThatThrownBy(deserializer::readNull)
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("non-null long document");
    }

    @Test
    void deserializer_overNull_readsNull() {
        DocumentDeserializer deserializer = new DocumentDeserializer(null);

        assertThat(deserializer.isNull()).isTrue();
        assertThat((Object) deserializer.readNull()).isNull();
        assertThat(deserializer.readDocument()).isNull();
    }

    @Test
    void parseDiscriminator_absoluteOrRelative_resolvesTheShapeId() {
        assertThat(DocumentDeserializer.parseDiscriminator("com.example#Foo", null))
            .isEqualTo(ShapeId.from("com.example#Foo"));
        assertThat(DocumentDeserializer.parseDiscriminator("com.example#Foo", "com.other"))
            .isEqualTo(ShapeId.from("com.example#Foo"));
        assertThat(DocumentDeserializer.parseDiscriminator("Foo", "com.other"))
            .isEqualTo(ShapeId.from("com.other#Foo"));
        assertThat(DocumentDeserializer.parseDiscriminator(null, "com.other")).isNull();
    }

    @Test
    void parseDiscriminator_relativeWithoutDefaultNamespace_throwsDiscriminatorException() {
        assertThatThrownBy(() -> DocumentDeserializer.parseDiscriminator("Foo", null))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("Attempted to parse a document discriminator that only provides a shape name, but no default "
                        + "namespace was configured: Foo");
    }

    @Test
    void parseDiscriminator_invalidShapeId_throwsDiscriminatorException() {
        assertThatThrownBy(() -> DocumentDeserializer.parseDiscriminator("com.example#", null))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("Unable to parse the document discriminator into a valid shape ID: com.example#");
        assertThatThrownBy(() -> DocumentDeserializer.parseDiscriminator("not valid", "com.other"))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("Unable to parse the document discriminator into a valid shape ID: not valid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "#Foo", "com.example#Foo#Bar", "com..example#Foo", "1Foo", "com.example#Foo$"})
    void parseDiscriminator_malformedText_throwsDiscriminatorException(String text) {
        assertThatThrownBy(() -> DocumentDeserializer.parseDiscriminator(text, "com.other"))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("Unable to parse the document discriminator into a valid shape ID: " + text)
            .hasCauseInstanceOf(ShapeIdSyntaxException.class);
    }

    @Test
    void parseDiscriminator_relativeName_keepsTheNameAndTakesTheDefaultNamespace() {
        ShapeId id = DocumentDeserializer.parseDiscriminator("_Foo1", "com.example.nested");

        assertThat(id.getNamespace()).isEqualTo("com.example.nested");
        assertThat(id.getName()).isEqualTo("_Foo1");
        assertThat(id.hasMember()).isFalse();
        assertThat(id).hasToString("com.example.nested#_Foo1");
    }

    @Test
    void parseDiscriminator_memberShapeId_throwsDiscriminatorException() {
        assertThatThrownBy(() -> DocumentDeserializer.parseDiscriminator("com.example#Foo$bar", null))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("A document discriminator must name a shape, not a member: com.example#Foo$bar");
        assertThatThrownBy(() -> DocumentDeserializer.parseDiscriminator("Foo$bar", "com.other"))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("A document discriminator must name a shape, not a member: Foo$bar");
    }
}
