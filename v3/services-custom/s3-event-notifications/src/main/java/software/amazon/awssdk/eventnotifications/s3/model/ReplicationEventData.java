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
 * The event data of replication events.
 */
public class ReplicationEventData implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.REPLICATION_EVENT_DATA.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.REPLICATION_EVENT_DATA;
    private static final Schema SCHEMA_REPLICATION_RULE_ID = SCHEMA.member("replicationRuleId");
    private static final Schema SCHEMA_DESTINATION_BUCKET = SCHEMA.member("destinationBucket");
    private static final Schema SCHEMA_S3_OPERATION = SCHEMA.member("s3Operation");
    private static final Schema SCHEMA_REQUEST_TIME = SCHEMA.member("requestTime");
    private static final Schema SCHEMA_FAILURE_REASON = SCHEMA.member("failureReason");
    private static final Schema SCHEMA_THRESHOLD = SCHEMA.member("threshold");
    private static final Schema SCHEMA_REPLICATION_TIME = SCHEMA.member("replicationTime");

    private final String replicationRuleId;
    private final String destinationBucket;
    private final String s3Operation;
    private final String requestTime;
    private final String failureReason;
    private final String threshold;
    private final String replicationTime;

    public ReplicationEventData(
        String replicationRuleId,
        String destinationBucket,
        String s3Operation,
        String requestTime,
        String failureReason,
        String threshold,
        String replicationTime) {
        this.replicationRuleId = replicationRuleId;
        this.destinationBucket = destinationBucket;
        this.s3Operation = s3Operation;
        this.requestTime = requestTime;
        this.failureReason = failureReason;
        this.threshold = threshold;
        this.replicationTime = replicationTime;
    }

    public String getReplicationRuleId() {
        return replicationRuleId;
    }

    public String getDestinationBucket() {
        return destinationBucket;
    }

    public String getS3Operation() {
        return s3Operation;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getThreshold() {
        return threshold;
    }

    public String getReplicationTime() {
        return replicationTime;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (replicationRuleId != null) {
            serializer.writeString(SCHEMA_REPLICATION_RULE_ID, replicationRuleId);
        }
        if (destinationBucket != null) {
            serializer.writeString(SCHEMA_DESTINATION_BUCKET, destinationBucket);
        }
        if (s3Operation != null) {
            serializer.writeString(SCHEMA_S3_OPERATION, s3Operation);
        }
        if (requestTime != null) {
            serializer.writeString(SCHEMA_REQUEST_TIME, requestTime);
        }
        if (failureReason != null) {
            serializer.writeString(SCHEMA_FAILURE_REASON, failureReason);
        }
        if (threshold != null) {
            serializer.writeString(SCHEMA_THRESHOLD, threshold);
        }
        if (replicationTime != null) {
            serializer.writeString(SCHEMA_REPLICATION_TIME, replicationTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_REPLICATION_RULE_ID, member, replicationRuleId);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_DESTINATION_BUCKET, member, destinationBucket);
            case 2 -> (T) SchemaUtils.validateSameMember(SCHEMA_S3_OPERATION, member, s3Operation);
            case 3 -> (T) SchemaUtils.validateSameMember(SCHEMA_REQUEST_TIME, member, requestTime);
            case 4 -> (T) SchemaUtils.validateSameMember(SCHEMA_FAILURE_REASON, member, failureReason);
            case 5 -> (T) SchemaUtils.validateSameMember(SCHEMA_THRESHOLD, member, threshold);
            case 6 -> (T) SchemaUtils.validateSameMember(SCHEMA_REPLICATION_TIME, member, replicationTime);
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

        ReplicationEventData that = (ReplicationEventData) o;

        if (!Objects.equals(replicationRuleId, that.replicationRuleId)) {
            return false;
        }
        if (!Objects.equals(destinationBucket, that.destinationBucket)) {
            return false;
        }
        if (!Objects.equals(s3Operation, that.s3Operation)) {
            return false;
        }
        if (!Objects.equals(requestTime, that.requestTime)) {
            return false;
        }
        if (!Objects.equals(failureReason, that.failureReason)) {
            return false;
        }
        if (!Objects.equals(threshold, that.threshold)) {
            return false;
        }
        return Objects.equals(replicationTime, that.replicationTime);
    }

    @Override
    public int hashCode() {
        int result = replicationRuleId != null ? replicationRuleId.hashCode() : 0;
        result = 31 * result + (destinationBucket != null ? destinationBucket.hashCode() : 0);
        result = 31 * result + (s3Operation != null ? s3Operation.hashCode() : 0);
        result = 31 * result + (requestTime != null ? requestTime.hashCode() : 0);
        result = 31 * result + (failureReason != null ? failureReason.hashCode() : 0);
        result = 31 * result + (threshold != null ? threshold.hashCode() : 0);
        result = 31 * result + (replicationTime != null ? replicationTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ReplicationEventData")
                       .add("replicationRuleId", replicationRuleId)
                       .add("destinationBucket", destinationBucket)
                       .add("s3Operation", s3Operation)
                       .add("requestTime", requestTime)
                       .add("failureReason", failureReason)
                       .add("threshold", threshold)
                       .add("replicationTime", replicationTime)
                       .build();
    }
}
