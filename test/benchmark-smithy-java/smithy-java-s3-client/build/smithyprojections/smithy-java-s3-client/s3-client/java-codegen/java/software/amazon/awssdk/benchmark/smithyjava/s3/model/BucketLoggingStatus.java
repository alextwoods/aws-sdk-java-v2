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
 * Container for logging status information.
 */
@SmithyGenerated
public final class BucketLoggingStatus implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.BUCKET_LOGGING_STATUS;
    private static final Schema $SCHEMA_LOGGING_ENABLED = $SCHEMA.member("LoggingEnabled");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient LoggingEnabled loggingEnabled;

    private BucketLoggingStatus(Builder builder) {
        this.loggingEnabled = builder.loggingEnabled;
    }

    public LoggingEnabled getLoggingEnabled() {
        return loggingEnabled;
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
        BucketLoggingStatus that = (BucketLoggingStatus) other;
        return Objects.equals(this.loggingEnabled, that.loggingEnabled);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(loggingEnabled);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (loggingEnabled != null) {
            serializer.writeStruct($SCHEMA_LOGGING_ENABLED, loggingEnabled);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOGGING_ENABLED, member, loggingEnabled);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BucketLoggingStatus}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.loggingEnabled(this.loggingEnabled);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BucketLoggingStatus}.
     */
    public static final class Builder implements ShapeBuilder<BucketLoggingStatus> {
        private LoggingEnabled loggingEnabled;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder loggingEnabled(LoggingEnabled loggingEnabled) {
            this.loggingEnabled = loggingEnabled;
            return this;
        }

        @Override
        public BucketLoggingStatus build() {
            return new BucketLoggingStatus(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> loggingEnabled((LoggingEnabled) SchemaUtils.validateSameMember($SCHEMA_LOGGING_ENABLED, member, value));
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
                    case 0 -> builder.loggingEnabled(LoggingEnabled.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
