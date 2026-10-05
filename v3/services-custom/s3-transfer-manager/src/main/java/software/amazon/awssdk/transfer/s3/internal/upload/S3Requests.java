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

package software.amazon.awssdk.transfer.s3.internal.upload;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ChecksumType;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.transfer.s3.internal.util.Shapes;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Builds the multipart requests of an upload or copy from the caller's {@code PutObjectRequest} or
 * {@code CopyObjectRequest}, carrying over every member the target operation models (V2
 * {@code SdkPojoConversionUtils} semantics, implemented over the model schemas).
 */
public final class S3Requests {
    /** PutObjectRequest members an upload may split into multipart requests (the V2 allow list). */
    static final Set<String> PUT_OBJECT_ALLOWED = Set.of(
        "ACL", "Bucket", "Body", "CacheControl", "ContentDisposition", "ContentEncoding", "ContentLanguage",
        "ContentLength", "ContentMD5", "ContentType", "ChecksumAlgorithm", "ChecksumCRC32", "ChecksumCRC32C",
        "ChecksumCRC64NVME", "ChecksumSHA1", "ChecksumSHA256", "ChecksumSHA512", "ChecksumMD5", "ChecksumXXHASH64",
        "ChecksumXXHASH3", "ChecksumXXHASH128", "Expires", "IfMatch", "IfNoneMatch", "GrantFullControl", "GrantRead",
        "GrantReadACP", "GrantWriteACP", "Key", "WriteOffsetBytes", "Metadata", "ServerSideEncryption",
        "StorageClass", "WebsiteRedirectLocation", "SSECustomerAlgorithm", "SSECustomerKey", "SSECustomerKeyMD5",
        "SSEKMSKeyId", "SSEKMSEncryptionContext", "BucketKeyEnabled", "RequestPayer", "Tagging", "ObjectLockMode",
        "ObjectLockRetainUntilDate", "ObjectLockLegalHoldStatus", "ObjectLockEventHold",
        "ObjectLockEventHoldDurationDays", "ObjectLockEventHoldDurationYears", "ExpectedBucketOwner");

    /** CopyObjectRequest members a multipart copy carries over (the V2 allow list, with V3's single CopySource). */
    static final Set<String> COPY_OBJECT_ALLOWED = Set.of(
        "ACL", "Bucket", "Key", "CacheControl", "ChecksumAlgorithm", "ContentDisposition", "ContentEncoding",
        "ContentLanguage", "ContentType", "CopySource", "CopySourceIfMatch", "CopySourceIfModifiedSince",
        "CopySourceIfNoneMatch", "CopySourceIfUnmodifiedSince", "CopySourceSSECustomerAlgorithm",
        "CopySourceSSECustomerKey", "CopySourceSSECustomerKeyMD5", "Expires", "GrantFullControl", "GrantRead",
        "GrantReadACP", "GrantWriteACP", "Metadata", "MetadataDirective", "TaggingDirective", "AnnotationDirective",
        "ServerSideEncryption", "StorageClass", "WebsiteRedirectLocation", "SSECustomerAlgorithm", "SSECustomerKey",
        "SSECustomerKeyMD5", "SSEKMSKeyId", "SSEKMSEncryptionContext", "BucketKeyEnabled", "RequestPayer", "Tagging",
        "ObjectLockMode", "ObjectLockRetainUntilDate", "ObjectLockLegalHoldStatus", "ObjectLockEventHold",
        "ObjectLockEventHoldDurationDays", "ObjectLockEventHoldDurationYears", "ExpectedBucketOwner",
        "ExpectedSourceBucketOwner");

    /** Whole-object values that do not apply to one part. */
    private static final Set<String> PART_EXCLUDED = partExcluded();

    private S3Requests() {
    }

    private static Set<String> partExcluded() {
        Set<String> names = PutObjectRequest.SCHEMA.members().stream().map(Schema::memberName)
                                                   .filter(n -> n.startsWith("Checksum") && !n.equals("ChecksumAlgorithm"))
                                                   .collect(Collectors.toSet());
        names.add("ContentMD5");
        names.add("ContentLength");
        return Set.copyOf(names);
    }

    public static void validateForMultipart(PutObjectRequest request) {
        Shapes.validateMembers(request, PUT_OBJECT_ALLOWED, "Multipart upload");
    }

    public static void validateForMultipart(CopyObjectRequest request) {
        Shapes.validateMembers(request, COPY_OBJECT_ALLOWED, "Multipart copy");
    }

    public static CreateMultipartUploadRequest createMultipartUpload(SerializableStruct request) {
        CreateMultipartUploadRequest.Builder builder =
            Shapes.copyMembers(request, CreateMultipartUploadRequest.builder(), Set.of());
        if (request instanceof PutObjectRequest put && checksumValueSpecified(put)) {
            builder.checksumType(ChecksumType.FULL_OBJECT);
        }
        return builder.build();
    }

    public static UploadPartRequest uploadPart(PutObjectRequest request, String uploadId, int partNumber,
                                               long length) {
        return Shapes.copyMembers(request, UploadPartRequest.builder(), PART_EXCLUDED)
                     .uploadId(uploadId).partNumber(partNumber).contentLength(length).build();
    }

    public static UploadPartCopyRequest uploadPartCopy(CopyObjectRequest request, String uploadId, int partNumber,
                                                       long first, long last) {
        return Shapes.copyMembers(request, UploadPartCopyRequest.builder(), Set.of())
                     .uploadId(uploadId).partNumber(partNumber).copySourceRange("bytes=" + first + "-" + last)
                     .build();
    }

    public static CompleteMultipartUploadRequest complete(SerializableStruct request, String uploadId,
                                                          List<CompletedPart> parts, Long objectSize) {
        CompleteMultipartUploadRequest.Builder builder =
            Shapes.copyMembers(request, CompleteMultipartUploadRequest.builder(), Set.of())
                  .uploadId(uploadId)
                  .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build());
        if (objectSize != null) {
            builder.mpuObjectSize(objectSize);
        }
        if (request instanceof PutObjectRequest put && checksumValueSpecified(put)) {
            builder.checksumType(ChecksumType.FULL_OBJECT);
        }
        return builder.build();
    }

    public static AbortMultipartUploadRequest abort(SerializableStruct request, String uploadId) {
        return Shapes.copyMembers(request, AbortMultipartUploadRequest.builder(), Set.of()).uploadId(uploadId).build();
    }

    public static ListPartsRequest listParts(PutObjectRequest request, String uploadId) {
        return Shapes.copyMembers(request, ListPartsRequest.builder(), Set.of()).uploadId(uploadId).build();
    }

    public static CompletedPart completedPart(UploadPartResponse response, int partNumber) {
        return Shapes.copyMembers(response, CompletedPart.builder(), Set.of()).partNumber(partNumber).build();
    }

    public static CompletedPart completedPart(CopyPartResult result, int partNumber) {
        return Shapes.copyMembers(result, CompletedPart.builder(), Set.of("LastModified")).partNumber(partNumber)
                     .build();
    }

    public static CompletedPart completedPart(Part part) {
        return Shapes.copyMembers(part, CompletedPart.builder(), Set.of("LastModified", "Size")).build();
    }

    public static PutObjectResponse putObjectResponse(CompleteMultipartUploadResponse response) {
        return Shapes.copyMembers(response, PutObjectResponse.builder(), Set.of()).build();
    }

    public static CopyObjectResponse copyObjectResponse(CompleteMultipartUploadResponse response) {
        CopyObjectResult result = Shapes.copyMembers(response, CopyObjectResult.builder(), Set.of()).build();
        return Shapes.copyMembers(response, CopyObjectResponse.builder(), Set.of()).copyObjectResult(result).build();
    }

    private static boolean checksumValueSpecified(PutObjectRequest request) {
        for (Schema member : request.schema().members()) {
            String name = member.memberName();
            if (name.startsWith("Checksum") && !name.equals("ChecksumAlgorithm")
                && request.getMemberValue(member) != null) {
                return true;
            }
        }
        return false;
    }
}
