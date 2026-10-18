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

package software.amazon.awssdk.codegen.poet.model;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeTranscoder;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.smithy.model.Model;

/**
 * Document members on the schema path (task 7.17): classes with {@code document} members, lists of documents and maps
 * of documents get schema serde members, and their operations route to the new path. Accessors keep returning
 * Legacy_Documents; generated code writes them through {@code SchemaSerdeSupport.wrapLegacy} and reads them through
 * {@code SchemaSerdeSupport.unwrapLegacy}.
 */
class SchemaSerdeDocumentMemberTest {
    private static final String PACKAGE = "software.amazon.awssdk.services.docs.model";
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace docs\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#awsJson1_1\n"
        + "@service(sdkId: \"Docs\")\n"
        + "@sigv4(name: \"docs\")\n"
        + "@awsJson1_1\n"
        + "service DocsService { version: \"2024-01-01\", operations: [PutDoc] }\n"
        + "operation PutDoc { input: PutDocRequest, output: PutDocResponse }\n"
        + "structure PutDocRequest { Doc: Document, Docs: DocumentList, DocMap: DocumentMap }\n"
        + "structure PutDocResponse { Doc: Document }\n"
        + "list DocumentList { member: Document }\n"
        + "map DocumentMap { key: String, value: Document }\n";

    private static IntermediateModel model;
    private static SchemaCodegen codegen;

    @TempDir
    Path dir;

    @BeforeAll
    static void generate() {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("docs.smithy", MODEL)
                            .assemble()
                            .unwrap();
        model = new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                               .model(smithy)
                                                               .customizationConfig(CustomizationConfig.create())
                                                               .build())
            .build();
        codegen = SchemaCodegen.create(model, TraitInitializers.load(SchemaSerdeDocumentMemberTest.class.getClassLoader()));
    }

    @Test
    void documentMemberClasses_haveSchemaSerdeMembersAndRouteToTheNewPath() {
        assertThat(codegen.serde().entries().keySet()).containsExactlyInAnyOrder("PutDocRequest", "PutDocResponse");
        assertThat(codegen.serde().operation(software.amazon.smithy.model.shapes.ShapeId.from("docs#PutDoc")))
            .isNotNull();
    }

    @Test
    void request_matchesGoldenFile() {
        MatcherAssert.assertThat(ModelPackageCompiler.modelClass(model, codegen, "PutDocRequest"),
                                 generatesTo("schemaserde/putdocrequest.java"));
    }

    @Test
    void sharedSerde_matchesGoldenFile() {
        ClassSpec sharedSerde = codegen.classSpecs().stream()
                                       .filter(s -> s.className().simpleName().equals("DocsSharedSerde"))
                                       .findFirst().orElseThrow(() -> new AssertionError("DocsSharedSerde"));
        MatcherAssert.assertThat(sharedSerde, generatesTo("schemaserde/docssharedserde.java"));
    }

    @Test
    void generatedModelPackage_writesAndReadsLegacyDocuments() throws Exception {
        try (URLClassLoader loader = RuntimeCompiler.classLoader(ModelPackageCompiler.compile(model, codegen, dir))) {
            Class<?> requestClass = loader.loadClass(PACKAGE + ".PutDocRequest");
            Schema schema = (Schema) requestClass.getField("SCHEMA").get(null);
            Document doc = Document.mapBuilder().putNumber("n", "1.50").putList("l", l -> l.addNull()).build();
            Method builderMethod = requestClass.getMethod("builder");
            Class<?> builderType = builderMethod.getReturnType();
            Object builder = builderMethod.invoke(null);
            builderType.getMethod("doc", Document.class).invoke(builder, doc);
            builderType.getMethod("docs", Collection.class)
                       .invoke(builder, Arrays.asList(Document.fromString("s"), Document.fromNull()));
            builderType.getMethod("docMap", Map.class)
                       .invoke(builder, Collections.singletonMap("k", Document.fromBoolean(true)));
            SerializableStruct request = (SerializableStruct) ((SdkBuilder<?, ?>) builder).build();

            List<String> written = new ArrayList<>();
            request.serializeMembers(new SpecificShapeSerializer() {
                @Override
                public void writeDocument(Schema member, software.amazon.awssdk.schema.serde.document.Document value) {
                    written.add(member.memberName() + "=" + value.type() + ":" + value.getMember("n").asBigDecimal());
                }

                @Override
                public <T> void writeList(Schema member, T state, int size,
                                          java.util.function.BiConsumer<T, software.amazon.awssdk.schema.serde
                                              .ShapeSerializer> consumer) {
                    written.add(member.memberName() + "[" + size + "]");
                }

                @Override
                public <T> void writeMap(Schema member, T state, int size,
                                         java.util.function.BiConsumer<T, software.amazon.awssdk.schema.serde
                                             .MapSerializer> consumer) {
                    written.add(member.memberName() + "{" + size + "}");
                }
            });
            assertThat(written).containsExactly("Doc=map:1.5", "Docs[2]", "DocMap{1}");

            Object copy = ShapeTranscoder.convert(request, schema.shapeBuilder());
            assertThat(copy).isEqualTo(request);
            assertThat(requestClass.getMethod("doc").invoke(copy)).isEqualTo(doc);
            assertThat(copy.toString()).isEqualTo(request.toString());
            assertThat(request.toString())
                .isEqualTo("PutDocRequest(Doc={\"n\": 1.50,\"l\": [null]}, Docs=[\"s\", null], DocMap={k=true})");
            assertThat(schema.member("Doc").memberTarget().id()).isEqualTo(ShapeId.from("smithy.api#Document"));
        }
    }
}
