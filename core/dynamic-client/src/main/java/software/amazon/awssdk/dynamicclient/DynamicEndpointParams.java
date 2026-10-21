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
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The inputs of one endpoint resolution of a {@link DynamicClient} call, passed to a {@link DynamicEndpointProvider}.
 *
 * <p>A generated client has one typed parameter class per service. A dynamic client only learns the parameters of a
 * service from its model, so these parameters carry what the parameters are bound from instead: the operation being
 * called, its input, and the execution attributes of the call, which hold the client configuration such as the region,
 * the FIPS and dual-stack settings and the client context parameters.
 *
 * <p>SDK addition modeled on smithy-java {@code software.amazon.smithy.java.endpoints.EndpointResolverParams}, with the
 * call's {@link ExecutionAttributes} in place of its {@code Context}. The parameters hold the call's attributes, not a
 * copy, so they are as thread safe as the call's attributes.
 */
@SdkPublicApi
public final class DynamicEndpointParams implements ToCopyableBuilder<DynamicEndpointParams.Builder, DynamicEndpointParams> {
    private final ApiOperation<?, ?> operation;
    private final SerializableStruct input;
    private final ExecutionAttributes executionAttributes;

    private DynamicEndpointParams(BuilderImpl builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.input = Validate.paramNotNull(builder.input, "input");
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
     * Returns the operation being called.
     *
     * @return the operation
     */
    public ApiOperation<?, ?> operation() {
        return operation;
    }

    /**
     * Returns the input of the call, whose members bind the {@code smithy.rules#contextParam} and
     * {@code smithy.rules#operationContextParams} parameters.
     *
     * @return the input
     */
    public SerializableStruct input() {
        return input;
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
        DynamicEndpointParams that = (DynamicEndpointParams) o;
        return operation.equals(that.operation)
               && input.equals(that.input)
               && executionAttributes.equals(that.executionAttributes);
    }

    @Override
    public int hashCode() {
        int result = operation.hashCode();
        result = 31 * result + input.hashCode();
        result = 31 * result + executionAttributes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DynamicEndpointParams")
                       .add("operation", operation.schema().id())
                       .build();
    }

    /**
     * The builder of {@link DynamicEndpointParams}.
     */
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, DynamicEndpointParams> {
        /**
         * Sets the operation being called. Required.
         *
         * @param operation the operation
         * @return this builder
         */
        Builder operation(ApiOperation<?, ?> operation);

        /**
         * Sets the input of the call. Required.
         *
         * @param input the input
         * @return this builder
         */
        Builder input(SerializableStruct input);

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
         * @throws NullPointerException if the operation or the input is not set
         */
        @Override
        DynamicEndpointParams build();
    }

    private static final class BuilderImpl implements Builder {
        private ApiOperation<?, ?> operation;
        private SerializableStruct input;
        private ExecutionAttributes executionAttributes;

        private BuilderImpl() {
        }

        private BuilderImpl(DynamicEndpointParams params) {
            this.operation = params.operation;
            this.input = params.input;
            this.executionAttributes = params.executionAttributes;
        }

        @Override
        public Builder operation(ApiOperation<?, ?> operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public Builder input(SerializableStruct input) {
            this.input = input;
            return this;
        }

        @Override
        public Builder executionAttributes(ExecutionAttributes executionAttributes) {
            this.executionAttributes = executionAttributes;
            return this;
        }

        @Override
        public DynamicEndpointParams build() {
            return new DynamicEndpointParams(this);
        }
    }
}
