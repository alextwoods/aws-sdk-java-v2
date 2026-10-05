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

package software.amazon.awssdk.endpoints.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.internal.Json;
import software.amazon.awssdk.endpoints.rules.internal.RuleEvaluator;
import software.amazon.awssdk.endpoints.rules.internal.RuleSetParser;

/**
 * A service's canonical endpoint rule set (the value of its {@code smithy.rules#endpointRuleSet} trait), parsed once
 * into an immutable rule tree and evaluated per request.
 *
 * <p>Evaluation has the semantics of the canonical Smithy rules evaluator, including the standard library and the AWS
 * functions ({@code aws.partition} over the bundled canonical {@link
 * software.amazon.awssdk.endpoints.partition.Partitions partition table}, {@code aws.parseArn},
 * {@code aws.isVirtualHostableS3Bucket}), so every regional, partition, FIPS, dual-stack, account-based and
 * endpoint-auth dimension the rules model resolves exactly as modeled. Instances are immutable and thread-safe; the
 * generated service code creates one per service and shares it across clients.
 */
public final class EndpointRuleSet {
    private final String version;
    private final List<RuleSetParameter> parameters;
    private final Map<String, RuleSetParameter> byName;
    private final RuleEvaluator evaluator;

    private EndpointRuleSet(RuleSetParser.ParsedRuleSet parsed) {
        this.version = parsed.version();
        this.parameters = parsed.parameters();
        Map<String, RuleSetParameter> names = new LinkedHashMap<>();
        parameters.forEach(p -> names.put(p.name(), p));
        this.byName = Map.copyOf(names);
        this.evaluator = new RuleEvaluator(parsed);
    }

    /**
     * Parses the canonical rule-set JSON document.
     *
     * @throws IllegalArgumentException if it is not a valid endpoint rule set
     */
    public static EndpointRuleSet fromJson(String json) {
        Objects.requireNonNull(json, "json");
        return fromDocument(Json.parseObject(json));
    }

    /**
     * Parses a rule set split into consecutive chunks of its canonical JSON document, as generated code stores it
     * (class-file string constants are limited in size).
     */
    public static EndpointRuleSet fromJsonChunks(String... chunks) {
        return fromJson(String.join("", chunks));
    }

    /**
     * A rule set from an already parsed JSON document (objects as {@code Map<String, Object>}, arrays as
     * {@code List<Object>}, integers as {@code Integer}).
     */
    public static EndpointRuleSet fromDocument(Map<String, Object> document) {
        return new EndpointRuleSet(RuleSetParser.parse(document));
    }

    /** The rules-language version of the rule set, for example {@code 1.0}. */
    public String version() {
        return version;
    }

    /** The parameters in declaration order. Immutable. */
    public List<RuleSetParameter> parameters() {
        return parameters;
    }

    /** The parameter named {@code name}, if the rule set declares it. */
    public Optional<RuleSetParameter> parameter(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    /**
     * Resolves the endpoint for {@code parameters}. Parameters the rule set does not declare are ignored by the rules.
     *
     * @throws EndpointResolutionException if the rules reach an error rule (its exact message is
     *         {@link EndpointResolutionException#ruleError()}), a value has the wrong type for its declared parameter,
     *         or no rule applies
     */
    public Endpoint resolve(EndpointParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        for (Map.Entry<String, Object> entry : parameters.asMap().entrySet()) {
            RuleSetParameter parameter = byName.get(entry.getKey());
            if (parameter != null && !parameter.type().accepts(entry.getValue())) {
                throw EndpointResolutionException.create(
                    "Endpoint parameter " + entry.getKey() + " must be a " + parameter.type() + " but is "
                    + entry.getValue() + ".");
            }
        }
        return evaluator.evaluate(parameters);
    }
}
