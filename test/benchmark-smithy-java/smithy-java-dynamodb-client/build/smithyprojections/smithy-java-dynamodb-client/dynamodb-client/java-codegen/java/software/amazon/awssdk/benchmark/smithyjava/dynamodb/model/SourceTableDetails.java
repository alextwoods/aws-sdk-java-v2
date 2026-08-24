package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * Contains the details of the table when the backup was created.
 */
@SmithyGenerated
public final class SourceTableDetails implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SOURCE_TABLE_DETAILS;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TABLE_ID = $SCHEMA.member("TableId");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_TABLE_SIZE_BYTES = $SCHEMA.member("TableSizeBytes");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_TABLE_CREATION_DATE_TIME = $SCHEMA.member("TableCreationDateTime");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_ITEM_COUNT = $SCHEMA.member("ItemCount");
    private static final Schema $SCHEMA_BILLING_MODE = $SCHEMA.member("BillingMode");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String tableId;
    private final transient String tableArn;
    private final transient Long tableSizeBytes;
    private final transient List<KeySchemaElement> keySchema;
    private final transient Instant tableCreationDateTime;
    private final transient ProvisionedThroughput provisionedThroughput;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient Long itemCount;
    private final transient BillingMode billingMode;

    private SourceTableDetails(Builder builder) {
        this.tableName = builder.tableName;
        this.tableId = builder.tableId;
        this.tableArn = builder.tableArn;
        this.tableSizeBytes = builder.tableSizeBytes;
        this.keySchema = Collections.unmodifiableList(builder.keySchema);
        this.tableCreationDateTime = builder.tableCreationDateTime;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.onDemandThroughput = builder.onDemandThroughput;
        this.itemCount = builder.itemCount;
        this.billingMode = builder.billingMode;
    }

    /**
     * The name of the table for which the backup was created.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Unique identifier for the table for which the backup was created.
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * ARN of the table for which backup was created.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * Size of the table in bytes. Note that this is an approximate value.
     */
    public Long getTableSizeBytes() {
        return tableSizeBytes;
    }

    /**
     * Schema of the table.
     */
    public List<KeySchemaElement> getKeySchema() {
        return keySchema;
    }

    public boolean hasKeySchema() {
        return true;
    }

    /**
     * Time when the source table was created.
     */
    public Instant getTableCreationDateTime() {
        return tableCreationDateTime;
    }

    /**
     * Read IOPs and Write IOPS on the table when the backup was created.
     */
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    /**
     * Number of items in the table. Note that this is an approximate value.
     */
    public Long getItemCount() {
        return itemCount;
    }

    /**
     * Controls how you are charged for read and write throughput and how you manage capacity. This setting can be
     * changed later.
     *
     * <ul>
     *   <li>
     *     <code>PROVISIONED</code> - Sets the read/write capacity mode to <code>PROVISIONED</code>. We recommend
     *     using <code>PROVISIONED</code> for predictable workloads.
     *   </li>
     *   <li>
     *     <code>PAY_PER_REQUEST</code> - Sets the read/write capacity mode to <code>PAY_PER_REQUEST</code>. We
     *     recommend using <code>PAY_PER_REQUEST</code> for unpredictable workloads.
     *   </li>
     * </ul>
     */
    public BillingMode getBillingMode() {
        return billingMode;
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
        SourceTableDetails that = (SourceTableDetails) other;
        return Objects.equals(this.tableSizeBytes, that.tableSizeBytes)
               && Objects.equals(this.itemCount, that.itemCount)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableId, that.tableId)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.billingMode, that.billingMode)
               && Objects.equals(this.tableCreationDateTime, that.tableCreationDateTime)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.keySchema, that.keySchema);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(tableId);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(tableSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(tableCreationDateTime);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
        $hc = 31 * $hc + Objects.hashCode(itemCount);
        $hc = 31 * $hc + Objects.hashCode(billingMode);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        serializer.writeString($SCHEMA_TABLE_ID, tableId);
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (tableSizeBytes != null) {
            serializer.writeLong($SCHEMA_TABLE_SIZE_BYTES, tableSizeBytes);
        }
        serializer.writeList($SCHEMA_KEY_SCHEMA, keySchema, keySchema.size(), SharedSerde.KeySchemaSerializer.INSTANCE);
        serializer.writeTimestamp($SCHEMA_TABLE_CREATION_DATE_TIME, tableCreationDateTime);
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
        }
        if (itemCount != null) {
            serializer.writeLong($SCHEMA_ITEM_COUNT, itemCount);
        }
        if (billingMode != null) {
            serializer.writeString($SCHEMA_BILLING_MODE, billingMode.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, tableId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, keySchema);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CREATION_DATE_TIME, member, tableCreationDateTime);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_SIZE_BYTES, member, tableSizeBytes);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, itemCount);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, billingMode);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SourceTableDetails}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.tableId(this.tableId);
        builder.tableArn(this.tableArn);
        builder.tableSizeBytes(this.tableSizeBytes);
        builder.keySchema(this.keySchema);
        builder.tableCreationDateTime(this.tableCreationDateTime);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.onDemandThroughput(this.onDemandThroughput);
        builder.itemCount(this.itemCount);
        builder.billingMode(this.billingMode);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SourceTableDetails}.
     */
    public static final class Builder implements ShapeBuilder<SourceTableDetails> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String tableId;
        private String tableArn;
        private Long tableSizeBytes;
        private List<KeySchemaElement> keySchema;
        private Instant tableCreationDateTime;
        private ProvisionedThroughput provisionedThroughput;
        private OnDemandThroughput onDemandThroughput;
        private Long itemCount;
        private BillingMode billingMode;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table for which the backup was created.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * Unique identifier for the table for which the backup was created.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableId(String tableId) {
            this.tableId = Objects.requireNonNull(tableId, "tableId cannot be null");
            tracker.setMember($SCHEMA_TABLE_ID);
            return this;
        }

        /**
         * ARN of the table for which backup was created.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * Size of the table in bytes. Note that this is an approximate value.
         *
         * @return this builder.
         */
        public Builder tableSizeBytes(Long tableSizeBytes) {
            this.tableSizeBytes = tableSizeBytes;
            return this;
        }

        /**
         * Schema of the table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder keySchema(List<KeySchemaElement> keySchema) {
            this.keySchema = Objects.requireNonNull(keySchema, "keySchema cannot be null");
            tracker.setMember($SCHEMA_KEY_SCHEMA);
            return this;
        }

        /**
         * Time when the source table was created.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableCreationDateTime(Instant tableCreationDateTime) {
            this.tableCreationDateTime = Objects.requireNonNull(tableCreationDateTime, "tableCreationDateTime cannot be null");
            tracker.setMember($SCHEMA_TABLE_CREATION_DATE_TIME);
            return this;
        }

        /**
         * Read IOPs and Write IOPS on the table when the backup was created.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = Objects.requireNonNull(provisionedThroughput, "provisionedThroughput cannot be null");
            tracker.setMember($SCHEMA_PROVISIONED_THROUGHPUT);
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder onDemandThroughput(OnDemandThroughput onDemandThroughput) {
            this.onDemandThroughput = onDemandThroughput;
            return this;
        }

        /**
         * Number of items in the table. Note that this is an approximate value.
         *
         * @return this builder.
         */
        public Builder itemCount(Long itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        /**
         * Controls how you are charged for read and write throughput and how you manage capacity. This setting can be
         * changed later.
         *
         * <ul>
         *   <li>
         *     <code>PROVISIONED</code> - Sets the read/write capacity mode to <code>PROVISIONED</code>. We recommend
         *     using <code>PROVISIONED</code> for predictable workloads.
         *   </li>
         *   <li>
         *     <code>PAY_PER_REQUEST</code> - Sets the read/write capacity mode to <code>PAY_PER_REQUEST</code>. We
         *     recommend using <code>PAY_PER_REQUEST</code> for unpredictable workloads.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder billingMode(BillingMode billingMode) {
            this.billingMode = billingMode;
            return this;
        }

        @Override
        public SourceTableDetails build() {
            tracker.validate();
            return new SourceTableDetails(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> tableId((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, value));
                case 2 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 3 -> tableCreationDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_TABLE_CREATION_DATE_TIME, member, value));
                case 4 -> provisionedThroughput((ProvisionedThroughput) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 5 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 6 -> tableSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_TABLE_SIZE_BYTES, member, value));
                case 7 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 8 -> itemCount((Long) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, value));
                case 9 -> billingMode((BillingMode) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SourceTableDetails> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_TABLE_ID)) {
                tableId("");
            }
            if (!tracker.checkMember($SCHEMA_KEY_SCHEMA)) {
                keySchema(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_TABLE_CREATION_DATE_TIME)) {
                tableCreationDateTime(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_PROVISIONED_THROUGHPUT)) {
                tracker.setMember($SCHEMA_PROVISIONED_THROUGHPUT);
            }
            return this;
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
                    case 1 -> builder.tableId(de.readString(member));
                    case 2 -> builder.keySchema(SharedSerde.deserializeKeySchema(member, de));
                    case 3 -> builder.tableCreationDateTime(de.readTimestamp(member));
                    case 4 -> builder.provisionedThroughput(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 5 -> builder.tableArn(de.readString(member));
                    case 6 -> builder.tableSizeBytes(de.readLong(member));
                    case 7 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 8 -> builder.itemCount(de.readLong(member));
                    case 9 -> builder.billingMode(BillingMode.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
