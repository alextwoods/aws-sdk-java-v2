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

package software.amazon.awssdk.codegen.c2j2smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.expectOnlyService;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.gapOccurrences;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.marker;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.member;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * C2J forms that codegen's C2J route accepts and the C2J test models of {@code test/codegen-generated-classes-test}
 * and {@code test/protocol-tests} use, which the converter used to reject or translate into an invalid Smithy model
 * (checkpoint 6).
 */
class LegacyC2jFormsConversionTest {
    private static final String NS = "com.amazonaws.x";
    private static final String REST_JSON = "{'metadata': {'serviceId': 'X', 'protocol': 'rest-json', "
                                            + "'apiVersion': '2020-01-01', 'endpointPrefix': 'x'}, ";

    // ----- bigdecimal -----

    @Test
    void lowercaseBigNumberTypes_becomeBigDecimalAndBigIntegerShapes() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'shapes': {'D': {'type': 'bigdecimal'}, "
                                                              + "'I': {'type': 'biginteger'}}}");

        assertThat(result.model().expectShape(ShapeId.from(NS + "#D")).getType()).isEqualTo(ShapeType.BIG_DECIMAL);
        assertThat(result.model().expectShape(ShapeId.from(NS + "#I")).getType()).isEqualTo(ShapeType.BIG_INTEGER);
    }

    // ----- stringArray -----

    @Test
    void ruleSetParameterTypes_stringArrayInAnyCase_isWrittenAsStringArray() {
        ConversionResult result = TestModels.convertJson(
            "x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}}",
            "{'version': '1.0', 'parameters': {"
            + "'Names': {'type': 'StringArray', 'required': false, 'documentation': 'Names.'}, "
            + "'Region': {'type': 'String', 'required': false, 'documentation': 'Region.'}}, 'rules': []}");

        Node parameters = expectOnlyService(result.model()).expectTrait(EndpointRuleSetTrait.class).getRuleSet()
                                                           .expectObjectNode().expectObjectMember("parameters");
        assertThat(parameters.expectObjectNode().expectObjectMember("Names").expectStringMember("type").getValue())
            .isEqualTo("stringArray");
        // Smithy reads string and boolean ignoring case; they are copied unchanged.
        assertThat(parameters.expectObjectNode().expectObjectMember("Region").expectStringMember("type").getValue())
            .isEqualTo("String");
    }

    // ----- empty endpoint tests -----

    @Test
    void endpointTests_withNoTestCases_emitNoTrait() {
        ConversionResult result = C2jToSmithyConverter.convert(
            C2jInput.builder()
                    .serviceName("x")
                    .serviceModel(TestModels.parse("{'metadata': {'serviceId': 'X', 'protocol': 'json'}}"))
                    .endpointTestsModel(TestModels.parse("{'version': '1.0', 'testCases': []}"))
                    .build());

        assertThat(expectOnlyService(result.model()).hasTrait(EndpointTestsTrait.class)).isFalse();
    }

    // ----- query string maps -----

    @Test
    void querystringMap_becomesHttpQueryParams() {
        ConversionResult result = TestModels.convertJson("x", REST_JSON
                                                              + "'operations': {'Op': {'http': {'method': 'GET', "
                                                              + "'requestUri': '/op'}, 'input': {'shape': 'In'}}, "
                                                              + "'Other': {'http': {'method': 'GET', "
                                                              + "'requestUri': '/other'}, 'input': {'shape': 'In2'}}}, "
                                                              + "'shapes': {'In': {'type': 'structure', 'members': {"
                                                              + "'Params': {'shape': 'M', 'location': 'querystring'}, "
                                                              + "'One': {'shape': 'S', 'location': 'querystring', "
                                                              + "'locationName': 'one'}}}, "
                                                              + "'In2': {'type': 'structure', 'members': {"
                                                              + "'Named': {'shape': 'M', 'location': 'querystring', "
                                                              + "'locationName': 'named'}}}, "
                                                              + "'M': {'type': 'map', 'key': {'shape': 'S'}, "
                                                              + "'value': {'shape': 'S'}}, 'S': {'type': 'string'}}}");

        MemberShape params = member(result.model(), NS, "In", "Params");
        assertThat(params.hasTrait(HttpQueryParamsTrait.class)).isTrue();
        assertThat(params.hasTrait(HttpQueryTrait.class)).isFalse();
        assertThat(member(result.model(), NS, "In2", "Named").hasTrait(HttpQueryParamsTrait.class)).isTrue();
        assertThat(gapOccurrences(result)).containsEntry("member.locationName(querystring map)", 1);
        assertThat(member(result.model(), NS, "In", "One").expectTrait(HttpQueryTrait.class).getValue())
            .isEqualTo("one");
        assertThat(ConvertAndAssembleTest.errorEvents(result)).isEmpty();
    }

    // ----- member-level streaming -----

    @Test
    void memberLevelStreaming_onlyStreamingReferences_makesTheBlobStreaming() {
        ConversionResult result = TestModels.convertJson("x", REST_JSON
                                                              + "'operations': {'Put': {'http': {'method': 'PUT', "
                                                              + "'requestUri': '/'}, 'input': {'shape': 'In'}}}, "
                                                              + "'shapes': {'In': {'type': 'structure', 'members': {"
                                                              + "'Body': {'shape': 'Body', 'streaming': true, "
                                                              + "'requiresLength': true}}, 'payload': 'Body'}, "
                                                              + "'Body': {'type': 'blob'}}}");

        Shape body = result.model().expectShape(ShapeId.from(NS + "#Body"));
        assertThat(body.hasTrait(StreamingTrait.class)).isTrue();
        assertThat(body.hasTrait(RequiresLengthTrait.class)).isTrue();
        MemberShape member = member(result.model(), NS, "In", "Body");
        assertThat(member.hasTrait(StreamingTrait.class)).isFalse();
        assertThat(member.hasTrait(RequiresLengthTrait.class)).isFalse();
        assertThat(member.expectTrait(DefaultTrait.class).toNode()).isEqualTo(Node.from(""));
        assertThat(ConvertAndAssembleTest.errorEvents(result)).isEmpty();
    }

    @Test
    void memberLevelStreaming_blobAlsoUsedWithoutTheFlag_targetsAStreamingCopy() {
        ConversionResult result = TestModels.convertJson("x", REST_JSON
                                                              + "'operations': {'Put': {'http': {'method': 'PUT', "
                                                              + "'requestUri': '/put'}, 'input': {'shape': 'In'}}, "
                                                              + "'Get': {'http': {'method': 'POST', "
                                                              + "'requestUri': '/get'}, 'input': {'shape': 'Other'}}}, "
                                                              + "'shapes': {'In': {'type': 'structure', 'required': "
                                                              + "['Body'], 'members': {'Body': {'shape': 'Body', "
                                                              + "'streaming': true}}, 'payload': 'Body'}, "
                                                              + "'Other': {'type': 'structure', 'members': {"
                                                              + "'Data': {'shape': 'Body'}}}, "
                                                              + "'Body': {'type': 'blob'}}}");

        Model model = result.model();
        assertThat(model.expectShape(ShapeId.from(NS + "#Body")).hasTrait(StreamingTrait.class)).isFalse();
        Shape copy = model.expectShape(ShapeId.from(NS + "#Body" + C2jToSmithyConverter.STREAMING_SUFFIX));
        assertThat(copy.hasTrait(StreamingTrait.class)).isTrue();
        assertThat(marker(copy, C2jMarkerTrait.SHAPE_NAME)).isEqualTo(Node.from("Body"));
        MemberShape body = member(model, NS, "In", "Body");
        assertThat(body.getTarget()).isEqualTo(copy.getId());
        // A required streaming member needs no default.
        assertThat(body.hasTrait(DefaultTrait.class)).isFalse();
        assertThat(member(model, NS, "Other", "Data").getTarget()).isEqualTo(ShapeId.from(NS + "#Body"));
        assertThat(ConvertAndAssembleTest.errorEvents(result)).isEmpty();
    }

    @Test
    void streamingBlobMember_notRequired_getsAnEmptyDefault() {
        ConversionResult result = TestModels.convertJson("x", REST_JSON
                                                              + "'operations': {'Put': {'http': {'method': 'PUT', "
                                                              + "'requestUri': '/'}, 'input': {'shape': 'In'}}}, "
                                                              + "'shapes': {'In': {'type': 'structure', 'members': {"
                                                              + "'Body': {'shape': 'Stream'}}, 'payload': 'Body'}, "
                                                              + "'Stream': {'type': 'blob', 'streaming': true}}}");

        assertThat(member(result.model(), NS, "In", "Body").expectTrait(DefaultTrait.class).toNode())
            .isEqualTo(Node.from(""));
        assertThat(ConvertAndAssembleTest.errorEvents(result)).isEmpty();
    }
}
