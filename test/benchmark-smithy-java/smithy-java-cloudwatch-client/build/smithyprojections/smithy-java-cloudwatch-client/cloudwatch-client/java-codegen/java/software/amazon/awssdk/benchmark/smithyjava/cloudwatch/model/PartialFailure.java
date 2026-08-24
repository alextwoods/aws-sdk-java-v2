package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * This array is empty if the API operation was successful for all the rules specified in the request. If the operation
 * could not process one of the rules, the following data is returned for each of those rules.
 */
@SmithyGenerated
public final class PartialFailure implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PARTIAL_FAILURE;
    private static final Schema $SCHEMA_FAILURE_RESOURCE = $SCHEMA.member("FailureResource");
    private static final Schema $SCHEMA_EXCEPTION_TYPE = $SCHEMA.member("ExceptionType");
    private static final Schema $SCHEMA_FAILURE_CODE = $SCHEMA.member("FailureCode");
    private static final Schema $SCHEMA_FAILURE_DESCRIPTION = $SCHEMA.member("FailureDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String failureResource;
    private final transient String exceptionType;
    private final transient String failureCode;
    private final transient String failureDescription;

    private PartialFailure(Builder builder) {
        this.failureResource = builder.failureResource;
        this.exceptionType = builder.exceptionType;
        this.failureCode = builder.failureCode;
        this.failureDescription = builder.failureDescription;
    }

    /**
     * The specified rule that could not be deleted.
     */
    public String getFailureResource() {
        return failureResource;
    }

    /**
     * The type of error.
     */
    public String getExceptionType() {
        return exceptionType;
    }

    /**
     * The code of the error.
     */
    public String getFailureCode() {
        return failureCode;
    }

    /**
     * A description of the error.
     */
    public String getFailureDescription() {
        return failureDescription;
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
        PartialFailure that = (PartialFailure) other;
        return Objects.equals(this.failureResource, that.failureResource)
               && Objects.equals(this.exceptionType, that.exceptionType)
               && Objects.equals(this.failureCode, that.failureCode)
               && Objects.equals(this.failureDescription, that.failureDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(failureResource);
        $hc = 31 * $hc + Objects.hashCode(exceptionType);
        $hc = 31 * $hc + Objects.hashCode(failureCode);
        $hc = 31 * $hc + Objects.hashCode(failureDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (failureResource != null) {
            serializer.writeString($SCHEMA_FAILURE_RESOURCE, failureResource);
        }
        if (exceptionType != null) {
            serializer.writeString($SCHEMA_EXCEPTION_TYPE, exceptionType);
        }
        if (failureCode != null) {
            serializer.writeString($SCHEMA_FAILURE_CODE, failureCode);
        }
        if (failureDescription != null) {
            serializer.writeString($SCHEMA_FAILURE_DESCRIPTION, failureDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_RESOURCE, member, failureResource);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCEPTION_TYPE, member, exceptionType);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_CODE, member, failureCode);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_DESCRIPTION, member, failureDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PartialFailure}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.failureResource(this.failureResource);
        builder.exceptionType(this.exceptionType);
        builder.failureCode(this.failureCode);
        builder.failureDescription(this.failureDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PartialFailure}.
     */
    public static final class Builder implements ShapeBuilder<PartialFailure> {
        private String failureResource;
        private String exceptionType;
        private String failureCode;
        private String failureDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The specified rule that could not be deleted.
         *
         * @return this builder.
         */
        public Builder failureResource(String failureResource) {
            this.failureResource = failureResource;
            return this;
        }

        /**
         * The type of error.
         *
         * @return this builder.
         */
        public Builder exceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
            return this;
        }

        /**
         * The code of the error.
         *
         * @return this builder.
         */
        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        /**
         * A description of the error.
         *
         * @return this builder.
         */
        public Builder failureDescription(String failureDescription) {
            this.failureDescription = failureDescription;
            return this;
        }

        @Override
        public PartialFailure build() {
            return new PartialFailure(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> failureResource((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_RESOURCE, member, value));
                case 1 -> exceptionType((String) SchemaUtils.validateSameMember($SCHEMA_EXCEPTION_TYPE, member, value));
                case 2 -> failureCode((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_CODE, member, value));
                case 3 -> failureDescription((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_DESCRIPTION, member, value));
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
                    case 0 -> builder.failureResource(de.readString(member));
                    case 1 -> builder.exceptionType(de.readString(member));
                    case 2 -> builder.failureCode(de.readString(member));
                    case 3 -> builder.failureDescription(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
