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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.auth.AuthSchemeResolutionCache;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.spi.identity.RequestIdentityProviderResolver;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Pipeline stage that resolves the auth scheme and identity for signing.
 */
@SdkInternalApi
public final class AuthSchemeResolutionStage implements MutableRequestToRequestPipeline {

    private static final String SIGV4A_SCHEME_ID = "aws.auth#sigv4a";
    private static final String BEARER_SCHEME_ID = "smithy.api#httpBearerAuth";

    /**
     * Per-client cache of the call-independent half of resolution; see {@link AuthSchemeResolutionCache}. Stages are
     * constructed per call, so the cache is the client's, held by its dependencies.
     */
    private final AuthSchemeResolutionCache resolutionCache;

    public AuthSchemeResolutionStage(HttpClientDependencies dependencies) {
        this.resolutionCache = dependencies != null ? dependencies.authSchemeResolutionCache()
                                                    : new AuthSchemeResolutionCache();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        ExecutionAttributes executionAttributes = context.executionAttributes();

        Map<String, AuthScheme<?>> authSchemes = executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return request;
        }

        SdkRequest sdkRequest = context.executionContext().interceptorContext().request();
        List<AuthSchemeOption> authOptions = resolveAuthSchemeOptions(executionAttributes, sdkRequest);
        if (authOptions == null || authOptions.isEmpty()) {
            return request;
        }

        IdentityProviders identityProviders = updateIdentityProvidersIfNeeded(executionAttributes, sdkRequest);

        // Skip resolution if auth scheme was already resolved by an old service interceptor
        SelectedAuthScheme<?> existing = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (existing != null && !"unset".equals(existing.authSchemeOption().schemeId())) {
            //Updates the identity on an already-resolved auth scheme, ensuring credential overrides via interceptors
            //are respected even with old service clients.
            updateIdentityOnExistingScheme(existing, identityProviders, executionAttributes);
            return request;
        }

        MetricCollector metricCollector =
            executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);

        SelectedAuthScheme<? extends Identity> selectedAuthScheme =
            resolve(authOptions, authSchemes, identityProviders, existing, executionAttributes, metricCollector);

        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_POST_INTERCEPTORS, existing);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);

        Consumer<ExecutionAttributes> signingMethodUpdater =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SIGNING_METHOD_UPDATER);
        if (signingMethodUpdater != null) {
            signingMethodUpdater.accept(executionAttributes);
        }

        recordBusinessMetrics(selectedAuthScheme, sdkRequest, executionAttributes);

        return request;
    }

    /**
     * {@link AuthSchemeResolver#selectAuthScheme} followed by {@link AuthSchemeResolver#mergePreExistingAuthSchemeProperties},
     * with the call-independent part of both served from the per-client cache when the inputs are the same instances as
     * last time. The merge is only cacheable when no interceptor edited the pre-existing scheme (the pre-interceptor
     * snapshot still holds the same option instance); otherwise the diff against the snapshot has to be redone.
     */
    private SelectedAuthScheme<? extends Identity> resolve(List<AuthSchemeOption> authOptions,
                                                           Map<String, AuthScheme<?>> authSchemes,
                                                           IdentityProviders identityProviders,
                                                           SelectedAuthScheme<?> existing,
                                                           ExecutionAttributes executionAttributes,
                                                           MetricCollector metricCollector) {
        SelectedAuthScheme<?> beforeInterceptors =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_PRE_INTERCEPTORS);
        AuthSchemeOption existingOption = existing == null ? null : existing.authSchemeOption();
        boolean mergeIsCacheable = existing == null
                                   || (beforeInterceptors != null && beforeInterceptors.authSchemeOption() == existingOption);

        if (mergeIsCacheable) {
            AuthSchemeResolutionCache.Entry<? extends Identity> cached =
                resolutionCache.lookup(authOptions, authSchemes, identityProviders, existingOption);
            if (cached != null) {
                return cached.resolution().select(metricCollector);
            }
        }

        AuthSchemeResolver.Resolution<? extends Identity> resolution =
            AuthSchemeResolver.resolve(authOptions, authSchemes, identityProviders);
        // As before this cache existed, the identity request is built from the resolved option, not the merged one:
        // selection always ran before the merge, so the merge never influenced which identity was fetched.
        AuthSchemeOption merged =
            AuthSchemeResolver.mergePreExistingProperties(resolution.authSchemeOption(), existing, beforeInterceptors);
        resolution = resolution.withAuthSchemeOption(merged);
        if (mergeIsCacheable) {
            resolutionCache.store(authOptions, authSchemes, identityProviders, existingOption, resolution);
        }
        return resolution.select(metricCollector);
    }

    private List<AuthSchemeOption> resolveAuthSchemeOptions(ExecutionAttributes executionAttributes, SdkRequest request) {
        AuthSchemeOptionsResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER);

        if (resolver == null) {
            return null;
        }
        return resolver.resolve(request, executionAttributes);
    }

    /**
     * Returns identity providers after applying any request-level overrides. This allows aws-core to inject
     * credential overrides from {@code AwsRequestOverrideConfiguration} (e.g., per-request credentials provider)
     * without sdk-core depending on aws-core. The resolver is set by {@code AwsExecutionContextBuilder} and runs
     * after interceptors have modified the request, ensuring user-injected credentials are respected.
     */
    private IdentityProviders updateIdentityProvidersIfNeeded(ExecutionAttributes executionAttributes, SdkRequest request) {
        IdentityProviders identityProviders =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);

        RequestIdentityProviderResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_RESOLVER);
        if (resolver != null) {
            identityProviders = resolver.resolve(request, identityProviders, executionAttributes);
        }
        return identityProviders;
    }

    @SuppressWarnings("unchecked")
    private <T extends Identity> void updateIdentityOnExistingScheme(SelectedAuthScheme<T> existing,
                                                                     IdentityProviders identityProviders,
                                                                     ExecutionAttributes executionAttributes) {
        AuthScheme<T> authScheme = (AuthScheme<T>) executionAttributes
            .getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES)
            .get(existing.authSchemeOption().schemeId());
        if (authScheme == null) {
            return;
        }
        IdentityProvider<T> identityProvider = authScheme.identityProvider(identityProviders);
        if (identityProvider == null) {
            return;
        }
        CompletableFuture<? extends T> identity = identityProvider.resolveIdentity(ResolveIdentityRequest.builder().build());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                         new SelectedAuthScheme<>(identity, existing.signer(), existing.authSchemeOption()));
    }

    private void recordBusinessMetrics(SelectedAuthScheme<? extends Identity> selectedAuthScheme,
                                       SdkRequest request,
                                       ExecutionAttributes executionAttributes) {
        if (selectedAuthScheme == null) {
            return;
        }

        BusinessMetricCollection businessMetrics =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        if (businessMetrics == null) {
            return;
        }

        String schemeId = selectedAuthScheme.authSchemeOption().schemeId();

        if (isSignerOverridden(request, executionAttributes)) {
            return;
        }

        if (SIGV4A_SCHEME_ID.equals(schemeId)) {
            businessMetrics.addMetric(BusinessMetricFeatureId.SIGV4A_SIGNING.value());
        }

        if (BEARER_SCHEME_ID.equals(schemeId) && selectedAuthScheme.identity().isDone()) {
            Identity identity = selectedAuthScheme.identity().getNow(null);
            if (identity instanceof TokenIdentity) {
                String tokenFromEnv = executionAttributes.getAttribute(SdkInternalExecutionAttribute.TOKEN_CONFIGURED_FROM_ENV);
                if (tokenFromEnv != null && tokenFromEnv.equals(((TokenIdentity) identity).token())) {
                    businessMetrics.addMetric(BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS.value());
                }
            }
        }
    }

    private boolean isSignerOverridden(SdkRequest request, ExecutionAttributes executionAttributes) {
        boolean isClientSignerOverridden =
            Boolean.TRUE.equals(executionAttributes.getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN));
        boolean isRequestSignerOverridden = request.overrideConfiguration()
                                                   .flatMap(RequestOverrideConfiguration::signer)
                                                   .isPresent();
        return isClientSignerOverridden || isRequestSignerOverridden;
    }
}
