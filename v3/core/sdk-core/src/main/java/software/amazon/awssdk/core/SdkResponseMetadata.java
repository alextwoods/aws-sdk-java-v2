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

import java.io.Serializable;
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
 * Metadata of one service response, exposed by modeled responses ({@link SdkResponse#sdkResponseMetadata()}) and SDK
 * exceptions ({@link software.amazon.awssdk.core.exception.SdkException#sdkResponseMetadata()}) (Requirement 21.1).
 *
 * <p>Every field reports exactly what the call supplied. The documented absent value of each field (Requirement
 * 21.13) is an empty {@link Optional} or {@link OptionalInt}, never {@code null} and never a placeholder such as
 * V2's {@code "UNKNOWN"} (the V2-compatible {@code AwsResponseMetadata} view keeps that placeholder). Transport
 * metadata is an immutable, insertion-ordered map of safe (non-secret) facts the transport supplied, for example
 * {@value #HTTP_CLIENT} (the HTTP Implementation's name); it is empty when none.
 *
 * <p>Immutable; equal when every field is equal.
 */
public final class SdkResponseMetadata implements ToCopyableBuilder<SdkResponseMetadata.Builder, SdkResponseMetadata>,
                                                  Serializable {
    /** The transport-metadata key of the name of the HTTP Implementation that carried the call. */
    public static final String HTTP_CLIENT = "httpClient";

    private static final long serialVersionUID = 1L;
    private static final int ABSENT = -1;
    private static final SdkResponseMetadata EMPTY = new SdkResponseMetadata(new Builder());

    private final String requestId;
    private final String extendedRequestId;
    private final int statusCode;
    private final String errorCode;
    private final int attemptCount;
    private final Map<String, String> transportMetadata;

    private SdkResponseMetadata(Builder builder) {
        this.requestId = builder.requestId;
        this.extendedRequestId = builder.extendedRequestId;
        this.statusCode = builder.statusCode;
        this.errorCode = builder.errorCode;
        this.attemptCount = builder.attemptCount;
        // Copy-on-write: an unchanged map of another metadata value (or the empty map) is already immutable.
        this.transportMetadata = builder.transportOwned
                                 ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.transportMetadata))
                                 : builder.transportMetadata;
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
        return statusCode == ABSENT ? OptionalInt.empty() : OptionalInt.of(statusCode);
    }

    /** The service error code of an error response, if supplied. */
    public Optional<String> errorCode() {
        return Optional.ofNullable(errorCode);
    }

    /** Number of attempts the call made, if known. */
    public OptionalInt attemptCount() {
        return attemptCount == ABSENT ? OptionalInt.empty() : OptionalInt.of(attemptCount);
    }

    /** Safe transport facts (for example {@value #HTTP_CLIENT}), in insertion order; empty when none. Immutable. */
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
               && statusCode == that.statusCode
               && Objects.equals(errorCode, that.errorCode)
               && attemptCount == that.attemptCount
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
                       .add("statusCode", statusCode == ABSENT ? null : statusCode)
                       .add("errorCode", errorCode)
                       .add("attemptCount", attemptCount == ABSENT ? null : attemptCount)
                       .add("transportMetadata", transportMetadata.isEmpty() ? null : transportMetadata)
                       .build();
    }

    /** Returns the shared empty instance for an empty deserialized value. */
    private Object readResolve() {
        return equals(EMPTY) ? EMPTY : this;
    }

    /** Builder of an {@link SdkResponseMetadata}; unset fields are absent. Not thread safe. */
    public static final class Builder implements CopyableBuilder<Builder, SdkResponseMetadata> {
        private String requestId;
        private String extendedRequestId;
        private int statusCode = ABSENT;
        private String errorCode;
        private int attemptCount = ABSENT;
        private Map<String, String> transportMetadata = Map.of();
        /** Whether {@link #transportMetadata} is this builder's own mutable copy rather than a shared immutable map. */
        private boolean transportOwned;

        private Builder() {
        }

        private Builder(SdkResponseMetadata metadata) {
            this.requestId = metadata.requestId;
            this.extendedRequestId = metadata.extendedRequestId;
            this.statusCode = metadata.statusCode;
            this.errorCode = metadata.errorCode;
            this.attemptCount = metadata.attemptCount;
            this.transportMetadata = metadata.transportMetadata;
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
            this.statusCode = statusCode == null ? ABSENT : statusCode;
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
            this.attemptCount = attemptCount == null ? ABSENT : attemptCount;
            return this;
        }

        /** Adds (or replaces) one safe transport fact. */
        public Builder putTransportMetadata(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            if (!transportOwned) {
                transportMetadata = new LinkedHashMap<>(transportMetadata);
                transportOwned = true;
            }
            transportMetadata.put(name, value);
            return this;
        }

        /** Replaces every transport fact with {@code transportMetadata}, in its iteration order. */
        public Builder transportMetadata(Map<String, String> transportMetadata) {
            Objects.requireNonNull(transportMetadata, "transportMetadata");
            this.transportMetadata = Map.of();
            this.transportOwned = false;
            transportMetadata.forEach(this::putTransportMetadata);
            return this;
        }

        /** Replaces every transport fact with those of {@code source}, sharing its immutable map without a copy. */
        public Builder transportMetadataOf(SdkResponseMetadata source) {
            this.transportMetadata = source.transportMetadata;
            this.transportOwned = false;
            return this;
        }

        @Override
        public SdkResponseMetadata build() {
            if (requestId == null && extendedRequestId == null && statusCode == ABSENT && errorCode == null
                && attemptCount == ABSENT && transportMetadata.isEmpty()) {
                return EMPTY;
            }
            return new SdkResponseMetadata(this);
        }
    }
}
