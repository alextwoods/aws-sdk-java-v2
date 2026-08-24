package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.nio.ByteBuffer;
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
 * The container for the records event.
 */
@SmithyGenerated
public final class RecordsEvent implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.RECORDS_EVENT;
    private static final Schema $SCHEMA_PAYLOAD = $SCHEMA.member("Payload");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ByteBuffer payload;

    private RecordsEvent(Builder builder) {
        this.payload = builder.payload == null ? null : builder.payload.duplicate();
    }

    /**
     * The byte array of partial, one or more result records. S3 Select doesn't guarantee that a record will be
     * self-contained in one record frame. To ensure continuous streaming of data, S3 Select might split the same record
     * across multiple record frames instead of aggregating the results in memory. Some S3 clients (for example, the SDK
     * for Java) handle this behavior by creating a <code>ByteStream</code> out of the response by default. Other
     * clients might not handle this behavior by default. In those cases, you must aggregate the results on the client
     * side and parse the response.
     */
    public ByteBuffer getPayload() {
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
        RecordsEvent that = (RecordsEvent) other;
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
            serializer.writeBlob($SCHEMA_PAYLOAD, payload);
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
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RecordsEvent}.
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
     * Builder for {@link RecordsEvent}.
     */
    public static final class Builder implements ShapeBuilder<RecordsEvent> {
        private ByteBuffer payload;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The byte array of partial, one or more result records. S3 Select doesn't guarantee that a record will be
         * self-contained in one record frame. To ensure continuous streaming of data, S3 Select might split the same record
         * across multiple record frames instead of aggregating the results in memory. Some S3 clients (for example, the SDK
         * for Java) handle this behavior by creating a <code>ByteStream</code> out of the response by default. Other
         * clients might not handle this behavior by default. In those cases, you must aggregate the results on the client
         * side and parse the response.
         *
         * @return this builder.
         */
        public Builder payload(ByteBuffer payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public RecordsEvent build() {
            return new RecordsEvent(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> payload((ByteBuffer) SchemaUtils.validateSameMember($SCHEMA_PAYLOAD, member, value));
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
                    case 0 -> builder.payload(de.readBlob(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
