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

@SmithyGenerated
public final class CreateGlobalTableOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_GLOBAL_TABLE_OUTPUT;
    private static final Schema $SCHEMA_GLOBAL_TABLE_DESCRIPTION = $SCHEMA.member("GlobalTableDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient GlobalTableDescription globalTableDescription;

    private CreateGlobalTableOutput(Builder builder) {
        this.globalTableDescription = builder.globalTableDescription;
    }

    /**
     * Contains the details of the global table.
     */
    public GlobalTableDescription getGlobalTableDescription() {
        return globalTableDescription;
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
        CreateGlobalTableOutput that = (CreateGlobalTableOutput) other;
        return Objects.equals(this.globalTableDescription, that.globalTableDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTableDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (globalTableDescription != null) {
            serializer.writeStruct($SCHEMA_GLOBAL_TABLE_DESCRIPTION, globalTableDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_DESCRIPTION, member, globalTableDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateGlobalTableOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTableDescription(this.globalTableDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateGlobalTableOutput}.
     */
    public static final class Builder implements ShapeBuilder<CreateGlobalTableOutput> {
        private GlobalTableDescription globalTableDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Contains the details of the global table.
         *
         * @return this builder.
         */
        public Builder globalTableDescription(GlobalTableDescription globalTableDescription) {
            this.globalTableDescription = globalTableDescription;
            return this;
        }

        @Override
        public CreateGlobalTableOutput build() {
            return new CreateGlobalTableOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTableDescription((GlobalTableDescription) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_DESCRIPTION, member, value));
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
                    case 0 -> builder.globalTableDescription(GlobalTableDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
