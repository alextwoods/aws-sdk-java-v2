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

package software.amazon.awssdk.pipeline;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The typed pipeline binding of one modeled operation: its static {@link AwsOperationDescriptor}, the generated
 * builders of its input and output types, and the per-operation metadata the pipeline plans from (endpoint binding,
 * modeled auth, {@code httpChecksum} trait, long polling) and, for inputs that are not generated types, the view of a
 * request the protocol serializes.
 *
 * <p>Generated services create one instance per operation as a {@code static final} constant next to the
 * descriptor, and the dynamic client creates one per runtime operation; a client reaches it only through that direct
 * reference. {@link ClientRuntime#plan(AwsOperation)} builds exactly one {@link PipelinePlan} per client and
 * operation from it: no class-name lookup, reflection or model parsing is involved (Requirements 10.11, 10.12).
 *
 * <p>Immutable, holds no customer data, and compared by identity.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public final class AwsOperation<I extends SerializableStruct, O extends SerializableStruct> {
    private final AwsOperationDescriptor descriptor;
    private final OperationSchemas schemas;
    private final Supplier<? extends ShapeBuilder<I>> inputBuilder;
    private final Supplier<? extends ShapeBuilder<O>> outputBuilder;
    private final EndpointBinding endpointBinding;
    private final ModeledAuth auth;
    private final HttpChecksumMetadata httpChecksum;
    private final boolean longPolling;
    private final Function<? super I, ? extends SerializableStruct> requestView;

    private AwsOperation(Builder<I, O> builder) {
        this.descriptor = builder.descriptor;
        this.schemas = builder.descriptor.schemas().orElseThrow(
            () -> new IllegalArgumentException("The descriptor " + builder.descriptor + " links no schemas"));
        this.inputBuilder = builder.inputBuilder;
        this.outputBuilder = builder.outputBuilder;
        this.endpointBinding = builder.endpointBinding;
        this.auth = builder.auth;
        this.httpChecksum = builder.httpChecksum;
        this.longPolling = builder.longPolling;
        this.requestView = builder.requestView;
    }

    /**
     * A builder of the binding of {@code descriptor}, whose modeled input and output are built by the given generated
     * builder suppliers.
     *
     * @throws IllegalArgumentException from {@link Builder#build()} if the descriptor links no schemas
     */
    public static <I extends SerializableStruct, O extends SerializableStruct> Builder<I, O> builder(
        AwsOperationDescriptor descriptor, Supplier<? extends ShapeBuilder<I>> inputBuilder,
        Supplier<? extends ShapeBuilder<O>> outputBuilder) {
        return new Builder<>(descriptor, inputBuilder, outputBuilder);
    }

    /** Absolute shape ID of the operation. */
    public ShapeId id() {
        return descriptor.id();
    }

    /** The operation's static descriptor. */
    public AwsOperationDescriptor descriptor() {
        return descriptor;
    }

    /** The operation, input, output, error and stream member schemas. */
    public OperationSchemas schemas() {
        return schemas;
    }

    /** A supplier of fresh builders of the operation's input type. */
    public Supplier<? extends ShapeBuilder<I>> inputBuilder() {
        return inputBuilder;
    }

    /** A supplier of fresh builders of the operation's output type. */
    public Supplier<? extends ShapeBuilder<O>> outputBuilder() {
        return outputBuilder;
    }

    /** The operation's endpoint parameter binding; {@link EndpointBinding#none()} when it binds nothing. */
    public EndpointBinding endpointBinding() {
        return endpointBinding;
    }

    /** The operation's modeled auth traits, if recorded. */
    public Optional<ModeledAuth> auth() {
        return Optional.ofNullable(auth);
    }

    /** The operation's {@code aws.protocols#httpChecksum} trait value, if it has one. */
    public Optional<HttpChecksumMetadata> httpChecksum() {
        return Optional.ofNullable(httpChecksum);
    }

    /** Whether the operation is a long-polling operation for retry-quota purposes. */
    public boolean longPolling() {
        return longPolling;
    }

    /**
     * The view of {@code request} the protocol serializes: {@code request} itself unless a
     * {@linkplain Builder#requestView request view} is set.
     */
    public SerializableStruct serializableRequest(I request) {
        return requestView == null ? request : Objects.requireNonNull(requestView.apply(request), "requestView");
    }

    @Override
    public String toString() {
        return "AwsOperation[" + descriptor.id() + "]";
    }

    /** Builder of an {@link AwsOperation}. */
    public static final class Builder<I extends SerializableStruct, O extends SerializableStruct> {
        private final AwsOperationDescriptor descriptor;
        private final Supplier<? extends ShapeBuilder<I>> inputBuilder;
        private final Supplier<? extends ShapeBuilder<O>> outputBuilder;
        private EndpointBinding endpointBinding = EndpointBinding.none();
        private ModeledAuth auth;
        private HttpChecksumMetadata httpChecksum;
        private boolean longPolling;
        private Function<? super I, ? extends SerializableStruct> requestView;

        private Builder(AwsOperationDescriptor descriptor, Supplier<? extends ShapeBuilder<I>> inputBuilder,
                        Supplier<? extends ShapeBuilder<O>> outputBuilder) {
            this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
            this.inputBuilder = Objects.requireNonNull(inputBuilder, "inputBuilder");
            this.outputBuilder = Objects.requireNonNull(outputBuilder, "outputBuilder");
        }

        /** Sets the operation's endpoint parameter binding. */
        public Builder<I, O> endpointBinding(EndpointBinding endpointBinding) {
            this.endpointBinding = Objects.requireNonNull(endpointBinding, "endpointBinding");
            return this;
        }

        /** Sets the operation's modeled auth traits; {@code null} means none are recorded. */
        public Builder<I, O> auth(ModeledAuth auth) {
            this.auth = auth;
            return this;
        }

        /** Sets the operation's {@code httpChecksum} trait value; {@code null} means it has none. */
        public Builder<I, O> httpChecksum(HttpChecksumMetadata httpChecksum) {
            this.httpChecksum = httpChecksum;
            return this;
        }

        /** Marks the operation as long polling. */
        public Builder<I, O> longPolling(boolean longPolling) {
            this.longPolling = longPolling;
            return this;
        }

        /**
         * Sets the view of each request the protocol serializes in place of the request itself; {@code null} (the
         * default) serializes the request. Generated inputs need none. The dynamic client sets one so its
         * schema-guided documents write structure and union members in modeled declaration order, as generated
         * types do; a view must serialize exactly the request's member values and expose them through
         * {@link SerializableStruct#getMemberValue}.
         */
        public Builder<I, O> requestView(Function<? super I, ? extends SerializableStruct> requestView) {
            this.requestView = requestView;
            return this;
        }

        /**
         * Builds the binding.
         *
         * @throws IllegalArgumentException if the descriptor links no schemas
         */
        public AwsOperation<I, O> build() {
            return new AwsOperation<>(this);
        }
    }
}
