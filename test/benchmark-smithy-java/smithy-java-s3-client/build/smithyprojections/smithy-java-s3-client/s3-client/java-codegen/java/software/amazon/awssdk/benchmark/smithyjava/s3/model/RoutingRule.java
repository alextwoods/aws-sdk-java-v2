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
 * Specifies the redirect behavior and when a redirect is applied. For more information about routing rules, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/how-to-page-redirect.html#advanced-conditional-redirects">
 * Configuring advanced conditional redirects</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class RoutingRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.ROUTING_RULE;
    private static final Schema $SCHEMA_CONDITION = $SCHEMA.member("Condition");
    private static final Schema $SCHEMA_REDIRECT = $SCHEMA.member("Redirect");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Condition condition;
    private final transient Redirect redirect;

    private RoutingRule(Builder builder) {
        this.condition = builder.condition;
        this.redirect = builder.redirect;
    }

    /**
     * A container for describing a condition that must be met for the specified redirect to apply. For example, 1. If
     * request is for pages in the <code>/docs</code> folder, redirect to the <code>/documents</code> folder. 2. If
     * request results in HTTP error 4xx, redirect request to another host where you might process the error.
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Container for redirect information. You can redirect requests to another host, to another page, or with another
     * protocol. In the event of an error, you can specify a different error code to return.
     */
    public Redirect getRedirect() {
        return redirect;
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
        RoutingRule that = (RoutingRule) other;
        return Objects.equals(this.condition, that.condition)
               && Objects.equals(this.redirect, that.redirect);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(condition);
        $hc = 31 * $hc + Objects.hashCode(redirect);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (condition != null) {
            serializer.writeStruct($SCHEMA_CONDITION, condition);
        }
        if (redirect != null) {
            serializer.writeStruct($SCHEMA_REDIRECT, redirect);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REDIRECT, member, redirect);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITION, member, condition);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RoutingRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.condition(this.condition);
        builder.redirect(this.redirect);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RoutingRule}.
     */
    public static final class Builder implements ShapeBuilder<RoutingRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Condition condition;
        private Redirect redirect;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A container for describing a condition that must be met for the specified redirect to apply. For example, 1. If
         * request is for pages in the <code>/docs</code> folder, redirect to the <code>/documents</code> folder. 2. If
         * request results in HTTP error 4xx, redirect request to another host where you might process the error.
         *
         * @return this builder.
         */
        public Builder condition(Condition condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Container for redirect information. You can redirect requests to another host, to another page, or with another
         * protocol. In the event of an error, you can specify a different error code to return.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder redirect(Redirect redirect) {
            this.redirect = Objects.requireNonNull(redirect, "redirect cannot be null");
            tracker.setMember($SCHEMA_REDIRECT);
            return this;
        }

        @Override
        public RoutingRule build() {
            tracker.validate();
            return new RoutingRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> redirect((Redirect) SchemaUtils.validateSameMember($SCHEMA_REDIRECT, member, value));
                case 1 -> condition((Condition) SchemaUtils.validateSameMember($SCHEMA_CONDITION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RoutingRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_REDIRECT)) {
                tracker.setMember($SCHEMA_REDIRECT);
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
                    case 0 -> builder.redirect(Redirect.builder().deserializeMember(de, member).build());
                    case 1 -> builder.condition(Condition.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
