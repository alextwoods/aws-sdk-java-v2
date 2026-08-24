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
 * This data type contains information about progress of an operation.
 */
@SmithyGenerated
public final class Progress implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.PROGRESS;
    private static final Schema $SCHEMA_BYTES_SCANNED = $SCHEMA.member("BytesScanned");
    private static final Schema $SCHEMA_BYTES_PROCESSED = $SCHEMA.member("BytesProcessed");
    private static final Schema $SCHEMA_BYTES_RETURNED = $SCHEMA.member("BytesReturned");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long bytesScanned;
    private final transient Long bytesProcessed;
    private final transient Long bytesReturned;

    private Progress(Builder builder) {
        this.bytesScanned = builder.bytesScanned;
        this.bytesProcessed = builder.bytesProcessed;
        this.bytesReturned = builder.bytesReturned;
    }

    /**
     * The current number of object bytes scanned.
     */
    public Long getBytesScanned() {
        return bytesScanned;
    }

    /**
     * The current number of uncompressed object bytes processed.
     */
    public Long getBytesProcessed() {
        return bytesProcessed;
    }

    /**
     * The current number of bytes of records payload data returned.
     */
    public Long getBytesReturned() {
        return bytesReturned;
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
        Progress that = (Progress) other;
        return Objects.equals(this.bytesScanned, that.bytesScanned)
               && Objects.equals(this.bytesProcessed, that.bytesProcessed)
               && Objects.equals(this.bytesReturned, that.bytesReturned);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bytesScanned);
        $hc = 31 * $hc + Objects.hashCode(bytesProcessed);
        $hc = 31 * $hc + Objects.hashCode(bytesReturned);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (bytesScanned != null) {
            serializer.writeLong($SCHEMA_BYTES_SCANNED, bytesScanned);
        }
        if (bytesProcessed != null) {
            serializer.writeLong($SCHEMA_BYTES_PROCESSED, bytesProcessed);
        }
        if (bytesReturned != null) {
            serializer.writeLong($SCHEMA_BYTES_RETURNED, bytesReturned);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BYTES_SCANNED, member, bytesScanned);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BYTES_PROCESSED, member, bytesProcessed);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BYTES_RETURNED, member, bytesReturned);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Progress}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bytesScanned(this.bytesScanned);
        builder.bytesProcessed(this.bytesProcessed);
        builder.bytesReturned(this.bytesReturned);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Progress}.
     */
    public static final class Builder implements ShapeBuilder<Progress> {
        private Long bytesScanned;
        private Long bytesProcessed;
        private Long bytesReturned;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The current number of object bytes scanned.
         *
         * @return this builder.
         */
        public Builder bytesScanned(Long bytesScanned) {
            this.bytesScanned = bytesScanned;
            return this;
        }

        /**
         * The current number of uncompressed object bytes processed.
         *
         * @return this builder.
         */
        public Builder bytesProcessed(Long bytesProcessed) {
            this.bytesProcessed = bytesProcessed;
            return this;
        }

        /**
         * The current number of bytes of records payload data returned.
         *
         * @return this builder.
         */
        public Builder bytesReturned(Long bytesReturned) {
            this.bytesReturned = bytesReturned;
            return this;
        }

        @Override
        public Progress build() {
            return new Progress(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bytesScanned((Long) SchemaUtils.validateSameMember($SCHEMA_BYTES_SCANNED, member, value));
                case 1 -> bytesProcessed((Long) SchemaUtils.validateSameMember($SCHEMA_BYTES_PROCESSED, member, value));
                case 2 -> bytesReturned((Long) SchemaUtils.validateSameMember($SCHEMA_BYTES_RETURNED, member, value));
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
                    case 0 -> builder.bytesScanned(de.readLong(member));
                    case 1 -> builder.bytesProcessed(de.readLong(member));
                    case 2 -> builder.bytesReturned(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
