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

package software.amazon.awssdk.codegen.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.Exemption;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.ExemptRegistry;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.Result;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.Rule;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.Violation;

class PerShapeDocumentCodeScannerTest {
    private static final String MODEL_FILE = "software/amazon/awssdk/services/demo/model/Foo.java";

    /**
     * A model class with a {@code document} member, as the schema serde generator renders it
     * ({@code codegen/.../poet/model/schemaserde/putdocrequest.java}), plus a use of the runtime converter.
     */
    private static final String LEGACY_DOCUMENT_MEMBER =
        "package software.amazon.awssdk.services.demo.model;\n"
        + "import software.amazon.awssdk.core.document.DocumentConverter;\n"
        + "import software.amazon.awssdk.core.schema.SchemaSerdeSupport;\n"
        + "/** Unlike smithy-java codegen, has no {@code toDocument()} and no {@code FooDocumentConverter}. */\n"
        + "public final class Foo {\n"
        + "    // fromDocument is gone too.\n"
        + "    public void serializeMembers(ShapeSerializer serializer) {\n"
        + "        serializer.writeDocument(DemoSchemas.FOO_DOC, SchemaSerdeSupport.wrapLegacy(doc));\n"
        + "    }\n"
        + "    void read(ShapeDeserializer de) { doc = SchemaSerdeSupport.unwrapLegacy(de.readDocument()); }\n"
        + "    Object legacy(Object d) { return DocumentConverter.toLegacy(DocumentConverter.toSchemaDocument(d)); }\n"
        + "    Object de(Document d) { return new DocumentDeserializer(d); }\n"
        + "}\n";

    private static final String TYPE_REGISTRY_FILE = "software/amazon/awssdk/services/demo/model/DemoTypeRegistry.java";

    private static final String TYPE_REGISTRY =
        "package software.amazon.awssdk.services.demo.model;\n"
        + "public final class DemoTypeRegistry {\n"
        + "    public static final TypeRegistry INSTANCE = TypeRegistry.builder()\n"
        + "        .putType(FOO_ID, Foo.class, () -> FooDocumentDeserializer.builder())\n"
        + "        .build();\n"
        + "}\n";

    private static final String OPERATIONS_FILE = "software/amazon/awssdk/services/demo/model/DemoOperations.java";

    private static final String OPERATIONS =
        "package software.amazon.awssdk.services.demo.model;\n"
        + "public final class DemoOperations {\n"
        + "    public static final ErrorRegistry ERROR_REGISTRY = ErrorRegistry.builder()\n"
        + "        .putLazyType(NOT_FOUND, () -> DemoSchemas.NOT_FOUND)\n"
        + "        .putLazyType(BAD, () -> BadDocumentConverter.SCHEMA)\n"
        + "        .build();\n"
        + "}\n";

    @TempDir
    Path tmp;

    @Test
    void scan_withLegacyDocumentHandling_reportsNothing() {
        Result result = PerShapeDocumentCodeScanner.scan(MODEL_FILE, LEGACY_DOCUMENT_MEMBER);

        assertThat(result.violations()).isEmpty();
        assertThat(result.exemptions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "public Document toDocument() { return Document.of(this); }",
        "public static Foo fromDocument(Document d) { return d.asShape(builder()); }",
        "Function<Foo, Document> f = Foo::toDocument;",
        "private static final Function<Document, Foo> fromDocument = null;"
    })
    void scan_withDocumentMethodOrField_reportsDocumentMethod(String member) {
        List<Violation> violations = violations(MODEL_FILE, "public final class Foo {\n    " + member + "\n}\n");

        assertThat(violations).extracting(Violation::rule).containsExactly(Rule.DOCUMENT_METHOD);
        assertThat(violations.get(0).match()).matches("(to|from)Document");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "final class FooDocumentConverter {}",
        "public final class FooDocumentSerializer implements ShapeSerializer {}",
        "class FooDocumentDeserializer {}",
        "final class DocumentConverter {}",
        "interface DocumentSerializer {}",
        "class Foo { Object s = new FooDocumentSerializer(); }",
        "class Foo { Object c = demo.internal.BarDocumentConverter.INSTANCE; }"
    })
    void scan_withPerShapeConverterType_reportsConversionType(String source) {
        assertThat(rules(MODEL_FILE, source)).containsExactly(Rule.DOCUMENT_CONVERSION_TYPE);
    }

    @Test
    void scan_withEveryRule_reportsEachRuleOncePerFile() {
        List<Violation> violations = violations(
            MODEL_FILE,
            "class Foo { Document toDocument() { return FooDocumentSerializer.toDocument(this); }\n"
            + "static Foo fromDocument(Document d) { return new FooDocumentConverter().fromDocument(d); } }");

        assertThat(violations).extracting(Violation::rule)
                              .containsExactly(Rule.DOCUMENT_METHOD, Rule.DOCUMENT_CONVERSION_TYPE);
        assertThat(violations).extracting(Violation::match)
                              .containsExactly("toDocument", "FooDocumentSerializer");
    }

    @Test
    void scan_withExemptRegistryDeclarations_skipsOnlyTheDeclarations() {
        Result typeRegistry = PerShapeDocumentCodeScanner.scan(TYPE_REGISTRY_FILE, TYPE_REGISTRY);
        Result operations = PerShapeDocumentCodeScanner.scan(OPERATIONS_FILE, OPERATIONS);

        assertThat(typeRegistry.violations()).isEmpty();
        assertThat(typeRegistry.exemptions()).extracting(Exemption::registry)
                                             .containsExactly(ExemptRegistry.PRIMARY_TYPE_REGISTRY);
        assertThat(operations.violations()).isEmpty();
        assertThat(operations.exemptions()).extracting(Exemption::registry)
                                           .containsExactly(ExemptRegistry.ERROR_REGISTRY);
    }

    @Test
    void scan_withConverterOutsideExemptDeclaration_reportsIt() {
        String operations = OPERATIONS.replace("}\n", "    static Document toDocument(Object o) { return null; }\n}\n");

        Result result = PerShapeDocumentCodeScanner.scan(OPERATIONS_FILE, operations);

        assertThat(result.exemptions()).hasSize(1);
        assertThat(result.violations()).extracting(Violation::rule).containsExactly(Rule.DOCUMENT_METHOD);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "software/amazon/awssdk/services/demo/model/Foo.java",
        "software/amazon/awssdk/services/demo/model/TypeRegistry.java",
        "software/amazon/awssdk/services/demo/model/Operations.java"
    })
    void scan_withRegistryDeclarationInOtherFile_reportsIt(String file) {
        assertThat(rules(file, TYPE_REGISTRY + OPERATIONS)).containsExactly(Rule.DOCUMENT_CONVERSION_TYPE);
    }

    @Test
    void scan_withTempDirectory_reportsViolatingFileByRelativePath() throws IOException {
        Path src = tmp.resolve("src");
        write(src, MODEL_FILE, LEGACY_DOCUMENT_MEMBER);
        write(src, TYPE_REGISTRY_FILE, TYPE_REGISTRY);
        write(src, "software/amazon/awssdk/services/demo/model/Bar.java",
              "public final class Bar { public Document toDocument() { return Document.of(this); } }\n");

        Result result = PerShapeDocumentCodeScanner.scan(src, tmp.resolve("missing"));

        assertThat(result.violations()).extracting(Violation::toString).containsExactly(
            "software/amazon/awssdk/services/demo/model/Bar.java: DOCUMENT_METHOD: toDocument");
        assertThat(result.exemptions()).extracting(Exemption::file).containsExactly(TYPE_REGISTRY_FILE);
    }

    private static List<Violation> violations(String file, String source) {
        return PerShapeDocumentCodeScanner.scan(file, source).violations();
    }

    private static List<Rule> rules(String file, String source) {
        return violations(file, source).stream().map(Violation::rule).collect(Collectors.toList());
    }

    private static void write(Path root, String file, String content) throws IOException {
        Path path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
