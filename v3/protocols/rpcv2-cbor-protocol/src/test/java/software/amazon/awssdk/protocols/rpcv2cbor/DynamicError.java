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

package software.amazon.awssdk.protocols.rpcv2cbor;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;

/**
 * Test support: a modeled error backed by a Smithy Java {@link StructDocument}, shaped like a generated V3 error (an
 * {@link AwsServiceException} whose builder is also the schema's {@link ShapeBuilder}).
 */
final class DynamicError extends AwsServiceException implements SerializableStruct {
    private static final long serialVersionUID = 1L;

    private final transient StructDocument value;

    private DynamicError(Builder builder, StructDocument value) {
        super(builder);
        this.value = value;
    }

    static Builder builder(Schema schema) {
        return new Builder(schema);
    }

    /** The deserialized members. */
    StructDocument value() {
        return value;
    }

    @Override
    public Schema schema() {
        return value.schema();
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        value.serializeMembers(serializer);
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return value.getMemberValue(member);
    }

    static final class Builder extends AwsServiceException.BuilderImpl implements ShapeBuilder<DynamicError> {
        private final Schema schema;
        private final ShapeBuilder<StructDocument> members;

        private Builder(Schema schema) {
            this.schema = schema;
            this.members = SchemaConverter.createDocumentBuilder(schema);
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public Builder deserialize(ShapeDeserializer deserializer) {
            members.deserialize(deserializer);
            return this;
        }

        @Override
        public DynamicError build() {
            return new DynamicError(this, members.build());
        }
    }
}
