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
 * The V1 S3 Metadata configuration for a general purpose bucket.
 *
 * <p>If you created your S3 Metadata configuration before July 15, 2025, we recommend that you delete and re-create
 * your configuration by using <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a> so that you can expire journal table records and
 * create a live inventory table.
 */
@SmithyGenerated
public final class MetadataTableConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METADATA_TABLE_CONFIGURATION;
    private static final Schema $SCHEMA_S3_TABLES_DESTINATION = $SCHEMA.member("S3TablesDestination");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient S3TablesDestination s3TablesDestination;

    private MetadataTableConfiguration(Builder builder) {
        this.s3TablesDestination = builder.s3TablesDestination;
    }

    /**
     * The destination information for the metadata table configuration. The destination table bucket must be in the
     * same Region and Amazon Web Services account as the general purpose bucket. The specified metadata table name must
     * be unique within the <code>aws_s3_metadata</code> namespace in the destination table bucket.
     */
    public S3TablesDestination getS3TablesDestination() {
        return s3TablesDestination;
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
        MetadataTableConfiguration that = (MetadataTableConfiguration) other;
        return Objects.equals(this.s3TablesDestination, that.s3TablesDestination);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(s3TablesDestination);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (s3TablesDestination != null) {
            serializer.writeStruct($SCHEMA_S3_TABLES_DESTINATION, s3TablesDestination);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_TABLES_DESTINATION, member, s3TablesDestination);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetadataTableConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.s3TablesDestination(this.s3TablesDestination);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetadataTableConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<MetadataTableConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private S3TablesDestination s3TablesDestination;

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
        public Builder s3TablesDestination(S3TablesDestination s3TablesDestination) {
            this.s3TablesDestination = Objects.requireNonNull(s3TablesDestination, "s3TablesDestination cannot be null");
            tracker.setMember($SCHEMA_S3_TABLES_DESTINATION);
            return this;
        }

        @Override
        public MetadataTableConfiguration build() {
            tracker.validate();
            return new MetadataTableConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s3TablesDestination((S3TablesDestination) SchemaUtils.validateSameMember($SCHEMA_S3_TABLES_DESTINATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetadataTableConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_S3_TABLES_DESTINATION)) {
                tracker.setMember($SCHEMA_S3_TABLES_DESTINATION);
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
                    case 0 -> builder.s3TablesDestination(S3TablesDestination.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
