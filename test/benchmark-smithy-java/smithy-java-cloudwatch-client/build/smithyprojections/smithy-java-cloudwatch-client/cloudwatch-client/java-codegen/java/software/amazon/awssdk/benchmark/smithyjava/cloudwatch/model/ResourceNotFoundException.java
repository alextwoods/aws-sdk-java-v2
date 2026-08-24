package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import software.amazon.smithy.java.core.error.ErrorFault;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * The named resource does not exist.
 */
@SmithyGenerated
public final class ResourceNotFoundException extends CloudWatchException {

    public static final Schema $SCHEMA = Schemas.RESOURCE_NOT_FOUND_EXCEPTION;
    private static final Schema $SCHEMA_RESOURCE_TYPE = $SCHEMA.member("ResourceType");
    private static final Schema $SCHEMA_RESOURCE_ID = $SCHEMA.member("ResourceId");
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceType;
    private final transient String resourceId;

    private ResourceNotFoundException(Builder builder) {
        super($SCHEMA, builder.message, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
        this.resourceType = builder.resourceType;
        this.resourceId = builder.resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (resourceType != null) {
            serializer.writeString($SCHEMA_RESOURCE_TYPE, resourceType);
        }
        if (resourceId != null) {
            serializer.writeString($SCHEMA_RESOURCE_ID, resourceId);
        }
        if (getMessage() != null) {
            serializer.writeString($SCHEMA_MESSAGE, getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_TYPE, member, resourceType);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ID, member, resourceId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, getMessage());
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ResourceNotFoundException}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.message(getMessage());
        builder.resourceType(this.resourceType);
        builder.resourceId(this.resourceId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ResourceNotFoundException}.
     */
    public static final class Builder implements ShapeBuilder<ResourceNotFoundException> {
        private String resourceType;
        private String resourceId;
        private String message;
        private Throwable $cause;
        private Boolean $captureStackTrace;
        private boolean $deserialized;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder withStackTrace() {
            this.$captureStackTrace = true;
            return this;
        }

        public Builder withoutStackTrace() {
            this.$captureStackTrace = false;
            return this;
        }

        public Builder withCause(Throwable cause) {
            this.$cause = cause;
            return this;
        }

        @Override
        public ResourceNotFoundException build() {
            return new ResourceNotFoundException(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceType((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_TYPE, member, value));
                case 1 -> resourceId((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ID, member, value));
                case 2 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            this.$deserialized = true;
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
                    case 0 -> builder.resourceType(de.readString(member));
                    case 1 -> builder.resourceId(de.readString(member));
                    case 2 -> builder.message(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
