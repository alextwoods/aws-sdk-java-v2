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

package software.amazon.awssdk.http;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * A transport failure, carrying its stable {@link HttpFailureCategory} and the {@link HttpPhase} it happened in
 * (Requirement 12.7). Every sync and async transport reports failures as this type (thrown by
 * {@link SyncHttpExchange#execute()} and the response stream, or through {@link AsyncHttpResponseHandler#onError} and
 * {@link AsyncHttpExchange#completion()}), with the engine's exception as the cause, so the pipeline and retry
 * classifier never inspect engine types.
 *
 * <p>The message never contains header values, query values or body data.
 */
public final class HttpTransportException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final HttpFailureCategory category;
    private final HttpPhase phase;
    private final String clientName;

    private HttpTransportException(BuilderImpl builder) {
        super(builder);
        this.category = builder.category;
        this.phase = builder.phase;
        this.clientName = builder.clientName;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * A failure of {@code kind} in {@code phase}, categorized by {@link HttpFailureCategory#of}.
     *
     * @param cause the engine's exception, or {@code null}
     */
    public static HttpTransportException create(HttpPhase phase, HttpFailureCategory.Kind kind, String message,
                                                Throwable cause) {
        return builder().phase(phase).category(HttpFailureCategory.of(phase, kind)).message(message).cause(cause)
                        .build();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** The stable failure category. */
    public HttpFailureCategory category() {
        return category;
    }

    /** The phase the failure happened in. */
    public HttpPhase phase() {
        return phase;
    }

    /** The {@link SdkSyncHttpClient#clientName()} or {@link SdkAsyncHttpClient#clientName()}, if known. */
    public Optional<String> clientName() {
        return Optional.ofNullable(clientName);
    }

    /** {@link HttpFailureCategory#transientByDefault()} of the category. */
    @Override
    public boolean retryable() {
        return category.transientByDefault();
    }

    /** Builder of an {@link HttpTransportException}; {@code category} and {@code phase} are required. */
    public interface Builder extends SdkClientException.Builder {
        Builder category(HttpFailureCategory category);

        Builder phase(HttpPhase phase);

        Builder clientName(String clientName);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        /** @throws NullPointerException if the category or phase is not set */
        @Override
        HttpTransportException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private HttpFailureCategory category;
        private HttpPhase phase;
        private String clientName;

        private BuilderImpl() {
        }

        private BuilderImpl(HttpTransportException ex) {
            super(ex);
            this.category = ex.category;
            this.phase = ex.phase;
            this.clientName = ex.clientName;
        }

        @Override
        public Builder category(HttpFailureCategory category) {
            this.category = category;
            return this;
        }

        @Override
        public Builder phase(HttpPhase phase) {
            this.phase = phase;
            return this;
        }

        @Override
        public Builder clientName(String clientName) {
            this.clientName = clientName;
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
        public HttpTransportException build() {
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(phase, "phase");
            if (message == null) {
                message = "HTTP transport failure (" + category + " during " + phase + ")"
                          + (clientName == null ? "" : " in " + clientName) + ".";
            }
            return new HttpTransportException(this);
        }
    }
}
