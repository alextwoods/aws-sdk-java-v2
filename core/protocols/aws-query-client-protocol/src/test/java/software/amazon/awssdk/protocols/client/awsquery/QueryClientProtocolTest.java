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

package software.amazon.awssdk.protocols.client.awsquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.awsquery.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.awsquery.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.awsquery.TestShapes.NO_INPUT;
import static software.amazon.awssdk.protocols.client.awsquery.TestShapes.NO_OUTPUT;
import static software.amazon.awssdk.protocols.client.awsquery.TestShapes.SETTINGS;
import static software.amazon.awssdk.protocols.client.awsquery.TestShapes.response;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactories;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.DashboardMissing;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.FooError;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.GreetRequest;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.GreetResponse;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.NoInputRequest;
import software.amazon.awssdk.protocols.client.awsquery.TestShapes.QueueMissing;
import software.amazon.awssdk.protocols.client.awsquery.internal.QueryFormSerializer;
import software.amazon.awssdk.protocols.client.http.HttpClientProtocol;
import software.amazon.awssdk.schema.traits.AwsQueryTrait;
import software.amazon.awssdk.schema.traits.Ec2QueryTrait;
import software.amazon.awssdk.utils.IoUtils;

class QueryClientProtocolTest {
    private static final URI ENDPOINT = URI.create("https://unresolved.invalid");
    private static final String XMLNS = " xmlns=\"https://example.com/doc/2024-01-01/\"";

    private static final AwsQueryClientProtocol AWS_QUERY = new AwsQueryClientProtocol(SETTINGS);
    private static final Ec2QueryClientProtocol EC2_QUERY = new Ec2QueryClientProtocol(SETTINGS);

    static Stream<HttpClientProtocol> protocols() {
        return Stream.of(AWS_QUERY, EC2_QUERY);
    }

    @Test
    void idCodecAndExchange() {
        assertThat(AWS_QUERY.id()).isEqualTo(AwsQueryTrait.ID);
        assertThat(EC2_QUERY.id()).isEqualTo(Ec2QueryTrait.ID);
        assertThat(AWS_QUERY.payloadCodec()).isNull();
        assertThat(EC2_QUERY.payloadCodec()).isNull();
        assertThat(AWS_QUERY.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThat(AWS_QUERY).hasToString("AwsQueryClientProtocol(smithy.example#ExampleService)");
        assertThat(EC2_QUERY).hasToString("Ec2QueryClientProtocol(smithy.example#ExampleService)");
    }

    @Test
    void serializeRequest_awsQuery_postsTheForm() throws IOException {
        SdkHttpFullRequest request = AWS_QUERY.serializeRequest(GREET, new GreetRequest("hello world", "Bob"),
                                                                new ExecutionAttributes(), ENDPOINT);

        String body = "Action=Greet&Version=2024-01-01&Text=hello%20world&name=Bob";
        assertThat(request.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(request.encodedPath()).isEqualTo("/");
        assertThat(request.rawQueryParameters()).isEmpty();
        assertThat(request.headers()).containsOnlyKeys("Content-Type", "Content-Length");
        assertThat(request.firstMatchingHeader("Content-Type"))
            .hasValue("application/x-www-form-urlencoded");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(body.length()));
        assertThat(body(request)).isEqualTo(body);
        ByteBuffer shared = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(StandardCharsets.UTF_8.decode(shared).toString()).isEqualTo(body);
    }

    @Test
    void serializeRequest_ec2Query_capitalizesTheKeys() throws IOException {
        SdkHttpFullRequest request = EC2_QUERY.serializeRequest(GREET, new GreetRequest("hi", "Bob"),
                                                                new ExecutionAttributes(), ENDPOINT);

        assertThat(body(request)).isEqualTo("Action=Greet&Version=2024-01-01&Text=hi&Name=Bob");
        assertThat(request.firstMatchingHeader("Content-Type"))
            .hasValue("application/x-www-form-urlencoded");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void serializeRequest_sizesTheBodyFromTheOperationsLastRequest(boolean ec2) throws IOException {
        HttpClientProtocol protocol = ec2 ? new Ec2QueryClientProtocol(SETTINGS) : new AwsQueryClientProtocol(SETTINGS);
        GreetRequest input = new GreetRequest("hello world", "Bob");
        SdkHttpFullRequest first = protocol.serializeRequest(GREET, input, new ExecutionAttributes(), ENDPOINT);
        String body = body(first);
        int size = body.length();
        QueryFormSerializer reference = ec2 ? QueryFormSerializer.forEc2Query("Greet", "2024-01-01")
                                            : QueryFormSerializer.forAwsQuery("Greet", "2024-01-01");
        input.serializeMembers(reference);
        int required = reference.requiredCapacity();
        for (int call = 2; call <= 3; call++) {
            SdkHttpFullRequest next = protocol.serializeRequest(GREET, input, new ExecutionAttributes(), ENDPOINT);
            assertThat(body(next)).as("call %s", call).isEqualTo(body);
            assertThat(next.headers()).as("call %s", call).isEqualTo(first.headers());
            assertThat(next.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(size));
            // The array starts at the capacity the last body needed, plus one eighth, and does not grow.
            int capacity = next.contentStreamProvider().get().contentAsByteBufferOrNull().array().length;
            assertThat(capacity).as("call %s", call).isEqualTo(Math.max(32, required + required / 8));
            assertThat(capacity).as("call %s", call).isLessThan(2 * size);
        }
    }

    @ParameterizedTest
    @MethodSource("protocols")
    void serializeRequest_bodyLargerThanTheDefaultCapacity_growsTheArray(HttpClientProtocol protocol)
        throws IOException {
        char[] chars = new char[300];
        Arrays.fill(chars, 'x');
        String text = new String(chars);
        HttpClientProtocol fresh = protocol instanceof Ec2QueryClientProtocol ? new Ec2QueryClientProtocol(SETTINGS)
                                                                              : new AwsQueryClientProtocol(SETTINGS);
        SdkHttpFullRequest large = fresh.serializeRequest(GREET, new GreetRequest(text, "Bob"),
                                                          new ExecutionAttributes(), ENDPOINT);
        String name = protocol instanceof Ec2QueryClientProtocol ? "Name" : "name";
        assertThat(body(large)).isEqualTo("Action=Greet&Version=2024-01-01&Text=" + text + "&" + name + "=Bob");
        SdkHttpFullRequest small = fresh.serializeRequest(GREET, new GreetRequest("hi", "Bob"),
                                                          new ExecutionAttributes(), ENDPOINT);
        assertThat(body(small)).isEqualTo("Action=Greet&Version=2024-01-01&Text=hi&" + name + "=Bob");
        assertThat(small.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(body(small).length()));
    }

    @ParameterizedTest
    @MethodSource("protocols")
    void serializeRequest_unitInput_writesOnlyTheActionAndVersion(HttpClientProtocol protocol) throws IOException {
        SdkHttpFullRequest request = protocol.serializeRequest(NO_INPUT, new NoInputRequest(),
                                                               new ExecutionAttributes(), ENDPOINT);

        assertThat(body(request)).isEqualTo("Action=NoInput&Version=2024-01-01");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("33");
    }

    @ParameterizedTest
    @MethodSource("protocols")
    void serializeRequest_endpointWithAPath_appendsASlash(HttpClientProtocol protocol) {
        SdkHttpFullRequest request = protocol.serializeRequest(GREET, new GreetRequest(null, null),
                                                               new ExecutionAttributes(),
                                                               URI.create("https://example.com/prefix"));
        assertThat(request.encodedPath()).isEqualTo("/prefix/");

        SdkHttpFullRequest moved = protocol.updateServiceEndpoint(
            protocol.serializeRequest(GREET, new GreetRequest(null, null), new ExecutionAttributes(), ENDPOINT),
            Endpoint.builder().url(URI.create("https://example.com:8443/base")).build());
        assertThat(moved.getUri()).hasToString("https://example.com:8443/base/");
    }

    @Test
    void constructor_withoutAServiceVersion_fails() {
        ProtocolSettings settings = SETTINGS.toBuilder().serviceVersion(null).build();

        assertThatThrownBy(() -> new AwsQueryClientProtocol(settings)).isInstanceOf(NullPointerException.class)
                                                                       .hasMessageContaining("serviceVersion");
        assertThatThrownBy(() -> new Ec2QueryClientProtocol(settings)).isInstanceOf(NullPointerException.class)
                                                                       .hasMessageContaining("serviceVersion");
    }

    @Test
    void deserializeResponse_awsQuery_readsTheResultAndTheBodyRequestId() {
        SdkHttpFullResponse response =
            response(200, "<GreetResponse" + XMLNS + "><GreetResult><greeting>hi</greeting><count>3</count>"
                          + "<unknown><a>b</a></unknown></GreetResult>"
                          + "<ResponseMetadata><RequestId>body-id</RequestId></ResponseMetadata></GreetResponse>")
                .putHeader("x-amzn-RequestId", "header-id")
                .putHeader("X-Count", "9")
                .build();

        GreetResponse output = AWS_QUERY.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response);

        assertThat(output.greeting()).isEqualTo("hi");
        assertThat(output.count()).isEqualTo("3");
        assertThat(output.sdkHttpResponse()).isSameAs(response);
        assertThat(output.responseMetadata().requestId()).isEqualTo("body-id");
    }

    @Test
    void deserializeResponse_ec2Query_readsTheResponseAndItsRequestId() {
        SdkHttpFullResponse response =
            response(200, "<GreetResponse" + XMLNS + "><requestId>ec2-id</requestId><greeting>hi</greeting>"
                          + "</GreetResponse>")
                .putHeader("x-amzn-RequestId", "header-id")
                .build();

        GreetResponse output = EC2_QUERY.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response);

        assertThat(output.greeting()).isEqualTo("hi");
        assertThat(output.responseMetadata().requestId()).isEqualTo("ec2-id");
    }

    @ParameterizedTest
    @MethodSource("protocols")
    void deserializeResponse_withoutABodyRequestId_usesTheHeader(HttpClientProtocol protocol) {
        String inner = protocol == AWS_QUERY ? "<GreetResult><greeting>hi</greeting></GreetResult>"
                                             : "<greeting>hi</greeting>";
        SdkHttpFullResponse response = response(200, "<GreetResponse>" + inner + "</GreetResponse>")
            .putHeader("x-amz-request-id", "header-id")
            .build();

        GreetResponse output = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response);

        assertThat(output.greeting()).isEqualTo("hi");
        assertThat(output.responseMetadata().requestId()).isEqualTo("header-id");
    }

    @ParameterizedTest
    @MethodSource("protocols")
    void deserializeResponse_noBody_buildsAnEmptyOutput(HttpClientProtocol protocol) {
        SdkHttpFullResponse response = response(200, null).putHeader("x-amzn-RequestId", "header-id").build();

        GreetResponse output = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response);

        assertThat(output.greeting()).isNull();
        assertThat(output.responseMetadata().requestId()).isEqualTo("header-id");
    }

    @Test
    void deserializeResponse_outputWithoutMembers_readsOnlyTheRequestId() {
        SdkHttpFullResponse response =
            response(200, "<NoOutputResponse><ResponseMetadata><RequestId>body-id</RequestId></ResponseMetadata>"
                          + "</NoOutputResponse>").build();

        GreetResponse output = AWS_QUERY.deserializeResponse(NO_OUTPUT, new ExecutionAttributes(), ERRORS, null,
                                                             response);

        assertThat(output.responseMetadata().requestId()).isEqualTo("body-id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not xml", "<OtherResponse><GreetResult/></OtherResponse>",
                            "<GreetResponse><OtherResult/></GreetResponse>", "<GreetResponse><GreetResult>"})
    void deserializeResponse_unreadableBody_isAClientException(String body) {
        assertThatThrownBy(() -> AWS_QUERY.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                               response(200, body).build()))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall Greet: ");
    }

    @Test
    void deserializeResponse_outputWithoutMembersAndAnotherRoot_usesTheHeaderRequestId() {
        SdkHttpFullResponse response = response(200, "<Other/>").putHeader("x-amzn-RequestId", "header-id").build();

        GreetResponse output = AWS_QUERY.deserializeResponse(NO_OUTPUT, new ExecutionAttributes(), ERRORS, null,
                                                             response);

        assertThat(output.responseMetadata().requestId()).isEqualTo("header-id");
    }

    @Test
    void awsQueryError_resolvesTheModeledErrorWithItsMembersAndMetadata() {
        String body = "<ErrorResponse" + XMLNS + "><Error><Type>Sender</Type><Code>FooError</Code>"
                      + "<Message>bad thing</Message><Reason>because</Reason></Error>"
                      + "<RequestId>body-id</RequestId></ErrorResponse>";
        SdkHttpFullResponse response = response(400, body).putHeader("x-amzn-RequestId", "header-id")
                                                          .putHeader("x-amz-id-2", "ext-id")
                                                          .build();

        AwsServiceException e = error(AWS_QUERY, response);

        assertThat(e).isInstanceOf(FooError.class);
        assertThat(((FooError) e).reason()).isEqualTo("because");
        assertThat(e.statusCode()).isEqualTo(400);
        assertThat(e.requestId()).isEqualTo("body-id");
        assertThat(e.extendedRequestId()).isEqualTo("ext-id");
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("bad thing");
        assertThat(e.awsErrorDetails().rawResponse().asUtf8String()).isEqualTo(body);
    }

    @Test
    void ec2QueryError_resolvesTheModeledErrorFromTheFirstError() {
        String body = "<Response><Errors><Error><Code>FooError</Code><Message>first</Message>"
                      + "<Reason>r1</Reason></Error>"
                      + "<Error><Code>QueueMissing</Code><Message>second</Message></Error></Errors>"
                      + "<RequestID>ec2-id</RequestID></Response>";

        AwsServiceException e = error(EC2_QUERY, response(400, body).build());

        assertThat(e).isInstanceOf(FooError.class);
        assertThat(((FooError) e).reason()).isEqualTo("r1");
        assertThat(e.requestId()).isEqualTo("ec2-id");
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("first");
    }

    static Stream<Arguments> customCodes() {
        return Stream.of(Arguments.of(QueueMissing.CODE, QueueMissing.class, QueueMissing.CODE),
                         Arguments.of(QueueMissing.CODE + ":suffix", QueueMissing.class, QueueMissing.CODE),
                         // As in CloudWatch, the code of DashboardMissing is the name of another error shape.
                         Arguments.of(DashboardMissing.CODE, DashboardMissing.class, DashboardMissing.CODE),
                         Arguments.of("smithy.example#FooError", FooError.class, "FooError"));
    }

    @ParameterizedTest
    @MethodSource("customCodes")
    void error_resolvesCustomCodesFirst(String code, Class<?> expected, String reportedCode) {
        AwsServiceException query = error(AWS_QUERY, response(400, errorResponse(code)).build());
        AwsServiceException ec2 = error(EC2_QUERY, response(400, ec2Response(code)).build());

        assertThat(query).isExactlyInstanceOf(expected);
        assertThat(ec2).isExactlyInstanceOf(expected);
        assertThat(query.awsErrorDetails().errorCode()).isEqualTo(reportedCode);
        assertThat(ec2.awsErrorDetails().errorCode()).isEqualTo(reportedCode);
    }

    @Test
    void error_unknownCode_isTheBaseExceptionWithTheCodeMessageAndRequestId() {
        AwsServiceException e = error(AWS_QUERY, response(500, errorResponse("Throttled")).build());

        assertThat(e).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(e.statusCode()).isEqualTo(500);
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("Throttled");
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("msg");
        assertThat(e.requestId()).isEqualTo("rid");
    }

    static Stream<Arguments> unresolvedBodies() {
        return Stream.of(Arguments.of(AWS_QUERY, null),
                         Arguments.of(AWS_QUERY, ""),
                         Arguments.of(AWS_QUERY, "not xml"),
                         Arguments.of(AWS_QUERY, "<Error><Code>FooError</Code></Error>"),
                         Arguments.of(AWS_QUERY, ec2Response("FooError")),
                         Arguments.of(AWS_QUERY, "<ErrorResponse><Error><Message>m</Message></Error></ErrorResponse>"),
                         Arguments.of(EC2_QUERY, errorResponse("FooError")),
                         Arguments.of(EC2_QUERY, "<Response><Errors/></Response>"));
    }

    @ParameterizedTest
    @MethodSource("unresolvedBodies")
    void error_withoutAResolvableCode_isTheBaseException(HttpClientProtocol protocol, String body) {
        AwsServiceException e = error(protocol, response(400, body).putHeader("x-amzn-RequestId", "header-id").build());

        assertThat(e).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(e.statusCode()).isEqualTo(400);
        assertThat(e.requestId()).isEqualTo("header-id");
        assertThat(e.awsErrorDetails().errorCode()).isNull();
    }

    @Test
    void error_ignoresTheErrorTypeHeader() {
        AwsServiceException e = error(AWS_QUERY, response(400, errorResponse("Throttled"))
            .putHeader("X-Amzn-Errortype", "FooError").build());

        assertThat(e).isExactlyInstanceOf(ExampleServiceException.class);
    }

    @Test
    void factories_areRegisteredAndCreateTheProtocols() {
        assertThat(ClientProtocolFactories.forId(AwsQueryTrait.ID).createProtocol(SETTINGS, null))
            .isInstanceOf(AwsQueryClientProtocol.class);
        assertThat(ClientProtocolFactories.forId(Ec2QueryTrait.ID).createProtocol(SETTINGS, null))
            .isInstanceOf(Ec2QueryClientProtocol.class);
        assertThat(AwsQueryClientProtocol.Factory.INSTANCE.id()).isEqualTo(AwsQueryTrait.ID);
        assertThat(Ec2QueryClientProtocol.Factory.INSTANCE.id()).isEqualTo(Ec2QueryTrait.ID);
        assertThat(AwsQueryClientProtocol.Factory.INSTANCE.createProtocol(SETTINGS, AwsQueryTrait.INSTANCE).id())
            .isEqualTo(AwsQueryTrait.ID);
        assertThat(Ec2QueryClientProtocol.Factory.INSTANCE.createProtocol(SETTINGS, Ec2QueryTrait.INSTANCE).id())
            .isEqualTo(Ec2QueryTrait.ID);
    }

    private static String errorResponse(String code) {
        return "<ErrorResponse><Error><Code>" + code + "</Code><Message>msg</Message></Error>"
               + "<RequestId>rid</RequestId></ErrorResponse>";
    }

    private static String ec2Response(String code) {
        return "<Response><Errors><Error><Code>" + code + "</Code><Message>msg</Message></Error></Errors>"
               + "<RequestID>rid</RequestID></Response>";
    }

    private static AwsServiceException error(HttpClientProtocol protocol, SdkHttpFullResponse response) {
        try {
            protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response);
        } catch (AwsServiceException e) {
            return e;
        }
        throw new AssertionError("Expected an error");
    }

    private static String body(SdkHttpFullRequest request) throws IOException {
        return IoUtils.toUtf8String(request.contentStreamProvider().get().newStream());
    }
}
