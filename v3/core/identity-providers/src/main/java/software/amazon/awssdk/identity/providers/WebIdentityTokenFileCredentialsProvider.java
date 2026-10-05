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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.providers.internal.IdentitySettings;
import software.amazon.awssdk.identity.providers.internal.StsCredentialsClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * Credentials from STS {@code AssumeRoleWithWebIdentity} with a web identity token file (for example EKS IRSA), with
 * the V2 name, settings and refresh semantics: the role ARN, token file and session name come from the builder, else
 * {@code aws.roleArn}/{@code AWS_ROLE_ARN}, {@code aws.webIdentityTokenFile}/{@code AWS_WEB_IDENTITY_TOKEN_FILE} and
 * {@code aws.roleSessionName}/{@code AWS_ROLE_SESSION_NAME}; the token file is re-read on every refresh; credentials go
 * stale 1 minute and are prefetched 5 minutes before expiry.
 */
public final class WebIdentityTokenFileCredentialsProvider
    extends CachingIdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "WebIdentityTokenProvider";

    private final String roleArn;
    private final Path tokenFile;
    private final String roleSessionName;
    private final Integer durationSeconds;
    private final RuntimeException configurationError;
    private final StsCredentialsClient sts;

    private WebIdentityTokenFileCredentialsProvider(Builder builder) {
        super(AwsCredentialsIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.STRICT);
        SystemSettings settings = builder.settings;
        String arn = builder.roleArn != null ? builder.roleArn
                                             : IdentitySettings.AWS_ROLE_ARN.value(settings).map(String::strip)
                                                                            .orElse(null);
        String file = builder.webIdentityTokenFile != null
                      ? builder.webIdentityTokenFile.toString()
                      : IdentitySettings.AWS_WEB_IDENTITY_TOKEN_FILE.value(settings).map(String::strip).orElse(null);
        RuntimeException error = null;
        if (file == null) {
            error = SdkClientException.create("Either the environment variable AWS_WEB_IDENTITY_TOKEN_FILE or the "
                                              + "javaproperty aws.webIdentityTokenFile must be set.");
        } else if (arn == null) {
            error = SdkClientException.create("Either the environment variable AWS_ROLE_ARN or the javaproperty "
                                              + "aws.roleArn must be set.");
        }
        this.configurationError = error;
        this.roleArn = arn;
        this.tokenFile = file == null ? null : Paths.get(file);
        this.roleSessionName = builder.roleSessionName != null
                               ? builder.roleSessionName
                               : IdentitySettings.AWS_ROLE_SESSION_NAME.value(settings).map(String::strip)
                                                                        .orElse("aws-sdk-java-" + System.currentTimeMillis());
        this.durationSeconds = builder.durationSeconds;
        this.sts = error != null ? null
                                 : new StsCredentialsClient(builder.region != null ? builder.region
                                                                                   : StsRegions.defaultRegion(settings),
                                                            builder.endpoint, builder.httpClient);
    }

    public static WebIdentityTokenFileCredentialsProvider create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Whether the settings configure web identity credentials. */
    public static boolean isConfigured(SystemSettings settings) {
        return IdentitySettings.AWS_WEB_IDENTITY_TOKEN_FILE.value(settings).isPresent()
               && IdentitySettings.AWS_ROLE_ARN.value(settings).isPresent();
    }

    @Override
    protected RefreshResult<AwsCredentialsIdentity> load() {
        if (configurationError != null) {
            throw configurationError;
        }
        String token;
        try {
            token = Files.readString(tokenFile, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the web identity token file " + tokenFile, e);
        }
        AwsSessionCredentialsIdentity credentials = sts.assumeRoleWithWebIdentity(roleArn, roleSessionName, token,
                                                                                  durationSeconds, PROVIDER_NAME);
        return stsStyle(credentials, credentials.expirationTime().orElse(null));
    }

    @Override
    public void close() {
        if (sts != null) {
            sts.close();
        }
    }

    @Override
    public String toString() {
        return "WebIdentityTokenCredentialsProvider()";
    }

    /** Builder of a {@link WebIdentityTokenFileCredentialsProvider}. */
    public static final class Builder {
        private String roleArn;
        private Path webIdentityTokenFile;
        private String roleSessionName;
        private Integer durationSeconds;
        private String region;
        private URI endpoint;
        private SystemSettings settings = SystemSettings.system();
        private SdkSyncHttpClient httpClient;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        public Builder webIdentityTokenFile(Path webIdentityTokenFile) {
            this.webIdentityTokenFile = webIdentityTokenFile;
            return this;
        }

        public Builder roleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
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

        public WebIdentityTokenFileCredentialsProvider build() {
            return new WebIdentityTokenFileCredentialsProvider(this);
        }
    }
}
