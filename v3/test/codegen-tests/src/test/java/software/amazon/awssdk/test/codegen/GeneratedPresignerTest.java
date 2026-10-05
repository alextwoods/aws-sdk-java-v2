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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.10, 16.5, 18.1-18.3: the generated Modern Auth presigner of a synthetic restXml service with S3's
 * shape ID, which the V2 baseline classifies as having {@code S3Presigner}. A probe against the generated public API
 * presigns {@code GetObject} and {@code PutObject} with SigV4 query-string signing; an interceptor observes that the
 * calls stop after signing, and the presigner has no transport to transmit with.
 */
class GeneratedPresignerTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.s3like";

    private static final String MODEL = """
        $version: "2"
        namespace com.amazonaws.s3

        @aws.protocols#restXml(noErrorWrapping: true)
        @aws.auth#sigv4(name: "s3")
        @aws.api#service(sdkId: "S3", endpointPrefix: "s3")
        service AmazonS3 {
            version: "2006-03-01"
            operations: [GetObject, PutObject, ListBuckets]
        }

        @readonly
        @http(method: "GET", uri: "/{Bucket}/{Key+}")
        operation GetObject {
            input := {
                @required
                @httpLabel
                Bucket: String

                @required
                @httpLabel
                Key: String

                @httpQuery("versionId")
                VersionId: String
            }
            output := {
                @httpHeader("ETag")
                ETag: String
            }
        }

        @idempotent
        @http(method: "PUT", uri: "/{Bucket}/{Key+}")
        operation PutObject {
            input := {
                @required
                @httpLabel
                Bucket: String

                @required
                @httpLabel
                Key: String

                @httpHeader("Content-Type")
                ContentType: String
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
        import java.util.concurrent.atomic.AtomicInteger;
        import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.pipeline.ExecutionInterceptor;
        import software.amazon.awssdk.pipeline.HookContext;
        import software.amazon.awssdk.regions.Region;
        import PKG.model.PutObjectRequest;
        import PKG.presigner.S3Presigner;
        import PKG.presigner.model.PresignedGetObjectRequest;
        import PKG.presigner.model.PresignedPutObjectRequest;
        import PKG.presigner.model.PutObjectPresignRequest;

        public final class PresignProbe {
            private PresignProbe() {
            }

            public static String presign() {
                AtomicInteger signed = new AtomicInteger();
                AtomicInteger transmitted = new AtomicInteger();
                ExecutionInterceptor observer = new ExecutionInterceptor() {
                    @Override
                    public void beforeTransmission(HookContext.BeforeTransmission context) {
                        transmitted.incrementAndGet();
                    }

                    @Override
                    public software.amazon.awssdk.auth.api.SignedRequest modifyAfterSigning(
                        HookContext.ModifyAfterSigning context) {
                        signed.incrementAndGet();
                        return context.signedRequest();
                    }
                };
                try (S3Presigner presigner = S3Presigner.builder()
                         .region(Region.of("us-east-1"))
                         .credentialsProvider(StaticCredentialsProvider.create(
                             AwsCredentialsIdentity.create("AKIDEXAMPLE", "secret")))
                         .endpointOverride(URI.create("https://s3.test"))
                         .addExecutionInterceptor(observer)
                         .build()) {
                    PresignedGetObjectRequest get = presigner.presignGetObject(
                        r -> r.signatureDuration(Duration.ofMinutes(10))
                              .getObjectRequest(g -> g.bucket("bucket").key("photos/a.jpg").versionId("v1")));
                    PresignedPutObjectRequest put = presigner.presignPutObject(
                        PutObjectPresignRequest.builder()
                                               .signatureDuration(Duration.ofMinutes(5))
                                               .putObjectRequest(PutObjectRequest.builder().bucket("bucket").key("k")
                                                                                 .contentType("text/plain").build())
                                               .build());
                    long seconds = Duration.between(java.time.Instant.now(), get.expiration()).getSeconds();
                    return get.url() + "\\n" + get.httpRequest().method() + "\\n" + get.isBrowserExecutable() + "\\n"
                           + (seconds > 590 && seconds <= 600) + "\\n"
                           + put.url() + "\\n" + put.httpRequest().method() + "\\n" + put.isBrowserExecutable() + "\\n"
                           + put.signedHeaders().get("content-type") + "\\n"
                           + signed.get() + "\\n" + transmitted.get() + "\\n" + get;
                }
            }
        }
        """.replace("PKG", PACKAGE);

    @TempDir
    static Path temp;

    private static URLClassLoader loader;
    private static Path sourceRoot;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("s3like.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("com.amazonaws.s3#AmazonS3", PACKAGE,
                                                                            models, temp.resolve("out")), List.of());
        sourceRoot = temp.resolve("sources");
        copyTree(result.sourceRoot(), sourceRoot);
        Path probe = Files.createDirectories(sourceRoot.resolve("example/probe")).resolve("PresignProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sourceRoot, temp.resolve("classes")).loader();
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void thePresignerSignsTheQueryStringAndNeverTransmits() throws Exception {
        String[] lines = probe().split("\n");

        String get = lines[0];
        assertTrue(get.startsWith("https://s3.test/bucket/photos/a.jpg?"), get);
        assertTrue(get.contains("versionId=v1"), get);
        assertTrue(get.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"), get);
        assertTrue(get.matches(".*X-Amz-Credential=AKIDEXAMPLE%2F\\d{8}%2Fus-east-1%2Fs3%2Faws4_request.*"), get);
        assertTrue(get.contains("X-Amz-Expires=600"), get);
        assertTrue(get.contains("X-Amz-SignedHeaders=host"), get);
        assertTrue(get.matches(".*X-Amz-Signature=[0-9a-f]{64}.*"), get);
        assertEquals("GET", lines[1]);
        assertEquals("true", lines[2], "a GET whose only header is host is browser-executable");
        assertEquals("true", lines[3], "the expiration is the signing time plus the signature duration");

        String put = lines[4];
        assertTrue(put.startsWith("https://s3.test/bucket/k?"), put);
        assertTrue(put.contains("X-Amz-Expires=300"), put);
        assertTrue(put.contains("X-Amz-SignedHeaders=content-type%3Bhost"), put);
        assertTrue(put.matches(".*X-Amz-Signature=[0-9a-f]{64}.*"), put);
        assertEquals("PUT", lines[5]);
        assertEquals("false", lines[6]);
        assertEquals("[text/plain]", lines[7], "the sender must send the signed Content-Type");

        assertEquals("2", lines[8], "each presign call runs the signing stage once");
        assertEquals("0", lines[9], "no presign call reaches transmission");
        assertFalse(lines[10].contains("X-Amz-Signature"), "toString never renders the signed URL");
    }

    @Test
    void onlyTheClassifiedOperationsArePresignedAndTheBuilderHasNoSignerOrTransportOption() throws Exception {
        Path presigner = sourceRoot.resolve(PACKAGE.replace('.', '/') + "/presigner/S3Presigner.java");
        String source = Files.readString(presigner, StandardCharsets.UTF_8);

        assertTrue(source.contains("presignGetObject(GetObjectPresignRequest request)"));
        assertTrue(source.contains("presignPutObject(PutObjectPresignRequest request)"));
        // ListBuckets is not in V2's S3Presigner; the classifier's other operations are absent from this model.
        assertFalse(source.contains("presignListBuckets"));
        assertFalse(source.contains("presignHeadObject"));
        for (String excluded : List.of("signer(", "Signer ", "presigner(", "httpClient(")) {
            assertFalse(source.contains(excluded), excluded);
        }
        Class<?> builder = Class.forName(PACKAGE + ".presigner.S3Presigner$Builder", false, loader);
        Map<String, Integer> methods = new java.util.TreeMap<>();
        for (var method : builder.getMethods()) {
            methods.merge(method.getName(), 1, Integer::sum);
        }
        assertEquals(Map.of("addExecutionInterceptor", 1, "build", 1, "credentialsProvider", 1, "endpointOverride", 1,
                            "region", 1), methods);
    }

    private static void copyTree(Path from, Path to) throws Exception {
        try (var walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    private static String probe() throws Exception {
        try {
            return (String) Class.forName("example.probe.PresignProbe", true, loader).getMethod("presign")
                                 .invoke(null);
        } catch (InvocationTargetException e) {
            throw new AssertionError("PresignProbe failed", e.getCause());
        }
    }
}
