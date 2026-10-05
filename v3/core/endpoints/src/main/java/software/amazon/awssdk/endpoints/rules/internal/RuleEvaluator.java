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

package software.amazon.awssdk.endpoints.rules.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.rules.RuleSetParameter;

/**
 * Evaluates a parsed rule set as the canonical Smithy decision-tree evaluator does: parameter defaults are bound, then
 * every bound parameter value; top-level rules are tried in order; a rule applies when every condition in order is
 * truthy (non-empty and not {@code false}), a condition's non-empty result is assigned to its {@code assign} name for
 * the rest of the rule; the first applying rule's endpoint or error is the result, and a tree rule whose conditions
 * hold must itself produce a result.
 *
 * <p>Stateless and thread-safe: each evaluation has its own {@link Scope}.
 */
public final class RuleEvaluator {
    /** A rule that did not apply. */
    private static final Object NO_MATCH = new Object();

    private final RuleSetParser.ParsedRuleSet ruleSet;

    public RuleEvaluator(RuleSetParser.ParsedRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Resolves the endpoint for {@code parameters}.
     *
     * @throws EndpointResolutionException if an error rule applies (with {@link EndpointResolutionException#ruleError()}
     *         set to its message), no rule applies, or the rule set is invalid for these parameters
     */
    public Endpoint evaluate(EndpointParameters parameters) {
        Scope scope = new Scope(ruleSet.parameters().size());
        for (RuleSetParameter parameter : ruleSet.parameters()) {
            if (parameter.defaultValue() != null) {
                scope.put(parameter.name(), parameter.defaultValue());
            }
        }
        parameters.asMap().forEach(scope::put);
        Object result;
        try {
            result = evaluateRules(ruleSet.rules(), scope);
        } catch (RuleEvaluationError e) {
            throw EndpointResolutionException.builder()
                                             .message("Endpoint rules evaluation failed: " + e.getMessage())
                                             .cause(e)
                                             .build();
        }
        if (result == NO_MATCH) {
            throw EndpointResolutionException.create("No endpoint rule matched the endpoint parameters " + parameters
                                                     + ".");
        }
        if (result instanceof String error) {
            throw EndpointResolutionException.ofRuleError(error);
        }
        return (Endpoint) result;
    }

    private static Object evaluateRules(List<Rule> rules, Scope scope) {
        for (Rule rule : rules) {
            Object result = evaluateRule(rule, scope);
            if (result != NO_MATCH) {
                return result;
            }
        }
        return NO_MATCH;
    }

    /** The rule's endpoint, error message ({@code String}), or {@link #NO_MATCH}. */
    private static Object evaluateRule(Rule rule, Scope scope) {
        List<Rule.Condition> conditions = rule.conditions();
        List<String> assigned = null;
        try {
            for (Rule.Condition condition : conditions) {
                Object value = condition.function().eval(scope);
                if (value != null && condition.assign() != null) {
                    scope.put(condition.assign(), value);
                    if (assigned == null) {
                        assigned = new ArrayList<>(2);
                    }
                    assigned.add(condition.assign());
                }
                if (value == null || Boolean.FALSE.equals(value)) {
                    return NO_MATCH;
                }
            }
            return switch (rule) {
                case Rule.EndpointRule endpoint -> endpoint(endpoint.endpoint(), scope);
                case Rule.ErrorRule error -> Expr.expectString(error.error().eval(scope), "error rule");
                case Rule.TreeRule tree -> {
                    Object result = evaluateRules(tree.rules(), scope);
                    if (result == NO_MATCH) {
                        throw new RuleEvaluationError("no rules inside of a tree rule matched (invalid rule set)");
                    }
                    yield result;
                }
            };
        } finally {
            if (assigned != null) {
                assigned.forEach(scope::remove);
            }
        }
    }

    private static Endpoint endpoint(Rule.EndpointTemplate template, Scope scope) {
        String url = Expr.expectString(template.url().eval(scope), "endpoint url");
        Endpoint.Builder builder = Endpoint.builder();
        try {
            builder.url(URI.create(url));
        } catch (IllegalArgumentException e) {
            throw new RuleEvaluationError("the endpoint rules produced an invalid URL: " + url);
        }
        for (Map.Entry<String, List<Expr>> header : template.headers().entrySet()) {
            List<String> values = new ArrayList<>(header.getValue().size());
            for (Expr value : header.getValue()) {
                values.add(Expr.expectString(value.eval(scope), "header " + header.getKey()));
            }
            builder.putHeader(header.getKey(), values);
        }
        for (Map.Entry<String, Expr> property : template.properties().entrySet()) {
            Object value = property.getValue().eval(scope);
            try {
                builder.putProperty(property.getKey(), value);
            } catch (IllegalArgumentException e) {
                throw new RuleEvaluationError("endpoint property " + property.getKey() + " has no value");
            }
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new RuleEvaluationError(e.getMessage());
        }
    }
}
