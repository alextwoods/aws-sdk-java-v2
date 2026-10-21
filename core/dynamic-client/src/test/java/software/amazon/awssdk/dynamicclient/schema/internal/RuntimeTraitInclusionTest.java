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

package software.amazon.awssdk.dynamicclient.schema.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class RuntimeTraitInclusionTest {
    private static final String IDL = "$version: \"2.0\"\n"
                                      + "namespace test.inc\n"
                                      + "use aws.protocols#restJson1\n"
                                      + "use aws.protocols#awsJson1_0\n"
                                      + "use aws.auth#sigv4\n"
                                      + "@restJson1\n"
                                      + "@sigv4(name: \"svc\")\n"
                                      + "@documentation(\"Not a protocol\")\n"
                                      + "service RestService { version: \"1\" }\n"
                                      + "@awsJson1_0\n"
                                      + "service JsonService { version: \"1\" }\n"
                                      + "@documentation(\"Doc\")\n"
                                      + "@length(min: 1)\n"
                                      + "@sensitive\n"
                                      + "@pattern(\"^a\")\n"
                                      + "string Text\n";

    private static final Model MODEL = Model.assembler()
                                            .discoverModels(Model.class.getClassLoader())
                                            .addUnparsedModel("inclusion.smithy", IDL)
                                            .assemble()
                                            .unwrap();

    @Test
    void forService_includesDefaultsAndTheServicesProtocolAndAuthTraits() {
        RuntimeTraitInclusion policy = RuntimeTraitInclusion.forService(MODEL, service("RestService"));

        assertThat(policy.effectiveTraits()).contains(ShapeId.from("smithy.api#required"),
                                                      ShapeId.from("smithy.api#unitType"),
                                                      ShapeId.from("aws.protocols#restJson1"),
                                                      ShapeId.from("aws.auth#sigv4"),
                                                      ShapeId.from("smithy.api#httpHeader"),
                                                      ShapeId.from("smithy.api#httpPayload"),
                                                      ShapeId.from("smithy.api#cors"));
        assertThat(policy.includes(ShapeId.from("aws.protocols#awsJson1_0"))).isFalse();
        assertThat(policy.includes(ShapeId.from("smithy.api#documentation"))).isFalse();
        assertThat(policy.includes(ShapeId.from("smithy.api#length"))).isFalse();
    }

    @Test
    void forServices_noService_includesTheDefaultsOnly() {
        RuntimeTraitInclusion policy = RuntimeTraitInclusion.forServices(MODEL, Collections.emptyList());

        assertThat(policy.effectiveTraits().stream().map(ShapeId::toString).collect(Collectors.toList()))
            .containsExactlyElementsOf(DefaultTraitInclusion.DEFAULT_TRAITS);
    }

    @Test
    void forServices_twoServices_includesTheProtocolTraitsOfBoth() {
        RuntimeTraitInclusion policy = RuntimeTraitInclusion.forServices(MODEL, MODEL.getServiceShapes());

        assertThat(policy.effectiveTraits()).contains(ShapeId.from("aws.protocols#restJson1"),
                                                      ShapeId.from("aws.protocols#awsJson1_0"));
    }

    @Test
    void includedTraits_keepsIncludedTraitsSortedById() {
        RuntimeTraitInclusion policy = RuntimeTraitInclusion.forService(MODEL, service("RestService"));
        List<String> traits = policy.includedTraits(MODEL.expectShape(ShapeId.from("test.inc#Text")))
                                    .stream().map(t -> t.toShapeId().toString()).collect(Collectors.toList());

        assertThat(traits).containsExactly("smithy.api#sensitive");
    }

    @Test
    void isHardExcluded_coversDocumentationTraitsAndTheC2jNamespace() {
        assertThat(RuntimeTraitInclusion.isHardExcluded(ShapeId.from("smithy.api#documentation"))).isTrue();
        assertThat(RuntimeTraitInclusion.isHardExcluded(ShapeId.from("smithy.api#tags"))).isTrue();
        assertThat(RuntimeTraitInclusion.isHardExcluded(ShapeId.from("com.amazonaws.c2j#anything"))).isTrue();
        assertThat(RuntimeTraitInclusion.isHardExcluded(ShapeId.from("smithy.api#length"))).isFalse();
        assertThat(RuntimeTraitInclusion.isHardExcluded(ShapeId.from("smithy.api#required"))).isFalse();
    }

    private static ServiceShape service(String name) {
        return MODEL.expectShape(ShapeId.fromParts("test.inc", name), ServiceShape.class);
    }
}
