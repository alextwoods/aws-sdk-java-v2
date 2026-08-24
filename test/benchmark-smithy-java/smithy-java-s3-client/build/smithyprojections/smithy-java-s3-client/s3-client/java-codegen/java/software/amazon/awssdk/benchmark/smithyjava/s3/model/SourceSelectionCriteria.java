package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
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
 * A container that describes additional filters for identifying the source objects that you want to replicate. You can
 * choose to enable or disable the replication of these objects. Currently, Amazon S3 supports only the filter that you
 * can specify for objects created with server-side encryption using a customer managed key stored in Amazon Web
 * Services Key Management Service (SSE-KMS).
 */
@SmithyGenerated
public final class SourceSelectionCriteria implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.SOURCE_SELECTION_CRITERIA;
    private static final Schema $SCHEMA_SSE_KMS_ENCRYPTED_OBJECTS = $SCHEMA.member("SseKmsEncryptedObjects");
    private static final Schema $SCHEMA_REPLICA_MODIFICATIONS = $SCHEMA.member("ReplicaModifications");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient SseKmsEncryptedObjects sseKmsEncryptedObjects;
    private final transient ReplicaModifications replicaModifications;

    private SourceSelectionCriteria(Builder builder) {
        this.sseKmsEncryptedObjects = builder.sseKmsEncryptedObjects;
        this.replicaModifications = builder.replicaModifications;
    }

    /**
     * A container for filter information for the selection of Amazon S3 objects encrypted with Amazon Web Services KMS.
     * If you include <code>SourceSelectionCriteria</code> in the replication configuration, this element is required.
     */
    public SseKmsEncryptedObjects getSseKmsEncryptedObjects() {
        return sseKmsEncryptedObjects;
    }

    /**
     * A filter that you can specify for selections for modifications on replicas. Amazon S3 doesn't replicate replica
     * modifications by default. In the latest version of replication configuration (when <code>Filter</code> is
     * specified), you can specify this element and set the status to <code>Enabled</code> to replicate modifications on
     * replicas.
     *
     * <p> If you don't specify the <code>Filter</code> element, Amazon S3 assumes that the replication configuration is
     * the earlier version, V1. In the earlier version, this element is not allowed
     */
    public ReplicaModifications getReplicaModifications() {
        return replicaModifications;
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
        SourceSelectionCriteria that = (SourceSelectionCriteria) other;
        return Objects.equals(this.sseKmsEncryptedObjects, that.sseKmsEncryptedObjects)
               && Objects.equals(this.replicaModifications, that.replicaModifications);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sseKmsEncryptedObjects);
        $hc = 31 * $hc + Objects.hashCode(replicaModifications);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sseKmsEncryptedObjects != null) {
            serializer.writeStruct($SCHEMA_SSE_KMS_ENCRYPTED_OBJECTS, sseKmsEncryptedObjects);
        }
        if (replicaModifications != null) {
            serializer.writeStruct($SCHEMA_REPLICA_MODIFICATIONS, replicaModifications);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_KMS_ENCRYPTED_OBJECTS, member, sseKmsEncryptedObjects);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_MODIFICATIONS, member, replicaModifications);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SourceSelectionCriteria}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sseKmsEncryptedObjects(this.sseKmsEncryptedObjects);
        builder.replicaModifications(this.replicaModifications);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SourceSelectionCriteria}.
     */
    public static final class Builder implements ShapeBuilder<SourceSelectionCriteria> {
        private SseKmsEncryptedObjects sseKmsEncryptedObjects;
        private ReplicaModifications replicaModifications;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A container for filter information for the selection of Amazon S3 objects encrypted with Amazon Web Services KMS.
         * If you include <code>SourceSelectionCriteria</code> in the replication configuration, this element is required.
         *
         * @return this builder.
         */
        public Builder sseKmsEncryptedObjects(SseKmsEncryptedObjects sseKmsEncryptedObjects) {
            this.sseKmsEncryptedObjects = sseKmsEncryptedObjects;
            return this;
        }

        /**
         * A filter that you can specify for selections for modifications on replicas. Amazon S3 doesn't replicate replica
         * modifications by default. In the latest version of replication configuration (when <code>Filter</code> is
         * specified), you can specify this element and set the status to <code>Enabled</code> to replicate modifications on
         * replicas.
         *
         * <p> If you don't specify the <code>Filter</code> element, Amazon S3 assumes that the replication configuration is
         * the earlier version, V1. In the earlier version, this element is not allowed
         *
         * @return this builder.
         */
        public Builder replicaModifications(ReplicaModifications replicaModifications) {
            this.replicaModifications = replicaModifications;
            return this;
        }

        @Override
        public SourceSelectionCriteria build() {
            return new SourceSelectionCriteria(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> sseKmsEncryptedObjects((SseKmsEncryptedObjects) SchemaUtils.validateSameMember($SCHEMA_SSE_KMS_ENCRYPTED_OBJECTS, member, value));
                case 1 -> replicaModifications((ReplicaModifications) SchemaUtils.validateSameMember($SCHEMA_REPLICA_MODIFICATIONS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
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
                    case 0 -> builder.sseKmsEncryptedObjects(SseKmsEncryptedObjects.builder().deserializeMember(de, member).build());
                    case 1 -> builder.replicaModifications(ReplicaModifications.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
