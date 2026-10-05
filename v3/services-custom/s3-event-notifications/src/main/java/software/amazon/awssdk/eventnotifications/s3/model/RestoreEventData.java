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

import java.time.Instant;
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
 * The restoration information of an archived object, in {@code s3:ObjectRestore:Completed} events. For more
 * information about archive and storage classes, see
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/restoring-objects.html">Restoring an archived
 * object</a>.
 */
public class RestoreEventData implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.RESTORE_EVENT_DATA.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.RESTORE_EVENT_DATA;
    private static final Schema SCHEMA_LIFECYCLE_RESTORATION_EXPIRY_TIME =
        SCHEMA.member("lifecycleRestorationExpiryTime");
    private static final Schema SCHEMA_LIFECYCLE_RESTORE_STORAGE_CLASS = SCHEMA.member("lifecycleRestoreStorageClass");

    private final Instant lifecycleRestorationExpiryTime;
    private final String lifecycleRestoreStorageClass;

    /**
     * @param lifecycleRestorationExpiryTime the expiry time in ISO-8601 format, for example
     *        {@code 1970-01-01T00:00:00.000Z}, or {@code null}
     * @param lifecycleRestoreStorageClass the source storage class of the restored object
     * @throws java.time.format.DateTimeParseException if the expiry time is not an ISO-8601 instant
     */
    public RestoreEventData(String lifecycleRestorationExpiryTime, String lifecycleRestoreStorageClass) {
        this.lifecycleRestorationExpiryTime =
            lifecycleRestorationExpiryTime != null ? Instant.parse(lifecycleRestorationExpiryTime) : null;
        this.lifecycleRestoreStorageClass = lifecycleRestoreStorageClass;
    }

    /**
     * @return the time when the restored copy of the object expires, or {@code null}.
     */
    public Instant getLifecycleRestorationExpiryTime() {
        return lifecycleRestorationExpiryTime;
    }

    /**
     * @return the source storage class of the restored object.
     */
    public String getLifecycleRestoreStorageClass() {
        return lifecycleRestoreStorageClass;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (lifecycleRestorationExpiryTime != null) {
            serializer.writeTimestamp(SCHEMA_LIFECYCLE_RESTORATION_EXPIRY_TIME, lifecycleRestorationExpiryTime);
        }
        if (lifecycleRestoreStorageClass != null) {
            serializer.writeString(SCHEMA_LIFECYCLE_RESTORE_STORAGE_CLASS, lifecycleRestoreStorageClass);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_LIFECYCLE_RESTORATION_EXPIRY_TIME, member,
                                                         lifecycleRestorationExpiryTime);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_LIFECYCLE_RESTORE_STORAGE_CLASS, member,
                                                         lifecycleRestoreStorageClass);
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

        RestoreEventData that = (RestoreEventData) o;

        if (!Objects.equals(lifecycleRestorationExpiryTime, that.lifecycleRestorationExpiryTime)) {
            return false;
        }
        return Objects.equals(lifecycleRestoreStorageClass, that.lifecycleRestoreStorageClass);
    }

    @Override
    public int hashCode() {
        int result = lifecycleRestorationExpiryTime != null ? lifecycleRestorationExpiryTime.hashCode() : 0;
        result = 31 * result + (lifecycleRestoreStorageClass != null ? lifecycleRestoreStorageClass.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("RestoreEventData")
                       .add("lifecycleRestorationExpiryTime", lifecycleRestorationExpiryTime)
                       .add("lifecycleRestoreStorageClass", lifecycleRestoreStorageClass)
                       .build();
    }
}
