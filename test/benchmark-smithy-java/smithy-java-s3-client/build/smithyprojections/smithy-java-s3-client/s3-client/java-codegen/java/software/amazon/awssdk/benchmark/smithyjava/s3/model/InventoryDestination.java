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
 * Specifies the S3 Inventory configuration for an Amazon S3 bucket.
 */
@SmithyGenerated
public final class InventoryDestination implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_DESTINATION;
    private static final Schema $SCHEMA_S3_BUCKET_DESTINATION = $SCHEMA.member("S3BucketDestination");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient InventoryS3BucketDestination s3BucketDestination;

    private InventoryDestination(Builder builder) {
        this.s3BucketDestination = builder.s3BucketDestination;
    }

    /**
     * Contains the bucket name, file format, bucket owner (optional), and prefix (optional) where inventory results are
     * published.
     */
    public InventoryS3BucketDestination getS3BucketDestination() {
        return s3BucketDestination;
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
        InventoryDestination that = (InventoryDestination) other;
        return Objects.equals(this.s3BucketDestination, that.s3BucketDestination);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(s3BucketDestination);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (s3BucketDestination != null) {
            serializer.writeStruct($SCHEMA_S3_BUCKET_DESTINATION, s3BucketDestination);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_DESTINATION, member, s3BucketDestination);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryDestination}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.s3BucketDestination(this.s3BucketDestination);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryDestination}.
     */
    public static final class Builder implements ShapeBuilder<InventoryDestination> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private InventoryS3BucketDestination s3BucketDestination;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Contains the bucket name, file format, bucket owner (optional), and prefix (optional) where inventory results are
         * published.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder s3BucketDestination(InventoryS3BucketDestination s3BucketDestination) {
            this.s3BucketDestination = Objects.requireNonNull(s3BucketDestination, "s3BucketDestination cannot be null");
            tracker.setMember($SCHEMA_S3_BUCKET_DESTINATION);
            return this;
        }

        @Override
        public InventoryDestination build() {
            tracker.validate();
            return new InventoryDestination(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s3BucketDestination((InventoryS3BucketDestination) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_DESTINATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventoryDestination> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_S3_BUCKET_DESTINATION)) {
                tracker.setMember($SCHEMA_S3_BUCKET_DESTINATION);
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
                    case 0 -> builder.s3BucketDestination(InventoryS3BucketDestination.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
