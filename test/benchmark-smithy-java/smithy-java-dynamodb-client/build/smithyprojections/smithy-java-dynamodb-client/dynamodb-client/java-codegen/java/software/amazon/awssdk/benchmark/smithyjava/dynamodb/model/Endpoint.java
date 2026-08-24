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
 * An endpoint information details.
 */
@SmithyGenerated
public final class Endpoint implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ENDPOINT;
    private static final Schema $SCHEMA_ADDRESS = $SCHEMA.member("Address");
    private static final Schema $SCHEMA_CACHE_PERIOD_IN_MINUTES = $SCHEMA.member("CachePeriodInMinutes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String address;
    private final transient long cachePeriodInMinutes;

    private Endpoint(Builder builder) {
        this.address = builder.address;
        this.cachePeriodInMinutes = builder.cachePeriodInMinutes;
    }

    /**
     * IP address of the endpoint.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Endpoint cache time to live (TTL) value.
     */
    public long getCachePeriodInMinutes() {
        return cachePeriodInMinutes;
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
        Endpoint that = (Endpoint) other;
        return this.cachePeriodInMinutes == that.cachePeriodInMinutes
               && Objects.equals(this.address, that.address);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(address);
        $hc = 31 * $hc + Long.hashCode(cachePeriodInMinutes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ADDRESS, address);
        serializer.writeLong($SCHEMA_CACHE_PERIOD_IN_MINUTES, cachePeriodInMinutes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ADDRESS, member, address);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CACHE_PERIOD_IN_MINUTES, member, cachePeriodInMinutes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Endpoint}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.address(this.address);
        builder.cachePeriodInMinutes(this.cachePeriodInMinutes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Endpoint}.
     */
    public static final class Builder implements ShapeBuilder<Endpoint> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String address;
        private long cachePeriodInMinutes = 0L;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * IP address of the endpoint.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder address(String address) {
            this.address = Objects.requireNonNull(address, "address cannot be null");
            tracker.setMember($SCHEMA_ADDRESS);
            return this;
        }

        /**
         * Endpoint cache time to live (TTL) value.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder cachePeriodInMinutes(long cachePeriodInMinutes) {
            this.cachePeriodInMinutes = cachePeriodInMinutes;
            return this;
        }

        @Override
        public Endpoint build() {
            tracker.validate();
            return new Endpoint(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> address((String) SchemaUtils.validateSameMember($SCHEMA_ADDRESS, member, value));
                case 1 -> cachePeriodInMinutes((long) SchemaUtils.validateSameMember($SCHEMA_CACHE_PERIOD_IN_MINUTES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Endpoint> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ADDRESS)) {
                address("");
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
                    case 0 -> builder.address(de.readString(member));
                    case 1 -> builder.cachePeriodInMinutes(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
