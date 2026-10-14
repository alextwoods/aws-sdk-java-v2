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

package software.amazon.awssdk.services.cloudwatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;

/**
 * Runtime protocol selection of the generated CloudWatch builders (task 3.23, Requirements 12.1, 12.3, 12.5, 13.3).
 * CloudWatch declares {@code rpcv2Cbor}, {@code awsJson1_0} and {@code awsQuery}, and defaults to {@code rpcv2Cbor}.
 */
class CloudWatchProtocolSelectionTest {
    private static final String CLOUDWATCH = "com.amazonaws.cloudwatch#GraniteServiceVersion20100801";

    private static final MessageExchange<String, String> STRING_EXCHANGE = new MessageExchange<String, String>() {
        @Override
        public String toString() {
            return "StringExchange(request=java.lang.String, response=java.lang.String)";
        }
    };

    @Test
    void defaultProtocol_isRpcv2CborForTheCloudWatchService() {
        try (CloudWatchClient client = configure(CloudWatchClient.builder()).httpClient(new NoOpHttpClient()).build();
             CloudWatchAsyncClient asyncClient = configure(CloudWatchAsyncClient.builder())
                 .httpClient(new NoOpAsyncHttpClient()).build()) {
            for (SdkClient c : new SdkClient[] {client, asyncClient}) {
                SdkClientConfiguration config = configuration(c);
                assertThat(config.option(SdkClientOption.CLIENT_PROTOCOL_FACTORY))
                    .isSameAs(RpcV2CborProtocol.Factory.INSTANCE);
                ClientProtocol<?, ?> protocol = config.option(SdkClientOption.CLIENT_PROTOCOL);
                assertThat(protocol).isInstanceOf(RpcV2CborProtocol.class);
                assertThat(protocol.id()).isEqualTo(Rpcv2CborTrait.ID);
                assertThat(protocol.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
                assertThat(protocol).hasToString("RpcV2CborProtocol(" + CLOUDWATCH + ")");
            }
        }
    }

    @Test
    void protocolOverride_isUsedInsteadOfTheDefault() {
        ClientProtocol<?, ?> override = protocol(HttpMessageExchange.INSTANCE);
        try (CloudWatchClient client = configure(CloudWatchClient.builder()).httpClient(new NoOpHttpClient())
                                                                             .protocol(override)
                                                                             .build();
             CloudWatchAsyncClient asyncClient = configure(CloudWatchAsyncClient.builder())
                 .httpClient(new NoOpAsyncHttpClient()).protocol(override).build()) {
            assertThat(configuration(client).option(SdkClientOption.CLIENT_PROTOCOL)).isSameAs(override);
            assertThat(configuration(asyncClient).option(SdkClientOption.CLIENT_PROTOCOL)).isSameAs(override);
        }
    }

    @Test
    void nullProtocolOverride_meansNoOverride() {
        try (CloudWatchClient client = configure(CloudWatchClient.builder()).httpClient(new NoOpHttpClient())
                                                                             .protocol(protocol(STRING_EXCHANGE))
                                                                             .protocol(null)
                                                                             .build()) {
            assertThat(configuration(client).option(SdkClientOption.CLIENT_PROTOCOL))
                .isInstanceOf(RpcV2CborProtocol.class);
        }
    }

    @Test
    void protocolWhoseMessageExchangeDiffersFromTheTransport_failsTheBuild() {
        ClientProtocol<?, ?> override = protocol(STRING_EXCHANGE);

        assertThatThrownBy(() -> configure(CloudWatchClient.builder()).httpClient(new NoOpHttpClient())
                                                                      .protocol(override)
                                                                      .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("StringExchange(request=java.lang.String, response=java.lang.String)")
            .hasMessageContaining("request=software.amazon.awssdk.http.SdkHttpFullRequest, "
                                  + "response=software.amazon.awssdk.http.SdkHttpFullResponse");
        assertThatThrownBy(() -> configure(CloudWatchAsyncClient.builder()).httpClient(new NoOpAsyncHttpClient())
                                                                           .protocol(override)
                                                                           .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("StringExchange(request=java.lang.String, response=java.lang.String)");
    }

    private static <B extends AwsClientBuilder<B, ?>> B configure(B builder) {
        return builder.region(Region.US_WEST_2)
                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")));
    }

    /**
     * The client configuration a generated client was built with.
     */
    private static SdkClientConfiguration configuration(SdkClient client) {
        try {
            Field field = client.getClass().getDeclaredField("clientConfiguration");
            field.setAccessible(true);
            return (SdkClientConfiguration) field.get(client);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A protocol that only reports its message exchange; it is never asked to serialize anything here.
     */
    private static ClientProtocol<?, ?> protocol(MessageExchange<?, ?> exchange) {
        return (ClientProtocol<?, ?>) Proxy.newProxyInstance(
            CloudWatchProtocolSelectionTest.class.getClassLoader(), new Class<?>[] {ClientProtocol.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "messageExchange":
                        return exchange;
                    case "toString":
                        return "TestProtocol";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    private static final class NoOpHttpClient implements SdkHttpClient {
        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }

    private static final class NoOpAsyncHttpClient implements SdkAsyncHttpClient {
        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
