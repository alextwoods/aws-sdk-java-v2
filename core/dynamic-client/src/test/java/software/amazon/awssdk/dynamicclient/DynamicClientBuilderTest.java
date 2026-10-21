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

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.regions.Region;

/**
 * Requirement 26.5: the dynamic client builder takes the options of a generated client builder, and resolves an
 * option that is not set to the default a generated builder resolves. Also the builder's own options, the names the
 * configuration derives from the model, builder immutability and {@link DynamicClient#close()}.
 */
class DynamicClientBuilderTest {

    @Test
    void region_configured_isTheRegionOfBothConfigurations() {
        try (DynamicClient client = builder().region(Region.EU_WEST_1).build()) {
            assertThat(client.syncClientConfiguration().option(AwsClientOption.AWS_REGION)).isEqualTo(Region.EU_WEST_1);
            assertThat(client.asyncClientConfiguration().option(AwsClientOption.AWS_REGION)).isEqualTo(Region.EU_WEST_1);
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.EU_WEST_1);
        }
    }

    @Test
    void region_notConfigured_resolvesFromTheDefaultRegionChain() {
        String previous = System.getProperty("aws.region");
        System.setProperty("aws.region", "ap-southeast-2");
        try (DynamicClient client = builder().region(null).build()) {
            assertThat(client.syncClientConfiguration().option(AwsClientOption.AWS_REGION))
                .isEqualTo(Region.AP_SOUTHEAST_2);
        } finally {
            restoreProperty("aws.region", previous);
        }
    }

    @Test
    void credentialsProvider_configured_isTheProviderOfBothConfigurations() {
        try (DynamicClient client = builder().build()) {
            assertThat(client.syncClientConfiguration().option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER))
                .isSameAs(TestCredentials.PROVIDER);
            assertThat(client.asyncClientConfiguration().option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER))
                .isSameAs(TestCredentials.PROVIDER);
            assertThat(client.serviceClientConfiguration().credentialsProvider()).isSameAs(TestCredentials.PROVIDER);
        }
    }

    @Test
    void credentialsProvider_notConfigured_isTheDefaultCredentialsProvider() {
        try (DynamicClient client = builder().credentialsProvider((IdentityProvider<AwsCredentialsIdentity>) null).build()) {
            assertThat(client.syncClientConfiguration().option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)
                             .getClass().getSimpleName())
                .isEqualTo("DefaultCredentialsProvider");
        }
    }

    @Test
    void httpClient_configured_isUsedAndNotClosedByTheClient() {
        StubHttpClients.Sync sync = new StubHttpClients.Sync();
        StubHttpClients.Async async = new StubHttpClients.Async();
        DynamicClient client = builder().httpClient(sync).httpClient(async).build();

        assertThat(client.syncClientConfiguration().option(SdkClientOption.SYNC_HTTP_CLIENT).clientName())
            .isEqualTo("StubSync");
        assertThat(client.asyncClientConfiguration().option(SdkClientOption.ASYNC_HTTP_CLIENT).clientName())
            .isEqualTo("StubAsync");
        client.close();
        assertThat(sync.closed).hasValue(0);
        assertThat(async.closed).hasValue(0);
    }

    @Test
    void httpClientBuilder_configured_createsClientsThatTheClientCloses() {
        StubHttpClients.SyncBuilder sync = new StubHttpClients.SyncBuilder();
        StubHttpClients.AsyncBuilder async = new StubHttpClients.AsyncBuilder();
        DynamicClient client = builder().httpClient((SdkHttpClient) null)
                                        .httpClient((SdkAsyncHttpClient) null)
                                        .httpClientBuilder(sync)
                                        .httpClientBuilder(async)
                                        .build();

        assertThat(client.syncClientConfiguration().option(SdkClientOption.SYNC_HTTP_CLIENT)).isSameAs(sync.client);
        assertThat(client.asyncClientConfiguration().option(SdkClientOption.ASYNC_HTTP_CLIENT)).isSameAs(async.client);
        client.close();
        assertThat(sync.client.closed).hasValue(1);
        assertThat(async.client.closed).hasValue(1);
    }

    @Test
    void httpClient_notConfigured_resolvesTheDefaultSyncAndAsyncClients() {
        try (DynamicClient client = DynamicClient.builder()
                                                 .model(DynamicTestModels.weather())
                                                 .region(Region.US_WEST_2)
                                                 .credentialsProvider(TestCredentials.PROVIDER)
                                                 .build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.SYNC_HTTP_CLIENT)).isNotNull();
            assertThat(client.asyncClientConfiguration().option(SdkClientOption.ASYNC_HTTP_CLIENT)).isNotNull();
        }
    }

    @Test
    void build_resolvesASyncAndAnAsyncConfiguration() {
        try (DynamicClient client = builder().build()) {
            SdkClientConfiguration sync = client.syncClientConfiguration();
            SdkClientConfiguration async = client.asyncClientConfiguration();

            assertThat(sync.option(SdkClientOption.CLIENT_TYPE)).isEqualTo(ClientType.SYNC);
            assertThat(async.option(SdkClientOption.CLIENT_TYPE)).isEqualTo(ClientType.ASYNC);
            assertThat(sync.option(SdkClientOption.SYNC_HTTP_CLIENT)).isNotNull();
            assertThat(async.option(SdkClientOption.ASYNC_HTTP_CLIENT)).isNotNull();
            assertThat(async.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR)).isNotNull();
            assertThat(sync.option(SdkClientOption.RETRY_STRATEGY)).isNotNull();
            assertThat(async.option(SdkClientOption.RETRY_STRATEGY)).isNotNull();
        }
    }

    @Test
    void protocol_configured_isTheProtocolOfBothConfigurations() {
        StubClientProtocol protocol = StubClientProtocol.http();
        try (DynamicClient client = builder().protocol(protocol).build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL)).isSameAs(protocol);
            assertThat(client.asyncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL)).isSameAs(protocol);
        }
    }

    @Test
    void protocol_withAnotherMessageExchange_failsTheBuildAndClosesWhatItCreated() {
        StubHttpClients.SyncBuilder sync = new StubHttpClients.SyncBuilder();
        DynamicClient.Builder builder = builder().httpClient((SdkHttpClient) null)
                                                 .httpClientBuilder(sync)
                                                 .protocol(StubClientProtocol.otherExchange());

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("The client protocol and the client transport exchange different message types")
            .hasMessageContaining("OtherExchange")
            .hasMessageContaining("HttpMessageExchange");
        assertThat(sync.client.closed).hasValue(1);
    }

    @Test
    void authSchemeResolver_configured_isInTheConfiguration() {
        DynamicAuthSchemeResolver resolver = params -> Collections.emptyList();
        try (DynamicClient client = builder().authSchemeResolver(resolver).build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.AUTH_SCHEME_PROVIDER)).isSameAs(resolver);
            assertThat(client.asyncClientConfiguration().option(SdkClientOption.AUTH_SCHEME_PROVIDER)).isSameAs(resolver);
            assertThat(client.serviceClientConfiguration().authSchemeResolver()).isSameAs(resolver);
        }
    }

    @Test
    void endpointProvider_configured_isInTheConfiguration() {
        DynamicEndpointProvider provider =
            params -> CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("https://example.com")).build());
        try (DynamicClient client = builder().endpointProvider(provider).build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.ENDPOINT_PROVIDER)).isSameAs(provider);
            assertThat(client.asyncClientConfiguration().option(SdkClientOption.ENDPOINT_PROVIDER)).isSameAs(provider);
            assertThat(client.serviceClientConfiguration().endpointProvider()).containsSame(provider);
        }
    }

    @Test
    void endpointOverride_configured_isTheClientEndpoint() {
        URI endpoint = URI.create("https://localhost:8443");
        try (DynamicClient client = builder().endpointOverride(endpoint).build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())
                .isEqualTo(endpoint);
            assertThat(client.serviceClientConfiguration().endpointOverride()).contains(endpoint);
        }
    }

    @Test
    void endpointOverride_notConfigured_isTheRegionalEndpointOfTheEndpointPrefix() {
        try (DynamicClient client = builder().build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())
                .isEqualTo(URI.create("https://forecast.us-west-2.amazonaws.com"));
            assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
        }
    }

    @Test
    void endpointOverride_fromTheServiceSystemProperty_isTheClientEndpoint() {
        String property = "aws.endpointUrlWeatherForecast";
        String previous = System.getProperty(property);
        System.setProperty(property, "https://weather.example.com");
        try (DynamicClient client = builder().build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())
                .isEqualTo(URI.create("https://weather.example.com"));
        } finally {
            restoreProperty(property, previous);
        }
    }

    @Test
    void overrideConfiguration_configured_appliesToBothConfigurations() {
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
        };
        try (DynamicClient client = builder().overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(7))
                                                                          .addExecutionInterceptor(interceptor))
                                             .build()) {
            for (SdkClientConfiguration config : configurations(client)) {
                assertThat(config.option(SdkClientOption.API_CALL_TIMEOUT)).isEqualTo(Duration.ofSeconds(7));
                assertThat(config.option(SdkClientOption.EXECUTION_INTERCEPTORS)).contains(interceptor);
            }
            assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallTimeout())
                .contains(Duration.ofSeconds(7));
        }
    }

    @Test
    void addPlugin_runsOnBothConfigurationsWithADynamicServiceClientConfigurationBuilder() {
        List<SdkServiceClientConfiguration.Builder> seen = new ArrayList<>();
        SdkPlugin plugin = config -> {
            seen.add(config);
            ((DynamicServiceClientConfiguration.Builder) config).region(Region.CA_CENTRAL_1)
                                                                .overrideConfiguration(o -> o.apiCallAttemptTimeout(
                                                                    Duration.ofSeconds(3)));
        };
        try (DynamicClient client = builder().addPlugin(plugin).build()) {
            assertThat(seen).hasSize(2).allMatch(c -> c instanceof DynamicServiceClientConfiguration.Builder);
            for (SdkClientConfiguration config : configurations(client)) {
                assertThat(config.option(AwsClientOption.AWS_REGION)).isEqualTo(Region.CA_CENTRAL_1);
                assertThat(config.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT)).isEqualTo(Duration.ofSeconds(3));
            }
        }
    }

    @Test
    void putAuthScheme_configured_isInTheAuthSchemes() {
        AuthScheme<AwsCredentialsIdentity> scheme = new TestAuthScheme();
        try (DynamicClient client = builder().putAuthScheme(scheme).build()) {
            for (SdkClientConfiguration config : configurations(client)) {
                assertThat(config.option(SdkClientOption.AUTH_SCHEMES)).containsEntry("test.auth#scheme", scheme);
            }
            assertThat(client.serviceClientConfiguration().authSchemes()).containsEntry("test.auth#scheme", scheme);
        }
    }

    @Test
    void build_derivesTheServiceNamesFromTheModel() {
        try (DynamicClient client = builder().build()) {
            for (SdkClientConfiguration config : configurations(client)) {
                assertThat(config.option(SdkClientOption.SERVICE_NAME)).isEqualTo("WeatherForecast");
                assertThat(config.option(AwsClientOption.SERVICE_SIGNING_NAME)).isEqualTo("weather-signing");
                assertThat(config.option(AwsClientOption.ENDPOINT_PREFIX)).isEqualTo("forecast");
            }
            assertThat(client.serviceName()).isEqualTo("weather-signing");
        }
    }

    @Test
    void serviceName_withoutASigningName_isTheEndpointPrefix() {
        try (DynamicClient client = builder().model(DynamicTestModels.twoServices())
                                             .serviceId(DynamicTestModels.ALPHA)
                                             .build()) {
            assertThat(client.serviceName()).isEqualTo("alphaservice");
            assertThat(client.syncClientConfiguration().option(SdkClientOption.SERVICE_NAME)).isEqualTo("Alpha");
            assertThat(client.syncClientConfiguration().option(AwsClientOption.SERVICE_SIGNING_NAME)).isEmpty();
        }
    }

    @Test
    void builder_returnsTheModelAndServiceIdItWasGiven() {
        DynamicClient.Builder builder = DynamicClient.builder();
        assertThat(builder.model()).isNull();
        assertThat(builder.serviceId()).isNull();

        builder.model(DynamicTestModels.twoServices()).serviceId(DynamicTestModels.BETA);

        assertThat(builder.model()).isSameAs(DynamicTestModels.twoServices());
        assertThat(builder.serviceId()).isEqualTo(DynamicTestModels.BETA);
    }

    @Test
    void builder_changedAfterBuild_doesNotChangeTheBuiltClient() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.twoServices())
                                                 .serviceId(DynamicTestModels.ALPHA)
                                                 .region(Region.US_EAST_1);
        try (DynamicClient first = builder.build()) {
            builder.serviceId(DynamicTestModels.BETA)
                   .region(Region.EU_WEST_2)
                   .protocol(StubClientProtocol.http())
                   .overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(1)));
            try (DynamicClient second = builder.build()) {
                assertThat(first.service().schema().id().toString()).isEqualTo("test.multi#AlphaService");
                assertThat(first.syncClientConfiguration().option(AwsClientOption.AWS_REGION)).isEqualTo(Region.US_EAST_1);
                assertThat(first.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
                    .isInstanceOf(AwsJson1Protocol.class);
                assertThat(first.syncClientConfiguration().option(SdkClientOption.API_CALL_TIMEOUT)).isNull();

                assertThat(second.service().schema().id().toString()).isEqualTo("test.multi#BetaService");
                assertThat(second.syncClientConfiguration().option(AwsClientOption.AWS_REGION))
                    .isEqualTo(Region.EU_WEST_2);
                assertThat(second.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
                    .isInstanceOf(StubClientProtocol.class);
                assertThat(second.schemaConverter()).isNotSameAs(first.schemaConverter());
            }
        }
    }

    @Test
    void build_afterAFailedBuild_succeedsOnceTheBuilderIsFixed() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.twoServices());
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);

        try (DynamicClient client = builder.serviceId(DynamicTestModels.ALPHA).build()) {
            assertThat(client.serviceShape().getId()).isEqualTo(DynamicTestModels.ALPHA);
        }
    }

    private static List<SdkClientConfiguration> configurations(DynamicClient client) {
        List<SdkClientConfiguration> result = new ArrayList<>();
        result.add(client.syncClientConfiguration());
        result.add(client.asyncClientConfiguration());
        return result;
    }

    private static void restoreProperty(String name, String previous) {
        if (previous == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previous);
        }
    }

    private static DynamicClient.Builder builder() {
        return DynamicClient.builder()
                            .model(DynamicTestModels.weather())
                            .region(Region.US_WEST_2)
                            .credentialsProvider(TestCredentials.PROVIDER)
                            .httpClient(new StubHttpClients.Sync())
                            .httpClient(new StubHttpClients.Async());
    }

    private static final class TestAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
        @Override
        public String schemeId() {
            return "test.auth#scheme";
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            throw new UnsupportedOperationException();
        }
    }
}
