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

package software.amazon.awssdk.awscore.operation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Immutable static descriptor of one modeled operation of a generated service.
 *
 * <p>Generated services create exactly one descriptor per operation in the service closure, as the
 * {@code static final INSTANCE} of a generated {@code <Operation>Operation} class, and expose them through a
 * generated {@code <Service>Operations} table whose constants reference each {@code INSTANCE} directly. Callers
 * always reach a descriptor through such a static reference: resolving a descriptor never loads a class by name,
 * uses reflection or service discovery, or reads a Smithy model.
 *
 * <p>A descriptor records the exact Smithy identity of the operation: its shape ID, the service it was generated for,
 * its input and output structures ({@code smithy.api#Unit} when the operation has none) and every modeled error it can
 * return in that service (operation and service errors). Generated descriptors also link the operation's
 * {@linkplain #schemas() schemas}: the operation schema and its input, output, error and streaming member schemas,
 * taken from the service's generated schema holder, whose identities the descriptor checks against its shape IDs.
 * Builder suppliers and pipeline metadata are added to this contract by the model-type and pipeline generation steps;
 * the static-reference rule above does not change.
 *
 * <p>Descriptors are singletons, so equality is identity.
 */
public final class AwsOperationDescriptor {

    private final ShapeId id;
    private final ShapeId serviceId;
    private final ShapeId inputId;
    private final ShapeId outputId;
    private final List<ShapeId> errorIds;
    private final OperationSchemas schemas;

    private AwsOperationDescriptor(Builder builder) {
        this.id = requireShapeId(builder.id, "id");
        this.serviceId = requireShapeId(builder.serviceId, "service");
        this.inputId = requireShapeId(builder.inputId, "input");
        this.outputId = requireShapeId(builder.outputId, "output");
        this.errorIds = List.copyOf(builder.errorIds);
        this.schemas = builder.schemas;
        if (schemas != null) {
            requireSame("operation", id, schemas.id());
            requireSame("input", inputId, schemas.input().id());
            requireSame("output", outputId, schemas.output().id());
            requireSame("errors", errorIds, schemas.errors().stream().map(Schema::id).toList());
        }
    }

    /** A builder for generated descriptor constants. */
    public static Builder builder() {
        return new Builder();
    }

    /** Absolute shape ID of the operation. */
    public ShapeId id() {
        return id;
    }

    /** Absolute shape ID of the service this descriptor was generated for. */
    public ShapeId serviceId() {
        return serviceId;
    }

    /** Absolute shape ID of the input structure, {@code smithy.api#Unit} if the operation has no input. */
    public ShapeId inputId() {
        return inputId;
    }

    /** Absolute shape ID of the output structure, {@code smithy.api#Unit} if the operation has no output. */
    public ShapeId outputId() {
        return outputId;
    }

    /** Absolute shape IDs of the modeled operation and service errors, in the order they were added. */
    public List<ShapeId> errorIds() {
        return errorIds;
    }

    /**
     * The operation schema and its links to the input, output, error and streaming member schemas. Every generated
     * descriptor has them; a descriptor built without them (for example in a test) returns empty.
     */
    public Optional<OperationSchemas> schemas() {
        return Optional.ofNullable(schemas);
    }

    @Override
    public String toString() {
        return "AwsOperationDescriptor[" + id + "]";
    }

    private static void requireSame(String name, Object declared, Object linked) {
        if (!declared.equals(linked)) {
            throw new IllegalArgumentException("The linked " + name + " schemas " + linked + " do not match the "
                                               + "descriptor's " + name + " " + declared);
        }
    }

    private static ShapeId requireShapeId(ShapeId value, String name) {
        Objects.requireNonNull(value, name);
        if (value.hasMember()) {
            throw new IllegalArgumentException(name + " must be a shape ID, not the member ID " + value);
        }
        return value;
    }

    /** Builder of {@link AwsOperationDescriptor}; {@code id}, {@code service}, {@code input} and {@code output} are required. */
    public static final class Builder {

        private ShapeId id;
        private ShapeId serviceId;
        private ShapeId inputId;
        private ShapeId outputId;
        private final List<ShapeId> errorIds = new ArrayList<>();
        private final Set<ShapeId> seenErrors = new HashSet<>();
        private OperationSchemas schemas;

        private Builder() {
        }

        /** Sets the operation shape ID. */
        public Builder id(ShapeId id) {
            this.id = id;
            return this;
        }

        /** Sets the service shape ID. */
        public Builder service(ShapeId serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        /** Sets the input structure shape ID. */
        public Builder input(ShapeId inputId) {
            this.inputId = inputId;
            return this;
        }

        /** Sets the output structure shape ID. */
        public Builder output(ShapeId outputId) {
            this.outputId = outputId;
            return this;
        }

        /**
         * Appends modeled error shape IDs; generated descriptors add them sorted by absolute shape ID.
         *
         * @throws IllegalArgumentException if an error is a member ID or was already added
         */
        public Builder errors(ShapeId... errors) {
            for (ShapeId error : errors) {
                requireShapeId(error, "error");
                if (!seenErrors.add(error)) {
                    throw new IllegalArgumentException("Error " + error + " is listed more than once");
                }
                errorIds.add(error);
            }
            return this;
        }

        /** Links the operation's schemas; their shape IDs must match the descriptor's, errors in the same order. */
        public Builder schemas(OperationSchemas schemas) {
            this.schemas = Objects.requireNonNull(schemas, "schemas");
            return this;
        }

        /**
         * Builds the descriptor.
         *
         * @throws NullPointerException if a required shape ID is missing
         * @throws IllegalArgumentException if the linked schemas do not match the descriptor's shape IDs
         */
        public AwsOperationDescriptor build() {
            return new AwsOperationDescriptor(this);
        }
    }
}
