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
public final class GetDatasetOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_DATASET_OUTPUT;
    private static final Schema $SCHEMA_DATASET_ID = $SCHEMA.member("DatasetId");
    private static final Schema $SCHEMA_ARN = $SCHEMA.member("Arn");
    private static final Schema $SCHEMA_KMS_KEY_ARN = $SCHEMA.member("KmsKeyArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String datasetId;
    private final transient String arn;
    private final transient String kmsKeyArn;

    private GetDatasetOutput(Builder builder) {
        this.datasetId = builder.datasetId;
        this.arn = builder.arn;
        this.kmsKeyArn = builder.kmsKeyArn;
    }

    /**
     * Returns the identifier of the dataset.
     */
    public String getDatasetId() {
        return datasetId;
    }

    /**
     * Returns the Amazon Resource Name (ARN) of the dataset, in the format <code>arn:aws:cloudwatch:<i>Region</i>:<i>
     * account-id</i>:dataset/<i>dataset-id</i></code>.
     */
    public String getArn() {
        return arn;
    }

    /**
     * Returns the Amazon Resource Name (ARN) of the customer managed Amazon Web Services KMS key that is currently
     * associated with the dataset, if any. If the dataset is not associated with a customer managed KMS key, this field
     * is not included in the response and the dataset is encrypted at rest using an Amazon Web Services owned key.
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
        GetDatasetOutput that = (GetDatasetOutput) other;
        return Objects.equals(this.datasetId, that.datasetId)
               && Objects.equals(this.arn, that.arn)
               && Objects.equals(this.kmsKeyArn, that.kmsKeyArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(datasetId);
        $hc = 31 * $hc + Objects.hashCode(arn);
        $hc = 31 * $hc + Objects.hashCode(kmsKeyArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (datasetId != null) {
            serializer.writeString($SCHEMA_DATASET_ID, datasetId);
        }
        if (arn != null) {
            serializer.writeString($SCHEMA_ARN, arn);
        }
        if (kmsKeyArn != null) {
            serializer.writeString($SCHEMA_KMS_KEY_ARN, kmsKeyArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATASET_ID, member, datasetId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARN, member, arn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, kmsKeyArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetDatasetOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.datasetId(this.datasetId);
        builder.arn(this.arn);
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
     * Builder for {@link GetDatasetOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetDatasetOutput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String datasetId;
        private String arn;
        private String kmsKeyArn;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_DATASET_ID);
            tracker.setMember($SCHEMA_ARN);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Returns the identifier of the dataset.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder datasetId(String datasetId) {
            this.datasetId = Objects.requireNonNull(datasetId, "datasetId cannot be null");
            tracker.setMember($SCHEMA_DATASET_ID);
            return this;
        }

        /**
         * Returns the Amazon Resource Name (ARN) of the dataset, in the format <code>arn:aws:cloudwatch:<i>Region</i>:<i>
         * account-id</i>:dataset/<i>dataset-id</i></code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder arn(String arn) {
            this.arn = Objects.requireNonNull(arn, "arn cannot be null");
            tracker.setMember($SCHEMA_ARN);
            return this;
        }

        /**
         * Returns the Amazon Resource Name (ARN) of the customer managed Amazon Web Services KMS key that is currently
         * associated with the dataset, if any. If the dataset is not associated with a customer managed KMS key, this field
         * is not included in the response and the dataset is encrypted at rest using an Amazon Web Services owned key.
         *
         * @return this builder.
         */
        public Builder kmsKeyArn(String kmsKeyArn) {
            this.kmsKeyArn = kmsKeyArn;
            return this;
        }

        @Override
        public GetDatasetOutput build() {
            tracker.validate();
            return new GetDatasetOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> datasetId((String) SchemaUtils.validateSameMember($SCHEMA_DATASET_ID, member, value));
                case 1 -> arn((String) SchemaUtils.validateSameMember($SCHEMA_ARN, member, value));
                case 2 -> kmsKeyArn((String) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetDatasetOutput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DATASET_ID)) {
                datasetId("");
            }
            if (!tracker.checkMember($SCHEMA_ARN)) {
                arn("");
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
                    case 0 -> builder.datasetId(de.readString(member));
                    case 1 -> builder.arn(de.readString(member));
                    case 2 -> builder.kmsKeyArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
