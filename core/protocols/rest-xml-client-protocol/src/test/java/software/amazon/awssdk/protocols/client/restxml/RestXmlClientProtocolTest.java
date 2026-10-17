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

package software.amazon.awssdk.protocols.client.restxml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.BLOB_PAYLOAD;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.BLOB_PAYLOAD_OPERATION;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.CONFIG_PAYLOAD;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.CONFIG_PAYLOAD_OPERATION;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.EMPTY;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.GREET_INPUT;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.HEADER_ONLY;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.HEADER_ONLY_OPERATION;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.NESTED;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.NO_OUTPUT;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.PAYLOAD_CONFIG;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.SETTINGS;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.STREAMING_OUTPUT_OPERATION;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.STRUCT_PAYLOAD;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.STRUCT_PAYLOAD_OPERATION;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.XMLNS;
import static software.amazon.awssdk.protocols.client.restxml.TestShapes.response;

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
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.codecs.xml.XmlSettings;
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
import software.amazon.awssdk.protocols.client.restxml.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.restxml.TestShapes.FooError;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.RestXmlTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.utils.IoUtils;

class RestXmlClientProtocolTest {
    private static final URI ENDPOINT = URI.create("https://example.com/prefix");
    private static final RestXmlClientProtocol PROTOCOL = new RestXmlClientProtocol(SETTINGS);
    private static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    @Test
    void identityCodecAndMessageExchange() {
        assertThat(PROTOCOL.id()).isEqualTo(RestXmlTrait.ID);
        assertThat(PROTOCOL.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThat(PROTOCOL.payloadCodec()).isInstanceOf(XmlCodec.class).isSameAs(PROTOCOL.payloadCodec());
        XmlCodec codec = (XmlCodec) PROTOCOL.payloadCodec();
        assertThat(codec.settings().defaultXmlNamespace()).isEqualTo(new XmlNamespaceTrait(XMLNS));
        assertThat(codec.settings().toBuilder().defaultXmlNamespace(null).build()).isEqualTo(XmlSettings.REST_XML);
        assertThat(codec.strictRootElement()).isFalse();
        assertThat(codec.xmlDeclaration()).isTrue();
        assertThat(codec.wrapperElements()).isEmpty();
        assertThat(PROTOCOL).hasToString("RestXmlClientProtocol(smithy.example#ExampleService)");
        assertThatThrownBy(() -> new RestXmlClientProtocol(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void serializeRequest_withEveryBinding_placesMembersByTheirTraits() throws IOException {
        SdkHttpFullRequest request = serialize(GREET, SchemaStruct.builder(GREET_INPUT)
                                                                  .set("label", "a b")
                                                                  .set("path", "x/y z")
                                                                  .set("query", "v&w")
                                                                  .set("header", "h")
                                                                  .set("id", "7")
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
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/xml");
        String body = body(request);
        assertThat(body).isEqualTo(DECLARATION + "<GreetRequest xmlns=\"" + XMLNS + "\" id=\"7\"><Msg>hi</Msg>"
                                   + "<nested><value>n</value></nested></GreetRequest>");
        assertThat(request.firstMatchingHeader("Content-Length"))
            .hasValue(String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
        assertThat(request.contentStreamProvider().get().contentAsByteBufferOrNull()).as("zero-copy body").isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"/x/y, /x/y", "x/y, x/y", "//x, //x", "'/', /"})
    void serializeRequest_withGreedyLabel_keepsTheLeadingSlashLikeTheLegacyMarshaller(String value, String sent) {
        SdkHttpFullRequest request = serialize(GREET, SchemaStruct.builder(GREET_INPUT)
                                                                  .set("label", "l")
                                                                  .set("path", value)
                                                                  .build());

        assertThat(request.encodedPath()).isEqualTo("/prefix/greet/l/" + sent);
    }

    @Test
    void serializeRequest_withBodyMembersAllNull_sendsTheEmptyRootElement() throws IOException {
        SdkHttpFullRequest request = serialize(GREET, SchemaStruct.builder(GREET_INPUT)
                                                                  .set("label", "l")
                                                                  .set("path", "p")
                                                                  .build());

        assertThat(body(request)).isEqualTo(DECLARATION + "<GreetRequest xmlns=\"" + XMLNS + "\"></GreetRequest>");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/xml");
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
    void serializeRequest_withStructurePayload_sendsItAsTheRootOrNoBodyWhenNull() throws IOException {
        SdkHttpFullRequest unset = serialize(STRUCT_PAYLOAD_OPERATION,
                                             SchemaStruct.builder(STRUCT_PAYLOAD).set("header", "h").build());
        SdkHttpFullRequest set = serialize(STRUCT_PAYLOAD_OPERATION,
                                           SchemaStruct.builder(STRUCT_PAYLOAD).set("nested", nested("n")).build());

        assertThat(unset.contentStreamProvider()).isEmpty();
        assertThat(unset.firstMatchingHeader("Content-Type")).isEmpty();
        assertThat(unset.headers()).containsOnlyKeys("X-Header");
        assertThat(body(set)).isEqualTo(DECLARATION + "<Nested xmlns=\"" + XMLNS + "\"><value>n</value></Nested>");
        assertThat(set.firstMatchingHeader("Content-Type")).hasValue("application/xml");
    }

    @Test
    void serializeRequest_withPayloadXmlNameAndNamespace_namesTheRootByTheTarget() throws IOException {
        SchemaStruct config = SchemaStruct.builder(PAYLOAD_CONFIG).set("value", "v").build();

        SdkHttpFullRequest request = serialize(CONFIG_PAYLOAD_OPERATION,
                                               SchemaStruct.builder(CONFIG_PAYLOAD).set("config", config).build());

        assertThat(body(request)).isEqualTo(DECLARATION + "<Config xmlns=\"https://config.example.com/\">"
                                            + "<value>v</value></Config>");
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

    @ParameterizedTest
    @ValueSource(strings = {
        "<GreetOutput><Hello>hi</Hello><greeting>ignored</greeting></GreetOutput>",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<GreetOutput xmlns=\"https://example.com/\"><Hello>hi</Hello>"
        + "</GreetOutput>",
        "<GreetResult><Hello>hi</Hello></GreetResult>"})
    void deserializeResponse_withBodyHeaderAndStatus_readsEachByItsTrait(String body) {
        SdkHttpFullResponse response = response(201, body).putHeader("X-Count", "3").build();

        SchemaStruct output = deserialize(GREET, response);

        assertThat(output.get("greeting")).isEqualTo("hi");
        assertThat(output.get("count")).isEqualTo(3);
        assertThat(output.get("status")).isEqualTo(201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/json", "text/xml; charset=utf-8"})
    void deserializeResponse_withAnyContentType_parsesTheBodyWithoutChecking(String contentType) {
        SdkHttpFullResponse response = response(200, "<GreetOutput><Hello>hi</Hello></GreetOutput>")
            .putHeader("Content-Type", contentType)
            .build();

        assertThat(deserialize(GREET, response).get("greeting")).isEqualTo("hi");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not xml", "<Unclosed>"})
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
        SchemaStruct output = deserialize(STRUCT_PAYLOAD_OPERATION,
                                          response(200, "<Nested><value>n</value></Nested>").build());

        assertThat(((SchemaStruct) output.get("nested")).get("value")).isEqualTo("n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"<Nested><value>n</value></Nested>", "raw bytes", "<?xml version=\"1.0\"?><a/>"})
    void deserializeResponse_withBlobPayload_readsTheRawBodyWithoutParsingIt(String body) {
        SchemaStruct output = deserialize(BLOB_PAYLOAD_OPERATION,
                                          response(200, body).putHeader("Content-Type", "application/xml").build());

        assertThat(output.get("data")).isEqualTo(ByteBuffer.wrap(TestShapes.utf8(body)));
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
    @ValueSource(strings = {"<GreetOutput><Hello>hi", "<GreetOutput></GreetOutput><trailing/>", "not xml",
                            "<GreetOutput><Hello>hi</Hello></Other>"})
    void deserializeResponse_withMalformedBody_failsAsAClientException(String body) {
        assertThatThrownBy(() -> deserialize(GREET, response(200, body).build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall Greet: ");
    }

    @Test
    void deserializeResponse_withInvalidHeaderValue_failsNamingTheMember() {
        assertThatThrownBy(() -> deserialize(GREET, response(200, "").putHeader("X-Count", "three").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("smithy.example#GreetOutput$count")
            .hasMessageNotContaining("three");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<ErrorResponse><Error><Type>Sender</Type><Code>FooError</Code><Message>m</Message><Reason>r</Reason></Error>"
        + "<RequestId>body-id</RequestId></ErrorResponse>",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ErrorResponse xmlns=\"https://example.com/\">\n  <Error>\n"
        + "    <Code>FooError</Code>\n    <Message>m</Message>\n    <Reason>r</Reason>\n  </Error>\n"
        + "  <RequestID>body-id</RequestID>\n</ErrorResponse>\n",
        "<Error><Code>FooError</Code><Message>m</Message><Reason>r</Reason><RequestId>body-id</RequestId></Error>",
        "<Error><Code>smithy.example#FooError</Code><Message>m</Message><Reason>r</Reason>"
        + "<RequestId>body-id</RequestId></Error>"})
    void deserializeResponse_withWrappedOrBareError_resolvesTheModeledErrorWithItsBindings(String body) {
        SdkHttpFullResponse response = response(404, body)
            .putHeader("X-Retry-After", "5")
            .putHeader("x-amzn-RequestId", "header-id")
            .putHeader("x-amz-id-2", "ext-id")
            .build();

        FooError error = (FooError) error(response);

        assertThat(error.reason()).isEqualTo("r");
        assertThat(error.retryAfter()).isEqualTo("5");
        assertThat(error.getMessage()).startsWith("m");
        assertThat(error.statusCode()).isEqualTo(404);
        assertThat(error.requestId()).isEqualTo("body-id");
        assertThat(error.extendedRequestId()).isEqualTo("ext-id");
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(error.awsErrorDetails().errorMessage()).isEqualTo("m");
        assertThat(error.awsErrorDetails().rawResponse().asUtf8String()).isEqualTo(body);
    }

    @ParameterizedTest
    @CsvSource({"x-amzn-RequestId, a", "x-amz-request-id, b"})
    void deserializeResponse_withoutBodyRequestId_readsTheRequestIdHeader(String header, String value) {
        SdkHttpFullResponse response = response(404, "<ErrorResponse><Error><Code>FooError</Code></Error></ErrorResponse>")
            .putHeader(header, value)
            .build();

        assertThat(error(response).requestId()).isEqualTo(value);
    }

    @Test
    void deserializeResponse_withRequestIdNextToAndInsideError_prefersTheOneNextToIt() {
        SdkHttpFullResponse response = response(404, "<ErrorResponse><Error><Code>FooError</Code>"
                                                     + "<RequestId>inner</RequestId></Error>"
                                                     + "<RequestId>outer</RequestId></ErrorResponse>").build();

        assertThat(error(response).requestId()).isEqualTo("outer");
    }

    @Test
    void deserializeResponse_withErrorTypeHeader_ignoresItLikeTheLegacyUnmarshaller() {
        SdkHttpFullResponse withBody = response(400, "<Error><Code>Other</Code></Error>")
            .putHeader("X-Amzn-Errortype", "FooError")
            .putHeader("X-Amzn-Error-Message", "header")
            .build();
        SdkHttpFullResponse headerOnly = response(400, "").putHeader("X-Amzn-Errortype", "FooError").build();

        AwsServiceException fromBody = (AwsServiceException) error(withBody);
        assertThat(fromBody).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(fromBody.awsErrorDetails().errorCode()).isEqualTo("Other");
        assertThat(fromBody.awsErrorDetails().errorMessage()).isNull();
        assertThat(error(headerOnly)).isExactlyInstanceOf(ExampleServiceException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "'<ErrorResponse><Error><Code>Unknown</Code><Message>text</Message></Error></ErrorResponse>', Unknown, text",
        "'<Error><Code>Unknown</Code></Error>', Unknown, ",
        "'<Error><Message>text</Message></Error>', , text",
        "'<ErrorResponse></ErrorResponse>', , ",
        "'<Other><Code>FooError</Code></Other>', FooError, ",
        "'not xml', , ",
        "'<ErrorResponse><Error><Code>FooError', , ",
        "'', , "})
    void deserializeResponse_withUnresolvedError_returnsTheBaseException(String body, String code, String message) {
        SdkServiceException error = error(response(500, body).putHeader("x-amzn-RequestId", "req").build());

        if ("FooError".equals(code)) {
            // An unwrapped root of another name still has its Code read, as the codec reads any root leniently.
            assertThat(error).isInstanceOf(FooError.class);
            return;
        }
        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(error.statusCode()).isEqualTo(500);
        assertThat(error.requestId()).isEqualTo("req");
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo(code);
        assertThat(((AwsServiceException) error).awsErrorDetails().errorMessage()).isEqualTo(message);
    }

    @Test
    void factory_isRegisteredAndCreatesTheProtocol() {
        assertThat(ClientProtocolFactories.forId(RestXmlTrait.ID)).isInstanceOf(RestXmlClientProtocol.Factory.class);
        assertThat(RestXmlClientProtocol.Factory.INSTANCE.id()).isEqualTo(RestXmlTrait.ID);

        ProtocolSettings settings = ProtocolSettings.builder().service(ShapeId.fromParts("com.example", "Svc")).build();
        ClientProtocol<?, ?> created = RestXmlClientProtocol.Factory.INSTANCE.createProtocol(settings, null);
        ClientProtocol<?, ?> withTrait = new RestXmlClientProtocol.Factory()
            .createProtocol(settings, new RestXmlTrait(Arrays.asList("http/1.1"), Arrays.asList("http/1.1"), true));

        assertThat(created).isInstanceOf(RestXmlClientProtocol.class)
                           .hasToString("RestXmlClientProtocol(com.example#Svc)");
        assertThat(withTrait).isInstanceOf(RestXmlClientProtocol.class);
        assertThat(((XmlCodec) created.payloadCodec()).settings().defaultXmlNamespace()).isNull();
    }

    @Test
    void serializeRequest_withoutServiceNamespace_declaresNone() throws IOException {
        Schema service = Schema.createService(ShapeId.fromParts("com.example", "Svc"));
        RestXmlClientProtocol protocol = new RestXmlClientProtocol(ProtocolSettings.builder()
                                                                                   .serviceSchema(service)
                                                                                   .build());

        SdkHttpFullRequest request = protocol.serializeRequest(STRUCT_PAYLOAD_OPERATION,
                                                               SchemaStruct.builder(STRUCT_PAYLOAD)
                                                                           .set("nested", nested("n"))
                                                                           .build(),
                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(body(request)).isEqualTo(DECLARATION + "<Nested><value>n</value></Nested>");
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
