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
public final class GetDatasetInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_DATASET_INPUT;
    private static final Schema $SCHEMA_DATASET_IDENTIFIER = $SCHEMA.member("DatasetIdentifier");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String datasetIdentifier;

    private GetDatasetInput(Builder builder) {
        this.datasetIdentifier = builder.datasetIdentifier;
    }

    /**
     * Specifies the identifier of the dataset to retrieve. For the <code>default</code> dataset, you can specify either
     * <code>default</code> or the full dataset Amazon Resource Name (ARN) in the format <code>arn:aws:cloudwatch:<i>
     * Region</i>:<i>account-id</i>:dataset/default</code>.
     */
    public String getDatasetIdentifier() {
        return datasetIdentifier;
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
        GetDatasetInput that = (GetDatasetInput) other;
        return Objects.equals(this.datasetIdentifier, that.datasetIdentifier);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(datasetIdentifier);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATASET_IDENTIFIER, member, datasetIdentifier);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetDatasetInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.datasetIdentifier(this.datasetIdentifier);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetDatasetInput}.
     */
    public static final class Builder implements ShapeBuilder<GetDatasetInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String datasetIdentifier;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_DATASET_IDENTIFIER);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the identifier of the dataset to retrieve. For the <code>default</code> dataset, you can specify either
         * <code>default</code> or the full dataset Amazon Resource Name (ARN) in the format <code>arn:aws:cloudwatch:<i>
         * Region</i>:<i>account-id</i>:dataset/default</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder datasetIdentifier(String datasetIdentifier) {
            this.datasetIdentifier = Objects.requireNonNull(datasetIdentifier, "datasetIdentifier cannot be null");
            tracker.setMember($SCHEMA_DATASET_IDENTIFIER);
            return this;
        }

        @Override
        public GetDatasetInput build() {
            tracker.validate();
            return new GetDatasetInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> datasetIdentifier((String) SchemaUtils.validateSameMember($SCHEMA_DATASET_IDENTIFIER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetDatasetInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DATASET_IDENTIFIER)) {
                datasetIdentifier("");
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
