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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * The anonymous {@code smithy.api#noAuth} scheme, with the V2 name: it needs no configured identity provider and its
 * signer returns the request unchanged. It is a candidate only when the model makes it one (an operation with
 * {@code @optionalAuth}, or with no effective auth scheme), never as a silent fallback.
 */
public final class NoAuthAuthScheme implements AuthScheme<Identity> {
    private static final NoAuthAuthScheme INSTANCE = new NoAuthAuthScheme();
    private static final Identity ANONYMOUS = new Identity() {
        @Override
        public Optional<String> providerName() {
            return Optional.of("Anonymous");
        }

        @Override
        public String toString() {
            return "AnonymousIdentity";
        }
    };
    private static final IdentityProvider<Identity> PROVIDER = new IdentityProvider<>() {
        @Override
        public Class<Identity> identityType() {
            return Identity.class;
        }

        @Override
        public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(ANONYMOUS);
        }
    };
    private static final HttpSigner<Identity> SIGNER = request -> SignedRequest.unsigned(request.request());

    private NoAuthAuthScheme() {
    }

    public static NoAuthAuthScheme create() {
        return INSTANCE;
    }

    /** The anonymous identity the scheme resolves. */
    public static Identity anonymousIdentity() {
        return ANONYMOUS;
    }

    @Override
    public String schemeId() {
        return AuthSchemeIds.NO_AUTH;
    }

    @Override
    public Class<Identity> identityType() {
        return Identity.class;
    }

    /** Always the anonymous provider, whatever is configured. */
    @Override
    public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
        return Optional.of(PROVIDER);
    }

    @Override
    public HttpSigner<Identity> signer() {
        return SIGNER;
    }

    @Override
    public String toString() {
        return "NoAuthAuthScheme";
    }
}
