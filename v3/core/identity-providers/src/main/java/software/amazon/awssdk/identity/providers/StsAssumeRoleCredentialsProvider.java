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
import software.amazon.awssdk.identity.providers.internal.StsCredentialsClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * Credentials of an assumed IAM role, from STS {@code AssumeRole} signed with source credentials, with the V2 name and
 * refresh semantics (stale 1 minute and prefetched 5 minutes before expiry). This is the provider of profiles with
 * {@code role_arn} and {@code source_profile} or {@code credential_source}.
 */
public final class StsAssumeRoleCredentialsProvider extends CachingIdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "StsAssumeRoleCredentialsProvider";

    private final IdentityProvider<? extends AwsCredentialsIdentity> source;
    private final String roleArn;
    private final String roleSessionName;
    private final String externalId;
    private final Integer durationSeconds;
    private final StsCredentialsClient sts;

    private StsAssumeRoleCredentialsProvider(Builder builder) {
        super(AwsCredentialsIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.STRICT);
        this.source = Objects.requireNonNull(builder.source, "sourceCredentialsProvider");
        this.roleArn = Objects.requireNonNull(builder.roleArn, "roleArn");
        this.roleSessionName = builder.roleSessionName != null ? builder.roleSessionName
                                                               : "aws-sdk-java-" + System.currentTimeMillis();
        this.externalId = builder.externalId;
        this.durationSeconds = builder.durationSeconds;
        String region = builder.region != null ? builder.region : StsRegions.defaultRegion(builder.settings);
        this.sts = new StsCredentialsClient(region, builder.endpoint, builder.httpClient);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected RefreshResult<AwsCredentialsIdentity> load() {
        AwsCredentialsIdentity sourceCredentials;
        try {
            sourceCredentials = source.resolveIdentity().join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException cause ? cause : e;
        }
        AwsSessionCredentialsIdentity credentials = sts.assumeRole(sourceCredentials, roleArn, roleSessionName,
                                                                   externalId, durationSeconds, PROVIDER_NAME);
        return stsStyle(credentials, credentials.expirationTime().orElse(null));
    }

    @Override
    public void close() {
        sts.close();
        if (source instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to close the source credentials provider", e);
            }
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "(roleArn=" + roleArn + ")";
    }

    /** Builder of a {@link StsAssumeRoleCredentialsProvider}. */
    public static final class Builder {
        private IdentityProvider<? extends AwsCredentialsIdentity> source;
        private String roleArn;
        private String roleSessionName;
        private String externalId;
        private Integer durationSeconds;
        private String region;
        private URI endpoint;
        private SystemSettings settings = SystemSettings.system();
        private SdkSyncHttpClient httpClient;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The credentials {@code AssumeRole} is signed with. Required; closed with this provider. */
        public Builder sourceCredentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> source) {
            this.source = source;
            return this;
        }

        /** The role to assume. Required. */
        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        /** The role session name; {@code aws-sdk-java-<millis>} by default. */
        public Builder roleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        /** The STS region; by default the default region chain's, else {@code us-east-1}. */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /** The STS endpoint, overriding the regional endpoint. */
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /** The settings the default region is read from. */
        public Builder settings(SystemSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
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

        public StsAssumeRoleCredentialsProvider build() {
            return new StsAssumeRoleCredentialsProvider(this);
        }
    }
}
