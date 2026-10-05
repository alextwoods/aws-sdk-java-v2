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

package software.amazon.awssdk.awscodegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.knowledge.PaginationInfo;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Render plan of a service's paginators (Requirements 7.8, 8.6): one {@code <Operation>Iterable} and, with an async
 * client, one {@code <Operation>Publisher} per {@code smithy.api#paginated} operation, in the service's
 * {@code paginators} package. Tokens, items and page sizes come from the canonical pagination trait (with the
 * service-level defaults) through Smithy's {@link PaginatedIndex}.
 *
 * @param syncClient the sync client interface
 * @param asyncClient the async client interface, or {@code null}
 * @param paginators one entry per paginated operation, sorted by absolute shape ID
 */
record PaginatorPlan(Symbol syncClient, Symbol asyncClient, List<Paginator> paginators) {

    /** Methods of {@code SdkIterable} that an items method must not override. */
    private static final Set<String> ITERABLE_METHODS = Set.of("iterator", "stream", "forEach", "spliterator",
                                                               "flatMapIterable", "pageItems", "nextToken");

    PaginatorPlan {
        paginators = List.copyOf(paginators);
    }

    /**
     * One paginated operation.
     *
     * @param operationName contextual operation name
     * @param operationMethod the client's operation method
     * @param input the input type
     * @param output the output type
     * @param iterable the sync paginator type
     * @param publisher the async paginator type, or {@code null}
     * @param inputTokenSetter the input builder's token setter
     * @param outputToken the getter path to the output token
     * @param tokenType the token's type
     * @param items the getter path to the items list, or empty when no items are modeled
     * @param itemType the items' element type, or {@code null}
     * @param itemsMethod the paginator's items method, or {@code null}
     */
    record Paginator(String operationName, String operationMethod, Symbol input, Symbol output, Symbol iterable,
                     Symbol publisher, String inputTokenSetter, List<Step> outputToken, Symbol tokenType,
                     List<Step> items, Symbol itemType, String itemsMethod) {
        Paginator {
            outputToken = List.copyOf(outputToken);
            items = List.copyOf(items);
        }
    }

    /**
     * One getter of a member path.
     *
     * @param getter the getter
     * @param type the getter's result type
     */
    record Step(String getter, Symbol type) {
    }

    static PaginatorPlan of(AwsCodegenContext context, ClientPlan client) {
        Model model = context.model();
        ServiceShape service = context.service();
        PaginatedIndex index = PaginatedIndex.of(model);
        List<Paginator> paginators = new ArrayList<>();
        for (OperationShape operation : CodegenOrdering.byShapeId(
            TopDownIndex.of(model).getContainedOperations(service))) {
            Optional<PaginationInfo> info = index.getPaginationInfo(service, operation);
            if (info.isEmpty() || info.get().getInputTokenMember() == null) {
                continue;
            }
            paginators.add(paginator(context, client, operation, info.get()));
        }
        return new PaginatorPlan(client.syncClient(), client.asyncClient(), paginators);
    }

    /** The sync paginator type of {@code operationName}. */
    static Symbol iterableType(AwsCodegenSettings settings, String operationName) {
        return type(settings, operationName + "Iterable");
    }

    /** The async paginator type of {@code operationName}. */
    static Symbol publisherType(AwsCodegenSettings settings, String operationName) {
        return type(settings, operationName + "Publisher");
    }

    /** Whether {@code operation} gets paginators. */
    static boolean paginated(AwsCodegenContext context, OperationShape operation) {
        return PaginatedIndex.of(context.model()).getPaginationInfo(context.service(), operation)
                             .map(info -> info.getInputTokenMember() != null).orElse(false);
    }

    private static Paginator paginator(AwsCodegenContext context, ClientPlan client, OperationShape operation,
                                       PaginationInfo info) {
        SymbolProvider symbols = context.symbolProvider();
        String name = operation.getId().getName(context.service());
        List<Step> token = path(context, info.getOutputTokenMemberPath());
        List<MemberShape> itemPath = info.getItemsMemberPath();
        List<Step> items = List.of();
        Symbol itemType = null;
        String itemsMethod = null;
        if (!itemPath.isEmpty()) {
            Shape target = context.model().expectShape(itemPath.get(itemPath.size() - 1).getTarget());
            // Items of collection elements (DynamoDB's list of item maps) need parameterized element symbols, which
            // the plan does not carry yet; their pages are still paginated.
            if (target instanceof ListShape list
                && !(context.model().expectShape(list.getMember().getTarget()) instanceof CollectionShape)
                && !context.model().expectShape(list.getMember().getTarget()).isMapShape()) {
                items = path(context, itemPath);
                itemType = symbols.toSymbol(context.model().expectShape(list.getMember().getTarget()));
                itemsMethod = symbols.toMemberName(itemPath.get(itemPath.size() - 1));
                if (ITERABLE_METHODS.contains(itemsMethod)) {
                    itemsMethod = itemsMethod + "Items";
                }
            }
        }
        return new Paginator(name, ClientPlan.methodName(name),
                             symbols.toSymbol(info.getInput()), symbols.toSymbol(info.getOutput()),
                             iterableType(context.settings(), name),
                             client.asyncClient() == null ? null : publisherType(context.settings(), name),
                             symbols.toMemberName(info.getInputTokenMember()), token,
                             token.get(token.size() - 1).type(), items, itemType, itemsMethod);
    }

    private static List<Step> path(AwsCodegenContext context, List<MemberShape> members) {
        List<Step> steps = new ArrayList<>();
        for (MemberShape member : members) {
            steps.add(new Step(context.symbolProvider().toMemberName(member),
                               context.symbolProvider().toSymbol(context.model().expectShape(member.getTarget()))));
        }
        return steps;
    }

    private static Symbol type(AwsCodegenSettings settings, String name) {
        String pkg = settings.packageName() + ".paginators";
        return Symbol.builder().namespace(pkg, ".").name(name)
                     .definitionFile(pkg.replace('.', '/') + "/" + name + ".java").build();
    }
}
