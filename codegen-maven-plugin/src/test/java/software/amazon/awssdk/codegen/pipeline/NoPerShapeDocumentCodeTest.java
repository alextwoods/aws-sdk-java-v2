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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.Exemption;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.ExemptRegistry;
import software.amazon.awssdk.codegen.pipeline.PerShapeDocumentCodeScanner.Result;

/**
 * Checks that Codegen generates no per-shape document conversion code (task 11.18, design B.6, Requirement 24.1): no
 * {@code toDocument}/{@code fromDocument} method or field and no {@code *DocumentConverter},
 * {@code *DocumentSerializer} or {@code *DocumentDeserializer} type outside the exempt registries
 * ({@link PerShapeDocumentCodeScanner}).
 *
 * <p>Each Prototype_Service_Set module ({@link LegacyRouteParity#PROTOTYPE_MODULES}) is generated through the
 * {@link CodegenPipeline} as {@code GenerationMojo} does ({@link LegacyRouteParity#generateWithPipeline}), and its
 * generated sources and tests are scanned. The test also checks that the scan found each exempt registry's
 * declaration, so that a renamed registry fails the test instead of being scanned as ordinary code unnoticed.
 *
 * <p>No Prototype_Service_Set service has a member that targets a {@code document} shape, so the Legacy_Document
 * exemption (Requirement 21) is not exercised by their output. The codegen golden files
 * ({@code codegen/src/test/resources/.../poet}), which the codegen module's generator tests compare with the
 * generators' output and which include model classes with {@code document} members, are scanned too.
 */
class NoPerShapeDocumentCodeTest {
    private static final Path CODEGEN_GOLDEN_FILES = Paths.get("..", "codegen", "src", "test", "resources", "software",
                                                               "amazon", "awssdk", "codegen", "poet");

    @TempDir
    Path tmp;

    static Stream<String> prototypeModules() {
        return LegacyRouteParity.PROTOTYPE_MODULES.stream();
    }

    @ParameterizedTest
    @MethodSource("prototypeModules")
    void prototypeModule_generatesNoPerShapeDocumentCode(String module) {
        assertThat(LegacyRouteParity.codegenResources(module)).as("C2J model of " + module).isDirectory();
        Path out = tmp.resolve("out");
        LegacyRouteParity.generateWithPipeline(module, out, tmp.resolve("build"));

        Result result = PerShapeDocumentCodeScanner.scan(out.resolve("src"), out.resolve("tests"));

        assertThat(result.violations())
            .as("%s generates per-shape document conversion code", module)
            .isEmpty();
        assertThat(result.exemptions())
            .as("%s generates the exempt registries under the names PerShapeDocumentCodeScanner.ExemptRegistry expects",
                module)
            .extracting(Exemption::registry)
            .containsAll(Arrays.asList(ExemptRegistry.values()));
    }

    @Test
    void codegenGoldenFiles_containNoPerShapeDocumentCode() {
        assertThat(CODEGEN_GOLDEN_FILES).isDirectory();

        Result result = PerShapeDocumentCodeScanner.scan(CODEGEN_GOLDEN_FILES);

        assertThat(result.violations()).as("codegen golden files with per-shape document conversion code").isEmpty();
        assertThat(result.exemptions()).extracting(Exemption::registry)
                                       .containsAll(Arrays.asList(ExemptRegistry.values()));
        // The golden files include the Legacy_Document handling of a document member.
        assertThat(PerOperationSerdeScanner.javaFiles(CODEGEN_GOLDEN_FILES))
            .anySatisfy(f -> assertThat(PerOperationSerdeScanner.read(f)).contains("SchemaSerdeSupport.wrapLegacy("));
    }
}
