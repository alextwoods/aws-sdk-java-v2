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
 * Container for the expiration for the lifecycle of the object.
 *
 * <p>For more information see, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html">Managing your storage lifecycle</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class LifecycleExpiration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.LIFECYCLE_EXPIRATION;
    private static final Schema $SCHEMA_DATE = $SCHEMA.member("Date");
    private static final Schema $SCHEMA_DAYS = $SCHEMA.member("Days");
    private static final Schema $SCHEMA_EXPIRED_OBJECT_DELETE_MARKER = $SCHEMA.member("ExpiredObjectDeleteMarker");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant date;
    private final transient Integer days;
    private final transient Boolean expiredObjectDeleteMarker;

    private LifecycleExpiration(Builder builder) {
        this.date = builder.date;
        this.days = builder.days;
        this.expiredObjectDeleteMarker = builder.expiredObjectDeleteMarker;
    }

    /**
     * Indicates at what date the object is to be moved or deleted. The date value must conform to the ISO 8601 format.
     * The time is always midnight UTC.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     */
    public Instant getDate() {
        return date;
    }

    /**
     * Indicates the lifetime, in days, of the objects that are subject to the rule. The value must be a non-zero
     * positive integer.
     */
    public Integer getDays() {
        return days;
    }

    /**
     * Indicates whether Amazon S3 will remove a delete marker with no noncurrent versions. If set to true, the delete
     * marker will be expired; if set to false the policy takes no action. This cannot be specified with Days or Date in
     * a Lifecycle Expiration Policy.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     */
    public Boolean isExpiredObjectDeleteMarker() {
        return expiredObjectDeleteMarker;
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
        LifecycleExpiration that = (LifecycleExpiration) other;
        return Objects.equals(this.expiredObjectDeleteMarker, that.expiredObjectDeleteMarker)
               && Objects.equals(this.days, that.days)
               && Objects.equals(this.date, that.date);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(date);
        $hc = 31 * $hc + Objects.hashCode(days);
        $hc = 31 * $hc + Objects.hashCode(expiredObjectDeleteMarker);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (date != null) {
            serializer.writeTimestamp($SCHEMA_DATE, date);
        }
        if (days != null) {
            serializer.writeInteger($SCHEMA_DAYS, days);
        }
        if (expiredObjectDeleteMarker != null) {
            serializer.writeBoolean($SCHEMA_EXPIRED_OBJECT_DELETE_MARKER, expiredObjectDeleteMarker);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATE, member, date);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, days);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRED_OBJECT_DELETE_MARKER, member, expiredObjectDeleteMarker);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LifecycleExpiration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.date(this.date);
        builder.days(this.days);
        builder.expiredObjectDeleteMarker(this.expiredObjectDeleteMarker);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LifecycleExpiration}.
     */
    public static final class Builder implements ShapeBuilder<LifecycleExpiration> {
        private Instant date;
        private Integer days;
        private Boolean expiredObjectDeleteMarker;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates at what date the object is to be moved or deleted. The date value must conform to the ISO 8601 format.
         * The time is always midnight UTC.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * @return this builder.
         */
        public Builder date(Instant date) {
            this.date = date;
            return this;
        }

        /**
         * Indicates the lifetime, in days, of the objects that are subject to the rule. The value must be a non-zero
         * positive integer.
         *
         * @return this builder.
         */
        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        /**
         * Indicates whether Amazon S3 will remove a delete marker with no noncurrent versions. If set to true, the delete
         * marker will be expired; if set to false the policy takes no action. This cannot be specified with Days or Date in
         * a Lifecycle Expiration Policy.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * @return this builder.
         */
        public Builder expiredObjectDeleteMarker(Boolean expiredObjectDeleteMarker) {
            this.expiredObjectDeleteMarker = expiredObjectDeleteMarker;
            return this;
        }

        @Override
        public LifecycleExpiration build() {
            return new LifecycleExpiration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> date((Instant) SchemaUtils.validateSameMember($SCHEMA_DATE, member, value));
                case 1 -> days((Integer) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, value));
                case 2 -> expiredObjectDeleteMarker((Boolean) SchemaUtils.validateSameMember($SCHEMA_EXPIRED_OBJECT_DELETE_MARKER, member, value));
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
                    case 0 -> builder.date(de.readTimestamp(member));
                    case 1 -> builder.days(de.readInteger(member));
                    case 2 -> builder.expiredObjectDeleteMarker(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
