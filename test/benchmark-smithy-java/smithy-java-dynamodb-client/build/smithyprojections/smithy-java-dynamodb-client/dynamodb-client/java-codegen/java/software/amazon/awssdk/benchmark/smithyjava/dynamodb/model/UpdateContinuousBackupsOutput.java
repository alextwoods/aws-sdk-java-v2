package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class UpdateContinuousBackupsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_CONTINUOUS_BACKUPS_OUTPUT;
    private static final Schema $SCHEMA_CONTINUOUS_BACKUPS_DESCRIPTION = $SCHEMA.member("ContinuousBackupsDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ContinuousBackupsDescription continuousBackupsDescription;

    private UpdateContinuousBackupsOutput(Builder builder) {
        this.continuousBackupsDescription = builder.continuousBackupsDescription;
    }

    /**
     * Represents the continuous backups and point in time recovery settings on the table.
     */
    public ContinuousBackupsDescription getContinuousBackupsDescription() {
        return continuousBackupsDescription;
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
        UpdateContinuousBackupsOutput that = (UpdateContinuousBackupsOutput) other;
        return Objects.equals(this.continuousBackupsDescription, that.continuousBackupsDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(continuousBackupsDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (continuousBackupsDescription != null) {
            serializer.writeStruct($SCHEMA_CONTINUOUS_BACKUPS_DESCRIPTION, continuousBackupsDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUOUS_BACKUPS_DESCRIPTION, member, continuousBackupsDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateContinuousBackupsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.continuousBackupsDescription(this.continuousBackupsDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateContinuousBackupsOutput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateContinuousBackupsOutput> {
        private ContinuousBackupsDescription continuousBackupsDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the continuous backups and point in time recovery settings on the table.
         *
         * @return this builder.
         */
        public Builder continuousBackupsDescription(ContinuousBackupsDescription continuousBackupsDescription) {
            this.continuousBackupsDescription = continuousBackupsDescription;
            return this;
        }

        @Override
        public UpdateContinuousBackupsOutput build() {
            return new UpdateContinuousBackupsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> continuousBackupsDescription((ContinuousBackupsDescription) SchemaUtils.validateSameMember($SCHEMA_CONTINUOUS_BACKUPS_DESCRIPTION, member, value));
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
                    case 0 -> builder.continuousBackupsDescription(ContinuousBackupsDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
