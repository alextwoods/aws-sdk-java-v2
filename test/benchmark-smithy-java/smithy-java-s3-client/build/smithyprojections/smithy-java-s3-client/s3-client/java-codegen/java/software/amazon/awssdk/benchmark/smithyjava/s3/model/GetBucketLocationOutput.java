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
public final class GetBucketLocationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_LOCATION_OUTPUT;
    private static final Schema $SCHEMA_LOCATION_CONSTRAINT = $SCHEMA.member("LocationConstraint");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BucketLocationConstraint locationConstraint;

    private GetBucketLocationOutput(Builder builder) {
        this.locationConstraint = builder.locationConstraint;
    }

    /**
     * Specifies the Region where the bucket resides. For a list of all the Amazon S3 supported location constraints by
     * Region, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a>.
     *
     * <p>Buckets in Region <code>us-east-1</code> have a LocationConstraint of <code>null</code>. Buckets with a
     * LocationConstraint of <code>EU</code> reside in <code>eu-west-1</code>.
     */
    public BucketLocationConstraint getLocationConstraint() {
        return locationConstraint;
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
        GetBucketLocationOutput that = (GetBucketLocationOutput) other;
        return Objects.equals(this.locationConstraint, that.locationConstraint);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(locationConstraint);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (locationConstraint != null) {
            serializer.writeString($SCHEMA_LOCATION_CONSTRAINT, locationConstraint.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCATION_CONSTRAINT, member, locationConstraint);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketLocationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.locationConstraint(this.locationConstraint);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketLocationOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketLocationOutput> {
        private BucketLocationConstraint locationConstraint;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the Region where the bucket resides. For a list of all the Amazon S3 supported location constraints by
         * Region, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a>.
         *
         * <p>Buckets in Region <code>us-east-1</code> have a LocationConstraint of <code>null</code>. Buckets with a
         * LocationConstraint of <code>EU</code> reside in <code>eu-west-1</code>.
         *
         * @return this builder.
         */
        public Builder locationConstraint(BucketLocationConstraint locationConstraint) {
            this.locationConstraint = locationConstraint;
            return this;
        }

        @Override
        public GetBucketLocationOutput build() {
            return new GetBucketLocationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> locationConstraint((BucketLocationConstraint) SchemaUtils.validateSameMember($SCHEMA_LOCATION_CONSTRAINT, member, value));
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
                    case 0 -> builder.locationConstraint(BucketLocationConstraint.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
