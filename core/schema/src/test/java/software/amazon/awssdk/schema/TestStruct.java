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

import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A small hand-written structure with one string member, shaped like a generated model class, for testing the serde
 * interfaces' default methods.
 */
final class TestStruct implements SerializableStruct {
    public static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts("com.example", "TestStruct"))
                                              .putMember("name", PreludeSchemas.STRING)
                                              .builderSupplier(TestStruct::builder)
                                              .shapeClass(TestStruct.class)
                                              .build();
    public static final Schema NAME = SCHEMA.member("name");

    private final String name;

    private TestStruct(Builder builder) {
        this.name = builder.name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (name != null) {
            serializer.writeString(NAME, name);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member == NAME) {
            return (T) name;
        }
        throw new IllegalArgumentException("Not a member of " + SCHEMA.id() + ": " + member.id());
    }

    /**
     * Builder that implements only the abstract {@link ShapeBuilder} methods, so the defaults stay in effect.
     */
    public static final class Builder implements ShapeBuilder<TestStruct> {
        private String name;
        private int deserializeCalls;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public int deserializeCalls() {
            return deserializeCalls;
        }

        @Override
        public TestStruct build() {
            return new TestStruct(this);
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            deserializeCalls++;
            decoder.readStruct(SCHEMA, this, (builder, member, d) -> builder.name = d.readString(member));
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }
}
