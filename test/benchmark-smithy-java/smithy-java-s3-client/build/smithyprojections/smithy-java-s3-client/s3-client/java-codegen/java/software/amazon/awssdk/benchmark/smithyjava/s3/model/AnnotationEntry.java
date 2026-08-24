package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
 * Describes a single annotation attached to an object, including its name, last modified time, size, ETag, checksum
 * algorithm, and replication status. Returned in the response from <code>ListObjectAnnotations</code>.
 */
@SmithyGenerated
public final class AnnotationEntry implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.ANNOTATION_ENTRY;
    private static final Schema $SCHEMA_ANNOTATION_NAME = $SCHEMA.member("AnnotationName");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_SIZE = $SCHEMA.member("Size");
    private static final Schema $SCHEMA_REPLICATION_STATUS = $SCHEMA.member("ReplicationStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String annotationName;
    private final transient Instant lastModified;
    private final transient String eTag;
    private final transient List<ChecksumAlgorithm> checksumAlgorithm;
    private final transient long size;
    private final transient ReplicationStatus replicationStatus;

    private AnnotationEntry(Builder builder) {
        this.annotationName = builder.annotationName;
        this.lastModified = builder.lastModified;
        this.eTag = builder.eTag;
        this.checksumAlgorithm = builder.checksumAlgorithm == null ? null : Collections.unmodifiableList(builder.checksumAlgorithm);
        this.size = builder.size;
        this.replicationStatus = builder.replicationStatus;
    }

    /**
     * The name of the annotation.
     */
    public String getAnnotationName() {
        return annotationName;
    }

    /**
     * The date and time the annotation was last modified.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * The entity tag of the annotation.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * The checksum algorithm used for the annotation.
     */
    public List<ChecksumAlgorithm> getChecksumAlgorithm() {
        if (checksumAlgorithm == null) {
            return Collections.emptyList();
        }
        return checksumAlgorithm;
    }

    public boolean hasChecksumAlgorithm() {
        return checksumAlgorithm != null;
    }

    /**
     * The size of the annotation payload, in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * The replication status of the annotation.
     */
    public ReplicationStatus getReplicationStatus() {
        return replicationStatus;
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
        AnnotationEntry that = (AnnotationEntry) other;
        return this.size == that.size
               && Objects.equals(this.annotationName, that.annotationName)
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.replicationStatus, that.replicationStatus)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(annotationName);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Long.hashCode(size);
        $hc = 31 * $hc + Objects.hashCode(replicationStatus);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ANNOTATION_NAME, annotationName);
        serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (checksumAlgorithm != null) {
            serializer.writeList($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm, checksumAlgorithm.size(), SharedSerde.ChecksumAlgorithmListSerializer.INSTANCE);
        }
        serializer.writeLong($SCHEMA_SIZE, size);
        if (replicationStatus != null) {
            serializer.writeString($SCHEMA_REPLICATION_STATUS, replicationStatus.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, annotationName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, size);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_STATUS, member, replicationStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnnotationEntry}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.annotationName(this.annotationName);
        builder.lastModified(this.lastModified);
        builder.eTag(this.eTag);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.size(this.size);
        builder.replicationStatus(this.replicationStatus);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AnnotationEntry}.
     */
    public static final class Builder implements ShapeBuilder<AnnotationEntry> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String annotationName;
        private Instant lastModified;
        private String eTag;
        private List<ChecksumAlgorithm> checksumAlgorithm;
        private long size;
        private ReplicationStatus replicationStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the annotation.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder annotationName(String annotationName) {
            this.annotationName = Objects.requireNonNull(annotationName, "annotationName cannot be null");
            tracker.setMember($SCHEMA_ANNOTATION_NAME);
            return this;
        }

        /**
         * The date and time the annotation was last modified.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = Objects.requireNonNull(lastModified, "lastModified cannot be null");
            tracker.setMember($SCHEMA_LAST_MODIFIED);
            return this;
        }

        /**
         * The entity tag of the annotation.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * The checksum algorithm used for the annotation.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(List<ChecksumAlgorithm> checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * The size of the annotation payload, in bytes.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder size(long size) {
            this.size = size;
            tracker.setMember($SCHEMA_SIZE);
            return this;
        }

        /**
         * The replication status of the annotation.
         *
         * @return this builder.
         */
        public Builder replicationStatus(ReplicationStatus replicationStatus) {
            this.replicationStatus = replicationStatus;
            return this;
        }

        @Override
        public AnnotationEntry build() {
            tracker.validate();
            return new AnnotationEntry(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> annotationName((String) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, value));
                case 1 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 2 -> size((long) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, value));
                case 3 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 4 -> checksumAlgorithm((List<ChecksumAlgorithm>) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 5 -> replicationStatus((ReplicationStatus) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_STATUS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AnnotationEntry> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ANNOTATION_NAME)) {
                annotationName("");
            }
            if (!tracker.checkMember($SCHEMA_LAST_MODIFIED)) {
                lastModified(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_SIZE)) {
                tracker.setMember($SCHEMA_SIZE);
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
                    case 0 -> builder.annotationName(de.readString(member));
                    case 1 -> builder.lastModified(de.readTimestamp(member));
                    case 2 -> builder.size(de.readLong(member));
                    case 3 -> builder.eTag(de.readString(member));
                    case 4 -> builder.checksumAlgorithm(SharedSerde.deserializeChecksumAlgorithmList(member, de));
                    case 5 -> builder.replicationStatus(ReplicationStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
