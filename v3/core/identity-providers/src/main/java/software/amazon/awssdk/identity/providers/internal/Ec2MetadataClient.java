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

package software.amazon.awssdk.identity.providers.internal;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * The EC2 instance metadata (IMDS) client of the instance-profile credentials and region providers, with the V2
 * behavior:
 *
 * <ul>
 *   <li>endpoint: {@code AWS_EC2_METADATA_SERVICE_ENDPOINT}, else the profile's {@code ec2_metadata_service_endpoint},
 *       else by mode ({@code AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE} / profile, {@code IPv4} by default):
 *       {@code http://169.254.169.254} or {@code http://[fd00:ec2::254]};</li>
 *   <li>IMDSv2: a session token from {@code PUT /latest/api/token} (TTL 21600 s) is sent as
 *       {@code x-aws-ec2-metadata-token}. A 400 token response fails; any other token failure falls back to IMDSv1
 *       unless {@code AWS_EC2_METADATA_V1_DISABLED} / {@code ec2_metadata_v1_disabled} is true;</li>
 *   <li>{@code AWS_EC2_METADATA_DISABLED=true} disables IMDS;</li>
 *   <li>connection and read timeout {@code AWS_METADATA_SERVICE_TIMEOUT} seconds (default 1).</li>
 * </ul>
 *
 * <p>Not a public API.
 */
public final class Ec2MetadataClient implements AutoCloseable {
    public static final String TOKEN_PATH = "/latest/api/token";
    public static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    public static final String TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    public static final String TOKEN_TTL = "21600";

    private final SystemSettings settings;
    private final Supplier<Optional<Profile>> profile;
    private final String endpointOverride;
    private final Duration timeout;
    private final IdentityHttpClient http;

    public Ec2MetadataClient(SystemSettings settings, Supplier<Optional<Profile>> profile, String endpointOverride,
                             SdkSyncHttpClient httpClient) {
        this.settings = settings;
        this.profile = profile;
        this.endpointOverride = endpointOverride;
        this.timeout = Duration.ofMillis(timeoutMillis(settings));
        this.http = IdentityHttpClient.create(httpClient, timeout);
    }

    /** Whether IMDS is disabled by {@code AWS_EC2_METADATA_DISABLED}. */
    public boolean disabled() {
        return IdentitySettings.AWS_EC2_METADATA_DISABLED.value(settings).map(v -> v.strip().equalsIgnoreCase("true"))
                                                         .orElse(false);
    }

    /** The IMDS base endpoint, without a trailing slash. */
    public String endpoint() {
        if (endpointOverride != null) {
            return strip(endpointOverride);
        }
        Optional<String> configured = IdentitySettings.AWS_EC2_METADATA_SERVICE_ENDPOINT.value(settings)
                                                                                         .or(() -> profileValue(
                                                                                             ProfileProperty
                                                                                                 .EC2_METADATA_SERVICE_ENDPOINT));
        if (configured.isPresent()) {
            return strip(configured.get().strip());
        }
        String mode = IdentitySettings.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.value(settings)
                                                                            .or(() -> profileValue(
                                                                                ProfileProperty
                                                                                    .EC2_METADATA_SERVICE_ENDPOINT_MODE))
                                                                            .orElse("IPv4").strip();
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "ipv4" -> "http://169.254.169.254";
            case "ipv6" -> "http://[fd00:ec2::254]";
            default -> throw SdkClientException.create("Unsupported EC2 metadata endpoint mode: " + mode
                                                       + ". Use IPv4 or IPv6.");
        };
    }

    /** An IMDSv2 session token, or empty to use IMDSv1. */
    public Optional<String> token() {
        SdkHttpRequest request = IdentityHttpClient.request("PUT", URI.create(endpoint() + TOKEN_PATH))
                                                   .putHeader(TOKEN_TTL_HEADER, TOKEN_TTL)
                                                   .build();
        IdentityHttpClient.Response response;
        try {
            response = http.send(request, timeout);
        } catch (SdkClientException e) {
            return fallBackToV1(e);
        }
        if (response.isSuccessful()) {
            return Optional.of(response.body().strip());
        }
        if (response.statusCode() == 400) {
            throw SdkClientException.create("Unable to fetch metadata token.");
        }
        return fallBackToV1(SdkClientException.create("The metadata token request failed with HTTP "
                                                      + response.statusCode()));
    }

    private Optional<String> fallBackToV1(Exception failure) {
        if (v1Disabled()) {
            throw SdkClientException.create("Failed to retrieve IMDS token, and fallback to IMDS v1 is disabled via the "
                                            + IdentitySettings.AWS_EC2_METADATA_V1_DISABLED.describe() + ", or "
                                            + ProfileProperty.EC2_METADATA_V1_DISABLED
                                            + " configuration file profile setting.", failure);
        }
        return Optional.empty();
    }

    /**
     * The body of metadata {@code path}.
     *
     * @throws SdkClientException if IMDS is unreachable or returns an error status
     */
    public String get(String path, Optional<String> token) {
        SdkHttpRequest.Builder request = IdentityHttpClient.request("GET", URI.create(endpoint() + path));
        token.ifPresent(t -> request.putHeader(TOKEN_HEADER, t));
        IdentityHttpClient.Response response = http.send(request.build(), timeout);
        if (!response.isSuccessful()) {
            throw SdkClientException.create("Failed to load " + path + " from IMDS: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private boolean v1Disabled() {
        return IdentitySettings.AWS_EC2_METADATA_V1_DISABLED.value(settings)
                                                            .or(() -> profileValue(
                                                                ProfileProperty.EC2_METADATA_V1_DISABLED))
                                                            .map(v -> v.strip().equalsIgnoreCase("true"))
                                                            .orElse(false);
    }

    private Optional<String> profileValue(String property) {
        return profile.get().flatMap(p -> p.property(property)).filter(v -> !v.isBlank());
    }

    private static long timeoutMillis(SystemSettings settings) {
        String value = IdentitySettings.AWS_METADATA_SERVICE_TIMEOUT.value(settings).orElse("1").strip();
        try {
            return Math.round(Double.parseDouble(value) * 1000);
        } catch (NumberFormatException e) {
            throw SdkClientException.create(IdentitySettings.AWS_METADATA_SERVICE_TIMEOUT.describe()
                                            + " must be a number of seconds, but was '" + value + "'.");
        }
    }

    private static String strip(String endpoint) {
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    @Override
    public void close() {
        http.close();
    }
}
