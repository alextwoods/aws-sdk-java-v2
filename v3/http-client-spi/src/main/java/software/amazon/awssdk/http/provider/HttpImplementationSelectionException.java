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

package software.amazon.awssdk.http.provider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * Client construction cannot select or construct an HTTP implementation (Requirements 12.11-12.14). It is thrown
 * before a client is returned, after every SDK-owned HTTP resource acquired during construction has been released, and
 * is never retryable.
 *
 * <p>Besides the message, it carries the structured facts of the failure: the {@link #reason()}, the transport
 * {@link #mode()}, the {@link #implementations() implementations} involved (sorted names) and, for
 * {@link Reason#UNSUPPORTED_OPTION}, every {@link #unsupportedOptions() unsupported option} in declaration order.
 * {@link #option()} is the V2 configuration name of the option at fault when exactly one is.
 */
public final class HttpImplementationSelectionException extends SdkClientConfigurationException {
    private static final long serialVersionUID = 1L;

    private final Reason reason;
    private final HttpTransportMode mode;
    private final List<String> implementations;
    private final List<HttpClientOption> unsupportedOptions;

    /** Why selection or construction failed. */
    public enum Reason {
        /** Both a built client and an implementation (builder) were configured explicitly. */
        EXPLICIT_CONFLICT,
        /** No explicit client or implementation was configured and none is installed for the mode. */
        NO_IMPLEMENTATION,
        /** Two or more installed implementations share the highest priority for the mode. */
        TIED_PRIORITY,
        /** The selected implementation, or the client it built, does not support a configured option. */
        UNSUPPORTED_OPTION,
        /** An implementation's metadata is invalid, or the client it built does not match it. */
        INVALID_IMPLEMENTATION,
        /** Discovering or instantiating an installed implementation failed. */
        DISCOVERY_FAILED
    }

    private HttpImplementationSelectionException(BuilderImpl builder) {
        super(builder);
        this.reason = builder.reason;
        this.mode = builder.mode;
        this.implementations = builder.implementations;
        this.unsupportedOptions = builder.unsupportedOptions;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Why selection or construction failed. */
    public Reason reason() {
        return reason;
    }

    /** The transport mode being constructed, if the failure concerns one. */
    public Optional<HttpTransportMode> mode() {
        return Optional.ofNullable(mode);
    }

    /** Names of the implementations involved, sorted; every tied one for {@link Reason#TIED_PRIORITY}. */
    public List<String> implementations() {
        return implementations;
    }

    /** The configured options the implementation does not support, in declaration order. */
    public List<HttpClientOption> unsupportedOptions() {
        return unsupportedOptions;
    }

    /** Builder of an {@link HttpImplementationSelectionException}; {@code reason} and {@code message} are required. */
    public interface Builder extends SdkClientConfigurationException.Builder {
        Builder reason(Reason reason);

        Builder mode(HttpTransportMode mode);

        Builder implementations(List<String> implementations);

        Builder unsupportedOptions(List<HttpClientOption> unsupportedOptions);

        @Override
        Builder option(String option);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        HttpImplementationSelectionException build();
    }

    private static final class BuilderImpl extends SdkClientConfigurationException.BuilderImpl implements Builder {
        private Reason reason;
        private HttpTransportMode mode;
        private List<String> implementations = List.of();
        private List<HttpClientOption> unsupportedOptions = List.of();

        private BuilderImpl() {
        }

        private BuilderImpl(HttpImplementationSelectionException ex) {
            super(ex);
            this.reason = ex.reason;
            this.mode = ex.mode;
            this.implementations = ex.implementations;
            this.unsupportedOptions = ex.unsupportedOptions;
        }

        @Override
        public Builder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        @Override
        public Builder mode(HttpTransportMode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public Builder implementations(List<String> implementations) {
            this.implementations = implementations.stream().sorted().toList();
            return this;
        }

        @Override
        public Builder unsupportedOptions(List<HttpClientOption> unsupportedOptions) {
            this.unsupportedOptions = unsupportedOptions.stream().sorted().toList();
            return this;
        }

        @Override
        public Builder option(String option) {
            this.option = option;
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
        public HttpImplementationSelectionException build() {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(message, "message");
            return new HttpImplementationSelectionException(this);
        }
    }
}
