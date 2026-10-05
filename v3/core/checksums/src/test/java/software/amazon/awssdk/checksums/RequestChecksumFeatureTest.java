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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.checksums.ResolvedRequestChecksum.Location;
import software.amazon.awssdk.checksums.TestSupport.MapStruct;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

class RequestChecksumFeatureTest {
    static final ShapeId OPERATION = ShapeId.from("com.example#PutThing");
    static final Schema STRING = Schema.createString(ShapeId.from("smithy.api#String"));
    static final Schema BLOB = Schema.createBlob(ShapeId.from("smithy.api#Blob"));
    static final Schema STREAM = Schema.createBlob(ShapeId.from("com.example#Stream"), new StreamingTrait());
    static final String ALGORITHM_HEADER = "x-amz-sdk-checksum-algorithm";

    static final Schema INPUT = Schema.structureBuilder(ShapeId.from("com.example#PutThingInput"))
                                      .putMember("ChecksumAlgorithm", STRING, new HttpHeaderTrait(ALGORITHM_HEADER))
                                      .putMember("Body", BLOB, new HttpPayloadTrait())
                                      .build();
    static final Schema STREAMING_INPUT = Schema.structureBuilder(ShapeId.from("com.example#PutStreamInput"))
                                                .putMember("ChecksumAlgorithm", STRING,
                                                           new HttpHeaderTrait(ALGORITHM_HEADER))
                                                .putMember("Body", STREAM, new HttpPayloadTrait())
                                                .build();
    static final Schema OPERATION_SCHEMA = Schema.createOperation(OPERATION);

    private static final HttpChecksumMetadata OPTIONAL = HttpChecksumMetadata.builder()
                                                                             .requestAlgorithmMember("ChecksumAlgorithm")
                                                                             .build();
    private static final HttpChecksumMetadata REQUIRED = HttpChecksumMetadata.builder()
                                                                             .requestAlgorithmMember("ChecksumAlgorithm")
                                                                             .requestChecksumRequired(true)
                                                                             .build();
    private static final byte[] PAYLOAD = "hello checksums".getBytes(StandardCharsets.UTF_8);

    @Test
    void forOperation_whenNoRequestBehavior_isEmpty() {
        HttpChecksumMetadata responseOnly = HttpChecksumMetadata.builder()
                                                                .requestValidationModeMember("ChecksumAlgorithm")
                                                                .addResponseAlgorithm("CRC32")
                                                                .build();
        assertThat(RequestChecksumFeature.forOperation(OPERATION_SCHEMA, INPUT, null)).isEmpty();
        assertThat(RequestChecksumFeature.forOperation(OPERATION_SCHEMA, INPUT, responseOnly)).isEmpty();
    }

    @Test
    void forOperation_whenAlgorithmMemberMissing_rejectsWithOperationAndSetting() {
        HttpChecksumMetadata metadata = HttpChecksumMetadata.builder().requestAlgorithmMember("Nope").build();
        assertThatThrownBy(() -> RequestChecksumFeature.forOperation(OPERATION_SCHEMA, INPUT, metadata))
            .isInstanceOfSatisfying(ModelConflictException.class, e -> {
                assertThat(e.operation()).isEqualTo(OPERATION);
                assertThat(e.setting()).isEqualTo("httpChecksum.requestAlgorithmMember");
                assertThat(e.retryable()).isFalse();
            })
            .hasMessageContaining("Nope");
    }

    @Test
    void apply_whenWhenSupportedAndUnset_addsDefaultCrc32HeaderAndImplicitAlgorithm() {
        RequestChecksumFeature feature = feature(INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request().body(HttpBody.fromBytes(PAYLOAD)).build();

        SdkHttpRequest result = feature.apply(new MapStruct(INPUT), request, RequestChecksumCalculation.WHEN_SUPPORTED,
                                              PayloadSigning.SIGNED);

        assertThat(result.headers().firstValue("x-amz-checksum-crc32")).contains(crc32(PAYLOAD));
        assertThat(result.headers().firstValue(ALGORITHM_HEADER)).contains("CRC32");
        assertThat(result.body()).isEqualTo(request.body());
    }

    @Test
    void apply_whenWhenRequiredAndOptional_sendsNothing() {
        RequestChecksumFeature feature = feature(INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request().body(HttpBody.fromBytes(PAYLOAD)).build();

        assertThat(feature.apply(new MapStruct(INPUT), request, RequestChecksumCalculation.WHEN_REQUIRED,
                                 PayloadSigning.SIGNED)).isSameAs(request);
    }

    @Test
    void apply_whenWhenRequiredAndRequired_sendsDefault() {
        RequestChecksumFeature feature = feature(INPUT, REQUIRED);
        SdkHttpRequest request = TestSupport.request().body(HttpBody.fromBytes(PAYLOAD)).build();

        SdkHttpRequest result = feature.apply(new MapStruct(INPUT), request, RequestChecksumCalculation.WHEN_REQUIRED,
                                              PayloadSigning.UNSIGNED);

        assertThat(result.headers().firstValue("x-amz-checksum-crc32")).contains(crc32(PAYLOAD));
    }

    @Test
    void apply_whenCallerSelectsAlgorithm_usesItWithoutImplicitHeader() {
        RequestChecksumFeature feature = feature(INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request().body(HttpBody.fromBytes(PAYLOAD)).build();

        SdkHttpRequest result = feature.apply(new MapStruct(INPUT).with("ChecksumAlgorithm", "SHA256"), request,
                                              RequestChecksumCalculation.WHEN_REQUIRED, PayloadSigning.SIGNED);

        assertThat(result.headers().firstValue("x-amz-checksum-sha256"))
            .contains(SdkChecksum.base64(ChecksumAlgorithm.SHA256, PAYLOAD));
        assertThat(result.headers().contains(ALGORITHM_HEADER)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MD5", "XXHASH64", "CRC16"})
    void apply_whenCallerSelectsUnsupportedAlgorithm_blocksWithOperationAndMember(String algorithm) {
        RequestChecksumFeature feature = feature(INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request().body(HttpBody.fromBytes(PAYLOAD)).build();

        assertThatThrownBy(() -> feature.apply(new MapStruct(INPUT).with("ChecksumAlgorithm", algorithm), request,
                                               RequestChecksumCalculation.WHEN_SUPPORTED, PayloadSigning.SIGNED))
            .isInstanceOfSatisfying(ModelConflictException.class, e -> {
                assertThat(e.operation()).isEqualTo(OPERATION);
                assertThat(e.setting()).isEqualTo("ChecksumAlgorithm");
            })
            .hasMessageContaining(OPERATION.toString());
    }

    @Test
    void apply_whenChecksumHeaderSuppliedExplicitly_skipsEvenForUnsupportedAlgorithm() {
        RequestChecksumFeature feature = feature(INPUT, REQUIRED);
        SdkHttpRequest request = TestSupport.request().putHeader("x-amz-checksum-xxhash64", "AAAAAAAAAAA=")
                                            .body(HttpBody.fromBytes(PAYLOAD)).build();

        assertThat(feature.apply(new MapStruct(INPUT).with("ChecksumAlgorithm", "XXHASH64"), request,
                                 RequestChecksumCalculation.WHEN_SUPPORTED, PayloadSigning.SIGNED)).isSameAs(request);
    }

    @Test
    void apply_whenLegacyChecksumRequired_addsContentMd5() {
        Schema legacy = Schema.createOperation(OPERATION, new HttpChecksumRequiredTrait());
        RequestChecksumFeature feature = RequestChecksumFeature.forOperation(legacy, INPUT, null).orElseThrow();
        SdkHttpRequest request = TestSupport.request().body(HttpBody.fromBytes(PAYLOAD)).build();

        SdkHttpRequest result = feature.apply(new MapStruct(INPUT), request, RequestChecksumCalculation.WHEN_REQUIRED,
                                              PayloadSigning.SIGNED);

        assertThat(result.headers().firstValue("Content-MD5"))
            .contains(SdkChecksum.base64(ChecksumAlgorithm.MD5, PAYLOAD));
        assertThat(feature.apply(new MapStruct(INPUT), result, RequestChecksumCalculation.WHEN_REQUIRED,
                                 PayloadSigning.SIGNED)).isSameAs(result);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 16, 40})
    void apply_whenStreamingUnsigned_sendsAwsChunkedTrailer(int size) {
        byte[] payload = new byte[size];
        new Random(size).nextBytes(payload);
        RequestChecksumFeature feature = RequestChecksumFeature.forOperation(OPERATION_SCHEMA, STREAMING_INPUT,
                                                                             OPTIONAL, 16).orElseThrow();
        SdkHttpRequest request = TestSupport.request()
                                            .putHeader("Content-Encoding", "gzip")
                                            .putHeader("Content-Length", Integer.toString(size))
                                            .body(HttpBody.streaming(StreamingBodies.fromBytes(payload, null)))
                                            .build();

        SdkHttpRequest result = feature.apply(new MapStruct(STREAMING_INPUT), request,
                                              RequestChecksumCalculation.WHEN_SUPPORTED, PayloadSigning.UNSIGNED);

        byte[] expected = ChunkTransformsTest.expectedChunked(payload);
        assertThat(result.headers().firstValue("x-amz-trailer")).contains("x-amz-checksum-crc32");
        assertThat(result.headers().firstValue("Content-Encoding")).contains("gzip, aws-chunked");
        assertThat(result.headers().firstValue("x-amz-content-sha256")).contains("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(result.headers().firstValue("x-amz-decoded-content-length")).contains(Integer.toString(size));
        assertThat(result.headers().firstValue("Content-Length")).contains(Integer.toString(expected.length));
        assertThat(result.body().contentLength()).hasValue(expected.length);
        assertThat(TestSupport.readStream(result.body())).isEqualTo(expected);
        assertThat(TestSupport.readPublisher(result.body().streamingBody())).isEqualTo(expected);
    }

    @Test
    void apply_whenStreamingSigned_computesHeaderOverReplayableBody() {
        RequestChecksumFeature feature = feature(STREAMING_INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request()
                                            .body(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
                                            .build();

        SdkHttpRequest result = feature.apply(new MapStruct(STREAMING_INPUT), request,
                                              RequestChecksumCalculation.WHEN_SUPPORTED, PayloadSigning.SIGNED);

        assertThat(feature.resolve(new MapStruct(STREAMING_INPUT), request, RequestChecksumCalculation.WHEN_SUPPORTED,
                                   PayloadSigning.SIGNED).orElseThrow().location()).isEqualTo(Location.HEADER);
        assertThat(result.headers().firstValue("x-amz-checksum-crc32")).contains(crc32(PAYLOAD));
        assertThat(TestSupport.readStream(result.body())).isEqualTo(PAYLOAD);
    }

    @Test
    void apply_whenHeaderChecksumOfOneShotStream_failsBeforeConsumingIt() {
        RequestChecksumFeature feature = feature(STREAMING_INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request()
                                            .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                                new ByteArrayInputStream(PAYLOAD), PAYLOAD.length, null)))
                                            .build();

        assertThatThrownBy(() -> feature.apply(new MapStruct(STREAMING_INPUT), request,
                                               RequestChecksumCalculation.WHEN_SUPPORTED, PayloadSigning.SIGNED))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("not replayable");
        assertThat(request.body().streamingBody().isAvailable()).isTrue();
    }

    @Test
    void prepare_whenHeaderChecksumOfStream_leavesTheDigestToTheCallerWithoutReadingTheBody() {
        RequestChecksumFeature feature = feature(STREAMING_INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request()
                                            .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                                new ByteArrayInputStream(PAYLOAD), PAYLOAD.length, null)))
                                            .build();

        RequestChecksumFeature.Prepared prepared =
            feature.prepare(new MapStruct(STREAMING_INPUT), request, RequestChecksumCalculation.WHEN_SUPPORTED,
                            PayloadSigning.SIGNED);

        ResolvedRequestChecksum pending = prepared.pendingHeaderChecksum().orElseThrow();
        assertThat(pending.algorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
        assertThat(pending.location()).isEqualTo(Location.HEADER);
        assertThat(prepared.request().headers().firstValue(ALGORITHM_HEADER)).contains("CRC32");
        assertThat(prepared.request().headers().contains("x-amz-checksum-crc32")).isFalse();
        assertThat(prepared.request().body()).isSameAs(request.body());
        assertThat(request.body().streamingBody().isAvailable()).as("the one-shot body is not read").isTrue();

        SdkChecksum crc = SdkChecksum.create(ChecksumAlgorithm.CRC32);
        crc.update(PAYLOAD, 0, PAYLOAD.length);
        SdkHttpRequest completed = RequestChecksumFeature.withHeaderChecksum(prepared.request(), pending, crc.digest());
        assertThat(completed.headers().firstValue("x-amz-checksum-crc32")).contains(crc32(PAYLOAD));
    }

    @Test
    void prepare_whenNotAStreamingHeaderChecksum_completesTheRequestLikeApply() {
        RequestChecksumFeature feature = feature(STREAMING_INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request()
                                            .body(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
                                            .build();

        RequestChecksumFeature.Prepared prepared =
            feature.prepare(new MapStruct(STREAMING_INPUT), request, RequestChecksumCalculation.WHEN_SUPPORTED,
                            PayloadSigning.UNSIGNED);

        assertThat(prepared.pendingHeaderChecksum()).isEmpty();
        assertThat(prepared.request().headers().firstValue("x-amz-content-sha256"))
            .contains("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThatThrownBy(() -> RequestChecksumFeature.withHeaderChecksum(
            request, new ResolvedRequestChecksum(ChecksumAlgorithm.CRC32, "x-amz-checksum-crc32", Location.TRAILER,
                                                 false), new byte[4]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("trailer");
    }

    @Test
    void apply_whenStreamingSignedChunks_declaresTrailerForTheSigner() {
        RequestChecksumFeature feature = feature(STREAMING_INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request()
                                            .body(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
                                            .build();

        SdkHttpRequest result = feature.apply(new MapStruct(STREAMING_INPUT), request,
                                              RequestChecksumCalculation.WHEN_SUPPORTED,
                                              PayloadSigning.STREAMING_SIGNED);

        assertThat(result.headers().firstValue("x-amz-trailer")).contains("x-amz-checksum-crc32");
        assertThat(result.headers().firstValue(ALGORITHM_HEADER)).contains("CRC32");
        assertThat(result.body()).isSameAs(request.body());
    }

    @Test
    void apply_whenTrailerNeededButAwsChunkedAlreadyDeclared_blocks() {
        RequestChecksumFeature feature = feature(STREAMING_INPUT, OPTIONAL);
        SdkHttpRequest request = TestSupport.request().putHeader("Content-Encoding", "aws-chunked")
                                            .body(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
                                            .build();

        assertThatThrownBy(() -> feature.apply(new MapStruct(STREAMING_INPUT), request,
                                               RequestChecksumCalculation.WHEN_SUPPORTED, PayloadSigning.UNSIGNED))
            .isInstanceOfSatisfying(ModelConflictException.class,
                                    e -> assertThat(e.setting()).isEqualTo("Content-Encoding"));
    }

    private static RequestChecksumFeature feature(Schema input, HttpChecksumMetadata metadata) {
        return RequestChecksumFeature.forOperation(OPERATION_SCHEMA, input, metadata).orElseThrow();
    }

    private static String crc32(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        return Base64.getEncoder().encodeToString(java.nio.ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
