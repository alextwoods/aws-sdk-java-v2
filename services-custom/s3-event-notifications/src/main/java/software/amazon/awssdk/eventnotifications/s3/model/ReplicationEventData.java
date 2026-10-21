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
 * The ReplicationEventData is only visible for
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/replication.html">replication</a> related events.
 */
@SdkPublicApi
public class ReplicationEventData implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.REPLICATION_EVENT_DATA;
    private static final Schema M_REPLICATION_RULE_ID = SCHEMA.member("replicationRuleId");
    private static final Schema M_DESTINATION_BUCKET = SCHEMA.member("destinationBucket");
    private static final Schema M_S3_OPERATION = SCHEMA.member("s3Operation");
    private static final Schema M_REQUEST_TIME = SCHEMA.member("requestTime");
    private static final Schema M_FAILURE_REASON = SCHEMA.member("failureReason");
    private static final Schema M_THRESHOLD = SCHEMA.member("threshold");
    private static final Schema M_REPLICATION_TIME = SCHEMA.member("replicationTime");

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
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes every member, as {@code null} when it is null.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_REPLICATION_RULE_ID, replicationRuleId);
        writeNullable(serializer, M_DESTINATION_BUCKET, destinationBucket);
        writeNullable(serializer, M_S3_OPERATION, s3Operation);
        writeNullable(serializer, M_REQUEST_TIME, requestTime);
        writeNullable(serializer, M_FAILURE_REASON, failureReason);
        writeNullable(serializer, M_THRESHOLD, threshold);
        writeNullable(serializer, M_REPLICATION_TIME, replicationTime);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0:
                return (T) SchemaUtils.validateSameMember(M_REPLICATION_RULE_ID, member, replicationRuleId);
            case 1:
                return (T) SchemaUtils.validateSameMember(M_DESTINATION_BUCKET, member, destinationBucket);
            case 2:
                return (T) SchemaUtils.validateSameMember(M_S3_OPERATION, member, s3Operation);
            case 3:
                return (T) SchemaUtils.validateSameMember(M_REQUEST_TIME, member, requestTime);
            case 4:
                return (T) SchemaUtils.validateSameMember(M_FAILURE_REASON, member, failureReason);
            case 5:
                return (T) SchemaUtils.validateSameMember(M_THRESHOLD, member, threshold);
            case 6:
                return (T) SchemaUtils.validateSameMember(M_REPLICATION_TIME, member, replicationTime);
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
