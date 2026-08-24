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
 * Specifies the lifecycle configuration for objects in an Amazon S3 bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html">Object
 * Lifecycle Management</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class BucketLifecycleConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.BUCKET_LIFECYCLE_CONFIGURATION;
    private static final Schema $SCHEMA_RULES = $SCHEMA.member("Rules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<LifecycleRule> rules;

    private BucketLifecycleConfiguration(Builder builder) {
        this.rules = Collections.unmodifiableList(builder.rules);
    }

    /**
     * A lifecycle rule for individual objects in an Amazon S3 bucket.
     */
    public List<LifecycleRule> getRules() {
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
        BucketLifecycleConfiguration that = (BucketLifecycleConfiguration) other;
        return Objects.equals(this.rules, that.rules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(rules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_RULES, rules, rules.size(), SharedSerde.LifecycleRulesSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULES, member, rules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BucketLifecycleConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
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
     * Builder for {@link BucketLifecycleConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<BucketLifecycleConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<LifecycleRule> rules;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A lifecycle rule for individual objects in an Amazon S3 bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder rules(List<LifecycleRule> rules) {
            this.rules = Objects.requireNonNull(rules, "rules cannot be null");
            tracker.setMember($SCHEMA_RULES);
            return this;
        }

        @Override
        public BucketLifecycleConfiguration build() {
            tracker.validate();
            return new BucketLifecycleConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> rules((List<LifecycleRule>) SchemaUtils.validateSameMember($SCHEMA_RULES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<BucketLifecycleConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
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
                    case 0 -> builder.rules(SharedSerde.deserializeLifecycleRules(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
