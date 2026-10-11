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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Builds a {@link SerializableShape}, populating itself from a {@link ShapeDeserializer} or member by member.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.ShapeBuilder}. smithy-java's {@code schema()} is
 * named {@link #sdkSchema()} here, for the reason given on {@link SerializableStruct}.
 *
 * @param <T> the shape to build
 */
@SdkPublicApi
public interface ShapeBuilder<T extends SerializableShape> {
    /**
     * Builds the shape.
     *
     * @return the built shape
     */
    T build();

    /**
     * Deserializes data from the given decoder into the state of the builder. Members absent from the data keep the
     * values already set on the builder.
     *
     * @param decoder the decoder used to deserialize the shape
     * @return this builder
     */
    ShapeBuilder<T> deserialize(ShapeDeserializer decoder);

    /**
     * Deserializes data into the shape using a member schema that targets the shape, so that member traits can
     * influence deserialization. The default calls {@link #deserialize(ShapeDeserializer)}.
     *
     * @param decoder the decoder used to deserialize the shape
     * @param schema the schema to use when deserializing, which must be a member that targets the shape
     * @return this builder
     */
    default ShapeBuilder<T> deserializeMember(ShapeDeserializer decoder, Schema schema) {
        return deserialize(decoder);
    }

    /**
     * Sets a member on the builder from its member schema. The default throws, for builders that support no members.
     *
     * @param member the member to set
     * @param value the value to set
     * @throws IllegalArgumentException if the member is not part of the schema
     * @throws ClassCastException if the value is not compatible with the member
     */
    default void setMemberValue(Schema member, Object value) {
        throw new IllegalArgumentException(
            "Attempted to set non-existent member of " + sdkSchema().id() + ": " + member.id());
    }

    /**
     * Performs any error correction needed before the shape can be built, for example setting a missing required
     * number member to zero. The default does nothing.
     *
     * @return this builder
     * @see <a href="https://smithy.io/2.0/spec/aggregate-types.html#client-error-correction">Client Error Correction</a>
     */
    default ShapeBuilder<T> errorCorrection() {
        return this;
    }

    /**
     * Returns the schema of the shape this builder creates.
     *
     * @return the schema of the shape
     */
    Schema sdkSchema();
}
