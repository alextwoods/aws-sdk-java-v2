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

package software.amazon.awssdk.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Metadata of one service response, exposed by modeled responses and SDK exceptions (Requirement 21.1).
 *
 * <p>Every field reports exactly what the response supplied: an unavailable field is an empty {@link Optional} or
 * {@link OptionalInt} (Requirement 21.13), never {@code null} and never a placeholder such as V2's {@code "UNKNOWN"}.
 * Transport metadata is an immutable, insertion-ordered map of safe (non-secret) transport facts, empty when none.
 *
 * <p>Immutable; equal when every field is equal.
 */
public final class SdkResponseMetadata implements ToCopyableBuilder<SdkResponseMetadata.Builder, SdkResponseMetadata> {
    private static final SdkResponseMetadata EMPTY = new SdkResponseMetadata(new Builder());

    private final String requestId;
    private final String extendedRequestId;
    private final Integer statusCode;
    private final String errorCode;
    private final Integer attemptCount;
    private final Map<String, String> transportMetadata;

    private SdkResponseMetadata(Builder builder) {
        this.requestId = builder.requestId;
        this.extendedRequestId = builder.extendedRequestId;
        this.statusCode = builder.statusCode;
        this.errorCode = builder.errorCode;
        this.attemptCount = builder.attemptCount;
        this.transportMetadata = builder.transportMetadata.isEmpty()
                                 ? Map.of()
                                 : Collections.unmodifiableMap(new LinkedHashMap<>(builder.transportMetadata));
    }

    /** Metadata with no supplied field. */
    public static SdkResponseMetadata empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The service's request ID, if supplied. */
    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }

    /** The service's extended request ID (for example S3's {@code x-amz-id-2}), if supplied. */
    public Optional<String> extendedRequestId() {
        return Optional.ofNullable(extendedRequestId);
    }

    /** HTTP status code of the response, if a response was received. */
    public OptionalInt statusCode() {
        return statusCode == null ? OptionalInt.empty() : OptionalInt.of(statusCode);
    }

    /** The service error code of an error response, if supplied. */
    public Optional<String> errorCode() {
        return Optional.ofNullable(errorCode);
    }

    /** Number of attempts made by the call, if known. */
    public OptionalInt attemptCount() {
        return attemptCount == null ? OptionalInt.empty() : OptionalInt.of(attemptCount);
    }

    /** Safe transport facts (for example the negotiated protocol), in insertion order; empty when none. */
    public Map<String, String> transportMetadata() {
        return transportMetadata;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SdkResponseMetadata that
               && Objects.equals(requestId, that.requestId)
               && Objects.equals(extendedRequestId, that.extendedRequestId)
               && Objects.equals(statusCode, that.statusCode)
               && Objects.equals(errorCode, that.errorCode)
               && Objects.equals(attemptCount, that.attemptCount)
               && transportMetadata.equals(that.transportMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, extendedRequestId, statusCode, errorCode, attemptCount, transportMetadata);
    }

    @Override
    public String toString() {
        return ToString.builder("SdkResponseMetadata")
                       .add("requestId", requestId)
                       .add("extendedRequestId", extendedRequestId)
                       .add("statusCode", statusCode)
                       .add("errorCode", errorCode)
                       .add("attemptCount", attemptCount)
                       .add("transportMetadata", transportMetadata.isEmpty() ? null : transportMetadata)
                       .build();
    }

    /** Builder of an {@link SdkResponseMetadata}; unset fields are absent. */
    public static final class Builder implements CopyableBuilder<Builder, SdkResponseMetadata> {
        private String requestId;
        private String extendedRequestId;
        private Integer statusCode;
        private String errorCode;
        private Integer attemptCount;
        private final Map<String, String> transportMetadata = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(SdkResponseMetadata metadata) {
            this.requestId = metadata.requestId;
            this.extendedRequestId = metadata.extendedRequestId;
            this.statusCode = metadata.statusCode;
            this.errorCode = metadata.errorCode;
            this.attemptCount = metadata.attemptCount;
            this.transportMetadata.putAll(metadata.transportMetadata);
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder extendedRequestId(String extendedRequestId) {
            this.extendedRequestId = extendedRequestId;
            return this;
        }

        /** The HTTP status code; {@code null} means no response was received. */
        public Builder statusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * The number of attempts; {@code null} means unknown.
         *
         * @throws IllegalArgumentException if the count is less than one
         */
        public Builder attemptCount(Integer attemptCount) {
            if (attemptCount != null && attemptCount < 1) {
                throw new IllegalArgumentException("attemptCount must be at least 1, but was " + attemptCount + ".");
            }
            this.attemptCount = attemptCount;
            return this;
        }

        /** Adds (or replaces) one safe transport fact. */
        public Builder putTransportMetadata(String name, String value) {
            transportMetadata.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Replaces every transport fact with {@code transportMetadata}, in its iteration order. */
        public Builder transportMetadata(Map<String, String> transportMetadata) {
            this.transportMetadata.clear();
            transportMetadata.forEach(this::putTransportMetadata);
            return this;
        }

        @Override
        public SdkResponseMetadata build() {
            if (requestId == null && extendedRequestId == null && statusCode == null && errorCode == null
                && attemptCount == null && transportMetadata.isEmpty()) {
                return EMPTY;
            }
            return new SdkResponseMetadata(this);
        }
    }
}
