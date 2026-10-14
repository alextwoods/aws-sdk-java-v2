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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AuthSchemeResolutionStage;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

/**
 * Compares {@link AuthSchemeResolutionCache#resolve} with the legacy {@link AuthSchemeResolutionStage}, and tests the
 * per-client caching (H7) and placeholder (H4).
 */
class AuthSchemeResolutionCacheTest {
    private static final String SIGV4 = "aws.auth#sigv4";
    private static final String OTHER = "com.example#other";
    private static final SignerProperty<String> REGION = SignerProperty.create(AuthSchemeResolutionCacheTest.class, "region");

    private final CountingScheme sigv4 = new CountingScheme(SIGV4, true);
    private final CountingScheme noIdentity = new CountingScheme(OTHER, false);
    private final IdentityProviders identityProviders =
        IdentityProviders.builder().putIdentityProvider(new RequestMutationStagesTest.StaticCredentials()).build();
    private final Map<String, AuthScheme<?>> authSchemes = schemes(sigv4, noIdentity);

    @Test
    void resolve_selectsTheSameAuthSchemeAsTheLegacyStage() throws Exception {
        List<AuthSchemeOption> options = Arrays.asList(option("com.example#missing"), option(OTHER),
                                                       AuthSchemeOption.builder().schemeId(SIGV4)
                                                                       .putSignerProperty(REGION, "us-west-2")
                                                                       .build());
        RequestExecutionContext legacy = context(options, identityProviders);
        new AuthSchemeResolutionStage(null).execute(SdkHttpFullRequest.builder(), legacy);

        RequestExecutionContext context = context(options, identityProviders);
        cache().resolve(context);

        assertSameSelection(context.executionAttributes(), legacy.executionAttributes());
        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .authSchemeOption().schemeId()).isEqualTo(SIGV4);
    }

    @Test
    void resolve_mergesThePlaceholderCheckSumAlgorithmAsTheLegacyStage() throws Exception {
        ChecksumSpecs specs = ChecksumSpecs.builder().algorithmV2(DefaultChecksumAlgorithm.CRC32).build();
        AuthSchemeResolutionCache cache = cache();

        RequestExecutionContext legacy = context(Arrays.asList(option(SIGV4)), identityProviders);
        legacy.executionAttributes().putAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, specs);
        snapshotPreInterceptors(legacy.executionAttributes());
        new AuthSchemeResolutionStage(null).execute(SdkHttpFullRequest.builder(), legacy);

        RequestExecutionContext context = context(Arrays.asList(option(SIGV4)), identityProviders);
        cache.seedResolvedChecksumSpecs(context.executionAttributes(), specs);
        snapshotPreInterceptors(context.executionAttributes());
        cache.resolve(context);

        assertSameSelection(context.executionAttributes(), legacy.executionAttributes());
        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM))
            .isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    void resolve_noUsableScheme_failsWithTheLegacyMessage() {
        List<AuthSchemeOption> options = Arrays.asList(option("com.example#missing"), option(OTHER), option("b#throws"));
        CountingScheme throwing = new CountingScheme("b#throws", true);
        throwing.signerFailure = new IllegalStateException("no signer");
        Map<String, AuthScheme<?>> schemes = schemes(sigv4, noIdentity, throwing);

        Throwable legacy = failure(() -> new AuthSchemeResolutionStage(null)
            .execute(SdkHttpFullRequest.builder(), context(schemes, options, identityProviders)));
        AuthSchemeResolutionCache cache = AuthSchemeResolutionCache.create(config(schemes));

        assertThatThrownBy(() -> cache.resolve(context(schemes, options, identityProviders)))
            .isInstanceOf(SdkException.class)
            .hasMessage(legacy.getMessage());
        assertThatThrownBy(() -> cache.resolve(context(schemes, options, identityProviders)))
            .hasMessage(legacy.getMessage());
    }

    @Test
    void resolve_clientIdentityProviders_looksUpTheSchemeOncePerClient() {
        AuthSchemeResolutionCache cache = cache();
        for (int i = 0; i < 5; i++) {
            cache.resolve(context(Arrays.asList(option(SIGV4)), identityProviders));
        }

        assertThat(sigv4.identityProviderCalls.get()).isEqualTo(1);
        assertThat(sigv4.signerCalls.get()).isEqualTo(1);
        assertThat(sigv4.provider.resolveCalls.get()).isEqualTo(5);
    }

    @Test
    void resolve_requestLevelIdentityProviders_areNotCached() {
        AuthSchemeResolutionCache cache = cache();
        IdentityProviders perRequest = identityProviders.copy(b -> { });
        for (int i = 0; i < 3; i++) {
            cache.resolve(context(Arrays.asList(option(SIGV4)), perRequest));
        }

        assertThat(sigv4.identityProviderCalls.get()).isEqualTo(3);
    }

    @Test
    void resolve_cachesArePerClient() {
        CountingScheme otherClientScheme = new CountingScheme(SIGV4, true);
        Map<String, AuthScheme<?>> otherSchemes = schemes(otherClientScheme);
        AuthSchemeResolutionCache first = cache();
        AuthSchemeResolutionCache second = AuthSchemeResolutionCache.create(config(otherSchemes));

        RequestExecutionContext firstCall = context(Arrays.asList(option(SIGV4)), identityProviders);
        first.resolve(firstCall);
        RequestExecutionContext secondCall = context(otherSchemes, Arrays.asList(option(SIGV4)), identityProviders);
        second.resolve(secondCall);

        assertThat(selected(firstCall).signer()).isSameAs(sigv4.signer);
        assertThat(selected(secondCall).signer()).isSameAs(otherClientScheme.signer);
    }

    @Test
    void resolve_alreadyResolvedScheme_onlyRefreshesTheIdentityAsTheLegacyStage() throws Exception {
        SelectedAuthScheme<AwsCredentialsIdentity> resolved =
            new SelectedAuthScheme<>(CompletableFuture.completedFuture(AwsCredentialsIdentity.create("old", "old")),
                                     sigv4.signer, option(SIGV4));
        RequestExecutionContext legacy = context(Arrays.asList(option(SIGV4)), identityProviders);
        legacy.executionAttributes().putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, resolved);
        new AuthSchemeResolutionStage(null).execute(SdkHttpFullRequest.builder(), legacy);

        RequestExecutionContext context = context(Arrays.asList(option(SIGV4)), identityProviders);
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, resolved);
        cache().resolve(context);

        assertThat(selected(context).authSchemeOption()).isSameAs(resolved.authSchemeOption());
        assertThat(selected(context).identity().join()).isSameAs(selected(legacy).identity().join());
        assertThat(context.executionAttributes()
                          .getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_POST_INTERCEPTORS)).isNull();
    }

    @Test
    void resolve_noAuthSchemesOrOptions_doesNothing() {
        RequestExecutionContext noSchemes = context(Arrays.asList(option(SIGV4)), identityProviders);
        noSchemes.executionAttributes().putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, null);
        RequestExecutionContext noOptions = context(new ArrayList<>(), identityProviders);

        cache().resolve(noSchemes);
        cache().resolve(noOptions);

        assertThat(selected(noSchemes)).isNull();
        assertThat(selected(noOptions)).isNull();
    }

    @Test
    void resolve_runsTheSigningMethodUpdater() {
        AtomicInteger updates = new AtomicInteger();
        RequestExecutionContext context = context(Arrays.asList(option(SIGV4)), identityProviders);
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.SIGNING_METHOD_UPDATER,
                                                   attributes -> updates.incrementAndGet());

        cache().resolve(context);

        assertThat(updates).hasValue(1);
    }

    @Test
    void resolve_identityFetchDuration_isMeasuredOnlyWhenCollecting() {
        MetricCollector collector = mock(MetricCollector.class);
        RequestExecutionContext measured = context(Arrays.asList(option(SIGV4)), identityProviders);
        measured.executionAttributes().putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR, collector);
        RequestExecutionContext unmeasured = context(Arrays.asList(option(SIGV4)), identityProviders);
        unmeasured.executionAttributes().putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR,
                                                      NoOpMetricCollector.create());

        cache().resolve(measured);
        cache().resolve(unmeasured);

        verify(collector).reportMetric(eq(CoreMetric.CREDENTIALS_FETCH_DURATION), any());
        assertThat(selected(unmeasured).identity().join()).isNotNull();
    }

    @Test
    void seedResolvedChecksumSpecs_matchesWritingTheAttributeAndReusesOnePlaceholder() {
        AuthSchemeResolutionCache cache = cache();
        ChecksumSpecs specs = ChecksumSpecs.builder().algorithmV2(DefaultChecksumAlgorithm.SHA256).build();

        ExecutionAttributes legacy = new ExecutionAttributes();
        legacy.putAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, specs);
        ExecutionAttributes first = new ExecutionAttributes();
        cache.seedResolvedChecksumSpecs(first, specs);
        ExecutionAttributes second = new ExecutionAttributes();
        cache.seedResolvedChecksumSpecs(second, null);

        SelectedAuthScheme<?> legacyScheme = legacy.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        SelectedAuthScheme<?> firstScheme = first.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        SelectedAuthScheme<?> secondScheme = second.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(firstScheme.authSchemeOption()).hasToString(legacyScheme.authSchemeOption().toString());
        assertThat(first.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS))
            .isEqualTo(legacy.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS));
        assertThat(secondScheme.authSchemeOption().schemeId()).isEqualTo("unset");
        assertThat(secondScheme.authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM)).isNull();
        assertThat(firstScheme.identity()).isSameAs(secondScheme.identity()).isSameAs(cache.placeholder().identity());
        assertThat(firstScheme.signer()).isSameAs(secondScheme.signer());
        assertThatThrownBy(() -> signUnchecked(firstScheme.signer())).hasMessage("A signer was not configured.");
    }

    @Test
    void seedResolvedChecksumSpecs_existingAuthScheme_isKept() {
        SelectedAuthScheme<AwsCredentialsIdentity> existing =
            new SelectedAuthScheme<>(CompletableFuture.completedFuture(AwsCredentialsIdentity.create("a", "b")),
                                     sigv4.signer, option(SIGV4));
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, existing);

        cache().seedResolvedChecksumSpecs(attributes, ChecksumSpecs.builder().build());

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME).signer())
            .isSameAs(sigv4.signer);
    }

    private AuthSchemeResolutionCache cache() {
        return AuthSchemeResolutionCache.create(config(authSchemes));
    }

    private SdkClientConfiguration config(Map<String, AuthScheme<?>> schemes) {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.AUTH_SCHEMES, schemes)
                                     .option(SdkClientOption.IDENTITY_PROVIDERS, identityProviders)
                                     .build();
    }

    private RequestExecutionContext context(List<AuthSchemeOption> options, IdentityProviders providers) {
        return context(authSchemes, options, providers);
    }

    private static RequestExecutionContext context(Map<String, AuthScheme<?>> schemes,
                                                   List<AuthSchemeOption> options,
                                                   IdentityProviders providers) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, schemes);
        attributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, providers);
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER, (r, a) -> options);
        SdkHttpFullRequest httpRequest = ApiCallStagesTestSupport.marshalledRequest(ApiCallStagesTestSupport.CLIENT_ENDPOINT,
                                                                                    null);
        return ApiCallStagesTestSupport.context(NoopTestRequest.builder().build(), attributes, httpRequest);
    }

    private static void snapshotPreInterceptors(ExecutionAttributes attributes) {
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_PRE_INTERCEPTORS,
                                attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME));
    }

    private static void assertSameSelection(ExecutionAttributes actual, ExecutionAttributes expected) {
        SelectedAuthScheme<?> actualScheme = actual.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        SelectedAuthScheme<?> expectedScheme = expected.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(actualScheme.authSchemeOption()).hasToString(expectedScheme.authSchemeOption().toString());
        assertThat(actualScheme.signer()).isSameAs(expectedScheme.signer());
        assertThat(actualScheme.identity().join()).isSameAs(expectedScheme.identity().join());
        SelectedAuthScheme<?> actualSnapshot =
            actual.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_POST_INTERCEPTORS);
        SelectedAuthScheme<?> expectedSnapshot =
            expected.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_POST_INTERCEPTORS);
        if (expectedSnapshot == null) {
            assertThat(actualSnapshot).isNull();
        } else {
            assertThat(actualSnapshot.authSchemeOption()).hasToString(expectedSnapshot.authSchemeOption().toString());
        }
    }

    private static SelectedAuthScheme<?> selected(RequestExecutionContext context) {
        return context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
    }

    private static AuthSchemeOption option(String schemeId) {
        return AuthSchemeOption.builder().schemeId(schemeId).build();
    }

    private static Map<String, AuthScheme<?>> schemes(CountingScheme... schemes) {
        Map<String, AuthScheme<?>> map = new HashMap<>();
        for (CountingScheme scheme : schemes) {
            map.put(scheme.schemeId(), scheme);
        }
        return map;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void signUnchecked(HttpSigner signer) {
        signer.sign((SignRequest) null);
    }

    private static Throwable failure(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("Expected a failure");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class CountingProvider implements IdentityProvider<AwsCredentialsIdentity> {
        private final AtomicInteger resolveCalls = new AtomicInteger();
        private final IdentityProvider<AwsCredentialsIdentity> delegate = new RequestMutationStagesTest.StaticCredentials();

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<? extends AwsCredentialsIdentity> resolveIdentity(
            software.amazon.awssdk.identity.spi.ResolveIdentityRequest request) {
            resolveCalls.incrementAndGet();
            return delegate.resolveIdentity(request);
        }
    }

    private static final class CountingScheme implements AuthScheme<AwsCredentialsIdentity> {
        private final String schemeId;
        private final boolean hasIdentity;
        private final AtomicInteger identityProviderCalls = new AtomicInteger();
        private final AtomicInteger signerCalls = new AtomicInteger();
        private final CountingProvider provider = new CountingProvider();
        private final HttpSigner<AwsCredentialsIdentity> signer = new RequestMutationStagesTest.NoOpSigner<>();
        private RuntimeException signerFailure;

        private CountingScheme(String schemeId, boolean hasIdentity) {
            this.schemeId = schemeId;
            this.hasIdentity = hasIdentity;
        }

        @Override
        public String schemeId() {
            return schemeId;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            identityProviderCalls.incrementAndGet();
            return hasIdentity && providers.identityProvider(AwsCredentialsIdentity.class) != null ? provider : null;
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            signerCalls.incrementAndGet();
            if (signerFailure != null) {
                throw signerFailure;
            }
            return signer;
        }
    }
}
