package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.io.Closeable;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public final class SelectObjectContentOutput implements SerializableStruct, Closeable {

    public static final Schema $SCHEMA = Schemas6.SELECT_OBJECT_CONTENT_OUTPUT;
    private static final Schema $SCHEMA_PAYLOAD = $SCHEMA.member("Payload");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient EventStream<SelectObjectContentEventStream> payload;

    private SelectObjectContentOutput(Builder builder) {
        this.payload = builder.payload;
    }

    /**
     * The array of results.
     */
    public EventStream<SelectObjectContentEventStream> getPayload() {
        return payload;
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
        SelectObjectContentOutput that = (SelectObjectContentOutput) other;
        return Objects.equals(this.payload, that.payload);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(payload);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (payload != null) {
            serializer.writeEventStream($SCHEMA_PAYLOAD, payload);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PAYLOAD, member, payload);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Closes the underlying stream.
     */
    @Override
    public void close() {
        if (payload != null) {
            payload.close();
        }
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SelectObjectContentOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.payload(this.payload);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SelectObjectContentOutput}.
     */
    public static final class Builder implements ShapeBuilder<SelectObjectContentOutput> {
        private EventStream<SelectObjectContentEventStream> payload;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The array of results.
         *
         * @return this builder.
         */
        public Builder payload(EventStream<SelectObjectContentEventStream> payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public SelectObjectContentOutput build() {
            return new SelectObjectContentOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> payload((EventStream<SelectObjectContentEventStream>) SchemaUtils.validateSameMember($SCHEMA_PAYLOAD, member, value));
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
                    case 0 -> builder.payload((EventStream<SelectObjectContentEventStream>) de.readEventStream(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
