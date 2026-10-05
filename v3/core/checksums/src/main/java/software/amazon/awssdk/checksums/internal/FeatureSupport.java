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

package software.amazon.awssdk.checksums.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;

/** Schema, member and header helpers shared by the payload feature plugins. */
public final class FeatureSupport {
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_MD5 = "Content-MD5";
    public static final String X_AMZ_TRAILER = "x-amz-trailer";
    public static final String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";
    public static final String X_AMZ_DECODED_CONTENT_LENGTH = "x-amz-decoded-content-length";
    public static final String AWS_CHUNKED = "aws-chunked";
    public static final String STREAMING_UNSIGNED_PAYLOAD_TRAILER = "STREAMING-UNSIGNED-PAYLOAD-TRAILER";

    private static final TraitKey<RequiresLengthTrait> REQUIRES_LENGTH = TraitKey.get(RequiresLengthTrait.class);

    private FeatureSupport() {
    }

    /**
     * The top-level input member {@code name} of {@code input}.
     *
     * @throws ModelConflictException naming {@code setting} if the input has no such member
     */
    public static Schema requireMember(ShapeId operation, Schema input, String name, String setting) {
        for (Schema member : input.members()) {
            if (member.memberName().equals(name)) {
                return member;
            }
        }
        throw ModelConflictException.create(operation, setting, "the input " + input.id() + " has no member '" + name
                                                                + "'");
    }

    /** The HTTP header a member is bound to by {@code @httpHeader}, if any. */
    public static Optional<String> headerBinding(Schema member) {
        HttpHeaderTrait header = member.getTrait(TraitKey.HTTP_HEADER_TRAIT);
        return header == null ? Optional.empty() : Optional.of(header.getValue());
    }

    /** The wire string of a string or enum member of {@code input}, if set. */
    public static Optional<String> stringValue(SerializableStruct input, Schema member) {
        Object value = input.getMemberValue(member);
        return value == null ? Optional.empty() : Optional.of(value.toString());
    }

    /** The top-level {@code @streaming} blob member of {@code input}, if any (not an event stream). */
    public static Optional<Schema> streamingBlobMember(Schema input) {
        for (Schema member : input.members()) {
            if (member.type() == ShapeType.BLOB && member.hasTrait(TraitKey.STREAMING_TRAIT)) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    /** True if {@code input} has a top-level event stream member ({@code @streaming} union). */
    public static boolean hasEventStream(Schema input) {
        for (Schema member : input.members()) {
            if (member.type() == ShapeType.UNION && member.hasTrait(TraitKey.STREAMING_TRAIT)) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code member} has {@code @requiresLength}. */
    public static boolean requiresLength(Schema member) {
        return member.hasTrait(REQUIRES_LENGTH);
    }

    /**
     * The first header that already carries a payload checksum: an {@code x-amz-checksum-*} algorithm header, an
     * {@code x-amz-trailer} declaration or {@code Content-MD5}.
     */
    public static Optional<String> presentIntegrityHeader(HttpHeaders headers) {
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            if (name.equalsIgnoreCase(CONTENT_MD5) || name.equalsIgnoreCase(X_AMZ_TRAILER)) {
                return Optional.of(name);
            }
            if (isAlgorithmHeader(name)) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    /** True if {@code name} is the checksum header of a modeled algorithm (not, for example, x-amz-checksum-mode). */
    public static boolean isAlgorithmHeader(String name) {
        for (ChecksumAlgorithm algorithm : ChecksumAlgorithm.values()) {
            if (algorithm.headerName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** The content codings of every {@code Content-Encoding} field, in order, lower case. */
    public static List<String> contentEncodings(HttpHeaders headers) {
        List<String> codings = new ArrayList<>();
        for (String value : headers.allValues(CONTENT_ENCODING)) {
            for (String coding : value.split(",")) {
                String trimmed = coding.trim();
                if (!trimmed.isEmpty()) {
                    codings.add(trimmed.toLowerCase(Locale.ROOT));
                }
            }
        }
        return codings;
    }

    /**
     * Appends {@code coding} after every coding already listed in {@code Content-Encoding} (RFC 9110 section 8.4:
     * codings in the order they were applied), as one field.
     */
    public static void appendContentEncoding(SdkHttpRequest.Builder request, HttpHeaders current, String coding) {
        List<String> values = current.allValues(CONTENT_ENCODING);
        StringBuilder combined = new StringBuilder();
        for (String value : values) {
            if (!value.isBlank()) {
                if (combined.length() > 0) {
                    combined.append(", ");
                }
                combined.append(value.trim());
            }
        }
        if (combined.length() > 0) {
            combined.append(", ");
        }
        request.putHeader(CONTENT_ENCODING, combined.append(coding).toString());
    }

    /** Sets {@code Content-Length} to {@code length}, or removes it when the length is unknown ({@code -1}). */
    public static void contentLength(SdkHttpRequest.Builder request, long length) {
        if (length < 0) {
            request.removeHeader(CONTENT_LENGTH);
        } else {
            request.putHeader(CONTENT_LENGTH, Long.toString(length));
        }
    }
}
