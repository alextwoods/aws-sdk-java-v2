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

/**
 * Sets the maximum number of read and write units for the specified on-demand table. If you use this parameter, you
 * must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
 */
@SmithyGenerated
public final class OnDemandThroughput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ON_DEMAND_THROUGHPUT;
    private static final Schema $SCHEMA_MAX_READ_REQUEST_UNITS = $SCHEMA.member("MaxReadRequestUnits");
    private static final Schema $SCHEMA_MAX_WRITE_REQUEST_UNITS = $SCHEMA.member("MaxWriteRequestUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long maxReadRequestUnits;
    private final transient Long maxWriteRequestUnits;

    private OnDemandThroughput(Builder builder) {
        this.maxReadRequestUnits = builder.maxReadRequestUnits;
        this.maxWriteRequestUnits = builder.maxWriteRequestUnits;
    }

    /**
     * Maximum number of read request units for the specified table.
     *
     * <p>To specify a maximum <code>OnDemandThroughput</code> on your table, set the value of <code>MaxReadRequestUnits</code>
     * as greater than or equal to 1. To remove the maximum <code>OnDemandThroughput</code> that is currently set on
     * your table, set the value of <code>MaxReadRequestUnits</code> to -1.
     */
    public Long getMaxReadRequestUnits() {
        return maxReadRequestUnits;
    }

    /**
     * Maximum number of write request units for the specified table.
     *
     * <p>To specify a maximum <code>OnDemandThroughput</code> on your table, set the value of <code>
     * MaxWriteRequestUnits</code> as greater than or equal to 1. To remove the maximum <code>OnDemandThroughput</code>
     * that is currently set on your table, set the value of <code>MaxWriteRequestUnits</code> to -1.
     */
    public Long getMaxWriteRequestUnits() {
        return maxWriteRequestUnits;
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
        OnDemandThroughput that = (OnDemandThroughput) other;
        return Objects.equals(this.maxReadRequestUnits, that.maxReadRequestUnits)
               && Objects.equals(this.maxWriteRequestUnits, that.maxWriteRequestUnits);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(maxReadRequestUnits);
        $hc = 31 * $hc + Objects.hashCode(maxWriteRequestUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (maxReadRequestUnits != null) {
            serializer.writeLong($SCHEMA_MAX_READ_REQUEST_UNITS, maxReadRequestUnits);
        }
        if (maxWriteRequestUnits != null) {
            serializer.writeLong($SCHEMA_MAX_WRITE_REQUEST_UNITS, maxWriteRequestUnits);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_READ_REQUEST_UNITS, member, maxReadRequestUnits);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_WRITE_REQUEST_UNITS, member, maxWriteRequestUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link OnDemandThroughput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.maxReadRequestUnits(this.maxReadRequestUnits);
        builder.maxWriteRequestUnits(this.maxWriteRequestUnits);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OnDemandThroughput}.
     */
    public static final class Builder implements ShapeBuilder<OnDemandThroughput> {
        private Long maxReadRequestUnits;
        private Long maxWriteRequestUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Maximum number of read request units for the specified table.
         *
         * <p>To specify a maximum <code>OnDemandThroughput</code> on your table, set the value of <code>MaxReadRequestUnits</code>
         * as greater than or equal to 1. To remove the maximum <code>OnDemandThroughput</code> that is currently set on
         * your table, set the value of <code>MaxReadRequestUnits</code> to -1.
         *
         * @return this builder.
         */
        public Builder maxReadRequestUnits(Long maxReadRequestUnits) {
            this.maxReadRequestUnits = maxReadRequestUnits;
            return this;
        }

        /**
         * Maximum number of write request units for the specified table.
         *
         * <p>To specify a maximum <code>OnDemandThroughput</code> on your table, set the value of <code>
         * MaxWriteRequestUnits</code> as greater than or equal to 1. To remove the maximum <code>OnDemandThroughput</code>
         * that is currently set on your table, set the value of <code>MaxWriteRequestUnits</code> to -1.
         *
         * @return this builder.
         */
        public Builder maxWriteRequestUnits(Long maxWriteRequestUnits) {
            this.maxWriteRequestUnits = maxWriteRequestUnits;
            return this;
        }

        @Override
        public OnDemandThroughput build() {
            return new OnDemandThroughput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> maxReadRequestUnits((Long) SchemaUtils.validateSameMember($SCHEMA_MAX_READ_REQUEST_UNITS, member, value));
                case 1 -> maxWriteRequestUnits((Long) SchemaUtils.validateSameMember($SCHEMA_MAX_WRITE_REQUEST_UNITS, member, value));
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
                    case 0 -> builder.maxReadRequestUnits(de.readLong(member));
                    case 1 -> builder.maxWriteRequestUnits(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
