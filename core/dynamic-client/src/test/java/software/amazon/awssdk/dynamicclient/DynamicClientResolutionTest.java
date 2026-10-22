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

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.dynamicclient.internal.ModelAuthSchemeResolver;
import software.amazon.awssdk.dynamicclient.internal.RulesEndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.smithy.model.Model;

/**
 * The protocol, transport, auth and endpoint resolution of {@link DynamicClient.Builder#build()} (Requirement 27).
 */
class DynamicClientResolutionTest {
    private static final Model AUTH_MODEL = assemble("/software/amazon/awssdk/dynamicclient/internal/auth.smithy");
    private static final Model NO_PROTOCOL_MODEL = assemble("/software/amazon/awssdk/dynamicclient/no-protocol.smithy");

    @Test
    void build_withoutAConfiguredProtocol_selectsTheProtocolOfTheModel() {
        try (DynamicClient restJson = builder().build();
             DynamicClient awsJson = builder().model(DynamicTestModels.twoServices())
                                              .serviceId(DynamicTestModels.ALPHA)
                                              .build()) {
            assertThat(restJson.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
                .isInstanceOf(RestJsonClientProtocol.class);
            assertThat(restJson.asyncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
                .isInstanceOf(RestJsonClientProtocol.class);
            assertThat(awsJson.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
                .isInstanceOf(AwsJson1Protocol.class);
        }
    }

    @Test
    void build_serviceWithoutProtocolTraits_failsSayingNoneWereFoundAndClosesWhatItCreated() {
        StubHttpClients.SyncBuilder sync = new StubHttpClients.SyncBuilder();
        StubHttpClients.AsyncBuilder async = new StubHttpClients.AsyncBuilder();
        DynamicClient.Builder builder = DynamicClient.builder()
                                                   .model(NO_PROTOCOL_MODEL)
                                                   .region(Region.US_WEST_2)
                                                   .credentialsProvider(TestCredentials.PROVIDER)
                                                   .httpClientBuilder(sync)
                                                   .httpClientBuilder(async);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Service test.noprotocol#NoProtocolService declares no protocol that the dynamic "
                                    + "client supports. Protocol traits on the service: none found.");
        assertThat(sync.client.closed.get()).isEqualTo(1);
    }

    @Test
    void build_serviceWithoutProtocolTraitsAndAConfiguredProtocol_usesTheConfiguredProtocol() {
        StubClientProtocol protocol = StubClientProtocol.http();

        try (DynamicClient client = builder().model(NO_PROTOCOL_MODEL).protocol(protocol).build()) {
            assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL)).isSameAs(protocol);
        }
    }

    @Test
    void build_transportOfAnotherMessageExchange_failsNamingTheProtocolAndTheTransport() {
        SdkPlugin otherTransport = config -> {
            SdkClientConfiguration.Builder options = CallTestClients.clientOptions(config);
            options.option(SdkClientOption.CLIENT_TRANSPORT, new OtherSyncTransport());
            options.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, new OtherAsyncTransport());
        };

        assertThatThrownBy(() -> builder().addPlugin(otherTransport).build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("The client protocol and the client transport exchange different message types")
            .hasMessageContaining("RestJsonClientProtocol")
            .hasMessageContaining("OtherSyncTransport")
            .hasMessageContaining("exchanges OtherExchange");
    }

    @Test
    void build_withoutAConfiguredEndpointProvider_evaluatesTheEndpointRulesOfTheModel() {
        try (DynamicClient weather = builder().build();
             DynamicClient account = builder().model(DynamicTestModels.accountPrototype()).build()) {
            RulesEndpointProvider noRules =
                (RulesEndpointProvider) weather.syncClientConfiguration().option(SdkClientOption.ENDPOINT_PROVIDER);
            RulesEndpointProvider rules =
                (RulesEndpointProvider) account.asyncClientConfiguration().option(SdkClientOption.ENDPOINT_PROVIDER);

            assertThat(noRules.hasRules()).isFalse();
            assertThat(rules.hasRules()).isTrue();
            assertThat(account.syncClientConfiguration().option(SdkClientOption.ENDPOINT_PROVIDER)).isSameAs(rules);
        }
    }

    @Test
    void build_withoutAConfiguredAuthSchemeResolver_resolvesTheAuthSchemesOfTheModel() {
        try (DynamicClient client = builder().model(AUTH_MODEL).build()) {
            for (SdkClientConfiguration config : new SdkClientConfiguration[] {client.syncClientConfiguration(),
                                                                                 client.asyncClientConfiguration()}) {
                assertThat(config.option(SdkClientOption.AUTH_SCHEME_PROVIDER)).isInstanceOf(ModelAuthSchemeResolver.class);
                assertThat(config.option(SdkClientOption.AUTH_SCHEMES))
                    .containsOnlyKeys("aws.auth#sigv4", "smithy.api#httpBearerAuth", "smithy.api#noAuth");
            }
        }
    }

    @Test
    void build_authSchemesWithoutAnImplementation_logOneWarningPerScheme() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             DynamicClient client = builder().model(AUTH_MODEL).build()) {
            List<String> warnings = warnings(logCaptor);

            assertThat(warnings).hasSize(2);
            assertThat(warnings.get(0))
                .isEqualTo("The auth scheme aws.auth#sigv4a of service test.auth#AuthService has no implementation, so "
                           + "this dynamic client does not use it. Every operation that lists it has another auth scheme. "
                           + "To use it, add the software.amazon.awssdk:http-auth-aws-crt dependency, or register an "
                           + "implementation with DynamicClient.Builder.putAuthScheme.");
            assertThat(warnings.get(1))
                .isEqualTo("The auth scheme smithy.api#httpBasicAuth of service test.auth#AuthService has no "
                           + "implementation, so this dynamic client does not use it. Calls to these operations fail "
                           + "because they have no other auth scheme: [BasicOnly]. To use it, register an "
                           + "implementation with DynamicClient.Builder.putAuthScheme.");
        }
    }

    @Test
    void build_authSchemesAddedByTheBuilderOrAPlugin_areNotReportedMissing() {
        SdkPlugin addsBasic = config -> ((DynamicServiceClientConfiguration.Builder) config)
            .putAuthScheme(new TestAuthScheme("smithy.api#httpBasicAuth"));

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             DynamicClient fromBuilder = builder().model(AUTH_MODEL)
                                                  .putAuthScheme(new TestAuthScheme("aws.auth#sigv4a"))
                                                  .putAuthScheme(new TestAuthScheme("smithy.api#httpBasicAuth"))
                                                  .build();
             DynamicClient fromPlugin = builder().model(AUTH_MODEL)
                                                 .putAuthScheme(new TestAuthScheme("aws.auth#sigv4a"))
                                                 .addPlugin(addsBasic)
                                                 .build()) {
            ModelAuthSchemeResolver resolver = (ModelAuthSchemeResolver) fromPlugin.syncClientConfiguration()
                                                                                   .option(SdkClientOption.AUTH_SCHEME_PROVIDER);

            assertThat(warnings(logCaptor)).isEmpty();
            assertThat(resolver.implementedSchemes()).contains("smithy.api#httpBasicAuth");
        }
    }

    @Test
    void build_withAConfiguredAuthSchemeResolver_logsNoWarning() {
        DynamicAuthSchemeResolver resolver = params -> Collections.emptyList();

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             DynamicClient client = builder().model(AUTH_MODEL).authSchemeResolver(resolver).build()) {
            assertThat(warnings(logCaptor)).isEmpty();
            assertThat(client.syncClientConfiguration().option(SdkClientOption.AUTH_SCHEME_PROVIDER)).isSameAs(resolver);
        }
    }

    private static List<String> warnings(LogCaptor logCaptor) {
        return logCaptor.loggedEvents()
                        .stream()
                        .filter(e -> e.getLevel() == Level.WARN)
                        .filter(e -> e.getLoggerName().equals(DynamicClient.class.getName()))
                        .map(LogEvent::getMessage)
                        .map(m -> m.getFormattedMessage())
                        .collect(Collectors.toList());
    }

    private static Model assemble(String resource) {
        URL url = DynamicClientResolutionTest.class.getResource(resource);
        return Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(url).assemble().unwrap();
    }

    private static DynamicClient.Builder builder() {
        return DynamicClient.builder()
                            .model(DynamicTestModels.weather())
                            .region(Region.US_WEST_2)
                            .credentialsProvider(TestCredentials.PROVIDER)
                            .httpClient(new StubHttpClients.Sync())
                            .httpClient(new StubHttpClients.Async());
    }

    private static final class OtherSyncTransport implements ClientTransport<Object, Object> {
        @Override
        public MessageExchange<Object, Object> messageExchange() {
            return StubClientProtocol.OTHER_EXCHANGE;
        }

        @Override
        public Object send(ExecutionAttributes context, Object request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return "OtherSyncTransport";
        }
    }

    private static final class OtherAsyncTransport implements AsyncClientTransport<Object, Object> {
        @Override
        public MessageExchange<Object, Object> messageExchange() {
            return StubClientProtocol.OTHER_EXCHANGE;
        }

        @Override
        public CompletableFuture<Object> send(ExecutionAttributes context, Object request,
                                              AsyncResponseHandoff<Object> streamingHandoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return "OtherAsyncTransport";
        }
    }

    private static final class TestAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
        private final String schemeId;

        private TestAuthScheme(String schemeId) {
            this.schemeId = schemeId;
        }

        @Override
        public String schemeId() {
            return schemeId;
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
