package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * Specifies an S3 Inventory filter. The inventory only includes objects that meet the filter's criteria.
 */
@SmithyGenerated
public final class InventoryFilter implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_FILTER;
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String prefix;

    private InventoryFilter(Builder builder) {
        this.prefix = builder.prefix;
    }

    /**
     * The prefix that an object must have to be included in the inventory results.
     */
    public String getPrefix() {
        return prefix;
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
        InventoryFilter that = (InventoryFilter) other;
        return Objects.equals(this.prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(prefix);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_PREFIX, prefix);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryFilter}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.prefix(this.prefix);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryFilter}.
     */
    public static final class Builder implements ShapeBuilder<InventoryFilter> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String prefix;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The prefix that an object must have to be included in the inventory results.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = Objects.requireNonNull(prefix, "prefix cannot be null");
            tracker.setMember($SCHEMA_PREFIX);
            return this;
        }

        @Override
        public InventoryFilter build() {
            tracker.validate();
            return new InventoryFilter(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventoryFilter> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_PREFIX)) {
                prefix("");
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
                    case 0 -> builder.prefix(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
