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
 * Describes the location where the restore job's output is stored.
 */
@SmithyGenerated
public final class OutputLocation implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.OUTPUT_LOCATION;
    private static final Schema $SCHEMA_S3 = $SCHEMA.member("S3");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient S3Location s3;

    private OutputLocation(Builder builder) {
        this.s3 = builder.s3;
    }

    /**
     * Describes an S3 location that will receive the results of the restore request.
     */
    public S3Location getS3() {
        return s3;
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
        OutputLocation that = (OutputLocation) other;
        return Objects.equals(this.s3, that.s3);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(s3);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (s3 != null) {
            serializer.writeStruct($SCHEMA_S3, s3);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3, member, s3);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link OutputLocation}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.s3(this.s3);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OutputLocation}.
     */
    public static final class Builder implements ShapeBuilder<OutputLocation> {
        private S3Location s3;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Describes an S3 location that will receive the results of the restore request.
         *
         * @return this builder.
         */
        public Builder s3(S3Location s3) {
            this.s3 = s3;
            return this;
        }

        @Override
        public OutputLocation build() {
            return new OutputLocation(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s3((S3Location) SchemaUtils.validateSameMember($SCHEMA_S3, member, value));
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
                    case 0 -> builder.s3(S3Location.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
