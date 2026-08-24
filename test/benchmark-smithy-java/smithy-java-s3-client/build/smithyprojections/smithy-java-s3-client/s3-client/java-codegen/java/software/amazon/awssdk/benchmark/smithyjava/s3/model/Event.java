package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.SmithyEnum;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * The bucket event for which to send notifications.
 */
@SmithyGenerated
public sealed interface Event extends SmithyEnum, SerializableShape {
    Event S3_REDUCED_REDUNDANCY_LOST_OBJECT = new S3ReducedRedundancyLostObjectType();
    Event S3_OBJECT_CREATED_ = new S3ObjectCreatedType();
    Event S3_OBJECT_CREATED_PUT = new S3ObjectCreatedPutType();
    Event S3_OBJECT_CREATED_POST = new S3ObjectCreatedPostType();
    Event S3_OBJECT_CREATED_COPY = new S3ObjectCreatedCopyType();
    Event S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD = new S3ObjectCreatedCompleteMultipartUploadType();
    Event S3_OBJECT_REMOVED_ = new S3ObjectRemovedType();
    Event S3_OBJECT_REMOVED_DELETE = new S3ObjectRemovedDeleteType();
    Event S3_OBJECT_REMOVED_DELETE_MARKER_CREATED = new S3ObjectRemovedDeleteMarkerCreatedType();
    Event S3_OBJECT_RESTORE_ = new S3ObjectRestoreType();
    Event S3_OBJECT_RESTORE_POST = new S3ObjectRestorePostType();
    Event S3_OBJECT_RESTORE_COMPLETED = new S3ObjectRestoreCompletedType();
    Event S3_REPLICATION_ = new S3ReplicationType();
    Event S3_REPLICATION_OPERATION_FAILED_REPLICATION = new S3ReplicationOperationFailedReplicationType();
    Event S3_REPLICATION_OPERATION_NOT_TRACKED = new S3ReplicationOperationNotTrackedType();
    Event S3_REPLICATION_OPERATION_MISSED_THRESHOLD = new S3ReplicationOperationMissedThresholdType();
    Event S3_REPLICATION_OPERATION_REPLICATED_AFTER_THRESHOLD = new S3ReplicationOperationReplicatedAfterThresholdType();
    Event S3_OBJECT_RESTORE_DELETE = new S3ObjectRestoreDeleteType();
    Event S3_LIFECYCLE_TRANSITION = new S3LifecycleTransitionType();
    Event S3_INTELLIGENT_TIERING = new S3IntelligentTieringType();
    Event S3_OBJECT_ACL_PUT = new S3ObjectAclPutType();
    Event S3_LIFECYCLE_EXPIRATION_ = new S3LifecycleExpirationType();
    Event S3_LIFECYCLE_EXPIRATION_DELETE = new S3LifecycleExpirationDeleteType();
    Event S3_LIFECYCLE_EXPIRATION_DELETE_MARKER_CREATED = new S3LifecycleExpirationDeleteMarkerCreatedType();
    Event S3_OBJECT_TAGGING_ = new S3ObjectTaggingType();
    Event S3_OBJECT_TAGGING_PUT = new S3ObjectTaggingPutType();
    Event S3_OBJECT_TAGGING_DELETE = new S3ObjectTaggingDeleteType();
    Event S3_OBJECT_ANNOTATION_ = new S3ObjectAnnotationType();
    Event S3_OBJECT_ANNOTATION_PUT = new S3ObjectAnnotationPutType();
    Event S3_OBJECT_ANNOTATION_DELETE = new S3ObjectAnnotationDeleteType();
    List<Event> $TYPES = List.of(S3_REDUCED_REDUNDANCY_LOST_OBJECT, S3_OBJECT_CREATED_, S3_OBJECT_CREATED_PUT, S3_OBJECT_CREATED_POST, S3_OBJECT_CREATED_COPY, S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD, S3_OBJECT_REMOVED_, S3_OBJECT_REMOVED_DELETE, S3_OBJECT_REMOVED_DELETE_MARKER_CREATED, S3_OBJECT_RESTORE_, S3_OBJECT_RESTORE_POST, S3_OBJECT_RESTORE_COMPLETED, S3_REPLICATION_, S3_REPLICATION_OPERATION_FAILED_REPLICATION, S3_REPLICATION_OPERATION_NOT_TRACKED, S3_REPLICATION_OPERATION_MISSED_THRESHOLD, S3_REPLICATION_OPERATION_REPLICATED_AFTER_THRESHOLD, S3_OBJECT_RESTORE_DELETE, S3_LIFECYCLE_TRANSITION, S3_INTELLIGENT_TIERING, S3_OBJECT_ACL_PUT, S3_LIFECYCLE_EXPIRATION_, S3_LIFECYCLE_EXPIRATION_DELETE, S3_LIFECYCLE_EXPIRATION_DELETE_MARKER_CREATED, S3_OBJECT_TAGGING_, S3_OBJECT_TAGGING_PUT, S3_OBJECT_TAGGING_DELETE, S3_OBJECT_ANNOTATION_, S3_OBJECT_ANNOTATION_PUT, S3_OBJECT_ANNOTATION_DELETE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#Event"),
        Set.of(S3_REDUCED_REDUNDANCY_LOST_OBJECT.getValue(), S3_OBJECT_CREATED_.getValue(), S3_OBJECT_CREATED_PUT.getValue(), S3_OBJECT_CREATED_POST.getValue(), S3_OBJECT_CREATED_COPY.getValue(), S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD.getValue(), S3_OBJECT_REMOVED_.getValue(), S3_OBJECT_REMOVED_DELETE.getValue(), S3_OBJECT_REMOVED_DELETE_MARKER_CREATED.getValue(), S3_OBJECT_RESTORE_.getValue(), S3_OBJECT_RESTORE_POST.getValue(), S3_OBJECT_RESTORE_COMPLETED.getValue(), S3_REPLICATION_.getValue(), S3_REPLICATION_OPERATION_FAILED_REPLICATION.getValue(), S3_REPLICATION_OPERATION_NOT_TRACKED.getValue(), S3_REPLICATION_OPERATION_MISSED_THRESHOLD.getValue(), S3_REPLICATION_OPERATION_REPLICATED_AFTER_THRESHOLD.getValue(), S3_OBJECT_RESTORE_DELETE.getValue(), S3_LIFECYCLE_TRANSITION.getValue(), S3_INTELLIGENT_TIERING.getValue(), S3_OBJECT_ACL_PUT.getValue(), S3_LIFECYCLE_EXPIRATION_.getValue(), S3_LIFECYCLE_EXPIRATION_DELETE.getValue(), S3_LIFECYCLE_EXPIRATION_DELETE_MARKER_CREATED.getValue(), S3_OBJECT_TAGGING_.getValue(), S3_OBJECT_TAGGING_PUT.getValue(), S3_OBJECT_TAGGING_DELETE.getValue(), S3_OBJECT_ANNOTATION_.getValue(), S3_OBJECT_ANNOTATION_PUT.getValue(), S3_OBJECT_ANNOTATION_DELETE.getValue()), Event.class
    );

    ShapeId $ID = $SCHEMA.id();

    String getValue();

    @Override
    default void serialize(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA, getValue());
    }

    /**
     * Create an unknown enum variant with the given value.
     *
     * @param value value for the unknown variant.
     */
    static Event unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Event> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Event} constant with the specified value.
     *
     * @param value value to create {@code Event} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Event from(String value) {
        return switch (value) {
            case "s3:ReducedRedundancyLostObject" -> S3_REDUCED_REDUNDANCY_LOST_OBJECT;
            case "s3:ObjectCreated:*" -> S3_OBJECT_CREATED_;
            case "s3:ObjectCreated:Put" -> S3_OBJECT_CREATED_PUT;
            case "s3:ObjectCreated:Post" -> S3_OBJECT_CREATED_POST;
            case "s3:ObjectCreated:Copy" -> S3_OBJECT_CREATED_COPY;
            case "s3:ObjectCreated:CompleteMultipartUpload" -> S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD;
            case "s3:ObjectRemoved:*" -> S3_OBJECT_REMOVED_;
            case "s3:ObjectRemoved:Delete" -> S3_OBJECT_REMOVED_DELETE;
            case "s3:ObjectRemoved:DeleteMarkerCreated" -> S3_OBJECT_REMOVED_DELETE_MARKER_CREATED;
            case "s3:ObjectRestore:*" -> S3_OBJECT_RESTORE_;
            case "s3:ObjectRestore:Post" -> S3_OBJECT_RESTORE_POST;
            case "s3:ObjectRestore:Completed" -> S3_OBJECT_RESTORE_COMPLETED;
            case "s3:Replication:*" -> S3_REPLICATION_;
            case "s3:Replication:OperationFailedReplication" -> S3_REPLICATION_OPERATION_FAILED_REPLICATION;
            case "s3:Replication:OperationNotTracked" -> S3_REPLICATION_OPERATION_NOT_TRACKED;
            case "s3:Replication:OperationMissedThreshold" -> S3_REPLICATION_OPERATION_MISSED_THRESHOLD;
            case "s3:Replication:OperationReplicatedAfterThreshold" -> S3_REPLICATION_OPERATION_REPLICATED_AFTER_THRESHOLD;
            case "s3:ObjectRestore:Delete" -> S3_OBJECT_RESTORE_DELETE;
            case "s3:LifecycleTransition" -> S3_LIFECYCLE_TRANSITION;
            case "s3:IntelligentTiering" -> S3_INTELLIGENT_TIERING;
            case "s3:ObjectAcl:Put" -> S3_OBJECT_ACL_PUT;
            case "s3:LifecycleExpiration:*" -> S3_LIFECYCLE_EXPIRATION_;
            case "s3:LifecycleExpiration:Delete" -> S3_LIFECYCLE_EXPIRATION_DELETE;
            case "s3:LifecycleExpiration:DeleteMarkerCreated" -> S3_LIFECYCLE_EXPIRATION_DELETE_MARKER_CREATED;
            case "s3:ObjectTagging:*" -> S3_OBJECT_TAGGING_;
            case "s3:ObjectTagging:Put" -> S3_OBJECT_TAGGING_PUT;
            case "s3:ObjectTagging:Delete" -> S3_OBJECT_TAGGING_DELETE;
            case "s3:ObjectAnnotation:*" -> S3_OBJECT_ANNOTATION_;
            case "s3:ObjectAnnotation:Put" -> S3_OBJECT_ANNOTATION_PUT;
            case "s3:ObjectAnnotation:Delete" -> S3_OBJECT_ANNOTATION_DELETE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class S3ReducedRedundancyLostObjectType implements Event {
        private S3ReducedRedundancyLostObjectType() {}

        @Override
        public String getValue() {
            return "s3:ReducedRedundancyLostObject";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectCreatedType implements Event {
        private S3ObjectCreatedType() {}

        @Override
        public String getValue() {
            return "s3:ObjectCreated:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectCreatedPutType implements Event {
        private S3ObjectCreatedPutType() {}

        @Override
        public String getValue() {
            return "s3:ObjectCreated:Put";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectCreatedPostType implements Event {
        private S3ObjectCreatedPostType() {}

        @Override
        public String getValue() {
            return "s3:ObjectCreated:Post";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectCreatedCopyType implements Event {
        private S3ObjectCreatedCopyType() {}

        @Override
        public String getValue() {
            return "s3:ObjectCreated:Copy";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectCreatedCompleteMultipartUploadType implements Event {
        private S3ObjectCreatedCompleteMultipartUploadType() {}

        @Override
        public String getValue() {
            return "s3:ObjectCreated:CompleteMultipartUpload";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRemovedType implements Event {
        private S3ObjectRemovedType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRemoved:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRemovedDeleteType implements Event {
        private S3ObjectRemovedDeleteType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRemoved:Delete";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRemovedDeleteMarkerCreatedType implements Event {
        private S3ObjectRemovedDeleteMarkerCreatedType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRemoved:DeleteMarkerCreated";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRestoreType implements Event {
        private S3ObjectRestoreType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRestore:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRestorePostType implements Event {
        private S3ObjectRestorePostType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRestore:Post";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRestoreCompletedType implements Event {
        private S3ObjectRestoreCompletedType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRestore:Completed";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ReplicationType implements Event {
        private S3ReplicationType() {}

        @Override
        public String getValue() {
            return "s3:Replication:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ReplicationOperationFailedReplicationType implements Event {
        private S3ReplicationOperationFailedReplicationType() {}

        @Override
        public String getValue() {
            return "s3:Replication:OperationFailedReplication";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ReplicationOperationNotTrackedType implements Event {
        private S3ReplicationOperationNotTrackedType() {}

        @Override
        public String getValue() {
            return "s3:Replication:OperationNotTracked";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ReplicationOperationMissedThresholdType implements Event {
        private S3ReplicationOperationMissedThresholdType() {}

        @Override
        public String getValue() {
            return "s3:Replication:OperationMissedThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ReplicationOperationReplicatedAfterThresholdType implements Event {
        private S3ReplicationOperationReplicatedAfterThresholdType() {}

        @Override
        public String getValue() {
            return "s3:Replication:OperationReplicatedAfterThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectRestoreDeleteType implements Event {
        private S3ObjectRestoreDeleteType() {}

        @Override
        public String getValue() {
            return "s3:ObjectRestore:Delete";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3LifecycleTransitionType implements Event {
        private S3LifecycleTransitionType() {}

        @Override
        public String getValue() {
            return "s3:LifecycleTransition";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3IntelligentTieringType implements Event {
        private S3IntelligentTieringType() {}

        @Override
        public String getValue() {
            return "s3:IntelligentTiering";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectAclPutType implements Event {
        private S3ObjectAclPutType() {}

        @Override
        public String getValue() {
            return "s3:ObjectAcl:Put";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3LifecycleExpirationType implements Event {
        private S3LifecycleExpirationType() {}

        @Override
        public String getValue() {
            return "s3:LifecycleExpiration:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3LifecycleExpirationDeleteType implements Event {
        private S3LifecycleExpirationDeleteType() {}

        @Override
        public String getValue() {
            return "s3:LifecycleExpiration:Delete";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3LifecycleExpirationDeleteMarkerCreatedType implements Event {
        private S3LifecycleExpirationDeleteMarkerCreatedType() {}

        @Override
        public String getValue() {
            return "s3:LifecycleExpiration:DeleteMarkerCreated";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectTaggingType implements Event {
        private S3ObjectTaggingType() {}

        @Override
        public String getValue() {
            return "s3:ObjectTagging:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectTaggingPutType implements Event {
        private S3ObjectTaggingPutType() {}

        @Override
        public String getValue() {
            return "s3:ObjectTagging:Put";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectTaggingDeleteType implements Event {
        private S3ObjectTaggingDeleteType() {}

        @Override
        public String getValue() {
            return "s3:ObjectTagging:Delete";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectAnnotationType implements Event {
        private S3ObjectAnnotationType() {}

        @Override
        public String getValue() {
            return "s3:ObjectAnnotation:*";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectAnnotationPutType implements Event {
        private S3ObjectAnnotationPutType() {}

        @Override
        public String getValue() {
            return "s3:ObjectAnnotation:Put";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class S3ObjectAnnotationDeleteType implements Event {
        private S3ObjectAnnotationDeleteType() {}

        @Override
        public String getValue() {
            return "s3:ObjectAnnotation:Delete";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Event {
        public $Unknown {
            Objects.requireNonNull(value, "Value cannot be null");
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

        private final class $Hidden implements Event {
            @Override
            public String getValue() {
                return null;
            }
        }
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Event}.
     */
    final class Builder implements ShapeBuilder<Event> {
        private String value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        private Builder value(String value) {
            this.value = Objects.requireNonNull(value, "Enum value cannot be null");
            return this;
        }

        @Override
        public Event build() {
            return switch (value) {
                case "s3:ReducedRedundancyLostObject" -> S3_REDUCED_REDUNDANCY_LOST_OBJECT;
                case "s3:ObjectCreated:*" -> S3_OBJECT_CREATED_;
                case "s3:ObjectCreated:Put" -> S3_OBJECT_CREATED_PUT;
                case "s3:ObjectCreated:Post" -> S3_OBJECT_CREATED_POST;
                case "s3:ObjectCreated:Copy" -> S3_OBJECT_CREATED_COPY;
                case "s3:ObjectCreated:CompleteMultipartUpload" -> S3_OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD;
                case "s3:ObjectRemoved:*" -> S3_OBJECT_REMOVED_;
                case "s3:ObjectRemoved:Delete" -> S3_OBJECT_REMOVED_DELETE;
                case "s3:ObjectRemoved:DeleteMarkerCreated" -> S3_OBJECT_REMOVED_DELETE_MARKER_CREATED;
                case "s3:ObjectRestore:*" -> S3_OBJECT_RESTORE_;
                case "s3:ObjectRestore:Post" -> S3_OBJECT_RESTORE_POST;
                case "s3:ObjectRestore:Completed" -> S3_OBJECT_RESTORE_COMPLETED;
                case "s3:Replication:*" -> S3_REPLICATION_;
                case "s3:Replication:OperationFailedReplication" -> S3_REPLICATION_OPERATION_FAILED_REPLICATION;
                case "s3:Replication:OperationNotTracked" -> S3_REPLICATION_OPERATION_NOT_TRACKED;
                case "s3:Replication:OperationMissedThreshold" -> S3_REPLICATION_OPERATION_MISSED_THRESHOLD;
                case "s3:Replication:OperationReplicatedAfterThreshold" -> S3_REPLICATION_OPERATION_REPLICATED_AFTER_THRESHOLD;
                case "s3:ObjectRestore:Delete" -> S3_OBJECT_RESTORE_DELETE;
                case "s3:LifecycleTransition" -> S3_LIFECYCLE_TRANSITION;
                case "s3:IntelligentTiering" -> S3_INTELLIGENT_TIERING;
                case "s3:ObjectAcl:Put" -> S3_OBJECT_ACL_PUT;
                case "s3:LifecycleExpiration:*" -> S3_LIFECYCLE_EXPIRATION_;
                case "s3:LifecycleExpiration:Delete" -> S3_LIFECYCLE_EXPIRATION_DELETE;
                case "s3:LifecycleExpiration:DeleteMarkerCreated" -> S3_LIFECYCLE_EXPIRATION_DELETE_MARKER_CREATED;
                case "s3:ObjectTagging:*" -> S3_OBJECT_TAGGING_;
                case "s3:ObjectTagging:Put" -> S3_OBJECT_TAGGING_PUT;
                case "s3:ObjectTagging:Delete" -> S3_OBJECT_TAGGING_DELETE;
                case "s3:ObjectAnnotation:*" -> S3_OBJECT_ANNOTATION_;
                case "s3:ObjectAnnotation:Put" -> S3_OBJECT_ANNOTATION_PUT;
                case "s3:ObjectAnnotation:Delete" -> S3_OBJECT_ANNOTATION_DELETE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
