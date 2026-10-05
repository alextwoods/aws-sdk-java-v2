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

import java.util.Objects;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A configured or supplied setting conflicts with what the operation's Smithy model allows, so the call is blocked
 * before any HTTP transmission (Requirement 13.18).
 *
 * <p>Examples are a request checksum algorithm the model or this runtime does not support, a caller-supplied
 * integrity header that SDK request compression would invalidate, or a {@code Content-Encoding} that collides with an
 * encoding the SDK must apply. {@link #operation()} is the absolute shape ID of the operation and {@link #setting()}
 * names the conflicting setting (a configuration option, a modeled input member, or an HTTP header). The error is not
 * retryable: every attempt would conflict the same way. Messages name settings, never their values' secrets.
 */
public final class ModelConflictException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final ShapeId operation;
    private final String setting;

    private ModelConflictException(BuilderImpl builder) {
        super(builder);
        this.operation = builder.operation;
        this.setting = builder.setting;
    }

    /**
     * The conflict of {@code setting} with the model of {@code operation}, described by {@code detail}.
     *
     * @param operation absolute shape ID of the operation
     * @param setting name of the conflicting setting
     * @param detail what conflicts and why; prefixed with the operation and setting in the message
     */
    public static ModelConflictException create(ShapeId operation, String setting, String detail) {
        return builder().operation(operation).setting(setting).message(describe(operation, setting, detail)).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Absolute shape ID of the operation whose model the setting conflicts with. */
    public ShapeId operation() {
        return operation;
    }

    /** Name of the conflicting setting: a configuration option, modeled member, or HTTP header. */
    public String setting() {
        return setting;
    }

    /** Not retryable: the conflict is the same on every attempt. */
    @Override
    public boolean retryable() {
        return false;
    }

    private static String describe(ShapeId operation, String setting, String detail) {
        return "Operation " + operation + ": setting '" + setting + "' conflicts with the model: " + detail;
    }

    /** Builder of a {@link ModelConflictException}; the operation and setting are required. */
    public interface Builder extends SdkClientException.Builder {
        Builder operation(ShapeId operation);

        Builder setting(String setting);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        /** @throws NullPointerException if the operation or setting is not set */
        @Override
        ModelConflictException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private ShapeId operation;
        private String setting;

        private BuilderImpl() {
        }

        private BuilderImpl(ModelConflictException ex) {
            super(ex);
            this.operation = ex.operation;
            this.setting = ex.setting;
        }

        @Override
        public Builder operation(ShapeId operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public Builder setting(String setting) {
            this.setting = setting;
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
        public ModelConflictException build() {
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(setting, "setting");
            if (message == null) {
                message = describe(operation, setting, "conflicting setting");
            }
            return new ModelConflictException(this);
        }
    }
}
