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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.c2j2smithy.C2jInput;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Requirement 17.9: the intermediate model is built from standard traits only. For each Prototype_Service_Set
 * service, removing every {@code com.amazonaws.c2j} marker trait from the converted model must not change the
 * intermediate model {@code SmithyIntermediateModelBuilder} builds from it (compared by its JSON form).
 *
 * <p>A smoke check, not Property 46 (task 1.24): it covers the seven prototype models only.
 */
class MarkerTraitIndependenceTest {
    private static final String MARKER_NAMESPACE = "com.amazonaws.c2j";

    @TempDir
    Path tmp;

    @ParameterizedTest
    @ValueSource(strings = {"sqs", "secretsmanager", "account", "route53", "sns", "cloudwatch", "dynamodb"})
    void removingMarkerTraits_doesNotChangeTheIntermediateModel(String service) throws IOException {
        Path dir = C2jServiceModelRoundTripTest.prototypeServiceDirectory(service);
        Assumptions.assumeTrue(Files.isRegularFile(dir.resolve(C2jInput.SERVICE_MODEL_FILE)),
                               "C2J model of " + service + " not found");
        CustomizationConfig config = ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                                                                        dir.resolve("customization.config").toFile(),
                                                                        true)
                                                     .orElse(CustomizationConfig.create());
        Model model = CodegenPipeline.create().prepare(inputs(service, dir, config)).smithyModel();
        Model withoutMarkers = ModelTransformer.create().removeTraitsIf(
            model, (shape, trait) -> trait.toShapeId().getNamespace().equals(MARKER_NAMESPACE));
        assertThat(withoutMarkers.shapes().flatMap(s -> s.getAllTraits().keySet().stream()))
            .noneMatch(id -> id.getNamespace().equals(MARKER_NAMESPACE));

        assertThat(json(build(withoutMarkers, config))).isEqualTo(json(build(model, config)));
    }

    private ServiceInputs inputs(String service, Path dir, CustomizationConfig config) {
        return ServiceInputs.builder()
                            .serviceName(service)
                            .customizationConfig(config)
                            .c2jModelDirectory(dir)
                            .sourcesDirectory(tmp.resolve("src"))
                            .resourcesDirectory(tmp.resolve("resources"))
                            .testsDirectory(tmp.resolve("tests"))
                            .buildDirectory(tmp.resolve("build"))
                            .classLoader(CodegenPipeline.class.getClassLoader())
                            .build();
    }

    private static IntermediateModel build(Model model, CustomizationConfig config) {
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(model)
                                                              .customizationConfig(config)
                                                              .build())
            .build();
    }

    private static String json(IntermediateModel model) throws IOException {
        StringWriter writer = new StringWriter();
        Jackson.writeWithObjectMapper(model, writer);
        return writer.toString();
    }
}
