package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
 * Represents the output of a <code>ListTables</code> operation.
 */
@SmithyGenerated
public final class ListTablesOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_TABLES_OUTPUT;
    private static final Schema $SCHEMA_TABLE_NAMES = $SCHEMA.member("TableNames");
    private static final Schema $SCHEMA_LAST_EVALUATED_TABLE_NAME = $SCHEMA.member("LastEvaluatedTableName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> tableNames;
    private final transient String lastEvaluatedTableName;

    private ListTablesOutput(Builder builder) {
        this.tableNames = builder.tableNames == null ? null : Collections.unmodifiableList(builder.tableNames);
        this.lastEvaluatedTableName = builder.lastEvaluatedTableName;
    }

    /**
     * The names of the tables associated with the current account at the current endpoint. The maximum size of this
     * array is 100.
     *
     * <p>If <code>LastEvaluatedTableName</code> also appears in the output, you can use this value as the <code>
     * ExclusiveStartTableName</code> parameter in a subsequent <code>ListTables</code> request and obtain the next page
     * of results.
     */
    public List<String> getTableNames() {
        if (tableNames == null) {
            return Collections.emptyList();
        }
        return tableNames;
    }

    public boolean hasTableNames() {
        return tableNames != null;
    }

    /**
     * The name of the last table in the current page of results. Use this value as the <code>ExclusiveStartTableName</code>
     * in a new request to obtain the next page of results, until all the table names are returned.
     *
     * <p>If you do not receive a <code>LastEvaluatedTableName</code> value in the response, this means that there are
     * no more table names to be retrieved.
     */
    public String getLastEvaluatedTableName() {
        return lastEvaluatedTableName;
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
        ListTablesOutput that = (ListTablesOutput) other;
        return Objects.equals(this.lastEvaluatedTableName, that.lastEvaluatedTableName)
               && Objects.equals(this.tableNames, that.tableNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableNames);
        $hc = 31 * $hc + Objects.hashCode(lastEvaluatedTableName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableNames != null) {
            serializer.writeList($SCHEMA_TABLE_NAMES, tableNames, tableNames.size(), SharedSerde.TableNameListSerializer.INSTANCE);
        }
        if (lastEvaluatedTableName != null) {
            serializer.writeString($SCHEMA_LAST_EVALUATED_TABLE_NAME, lastEvaluatedTableName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAMES, member, tableNames);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_TABLE_NAME, member, lastEvaluatedTableName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListTablesOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableNames(this.tableNames);
        builder.lastEvaluatedTableName(this.lastEvaluatedTableName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListTablesOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListTablesOutput> {
        private List<String> tableNames;
        private String lastEvaluatedTableName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The names of the tables associated with the current account at the current endpoint. The maximum size of this
         * array is 100.
         *
         * <p>If <code>LastEvaluatedTableName</code> also appears in the output, you can use this value as the <code>
         * ExclusiveStartTableName</code> parameter in a subsequent <code>ListTables</code> request and obtain the next page
         * of results.
         *
         * @return this builder.
         */
        public Builder tableNames(List<String> tableNames) {
            this.tableNames = tableNames;
            return this;
        }

        /**
         * The name of the last table in the current page of results. Use this value as the <code>ExclusiveStartTableName</code>
         * in a new request to obtain the next page of results, until all the table names are returned.
         *
         * <p>If you do not receive a <code>LastEvaluatedTableName</code> value in the response, this means that there are
         * no more table names to be retrieved.
         *
         * @return this builder.
         */
        public Builder lastEvaluatedTableName(String lastEvaluatedTableName) {
            this.lastEvaluatedTableName = lastEvaluatedTableName;
            return this;
        }

        @Override
        public ListTablesOutput build() {
            return new ListTablesOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableNames((List<String>) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAMES, member, value));
                case 1 -> lastEvaluatedTableName((String) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_TABLE_NAME, member, value));
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
                    case 0 -> builder.tableNames(SharedSerde.deserializeTableNameList(member, de));
                    case 1 -> builder.lastEvaluatedTableName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
