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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * The IAM Identity Center calls of the SSO providers: SSO {@code GetRoleCredentials} (REST-JSON,
 * {@code https://portal.sso.<region>.amazonaws.com}) and SSO-OIDC {@code CreateToken} with the refresh-token grant
 * ({@code https://oidc.<region>.amazonaws.com}). Identity providers cannot depend on the generated SSO artifacts, so
 * these calls are implemented natively. Not a public API.
 */
public final class SsoClient implements AutoCloseable {
    private final String region;
    private final URI portalEndpoint;
    private final URI oidcEndpoint;
    private final IdentityHttpClient http;

    public SsoClient(String region, URI portalEndpoint, URI oidcEndpoint, SdkSyncHttpClient httpClient) {
        this.region = region;
        String suffix = StsCredentialsClient.dnsSuffix(region);
        this.portalEndpoint = portalEndpoint != null ? portalEndpoint
                                                     : URI.create("https://portal.sso." + region + "." + suffix);
        this.oidcEndpoint = oidcEndpoint != null ? oidcEndpoint : URI.create("https://oidc." + region + "." + suffix);
        this.http = IdentityHttpClient.create(httpClient, null);
    }

    /** The region. */
    public String region() {
        return region;
    }

    /** {@code GetRoleCredentials} with the SSO access token. */
    public AwsSessionCredentialsIdentity getRoleCredentials(String accessToken, String accountId, String roleName,
                                                            String providerName) {
        SdkHttpRequest request = IdentityHttpClient.request("GET", portalEndpoint.resolve("/federation/credentials"))
                                                   .addQueryParameter("account_id", accountId)
                                                   .addQueryParameter("role_name", roleName)
                                                   .putHeader("x-amz-sso_bearer_token", accessToken)
                                                   .build();
        IdentityHttpClient.Response response = http.send(request, Duration.ofSeconds(30));
        if (!response.isSuccessful()) {
            throw SdkClientException.create("SSO GetRoleCredentials failed with HTTP " + response.statusCode()
                                            + errorDetail(response.body()));
        }
        Object roleCredentials = Json.parseObject(response.body()).get("roleCredentials");
        if (!(roleCredentials instanceof Map<?, ?>)) {
            throw SdkClientException.create("The SSO GetRoleCredentials response has no roleCredentials.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> credentials = (Map<String, Object>) roleCredentials;
        String accessKeyId = Json.string(credentials, "accessKeyId");
        String secretAccessKey = Json.string(credentials, "secretAccessKey");
        String sessionToken = Json.string(credentials, "sessionToken");
        if (accessKeyId == null || secretAccessKey == null || sessionToken == null) {
            throw SdkClientException.create("The SSO GetRoleCredentials response is missing credentials.");
        }
        return LoadedCredentials.session(accessKeyId, secretAccessKey, sessionToken, accountId,
                                         LoadedCredentials.epochMillis(credentials.get("expiration")), providerName);
    }

    /** A refreshed token. */
    public record RefreshedToken(String accessToken, Instant expiresAt, String refreshToken) {
        @Override
        public String toString() {
            return "RefreshedToken[" + expiresAt + "]";
        }
    }

    /** SSO-OIDC {@code CreateToken} with {@code grantType=refresh_token}. */
    public RefreshedToken refreshToken(String clientId, String clientSecret, String refreshToken, Instant now) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", clientId);
        body.put("clientSecret", clientSecret);
        body.put("grantType", "refresh_token");
        body.put("refreshToken", refreshToken);
        SdkHttpRequest request = IdentityHttpClient.request("POST", oidcEndpoint.resolve("/token"))
                                                   .putHeader("Content-Type", "application/json")
                                                   .body(HttpBody.fromUtf8(Json.write(body)))
                                                   .build();
        IdentityHttpClient.Response response = http.send(request, Duration.ofSeconds(30));
        if (!response.isSuccessful()) {
            throw SdkClientException.create("SSO-OIDC CreateToken failed with HTTP " + response.statusCode()
                                            + errorDetail(response.body()));
        }
        Map<String, Object> token = Json.parseObject(response.body());
        String accessToken = Json.string(token, "accessToken");
        Object expiresIn = token.get("expiresIn");
        if (accessToken == null || !(expiresIn instanceof BigDecimal seconds)) {
            throw SdkClientException.create("The SSO-OIDC CreateToken response has no accessToken or expiresIn.");
        }
        String newRefreshToken = Json.string(token, "refreshToken");
        return new RefreshedToken(accessToken, now.plusSeconds(seconds.longValue()),
                                  newRefreshToken != null ? newRefreshToken : refreshToken);
    }

    private static String errorDetail(String body) {
        try {
            Map<String, Object> error = Json.parseObject(body);
            String code = Json.string(error, "error");
            if (code == null) {
                code = Json.string(error, "__type");
            }
            String message = Json.string(error, "message");
            return code == null && message == null ? "" : ": " + code + " - " + message;
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public void close() {
        http.close();
    }
}
