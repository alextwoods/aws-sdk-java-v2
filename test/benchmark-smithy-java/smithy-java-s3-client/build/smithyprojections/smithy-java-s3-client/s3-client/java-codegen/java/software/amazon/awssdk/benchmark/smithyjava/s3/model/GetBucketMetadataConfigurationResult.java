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
 * The S3 Metadata configuration for a general purpose bucket.
 */
@SmithyGenerated
public final class GetBucketMetadataConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_METADATA_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_METADATA_CONFIGURATION_RESULT = $SCHEMA.member("MetadataConfigurationResult");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient MetadataConfigurationResult metadataConfigurationResult;

    private GetBucketMetadataConfigurationResult(Builder builder) {
        this.metadataConfigurationResult = builder.metadataConfigurationResult;
    }

    /**
     * The metadata configuration for a general purpose bucket.
     */
    public MetadataConfigurationResult getMetadataConfigurationResult() {
        return metadataConfigurationResult;
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
        GetBucketMetadataConfigurationResult that = (GetBucketMetadataConfigurationResult) other;
        return Objects.equals(this.metadataConfigurationResult, that.metadataConfigurationResult);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metadataConfigurationResult);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metadataConfigurationResult != null) {
            serializer.writeStruct($SCHEMA_METADATA_CONFIGURATION_RESULT, metadataConfigurationResult);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA_CONFIGURATION_RESULT, member, metadataConfigurationResult);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketMetadataConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metadataConfigurationResult(this.metadataConfigurationResult);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketMetadataConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketMetadataConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private MetadataConfigurationResult metadataConfigurationResult;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metadata configuration for a general purpose bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder metadataConfigurationResult(MetadataConfigurationResult metadataConfigurationResult) {
            this.metadataConfigurationResult = Objects.requireNonNull(metadataConfigurationResult, "metadataConfigurationResult cannot be null");
            tracker.setMember($SCHEMA_METADATA_CONFIGURATION_RESULT);
            return this;
        }

        @Override
        public GetBucketMetadataConfigurationResult build() {
            tracker.validate();
            return new GetBucketMetadataConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metadataConfigurationResult((MetadataConfigurationResult) SchemaUtils.validateSameMember($SCHEMA_METADATA_CONFIGURATION_RESULT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetBucketMetadataConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_METADATA_CONFIGURATION_RESULT)) {
                tracker.setMember($SCHEMA_METADATA_CONFIGURATION_RESULT);
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
                    case 0 -> builder.metadataConfigurationResult(MetadataConfigurationResult.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
