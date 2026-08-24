package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the provisioned throughput settings for the specified global secondary index. You must use <code>
 * ProvisionedThroughput</code> or <code>OnDemandThroughput</code> based on your table’s capacity mode.
 *
 * <p>For current minimum and maximum provisioned throughput values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a> in
 * the <i>Amazon DynamoDB Developer Guide</i>.
 */
@SmithyGenerated
public final class ProvisionedThroughput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PROVISIONED_THROUGHPUT;
    private static final Schema $SCHEMA_READ_CAPACITY_UNITS = $SCHEMA.member("ReadCapacityUnits");
    private static final Schema $SCHEMA_WRITE_CAPACITY_UNITS = $SCHEMA.member("WriteCapacityUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient long readCapacityUnits;
    private final transient long writeCapacityUnits;

    private ProvisionedThroughput(Builder builder) {
        this.readCapacityUnits = builder.readCapacityUnits;
        this.writeCapacityUnits = builder.writeCapacityUnits;
    }

    /**
     * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a <code>
     * ThrottlingException</code>. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughput.html">Specifying Read and Write Requirements</a> in the <i>
     * Amazon DynamoDB Developer Guide</i>.
     *
     * <p>If read/write capacity mode is <code>PAY_PER_REQUEST</code> the value is set to 0.
     */
    public long getReadCapacityUnits() {
        return readCapacityUnits;
    }

    /**
     * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>. For
     * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughput.html">Specifying Read and Write Requirements</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     *
     * <p>If read/write capacity mode is <code>PAY_PER_REQUEST</code> the value is set to 0.
     */
    public long getWriteCapacityUnits() {
        return writeCapacityUnits;
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
        ProvisionedThroughput that = (ProvisionedThroughput) other;
        return this.readCapacityUnits == that.readCapacityUnits
               && this.writeCapacityUnits == that.writeCapacityUnits;
    }

    @Override
    public int hashCode() {
        int $hc = Long.hashCode(readCapacityUnits);
        $hc = 31 * $hc + Long.hashCode(writeCapacityUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeLong($SCHEMA_READ_CAPACITY_UNITS, readCapacityUnits);
        serializer.writeLong($SCHEMA_WRITE_CAPACITY_UNITS, writeCapacityUnits);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, readCapacityUnits);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, writeCapacityUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ProvisionedThroughput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.readCapacityUnits(this.readCapacityUnits);
        builder.writeCapacityUnits(this.writeCapacityUnits);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ProvisionedThroughput}.
     */
    public static final class Builder implements ShapeBuilder<ProvisionedThroughput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private long readCapacityUnits;
        private long writeCapacityUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a <code>
         * ThrottlingException</code>. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughput.html">Specifying Read and Write Requirements</a> in the <i>
         * Amazon DynamoDB Developer Guide</i>.
         *
         * <p>If read/write capacity mode is <code>PAY_PER_REQUEST</code> the value is set to 0.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder readCapacityUnits(long readCapacityUnits) {
            this.readCapacityUnits = readCapacityUnits;
            tracker.setMember($SCHEMA_READ_CAPACITY_UNITS);
            return this;
        }

        /**
         * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>. For
         * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughput.html">Specifying Read and Write Requirements</a> in the <i>Amazon DynamoDB Developer Guide</i>
         * .
         *
         * <p>If read/write capacity mode is <code>PAY_PER_REQUEST</code> the value is set to 0.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder writeCapacityUnits(long writeCapacityUnits) {
            this.writeCapacityUnits = writeCapacityUnits;
            tracker.setMember($SCHEMA_WRITE_CAPACITY_UNITS);
            return this;
        }

        @Override
        public ProvisionedThroughput build() {
            tracker.validate();
            return new ProvisionedThroughput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> readCapacityUnits((long) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, value));
                case 1 -> writeCapacityUnits((long) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ProvisionedThroughput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_READ_CAPACITY_UNITS)) {
                tracker.setMember($SCHEMA_READ_CAPACITY_UNITS);
            }
            if (!tracker.checkMember($SCHEMA_WRITE_CAPACITY_UNITS)) {
                tracker.setMember($SCHEMA_WRITE_CAPACITY_UNITS);
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
                    case 0 -> builder.readCapacityUnits(de.readLong(member));
                    case 1 -> builder.writeCapacityUnits(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
