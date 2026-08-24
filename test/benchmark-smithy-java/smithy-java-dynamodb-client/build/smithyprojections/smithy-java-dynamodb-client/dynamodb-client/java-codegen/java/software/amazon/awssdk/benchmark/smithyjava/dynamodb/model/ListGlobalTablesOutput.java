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

@SmithyGenerated
public final class ListGlobalTablesOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_GLOBAL_TABLES_OUTPUT;
    private static final Schema $SCHEMA_GLOBAL_TABLES = $SCHEMA.member("GlobalTables");
    private static final Schema $SCHEMA_LAST_EVALUATED_GLOBAL_TABLE_NAME = $SCHEMA.member("LastEvaluatedGlobalTableName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<GlobalTable> globalTables;
    private final transient String lastEvaluatedGlobalTableName;

    private ListGlobalTablesOutput(Builder builder) {
        this.globalTables = builder.globalTables == null ? null : Collections.unmodifiableList(builder.globalTables);
        this.lastEvaluatedGlobalTableName = builder.lastEvaluatedGlobalTableName;
    }

    /**
     * List of global table names.
     */
    public List<GlobalTable> getGlobalTables() {
        if (globalTables == null) {
            return Collections.emptyList();
        }
        return globalTables;
    }

    public boolean hasGlobalTables() {
        return globalTables != null;
    }

    /**
     * Last evaluated global table name.
     */
    public String getLastEvaluatedGlobalTableName() {
        return lastEvaluatedGlobalTableName;
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
        ListGlobalTablesOutput that = (ListGlobalTablesOutput) other;
        return Objects.equals(this.lastEvaluatedGlobalTableName, that.lastEvaluatedGlobalTableName)
               && Objects.equals(this.globalTables, that.globalTables);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTables);
        $hc = 31 * $hc + Objects.hashCode(lastEvaluatedGlobalTableName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (globalTables != null) {
            serializer.writeList($SCHEMA_GLOBAL_TABLES, globalTables, globalTables.size(), SharedSerde.GlobalTableListSerializer.INSTANCE);
        }
        if (lastEvaluatedGlobalTableName != null) {
            serializer.writeString($SCHEMA_LAST_EVALUATED_GLOBAL_TABLE_NAME, lastEvaluatedGlobalTableName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLES, member, globalTables);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_GLOBAL_TABLE_NAME, member, lastEvaluatedGlobalTableName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListGlobalTablesOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTables(this.globalTables);
        builder.lastEvaluatedGlobalTableName(this.lastEvaluatedGlobalTableName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListGlobalTablesOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListGlobalTablesOutput> {
        private List<GlobalTable> globalTables;
        private String lastEvaluatedGlobalTableName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * List of global table names.
         *
         * @return this builder.
         */
        public Builder globalTables(List<GlobalTable> globalTables) {
            this.globalTables = globalTables;
            return this;
        }

        /**
         * Last evaluated global table name.
         *
         * @return this builder.
         */
        public Builder lastEvaluatedGlobalTableName(String lastEvaluatedGlobalTableName) {
            this.lastEvaluatedGlobalTableName = lastEvaluatedGlobalTableName;
            return this;
        }

        @Override
        public ListGlobalTablesOutput build() {
            return new ListGlobalTablesOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTables((List<GlobalTable>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLES, member, value));
                case 1 -> lastEvaluatedGlobalTableName((String) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_GLOBAL_TABLE_NAME, member, value));
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
                    case 0 -> builder.globalTables(SharedSerde.deserializeGlobalTableList(member, de));
                    case 1 -> builder.lastEvaluatedGlobalTableName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
