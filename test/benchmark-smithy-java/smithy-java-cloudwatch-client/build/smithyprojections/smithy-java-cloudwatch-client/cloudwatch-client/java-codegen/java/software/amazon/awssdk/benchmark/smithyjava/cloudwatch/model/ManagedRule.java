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

/**
 * Contains the information that's required to enable a managed Contributor Insights rule for an Amazon Web Services
 * resource.
 *
 * <pre>{@code
 *     </p>
 *
 * }</pre>
 */
@SmithyGenerated
public final class ManagedRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.MANAGED_RULE;
    private static final Schema $SCHEMA_TEMPLATE_NAME = $SCHEMA.member("TemplateName");
    private static final Schema $SCHEMA_RESOURCEAR_N = $SCHEMA.member("ResourceARN");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String templateName;
    private final transient String resourcearN;
    private final transient List<Tag> tags;

    private ManagedRule(Builder builder) {
        this.templateName = builder.templateName;
        this.resourcearN = builder.resourcearN;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
    }

    /**
     * The template name for the managed Contributor Insights rule, as returned by <code>ListManagedInsightRules</code>.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * The ARN of an Amazon Web Services resource that has managed Contributor Insights rules.
     */
    public String getResourcearN() {
        return resourcearN;
    }

    /**
     * A list of key-value pairs that you can associate with a managed Contributor Insights rule. You can associate as
     * many as 50 tags with a rule. Tags can help you organize and categorize your resources. You also can use them to
     * scope user permissions by granting a user permission to access or change only the resources that have certain tag
     * values. To associate tags with a rule, you must have the <code>cloudwatch:TagResource</code> permission in
     * addition to the <code>cloudwatch:PutInsightRule</code> permission. If you are using this operation to update an
     * existing Contributor Insights rule, any tags that you specify in this parameter are ignored. To change the tags
     * of an existing rule, use <code>TagResource</code>.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
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
        ManagedRule that = (ManagedRule) other;
        return Objects.equals(this.templateName, that.templateName)
               && Objects.equals(this.resourcearN, that.resourcearN)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(templateName);
        $hc = 31 * $hc + Objects.hashCode(resourcearN);
        $hc = 31 * $hc + Objects.hashCode(tags);
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
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TEMPLATE_NAME, member, templateName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, resourcearN);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ManagedRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.templateName(this.templateName);
        builder.resourcearN(this.resourcearN);
        builder.tags(this.tags);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ManagedRule}.
     */
    public static final class Builder implements ShapeBuilder<ManagedRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String templateName;
        private String resourcearN;
        private List<Tag> tags;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_TEMPLATE_NAME);
            tracker.setMember($SCHEMA_RESOURCEAR_N);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The template name for the managed Contributor Insights rule, as returned by <code>ListManagedInsightRules</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder templateName(String templateName) {
            this.templateName = Objects.requireNonNull(templateName, "templateName cannot be null");
            tracker.setMember($SCHEMA_TEMPLATE_NAME);
            return this;
        }

        /**
         * The ARN of an Amazon Web Services resource that has managed Contributor Insights rules.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder resourcearN(String resourcearN) {
            this.resourcearN = Objects.requireNonNull(resourcearN, "resourcearN cannot be null");
            tracker.setMember($SCHEMA_RESOURCEAR_N);
            return this;
        }

        /**
         * A list of key-value pairs that you can associate with a managed Contributor Insights rule. You can associate as
         * many as 50 tags with a rule. Tags can help you organize and categorize your resources. You also can use them to
         * scope user permissions by granting a user permission to access or change only the resources that have certain tag
         * values. To associate tags with a rule, you must have the <code>cloudwatch:TagResource</code> permission in
         * addition to the <code>cloudwatch:PutInsightRule</code> permission. If you are using this operation to update an
         * existing Contributor Insights rule, any tags that you specify in this parameter are ignored. To change the tags
         * of an existing rule, use <code>TagResource</code>.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        @Override
        public ManagedRule build() {
            tracker.validate();
            return new ManagedRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> templateName((String) SchemaUtils.validateSameMember($SCHEMA_TEMPLATE_NAME, member, value));
                case 1 -> resourcearN((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, value));
                case 2 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ManagedRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TEMPLATE_NAME)) {
                templateName("");
            }
            if (!tracker.checkMember($SCHEMA_RESOURCEAR_N)) {
                resourcearN("");
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
                    case 0 -> builder.templateName(de.readString(member));
                    case 1 -> builder.resourcearN(de.readString(member));
                    case 2 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
