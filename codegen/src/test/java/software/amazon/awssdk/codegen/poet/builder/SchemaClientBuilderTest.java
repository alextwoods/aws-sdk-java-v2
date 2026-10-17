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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    void defaultProtocolWithOnlyAStagedFactory_keepsTheLegacyBuilder() {
        // awsJson1_0 has a staged factory only: without stagedClientProtocols the builder gets no protocol selection.
        IntermediateModel model = model("@awsJson1_0\n@awsQuery");

        assertThat(ClientProtocolSelection.forModel(model)).isEmpty();
        assertThat(methodNames(new BaseClientBuilderInterface(model).poetSpec())).doesNotContain("protocol");
        TypeSpec builderClass = new BaseClientBuilderClass(model).poetSpec();
        assertThat(methodNames(builderClass)).doesNotContain("protocol", "protocolSettings");
        assertThat(builderClass.toString()).doesNotContain("CLIENT_PROTOCOL");
    }

    @ParameterizedTest
    @CsvSource({
        "@awsJson1_0 @awsQuery, aws.protocols#awsJson1_0, awsjson.AwsJson1Protocol",
        "@awsJson1_1, aws.protocols#awsJson1_1, awsjson.AwsJson11Protocol",
        "@awsJson1_1 @awsJson1_0, aws.protocols#awsJson1_0, awsjson.AwsJson1Protocol",
        "@restJson1, aws.protocols#restJson1, restjson.RestJsonClientProtocol",
        "@restJson1 @awsJson1_1, aws.protocols#awsJson1_1, awsjson.AwsJson11Protocol",
        "@restXml, aws.protocols#restXml, restxml.RestXmlClientProtocol",
        "@restXml @restJson1, aws.protocols#restJson1, restjson.RestJsonClientProtocol"
    })
    void stagedClientProtocols_selectsTheStagedFactory(String traits, String protocol, String protocolClass) {
        IntermediateModel model = model(traits, stagedClientProtocols());

        ClientProtocolSelection selection = ClientProtocolSelection.forModel(model).orElseThrow(AssertionError::new);

        assertThat(selection.defaultProtocol()).isEqualTo(ShapeId.from(protocol));
        assertThat(selection.factoryClass().reflectionName())
            .isEqualTo("software.amazon.awssdk.protocols.client." + protocolClass + "$Factory");
        assertThat(methodNames(new BaseClientBuilderInterface(model).poetSpec())).contains("protocol");
        assertThat(new BaseClientBuilderClass(model, schemaCodegen(model)).poetSpec().toString())
            .contains("CLIENT_PROTOCOL_FACTORY, software.amazon.awssdk.protocols.client." + protocolClass
                      + ".Factory.INSTANCE");
    }

    @ParameterizedTest
    @CsvSource({"@restJson1", "@restXml"})
    void restProtocolWithoutStagedClientProtocols_keepsTheLegacyBuilder(String traits) {
        assertThat(ClientProtocolSelection.forModel(model(traits))).isEmpty();
    }

    @Test
    void stagedClientProtocols_keepsRpcv2CborFirstAndChangesNothingWithoutAFactory() {
        ClientProtocolSelection selection = ClientProtocolSelection.forModel(model("@awsJson1_0\n@rpcv2Cbor",
                                                                                   stagedClientProtocols()))
                                                                   .orElseThrow(AssertionError::new);
        assertThat(selection.factoryClass().reflectionName())
            .isEqualTo("software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol$Factory");

        assertThat(ClientProtocolSelection.forModel(model("@awsQuery", stagedClientProtocols()))).isEmpty();
    }

    private static CustomizationConfig stagedClientProtocols() {
        CustomizationConfig config = CustomizationConfig.create();
        config.setStagedClientProtocols(true);
        return config;
    }

    private static SchemaCodegen schemaCodegen(IntermediateModel model) {
        return GeneratorTaskParams.create(model, "sources/", "tests/", "resources/").getSchemaCodegen();
    }

    private static List<String> methodNames(TypeSpec spec) {
        return spec.methodSpecs.stream().map(m -> m.name).collect(Collectors.toList());
    }

    private static IntermediateModel model(String protocolTraits) {
        return model(protocolTraits, CustomizationConfig.create());
    }

    private static IntermediateModel model(String protocolTraits, CustomizationConfig config) {
        String src = "$version: \"2.0\"\nnamespace demo\n\n"
                     + "use aws.api#service\n"
                     + "use aws.auth#sigv4\n"
                     + "use aws.protocols#awsJson1_0\n"
                     + "use aws.protocols#awsJson1_1\n"
                     + "use aws.protocols#awsQuery\n"
                     + "use aws.protocols#restJson1\n"
                     + "use aws.protocols#restXml\n"
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
                                                              .customizationConfig(config)
                                                              .build())
            .build();
    }
}
