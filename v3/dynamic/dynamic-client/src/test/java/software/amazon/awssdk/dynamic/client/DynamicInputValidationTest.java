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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.json;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.dynamic.client.DynamicInvocationException.Violation;
import software.amazon.awssdk.dynamic.client.TestSupport.RecordingInterceptor;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Finite inputs are validated against the exact operation input schema before anything of the call runs
 * (Requirements 14.11, 14.19): every invalid element is reported with its path, and nothing reaches an interceptor or
 * the transport.
 */
class DynamicInputValidationTest {
    private static final ShapeId INPUT = ShapeId.from("example.weather#GetForecastInput");

    private static TestTransport transport;
    private static RecordingInterceptor interceptor;
    private static DynamicClient client;

    @BeforeAll
    static void build() {
        transport = TestTransport.sync(r -> json(200, "{}"));
        interceptor = new RecordingInterceptor();
        client = TestSupport.syncClient(transport).addExecutionInterceptor(interceptor).build();
    }

    @AfterAll
    static void close() {
        client.close();
    }

    @BeforeEach
    void reset() {
        interceptor.hooks.clear();
    }

    static Stream<Arguments> invalidInputs() {
        List<Document> withNull = new ArrayList<>();
        withNull.add(Document.of("a"));
        withNull.add(null);
        return Stream.of(
            Arguments.of("unknown member", doc(Map.of("city", Document.of("x"), "nope", Document.of(1))),
                         "input.nope", "is not a member of example.weather#GetForecastInput"),
            Arguments.of("wrong scalar type", doc(Map.of("days", Document.of("three"))),
                         "input.days", "expected an integer value for smithy.api#Integer, found a string document"),
            Arguments.of("integer out of range", doc(Map.of("days", Document.of(3_000_000_000L))),
                         "input.days", "outside the range [-2147483648, 2147483647]"),
            Arguments.of("fractional integer", doc(Map.of("days", Document.of(1.5))),
                         "input.days", "is 1.5, not an integral value"),
            Arguments.of("byte out of range", doc(Map.of("small", Document.of(300))),
                         "input.small", "outside the range [-128, 127]"),
            Arguments.of("missing nested required member",
                         doc(Map.of("location", doc(Map.of("longitude", Document.of(1.0))))),
                         "input.location.latitude", "is required by example.weather#Location"),
            Arguments.of("union with two members",
                         doc(Map.of("filter", doc(Map.of("minimum", Document.of(1), "maximum", Document.of(2))))),
                         "input.filter", "exactly one member set, not 2"),
            Arguments.of("empty union", doc(Map.of("filter", doc(Map.of()))),
                         "input.filter", "exactly one member set, not 0"),
            Arguments.of("null element of a dense list", doc(Map.of("tags", Document.of(withNull))),
                         "input.tags[1]", "is null in the dense list example.weather#TagList"),
            Arguments.of("wrong map value", doc(Map.of("labels", doc(Map.of("owner", Document.of(1))))),
                         "input.labels[\"owner\"]", "found an integer document"),
            Arguments.of("list given for a map", doc(Map.of("labels", Document.of(List.of()))),
                         "input.labels", "found a list document"),
            Arguments.of("unparseable timestamp", doc(Map.of("when", Document.of("yesterday"))),
                         "input.when", "not an RFC 3339 date-time"),
            Arguments.of("not a structure", Document.of("GetForecast"),
                         "input", "expected a structure value for example.weather#GetForecastInput, found a "
                                  + "string document"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidInputs")
    void invalidInputIsRejectedBeforeTransportWithTheInvalidElement(String name, Document input, String path,
                                                                   String problem) {
        int before = transport.transmissions();
        DynamicInvocationException e = assertThrows(DynamicInvocationException.class,
                                                    () -> client.call("GetForecast", input));
        assertEquals(DynamicInvocationException.Reason.INVALID_INPUT, e.reason());
        assertEquals(TestSupport.WEATHER, e.service());
        assertEquals(ShapeId.from("example.weather#GetForecast"), e.operation());
        assertEquals("GetForecast", e.operationName());
        assertEquals(1, e.violations().size(), e.violations().toString());
        Violation violation = e.violations().get(0);
        assertEquals(path, violation.path());
        assertTrue(violation.problem().contains(problem), violation.problem());
        assertTrue(e.getMessage().contains(INPUT.toString()) && e.getMessage().contains(path), e.getMessage());
        assertEquals(before, transport.transmissions(), "nothing may be transmitted");
        assertEquals(List.of(), interceptor.hooks, "no interceptor may run");
    }

    @Test
    void everyViolationIsReportedInTraversalOrder() {
        Document input = doc(Map.of("zzz", Document.of(1), "aaa", Document.of(2), "days", Document.of("x"),
                                    "location", doc(Map.of())));
        DynamicInvocationException e = assertThrows(DynamicInvocationException.class,
                                                    () -> client.call("GetForecast", input));
        assertEquals(List.of("input.aaa", "input.zzz", "input.days", "input.location.latitude"),
                     e.violations().stream().map(Violation::path).toList());
        assertEquals(List.of(), interceptor.hooks);
    }

    @Test
    void aDocumentOfAnotherStructureIsRejected() {
        var operation = client.service().operation("GetForecast");
        Document output = StructDocument.of(operation.schemas().output(), Document.of(Map.of()));
        DynamicInvocationException e = assertThrows(DynamicInvocationException.class,
                                                    () -> client.call("GetForecast", output));
        assertEquals("input", e.violations().get(0).path());
        assertTrue(e.violations().get(0).problem().contains("is a document of example.weather#GetForecastOutput"),
                   e.violations().get(0).problem());
    }

    @Test
    void conformingInputsReachThePipeline() {
        int before = transport.transmissions();
        // A required member of an @input structure is client optional, as Smithy defines; the idempotency token is
        // filled by the pipeline; enum values are open; timestamps may be numbers or date-time strings.
        client.call("GetForecast", doc(Map.of("unit", Document.of("KELVIN"),
                                              "when", Document.of(1_700_000_000))));
        client.call("GetForecast", doc(Map.of("when", Document.of("2024-01-02T03:04:05Z"),
                                              "location", doc(Map.of("latitude", Document.of(47.6))),
                                              "filter", doc(Map.of("maximum", Document.of(30))),
                                              "tags", Document.of(List.of(Document.of("a"))),
                                              "labels", doc(Map.of("owner", Document.of("me"))),
                                              "small", Document.of((byte) 7))));
        client.call("GetForecast", doc(Map.of("when", Document.of(Instant.parse("2024-01-02T03:04:05Z")),
                                              "days", Document.of(2.0))));
        assertEquals(before + 3, transport.transmissions());
        String body = transport.requests().get(before + 1).body();
        assertTrue(body.contains("\"latitude\":47.6") && body.contains("\"maximum\":30"), body);
        String integral = transport.requests().get(before + 2).body();
        assertTrue(integral.contains("\"days\":2,") || integral.contains("\"days\":2}"), integral);
    }
}
