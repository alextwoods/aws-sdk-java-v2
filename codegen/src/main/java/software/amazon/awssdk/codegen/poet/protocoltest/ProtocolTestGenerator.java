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

package software.amazon.awssdk.codegen.poet.protocoltest;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestsTrait;

/**
 * The Protocol_Test_Generator (design D.1): generates JUnit 5 tests from the Smithy {@code httpRequestTests} and
 * {@code httpResponseTests} traits of a service built from a Smithy model.
 *
 * <p>One {@code @Test} method is generated for each case whose {@code protocol} is a Supported_Protocol and whose
 * {@code appliesTo} is {@code client} or unset: request cases on the service's operations, response cases on its
 * operations, and response cases on its error shapes (Requirement 32.1). Each method has a {@code @DisplayName} equal
 * to the case id. Methods are grouped into classes by protocol and kind ({@code <Svc>RequestTests1},
 * {@code <Svc>ResponseTests1}, …, with the protocol name in the class name when the service has cases for more than one
 * protocol), at most {@value #DEFAULT_MAX_TESTS_PER_CLASS} per class, each class tagged
 * {@code @Tag("protocol:<protocol ShapeId>")}.
 *
 * <p>Every class gets its client through {@code ProtocolTestHarnesses.forClient} with a fixed region, fixed static
 * credentials (or anonymous credentials when the service has no SigV4 auth) and a no-retry strategy; request test
 * classes also fix the idempotency token for their duration.
 *
 * <p>A case listed in the skip list is generated with {@code @Disabled("<reason>")}. Everything is rendered when the
 * instance is created, so an unknown skip id or a selected case that is not skipped but cannot be generated fails with a
 * {@link ProtocolTestCodegenException} naming the ids, before any file is written (Requirements 32.6–32.8).
 */
@SdkInternalApi
public final class ProtocolTestGenerator {
    /** The most test methods in one generated class. */
    public static final int DEFAULT_MAX_TESTS_PER_CLASS = 150;

    /** The Supported_Protocols, by protocol trait ShapeId. */
    public static final Set<ShapeId> SUPPORTED_PROTOCOLS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        ShapeId.from("smithy.protocols#rpcv2Cbor"),
        ShapeId.from("aws.protocols#awsJson1_0"),
        ShapeId.from("aws.protocols#awsJson1_1"),
        ShapeId.from("aws.protocols#restJson1"),
        ShapeId.from("aws.protocols#restXml"),
        ShapeId.from("aws.protocols#awsQuery"),
        ShapeId.from("aws.protocols#ec2Query"))));

    private static final String DEFAULT_HOST = "example.com";
    private static final String HARNESS = "HARNESS";
    private static final String PREVIOUS_TOKEN_GENERATOR = "previousIdempotencyTokenGenerator";

    private final IntermediateModel intermediateModel;
    private final Model model;
    private final ServiceShape service;
    private final ShapeValueLiteralGenerator literals;
    private final ProtocolTestSkipList skips;
    private final int maxTestsPerClass;
    private final List<TestCase> cases;
    private final List<ClassSpec> classSpecs;

    private ProtocolTestGenerator(Builder builder) {
        this.intermediateModel = Objects.requireNonNull(builder.model, "model");
        this.model = Objects.requireNonNull(intermediateModel.smithyModel(),
                                            "Protocol tests are generated only for services built from a Smithy model");
        this.service = model.expectShape(Objects.requireNonNull(intermediateModel.serviceId(), "serviceId"),
                                         ServiceShape.class);
        this.skips = builder.skips == null ? ProtocolTestSkipList.empty() : builder.skips;
        if (builder.maxTestsPerClass < 1) {
            throw new IllegalArgumentException("maxTestsPerClass must be positive: " + builder.maxTestsPerClass);
        }
        this.maxTestsPerClass = builder.maxTestsPerClass;
        this.literals = new ShapeValueLiteralGenerator(intermediateModel);
        this.cases = Collections.unmodifiableList(selectCases());
        skips.validateAgainst(knownCases());
        this.classSpecs = Collections.unmodifiableList(render());
    }

    /**
     * Whether protocol tests are generated for {@code model}: its {@code customization.config} sets
     * {@code "protocolTests": true}.
     */
    public static boolean isEnabled(IntermediateModel model) {
        return model.getCustomizationConfig().isProtocolTests();
    }

    /**
     * Prepares protocol test generation for {@code model}, with the skip list its {@code customization.config} names in
     * {@code protocolTestSkips}.
     *
     * @throws ProtocolTestCodegenException if the service was not built from a Smithy model, the skip list is invalid
     *                                      or names an unknown case, or a case that is not skipped cannot be generated
     */
    public static ProtocolTestGenerator create(IntermediateModel model) {
        if (model.smithyModel() == null || model.serviceId() == null) {
            throw new ProtocolTestCodegenException("protocolTests is set for " + model.getMetadata().getServiceName()
                                                   + ", but protocol tests are generated only for services built from a "
                                                   + "Smithy model");
        }
        String skipFile = model.getCustomizationConfig().getProtocolTestSkips();
        ProtocolTestSkipList skips = skipFile == null ? ProtocolTestSkipList.empty()
                                                      : ProtocolTestSkipList.load(Paths.get(skipFile));
        return builder().model(model).skips(skips).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The generated test classes, request classes before response classes, per protocol in ShapeId order.
     */
    public List<ClassSpec> classSpecs() {
        return classSpecs;
    }

    /**
     * The ids of the selected test cases, in generation order.
     */
    public List<String> caseIds() {
        return cases.stream().map(c -> c.id).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Case selection
    // ---------------------------------------------------------------------------------------------------------------

    private List<TestCase> selectCases() {
        List<OperationShape> operations = new ArrayList<>(TopDownIndex.of(model).getContainedOperations(service));
        operations.sort(Comparator.comparing(Shape::getId));
        List<TestCase> result = new ArrayList<>();
        for (OperationShape operation : operations) {
            operation.getTrait(HttpRequestTestsTrait.class).ifPresent(trait -> {
                for (HttpRequestTestCase testCase : trait.getTestCases()) {
                    if (isSelected(testCase)) {
                        result.add(new TestCase(Kind.REQUEST, testCase, operation, null));
                    }
                }
            });
            operation.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
                for (HttpResponseTestCase testCase : trait.getTestCases()) {
                    if (isSelected(testCase)) {
                        result.add(new TestCase(Kind.RESPONSE, testCase, operation, null));
                    }
                }
            });
        }
        for (StructureShape error : errorShapes(operations)) {
            error.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
                for (HttpResponseTestCase testCase : trait.getTestCases()) {
                    if (isSelected(testCase)) {
                        result.add(new TestCase(Kind.ERROR, testCase, operationFor(error, operations), error));
                    }
                }
            });
        }
        return result;
    }

    private static boolean isSelected(HttpMessageTestCase testCase) {
        return SUPPORTED_PROTOCOLS.contains(testCase.getProtocol())
               && testCase.getAppliesTo().map(a -> a == AppliesTo.CLIENT).orElse(true);
    }

    /**
     * The service's error shapes: service-level errors and the errors of its operations, in ShapeId order.
     */
    private List<StructureShape> errorShapes(List<OperationShape> operations) {
        Set<ShapeId> ids = new TreeSet<>(service.getErrors());
        for (OperationShape operation : operations) {
            ids.addAll(operation.getErrors());
        }
        List<StructureShape> errors = new ArrayList<>();
        for (ShapeId id : ids) {
            model.getShape(id).flatMap(Shape::asStructureShape).ifPresent(errors::add);
        }
        return errors;
    }

    /**
     * The operation an error response case calls: the first operation, in ShapeId order, that declares the error (any
     * operation for a service-level error), preferring one whose input has no members, so an empty input is valid.
     * Null when the service has no operation.
     */
    private OperationShape operationFor(StructureShape error, List<OperationShape> operations) {
        List<OperationShape> candidates = operations.stream()
                                                    .filter(o -> o.getErrors().contains(error.getId()))
                                                    .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            candidates = operations;
        }
        for (OperationShape candidate : candidates) {
            Shape input = model.expectShape(candidate.getInputShape());
            if (input.members().isEmpty()) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private Map<String, Set<String>> knownCases() {
        Map<String, Set<String>> known = new LinkedHashMap<>();
        for (TestCase testCase : cases) {
            known.computeIfAbsent(testCase.id, k -> new HashSet<>()).add(testCase.protocol);
        }
        return known;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------------------------------

    private List<ClassSpec> render() {
        Map<String, List<MethodSpec>> requestTests = new TreeMap<>();
        Map<String, List<MethodSpec>> responseTests = new TreeMap<>();
        Map<String, Set<String>> methodNames = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        for (TestCase testCase : cases) {
            Optional<ProtocolTestSkipList.Entry> skip = skips.find(testCase.id, testCase.protocol);
            String group = (testCase.kind == Kind.REQUEST ? "request " : "response ") + testCase.protocol;
            String name = uniqueName(methodName(testCase), methodNames.computeIfAbsent(group, g -> new HashSet<>()));
            MethodSpec.Builder method = testMethod(name, testCase, skip);
            try {
                method.addCode(body(testCase));
            } catch (UngeneratableCaseException e) {
                if (!skip.isPresent()) {
                    failures.add(testCase.id + " (" + testCase.protocol + "): " + e.getMessage());
                    continue;
                }
                method.addComment("Not generated: $L", e.getMessage());
            }
            (testCase.kind == Kind.REQUEST ? requestTests : responseTests)
                .computeIfAbsent(testCase.protocol, p -> new ArrayList<>())
                .add(method.build());
        }
        if (!failures.isEmpty()) {
            throw new ProtocolTestCodegenException(
                "Protocol test cases of " + service.getId() + " cannot be generated. List each one in the protocol test "
                + "skip list with a reason, or fix the model:\n  " + String.join("\n  ", failures));
        }
        Set<String> protocols = new TreeSet<>(requestTests.keySet());
        protocols.addAll(responseTests.keySet());
        boolean qualify = protocols.size() > 1;
        List<ClassSpec> specs = new ArrayList<>();
        for (String protocol : protocols) {
            specs.addAll(classes(protocol, Kind.REQUEST, requestTests.getOrDefault(protocol, Collections.emptyList()),
                                 qualify));
            specs.addAll(classes(protocol, Kind.RESPONSE, responseTests.getOrDefault(protocol, Collections.emptyList()),
                                 qualify));
        }
        return specs;
    }

    private List<ClassSpec> classes(String protocol, Kind kind, List<MethodSpec> tests, boolean qualify) {
        List<ClassSpec> specs = new ArrayList<>();
        Metadata metadata = intermediateModel.getMetadata();
        String prefix = metadata.getServiceName()
                        + (qualify ? protocolClassPart(protocol) : "")
                        + (kind == Kind.REQUEST ? "RequestTests" : "ResponseTests");
        String packageName = metadata.getFullClientPackageName() + ".protocoltests";
        for (int start = 0, index = 1; start < tests.size(); start += maxTestsPerClass, index++) {
            List<MethodSpec> chunk = tests.subList(start, Math.min(tests.size(), start + maxTestsPerClass));
            ClassName className = ClassName.get(packageName, prefix + index);
            specs.add(new GeneratedTestClass(className, testClass(className, protocol, kind, chunk)));
        }
        return specs;
    }

    private static String protocolClassPart(String protocol) {
        String name = ShapeId.from(protocol).getName();
        return StringUtils.capitalize(name.replaceAll("[^A-Za-z0-9]", ""));
    }

    private TypeSpec testClass(ClassName className, String protocol, Kind kind, List<MethodSpec> tests) {
        TypeSpec.Builder type = PoetUtils.createClassBuilder(className)
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(AnnotationSpec.builder(Tag.class)
                                                                      .addMember("value", "$S",
                                                                                 ProtocolTestRuntimeTypes.PROTOCOL_TAG_PREFIX
                                                                                 + protocol)
                                                                      .build())
                                         .addField(harnessField(protocol));
        if (kind == Kind.REQUEST) {
            addIdempotencyTokenFixture(type);
        }
        type.addMethods(tests);
        return type.build();
    }

    private FieldSpec harnessField(String protocol) {
        Metadata metadata = intermediateModel.getMetadata();
        ClassName client = clientInterface();
        ClassName builder = ClassName.get(metadata.getFullClientPackageName(), metadata.getSyncBuilderInterface());
        CodeBlock credentials = hasSigV4()
                                ? CodeBlock.of("$T.create($T.create($S, $S))", StaticCredentialsProvider.class,
                                               AwsBasicCredentials.class, "akid", "secret")
                                : CodeBlock.of("$T.create()", AnonymousCredentialsProvider.class);
        CodeBlock initializer = CodeBlock.builder()
            .add("$T.<$T, $T>forClient($S, $T::builder,$>$>\n", ProtocolTestRuntimeTypes.PROTOCOL_TEST_HARNESSES, client,
                 builder, protocol, client)
            .add("b -> b.region($T.US_EAST_1)\n", Region.class)
            .add("      .credentialsProvider($L)\n", credentials)
            .add("      .overrideConfiguration(o -> o.retryStrategy($T.doNotRetry())))$<$<", AwsRetryStrategy.class)
            .build();
        return FieldSpec.builder(ParameterizedTypeName.get(ProtocolTestRuntimeTypes.PROTOCOL_TEST_HARNESS, client), HARNESS,
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(initializer)
                        .build();
    }

    private boolean hasSigV4() {
        return service.hasTrait(SigV4Trait.class) || service.hasTrait(SigV4ATrait.class);
    }

    private static void addIdempotencyTokenFixture(TypeSpec.Builder type) {
        TypeName supplier = ParameterizedTypeName.get(Supplier.class, String.class);
        type.addField(FieldSpec.builder(supplier, PREVIOUS_TOKEN_GENERATOR, Modifier.PRIVATE, Modifier.STATIC).build());
        type.addMethod(MethodSpec.methodBuilder("fixIdempotencyToken")
                                 .addAnnotation(BeforeAll.class)
                                 .addModifiers(Modifier.STATIC)
                                 .addStatement("$N = $T.getGenerator()", PREVIOUS_TOKEN_GENERATOR, IdempotentUtils.class)
                                 .addStatement("$T.setGenerator(() -> $S)", IdempotentUtils.class,
                                               ProtocolTestRuntimeTypes.IDEMPOTENCY_TOKEN)
                                 .build());
        type.addMethod(MethodSpec.methodBuilder("restoreIdempotencyToken")
                                 .addAnnotation(AfterAll.class)
                                 .addModifiers(Modifier.STATIC)
                                 .addStatement("$T.setGenerator($N)", IdempotentUtils.class, PREVIOUS_TOKEN_GENERATOR)
                                 .build());
    }

    private static MethodSpec.Builder testMethod(String name, TestCase testCase,
                                                 Optional<ProtocolTestSkipList.Entry> skip) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(name)
                                              .addAnnotation(Test.class)
                                              .addAnnotation(AnnotationSpec.builder(DisplayName.class)
                                                                           .addMember("value", "$S", testCase.id)
                                                                           .build());
        skip.ifPresent(s -> method.addAnnotation(AnnotationSpec.builder(Disabled.class)
                                                               .addMember("value", "$S", s.reason())
                                                               .build()));
        testCase.documentation.ifPresent(d -> method.addJavadoc("$L\n", d.trim()));
        return method;
    }

    private static String methodName(TestCase testCase) {
        String prefix = testCase.kind == Kind.REQUEST ? "request_" : testCase.kind == Kind.RESPONSE ? "response_" : "error_";
        return prefix + testCase.id.replaceAll("[^A-Za-z0-9_$]", "_");
    }

    private static String uniqueName(String name, Set<String> used) {
        String candidate = name;
        for (int i = 2; !used.add(candidate); i++) {
            candidate = name + "_" + i;
        }
        return candidate;
    }

    private CodeBlock body(TestCase testCase) {
        if (testCase.operation == null) {
            throw new UngeneratableCaseException("the service has no operation to call");
        }
        OperationModel operation = operationModel(testCase.operation);
        if (operation.hasEventStreamInput() || operation.hasEventStreamOutput()) {
            throw new UngeneratableCaseException("operation " + testCase.operation.getId().getName()
                                                 + " uses an event stream, which has no synchronous client method");
        }
        switch (testCase.kind) {
            case REQUEST:
                return requestBody((HttpRequestTestCase) testCase.testCase, testCase.operation, operation);
            case RESPONSE:
                return responseBody((HttpResponseTestCase) testCase.testCase, testCase.operation, operation);
            case ERROR:
                return errorBody((HttpResponseTestCase) testCase.testCase, testCase.errorShape, testCase.operation,
                                 operation);
            default:
                throw new IllegalStateException("Unknown kind " + testCase.kind);
        }
    }

    private OperationModel operationModel(OperationShape operation) {
        for (OperationModel candidate : intermediateModel.getOperations().values()) {
            if (operation.getId().equals(candidate.operationId())) {
                return candidate;
            }
        }
        throw new UngeneratableCaseException("operation " + operation.getId() + " has no generated client method");
    }

    private ClassName clientInterface() {
        Metadata metadata = intermediateModel.getMetadata();
        return ClassName.get(metadata.getFullClientPackageName(), metadata.getSyncInterface());
    }

    private CodeBlock requestBody(HttpRequestTestCase testCase, OperationShape operationShape, OperationModel operation) {
        CodeBlock.Builder code = CodeBlock.builder();
        Shape inputShape = model.expectShape(operationShape.getInputShape());
        ClassName inputClass = literals.modelClass(operation.getInputShape());
        code.addStatement("$T input = $L", inputClass,
                          literals.structure(operation.getInputShape(), inputShape, testCase.getParams(), false,
                                            operation.hasStreamingInput(), "params"));
        code.addStatement("$T request = $N.captureRequest(c -> c.$N($L), $S)", ProtocolTestRuntimeTypes.CAPTURED_REQUEST,
                          HARNESS, operation.getMethodName(), callArguments(operation, testCase.getParams()),
                          "https://" + testCase.getHost().orElse(DEFAULT_HOST));
        ClassName asserts = ProtocolTestRuntimeTypes.PROTOCOL_ASSERTS;
        code.addStatement("$T.method(request, $S)", asserts, testCase.getMethod());
        code.addStatement("$T.uri(request, $S)", asserts, testCase.getUri());
        addListAssertion(code, "queryParams", testCase.getQueryParams());
        addListAssertion(code, "forbidQueryParams", testCase.getForbidQueryParams());
        addListAssertion(code, "requireQueryParams", testCase.getRequireQueryParams());
        testCase.getResolvedHost().ifPresent(h -> code.addStatement("$T.resolvedHost(request, $S)", asserts, h));
        if (!testCase.getHeaders().isEmpty()) {
            code.addStatement("$T.headers(request, $L)", asserts, stringMap(testCase.getHeaders()));
        }
        addListAssertion(code, "forbidHeaders", testCase.getForbidHeaders());
        addListAssertion(code, "requireHeaders", testCase.getRequireHeaders());
        testCase.getBody().ifPresent(body -> code.addStatement("$T.body(request, $S, $S)", asserts, body,
                                                               testCase.getBodyMediaType().orElse(null)));
        return code.build();
    }

    /**
     * {@code input}, then the request body of a streaming input (the case's payload member, or an empty body), then a
     * {@code ResponseBytes} transformer for a streaming output.
     */
    private CodeBlock callArguments(OperationModel operation, ObjectNode params) {
        CodeBlock.Builder arguments = CodeBlock.builder().add("input");
        if (operation.hasStreamingInput()) {
            Optional<String> payload = ShapeValueLiteralGenerator.streamingMemberName(operation.getInputShape())
                                                                .flatMap(params::getStringMember)
                                                                .map(StringNode::getValue);
            if (payload.isPresent()) {
                arguments.add(", $T.fromBytes($T.fromUtf8String($S).asByteArray())", RequestBody.class, SdkBytes.class,
                              payload.get());
            } else {
                arguments.add(", $T.empty()", RequestBody.class);
            }
        }
        if (operation.hasStreamingOutput()) {
            arguments.add(", $T.<$T>toBytes()", ResponseTransformer.class,
                          literals.modelClass(operation.getOutputShape()));
        }
        return arguments.build();
    }

    private static void addListAssertion(CodeBlock.Builder code, String assertion, List<String> values) {
        if (!values.isEmpty()) {
            code.addStatement("$T.$N(request, $L)", ProtocolTestRuntimeTypes.PROTOCOL_ASSERTS, assertion,
                              stringList(values));
        }
    }

    private static CodeBlock stringList(List<String> values) {
        CodeBlock.Builder code = CodeBlock.builder().add("$T.asList(", Arrays.class);
        for (int i = 0; i < values.size(); i++) {
            code.add(i == 0 ? "$S" : ", $S", values.get(i));
        }
        return code.add(")").build();
    }

    private static CodeBlock stringMap(Map<String, String> values) {
        CodeBlock.Builder code = CodeBlock.builder().add("$T.<$T>mapOf(", ProtocolTestRuntimeTypes.PROTOCOL_TEST_VALUES,
                                                         String.class);
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            code.add(first ? "$S, $S" : ", $S, $S", entry.getKey(), entry.getValue());
            first = false;
        }
        return code.add(")").build();
    }

    private CodeBlock stubbedResponse(HttpResponseTestCase testCase) {
        return CodeBlock.of("$T.fromTestCase($L, $L, $S, $S)", ProtocolTestRuntimeTypes.STUBBED_RESPONSE,
                            testCase.getCode(), stringMap(testCase.getHeaders()), testCase.getBody().orElse(null),
                            testCase.getBodyMediaType().orElse(null));
    }

    private CodeBlock streamingArguments(OperationModel operation) {
        CodeBlock.Builder arguments = CodeBlock.builder();
        if (operation.hasStreamingInput()) {
            arguments.add(", $T.empty()", RequestBody.class);
        }
        if (operation.hasStreamingOutput()) {
            arguments.add(", $T.<$T>toBytes()", ResponseTransformer.class,
                          literals.modelClass(operation.getOutputShape()));
        }
        return arguments.build();
    }

    private CodeBlock responseBody(HttpResponseTestCase testCase, OperationShape operationShape, OperationModel operation) {
        CodeBlock.Builder code = CodeBlock.builder();
        ShapeModel output = operation.getOutputShape();
        Shape outputShape = model.expectShape(operationShape.getOutputShape());
        ClassName outputClass = literals.modelClass(output);
        code.addStatement("$T expected = $L", outputClass,
                          literals.structure(output, outputShape, testCase.getParams(), false, operation.hasStreamingOutput(),
                                            "params"));
        code.addStatement("$T response = $L", ProtocolTestRuntimeTypes.STUBBED_RESPONSE, stubbedResponse(testCase));
        CodeBlock call = CodeBlock.of("c -> c.$N($L)", operation.getMethodName(), emptyInputCallArguments(operation));
        if (operation.hasStreamingOutput()) {
            code.addStatement("$T actual = $N.respondWith(response, $L)",
                              ParameterizedTypeName.get(ClassName.get(ResponseBytes.class), outputClass), HARNESS, call);
            code.addStatement("$T.outputEquals(expected, actual.response())", ProtocolTestRuntimeTypes.PROTOCOL_ASSERTS);
            Optional<String> payload = ShapeValueLiteralGenerator.streamingMemberName(output)
                                                                .flatMap(testCase.getParams()::getStringMember)
                                                                .map(StringNode::getValue);
            payload.ifPresent(p -> code.addStatement("$T.outputEquals($T.fromUtf8String($S), $T.fromByteArray("
                                                     + "actual.asByteArray()))",
                                                     ProtocolTestRuntimeTypes.PROTOCOL_ASSERTS, SdkBytes.class, p,
                                                     SdkBytes.class));
        } else {
            code.addStatement("$T actual = $N.respondWith(response, $L)", outputClass, HARNESS, call);
            code.addStatement("$T.outputEquals(expected, actual)", ProtocolTestRuntimeTypes.PROTOCOL_ASSERTS);
        }
        return code.build();
    }

    private CodeBlock errorBody(HttpResponseTestCase testCase, StructureShape errorShape, OperationShape operationShape,
                                OperationModel operation) {
        ShapeModel error = intermediateModel.getShape(errorShape.getId());
        if (error == null) {
            throw new UngeneratableCaseException("error " + errorShape.getId() + " has no generated exception class");
        }
        ClassName errorClass = literals.modelClass(error);
        CodeBlock.Builder code = CodeBlock.builder();
        code.addStatement("$T expected = $L", errorClass,
                          literals.structure(error, errorShape, testCase.getParams(), true, false, "params"));
        code.addStatement("$T response = $L", ProtocolTestRuntimeTypes.STUBBED_RESPONSE, stubbedResponse(testCase));
        code.addStatement("$T actual = $T.assertThrows($T.class, () -> $N.respondWith(response, c -> c.$N($L)))",
                          errorClass, Assertions.class, errorClass, HARNESS, operation.getMethodName(),
                          emptyInputCallArguments(operation));
        code.addStatement("$T.outputEquals(expected, actual)", ProtocolTestRuntimeTypes.PROTOCOL_ASSERTS);
        return code.build();
    }

    /**
     * Arguments for a call whose input has no members set: an empty input, an empty request body for a streaming
     * input, and a {@code ResponseBytes} transformer for a streaming output. Response tests check the output, not the
     * request, so the input is not taken from the case.
     */
    private CodeBlock emptyInputCallArguments(OperationModel operation) {
        return CodeBlock.builder()
                        .add("$T.builder().build()", literals.modelClass(operation.getInputShape()))
                        .add(streamingArguments(operation))
                        .build();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Types
    // ---------------------------------------------------------------------------------------------------------------

    private enum Kind {
        REQUEST,
        RESPONSE,
        ERROR
    }

    private static final class TestCase {
        private final Kind kind;
        private final HttpMessageTestCase testCase;
        private final OperationShape operation;
        private final StructureShape errorShape;
        private final String id;
        private final String protocol;
        private final Optional<String> documentation;

        private TestCase(Kind kind, HttpMessageTestCase testCase, OperationShape operation, StructureShape errorShape) {
            this.kind = kind;
            this.testCase = testCase;
            this.operation = operation;
            this.errorShape = errorShape;
            this.id = testCase.getId();
            this.protocol = testCase.getProtocol().toString();
            this.documentation = testCase.getDocumentation();
        }
    }

    private static final class GeneratedTestClass implements ClassSpec {
        private final ClassName className;
        private final TypeSpec spec;

        private GeneratedTestClass(ClassName className, TypeSpec spec) {
            this.className = className;
            this.spec = spec;
        }

        @Override
        public TypeSpec poetSpec() {
            return spec;
        }

        @Override
        public ClassName className() {
            return className;
        }
    }

    /**
     * Inputs of a {@link ProtocolTestGenerator}.
     */
    public static final class Builder {
        private IntermediateModel model;
        private ProtocolTestSkipList skips;
        private int maxTestsPerClass = DEFAULT_MAX_TESTS_PER_CLASS;

        private Builder() {
        }

        /**
         * An intermediate model built from a Smithy model.
         */
        public Builder model(IntermediateModel model) {
            this.model = model;
            return this;
        }

        public Builder skips(ProtocolTestSkipList skips) {
            this.skips = skips;
            return this;
        }

        /**
         * The most test methods in one class; {@value #DEFAULT_MAX_TESTS_PER_CLASS} by default.
         */
        public Builder maxTestsPerClass(int maxTestsPerClass) {
            this.maxTestsPerClass = maxTestsPerClass;
            return this;
        }

        public ProtocolTestGenerator build() {
            return new ProtocolTestGenerator(this);
        }
    }
}
