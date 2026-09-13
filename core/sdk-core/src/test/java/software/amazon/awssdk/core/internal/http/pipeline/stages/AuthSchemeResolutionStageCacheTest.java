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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * The per-client resolution cache as seen through the stage: what a second call with the same inputs reuses, what it
 * must not reuse, and what every kind of change to the inputs does.
 */
class AuthSchemeResolutionStageCacheTest {
    private static final String SCHEME_ID = "test.scheme";
    private static final SignerProperty<String> REGION = SignerProperty.create(AuthSchemeResolutionStageCacheTest.class,
                                                                             "Region");
    private static final SignerProperty<String> NAME = SignerProperty.create(AuthSchemeResolutionStageCacheTest.class,
                                                                           "Name");

    private HttpClientDependencies dependencies;
    private SdkHttpFullRequest.Builder httpRequestBuilder;
    private SdkRequest sdkRequest;

    private IdentityProvider<Identity> identityProvider;
    private HttpSigner<Identity> signer;
    private Map<String, AuthScheme<?>> authSchemes;
    private List<AuthSchemeOption> authOptions;
    private IdentityProviders identityProviders;
    private final AtomicInteger optionsResolverCalls = new AtomicInteger();

    @BeforeEach
    void setup() {
        dependencies = HttpClientDependencies.builder()
                                             .clientConfiguration(SdkClientConfiguration.builder().build())
                                             .build();
        httpRequestBuilder = mock(SdkHttpFullRequest.Builder.class);
        sdkRequest = mock(SdkRequest.class);
        identityProvider = mockIdentityProvider();
        signer = mock(HttpSigner.class);
        authSchemes = authSchemes(identityProvider, signer);
        authOptions = Collections.singletonList(AuthSchemeOption.builder()
                                                                .schemeId(SCHEME_ID)
                                                                .putSignerProperty(NAME, "svc")
                                                                .build());
        identityProviders = mock(IdentityProviders.class);
    }

    @Test
    void execute_sameInputsTwice_reusesSelectionAndResolvesIdentityEachCall() throws Exception {
        SelectedAuthScheme<?> first = run(attributes());
        SelectedAuthScheme<?> second = run(attributes());

        assertThat(second.authSchemeOption()).isSameAs(first.authSchemeOption());
        assertThat(second.signer()).isSameAs(first.signer());
        // The identity is not cached: the provider is asked on every call.
        verify(identityProvider, times(2)).resolveIdentity(any(ResolveIdentityRequest.class));
        assertThat(dependencies.authSchemeResolutionCache().lookup(authOptions, authSchemes, identityProviders, null))
            .isNotNull();
    }

    @Test
    void execute_placeholderSchemePresent_mergedOptionIsCachedAndCorrect() throws Exception {
        // The H4 placeholder: the same instance on every call, carrying a property the resolved option lacks.
        SelectedAuthScheme<?> placeholder = placeholder("us-west-2");

        SelectedAuthScheme<?> first = run(attributesWithPlaceholder(placeholder));
        SelectedAuthScheme<?> second = run(attributesWithPlaceholder(placeholder));

        assertThat(first.authSchemeOption().signerProperty(REGION)).isEqualTo("us-west-2");
        assertThat(first.authSchemeOption().signerProperty(NAME)).isEqualTo("svc");
        assertThat(second.authSchemeOption()).isSameAs(first.authSchemeOption());
    }

    @Test
    void execute_interceptorEditedTheScheme_isNotServedFromCache() throws Exception {
        SelectedAuthScheme<?> placeholder = placeholder("us-west-2");
        run(attributesWithPlaceholder(placeholder));

        // An interceptor replaced SELECTED_AUTH_SCHEME with a different option: the snapshot no longer matches.
        ExecutionAttributes attrs = attributesWithPlaceholder(placeholder);
        SelectedAuthScheme<?> edited = placeholder("eu-west-1");
        attrs.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, edited);
        SelectedAuthScheme<?> selected = run(attrs);

        assertThat(selected.authSchemeOption().signerProperty(REGION)).isEqualTo("eu-west-1");
    }

    @Test
    void execute_differentOptionsListInstance_misses() throws Exception {
        run(attributes());
        List<AuthSchemeOption> otherList = new ArrayList<>(authOptions);
        ExecutionAttributes attrs = attributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER, (req, a) -> otherList);

        SelectedAuthScheme<?> selected = run(attrs);

        // Resolved afresh from the new list (its option instance, not the cached one).
        assertThat(selected.authSchemeOption()).isSameAs(otherList.get(0));
    }

    @Test
    void execute_requestLevelIdentityProviders_useTheOverride() throws Exception {
        run(attributes());
        IdentityProvider<Identity> overrideProvider = mockIdentityProvider();
        HttpSigner<Identity> overrideSigner = mock(HttpSigner.class);
        IdentityProviders overrideProviders = mock(IdentityProviders.class);
        // The scheme hands out a different provider for the override providers.
        AuthScheme<Identity> scheme = mock(AuthScheme.class);
        doReturn(identityProvider).when(scheme).identityProvider(identityProviders);
        doReturn(overrideProvider).when(scheme).identityProvider(overrideProviders);
        doReturn(overrideSigner).when(scheme).signer();
        Map<String, AuthScheme<?>> schemes = new HashMap<>();
        schemes.put(SCHEME_ID, scheme);

        ExecutionAttributes attrs = attributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, schemes);
        attrs.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_RESOLVER, (req, p, a) -> overrideProviders);
        run(attrs);

        verify(overrideProvider).resolveIdentity(any(ResolveIdentityRequest.class));
        verify(identityProvider, times(1)).resolveIdentity(any(ResolveIdentityRequest.class));
    }

    @Test
    void execute_differentAuthSchemesMap_misses() throws Exception {
        run(attributes());
        HttpSigner<Identity> otherSigner = mock(HttpSigner.class);
        ExecutionAttributes attrs = attributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes(identityProvider, otherSigner));

        SelectedAuthScheme<?> selected = run(attrs);

        assertThat(selected.signer()).isSameAs(otherSigner);
    }

    @Test
    void execute_freshOptionsListEveryCall_stopsStoringAfterTheStreak() throws Exception {
        for (int i = 0; i < 20; i++) {
            List<AuthSchemeOption> fresh = new ArrayList<>(authOptions);
            ExecutionAttributes attrs = attributes();
            attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER, (req, a) -> fresh);
            SelectedAuthScheme<?> selected = run(attrs);
            assertThat(selected.authSchemeOption()).isSameAs(fresh.get(0));
        }
        // After MAX_CONSECUTIVE_MISSES the cache stopped storing: the entry still holds one of the early lists, and a
        // new (even stable) list is resolved correctly but not stored. The cache has degraded to a pass-through
        // rather than writing a shared field on every call.
        List<AuthSchemeOption> stable = new ArrayList<>(authOptions);
        ExecutionAttributes attrs = attributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER, (req, a) -> stable);
        SelectedAuthScheme<?> selected = run(attrs);
        assertThat(selected.authSchemeOption()).isSameAs(stable.get(0));
        assertThat(dependencies.authSchemeResolutionCache().lookup(stable, authSchemes, identityProviders, null))
            .isNull();
    }

    @Test
    void execute_stageInstancesAreIndependentButShareTheClientCache() throws Exception {
        ExecutionAttributes attrs = attributes();
        new AuthSchemeResolutionStage(dependencies).execute(httpRequestBuilder, context(attrs));
        SelectedAuthScheme<?> first = attrs.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        ExecutionAttributes attrs2 = attributes();
        new AuthSchemeResolutionStage(dependencies).execute(httpRequestBuilder, context(attrs2));
        SelectedAuthScheme<?> second = attrs2.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        assertThat(second.authSchemeOption()).isSameAs(first.authSchemeOption());
        assertThat(optionsResolverCalls.get()).isEqualTo(2);
    }

    private SelectedAuthScheme<?> run(ExecutionAttributes attrs) throws Exception {
        new AuthSchemeResolutionStage(dependencies).execute(httpRequestBuilder, context(attrs));
        return attrs.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
    }

    private ExecutionAttributes attributes() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes);
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER, (req, a) -> {
            optionsResolverCalls.incrementAndGet();
            return authOptions;
        });
        attrs.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, identityProviders);
        return attrs;
    }

    private ExecutionAttributes attributesWithPlaceholder(SelectedAuthScheme<?> placeholder) {
        ExecutionAttributes attrs = attributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, placeholder);
        attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_PRE_INTERCEPTORS, placeholder);
        return attrs;
    }

    private SelectedAuthScheme<Identity> placeholder(String region) {
        AuthSchemeOption option = AuthSchemeOption.builder().schemeId("unset").putSignerProperty(REGION, region).build();
        return new SelectedAuthScheme<>(CompletableFuture.completedFuture(mock(Identity.class)), signer, option);
    }

    private RequestExecutionContext context(ExecutionAttributes attrs) {
        InterceptorContext interceptorContext = InterceptorContext.builder().request(sdkRequest).build();
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .interceptorContext(interceptorContext)
                                                            .executionAttributes(attrs)
                                                            .build();
        return RequestExecutionContext.builder().executionContext(executionContext).originalRequest(sdkRequest).build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AuthScheme<?>> authSchemes(IdentityProvider<Identity> provider, HttpSigner<Identity> signer) {
        AuthScheme<Identity> scheme = mock(AuthScheme.class);
        doReturn(provider).when(scheme).identityProvider(any());
        doReturn(signer).when(scheme).signer();
        Map<String, AuthScheme<?>> schemes = new HashMap<>();
        schemes.put(SCHEME_ID, scheme);
        return schemes;
    }

    @SuppressWarnings("unchecked")
    private static IdentityProvider<Identity> mockIdentityProvider() {
        IdentityProvider<Identity> provider = mock(IdentityProvider.class);
        doReturn(CompletableFuture.completedFuture(mock(Identity.class)))
            .when(provider).resolveIdentity(any(ResolveIdentityRequest.class));
        return provider;
    }
}
