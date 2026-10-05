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

import java.util.Optional;

/**
 * A client cannot be constructed from its configuration: an unsupported or invalid option, an incompatible component,
 * or a capability or runtime-version conflict (Requirement 9.9). It is thrown before a client is returned, so no call
 * exists, and it is never retryable.
 *
 * <p>{@link #option()} names the offending configuration option when one option is at fault. Subtypes (for example
 * protocol selection failures) add structured facts.
 */
public class SdkClientConfigurationException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final String option;

    protected SdkClientConfigurationException(Builder builder) {
        super(builder);
        this.option = builder.option();
    }

    public static SdkClientConfigurationException create(String message) {
        return builder().message(message).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Name of the configuration option at fault, if one is. */
    public Optional<String> option() {
        return Optional.ofNullable(option);
    }

    /** Not retryable: construction fails the same way until the configuration changes. */
    @Override
    public final boolean retryable() {
        return false;
    }

    /** Builder of an {@link SdkClientConfigurationException}. */
    public interface Builder extends SdkClientException.Builder {
        /** Name of the configuration option at fault. */
        Builder option(String option);

        String option();

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        SdkClientConfigurationException build();
    }

    protected static class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        protected String option;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkClientConfigurationException ex) {
            super(ex);
            this.option = ex.option;
        }

        @Override
        public Builder option(String option) {
            this.option = option;
            return this;
        }

        @Override
        public String option() {
            return option;
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
        public SdkClientConfigurationException build() {
            return new SdkClientConfigurationException(this);
        }
    }
}
