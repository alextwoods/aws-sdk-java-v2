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

import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsString;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The paginator and waiter views come from {@code @paginated} and {@code smithy.waiters#waitable}, and the
 * intermediate model keeps the Smithy model and the ShapeIds of its shapes, operations and members.
 */
class PaginatorAndWaiterTraitTranslatorTest {
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#awsJson1_0\n"
        + "use smithy.waiters#waitable\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@awsJson1_0\n"
        + "@paginated(inputToken: \"NextToken\", outputToken: \"NextToken\")\n"
        + "service DemoService { version: \"2024-01-01\", operations: [ListThings, GetThing, Plain] }\n\n"
        + "@paginated(items: \"Things\", pageSize: \"MaxResults\")\n"
        + "operation ListThings { input: ListThingsRequest, output: ListThingsResponse }\n"
        + "structure ListThingsRequest { NextToken: String, MaxResults: Integer }\n"
        + "structure ListThingsResponse { NextToken: String, Things: ThingList }\n"
        + "list ThingList { member: String }\n\n"
        + "@waitable(ThingReady: {\n"
        + "  minDelay: 5\n"
        + "  acceptors: [\n"
        + "    { state: \"success\", matcher: { output: { path: \"Status\", expected: \"READY\", comparator: "
        + "\"stringEquals\" } } }\n"
        + "    { state: \"failure\", matcher: { output: { path: \"Done\", expected: \"false\", comparator: "
        + "\"booleanEquals\" } } }\n"
        + "    { state: \"retry\", matcher: { errorType: \"NotFound\" } }\n"
        + "    { state: \"success\", matcher: { success: true } }\n"
        + "    { state: \"failure\", matcher: { success: false } }\n"
        + "  ]\n"
        + "})\n"
        + "operation GetThing { input: GetThingRequest, output: GetThingResponse, errors: [NotFound] }\n"
        + "structure GetThingRequest {}\n"
        + "structure GetThingResponse { Status: String, Done: Boolean }\n"
        + "@error(\"client\")\n"
        + "structure NotFound { message: String }\n\n"
        + "operation Plain { input: PlainRequest, output: PlainResponse }\n"
        + "structure PlainRequest {}\n"
        + "structure PlainResponse {}\n";

    private static IntermediateModel build() {
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", MODEL)
                           .assemble()
                           .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(model)
                                                              .customizationConfig(CustomizationConfig.create())
                                                              .build())
            .build();
    }

    @Test
    void paginated_mergesServiceDefaultsIntoTheOperationPaginator() {
        IntermediateModel model = build();

        assertThat(model.getPaginators()).containsOnlyKeys("ListThings");
        PaginatorDefinition paginator = model.getPaginators().get("ListThings");
        assertThat(paginator.getInputToken()).containsExactly("NextToken");
        assertThat(paginator.getOutputToken()).containsExactly("NextToken");
        assertThat(paginator.getResultKey()).containsExactly("Things");
        assertThat(paginator.getLimitKey()).isEqualTo("MaxResults");
        assertThat(paginator.getMoreResults()).isNull();
        assertThat(model.getOperation("ListThings").isPaginated()).isTrue();
        assertThat(model.getOperation("GetThing").isPaginated()).isFalse();
    }

    @Test
    void waitable_mapsEveryAcceptorToTheC2jMatchers() {
        IntermediateModel model = build();

        assertThat(model.getWaiters()).containsOnlyKeys("ThingReady");
        WaiterDefinition waiter = model.getWaiters().get("ThingReady");
        assertThat(waiter.getOperation()).isEqualTo("GetThing");
        assertThat(waiter.getDelay()).isEqualTo(5);
        assertThat(waiter.getMaxAttempts()).isEqualTo(WaiterTraitTranslator.DEFAULT_MAX_ATTEMPTS);

        List<Acceptor> acceptors = waiter.getAcceptors();
        assertThat(acceptors).hasSize(5);
        assertAcceptor(acceptors.get(0), "success", "path", "Status");
        assertThat(acceptors.get(0).getExpected()).isInstanceOf(JrsString.class);
        assertThat(acceptors.get(0).getExpected().asText()).isEqualTo("READY");
        assertAcceptor(acceptors.get(1), "failure", "path", "Done");
        assertThat(acceptors.get(1).getExpected()).isEqualTo(JrsBoolean.FALSE);
        assertAcceptor(acceptors.get(2), "retry", "error", null);
        assertThat(acceptors.get(2).getExpected().asText()).isEqualTo("NotFound");
        assertAcceptor(acceptors.get(3), "success", "status", null);
        assertThat(acceptors.get(3).getExpected()).isInstanceOf(JrsNumber.class);
        assertThat(acceptors.get(3).getExpected().asText()).isEqualTo("200");
        assertAcceptor(acceptors.get(4), "failure", "error", null);
        assertThat(acceptors.get(4).getExpected()).isEqualTo(JrsBoolean.TRUE);
    }

    @Test
    void intermediateModel_keepsTheSmithyModelAndShapeIds() {
        IntermediateModel model = build();

        assertThat(model.smithyModel()).isNotNull();
        assertThat(model.serviceId()).isEqualTo(ShapeId.from("demo#DemoService"));
        assertThat(model.getOperation("ListThings").operationId()).isEqualTo(ShapeId.from("demo#ListThings"));
        assertThat(model.getShape(ShapeId.from("demo#ListThingsResponse")).getShapeName())
            .isEqualTo("ListThingsResponse");
        assertThat(model.getShapes().get("ListThingsResponse").findMemberModelByC2jName("Things").memberId())
            .isEqualTo(ShapeId.from("demo#ListThingsResponse$Things"));
        assertThat(model.getShape(ShapeId.from("demo#Missing"))).isNull();
    }

    private static void assertAcceptor(Acceptor acceptor, String state, String matcher, String argument) {
        assertThat(acceptor.getState()).isEqualTo(state);
        assertThat(acceptor.getMatcher()).isEqualTo(matcher);
        assertThat(acceptor.getArgument()).isEqualTo(argument);
    }
}
