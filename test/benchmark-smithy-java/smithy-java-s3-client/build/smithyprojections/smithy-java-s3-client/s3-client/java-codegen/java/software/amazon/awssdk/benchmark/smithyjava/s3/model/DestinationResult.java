package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * The destination information for the S3 Metadata configuration.
 */
@SmithyGenerated
public final class DestinationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.DESTINATION_RESULT;
    private static final Schema $SCHEMA_TABLE_BUCKET_TYPE = $SCHEMA.member("TableBucketType");
    private static final Schema $SCHEMA_TABLE_BUCKET_ARN = $SCHEMA.member("TableBucketArn");
    private static final Schema $SCHEMA_TABLE_NAMESPACE = $SCHEMA.member("TableNamespace");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient S3TablesBucketType tableBucketType;
    private final transient String tableBucketArn;
    private final transient String tableNamespace;

    private DestinationResult(Builder builder) {
        this.tableBucketType = builder.tableBucketType;
        this.tableBucketArn = builder.tableBucketArn;
        this.tableNamespace = builder.tableNamespace;
    }

    /**
     * The type of the table bucket where the metadata configuration is stored. The <code>aws</code> value indicates an
     * Amazon Web Services managed table bucket, and the <code>customer</code> value indicates a customer-managed table
     * bucket. V2 metadata configurations are stored in Amazon Web Services managed table buckets, and V1 metadata
     * configurations are stored in customer-managed table buckets.
     */
    public S3TablesBucketType getTableBucketType() {
        return tableBucketType;
    }

    /**
     * The Amazon Resource Name (ARN) of the table bucket where the metadata configuration is stored.
     */
    public String getTableBucketArn() {
        return tableBucketArn;
    }

    /**
     * The namespace in the table bucket where the metadata tables for a metadata configuration are stored.
     */
    public String getTableNamespace() {
        return tableNamespace;
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
        DestinationResult that = (DestinationResult) other;
        return Objects.equals(this.tableBucketArn, that.tableBucketArn)
               && Objects.equals(this.tableNamespace, that.tableNamespace)
               && Objects.equals(this.tableBucketType, that.tableBucketType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableBucketType);
        $hc = 31 * $hc + Objects.hashCode(tableBucketArn);
        $hc = 31 * $hc + Objects.hashCode(tableNamespace);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableBucketType != null) {
            serializer.writeString($SCHEMA_TABLE_BUCKET_TYPE, tableBucketType.getValue());
        }
        if (tableBucketArn != null) {
            serializer.writeString($SCHEMA_TABLE_BUCKET_ARN, tableBucketArn);
        }
        if (tableNamespace != null) {
            serializer.writeString($SCHEMA_TABLE_NAMESPACE, tableNamespace);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_BUCKET_TYPE, member, tableBucketType);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_BUCKET_ARN, member, tableBucketArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAMESPACE, member, tableNamespace);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DestinationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableBucketType(this.tableBucketType);
        builder.tableBucketArn(this.tableBucketArn);
        builder.tableNamespace(this.tableNamespace);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DestinationResult}.
     */
    public static final class Builder implements ShapeBuilder<DestinationResult> {
        private S3TablesBucketType tableBucketType;
        private String tableBucketArn;
        private String tableNamespace;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The type of the table bucket where the metadata configuration is stored. The <code>aws</code> value indicates an
         * Amazon Web Services managed table bucket, and the <code>customer</code> value indicates a customer-managed table
         * bucket. V2 metadata configurations are stored in Amazon Web Services managed table buckets, and V1 metadata
         * configurations are stored in customer-managed table buckets.
         *
         * @return this builder.
         */
        public Builder tableBucketType(S3TablesBucketType tableBucketType) {
            this.tableBucketType = tableBucketType;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the table bucket where the metadata configuration is stored.
         *
         * @return this builder.
         */
        public Builder tableBucketArn(String tableBucketArn) {
            this.tableBucketArn = tableBucketArn;
            return this;
        }

        /**
         * The namespace in the table bucket where the metadata tables for a metadata configuration are stored.
         *
         * @return this builder.
         */
        public Builder tableNamespace(String tableNamespace) {
            this.tableNamespace = tableNamespace;
            return this;
        }

        @Override
        public DestinationResult build() {
            return new DestinationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableBucketType((S3TablesBucketType) SchemaUtils.validateSameMember($SCHEMA_TABLE_BUCKET_TYPE, member, value));
                case 1 -> tableBucketArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_BUCKET_ARN, member, value));
                case 2 -> tableNamespace((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAMESPACE, member, value));
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
                    case 0 -> builder.tableBucketType(S3TablesBucketType.builder().deserializeMember(de, member).build());
                    case 1 -> builder.tableBucketArn(de.readString(member));
                    case 2 -> builder.tableNamespace(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
