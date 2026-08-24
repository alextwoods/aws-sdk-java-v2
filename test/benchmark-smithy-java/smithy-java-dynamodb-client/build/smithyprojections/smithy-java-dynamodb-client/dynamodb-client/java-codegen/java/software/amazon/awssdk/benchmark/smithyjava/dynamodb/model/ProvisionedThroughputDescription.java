package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * Represents the provisioned throughput settings for the table, consisting of read and write capacity units, along with
 * data about increases and decreases.
 */
@SmithyGenerated
public final class ProvisionedThroughputDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PROVISIONED_THROUGHPUT_DESCRIPTION;
    private static final Schema $SCHEMA_LAST_INCREASE_DATE_TIME = $SCHEMA.member("LastIncreaseDateTime");
    private static final Schema $SCHEMA_LAST_DECREASE_DATE_TIME = $SCHEMA.member("LastDecreaseDateTime");
    private static final Schema $SCHEMA_NUMBER_OF_DECREASES_TODAY = $SCHEMA.member("NumberOfDecreasesToday");
    private static final Schema $SCHEMA_READ_CAPACITY_UNITS = $SCHEMA.member("ReadCapacityUnits");
    private static final Schema $SCHEMA_WRITE_CAPACITY_UNITS = $SCHEMA.member("WriteCapacityUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant lastIncreaseDateTime;
    private final transient Instant lastDecreaseDateTime;
    private final transient Long numberOfDecreasesToday;
    private final transient Long readCapacityUnits;
    private final transient Long writeCapacityUnits;

    private ProvisionedThroughputDescription(Builder builder) {
        this.lastIncreaseDateTime = builder.lastIncreaseDateTime;
        this.lastDecreaseDateTime = builder.lastDecreaseDateTime;
        this.numberOfDecreasesToday = builder.numberOfDecreasesToday;
        this.readCapacityUnits = builder.readCapacityUnits;
        this.writeCapacityUnits = builder.writeCapacityUnits;
    }

    /**
     * The date and time of the last provisioned throughput increase for this table.
     */
    public Instant getLastIncreaseDateTime() {
        return lastIncreaseDateTime;
    }

    /**
     * The date and time of the last provisioned throughput decrease for this table.
     */
    public Instant getLastDecreaseDateTime() {
        return lastDecreaseDateTime;
    }

    /**
     * The number of provisioned throughput decreases for this table during this UTC calendar day. For current maximums
     * on provisioned throughput decreases, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     */
    public Long getNumberOfDecreasesToday() {
        return numberOfDecreasesToday;
    }

    /**
     * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a <code>
     * ThrottlingException</code>. Eventually consistent reads require less effort than strongly consistent reads, so a
     * setting of 50 <code>ReadCapacityUnits</code> per second provides 100 eventually consistent <code>
     * ReadCapacityUnits</code> per second.
     *
     * <p>For a table or global secondary index that uses on-demand capacity mode (<code>PAY_PER_REQUEST</code>), this
     * value is <code>0</code>, because on-demand mode does not use provisioned throughput.
     */
    public Long getReadCapacityUnits() {
        return readCapacityUnits;
    }

    /**
     * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>.
     *
     * <p>For a table or global secondary index that uses on-demand capacity mode (<code>PAY_PER_REQUEST</code>), this
     * value is <code>0</code>, because on-demand mode does not use provisioned throughput.
     */
    public Long getWriteCapacityUnits() {
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
        ProvisionedThroughputDescription that = (ProvisionedThroughputDescription) other;
        return Objects.equals(this.numberOfDecreasesToday, that.numberOfDecreasesToday)
               && Objects.equals(this.readCapacityUnits, that.readCapacityUnits)
               && Objects.equals(this.writeCapacityUnits, that.writeCapacityUnits)
               && Objects.equals(this.lastIncreaseDateTime, that.lastIncreaseDateTime)
               && Objects.equals(this.lastDecreaseDateTime, that.lastDecreaseDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(lastIncreaseDateTime);
        $hc = 31 * $hc + Objects.hashCode(lastDecreaseDateTime);
        $hc = 31 * $hc + Objects.hashCode(numberOfDecreasesToday);
        $hc = 31 * $hc + Objects.hashCode(readCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(writeCapacityUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (lastIncreaseDateTime != null) {
            serializer.writeTimestamp($SCHEMA_LAST_INCREASE_DATE_TIME, lastIncreaseDateTime);
        }
        if (lastDecreaseDateTime != null) {
            serializer.writeTimestamp($SCHEMA_LAST_DECREASE_DATE_TIME, lastDecreaseDateTime);
        }
        if (numberOfDecreasesToday != null) {
            serializer.writeLong($SCHEMA_NUMBER_OF_DECREASES_TODAY, numberOfDecreasesToday);
        }
        if (readCapacityUnits != null) {
            serializer.writeLong($SCHEMA_READ_CAPACITY_UNITS, readCapacityUnits);
        }
        if (writeCapacityUnits != null) {
            serializer.writeLong($SCHEMA_WRITE_CAPACITY_UNITS, writeCapacityUnits);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_INCREASE_DATE_TIME, member, lastIncreaseDateTime);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_DECREASE_DATE_TIME, member, lastDecreaseDateTime);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NUMBER_OF_DECREASES_TODAY, member, numberOfDecreasesToday);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, readCapacityUnits);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, writeCapacityUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ProvisionedThroughputDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.lastIncreaseDateTime(this.lastIncreaseDateTime);
        builder.lastDecreaseDateTime(this.lastDecreaseDateTime);
        builder.numberOfDecreasesToday(this.numberOfDecreasesToday);
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
     * Builder for {@link ProvisionedThroughputDescription}.
     */
    public static final class Builder implements ShapeBuilder<ProvisionedThroughputDescription> {
        private Instant lastIncreaseDateTime;
        private Instant lastDecreaseDateTime;
        private Long numberOfDecreasesToday;
        private Long readCapacityUnits;
        private Long writeCapacityUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The date and time of the last provisioned throughput increase for this table.
         *
         * @return this builder.
         */
        public Builder lastIncreaseDateTime(Instant lastIncreaseDateTime) {
            this.lastIncreaseDateTime = lastIncreaseDateTime;
            return this;
        }

        /**
         * The date and time of the last provisioned throughput decrease for this table.
         *
         * @return this builder.
         */
        public Builder lastDecreaseDateTime(Instant lastDecreaseDateTime) {
            this.lastDecreaseDateTime = lastDecreaseDateTime;
            return this;
        }

        /**
         * The number of provisioned throughput decreases for this table during this UTC calendar day. For current maximums
         * on provisioned throughput decreases, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account, and Table Quotas</a> in the <i>Amazon DynamoDB
         * Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder numberOfDecreasesToday(Long numberOfDecreasesToday) {
            this.numberOfDecreasesToday = numberOfDecreasesToday;
            return this;
        }

        /**
         * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a <code>
         * ThrottlingException</code>. Eventually consistent reads require less effort than strongly consistent reads, so a
         * setting of 50 <code>ReadCapacityUnits</code> per second provides 100 eventually consistent <code>
         * ReadCapacityUnits</code> per second.
         *
         * <p>For a table or global secondary index that uses on-demand capacity mode (<code>PAY_PER_REQUEST</code>), this
         * value is <code>0</code>, because on-demand mode does not use provisioned throughput.
         *
         * @return this builder.
         */
        public Builder readCapacityUnits(Long readCapacityUnits) {
            this.readCapacityUnits = readCapacityUnits;
            return this;
        }

        /**
         * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>.
         *
         * <p>For a table or global secondary index that uses on-demand capacity mode (<code>PAY_PER_REQUEST</code>), this
         * value is <code>0</code>, because on-demand mode does not use provisioned throughput.
         *
         * @return this builder.
         */
        public Builder writeCapacityUnits(Long writeCapacityUnits) {
            this.writeCapacityUnits = writeCapacityUnits;
            return this;
        }

        @Override
        public ProvisionedThroughputDescription build() {
            return new ProvisionedThroughputDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> lastIncreaseDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_INCREASE_DATE_TIME, member, value));
                case 1 -> lastDecreaseDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_DECREASE_DATE_TIME, member, value));
                case 2 -> numberOfDecreasesToday((Long) SchemaUtils.validateSameMember($SCHEMA_NUMBER_OF_DECREASES_TODAY, member, value));
                case 3 -> readCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, value));
                case 4 -> writeCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, value));
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
                    case 0 -> builder.lastIncreaseDateTime(de.readTimestamp(member));
                    case 1 -> builder.lastDecreaseDateTime(de.readTimestamp(member));
                    case 2 -> builder.numberOfDecreasesToday(de.readLong(member));
                    case 3 -> builder.readCapacityUnits(de.readLong(member));
                    case 4 -> builder.writeCapacityUnits(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
