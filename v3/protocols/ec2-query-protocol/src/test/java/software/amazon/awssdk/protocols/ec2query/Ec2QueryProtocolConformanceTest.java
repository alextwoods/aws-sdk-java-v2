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

package software.amazon.awssdk.protocols.ec2query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
 * {@code ec2Query} against this runtime, with Smithy Java dynamic schemas standing in for generated types (the full
 * generated-client conformance runners are task 18.1).
 *
 * <p>Form bodies are compared as the multiset of their raw (still percent-encoded) {@code key=value} pairs, since the
 * protocol tests allow any parameter order; XML output is compared as the deserialized value. Cases that exercise
 * features outside the protocol runtime are skipped with their reason: client-side request compression (pipeline)
 * and idempotency-token auto-fill (generated builders).
 */
class Ec2QueryProtocolConformanceTest {
    /** Test case ID to the reason it is skipped. */
    private static final Map<String, String> SKIPPED = Map.of(
        "Ec2ProtocolIdempotencyTokenAutoFill",
        "idempotency-token auto-fill is generated-builder behavior, which the dynamic schema builders of this runner "
        + "do not apply");

    private static final ShapeId PROTOCOL = ProtocolIds.EC2_QUERY;

    private static Model model;

    @BeforeAll
    static void loadModels() {
        model = Model.assembler(Ec2QueryProtocolConformanceTest.class.getClassLoader())
                     .discoverModels(Ec2QueryProtocolConformanceTest.class.getClassLoader())
                     .assemble()
                     .unwrap();
    }

    private static ProtocolProvider provider() {
        return new Ec2QueryProtocolProvider();
    }

    @TestFactory
    Stream<DynamicTest> officialEc2QueryProtocolTests() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.findTrait(PROTOCOL).isEmpty()) {
                continue;
            }
            DynamicService dynamic = new DynamicService(model, service.getId());
            ClientProtocol client = dynamic.client(provider(), true);
            for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
                addOperationTests(tests, dynamic, client, operation);
            }
        }
        assertFalse(tests.isEmpty(), "No " + PROTOCOL + " protocol tests were found");
        return tests.stream();
    }

    private static void addOperationTests(List<DynamicTest> tests, DynamicService dynamic, ClientProtocol client,
                                          OperationShape operation) {
        OperationProtocol plan = client.operation(dynamic.schemas(operation));
        operation.getTrait(HttpRequestTestsTrait.class).ifPresent(trait -> {
            for (HttpRequestTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(PROTOCOL)) {
                    tests.add(DynamicTest.dynamicTest("request " + test.getId(),
                                                      located(test, () -> runRequest(dynamic, operation, plan, test))));
                }
            }
        });
        operation.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
            for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(PROTOCOL)) {
                    tests.add(DynamicTest.dynamicTest("response " + test.getId(),
                                                      located(test, () -> runResponse(dynamic, operation, plan, test))));
                }
            }
        });
        for (ShapeId errorId : operation.getErrors(dynamic.service())) {
            StructureShape error = dynamic.model().expectShape(errorId, StructureShape.class);
            error.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
                for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                    if (test.getProtocol().equals(PROTOCOL)) {
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
        test.getBody().ifPresent(expected -> assertEquals(formPairs(expected),
                                                          formPairs(request.body().asUtf8String()),
                                                          () -> "form body " + request.body().asUtf8String()));
    }

    /** The raw {@code key=value} pairs of a form body, sorted (parameter order is not significant). */
    static List<String> formPairs(String body) {
        if (body.isEmpty()) {
            return List.of();
        }
        List<String> pairs = new ArrayList<>(Arrays.asList(body.split("&", -1)));
        pairs.sort(null);
        return pairs;
    }

    private static void runResponse(DynamicService dynamic, OperationShape operation, OperationProtocol plan,
                                    HttpResponseTestCase test) {
        skipIfUnsupported(test);
        StructureShape outputShape = dynamic.model().expectShape(operation.getOutputShape(), StructureShape.class);
        StructDocument actual = plan.deserializeResponse(response(test), dynamic.builder(outputShape));
        StructDocument expected = dynamic.value(outputShape, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)), normalize(DynamicService.canonical(actual)),
                     "output");
    }

    private static void runError(DynamicService dynamic, StructureShape error, OperationProtocol plan,
                                 HttpResponseTestCase test) {
        skipIfUnsupported(test);
        AwsServiceException exception = plan.deserializeError(response(test));
        DynamicError modeled = assertInstanceOf(DynamicError.class, exception, "modeled error");
        assertEquals(error.getId(), modeled.schema().id(), "error shape");
        assertEquals(test.getCode(), modeled.statusCode(), "status code");
        test.getVendorParams().getStringMember("code").ifPresent(
            code -> assertEquals(code.getValue(), modeled.awsErrorDetails().errorCode(), "error code"));
        StructDocument expected = dynamic.value(error, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)),
                     normalize(DynamicService.canonical(modeled.value())), "error members");
    }

    private static SdkHttpResponse response(HttpResponseTestCase test) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(test.getCode());
        test.getHeaders().forEach(response::addHeader);
        test.getBody().ifPresent(body -> response.body(HttpBody.fromUtf8(body)));
        return response.build();
    }

    /** A comparable form of a JSON value in which numbers compare by numeric value ({@code 2} equals {@code 2.0}). */
    private static Object normalize(Node node) {
        if (node.isObjectNode()) {
            Map<String, Object> out = new TreeMap<>();
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
