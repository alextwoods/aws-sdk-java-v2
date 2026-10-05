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

import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * A response payload does not match the checksum the service sent for it (Requirement 13.7).
 *
 * <p>For a buffered response it is raised before the modeled response is deserialized or returned. For a streaming
 * response it is raised by the read that reaches the end of the payload (or delivered as {@code onError}), before
 * end-of-stream is signalled, so a caller never observes a complete payload with an invalid checksum. The message
 * holds the algorithm and both Base64 checksum values, which are not secret. Like the V2 failure it replaces, it is
 * not marked retryable; the retry classifier decides.
 */
public final class ChecksumMismatchException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final ChecksumAlgorithm algorithm;
    private final String expected;
    private final String computed;

    private ChecksumMismatchException(BuilderImpl builder) {
        super(builder);
        this.algorithm = builder.algorithm;
        this.expected = builder.expected;
        this.computed = builder.computed;
    }

    /** The mismatch of the {@code algorithm} checksum {@code computed} over the payload and the {@code expected} one. */
    public static ChecksumMismatchException create(ChecksumAlgorithm algorithm, String expected, String computed) {
        return builder().algorithm(algorithm).expected(expected).computed(computed).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** The validated algorithm. */
    public ChecksumAlgorithm algorithm() {
        return algorithm;
    }

    /** The Base64 checksum the service sent. */
    public String expected() {
        return expected;
    }

    /** The Base64 checksum computed over the received payload. */
    public String computed() {
        return computed;
    }

    private static String describe(ChecksumAlgorithm algorithm, String expected, String computed) {
        return "The " + algorithm.algorithmId() + " checksum of the response payload is " + computed
               + ", but the service sent " + expected + " in " + algorithm.headerName();
    }

    /** Builder of a {@link ChecksumMismatchException}; algorithm, expected and computed values are required. */
    public interface Builder extends SdkClientException.Builder {
        Builder algorithm(ChecksumAlgorithm algorithm);

        Builder expected(String expected);

        Builder computed(String computed);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        ChecksumMismatchException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private ChecksumAlgorithm algorithm;
        private String expected;
        private String computed;

        private BuilderImpl() {
        }

        private BuilderImpl(ChecksumMismatchException ex) {
            super(ex);
            this.algorithm = ex.algorithm;
            this.expected = ex.expected;
            this.computed = ex.computed;
        }

        @Override
        public Builder algorithm(ChecksumAlgorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        @Override
        public Builder expected(String expected) {
            this.expected = expected;
            return this;
        }

        @Override
        public Builder computed(String computed) {
            this.computed = computed;
            return this;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            this.writableStackTrace = writableStackTrace;
            return this;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            this.numAttempts = numAttempts;
            return this;
        }

        @Override
        public ChecksumMismatchException build() {
            Objects.requireNonNull(algorithm, "algorithm");
            Objects.requireNonNull(expected, "expected");
            Objects.requireNonNull(computed, "computed");
            if (message == null) {
                message = describe(algorithm, expected, computed);
            }
            return new ChecksumMismatchException(this);
        }
    }
}
