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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.InstalledProtocols;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/** Request, response and error fixtures of the restJson1 runtime. */
class RestJsonProtocolTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.widgets
        use aws.protocols#restJson1

        @restJson1
        service Widgets {
            version: "2020-01-01"
            operations: [GetWidget, PutWidget, ListWidgets, Upload, Configure, Describe, Subscribe, PutObject, PutFinite]
        }

        @http(method: "PUT", uri: "/widgets/{id}?mode=full", code: 201)
        @endpoint(hostPrefix: "{tenant}.")
        operation PutWidget {
            input := {
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
                @jsonName("display_name")
                name: String
                created: Timestamp
                @timestampFormat("date-time")
                updated: Timestamp
            }
            output := {
                @httpHeader("X-Version")
                version: Integer
                @httpResponseCode
                status: Integer
                @jsonName("display_name")
                name: String
                created: Timestamp
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

        @readonly
        @http(method: "GET", uri: "/widgets")
        operation ListWidgets {
            input := {
                @httpQuery("limit")
                limit: Integer
            }
            output := {
                items: Tags
            }
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

        @http(method: "PUT", uri: "/objects")
        operation PutObject {
            input := {
                @httpHeader("X-Meta")
                meta: String
                @required
                @httpPayload
                body: Body
            }
            output := {
                @httpHeader("X-Meta")
                meta: String
                @required
                @httpPayload
                body: Body
            }
        }
        @http(method: "PUT", uri: "/finite")
        operation PutFinite {
            input := {
                @required
                @httpPayload
                body: FiniteBody
            }
        }
        @streaming
        blob Body
        @streaming
        @requiresLength
        blob FiniteBody
        @mediaType("image/png")
        blob Photo

        structure Config {
            @jsonName("max_size")
            maxSize: Integer
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
            message: String
            @httpHeader("X-Widget-Id")
            widgetId: String
            reason: String
        }

        @error("server")
        structure Throttled {
            message: String
        }
        """;

    private static final URI ENDPOINT = URI.create("https://widgets.example.com");

    private static DynamicService service;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(RestJsonProtocolTest.class.getClassLoader())
                           .discoverModels(RestJsonProtocolTest.class.getClassLoader())
                           .addUnparsedModel("widgets.smithy", MODEL)
                           .assemble()
                           .unwrap();
        service = new DynamicService(model, ShapeId.from("example.widgets#Widgets"));
    }

    private static OperationProtocol plan(String operation) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.client(new RestJsonProtocolProvider(), true).operation(service.schemas(shape));
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
    void serializesBindingsAndJsonBody() {
        SdkHttpRequest request = plan("PutWidget").serializeRequest(input("PutWidget", """
            {"id": "w 1", "tenant": "acme", "tags": ["a", "b"], "sent": 1576540098, "name": "n",
             "created": 1.5, "updated": 0}"""), URI.create("https://widgets.example.com:8443/base"));
        assertEquals("PUT", request.method());
        assertEquals("acme.widgets.example.com", request.host());
        assertEquals(8443, request.port());
        assertEquals("/base/widgets/w%201", request.encodedPath());
        assertEquals("mode=full&tag=a&tag=b", request.encodedQuery());
        assertEquals(Optional.of("Mon, 16 Dec 2019 23:48:18 GMT"), request.headers().firstValue("X-Request-Time"));
        assertEquals(Optional.of("application/json"), request.headers().firstValue("Content-Type"));
        // jsonName is honored, timestamps default to epoch seconds, HTTP-bound members stay out of the body; a
        // hostLabel member is also serialized in the body.
        assertEquals(Node.parse("{\"tenant\":\"acme\",\"display_name\":\"n\",\"created\":1.5,"
                                + "\"updated\":\"1970-01-01T00:00:00Z\"}"),
                     Node.parse(request.body().asUtf8String()));
    }

    @Test
    void bodyMembersAlwaysSendAJsonObjectAndNoBodyMembersSendNothing() {
        SdkHttpRequest members = plan("PutWidget").serializeRequest(input("PutWidget", "{\"id\":\"w\",\"tenant\":\"t\"}"),
                                                                    ENDPOINT);
        assertEquals("{\"tenant\":\"t\"}", members.body().asUtf8String());
        assertEquals(Optional.of("application/json"), members.headers().firstValue("Content-Type"));
        SdkHttpRequest none = plan("GetWidget").serializeRequest(input("GetWidget", "{\"id\":\"w\"}"), ENDPOINT);
        assertEquals("GET", none.method());
        assertEquals(HttpBody.Kind.EMPTY, none.body().kind());
        assertFalse(none.headers().contains("Content-Type"));
        SdkHttpRequest query = plan("ListWidgets").serializeRequest(input("ListWidgets", "{\"limit\":5}"), ENDPOINT);
        assertEquals("/widgets", query.encodedPath());
        assertEquals("limit=5", query.encodedQuery());
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

        SdkHttpRequest structure = plan("Configure").serializeRequest(input("Configure",
                                                                            "{\"config\":{\"maxSize\":3}}"), ENDPOINT);
        assertEquals(Node.parse("{\"max_size\":3}"), Node.parse(structure.body().asUtf8String()));
        assertEquals(Optional.of("application/json"), structure.headers().firstValue("Content-Type"));
        assertEquals("{}", plan("Configure").serializeRequest(input("Configure", "{}"), ENDPOINT)
                                            .body().asUtf8String());

        SdkHttpRequest text = plan("Describe").serializeRequest(input("Describe", "{\"text\":\"héllo\"}"), ENDPOINT);
        assertEquals("héllo", text.body().asUtf8String());
        assertEquals(Optional.of("text/plain"), text.headers().firstValue("Content-Type"));
    }

    @Test
    void readsHeadersStatusCodeAndJsonBody() {
        StructDocument output = read("PutWidget", response(
            201, HttpBody.fromUtf8("{\"display_name\":\"n\",\"created\":512.035}"), "X-Version", "7"));
        assertEquals(7, output.getMember("version").asInteger());
        assertEquals(201, output.getMember("status").asInteger());
        assertEquals("n", output.getMember("name").asString());
        assertEquals(Instant.ofEpochSecond(512, 35_000_000), output.getMember("created").asTimestamp());
        StructDocument empty = read("PutWidget", response(201, HttpBody.empty()));
        assertEquals(201, empty.getMember("status").asInteger());
        assertEquals(null, empty.getMember("name"));
    }

    @Test
    void readsPayloadResponses() {
        assertEquals("abc", utf8(read("Upload", response(200, HttpBody.fromUtf8("abc")))
                                                        .getMember("data").asBlob()));
        assertEquals(3, read("Configure", response(200, HttpBody.fromUtf8("{\"max_size\":3}")))
            .getMember("config").getMember("maxSize").asInteger());
        assertEquals(null, read("Configure", response(200, HttpBody.empty())).getMember("config"));
        assertEquals("text", read("Describe", response(200, HttpBody.fromUtf8("text"))).getMember("text").asString());
    }

    @Test
    void invalidWireInputIsLocatedWithoutWireValues() {
        SdkProtocolException header = assertThrows(SdkProtocolException.class, () -> read("PutWidget", response(
            201, HttpBody.empty(), "X-Version", "seven")));
        assertEquals(Optional.of("header X-Version"), header.wireLocation());
        assertEquals(List.of("version"), header.memberPath());
        assertEquals(ProtocolIds.REST_JSON_1, header.protocolId());
        assertEquals(ShapeId.from("example.widgets#PutWidget"), header.operationId());
        assertFalse(header.getMessage().contains("seven"));
        SdkProtocolException body = assertThrows(SdkProtocolException.class, () -> read("PutWidget", response(
            201, HttpBody.fromUtf8("{\"created\": true}"))));
        assertEquals(List.of("created"), body.memberPath());
        assertEquals(Optional.of("body"), body.wireLocation());
    }

    @Test
    void mapsErrorTypeHeaderToModeledErrorWithBoundMembers() {
        AwsServiceException e = plan("PutWidget").deserializeError(response(
            404, HttpBody.fromUtf8("{\"message\":\"gone\",\"reason\":\"deleted\"}"),
            "X-Amzn-Errortype", "WidgetNotFound:http://internal.amazon.com/coral/", "X-Widget-Id", "w-1",
            "x-amzn-RequestId", "req-1"));
        DynamicError error = assertInstanceOf(DynamicError.class, e);
        assertEquals(ShapeId.from("example.widgets#WidgetNotFound"), error.schema().id());
        assertEquals("w-1", error.value().getMember("widgetId").asString());
        assertEquals("deleted", error.value().getMember("reason").asString());
        assertEquals("WidgetNotFound", e.awsErrorDetails().errorCode());
        assertEquals("gone", e.awsErrorDetails().errorMessage());
        assertEquals("req-1", e.requestId());
        assertEquals(404, e.statusCode());
    }

    @Test
    void mapsBodyTypeAndCodeToModeledErrors() {
        AwsServiceException byType = plan("PutWidget").deserializeError(response(
            503, HttpBody.fromUtf8("{\"__type\":\"example.widgets#Throttled\",\"message\":\"slow\"}")));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) byType).schema().id());
        AwsServiceException byCode = plan("PutWidget").deserializeError(response(
            503, HttpBody.fromUtf8("{\"code\":\"Throttled\"}")));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) byCode).schema().id());
        // An error the operation does not declare still resolves through the service registry.
        AwsServiceException registry = plan("GetWidget").deserializeError(response(
            503, HttpBody.fromUtf8("{\"code\":\"Throttled\"}")));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) registry).schema().id());
    }

    @Test
    void unknownOrUnreadableErrorsAreGeneric() {
        AwsServiceException unknown = plan("PutWidget").deserializeError(response(
            500, HttpBody.fromUtf8("{\"__type\":\"Mystery\",\"Message\":\"boom\"}")));
        assertFalse(unknown instanceof DynamicError);
        assertEquals("Mystery", unknown.awsErrorDetails().errorCode());
        assertEquals("boom", unknown.awsErrorDetails().errorMessage());
        AwsServiceException html = plan("PutWidget").deserializeError(response(502, HttpBody.fromUtf8("<html>")));
        assertEquals(null, html.awsErrorDetails().errorCode());
        assertEquals(502, html.statusCode());
    }

    @Test
    void eventStreamPayloadsAreReadIncrementally() {
        byte[] frames = eventFrames("application/json", null, "{\"n\":1}".getBytes(StandardCharsets.UTF_8),
                                    "{\"n\":2}".getBytes(StandardCharsets.UTF_8));
        SdkHttpResponse response = response(200, HttpBody.streaming(StreamingBodies.fromInputStream(
            new ByteArrayInputStream(frames), -1, "application/vnd.amazon.eventstream", 3)));
        assertTicks(read("Subscribe", response), 1, 2);
        assertFalse(plan("Subscribe").serializeRequest(input("Subscribe", "{}"), ENDPOINT).body().kind()
                    == HttpBody.Kind.STREAMING);
    }

    @Test
    void streamingBlobRequestPayloadsPassTheStreamThroughUnread() throws IOException {
        byte[] data = "a large object".getBytes(StandardCharsets.UTF_8);
        TrackingStream source = new TrackingStream(data);
        SdkHttpRequest request = plan("PutObject").serializeRequest(
            streamInput("PutObject", DataStream.ofInputStream(source, "application/x-custom", data.length)), ENDPOINT);
        assertEquals("PUT", request.method());
        assertEquals(Optional.of("m"), request.headers().firstValue("X-Meta"));
        assertEquals(Optional.of("application/x-custom"), request.headers().firstValue("Content-Type"));
        assertEquals(HttpBody.Kind.STREAMING, request.body().kind());
        StreamingBody body = request.body().streamingBody();
        assertEquals(0, source.reads, "serialization reads nothing");
        assertEquals(data.length, body.contentLength().getAsLong());
        assertFalse(body.isReplayable());
        try (InputStream in = body.openStream()) {
            assertEquals("a large object", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThrows(IllegalStateException.class, body::openStream, "a one-shot stream is never replayed");

        SdkHttpRequest replayable = plan("PutObject").serializeRequest(
            streamInput("PutObject", DataStream.ofBytes(data)), ENDPOINT);
        StreamingBody replay = replayable.body().streamingBody();
        assertTrue(replay.isReplayable());
        assertEquals(Optional.of("application/octet-stream"), replayable.headers().firstValue("Content-Type"));
        for (int attempt = 0; attempt < 2; attempt++) {
            try (InputStream in = replay.openStream()) {
                assertEquals(data.length, in.readAllBytes().length);
            }
        }
    }

    @Test
    void requiresLengthPayloadsRejectStreamsOfUnknownLength() {
        DataStream unknown = DataStream.ofInputStream(new TrackingStream(new byte[3]));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plan("PutFinite")
            .serializeRequest(streamInput("PutFinite", unknown), ENDPOINT));
        assertTrue(e.getMessage().contains("requiresLength"), e.getMessage());
        assertEquals(HttpBody.Kind.STREAMING, plan("PutFinite").serializeRequest(
            streamInput("PutFinite", DataStream.ofBytes(new byte[3])), ENDPOINT).body().kind());
    }

    @Test
    void streamingBlobResponsePayloadsAreStreamsOverTheUnreadBody() throws IOException {
        byte[] data = "downloaded".getBytes(StandardCharsets.UTF_8);
        TrackingStream source = new TrackingStream(data);
        StreamingBody body = StreamingBodies.fromInputStream(source, data.length, null);
        StructDocument output = read("PutObject", response(200, HttpBody.streaming(body), "X-Meta", "m",
                                                           "Content-Type", "text/plain"));
        assertEquals("m", output.getMember("meta").asString());
        DataStream stream = output.getMember("body").asDataStream();
        assertEquals(0, source.reads, "deserialization reads nothing");
        assertEquals("text/plain", stream.contentType());
        assertEquals(data.length, stream.contentLength());
        try (InputStream in = stream.asInputStream()) {
            assertEquals("downloaded", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static StructDocument streamInput(String operation, DataStream stream) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        Schema schema = service.schema(service.model().expectShape(shape.getInputShape()));
        ShapeBuilder<StructDocument> builder = service.builder(service.model().expectShape(shape.getInputShape()));
        if (schema.member("meta") != null) {
            builder.setMemberValue(schema.member("meta"), "m");
        }
        builder.setMemberValue(schema.member("body"), stream);
        return builder.build();
    }

    /** A byte source counting read calls. */
    private static final class TrackingStream extends ByteArrayInputStream {
        int reads;

        TrackingStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read() {
            reads++;
            return super.read();
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            reads++;
            return super.read(b, off, len);
        }
    }

    @Test
    void rejectsInputOfAnotherOperation() {
        assertThrows(IllegalArgumentException.class,
                     () -> plan("PutWidget").serializeRequest(input("GetWidget", "{\"id\":\"w\"}"), ENDPOINT));
    }

    @Test
    void isDiscoveredAsAServiceEntry() {
        InstalledProtocols installed = InstalledProtocols.discover(RestJsonProtocolTest.class.getClassLoader());
        assertInstanceOf(RestJsonProtocolProvider.class, installed.provider(ProtocolIds.REST_JSON_1).orElseThrow());
        assertTrue(new RestJsonProtocolProvider().usesHttpBindingTraits());
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
