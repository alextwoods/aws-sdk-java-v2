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

package software.amazon.awssdk.protocols.awsjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestsTrait;

/**
 * Runs the official Smithy {@code smithy-aws-protocol-tests} client request, response and error cases of
 * {@code awsJson1_0} and {@code awsJson1_1} against this runtime, with Smithy Java dynamic schemas standing in for
 * generated types (the full generated-client conformance runners are task 18.1).
 *
 * <p>Cases that exercise features outside the protocol runtime are skipped with their reason: client-side request
 * compression (pipeline), default-value population and required-member error correction (generated builders) and
 * service-specific customizations (MachineLearning's PredictEndpoint).
 */
class AwsJsonProtocolConformanceTest {
    private static final String GENERATED_BUILDER = "default-value population and required-member error correction "
                                                    + "are generated-builder behavior, which the dynamic schema "
                                                    + "builders of this runner do not apply";

    /** Test case ID to the reason it is skipped. */
    private static final Map<String, String> SKIPPED = Map.ofEntries(
        Map.entry("MachinelearningPredictEndpoint",
                  "service customization (PredictEndpoint host override) belongs to the MachineLearning client"),
        Map.entry("AwsJson10ClientPopulatesDefaultValuesInInput",
                  GENERATED_BUILDER),
        Map.entry("AwsJson10ClientPopulatesDefaultsValuesWhenMissingInResponse",
                  GENERATED_BUILDER),
        Map.entry("AwsJson10ClientPopulatesNestedDefaultValuesWhenMissing",
                  GENERATED_BUILDER),
        Map.entry("AwsJson10ClientPopulatesNestedDefaultsWhenMissingInResponseBody",
                  GENERATED_BUILDER),
        Map.entry("AwsJson10ClientErrorCorrectsWhenServerFailsToSerializeRequiredValues",
                  GENERATED_BUILDER),
        Map.entry("AwsJson10ClientErrorCorrectsWithDefaultValuesWhenServerFailsToSerializeRequiredValues",
                  GENERATED_BUILDER));

    private static final List<ShapeId> PROTOCOLS = List.of(ProtocolIds.AWS_JSON_1_0, ProtocolIds.AWS_JSON_1_1);

    private static Model model;

    @BeforeAll
    static void loadModels() {
        model = Model.assembler(AwsJsonProtocolConformanceTest.class.getClassLoader())
                     .discoverModels(AwsJsonProtocolConformanceTest.class.getClassLoader())
                     .assemble()
                     .unwrap();
    }

    @TestFactory
    Stream<DynamicTest> officialAwsJsonProtocolTests() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapes()) {
            for (ShapeId protocolId : PROTOCOLS) {
                if (service.findTrait(protocolId).isEmpty()) {
                    continue;
                }
                ProtocolProvider provider = protocolId.equals(ProtocolIds.AWS_JSON_1_0)
                                            ? new AwsJson10ProtocolProvider() : new AwsJson11ProtocolProvider();
                DynamicService dynamic = new DynamicService(model, service.getId());
                ClientProtocol client = dynamic.client(provider, true);
                for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
                    addOperationTests(tests, dynamic, client, protocolId, operation);
                }
            }
        }
        assertFalse(tests.isEmpty(), "No awsJson protocol tests were found");
        return tests.stream();
    }

    private static void addOperationTests(List<DynamicTest> tests, DynamicService dynamic, ClientProtocol client,
                                          ShapeId protocolId, OperationShape operation) {
        OperationProtocol plan = client.operation(dynamic.schemas(operation));
        operation.getTrait(HttpRequestTestsTrait.class).ifPresent(trait -> {
            for (HttpRequestTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(protocolId)) {
                    tests.add(DynamicTest.dynamicTest("request " + test.getId(),
                                                      located(test, () -> runRequest(dynamic, operation, plan, test))));
                }
            }
        });
        operation.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
            for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(protocolId)) {
                    tests.add(DynamicTest.dynamicTest("response " + test.getId(),
                                                      located(test, () -> runResponse(dynamic, operation, plan, test))));
                }
            }
        });
        for (ShapeId errorId : operation.getErrors(dynamic.service())) {
            StructureShape error = dynamic.model().expectShape(errorId, StructureShape.class);
            error.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
                for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                    if (test.getProtocol().equals(protocolId)) {
                        tests.add(DynamicTest.dynamicTest("error " + operation.getId().getName() + " " + test.getId(),
                                                          located(test, () -> runError(dynamic, error, plan, test))));
                    }
                }
            });
        }
    }

    /** Prefixes an assertion failure with the protocol test case ID (dynamic test names are not in reports). */
    private static Executable located(HttpMessageTestCase test, Executable body) {
        return () -> {
            try {
                body.execute();
            } catch (AssertionFailedError e) {
                throw new AssertionFailedError(test.getId() + ": " + e.getMessage(), e.getExpected(), e.getActual(),
                                               e);
            }
        };
    }

    private static void skipIfUnsupported(HttpMessageTestCase test) {
        String reason = SKIPPED.get(test.getId());
        Assumptions.assumeTrue(reason == null, () -> "Skipped: " + reason);
        Assumptions.assumeTrue(test.getVendorParams().isEmpty() || !test.getId().contains("Compression"),
                               "Skipped: request compression is a pipeline feature");
    }

    private static void runRequest(DynamicService dynamic, OperationShape operation, OperationProtocol plan,
                                   HttpRequestTestCase test) {
        skipIfUnsupported(test);
        Assumptions.assumeFalse(operation.hasTrait(ShapeId.from("smithy.api#requestCompression")),
                                "Skipped: request compression is a pipeline feature");
        StructDocument input = dynamic.value(dynamic.model().expectShape(operation.getInputShape()), test.getParams());
        String host = test.getHost().orElse("example.com");
        SdkHttpRequest request = plan.serializeRequest(input, URI.create("https://" + host));
        assertEquals(test.getMethod(), request.method(), "method");
        assertEquals(test.getUri(), request.encodedPath(), "uri");
        test.getResolvedHost().ifPresent(resolved -> assertEquals(resolved, request.host(), "resolved host"));
        for (Map.Entry<String, String> header : test.getHeaders().entrySet()) {
            assertEquals(Optional.of(header.getValue()), request.headers().firstValue(header.getKey()),
                         "header " + header.getKey());
        }
        for (String forbidden : test.getForbidHeaders()) {
            assertFalse(request.headers().contains(forbidden), "forbidden header " + forbidden);
        }
        for (String required : test.getRequireHeaders()) {
            if (!required.equalsIgnoreCase("Content-Length")) { // a transport header, set by the HTTP client
                assertTrue(request.headers().contains(required), "required header " + required);
            }
        }
        test.getBody().ifPresent(expected -> assertBody(expected, test.getBodyMediaType().orElse(""),
                                                        request.body().asUtf8String()));
    }

    private static void runResponse(DynamicService dynamic, OperationShape operation, OperationProtocol plan,
                                    HttpResponseTestCase test) {
        skipIfUnsupported(test);
        StructureShape outputShape = dynamic.model().expectShape(operation.getOutputShape(), StructureShape.class);
        StructDocument actual = plan.deserializeResponse(response(test), dynamic.builder(outputShape));
        StructDocument expected = dynamic.value(outputShape, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)), normalize(DynamicService.canonical(actual)), "output");
    }

    private static void runError(DynamicService dynamic, StructureShape error, OperationProtocol plan,
                                 HttpResponseTestCase test) {
        skipIfUnsupported(test);
        AwsServiceException exception = plan.deserializeError(response(test));
        DynamicError modeled = assertInstanceOf(DynamicError.class, exception, "modeled error");
        assertEquals(error.getId(), modeled.schema().id(), "error shape");
        assertEquals(test.getCode(), modeled.statusCode(), "status code");
        StructDocument expected = dynamic.value(error, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)), normalize(DynamicService.canonical(modeled.value())),
                     "error members");
    }

    private static SdkHttpResponse response(HttpResponseTestCase test) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(test.getCode());
        test.getHeaders().forEach(response::addHeader);
        test.getBody().ifPresent(body -> response.body(HttpBody.fromUtf8(body)));
        return response.build();
    }

    private static void assertBody(String expected, String mediaType, String actual) {
        if (mediaType.contains("json") && !expected.isEmpty()) {
            assertEquals(normalize(Node.parse(expected)), normalize(Node.parse(actual)), () -> "JSON body " + actual);
        } else {
            assertEquals(expected, actual, "body");
        }
    }

    /** A comparable form of a JSON value in which numbers compare by numeric value ({@code 2} equals {@code 2.0}). */
    private static Object normalize(Node node) {
        if (node.isObjectNode()) {
            Map<String, Object> out = new java.util.TreeMap<>();
            node.expectObjectNode().getStringMap().forEach((k, v) -> out.put(k, normalize(v)));
            return out;
        }
        if (node.isArrayNode()) {
            List<Object> out = new ArrayList<>();
            node.expectArrayNode().getElements().forEach(e -> out.add(normalize(e)));
            return out;
        }
        if (node.isNumberNode()) {
            java.math.BigDecimal value = new java.math.BigDecimal(node.expectNumberNode().getValue().toString());
            return value.signum() == 0 ? java.math.BigDecimal.ZERO : value.stripTrailingZeros();
        }
        return node;
    }
}
