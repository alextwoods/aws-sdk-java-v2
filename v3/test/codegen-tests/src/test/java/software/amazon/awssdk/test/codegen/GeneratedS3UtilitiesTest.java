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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.10 and 16.5: the generated {@code S3Utilities} of a synthetic restXml service with S3's shape ID and
 * S3-like endpoint rules. {@code getUrl} resolves through the generated endpoint rules (virtual-hosted-style,
 * path-style, endpoint override), standalone and through the sync client's {@code utilities()}; {@code parseUri}
 * follows V2's {@code S3Utilities.parseUri} cases.
 */
class GeneratedS3UtilitiesTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.s3utilities";

    private static final String MODEL = """
        $version: "2"
        namespace com.amazonaws.s3

        @aws.protocols#restXml(noErrorWrapping: true)
        @aws.auth#sigv4(name: "s3")
        @aws.api#service(sdkId: "S3", endpointPrefix: "s3")
        @smithy.rules#endpointRuleSet({
            version: "1.0"
            parameters: {
                Bucket: { type: "String", required: false, documentation: "The bucket." }
                Region: { type: "String", builtIn: "AWS::Region", required: true, documentation: "The region." }
                Endpoint: { type: "String", builtIn: "SDK::Endpoint", required: false, documentation: "Override." }
                ForcePathStyle: {
                    type: "Boolean", builtIn: "AWS::S3::ForcePathStyle", required: true, default: false,
                    documentation: "Path-style addressing."
                }
            }
            rules: [
                {
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Bucket" }] }
                        { fn: "isSet", argv: [{ ref: "Endpoint" }] }
                        { fn: "booleanEquals", argv: [{ ref: "ForcePathStyle" }, true] }
                    ]
                    endpoint: { url: "{Endpoint}/{Bucket}" }
                    type: "endpoint"
                }
                {
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Bucket" }] }
                        { fn: "isSet", argv: [{ ref: "Endpoint" }] }
                        { fn: "parseURL", argv: [{ ref: "Endpoint" }], assign: "url" }
                    ]
                    endpoint: { url: "{url#scheme}://{Bucket}.{url#authority}{url#path}" }
                    type: "endpoint"
                }
                {
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Bucket" }] }
                        { fn: "booleanEquals", argv: [{ ref: "ForcePathStyle" }, false] }
                        { fn: "aws.isVirtualHostableS3Bucket", argv: [{ ref: "Bucket" }, false] }
                    ]
                    endpoint: { url: "https://{Bucket}.s3.{Region}.amazonaws.com" }
                    type: "endpoint"
                }
                {
                    conditions: [{ fn: "isSet", argv: [{ ref: "Bucket" }] }]
                    endpoint: { url: "https://s3.{Region}.amazonaws.com/{Bucket}" }
                    type: "endpoint"
                }
                {
                    conditions: []
                    endpoint: { url: "https://s3.{Region}.amazonaws.com" }
                    type: "endpoint"
                }
            ]
        })
        service AmazonS3 {
            version: "2006-03-01"
            operations: [GetObject, ListBuckets]
        }

        @readonly
        @http(method: "GET", uri: "/{Bucket}/{Key+}")
        operation GetObject {
            input := {
                @required
                @httpLabel
                @smithy.rules#contextParam(name: "Bucket")
                Bucket: String

                @required
                @httpLabel
                Key: String
            }
            output := {}
        }

        @readonly
        @http(method: "GET", uri: "/")
        operation ListBuckets {
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
        import PKG.S3Client;
        import PKG.S3Uri;
        import PKG.S3Utilities;

        public final class S3UtilitiesProbe {
            private S3UtilitiesProbe() {
            }

            public static String url(String region, String endpoint, String pathStyle, String bucket, String key,
                                     String versionId) {
                S3Utilities.Builder builder = S3Utilities.builder();
                if (!region.isEmpty()) {
                    builder.region(Region.of(region));
                }
                if (!endpoint.isEmpty()) {
                    builder.endpointOverride(URI.create(endpoint));
                }
                if (!pathStyle.isEmpty()) {
                    builder.forcePathStyle(Boolean.valueOf(pathStyle));
                }
                return builder.build().getUrl(r -> r.bucket(bucket).key(key)
                                                    .versionId(versionId.isEmpty() ? null : versionId)).toString();
            }

            public static String requestRegionWins() {
                return S3Utilities.builder().region(Region.of("us-west-2")).build()
                                  .getUrl(r -> r.bucket("b").key("k").region(Region.of("ap-south-1"))
                                                .endpoint(URI.create("https://request.test"))).toString();
            }

            public static String throughTheClient() {
                try (S3Client client = S3Client.builder().region(Region.of("eu-west-1"))
                         .credentialsProvider(StaticCredentialsProvider.create(
                             AwsCredentialsIdentity.create("AKIDEXAMPLE", "secret")))
                         .endpointOverride(URI.create("https://s3.test"))
                         .httpClient(new NoHttp()).build()) {
                    return client.utilities().getUrl(r -> r.bucket("my-bucket").key("a/b.txt")) + "\\n"
                           + client.utilities().parseUri(URI.create("s3://cli-bucket/k")).bucket().orElse(null);
                }
            }

            public static String parse(String uri) {
                S3Uri parsed = S3Utilities.builder().build().parseUri(URI.create(uri));
                return parsed.bucket().orElse("-") + "|" + parsed.key().orElse("-") + "|"
                       + parsed.region().map(Region::id).orElse("-") + "|" + parsed.isPathStyle() + "|"
                       + parsed.rawQueryParameters() + "|" + parsed.firstMatchingRawQueryParameter("versionId")
                                                                 .orElse("-");
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

    @TempDir
    static Path temp;

    private static URLClassLoader loader;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("s3like.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("com.amazonaws.s3#AmazonS3", PACKAGE,
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
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("S3UtilitiesProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @ParameterizedTest(name = "{0} {1} pathStyle={2} {3}/{4}")
    @CsvSource(delimiter = ';', value = {
        "us-west-2;;;my-bucket;photos/a b.jpg;;https://my-bucket.s3.us-west-2.amazonaws.com/photos/a%20b.jpg",
        "us-west-2;;true;my-bucket;photos/a b.jpg;;https://s3.us-west-2.amazonaws.com/my-bucket/photos/a%20b.jpg",
        "us-west-2;;;My.Bucket;k;;https://s3.us-west-2.amazonaws.com/My.Bucket/k",
        "us-east-1;;;bkt;dir/;v1;https://bkt.s3.us-east-1.amazonaws.com/dir/?versionId=v1",
        "us-east-1;;;bkt;a*~(x)+;;https://bkt.s3.us-east-1.amazonaws.com/a%2A~%28x%29%2B",
        "us-east-1;;;b;k;;https://s3.us-east-1.amazonaws.com/b/k",
        "us-east-1;https://s3.test:8443;;my-bucket;k;;https://my-bucket.s3.test:8443/k",
        "us-east-1;https://s3.test:8443;true;my-bucket;k;;https://s3.test:8443/my-bucket/k",
        "us-east-1;https://s3.test/base;;my-bucket;k;;https://my-bucket.s3.test/base/k"})
    void getUrl_resolvesThroughTheEndpointRules(String region, String endpoint, String pathStyle, String bucket,
                                                String key, String versionId, String expected) throws Exception {
        assertEquals(expected, probe("url", region, endpoint, pathStyle, bucket, key, versionId));
    }

    @Test
    void getUrl_theRequestsRegionAndEndpointWin() throws Exception {
        assertEquals("https://b.request.test/k", probe("requestRegionWins"));
    }

    @Test
    void getUrl_requiresARegion() {
        AssertionError failure = assertThrows(AssertionError.class, () -> probe("url", "", "", "", "b", "k", ""));
        assertTrue(failure.getCause() instanceof IllegalArgumentException, String.valueOf(failure.getCause()));
    }

    @Test
    void theClientsUtilities_shareItsRegionAndEndpoint() throws Exception {
        assertEquals("https://my-bucket.s3.test/a/b.txt\ncli-bucket", probe("throughTheClient"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = ';', value = {
        "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=abc123;"
            + "myBucket|doc.txt|us-west-1|false|{versionId=[abc123]}|abc123",
        "https://s3.us-west-1.amazonaws.com/myBucket/doc.txt?versionId=abc123;"
            + "myBucket|doc.txt|us-west-1|true|{versionId=[abc123]}|abc123",
        "https://s3-us-west-1.amazonaws.com/myBucket/doc.txt;myBucket|doc.txt|us-west-1|true|{}|-",
        "https://s3.amazonaws.com/myBucket/doc.txt;myBucket|doc.txt|-|true|{}|-",
        "https://myBucket.s3.amazonaws.com/doc.txt;myBucket|doc.txt|-|false|{}|-",
        "https://s3.us-east-1.amazonaws.com/myBucket;myBucket|-|us-east-1|true|{}|-",
        "https://s3.us-east-1.amazonaws.com/myBucket/;myBucket|-|us-east-1|true|{}|-",
        "https://s3.us-east-1.amazonaws.com/;-|-|us-east-1|true|{}|-",
        "https://my.dotted.bucket.s3.us-east-1.amazonaws.com/;my.dotted.bucket|-|us-east-1|false|{}|-",
        "https://myBucket.s3.us-west-1.amazonaws.com/path/to/doc%20one.txt;"
            + "myBucket|path/to/doc one.txt|us-west-1|false|{}|-",
        "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=a%20b&partNumber=19&partNumber=20&flag;"
            + "myBucket|doc.txt|us-west-1|false|{versionId=[a b], partNumber=[19, 20], flag=[null]}|a b",
        "s3://myBucket/resources/doc.txt;myBucket|resources/doc.txt|-|false|{}|-",
        "s3://myBucket;myBucket|-|-|false|{}|-"})
    void parseUri_followsV2(String uri, String expected) throws Exception {
        assertEquals(expected, probe("parse", uri));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/bucket/key", "file:///tmp/key",
                            "https://myendpoint-123456789012.s3-accesspoint.us-east-1.amazonaws.com/doc.txt",
                            "https://op-01234567890123456.s3-outposts.us-west-2.amazonaws.com/doc.txt"})
    void parseUri_rejectsUnsupportedUris(String uri) {
        AssertionError failure = assertThrows(AssertionError.class, () -> probe("parse", uri));
        assertTrue(failure.getCause() instanceof IllegalArgumentException, String.valueOf(failure.getCause()));
    }

    private static String probe(String method, String... arguments) throws Exception {
        Class<?>[] types = new Class<?>[arguments.length];
        Object[] values = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            types[i] = String.class;
            // CsvSource passes an empty column as null.
            values[i] = arguments[i] == null ? "" : arguments[i];
        }
        try {
            return (String) Class.forName("example.probe.S3UtilitiesProbe", true, loader).getMethod(method, types)
                                 .invoke(null, values);
        } catch (InvocationTargetException e) {
            throw new AssertionError("S3UtilitiesProbe." + method + " failed", e.getCause());
        }
    }
}
