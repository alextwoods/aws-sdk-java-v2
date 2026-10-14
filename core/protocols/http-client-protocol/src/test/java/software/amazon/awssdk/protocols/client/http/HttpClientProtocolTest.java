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
import static software.amazon.awssdk.protocols.client.http.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.http.TestShapes.cbor;
import static software.amazon.awssdk.protocols.client.http.TestShapes.response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.http.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.http.TestShapes.FooError;
import software.amazon.awssdk.protocols.client.http.TestShapes.Greeting;
import software.amazon.awssdk.protocols.client.http.TestShapes.GreetingResponse;
import software.amazon.awssdk.protocols.client.http.internal.MarshallBufferSizeTracker;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;

class HttpClientProtocolTest {
    private static final ShapeId PROTOCOL_ID = ShapeId.fromParts("smithy.example", "testProtocol");

    @Test
    void identityAndMessageExchange() {
        TestProtocol protocol = new TestProtocol();
        assertThat(protocol.id()).isEqualTo(PROTOCOL_ID);
        assertThat(protocol.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThatThrownBy(() -> new TestProtocol(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "[{index}] {0} + {1}")
    @CsvSource(value = {
        "https://example.com, /service/X/operation/Y, /service/X/operation/Y",
        "https://example.com/, /service/X/operation/Y, /service/X/operation/Y",
        "https://example.com/prefix, /service/X/operation/Y, /prefix/service/X/operation/Y",
        "https://example.com/prefix/, /service/X/operation/Y, /prefix/service/X/operation/Y",
        "https://example.com/prefix, /, /prefix/",
        "https://example.com/a%20b, /c%2Fd, /a%20b/c%2Fd",
        "https://example.com/prefix, EMPTY, /prefix",
    }, nullValues = "EMPTY")
    void updateServiceEndpointJoinsPaths(String endpoint, String requestPath, String expectedPath) {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.POST)
                                                       .protocol("http")
                                                       .host("placeholder")
                                                       .encodedPath(requestPath == null ? "" : requestPath)
                                                       .build();

        SdkHttpFullRequest updated = new TestProtocol().updateServiceEndpoint(request, endpoint(endpoint));

        assertThat(updated.encodedPath()).isEqualTo(expectedPath);
    }

    @Test
    void updateServiceEndpointTakesSchemeHostAndPortAndKeepsTheRest() {
        ContentStreamProvider body = ContentStreamProvider.fromUtf8String("body");
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.PUT)
                                                       .uri(URI.create("http://placeholder:1234/op"))
                                                       .putRawQueryParameter("q", "1")
                                                       .putHeader("X-Test", "a")
                                                       .contentStreamProvider(body)
                                                       .build();
        Endpoint endpoint = Endpoint.builder()
                                    .url(URI.create("https://svc.example.com:8443/base?ignored=1"))
                                    .putHeader("X-Endpoint", "e")
                                    .build();

        SdkHttpFullRequest updated = new TestProtocol().updateServiceEndpoint(request, endpoint);

        assertThat(updated.protocol()).isEqualTo("https");
        assertThat(updated.host()).isEqualTo("svc.example.com");
        assertThat(updated.port()).isEqualTo(8443);
        assertThat(updated.encodedPath()).isEqualTo("/base/op");
        assertThat(updated.method()).isEqualTo(SdkHttpMethod.PUT);
        assertThat(updated.rawQueryParameters()).isEqualTo(Collections.singletonMap("q", Collections.singletonList("1")));
        assertThat(updated.headers()).isEqualTo(request.headers());
        assertThat(updated.contentStreamProvider()).containsSame(body);

        // The original request is unchanged.
        assertThat(request.getUri()).isEqualTo(URI.create("http://placeholder:1234/op?q=1"));

        SdkHttpFullRequest defaultPort = new TestProtocol().updateServiceEndpoint(request, endpoint("https://other.example.com"));
        assertThat(defaultPort.port()).isEqualTo(443);
        assertThat(defaultPort.getUri()).isEqualTo(URI.create("https://other.example.com/op?q=1"));
    }

    @Test
    void serializeRequestRoundTripsAndSharesTheBodyBuffer() {
        TestProtocol protocol = new TestProtocol();
        SdkHttpFullRequest request = protocol.serializeRequest(GREET, new Greeting("hello"), new ExecutionAttributes(),
                                                               URI.create("https://example.com"));

        ContentStreamProvider body = request.contentStreamProvider().get();
        ByteBuffer buffer = body.contentAsByteBufferOrNull();
        assertThat(buffer).isNotNull();
        assertThat(CODEC.deserializeShape(buffer, GREET.outputBuilder()).message()).isEqualTo("hello");
    }

    @Test
    void requestBodyBufferStartsAtTheLastBodySizeOfTheOperation() {
        TestProtocol protocol = new TestProtocol();
        SimpleApiOperation<Greeting, Greeting> other = TestShapes.operation("Other");
        assertThat(protocol.bodyBuffer(GREET).array()).hasSize(MarshallBufferSizeTracker.DEFAULT_CAPACITY);

        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            large.append('x');
        }
        SdkHttpFullRequest request = protocol.serializeRequest(GREET, new Greeting(large.toString()),
                                                               new ExecutionAttributes(), URI.create("https://example.com"));
        int bodySize = request.contentStreamProvider().get().contentAsByteBufferOrNull().remaining();

        assertThat(protocol.bodyBuffer(GREET).array().length).isGreaterThanOrEqualTo(bodySize);
        assertThat(protocol.bodyBuffer(other).array()).hasSize(MarshallBufferSizeTracker.DEFAULT_CAPACITY);
        // Hints are per protocol instance.
        assertThat(new TestProtocol().bodyBuffer(GREET).array()).hasSize(MarshallBufferSizeTracker.DEFAULT_CAPACITY);
    }

    @Test
    void serializationFailureBecomesAClientExceptionNamingTheOperation() {
        TestProtocol protocol = new TestProtocol();
        SerializationException cause = new SerializationException("Required httpLabel member Name was null");
        protocol.serializeFailure = cause;

        assertThatThrownBy(() -> protocol.serializeRequest(GREET, new Greeting("x"), new ExecutionAttributes(),
                                                           URI.create("https://example.com")))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessage("Unable to marshall Greet: Required httpLabel member Name was null")
            .hasCause(cause);

        protocol.serializeFailure = new IllegalStateException();
        assertThatThrownBy(() -> protocol.serializeRequest(GREET, new Greeting("x"), new ExecutionAttributes(),
                                                           URI.create("https://example.com")))
            .hasMessage("Unable to marshall Greet: java.lang.IllegalStateException");

        SdkClientException sdk = SdkClientException.create("already an SDK exception");
        protocol.serializeFailure = sdk;
        assertThatThrownBy(() -> protocol.serializeRequest(GREET, new Greeting("x"), new ExecutionAttributes(),
                                                           URI.create("https://example.com")))
            .isSameAs(sdk);
    }

    @Test
    void successfulResponseIsDeserialized() {
        Greeting output = new TestProtocol().deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                 response(200, cbor("message", "hi")).build());
        assertThat(output.message()).isEqualTo("hi");
    }

    @Test
    void buildOutputAttachesTheHttpResponseAndResponseMetadata() {
        SimpleApiOperation<Greeting, GreetingResponse> operation =
            SimpleApiOperation.<Greeting, GreetingResponse>builder()
                              .schema(Schema.createOperation(ShapeId.fromParts(TestShapes.NS, "GreetResponse")))
                              .inputSchema(() -> Greeting.SCHEMA)
                              .outputSchema(() -> GreetingResponse.SCHEMA)
                              .errorRegistry(ERRORS)
                              .service(TestShapes.SERVICE)
                              .build();
        SdkHttpFullResponse response = response(200, cbor("message", "hi")).putHeader("x-amzn-RequestId", "req-1")
                                                                           .putHeader("X-Other", "a")
                                                                           .build();

        GreetingResponse output = new TestProtocol().deserializeResponse(operation, new ExecutionAttributes(), ERRORS,
                                                                         null, response);

        assertThat(output.message()).isEqualTo("hi");
        assertThat(output.sdkHttpResponse()).isSameAs(response);
        assertThat(output.responseMetadata().requestId()).isEqualTo("req-1");
        assertThat(output.responseMetadata().toString()).contains("X-Other");
    }

    @Test
    void unparseableSuccessBodyBecomesAClientExceptionNamingTheOperation() {
        SdkHttpFullResponse response = response(200, new byte[] {(byte) 0xbf, 0x63}).build();

        assertThatThrownBy(() -> new TestProtocol().deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                        response))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall Greet: ")
            .hasCauseInstanceOf(SerializationException.class);
    }

    @Test
    void bodyReadFailureOnSuccessKeepsTheIoExceptionAsTheCause() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Connection reset");
            }
        };
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .content(AbortableInputStream.create(failing))
                                                          .build();

        assertThatThrownBy(() -> new TestProtocol().deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                                        response))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessage("Unable to unmarshall Greet: Connection reset")
            .hasCauseExactlyInstanceOf(IOException.class);
    }

    @Test
    void errorResponseThrowsTheCreatedError() {
        TestProtocol protocol = new TestProtocol();
        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                              response(400, cbor("__type", "FooError")).build()))
            .isExactlyInstanceOf(FooError.class)
            .satisfies(e -> assertThat(((SdkServiceException) e).statusCode()).isEqualTo(400));

        assertThatThrownBy(() -> protocol.deserializeResponse(GREET, new ExecutionAttributes(), ERRORS, null,
                                                              response(302, null).build()))
            .isExactlyInstanceOf(ExampleServiceException.class);
    }

    private static Endpoint endpoint(String url) {
        return Endpoint.builder().url(URI.create(url)).build();
    }

    /**
     * A minimal CBOR protocol over the base class.
     */
    private static final class TestProtocol extends HttpClientProtocol {
        private final HttpErrorDeserializer errors = HttpErrorDeserializer.builder()
                                                                          .codec(CODEC)
                                                                          .serviceId(TestShapes.SERVICE_ID)
                                                                          .baseExceptionBuilder(ExampleServiceException::builder)
                                                                          .build();
        private RuntimeException serializeFailure;

        private TestProtocol() {
            this(PROTOCOL_ID);
        }

        private TestProtocol(ShapeId id) {
            super(id);
        }

        ByteBufferOutputStream bodyBuffer(ApiOperation<?, ?> operation) {
            return newRequestBodyBuffer(operation);
        }

        @Override
        public Codec payloadCodec() {
            return CODEC;
        }

        @Override
        protected <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest createRequest(
            ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
            if (serializeFailure != null) {
                throw serializeFailure;
            }
            ByteBufferOutputStream out = newRequestBodyBuffer(operation);
            try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
                input.serialize(serializer);
            }
            return SdkHttpFullRequest.builder()
                                     .method(SdkHttpMethod.POST)
                                     .uri(endpoint)
                                     .encodedPath("/operation/" + operation.name())
                                     .contentStreamProvider(finishRequestBody(operation, out))
                                     .build();
        }

        @Override
        protected <I extends SerializableStruct, O extends SerializableStruct> O deserializeOutput(
            ApiOperation<I, O> operation, ExecutionAttributes context, SdkHttpFullRequest request,
            SdkHttpFullResponse response) {
            ShapeBuilder<O> builder = operation.outputBuilder();
            builder.deserialize(CODEC.createDeserializer(ResponseBodies.asByteBuffer(context, response)));
            return buildOutput(builder, response);
        }

        @Override
        protected SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                                  TypeRegistry errorRegistry, SdkHttpFullRequest request,
                                                  SdkHttpFullResponse response) {
            return errors.createError(operation, context, errorRegistry, response);
        }
    }
}
