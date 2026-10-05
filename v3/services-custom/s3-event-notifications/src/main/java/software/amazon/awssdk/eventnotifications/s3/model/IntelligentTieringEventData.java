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
 * The event data of S3 Intelligent-Tiering events.
 */
public class IntelligentTieringEventData implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.INTELLIGENT_TIERING_EVENT_DATA.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.INTELLIGENT_TIERING_EVENT_DATA;
    private static final Schema SCHEMA_DESTINATION_ACCESS_TIER = SCHEMA.member("destinationAccessTier");

    private final String destinationAccessTier;

    public IntelligentTieringEventData(String destinationAccessTier) {
        this.destinationAccessTier = destinationAccessTier;
    }

    /**
     * @return the access tier the object moved to.
     */
    public String getDestinationAccessTier() {
        return destinationAccessTier;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (destinationAccessTier != null) {
            serializer.writeString(SCHEMA_DESTINATION_ACCESS_TIER, destinationAccessTier);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_DESTINATION_ACCESS_TIER, member, destinationAccessTier);
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

        IntelligentTieringEventData that = (IntelligentTieringEventData) o;

        return Objects.equals(destinationAccessTier, that.destinationAccessTier);
    }

    @Override
    public int hashCode() {
        return destinationAccessTier != null ? destinationAccessTier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("IntelligentTieringEventDataEntity")
                       .add("destinationAccessTier", destinationAccessTier)
                       .build();
    }
}
