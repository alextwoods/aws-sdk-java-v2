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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.protocols.rpcv2cbor.CborValues.map;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

/** Request, response and error fixtures of the rpcv2Cbor runtime. */
class Rpcv2CborProtocolTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.widgets

        use smithy.protocols#rpcv2Cbor
        use aws.protocols#awsQueryCompatible
        use aws.protocols#awsQueryError

        @rpcv2Cbor
        service Widgets {
            version: "2020-01-01"
            operations: [GetWidget, PutWidget, Ping, Subscribe]
        }

        @rpcv2Cbor
        @awsQueryCompatible
        service LegacyWidgets {
            version: "2020-01-01"
            operations: [GetWidget]
        }

        @http(method: "POST", uri: "/widgets/{id}")
        operation GetWidget {
            input := {
                @required
                @httpLabel
                id: String
                @jsonName("renamed")
                @httpHeader("x-name")
                name: String
                @timestampFormat("date-time")
                created: Timestamp
            }
            output := {
                id: String
                created: Timestamp
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

        operation Ping {}

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
    private static final ShapeId WIDGET_NOT_FOUND = ShapeId.from("example.widgets#WidgetNotFound");
    private static final ShapeId THROTTLED = ShapeId.from("example.widgets#Throttled");
    private static final String[] PROTOCOL = {"smithy-protocol", "rpc-v2-cbor"};

    private static DynamicService service;
    private static DynamicService legacy;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(Rpcv2CborProtocolTest.class.getClassLoader())
                           .discoverModels(Rpcv2CborProtocolTest.class.getClassLoader())
                           .addUnparsedModel("widgets.smithy", MODEL)
                           .assemble()
                           .unwrap();
        service = new DynamicService(model, ShapeId.from("example.widgets#Widgets"));
        legacy = new DynamicService(model, ShapeId.from("example.widgets#LegacyWidgets"));
    }

    private static OperationProtocol plan(DynamicService service, String operation, boolean hostPrefixInjection) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.client(new Rpcv2CborProtocolProvider(), hostPrefixInjection).operation(service.schemas(shape));
    }

    private static OperationProtocol plan(String operation) {
        return plan(service, operation, true);
    }

    private static StructDocument input(String operation, String params) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.value(service.model().expectShape(shape.getInputShape()), Node.parse(params));
    }

    private static SdkHttpResponse response(int status, byte[] body, String... headers) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(status).body(HttpBody.fromBytes(body));
        for (int i = 0; i < headers.length; i += 2) {
            builder.addHeader(headers[i], headers[i + 1]);
        }
        return builder.build();
    }

    private static SdkHttpResponse cbor(int status, Object body, String... headers) {
        String[] all = new String[headers.length + 2];
        System.arraycopy(PROTOCOL, 0, all, 0, 2);
        System.arraycopy(headers, 0, all, 2, headers.length);
        return response(status, CborValues.encode(body), all);
    }

    private static StructDocument getWidgetOutput(SdkHttpResponse response) {
        return plan("GetWidget").deserializeResponse(
            response, service.builder(service.model().expectShape(service.operation(GET_WIDGET).getOutputShape())));
    }

    @Test
    void serializesRequestToTheOperationPathIgnoringHttpBindings() {
        SdkHttpRequest request = plan("GetWidget").serializeRequest(
            input("GetWidget", "{\"id\": \"w-1\", \"name\": \"n\", \"created\": 1.5}"),
            URI.create("https://widgets.example.com:8443/base/"));
        assertEquals("POST", request.method());
        assertEquals("widgets.example.com", request.host());
        assertEquals(8443, request.port());
        assertEquals("/base/service/Widgets/operation/GetWidget", request.encodedPath());
        assertEquals(List.of("smithy-protocol", "Accept", "Content-Type"),
                     List.of(request.headers().name(0), request.headers().name(1), request.headers().name(2)));
        assertEquals(Optional.of("rpc-v2-cbor"), request.headers().firstValue("Smithy-Protocol"));
        assertEquals(Optional.of("application/cbor"), request.headers().firstValue("accept"));
        assertEquals(Optional.of("application/cbor"), request.headers().firstValue("content-type"));
        assertEquals(3, request.headers().size(), "no X-Amz-Target, x-amzn-query-mode or bound headers");
        // Modeled member names (jsonName ignored), no HTTP bindings, tag-1 epoch-seconds timestamps whatever the
        // timestampFormat.
        assertEquals(CborValues.decode(CborValues.encode(
                         map("id", "w-1", "name", "n", "created", new CborValues.Timestamp(1.5)))),
                     CborValues.decode(request.body().asByteArray()));
    }

    @Test
    void unitInputSendsNoBodyAndNoContentType() {
        OperationShape ping = service.operation(ShapeId.from("example.widgets#Ping"));
        OperationProtocol plan = plan("Ping");
        SdkHttpRequest request = plan.serializeRequest(
            service.value(service.model().expectShape(ping.getInputShape()), Node.objectNode()), ENDPOINT);
        assertEquals("/service/Widgets/operation/Ping", request.encodedPath());
        assertEquals(HttpBody.Kind.EMPTY, request.body().kind());
        assertFalse(request.headers().contains("Content-Type"));
        assertEquals(Optional.of("application/cbor"), request.headers().firstValue("Accept"));
    }

    @Test
    void emptyInputIsAnEmptyMap() {
        SdkHttpRequest request = plan("PutWidget").serializeRequest(input("PutWidget", "{\"tenant\": \"t\"}"),
                                                                    ENDPOINT);
        assertEquals(CborValues.decode(CborValues.encode(map("tenant", "t"))),
                     CborValues.decode(request.body().asByteArray()));
    }

    @Test
    void appliesHostPrefixUnlessDisabled() {
        StructDocument input = input("PutWidget", "{\"tenant\": \"acme\"}");
        assertEquals("acme.data.widgets.example.com", plan("PutWidget").serializeRequest(input, ENDPOINT).host());
        assertEquals("widgets.example.com",
                     plan(service, "PutWidget", false).serializeRequest(input, ENDPOINT).host());
        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class, () -> plan("PutWidget")
            .serializeRequest(input("PutWidget", "{\"tenant\": \"not/valid\"}"), ENDPOINT));
        assertFalse(invalid.getMessage().contains("not/valid"));
    }

    @Test
    void queryCompatibleServiceSendsQueryModeHeader() {
        OperationShape op = legacy.operation(GET_WIDGET);
        SdkHttpRequest request = plan(legacy, "GetWidget", true).serializeRequest(
            legacy.value(legacy.model().expectShape(op.getInputShape()), Node.parse("{\"id\": \"w\"}")), ENDPOINT);
        assertEquals("/service/LegacyWidgets/operation/GetWidget", request.encodedPath());
        assertEquals(Optional.of("true"), request.headers().firstValue("x-amzn-query-mode"));
    }

    @Test
    void deserializesResponseWithMillisecondTimestamps() {
        StructDocument output = getWidgetOutput(cbor(200, map(
            "id", "w", "created", new CborValues.Timestamp(512.035), "items", List.of(map("size", 3)),
            "unknown", map("nested", List.of(1, 2)))));
        Schema schema = output.schema();
        assertEquals("w", output.getMemberValue(schema.member("id")));
        assertEquals(Instant.ofEpochSecond(512, 35_000_000), output.getMemberValue(schema.member("created")));
        assertEquals(Node.parse("{\"id\":\"w\",\"created\":512.035,\"items\":[{\"size\":3}]}"),
                     DynamicService.canonical(output));
    }

    @Test
    void emptyResponseBodyIsAnEmptyOutput() {
        StructDocument output = getWidgetOutput(response(200, new byte[0], PROTOCOL));
        assertEquals(Node.parse("{}"), DynamicService.canonical(output));
    }

    @Test
    void responseWithoutTheProtocolHeaderIsMalformed() {
        for (SdkHttpResponse malformed : List.of(response(200, CborValues.encode(map("id", "w"))),
                                                 response(200, CborValues.encode(map("id", "w")),
                                                          "smithy-protocol", "rpc-v2-json"))) {
            SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> getWidgetOutput(malformed));
            assertEquals(Optional.of("header smithy-protocol"), e.wireLocation());
            assertEquals(List.of(), e.memberPath());
            assertFalse(e.getMessage().contains("rpc-v2-json"), e.getMessage());
        }
    }

    @Test
    void invalidWireInputIsLocatedWithoutWireValues() {
        SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> getWidgetOutput(
            cbor(200, map("items", List.of(map("size", 1), map("size", "secret-value"))))));
        assertEquals(ProtocolIds.RPC_V2_CBOR, e.protocolId());
        assertEquals(GET_WIDGET, e.operationId());
        assertEquals(List.of("items", "[1]", "size"), e.memberPath());
        assertEquals(ShapeId.from("smithy.api#Integer"), e.shapeId());
        assertEquals(Optional.of("body"), e.wireLocation());
        assertFalse(e.getMessage().contains("secret-value"), e.getMessage());
        assertFalse(e.retryable());

        SdkProtocolException root = assertThrows(SdkProtocolException.class, () -> getWidgetOutput(
            response(200, "{not cbor".getBytes(StandardCharsets.UTF_8), PROTOCOL)));
        assertEquals(List.of(), root.memberPath());
        SdkProtocolException untagged = assertThrows(SdkProtocolException.class, () -> getWidgetOutput(
            cbor(200, map("created", 5))));
        assertEquals(List.of("created"), untagged.memberPath());
    }

    @Test
    void onlyStatus200IsSuccess() {
        OperationProtocol plan = plan("GetWidget");
        assertFalse(plan.isError(cbor(200, map())));
        assertTrue(plan.isError(cbor(201, map())));
        assertTrue(plan.isError(response(204, new byte[0], PROTOCOL)));
        assertTrue(plan.isError(cbor(400, map())));
    }

    @Test
    void mapsAbsoluteTypeToModeledErrorIgnoringHeaderAndCode() {
        AwsServiceException e = plan("GetWidget").deserializeError(cbor(
            404, map("__type", "example.widgets#WidgetNotFound", "code", "Throttled", "message", "gone",
                     "widgetId", "w-1"),
            "X-Amzn-Errortype", "Throttled", "x-amzn-RequestId", "req-1"));
        DynamicError error = assertInstanceOf(DynamicError.class, e);
        assertEquals(WIDGET_NOT_FOUND, error.schema().id());
        assertEquals(404, error.statusCode());
        assertEquals("req-1", error.requestId());
        assertEquals("WidgetNotFound", error.awsErrorDetails().errorCode());
        assertEquals("gone", error.awsErrorDetails().errorMessage());
        assertEquals("Widgets", error.awsErrorDetails().serviceName());
        assertEquals("w-1", error.getMemberValue(error.schema().member("widgetId")));
    }

    @Test
    void mapsRelativeTypeAndCapitalizedMessage() {
        AwsServiceException e = plan("GetWidget").deserializeError(cbor(500, map("__type", "Throttled",
                                                                                 "Message", "slow")));
        assertEquals(THROTTLED, ((DynamicError) e).schema().id());
        assertEquals("slow", e.awsErrorDetails().errorMessage());
    }

    @Test
    void codeOrErrorTypeHeaderAloneIdentifyNoModeledError() {
        AwsServiceException e = plan("GetWidget").deserializeError(cbor(
            500, map("code", "Throttled"), "X-Amzn-Errortype", "Throttled"));
        assertEquals(AwsServiceException.class, e.getClass());
        assertNull(e.awsErrorDetails().errorCode());
    }

    @Test
    void awsQueryCompatibleReportsQueryCodeAndResolvesByIt() {
        OperationProtocol plan = plan(legacy, "GetWidget", true);
        AwsServiceException typed = plan.deserializeError(cbor(
            400, map("__type", "example.widgets#WidgetNotFound"), "x-amzn-query-error", "AWS.Widgets.NotFound;Sender"));
        assertEquals(WIDGET_NOT_FOUND, ((DynamicError) typed).schema().id());
        assertEquals("AWS.Widgets.NotFound", typed.awsErrorDetails().errorCode());
        AwsServiceException byQueryCode = plan.deserializeError(cbor(
            400, map(), "x-amzn-query-error", "AWS.Widgets.NotFound;Sender"));
        assertEquals(WIDGET_NOT_FOUND, ((DynamicError) byQueryCode).schema().id());
        // Without awsQueryCompatible the header is not interpreted.
        AwsServiceException notCompatible = plan("GetWidget").deserializeError(cbor(
            400, map(), "x-amzn-query-error", "AWS.Widgets.NotFound;Sender"));
        assertEquals(AwsServiceException.class, notCompatible.getClass());
    }

    @Test
    void unknownOrUnreadableErrorIsGenericServiceException() {
        AwsServiceException e = plan("GetWidget").deserializeError(cbor(
            503, map("__type", "aws.other#Mystery", "message", "what"), "x-amzn-RequestId", "req-2",
            "x-amz-id-2", "ext-2"));
        assertEquals(AwsServiceException.class, e.getClass());
        assertEquals("Mystery", e.awsErrorDetails().errorCode());
        assertEquals("what", e.awsErrorDetails().errorMessage());
        assertEquals("req-2", e.requestId());
        assertEquals("ext-2", e.extendedRequestId());
        assertEquals(503, e.statusCode());

        AwsServiceException notCbor = plan("GetWidget").deserializeError(
            response(502, "<html/>".getBytes(StandardCharsets.UTF_8), PROTOCOL));
        assertEquals(AwsServiceException.class, notCbor.getClass());
        assertEquals(502, notCbor.statusCode());
    }

    @Test
    void malformedErrorIsReportedFromItsStatusCodeAlone() {
        AwsServiceException e = plan("GetWidget").deserializeError(response(
            503, CborValues.encode(map("__type", "example.widgets#Throttled", "message", "slow")),
            "x-amzn-RequestId", "req-3"));
        assertEquals(AwsServiceException.class, e.getClass());
        assertEquals(503, e.statusCode());
        assertNull(e.awsErrorDetails().errorCode());
        assertNull(e.awsErrorDetails().errorMessage());
        assertEquals("req-3", e.requestId());
    }

    @Test
    void modeledErrorBodyViolatingItsSchemaIsLocated() {
        SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> plan("GetWidget").deserializeError(
            cbor(404, map("__type", "example.widgets#WidgetNotFound", "widgetId", 7))));
        assertEquals(List.of("widgetId"), e.memberPath());
        assertEquals(ShapeId.from("smithy.api#String"), e.shapeId());
    }

    @Test
    void eventStreamOutputsReadTheInitialResponseThenEvents() {
        // CBOR {} and {"n": 1} / {"n": 2}.
        byte[] frames = eventFrames("application/cbor", new byte[] {(byte) 0xA0},
                                    new byte[] {(byte) 0xA1, 0x61, 'n', 0x01}, new byte[] {(byte) 0xA1, 0x61, 'n', 0x02});
        SdkHttpResponse response = response(200, frames, PROTOCOL);
        OperationShape subscribe = service.operation(ShapeId.from("example.widgets#Subscribe"));
        StructDocument output = plan("Subscribe").deserializeResponse(
            response, service.builder(service.model().expectShape(subscribe.getOutputShape())));
        assertTicks(output, 1, 2);
        assertEquals(java.util.Optional.of("application/vnd.amazon.eventstream"),
                     plan("Subscribe").serializeRequest(input("Subscribe", "{}"), ENDPOINT).headers()
                                      .firstValue("Accept"));
    }

    @Test
    void rejectsInputOrOutputOfAnotherOperation() {
        assertThrows(IllegalArgumentException.class, () -> plan("GetWidget")
            .serializeRequest(input("PutWidget", "{\"tenant\":\"a\"}"), ENDPOINT));
        OperationShape put = service.operation(ShapeId.from("example.widgets#PutWidget"));
        assertThrows(IllegalArgumentException.class, () -> plan("GetWidget").deserializeResponse(
            cbor(200, map()), service.builder(service.model().expectShape(put.getOutputShape()))));
    }

    @Test
    void providerIsDiscoveredAsAServiceEntry() {
        InstalledProtocols installed = InstalledProtocols.discover(Rpcv2CborProtocolTest.class.getClassLoader());
        assertInstanceOf(Rpcv2CborProtocolProvider.class, installed.provider(ProtocolIds.RPC_V2_CBOR).orElseThrow());
        assertFalse(new Rpcv2CborProtocolProvider().usesHttpBindingTraits());
        assertEquals(ProtocolIds.RPC_V2_CBOR, service.client(new Rpcv2CborProtocolProvider(), true).protocolId());
    }

    @Test
    void semanticCborComparisonIgnoresEncodingChoices() {
        // Indefinite map {"a": 1.0 (float16), "b": [indefinite text "x" "y"]} versus definite {"b": ["xy"], "a": 1}.
        byte[] indefinite = {(byte) 0xBF, 0x61, 'a', (byte) 0xF9, 0x3C, 0x00, 0x61, 'b', (byte) 0x9F, 0x7F, 0x61, 'x',
                             0x61, 'y', (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        byte[] definite = CborValues.encode(map("b", List.of("xy"), "a", 1));
        assertEquals(CborValues.decode(definite), CborValues.decode(indefinite));
        assertFalse(CborValues.decode(CborValues.encode(map("a", 1))).equals(
            CborValues.decode(CborValues.encode(map("a", 2)))));
        assertEquals(new BigDecimal("0.1"), CborValues.number(new BigDecimal("0.10")));
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
