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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AuthSchemeResolutionStage;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.spi.identity.RequestIdentityProviderResolver;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * The auth scheme resolution step of the schema-based request pipeline, with the values that depend only on the client
 * configuration computed once per client (racecar H7 and H4, re-implemented).
 *
 * <p>{@link #resolve(RequestExecutionContext)} has the observable behavior of the legacy
 * {@link AuthSchemeResolutionStage}: the same execution attributes are read and written in the same order, and the same
 * auth scheme, identity, signer and failure message are selected. The difference is in what is computed per call:
 * <ul>
 *     <li><b>H7.</b> When the call uses the client's own auth schemes and identity providers (no request-level
 *     credentials or token override), the auth scheme, identity provider and signer for an option's scheme ID come from
 *     a per-client cache instead of being looked up on every call. A call with overrides takes the uncached path,
 *     which is the legacy {@link AuthSchemeResolver#selectAuthScheme} logic.</li>
 *     <li><b>H4.</b> The placeholder {@code "unset"} auth scheme that carries the request checksum algorithm before an
 *     auth scheme is resolved is built once per client ({@link #seedResolvedChecksumSpecs}), instead of a new
 *     identity future and signer for every call.</li>
 *     <li><b>H8.</b> The identity fetch duration is measured only when the call's metric collector collects
 *     metrics.</li>
 * </ul>
 *
 * <p>The options themselves ({@link SdkInternalExecutionAttribute#AUTH_SCHEME_OPTIONS_RESOLVER}) are resolved on every
 * call: an auth scheme provider may depend on the request, so caching them would not be safe in general.
 */
@SdkInternalApi
@ThreadSafe
public final class AuthSchemeResolutionCache {
    /** The scheme ID of the placeholder auth scheme; the same as the legacy placeholder's. */
    static final String UNSET_SCHEME_ID = "unset";

    private static final Logger LOG = Logger.loggerFor(AuthSchemeResolver.class);
    private static final String SIGV4A_SCHEME_ID = "aws.auth#sigv4a";
    private static final String BEARER_SCHEME_ID = "smithy.api#httpBearerAuth";

    private final Map<String, AuthScheme<?>> clientAuthSchemes;
    private final IdentityProviders clientIdentityProviders;
    private final Map<String, Binding<?>> bindings = new ConcurrentHashMap<>();
    private final SelectedAuthScheme<UnsetIdentity> placeholder;

    private AuthSchemeResolutionCache(SdkClientConfiguration clientConfiguration) {
        this.clientAuthSchemes = clientConfiguration.option(SdkClientOption.AUTH_SCHEMES);
        this.clientIdentityProviders = clientConfiguration.option(SdkClientOption.IDENTITY_PROVIDERS);
        this.placeholder = new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                                    new UnsetHttpSigner(),
                                                    AuthSchemeOption.builder().schemeId(UNSET_SCHEME_ID).build());
    }

    /**
     * Creates the cache for a client configuration.
     */
    public static AuthSchemeResolutionCache create(SdkClientConfiguration clientConfiguration) {
        return new AuthSchemeResolutionCache(clientConfiguration);
    }

    /**
     * Writes {@link SdkExecutionAttribute#RESOLVED_CHECKSUM_SPECS} for a call, as the client handler does before the
     * interceptors run. When the call has no {@link SdkInternalExecutionAttribute#SELECTED_AUTH_SCHEME} yet, the
     * attribute is backed by this client's placeholder auth scheme (scheme ID {@code "unset"}), so the call gets the
     * same placeholder values as from writing the attribute directly, without creating a new placeholder identity and
     * signer.
     *
     * @param attributes the call's execution attributes
     * @param checksumSpecs the resolved checksum specs, may be null
     */
    public void seedResolvedChecksumSpecs(ExecutionAttributes attributes, ChecksumSpecs checksumSpecs) {
        if (attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME) == null) {
            attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, placeholder);
        }
        attributes.putAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, checksumSpecs);
    }

    /**
     * Returns this client's placeholder auth scheme: scheme ID {@code "unset"}, no properties, an identity that carries
     * nothing and a signer that fails, as the legacy placeholder.
     */
    SelectedAuthScheme<?> placeholder() {
        return placeholder;
    }

    /**
     * Resolves the call's auth scheme and identity, with the behavior of {@link AuthSchemeResolutionStage}.
     *
     * @param context the call's context; the request is the interceptor context's request (after {@code modifyRequest})
     */
    public void resolve(RequestExecutionContext context) {
        ExecutionAttributes executionAttributes = context.executionAttributes();
        Map<String, AuthScheme<?>> authSchemes = executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return;
        }

        SdkRequest sdkRequest = context.executionContext().interceptorContext().request();
        List<AuthSchemeOption> authOptions = resolveAuthSchemeOptions(executionAttributes, sdkRequest);
        if (authOptions == null || authOptions.isEmpty()) {
            return;
        }

        IdentityProviders identityProviders = identityProviders(executionAttributes, sdkRequest);
        boolean cacheable = authSchemes == clientAuthSchemes
                            && identityProviders == clientIdentityProviders
                            && identityProviders != null;

        SelectedAuthScheme<?> existing = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (existing != null && !UNSET_SCHEME_ID.equals(existing.authSchemeOption().schemeId())) {
            updateIdentityOnExistingScheme(existing, authSchemes, identityProviders, cacheable, executionAttributes);
            return;
        }

        MetricCollector metricCollector =
            executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        MetricCollector measuringCollector = MetricUtils.collectsMetrics(metricCollector) ? metricCollector : null;
        SelectedAuthScheme<? extends Identity> selectedAuthScheme =
            cacheable ? selectCached(authOptions, identityProviders, measuringCollector)
                      : AuthSchemeResolver.selectAuthScheme(authOptions, authSchemes, identityProviders, measuringCollector);

        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_POST_INTERCEPTORS,
                                         executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME));
        selectedAuthScheme = AuthSchemeResolver.mergePreExistingAuthSchemeProperties(selectedAuthScheme, executionAttributes);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);

        Consumer<ExecutionAttributes> signingMethodUpdater =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SIGNING_METHOD_UPDATER);
        if (signingMethodUpdater != null) {
            signingMethodUpdater.accept(executionAttributes);
        }

        recordBusinessMetrics(selectedAuthScheme, sdkRequest, executionAttributes);
    }

    private static List<AuthSchemeOption> resolveAuthSchemeOptions(ExecutionAttributes executionAttributes,
                                                                   SdkRequest request) {
        AuthSchemeOptionsResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER);
        if (resolver == null) {
            return null;
        }
        return resolver.resolve(request, executionAttributes);
    }

    private static IdentityProviders identityProviders(ExecutionAttributes executionAttributes, SdkRequest request) {
        IdentityProviders identityProviders =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);
        RequestIdentityProviderResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_RESOLVER);
        if (resolver != null) {
            identityProviders = resolver.resolve(request, identityProviders, executionAttributes);
        }
        return identityProviders;
    }

    /**
     * {@link AuthSchemeResolver#selectAuthScheme} with the per-scheme lookups taken from the cache: the same options are
     * tried in the same order, with the same discard reasons, debug entry and failure message.
     */
    private SelectedAuthScheme<? extends Identity> selectCached(List<AuthSchemeOption> authOptions,
                                                               IdentityProviders identityProviders,
                                                               MetricCollector metricCollector) {
        List<Supplier<String>> discardedReasons = null;
        for (AuthSchemeOption authOption : authOptions) {
            Binding<?> binding = binding(authOption.schemeId(), identityProviders);
            if (binding.discardReason != null) {
                if (discardedReasons == null) {
                    discardedReasons = new ArrayList<>();
                }
                discardedReasons.add(binding.discardReason);
                continue;
            }
            SelectedAuthScheme<? extends Identity> selected = binding.select(authOption, metricCollector);
            if (discardedReasons != null) {
                List<Supplier<String>> reasons = discardedReasons;
                LOG.debug(() -> String.format("%s auth will be used, discarded: '%s'",
                                              authOption.schemeId(), join(reasons)));
            }
            return selected;
        }
        throw SdkException.builder()
                          .message("Failed to determine how to authenticate the user: "
                                   + (discardedReasons == null ? "" : join(discardedReasons)))
                          .build();
    }

    private Binding<?> binding(String schemeId, IdentityProviders identityProviders) {
        Binding<?> binding = bindings.get(schemeId);
        if (binding == null) {
            binding = bindings.computeIfAbsent(schemeId, id -> Binding.create(id, clientAuthSchemes.get(id),
                                                                              identityProviders));
        }
        return binding;
    }

    @SuppressWarnings("unchecked")
    private <T extends Identity> void updateIdentityOnExistingScheme(SelectedAuthScheme<T> existing,
                                                                     Map<String, AuthScheme<?>> authSchemes,
                                                                     IdentityProviders identityProviders,
                                                                     boolean cacheable,
                                                                     ExecutionAttributes executionAttributes) {
        String schemeId = existing.authSchemeOption().schemeId();
        IdentityProvider<T> identityProvider;
        if (cacheable) {
            Binding<?> binding = binding(schemeId, identityProviders);
            if (binding.authScheme == null || binding.identityProvider == null) {
                return;
            }
            identityProvider = (IdentityProvider<T>) binding.identityProvider;
        } else {
            AuthScheme<T> authScheme = (AuthScheme<T>) authSchemes.get(schemeId);
            if (authScheme == null) {
                return;
            }
            identityProvider = authScheme.identityProvider(identityProviders);
            if (identityProvider == null) {
                return;
            }
        }
        CompletableFuture<? extends T> identity = identityProvider.resolveIdentity(ResolveIdentityRequest.builder().build());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                         new SelectedAuthScheme<>(identity, existing.signer(), existing.authSchemeOption()));
    }

    /**
     * Same as the legacy stage's business metrics: SigV4a signing, and a bearer token taken from the environment, unless
     * the signer is overridden.
     */
    private static void recordBusinessMetrics(SelectedAuthScheme<? extends Identity> selectedAuthScheme,
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
        if (isSignerOverridden(request, executionAttributes)) {
            return;
        }
        String schemeId = selectedAuthScheme.authSchemeOption().schemeId();
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

    private static boolean isSignerOverridden(SdkRequest request, ExecutionAttributes executionAttributes) {
        return Boolean.TRUE.equals(executionAttributes.getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN))
               || request.overrideConfiguration().flatMap(RequestOverrideConfiguration::signer).isPresent();
    }

    private static String join(List<Supplier<String>> reasons) {
        return reasons.stream().map(Supplier::get).collect(Collectors.joining(", "));
    }

    /**
     * The per-client result of looking up one scheme ID: the auth scheme, its identity provider and its signer, or the
     * reason {@link AuthSchemeResolver#selectAuthScheme} gives for discarding the option.
     */
    private static final class Binding<T extends Identity> {
        private final AuthScheme<T> authScheme;
        private final IdentityProvider<T> identityProvider;
        private final HttpSigner<T> signer;
        private final SdkMetric<Duration> identityMetric;
        private final Supplier<String> discardReason;

        private Binding(AuthScheme<T> authScheme, IdentityProvider<T> identityProvider, HttpSigner<T> signer,
                        Supplier<String> discardReason) {
            this.authScheme = authScheme;
            this.identityProvider = identityProvider;
            this.signer = signer;
            this.identityMetric = identityProvider == null ? null : identityMetric(identityProvider);
            this.discardReason = discardReason;
        }

        @SuppressWarnings("unchecked")
        private static <T extends Identity> Binding<T> create(String schemeId,
                                                              AuthScheme<?> scheme,
                                                              IdentityProviders identityProviders) {
            AuthScheme<T> authScheme = (AuthScheme<T>) scheme;
            if (authScheme == null) {
                return new Binding<>(null, null, null,
                                     () -> String.format("'%s' is not enabled for this request.", schemeId));
            }
            IdentityProvider<T> identityProvider = authScheme.identityProvider(identityProviders);
            if (identityProvider == null) {
                return new Binding<>(authScheme, null, null,
                                     () -> String.format("'%s' does not have an identity provider configured.", schemeId));
            }
            try {
                return new Binding<>(authScheme, identityProvider, authScheme.signer(), null);
            } catch (RuntimeException e) {
                String message = e.getMessage();
                return new Binding<>(authScheme, identityProvider, null,
                                     () -> String.format("'%s' signer could not be retrieved: %s", schemeId, message));
            }
        }

        private SelectedAuthScheme<T> select(AuthSchemeOption authOption, MetricCollector metricCollector) {
            ResolveIdentityRequest.Builder identityRequest = ResolveIdentityRequest.builder();
            authOption.forEachIdentityProperty(identityRequest::putProperty);
            ResolveIdentityRequest request = identityRequest.build();
            CompletableFuture<? extends T> identity =
                identityMetric == null || metricCollector == null
                ? identityProvider.resolveIdentity(request)
                : MetricUtils.reportDuration(() -> identityProvider.resolveIdentity(request), metricCollector, identityMetric);
            return new SelectedAuthScheme<>(identity, signer, authOption);
        }

        private static SdkMetric<Duration> identityMetric(IdentityProvider<?> identityProvider) {
            Class<?> identityType = identityProvider.identityType();
            if (identityType == AwsCredentialsIdentity.class) {
                return CoreMetric.CREDENTIALS_FETCH_DURATION;
            }
            if (identityType == TokenIdentity.class) {
                return CoreMetric.TOKEN_FETCH_DURATION;
            }
            return null;
        }
    }

    /**
     * The identity of the placeholder auth scheme.
     */
    private static final class UnsetIdentity implements Identity {
    }

    /**
     * The signer of the placeholder auth scheme, which fails as the legacy placeholder's does.
     */
    private static final class UnsetHttpSigner implements HttpSigner<UnsetIdentity> {
        @Override
        public SignedRequest sign(SignRequest<? extends UnsetIdentity> request) {
            throw new IllegalStateException("A signer was not configured.");
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends UnsetIdentity> request) {
            return CompletableFutureUtils.failedFuture(new IllegalStateException("A signer was not configured."));
        }
    }
}
