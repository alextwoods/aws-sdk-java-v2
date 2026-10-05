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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientDiscoveredEndpointTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryIdTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryTrait;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.OperationContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;

/**
 * Render plan of {@code <root>.endpoints.internal.<Service>EndpointRules}: the service's canonical endpoint rule set
 * (as chunks of its canonical JSON text, parsed once at run time) and one generated {@code EndpointBinding} per
 * operation from the operation's endpoint traits.
 *
 * @param rules symbol of the generated class
 * @param provider symbol of the service's endpoint provider interface
 * @param params symbol of the service's endpoint parameters class
 * @param service absolute shape ID of the service
 * @param ruleSetChunks consecutive chunks of the compact canonical JSON of the {@code endpointRuleSet} trait
 * @param bindings one binding per operation, sorted by absolute operation shape ID
 * @param clientContextParams the service's {@code clientContextParams}, sorted by name
 * @param discoveryOperation the {@code clientEndpointDiscovery} operation ID, or an empty string
 * @param discoveryError the {@code clientEndpointDiscovery} error ID, or an empty string
 */
record EndpointRulesPlan(Symbol rules, Symbol provider, Symbol params, ShapeId service, List<String> ruleSetChunks,
                         List<Binding> bindings, List<ClientContextParam> clientContextParams,
                         String discoveryOperation, String discoveryError) implements RenderPlan {
    /** Characters per rule-set chunk; far below the class-file limit of 65535 UTF-8 bytes per string constant. */
    static final int CHUNK_SIZE = 8192;

    EndpointRulesPlan {
        ruleSetChunks = List.copyOf(ruleSetChunks);
        bindings = List.copyOf(bindings);
        clientContextParams = List.copyOf(clientContextParams);
    }

    /**
     * The endpoint facts of one operation.
     *
     * @param constant the Java constant name
     * @param operationName the operation's contextual shape name
     * @param operation the operation shape ID
     * @param staticParams the {@code staticContextParams}, sorted by parameter name
     * @param contextParams the {@code contextParam} input members, in member order
     * @param operationContextParams the {@code operationContextParams}, sorted by parameter name
     * @param hostPrefix the {@code endpoint} trait host prefix, or an empty string
     * @param discovered whether the operation has {@code clientDiscoveredEndpoint}
     * @param discoveryRequired whether its discovered endpoint is required
     * @param discoveryIds the input members with {@code clientEndpointDiscoveryId}, in member order
     */
    record Binding(String constant, String operationName, ShapeId operation, List<StaticParam> staticParams,
                   List<ContextParam> contextParams, List<OperationContextParam> operationContextParams,
                   String hostPrefix, boolean discovered, boolean discoveryRequired, List<String> discoveryIds) {
        Binding {
            staticParams = List.copyOf(staticParams);
            contextParams = List.copyOf(contextParams);
            operationContextParams = List.copyOf(operationContextParams);
            discoveryIds = List.copyOf(discoveryIds);
        }
    }

    /**
     * One static context parameter value.
     *
     * @param name the parameter name
     * @param kind {@code STRING}, {@code BOOLEAN} or {@code STRING_ARRAY}
     * @param string the string value ({@code STRING})
     * @param bool the boolean value ({@code BOOLEAN})
     * @param strings the list value ({@code STRING_ARRAY})
     */
    record StaticParam(String name, String kind, String string, boolean bool, List<String> strings) {
        StaticParam {
            strings = List.copyOf(strings);
        }
    }

    /**
     * One context parameter.
     *
     * @param name the parameter name
     * @param member the input member name
     */
    record ContextParam(String name, String member) {
    }

    /**
     * One operation context parameter.
     *
     * @param name the parameter name
     * @param path the JMESPath expression over the input
     */
    record OperationContextParam(String name, String path) {
    }

    /**
     * One client context parameter.
     *
     * @param name the parameter name
     * @param type the Smithy shape type name ({@code boolean} or {@code string})
     */
    record ClientContextParam(String name, String type) {
    }

    static EndpointRulesPlan of(AwsCodegenContext context, EndpointRuleSetTrait trait) {
        Model model = context.model();
        List<OperationShape> operations = CodegenOrdering.byShapeId(
            TopDownIndex.of(model).getContainedOperations(context.service()));
        Map<String, ShapeId> constants = new HashMap<>();
        List<Binding> bindings = new ArrayList<>();
        for (OperationShape operation : operations) {
            String name = operation.getId().getName(context.service());
            String constant = OperationTablePlan.constantName(name);
            ShapeId previous = constants.putIfAbsent(constant, operation.getId());
            if (previous != null) {
                throw new AwsCodegenException("Operations " + previous + " and " + operation.getId() + " of service "
                                              + context.service().getId() + " both map to the endpoint binding "
                                              + "constant " + constant + ".");
            }
            bindings.add(binding(model, operation, name, constant));
        }
        List<ClientContextParam> clientContextParams = new ArrayList<>();
        context.service().getTrait(ClientContextParamsTrait.class).ifPresent(t -> new TreeMap<>(t.getParameters())
            .forEach((name, definition) -> clientContextParams.add(
                new ClientContextParam(name, definition.getType().toString()))));
        Optional<ClientEndpointDiscoveryTrait> discovery = context.service()
                                                                  .getTrait(ClientEndpointDiscoveryTrait.class);
        return new EndpointRulesPlan(
            EndpointRulesGenerator.rulesSymbol(context), EndpointRulesGenerator.providerSymbol(context),
            EndpointRulesGenerator.paramsSymbol(context), context.service().getId(),
            chunks(Node.printJson(trait.getRuleSet())), bindings, clientContextParams,
            discovery.map(d -> d.getOperation().toString()).orElse(""),
            discovery.flatMap(ClientEndpointDiscoveryTrait::getOptionalError).map(ShapeId::toString).orElse(""));
    }

    private static Binding binding(Model model, OperationShape operation, String name, String constant) {
        List<StaticParam> staticParams = new ArrayList<>();
        operation.getTrait(StaticContextParamsTrait.class).ifPresent(t -> new TreeMap<>(t.getParameters()).forEach(
            (param, definition) -> staticParams.add(staticParam(operation, param, definition.getValue()))));
        List<OperationContextParam> operationContextParams = new ArrayList<>();
        operation.getTrait(OperationContextParamsTrait.class).ifPresent(t -> new TreeMap<>(t.getParameters())
            .forEach((param, definition) -> operationContextParams.add(
                new OperationContextParam(param, definition.getPath()))));
        List<ContextParam> contextParams = new ArrayList<>();
        List<String> discoveryIds = new ArrayList<>();
        Shape input = model.expectShape(operation.getInputShape());
        for (MemberShape member : CodegenOrdering.members(input)) {
            member.getTrait(ContextParamTrait.class).ifPresent(
                t -> contextParams.add(new ContextParam(t.getName(), member.getMemberName())));
            if (member.hasTrait(ClientEndpointDiscoveryIdTrait.class)) {
                discoveryIds.add(member.getMemberName());
            }
        }
        Optional<ClientDiscoveredEndpointTrait> discovered = operation.getTrait(ClientDiscoveredEndpointTrait.class);
        return new Binding(constant, name, operation.getId(), staticParams, contextParams, operationContextParams,
                           operation.getTrait(EndpointTrait.class).map(t -> t.getHostPrefix().toString()).orElse(""),
                           discovered.isPresent(), discovered.map(ClientDiscoveredEndpointTrait::isRequired)
                                                             .orElse(false),
                           discoveryIds);
    }

    private static StaticParam staticParam(OperationShape operation, String name, Node value) {
        if (value.isBooleanNode()) {
            return new StaticParam(name, "BOOLEAN", "", value.expectBooleanNode().getValue(), List.of());
        }
        if (value.isStringNode()) {
            return new StaticParam(name, "STRING", value.expectStringNode().getValue(), false, List.of());
        }
        if (value.isArrayNode()) {
            List<String> strings = value.expectArrayNode().getElements().stream()
                                        .map(element -> element.expectStringNode().getValue()).toList();
            return new StaticParam(name, "STRING_ARRAY", "", false, strings);
        }
        throw new AwsCodegenException("Static context parameter " + name + " of operation " + operation.getId()
                                      + " has an unsupported value " + Node.printJson(value) + ".");
    }

    /** {@code text} split into consecutive chunks of at most {@link #CHUNK_SIZE} characters, never inside a pair. */
    static List<String> chunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + CHUNK_SIZE);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
                end--;
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
