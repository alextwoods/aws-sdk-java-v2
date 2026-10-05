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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/** The sync driver of {@link AwsCallEngine} over the shared stages, against a scripted transport and protocol. */
class AwsCallEngineSyncTest {
    private final FakeHttp http = new FakeHttp();

    @Test
    void executeSync_success_serializesTransmitsOnceAndDeserializes() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(200, "v1");

        Struct output = engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                                           null, null);

        assertThat((String) output.getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("v1");
        assertThat(http.transmitted).hasSize(1);
        SdkHttpRequest sent = http.transmitted.get(0);
        assertThat(sent.host()).isEqualTo("things.example.com");
        assertThat(sent.headers().firstValue("X-Name")).contains("a");
        assertThat(sent.body().asUtf8String()).isEqualTo("a");
        assertThat(http.closedExchanges).isEqualTo(1);
    }

    @Test
    void executeSync_retryableFailures_eachGrantedTokenAuthorizesOneMoreTransmission() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(503, null).respond(500, null).respond(200, "v3");

        Struct output = engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                                           null, null);

        assertThat((String) output.getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("v3");
        assertThat(http.transmitted).hasSize(3);
        assertThat(http.closedExchanges).isEqualTo(3);
    }

    @Test
    void executeSync_retriesExhausted_surfacesTheLastServiceFailure() {
        AwsCallEngine engine = engine(runtime(retries(2)).build());
        http.respond(503, null).respond(503, null).respond(200, "unused");

        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> assertThat(e.statusCode()).isEqualTo(503));
        assertThat(http.transmitted).hasSize(2);
        assertThat(http.closedExchanges).isEqualTo(2);
    }

    @Test
    void executeSync_nonRetryableServiceError_isSurfacedAfterOneTransmission() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(400, null).respond(200, "unused");

        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> {
                assertThat(e.statusCode()).isEqualTo(400);
                assertThat(e.awsErrorDetails().errorCode()).isEqualTo("Error400");
            });
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void executeSync_nonSdkFailure_takesTheOneMappingPathWithItsCause() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        IllegalStateException boom = new IllegalStateException("boom");
        http.fail(boom);

        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("com.example#GetThing")
            .hasCause(boom);
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void executeSync_idempotencyToken_isInjectedOnceAndStableAcrossRetries() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(503, null).respond(200, "v");

        engine.executeSync(engine.runtime().plan(PUT_THING), PipelineFixtures.request(PUT_INPUT, "a"), null, null);

        assertThat(http.transmitted).hasSize(2);
        Optional<String> first = http.transmitted.get(0).headers().firstValue("X-Token");
        assertThat(first).isPresent();
        assertThat(http.transmitted.get(1).headers().firstValue("X-Token")).isEqualTo(first);
    }

    @Test
    void executeSync_overrideHeadersAndQuery_areAppliedToEveryAttempt() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(503, null).respond(200, "v");
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder()
                                                                                  .putHeader("X-Extra", "1")
                                                                                  .putRawQueryParameter("q", "2")
                                                                                  .build();

        engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"), override, null);

        assertThat(http.transmitted).hasSize(2).allSatisfy(request -> {
            assertThat(request.headers().firstValue("X-Extra")).contains("1");
            assertThat(request.encodedQuery()).isEqualTo("q=2");
        });
    }

    @Test
    void executeSync_noEndpoint_failsBeforeAnyTransmission() {
        AwsCallEngine engine = engine(runtime(retries(3)).configuration(SdkClientConfiguration.empty()).build());

        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No endpoint is configured");
        assertThat(http.prepared).isZero();
    }

    @Test
    void executeSync_noAuthOperation_isSelectedAndSentUnsigned() {
        ModeledAuth auth = ModeledAuth.builder().operationId("com.example#Open").build();
        AwsOperation<Struct, Struct> open = PipelineFixtures.operation("com.example#Open", GET_INPUT, GET_OUTPUT,
                                                                       auth);
        AwsCallEngine engine = engine(runtime(retries(1))
                                          .authSchemeSelector(AuthSchemeSelector.create(
                                              List.of(NoAuthAuthScheme.create())))
                                          .build());
        http.respond(200, "v");

        engine.executeSync(engine.runtime().plan(open), PipelineFixtures.request(GET_INPUT, "a"), null, null);

        assertThat(http.transmitted).hasSize(1);
        assertThat(http.transmitted.get(0).headers().contains("Authorization")).isFalse();
    }

    @Test
    void executeSync_requiredAuthWithoutSelector_failsBeforeAnyTransmission() {
        ModeledAuth auth = ModeledAuth.builder().operationId("com.example#Signed")
                                      .addServiceScheme(AuthSchemeIds.SIGV4).build();
        AwsOperation<Struct, Struct> signed = PipelineFixtures.operation("com.example#Signed", GET_INPUT,
                                                                         GET_OUTPUT, auth);
        AwsCallEngine engine = engine(runtime(retries(3)).build());

        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(signed),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("no auth scheme selector");
        assertThat(http.prepared).isZero();
    }

    @Test
    void executeSync_interruptDuringRetryDelay_cancelsTheCallWithoutAnotherTransmission() {
        RecordingRetryStrategy slow = new RecordingRetryStrategy(
            StandardRetryStrategy.builder().maxAttempts(3)
                                 .backoffStrategy(BackoffStrategy.fixedDelay(Duration.ofMinutes(5)))
                                 .build());
        AwsCallEngine engine = engine(runtime(slow).build());
        http.respond(503, null).respond(200, "unused");

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                        PipelineFixtures.request(GET_INPUT, "a"), null, null))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("was interrupted")
                .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        assertThat(http.transmitted).hasSize(1);
        // The granted token of attempt 2 authorized no attempt; the interruption released it once.
        assertThat(slow.ledger).containsExactly("acquire#1", "retry#1", "failure#2");
    }

    @Test
    void executeSync_rejectsAPlanOfAnotherClientAndTheWrongInput() {
        AwsCallEngine engine = engine(runtime(retries(1)).build());
        PipelinePlan<Struct, Struct> foreign = runtime(retries(1)).build().plan(GET_THING);

        assertThatThrownBy(() -> engine.executeSync(foreign, PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("another client");
        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                    PipelineFixtures.request(PUT_INPUT, "a"), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#GetThingInput");
        assertThat(http.prepared).isZero();
    }

    @Test
    void engine_requiresTheTransportOfItsRuntimesMode() {
        assertThatThrownBy(() -> AwsCallEngine.builder().runtime(runtime(retries(1)).build()).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("syncHttpClient");

        AwsCallEngine sync = engine(runtime(retries(1)).build());
        assertThatThrownBy(() -> sync.executeAsync(sync.runtime().plan(GET_THING),
                                                   PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(IllegalStateException.class);
        assertThat(http.prepared).isZero();
        // The async-mode checks are in AwsCallEngineAsyncTest.
    }

    private AwsCallEngine engine(ClientRuntime runtime) {
        return AwsCallEngine.builder().runtime(runtime).syncHttpClient(http).build();
    }

    private static ClientRuntime.Builder runtime(RetryStrategy retryStrategy) {
        return PipelineFixtures.engineRuntime(retryStrategy, HttpTransportMode.SYNC);
    }

    /** A scripted sync transport: one outcome per executed exchange. */
    private static final class FakeHttp implements SdkSyncHttpClient {
        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<SdkHttpRequest> transmitted = new ArrayList<>();
        private int prepared;
        private int closedExchanges;

        FakeHttp respond(int status, String body) {
            outcomes.add(SdkHttpResponse.builder()
                                        .statusCode(status)
                                        .body(body == null ? HttpBody.empty() : HttpBody.fromUtf8(body))
                                        .build());
            return this;
        }

        FakeHttp fail(RuntimeException failure) {
            outcomes.add(failure);
            return this;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            prepared++;
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    transmitted.add(request);
                    Object outcome = outcomes.remove();
                    if (outcome instanceof RuntimeException failure) {
                        throw failure;
                    }
                    return (SdkHttpResponse) outcome;
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                    closedExchanges++;
                }
            };
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "FakeHttp";
        }

        @Override
        public void close() {
        }
    }
}
