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

package software.amazon.awssdk.codegen.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.model.service.Waiters;

class C2jSidecarSupplementTest {
    private static final String PAGINATORS =
        "{\"pagination\": {"
        + "\"Same\": {\"input_token\": \"NextToken\", \"output_token\": \"NextToken\", \"result_key\": \"Items\"},"
        + "\"MoreResults\": {\"input_token\": \"Marker\", \"output_token\": \"NextMarker\","
        + " \"more_results\": \"IsTruncated\"},"
        + "\"TwoTokens\": {\"input_token\": [\"A\", \"B\"], \"output_token\": [\"NextA\", \"NextB\"]},"
        + "\"Deprecated\": {\"input_token\": \"NextToken\", \"output_token\": \"NextToken\"}"
        + "}}";

    private static final String WAITERS =
        "{\"version\": 2, \"waiters\": {"
        + "\"Ready\": {\"operation\": \"Same\", \"delay\": 5, \"maxAttempts\": 25, \"acceptors\": ["
        + "{\"state\": \"success\", \"matcher\": \"status\", \"expected\": 200}]},"
        + "\"Gone\": {\"operation\": \"Same\", \"delay\": 5, \"maxAttempts\": 40, \"acceptors\": ["
        + "{\"state\": \"success\", \"matcher\": \"status\", \"expected\": 200}]}"
        + "}}";

    @Test
    void sidecarDefinitions_replaceOnlyTheOnesTheTraitsCouldNotExpress() throws IOException {
        Paginators paginators = Jackson.load(Paginators.class, PAGINATORS);
        Waiters waiters = Jackson.load(Waiters.class, WAITERS);

        Map<String, OperationModel> operations = new HashMap<>();
        for (String name : Arrays.asList("Same", "MoreResults", "TwoTokens")) {
            OperationModel operation = new OperationModel();
            operation.setOperationName(name);
            operations.put(name, operation);
        }
        IntermediateModel model = new IntermediateModel(null, operations, new HashMap<>(), null);
        PaginatorDefinition same = paginator("NextToken", "NextToken", "Items");
        Map<String, PaginatorDefinition> fromTraits = new LinkedHashMap<>();
        fromTraits.put("Same", same);
        fromTraits.put("MoreResults", paginator("Marker", "NextMarker", null));
        model.setPaginators(fromTraits);
        Map<String, WaiterDefinition> waitersFromTraits = new LinkedHashMap<>();
        WaiterDefinition ready = waiters.getWaiters().get("Ready");
        WaiterDefinition derivedReady = new WaiterDefinition();
        derivedReady.setOperation("Same");
        derivedReady.setDelay(5);
        derivedReady.setMaxAttempts(25);
        derivedReady.setAcceptors(ready.getAcceptors());
        waitersFromTraits.put("Ready", derivedReady);
        model.setWaiters(waitersFromTraits);

        C2jSidecarSupplement supplement = C2jSidecarSupplement.apply(model, paginators, waiters);

        // The deprecated (removed) operation keeps no paginator; the order is the sidecar file's.
        assertThat(model.getPaginators()).containsOnlyKeys("Same", "MoreResults", "TwoTokens");
        assertThat(model.getPaginators().get("Same")).isSameAs(same);
        assertThat(model.getPaginators().get("MoreResults").getMoreResults()).isEqualTo("IsTruncated");
        assertThat(model.getPaginators().get("TwoTokens").getInputToken()).containsExactly("A", "B");
        assertThat(supplement.supplementedPaginators()).containsExactly("MoreResults", "TwoTokens");
        assertThat(operations.get("Same").isPaginated()).isTrue();
        assertThat(operations.get("TwoTokens").isPaginated()).isTrue();

        assertThat(model.getWaiters().get("Ready")).isSameAs(derivedReady);
        assertThat(model.getWaiters().get("Gone").getMaxAttempts()).isEqualTo(40);
        assertThat(supplement.supplementedWaiters()).containsExactly("Gone");
    }

    private static PaginatorDefinition paginator(String inputToken, String outputToken, String resultKey) {
        PaginatorDefinition definition = new PaginatorDefinition();
        definition.setInputToken(Collections.singletonList(inputToken));
        definition.setOutputToken(Collections.singletonList(outputToken));
        if (resultKey != null) {
            definition.setResultKey(Collections.singletonList(resultKey));
        }
        return definition;
    }
}
