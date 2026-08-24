package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * An evaluation window that advances each time the alarm is evaluated, forming a rolling time window. This is the
 * default evaluation window. A sliding window has no additional configuration options.
 *
 * <p>Choose a sliding window when you need the fastest detection and the calendar boundaries of the data don't matter,
 * such as for continuous performance, latency, or resource-exhaustion monitoring.
 */
@SmithyGenerated
public final class SlidingWindow implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SLIDING_WINDOW;

    public static final ShapeId $ID = $SCHEMA.id();

    private SlidingWindow(Builder builder) {
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
        return other != null && getClass() == other.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SlidingWindow}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SlidingWindow}.
     */
    public static final class Builder implements ShapeBuilder<SlidingWindow> {

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        @Override
        public SlidingWindow build() {
            return new SlidingWindow(this);
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
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {}
        }
    }
}
