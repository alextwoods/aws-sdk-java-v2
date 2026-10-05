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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.protocols.core.binding.HttpOperationBindings;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

class HttpBindingWriterTest {
    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static final Schema UNIT = Schema.structureBuilder(ShapeId.from("smithy.api#Unit"), new UnitTypeTrait())
                                             .build();
    private static final Schema STRING_LIST = Schema.listBuilder(id("StringList"))
                                                    .putMember("member", PreludeSchemas.STRING).build();
    private static final Schema TIMESTAMP_LIST = Schema.listBuilder(id("TimestampList"))
                                                       .putMember("member", PreludeSchemas.TIMESTAMP).build();
    private static final Schema STRING_MAP = Schema.mapBuilder(id("StringMap"))
                                                   .putMember("key", PreludeSchemas.STRING)
                                                   .putMember("value", PreludeSchemas.STRING).build();
    private static final Schema JSON_TEXT = Schema.createString(id("JsonText"), new MediaTypeTrait("application/json"));

    private static ShapeId id(String name) {
        return ShapeId.from("com.example#" + name);
    }

    private static final Schema INPUT = Schema.structureBuilder(id("PutWidgetInput"))
        .putMember("bucket", PreludeSchemas.STRING, new HttpLabelTrait())
        .putMember("key", PreludeSchemas.STRING, new HttpLabelTrait())
        .putMember("version", PreludeSchemas.STRING, new HttpQueryTrait("v"))
        .putMember("since", PreludeSchemas.TIMESTAMP, new HttpQueryTrait("since"))
        .putMember("ids", STRING_LIST, new HttpQueryTrait("id"))
        .putMember("extra", STRING_MAP, new HttpQueryParamsTrait())
        .putMember("ratio", PreludeSchemas.FLOAT, new HttpHeaderTrait("X-Ratio"))
        .putMember("modified", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-Modified"))
        .putMember("epoch", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-Epoch"),
                   new TimestampFormatTrait("epoch-seconds"))
        .putMember("tags", STRING_LIST, new HttpHeaderTrait("X-Tags"))
        .putMember("dates", TIMESTAMP_LIST, new HttpHeaderTrait("X-Dates"))
        .putMember("json", JSON_TEXT, new HttpHeaderTrait("X-Json"))
        .putMember("data", PreludeSchemas.BLOB, new HttpHeaderTrait("X-Data"))
        .putMember("color", PreludeSchemas.STRING, new HttpHeaderTrait("x-meta-color"))
        .putMember("meta", STRING_MAP, new HttpPrefixHeadersTrait("x-meta-"))
        .putMember("name", PreludeSchemas.STRING)
        .putMember("size", PreludeSchemas.INTEGER)
        .build();

    private static HttpBindingWriter writer(Schema input, String uri) {
        Schema operation = Schema.createOperation(
            id("PutWidget"), HttpTrait.builder().method("PUT").uri(UriPattern.parse(uri)).code(200).build());
        return HttpBindingWriter.of(HttpOperationBindings.of(
            OperationSchemas.builder(operation).input(input).output(UNIT).build()));
    }

    private static SerializableStruct value(Schema schema, String json) {
        return JSON.deserializeShape(json.getBytes(StandardCharsets.UTF_8),
                                     SchemaConverter.createDocumentBuilder(schema));
    }

    private static SdkHttpRequest write(HttpBindingWriter writer, SerializableStruct input, String basePath) {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder().method("PUT").scheme("https").host("example.com");
        writer.write(input, basePath, request);
        return request.build();
    }

    @Test
    void writesLabelsQueryAndHeaders() {
        SerializableStruct input = value(INPUT, """
            {"bucket": "my bucket", "key": "a/b c", "version": "3", "since": 1576540098,
             "ids": ["x", "y"], "extra": {"v": "shadowed", "flag": "on"}, "ratio": 1.1,
             "modified": 1576540098, "epoch": 1576540098.5, "tags": ["a", "b,c", "d\\"e"],
             "dates": [1576540098, 1576626498], "json": "{\\"a\\":1}", "data": "aGVsbG8=",
             "color": "blue", "meta": {"color": "red", "size": "large"}, "name": "body only"}
            """);
        SdkHttpRequest request = write(writer(INPUT, "/{bucket}/{key+}?x-id=Put&flag"), input, "/base/");

        assertEquals("/base/my%20bucket/a/b%20c", request.encodedPath());
        assertEquals("x-id=Put&flag&v=3&since=2019-12-16T23%3A48%3A18Z&id=x&id=y&flag=on", request.encodedQuery());
        assertEquals(List.of("1.1"), request.headers().allValues("X-Ratio"));
        assertEquals(List.of("Mon, 16 Dec 2019 23:48:18 GMT"), request.headers().allValues("X-Modified"));
        // Smithy's epoch-seconds text has millisecond precision.
        assertEquals(List.of("1576540098.500"), request.headers().allValues("X-Epoch"));
        assertEquals(List.of("a, \"b,c\", \"d\\\"e\""), request.headers().allValues("X-Tags"));
        assertEquals(List.of("Mon, 16 Dec 2019 23:48:18 GMT, Tue, 17 Dec 2019 23:48:18 GMT"),
                     request.headers().allValues("X-Dates"));
        assertEquals(List.of("eyJhIjoxfQ=="), request.headers().allValues("X-Json"));
        assertEquals(List.of("aGVsbG8="), request.headers().allValues("X-Data"));
        // The httpHeader member x-meta-color takes precedence over the prefix-headers entry of the same name.
        assertEquals(List.of("blue"), request.headers().allValues("x-meta-color"));
        assertEquals(List.of("large"), request.headers().allValues("x-meta-size"));
    }

    @Test
    void absentOrEmptyLabelsAreRejectedWithoutTheirValue() {
        HttpBindingWriter writer = writer(INPUT, "/{bucket}/{key+}");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> write(writer, value(INPUT, "{\"key\":\"k\"}"), ""));
        assertEquals(true, e.getMessage().contains("bucket"));
        assertThrows(IllegalArgumentException.class,
                     () -> write(writer, value(INPUT, "{\"bucket\":\"\",\"key\":\"k\"}"), ""));
    }

    @Test
    void bodyMembersViewSerializesOnlyUnboundMembers() {
        SerializableStruct input = value(INPUT, """
            {"bucket": "b", "key": "k", "version": "3", "color": "blue", "name": "n", "size": 2}
            """);
        HttpBindingWriter writer = writer(INPUT, "/{bucket}/{key}");
        assertEquals("{\"name\":\"n\",\"size\":2}", utf8(JSON.serialize(writer.bodyMembers(input))));
        assertEquals("{}", utf8(JSON.serialize(writer.payloadMember(input))));
    }

    @Test
    void payloadMemberViewSerializesOnlyThePayload() {
        Schema input = Schema.structureBuilder(id("UploadInput"))
                             .putMember("name", PreludeSchemas.STRING, new HttpHeaderTrait("X-Name"))
                             .putMember("data", PreludeSchemas.STRING, new HttpPayloadTrait())
                             .build();
        HttpBindingWriter writer = writer(input, "/upload");
        SerializableStruct value = value(input, "{\"name\":\"n\",\"data\":\"text\"}");
        assertEquals("{}", utf8(JSON.serialize(writer.bodyMembers(value))));
        assertEquals("{\"data\":\"text\"}", utf8(JSON.serialize(writer.payloadMember(value))));
        assertEquals("/upload", write(writer, value, "").encodedPath());
    }

    @Test
    void numbersAndListItemsUseTheirCanonicalText() {
        assertEquals("1.1", HttpBindingWriter.formatFloat(1.1f));
        assertEquals("3", HttpBindingWriter.formatFloat(3f));
        assertEquals("NaN", HttpBindingWriter.formatFloat(Float.NaN));
        assertEquals("-Infinity", HttpBindingWriter.formatDouble(Double.NEGATIVE_INFINITY));
        assertEquals("5.1", HttpBindingWriter.formatDouble(5.1));
        assertEquals("-0.0", HttpBindingWriter.formatDouble(-0.0));
        assertEquals("-0.0", HttpBindingWriter.formatFloat(-0.0f));
        assertEquals("0", HttpBindingWriter.formatDouble(0.0));
        assertEquals("plain", HttpBindingWriter.headerListItem("plain"));
        assertEquals("\"a,b\"", HttpBindingWriter.headerListItem("a,b"));
        assertEquals("\"q\\\"\"", HttpBindingWriter.headerListItem("q\""));
    }

    private static String utf8(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
    }
}
