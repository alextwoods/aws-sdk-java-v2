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
 * The status of a managed Contributor Insights rule.
 */
@SmithyGenerated
public final class ManagedRuleState implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.MANAGED_RULE_STATE;
    private static final Schema $SCHEMA_RULE_NAME = $SCHEMA.member("RuleName");
    private static final Schema $SCHEMA_STATE = $SCHEMA.member("State");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String ruleName;
    private final transient String state;

    private ManagedRuleState(Builder builder) {
        this.ruleName = builder.ruleName;
        this.state = builder.state;
    }

    /**
     * The name of the Contributor Insights rule that contains data for the specified Amazon Web Services resource.
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Indicates whether the rule is enabled or disabled.
     */
    public String getState() {
        return state;
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
        ManagedRuleState that = (ManagedRuleState) other;
        return Objects.equals(this.ruleName, that.ruleName)
               && Objects.equals(this.state, that.state);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(ruleName);
        $hc = 31 * $hc + Objects.hashCode(state);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (ruleName != null) {
            serializer.writeString($SCHEMA_RULE_NAME, ruleName);
        }
        if (state != null) {
            serializer.writeString($SCHEMA_STATE, state);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_NAME, member, ruleName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE, member, state);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ManagedRuleState}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.ruleName(this.ruleName);
        builder.state(this.state);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ManagedRuleState}.
     */
    public static final class Builder implements ShapeBuilder<ManagedRuleState> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String ruleName;
        private String state;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_RULE_NAME);
            tracker.setMember($SCHEMA_STATE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the Contributor Insights rule that contains data for the specified Amazon Web Services resource.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder ruleName(String ruleName) {
            this.ruleName = Objects.requireNonNull(ruleName, "ruleName cannot be null");
            tracker.setMember($SCHEMA_RULE_NAME);
            return this;
        }

        /**
         * Indicates whether the rule is enabled or disabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder state(String state) {
            this.state = Objects.requireNonNull(state, "state cannot be null");
            tracker.setMember($SCHEMA_STATE);
            return this;
        }

        @Override
        public ManagedRuleState build() {
            tracker.validate();
            return new ManagedRuleState(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> ruleName((String) SchemaUtils.validateSameMember($SCHEMA_RULE_NAME, member, value));
                case 1 -> state((String) SchemaUtils.validateSameMember($SCHEMA_STATE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ManagedRuleState> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RULE_NAME)) {
                ruleName("");
            }
            if (!tracker.checkMember($SCHEMA_STATE)) {
                state("");
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
                    case 0 -> builder.ruleName(de.readString(member));
                    case 1 -> builder.state(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
