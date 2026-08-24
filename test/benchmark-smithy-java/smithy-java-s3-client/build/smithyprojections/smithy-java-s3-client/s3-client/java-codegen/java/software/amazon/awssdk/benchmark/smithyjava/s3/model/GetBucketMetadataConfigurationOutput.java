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

@SmithyGenerated
public final class GetBucketMetadataConfigurationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_METADATA_CONFIGURATION_OUTPUT;
    private static final Schema $SCHEMA_GET_BUCKET_METADATA_CONFIGURATION_RESULT = $SCHEMA.member("GetBucketMetadataConfigurationResult");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient GetBucketMetadataConfigurationResult getBucketMetadataConfigurationResult;

    private GetBucketMetadataConfigurationOutput(Builder builder) {
        this.getBucketMetadataConfigurationResult = builder.getBucketMetadataConfigurationResult;
    }

    /**
     * The metadata configuration for the general purpose bucket.
     */
    public GetBucketMetadataConfigurationResult getGetBucketMetadataConfigurationResult() {
        return getBucketMetadataConfigurationResult;
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
        GetBucketMetadataConfigurationOutput that = (GetBucketMetadataConfigurationOutput) other;
        return Objects.equals(this.getBucketMetadataConfigurationResult, that.getBucketMetadataConfigurationResult);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(getBucketMetadataConfigurationResult);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (getBucketMetadataConfigurationResult != null) {
            serializer.writeStruct($SCHEMA_GET_BUCKET_METADATA_CONFIGURATION_RESULT, getBucketMetadataConfigurationResult);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GET_BUCKET_METADATA_CONFIGURATION_RESULT, member, getBucketMetadataConfigurationResult);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketMetadataConfigurationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.getBucketMetadataConfigurationResult(this.getBucketMetadataConfigurationResult);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketMetadataConfigurationOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketMetadataConfigurationOutput> {
        private GetBucketMetadataConfigurationResult getBucketMetadataConfigurationResult;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metadata configuration for the general purpose bucket.
         *
         * @return this builder.
         */
        public Builder getBucketMetadataConfigurationResult(GetBucketMetadataConfigurationResult getBucketMetadataConfigurationResult) {
            this.getBucketMetadataConfigurationResult = getBucketMetadataConfigurationResult;
            return this;
        }

        @Override
        public GetBucketMetadataConfigurationOutput build() {
            return new GetBucketMetadataConfigurationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> getBucketMetadataConfigurationResult((GetBucketMetadataConfigurationResult) SchemaUtils.validateSameMember($SCHEMA_GET_BUCKET_METADATA_CONFIGURATION_RESULT, member, value));
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
                    case 0 -> builder.getBucketMetadataConfigurationResult(GetBucketMetadataConfigurationResult.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
