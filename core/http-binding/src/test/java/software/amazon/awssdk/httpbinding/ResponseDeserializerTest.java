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

package software.amazon.awssdk.httpbinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.utf8;
import static software.amazon.awssdk.httpbinding.TestModel.ALL_OUTPUT;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;

class ResponseDeserializerTest {
    private static final Codec CODEC = RequestSerializerTest.CODEC;

    @Test
    void readsEveryBindingLocation() {
        SdkHttpFullResponse response =
            response(201, "{\"name\":\"n\",\"size\":3,\"token\":\"from-body\"}")
                .putHeader("x-token", "tok")
                .putHeader("X-WHEN", "Tue, 29 Apr 2014 18:30:38 GMT")
                .putHeader("X-Items", "a, \"b,c\", \"\\\"q\\\"\"")
                .putHeader("X-Numbers", Arrays.asList("1, 2", "3"))
                .putHeader("X-Dates", "Mon, 16 Dec 2019 23:48:18 GMT, Tue, 17 Dec 2019 00:00:00 GMT")
                .putHeader("X-Iso-Dates", "2019-12-16T23:48:18Z, 2019-12-17T00:00:00Z")
                .putHeader("X-Flag", "true")
                .putHeader("X-Num", "7")
                .putHeader("X-Epoch", "1576540098.5")
                .putHeader("x-meta-Foo", "f")
                .putHeader("X-Meta-bar", Arrays.asList("b1", "b2"))
                .putHeader("Content-Type", "application/json")
                .build();

        SchemaStruct output = deserialize(ALL_OUTPUT, response, "application/json");

        assertThat(output.get("token")).isEqualTo("tok");
        assertThat(output.get("when")).isEqualTo(Instant.parse("2014-04-29T18:30:38Z"));
        assertThat(output.get("items")).isEqualTo(Arrays.asList("a", "b,c", "\"q\""));
        assertThat(output.get("numbers")).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(output.get("dates")).isEqualTo(Arrays.asList(Instant.parse("2019-12-16T23:48:18Z"),
                                                               Instant.parse("2019-12-17T00:00:00Z")));
        assertThat(output.get("isoDates")).isEqualTo(Arrays.asList(Instant.parse("2019-12-16T23:48:18Z"),
                                                                  Instant.parse("2019-12-17T00:00:00Z")));
        assertThat(output.get("flag")).isEqualTo(true);
        assertThat(output.get("num")).isEqualTo(7);
        assertThat(output.get("epoch")).isEqualTo(Instant.parse("2019-12-16T23:48:18.500Z"));
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Foo", "f");
        meta.put("bar", "b1");
        assertThat(output.get("meta")).isEqualTo(meta);
        assertThat(output.get("status")).isEqualTo(201);
        assertThat(output.get("name")).isEqualTo("n");
        assertThat(output.get("size")).isEqualTo(3L);
    }

    @Test
    void absentHeadersLeaveMembersUnset() {
        SchemaStruct output = deserialize(ALL_OUTPUT, response(200, "{}").build(), null);
        assertThat(output.get("token")).isNull();
        assertThat(output.get("items")).isNull();
        assertThat(output.get("num")).isNull();
        assertThat(output.get("meta")).isEqualTo(Collections.emptyMap());
        assertThat(output.get("status")).isEqualTo(200);
    }

    @ParameterizedTest
    @CsvSource({
        "X-Num, abc, integer, num",
        "X-Num, 99999999999, integer, num",
        "X-Flag, yes, boolean, flag",
        "X-When, 2014-04-29T18:30:38Z, http-date timestamp, when",
        "X-Epoch, soon, epoch-seconds timestamp, epoch",
        "X-Numbers, '1, x', integer, numbers",
        "X-Dates, 'Mon, 16 Dec 2019', http-date timestamp, dates"
    })
    void invalidHeaderValueFailsNamingTheMember(String header, String value, String type, String member) {
        SdkHttpFullResponse response = response(200, "{}").putHeader(header, value).build();
        assertThatThrownBy(() -> deserialize(ALL_OUTPUT, response, null))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Invalid " + type + " in HTTP header bindings for member smithy.example#AllBindingsOutput$"
                        + member);
    }

    @Test
    void codecBodyIsReadFromTheGivenBufferFirst() {
        SdkHttpFullResponse response = response(200, "{\"name\":\"from-stream\"}").build();
        SchemaStruct.Builder builder = SchemaStruct.builder(ALL_OUTPUT);
        HttpBinding.create().responseDeserializer()
                   .payloadCodec(CODEC)
                   .response(response)
                   .body(ByteBuffer.wrap(utf8("{\"name\":\"from-buffer\"}")))
                   .outputShapeBuilder(builder)
                   .deserialize();
        assertThat(builder.build().get("name")).isEqualTo("from-buffer");
    }

    @Test
    void unexpectedContentTypeIsRejectedAndMissingContentTypeAccepted() {
        SdkHttpFullResponse wrong = response(200, "{}").putHeader("Content-Type", "text/html").build();
        assertThatThrownBy(() -> deserialize(ALL_OUTPUT, wrong, "application/json"))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("text/html");
        assertThat(deserialize(ALL_OUTPUT, response(200, "{\"name\":\"n\"}").build(), "application/json").get("name"))
            .isEqualTo("n");
    }

    @Test
    void malformedBodyAndTrailingContentAreRejected() {
        assertThatThrownBy(() -> deserialize(ALL_OUTPUT, response(200, "{\"name\":").build(), null))
            .isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> deserialize(ALL_OUTPUT, response(200, "{} {}").build(), null))
            .isInstanceOf(SerializationException.class);
    }

    @Test
    void emptyBodyGivesNoBodyMembers() {
        SchemaStruct output = deserialize(ALL_OUTPUT, SdkHttpFullResponse.builder().statusCode(204).build(), null);
        assertThat(output.get("name")).isNull();
        assertThat(output.get("status")).isEqualTo(204);
    }

    @Test
    void xmlCodecReadsOnlyBodyMembers() {
        Codec xml = XmlCodec.builder().build();
        SdkHttpFullResponse response =
            response(200, "<AllBindingsOutput><name>n</name><size>3</size><token>from-body</token></AllBindingsOutput>")
                .putHeader("X-Token", "tok")
                .build();
        SchemaStruct.Builder builder = SchemaStruct.builder(ALL_OUTPUT);
        HttpBinding.create().responseDeserializer().payloadCodec(xml).response(response).outputShapeBuilder(builder)
                   .deserialize();
        SchemaStruct output = builder.build();
        assertThat(output.get("name")).isEqualTo("n");
        assertThat(output.get("size")).isEqualTo(3L);
        assertThat(output.get("token")).isEqualTo("tok");
    }

    // Ported from smithy-java PayloadDeserializerTest#stringPayloadIsReadWhenResponseIsReadonlyBuffer.
    @Test
    void stringPayloadIsReadWhenResponseIsReadonlyBuffer() {
        SchemaStruct.Builder builder = SchemaStruct.builder(TestModel.STRING_PAYLOAD);
        HttpBinding.create().responseDeserializer()
                   .payloadCodec(CODEC)
                   .payloadMediaType("application/json")
                   .response(SdkHttpFullResponse.builder().statusCode(200).build())
                   .body(ByteBuffer.wrap(utf8("string value")).asReadOnlyBuffer())
                   .outputShapeBuilder(builder)
                   .deserialize();
        assertThat(builder.build().get("value")).isEqualTo("string value");
    }

    // Ported from smithy-java HttpBindingDeserializerTest (string and blob payloads from a non-replayable stream).
    @Test
    void stringAndBlobPayloadsFromAStreamAreRead() {
        SchemaStruct string = deserialize(TestModel.STRING_PAYLOAD, response(200, "string value").build(), null);
        assertThat(string.get("value")).isEqualTo("string value");
        SchemaStruct blob = deserialize(TestModel.BLOB_PAYLOAD,
                                        response(200, "blob value").putHeader("X-Token", "t").build(), null);
        assertThat(StandardCharsets.UTF_8.decode((ByteBuffer) blob.get("data")).toString()).isEqualTo("blob value");
        assertThat(blob.get("token")).isEqualTo("t");
    }

    @Test
    void structPayloadIsReadThroughTheCodec() {
        SdkHttpFullResponse response = response(200, "{\"name\":\"Phreddy\"}")
            .putHeader("Content-Type", "application/json").build();
        SchemaStruct output = deserialize(TestModel.STRUCT_PAYLOAD, response, "application/json");
        assertThat(((SchemaStruct) output.get("greeting")).get("name")).isEqualTo("Phreddy");
    }

    @Test
    void emptyBodyLeavesThePayloadUnset() {
        for (Schema schema : Arrays.asList(TestModel.STRING_PAYLOAD, TestModel.BLOB_PAYLOAD, TestModel.STRUCT_PAYLOAD,
                                           TestModel.LIST_PAYLOAD)) {
            SchemaStruct output = deserialize(schema, response(200, "").build(), null);
            assertThat(output).as(schema.id().toString()).isEqualTo(SchemaStruct.builder(schema).build());
        }
    }

    // Adapted from smithy-java HttpBindingDeserializerTest#responseDeserializerLeavesStreamingPayloadOpen.
    @Test
    void streamingPayloadLeavesTheBodyUnread() {
        InputStream unreadable = new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("the body must not be read");
            }
        };
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .putHeader("X-Token", "t")
                                                          .content(AbortableInputStream.create(unreadable))
                                                          .build();
        SchemaStruct output = deserialize(TestModel.STREAMING_PAYLOAD, response, null);
        assertThat(output.get("token")).isEqualTo("t");
        assertThat(output.get("body")).isNull();
    }

    @Test
    void errorBuilderIsPopulated() {
        SdkHttpFullResponse response = response(404, "{\"message\":\"gone\"}").putHeader("X-Resource", "r").build();
        SchemaStruct.Builder builder = SchemaStruct.builder(TestModel.NOT_FOUND);
        HttpBinding.create().responseDeserializer()
                   .payloadCodec(CODEC)
                   .response(response)
                   .outputShapeBuilder(SchemaStruct.builder(ALL_OUTPUT))
                   .errorShapeBuilder(builder)
                   .deserialize();
        assertThat(builder.build().get("message")).isEqualTo("gone");
        assertThat(builder.build().get("resource")).isEqualTo("r");
    }

    @Test
    void requiresABuilder() {
        assertThatThrownBy(() -> HttpBinding.create().responseDeserializer().payloadCodec(CODEC).deserialize())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("outputShapeBuilder");
    }

    static SdkHttpFullResponse.Builder response(int status, String body) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(status)
                                  .content(AbortableInputStream.create(new ByteArrayInputStream(utf8(body))));
    }

    static SchemaStruct deserialize(Schema schema, SdkHttpFullResponse response, String mediaType) {
        SchemaStruct.Builder builder = SchemaStruct.builder(schema);
        HttpBinding.create().responseDeserializer()
                   .payloadCodec(CODEC)
                   .payloadMediaType(mediaType)
                   .response(response)
                   .outputShapeBuilder(builder)
                   .deserialize();
        return builder.build();
    }
}
