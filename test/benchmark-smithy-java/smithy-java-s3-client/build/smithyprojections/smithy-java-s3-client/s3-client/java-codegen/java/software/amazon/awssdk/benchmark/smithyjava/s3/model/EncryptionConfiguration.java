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
 * Specifies encryption-related information for an Amazon S3 bucket that is a destination for replicated objects.
 *
 * <p>If you're specifying a customer managed KMS key, we recommend using a fully qualified KMS key ARN. If you use a
 * KMS key alias instead, then KMS resolves the key within the requester’s account. This behavior can result in data
 * that's encrypted with a KMS key that belongs to the requester, and not the bucket owner.
 */
@SmithyGenerated
public final class EncryptionConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.ENCRYPTION_CONFIGURATION;
    private static final Schema $SCHEMA_REPLICA_KMS_KEYI_D = $SCHEMA.member("ReplicaKmsKeyID");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String replicaKmsKeyiD;

    private EncryptionConfiguration(Builder builder) {
        this.replicaKmsKeyiD = builder.replicaKmsKeyiD;
    }

    /**
     * Specifies the ID (Key ARN or Alias ARN) of the customer managed Amazon Web Services KMS key stored in Amazon Web
     * Services Key Management Service (KMS) for the destination bucket. Amazon S3 uses this key to encrypt replica
     * objects. Amazon S3 only supports symmetric encryption KMS keys. For more information, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/symmetric-asymmetric.html">Asymmetric keys in
     * Amazon Web Services KMS</a> in the <i>Amazon Web Services Key Management Service Developer Guide</i>.
     */
    public String getReplicaKmsKeyiD() {
        return replicaKmsKeyiD;
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
        EncryptionConfiguration that = (EncryptionConfiguration) other;
        return Objects.equals(this.replicaKmsKeyiD, that.replicaKmsKeyiD);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(replicaKmsKeyiD);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (replicaKmsKeyiD != null) {
            serializer.writeString($SCHEMA_REPLICA_KMS_KEYI_D, replicaKmsKeyiD);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_KMS_KEYI_D, member, replicaKmsKeyiD);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link EncryptionConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.replicaKmsKeyiD(this.replicaKmsKeyiD);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EncryptionConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<EncryptionConfiguration> {
        private String replicaKmsKeyiD;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the ID (Key ARN or Alias ARN) of the customer managed Amazon Web Services KMS key stored in Amazon Web
         * Services Key Management Service (KMS) for the destination bucket. Amazon S3 uses this key to encrypt replica
         * objects. Amazon S3 only supports symmetric encryption KMS keys. For more information, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/symmetric-asymmetric.html">Asymmetric keys in
         * Amazon Web Services KMS</a> in the <i>Amazon Web Services Key Management Service Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder replicaKmsKeyiD(String replicaKmsKeyiD) {
            this.replicaKmsKeyiD = replicaKmsKeyiD;
            return this;
        }

        @Override
        public EncryptionConfiguration build() {
            return new EncryptionConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> replicaKmsKeyiD((String) SchemaUtils.validateSameMember($SCHEMA_REPLICA_KMS_KEYI_D, member, value));
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
                    case 0 -> builder.replicaKmsKeyiD(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
