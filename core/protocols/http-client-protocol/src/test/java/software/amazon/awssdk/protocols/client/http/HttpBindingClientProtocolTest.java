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
import java.nio.charset.StandardCharsets;
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

    /**
     * {@code PUT /blob}: a header and a string {@code @httpPayload}, sent as the raw body as a blob payload is.
     */
    private static final Schema BLOB_SHAPE = Schema.structureBuilder(ShapeId.fromParts(NS, "BlobShape"))
                                                   .putMember("tag", PreludeSchemas.STRING,
                                                              new HttpHeaderTrait("X-Tag"))
                                                   .putMember("data", PreludeSchemas.STRING,
                                                              HttpPayloadTrait.INSTANCE)
                                                   .builderSupplier(() -> new Values(
                                                       HttpBindingClientProtocolTest.BLOB_SHAPE))
                                                   .build();

    private static final ApiOperation<SerializableStruct, SerializableStruct> PUT_BLOB =
        operation("PutBlob", new HttpTrait("PUT", "/blob"), BLOB_SHAPE);

    private static final Schema BODY_ONLY = Schema.structureBuilder(ShapeId.fromParts(NS, "BodyOnly"))
                                                  .putMember("message", PreludeSchemas.STRING)
                                                  .builderSupplier(() -> new Values(
                                                      HttpBindingClientProtocolTest.BODY_ONLY))
                                                  .build();

    /**
     * {@code PUT /struct}: a header and a structure {@code @httpPayload}.
     */
    private static final Schema STRUCT_PAYLOAD_SHAPE =
        Schema.structureBuilder(ShapeId.fromParts(NS, "StructPayloadShape"))
              .putMember("tag", PreludeSchemas.STRING, new HttpHeaderTrait("X-Tag"))
              .putMember("greeting", BODY_ONLY, HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> new Values(HttpBindingClientProtocolTest.STRUCT_PAYLOAD_SHAPE))
              .build();

    private static final ApiOperation<SerializableStruct, SerializableStruct> PUT_STRUCT_PAYLOAD =
        operation("PutStructPayload", new HttpTrait("PUT", "/struct"), STRUCT_PAYLOAD_SHAPE);

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
    void serializeRequest_withoutACodecBody_skipsTheBodyBufferAndSendsTheSameRequest() {
        TestProtocol protocol = new TestProtocol();
        SerializableStruct headerOnly = new Values(HEADER_ONLY).set("tag", "t").build();
        SerializableStruct blob = new Values(BLOB_SHAPE).set("tag", "t").set("data", "payload").build();

        // The first request records that no codec body was written; the next ones get no buffer and are the same.
        assertSameRequests(protocol, TAG, headerOnly);
        assertThat(protocol.lastRequestBodyWasEmpty(TAG)).isTrue();
        assertSameRequests(protocol, PUT_BLOB, blob);
        assertThat(protocol.lastRequestBodyWasEmpty(PUT_BLOB)).isTrue();
        assertThat(body(protocol.serializeRequest(PUT_BLOB, blob, new ExecutionAttributes(), ENDPOINT)))
            .isEqualTo("payload");

        SerializableStruct item = new Values(ITEM).set("key", "k").set("message", "hi").build();
        assertSameRequests(protocol, PUT_ITEM, item);
        assertThat(protocol.lastRequestBodyWasEmpty(PUT_ITEM)).isFalse();
    }

    @Test
    void serializeRequest_whenAnOperationStartsWritingACodecBody_usesTheSerializersOwnBuffer() {
        TestProtocol protocol = new TestProtocol();
        protocol.omitEmptyPayload = true;
        SdkHttpFullRequest none = protocol.serializeRequest(TAG, new Values(HEADER_ONLY).set("tag", "t").build(),
                                                            new ExecutionAttributes(), ENDPOINT);
        assertThat(none.contentStreamProvider()).isEmpty();
        assertThat(protocol.lastRequestBodyWasEmpty(TAG)).isTrue();

        // An input whose schema has a body member, sent through the same operation instance.
        SdkHttpFullRequest withBody = protocol.serializeRequest(TAG, new Values(BODY_ONLY).set("message", "hi").build(),
                                                                new ExecutionAttributes(), ENDPOINT);
        ByteBuffer body = withBody.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(withBody.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(body.remaining()));
        assertThat(decode(BODY_ONLY, body).values).containsExactly(entry("message", "hi"));
    }

    @Test
    void serializeRequest_whenAllowEmptyStructPayloadFlips_sendsTheEmptyStructure() {
        TestProtocol protocol = new TestProtocol();
        SerializableStruct input = new Values(STRUCT_PAYLOAD_SHAPE).set("tag", "t").build();
        SdkHttpFullRequest first = protocol.serializeRequest(PUT_STRUCT_PAYLOAD, input, new ExecutionAttributes(),
                                                             ENDPOINT);
        assertThat(first.contentStreamProvider()).isEmpty();

        protocol.allowEmptyStructPayload = true;
        SdkHttpFullRequest second = protocol.serializeRequest(PUT_STRUCT_PAYLOAD, input, new ExecutionAttributes(),
                                                              ENDPOINT);
        ByteBuffer body = second.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(body.remaining()).isPositive();
        assertThat(second.firstMatchingHeader("Content-Type")).hasValue("application/cbor");
        assertThat(second.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(body.remaining()));
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

    /**
     * Serializes the input three times and checks the requests match: method, path, headers, query and body.
     */
    private static void assertSameRequests(TestProtocol protocol, ApiOperation<SerializableStruct, SerializableStruct> op,
                                           SerializableStruct input) {
        SdkHttpFullRequest first = protocol.serializeRequest(op, input, new ExecutionAttributes(), ENDPOINT);
        for (int i = 2; i <= 3; i++) {
            SdkHttpFullRequest next = protocol.serializeRequest(op, input, new ExecutionAttributes(), ENDPOINT);
            assertThat(next.method()).as("call %s", i).isEqualTo(first.method());
            assertThat(next.encodedPath()).as("call %s", i).isEqualTo(first.encodedPath());
            assertThat(next.headers()).as("call %s", i).isEqualTo(first.headers());
            assertThat(next.rawQueryParameters()).as("call %s", i).isEqualTo(first.rawQueryParameters());
            assertThat(body(next)).as("call %s", i).isEqualTo(body(first));
        }
    }

    private static String body(SdkHttpFullRequest request) {
        if (!request.contentStreamProvider().isPresent()) {
            return null;
        }
        ByteBuffer body = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        return StandardCharsets.ISO_8859_1.decode(body.duplicate()).toString();
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
        private boolean omitEmptyPayload;
        private boolean allowEmptyStructPayload;
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

        @Override
        protected boolean omitEmptyPayload() {
            return omitEmptyPayload;
        }

        @Override
        protected boolean allowEmptyStructPayload(Schema inputSchema) {
            return allowEmptyStructPayload;
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
