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
public final class GetBucketVersioningOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_BUCKET_VERSIONING_OUTPUT;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_MFA_DELETE = $SCHEMA.member("MFADelete");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BucketVersioningStatus status;
    private final transient MFADeleteStatus mfaDelete;

    private GetBucketVersioningOutput(Builder builder) {
        this.status = builder.status;
        this.mfaDelete = builder.mfaDelete;
    }

    /**
     * The versioning state of the bucket.
     */
    public BucketVersioningStatus getStatus() {
        return status;
    }

    /**
     * Specifies whether MFA delete is enabled in the bucket versioning configuration. This element is only returned if
     * the bucket has been configured with MFA delete. If the bucket has never been so configured, this element is not
     * returned.
     */
    public MFADeleteStatus getMfaDelete() {
        return mfaDelete;
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
        GetBucketVersioningOutput that = (GetBucketVersioningOutput) other;
        return Objects.equals(this.status, that.status)
               && Objects.equals(this.mfaDelete, that.mfaDelete);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(mfaDelete);
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
        if (mfaDelete != null) {
            serializer.writeString($SCHEMA_MFA_DELETE, mfaDelete.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MFA_DELETE, member, mfaDelete);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketVersioningOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.status(this.status);
        builder.mfaDelete(this.mfaDelete);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketVersioningOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketVersioningOutput> {
        private BucketVersioningStatus status;
        private MFADeleteStatus mfaDelete;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The versioning state of the bucket.
         *
         * @return this builder.
         */
        public Builder status(BucketVersioningStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Specifies whether MFA delete is enabled in the bucket versioning configuration. This element is only returned if
         * the bucket has been configured with MFA delete. If the bucket has never been so configured, this element is not
         * returned.
         *
         * @return this builder.
         */
        public Builder mfaDelete(MFADeleteStatus mfaDelete) {
            this.mfaDelete = mfaDelete;
            return this;
        }

        @Override
        public GetBucketVersioningOutput build() {
            return new GetBucketVersioningOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((BucketVersioningStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 1 -> mfaDelete((MFADeleteStatus) SchemaUtils.validateSameMember($SCHEMA_MFA_DELETE, member, value));
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
                    case 0 -> builder.status(BucketVersioningStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.mfaDelete(MFADeleteStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
