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
 * The journal table record expiration settings for a journal table in an S3 Metadata configuration.
 */
@SmithyGenerated
public final class RecordExpiration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.RECORD_EXPIRATION;
    private static final Schema $SCHEMA_EXPIRATION = $SCHEMA.member("Expiration");
    private static final Schema $SCHEMA_DAYS = $SCHEMA.member("Days");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ExpirationState expiration;
    private final transient Integer days;

    private RecordExpiration(Builder builder) {
        this.expiration = builder.expiration;
        this.days = builder.days;
    }

    /**
     * Specifies whether journal table record expiration is enabled or disabled.
     */
    public ExpirationState getExpiration() {
        return expiration;
    }

    /**
     * If you enable journal table record expiration, you can set the number of days to retain your journal table
     * records. Journal table records must be retained for a minimum of 7 days. To set this value, specify any whole
     * number from <code>7</code> to <code>2147483647</code>. For example, to retain your journal table records for one
     * year, set this value to <code>365</code>.
     */
    public Integer getDays() {
        return days;
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
        RecordExpiration that = (RecordExpiration) other;
        return Objects.equals(this.days, that.days)
               && Objects.equals(this.expiration, that.expiration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(expiration);
        $hc = 31 * $hc + Objects.hashCode(days);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_EXPIRATION, expiration.getValue());
        if (days != null) {
            serializer.writeInteger($SCHEMA_DAYS, days);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, expiration);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, days);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RecordExpiration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.expiration(this.expiration);
        builder.days(this.days);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RecordExpiration}.
     */
    public static final class Builder implements ShapeBuilder<RecordExpiration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ExpirationState expiration;
        private Integer days;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether journal table record expiration is enabled or disabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expiration(ExpirationState expiration) {
            this.expiration = Objects.requireNonNull(expiration, "expiration cannot be null");
            tracker.setMember($SCHEMA_EXPIRATION);
            return this;
        }

        /**
         * If you enable journal table record expiration, you can set the number of days to retain your journal table
         * records. Journal table records must be retained for a minimum of 7 days. To set this value, specify any whole
         * number from <code>7</code> to <code>2147483647</code>. For example, to retain your journal table records for one
         * year, set this value to <code>365</code>.
         *
         * @return this builder.
         */
        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        @Override
        public RecordExpiration build() {
            tracker.validate();
            return new RecordExpiration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> expiration((ExpirationState) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, value));
                case 1 -> days((Integer) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RecordExpiration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_EXPIRATION)) {
                expiration(ExpirationState.unknown(""));
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
                    case 0 -> builder.expiration(ExpirationState.builder().deserializeMember(de, member).build());
                    case 1 -> builder.days(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
