package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Specifies which Amazon S3 objects to replicate and where to store the replicas.
 */
@SmithyGenerated
public final class ReplicationRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REPLICATION_RULE;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("ID");
    private static final Schema $SCHEMA_PRIORITY = $SCHEMA.member("Priority");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_SOURCE_SELECTION_CRITERIA = $SCHEMA.member("SourceSelectionCriteria");
    private static final Schema $SCHEMA_EXISTING_OBJECT_REPLICATION = $SCHEMA.member("ExistingObjectReplication");
    private static final Schema $SCHEMA_DESTINATION = $SCHEMA.member("Destination");
    private static final Schema $SCHEMA_DELETE_MARKER_REPLICATION = $SCHEMA.member("DeleteMarkerReplication");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient Integer priority;
    private final transient String prefix;
    private final transient ReplicationRuleFilter filter;
    private final transient ReplicationRuleStatus status;
    private final transient SourceSelectionCriteria sourceSelectionCriteria;
    private final transient ExistingObjectReplication existingObjectReplication;
    private final transient Destination destination;
    private final transient DeleteMarkerReplication deleteMarkerReplication;

    private ReplicationRule(Builder builder) {
        this.id = builder.id;
        this.priority = builder.priority;
        this.prefix = builder.prefix;
        this.filter = builder.filter;
        this.status = builder.status;
        this.sourceSelectionCriteria = builder.sourceSelectionCriteria;
        this.existingObjectReplication = builder.existingObjectReplication;
        this.destination = builder.destination;
        this.deleteMarkerReplication = builder.deleteMarkerReplication;
    }

    /**
     * A unique identifier for the rule. The maximum value is 255 characters.
     */
    public String getId() {
        return id;
    }

    /**
     * The priority indicates which rule has precedence whenever two or more replication rules conflict. Amazon S3 will
     * attempt to replicate objects according to all replication rules. However, if there are two or more rules with the
     * same destination bucket, then objects will be replicated according to the rule with the highest priority. The
     * higher the number, the higher the priority.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication.html">Replication</a> in the <i>Amazon S3 User Guide</i>.
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * An object key name prefix that identifies the object or objects to which the rule applies. The maximum prefix
     * length is 1,024 characters. To include all objects in a bucket, specify an empty string.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     *
     * @deprecated
     */
    @Deprecated
    public String getPrefix() {
        return prefix;
    }

    public ReplicationRuleFilter getFilter() {
        return filter;
    }

    /**
     * Specifies whether the rule is enabled.
     */
    public ReplicationRuleStatus getStatus() {
        return status;
    }

    /**
     * A container that describes additional filters for identifying the source objects that you want to replicate. You
     * can choose to enable or disable the replication of these objects. Currently, Amazon S3 supports only the filter
     * that you can specify for objects created with server-side encryption using a customer managed key stored in
     * Amazon Web Services Key Management Service (SSE-KMS).
     */
    public SourceSelectionCriteria getSourceSelectionCriteria() {
        return sourceSelectionCriteria;
    }

    /**
     * Optional configuration to replicate existing source bucket objects.
     *
     * <p>This parameter is no longer supported. To replicate existing objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-batch-replication-batch.html">Replicating existing objects with
     * S3 Batch Replication</a> in the <i>Amazon S3 User Guide</i>.
     */
    public ExistingObjectReplication getExistingObjectReplication() {
        return existingObjectReplication;
    }

    /**
     * A container for information about the replication destination and its configurations including enabling the S3
     * Replication Time Control (S3 RTC).
     */
    public Destination getDestination() {
        return destination;
    }

    public DeleteMarkerReplication getDeleteMarkerReplication() {
        return deleteMarkerReplication;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ReplicationRule that = (ReplicationRule) other;
        return Objects.equals(this.priority, that.priority)
               && Objects.equals(this.id, that.id)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.filter, that.filter)
               && Objects.equals(this.sourceSelectionCriteria, that.sourceSelectionCriteria)
               && Objects.equals(this.existingObjectReplication, that.existingObjectReplication)
               && Objects.equals(this.destination, that.destination)
               && Objects.equals(this.deleteMarkerReplication, that.deleteMarkerReplication);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(priority);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(filter);
        $hc = 31 * $hc + Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(sourceSelectionCriteria);
        $hc = 31 * $hc + Objects.hashCode(existingObjectReplication);
        $hc = 31 * $hc + Objects.hashCode(destination);
        $hc = 31 * $hc + Objects.hashCode(deleteMarkerReplication);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (priority != null) {
            serializer.writeInteger($SCHEMA_PRIORITY, priority);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (filter != null) {
            serializer.writeStruct($SCHEMA_FILTER, filter);
        }
        serializer.writeString($SCHEMA_STATUS, status.getValue());
        if (sourceSelectionCriteria != null) {
            serializer.writeStruct($SCHEMA_SOURCE_SELECTION_CRITERIA, sourceSelectionCriteria);
        }
        if (existingObjectReplication != null) {
            serializer.writeStruct($SCHEMA_EXISTING_OBJECT_REPLICATION, existingObjectReplication);
        }
        if (destination != null) {
            serializer.writeStruct($SCHEMA_DESTINATION, destination);
        }
        if (deleteMarkerReplication != null) {
            serializer.writeStruct($SCHEMA_DELETE_MARKER_REPLICATION, deleteMarkerReplication);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION, member, destination);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PRIORITY, member, priority);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_SELECTION_CRITERIA, member, sourceSelectionCriteria);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXISTING_OBJECT_REPLICATION, member, existingObjectReplication);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER_REPLICATION, member, deleteMarkerReplication);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicationRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.priority(this.priority);
        builder.prefix(this.prefix);
        builder.filter(this.filter);
        builder.status(this.status);
        builder.sourceSelectionCriteria(this.sourceSelectionCriteria);
        builder.existingObjectReplication(this.existingObjectReplication);
        builder.destination(this.destination);
        builder.deleteMarkerReplication(this.deleteMarkerReplication);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicationRule}.
     */
    public static final class Builder implements ShapeBuilder<ReplicationRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private Integer priority;
        private String prefix;
        private ReplicationRuleFilter filter;
        private ReplicationRuleStatus status;
        private SourceSelectionCriteria sourceSelectionCriteria;
        private ExistingObjectReplication existingObjectReplication;
        private Destination destination;
        private DeleteMarkerReplication deleteMarkerReplication;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A unique identifier for the rule. The maximum value is 255 characters.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The priority indicates which rule has precedence whenever two or more replication rules conflict. Amazon S3 will
         * attempt to replicate objects according to all replication rules. However, if there are two or more rules with the
         * same destination bucket, then objects will be replicated according to the rule with the highest priority. The
         * higher the number, the higher the priority.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication.html">Replication</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        /**
         * An object key name prefix that identifies the object or objects to which the rule applies. The maximum prefix
         * length is 1,024 characters. To include all objects in a bucket, specify an empty string.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * @return this builder.
         * @deprecated
         */
        @Deprecated
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder filter(ReplicationRuleFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Specifies whether the rule is enabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(ReplicationRuleStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        /**
         * A container that describes additional filters for identifying the source objects that you want to replicate. You
         * can choose to enable or disable the replication of these objects. Currently, Amazon S3 supports only the filter
         * that you can specify for objects created with server-side encryption using a customer managed key stored in
         * Amazon Web Services Key Management Service (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder sourceSelectionCriteria(SourceSelectionCriteria sourceSelectionCriteria) {
            this.sourceSelectionCriteria = sourceSelectionCriteria;
            return this;
        }

        /**
         * Optional configuration to replicate existing source bucket objects.
         *
         * <p>This parameter is no longer supported. To replicate existing objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-batch-replication-batch.html">Replicating existing objects with
         * S3 Batch Replication</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder existingObjectReplication(ExistingObjectReplication existingObjectReplication) {
            this.existingObjectReplication = existingObjectReplication;
            return this;
        }

        /**
         * A container for information about the replication destination and its configurations including enabling the S3
         * Replication Time Control (S3 RTC).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder destination(Destination destination) {
            this.destination = Objects.requireNonNull(destination, "destination cannot be null");
            tracker.setMember($SCHEMA_DESTINATION);
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder deleteMarkerReplication(DeleteMarkerReplication deleteMarkerReplication) {
            this.deleteMarkerReplication = deleteMarkerReplication;
            return this;
        }

        @Override
        public ReplicationRule build() {
            tracker.validate();
            return new ReplicationRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((ReplicationRuleStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 1 -> destination((Destination) SchemaUtils.validateSameMember($SCHEMA_DESTINATION, member, value));
                case 2 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 3 -> priority((Integer) SchemaUtils.validateSameMember($SCHEMA_PRIORITY, member, value));
                case 4 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 5 -> filter((ReplicationRuleFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                case 6 -> sourceSelectionCriteria((SourceSelectionCriteria) SchemaUtils.validateSameMember($SCHEMA_SOURCE_SELECTION_CRITERIA, member, value));
                case 7 -> existingObjectReplication((ExistingObjectReplication) SchemaUtils.validateSameMember($SCHEMA_EXISTING_OBJECT_REPLICATION, member, value));
                case 8 -> deleteMarkerReplication((DeleteMarkerReplication) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER_REPLICATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicationRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(ReplicationRuleStatus.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_DESTINATION)) {
                tracker.setMember($SCHEMA_DESTINATION);
            }
            return this;
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.status(ReplicationRuleStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.destination(Destination.builder().deserializeMember(de, member).build());
                    case 2 -> builder.id(de.readString(member));
                    case 3 -> builder.priority(de.readInteger(member));
                    case 4 -> builder.prefix(de.readString(member));
                    case 5 -> builder.filter(ReplicationRuleFilter.builder().deserializeMember(de, member).build());
                    case 6 -> builder.sourceSelectionCriteria(SourceSelectionCriteria.builder().deserializeMember(de, member).build());
                    case 7 -> builder.existingObjectReplication(ExistingObjectReplication.builder().deserializeMember(de, member).build());
                    case 8 -> builder.deleteMarkerReplication(DeleteMarkerReplication.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
