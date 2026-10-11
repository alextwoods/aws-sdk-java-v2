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
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.gapOccurrences;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.restJsonOperation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.waiters.Acceptor;
import software.amazon.smithy.waiters.AcceptorState;
import software.amazon.smithy.waiters.WaitableTrait;
import software.amazon.smithy.waiters.Waiter;

/**
 * {@code paginators-1.json} to {@code @paginated} and {@code waiters-2.json} to {@code smithy.waiters#waitable}.
 */
class PaginatorAndWaiterConversionTest {
    private static final String SERVICE = "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                          + "'operations': {'List': {'input': {'shape': 'In'}, 'output': {'shape': "
                                          + "'Out'}}}, 'shapes': {"
                                          + "'In': {'type': 'structure', 'required': ['Req'], 'members': {'Token': "
                                          + "{'shape': 'S'}, 'Start': {'shape': 'M'}, 'Req': {'shape': 'S'}, 'Size': "
                                          + "{'shape': 'L'}, 'Text': {'shape': 'S'}}}, "
                                          + "'Out': {'type': 'structure', 'required': ['Next'], 'members': {'Next': "
                                          + "{'shape': 'S'}, 'Last': {'shape': 'M'}, 'Page': {'shape': 'Page'}, "
                                          + "'Items': {'shape': 'Items'}}}, "
                                          + "'Page': {'type': 'structure', 'members': {'Token': {'shape': 'S'}, "
                                          + "'Items': {'shape': 'Items'}}}, "
                                          + "'Items': {'type': 'list', 'member': {'shape': 'S'}}, "
                                          + "'M': {'type': 'map', 'key': {'shape': 'S'}, 'value': {'shape': 'S'}}, "
                                          + "'S': {'type': 'string'}, 'L': {'type': 'long'}}}";

    @Test
    void paginator_fullyExpressible_becomesPaginatedTrait() {
        PaginatedTrait paginated = restJsonOperation("ListThings").expectTrait(PaginatedTrait.class);

        assertThat(paginated.getInputToken()).contains("NextToken");
        assertThat(paginated.getOutputToken()).contains("NextToken");
        assertThat(paginated.getPageSize()).contains("MaxResults");
        assertThat(paginated.getItems()).contains("Things");
        assertThat(restJsonOperation("ListThings").hasTrait(SuppressTrait.class)).isFalse();
    }

    @Test
    void paginator_multipleTokens_isNotConvertedAndRecorded() {
        ConversionResult result = TestModels.convertFixture("restjson");

        assertThat(restJsonOperation("GetThing").hasTrait(PaginatedTrait.class)).isFalse();
        assertThat(gapOccurrences(result)).containsKeys("paginator.input_token(multiple)",
                                                        "paginator.output_token(multiple)");
    }

    @Test
    void paginator_outputPaths_areResolved() {
        PaginatedTrait paginated = paginate("{'input_token': 'Token', 'output_token': 'Page.Token', "
                                            + "'result_key': 'Page.Items'}").expectTrait(PaginatedTrait.class);

        assertThat(paginated.getOutputToken()).contains("Page.Token");
        assertThat(paginated.getItems()).contains("Page.Items");
    }

    @Test
    void paginator_mapTokensLongPageSizeAndRequiredOutputToken_areSuppressed() {
        OperationShape op = paginate("{'input_token': 'Start', 'output_token': 'Next', 'limit_key': 'Size'}");

        assertThat(op.hasTrait(PaginatedTrait.class)).isTrue();
        assertThat(op.expectTrait(SuppressTrait.class).getValues())
            .containsExactly("PaginatedTrait.ShouldNotBeRequired", "PaginatedTrait.WrongShapeType");
    }

    @Test
    void paginator_requiredInputToken_isNotConverted() {
        assertThat(paginate("{'input_token': 'Req', 'output_token': 'Next'}").hasTrait(PaginatedTrait.class))
            .isFalse();
    }

    @Test
    void paginator_stringLimitKeyAndMultipleResultKeys_dropOnlyThoseMembers() {
        ConversionResult result = convertWithPaginator("{'input_token': 'Token', 'output_token': 'Next', 'limit_key': "
                                                       + "'Text', 'result_key': ['Items', 'Last'], 'more_results': "
                                                       + "'Done'}");

        PaginatedTrait paginated = operation(result).expectTrait(PaginatedTrait.class);
        assertThat(paginated.getPageSize()).isEmpty();
        assertThat(paginated.getItems()).isEmpty();
        assertThat(gapOccurrences(result)).containsKeys("paginator.limit_key(unresolvable)",
                                                        "paginator.result_key(multiple)", "paginator.more_results");
    }

    @Test
    void waiters_convertibleAcceptors_becomeWaitableTrait() {
        Map<String, Waiter> waiters = restJsonOperation("GetThing").expectTrait(WaitableTrait.class).getWaiters();

        assertThat(waiters).containsOnlyKeys("ThingExists", "ThingGone");
        Waiter exists = waiters.get("ThingExists");
        assertThat(exists.getDocumentation()).contains("Waits until the thing exists.");
        assertThat(exists.getMinDelay()).isEqualTo(5);
        assertThat(exists.getAcceptors()).extracting(a -> a.getMatcher().toNode())
                                         .containsExactly(
                                             Node.parse("{\"output\": {\"path\": \"Thing.Name\", \"expected\": "
                                                        + "\"ready\", \"comparator\": \"stringEquals\"}}"),
                                             Node.parse("{\"output\": {\"path\": \"Thing.Tags\", \"expected\": "
                                                        + "\"broken\", \"comparator\": \"anyStringEquals\"}}"),
                                             Node.parse("{\"errorType\": \"ThingNotFound\"}"));
        assertThat(exists.getAcceptors()).extracting(Acceptor::getState)
                                         .containsExactly(AcceptorState.SUCCESS, AcceptorState.FAILURE,
                                                          AcceptorState.RETRY);

        Waiter gone = waiters.get("ThingGone");
        assertThat(gone.getMinDelay()).isEqualTo(300);
        assertThat(gone.getMaxDelay()).isEqualTo(300);
        assertThat(gone.getAcceptors()).extracting(a -> a.getMatcher().toNode())
                                       .containsExactly(Node.parse("{\"success\": true}"),
                                                        Node.parse("{\"success\": false}"));
    }

    @Test
    void waiters_statusOtherThan200AndMaxAttempts_areRecorded() {
        Map<String, Integer> gaps = gapOccurrences(TestModels.convertFixture("restjson"));

        assertThat(gaps).containsEntry("waiter.acceptor.matcher=status", 1);
        assertThat(gaps).containsEntry("waiter.maxAttempts", 2);
    }

    @Test
    void waiters_booleanPathExpected_usesBooleanEquals() {
        JsonNode waiters = TestModels.parse("{'version': 2, 'waiters': {'Listed': {'operation': 'List', 'delay': 1, "
                                            + "'acceptors': [{'matcher': 'path', 'argument': 'length(Items) > `0`', "
                                            + "'expected': true, 'state': 'success'}]}}}");
        Model model = C2jToSmithyConverter.convert(C2jInput.builder().serviceName("x").serviceModel(TestModels.parse(SERVICE))
                                                           .waitersModel(waiters).build()).model();

        Waiter listed = operation(model).expectTrait(WaitableTrait.class).getWaiters().get("Listed");
        assertThat(listed.getAcceptors().get(0).getMatcher().toNode().expectObjectNode().expectObjectMember("output")
                         .expectStringMember("comparator").getValue()).isEqualTo("booleanEquals");
    }

    private static OperationShape paginate(String paginator) {
        return operation(convertWithPaginator(paginator));
    }

    private static ConversionResult convertWithPaginator(String paginator) {
        JsonNode paginators = TestModels.parse("{'pagination': {'List': " + paginator + "}}");
        return C2jToSmithyConverter.convert(C2jInput.builder().serviceName("x").serviceModel(TestModels.parse(SERVICE))
                                                    .paginatorsModel(paginators).build());
    }

    private static OperationShape operation(ConversionResult result) {
        return operation(result.model());
    }

    private static OperationShape operation(Model model) {
        return model.expectShape(ShapeId.from("com.amazonaws.x#List"), OperationShape.class);
    }
}
