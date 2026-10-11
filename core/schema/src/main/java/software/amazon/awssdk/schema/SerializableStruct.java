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
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A structure or union value: a {@link SerializableShape} that exposes its {@link Schema} and its member values.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SerializableStruct}. smithy-java's
 * {@code schema()} is named {@link #sdkSchema()} here, because generated SDK model classes already have public getters
 * named {@code schema()} for modeled members called {@code Schema} (for example CloudWatch {@code InsightRule}), and
 * Java cannot overload a method by return type.
 */
@SdkPublicApi
public interface SerializableStruct extends SerializableShape {
    /**
     * Returns the schema of the shape.
     *
     * @return the schema of the shape
     */
    Schema sdkSchema();

    /**
     * Writes this value with exactly one call to {@link ShapeSerializer#writeStruct(Schema, SerializableStruct)},
     * passing {@link #sdkSchema()} and this value.
     *
     * @param encoder where to serialize the shape
     */
    @Override
    default void serialize(ShapeSerializer encoder) {
        encoder.writeStruct(sdkSchema(), this);
    }

    /**
     * Writes the members of the structure or union to the serializer. Each set member is written once with its member
     * schema, in ascending member index order; unset members are not written.
     *
     * @param serializer the serializer to write to
     */
    void serializeMembers(ShapeSerializer serializer);

    /**
     * Returns the value of a member.
     *
     * @param member the member schema, which must be a member of {@link #sdkSchema()}
     * @param <T> the member's value type
     * @return the value of the member, or null if it is not set
     * @throws IllegalArgumentException if {@code member} is not a member of the shape
     */
    <T> T getMemberValue(Schema member);
}
