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

@SmithyGenerated
public final class DescribeTimeToLiveOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_TIME_TO_LIVE_OUTPUT;
    private static final Schema $SCHEMA_TIME_TO_LIVE_DESCRIPTION = $SCHEMA.member("TimeToLiveDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TimeToLiveDescription timeToLiveDescription;

    private DescribeTimeToLiveOutput(Builder builder) {
        this.timeToLiveDescription = builder.timeToLiveDescription;
    }

    /**
     *
     */
    public TimeToLiveDescription getTimeToLiveDescription() {
        return timeToLiveDescription;
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
        DescribeTimeToLiveOutput that = (DescribeTimeToLiveOutput) other;
        return Objects.equals(this.timeToLiveDescription, that.timeToLiveDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timeToLiveDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (timeToLiveDescription != null) {
            serializer.writeStruct($SCHEMA_TIME_TO_LIVE_DESCRIPTION, timeToLiveDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_DESCRIPTION, member, timeToLiveDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeTimeToLiveOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timeToLiveDescription(this.timeToLiveDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeTimeToLiveOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeTimeToLiveOutput> {
        private TimeToLiveDescription timeToLiveDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         *
         *
         * @return this builder.
         */
        public Builder timeToLiveDescription(TimeToLiveDescription timeToLiveDescription) {
            this.timeToLiveDescription = timeToLiveDescription;
            return this;
        }

        @Override
        public DescribeTimeToLiveOutput build() {
            return new DescribeTimeToLiveOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timeToLiveDescription((TimeToLiveDescription) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_DESCRIPTION, member, value));
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
                    case 0 -> builder.timeToLiveDescription(TimeToLiveDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
