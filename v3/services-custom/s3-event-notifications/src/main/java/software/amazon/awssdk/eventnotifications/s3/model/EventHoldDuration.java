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
 * The duration of an object retention event hold: a single unit and value, for example 90 {@code days}
 * or 1 {@code years}, sent by Amazon S3 as the JSON object {@code {"<unit>": <value>}}.
 */
public class EventHoldDuration implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.EVENT_HOLD_DURATION.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.EVENT_HOLD_DURATION;
    private static final Schema SCHEMA_UNIT = SCHEMA.member("unit");
    private static final Schema SCHEMA_VALUE = SCHEMA.member("value");

    private final String unit;
    private final Long value;

    public EventHoldDuration(String unit, Long value) {
        this.unit = unit;
        this.value = value;
    }

    /**
     * @return the unit of the duration, as sent by Amazon S3, for example {@code days}.
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @return the number of units.
     */
    public Long getValue() {
        return value;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (unit != null) {
            serializer.writeString(SCHEMA_UNIT, unit);
        }
        if (value != null) {
            serializer.writeLong(SCHEMA_VALUE, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_UNIT, member, unit);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_VALUE, member, value);
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

        EventHoldDuration that = (EventHoldDuration) o;

        if (!Objects.equals(unit, that.unit)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = unit != null ? unit.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("EventHoldDuration")
                       .add("unit", unit)
                       .add("value", value)
                       .build();
    }
}
