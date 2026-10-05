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

package software.amazon.awssdk.eventnotifications.s3.model;

import java.util.Objects;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSchemas;
import software.amazon.awssdk.eventnotifications.s3.internal.SerdeSupport;
import software.amazon.awssdk.utils.ToString;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The transition information of an S3 Lifecycle transition event.
 */
public class TransitionEventData implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.TRANSITION_EVENT_DATA.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.TRANSITION_EVENT_DATA;
    private static final Schema SCHEMA_DESTINATION_STORAGE_CLASS = SCHEMA.member("destinationStorageClass");

    private final String destinationStorageClass;

    public TransitionEventData(String destinationStorageClass) {
        this.destinationStorageClass = destinationStorageClass;
    }

    /**
     * @return the storage class the object transitioned to.
     */
    public String getDestinationStorageClass() {
        return destinationStorageClass;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (destinationStorageClass != null) {
            serializer.writeString(SCHEMA_DESTINATION_STORAGE_CLASS, destinationStorageClass);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_DESTINATION_STORAGE_CLASS, member, destinationStorageClass);
            default -> throw SerdeSupport.unknownMember(SCHEMA, member);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransitionEventData that = (TransitionEventData) o;

        return Objects.equals(destinationStorageClass, that.destinationStorageClass);
    }

    @Override
    public int hashCode() {
        return destinationStorageClass != null ? destinationStorageClass.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("TransitionEventData")
                       .add("destinationStorageClass", destinationStorageClass)
                       .build();
    }
}
