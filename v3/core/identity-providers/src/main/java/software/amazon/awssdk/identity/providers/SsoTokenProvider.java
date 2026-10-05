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
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.providers.internal.Json;
import software.amazon.awssdk.identity.providers.internal.LoadedCredentials;
import software.amazon.awssdk.identity.providers.internal.SsoClient;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * The IAM Identity Center (SSO) access token cached by {@code aws sso login} in {@code ~/.aws/sso/cache}, with the V2
 * {@code SsoOidcTokenProvider} / legacy SSO token semantics:
 *
 * <ul>
 *   <li>{@code sso-session} configuration: the cache file is named after the SHA-1 of the session name. A token within
 *       5 minutes of expiry is refreshed with SSO-OIDC {@code CreateToken} (refresh-token grant) and written back to the
 *       cache file; if that refresh fails, the old token is used until it expires, then resolution fails.</li>
 *   <li>legacy configuration (no session): the file is named after the SHA-1 of the start URL and is never refreshed;
 *       an expired token fails with the V2 "run aws sso login" error.</li>
 * </ul>
 *
 * <p>It serves the {@code smithy.api#httpBearerAuth} scheme directly and supplies the token of
 * {@link SsoCredentialsProvider}.
 */
public final class SsoTokenProvider extends CachingIdentityProvider<TokenIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "SsoTokenProvider";

    private static final Duration STALE = Duration.ofMinutes(1);
    private static final Duration PREFETCH = Duration.ofMinutes(5);

    private final String sessionName;
    private final String startUrl;
    private final Path cacheFile;
    private final SsoClient oidc;

    private SsoTokenProvider(Builder builder) {
        super(TokenIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.STRICT);
        this.sessionName = builder.sessionName;
        this.startUrl = builder.startUrl;
        if (sessionName == null && startUrl == null) {
            throw new IllegalArgumentException("An SSO token provider needs an sso-session name or a start URL.");
        }
        Path directory = builder.cacheDirectory != null
                         ? builder.cacheDirectory
                         : Path.of(ProfileFileLocation.userHome(builder.settings), ".aws", "sso", "cache");
        this.cacheFile = directory.resolve(sha1(sessionName != null ? sessionName : startUrl) + ".json");
        this.oidc = sessionName == null ? null
                                        : new SsoClient(Objects.requireNonNull(builder.region, "region"), null,
                                                        builder.oidcEndpoint, builder.httpClient);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The token cache file this provider reads. */
    public Path cacheFile() {
        return cacheFile;
    }

    @Override
    protected RefreshResult<TokenIdentity> load() {
        Map<String, Object> cached = readCache();
        String accessToken = Json.string(cached, "accessToken");
        Instant expiresAt = LoadedCredentials.instant(Json.string(cached, "expiresAt"), "the SSO token cache");
        if (accessToken == null || expiresAt == null) {
            throw SdkClientException.create("The SSO token cache file " + cacheFile + " has no accessToken or "
                                            + "expiresAt.");
        }
        Instant now = clock.instant();
        if (sessionName == null) {
            if (!now.isBefore(expiresAt)) {
                throw expired();
            }
            return result(accessToken, expiresAt);
        }
        if (now.plus(PREFETCH).isBefore(expiresAt)) {
            return result(accessToken, expiresAt);
        }
        try {
            SsoClient.RefreshedToken refreshed = refresh(cached, now);
            return result(refreshed.accessToken(), refreshed.expiresAt());
        } catch (RuntimeException e) {
            if (now.isBefore(expiresAt)) {
                return result(accessToken, expiresAt);
            }
            throw SdkClientException.create("The SSO session token has expired and could not be refreshed. To "
                                            + "refresh this SSO session run 'aws sso login' with the corresponding "
                                            + "profile.", e);
        }
    }

    private SsoClient.RefreshedToken refresh(Map<String, Object> cached, Instant now) {
        String clientId = Json.string(cached, "clientId");
        String clientSecret = Json.string(cached, "clientSecret");
        String refreshToken = Json.string(cached, "refreshToken");
        Instant registrationExpiresAt = LoadedCredentials.instant(Json.string(cached, "registrationExpiresAt"),
                                                                  "the SSO token cache");
        if (clientId == null || clientSecret == null || refreshToken == null
            || (registrationExpiresAt != null && !now.isBefore(registrationExpiresAt))) {
            throw SdkClientException.create("The SSO token cannot be refreshed: the client registration or refresh "
                                            + "token is missing or expired.");
        }
        SsoClient.RefreshedToken refreshed = oidc.refreshToken(clientId, clientSecret, refreshToken, now);
        Map<String, Object> updated = new LinkedHashMap<>(cached);
        updated.put("accessToken", refreshed.accessToken());
        updated.put("expiresAt", refreshed.expiresAt().toString());
        updated.put("refreshToken", refreshed.refreshToken());
        writeCache(updated);
        return refreshed;
    }

    private RefreshResult<TokenIdentity> result(String token, Instant expiresAt) {
        return RefreshResult.of(TokenIdentity.create(token, expiresAt, PROVIDER_NAME), expiresAt.minus(STALE),
                                sessionName == null ? null : expiresAt.minus(PREFETCH));
    }

    private Map<String, Object> readCache() {
        String content;
        try {
            content = Files.readString(cacheFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw SdkClientException.create("Unable to load the SSO token from " + cacheFile + ". To refresh this SSO "
                                            + "session run 'aws sso login' with the corresponding profile.", e);
        }
        return Json.parseObject(content);
    }

    private void writeCache(Map<String, Object> content) {
        try {
            Path temp = Files.createTempFile(cacheFile.getParent(), "sso-token", ".tmp");
            Files.writeString(temp, Json.write(content), StandardCharsets.UTF_8);
            Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to update the SSO token cache " + cacheFile, e);
        }
    }

    private SdkClientException expired() {
        return SdkClientException.create("The SSO session associated with this profile has expired or is otherwise "
                                         + "invalid. To refresh this SSO session run aws sso login with the "
                                         + "corresponding profile.");
    }

    private static String sha1(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                                                         .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is unavailable", e);
        }
    }

    @Override
    public void close() {
        if (oidc != null) {
            oidc.close();
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "(" + (sessionName != null ? "session=" + sessionName : "startUrl=" + startUrl) + ")";
    }

    /** Builder of an {@link SsoTokenProvider}. */
    public static final class Builder {
        private String sessionName;
        private String startUrl;
        private String region;
        private Path cacheDirectory;
        private URI oidcEndpoint;
        private SystemSettings settings = SystemSettings.system();
        private SdkSyncHttpClient httpClient;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The {@code sso-session} name; enables refresh. */
        public Builder sessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }

        /** The start URL (legacy configuration without a session). */
        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        /** The SSO region (the SSO-OIDC region of refreshes). */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /** The token cache directory; {@code ~/.aws/sso/cache} by default. */
        public Builder cacheDirectory(Path cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            return this;
        }

        /** The SSO-OIDC endpoint, overriding the regional endpoint. */
        public Builder oidcEndpoint(URI oidcEndpoint) {
            this.oidcEndpoint = oidcEndpoint;
            return this;
        }

        /** The settings the home directory is read from. */
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

        public SsoTokenProvider build() {
            return new SsoTokenProvider(this);
        }
    }
}
