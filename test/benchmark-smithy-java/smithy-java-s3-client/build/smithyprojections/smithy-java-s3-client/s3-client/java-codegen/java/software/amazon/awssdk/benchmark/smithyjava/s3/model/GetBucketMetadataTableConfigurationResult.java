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
public final class GetBucketMetadataTableConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_METADATA_TABLE_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT = $SCHEMA.member("MetadataTableConfigurationResult");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_ERROR = $SCHEMA.member("Error");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient MetadataTableConfigurationResult metadataTableConfigurationResult;
    private final transient String status;
    private final transient ErrorDetails error;

    private GetBucketMetadataTableConfigurationResult(Builder builder) {
        this.metadataTableConfigurationResult = builder.metadataTableConfigurationResult;
        this.status = builder.status;
        this.error = builder.error;
    }

    /**
     * The V1 S3 Metadata configuration for a general purpose bucket.
     */
    public MetadataTableConfigurationResult getMetadataTableConfigurationResult() {
        return metadataTableConfigurationResult;
    }

    /**
     * The status of the metadata table. The status values are:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The metadata table is in the process of being created in the specified table
     *     bucket.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The metadata table has been created successfully, and records are being delivered
     *     to the table.
     *   </li>
     *   <li>
     *     <code>FAILED</code> - Amazon S3 is unable to create the metadata table, or Amazon S3 is unable to deliver
     *     records. See <code>ErrorDetails</code> for details.
     *   </li>
     * </ul>
     */
    public String getStatus() {
        return status;
    }

    /**
     * If the <code>CreateBucketMetadataTableConfiguration</code> request succeeds, but S3 Metadata was unable to create
     * the table, this structure contains the error code and error message.
     */
    public ErrorDetails getError() {
        return error;
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
        GetBucketMetadataTableConfigurationResult that = (GetBucketMetadataTableConfigurationResult) other;
        return Objects.equals(this.status, that.status)
               && Objects.equals(this.metadataTableConfigurationResult, that.metadataTableConfigurationResult)
               && Objects.equals(this.error, that.error);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metadataTableConfigurationResult);
        $hc = 31 * $hc + Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(error);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metadataTableConfigurationResult != null) {
            serializer.writeStruct($SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT, metadataTableConfigurationResult);
        }
        serializer.writeString($SCHEMA_STATUS, status);
        if (error != null) {
            serializer.writeStruct($SCHEMA_ERROR, error);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT, member, metadataTableConfigurationResult);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, error);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketMetadataTableConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metadataTableConfigurationResult(this.metadataTableConfigurationResult);
        builder.status(this.status);
        builder.error(this.error);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketMetadataTableConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketMetadataTableConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private MetadataTableConfigurationResult metadataTableConfigurationResult;
        private String status;
        private ErrorDetails error;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The V1 S3 Metadata configuration for a general purpose bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder metadataTableConfigurationResult(MetadataTableConfigurationResult metadataTableConfigurationResult) {
            this.metadataTableConfigurationResult = Objects.requireNonNull(metadataTableConfigurationResult, "metadataTableConfigurationResult cannot be null");
            tracker.setMember($SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT);
            return this;
        }

        /**
         * The status of the metadata table. The status values are:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The metadata table is in the process of being created in the specified table
         *     bucket.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The metadata table has been created successfully, and records are being delivered
         *     to the table.
         *   </li>
         *   <li>
         *     <code>FAILED</code> - Amazon S3 is unable to create the metadata table, or Amazon S3 is unable to deliver
         *     records. See <code>ErrorDetails</code> for details.
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(String status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        /**
         * If the <code>CreateBucketMetadataTableConfiguration</code> request succeeds, but S3 Metadata was unable to create
         * the table, this structure contains the error code and error message.
         *
         * @return this builder.
         */
        public Builder error(ErrorDetails error) {
            this.error = error;
            return this;
        }

        @Override
        public GetBucketMetadataTableConfigurationResult build() {
            tracker.validate();
            return new GetBucketMetadataTableConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metadataTableConfigurationResult((MetadataTableConfigurationResult) SchemaUtils.validateSameMember($SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT, member, value));
                case 1 -> status((String) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 2 -> error((ErrorDetails) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetBucketMetadataTableConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT)) {
                tracker.setMember($SCHEMA_METADATA_TABLE_CONFIGURATION_RESULT);
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status("");
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
                    case 0 -> builder.metadataTableConfigurationResult(MetadataTableConfigurationResult.builder().deserializeMember(de, member).build());
                    case 1 -> builder.status(de.readString(member));
                    case 2 -> builder.error(ErrorDetails.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
