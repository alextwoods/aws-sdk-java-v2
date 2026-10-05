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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.test.codegen.GeneratedClientTest.EchoHttp;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Requirements 13.6, 14.16, 19.6: a generated operation with {@code aws.protocols#httpChecksum} binds the same
 * {@link HttpChecksumMetadata} the Dynamic Client reads from the trait value ({@code HttpChecksumMetadata.fromNode}),
 * so a generated client computes the modeled request checksum on the wire.
 */
class GeneratedHttpChecksumTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.checksums";

    private static final String MODEL = """
        $version: "2"
        namespace example.checksums

        @aws.protocols#awsJson1_0
        @aws.api#service(sdkId: "Checksums", endpointPrefix: "checksums")
        @auth([])
        service Checksums {
            version: "2024-01-01"
            operations: [PutRequired, PutAlgorithm, Plain]
        }

        @aws.protocols#httpChecksum(requestChecksumRequired: true)
        operation PutRequired {
            input := {
                data: String
            }
        }

        @aws.protocols#httpChecksum(
            requestAlgorithmMember: "checksumAlgorithm"
            requestValidationModeMember: "checksumMode"
            responseAlgorithms: ["CRC32", "SHA256"]
        )
        operation PutAlgorithm {
            input := {
                data: String
                checksumAlgorithm: ChecksumAlgorithm
                checksumMode: ChecksumMode
            }
            output := {
                data: String
            }
        }

        operation Plain {
            input := {
                data: String
            }
        }

        enum ChecksumAlgorithm {
            CRC32
            CRC32C
            SHA1
            SHA256
        }

        enum ChecksumMode {
            ENABLED
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.regions.Region;
        import PKG.ChecksumsClient;

        public final class ChecksumProbe {
            private ChecksumProbe() {
            }

            public static void call(SdkSyncHttpClient http) {
                try (ChecksumsClient client = ChecksumsClient.builder().region(Region.of("us-east-1"))
                                                             .endpointOverride(URI.create("https://checksums.test"))
                                                             .httpClient(http).build()) {
                    client.putRequired(r -> r.data("required"));
                    client.putAlgorithm(r -> r.data("algorithm"));
                    client.plain(r -> r.data("plain"));
                }
            }
        }
        """.replace("PKG", PACKAGE);

    @TempDir
    static Path temp;

    private static URLClassLoader loader;
    private static Model model;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("checksums.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("example.checksums#Checksums", PACKAGE,
                                                                            models, temp.resolve("out")), List.of());
        Path sources = temp.resolve("sources");
        copyTree(result.sourceRoot(), sources);
        Files.writeString(Files.createDirectories(sources.resolve("example/probe")).resolve("ChecksumProbe.java"),
                          PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
        model = Model.assembler().discoverModels().addUnparsedModel("checksums.smithy", MODEL).assemble().unwrap();
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void generatedBindingsCarryTheTraitValueAsTheDynamicClientReadsIt() throws Exception {
        for (String operation : List.of("PutRequired", "PutAlgorithm")) {
            HttpChecksumMetadata expected = HttpChecksumMetadata.fromNode(
                model.expectShape(ShapeId.from("example.checksums#" + operation), OperationShape.class)
                     .findTrait(HttpChecksumMetadata.TRAIT_ID).orElseThrow().toNode());
            assertEquals(expected, binding(operation).httpChecksum().orElse(null), operation);
        }
        assertTrue(binding("Plain").httpChecksum().isEmpty());
    }

    @Test
    void generatedClientsSendTheModeledRequestChecksum() throws Exception {
        EchoHttp http = new EchoHttp(HttpTransportMode.SYNC);
        try {
            Class.forName("example.probe.ChecksumProbe", true, loader).getMethod("call", SdkSyncHttpClient.class)
                 .invoke(null, http);
        } catch (InvocationTargetException e) {
            throw new AssertionError("ChecksumProbe.call failed", e.getCause());
        }
        assertEquals(3, http.requests.size());
        // requestChecksumRequired without an algorithm member: Content-MD5.
        SdkHttpRequest required = http.requests.get(0);
        byte[] requiredBody = body(required);
        assertEquals(Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(requiredBody)),
                     required.headers().firstValue("Content-MD5").orElse(null));
        // An algorithm member left unset under the default WHEN_SUPPORTED calculation: CRC32.
        SdkHttpRequest algorithm = http.requests.get(1);
        CRC32 crc = new CRC32();
        crc.update(body(algorithm));
        byte[] crcBytes = ByteBuffer.allocate(4).putInt((int) crc.getValue()).array();
        assertEquals(Base64.getEncoder().encodeToString(crcBytes),
                     algorithm.headers().firstValue("x-amz-checksum-crc32").orElse(null));
        // No trait: no checksum.
        SdkHttpRequest plain = http.requests.get(2);
        for (int i = 0; i < plain.headers().size(); i++) {
            String name = plain.headers().name(i).toLowerCase(java.util.Locale.ROOT);
            assertTrue(!name.startsWith("x-amz-checksum-") && !name.equals("content-md5"), name);
        }
    }

    private static AwsOperation<?, ?> binding(String operation) throws Exception {
        Field field = Class.forName(PACKAGE + ".ChecksumsOperationBindings", true, loader)
                           .getDeclaredField(operation.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
        field.setAccessible(true);
        return (AwsOperation<?, ?>) field.get(null);
    }

    private static byte[] body(SdkHttpRequest request) {
        return request.body().buffered().asByteArray();
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
}
