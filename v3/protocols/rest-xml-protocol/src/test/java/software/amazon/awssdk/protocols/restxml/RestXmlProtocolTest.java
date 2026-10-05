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

package software.amazon.awssdk.protocols.restxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.ByteBuffer;
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
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/** Request, response and error fixtures of the restXml runtime. */
class RestXmlProtocolTest {
    private static final String NS = "https://widgets.example.com/doc";
    private static final String MODEL = """
        $version: "2"
        namespace example.widgets
        use aws.protocols#restXml

        @restXml
        @xmlNamespace(uri: "https://widgets.example.com/doc")
        service Widgets {
            version: "2020-01-01"
            operations: [GetWidget, PutWidget, Upload, Configure, Describe, Subscribe]
        }

        @http(method: "PUT", uri: "/widgets/{id}?mode=full", code: 201)
        @endpoint(hostPrefix: "{tenant}.")
        operation PutWidget {
            input := @xmlName("Widget") {
                @required
                @httpLabel
                id: String
                @required
                @hostLabel
                tenant: String
                @httpQuery("tag")
                tags: Tags
                @httpHeader("X-Request-Time")
                sent: Timestamp
                @xmlName("DisplayName")
                name: String
                created: Timestamp
                @xmlAttribute
                kind: String
            }
            output := {
                @httpHeader("X-Version")
                version: Integer
                @httpResponseCode
                status: Integer
                @xmlName("DisplayName")
                name: String
                created: Timestamp
                items: Tags
            }
            errors: [WidgetNotFound, Throttled]
        }

        @readonly
        @http(method: "GET", uri: "/widgets/{id}")
        operation GetWidget {
            input := {
                @required
                @httpLabel
                id: String
            }
            output := {
                @httpHeader("X-Name")
                name: String
            }
            errors: [WidgetNotFound]
        }

        @http(method: "POST", uri: "/upload")
        operation Upload {
            input := {
                @httpHeader("Content-Type")
                contentType: String
                @httpPayload
                data: Photo
            }
            output := {
                @httpPayload
                data: Blob
            }
        }

        @http(method: "POST", uri: "/configure")
        operation Configure {
            input := {
                @httpPayload
                @xmlName("Settings")
                config: Config
            }
            output := {
                @httpPayload
                config: Config
            }
        }

        @http(method: "POST", uri: "/describe")
        operation Describe {
            input := {
                @httpPayload
                text: String
            }
            output := {
                @httpPayload
                text: String
            }
        }

        @http(method: "GET", uri: "/events")
        operation Subscribe {
            output := {
                @httpPayload
                events: Events
            }
        }

        @mediaType("image/png")
        blob Photo

        structure Config {
            MaxSize: Integer
        }

        @streaming
        union Events {
            tick: Tick
        }

        structure Tick {
            n: Integer
        }

        list Tags {
            member: String
        }

        @error("client")
        @httpError(404)
        structure WidgetNotFound {
            Message: String
            @httpHeader("X-Widget-Id")
            widgetId: String
            Reason: String
        }

        @error("server")
        structure Throttled {
            Message: String
        }
        """;

    private static final URI ENDPOINT = URI.create("https://widgets.example.com");

    private static DynamicService service;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(RestXmlProtocolTest.class.getClassLoader())
                           .discoverModels(RestXmlProtocolTest.class.getClassLoader())
                           .addUnparsedModel("widgets.smithy", MODEL)
                           .assemble()
                           .unwrap();
        service = new DynamicService(model, ShapeId.from("example.widgets#Widgets"));
    }

    private static OperationProtocol plan(String operation) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.client(new RestXmlProtocolProvider(), true).operation(service.schemas(shape));
    }

    private static StructDocument input(String operation, String params) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.value(service.model().expectShape(shape.getInputShape()), Node.parse(params));
    }

    private static StructDocument read(String operation, SdkHttpResponse response) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return plan(operation).deserializeResponse(response,
                                                   service.builder(service.model().expectShape(shape.getOutputShape())));
    }

    private static SdkHttpResponse response(int status, HttpBody body, String... headers) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(status).body(body);
        for (int i = 0; i < headers.length; i += 2) {
            builder.addHeader(headers[i], headers[i + 1]);
        }
        return builder.build();
    }

    @Test
    void serializesBindingsAndXmlBodyUnderTheInputRoot() {
        SdkHttpRequest request = plan("PutWidget").serializeRequest(input("PutWidget", """
            {"id": "w 1", "tenant": "acme", "tags": ["a", "b"], "sent": 1576540098, "name": "n",
             "created": 0, "kind": "k"}"""), URI.create("https://widgets.example.com:8443/base"));
        assertEquals("PUT", request.method());
        assertEquals("acme.widgets.example.com", request.host());
        assertEquals(8443, request.port());
        assertEquals("/base/widgets/w%201", request.encodedPath());
        assertEquals("mode=full&tag=a&tag=b", request.encodedQuery());
        assertEquals(Optional.of("Mon, 16 Dec 2019 23:48:18 GMT"), request.headers().firstValue("X-Request-Time"));
        assertEquals(Optional.of("application/xml"), request.headers().firstValue("Content-Type"));
        // The root is the input's xmlName in the service namespace; xmlName and xmlAttribute are honored, timestamps
        // default to date-time, HTTP-bound members stay out of the body; a hostLabel member is also in the body.
        XmlBodies.assertEquivalent("<Widget xmlns=\"" + NS + "\" kind=\"k\"><tenant>acme</tenant>"
                                   + "<DisplayName>n</DisplayName><created>1970-01-01T00:00:00Z</created></Widget>",
                                   request.body().asUtf8String());
    }

    @Test
    void bodyMembersAlwaysSendADocumentAndNoBodyMembersSendNothing() {
        SdkHttpRequest members = plan("PutWidget").serializeRequest(input("PutWidget", "{\"id\":\"w\",\"tenant\":\"t\"}"),
                                                                    ENDPOINT);
        XmlBodies.assertEquivalent("<Widget xmlns=\"" + NS + "\"><tenant>t</tenant></Widget>",
                                   members.body().asUtf8String());
        SdkHttpRequest none = plan("GetWidget").serializeRequest(input("GetWidget", "{\"id\":\"w\"}"), ENDPOINT);
        assertEquals("GET", none.method());
        assertEquals(HttpBody.Kind.EMPTY, none.body().kind());
        assertFalse(none.headers().contains("Content-Type"));
    }

    @Test
    void payloadsFollowTheirTargetType() {
        SdkHttpRequest blob = plan("Upload").serializeRequest(input("Upload", "{\"data\":\"raw bytes\"}"), ENDPOINT);
        assertEquals("raw bytes", blob.body().asUtf8String());
        assertEquals(Optional.of("image/png"), blob.headers().firstValue("Content-Type"));
        SdkHttpRequest explicit = plan("Upload").serializeRequest(
            input("Upload", "{\"data\":\"x\",\"contentType\":\"image/jpeg\"}"), ENDPOINT);
        assertEquals(List.of("image/jpeg"), explicit.headers().allValues("Content-Type"));
        SdkHttpRequest noBlob = plan("Upload").serializeRequest(input("Upload", "{}"), ENDPOINT);
        assertEquals(HttpBody.Kind.EMPTY, noBlob.body().kind());
        assertFalse(noBlob.headers().contains("Content-Type"));

        // A structure payload is the payload member's element (its own xmlName), in the service namespace.
        SdkHttpRequest structure = plan("Configure").serializeRequest(input("Configure",
                                                                            "{\"config\":{\"MaxSize\":3}}"), ENDPOINT);
        XmlBodies.assertEquivalent("<Settings xmlns=\"" + NS + "\"><MaxSize>3</MaxSize></Settings>",
                                   structure.body().asUtf8String());
        assertEquals(Optional.of("application/xml"), structure.headers().firstValue("Content-Type"));
        SdkHttpRequest noStructure = plan("Configure").serializeRequest(input("Configure", "{}"), ENDPOINT);
        assertEquals(HttpBody.Kind.EMPTY, noStructure.body().kind());
        assertFalse(noStructure.headers().contains("Content-Type"));

        SdkHttpRequest text = plan("Describe").serializeRequest(input("Describe", "{\"text\":\"héllo\"}"), ENDPOINT);
        assertEquals("héllo", text.body().asUtf8String());
        assertEquals(Optional.of("text/plain"), text.headers().firstValue("Content-Type"));
    }

    @Test
    void readsHeadersStatusCodeAndXmlBody() {
        StructDocument output = read("PutWidget", response(
            201, HttpBody.fromUtf8("<AnyRoot xmlns=\"" + NS + "\"><DisplayName>n</DisplayName>"
                                   + "<created>2019-12-16T23:48:18Z</created>"
                                   + "<items><member>a</member><member>b</member></items></AnyRoot>"),
            "X-Version", "7"));
        assertEquals(7, output.getMember("version").asInteger());
        assertEquals(201, output.getMember("status").asInteger());
        assertEquals("n", output.getMember("name").asString());
        assertEquals(Instant.ofEpochSecond(1576540098), output.getMember("created").asTimestamp());
        assertEquals(2, output.getMember("items").asList().size());
        for (HttpBody blank : List.of(HttpBody.empty(), HttpBody.fromUtf8(" \n "))) {
            StructDocument empty = read("PutWidget", response(201, blank));
            assertEquals(201, empty.getMember("status").asInteger());
            assertNull(empty.getMember("name"));
        }
    }

    @Test
    void readsPayloadResponses() {
        assertEquals("abc", utf8(read("Upload", response(200, HttpBody.fromUtf8("abc")))
                                                        .getMember("data").asBlob()));
        assertEquals(3, read("Configure", response(200, HttpBody.fromUtf8("<Config><MaxSize>3</MaxSize></Config>")))
            .getMember("config").getMember("MaxSize").asInteger());
        assertNull(read("Configure", response(200, HttpBody.empty())).getMember("config"));
        assertNull(read("Configure", response(200, HttpBody.fromUtf8("\n"))).getMember("config"));
        assertEquals(" text ", read("Describe", response(200, HttpBody.fromUtf8(" text ")))
            .getMember("text").asString());
    }

    @Test
    void invalidWireInputIsLocatedWithoutWireValues() {
        SdkProtocolException header = assertThrows(SdkProtocolException.class, () -> read("PutWidget", response(
            201, HttpBody.empty(), "X-Version", "seven")));
        assertEquals(Optional.of("header X-Version"), header.wireLocation());
        assertEquals(List.of("version"), header.memberPath());
        assertEquals(ProtocolIds.REST_XML, header.protocolId());
        assertEquals(ShapeId.from("example.widgets#PutWidget"), header.operationId());
        assertFalse(header.getMessage().contains("seven"));
        SdkProtocolException body = assertThrows(SdkProtocolException.class, () -> read("PutWidget", response(
            201, HttpBody.fromUtf8("<Out><created>yesterday</created></Out>"))));
        assertEquals(List.of("created"), body.memberPath());
        assertEquals(Optional.of("body"), body.wireLocation());
        assertFalse(body.getMessage().contains("yesterday"));
        SdkProtocolException malformed = assertThrows(SdkProtocolException.class, () -> read("PutWidget", response(
            201, HttpBody.fromUtf8("<Out><DisplayName>"))));
        assertEquals(Optional.of("body"), malformed.wireLocation());
    }

    @Test
    void mapsWrappedErrorsToModeledErrorsWithBoundMembers() {
        AwsServiceException e = plan("PutWidget").deserializeError(response(
            404, HttpBody.fromUtf8("<ErrorResponse><Error><Type>Sender</Type><Code>WidgetNotFound</Code>"
                                   + "<Message>gone</Message><Reason>deleted</Reason></Error>"
                                   + "<RequestId>req-body</RequestId></ErrorResponse>"),
            "X-Widget-Id", "w-1", "x-amz-request-id", "req-header", "x-amz-id-2", "ext-1"));
        DynamicError error = assertInstanceOf(DynamicError.class, e);
        assertEquals(ShapeId.from("example.widgets#WidgetNotFound"), error.schema().id());
        assertEquals("w-1", error.value().getMember("widgetId").asString());
        assertEquals("deleted", error.value().getMember("Reason").asString());
        assertEquals("gone", error.value().getMember("Message").asString());
        assertEquals("WidgetNotFound", e.awsErrorDetails().errorCode());
        assertEquals("gone", e.awsErrorDetails().errorMessage());
        assertEquals("req-body", e.requestId());
        assertEquals("ext-1", e.extendedRequestId());
        assertEquals(404, e.statusCode());
    }

    @Test
    void mapsUnwrappedErrorsAndFallsBackToTheRequestIdHeader() {
        AwsServiceException e = plan("PutWidget").deserializeError(response(
            503, HttpBody.fromUtf8("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                   + "<Error><Code>Throttled</Code><Message>slow</Message></Error>"),
            "x-amz-request-id", "req-header"));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) e).schema().id());
        assertEquals("slow", e.awsErrorDetails().errorMessage());
        assertEquals("req-header", e.requestId());
        // An error the operation does not declare still resolves through the service registry.
        AwsServiceException registry = plan("GetWidget").deserializeError(response(
            503, HttpBody.fromUtf8("<ErrorResponse><Error><Code>Throttled</Code></Error></ErrorResponse>")));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) registry).schema().id());
    }

    @Test
    void unknownOrUnreadableErrorsAreGeneric() {
        AwsServiceException unknown = plan("PutWidget").deserializeError(response(
            500, HttpBody.fromUtf8("<ErrorResponse><Error><Code>Mystery</Code><Message>boom</Message></Error>"
                                   + "<RequestId>r</RequestId></ErrorResponse>")));
        assertFalse(unknown instanceof DynamicError);
        assertEquals("Mystery", unknown.awsErrorDetails().errorCode());
        assertEquals("boom", unknown.awsErrorDetails().errorMessage());
        assertEquals("r", unknown.requestId());
        for (String body : List.of("<html><body>Bad gateway", "", "  ", "<Other><Code>Throttled</Code></Other>")) {
            AwsServiceException generic = plan("PutWidget").deserializeError(response(502, HttpBody.fromUtf8(body)));
            assertFalse(generic instanceof DynamicError, body);
            assertNull(generic.awsErrorDetails().errorCode(), body);
            assertEquals(502, generic.statusCode());
        }
    }

    @Test
    void eventStreamPayloadsAreReadIncrementally() {
        byte[] frames = eventFrames("application/xml", null,
                                    "<Tick><n>1</n></Tick>".getBytes(StandardCharsets.UTF_8),
                                    "<Tick><n>2</n></Tick>".getBytes(StandardCharsets.UTF_8));
        SdkHttpResponse response = response(200, HttpBody.streaming(software.amazon.awssdk.http.StreamingBodies.fromInputStream(
            new java.io.ByteArrayInputStream(frames), -1, "application/vnd.amazon.eventstream", 5)));
        assertTicks(read("Subscribe", response), 1, 2);
    }

    @Test
    void rejectsInputOfAnotherOperation() {
        assertThrows(IllegalArgumentException.class,
                     () -> plan("PutWidget").serializeRequest(input("GetWidget", "{\"id\":\"w\"}"), ENDPOINT));
    }

    @Test
    void isDiscoveredAsAServiceEntry() {
        InstalledProtocols installed = InstalledProtocols.discover(RestXmlProtocolTest.class.getClassLoader());
        assertInstanceOf(RestXmlProtocolProvider.class, installed.provider(ProtocolIds.REST_XML).orElseThrow());
        assertTrue(new RestXmlProtocolProvider().usesHttpBindingTraits());
    }

    private static String utf8(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
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
