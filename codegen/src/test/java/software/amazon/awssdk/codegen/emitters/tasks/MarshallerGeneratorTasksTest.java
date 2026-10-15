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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.schema.SchemaOperationRouting;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.smithy.model.Model;

/**
 * A schema-based service, whose operations all run on the schema-based pipeline path, generates no {@code transform}
 * marshallers; every other service keeps them (task 5.1, Requirement 17.12). The fixture is the client tests'
 * {@code schema-routing.smithy}: rpcv2Cbor like CloudWatch, with an event-stream operation and one whose input class
 * has no schema serde members, which stay on the legacy path.
 */
class MarshallerGeneratorTasksTest {
    private static final String ALL_OPERATIONS = "operations: [GetThing, PutThing, ListThings, WatchThings, CountThings]";
    private static final String ROUTED_OPERATIONS = "operations: [GetThing, PutThing, ListThings]";

    @Test
    void createTasks_whenEveryOperationIsRouted_generatesNoMarshallers() {
        IntermediateModel model = model(resource().replace(ALL_OPERATIONS, ROUTED_OPERATIONS));
        GeneratorTaskParams params = params(model);

        assertThat(SchemaOperationRouting.routesEveryOperation(model, params.getSchemaCodegen())).isTrue();
        assertThat(new MarshallerGeneratorTasks(params).hasTasks()).isFalse();
    }

    @Test
    void createTasks_whenAnOperationStaysOnTheLegacyPath_generatesEveryRequestMarshaller() throws Exception {
        // The event-stream operation (methods only in the async client) keeps the whole service on marshallers.
        IntermediateModel model = model(resource().replace(ALL_OPERATIONS,
                                                           "operations: [GetThing, PutThing, ListThings, WatchThings]"));
        GeneratorTaskParams params = params(model);

        assertThat(SchemaOperationRouting.routesEveryOperation(model, params.getSchemaCodegen())).isFalse();
        MarshallerGeneratorTasks tasks = new MarshallerGeneratorTasks(params);
        assertThat(tasks.hasTasks()).isTrue();
        assertThat(tasks.createTasks()).hasSize(4);
    }

    @Test
    void createTasks_withoutRuntimeProtocolSelection_generatesEveryRequestMarshaller() throws Exception {
        IntermediateModel model = model(resource().replace(ALL_OPERATIONS, ROUTED_OPERATIONS)
                                                  .replace("@rpcv2Cbor\n@awsJson1_0", "@awsJson1_0"));
        MarshallerGeneratorTasks tasks = new MarshallerGeneratorTasks(params(model));

        assertThat(tasks.hasTasks()).isTrue();
        assertThat(tasks.createTasks()).hasSize(3);
    }

    @Test
    void createTasks_forAC2jModel_generatesMarshallers() throws Exception {
        MarshallerGeneratorTasks tasks = new MarshallerGeneratorTasks(params(ClientTestModels.rpcv2ServiceModels()));

        assertThat(tasks.hasTasks()).isTrue();
        assertThat(tasks.createTasks()).isNotEmpty();
    }

    private static GeneratorTaskParams params(IntermediateModel model) {
        return GeneratorTaskParams.create(model, "sources/", "tests/", "resources/");
    }

    private static IntermediateModel model(String src) {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("schema-routing.smithy", src)
                            .assemble()
                            .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(smithy)
                                                              .customizationConfig(CustomizationConfig.create())
                                                              .build())
            .build();
    }

    private static String resource() {
        try (InputStream in = MarshallerGeneratorTasksTest.class.getResourceAsStream(
            "/software/amazon/awssdk/codegen/poet/client/schema-routing.smithy")) {
            return IoUtils.toUtf8String(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
