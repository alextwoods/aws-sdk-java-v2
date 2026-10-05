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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.providers.internal.Ec2MetadataClient;
import software.amazon.awssdk.identity.providers.internal.Json;
import software.amazon.awssdk.identity.providers.internal.LoadedCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * Credentials of the EC2 instance profile, from instance metadata, with the V2 name, settings and refresh semantics
 * (IMDS client behavior as described by the instance metadata settings: endpoint, mode, IMDSv2 token with IMDSv1
 * fallback, disable switches, timeout).
 *
 * <p>Credentials go stale {@code staleTime} (default 1 second) before expiry and are prefetched halfway to expiry (at
 * least 5 minutes after loading; 1 hour when there is no expiry). As in V2 this provider is statically stable: when a
 * refresh of stale credentials fails, the stale credentials are kept for a short jittered period instead of failing
 * calls, unless they were {@linkplain #invalidate rejected}.
 */
public final class InstanceProfileCredentialsProvider extends CachingIdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "InstanceProfileCredentialsProvider";

    private static final String SECURITY_CREDENTIALS_PATH = "/latest/meta-data/iam/security-credentials/";

    private final Ec2MetadataClient imds;
    private final Duration staleTime;

    private InstanceProfileCredentialsProvider(Builder builder) {
        super(AwsCredentialsIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.ALLOW);
        this.staleTime = builder.staleTime;
        SystemSettings settings = builder.settings;
        Supplier<ProfileFile> file = builder.profileFile != null ? builder.profileFile
                                                                 : () -> ProfileFile.defaultProfileFile(settings);
        String name = builder.profileName != null ? builder.profileName : ProfileFileLocation.profileName(settings);
        Supplier<Optional<Profile>> profile = () -> file.get().profile(name);
        this.imds = new Ec2MetadataClient(settings, profile, builder.endpoint, builder.httpClient);
    }

    public static InstanceProfileCredentialsProvider create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected RefreshResult<AwsCredentialsIdentity> load() {
        if (imds.disabled()) {
            throw SdkClientException.create("IMDS credentials have been disabled by environment variable or system "
                                            + "property.");
        }
        try {
            Optional<String> token = imds.token();
            String[] roles = imds.get(SECURITY_CREDENTIALS_PATH, token).strip().split("\n");
            if (roles.length == 0 || roles[0].isBlank()) {
                throw SdkClientException.create("Unable to load credentials path");
            }
            String document = imds.get(SECURITY_CREDENTIALS_PATH + roles[0].strip(), token);
            AwsCredentialsIdentity credentials = LoadedCredentials.credentials(Json.parseObject(document), "Token",
                                                                               null, PROVIDER_NAME, "IMDS");
            Instant expiration = credentials.expirationTime().orElse(null);
            return RefreshResult.of(credentials, expiration == null ? null : expiration.minus(staleTime),
                                    prefetchTime(expiration));
        } catch (RuntimeException e) {
            throw SdkClientException.create("Failed to load credentials from IMDS.", e);
        }
    }

    private Instant prefetchTime(Instant expiration) {
        Instant now = clock.instant();
        if (expiration == null) {
            return now.plus(Duration.ofMinutes(60));
        }
        Duration untilExpiration = Duration.between(now, expiration);
        if (untilExpiration.isNegative()) {
            return null;
        }
        Duration half = untilExpiration.dividedBy(2);
        return now.plus(half.compareTo(Duration.ofMinutes(5)) > 0 ? half : Duration.ofMinutes(5));
    }

    @Override
    public void close() {
        imds.close();
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "()";
    }

    /** Builder of an {@link InstanceProfileCredentialsProvider}. */
    public static final class Builder {
        private SystemSettings settings = SystemSettings.system();
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private String endpoint;
        private Duration staleTime = Duration.ofSeconds(1);
        private SdkSyncHttpClient httpClient;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The settings IMDS configuration is read from; the process's by default. */
        public Builder settings(SystemSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
            return this;
        }

        /** The profile file IMDS configuration is read from; the default profile file by default. */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /** The profile IMDS configuration is read from; the active profile by default. */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /** The IMDS endpoint, overriding every endpoint setting. */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /** How long before expiry credentials go stale; default 1 second. */
        public Builder staleTime(Duration staleTime) {
            this.staleTime = Objects.requireNonNull(staleTime, "staleTime");
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

        /** Where loads run; a virtual thread per load by default. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public InstanceProfileCredentialsProvider build() {
            return new InstanceProfileCredentialsProvider(this);
        }
    }
}
