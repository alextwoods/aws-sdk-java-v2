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

package software.amazon.awssdk.protocols.restjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestsTrait;

/**
 * Runs the official Smithy {@code smithy-aws-protocol-tests} client request, response and error cases of
 * {@code restJson1} against this runtime, with Smithy Java dynamic schemas standing in for generated types (the full
 * generated-client conformance runners are task 18.1). Modelled on aws-json-protocol's
 * {@code AwsJsonProtocolConformanceTest}, with query-string and payload checks added.
 *
 * <p>Cases that exercise features outside the protocol runtime are skipped with their reason (see {@link #SKIPPED}
 * and {@link #skipReason}).
 */
class RestJsonProtocolConformanceTest {
    private static final String GENERATED_BUILDER = "default-value population is generated-builder behavior, which "
                                                    + "the dynamic schema builders of this runner do not apply";
    private static final String IDEMPOTENCY_TOKEN = "idempotency-token auto-fill is client/generated-builder "
                                                    + "behavior, not protocol SERDE";
    private static final String CHECKSUM = "request checksums (httpChecksumRequired/httpChecksum) are a pipeline "
                                           + "feature";
    private static final String COMPRESSION = "request compression is a pipeline feature";
    private static final String GLACIER = "Glacier client customization (default accountId \"-\", "
                                          + "x-amz-glacier-version header, tree-hash checksum headers) belongs to the "
                                          + "Glacier client";

    /** Test case ID to the reason it is skipped. */
    private static final Map<String, String> SKIPPED = Map.ofEntries(
        Map.entry("RestJsonClientPopulatesDefaultValuesInInput", GENERATED_BUILDER),
        Map.entry("RestJsonClientPopulatesDefaultsValuesWhenMissingInResponse", GENERATED_BUILDER),
        Map.entry("RestJsonClientPopulatesNestedDefaultValuesWhenMissing", GENERATED_BUILDER),
        Map.entry("RestJsonClientPopulatesNestedDefaultsWhenMissingInResponseBody", GENERATED_BUILDER),
        Map.entry("RestJsonQueryIdempotencyTokenAutoFill", IDEMPOTENCY_TOKEN),
        Map.entry("RestJsonHttpChecksumRequired", CHECKSUM),
        Map.entry("GlacierAccountId", GLACIER),
        Map.entry("GlacierChecksums", GLACIER),
        Map.entry("GlacierVersionHeader", GLACIER),
        Map.entry("GlacierMultipartChecksums", GLACIER),
        Map.entry("ApiGatewayAccept", "service customization (the Accept: application/json header) belongs to the "
                                      + "API Gateway client"));

    private static Model model;

    @BeforeAll
    static void loadModels() {
        model = Model.assembler(RestJsonProtocolConformanceTest.class.getClassLoader())
                     .discoverModels(RestJsonProtocolConformanceTest.class.getClassLoader())
                     .assemble()
                     .unwrap();
    }

    @TestFactory
    Stream<DynamicTest> officialRestJsonProtocolTests() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.findTrait(ProtocolIds.REST_JSON_1).isEmpty()) {
                continue;
            }
            DynamicService dynamic = new DynamicService(model, service.getId());
            ClientProtocol client = dynamic.client(new RestJsonProtocolProvider(), true);
            for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
                addOperationTests(tests, dynamic, client, operation);
            }
        }
        assertFalse(tests.isEmpty(), "No restJson1 protocol tests were found");
        return tests.stream();
    }

    /**
     * Every skipped case has a reason owned by another task, and every {@link #SKIPPED} entry names a real client
     * case (so the list cannot go stale). Prints the skip catalogue.
     */
    @Test
    void skippedCasesAreCataloguedWithReasons() {
        Map<String, String> skipped = new TreeMap<>();
        int total = 0;
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.findTrait(ProtocolIds.REST_JSON_1).isEmpty()) {
                continue;
            }
            DynamicService dynamic = new DynamicService(model, service.getId());
            for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
                Map<HttpMessageTestCase, String> cases = new LinkedHashMap<>();
                operation.getTrait(HttpRequestTestsTrait.class)
                         .ifPresent(t -> t.getTestCasesFor(AppliesTo.CLIENT).forEach(c -> cases.put(c, "request")));
                operation.getTrait(HttpResponseTestsTrait.class)
                         .ifPresent(t -> t.getTestCasesFor(AppliesTo.CLIENT).forEach(c -> cases.put(c, "response")));
                for (ShapeId errorId : operation.getErrors(service)) {
                    model.expectShape(errorId).getTrait(HttpResponseTestsTrait.class)
                         .ifPresent(t -> t.getTestCasesFor(AppliesTo.CLIENT).forEach(c -> cases.put(c, "error")));
                }
                for (Map.Entry<HttpMessageTestCase, String> entry : cases.entrySet()) {
                    HttpMessageTestCase test = entry.getKey();
                    if (!test.getProtocol().equals(ProtocolIds.REST_JSON_1)) {
                        continue;
                    }
                    total++;
                    String reason = skipReason(dynamic, operation, test);
                    if (reason != null) {
                        skipped.put(entry.getValue() + " " + service.getId().getName() + " "
                                    + operation.getId().getName() + " " + test.getId(), reason);
                    }
                }
            }
        }
        System.out.println("restJson1 conformance: " + total + " client cases, " + skipped.size() + " skipped");
        skipped.forEach((id, reason) -> System.out.println("  skipped " + id + ": " + reason));
        for (String id : SKIPPED.keySet()) {
            assertTrue(skipped.keySet().stream().anyMatch(k -> k.endsWith(" " + id)), "stale skip entry " + id);
        }
    }

    private static void addOperationTests(List<DynamicTest> tests, DynamicService dynamic, ClientProtocol client,
                                          OperationShape operation) {
        OperationProtocol plan = client.operation(dynamic.schemas(operation));
        String service = dynamic.service().getId().getName() + " ";
        operation.getTrait(HttpRequestTestsTrait.class).ifPresent(trait -> {
            for (HttpRequestTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(ProtocolIds.REST_JSON_1)) {
                    tests.add(DynamicTest.dynamicTest("request " + service + test.getId(), located(
                        test, () -> runRequest(dynamic, operation, plan, test))));
                }
            }
        });
        operation.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
            for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                if (test.getProtocol().equals(ProtocolIds.REST_JSON_1)) {
                    tests.add(DynamicTest.dynamicTest("response " + service + test.getId(), located(
                        test, () -> runResponse(dynamic, operation, plan, test))));
                }
            }
        });
        for (ShapeId errorId : operation.getErrors(dynamic.service())) {
            StructureShape error = dynamic.model().expectShape(errorId, StructureShape.class);
            error.getTrait(HttpResponseTestsTrait.class).ifPresent(trait -> {
                for (HttpResponseTestCase test : trait.getTestCasesFor(AppliesTo.CLIENT)) {
                    if (test.getProtocol().equals(ProtocolIds.REST_JSON_1)) {
                        tests.add(DynamicTest.dynamicTest(
                            "error " + service + operation.getId().getName() + " " + test.getId(),
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

    /** The reason a case is skipped, or {@code null} if it runs. */
    static String skipReason(DynamicService dynamic, OperationShape operation, HttpMessageTestCase test) {
        String reason = SKIPPED.get(test.getId());
        if (reason != null) {
            return reason;
        }
        if (operation.hasTrait(ShapeId.from("smithy.api#requestCompression"))) {
            return COMPRESSION;
        }
        if (operation.hasTrait(ShapeId.from("aws.protocols#httpChecksum"))
            || operation.hasTrait(ShapeId.from("smithy.api#httpChecksumRequired"))) {
            return CHECKSUM;
        }
        return null;
    }

    /** The streaming blob member of {@code structure}, if any. */
    private static Optional<MemberShape> streamingBlob(DynamicService dynamic, ShapeId structure) {
        for (MemberShape member : dynamic.model().expectShape(structure).members()) {
            Shape target = dynamic.model().expectShape(member.getTarget());
            if (target.isBlobShape() && target.hasTrait(StreamingTrait.class)) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    /**
     * The request body as text. A streaming blob payload must arrive unbuffered, as a {@link HttpBody.Kind#STREAMING}
     * body; it is read here through its blocking view.
     */
    private static String bodyText(DynamicService dynamic, OperationShape operation, SdkHttpRequest request) {
        HttpBody body = request.body();
        if (streamingBlob(dynamic, operation.getInputShape()).isPresent() && body.kind() != HttpBody.Kind.EMPTY) {
            assertEquals(HttpBody.Kind.STREAMING, body.kind(), "streaming payloads are not buffered");
            try (InputStream in = body.streamingBody().openStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return body.asUtf8String();
    }

    /** The bytes of a streaming blob output member ({@code ""} if absent), read through its blocking view. */
    private static String streamText(StructDocument value, String member) {
        Document document = value.getMember(member);
        if (document == null) {
            return "";
        }
        try (InputStream in = document.asDataStream().asInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void skipIfUnsupported(DynamicService dynamic, OperationShape operation,
                                          HttpMessageTestCase test) {
        String reason = skipReason(dynamic, operation, test);
        Assumptions.assumeTrue(reason == null, () -> "Skipped: " + reason);
    }

    private static void runRequest(DynamicService dynamic, OperationShape operation, OperationProtocol plan,
                                   HttpRequestTestCase test) {
        skipIfUnsupported(dynamic, operation, test);
        StructDocument input = dynamic.value(dynamic.model().expectShape(operation.getInputShape()), test.getParams());
        String host = test.getHost().orElse("example.com");
        SdkHttpRequest request = plan.serializeRequest(input, URI.create("https://" + host));
        assertEquals(test.getMethod(), request.method(), "method");
        assertEquals(test.getUri(), request.encodedPath(), "uri");
        test.getResolvedHost().ifPresent(resolved -> assertEquals(resolved, request.host(), "resolved host"));
        List<String> query = request.encodedQuery().isEmpty() ? List.of()
                                                              : Arrays.asList(request.encodedQuery().split("&"));
        List<String> remaining = new ArrayList<>(query);
        for (String expected : test.getQueryParams()) {
            assertTrue(remaining.remove(expected), () -> "query parameter " + expected + " in " + query);
        }
        List<String> names = query.stream().map(p -> p.contains("=") ? p.substring(0, p.indexOf('=')) : p).toList();
        for (String forbidden : test.getForbidQueryParams()) {
            assertFalse(names.contains(forbidden), "forbidden query parameter " + forbidden);
        }
        for (String required : test.getRequireQueryParams()) {
            assertTrue(names.contains(required), "required query parameter " + required);
        }
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
                                                        bodyText(dynamic, operation, request)));
    }

    private static void runResponse(DynamicService dynamic, OperationShape operation, OperationProtocol plan,
                                    HttpResponseTestCase test) {
        skipIfUnsupported(dynamic, operation, test);
        StructureShape outputShape = dynamic.model().expectShape(operation.getOutputShape(), StructureShape.class);
        StructDocument actual = plan.deserializeResponse(response(test), dynamic.builder(outputShape));
        StructDocument expected = dynamic.value(outputShape, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)), normalize(DynamicService.canonical(actual)),
                     "output");
        streamingBlob(dynamic, outputShape.getId()).ifPresent(member -> assertEquals(
            streamText(expected, member.getMemberName()), streamText(actual, member.getMemberName()),
            "streaming payload " + member.getMemberName()));
    }

    private static void runError(DynamicService dynamic, StructureShape error, OperationProtocol plan,
                                 HttpResponseTestCase test) {
        skipIfUnsupported(dynamic, dynamic.operation(plan.schemas().id()), test);
        AwsServiceException exception = plan.deserializeError(response(test));
        DynamicError modeled = assertInstanceOf(DynamicError.class, exception, "modeled error");
        assertEquals(error.getId(), modeled.schema().id(), "error shape");
        assertEquals(test.getCode(), modeled.statusCode(), "status code");
        StructDocument expected = dynamic.value(error, test.getParams());
        assertEquals(normalize(DynamicService.canonical(expected)),
                     normalize(DynamicService.canonical(modeled.value())), "error members");
    }

    /**
     * The response of {@code test}, its body delivered as a one-shot {@link HttpBody.Kind#STREAMING} body, as a
     * transport delivers it: streaming payloads must pass it through unread, and every finite output or error must
     * buffer it.
     */
    private static SdkHttpResponse response(HttpResponseTestCase test) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(test.getCode());
        test.getHeaders().forEach(response::addHeader);
        test.getBody().ifPresent(body -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            response.body(HttpBody.streaming(StreamingBodies.fromInputStream(new ByteArrayInputStream(bytes),
                                                                             bytes.length, null)));
        });
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
            BigDecimal value = new BigDecimal(node.expectNumberNode().getValue().toString());
            return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        }
        return node;
    }
}
