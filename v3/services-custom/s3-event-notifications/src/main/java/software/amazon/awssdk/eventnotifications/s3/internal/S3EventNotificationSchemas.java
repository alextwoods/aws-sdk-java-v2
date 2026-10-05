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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * The one schema of every shape of the S3 event notification message, as modeled in
 * {@code src/main/smithy/s3-event-notifications.smithy} (namespace {@value #NAMESPACE}).
 *
 * <p>Each model type publishes its schema as {@code SCHEMA}, the same object as the constant here. Loading this class
 * loads no model type, so the type registry can declare its shape IDs without initializing the types.
 */
public final class S3EventNotificationSchemas {
    /** The Smithy namespace of every S3 event notification shape. */
    public static final String NAMESPACE = "software.amazon.awssdk.eventnotifications.s3";

    private static final TimestampFormatTrait DATE_TIME = new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME);

    public static final Schema USER_IDENTITY = Schema.structureBuilder(id("UserIdentity"))
        .putMember("principalId", PreludeSchemas.STRING)
        .build();

    public static final Schema REQUEST_PARAMETERS = Schema.structureBuilder(id("RequestParameters"))
        .putMember("sourceIPAddress", PreludeSchemas.STRING)
        .build();

    public static final Schema RESPONSE_ELEMENTS = Schema.structureBuilder(id("ResponseElements"))
        .putMember("xAmzRequestId", PreludeSchemas.STRING, new JsonNameTrait("x-amz-request-id"))
        .putMember("xAmzId2", PreludeSchemas.STRING, new JsonNameTrait("x-amz-id-2"))
        .build();

    public static final Schema AWS_GENERATED_TAGS = Schema.mapBuilder(id("AwsGeneratedTags"), new SparseTrait())
        .putMember("key", PreludeSchemas.STRING)
        .putMember("value", PreludeSchemas.STRING)
        .build();

    public static final Schema S3_BUCKET = Schema.structureBuilder(id("S3Bucket"))
        .putMember("name", PreludeSchemas.STRING)
        .putMember("ownerIdentity", USER_IDENTITY)
        .putMember("arn", PreludeSchemas.STRING)
        .putMember("awsGeneratedTags", AWS_GENERATED_TAGS)
        .build();

    public static final Schema S3_OBJECT = Schema.structureBuilder(id("S3Object"))
        .putMember("key", PreludeSchemas.STRING)
        .putMember("size", PreludeSchemas.LONG)
        .putMember("eTag", PreludeSchemas.STRING)
        .putMember("versionId", PreludeSchemas.STRING)
        .putMember("sequencer", PreludeSchemas.STRING)
        .putMember("hasObjectAnnotation", PreludeSchemas.BOOLEAN)
        .build();

    public static final Schema S3_OBJECT_ANNOTATION = Schema.structureBuilder(id("S3ObjectAnnotation"))
        .putMember("name", PreludeSchemas.STRING)
        .putMember("size", PreludeSchemas.LONG)
        .putMember("eTag", PreludeSchemas.STRING)
        .build();

    public static final Schema S3_OBJECT_ANNOTATION_LIST =
        Schema.listBuilder(id("S3ObjectAnnotationList"), new SparseTrait())
              .putMember("member", S3_OBJECT_ANNOTATION)
              .build();

    public static final Schema S3 = Schema.structureBuilder(id("S3"))
        .putMember("s3SchemaVersion", PreludeSchemas.STRING)
        .putMember("configurationId", PreludeSchemas.STRING)
        .putMember("bucket", S3_BUCKET)
        .putMember("object", S3_OBJECT)
        .putMember("objectAnnotation", S3_OBJECT_ANNOTATION_LIST)
        .build();

    public static final Schema RESTORE_EVENT_DATA = Schema.structureBuilder(id("RestoreEventData"))
        .putMember("lifecycleRestorationExpiryTime", PreludeSchemas.TIMESTAMP, DATE_TIME)
        .putMember("lifecycleRestoreStorageClass", PreludeSchemas.STRING)
        .build();

    public static final Schema GLACIER_EVENT_DATA = Schema.structureBuilder(id("GlacierEventData"))
        .putMember("restoreEventData", RESTORE_EVENT_DATA)
        .build();

    public static final Schema REPLICATION_EVENT_DATA = Schema.structureBuilder(id("ReplicationEventData"))
        .putMember("replicationRuleId", PreludeSchemas.STRING)
        .putMember("destinationBucket", PreludeSchemas.STRING)
        .putMember("s3Operation", PreludeSchemas.STRING)
        .putMember("requestTime", PreludeSchemas.STRING)
        .putMember("failureReason", PreludeSchemas.STRING)
        .putMember("threshold", PreludeSchemas.STRING)
        .putMember("replicationTime", PreludeSchemas.STRING)
        .build();

    public static final Schema INTELLIGENT_TIERING_EVENT_DATA =
        Schema.structureBuilder(id("IntelligentTieringEventData"))
              .putMember("destinationAccessTier", PreludeSchemas.STRING)
              .build();

    public static final Schema TRANSITION_EVENT_DATA = Schema.structureBuilder(id("TransitionEventData"))
        .putMember("destinationStorageClass", PreludeSchemas.STRING)
        .build();

    public static final Schema LIFECYCLE_EVENT_DATA = Schema.structureBuilder(id("LifecycleEventData"))
        .putMember("transitionEventData", TRANSITION_EVENT_DATA)
        .build();

    public static final Schema EVENT_HOLD_DURATION = Schema.structureBuilder(id("EventHoldDuration"))
        .putMember("unit", PreludeSchemas.STRING)
        .putMember("value", PreludeSchemas.LONG)
        .build();

    public static final Schema OBJECT_RETENTION_EVENT_DATA = Schema.structureBuilder(id("ObjectRetentionEventData"))
        .putMember("mode", PreludeSchemas.STRING)
        .putMember("retainUntilDate", PreludeSchemas.STRING)
        .putMember("eventHold", PreludeSchemas.STRING)
        .putMember("eventHoldDuration", EVENT_HOLD_DURATION)
        .build();

    public static final Schema S3_EVENT_NOTIFICATION_RECORD = Schema.structureBuilder(id("S3EventNotificationRecord"))
        .putMember("eventVersion", PreludeSchemas.STRING)
        .putMember("eventSource", PreludeSchemas.STRING)
        .putMember("awsRegion", PreludeSchemas.STRING)
        .putMember("eventTime", PreludeSchemas.TIMESTAMP, DATE_TIME)
        .putMember("eventName", PreludeSchemas.STRING)
        .putMember("userIdentity", USER_IDENTITY)
        .putMember("requestParameters", REQUEST_PARAMETERS)
        .putMember("responseElements", RESPONSE_ELEMENTS)
        .putMember("s3", S3)
        .putMember("glacierEventData", GLACIER_EVENT_DATA)
        .putMember("replicationEventData", REPLICATION_EVENT_DATA)
        .putMember("intelligentTieringEventData", INTELLIGENT_TIERING_EVENT_DATA)
        .putMember("lifecycleEventData", LIFECYCLE_EVENT_DATA)
        .putMember("objectRetentionEventData", OBJECT_RETENTION_EVENT_DATA)
        .build();

    public static final Schema S3_EVENT_NOTIFICATION_RECORD_LIST =
        Schema.listBuilder(id("S3EventNotificationRecordList"), new SparseTrait())
              .putMember("member", S3_EVENT_NOTIFICATION_RECORD)
              .build();

    public static final Schema S3_EVENT_NOTIFICATION = Schema.structureBuilder(id("S3EventNotification"))
        .putMember("Records", S3_EVENT_NOTIFICATION_RECORD_LIST)
        .build();

    /** Every schema above, sorted by shape ID. */
    private static final List<Schema> ALL = Stream.of(
        USER_IDENTITY, REQUEST_PARAMETERS, RESPONSE_ELEMENTS, AWS_GENERATED_TAGS, S3_BUCKET, S3_OBJECT,
        S3_OBJECT_ANNOTATION, S3_OBJECT_ANNOTATION_LIST, S3, RESTORE_EVENT_DATA, GLACIER_EVENT_DATA,
        REPLICATION_EVENT_DATA, INTELLIGENT_TIERING_EVENT_DATA, TRANSITION_EVENT_DATA, LIFECYCLE_EVENT_DATA,
        EVENT_HOLD_DURATION, OBJECT_RETENTION_EVENT_DATA, S3_EVENT_NOTIFICATION_RECORD,
        S3_EVENT_NOTIFICATION_RECORD_LIST, S3_EVENT_NOTIFICATION).sorted(Comparator.comparing(Schema::id)).toList();

    private static final Map<ShapeId, Schema> BY_ID =
        ALL.stream().collect(Collectors.toUnmodifiableMap(Schema::id, Function.identity()));

    private S3EventNotificationSchemas() {
    }

    /** Every schema of the namespace (structures, lists and maps), sorted by shape ID. */
    public static List<Schema> all() {
        return ALL;
    }

    /** The schema with exactly the absolute shape ID {@code id}, or {@code null}. */
    public static Schema get(ShapeId id) {
        return id == null ? null : BY_ID.get(id);
    }

    private static ShapeId id(String name) {
        return ShapeId.fromParts(NAMESPACE, name);
    }
}
