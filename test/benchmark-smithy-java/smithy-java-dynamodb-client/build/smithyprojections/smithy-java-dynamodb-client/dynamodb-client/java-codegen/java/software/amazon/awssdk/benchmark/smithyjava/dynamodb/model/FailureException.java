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
 * Represents a failure a contributor insights operation.
 */
@SmithyGenerated
public final class FailureException implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.FAILURE_EXCEPTION;
    private static final Schema $SCHEMA_EXCEPTION_NAME = $SCHEMA.member("ExceptionName");
    private static final Schema $SCHEMA_EXCEPTION_DESCRIPTION = $SCHEMA.member("ExceptionDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String exceptionName;
    private final transient String exceptionDescription;

    private FailureException(Builder builder) {
        this.exceptionName = builder.exceptionName;
        this.exceptionDescription = builder.exceptionDescription;
    }

    /**
     * Exception name.
     */
    public String getExceptionName() {
        return exceptionName;
    }

    /**
     * Description of the failure.
     */
    public String getExceptionDescription() {
        return exceptionDescription;
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
        FailureException that = (FailureException) other;
        return Objects.equals(this.exceptionName, that.exceptionName)
               && Objects.equals(this.exceptionDescription, that.exceptionDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exceptionName);
        $hc = 31 * $hc + Objects.hashCode(exceptionDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exceptionName != null) {
            serializer.writeString($SCHEMA_EXCEPTION_NAME, exceptionName);
        }
        if (exceptionDescription != null) {
            serializer.writeString($SCHEMA_EXCEPTION_DESCRIPTION, exceptionDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCEPTION_NAME, member, exceptionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCEPTION_DESCRIPTION, member, exceptionDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link FailureException}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exceptionName(this.exceptionName);
        builder.exceptionDescription(this.exceptionDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FailureException}.
     */
    public static final class Builder implements ShapeBuilder<FailureException> {
        private String exceptionName;
        private String exceptionDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Exception name.
         *
         * @return this builder.
         */
        public Builder exceptionName(String exceptionName) {
            this.exceptionName = exceptionName;
            return this;
        }

        /**
         * Description of the failure.
         *
         * @return this builder.
         */
        public Builder exceptionDescription(String exceptionDescription) {
            this.exceptionDescription = exceptionDescription;
            return this;
        }

        @Override
        public FailureException build() {
            return new FailureException(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exceptionName((String) SchemaUtils.validateSameMember($SCHEMA_EXCEPTION_NAME, member, value));
                case 1 -> exceptionDescription((String) SchemaUtils.validateSameMember($SCHEMA_EXCEPTION_DESCRIPTION, member, value));
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
                    case 0 -> builder.exceptionName(de.readString(member));
                    case 1 -> builder.exceptionDescription(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
