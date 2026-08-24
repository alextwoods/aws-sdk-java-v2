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
 * Represents the properties of a global secondary index.
 */
@SmithyGenerated
public final class GlobalSecondaryIndexDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_SECONDARY_INDEX_DESCRIPTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_PROJECTION = $SCHEMA.member("Projection");
    private static final Schema $SCHEMA_INDEX_STATUS = $SCHEMA.member("IndexStatus");
    private static final Schema $SCHEMA_BACKFILLING = $SCHEMA.member("Backfilling");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_INDEX_SIZE_BYTES = $SCHEMA.member("IndexSizeBytes");
    private static final Schema $SCHEMA_ITEM_COUNT = $SCHEMA.member("ItemCount");
    private static final Schema $SCHEMA_INDEX_ARN = $SCHEMA.member("IndexArn");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient List<KeySchemaElement> keySchema;
    private final transient Projection projection;
    private final transient IndexStatus indexStatus;
    private final transient Boolean backfilling;
    private final transient ProvisionedThroughputDescription provisionedThroughput;
    private final transient Long indexSizeBytes;
    private final transient Long itemCount;
    private final transient String indexArn;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient GlobalSecondaryIndexWarmThroughputDescription warmThroughput;

    private GlobalSecondaryIndexDescription(Builder builder) {
        this.indexName = builder.indexName;
        this.keySchema = builder.keySchema == null ? null : Collections.unmodifiableList(builder.keySchema);
        this.projection = builder.projection;
        this.indexStatus = builder.indexStatus;
        this.backfilling = builder.backfilling;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.indexSizeBytes = builder.indexSizeBytes;
        this.itemCount = builder.itemCount;
        this.indexArn = builder.indexArn;
        this.onDemandThroughput = builder.onDemandThroughput;
        this.warmThroughput = builder.warmThroughput;
    }

    /**
     * The name of the global secondary index.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The complete key schema for a global secondary index, which consists of one or more pairs of attribute names and
     * key types:
     *
     * <ul>
     *   <li>
     *     <code>HASH</code> - partition key
     *   </li>
     *   <li>
     *     <code>RANGE</code> - sort key
     *   </li>
     * </ul>
     *
     * <p>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute" derives
     * from DynamoDB's usage of an internal hash function to evenly distribute data items across partitions, based on
     * their partition key values.
     *
     * <p>The sort key of an item is also known as its <i>range attribute</i>. The term "range attribute" derives from
     * the way DynamoDB stores items with the same partition key physically close together, in sorted order by the sort
     * key value.
     */
    public List<KeySchemaElement> getKeySchema() {
        if (keySchema == null) {
            return Collections.emptyList();
        }
        return keySchema;
    }

    public boolean hasKeySchema() {
        return keySchema != null;
    }

    /**
     * Represents attributes that are copied (projected) from the table into the global secondary index. These are in
     * addition to the primary key attributes and index key attributes, which are automatically projected.
     */
    public Projection getProjection() {
        return projection;
    }

    /**
     * The current state of the global secondary index:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The index is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The index is being updated.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The index is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The index is ready for use.
     *   </li>
     * </ul>
     */
    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    /**
     * Indicates whether the index is currently backfilling. <i>Backfilling</i> is the process of reading items from the
     * table and determining whether they can be added to the index. (Not all items will qualify: For example, a
     * partition key cannot have any duplicate values.) If an item can be added to the index, DynamoDB will do so. After
     * all items have been processed, the backfilling operation is complete and <code>Backfilling</code> is false.
     *
     * <p>You can delete an index that is being created during the <code>Backfilling</code> phase when <code>IndexStatus</code>
     * is set to CREATING and <code>Backfilling</code> is true. You can't delete the index that is being created when <code>
     * IndexStatus</code> is set to CREATING and <code>Backfilling</code> is false.
     *
     * <p>For indexes that were created during a <code>CreateTable</code> operation, the <code>Backfilling</code>
     * attribute does not appear in the <code>DescribeTable</code> output.
     */
    public Boolean isBackfilling() {
        return backfilling;
    }

    /**
     * Represents the provisioned throughput settings for the specified global secondary index.
     *
     * <p>For current minimum and maximum provisioned throughput values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ProvisionedThroughputDescription getProvisionedThroughput() {
        return provisionedThroughput;
    }

    /**
     * The total size of the specified index, in bytes. DynamoDB updates this value approximately every six hours.
     * Recent changes might not be reflected in this value.
     */
    public Long getIndexSizeBytes() {
        return indexSizeBytes;
    }

    /**
     * The number of items in the specified index. DynamoDB updates this value approximately every six hours. Recent
     * changes might not be reflected in this value.
     */
    public Long getItemCount() {
        return itemCount;
    }

    /**
     * The Amazon Resource Name (ARN) that uniquely identifies the index.
     */
    public String getIndexArn() {
        return indexArn;
    }

    /**
     * The maximum number of read and write units for the specified global secondary index. If you use this parameter,
     * you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
     */
    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    /**
     * Represents the warm throughput value (in read units per second and write units per second) for the specified
     * secondary index.
     */
    public GlobalSecondaryIndexWarmThroughputDescription getWarmThroughput() {
        return warmThroughput;
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
        GlobalSecondaryIndexDescription that = (GlobalSecondaryIndexDescription) other;
        return Objects.equals(this.backfilling, that.backfilling)
               && Objects.equals(this.indexSizeBytes, that.indexSizeBytes)
               && Objects.equals(this.itemCount, that.itemCount)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.indexArn, that.indexArn)
               && Objects.equals(this.indexStatus, that.indexStatus)
               && Objects.equals(this.projection, that.projection)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.warmThroughput, that.warmThroughput)
               && Objects.equals(this.keySchema, that.keySchema);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(projection);
        $hc = 31 * $hc + Objects.hashCode(indexStatus);
        $hc = 31 * $hc + Objects.hashCode(backfilling);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(indexSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(itemCount);
        $hc = 31 * $hc + Objects.hashCode(indexArn);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
        $hc = 31 * $hc + Objects.hashCode(warmThroughput);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        if (keySchema != null) {
            serializer.writeList($SCHEMA_KEY_SCHEMA, keySchema, keySchema.size(), SharedSerde.KeySchemaSerializer.INSTANCE);
        }
        if (projection != null) {
            serializer.writeStruct($SCHEMA_PROJECTION, projection);
        }
        if (indexStatus != null) {
            serializer.writeString($SCHEMA_INDEX_STATUS, indexStatus.getValue());
        }
        if (backfilling != null) {
            serializer.writeBoolean($SCHEMA_BACKFILLING, backfilling);
        }
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (indexSizeBytes != null) {
            serializer.writeLong($SCHEMA_INDEX_SIZE_BYTES, indexSizeBytes);
        }
        if (itemCount != null) {
            serializer.writeLong($SCHEMA_ITEM_COUNT, itemCount);
        }
        if (indexArn != null) {
            serializer.writeString($SCHEMA_INDEX_ARN, indexArn);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
        }
        if (warmThroughput != null) {
            serializer.writeStruct($SCHEMA_WARM_THROUGHPUT, warmThroughput);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, keySchema);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, projection);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, indexStatus);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKFILLING, member, backfilling);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_SIZE_BYTES, member, indexSizeBytes);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, itemCount);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_ARN, member, indexArn);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalSecondaryIndexDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.keySchema(this.keySchema);
        builder.projection(this.projection);
        builder.indexStatus(this.indexStatus);
        builder.backfilling(this.backfilling);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.indexSizeBytes(this.indexSizeBytes);
        builder.itemCount(this.itemCount);
        builder.indexArn(this.indexArn);
        builder.onDemandThroughput(this.onDemandThroughput);
        builder.warmThroughput(this.warmThroughput);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalSecondaryIndexDescription}.
     */
    public static final class Builder implements ShapeBuilder<GlobalSecondaryIndexDescription> {
        private String indexName;
        private List<KeySchemaElement> keySchema;
        private Projection projection;
        private IndexStatus indexStatus;
        private Boolean backfilling;
        private ProvisionedThroughputDescription provisionedThroughput;
        private Long indexSizeBytes;
        private Long itemCount;
        private String indexArn;
        private OnDemandThroughput onDemandThroughput;
        private GlobalSecondaryIndexWarmThroughputDescription warmThroughput;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global secondary index.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The complete key schema for a global secondary index, which consists of one or more pairs of attribute names and
         * key types:
         *
         * <ul>
         *   <li>
         *     <code>HASH</code> - partition key
         *   </li>
         *   <li>
         *     <code>RANGE</code> - sort key
         *   </li>
         * </ul>
         *
         * <p>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute" derives
         * from DynamoDB's usage of an internal hash function to evenly distribute data items across partitions, based on
         * their partition key values.
         *
         * <p>The sort key of an item is also known as its <i>range attribute</i>. The term "range attribute" derives from
         * the way DynamoDB stores items with the same partition key physically close together, in sorted order by the sort
         * key value.
         *
         * @return this builder.
         */
        public Builder keySchema(List<KeySchemaElement> keySchema) {
            this.keySchema = keySchema;
            return this;
        }

        /**
         * Represents attributes that are copied (projected) from the table into the global secondary index. These are in
         * addition to the primary key attributes and index key attributes, which are automatically projected.
         *
         * @return this builder.
         */
        public Builder projection(Projection projection) {
            this.projection = projection;
            return this;
        }

        /**
         * The current state of the global secondary index:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The index is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The index is being updated.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The index is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The index is ready for use.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder indexStatus(IndexStatus indexStatus) {
            this.indexStatus = indexStatus;
            return this;
        }

        /**
         * Indicates whether the index is currently backfilling. <i>Backfilling</i> is the process of reading items from the
         * table and determining whether they can be added to the index. (Not all items will qualify: For example, a
         * partition key cannot have any duplicate values.) If an item can be added to the index, DynamoDB will do so. After
         * all items have been processed, the backfilling operation is complete and <code>Backfilling</code> is false.
         *
         * <p>You can delete an index that is being created during the <code>Backfilling</code> phase when <code>IndexStatus</code>
         * is set to CREATING and <code>Backfilling</code> is true. You can't delete the index that is being created when <code>
         * IndexStatus</code> is set to CREATING and <code>Backfilling</code> is false.
         *
         * <p>For indexes that were created during a <code>CreateTable</code> operation, the <code>Backfilling</code>
         * attribute does not appear in the <code>DescribeTable</code> output.
         *
         * @return this builder.
         */
        public Builder backfilling(Boolean backfilling) {
            this.backfilling = backfilling;
            return this;
        }

        /**
         * Represents the provisioned throughput settings for the specified global secondary index.
         *
         * <p>For current minimum and maximum provisioned throughput values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a>
         * in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder provisionedThroughput(ProvisionedThroughputDescription provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * The total size of the specified index, in bytes. DynamoDB updates this value approximately every six hours.
         * Recent changes might not be reflected in this value.
         *
         * @return this builder.
         */
        public Builder indexSizeBytes(Long indexSizeBytes) {
            this.indexSizeBytes = indexSizeBytes;
            return this;
        }

        /**
         * The number of items in the specified index. DynamoDB updates this value approximately every six hours. Recent
         * changes might not be reflected in this value.
         *
         * @return this builder.
         */
        public Builder itemCount(Long itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) that uniquely identifies the index.
         *
         * @return this builder.
         */
        public Builder indexArn(String indexArn) {
            this.indexArn = indexArn;
            return this;
        }

        /**
         * The maximum number of read and write units for the specified global secondary index. If you use this parameter,
         * you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
         *
         * @return this builder.
         */
        public Builder onDemandThroughput(OnDemandThroughput onDemandThroughput) {
            this.onDemandThroughput = onDemandThroughput;
            return this;
        }

        /**
         * Represents the warm throughput value (in read units per second and write units per second) for the specified
         * secondary index.
         *
         * @return this builder.
         */
        public Builder warmThroughput(GlobalSecondaryIndexWarmThroughputDescription warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        @Override
        public GlobalSecondaryIndexDescription build() {
            return new GlobalSecondaryIndexDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 2 -> projection((Projection) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, value));
                case 3 -> indexStatus((IndexStatus) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, value));
                case 4 -> backfilling((Boolean) SchemaUtils.validateSameMember($SCHEMA_BACKFILLING, member, value));
                case 5 -> provisionedThroughput((ProvisionedThroughputDescription) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 6 -> indexSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_INDEX_SIZE_BYTES, member, value));
                case 7 -> itemCount((Long) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, value));
                case 8 -> indexArn((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_ARN, member, value));
                case 9 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 10 -> warmThroughput((GlobalSecondaryIndexWarmThroughputDescription) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.keySchema(SharedSerde.deserializeKeySchema(member, de));
                    case 2 -> builder.projection(Projection.builder().deserializeMember(de, member).build());
                    case 3 -> builder.indexStatus(IndexStatus.builder().deserializeMember(de, member).build());
                    case 4 -> builder.backfilling(de.readBoolean(member));
                    case 5 -> builder.provisionedThroughput(ProvisionedThroughputDescription.builder().deserializeMember(de, member).build());
                    case 6 -> builder.indexSizeBytes(de.readLong(member));
                    case 7 -> builder.itemCount(de.readLong(member));
                    case 8 -> builder.indexArn(de.readString(member));
                    case 9 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 10 -> builder.warmThroughput(GlobalSecondaryIndexWarmThroughputDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
