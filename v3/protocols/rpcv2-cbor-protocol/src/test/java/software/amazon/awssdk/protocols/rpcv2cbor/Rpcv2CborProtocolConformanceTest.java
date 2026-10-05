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

package software.amazon.awssdk.protocols.rpcv2cbor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
 * Runs the official Smithy client request, response and error cases of {@code smithy.protocols#rpcv2Cbor} against
 * this runtime: the {@code smithy-protocol-tests} cases ({@code smithy.protocoltests.rpcv2Cbor#RpcV2Protocol}) and the
 * {@code awsQueryCompatible} cases of {@code smithy-aws-protocol-tests} ({@code aws.protocoltests.rpcv2cbor}). Smithy
 * Java dynamic schemas stand in for generated types (the full generated-client conformance runners are task 18.1).
 *
 * <p>CBOR bodies are compared semantically ({@link CborValues#decode}), not byte for byte: the protocol permits
 * definite or indefinite lengths, any map entry order and any float width that preserves the value.
 *
 * <p>Cases that exercise features outside the protocol runtime are skipped with their reason ({@link #SKIPPED}).
 */
class Rpcv2CborProtocolConformanceTest {
    private static final String GENERATED_BUILDER = "default-value population is behavior of the generated model "
                                                    + "builders (task 3), which the dynamic schema builders of this "
                                                    + "runner do not apply; generated-client conformance is task 18.1";

    /** Test case ID to the reason it is skipped. */
    private static final Map<String, String> SKIPPED = Map.ofEntries(
        Map.entry("RpcV2CborClientPopulatesDefaultValuesInInput", GENERATED_BUILDER),
        Map.entry("RpcV2CborClientPopulatesDefaultsValuesWhenMissingInResponse", GENERATED_BUILDER));

    /** The rpcv2Cbor test services of smithy-protocol-tests and smithy-aws-protocol-tests. */
    private static final List<ShapeId> EXPECTED_SERVICES = List.of(
        ShapeId.from("smithy.protocoltests.rpcv2Cbor#RpcV2Protocol"),
        ShapeId.from("aws.protocoltests.rpcv2cbor#QueryCompatibleRpcV2Protocol"),
        ShapeId.from("aws.protocoltests.rpcv2cbor#NonQueryCompatibleRpcV2Protocol"));

    private static Model model;

    @BeforeAll
    static void loadModels() {
        model = Model.assembler(Rpcv2CborProtocolConformanceTest.class.getClassLoader())
                     .discoverModels(Rpcv2CborProtocolConformanceTest.class.getClassLoader())
                     .assemble()
                     .unwrap();
    }

    @TestFactory
    Stream<DynamicTest> officialRpcv2CborProtocolTests() {
        List<DynamicTest> tests = new ArrayList<>();
        Map<ShapeId, Integer> casesByService = new TreeMap<>();
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.findTrait(ProtocolIds.RPC_V2_CBOR).isEmpty()) {
                continue;
            }
            DynamicService dynamic = new DynamicService(model, service.getId());
            ClientProtocol client = dynamic.client(new Rpcv2CborProtocolProvider(), true);
            int before = tests.size();
            for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
                addOperationTests(tests, dynamic, client, operation);
            }
            casesByService.put(service.getId(), tests.size() - before);
        }
        // Both test-model artifacts must contribute cases, so that dropping either dependency fails this runner.
        for (ShapeId expected : EXPECTED_SERVICES) {
            assertTrue(casesByService.getOrDefault(expected, 0) > 0,
                       () -> "No rpcv2Cbor protocol tests were found for " + expected + ": " + casesByService);
        }
        return tests.stream();
    }

    private static void addOperationTests(List<DynamicTest> tests, DynamicService dynamic, ClientProtocol client,
                                          OperationShape operation) {
        OperationProtocol plan = client.operation(dynamic.schemas(operation));
        String service = dynamic.service().getId().getName();
        operation.getTrait(HttpRequestTestsTrait.class).ifPresent(trait -> {
            for (HttpRequestTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(ProtocolIds.RPC_V2_CBOR)) {
                    tests.add(DynamicTest.dynamicTest("request " + service + " " + test.getId(),
                                                      located(test, () -> runRequest(dynamic, operation, plan, test))));
                }
            }
        });
        operation.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
            for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(ProtocolIds.RPC_V2_CBOR)) {
                    tests.add(DynamicTest.dynamicTest("response " + service + " " + test.getId(),
                                                      located(test, () -> runResponse(dynamic, operation, plan, test))));
                }
            }
        });
        for (ShapeId errorId : operation.getErrors(dynamic.service())) {
            StructureShape error = dynamic.model().expectShape(errorId, StructureShape.class);
            error.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
                for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                    if (test.getProtocol().equals(ProtocolIds.RPC_V2_CBOR)) {
                        tests.add(DynamicTest.dynamicTest("error " + service + " " + operation.getId().getName() + " "
                                                          + test.getId(),
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
        assertEquals("", Optional.ofNullable(request.encodedQuery()).orElse(""), "query string");
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
                                                        request.body().asByteArray()));
    }

    private static void runResponse(DynamicService dynamic, OperationShape operation, OperationProtocol plan,
                                    HttpResponseTestCase test) {
        skipIfUnsupported(test);
        SdkHttpResponse response = response(test);
        assertFalse(plan.isError(response), "a status code " + test.getCode() + " response is not an error");
        StructureShape outputShape = dynamic.model().expectShape(operation.getOutputShape(), StructureShape.class);
        StructDocument actual = plan.deserializeResponse(response, dynamic.builder(outputShape));
        StructDocument expected = dynamic.value(outputShape, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)), normalize(DynamicService.canonical(actual)),
                     "output");
    }

    private static void runError(DynamicService dynamic, StructureShape error, OperationProtocol plan,
                                 HttpResponseTestCase test) {
        skipIfUnsupported(test);
        SdkHttpResponse response = response(test);
        assertTrue(plan.isError(response), "a status code " + test.getCode() + " response is an error");
        AwsServiceException exception = plan.deserializeError(response);
        DynamicError modeled = assertInstanceOf(DynamicError.class, exception, "modeled error");
        assertEquals(error.getId(), modeled.schema().id(), "error shape");
        assertEquals(test.getCode(), modeled.statusCode(), "status code");
        StructDocument expected = dynamic.value(error, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)),
                     normalize(DynamicService.canonical(modeled.value())), "error members");
        // awsQueryCompatible cases name the error code callers see (the x-amzn-query-error code, else the shape name).
        test.getVendorParams().getStringMember("code").ifPresent(
            code -> assertEquals(code.getValue(), modeled.awsErrorDetails().errorCode(), "error code"));
    }

    private static SdkHttpResponse response(HttpResponseTestCase test) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(test.getCode());
        test.getHeaders().forEach(response::addHeader);
        test.getBody().ifPresent(body -> response.body(HttpBody.fromBytes(
            isCbor(test.getBodyMediaType().orElse("")) ? Base64.getDecoder().decode(body)
                                                       : body.getBytes(StandardCharsets.UTF_8))));
        return response.build();
    }

    private static boolean isCbor(String mediaType) {
        return mediaType.contains("cbor");
    }

    private static void assertBody(String expected, String mediaType, byte[] actual) {
        if (expected.isEmpty()) {
            assertEquals(0, actual.length, "empty body");
        } else if (isCbor(mediaType)) {
            byte[] expectedBytes = Base64.getDecoder().decode(expected);
            assertEquals(CborValues.decode(expectedBytes), CborValues.decode(actual),
                         () -> "CBOR body " + Base64.getEncoder().encodeToString(actual));
        } else {
            assertEquals(expected, new String(actual, StandardCharsets.UTF_8), "body");
        }
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
            return CborValues.number(new BigDecimal(node.expectNumberNode().getValue().toString()));
        }
        return node;
    }
}
