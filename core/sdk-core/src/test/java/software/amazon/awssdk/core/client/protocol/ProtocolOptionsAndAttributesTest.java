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

package software.amazon.awssdk.core.client.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * The client options and execution attributes that carry the protocol SPI through a client and a call.
 */
class ProtocolOptionsAndAttributesTest {
    private static final ProtocolSettings SETTINGS =
        ProtocolSettings.builder().service(ShapeId.from("com.example#ExampleService")).build();

    @Test
    void clientOptions_storeProtocolFactoryAndTransports() {
        ClientProtocol<?, ?> protocol = new TestClientProtocolFactory().createProtocol(SETTINGS, null);
        ClientProtocolFactory<?> factory = new TestClientProtocolFactory();
        CountingTransport transport = new CountingTransport();
        CountingAsyncTransport asyncTransport = new CountingAsyncTransport();

        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                                                                     .option(SdkClientOption.CLIENT_PROTOCOL_FACTORY, factory)
                                                                     .option(SdkClientOption.CLIENT_TRANSPORT, transport)
                                                                     .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT,
                                                                             asyncTransport)
                                                                     .build();

        assertThat(configuration.option(SdkClientOption.CLIENT_PROTOCOL)).isSameAs(protocol);
        assertThat(configuration.option(SdkClientOption.CLIENT_PROTOCOL_FACTORY)).isSameAs(factory);
        assertThat(configuration.option(SdkClientOption.CLIENT_TRANSPORT)).isSameAs(transport);
        assertThat(configuration.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT)).isSameAs(asyncTransport);
    }

    @Test
    void clientOptions_protocolNotSet_isNull() {
        assertThat(SdkClientConfiguration.builder().build().option(SdkClientOption.CLIENT_PROTOCOL)).isNull();
    }

    @Test
    void close_configurationWithTransports_closesThemOnce() {
        CountingTransport transport = new CountingTransport();
        CountingAsyncTransport asyncTransport = new CountingAsyncTransport();
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(SdkClientOption.CLIENT_TRANSPORT, transport)
                                                                     .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT,
                                                                             asyncTransport)
                                                                     .build();

        configuration.close();

        assertThat(transport.closed).hasValue(1);
        assertThat(asyncTransport.closed).hasValue(1);
    }

    @Test
    void executionAttributes_storeOperationProtocolAndErrorRegistry() {
        Schema service = Schema.createService(ShapeId.from("com.example#ExampleService"));
        Schema operationSchema = Schema.createOperation(ShapeId.from("com.example#GetThing"));
        Schema structure = Schema.structureBuilder(ShapeId.from("com.example#Empty")).build();
        ApiOperation<?, ?> operation = SimpleApiOperation.<SerializableStruct, SerializableStruct>builder()
                                                         .schema(operationSchema)
                                                         .inputSchema(() -> structure)
                                                         .outputSchema(() -> structure)
                                                         .service(() -> service)
                                                         .build();
        ClientProtocol<?, ?> protocol = new TestClientProtocolFactory().createProtocol(SETTINGS, null);
        TypeRegistry errors = TypeRegistry.empty();

        ExecutionAttributes attributes = ExecutionAttributes.builder()
                                                            .put(SdkExecutionAttribute.API_OPERATION, operation)
                                                            .put(SdkExecutionAttribute.CLIENT_PROTOCOL, protocol)
                                                            .put(SdkExecutionAttribute.ERROR_REGISTRY, errors)
                                                            .build();

        assertThat(attributes.getAttribute(SdkExecutionAttribute.API_OPERATION)).isSameAs(operation);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL)).isSameAs(protocol);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY)).isSameAs(errors);
    }

    @Test
    void executionAttributes_haveDescriptiveNames() {
        assertThat(SdkExecutionAttribute.API_OPERATION).hasToString("ApiOperation");
        assertThat(SdkExecutionAttribute.CLIENT_PROTOCOL).hasToString("ClientProtocol");
        assertThat(SdkExecutionAttribute.ERROR_REGISTRY).hasToString("ErrorRegistry");
    }

    private static final class CountingTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        private final AtomicInteger closed = new AtomicInteger();

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }

    private static final class CountingAsyncTransport
        implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        private final AtomicInteger closed = new AtomicInteger();

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context,
                                                           SdkHttpFullRequest request,
                                                           AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }
}
