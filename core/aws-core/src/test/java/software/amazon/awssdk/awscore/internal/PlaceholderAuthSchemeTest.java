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

package software.amazon.awssdk.awscore.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.awscore.client.http.NoopTestAwsRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;

/**
 * The per-call {@code SERVICE_SIGNING_NAME} / {@code SIGNING_REGION} writes are replaced by a placeholder auth scheme
 * computed once per client. These pin that the replacement is observably identical, and that it steps aside when a
 * request already carries an auth scheme.
 */
class PlaceholderAuthSchemeTest {

    private static final String SIGNING_NAME = "svc";
    private static final Region REGION = Region.EU_WEST_2;

    /** What the two legacy writes produce on a fresh attribute set — the reference the placeholder must match. */
    private static SelectedAuthScheme<?> legacyResult() {
        ExecutionAttributes scratch = new ExecutionAttributes();
        scratch.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
               .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, REGION);
        return scratch.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
    }

    private static SdkClientConfiguration.Builder baseConfig() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.emptyList())
                                     .option(AwsClientOption.SERVICE_SIGNING_NAME, SIGNING_NAME)
                                     .option(AwsClientOption.SIGNING_REGION, REGION)
                                     .option(AwsClientOption.AWS_REGION, REGION)
                                     // Post-SRA: an auth-schemes map keeps the builder off the legacy signer path,
                                     // which would otherwise demand a credentials provider.
                                     .option(SdkClientOption.AUTH_SCHEMES, Collections.emptyMap())
                                     .option(SdkClientOption.EXECUTION_ATTRIBUTES, new ExecutionAttributes());
    }

    private static ClientExecutionParams<SdkRequest, SdkResponse> params(AwsRequest request) {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(request)
            .withFullDuplex(false)
            .withOperationName("TestOperation");
    }

    @Test
    void placeholder_readsBackExactlyAsTheLegacyWritesDid() {
        SelectedAuthScheme<?> legacy = legacyResult();
        SdkClientConfiguration config = baseConfig()
            .option(AwsInternalClientOption.PLACEHOLDER_AUTH_SCHEME, legacy)
            .build();

        ExecutionContext ctx = AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(
            params(NoopTestAwsRequest.builder().build()), config);
        ExecutionAttributes attrs = ctx.executionAttributes();

        // The legacy read mappings resolve through SELECTED_AUTH_SCHEME, so the values are what matters.
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(REGION);
        SelectedAuthScheme<?> selected = attrs.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(selected.authSchemeOption().schemeId()).isEqualTo(legacy.authSchemeOption().schemeId());
        assertThat(selected.authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME))
            .isEqualTo(SIGNING_NAME);
        assertThat(selected.authSchemeOption().signerProperty(AwsV4HttpSigner.REGION_NAME)).isEqualTo(REGION.id());
    }

    @Test
    void placeholder_isTheSameInstanceAcrossCalls() {
        SdkClientConfiguration config = baseConfig()
            .option(AwsInternalClientOption.PLACEHOLDER_AUTH_SCHEME, legacyResult())
            .build();

        SelectedAuthScheme<?> configured = config.option(AwsInternalClientOption.PLACEHOLDER_AUTH_SCHEME);
        assertThat(configured).as("option round-trips through the client configuration").isNotNull();
        SelectedAuthScheme<?> first = AwsExecutionContextBuilder
            .invokeInterceptorsAndCreateExecutionContext(params(NoopTestAwsRequest.builder().build()), config)
            .executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        SelectedAuthScheme<?> second = AwsExecutionContextBuilder
            .invokeInterceptorsAndCreateExecutionContext(params(NoopTestAwsRequest.builder().build()), config)
            .executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        assertThat(first).as("placeholder installed, not rebuilt").isSameAs(configured);
        assertThat(first).isSameAs(second);
    }

    @Test
    void requestLevelAuthScheme_isLayeredOnto_notReplaced() {
        // A request-level execution attribute already carries an auth scheme; the legacy writes must layer the
        // signing name and region onto THAT, as they always did, rather than replace it with the placeholder.
        SelectedAuthScheme<?> fromRequest = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(AwsCredentialsIdentity.create("ak", "sk")),
            mock(HttpSigner.class),
            AuthSchemeOption.builder().schemeId("request-supplied").build());
        AwsRequest request = NoopTestAwsRequest.builder()
            .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                .putExecutionAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, fromRequest)
                .build())
            .build();
        SdkClientConfiguration config = baseConfig()
            .option(AwsInternalClientOption.PLACEHOLDER_AUTH_SCHEME, legacyResult())
            .build();

        ExecutionAttributes attrs = AwsExecutionContextBuilder
            .invokeInterceptorsAndCreateExecutionContext(params(request), config).executionAttributes();
        SelectedAuthScheme<?> selected = attrs.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        assertThat(selected.authSchemeOption().schemeId()).isEqualTo("request-supplied");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(REGION);
    }

    @Test
    void noPlaceholderConfigured_fallsBackToTheLegacyWrites() {
        SdkClientConfiguration config = baseConfig().build();

        ExecutionAttributes attrs = AwsExecutionContextBuilder
            .invokeInterceptorsAndCreateExecutionContext(params(NoopTestAwsRequest.builder().build()), config)
            .executionAttributes();

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(REGION);
    }
}
