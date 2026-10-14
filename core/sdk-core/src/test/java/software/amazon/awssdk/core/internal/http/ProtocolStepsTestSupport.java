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

package software.amazon.awssdk.core.internal.http;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * A stub protocol, operation, shapes and interceptor for the {@link ProtocolMarshalling} and
 * {@link ProtocolResponseHandling} tests. The shapes are shaped like generated model classes: an {@link SdkRequest}
 * and an {@link SdkResponse} that are also {@link SerializableStruct}s.
 */
final class ProtocolStepsTestSupport {
    static final String NS = "com.example";
    static final ShapeId PROTOCOL_ID = ShapeId.from("com.example.test#stubProtocol");
    static final ShapeId OPERATION_ID = ShapeId.fromParts(NS, "GetThing");
    static final String SECRET = "member-value-that-must-not-be-logged";

    static final Schema SERVICE_SCHEMA = Schema.createService(ShapeId.fromParts(NS, "ThingService"));
    static final ApiOperation<TestRequest, TestResponse> OPERATION =
        SimpleApiOperation.<TestRequest, TestResponse>builder()
                          .schema(Schema.createOperation(OPERATION_ID))
                          .inputSchema(() -> TestRequest.SCHEMA)
                          .outputSchema(() -> TestResponse.SCHEMA)
                          .errorRegistry(TypeRegistry.empty())
                          .service(() -> SERVICE_SCHEMA)
                          .build();

    private ProtocolStepsTestSupport() {
    }

    /**
     * Creates an execution context like the client handler's after {@code modifyRequest}: the attributes hold the
     * operation and the protocol, and the interceptor context holds the request.
     */
    static ExecutionContext executionContext(ClientProtocol<?, ?> protocol,
                                             SdkRequest request,
                                             MetricCollector metricCollector,
                                             ExecutionInterceptor... interceptors) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkExecutionAttribute.API_OPERATION, OPERATION);
        attributes.putAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL, protocol);
        List<ExecutionInterceptor> interceptorList = new ArrayList<>();
        Collections.addAll(interceptorList, interceptors);
        return ExecutionContext.builder()
                               .executionAttributes(attributes)
                               .interceptorChain(new ExecutionInterceptorChain(interceptorList))
                               .interceptorContext(InterceptorContext.builder().request(request).build())
                               .metricCollector(metricCollector)
                               .build();
    }

    static SdkHttpFullRequest serializedRequest(URI endpoint, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.POST)
                                 .uri(endpoint)
                                 .encodedPath("/service/ThingService/operation/GetThing")
                                 .putHeader("Content-Type", "application/cbor")
                                 .putHeader("Content-Length", String.valueOf(bytes.length))
                                 .contentStreamProvider(() -> new ByteArrayInputStream(bytes))
                                 .build();
    }

    static SdkHttpFullResponse response(int status, TrackingStream body) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(status)
                                  .putHeader("x-amzn-RequestId", "request-1")
                                  .content(AbortableInputStream.create(body))
                                  .build();
    }

    /**
     * A response body that records whether it was closed.
     */
    static final class TrackingStream extends ByteArrayInputStream {
        private volatile boolean closed;

        TrackingStream(String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            closed = true;
        }

        boolean closed() {
            return closed;
        }
    }

    /**
     * Serializes and deserializes through functions set by each test, and records the arguments it received.
     */
    static final class StubProtocol implements ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> {
        private final MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> exchange;
        Serialize serialize = (operation, input, context, endpoint) -> serializedRequest(endpoint, "{}");
        Deserialize deserialize = (operation, context, registry, request, response) -> {
            throw new UnsupportedOperationException();
        };
        ApiOperation<?, ?> operation;
        Object input;
        ExecutionAttributes context;
        URI endpoint;
        TypeRegistry errorRegistry;
        SdkHttpFullRequest request;
        SdkHttpFullResponse response;
        int deserializeCalls;

        StubProtocol() {
            this(HttpMessageExchange.INSTANCE);
        }

        StubProtocol(MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> exchange) {
            this.exchange = exchange;
        }

        @Override
        public ShapeId id() {
            return PROTOCOL_ID;
        }

        @Override
        public Codec payloadCodec() {
            return null;
        }

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return exchange;
        }

        @Override
        public <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest serializeRequest(
            ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
            this.operation = operation;
            this.input = input;
            this.context = context;
            this.endpoint = endpoint;
            return serialize.apply(operation, input, context, endpoint);
        }

        @Override
        public SdkHttpFullRequest updateServiceEndpoint(SdkHttpFullRequest request, Endpoint endpoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            this.operation = operation;
            this.context = context;
            this.errorRegistry = errorRegistry;
            this.request = request;
            this.response = response;
            deserializeCalls++;
            return (O) deserialize.apply(operation, context, errorRegistry, request, response);
        }
    }

    @FunctionalInterface
    interface Serialize {
        SdkHttpFullRequest apply(ApiOperation<?, ?> operation, Object input, ExecutionAttributes context, URI endpoint);
    }

    @FunctionalInterface
    interface Deserialize {
        Object apply(ApiOperation<?, ?> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
                     SdkHttpFullRequest request, SdkHttpFullResponse response);
    }

    /**
     * Records each hook it receives, with the request, HTTP request, request body and response the context exposes.
     */
    static final class RecordingInterceptor implements ExecutionInterceptor {
        final List<String> hooks = new ArrayList<>();
        final List<Context.AfterMarshalling> afterMarshalling = new ArrayList<>();
        final List<Context.AfterUnmarshalling> afterUnmarshalling = new ArrayList<>();
        final List<Context.ModifyResponse> modifyResponse = new ArrayList<>();
        final List<ExecutionAttributes> attributes = new ArrayList<>();
        SdkResponse replacement;
        RuntimeException afterUnmarshallingFailure;

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
            hooks.add("beforeMarshalling");
            attributes.add(executionAttributes);
        }

        @Override
        public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
            hooks.add("afterMarshalling");
            afterMarshalling.add(context);
            attributes.add(executionAttributes);
        }

        @Override
        public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
            hooks.add("afterUnmarshalling");
            afterUnmarshalling.add(context);
            attributes.add(executionAttributes);
            if (afterUnmarshallingFailure != null) {
                throw afterUnmarshallingFailure;
            }
        }

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
            hooks.add("modifyResponse");
            modifyResponse.add(context);
            attributes.add(executionAttributes);
            return replacement != null ? replacement : context.response();
        }
    }

    /**
     * An operation input shaped like a generated request class, with one member holding {@link #SECRET} by default.
     */
    static final class TestRequest extends SdkRequest implements SerializableStruct {
        static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts(NS, "GetThingRequest"))
                                           .putMember("name", PreludeSchemas.STRING)
                                           .build();
        private final String name;

        TestRequest(String name) {
            this.name = name;
        }

        @Override
        public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
            return Optional.empty();
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (name != null) {
                serializer.writeString(SCHEMA.member("name"), name);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) name;
        }

        @Override
        public String toString() {
            return "TestRequest(name=" + name + ")";
        }
    }

    /**
     * An operation output shaped like a generated response class. {@link #copies()} counts the {@code toBuilder}
     * copies it went through.
     */
    static final class TestResponse extends SdkResponse implements SerializableStruct {
        static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts(NS, "GetThingResponse"))
                                           .putMember("value", PreludeSchemas.STRING)
                                           .build();
        private final String value;
        private final int copies;

        private TestResponse(TestResponseBuilder builder) {
            super(builder);
            this.value = builder.value;
            this.copies = builder.copies;
        }

        static TestResponseBuilder builder() {
            return new TestResponseBuilder();
        }

        String value() {
            return value;
        }

        int copies() {
            return copies;
        }

        @Override
        public TestResponseBuilder toBuilder() {
            TestResponseBuilder builder = new TestResponseBuilder(this);
            builder.value = value;
            builder.copies = copies + 1;
            return builder;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (value != null) {
                serializer.writeString(SCHEMA.member("value"), value);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) value;
        }

        static final class TestResponseBuilder extends BuilderImpl implements ShapeBuilder<TestResponse> {
            private String value;
            private int copies;

            TestResponseBuilder() {
            }

            TestResponseBuilder(TestResponse response) {
                super(response);
            }

            TestResponseBuilder value(String value) {
                this.value = value;
                return this;
            }

            @Override
            public TestResponse build() {
                return new TestResponse(this);
            }

            @Override
            public TestResponseBuilder deserialize(ShapeDeserializer decoder) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Schema sdkSchema() {
                return TestResponse.SCHEMA;
            }
        }
    }
}
