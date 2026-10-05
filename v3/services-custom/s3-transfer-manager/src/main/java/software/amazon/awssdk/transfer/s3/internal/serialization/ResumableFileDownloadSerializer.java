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
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.invalid;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.longValue;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.object;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TokenFields.string;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

/**
 * Reads and writes {@link ResumableFileDownload} tokens as JSON with the V2 field names ({@code bytesTransferred},
 * {@code fileLastModified}, {@code totalSizeInBytes}, {@code s3ObjectLastModified}, {@code s3ObjectEtag},
 * {@code completedParts}, {@code downloadFileRequest.destination}, {@code downloadFileRequest.getObjectRequest}).
 * ADAPTED from V2 in the same way as {@link ResumableFileUploadSerializer}. Not a public API.
 */
public final class ResumableFileDownloadSerializer {
    private static final String BYTES_TRANSFERRED = "bytesTransferred";
    private static final String FILE_LAST_MODIFIED = "fileLastModified";
    private static final String TOTAL_SIZE_IN_BYTES = "totalSizeInBytes";
    private static final String S3_OBJECT_LAST_MODIFIED = "s3ObjectLastModified";
    private static final String S3_OBJECT_ETAG = "s3ObjectEtag";
    private static final String COMPLETED_PARTS = "completedParts";
    private static final String DOWNLOAD_FILE_REQUEST = "downloadFileRequest";
    private static final String DESTINATION = "destination";
    private static final String GET_OBJECT_REQUEST = "getObjectRequest";

    private ResumableFileDownloadSerializer() {
    }

    public static byte[] toJson(ResumableFileDownload download) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put(BYTES_TRANSFERRED, download.bytesTransferred());
        if (download.fileLastModified() != null) {
            json.put(FILE_LAST_MODIFIED, download.fileLastModified().toString());
        }
        download.totalSizeInBytes().ifPresent(v -> json.put(TOTAL_SIZE_IN_BYTES, v));
        download.s3ObjectLastModified().ifPresent(v -> json.put(S3_OBJECT_LAST_MODIFIED, v.toString()));
        download.s3ObjectEtag().ifPresent(v -> json.put(S3_OBJECT_ETAG, v));
        if (!download.completedParts().isEmpty()) {
            json.put(COMPLETED_PARTS, new ArrayList<Object>(download.completedParts()));
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(DESTINATION, download.downloadFileRequest().destination().toString());
        request.put(GET_OBJECT_REQUEST, ShapeJson.toJson(download.downloadFileRequest().getObjectRequest()));
        json.put(DOWNLOAD_FILE_REQUEST, request);
        return Json.write(json).getBytes(StandardCharsets.UTF_8);
    }

    public static ResumableFileDownload fromJson(String contents) {
        Map<String, Object> json = object(Json.parse(contents), "token");
        Map<String, Object> request = object(json.get(DOWNLOAD_FILE_REQUEST), DOWNLOAD_FILE_REQUEST);
        GetObjectRequest getObjectRequest =
            ShapeJson.fromJson(request.get(GET_OBJECT_REQUEST), GetObjectRequest.builder()).build();
        DownloadFileRequest downloadFileRequest =
            DownloadFileRequest.builder()
                               .destination(Paths.get(string(request.get(DESTINATION), DESTINATION)))
                               .getObjectRequest(getObjectRequest)
                               .build();
        return ResumableFileDownload.builder()
                                    .downloadFileRequest(downloadFileRequest)
                                    .bytesTransferred(longValue(json.get(BYTES_TRANSFERRED), BYTES_TRANSFERRED))
                                    .fileLastModified(instant(json.get(FILE_LAST_MODIFIED), FILE_LAST_MODIFIED))
                                    .totalSizeInBytes(longValue(json.get(TOTAL_SIZE_IN_BYTES), TOTAL_SIZE_IN_BYTES))
                                    .s3ObjectLastModified(instant(json.get(S3_OBJECT_LAST_MODIFIED),
                                                                  S3_OBJECT_LAST_MODIFIED))
                                    .s3ObjectEtag((String) json.get(S3_OBJECT_ETAG))
                                    .completedParts(parts(json.get(COMPLETED_PARTS)))
                                    .build();
    }

    public static ResumableFileDownload fromJson(byte[] contents) {
        return fromJson(new String(contents, StandardCharsets.UTF_8));
    }

    public static ResumableFileDownload fromJson(InputStream contents) {
        try {
            return fromJson(contents.readAllBytes());
        } catch (IOException e) {
            throw SdkClientException.create("Failed to read the resumable file download token", e);
        }
    }

    private static List<Integer> parts(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw invalid(COMPLETED_PARTS);
        }
        List<Integer> result = new ArrayList<>(list.size());
        for (Object part : list) {
            if (!(part instanceof BigDecimal d)) {
                throw invalid(COMPLETED_PARTS);
            }
            try {
                result.add(d.intValueExact());
            } catch (ArithmeticException e) {
                throw invalid(COMPLETED_PARTS);
            }
        }
        return result;
    }
}
