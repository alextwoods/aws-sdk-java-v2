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
 * The V1 S3 Metadata configuration for a general purpose bucket. The destination table bucket must be in the same
 * Region and Amazon Web Services account as the general purpose bucket. The specified metadata table name must be
 * unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
 *
 * <p>If you created your S3 Metadata configuration before July 15, 2025, we recommend that you delete and re-create
 * your configuration by using <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a> so that you can expire journal table records and
 * create a live inventory table.
 */
@SmithyGenerated
public final class MetadataTableConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.METADATA_TABLE_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_S3_TABLES_DESTINATION_RESULT = $SCHEMA.member("S3TablesDestinationResult");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient S3TablesDestinationResult s3TablesDestinationResult;

    private MetadataTableConfigurationResult(Builder builder) {
        this.s3TablesDestinationResult = builder.s3TablesDestinationResult;
    }

    /**
     * The destination information for the metadata table configuration. The destination table bucket must be in the
     * same Region and Amazon Web Services account as the general purpose bucket. The specified metadata table name must
     * be unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
     */
    public S3TablesDestinationResult getS3TablesDestinationResult() {
        return s3TablesDestinationResult;
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
        MetadataTableConfigurationResult that = (MetadataTableConfigurationResult) other;
        return Objects.equals(this.s3TablesDestinationResult, that.s3TablesDestinationResult);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(s3TablesDestinationResult);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (s3TablesDestinationResult != null) {
            serializer.writeStruct($SCHEMA_S3_TABLES_DESTINATION_RESULT, s3TablesDestinationResult);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_TABLES_DESTINATION_RESULT, member, s3TablesDestinationResult);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetadataTableConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.s3TablesDestinationResult(this.s3TablesDestinationResult);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetadataTableConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<MetadataTableConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private S3TablesDestinationResult s3TablesDestinationResult;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The destination information for the metadata table configuration. The destination table bucket must be in the
         * same Region and Amazon Web Services account as the general purpose bucket. The specified metadata table name must
         * be unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder s3TablesDestinationResult(S3TablesDestinationResult s3TablesDestinationResult) {
            this.s3TablesDestinationResult = Objects.requireNonNull(s3TablesDestinationResult, "s3TablesDestinationResult cannot be null");
            tracker.setMember($SCHEMA_S3_TABLES_DESTINATION_RESULT);
            return this;
        }

        @Override
        public MetadataTableConfigurationResult build() {
            tracker.validate();
            return new MetadataTableConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s3TablesDestinationResult((S3TablesDestinationResult) SchemaUtils.validateSameMember($SCHEMA_S3_TABLES_DESTINATION_RESULT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetadataTableConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_S3_TABLES_DESTINATION_RESULT)) {
                tracker.setMember($SCHEMA_S3_TABLES_DESTINATION_RESULT);
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
                    case 0 -> builder.s3TablesDestinationResult(S3TablesDestinationResult.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
