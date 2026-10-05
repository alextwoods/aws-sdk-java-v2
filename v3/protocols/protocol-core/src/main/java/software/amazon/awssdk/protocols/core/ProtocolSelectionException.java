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

package software.amazon.awssdk.protocols.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Client construction cannot select a protocol (Requirements 5.6, 5.12). It is thrown before a client is returned and
 * is never retryable.
 *
 * <p>Besides the message, it carries the structured facts of the failure: the {@link #reason()}, the service, the
 * requested protocol (if the caller named one), the protocols the model declares and the protocols installed, and,
 * for conflicting or failing providers, the provider class names involved. All lists are sorted.
 *
 * <p>It is a client configuration failure: a {@link SdkClientConfigurationException}.
 */
public final class ProtocolSelectionException extends SdkClientConfigurationException {
    private static final long serialVersionUID = 1L;

    private final Reason reason;
    private final ShapeId serviceId;
    private final ShapeId requested;
    private final List<ShapeId> declared;
    private final List<ShapeId> installed;
    private final List<String> providers;

    /** Why selection failed. */
    public enum Reason {
        /** The requested protocol is not declared by the service's canonical model. */
        NOT_DECLARED,
        /** The requested, declared protocol has no installed runtime. */
        NOT_INSTALLED,
        /** No protocol was requested and none of the declared protocols has an installed runtime. */
        NO_INSTALLED_DECLARED_PROTOCOL,
        /** Two or more installed providers implement the same protocol. */
        DUPLICATE_PROVIDER,
        /** Discovering or instantiating an installed provider failed. */
        DISCOVERY_FAILED,
        /** The selected runtime rejected the service's protocol data. */
        UNSUPPORTED_BY_RUNTIME
    }

    private ProtocolSelectionException(BuilderImpl builder) {
        super(builder);
        this.reason = builder.reason;
        this.serviceId = builder.serviceId;
        this.requested = builder.requested;
        this.declared = builder.declared;
        this.installed = builder.installed;
        this.providers = builder.providers;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Why selection failed. */
    public Reason reason() {
        return reason;
    }

    /** The service whose client was being constructed; empty for failures of the installed set itself. */
    public Optional<ShapeId> serviceId() {
        return Optional.ofNullable(serviceId);
    }

    /** The protocol the caller requested, if any. */
    public Optional<ShapeId> requested() {
        return Optional.ofNullable(requested);
    }

    /** The protocols the service's model declares, sorted. */
    public List<ShapeId> declared() {
        return declared;
    }

    /** The protocols with an installed runtime, sorted. */
    public List<ShapeId> installed() {
        return installed;
    }

    /** Provider class names involved in a duplicate or failed provider, sorted. */
    public List<String> providers() {
        return providers;
    }


    /** Builder of a {@link ProtocolSelectionException}; {@code reason} and {@code message} are required. */
    public interface Builder extends SdkClientConfigurationException.Builder {
        Builder reason(Reason reason);

        Builder serviceId(ShapeId serviceId);

        Builder requested(ShapeId requested);

        Builder declared(List<ShapeId> declared);

        Builder installed(List<ShapeId> installed);

        Builder providers(List<String> providers);

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
        ProtocolSelectionException build();
    }

    private static final class BuilderImpl extends SdkClientConfigurationException.BuilderImpl implements Builder {
        private Reason reason;
        private ShapeId serviceId;
        private ShapeId requested;
        private List<ShapeId> declared = List.of();
        private List<ShapeId> installed = List.of();
        private List<String> providers = List.of();

        private BuilderImpl() {
        }

        private BuilderImpl(ProtocolSelectionException ex) {
            super(ex);
            this.reason = ex.reason;
            this.serviceId = ex.serviceId;
            this.requested = ex.requested;
            this.declared = ex.declared;
            this.installed = ex.installed;
            this.providers = ex.providers;
        }

        @Override
        public Builder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        @Override
        public Builder serviceId(ShapeId serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        @Override
        public Builder requested(ShapeId requested) {
            this.requested = requested;
            return this;
        }

        @Override
        public Builder declared(List<ShapeId> declared) {
            this.declared = declared.stream().sorted().toList();
            return this;
        }

        @Override
        public Builder installed(List<ShapeId> installed) {
            this.installed = installed.stream().sorted().toList();
            return this;
        }

        @Override
        public Builder providers(List<String> providers) {
            this.providers = providers.stream().sorted().toList();
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
        public ProtocolSelectionException build() {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(message, "message");
            return new ProtocolSelectionException(this);
        }
    }
}
