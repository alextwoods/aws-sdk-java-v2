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
 * Container for Payer.
 */
@SmithyGenerated
public final class RequestPaymentConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.REQUEST_PAYMENT_CONFIGURATION;
    private static final Schema $SCHEMA_PAYER = $SCHEMA.member("Payer");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Payer payer;

    private RequestPaymentConfiguration(Builder builder) {
        this.payer = builder.payer;
    }

    /**
     * Specifies who pays for the download and request fees.
     */
    public Payer getPayer() {
        return payer;
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
        RequestPaymentConfiguration that = (RequestPaymentConfiguration) other;
        return Objects.equals(this.payer, that.payer);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(payer);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_PAYER, payer.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PAYER, member, payer);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RequestPaymentConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.payer(this.payer);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RequestPaymentConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<RequestPaymentConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Payer payer;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies who pays for the download and request fees.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder payer(Payer payer) {
            this.payer = Objects.requireNonNull(payer, "payer cannot be null");
            tracker.setMember($SCHEMA_PAYER);
            return this;
        }

        @Override
        public RequestPaymentConfiguration build() {
            tracker.validate();
            return new RequestPaymentConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> payer((Payer) SchemaUtils.validateSameMember($SCHEMA_PAYER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RequestPaymentConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_PAYER)) {
                payer(Payer.unknown(""));
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
                    case 0 -> builder.payer(Payer.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
