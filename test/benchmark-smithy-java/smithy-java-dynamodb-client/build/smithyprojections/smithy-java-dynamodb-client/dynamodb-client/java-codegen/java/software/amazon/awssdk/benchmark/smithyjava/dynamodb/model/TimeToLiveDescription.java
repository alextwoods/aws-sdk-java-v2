package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * The description of the Time to Live (TTL) status on the specified table.
 */
@SmithyGenerated
public final class TimeToLiveDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TIME_TO_LIVE_DESCRIPTION;
    private static final Schema $SCHEMA_TIME_TO_LIVE_STATUS = $SCHEMA.member("TimeToLiveStatus");
    private static final Schema $SCHEMA_ATTRIBUTE_NAME = $SCHEMA.member("AttributeName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TimeToLiveStatus timeToLiveStatus;
    private final transient String attributeName;

    private TimeToLiveDescription(Builder builder) {
        this.timeToLiveStatus = builder.timeToLiveStatus;
        this.attributeName = builder.attributeName;
    }

    /**
     * The TTL status for the table.
     */
    public TimeToLiveStatus getTimeToLiveStatus() {
        return timeToLiveStatus;
    }

    /**
     * The name of the TTL attribute for items in the table.
     */
    public String getAttributeName() {
        return attributeName;
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
        TimeToLiveDescription that = (TimeToLiveDescription) other;
        return Objects.equals(this.attributeName, that.attributeName)
               && Objects.equals(this.timeToLiveStatus, that.timeToLiveStatus);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timeToLiveStatus);
        $hc = 31 * $hc + Objects.hashCode(attributeName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (timeToLiveStatus != null) {
            serializer.writeString($SCHEMA_TIME_TO_LIVE_STATUS, timeToLiveStatus.getValue());
        }
        if (attributeName != null) {
            serializer.writeString($SCHEMA_ATTRIBUTE_NAME, attributeName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_STATUS, member, timeToLiveStatus);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, attributeName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TimeToLiveDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timeToLiveStatus(this.timeToLiveStatus);
        builder.attributeName(this.attributeName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TimeToLiveDescription}.
     */
    public static final class Builder implements ShapeBuilder<TimeToLiveDescription> {
        private TimeToLiveStatus timeToLiveStatus;
        private String attributeName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The TTL status for the table.
         *
         * @return this builder.
         */
        public Builder timeToLiveStatus(TimeToLiveStatus timeToLiveStatus) {
            this.timeToLiveStatus = timeToLiveStatus;
            return this;
        }

        /**
         * The name of the TTL attribute for items in the table.
         *
         * @return this builder.
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        @Override
        public TimeToLiveDescription build() {
            return new TimeToLiveDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timeToLiveStatus((TimeToLiveStatus) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_STATUS, member, value));
                case 1 -> attributeName((String) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, value));
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
                    case 0 -> builder.timeToLiveStatus(TimeToLiveStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.attributeName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
