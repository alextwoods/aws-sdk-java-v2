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
 * Represents one of the following:
 *
 * <ul>
 *   <li>
 *     A new global secondary index to be added to an existing table.
 *   </li>
 *   <li>
 *     New provisioned throughput parameters for an existing global secondary index.
 *   </li>
 *   <li>
 *     An existing global secondary index to be removed from an existing table.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class GlobalSecondaryIndexUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_SECONDARY_INDEX_UPDATE;
    private static final Schema $SCHEMA_UPDATE = $SCHEMA.member("Update");
    private static final Schema $SCHEMA_CREATE = $SCHEMA.member("Create");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient UpdateGlobalSecondaryIndexAction update;
    private final transient CreateGlobalSecondaryIndexAction create;
    private final transient DeleteGlobalSecondaryIndexAction delete;

    private GlobalSecondaryIndexUpdate(Builder builder) {
        this.update = builder.update;
        this.create = builder.create;
        this.delete = builder.delete;
    }

    /**
     * The name of an existing global secondary index, along with new provisioned throughput settings to be applied to
     * that index.
     */
    public UpdateGlobalSecondaryIndexAction getUpdate() {
        return update;
    }

    /**
     * The parameters required for creating a global secondary index on an existing table:
     *
     * <ul>
     *   <li>
     *     <code>IndexName </code>
     *   </li>
     *   <li>
     *     <code>KeySchema </code>
     *   </li>
     *   <li>
     *     <code>AttributeDefinitions </code>
     *   </li>
     *   <li>
     *     <code>Projection </code>
     *   </li>
     *   <li>
     *     <code>ProvisionedThroughput </code>
     *   </li>
     * </ul>
     */
    public CreateGlobalSecondaryIndexAction getCreate() {
        return create;
    }

    /**
     * The name of an existing global secondary index to be removed.
     */
    public DeleteGlobalSecondaryIndexAction getDelete() {
        return delete;
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
        GlobalSecondaryIndexUpdate that = (GlobalSecondaryIndexUpdate) other;
        return Objects.equals(this.update, that.update)
               && Objects.equals(this.create, that.create)
               && Objects.equals(this.delete, that.delete);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(update);
        $hc = 31 * $hc + Objects.hashCode(create);
        $hc = 31 * $hc + Objects.hashCode(delete);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (update != null) {
            serializer.writeStruct($SCHEMA_UPDATE, update);
        }
        if (create != null) {
            serializer.writeStruct($SCHEMA_CREATE, create);
        }
        if (delete != null) {
            serializer.writeStruct($SCHEMA_DELETE, delete);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPDATE, member, update);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, create);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, delete);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalSecondaryIndexUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.update(this.update);
        builder.create(this.create);
        builder.delete(this.delete);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalSecondaryIndexUpdate}.
     */
    public static final class Builder implements ShapeBuilder<GlobalSecondaryIndexUpdate> {
        private UpdateGlobalSecondaryIndexAction update;
        private CreateGlobalSecondaryIndexAction create;
        private DeleteGlobalSecondaryIndexAction delete;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of an existing global secondary index, along with new provisioned throughput settings to be applied to
         * that index.
         *
         * @return this builder.
         */
        public Builder update(UpdateGlobalSecondaryIndexAction update) {
            this.update = update;
            return this;
        }

        /**
         * The parameters required for creating a global secondary index on an existing table:
         *
         * <ul>
         *   <li>
         *     <code>IndexName </code>
         *   </li>
         *   <li>
         *     <code>KeySchema </code>
         *   </li>
         *   <li>
         *     <code>AttributeDefinitions </code>
         *   </li>
         *   <li>
         *     <code>Projection </code>
         *   </li>
         *   <li>
         *     <code>ProvisionedThroughput </code>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder create(CreateGlobalSecondaryIndexAction create) {
            this.create = create;
            return this;
        }

        /**
         * The name of an existing global secondary index to be removed.
         *
         * @return this builder.
         */
        public Builder delete(DeleteGlobalSecondaryIndexAction delete) {
            this.delete = delete;
            return this;
        }

        @Override
        public GlobalSecondaryIndexUpdate build() {
            return new GlobalSecondaryIndexUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> update((UpdateGlobalSecondaryIndexAction) SchemaUtils.validateSameMember($SCHEMA_UPDATE, member, value));
                case 1 -> create((CreateGlobalSecondaryIndexAction) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, value));
                case 2 -> delete((DeleteGlobalSecondaryIndexAction) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
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
                    case 0 -> builder.update(UpdateGlobalSecondaryIndexAction.builder().deserializeMember(de, member).build());
                    case 1 -> builder.create(CreateGlobalSecondaryIndexAction.builder().deserializeMember(de, member).build());
                    case 2 -> builder.delete(DeleteGlobalSecondaryIndexAction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
