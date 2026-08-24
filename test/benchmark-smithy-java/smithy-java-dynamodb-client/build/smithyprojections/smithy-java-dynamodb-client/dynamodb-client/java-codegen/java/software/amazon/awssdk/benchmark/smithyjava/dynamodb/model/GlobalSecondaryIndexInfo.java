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
 * Represents the properties of a global secondary index for the table when the backup was created.
 */
@SmithyGenerated
public final class GlobalSecondaryIndexInfo implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_SECONDARY_INDEX_INFO;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_PROJECTION = $SCHEMA.member("Projection");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient List<KeySchemaElement> keySchema;
    private final transient Projection projection;
    private final transient ProvisionedThroughput provisionedThroughput;
    private final transient OnDemandThroughput onDemandThroughput;

    private GlobalSecondaryIndexInfo(Builder builder) {
        this.indexName = builder.indexName;
        this.keySchema = builder.keySchema == null ? null : Collections.unmodifiableList(builder.keySchema);
        this.projection = builder.projection;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.onDemandThroughput = builder.onDemandThroughput;
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
     * Represents the provisioned throughput settings for the specified global secondary index.
     */
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
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
        GlobalSecondaryIndexInfo that = (GlobalSecondaryIndexInfo) other;
        return Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.projection, that.projection)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.keySchema, that.keySchema);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(projection);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
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
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
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
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalSecondaryIndexInfo}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.keySchema(this.keySchema);
        builder.projection(this.projection);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.onDemandThroughput(this.onDemandThroughput);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalSecondaryIndexInfo}.
     */
    public static final class Builder implements ShapeBuilder<GlobalSecondaryIndexInfo> {
        private String indexName;
        private List<KeySchemaElement> keySchema;
        private Projection projection;
        private ProvisionedThroughput provisionedThroughput;
        private OnDemandThroughput onDemandThroughput;

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
         * Represents the provisioned throughput settings for the specified global secondary index.
         *
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

        @Override
        public GlobalSecondaryIndexInfo build() {
            return new GlobalSecondaryIndexInfo(this);
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
                    case 3 -> builder.provisionedThroughput(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 4 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
