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

package software.amazon.awssdk.dynamic.model;

import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.smithy.runtime.schema.UnknownTraits;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * One operation of a runtime model, described with the same runtime types as a generated operation
 * (Requirements 14.1, 14.6, 14.17).
 *
 * <p>{@link #descriptor()} is an {@link AwsOperationDescriptor} exactly like the static descriptor a generated service
 * emits for the operation: the operation, service, input and output shape IDs, every modeled operation and service
 * error sorted by shape ID, and linked {@link OperationSchemas} (the operation schema, input, output, errors and
 * streaming members) from the service's {@code ServiceSchemaIndex}. In place of generated builders, finite inputs and
 * outputs are built as schema-guided {@link StructDocument}s ({@link #inputBuilder()}, {@link #outputBuilder()}), which
 * implement {@code SerializableStruct}; the dynamic client binds both to the shared AWS pipeline.
 *
 * <p>Instances are created once per operation by {@link DynamicService}, are immutable, hold no customer data and are
 * compared by identity.
 */
public final class DynamicAwsOperation {

    private final String name;
    private final AwsOperationDescriptor descriptor;
    private final UnknownTraits unknownTraits;
    private final Supplier<ShapeBuilder<StructDocument>> inputBuilder;
    private final Supplier<ShapeBuilder<StructDocument>> outputBuilder;

    DynamicAwsOperation(String name, AwsOperationDescriptor descriptor, UnknownTraits unknownTraits) {
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.unknownTraits = Objects.requireNonNull(unknownTraits, "unknownTraits");
        OperationSchemas schemas = descriptor.schemas().orElseThrow(
            () -> new IllegalArgumentException("The descriptor " + descriptor + " links no schemas"));
        ShapeId service = descriptor.serviceId();
        this.inputBuilder = () -> SchemaConverter.createDocumentBuilder(schemas.input(), service);
        this.outputBuilder = () -> SchemaConverter.createDocumentBuilder(schemas.output(), service);
    }

    /** The exact modeled operation name in its service (the contextual name of the operation shape). */
    public String name() {
        return name;
    }

    /** Absolute shape ID of the operation. */
    public ShapeId id() {
        return descriptor.id();
    }

    /** The operation descriptor, of the same type and with the same contents as a generated one. */
    public AwsOperationDescriptor descriptor() {
        return descriptor;
    }

    /** The operation, input, output, error and streaming member schemas. */
    public OperationSchemas schemas() {
        return descriptor.schemas().orElseThrow();
    }

    /** The unknown traits of the operation shape, keyed by exact trait ID. */
    public UnknownTraits unknownTraits() {
        return unknownTraits;
    }

    /** A supplier of fresh schema-guided builders of the operation's input. */
    public Supplier<ShapeBuilder<StructDocument>> inputBuilder() {
        return inputBuilder;
    }

    /** A supplier of fresh schema-guided builders of the operation's output. */
    public Supplier<ShapeBuilder<StructDocument>> outputBuilder() {
        return outputBuilder;
    }

    @Override
    public String toString() {
        return "DynamicAwsOperation[" + descriptor.id() + "]";
    }
}
