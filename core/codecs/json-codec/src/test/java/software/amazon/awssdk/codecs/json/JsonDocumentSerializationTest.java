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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Tests of writing documents with the JSON codec: the {@code __type} member of structure documents, the writer's
 * settings for typed structure members, and JSON Protocol_Documents written with other settings than they were read
 * with.
 */
class JsonDocumentSerializationTest {
    private static final Instant ONE_SECOND = Instant.ofEpochSecond(1);

    private static final Schema INNER = Schema.structureBuilder(ShapeId.from("test#Inner"))
                                              .putMember("value", PreludeSchemas.STRING, new JsonNameTrait("VALUE"))
                                              .build();
    private static final Schema CHOICE = Schema.unionBuilder(ShapeId.from("test#Choice"))
                                               .putMember("text", PreludeSchemas.STRING, new JsonNameTrait("TEXT"))
                                               .build();
    private static final Schema PAYLOAD =
        Schema.structureBuilder(ShapeId.from("test#Payload"))
              .putMember("name", PreludeSchemas.STRING, new JsonNameTrait("Name"))
              .putMember("created", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
              .putMember("updated", PreludeSchemas.TIMESTAMP)
              .putMember("inner", INNER)
              .putMember("document", PreludeSchemas.DOCUMENT)
              .build();

    private static final JsonCodec REST_JSON = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();
    private static final JsonCodec AWS_JSON = JsonCodec.builder().settings(JsonSettings.AWS_JSON).build();

    private static SchemaStruct inner(String value) {
        return SchemaStruct.builder(INNER).set("value", value).build();
    }

    private static SchemaStruct payload(Object document) {
        return SchemaStruct.builder(PAYLOAD)
                           .set("name", "n")
                           .set("created", ONE_SECOND)
                           .set("updated", ONE_SECOND)
                           .set("inner", inner("v"))
                           .set("document", document)
                           .build();
    }

    private static String write(JsonCodec codec, Document document) {
        return utf8(codec.serialize(document));
    }

    private static Document read(JsonCodec codec, String json) {
        return codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8)).readDocument();
    }

    private static String utf8(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static Stream<Arguments> typedStructureCases() {
        return Stream.of(
            Arguments.of(JsonSettings.REST_JSON_1,
                         "{\"__type\":\"test#Payload\",\"Name\":\"n\",\"created\":\"1970-01-01T00:00:01Z\","
                         + "\"updated\":1,\"inner\":{\"VALUE\":\"v\"}}"),
            Arguments.of(JsonSettings.AWS_JSON,
                         "{\"__type\":\"test#Payload\",\"name\":\"n\",\"created\":\"1970-01-01T00:00:01Z\","
                         + "\"updated\":1,\"inner\":{\"value\":\"v\"}}"),
            Arguments.of(JsonSettings.builder()
                                     .useTimestampFormat(false)
                                     .defaultTimestampFormat(TimestampFormatTrait.Format.HTTP_DATE)
                                     .serializeTypeInDocuments(false)
                                     .build(),
                         "{\"name\":\"n\",\"created\":\"Thu, 01 Jan 1970 00:00:01 GMT\","
                         + "\"updated\":\"Thu, 01 Jan 1970 00:00:01 GMT\",\"inner\":{\"value\":\"v\"}}"));
    }

    @ParameterizedTest
    @MethodSource("typedStructureCases")
    void writeDocument_typedStructure_writesTypeFirstAndMembersWithTheWriterSettings(JsonSettings settings,
                                                                                    String expected) {
        JsonCodec codec = JsonCodec.builder().settings(settings).build();

        assertThat(write(codec, Document.of(payload(null)))).isEqualTo(expected);
    }

    @Test
    void writeDocument_typedTimestampMember_usesTheMembersFormatWhenTheWriterUsesTimestampFormats() {
        Document created = Document.of(payload(null)).getMember("created");
        JsonCodec noFormats = JsonCodec.builder().useTimestampFormat(false).build();

        assertThat(write(REST_JSON, created)).isEqualTo("\"1970-01-01T00:00:01Z\"");
        assertThat(write(noFormats, created)).isEqualTo("1");
    }

    @Test
    void writeDocument_withoutSerializeTypeInDocuments_writesNoType() {
        JsonCodec codec = JsonCodec.builder()
                                    .settings(JsonSettings.REST_JSON_1)
                                    .serializeTypeInDocuments(false)
                                    .build();

        assertThat(write(codec, Document.of(inner("v")))).isEqualTo("{\"VALUE\":\"v\"}");
    }

    @Test
    void writeDocument_typedUnion_writesNoTypeBecauseUnionDocumentsHaveNoDiscriminator() {
        Document document = Document.of(SchemaStruct.builder(CHOICE).set("text", "t").build());

        assertThat(document.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(document.getDiscriminator()).isNull();
        assertThat(write(REST_JSON, document)).isEqualTo("{\"TEXT\":\"t\"}");
    }

    @Test
    void writeDocument_documentNestedInStructureOrList_writesTheNestedDocumentsType() {
        Document nested = Document.of(listOf(Document.of(inner("a")), Document.of("s"), Document.of(inner("b"))));

        assertThat(write(AWS_JSON, Document.of(payload(nested))))
            .isEqualTo("{\"__type\":\"test#Payload\",\"name\":\"n\",\"created\":\"1970-01-01T00:00:01Z\",\"updated\":1,"
                       + "\"inner\":{\"value\":\"v\"},\"document\":[{\"__type\":\"test#Inner\",\"value\":\"a\"},\"s\","
                       + "{\"__type\":\"test#Inner\",\"value\":\"b\"}]}");
    }

    @Test
    void writeDocument_typedStructureReadBack_isAProtocolDocumentWithTheSameDiscriminatorAndBytes() {
        SchemaStruct original = payload(Document.of(inner("d")));
        String json = write(REST_JSON, Document.of(original));

        Document read = read(REST_JSON, json);

        assertThat(read.type()).isEqualTo(ShapeType.MAP);
        assertThat(read.getDiscriminator()).isEqualTo(ShapeId.from("test#Payload"));
        assertThat(write(REST_JSON, read)).isEqualTo(json);
        SchemaStruct shape = read.asShape(SchemaStruct.builder(PAYLOAD));
        assertThat(shape.get("name")).isEqualTo("n");
        assertThat(shape.get("created")).isEqualTo(ONE_SECOND);
        assertThat(shape.get("inner")).isEqualTo(inner("v"));
        assertThat(((Document) shape.get("document")).getDiscriminator()).isEqualTo(ShapeId.from("test#Inner"));
    }

    @Test
    void writeDocument_protocolDocumentWithOtherSettings_writesItsValuesAsRead() {
        // Read with awsJson settings; written with restJson settings. The map keeps its keys (no jsonName applies),
        // its numbers and strings, and its relative __type, as in smithy-java.
        JsonCodec reader = JsonCodec.builder().settings(JsonSettings.AWS_JSON).defaultNamespace("com.example").build();
        String json = "{\"__type\":\"Payload\",\"name\":\"n\",\"Name\":\"x\",\"created\":1,"
                      + "\"updated\":\"1970-01-01T00:00:01Z\"}";
        Document document = read(reader, json);
        JsonCodec noType = JsonCodec.builder()
                                     .settings(JsonSettings.REST_JSON_1)
                                     .serializeTypeInDocuments(false)
                                     .build();

        assertThat(document.getDiscriminator()).isEqualTo(ShapeId.from("com.example#Payload"));
        assertThat(write(REST_JSON, document)).isEqualTo(json);
        assertThat(write(noType, document)).isEqualTo(json);
    }

    @Test
    void writeDocument_protocolDocumentConvertedToShape_writesTheWriterSettings() {
        // A document read with awsJson settings and converted to a typed structure is written with restJson names
        // and an absolute __type, whatever its relative __type was.
        JsonCodec reader = JsonCodec.builder().settings(JsonSettings.AWS_JSON).defaultNamespace("test").build();
        Document read = read(reader, "{\"__type\":\"Payload\",\"name\":\"n\",\"created\":\"1970-01-01T00:00:01Z\","
                                     + "\"updated\":1,\"inner\":{\"value\":\"v\"}}");
        SchemaStruct shape = read.asShape(SchemaStruct.builder(PAYLOAD));

        assertThat(Document.of(shape).getDiscriminator()).isEqualTo(read.getDiscriminator());
        assertThat(write(REST_JSON, Document.of(shape)))
            .isEqualTo("{\"__type\":\"test#Payload\",\"Name\":\"n\",\"created\":\"1970-01-01T00:00:01Z\",\"updated\":1,"
                       + "\"inner\":{\"VALUE\":\"v\"}}");
    }

    @Test
    void writeDocument_protocolDocumentWithInvalidType_isWrittenWithoutReadingTheDiscriminator() {
        Document document = read(REST_JSON, "{\"__type\":\"Foo\",\"a\":1}");

        assertThatThrownBy(document::getDiscriminator).isInstanceOf(DiscriminatorException.class);
        assertThat(write(REST_JSON, document)).isEqualTo("{\"__type\":\"Foo\",\"a\":1}");
    }

    @Test
    void writeDocument_structureDocumentThatDoesNotWriteAStructure_throwsSerializationException() {
        Document document = new StructureDocument(ShapeId.from("test#Inner")) {
            @Override
            public void serializeContents(ShapeSerializer serializer) {
                serializer.writeString(PreludeSchemas.STRING, "nope");
            }
        };

        assertThatThrownBy(() -> write(REST_JSON, document))
            .isInstanceOf(SerializationException.class)
            .hasMessage("A structure document must write a structure, but wrote a value of type string with schema "
                        + "smithy.api#String");
    }

    @Test
    void writeDocument_discriminatorFails_throwsOnlyWhenTheTypeIsWritten() {
        Document document = new StructureDocument(null) {
            @Override
            public ShapeId getDiscriminator() {
                throw new DiscriminatorException("bad discriminator");
            }
        };
        JsonCodec noType = JsonCodec.builder().settings(JsonSettings.AWS_JSON).serializeTypeInDocuments(false).build();

        assertThatThrownBy(() -> write(REST_JSON, document))
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("bad discriminator");
        assertThat(write(noType, document)).isEqualTo("{\"value\":\"v\"}");
    }

    /**
     * A custom structure document over {@code inner("v")}, with a fixed discriminator.
     */
    private static class StructureDocument implements Document {
        private final ShapeId discriminator;

        StructureDocument(ShapeId discriminator) {
            this.discriminator = discriminator;
        }

        @Override
        public ShapeType type() {
            return ShapeType.STRUCTURE;
        }

        @Override
        public ShapeId getDiscriminator() {
            return discriminator;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeStruct(INNER, inner("v"));
        }
    }
}
