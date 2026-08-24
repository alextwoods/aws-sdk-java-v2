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
 * Represents the output of an <code>UpdateTable</code> operation.
 */
@SmithyGenerated
public final class UpdateTableOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_TABLE_OUTPUT;
    private static final Schema $SCHEMA_TABLE_DESCRIPTION = $SCHEMA.member("TableDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TableDescription tableDescription;

    private UpdateTableOutput(Builder builder) {
        this.tableDescription = builder.tableDescription;
    }

    /**
     * Represents the properties of the table.
     */
    public TableDescription getTableDescription() {
        return tableDescription;
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
        UpdateTableOutput that = (UpdateTableOutput) other;
        return Objects.equals(this.tableDescription, that.tableDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableDescription != null) {
            serializer.writeStruct($SCHEMA_TABLE_DESCRIPTION, tableDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_DESCRIPTION, member, tableDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateTableOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableDescription(this.tableDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateTableOutput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateTableOutput> {
        private TableDescription tableDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the properties of the table.
         *
         * @return this builder.
         */
        public Builder tableDescription(TableDescription tableDescription) {
            this.tableDescription = tableDescription;
            return this;
        }

        @Override
        public UpdateTableOutput build() {
            return new UpdateTableOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableDescription((TableDescription) SchemaUtils.validateSameMember($SCHEMA_TABLE_DESCRIPTION, member, value));
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
                    case 0 -> builder.tableDescription(TableDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
