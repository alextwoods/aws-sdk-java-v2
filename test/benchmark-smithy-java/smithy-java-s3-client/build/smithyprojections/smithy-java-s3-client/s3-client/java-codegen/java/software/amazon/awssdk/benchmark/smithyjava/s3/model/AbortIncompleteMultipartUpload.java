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
 * Specifies the days since the initiation of an incomplete multipart upload that Amazon S3 will wait before permanently
 * removing all parts of the upload. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html#mpu-abort-incomplete-mpu-lifecycle-config"> Aborting Incomplete Multipart Uploads Using a Bucket
 * Lifecycle Configuration</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class AbortIncompleteMultipartUpload implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ABORT_INCOMPLETE_MULTIPART_UPLOAD;
    private static final Schema $SCHEMA_DAYS_AFTER_INITIATION = $SCHEMA.member("DaysAfterInitiation");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer daysAfterInitiation;

    private AbortIncompleteMultipartUpload(Builder builder) {
        this.daysAfterInitiation = builder.daysAfterInitiation;
    }

    /**
     * Specifies the number of days after which Amazon S3 aborts an incomplete multipart upload.
     */
    public Integer getDaysAfterInitiation() {
        return daysAfterInitiation;
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
        AbortIncompleteMultipartUpload that = (AbortIncompleteMultipartUpload) other;
        return Objects.equals(this.daysAfterInitiation, that.daysAfterInitiation);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(daysAfterInitiation);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (daysAfterInitiation != null) {
            serializer.writeInteger($SCHEMA_DAYS_AFTER_INITIATION, daysAfterInitiation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS_AFTER_INITIATION, member, daysAfterInitiation);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AbortIncompleteMultipartUpload}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.daysAfterInitiation(this.daysAfterInitiation);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AbortIncompleteMultipartUpload}.
     */
    public static final class Builder implements ShapeBuilder<AbortIncompleteMultipartUpload> {
        private Integer daysAfterInitiation;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the number of days after which Amazon S3 aborts an incomplete multipart upload.
         *
         * @return this builder.
         */
        public Builder daysAfterInitiation(Integer daysAfterInitiation) {
            this.daysAfterInitiation = daysAfterInitiation;
            return this;
        }

        @Override
        public AbortIncompleteMultipartUpload build() {
            return new AbortIncompleteMultipartUpload(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> daysAfterInitiation((Integer) SchemaUtils.validateSameMember($SCHEMA_DAYS_AFTER_INITIATION, member, value));
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
                    case 0 -> builder.daysAfterInitiation(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
