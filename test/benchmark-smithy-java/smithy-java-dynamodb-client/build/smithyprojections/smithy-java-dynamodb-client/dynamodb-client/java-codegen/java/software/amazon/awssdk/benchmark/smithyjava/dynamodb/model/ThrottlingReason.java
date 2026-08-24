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
 * Represents the specific reason why a DynamoDB request was throttled and the ARN of the impacted resource. This helps
 * identify exactly what resource is being throttled, what type of operation caused it, and why the throttling occurred.
 */
@SmithyGenerated
public final class ThrottlingReason implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.THROTTLING_REASON;
    private static final Schema $SCHEMA_REASON = $SCHEMA.member("reason");
    private static final Schema $SCHEMA_RESOURCE = $SCHEMA.member("resource");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String reason;
    private final transient String resource;

    private ThrottlingReason(Builder builder) {
        this.reason = builder.reason;
        this.resource = builder.resource;
    }

    /**
     * The reason for throttling. The throttling reason follows a specific format: <code>
     * ResourceType+OperationType+LimitType</code>:
     *
     * <ul>
     *   <li>
     *     Resource Type (What is being throttled): Table or Index
     *   </li>
     *   <li>
     *     Operation Type (What kind of operation): Read or Write
     *   </li>
     *   <li>
     *     Limit Type (Why the throttling occurred):
     *
     *     <ul>
     *       <li>
     *         <code>ProvisionedThroughputExceeded</code>: The request rate is exceeding the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">provisioned
     *         throughput capacity</a> (read or write capacity units) configured for a table or a global
     *         secondary index (GSI) in provisioned capacity mode.
     *       </li>
     *       <li>
     *         <code>AccountLimitExceeded</code>: The request rate has caused a table or global secondary index
     *         (GSI) in on-demand mode to exceed the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ServiceQuotas.html#default-limits-throughput">per-table account-level service quotas</a> for
     *         read/write throughput in the current Amazon Web Services Region.
     *       </li>
     *       <li>
     *         <code>KeyRangeThroughputExceeded</code>: The request rate directed at a specific partition key
     *         value has exceeded the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-partition-key-design.html">internal partition-level throughput limits</a>, indicating uneven
     *         access patterns across the table's or GSI's key space.
     *       </li>
     *       <li>
     *         <code>MaxOnDemandThroughputExceeded</code>: The request rate has exceeded the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode-max-throughput.html">configured
     *         maximum throughput limits</a> set for a table or index in on-demand capacity mode.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>Examples of complete throttling reasons:
     *
     * <ul>
     *   <li>
     *     TableReadProvisionedThroughputExceeded
     *   </li>
     *   <li>
     *     IndexWriteAccountLimitExceeded
     *   </li>
     * </ul>
     *
     * <p>This helps identify exactly what resource is being throttled, what type of operation caused it, and why the
     * throttling occurred.
     */
    public String getReason() {
        return reason;
    }

    /**
     * The Amazon Resource Name (ARN) of the DynamoDB table or index that experienced the throttling event.
     */
    public String getResource() {
        return resource;
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
        ThrottlingReason that = (ThrottlingReason) other;
        return Objects.equals(this.reason, that.reason)
               && Objects.equals(this.resource, that.resource);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(reason);
        $hc = 31 * $hc + Objects.hashCode(resource);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (reason != null) {
            serializer.writeString($SCHEMA_REASON, reason);
        }
        if (resource != null) {
            serializer.writeString($SCHEMA_RESOURCE, resource);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REASON, member, reason);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE, member, resource);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ThrottlingReason}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.reason(this.reason);
        builder.resource(this.resource);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ThrottlingReason}.
     */
    public static final class Builder implements ShapeBuilder<ThrottlingReason> {
        private String reason;
        private String resource;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The reason for throttling. The throttling reason follows a specific format: <code>
         * ResourceType+OperationType+LimitType</code>:
         *
         * <ul>
         *   <li>
         *     Resource Type (What is being throttled): Table or Index
         *   </li>
         *   <li>
         *     Operation Type (What kind of operation): Read or Write
         *   </li>
         *   <li>
         *     Limit Type (Why the throttling occurred):
         *
         *     <ul>
         *       <li>
         *         <code>ProvisionedThroughputExceeded</code>: The request rate is exceeding the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">provisioned
         *         throughput capacity</a> (read or write capacity units) configured for a table or a global
         *         secondary index (GSI) in provisioned capacity mode.
         *       </li>
         *       <li>
         *         <code>AccountLimitExceeded</code>: The request rate has caused a table or global secondary index
         *         (GSI) in on-demand mode to exceed the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ServiceQuotas.html#default-limits-throughput">per-table account-level service quotas</a> for
         *         read/write throughput in the current Amazon Web Services Region.
         *       </li>
         *       <li>
         *         <code>KeyRangeThroughputExceeded</code>: The request rate directed at a specific partition key
         *         value has exceeded the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-partition-key-design.html">internal partition-level throughput limits</a>, indicating uneven
         *         access patterns across the table's or GSI's key space.
         *       </li>
         *       <li>
         *         <code>MaxOnDemandThroughputExceeded</code>: The request rate has exceeded the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode-max-throughput.html">configured
         *         maximum throughput limits</a> set for a table or index in on-demand capacity mode.
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * <p>Examples of complete throttling reasons:
         *
         * <ul>
         *   <li>
         *     TableReadProvisionedThroughputExceeded
         *   </li>
         *   <li>
         *     IndexWriteAccountLimitExceeded
         *   </li>
         * </ul>
         *
         * <p>This helps identify exactly what resource is being throttled, what type of operation caused it, and why the
         * throttling occurred.
         *
         * @return this builder.
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the DynamoDB table or index that experienced the throttling event.
         *
         * @return this builder.
         */
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        @Override
        public ThrottlingReason build() {
            return new ThrottlingReason(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> reason((String) SchemaUtils.validateSameMember($SCHEMA_REASON, member, value));
                case 1 -> resource((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE, member, value));
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
                    case 0 -> builder.reason(de.readString(member));
                    case 1 -> builder.resource(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
