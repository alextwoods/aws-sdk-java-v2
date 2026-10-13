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

package software.amazon.awssdk.schema;

import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.internal.OperationMembersExtension;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * A modeled Smithy operation: its schema, the schemas of its input, output and errors, the registry that creates its
 * errors, and its effective auth schemes.
 *
 * <p>Generated clients hold one instance per operation, shared by the sync and async clients, and the Dynamic_Client
 * creates them from a model. Client protocols serialize requests and deserialize responses from these schemas, and
 * the request pipeline exposes the instance of the current call to interceptors and plugins.
 *
 * <p>Implementations are immutable and safe to use from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.ApiOperation}. The member lookups of the
 * default methods are computed once per input or output schema and cached, instead of on every call.
 *
 * @param <I> the operation input type
 * @param <O> the operation output type
 */
@SdkPublicApi
public interface ApiOperation<I extends SerializableStruct, O extends SerializableStruct> {
    /**
     * Creates a builder for the operation input.
     *
     * @return a new input builder
     */
    ShapeBuilder<I> inputBuilder();

    /**
     * Creates a builder for the operation output.
     *
     * @return a new output builder
     */
    ShapeBuilder<O> outputBuilder();

    /**
     * Returns the schema of the operation.
     *
     * @return the operation schema, including the operation's runtime traits
     */
    Schema schema();

    /**
     * Returns the schema of the input structure.
     *
     * @return the input schema
     */
    Schema inputSchema();

    /**
     * Returns the schema of the output structure.
     *
     * @return the output schema
     */
    Schema outputSchema();

    /**
     * Returns the registry used to create the errors this operation can return. For generated clients this is the
     * service's single error registry, which also holds errors the operation does not declare.
     *
     * @return the error registry
     */
    TypeRegistry errorRegistry();

    /**
     * Returns the schemas of the errors the operation declares, in model order.
     *
     * @return an unmodifiable list of error schemas
     */
    List<Schema> errorSchemas();

    /**
     * Returns the IDs of the auth schemes that apply to the operation, in priority order.
     *
     * @return an unmodifiable list of auth scheme trait IDs, for example {@code aws.auth#sigv4}; empty when the
     *     operation needs no auth
     */
    List<ShapeId> effectiveAuthSchemes();

    /**
     * Returns the service the operation belongs to.
     *
     * @return the service
     */
    ApiService service();

    /**
     * Returns the input member that holds the idempotency token.
     *
     * @return the first input member with the {@code idempotencyToken} trait, or null if there is none
     */
    default Schema idempotencyTokenMember() {
        return OperationMembersExtension.of(inputSchema()).idempotencyTokenMember();
    }

    /**
     * Returns the input member that holds a streaming blob or an event stream.
     *
     * @return the first input member with the {@code streaming} trait, or null if there is none
     */
    default Schema inputStreamMember() {
        return OperationMembersExtension.of(inputSchema()).streamMember();
    }

    /**
     * Returns the output member that holds a streaming blob or an event stream.
     *
     * @return the first output member with the {@code streaming} trait, or null if there is none
     */
    default Schema outputStreamMember() {
        return OperationMembersExtension.of(outputSchema()).streamMember();
    }

    /**
     * Returns a supplier of builders for the events of the input event stream.
     *
     * @return the supplier, or null if the operation has no input event stream
     */
    default Supplier<ShapeBuilder<? extends SerializableStruct>> inputEventBuilderSupplier() {
        return null;
    }

    /**
     * Returns a supplier of builders for the events of the output event stream.
     *
     * @return the supplier, or null if the operation has no output event stream
     */
    default Supplier<ShapeBuilder<? extends SerializableStruct>> outputEventBuilderSupplier() {
        return null;
    }

    /**
     * Returns the resource the operation is bound to. An operation is bound to at most one resource, or directly to
     * the service.
     *
     * @return the resource, or null if the operation has no parent resource
     */
    default ApiResource boundResource() {
        return null;
    }

    /**
     * Returns the name of the operation.
     *
     * @return the shape name of the operation's shape ID, for example {@code GetQueueUrl}
     */
    default String name() {
        return schema().id().getName();
    }
}
