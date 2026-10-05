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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Reads S3 event notification JSON with V2's reading behavior: absent and JSON {@code null} fields are {@code null},
 * unknown fields are ignored at every level, a field of an unexpected JSON type fails with an
 * {@link IllegalArgumentException} carrying V2's message, and malformed JSON fails with an
 * {@link UncheckedIOException}.
 */
public final class S3EventNotificationReader {
    private S3EventNotificationReader() {
    }

    public static S3EventNotification read(String event) {
        return read(event.getBytes(StandardCharsets.UTF_8));
    }

    public static S3EventNotification read(InputStream event) {
        Objects.requireNonNull(event, "event");
        try {
            return read(event.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static S3EventNotification read(byte[] event) {
        Object root;
        try {
            root = JsonTree.parse(event);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, Object> records = expectObjectOrNull(root, "Records");
        if (records == null) {
            return new S3EventNotification(null);
        }
        return new S3EventNotification(readRecords(records.get("Records")));
    }

    private static List<S3EventNotificationRecord> readRecords(Object node) {
        List<Object> recordArray = expectArrayOrNull(node, "Records");
        if (recordArray == null) {
            return null;
        }
        List<S3EventNotificationRecord> records = new ArrayList<>(recordArray.size());
        recordArray.forEach(r -> records.add(readRecord(r)));
        return records;
    }

    private static S3EventNotificationRecord readRecord(Object node) {
        Map<String, Object> recordNode = expectObjectOrNull(node, "Records[]");
        if (recordNode == null) {
            return null;
        }
        S3EventNotificationRecord eventNotificationRecord = new S3EventNotificationRecord();
        eventNotificationRecord.setEventVersion(expectStringOrNull(recordNode, "eventVersion"));
        eventNotificationRecord.setAwsRegion(expectStringOrNull(recordNode, "awsRegion"));
        eventNotificationRecord.setEventName(expectStringOrNull(recordNode, "eventName"));
        eventNotificationRecord.setEventSource(expectStringOrNull(recordNode, "eventSource"));
        String eventTime = expectStringOrNull(recordNode, "eventTime");
        eventNotificationRecord.setEventTime(eventTime != null ? Instant.parse(eventTime) : null);
        eventNotificationRecord.setRequestParameters(readRequestParameters(recordNode.get("requestParameters")));
        eventNotificationRecord.setResponseElements(readResponseElements(recordNode.get("responseElements")));
        eventNotificationRecord.setS3(readS3(recordNode.get("s3")));
        eventNotificationRecord.setUserIdentity(readUserIdentity(recordNode.get("userIdentity"), "userIdentity"));
        eventNotificationRecord.setGlacierEventData(readGlacierEventData(recordNode.get("glacierEventData")));
        eventNotificationRecord.setLifecycleEventData(readLifecycleEventData(recordNode.get("lifecycleEventData")));
        eventNotificationRecord.setIntelligentTieringEventData(
            readIntelligentTieringEventData(recordNode.get("intelligentTieringEventData")));
        eventNotificationRecord.setReplicationEventData(
            readReplicationEventData(recordNode.get("replicationEventData")));
        eventNotificationRecord.setObjectRetentionEventData(
            readObjectRetentionEventData(recordNode.get("objectRetentionEventData")));
        return eventNotificationRecord;
    }

    private static ObjectRetentionEventData readObjectRetentionEventData(Object node) {
        Map<String, Object> retentionNode = expectObjectOrNull(node, "objectRetentionEventData");
        if (retentionNode == null) {
            return null;
        }
        String mode = expectStringOrNull(retentionNode, "mode");
        String retainUntilDate = expectStringOrNull(retentionNode, "retainUntilDate");
        String eventHold = expectStringOrNull(retentionNode, "eventHold");
        EventHoldDuration eventHoldDuration = readEventHoldDuration(retentionNode.get("eventHoldDuration"));
        return new ObjectRetentionEventData(mode, retainUntilDate, eventHold, eventHoldDuration);
    }

    private static EventHoldDuration readEventHoldDuration(Object node) {
        Map<String, Object> durationNode = expectObjectOrNull(node, "eventHoldDuration");
        if (durationNode == null || durationNode.isEmpty()) {
            return null;
        }
        // A single {unit: value} pair (for example {"days": 90} or {"years": 1}); whichever unit the service emits is
        // read, so a new unit does not require an SDK change.
        Map.Entry<String, Object> entry = durationNode.entrySet().iterator().next();
        String unit = entry.getKey();
        return new EventHoldDuration(unit, expectLong(entry.getValue(), unit));
    }

    private static ReplicationEventData readReplicationEventData(Object node) {
        Map<String, Object> replicationDataNode = expectObjectOrNull(node, "replicationEventData");
        if (replicationDataNode == null) {
            return null;
        }
        return new ReplicationEventData(expectStringOrNull(replicationDataNode, "replicationRuleId"),
                                        expectStringOrNull(replicationDataNode, "destinationBucket"),
                                        expectStringOrNull(replicationDataNode, "s3Operation"),
                                        expectStringOrNull(replicationDataNode, "requestTime"),
                                        expectStringOrNull(replicationDataNode, "failureReason"),
                                        expectStringOrNull(replicationDataNode, "threshold"),
                                        expectStringOrNull(replicationDataNode, "replicationTime"));
    }

    private static IntelligentTieringEventData readIntelligentTieringEventData(Object node) {
        Map<String, Object> tieringNode = expectObjectOrNull(node, "intelligentTieringEventData");
        if (tieringNode == null) {
            return null;
        }
        return new IntelligentTieringEventData(expectStringOrNull(tieringNode, "destinationAccessTier"));
    }

    private static LifecycleEventData readLifecycleEventData(Object node) {
        Map<String, Object> lifecycleNode = expectObjectOrNull(node, "lifecycleEventData");
        if (lifecycleNode == null) {
            return null;
        }
        Map<String, Object> transitionNode =
            expectObjectOrNull(lifecycleNode.get("transitionEventData"), "transitionEventData");
        if (transitionNode == null) {
            return new LifecycleEventData(null);
        }
        return new LifecycleEventData(
            new TransitionEventData(expectStringOrNull(transitionNode, "destinationStorageClass")));
    }

    private static GlacierEventData readGlacierEventData(Object node) {
        Map<String, Object> glacierNode = expectObjectOrNull(node, "glacierEventData");
        if (glacierNode == null) {
            return null;
        }
        Map<String, Object> restoreNode = expectObjectOrNull(glacierNode.get("restoreEventData"), "restoreEventData");
        if (restoreNode == null) {
            return new GlacierEventData(null);
        }
        return new GlacierEventData(
            new RestoreEventData(expectStringOrNull(restoreNode, "lifecycleRestorationExpiryTime"),
                                 expectStringOrNull(restoreNode, "lifecycleRestoreStorageClass")));
    }

    private static UserIdentity readUserIdentity(Object node, String name) {
        Map<String, Object> identityNode = expectObjectOrNull(node, name);
        if (identityNode == null) {
            return null;
        }
        return new UserIdentity(expectStringOrNull(identityNode, "principalId"));
    }

    private static S3 readS3(Object node) {
        Map<String, Object> s3 = expectObjectOrNull(node, "s3");
        if (s3 == null) {
            return null;
        }
        String configurationId = expectStringOrNull(s3, "configurationId");
        S3Bucket bucket = readBucket(s3.get("bucket"));
        S3Object object = readObject(s3.get("object"));
        String s3SchemaVersion = expectStringOrNull(s3, "s3SchemaVersion");
        List<S3ObjectAnnotation> objectAnnotation = readObjectAnnotations(s3.get("objectAnnotation"));
        return new S3(configurationId, bucket, object, s3SchemaVersion, objectAnnotation);
    }

    private static S3Object readObject(Object node) {
        Map<String, Object> objectNode = expectObjectOrNull(node, "object");
        if (objectNode == null) {
            return null;
        }
        String key = expectStringOrNull(objectNode, "key");
        Long size = expectLong(objectNode.get("size"), "size");
        String eTag = expectStringOrNull(objectNode, "eTag");
        String versionId = expectStringOrNull(objectNode, "versionId");
        String sequencer = expectStringOrNull(objectNode, "sequencer");
        Object hasObjectAnnotationNode = objectNode.get("hasObjectAnnotation");
        Boolean hasObjectAnnotation = isNull(hasObjectAnnotationNode)
                                      ? null
                                      : expectBoolean(hasObjectAnnotationNode, "hasObjectAnnotation");
        return new S3Object(key, size, eTag, versionId, sequencer, hasObjectAnnotation);
    }

    private static S3Bucket readBucket(Object node) {
        Map<String, Object> bucketNode = expectObjectOrNull(node, "bucket");
        if (bucketNode == null) {
            return null;
        }
        String name = expectStringOrNull(bucketNode, "name");
        UserIdentity ownerIdentity = readUserIdentity(bucketNode.get("ownerIdentity"), "ownerIdentity");
        String arn = expectStringOrNull(bucketNode, "arn");
        Map<String, String> awsGeneratedTags = readStringMapOrNull(bucketNode.get("awsGeneratedTags"),
                                                                   "awsGeneratedTags");
        return new S3Bucket(name, ownerIdentity, arn, awsGeneratedTags);
    }

    private static Map<String, String> readStringMapOrNull(Object node, String name) {
        Map<String, Object> mapNode = expectObjectOrNull(node, name);
        if (mapNode == null) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        mapNode.forEach((tagKey, tagValue) -> result.put(tagKey, readTagStringValueOrNull(name, tagKey, tagValue)));
        return result;
    }

    private static String readTagStringValueOrNull(String mapName, String tagKey, Object tagValue) {
        if (isNull(tagValue)) {
            return null;
        }
        isTrue(tagValue instanceof String, "The value of tag '%s' in '%s' was not a string, but was: %s",
               tagKey, mapName, JsonTree.describe(tagValue));
        return (String) tagValue;
    }

    private static ResponseElements readResponseElements(Object node) {
        Map<String, Object> responseElementNode = expectObjectOrNull(node, "responseElements");
        if (responseElementNode == null) {
            return null;
        }
        String requestId = expectStringOrNull(responseElementNode, "x-amz-request-id");
        String id2 = expectStringOrNull(responseElementNode, "x-amz-id-2");
        return new ResponseElements(id2, requestId);
    }

    private static RequestParameters readRequestParameters(Object node) {
        Map<String, Object> requestParametersNode = expectObjectOrNull(node, "requestParameters");
        if (requestParametersNode == null) {
            return null;
        }
        return new RequestParameters(expectStringOrNull(requestParametersNode, "sourceIPAddress"));
    }

    private static List<S3ObjectAnnotation> readObjectAnnotations(Object node) {
        List<Object> annotationArray = expectArrayOrNull(node, "objectAnnotation");
        if (annotationArray == null) {
            return null;
        }
        List<S3ObjectAnnotation> annotations = new ArrayList<>(annotationArray.size());
        annotationArray.forEach(a -> annotations.add(readObjectAnnotation(a)));
        return annotations;
    }

    private static S3ObjectAnnotation readObjectAnnotation(Object node) {
        Map<String, Object> annotationNode = expectObjectOrNull(node, "objectAnnotation[]");
        if (annotationNode == null) {
            return null;
        }
        return new S3ObjectAnnotation(expectStringOrNull(annotationNode, "name"),
                                      expectLong(annotationNode.get("size"), "size"),
                                      expectStringOrNull(annotationNode, "eTag"));
    }

    private static String expectStringOrNull(Map<String, Object> node, String name) {
        Object value = node.get(name);
        if (isNull(value)) {
            return null;
        }
        isTrue(value instanceof String, "'%s' was not a string", name);
        return (String) value;
    }

    private static List<Object> expectArrayOrNull(Object node, String name) {
        if (isNull(node)) {
            return null;
        }
        isTrue(node instanceof List, "expected '%s' to be an array, but was not.", name);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) node;
        return list;
    }

    private static Map<String, Object> expectObjectOrNull(Object node, String name) {
        if (isNull(node)) {
            return null;
        }
        isTrue(node instanceof Map, "expected '%s' to be an object, but was not.", name);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) node;
        return map;
    }

    /**
     * The value of a numeric field. As in V2, the JSON number text must be a {@code long}: a fraction, an exponent or
     * an out-of-range value fails with a {@link NumberFormatException}.
     */
    private static Long expectLong(Object node, String name) {
        if (isNull(node)) {
            return null;
        }
        isTrue(node instanceof JsonTree.JsonNumber, "expected '%s' to be numeric, but was not", name);
        return Long.parseLong(((JsonTree.JsonNumber) node).text());
    }

    private static Boolean expectBoolean(Object node, String name) {
        isTrue(node instanceof Boolean, "expected '%s' to be boolean, but was not", name);
        return (Boolean) node;
    }

    private static boolean isNull(Object node) {
        return node == null || node == JsonTree.NULL;
    }

    private static void isTrue(boolean condition, String template, Object... arguments) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(template, arguments));
        }
    }
}
