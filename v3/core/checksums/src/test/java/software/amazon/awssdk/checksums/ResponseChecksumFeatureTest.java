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

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.ChecksumValidationResult.Status;
import software.amazon.awssdk.checksums.ResponseChecksumFeature.ValidatedResponse;
import software.amazon.awssdk.checksums.TestSupport.MapStruct;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;

class ResponseChecksumFeatureTest {
    private static final Schema INPUT = Schema.structureBuilder(ShapeId.from("com.example#GetThingInput"))
                                              .putMember("ChecksumMode", RequestChecksumFeatureTest.STRING,
                                                         new HttpHeaderTrait("x-amz-checksum-mode"))
                                              .build();
    private static final Schema OUTPUT = Schema.structureBuilder(ShapeId.from("com.example#GetThingOutput"))
                                               .putMember("Data", RequestChecksumFeatureTest.BLOB)
                                               .build();
    private static final Schema STREAMING_OUTPUT =
        Schema.structureBuilder(ShapeId.from("com.example#GetStreamOutput"))
              .putMember("Body", RequestChecksumFeatureTest.STREAM, new HttpPayloadTrait())
              .build();
    private static final HttpChecksumMetadata METADATA = HttpChecksumMetadata.builder()
                                                                             .requestValidationModeMember("ChecksumMode")
                                                                             .addResponseAlgorithm("XXHASH64")
                                                                             .addResponseAlgorithm("CRC32C")
                                                                             .addResponseAlgorithm("MD5")
                                                                             .addResponseAlgorithm("SHA256")
                                                                             .build();
    private static final byte[] PAYLOAD = "response payload".getBytes(StandardCharsets.UTF_8);

    @Test
    void forOperation_keepsSupportedAlgorithmsInModeledOrder() {
        assertThat(feature(OUTPUT).validationAlgorithms())
            .containsExactly(ChecksumAlgorithm.CRC32C, ChecksumAlgorithm.SHA256);
    }

    @Test
    void forOperation_whenUnknownAlgorithm_rejects() {
        HttpChecksumMetadata metadata = HttpChecksumMetadata.builder().requestValidationModeMember("ChecksumMode")
                                                            .addResponseAlgorithm("CRC16").build();
        assertThatThrownBy(() -> ResponseChecksumFeature.forOperation(
            RequestChecksumFeatureTest.OPERATION_SCHEMA, INPUT, OUTPUT, metadata))
            .isInstanceOfSatisfying(ModelConflictException.class,
                                    e -> assertThat(e.setting()).isEqualTo("httpChecksum.responseAlgorithms"))
            .hasMessageContaining("CRC16");
    }

    @Test
    void validationEnabled_followsMemberThenConfiguration() {
        ResponseChecksumFeature feature = feature(OUTPUT);
        assertThat(feature.validationEnabled(new MapStruct(INPUT), ResponseChecksumValidation.WHEN_SUPPORTED)).isTrue();
        assertThat(feature.validationEnabled(new MapStruct(INPUT), ResponseChecksumValidation.WHEN_REQUIRED)).isFalse();
        assertThat(feature.validationEnabled(new MapStruct(INPUT).with("ChecksumMode", "ENABLED"),
                                             ResponseChecksumValidation.WHEN_REQUIRED)).isTrue();
        assertThat(feature.validationEnabled(new MapStruct(INPUT).with("ChecksumMode", "DISABLED"),
                                             ResponseChecksumValidation.WHEN_SUPPORTED)).isFalse();
    }

    @Test
    void prepareRequest_whenUnsetUnderWhenSupported_setsImplicitEnabledHeader() {
        ResponseChecksumFeature feature = feature(OUTPUT);
        SdkHttpRequest request = TestSupport.request().build();

        assertThat(feature.prepareRequest(new MapStruct(INPUT), request, ResponseChecksumValidation.WHEN_SUPPORTED)
                          .headers().firstValue("x-amz-checksum-mode")).contains("ENABLED");
        assertThat(feature.prepareRequest(new MapStruct(INPUT), request, ResponseChecksumValidation.WHEN_REQUIRED))
            .isSameAs(request);
    }

    @Test
    void validate_whenBufferedChecksumMatches_validatesFirstSupportedModeledAlgorithm() {
        SdkHttpResponse response = response(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
            .addHeader("x-amz-checksum-xxhash64", "ignored")
            .addHeader("x-amz-checksum-sha256", SdkChecksum.base64(ChecksumAlgorithm.SHA256, PAYLOAD))
            .addHeader("x-amz-checksum-crc32c", SdkChecksum.base64(ChecksumAlgorithm.CRC32C, PAYLOAD))
            .build();

        ValidatedResponse validated = feature(OUTPUT).validate(response, true);

        assertThat(validated.result()).isEqualTo(ChecksumValidationResult.of(Status.VALIDATED,
                                                                             ChecksumAlgorithm.CRC32C));
        assertThat(validated.response().body().kind()).isEqualTo(HttpBody.Kind.BYTES);
        assertThat(validated.response().body().asByteArray()).isEqualTo(PAYLOAD);
    }

    @Test
    void validate_whenBufferedChecksumDiffers_throwsBeforeReturning() {
        SdkHttpResponse response = response(HttpBody.fromBytes(PAYLOAD))
            .addHeader("x-amz-checksum-crc32c", SdkChecksum.base64(ChecksumAlgorithm.CRC32C, new byte[1]))
            .build();

        assertThatThrownBy(() -> feature(OUTPUT).validate(response, true))
            .isInstanceOfSatisfying(ChecksumMismatchException.class,
                                    e -> assertThat(e.algorithm()).isEqualTo(ChecksumAlgorithm.CRC32C));
    }

    @Test
    void validate_whenStreamingOutput_failsFinalRead() {
        SdkHttpResponse response = response(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
            .addHeader("x-amz-checksum-sha256", SdkChecksum.base64(ChecksumAlgorithm.SHA256, new byte[1]))
            .build();

        ValidatedResponse validated = feature(STREAMING_OUTPUT).validate(response, true);

        assertThat(validated.result().status()).isEqualTo(Status.VALIDATING_STREAM);
        assertThat(validated.response().body().contentLength()).hasValue(PAYLOAD.length);
        assertThatThrownBy(() -> TestSupport.readStream(validated.response().body()))
            .isInstanceOf(ChecksumMismatchException.class);
    }

    @Test
    void validate_whenStreamingOutputMatches_deliversPayload() {
        SdkHttpResponse response = response(HttpBody.streaming(StreamingBodies.fromBytes(PAYLOAD, null)))
            .addHeader("x-amz-checksum-sha256", SdkChecksum.base64(ChecksumAlgorithm.SHA256, PAYLOAD))
            .build();

        ValidatedResponse validated = feature(STREAMING_OUTPUT).validate(response, true);

        assertThat(TestSupport.readPublisher(validated.response().body().streamingBody())).isEqualTo(PAYLOAD);
    }

    @Test
    void validate_whenNotValidatable_reportsWhy() {
        ResponseChecksumFeature feature = feature(OUTPUT);
        SdkHttpResponse plain = response(HttpBody.fromBytes(PAYLOAD)).build();
        SdkHttpResponse composite = response(HttpBody.fromBytes(PAYLOAD))
            .addHeader("x-amz-checksum-crc32c", "AAAAAA==-3").build();
        SdkHttpResponse error = SdkHttpResponse.builder().statusCode(500)
                                               .addHeader("x-amz-checksum-crc32c", "AAAAAA==")
                                               .body(HttpBody.fromBytes(PAYLOAD)).build();

        assertThat(feature.validate(plain, false).result().status()).isEqualTo(Status.NOT_ENABLED);
        assertThat(feature.validate(plain, true).result().status()).isEqualTo(Status.CHECKSUM_NOT_FOUND);
        assertThat(feature.validate(composite, true).result().status()).isEqualTo(Status.COMPOSITE_CHECKSUM_SKIPPED);
        assertThat(feature.validate(error, true).result().status()).isEqualTo(Status.NOT_SUCCESS_RESPONSE);
    }

    private static ResponseChecksumFeature feature(Schema output) {
        return ResponseChecksumFeature.forOperation(RequestChecksumFeatureTest.OPERATION_SCHEMA, INPUT, output,
                                                    METADATA).orElseThrow();
    }

    private static SdkHttpResponse.Builder response(HttpBody body) {
        return SdkHttpResponse.builder().statusCode(200).body(body);
    }
}
