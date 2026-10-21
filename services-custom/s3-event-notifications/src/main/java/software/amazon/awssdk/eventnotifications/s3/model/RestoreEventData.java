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

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * The RestoreEventData contains attributes that are related to the restore request.
 */
@SdkPublicApi
public class RestoreEventData implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.RESTORE_EVENT_DATA;
    private static final Schema M_LIFECYCLE_RESTORATION_EXPIRY_TIME = SCHEMA.member("lifecycleRestorationExpiryTime");
    private static final Schema M_LIFECYCLE_RESTORE_STORAGE_CLASS = SCHEMA.member("lifecycleRestoreStorageClass");

    private final Instant lifecycleRestorationExpiryTime;
    private final String lifecycleRestoreStorageClass;

    public RestoreEventData(String lifecycleRestorationExpiryTime, String lifecycleRestoreStorageClass) {
        this.lifecycleRestorationExpiryTime =
            lifecycleRestorationExpiryTime != null ? Instant.parse(lifecycleRestorationExpiryTime) : null;
        this.lifecycleRestoreStorageClass = lifecycleRestoreStorageClass;
    }

    /**
     * @return The time, in ISO-8601 format, for example, 1970-01-01T00:00:00.000Z, of Restore Expiry.
     */
    public Instant getLifecycleRestorationExpiryTime() {
        return lifecycleRestorationExpiryTime;
    }

    /**
     * @return The source storage class for restore.
     */
    public String getLifecycleRestoreStorageClass() {
        return lifecycleRestoreStorageClass;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes every member, as {@code null} when it is null. {@code lifecycleRestorationExpiryTime} is written in the
     * {@code ISO_INSTANT} format.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_LIFECYCLE_RESTORATION_EXPIRY_TIME, lifecycleRestorationExpiryTime);
        writeNullable(serializer, M_LIFECYCLE_RESTORE_STORAGE_CLASS, lifecycleRestoreStorageClass);
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code lifecycleRestorationExpiryTime} is returned as its {@code ISO_INSTANT} string, the member's schema type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0:
                return (T) SchemaUtils.validateSameMember(
                    M_LIFECYCLE_RESTORATION_EXPIRY_TIME, member,
                    S3EventNotificationSerde.formatInstant(lifecycleRestorationExpiryTime));
            case 1:
                return (T) SchemaUtils.validateSameMember(M_LIFECYCLE_RESTORE_STORAGE_CLASS, member,
                                                          lifecycleRestoreStorageClass);
            default:
                throw SchemaUtils.notAMember(SCHEMA, member);
        }
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
