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
 * Container for S3 Glacier job parameters.
 */
@SmithyGenerated
public final class GlacierJobParameters implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.GLACIER_JOB_PARAMETERS;
    private static final Schema $SCHEMA_TIER = $SCHEMA.member("Tier");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Tier tier;

    private GlacierJobParameters(Builder builder) {
        this.tier = builder.tier;
    }

    /**
     * Retrieval tier at which the restore will be processed.
     */
    public Tier getTier() {
        return tier;
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
        GlacierJobParameters that = (GlacierJobParameters) other;
        return Objects.equals(this.tier, that.tier);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tier);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TIER, tier.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIER, member, tier);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlacierJobParameters}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tier(this.tier);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlacierJobParameters}.
     */
    public static final class Builder implements ShapeBuilder<GlacierJobParameters> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Tier tier;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Retrieval tier at which the restore will be processed.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tier(Tier tier) {
            this.tier = Objects.requireNonNull(tier, "tier cannot be null");
            tracker.setMember($SCHEMA_TIER);
            return this;
        }

        @Override
        public GlacierJobParameters build() {
            tracker.validate();
            return new GlacierJobParameters(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tier((Tier) SchemaUtils.validateSameMember($SCHEMA_TIER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GlacierJobParameters> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TIER)) {
                tier(Tier.unknown(""));
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
                    case 0 -> builder.tier(Tier.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
