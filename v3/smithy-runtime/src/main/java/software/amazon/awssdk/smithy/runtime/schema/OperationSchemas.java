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

package software.amazon.awssdk.smithy.runtime.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The schema of one modeled operation together with exact links to the schemas the AWS pipeline and the dynamic client
 * need for it (Requirement 4.6): its input and output structures, every modeled error it can return in its service
 * (operation and service errors), and its streaming input and output members.
 *
 * <p>Smithy Java's operation {@code Schema} carries only the operation's identity and traits; the links are held here.
 * Generated services create one instance per operation, referenced from the operation's static descriptor. An
 * operation without modeled input or output links the {@code smithy.api#Unit} schema.
 *
 * <p>Instances are immutable and compared by identity.
 */
public final class OperationSchemas {

    private final Schema operation;
    private final Schema input;
    private final Schema output;
    private final List<Schema> errors;
    private final Schema inputStreamMember;
    private final Schema outputStreamMember;

    private OperationSchemas(Builder builder) {
        this.operation = builder.operation;
        this.input = requireStructure(builder.input, "input");
        this.output = requireStructure(builder.output, "output");
        this.errors = List.copyOf(builder.errors);
        this.inputStreamMember = streamMember(input, builder.inputStreamMember, "input");
        this.outputStreamMember = streamMember(output, builder.outputStreamMember, "output");
    }

    /**
     * A builder for the links of the operation described by {@code operation}.
     *
     * @throws IllegalArgumentException if {@code operation} is not an operation schema
     */
    public static Builder builder(Schema operation) {
        return new Builder(operation);
    }

    /** Absolute shape ID of the operation. */
    public ShapeId id() {
        return operation.id();
    }

    /** The operation schema. */
    public Schema operation() {
        return operation;
    }

    /** The input structure schema; the {@code smithy.api#Unit} schema if the operation has no input. */
    public Schema input() {
        return input;
    }

    /** The output structure schema; the {@code smithy.api#Unit} schema if the operation has no output. */
    public Schema output() {
        return output;
    }

    /** The schemas of the modeled operation and service errors, in the order they were added. */
    public List<Schema> errors() {
        return errors;
    }

    /** The input member that targets a streaming blob or an event stream, if any. */
    public Optional<Schema> inputStreamMember() {
        return Optional.ofNullable(inputStreamMember);
    }

    /** The output member that targets a streaming blob or an event stream, if any. */
    public Optional<Schema> outputStreamMember() {
        return Optional.ofNullable(outputStreamMember);
    }

    @Override
    public String toString() {
        return "OperationSchemas[" + operation.id() + "]";
    }

    private static Schema requireStructure(Schema schema, String name) {
        Objects.requireNonNull(schema, name);
        if (schema.isMember() || schema.type() != ShapeType.STRUCTURE) {
            throw new IllegalArgumentException("The " + name + " of an operation must be a structure schema, not "
                                               + schema.type() + " " + schema.id());
        }
        return schema;
    }

    private static Schema streamMember(Schema structure, String memberName, String name) {
        if (memberName == null) {
            return null;
        }
        Schema member = structure.member(memberName);
        if (member == null) {
            throw new IllegalArgumentException("The " + name + " stream member " + memberName + " is not a member of "
                                               + structure.id());
        }
        return member;
    }

    /** Builder of {@link OperationSchemas}; {@code input} and {@code output} are required. */
    public static final class Builder {
        private final Schema operation;
        private Schema input;
        private Schema output;
        private final List<Schema> errors = new ArrayList<>();
        private final Set<ShapeId> errorIds = new HashSet<>();
        private String inputStreamMember;
        private String outputStreamMember;

        private Builder(Schema operation) {
            Objects.requireNonNull(operation, "operation");
            if (operation.type() != ShapeType.OPERATION) {
                throw new IllegalArgumentException("Not an operation schema: " + operation.type() + " "
                                                   + operation.id());
            }
            this.operation = operation;
        }

        /** Sets the input structure schema. */
        public Builder input(Schema input) {
            this.input = input;
            return this;
        }

        /** Sets the output structure schema. */
        public Builder output(Schema output) {
            this.output = output;
            return this;
        }

        /**
         * Appends modeled error schemas; generated operations add them sorted by absolute shape ID.
         *
         * @throws IllegalArgumentException if an error is not a structure schema with the {@code smithy.api#error}
         *         trait or was already added
         */
        public Builder errors(Schema... errors) {
            for (Schema error : errors) {
                requireStructure(error, "error");
                if (!error.hasTrait(TraitKey.ERROR_TRAIT)) {
                    throw new IllegalArgumentException("Error " + error.id() + " has no smithy.api#error trait");
                }
                if (!errorIds.add(error.id())) {
                    throw new IllegalArgumentException("Error " + error.id() + " is listed more than once");
                }
                this.errors.add(error);
            }
            return this;
        }

        /** Names the input member that targets a streaming blob or an event stream. */
        public Builder inputStreamMember(String memberName) {
            this.inputStreamMember = Objects.requireNonNull(memberName, "memberName");
            return this;
        }

        /** Names the output member that targets a streaming blob or an event stream. */
        public Builder outputStreamMember(String memberName) {
            this.outputStreamMember = Objects.requireNonNull(memberName, "memberName");
            return this;
        }

        /**
         * Builds the links.
         *
         * @throws NullPointerException if the input or output schema is missing
         * @throws IllegalArgumentException if the input or output is not a structure schema, or a named stream member
         *         does not exist
         */
        public OperationSchemas build() {
            return new OperationSchemas(this);
        }
    }
}
