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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.TestUtils;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Signs randomized requests with {@link DefaultAwsV4HttpSigner#sign} (which takes the {@link FastV4HeaderSigner} path when it
 * applies) and with {@link DefaultAwsV4HttpSigner#signWithoutFastPath} (the composed legacy path), and requires the same
 * outcome: the same exception, or the same method, URI, payload object and headers, which include {@code Authorization} and
 * every {@code X-Amz-*} header.
 */
class FastV4HeaderSignerDifferentialTest {
    private static final long SEED = Long.getLong("fastV4.seed", 20_260_920L);
    private static final int ITERATIONS = Integer.getInteger("fastV4.iterations", 10_000);

    private static final String[] HOSTS = {"test.com", "dynamodb.us-east-1.amazonaws.com", "a-b.example"};
    private static final String[] PATHS = {
        "", "/", "/foo", "/foo/bar/", "/a%20b", "/~user", "/x/./y", "/x/../y", "/x/y/..", "//double//slash", "/key%2Fslash",
        "/!*'()", "/caf%C3%A9", "/ab+c", "/a=b&c", "/%7E/%41", "/trailing/", "/a b", "/caf\u00e9", "relative", "/a/b/c/d/e/f"
    };
    private static final String[] QUERY_NAMES = {"a", "B", "list", "empty", "x y", "\u00e9", "~", "a+b", "", "Z", "aa"};
    private static final String[] QUERY_VALUES = {null, "", "1", "v v", "%", "=", "\u00e9", "a&b", "2", "/x/"};
    private static final String[] HEADER_NAMES = {
        "X-Amz-Target", "Content-Type", "content-length", "X-Custom", "x_under", "xZ", "x-a", "X-[", "User-Agent", "Connection",
        "x-amzn-trace-id", "Expect", "Transfer-Encoding", "X-Forwarded-For", "Host", "HOST", "X-Amz-Date",
        "x-amz-content-sha256", "X-Amz-Security-Token", "Authorization", "x-amz-trailer", "X-Amz-Meta-\u00dcn\u00ef",
        "x-amz-decoded-content-length"
    };
    private static final String[] HEADER_VALUES = {
        "", "  a  b  ", "v", "a\tb\nc", "\u00e9t\u00e9", "x,y", "  ", "application/x-amz-json-1.0", "DynamoDB_20120810.GetItem",
        "\u000b\f\r"
    };
    private static final String[] REGIONS = {"us-east-1", "eu-west-2", "cn-north-1", "us-gov-west-1"};
    private static final String[] SERVICES = {"dynamodb", "s3", "execute-api", "sqs"};
    private static final String[] ACCESS_KEYS = {"AKIDEXAMPLE", " AKIDPADDED ", "ASIA0000"};
    private static final String[] SECRETS = {"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "secret", " padded-secret "};
    private static final long[] INSTANTS = {
        1_600_041_599L, 1_600_041_600L, 1_440_938_160L, 951_782_400L, 4_102_444_799L
    };

    private final DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    void randomizedRequests_fastPathMatchesLegacyPath() {
        Random random = new Random(SEED);
        int fastPathCases = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            SignRequest<? extends AwsCredentialsIdentity> request = randomRequest(random);
            if (FastV4HeaderSigner.canSign(request)) {
                fastPathCases++;
            }
            assertSameOutcome(request, "seed=" + SEED + ", iteration=" + i);
        }
        // Most cases must exercise the fast path; the rest check that dispatch leaves other cases on the legacy path.
        assertThat(fastPathCases).isGreaterThan(ITERATIONS * 6 / 10);
    }

    @Test
    void concurrentSigning_fastPathMatchesLegacyPath() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                long threadSeed = SEED + t + 1;
                futures.add(executor.submit(() -> {
                    Random random = new Random(threadSeed);
                    for (int i = 0; i < 1_000; i++) {
                        assertSameOutcome(randomRequest(random), "seed=" + threadSeed + ", iteration=" + i);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(2, TimeUnit.MINUTES);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * The per-thread HMAC pad state is keyed by the signing key. Four threads sign requests whose scope (secret key, date,
     * region and service) changes or repeats at random between calls, so each thread both reuses and replaces its pads, and
     * every result must match the legacy path.
     */
    @Test
    void concurrentSigning_interleavedSigningScopes_fastPathMatchesLegacyPath() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                long threadSeed = SEED + 100 + t;
                futures.add(executor.submit(() -> {
                    Random random = new Random(threadSeed);
                    for (int i = 0; i < 2_000; i++) {
                        SignRequest<? extends AwsCredentialsIdentity> request = randomScopeRequest(random);
                        assertThat(FastV4HeaderSigner.canSign(request)).isTrue();
                        assertSameOutcome(request, "seed=" + threadSeed + ", iteration=" + i);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(2, TimeUnit.MINUTES);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * {@link V4SigningResources#hmacSha256} against {@code Mac} for keys of every length up to past two SHA-256 blocks
     * (longer than 64 bytes is hashed first), with keys that change, repeat, or repeat with the same content in a new array,
     * and on the {@code Mac.init} fallback used when the digest cannot be cloned.
     */
    @Test
    void hmacSha256_matchesMac() throws Exception {
        Random random = new Random(SEED + 200);
        Mac mac = Mac.getInstance("HmacSHA256");
        for (boolean cloneSupported : new boolean[] {true, false}) {
            V4SigningResources resources = V4SigningResources.acquire();
            try {
                if (!cloneSupported) {
                    Field field = V4SigningResources.class.getDeclaredField("digestCloneSupported");
                    field.setAccessible(true);
                    field.setBoolean(resources, false);
                }
                byte[] key = randomKey(random, 32);
                for (int i = 0; i < 3_000; i++) {
                    int change = random.nextInt(4);
                    if (change == 0) {
                        key = randomKey(random, 1 + random.nextInt(130));
                    } else if (change == 1) {
                        key = key.clone();
                    }
                    byte[] data = new byte[random.nextInt(300)];
                    random.nextBytes(data);
                    if (resources.bytes.length < data.length) {
                        resources.bytes = new byte[data.length];
                    }
                    System.arraycopy(data, 0, resources.bytes, 0, data.length);

                    mac.init(new SecretKeySpec(key, "HmacSHA256"));
                    byte[] expected = mac.doFinal(data);
                    assertThat(resources.hmacSha256(data.length, key))
                        .as("cloneSupported=%s, iteration=%s, key length=%s", cloneSupported, i, key.length)
                        .isEqualTo(expected);
                }
            } finally {
                resources.release();
            }
        }
    }

    private static byte[] randomKey(Random random, int length) {
        byte[] key = new byte[length];
        random.nextBytes(key);
        return key;
    }

    /**
     * A fast-path request with a scope picked from a small set, so consecutive calls often share a signing key.
     */
    private static SignRequest<? extends AwsCredentialsIdentity> randomScopeRequest(Random random) {
        SdkHttpRequest http = SdkHttpRequest.builder()
                                            .method(SdkHttpMethod.POST)
                                            .protocol("https")
                                            .host(pick(random, HOSTS))
                                            .encodedPath(pick(random, PATHS))
                                            .putHeader("Content-Type", "application/x-amz-json-1.0")
                                            .putHeader("X-Amz-Target", "DynamoDB_20120810.GetItem")
                                            .build();
        AwsCredentialsIdentity identity = random.nextBoolean()
                                          ? AwsCredentialsIdentity.create("AKIDEXAMPLE", pick(random, SECRETS))
                                          : AwsSessionCredentialsIdentity.create("AKIDEXAMPLE", pick(random, SECRETS), "token");
        // Two instants a few seconds apart on each side of a UTC midnight: the date stamp, and so the key, changes.
        Instant instant = Instant.ofEpochSecond(1_600_041_598L + random.nextInt(4));
        byte[] body = new byte[random.nextInt(64)];
        random.nextBytes(body);
        return SignRequest.builder(identity)
                          .request(http)
                          .payload(ContentStreamProvider.fromByteArray(body))
                          .putProperty(AwsV4HttpSigner.REGION_NAME, pick(random, REGIONS))
                          .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, pick(random, SERVICES))
                          .putProperty(AwsV4HttpSigner.SIGNING_CLOCK, Clock.fixed(instant, ZoneOffset.UTC))
                          .build();
    }

    private void assertSameOutcome(SignRequest<? extends AwsCredentialsIdentity> request, String description) {
        Outcome legacy = Outcome.of(() -> signer.signWithoutFastPath(request));
        Outcome current = Outcome.of(() -> signer.sign(request));

        if (legacy.failure != null) {
            assertThat(current.failure).as(description).isNotNull();
            assertThat(current.failure.getClass()).as(description).isEqualTo(legacy.failure.getClass());
            assertThat(current.failure.getMessage()).as(description).isEqualTo(legacy.failure.getMessage());
            return;
        }
        assertThat(current.failure).as(description).isNull();

        SdkHttpRequest legacyRequest = legacy.result.request();
        SdkHttpRequest currentRequest = current.result.request();
        assertThat(xAmzAndAuthorization(currentRequest)).as(description).isEqualTo(xAmzAndAuthorization(legacyRequest));
        assertThat(currentRequest.headers()).as(description).isEqualTo(legacyRequest.headers());
        assertThat(currentRequest.method()).as(description).isEqualTo(legacyRequest.method());
        // Compared by component: getUri() rejects some raw paths that signing accepts when the path is not normalized.
        assertThat(currentRequest.protocol()).as(description).isEqualTo(legacyRequest.protocol());
        assertThat(currentRequest.host()).as(description).isEqualTo(legacyRequest.host());
        assertThat(currentRequest.port()).as(description).isEqualTo(legacyRequest.port());
        assertThat(currentRequest.encodedPath()).as(description).isEqualTo(legacyRequest.encodedPath());
        assertThat(currentRequest.rawQueryParameters()).as(description).isEqualTo(legacyRequest.rawQueryParameters());
        if (FastV4HeaderSigner.canSign(request)) {
            // Both paths pass the caller's payload through unchanged.
            assertThat(current.result.payload().orElse(null)).as(description)
                                                              .isSameAs(legacy.result.payload().orElse(null))
                                                              .isSameAs(request.payload().orElse(null));
        } else {
            // Legacy-only cases may wrap the payload in a new object per call (chunk encoding).
            assertThat(current.result.payload().isPresent()).as(description)
                                                            .isEqualTo(legacy.result.payload().isPresent());
        }
    }

    private static Map<String, List<String>> xAmzAndAuthorization(SdkHttpRequest request) {
        Map<String, List<String>> result = new TreeMap<>();
        request.forEachHeader((name, values) -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("x-amz-") || lower.equals("authorization")) {
                result.put(name, values);
            }
        });
        return result;
    }

    private static SignRequest<? extends AwsCredentialsIdentity> randomRequest(Random random) {
        SdkHttpRequest.Builder http = SdkHttpRequest.builder()
                                                    .method(pick(random, SdkHttpMethod.values()))
                                                    .protocol(random.nextInt(5) == 0 ? "http" : "https")
                                                    .host(pick(random, HOSTS))
                                                    .encodedPath(pick(random, PATHS));
        int port = random.nextInt(5);
        if (port == 1) {
            http.port(443);
        } else if (port == 2) {
            http.port(80);
        } else if (port == 3) {
            http.port(8443);
        }

        int queryCount = random.nextInt(5);
        for (int q = 0; q < queryCount; q++) {
            http.appendRawQueryParameter(pick(random, QUERY_NAMES), pick(random, QUERY_VALUES));
        }

        int headerCount = random.nextInt(7);
        for (int h = 0; h < headerCount; h++) {
            String name = pick(random, HEADER_NAMES);
            int shape = random.nextInt(10);
            if (shape == 0) {
                http.putHeader(name, Collections.emptyList());
            } else if (shape < 3) {
                http.appendHeader(name, pick(random, HEADER_VALUES));
                http.appendHeader(name, pick(random, HEADER_VALUES));
            } else {
                http.appendHeader(name, pick(random, HEADER_VALUES));
            }
        }

        AwsCredentialsIdentity identity;
        int identityKind = random.nextInt(20);
        if (identityKind == 0) {
            identity = new TestUtils.AnonymousCredentialsIdentity();
        } else if (identityKind < 9) {
            identity = AwsSessionCredentialsIdentity.create(pick(random, ACCESS_KEYS), pick(random, SECRETS),
                                                            random.nextBoolean() ? "token" : " session/token== ");
        } else {
            identity = AwsCredentialsIdentity.create(pick(random, ACCESS_KEYS), pick(random, SECRETS));
        }

        long epochSecond = random.nextBoolean()
                           ? pick(random, INSTANTS)
                           : 946_684_800L + (long) (random.nextDouble() * 3_155_760_000L);
        Instant instant = Instant.ofEpochSecond(epochSecond, random.nextInt(1_000_000_000));

        SignRequest.Builder<AwsCredentialsIdentity> builder =
            SignRequest.builder(identity)
                       .request(http.build())
                       .putProperty(AwsV4HttpSigner.REGION_NAME, pick(random, REGIONS))
                       .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, pick(random, SERVICES))
                       .putProperty(AwsV4HttpSigner.SIGNING_CLOCK, Clock.fixed(instant, ZoneOffset.UTC));

        int payloadKind = random.nextInt(4);
        if (payloadKind == 1) {
            builder.payload(ContentStreamProvider.fromByteArray(new byte[0]));
        } else if (payloadKind >= 2) {
            byte[] body = new byte[1 + random.nextInt(payloadKind == 2 ? 512 : 20_000)];
            random.nextBytes(body);
            builder.payload(ContentStreamProvider.fromByteArray(body));
        }

        putRandomBoolean(random, builder, AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED);
        putRandomBoolean(random, builder, AwsV4HttpSigner.DOUBLE_URL_ENCODE);
        putRandomBoolean(random, builder, AwsV4HttpSigner.NORMALIZE_PATH);

        // Cases the fast path must leave to the legacy path.
        switch (random.nextInt(25)) {
            case 0:
                builder.putProperty(AwsV4HttpSigner.AUTH_LOCATION, AuthLocation.QUERY_STRING);
                break;
            case 1:
                builder.putProperty(AwsV4HttpSigner.CHUNK_ENCODING_ENABLED, true);
                break;
            case 2:
                builder.putProperty(AwsV4HttpSigner.CHECKSUM_ALGORITHM, DefaultChecksumAlgorithm.CRC32);
                break;
            case 3:
                builder.request(http.putHeader("Content-Type", "application/vnd.amazon.eventstream").build());
                break;
            case 4:
                builder.putProperty(AwsV4HttpSigner.EXPIRATION_DURATION, Duration.ofMinutes(5));
                break;
            case 5:
                builder.putProperty(AwsV4HttpSigner.AUTH_LOCATION, AuthLocation.HEADER);
                builder.putProperty(AwsV4HttpSigner.CHUNK_ENCODING_ENABLED, false);
                break;
            default:
                break;
        }
        return builder.build();
    }

    private static void putRandomBoolean(Random random,
                                         SignRequest.Builder<AwsCredentialsIdentity> builder,
                                         software.amazon.awssdk.http.auth.spi.signer.SignerProperty<Boolean> property) {
        int choice = random.nextInt(3);
        if (choice > 0) {
            builder.putProperty(property, choice == 1);
        }
    }

    private static <T> T pick(Random random, T[] values) {
        return values[random.nextInt(values.length)];
    }

    private static long pick(Random random, long[] values) {
        return values[random.nextInt(values.length)];
    }

    private static final class Outcome {
        private final SignedRequest result;
        private final RuntimeException failure;

        private Outcome(SignedRequest result, RuntimeException failure) {
            this.result = result;
            this.failure = failure;
        }

        static Outcome of(java.util.function.Supplier<SignedRequest> signing) {
            try {
                return new Outcome(signing.get(), null);
            } catch (RuntimeException e) {
                return new Outcome(null, e);
            }
        }
    }
}
