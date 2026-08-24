package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.Map;
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
 * The capacity units consumed by an operation. The data returned includes the total provisioned throughput consumed,
 * along with statistics for the table and any indexes involved in the operation. <code>ConsumedCapacity</code> is only
 * returned if the request asked for it. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a> in the <i>Amazon
 * DynamoDB Developer Guide</i>.
 */
@SmithyGenerated
public final class ConsumedCapacity implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CONSUMED_CAPACITY;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_CAPACITY_UNITS = $SCHEMA.member("CapacityUnits");
    private static final Schema $SCHEMA_READ_CAPACITY_UNITS = $SCHEMA.member("ReadCapacityUnits");
    private static final Schema $SCHEMA_WRITE_CAPACITY_UNITS = $SCHEMA.member("WriteCapacityUnits");
    private static final Schema $SCHEMA_TABLE = $SCHEMA.member("Table");
    private static final Schema $SCHEMA_LOCAL_SECONDARY_INDEXES = $SCHEMA.member("LocalSecondaryIndexes");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_VECTOR_INDEXES = $SCHEMA.member("VectorIndexes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient Double capacityUnits;
    private final transient Double readCapacityUnits;
    private final transient Double writeCapacityUnits;
    private final transient Capacity table;
    private final transient Map<String, Capacity> localSecondaryIndexes;
    private final transient Map<String, Capacity> globalSecondaryIndexes;
    private final transient Map<String, VectorCapacity> vectorIndexes;

    private ConsumedCapacity(Builder builder) {
        this.tableName = builder.tableName;
        this.capacityUnits = builder.capacityUnits;
        this.readCapacityUnits = builder.readCapacityUnits;
        this.writeCapacityUnits = builder.writeCapacityUnits;
        this.table = builder.table;
        this.localSecondaryIndexes = builder.localSecondaryIndexes == null ? null : Collections.unmodifiableMap(builder.localSecondaryIndexes);
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableMap(builder.globalSecondaryIndexes);
        this.vectorIndexes = builder.vectorIndexes == null ? null : Collections.unmodifiableMap(builder.vectorIndexes);
    }

    /**
     * The name of the table that was affected by the operation. If you had specified the Amazon Resource Name (ARN) of
     * a table in the input, you'll see the table ARN in the response.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The total number of capacity units consumed by the operation.
     */
    public Double getCapacityUnits() {
        return capacityUnits;
    }

    /**
     * The total number of read capacity units consumed by the operation.
     */
    public Double getReadCapacityUnits() {
        return readCapacityUnits;
    }

    /**
     * The total number of write capacity units consumed by the operation.
     */
    public Double getWriteCapacityUnits() {
        return writeCapacityUnits;
    }

    /**
     * The amount of throughput consumed on the table affected by the operation.
     */
    public Capacity getTable() {
        return table;
    }

    /**
     * The amount of throughput consumed on each local index affected by the operation.
     */
    public Map<String, Capacity> getLocalSecondaryIndexes() {
        if (localSecondaryIndexes == null) {
            return Collections.emptyMap();
        }
        return localSecondaryIndexes;
    }

    public boolean hasLocalSecondaryIndexes() {
        return localSecondaryIndexes != null;
    }

    /**
     * The amount of throughput consumed on each global index affected by the operation.
     */
    public Map<String, Capacity> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyMap();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    /**
     * The amount of throughput consumed on each vector index affected by the operation. Each entry contains
     * <code>VectorWriteRequestBytes</code> (for write operations) or <code>VectorSearchRequestBytes</code> (for search
     * operations).
     */
    public Map<String, VectorCapacity> getVectorIndexes() {
        if (vectorIndexes == null) {
            return Collections.emptyMap();
        }
        return vectorIndexes;
    }

    public boolean hasVectorIndexes() {
        return vectorIndexes != null;
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
        ConsumedCapacity that = (ConsumedCapacity) other;
        return Objects.equals(this.capacityUnits, that.capacityUnits)
               && Objects.equals(this.readCapacityUnits, that.readCapacityUnits)
               && Objects.equals(this.writeCapacityUnits, that.writeCapacityUnits)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.table, that.table)
               && Objects.equals(this.localSecondaryIndexes, that.localSecondaryIndexes)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes)
               && Objects.equals(this.vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(capacityUnits);
        $hc = 31 * $hc + Objects.hashCode(readCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(writeCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(table);
        $hc = 31 * $hc + Objects.hashCode(localSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(vectorIndexes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (capacityUnits != null) {
            serializer.writeDouble($SCHEMA_CAPACITY_UNITS, capacityUnits);
        }
        if (readCapacityUnits != null) {
            serializer.writeDouble($SCHEMA_READ_CAPACITY_UNITS, readCapacityUnits);
        }
        if (writeCapacityUnits != null) {
            serializer.writeDouble($SCHEMA_WRITE_CAPACITY_UNITS, writeCapacityUnits);
        }
        if (table != null) {
            serializer.writeStruct($SCHEMA_TABLE, table);
        }
        if (localSecondaryIndexes != null) {
            serializer.writeMap($SCHEMA_LOCAL_SECONDARY_INDEXES, localSecondaryIndexes, localSecondaryIndexes.size(), SharedSerde.SecondaryIndexesCapacityMapSerializer.INSTANCE);
        }
        if (globalSecondaryIndexes != null) {
            serializer.writeMap($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.SecondaryIndexesCapacityMapSerializer.INSTANCE);
        }
        if (vectorIndexes != null) {
            serializer.writeMap($SCHEMA_VECTOR_INDEXES, vectorIndexes, vectorIndexes.size(), SharedSerde.VectorIndexesCapacityMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CAPACITY_UNITS, member, capacityUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, readCapacityUnits);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, writeCapacityUnits);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE, member, table);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, localSecondaryIndexes);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, vectorIndexes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ConsumedCapacity}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.capacityUnits(this.capacityUnits);
        builder.readCapacityUnits(this.readCapacityUnits);
        builder.writeCapacityUnits(this.writeCapacityUnits);
        builder.table(this.table);
        builder.localSecondaryIndexes(this.localSecondaryIndexes);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.vectorIndexes(this.vectorIndexes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ConsumedCapacity}.
     */
    public static final class Builder implements ShapeBuilder<ConsumedCapacity> {
        private String tableName;
        private Double capacityUnits;
        private Double readCapacityUnits;
        private Double writeCapacityUnits;
        private Capacity table;
        private Map<String, Capacity> localSecondaryIndexes;
        private Map<String, Capacity> globalSecondaryIndexes;
        private Map<String, VectorCapacity> vectorIndexes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table that was affected by the operation. If you had specified the Amazon Resource Name (ARN) of
         * a table in the input, you'll see the table ARN in the response.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The total number of capacity units consumed by the operation.
         *
         * @return this builder.
         */
        public Builder capacityUnits(Double capacityUnits) {
            this.capacityUnits = capacityUnits;
            return this;
        }

        /**
         * The total number of read capacity units consumed by the operation.
         *
         * @return this builder.
         */
        public Builder readCapacityUnits(Double readCapacityUnits) {
            this.readCapacityUnits = readCapacityUnits;
            return this;
        }

        /**
         * The total number of write capacity units consumed by the operation.
         *
         * @return this builder.
         */
        public Builder writeCapacityUnits(Double writeCapacityUnits) {
            this.writeCapacityUnits = writeCapacityUnits;
            return this;
        }

        /**
         * The amount of throughput consumed on the table affected by the operation.
         *
         * @return this builder.
         */
        public Builder table(Capacity table) {
            this.table = table;
            return this;
        }

        /**
         * The amount of throughput consumed on each local index affected by the operation.
         *
         * @return this builder.
         */
        public Builder localSecondaryIndexes(Map<String, Capacity> localSecondaryIndexes) {
            this.localSecondaryIndexes = localSecondaryIndexes;
            return this;
        }

        /**
         * The amount of throughput consumed on each global index affected by the operation.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(Map<String, Capacity> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * The amount of throughput consumed on each vector index affected by the operation. Each entry contains
         * <code>VectorWriteRequestBytes</code> (for write operations) or <code>VectorSearchRequestBytes</code> (for search
         * operations).
         *
         * @return this builder.
         */
        public Builder vectorIndexes(Map<String, VectorCapacity> vectorIndexes) {
            this.vectorIndexes = vectorIndexes;
            return this;
        }

        @Override
        public ConsumedCapacity build() {
            return new ConsumedCapacity(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> capacityUnits((Double) SchemaUtils.validateSameMember($SCHEMA_CAPACITY_UNITS, member, value));
                case 2 -> readCapacityUnits((Double) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, value));
                case 3 -> writeCapacityUnits((Double) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, value));
                case 4 -> table((Capacity) SchemaUtils.validateSameMember($SCHEMA_TABLE, member, value));
                case 5 -> localSecondaryIndexes((Map<String, Capacity>) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, value));
                case 6 -> globalSecondaryIndexes((Map<String, Capacity>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 7 -> vectorIndexes((Map<String, VectorCapacity>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, value));
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.capacityUnits(de.readDouble(member));
                    case 2 -> builder.readCapacityUnits(de.readDouble(member));
                    case 3 -> builder.writeCapacityUnits(de.readDouble(member));
                    case 4 -> builder.table(Capacity.builder().deserializeMember(de, member).build());
                    case 5 -> builder.localSecondaryIndexes(SharedSerde.deserializeSecondaryIndexesCapacityMap(member, de));
                    case 6 -> builder.globalSecondaryIndexes(SharedSerde.deserializeSecondaryIndexesCapacityMap(member, de));
                    case 7 -> builder.vectorIndexes(SharedSerde.deserializeVectorIndexesCapacityMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
