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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;

class ModeledAuthSelectionTest {
    private static final String SIGV4 = AuthSchemeIds.SIGV4;
    private static final String SIGV4A = AuthSchemeIds.SIGV4A;
    private static final String BEARER = AuthSchemeIds.HTTP_BEARER;
    private static final String NO_AUTH = AuthSchemeIds.NO_AUTH;

    @Test
    void serviceSchemesAreSortedWithoutAnAuthTraitAndOrderedWithOne() {
        ModeledAuth.Builder service = ModeledAuth.builder().operationId("ns#Op").addServiceScheme(SIGV4A)
                                                 .addServiceScheme(SIGV4);
        assertEquals(List.of(SIGV4, SIGV4A), service.build().effectiveSchemeIds());
        assertEquals(List.of(SIGV4A, SIGV4), service.serviceAuthTrait(List.of(SIGV4A, SIGV4)).build()
                                                    .effectiveSchemeIds());
    }

    @Test
    void serviceSchemesWithoutAnAuthTraitAreSortedLikeSmithyShapeIds() {
        // Smithy's ShapeId.compareTo orders the full ID case-insensitively, then case-sensitively as a tiebreak.
        ModeledAuth auth = ModeledAuth.builder().operationId("ns#Op").addServiceScheme("test.auth#Beta")
                                      .addServiceScheme("test.auth#alpha").addServiceScheme("test.auth#beta")
                                      .addServiceScheme("Test.auth#gamma").optionalAuth(true).build();
        assertEquals(List.of("test.auth#alpha", "test.auth#Beta", "test.auth#beta", "Test.auth#gamma", NO_AUTH),
                     auth.effectiveSchemeIds());
    }

    @Test
    void operationAuthTraitSelectsAppliedSchemesInItsOrder() {
        ModeledAuth auth = ModeledAuth.builder().operationId("ns#Op").addServiceScheme(SIGV4).addServiceScheme(SIGV4A)
                                      .operationAuthTrait(List.of(SIGV4A, "custom#unapplied", SIGV4)).build();
        assertEquals(List.of(SIGV4A, SIGV4), auth.effectiveSchemeIds());
    }

    @Test
    void noAuthIsAppendedForOptionalAuthOrNoScheme() {
        assertEquals(List.of(SIGV4, NO_AUTH), ModeledAuth.builder().operationId("ns#Op").addServiceScheme(SIGV4)
                                                         .optionalAuth(true).build().effectiveSchemeIds());
        assertEquals(List.of(NO_AUTH), ModeledAuth.builder().operationId("ns#Op").addServiceScheme(SIGV4)
                                                  .operationAuthTrait(List.of()).build().effectiveSchemeIds());
        assertEquals(List.of(NO_AUTH), ModeledAuth.builder().operationId("ns#Op").build().effectiveSchemeIds());
    }

    @Test
    void selectorPicksTheFirstUsableSchemeAndHonorsPreference() {
        List<AuthSchemeOption> options = List.of(option(SIGV4), option(BEARER), option(NO_AUTH));
        IdentityProviders tokens = IdentityProviders.builder().putIdentityProvider(tokenProvider()).build();
        AuthSchemeSelector selector = AuthSchemeSelector.create(List.of(new TokenScheme(), NoAuthAuthScheme.create()));

        // sigv4 is not configured on this client, so bearer (which has a token provider) is chosen.
        assertEquals(BEARER, selector.select("ns#Op", options, tokens).option().schemeId());
        // Without a token provider only the modeled noAuth candidate remains.
        assertEquals(NO_AUTH, selector.select("ns#Op", options, IdentityProviders.empty()).option().schemeId());

        AuthSchemeSelector preferring = AuthSchemeSelector.create(List.of(new TokenScheme(), NoAuthAuthScheme.create()),
                                                                  AuthSchemeSelector.parsePreference(" noAuth , x"));
        assertEquals(NO_AUTH, preferring.select("ns#Op", options, tokens).option().schemeId());
    }

    @Test
    void selectionFailsNamingTheOperationAndEveryRejection() {
        AuthSchemeSelector selector = AuthSchemeSelector.create(List.of(new TokenScheme()));
        SdkClientException e = assertThrows(SdkClientException.class, () -> selector.select(
            "ns#Op", List.of(option(SIGV4), option(BEARER)), IdentityProviders.empty()));
        assertTrue(e.getMessage().contains("ns#Op"), e.getMessage());
        assertTrue(e.getMessage().contains(SIGV4 + " is not supported"), e.getMessage());
        assertTrue(e.getMessage().contains(BEARER + " has no identity provider"), e.getMessage());
    }

    @Test
    void selectedSchemeResolvesSignsAndReportsRefreshability() {
        IdentityProvider<TokenIdentity> provider = tokenProvider();
        SelectedAuthScheme<?> selected = AuthSchemeSelector.create(List.of(new TokenScheme())).select(
            "ns#Op", List.of(option(BEARER)), IdentityProviders.builder().putIdentityProvider(provider).build());
        assertSame(provider, selected.identityProvider());
        assertFalse(selected.invalidate(TokenIdentity.create("t")));
    }

    @Test
    void sign_withPayloadSha256_passesItWithTheOptionPropertiesAndDefaultModeIsEmpty() {
        SignerProperty<String> custom = SignerProperty.create(ModeledAuthSelectionTest.class, "Custom");
        List<SignRequest<?>> seen = new ArrayList<>();
        AuthScheme<TokenIdentity> recording = new AuthScheme<>() {
            @Override
            public String schemeId() {
                return BEARER;
            }

            @Override
            public Class<TokenIdentity> identityType() {
                return TokenIdentity.class;
            }

            @Override
            public HttpSigner<TokenIdentity> signer() {
                return request -> {
                    seen.add(request);
                    return SignedRequest.unsigned(request.request());
                };
            }
        };
        AuthSchemeOption option = AuthSchemeOption.builder().schemeId(BEARER).putSignerProperty(custom, "v").build();
        SelectedAuthScheme<TokenIdentity> selected =
            new SelectedAuthScheme<>(recording, option, tokenProvider());
        SdkHttpRequest request = SdkHttpRequest.builder().method("PUT").host("test.com").build();

        selected.sign(request, TokenIdentity.create("t"), "abc123");
        selected.sign(request, TokenIdentity.create("t"));

        assertEquals("abc123", seen.get(0).property(HttpSigner.PAYLOAD_SHA256));
        assertEquals("v", seen.get(0).property(custom));
        assertNull(seen.get(1).property(HttpSigner.PAYLOAD_SHA256));
        assertEquals(Optional.empty(), selected.payloadSigningMode(request, TokenIdentity.create("t")));
    }

    private static AuthSchemeOption option(String schemeId) {
        return AuthSchemeOption.builder().schemeId(schemeId).build();
    }

    private static IdentityProvider<TokenIdentity> tokenProvider() {
        return new IdentityProvider<>() {
            @Override
            public Class<TokenIdentity> identityType() {
                return TokenIdentity.class;
            }

            @Override
            public CompletableFuture<TokenIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(TokenIdentity.create("t"));
            }
        };
    }

    private static final class TokenScheme implements AuthScheme<TokenIdentity> {
        @Override
        public String schemeId() {
            return BEARER;
        }

        @Override
        public Class<TokenIdentity> identityType() {
            return TokenIdentity.class;
        }

        @Override
        public HttpSigner<TokenIdentity> signer() {
            return request -> SignedRequest.unsigned(request.request());
        }
    }
}
