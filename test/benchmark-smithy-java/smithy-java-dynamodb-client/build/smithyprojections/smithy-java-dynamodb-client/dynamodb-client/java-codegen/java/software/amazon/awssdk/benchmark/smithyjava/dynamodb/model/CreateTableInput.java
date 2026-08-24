package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the input of a <code>CreateTable</code> operation.
 */
@SmithyGenerated
public final class CreateTableInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_TABLE_INPUT;
    private static final Schema $SCHEMA_ATTRIBUTE_DEFINITIONS = $SCHEMA.member("AttributeDefinitions");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_LOCAL_SECONDARY_INDEXES = $SCHEMA.member("LocalSecondaryIndexes");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_BILLING_MODE = $SCHEMA.member("BillingMode");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_STREAM_SPECIFICATION = $SCHEMA.member("StreamSpecification");
    private static final Schema $SCHEMA_SSE_SPECIFICATION = $SCHEMA.member("SSESpecification");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_TABLE_CLASS = $SCHEMA.member("TableClass");
    private static final Schema $SCHEMA_DELETION_PROTECTION_ENABLED = $SCHEMA.member("DeletionProtectionEnabled");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");
    private static final Schema $SCHEMA_RESOURCE_POLICY = $SCHEMA.member("ResourcePolicy");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_GLOBAL_TABLE_SOURCE_ARN = $SCHEMA.member("GlobalTableSourceArn");
    private static final Schema $SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE = $SCHEMA.member("GlobalTableSettingsReplicationMode");
    private static final Schema $SCHEMA_VECTOR_INDEXES = $SCHEMA.member("VectorIndexes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AttributeDefinition> attributeDefinitions;
    private final transient String tableName;
    private final transient List<KeySchemaElement> keySchema;
    private final transient List<LocalSecondaryIndex> localSecondaryIndexes;
    private final transient List<GlobalSecondaryIndex> globalSecondaryIndexes;
    private final transient BillingMode billingMode;
    private final transient ProvisionedThroughput provisionedThroughput;
    private final transient StreamSpecification streamSpecification;
    private final transient SSESpecification sseSpecification;
    private final transient List<Tag> tags;
    private final transient TableClass tableClass;
    private final transient Boolean deletionProtectionEnabled;
    private final transient WarmThroughput warmThroughput;
    private final transient String resourcePolicy;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient String globalTableSourceArn;
    private final transient GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;
    private final transient List<VectorIndex> vectorIndexes;

    private CreateTableInput(Builder builder) {
        this.attributeDefinitions = builder.attributeDefinitions == null ? null : Collections.unmodifiableList(builder.attributeDefinitions);
        this.tableName = builder.tableName;
        this.keySchema = builder.keySchema == null ? null : Collections.unmodifiableList(builder.keySchema);
        this.localSecondaryIndexes = builder.localSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.localSecondaryIndexes);
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.billingMode = builder.billingMode;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.streamSpecification = builder.streamSpecification;
        this.sseSpecification = builder.sseSpecification;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.tableClass = builder.tableClass;
        this.deletionProtectionEnabled = builder.deletionProtectionEnabled;
        this.warmThroughput = builder.warmThroughput;
        this.resourcePolicy = builder.resourcePolicy;
        this.onDemandThroughput = builder.onDemandThroughput;
        this.globalTableSourceArn = builder.globalTableSourceArn;
        this.globalTableSettingsReplicationMode = builder.globalTableSettingsReplicationMode;
        this.vectorIndexes = builder.vectorIndexes == null ? null : Collections.unmodifiableList(builder.vectorIndexes);
    }

    /**
     * An array of attributes that describe the key schema for the table and indexes.
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
     * The name of the table to create. You can also provide the Amazon Resource Name (ARN) of the table in this
     * parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Specifies the attributes that make up the primary key for a table or an index. The attributes in <code>KeySchema</code>
     * must also be defined in the <code>AttributeDefinitions</code> array. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataModel.html">Data Model</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p>Each <code>KeySchemaElement</code> in the array is composed of:
     *
     * <ul>
     *   <li>
     *     <code>AttributeName</code> - The name of this key attribute.
     *   </li>
     *   <li>
     *     <code>KeyType</code> - The role that the key attribute will assume:
     *
     *     <ul>
     *       <li>
     *         <code>HASH</code> - partition key
     *       </li>
     *       <li>
     *         <code>RANGE</code> - sort key
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute" derives
     * from the DynamoDB usage of an internal hash function to evenly distribute data items across partitions, based on
     * their partition key values.
     *
     * <p>The sort key of an item is also known as its <i>range attribute</i>. The term "range attribute" derives from
     * the way DynamoDB stores items with the same partition key physically close together, in sorted order by the sort
     * key value.
     *
     * <p>For a simple primary key (partition key), you must provide exactly one element with a <code>KeyType</code> of <code>
     * HASH</code>.
     *
     * <p>For a composite primary key (partition key and sort key), you must provide exactly two elements, in this
     * order: The first element must have a <code>KeyType</code> of <code>HASH</code>, and the second element must have
     * a <code>KeyType</code> of <code>RANGE</code>.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#WorkingWithTables.primary.key">Working with Tables</a> in the <i>Amazon DynamoDB Developer Guide</i>.
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
     * One or more local secondary indexes (the maximum is 5) to be created on the table. Each index is scoped to a
     * given partition key value. There is a 10 GB size limit per partition key value; otherwise, the size of a local
     * secondary index is unconstrained.
     *
     * <p>Each local secondary index in the array includes the following:
     *
     * <ul>
     *   <li>
     *     <code>IndexName</code> - The name of the local secondary index. Must be unique only for this table.
     *   </li>
     *   <li>
     *     <code>KeySchema</code> - Specifies the key schema for the local secondary index. The key schema must
     *     begin with the same partition key as the table.
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
     * </ul>
     */
    public List<LocalSecondaryIndex> getLocalSecondaryIndexes() {
        if (localSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return localSecondaryIndexes;
    }

    public boolean hasLocalSecondaryIndexes() {
        return localSecondaryIndexes != null;
    }

    /**
     * One or more global secondary indexes (the maximum is 20) to be created on the table. Each global secondary index
     * in the array includes the following:
     *
     * <ul>
     *   <li>
     *     <code>IndexName</code> - The name of the global secondary index. Must be unique only for this table.
     *   </li>
     *   <li>
     *     <code>KeySchema</code> - Specifies the key schema for the global secondary index. Each global secondary
     *     index supports up to 4 partition keys and up to 4 sort keys.
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
     *     <code>ProvisionedThroughput</code> - The provisioned throughput settings for the global secondary index,
     *     consisting of read and write capacity units.
     *   </li>
     * </ul>
     */
    public List<GlobalSecondaryIndex> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    /**
     * Controls how you are charged for read and write throughput and how you manage capacity. This setting can be
     * changed later.
     *
     * <ul>
     *   <li>
     *     <code>PAY_PER_REQUEST</code> - We recommend using <code>PAY_PER_REQUEST</code> for most DynamoDB
     *     workloads. <code>PAY_PER_REQUEST</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html">On-demand capacity mode</a>.
     *   </li>
     *   <li>
     *     <code>PROVISIONED</code> - We recommend using <code>PROVISIONED</code> for steady workloads with
     *     predictable growth where capacity requirements can be reliably forecasted. <code>PROVISIONED</code> sets
     *     the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a>.
     *   </li>
     * </ul>
     */
    public BillingMode getBillingMode() {
        return billingMode;
    }

    /**
     * Represents the provisioned throughput settings for a specified table or index. The settings can be modified using
     * the <code>UpdateTable</code> operation.
     *
     * <p> If you set BillingMode as <code>PROVISIONED</code>, you must specify this property. If you set BillingMode as
     * <code>PAY_PER_REQUEST</code>, you cannot specify this property.
     *
     * <p>For current minimum and maximum provisioned throughput values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    /**
     * The settings for DynamoDB Streams on the table. These settings consist of:
     *
     * <ul>
     *   <li>
     *     <code>StreamEnabled</code> - Indicates whether DynamoDB Streams is to be enabled (true) or disabled
     *     (false).
     *   </li>
     *   <li>
     *     <code>StreamViewType</code> - When an item in the table is modified, <code>StreamViewType</code>
     *     determines what information is written to the table's stream. Valid values for <code>StreamViewType</code>
     *     are:
     *
     *     <ul>
     *       <li>
     *         <code>KEYS_ONLY</code> - Only the key attributes of the modified item are written to the stream.
     *       </li>
     *       <li>
     *         <code>NEW_IMAGE</code> - The entire item, as it appears after it was modified, is written to the
     *         stream.
     *       </li>
     *       <li>
     *         <code>OLD_IMAGE</code> - The entire item, as it appeared before it was modified, is written to
     *         the stream.
     *       </li>
     *       <li>
     *         <code>NEW_AND_OLD_IMAGES</code> - Both the new and the old item images of the item are written to
     *         the stream.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     */
    public StreamSpecification getStreamSpecification() {
        return streamSpecification;
    }

    /**
     * Represents the settings used to enable server-side encryption.
     */
    public SSESpecification getSseSpecification() {
        return sseSpecification;
    }

    /**
     * A list of key-value pairs to label the table. For more information, see <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a>.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
    }

    /**
     * The table class of the new table. Valid values are <code>STANDARD</code> and
     * <code>STANDARD_INFREQUENT_ACCESS</code>.
     */
    public TableClass getTableClass() {
        return tableClass;
    }

    /**
     * Indicates whether deletion protection is to be enabled (true) or disabled (false) on the table.
     */
    public Boolean isDeletionProtectionEnabled() {
        return deletionProtectionEnabled;
    }

    /**
     * Represents the warm throughput (in read units per second and write units per second) for creating a table.
     */
    public WarmThroughput getWarmThroughput() {
        return warmThroughput;
    }

    /**
     * An Amazon Web Services resource-based policy document in JSON format that will be attached to the table.
     *
     * <p>When you attach a resource-based policy while creating a table, the policy application is <i>strongly
     * consistent</i>.
     *
     * <p>The maximum size supported for a resource-based policy document is 20 KB. DynamoDB counts whitespaces when
     * calculating the size of a policy against this limit. For a full list of all considerations that apply for
     * resource-based policies, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/rbac-considerations.html">Resource-based policy considerations</a>.
     *
     * <p>You need to specify the <code>CreateTable</code> and <code>PutResourcePolicy</code> IAM actions for
     * authorizing a user to create a table with a resource-based policy.
     */
    public String getResourcePolicy() {
        return resourcePolicy;
    }

    /**
     * Sets the maximum number of read and write units for the specified table in on-demand capacity mode. If you use
     * this parameter, you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
     */
    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    /**
     * The Amazon Resource Name (ARN) of the source table used for the creation of a multi-account global table.
     */
    public String getGlobalTableSourceArn() {
        return globalTableSourceArn;
    }

    /**
     * Controls the settings synchronization mode for the global table. For multi-account global tables, this parameter
     * is required and the only supported value is ENABLED. For same-account global tables, this parameter is set to
     * ENABLED_WITH_OVERRIDES.
     */
    public GlobalTableSettingsReplicationMode getGlobalTableSettingsReplicationMode() {
        return globalTableSettingsReplicationMode;
    }

    /**
     * One or more vector indexes to be created on the table. Each vector index enables similarity search on a vector
     * attribute. Each element in the list consists of:
     *
     * <ul>
     *   <li>
     *     <code>IndexName</code> - The name of the vector index. Must be unique within the table.
     *   </li>
     *   <li>
     *     <code>VectorAttribute</code> - The attribute that contains vector embeddings. If multiple vector indexes
     *     reference the same attribute, they must all use the same number of dimensions.
     *   </li>
     *   <li>
     *     <code>Dimensions</code> - The number of dimensions in each vector.
     *   </li>
     *   <li>
     *     <code>DistanceFunction</code> - The distance function used to calculate similarity. Valid values: <code>
     *     COSINE</code>, <code>EUCLIDEAN</code>, <code>DOT_PRODUCT</code>.
     *   </li>
     *   <li>
     *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the vector
     *     index. The total number of projected non-key attributes is shared across the vector attribute (counts as
     *     1) and <code>INLINE_FILTER</code> search schema elements (each counts as 1). <code>HASH</code> search
     *     schema elements do not count toward this limit.
     *   </li>
     *   <li>
     *     <code>SearchSchema</code> - (Optional) Defines the partition key (<code>HASH</code>) and inline filter (<code>
     *     INLINE_FILTER</code>) attributes for the vector index.
     *   </li>
     * </ul>
     */
    public List<VectorIndex> getVectorIndexes() {
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
        CreateTableInput that = (CreateTableInput) other;
        return Objects.equals(this.deletionProtectionEnabled, that.deletionProtectionEnabled)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.resourcePolicy, that.resourcePolicy)
               && Objects.equals(this.globalTableSourceArn, that.globalTableSourceArn)
               && Objects.equals(this.billingMode, that.billingMode)
               && Objects.equals(this.tableClass, that.tableClass)
               && Objects.equals(this.globalTableSettingsReplicationMode, that.globalTableSettingsReplicationMode)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.streamSpecification, that.streamSpecification)
               && Objects.equals(this.sseSpecification, that.sseSpecification)
               && Objects.equals(this.warmThroughput, that.warmThroughput)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.attributeDefinitions, that.attributeDefinitions)
               && Objects.equals(this.keySchema, that.keySchema)
               && Objects.equals(this.localSecondaryIndexes, that.localSecondaryIndexes)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes)
               && Objects.equals(this.tags, that.tags)
               && Objects.equals(this.vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeDefinitions);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(localSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(billingMode);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(streamSpecification);
        $hc = 31 * $hc + Objects.hashCode(sseSpecification);
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(tableClass);
        $hc = 31 * $hc + Objects.hashCode(deletionProtectionEnabled);
        $hc = 31 * $hc + Objects.hashCode(warmThroughput);
        $hc = 31 * $hc + Objects.hashCode(resourcePolicy);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
        $hc = 31 * $hc + Objects.hashCode(globalTableSourceArn);
        $hc = 31 * $hc + Objects.hashCode(globalTableSettingsReplicationMode);
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
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (keySchema != null) {
            serializer.writeList($SCHEMA_KEY_SCHEMA, keySchema, keySchema.size(), SharedSerde.KeySchemaSerializer.INSTANCE);
        }
        if (localSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_LOCAL_SECONDARY_INDEXES, localSecondaryIndexes, localSecondaryIndexes.size(), SharedSerde.LocalSecondaryIndexListSerializer.INSTANCE);
        }
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.GlobalSecondaryIndexListSerializer.INSTANCE);
        }
        if (billingMode != null) {
            serializer.writeString($SCHEMA_BILLING_MODE, billingMode.getValue());
        }
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (streamSpecification != null) {
            serializer.writeStruct($SCHEMA_STREAM_SPECIFICATION, streamSpecification);
        }
        if (sseSpecification != null) {
            serializer.writeStruct($SCHEMA_SSE_SPECIFICATION, sseSpecification);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
        if (tableClass != null) {
            serializer.writeString($SCHEMA_TABLE_CLASS, tableClass.getValue());
        }
        if (deletionProtectionEnabled != null) {
            serializer.writeBoolean($SCHEMA_DELETION_PROTECTION_ENABLED, deletionProtectionEnabled);
        }
        if (warmThroughput != null) {
            serializer.writeStruct($SCHEMA_WARM_THROUGHPUT, warmThroughput);
        }
        if (resourcePolicy != null) {
            serializer.writeString($SCHEMA_RESOURCE_POLICY, resourcePolicy);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
        }
        if (globalTableSourceArn != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_SOURCE_ARN, globalTableSourceArn);
        }
        if (globalTableSettingsReplicationMode != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, globalTableSettingsReplicationMode.getValue());
        }
        if (vectorIndexes != null) {
            serializer.writeList($SCHEMA_VECTOR_INDEXES, vectorIndexes, vectorIndexes.size(), SharedSerde.VectorIndexListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, attributeDefinitions);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, keySchema);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, localSecondaryIndexes);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, billingMode);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_SPECIFICATION, member, streamSpecification);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION, member, sseSpecification);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS, member, tableClass);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETION_PROTECTION_ENABLED, member, deletionProtectionEnabled);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_POLICY, member, resourcePolicy);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SOURCE_ARN, member, globalTableSourceArn);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, globalTableSettingsReplicationMode);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, vectorIndexes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateTableInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeDefinitions(this.attributeDefinitions);
        builder.tableName(this.tableName);
        builder.keySchema(this.keySchema);
        builder.localSecondaryIndexes(this.localSecondaryIndexes);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.billingMode(this.billingMode);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.streamSpecification(this.streamSpecification);
        builder.sseSpecification(this.sseSpecification);
        builder.tags(this.tags);
        builder.tableClass(this.tableClass);
        builder.deletionProtectionEnabled(this.deletionProtectionEnabled);
        builder.warmThroughput(this.warmThroughput);
        builder.resourcePolicy(this.resourcePolicy);
        builder.onDemandThroughput(this.onDemandThroughput);
        builder.globalTableSourceArn(this.globalTableSourceArn);
        builder.globalTableSettingsReplicationMode(this.globalTableSettingsReplicationMode);
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
     * Builder for {@link CreateTableInput}.
     */
    public static final class Builder implements ShapeBuilder<CreateTableInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<AttributeDefinition> attributeDefinitions;
        private String tableName;
        private List<KeySchemaElement> keySchema;
        private List<LocalSecondaryIndex> localSecondaryIndexes;
        private List<GlobalSecondaryIndex> globalSecondaryIndexes;
        private BillingMode billingMode;
        private ProvisionedThroughput provisionedThroughput;
        private StreamSpecification streamSpecification;
        private SSESpecification sseSpecification;
        private List<Tag> tags;
        private TableClass tableClass;
        private Boolean deletionProtectionEnabled;
        private WarmThroughput warmThroughput;
        private String resourcePolicy;
        private OnDemandThroughput onDemandThroughput;
        private String globalTableSourceArn;
        private GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;
        private List<VectorIndex> vectorIndexes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of attributes that describe the key schema for the table and indexes.
         *
         * @return this builder.
         */
        public Builder attributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
            this.attributeDefinitions = attributeDefinitions;
            return this;
        }

        /**
         * The name of the table to create. You can also provide the Amazon Resource Name (ARN) of the table in this
         * parameter.
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
         * Specifies the attributes that make up the primary key for a table or an index. The attributes in <code>KeySchema</code>
         * must also be defined in the <code>AttributeDefinitions</code> array. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataModel.html">Data Model</a>
         * in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * <p>Each <code>KeySchemaElement</code> in the array is composed of:
         *
         * <ul>
         *   <li>
         *     <code>AttributeName</code> - The name of this key attribute.
         *   </li>
         *   <li>
         *     <code>KeyType</code> - The role that the key attribute will assume:
         *
         *     <ul>
         *       <li>
         *         <code>HASH</code> - partition key
         *       </li>
         *       <li>
         *         <code>RANGE</code> - sort key
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * <p>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute" derives
         * from the DynamoDB usage of an internal hash function to evenly distribute data items across partitions, based on
         * their partition key values.
         *
         * <p>The sort key of an item is also known as its <i>range attribute</i>. The term "range attribute" derives from
         * the way DynamoDB stores items with the same partition key physically close together, in sorted order by the sort
         * key value.
         *
         * <p>For a simple primary key (partition key), you must provide exactly one element with a <code>KeyType</code> of <code>
         * HASH</code>.
         *
         * <p>For a composite primary key (partition key and sort key), you must provide exactly two elements, in this
         * order: The first element must have a <code>KeyType</code> of <code>HASH</code>, and the second element must have
         * a <code>KeyType</code> of <code>RANGE</code>.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#WorkingWithTables.primary.key">Working with Tables</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder keySchema(List<KeySchemaElement> keySchema) {
            this.keySchema = keySchema;
            return this;
        }

        /**
         * One or more local secondary indexes (the maximum is 5) to be created on the table. Each index is scoped to a
         * given partition key value. There is a 10 GB size limit per partition key value; otherwise, the size of a local
         * secondary index is unconstrained.
         *
         * <p>Each local secondary index in the array includes the following:
         *
         * <ul>
         *   <li>
         *     <code>IndexName</code> - The name of the local secondary index. Must be unique only for this table.
         *   </li>
         *   <li>
         *     <code>KeySchema</code> - Specifies the key schema for the local secondary index. The key schema must
         *     begin with the same partition key as the table.
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
         * </ul>
         *
         * @return this builder.
         */
        public Builder localSecondaryIndexes(List<LocalSecondaryIndex> localSecondaryIndexes) {
            this.localSecondaryIndexes = localSecondaryIndexes;
            return this;
        }

        /**
         * One or more global secondary indexes (the maximum is 20) to be created on the table. Each global secondary index
         * in the array includes the following:
         *
         * <ul>
         *   <li>
         *     <code>IndexName</code> - The name of the global secondary index. Must be unique only for this table.
         *   </li>
         *   <li>
         *     <code>KeySchema</code> - Specifies the key schema for the global secondary index. Each global secondary
         *     index supports up to 4 partition keys and up to 4 sort keys.
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
         *     <code>ProvisionedThroughput</code> - The provisioned throughput settings for the global secondary index,
         *     consisting of read and write capacity units.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<GlobalSecondaryIndex> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * Controls how you are charged for read and write throughput and how you manage capacity. This setting can be
         * changed later.
         *
         * <ul>
         *   <li>
         *     <code>PAY_PER_REQUEST</code> - We recommend using <code>PAY_PER_REQUEST</code> for most DynamoDB
         *     workloads. <code>PAY_PER_REQUEST</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html">On-demand capacity mode</a>.
         *   </li>
         *   <li>
         *     <code>PROVISIONED</code> - We recommend using <code>PROVISIONED</code> for steady workloads with
         *     predictable growth where capacity requirements can be reliably forecasted. <code>PROVISIONED</code> sets
         *     the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a>.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder billingMode(BillingMode billingMode) {
            this.billingMode = billingMode;
            return this;
        }

        /**
         * Represents the provisioned throughput settings for a specified table or index. The settings can be modified using
         * the <code>UpdateTable</code> operation.
         *
         * <p> If you set BillingMode as <code>PROVISIONED</code>, you must specify this property. If you set BillingMode as
         * <code>PAY_PER_REQUEST</code>, you cannot specify this property.
         *
         * <p>For current minimum and maximum provisioned throughput values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a>
         * in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * The settings for DynamoDB Streams on the table. These settings consist of:
         *
         * <ul>
         *   <li>
         *     <code>StreamEnabled</code> - Indicates whether DynamoDB Streams is to be enabled (true) or disabled
         *     (false).
         *   </li>
         *   <li>
         *     <code>StreamViewType</code> - When an item in the table is modified, <code>StreamViewType</code>
         *     determines what information is written to the table's stream. Valid values for <code>StreamViewType</code>
         *     are:
         *
         *     <ul>
         *       <li>
         *         <code>KEYS_ONLY</code> - Only the key attributes of the modified item are written to the stream.
         *       </li>
         *       <li>
         *         <code>NEW_IMAGE</code> - The entire item, as it appears after it was modified, is written to the
         *         stream.
         *       </li>
         *       <li>
         *         <code>OLD_IMAGE</code> - The entire item, as it appeared before it was modified, is written to
         *         the stream.
         *       </li>
         *       <li>
         *         <code>NEW_AND_OLD_IMAGES</code> - Both the new and the old item images of the item are written to
         *         the stream.
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder streamSpecification(StreamSpecification streamSpecification) {
            this.streamSpecification = streamSpecification;
            return this;
        }

        /**
         * Represents the settings used to enable server-side encryption.
         *
         * @return this builder.
         */
        public Builder sseSpecification(SSESpecification sseSpecification) {
            this.sseSpecification = sseSpecification;
            return this;
        }

        /**
         * A list of key-value pairs to label the table. For more information, see <a
         * href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a>.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * The table class of the new table. Valid values are <code>STANDARD</code> and
         * <code>STANDARD_INFREQUENT_ACCESS</code>.
         *
         * @return this builder.
         */
        public Builder tableClass(TableClass tableClass) {
            this.tableClass = tableClass;
            return this;
        }

        /**
         * Indicates whether deletion protection is to be enabled (true) or disabled (false) on the table.
         *
         * @return this builder.
         */
        public Builder deletionProtectionEnabled(Boolean deletionProtectionEnabled) {
            this.deletionProtectionEnabled = deletionProtectionEnabled;
            return this;
        }

        /**
         * Represents the warm throughput (in read units per second and write units per second) for creating a table.
         *
         * @return this builder.
         */
        public Builder warmThroughput(WarmThroughput warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        /**
         * An Amazon Web Services resource-based policy document in JSON format that will be attached to the table.
         *
         * <p>When you attach a resource-based policy while creating a table, the policy application is <i>strongly
         * consistent</i>.
         *
         * <p>The maximum size supported for a resource-based policy document is 20 KB. DynamoDB counts whitespaces when
         * calculating the size of a policy against this limit. For a full list of all considerations that apply for
         * resource-based policies, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/rbac-considerations.html">Resource-based policy considerations</a>.
         *
         * <p>You need to specify the <code>CreateTable</code> and <code>PutResourcePolicy</code> IAM actions for
         * authorizing a user to create a table with a resource-based policy.
         *
         * @return this builder.
         */
        public Builder resourcePolicy(String resourcePolicy) {
            this.resourcePolicy = resourcePolicy;
            return this;
        }

        /**
         * Sets the maximum number of read and write units for the specified table in on-demand capacity mode. If you use
         * this parameter, you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
         *
         * @return this builder.
         */
        public Builder onDemandThroughput(OnDemandThroughput onDemandThroughput) {
            this.onDemandThroughput = onDemandThroughput;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the source table used for the creation of a multi-account global table.
         *
         * @return this builder.
         */
        public Builder globalTableSourceArn(String globalTableSourceArn) {
            this.globalTableSourceArn = globalTableSourceArn;
            return this;
        }

        /**
         * Controls the settings synchronization mode for the global table. For multi-account global tables, this parameter
         * is required and the only supported value is ENABLED. For same-account global tables, this parameter is set to
         * ENABLED_WITH_OVERRIDES.
         *
         * @return this builder.
         */
        public Builder globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode) {
            this.globalTableSettingsReplicationMode = globalTableSettingsReplicationMode;
            return this;
        }

        /**
         * One or more vector indexes to be created on the table. Each vector index enables similarity search on a vector
         * attribute. Each element in the list consists of:
         *
         * <ul>
         *   <li>
         *     <code>IndexName</code> - The name of the vector index. Must be unique within the table.
         *   </li>
         *   <li>
         *     <code>VectorAttribute</code> - The attribute that contains vector embeddings. If multiple vector indexes
         *     reference the same attribute, they must all use the same number of dimensions.
         *   </li>
         *   <li>
         *     <code>Dimensions</code> - The number of dimensions in each vector.
         *   </li>
         *   <li>
         *     <code>DistanceFunction</code> - The distance function used to calculate similarity. Valid values: <code>
         *     COSINE</code>, <code>EUCLIDEAN</code>, <code>DOT_PRODUCT</code>.
         *   </li>
         *   <li>
         *     <code>Projection</code> - Specifies attributes that are copied (projected) from the table into the vector
         *     index. The total number of projected non-key attributes is shared across the vector attribute (counts as
         *     1) and <code>INLINE_FILTER</code> search schema elements (each counts as 1). <code>HASH</code> search
         *     schema elements do not count toward this limit.
         *   </li>
         *   <li>
         *     <code>SearchSchema</code> - (Optional) Defines the partition key (<code>HASH</code>) and inline filter (<code>
         *     INLINE_FILTER</code>) attributes for the vector index.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder vectorIndexes(List<VectorIndex> vectorIndexes) {
            this.vectorIndexes = vectorIndexes;
            return this;
        }

        @Override
        public CreateTableInput build() {
            tracker.validate();
            return new CreateTableInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> attributeDefinitions((List<AttributeDefinition>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, value));
                case 2 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 3 -> localSecondaryIndexes((List<LocalSecondaryIndex>) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, value));
                case 4 -> globalSecondaryIndexes((List<GlobalSecondaryIndex>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 5 -> billingMode((BillingMode) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, value));
                case 6 -> provisionedThroughput((ProvisionedThroughput) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 7 -> streamSpecification((StreamSpecification) SchemaUtils.validateSameMember($SCHEMA_STREAM_SPECIFICATION, member, value));
                case 8 -> sseSpecification((SSESpecification) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION, member, value));
                case 9 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 10 -> tableClass((TableClass) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS, member, value));
                case 11 -> deletionProtectionEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETION_PROTECTION_ENABLED, member, value));
                case 12 -> warmThroughput((WarmThroughput) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
                case 13 -> resourcePolicy((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_POLICY, member, value));
                case 14 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 15 -> globalTableSourceArn((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SOURCE_ARN, member, value));
                case 16 -> globalTableSettingsReplicationMode((GlobalTableSettingsReplicationMode) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, value));
                case 17 -> vectorIndexes((List<VectorIndex>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateTableInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
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
                    case 1 -> builder.attributeDefinitions(SharedSerde.deserializeAttributeDefinitions(member, de));
                    case 2 -> builder.keySchema(SharedSerde.deserializeKeySchema(member, de));
                    case 3 -> builder.localSecondaryIndexes(SharedSerde.deserializeLocalSecondaryIndexList(member, de));
                    case 4 -> builder.globalSecondaryIndexes(SharedSerde.deserializeGlobalSecondaryIndexList(member, de));
                    case 5 -> builder.billingMode(BillingMode.builder().deserializeMember(de, member).build());
                    case 6 -> builder.provisionedThroughput(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 7 -> builder.streamSpecification(StreamSpecification.builder().deserializeMember(de, member).build());
                    case 8 -> builder.sseSpecification(SSESpecification.builder().deserializeMember(de, member).build());
                    case 9 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 10 -> builder.tableClass(TableClass.builder().deserializeMember(de, member).build());
                    case 11 -> builder.deletionProtectionEnabled(de.readBoolean(member));
                    case 12 -> builder.warmThroughput(WarmThroughput.builder().deserializeMember(de, member).build());
                    case 13 -> builder.resourcePolicy(de.readString(member));
                    case 14 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 15 -> builder.globalTableSourceArn(de.readString(member));
                    case 16 -> builder.globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode.builder().deserializeMember(de, member).build());
                    case 17 -> builder.vectorIndexes(SharedSerde.deserializeVectorIndexList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
