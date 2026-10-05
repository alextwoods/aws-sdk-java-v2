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

/**
 * A failure detected on the client, without (or before) a service response: for example an invalid request or model
 * value, or a transport failure. Source compatible with the V2 {@code SdkClientException}.
 */
public class SdkClientException extends SdkException {

    private static final long serialVersionUID = 1L;

    protected SdkClientException(Builder builder) {
        super(builder);
    }

    public static SdkClientException create(String message) {
        return builder().message(message).build();
    }

    public static SdkClientException create(String message, Throwable cause) {
        return builder().message(message).cause(cause).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** The raw message followed by the attempt count, when known. */
    @Override
    public String getMessage() {
        String message = rawMessage();
        return numAttempts() == null ? message : message + " (SDK Attempt Count: " + numAttempts() + ")";
    }

    /** Builder of an {@link SdkClientException}. */
    public interface Builder extends SdkException.Builder {

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        SdkClientException build();
    }

    protected static class BuilderImpl extends SdkException.BuilderImpl implements Builder {

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkClientException ex) {
            super(ex);
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
        public SdkClientException build() {
            return new SdkClientException(this);
        }
    }
}
