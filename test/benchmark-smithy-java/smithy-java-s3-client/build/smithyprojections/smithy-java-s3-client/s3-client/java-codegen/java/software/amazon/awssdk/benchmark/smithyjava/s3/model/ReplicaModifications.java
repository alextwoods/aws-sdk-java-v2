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
 * A filter that you can specify for selection for modifications on replicas. Amazon S3 doesn't replicate replica
 * modifications by default. In the latest version of replication configuration (when <code>Filter</code> is specified),
 * you can specify this element and set the status to <code>Enabled</code> to replicate modifications on replicas.
 *
 * <p> If you don't specify the <code>Filter</code> element, Amazon S3 assumes that the replication configuration is the
 * earlier version, V1. In the earlier version, this element is not allowed.
 */
@SmithyGenerated
public final class ReplicaModifications implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REPLICA_MODIFICATIONS;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ReplicaModificationsStatus status;

    private ReplicaModifications(Builder builder) {
        this.status = builder.status;
    }

    /**
     * Specifies whether Amazon S3 replicates modifications on replicas.
     */
    public ReplicaModificationsStatus getStatus() {
        return status;
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
        ReplicaModifications that = (ReplicaModifications) other;
        return Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(status);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_STATUS, status.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaModifications}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.status(this.status);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaModifications}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaModifications> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ReplicaModificationsStatus status;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether Amazon S3 replicates modifications on replicas.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(ReplicaModificationsStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        @Override
        public ReplicaModifications build() {
            tracker.validate();
            return new ReplicaModifications(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((ReplicaModificationsStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicaModifications> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(ReplicaModificationsStatus.unknown(""));
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
                    case 0 -> builder.status(ReplicaModificationsStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
