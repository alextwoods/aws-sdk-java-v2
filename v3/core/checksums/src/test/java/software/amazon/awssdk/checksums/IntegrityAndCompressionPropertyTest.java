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

package software.amazon.awssdk.checksums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import java.util.zip.GZIPInputStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.checksums.ChecksumValidationResult.Status;
import software.amazon.awssdk.checksums.ResponseChecksumFeature.ValidatedResponse;
import software.amazon.awssdk.checksums.TestSupport.MapStruct;
import software.amazon.awssdk.checksums.compression.CompressionSettings;
import software.amazon.awssdk.checksums.compression.RequestCompressionFeature;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;

/**
 * Property 46: Integrity and compression transformations are exact (Requirements 13.6, 13.7, 13.8).
 *
 * <p>Every oracle here is independent of the runtime under test: digests come from {@link CRC32}, {@link CRC32C},
 * {@link MessageDigest} and a bitwise CRC-64/NVME written below (anchored to the published check values by
 * {@link #oraclesMatchPublishedCheckValues}); algorithm IDs, header names and the default algorithm are the literal
 * Smithy {@code httpChecksum} values; {@code aws-chunked} bodies are parsed by a decoder written here from the framing
 * grammar; and gzip output is decompressed by {@link GZIPInputStream} and its RFC 1952 trailer checked by hand.
 * Streaming payloads are delivered through a blocking view with random short reads and a demand-driven view with
 * random chunk sizes and random subscriber demand; both views of every transformed body must yield identical bytes.
 * <ul>
 *   <li>{@link #requestChecksumCoversExactModeledScope}: for generated models (optional, required, legacy
 *   {@code httpChecksumRequired}), caller-selected or defaulted algorithms, calculation modes, payload signing, chunk
 *   sizes, and buffered or streaming bodies, the checksum is sent at its modeled location (header or unsigned
 *   trailer, or only declared for a chunk signer) and equals the oracle digest of exactly the payload bytes; the
 *   {@code aws-chunked} encoding frames exactly the payload with exact {@code Content-Length} and
 *   {@code x-amz-decoded-content-length}.</li>
 *   <li>{@link #responseChecksumAcceptsCorrectAndNeverReturnsCorruptedIntegrity}: for generated modeled response
 *   algorithm lists, present, decoy, unmodeled, unsupported and composite checksum headers, and responses corrupted
 *   by one random byte of payload or checksum, exactly the first modeled supported non-composite header is validated;
 *   correct data is accepted unchanged, and corrupted data never yields a response (buffered) or a completed read
 *   (streaming).</li>
 *   <li>{@link #requestCompressionAppliesExactAlgorithmThresholdAndFields}: for generated encoding priority lists,
 *   thresholds at and around their boundaries (0, 1, the 10240 default, the 10485760 maximum), and buffered, sized
 *   and unsized streaming payloads, gzip is applied exactly when the modeled rule says so, decompresses to the
 *   payload, and sets exactly the modeled {@code Content-Encoding} and {@code Content-Length} fields.</li>
 *   <li>{@link #requestChecksumCoversCompressedPayload}: in pipeline order (compression, then checksum), the
 *   checksum covers exactly the compressed bytes that are transmitted.</li>
 * </ul>
 */
class IntegrityAndCompressionPropertyTest {
    /** Algorithms this runtime computes and validates for {@code httpChecksum}, as the requirements list them. */
    private static final List<String> FLEXIBLE = List.of("CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256", "SHA512");
    /** Every algorithm ID the {@code httpChecksum} trait can model. */
    private static final List<String> ALL_IDS = List.of("CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256", "SHA512",
                                                        "MD5", "XXHASH64", "XXHASH3", "XXHASH128");
    private static final String DEFAULT_ALGORITHM = "CRC32";
    private static final int DEFAULT_CHUNK_SIZE = 64 * 1024;
    private static final String ALGORITHM_HEADER = RequestChecksumFeatureTest.ALGORITHM_HEADER;
    private static final long CRC64_NVME_REFLECTED_POLY = 0x9A6C9329AC4BC9B5L;

    private static final Schema OPERATION_SCHEMA = RequestChecksumFeatureTest.OPERATION_SCHEMA;
    private static final Schema LEGACY_OPERATION_SCHEMA =
        Schema.createOperation(RequestChecksumFeatureTest.OPERATION, new HttpChecksumRequiredTrait());
    private static final Schema INPUT = RequestChecksumFeatureTest.INPUT;
    private static final Schema STREAMING_INPUT = RequestChecksumFeatureTest.STREAMING_INPUT;

    private static final Schema MODE_INPUT = Schema.structureBuilder(ShapeId.from("com.example#GetThingInput"))
                                                   .putMember("ChecksumMode", RequestChecksumFeatureTest.STRING,
                                                              new HttpHeaderTrait("x-amz-checksum-mode"))
                                                   .build();
    private static final Schema OUTPUT = Schema.structureBuilder(ShapeId.from("com.example#GetThingOutput"))
                                               .putMember("Data", RequestChecksumFeatureTest.BLOB,
                                                          new HttpPayloadTrait())
                                               .build();
    private static final Schema STREAMING_OUTPUT =
        Schema.structureBuilder(ShapeId.from("com.example#GetStreamOutput"))
              .putMember("Body", RequestChecksumFeatureTest.STREAM, new HttpPayloadTrait())
              .build();

    enum ModelShape { FLEXIBLE_OPTIONAL, FLEXIBLE_REQUIRED, LEGACY_REQUIRED }

    enum BodyShape { BYTES, STREAM, STREAM_UNKNOWN_LENGTH }

    enum Entropy { RANDOM, REPETITIVE, ZEROS }

    enum Corruption { NONE, PAYLOAD_BYTE, CHECKSUM_BYTE }

    /** Payload bytes, printed by size and kind rather than content. */
    record Payload(byte[] bytes, Entropy entropy) {
        static Payload of(int size, long seed, Entropy entropy) {
            Random random = new Random(seed);
            byte[] bytes = new byte[size];
            switch (entropy) {
                case RANDOM -> random.nextBytes(bytes);
                case REPETITIVE -> {
                    byte[] pattern = new byte[1 + random.nextInt(16)];
                    random.nextBytes(pattern);
                    for (int i = 0; i < size; i++) {
                        bytes[i] = random.nextInt(64) == 0 ? (byte) random.nextInt() : pattern[i % pattern.length];
                    }
                }
                case ZEROS -> {
                    // All zero.
                }
            }
            return new Payload(bytes, entropy);
        }

        @Override
        public String toString() {
            return "Payload[" + bytes.length + " bytes, " + entropy + "]";
        }
    }

    /**
     * How a streaming source delivers its bytes: the seed of the random short reads and chunk sizes, and the chunk
     * bound of the body (and so of the bodies transformed from it).
     */
    record Shaping(long seed, int maxChunk) {
    }

    record CompressionCase(int threshold, Payload payload) {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------------------------------------------------------

    @Example
    void oraclesMatchPublishedCheckValues() {
        byte[] check = "123456789".getBytes(StandardCharsets.US_ASCII);
        assertThat(ByteBuffer.wrap(oracleDigest("CRC32", check)).getInt()).isEqualTo(0xCBF43926);
        assertThat(ByteBuffer.wrap(oracleDigest("CRC32C", check)).getInt()).isEqualTo(0xE3069283);
        assertThat(ByteBuffer.wrap(oracleDigest("CRC64NVME", check)).getLong()).isEqualTo(0xAE8B14860A799888L);
        assertThat(decodeAwsChunked("3\r\nabc\r\n0\r\nx-amz-checksum-crc32:AAAAAA==\r\n\r\n"
                                        .getBytes(StandardCharsets.US_ASCII)))
            .isEqualTo(new Chunked(List.of(3), "abc".getBytes(StandardCharsets.US_ASCII),
                                   Map.of("x-amz-checksum-crc32", "AAAAAA==")));
    }

    @Label("Property 46 (request checksum): the checksum covers the exact modeled scope, algorithm and location")
    @Property(tries = 1_000)
    void requestChecksumCoversExactModeledScope(@ForAll("payloads") Payload payload,
                                                @ForAll("modelShapes") ModelShape model,
                                                @ForAll("mostlyTrue") boolean streamingModel,
                                                @ForAll BodyShape bodyShape,
                                                @ForAll("selectedAlgorithms") Optional<String> selected,
                                                @ForAll RequestChecksumCalculation calculation,
                                                @ForAll PayloadSigning signing,
                                                @ForAll("chunkSizes") OptionalInt chunkSize,
                                                @ForAll("shapings") Shaping shaping) {
        byte[] bytes = payload.bytes();
        Schema input = streamingModel ? STREAMING_INPUT : INPUT;
        RequestChecksumFeature feature = requestFeature(model, input, chunkSize);
        MapStruct struct = new MapStruct(input);
        selected.ifPresent(id -> struct.with("ChecksumAlgorithm", id));
        SdkHttpRequest request = requestWithBody(bytes, bodyShape, shaping);

        SdkHttpRequest result = feature.apply(struct, request, calculation, signing);

        if (model == ModelShape.LEGACY_REQUIRED) {
            // No algorithm member: the legacy trait's Content-MD5 over exactly the payload.
            Statistics.label("location").collect("Content-MD5");
            assertThat(result.headers().firstValue("Content-MD5")).contains(oracleBase64("MD5", bytes));
            assertThat(result.headers().contains("x-amz-trailer")).isFalse();
            assertThat(readAllViews(result.body(), shaping)).isEqualTo(bytes);
            return;
        }
        boolean required = model == ModelShape.FLEXIBLE_REQUIRED;
        Optional<String> algorithm = selected.isPresent() ? selected
            : required || calculation == RequestChecksumCalculation.WHEN_SUPPORTED ? Optional.of(DEFAULT_ALGORITHM)
            : Optional.empty();
        if (algorithm.isEmpty()) {
            Statistics.label("location").collect("none");
            assertThat(result).isSameAs(request);
            return;
        }
        String name = headerName(algorithm.get());
        String expected = oracleBase64(algorithm.get(), bytes);
        // The implicit member value is sent only when the SDK chose the algorithm.
        assertThat(result.headers().firstValue(ALGORITHM_HEADER)).isEqualTo(
            selected.isEmpty() ? Optional.of(DEFAULT_ALGORITHM) : Optional.empty());

        boolean trailer = streamingModel && bodyShape != BodyShape.BYTES && signing != PayloadSigning.SIGNED;
        if (!trailer) {
            Statistics.label("location").collect("header");
            assertThat(result.headers().firstValue(name)).contains(expected);
            assertThat(result.headers().contains("x-amz-trailer")).isFalse();
            assertThat(result.headers().contains("Content-Encoding")).isFalse();
            assertThat(readAllViews(result.body(), shaping)).isEqualTo(bytes);
            return;
        }
        assertThat(result.headers().firstValue("x-amz-trailer")).contains(name);
        assertThat(result.headers().contains(name)).isFalse();
        if (signing == PayloadSigning.STREAMING_SIGNED) {
            // Only declared: the chunk signer frames the untouched payload and writes the trailer.
            Statistics.label("location").collect("declared trailer");
            assertThat(result.body()).isSameAs(request.body());
            assertThat(result.headers().contains("Content-Encoding")).isFalse();
            return;
        }
        Statistics.label("location").collect("unsigned trailer");
        byte[] encoded = readAllViews(result.body(), shaping);
        Chunked chunked = decodeAwsChunked(encoded);
        int frameSize = chunkSize.orElse(DEFAULT_CHUNK_SIZE);
        assertThat(chunked.data()).isEqualTo(bytes);
        assertExactFrames(chunked.frames(), bytes.length, frameSize);
        assertThat(chunked.trailers()).isEqualTo(Map.of(name, expected));
        assertThat(result.headers().firstValue("Content-Encoding")).contains("aws-chunked");
        assertThat(result.headers().firstValue("x-amz-content-sha256")).contains("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        if (bodyShape == BodyShape.STREAM) {
            assertThat(result.headers().firstValue("Content-Length")).contains(Integer.toString(encoded.length));
            assertThat(result.body().contentLength()).hasValue(encoded.length);
            assertThat(result.headers().firstValue("x-amz-decoded-content-length"))
                .contains(Integer.toString(bytes.length));
        } else {
            assertThat(result.headers().contains("Content-Length")).isFalse();
            assertThat(result.body().contentLength()).isEmpty();
            assertThat(result.headers().contains("x-amz-decoded-content-length")).isFalse();
        }
    }

    @Label("Property 46 (response checksum): correct integrity data is accepted and corrupted data is never returned")
    @Property(tries = 1_000)
    void responseChecksumAcceptsCorrectAndNeverReturnsCorruptedIntegrity(
        @ForAll("payloads") Payload payload,
        @ForAll("modeledResponseAlgorithms") List<String> modeled,
        @ForAll("headerSubsets") Set<String> present,
        @ForAll("compositeSubsets") Set<String> composite,
        @ForAll Corruption corruption,
        @ForAll @IntRange(min = 0, max = 1 << 20) int corruptionIndex,
        @ForAll @IntRange(min = 1, max = 255) int corruptionMask,
        @ForAll boolean streamingModel,
        @ForAll BodyShape bodyShape,
        @ForAll("mostlyTrue") boolean enabled,
        @ForAll("statusCodes") int status,
        @ForAll("shapings") Shaping shaping) {
        byte[] bytes = payload.bytes();
        HttpChecksumMetadata.Builder metadata = HttpChecksumMetadata.builder().requestValidationModeMember("ChecksumMode");
        modeled.forEach(metadata::addResponseAlgorithm);
        ResponseChecksumFeature feature =
            ResponseChecksumFeature.forOperation(OPERATION_SCHEMA, MODE_INPUT,
                                                 streamingModel ? STREAMING_OUTPUT : OUTPUT, metadata.build())
                                   .orElseThrow();

        // Oracle selection: the first modeled, supported algorithm with a present, non-composite header.
        String selected = null;
        boolean sawComposite = false;
        for (String id : modeled) {
            if (FLEXIBLE.contains(id) && present.contains(id)) {
                if (composite.contains(id)) {
                    sawComposite = true;
                    continue;
                }
                selected = id;
                break;
            }
        }

        // Every header other than the selected one is a decoy that must not be validated: its value is wrong for
        // the payload unless it is composite (then it is skipped).
        Map<String, String> headers = new LinkedHashMap<>();
        for (String id : ALL_IDS) {
            if (!present.contains(id)) {
                continue;
            }
            String value;
            if (FLEXIBLE.contains(id) && composite.contains(id) && modeled.contains(id)) {
                value = oracleBase64(id, bytes) + "-" + (2 + id.length());
            } else if (id.equals(selected)) {
                value = oracleBase64(id, bytes);
            } else {
                value = wrongBase64(id, bytes);
            }
            headers.put(headerName(id), value);
        }

        byte[] received = bytes;
        Corruption effective = corruption;
        if (corruption == Corruption.PAYLOAD_BYTE && bytes.length > 0) {
            received = bytes.clone();
            received[corruptionIndex % received.length] ^= (byte) corruptionMask;
        } else if (corruption == Corruption.CHECKSUM_BYTE && selected != null) {
            byte[] digest = oracleDigest(selected, bytes);
            digest[corruptionIndex % digest.length] ^= (byte) corruptionMask;
            headers.put(headerName(selected), Base64.getEncoder().encodeToString(digest));
        } else {
            effective = Corruption.NONE;
        }

        HttpBody body = responseBody(received, bodyShape, shaping);
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(status).body(body);
        headers.forEach(builder::addHeader);
        SdkHttpResponse response = builder.build();

        boolean success = status >= 200 && status < 300;
        boolean streamingValidation = streamingModel && bodyShape != BodyShape.BYTES;
        Statistics.label("outcome").collect(!enabled ? "disabled" : !success ? "error status"
            : selected == null ? (sawComposite ? "composite skipped" : "not found")
            : (effective == Corruption.NONE ? "valid " : "corrupted ") + (streamingValidation ? "stream" : "buffered"));

        if (!enabled || !success || selected == null) {
            ValidatedResponse validated = feature.validate(response, enabled);
            Status expectedStatus = !enabled ? Status.NOT_ENABLED
                : !success ? Status.NOT_SUCCESS_RESPONSE
                : sawComposite ? Status.COMPOSITE_CHECKSUM_SKIPPED : Status.CHECKSUM_NOT_FOUND;
            assertThat(validated.result()).isEqualTo(ChecksumValidationResult.of(expectedStatus));
            assertThat(validated.response()).isSameAs(response);
            assertThat(readAllViews(validated.response().body(), shaping)).isEqualTo(received);
            return;
        }

        ChecksumAlgorithm algorithm = ChecksumAlgorithm.valueOf(selected);
        String sent = headers.get(headerName(selected));
        String computed = oracleBase64(selected, received);
        if (!streamingValidation) {
            if (effective == Corruption.NONE) {
                ValidatedResponse validated = feature.validate(response, true);
                assertThat(validated.result()).isEqualTo(ChecksumValidationResult.of(Status.VALIDATED, algorithm));
                assertThat(validated.response().body().kind()).isNotEqualTo(HttpBody.Kind.STREAMING);
                assertThat(validated.response().body().asByteArray()).isEqualTo(bytes);
            } else {
                assertThatThrownBy(() -> feature.validate(response, true))
                    .isInstanceOfSatisfying(ChecksumMismatchException.class,
                                            e -> assertMismatch(e, algorithm, sent, computed));
            }
            return;
        }

        ValidatedResponse validated = feature.validate(response, true);
        assertThat(validated.result()).isEqualTo(ChecksumValidationResult.of(Status.VALIDATING_STREAM, algorithm));
        HttpBody validatingBody = validated.response().body();
        assertThat(validatingBody.contentLength()).isEqualTo(body.contentLength());
        if (effective == Corruption.NONE) {
            assertThat(readSync(validatingBody.streamingBody(), shaping.seed())).isEqualTo(bytes);
            assertThat(readAsync(validatingBody.streamingBody(), shaping.seed())).isEqualTo(bytes);
        } else {
            assertThatThrownBy(() -> readSync(validatingBody.streamingBody(), shaping.seed()))
                .isInstanceOfSatisfying(ChecksumMismatchException.class,
                                        e -> assertMismatch(e, algorithm, sent, computed));
            assertThatThrownBy(() -> readAsync(validatingBody.streamingBody(), shaping.seed()))
                .isInstanceOfSatisfying(ChecksumMismatchException.class,
                                        e -> assertMismatch(e, algorithm, sent, computed));
        }
    }

    @Label("Property 46 (request compression): the exact modeled algorithm, threshold and protocol fields are applied")
    @Property(tries = 1_000)
    void requestCompressionAppliesExactAlgorithmThresholdAndFields(
        @ForAll("encodingLists") List<String> encodings,
        @ForAll("compressionCases") CompressionCase compressionCase,
        @ForAll boolean enabled,
        @ForAll boolean streamingModel,
        @ForAll BodyShape bodyShape,
        @ForAll("callerEncodings") Optional<String> callerEncoding,
        @ForAll("shapings") Shaping shaping) {
        Schema operation = Schema.createOperation(RequestChecksumFeatureTest.OPERATION,
                                                  RequestCompressionTrait.builder().encodings(encodings).build());
        Schema input = streamingModel ? STREAMING_INPUT : INPUT;
        Optional<RequestCompressionFeature> planned = RequestCompressionFeature.forOperation(operation, input);
        boolean gzipModeled = encodings.stream().anyMatch(e -> e.equalsIgnoreCase("gzip"));
        assertThat(planned.isPresent()).isEqualTo(gzipModeled);
        if (!gzipModeled) {
            Statistics.label("compression").collect("no supported encoding");
            return;
        }
        assertThat(planned.get().encoding()).isEqualTo("gzip");

        byte[] bytes = compressionCase.payload().bytes();
        int threshold = compressionCase.threshold();
        SdkHttpRequest.Builder builder = requestWithBody(bytes, bodyShape, shaping).toBuilder();
        callerEncoding.ifPresent(coding -> builder.putHeader("Content-Encoding", coding));
        SdkHttpRequest request = builder.build();

        SdkHttpRequest result = planned.get().apply(request, new CompressionSettings(enabled, threshold));

        boolean empty = bodyShape == BodyShape.BYTES && bytes.length == 0;
        boolean streaming = bodyShape != BodyShape.BYTES;
        boolean compress = enabled && !empty
                           && (streaming && (streamingModel || bodyShape == BodyShape.STREAM_UNKNOWN_LENGTH)
                               || bytes.length >= threshold);
        Statistics.label("compression").collect(!compress ? "unchanged"
            : streaming ? "stream compressed" : "buffered compressed");
        Statistics.label("threshold").collect(bytes.length == threshold ? "at" : bytes.length < threshold ? "below"
            : "above");
        if (!compress) {
            assertThat(result).isSameAs(request);
            return;
        }
        byte[] compressed = readAllViews(result.body(), shaping);
        assertExactGzip(compressed, bytes);
        assertThat(result.headers().allValues("Content-Encoding"))
            .containsExactly(callerEncoding.map(coding -> coding + ", gzip").orElse("gzip"));
        if (streaming) {
            assertThat(result.headers().contains("Content-Length")).isFalse();
            assertThat(result.body().contentLength()).isEmpty();
        } else {
            assertThat(result.headers().firstValue("Content-Length")).contains(Integer.toString(compressed.length));
            assertThat(result.body().contentLength()).hasValue(compressed.length);
        }
    }

    @Label("Property 46 (compression then checksum): the checksum covers exactly the transmitted compressed bytes")
    @Property(tries = 500)
    void requestChecksumCoversCompressedPayload(@ForAll("nonEmptyPayloads") Payload payload,
                                                @ForAll boolean streamingModel,
                                                @ForAll BodyShape bodyShape,
                                                @ForAll("algorithms") String algorithm,
                                                @ForAll PayloadSigning signing,
                                                @ForAll("chunkSizes") OptionalInt chunkSize,
                                                @ForAll("shapings") Shaping shaping) {
        byte[] bytes = payload.bytes();
        Schema operation = Schema.createOperation(RequestChecksumFeatureTest.OPERATION,
                                                  RequestCompressionTrait.builder().encodings(List.of("gzip"))
                                                                         .build());
        Schema input = streamingModel ? STREAMING_INPUT : INPUT;
        HttpChecksumMetadata metadata = HttpChecksumMetadata.builder().requestAlgorithmMember("ChecksumAlgorithm")
                                                            .requestChecksumRequired(true).build();
        RequestChecksumFeature checksums = chunkSize.isPresent()
            ? RequestChecksumFeature.forOperation(operation, input, metadata, chunkSize.getAsInt()).orElseThrow()
            : RequestChecksumFeature.forOperation(operation, input, metadata).orElseThrow();
        SdkHttpRequest request = requestWithBody(bytes, bodyShape, shaping);

        SdkHttpRequest compressed = RequestCompressionFeature.forOperation(operation, input).orElseThrow()
                                                             .apply(request, new CompressionSettings(true, 0));
        byte[] gzip = readSync(compressed.body(), shaping.seed());
        assertExactGzip(gzip, bytes);
        SdkHttpRequest result = checksums.apply(new MapStruct(input).with("ChecksumAlgorithm", algorithm), compressed,
                                                RequestChecksumCalculation.WHEN_REQUIRED, signing);

        String name = headerName(algorithm);
        String expected = oracleBase64(algorithm, gzip);
        boolean trailer = streamingModel && bodyShape != BodyShape.BYTES && signing != PayloadSigning.SIGNED;
        if (!trailer) {
            Statistics.label("location").collect("header");
            assertThat(result.headers().firstValue(name)).contains(expected);
            assertThat(result.headers().allValues("Content-Encoding")).containsExactly("gzip");
            assertThat(readAllViews(result.body(), shaping)).isEqualTo(gzip);
        } else if (signing == PayloadSigning.STREAMING_SIGNED) {
            Statistics.label("location").collect("declared trailer");
            assertThat(result.headers().firstValue("x-amz-trailer")).contains(name);
            assertThat(result.body()).isSameAs(compressed.body());
        } else {
            Statistics.label("location").collect("unsigned trailer");
            Chunked chunked = decodeAwsChunked(readAllViews(result.body(), shaping));
            assertThat(chunked.data()).isEqualTo(gzip);
            assertExactFrames(chunked.frames(), gzip.length, chunkSize.orElse(DEFAULT_CHUNK_SIZE));
            assertThat(chunked.trailers()).isEqualTo(Map.of(name, expected));
            assertThat(result.headers().allValues("Content-Encoding")).containsExactly("gzip, aws-chunked");
            // A compressed stream has no length until sent, so neither length field can be exact and neither is sent.
            assertThat(result.headers().contains("Content-Length")).isFalse();
            assertThat(result.headers().contains("x-amz-decoded-content-length")).isFalse();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary<Payload> payloads() {
        return payloadsOfSize(Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.integers().between(0, 64)),
            Tuple.of(4, Arbitraries.integers().between(65, 4096)),
            Tuple.of(1, Arbitraries.integers().between(60_000, 70_000))));
    }

    @Provide
    Arbitrary<Payload> nonEmptyPayloads() {
        return payloadsOfSize(Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.integers().between(1, 64)),
            Tuple.of(4, Arbitraries.integers().between(65, 4096)),
            Tuple.of(1, Arbitraries.integers().between(60_000, 70_000))));
    }

    private static Arbitrary<Payload> payloadsOfSize(Arbitrary<Integer> sizes) {
        return Combinators.combine(sizes, Arbitraries.longs(), Arbitraries.of(Entropy.class)).as(Payload::of);
    }

    @Provide
    Arbitrary<CompressionCase> compressionCases() {
        Arbitrary<Integer> thresholds = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(0)),
            Tuple.of(1, Arbitraries.just(1)),
            Tuple.of(2, Arbitraries.just(CompressionSettings.DEFAULT_MIN_COMPRESSION_SIZE_BYTES)),
            Tuple.of(1, Arbitraries.just(CompressionSettings.MAX_MIN_COMPRESSION_SIZE_BYTES)),
            Tuple.of(3, Arbitraries.integers().between(0, 5_000)));
        return thresholds.flatMap(threshold -> {
            // Sizes at and around the threshold; payloads stay under 70 KB, so a larger threshold (the maximum) is
            // approached from below only.
            int base = Math.min(threshold, 70_000);
            Arbitrary<Integer> nearThreshold = Arbitraries.integers().between(-2, 2)
                                                          .map(delta -> Math.max(0, base + delta));
            Arbitrary<Integer> sizes = Arbitraries.frequencyOf(
                Tuple.of(3, nearThreshold),
                Tuple.of(2, Arbitraries.integers().between(0, 12_000)));
            return payloadsOfSize(sizes).map(payload -> new CompressionCase(threshold, payload));
        });
    }

    @Provide
    Arbitrary<Optional<String>> selectedAlgorithms() {
        return Arbitraries.of(FLEXIBLE).optional(0.6);
    }

    @Provide
    Arbitrary<String> algorithms() {
        return Arbitraries.of(FLEXIBLE);
    }

    @Provide
    Arbitrary<List<String>> modeledResponseAlgorithms() {
        return Arbitraries.of(ALL_IDS).list().uniqueElements().ofMinSize(1).ofMaxSize(6);
    }

    /** Present checksum headers: mostly several supported ones, so a modeled one is usually found, plus decoys. */
    @Provide
    Arbitrary<Set<String>> headerSubsets() {
        List<String> unsupported = ALL_IDS.stream().filter(id -> !FLEXIBLE.contains(id)).toList();
        Arbitrary<Set<String>> supported = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.subsetOf(FLEXIBLE)),
            Tuple.of(3, Arbitraries.subsetOf(FLEXIBLE).ofMinSize(3)));
        return Combinators.combine(supported, Arbitraries.subsetOf(unsupported)).as((a, b) -> {
            Set<String> all = new HashSet<>(a);
            all.addAll(b);
            return all;
        });
    }

    @Provide
    Arbitrary<ModelShape> modelShapes() {
        return Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(ModelShape.LEGACY_REQUIRED)),
                                       Tuple.of(2, Arbitraries.just(ModelShape.FLEXIBLE_OPTIONAL)),
                                       Tuple.of(2, Arbitraries.just(ModelShape.FLEXIBLE_REQUIRED)));
    }

    @Provide
    Arbitrary<Boolean> mostlyTrue() {
        return Arbitraries.frequencyOf(Tuple.of(4, Arbitraries.just(true)), Tuple.of(1, Arbitraries.just(false)));
    }

    @Provide
    Arbitrary<Set<String>> compositeSubsets() {
        return Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.just(Set.<String>of())),
                                       Tuple.of(1, Arbitraries.subsetOf(FLEXIBLE)));
    }

    @Provide
    Arbitrary<Integer> statusCodes() {
        return Arbitraries.frequencyOf(Tuple.of(12, Arbitraries.just(200)), Tuple.of(1, Arbitraries.just(206)),
                                       Tuple.of(1, Arbitraries.just(404)), Tuple.of(1, Arbitraries.just(503)));
    }

    @Provide
    Arbitrary<OptionalInt> chunkSizes() {
        return Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(OptionalInt.empty())),
                                       Tuple.of(4, Arbitraries.integers().between(1, 64).map(OptionalInt::of)),
                                       Tuple.of(1, Arbitraries.integers().between(65, 5_000).map(OptionalInt::of)));
    }

    @Provide
    Arbitrary<Shaping> shapings() {
        Arbitrary<Integer> maxChunks = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.integers().between(1, 16)),
            Tuple.of(3, Arbitraries.integers().between(17, 8_192)),
            Tuple.of(1, Arbitraries.just(StreamingBodies.DEFAULT_MAX_CHUNK_SIZE)));
        return Combinators.combine(Arbitraries.longs(), maxChunks).as(Shaping::new);
    }

    @Provide
    Arbitrary<List<String>> encodingLists() {
        return Arbitraries.of("gzip", "GZIP", "br", "zstd", "compress").list().uniqueElements().ofMinSize(1)
                          .ofMaxSize(4);
    }

    @Provide
    Arbitrary<Optional<String>> callerEncodings() {
        return Arbitraries.of("br", "deflate, br").optional(0.3);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------------------------------------------

    private static RequestChecksumFeature requestFeature(ModelShape model, Schema input, OptionalInt chunkSize) {
        Schema operation = model == ModelShape.LEGACY_REQUIRED ? LEGACY_OPERATION_SCHEMA : OPERATION_SCHEMA;
        HttpChecksumMetadata metadata = switch (model) {
            case FLEXIBLE_OPTIONAL -> HttpChecksumMetadata.builder().requestAlgorithmMember("ChecksumAlgorithm").build();
            case FLEXIBLE_REQUIRED -> HttpChecksumMetadata.builder().requestAlgorithmMember("ChecksumAlgorithm")
                                                          .requestChecksumRequired(true).build();
            case LEGACY_REQUIRED -> null;
        };
        return (chunkSize.isPresent()
                ? RequestChecksumFeature.forOperation(operation, input, metadata, chunkSize.getAsInt())
                : RequestChecksumFeature.forOperation(operation, input, metadata)).orElseThrow();
    }

    /** A serialized request: a buffered body, or a replayable stream with its Content-Length when known. */
    private static SdkHttpRequest requestWithBody(byte[] bytes, BodyShape shape, Shaping shaping) {
        SdkHttpRequest.Builder builder = TestSupport.request();
        if (shape == BodyShape.BYTES) {
            return builder.body(HttpBody.fromBytes(bytes)).build();
        }
        boolean known = shape == BodyShape.STREAM;
        if (known) {
            builder.putHeader("Content-Length", Integer.toString(bytes.length));
        }
        return builder.body(HttpBody.streaming(shapedSource(bytes, known, shaping))).build();
    }

    private static HttpBody responseBody(byte[] bytes, BodyShape shape, Shaping shaping) {
        return shape == BodyShape.BYTES ? HttpBody.fromBytes(bytes)
                                        : HttpBody.streaming(shapedSource(bytes, shape == BodyShape.STREAM, shaping));
    }

    /**
     * A replayable streaming body whose blocking view returns random short reads and whose demand-driven view emits
     * random-sized chunks, both determined by the shaping seed.
     */
    private static StreamingBody shapedSource(byte[] bytes, boolean knownLength, Shaping shaping) {
        return StreamingBodies.of(knownLength ? bytes.length : -1, null, true,
                                  () -> new ShortReadInputStream(bytes, new Random(shaping.seed())),
                                  new RandomChunkPublisher(bytes, shaping.seed(), shaping.maxChunk()),
                                  shaping.maxChunk());
    }

    /** The bytes of {@code body}; a stream is read through both views, which must agree exactly. */
    private static byte[] readAllViews(HttpBody body, Shaping shaping) {
        if (body.kind() != HttpBody.Kind.STREAMING) {
            return body.asByteArray();
        }
        byte[] sync = readSync(body.streamingBody(), shaping.seed());
        byte[] async = readAsync(body.streamingBody(), ~shaping.seed());
        assertThat(async).as("the demand-driven view yields the blocking view's bytes").isEqualTo(sync);
        return sync;
    }

    private static byte[] readSync(HttpBody body, long seed) {
        return body.kind() == HttpBody.Kind.STREAMING ? readSync(body.streamingBody(), seed) : body.asByteArray();
    }

    /** Reads the blocking view with a random mix of single-byte and random-length bulk reads. */
    private static byte[] readSync(StreamingBody body, long seed) {
        Random random = new Random(seed * 31 + 7);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8_192];
        try (InputStream in = body.openStream()) {
            while (true) {
                if (random.nextInt(8) == 0) {
                    int b = in.read();
                    if (b < 0) {
                        break;
                    }
                    out.write(b);
                } else {
                    int offset = random.nextInt(16);
                    int n = in.read(buffer, offset, 1 + random.nextInt(buffer.length - offset));
                    if (n < 0) {
                        break;
                    }
                    out.write(buffer, offset, n);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** Reads the demand-driven view with random demand; fails with the stream's error. */
    private static byte[] readAsync(Publisher<ByteBuffer> publisher, long seed) {
        CompletableFuture<byte[]> result = new CompletableFuture<>();
        AtomicInteger terminals = new AtomicInteger();
        Random random = new Random(seed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        publisher.subscribe(new Subscriber<>() {
            private Subscription subscription;
            private long outstanding;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                outstanding = 1 + random.nextInt(4);
                s.request(outstanding);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                if (terminals.get() > 0 || outstanding <= 0) {
                    result.completeExceptionally(new AssertionError("onNext after a terminal signal or beyond demand"));
                    return;
                }
                byte[] bytes = new byte[chunk.remaining()];
                chunk.get(bytes);
                out.writeBytes(bytes);
                outstanding--;
                if (outstanding == 0 || random.nextInt(4) == 0) {
                    long more = 1 + random.nextInt(4);
                    outstanding += more;
                    subscription.request(more);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (terminals.incrementAndGet() == 1) {
                    result.completeExceptionally(t);
                }
            }

            @Override
            public void onComplete() {
                if (terminals.incrementAndGet() == 1) {
                    result.complete(out.toByteArray());
                }
            }
        });
        try {
            byte[] bytes = result.get(30, TimeUnit.SECONDS);
            assertThat(terminals.get()).as("exactly one terminal signal").isEqualTo(1);
            return bytes;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A blocking source returning random short reads. */
    private static final class ShortReadInputStream extends InputStream {
        private final byte[] bytes;
        private final Random random;
        private int position;

        ShortReadInputStream(byte[] bytes, Random random) {
            this.bytes = bytes;
            this.random = random;
        }

        @Override
        public int read() {
            return position < bytes.length ? bytes[position++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] target, int offset, int length) {
            if (length == 0) {
                return 0;
            }
            if (position >= bytes.length) {
                return -1;
            }
            int n = Math.min(bytes.length - position, 1 + random.nextInt(length));
            System.arraycopy(bytes, position, target, offset, n);
            position += n;
            return n;
        }
    }

    /** A demand-driven source emitting random-sized read-only chunks, trampolined so re-entrant requests are safe. */
    private static final class RandomChunkPublisher implements Publisher<ByteBuffer> {
        private final byte[] bytes;
        private final long seed;
        private final int maxChunk;

        RandomChunkPublisher(byte[] bytes, long seed, int maxChunk) {
            this.bytes = bytes;
            this.seed = seed;
            this.maxChunk = maxChunk;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            Random random = new Random(seed ^ 0x5DEECE66DL);
            AtomicLong demand = new AtomicLong();
            AtomicInteger wip = new AtomicInteger();
            subscriber.onSubscribe(new Subscription() {
                private int position;
                private volatile boolean done;

                @Override
                public void request(long n) {
                    if (n <= 0) {
                        done = true;
                        subscriber.onError(new IllegalArgumentException("Rule 3.9: non-positive request " + n));
                        return;
                    }
                    demand.getAndAccumulate(n, (a, b) -> a + b < 0 ? Long.MAX_VALUE : a + b);
                    if (wip.getAndIncrement() != 0) {
                        return;
                    }
                    int missed = 1;
                    do {
                        while (!done && position < bytes.length && demand.get() > 0) {
                            int size = 1 + random.nextInt(Math.min(maxChunk, bytes.length - position));
                            ByteBuffer chunk = ByteBuffer.wrap(bytes, position, size).slice().asReadOnlyBuffer();
                            position += size;
                            demand.decrementAndGet();
                            subscriber.onNext(chunk);
                        }
                        if (!done && position >= bytes.length) {
                            done = true;
                            subscriber.onComplete();
                        }
                        missed = wip.addAndGet(-missed);
                    } while (missed != 0);
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Independent oracles
    // ---------------------------------------------------------------------------------------------------------------

    /** The checksum header and trailer name of an algorithm ID. */
    private static String headerName(String algorithmId) {
        return "x-amz-checksum-" + algorithmId.toLowerCase(Locale.ROOT);
    }

    private static String oracleBase64(String algorithmId, byte[] data) {
        return Base64.getEncoder().encodeToString(oracleDigest(algorithmId, data));
    }

    /** A value that is wrong for {@code data}: the correct digest with one bit flipped, or zeros if not computable. */
    private static String wrongBase64(String algorithmId, byte[] data) {
        if (algorithmId.startsWith("XXHASH")) {
            return Base64.getEncoder().encodeToString(new byte[algorithmId.equals("XXHASH128") ? 16 : 8]);
        }
        byte[] digest = oracleDigest(algorithmId, data);
        digest[0] ^= 0x01;
        return Base64.getEncoder().encodeToString(digest);
    }

    /** The big-endian digest bytes of {@code data}, computed without the runtime under test. */
    private static byte[] oracleDigest(String algorithmId, byte[] data) {
        switch (algorithmId) {
            case "CRC32" -> {
                CRC32 crc = new CRC32();
                crc.update(data);
                return ByteBuffer.allocate(4).putInt((int) crc.getValue()).array();
            }
            case "CRC32C" -> {
                CRC32C crc = new CRC32C();
                crc.update(data);
                return ByteBuffer.allocate(4).putInt((int) crc.getValue()).array();
            }
            case "CRC64NVME" -> {
                return ByteBuffer.allocate(8).putLong(crc64Nvme(data)).array();
            }
            case "SHA1" -> {
                return messageDigest("SHA-1", data);
            }
            case "SHA256" -> {
                return messageDigest("SHA-256", data);
            }
            case "SHA512" -> {
                return messageDigest("SHA-512", data);
            }
            case "MD5" -> {
                return messageDigest("MD5", data);
            }
            default -> throw new IllegalArgumentException("No oracle for " + algorithmId);
        }
    }

    private static byte[] messageDigest(String name, byte[] data) {
        try {
            return MessageDigest.getInstance(name).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Bitwise CRC-64/NVME: reflected, initial value and final XOR all ones. */
    private static long crc64Nvme(byte[] data) {
        long crc = -1L;
        for (byte b : data) {
            crc ^= b & 0xFFL;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >>> 1) ^ CRC64_NVME_REFLECTED_POLY : crc >>> 1;
            }
        }
        return ~crc;
    }

    private static void assertMismatch(ChecksumMismatchException e, ChecksumAlgorithm algorithm, String sent,
                                       String computed) {
        assertThat(e.algorithm()).isEqualTo(algorithm);
        assertThat(e.expected()).isEqualTo(sent);
        assertThat(e.computed()).isEqualTo(computed);
    }

    /** Every frame carries exactly {@code chunkSize} bytes except a shorter, non-empty last one. */
    private static void assertExactFrames(List<Integer> frames, int length, int chunkSize) {
        List<Integer> expected = new ArrayList<>();
        for (int remaining = length; remaining > 0; remaining -= chunkSize) {
            expected.add(Math.min(remaining, chunkSize));
        }
        assertThat(frames).isEqualTo(expected);
    }

    /** One RFC 1952 member that decompresses to {@code payload}, with its CRC-32 and ISIZE trailer. */
    private static void assertExactGzip(byte[] gzip, byte[] payload) {
        assertThat(gzip.length).isGreaterThanOrEqualTo(18);
        assertThat(gzip[0]).isEqualTo((byte) 0x1f);
        assertThat(gzip[1]).isEqualTo((byte) 0x8b);
        assertThat(gzip[2]).as("deflate").isEqualTo((byte) 8);
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gzip))) {
            assertThat(in.readAllBytes()).isEqualTo(payload);
        } catch (IOException e) {
            fail("not a valid gzip stream", e);
        }
        CRC32 crc = new CRC32();
        crc.update(payload);
        ByteBuffer trailer = ByteBuffer.wrap(gzip, gzip.length - 8, 8).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(trailer.getInt() & 0xFFFFFFFFL).isEqualTo(crc.getValue());
        assertThat(trailer.getInt() & 0xFFFFFFFFL).isEqualTo(payload.length & 0xFFFFFFFFL);
    }

    /** A decoded {@code aws-chunked} body: the data length of each frame, the data, and the trailers in order. */
    record Chunked(List<Integer> frames, byte[] data, Map<String, String> trailers) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Chunked other && frames.equals(other.frames)
                   && Arrays.equals(data, other.data) && trailers.equals(other.trailers);
        }

        @Override
        public int hashCode() {
            return frames.hashCode() * 31 + Arrays.hashCode(data);
        }
    }

    /**
     * Parses {@code hex(size) CRLF data CRLF}... {@code 0 CRLF} {@code name:value CRLF}... {@code CRLF}, failing on
     * anything else, including trailing bytes.
     */
    static Chunked decodeAwsChunked(byte[] encoded) {
        int[] position = {0};
        List<Integer> frames = new ArrayList<>();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (true) {
            String sizeLine = line(encoded, position);
            assertThat(sizeLine).as("chunk size line").matches("[0-9a-fA-F]{1,8}");
            int size = Integer.parseInt(sizeLine, 16);
            if (size == 0) {
                break;
            }
            assertThat(position[0] + size + 2).as("frame fits").isLessThanOrEqualTo(encoded.length);
            data.write(encoded, position[0], size);
            position[0] += size;
            assertThat(line(encoded, position)).as("CRLF after frame data").isEmpty();
            frames.add(size);
        }
        Map<String, String> trailers = new LinkedHashMap<>();
        while (true) {
            String trailer = line(encoded, position);
            if (trailer.isEmpty()) {
                break;
            }
            int colon = trailer.indexOf(':');
            assertThat(colon).as("trailer '%s' has a name", trailer).isPositive();
            assertThat(trailers.put(trailer.substring(0, colon), trailer.substring(colon + 1))).isNull();
        }
        assertThat(position[0]).as("no bytes after the final CRLF").isEqualTo(encoded.length);
        return new Chunked(frames, data.toByteArray(), trailers);
    }

    private static String line(byte[] encoded, int[] position) {
        for (int i = position[0]; i + 1 < encoded.length; i++) {
            if (encoded[i] == '\r' && encoded[i + 1] == '\n') {
                String line = new String(encoded, position[0], i - position[0], StandardCharsets.ISO_8859_1);
                position[0] = i + 2;
                return line;
            }
        }
        throw new AssertionError("missing CRLF at offset " + position[0]);
    }
}
