package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * Represents the properties of a table.
 */
@SmithyGenerated
public final class TableDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TABLE_DESCRIPTION;
    private static final Schema $SCHEMA_ATTRIBUTE_DEFINITIONS = $SCHEMA.member("AttributeDefinitions");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_TABLE_STATUS = $SCHEMA.member("TableStatus");
    private static final Schema $SCHEMA_CREATION_DATE_TIME = $SCHEMA.member("CreationDateTime");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_TABLE_SIZE_BYTES = $SCHEMA.member("TableSizeBytes");
    private static final Schema $SCHEMA_ITEM_COUNT = $SCHEMA.member("ItemCount");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_TABLE_ID = $SCHEMA.member("TableId");
    private static final Schema $SCHEMA_BILLING_MODE_SUMMARY = $SCHEMA.member("BillingModeSummary");
    private static final Schema $SCHEMA_LOCAL_SECONDARY_INDEXES = $SCHEMA.member("LocalSecondaryIndexes");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_STREAM_SPECIFICATION = $SCHEMA.member("StreamSpecification");
    private static final Schema $SCHEMA_LATEST_STREAM_LABEL = $SCHEMA.member("LatestStreamLabel");
    private static final Schema $SCHEMA_LATEST_STREAM_ARN = $SCHEMA.member("LatestStreamArn");
    private static final Schema $SCHEMA_GLOBAL_TABLE_VERSION = $SCHEMA.member("GlobalTableVersion");
    private static final Schema $SCHEMA_REPLICAS = $SCHEMA.member("Replicas");
    private static final Schema $SCHEMA_GLOBAL_TABLE_WITNESSES = $SCHEMA.member("GlobalTableWitnesses");
    private static final Schema $SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE = $SCHEMA.member("GlobalTableSettingsReplicationMode");
    private static final Schema $SCHEMA_RESTORE_SUMMARY = $SCHEMA.member("RestoreSummary");
    private static final Schema $SCHEMA_SSE_DESCRIPTION = $SCHEMA.member("SSEDescription");
    private static final Schema $SCHEMA_ARCHIVAL_SUMMARY = $SCHEMA.member("ArchivalSummary");
    private static final Schema $SCHEMA_TABLE_CLASS_SUMMARY = $SCHEMA.member("TableClassSummary");
    private static final Schema $SCHEMA_DELETION_PROTECTION_ENABLED = $SCHEMA.member("DeletionProtectionEnabled");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");
    private static final Schema $SCHEMA_MULTI_REGION_CONSISTENCY = $SCHEMA.member("MultiRegionConsistency");
    private static final Schema $SCHEMA_VECTOR_INDEXES = $SCHEMA.member("VectorIndexes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AttributeDefinition> attributeDefinitions;
    private final transient String tableName;
    private final transient List<KeySchemaElement> keySchema;
    private final transient TableStatus tableStatus;
    private final transient Instant creationDateTime;
    private final transient ProvisionedThroughputDescription provisionedThroughput;
    private final transient Long tableSizeBytes;
    private final transient Long itemCount;
    private final transient String tableArn;
    private final transient String tableId;
    private final transient BillingModeSummary billingModeSummary;
    private final transient List<LocalSecondaryIndexDescription> localSecondaryIndexes;
    private final transient List<GlobalSecondaryIndexDescription> globalSecondaryIndexes;
    private final transient StreamSpecification streamSpecification;
    private final transient String latestStreamLabel;
    private final transient String latestStreamArn;
    private final transient String globalTableVersion;
    private final transient List<ReplicaDescription> replicas;
    private final transient List<GlobalTableWitnessDescription> globalTableWitnesses;
    private final transient GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;
    private final transient RestoreSummary restoreSummary;
    private final transient SSEDescription sseDescription;
    private final transient ArchivalSummary archivalSummary;
    private final transient TableClassSummary tableClassSummary;
    private final transient Boolean deletionProtectionEnabled;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient TableWarmThroughputDescription warmThroughput;
    private final transient MultiRegionConsistency multiRegionConsistency;
    private final transient List<VectorIndexDescription> vectorIndexes;

    private TableDescription(Builder builder) {
        this.attributeDefinitions = builder.attributeDefinitions == null ? null : Collections.unmodifiableList(builder.attributeDefinitions);
        this.tableName = builder.tableName;
        this.keySchema = builder.keySchema == null ? null : Collections.unmodifiableList(builder.keySchema);
        this.tableStatus = builder.tableStatus;
        this.creationDateTime = builder.creationDateTime;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.tableSizeBytes = builder.tableSizeBytes;
        this.itemCount = builder.itemCount;
        this.tableArn = builder.tableArn;
        this.tableId = builder.tableId;
        this.billingModeSummary = builder.billingModeSummary;
        this.localSecondaryIndexes = builder.localSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.localSecondaryIndexes);
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.streamSpecification = builder.streamSpecification;
        this.latestStreamLabel = builder.latestStreamLabel;
        this.latestStreamArn = builder.latestStreamArn;
        this.globalTableVersion = builder.globalTableVersion;
        this.replicas = builder.replicas == null ? null : Collections.unmodifiableList(builder.replicas);
        this.globalTableWitnesses = builder.globalTableWitnesses == null ? null : Collections.unmodifiableList(builder.globalTableWitnesses);
        this.globalTableSettingsReplicationMode = builder.globalTableSettingsReplicationMode;
        this.restoreSummary = builder.restoreSummary;
        this.sseDescription = builder.sseDescription;
        this.archivalSummary = builder.archivalSummary;
        this.tableClassSummary = builder.tableClassSummary;
        this.deletionProtectionEnabled = builder.deletionProtectionEnabled;
        this.onDemandThroughput = builder.onDemandThroughput;
        this.warmThroughput = builder.warmThroughput;
        this.multiRegionConsistency = builder.multiRegionConsistency;
        this.vectorIndexes = builder.vectorIndexes == null ? null : Collections.unmodifiableList(builder.vectorIndexes);
    }

    /**
     * An array of <code>AttributeDefinition</code> objects. Each of these objects describes one attribute in the table
     * and index key schema.
     *
     * <p>Each <code>AttributeDefinition</code> object in this array is composed of:
     *
     * <ul>
     *   <li>
     *     <code>AttributeName</code> - The name of the attribute.
     *   </li>
     *   <li>
     *     <code>AttributeType</code> - The data type for the attribute.
     *   </li>
     * </ul>
     */
    public List<AttributeDefinition> getAttributeDefinitions() {
        if (attributeDefinitions == null) {
            return Collections.emptyList();
        }
        return attributeDefinitions;
    }

    public boolean hasAttributeDefinitions() {
        return attributeDefinitions != null;
    }

    /**
     * The name of the table.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The primary key structure for the table. Each <code>KeySchemaElement</code> consists of:
     *
     * <ul>
     *   <li>
     *     <code>AttributeName</code> - The name of the attribute.
     *   </li>
     *   <li>
     *     <code>KeyType</code> - The role of the attribute:
     *
     *     <ul>
     *       <li>
     *         <code>HASH</code> - partition key
     *       </li>
     *       <li>
     *         <code>RANGE</code> - sort key
     *       </li>
     *     </ul>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute"
     *     derives from DynamoDB's usage of an internal hash function to evenly distribute data items across
     *     partitions, based on their partition key values.The sort key of an item is also known as its <i>range
     *     attribute</i>. The term "range attribute" derives from the way DynamoDB stores items with the same
     *     partition key physically close together, in sorted order by the sort key value.
     *   </li>
     * </ul>
     *
     * <p>For more information about primary keys, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataModel.html#DataModelPrimaryKey">Primary Key</a> in the <i>Amazon DynamoDB Developer Guide</i>.
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
     * The current state of the table:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The table is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The table/index configuration is being updated. The table/index remains available
     *     for data operations when <code>UPDATING</code>.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The table is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The table is ready for use.
     *   </li>
     *   <li>
     *     <code>INACCESSIBLE_ENCRYPTION_CREDENTIALS</code> - The KMS key used to encrypt the table in inaccessible.
     *     Table operations may fail due to failure to use the KMS key. DynamoDB will initiate the table archival
     *     process when a table's KMS key remains inaccessible for more than seven days.
     *   </li>
     *   <li>
     *     <code>ARCHIVING</code> - The table is being archived. Operations are not allowed until archival is
     *     complete.
     *   </li>
     *   <li>
     *     <code>ARCHIVED</code> - The table has been archived. See the ArchivalReason for more information.
     *   </li>
     * </ul>
     */
    public TableStatus getTableStatus() {
        return tableStatus;
    }

    /**
     * The date and time when the table was created, in <a href="http://www.epochconverter.com/">UNIX epoch time</a>
     * format.
     */
    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    /**
     * The provisioned throughput settings for the table, consisting of read and write capacity units, along with data
     * about increases and decreases.
     */
    public ProvisionedThroughputDescription getProvisionedThroughput() {
        return provisionedThroughput;
    }

    /**
     * The total size of the specified table, in bytes. DynamoDB updates this value approximately every six hours.
     * Recent changes might not be reflected in this value.
     */
    public Long getTableSizeBytes() {
        return tableSizeBytes;
    }

    /**
     * The number of items in the specified table. DynamoDB updates this value approximately every six hours. Recent
     * changes might not be reflected in this value.
     */
    public Long getItemCount() {
        return itemCount;
    }

    /**
     * The Amazon Resource Name (ARN) that uniquely identifies the table.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * A unique identifier for the table, in UUID format, generated by DynamoDB when the table is created.
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Contains the details for the read/write capacity mode.
     */
    public BillingModeSummary getBillingModeSummary() {
        return billingModeSummary;
    }

    /**
     * Represents one or more local secondary indexes on the table. Each index is scoped to a given partition key value.
     * Tables with one or more local secondary indexes are subject to an item collection size limit, where the amount of
     * data within a given item collection cannot exceed 10 GB. Each element is composed of:
     *
     * <ul>
     *   <li>
     *     <code>IndexName</code> - The name of the local secondary index.
     *   </li>
     *   <li>
     *     <code>KeySchema</code> - Specifies the complete index key schema. The attribute names in the key schema
     *     must be between 1 and 255 characters (inclusive). The key schema must begin with the same partition key
     *     as the table.
     *   </li>
     *   <li>
     *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the index.
     *     These are in addition to the primary key attributes and index key attributes, which are automatically
     *     projected. Each attribute specification is composed of:
     *
     *     <ul>
     *       <li>
     *         <code>ProjectionType</code> - One of the following:
     *
     *         <ul>
     *           <li>
     *             <code>KEYS_ONLY</code> - Only the index and primary keys are projected into the index.
     *           </li>
     *           <li>
     *             <code>INCLUDE</code> - Only the specified table attributes are projected into the index.
     *             The list of projected attributes is in <code>NonKeyAttributes</code>.
     *           </li>
     *           <li>
     *             <code>ALL</code> - All of the table attributes are projected into the index.
     *           </li>
     *         </ul>
     *       </li>
     *       <li>
     *         <code>NonKeyAttributes</code> - A list of one or more non-key attribute names that are projected
     *         into the secondary index. The total count of attributes provided in <code>NonKeyAttributes</code>
     *         , summed across all of the secondary indexes, must not exceed 100. If you project the same
     *         attribute into two different indexes, this counts as two distinct attributes when determining the
     *         total. This limit only applies when you specify the ProjectionType of <code>INCLUDE</code>. You
     *         still can specify the ProjectionType of <code>ALL</code> to project all attributes from the
     *         source table, even if the table has more than 100 attributes.
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     <code>IndexSizeBytes</code> - Represents the total size of the index, in bytes. DynamoDB updates this
     *     value approximately every six hours. Recent changes might not be reflected in this value.
     *   </li>
     *   <li>
     *     <code>ItemCount</code> - Represents the number of items in the index. DynamoDB updates this value
     *     approximately every six hours. Recent changes might not be reflected in this value.
     *   </li>
     * </ul>
     *
     * <p>If the table is in the <code>DELETING</code> state, no information about indexes will be returned.
     */
    public List<LocalSecondaryIndexDescription> getLocalSecondaryIndexes() {
        if (localSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return localSecondaryIndexes;
    }

    public boolean hasLocalSecondaryIndexes() {
        return localSecondaryIndexes != null;
    }

    /**
     * The global secondary indexes, if any, on the table. Each index is scoped to a given partition key value. Each
     * element is composed of:
     *
     * <ul>
     *   <li>
     *     <code>Backfilling</code> - If true, then the index is currently in the backfilling phase. Backfilling
     *     occurs only when a new global secondary index is added to the table. It is the process by which DynamoDB
     *     populates the new index with data from the table. (This attribute does not appear for indexes that were
     *     created during a <code>CreateTable</code> operation.)  You can delete an index that is being created
     *     during the <code>Backfilling</code> phase when <code>IndexStatus</code> is set to CREATING and <code>
     *     Backfilling</code> is true. You can't delete the index that is being created when <code>IndexStatus</code>
     *     is set to CREATING and <code>Backfilling</code> is false. (This attribute does not appear for indexes
     *     that were created during a <code>CreateTable</code> operation.)
     *   </li>
     *   <li>
     *     <code>IndexName</code> - The name of the global secondary index.
     *   </li>
     *   <li>
     *     <code>IndexSizeBytes</code> - The total size of the global secondary index, in bytes. DynamoDB updates
     *     this value approximately every six hours. Recent changes might not be reflected in this value.
     *   </li>
     *   <li>
     *     <code>IndexStatus</code> - The current status of the global secondary index:
     *
     *     <ul>
     *       <li>
     *         <code>CREATING</code> - The index is being created.
     *       </li>
     *       <li>
     *         <code>UPDATING</code> - The index is being updated.
     *       </li>
     *       <li>
     *         <code>DELETING</code> - The index is being deleted.
     *       </li>
     *       <li>
     *         <code>ACTIVE</code> - The index is ready for use.
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     <code>ItemCount</code> - The number of items in the global secondary index. DynamoDB updates this value
     *     approximately every six hours. Recent changes might not be reflected in this value.
     *   </li>
     *   <li>
     *     <code>KeySchema</code> - Specifies the complete index key schema. The attribute names in the key schema
     *     must be between 1 and 255 characters (inclusive). The key schema must begin with the same partition key
     *     as the table.
     *   </li>
     *   <li>
     *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the index.
     *     These are in addition to the primary key attributes and index key attributes, which are automatically
     *     projected. Each attribute specification is composed of:
     *
     *     <ul>
     *       <li>
     *         <code>ProjectionType</code> - One of the following:
     *
     *         <ul>
     *           <li>
     *             <code>KEYS_ONLY</code> - Only the index and primary keys are projected into the index.
     *           </li>
     *           <li>
     *             <code>INCLUDE</code> - In addition to the attributes described in <code>KEYS_ONLY</code>,
     *             the secondary index will include other non-key attributes that you specify.
     *           </li>
     *           <li>
     *             <code>ALL</code> - All of the table attributes are projected into the index.
     *           </li>
     *         </ul>
     *       </li>
     *       <li>
     *         <code>NonKeyAttributes</code> - A list of one or more non-key attribute names that are projected
     *         into the secondary index. The total count of attributes provided in <code>NonKeyAttributes</code>
     *         , summed across all of the secondary indexes, must not exceed 100. If you project the same
     *         attribute into two different indexes, this counts as two distinct attributes when determining the
     *         total. This limit only applies when you specify the ProjectionType of <code>INCLUDE</code>. You
     *         still can specify the ProjectionType of <code>ALL</code> to project all attributes from the
     *         source table, even if the table has more than 100 attributes.
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     <code>ProvisionedThroughput</code> - The provisioned throughput settings for the global secondary index,
     *     consisting of read and write capacity units, along with data about increases and decreases.
     *   </li>
     * </ul>
     *
     * <p>If the table is in the <code>DELETING</code> state, no information about indexes will be returned.
     */
    public List<GlobalSecondaryIndexDescription> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    /**
     * The current DynamoDB Streams configuration for the table.
     */
    public StreamSpecification getStreamSpecification() {
        return streamSpecification;
    }

    /**
     * A timestamp, in ISO 8601 format, for this stream.
     *
     * <p>Note that <code>LatestStreamLabel</code> is not a unique identifier for the stream, because it is possible
     * that a stream from another table might have the same timestamp. However, the combination of the following three
     * elements is guaranteed to be unique:
     *
     * <ul>
     *   <li>
     *     Amazon Web Services customer ID
     *   </li>
     *   <li>
     *     Table name
     *   </li>
     *   <li>
     *     <code>StreamLabel</code>
     *   </li>
     * </ul>
     */
    public String getLatestStreamLabel() {
        return latestStreamLabel;
    }

    /**
     * The Amazon Resource Name (ARN) that uniquely identifies the latest stream for this table.
     */
    public String getLatestStreamArn() {
        return latestStreamArn;
    }

    /**
     * Represents the version of <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">global tables</a> in
     * use, if the table is replicated across Amazon Web Services Regions.
     */
    public String getGlobalTableVersion() {
        return globalTableVersion;
    }

    /**
     * Represents replicas of the table.
     */
    public List<ReplicaDescription> getReplicas() {
        if (replicas == null) {
            return Collections.emptyList();
        }
        return replicas;
    }

    public boolean hasReplicas() {
        return replicas != null;
    }

    /**
     * The witness Region and its current status in the MRSC global table. Only one witness Region can be configured per
     * MRSC global table.
     */
    public List<GlobalTableWitnessDescription> getGlobalTableWitnesses() {
        if (globalTableWitnesses == null) {
            return Collections.emptyList();
        }
        return globalTableWitnesses;
    }

    public boolean hasGlobalTableWitnesses() {
        return globalTableWitnesses != null;
    }

    /**
     * Indicates one of the settings synchronization modes for the global table:
     *
     * <ul>
     *   <li>
     *     <code>ENABLED</code>: Indicates that the settings synchronization mode for the global table is enabled.
     *   </li>
     *   <li>
     *     <code>DISABLED</code>: Indicates that the settings synchronization mode for the global table is disabled.
     *   </li>
     *   <li>
     *     <code>ENABLED_WITH_OVERRIDES</code>: This mode is set by default for a same account global table.
     *     Indicates that certain global table settings can be overridden.
     *   </li>
     * </ul>
     */
    public GlobalTableSettingsReplicationMode getGlobalTableSettingsReplicationMode() {
        return globalTableSettingsReplicationMode;
    }

    /**
     * Contains details for the restore.
     */
    public RestoreSummary getRestoreSummary() {
        return restoreSummary;
    }

    /**
     * The description of the server-side encryption status on the specified table.
     */
    public SSEDescription getSseDescription() {
        return sseDescription;
    }

    /**
     * Contains information about the table archive.
     */
    public ArchivalSummary getArchivalSummary() {
        return archivalSummary;
    }

    /**
     * Contains details of the table class.
     */
    public TableClassSummary getTableClassSummary() {
        return tableClassSummary;
    }

    /**
     * Indicates whether deletion protection is enabled (true) or disabled (false) on the table.
     */
    public Boolean isDeletionProtectionEnabled() {
        return deletionProtectionEnabled;
    }

    /**
     * The maximum number of read and write units for the specified on-demand table. If you use this parameter, you must
     * specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
     */
    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    /**
     * Describes the warm throughput value of the base table.
     */
    public TableWarmThroughputDescription getWarmThroughput() {
        return warmThroughput;
    }

    /**
     * Indicates one of the following consistency modes for a global table:
     *
     * <ul>
     *   <li>
     *     <code>EVENTUAL</code>: Indicates that the global table is configured for multi-Region eventual
     *     consistency (MREC).
     *   </li>
     *   <li>
     *     <code>STRONG</code>: Indicates that the global table is configured for multi-Region strong consistency
     *     (MRSC).
     *   </li>
     * </ul>
     *
     * <p>If you don't specify this field, the global table consistency mode defaults to <code>EVENTUAL</code>. For more
     * information about global tables consistency modes, see <a href="https://docs.aws.amazon.com/V2globaltables_HowItWorks.html#V2globaltables_HowItWorks.consistency-modes"> Consistency modes</a> in DynamoDB developer guide.
     */
    public MultiRegionConsistency getMultiRegionConsistency() {
        return multiRegionConsistency;
    }

    /**
     * The vector indexes, if any, on the table. Each element is composed of:
     *
     * <ul>
     *   <li>
     *     <code>IndexName</code> - The name of the vector index.
     *   </li>
     *   <li>
     *     <code>IndexStatus</code> - The current status of the vector index: <code>CREATING</code>, <code>ACTIVE</code>
     *     , or <code>DELETING</code>.
     *   </li>
     *   <li>
     *     <code>Backfilling</code> - Specifies whether the index is currently backfilling. During backfill, <code>
     *     SearchVectors</code> operations might return incomplete results.
     *   </li>
     *   <li>
     *     <code>VectorAttribute</code> - The attribute that contains vector embeddings.
     *   </li>
     *   <li>
     *     <code>Dimensions</code> - The number of dimensions in each vector.
     *   </li>
     *   <li>
     *     <code>DistanceFunction</code> - The distance function used to calculate similarity (<code>COSINE</code>, <code>
     *     EUCLIDEAN</code>, or <code>DOT_PRODUCT</code>).
     *   </li>
     *   <li>
     *     <code>SearchSchema</code> - The partition key and inline filter attributes for the vector index.
     *   </li>
     *   <li>
     *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the vector
     *     index.
     *   </li>
     *   <li>
     *     <code>IndexArn</code> - The Amazon Resource Name (ARN) that uniquely identifies the index.
     *   </li>
     *   <li>
     *     <code>IndexSizeBytes</code> - The total size of the vector index, in bytes. Amazon DynamoDB updates this
     *     value approximately every six hours. Recent changes might not be reflected in this value.
     *   </li>
     *   <li>
     *     <code>ItemCount</code> - The number of items indexed in the vector index. Amazon DynamoDB updates this
     *     value approximately every six hours. Recent changes might not be reflected in this value.
     *   </li>
     * </ul>
     */
    public List<VectorIndexDescription> getVectorIndexes() {
        if (vectorIndexes == null) {
            return Collections.emptyList();
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
        TableDescription that = (TableDescription) other;
        return Objects.equals(this.deletionProtectionEnabled, that.deletionProtectionEnabled)
               && Objects.equals(this.tableSizeBytes, that.tableSizeBytes)
               && Objects.equals(this.itemCount, that.itemCount)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.tableId, that.tableId)
               && Objects.equals(this.latestStreamLabel, that.latestStreamLabel)
               && Objects.equals(this.latestStreamArn, that.latestStreamArn)
               && Objects.equals(this.globalTableVersion, that.globalTableVersion)
               && Objects.equals(this.tableStatus, that.tableStatus)
               && Objects.equals(this.globalTableSettingsReplicationMode, that.globalTableSettingsReplicationMode)
               && Objects.equals(this.multiRegionConsistency, that.multiRegionConsistency)
               && Objects.equals(this.creationDateTime, that.creationDateTime)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.billingModeSummary, that.billingModeSummary)
               && Objects.equals(this.streamSpecification, that.streamSpecification)
               && Objects.equals(this.restoreSummary, that.restoreSummary)
               && Objects.equals(this.sseDescription, that.sseDescription)
               && Objects.equals(this.archivalSummary, that.archivalSummary)
               && Objects.equals(this.tableClassSummary, that.tableClassSummary)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.warmThroughput, that.warmThroughput)
               && Objects.equals(this.attributeDefinitions, that.attributeDefinitions)
               && Objects.equals(this.keySchema, that.keySchema)
               && Objects.equals(this.localSecondaryIndexes, that.localSecondaryIndexes)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes)
               && Objects.equals(this.replicas, that.replicas)
               && Objects.equals(this.globalTableWitnesses, that.globalTableWitnesses)
               && Objects.equals(this.vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeDefinitions);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(tableStatus);
        $hc = 31 * $hc + Objects.hashCode(creationDateTime);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(tableSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(itemCount);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(tableId);
        $hc = 31 * $hc + Objects.hashCode(billingModeSummary);
        $hc = 31 * $hc + Objects.hashCode(localSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(streamSpecification);
        $hc = 31 * $hc + Objects.hashCode(latestStreamLabel);
        $hc = 31 * $hc + Objects.hashCode(latestStreamArn);
        $hc = 31 * $hc + Objects.hashCode(globalTableVersion);
        $hc = 31 * $hc + Objects.hashCode(replicas);
        $hc = 31 * $hc + Objects.hashCode(globalTableWitnesses);
        $hc = 31 * $hc + Objects.hashCode(globalTableSettingsReplicationMode);
        $hc = 31 * $hc + Objects.hashCode(restoreSummary);
        $hc = 31 * $hc + Objects.hashCode(sseDescription);
        $hc = 31 * $hc + Objects.hashCode(archivalSummary);
        $hc = 31 * $hc + Objects.hashCode(tableClassSummary);
        $hc = 31 * $hc + Objects.hashCode(deletionProtectionEnabled);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
        $hc = 31 * $hc + Objects.hashCode(warmThroughput);
        $hc = 31 * $hc + Objects.hashCode(multiRegionConsistency);
        $hc = 31 * $hc + Objects.hashCode(vectorIndexes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (attributeDefinitions != null) {
            serializer.writeList($SCHEMA_ATTRIBUTE_DEFINITIONS, attributeDefinitions, attributeDefinitions.size(), SharedSerde.AttributeDefinitionsSerializer.INSTANCE);
        }
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (keySchema != null) {
            serializer.writeList($SCHEMA_KEY_SCHEMA, keySchema, keySchema.size(), SharedSerde.KeySchemaSerializer.INSTANCE);
        }
        if (tableStatus != null) {
            serializer.writeString($SCHEMA_TABLE_STATUS, tableStatus.getValue());
        }
        if (creationDateTime != null) {
            serializer.writeTimestamp($SCHEMA_CREATION_DATE_TIME, creationDateTime);
        }
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (tableSizeBytes != null) {
            serializer.writeLong($SCHEMA_TABLE_SIZE_BYTES, tableSizeBytes);
        }
        if (itemCount != null) {
            serializer.writeLong($SCHEMA_ITEM_COUNT, itemCount);
        }
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (tableId != null) {
            serializer.writeString($SCHEMA_TABLE_ID, tableId);
        }
        if (billingModeSummary != null) {
            serializer.writeStruct($SCHEMA_BILLING_MODE_SUMMARY, billingModeSummary);
        }
        if (localSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_LOCAL_SECONDARY_INDEXES, localSecondaryIndexes, localSecondaryIndexes.size(), SharedSerde.LocalSecondaryIndexDescriptionListSerializer.INSTANCE);
        }
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.GlobalSecondaryIndexDescriptionListSerializer.INSTANCE);
        }
        if (streamSpecification != null) {
            serializer.writeStruct($SCHEMA_STREAM_SPECIFICATION, streamSpecification);
        }
        if (latestStreamLabel != null) {
            serializer.writeString($SCHEMA_LATEST_STREAM_LABEL, latestStreamLabel);
        }
        if (latestStreamArn != null) {
            serializer.writeString($SCHEMA_LATEST_STREAM_ARN, latestStreamArn);
        }
        if (globalTableVersion != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_VERSION, globalTableVersion);
        }
        if (replicas != null) {
            serializer.writeList($SCHEMA_REPLICAS, replicas, replicas.size(), SharedSerde.ReplicaDescriptionListSerializer.INSTANCE);
        }
        if (globalTableWitnesses != null) {
            serializer.writeList($SCHEMA_GLOBAL_TABLE_WITNESSES, globalTableWitnesses, globalTableWitnesses.size(), SharedSerde.GlobalTableWitnessDescriptionListSerializer.INSTANCE);
        }
        if (globalTableSettingsReplicationMode != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, globalTableSettingsReplicationMode.getValue());
        }
        if (restoreSummary != null) {
            serializer.writeStruct($SCHEMA_RESTORE_SUMMARY, restoreSummary);
        }
        if (sseDescription != null) {
            serializer.writeStruct($SCHEMA_SSE_DESCRIPTION, sseDescription);
        }
        if (archivalSummary != null) {
            serializer.writeStruct($SCHEMA_ARCHIVAL_SUMMARY, archivalSummary);
        }
        if (tableClassSummary != null) {
            serializer.writeStruct($SCHEMA_TABLE_CLASS_SUMMARY, tableClassSummary);
        }
        if (deletionProtectionEnabled != null) {
            serializer.writeBoolean($SCHEMA_DELETION_PROTECTION_ENABLED, deletionProtectionEnabled);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
        }
        if (warmThroughput != null) {
            serializer.writeStruct($SCHEMA_WARM_THROUGHPUT, warmThroughput);
        }
        if (multiRegionConsistency != null) {
            serializer.writeString($SCHEMA_MULTI_REGION_CONSISTENCY, multiRegionConsistency.getValue());
        }
        if (vectorIndexes != null) {
            serializer.writeList($SCHEMA_VECTOR_INDEXES, vectorIndexes, vectorIndexes.size(), SharedSerde.VectorIndexDescriptionListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, attributeDefinitions);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, keySchema);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, tableStatus);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE_TIME, member, creationDateTime);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_SIZE_BYTES, member, tableSizeBytes);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, itemCount);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, tableId);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE_SUMMARY, member, billingModeSummary);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, localSecondaryIndexes);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_SPECIFICATION, member, streamSpecification);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_LATEST_STREAM_LABEL, member, latestStreamLabel);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_LATEST_STREAM_ARN, member, latestStreamArn);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_VERSION, member, globalTableVersion);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICAS, member, replicas);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_WITNESSES, member, globalTableWitnesses);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, globalTableSettingsReplicationMode);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_SUMMARY, member, restoreSummary);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_DESCRIPTION, member, sseDescription);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_SUMMARY, member, archivalSummary);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS_SUMMARY, member, tableClassSummary);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETION_PROTECTION_ENABLED, member, deletionProtectionEnabled);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_MULTI_REGION_CONSISTENCY, member, multiRegionConsistency);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, vectorIndexes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TableDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeDefinitions(this.attributeDefinitions);
        builder.tableName(this.tableName);
        builder.keySchema(this.keySchema);
        builder.tableStatus(this.tableStatus);
        builder.creationDateTime(this.creationDateTime);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.tableSizeBytes(this.tableSizeBytes);
        builder.itemCount(this.itemCount);
        builder.tableArn(this.tableArn);
        builder.tableId(this.tableId);
        builder.billingModeSummary(this.billingModeSummary);
        builder.localSecondaryIndexes(this.localSecondaryIndexes);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.streamSpecification(this.streamSpecification);
        builder.latestStreamLabel(this.latestStreamLabel);
        builder.latestStreamArn(this.latestStreamArn);
        builder.globalTableVersion(this.globalTableVersion);
        builder.replicas(this.replicas);
        builder.globalTableWitnesses(this.globalTableWitnesses);
        builder.globalTableSettingsReplicationMode(this.globalTableSettingsReplicationMode);
        builder.restoreSummary(this.restoreSummary);
        builder.sseDescription(this.sseDescription);
        builder.archivalSummary(this.archivalSummary);
        builder.tableClassSummary(this.tableClassSummary);
        builder.deletionProtectionEnabled(this.deletionProtectionEnabled);
        builder.onDemandThroughput(this.onDemandThroughput);
        builder.warmThroughput(this.warmThroughput);
        builder.multiRegionConsistency(this.multiRegionConsistency);
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
     * Builder for {@link TableDescription}.
     */
    public static final class Builder implements ShapeBuilder<TableDescription> {
        private List<AttributeDefinition> attributeDefinitions;
        private String tableName;
        private List<KeySchemaElement> keySchema;
        private TableStatus tableStatus;
        private Instant creationDateTime;
        private ProvisionedThroughputDescription provisionedThroughput;
        private Long tableSizeBytes;
        private Long itemCount;
        private String tableArn;
        private String tableId;
        private BillingModeSummary billingModeSummary;
        private List<LocalSecondaryIndexDescription> localSecondaryIndexes;
        private List<GlobalSecondaryIndexDescription> globalSecondaryIndexes;
        private StreamSpecification streamSpecification;
        private String latestStreamLabel;
        private String latestStreamArn;
        private String globalTableVersion;
        private List<ReplicaDescription> replicas;
        private List<GlobalTableWitnessDescription> globalTableWitnesses;
        private GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;
        private RestoreSummary restoreSummary;
        private SSEDescription sseDescription;
        private ArchivalSummary archivalSummary;
        private TableClassSummary tableClassSummary;
        private Boolean deletionProtectionEnabled;
        private OnDemandThroughput onDemandThroughput;
        private TableWarmThroughputDescription warmThroughput;
        private MultiRegionConsistency multiRegionConsistency;
        private List<VectorIndexDescription> vectorIndexes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of <code>AttributeDefinition</code> objects. Each of these objects describes one attribute in the table
         * and index key schema.
         *
         * <p>Each <code>AttributeDefinition</code> object in this array is composed of:
         *
         * <ul>
         *   <li>
         *     <code>AttributeName</code> - The name of the attribute.
         *   </li>
         *   <li>
         *     <code>AttributeType</code> - The data type for the attribute.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder attributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
            this.attributeDefinitions = attributeDefinitions;
            return this;
        }

        /**
         * The name of the table.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The primary key structure for the table. Each <code>KeySchemaElement</code> consists of:
         *
         * <ul>
         *   <li>
         *     <code>AttributeName</code> - The name of the attribute.
         *   </li>
         *   <li>
         *     <code>KeyType</code> - The role of the attribute:
         *
         *     <ul>
         *       <li>
         *         <code>HASH</code> - partition key
         *       </li>
         *       <li>
         *         <code>RANGE</code> - sort key
         *       </li>
         *     </ul>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute"
         *     derives from DynamoDB's usage of an internal hash function to evenly distribute data items across
         *     partitions, based on their partition key values.The sort key of an item is also known as its <i>range
         *     attribute</i>. The term "range attribute" derives from the way DynamoDB stores items with the same
         *     partition key physically close together, in sorted order by the sort key value.
         *   </li>
         * </ul>
         *
         * <p>For more information about primary keys, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataModel.html#DataModelPrimaryKey">Primary Key</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder keySchema(List<KeySchemaElement> keySchema) {
            this.keySchema = keySchema;
            return this;
        }

        /**
         * The current state of the table:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The table is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The table/index configuration is being updated. The table/index remains available
         *     for data operations when <code>UPDATING</code>.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The table is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The table is ready for use.
         *   </li>
         *   <li>
         *     <code>INACCESSIBLE_ENCRYPTION_CREDENTIALS</code> - The KMS key used to encrypt the table in inaccessible.
         *     Table operations may fail due to failure to use the KMS key. DynamoDB will initiate the table archival
         *     process when a table's KMS key remains inaccessible for more than seven days.
         *   </li>
         *   <li>
         *     <code>ARCHIVING</code> - The table is being archived. Operations are not allowed until archival is
         *     complete.
         *   </li>
         *   <li>
         *     <code>ARCHIVED</code> - The table has been archived. See the ArchivalReason for more information.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder tableStatus(TableStatus tableStatus) {
            this.tableStatus = tableStatus;
            return this;
        }

        /**
         * The date and time when the table was created, in <a href="http://www.epochconverter.com/">UNIX epoch time</a>
         * format.
         *
         * @return this builder.
         */
        public Builder creationDateTime(Instant creationDateTime) {
            this.creationDateTime = creationDateTime;
            return this;
        }

        /**
         * The provisioned throughput settings for the table, consisting of read and write capacity units, along with data
         * about increases and decreases.
         *
         * @return this builder.
         */
        public Builder provisionedThroughput(ProvisionedThroughputDescription provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * The total size of the specified table, in bytes. DynamoDB updates this value approximately every six hours.
         * Recent changes might not be reflected in this value.
         *
         * @return this builder.
         */
        public Builder tableSizeBytes(Long tableSizeBytes) {
            this.tableSizeBytes = tableSizeBytes;
            return this;
        }

        /**
         * The number of items in the specified table. DynamoDB updates this value approximately every six hours. Recent
         * changes might not be reflected in this value.
         *
         * @return this builder.
         */
        public Builder itemCount(Long itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) that uniquely identifies the table.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * A unique identifier for the table, in UUID format, generated by DynamoDB when the table is created.
         *
         * @return this builder.
         */
        public Builder tableId(String tableId) {
            this.tableId = tableId;
            return this;
        }

        /**
         * Contains the details for the read/write capacity mode.
         *
         * @return this builder.
         */
        public Builder billingModeSummary(BillingModeSummary billingModeSummary) {
            this.billingModeSummary = billingModeSummary;
            return this;
        }

        /**
         * Represents one or more local secondary indexes on the table. Each index is scoped to a given partition key value.
         * Tables with one or more local secondary indexes are subject to an item collection size limit, where the amount of
         * data within a given item collection cannot exceed 10 GB. Each element is composed of:
         *
         * <ul>
         *   <li>
         *     <code>IndexName</code> - The name of the local secondary index.
         *   </li>
         *   <li>
         *     <code>KeySchema</code> - Specifies the complete index key schema. The attribute names in the key schema
         *     must be between 1 and 255 characters (inclusive). The key schema must begin with the same partition key
         *     as the table.
         *   </li>
         *   <li>
         *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the index.
         *     These are in addition to the primary key attributes and index key attributes, which are automatically
         *     projected. Each attribute specification is composed of:
         *
         *     <ul>
         *       <li>
         *         <code>ProjectionType</code> - One of the following:
         *
         *         <ul>
         *           <li>
         *             <code>KEYS_ONLY</code> - Only the index and primary keys are projected into the index.
         *           </li>
         *           <li>
         *             <code>INCLUDE</code> - Only the specified table attributes are projected into the index.
         *             The list of projected attributes is in <code>NonKeyAttributes</code>.
         *           </li>
         *           <li>
         *             <code>ALL</code> - All of the table attributes are projected into the index.
         *           </li>
         *         </ul>
         *       </li>
         *       <li>
         *         <code>NonKeyAttributes</code> - A list of one or more non-key attribute names that are projected
         *         into the secondary index. The total count of attributes provided in <code>NonKeyAttributes</code>
         *         , summed across all of the secondary indexes, must not exceed 100. If you project the same
         *         attribute into two different indexes, this counts as two distinct attributes when determining the
         *         total. This limit only applies when you specify the ProjectionType of <code>INCLUDE</code>. You
         *         still can specify the ProjectionType of <code>ALL</code> to project all attributes from the
         *         source table, even if the table has more than 100 attributes.
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *     <code>IndexSizeBytes</code> - Represents the total size of the index, in bytes. DynamoDB updates this
         *     value approximately every six hours. Recent changes might not be reflected in this value.
         *   </li>
         *   <li>
         *     <code>ItemCount</code> - Represents the number of items in the index. DynamoDB updates this value
         *     approximately every six hours. Recent changes might not be reflected in this value.
         *   </li>
         * </ul>
         *
         * <p>If the table is in the <code>DELETING</code> state, no information about indexes will be returned.
         *
         * @return this builder.
         */
        public Builder localSecondaryIndexes(List<LocalSecondaryIndexDescription> localSecondaryIndexes) {
            this.localSecondaryIndexes = localSecondaryIndexes;
            return this;
        }

        /**
         * The global secondary indexes, if any, on the table. Each index is scoped to a given partition key value. Each
         * element is composed of:
         *
         * <ul>
         *   <li>
         *     <code>Backfilling</code> - If true, then the index is currently in the backfilling phase. Backfilling
         *     occurs only when a new global secondary index is added to the table. It is the process by which DynamoDB
         *     populates the new index with data from the table. (This attribute does not appear for indexes that were
         *     created during a <code>CreateTable</code> operation.)  You can delete an index that is being created
         *     during the <code>Backfilling</code> phase when <code>IndexStatus</code> is set to CREATING and <code>
         *     Backfilling</code> is true. You can't delete the index that is being created when <code>IndexStatus</code>
         *     is set to CREATING and <code>Backfilling</code> is false. (This attribute does not appear for indexes
         *     that were created during a <code>CreateTable</code> operation.)
         *   </li>
         *   <li>
         *     <code>IndexName</code> - The name of the global secondary index.
         *   </li>
         *   <li>
         *     <code>IndexSizeBytes</code> - The total size of the global secondary index, in bytes. DynamoDB updates
         *     this value approximately every six hours. Recent changes might not be reflected in this value.
         *   </li>
         *   <li>
         *     <code>IndexStatus</code> - The current status of the global secondary index:
         *
         *     <ul>
         *       <li>
         *         <code>CREATING</code> - The index is being created.
         *       </li>
         *       <li>
         *         <code>UPDATING</code> - The index is being updated.
         *       </li>
         *       <li>
         *         <code>DELETING</code> - The index is being deleted.
         *       </li>
         *       <li>
         *         <code>ACTIVE</code> - The index is ready for use.
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *     <code>ItemCount</code> - The number of items in the global secondary index. DynamoDB updates this value
         *     approximately every six hours. Recent changes might not be reflected in this value.
         *   </li>
         *   <li>
         *     <code>KeySchema</code> - Specifies the complete index key schema. The attribute names in the key schema
         *     must be between 1 and 255 characters (inclusive). The key schema must begin with the same partition key
         *     as the table.
         *   </li>
         *   <li>
         *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the index.
         *     These are in addition to the primary key attributes and index key attributes, which are automatically
         *     projected. Each attribute specification is composed of:
         *
         *     <ul>
         *       <li>
         *         <code>ProjectionType</code> - One of the following:
         *
         *         <ul>
         *           <li>
         *             <code>KEYS_ONLY</code> - Only the index and primary keys are projected into the index.
         *           </li>
         *           <li>
         *             <code>INCLUDE</code> - In addition to the attributes described in <code>KEYS_ONLY</code>,
         *             the secondary index will include other non-key attributes that you specify.
         *           </li>
         *           <li>
         *             <code>ALL</code> - All of the table attributes are projected into the index.
         *           </li>
         *         </ul>
         *       </li>
         *       <li>
         *         <code>NonKeyAttributes</code> - A list of one or more non-key attribute names that are projected
         *         into the secondary index. The total count of attributes provided in <code>NonKeyAttributes</code>
         *         , summed across all of the secondary indexes, must not exceed 100. If you project the same
         *         attribute into two different indexes, this counts as two distinct attributes when determining the
         *         total. This limit only applies when you specify the ProjectionType of <code>INCLUDE</code>. You
         *         still can specify the ProjectionType of <code>ALL</code> to project all attributes from the
         *         source table, even if the table has more than 100 attributes.
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *     <code>ProvisionedThroughput</code> - The provisioned throughput settings for the global secondary index,
         *     consisting of read and write capacity units, along with data about increases and decreases.
         *   </li>
         * </ul>
         *
         * <p>If the table is in the <code>DELETING</code> state, no information about indexes will be returned.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<GlobalSecondaryIndexDescription> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * The current DynamoDB Streams configuration for the table.
         *
         * @return this builder.
         */
        public Builder streamSpecification(StreamSpecification streamSpecification) {
            this.streamSpecification = streamSpecification;
            return this;
        }

        /**
         * A timestamp, in ISO 8601 format, for this stream.
         *
         * <p>Note that <code>LatestStreamLabel</code> is not a unique identifier for the stream, because it is possible
         * that a stream from another table might have the same timestamp. However, the combination of the following three
         * elements is guaranteed to be unique:
         *
         * <ul>
         *   <li>
         *     Amazon Web Services customer ID
         *   </li>
         *   <li>
         *     Table name
         *   </li>
         *   <li>
         *     <code>StreamLabel</code>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder latestStreamLabel(String latestStreamLabel) {
            this.latestStreamLabel = latestStreamLabel;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) that uniquely identifies the latest stream for this table.
         *
         * @return this builder.
         */
        public Builder latestStreamArn(String latestStreamArn) {
            this.latestStreamArn = latestStreamArn;
            return this;
        }

        /**
         * Represents the version of <a
         * href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">global tables</a> in
         * use, if the table is replicated across Amazon Web Services Regions.
         *
         * @return this builder.
         */
        public Builder globalTableVersion(String globalTableVersion) {
            this.globalTableVersion = globalTableVersion;
            return this;
        }

        /**
         * Represents replicas of the table.
         *
         * @return this builder.
         */
        public Builder replicas(List<ReplicaDescription> replicas) {
            this.replicas = replicas;
            return this;
        }

        /**
         * The witness Region and its current status in the MRSC global table. Only one witness Region can be configured per
         * MRSC global table.
         *
         * @return this builder.
         */
        public Builder globalTableWitnesses(List<GlobalTableWitnessDescription> globalTableWitnesses) {
            this.globalTableWitnesses = globalTableWitnesses;
            return this;
        }

        /**
         * Indicates one of the settings synchronization modes for the global table:
         *
         * <ul>
         *   <li>
         *     <code>ENABLED</code>: Indicates that the settings synchronization mode for the global table is enabled.
         *   </li>
         *   <li>
         *     <code>DISABLED</code>: Indicates that the settings synchronization mode for the global table is disabled.
         *   </li>
         *   <li>
         *     <code>ENABLED_WITH_OVERRIDES</code>: This mode is set by default for a same account global table.
         *     Indicates that certain global table settings can be overridden.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode) {
            this.globalTableSettingsReplicationMode = globalTableSettingsReplicationMode;
            return this;
        }

        /**
         * Contains details for the restore.
         *
         * @return this builder.
         */
        public Builder restoreSummary(RestoreSummary restoreSummary) {
            this.restoreSummary = restoreSummary;
            return this;
        }

        /**
         * The description of the server-side encryption status on the specified table.
         *
         * @return this builder.
         */
        public Builder sseDescription(SSEDescription sseDescription) {
            this.sseDescription = sseDescription;
            return this;
        }

        /**
         * Contains information about the table archive.
         *
         * @return this builder.
         */
        public Builder archivalSummary(ArchivalSummary archivalSummary) {
            this.archivalSummary = archivalSummary;
            return this;
        }

        /**
         * Contains details of the table class.
         *
         * @return this builder.
         */
        public Builder tableClassSummary(TableClassSummary tableClassSummary) {
            this.tableClassSummary = tableClassSummary;
            return this;
        }

        /**
         * Indicates whether deletion protection is enabled (true) or disabled (false) on the table.
         *
         * @return this builder.
         */
        public Builder deletionProtectionEnabled(Boolean deletionProtectionEnabled) {
            this.deletionProtectionEnabled = deletionProtectionEnabled;
            return this;
        }

        /**
         * The maximum number of read and write units for the specified on-demand table. If you use this parameter, you must
         * specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
         *
         * @return this builder.
         */
        public Builder onDemandThroughput(OnDemandThroughput onDemandThroughput) {
            this.onDemandThroughput = onDemandThroughput;
            return this;
        }

        /**
         * Describes the warm throughput value of the base table.
         *
         * @return this builder.
         */
        public Builder warmThroughput(TableWarmThroughputDescription warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        /**
         * Indicates one of the following consistency modes for a global table:
         *
         * <ul>
         *   <li>
         *     <code>EVENTUAL</code>: Indicates that the global table is configured for multi-Region eventual
         *     consistency (MREC).
         *   </li>
         *   <li>
         *     <code>STRONG</code>: Indicates that the global table is configured for multi-Region strong consistency
         *     (MRSC).
         *   </li>
         * </ul>
         *
         * <p>If you don't specify this field, the global table consistency mode defaults to <code>EVENTUAL</code>. For more
         * information about global tables consistency modes, see <a href="https://docs.aws.amazon.com/V2globaltables_HowItWorks.html#V2globaltables_HowItWorks.consistency-modes"> Consistency modes</a> in DynamoDB developer guide.
         *
         * @return this builder.
         */
        public Builder multiRegionConsistency(MultiRegionConsistency multiRegionConsistency) {
            this.multiRegionConsistency = multiRegionConsistency;
            return this;
        }

        /**
         * The vector indexes, if any, on the table. Each element is composed of:
         *
         * <ul>
         *   <li>
         *     <code>IndexName</code> - The name of the vector index.
         *   </li>
         *   <li>
         *     <code>IndexStatus</code> - The current status of the vector index: <code>CREATING</code>, <code>ACTIVE</code>
         *     , or <code>DELETING</code>.
         *   </li>
         *   <li>
         *     <code>Backfilling</code> - Specifies whether the index is currently backfilling. During backfill, <code>
         *     SearchVectors</code> operations might return incomplete results.
         *   </li>
         *   <li>
         *     <code>VectorAttribute</code> - The attribute that contains vector embeddings.
         *   </li>
         *   <li>
         *     <code>Dimensions</code> - The number of dimensions in each vector.
         *   </li>
         *   <li>
         *     <code>DistanceFunction</code> - The distance function used to calculate similarity (<code>COSINE</code>, <code>
         *     EUCLIDEAN</code>, or <code>DOT_PRODUCT</code>).
         *   </li>
         *   <li>
         *     <code>SearchSchema</code> - The partition key and inline filter attributes for the vector index.
         *   </li>
         *   <li>
         *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the vector
         *     index.
         *   </li>
         *   <li>
         *     <code>IndexArn</code> - The Amazon Resource Name (ARN) that uniquely identifies the index.
         *   </li>
         *   <li>
         *     <code>IndexSizeBytes</code> - The total size of the vector index, in bytes. Amazon DynamoDB updates this
         *     value approximately every six hours. Recent changes might not be reflected in this value.
         *   </li>
         *   <li>
         *     <code>ItemCount</code> - The number of items indexed in the vector index. Amazon DynamoDB updates this
         *     value approximately every six hours. Recent changes might not be reflected in this value.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder vectorIndexes(List<VectorIndexDescription> vectorIndexes) {
            this.vectorIndexes = vectorIndexes;
            return this;
        }

        @Override
        public TableDescription build() {
            return new TableDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> attributeDefinitions((List<AttributeDefinition>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, value));
                case 1 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 2 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 3 -> tableStatus((TableStatus) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, value));
                case 4 -> creationDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE_TIME, member, value));
                case 5 -> provisionedThroughput((ProvisionedThroughputDescription) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 6 -> tableSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_TABLE_SIZE_BYTES, member, value));
                case 7 -> itemCount((Long) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, value));
                case 8 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 9 -> tableId((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, value));
                case 10 -> billingModeSummary((BillingModeSummary) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE_SUMMARY, member, value));
                case 11 -> localSecondaryIndexes((List<LocalSecondaryIndexDescription>) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, value));
                case 12 -> globalSecondaryIndexes((List<GlobalSecondaryIndexDescription>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 13 -> streamSpecification((StreamSpecification) SchemaUtils.validateSameMember($SCHEMA_STREAM_SPECIFICATION, member, value));
                case 14 -> latestStreamLabel((String) SchemaUtils.validateSameMember($SCHEMA_LATEST_STREAM_LABEL, member, value));
                case 15 -> latestStreamArn((String) SchemaUtils.validateSameMember($SCHEMA_LATEST_STREAM_ARN, member, value));
                case 16 -> globalTableVersion((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_VERSION, member, value));
                case 17 -> replicas((List<ReplicaDescription>) SchemaUtils.validateSameMember($SCHEMA_REPLICAS, member, value));
                case 18 -> globalTableWitnesses((List<GlobalTableWitnessDescription>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_WITNESSES, member, value));
                case 19 -> globalTableSettingsReplicationMode((GlobalTableSettingsReplicationMode) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, value));
                case 20 -> restoreSummary((RestoreSummary) SchemaUtils.validateSameMember($SCHEMA_RESTORE_SUMMARY, member, value));
                case 21 -> sseDescription((SSEDescription) SchemaUtils.validateSameMember($SCHEMA_SSE_DESCRIPTION, member, value));
                case 22 -> archivalSummary((ArchivalSummary) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_SUMMARY, member, value));
                case 23 -> tableClassSummary((TableClassSummary) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS_SUMMARY, member, value));
                case 24 -> deletionProtectionEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETION_PROTECTION_ENABLED, member, value));
                case 25 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 26 -> warmThroughput((TableWarmThroughputDescription) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
                case 27 -> multiRegionConsistency((MultiRegionConsistency) SchemaUtils.validateSameMember($SCHEMA_MULTI_REGION_CONSISTENCY, member, value));
                case 28 -> vectorIndexes((List<VectorIndexDescription>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, value));
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
                    case 0 -> builder.attributeDefinitions(SharedSerde.deserializeAttributeDefinitions(member, de));
                    case 1 -> builder.tableName(de.readString(member));
                    case 2 -> builder.keySchema(SharedSerde.deserializeKeySchema(member, de));
                    case 3 -> builder.tableStatus(TableStatus.builder().deserializeMember(de, member).build());
                    case 4 -> builder.creationDateTime(de.readTimestamp(member));
                    case 5 -> builder.provisionedThroughput(ProvisionedThroughputDescription.builder().deserializeMember(de, member).build());
                    case 6 -> builder.tableSizeBytes(de.readLong(member));
                    case 7 -> builder.itemCount(de.readLong(member));
                    case 8 -> builder.tableArn(de.readString(member));
                    case 9 -> builder.tableId(de.readString(member));
                    case 10 -> builder.billingModeSummary(BillingModeSummary.builder().deserializeMember(de, member).build());
                    case 11 -> builder.localSecondaryIndexes(SharedSerde.deserializeLocalSecondaryIndexDescriptionList(member, de));
                    case 12 -> builder.globalSecondaryIndexes(SharedSerde.deserializeGlobalSecondaryIndexDescriptionList(member, de));
                    case 13 -> builder.streamSpecification(StreamSpecification.builder().deserializeMember(de, member).build());
                    case 14 -> builder.latestStreamLabel(de.readString(member));
                    case 15 -> builder.latestStreamArn(de.readString(member));
                    case 16 -> builder.globalTableVersion(de.readString(member));
                    case 17 -> builder.replicas(SharedSerde.deserializeReplicaDescriptionList(member, de));
                    case 18 -> builder.globalTableWitnesses(SharedSerde.deserializeGlobalTableWitnessDescriptionList(member, de));
                    case 19 -> builder.globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode.builder().deserializeMember(de, member).build());
                    case 20 -> builder.restoreSummary(RestoreSummary.builder().deserializeMember(de, member).build());
                    case 21 -> builder.sseDescription(SSEDescription.builder().deserializeMember(de, member).build());
                    case 22 -> builder.archivalSummary(ArchivalSummary.builder().deserializeMember(de, member).build());
                    case 23 -> builder.tableClassSummary(TableClassSummary.builder().deserializeMember(de, member).build());
                    case 24 -> builder.deletionProtectionEnabled(de.readBoolean(member));
                    case 25 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 26 -> builder.warmThroughput(TableWarmThroughputDescription.builder().deserializeMember(de, member).build());
                    case 27 -> builder.multiRegionConsistency(MultiRegionConsistency.builder().deserializeMember(de, member).build());
                    case 28 -> builder.vectorIndexes(SharedSerde.deserializeVectorIndexDescriptionList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
