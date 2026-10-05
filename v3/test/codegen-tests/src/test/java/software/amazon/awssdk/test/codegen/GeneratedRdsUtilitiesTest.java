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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.10 and 16.5: the generated {@code RdsUtilities} of a synthetic service with RDS's shape ID, reached
 * standalone and through the sync client's {@code utilities()}. The token is checked against an independent SigV4
 * query-string signature of the same {@code connect} request at the token's own signing time.
 */
class GeneratedRdsUtilitiesTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.rdslike";

    private static final String MODEL = """
        $version: "2"
        namespace com.amazonaws.rds

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "rds")
        @aws.api#service(sdkId: "RDS", endpointPrefix: "rds")
        service AmazonRDSv19 {
            version: "2014-10-31"
            operations: [DescribeDBInstances]
        }

        operation DescribeDBInstances {
            input := {}
            output := {}
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.time.Duration;
        import software.amazon.awssdk.http.HttpClientCapabilities;
        import software.amazon.awssdk.http.HttpExecutionOptions;
        import software.amazon.awssdk.http.HttpTransportMode;
        import software.amazon.awssdk.http.Protocol;
        import software.amazon.awssdk.http.SdkHttpRequest;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.http.SyncHttpExchange;
        import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.regions.Region;
        import PKG.RdsClient;
        import PKG.RdsUtilities;

        public final class RdsProbe {
            private static final StaticCredentialsProvider CREDENTIALS =
                StaticCredentialsProvider.create(AwsCredentialsIdentity.create("AKIDEXAMPLE", "secret"));

            private RdsProbe() {
            }

            public static String standalone() {
                RdsUtilities utilities = RdsUtilities.builder().region(Region.of("us-west-2"))
                                                     .credentialsProvider(CREDENTIALS).build();
                return utilities.generateAuthenticationToken(r -> r.hostname("db.example.com").port(3306)
                                                                   .username("app user"));
            }

            public static String throughTheClient() {
                try (RdsClient client = RdsClient.builder().region(Region.of("eu-west-1"))
                                                 .credentialsProvider(CREDENTIALS)
                                                 .endpointOverride(URI.create("https://rds.test"))
                                                 .httpClient(new NoHttp()).build()) {
                    return client.utilities().generateAuthenticationToken(r -> r.hostname("db.example.com")
                                                                                .port(5432).username("admin"));
                }
            }

            private static final class NoHttp implements SdkSyncHttpClient {
                @Override
                public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
                    throw new AssertionError("utilities never transmit");
                }

                @Override
                public HttpClientCapabilities capabilities() {
                    return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                                 .maxBufferedBytesPerExchange(1 << 20)
                                                 .cancellationTimeout(Duration.ofSeconds(1)).build();
                }

                @Override
                public String clientName() {
                    return "NoHttp";
                }

                @Override
                public void close() {
                }
            }
        }
        """.replace("PKG", PACKAGE);

    private static final Pattern DATE = Pattern.compile("X-Amz-Date=(\\d{8}T\\d{6}Z)");
    private static final DateTimeFormatter AMZ_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    @TempDir
    static Path temp;

    private static URLClassLoader loader;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("rdslike.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("com.amazonaws.rds#AmazonRDSv19",
                                                                            PACKAGE, models, temp.resolve("out")),
                                                      List.of());
        Path sources = temp.resolve("sources");
        try (var walk = Files.walk(result.sourceRoot())) {
            for (Path source : walk.toList()) {
                Path target = sources.resolve(result.sourceRoot().relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("RdsProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void standaloneUtilities_generateTheV2ShapedSigV4Token() throws Exception {
        String token = probe("standalone");

        assertTrue(token.startsWith("db.example.com:3306/?Action=connect&DBUser=app%20user&"), token);
        assertTrue(token.contains("X-Amz-Expires=900"), token);
        assertTrue(token.matches(".*X-Amz-Credential=AKIDEXAMPLE%2F\\d{8}%2Fus-west-2%2Frds-db%2Faws4_request.*"),
                   token);
        assertEquals(expectedToken("db.example.com", 3306, "app user", "us-west-2", token), token);
    }

    @Test
    void theClientsUtilities_shareItsRegionAndCredentials() throws Exception {
        String token = probe("throughTheClient");

        assertTrue(token.startsWith("db.example.com:5432/?Action=connect&DBUser=admin&"), token);
        assertEquals(expectedToken("db.example.com", 5432, "admin", "eu-west-1", token), token);
    }

    /** The token an independent SigV4 query-string signing of the connect request produces at the token's time. */
    private static String expectedToken(String host, int port, String user, String region, String token) {
        Matcher date = DATE.matcher(token);
        assertTrue(date.find(), token);
        Instant signedAt = Instant.from(AMZ_DATE.parse(date.group(1)));
        SdkHttpRequest connect = SdkHttpRequest.builder().method("GET").scheme("https").host(host).port(port)
                                               .encodedPath("/").addQueryParameter("Action", "connect")
                                               .addQueryParameter("DBUser", user).build();
        SdkHttpRequest signed = AwsV4HttpSigner.create().sign(
            SignRequest.<AwsCredentialsIdentity>builder(AwsCredentialsIdentity.create("AKIDEXAMPLE", "secret"))
                       .request(connect)
                       .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                       .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "rds-db")
                       .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION,
                                    AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
                       .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, Duration.ofMinutes(15))
                       .putProperty(HttpSigner.SIGNING_CLOCK, Clock.fixed(signedAt, ZoneOffset.UTC))
                       .build()).request();
        String signature = signed.queryParameters().stream().filter(p -> p.rawName().equals("X-Amz-Signature"))
                                 .map(HttpQueryParameter::rawValue).findFirst().orElseThrow();
        assertTrue(signature.matches("[0-9a-f]{64}"), signature);
        return signed.authority() + signed.encodedPath() + "?" + signed.encodedQuery();
    }

    private static String probe(String method) throws Exception {
        try {
            return (String) Class.forName("example.probe.RdsProbe", true, loader).getMethod(method).invoke(null);
        } catch (InvocationTargetException e) {
            throw new AssertionError("RdsProbe." + method + " failed", e.getCause());
        }
    }
}
