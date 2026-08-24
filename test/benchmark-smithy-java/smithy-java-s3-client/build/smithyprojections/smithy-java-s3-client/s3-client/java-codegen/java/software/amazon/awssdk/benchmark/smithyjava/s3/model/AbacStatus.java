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
 * The ABAC status of the general purpose bucket. When ABAC is enabled for the general purpose bucket, you can use tags
 * to manage access to the general purpose buckets as well as for cost tracking purposes. When ABAC is disabled for the
 * general purpose buckets, you can only use tags for cost tracking purposes. For more information, see <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/buckets-tagging.html">Using tags with S3 general purpose
 * buckets</a>.
 */
@SmithyGenerated
public final class AbacStatus implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ABAC_STATUS;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BucketAbacStatus status;

    private AbacStatus(Builder builder) {
        this.status = builder.status;
    }

    /**
     * The ABAC status of the general purpose bucket.
     */
    public BucketAbacStatus getStatus() {
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
        AbacStatus that = (AbacStatus) other;
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
     * @return a builder for {@link AbacStatus}.
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
     * Builder for {@link AbacStatus}.
     */
    public static final class Builder implements ShapeBuilder<AbacStatus> {
        private BucketAbacStatus status;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ABAC status of the general purpose bucket.
         *
         * @return this builder.
         */
        public Builder status(BucketAbacStatus status) {
            this.status = status;
            return this;
        }

        @Override
        public AbacStatus build() {
            return new AbacStatus(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((BucketAbacStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
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
                    case 0 -> builder.status(BucketAbacStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
