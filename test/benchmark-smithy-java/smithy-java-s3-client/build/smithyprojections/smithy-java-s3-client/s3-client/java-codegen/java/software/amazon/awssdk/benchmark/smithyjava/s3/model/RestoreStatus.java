package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
 * Specifies the restoration status of an object. Objects in certain storage classes must be restored before they can be
 * retrieved. For more information about these storage classes and how to work with archived objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/archived-objects.html"> Working
 * with archived objects</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>This functionality is not supported for directory buckets. Directory buckets only support <code>EXPRESS_ONEZONE</code>
 * (the S3 Express One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent
 * Access storage class) in Dedicated Local Zones.
 */
@SmithyGenerated
public final class RestoreStatus implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.RESTORE_STATUS;
    private static final Schema $SCHEMA_IS_RESTORE_IN_PROGRESS = $SCHEMA.member("IsRestoreInProgress");
    private static final Schema $SCHEMA_RESTORE_EXPIRY_DATE = $SCHEMA.member("RestoreExpiryDate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean isRestoreInProgress;
    private final transient Instant restoreExpiryDate;

    private RestoreStatus(Builder builder) {
        this.isRestoreInProgress = builder.isRestoreInProgress;
        this.restoreExpiryDate = builder.restoreExpiryDate;
    }

    /**
     * Specifies whether the object is currently being restored. If the object restoration is in progress, the header
     * returns the value <code>TRUE</code>. For example:
     *
     * <p><code>x-amz-optional-object-attributes: IsRestoreInProgress="true"</code>
     *
     * <p>If the object restoration has completed, the header returns the value <code>FALSE</code>. For example:
     *
     * <p><code>x-amz-optional-object-attributes: IsRestoreInProgress="false",
     * RestoreExpiryDate="2012-12-21T00:00:00.000Z"</code>
     *
     * <p>If the object hasn't been restored, there is no header response.
     */
    public Boolean isIsRestoreInProgress() {
        return isRestoreInProgress;
    }

    /**
     * Indicates when the restored copy will expire. This value is populated only if the object has already been
     * restored. For example:
     *
     * <p><code>x-amz-optional-object-attributes: IsRestoreInProgress="false",
     * RestoreExpiryDate="2012-12-21T00:00:00.000Z"</code>
     */
    public Instant getRestoreExpiryDate() {
        return restoreExpiryDate;
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
        RestoreStatus that = (RestoreStatus) other;
        return Objects.equals(this.isRestoreInProgress, that.isRestoreInProgress)
               && Objects.equals(this.restoreExpiryDate, that.restoreExpiryDate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(isRestoreInProgress);
        $hc = 31 * $hc + Objects.hashCode(restoreExpiryDate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (isRestoreInProgress != null) {
            serializer.writeBoolean($SCHEMA_IS_RESTORE_IN_PROGRESS, isRestoreInProgress);
        }
        if (restoreExpiryDate != null) {
            serializer.writeTimestamp($SCHEMA_RESTORE_EXPIRY_DATE, restoreExpiryDate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_RESTORE_IN_PROGRESS, member, isRestoreInProgress);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_EXPIRY_DATE, member, restoreExpiryDate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RestoreStatus}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.isRestoreInProgress(this.isRestoreInProgress);
        builder.restoreExpiryDate(this.restoreExpiryDate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RestoreStatus}.
     */
    public static final class Builder implements ShapeBuilder<RestoreStatus> {
        private Boolean isRestoreInProgress;
        private Instant restoreExpiryDate;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether the object is currently being restored. If the object restoration is in progress, the header
         * returns the value <code>TRUE</code>. For example:
         *
         * <p><code>x-amz-optional-object-attributes: IsRestoreInProgress="true"</code>
         *
         * <p>If the object restoration has completed, the header returns the value <code>FALSE</code>. For example:
         *
         * <p><code>x-amz-optional-object-attributes: IsRestoreInProgress="false",
         * RestoreExpiryDate="2012-12-21T00:00:00.000Z"</code>
         *
         * <p>If the object hasn't been restored, there is no header response.
         *
         * @return this builder.
         */
        public Builder isRestoreInProgress(Boolean isRestoreInProgress) {
            this.isRestoreInProgress = isRestoreInProgress;
            return this;
        }

        /**
         * Indicates when the restored copy will expire. This value is populated only if the object has already been
         * restored. For example:
         *
         * <p><code>x-amz-optional-object-attributes: IsRestoreInProgress="false",
         * RestoreExpiryDate="2012-12-21T00:00:00.000Z"</code>
         *
         * @return this builder.
         */
        public Builder restoreExpiryDate(Instant restoreExpiryDate) {
            this.restoreExpiryDate = restoreExpiryDate;
            return this;
        }

        @Override
        public RestoreStatus build() {
            return new RestoreStatus(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> isRestoreInProgress((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_RESTORE_IN_PROGRESS, member, value));
                case 1 -> restoreExpiryDate((Instant) SchemaUtils.validateSameMember($SCHEMA_RESTORE_EXPIRY_DATE, member, value));
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
                    case 0 -> builder.isRestoreInProgress(de.readBoolean(member));
                    case 1 -> builder.restoreExpiryDate(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
