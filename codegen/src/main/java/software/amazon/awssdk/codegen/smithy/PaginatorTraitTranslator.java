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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Builds the intermediate model's paginator view from {@code @paginated}. Smithy counterpart to reading
 * {@code paginators-1.json}.
 *
 * <p>The service's {@code @paginated} defaults are merged into each operation's trait ({@link PaginatedIndex}). Only
 * operations with a complete trait (input and output token) get a paginator; the mapping is
 * {@code inputToken} to {@code input_token}, {@code outputToken} to {@code output_token}, {@code items} to
 * {@code result_key} and {@code pageSize} to {@code limit_key}. {@code @paginated} has no {@code more_results}, so it
 * is never set.
 */
public final class PaginatorTraitTranslator {

    private PaginatorTraitTranslator() {
    }

    /**
     * The paginator definitions of the service's operations, keyed by operation name, in operation name order.
     */
    public static Map<String, PaginatorDefinition> paginators(Model model, ServiceShape service) {
        PaginatedIndex index = PaginatedIndex.of(model);
        Map<String, PaginatorDefinition> paginators = new TreeMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            if (!operation.hasTrait(PaginatedTrait.class)) {
                continue;
            }
            index.getPaginationInfo(service, operation).ifPresent(info -> {
                PaginatedTrait trait = info.getPaginatedTrait();
                if (!trait.getInputToken().isPresent() || !trait.getOutputToken().isPresent()) {
                    return;
                }
                PaginatorDefinition definition = new PaginatorDefinition();
                definition.setInputToken(Collections.singletonList(trait.getInputToken().get()));
                definition.setOutputToken(Collections.singletonList(trait.getOutputToken().get()));
                trait.getItems().ifPresent(items -> definition.setResultKey(Collections.singletonList(items)));
                trait.getPageSize().ifPresent(definition::setLimitKey);
                paginators.put(operation.getId().getName(), definition);
            });
        }
        return new LinkedHashMap<>(paginators);
    }

    /**
     * Sets {@link OperationModel#isPaginated()} on every operation: true when it has a paginator with the fields
     * paginator generation needs, as the C2J route decides.
     */
    public static void markPaginatedOperations(Map<String, OperationModel> operations,
                                               Map<String, PaginatorDefinition> paginators) {
        operations.forEach((name, operation) -> {
            PaginatorDefinition paginator = paginators.get(name);
            operation.setPaginated(paginator != null && paginator.hasAllRequiredFields());
        });
    }
}
