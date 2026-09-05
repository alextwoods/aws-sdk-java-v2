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

package software.amazon.awssdk.bridge.smithyjava.auth;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.context.Context;

/**
 * Adapts a v2 {@link IdentityProvider} to smithy-java's
 * {@link software.amazon.smithy.java.auth.api.identity.IdentityResolver}.
 *
 * <h2>Why this is bridged rather than translated</h2>
 *
 * <p>This is one of the components deliberately bridged rather than replaced. smithy-java's
 * {@code aws-credential-chain} ships only environment-variable and system-property resolvers, whereas
 * v2's {@code DefaultCredentialsProvider} chain covers profile files, SSO, container credentials,
 * IMDS, process credentials, web identity tokens, and STS assume-role — including all of their
 * caching, prefetch, and refresh behavior. Reproducing that is not in scope for the prototype, and
 * losing it would make the client unusable in practice.
 *
 * <p>The v2 provider is also where credential caching lives, so keeping it means credential resolution
 * stays a cache hit on the hot path even though smithy resolves identity once per <em>attempt</em>
 * rather than once per execution.
 */
@SdkProtectedApi
public final class V2IdentityResolver
        implements software.amazon.smithy.java.auth.api.identity.IdentityResolver<
                software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity> {

    private final IdentityProvider<? extends AwsCredentialsIdentity> v2Provider;

    public V2IdentityResolver(IdentityProvider<? extends AwsCredentialsIdentity> v2Provider) {
        this.v2Provider = v2Provider;
    }

    @Override
    public IdentityResult<software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity> resolveIdentity(
            Context requestProperties
    ) {
        // v2's resolveIdentity(ResolveIdentityRequest) properties are not forwarded; see
        // compatability_issues.md 4.2.
        AwsCredentialsIdentity v2Identity = CompletableFutureUtils.joinLikeSync(v2Provider.resolveIdentity());
        return IdentityResult.of(new SmithyCredentialsAdapter(v2Identity));
    }

    @Override
    public Class<software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity> identityType() {
        return software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity.class;
    }

    /**
     * Wraps a v2 credentials identity as a smithy one. This is a thin view rather than a copy so that
     * per-attempt identity resolution does not allocate credential strings.
     */
    private static final class SmithyCredentialsAdapter
            implements software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity {

        private final AwsCredentialsIdentity v2;

        private SmithyCredentialsAdapter(AwsCredentialsIdentity v2) {
            this.v2 = v2;
        }

        @Override
        public String accessKeyId() {
            return v2.accessKeyId();
        }

        @Override
        public String secretAccessKey() {
            return v2.secretAccessKey();
        }

        @Override
        public String sessionToken() {
            return v2 instanceof software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity session
                    ? session.sessionToken()
                    : null;
        }

        @Override
        public String accountId() {
            return v2.accountId().orElse(null);
        }

        /**
         * The v2 identity this view wraps. Used by the endpoint bridge, which needs to hand a v2
         * identity back to the v2 rules engine for account-ID-based routing.
         *
         * @return the v2 identity.
         */
        AwsCredentialsIdentity v2Identity() {
            return v2;
        }
    }

    /**
     * Recovers the v2 identity from a smithy identity that this resolver produced.
     *
     * <p>The endpoint bridge needs this: smithy resolves identity before the endpoint and stashes it in
     * {@code CallContext.IDENTITY}, but the v2 rules engine expects a v2 {@code AwsCredentialsIdentity}
     * inside a {@code SelectedAuthScheme}.
     *
     * @param smithyIdentity a smithy AWS credentials identity, possibly not produced by this resolver.
     * @return the underlying v2 identity, or a converted copy if it came from elsewhere.
     */
    public static AwsCredentialsIdentity toV2(
            software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity smithyIdentity
    ) {
        if (smithyIdentity instanceof SmithyCredentialsAdapter adapter) {
            return adapter.v2Identity();
        }
        String sessionToken = smithyIdentity.sessionToken();
        if (sessionToken != null) {
            return software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity
                    .builder()
                    .accessKeyId(smithyIdentity.accessKeyId())
                    .secretAccessKey(smithyIdentity.secretAccessKey())
                    .sessionToken(sessionToken)
                    .accountId(smithyIdentity.accountId())
                    .build();
        }
        return AwsCredentialsIdentity.builder()
                                     .accessKeyId(smithyIdentity.accessKeyId())
                                     .secretAccessKey(smithyIdentity.secretAccessKey())
                                     .accountId(smithyIdentity.accountId())
                                     .build();
    }
}
