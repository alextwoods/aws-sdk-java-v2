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

import com.fasterxml.jackson.jr.stree.JrsValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.smithy.PaginatorTraitTranslator;

/**
 * Stage 5 of the Codegen_Pipeline, for a C2J model only: supplements the paginator and waiter views that
 * {@code SmithyIntermediateModelBuilder} builds from {@code @paginated} and {@code smithy.waiters#waitable} with the
 * {@code paginators-1.json} and {@code waiters-2.json} definitions that the C2J_Converter could not express in those
 * traits.
 *
 * <p>Smithy cannot express every C2J paginator and waiter: several input tokens, a required input token, several result
 * keys, a string limit key, {@code more_results}, a status acceptor other than 200, and the waiter attempt count (see
 * the converter's gap report). Generating those from the traits alone would remove public paginator and waiter
 * methods. So, per operation (paginators) and per waiter name, the trait-derived definition is kept when it equals the
 * sidecar definition, and the sidecar definition is used otherwise. The result has the sidecar file's entries in the
 * sidecar file's order; a native Smithy model is not supplemented.
 */
@SdkInternalApi
final class C2jSidecarSupplement {
    private final List<String> supplementedPaginators;
    private final List<String> supplementedWaiters;

    private C2jSidecarSupplement(List<String> supplementedPaginators, List<String> supplementedWaiters) {
        this.supplementedPaginators = Collections.unmodifiableList(supplementedPaginators);
        this.supplementedWaiters = Collections.unmodifiableList(supplementedWaiters);
    }

    /**
     * Replaces the intermediate model's paginators and waiters as described in the class documentation.
     *
     * @return the operation names and waiter names whose sidecar definition was used
     */
    static C2jSidecarSupplement apply(IntermediateModel model, Paginators paginators, Waiters waiters) {
        List<String> supplementedPaginators = new ArrayList<>();
        Map<String, PaginatorDefinition> fromTraits = model.getPaginators();
        Map<String, PaginatorDefinition> resultPaginators = new LinkedHashMap<>();
        paginators.getPagination().forEach((operation, sidecar) -> {
            if (!model.getOperations().containsKey(operation)) {
                // Deprecated operations are removed, with their paginators.
                return;
            }
            PaginatorDefinition derived = fromTraits.get(operation);
            if (derived != null && key(derived).equals(key(sidecar))) {
                resultPaginators.put(operation, derived);
            } else {
                resultPaginators.put(operation, sidecar);
                supplementedPaginators.add(operation);
            }
        });
        model.setPaginators(resultPaginators);
        PaginatorTraitTranslator.markPaginatedOperations(model.getOperations(), resultPaginators);

        List<String> supplementedWaiters = new ArrayList<>();
        Map<String, WaiterDefinition> waitersFromTraits = model.getWaiters();
        Map<String, WaiterDefinition> resultWaiters = new LinkedHashMap<>();
        waiters.getWaiters().forEach((name, sidecar) -> {
            WaiterDefinition derived = waitersFromTraits.get(name);
            if (derived != null && key(derived).equals(key(sidecar))) {
                resultWaiters.put(name, derived);
            } else {
                resultWaiters.put(name, sidecar);
                supplementedWaiters.add(name);
            }
        });
        model.setWaiters(resultWaiters);
        return new C2jSidecarSupplement(supplementedPaginators, supplementedWaiters);
    }

    /**
     * The operations whose paginator came from {@code paginators-1.json}, in file order.
     */
    List<String> supplementedPaginators() {
        return supplementedPaginators;
    }

    /**
     * The waiters that came from {@code waiters-2.json}, in file order.
     */
    List<String> supplementedWaiters() {
        return supplementedWaiters;
    }

    private static List<Object> key(PaginatorDefinition p) {
        return Arrays.asList(p.getInputToken(), p.getOutputToken(), p.getResultKey(), p.getLimitKey(),
                                       p.getMoreResults());
    }

    private static List<Object> key(WaiterDefinition w) {
        List<Object> acceptors = w.getAcceptors() == null
                                 ? null
                                 : w.getAcceptors().stream().map(C2jSidecarSupplement::key).collect(Collectors.toList());
        return Arrays.asList(w.getOperation(), w.getDelay(), w.getMaxAttempts(), acceptors);
    }

    private static List<Object> key(Acceptor a) {
        JrsValue expected = a.getExpected();
        // The type of the expected value matters to the generators (a boolean, a string, a number).
        String expectedKey = expected == null ? null : expected.getClass().getSimpleName() + ":" + expected.asText();
        return Arrays.asList(a.getState(), a.getMatcher(), a.getArgument(), expectedKey);
    }
}
