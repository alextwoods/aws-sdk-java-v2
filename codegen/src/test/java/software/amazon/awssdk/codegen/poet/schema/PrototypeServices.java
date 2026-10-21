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

package software.amazon.awssdk.codegen.poet.schema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assumptions;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline;
import software.amazon.awssdk.codegen.pipeline.ServiceInputs;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Prepares the IntermediateModel of a C2J-route prototype service from its {@code services/<name>} codegen resources,
 * as the codegen Maven plugin does.
 */
final class PrototypeServices {
    private PrototypeServices() {
    }

    /**
     * Runs pipeline stages 1 to 5 for a service. Skips the calling test if the service's C2J model is not in the
     * repository.
     *
     * @param service the service module name, for example {@code sqs}
     * @param tmp a directory for the pipeline's output and build directories
     */
    static IntermediateModel prepare(String service, Path tmp) {
        Path dir = directory(service);
        Assumptions.assumeTrue(Files.isRegularFile(dir.resolve("service-2.json")), "C2J model of " + service
                                                                                   + " not found");
        CustomizationConfig config = ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                                                                        dir.resolve("customization.config").toFile(),
                                                                        true)
                                                     .orElse(CustomizationConfig.create());
        ServiceInputs inputs = ServiceInputs.builder()
                                            .serviceName(service)
                                            .customizationConfig(config)
                                            .c2jModelDirectory(dir)
                                            .sourcesDirectory(tmp.resolve(service + "/src"))
                                            .resourcesDirectory(tmp.resolve(service + "/resources"))
                                            .testsDirectory(tmp.resolve(service + "/tests"))
                                            .buildDirectory(tmp.resolve(service + "/build"))
                                            .classLoader(CodegenPipeline.class.getClassLoader())
                                            .build();
        return CodegenPipeline.create().prepare(inputs).intermediateModel();
    }

    /**
     * @return the codegen resources directory of a service module
     */
    static Path directory(String service) {
        Path dir = Paths.get("..", "services", service, "src", "main", "resources", "codegen-resources");
        if (!Files.isRegularFile(dir.resolve("service-2.json"))) {
            dir = dir.resolve(service);
        }
        return dir;
    }

    /**
     * @return the trait initializers the codegen Maven plugin uses
     */
    static TraitInitializers initializers() {
        return TraitInitializers.load(PrototypeServices.class.getClassLoader());
    }
}
