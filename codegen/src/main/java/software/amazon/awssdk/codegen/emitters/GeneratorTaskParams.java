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

package software.amazon.awssdk.codegen.emitters;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.emitters.tasks.SharedModelsTaskParamsValidator;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.schema.TraitInitializers;

/**
 * Parameters for generator tasks.
 */
public class GeneratorTaskParams {
    private static final Consumer<GeneratorTaskParams> TASK_PARAMS_VALIDATORS = new SharedModelsTaskParamsValidator();

    private final IntermediateModel model;
    private final IntermediateModel shareModelsTarget;
    private final GeneratorPathProvider pathProvider;
    private final PoetExtension poetExtensions;
    private final Logger log = LoggerFactory.getLogger(GeneratorTaskParams.class);
    private SchemaCodegen schemaCodegen;
    private boolean schemaCodegenPrepared;

    private GeneratorTaskParams(IntermediateModel model,
                                IntermediateModel shareModelsTarget,
                                GeneratorPathProvider pathProvider) {
        this.model = model;
        this.shareModelsTarget = shareModelsTarget;
        this.pathProvider = pathProvider;
        this.poetExtensions = new PoetExtension(model);
    }

    public static GeneratorTaskParams create(IntermediateModel model, String sourceDirectory, String testDirectory,
                                             String resourcesDirectory) {
        return create(model, sourceDirectory, testDirectory, resourcesDirectory, null);
    }

    /**
     * @param shareModelsTarget the model of the service whose model package this service shares
     *     ({@code shareModelConfig}), or null
     */
    public static GeneratorTaskParams create(IntermediateModel model, String sourceDirectory, String testDirectory,
                                             String resourcesDirectory, IntermediateModel shareModelsTarget) {
        GeneratorPathProvider pathProvider = new GeneratorPathProvider(model, sourceDirectory, testDirectory, resourcesDirectory);
        GeneratorTaskParams params = new GeneratorTaskParams(model, shareModelsTarget, pathProvider);
        TASK_PARAMS_VALIDATORS.accept(params);
        return params;
    }

    /**
     * @return Built intermediate model
     */
    public IntermediateModel getModel() {
        return model;
    }

    /**
     * @return the model of the service whose model package this service shares, or null
     */
    public IntermediateModel getShareModelsTarget() {
        return shareModelsTarget;
    }

    /**
     * Returns the Schema_Generator of the model, prepared once and shared by the schema and model class generator
     * tasks: the model classes read which of them get schema serde members (task 3.7).
     *
     * @return the prepared generator, or null if the model gets no schemas ({@link SchemaCodegen#isApplicable})
     * @throws software.amazon.awssdk.codegen.schema.SchemaCodegenException if schema generation cannot proceed
     */
    public synchronized SchemaCodegen getSchemaCodegen() {
        if (!schemaCodegenPrepared) {
            schemaCodegen = SchemaCodegen.isApplicable(model)
                            ? SchemaCodegen.create(model, TraitInitializers.load(GeneratorTaskParams.class.getClassLoader()))
                            : null;
            schemaCodegenPrepared = true;
        }
        return schemaCodegen;
    }

    /**
     * @return Provider for common paths.
     */
    public GeneratorPathProvider getPathProvider() {
        return pathProvider;
    }

    /**
     * @return Extensions and convenience methods for Java Poet.
     */
    public PoetExtension getPoetExtensions() {
        return poetExtensions;
    }

    /**
     * @return Logger
     */
    public Logger getLog() {
        return log;
    }
}
