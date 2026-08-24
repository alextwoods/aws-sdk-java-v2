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
 * Describes the versioning state of an Amazon S3 bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTVersioningStatus.html">PUT Bucket versioning</a> in the <i>
 * Amazon S3 API Reference</i>.
 */
@SmithyGenerated
public final class VersioningConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.VERSIONING_CONFIGURATION;
    private static final Schema $SCHEMA_MFA_DELETE = $SCHEMA.member("MFADelete");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient MFADelete mfaDelete;
    private final transient BucketVersioningStatus status;

    private VersioningConfiguration(Builder builder) {
        this.mfaDelete = builder.mfaDelete;
        this.status = builder.status;
    }

    /**
     * Specifies whether MFA delete is enabled in the bucket versioning configuration. This element is only returned if
     * the bucket has been configured with MFA delete. If the bucket has never been so configured, this element is not
     * returned.
     */
    public MFADelete getMfaDelete() {
        return mfaDelete;
    }

    /**
     * The versioning state of the bucket.
     */
    public BucketVersioningStatus getStatus() {
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
        VersioningConfiguration that = (VersioningConfiguration) other;
        return Objects.equals(this.mfaDelete, that.mfaDelete)
               && Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(mfaDelete);
        $hc = 31 * $hc + Objects.hashCode(status);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (mfaDelete != null) {
            serializer.writeString($SCHEMA_MFA_DELETE, mfaDelete.getValue());
        }
        if (status != null) {
            serializer.writeString($SCHEMA_STATUS, status.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MFA_DELETE, member, mfaDelete);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link VersioningConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.mfaDelete(this.mfaDelete);
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
     * Builder for {@link VersioningConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<VersioningConfiguration> {
        private MFADelete mfaDelete;
        private BucketVersioningStatus status;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether MFA delete is enabled in the bucket versioning configuration. This element is only returned if
         * the bucket has been configured with MFA delete. If the bucket has never been so configured, this element is not
         * returned.
         *
         * @return this builder.
         */
        public Builder mfaDelete(MFADelete mfaDelete) {
            this.mfaDelete = mfaDelete;
            return this;
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

        @Override
        public VersioningConfiguration build() {
            return new VersioningConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> mfaDelete((MFADelete) SchemaUtils.validateSameMember($SCHEMA_MFA_DELETE, member, value));
                case 1 -> status((BucketVersioningStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
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
                    case 0 -> builder.mfaDelete(MFADelete.builder().deserializeMember(de, member).build());
                    case 1 -> builder.status(BucketVersioningStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
