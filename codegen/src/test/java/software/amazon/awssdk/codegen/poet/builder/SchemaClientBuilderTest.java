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

package software.amazon.awssdk.codegen.poet.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.builder.BuilderClassTestUtils.validateGeneration;

import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.schema.ClientProtocolSelection;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.poet.schema.SchemaOperationRouting;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Runtime protocol selection in the base client builders of schema-enabled services (task 3.23, Requirements 12.1,
 * 12.3, 12.5, 16.3, 10.8), and the legacy protocol option that a schema-based service's builder does not set (task
 * 5.1). Builders of services generated from C2J are covered by the other builder tests, whose golden files do not
 * change.
 */
class SchemaClientBuilderTest {
    @Test
    void baseClientBuilderInterface_hasADefaultProtocolMethod() {
        validateGeneration(BaseClientBuilderInterface::new, model("@rpcv2Cbor\n@awsJson1_0\n@awsQuery"),
                           "test-schema-client-builder-interface.java");
    }

    @Test
    void baseClientBuilderClass_defaultsToTheHighestPriorityProtocolAndValidatesTheTransport() {
        validateGeneration(m -> new BaseClientBuilderClass(m, schemaCodegen(m)), model("@rpcv2Cbor\n@awsJson1_0\n@awsQuery"),
                           "test-schema-client-builder-class.java");
    }

    @Test
    void baseClientBuilderClass_forASchemaBasedService_setsNoLegacyProtocolOption() {
        // Every operation of the fixture is routed (task 5.1): no legacy JSON protocol factory reads
        // ENABLE_FAST_UNMARSHALLER, so the builder does not set it.
        IntermediateModel model = model("@rpcv2Cbor\n@awsJson1_0\n@awsQuery");
        SchemaCodegen schemaCodegen = schemaCodegen(model);

        assertThat(SchemaOperationRouting.routesEveryOperation(model, schemaCodegen)).isTrue();
        assertThat(new BaseClientBuilderClass(model, schemaCodegen).poetSpec().toString())
            .doesNotContain("ENABLE_FAST_UNMARSHALLER", "SdkClientJsonProtocolAdvancedOption");
    }

    @Test
    void baseClientBuilderClass_withoutRuntimeProtocolSelection_keepsTheLegacyProtocolOption() {
        IntermediateModel model = model("@awsJson1_0\n@awsQuery");
        SchemaCodegen schemaCodegen = schemaCodegen(model);

        assertThat(SchemaOperationRouting.routesEveryOperation(model, schemaCodegen)).isFalse();
        assertThat(new BaseClientBuilderClass(model, schemaCodegen).poetSpec().toString())
            .contains("ENABLE_FAST_UNMARSHALLER");
    }

    @Test
    void selection_picksRpcv2CborForACloudWatchLikeService() {
        ClientProtocolSelection selection = ClientProtocolSelection.forModel(model("@awsQuery\n@awsJson1_0\n@rpcv2Cbor"))
                                                                   .orElseThrow(AssertionError::new);

        assertThat(selection.defaultProtocol()).isEqualTo(ShapeId.from("smithy.protocols#rpcv2Cbor"));
        assertThat(selection.factoryClass().reflectionName())
            .isEqualTo("software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol$Factory");
        assertThat(selection.baseExceptionClass().reflectionName())
            .isEqualTo("software.amazon.awssdk.services.demo.model.DemoException");
        assertThat(selection.serviceSchema().toString())
            .isEqualTo("software.amazon.awssdk.services.demo.internal.DemoServiceSchemas.SERVICE");
        assertThat(selection.serviceVersion()).isEqualTo("2024-01-01");
    }

    @Test
    void defaultProtocolWithoutAFactory_keepsTheLegacyBuilder() {
        // awsJson1_0 has no ClientProtocolFactory in phase 2, so the builder gets no protocol selection.
        IntermediateModel model = model("@awsJson1_0\n@awsQuery");

        assertThat(ClientProtocolSelection.forModel(model)).isEmpty();
        assertThat(methodNames(new BaseClientBuilderInterface(model).poetSpec())).doesNotContain("protocol");
        TypeSpec builderClass = new BaseClientBuilderClass(model).poetSpec();
        assertThat(methodNames(builderClass)).doesNotContain("protocol", "protocolSettings");
        assertThat(builderClass.toString()).doesNotContain("CLIENT_PROTOCOL");
    }

    private static SchemaCodegen schemaCodegen(IntermediateModel model) {
        return GeneratorTaskParams.create(model, "sources/", "tests/", "resources/").getSchemaCodegen();
    }

    private static List<String> methodNames(TypeSpec spec) {
        return spec.methodSpecs.stream().map(m -> m.name).collect(Collectors.toList());
    }

    private static IntermediateModel model(String protocolTraits) {
        String src = "$version: \"2.0\"\nnamespace demo\n\n"
                     + "use aws.api#service\n"
                     + "use aws.auth#sigv4\n"
                     + "use aws.protocols#awsJson1_0\n"
                     + "use aws.protocols#awsQuery\n"
                     + "use smithy.protocols#rpcv2Cbor\n"
                     + "@service(sdkId: \"Demo\")\n"
                     + "@sigv4(name: \"demo\")\n"
                     + "@xmlNamespace(uri: \"https://demo.example.com\")\n"
                     + protocolTraits + "\n"
                     + "service DemoService { version: \"2024-01-01\", operations: [GetThing] }\n"
                     + "operation GetThing { input: GetThingRequest, output: GetThingResponse }\n"
                     + "structure GetThingRequest { Name: String }\n"
                     + "structure GetThingResponse { Name: String }\n";
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("demo.smithy", src)
                            .assemble()
                            .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(smithy)
                                                              .customizationConfig(CustomizationConfig.create())
                                                              .build())
            .build();
    }
}
