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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * The GlacierEventData is only visible for s3:ObjectRestore:Completed events.
 * Contains information related to restoring an archived object. For more information about archive and storage classes, see
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/restoring-objects.html">Restoring an archived object</a>
 */
@SdkPublicApi
public class GlacierEventData implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.GLACIER_EVENT_DATA;
    private static final Schema M_RESTORE_EVENT_DATA = SCHEMA.member("restoreEventData");

    private final RestoreEventData restoreEventData;

    public GlacierEventData(RestoreEventData restoreEventData) {
        this.restoreEventData = restoreEventData;
    }

    public RestoreEventData getRestoreEventData() {
        return restoreEventData;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes {@code restoreEventData} only when it is set.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (restoreEventData != null) {
            serializer.writeStruct(M_RESTORE_EVENT_DATA, restoreEventData);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member.memberIndex() == 0) {
            return (T) SchemaUtils.validateSameMember(M_RESTORE_EVENT_DATA, member, restoreEventData);
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

        GlacierEventData that = (GlacierEventData) o;

        return Objects.equals(restoreEventData, that.restoreEventData);
    }

    @Override
    public int hashCode() {
        return restoreEventData != null ? restoreEventData.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("GlacierEventDataEntity")
                       .add("restoreEventData", restoreEventData)
                       .build();
    }
}
