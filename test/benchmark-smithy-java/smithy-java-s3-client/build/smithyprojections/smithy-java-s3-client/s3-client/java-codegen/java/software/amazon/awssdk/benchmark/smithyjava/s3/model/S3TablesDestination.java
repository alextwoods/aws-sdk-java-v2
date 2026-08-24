package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * The destination information for a V1 S3 Metadata configuration. The destination table bucket must be in the same
 * Region and Amazon Web Services account as the general purpose bucket. The specified metadata table name must be
 * unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
 *
 * <p>If you created your S3 Metadata configuration before July 15, 2025, we recommend that you delete and re-create
 * your configuration by using <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a> so that you can expire journal table records and
 * create a live inventory table.
 */
@SmithyGenerated
public final class S3TablesDestination implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.S3_TABLES_DESTINATION;
    private static final Schema $SCHEMA_TABLE_BUCKET_ARN = $SCHEMA.member("TableBucketArn");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableBucketArn;
    private final transient String tableName;

    private S3TablesDestination(Builder builder) {
        this.tableBucketArn = builder.tableBucketArn;
        this.tableName = builder.tableName;
    }

    /**
     * The Amazon Resource Name (ARN) for the table bucket that's specified as the destination in the metadata table
     * configuration. The destination table bucket must be in the same Region and Amazon Web Services account as the
     * general purpose bucket.
     */
    public String getTableBucketArn() {
        return tableBucketArn;
    }

    /**
     * The name for the metadata table in your metadata table configuration. The specified metadata table name must be
     * unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
     */
    public String getTableName() {
        return tableName;
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
        S3TablesDestination that = (S3TablesDestination) other;
        return Objects.equals(this.tableBucketArn, that.tableBucketArn)
               && Objects.equals(this.tableName, that.tableName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableBucketArn);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_BUCKET_ARN, tableBucketArn);
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_BUCKET_ARN, member, tableBucketArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link S3TablesDestination}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableBucketArn(this.tableBucketArn);
        builder.tableName(this.tableName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link S3TablesDestination}.
     */
    public static final class Builder implements ShapeBuilder<S3TablesDestination> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableBucketArn;
        private String tableName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) for the table bucket that's specified as the destination in the metadata table
         * configuration. The destination table bucket must be in the same Region and Amazon Web Services account as the
         * general purpose bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableBucketArn(String tableBucketArn) {
            this.tableBucketArn = Objects.requireNonNull(tableBucketArn, "tableBucketArn cannot be null");
            tracker.setMember($SCHEMA_TABLE_BUCKET_ARN);
            return this;
        }

        /**
         * The name for the metadata table in your metadata table configuration. The specified metadata table name must be
         * unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        @Override
        public S3TablesDestination build() {
            tracker.validate();
            return new S3TablesDestination(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableBucketArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_BUCKET_ARN, member, value));
                case 1 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<S3TablesDestination> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_BUCKET_ARN)) {
                tableBucketArn("");
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
                    case 0 -> builder.tableBucketArn(de.readString(member));
                    case 1 -> builder.tableName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
