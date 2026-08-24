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
 * A container specifying the time value for S3 Replication Time Control (S3 RTC) and replication metrics
 * <code>EventThreshold</code>.
 */
@SmithyGenerated
public final class ReplicationTimeValue implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REPLICATION_TIME_VALUE;
    private static final Schema $SCHEMA_MINUTES = $SCHEMA.member("Minutes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer minutes;

    private ReplicationTimeValue(Builder builder) {
        this.minutes = builder.minutes;
    }

    /**
     * Contains an integer specifying time in minutes.
     *
     * <p> Valid value: 15
     */
    public Integer getMinutes() {
        return minutes;
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
        ReplicationTimeValue that = (ReplicationTimeValue) other;
        return Objects.equals(this.minutes, that.minutes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(minutes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (minutes != null) {
            serializer.writeInteger($SCHEMA_MINUTES, minutes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MINUTES, member, minutes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicationTimeValue}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.minutes(this.minutes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicationTimeValue}.
     */
    public static final class Builder implements ShapeBuilder<ReplicationTimeValue> {
        private Integer minutes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Contains an integer specifying time in minutes.
         *
         * <p> Valid value: 15
         *
         * @return this builder.
         */
        public Builder minutes(Integer minutes) {
            this.minutes = minutes;
            return this;
        }

        @Override
        public ReplicationTimeValue build() {
            return new ReplicationTimeValue(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> minutes((Integer) SchemaUtils.validateSameMember($SCHEMA_MINUTES, member, value));
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
                    case 0 -> builder.minutes(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
