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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.api.SignerProperty;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;

/** {@link AwsCallEngine#presign}: the shared stages up to signing, then no transmission (Requirements 7.10, 18.3). */
class AwsCallEnginePresignTest {
    private static final String SCHEME_ID = "test#presignable";
    private static final SignerProperty<String> PLACEMENT =
        SignerProperty.create(AwsCallEnginePresignTest.class, "Placement");
    private static final Instant NOW = Instant.parse("2024-05-01T10:00:00Z");
    private static final PresignSpec PRESIGN = new PresignSpec(
        Duration.ofMinutes(10),
        option -> option.toBuilder()
                        .putSignerProperty(PLACEMENT, "query")
                        .putSignerProperty(HttpSigner.SIGNING_CLOCK, Clock.fixed(NOW, ZoneOffset.UTC))
                        .build(),
        // The test signer records its expiration in X-Expires-At.
        signed -> signed.queryParameters().stream().filter(p -> p.rawName().equals("X-Expires-At"))
                        .map(p -> Instant.parse(p.rawValue())).findFirst().orElseThrow());
    private static final AwsOperation<Struct, Struct> SIGNED = PipelineFixtures.operation(
        "com.example#Signed", GET_INPUT, GET_OUTPUT,
        ModeledAuth.builder().operationId("com.example#Signed").addServiceScheme(SCHEME_ID).build());

    private final List<PipelineStage> stages = new ArrayList<>();

    @Test
    void presign_signingOnlyEngine_runsTheSharedStagesUpToSigningAndNeverTransmits() {
        AwsCallEngine engine = signingOnly(runtime(new QuerySigningScheme()));

        PresignedHttpRequest presigned = engine.presign(engine.runtime().plan(SIGNED),
                                                        PipelineFixtures.request(GET_INPUT, "a"), null, PRESIGN);

        assertThat(presigned.url().toString())
            .startsWith("https://things.example.com")
            .contains("X-Placement=query", "X-Signature=sig");
        assertThat(presigned.method()).isEqualTo("POST");
        assertThat(presigned.signedHeaders()).containsEntry("x-name", List.of("a"));
        assertThat(presigned.expiration()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.toString()).doesNotContain("X-Signature");
        assertThat(stages).containsSubsequence(PipelineStage.VALIDATE, PipelineStage.ACQUIRE_RETRY_TOKEN,
                                               PipelineStage.RESOLVE_ENDPOINT, PipelineStage.SELECT_AUTH_SCHEME,
                                               PipelineStage.RESOLVE_IDENTITY, PipelineStage.SERIALIZE,
                                               PipelineStage.SIGN)
                          .doesNotContain(PipelineStage.TRANSMIT, PipelineStage.DESERIALIZE,
                                          PipelineStage.CLASSIFY_RETRY, PipelineStage.MAP_EXCEPTION);
    }

    @Test
    void presign_engineWithTransport_invokesTheTransportZeroTimes() {
        CountingHttp http = new CountingHttp();
        AwsCallEngine engine = AwsCallEngine.builder().runtime(runtime(new QuerySigningScheme()))
                                            .syncHttpClient(http).build();

        PresignedHttpRequest presigned = engine.presign(engine.runtime().plan(SIGNED),
                                                        PipelineFixtures.request(GET_INPUT, "a"), null, PRESIGN);

        assertThat(presigned.url().getQuery()).contains("X-Signature=sig");
        assertThat(http.prepared).isZero();
    }

    @Test
    void presign_unsignedOperation_failsThroughTheMappingPath() {
        ModeledAuth open = ModeledAuth.builder().operationId("com.example#Open").build();
        AwsOperation<Struct, Struct> operation = PipelineFixtures.operation("com.example#Open", GET_INPUT, GET_OUTPUT,
                                                                            open);
        ClientRuntime runtime = PipelineFixtures.engineRuntime(retries(1), HttpTransportMode.SYNC)
                                                .authSchemeSelector(AuthSchemeSelector.create(
                                                    List.of(NoAuthAuthScheme.create())))
                                                .build();
        AwsCallEngine engine = signingOnly(runtime);

        assertThatThrownBy(() -> engine.presign(engine.runtime().plan(operation),
                                                PipelineFixtures.request(GET_INPUT, "a"), null, PRESIGN))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("cannot be presigned");
        assertThat(stages).contains(PipelineStage.MAP_EXCEPTION).doesNotContain(PipelineStage.TRANSMIT);
    }

    @Test
    void signingOnlyEngine_refusesToExecuteAndToTakeATransport() {
        AwsCallEngine engine = signingOnly(runtime(new QuerySigningScheme()));

        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(SIGNED),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("signing-only");
        assertThatThrownBy(() -> AwsCallEngine.builder().runtime(runtime(new QuerySigningScheme())).signingOnly()
                                              .syncHttpClient(new CountingHttp()).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void presignSpec_rejectsANonPositiveExpiration() {
        assertThatThrownBy(() -> new PresignSpec(Duration.ZERO, o -> o, r -> NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void presignedRequest_browserExecutable_onlyForAGetWithTheHostHeaderAlone() {
        SdkHttpRequest get = SdkHttpRequest.builder().method("GET").scheme("https").host("b.example.com")
                                           .encodedPath("/k").putHeader("Host", "b.example.com")
                                           .addQueryParameter("X-Amz-Signature", "s").build();
        PresignedHttpRequest browser = new PresignedHttpRequest(get, NOW);

        assertThat(browser.isBrowserExecutable()).isTrue();
        assertThat(browser.url().toString()).isEqualTo("https://b.example.com/k?X-Amz-Signature=s");
        assertThat(new PresignedHttpRequest(get.toBuilder().putHeader("X-Extra", "1").build(), NOW)
                       .isBrowserExecutable()).isFalse();
        assertThat(new PresignedHttpRequest(get.toBuilder().method("PUT").body(HttpBody.fromUtf8("x")).build(), NOW)
                       .isBrowserExecutable()).isFalse();
    }

    private AwsCallEngine signingOnly(ClientRuntime runtime) {
        return AwsCallEngine.builder().runtime(runtime).signingOnly()
                            .stageObserver((call, attempt, stage) -> stages.add(stage)).build();
    }

    private static ClientRuntime runtime(AuthScheme<Identity> scheme) {
        return PipelineFixtures.engineRuntime(retries(3), HttpTransportMode.SYNC)
                               .authSchemeSelector(AuthSchemeSelector.create(List.of(scheme)))
                               .build();
    }

    /** Signs into the query string where {@link #PLACEMENT} says so; checks the presign adjustment reached it. */
    private static final class QuerySigningScheme implements AuthScheme<Identity> {
        @Override
        public String schemeId() {
            return SCHEME_ID;
        }

        @Override
        public Class<Identity> identityType() {
            return Identity.class;
        }

        @Override
        public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
            return Optional.of(new IdentityProvider<>() {
                @Override
                public Class<Identity> identityType() {
                    return Identity.class;
                }

                @Override
                public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
                    return CompletableFuture.completedFuture(new Identity() { });
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return request -> new SignedRequest(
                request.request().toBuilder()
                       .addQueryParameter("X-Placement", request.requireProperty(PLACEMENT, "header"))
                       .addQueryParameter("X-Signature", "sig")
                       .addRawQueryParameter("X-Expires-At", request.requireProperty(HttpSigner.SIGNING_CLOCK)
                                                                    .instant().plus(Duration.ofMinutes(10))
                                                                    .toString())
                       .build(),
                "sig");
        }
    }

    private static final class CountingHttp implements SdkSyncHttpClient {
        private int prepared;

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            prepared++;
            throw new AssertionError("A presign call must not transmit");
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "CountingHttp";
        }

        @Override
        public void close() {
        }
    }
}
