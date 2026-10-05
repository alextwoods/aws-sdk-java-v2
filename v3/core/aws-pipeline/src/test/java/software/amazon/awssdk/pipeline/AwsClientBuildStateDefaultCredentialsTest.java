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

package software.amazon.awssdk.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.pipeline.client.AwsClientParts;
import software.amazon.awssdk.pipeline.client.AwsServiceClientDefinition;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/** The default credentials factory of {@link AwsServiceClientDefinition}, as {@link AwsClientBuildState} uses it. */
class AwsClientBuildStateDefaultCredentialsTest {
    private static final ShapeId AWS_JSON = ShapeId.from("aws.protocols#awsJson1_0");
    private static final ServiceProtocols PROTOCOLS =
        ServiceProtocols.builder(Schema.createService(PipelineFixtures.SERVICE))
                        .protocol(ModeledProtocol.of(AWS_JSON))
                        .build();

    @Test
    void withoutAnExplicitProviderTheFactoryProviderIsUsedAndClosedWithTheClient() {
        RecordingFactory factory = new RecordingFactory();
        AwsClientBuildState state = state(true);
        ConfigurationSources sources = ConfigurationSources.builder()
                                                           .jvmProperties(SettingSource.empty())
                                                           .build();
        state.configurationSources(sources);

        AwsClientParts parts = state.buildSync(definition().withDefaultCredentials(factory));
        assertEquals(List.of(sources), factory.sources, "called once, with the client's sources");
        FakeCredentials created = factory.created.get(0);
        assertSame(created, parts.configuration().get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).orElseThrow());
        assertFalse(created.closed);
        parts.close();
        assertTrue(created.closed, "the SDK-owned default provider is closed with the client");
    }

    @Test
    void anExplicitProviderSkipsTheFactory() {
        RecordingFactory factory = new RecordingFactory();
        FakeCredentials explicit = new FakeCredentials();
        AwsClientBuildState state = state(true);
        state.credentialsProvider(explicit);

        try (AwsClientParts parts = state.buildSync(definition().withDefaultCredentials(factory))) {
            assertTrue(factory.created.isEmpty());
            assertSame(explicit, parts.configuration().get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)
                                      .orElseThrow());
        }
        assertFalse(explicit.closed, "a caller-owned provider is not closed by the client");
    }

    @Test
    void aFailedBuildClosesTheDefaultProvider() {
        RecordingFactory factory = new RecordingFactory();
        // The provider has no request/response runtime, so the build fails after the credentials are created.
        AwsClientBuildState state = state(false);

        assertThrows(UnsupportedOperationException.class,
                     () -> state.buildSync(definition().withDefaultCredentials(factory)));
        assertEquals(1, factory.created.size());
        assertTrue(factory.created.get(0).closed, "a failed build closes the SDK-owned default provider");
    }

    @Test
    void aDefinitionWithoutAFactoryHasNoDefaultProvider() {
        try (AwsClientParts parts = state(true).buildSync(definition())) {
            assertFalse(parts.configuration().get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).isPresent());
        }
    }

    private static AwsServiceClientDefinition definition() {
        return new AwsServiceClientDefinition(PipelineFixtures.SERVICE, "things", PROTOCOLS,
                                              SchemaTypeRegistry.builder().build(), null);
    }

    private static AwsClientBuildState state(boolean withRuntime) {
        AwsClientBuildState state = new AwsClientBuildState();
        state.httpClient(new UnusedHttp());
        state.configurationSources(ConfigurationSources.none());
        state.protocolProvider(new ProtocolProvider() {
            @Override
            public ShapeId protocolId() {
                return AWS_JSON;
            }

            @Override
            public boolean usesHttpBindingTraits() {
                return false;
            }

            @Override
            public ClientProtocol createClientProtocol(ProtocolContext context) {
                if (!withRuntime) {
                    return ProtocolProvider.super.createClientProtocol(context);
                }
                return new PipelineFixtures.CountingProtocol();
            }
        });
        return state;
    }

    private static final class RecordingFactory implements Function<ConfigurationSources, FakeCredentials> {
        final List<ConfigurationSources> sources = new ArrayList<>();
        final List<FakeCredentials> created = new ArrayList<>();

        @Override
        public FakeCredentials apply(ConfigurationSources configurationSources) {
            sources.add(configurationSources);
            FakeCredentials credentials = new FakeCredentials();
            created.add(credentials);
            return credentials;
        }
    }

    private static final class FakeCredentials implements IdentityProvider<AwsCredentialsIdentity>, AutoCloseable {
        boolean closed;

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("akid", "secret"));
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** A caller-owned sync transport the builds resolve but never call. */
    private static final class UnusedHttp implements SdkSyncHttpClient {
        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "UnusedHttp";
        }

        @Override
        public void close() {
        }
    }
}
