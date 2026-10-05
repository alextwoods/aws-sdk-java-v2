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

package software.amazon.awssdk.core.exception;

import java.util.List;
import java.util.Objects;

/**
 * Closing SDK-owned resources failed (Requirement 9.14). It is thrown once, after every SDK-owned resource received
 * one close attempt, and carries every close failure as a suppressed exception in close order; it has no cause. It is
 * never retryable.
 */
public final class SdkCloseException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private SdkCloseException(BuilderImpl builder) {
        super(builder);
        builder.failures.forEach(this::addSuppressed);
    }

    /**
     * The exception reporting {@code failures}, in order.
     *
     * @throws IllegalArgumentException if {@code failures} is empty
     */
    public static SdkCloseException create(List<? extends Throwable> failures) {
        return builder().failures(failures).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Every close failure, in close order (the same as {@link #getSuppressed()}). */
    public List<Throwable> failures() {
        return List.of(getSuppressed());
    }

    /** Builder of an {@link SdkCloseException}; at least one failure is required. */
    public interface Builder extends SdkClientException.Builder {
        Builder failures(List<? extends Throwable> failures);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        /** @throws IllegalArgumentException if no failure is set */
        @Override
        SdkCloseException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private List<Throwable> failures = List.of();

        private BuilderImpl() {
        }

        private BuilderImpl(SdkCloseException ex) {
            super(ex);
            this.failures = ex.failures();
        }

        @Override
        public Builder failures(List<? extends Throwable> failures) {
            this.failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
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
        public SdkCloseException build() {
            if (failures.isEmpty()) {
                throw new IllegalArgumentException("At least one close failure is required.");
            }
            if (message == null) {
                message = "Closing " + failures.size() + " SDK-owned resource(s) failed.";
            }
            return new SdkCloseException(this);
        }
    }
}
