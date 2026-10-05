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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.dynamic.client.TestSupport.Recorded;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;

/**
 * Requirements 14.16, 19.6: a dynamic call writes structure members in modeled declaration order, as generated types
 * and V2 do, although Smithy Java indexes required members first. The input and the structures nested in it (directly,
 * as list elements and as map values) declare optional members before required ones. Every protocol with ordered
 * structure members is covered: JSON object members, XML elements, query form fields and CBOR map keys.
 */
class DeclaredMemberOrderTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.order

        PROTOCOL
        @aws.api#service(sdkId: "Order")
        @xmlNamespace(uri: "https://example.com/")
        service Order {
            version: "2020-01-01"
            operations: [Put]
        }

        HTTP
        operation Put {
            input := {
                note: String
                @required
                first: String
                items: Items
                byName: ItemMap
                item: Item
            }
        }

        structure Item {
            tag: String
            @required
            id: String
            count: Integer
            @required
            name: String
        }

        list Items {
            member: Item
        }

        map ItemMap {
            key: String
            value: Item
        }
        """;

    private static final String ITEM = "{\"tag\":\"t\",\"id\":\"i\",\"count\":1,\"name\":\"n\"}";
    private static final String JSON_BODY = "{\"note\":\"x\",\"first\":\"f\",\"items\":[" + ITEM + "],\"byName\":{\"k\":"
                                            + ITEM + "},\"item\":" + ITEM + "}";

    @Test
    void awsJson10BodiesUseDeclarationOrder() {
        Recorded request = call("@aws.protocols#awsJson1_0", "",
                                r -> TestSupport.json(200, "{}"));
        assertEquals(JSON_BODY, request.body());
    }

    @Test
    void restJson1BodiesUseDeclarationOrder() {
        Recorded request = call("@aws.protocols#restJson1", "@http(method: \"POST\", uri: \"/put\")",
                                r -> TestSupport.restJson(200, "{}"));
        assertEquals(JSON_BODY, request.body());
    }

    @Test
    void restXmlElementsUseDeclarationOrder() {
        Recorded request = call("@aws.protocols#restXml", "@http(method: \"POST\", uri: \"/put\")",
                                r -> response(200, "application/xml", ""));
        List<String> item = List.of("<tag>t</tag>", "<id>i</id>", "<count>1</count>", "<name>n</name>");
        assertInOrder(request.body(), concat(List.of("<note>x</note>", "<first>f</first>", "<items>"), item,
                                             List.of("<byName>", "<key>k</key>"), item, List.of("<item>"), item));
    }

    @Test
    void awsQueryFieldsUseDeclarationOrder() {
        Recorded request = call("@aws.protocols#awsQuery", "",
                                r -> response(200, "text/xml",
                                              "<PutResponse xmlns=\"https://example.com/\"><PutResult/>"
                                              + "</PutResponse>"));
        assertInOrder(request.body(), concat(List.of("note=x", "first=f"), fields("items.member.1."),
                                             List.of("byName.entry.1.key=k"), fields("byName.entry.1.value."),
                                             fields("item.")));
    }

    @Test
    void ec2QueryFieldsUseDeclarationOrder() {
        Recorded request = call("@aws.protocols#ec2Query", "",
                                r -> response(200, "text/xml",
                                              "<PutResponse xmlns=\"https://example.com/\"></PutResponse>"));
        assertInOrder(request.body(), concat(List.of("Note=x", "First=f"), ec2Fields("Items.1."),
                                             ec2Fields("Item.")));
    }

    @Test
    void rpcv2CborKeysUseDeclarationOrder() {
        Recorded request = call("@smithy.protocols#rpcv2Cbor", "",
                                r -> SdkHttpResponse.builder().statusCode(200)
                                                    .addHeader("Content-Type", "application/cbor")
                                                    .addHeader("smithy-protocol", "rpc-v2-cbor")
                                                    .body(HttpBody.fromBytes(new byte[] {(byte) 0xA0})).build());
        List<String> item = List.of("tag", "id", "count", "name");
        assertInOrder(request.body(), concat(List.of("note", "first", "items"), item, List.of("byName"), item,
                                             List.of("item"), item));
    }

    /**
     * The events of an input event stream are encoded by the protocol apart from the request, so the request view
     * also views each event: an event structure, and a structure nested in it, that declare an optional member
     * before a required one are written in declaration order (found by Property 54).
     */
    @Test
    void inputEventPayloadsUseDeclarationOrder() throws Exception {
        String idl = """
            $version: "2"
            namespace example.order

            @aws.protocols#restJson1
            @aws.api#service(sdkId: "Order")
            service Order {
                version: "2020-01-01"
                operations: [Stream]
            }

            @http(method: "POST", uri: "/stream")
            operation Stream {
                input := {
                    @httpPayload
                    events: Events
                }
            }

            @streaming
            union Events {
                entry: Entry
            }

            structure Entry {
                note: String
                @required
                level: Integer
                item: Item
            }

            structure Item {
                tag: String
                @required
                id: String
                count: Integer
                @required
                name: String
            }
            """;
        TestTransport transport = TestTransport.sync(r -> TestSupport.restJson(200, "{}"));
        try (DynamicClient client = TestSupport.syncClient(transport).model(TestSupport.load(idl))
                                               .endpointOverride(URI.create("https://order.example.com"))
                                               .build()) {
            EventStream<SerializableStruct> writer = EventStream.newWriter();
            Map<String, Document> entry = new LinkedHashMap<>();
            entry.put("item", item());
            entry.put("level", Document.of(3));
            entry.put("note", Document.of("x"));
            CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
                writer.asWriter().write(client.inputEvent("Stream", Document.of(Map.of("entry",
                                                                                       Document.of(entry)))));
                writer.close();
            });
            client.call("Stream", Document.of(Map.of("events", Document.ofObject(writer))));
            producer.get(10, TimeUnit.SECONDS);
        }
        assertTrue(transport.requests().get(0).body().contains("{\"note\":\"x\",\"level\":3,\"item\":" + ITEM + "}"),
                   transport.requests().get(0).body());
    }

    private static List<String> ec2Fields(String prefix) {
        return List.of(prefix + "Tag=t", prefix + "Id=i", prefix + "Count=1", prefix + "Name=n");
    }

    private static List<String> fields(String prefix) {
        return List.of(prefix + "tag=t", prefix + "id=i", prefix + "count=1", prefix + "name=n");
    }

    @SafeVarargs
    private static List<String> concat(List<String>... parts) {
        return java.util.Arrays.stream(parts).flatMap(List::stream).toList();
    }

    /** Each token occurs in {@code body}, each after the previous one. */
    private static void assertInOrder(String body, List<String> tokens) {
        int from = 0;
        for (String token : tokens) {
            int at = body.indexOf(token, from);
            assertTrue(at >= 0, () -> "Expected " + token + " after offset in " + body + " (tokens " + tokens + ")");
            from = at + token.length();
        }
    }

    private static Recorded call(String protocol, String http, Function<Recorded, SdkHttpResponse> responder) {
        TestTransport transport = TestTransport.sync(responder);
        String idl = MODEL.replace("PROTOCOL", protocol).replace("HTTP", http);
        try (DynamicClient client = TestSupport.syncClient(transport).model(TestSupport.load(idl))
                                               .endpointOverride(URI.create("https://order.example.com"))
                                               .build()) {
            client.call("Put", input());
        }
        assertEquals(1, transport.transmissions());
        return transport.requests().get(0);
    }

    /** The input, built with members in an order unrelated to both orders. */
    private static Document input() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("item", item());
        members.put("byName", Document.of(Map.of("k", item())));
        members.put("first", Document.of("f"));
        members.put("items", Document.of(List.of(item())));
        members.put("note", Document.of("x"));
        return Document.of(members);
    }

    private static Document item() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("name", Document.of("n"));
        members.put("count", Document.of(1));
        members.put("id", Document.of("i"));
        members.put("tag", Document.of("t"));
        return Document.of(members);
    }

    private static SdkHttpResponse response(int status, String contentType, String body) {
        return SdkHttpResponse.builder().statusCode(status).addHeader("Content-Type", contentType)
                              .body(HttpBody.fromUtf8(body)).build();
    }
}
