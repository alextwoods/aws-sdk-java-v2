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
 * The consumed capacity for vector index operations, including vector search request bytes and vector write request
 * bytes.
 */
@SmithyGenerated
public final class VectorCapacity implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.VECTOR_CAPACITY;
    private static final Schema $SCHEMA_VECTOR_SEARCH_REQUEST_BYTES = $SCHEMA.member("VectorSearchRequestBytes");
    private static final Schema $SCHEMA_VECTOR_WRITE_REQUEST_BYTES = $SCHEMA.member("VectorWriteRequestBytes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Double vectorSearchRequestBytes;
    private final transient Double vectorWriteRequestBytes;

    private VectorCapacity(Builder builder) {
        this.vectorSearchRequestBytes = builder.vectorSearchRequestBytes;
        this.vectorWriteRequestBytes = builder.vectorWriteRequestBytes;
    }

    /**
     * The number of vector search request bytes consumed by a <code>SearchVectors</code> operation.
     */
    public Double getVectorSearchRequestBytes() {
        return vectorSearchRequestBytes;
    }

    /**
     * The number of vector write request bytes consumed when writing to a vector index. Reported for write operations
     * that modify attributes indexed by a vector index.
     */
    public Double getVectorWriteRequestBytes() {
        return vectorWriteRequestBytes;
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
        VectorCapacity that = (VectorCapacity) other;
        return Objects.equals(this.vectorSearchRequestBytes, that.vectorSearchRequestBytes)
               && Objects.equals(this.vectorWriteRequestBytes, that.vectorWriteRequestBytes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(vectorSearchRequestBytes);
        $hc = 31 * $hc + Objects.hashCode(vectorWriteRequestBytes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (vectorSearchRequestBytes != null) {
            serializer.writeDouble($SCHEMA_VECTOR_SEARCH_REQUEST_BYTES, vectorSearchRequestBytes);
        }
        if (vectorWriteRequestBytes != null) {
            serializer.writeDouble($SCHEMA_VECTOR_WRITE_REQUEST_BYTES, vectorWriteRequestBytes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_SEARCH_REQUEST_BYTES, member, vectorSearchRequestBytes);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_WRITE_REQUEST_BYTES, member, vectorWriteRequestBytes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link VectorCapacity}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.vectorSearchRequestBytes(this.vectorSearchRequestBytes);
        builder.vectorWriteRequestBytes(this.vectorWriteRequestBytes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link VectorCapacity}.
     */
    public static final class Builder implements ShapeBuilder<VectorCapacity> {
        private Double vectorSearchRequestBytes;
        private Double vectorWriteRequestBytes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The number of vector search request bytes consumed by a <code>SearchVectors</code> operation.
         *
         * @return this builder.
         */
        public Builder vectorSearchRequestBytes(Double vectorSearchRequestBytes) {
            this.vectorSearchRequestBytes = vectorSearchRequestBytes;
            return this;
        }

        /**
         * The number of vector write request bytes consumed when writing to a vector index. Reported for write operations
         * that modify attributes indexed by a vector index.
         *
         * @return this builder.
         */
        public Builder vectorWriteRequestBytes(Double vectorWriteRequestBytes) {
            this.vectorWriteRequestBytes = vectorWriteRequestBytes;
            return this;
        }

        @Override
        public VectorCapacity build() {
            return new VectorCapacity(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> vectorSearchRequestBytes((Double) SchemaUtils.validateSameMember($SCHEMA_VECTOR_SEARCH_REQUEST_BYTES, member, value));
                case 1 -> vectorWriteRequestBytes((Double) SchemaUtils.validateSameMember($SCHEMA_VECTOR_WRITE_REQUEST_BYTES, member, value));
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
                    case 0 -> builder.vectorSearchRequestBytes(de.readDouble(member));
                    case 1 -> builder.vectorWriteRequestBytes(de.readDouble(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
