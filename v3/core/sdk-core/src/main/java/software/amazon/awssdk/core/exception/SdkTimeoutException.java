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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * An SDK-enforced timeout elapsed: the whole-call {@code apiCallTimeout} ({@link Scope#API_CALL}, terminal) or one
 * {@code apiCallAttemptTimeout} ({@link Scope#API_CALL_ATTEMPT}, which the retry strategy classifies).
 */
public final class SdkTimeoutException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final Scope scope;
    private final Duration timeout;
    private final Integer attempt;

    /** Which timeout elapsed. */
    public enum Scope {
        /** The whole-call timeout, across every attempt and retry delay. */
        API_CALL,
        /** One attempt's timeout. */
        API_CALL_ATTEMPT
    }

    private SdkTimeoutException(BuilderImpl builder) {
        super(builder);
        this.scope = builder.scope;
        this.timeout = builder.timeout;
        this.attempt = builder.attempt;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Which timeout elapsed. */
    public Scope scope() {
        return scope;
    }

    /** The configured timeout that elapsed, if known. */
    public Optional<Duration> timeout() {
        return Optional.ofNullable(timeout);
    }

    /** The attempt (numbered from one) that was running when the timeout elapsed, if known. */
    public OptionalInt attempt() {
        return attempt == null ? OptionalInt.empty() : OptionalInt.of(attempt);
    }

    /** Builder of an {@link SdkTimeoutException}; {@code scope} is required. */
    public interface Builder extends SdkClientException.Builder {
        Builder scope(Scope scope);

        Builder timeout(Duration timeout);

        Builder attempt(Integer attempt);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        /** @throws NullPointerException if the scope is not set */
        @Override
        SdkTimeoutException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private Scope scope;
        private Duration timeout;
        private Integer attempt;

        private BuilderImpl() {
        }

        private BuilderImpl(SdkTimeoutException ex) {
            super(ex);
            this.scope = ex.scope;
            this.timeout = ex.timeout;
            this.attempt = ex.attempt;
        }

        @Override
        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }

        @Override
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        @Override
        public Builder attempt(Integer attempt) {
            this.attempt = attempt;
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
        public SdkTimeoutException build() {
            Objects.requireNonNull(scope, "scope");
            if (message == null) {
                message = (scope == Scope.API_CALL ? "The API call" : "An API call attempt") + " timed out"
                          + (timeout == null ? "" : " after " + timeout) + ".";
            }
            return new SdkTimeoutException(this);
        }
    }
}
