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

import java.util.Map;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.core.internal.exception.SdkExceptionCallMetadata;
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Base class of every exception the SDK throws, source compatible with the V2 {@code SdkException}: the same builder
 * contract, {@link #rawMessage()}, {@link #numAttempts()} and {@link #retryable()}.
 *
 * <p>When a call surfaces an exception, the AWS pipeline records the call's attempt count and safe transport facts
 * on it once, so {@link #numAttempts()} and {@link #sdkResponseMetadata()} report them (Requirement 21.1).
 */
public class SdkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    static {
        SdkExceptionCallMetadata.install(SdkException::recordCallMetadata);
    }

    // Written at most once after construction, by the pipeline's exception mapping, before the caller sees it.
    private volatile Integer attempts;
    private volatile Map<String, String> transportMetadata = Map.of();
    private volatile boolean callMetadataRecorded;

    protected SdkException(Builder builder) {
        super(messageFromBuilder(builder), builder.cause(), true, builder.writableStackTrace() == null
                                                                 || builder.writableStackTrace());
        this.attempts = builder.numAttempts();
    }

    /** The installed {@link SdkExceptionCallMetadata.Recorder}: the first recording wins. */
    private static void recordCallMetadata(SdkException exception, int attempts, Map<String, String> transport) {
        synchronized (exception) {
            if (exception.callMetadataRecorded) {
                return;
            }
            exception.callMetadataRecorded = true;
            if (attempts >= 1) {
                exception.attempts = attempts;
            }
            exception.transportMetadata = transport;
        }
    }

    /**
     * The response metadata of this failure (Requirement 21.1): the attempt count and transport facts the call
     * recorded, plus, for a service error, its request IDs, HTTP status and error code. Absent fields are empty
     * (Requirement 21.13); an exception that no call surfaced has only what its builder supplied. A new value on each
     * call; never null.
     */
    public SdkResponseMetadata sdkResponseMetadata() {
        Integer count = attempts;
        return SdkResponseMetadata.builder()
                                  .attemptCount(count == null || count < 1 ? null : count)
                                  .transportMetadata(transportMetadata)
                                  .build();
    }

    /** A new exception with {@code message} and {@code cause}. */
    public static SdkException create(String message, Throwable cause) {
        return builder().message(message).cause(cause).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /** A builder initialized with this exception's properties. */
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Number of attempts made before this exception was thrown, if known. */
    public Integer numAttempts() {
        return attempts;
    }

    /** The message given to the builder (or the cause's message), without diagnostics subclasses add. */
    public String rawMessage() {
        return super.getMessage();
    }

    /** Whether the SDK may retry the failed request; {@code false} unless a subclass says otherwise. */
    public boolean retryable() {
        return false;
    }

    private static String messageFromBuilder(Builder builder) {
        if (builder.message() != null) {
            return builder.message();
        }
        return builder.cause() == null ? null : builder.cause().getMessage();
    }

    /** Builder of an {@link SdkException}. */
    public interface Builder extends Buildable {

        Builder cause(Throwable cause);

        Throwable cause();

        Builder message(String message);

        String message();

        /** Whether the stack trace is writable; {@code null} (the default) means writable. */
        Builder writableStackTrace(Boolean writableStackTrace);

        Boolean writableStackTrace();

        Builder numAttempts(Integer numAttempts);

        Integer numAttempts();

        @Override
        SdkException build();
    }

    protected static class BuilderImpl implements Builder {

        protected Throwable cause;
        protected String message;
        protected Integer numAttempts;
        protected Boolean writableStackTrace;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkException ex) {
            this.cause = ex.getCause();
            this.message = ex.rawMessage();
            this.numAttempts = ex.numAttempts();
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Throwable cause() {
            return cause;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            this.writableStackTrace = writableStackTrace;
            return this;
        }

        @Override
        public Boolean writableStackTrace() {
            return writableStackTrace;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            this.numAttempts = numAttempts;
            return this;
        }

        @Override
        public Integer numAttempts() {
            return numAttempts;
        }

        @Override
        public SdkException build() {
            return new SdkException(this);
        }
    }
}
