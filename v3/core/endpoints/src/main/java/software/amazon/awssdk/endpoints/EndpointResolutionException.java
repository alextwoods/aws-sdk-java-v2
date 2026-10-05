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

package software.amazon.awssdk.endpoints;

import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * No endpoint can be resolved for a request: the endpoint rules reached an error rule (the configuration or request
 * is invalid for the service, for example FIPS in a partition without FIPS), the parameters could not be bound, or a
 * resolved endpoint could not be completed (for example an invalid host prefix label). No request is transmitted.
 *
 * <p>{@link #ruleError()} is the exact message of the error rule the rule set reached, when that is the cause. It is
 * never retryable: resolution fails the same way until the configuration or request changes.
 */
public class EndpointResolutionException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final String ruleError;

    protected EndpointResolutionException(Builder builder) {
        super(builder);
        this.ruleError = builder.ruleError();
    }

    public static EndpointResolutionException create(String message) {
        return builder().message(message).build();
    }

    /** The failure of a rule set that reached error rule {@code ruleError}. */
    public static EndpointResolutionException ofRuleError(String ruleError) {
        return builder().message(ruleError).ruleError(ruleError).build();
    }

    /**
     * The failure of resolving the endpoint of {@code operation} with {@code failure}: its message is prefixed with
     * the operation, its {@link #ruleError()} is {@code failure}'s, and {@code failure} is its cause (Requirement
     * 13.18).
     */
    public static EndpointResolutionException forOperation(String operation, EndpointResolutionException failure) {
        return builder().message("Failed to resolve the endpoint of operation " + operation + ": "
                                 + failure.getMessage())
                        .ruleError(failure.ruleError)
                        .cause(failure)
                        .build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** The exact message of the error rule the endpoint rules reached, if an error rule is the cause. */
    public Optional<String> ruleError() {
        return Optional.ofNullable(ruleError);
    }

    @Override
    public final boolean retryable() {
        return false;
    }

    /** Builder of an {@link EndpointResolutionException}. */
    public interface Builder extends SdkClientException.Builder {
        /** The exact message of the error rule the endpoint rules reached. */
        Builder ruleError(String ruleError);

        String ruleError();

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        EndpointResolutionException build();
    }

    protected static class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        protected String ruleError;

        protected BuilderImpl() {
        }

        protected BuilderImpl(EndpointResolutionException ex) {
            super(ex);
            this.ruleError = ex.ruleError;
        }

        @Override
        public Builder ruleError(String ruleError) {
            this.ruleError = ruleError;
            return this;
        }

        @Override
        public String ruleError() {
            return ruleError;
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
        public EndpointResolutionException build() {
            return new EndpointResolutionException(this);
        }
    }
}
