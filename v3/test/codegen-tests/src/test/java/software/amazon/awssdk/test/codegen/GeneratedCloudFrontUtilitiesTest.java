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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.10 and 16.5: the generated {@code CloudFrontUtilities} of a synthetic service with CloudFront's
 * shape ID. Signed URLs and cookies for canned and custom policies are checked against V2's URL/cookie shapes and
 * V2's policy JSON, and each signature is verified with the public half of a generated test key (RSA with SHA-1 and
 * SHA-256, and EC).
 */
class GeneratedCloudFrontUtilitiesTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.cloudfrontlike";
    private static final long EXPIRES = 1_900_000_000L;
    private static final long ACTIVE = 1_800_000_000L;

    private static final String MODEL = """
        $version: "2"
        namespace com.amazonaws.cloudfront

        @aws.protocols#restXml
        @aws.auth#sigv4(name: "cloudfront")
        @aws.api#service(sdkId: "CloudFront", endpointPrefix: "cloudfront")
        service Cloudfront2020_05_31 {
            version: "2020-05-31"
            operations: [ListDistributions]
        }

        @readonly
        @http(method: "GET", uri: "/2020-05-31/distribution")
        operation ListDistributions {
            input := {}
            output := {}
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.security.PrivateKey;
        import java.time.Instant;
        import PKG.CloudFrontUtilities;
        import PKG.cookie.CookiesForCannedPolicy;
        import PKG.cookie.CookiesForCustomPolicy;
        import PKG.url.SignedUrl;
        import PKG.utils.SigningHashAlgorithm;

        public final class CloudFrontProbe {
            private CloudFrontProbe() {
            }

            private static SigningHashAlgorithm hash(String hash) {
                return hash.isEmpty() ? null : SigningHashAlgorithm.valueOf(hash);
            }

            public static String cannedUrl(PrivateKey key, String resource, String hash) {
                SignedUrl url = CloudFrontUtilities.create().getSignedUrlWithCannedPolicy(
                    r -> r.resourceUrl(resource).privateKey(key).keyPairId("KPID")
                          .expirationDate(Instant.ofEpochSecond(EXPIRES)).hashAlgorithm(hash(hash)));
                return url.url() + "\\n" + url.protocol() + "\\n" + url.domain() + "\\n" + url.encodedPath() + "\\n"
                       + url.createHttpGetRequest().host();
            }

            public static String customUrl(PrivateKey key, String resource, String pattern, String ip) {
                return CloudFrontUtilities.create().getSignedUrlWithCustomPolicy(
                    r -> r.resourceUrl(resource).resourceUrlPattern(pattern.isEmpty() ? null : pattern)
                          .privateKey(key).keyPairId("KPID").expirationDate(Instant.ofEpochSecond(EXPIRES))
                          .activeDate(Instant.ofEpochSecond(ACTIVE)).ipRange(ip.isEmpty() ? null : ip)).url();
            }

            public static String cannedCookies(PrivateKey key, String resource, String hash) {
                CookiesForCannedPolicy cookies = CloudFrontUtilities.create().getCookiesForCannedPolicy(
                    r -> r.resourceUrl(resource).privateKey(key).keyPairId("KPID")
                          .expirationDate(Instant.ofEpochSecond(EXPIRES)).hashAlgorithm(hash(hash)));
                return cookies.resourceUrl() + "\\n" + cookies.keyPairIdHeaderValue() + "\\n"
                       + cookies.signatureHeaderValue() + "\\n" + cookies.expiresHeaderValue() + "\\n"
                       + cookies.hashAlgorithmHeaderValue().orElse("-");
            }

            public static String customCookies(PrivateKey key, String resource) {
                CookiesForCustomPolicy cookies = CloudFrontUtilities.create().getCookiesForCustomPolicy(
                    r -> r.resourceUrl(resource).privateKey(key).keyPairId("KPID")
                          .expirationDate(Instant.ofEpochSecond(EXPIRES)).ipRange("10.0.0.0/8"));
                return cookies.keyPairIdHeaderValue() + "\\n" + cookies.signatureHeaderValue() + "\\n"
                       + cookies.policyHeaderValue() + "\\n" + cookies.hashAlgorithmHeaderValue().orElse("-") + "\\n"
                       + cookies.createHttpGetRequest().headers().firstValue("Cookie").orElse("-");
            }
        }
        """.replace("PKG", PACKAGE).replace("EXPIRES", EXPIRES + "L").replace("ACTIVE", ACTIVE + "L");

    private static final Pattern SIGNATURE = Pattern.compile("Signature=([^&\\n]+)");

    @TempDir
    static Path temp;

    private static URLClassLoader loader;
    private static KeyPair rsa;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("cloudfrontlike.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("com.amazonaws.cloudfront#Cloudfront2020_05_31", PACKAGE, models,
                                  temp.resolve("out")), List.of());
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
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("CloudFrontProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        rsa = generator.generateKeyPair();
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void cannedPolicyUrl_hasV2sShapeAndAVerifiableSha1Signature() throws Exception {
        String resource = "https://d111111abcdef8.cloudfront.net/images/a%20b.jpg?size=large";
        String[] lines = probe("cannedUrl", rsa.getPrivate(), resource, "").split("\n");

        String signature = signature(lines[0]);
        assertEquals(resource + "&Expires=" + EXPIRES + "&Signature=" + signature + "&Key-Pair-Id=KPID", lines[0]);
        assertEquals("https", lines[1]);
        assertEquals("d111111abcdef8.cloudfront.net", lines[2]);
        assertEquals("/images/a%20b.jpg?size=large&Expires=" + EXPIRES + "&Signature=" + signature
                     + "&Key-Pair-Id=KPID", lines[3]);
        assertEquals("d111111abcdef8.cloudfront.net", lines[4]);
        verify("SHA1withRSA", rsa.getPublic(), cannedPolicy(resource), signature);
    }

    @Test
    void sha256_addsTheHashAlgorithmParameter() throws Exception {
        String resource = "https://d111111abcdef8.cloudfront.net/doc.txt";
        String url = probe("cannedUrl", rsa.getPrivate(), resource, "SHA256").split("\n")[0];

        String signature = signature(url);
        assertEquals(resource + "?Expires=" + EXPIRES + "&Signature=" + signature
                     + "&Key-Pair-Id=KPID&Hash-Algorithm=SHA256", url);
        verify("SHA256withRSA", rsa.getPublic(), cannedPolicy(resource), signature);
    }

    @Test
    void customPolicyUrl_signsTheEncodedV2Policy() throws Exception {
        String resource = "https://d111111abcdef8.cloudfront.net/videos/v.mp4";
        String pattern = "https://d111111abcdef8.cloudfront.net/videos/*";
        String url = probe("customUrl", rsa.getPrivate(), resource, pattern, "192.0.2.0/24");

        String policy = "{\"Statement\":[{\"Resource\":\"" + pattern + "\",\"Condition\":{\"DateLessThan\":"
                        + "{\"AWS:EpochTime\":" + EXPIRES + "},\"IpAddress\":{\"AWS:SourceIp\":\"192.0.2.0/24\"},"
                        + "\"DateGreaterThan\":{\"AWS:EpochTime\":" + ACTIVE + "}}}]}";
        String signature = signature(url);
        assertEquals(resource + "?Policy=" + urlSafe(policy.getBytes(StandardCharsets.UTF_8)) + "&Signature="
                     + signature + "&Key-Pair-Id=KPID", url);
        verify("SHA1withRSA", rsa.getPublic(), policy, signature);
    }

    @Test
    void cannedCookies_carryV2sCookieValues() throws Exception {
        String resource = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
        String[] lines = probe("cannedCookies", rsa.getPrivate(), resource, "").split("\n");

        assertEquals(resource, lines[0]);
        assertEquals("CloudFront-Key-Pair-Id=KPID", lines[1]);
        assertTrue(lines[2].startsWith("CloudFront-Signature="), lines[2]);
        assertEquals("CloudFront-Expires=" + EXPIRES, lines[3]);
        assertEquals("-", lines[4]);
        verify("SHA1withRSA", rsa.getPublic(), cannedPolicy(resource),
               lines[2].substring("CloudFront-Signature=".length()));
    }

    @Test
    void customCookies_withAnEcKey_carryThePolicyAndARequest() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair ec = generator.generateKeyPair();
        String resource = "https://d111111abcdef8.cloudfront.net/s3ObjectKey";
        String[] lines = probe("customCookies", ec.getPrivate(), resource).split("\n");

        String policy = "{\"Statement\":[{\"Resource\":\"" + resource + "\",\"Condition\":{\"DateLessThan\":"
                        + "{\"AWS:EpochTime\":" + EXPIRES + "},\"IpAddress\":{\"AWS:SourceIp\":\"10.0.0.0/8\"}}}]}";
        assertEquals("CloudFront-Key-Pair-Id=KPID", lines[0]);
        assertEquals("CloudFront-Policy=" + urlSafe(policy.getBytes(StandardCharsets.UTF_8)), lines[2]);
        assertEquals("-", lines[3]);
        assertEquals(lines[2] + "; " + lines[1] + "; " + lines[0], lines[4]);
        verify("SHA1withECDSA", ec.getPublic(), policy, lines[1].substring("CloudFront-Signature=".length()));
    }

    @Test
    void resourcesWithQuotesAreRejected() {
        AssertionError failure = assertThrows(AssertionError.class, () ->
            probe("cannedUrl", rsa.getPrivate(), "https://d111111abcdef8.cloudfront.net/a\"b", ""));
        assertTrue(failure.getCause() instanceof IllegalArgumentException, String.valueOf(failure.getCause()));
    }

    private static String cannedPolicy(String resource) {
        return "{\"Statement\":[{\"Resource\":\"" + resource + "\",\"Condition\":{\"DateLessThan\":"
               + "{\"AWS:EpochTime\":" + EXPIRES + "}}}]}";
    }

    private static String urlSafe(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes).replace('+', '-').replace('=', '_').replace('/', '~');
    }

    private static String signature(String url) {
        Matcher matcher = SIGNATURE.matcher(url);
        assertTrue(matcher.find(), url);
        return matcher.group(1);
    }

    private static void verify(String algorithm, PublicKey key, String policy, String urlSafeSignature)
        throws Exception {
        byte[] signature = Base64.getDecoder().decode(urlSafeSignature.replace('-', '+').replace('_', '=')
                                                                      .replace('~', '/'));
        Signature verifier = Signature.getInstance(algorithm);
        verifier.initVerify(key);
        verifier.update(policy.getBytes(StandardCharsets.UTF_8));
        assertTrue(verifier.verify(signature), algorithm + " signature of " + policy);
    }

    private static String probe(String method, PrivateKey key, String... arguments) throws Exception {
        Class<?>[] types = new Class<?>[arguments.length + 1];
        Object[] values = new Object[arguments.length + 1];
        types[0] = PrivateKey.class;
        values[0] = key;
        for (int i = 0; i < arguments.length; i++) {
            types[i + 1] = String.class;
            values[i + 1] = arguments[i];
        }
        try {
            return (String) Class.forName("example.probe.CloudFrontProbe", true, loader).getMethod(method, types)
                                 .invoke(null, values);
        } catch (InvocationTargetException e) {
            throw new AssertionError("CloudFrontProbe." + method + " failed", e.getCause());
        }
    }
}
