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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.SourceVersion;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Render plan of a service's generated clients (Requirements 7.1-7.7, 7.11, 7.12, 8.1-8.3, 8.13, 17.1): the public
 * {@code <Service>Client} and {@code <Service>AsyncClient} interfaces and their builder interfaces in the service's
 * root package, and, package-private beside them, their default implementations and builders and the
 * {@code <Service>OperationBindings} holder of one static typed pipeline binding per operation.
 *
 * <p>The plan is computed from the model and symbols alone; which V2 surfaces are generated is decided by its
 * {@link ClientSurfaceClassifier} input.
 *
 * @param service absolute shape ID of the service
 * @param serviceName the V2-compatible {@code SERVICE_NAME} (the endpoint prefix)
 * @param syncClient the sync client interface
 * @param syncBuilder the sync client builder interface
 * @param defaultSyncClient the sync client implementation
 * @param defaultSyncBuilder the sync client builder implementation
 * @param asyncClient the async client interface, or {@code null} when the service has none
 * @param asyncBuilder the async client builder interface, or {@code null}
 * @param defaultAsyncClient the async client implementation, or {@code null}
 * @param defaultAsyncBuilder the async client builder implementation, or {@code null}
 * @param bindings the holder of the operations' typed pipeline bindings
 * @param operationTable the service's static operation table
 * @param serviceMetadata the service's metadata class
 * @param serviceException the service's base exception
 * @param endpointRules the service's endpoint rules class, or {@code null} when it has no rule set
 * @param operations one entry per operation, sorted by absolute shape ID
 * @param syncWaiter the sync waiter interface, or {@code null} when the service has no generated waiter
 * @param asyncWaiter the async waiter interface, or {@code null}
 * @param utilities the service utility entry point the sync client's {@code utilities()} returns, or {@code null}
 *     when the classifier gives the service none (or none this generator implements)
 */
record ClientPlan(ShapeId service, String serviceName, Symbol syncClient, Symbol syncBuilder,
                  Symbol defaultSyncClient, Symbol defaultSyncBuilder, Symbol asyncClient, Symbol asyncBuilder,
                  Symbol defaultAsyncClient, Symbol defaultAsyncBuilder, Symbol bindings, Symbol operationTable,
                  Symbol serviceMetadata, Symbol serviceException, Symbol endpointRules, List<Operation> operations,
                  Symbol syncWaiter, Symbol asyncWaiter, Symbol utilities) {

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");
    private static final ShapeId HTTP_CHECKSUM = ShapeId.from("aws.protocols#httpChecksum");
    /** Methods every generated client declares or inherits, which an operation method must not overload. */
    private static final Set<String> RESERVED_METHODS = Set.of(
        "builder", "create", "close", "serviceName", "waiter", "utilities", "serviceClientConfiguration", "clone",
        "equals",
        "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait");

    ClientPlan {
        operations = List.copyOf(operations);
    }

    /**
     * One client operation.
     *
     * @param id absolute shape ID
     * @param name contextual (service-renamed) operation name
     * @param method Java method name, as V2 names it ({@code GetItem} becomes {@code getItem})
     * @param constant the operation-table, endpoint-binding and operation-binding constant
     * @param input the input type ({@code Unit} when the operation has no input)
     * @param output the output type ({@code Unit} when the operation has no output)
     * @param unitInput whether the operation has no input
     * @param unitOutput whether the operation has no output
     * @param consumerOverload whether the request-builder consumer overloads are generated
     * @param noArgumentOverload whether the no-argument overload is generated
     * @param streamingInput whether the input has a streaming blob member
     * @param streamingOutput whether the output has a streaming blob member
     * @param errors the operation's modeled errors
     * @param auth the operation's modeled auth
     * @param httpChecksum the operation's {@code aws.protocols#httpChecksum} trait value, or {@code null}
     * @param syncPaginator the sync paginator type, or {@code null} when the operation is not paginated
     * @param asyncPaginator the async paginator type, or {@code null}
     * @param inputEvent the event union of the input's event stream member, or {@code null} when it has none
     * @param inputEventMember the modeled name of the input's event stream member, or {@code null}
     * @param outputEvent the event union of the output's event stream member, or {@code null} when it has none
     * @param responseHandler the operation's {@code <Operation>ResponseHandler} in the model package (as in V2),
     *     or {@code null} when the output has no event stream
     */
    record Operation(ShapeId id, String name, String method, String constant, Symbol input, Symbol output,
                     boolean unitInput, boolean unitOutput, boolean consumerOverload, boolean noArgumentOverload,
                     boolean streamingInput, boolean streamingOutput, List<Symbol> errors, Auth auth,
                     HttpChecksum httpChecksum, Symbol syncPaginator, Symbol asyncPaginator, Symbol inputEvent,
                     String inputEventMember, Symbol outputEvent, Symbol responseHandler) {
        Operation {
            errors = List.copyOf(errors);
        }

        /**
         * Whether the input or the output has an event stream: as in V2, such an operation has only async methods,
         * which take the input events as a {@code Publisher} and hand the output events to the response handler.
         */
        boolean eventStream() {
            return inputEvent != null || outputEvent != null;
        }
    }

    /**
     * The modeled auth of one operation, rendered as a static {@code ModeledAuth}.
     *
     * @param serviceSchemes the service's auth-definition traits with their string member values, by scheme ID
     * @param serviceAuthTrait the service's {@code @auth} value, or {@code null}
     * @param operationAuthTrait the operation's {@code @auth} value, or {@code null}
     * @param optionalAuth whether the operation has {@code @optionalAuth}
     * @param unsignedPayload whether the operation has {@code @aws.auth#unsignedPayload}
     */
    record Auth(Map<String, Map<String, String>> serviceSchemes, List<String> serviceAuthTrait,
                List<String> operationAuthTrait, boolean optionalAuth, boolean unsignedPayload) {
    }

    /**
     * The {@code aws.protocols#httpChecksum} trait value of one operation, rendered as a static
     * {@code HttpChecksumMetadata} equal to the one the runtime reads from the trait's node value.
     *
     * @param requestChecksumRequired the {@code requestChecksumRequired} value ({@code false} when absent)
     * @param requestAlgorithmMember the {@code requestAlgorithmMember} value, or {@code null}
     * @param requestValidationModeMember the {@code requestValidationModeMember} value, or {@code null}
     * @param responseAlgorithms the {@code responseAlgorithms} values, in modeled order
     */
    record HttpChecksum(boolean requestChecksumRequired, String requestAlgorithmMember,
                        String requestValidationModeMember, List<String> responseAlgorithms) {
        HttpChecksum {
            responseAlgorithms = List.copyOf(responseAlgorithms);
        }

        /** The value of {@code operation}'s trait, or {@code null} if it has none. */
        static HttpChecksum of(OperationShape operation) {
            return operation.findTrait(HTTP_CHECKSUM).map(trait -> {
                ObjectNode node = trait.toNode().expectObjectNode();
                return new HttpChecksum(
                    node.getBooleanMemberOrDefault("requestChecksumRequired", false),
                    node.getStringMember("requestAlgorithmMember").map(StringNode::getValue).orElse(null),
                    node.getStringMember("requestValidationModeMember").map(StringNode::getValue).orElse(null),
                    node.getArrayMember("responseAlgorithms")
                        .map(array -> array.getElements().stream().map(e -> e.expectStringNode().getValue()).toList())
                        .orElse(List.of()));
            }).orElse(null);
        }
    }

    static ClientPlan of(AwsCodegenContext context, ClientSurfaceClassifier classifier) {
        Model model = context.model();
        ServiceShape service = context.service();
        AwsCodegenSettings settings = context.settings();
        String type = AwsSymbolProvider.serviceTypeName(service);
        String pkg = settings.packageName();
        boolean async = classifier.asyncClient(service);
        Map<String, Map<String, String>> schemes = new TreeMap<>();
        ServiceIndex.of(model).getAuthSchemes(service).forEach((id, trait) -> schemes.put(id.toString(),
                                                                                         stringMembers(trait)));
        List<String> serviceAuth = service.getTrait(AuthTrait.class).map(t -> ids(t)).orElse(null);
        List<Operation> operations = new ArrayList<>();
        for (OperationShape operation : CodegenOrdering.byShapeId(
            TopDownIndex.of(model).getContainedOperations(service))) {
            operations.add(operation(context, classifier, operation, schemes, serviceAuth, async));
        }
        Symbol rules = service.hasTrait(EndpointRuleSetTrait.class) ? EndpointRulesGenerator.rulesSymbol(context)
                                                                     : null;
        return new ClientPlan(
            service.getId(), serviceName(service, type),
            type(pkg, type + "Client"), type(pkg, type + "ClientBuilder"),
            type(pkg, "Default" + type + "Client"), type(pkg, "Default" + type + "ClientBuilder"),
            async ? type(pkg, type + "AsyncClient") : null, async ? type(pkg, type + "AsyncClientBuilder") : null,
            async ? type(pkg, "Default" + type + "AsyncClient") : null,
            async ? type(pkg, "Default" + type + "AsyncClientBuilder") : null,
            type(pkg, type + "OperationBindings"), AwsSymbolProvider.operationTable(service, settings),
            AwsSymbolProvider.serviceMetadata(service, settings), AwsSymbolProvider.serviceException(service, settings),
            rules, operations, WaiterPlan.syncWaiterType(context),
            async ? WaiterPlan.asyncWaiterType(context) : null,
            classifier.utilities(service).filter(u -> UtilitiesGenerator.implemented(service, u.kind()))
                      .map(u -> type(pkg, u.typeName())).orElse(null));
    }

    private static Operation operation(AwsCodegenContext context, ClientSurfaceClassifier classifier,
                                       OperationShape operation, Map<String, Map<String, String>> schemes,
                                       List<String> serviceAuth, boolean async) {
        Model model = context.model();
        ServiceShape service = context.service();
        String name = operation.getId().getName(service);
        Shape input = model.expectShape(operation.getInputShape());
        Shape output = model.expectShape(operation.getOutputShape());
        boolean unitInput = operation.getInputShape().equals(UNIT);
        boolean unitOutput = operation.getOutputShape().equals(UNIT);
        boolean required = input.members().stream().anyMatch(MemberShape::isRequired);
        boolean streamingInput = hasStreamingBlob(model, input);
        boolean paginated = !unitInput && PaginatorPlan.paginated(context, operation);
        MemberShape inputEvents = eventStreamMember(model, input);
        MemberShape outputEvents = eventStreamMember(model, output);
        boolean noArgument = !required && !streamingInput && inputEvents == null && outputEvents == null
                             && classifier.noArgumentOverload(service, operation);
        List<Symbol> errors = new ArrayList<>();
        // The operation's own errors, then the service's errors, each sorted by shape ID so that declaration order
        // never changes the output.
        Set<ShapeId> errorIds = new LinkedHashSet<>(new TreeSet<>(operation.getErrors()));
        errorIds.addAll(new TreeSet<>(service.getErrors()));
        for (ShapeId error : errorIds) {
            errors.add(context.symbolProvider().toSymbol(model.expectShape(error)));
        }
        Auth auth = new Auth(schemes, serviceAuth, operation.getTrait(AuthTrait.class).map(t -> ids(t)).orElse(null),
                             operation.hasTrait(OptionalAuthTrait.class),
                             operation.hasTrait(UnsignedPayloadTrait.class));
        return new Operation(operation.getId(), name, methodName(name),
                             OperationTablePlan.constantName(name),
                             context.symbolProvider().toSymbol(input), context.symbolProvider().toSymbol(output),
                             unitInput, unitOutput, !unitInput && classifier.consumerOverload(service, operation),
                             noArgument, streamingInput, hasStreamingBlob(model, output), errors, auth,
                             HttpChecksum.of(operation),
                             paginated ? PaginatorPlan.iterableType(context.settings(), name) : null,
                             paginated && async ? PaginatorPlan.publisherType(context.settings(), name) : null,
                             eventSymbol(context, inputEvents),
                             inputEvents == null ? null : inputEvents.getMemberName(),
                             eventSymbol(context, outputEvents),
                             outputEvents == null ? null
                                                  : responseHandler(context.symbolProvider().toSymbol(output), name));
    }

    /** The member of {@code structure} that targets a {@code @streaming} union (its event stream), or {@code null}. */
    private static MemberShape eventStreamMember(Model model, Shape structure) {
        return structure.members().stream()
                        .filter(member -> {
                            Shape target = model.expectShape(member.getTarget());
                            return target.isUnionShape() && target.hasTrait(StreamingTrait.class);
                        })
                        .findFirst().orElse(null);
    }

    private static Symbol eventSymbol(AwsCodegenContext context, MemberShape member) {
        return member == null ? null
                              : context.symbolProvider().toSymbol(context.model().expectShape(member.getTarget()));
    }

    /** V2's {@code <Operation>ResponseHandler}, beside the operation's output type in the model package. */
    private static Symbol responseHandler(Symbol output, String operationName) {
        return type(output.getNamespace(), operationName + "ResponseHandler");
    }

    /**
     * The Java method of an operation, as V2 names it ({@code GetItem} is {@code getItem}); a name that is not a Java
     * identifier or that every client declares or inherits gets the suffix {@code Operation}.
     */
    static String methodName(String operationName) {
        String method = AwsSymbolProvider.lowerLeadingCapitals(operationName);
        return SourceVersion.isName(method, SourceVersion.RELEASE_21) && !RESERVED_METHODS.contains(method)
               ? method : method + "Operation";
    }

    private static boolean hasStreamingBlob(Model model, Shape structure) {
        return structure.members().stream()
                        .map(member -> model.expectShape(member.getTarget()))
                        .anyMatch(target -> target.isBlobShape() && target.hasTrait(StreamingTrait.class));
    }

    private static List<String> ids(AuthTrait trait) {
        return trait.getValueSet().stream().map(ShapeId::toString).toList();
    }

    private static Map<String, String> stringMembers(Trait trait) {
        Map<String, String> values = new TreeMap<>();
        Node node = trait.toNode();
        node.asObjectNode().ifPresent(object -> object.getStringMap().forEach(
            (key, value) -> value.asStringNode().ifPresent(s -> values.put(key, s.getValue()))));
        return values;
    }

    /** V2's {@code SERVICE_NAME}: the {@code aws.api#service} endpoint prefix, else the lower-case type name. */
    private static String serviceName(ServiceShape service, String type) {
        return service.getTrait(ServiceTrait.class).map(ServiceTrait::getEndpointPrefix)
                      .orElse(type.toLowerCase(Locale.ROOT));
    }

    private static Symbol type(String packageName, String name) {
        return Symbol.builder()
                     .namespace(packageName, ".")
                     .name(name)
                     .definitionFile(packageName.replace('.', '/') + "/" + name + ".java")
                     .build();
    }
}
