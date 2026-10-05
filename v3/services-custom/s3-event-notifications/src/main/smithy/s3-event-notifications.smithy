$version: "2"

// The shapes of the S3 event notification message
// (https://docs.aws.amazon.com/AmazonS3/latest/userguide/notification-content-structure.html), in the member order of
// the message. This model is the source of the schemas published by software.amazon.awssdk:s3-event-notifications
// (S3EventNotificationSchemas); S3EventNotificationSchemaFidelityTest fails the build if they differ. It is not
// packaged: the library does not read a Smithy model at run time.
//
// Member names are the JSON field names of the message, except where a field name is not a Smithy identifier
// (@jsonName). The eventHoldDuration field is a single {"<unit>": <value>} pair on the wire; its schema describes the
// Java value (unit and value), which S3EventNotification.fromJson and toJson map to and from that pair.
namespace software.amazon.awssdk.eventnotifications.s3

structure S3EventNotification {
    Records: S3EventNotificationRecordList
}

@sparse
list S3EventNotificationRecordList {
    member: S3EventNotificationRecord
}

structure S3EventNotificationRecord {
    eventVersion: String
    eventSource: String
    awsRegion: String
    @timestampFormat("date-time")
    eventTime: Timestamp
    eventName: String
    userIdentity: UserIdentity
    requestParameters: RequestParameters
    responseElements: ResponseElements
    s3: S3
    glacierEventData: GlacierEventData
    replicationEventData: ReplicationEventData
    intelligentTieringEventData: IntelligentTieringEventData
    lifecycleEventData: LifecycleEventData
    objectRetentionEventData: ObjectRetentionEventData
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
    @timestampFormat("date-time")
    lifecycleRestorationExpiryTime: Timestamp
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

structure ObjectRetentionEventData {
    mode: String
    retainUntilDate: String
    eventHold: String
    eventHoldDuration: EventHoldDuration
}

structure EventHoldDuration {
    unit: String
    value: Long
}
