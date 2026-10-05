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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityRefreshException;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.regions.Region;

/**
 * The container and instance-metadata providers over a real sync transport against local servers, including the
 * refresh contract: a rejected identity is refreshed before the next resolution and a failed required refresh fails
 * with its cause (Requirements 13.4, 13.16, 13.17, 13.19).
 */
class NetworkCredentialsProvidersTest {
    @TempDir
    Path home;

    private LocalServer server;
    private UrlConnectionHttpClient http;

    @BeforeEach
    void start() throws Exception {
        server = new LocalServer();
        http = UrlConnectionHttpClient.create();
    }

    @AfterEach
    void stop() {
        http.close();
        server.close();
    }

    @Test
    void containerProviderSendsTheTokenFileRetriesServerErrorsAndParsesCredentials() throws Exception {
        Path tokenFile = Files.writeString(home.resolve("token"), "file-token\n");
        AtomicInteger calls = new AtomicInteger();
        server.route("GET", "/creds", r -> calls.incrementAndGet() == 1 ? new LocalServer.Response(500, "")
                                                                         : new LocalServer.Response(200, creds("A1", 3600)));
        ContainerCredentialsProvider provider = ContainerCredentialsProvider.builder()
            .settings(settings(Map.of("AWS_CONTAINER_SERVICE_ENDPOINT", server.endpoint(),
                                      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI", "/creds",
                                      "AWS_CONTAINER_AUTHORIZATION_TOKEN", "env-token",
                                      "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE", tokenFile.toString())))
            .httpClient(http).build();

        AwsCredentialsIdentity credentials = provider.resolveIdentity().join();
        assertEquals("A1", credentials.accessKeyId());
        assertEquals("T", ((AwsSessionCredentialsIdentity) credentials).sessionToken());
        assertEquals("123456789012", credentials.accountId().orElseThrow());
        assertTrue(credentials.expirationTime().isPresent());
        assertEquals("file-token", server.requests.get(1).header("Authorization"));
        assertEquals(2, calls.get());
    }

    @Test
    void containerFullUriMustBeHttpsOrLoopback() {
        ContainerCredentialsProvider provider = ContainerCredentialsProvider.builder()
            .settings(settings(Map.of("AWS_CONTAINER_CREDENTIALS_FULL_URI", "http://example.com/creds")))
            .httpClient(http).build();
        CompletionException e = assertThrows(CompletionException.class, () -> provider.resolveIdentity().join());
        assertInstanceOf(IdentityRefreshException.class, e.getCause());
        assertTrue(e.getCause().getCause().getMessage().contains("invalid host"), e.getCause().getCause().getMessage());
    }

    @Test
    void rejectedIdentityIsRefreshedAndFailedRefreshKeepsTheCause() {
        AtomicInteger calls = new AtomicInteger();
        server.route("GET", "/creds", r -> calls.incrementAndGet() == 3 ? new LocalServer.Response(503, "down")
                                                                         : new LocalServer.Response(
                                                                             200, creds("K" + calls.get(), 3600)));
        ContainerCredentialsProvider provider = ContainerCredentialsProvider.builder()
            .settings(settings(Map.of("AWS_CONTAINER_CREDENTIALS_FULL_URI", server.endpoint() + "/creds")))
            .httpClient(http).build();

        AwsCredentialsIdentity first = provider.resolveIdentity().join();
        assertEquals("K1", first.accessKeyId());
        assertEquals(first, provider.resolveIdentity().join()); // cached

        provider.invalidate(first);
        AwsCredentialsIdentity refreshed = provider.resolveIdentity().join();
        assertEquals("K2", refreshed.accessKeyId());

        provider.invalidate(refreshed);
        server.route("GET", "/creds", 503, "down");
        CompletionException e = assertThrows(CompletionException.class, () -> provider.resolveIdentity().join());
        IdentityRefreshException refresh = assertInstanceOf(IdentityRefreshException.class, e.getCause());
        assertInstanceOf(SdkClientException.class, refresh.getCause());
        assertTrue(refresh.getCause().getMessage().contains("HTTP 503"), refresh.getCause().getMessage());
    }

    @Test
    void instanceProfileUsesImdsV2Tokens() {
        server.route("PUT", "/latest/api/token", r -> {
            assertEquals("21600", r.header("x-aws-ec2-metadata-token-ttl-seconds"));
            return new LocalServer.Response(200, "imds-token");
        });
        server.route("GET", "/latest/meta-data/iam/security-credentials/", 200, "my-role\n");
        server.route("GET", "/latest/meta-data/iam/security-credentials/my-role", 200, creds("I1", 3600));
        server.route("GET", "/latest/dynamic/instance-identity/document", 200, "{\"region\":\"us-west-2\"}");
        SystemSettings settings = settings(Map.of("AWS_EC2_METADATA_SERVICE_ENDPOINT", server.endpoint()));

        AwsCredentialsIdentity credentials = InstanceProfileCredentialsProvider.builder().settings(settings)
                                                                               .httpClient(http).build()
                                                                               .resolveIdentity().join();
        assertEquals("I1", credentials.accessKeyId());
        assertTrue(server.requests.stream().filter(r -> r.method().equals("GET"))
                                  .allMatch(r -> "imds-token".equals(r.header("x-aws-ec2-metadata-token"))));
        assertEquals(Region.US_WEST_2, InstanceProfileRegionProvider.create(settings, null, http).getRegion());
    }

    @Test
    void instanceProfileFallsBackToImdsV1UnlessDisabledAndFailsOn400() {
        server.route("PUT", "/latest/api/token", 403, "");
        server.route("GET", "/latest/meta-data/iam/security-credentials/", 200, "role");
        server.route("GET", "/latest/meta-data/iam/security-credentials/role", 200, creds("V1", 3600));
        Map<String, String> env = new HashMap<>(Map.of("AWS_EC2_METADATA_SERVICE_ENDPOINT", server.endpoint()));

        assertEquals("V1", InstanceProfileCredentialsProvider.builder().settings(settings(env)).httpClient(http).build()
                                                             .resolveIdentity().join().accessKeyId());

        env.put("AWS_EC2_METADATA_V1_DISABLED", "true");
        CompletionException disabled = assertThrows(CompletionException.class, () -> InstanceProfileCredentialsProvider
            .builder().settings(settings(env)).httpClient(http).build().resolveIdentity().join());
        assertTrue(rootMessage(disabled).contains("fallback to IMDS v1 is disabled"), rootMessage(disabled));

        server.route("PUT", "/latest/api/token", 400, "");
        env.remove("AWS_EC2_METADATA_V1_DISABLED");
        CompletionException badRequest = assertThrows(CompletionException.class, () -> InstanceProfileCredentialsProvider
            .builder().settings(settings(env)).httpClient(http).build().resolveIdentity().join());
        assertTrue(rootMessage(badRequest).contains("Unable to fetch metadata token"), rootMessage(badRequest));

        env.put("AWS_EC2_METADATA_DISABLED", "true");
        CompletionException off = assertThrows(CompletionException.class, () -> InstanceProfileCredentialsProvider
            .builder().settings(settings(env)).httpClient(http).build().resolveIdentity().join());
        assertTrue(rootMessage(off).contains("disabled"), rootMessage(off));
    }

    private SystemSettings settings(Map<String, String> env) {
        Map<String, String> all = new HashMap<>(env);
        all.put("HOME", home.toString());
        return new SystemSettings(SettingSource.empty(), SettingSource.fromMap(all));
    }

    static String creds(String accessKeyId, long expiresInSeconds) {
        return "{\"Code\":\"Success\",\"AccessKeyId\":\"" + accessKeyId + "\",\"SecretAccessKey\":\"S\",\"Token\":\"T\","
               + "\"AccountId\":\"123456789012\",\"Expiration\":\""
               + Instant.now().plusSeconds(expiresInSeconds).truncatedTo(ChronoUnit.SECONDS) + "\"}";
    }

    private static String rootMessage(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            messages.append(c.getMessage()).append(" | ");
        }
        return messages.toString();
    }
}
