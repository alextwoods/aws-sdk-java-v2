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
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.body;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.serialize;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.utf8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Ported from smithy-java {@code PayloadSerializerTest}, plus the {@code Content-Type} and streaming cases.
 */
class PayloadSerializerTest {
    @Test
    void blobPayloadIsWrittenVerbatim() throws IOException {
        SdkHttpFullRequest request = serialize(TestModel.BLOB_OPERATION,
                                               SchemaStruct.builder(TestModel.BLOB_PAYLOAD)
                                                           .set("data", ByteBuffer.wrap(utf8("hello")))
                                                           .set("token", "t")
                                                           .build());
        assertThat(body(request)).isEqualTo("hello");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/octet-stream");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("5");
        assertThat(request.firstMatchingHeader("X-Token")).hasValue("t");
    }

    @Test
    void blobPayloadFromAReadOnlyBufferIsCopied() throws IOException {
        ByteBuffer data = ByteBuffer.wrap(utf8("xhellox"), 1, 5).slice().asReadOnlyBuffer();
        SdkHttpFullRequest request = serialize(TestModel.BLOB_OPERATION,
                                               SchemaStruct.builder(TestModel.BLOB_PAYLOAD).set("data", data).build());
        assertThat(body(request)).isEqualTo("hello");
        assertThat(data.remaining()).isEqualTo(5);
    }

    @Test
    void nullBlobPayloadWritesNoBody() {
        SdkHttpFullRequest request = serialize(TestModel.BLOB_OPERATION,
                                               SchemaStruct.builder(TestModel.BLOB_PAYLOAD).build());
        assertThat(request.contentStreamProvider()).isEmpty();
        assertThat(request.headers()).isEmpty();
    }

    @Test
    void stringPayloadIsWrittenAsRawBytes() throws IOException {
        SdkHttpFullRequest request = serialize(TestModel.STRING_OPERATION,
                                               SchemaStruct.builder(TestModel.STRING_PAYLOAD)
                                                           .set("value", "hello ü")
                                                           .build());
        assertThat(body(request)).isEqualTo("hello ü");
        // @mediaType on the target gives the Content-Type.
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("text/markdown");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("8");
    }

    @Test
    void listPayloadIsSerializedThroughCodec() throws IOException {
        SdkHttpFullRequest request = serialize(TestModel.LIST_OPERATION,
                                               SchemaStruct.builder(TestModel.LIST_PAYLOAD)
                                                           .set("items", Arrays.asList("a", "b", "c"))
                                                           .build());
        assertThat(body(request)).isEqualTo("[\"a\",\"b\",\"c\"]");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");
    }

    @Test
    void structPayloadIsSerializedThroughCodec() throws IOException {
        SdkHttpFullRequest request = serialize(TestModel.STRUCT_OPERATION,
                                               SchemaStruct.builder(TestModel.STRUCT_PAYLOAD)
                                                           .set("greeting", SchemaStruct.builder(TestModel.GREETING)
                                                                                        .set("name", "Phreddy")
                                                                                        .build())
                                                           .set("token", "t")
                                                           .build());
        assertThat(body(request)).isEqualTo("{\"name\":\"Phreddy\"}");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");
        assertThat(request.firstMatchingHeader("X-Token")).hasValue("t");
    }

    @Test
    void nullStructPayloadWritesNoBodyUnlessAnEmptyStructureIsAllowed() throws IOException {
        SchemaStruct input = SchemaStruct.builder(TestModel.STRUCT_PAYLOAD).build();
        assertThat(serialize(TestModel.STRUCT_OPERATION, input).contentStreamProvider()).isEmpty();

        SdkHttpFullRequest request = HttpBinding.create().requestSerializer()
                                                .operation(TestModel.STRUCT_OPERATION)
                                                .payloadCodec(RequestSerializerTest.CODEC)
                                                .payloadMediaType("application/json")
                                                .endpoint(RequestSerializerTest.ENDPOINT)
                                                .allowEmptyStructPayload(true)
                                                .shapeValue(input)
                                                .serializeRequest();
        assertThat(body(request)).isEqualTo("{}");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");
    }

    @Test
    void streamingPayloadIsLeftToTheProtocol() {
        SdkHttpFullRequest request = serialize(TestModel.STREAMING_OPERATION,
                                               SchemaStruct.builder(TestModel.STREAMING_PAYLOAD)
                                                           .set("token", "t")
                                                           .build());
        assertThat(request.contentStreamProvider()).isEmpty();
        assertThat(request.headers()).containsOnlyKeys("X-Token");
        assertThat(HttpBinding.create().hasStreamingPayload(TestModel.STREAMING_PAYLOAD, false)).isTrue();
        assertThat(HttpBinding.create().hasStreamingPayload(TestModel.STREAMING_PAYLOAD, true)).isTrue();
        assertThat(HttpBinding.create().hasStreamingPayload(TestModel.BLOB_PAYLOAD, false)).isFalse();
        assertThat(HttpBinding.create().hasStructPayload(TestModel.STRUCT_PAYLOAD)).isTrue();
        assertThat(HttpBinding.create().hasStructPayload(TestModel.BLOB_PAYLOAD)).isFalse();
    }
}
