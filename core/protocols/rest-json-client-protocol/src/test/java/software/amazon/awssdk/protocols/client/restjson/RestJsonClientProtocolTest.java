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

package software.amazon.awssdk.protocols.client.restjson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.BLOB_PAYLOAD;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.BLOB_PAYLOAD_OPERATION;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.EMPTY;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.GREET_INPUT;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.HEADER_ONLY;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.HEADER_ONLY_OPERATION;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.NESTED;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.NO_OUTPUT;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.SETTINGS;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.STREAMING_OUTPUT_OPERATION;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.STRUCT_PAYLOAD;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.STRUCT_PAYLOAD_OPERATION;
import static software.amazon.awssdk.protocols.client.restjson.TestShapes.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactories;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.restjson.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.restjson.TestShapes.FooError;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.utils.IoUtils;

class RestJsonClientProtocolTest {
    private static final URI ENDPOINT = URI.create("https://example.com/prefix");
    private static final RestJsonClientProtocol PROTOCOL = new RestJsonClientProtocol(SETTINGS);

    @Test
    void identityCodecAndMessageExchange() {
        assertThat(PROTOCOL.id()).isEqualTo(RestJson1Trait.ID);
        assertThat(PROTOCOL.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThat(PROTOCOL.payloadCodec()).isInstanceOf(JsonCodec.class).isSameAs(PROTOCOL.payloadCodec());
        JsonSettings settings = ((JsonCodec) PROTOCOL.payloadCodec()).settings();
        assertThat(settings.defaultNamespace()).isEqualTo(TestShapes.NS);
        assertThat(settings.toBuilder().defaultNamespace(null).build()).isEqualTo(JsonSettings.REST_JSON_1);
        assertThat(PROTOCOL).hasToString("RestJsonClientProtocol(smithy.example#ExampleService)");
        assertThatThrownBy(() -> new RestJsonClientProtocol(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void serializeRequest_withEveryBinding_placesMembersByTheirTraits() throws IOException {
        SdkHttpFullRequest request = serialize(GREET, SchemaStruct.builder(GREET_INPUT)
                                                                  .set("label", "a b")
                                                                  .set("path", "x/y z")
                                                                  .set("query", "v&w")
                                                                  .set("header", "h")
                                                                  .set("message", "hi")
                                                                  .set("nested", nested("n"))
                                                                  .build());

        assertThat(request.method()).isEqualTo(SdkHttpMethod.PUT);
        assertThat(request.protocol()).isEqualTo("https");
        assertThat(request.host()).isEqualTo("example.com");
        assertThat(request.encodedPath()).isEqualTo("/prefix/greet/a%20b/x/y%20z");
        assertThat(request.rawQueryParameters()).containsOnlyKeys("mode", "q");
        assertThat(request.rawQueryParameters().get("mode")).containsExactly("fast");
        assertThat(request.rawQueryParameters().get("q")).containsExactly("v&w");
        assertThat(request.headers()).containsOnlyKeys("X-Header", "Content-Type", "Content-Length");
        assertThat(request.firstMatchingHeader("X-Header")).hasValue("h");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");
        String body = body(request);
        assertThat(body).isEqualTo("{\"Msg\":\"hi\",\"nested\":{\"value\":\"n\"}}");
        assertThat(request.firstMatchingHeader("Content-Length"))
            .hasValue(String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
        assertThat(request.contentStreamProvider().get().contentAsByteBufferOrNull()).as("zero-copy body").isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"/x/y, x/y", "x/y, x/y", "//x, /x", "'/', ''"})
    void serializeRequest_withGreedyLabel_dropsOneLeadingSlashLikeTheLegacyMarshaller(String value, String sent) {
        SdkHttpFullRequest request = serialize(GREET, SchemaStruct.builder(GREET_INPUT)
                                                                  .set("label", "l")
                                                                  .set("path", value)
                                                                  .build());

        assertThat(request.encodedPath()).isEqualTo("/prefix/greet/l/" + sent);
    }

    @Test
    void serializeRequest_withBodyMembersAllNull_sendsAnEmptyObject() throws IOException {
        SdkHttpFullRequest request = serialize(GREET, SchemaStruct.builder(GREET_INPUT)
                                                                  .set("label", "l")
                                                                  .set("path", "p")
                                                                  .build());

        assertThat(body(request)).isEqualTo("{}");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("2");
        assertThat(request.rawQueryParameters()).containsOnlyKeys("mode");
    }

    @Test
    void serializeRequest_withoutBodyOrPayloadMembers_sendsNoBody() {
        SdkHttpFullRequest headerOnly = serialize(HEADER_ONLY_OPERATION,
                                                  SchemaStruct.builder(HEADER_ONLY).set("header", "h").build());
        SdkHttpFullRequest empty = serialize(NO_OUTPUT, SchemaStruct.builder(EMPTY).build());

        for (SdkHttpFullRequest request : Arrays.asList(headerOnly, empty)) {
            assertThat(request.contentStreamProvider()).isEmpty();
            assertThat(request.firstMatchingHeader("Content-Type")).isEmpty();
            assertThat(request.firstMatchingHeader("Content-Length")).isEmpty();
        }
        assertThat(headerOnly.method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(headerOnly.headers()).containsOnlyKeys("X-Header");
    }

    @Test
    void serializeRequest_withNullStructurePayload_sendsAnEmptyObject() throws IOException {
        SdkHttpFullRequest unset = serialize(STRUCT_PAYLOAD_OPERATION,
                                             SchemaStruct.builder(STRUCT_PAYLOAD).set("header", "h").build());
        SdkHttpFullRequest set = serialize(STRUCT_PAYLOAD_OPERATION,
                                           SchemaStruct.builder(STRUCT_PAYLOAD).set("nested", nested("n")).build());

        assertThat(body(unset)).isEqualTo("{}");
        assertThat(unset.firstMatchingHeader("Content-Type")).hasValue("application/json");
        assertThat(body(set)).isEqualTo("{\"value\":\"n\"}");
        assertThat(set.firstMatchingHeader("Content-Type")).hasValue("application/json");
    }

    @Test
    void serializeRequest_withBlobPayload_sendsTheRawBytesOrNoBody() throws IOException {
        SdkHttpFullRequest unset = serialize(BLOB_PAYLOAD_OPERATION, SchemaStruct.builder(BLOB_PAYLOAD).build());
        SdkHttpFullRequest set = serialize(BLOB_PAYLOAD_OPERATION,
                                           SchemaStruct.builder(BLOB_PAYLOAD)
                                                       .set("data", ByteBuffer.wrap(TestShapes.utf8("raw")))
                                                       .build());

        assertThat(unset.contentStreamProvider()).isEmpty();
        assertThat(body(set)).isEqualTo("raw");
        assertThat(set.firstMatchingHeader("Content-Type")).hasValue("application/octet-stream");
    }

    @Test
    void serializeRequest_withNullLabel_failsAsAClientException() {
        assertThatThrownBy(() -> serialize(GREET, SchemaStruct.builder(GREET_INPUT).set("path", "p").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to marshall Greet: ")
            .hasMessageContaining("smithy.example#GreetInput$label");
    }

    @Test
    void deserializeResponse_withBodyHeaderAndStatus_readsEachByItsTrait() {
        SdkHttpFullResponse response = response(201, "{\"Hello\":\"hi\",\"greeting\":\"ignored\"}")
            .putHeader("X-Count", "3")
            .build();

        SchemaStruct output = deserialize(GREET, response);

        assertThat(output.get("greeting")).isEqualTo("hi");
        assertThat(output.get("count")).isEqualTo(3);
        assertThat(output.get("status")).isEqualTo(201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/xml", "application/json; charset=utf-8"})
    void deserializeResponse_withAnyContentType_parsesTheBodyWithoutChecking(String contentType) {
        SdkHttpFullResponse response = response(200, "{\"Hello\":\"hi\"}").putHeader("Content-Type", contentType)
                                                                          .build();

        assertThat(deserialize(GREET, response).get("greeting")).isEqualTo("hi");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not json", "{\"Hello\":"})
    void deserializeResponse_withOutputWithoutMembers_ignoresTheBody(String body) {
        SchemaStruct output = deserialize(NO_OUTPUT, response(200, body).putHeader("Content-Type", "text/plain").build());

        assertThat(output.sdkSchema()).isSameAs(EMPTY);
    }

    @Test
    void deserializeResponse_withEmptyOrMissingBody_leavesBodyMembersUnset() {
        SchemaStruct empty = deserialize(GREET, response(200, "").putHeader("X-Count", "1").build());
        SchemaStruct missing = deserialize(GREET, response(200, null).build());

        assertThat(empty.get("greeting")).isNull();
        assertThat(empty.get("count")).isEqualTo(1);
        assertThat(missing.get("greeting")).isNull();
    }

    @Test
    void deserializeResponse_withStructurePayload_readsTheWholeBodyIntoIt() {
        SchemaStruct output = deserialize(STRUCT_PAYLOAD_OPERATION, response(200, "{\"value\":\"n\"}").build());

        assertThat(((SchemaStruct) output.get("nested")).get("value")).isEqualTo("n");
    }

    @Test
    void deserializeResponse_withStreamingPayload_readsHeadersAndLeavesTheBodyUnread() throws IOException {
        ByteArrayInputStream content = new ByteArrayInputStream(TestShapes.utf8("stream"));
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .putHeader("X-Header", "h")
                                                          .content(AbortableInputStream.create(content))
                                                          .build();

        SchemaStruct output = deserialize(STREAMING_OUTPUT_OPERATION, response);

        assertThat(output.get("header")).isEqualTo("h");
        assertThat(output.get("body")).isNull();
        assertThat(IoUtils.toUtf8String(content)).isEqualTo("stream");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"Hello\":", "{\"Hello\":\"hi\"} trailing", "[]", "{\"Hello\":true}"})
    void deserializeResponse_withMalformedBody_failsAsAClientException(String body) {
        assertThatThrownBy(() -> deserialize(GREET, response(200, body).build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall Greet: ");
    }

    @Test
    void deserializeResponse_withInvalidHeaderValue_failsNamingTheMember() {
        assertThatThrownBy(() -> deserialize(GREET, response(200, "{}").putHeader("X-Count", "three").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("smithy.example#GreetOutput$count")
            .hasMessageNotContaining("three");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "FooError",
        "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/",
        "smithy.example#FooError",
        "aws.protocoltests.restjson#FooError",
        "smithy.example#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"})
    void deserializeResponse_withErrorTypeHeader_resolvesTheModeledErrorWithItsBindings(String errorType) {
        SdkHttpFullResponse response = response(404, "{\"message\":\"m\",\"reason\":\"r\"}")
            .putHeader("X-Amzn-Errortype", errorType)
            .putHeader("X-Retry-After", "5")
            .putHeader("x-amzn-RequestId", "req-1")
            .build();

        FooError error = (FooError) error(response);

        assertThat(error.reason()).isEqualTo("r");
        assertThat(error.retryAfter()).isEqualTo("5");
        assertThat(error.getMessage()).startsWith("m");
        assertThat(error.statusCode()).isEqualTo(404);
        assertThat(error.requestId()).isEqualTo("req-1");
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(error.awsErrorDetails().errorMessage()).isEqualTo("m");
    }

    @ParameterizedTest
    @CsvSource({
        "'{\"__type\":\"FooError\"}'",
        "'{\"__type\":\"smithy.example#FooError:http://internal.amazon.com/\"}'",
        "'{\"code\":\"FooError\"}'",
        "'{\"__type\":\"FooError\",\"code\":\"Other\"}'"})
    void deserializeResponse_withBodyDiscriminator_resolvesTheModeledError(String body) {
        assertThat(error(response(400, body).build())).isInstanceOf(FooError.class);
    }

    @Test
    void deserializeResponse_withHeaderAndBodyDiscriminators_prefersTheHeader() {
        SdkHttpFullResponse response = response(400, "{\"__type\":\"Other\"}")
            .putHeader("X-Amzn-Errortype", "FooError")
            .build();

        assertThat(error(response)).isInstanceOf(FooError.class);
    }

    @ParameterizedTest
    @CsvSource({"message", "Message", "errorMessage"})
    void deserializeResponse_withMessageKey_readsTheMessage(String key) {
        SdkHttpFullResponse response = response(400, "{\"" + key + "\":\"text\"}")
            .putHeader("X-Amzn-Errortype", "Unknown")
            .build();

        AwsServiceException error = (AwsServiceException) error(response);

        assertThat(error.awsErrorDetails().errorMessage()).isEqualTo("text");
    }

    @Test
    void deserializeResponse_withMessageHeader_prefersItOverTheBody() {
        SdkHttpFullResponse response = response(400, "{\"message\":\"body\"}")
            .putHeader("X-Amzn-Errortype", "FooError")
            .putHeader("X-Amzn-Error-Message", "header")
            .build();

        assertThat(((AwsServiceException) error(response)).awsErrorDetails().errorMessage()).isEqualTo("header");
    }

    @ParameterizedTest
    @CsvSource({"'{\"__type\":\"Unknown\"}', Unknown", "'{}', ", "'not json', ", "'', "})
    void deserializeResponse_withUnresolvedError_returnsTheBaseException(String body, String code) {
        SdkServiceException error = error(response(500, body).build());

        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(error.statusCode()).isEqualTo(500);
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo(code);
    }

    @Test
    void factory_isRegisteredAndCreatesTheProtocol() {
        assertThat(ClientProtocolFactories.forId(RestJson1Trait.ID)).isInstanceOf(RestJsonClientProtocol.Factory.class);
        assertThat(RestJsonClientProtocol.Factory.INSTANCE.id()).isEqualTo(RestJson1Trait.ID);

        ProtocolSettings settings = ProtocolSettings.builder().service(ShapeId.fromParts("com.example", "Svc")).build();
        ClientProtocol<?, ?> created = RestJsonClientProtocol.Factory.INSTANCE.createProtocol(settings, null);
        ClientProtocol<?, ?> withTrait = new RestJsonClientProtocol.Factory().createProtocol(settings,
                                                                                            new RestJson1Trait());

        assertThat(created).isInstanceOf(RestJsonClientProtocol.class)
                           .hasToString("RestJsonClientProtocol(com.example#Svc)");
        assertThat(withTrait).isInstanceOf(RestJsonClientProtocol.class);
        assertThat(((JsonCodec) created.payloadCodec()).settings().defaultNamespace()).isEqualTo("com.example");
    }

    private static SchemaStruct nested(String value) {
        return SchemaStruct.builder(NESTED).set("value", value).build();
    }

    private static SdkHttpFullRequest serialize(ApiOperation<SerializableStruct, SerializableStruct> operation,
                                                SerializableStruct input) {
        return PROTOCOL.serializeRequest(operation, input, new ExecutionAttributes(), ENDPOINT);
    }

    private static SchemaStruct deserialize(ApiOperation<SerializableStruct, SerializableStruct> operation,
                                            SdkHttpFullResponse response) {
        return (SchemaStruct) PROTOCOL.deserializeResponse(operation, new ExecutionAttributes(), ERRORS,
                                                           request(), response);
    }

    private static SdkServiceException error(SdkHttpFullResponse response) {
        try {
            deserialize(GREET, response);
        } catch (SdkServiceException e) {
            return e;
        }
        throw new AssertionError("Expected an SdkServiceException");
    }

    private static SdkHttpFullRequest request() {
        return SdkHttpFullRequest.builder().method(SdkHttpMethod.GET).uri(ENDPOINT).build();
    }

    private static String body(SdkHttpFullRequest request) throws IOException {
        return IoUtils.toUtf8String(request.contentStreamProvider().get().newStream());
    }
}
