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

package software.amazon.awssdk.awscore.client.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.AwsJson1_0Trait;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;

/**
 * Runtime protocol selection support for generated builders (task 3.23, Requirements 12.3, 12.5, 13.3).
 */
class ClientProtocolSupportTest {
    private static final ShapeId SERVICE_ID = ShapeId.fromParts("demo", "DemoService");
    private static final Rpcv2CborTrait CBOR_TRAIT = new Rpcv2CborTrait();
    private static final Schema SERVICE = Schema.createService(SERVICE_ID, new AwsJson1_0Trait(), CBOR_TRAIT);

    private static final MessageExchange<String, String> STRING_EXCHANGE = new MessageExchange<String, String>() {
        @Override
        public String toString() {
            return "StringExchange(request=java.lang.String, response=java.lang.String)";
        }
    };

    @Test
    void createProtocol_passesTheServiceSchemaTraitWithTheFactoryId() {
        RecordingFactory factory = new RecordingFactory();
        ProtocolSettings settings = ProtocolSettings.builder().serviceSchema(SERVICE).build();

        ClientProtocol<?, ?> protocol = ClientProtocolSupport.createProtocol(factory, settings);

        assertThat(protocol).isSameAs(factory.created);
        assertThat(factory.trait.get()).isSameAs(CBOR_TRAIT);
        assertThat(factory.settings.get()).isSameAs(settings);
    }

    @Test
    void createProtocol_passesNullWhenTheServiceDoesNotDeclareTheProtocol() {
        RecordingFactory factory = new RecordingFactory();

        ClientProtocolSupport.createProtocol(factory, ProtocolSettings.builder()
                                                                      .serviceSchema(Schema.createService(SERVICE_ID))
                                                                      .build());
        assertThat(factory.trait.get()).isNull();

        ClientProtocolSupport.createProtocol(factory, ProtocolSettings.builder().service(SERVICE_ID).build());
        assertThat(factory.trait.get()).isNull();
    }

    @Test
    void createProtocol_rejectsAFactoryThatReturnsNull() {
        RecordingFactory factory = new RecordingFactory(null);

        assertThatThrownBy(() -> ClientProtocolSupport.createProtocol(factory, ProtocolSettings.builder()
                                                                                                .serviceSchema(SERVICE)
                                                                                                .build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("null protocol")
            .hasMessageContaining("demo#DemoService");
    }

    @Test
    void validateMessageExchange_withoutAProtocol_doesNothing() {
        assertThatCode(() -> ClientProtocolSupport.validateMessageExchange(config(ClientType.SYNC, null)))
            .doesNotThrowAnyException();
    }

    @Test
    void validateMessageExchange_httpProtocolAndDefaultTransport_passes() {
        ClientProtocol<?, ?> protocol = protocol(HttpMessageExchange.INSTANCE);

        assertThatCode(() -> ClientProtocolSupport.validateMessageExchange(config(ClientType.SYNC, protocol)))
            .doesNotThrowAnyException();
        assertThatCode(() -> ClientProtocolSupport.validateMessageExchange(config(ClientType.ASYNC, protocol)))
            .doesNotThrowAnyException();
    }

    @Test
    void validateMessageExchange_mismatchWithTheDefaultTransport_namesBothTypePairs() {
        ClientProtocol<?, ?> protocol = protocol(STRING_EXCHANGE);

        for (ClientType type : new ClientType[] {ClientType.SYNC, ClientType.ASYNC}) {
            assertThatThrownBy(() -> ClientProtocolSupport.validateMessageExchange(config(type, protocol)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("StringExchange(request=java.lang.String, response=java.lang.String)")
                .hasMessageContaining("request=software.amazon.awssdk.http.SdkHttpFullRequest")
                .hasMessageContaining("response=software.amazon.awssdk.http.SdkHttpFullResponse")
                .hasMessageContaining("the SDK HTTP client transport");
        }
    }

    @Test
    void validateMessageExchange_usesTheTransportOfTheClientType() {
        ClientProtocol<?, ?> protocol = protocol(STRING_EXCHANGE);
        SdkClientConfiguration sync = config(ClientType.SYNC, protocol).toBuilder()
                                                                       .option(SdkClientOption.CLIENT_TRANSPORT,
                                                                               new StringTransport())
                                                                       .build();
        assertThatCode(() -> ClientProtocolSupport.validateMessageExchange(sync)).doesNotThrowAnyException();

        // The async client ignores the sync transport and checks its own.
        SdkClientConfiguration asyncWithSyncTransport = sync.toBuilder()
                                                            .option(SdkClientOption.CLIENT_TYPE, ClientType.ASYNC)
                                                            .build();
        assertThatThrownBy(() -> ClientProtocolSupport.validateMessageExchange(asyncWithSyncTransport))
            .isInstanceOf(IllegalStateException.class);

        SdkClientConfiguration async = asyncWithSyncTransport.toBuilder()
                                                             .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT,
                                                                     new StringAsyncTransport())
                                                             .build();
        assertThatCode(() -> ClientProtocolSupport.validateMessageExchange(async)).doesNotThrowAnyException();
    }

    @Test
    void validateMessageExchange_mismatchWithAConfiguredTransport_namesTheTransport() {
        SdkClientConfiguration config = config(ClientType.SYNC, protocol(HttpMessageExchange.INSTANCE))
            .toBuilder()
            .option(SdkClientOption.CLIENT_TRANSPORT, new StringTransport())
            .build();

        assertThatThrownBy(() -> ClientProtocolSupport.validateMessageExchange(config))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("transport StringTransport exchanges StringExchange(request=java.lang.String, "
                                  + "response=java.lang.String)")
            .hasMessageContaining("request=software.amazon.awssdk.http.SdkHttpFullRequest");
    }

    private static SdkClientConfiguration config(ClientType type, ClientProtocol<?, ?> protocol) {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.CLIENT_TYPE, type)
                                     .option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                                     .build();
    }

    /**
     * A protocol that only reports its message exchange; any other call fails.
     */
    private static ClientProtocol<?, ?> protocol(MessageExchange<?, ?> exchange) {
        return (ClientProtocol<?, ?>) Proxy.newProxyInstance(
            ClientProtocolSupportTest.class.getClassLoader(), new Class<?>[] {ClientProtocol.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "messageExchange":
                        return exchange;
                    case "toString":
                        return "TestProtocol";
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    private static final class RecordingFactory implements ClientProtocolFactory<Rpcv2CborTrait> {
        private final ClientProtocol<?, ?> created;
        private final AtomicReference<Rpcv2CborTrait> trait = new AtomicReference<>();
        private final AtomicReference<ProtocolSettings> settings = new AtomicReference<>();

        private RecordingFactory() {
            this(protocol(HttpMessageExchange.INSTANCE));
        }

        private RecordingFactory(ClientProtocol<?, ?> created) {
            this.created = created;
        }

        @Override
        public ShapeId id() {
            return Rpcv2CborTrait.ID;
        }

        @Override
        public ClientProtocol<?, ?> createProtocol(ProtocolSettings settings, Rpcv2CborTrait protocolTrait) {
            this.settings.set(settings);
            this.trait.set(protocolTrait);
            return created;
        }
    }

    private static final class StringTransport implements ClientTransport<String, String> {
        @Override
        public MessageExchange<String, String> messageExchange() {
            return STRING_EXCHANGE;
        }

        @Override
        public String send(ExecutionAttributes context, String request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return "StringTransport";
        }
    }

    private static final class StringAsyncTransport implements AsyncClientTransport<String, String> {
        @Override
        public MessageExchange<String, String> messageExchange() {
            return STRING_EXCHANGE;
        }

        @Override
        public CompletableFuture<String> send(ExecutionAttributes context, String request,
                                              AsyncResponseHandoff<String> streamingHandoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
