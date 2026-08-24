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
 * Represents the output of a <code>DescribeLimits</code> operation.
 */
@SmithyGenerated
public final class DescribeLimitsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_LIMITS_OUTPUT;
    private static final Schema $SCHEMA_ACCOUNT_MAX_READ_CAPACITY_UNITS = $SCHEMA.member("AccountMaxReadCapacityUnits");
    private static final Schema $SCHEMA_ACCOUNT_MAX_WRITE_CAPACITY_UNITS = $SCHEMA.member("AccountMaxWriteCapacityUnits");
    private static final Schema $SCHEMA_TABLE_MAX_READ_CAPACITY_UNITS = $SCHEMA.member("TableMaxReadCapacityUnits");
    private static final Schema $SCHEMA_TABLE_MAX_WRITE_CAPACITY_UNITS = $SCHEMA.member("TableMaxWriteCapacityUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long accountMaxReadCapacityUnits;
    private final transient Long accountMaxWriteCapacityUnits;
    private final transient Long tableMaxReadCapacityUnits;
    private final transient Long tableMaxWriteCapacityUnits;

    private DescribeLimitsOutput(Builder builder) {
        this.accountMaxReadCapacityUnits = builder.accountMaxReadCapacityUnits;
        this.accountMaxWriteCapacityUnits = builder.accountMaxWriteCapacityUnits;
        this.tableMaxReadCapacityUnits = builder.tableMaxReadCapacityUnits;
        this.tableMaxWriteCapacityUnits = builder.tableMaxWriteCapacityUnits;
    }

    /**
     * The maximum total read capacity units that your account allows you to provision across all of your tables in this
     * Region.
     */
    public Long getAccountMaxReadCapacityUnits() {
        return accountMaxReadCapacityUnits;
    }

    /**
     * The maximum total write capacity units that your account allows you to provision across all of your tables in
     * this Region.
     */
    public Long getAccountMaxWriteCapacityUnits() {
        return accountMaxWriteCapacityUnits;
    }

    /**
     * The maximum read capacity units that your account allows you to provision for a new table that you are creating
     * in this Region, including the read capacity units provisioned for its global secondary indexes (GSIs).
     */
    public Long getTableMaxReadCapacityUnits() {
        return tableMaxReadCapacityUnits;
    }

    /**
     * The maximum write capacity units that your account allows you to provision for a new table that you are creating
     * in this Region, including the write capacity units provisioned for its global secondary indexes (GSIs).
     */
    public Long getTableMaxWriteCapacityUnits() {
        return tableMaxWriteCapacityUnits;
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
        DescribeLimitsOutput that = (DescribeLimitsOutput) other;
        return Objects.equals(this.accountMaxReadCapacityUnits, that.accountMaxReadCapacityUnits)
               && Objects.equals(this.accountMaxWriteCapacityUnits, that.accountMaxWriteCapacityUnits)
               && Objects.equals(this.tableMaxReadCapacityUnits, that.tableMaxReadCapacityUnits)
               && Objects.equals(this.tableMaxWriteCapacityUnits, that.tableMaxWriteCapacityUnits);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(accountMaxReadCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(accountMaxWriteCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(tableMaxReadCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(tableMaxWriteCapacityUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (accountMaxReadCapacityUnits != null) {
            serializer.writeLong($SCHEMA_ACCOUNT_MAX_READ_CAPACITY_UNITS, accountMaxReadCapacityUnits);
        }
        if (accountMaxWriteCapacityUnits != null) {
            serializer.writeLong($SCHEMA_ACCOUNT_MAX_WRITE_CAPACITY_UNITS, accountMaxWriteCapacityUnits);
        }
        if (tableMaxReadCapacityUnits != null) {
            serializer.writeLong($SCHEMA_TABLE_MAX_READ_CAPACITY_UNITS, tableMaxReadCapacityUnits);
        }
        if (tableMaxWriteCapacityUnits != null) {
            serializer.writeLong($SCHEMA_TABLE_MAX_WRITE_CAPACITY_UNITS, tableMaxWriteCapacityUnits);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_MAX_READ_CAPACITY_UNITS, member, accountMaxReadCapacityUnits);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_MAX_WRITE_CAPACITY_UNITS, member, accountMaxWriteCapacityUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_MAX_READ_CAPACITY_UNITS, member, tableMaxReadCapacityUnits);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_MAX_WRITE_CAPACITY_UNITS, member, tableMaxWriteCapacityUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeLimitsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.accountMaxReadCapacityUnits(this.accountMaxReadCapacityUnits);
        builder.accountMaxWriteCapacityUnits(this.accountMaxWriteCapacityUnits);
        builder.tableMaxReadCapacityUnits(this.tableMaxReadCapacityUnits);
        builder.tableMaxWriteCapacityUnits(this.tableMaxWriteCapacityUnits);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeLimitsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeLimitsOutput> {
        private Long accountMaxReadCapacityUnits;
        private Long accountMaxWriteCapacityUnits;
        private Long tableMaxReadCapacityUnits;
        private Long tableMaxWriteCapacityUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The maximum total read capacity units that your account allows you to provision across all of your tables in this
         * Region.
         *
         * @return this builder.
         */
        public Builder accountMaxReadCapacityUnits(Long accountMaxReadCapacityUnits) {
            this.accountMaxReadCapacityUnits = accountMaxReadCapacityUnits;
            return this;
        }

        /**
         * The maximum total write capacity units that your account allows you to provision across all of your tables in
         * this Region.
         *
         * @return this builder.
         */
        public Builder accountMaxWriteCapacityUnits(Long accountMaxWriteCapacityUnits) {
            this.accountMaxWriteCapacityUnits = accountMaxWriteCapacityUnits;
            return this;
        }

        /**
         * The maximum read capacity units that your account allows you to provision for a new table that you are creating
         * in this Region, including the read capacity units provisioned for its global secondary indexes (GSIs).
         *
         * @return this builder.
         */
        public Builder tableMaxReadCapacityUnits(Long tableMaxReadCapacityUnits) {
            this.tableMaxReadCapacityUnits = tableMaxReadCapacityUnits;
            return this;
        }

        /**
         * The maximum write capacity units that your account allows you to provision for a new table that you are creating
         * in this Region, including the write capacity units provisioned for its global secondary indexes (GSIs).
         *
         * @return this builder.
         */
        public Builder tableMaxWriteCapacityUnits(Long tableMaxWriteCapacityUnits) {
            this.tableMaxWriteCapacityUnits = tableMaxWriteCapacityUnits;
            return this;
        }

        @Override
        public DescribeLimitsOutput build() {
            return new DescribeLimitsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> accountMaxReadCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_MAX_READ_CAPACITY_UNITS, member, value));
                case 1 -> accountMaxWriteCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_MAX_WRITE_CAPACITY_UNITS, member, value));
                case 2 -> tableMaxReadCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_TABLE_MAX_READ_CAPACITY_UNITS, member, value));
                case 3 -> tableMaxWriteCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_TABLE_MAX_WRITE_CAPACITY_UNITS, member, value));
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
                    case 0 -> builder.accountMaxReadCapacityUnits(de.readLong(member));
                    case 1 -> builder.accountMaxWriteCapacityUnits(de.readLong(member));
                    case 2 -> builder.tableMaxReadCapacityUnits(de.readLong(member));
                    case 3 -> builder.tableMaxWriteCapacityUnits(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
