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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.10 and 16.5: the generated {@code DsqlUtilities} of a synthetic service with DSQL's shape ID, reached
 * standalone and through the sync client's {@code utilities()}. Each token is checked against an independent SigV4
 * query-string signature of the same {@code DbConnect}/{@code DbConnectAdmin} request at the token's signing time.
 */
class GeneratedDsqlUtilitiesTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.dsqllike";

    private static final String MODEL = """
        $version: "2"
        namespace com.amazonaws.dsql

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "dsql")
        @aws.api#service(sdkId: "DSQL", endpointPrefix: "dsql")
        service DSQL {
            version: "2018-05-10"
            operations: [ListClusters]
        }

        @readonly
        @http(method: "GET", uri: "/cluster")
        operation ListClusters {
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
        import PKG.DsqlClient;
        import PKG.DsqlUtilities;

        public final class DsqlProbe {
            private static final StaticCredentialsProvider CREDENTIALS =
                StaticCredentialsProvider.create(AwsCredentialsIdentity.create("AKIDEXAMPLE", "secret"));

            private DsqlProbe() {
            }

            public static String standalone() {
                DsqlUtilities utilities = DsqlUtilities.builder().region(Region.of("us-east-1"))
                                                       .credentialsProvider(CREDENTIALS).build();
                return utilities.generateDbConnectAuthToken(r -> r.hostname("cluster.dsql.us-east-1.on.aws"));
            }

            public static String adminWithExpiration() {
                DsqlUtilities utilities = DsqlUtilities.builder().credentialsProvider(CREDENTIALS).build();
                return utilities.generateDbConnectAdminAuthToken(r -> r.hostname("cluster.dsql.us-east-1.on.aws")
                                                                       .region(Region.of("us-east-2"))
                                                                       .expiresIn(Duration.ofSeconds(3600)));
            }

            public static String throughTheClient() {
                try (DsqlClient client = DsqlClient.builder().region(Region.of("eu-west-1"))
                                                   .credentialsProvider(CREDENTIALS)
                                                   .endpointOverride(URI.create("https://dsql.test"))
                                                   .httpClient(new NoHttp()).build()) {
                    return client.utilities().generateDbConnectAdminAuthToken(r -> r.hostname("c.example.com"));
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
        Files.writeString(models.resolve("dsqllike.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("com.amazonaws.dsql#DSQL", PACKAGE,
                                                                            models, temp.resolve("out")), List.of());
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
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("DsqlProbe.java");
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
    void dbConnect_defaultsTo900Seconds() throws Exception {
        String token = probe("standalone");

        assertTrue(token.startsWith("cluster.dsql.us-east-1.on.aws/?Action=DbConnect&"), token);
        assertTrue(token.contains("X-Amz-Expires=900"), token);
        assertTrue(token.matches(".*X-Amz-Credential=AKIDEXAMPLE%2F\\d{8}%2Fus-east-1%2Fdsql%2Faws4_request.*"),
                   token);
        assertEquals(expected("cluster.dsql.us-east-1.on.aws", "DbConnect", "us-east-1", Duration.ofSeconds(900),
                              token), token);
    }

    @Test
    void dbConnectAdmin_usesTheRequestsRegionAndExpiration() throws Exception {
        String token = probe("adminWithExpiration");

        assertTrue(token.startsWith("cluster.dsql.us-east-1.on.aws/?Action=DbConnectAdmin&"), token);
        assertTrue(token.contains("X-Amz-Expires=3600"), token);
        assertEquals(expected("cluster.dsql.us-east-1.on.aws", "DbConnectAdmin", "us-east-2",
                              Duration.ofSeconds(3600), token), token);
    }

    @Test
    void theClientsUtilities_shareItsRegionAndCredentials() throws Exception {
        String token = probe("throughTheClient");

        assertEquals(expected("c.example.com", "DbConnectAdmin", "eu-west-1", Duration.ofSeconds(900), token),
                     token);
    }

    /** The token an independent SigV4 query-string signing of the request produces at the token's time. */
    private static String expected(String host, String action, String region, Duration expiresIn, String token) {
        Matcher date = DATE.matcher(token);
        assertTrue(date.find(), token);
        Instant signedAt = Instant.from(AMZ_DATE.parse(date.group(1)));
        SdkHttpRequest connect = SdkHttpRequest.builder().method("GET").scheme("https").host(host)
                                               .encodedPath("/").addQueryParameter("Action", action).build();
        SdkHttpRequest signed = AwsV4HttpSigner.create().sign(
            SignRequest.<AwsCredentialsIdentity>builder(AwsCredentialsIdentity.create("AKIDEXAMPLE", "secret"))
                       .request(connect)
                       .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                       .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "dsql")
                       .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION,
                                    AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
                       .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, expiresIn)
                       .putProperty(HttpSigner.SIGNING_CLOCK, Clock.fixed(signedAt, ZoneOffset.UTC))
                       .build()).request();
        return signed.authority() + signed.encodedPath() + "?" + signed.encodedQuery();
    }

    private static String probe(String method) throws Exception {
        try {
            return (String) Class.forName("example.probe.DsqlProbe", true, loader).getMethod(method).invoke(null);
        } catch (InvocationTargetException e) {
            throw new AssertionError("DsqlProbe." + method + " failed", e.getCause());
        }
    }
}
