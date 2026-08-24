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
 * Represents the properties of a local secondary index.
 */
@SmithyGenerated
public final class LocalSecondaryIndexDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LOCAL_SECONDARY_INDEX_DESCRIPTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_KEY_SCHEMA = $SCHEMA.member("KeySchema");
    private static final Schema $SCHEMA_PROJECTION = $SCHEMA.member("Projection");
    private static final Schema $SCHEMA_INDEX_SIZE_BYTES = $SCHEMA.member("IndexSizeBytes");
    private static final Schema $SCHEMA_ITEM_COUNT = $SCHEMA.member("ItemCount");
    private static final Schema $SCHEMA_INDEX_ARN = $SCHEMA.member("IndexArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient List<KeySchemaElement> keySchema;
    private final transient Projection projection;
    private final transient Long indexSizeBytes;
    private final transient Long itemCount;
    private final transient String indexArn;

    private LocalSecondaryIndexDescription(Builder builder) {
        this.indexName = builder.indexName;
        this.keySchema = builder.keySchema == null ? null : Collections.unmodifiableList(builder.keySchema);
        this.projection = builder.projection;
        this.indexSizeBytes = builder.indexSizeBytes;
        this.itemCount = builder.itemCount;
        this.indexArn = builder.indexArn;
    }

    /**
     * Represents the name of the local secondary index.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The complete key schema for the local secondary index, consisting of one or more pairs of attribute names and key
     * types:
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
        LocalSecondaryIndexDescription that = (LocalSecondaryIndexDescription) other;
        return Objects.equals(this.indexSizeBytes, that.indexSizeBytes)
               && Objects.equals(this.itemCount, that.itemCount)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.indexArn, that.indexArn)
               && Objects.equals(this.projection, that.projection)
               && Objects.equals(this.keySchema, that.keySchema);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(keySchema);
        $hc = 31 * $hc + Objects.hashCode(projection);
        $hc = 31 * $hc + Objects.hashCode(indexSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(itemCount);
        $hc = 31 * $hc + Objects.hashCode(indexArn);
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
        if (indexSizeBytes != null) {
            serializer.writeLong($SCHEMA_INDEX_SIZE_BYTES, indexSizeBytes);
        }
        if (itemCount != null) {
            serializer.writeLong($SCHEMA_ITEM_COUNT, itemCount);
        }
        if (indexArn != null) {
            serializer.writeString($SCHEMA_INDEX_ARN, indexArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, keySchema);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, projection);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_SIZE_BYTES, member, indexSizeBytes);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, itemCount);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_ARN, member, indexArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LocalSecondaryIndexDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.keySchema(this.keySchema);
        builder.projection(this.projection);
        builder.indexSizeBytes(this.indexSizeBytes);
        builder.itemCount(this.itemCount);
        builder.indexArn(this.indexArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LocalSecondaryIndexDescription}.
     */
    public static final class Builder implements ShapeBuilder<LocalSecondaryIndexDescription> {
        private String indexName;
        private List<KeySchemaElement> keySchema;
        private Projection projection;
        private Long indexSizeBytes;
        private Long itemCount;
        private String indexArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the name of the local secondary index.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The complete key schema for the local secondary index, consisting of one or more pairs of attribute names and key
         * types:
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

        @Override
        public LocalSecondaryIndexDescription build() {
            return new LocalSecondaryIndexDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> keySchema((List<KeySchemaElement>) SchemaUtils.validateSameMember($SCHEMA_KEY_SCHEMA, member, value));
                case 2 -> projection((Projection) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, value));
                case 3 -> indexSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_INDEX_SIZE_BYTES, member, value));
                case 4 -> itemCount((Long) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, value));
                case 5 -> indexArn((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_ARN, member, value));
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
                    case 3 -> builder.indexSizeBytes(de.readLong(member));
                    case 4 -> builder.itemCount(de.readLong(member));
                    case 5 -> builder.indexArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
