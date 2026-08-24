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

@SmithyGenerated
public final class DescribeGlobalTableSettingsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_GLOBAL_TABLE_SETTINGS_INPUT;
    private static final Schema $SCHEMA_GLOBAL_TABLE_NAME = $SCHEMA.member("GlobalTableName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String globalTableName;

    private DescribeGlobalTableSettingsInput(Builder builder) {
        this.globalTableName = builder.globalTableName;
    }

    /**
     * The name of the global table to describe.
     */
    public String getGlobalTableName() {
        return globalTableName;
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
        DescribeGlobalTableSettingsInput that = (DescribeGlobalTableSettingsInput) other;
        return Objects.equals(this.globalTableName, that.globalTableName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTableName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_GLOBAL_TABLE_NAME, globalTableName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, globalTableName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeGlobalTableSettingsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTableName(this.globalTableName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeGlobalTableSettingsInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeGlobalTableSettingsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String globalTableName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global table to describe.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder globalTableName(String globalTableName) {
            this.globalTableName = Objects.requireNonNull(globalTableName, "globalTableName cannot be null");
            tracker.setMember($SCHEMA_GLOBAL_TABLE_NAME);
            return this;
        }

        @Override
        public DescribeGlobalTableSettingsInput build() {
            tracker.validate();
            return new DescribeGlobalTableSettingsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTableName((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeGlobalTableSettingsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_GLOBAL_TABLE_NAME)) {
                globalTableName("");
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
                    case 0 -> builder.globalTableName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
