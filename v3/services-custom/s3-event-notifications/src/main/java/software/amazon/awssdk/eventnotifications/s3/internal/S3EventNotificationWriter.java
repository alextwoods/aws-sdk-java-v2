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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.eventnotifications.s3.model.EventHoldDuration;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ObjectRetentionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ReplicationEventData;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.S3ObjectAnnotation;
import software.amazon.awssdk.eventnotifications.s3.model.TransitionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;

/**
 * Writes S3 event notification JSON with V2's field order and null handling: the optional event data
 * ({@code glacierEventData}, {@code replicationEventData}, {@code intelligentTieringEventData},
 * {@code lifecycleEventData}, {@code objectRetentionEventData}), {@code objectAnnotation},
 * {@code hasObjectAnnotation}, {@code eventHold}, {@code eventHoldDuration} and empty {@code awsGeneratedTags} are
 * omitted when absent; every other absent field is written as JSON {@code null}.
 *
 * <p>Three V2 writer failures are corrected so that every value the model can hold is written as documented (absent
 * values as {@code null}, absent optional nested data as an empty object): V2 threw a {@code NullPointerException} for
 * a bucket without an owner identity and for a {@code null} object annotation element, and failed with a JSON
 * generation error for {@link GlacierEventData} without restore event data. V3 writes {@code "ownerIdentity":null},
 * a {@code null} element and {@code "glacierEventData":{}} instead. Everything V2 wrote is written identically
 * (S3EventNotificationV2DifferentialTest).
 */
public final class S3EventNotificationWriter {
    public static final S3EventNotificationWriter COMPACT = new S3EventNotificationWriter(false);
    public static final S3EventNotificationWriter PRETTY = new S3EventNotificationWriter(true);

    private final boolean prettyPrint;

    private S3EventNotificationWriter(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public String writeToString(S3EventNotification event) {
        JsonOutput writer = new JsonOutput(prettyPrint);
        writer.startObject();
        writeRecords(writer, event.getRecords());
        writer.endObject();
        return writer.result();
    }

    private static void writeRecords(JsonOutput writer, List<S3EventNotificationRecord> records) {
        writer.fieldName("Records");
        if (records == null) {
            writer.nullValue();
            return;
        }
        writer.startArray();
        records.forEach(rec -> writeRecord(writer, rec));
        writer.endArray();
    }

    private static void writeRecord(JsonOutput writer, S3EventNotificationRecord rec) {
        if (rec == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "eventVersion", rec.getEventVersion());
        writeStringField(writer, "eventSource", rec.getEventSource());
        writeStringField(writer, "awsRegion", rec.getAwsRegion());
        writeStringField(writer, "eventTime", format(rec.getEventTime()));
        writeStringField(writer, "eventName", rec.getEventName());
        writeUserIdentity(writer, "userIdentity", rec.getUserIdentity());
        writeRequestParam(writer, rec.getRequestParameters());
        writeResponseElements(writer, rec.getResponseElements());
        writeS3(writer, rec.getS3());
        if (rec.getGlacierEventData() != null) {
            writeGlacierEventData(writer, rec.getGlacierEventData());
        }
        if (rec.getReplicationEventData() != null) {
            writeReplicationEventData(writer, rec.getReplicationEventData());
        }
        if (rec.getIntelligentTieringEventData() != null) {
            writeIntelligentTieringEventData(writer, rec.getIntelligentTieringEventData());
        }
        if (rec.getLifecycleEventData() != null) {
            writeLifecycleEventData(writer, rec.getLifecycleEventData());
        }
        if (rec.getObjectRetentionEventData() != null) {
            writeObjectRetentionEventData(writer, rec.getObjectRetentionEventData());
        }
        writer.endObject();
    }

    private static void writeObjectRetentionEventData(JsonOutput writer, ObjectRetentionEventData data) {
        writer.fieldName("objectRetentionEventData");
        writer.startObject();
        writeStringField(writer, "mode", data.getMode());
        writeStringField(writer, "retainUntilDate", data.getRetainUntilDate());
        if (data.getEventHold() != null) {
            writeStringField(writer, "eventHold", data.getEventHold());
        }
        EventHoldDuration eventHoldDuration = data.getEventHoldDuration();
        if (eventHoldDuration != null && eventHoldDuration.getUnit() != null) {
            writer.fieldName("eventHoldDuration");
            writer.startObject();
            // A single {unit: value} pair, for example {"days": 90} or {"years": 1}.
            writer.fieldName(eventHoldDuration.getUnit()).value(eventHoldDuration.getValue());
            writer.endObject();
        }
        writer.endObject();
    }

    private static void writeLifecycleEventData(JsonOutput writer, LifecycleEventData lifecycleEventData) {
        writer.fieldName("lifecycleEventData");
        writer.startObject();
        TransitionEventData transitionEventData = lifecycleEventData.getTransitionEventData();
        if (transitionEventData != null) {
            writer.fieldName("transitionEventData");
            writer.startObject();
            writeStringField(writer, "destinationStorageClass", transitionEventData.getDestinationStorageClass());
            writer.endObject();
        }
        writer.endObject();
    }

    private static void writeIntelligentTieringEventData(JsonOutput writer, IntelligentTieringEventData data) {
        writer.fieldName("intelligentTieringEventData");
        writer.startObject();
        writeStringField(writer, "destinationAccessTier", data.getDestinationAccessTier());
        writer.endObject();
    }

    private static void writeReplicationEventData(JsonOutput writer, ReplicationEventData data) {
        writer.fieldName("replicationEventData");
        writer.startObject();
        writeStringField(writer, "replicationRuleId", data.getReplicationRuleId());
        writeStringField(writer, "destinationBucket", data.getDestinationBucket());
        writeStringField(writer, "s3Operation", data.getS3Operation());
        writeStringField(writer, "requestTime", data.getRequestTime());
        writeStringField(writer, "failureReason", data.getFailureReason());
        writeStringField(writer, "threshold", data.getThreshold());
        writeStringField(writer, "replicationTime", data.getReplicationTime());
        writer.endObject();
    }

    private static void writeGlacierEventData(JsonOutput writer, GlacierEventData glacierEventData) {
        writer.fieldName("glacierEventData");
        writer.startObject();
        RestoreEventData restoreEventData = glacierEventData.getRestoreEventData();
        if (restoreEventData != null) {
            writer.fieldName("restoreEventData");
            writer.startObject();
            writeStringField(writer, "lifecycleRestorationExpiryTime",
                             format(restoreEventData.getLifecycleRestorationExpiryTime()));
            writeStringField(writer, "lifecycleRestoreStorageClass", restoreEventData.getLifecycleRestoreStorageClass());
            writer.endObject();
        }
        writer.endObject();
    }

    private static void writeS3(JsonOutput writer, S3 s3) {
        writer.fieldName("s3");
        if (s3 == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "s3SchemaVersion", s3.getS3SchemaVersion());
        writeStringField(writer, "configurationId", s3.getConfigurationId());
        writeS3Bucket(writer, s3.getBucket());
        writeS3Object(writer, s3.getObject());
        if (s3.getObjectAnnotation() != null) {
            writeS3ObjectAnnotations(writer, s3.getObjectAnnotation());
        }
        writer.endObject();
    }

    private static void writeS3Object(JsonOutput writer, S3Object s3Object) {
        writer.fieldName("object");
        if (s3Object == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "key", s3Object.getKey());
        writer.fieldName("size").value(s3Object.getSizeAsLong());
        writeStringField(writer, "eTag", s3Object.getETag());
        writeStringField(writer, "versionId", s3Object.getVersionId());
        writeStringField(writer, "sequencer", s3Object.getSequencer());
        if (s3Object.getHasObjectAnnotation() != null) {
            writer.fieldName("hasObjectAnnotation").value(s3Object.getHasObjectAnnotation().booleanValue());
        }
        writer.endObject();
    }

    private static void writeS3Bucket(JsonOutput writer, S3Bucket bucket) {
        writer.fieldName("bucket");
        if (bucket == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "name", bucket.getName());
        // V2 wrote null for an owner without a principal ID (and failed for an absent owner): both are null.
        UserIdentity ownerIdentity = bucket.getOwnerIdentity();
        writer.fieldName("ownerIdentity");
        if (ownerIdentity == null || ownerIdentity.getPrincipalId() == null) {
            writer.nullValue();
        } else {
            writer.startObject();
            writeStringField(writer, "principalId", ownerIdentity.getPrincipalId());
            writer.endObject();
        }
        writeStringField(writer, "arn", bucket.getArn());
        Map<String, String> awsGeneratedTags = bucket.getAwsGeneratedTags();
        if (awsGeneratedTags != null && !awsGeneratedTags.isEmpty()) {
            writer.fieldName("awsGeneratedTags");
            writer.startObject();
            awsGeneratedTags.forEach((k, v) -> writeStringField(writer, k, v));
            writer.endObject();
        }
        writer.endObject();
    }

    private static void writeResponseElements(JsonOutput writer, ResponseElements responseElements) {
        writer.fieldName("responseElements");
        if (responseElements == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "x-amz-request-id", responseElements.getXAmzRequestId());
        writeStringField(writer, "x-amz-id-2", responseElements.getXAmzId2());
        writer.endObject();
    }

    private static void writeRequestParam(JsonOutput writer, RequestParameters requestParameters) {
        writer.fieldName("requestParameters");
        if (requestParameters == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "sourceIPAddress", requestParameters.getSourceIpAddress());
        writer.endObject();
    }

    private static void writeUserIdentity(JsonOutput writer, String field, UserIdentity userIdentity) {
        writer.fieldName(field);
        if (userIdentity == null) {
            writer.nullValue();
            return;
        }
        writer.startObject();
        writeStringField(writer, "principalId", userIdentity.getPrincipalId());
        writer.endObject();
    }

    private static void writeS3ObjectAnnotations(JsonOutput writer, List<S3ObjectAnnotation> annotations) {
        writer.fieldName("objectAnnotation");
        writer.startArray();
        for (S3ObjectAnnotation annotation : annotations) {
            if (annotation == null) {
                writer.nullValue();
                continue;
            }
            writer.startObject();
            writeStringField(writer, "name", annotation.getName());
            writer.fieldName("size").value(annotation.getSize());
            writeStringField(writer, "eTag", annotation.getETag());
            writer.endObject();
        }
        writer.endArray();
    }

    private static void writeStringField(JsonOutput writer, String field, String value) {
        writer.fieldName(field).value(value);
    }

    private static String format(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
