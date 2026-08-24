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
 * Describes where logs are stored and the prefix that Amazon S3 assigns to all log object keys for a bucket. For more
 * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTlogging.html">PUT Bucket logging</a> in the <i>Amazon S3 API Reference</i>.
 */
@SmithyGenerated
public final class LoggingEnabled implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.LOGGING_ENABLED;
    private static final Schema $SCHEMA_TARGET_BUCKET = $SCHEMA.member("TargetBucket");
    private static final Schema $SCHEMA_TARGET_GRANTS = $SCHEMA.member("TargetGrants");
    private static final Schema $SCHEMA_TARGET_PREFIX = $SCHEMA.member("TargetPrefix");
    private static final Schema $SCHEMA_TARGET_OBJECT_KEY_FORMAT = $SCHEMA.member("TargetObjectKeyFormat");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String targetBucket;
    private final transient List<TargetGrant> targetGrants;
    private final transient String targetPrefix;
    private final transient TargetObjectKeyFormat targetObjectKeyFormat;

    private LoggingEnabled(Builder builder) {
        this.targetBucket = builder.targetBucket;
        this.targetGrants = builder.targetGrants == null ? null : Collections.unmodifiableList(builder.targetGrants);
        this.targetPrefix = builder.targetPrefix;
        this.targetObjectKeyFormat = builder.targetObjectKeyFormat;
    }

    /**
     * Specifies the bucket where you want Amazon S3 to store server access logs. You can have your logs delivered to
     * any bucket that you own, including the same bucket that is being logged. You can also configure multiple buckets
     * to deliver their logs to the same target bucket. In this case, you should choose a different
     * <code>TargetPrefix</code> for each source bucket so that the delivered log files can be distinguished by key.
     */
    public String getTargetBucket() {
        return targetBucket;
    }

    /**
     * Container for granting information.
     *
     * <p>Buckets that use the bucket owner enforced setting for Object Ownership don't support target grants. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/enable-server-access-logging.html#grant-log-delivery-permissions-general">Permissions for server access log delivery</a> in the <i>Amazon S3 User Guide</i>.
     */
    public List<TargetGrant> getTargetGrants() {
        if (targetGrants == null) {
            return Collections.emptyList();
        }
        return targetGrants;
    }

    public boolean hasTargetGrants() {
        return targetGrants != null;
    }

    /**
     * A prefix for all log object keys. If you store log files from multiple Amazon S3 buckets in a single bucket, you
     * can use a prefix to distinguish which log files came from which bucket.
     */
    public String getTargetPrefix() {
        return targetPrefix;
    }

    /**
     * Amazon S3 key format for log objects.
     */
    public TargetObjectKeyFormat getTargetObjectKeyFormat() {
        return targetObjectKeyFormat;
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
        LoggingEnabled that = (LoggingEnabled) other;
        return Objects.equals(this.targetBucket, that.targetBucket)
               && Objects.equals(this.targetPrefix, that.targetPrefix)
               && Objects.equals(this.targetObjectKeyFormat, that.targetObjectKeyFormat)
               && Objects.equals(this.targetGrants, that.targetGrants);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(targetBucket);
        $hc = 31 * $hc + Objects.hashCode(targetGrants);
        $hc = 31 * $hc + Objects.hashCode(targetPrefix);
        $hc = 31 * $hc + Objects.hashCode(targetObjectKeyFormat);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TARGET_BUCKET, targetBucket);
        if (targetGrants != null) {
            serializer.writeList($SCHEMA_TARGET_GRANTS, targetGrants, targetGrants.size(), SharedSerde.TargetGrantsSerializer.INSTANCE);
        }
        serializer.writeString($SCHEMA_TARGET_PREFIX, targetPrefix);
        if (targetObjectKeyFormat != null) {
            serializer.writeStruct($SCHEMA_TARGET_OBJECT_KEY_FORMAT, targetObjectKeyFormat);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_BUCKET, member, targetBucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_PREFIX, member, targetPrefix);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_GRANTS, member, targetGrants);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_OBJECT_KEY_FORMAT, member, targetObjectKeyFormat);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LoggingEnabled}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.targetBucket(this.targetBucket);
        builder.targetGrants(this.targetGrants);
        builder.targetPrefix(this.targetPrefix);
        builder.targetObjectKeyFormat(this.targetObjectKeyFormat);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LoggingEnabled}.
     */
    public static final class Builder implements ShapeBuilder<LoggingEnabled> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String targetBucket;
        private List<TargetGrant> targetGrants;
        private String targetPrefix;
        private TargetObjectKeyFormat targetObjectKeyFormat;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the bucket where you want Amazon S3 to store server access logs. You can have your logs delivered to
         * any bucket that you own, including the same bucket that is being logged. You can also configure multiple buckets
         * to deliver their logs to the same target bucket. In this case, you should choose a different
         * <code>TargetPrefix</code> for each source bucket so that the delivered log files can be distinguished by key.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder targetBucket(String targetBucket) {
            this.targetBucket = Objects.requireNonNull(targetBucket, "targetBucket cannot be null");
            tracker.setMember($SCHEMA_TARGET_BUCKET);
            return this;
        }

        /**
         * Container for granting information.
         *
         * <p>Buckets that use the bucket owner enforced setting for Object Ownership don't support target grants. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/enable-server-access-logging.html#grant-log-delivery-permissions-general">Permissions for server access log delivery</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder targetGrants(List<TargetGrant> targetGrants) {
            this.targetGrants = targetGrants;
            return this;
        }

        /**
         * A prefix for all log object keys. If you store log files from multiple Amazon S3 buckets in a single bucket, you
         * can use a prefix to distinguish which log files came from which bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder targetPrefix(String targetPrefix) {
            this.targetPrefix = Objects.requireNonNull(targetPrefix, "targetPrefix cannot be null");
            tracker.setMember($SCHEMA_TARGET_PREFIX);
            return this;
        }

        /**
         * Amazon S3 key format for log objects.
         *
         * @return this builder.
         */
        public Builder targetObjectKeyFormat(TargetObjectKeyFormat targetObjectKeyFormat) {
            this.targetObjectKeyFormat = targetObjectKeyFormat;
            return this;
        }

        @Override
        public LoggingEnabled build() {
            tracker.validate();
            return new LoggingEnabled(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> targetBucket((String) SchemaUtils.validateSameMember($SCHEMA_TARGET_BUCKET, member, value));
                case 1 -> targetPrefix((String) SchemaUtils.validateSameMember($SCHEMA_TARGET_PREFIX, member, value));
                case 2 -> targetGrants((List<TargetGrant>) SchemaUtils.validateSameMember($SCHEMA_TARGET_GRANTS, member, value));
                case 3 -> targetObjectKeyFormat((TargetObjectKeyFormat) SchemaUtils.validateSameMember($SCHEMA_TARGET_OBJECT_KEY_FORMAT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<LoggingEnabled> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TARGET_BUCKET)) {
                targetBucket("");
            }
            if (!tracker.checkMember($SCHEMA_TARGET_PREFIX)) {
                targetPrefix("");
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
                    case 0 -> builder.targetBucket(de.readString(member));
                    case 1 -> builder.targetPrefix(de.readString(member));
                    case 2 -> builder.targetGrants(SharedSerde.deserializeTargetGrants(member, de));
                    case 3 -> builder.targetObjectKeyFormat(TargetObjectKeyFormat.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
