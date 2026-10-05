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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.InstalledProtocols;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/** Request, response and error fixtures of the AWS JSON runtime (awsJson1_0 and awsJson1_1). */
class AwsJsonProtocolTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.widgets

        use aws.protocols#awsJson1_0
        use aws.protocols#awsJson1_1
        use aws.protocols#awsQueryCompatible
        use aws.protocols#awsQueryError

        @awsJson1_0
        @awsJson1_1
        @awsQueryCompatible
        service Widgets {
            version: "2020-01-01"
            operations: [GetWidget, PutWidget, Subscribe]
        }

        operation GetWidget {
            input := {
                @required
                id: String
                @jsonName("renamed")
                name: String
                created: Timestamp
            }
            output := {
                id: String
                created: Timestamp
                @timestampFormat("date-time")
                updated: Timestamp
                items: Items
            }
            errors: [WidgetNotFound, Throttled]
        }

        @endpoint(hostPrefix: "{tenant}.data.")
        operation PutWidget {
            input := {
                @required
                @hostLabel
                tenant: String
            }
        }

        operation Subscribe {
            output := {
                events: Events
            }
        }

        @streaming
        union Events {
            tick: Tick
        }

        structure Tick {
            n: Integer
        }

        list Items {
            member: Item
        }

        structure Item {
            size: Integer
        }

        @error("client")
        @awsQueryError(code: "AWS.Widgets.NotFound", httpResponseCode: 404)
        structure WidgetNotFound {
            message: String
            widgetId: String
        }

        @error("server")
        structure Throttled {
            message: String
        }
        """;

    private static final URI ENDPOINT = URI.create("https://widgets.example.com");
    private static final ShapeId GET_WIDGET = ShapeId.from("example.widgets#GetWidget");

    private static DynamicService service;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(AwsJsonProtocolTest.class.getClassLoader())
                           .discoverModels(AwsJsonProtocolTest.class.getClassLoader())
                           .addUnparsedModel("widgets.smithy", MODEL)
                           .assemble()
                           .unwrap();
        service = new DynamicService(model, ShapeId.from("example.widgets#Widgets"));
    }

    private static OperationProtocol plan(boolean json11, String operation, boolean hostPrefixInjection) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.client(json11 ? new AwsJson11ProtocolProvider() : new AwsJson10ProtocolProvider(),
                              hostPrefixInjection)
                      .operation(service.schemas(shape));
    }

    private static StructDocument input(String operation, String params) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.value(service.model().expectShape(shape.getInputShape()), Node.parse(params));
    }

    private static SdkHttpResponse response(int status, String body, String... headers) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(status).body(HttpBody.fromUtf8(body));
        for (int i = 0; i < headers.length; i += 2) {
            builder.addHeader(headers[i], headers[i + 1]);
        }
        return builder.build();
    }

    @Test
    void serializesAwsJson10Request() {
        SdkHttpRequest request = plan(false, "GetWidget", true)
            .serializeRequest(input("GetWidget", "{\"id\": \"w-1\", \"name\": \"n\", \"created\": 1.5}"),
                              URI.create("https://widgets.example.com:8443/base"));
        assertEquals("POST", request.method());
        assertEquals("widgets.example.com", request.host());
        assertEquals(8443, request.port());
        assertEquals("/base/", request.encodedPath());
        assertEquals(Optional.of("application/x-amz-json-1.0"), request.headers().firstValue("content-type"));
        assertEquals(Optional.of("Widgets.GetWidget"), request.headers().firstValue("X-Amz-Target"));
        assertEquals(Optional.of("true"), request.headers().firstValue("x-amzn-query-mode"));
        // jsonName is ignored by awsJson; timestamps default to epoch seconds.
        assertEquals(Node.parse("{\"id\":\"w-1\",\"name\":\"n\",\"created\":1.5}"),
                     Node.parse(request.body().asUtf8String()));
        assertEquals(List.of("Content-Type", "X-Amz-Target", "x-amzn-query-mode"),
                     List.of(request.headers().name(0), request.headers().name(1), request.headers().name(2)));
    }

    @Test
    void awsJson11UsesItsContentType() {
        SdkHttpRequest request = plan(true, "GetWidget", true).serializeRequest(input("GetWidget", "{\"id\":\"x\"}"),
                                                                               ENDPOINT);
        assertEquals(Optional.of("application/x-amz-json-1.1"), request.headers().firstValue("Content-Type"));
        assertEquals("/", request.encodedPath());
    }

    @Test
    void appliesHostPrefixUnlessDisabled() {
        StructDocument input = input("PutWidget", "{\"tenant\": \"acme\"}");
        assertEquals("acme.data.widgets.example.com",
                     plan(false, "PutWidget", true).serializeRequest(input, ENDPOINT).host());
        assertEquals("widgets.example.com", plan(false, "PutWidget", false).serializeRequest(input, ENDPOINT).host());
        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class, () -> plan(false, "PutWidget",
            true).serializeRequest(input("PutWidget", "{\"tenant\": \"not/valid\"}"), ENDPOINT));
        assertFalse(invalid.getMessage().contains("not/valid"));
    }

    @Test
    void readsEpochSecondsExactly() {
        StructDocument output = plan(false, "GetWidget", true).deserializeResponse(
            response(200, "{\"created\": 512.035, \"updated\": \"2020-01-02T03:04:05.123Z\", \"id\": \"w\"}"),
            service.builder(service.model().expectShape(service.operation(GET_WIDGET).getOutputShape())));
        Schema schema = output.schema();
        assertEquals(Instant.ofEpochSecond(512, 35_000_000), output.getMemberValue(schema.member("created")));
        assertEquals(Instant.parse("2020-01-02T03:04:05.123Z"), output.getMemberValue(schema.member("updated")));
    }

    @Test
    void emptyResponseBodyIsAnEmptyOutput() {
        StructDocument output = plan(false, "GetWidget", true).deserializeResponse(
            response(200, ""), service.builder(service.model().expectShape(
                service.operation(GET_WIDGET).getOutputShape())));
        assertEquals(Node.parse("{}"), DynamicService.canonical(output));
    }

    @Test
    void invalidWireInputIsLocatedWithoutWireValues() {
        SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> plan(false, "GetWidget", true)
            .deserializeResponse(response(200, "{\"items\": [{\"size\": 1}, {\"size\": \"secret-value\"}]}"),
                                 service.builder(service.model().expectShape(
                                     service.operation(GET_WIDGET).getOutputShape()))));
        assertEquals(ProtocolIds.AWS_JSON_1_0, e.protocolId());
        assertEquals(GET_WIDGET, e.operationId());
        assertEquals(List.of("items", "[1]", "size"), e.memberPath());
        assertEquals("items[1].size", e.renderedMemberPath());
        assertEquals(ShapeId.from("smithy.api#Integer"), e.shapeId());
        assertEquals(Optional.of("body"), e.wireLocation());
        assertFalse(e.getMessage().contains("secret-value"), e.getMessage());
        assertFalse(e.retryable());

        SdkProtocolException root = assertThrows(SdkProtocolException.class, () -> plan(false, "GetWidget", true)
            .deserializeResponse(response(200, "{not json"), service.builder(service.model().expectShape(
                service.operation(GET_WIDGET).getOutputShape()))));
        assertEquals(List.of(), root.memberPath());
    }

    @Test
    void mapsErrorTypeHeaderWithSuffixToModeledError() {
        AwsServiceException e = plan(false, "GetWidget", true).deserializeError(response(
            404, "{\"message\": \"gone\", \"widgetId\": \"w-1\"}",
            "X-Amzn-Errortype", "WidgetNotFound:http://internal.amazon.com/coral/com.amazon.coral.validate/",
            "x-amzn-RequestId", "req-1"));
        DynamicError error = assertInstanceOf(DynamicError.class, e);
        assertEquals(ShapeId.from("example.widgets#WidgetNotFound"), error.schema().id());
        assertEquals(404, error.statusCode());
        assertEquals("req-1", error.requestId());
        assertEquals("WidgetNotFound", error.awsErrorDetails().errorCode());
        assertEquals("gone", error.awsErrorDetails().errorMessage());
        assertEquals("Widgets", error.awsErrorDetails().serviceName());
        assertEquals("w-1", error.getMemberValue(error.schema().member("widgetId")));
    }

    @Test
    void mapsNamespacedBodyTypeAndCodeToModeledError() {
        OperationProtocol plan = plan(true, "GetWidget", true);
        AwsServiceException byType = plan.deserializeError(response(
            500, "{\"__type\": \"example.widgets#Throttled\", \"code\": \"WidgetNotFound\", \"Message\": \"slow\"}"));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) byType).schema().id());
        assertEquals("slow", byType.awsErrorDetails().errorMessage());
        AwsServiceException byCode = plan.deserializeError(response(500, "{\"code\": \"Throttled\"}"));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) byCode).schema().id());
    }

    @Test
    void awsQueryCompatibleReportsQueryCodeAndResolvesByIt() {
        OperationProtocol plan = plan(false, "GetWidget", true);
        AwsServiceException typed = plan.deserializeError(response(
            400, "{\"__type\": \"WidgetNotFound\"}", "x-amzn-query-error", "AWS.Widgets.NotFound;Sender"));
        assertEquals(ShapeId.from("example.widgets#WidgetNotFound"), ((DynamicError) typed).schema().id());
        assertEquals("AWS.Widgets.NotFound", typed.awsErrorDetails().errorCode());
        AwsServiceException byQueryCode = plan.deserializeError(response(
            400, "{}", "x-amzn-query-error", "AWS.Widgets.NotFound;Sender"));
        assertEquals(ShapeId.from("example.widgets#WidgetNotFound"), ((DynamicError) byQueryCode).schema().id());
    }

    @Test
    void unknownErrorIsGenericServiceException() {
        AwsServiceException e = plan(false, "GetWidget", true).deserializeError(response(
            503, "{\"__type\": \"aws.other#Mystery\", \"message\": \"what\"}", "x-amzn-RequestId", "req-2",
            "x-amz-id-2", "ext-2"));
        assertEquals(AwsServiceException.class, e.getClass());
        assertEquals("Mystery", e.awsErrorDetails().errorCode());
        assertEquals("what", e.awsErrorDetails().errorMessage());
        assertEquals("req-2", e.requestId());
        assertEquals("ext-2", e.extendedRequestId());
        assertEquals(503, e.statusCode());

        AwsServiceException notJson = plan(false, "GetWidget", true).deserializeError(response(502, "<html/>"));
        assertEquals(AwsServiceException.class, notJson.getClass());
        assertEquals(502, notJson.statusCode());
    }

    @Test
    void eventStreamOutputsReadTheInitialResponseThenEvents() {
        byte[] frames = eventFrames("application/json", "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                    "{\"n\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                    "{\"n\":2}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromBytes(frames)).build();
        OperationShape subscribe = service.operation(ShapeId.from("example.widgets#Subscribe"));
        StructDocument output = plan(false, "Subscribe", true).deserializeResponse(
            response, service.builder(service.model().expectShape(subscribe.getOutputShape())));
        assertTicks(output, 1, 2);
    }

    @Test
    void rejectsInputOfAnotherOperation() {
        assertThrows(IllegalArgumentException.class, () -> plan(false, "GetWidget", true)
            .serializeRequest(input("PutWidget", "{\"tenant\":\"a\"}"), ENDPOINT));
    }

    @Test
    void bothVersionsAreDiscoveredAsServiceEntries() {
        InstalledProtocols installed = InstalledProtocols.discover(AwsJsonProtocolTest.class.getClassLoader());
        assertInstanceOf(AwsJson10ProtocolProvider.class, installed.provider(ProtocolIds.AWS_JSON_1_0).orElseThrow());
        assertInstanceOf(AwsJson11ProtocolProvider.class, installed.provider(ProtocolIds.AWS_JSON_1_1).orElseThrow());
        assertFalse(new AwsJson10ProtocolProvider().usesHttpBindingTraits());
    }
    /** Encoded {@code tick} events of the given payloads, after an optional {@code initial-response} payload. */
    private static byte[] eventFrames(String contentType, byte[] initial, byte[]... payloads) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        if (initial != null) {
            out.writeBytes(frame("initial-response", contentType, initial));
        }
        for (byte[] payload : payloads) {
            out.writeBytes(frame("tick", contentType, payload));
        }
        return out.toByteArray();
    }

    private static byte[] frame(String eventType, String contentType, byte[] payload) {
        java.util.Map<String, software.amazon.awssdk.eventstream.HeaderValue> headers = new java.util.LinkedHashMap<>();
        headers.put(":message-type", software.amazon.awssdk.eventstream.HeaderValue.ofString("event"));
        headers.put(":event-type", software.amazon.awssdk.eventstream.HeaderValue.ofString(eventType));
        headers.put(":content-type", software.amazon.awssdk.eventstream.HeaderValue.ofString(contentType));
        java.nio.ByteBuffer encoded = software.amazon.awssdk.eventstream.MessageEncoder.encode(
            new software.amazon.awssdk.eventstream.Message(headers, payload));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }

    private static void assertTicks(StructDocument output, int... expected) {
        software.amazon.smithy.java.core.serde.event.EventStreamReader<?> reader =
            output.getMember("events").asEventStream().asReader();
        for (int n : expected) {
            StructDocument event = (StructDocument) reader.read();
            assertEquals(n, event.getMember("tick").getMember("n").asInteger());
        }
        assertEquals(null, reader.read());
    }
}
