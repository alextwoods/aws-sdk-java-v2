package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * The configuration settings that define the warm-up behavior for an alarm. Use these settings to delay alarm
 * evaluation after you create or update the alarm, which reduces alarm noise while a new resource or service starts
 * publishing data.
 *
 * <p>During the warm-up period, the alarm stays in <code>INSUFFICIENT_DATA</code> and does not perform alarm actions.
 */
@SmithyGenerated
public final class WarmUpConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.WARM_UP_CONFIGURATION;
    private static final Schema $SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES = $SCHEMA.member("WarmUpPeriodDurationInMinutes");
    private static final Schema $SCHEMA_ONLY_START_EVALUATING_AFTER_WARM_UP_PERIOD_ENDS = $SCHEMA.member("OnlyStartEvaluatingAfterWarmUpPeriodEnds");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer warmUpPeriodDurationInMinutes;
    private final transient Boolean onlyStartEvaluatingAfterWarmUpPeriodEnds;

    private WarmUpConfiguration(Builder builder) {
        this.warmUpPeriodDurationInMinutes = builder.warmUpPeriodDurationInMinutes;
        this.onlyStartEvaluatingAfterWarmUpPeriodEnds = builder.onlyStartEvaluatingAfterWarmUpPeriodEnds;
    }

    /**
     * The length of the warm-up period, in minutes. After you create or update the alarm, the alarm stays in <code>
     * INSUFFICIENT_DATA</code> for this duration. During this time, the alarm does not perform alarm actions.
     *
     * <p>You can change this value at any time, including after the warm-up period ends. If you change it after the
     * warm-up period ends, the new value does not restart the warm-up period.
     */
    public Integer getWarmUpPeriodDurationInMinutes() {
        return warmUpPeriodDurationInMinutes;
    }

    /**
     * Specifies whether the alarm waits for the full warm-up period before it starts to evaluate. The default is
     * <code>false</code>. If <code>true</code>, the alarm waits the entire <code>WarmUpPeriodDurationInMinutes</code>
     * before it starts to evaluate, even if metric data arrives earlier. If <code>false</code>, the alarm ends the
     * warm-up period early. Evaluation begins as soon as the alarm has enough metric data to fill its evaluation
     * window.
     */
    public Boolean isOnlyStartEvaluatingAfterWarmUpPeriodEnds() {
        return onlyStartEvaluatingAfterWarmUpPeriodEnds;
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
        WarmUpConfiguration that = (WarmUpConfiguration) other;
        return Objects.equals(this.onlyStartEvaluatingAfterWarmUpPeriodEnds, that.onlyStartEvaluatingAfterWarmUpPeriodEnds)
               && Objects.equals(this.warmUpPeriodDurationInMinutes, that.warmUpPeriodDurationInMinutes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(warmUpPeriodDurationInMinutes);
        $hc = 31 * $hc + Objects.hashCode(onlyStartEvaluatingAfterWarmUpPeriodEnds);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (warmUpPeriodDurationInMinutes != null) {
            serializer.writeInteger($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES, warmUpPeriodDurationInMinutes);
        }
        if (onlyStartEvaluatingAfterWarmUpPeriodEnds != null) {
            serializer.writeBoolean($SCHEMA_ONLY_START_EVALUATING_AFTER_WARM_UP_PERIOD_ENDS, onlyStartEvaluatingAfterWarmUpPeriodEnds);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES, member, warmUpPeriodDurationInMinutes);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ONLY_START_EVALUATING_AFTER_WARM_UP_PERIOD_ENDS, member, onlyStartEvaluatingAfterWarmUpPeriodEnds);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link WarmUpConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.warmUpPeriodDurationInMinutes(this.warmUpPeriodDurationInMinutes);
        builder.onlyStartEvaluatingAfterWarmUpPeriodEnds(this.onlyStartEvaluatingAfterWarmUpPeriodEnds);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link WarmUpConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<WarmUpConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Integer warmUpPeriodDurationInMinutes;
        private Boolean onlyStartEvaluatingAfterWarmUpPeriodEnds;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The length of the warm-up period, in minutes. After you create or update the alarm, the alarm stays in <code>
         * INSUFFICIENT_DATA</code> for this duration. During this time, the alarm does not perform alarm actions.
         *
         * <p>You can change this value at any time, including after the warm-up period ends. If you change it after the
         * warm-up period ends, the new value does not restart the warm-up period.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder warmUpPeriodDurationInMinutes(Integer warmUpPeriodDurationInMinutes) {
            this.warmUpPeriodDurationInMinutes = warmUpPeriodDurationInMinutes;
            tracker.setMember($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES);
            return this;
        }

        /**
         * Specifies whether the alarm waits for the full warm-up period before it starts to evaluate. The default is
         * <code>false</code>. If <code>true</code>, the alarm waits the entire <code>WarmUpPeriodDurationInMinutes</code>
         * before it starts to evaluate, even if metric data arrives earlier. If <code>false</code>, the alarm ends the
         * warm-up period early. Evaluation begins as soon as the alarm has enough metric data to fill its evaluation
         * window.
         *
         * @return this builder.
         */
        public Builder onlyStartEvaluatingAfterWarmUpPeriodEnds(Boolean onlyStartEvaluatingAfterWarmUpPeriodEnds) {
            this.onlyStartEvaluatingAfterWarmUpPeriodEnds = onlyStartEvaluatingAfterWarmUpPeriodEnds;
            return this;
        }

        @Override
        public WarmUpConfiguration build() {
            tracker.validate();
            return new WarmUpConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> warmUpPeriodDurationInMinutes((Integer) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES, member, value));
                case 1 -> onlyStartEvaluatingAfterWarmUpPeriodEnds((Boolean) SchemaUtils.validateSameMember($SCHEMA_ONLY_START_EVALUATING_AFTER_WARM_UP_PERIOD_ENDS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<WarmUpConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES)) {
                tracker.setMember($SCHEMA_WARM_UP_PERIOD_DURATION_IN_MINUTES);
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
                    case 0 -> builder.warmUpPeriodDurationInMinutes(de.readInteger(member));
                    case 1 -> builder.onlyStartEvaluatingAfterWarmUpPeriodEnds(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
