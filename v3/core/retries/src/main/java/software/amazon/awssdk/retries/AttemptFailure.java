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

package software.amazon.awssdk.retries;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.ToString;

/**
 * Everything the retry owner knows about one failed attempt: the failure, the response metadata (status code, error
 * code, retry-delay hint), the modeled retry traits and whether the request can be sent again.
 *
 * <p>Unset metadata is derived where possible: the status code from an {@link SdkServiceException} failure, and the
 * modeled traits from a generated error's Schema ({@link ModeledRetryTraits#of(Throwable)}).
 */
public final class AttemptFailure {
    private final Throwable failure;
    private final int statusCode;
    private final String errorCode;
    private final ModeledRetryTraits modeledTraits;
    private final String retryAfterHint;
    private final boolean requestReplayable;

    private AttemptFailure(Builder builder) {
        this.failure = Objects.requireNonNull(builder.failure, "failure");
        this.statusCode = builder.statusCode != 0 ? builder.statusCode
                                                  : failure instanceof SdkServiceException service
                                                    ? service.statusCode() : 0;
        this.errorCode = builder.errorCode;
        this.modeledTraits = builder.modeledTraits != null ? builder.modeledTraits : ModeledRetryTraits.of(failure);
        this.retryAfterHint = builder.retryAfterHint;
        this.requestReplayable = builder.requestReplayable;
    }

    /** A failure with no response metadata beyond what {@code failure} carries. */
    public static AttemptFailure of(Throwable failure) {
        return builder().failure(failure).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The failure of the attempt, as thrown by the pipeline stage that failed. */
    public Throwable failure() {
        return failure;
    }

    /** The HTTP status code of the response, if the attempt received one. */
    public OptionalInt statusCode() {
        return statusCode == 0 ? OptionalInt.empty() : OptionalInt.of(statusCode);
    }

    /** The protocol-decoded AWS error code of the response, if any. */
    public Optional<String> errorCode() {
        return Optional.ofNullable(errorCode);
    }

    /** The retry traits of the modeled error; {@link ModeledRetryTraits#NONE} for an unmodeled failure. */
    public ModeledRetryTraits modeledTraits() {
        return modeledTraits;
    }

    /** The raw {@value RetryHints#X_AMZ_RETRY_AFTER} value of the response, if present; validated when used. */
    public Optional<String> retryAfterHint() {
        return Optional.ofNullable(retryAfterHint);
    }

    /**
     * Whether the request can be sent again. {@code false} when a non-replayable request body was (possibly
     * partially) consumed by this attempt; such a failure is never retried.
     */
    public boolean requestReplayable() {
        return requestReplayable;
    }

    @Override
    public String toString() {
        // No header or body values beyond the bounded hint; the failure is summarized by type.
        return ToString.builder("AttemptFailure")
                       .add("failure", failure.getClass().getName())
                       .add("statusCode", statusCode == 0 ? null : statusCode)
                       .add("errorCode", errorCode)
                       .add("modeledTraits", modeledTraits)
                       .add("retryAfterHint", retryAfterHint)
                       .add("requestReplayable", requestReplayable)
                       .build();
    }

    /** Builder of an {@link AttemptFailure}; {@code failure} is required. */
    public static final class Builder {
        private Throwable failure;
        private int statusCode;
        private String errorCode;
        private ModeledRetryTraits modeledTraits;
        private String retryAfterHint;
        private boolean requestReplayable = true;

        private Builder() {
        }

        public Builder failure(Throwable failure) {
            this.failure = failure;
            return this;
        }

        /** The HTTP status code; {@code 0} (the default) means none. */
        public Builder statusCode(int statusCode) {
            if (statusCode != 0 && (statusCode < 100 || statusCode > 999)) {
                throw new IllegalArgumentException("Not an HTTP status code: " + statusCode);
            }
            this.statusCode = statusCode;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /** Overrides the traits derived from the failure; {@code null} restores derivation. */
        public Builder modeledTraits(ModeledRetryTraits modeledTraits) {
            this.modeledTraits = modeledTraits;
            return this;
        }

        /** The raw {@value RetryHints#X_AMZ_RETRY_AFTER} header value, or {@code null}. */
        public Builder retryAfterHint(String retryAfterHint) {
            this.retryAfterHint = retryAfterHint;
            return this;
        }

        /** The status code and {@value RetryHints#X_AMZ_RETRY_AFTER} hint of {@code response}. */
        public Builder response(SdkHttpResponse response) {
            Objects.requireNonNull(response, "response");
            statusCode(response.statusCode());
            this.retryAfterHint = response.headers().firstValue(RetryHints.X_AMZ_RETRY_AFTER).orElse(null);
            return this;
        }

        /** Whether the request can be sent again; default {@code true}. */
        public Builder requestReplayable(boolean requestReplayable) {
            this.requestReplayable = requestReplayable;
            return this;
        }

        /** @throws NullPointerException if the failure is not set */
        public AttemptFailure build() {
            return new AttemptFailure(this);
        }
    }
}
