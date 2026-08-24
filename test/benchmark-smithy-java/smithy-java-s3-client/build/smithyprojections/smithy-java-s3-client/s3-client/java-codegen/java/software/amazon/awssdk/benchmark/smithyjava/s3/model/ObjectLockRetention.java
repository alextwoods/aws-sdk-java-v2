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
 * A Retention configuration for an object.
 */
@SmithyGenerated
public final class ObjectLockRetention implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.OBJECT_LOCK_RETENTION;
    private static final Schema $SCHEMA_MODE = $SCHEMA.member("Mode");
    private static final Schema $SCHEMA_RETAIN_UNTIL_DATE = $SCHEMA.member("RetainUntilDate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectLockRetentionMode mode;
    private final transient Instant retainUntilDate;

    private ObjectLockRetention(Builder builder) {
        this.mode = builder.mode;
        this.retainUntilDate = builder.retainUntilDate;
    }

    /**
     * Indicates the Retention mode for the specified object.
     */
    public ObjectLockRetentionMode getMode() {
        return mode;
    }

    /**
     * The date on which this Object Lock Retention will expire.
     */
    public Instant getRetainUntilDate() {
        return retainUntilDate;
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
        ObjectLockRetention that = (ObjectLockRetention) other;
        return Objects.equals(this.mode, that.mode)
               && Objects.equals(this.retainUntilDate, that.retainUntilDate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(mode);
        $hc = 31 * $hc + Objects.hashCode(retainUntilDate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (mode != null) {
            serializer.writeString($SCHEMA_MODE, mode.getValue());
        }
        if (retainUntilDate != null) {
            serializer.writeTimestamp($SCHEMA_RETAIN_UNTIL_DATE, retainUntilDate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MODE, member, mode);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETAIN_UNTIL_DATE, member, retainUntilDate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ObjectLockRetention}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.mode(this.mode);
        builder.retainUntilDate(this.retainUntilDate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ObjectLockRetention}.
     */
    public static final class Builder implements ShapeBuilder<ObjectLockRetention> {
        private ObjectLockRetentionMode mode;
        private Instant retainUntilDate;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates the Retention mode for the specified object.
         *
         * @return this builder.
         */
        public Builder mode(ObjectLockRetentionMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * The date on which this Object Lock Retention will expire.
         *
         * @return this builder.
         */
        public Builder retainUntilDate(Instant retainUntilDate) {
            this.retainUntilDate = retainUntilDate;
            return this;
        }

        @Override
        public ObjectLockRetention build() {
            return new ObjectLockRetention(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> mode((ObjectLockRetentionMode) SchemaUtils.validateSameMember($SCHEMA_MODE, member, value));
                case 1 -> retainUntilDate((Instant) SchemaUtils.validateSameMember($SCHEMA_RETAIN_UNTIL_DATE, member, value));
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
                    case 0 -> builder.mode(ObjectLockRetentionMode.builder().deserializeMember(de, member).build());
                    case 1 -> builder.retainUntilDate(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
