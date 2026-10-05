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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.SourceVersion;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Render plan of the public {@code <root>.endpoints.<Service>EndpointParams} class: one typed, optional property per
 * parameter of the service's canonical endpoint rule set, in declaration order.
 *
 * @param params symbol of the generated class
 * @param service absolute shape ID of the service
 * @param parameters the rule-set parameters in declaration order
 */
record EndpointParamsPlan(Symbol params, ShapeId service, List<Parameter> parameters) implements RenderPlan {
    /** Accessor names every generated params class declares itself. */
    private static final Set<String> RESERVED = Set.of("builder", "toBuilder", "toEndpointParameters", "build",
                                                       "equals", "hashCode", "toString", "getClass", "wait",
                                                       "notify", "notifyAll", "clone", "finalize");

    EndpointParamsPlan {
        parameters = List.copyOf(parameters);
    }

    /**
     * One rule-set parameter.
     *
     * @param name the parameter name in the rule set
     * @param javaName the accessor and builder method name
     * @param type {@code STRING}, {@code BOOLEAN} or {@code STRING_ARRAY}
     * @param region whether the parameter is bound to {@code AWS::Region}, and so is typed as a {@code Region}
     * @param builtIn the built-in ID, or an empty string
     * @param documentation the parameter documentation, or an empty string
     * @param deprecated whether the rule set deprecates the parameter
     */
    record Parameter(String name, String javaName, String type, boolean region, String builtIn, String documentation,
                     boolean deprecated) {
    }

    static EndpointParamsPlan of(AwsCodegenContext context, EndpointRuleSetTrait trait) {
        return new EndpointParamsPlan(EndpointRulesGenerator.paramsSymbol(context), context.service().getId(),
                                      parameters(context.service().getId(), trait));
    }

    /** The rule-set parameters of {@code trait}, in declaration order. */
    static List<Parameter> parameters(ShapeId service, EndpointRuleSetTrait trait) {
        ObjectNode parameters = trait.getRuleSet().expectObjectNode()
                                     .getObjectMember("parameters").orElse(Node.objectNode());
        List<Parameter> result = new ArrayList<>();
        Set<String> javaNames = new HashSet<>();
        parameters.getStringMap().forEach((name, node) -> {
            ObjectNode definition = node.expectObjectNode();
            String type = switch (definition.expectStringMember("type").getValue().toLowerCase(Locale.ROOT)) {
                case "string" -> "STRING";
                case "boolean" -> "BOOLEAN";
                case "stringarray" -> "STRING_ARRAY";
                default -> throw new AwsCodegenException("Endpoint rule-set parameter " + name + " of service "
                                                         + service + " has unsupported type "
                                                         + definition.expectStringMember("type").getValue() + ".");
            };
            String builtIn = definition.getStringMemberOrDefault("builtIn", "");
            String javaName = javaName(name);
            if (!javaNames.add(javaName)) {
                throw new AwsCodegenException("Endpoint rule-set parameters of service " + service + " map to the "
                                              + "same Java name " + javaName + ".");
            }
            result.add(new Parameter(name, javaName, type, "STRING".equals(type) && "AWS::Region".equals(builtIn),
                                     builtIn, definition.getStringMemberOrDefault("documentation", ""),
                                     definition.getMember("deprecated").isPresent()));
        });
        return result;
    }

    /**
     * The V2 accessor name of parameter {@code name}: lower camel case with acronyms folded ({@code UseFIPS} becomes
     * {@code useFips}, {@code UseS3ExpressControlEndpoint} becomes {@code useS3ExpressControlEndpoint}); names that
     * are Java keywords or clash with a generated method get a {@code Value} suffix.
     */
    static String javaName(String name) {
        List<String> words = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= name.length(); i++) {
            boolean boundary = i == name.length()
                               || Character.isUpperCase(name.charAt(i)) && (Character.isLowerCase(name.charAt(i - 1))
                                                                            || Character.isDigit(name.charAt(i - 1)))
                               || Character.isUpperCase(name.charAt(i)) && i + 1 < name.length()
                                  && Character.isUpperCase(name.charAt(i - 1))
                                  && Character.isLowerCase(name.charAt(i + 1));
            if (boundary) {
                words.add(name.substring(start, i));
                start = i;
            }
        }
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            String lower = word.toLowerCase(Locale.ROOT);
            result.append(result.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        String javaName = result.toString();
        if (!SourceVersion.isName(javaName, SourceVersion.RELEASE_21) || RESERVED.contains(javaName)) {
            return javaName + "Value";
        }
        return javaName;
    }
}
