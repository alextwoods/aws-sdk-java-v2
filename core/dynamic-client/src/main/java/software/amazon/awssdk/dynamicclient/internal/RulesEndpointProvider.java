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

package software.amazon.awssdk.dynamicclient.internal;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.dynamicclient.DynamicEndpointParams;
import software.amazon.awssdk.dynamicclient.DynamicEndpointProvider;
import software.amazon.awssdk.dynamicclient.schema.internal.ModelConversions;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.jmespath.node.NodeJmespathRuntime;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.ArrayValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.RecordValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.StringValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.traits.ContextIndex;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.OperationContextParamDefinition;
import software.amazon.smithy.rulesengine.traits.StaticContextParamDefinition;

/**
 * The default endpoint provider of a dynamic client: evaluates the service's {@code smithy.rules#endpointRuleSet}, or
 * its {@code smithy.rules#endpointBdd} when it has no rule set, with the {@code smithy-rules-engine} evaluator for
 * each call (Requirements 27.5, 27.6 and 27.9).
 *
 * <p>The parameters are bound in the order of the Smithy rules engine specification, most specific last, so a later
 * binding replaces an earlier one: the parameter defaults (applied by the evaluator), the built-ins, the
 * {@code smithy.rules#operationContextParams} of the operation (JMESPath over the input), the
 * {@code smithy.rules#contextParam} members of the input, and the {@code smithy.rules#staticContextParams} of the
 * operation. The built-ins are those a generated client binds from the call's execution attributes:
 * {@code AWS::Region}, {@code AWS::UseFIPS}, {@code AWS::UseDualStack}, {@code AWS::Auth::AccountId} (from the
 * resolved AWS credentials), {@code AWS::Auth::AccountIdEndpointMode} and {@code AWS::S3::UseGlobalEndpoint}.
 *
 * <p>A configured endpoint override is the endpoint of every call; the rules are not evaluated (Requirement 27.6), so
 * the {@code SDK::Endpoint} built-in is never bound.
 *
 * <p>The result becomes an SDK {@link Endpoint}: its URL, its headers, and the {@code authSchemes} and
 * {@code metricValues} properties as {@link AwsEndpointAttribute#AUTH_SCHEMES} and
 * {@link AwsEndpointAttribute#METRIC_VALUES}, as in a generated endpoint provider. An error rule completes the future
 * with an {@link SdkClientException} whose message is the rule's error.
 */
@SdkInternalApi
@ThreadSafe
public final class RulesEndpointProvider implements DynamicEndpointProvider {
    private static final Logger log = Logger.loggerFor(RulesEndpointProvider.class);

    private static final String REGION = "AWS::Region";
    private static final String USE_FIPS = "AWS::UseFIPS";
    private static final String USE_DUAL_STACK = "AWS::UseDualStack";
    private static final String ACCOUNT_ID = "AWS::Auth::AccountId";
    private static final String ACCOUNT_ID_ENDPOINT_MODE = "AWS::Auth::AccountIdEndpointMode";
    private static final String S3_USE_GLOBAL_ENDPOINT = "AWS::S3::UseGlobalEndpoint";

    private final Model model;
    private final ShapeId serviceId;
    private final Parameters parameters;
    private final Function<Map<Identifier, Value>, Value> evaluator;
    private final boolean accountIdBound;
    private final Map<software.amazon.awssdk.schema.ShapeId, OperationBindings> operationBindings =
        new ConcurrentHashMap<>();

    private RulesEndpointProvider(Model model, ShapeId serviceId, Parameters parameters,
                                  Function<Map<Identifier, Value>, Value> evaluator) {
        this.model = model;
        this.serviceId = serviceId;
        this.parameters = parameters;
        this.evaluator = evaluator;
        this.accountIdBound = parameters != null && hasBuiltIn(parameters, ACCOUNT_ID);
    }

    private static boolean hasBuiltIn(Parameters parameters, String builtIn) {
        for (Parameter parameter : parameters) {
            if (parameter.getBuiltIn().filter(builtIn::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the endpoint provider of a service: one that evaluates its {@code endpointRuleSet}, else its
     * {@code endpointBdd}, else one that fails every call that has no endpoint override.
     *
     * @param model the model
     * @param service the service shape of the model
     * @return the provider
     */
    public static RulesEndpointProvider forService(Model model, ServiceShape service) {
        Validate.paramNotNull(model, "model");
        Validate.paramNotNull(service, "service");
        Optional<EndpointRuleSetTrait> ruleSetTrait = service.getTrait(EndpointRuleSetTrait.class);
        if (ruleSetTrait.isPresent()) {
            EndpointRuleSet ruleSet = ruleSetTrait.get().getEndpointRuleSet();
            return new RulesEndpointProvider(model, service.getId(), ruleSet.getParameters(),
                                             args -> RuleEvaluator.evaluate(ruleSet, args));
        }
        Optional<EndpointBddTrait> bddTrait = service.getTrait(EndpointBddTrait.class);
        if (bddTrait.isPresent()) {
            EndpointBddTrait bdd = bddTrait.get();
            return new RulesEndpointProvider(model, service.getId(), bdd.getParameters(),
                                             args -> RuleEvaluator.evaluate(bdd, args));
        }
        return new RulesEndpointProvider(model, service.getId(), null, null);
    }

    /**
     * Returns whether the service has endpoint rules ({@code endpointRuleSet} or {@code endpointBdd}).
     *
     * @return true if the provider evaluates rules
     */
    public boolean hasRules() {
        return evaluator != null;
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(DynamicEndpointParams params) {
        try {
            URI override = endpointOverride(params.executionAttributes());
            if (override != null) {
                return CompletableFuture.completedFuture(Endpoint.builder().url(override).build());
            }
            if (evaluator == null) {
                throw SdkClientException.create(
                    "No endpoint provider or endpointRuleSet available for service " + serviceId + ": its model has "
                    + "neither a smithy.rules#endpointRuleSet nor a smithy.rules#endpointBdd trait. Configure an "
                    + "endpointOverride or an endpointProvider on the DynamicClient builder.");
            }
            CompletableFuture<? extends Identity> identity = accountIdBound ? identity(params.executionAttributes()) : null;
            if (identity == null) {
                return CompletableFuture.completedFuture(evaluate(params, null));
            }
            // The account ID comes from the resolved credentials. Wait for them without blocking the calling thread.
            return identity.thenApply(resolved -> evaluate(params, accountId(resolved)));
        } catch (Exception e) {
            return CompletableFutureUtils.failedFuture(e);
        }
    }

    /**
     * Binds the rule set parameters of a call.
     *
     * @param params the call
     * @param accountId the account ID of the call's credentials, or null
     * @return the parameter values, without the defaults the evaluator applies
     */
    Map<Identifier, Value> bindParameters(DynamicEndpointParams params, String accountId) {
        Map<Identifier, Value> args = new HashMap<>();
        ExecutionAttributes attributes = params.executionAttributes();
        for (Parameter parameter : parameters) {
            if (parameter.getBuiltIn().isPresent()) {
                Node value = builtIn(parameter.getBuiltIn().get(), attributes, accountId);
                if (value != null) {
                    args.put(parameter.getName(), toValue(parameter, value, "built-in"));
                }
            }
        }
        OperationBindings operation = operationBindings.computeIfAbsent(params.operation().schema().id(),
                                                                        this::bindingsOf);
        if (operation.readsInput()) {
            Node input = toNode(inputDocument(params.input()));
            operation.operationContextParams.forEach((name, expression) -> {
                bind(args, name, expression.evaluate(input, NodeJmespathRuntime.INSTANCE), "operationContextParams");
            });
            operation.contextParams.forEach((name, member) -> {
                Node value = input.isObjectNode() ? input.expectObjectNode().getMember(member).orElse(null) : null;
                bind(args, name, value, "contextParam");
            });
        }
        operation.staticContextParams.forEach((name, value) -> bind(args, name, value, "staticContextParams"));
        for (Parameter parameter : parameters) {
            if (parameter.isRequired() && !parameter.getDefault().isPresent() && !args.containsKey(parameter.getName())) {
                throw SdkClientException.create("Cannot resolve the endpoint of " + params.operation().schema().id()
                                                + ": the required endpoint parameter " + parameter.getName()
                                                + " has no value.");
            }
        }
        return args;
    }

    private Endpoint evaluate(DynamicEndpointParams params, String accountId) {
        Value result;
        try {
            result = evaluator.apply(bindParameters(params, accountId));
        } catch (SdkClientException e) {
            throw e;
        } catch (RuntimeException e) {
            throw SdkClientException.create("Unable to resolve the endpoint of " + params.operation().schema().id()
                                            + " from the endpoint rules of " + serviceId + ": " + e.getMessage(), e);
        }
        if (result instanceof StringValue) {
            throw SdkClientException.create(((StringValue) result).getValue());
        }
        if (!(result instanceof EndpointValue)) {
            throw SdkClientException.create("The endpoint rules of " + serviceId + " returned " + result
                                            + " instead of an endpoint or an error");
        }
        return toEndpoint((EndpointValue) result);
    }

    private void bind(Map<Identifier, Value> args, String name, Node value, String source) {
        if (value == null || value.isNullNode()) {
            return;
        }
        Identifier id = Identifier.of(name);
        Optional<Parameter> parameter = parameters.get(id);
        if (parameter.isPresent()) {
            args.put(id, toValue(parameter.get(), value, source));
        }
    }

    private OperationBindings bindingsOf(software.amazon.awssdk.schema.ShapeId operationId) {
        Optional<OperationShape> operation = model.getShape(ModelConversions.smithyShapeId(operationId))
                                                  .flatMap(s -> s.asOperationShape());
        if (!operation.isPresent()) {
            return OperationBindings.NONE;
        }
        ContextIndex index = ContextIndex.of(model);
        Map<String, JmespathExpression> operationContextParams = new LinkedHashMap<>();
        index.getOperationContextParams(operation.get()).ifPresent(trait -> {
            for (Map.Entry<String, OperationContextParamDefinition> e : trait.getParameters().entrySet()) {
                operationContextParams.put(e.getKey(), JmespathExpression.parse(e.getValue().getPath()));
            }
        });
        Map<String, String> contextParams = new LinkedHashMap<>();
        for (Map.Entry<MemberShape, ContextParamTrait> e : index.getContextParams(operation.get()).entrySet()) {
            contextParams.put(e.getValue().getName(), e.getKey().getMemberName());
        }
        Map<String, Node> staticContextParams = new LinkedHashMap<>();
        index.getStaticContextParams(operation.get()).ifPresent(trait -> {
            for (Map.Entry<String, StaticContextParamDefinition> e : trait.getParameters().entrySet()) {
                staticContextParams.put(e.getKey(), e.getValue().getValue());
            }
        });
        return new OperationBindings(operationContextParams, contextParams, staticContextParams);
    }

    private static Node builtIn(String name, ExecutionAttributes attributes, String accountId) {
        switch (name) {
            case REGION:
                Region region = attributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
                return region == null ? null : Node.from(region.id());
            case USE_FIPS:
                return booleanNode(attributes.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED));
            case USE_DUAL_STACK:
                return booleanNode(attributes.getAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED));
            case ACCOUNT_ID:
                return accountId == null ? null : Node.from(accountId);
            case ACCOUNT_ID_ENDPOINT_MODE:
                AccountIdEndpointMode mode = attributes.getAttribute(AwsExecutionAttribute.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE);
                return mode == null ? null : Node.from(StringUtils.lowerCase(mode.name()));
            case S3_USE_GLOBAL_ENDPOINT:
                return booleanNode(attributes.getAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT));
            default:
                return null;
        }
    }

    private static Node booleanNode(Boolean value) {
        return value == null ? null : Node.from(value);
    }

    /**
     * The identity of the call's selected auth scheme, whose account ID binds {@code AWS::Auth::AccountId}, as the
     * generated {@code resolveAndRecordAccountIdFromIdentity} reads it.
     */
    private static CompletableFuture<? extends Identity> identity(ExecutionAttributes attributes) {
        SelectedAuthScheme<?> selected = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        return selected == null ? null : selected.identity();
    }

    private static String accountId(Identity identity) {
        if (identity instanceof AwsCredentialsIdentity) {
            return ((AwsCredentialsIdentity) identity).accountId().orElse(null);
        }
        return null;
    }

    private static URI endpointOverride(ExecutionAttributes attributes) {
        ClientEndpointProvider provider = attributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER);
        if (provider == null || !provider.isEndpointOverridden()) {
            return null;
        }
        return provider.clientEndpoint();
    }

    private static Document inputDocument(SerializableStruct input) {
        return input instanceof Document ? (Document) input : Document.of(input);
    }

    /**
     * Converts a document to a smithy-model node for JMESPath evaluation. Members are keyed by their Smithy member
     * names; a blob becomes its base64 text and a timestamp its epoch seconds.
     */
    static Node toNode(Document document) {
        if (document == null) {
            return Node.nullNode();
        }
        switch (document.type()) {
            case BOOLEAN:
                return Node.from(document.asBoolean());
            case STRING:
            case ENUM:
                return Node.from(document.asString());
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                return Node.from(document.asNumber());
            case BLOB:
                ByteBuffer blob = document.asBlob();
                byte[] bytes = new byte[blob.remaining()];
                blob.get(bytes);
                return Node.from(Base64.getEncoder().encodeToString(bytes));
            case TIMESTAMP:
                return Node.from(document.asTimestamp().toEpochMilli() / 1000.0);
            case LIST:
                ArrayNode.Builder list = ArrayNode.builder();
                for (Document element : document.asList()) {
                    list.withValue(toNode(element));
                }
                return list.build();
            case MAP:
            case STRUCTURE:
            case UNION:
                ObjectNode.Builder object = ObjectNode.builder();
                for (Map.Entry<String, Document> member : document.asMap().entrySet()) {
                    object.withMember(member.getKey(), toNode(member.getValue()));
                }
                return object.build();
            default:
                throw new IllegalArgumentException("Cannot read a " + document.type() + " document in an endpoint "
                                                   + "parameter binding");
        }
    }

    private static Value toValue(Parameter parameter, Node value, String source) {
        ParameterType type = parameter.getType();
        if (type == ParameterType.STRING && value.isStringNode()) {
            return Value.stringValue(value.expectStringNode().getValue());
        }
        if (type == ParameterType.BOOLEAN && value.isBooleanNode()) {
            return Value.booleanValue(value.expectBooleanNode().getValue());
        }
        if (type == ParameterType.STRING_ARRAY && value.isArrayNode()) {
            List<Value> elements = new ArrayList<>();
            boolean allStrings = true;
            for (Node element : value.expectArrayNode().getElements()) {
                allStrings &= element.isStringNode();
                if (element.isStringNode()) {
                    elements.add(Value.stringValue(element.expectStringNode().getValue()));
                }
            }
            if (allStrings) {
                return Value.arrayValue(elements);
            }
        }
        throw SdkClientException.create("The " + source + " value of endpoint parameter " + parameter.getName()
                                        + " is a " + value.getType() + ", but the parameter is a " + type);
    }

    private static Endpoint toEndpoint(EndpointValue value) {
        Endpoint.Builder endpoint = Endpoint.builder().url(URI.create(value.getUrl()));
        value.getHeaders().forEach((name, values) -> values.forEach(v -> endpoint.putHeader(name, v)));
        for (Map.Entry<String, Value> property : value.getProperties().entrySet()) {
            switch (property.getKey()) {
                case "authSchemes":
                    endpoint.putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, authSchemes(property.getValue()));
                    break;
                case "metricValues":
                    endpoint.putAttribute(AwsEndpointAttribute.METRIC_VALUES, strings(property.getValue()));
                    break;
                default:
                    log.debug(() -> "Ignoring the endpoint property " + property.getKey()
                                    + ", which the dynamic client does not support");
                    break;
            }
        }
        return endpoint.build();
    }

    private static List<EndpointAuthScheme> authSchemes(Value value) {
        List<EndpointAuthScheme> schemes = new ArrayList<>();
        for (Value element : value.expectArrayValue().getValues()) {
            RecordValue record = element.expectRecordValue();
            String name = record.get("name").expectStringValue().getValue();
            if ("sigv4".equals(name)) {
                SigV4AuthScheme.Builder scheme = SigV4AuthScheme.builder();
                stringMember(record, "signingName").ifPresent(scheme::signingName);
                stringMember(record, "signingRegion").ifPresent(scheme::signingRegion);
                booleanMember(record, "disableDoubleEncoding").ifPresent(scheme::disableDoubleEncoding);
                schemes.add(scheme.build());
            } else if ("sigv4a".equals(name)) {
                SigV4aAuthScheme.Builder scheme = SigV4aAuthScheme.builder();
                stringMember(record, "signingName").ifPresent(scheme::signingName);
                member(record, "signingRegionSet").ifPresent(set -> scheme.signingRegionSet(strings(set)));
                booleanMember(record, "disableDoubleEncoding").ifPresent(scheme::disableDoubleEncoding);
                schemes.add(scheme.build());
            } else {
                log.debug(() -> "Ignoring the endpoint auth scheme " + name + ", which the dynamic client does not "
                                + "support");
            }
        }
        return Collections.unmodifiableList(schemes);
    }

    private static Optional<Value> member(RecordValue record, String name) {
        Value value = record.get(name);
        return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    private static Optional<String> stringMember(RecordValue record, String name) {
        return member(record, name).map(v -> v.expectStringValue().getValue());
    }

    private static Optional<Boolean> booleanMember(RecordValue record, String name) {
        return member(record, name).map(v -> v.expectBooleanValue().getValue());
    }

    private static List<String> strings(Value value) {
        ArrayValue array = value.expectArrayValue();
        List<String> result = new ArrayList<>(array.getValues().size());
        for (Value element : array.getValues()) {
            result.add(element.expectStringValue().getValue());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * The parameter bindings an operation declares, with its JMESPath expressions parsed once.
     */
    private static final class OperationBindings {
        static final OperationBindings NONE = new OperationBindings(Collections.emptyMap(), Collections.emptyMap(),
                                                                    Collections.emptyMap());

        private final Map<String, JmespathExpression> operationContextParams;
        private final Map<String, String> contextParams;
        private final Map<String, Node> staticContextParams;

        private OperationBindings(Map<String, JmespathExpression> operationContextParams,
                                  Map<String, String> contextParams, Map<String, Node> staticContextParams) {
            this.operationContextParams = operationContextParams;
            this.contextParams = contextParams;
            this.staticContextParams = staticContextParams;
        }

        boolean readsInput() {
            return !operationContextParams.isEmpty() || !contextParams.isEmpty();
        }
    }
}
