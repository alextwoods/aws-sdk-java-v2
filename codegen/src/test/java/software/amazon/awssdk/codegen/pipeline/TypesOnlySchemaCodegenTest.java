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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.shapes.ShapeId;

class TypesOnlySchemaCodegenTest {
    private static final String MODEL = "$version: \"2.0\"\n"
                                        + "namespace example.events\n"
                                        + "service Events { version: \"1\" }\n"
                                        + "structure Event { name: String, detail: Detail }\n"
                                        + "structure Detail { size: Long }\n"
                                        + "structure Unrelated { value: String }\n";

    @TempDir
    Path dir;

    @Test
    void generate_rootShape_writesOnlySchemasIndexAndServiceEntry() throws IOException {
        Path model = write("events.smithy", MODEL);

        builder(model).build().generate();

        Path packageDir = dir.resolve("sources/example/internal");
        assertThat(list(packageDir)).containsExactly("EventsSchemaIndex.java", "EventsSchemas.java");
        String schemas = new String(Files.readAllBytes(packageDir.resolve("EventsSchemas.java")), StandardCharsets.UTF_8);
        assertThat(schemas).contains("\"Event\"", "\"Detail\"").doesNotContain("Unrelated");
        Path serviceFile = dir.resolve("resources/META-INF/services/software.amazon.awssdk.schema.SchemaIndex");
        assertThat(Files.readAllLines(serviceFile)).containsExactly("example.internal.EventsSchemaIndex");
    }

    @Test
    void generate_invalidModel_failsWithoutWriting() throws IOException {
        Path model = write("events.smithy", MODEL + "structure Broken { value: Missing }\n");

        assertThatThrownBy(() -> builder(model).build().generate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ERROR");
        assertThat(Files.exists(dir.resolve("sources"))).isFalse();
    }

    @Test
    void generate_notAService_fails() throws IOException {
        Path model = write("events.smithy", MODEL);

        assertThatThrownBy(() -> builder(model).service(ShapeId.from("example.events#Event")).build().generate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("example.events#Event");
    }

    private TypesOnlySchemaCodegen.Builder builder(Path model) {
        return TypesOnlySchemaCodegen.builder()
                                     .smithyModelSources(Collections.singletonList(model))
                                     .service(ShapeId.from("example.events#Events"))
                                     .rootShapes(Collections.singletonList(ShapeId.from("example.events#Event")))
                                     .packageName("example.internal")
                                     .classPrefix("Events")
                                     .sourcesDirectory(dir.resolve("sources"))
                                     .resourcesDirectory(dir.resolve("resources"));
    }

    private Path write(String name, String content) throws IOException {
        Path models = Files.createDirectories(dir.resolve("model"));
        return Files.write(models.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> list(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }
}
