package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public final class ListBucketInventoryConfigurationsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_BUCKET_INVENTORY_CONFIGURATIONS_OUTPUT;
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_INVENTORY_CONFIGURATION_LIST = $SCHEMA.member("InventoryConfigurationList");
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_NEXT_CONTINUATION_TOKEN = $SCHEMA.member("NextContinuationToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String continuationToken;
    private final transient List<InventoryConfiguration> inventoryConfigurationList;
    private final transient Boolean isTruncated;
    private final transient String nextContinuationToken;

    private ListBucketInventoryConfigurationsOutput(Builder builder) {
        this.continuationToken = builder.continuationToken;
        this.inventoryConfigurationList = builder.inventoryConfigurationList == null ? null : Collections.unmodifiableList(builder.inventoryConfigurationList);
        this.isTruncated = builder.isTruncated;
        this.nextContinuationToken = builder.nextContinuationToken;
    }

    /**
     * If sent in the request, the marker that is used as a starting point for this inventory configuration list
     * response.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * The list of inventory configurations for a bucket.
     */
    public List<InventoryConfiguration> getInventoryConfigurationList() {
        if (inventoryConfigurationList == null) {
            return Collections.emptyList();
        }
        return inventoryConfigurationList;
    }

    public boolean hasInventoryConfigurationList() {
        return inventoryConfigurationList != null;
    }

    /**
     * Tells whether the returned list of inventory configurations is complete. A value of true indicates that the list
     * is not complete and the NextContinuationToken is provided for a subsequent request.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * The marker used to continue this inventory configuration listing. Use the <code>NextContinuationToken</code> from
     * this response to continue the listing in a subsequent request. The continuation token is an opaque value that
     * Amazon S3 understands.
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
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
        ListBucketInventoryConfigurationsOutput that = (ListBucketInventoryConfigurationsOutput) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.nextContinuationToken, that.nextContinuationToken)
               && Objects.equals(this.inventoryConfigurationList, that.inventoryConfigurationList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(inventoryConfigurationList);
        $hc = 31 * $hc + Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(nextContinuationToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (inventoryConfigurationList != null) {
            serializer.writeList($SCHEMA_INVENTORY_CONFIGURATION_LIST, inventoryConfigurationList, inventoryConfigurationList.size(), SharedSerde.InventoryConfigurationListSerializer.INSTANCE);
        }
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (nextContinuationToken != null) {
            serializer.writeString($SCHEMA_NEXT_CONTINUATION_TOKEN, nextContinuationToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_CONFIGURATION_LIST, member, inventoryConfigurationList);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, nextContinuationToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListBucketInventoryConfigurationsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.continuationToken(this.continuationToken);
        builder.inventoryConfigurationList(this.inventoryConfigurationList);
        builder.isTruncated(this.isTruncated);
        builder.nextContinuationToken(this.nextContinuationToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListBucketInventoryConfigurationsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListBucketInventoryConfigurationsOutput> {
        private String continuationToken;
        private List<InventoryConfiguration> inventoryConfigurationList;
        private Boolean isTruncated;
        private String nextContinuationToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If sent in the request, the marker that is used as a starting point for this inventory configuration list
         * response.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * The list of inventory configurations for a bucket.
         *
         * @return this builder.
         */
        public Builder inventoryConfigurationList(List<InventoryConfiguration> inventoryConfigurationList) {
            this.inventoryConfigurationList = inventoryConfigurationList;
            return this;
        }

        /**
         * Tells whether the returned list of inventory configurations is complete. A value of true indicates that the list
         * is not complete and the NextContinuationToken is provided for a subsequent request.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * The marker used to continue this inventory configuration listing. Use the <code>NextContinuationToken</code> from
         * this response to continue the listing in a subsequent request. The continuation token is an opaque value that
         * Amazon S3 understands.
         *
         * @return this builder.
         */
        public Builder nextContinuationToken(String nextContinuationToken) {
            this.nextContinuationToken = nextContinuationToken;
            return this;
        }

        @Override
        public ListBucketInventoryConfigurationsOutput build() {
            return new ListBucketInventoryConfigurationsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 1 -> inventoryConfigurationList((List<InventoryConfiguration>) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_CONFIGURATION_LIST, member, value));
                case 2 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 3 -> nextContinuationToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, value));
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
                    case 0 -> builder.continuationToken(de.readString(member));
                    case 1 -> builder.inventoryConfigurationList(SharedSerde.deserializeInventoryConfigurationList(member, de));
                    case 2 -> builder.isTruncated(de.readBoolean(member));
                    case 3 -> builder.nextContinuationToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
