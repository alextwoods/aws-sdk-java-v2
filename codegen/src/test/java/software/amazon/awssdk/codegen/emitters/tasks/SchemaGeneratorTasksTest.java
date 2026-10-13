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

package software.amazon.awssdk.codegen.emitters.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.SchemaTraitsConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.smithy.model.Model;

/**
 * Wiring of the Schema_Generator into the generator task tree: schema classes and the {@code ServiceLoader} entry are
 * generated for an intermediate model built from Smithy, nothing for one built from C2J, and an invalid configuration
 * fails when the task tree is created.
 */
class SchemaGeneratorTasksTest {
    private static final String SERVICE_FILE = "META-INF/services/software.amazon.awssdk.schema.SchemaIndex";
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@restJson1\n"
        + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
        + "@http(method: \"POST\", uri: \"/op\")\n"
        + "operation Op { input: OpRequest, output: OpResponse }\n"
        + "structure OpRequest { Name: String }\n"
        + "structure OpResponse { Items: Items }\n"
        + "list Items { member: String }\n";

    @TempDir
    Path dir;

    @Test
    void smithyModel_generatesSchemaClassesAndServiceEntry() throws IOException {
        IntermediateModel model = build(CustomizationConfig.create());
        Path resources = dir.resolve("resources");
        Files.createDirectories(resources.resolve(SERVICE_FILE).getParent());
        Files.write(resources.resolve(SERVICE_FILE), "other.Index\n".getBytes(StandardCharsets.UTF_8));

        new SchemaGeneratorTasks(params(model)).invoke();

        Path modelDir = dir.resolve("src/software/amazon/awssdk/services/demo/model");
        assertThat(modelDir.resolve("DemoSchemas.java")).exists();
        assertThat(modelDir.resolve("DemoSchemaIndex.java")).exists();
        assertThat(new String(Files.readAllBytes(modelDir.resolve("DemoOperations.java")), StandardCharsets.UTF_8))
            .contains("public static final ErrorRegistry ERROR_REGISTRY",
                      "public static final ApiOperation<SerializableStruct, SerializableStruct> OP");
        assertThat(dir.resolve("src/software/amazon/awssdk/services/demo/internal/DemoServiceSchemas.java")).exists();
        assertThat(new String(Files.readAllBytes(resources.resolve(SERVICE_FILE)), StandardCharsets.UTF_8))
            .isEqualTo("other.Index\nsoftware.amazon.awssdk.services.demo.model.DemoSchemaIndex\n");
        assertThat(new String(Files.readAllBytes(modelDir.resolve("DemoSchemas.java")), StandardCharsets.UTF_8))
            .contains("static final Schema ITEMS", "Schema.listBuilder(ShapeId.fromParts(NS, \"Items\"))");
    }

    @Test
    void c2jModel_hasNoTasks() {
        // An intermediate model with no Smithy model, as the legacy C2J route builds.
        IntermediateModel legacy = new IntermediateModel();
        legacy.setMetadata(build(CustomizationConfig.create()).getMetadata());
        legacy.setCustomizationConfig(CustomizationConfig.create());
        SchemaGeneratorTasks tasks = new SchemaGeneratorTasks(params(legacy));
        assertThat(tasks.hasTasks()).isFalse();
    }

    @Test
    void invalidSchemaTraits_failWhenTheTaskTreeIsCreated() {
        SchemaTraitsConfig schemaTraits = new SchemaTraitsConfig();
        schemaTraits.setRemove(Collections.singletonList("smithy.api#required"));
        CustomizationConfig config = CustomizationConfig.create();
        config.setSchemaTraits(schemaTraits);
        IntermediateModel model = build(config);

        assertThatThrownBy(() -> new SchemaGeneratorTasks(params(model)))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#required");
        assertThat(dir.resolve("src")).doesNotExist();
    }

    private GeneratorTaskParams params(IntermediateModel model) {
        return GeneratorTaskParams.create(model, dir.resolve("src").toString(), dir.resolve("tests").toString(),
                                          dir.resolve("resources").toString());
    }

    private static IntermediateModel build(CustomizationConfig config) {
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", MODEL)
                           .assemble()
                           .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(model)
                                                              .customizationConfig(config)
                                                              .build())
            .build();
    }
}
