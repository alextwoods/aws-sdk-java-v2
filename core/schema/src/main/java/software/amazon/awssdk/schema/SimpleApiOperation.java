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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A data-driven {@link ApiOperation}. Generated clients hold one instance per operation in a shared
 * {@code <Service>Operations} class instead of generating one class per operation.
 *
 * <p>The input and output schemas are given as suppliers and the error schemas as shape IDs, so that creating the
 * operation initializes no generated model or exception class. Each supplier is invoked on the first call that needs
 * its schema, and the error schemas are resolved through the {@linkplain #errorRegistry() error registry} on the first
 * call to {@link #errorSchemas()}. Each result is published once: when threads race on a first call, each may invoke
 * the supplier, but all of them, and every later caller, get the value that was published first. A supplier that
 * throws or returns null leaves nothing cached, so the next call tries again.
 *
 * <p>Instances are immutable apart from this caching and are safe to use from multiple threads.
 *
 * <p>SDK addition with no smithy-java counterpart: smithy-java generates one {@code ApiOperation} class per operation.
 *
 * @param <I> the operation input type
 * @param <O> the operation output type
 */
@SdkProtectedApi
public final class SimpleApiOperation<I extends SerializableStruct, O extends SerializableStruct>
    implements ApiOperation<I, O> {

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<SimpleApiOperation, Schema> INPUT_SCHEMA =
        AtomicReferenceFieldUpdater.newUpdater(SimpleApiOperation.class, Schema.class, "inputSchema");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<SimpleApiOperation, Schema> OUTPUT_SCHEMA =
        AtomicReferenceFieldUpdater.newUpdater(SimpleApiOperation.class, Schema.class, "outputSchema");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<SimpleApiOperation, List> ERROR_SCHEMAS =
        AtomicReferenceFieldUpdater.newUpdater(SimpleApiOperation.class, List.class, "errorSchemas");

    private final Schema schema;
    private final Supplier<Schema> inputSchemaSupplier;
    private final Supplier<Schema> outputSchemaSupplier;
    private final List<ShapeId> errorIds;
    private final TypeRegistry errorRegistry;
    private final List<ShapeId> effectiveAuthSchemes;
    private final ApiService service;

    private volatile Schema inputSchema;
    private volatile Schema outputSchema;
    private volatile List<Schema> errorSchemas;

    private SimpleApiOperation(Builder<I, O> builder) {
        this.schema = Validate.paramNotNull(builder.schema, "schema");
        Validate.isTrue(schema.type() == ShapeType.OPERATION, "schema must be an operation schema: %s", schema);
        this.inputSchemaSupplier = Validate.paramNotNull(builder.inputSchema, "inputSchema");
        this.outputSchemaSupplier = Validate.paramNotNull(builder.outputSchema, "outputSchema");
        this.service = Validate.paramNotNull(builder.service, "service");
        this.errorIds = builder.errorIds;
        this.errorRegistry = builder.errorRegistry;
        this.effectiveAuthSchemes = builder.effectiveAuthSchemes;
    }

    /**
     * Creates a builder for an operation.
     *
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @return a new builder
     */
    public static <I extends SerializableStruct, O extends SerializableStruct> Builder<I, O> builder() {
        return new Builder<>();
    }

    /**
     * Creates a builder for the operation input from the input schema's builder supplier.
     *
     * @throws IllegalStateException if the input schema has no builder supplier
     */
    @Override
    public ShapeBuilder<I> inputBuilder() {
        return newBuilder(inputSchema(), "input");
    }

    /**
     * Creates a builder for the operation output from the output schema's builder supplier.
     *
     * @throws IllegalStateException if the output schema has no builder supplier
     */
    @Override
    public ShapeBuilder<O> outputBuilder() {
        return newBuilder(outputSchema(), "output");
    }

    @Override
    public Schema schema() {
        return schema;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the input schema supplier returns null
     */
    @Override
    public Schema inputSchema() {
        Schema result = inputSchema;
        if (result != null) {
            return result;
        }
        Schema supplied = supplied(inputSchemaSupplier, "input");
        return INPUT_SCHEMA.compareAndSet(this, null, supplied) ? supplied : inputSchema;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the output schema supplier returns null
     */
    @Override
    public Schema outputSchema() {
        Schema result = outputSchema;
        if (result != null) {
            return result;
        }
        Schema supplied = supplied(outputSchemaSupplier, "output");
        return OUTPUT_SCHEMA.compareAndSet(this, null, supplied) ? supplied : outputSchema;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return errorRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The schemas are resolved on the first call. With an {@link ErrorRegistry}, they are the schemas its entries
     * were registered with ({@link ErrorRegistry#getSchema(ShapeId)}); with any other registry, they are the
     * {@linkplain ShapeBuilder#sdkSchema() schemas of the builders} it creates for the error shape IDs.
     *
     * @throws IllegalStateException if the error registry does not contain one of the error shape IDs
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Schema> errorSchemas() {
        List<Schema> result = errorSchemas;
        if (result != null) {
            return result;
        }
        List<Schema> resolved = resolveErrorSchemas();
        return ERROR_SCHEMAS.compareAndSet(this, null, resolved) ? resolved : errorSchemas;
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return effectiveAuthSchemes;
    }

    @Override
    public ApiService service() {
        return service;
    }

    @Override
    public String toString() {
        return ToString.builder("SimpleApiOperation").add("schema", schema.id()).build();
    }

    private Schema supplied(Supplier<Schema> supplier, String kind) {
        Schema supplied = supplier.get();
        if (supplied == null) {
            throw new IllegalStateException("The " + kind + " schema supplier of " + schema.id() + " returned null");
        }
        return supplied;
    }

    @SuppressWarnings("unchecked")
    private <T extends SerializableStruct> ShapeBuilder<T> newBuilder(Schema structSchema, String kind) {
        ShapeBuilder<?> builder = structSchema.shapeBuilder();
        if (builder == null) {
            throw new IllegalStateException("The " + kind + " schema " + structSchema.id() + " of " + schema.id()
                                            + " has no builder supplier");
        }
        return (ShapeBuilder<T>) builder;
    }

    private List<Schema> resolveErrorSchemas() {
        if (errorIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Schema> resolved = new ArrayList<>(errorIds.size());
        for (ShapeId errorId : errorIds) {
            Schema errorSchema = errorSchema(errorId);
            if (errorSchema == null) {
                throw new IllegalStateException("The error registry of " + schema.id() + " does not contain its error "
                                                + errorId);
            }
            resolved.add(errorSchema);
        }
        return Collections.unmodifiableList(resolved);
    }

    private Schema errorSchema(ShapeId errorId) {
        if (errorRegistry instanceof ErrorRegistry) {
            return ((ErrorRegistry) errorRegistry).getSchema(errorId);
        }
        ShapeBuilder<?> builder = errorRegistry.createBuilder(errorId);
        return builder == null ? null : builder.sdkSchema();
    }

    /**
     * Builds a {@link SimpleApiOperation}. {@link #schema(Schema)}, {@link #inputSchema(Supplier)},
     * {@link #outputSchema(Supplier)} and {@link #service(ApiService)} are required. By default the operation has no
     * errors, the {@linkplain TypeRegistry#empty() empty} error registry, and no effective auth schemes.
     *
     * @param <I> the operation input type
     * @param <O> the operation output type
     */
    public static final class Builder<I extends SerializableStruct, O extends SerializableStruct> {
        private Schema schema;
        private Supplier<Schema> inputSchema;
        private Supplier<Schema> outputSchema;
        private List<ShapeId> errorIds = Collections.emptyList();
        private TypeRegistry errorRegistry = TypeRegistry.empty();
        private List<ShapeId> effectiveAuthSchemes = Collections.emptyList();
        private ApiService service;

        private Builder() {
        }

        /**
         * Sets the operation schema.
         *
         * @param schema a schema of type {@link ShapeType#OPERATION}
         * @return this builder
         */
        public Builder<I, O> schema(Schema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Sets the supplier of the input structure's schema, invoked on first use.
         *
         * @param inputSchema the supplier, typically {@code () -> GetQueueUrlRequest.SCHEMA}
         * @return this builder
         */
        public Builder<I, O> inputSchema(Supplier<Schema> inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        /**
         * Sets the supplier of the output structure's schema, invoked on first use.
         *
         * @param outputSchema the supplier, typically {@code () -> GetQueueUrlResponse.SCHEMA}
         * @return this builder
         */
        public Builder<I, O> outputSchema(Supplier<Schema> outputSchema) {
            this.outputSchema = outputSchema;
            return this;
        }

        /**
         * Sets the shape IDs of the errors the operation declares, in model order. Their schemas are resolved through
         * the {@linkplain #errorRegistry(TypeRegistry) error registry} on first use.
         *
         * @param errorIds the error shape IDs
         * @return this builder
         * @throws NullPointerException if the array or any element is null
         */
        public Builder<I, O> errorIds(ShapeId... errorIds) {
            this.errorIds = copyOf(errorIds, "errorIds");
            return this;
        }

        /**
         * Sets the registry used to create the operation's errors.
         *
         * @param errorRegistry the registry, typically the service's error registry
         * @return this builder
         * @throws NullPointerException if {@code errorRegistry} is null
         */
        public Builder<I, O> errorRegistry(TypeRegistry errorRegistry) {
            this.errorRegistry = Validate.paramNotNull(errorRegistry, "errorRegistry");
            return this;
        }

        /**
         * Sets the IDs of the auth schemes that apply to the operation, in priority order.
         *
         * @param effectiveAuthSchemes the auth scheme trait IDs, for example {@code aws.auth#sigv4}
         * @return this builder
         * @throws NullPointerException if the array or any element is null
         */
        public Builder<I, O> effectiveAuthSchemes(ShapeId... effectiveAuthSchemes) {
            this.effectiveAuthSchemes = copyOf(effectiveAuthSchemes, "effectiveAuthSchemes");
            return this;
        }

        /**
         * Sets the service the operation belongs to.
         *
         * @param service the service
         * @return this builder
         */
        public Builder<I, O> service(ApiService service) {
            this.service = service;
            return this;
        }

        /**
         * Creates the operation.
         *
         * @return the operation
         * @throws NullPointerException if the schema, either schema supplier or the service is not set
         * @throws IllegalArgumentException if the schema is not an operation schema
         */
        public SimpleApiOperation<I, O> build() {
            return new SimpleApiOperation<>(this);
        }

        private static List<ShapeId> copyOf(ShapeId[] ids, String paramName) {
            Validate.paramNotNull(ids, paramName);
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == null) {
                    throw new NullPointerException(paramName + "[" + i + "] must not be null");
                }
            }
            return ids.length == 0
                   ? Collections.emptyList()
                   : Collections.unmodifiableList(new ArrayList<>(Arrays.asList(ids)));
        }
    }
}
