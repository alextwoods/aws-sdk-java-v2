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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.instant;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.longValue;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.object;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.string;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * Reads and writes {@link ResumableFileUpload} tokens as JSON with the V2 field names ({@code fileLength},
 * {@code fileLastModified}, {@code multipartUploadId}, {@code partSizeInBytes}, {@code totalParts},
 * {@code transferredParts}, {@code uploadFileRequest.source}, {@code uploadFileRequest.putObjectRequest}). ADAPTED
 * from V2: timestamps are ISO-8601 strings and request members are keyed by modeled member name, so V2 tokens are not
 * read. Transfer listeners are not serialized. Not a public API.
 */
public final class ResumableFileUploadSerializer {
    private static final String MULTIPART_UPLOAD_ID = "multipartUploadId";
    private static final String FILE_LENGTH = "fileLength";
    private static final String FILE_LAST_MODIFIED = "fileLastModified";
    private static final String PART_SIZE_IN_BYTES = "partSizeInBytes";
    private static final String TOTAL_PARTS = "totalParts";
    private static final String TRANSFERRED_PARTS = "transferredParts";
    private static final String UPLOAD_FILE_REQUEST = "uploadFileRequest";
    private static final String SOURCE = "source";
    private static final String PUT_OBJECT_REQUEST = "putObjectRequest";

    private ResumableFileUploadSerializer() {
    }

    public static byte[] toJson(ResumableFileUpload upload) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put(FILE_LENGTH, upload.fileLength());
        json.put(FILE_LAST_MODIFIED, upload.fileLastModified().toString());
        upload.multipartUploadId().ifPresent(id -> json.put(MULTIPART_UPLOAD_ID, id));
        upload.partSizeInBytes().ifPresent(v -> json.put(PART_SIZE_IN_BYTES, v));
        upload.totalParts().ifPresent(v -> json.put(TOTAL_PARTS, v));
        upload.transferredParts().ifPresent(v -> json.put(TRANSFERRED_PARTS, v));
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(SOURCE, upload.uploadFileRequest().source().toString());
        request.put(PUT_OBJECT_REQUEST, ShapeJson.toJson(upload.uploadFileRequest().putObjectRequest()));
        json.put(UPLOAD_FILE_REQUEST, request);
        return Json.write(json).getBytes(StandardCharsets.UTF_8);
    }

    public static ResumableFileUpload fromJson(String contents) {
        Map<String, Object> json = object(Json.parse(contents), "token");
        Map<String, Object> request = object(json.get(UPLOAD_FILE_REQUEST), UPLOAD_FILE_REQUEST);
        PutObjectRequest putObjectRequest =
            ShapeJson.fromJson(request.get(PUT_OBJECT_REQUEST), PutObjectRequest.builder()).build();
        UploadFileRequest uploadFileRequest =
            UploadFileRequest.builder()
                             .source(Paths.get(string(request.get(SOURCE), SOURCE)))
                             .putObjectRequest(putObjectRequest)
                             .build();
        return ResumableFileUpload.builder()
                                  .uploadFileRequest(uploadFileRequest)
                                  .fileLength(longValue(json.get(FILE_LENGTH), FILE_LENGTH))
                                  .fileLastModified(instant(json.get(FILE_LAST_MODIFIED), FILE_LAST_MODIFIED))
                                  .multipartUploadId((String) json.get(MULTIPART_UPLOAD_ID))
                                  .partSizeInBytes(longValue(json.get(PART_SIZE_IN_BYTES), PART_SIZE_IN_BYTES))
                                  .totalParts(longValue(json.get(TOTAL_PARTS), TOTAL_PARTS))
                                  .transferredParts(longValue(json.get(TRANSFERRED_PARTS), TRANSFERRED_PARTS))
                                  .build();
    }

    public static ResumableFileUpload fromJson(byte[] contents) {
        return fromJson(new String(contents, StandardCharsets.UTF_8));
    }

    public static ResumableFileUpload fromJson(InputStream contents) {
        try {
            return fromJson(contents.readAllBytes());
        } catch (IOException e) {
            throw SdkClientException.create("Failed to read the resumable file upload token", e);
        }
    }
}
