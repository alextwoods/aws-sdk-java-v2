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
public final class UpdateTimeToLiveOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_TIME_TO_LIVE_OUTPUT;
    private static final Schema $SCHEMA_TIME_TO_LIVE_SPECIFICATION = $SCHEMA.member("TimeToLiveSpecification");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TimeToLiveSpecification timeToLiveSpecification;

    private UpdateTimeToLiveOutput(Builder builder) {
        this.timeToLiveSpecification = builder.timeToLiveSpecification;
    }

    /**
     * Represents the output of an <code>UpdateTimeToLive</code> operation.
     */
    public TimeToLiveSpecification getTimeToLiveSpecification() {
        return timeToLiveSpecification;
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
        UpdateTimeToLiveOutput that = (UpdateTimeToLiveOutput) other;
        return Objects.equals(this.timeToLiveSpecification, that.timeToLiveSpecification);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timeToLiveSpecification);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (timeToLiveSpecification != null) {
            serializer.writeStruct($SCHEMA_TIME_TO_LIVE_SPECIFICATION, timeToLiveSpecification);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION, member, timeToLiveSpecification);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateTimeToLiveOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timeToLiveSpecification(this.timeToLiveSpecification);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateTimeToLiveOutput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateTimeToLiveOutput> {
        private TimeToLiveSpecification timeToLiveSpecification;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the output of an <code>UpdateTimeToLive</code> operation.
         *
         * @return this builder.
         */
        public Builder timeToLiveSpecification(TimeToLiveSpecification timeToLiveSpecification) {
            this.timeToLiveSpecification = timeToLiveSpecification;
            return this;
        }

        @Override
        public UpdateTimeToLiveOutput build() {
            return new UpdateTimeToLiveOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timeToLiveSpecification((TimeToLiveSpecification) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION, member, value));
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
                    case 0 -> builder.timeToLiveSpecification(TimeToLiveSpecification.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
