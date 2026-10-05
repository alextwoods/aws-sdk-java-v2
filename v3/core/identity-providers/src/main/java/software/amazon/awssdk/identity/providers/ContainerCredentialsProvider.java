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
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.providers.internal.IdentityHttpClient;
import software.amazon.awssdk.identity.providers.internal.IdentitySettings;
import software.amazon.awssdk.identity.providers.internal.Json;
import software.amazon.awssdk.identity.providers.internal.LoadedCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * Credentials from the ECS/EKS container credentials endpoint, with the V2 name, settings and refresh semantics:
 *
 * <ul>
 *   <li>{@code AWS_CONTAINER_CREDENTIALS_RELATIVE_URI} (resolved against {@code AWS_CONTAINER_SERVICE_ENDPOINT},
 *       default {@code http://169.254.170.2}) wins over {@code AWS_CONTAINER_CREDENTIALS_FULL_URI}; a full URI must be
 *       HTTPS, or HTTP to a loopback address or {@code 169.254.170.2}, {@code 169.254.170.23} or
 *       {@code fd00:ec2::23};</li>
 *   <li>the {@code Authorization} header is the content of {@code AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE}, else
 *       {@code AWS_CONTAINER_AUTHORIZATION_TOKEN};</li>
 *   <li>a load is attempted up to 3 times on server errors and transport failures;</li>
 *   <li>credentials go stale 1 minute before expiry and are prefetched 15 minutes before expiry (at most 1 hour after
 *       loading).</li>
 * </ul>
 */
public final class ContainerCredentialsProvider extends CachingIdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "ContainerCredentialsProvider";

    private static final String DEFAULT_SERVICE_ENDPOINT = "http://169.254.170.2";
    private static final Set<String> ALLOWED_HTTP_HOSTS = Set.of("169.254.170.2", "169.254.170.23", "fd00:ec2::23",
                                                                 "[fd00:ec2::23]");
    private static final int MAX_ATTEMPTS = 3;

    private final SystemSettings settings;
    private final IdentityHttpClient http;

    private ContainerCredentialsProvider(Builder builder) {
        super(AwsCredentialsIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.STRICT);
        this.settings = builder.settings;
        this.http = IdentityHttpClient.create(builder.httpClient, Duration.ofSeconds(1));
    }

    public static ContainerCredentialsProvider create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Whether the settings configure a container credentials endpoint. */
    public static boolean isConfigured(SystemSettings settings) {
        return IdentitySettings.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.value(settings).isPresent()
               || IdentitySettings.AWS_CONTAINER_CREDENTIALS_FULL_URI.value(settings).isPresent();
    }

    @Override
    protected RefreshResult<AwsCredentialsIdentity> load() {
        URI endpoint = endpoint();
        SdkHttpRequest.Builder request = IdentityHttpClient.request("GET", endpoint);
        authorizationToken().ifPresent(token -> request.putHeader("Authorization", token));
        IdentityHttpClient.Response response = null;
        RuntimeException failure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                response = http.send(request.build(), Duration.ofSeconds(5));
                failure = null;
                if (response.statusCode() < 500) {
                    break;
                }
            } catch (RuntimeException e) {
                failure = e;
            }
        }
        if (failure != null) {
            throw SdkClientException.create("Failed to load credentials from the container endpoint.", failure);
        }
        if (!response.isSuccessful()) {
            throw SdkClientException.create("Failed to load credentials from the container endpoint: HTTP "
                                            + response.statusCode() + ".");
        }
        AwsCredentialsIdentity credentials = LoadedCredentials.credentials(Json.parseObject(response.body()), "Token",
                                                                           null, PROVIDER_NAME, "the container endpoint");
        Instant expiration = credentials.expirationTime().orElse(null);
        Instant now = clock.instant();
        Instant oneHour = now.plus(Duration.ofHours(1));
        if (expiration == null) {
            return RefreshResult.of(credentials, null, oneHour);
        }
        Instant prefetch = expiration.minus(Duration.ofMinutes(15));
        return RefreshResult.of(credentials, expiration.minus(Duration.ofMinutes(1)),
                                prefetch.isBefore(oneHour) ? prefetch : oneHour);
    }

    private URI endpoint() {
        Optional<String> relative = IdentitySettings.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.value(settings);
        if (relative.isPresent()) {
            String base = IdentitySettings.AWS_CONTAINER_SERVICE_ENDPOINT.value(settings).orElse(DEFAULT_SERVICE_ENDPOINT);
            return URI.create(base + relative.get().strip());
        }
        Optional<String> full = IdentitySettings.AWS_CONTAINER_CREDENTIALS_FULL_URI.value(settings);
        if (full.isEmpty()) {
            throw SdkClientException.create("Cannot fetch credentials from container - neither "
                                            + IdentitySettings.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable()
                                            + " or "
                                            + IdentitySettings.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.environmentVariable()
                                            + " environment variables are set.");
        }
        URI uri = URI.create(full.get().strip());
        validate(uri);
        return uri;
    }

    private static void validate(URI uri) {
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return;
        }
        String host = uri.getHost();
        if ("http".equalsIgnoreCase(uri.getScheme()) && host != null
            && (ALLOWED_HTTP_HOSTS.contains(host) || allLoopback(host))) {
            return;
        }
        throw SdkClientException.create("The full URI (" + uri + ") contained within environment variable "
                                        + IdentitySettings.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable()
                                        + " has an invalid host. Host should resolve to a loopback address or have the "
                                        + "full URI be HTTPS.");
    }

    private static boolean allLoopback(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (!address.isLoopbackAddress()) {
                    return false;
                }
            }
            return addresses.length > 0;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private Optional<String> authorizationToken() {
        Optional<String> file = IdentitySettings.AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE.value(settings);
        String token;
        if (file.isPresent()) {
            try {
                token = Files.readString(Paths.get(file.get().strip()), StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read the container authorization token file", e);
            }
        } else {
            token = IdentitySettings.AWS_CONTAINER_AUTHORIZATION_TOKEN.value(settings).map(String::strip).orElse(null);
        }
        if (token != null && (token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0)) {
            throw SdkClientException.create("The container authorization token contains a newline.");
        }
        return Optional.ofNullable(token);
    }

    @Override
    public void close() {
        http.close();
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "()";
    }

    /** Builder of a {@link ContainerCredentialsProvider}. */
    public static final class Builder {
        private SystemSettings settings = SystemSettings.system();
        private SdkSyncHttpClient httpClient;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The settings the endpoint and token are read from; the process's by default. */
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

        /** Where loads run; a virtual thread per load by default. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public ContainerCredentialsProvider build() {
            return new ContainerCredentialsProvider(this);
        }
    }
}
