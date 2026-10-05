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

package software.amazon.awssdk.transfer.s3.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

/** Resumable-transfer tokens round-trip through every V2 serialization form and reject malformed input. */
class ResumableTransferSerializationTest {
    private static final ResumableFileUpload UPLOAD = ResumableFileUpload.builder()
        .uploadFileRequest(u -> u.source(Paths.get("/tmp/in \"quoted\".bin"))
                                 .putObjectRequest(PutObjectRequest.builder()
                                                                   .bucket("b").key("dir/k")
                                                                   .metadata(Map.of("a", "1", "b", "\u00e9\n"))
                                                                   .storageClass(StorageClass.STANDARD_IA)
                                                                   .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                   .objectLockRetainUntilDate(
                                                                       Instant.parse("2030-01-02T03:04:05.123Z"))
                                                                   .bucketKeyEnabled(true)
                                                                   .build()))
        .fileLength(123L)
        .fileLastModified(Instant.parse("2024-05-06T07:08:09.000000001Z"))
        .multipartUploadId("id/with+chars")
        .partSizeInBytes(8L * 1024 * 1024)
        .totalParts(16L)
        .transferredParts(3L)
        .build();

    private static final ResumableFileDownload DOWNLOAD = ResumableFileDownload.builder()
        .downloadFileRequest(d -> d.destination(Paths.get("out.bin"))
                                   .getObjectRequest(GetObjectRequest.builder().bucket("b").key("k").versionId("v1")
                                                                     .build()))
        .bytesTransferred(42L)
        .fileLastModified(Instant.parse("2024-01-01T00:00:00Z"))
        .s3ObjectLastModified(Instant.parse("2023-01-01T00:00:00Z"))
        .s3ObjectEtag("\"etag\"")
        .totalSizeInBytes(1000L)
        .completedParts(List.of(1, 3))
        .build();

    @TempDir
    Path dir;

    @Test
    void resumableFileUpload_roundTripsThroughEveryForm() throws java.io.IOException {
        assertThat(ResumableFileUpload.fromString(UPLOAD.serializeToString())).isEqualTo(UPLOAD);
        assertThat(ResumableFileUpload.fromBytes(UPLOAD.serializeToBytes())).isEqualTo(UPLOAD);
        Path file = dir.resolve("upload.json");
        UPLOAD.serializeToFile(file);
        assertThat(ResumableFileUpload.fromFile(file)).isEqualTo(UPLOAD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UPLOAD.serializeToOutputStream(out);
        assertThat(ResumableFileUpload.fromBytes(software.amazon.awssdk.core.SdkBytes.fromByteArray(out.toByteArray())))
            .isEqualTo(UPLOAD);
        assertThat(new String(UPLOAD.serializeToInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
            .contains("\"multipartUploadId\":\"id/with+chars\"").contains("\"StorageClass\":\"STANDARD_IA\"");
    }

    @Test
    void resumableFileDownload_roundTripsThroughEveryForm() {
        assertThat(ResumableFileDownload.fromString(DOWNLOAD.serializeToString())).isEqualTo(DOWNLOAD);
        Path file = dir.resolve("download.json");
        DOWNLOAD.serializeToFile(file);
        assertThat(ResumableFileDownload.fromFile(file)).isEqualTo(DOWNLOAD);
        assertThat(ResumableFileDownload.fromBytes(DOWNLOAD.serializeToBytes())).isEqualTo(DOWNLOAD);
    }

    @Test
    void fromString_malformedToken_failsWithAClientException() {
        assertThatThrownBy(() -> ResumableFileUpload.fromString("{\"fileLength\": 1"))
            .isInstanceOf(SdkClientException.class).hasMessageContaining("Invalid resumable transfer token");
        assertThatThrownBy(() -> ResumableFileDownload.fromString("[]"))
            .isInstanceOf(SdkClientException.class);
        assertThatThrownBy(() -> ResumableFileUpload.fromString(
            "{\"fileLength\":\"x\",\"fileLastModified\":\"2024-01-01T00:00:00Z\",\"uploadFileRequest\":"
            + "{\"source\":\"a\",\"putObjectRequest\":{}}}"))
            .isInstanceOf(SdkClientException.class).hasMessageContaining("fileLength");
    }

    @Test
    void fromString_unknownRequestMembers_areIgnored() {
        String json = new String(UPLOAD.serializeToBytes().asByteArray())
            .replace("\"Bucket\":", "\"NotAModeledMember\":\"x\",\"Bucket\":");
        assertThat(ResumableFileUpload.fromString(json)).isEqualTo(UPLOAD);
    }
}
