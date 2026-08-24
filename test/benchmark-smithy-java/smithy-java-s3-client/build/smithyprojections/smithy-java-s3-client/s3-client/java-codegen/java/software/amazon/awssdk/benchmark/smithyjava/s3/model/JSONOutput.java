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
 * Specifies JSON as request's output serialization format.
 */
@SmithyGenerated
public final class JSONOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.JSON_OUTPUT;
    private static final Schema $SCHEMA_RECORD_DELIMITER = $SCHEMA.member("RecordDelimiter");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String recordDelimiter;

    private JSONOutput(Builder builder) {
        this.recordDelimiter = builder.recordDelimiter;
    }

    /**
     * The value used to separate individual records in the output. If no value is specified, Amazon S3 uses a newline
     * character ('\n').
     */
    public String getRecordDelimiter() {
        return recordDelimiter;
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
        JSONOutput that = (JSONOutput) other;
        return Objects.equals(this.recordDelimiter, that.recordDelimiter);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(recordDelimiter);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (recordDelimiter != null) {
            serializer.writeString($SCHEMA_RECORD_DELIMITER, recordDelimiter);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECORD_DELIMITER, member, recordDelimiter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link JSONOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.recordDelimiter(this.recordDelimiter);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link JSONOutput}.
     */
    public static final class Builder implements ShapeBuilder<JSONOutput> {
        private String recordDelimiter;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The value used to separate individual records in the output. If no value is specified, Amazon S3 uses a newline
         * character ('\n').
         *
         * @return this builder.
         */
        public Builder recordDelimiter(String recordDelimiter) {
            this.recordDelimiter = recordDelimiter;
            return this;
        }

        @Override
        public JSONOutput build() {
            return new JSONOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> recordDelimiter((String) SchemaUtils.validateSameMember($SCHEMA_RECORD_DELIMITER, member, value));
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
                    case 0 -> builder.recordDelimiter(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
