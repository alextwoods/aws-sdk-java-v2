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

package software.amazon.awssdk.dynamicclient;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The inputs of one auth scheme resolution of a {@link DynamicClient} call, passed to a
 * {@link DynamicAuthSchemeResolver}: the protocol of the client, the operation being called, and the execution
 * attributes of the call, which hold the client configuration such as the region.
 *
 * <p>SDK addition modeled on smithy-java
 * {@code software.amazon.smithy.java.client.core.auth.scheme.AuthSchemeResolverParams}, with the call's
 * {@link ExecutionAttributes} in place of its {@code Context}. The parameters hold the call's attributes, not a copy, so
 * they are as thread safe as the call's attributes.
 */
@SdkPublicApi
public final class DynamicAuthSchemeParams
    implements ToCopyableBuilder<DynamicAuthSchemeParams.Builder, DynamicAuthSchemeParams> {
    private final ShapeId protocolId;
    private final ApiOperation<?, ?> operation;
    private final ExecutionAttributes executionAttributes;

    private DynamicAuthSchemeParams(BuilderImpl builder) {
        this.protocolId = Validate.paramNotNull(builder.protocolId, "protocolId");
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.executionAttributes = builder.executionAttributes != null ? builder.executionAttributes
                                                                       : new ExecutionAttributes();
    }

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Returns the ShapeId of the client's protocol, for example {@code aws.protocols#restJson1}.
     *
     * @return the protocol ID
     */
    public ShapeId protocolId() {
        return protocolId;
    }

    /**
     * Returns the operation being called. Its {@link ApiOperation#effectiveAuthSchemes()} are the modeled auth schemes
     * in priority order.
     *
     * @return the operation
     */
    public ApiOperation<?, ?> operation() {
        return operation;
    }

    /**
     * Returns the execution attributes of the call.
     *
     * @return the execution attributes; empty when none were set
     */
    public ExecutionAttributes executionAttributes() {
        return executionAttributes;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DynamicAuthSchemeParams that = (DynamicAuthSchemeParams) o;
        return protocolId.equals(that.protocolId)
               && operation.equals(that.operation)
               && executionAttributes.equals(that.executionAttributes);
    }

    @Override
    public int hashCode() {
        int result = protocolId.hashCode();
        result = 31 * result + operation.hashCode();
        result = 31 * result + executionAttributes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DynamicAuthSchemeParams")
                       .add("protocolId", protocolId)
                       .add("operation", operation.schema().id())
                       .build();
    }

    /**
     * The builder of {@link DynamicAuthSchemeParams}.
     */
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, DynamicAuthSchemeParams> {
        /**
         * Sets the ShapeId of the client's protocol. Required.
         *
         * @param protocolId the protocol ID
         * @return this builder
         */
        Builder protocolId(ShapeId protocolId);

        /**
         * Sets the operation being called. Required.
         *
         * @param operation the operation
         * @return this builder
         */
        Builder operation(ApiOperation<?, ?> operation);

        /**
         * Sets the execution attributes of the call. Defaults to empty attributes.
         *
         * @param executionAttributes the execution attributes
         * @return this builder
         */
        Builder executionAttributes(ExecutionAttributes executionAttributes);

        /**
         * Builds the parameters.
         *
         * @return the parameters
         * @throws NullPointerException if the protocol ID or the operation is not set
         */
        @Override
        DynamicAuthSchemeParams build();
    }

    private static final class BuilderImpl implements Builder {
        private ShapeId protocolId;
        private ApiOperation<?, ?> operation;
        private ExecutionAttributes executionAttributes;

        private BuilderImpl() {
        }

        private BuilderImpl(DynamicAuthSchemeParams params) {
            this.protocolId = params.protocolId;
            this.operation = params.operation;
            this.executionAttributes = params.executionAttributes;
        }

        @Override
        public Builder protocolId(ShapeId protocolId) {
            this.protocolId = protocolId;
            return this;
        }

        @Override
        public Builder operation(ApiOperation<?, ?> operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public Builder executionAttributes(ExecutionAttributes executionAttributes) {
            this.executionAttributes = executionAttributes;
            return this;
        }

        @Override
        public DynamicAuthSchemeParams build() {
            return new DynamicAuthSchemeParams(this);
        }
    }
}
