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

package software.amazon.awssdk.messaging.payload;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Validate;

/**
 * Thrown by the {@link MessagePayloads} deserialize methods when a message body cannot be read into a typed value.
 * {@link #cause()} tells which of the three failures occurred, and {@link #getCause()} holds the exception of the
 * codec or type registry that detected it. No partially populated value is returned.
 */
@SdkPublicApi
public final class MessagePayloadException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    private final Cause cause;

    private MessagePayloadException(BuilderImpl builder) {
        super(builder);
        this.cause = Validate.paramNotNull(builder.payloadCause, "cause");
    }

    /**
     * Creates a builder. {@link Builder#cause(Cause)} must be set before {@link Builder#build()}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Returns which of the three failures occurred.
     *
     * @return the failure cause, never null
     */
    public Cause cause() {
        return cause;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * The reason a message body could not be deserialized.
     */
    public enum Cause {
        /**
         * The body is not well-formed for the codec: for example invalid JSON, truncated CBOR, or, for a binary codec,
         * text that is not valid base64.
         */
        MALFORMED,

        /**
         * The body is well-formed, but its data does not conform to the target shape's schema: for example a string
         * where the schema has an integer.
         */
        NON_CONFORMING,

        /**
         * The concrete shape could not be resolved from the type registry: the body has no {@code __type}
         * discriminator, or one that is not a valid ShapeId, or the discriminator or the caller-supplied ShapeId is
         * not registered.
         */
        UNKNOWN_DISCRIMINATOR
    }

    /**
     * Builds a {@link MessagePayloadException}.
     */
    public interface Builder extends SdkClientException.Builder {
        /**
         * Sets which of the three failures occurred. Required.
         *
         * @param cause the failure cause
         * @return this builder
         */
        Builder cause(Cause cause);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        /**
         * Builds the exception.
         *
         * @return the exception
         * @throws NullPointerException if {@link #cause(Cause)} was not set
         */
        @Override
        MessagePayloadException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private Cause payloadCause;

        private BuilderImpl() {
        }

        private BuilderImpl(MessagePayloadException ex) {
            super(ex);
            this.payloadCause = ex.cause;
        }

        @Override
        public Builder cause(Cause cause) {
            this.payloadCause = cause;
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
        public MessagePayloadException build() {
            return new MessagePayloadException(this);
        }
    }
}
