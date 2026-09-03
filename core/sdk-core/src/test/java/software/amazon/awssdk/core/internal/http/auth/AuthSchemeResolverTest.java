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

package software.amazon.awssdk.core.internal.http.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

class AuthSchemeResolverTest {

    private static final String SCHEME_A = "schemeA";
    private static final String SCHEME_B = "schemeB";
    private static final SignerProperty<String> PROPERTY_A =
        SignerProperty.create(AuthSchemeResolverTest.class, "propertyA");
    private static final SignerProperty<String> PROPERTY_B =
        SignerProperty.create(AuthSchemeResolverTest.class, "propertyB");
    private static final SignerProperty<String> UNCHANGED_PROPERTY =
        SignerProperty.create(AuthSchemeResolverTest.class, "unchangedProperty");
    private static final SignerProperty<String> CURRENT_ONLY_PROPERTY =
        SignerProperty.create(AuthSchemeResolverTest.class, "currentOnlyProperty");
    private static final IdentityProperty<String> CURRENT_ONLY_IDENTITY_PROPERTY =
        IdentityProperty.create(AuthSchemeResolverTest.class, "currentOnlyIdentityProperty");

    @Test
    void selectAuthScheme_firstOptionSucceeds_returnsFirstScheme() {
        AuthScheme<Identity> schemeA = createMockAuthScheme();
        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_A, schemeA);

        List<AuthSchemeOption> options = Collections.singletonList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_A);
    }

    @Test
    void selectAuthScheme_firstOptionNoScheme_fallsBackToSecond() {
        AuthScheme<Identity> schemeB = createMockAuthScheme();
        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_B, schemeB);

        List<AuthSchemeOption> options = Arrays.asList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build(),
            AuthSchemeOption.builder().schemeId(SCHEME_B).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
    }

    @Test
    void selectAuthScheme_firstOptionNoIdentityProvider_fallsBackToSecond() {
        AuthScheme<Identity> schemeA = createMockAuthScheme();
        when(schemeA.identityProvider(any())).thenReturn(null);

        AuthScheme<Identity> schemeB = createMockAuthScheme();

        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_A, schemeA);
        authSchemes.put(SCHEME_B, schemeB);

        List<AuthSchemeOption> options = Arrays.asList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build(),
            AuthSchemeOption.builder().schemeId(SCHEME_B).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
    }

    @Test
    void selectAuthScheme_signerThrows_fallsBackToSecond() {
        AuthScheme<Identity> schemeA = createMockAuthScheme();
        when(schemeA.signer()).thenThrow(new RuntimeException("Signer not available"));

        AuthScheme<Identity> schemeB = createMockAuthScheme();

        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_A, schemeA);
        authSchemes.put(SCHEME_B, schemeB);

        List<AuthSchemeOption> options = Arrays.asList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build(),
            AuthSchemeOption.builder().schemeId(SCHEME_B).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
    }

    @Test
    void selectAuthScheme_allOptionsFail_throwsException() {
        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();

        List<AuthSchemeOption> options = Collections.singletonList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build()
        );

        assertThatThrownBy(() -> AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null))
            .isInstanceOf(SdkException.class)
            .hasMessageContaining("Failed to determine how to authenticate");
    }

    @Test
    void mergeProperties_noExistingScheme_returnsOriginal() {
        SelectedAuthScheme<Identity> selected = createSelectedAuthScheme(SCHEME_A);
        ExecutionAttributes attributes = new ExecutionAttributes();

        SelectedAuthScheme<Identity> result = AuthSchemeResolver.mergePreExistingAuthSchemeProperties(
            selected, attributes);

        assertThat(result).isSameAs(selected);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeProperties_withExistingScheme_returnsNewInstance() {
        SelectedAuthScheme<Identity> selected = createSelectedAuthScheme(SCHEME_A);
        SelectedAuthScheme<Identity> existing = createSelectedAuthScheme(SCHEME_B);

        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, existing);

        SelectedAuthScheme<Identity> result = AuthSchemeResolver.mergePreExistingAuthSchemeProperties(
            selected, attributes);

        assertThat(result).isNotSameAs(selected);
        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_A);
    }

    @Test
    void applyInterceptorModifiedProperties_afterInterceptorsNull_doesNothing() {
        TrackingAuthSchemeOption currentOption = new TrackingAuthSchemeOption(option(SCHEME_A, PROPERTY_A, "endpoint"));
        SelectedAuthScheme<Identity> current = createSelectedAuthScheme(currentOption);
        ExecutionAttributes attributes = attributesWithSelectedScheme(current);

        AuthSchemeResolver.applyInterceptorModifiedProperties(current, createSelectedAuthScheme(SCHEME_A), null,
                                                              attributes);

        assertThat(selectedScheme(attributes)).isSameAs(current);
        assertThat(currentOption.toBuilderCalls()).isZero();
    }

    @Test
    void applyInterceptorModifiedProperties_currentSchemeNull_failsFast() {
        SelectedAuthScheme<Identity> before = createSelectedAuthScheme(option(SCHEME_A, PROPERTY_A, "before"));
        SelectedAuthScheme<Identity> after = createSelectedAuthScheme(option(SCHEME_A, PROPERTY_A, "after"));

        assertThatThrownBy(() -> AuthSchemeResolver.applyInterceptorModifiedProperties(
            null, before, after, new ExecutionAttributes()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void applyInterceptorModifiedProperties_beforeInterceptorsNull_doesNotRebuild() {
        TrackingAuthSchemeOption currentOption = new TrackingAuthSchemeOption(option(SCHEME_A, PROPERTY_A, "endpoint"));
        SelectedAuthScheme<Identity> current = createSelectedAuthScheme(currentOption);
        ExecutionAttributes attributes = attributesWithSelectedScheme(current);

        AuthSchemeResolver.applyInterceptorModifiedProperties(
            current, null, createSelectedAuthScheme(option(SCHEME_A, PROPERTY_A, "interceptor")), attributes);

        assertThat(selectedScheme(attributes)).isSameAs(current);
        assertThat(currentOption.toBuilderCalls()).isZero();
    }

    @Test
    void applyInterceptorModifiedProperties_sameOptionSnapshot_doesNotRebuild() {
        TrackingAuthSchemeOption snapshotOption =
            new TrackingAuthSchemeOption(option(SCHEME_A, PROPERTY_A, "same"));
        TrackingAuthSchemeOption currentOption = new TrackingAuthSchemeOption(option(SCHEME_A, PROPERTY_A, "endpoint"));
        SelectedAuthScheme<Identity> current = createSelectedAuthScheme(currentOption);
        ExecutionAttributes attributes = attributesWithSelectedScheme(current);

        AuthSchemeResolver.applyInterceptorModifiedProperties(
            current, createSelectedAuthScheme(snapshotOption), createSelectedAuthScheme(snapshotOption), attributes);

        assertThat(selectedScheme(attributes)).isSameAs(current);
        assertThat(snapshotOption.forEachSignerPropertyCalls()).isZero();
        assertThat(currentOption.toBuilderCalls()).isZero();
    }

    @Test
    void applyInterceptorModifiedProperties_equalDistinctOptions_doesNotRebuild() {
        TrackingAuthSchemeOption currentOption = new TrackingAuthSchemeOption(option(SCHEME_A, PROPERTY_A, "endpoint"));
        SelectedAuthScheme<Identity> current = createSelectedAuthScheme(currentOption);
        SelectedAuthScheme<Identity> before =
            createSelectedAuthScheme(option(SCHEME_A, PROPERTY_A, new String("same")));
        SelectedAuthScheme<Identity> after =
            createSelectedAuthScheme(option(SCHEME_A, PROPERTY_A, new String("same")));
        ExecutionAttributes attributes = attributesWithSelectedScheme(current);

        AuthSchemeResolver.applyInterceptorModifiedProperties(current, before, after, attributes);

        assertThat(selectedScheme(attributes)).isSameAs(current);
        assertThat(currentOption.toBuilderCalls()).isZero();
    }

    @Test
    void applyInterceptorModifiedProperties_multipleChanges_applyOnlyChangesAndPreserveCurrentScheme() {
        AuthSchemeOption beforeOption = AuthSchemeOption.builder()
                                                        .schemeId(SCHEME_A)
                                                        .putSignerProperty(PROPERTY_A, "old-a")
                                                        .putSignerProperty(PROPERTY_B, "old-b")
                                                        .putSignerProperty(UNCHANGED_PROPERTY, "same")
                                                        .build();
        AuthSchemeOption afterOption = AuthSchemeOption.builder()
                                                       .schemeId(SCHEME_A)
                                                       .putSignerProperty(PROPERTY_A, "interceptor-a")
                                                       .putSignerProperty(PROPERTY_B, null)
                                                       .putSignerProperty(UNCHANGED_PROPERTY, new String("same"))
                                                       .build();
        AuthSchemeOption currentOption = AuthSchemeOption.builder()
                                                         .schemeId(SCHEME_B)
                                                         .putSignerProperty(PROPERTY_A, "endpoint-a")
                                                         .putSignerProperty(PROPERTY_B, "endpoint-b")
                                                         .putSignerProperty(UNCHANGED_PROPERTY, "endpoint-same")
                                                         .putSignerProperty(CURRENT_ONLY_PROPERTY, "current-only")
                                                         .putIdentityProperty(CURRENT_ONLY_IDENTITY_PROPERTY,
                                                                              "current-identity")
                                                         .build();
        Identity identity = mock(Identity.class);
        CompletableFuture<Identity> identityFuture = CompletableFuture.completedFuture(identity);
        HttpSigner<Identity> signer = mock(HttpSigner.class);
        SelectedAuthScheme<Identity> current = new SelectedAuthScheme<>(identityFuture, signer, currentOption);
        ExecutionAttributes attributes = attributesWithSelectedScheme(current);

        AuthSchemeResolver.applyInterceptorModifiedProperties(
            current, createSelectedAuthScheme(beforeOption), createSelectedAuthScheme(afterOption), attributes);

        SelectedAuthScheme<?> result = selectedScheme(attributes);
        assertThat(result).isNotSameAs(current);
        assertThat(result.identity()).isSameAs(identityFuture);
        assertThat(result.signer()).isSameAs(signer);
        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
        assertThat(result.authSchemeOption().signerProperty(PROPERTY_A)).isEqualTo("interceptor-a");
        assertThat(result.authSchemeOption().signerProperty(PROPERTY_B)).isNull();
        assertThat(result.authSchemeOption().signerProperty(UNCHANGED_PROPERTY)).isEqualTo("endpoint-same");
        assertThat(result.authSchemeOption().signerProperty(CURRENT_ONLY_PROPERTY)).isEqualTo("current-only");
        assertThat(result.authSchemeOption().identityProperty(CURRENT_ONLY_IDENTITY_PROPERTY))
            .isEqualTo("current-identity");
    }

    @SuppressWarnings("unchecked")
    private AuthScheme<Identity> createMockAuthScheme() {
        AuthScheme<Identity> scheme = mock(AuthScheme.class);
        IdentityProvider<Identity> identityProvider = mock(IdentityProvider.class);
        Identity mockIdentity = mock(Identity.class);
        doReturn(CompletableFuture.completedFuture(mockIdentity))
            .when(identityProvider).resolveIdentity(any(ResolveIdentityRequest.class));
        when(scheme.identityProvider(any())).thenReturn(identityProvider);
        when(scheme.signer()).thenReturn(mock(HttpSigner.class));
        return scheme;
    }

    private SelectedAuthScheme<Identity> createSelectedAuthScheme(String schemeId) {
        return createSelectedAuthScheme(AuthSchemeOption.builder().schemeId(schemeId).build());
    }

    private SelectedAuthScheme<Identity> createSelectedAuthScheme(AuthSchemeOption option) {
        Identity mockIdentity = mock(Identity.class);
        HttpSigner<Identity> mockSigner = mock(HttpSigner.class);
        return new SelectedAuthScheme<>(CompletableFuture.completedFuture(mockIdentity), mockSigner, option);
    }

    private static AuthSchemeOption option(String schemeId, SignerProperty<String> property, String value) {
        return AuthSchemeOption.builder().schemeId(schemeId).putSignerProperty(property, value).build();
    }

    private static ExecutionAttributes attributesWithSelectedScheme(SelectedAuthScheme<?> selectedScheme) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedScheme);
        return attributes;
    }

    private static SelectedAuthScheme<?> selectedScheme(ExecutionAttributes attributes) {
        return attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
    }

    private static final class TrackingAuthSchemeOption implements AuthSchemeOption {
        private final AuthSchemeOption delegate;
        private int forEachSignerPropertyCalls;
        private int toBuilderCalls;

        private TrackingAuthSchemeOption(AuthSchemeOption delegate) {
            this.delegate = delegate;
        }

        @Override
        public String schemeId() {
            return delegate.schemeId();
        }

        @Override
        public <T> T identityProperty(IdentityProperty<T> property) {
            return delegate.identityProperty(property);
        }

        @Override
        public <T> T signerProperty(SignerProperty<T> property) {
            return delegate.signerProperty(property);
        }

        @Override
        public void forEachIdentityProperty(IdentityPropertyConsumer consumer) {
            delegate.forEachIdentityProperty(consumer);
        }

        @Override
        public void forEachSignerProperty(SignerPropertyConsumer consumer) {
            forEachSignerPropertyCalls++;
            delegate.forEachSignerProperty(consumer);
        }

        @Override
        public Builder toBuilder() {
            toBuilderCalls++;
            return delegate.toBuilder();
        }

        private int forEachSignerPropertyCalls() {
            return forEachSignerPropertyCalls;
        }

        private int toBuilderCalls() {
            return toBuilderCalls;
        }
    }
}
