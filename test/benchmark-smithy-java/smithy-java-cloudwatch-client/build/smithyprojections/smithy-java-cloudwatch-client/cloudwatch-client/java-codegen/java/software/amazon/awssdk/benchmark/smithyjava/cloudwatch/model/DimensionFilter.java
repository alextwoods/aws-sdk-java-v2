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

/**
 * Represents filters for a dimension.
 */
@SmithyGenerated
public final class DimensionFilter implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DIMENSION_FILTER;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_VALUE = $SCHEMA.member("Value");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String name;
    private final transient String value;

    private DimensionFilter(Builder builder) {
        this.name = builder.name;
        this.value = builder.value;
    }

    /**
     * The dimension name to be matched.
     */
    public String getName() {
        return name;
    }

    /**
     * The value of the dimension to be matched.
     */
    public String getValue() {
        return value;
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
        DimensionFilter that = (DimensionFilter) other;
        return Objects.equals(this.name, that.name)
               && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(value);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
        if (value != null) {
            serializer.writeString($SCHEMA_VALUE, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DimensionFilter}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.value(this.value);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DimensionFilter}.
     */
    public static final class Builder implements ShapeBuilder<DimensionFilter> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String name;
        private String value;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The dimension name to be matched.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            tracker.setMember($SCHEMA_NAME);
            return this;
        }

        /**
         * The value of the dimension to be matched.
         *
         * @return this builder.
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        public DimensionFilter build() {
            tracker.validate();
            return new DimensionFilter(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> value((String) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DimensionFilter> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAME)) {
                name("");
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
                    case 0 -> builder.name(de.readString(member));
                    case 1 -> builder.value(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
