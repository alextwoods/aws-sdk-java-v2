package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
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

@SmithyGenerated
public final class PutManagedInsightRulesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_MANAGED_INSIGHT_RULES_INPUT;
    private static final Schema $SCHEMA_MANAGED_RULES = $SCHEMA.member("ManagedRules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ManagedRule> managedRules;

    private PutManagedInsightRulesInput(Builder builder) {
        this.managedRules = builder.managedRules == null ? null : Collections.unmodifiableList(builder.managedRules);
    }

    /**
     * A list of <code>ManagedRules</code> to enable.
     */
    public List<ManagedRule> getManagedRules() {
        if (managedRules == null) {
            return Collections.emptyList();
        }
        return managedRules;
    }

    public boolean hasManagedRules() {
        return managedRules != null;
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
        PutManagedInsightRulesInput that = (PutManagedInsightRulesInput) other;
        return Objects.equals(this.managedRules, that.managedRules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(managedRules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (managedRules != null) {
            serializer.writeList($SCHEMA_MANAGED_RULES, managedRules, managedRules.size(), SharedSerde.ManagedRulesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MANAGED_RULES, member, managedRules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutManagedInsightRulesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.managedRules(this.managedRules);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutManagedInsightRulesInput}.
     */
    public static final class Builder implements ShapeBuilder<PutManagedInsightRulesInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<ManagedRule> managedRules;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_MANAGED_RULES);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A list of <code>ManagedRules</code> to enable.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder managedRules(List<ManagedRule> managedRules) {
            this.managedRules = Objects.requireNonNull(managedRules, "managedRules cannot be null");
            tracker.setMember($SCHEMA_MANAGED_RULES);
            return this;
        }

        @Override
        public PutManagedInsightRulesInput build() {
            tracker.validate();
            return new PutManagedInsightRulesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> managedRules((List<ManagedRule>) SchemaUtils.validateSameMember($SCHEMA_MANAGED_RULES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutManagedInsightRulesInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_MANAGED_RULES)) {
                managedRules(Collections.emptyList());
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
                    case 0 -> builder.managedRules(SharedSerde.deserializeManagedRules(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
