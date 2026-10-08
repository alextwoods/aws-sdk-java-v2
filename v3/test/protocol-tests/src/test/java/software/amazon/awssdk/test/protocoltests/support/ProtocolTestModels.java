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

package software.amazon.awssdk.test.protocoltests.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpMalformedRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpMalformedRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestsTrait;
import software.amazon.smithy.protocoltests.traits.eventstream.EventStreamTestCase;
import software.amazon.smithy.protocoltests.traits.eventstream.EventStreamTestsTrait;

/**
 * The pinned Smithy protocol test models ({@code smithy-aws-protocol-tests} and {@code smithy-protocol-tests} at the
 * pinned Smithy release), their cases, and the codegen input generated from them.
 *
 * <p>The jars are read from {@code v3.protocolTests.models} (copied there by the build, off the test class path) and
 * assembled with the trait definitions on the test class path. Every case of every service in them is discovered:
 * {@code httpRequestTests} and {@code httpResponseTests} of operations, {@code httpResponseTests} of every error an
 * operation binds (one case per operation and error), {@code httpMalformedRequestTests} and
 * {@code eventStreamTests}. A case does not apply to a V3 client, with a recorded reason, only when it is a server
 * case ({@code appliesTo: server} or a malformed-request case, which Smithy defines for servers) or targets a protocol
 * that is not one of the seven V3 launch protocols (Requirement 5.5).
 *
 * <p>The codegen input ({@link #writeCodegenModel}) is the JSON AST of exactly the shapes defined by the pinned jars,
 * with two harness bindings the test services do not model and every generated client requires; neither changes the
 * protocol behavior under test:
 * <ul>
 *   <li>a service without an endpoint rule set receives one that resolves exactly the caller's endpoint (the
 *       {@code SDK::Endpoint} built-in), because Smithy protocol tests give the endpoint host per case; the AmazonS3
 *       test service instead receives the canonical S3 service's endpoint rule set, client context parameters and the
 *       context parameters of its same-named operations and input members, so its virtual-host addressing cases run
 *       through the canonical S3 rules;</li>
 *   <li>a service without any auth scheme or {@code @auth} trait receives {@code @auth([])} (anonymous): the
 *       protocol tests do not model authentication for it, and codegen requires modeled auth semantics.</li>
 * </ul>
 */
public final class ProtocolTestModels {
    /** The seven V3 launch protocols (Requirement 5.5). */
    public static final Set<ShapeId> LAUNCH_PROTOCOLS = Set.of(
        ProtocolIds.AWS_JSON_1_0, ProtocolIds.AWS_JSON_1_1, ProtocolIds.REST_JSON_1, ProtocolIds.REST_XML,
        ProtocolIds.AWS_QUERY, ProtocolIds.EC2_QUERY, ProtocolIds.RPC_V2_CBOR);

    /** The pinned artifacts whose shapes are protocol test models. */
    private static final List<String> TEST_MODEL_ARTIFACTS = List.of("smithy-aws-protocol-tests",
                                                                      "smithy-protocol-tests");

    static final ShapeId AMAZON_S3 = ShapeId.from("com.amazonaws.s3#AmazonS3");
    private static final ShapeId ENDPOINT_RULE_SET = ShapeId.from("smithy.rules#endpointRuleSet");
    private static final ShapeId CLIENT_CONTEXT_PARAMS = ShapeId.from("smithy.rules#clientContextParams");
    private static final ShapeId CONTEXT_PARAM = ShapeId.from("smithy.rules#contextParam");
    private static final List<ShapeId> OPERATION_ENDPOINT_TRAITS = List.of(
        ShapeId.from("smithy.rules#staticContextParams"), ShapeId.from("smithy.rules#operationContextParams"));

    /** Resolves exactly the caller's endpoint; the harness sets an endpoint override for every call. */
    private static final String CALLER_ENDPOINT_RULES = """
        {
          "version": "1.0",
          "parameters": {
            "Endpoint": {"builtIn": "SDK::Endpoint", "required": false, "type": "String",
                         "documentation": "The endpoint the protocol test case gives."}
          },
          "rules": [
            {"conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]}],
             "endpoint": {"url": {"ref": "Endpoint"}}, "type": "endpoint"},
            {"conditions": [], "error": "The protocol conformance harness sets an endpoint for every call.",
             "type": "error"}
          ]
        }
        """;

    private final List<Path> jars;
    private final Model model;
    private final List<ProtocolCase> cases;

    private ProtocolTestModels(List<Path> jars, Model model) {
        this.jars = jars;
        this.model = model;
        this.cases = discover(model);
    }

    /** Loads the pinned jars from the directory named by system property {@code v3.protocolTests.models}. */
    public static ProtocolTestModels load() {
        Path directory = Path.of(Conformance.requiredProperty("v3.protocolTests.models"));
        String version = Conformance.requiredProperty("v3.protocolTests.smithyVersion");
        List<Path> jars = new ArrayList<>();
        for (String artifact : TEST_MODEL_ARTIFACTS) {
            Path jar = directory.resolve(artifact + "-" + version + ".jar");
            if (!Files.isRegularFile(jar)) {
                throw new IllegalStateException("The pinned protocol test model " + jar + " is missing; the build "
                                                + "copies it in generate-test-resources.");
            }
            jars.add(jar);
        }
        ClassLoader loader = ProtocolTestModels.class.getClassLoader();
        ModelAssembler assembler = Model.assembler(loader).discoverModels(loader);
        jars.forEach(assembler::addImport);
        return new ProtocolTestModels(List.copyOf(jars), assembler.assemble().unwrap());
    }

    public Model model() {
        return model;
    }

    /** The pinned jars, for the report. */
    public List<Path> jars() {
        return jars;
    }

    /** Every case of every test service, sorted by key. */
    public List<ProtocolCase> cases() {
        return cases;
    }

    /** The services with at least one applicable case, sorted by shape ID. */
    public List<ServiceShape> servicesUnderTest() {
        Set<ShapeId> ids = cases.stream().filter(ProtocolCase::applicable).map(ProtocolCase::service)
                                .collect(Collectors.toCollection(TreeSet::new));
        return ids.stream().map(id -> model.expectShape(id, ServiceShape.class)).toList();
    }

    private static List<ProtocolCase> discover(Model model) {
        List<ProtocolCase> out = new ArrayList<>();
        ServiceIndex services = ServiceIndex.of(model);
        TopDownIndex topDown = TopDownIndex.of(model);
        for (ServiceShape service : model.getServiceShapes()) {
            if (!fromPinnedJars(service)) {
                continue;
            }
            Set<ShapeId> serviceProtocols = services.getProtocols(service).keySet();
            for (OperationShape operation : topDown.getContainedOperations(service)) {
                operation.getTrait(HttpRequestTestsTrait.class).ifPresent(trait -> trait.getTestCases().forEach(
                    c -> out.add(http(ProtocolCase.Kind.REQUEST, service, serviceProtocols, operation, null, c))));
                operation.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> trait.getTestCases().forEach(
                    c -> out.add(http(ProtocolCase.Kind.RESPONSE, service, serviceProtocols, operation, null, c))));
                for (ShapeId errorId : operation.getErrors(service)) {
                    StructureShape error = model.expectShape(errorId, StructureShape.class);
                    error.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> trait.getTestCases().forEach(
                        c -> out.add(http(ProtocolCase.Kind.ERROR, service, serviceProtocols, operation, errorId, c))));
                }
                operation.getTrait(HttpMalformedRequestTestsTrait.class).ifPresent(trait -> {
                    for (HttpMalformedRequestTestCase c : trait.getTestCases()) {
                        out.add(new ProtocolCase(ProtocolCase.Kind.MALFORMED_REQUEST, service.getId(), c.getProtocol(),
                                                 operation.getId(), null, c.getId(), null, null,
                                                 "httpMalformedRequestTests define server behavior for malformed "
                                                 + "requests; a client never receives a request"));
                    }
                });
                operation.getTrait(EventStreamTestsTrait.class).ifPresent(trait -> {
                    for (EventStreamTestCase c : trait.getTestCases()) {
                        String reason = c.getAppliesTo().filter(a -> a == AppliesTo.SERVER).isPresent()
                                        ? serverOnly() : protocolReason(c.getProtocol(), serviceProtocols);
                        out.add(new ProtocolCase(ProtocolCase.Kind.EVENT_STREAM, service.getId(), c.getProtocol(),
                                                 operation.getId(), null, c.getId(), null, c, reason));
                    }
                });
            }
        }
        out.sort(Comparator.comparing(ProtocolCase::key));
        return List.copyOf(out);
    }

    private static ProtocolCase http(ProtocolCase.Kind kind, ServiceShape service, Set<ShapeId> serviceProtocols,
                                     OperationShape operation, ShapeId error, HttpMessageTestCase c) {
        String reason = c.getAppliesTo().filter(a -> a == AppliesTo.SERVER).isPresent()
                        ? serverOnly() : protocolReason(c.getProtocol(), serviceProtocols);
        return new ProtocolCase(kind, service.getId(), c.getProtocol(), operation.getId(), error, c.getId(), c, null,
                                reason);
    }

    private static String serverOnly() {
        return "a server case (appliesTo: server)";
    }

    private static String protocolReason(ShapeId protocol, Set<ShapeId> serviceProtocols) {
        if (!LAUNCH_PROTOCOLS.contains(protocol)) {
            return "protocol " + protocol + " is not a V3 launch protocol (Requirement 5.5), so no V3 runtime exists";
        }
        if (!serviceProtocols.contains(protocol)) {
            return "the service does not declare protocol " + protocol;
        }
        return null;
    }

    /** Whether {@code shape} is defined by one of the pinned protocol test jars. */
    static boolean fromPinnedJars(Shape shape) {
        String file = shape.getSourceLocation().getFilename();
        return TEST_MODEL_ARTIFACTS.stream().anyMatch(artifact -> file.contains("/" + artifact + "-"));
    }

    /**
     * Writes the codegen input for {@code servicesUnderTest} to {@code directory/model.json}: the shapes of the pinned
     * jars with the harness bindings described on this class.
     *
     * @param canonicalS3 the canonical S3 model, whose endpoint semantics the AmazonS3 test service receives
     */
    public Path writeCodegenModel(Path directory, Model canonicalS3) {
        Model prepared = model;
        for (ServiceShape service : servicesUnderTest()) {
            prepared = prepare(prepared, service.getId(), canonicalS3);
        }
        ObjectNode ast = ModelSerializer.builder()
                                        .shapeFilter(ProtocolTestModels::fromPinnedJars)
                                        .metadataFilter(key -> key.equals("suppressions") || key.equals("validators"))
                                        .build()
                                        .serialize(prepared);
        ast = withSuppression(ast, Node.objectNodeBuilder()
            .withMember("id", "RuleSetAuthSchemes")
            .withMember("namespace", AMAZON_S3.getNamespace())
            .withMember("reason", "The canonical S3 endpoint rules grafted onto the AmazonS3 protocol test service use "
                                  + "sigv4-s3express auth sub-schemes, whose validation Smithy reports as subject to "
                                  + "change (DANGER); the canonical S3 module generates from the same rules.")
            .build());
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve("model.json");
            Files.writeString(file, Node.prettyPrintJson(ast), StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectNode withSuppression(ObjectNode ast, ObjectNode suppression) {
        ObjectNode metadata = ast.getObjectMember("metadata").orElse(Node.objectNode());
        List<Node> suppressions = new ArrayList<>(metadata.getArrayMember("suppressions")
                                                          .map(a -> a.getElements()).orElse(List.of()));
        suppressions.add(suppression);
        return ast.withMember("metadata", metadata.withMember("suppressions", Node.fromNodes(suppressions)));
    }

    private static Model prepare(Model model, ShapeId serviceId, Model canonicalS3) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        List<Shape> replaced = new ArrayList<>();
        ServiceShape.Builder builder = service.toBuilder();
        if (serviceId.equals(AMAZON_S3)) {
            ServiceShape s3 = canonicalS3.getServiceShapes().stream()
                                         .filter(s -> s.findTrait(ENDPOINT_RULE_SET).isPresent())
                                         .findFirst().orElseThrow();
            copyTrait(s3, ENDPOINT_RULE_SET, builder::addTrait);
            List<Shape> operations = s3OperationBindings(model, service, canonicalS3, s3);
            builder.addTrait(new DynamicTrait(CLIENT_CONTEXT_PARAMS, s3ClientContextParams(s3, operations)));
            replaced.addAll(operations);
        } else if (!service.hasTrait(ENDPOINT_RULE_SET)) {
            builder.addTrait(new DynamicTrait(ENDPOINT_RULE_SET, Node.parse(CALLER_ENDPOINT_RULES)));
        }
        if (ServiceIndex.of(model).getAuthSchemes(service).isEmpty() && !service.hasTrait(AuthTrait.class)) {
            builder.addTrait(new DynamicTrait(AuthTrait.ID, Node.arrayNode()));
        }
        replaced.add(builder.build());
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    /**
     * The operations of the AmazonS3 test service, and their input members, with the endpoint context bindings of the
     * canonical S3 operations and input members of the same names.
     */
    private static List<Shape> s3OperationBindings(Model model, ServiceShape service, Model canonical,
                                                   ServiceShape s3) {
        Map<String, OperationShape> canonicalOperations =
            TopDownIndex.of(canonical).getContainedOperations(s3).stream()
                        .collect(Collectors.toMap(o -> o.getId().getName(), Function.identity()));
        List<Shape> out = new ArrayList<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            OperationShape source = canonicalOperations.get(operation.getId().getName());
            if (source == null) {
                continue;
            }
            OperationShape.Builder copy = operation.toBuilder();
            OPERATION_ENDPOINT_TRAITS.forEach(id -> copyTrait(source, id, copy::addTrait));
            out.add(copy.build());
            StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);
            StructureShape canonicalInput = canonical.expectShape(source.getInputShape(), StructureShape.class);
            StructureShape.Builder inputCopy = input.toBuilder();
            boolean changed = false;
            for (MemberShape member : input.getAllMembers().values()) {
                MemberShape canonicalMember = canonicalInput.getAllMembers().get(member.getMemberName());
                if (canonicalMember != null && canonicalMember.findTrait(CONTEXT_PARAM).isPresent()) {
                    MemberShape.Builder memberCopy = member.toBuilder();
                    copyTrait(canonicalMember, CONTEXT_PARAM, memberCopy::addTrait);
                    inputCopy.addMember(memberCopy.build());
                    changed = true;
                }
            }
            if (changed) {
                out.add(inputCopy.build());
            }
        }
        return out;
    }

    /**
     * The canonical S3 client context parameters plus every rule-set parameter that canonical S3 binds only on
     * operations or members the AmazonS3 test service does not have (so it is unset for the test operations, exactly
     * as for the same canonical operations); without a binding, rule-set validation rejects the service.
     */
    private static ObjectNode s3ClientContextParams(ServiceShape s3, List<Shape> boundShapes) {
        ObjectNode params = s3.findTrait(CLIENT_CONTEXT_PARAMS).map(t -> t.toNode().expectObjectNode())
                              .orElse(Node.objectNode());
        Set<String> bound = new TreeSet<>(params.getStringMap().keySet());
        for (Shape shape : boundShapes) {
            shape.findTrait(OPERATION_ENDPOINT_TRAITS.get(0))
                 .ifPresent(t -> bound.addAll(t.toNode().expectObjectNode().getStringMap().keySet()));
            shape.findTrait(OPERATION_ENDPOINT_TRAITS.get(1))
                 .ifPresent(t -> bound.addAll(t.toNode().expectObjectNode().getStringMap().keySet()));
            for (MemberShape member : shape.members()) {
                member.findTrait(CONTEXT_PARAM).ifPresent(
                    t -> bound.add(t.toNode().expectObjectNode().expectStringMember("name").getValue()));
            }
        }
        ObjectNode.Builder out = params.toBuilder();
        ObjectNode ruleSetParameters = s3.findTrait(ENDPOINT_RULE_SET).orElseThrow().toNode().expectObjectNode()
                                         .expectObjectMember("parameters");
        ruleSetParameters.getStringMap().forEach((name, definition) -> {
            ObjectNode parameter = definition.expectObjectNode();
            if (!parameter.containsMember("builtIn") && !bound.contains(name)) {
                out.withMember(name, Node.objectNodeBuilder()
                                         .withMember("type", clientContextType(
                                             parameter.expectStringMember("type").getValue()))
                                         .withMember("documentation", "Unset for the AmazonS3 protocol test "
                                                                      + "operations, as for the same canonical S3 "
                                                                      + "operations.")
                                         .build());
            }
        });
        return out.build();
    }

    /** The {@code clientContextParams} type of a rule-set parameter type. */
    private static String clientContextType(String ruleSetType) {
        return switch (ruleSetType) {
            case "String", "string" -> "string";
            case "Boolean", "boolean" -> "boolean";
            default -> ruleSetType;
        };
    }

    private static void copyTrait(Shape source, ShapeId trait, java.util.function.Consumer<Trait> target) {
        source.findTrait(trait).ifPresent(t -> target.accept(new DynamicTrait(trait, t.toNode())));
    }

    /**
     * Loads a canonical model file (JSON AST) with the trait definitions on the test class path. It is only read for
     * trait values, so it is not validated (service generation validates it).
     */
    public static Model loadCanonical(Path file) {
        ClassLoader loader = ProtocolTestModels.class.getClassLoader();
        return Model.assembler(loader).discoverModels(loader)
                    .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                    .disableValidation()
                    .addImport(file).assemble().unwrap();
    }
}
