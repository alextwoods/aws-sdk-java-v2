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
 * Represents a new global secondary index to be added to an existing table.
 */
@SmithyGenerated
public final class CreateGlobalSecondaryIndexAction implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_GLOBAL_SECONDARY_INDEX_ACTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_PROJECTION = $SCHEMA.member("Projection");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient List<KeySchemaElement> keySchema;
    private final transient Projection projection;
    private final transient ProvisionedThroughput provisionedThroughput;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient WarmThroughput warmThroughput;

    private CreateGlobalSecondaryIndexAction(Builder builder) {
        this.indexName = builder.indexName;
        this.keySchema = Collections.unmodifiableList(builder.keySchema);
        this.projection = builder.projection;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.onDemandThroughput = builder.onDemandThroughput;
        this.warmThroughput = builder.warmThroughput;
    }

    /**
     * The name of the global secondary index to be created.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The key schema for the global secondary index. Global secondary index supports up to 4 partition and up to 4 sort
     * keys.
     */
    public List<KeySchemaElement> getKeySchema() {
        return keySchema;
    }

    public boolean hasKeySchema() {
        return true;
    }

    /**
     * Represents attributes that are copied (projected) from the table into an index. These are in addition to the
     * primary key attributes and index key attributes, which are automatically projected.
     */
    public Projection getProjection() {
        return projection;
    }

    /**
     * Represents the provisioned throughput settings for the specified global secondary index.
     *
     * <p>For current minimum and maximum provisioned throughput values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    /**
     * The maximum number of read and write units for the global secondary index being created. If you use this
     * parameter, you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both. You
     * must use either <code>OnDemand Throughput</code> or <code>ProvisionedThroughput</code> based on your table's
     * capacity mode.
     */
    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    /**
     * Represents the warm throughput value (in read units per second and write units per second) when creating a
     * secondary index.
     */
    public WarmThroughput getWarmThroughput() {
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
        CreateGlobalSecondaryIndexAction that = (CreateGlobalSecondaryIndexAction) other;
        return Objects.equals(this.indexName, that.indexName)
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
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
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
        serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        serializer.writeList($SCHEMA_KEY_SCHEMA, keySchema, keySchema.size(), SharedSerde.KeySchemaSerializer.INSTANCE);
        if (projection != null) {
            serializer.writeStruct($SCHEMA_PROJECTION, projection);
        }
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
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
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateGlobalSecondaryIndexAction}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.keySchema(this.keySchema);
        builder.projection(this.projection);
        builder.provisionedThroughput(this.provisionedThroughput);
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
     * Builder for {@link CreateGlobalSecondaryIndexAction}.
     */
    public static final class Builder implements ShapeBuilder<CreateGlobalSecondaryIndexAction> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String indexName;
        private List<KeySchemaElement> keySchema;
        private Projection projection;
        private ProvisionedThroughput provisionedThroughput;
        private OnDemandThroughput onDemandThroughput;
        private WarmThroughput warmThroughput;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global secondary index to be created.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = Objects.requireNonNull(indexName, "indexName cannot be null");
            tracker.setMember($SCHEMA_INDEX_NAME);
            return this;
        }

        /**
         * The key schema for the global secondary index. Global secondary index supports up to 4 partition and up to 4 sort
         * keys.
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
         * Represents attributes that are copied (projected) from the table into an index. These are in addition to the
         * primary key attributes and index key attributes, which are automatically projected.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder projection(Projection projection) {
            this.projection = Objects.requireNonNull(projection, "projection cannot be null");
            tracker.setMember($SCHEMA_PROJECTION);
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
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * The maximum number of read and write units for the global secondary index being created. If you use this
         * parameter, you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both. You
         * must use either <code>OnDemand Throughput</code> or <code>ProvisionedThroughput</code> based on your table's
         * capacity mode.
         *
         * @return this builder.
         */
        public Builder onDemandThroughput(OnDemandThroughput onDemandThroughput) {
            this.onDemandThroughput = onDemandThroughput;
            return this;
        }

        /**
         * Represents the warm throughput value (in read units per second and write units per second) when creating a
         * secondary index.
         *
         * @return this builder.
         */
        public Builder warmThroughput(WarmThroughput warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        @Override
        public CreateGlobalSecondaryIndexAction build() {
            tracker.validate();
            return new CreateGlobalSecondaryIndexAction(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 2 -> projection((Projection) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, value));
                case 3 -> provisionedThroughput((ProvisionedThroughput) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 4 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 5 -> warmThroughput((WarmThroughput) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateGlobalSecondaryIndexAction> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_INDEX_NAME)) {
                indexName("");
            }
            if (!tracker.checkMember($SCHEMA_KEY_SCHEMA)) {
                keySchema(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_PROJECTION)) {
                tracker.setMember($SCHEMA_PROJECTION);
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.keySchema(SharedSerde.deserializeKeySchema(member, de));
                    case 2 -> builder.projection(Projection.builder().deserializeMember(de, member).build());
                    case 3 -> builder.provisionedThroughput(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 4 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 5 -> builder.warmThroughput(WarmThroughput.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
