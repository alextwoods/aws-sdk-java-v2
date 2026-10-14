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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Compares {@link UserAgentHeaderCache} with the legacy {@link ApplyUserAgentStage} for client-level combinations; the
 * request-level values are compared in {@link RequestMutationStagesTest}.
 */
class UserAgentHeaderCacheTest {

    static Stream<Arguments> clientConfigurations() {
        return Stream.of(
            Arguments.of("defaults", (Consumer<SdkClientConfiguration.Builder>) c -> { }),
            Arguments.of("prefix", (Consumer<SdkClientConfiguration.Builder>)
                c -> c.option(SdkAdvancedClientOption.USER_AGENT_PREFIX, "  pre  ")),
            Arguments.of("suffix", (Consumer<SdkClientConfiguration.Builder>)
                c -> c.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "suf")),
            Arguments.of("blank prefix and suffix", (Consumer<SdkClientConfiguration.Builder>)
                c -> c.option(SdkAdvancedClientOption.USER_AGENT_PREFIX, " ")
                      .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, ""))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientConfigurations")
    void headerValue_matchesTheLegacyStage(String name, Consumer<SdkClientConfiguration.Builder> customization)
        throws Exception {
        SdkClientConfiguration.Builder builder = ApiCallStagesTestSupport.baseConfig();
        customization.accept(builder);
        SdkClientConfiguration config = builder.build();

        String expected = new ApplyUserAgentStage(ApiCallStagesTestSupport.dependencies(config))
            .execute(SdkHttpFullRequest.builder(), context(null))
            .firstMatchingHeader(ApplyUserAgentStage.HEADER_USER_AGENT).orElse(null);

        assertThat(UserAgentHeaderCache.create(config).headerValue(context(null))).isEqualTo(expected);
    }

    @Test
    void headerValue_withoutRequestValues_returnsThePrecomputedValue() {
        UserAgentHeaderCache cache = UserAgentHeaderCache.create(ApiCallStagesTestSupport.baseConfig().build());

        assertThat(cache.headerValue(context(null))).isSameAs(cache.headerValue(context(null)));
    }

    @Test
    void headerValue_withRequestValues_buildsANewValue() {
        UserAgentHeaderCache cache = UserAgentHeaderCache.create(ApiCallStagesTestSupport.baseConfig().build());

        String value = cache.headerValue(context(o -> o.addApiName(ApiName.builder().name("lib").version("2").build())));

        assertThat(value).isEqualTo(ApiCallStagesTestSupport.CLIENT_USER_AGENT + " lib/2");
    }

    @Test
    void headerValue_identityNotResolvedYet_waitsForItAsTheLegacyStage() {
        UserAgentHeaderCache cache = UserAgentHeaderCache.create(ApiCallStagesTestSupport.baseConfig().build());
        CompletableFuture<AwsCredentialsIdentity> identity = new CompletableFuture<>();
        RequestExecutionContext context = context(null);
        context.executionAttributes().putAttribute(
            SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
            new SelectedAuthScheme<>(identity, new RequestMutationStagesTest.NoOpSigner<>(),
                                     AuthSchemeOption.builder().schemeId("aws.auth#sigv4").build()));
        CompletableFuture.runAsync(() -> {
            sleep();
            identity.complete(AwsSessionCredentialsIdentity.builder().accessKeyId("a").secretAccessKey("s")
                                                           .sessionToken("t").providerName("w").build());
        });

        assertThat(cache.headerValue(context)).isEqualTo(ApiCallStagesTestSupport.CLIENT_USER_AGENT + " m/w");
    }

    @Test
    void headerValue_identityFailed_failsAsTheLegacyStage() {
        UserAgentHeaderCache cache = UserAgentHeaderCache.create(ApiCallStagesTestSupport.baseConfig().build());
        CompletableFuture<AwsCredentialsIdentity> identity = new CompletableFuture<>();
        identity.completeExceptionally(new IllegalStateException("no credentials"));
        RequestExecutionContext context = context(null);
        context.executionAttributes().putAttribute(
            SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
            new SelectedAuthScheme<>(identity, new RequestMutationStagesTest.NoOpSigner<>(),
                                     AuthSchemeOption.builder().schemeId("aws.auth#sigv4").build()));

        assertThatThrownBy(() -> cache.headerValue(context)).isInstanceOf(IllegalStateException.class)
                                                           .hasMessage("no credentials");
    }

    private static void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static RequestExecutionContext context(Consumer<SdkRequestOverrideConfiguration.Builder> override) {
        NoopTestRequest.Builder request = NoopTestRequest.builder();
        if (override != null) {
            SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
            override.accept(builder);
            request.overrideConfiguration(builder.build());
        }
        return ApiCallStagesTestSupport.context(request.build(), new ExecutionAttributes(),
                                                ApiCallStagesTestSupport.marshalledRequest(
                                                    ApiCallStagesTestSupport.CLIENT_ENDPOINT, null));
    }
}
