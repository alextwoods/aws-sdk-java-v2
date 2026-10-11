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

package software.amazon.awssdk.codegen.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;

class TraitInclusionPolicyTest {
    private static final String IDL = String.join("\n",
        "$version: \"2\"",
        "namespace test.policy",
        "",
        "use aws.api#service",
        "use aws.auth#sigv4",
        "use aws.protocols#awsJson1_0",
        "use aws.protocols#awsQuery",
        "use aws.protocols#restJson1",
        "use smithy.protocols#rpcv2Cbor",
        "",
        "@restJson1",
        "@sigv4(name: \"rest\")",
        "@service(sdkId: \"Rest\")",
        "@documentation(\"A service.\")",
        "service RestService { version: \"2020-01-01\", operations: [GetItem] }",
        "",
        "@rpcv2Cbor",
        "@awsJson1_0",
        "@awsQuery",
        "@xmlNamespace(uri: \"https://example.com/doc/\")",
        "service MultiProtocolService { version: \"2020-01-01\" }",
        "",
        "@customProtocol",
        "@customAuth",
        "service CustomService { version: \"2020-01-01\" }",
        "",
        "@trait(selector: \"service\")",
        "@protocolDefinition(traits: [length, documentation, jsonName, customMarker])",
        "structure customProtocol {}",
        "",
        "@trait(selector: \"service\")",
        "@authDefinition(traits: [customAuthMarker])",
        "structure customAuth {}",
        "",
        "@trait",
        "structure customMarker {}",
        "",
        "@trait",
        "structure customAuthMarker {}",
        "",
        "@http(method: \"GET\", uri: \"/items\")",
        "@readonly",
        "operation GetItem { input: Item, output: Item }",
        "",
        "structure Item {",
        "    @jsonName(\"n\")",
        "    @documentation(\"The name.\")",
        "    @length(min: 1)",
        "    @required",
        "    @httpHeader(\"X-Name\")",
        "    name: String",
        "}");

    private static Model model;

    @BeforeAll
    static void loadModel() {
        model = Model.assembler(TraitInclusionPolicyTest.class.getClassLoader())
                     .discoverModels(TraitInclusionPolicyTest.class.getClassLoader())
                     .addUnparsedModel("test.smithy", IDL)
                     .assemble()
                     .unwrap();
    }

    @Test
    void effectiveSetHasDefaultsProtocolAndAuthTraitsWithoutExclusions() {
        TraitInclusionPolicy policy = policy("RestService", TraitInclusionConfig.empty());

        assertThat(policy.effectiveTraits()).containsAll(DefaultTraitInclusion.DEFAULT_TRAITS);
        assertThat(policy.effectiveTraits()).contains(id("aws.protocols#restJson1"), id("aws.auth#sigv4"));
        List<ShapeId> definitionTraits = new ArrayList<>(
            model.expectShape(id("aws.protocols#restJson1")).expectTrait(ProtocolDefinitionTrait.class).getTraits());
        definitionTraits.addAll(model.expectShape(id("aws.auth#sigv4")).expectTrait(AuthDefinitionTrait.class).getTraits());
        definitionTraits.removeAll(DefaultTraitInclusion.EXCLUDED_CONSTRAINT_TRAITS);
        assertThat(definitionTraits).contains(id("smithy.api#httpHeader"), id("smithy.api#httpLabel"));
        assertThat(policy.effectiveTraits()).containsAll(definitionTraits);

        assertThat(policy.effectiveTraits()).doesNotContainAnyElementsOf(DefaultTraitInclusion.EXCLUDED_CONSTRAINT_TRAITS);
        assertThat(policy.effectiveTraits()).doesNotContainAnyElementsOf(DefaultTraitInclusion.HARD_EXCLUDED_TRAITS);
        // Service traits that are not protocol or auth definitions are not included.
        assertThat(policy.effectiveTraits()).doesNotContain(id("aws.api#service"));
        assertThat(policy.configuredTraits()).isEmpty();
        assertThat(policy.sdkTraits()).isEqualTo(policy.effectiveTraits());
        assertThat(policy.serviceId()).isEqualTo(id("test.policy#RestService"));
    }

    @Test
    void everyProtocolTraitOfAMultiProtocolServiceIsIncluded() {
        TraitInclusionPolicy policy = policy("MultiProtocolService", TraitInclusionConfig.empty());

        assertThat(policy.effectiveTraits()).contains(id("smithy.protocols#rpcv2Cbor"), id("aws.protocols#awsJson1_0"),
                                                      id("aws.protocols#awsQuery"));
        assertThat(policy.effectiveTraits()).doesNotContain(id("aws.protocols#restJson1"));
        // restJson1's binding traits come only from restJson1, which this service does not declare.
        assertThat(policy.effectiveTraits()).doesNotContain(id("smithy.api#httpHeader"));
    }

    @Test
    void constraintAndHardExcludedTraitsListedByADefinitionAreLeftOut() {
        TraitInclusionPolicy policy = policy("CustomService", TraitInclusionConfig.empty());

        assertThat(policy.effectiveTraits()).contains(id("test.policy#customProtocol"), id("test.policy#customMarker"),
                                                      id("test.policy#customAuth"), id("test.policy#customAuthMarker"),
                                                      id("smithy.api#jsonName"));
        assertThat(policy.effectiveTraits()).doesNotContain(id("smithy.api#length"), id("smithy.api#documentation"));
    }

    @Test
    void configAdditionsAreIncludedAndConfigRemovalsUndoThem() {
        TraitInclusionConfig config = TraitInclusionConfig.builder()
                                                          .add("smithy.api#deprecated")
                                                          .add(id("smithy.api#readonly"))
                                                          .add("smithy.api#length")
                                                          .add("smithy.api#jsonName")
                                                          .remove("smithy.api#readonly")
                                                          .build();
        TraitInclusionPolicy policy = policy("RestService", config);

        assertThat(policy.includes(id("smithy.api#deprecated"))).isTrue();
        assertThat(policy.includes(id("smithy.api#length"))).isTrue();
        assertThat(policy.includes(id("smithy.api#readonly"))).isFalse();
        // Adding a trait the SDK already includes changes nothing.
        assertThat(policy.configuredTraits()).containsExactly(id("smithy.api#deprecated"), id("smithy.api#length"));
        assertThat(policy.effectiveTraits()).containsAll(policy.sdkTraits()).containsAll(policy.configuredTraits());
    }

    @Test
    void removingADefaultTraitFails() {
        assertThatThrownBy(() -> policy("RestService", TraitInclusionConfig.builder().remove("smithy.api#jsonName").build()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#jsonName")
            .hasMessageContaining("cannot be removed");
    }

    @Test
    void removingAProtocolOrAuthTraitFails() {
        assertThatThrownBy(() -> policy("RestService", TraitInclusionConfig.builder()
                                                                           .remove("smithy.api#httpHeader")
                                                                           .remove("aws.protocols#restJson1")
                                                                           .remove("aws.auth#sigv4")
                                                                           .build()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#httpHeader")
            .hasMessageContaining("aws.protocols#restJson1")
            .hasMessageContaining("aws.auth#sigv4");
    }

    @Test
    void removingATraitThatWasNotAddedFails() {
        assertThatThrownBy(() -> policy("RestService", TraitInclusionConfig.builder().remove("smithy.api#readonly").build()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#readonly")
            .hasMessageContaining("only traits added by schemaTraits can be removed");
    }

    @Test
    void addingAHardExcludedTraitFails() {
        assertThatThrownBy(() -> policy("RestService", TraitInclusionConfig.builder()
                                                                           .add("smithy.api#documentation")
                                                                           .add("com.amazonaws.c2j#locationName")
                                                                           .build()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#documentation")
            .hasMessageContaining("com.amazonaws.c2j#locationName")
            .hasMessageContaining("cannot be added");
    }

    @Test
    void invalidTraitIdentifiersFail() {
        assertThatThrownBy(() -> policy("RestService", TraitInclusionConfig.builder()
                                                                           .add("jsonName")
                                                                           .add("smithy.api#")
                                                                           .remove("not a shape id")
                                                                           .add("smithy.api#Shape$member")
                                                                           .build()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("`jsonName`")
            .hasMessageContaining("`smithy.api#`")
            .hasMessageContaining("`not a shape id`")
            .hasMessageContaining("`smithy.api#Shape$member`");
    }

    @Test
    void includedTraitsAreAShapesOwnIncludedTraitsSortedById() {
        TraitInclusionPolicy policy = policy("RestService", TraitInclusionConfig.empty());

        List<Trait> traits = policy.includedTraits(model.expectShape(id("test.policy#Item$name")));
        List<ShapeId> ids = new ArrayList<>();
        traits.forEach(t -> ids.add(t.toShapeId()));
        assertThat(ids).containsExactly(id("smithy.api#httpHeader"), id("smithy.api#jsonName"), id("smithy.api#required"));
        // A member does not carry its target's traits.
        assertThat(policy.includedTraits(model.expectShape(id("smithy.api#String")))).isEmpty();
    }

    @Test
    void configIsReadFromCustomizationConfig() throws IOException {
        CustomizationConfig customization = Jackson.load(CustomizationConfig.class,
            "{\"schemaTraits\": {\"add\": [\"smithy.api#deprecated\", \"smithy.api#readonly\"], "
            + "\"remove\": [\"smithy.api#readonly\"]}}");
        TraitInclusionConfig config = TraitInclusionConfig.fromCustomization(customization);

        assertThat(config.additions()).containsExactly("smithy.api#deprecated", "smithy.api#readonly");
        assertThat(config.removals()).containsExactly("smithy.api#readonly");
        assertThat(TraitInclusionConfig.fromCustomization(CustomizationConfig.create()).additions()).isEmpty();
        assertThat(TraitInclusionConfig.fromCustomization(null).removals()).isEmpty();
    }

    @Test
    void forIntermediateModelUsesItsServiceAndCustomizationConfig() throws IOException {
        CustomizationConfig customization = Jackson.load(CustomizationConfig.class,
            "{\"schemaTraits\": {\"add\": [\"smithy.api#deprecated\"]}}");
        IntermediateModel intermediateModel =
            new IntermediateModel(new Metadata(), Collections.emptyMap(), Collections.emptyMap(), customization)
                .withSmithyModel(model, id("test.policy#RestService"));

        TraitInclusionPolicy policy = TraitInclusionPolicy.forService(intermediateModel);
        assertThat(policy.serviceId()).isEqualTo(id("test.policy#RestService"));
        assertThat(policy.configuredTraits()).containsExactly(id("smithy.api#deprecated"));
        assertThat(policy.includes(id("aws.protocols#restJson1"))).isTrue();
    }

    private static TraitInclusionPolicy policy(String service, TraitInclusionConfig config) {
        return TraitInclusionPolicy.forService(model, model.expectShape(id("test.policy#" + service), ServiceShape.class),
                                               config);
    }

    private static ShapeId id(String id) {
        return ShapeId.from(id);
    }
}
