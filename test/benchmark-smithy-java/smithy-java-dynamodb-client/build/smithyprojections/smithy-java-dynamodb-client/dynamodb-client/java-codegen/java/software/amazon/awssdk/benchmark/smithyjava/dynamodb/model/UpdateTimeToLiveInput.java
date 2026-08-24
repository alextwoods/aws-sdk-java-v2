package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the input of an <code>UpdateTimeToLive</code> operation.
 */
@SmithyGenerated
public final class UpdateTimeToLiveInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_TIME_TO_LIVE_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TIME_TO_LIVE_SPECIFICATION = $SCHEMA.member("TimeToLiveSpecification");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient TimeToLiveSpecification timeToLiveSpecification;

    private UpdateTimeToLiveInput(Builder builder) {
        this.tableName = builder.tableName;
        this.timeToLiveSpecification = builder.timeToLiveSpecification;
    }

    /**
     * The name of the table to be configured. You can also provide the Amazon Resource Name (ARN) of the table in this
     * parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Represents the settings used to enable or disable Time to Live for the specified table.
     */
    public TimeToLiveSpecification getTimeToLiveSpecification() {
        return timeToLiveSpecification;
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
        UpdateTimeToLiveInput that = (UpdateTimeToLiveInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.timeToLiveSpecification, that.timeToLiveSpecification);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(timeToLiveSpecification);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (timeToLiveSpecification != null) {
            serializer.writeStruct($SCHEMA_TIME_TO_LIVE_SPECIFICATION, timeToLiveSpecification);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION, member, timeToLiveSpecification);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateTimeToLiveInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.timeToLiveSpecification(this.timeToLiveSpecification);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateTimeToLiveInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateTimeToLiveInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private TimeToLiveSpecification timeToLiveSpecification;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table to be configured. You can also provide the Amazon Resource Name (ARN) of the table in this
         * parameter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * Represents the settings used to enable or disable Time to Live for the specified table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder timeToLiveSpecification(TimeToLiveSpecification timeToLiveSpecification) {
            this.timeToLiveSpecification = Objects.requireNonNull(timeToLiveSpecification, "timeToLiveSpecification cannot be null");
            tracker.setMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION);
            return this;
        }

        @Override
        public UpdateTimeToLiveInput build() {
            tracker.validate();
            return new UpdateTimeToLiveInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> timeToLiveSpecification((TimeToLiveSpecification) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateTimeToLiveInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION)) {
                tracker.setMember($SCHEMA_TIME_TO_LIVE_SPECIFICATION);
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.timeToLiveSpecification(TimeToLiveSpecification.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
