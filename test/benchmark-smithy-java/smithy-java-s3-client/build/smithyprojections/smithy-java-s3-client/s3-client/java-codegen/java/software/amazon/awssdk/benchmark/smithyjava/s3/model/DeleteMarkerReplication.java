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
 * Specifies whether Amazon S3 replicates delete markers. If you specify a <code>Filter</code> in your replication
 * configuration, you must also include a <code>DeleteMarkerReplication</code> element. If your <code>Filter</code>
 * includes a <code>Tag</code> element, the <code>DeleteMarkerReplication</code><code>Status</code> must be set to
 * Disabled, because Amazon S3 does not support replicating delete markers for tag-based rules. For an example
 * configuration, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-add-config.html#replication-config-min-rule-config">Basic Rule Configuration</a>.
 *
 * <p>For more information about delete marker replication, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/delete-marker-replication.html">Basic Rule Configuration</a>.
 *
 * <p>If you are using an earlier version of the replication configuration, Amazon S3 handles replication of delete
 * markers differently. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-add-config.html#replication-backward-compat-considerations">Backward Compatibility</a>.
 */
@SmithyGenerated
public final class DeleteMarkerReplication implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.DELETE_MARKER_REPLICATION;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient DeleteMarkerReplicationStatus status;

    private DeleteMarkerReplication(Builder builder) {
        this.status = builder.status;
    }

    /**
     * Indicates whether to replicate delete markers.
     *
     * <p>Indicates whether to replicate delete markers.
     */
    public DeleteMarkerReplicationStatus getStatus() {
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
        DeleteMarkerReplication that = (DeleteMarkerReplication) other;
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
        if (status != null) {
            serializer.writeString($SCHEMA_STATUS, status.getValue());
        }
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
     * @return a builder for {@link DeleteMarkerReplication}.
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
     * Builder for {@link DeleteMarkerReplication}.
     */
    public static final class Builder implements ShapeBuilder<DeleteMarkerReplication> {
        private DeleteMarkerReplicationStatus status;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether to replicate delete markers.
         *
         * <p>Indicates whether to replicate delete markers.
         *
         * @return this builder.
         */
        public Builder status(DeleteMarkerReplicationStatus status) {
            this.status = status;
            return this;
        }

        @Override
        public DeleteMarkerReplication build() {
            return new DeleteMarkerReplication(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((DeleteMarkerReplicationStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
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
                    case 0 -> builder.status(DeleteMarkerReplicationStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
