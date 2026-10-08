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

package software.amazon.awssdk.dynamic.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SignerProperty;
import software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.dynamic.model.DynamicAwsOperation;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.dynamic.model.DynamicWaiterDefinition;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.identity.providers.DefaultCredentialsProvider;
import software.amazon.awssdk.pipeline.AuthOptionsResolver;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.pipeline.client.AwsServiceClientDefinition;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * The pipeline binding of one runtime service: the same client-independent facts and per-operation bindings a
 * generated service's builders and {@code <Service>OperationBindings} hand to the shared pipeline, derived from the
 * service's codegen view of the runtime model exactly as {@code aws-codegen} derives them (Requirements 14.14,
 * 14.15):
 * <ul>
 *   <li>the {@link AwsServiceClientDefinition}: service ID; V2-compatible service name (the {@code aws.api#service}
 *       endpoint prefix, else the lower-case service name); the declared protocols with their options,
 *       {@code awsQueryCompatible}, {@code awsQueryError} codes, query version and {@code ec2QueryName}s; a type
 *       registry of every structure and union (modeled errors as {@link DynamicServiceException}); the
 *       {@code endpointRuleSet}; and, for services with an AWS credential auth scheme, the default credentials
 *       chain over the client's configuration sources;</li>
 *   <li>Modern Auth: for services with an AWS auth scheme, the AWS auth-scheme selector and the modeled options with
 *       the signing name from the model and the region from the resolved configuration;</li>
 *   <li>one {@link AwsOperation} per operation, built on first use: the runtime operation's descriptor and
 *       schema-guided document builders, its endpoint binding (static, context and operation context parameters,
 *       host prefix, endpoint discovery) when the service has a rule set, its {@link ModeledAuth}, its
 *       {@code httpChecksum} trait value, and a request view that serializes the input's structure and union
 *       members in modeled declaration order, as generated types do ({@link DeclaredMemberOrder});</li>
 *   <li>one compiled waiter per modeled waiter, built on first use (see {@link DynamicWaiters}).</li>
 * </ul>
 * Plans, configuration, interceptors, plugins and calls then belong to the shared {@code AwsClientBuildState},
 * {@code ClientRuntime} and {@code AwsCallEngine}; nothing here executes a call.
 *
 * <p>One instance per client; thread safe.
 */
final class DynamicServiceBinding {
    private static final ShapeId SERVICE_TRAIT = ShapeId.from("aws.api#service");
    private static final ShapeId ENDPOINT_RULE_SET = ShapeId.from("smithy.rules#endpointRuleSet");
    private static final ShapeId STATIC_CONTEXT_PARAMS = ShapeId.from("smithy.rules#staticContextParams");
    private static final ShapeId CONTEXT_PARAM = ShapeId.from("smithy.rules#contextParam");
    private static final ShapeId OPERATION_CONTEXT_PARAMS = ShapeId.from("smithy.rules#operationContextParams");
    private static final ShapeId CLIENT_DISCOVERED_ENDPOINT = ShapeId.from("aws.api#clientDiscoveredEndpoint");
    private static final ShapeId CLIENT_ENDPOINT_DISCOVERY_ID = ShapeId.from("aws.api#clientEndpointDiscoveryId");
    private static final ShapeId UNSIGNED_PAYLOAD = ShapeId.from(ModeledAuth.UNSIGNED_PAYLOAD_TRAIT);
    private static final ShapeId REST_XML = ShapeId.from("aws.protocols#restXml");
    private static final ShapeId AWS_QUERY = ShapeId.from("aws.protocols#awsQuery");
    private static final ShapeId EC2_QUERY = ShapeId.from("aws.protocols#ec2Query");
    private static final ShapeId AWS_QUERY_COMPATIBLE = ShapeId.from("aws.protocols#awsQueryCompatible");
    private static final ShapeId AWS_QUERY_ERROR = ShapeId.from("aws.protocols#awsQueryError");
    private static final ShapeId EC2_QUERY_NAME = ShapeId.from("aws.protocols#ec2QueryName");
    private static final String SIGV4 = "aws.auth#sigv4";
    private static final String SIGV4A = "aws.auth#sigv4a";
    private static final String SIGV4_S3_EXPRESS = "aws.auth#sigv4-s3express";
    private static final String BEARER = "smithy.api#httpBearerAuth";
    private static final ShapeId S3 = ShapeId.from("com.amazonaws.s3#AmazonS3");

    private final DynamicService service;
    private final Model model;
    private final ServiceShape shape;
    private final SortedMap<String, Map<String, String>> authSchemes;
    private final List<String> serviceAuthTrait;
    private final EndpointRuleSet rules;
    private final AwsServiceClientDefinition definition;
    private final DeclaredMemberOrder memberOrder;
    private final ConcurrentHashMap<DynamicAwsOperation, DynamicOperationBinding> operations =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DynamicWaiters.Compiled> waiters = new ConcurrentHashMap<>();

    private DynamicServiceBinding(DynamicService service) {
        this.service = service;
        this.model = service.model();
        this.shape = model.expectShape(service.id(), ServiceShape.class);
        SortedMap<String, Map<String, String>> schemes = new TreeMap<>();
        ServiceIndex.of(model).getAuthSchemes(shape).forEach((id, trait) -> schemes.put(id.toString(),
                                                                                       stringMembers(trait)));
        this.authSchemes = schemes;
        this.serviceAuthTrait = shape.getTrait(AuthTrait.class).map(DynamicServiceBinding::ids).orElse(null);
        this.rules = shape.findTrait(ENDPOINT_RULE_SET)
                          .map(trait -> EndpointRuleSet.fromJson(Node.printJson(trait.toNode())))
                          .orElse(null);
        AwsServiceClientDefinition defined = new AwsServiceClientDefinition(service.id(), serviceName(),
                                                                            protocols(), typeRegistry(), rules);
        if (authSchemes.containsKey(SIGV4) || authSchemes.containsKey(SIGV4A)
            || authSchemes.containsKey(SIGV4_S3_EXPRESS)) {
            // Without a credentials provider, the default chain over the client's JVM-property and environment
            // sources, as generated services configure it.
            defined = defined.withDefaultCredentials(
                sources -> DefaultCredentialsProvider.builder()
                                                     .settings(new SystemSettings(sources.jvmProperties(),
                                                                                  sources.environment()))
                                                     .build());
        }
        this.definition = defined;
        this.memberOrder = new DeclaredMemberOrder(service.schemaIndex(), service.id());
    }

    /** The binding of {@code service}. */
    static DynamicServiceBinding of(DynamicService service) {
        return new DynamicServiceBinding(service);
    }

    /** The selected service. */
    DynamicService service() {
        return service;
    }

    /** The client-independent facts a client of the service is built from. */
    AwsServiceClientDefinition definition() {
        return definition;
    }

    /**
     * Installs the service's Modern Auth in a client's build state, as generated builders do: nothing for a service
     * without an AWS auth scheme.
     */
    void configure(AwsClientBuildState state) {
        boolean s3Express = authSchemes.containsKey(SIGV4_S3_EXPRESS);
        boolean aws = s3Express || authSchemes.containsKey(SIGV4) || authSchemes.containsKey(SIGV4A)
                      || authSchemes.containsKey(BEARER);
        if (!aws) {
            return;
        }
        state.authSchemeSelector(AuthSchemeSelector.create(s3Express ? AwsAuthSchemeOptions.s3Schemes()
                                                                     : AwsAuthSchemeOptions.defaultSchemes()));
        // The service-wide signer defaults of V2's legacy auth types, as aws-codegen emits them (S3 only).
        Map<SignerProperty<?>, Object> signerDefaults = S3.equals(shape.getId())
                                                        ? AwsAuthSchemeOptions.s3SignerDefaults() : Map.of();
        state.authOptions((auth, options) -> authOptions(auth, options, signerDefaults));
    }

    /**
     * The binding of the operation with exactly the modeled name {@code name}, built on first use.
     *
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException naming the service, the name and every
     *         operation name in lexical order, if the service has no such operation
     */
    DynamicOperationBinding operation(String name) {
        DynamicAwsOperation operation = service.operation(name);
        DynamicOperationBinding binding = operations.get(operation);
        return binding != null ? binding : operations.computeIfAbsent(operation, this::bind);
    }

    private static List<AuthSchemeOption> authOptions(ModeledAuth auth, CallOptions options,
                                                      Map<SignerProperty<?>, Object> signerDefaults) {
        return options.get(AwsClientOption.AWS_REGION)
                      .map(region -> AwsAuthSchemeOptions.modeled(auth, region.id(), null, signerDefaults))
                      .orElseGet(() -> AuthOptionsResolver.modeled().options(auth, options));
    }

    private DynamicOperationBinding bind(DynamicAwsOperation operation) {
        OperationShape operationShape = model.expectShape(operation.id(), OperationShape.class);
        AwsOperation.Builder<StructDocument, StructDocument> builder =
            AwsOperation.builder(operation.descriptor(), operation.inputBuilder(),
                                () -> DynamicDefaults.filling(operation.outputBuilder().get(), service.id()));
        if (rules != null) {
            builder.endpointBinding(endpointBinding(operation.name(), operationShape));
        }
        builder.auth(auth(operationShape));
        builder.requestView(memberOrder::request);
        operationShape.findTrait(HttpChecksumMetadata.TRAIT_ID)
                      .ifPresent(trait -> builder.httpChecksum(HttpChecksumMetadata.fromNode(trait.toNode())));
        DynamicInputValidator inputEvents = operation.schemas().inputStreamMember()
                                                     .filter(member -> member.type() == ShapeType.UNION)
                                                     .map(member -> DynamicInputValidator.of(model,
                                                                                             member.memberTarget(),
                                                                                             "event"))
                                                     .orElse(null);
        return new DynamicOperationBinding(service.id(), operation, builder.build(),
                                           DynamicInputValidator.of(model, operation.schemas().input()),
                                           inputEvents);
    }

    /**
     * The compiled waiter named exactly {@code name}, compiled on first use.
     *
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException naming the service, the name and every
     *         waiter name in lexical order, if the service has no such waiter
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#UNSUPPORTED_WAITER}) if an
     *         acceptor path is outside the supported subset
     */
    DynamicWaiters.Compiled waiter(String name) {
        DynamicWaiters.Compiled compiled = waiters.get(name);
        if (compiled != null) {
            return compiled;
        }
        DynamicWaiterDefinition definition = service.waiter(name);
        return waiters.computeIfAbsent(name, n -> DynamicWaiters.compile(this, definition));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Service definition

    /** V2's {@code SERVICE_NAME}: the {@code aws.api#service} endpoint prefix, else the lower-case service name. */
    private String serviceName() {
        String lowerName = shape.getId().getName().toLowerCase(Locale.ROOT);
        return shape.findTrait(SERVICE_TRAIT).flatMap(trait -> trait.toNode().asObjectNode()).map(
            node -> node.getStringMember("endpointPrefix")
                        .or(() -> node.getStringMember("arnNamespace"))
                        .map(StringNode::getValue)
                        .orElse(lowerName))
                    .orElse(lowerName);
    }

    /** The declared protocols and the protocol data of the model that runtime schemas do not carry. */
    private ServiceProtocols protocols() {
        ServiceProtocols.Builder protocols = ServiceProtocols.builder(service.schema());
        new TreeMap<>(ServiceIndex.of(model).getProtocols(shape)).forEach((id, trait) -> {
            ObjectNode node = trait.toNode().asObjectNode().orElse(Node.objectNode());
            boolean noErrorWrapping = id.equals(REST_XML)
                                      && node.getBooleanMemberOrDefault("noErrorWrapping", false);
            protocols.protocol(new ModeledProtocol(id, strings(node, "http"), strings(node, "eventStreamHttp"),
                                                   noErrorWrapping));
        });
        if (shape.hasTrait(AWS_QUERY_COMPATIBLE)) {
            protocols.awsQueryCompatible(true);
        }
        SortedMap<ShapeId, ObjectNode> queryErrors = new TreeMap<>();
        OperationIndex operationIndex = OperationIndex.of(model);
        List<StructureShape> errors = new ArrayList<>(operationIndex.getErrors(shape));
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(shape)) {
            errors.addAll(operationIndex.getErrors(shape, operation));
        }
        for (StructureShape error : errors) {
            error.findTrait(AWS_QUERY_ERROR).flatMap(trait -> trait.toNode().asObjectNode())
                 .ifPresent(node -> queryErrors.putIfAbsent(error.getId(), node));
        }
        queryErrors.forEach((error, node) -> protocols.awsQueryError(
            error, node.expectStringMember("code").getValue(),
            node.expectNumberMember("httpResponseCode").getValue().intValue()));
        boolean ec2Query = shape.hasTrait(EC2_QUERY);
        if (ec2Query || shape.hasTrait(AWS_QUERY)) {
            protocols.serviceVersion(shape.getVersion());
        }
        if (ec2Query) {
            SortedMap<ShapeId, String> names = new TreeMap<>();
            for (Shape walked : new Walker(model).walkShapes(shape)) {
                walked.asMemberShape().ifPresent(member -> member.findTrait(EC2_QUERY_NAME).ifPresent(
                    trait -> names.put(member.getId(), trait.toNode().expectStringNode().getValue())));
            }
            names.forEach(protocols::ec2QueryName);
        }
        return protocols.build();
    }

    /**
     * A registry of every structure and (non-streaming) union of the service closure, by exact shape ID: modeled
     * errors build {@link DynamicServiceException}s, other types schema-guided documents.
     */
    private SchemaTypeRegistry typeRegistry() {
        SortedMap<ShapeId, Schema> types = new TreeMap<>();
        List<Schema> candidates = new ArrayList<>(service.schemaIndex().types());
        candidates.addAll(service.schemaIndex().shapes());
        for (Schema schema : candidates) {
            boolean struct = schema.type() == ShapeType.STRUCTURE
                             || schema.type() == ShapeType.UNION && !schema.hasTrait(TraitKey.STREAMING_TRAIT);
            if (struct && !schema.isMember()) {
                types.putIfAbsent(schema.id(), schema);
            }
        }
        if (types.isEmpty()) {
            return SchemaTypeRegistry.builder().build();
        }
        ShapeId serviceId = service.id();
        List<ShapeId> ids = List.copyOf(types.keySet());
        return SchemaTypeRegistry.builder().partition(ids, () -> {
            List<RegisteredType<?>> entries = new ArrayList<>(ids.size());
            for (Schema schema : types.values()) {
                if (schema.hasTrait(TraitKey.ERROR_TRAIT)) {
                    entries.add(RegisteredType.of(schema, DynamicServiceException.class,
                                                  () -> DynamicServiceException.builder(schema, serviceId)));
                } else {
                    entries.add(RegisteredType.of(schema, StructDocument.class,
                                                  () -> DynamicDefaults.filling(
                                                      SchemaConverter.createDocumentBuilder(schema, serviceId),
                                                      serviceId)));
                }
            }
            return entries;
        }).build();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Operation bindings

    /** The operation's modeled auth, as generated bindings record it. */
    private ModeledAuth auth(OperationShape operation) {
        ModeledAuth.Builder auth = ModeledAuth.builder().operationId(operation.getId().toString());
        authSchemes.forEach((scheme, values) -> {
            if (values.isEmpty()) {
                auth.addServiceScheme(scheme);
            } else {
                auth.addServiceScheme(scheme, values);
            }
        });
        if (serviceAuthTrait != null) {
            auth.serviceAuthTrait(serviceAuthTrait);
        }
        operation.getTrait(AuthTrait.class).ifPresent(trait -> auth.operationAuthTrait(ids(trait)));
        return auth.optionalAuth(operation.hasTrait(OptionalAuthTrait.class))
                   .unsignedPayload(operation.hasTrait(UNSIGNED_PAYLOAD))
                   .build();
    }

    /** The operation's endpoint parameter binding, as generated endpoint rules declare it. */
    private EndpointBinding endpointBinding(String name, OperationShape operation) {
        EndpointBinding.Builder binding = EndpointBinding.builder(name);
        sortedObjectMembers(operation, STATIC_CONTEXT_PARAMS).forEach((param, definition) -> binding.staticContextParam(
            param, staticValue(operation, param, definition.expectObjectNode().expectMember("value"))));
        Shape input = model.expectShape(operation.getInputShape());
        List<String> discoveryIds = new ArrayList<>();
        for (MemberShape member : input.members()) {
            member.findTrait(CONTEXT_PARAM).ifPresent(trait -> binding.contextParam(
                trait.toNode().expectObjectNode().expectStringMember("name").getValue(), member.getMemberName()));
            if (member.hasTrait(CLIENT_ENDPOINT_DISCOVERY_ID)) {
                discoveryIds.add(member.getMemberName());
            }
        }
        sortedObjectMembers(operation, OPERATION_CONTEXT_PARAMS).forEach(
            (param, definition) -> binding.operationContextParam(
                param, definition.expectObjectNode().expectStringMember("path").getValue()));
        operation.getTrait(EndpointTrait.class)
                 .ifPresent(trait -> binding.hostPrefix(trait.getHostPrefix().toString()));
        Optional<ObjectNode> discovered = operation.findTrait(CLIENT_DISCOVERED_ENDPOINT)
                                                   .flatMap(trait -> trait.toNode().asObjectNode());
        discovered.ifPresent(node -> binding.discovery(node.getBooleanMemberOrDefault("required", false),
                                                       discoveryIds));
        return binding.build();
    }

    /** The members of the object value of {@code shape}'s trait {@code traitId}, sorted by name; empty without it. */
    private static SortedMap<String, Node> sortedObjectMembers(Shape shape, ShapeId traitId) {
        SortedMap<String, Node> members = new TreeMap<>();
        shape.findTrait(traitId).flatMap(trait -> trait.toNode().asObjectNode())
             .ifPresent(object -> members.putAll(object.getStringMap()));
        return members;
    }

    private static Object staticValue(OperationShape operation, String param, Node value) {
        if (value.isBooleanNode()) {
            return value.expectBooleanNode().getValue();
        }
        if (value.isStringNode()) {
            return value.expectStringNode().getValue();
        }
        if (value.isArrayNode()) {
            return value.expectArrayNode().getElements().stream()
                        .map(element -> element.expectStringNode().getValue()).toList();
        }
        throw new IllegalArgumentException("Static context parameter " + param + " of operation " + operation.getId()
                                           + " has an unsupported value " + Node.printJson(value));
    }

    private static List<String> ids(AuthTrait trait) {
        return trait.getValueSet().stream().map(ShapeId::toString).toList();
    }

    /** The string members of an auth-definition trait value, sorted by name, as generated bindings record them. */
    private static Map<String, String> stringMembers(Trait trait) {
        Map<String, String> values = new TreeMap<>();
        trait.toNode().asObjectNode().ifPresent(object -> object.getStringMap().forEach(
            (key, value) -> value.asStringNode().ifPresent(s -> values.put(key, s.getValue()))));
        return values;
    }

    private static List<String> strings(ObjectNode node, String member) {
        return node.getArrayMember(member)
                   .map(array -> array.getElements().stream().map(e -> e.expectStringNode().getValue()).toList())
                   .orElse(List.of());
    }
}
