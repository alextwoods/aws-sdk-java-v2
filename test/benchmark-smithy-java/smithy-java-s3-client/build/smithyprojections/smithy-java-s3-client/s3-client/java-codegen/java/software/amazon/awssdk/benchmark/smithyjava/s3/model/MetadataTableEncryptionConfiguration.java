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
 * The encryption settings for an S3 Metadata journal table or inventory table configuration.
 */
@SmithyGenerated
public final class MetadataTableEncryptionConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METADATA_TABLE_ENCRYPTION_CONFIGURATION;
    private static final Schema $SCHEMA_SSE_ALGORITHM = $SCHEMA.member("SseAlgorithm");
    private static final Schema $SCHEMA_KMS_KEY_ARN = $SCHEMA.member("KmsKeyArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TableSseAlgorithm sseAlgorithm;
    private final transient String kmsKeyArn;

    private MetadataTableEncryptionConfiguration(Builder builder) {
        this.sseAlgorithm = builder.sseAlgorithm;
        this.kmsKeyArn = builder.kmsKeyArn;
    }

    /**
     * The encryption type specified for a metadata table. To specify server-side encryption with Key Management Service
     * (KMS) keys (SSE-KMS), use the <code>aws:kms</code> value. To specify server-side encryption with Amazon S3
     * managed keys (SSE-S3), use the <code>AES256</code> value.
     */
    public TableSseAlgorithm getSseAlgorithm() {
        return sseAlgorithm;
    }

    /**
     * If server-side encryption with Key Management Service (KMS) keys (SSE-KMS) is specified, you must also specify
     * the KMS key Amazon Resource Name (ARN). You must specify a customer-managed KMS key that's located in the same
     * Region as the general purpose bucket that corresponds to the metadata table configuration.
     */
    public String getKmsKeyArn() {
        return kmsKeyArn;
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
        MetadataTableEncryptionConfiguration that = (MetadataTableEncryptionConfiguration) other;
        return Objects.equals(this.kmsKeyArn, that.kmsKeyArn)
               && Objects.equals(this.sseAlgorithm, that.sseAlgorithm);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sseAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(kmsKeyArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_SSE_ALGORITHM, sseAlgorithm.getValue());
        if (kmsKeyArn != null) {
            serializer.writeString($SCHEMA_KMS_KEY_ARN, kmsKeyArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_ALGORITHM, member, sseAlgorithm);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, kmsKeyArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetadataTableEncryptionConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sseAlgorithm(this.sseAlgorithm);
        builder.kmsKeyArn(this.kmsKeyArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetadataTableEncryptionConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<MetadataTableEncryptionConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private TableSseAlgorithm sseAlgorithm;
        private String kmsKeyArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The encryption type specified for a metadata table. To specify server-side encryption with Key Management Service
         * (KMS) keys (SSE-KMS), use the <code>aws:kms</code> value. To specify server-side encryption with Amazon S3
         * managed keys (SSE-S3), use the <code>AES256</code> value.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder sseAlgorithm(TableSseAlgorithm sseAlgorithm) {
            this.sseAlgorithm = Objects.requireNonNull(sseAlgorithm, "sseAlgorithm cannot be null");
            tracker.setMember($SCHEMA_SSE_ALGORITHM);
            return this;
        }

        /**
         * If server-side encryption with Key Management Service (KMS) keys (SSE-KMS) is specified, you must also specify
         * the KMS key Amazon Resource Name (ARN). You must specify a customer-managed KMS key that's located in the same
         * Region as the general purpose bucket that corresponds to the metadata table configuration.
         *
         * @return this builder.
         */
        public Builder kmsKeyArn(String kmsKeyArn) {
            this.kmsKeyArn = kmsKeyArn;
            return this;
        }

        @Override
        public MetadataTableEncryptionConfiguration build() {
            tracker.validate();
            return new MetadataTableEncryptionConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> sseAlgorithm((TableSseAlgorithm) SchemaUtils.validateSameMember($SCHEMA_SSE_ALGORITHM, member, value));
                case 1 -> kmsKeyArn((String) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetadataTableEncryptionConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_SSE_ALGORITHM)) {
                sseAlgorithm(TableSseAlgorithm.unknown(""));
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
                    case 0 -> builder.sseAlgorithm(TableSseAlgorithm.builder().deserializeMember(de, member).build());
                    case 1 -> builder.kmsKeyArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
