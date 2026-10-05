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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * The bearer token of a profile's {@code sso_session}, with the V2 name: an {@link SsoTokenProvider} for the
 * {@code [sso-session]} section the active profile names. {@link #create()} is also the V2
 * {@code DefaultAwsTokenProvider} chain, which has this single source.
 */
public final class ProfileTokenProvider implements RefreshableIdentityProvider<TokenIdentity>, AutoCloseable {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final SystemSettings settings;
    private final SdkSyncHttpClient httpClient;
    private final URI oidcEndpoint;
    private final Path cacheDirectory;
    private SsoTokenProvider delegate;

    private ProfileTokenProvider(Builder builder) {
        this.settings = builder.settings;
        this.profileFile = builder.profileFile != null ? builder.profileFile
                                                       : () -> ProfileFile.defaultProfileFile(settings);
        this.profileName = builder.profileName != null ? builder.profileName : ProfileFileLocation.profileName(settings);
        this.httpClient = builder.httpClient;
        this.oidcEndpoint = builder.oidcEndpoint;
        this.cacheDirectory = builder.cacheDirectory;
    }

    public static ProfileTokenProvider create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Class<TokenIdentity> identityType() {
        return TokenIdentity.class;
    }

    @Override
    public CompletableFuture<TokenIdentity> resolveIdentity(ResolveIdentityRequest request) {
        SsoTokenProvider current;
        synchronized (this) {
            current = delegate;
        }
        if (current != null) {
            return current.resolveIdentity(request);
        }
        // The first resolution reads the profile file: keep that blocking read off the caller thread.
        return CompletableFuture.supplyAsync(this::delegate, command -> Thread.ofVirtual().start(command))
                                .thenCompose(provider -> provider.resolveIdentity(request));
    }

    private synchronized SsoTokenProvider delegate() {
        if (delegate == null) {
            ProfileFile file = profileFile.get();
            Profile profile = file.profile(profileName).orElseThrow(
                () -> SdkClientException.create("Profile '" + profileName + "' does not exist."));
            String sessionName = profile.property(ProfileProperty.SSO_SESSION).orElseThrow(
                () -> SdkClientException.create("Profile '" + profileName + "' does not have an sso_session "
                                                + "configured for bearer tokens."));
            Profile session = file.section(ProfileFile.SSO_SESSION_SECTION, sessionName).orElseThrow(
                () -> SdkClientException.create("Sso-session section not found with sso-session title " + sessionName
                                                + "."));
            delegate = SsoTokenProvider.builder()
                                       .sessionName(sessionName)
                                       .region(session.property(ProfileProperty.SSO_REGION).orElseThrow(
                                           () -> SdkClientException.create("sso_region is not configured for "
                                                                           + "sso-session " + sessionName)))
                                       .startUrl(session.property(ProfileProperty.SSO_START_URL).orElse(null))
                                       .settings(settings)
                                       .httpClient(httpClient)
                                       .oidcEndpoint(oidcEndpoint)
                                       .cacheDirectory(cacheDirectory)
                                       .build();
        }
        return delegate;
    }

    @Override
    public void invalidate(Identity identity) {
        SsoTokenProvider current;
        synchronized (this) {
            current = delegate;
        }
        if (current != null) {
            current.invalidate(identity);
        }
    }

    @Override
    public void close() {
        SsoTokenProvider current;
        synchronized (this) {
            current = delegate;
        }
        if (current != null) {
            current.close();
        }
    }

    @Override
    public String toString() {
        return "ProfileTokenProvider(profileName=" + profileName + ")";
    }

    /** Builder of a {@link ProfileTokenProvider}. */
    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private SystemSettings settings = SystemSettings.system();
        private SdkSyncHttpClient httpClient;
        private URI oidcEndpoint;
        private Path cacheDirectory;

        private Builder() {
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Builder settings(SystemSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
            return this;
        }

        public Builder httpClient(SdkSyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder oidcEndpoint(URI oidcEndpoint) {
            this.oidcEndpoint = oidcEndpoint;
            return this;
        }

        public Builder cacheDirectory(Path cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            return this;
        }

        public ProfileTokenProvider build() {
            return new ProfileTokenProvider(this);
        }
    }
}
