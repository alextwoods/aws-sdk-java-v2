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

package software.amazon.awssdk.endpoints.binding;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.endpoints.rules.RuleSetParameter;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The endpoint facts of one operation, generated from its canonical traits: how the operation binds values to the
 * service's rule-set parameters, the host prefix it applies to the resolved endpoint, and whether it uses endpoint
 * discovery.
 *
 * <p>{@link #bind} fills each rule-set parameter from the most specific source that has a value, in the order the
 * Smithy rules engine specifies:
 * <ol>
 *   <li>{@code smithy.rules#staticContextParams} of the operation;</li>
 *   <li>{@code smithy.rules#contextParam} input members;</li>
 *   <li>{@code smithy.rules#operationContextParams} paths over the input;</li>
 *   <li>{@code smithy.rules#clientContextParams} values of the client;</li>
 *   <li>the parameter's built-in, sourced from client configuration;</li>
 * </ol>
 * and otherwise leaves it unset, so the rule set's default applies. Instances are immutable and thread-safe.
 */
public final class EndpointBinding {
    private static final EndpointBinding NONE = builder("").build();

    private final String operationName;
    private final Map<String, Object> staticContextParams;
    private final Map<String, String> contextParams;
    private final Map<String, OperationContextPath> operationContextParams;
    private final HostPrefix hostPrefix;
    private final Discovery discovery;

    private EndpointBinding(Builder builder) {
        this.operationName = builder.operationName;
        this.staticContextParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.staticContextParams));
        this.contextParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.contextParams));
        this.operationContextParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.operationContextParams));
        this.hostPrefix = builder.hostPrefix;
        this.discovery = builder.discovery;
    }

    /** A binding of an operation with no endpoint traits: parameters come from client values only. */
    public static EndpointBinding none() {
        return NONE;
    }

    /** A builder of the binding of operation {@code operationName} (its shape name). */
    public static Builder builder(String operationName) {
        return new Builder(operationName);
    }

    /** The operation shape name. */
    public String operationName() {
        return operationName;
    }

    /** Static context parameter values by parameter name. Immutable. */
    public Map<String, Object> staticContextParams() {
        return staticContextParams;
    }

    /** Input member names by the parameter name they bind. Immutable. */
    public Map<String, String> contextParams() {
        return contextParams;
    }

    /** Operation context parameter paths by parameter name. Immutable. */
    public Map<String, OperationContextPath> operationContextParams() {
        return operationContextParams;
    }

    /** The operation's host prefix, if it has an {@code endpoint} trait. */
    public Optional<HostPrefix> hostPrefix() {
        return Optional.ofNullable(hostPrefix);
    }

    /** The operation's endpoint discovery binding, if it has a {@code clientDiscoveredEndpoint} trait. */
    public Optional<Discovery> discovery() {
        return Optional.ofNullable(discovery);
    }

    /**
     * Binds the parameters of {@code ruleSet} for a call of this operation.
     *
     * @param input the operation input (a generated or dynamic structure), or {@code null} for none
     * @throws EndpointResolutionException if a bound value does not have its parameter's type
     */
    public EndpointParameters bind(EndpointRuleSet ruleSet, Object input, EndpointClientValues client) {
        Objects.requireNonNull(ruleSet, "ruleSet");
        Objects.requireNonNull(client, "client");
        EndpointParameters.Builder parameters = EndpointParameters.builder();
        for (RuleSetParameter parameter : ruleSet.parameters()) {
            String name = parameter.name();
            Object value = staticContextParams.get(name);
            if (value == null && contextParams.containsKey(name)) {
                value = member(input, contextParams.get(name));
            }
            if (value == null && operationContextParams.containsKey(name)) {
                value = operationContextParams.get(name).evaluate(input);
            }
            if (value == null) {
                value = client.clientContextParam(name);
            }
            if (value == null && parameter.builtIn() != null) {
                value = client.builtIn(parameter.builtIn());
            }
            if (value != null) {
                parameters.put(name, normalize(parameter, value));
            }
        }
        return parameters.build();
    }

    /**
     * Resolves the endpoint of a call: binds the parameters, evaluates {@code ruleSet}, and applies the host prefix
     * unless {@link EndpointClientValues#hostPrefixInjectionDisabled()}.
     *
     * @throws EndpointResolutionException if binding, rule evaluation or the host prefix fails
     */
    public Endpoint resolve(EndpointRuleSet ruleSet, Object input, EndpointClientValues client) {
        Endpoint endpoint = ruleSet.resolve(bind(ruleSet, input, client));
        return applyHostPrefix(endpoint, input, client);
    }

    /**
     * {@code endpoint} with this operation's host prefix applied (from the input's host-label members), or
     * {@code endpoint} itself if the operation has none or {@code client} disables host-prefix injection.
     */
    public Endpoint applyHostPrefix(Endpoint endpoint, Object input, EndpointClientValues client) {
        if (hostPrefix == null || client.hostPrefixInjectionDisabled()) {
            return endpoint;
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (String label : hostPrefix.labels()) {
            Object value = member(input, label);
            if (value != null) {
                labels.put(label, scalarString(value));
            }
        }
        URI prefixed = hostPrefix.apply(endpoint.url(), labels);
        return endpoint.toBuilder().url(prefixed).build();
    }

    /** The value of input member {@code memberName}, or {@code null}. */
    static Object member(Object input, String memberName) {
        if (input instanceof SerializableStruct struct) {
            Schema member = struct.schema().member(memberName);
            return member == null ? null : struct.getMemberValue(member);
        }
        if (input instanceof Map<?, ?> map) {
            return map.get(memberName);
        }
        return null;
    }

    /** A bound value converted to its parameter's rule-set type. */
    private Object normalize(RuleSetParameter parameter, Object value) {
        Object normalized = switch (parameter.type()) {
            case STRING -> value instanceof Boolean || value instanceof List<?> ? null : scalarString(value);
            case BOOLEAN -> value instanceof Boolean ? value : null;
            case STRING_ARRAY -> value instanceof List<?> list ? stringList(list) : null;
        };
        if (normalized == null) {
            throw EndpointResolutionException.create(
                "Endpoint parameter " + parameter.name() + " of operation " + operationName + " must be a "
                + parameter.type() + " but was bound to " + value + ".");
        }
        return normalized;
    }

    private static List<String> stringList(List<?> values) {
        List<String> strings = new ArrayList<>(values.size());
        for (Object value : values) {
            String string = value == null || value instanceof Boolean || value instanceof List<?> ? null
                                                                                                : scalarString(value);
            if (string == null) {
                return null;
            }
            strings.add(string);
        }
        return strings;
    }

    /** A string or string-like scalar (a region, URI or modeled enum) as its string value, else {@code null}. */
    private static String scalarString(Object value) {
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Region region) {
            return region.id();
        }
        if (value instanceof URI uri) {
            return uri.toString();
        }
        if (value instanceof Enum<?> e) {
            return e.toString();
        }
        return null;
    }

    @Override
    public String toString() {
        return "EndpointBinding(" + operationName + ")";
    }

    /**
     * The endpoint discovery facts of an operation ({@code aws.api#clientDiscoveredEndpoint}).
     *
     * @param required whether a discovered endpoint is required, rather than preferred, for the operation
     * @param idMembers the input members with {@code aws.api#clientEndpointDiscoveryId}, in member order
     */
    public record Discovery(boolean required, List<String> idMembers) {
        public Discovery {
            idMembers = List.copyOf(idMembers);
        }

        /** The discovery identifier values of {@code input}, by member name, for the discovery cache key. */
        public Map<String, String> ids(Object input) {
            Map<String, String> ids = new LinkedHashMap<>();
            for (String member : idMembers) {
                Object value = member(input, member);
                String string = value == null ? null : scalarString(value);
                if (string != null) {
                    ids.put(member, string);
                }
            }
            return Collections.unmodifiableMap(ids);
        }
    }

    /** Builder of an {@link EndpointBinding}; used by generated code. Not thread-safe. */
    public static final class Builder {
        private final String operationName;
        private final Map<String, Object> staticContextParams = new LinkedHashMap<>();
        private final Map<String, String> contextParams = new LinkedHashMap<>();
        private final Map<String, OperationContextPath> operationContextParams = new LinkedHashMap<>();
        private HostPrefix hostPrefix;
        private Discovery discovery;

        private Builder(String operationName) {
            this.operationName = Objects.requireNonNull(operationName, "operationName");
        }

        /** Binds parameter {@code name} to a static value (a String, Boolean or List of Strings). */
        public Builder staticContextParam(String name, Object value) {
            staticContextParams.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Binds parameter {@code name} to the input member {@code memberName}. */
        public Builder contextParam(String name, String memberName) {
            contextParams.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(memberName, "memberName"));
            return this;
        }

        /** Binds parameter {@code name} to the JMESPath {@code path} over the input. */
        public Builder operationContextParam(String name, String path) {
            operationContextParams.put(Objects.requireNonNull(name, "name"), OperationContextPath.parse(path));
            return this;
        }

        /** Sets the {@code hostPrefix} of the operation's {@code endpoint} trait. */
        public Builder hostPrefix(String template) {
            this.hostPrefix = HostPrefix.parse(template);
            return this;
        }

        /** Marks the operation as using endpoint discovery. */
        public Builder discovery(boolean required, List<String> idMembers) {
            this.discovery = new Discovery(required, idMembers);
            return this;
        }

        public EndpointBinding build() {
            return new EndpointBinding(this);
        }
    }
}
