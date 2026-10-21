$version: "2.0"

// The JSON format of Amazon S3 Event Notifications, as read by S3EventNotification.fromJson and written by toJson
// (https://docs.aws.amazon.com/AmazonS3/latest/userguide/notification-content-structure.html).
//
// Only the schemas are generated from this model (the codegen-maven-plugin generate-types goal). The model classes in
// software.amazon.awssdk.eventnotifications.s3.model are hand-written and implement SerializableStruct with these
// schemas. Member order is the order toJson writes the members in.
namespace software.amazon.awssdk.eventnotifications.s3

/// The S3 Event Notification types. It has no operations; S3EventNotification is the root shape.
service S3EventNotifications {
    version: "2.0"
}

/// An S3 event notification message: the body that S3 sends to SQS, SNS, Lambda or EventBridge.
structure S3EventNotification {
    Records: S3EventNotificationRecordList
}

/// The records of a notification. A null record is kept.
@sparse
list S3EventNotificationRecordList {
    member: S3EventNotificationRecord
}

structure S3EventNotificationRecord {
    eventVersion: String

    eventSource: String

    awsRegion: String

    /// An ISO-8601 instant. It is a string, not a timestamp: it is read with Instant.parse and written with
    /// DateTimeFormatter.ISO_INSTANT (fraction digits in groups of three), which the date-time format of the codecs
    /// does not reproduce.
    eventTime: String

    eventName: String

    userIdentity: UserIdentity

    requestParameters: RequestParameters

    responseElements: ResponseElements

    s3: S3

    glacierEventData: GlacierEventData

    replicationEventData: ReplicationEventData

    intelligentTieringEventData: IntelligentTieringEventData

    lifecycleEventData: LifecycleEventData
}

structure UserIdentity {
    principalId: String
}

structure RequestParameters {
    sourceIPAddress: String
}

structure ResponseElements {
    @jsonName("x-amz-request-id")
    xAmzRequestId: String

    @jsonName("x-amz-id-2")
    xAmzId2: String
}

structure S3 {
    s3SchemaVersion: String

    configurationId: String

    bucket: S3Bucket

    object: S3Object

    objectAnnotation: S3ObjectAnnotationList
}

structure S3Bucket {
    name: String

    ownerIdentity: UserIdentity

    arn: String

    awsGeneratedTags: AwsGeneratedTags
}

/// Tags that S3 generates for the bucket, in input order. A null value is kept.
@sparse
map AwsGeneratedTags {
    key: String
    value: String
}

structure S3Object {
    key: String

    size: Long

    eTag: String

    versionId: String

    sequencer: String

    hasObjectAnnotation: Boolean
}

@sparse
list S3ObjectAnnotationList {
    member: S3ObjectAnnotation
}

structure S3ObjectAnnotation {
    name: String

    size: Long

    eTag: String
}

structure GlacierEventData {
    restoreEventData: RestoreEventData
}

structure RestoreEventData {
    /// An ISO-8601 instant, a string for the reason given on S3EventNotificationRecord$eventTime.
    lifecycleRestorationExpiryTime: String

    lifecycleRestoreStorageClass: String
}

structure ReplicationEventData {
    replicationRuleId: String

    destinationBucket: String

    s3Operation: String

    requestTime: String

    failureReason: String

    threshold: String

    replicationTime: String
}

structure IntelligentTieringEventData {
    destinationAccessTier: String
}

structure LifecycleEventData {
    transitionEventData: TransitionEventData
}

structure TransitionEventData {
    destinationStorageClass: String
}
