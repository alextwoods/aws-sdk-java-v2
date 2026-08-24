package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * An error or warning for the operation.
 */
@SmithyGenerated
public final class DashboardValidationMessage implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DASHBOARD_VALIDATION_MESSAGE;
    private static final Schema $SCHEMA_DATA_PATH = $SCHEMA.member("DataPath");
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String dataPath;
    private final transient String message;

    private DashboardValidationMessage(Builder builder) {
        this.dataPath = builder.dataPath;
        this.message = builder.message;
    }

    /**
     * The data path related to the message.
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     * A message describing the error or warning.
     */
    public String getMessage() {
        return message;
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
        DashboardValidationMessage that = (DashboardValidationMessage) other;
        return Objects.equals(this.dataPath, that.dataPath)
               && Objects.equals(this.message, that.message);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dataPath);
        $hc = 31 * $hc + Objects.hashCode(message);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dataPath != null) {
            serializer.writeString($SCHEMA_DATA_PATH, dataPath);
        }
        if (message != null) {
            serializer.writeString($SCHEMA_MESSAGE, message);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATA_PATH, member, dataPath);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, message);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DashboardValidationMessage}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dataPath(this.dataPath);
        builder.message(this.message);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DashboardValidationMessage}.
     */
    public static final class Builder implements ShapeBuilder<DashboardValidationMessage> {
        private String dataPath;
        private String message;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The data path related to the message.
         *
         * @return this builder.
         */
        public Builder dataPath(String dataPath) {
            this.dataPath = dataPath;
            return this;
        }

        /**
         * A message describing the error or warning.
         *
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public DashboardValidationMessage build() {
            return new DashboardValidationMessage(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dataPath((String) SchemaUtils.validateSameMember($SCHEMA_DATA_PATH, member, value));
                case 1 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
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
                    case 0 -> builder.dataPath(de.readString(member));
                    case 1 -> builder.message(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
