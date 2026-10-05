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

package software.amazon.awssdk.identity.providers;

import java.net.URI;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.providers.internal.SsoClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;

/**
 * Role credentials from IAM Identity Center (SSO {@code GetRoleCredentials}) with an SSO access token, with the V2 name
 * and refresh semantics (stale 1 minute and prefetched 5 minutes before expiry). This is the provider of profiles with
 * {@code sso_account_id} and {@code sso_role_name}.
 */
public final class SsoCredentialsProvider extends CachingIdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "SsoCredentialsProvider";

    private final IdentityProvider<? extends TokenIdentity> tokenProvider;
    private final String accountId;
    private final String roleName;
    private final SsoClient sso;

    private SsoCredentialsProvider(Builder builder) {
        super(AwsCredentialsIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.STRICT);
        this.tokenProvider = Objects.requireNonNull(builder.tokenProvider, "tokenProvider");
        this.accountId = Objects.requireNonNull(builder.accountId, "accountId");
        this.roleName = Objects.requireNonNull(builder.roleName, "roleName");
        this.sso = new SsoClient(Objects.requireNonNull(builder.region, "region"), builder.endpoint, null,
                                 builder.httpClient);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected RefreshResult<AwsCredentialsIdentity> load() {
        TokenIdentity token;
        try {
            token = tokenProvider.resolveIdentity().join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException cause ? cause : e;
        }
        AwsSessionCredentialsIdentity credentials = sso.getRoleCredentials(token.token(), accountId, roleName,
                                                                           PROVIDER_NAME);
        return stsStyle(credentials, credentials.expirationTime().orElse(null));
    }

    @Override
    public void close() {
        sso.close();
        if (tokenProvider instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to close the SSO token provider", e);
            }
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "(accountId=" + accountId + ", roleName=" + roleName + ")";
    }

    /** Builder of an {@link SsoCredentialsProvider}. */
    public static final class Builder {
        private IdentityProvider<? extends TokenIdentity> tokenProvider;
        private String accountId;
        private String roleName;
        private String region;
        private URI endpoint;
        private SdkSyncHttpClient httpClient;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The SSO access token (an {@link SsoTokenProvider}). Required; closed with this provider. */
        public Builder tokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
            this.tokenProvider = tokenProvider;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        /** The SSO region. Required. */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /** The SSO portal endpoint, overriding the regional endpoint. */
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /** A caller-owned HTTP client; by default the installed sync implementation (SDK-owned). */
        public Builder httpClient(SdkSyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public SsoCredentialsProvider build() {
            return new SsoCredentialsProvider(this);
        }
    }
}
