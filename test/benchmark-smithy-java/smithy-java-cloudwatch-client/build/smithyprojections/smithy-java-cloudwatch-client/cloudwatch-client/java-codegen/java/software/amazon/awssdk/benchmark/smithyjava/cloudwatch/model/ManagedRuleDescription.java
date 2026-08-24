package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * Contains information about managed Contributor Insights rules, as returned by <code>ListManagedInsightRules</code>.
 */
@SmithyGenerated
public final class ManagedRuleDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.MANAGED_RULE_DESCRIPTION;
    private static final Schema $SCHEMA_TEMPLATE_NAME = $SCHEMA.member("TemplateName");
    private static final Schema $SCHEMA_RESOURCEAR_N = $SCHEMA.member("ResourceARN");
    private static final Schema $SCHEMA_RULE_STATE = $SCHEMA.member("RuleState");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String templateName;
    private final transient String resourcearN;
    private final transient ManagedRuleState ruleState;

    private ManagedRuleDescription(Builder builder) {
        this.templateName = builder.templateName;
        this.resourcearN = builder.resourcearN;
        this.ruleState = builder.ruleState;
    }

    /**
     * The template name for the managed rule. Used to enable managed rules using <code>PutManagedInsightRules</code>.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * If a managed rule is enabled, this is the ARN for the related Amazon Web Services resource.
     */
    public String getResourcearN() {
        return resourcearN;
    }

    /**
     * Describes the state of a managed rule. If present, it contains information about the Contributor Insights rule
     * that contains information about the related Amazon Web Services resource.
     */
    public ManagedRuleState getRuleState() {
        return ruleState;
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
        ManagedRuleDescription that = (ManagedRuleDescription) other;
        return Objects.equals(this.templateName, that.templateName)
               && Objects.equals(this.resourcearN, that.resourcearN)
               && Objects.equals(this.ruleState, that.ruleState);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(templateName);
        $hc = 31 * $hc + Objects.hashCode(resourcearN);
        $hc = 31 * $hc + Objects.hashCode(ruleState);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (templateName != null) {
            serializer.writeString($SCHEMA_TEMPLATE_NAME, templateName);
        }
        if (resourcearN != null) {
            serializer.writeString($SCHEMA_RESOURCEAR_N, resourcearN);
        }
        if (ruleState != null) {
            serializer.writeStruct($SCHEMA_RULE_STATE, ruleState);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TEMPLATE_NAME, member, templateName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, resourcearN);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_STATE, member, ruleState);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ManagedRuleDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.templateName(this.templateName);
        builder.resourcearN(this.resourcearN);
        builder.ruleState(this.ruleState);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ManagedRuleDescription}.
     */
    public static final class Builder implements ShapeBuilder<ManagedRuleDescription> {
        private String templateName;
        private String resourcearN;
        private ManagedRuleState ruleState;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The template name for the managed rule. Used to enable managed rules using <code>PutManagedInsightRules</code>.
         *
         * @return this builder.
         */
        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        /**
         * If a managed rule is enabled, this is the ARN for the related Amazon Web Services resource.
         *
         * @return this builder.
         */
        public Builder resourcearN(String resourcearN) {
            this.resourcearN = resourcearN;
            return this;
        }

        /**
         * Describes the state of a managed rule. If present, it contains information about the Contributor Insights rule
         * that contains information about the related Amazon Web Services resource.
         *
         * @return this builder.
         */
        public Builder ruleState(ManagedRuleState ruleState) {
            this.ruleState = ruleState;
            return this;
        }

        @Override
        public ManagedRuleDescription build() {
            return new ManagedRuleDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> templateName((String) SchemaUtils.validateSameMember($SCHEMA_TEMPLATE_NAME, member, value));
                case 1 -> resourcearN((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, value));
                case 2 -> ruleState((ManagedRuleState) SchemaUtils.validateSameMember($SCHEMA_RULE_STATE, member, value));
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
                    case 0 -> builder.templateName(de.readString(member));
                    case 1 -> builder.resourcearN(de.readString(member));
                    case 2 -> builder.ruleState(ManagedRuleState.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
