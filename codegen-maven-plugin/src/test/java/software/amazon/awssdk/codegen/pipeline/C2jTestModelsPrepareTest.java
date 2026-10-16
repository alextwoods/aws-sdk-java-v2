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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Runs stages 1 to 5 of the {@link CodegenPipeline} on every C2J model of {@code test/codegen-generated-classes-test},
 * as {@code GenerationMojo} does for that module (which allows {@code smithyValidationSuppressions}), so that a
 * converter or validation change that breaks one of them fails here and not only when that module builds.
 *
 * <p>{@code test/protocol-tests} is not covered: it is not in the reactor (Requirement 32.9; the Smithy protocol tests
 * of {@code test/smithy-protocol-tests} replace it).
 */
class C2jTestModelsPrepareTest {
    private static final Path CODEGEN_RESOURCES = Paths.get("..", "test", "codegen-generated-classes-test", "src",
                                                            "main", "resources", "codegen-resources");

    @TempDir
    Path tmp;

    static List<Path> testModels() throws IOException {
        try (Stream<Path> dirs = Files.list(CODEGEN_RESOURCES)) {
            return dirs.filter(d -> Files.isRegularFile(d.resolve("service-2.json")))
                       .sorted()
                       .collect(Collectors.toList());
        }
    }

    @Test
    void testModels_areFound() throws IOException {
        assertThat(testModels()).hasSizeGreaterThanOrEqualTo(30);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testModels")
    void prepare_c2jTestModel_succeeds(Path modelDirectory) {
        String service = modelDirectory.getFileName().toString();
        ServiceInputs inputs = ServiceInputs.builder()
                                            .serviceName(service)
                                            .c2jModelDirectory(modelDirectory)
                                            .customizationConfig(customizationConfig(modelDirectory))
                                            .allowSmithyValidationSuppressions(true)
                                            .sourcesDirectory(tmp.resolve("sources"))
                                            .resourcesDirectory(tmp.resolve("resources"))
                                            .testsDirectory(tmp.resolve("tests"))
                                            .buildDirectory(tmp.resolve("build"))
                                            .build();

        PreparedService prepared = CodegenPipeline.builder()
                                                  .listener(new CodegenPipelineListener() {
                                                  })
                                                  .build()
                                                  .prepare(inputs);

        assertThat(prepared.intermediateModel()).isNotNull();
    }

    // As GenerationMojo loads it.
    private static CustomizationConfig customizationConfig(Path modelDirectory) {
        CustomizationConfig config = ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                                                                        modelDirectory.resolve("customization.config")
                                                                                      .toFile(),
                                                                        true)
                                                     .orElse(CustomizationConfig.create());
        config.resolveProtocolTestSkips(modelDirectory);
        return config;
    }
}
