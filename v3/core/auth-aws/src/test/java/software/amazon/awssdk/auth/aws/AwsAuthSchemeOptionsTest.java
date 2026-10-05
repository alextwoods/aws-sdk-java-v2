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

package software.amazon.awssdk.auth.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.aws.s3express.S3ExpressIdentityProvider;
import software.amazon.awssdk.auth.aws.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

class AwsAuthSchemeOptionsTest {
    @Test
    void modeledOptionsCarryTraitAndClientSignerProperties() {
        ModeledAuth auth = ModeledAuth.builder().operationId("com.amazonaws.s3#PutObject")
                                      .addServiceScheme(AuthSchemeIds.SIGV4, Map.of("name", "s3"))
                                      .addServiceScheme(AuthSchemeIds.SIGV4A, Map.of("name", "s3"))
                                      .serviceAuthTrait(List.of(AuthSchemeIds.SIGV4, AuthSchemeIds.SIGV4A))
                                      .unsignedPayload(true)
                                      .build();
        List<AuthSchemeOption> options = AwsAuthSchemeOptions.modeled(
            auth, "us-west-2", null, Map.of(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false));

        assertEquals(2, options.size());
        AuthSchemeOption sigv4 = options.get(0);
        assertEquals(AuthSchemeIds.SIGV4, sigv4.schemeId());
        assertEquals("s3", sigv4.signerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME));
        assertEquals("us-west-2", sigv4.signerProperty(AwsV4HttpSigner.REGION_NAME));
        assertEquals(false, sigv4.signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED));
        assertEquals(false, sigv4.signerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE));
        assertEquals(RegionSet.create("us-west-2"), options.get(1).signerProperty(AwsV4aHttpSigner.REGION_SET));
    }

    @Test
    void modeled_withS3SignerDefaults_disablesPayloadSigningAndUriRewritingLikeV2() {
        ModeledAuth auth = ModeledAuth.builder().operationId("com.amazonaws.s3#PutObject")
                                      .addServiceScheme(AuthSchemeIds.SIGV4, Map.of("name", "s3"))
                                      .addServiceScheme(AuthSchemeIds.SIGV4_S3EXPRESS, Map.of("name", "s3express"))
                                      .serviceAuthTrait(List.of(AuthSchemeIds.SIGV4, AuthSchemeIds.SIGV4_S3EXPRESS))
                                      .build();
        List<AuthSchemeOption> options =
            AwsAuthSchemeOptions.modeled(auth, "us-west-2", null, AwsAuthSchemeOptions.s3SignerDefaults());

        for (AuthSchemeOption option : options) {
            assertEquals(false, option.signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED));
            assertEquals(false, option.signerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE));
            assertEquals(false, option.signerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH));
        }
    }

    @Test
    void anonymousOptionHasNoSignerProperties() {
        ModeledAuth auth = ModeledAuth.builder().operationId("ns#Op").addServiceScheme(AuthSchemeIds.SIGV4,
                                                                                       Map.of("name", "svc"))
                                      .optionalAuth(true).build();
        AuthSchemeOption noAuth = AwsAuthSchemeOptions.modeled(auth, "us-east-1").get(1);
        assertEquals(AuthSchemeIds.NO_AUTH, noAuth.schemeId());
        assertNull(noAuth.signerProperty(AwsV4HttpSigner.REGION_NAME));
    }

    @Test
    void s3ExpressIsSelectedWithItsProviderAndSessionsAreCachedAndRecreatedWhenRejected() {
        AtomicInteger sessions = new AtomicInteger();
        S3ExpressIdentityProvider express = S3ExpressIdentityProvider.builder()
            .baseCredentials(fixed(AwsCredentialsIdentity.create("akid", "secret")))
            .sessionLoader((bucket, base) -> S3ExpressSessionCredentials.create(
                "session-" + sessions.incrementAndGet(), "s", bucket, Instant.now().plusSeconds(300)))
            .executor(Runnable::run)
            .build();
        AuthSchemeOption option = AuthSchemeOption.builder().schemeId(AuthSchemeIds.SIGV4_S3EXPRESS)
                                                  .putIdentityProperty(S3ExpressIdentityProvider.BUCKET, "b--x-s3")
                                                  .build();
        var selected = AuthSchemeSelector.create(AwsAuthSchemeOptions.s3Schemes())
                                         .select("s3#GetObject", List.of(option),
                                                 IdentityProviders.builder().putIdentityProvider(express).build());
        S3ExpressSessionCredentials first = (S3ExpressSessionCredentials) selected.resolveIdentity().join();
        assertEquals(first, selected.resolveIdentity().join());
        assertEquals(1, sessions.get());

        selected.invalidate(first);
        S3ExpressSessionCredentials refreshed = (S3ExpressSessionCredentials) selected.resolveIdentity().join();
        assertEquals("session-2", refreshed.accessKeyId());
    }

    private static IdentityProvider<AwsCredentialsIdentity> fixed(AwsCredentialsIdentity credentials) {
        return new IdentityProvider<>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(credentials);
            }
        };
    }
}
