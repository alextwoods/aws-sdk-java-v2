package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * A container for replication rules. You can add up to 1,000 rules. The maximum size of a replication configuration is
 * 2 MB.
 */
@SmithyGenerated
public final class ReplicationConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REPLICATION_CONFIGURATION;
    private static final Schema $SCHEMA_ROLE = $SCHEMA.member("Role");
    private static final Schema $SCHEMA_RULES = $SCHEMA.member("Rules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String role;
    private final transient List<ReplicationRule> rules;

    private ReplicationConfiguration(Builder builder) {
        this.role = builder.role;
        this.rules = Collections.unmodifiableList(builder.rules);
    }

    /**
     * The Amazon Resource Name (ARN) of the Identity and Access Management (IAM) role that Amazon S3 assumes when
     * replicating objects. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-how-setup.html">How to Set Up Replication</a> in the <i>Amazon S3 User Guide</i>
     * .
     */
    public String getRole() {
        return role;
    }

    /**
     * A container for one or more replication rules. A replication configuration must have at least one rule and can
     * contain a maximum of 1,000 rules.
     */
    public List<ReplicationRule> getRules() {
        return rules;
    }

    public boolean hasRules() {
        return true;
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
        ReplicationConfiguration that = (ReplicationConfiguration) other;
        return Objects.equals(this.role, that.role)
               && Objects.equals(this.rules, that.rules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(role);
        $hc = 31 * $hc + Objects.hashCode(rules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ROLE, role);
        serializer.writeList($SCHEMA_RULES, rules, rules.size(), SharedSerde.ReplicationRulesSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ROLE, member, role);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULES, member, rules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicationConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.role(this.role);
        builder.rules(this.rules);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicationConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<ReplicationConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String role;
        private List<ReplicationRule> rules;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the Identity and Access Management (IAM) role that Amazon S3 assumes when
         * replicating objects. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-how-setup.html">How to Set Up Replication</a> in the <i>Amazon S3 User Guide</i>
         * .
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder role(String role) {
            this.role = Objects.requireNonNull(role, "role cannot be null");
            tracker.setMember($SCHEMA_ROLE);
            return this;
        }

        /**
         * A container for one or more replication rules. A replication configuration must have at least one rule and can
         * contain a maximum of 1,000 rules.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder rules(List<ReplicationRule> rules) {
            this.rules = Objects.requireNonNull(rules, "rules cannot be null");
            tracker.setMember($SCHEMA_RULES);
            return this;
        }

        @Override
        public ReplicationConfiguration build() {
            tracker.validate();
            return new ReplicationConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> role((String) SchemaUtils.validateSameMember($SCHEMA_ROLE, member, value));
                case 1 -> rules((List<ReplicationRule>) SchemaUtils.validateSameMember($SCHEMA_RULES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicationConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ROLE)) {
                role("");
            }
            if (!tracker.checkMember($SCHEMA_RULES)) {
                rules(Collections.emptyList());
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
                    case 0 -> builder.role(de.readString(member));
                    case 1 -> builder.rules(SharedSerde.deserializeReplicationRules(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
