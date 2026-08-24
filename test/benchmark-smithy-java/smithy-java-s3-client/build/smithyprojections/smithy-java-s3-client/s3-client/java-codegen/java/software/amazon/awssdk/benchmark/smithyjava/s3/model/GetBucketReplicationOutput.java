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

@SmithyGenerated
public final class GetBucketReplicationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_BUCKET_REPLICATION_OUTPUT;
    private static final Schema $SCHEMA_REPLICATION_CONFIGURATION = $SCHEMA.member("ReplicationConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ReplicationConfiguration replicationConfiguration;

    private GetBucketReplicationOutput(Builder builder) {
        this.replicationConfiguration = builder.replicationConfiguration;
    }

    public ReplicationConfiguration getReplicationConfiguration() {
        return replicationConfiguration;
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
        GetBucketReplicationOutput that = (GetBucketReplicationOutput) other;
        return Objects.equals(this.replicationConfiguration, that.replicationConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(replicationConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (replicationConfiguration != null) {
            serializer.writeStruct($SCHEMA_REPLICATION_CONFIGURATION, replicationConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_CONFIGURATION, member, replicationConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketReplicationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.replicationConfiguration(this.replicationConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketReplicationOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketReplicationOutput> {
        private ReplicationConfiguration replicationConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder replicationConfiguration(ReplicationConfiguration replicationConfiguration) {
            this.replicationConfiguration = replicationConfiguration;
            return this;
        }

        @Override
        public GetBucketReplicationOutput build() {
            return new GetBucketReplicationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> replicationConfiguration((ReplicationConfiguration) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_CONFIGURATION, member, value));
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
                    case 0 -> builder.replicationConfiguration(ReplicationConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
