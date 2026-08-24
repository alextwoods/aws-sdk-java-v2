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
 * The parameters for the table created as part of the import operation.
 */
@SmithyGenerated
public final class TableCreationParameters implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TABLE_CREATION_PARAMETERS;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_ATTRIBUTE_DEFINITIONS = $SCHEMA.member("AttributeDefinitions");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_BILLING_MODE = $SCHEMA.member("BillingMode");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_SSE_SPECIFICATION = $SCHEMA.member("SSESpecification");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_VECTOR_INDEXES = $SCHEMA.member("VectorIndexes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient List<AttributeDefinition> attributeDefinitions;
    private final transient List<KeySchemaElement> keySchema;
    private final transient BillingMode billingMode;
    private final transient ProvisionedThroughput provisionedThroughput;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient SSESpecification sseSpecification;
    private final transient List<GlobalSecondaryIndex> globalSecondaryIndexes;
    private final transient List<VectorIndex> vectorIndexes;

    private TableCreationParameters(Builder builder) {
        this.tableName = builder.tableName;
        this.attributeDefinitions = Collections.unmodifiableList(builder.attributeDefinitions);
        this.keySchema = Collections.unmodifiableList(builder.keySchema);
        this.billingMode = builder.billingMode;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.onDemandThroughput = builder.onDemandThroughput;
        this.sseSpecification = builder.sseSpecification;
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.vectorIndexes = builder.vectorIndexes == null ? null : Collections.unmodifiableList(builder.vectorIndexes);
    }

    /**
     * The name of the table created as part of the import operation.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The attributes of the table created as part of the import operation.
     */
    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public boolean hasAttributeDefinitions() {
        return true;
    }

    /**
     * The primary key and option sort key of the table created as part of the import operation.
     */
    public List<KeySchemaElement> getKeySchema() {
        return keySchema;
    }

    public boolean hasKeySchema() {
        return true;
    }

    /**
     * The billing mode for provisioning the table created as part of the import operation.
     */
    public BillingMode getBillingMode() {
        return billingMode;
    }

    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    public SSESpecification getSseSpecification() {
        return sseSpecification;
    }

    /**
     * The Global Secondary Indexes (GSI) of the table to be created as part of the import operation.
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
     * The vector indexes of the table to be created as part of the import operation.
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
        TableCreationParameters that = (TableCreationParameters) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.billingMode, that.billingMode)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.sseSpecification, that.sseSpecification)
               && Objects.equals(this.attributeDefinitions, that.attributeDefinitions)
               && Objects.equals(this.keySchema, that.keySchema)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes)
               && Objects.equals(this.vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(attributeDefinitions);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(billingMode);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
        $hc = 31 * $hc + Objects.hashCode(sseSpecification);
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
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        serializer.writeList($SCHEMA_ATTRIBUTE_DEFINITIONS, attributeDefinitions, attributeDefinitions.size(), SharedSerde.AttributeDefinitionsSerializer.INSTANCE);
        serializer.writeList($SCHEMA_KEY_SCHEMA, keySchema, keySchema.size(), SharedSerde.KeySchemaSerializer.INSTANCE);
        if (billingMode != null) {
            serializer.writeString($SCHEMA_BILLING_MODE, billingMode.getValue());
        }
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
        }
        if (sseSpecification != null) {
            serializer.writeStruct($SCHEMA_SSE_SPECIFICATION, sseSpecification);
        }
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.GlobalSecondaryIndexListSerializer.INSTANCE);
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
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, billingMode);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION, member, sseSpecification);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, vectorIndexes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TableCreationParameters}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.attributeDefinitions(this.attributeDefinitions);
        builder.keySchema(this.keySchema);
        builder.billingMode(this.billingMode);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.onDemandThroughput(this.onDemandThroughput);
        builder.sseSpecification(this.sseSpecification);
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
     * Builder for {@link TableCreationParameters}.
     */
    public static final class Builder implements ShapeBuilder<TableCreationParameters> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private List<AttributeDefinition> attributeDefinitions;
        private List<KeySchemaElement> keySchema;
        private BillingMode billingMode;
        private ProvisionedThroughput provisionedThroughput;
        private OnDemandThroughput onDemandThroughput;
        private SSESpecification sseSpecification;
        private List<GlobalSecondaryIndex> globalSecondaryIndexes;
        private List<VectorIndex> vectorIndexes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table created as part of the import operation.
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
         * The attributes of the table created as part of the import operation.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
            this.attributeDefinitions = Objects.requireNonNull(attributeDefinitions, "attributeDefinitions cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_DEFINITIONS);
            return this;
        }

        /**
         * The primary key and option sort key of the table created as part of the import operation.
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
         * The billing mode for provisioning the table created as part of the import operation.
         *
         * @return this builder.
         */
        public Builder billingMode(BillingMode billingMode) {
            this.billingMode = billingMode;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
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
         * @return this builder.
         */
        public Builder sseSpecification(SSESpecification sseSpecification) {
            this.sseSpecification = sseSpecification;
            return this;
        }

        /**
         * The Global Secondary Indexes (GSI) of the table to be created as part of the import operation.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<GlobalSecondaryIndex> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * The vector indexes of the table to be created as part of the import operation.
         *
         * @return this builder.
         */
        public Builder vectorIndexes(List<VectorIndex> vectorIndexes) {
            this.vectorIndexes = vectorIndexes;
            return this;
        }

        @Override
        public TableCreationParameters build() {
            tracker.validate();
            return new TableCreationParameters(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> attributeDefinitions((List<AttributeDefinition>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, value));
                case 2 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 3 -> billingMode((BillingMode) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, value));
                case 4 -> provisionedThroughput((ProvisionedThroughput) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 5 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 6 -> sseSpecification((SSESpecification) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION, member, value));
                case 7 -> globalSecondaryIndexes((List<GlobalSecondaryIndex>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 8 -> vectorIndexes((List<VectorIndex>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TableCreationParameters> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_DEFINITIONS)) {
                attributeDefinitions(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_KEY_SCHEMA)) {
                keySchema(Collections.emptyList());
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
                    case 3 -> builder.billingMode(BillingMode.builder().deserializeMember(de, member).build());
                    case 4 -> builder.provisionedThroughput(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 5 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 6 -> builder.sseSpecification(SSESpecification.builder().deserializeMember(de, member).build());
                    case 7 -> builder.globalSecondaryIndexes(SharedSerde.deserializeGlobalSecondaryIndexList(member, de));
                    case 8 -> builder.vectorIndexes(SharedSerde.deserializeVectorIndexList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
