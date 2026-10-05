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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.identity.providers.internal.Json;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.SystemSettings;

/** Profile credential sources with the V2 precedence, with STS and SSO served by a local server (Requirement 13.4). */
class ProfileCredentialsProviderTest {
    private static final String STS_RESPONSE =
        "<AssumeRoleResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\"><AssumeRoleResult><Credentials>"
        + "<AccessKeyId>ROLEKEY</AccessKeyId><SecretAccessKey>ROLESECRET</SecretAccessKey>"
        + "<SessionToken>ROLETOKEN</SessionToken><Expiration>%s</Expiration></Credentials>"
        + "<AssumedRoleUser><Arn>arn:aws:sts::111122223333:assumed-role/r/s</Arn></AssumedRoleUser>"
        + "</AssumeRoleResult></AssumeRoleResponse>";

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
    void staticAndSessionKeys() {
        AwsCredentialsIdentity basic = provider("[profile a]\naws_access_key_id=AK\naws_secret_access_key=SK\n"
                                                + "aws_account_id=1234\n", "a").resolveIdentity().join();
        assertEquals("AK", basic.accessKeyId());
        assertEquals("1234", basic.accountId().orElseThrow());
        assertFalse(basic instanceof AwsSessionCredentialsIdentity);

        AwsCredentialsIdentity session = provider("[profile a]\naws_access_key_id=AK\naws_secret_access_key=SK\n"
                                                  + "aws_session_token=ST\n", "a").resolveIdentity().join();
        assertEquals("ST", ((AwsSessionCredentialsIdentity) session).sessionToken());
    }

    @Test
    void missingOrInvalidConfigurationFailsWithV2Messages() {
        assertTrue(failure("[profile a]\naws_access_key_id=AK\n", "a")
                       .contains("Profile property 'aws_secret_access_key' was not configured for 'a'"));
        assertTrue(failure("[profile a]\nregion=us-east-1\n", "a").contains("no credentials for profile 'a'"));
        assertTrue(failure("[profile a]\nrole_arn=arn\nsource_profile=b\ncredential_source=Environment\n", "a")
                       .contains("has both source_profile and credential_source"));
        assertTrue(failure("[profile a]\nrole_arn=arn\nsource_profile=b\n[profile b]\nrole_arn=arn\n"
                           + "source_profile=a\n", "a").contains("Circular relationship"));
        assertTrue(failure("[profile a]\nlogin_session=x\n", "a").contains("login_session"));
    }

    @Test
    void credentialProcess() {
        String json = "{\\\"Version\\\":1,\\\"AccessKeyId\\\":\\\"PK\\\",\\\"SecretAccessKey\\\":\\\"PS\\\","
                      + "\\\"SessionToken\\\":\\\"PT\\\"}";
        AwsCredentialsIdentity credentials =
            provider("[profile p]\ncredential_process = printf \"" + json + "\"\naws_account_id=999\n", "p")
                .resolveIdentity().join();
        assertEquals("PK", credentials.accessKeyId());
        assertEquals("PT", ((AwsSessionCredentialsIdentity) credentials).sessionToken());
        assertEquals("999", credentials.accountId().orElseThrow());
        assertTrue(failure("[profile p]\ncredential_process = exit 3\n", "p").contains("non-zero exit value: 3"));
    }

    @Test
    void roleWithSourceProfileAssumesTheRoleSignedWithTheSourceCredentials() {
        server.route("POST", "/", 200, String.format(STS_RESPONSE, Instant.now().plus(1, ChronoUnit.HOURS)
                                                                          .truncatedTo(ChronoUnit.SECONDS)));
        AwsCredentialsIdentity credentials = provider(
            "[profile role]\nrole_arn=arn:aws:iam::111122223333:role/r\nsource_profile=base\nrole_session_name=s\n"
            + "external_id=ext\nregion=eu-west-1\n[profile base]\naws_access_key_id=BASEKEY\naws_secret_access_key=B\n",
            "role").resolveIdentity().join();

        assertEquals("ROLEKEY", credentials.accessKeyId());
        assertEquals("111122223333", credentials.accountId().orElseThrow());
        LocalServer.Request request = server.requests.get(0);
        assertTrue(request.header("Authorization").startsWith("AWS4-HMAC-SHA256 Credential=BASEKEY/"),
                   request.header("Authorization"));
        assertTrue(request.header("Authorization").contains("/eu-west-1/sts/aws4_request"));
        assertTrue(request.body().contains("Action=AssumeRole&"), request.body());
        assertTrue(request.body().contains("RoleArn=arn%3Aaws%3Aiam%3A%3A111122223333%3Arole%2Fr"), request.body());
        assertTrue(request.body().contains("ExternalId=ext"), request.body());
    }

    @Test
    void webIdentityProfileTakesPrecedenceOverSso() throws Exception {
        Path token = Files.writeString(home.resolve("jwt"), "the-jwt");
        server.route("POST", "/", r -> new LocalServer.Response(200, String.format(STS_RESPONSE.replace(
            "AssumeRole", "AssumeRoleWithWebIdentity"), Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS))));
        AwsCredentialsIdentity credentials = provider(
            "[profile w]\nrole_arn=arn:aws:iam::1:role/w\nweb_identity_token_file=" + token + "\nsso_account_id=1\n",
            "w").resolveIdentity().join();
        assertEquals("ROLEKEY", credentials.accessKeyId());
        LocalServer.Request request = server.requests.get(0);
        assertTrue(request.body().contains("Action=AssumeRoleWithWebIdentity"), request.body());
        assertTrue(request.body().contains("WebIdentityToken=the-jwt"), request.body());
        assertEquals(null, request.header("Authorization"));
    }

    @Test
    void legacySsoProfileUsesTheCachedTokenAndFailsWhenExpired() throws Exception {
        String startUrl = "https://d-123.awsapps.com/start";
        writeCache(sha1(startUrl), Map.of("accessToken", "legacy-token",
                                          "expiresAt", Instant.now().plusSeconds(3600).toString()));
        server.route("GET", "/federation/credentials", r -> {
            assertEquals("legacy-token", r.header("x-amz-sso_bearer_token"));
            assertTrue(r.query().contains("account_id=012345678901") && r.query().contains("role_name=Dev"), r.query());
            return new LocalServer.Response(200, "{\"roleCredentials\":{\"accessKeyId\":\"SSOKEY\",\"secretAccessKey\":"
                                                 + "\"S\",\"sessionToken\":\"T\",\"expiration\":"
                                                 + Instant.now().plusSeconds(3600).toEpochMilli() + "}}");
        });
        String profile = "[profile s]\nsso_start_url=" + startUrl + "\nsso_region=us-east-1\n"
                         + "sso_account_id=012345678901\nsso_role_name=Dev\n";
        AwsCredentialsIdentity credentials = provider(profile, "s").resolveIdentity().join();
        assertEquals("SSOKEY", credentials.accessKeyId());
        assertEquals("012345678901", credentials.accountId().orElseThrow());

        writeCache(sha1(startUrl), Map.of("accessToken", "legacy-token",
                                          "expiresAt", Instant.now().minusSeconds(60).toString()));
        assertTrue(failure(profile, "s").contains("aws sso login"));
    }

    @Test
    void ssoSessionTokenIsRefreshedNearExpiryAndWrittenBack() throws Exception {
        writeCache(sha1("my-sso"), Map.of("accessToken", "old", "expiresAt",
                                          Instant.now().plusSeconds(60).toString(), "clientId", "cid",
                                          "clientSecret", "csecret", "refreshToken", "rt",
                                          "registrationExpiresAt", Instant.now().plusSeconds(86400).toString()));
        server.route("POST", "/token", r -> {
            assertTrue(r.body().contains("\"grantType\":\"refresh_token\"") && r.body().contains("\"refreshToken\":\"rt\""),
                       r.body());
            return new LocalServer.Response(200, "{\"accessToken\":\"new\",\"expiresIn\":3600,\"refreshToken\":\"rt2\"}");
        });
        ProfileTokenProvider tokens = ProfileTokenProvider.builder()
            .profileFile(() -> ProfileFile.parse("[profile s]\nsso_session=my-sso\n[sso-session my-sso]\n"
                                                 + "sso_region=us-east-1\nsso_start_url=https://x\n",
                                                 ProfileFile.Type.CONFIGURATION))
            .profileName("s").settings(settings()).httpClient(http).oidcEndpoint(server.uri())
            .cacheDirectory(home.resolve(".aws/sso/cache")).build();

        TokenIdentity token = tokens.resolveIdentity().join();
        assertEquals("new", token.token());
        Map<String, Object> cache = Json.parseObject(Files.readString(
            home.resolve(".aws/sso/cache").resolve(sha1("my-sso") + ".json")));
        assertEquals("new", cache.get("accessToken"));
        assertEquals("rt2", cache.get("refreshToken"));
        assertEquals("cid", cache.get("clientId"));
    }

    private ProfileCredentialsProvider provider(String config, String profileName) {
        return ProfileCredentialsProvider.builder()
                                         .profileFile(ProfileFile.parse(config, ProfileFile.Type.CONFIGURATION))
                                         .profileName(profileName)
                                         .settings(settings())
                                         .httpClient(http)
                                         .stsEndpoint(server.uri())
                                         .ssoPortalEndpoint(server.uri())
                                         .ssoOidcEndpoint(server.uri())
                                         .ssoCacheDirectory(home.resolve(".aws/sso/cache"))
                                         .build();
    }

    private String failure(String config, String profileName) {
        CompletionException e = assertThrows(CompletionException.class,
                                             () -> provider(config, profileName).resolveIdentity().join());
        StringBuilder messages = new StringBuilder();
        for (Throwable c = e; c != null; c = c.getCause()) {
            messages.append(c.getMessage()).append(" | ");
        }
        return messages.toString();
    }

    private SystemSettings settings() {
        return new SystemSettings(SettingSource.empty(), SettingSource.fromMap(Map.of("HOME", home.toString())));
    }

    private void writeCache(String name, Map<String, Object> content) throws Exception {
        Path directory = Files.createDirectories(home.resolve(".aws/sso/cache"));
        Files.writeString(directory.resolve(name + ".json"), Json.write(content));
    }

    private static String sha1(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
