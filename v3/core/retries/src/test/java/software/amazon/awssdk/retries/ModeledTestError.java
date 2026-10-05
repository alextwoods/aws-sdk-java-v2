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

package software.amazon.awssdk.retries;

import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.RetryableTrait;

/** A generated-style modeled error: a service exception that is a {@link SerializableStruct} over its Schema. */
final class ModeledTestError extends SdkServiceException implements SerializableStruct {
    private static final long serialVersionUID = 1L;

    static final Schema PLAIN = Schema.structureBuilder(ShapeId.from("test#Plain"), new ErrorTrait("client")).build();
    static final Schema RETRYABLE = Schema.structureBuilder(ShapeId.from("test#Retryable"), new ErrorTrait("server"),
                                                            RetryableTrait.builder().build()).build();
    static final Schema THROTTLING = Schema.structureBuilder(ShapeId.from("test#Throttling"), new ErrorTrait("client"),
                                                             RetryableTrait.builder().throttling(true).build())
                                           .build();

    private final transient Schema schema;

    private ModeledTestError(Schema schema, int statusCode) {
        super(SdkServiceException.builder().message(schema.id().getName()).statusCode(statusCode));
        this.schema = schema;
    }

    static ModeledTestError of(Schema schema, int statusCode) {
        return new ModeledTestError(schema, statusCode);
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return null;
    }
}
