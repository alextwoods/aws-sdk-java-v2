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
 * Specifies the information about the bucket that will be created. For more information about directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-overview.html">
 * Directory buckets</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>This functionality is only supported by directory buckets.
 */
@SmithyGenerated
public final class BucketInfo implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BUCKET_INFO;
    private static final Schema $SCHEMA_DATA_REDUNDANCY = $SCHEMA.member("DataRedundancy");
    private static final Schema $SCHEMA_TYPE = $SCHEMA.member("Type");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient DataRedundancy dataRedundancy;
    private final transient BucketType type;

    private BucketInfo(Builder builder) {
        this.dataRedundancy = builder.dataRedundancy;
        this.type = builder.type;
    }

    /**
     * The number of Zone (Availability Zone or Local Zone) that's used for redundancy for the bucket.
     */
    public DataRedundancy getDataRedundancy() {
        return dataRedundancy;
    }

    /**
     * The type of bucket.
     */
    public BucketType getType() {
        return type;
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
        BucketInfo that = (BucketInfo) other;
        return Objects.equals(this.dataRedundancy, that.dataRedundancy)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dataRedundancy);
        $hc = 31 * $hc + Objects.hashCode(type);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dataRedundancy != null) {
            serializer.writeString($SCHEMA_DATA_REDUNDANCY, dataRedundancy.getValue());
        }
        if (type != null) {
            serializer.writeString($SCHEMA_TYPE, type.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATA_REDUNDANCY, member, dataRedundancy);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, type);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BucketInfo}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dataRedundancy(this.dataRedundancy);
        builder.type(this.type);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BucketInfo}.
     */
    public static final class Builder implements ShapeBuilder<BucketInfo> {
        private DataRedundancy dataRedundancy;
        private BucketType type;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The number of Zone (Availability Zone or Local Zone) that's used for redundancy for the bucket.
         *
         * @return this builder.
         */
        public Builder dataRedundancy(DataRedundancy dataRedundancy) {
            this.dataRedundancy = dataRedundancy;
            return this;
        }

        /**
         * The type of bucket.
         *
         * @return this builder.
         */
        public Builder type(BucketType type) {
            this.type = type;
            return this;
        }

        @Override
        public BucketInfo build() {
            return new BucketInfo(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dataRedundancy((DataRedundancy) SchemaUtils.validateSameMember($SCHEMA_DATA_REDUNDANCY, member, value));
                case 1 -> type((BucketType) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, value));
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
                    case 0 -> builder.dataRedundancy(DataRedundancy.builder().deserializeMember(de, member).build());
                    case 1 -> builder.type(BucketType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
