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

package software.amazon.awssdk.protocols.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.http.TestShapes.CODEC;
import static software.amazon.awssdk.protocols.client.http.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.http.TestShapes.NS;
import static software.amazon.awssdk.protocols.client.http.TestShapes.SERVICE;
import static software.amazon.awssdk.protocols.client.http.TestShapes.SERVICE_ID;
import static software.amazon.awssdk.protocols.client.http.TestShapes.cbor;
import static software.amazon.awssdk.protocols.client.http.TestShapes.response;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.httpbinding.HttpBinding;
import software.amazon.awssdk.protocols.client.http.TestShapes.FooError;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;
import software.amazon.awssdk.schema.traits.HttpPayloadTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;

/**
 * Tests the {@link HttpBindingClientProtocol} defaults and {@link HttpBindingErrorFactory} with the CBOR codec. The
 * restJson1 protocol tests cover the overridden hooks.
 */
class HttpBindingClientProtocolTest {
    private static final ShapeId PROTOCOL_ID = ShapeId.fromParts("smithy.example", "testHttpBinding");
    private static final URI ENDPOINT = URI.create("https://example.com");

    /**
     * {@code POST /items/{key+}}: a greedy label, a header and a body member.
     */
    private static final Schema ITEM = Schema.structureBuilder(ShapeId.fromParts(NS, "Item"))
                                             .putMember("key", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
                                             .putMember("tag", PreludeSchemas.STRING, new HttpHeaderTrait("X-Tag"))
                                             .putMember("message", PreludeSchemas.STRING)
                                             .builderSupplier(() -> new Values(HttpBindingClientProtocolTest.ITEM))
                                             .build();

    private static final Schema HEADER_ONLY = Schema.structureBuilder(ShapeId.fromParts(NS, "HeaderOnly"))
                                                    .putMember("tag", PreludeSchemas.STRING,
                                                               new HttpHeaderTrait("X-Tag"))
                                                    .builderSupplier(() -> new Values(
                                                        HttpBindingClientProtocolTest.HEADER_ONLY))
                                                    .build();

    private static final ApiOperation<SerializableStruct, SerializableStruct> PUT_ITEM =
        operation("PutItem", new HttpTrait("POST", "/items/{key+}"), ITEM);

    private static final ApiOperation<SerializableStruct, SerializableStruct> TAG =
        operation("Tag", new HttpTrait("POST", "/tag"), HEADER_ONLY);

    /**
     * {@code POST /chat}: a header and an event stream {@code @httpPayload}, in both directions.
     */
    private static final Schema CHAT_SHAPE =
        Schema.structureBuilder(ShapeId.fromParts(NS, "Chat"))
              .putMember("tag", PreludeSchemas.STRING, new HttpHeaderTrait("X-Tag"))
              .putMember("stream",
                         Schema.unionBuilder(ShapeId.fromParts(NS, "ChatStream"), StreamingTrait.INSTANCE)
                               .putMember("message", HEADER_ONLY)
                               .build(),
                         HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> new Values(HttpBindingClientProtocolTest.CHAT_SHAPE))
              .build();

    private static final ApiOperation<SerializableStruct, SerializableStruct> CHAT =
        operation("Chat", new HttpTrait("POST", "/chat"), CHAT_SHAPE);

    private static final TestProtocol PROTOCOL = new TestProtocol();

    @Test
    void serializeRequest_withDefaults_keepsGreedySlashAndBindsMembers() {
        SdkHttpFullRequest request = PROTOCOL.serializeRequest(PUT_ITEM, new Values(ITEM).set("key", "/a/b")
                                                                                         .set("tag", "t")
                                                                                         .set("message", "hi")
                                                                                         .build(),
                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(request.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(request.encodedPath()).isEqualTo("/items//a/b");
        assertThat(request.firstMatchingHeader("X-Tag")).hasValue("t");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/cbor");
        ByteBuffer body = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(body.remaining()));
        assertThat(decode(ITEM, body).values).containsExactly(entry("message", "hi"));
    }

    @Test
    void serializeRequest_withDefaults_sendsTheEmptyStructureForAnInputWithoutBodyMembers() {
        SdkHttpFullRequest request = PROTOCOL.serializeRequest(TAG, new Values(HEADER_ONLY).set("tag", "t").build(),
                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/cbor");
        ByteBuffer body = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(body.remaining()).isPositive();
        assertThat(decode(HEADER_ONLY, body).values).isEmpty();
    }

    @Test
    void deserializeResponse_readsHeaderAndBodyMembers() {
        SdkHttpFullResponse response = response(200, cbor("message", "hi")).putHeader("X-Tag", "t").build();

        Values output = (Values) PROTOCOL.deserializeResponse(PUT_ITEM, new ExecutionAttributes(), ERRORS,
                                                              request(), response);

        assertThat(output.values).containsEntry("tag", "t").containsEntry("message", "hi");
    }

    @Test
    void deserializeResponse_withErrorStatus_usesTheErrorDeserializer() {
        SdkHttpFullResponse response = response(400, cbor("__type", "FooError", "message", "m")).build();

        assertThatThrownBy(() -> PROTOCOL.deserializeResponse(PUT_ITEM, new ExecutionAttributes(), ERRORS, request(),
                                                              response))
            .isInstanceOf(FooError.class)
            .satisfies(e -> assertThat(((SdkServiceException) e).statusCode()).isEqualTo(400));
    }

    @Test
    void serializeRequest_withEventStreamInput_bindsTheOtherMembersAndLeavesTheBodyToTheEvents() {
        SdkHttpFullRequest request = PROTOCOL.serializeRequest(CHAT, new Values(CHAT_SHAPE).set("tag", "t").build(),
                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(request.encodedPath()).isEqualTo("/chat");
        assertThat(request.firstMatchingHeader("X-Tag")).hasValue("t");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/vnd.amazon.eventstream");
        assertThat(request.firstMatchingHeader("Transfer-Encoding")).hasValue("chunked");
        assertThat(request.firstMatchingHeader("Content-Length")).isEmpty();
        assertThat(request.contentStreamProvider()).isEmpty();
        assertThat(PROTOCOL.sendsInitialEventMessages()).isFalse();
        assertThat(PROTOCOL.inputEventEncoderFactory(CHAT).contentType())
            .isEqualTo("application/vnd.amazon.eventstream");
    }

    @Test
    void deserializeResponse_withEventStreamOutput_readsHeadersAndLeavesTheBodyToTheEvents() {
        SdkHttpFullResponse response = response(200, new byte[] {0, 0, 0, 16}).putHeader("X-Tag", "t").build();

        Values output = (Values) PROTOCOL.deserializeResponse(CHAT, new ExecutionAttributes(), ERRORS, request(),
                                                              response);

        assertThat(output.values).containsExactly(entry("tag", "t"));
        assertThat(PROTOCOL.outputEventDecoderFactory(CHAT)).isNotNull();
    }

    @Test
    void httpBindingErrorFactory_populatesHeaderAndBodyMembers() {
        Values builder = new Values(ITEM);
        SdkHttpFullResponse response = response(400, null).putHeader("X-Tag", "t").build();

        new HttpBindingErrorFactory(HttpBinding.create()).populateError(new ExecutionAttributes(), CODEC, response,
                                                                       ByteBuffer.wrap(cbor("message", "hi")),
                                                                       builder);

        assertThat(builder.values).containsEntry("tag", "t").containsEntry("message", "hi");
        assertThat(new HttpBindingErrorFactory()).hasToString("HttpBindingErrorFactory");
        assertThatThrownBy(() -> new HttpBindingErrorFactory(null)).isInstanceOf(NullPointerException.class);
    }

    private static ApiOperation<SerializableStruct, SerializableStruct> operation(String name, HttpTrait http,
                                                                                  Schema schema) {
        return SimpleApiOperation.<SerializableStruct, SerializableStruct>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts(NS, name), http))
                                 .inputSchema(() -> schema)
                                 .outputSchema(() -> schema)
                                 .errorRegistry(ERRORS)
                                 .service(SERVICE)
                                 .build();
    }

    private static Values decode(Schema schema, ByteBuffer body) {
        return new Values(schema).deserialize(CODEC.createDeserializer(body.duplicate()));
    }

    private static Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    private static SdkHttpFullRequest request() {
        return SdkHttpFullRequest.builder().method(SdkHttpMethod.POST).uri(ENDPOINT).build();
    }

    /**
     * A protocol with every hook at its default.
     */
    private static final class TestProtocol extends HttpBindingClientProtocol {
        private final HttpErrorDeserializer errors = HttpErrorDeserializer.builder()
                                                                          .codec(CODEC)
                                                                          .serviceId(SERVICE_ID)
                                                                          .baseExceptionBuilder(
                                                                              TestShapes.ExampleServiceException::builder)
                                                                          .errorPayloadParser(
                                                                              HttpErrorDeserializer
                                                                                  .TYPE_ONLY_ERROR_PAYLOAD_PARSER)
                                                                          .build();

        private TestProtocol() {
            super(PROTOCOL_ID);
        }

        @Override
        public Codec payloadCodec() {
            return CODEC;
        }

        @Override
        protected String payloadMediaType() {
            return "application/cbor";
        }

        @Override
        protected HttpErrorDeserializer errorDeserializer() {
            return errors;
        }
    }

    /**
     * A structure of string members, and its own builder.
     */
    private static final class Values implements SerializableStruct, ShapeBuilder<SerializableStruct> {
        private final Schema schema;
        private final Map<String, String> values = new LinkedHashMap<>();

        private Values(Schema schema) {
            this.schema = schema;
        }

        private Values set(String member, String value) {
            values.put(member, value);
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            for (Schema member : schema.members()) {
                String value = values.get(member.memberName());
                if (value != null) {
                    serializer.writeString(member, value);
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
        }

        @Override
        public Values deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, (builder, member, d) -> builder.set(member.memberName(),
                                                                                 d.readString(member)));
            return this;
        }

        @Override
        public Values build() {
            return this;
        }
    }
}
