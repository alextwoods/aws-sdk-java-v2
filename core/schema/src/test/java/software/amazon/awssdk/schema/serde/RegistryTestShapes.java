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

package software.amazon.awssdk.schema.serde;

import java.util.function.Function;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;

/**
 * Small hand-written structures shaped like generated model classes (a root schema with a shape class and a builder
 * supplier), for the type registry tests.
 */
final class RegistryTestShapes {
    static final String NS = "smithy.example";

    private RegistryTestShapes() {
    }

    /**
     * A structure with one string member, {@code name}.
     */
    abstract static class NamedStruct implements SerializableStruct {
        private final String name;

        NamedStruct(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (name != null) {
                serializer.writeString(sdkSchema().member("name"), name);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            if (member == sdkSchema().member("name")) {
                return (T) name;
            }
            throw new IllegalArgumentException("Not a member of " + sdkSchema().id() + ": " + member.id());
        }
    }

    /**
     * A builder for a {@link NamedStruct}.
     */
    static final class NamedBuilder<T extends NamedStruct> implements ShapeBuilder<T> {
        private final Schema schema;
        private final Function<String, T> factory;
        private String name;

        NamedBuilder(Schema schema, Function<String, T> factory) {
            this.schema = schema;
            this.factory = factory;
        }

        NamedBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public T build() {
            return factory.apply(name);
        }

        @Override
        public NamedBuilder<T> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, (builder, member, d) -> builder.name = d.readString(member));
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }
    }

    static final class Person extends NamedStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "Person");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("name", PreludeSchemas.STRING)
                                           .builderSupplier(Person::builder)
                                           .shapeClass(Person.class)
                                           .build();

        Person(String name) {
            super(name);
        }

        static NamedBuilder<Person> builder() {
            return new NamedBuilder<>(SCHEMA, Person::new);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }

    static final class Bird extends NamedStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "Bird");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("name", PreludeSchemas.STRING)
                                           .builderSupplier(Bird::builder)
                                           .shapeClass(Bird.class)
                                           .build();

        Bird(String name) {
            super(name);
        }

        static NamedBuilder<Bird> builder() {
            return new NamedBuilder<>(SCHEMA, Bird::new);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }

    /**
     * An error shape with a custom {@code awsQueryError} code, like SQS {@code QueueDoesNotExist}.
     */
    static final class QueueDoesNotExist extends NamedStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "QueueDoesNotExist");
        static final String CODE = "AWS.SimpleQueueService.NonExistentQueue";
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"),
                                                             new AwsQueryErrorTrait(CODE, 400))
                                           .putMember("name", PreludeSchemas.STRING)
                                           .builderSupplier(QueueDoesNotExist::builder)
                                           .shapeClass(QueueDoesNotExist.class)
                                           .build();

        QueueDoesNotExist(String name) {
            super(name);
        }

        static NamedBuilder<QueueDoesNotExist> builder() {
            return new NamedBuilder<>(SCHEMA, QueueDoesNotExist::new);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }

    /**
     * An error shape without a custom code.
     */
    static final class InternalError extends NamedStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "InternalError");
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("server"))
                                           .putMember("name", PreludeSchemas.STRING)
                                           .builderSupplier(InternalError::builder)
                                           .shapeClass(InternalError.class)
                                           .build();

        InternalError(String name) {
            super(name);
        }

        static NamedBuilder<InternalError> builder() {
            return new NamedBuilder<>(SCHEMA, InternalError::new);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }
}
