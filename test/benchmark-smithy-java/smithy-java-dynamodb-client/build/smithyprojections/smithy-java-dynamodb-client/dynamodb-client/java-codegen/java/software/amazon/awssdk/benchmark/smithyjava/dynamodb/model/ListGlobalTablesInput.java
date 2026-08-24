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
public final class ListGlobalTablesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_GLOBAL_TABLES_INPUT;
    private static final Schema $SCHEMA_EXCLUSIVE_START_GLOBAL_TABLE_NAME = $SCHEMA.member("ExclusiveStartGlobalTableName");
    private static final Schema $SCHEMA_LIMIT = $SCHEMA.member("Limit");
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String exclusiveStartGlobalTableName;
    private final transient Integer limit;
    private final transient String regionName;

    private ListGlobalTablesInput(Builder builder) {
        this.exclusiveStartGlobalTableName = builder.exclusiveStartGlobalTableName;
        this.limit = builder.limit;
        this.regionName = builder.regionName;
    }

    /**
     * The first global table name that this operation will evaluate.
     */
    public String getExclusiveStartGlobalTableName() {
        return exclusiveStartGlobalTableName;
    }

    /**
     * The maximum number of table names to return, if the parameter is not specified DynamoDB defaults to 100.
     *
     * <p>If the number of global tables DynamoDB finds reaches this limit, it stops the operation and returns the table
     * names collected up to that point, with a table name in the <code>LastEvaluatedGlobalTableName</code> to apply in
     * a subsequent operation to the <code>ExclusiveStartGlobalTableName</code> parameter.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Lists the global tables in a specific Region.
     */
    public String getRegionName() {
        return regionName;
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
        ListGlobalTablesInput that = (ListGlobalTablesInput) other;
        return Objects.equals(this.limit, that.limit)
               && Objects.equals(this.exclusiveStartGlobalTableName, that.exclusiveStartGlobalTableName)
               && Objects.equals(this.regionName, that.regionName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exclusiveStartGlobalTableName);
        $hc = 31 * $hc + Objects.hashCode(limit);
        $hc = 31 * $hc + Objects.hashCode(regionName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exclusiveStartGlobalTableName != null) {
            serializer.writeString($SCHEMA_EXCLUSIVE_START_GLOBAL_TABLE_NAME, exclusiveStartGlobalTableName);
        }
        if (limit != null) {
            serializer.writeInteger($SCHEMA_LIMIT, limit);
        }
        if (regionName != null) {
            serializer.writeString($SCHEMA_REGION_NAME, regionName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_GLOBAL_TABLE_NAME, member, exclusiveStartGlobalTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, limit);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListGlobalTablesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exclusiveStartGlobalTableName(this.exclusiveStartGlobalTableName);
        builder.limit(this.limit);
        builder.regionName(this.regionName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListGlobalTablesInput}.
     */
    public static final class Builder implements ShapeBuilder<ListGlobalTablesInput> {
        private String exclusiveStartGlobalTableName;
        private Integer limit;
        private String regionName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The first global table name that this operation will evaluate.
         *
         * @return this builder.
         */
        public Builder exclusiveStartGlobalTableName(String exclusiveStartGlobalTableName) {
            this.exclusiveStartGlobalTableName = exclusiveStartGlobalTableName;
            return this;
        }

        /**
         * The maximum number of table names to return, if the parameter is not specified DynamoDB defaults to 100.
         *
         * <p>If the number of global tables DynamoDB finds reaches this limit, it stops the operation and returns the table
         * names collected up to that point, with a table name in the <code>LastEvaluatedGlobalTableName</code> to apply in
         * a subsequent operation to the <code>ExclusiveStartGlobalTableName</code> parameter.
         *
         * @return this builder.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Lists the global tables in a specific Region.
         *
         * @return this builder.
         */
        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        @Override
        public ListGlobalTablesInput build() {
            return new ListGlobalTablesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exclusiveStartGlobalTableName((String) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_GLOBAL_TABLE_NAME, member, value));
                case 1 -> limit((Integer) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, value));
                case 2 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
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
                    case 0 -> builder.exclusiveStartGlobalTableName(de.readString(member));
                    case 1 -> builder.limit(de.readInteger(member));
                    case 2 -> builder.regionName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
