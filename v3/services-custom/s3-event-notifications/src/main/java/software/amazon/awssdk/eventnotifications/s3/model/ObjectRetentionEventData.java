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
 * The event data of {@code s3:ObjectRetention:Put} events, fired on the explicit
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectRetention.html">PutObjectRetention</a>
 * API call.
 */
public class ObjectRetentionEventData implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.OBJECT_RETENTION_EVENT_DATA.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.OBJECT_RETENTION_EVENT_DATA;
    private static final Schema SCHEMA_MODE = SCHEMA.member("mode");
    private static final Schema SCHEMA_RETAIN_UNTIL_DATE = SCHEMA.member("retainUntilDate");
    private static final Schema SCHEMA_EVENT_HOLD = SCHEMA.member("eventHold");
    private static final Schema SCHEMA_EVENT_HOLD_DURATION = SCHEMA.member("eventHoldDuration");

    private final String mode;
    private final String retainUntilDate;
    private final String eventHold;
    private final EventHoldDuration eventHoldDuration;

    public ObjectRetentionEventData(
        String mode,
        String retainUntilDate,
        String eventHold,
        EventHoldDuration eventHoldDuration) {
        this.mode = mode;
        this.retainUntilDate = retainUntilDate;
        this.eventHold = eventHold;
        this.eventHoldDuration = eventHoldDuration;
    }

    /**
     * @return the retention mode, for example {@code GOVERNANCE} or {@code COMPLIANCE}.
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return the date until which the object is retained, as sent by Amazon S3.
     */
    public String getRetainUntilDate() {
        return retainUntilDate;
    }

    /**
     * @return the event hold, as sent by Amazon S3, or {@code null} if the event has none.
     */
    public String getEventHold() {
        return eventHold;
    }

    /**
     * @return the event hold duration, or {@code null} if the event has none.
     */
    public EventHoldDuration getEventHoldDuration() {
        return eventHoldDuration;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (mode != null) {
            serializer.writeString(SCHEMA_MODE, mode);
        }
        if (retainUntilDate != null) {
            serializer.writeString(SCHEMA_RETAIN_UNTIL_DATE, retainUntilDate);
        }
        if (eventHold != null) {
            serializer.writeString(SCHEMA_EVENT_HOLD, eventHold);
        }
        if (eventHoldDuration != null) {
            serializer.writeStruct(SCHEMA_EVENT_HOLD_DURATION, eventHoldDuration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_MODE, member, mode);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_RETAIN_UNTIL_DATE, member, retainUntilDate);
            case 2 -> (T) SchemaUtils.validateSameMember(SCHEMA_EVENT_HOLD, member, eventHold);
            case 3 -> (T) SchemaUtils.validateSameMember(SCHEMA_EVENT_HOLD_DURATION, member, eventHoldDuration);
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

        ObjectRetentionEventData that = (ObjectRetentionEventData) o;

        if (!Objects.equals(mode, that.mode)) {
            return false;
        }
        if (!Objects.equals(retainUntilDate, that.retainUntilDate)) {
            return false;
        }
        if (!Objects.equals(eventHold, that.eventHold)) {
            return false;
        }
        return Objects.equals(eventHoldDuration, that.eventHoldDuration);
    }

    @Override
    public int hashCode() {
        int result = mode != null ? mode.hashCode() : 0;
        result = 31 * result + (retainUntilDate != null ? retainUntilDate.hashCode() : 0);
        result = 31 * result + (eventHold != null ? eventHold.hashCode() : 0);
        result = 31 * result + (eventHoldDuration != null ? eventHoldDuration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ObjectRetentionEventData")
                       .add("mode", mode)
                       .add("retainUntilDate", retainUntilDate)
                       .add("eventHold", eventHold)
                       .add("eventHoldDuration", eventHoldDuration)
                       .build();
    }
}
