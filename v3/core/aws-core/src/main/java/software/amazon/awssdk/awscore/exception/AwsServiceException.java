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

package software.amazon.awssdk.awscore.exception;

import java.util.StringJoiner;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.core.exception.SdkServiceException;

/**
 * An AWS service returned an error response: the base of every generated service exception
 * ({@code <Service>Exception}) and, through it, of every generated modeled error. Source compatible with the V2
 * {@code AwsServiceException}: {@link #awsErrorDetails()} and the V2 message format
 * {@code <message> (Service: <name>, Status Code: <code>, Request ID: <id>)}.
 *
 * <p>Error-code-based throttling and clock-skew classification is owned by the V3 retry and pipeline modules; this
 * class classifies throttling by status code only (see {@link SdkServiceException#isThrottlingException()}).
 */
public class AwsServiceException extends SdkServiceException {

    private static final long serialVersionUID = 1L;

    private final AwsErrorDetails awsErrorDetails;

    protected AwsServiceException(Builder builder) {
        super(builder);
        this.awsErrorDetails = builder.awsErrorDetails();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Details of the error response, if known. */
    public AwsErrorDetails awsErrorDetails() {
        return awsErrorDetails;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the service error code of {@link #awsErrorDetails()}, if any.
     */
    @Override
    public SdkResponseMetadata sdkResponseMetadata() {
        SdkResponseMetadata metadata = super.sdkResponseMetadata();
        String errorCode = awsErrorDetails == null ? null : awsErrorDetails.errorCode();
        return errorCode == null ? metadata : metadata.toBuilder().errorCode(errorCode).build();
    }

    @Override
    public String getMessage() {
        StringJoiner joiner = new StringJoiner(" ");
        String primary = rawMessage();
        if (primary == null && awsErrorDetails != null) {
            primary = awsErrorDetails.errorMessage();
        }
        if (primary != null) {
            joiner.add(primary);
        }
        if (awsErrorDetails != null) {
            StringJoiner details = new StringJoiner(", ", "(", ")");
            details.add("Service: " + awsErrorDetails.serviceName());
            details.add("Status Code: " + statusCode());
            details.add("Request ID: " + requestId());
            if (extendedRequestId() != null) {
                details.add("Extended Request ID: " + extendedRequestId());
            }
            joiner.add(details.toString());
        }
        if (numAttempts() != null) {
            joiner.add("(SDK Attempt Count: " + numAttempts() + ")");
        }
        String result = joiner.toString();
        return result.isEmpty() ? super.getMessage() : result;
    }

    /** Builder of an {@link AwsServiceException}. */
    public interface Builder extends SdkServiceException.Builder {

        Builder awsErrorDetails(AwsErrorDetails awsErrorDetails);

        AwsErrorDetails awsErrorDetails();

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        Builder requestId(String requestId);

        @Override
        Builder extendedRequestId(String extendedRequestId);

        @Override
        Builder statusCode(int statusCode);

        @Override
        AwsServiceException build();
    }

    protected static class BuilderImpl extends SdkServiceException.BuilderImpl implements Builder {

        protected AwsErrorDetails awsErrorDetails;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsServiceException ex) {
            super(ex);
            this.awsErrorDetails = ex.awsErrorDetails();
        }

        @Override
        public Builder awsErrorDetails(AwsErrorDetails awsErrorDetails) {
            this.awsErrorDetails = awsErrorDetails;
            return this;
        }

        @Override
        public AwsErrorDetails awsErrorDetails() {
            return awsErrorDetails;
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
        public Builder extendedRequestId(String extendedRequestId) {
            this.extendedRequestId = extendedRequestId;
            return this;
        }

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public AwsServiceException build() {
            return new AwsServiceException(this);
        }
    }
}
