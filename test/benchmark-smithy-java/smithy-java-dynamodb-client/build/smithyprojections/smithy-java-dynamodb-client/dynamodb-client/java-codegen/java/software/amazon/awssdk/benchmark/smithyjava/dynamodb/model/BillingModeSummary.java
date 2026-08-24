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
 * Contains the details for the read/write capacity mode. This page talks about <code>PROVISIONED</code> and <code>
 * PAY_PER_REQUEST</code> billing modes. For more information about these modes, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadWriteCapacityMode.html">Read/write capacity mode</a>.
 *
 * <p>You may need to switch to on-demand mode at least once in order to return a <code>BillingModeSummary</code>
 * response.
 */
@SmithyGenerated
public final class BillingModeSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BILLING_MODE_SUMMARY;
    private static final Schema $SCHEMA_BILLING_MODE = $SCHEMA.member("BillingMode");
    private static final Schema $SCHEMA_LAST_UPDATE_TO_PAY_PER_REQUEST_DATE_TIME = $SCHEMA.member("LastUpdateToPayPerRequestDateTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BillingMode billingMode;
    private final transient Instant lastUpdateToPayPerRequestDateTime;

    private BillingModeSummary(Builder builder) {
        this.billingMode = builder.billingMode;
        this.lastUpdateToPayPerRequestDateTime = builder.lastUpdateToPayPerRequestDateTime;
    }

    /**
     * Controls how you are charged for read and write throughput and how you manage capacity. This setting can be
     * changed later.
     *
     * <ul>
     *   <li>
     *     <code>PROVISIONED</code> - Sets the read/write capacity mode to <code>PROVISIONED</code>. We recommend
     *     using <code>PROVISIONED</code> for predictable workloads.
     *   </li>
     *   <li>
     *     <code>PAY_PER_REQUEST</code> - Sets the read/write capacity mode to <code>PAY_PER_REQUEST</code>. We
     *     recommend using <code>PAY_PER_REQUEST</code> for unpredictable workloads.
     *   </li>
     * </ul>
     */
    public BillingMode getBillingMode() {
        return billingMode;
    }

    /**
     * Represents the time when <code>PAY_PER_REQUEST</code> was last set as the read/write capacity mode.
     */
    public Instant getLastUpdateToPayPerRequestDateTime() {
        return lastUpdateToPayPerRequestDateTime;
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
        BillingModeSummary that = (BillingModeSummary) other;
        return Objects.equals(this.billingMode, that.billingMode)
               && Objects.equals(this.lastUpdateToPayPerRequestDateTime, that.lastUpdateToPayPerRequestDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(billingMode);
        $hc = 31 * $hc + Objects.hashCode(lastUpdateToPayPerRequestDateTime);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (billingMode != null) {
            serializer.writeString($SCHEMA_BILLING_MODE, billingMode.getValue());
        }
        if (lastUpdateToPayPerRequestDateTime != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATE_TO_PAY_PER_REQUEST_DATE_TIME, lastUpdateToPayPerRequestDateTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, billingMode);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_TO_PAY_PER_REQUEST_DATE_TIME, member, lastUpdateToPayPerRequestDateTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BillingModeSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.billingMode(this.billingMode);
        builder.lastUpdateToPayPerRequestDateTime(this.lastUpdateToPayPerRequestDateTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BillingModeSummary}.
     */
    public static final class Builder implements ShapeBuilder<BillingModeSummary> {
        private BillingMode billingMode;
        private Instant lastUpdateToPayPerRequestDateTime;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Controls how you are charged for read and write throughput and how you manage capacity. This setting can be
         * changed later.
         *
         * <ul>
         *   <li>
         *     <code>PROVISIONED</code> - Sets the read/write capacity mode to <code>PROVISIONED</code>. We recommend
         *     using <code>PROVISIONED</code> for predictable workloads.
         *   </li>
         *   <li>
         *     <code>PAY_PER_REQUEST</code> - Sets the read/write capacity mode to <code>PAY_PER_REQUEST</code>. We
         *     recommend using <code>PAY_PER_REQUEST</code> for unpredictable workloads.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder billingMode(BillingMode billingMode) {
            this.billingMode = billingMode;
            return this;
        }

        /**
         * Represents the time when <code>PAY_PER_REQUEST</code> was last set as the read/write capacity mode.
         *
         * @return this builder.
         */
        public Builder lastUpdateToPayPerRequestDateTime(Instant lastUpdateToPayPerRequestDateTime) {
            this.lastUpdateToPayPerRequestDateTime = lastUpdateToPayPerRequestDateTime;
            return this;
        }

        @Override
        public BillingModeSummary build() {
            return new BillingModeSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> billingMode((BillingMode) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, value));
                case 1 -> lastUpdateToPayPerRequestDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_TO_PAY_PER_REQUEST_DATE_TIME, member, value));
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
                    case 0 -> builder.billingMode(BillingMode.builder().deserializeMember(de, member).build());
                    case 1 -> builder.lastUpdateToPayPerRequestDateTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
