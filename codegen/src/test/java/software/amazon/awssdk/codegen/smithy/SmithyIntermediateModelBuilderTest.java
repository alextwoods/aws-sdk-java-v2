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

package software.amazon.awssdk.codegen.smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Covers the endpoint models reaching the intermediate model from the service's traits (the Smithy
 * path takes no sidecar input, so a service without the traits lands on the same defaults the C2J
 * path uses when the sidecar files are absent), and IDL 1 {@code @enum} strings becoming enums.
 */
class SmithyIntermediateModelBuilderTest {

    /**
     * {@code clientContextParams} binds the rule-set parameter to the service model, which the
     * rules-engine validator requires for any parameter that is not a registered built-in.
     */
    private static final String RULE_SET =
        "@smithy.rules#clientContextParams(\n"
        + "  Region: { type: \"string\", documentation: \"The region\" }\n"
        + ")\n"
        + "@smithy.rules#endpointRuleSet({\n"
        + "  version: \"1.0\"\n"
        + "  parameters: {\n"
        + "    Region: { required: false, type: \"string\" }\n"
        + "  }\n"
        + "  rules: [\n"
        + "    { conditions: [], endpoint: { url: \"https://example.amazonaws.com\" }, type: \"endpoint\" }\n"
        + "  ]\n"
        + "})\n";

    private static final String TESTS =
        "@smithy.rules#endpointTests({\n"
        + "  version: \"1.0\"\n"
        + "  testCases: [\n"
        + "    {\n"
        + "      documentation: \"from the trait\"\n"
        + "      params: { Region: \"us-east-1\" }\n"
        + "      expect: { endpoint: { url: \"https://example.amazonaws.com\" } }\n"
        + "    }\n"
        + "  ]\n"
        + "})\n";

    private static IntermediateModel build(String serviceTraits) {
        return buildService(serviceTraits
                            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
                            + "@http(method: \"POST\", uri: \"/op\")\n"
                            + "operation Op { input: OpRequest, output: OpResponse }\n"
                            + "structure OpRequest {}\n"
                            + "structure OpResponse {}\n");
    }

    /**
     * @param serviceAndShapes the service shape (with any extra traits before it) and every shape after the common
     *                         {@code @service}, {@code @sigv4} and {@code @restJson1} traits
     */
    private static IntermediateModel buildService(String serviceAndShapes) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + serviceAndShapes;
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", src)
                           .assemble()
                           .unwrap();
        return new SmithyIntermediateModelBuilder(
            SmithyModels.builder()
                        .model(model)
                        .customizationConfig(CustomizationConfig.create())
                        .build()).build();
    }

    private static IntermediateModel buildWithRequestMember(String shapes) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { state: State }\n"
            + "structure OpResponse {}\n"
            + shapes;
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", src)
                           .assemble()
                           .unwrap();
        return new SmithyIntermediateModelBuilder(
            SmithyModels.builder()
                        .model(model)
                        .customizationConfig(CustomizationConfig.create())
                        .build()).build();
    }

    /**
     * Native AWS Smithy models (AWS Account) still use IDL 1 {@code @enum} strings. They are generated as Java enums,
     * the same as an {@code enum} shape or a C2J enum, including values that have no enum name.
     */
    @Test
    void enumTraitString_isGeneratedAsAnEnum() {
        IntermediateModel model = buildWithRequestMember(
            "@enum([{ value: \"ACTIVE\" }, { value: \"pending-activation\" }])\n"
            + "string State\n");

        ShapeModel state = model.getShapes().get("State");
        assertThat(state).isNotNull();
        assertThat(state.getEnums()).extracting(EnumModel::getValue).containsExactly("ACTIVE", "pending-activation");
        assertThat(state.getEnums()).extracting(EnumModel::getName).containsExactly("ACTIVE", "PENDING_ACTIVATION");
        assertThat(model.getShapes().get("OpRequest").findMemberModelByC2jName("state").getEnumType()).isEqualTo("State");
        assertThat(model.smithyModel().expectShape(ShapeId.from("demo#State")).isEnumShape()).isTrue();
    }

    @Test
    void enumTraitStringThatCannotBeConverted_fails() {
        assertThatThrownBy(() -> buildWithRequestMember("@enum([{ value: \"1x\" }])\nstring State\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("demo#State");
    }

    /**
     * A union member targeting {@code smithy.api#Unit} gets the empty {@code Unit} model class a C2J model declares for
     * it (RestJson {@code PlayerAction} in the Smithy protocol tests).
     */
    @Test
    void unionMemberTargetingUnit_getsAUnitModelShape() {
        IntermediateModel model = buildWithRequestMember("union State { quit: Unit, name: String }\n");

        ShapeModel unit = model.getShapes().get("Unit");
        assertThat(unit).isNotNull();
        assertThat(unit.getType()).isEqualTo("Model");
        assertThat(unit.getMembers()).isEmpty();
        assertThat(model.getShapes().get("State").findMemberModelByC2jName("quit").getShape()).isSameAs(unit);
    }

    /**
     * An error structure in an event stream is a modeled exception event: it gets an exception class even when no
     * operation lists it, it is not an event, and the event stream member is linked to it.
     */
    @Test
    void errorInEventStream_isAnExceptionNotAnEvent() {
        IntermediateModel model = buildService(
            "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { @httpPayload state: State }\n"
            + "structure OpResponse {}\n"
            + "@streaming\nunion State { ping: Ping, failure: Failure }\n"
            + "structure Ping { @eventPayload payload: Blob }\n"
            + "@error(\"client\")\nstructure Failure { message: String }\n");

        ShapeModel failure = model.getShapes().get("FailureException");
        assertThat(failure).isNotNull();
        assertThat(failure.getType()).isEqualTo("Exception");
        assertThat(failure.isEvent()).isFalse();
        assertThat(model.getShapes().get("Ping").isEvent()).isTrue();
        ShapeModel state = model.getShapes().get("State");
        assertThat(state.isEventStream()).isTrue();
        assertThat(state.findMemberModelByC2jName("failure").getShape()).isSameAs(failure);
        assertThat(model.getShapes().get("OpRequest").findMemberModelByC2jName("state").getHttp().getIsStreaming())
            .as("an event stream is not a streaming blob")
            .isFalse();
    }

    /**
     * A service with no auth scheme has the effective auth {@code smithy.api#noAuth} (Smithy protocol test services
     * such as RestJsonValidation and RpcV2Protocol).
     */
    @Test
    void serviceWithoutAuth_hasNoAuth() {
        String src = "$version: \"2.0\"\nnamespace demo\n\n"
                     + "use aws.api#service\nuse aws.protocols#restJson1\n"
                     + "@service(sdkId: \"Demo\")\n@restJson1\n"
                     + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
                     + "@http(method: \"POST\", uri: \"/op\")\noperation Op {}\n";
        Model smithy = Model.assembler().discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("test.smithy", src).assemble().unwrap();
        IntermediateModel model = new SmithyIntermediateModelBuilder(
            SmithyModels.builder().model(smithy).customizationConfig(CustomizationConfig.create()).build()).build();

        assertThat(model.getMetadata().getAuthType()).isNull();
        assertThat(model.getMetadata().getAuth()).containsExactly(AuthType.NONE);
    }

    @Test
    void endpointTraitsPresent_reachTheIntermediateModel() {
        IntermediateModel model = build(RULE_SET + TESTS);

        assertThat(model.getEndpointRuleSetModel().getVersion()).isEqualTo("1.0");
        assertThat(model.getEndpointRuleSetModel().getParameters()).containsOnlyKeys("Region");
        assertThat(model.getEndpointTestSuiteModel().getTestCases()).hasSize(1);
    }

    /**
     * Neither getter returns null. {@code IntermediateModel} substitutes an empty test suite, and
     * {@code EndpointRuleSetModel.defaultRules(endpointPrefix)} for the rule-set, so a service
     * without the traits degrades to the generic rules exactly as a C2J service with no sidecar does.
     */
    @Test
    void endpointTraitsAbsent_fallBackToTheDefaultRules() {
        IntermediateModel model = build("");

        assertThat(model.getEndpointRuleSetModel())
            .usingRecursiveComparison()
            .isEqualTo(EndpointRuleSetModel.defaultRules(model.getMetadata().getEndpointPrefix()));
        assertThat(model.getEndpointTestSuiteModel().getTestCases()).isEmpty();
    }
}
