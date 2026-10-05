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

package software.amazon.awssdk.protocols.core.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings.Direction;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

class HttpBindingReaderTest {
    private static final WireContext WIRE = new WireContext(ShapeId.from("aws.protocols#restJson1"),
                                                            ShapeId.from("com.example#GetWidget"));
    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static final HttpBindingReader.BodyDeserializerFactory BODY = JSON::createDeserializer;

    private static final Schema STRING_LIST = Schema.listBuilder(id("StringList"))
                                                    .putMember("member", PreludeSchemas.STRING).build();
    private static final Schema TIMESTAMP_LIST = Schema.listBuilder(id("TimestampList"))
                                                       .putMember("member", PreludeSchemas.TIMESTAMP).build();
    private static final Schema STRING_MAP = Schema.mapBuilder(id("StringMap"))
                                                   .putMember("key", PreludeSchemas.STRING)
                                                   .putMember("value", PreludeSchemas.STRING).build();
    private static final Schema JSON_TEXT = Schema.createString(id("JsonText"), new MediaTypeTrait("application/json"));
    private static final Schema NESTED = Schema.structureBuilder(id("Nested"))
                                               .putMember("a", PreludeSchemas.STRING).build();

    private static ShapeId id(String name) {
        return ShapeId.from("com.example#" + name);
    }

    private static Schema output() {
        return Schema.structureBuilder(id("GetWidgetOutput"))
                     .putMember("name", PreludeSchemas.STRING, new HttpHeaderTrait("X-Name"))
                     .putMember("count", PreludeSchemas.INTEGER, new HttpHeaderTrait("X-Count"))
                     .putMember("enabled", PreludeSchemas.BOOLEAN, new HttpHeaderTrait("X-Enabled"))
                     .putMember("ratio", PreludeSchemas.DOUBLE, new HttpHeaderTrait("X-Ratio"))
                     .putMember("data", PreludeSchemas.BLOB, new HttpHeaderTrait("X-Data"))
                     .putMember("json", JSON_TEXT, new HttpHeaderTrait("X-Json"))
                     .putMember("modified", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-Modified"))
                     .putMember("epoch", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-Epoch"),
                                new TimestampFormatTrait("epoch-seconds"))
                     .putMember("tags", STRING_LIST, new HttpHeaderTrait("X-Tags"))
                     .putMember("dates", TIMESTAMP_LIST, new HttpHeaderTrait("X-Dates"))
                     .putMember("meta", STRING_MAP, new HttpPrefixHeadersTrait("x-meta-"))
                     .putMember("status", PreludeSchemas.INTEGER, new HttpResponseCodeTrait())
                     .putMember("body", PreludeSchemas.STRING)
                     .putMember("created", PreludeSchemas.TIMESTAMP)
                     .build();
    }

    private static StructDocument read(Schema structure, SdkHttpResponse response) {
        return read(structure, response, null);
    }

    private static StructDocument read(Schema structure, SdkHttpResponse response,
                                       LocatingDeserializer.TimestampReader timestamps) {
        HttpBindingReader reader = HttpBindingReader.of(HttpMessageBindings.of(structure, Direction.RESPONSE));
        return reader.read(WIRE, response, BODY, SchemaConverter.createDocumentBuilder(structure), timestamps);
    }

    private static Document member(StructDocument value, String name) {
        return value.getMember(name);
    }

    @Test
    void readsHeadersPrefixHeadersStatusCodeAndBodyMembers() {
        SdkHttpResponse response = SdkHttpResponse.builder()
            .statusCode(201)
            .addHeader("X-Name", "widget")
            .addHeader("X-Count", "42")
            .addHeader("X-Enabled", "true")
            .addHeader("X-Ratio", "NaN")
            .addHeader("X-Data", "aGVsbG8=")
            .addHeader("X-Json", "eyJhIjoxfQ==")
            .addHeader("X-Modified", "Mon, 16 Dec 2019 23:48:18 GMT")
            .addHeader("X-Epoch", "1576540098.5")
            .addHeader("X-Tags", "a, \"b,c\", \"d\\\"e\"")
            .addHeader("X-Tags", "f")
            .addHeader("X-Dates", "Mon, 16 Dec 2019 23:48:18 GMT, Tue, 17 Dec 2019 23:48:18 GMT")
            .addHeader("X-Meta-Color", "blue")
            .addHeader("x-meta-size", "large")
            .addHeader("X-Meta-Color", "ignored repeat")
            .body(HttpBody.fromUtf8("{\"body\":\"in body\",\"name\":\"not from body\",\"created\":1.5}"))
            .build();
        StructDocument value = read(output(), response);

        assertEquals("widget", member(value, "name").asString());
        assertEquals(42, member(value, "count").asInteger());
        assertTrue(member(value, "enabled").asBoolean());
        assertTrue(Double.isNaN(member(value, "ratio").asDouble()));
        assertEquals(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), member(value, "data").asBlob());
        assertEquals("{\"a\":1}", member(value, "json").asString());
        assertEquals(Instant.parse("2019-12-16T23:48:18Z"), member(value, "modified").asTimestamp());
        assertEquals(Instant.ofEpochSecond(1576540098, 500_000_000), member(value, "epoch").asTimestamp());
        assertEquals(List.of("a", "b,c", "d\"e", "f"), strings(member(value, "tags")));
        assertEquals(List.of(Instant.parse("2019-12-16T23:48:18Z"), Instant.parse("2019-12-17T23:48:18Z")),
                     member(value, "dates").asList().stream().map(Document::asTimestamp).toList());
        Map<String, Document> meta = member(value, "meta").asStringMap();
        assertEquals(List.of("Color", "size"), new ArrayList<>(meta.keySet()));
        assertEquals("blue", meta.get("Color").asString());
        assertEquals(201, member(value, "status").asInteger());
        assertEquals("in body", member(value, "body").asString());
        assertEquals(Instant.ofEpochSecond(1, 500_000_000), member(value, "created").asTimestamp());
    }

    private static List<String> strings(Document list) {
        return list.asList().stream().map(Document::asString).toList();
    }

    @Test
    void absentHeadersAndEmptyBodyLeaveMembersAbsentButNoMatchingPrefixReadsAnEmptyMap() {
        StructDocument value = read(output(), SdkHttpResponse.builder()
                                                             .statusCode(200)
                                                             .addHeader("X-Metadata", "not prefixed by x-meta-")
                                                             .build());
        assertNull(member(value, "name"));
        // Official restXml response case HttpPrefixHeadersAreNotPresent: no x-foo-* header reads fooMap: {}.
        Document meta = member(value, "meta");
        assertNotNull(meta);
        assertTrue(meta.asStringMap().isEmpty());
        assertNull(member(value, "body"));
        assertEquals(200, member(value, "status").asInteger());
    }

    @Test
    void bodyTimestampReaderIsNotAppliedToHeaders() {
        LocatingDeserializer.TimestampReader failing = (schema, delegate) -> {
            throw new IllegalStateException("body reader used");
        };
        StructDocument value = read(output(), SdkHttpResponse.builder()
                                                             .statusCode(200)
                                                             .addHeader("X-Modified", "Mon, 16 Dec 2019 23:48:18 GMT")
                                                             .build(), failing);
        assertEquals(Instant.parse("2019-12-16T23:48:18Z"), member(value, "modified").asTimestamp());
        assertThrows(SdkProtocolException.class, () -> read(output(), SdkHttpResponse.builder().statusCode(200)
            .body(HttpBody.fromUtf8("{\"created\":1}")).build(), failing));
    }

    @Test
    void invalidHeaderIsLocatedAtTheHeaderWithoutItsValue() {
        SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> read(
            output(), SdkHttpResponse.builder().statusCode(200).addHeader("X-Count", "secret-value").build()));
        assertEquals(List.of("count"), e.memberPath());
        assertEquals(Optional.of("header X-Count"), e.wireLocation());
        assertEquals(PreludeSchemas.INTEGER.id(), e.shapeId());
        assertFalse(e.getMessage().contains("secret-value"), e.getMessage());
        assertThrows(SdkProtocolException.class, () -> read(
            output(), SdkHttpResponse.builder().statusCode(200).addHeader("X-Ratio", "0x1p3").build()));
    }

    @Test
    void invalidBodyIsLocatedAtTheBodyMember() {
        SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> read(
            output(), SdkHttpResponse.builder().statusCode(200)
                                   .body(HttpBody.fromUtf8("{\"body\":\"x\",\"created\":true}")).build()));
        assertEquals(List.of("created"), e.memberPath());
        assertEquals(Optional.of(WireContext.BODY), e.wireLocation());
    }

    @Test
    void payloadsAreReadByKind() {
        Schema blob = payloadStructure("BlobOutput", PreludeSchemas.BLOB);
        assertEquals(ByteBuffer.wrap(new byte[] {1, 2, 3}),
                     member(read(blob, withBody(HttpBody.fromBytes(new byte[] {1, 2, 3}))), "payload").asBlob());
        Schema text = payloadStructure("TextOutput", PreludeSchemas.STRING);
        assertEquals("héllo", member(read(text, withBody(HttpBody.fromUtf8("héllo"))), "payload").asString());
        Schema structure = payloadStructure("StructOutput", NESTED);
        assertEquals("x", member(read(structure, withBody(HttpBody.fromUtf8("{\"a\":\"x\"}"))), "payload")
            .getMember("a").asString());
        Schema document = payloadStructure("DocOutput", PreludeSchemas.DOCUMENT);
        assertEquals(List.of(1, 2), member(read(document, withBody(HttpBody.fromUtf8("[1,2]"))), "payload")
            .asList().stream().map(Document::asInteger).toList());
        for (Schema schema : List.of(blob, text, structure, document)) {
            assertNull(member(read(schema, withBody(HttpBody.empty())), "payload"), schema.id().toString());
        }
    }

    @Test
    void streamingBlobPayloadsAreStreamsOverTheUnreadBody() throws Exception {
        Schema stream = Schema.createBlob(id("Stream"), new StreamingTrait());
        Schema structure = payloadStructure("StreamOutput", stream);
        byte[] data = "streamed".getBytes(StandardCharsets.UTF_8);
        StreamingBody body = StreamingBodies.fromInputStream(new ByteArrayInputStream(data), data.length, null);
        SdkHttpResponse response = SdkHttpResponse.builder().addHeader("Content-Type", "text/plain")
                                                  .body(HttpBody.streaming(body)).build();
        DataStream value = member(read(structure, response), "payload").asDataStream();
        assertTrue(body.isAvailable(), "deserialization does not read the stream");
        assertEquals("text/plain", value.contentType());
        assertEquals(data.length, value.contentLength());
        assertFalse(value.isReplayable());
        try (InputStream in = value.asInputStream()) {
            assertEquals("streamed", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertFalse(body.isAvailable());
        assertEquals(0, member(read(structure, withBody(HttpBody.empty())), "payload").asDataStream()
                                                                                      .contentLength());
        assertEquals(ByteBuffer.wrap(data), member(read(structure, withBody(HttpBody.fromBytes(data))), "payload")
            .asDataStream().asByteBuffer());
    }

    @Test
    void readWithoutStreamingPayload_bindsHeadersOnly_andLeavesTheBodyUnread() {
        Schema structure = payloadStructure("StreamOutput", Schema.createBlob(id("Stream"), new StreamingTrait()));
        byte[] data = "streamed".getBytes(StandardCharsets.UTF_8);
        StreamingBody body = StreamingBodies.fromInputStream(new ByteArrayInputStream(data), data.length, null);
        SdkHttpResponse response = SdkHttpResponse.builder().addHeader("X-Name", "widget")
                                                  .body(HttpBody.streaming(body)).build();
        HttpBindingReader reader = HttpBindingReader.of(HttpMessageBindings.of(structure, Direction.RESPONSE));

        StructDocument value = reader.readWithoutStreamingPayload(WIRE, response, BODY,
                                                                  SchemaConverter.createDocumentBuilder(structure),
                                                                  null);

        assertEquals("widget", member(value, "name").asString());
        assertEquals(null, member(value, "payload"));
        assertTrue(body.isAvailable(), "the payload is left to the caller");
        HttpBindingReader finite = HttpBindingReader.of(HttpMessageBindings.of(
            payloadStructure("TextOutput", PreludeSchemas.STRING), Direction.RESPONSE));
        assertThrows(IllegalArgumentException.class, () -> finite.readWithoutStreamingPayload(
            WIRE, response, BODY, SchemaConverter.createDocumentBuilder(structure), null));
    }

    @Test
    void streamingBodiesOfFiniteOutputsAreBufferedAndEventStreamsNeedAFactory() {
        Schema text = payloadStructure("TextOutput", PreludeSchemas.STRING);
        byte[] data = "héllo".getBytes(StandardCharsets.UTF_8);
        HttpBody body = HttpBody.streaming(StreamingBodies.fromInputStream(new ByteArrayInputStream(data),
                                                                           data.length, null));
        assertEquals("héllo", member(read(text, withBody(body)), "payload").asString());
        Schema events = Schema.unionBuilder(id("Events"), new StreamingTrait())
                              .putMember("tick", PreludeSchemas.STRING).build();
        assertThrows(UnsupportedOperationException.class,
                     () -> read(payloadStructure("EventOutput", events), withBody(HttpBody.empty())));
    }

    @Test
    void requestBindingsAndMismatchedBuildersAreRejected() {
        assertThrows(IllegalArgumentException.class,
                     () -> HttpBindingReader.of(HttpMessageBindings.of(output(), Direction.REQUEST)));
        HttpBindingReader reader = HttpBindingReader.of(HttpMessageBindings.of(output(), Direction.RESPONSE));
        assertThrows(IllegalArgumentException.class,
                     () -> reader.read(WIRE, withBody(HttpBody.empty()), BODY,
                                       SchemaConverter.createDocumentBuilder(NESTED), null));
    }

    @Test
    void headerListsSplitOutsideQuotesAndHttpDatesAtEverySecondComma() {
        Schema tags = output().member("tags");
        assertEquals(List.of("a", "b", "c"), HttpBindingReader.splitHeaderList(tags, List.of("a,b , c")));
        assertEquals(List.of("", "x"), HttpBindingReader.splitHeaderList(tags, List.of(" ,x")));
        assertEquals(List.of(" spaced ", "q\\"), HttpBindingReader.splitHeaderList(
            tags, List.of("\" spaced \", \"q\\\\\"")));
        Schema dates = output().member("dates");
        assertEquals(List.of("Mon, 16 Dec 2019 23:48:18 GMT", "Tue, 17 Dec 2019 23:48:18 GMT"),
                     HttpBindingReader.splitHeaderList(dates, List.of(
                         "Mon, 16 Dec 2019 23:48:18 GMT, Tue, 17 Dec 2019 23:48:18 GMT")));
    }

    private static Schema payloadStructure(String name, Schema target) {
        return Schema.structureBuilder(id(name))
                     .putMember("payload", target, new HttpPayloadTrait())
                     .putMember("name", PreludeSchemas.STRING, new HttpHeaderTrait("X-Name"))
                     .build();
    }

    private static SdkHttpResponse withBody(HttpBody body) {
        return SdkHttpResponse.builder().statusCode(200).body(body).build();
    }
}
