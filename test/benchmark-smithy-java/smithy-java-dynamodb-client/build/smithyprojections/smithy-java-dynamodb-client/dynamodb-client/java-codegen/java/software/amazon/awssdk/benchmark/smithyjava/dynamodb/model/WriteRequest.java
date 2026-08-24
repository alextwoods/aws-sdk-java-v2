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
 * Represents an operation to perform - either <code>DeleteItem</code> or <code>PutItem</code>. You can only request one
 * of these operations, not both, in a single <code>WriteRequest</code>. If you do need to perform both of these
 * operations, you need to provide two separate <code>WriteRequest</code> objects.
 */
@SmithyGenerated
public final class WriteRequest implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.WRITE_REQUEST;
    private static final Schema $SCHEMA_PUT_REQUEST = $SCHEMA.member("PutRequest");
    private static final Schema $SCHEMA_DELETE_REQUEST = $SCHEMA.member("DeleteRequest");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient PutRequest putRequest;
    private final transient DeleteRequest deleteRequest;

    private WriteRequest(Builder builder) {
        this.putRequest = builder.putRequest;
        this.deleteRequest = builder.deleteRequest;
    }

    /**
     * A request to perform a <code>PutItem</code> operation.
     */
    public PutRequest getPutRequest() {
        return putRequest;
    }

    /**
     * A request to perform a <code>DeleteItem</code> operation.
     */
    public DeleteRequest getDeleteRequest() {
        return deleteRequest;
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
        WriteRequest that = (WriteRequest) other;
        return Objects.equals(this.putRequest, that.putRequest)
               && Objects.equals(this.deleteRequest, that.deleteRequest);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(putRequest);
        $hc = 31 * $hc + Objects.hashCode(deleteRequest);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (putRequest != null) {
            serializer.writeStruct($SCHEMA_PUT_REQUEST, putRequest);
        }
        if (deleteRequest != null) {
            serializer.writeStruct($SCHEMA_DELETE_REQUEST, deleteRequest);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PUT_REQUEST, member, putRequest);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_REQUEST, member, deleteRequest);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link WriteRequest}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.putRequest(this.putRequest);
        builder.deleteRequest(this.deleteRequest);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link WriteRequest}.
     */
    public static final class Builder implements ShapeBuilder<WriteRequest> {
        private PutRequest putRequest;
        private DeleteRequest deleteRequest;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A request to perform a <code>PutItem</code> operation.
         *
         * @return this builder.
         */
        public Builder putRequest(PutRequest putRequest) {
            this.putRequest = putRequest;
            return this;
        }

        /**
         * A request to perform a <code>DeleteItem</code> operation.
         *
         * @return this builder.
         */
        public Builder deleteRequest(DeleteRequest deleteRequest) {
            this.deleteRequest = deleteRequest;
            return this;
        }

        @Override
        public WriteRequest build() {
            return new WriteRequest(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> putRequest((PutRequest) SchemaUtils.validateSameMember($SCHEMA_PUT_REQUEST, member, value));
                case 1 -> deleteRequest((DeleteRequest) SchemaUtils.validateSameMember($SCHEMA_DELETE_REQUEST, member, value));
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
                    case 0 -> builder.putRequest(PutRequest.builder().deserializeMember(de, member).build());
                    case 1 -> builder.deleteRequest(DeleteRequest.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
