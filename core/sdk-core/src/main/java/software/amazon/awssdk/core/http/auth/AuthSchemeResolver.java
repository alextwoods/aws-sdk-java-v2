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

package software.amazon.awssdk.core.http.auth;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.spi.identity.RequestIdentityProviderResolver;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Logger;

/**
 * Shared utility for selecting auth schemes from a list of options.
 */
@SdkProtectedApi
public final class AuthSchemeResolver {

    private static final Logger LOG = Logger.loggerFor(AuthSchemeResolver.class);

    private AuthSchemeResolver() {
    }

    /**
     * Resolve an auth scheme from execution attributes, applying any identity provider overrides.
     * This is a convenience method for use by interceptors that need to resolve an auth scheme
     * outside of the normal pipeline flow (e.g., presign interceptors).
     *
     * @param request The SDK request (may contain credential overrides)
     * @param executionAttributes The execution attributes containing auth scheme resolution inputs
     * @return The selected auth scheme
     */
    public static SelectedAuthScheme<? extends Identity> resolveAuthScheme(
            SdkRequest request, ExecutionAttributes executionAttributes) {
        AuthSchemeOptionsResolver optionsResolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER);
        Map<String, AuthScheme<?>> authSchemes =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES);
        IdentityProviders identityProviders =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);

        RequestIdentityProviderResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_RESOLVER);
        if (resolver != null) {
            identityProviders = resolver.resolve(request, identityProviders, executionAttributes);
        }

        List<AuthSchemeOption> authOptions = optionsResolver.resolve(request, executionAttributes);
        return selectAuthScheme(authOptions, authSchemes, identityProviders, null);
    }

    /**
     * Select an auth scheme from the given options.
     *
     * @param authOptions List of auth scheme options to try in order
     * @param authSchemes Map of available auth schemes
     * @param identityProviders Identity providers to use for resolving identity
     * @param metricCollector Optional metric collector for recording identity fetch duration
     * @return The selected auth scheme
     * @throws SdkException if no auth scheme could be selected
     */
    public static SelectedAuthScheme<? extends Identity> selectAuthScheme(
            List<AuthSchemeOption> authOptions,
            Map<String, AuthScheme<?>> authSchemes,
            IdentityProviders identityProviders,
            MetricCollector metricCollector) {

        List<Supplier<String>> discardedReasons = new ArrayList<>();

        for (AuthSchemeOption authOption : authOptions) {
            AuthScheme<?> authScheme = authSchemes.get(authOption.schemeId());
            SelectedAuthScheme<? extends Identity> selectedAuthScheme = trySelectAuthScheme(
                authOption, authScheme, identityProviders, discardedReasons, metricCollector);

            if (selectedAuthScheme != null) {
                if (!discardedReasons.isEmpty()) {
                    LOG.debug(() -> String.format("%s auth will be used, discarded: '%s'",
                        authOption.schemeId(),
                        discardedReasons.stream().map(Supplier::get).collect(Collectors.joining(", "))));
                }
                return selectedAuthScheme;
            }
        }

        throw SdkException.builder()
            .message("Failed to determine how to authenticate the user: " +
                     discardedReasons.stream().map(Supplier::get).collect(Collectors.joining(", ")))
            .build();
    }

    /**
     * Merge properties from any pre-existing auth scheme into the selected one.
     *
     * After auth scheme resolution produces a fresh selectedAuthScheme, this method ensures that any signer properties
     * explicitly set by interceptors (e.g., signing region override) take priority over the resolved values.
     */
    public static <T extends Identity> SelectedAuthScheme<T> mergePreExistingAuthSchemeProperties(
            SelectedAuthScheme<T> selectedAuthScheme,
            ExecutionAttributes executionAttributes) {

        // The "existing" auth scheme is what's currently on SELECTED_AUTH_SCHEME - potentially modified by interceptors.
        SelectedAuthScheme<?> existingAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        if (existingAuthScheme == null) {
            return selectedAuthScheme;
        }

        // Snapshot taken before interceptors ran — used to detect what interceptors changed.
        SelectedAuthScheme<?> authSchemeBeforeInterceptors =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_SNAPSHOT_PRE_INTERCEPTORS);

        // If no interceptor modified the auth scheme option, skip the diff logic. Still merge existing properties with
        // putIfAbsent so that properties from the initial placeholder (e.g., REGION_NAME) carry over to the
        // freshly resolved scheme.
        if (authSchemeBeforeInterceptors != null &&
            authSchemeBeforeInterceptors.authSchemeOption() == existingAuthScheme.authSchemeOption()) {
            // Fast path: the placeholder scheme created by legacy signer attribute writes (signing name/region)
            // usually contributes nothing, because the resolved option already carries every property. Skip the
            // option rebuild entirely in that case — this runs on every request.
            if (!hasPropertyAbsentFrom(existingAuthScheme.authSchemeOption(), selectedAuthScheme.authSchemeOption())) {
                return selectedAuthScheme;
            }
            AuthSchemeOption.Builder mergedOption = selectedAuthScheme.authSchemeOption().toBuilder();
            existingAuthScheme.authSchemeOption().forEachSignerProperty(mergedOption::putSignerPropertyIfAbsent);
            existingAuthScheme.authSchemeOption().forEachIdentityProperty(mergedOption::putIdentityPropertyIfAbsent);
            return new SelectedAuthScheme<>(
                selectedAuthScheme.identity(),
                selectedAuthScheme.signer(),
                mergedOption.build()
            );
        }

        // Start with the freshly resolved auth scheme as the base.
        AuthSchemeOption.Builder mergedOption = selectedAuthScheme.authSchemeOption().toBuilder();

        // For each signer property on the interceptor-modified scheme:
        // If the interceptor changed it (differs from pre-interceptor snapshot), apply interceptor override
        // If unchanged (same as before interceptors) only add if not already on the resolved scheme
        existingAuthScheme.authSchemeOption().forEachSignerProperty(new AuthSchemeOption.SignerPropertyConsumer() {
            @Override
            public <S> void accept(SignerProperty<S> key, S value) {
                if (wasModifiedByInterceptor(authSchemeBeforeInterceptors, key, value)) {
                    mergedOption.putSignerProperty(key, value);
                } else {
                    mergedOption.putSignerPropertyIfAbsent(key, value);
                }
            }
        });

        existingAuthScheme.authSchemeOption().forEachIdentityProperty(mergedOption::putIdentityPropertyIfAbsent);

        return new SelectedAuthScheme<>(
            selectedAuthScheme.identity(),
            selectedAuthScheme.signer(),
            mergedOption.build()
        );
    }

    /**
     * Returns true if {@code source} carries any signer or identity property that is absent from {@code target} —
     * i.e. whether a putIfAbsent merge of source into target would change anything. Allocation-free probe used to
     * skip the per-request option rebuild in the common case.
     */
    private static boolean hasPropertyAbsentFrom(AuthSchemeOption source, AuthSchemeOption target) {
        PropertyAbsenceProbe probe = new PropertyAbsenceProbe(target);
        source.forEachSignerProperty(probe);
        if (probe.foundAbsent) {
            return true;
        }
        source.forEachIdentityProperty(probe);
        return probe.foundAbsent;
    }

    /**
     * Builds a {@link ResolveIdentityRequest} from an option's identity properties, returning a shared
     * empty request when the option has none (the common case).
     */
    private static final class IdentityRequestBuilderConsumer implements AuthSchemeOption.IdentityPropertyConsumer {
        private static final ResolveIdentityRequest EMPTY = ResolveIdentityRequest.builder().build();

        private ResolveIdentityRequest.Builder builder;

        @Override
        public <T> void accept(IdentityProperty<T> key, T value) {
            if (builder == null) {
                builder = ResolveIdentityRequest.builder();
            }
            builder.putProperty(key, value);
        }

        ResolveIdentityRequest buildOrEmpty() {
            return builder == null ? EMPTY : builder.build();
        }
    }

    private static final class PropertyAbsenceProbe
            implements AuthSchemeOption.SignerPropertyConsumer, AuthSchemeOption.IdentityPropertyConsumer {
        private final AuthSchemeOption target;
        private boolean foundAbsent;

        PropertyAbsenceProbe(AuthSchemeOption target) {
            this.target = target;
        }

        @Override
        public <T> void accept(SignerProperty<T> key, T value) {
            if (!foundAbsent && target.signerProperty(key) == null) {
                foundAbsent = true;
            }
        }

        @Override
        public <T> void accept(IdentityProperty<T> key, T value) {
            if (!foundAbsent && target.identityProperty(key) == null) {
                foundAbsent = true;
            }
        }
    }

    /**
     * Returns true if the given property value differs from what it was before interceptors ran,
     * meaning an interceptor explicitly changed it.
     */
    private static <T> boolean wasModifiedByInterceptor(SelectedAuthScheme<?> authSchemeBeforeInterceptors,
                                                         SignerProperty<T> key, T currentValue) {
        if (authSchemeBeforeInterceptors == null) {
            return false;
        }
        T originalValue = authSchemeBeforeInterceptors.authSchemeOption().signerProperty(key);
        return !Objects.equals(originalValue, currentValue);
    }

    /**
     * Re-applies interceptor-modified signer properties onto the current auth scheme.
     * Called after endpoint resolution, which may have overwritten properties that interceptors set.
     */
    public static void applyInterceptorModifiedProperties(SelectedAuthScheme<?> currentScheme,
                                                          SelectedAuthScheme<?> authSchemeBeforeInterceptors,
                                                          SelectedAuthScheme<?> afterInterceptors,
                                                          ExecutionAttributes attrs) {
        if (afterInterceptors == null) {
            return;
        }

        Objects.requireNonNull(currentScheme.authSchemeOption());
        Objects.requireNonNull(afterInterceptors.authSchemeOption());
        if (authSchemeBeforeInterceptors == null ||
            authSchemeBeforeInterceptors.authSchemeOption() == afterInterceptors.authSchemeOption()) {
            return;
        }
        doApplyInterceptorModifiedProperties(currentScheme, authSchemeBeforeInterceptors, afterInterceptors, attrs);
    }

    private static <T extends Identity> void doApplyInterceptorModifiedProperties(
            SelectedAuthScheme<T> currentScheme,
            SelectedAuthScheme<?> authSchemeBeforeInterceptors,
            SelectedAuthScheme<?> afterInterceptors,
            ExecutionAttributes attrs) {

        InterceptorModifiedPropertyConsumer modifiedProperties =
            new InterceptorModifiedPropertyConsumer(currentScheme.authSchemeOption(), authSchemeBeforeInterceptors);
        afterInterceptors.authSchemeOption().forEachSignerProperty(modifiedProperties);

        AuthSchemeOption mergedOption = modifiedProperties.buildOrNull();
        if (mergedOption != null) {
            attrs.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                               new SelectedAuthScheme<>(currentScheme.identity(), currentScheme.signer(), mergedOption));
        }
    }

    private static final class InterceptorModifiedPropertyConsumer
            implements AuthSchemeOption.SignerPropertyConsumer {
        private final AuthSchemeOption currentOption;
        private final SelectedAuthScheme<?> authSchemeBeforeInterceptors;
        private AuthSchemeOption.Builder mergedOption;

        private InterceptorModifiedPropertyConsumer(AuthSchemeOption currentOption,
                                                      SelectedAuthScheme<?> authSchemeBeforeInterceptors) {
            this.currentOption = currentOption;
            this.authSchemeBeforeInterceptors = authSchemeBeforeInterceptors;
        }

        @Override
        public <T> void accept(SignerProperty<T> key, T value) {
            if (wasModifiedByInterceptor(authSchemeBeforeInterceptors, key, value)) {
                if (mergedOption == null) {
                    mergedOption = currentOption.toBuilder();
                }
                mergedOption.putSignerProperty(key, value);
            }
        }

        private AuthSchemeOption buildOrNull() {
            return mergedOption == null ? null : mergedOption.build();
        }
    }

    private static <T extends Identity> SelectedAuthScheme<T> trySelectAuthScheme(
            AuthSchemeOption authOption,
            AuthScheme<T> authScheme,
            IdentityProviders identityProviders,
            List<Supplier<String>> discardedReasons,
            MetricCollector metricCollector) {

        if (authScheme == null) {
            discardedReasons.add(() -> String.format("'%s' is not enabled for this request.", authOption.schemeId()));
            return null;
        }

        IdentityProvider<T> identityProvider = authScheme.identityProvider(identityProviders);
        if (identityProvider == null) {
            discardedReasons.add(() -> String.format("'%s' does not have an identity provider configured.",
                authOption.schemeId()));
            return null;
        }

        HttpSigner<T> signer;
        try {
            signer = authScheme.signer();
        } catch (RuntimeException e) {
            discardedReasons.add(() -> String.format("'%s' signer could not be retrieved: %s",
                authOption.schemeId(), e.getMessage()));
            return null;
        }

        // Most auth options carry no identity properties; share the empty request instead of
        // building one per call.
        IdentityRequestBuilderConsumer identityRequestBuilder = new IdentityRequestBuilderConsumer();
        authOption.forEachIdentityProperty(identityRequestBuilder);

        CompletableFuture<? extends T> identity = resolveIdentity(
            identityProvider, identityRequestBuilder.buildOrEmpty(), metricCollector);

        return new SelectedAuthScheme<>(identity, signer, authOption);
    }

    private static <T extends Identity> CompletableFuture<? extends T> resolveIdentity(
            IdentityProvider<T> identityProvider,
            ResolveIdentityRequest request,
            MetricCollector metricCollector) {

        SdkMetric<Duration> metric = getIdentityMetric(identityProvider);
        if (metric == null || metricCollector == null) {
            return identityProvider.resolveIdentity(request);
        }
        return MetricUtils.reportDuration(() -> identityProvider.resolveIdentity(request), metricCollector, metric);
    }

    private static SdkMetric<Duration> getIdentityMetric(IdentityProvider<?> identityProvider) {
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
