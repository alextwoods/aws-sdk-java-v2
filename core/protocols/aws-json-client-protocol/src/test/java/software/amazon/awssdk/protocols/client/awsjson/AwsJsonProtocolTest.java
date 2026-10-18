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

package software.amazon.awssdk.protocols.client.awsjson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.NO_INPUT;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.NO_OUTPUT;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.QUERY_COMPATIBLE_SETTINGS;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.SETTINGS;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.json;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.response;
import static software.amazon.awssdk.protocols.client.awsjson.TestShapes.utf8;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactories;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.FooError;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.GreetRequest;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.GreetResponse;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.GreetResponse.GreetResponseBuilder;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.NoInputRequest;
import software.amazon.awssdk.protocols.client.awsjson.TestShapes.QueueMissing;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.AwsJson1_0Trait;
import software.amazon.awssdk.schema.traits.AwsJson1_1Trait;
import software.amazon.awssdk.schema.traits.StreamingTrait;

/**
 * Tests both AWS JSON protocols. Every case runs against {@link AwsJson1Protocol} and {@link AwsJson11Protocol}, which
 * differ only in their protocol ID and content type.
 */
class AwsJsonProtocolTest {
    private static final URI ENDPOINT = URI.create("https://example.com");
    private static final List<Variant> VARIANTS = Arrays.asList(
        new Variant(AwsJson1_0Trait.ID, "application/x-amz-json-1.0", "AwsJson1Protocol", AwsJson1Protocol::new),
        new Variant(AwsJson1_1Trait.ID, "application/x-amz-json-1.1", "AwsJson11Protocol", AwsJson11Protocol::new));

    static Stream<Variant> variants() {
        return VARIANTS.stream();
    }

    @ParameterizedTest
    @MethodSource("variants")
    void identityCodecAndMessageExchange(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);

        assertThat(protocol.id()).isEqualTo(variant.id);
        assertThat(protocol.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThat(protocol.payloadCodec()).isInstanceOf(JsonCodec.class).isSameAs(protocol.payloadCodec());
        JsonSettings settings = ((JsonCodec) protocol.payloadCodec()).settings();
        assertThat(settings.useJsonName()).isFalse();
        assertThat(settings.useTimestampFormat()).isTrue();
        assertThat(settings.defaultNamespace()).isEqualTo(TestShapes.NS);
        assertThat(settings.toBuilder().defaultNamespace(null).build()).isEqualTo(JsonSettings.AWS_JSON);
        assertThat(protocol).hasToString(variant.name + "(smithy.example#ExampleService)");
        assertThatThrownBy(() -> variant.create(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("variants")
    void requestIsAPostToTheRootWithTargetContentTypeAndJsonBody(Variant variant) {
        SdkHttpFullRequest request = serialize(variant.create(SETTINGS), new GreetRequest("hi", null));

        assertThat(request.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(request.getUri()).isEqualTo(URI.create("https://example.com/"));
        assertThat(request.headers()).containsOnlyKeys("X-Amz-Target", "Content-Type", "Content-Length");
        assertThat(request.firstMatchingHeader("X-Amz-Target")).hasValue("ExampleService.Greet");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue(variant.contentType);

        ByteBuffer body = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(body).as("zero-copy body").isNotNull();
        assertThat(utf8(body)).isEqualTo("{\"message\":\"hi\"}");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(body.remaining()));
    }

    /**
     * Smithy binds a streaming blob only through {@code @httpPayload}, which the AWS JSON protocols do not have (task
     * 7.19); codegen keeps such operations on the legacy path.
     */
    @ParameterizedTest
    @MethodSource("variants")
    void streamingBlobMembersAreRejected(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);
        Schema streaming = Schema.structureBuilder(ShapeId.fromParts(TestShapes.NS, "Upload"))
                                 .putMember("body", Schema.createBlob(ShapeId.fromParts(TestShapes.NS, "Body"),
                                                                      StreamingTrait.INSTANCE))
                                 .build();
        SimpleApiOperation<GreetRequest, GreetResponse> upload =
            SimpleApiOperation.<GreetRequest, GreetResponse>builder()
                              .schema(Schema.createOperation(ShapeId.fromParts(TestShapes.NS, "Upload")))
                              .inputSchema(() -> streaming)
                              .outputSchema(() -> streaming)
                              .errorRegistry(ERRORS)
                              .service(TestShapes.SERVICE)
                              .build();

        assertThatThrownBy(() -> protocol.serializeRequest(upload, new GreetRequest("hi", null), new ExecutionAttributes(),
                                                           ENDPOINT))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Unable to marshall Upload: Streaming blob input is not supported by " + variant.id);
        assertThatThrownBy(() -> protocol.deserializeResponse(upload, new ExecutionAttributes(), ERRORS, null,
                                                              response(200, utf8("{}")).build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Unable to unmarshall Upload: Streaming blob output is not supported by " + variant.id);
    }

    @ParameterizedTest
    @MethodSource("variants")
    void serializeRequest_withEventStreamInput_sendsTheInitialRequestMessage(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);
        EventStreamInput input = new EventStreamInput("hi");

        SdkHttpFullRequest request = protocol.serializeRequest(EventStreamInput.OPERATION, input,
                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(request.firstMatchingHeader("X-Amz-Target")).hasValue("ExampleService.Chat");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/vnd.amazon.eventstream");
        assertThat(request.firstMatchingHeader("Transfer-Encoding")).hasValue("chunked");
        assertThat(request.firstMatchingHeader("Content-Length")).isEmpty();
        assertThat(protocol.sendsInitialEventMessages()).isTrue();
        ByteBuffer expected = protocol.inputEventEncoderFactory(EventStreamInput.OPERATION).newFrameEncoder().encode(
            protocol.inputEventEncoderFactory(EventStreamInput.OPERATION).newEventEncoder().encode(input));
        ByteBuffer body = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(body).isEqualTo(expected);
        String frame = new String(body.array(), body.arrayOffset() + body.position(), body.remaining(),
                                  StandardCharsets.ISO_8859_1);
        assertThat(frame).contains("initial-request", variant.contentType, "{\"greeting\":\"hi\"}");
    }

    @ParameterizedTest
    @MethodSource("variants")
    void deserializeResponse_withEventStreamOutput_leavesTheBodyForTheEventDecoder(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);
        SdkHttpFullResponse response = response(200, utf8("not JSON: event stream frames")).build();

        GreetResponse output = protocol.deserializeResponse(EventStreamInput.OPERATION, new ExecutionAttributes(),
                                                            ERRORS, null, response);

        assertThat(output.greeting()).isNull();
        assertThat(output.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(protocol.outputEventDecoderFactory(EventStreamInput.OPERATION)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8080/prefix", "http://localhost:8080/prefix/"})
    void requestPathIsAppendedToTheEndpointPath(String endpoint) {
        SdkHttpFullRequest request = new AwsJson1Protocol(SETTINGS).serializeRequest(
            GREET, new GreetRequest("hi", null), new ExecutionAttributes(), URI.create(endpoint));

        assertThat(request.getUri()).isEqualTo(URI.create("http://localhost:8080/prefix/"));
    }

    @ParameterizedTest
    @MethodSource("variants")
    void httpBindingTraitsAndJsonNameAreIgnored(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);
        SdkHttpFullRequest request = serialize(protocol, new GreetRequest("hi", "Ada"));

        assertThat(request.headers()).doesNotContainKey("X-Name");
        assertThat(request.rawQueryParameters()).isEmpty();
        assertThat(request.encodedPath()).isEqualTo("/");
        assertThat(utf8(request.contentStreamProvider().get().contentAsByteBufferOrNull()))
            .isEqualTo("{\"message\":\"hi\",\"name\":\"Ada\"}");

        GreetResponse output = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, request,
                                                            response(200, utf8("{\"count\":\"3\",\"Alias\":\"x\"}"))
                                                                .putHeader("X-Count", "7").build());
        assertThat(output.count()).isEqualTo("3");
    }

    @ParameterizedTest
    @MethodSource("variants")
    void inputWithoutMembersSendsAnEmptyObject(Variant variant) {
        SdkHttpFullRequest request = variant.create(SETTINGS).serializeRequest(NO_INPUT, new NoInputRequest(),
                                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(request.encodedPath()).isEqualTo("/");
        assertThat(request.firstMatchingHeader("X-Amz-Target")).hasValue("ExampleService.NoInput");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue(variant.contentType);
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("2");
        assertThat(utf8(request.contentStreamProvider().get().contentAsByteBufferOrNull())).isEqualTo("{}");
    }

    @ParameterizedTest
    @MethodSource("variants")
    void awsQueryCompatibleServicesSendTheQueryModeHeader(Variant variant) {
        AwsJsonProtocol queryCompatible = variant.create(QUERY_COMPATIBLE_SETTINGS);

        assertThat(serialize(queryCompatible, new GreetRequest("hi", null)).firstMatchingHeader("x-amzn-query-mode"))
            .hasValue("true");
        assertThat(serialize(variant.create(SETTINGS), new GreetRequest("hi", null))
                       .firstMatchingHeader("x-amzn-query-mode"))
            .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("variants")
    void outputIsDeserializedWithTheHttpResponseAndMetadata(Variant variant) {
        SdkHttpFullResponse response = response(200, json("greeting", "hello")).putHeader("x-amzn-RequestId", "req-1")
                                                                               .build();

        GreetResponse output = variant.create(SETTINGS).deserializeResponse(GREET, new ExecutionAttributes(), ERRORS,
                                                                            null, response);

        assertThat(output.greeting()).isEqualTo("hello");
        assertThat(output.sdkHttpResponse()).isSameAs(response);
        assertThat(output.responseMetadata().requestId()).isEqualTo("req-1");
    }

    @ParameterizedTest
    @MethodSource("variants")
    void emptyBodyOrEmptyObjectIsAnEmptyOutput(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);
        for (SdkHttpFullResponse response : new SdkHttpFullResponse[] {
            response(200, null).build(), response(200, new byte[0]).build(), response(200, utf8("{}")).build(),
            response(200, utf8(" {} \n")).build()}) {
            GreetResponse output = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                response);
            assertThat(output.greeting()).isNull();
            assertThat(output.count()).isNull();
            assertThat(output.sdkHttpResponse()).isSameAs(response);
        }
    }

    @ParameterizedTest
    @MethodSource("variants")
    void bodyOfAnOutputWithoutMembersIsNotRead(Variant variant) {
        // As earlier SDK clients (legacy-disposition.csv json-core-output.json#23).
        NoInputRequest output = variant.create(SETTINGS).deserializeResponse(
            NO_OUTPUT, new ExecutionAttributes(), ERRORS, null, response(200, utf8("THIS ISN'T JSON!!!")).build());

        assertThat(output).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"greeting\":", "{} {}", "[]", "{\"greeting\":true}"})
    void unparseableSuccessBodyIsAClientException(String body) {
        assertThatThrownBy(() -> new AwsJson1Protocol(SETTINGS).deserializeResponse(
            GREET, new ExecutionAttributes(), ERRORS, null, response(200, utf8(body)).build()))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall Greet: ")
            .hasCauseInstanceOf(SerializationException.class);
    }

    static Stream<Arguments> errorTypes() {
        return Stream.of("smithy.example#FooError",
                         "FooError",
                         "smithy.example#FooError:http://internal.amazon.com/coral/smithy.example/",
                         "FooError:http://internal.amazon.com/coral/smithy.example/",
                         "other.ns#FooError",
                         "com.amazonaws.example.v20240101#FooError:http://internal.amazon.com/coral/")
                     .flatMap(type -> VARIANTS.stream().map(variant -> Arguments.of(variant, type)));
    }

    @ParameterizedTest
    @MethodSource("errorTypes")
    void errorTypeIsResolvedFromTheBodyType(Variant variant, String type) {
        assertFooError(variant.create(SETTINGS), response(400, json("__type", type, "message", "Bad", "reason", "why")));
    }

    @ParameterizedTest
    @MethodSource("errorTypes")
    void errorTypeIsResolvedFromTheErrorTypeHeader(Variant variant, String type) {
        assertFooError(variant.create(SETTINGS), response(400, json("message", "Bad", "reason", "why"))
            .putHeader("X-Amzn-Errortype", type));
    }

    @ParameterizedTest
    @MethodSource("variants")
    void errorTypeIsResolvedFromTheCodeMember(Variant variant) {
        assertFooError(variant.create(SETTINGS), response(400, json("code", "FooError", "message", "Bad",
                                                                    "reason", "why")));
    }

    @ParameterizedTest
    @MethodSource("variants")
    void errorTypeHeaderWinsOverTheBodyAndTypeWinsOverCode(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);

        assertFooError(protocol, response(400, json("__type", "QueueMissing", "message", "Bad", "reason", "why"))
            .putHeader("X-Amzn-Errortype", "FooError"));
        assertFooError(protocol, response(400, json("__type", "FooError", "code", "QueueMissing", "message", "Bad",
                                                    "reason", "why")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"message", "Message", "errorMessage"})
    void errorMessageIsReadFromTheLegacyMessageMembers(String member) {
        assertThatThrownBy(() -> new AwsJson11Protocol(SETTINGS).deserializeResponse(
            GREET, new ExecutionAttributes(), ERRORS, null,
            response(400, json("__type", "Nope", member, "Boom")).build()))
            .isExactlyInstanceOf(ExampleServiceException.class)
            .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorMessage()).isEqualTo("Boom"));
    }

    @Test
    void errorMessageHeaderWinsOverTheBody() {
        assertThatThrownBy(() -> new AwsJson1Protocol(SETTINGS).deserializeResponse(
            GREET, new ExecutionAttributes(), ERRORS, null,
            response(400, json("__type", "FooError", "message", "body")).putHeader("X-Amzn-Error-Message", "header")
                                                                         .build()))
            .isExactlyInstanceOf(FooError.class)
            .satisfies(e -> assertThat(((FooError) e).awsErrorDetails().errorMessage()).isEqualTo("header"));
    }

    @ParameterizedTest
    @MethodSource("variants")
    void unresolvedErrorsAreTheBaseException(Variant variant) {
        AwsJsonProtocol protocol = variant.create(SETTINGS);

        assertBaseException(protocol, response(500, json("__type", "smithy.example#Nope:http://x/", "message", "Boom"))
            .build(), "Nope", 500);
        assertBaseException(protocol, response(400, utf8("{}")).build(), null, 400);
        assertBaseException(protocol, response(503, null).build(), null, 503);
        assertBaseException(protocol, response(400, utf8("<html>")).build(), null, 400);
        assertBaseException(protocol, response(400, utf8("<html>")).putHeader("X-Amzn-Errortype", "Nope").build(),
                            "Nope", 400);
    }

    @ParameterizedTest
    @MethodSource("variants")
    void awsQueryCompatibleErrorsResolveTheQueryErrorHeaderFirst(Variant variant) {
        AwsJsonProtocol queryCompatible = variant.create(QUERY_COMPATIBLE_SETTINGS);
        SdkHttpFullResponse headerOverridesBody = response(400, json("__type", "smithy.example#FooError"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();

        assertThatThrownBy(() -> queryCompatible.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                     headerOverridesBody))
            .isExactlyInstanceOf(QueueMissing.class)
            .satisfies(e -> assertThat(((QueueMissing) e).awsErrorDetails().errorCode()).isEqualTo(QueueMissing.CODE));

        // A query code that does not resolve is still the reported code; the protocol's own type picks the class.
        SdkHttpFullResponse unknownQueryCode = response(400, json("__type", "FooError"))
            .putHeader("x-amzn-query-error", "AWS.Unknown;Receiver")
            .build();
        assertThatThrownBy(() -> queryCompatible.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                     unknownQueryCode))
            .isExactlyInstanceOf(FooError.class)
            .satisfies(e -> assertThat(((FooError) e).awsErrorDetails().errorCode()).isEqualTo("AWS.Unknown"));
    }

    @ParameterizedTest
    @MethodSource("variants")
    void queryErrorHeaderIsIgnoredWhenTheServiceIsNotQueryCompatible(Variant variant) {
        SdkHttpFullResponse response = response(400, json("__type", "FooError"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();

        assertThatThrownBy(() -> variant.create(SETTINGS).deserializeResponse(GREET, new ExecutionAttributes(), ERRORS,
                                                                              null, response))
            .isExactlyInstanceOf(FooError.class)
            .satisfies(e -> assertThat(((FooError) e).awsErrorDetails().errorCode()).isEqualTo("FooError"));
    }

    @Test
    void factoriesAreRegisteredForTheProtocolIds() {
        ClientProtocolFactory<?> json10 = ClientProtocolFactories.forId(AwsJson1_0Trait.ID);
        ClientProtocolFactory<?> json11 = ClientProtocolFactories.forId(AwsJson1_1Trait.ID);

        assertThat(json10).isInstanceOf(AwsJson1Protocol.Factory.class);
        assertThat(json11).isInstanceOf(AwsJson11Protocol.Factory.class);
        assertThat(AwsJson1Protocol.Factory.INSTANCE.id()).isEqualTo(AwsJson1_0Trait.ID);
        assertThat(AwsJson11Protocol.Factory.INSTANCE.id()).isEqualTo(AwsJson1_1Trait.ID);
    }

    @Test
    void factoriesCreateTheProtocolFromTheSettings() {
        ProtocolSettings settings = ProtocolSettings.builder().service(ShapeId.fromParts("com.example", "Svc")).build();

        ClientProtocol<?, ?> json10 = AwsJson1Protocol.Factory.INSTANCE.createProtocol(settings, new AwsJson1_0Trait());
        ClientProtocol<?, ?> json11 = AwsJson11Protocol.Factory.INSTANCE.createProtocol(settings, null);

        assertThat(json10).isInstanceOf(AwsJson1Protocol.class);
        assertThat(json11).isInstanceOf(AwsJson11Protocol.class);
        for (ClientProtocol<?, ?> created : Arrays.asList(json10, json11)) {
            AwsJsonProtocol protocol = (AwsJsonProtocol) created;
            SdkHttpFullRequest request = serialize(protocol, new GreetRequest("hi", null));
            assertThat(request.firstMatchingHeader("X-Amz-Target")).hasValue("Svc.Greet");

            // Without a base exception supplier in the settings, unresolved errors are the generic service exception.
            assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                  response(400, json("__type", "FooError")).build()))
                .isInstanceOf(SdkServiceException.class)
                .isNotInstanceOf(FooError.class);
        }
    }

    private static void assertFooError(AwsJsonProtocol protocol, SdkHttpFullResponse.Builder response) {
        SdkHttpFullResponse built = response.putHeader("x-amzn-RequestId", "req-2").build();
        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, built))
            .isExactlyInstanceOf(FooError.class)
            .satisfies(e -> {
                FooError error = (FooError) e;
                assertThat(error.reason()).isEqualTo("why");
                assertThat(error.statusCode()).isEqualTo(400);
                assertThat(error.requestId()).isEqualTo("req-2");
                assertThat(error.awsErrorDetails().errorCode()).isEqualTo("FooError");
                assertThat(error.awsErrorDetails().errorMessage()).isEqualTo("Bad");
            });
    }

    private static void assertBaseException(AwsJsonProtocol protocol, SdkHttpFullResponse response, String errorCode,
                                            int status) {
        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response))
            .isExactlyInstanceOf(ExampleServiceException.class)
            .satisfies(e -> {
                AwsServiceException error = (AwsServiceException) e;
                assertThat(error.statusCode()).isEqualTo(status);
                assertThat(error.awsErrorDetails().errorCode()).isEqualTo(errorCode);
            });
    }

    private static SdkHttpFullRequest serialize(AwsJsonProtocol protocol, GreetRequest input) {
        return protocol.serializeRequest(GREET, input, new ExecutionAttributes(), ENDPOINT);
    }

    private static final class Variant {
        private final ShapeId id;
        private final String contentType;
        private final String name;
        private final Function<ProtocolSettings, AwsJsonProtocol> constructor;

        private Variant(ShapeId id, String contentType, String name,
                        Function<ProtocolSettings, AwsJsonProtocol> constructor) {
            this.id = id;
            this.contentType = contentType;
            this.name = name;
            this.constructor = constructor;
        }

        AwsJsonProtocol create(ProtocolSettings settings) {
            return constructor.apply(settings);
        }

        @Override
        public String toString() {
            return name;
        }
    }
    /**
     * The input of {@code Chat}, an operation with an input and an output event stream.
     */
    private static final class EventStreamInput implements SerializableStruct {
        private static final Schema EVENT = Schema.structureBuilder(ShapeId.fromParts(TestShapes.NS, "Message"))
                                                  .putMember("text", PreludeSchemas.STRING)
                                                  .build();
        private static final Schema STREAM =
            Schema.unionBuilder(ShapeId.fromParts(TestShapes.NS, "ChatStream"), StreamingTrait.INSTANCE)
                  .putMember("message", EVENT)
                  .build();
        private static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts(TestShapes.NS, "ChatInput"))
                                                   .putMember("greeting", PreludeSchemas.STRING)
                                                   .putMember("stream", STREAM)
                                                   .build();
        private static final Schema OUTPUT = Schema.structureBuilder(ShapeId.fromParts(TestShapes.NS, "ChatOutput"))
                                                   .putMember("greeting", PreludeSchemas.STRING)
                                                   .putMember("stream", STREAM)
                                                   .builderSupplier(GreetResponseBuilder::new)
                                                   .build();
        private static final SimpleApiOperation<EventStreamInput, GreetResponse> OPERATION =
            SimpleApiOperation.<EventStreamInput, GreetResponse>builder()
                              .schema(Schema.createOperation(ShapeId.fromParts(TestShapes.NS, "Chat")))
                              .inputSchema(() -> SCHEMA)
                              .outputSchema(() -> OUTPUT)
                              .errorRegistry(ERRORS)
                              .service(TestShapes.SERVICE)
                              .build();

        private final String greeting;

        private EventStreamInput(String greeting) {
            this.greeting = greeting;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString(SCHEMA.member("greeting"), greeting);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return "greeting".equals(member.memberName()) ? (T) greeting : null;
        }
    }
}
