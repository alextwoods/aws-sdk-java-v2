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
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.ProfileCredentialsResolver;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * Credentials a profile of the shared configuration files configures, with the V2 name and precedence (static keys,
 * session keys, {@code credential_process}, assumed roles through {@code source_profile}/{@code credential_source}, web
 * identity and SSO; see the {@code identity-providers} package description).
 *
 * <p>The profile file is read, and the profile's provider created, on the first resolution (off the caller thread),
 * then reused; the provider is closed with this one.
 */
public final class ProfileCredentialsProvider implements RefreshableIdentityProvider<AwsCredentialsIdentity>,
                                                         AutoCloseable {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final ProfileCredentialsResolver.Context context;
    private final Executor executor;
    private CompletableFuture<IdentityProvider<? extends AwsCredentialsIdentity>> delegate;

    private ProfileCredentialsProvider(Builder builder) {
        SystemSettings settings = builder.settings;
        this.profileFile = builder.profileFile != null ? builder.profileFile
                                                       : () -> ProfileFile.defaultProfileFile(settings);
        this.profileName = builder.profileName != null ? builder.profileName : ProfileFileLocation.profileName(settings);
        this.context = new ProfileCredentialsResolver.Context(settings, builder.httpClient, builder.stsEndpoint,
                                                              builder.ssoPortalEndpoint, builder.ssoOidcEndpoint,
                                                              builder.ssoCacheDirectory, builder.imdsEndpoint,
                                                              builder.clock, builder.executor);
        this.executor = builder.executor;
    }

    public static ProfileCredentialsProvider create() {
        return builder().build();
    }

    /** The provider of profile {@code profileName} of the default profile file. */
    public static ProfileCredentialsProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The profile this provider reads. */
    public String profileName() {
        return profileName;
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        return delegate().thenCompose(provider -> provider.resolveIdentity(request))
                         .thenApply(identity -> (AwsCredentialsIdentity) identity);
    }

    private synchronized CompletableFuture<IdentityProvider<? extends AwsCredentialsIdentity>> delegate() {
        if (delegate == null) {
            Supplier<IdentityProvider<? extends AwsCredentialsIdentity>> create = () -> {
                ProfileFile file = profileFile.get();
                return new ProfileCredentialsResolver(file, context).provider(profileName).orElseThrow(
                    () -> SdkClientException.create("Profile file contained no credentials for profile '"
                                                    + profileName + "': " + file));
            };
            delegate = executor != null ? CompletableFuture.supplyAsync(create, executor)
                                        : CompletableFuture.supplyAsync(create, ProfileExecutor.INSTANCE);
        }
        return delegate;
    }

    @Override
    public void invalidate(Identity identity) {
        CompletableFuture<IdentityProvider<? extends AwsCredentialsIdentity>> current;
        synchronized (this) {
            current = delegate;
        }
        if (current != null && current.isDone() && !current.isCompletedExceptionally()
            && current.join() instanceof RefreshableIdentityProvider<?> refreshable) {
            refreshable.invalidate(identity);
        }
    }

    @Override
    public void close() {
        CompletableFuture<IdentityProvider<? extends AwsCredentialsIdentity>> current;
        synchronized (this) {
            current = delegate;
        }
        if (current != null && current.isDone() && !current.isCompletedExceptionally()
            && current.join() instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw SdkClientException.create("Unable to close the credentials provider of profile " + profileName, e);
            }
        }
    }

    @Override
    public String toString() {
        return "ProfileCredentialsProvider(profileName=" + profileName + ")";
    }

    /** Runs profile loading on a virtual thread so resolution never blocks its caller. */
    private static final class ProfileExecutor implements Executor {
        static final ProfileExecutor INSTANCE = new ProfileExecutor();

        @Override
        public void execute(Runnable command) {
            Thread.ofVirtual().name("aws-sdk-profile-credentials").start(command);
        }
    }

    /** Builder of a {@link ProfileCredentialsProvider}. */
    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private SystemSettings settings = SystemSettings.system();
        private SdkSyncHttpClient httpClient;
        private URI stsEndpoint;
        private URI ssoPortalEndpoint;
        private URI ssoOidcEndpoint;
        private Path ssoCacheDirectory;
        private String imdsEndpoint;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The profile file; the default profile file by default. */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /** The profile file. */
        public Builder profileFile(ProfileFile profileFile) {
            Objects.requireNonNull(profileFile, "profileFile");
            this.profileFile = () -> profileFile;
            return this;
        }

        /** The profile; the active profile ({@code aws.profile} / {@code AWS_PROFILE}, else default) by default. */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /** The JVM property and environment sources; the process's by default. */
        public Builder settings(SystemSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
            return this;
        }

        /** A caller-owned HTTP client for STS, SSO, container and IMDS calls. */
        public Builder httpClient(SdkSyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /** The STS endpoint of role profiles, overriding the regional endpoint. */
        public Builder stsEndpoint(URI stsEndpoint) {
            this.stsEndpoint = stsEndpoint;
            return this;
        }

        /** The SSO portal endpoint of SSO profiles, overriding the regional endpoint. */
        public Builder ssoPortalEndpoint(URI ssoPortalEndpoint) {
            this.ssoPortalEndpoint = ssoPortalEndpoint;
            return this;
        }

        /** The SSO-OIDC endpoint of SSO profiles, overriding the regional endpoint. */
        public Builder ssoOidcEndpoint(URI ssoOidcEndpoint) {
            this.ssoOidcEndpoint = ssoOidcEndpoint;
            return this;
        }

        /** The SSO token cache directory; {@code ~/.aws/sso/cache} by default. */
        public Builder ssoCacheDirectory(Path ssoCacheDirectory) {
            this.ssoCacheDirectory = ssoCacheDirectory;
            return this;
        }

        /** The IMDS endpoint of {@code credential_source = Ec2InstanceMetadata}. */
        public Builder imdsEndpoint(String imdsEndpoint) {
            this.imdsEndpoint = imdsEndpoint;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /** Where profile loading and credential loads run; virtual threads by default. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public ProfileCredentialsProvider build() {
            return new ProfileCredentialsProvider(this);
        }
    }
}
