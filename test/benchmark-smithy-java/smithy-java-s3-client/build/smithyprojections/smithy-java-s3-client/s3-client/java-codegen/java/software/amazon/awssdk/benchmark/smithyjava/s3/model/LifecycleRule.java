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
 * A lifecycle rule for individual objects in an Amazon S3 bucket.
 *
 * <p>For more information see, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html">Managing your storage lifecycle</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class LifecycleRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.LIFECYCLE_RULE;
    private static final Schema $SCHEMA_EXPIRATION = $SCHEMA.member("Expiration");
    private static final Schema $SCHEMA_ID = $SCHEMA.member("ID");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_TRANSITIONS = $SCHEMA.member("Transitions");
    private static final Schema $SCHEMA_NONCURRENT_VERSION_TRANSITIONS = $SCHEMA.member("NoncurrentVersionTransitions");
    private static final Schema $SCHEMA_NONCURRENT_VERSION_EXPIRATION = $SCHEMA.member("NoncurrentVersionExpiration");
    private static final Schema $SCHEMA_ABORT_INCOMPLETE_MULTIPART_UPLOAD = $SCHEMA.member("AbortIncompleteMultipartUpload");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient LifecycleExpiration expiration;
    private final transient String id;
    private final transient String prefix;
    private final transient LifecycleRuleFilter filter;
    private final transient ExpirationStatus status;
    private final transient List<Transition> transitions;
    private final transient List<NoncurrentVersionTransition> noncurrentVersionTransitions;
    private final transient NoncurrentVersionExpiration noncurrentVersionExpiration;
    private final transient AbortIncompleteMultipartUpload abortIncompleteMultipartUpload;

    private LifecycleRule(Builder builder) {
        this.expiration = builder.expiration;
        this.id = builder.id;
        this.prefix = builder.prefix;
        this.filter = builder.filter;
        this.status = builder.status;
        this.transitions = builder.transitions == null ? null : Collections.unmodifiableList(builder.transitions);
        this.noncurrentVersionTransitions = builder.noncurrentVersionTransitions == null ? null : Collections.unmodifiableList(builder.noncurrentVersionTransitions);
        this.noncurrentVersionExpiration = builder.noncurrentVersionExpiration;
        this.abortIncompleteMultipartUpload = builder.abortIncompleteMultipartUpload;
    }

    /**
     * Specifies the expiration for the lifecycle of the object in the form of date, days and, whether the object has a
     * delete marker.
     */
    public LifecycleExpiration getExpiration() {
        return expiration;
    }

    /**
     * Unique identifier for the rule. The value cannot be longer than 255 characters.
     */
    public String getId() {
        return id;
    }

    /**
     * The general purpose bucket prefix that identifies one or more objects to which the rule applies. We recommend
     * using <code>Filter</code> instead of <code>Prefix</code> for new PUTs. Previous configurations where a prefix is
     * defined will continue to operate as before.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     *
     * @deprecated
     */
    @Deprecated
    public String getPrefix() {
        return prefix;
    }

    /**
     * The <code>Filter</code> is used to identify objects that a Lifecycle Rule applies to. A <code>Filter</code> must
     * have exactly one of <code>Prefix</code>, <code>Tag</code>, <code>ObjectSizeGreaterThan</code>, <code>
     * ObjectSizeLessThan</code>, or <code>And</code> specified. <code>Filter</code> is required if the <code>
     * LifecycleRule</code> does not contain a <code>Prefix</code> element.
     *
     * <p>For more information about <code>Tag</code> filters, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intro-lifecycle-filters.html">Adding filters to Lifecycle rules</a> in the <i>
     * Amazon S3 User Guide</i>.
     *
     * <p><code>Tag</code> filters are not supported for directory buckets.
     */
    public LifecycleRuleFilter getFilter() {
        return filter;
    }

    /**
     * If 'Enabled', the rule is currently being applied. If 'Disabled', the rule is not currently being applied.
     */
    public ExpirationStatus getStatus() {
        return status;
    }

    /**
     * Specifies when an Amazon S3 object transitions to a specified storage class.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     */
    public List<Transition> getTransitions() {
        if (transitions == null) {
            return Collections.emptyList();
        }
        return transitions;
    }

    public boolean hasTransitions() {
        return transitions != null;
    }

    /**
     * Specifies the transition rule for the lifecycle rule that describes when noncurrent objects transition to a
     * specific storage class. If your bucket is versioning-enabled (or versioning is suspended), you can set this
     * action to request that Amazon S3 transition noncurrent object versions to a specific storage class at a set
     * period in the object's lifetime.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     */
    public List<NoncurrentVersionTransition> getNoncurrentVersionTransitions() {
        if (noncurrentVersionTransitions == null) {
            return Collections.emptyList();
        }
        return noncurrentVersionTransitions;
    }

    public boolean hasNoncurrentVersionTransitions() {
        return noncurrentVersionTransitions != null;
    }

    public NoncurrentVersionExpiration getNoncurrentVersionExpiration() {
        return noncurrentVersionExpiration;
    }

    public AbortIncompleteMultipartUpload getAbortIncompleteMultipartUpload() {
        return abortIncompleteMultipartUpload;
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
        LifecycleRule that = (LifecycleRule) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.expiration, that.expiration)
               && Objects.equals(this.filter, that.filter)
               && Objects.equals(this.noncurrentVersionExpiration, that.noncurrentVersionExpiration)
               && Objects.equals(this.abortIncompleteMultipartUpload, that.abortIncompleteMultipartUpload)
               && Objects.equals(this.transitions, that.transitions)
               && Objects.equals(this.noncurrentVersionTransitions, that.noncurrentVersionTransitions);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(expiration);
        $hc = 31 * $hc + Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(filter);
        $hc = 31 * $hc + Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(transitions);
        $hc = 31 * $hc + Objects.hashCode(noncurrentVersionTransitions);
        $hc = 31 * $hc + Objects.hashCode(noncurrentVersionExpiration);
        $hc = 31 * $hc + Objects.hashCode(abortIncompleteMultipartUpload);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (expiration != null) {
            serializer.writeStruct($SCHEMA_EXPIRATION, expiration);
        }
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (filter != null) {
            serializer.writeStruct($SCHEMA_FILTER, filter);
        }
        serializer.writeString($SCHEMA_STATUS, status.getValue());
        if (transitions != null) {
            serializer.writeList($SCHEMA_TRANSITIONS, transitions, transitions.size(), SharedSerde.TransitionListSerializer.INSTANCE);
        }
        if (noncurrentVersionTransitions != null) {
            serializer.writeList($SCHEMA_NONCURRENT_VERSION_TRANSITIONS, noncurrentVersionTransitions, noncurrentVersionTransitions.size(), SharedSerde.NoncurrentVersionTransitionListSerializer.INSTANCE);
        }
        if (noncurrentVersionExpiration != null) {
            serializer.writeStruct($SCHEMA_NONCURRENT_VERSION_EXPIRATION, noncurrentVersionExpiration);
        }
        if (abortIncompleteMultipartUpload != null) {
            serializer.writeStruct($SCHEMA_ABORT_INCOMPLETE_MULTIPART_UPLOAD, abortIncompleteMultipartUpload);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, expiration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSITIONS, member, transitions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_NONCURRENT_VERSION_TRANSITIONS, member, noncurrentVersionTransitions);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_NONCURRENT_VERSION_EXPIRATION, member, noncurrentVersionExpiration);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_ABORT_INCOMPLETE_MULTIPART_UPLOAD, member, abortIncompleteMultipartUpload);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LifecycleRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.expiration(this.expiration);
        builder.id(this.id);
        builder.prefix(this.prefix);
        builder.filter(this.filter);
        builder.status(this.status);
        builder.transitions(this.transitions);
        builder.noncurrentVersionTransitions(this.noncurrentVersionTransitions);
        builder.noncurrentVersionExpiration(this.noncurrentVersionExpiration);
        builder.abortIncompleteMultipartUpload(this.abortIncompleteMultipartUpload);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LifecycleRule}.
     */
    public static final class Builder implements ShapeBuilder<LifecycleRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private LifecycleExpiration expiration;
        private String id;
        private String prefix;
        private LifecycleRuleFilter filter;
        private ExpirationStatus status;
        private List<Transition> transitions;
        private List<NoncurrentVersionTransition> noncurrentVersionTransitions;
        private NoncurrentVersionExpiration noncurrentVersionExpiration;
        private AbortIncompleteMultipartUpload abortIncompleteMultipartUpload;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the expiration for the lifecycle of the object in the form of date, days and, whether the object has a
         * delete marker.
         *
         * @return this builder.
         */
        public Builder expiration(LifecycleExpiration expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * Unique identifier for the rule. The value cannot be longer than 255 characters.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The general purpose bucket prefix that identifies one or more objects to which the rule applies. We recommend
         * using <code>Filter</code> instead of <code>Prefix</code> for new PUTs. Previous configurations where a prefix is
         * defined will continue to operate as before.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * @return this builder.
         * @deprecated
         */
        @Deprecated
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * The <code>Filter</code> is used to identify objects that a Lifecycle Rule applies to. A <code>Filter</code> must
         * have exactly one of <code>Prefix</code>, <code>Tag</code>, <code>ObjectSizeGreaterThan</code>, <code>
         * ObjectSizeLessThan</code>, or <code>And</code> specified. <code>Filter</code> is required if the <code>
         * LifecycleRule</code> does not contain a <code>Prefix</code> element.
         *
         * <p>For more information about <code>Tag</code> filters, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intro-lifecycle-filters.html">Adding filters to Lifecycle rules</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * <p><code>Tag</code> filters are not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder filter(LifecycleRuleFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * If 'Enabled', the rule is currently being applied. If 'Disabled', the rule is not currently being applied.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(ExpirationStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        /**
         * Specifies when an Amazon S3 object transitions to a specified storage class.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * @return this builder.
         */
        public Builder transitions(List<Transition> transitions) {
            this.transitions = transitions;
            return this;
        }

        /**
         * Specifies the transition rule for the lifecycle rule that describes when noncurrent objects transition to a
         * specific storage class. If your bucket is versioning-enabled (or versioning is suspended), you can set this
         * action to request that Amazon S3 transition noncurrent object versions to a specific storage class at a set
         * period in the object's lifetime.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * @return this builder.
         */
        public Builder noncurrentVersionTransitions(List<NoncurrentVersionTransition> noncurrentVersionTransitions) {
            this.noncurrentVersionTransitions = noncurrentVersionTransitions;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder noncurrentVersionExpiration(NoncurrentVersionExpiration noncurrentVersionExpiration) {
            this.noncurrentVersionExpiration = noncurrentVersionExpiration;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload abortIncompleteMultipartUpload) {
            this.abortIncompleteMultipartUpload = abortIncompleteMultipartUpload;
            return this;
        }

        @Override
        public LifecycleRule build() {
            tracker.validate();
            return new LifecycleRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((ExpirationStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 1 -> expiration((LifecycleExpiration) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, value));
                case 2 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 3 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 4 -> filter((LifecycleRuleFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                case 5 -> transitions((List<Transition>) SchemaUtils.validateSameMember($SCHEMA_TRANSITIONS, member, value));
                case 6 -> noncurrentVersionTransitions((List<NoncurrentVersionTransition>) SchemaUtils.validateSameMember($SCHEMA_NONCURRENT_VERSION_TRANSITIONS, member, value));
                case 7 -> noncurrentVersionExpiration((NoncurrentVersionExpiration) SchemaUtils.validateSameMember($SCHEMA_NONCURRENT_VERSION_EXPIRATION, member, value));
                case 8 -> abortIncompleteMultipartUpload((AbortIncompleteMultipartUpload) SchemaUtils.validateSameMember($SCHEMA_ABORT_INCOMPLETE_MULTIPART_UPLOAD, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<LifecycleRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(ExpirationStatus.unknown(""));
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
                    case 0 -> builder.status(ExpirationStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.expiration(LifecycleExpiration.builder().deserializeMember(de, member).build());
                    case 2 -> builder.id(de.readString(member));
                    case 3 -> builder.prefix(de.readString(member));
                    case 4 -> builder.filter(LifecycleRuleFilter.builder().deserializeMember(de, member).build());
                    case 5 -> builder.transitions(SharedSerde.deserializeTransitionList(member, de));
                    case 6 -> builder.noncurrentVersionTransitions(SharedSerde.deserializeNoncurrentVersionTransitionList(member, de));
                    case 7 -> builder.noncurrentVersionExpiration(NoncurrentVersionExpiration.builder().deserializeMember(de, member).build());
                    case 8 -> builder.abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
