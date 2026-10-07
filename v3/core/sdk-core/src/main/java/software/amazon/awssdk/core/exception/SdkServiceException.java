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

import software.amazon.awssdk.core.SdkResponseMetadata;

/**
 * A service returned an error response. Source compatible with the V2 {@code SdkServiceException}: request IDs, the
 * HTTP status code and the clock-skew, throttling and retryable classifications, which subclasses (for example
 * generated modeled errors) refine.
 */
public class SdkServiceException extends SdkException {

    private static final long serialVersionUID = 1L;

    /** HTTP status code of a throttled request. */
    private static final int THROTTLING = 429;

    private final String requestId;
    private final String extendedRequestId;
    private final int statusCode;

    protected SdkServiceException(Builder builder) {
        super(builder);
        this.requestId = builder.requestId();
        this.extendedRequestId = builder.extendedRequestId();
        this.statusCode = builder.statusCode();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** The service's request ID, if the response carried one. */
    public String requestId() {
        return requestId;
    }

    /** The service's extended request ID, if the response carried one. */
    public String extendedRequestId() {
        return extendedRequestId;
    }

    /** HTTP status code of the response; {@code 0} if unknown, as in V2. */
    public int statusCode() {
        return statusCode;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the request ID, extended request ID and HTTP status code (absent for the V2 absent value {@code 0}).
     */
    @Override
    public SdkResponseMetadata sdkResponseMetadata() {
        return super.sdkResponseMetadata().toBuilder()
                    .requestId(requestId)
                    .extendedRequestId(extendedRequestId)
                    .statusCode(statusCode == 0 ? null : statusCode)
                    .build();
    }

    /** Whether the error indicates client clock skew. */
    public boolean isClockSkewException() {
        return false;
    }

    /** Whether the error indicates throttling; by default, whether the status code is 429. */
    public boolean isThrottlingException() {
        return statusCode == THROTTLING;
    }

    /** Whether the modeled error is retryable. */
    public boolean isRetryableException() {
        return false;
    }

    /** Builder of an {@link SdkServiceException}. */
    public interface Builder extends SdkException.Builder {

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        Builder requestId(String requestId);

        String requestId();

        Builder extendedRequestId(String extendedRequestId);

        String extendedRequestId();

        Builder statusCode(int statusCode);

        int statusCode();

        @Override
        SdkServiceException build();
    }

    protected static class BuilderImpl extends SdkException.BuilderImpl implements Builder {

        protected String requestId;
        protected String extendedRequestId;
        protected int statusCode;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkServiceException ex) {
            super(ex);
            this.requestId = ex.requestId();
            this.extendedRequestId = ex.extendedRequestId();
            this.statusCode = ex.statusCode();
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
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        @Override
        public String requestId() {
            return requestId;
        }

        @Override
        public Builder extendedRequestId(String extendedRequestId) {
            this.extendedRequestId = extendedRequestId;
            return this;
        }

        @Override
        public String extendedRequestId() {
            return extendedRequestId;
        }

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public SdkServiceException build() {
            return new SdkServiceException(this);
        }
    }
}
