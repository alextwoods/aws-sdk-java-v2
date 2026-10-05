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

package software.amazon.awssdk.checksums.compression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

class RequestCompressionFeatureTest {
    private static final ShapeId OPERATION = ShapeId.from("com.example#PutMetrics");
    private static final Schema STRING = Schema.createString(ShapeId.from("smithy.api#String"));
    private static final Schema STREAM = Schema.createBlob(ShapeId.from("com.example#Stream"), new StreamingTrait());
    private static final Schema INPUT = Schema.structureBuilder(ShapeId.from("com.example#PutMetricsInput"))
                                              .putMember("Data", STRING)
                                              .build();
    private static final Schema STREAMING_INPUT = Schema.structureBuilder(ShapeId.from("com.example#UploadInput"))
                                                        .putMember("Body", STREAM)
                                                        .build();
    private static final CompressionSettings SETTINGS = new CompressionSettings(true, 100);

    @Test
    void forOperation_selectsFirstSupportedEncoding() {
        assertThat(RequestCompressionFeature.forOperation(operation("br", "GZIP"), INPUT))
            .hasValueSatisfying(feature -> assertThat(feature.encoding()).isEqualTo("gzip"));
        assertThat(RequestCompressionFeature.forOperation(operation("br"), INPUT)).isEmpty();
        assertThat(RequestCompressionFeature.forOperation(Schema.createOperation(OPERATION), INPUT)).isEmpty();
    }

    @Test
    void forOperation_whenStreamingMemberRequiresLength_rejects() {
        Schema input = Schema.structureBuilder(ShapeId.from("com.example#SizedInput"))
                             .putMember("Body", STREAM, new RequiresLengthTrait())
                             .build();
        assertThatThrownBy(() -> RequestCompressionFeature.forOperation(operation("gzip"), input))
            .isInstanceOfSatisfying(ModelConflictException.class, e -> {
                assertThat(e.operation()).isEqualTo(OPERATION);
                assertThat(e.setting()).isEqualTo("requestCompression");
            });
    }

    @Test
    void apply_whenBelowThreshold_leavesPayload() {
        SdkHttpRequest request = request(HttpBody.fromBytes(new byte[99])).build();
        assertThat(feature(INPUT).apply(request, SETTINGS)).isSameAs(request);
    }

    @Test
    void apply_whenDisabled_leavesPayload() {
        SdkHttpRequest request = request(HttpBody.fromBytes(new byte[1000])).build();
        assertThat(feature(INPUT).apply(request, new CompressionSettings(false, 0))).isSameAs(request);
    }

    @Test
    void apply_whenAtThreshold_compressesAndAppendsAfterCallerEncoding() throws IOException {
        byte[] payload = new byte[100];
        SdkHttpRequest request = request(HttpBody.fromBytes(payload)).putHeader("Content-Encoding", "br")
                                                                     .putHeader("Content-Length", "100").build();

        SdkHttpRequest result = feature(INPUT).apply(request, SETTINGS);

        assertThat(result.headers().allValues("Content-Encoding")).containsExactly("br, gzip");
        assertThat(result.headers().firstValue("Content-Length"))
            .contains(Long.toString(result.body().contentLength().getAsLong()));
        assertThat(gunzip(result.body().asByteArray())).isEqualTo(payload);
    }

    @Test
    void apply_whenModeledStreaming_compressesRegardlessOfSizeWithUnknownLength() throws IOException {
        byte[] payload = {1, 2, 3};
        SdkHttpRequest request = request(HttpBody.streaming(StreamingBodies.fromBytes(payload, null)))
            .putHeader("Content-Length", "3").build();

        SdkHttpRequest result = feature(STREAMING_INPUT).apply(request, SETTINGS);

        assertThat(result.headers().contains("Content-Length")).isFalse();
        assertThat(result.body().contentLength()).isEmpty();
        assertThat(result.headers().firstValue("Content-Encoding")).contains("gzip");
        try (InputStream in = result.body().streamingBody().openStream()) {
            assertThat(gunzip(in.readAllBytes())).isEqualTo(payload);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Content-MD5", "x-amz-checksum-crc32", "x-amz-trailer"})
    void apply_whenCallerSuppliedChecksum_blocks(String header) {
        SdkHttpRequest request = request(HttpBody.fromBytes(new byte[1000])).putHeader(header, "value").build();

        assertThatThrownBy(() -> feature(INPUT).apply(request, SETTINGS))
            .isInstanceOfSatisfying(ModelConflictException.class, e -> {
                assertThat(e.operation()).isEqualTo(OPERATION);
                assertThat(e.setting()).isEqualTo(header);
            });
    }

    @Test
    void settings_validateThresholdRange() {
        assertThatThrownBy(() -> new CompressionSettings(true, -1))
            .isInstanceOfSatisfying(SdkClientConfigurationException.class,
                                    e -> assertThat(e.option()).contains("RequestMinCompressionSizeBytes"));
        assertThatThrownBy(() -> new CompressionSettings(true, 10_485_761))
            .isInstanceOf(SdkClientConfigurationException.class);
        assertThat(new CompressionSettings(true, 10_485_760).minCompressionSizeBytes()).isEqualTo(10_485_760);
    }

    @Test
    void settings_fromConfiguration_defaultsAndReadsOptions() {
        assertThat(CompressionSettings.from(SdkClientConfiguration.empty())).isEqualTo(CompressionSettings.DEFAULT);
        SdkClientConfiguration configuration =
            SdkClientConfiguration.builder()
                                  .option(CompressionOptions.DISABLE_REQUEST_COMPRESSION, true)
                                  .option(CompressionOptions.REQUEST_MIN_COMPRESSION_SIZE_BYTES, 5)
                                  .build();
        assertThat(CompressionSettings.from(configuration)).isEqualTo(new CompressionSettings(false, 5));
    }

    private static Schema operation(String... encodings) {
        return Schema.createOperation(OPERATION, RequestCompressionTrait.builder().encodings(List.of(encodings))
                                                                        .build());
    }

    private static RequestCompressionFeature feature(Schema input) {
        return RequestCompressionFeature.forOperation(operation("gzip"), input).orElseThrow();
    }

    private static SdkHttpRequest.Builder request(HttpBody body) {
        return SdkHttpRequest.builder().method("POST").host("example.com").body(body);
    }

    private static byte[] gunzip(byte[] gzip) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gzip))) {
            return in.readAllBytes();
        }
    }
}
