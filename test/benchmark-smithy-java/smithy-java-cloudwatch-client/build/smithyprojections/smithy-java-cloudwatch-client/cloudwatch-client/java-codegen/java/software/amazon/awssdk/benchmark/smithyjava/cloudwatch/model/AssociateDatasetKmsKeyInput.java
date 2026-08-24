package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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

@SmithyGenerated
public final class AssociateDatasetKmsKeyInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ASSOCIATE_DATASET_KMS_KEY_INPUT;
    private static final Schema $SCHEMA_DATASET_IDENTIFIER = $SCHEMA.member("DatasetIdentifier");
    private static final Schema $SCHEMA_KMS_KEY_ARN = $SCHEMA.member("KmsKeyArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String datasetIdentifier;
    private final transient String kmsKeyArn;

    private AssociateDatasetKmsKeyInput(Builder builder) {
        this.datasetIdentifier = builder.datasetIdentifier;
        this.kmsKeyArn = builder.kmsKeyArn;
    }

    /**
     * Specifies the identifier of the dataset that you want to associate the KMS key with. For the <code>default</code>
     * dataset, you can specify either <code>default</code> or the full dataset Amazon Resource Name (ARN) in the format
     * <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>:dataset/default</code>.
     */
    public String getDatasetIdentifier() {
        return datasetIdentifier;
    }

    /**
     * Specifies the Amazon Resource Name (ARN) of the customer managed KMS key to associate with the dataset. The key
     * must be a symmetric encryption KMS key (<code>SYMMETRIC_DEFAULT</code>) in the same Amazon Web Services Region as
     * the dataset.
     *
     * <p>The ARN must be in the format <code>arn:aws:kms:<i>Region</i>:<i>account-id</i>:key/<i>key-id</i></code>. Key
     * IDs, aliases, and alias ARNs are not accepted.
     *
     * <p>For more information about KMS key ARNs, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#key-id-key-ARN">Key ARN</a> in the <i>Amazon Web Services Key Management
     * Service Developer Guide</i>.
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
        AssociateDatasetKmsKeyInput that = (AssociateDatasetKmsKeyInput) other;
        return Objects.equals(this.datasetIdentifier, that.datasetIdentifier)
               && Objects.equals(this.kmsKeyArn, that.kmsKeyArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(datasetIdentifier);
        $hc = 31 * $hc + Objects.hashCode(kmsKeyArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (datasetIdentifier != null) {
            serializer.writeString($SCHEMA_DATASET_IDENTIFIER, datasetIdentifier);
        }
        if (kmsKeyArn != null) {
            serializer.writeString($SCHEMA_KMS_KEY_ARN, kmsKeyArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATASET_IDENTIFIER, member, datasetIdentifier);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, kmsKeyArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AssociateDatasetKmsKeyInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.datasetIdentifier(this.datasetIdentifier);
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
     * Builder for {@link AssociateDatasetKmsKeyInput}.
     */
    public static final class Builder implements ShapeBuilder<AssociateDatasetKmsKeyInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String datasetIdentifier;
        private String kmsKeyArn;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_DATASET_IDENTIFIER);
            tracker.setMember($SCHEMA_KMS_KEY_ARN);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the identifier of the dataset that you want to associate the KMS key with. For the <code>default</code>
         * dataset, you can specify either <code>default</code> or the full dataset Amazon Resource Name (ARN) in the format
         * <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>:dataset/default</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder datasetIdentifier(String datasetIdentifier) {
            this.datasetIdentifier = Objects.requireNonNull(datasetIdentifier, "datasetIdentifier cannot be null");
            tracker.setMember($SCHEMA_DATASET_IDENTIFIER);
            return this;
        }

        /**
         * Specifies the Amazon Resource Name (ARN) of the customer managed KMS key to associate with the dataset. The key
         * must be a symmetric encryption KMS key (<code>SYMMETRIC_DEFAULT</code>) in the same Amazon Web Services Region as
         * the dataset.
         *
         * <p>The ARN must be in the format <code>arn:aws:kms:<i>Region</i>:<i>account-id</i>:key/<i>key-id</i></code>. Key
         * IDs, aliases, and alias ARNs are not accepted.
         *
         * <p>For more information about KMS key ARNs, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#key-id-key-ARN">Key ARN</a> in the <i>Amazon Web Services Key Management
         * Service Developer Guide</i>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder kmsKeyArn(String kmsKeyArn) {
            this.kmsKeyArn = Objects.requireNonNull(kmsKeyArn, "kmsKeyArn cannot be null");
            tracker.setMember($SCHEMA_KMS_KEY_ARN);
            return this;
        }

        @Override
        public AssociateDatasetKmsKeyInput build() {
            tracker.validate();
            return new AssociateDatasetKmsKeyInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> datasetIdentifier((String) SchemaUtils.validateSameMember($SCHEMA_DATASET_IDENTIFIER, member, value));
                case 1 -> kmsKeyArn((String) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AssociateDatasetKmsKeyInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DATASET_IDENTIFIER)) {
                datasetIdentifier("");
            }
            if (!tracker.checkMember($SCHEMA_KMS_KEY_ARN)) {
                kmsKeyArn("");
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
                    case 0 -> builder.datasetIdentifier(de.readString(member));
                    case 1 -> builder.kmsKeyArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
