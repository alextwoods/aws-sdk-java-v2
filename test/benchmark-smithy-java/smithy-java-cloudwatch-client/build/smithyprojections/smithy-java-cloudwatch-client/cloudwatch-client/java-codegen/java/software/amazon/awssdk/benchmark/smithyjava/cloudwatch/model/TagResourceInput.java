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
public final class TagResourceInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TAG_RESOURCE_INPUT;
    private static final Schema $SCHEMA_RESOURCEAR_N = $SCHEMA.member("ResourceARN");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourcearN;
    private final transient List<Tag> tags;

    private TagResourceInput(Builder builder) {
        this.resourcearN = builder.resourcearN;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
    }

    /**
     * The ARN of the CloudWatch resource that you're adding tags to.
     *
     * <p>The ARN format of an alarm is <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>:alarm:<i>alarm-name</i></code>
     *
     * <p>The ARN format of a Contributor Insights rule is <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>
     * :insight-rule/<i>insight-rule-name</i></code>
     *
     * <p>The ARN format of a dashboard is <code>arn:aws:cloudwatch::<i>account-id</i>:dashboard/<i>dashboard-name</i></code>
     *
     * <p>The ARN format of a metric stream is <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>:metric-stream/<i>
     * metric-stream-name</i></code>
     *
     * <p>For more information about ARN format, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/list_amazoncloudwatch.html#amazoncloudwatch-resources-for-iam-policies"> Resource Types Defined by Amazon CloudWatch</a> in the <i>
     * Amazon Web Services General Reference</i>.
     */
    public String getResourcearN() {
        return resourcearN;
    }

    /**
     * The list of key-value pairs to associate with the alarm.
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
        TagResourceInput that = (TagResourceInput) other;
        return Objects.equals(this.resourcearN, that.resourcearN)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourcearN);
        $hc = 31 * $hc + Objects.hashCode(tags);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, resourcearN);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TagResourceInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
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
     * Builder for {@link TagResourceInput}.
     */
    public static final class Builder implements ShapeBuilder<TagResourceInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourcearN;
        private List<Tag> tags;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_RESOURCEAR_N);
            tracker.setMember($SCHEMA_TAGS);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ARN of the CloudWatch resource that you're adding tags to.
         *
         * <p>The ARN format of an alarm is <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>:alarm:<i>alarm-name</i></code>
         *
         * <p>The ARN format of a Contributor Insights rule is <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>
         * :insight-rule/<i>insight-rule-name</i></code>
         *
         * <p>The ARN format of a dashboard is <code>arn:aws:cloudwatch::<i>account-id</i>:dashboard/<i>dashboard-name</i></code>
         *
         * <p>The ARN format of a metric stream is <code>arn:aws:cloudwatch:<i>Region</i>:<i>account-id</i>:metric-stream/<i>
         * metric-stream-name</i></code>
         *
         * <p>For more information about ARN format, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/list_amazoncloudwatch.html#amazoncloudwatch-resources-for-iam-policies"> Resource Types Defined by Amazon CloudWatch</a> in the <i>
         * Amazon Web Services General Reference</i>.
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
         * The list of key-value pairs to associate with the alarm.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = Objects.requireNonNull(tags, "tags cannot be null");
            tracker.setMember($SCHEMA_TAGS);
            return this;
        }

        @Override
        public TagResourceInput build() {
            tracker.validate();
            return new TagResourceInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourcearN((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, value));
                case 1 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TagResourceInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCEAR_N)) {
                resourcearN("");
            }
            if (!tracker.checkMember($SCHEMA_TAGS)) {
                tags(Collections.emptyList());
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
                    case 0 -> builder.resourcearN(de.readString(member));
                    case 1 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
