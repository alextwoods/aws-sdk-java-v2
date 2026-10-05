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

package software.amazon.awssdk.auth.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;

/**
 * The auth scheme an {@link AuthSchemeSelector} chose for one call, with the option and identity provider to use.
 *
 * <p>Identity is resolved per attempt through {@link #resolveIdentity()}, so a provider that refreshes supplies the
 * refreshed identity to the next signing (Requirements 13.16, 13.17); {@link #invalidate} reports a rejected identity.
 *
 * @param <IdentityT> the identity type
 */
public final class SelectedAuthScheme<IdentityT extends Identity> {
    private final AuthScheme<IdentityT> scheme;
    private final AuthSchemeOption option;
    private final IdentityProvider<? extends IdentityT> identityProvider;

    public SelectedAuthScheme(AuthScheme<IdentityT> scheme, AuthSchemeOption option,
                              IdentityProvider<? extends IdentityT> identityProvider) {
        this.scheme = Objects.requireNonNull(scheme, "scheme");
        this.option = Objects.requireNonNull(option, "option");
        this.identityProvider = Objects.requireNonNull(identityProvider, "identityProvider");
    }

    public AuthScheme<IdentityT> scheme() {
        return scheme;
    }

    public AuthSchemeOption option() {
        return option;
    }

    public IdentityProvider<? extends IdentityT> identityProvider() {
        return identityProvider;
    }

    /** Resolves the identity for this option's identity properties. */
    public CompletableFuture<? extends IdentityT> resolveIdentity() {
        return identityProvider.resolveIdentity(option.identityRequest());
    }

    /**
     * Reports that the service rejected {@code identity}; the next {@link #resolveIdentity()} of a refreshable provider
     * returns a refreshed identity or fails with the refresh failure.
     *
     * @return whether the provider's contract permits refresh
     */
    public boolean invalidate(Identity identity) {
        if (identityProvider instanceof RefreshableIdentityProvider<?> refreshable) {
            refreshable.invalidate(identity);
            return true;
        }
        return false;
    }

    /** Signs {@code request} with {@code identity} and this option's signer properties. */
    public SignedRequest sign(SdkHttpRequest request, IdentityT identity) {
        return sign(request, identity, null);
    }

    /**
     * Signs {@code request} with {@code identity}, this option's signer properties and, if not {@code null}, the
     * {@link HttpSigner#PAYLOAD_SHA256} of its streaming payload.
     */
    public SignedRequest sign(SdkHttpRequest request, IdentityT identity, String payloadSha256) {
        return scheme.signer().sign(signRequest(request, identity).putProperty(HttpSigner.PAYLOAD_SHA256,
                                                                               payloadSha256)
                                                                  .build());
    }

    /**
     * How this scheme's signer will cover the payload of {@code request} signed with {@code identity} and this
     * option's signer properties ({@link HttpSigner#payloadSigningMode}).
     */
    public Optional<PayloadSigningMode> payloadSigningMode(SdkHttpRequest request, IdentityT identity) {
        return scheme.signer().payloadSigningMode(signRequest(request, identity).build());
    }

    private SignRequest.Builder<IdentityT> signRequest(SdkHttpRequest request, IdentityT identity) {
        return SignRequest.builder(identity).request(request).putProperties(option.signerProperties());
    }

    @Override
    public String toString() {
        return "SelectedAuthScheme[" + option.schemeId() + "]";
    }
}
