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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Runs every case in the {@code serdeTests} array of the Document Type and Type Registries SEP's
 * {@code new-document-type-test-cases.json} against {@code new-document-type-test-model.json} (Requirement 20.5). Both
 * files are copied unchanged from the SEP repository (see {@code docs/design/schema-serde/porting-deviations.md}, task
 * 11.12).
 *
 * <p>Each case is one container with one test per SEP assertion. The setup follows the SEP's "Test Cases" section:
 * <ol>
 *     <li>{@code canonicalSerialization}: the case's {@code serialized} JSON as a document, written by the case's codec
 *     with the prelude document schema.</li>
 *     <li>{@code canonicalDataObject}: {@code canonicalSerialization} read with the subject's schema.</li>
 *     <li>{@code documentFromSerialization}: {@code canonicalSerialization} read as a document.</li>
 *     <li>{@code documentFromDataObject}: {@code Document.of(canonicalDataObject)}.</li>
 * </ol>
 *
 * <p>The data object is a {@link SchemaStruct}, which stands in for a generated class, and the subject schemas come
 * from {@link SmithyJsonAstSchemas}, which stands in for the Schema_Converter. Serializations are compared as text.
 *
 * <p>Assertion 6 has one difference from the SEP text. A document created from a structure data object has a
 * discriminator, and the JSON codec writes it as a leading {@code __type} member when
 * {@link JsonSettings#serializeTypeInDocuments()} is on (the default, and every protocol preset; Requirement 20.2). The
 * case settings do not name this setting. The test asserts that the output is {@code canonicalSerialization} with the
 * subject's ShapeId as {@code __type} when the setting is on, and exactly {@code canonicalSerialization} when it is
 * off.
 */
class DocumentSerdeTestSuite {
    private static final String MODEL_RESOURCE = "/new-document-type-test-model.json";
    private static final String CASES_RESOURCE = "/new-document-type-test-cases.json";
    private static final String TYPE_MEMBER = "__type";

    /** Reads the fixture files with the default settings. */
    private static final JsonCodec PLAIN_JSON = JsonCodec.builder().build();

    @TestFactory
    Stream<DynamicContainer> serdeTests() {
        Map<ShapeId, Schema> schemas = SmithyJsonAstSchemas.convert(readResource(MODEL_RESOURCE));
        List<Document> cases = readResource(CASES_RESOURCE).getMember("serdeTests").asList();
        assertThat(cases).as("serdeTests in %s", CASES_RESOURCE).isNotEmpty();
        return cases.stream().map(testCase -> new SerdeCase(testCase, schemas).tests());
    }

    private static Document readResource(String name) {
        try (InputStream in = DocumentSerdeTestSuite.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException(
                    "Test resource " + name + " is missing. Copy it unchanged from the Document Type and Type "
                    + "Registries SEP (seps/accepted/shared/schema-serde in the AwsDrSeps repository) into "
                    + "core/codecs/json-codec/src/test/resources.");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, read);
            }
            return PLAIN_JSON.createDeserializer(bytes.toByteArray()).readDocument();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read test resource " + name, e);
        }
    }

    private static String write(JsonCodec codec, Consumer<ShapeSerializer> writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = codec.createSerializer(out)) {
            writer.accept(serializer);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static JsonCodec.Builder codecBuilder(String caseName, Document testCase) {
        String codec = testCase.getMember("codec").asString();
        if (!"json".equals(codec)) {
            throw new IllegalArgumentException("Case \"" + caseName + "\" uses unsupported codec " + codec);
        }
        JsonCodec.Builder builder = JsonCodec.builder();
        Document settings = testCase.getMember("settings");
        if (settings == null) {
            return builder;
        }
        for (Map.Entry<String, Document> setting : settings.asMap().entrySet()) {
            Document value = setting.getValue();
            switch (setting.getKey()) {
                case "defaultNamespace":
                    builder.defaultNamespace(value.asString());
                    break;
                case "jsonName":
                    builder.useJsonName(value.asBoolean());
                    break;
                case "timestampFormat":
                    timestampSettings(caseName, builder, value);
                    break;
                default:
                    throw new IllegalArgumentException("Case \"" + caseName + "\" uses unsupported setting "
                                                       + setting.getKey());
            }
        }
        return builder;
    }

    private static void timestampSettings(String caseName, JsonCodec.Builder builder, Document timestampFormat) {
        for (Map.Entry<String, Document> setting : timestampFormat.asMap().entrySet()) {
            switch (setting.getKey()) {
                case "useTrait":
                    builder.useTimestampFormat(setting.getValue().asBoolean());
                    break;
                case "default":
                    builder.defaultTimestampFormat(TimestampFormatTrait.Format.fromString(
                        setting.getValue().asString()));
                    break;
                default:
                    throw new IllegalArgumentException("Case \"" + caseName + "\" uses unsupported timestampFormat "
                                                       + "setting " + setting.getKey());
            }
        }
    }

    /** One SEP test case and its four representations, created on first use so a setup failure fails each test. */
    private static final class SerdeCase {
        private final String name;
        private final Document testCase;
        private final Map<ShapeId, Schema> schemas;

        private Schema subject;
        private JsonCodec codec;
        private String canonicalSerialization;
        private SchemaStruct.Builder canonicalDataObjectBuilder;
        private SchemaStruct canonicalDataObject;
        private Document documentFromSerialization;
        private Document documentFromDataObject;

        SerdeCase(Document testCase, Map<ShapeId, Schema> schemas) {
            this.name = testCase.getMember("name").asString();
            this.testCase = testCase;
            this.schemas = schemas;
        }

        DynamicContainer tests() {
            return DynamicContainer.dynamicContainer(name, Stream.of(
                DynamicTest.dynamicTest("1. data object from canonicalSerialization", this::dataObjectFromSerialization),
                DynamicTest.dynamicTest("2. data object from documentFromDataObject",
                                        this::dataObjectFromDocumentFromDataObject),
                DynamicTest.dynamicTest("3. data object from documentFromSerialization",
                                        this::dataObjectFromDocumentFromSerialization),
                DynamicTest.dynamicTest("4. serialization of canonicalDataObject",
                                        this::serializationOfDataObject),
                DynamicTest.dynamicTest("5. serialization of documentFromSerialization with writeDocument",
                                        this::serializationOfDocumentFromSerialization),
                DynamicTest.dynamicTest("6. serialization of documentFromDataObject",
                                        this::serializationOfDocumentFromDataObject)));
        }

        private void setUp() {
            if (documentFromDataObject != null) {
                return;
            }
            ShapeId subjectId = ShapeId.from(testCase.getMember("subject").asString());
            subject = schemas.get(subjectId);
            assertThat(subject).as("subject %s in the test model", subjectId).isNotNull();
            codec = codecBuilder(name, testCase).build();

            Document serialized = testCase.getMember("serialized");
            canonicalSerialization = write(codec, s -> s.writeDocument(PreludeSchemas.DOCUMENT, serialized));
            canonicalDataObjectBuilder = SchemaStruct.builder(subject).deserialize(
                codec.createDeserializer(canonicalSerialization.getBytes(StandardCharsets.UTF_8)));
            canonicalDataObject = canonicalDataObjectBuilder.build();
            documentFromSerialization =
                codec.createDeserializer(canonicalSerialization.getBytes(StandardCharsets.UTF_8)).readDocument();
            documentFromDataObject = Document.of(canonicalDataObject);
        }

        private void dataObjectFromSerialization() {
            setUp();
            // The SEP needs no assertion beyond a successful setup. Every serialized member must map to a member of
            // the subject, or the remaining assertions would pass on a data object that lost data.
            assertThat(canonicalDataObjectBuilder.unknownMembers())
                .as("members of %s that %s does not have", canonicalSerialization, subject.id())
                .isEmpty();
        }

        private void dataObjectFromDocumentFromDataObject() {
            setUp();
            assertThat(documentFromDataObject.asShape(SchemaStruct.builder(subject))).isEqualTo(canonicalDataObject);
        }

        private void dataObjectFromDocumentFromSerialization() {
            setUp();
            assertThat(documentFromSerialization.asShape(SchemaStruct.builder(subject)))
                .isEqualTo(canonicalDataObject);
        }

        private void serializationOfDataObject() {
            setUp();
            assertThat(write(codec, canonicalDataObject::serialize)).isEqualTo(canonicalSerialization);
        }

        private void serializationOfDocumentFromSerialization() {
            setUp();
            assertThat(write(codec, s -> s.writeDocument(PreludeSchemas.DOCUMENT, documentFromSerialization)))
                .isEqualTo(canonicalSerialization);
        }

        private void serializationOfDocumentFromDataObject() {
            setUp();
            assertThat(codec.settings().serializeTypeInDocuments()).isTrue();
            assertThat(canonicalSerialization).startsWith("{");
            String type = "{\"" + TYPE_MEMBER + "\":\"" + subject.id() + "\"";
            String expectedWithType = "{}".equals(canonicalSerialization)
                                      ? type + "}"
                                      : type + "," + canonicalSerialization.substring(1);
            assertThat(write(codec, s -> s.writeDocument(PreludeSchemas.DOCUMENT, documentFromDataObject)))
                .as("with serializeTypeInDocuments on")
                .isEqualTo(expectedWithType);

            JsonCodec withoutTypes = codecBuilder(name, testCase).serializeTypeInDocuments(false).build();
            assertThat(write(withoutTypes, s -> s.writeDocument(PreludeSchemas.DOCUMENT, documentFromDataObject)))
                .as("with serializeTypeInDocuments off")
                .isEqualTo(canonicalSerialization);
        }
    }
}
