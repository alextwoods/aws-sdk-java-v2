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
 * Specifies when an object transitions to a specified storage class. For more information about Amazon S3 lifecycle
 * configuration rules, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/lifecycle-transition-general-considerations.html">Transitioning Objects Using Amazon S3 Lifecycle</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class Transition implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.TRANSITION;
    private static final Schema $SCHEMA_DATE = $SCHEMA.member("Date");
    private static final Schema $SCHEMA_DAYS = $SCHEMA.member("Days");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant date;
    private final transient Integer days;
    private final transient TransitionStorageClass storageClass;

    private Transition(Builder builder) {
        this.date = builder.date;
        this.days = builder.days;
        this.storageClass = builder.storageClass;
    }

    /**
     * Indicates when objects are transitioned to the specified storage class. The date value must be in ISO 8601
     * format. The time is always midnight UTC.
     */
    public Instant getDate() {
        return date;
    }

    /**
     * Indicates the number of days after creation when objects are transitioned to the specified storage class. The
     * value can be <code>0</code> or any positive integer. Be aware that some storage classes have a minimum storage
     * duration and that you're charged for transitioning objects before their minimum storage duration. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-transition-general-considerations.html#lifecycle-configuration-constraints"> Constraints and considerations for transitions</a> in the <i>Amazon S3 User Guide</i>.
     */
    public Integer getDays() {
        return days;
    }

    /**
     * The storage class to which you want the object to transition.
     */
    public TransitionStorageClass getStorageClass() {
        return storageClass;
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
        Transition that = (Transition) other;
        return Objects.equals(this.days, that.days)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.date, that.date);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(date);
        $hc = 31 * $hc + Objects.hashCode(days);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
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
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATE, member, date);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, days);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Transition}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.date(this.date);
        builder.days(this.days);
        builder.storageClass(this.storageClass);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Transition}.
     */
    public static final class Builder implements ShapeBuilder<Transition> {
        private Instant date;
        private Integer days;
        private TransitionStorageClass storageClass;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates when objects are transitioned to the specified storage class. The date value must be in ISO 8601
         * format. The time is always midnight UTC.
         *
         * @return this builder.
         */
        public Builder date(Instant date) {
            this.date = date;
            return this;
        }

        /**
         * Indicates the number of days after creation when objects are transitioned to the specified storage class. The
         * value can be <code>0</code> or any positive integer. Be aware that some storage classes have a minimum storage
         * duration and that you're charged for transitioning objects before their minimum storage duration. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-transition-general-considerations.html#lifecycle-configuration-constraints"> Constraints and considerations for transitions</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        /**
         * The storage class to which you want the object to transition.
         *
         * @return this builder.
         */
        public Builder storageClass(TransitionStorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        @Override
        public Transition build() {
            return new Transition(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> date((Instant) SchemaUtils.validateSameMember($SCHEMA_DATE, member, value));
                case 1 -> days((Integer) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, value));
                case 2 -> storageClass((TransitionStorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
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
                    case 2 -> builder.storageClass(TransitionStorageClass.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
