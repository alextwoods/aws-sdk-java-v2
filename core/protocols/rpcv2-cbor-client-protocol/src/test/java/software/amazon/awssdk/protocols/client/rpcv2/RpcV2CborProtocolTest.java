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

package software.amazon.awssdk.protocols.client.rpcv2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.CODEC;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.NO_INPUT;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.QUERY_COMPATIBLE_SETTINGS;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.SETTINGS;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.bytes;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.cbor;
import static software.amazon.awssdk.protocols.client.rpcv2.TestShapes.response;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
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
import software.amazon.awssdk.protocols.client.rpcv2.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.rpcv2.TestShapes.FooError;
import software.amazon.awssdk.protocols.client.rpcv2.TestShapes.GreetRequest;
import software.amazon.awssdk.protocols.client.rpcv2.TestShapes.GreetResponse;
import software.amazon.awssdk.protocols.client.rpcv2.TestShapes.NoInputRequest;
import software.amazon.awssdk.protocols.client.rpcv2.TestShapes.QueueMissing;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;

class RpcV2CborProtocolTest {
    private static final URI ENDPOINT = URI.create("https://example.com");
    private static final String SERVICE_PATH = "/service/ExampleService/operation/";

    private final RpcV2CborProtocol protocol = new RpcV2CborProtocol(SETTINGS);
    private final RpcV2CborProtocol queryCompatible = new RpcV2CborProtocol(QUERY_COMPATIBLE_SETTINGS);

    @Test
    void identityCodecAndMessageExchange() {
        assertThat(protocol.id()).isEqualTo(Rpcv2CborTrait.ID);
        assertThat(protocol.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThat(protocol.payloadCodec()).isInstanceOf(Rpcv2CborCodec.class).isSameAs(protocol.payloadCodec());
        assertThat(((Rpcv2CborCodec) protocol.payloadCodec()).settings()).isEqualTo(CborSettings.RPCV2);
        assertThat(protocol).hasToString("RpcV2CborProtocol(smithy.example#ExampleService)");
        assertThatThrownBy(() -> new RpcV2CborProtocol(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void requestIsAPostToTheOperationPathWithCborHeadersAndBody() {
        SdkHttpFullRequest request = serialize(protocol, new GreetRequest("hi", null));

        assertThat(request.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(request.getUri()).isEqualTo(URI.create("https://example.com" + SERVICE_PATH + "Greet"));
        assertThat(request.headers()).containsOnlyKeys("smithy-protocol", "Accept", "Content-Type", "Content-Length");
        assertThat(request.firstMatchingHeader("smithy-protocol")).hasValue("rpc-v2-cbor");
        assertThat(request.firstMatchingHeader("Accept")).hasValue("application/cbor");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/cbor");

        // {"message": "hi"} as an indefinite-length map.
        byte[] expected = concat(new byte[] {(byte) 0xbf, 0x67}, ascii("message"), new byte[] {0x62}, ascii("hi"),
                                 new byte[] {(byte) 0xff});
        ByteBuffer body = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(body).as("zero-copy body").isNotNull();
        assertThat(bytes(body)).containsExactly(expected);
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(expected.length));
    }

    @Test
    void requestPathIsAppendedToTheEndpointPath() {
        SdkHttpFullRequest request = protocol.serializeRequest(GREET, new GreetRequest("hi", null),
                                                               new ExecutionAttributes(),
                                                               URI.create("http://localhost:8080/prefix"));

        assertThat(request.getUri()).isEqualTo(URI.create("http://localhost:8080/prefix" + SERVICE_PATH + "Greet"));
    }

    @Test
    void httpBindingTraitsAndJsonNameAreIgnored() {
        SdkHttpFullRequest request = serialize(protocol, new GreetRequest("hi", "Ada"));

        assertThat(request.headers()).doesNotContainKey("X-Name");
        assertThat(request.rawQueryParameters()).isEmpty();
        assertThat(request.encodedPath()).isEqualTo(SERVICE_PATH + "Greet");
        Document body = CODEC.createDeserializer(request.contentStreamProvider().get().contentAsByteBufferOrNull())
                             .readDocument();
        assertThat(body.asMap().keySet()).containsExactly("message", "name");
        assertThat(body.getMember("name").asString()).isEqualTo("Ada");

        GreetResponse output = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, request,
                                                            response(200, cbor("count", "3"))
                                                                .putHeader("X-Count", "7").build());
        assertThat(output.count()).isEqualTo("3");
    }

    @Test
    void unitInputSendsNoBodyAndNoContentType() {
        SdkHttpFullRequest request = protocol.serializeRequest(NO_INPUT, new NoInputRequest(), new ExecutionAttributes(),
                                                               ENDPOINT);

        assertThat(request.encodedPath()).isEqualTo(SERVICE_PATH + "NoInput");
        assertThat(request.contentStreamProvider()).isEmpty();
        assertThat(request.firstMatchingHeader("Content-Type")).isEmpty();
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("0");
        assertThat(request.firstMatchingHeader("smithy-protocol")).hasValue("rpc-v2-cbor");
        assertThat(request.firstMatchingHeader("Accept")).hasValue("application/cbor");
    }

    @Test
    void awsQueryCompatibleServicesSendTheQueryModeHeader() {
        assertThat(serialize(queryCompatible, new GreetRequest("hi", null)).firstMatchingHeader("x-amzn-query-mode"))
            .hasValue("true");
        assertThat(queryCompatible.serializeRequest(NO_INPUT, new NoInputRequest(), new ExecutionAttributes(), ENDPOINT)
                                  .firstMatchingHeader("x-amzn-query-mode"))
            .hasValue("true");
        assertThat(serialize(protocol, new GreetRequest("hi", null)).firstMatchingHeader("x-amzn-query-mode")).isEmpty();
    }

    @Test
    void outputIsDeserializedWithTheHttpResponseAndMetadata() {
        SdkHttpFullResponse response = response(200, cbor("greeting", "hello")).putHeader("x-amzn-RequestId", "req-1")
                                                                               .putHeader("smithy-protocol",
                                                                                          "rpc-v2-cbor")
                                                                               .build();

        GreetResponse output = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response);

        assertThat(output.greeting()).isEqualTo("hello");
        assertThat(output.sdkHttpResponse()).isSameAs(response);
        assertThat(output.responseMetadata().requestId()).isEqualTo("req-1");
    }

    @Test
    void emptyBodyOrEmptyMapIsAnEmptyOutput() {
        GreetResponse noContent = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                               response(200, null).build());
        GreetResponse emptyBody = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                               response(200, new byte[0]).build());
        GreetResponse emptyMap = protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                              response(200, new byte[] {(byte) 0xbf, (byte) 0xff})
                                                                  .build());

        for (GreetResponse output : new GreetResponse[] {noContent, emptyBody, emptyMap}) {
            assertThat(output.greeting()).isNull();
            assertThat(output.count()).isNull();
            assertThat(output.sdkHttpResponse()).isNotNull();
        }
    }

    @Test
    void unparseableSuccessBodyIsAClientException() {
        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                              response(200, new byte[] {(byte) 0xbf, 0x63}).build()))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall Greet: ")
            .hasCauseInstanceOf(SerializationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "smithy.example#FooError",
        "FooError",
        "smithy.example#FooError:http://internal.amazon.com/coral/smithy.example/",
        "FooError:http://internal.amazon.com/coral/smithy.example/"
    })
    void errorTypeIsResolvedFromTheBodyType(String type) {
        SdkHttpFullResponse response = response(400, cbor("__type", type, "message", "Bad", "reason", "why"))
            .putHeader("x-amzn-RequestId", "req-2")
            .build();

        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response))
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

    @Test
    void absoluteTypeFromAnotherNamespaceFallsBackToTheShapeName() {
        // Like earlier SDK clients, which match the error code with its namespace removed.
        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                              response(400, cbor("__type", "other.ns#FooError")).build()))
            .isExactlyInstanceOf(FooError.class);
    }

    @Test
    void unknownErrorTypeIsTheBaseException() {
        assertBaseException(protocol, response(500, cbor("__type", "Nope", "message", "Boom")).build(), "Nope", 500);
    }

    @Test
    void errorWithoutABodyTypeIsTheBaseException() {
        // rpcv2Cbor reads only __type: neither the code member nor the X-Amzn-Errortype header is a discriminator.
        assertBaseException(protocol, response(400, cbor("code", "FooError")).putHeader("X-Amzn-Errortype", "FooError")
                                                                            .build(), null, 400);
        assertBaseException(protocol, response(503, null).build(), null, 503);
        assertBaseException(protocol, response(400, new byte[] {0x7f}).build(), null, 400);
    }

    @Test
    void awsQueryCompatibleErrorsResolveTheQueryErrorHeaderFirst() {
        SdkHttpFullResponse response = response(400, cbor("__type", "smithy.example#QueueMissing"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();

        assertThatThrownBy(() -> queryCompatible.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                     response))
            .isExactlyInstanceOf(QueueMissing.class)
            .satisfies(e -> assertThat(((QueueMissing) e).awsErrorDetails().errorCode()).isEqualTo(QueueMissing.CODE));

        SdkHttpFullResponse headerOverridesBody = response(400, cbor("__type", "smithy.example#FooError"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();
        assertThatThrownBy(() -> queryCompatible.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                     headerOverridesBody))
            .isExactlyInstanceOf(QueueMissing.class);
    }

    @Test
    void queryErrorHeaderIsIgnoredWhenTheServiceIsNotQueryCompatible() {
        SdkHttpFullResponse response = response(400, cbor("__type", "FooError"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();

        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response))
            .isExactlyInstanceOf(FooError.class)
            .satisfies(e -> assertThat(((FooError) e).awsErrorDetails().errorCode()).isEqualTo("FooError"));
    }

    @Test
    void factoryIsRegisteredForTheProtocolId() {
        ClientProtocolFactory<?> discovered = ClientProtocolFactories.forId(Rpcv2CborTrait.ID);

        assertThat(discovered).isInstanceOf(RpcV2CborProtocol.Factory.class);
        assertThat(discovered.id()).isEqualTo(Rpcv2CborTrait.ID);
        assertThat(RpcV2CborProtocol.Factory.INSTANCE.id()).isEqualTo(Rpcv2CborTrait.ID);
    }

    @Test
    void factoryCreatesTheProtocolFromTheSettings() {
        ProtocolSettings settings = ProtocolSettings.builder().service(ShapeId.fromParts("com.example", "Svc")).build();

        ClientProtocol<?, ?> created = RpcV2CborProtocol.Factory.INSTANCE.createProtocol(settings, new Rpcv2CborTrait());

        assertThat(created).isInstanceOf(RpcV2CborProtocol.class);
        assertThat(created.id()).isEqualTo(Rpcv2CborTrait.ID);
        SdkHttpFullRequest request = ((RpcV2CborProtocol) created).serializeRequest(GREET, new GreetRequest("hi", null),
                                                                                    new ExecutionAttributes(), ENDPOINT);
        assertThat(request.encodedPath()).isEqualTo("/service/Svc/operation/Greet");

        // Without a base exception supplier in the settings, unresolved errors are the generic service exception.
        assertThatThrownBy(() -> ((RpcV2CborProtocol) created).deserializeResponse(
            GREET, new ExecutionAttributes(), ERRORS, null, response(400, cbor("__type", "FooError")).build()))
            .isInstanceOf(SdkServiceException.class)
            .isNotInstanceOf(FooError.class);
    }

    private static void assertBaseException(RpcV2CborProtocol protocol, SdkHttpFullResponse response, String errorCode,
                                            int status) {
        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null, response))
            .isExactlyInstanceOf(ExampleServiceException.class)
            .satisfies(e -> {
                AwsServiceException error = (AwsServiceException) e;
                assertThat(error.statusCode()).isEqualTo(status);
                assertThat(error.awsErrorDetails().errorCode()).isEqualTo(errorCode);
            });
    }

    private static SdkHttpFullRequest serialize(RpcV2CborProtocol protocol, GreetRequest input) {
        return protocol.serializeRequest(GREET, input, new ExecutionAttributes(), ENDPOINT);
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }
}
