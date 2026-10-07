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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The user agent of every call (Requirements 21.6, 21.7): the client's service, SDK version, HTTP Implementation and
 * retry-mode dimensions and the operation, resolved once per plan, plus exactly the feature identifiers and API names
 * that the plan's plugins and the call's override contribute.
 */
class UserAgentTest {
    private static final ApiName DDB_MAPPER = ApiName.builder().name(ApiName.SDK_METRICS).version("d").build();
    private static final ApiName HLL = ApiName.builder().name("hll").version("lib 1").build();
    private static final UserAgent CLIENT = UserAgent.builder()
                                                     .serviceName("Things")
                                                     .sdkVersion("3.0.0")
                                                     .httpImplementation("Scripted Http")
                                                     .retryMode("standard")
                                                     .build();

    private final ScriptedHttp http = new ScriptedHttp();

    @Test
    void operationUserAgentCarriesEveryBaseDimensionAndNoFeatureList() {
        UserAgent.Operation operation = CLIENT.forOperation("GetThing", List.of());
        assertThat(operation.value())
            .startsWith("aws-sdk-java/3.0.0 ua/2.1 os/")
            .contains(" lang/java#" + System.getProperty("java.version"))
            .endsWith(" api/Things#3.0.0 md/op#GetThing md/http#Scripted_Http cfg/retry-mode#standard")
            .doesNotContain(" m/");
        assertThat(operation.value(List.of())).isSameAs(operation.value());
        assertThat(operation.value(null)).isSameAs(operation.value());
    }

    @Test
    void callContributionsAreAddedExactlyOnceInContributionOrder() {
        UserAgent.Operation operation = CLIENT.forOperation("GetThing", List.of("P", "P"));
        assertThat(operation.features()).containsExactly("P");
        assertThat(operation.value()).endsWith(" cfg/retry-mode#standard m/P");
        assertThat(operation.value(List.of(DDB_MAPPER, HLL, DDB_MAPPER, HLL)))
            .endsWith(" cfg/retry-mode#standard m/P,d hll/lib_1");
        assertThat(CLIENT.forOperation("GetThing", List.of()).value(List.of(HLL)))
            .endsWith(" cfg/retry-mode#standard hll/lib_1");
    }

    @Test
    void retryModeNamesTheModernRetryStrategies() {
        assertThat(UserAgent.retryMode(StandardRetryStrategy.create())).isEqualTo("standard");
        assertThat(UserAgent.retryMode(AdaptiveRetryStrategy.builder().build())).isEqualTo("adaptive");
        assertThat(UserAgent.retryMode(PipelineFixtures.retries(2))).isEqualTo("standard");
        assertThat(UserAgent.retryMode(new PipelineFixtures.RecordingRetryStrategy(PipelineFixtures.retries(2))))
            .isEqualTo("custom");
        assertThat(UserAgent.token("a b/c,d#e")).isEqualTo("a_b_c_d#e");
    }

    @Test
    void syncCallsSendThePlanUserAgentPlusOnlyTheirOwnContributions() {
        AwsCallEngine engine = engine(runtime(HttpTransportMode.SYNC).build());
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
        http.respond(200, "v").respond(200, "v").respond(200, "v");

        engine.executeSync(plan, PipelineFixtures.request(GET_INPUT, "a"), null, null);
        engine.executeSync(plan, PipelineFixtures.request(GET_INPUT, "a"), contributing(DDB_MAPPER), null);
        engine.executeSync(plan, PipelineFixtures.request(GET_INPUT, "a"), null, null);

        String base = plan.userAgent().orElseThrow().value();
        assertThat(base).contains(" api/Things#").contains(" md/op#GetThing").contains(" md/http#ScriptedHttp")
                        .contains(" cfg/retry-mode#standard").doesNotContain(" m/");
        assertThat(userAgents()).containsExactly(base, base + " m/d", base);
        assertThat(engine.runtime().plan(GET_THING).userAgent().orElseThrow()).isSameAs(plan.userAgent().orElseThrow());
    }

    @Test
    void asyncCallsSendTheSameUserAgentAsSyncCalls() {
        ClientRuntime runtime = runtime(HttpTransportMode.ASYNC).completionExecutor(Runnable::run)
                                                                .retryScheduler(new PipelineFixtures.ManualScheduler())
                                                                .build();
        AwsCallEngine engine = AwsCallEngine.builder().runtime(runtime).asyncHttpClient(http).build();
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
        http.respond(200, "v").respond(200, "v");

        engine.executeAsync(plan, PipelineFixtures.request(GET_INPUT, "a"), contributing(DDB_MAPPER, HLL), null)
              .join();
        engine.executeAsync(plan, PipelineFixtures.request(GET_INPUT, "a"), null, null).join();

        String base = plan.userAgent().orElseThrow().value();
        assertThat(userAgents()).containsExactly(base + " m/d hll/lib_1", base);
    }

    @Test
    void pluginFeaturesAreInEveryCallOfTheirPlansOnly() {
        AwsFeaturePlugin plugin = new AwsFeaturePlugin() {
            @Override
            public ShapeId id() {
                return ShapeId.from("com.example#UserAgentFeature");
            }

            @Override
            public boolean appliesTo(AwsOperation<?, ?> operation) {
                return operation == GET_THING;
            }

            @Override
            public void configure(PipelinePlan.Builder<?, ?> plan) {
                plan.addUserAgentFeature("P");
            }
        };
        AwsCallEngine engine = engine(runtime(HttpTransportMode.SYNC).addPlugin(plugin).build());
        http.respond(200, "v").respond(200, "v");

        engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"), null, null);
        engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                           contributing(DDB_MAPPER), null);

        assertThat(userAgents()).allSatisfy(ua -> assertThat(ua).contains(" md/op#GetThing"));
        assertThat(userAgents().get(0)).endsWith(" m/P");
        assertThat(userAgents().get(1)).endsWith(" m/P,d");
        assertThat(engine.runtime().plan(PipelineFixtures.PUT_THING).userAgent().orElseThrow().features()).isEmpty();
    }

    @Test
    void anOverrideUserAgentHeaderWinsAndAClientWithoutUserAgentSendsNone() {
        AwsCallEngine withUserAgent = engine(runtime(HttpTransportMode.SYNC).build());
        AwsCallEngine without = engine(PipelineFixtures.engineRuntime(PipelineFixtures.retries(1),
                                                                      HttpTransportMode.SYNC).build());
        http.respond(200, "v").respond(200, "v");

        withUserAgent.executeSync(withUserAgent.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                                  AwsRequestOverrideConfiguration.builder().putHeader("user-agent", "mine").build(),
                                  null);
        without.executeSync(without.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"), null, null);

        assertThat(http.requests.get(0).headers().allValues(UserAgent.HEADER)).containsExactly("mine");
        assertThat(http.requests.get(1).headers().contains(UserAgent.HEADER)).isFalse();
        assertThat(without.runtime().plan(GET_THING).userAgent()).isEmpty();
    }

    private List<String> userAgents() {
        return http.requests.stream().map(SdkHttpRequest::headers)
                            .map(h -> h.allValues(UserAgent.HEADER))
                            .map(values -> {
                                assertThat(values).hasSize(1);
                                return values.get(0);
                            })
                            .toList();
    }

    private static AwsRequestOverrideConfiguration contributing(ApiName... apiNames) {
        AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
        for (ApiName apiName : apiNames) {
            builder.addApiName(apiName);
        }
        return builder.build();
    }

    private ClientRuntime.Builder runtime(HttpTransportMode mode) {
        return PipelineFixtures.engineRuntime(PipelineFixtures.retries(1), mode)
                               .userAgent(UserAgent.builder()
                                                   .serviceName("Things")
                                                   .httpImplementation(http.clientName())
                                                   .retryMode(UserAgent.retryMode(PipelineFixtures.retries(1)))
                                                   .build());
    }

    private AwsCallEngine engine(ClientRuntime runtime) {
        return AwsCallEngine.builder().runtime(runtime).syncHttpClient(http).build();
    }
}
