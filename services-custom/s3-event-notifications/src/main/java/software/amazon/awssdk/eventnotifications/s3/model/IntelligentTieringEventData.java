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


import static software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde.writeNullable;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * The IntelligentTieringEventData key is only visible for
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intelligent-tiering.html">S3 Intelligent-Tiering</a>
 * related events.
 */
@SdkPublicApi
public class IntelligentTieringEventData implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.INTELLIGENT_TIERING_EVENT_DATA;
    private static final Schema M_DESTINATION_ACCESS_TIER = SCHEMA.member("destinationAccessTier");

    private final String destinationAccessTier;

    public IntelligentTieringEventData(String destinationAccessTier) {
        this.destinationAccessTier = destinationAccessTier;
    }

    public String getDestinationAccessTier() {
        return destinationAccessTier;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes {@code destinationAccessTier}, as {@code null} when it is null.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_DESTINATION_ACCESS_TIER, destinationAccessTier);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member.memberIndex() == 0) {
            return (T) SchemaUtils.validateSameMember(M_DESTINATION_ACCESS_TIER, member, destinationAccessTier);
        }
        throw SchemaUtils.notAMember(SCHEMA, member);
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
