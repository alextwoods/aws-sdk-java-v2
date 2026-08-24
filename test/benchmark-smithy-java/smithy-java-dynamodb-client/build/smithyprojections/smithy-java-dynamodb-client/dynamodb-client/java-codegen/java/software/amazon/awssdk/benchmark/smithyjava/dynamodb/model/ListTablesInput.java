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
 * Represents the input of a <code>ListTables</code> operation.
 */
@SmithyGenerated
public final class ListTablesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_TABLES_INPUT;
    private static final Schema $SCHEMA_EXCLUSIVE_START_TABLE_NAME = $SCHEMA.member("ExclusiveStartTableName");
    private static final Schema $SCHEMA_LIMIT = $SCHEMA.member("Limit");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String exclusiveStartTableName;
    private final transient Integer limit;

    private ListTablesInput(Builder builder) {
        this.exclusiveStartTableName = builder.exclusiveStartTableName;
        this.limit = builder.limit;
    }

    /**
     * The first table name that this operation will evaluate. Use the value that was returned for
     * <code>LastEvaluatedTableName</code> in a previous operation, so that you can obtain the next page of results.
     */
    public String getExclusiveStartTableName() {
        return exclusiveStartTableName;
    }

    /**
     * A maximum number of table names to return. If this parameter is not specified, the limit is 100.
     */
    public Integer getLimit() {
        return limit;
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
        ListTablesInput that = (ListTablesInput) other;
        return Objects.equals(this.limit, that.limit)
               && Objects.equals(this.exclusiveStartTableName, that.exclusiveStartTableName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exclusiveStartTableName);
        $hc = 31 * $hc + Objects.hashCode(limit);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exclusiveStartTableName != null) {
            serializer.writeString($SCHEMA_EXCLUSIVE_START_TABLE_NAME, exclusiveStartTableName);
        }
        if (limit != null) {
            serializer.writeInteger($SCHEMA_LIMIT, limit);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_TABLE_NAME, member, exclusiveStartTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, limit);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListTablesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exclusiveStartTableName(this.exclusiveStartTableName);
        builder.limit(this.limit);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListTablesInput}.
     */
    public static final class Builder implements ShapeBuilder<ListTablesInput> {
        private String exclusiveStartTableName;
        private Integer limit;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The first table name that this operation will evaluate. Use the value that was returned for
         * <code>LastEvaluatedTableName</code> in a previous operation, so that you can obtain the next page of results.
         *
         * @return this builder.
         */
        public Builder exclusiveStartTableName(String exclusiveStartTableName) {
            this.exclusiveStartTableName = exclusiveStartTableName;
            return this;
        }

        /**
         * A maximum number of table names to return. If this parameter is not specified, the limit is 100.
         *
         * @return this builder.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public ListTablesInput build() {
            return new ListTablesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exclusiveStartTableName((String) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_TABLE_NAME, member, value));
                case 1 -> limit((Integer) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, value));
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
                    case 0 -> builder.exclusiveStartTableName(de.readString(member));
                    case 1 -> builder.limit(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
