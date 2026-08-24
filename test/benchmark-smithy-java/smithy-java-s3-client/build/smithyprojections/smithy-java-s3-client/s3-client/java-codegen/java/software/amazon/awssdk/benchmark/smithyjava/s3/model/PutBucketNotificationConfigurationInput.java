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

@SmithyGenerated
public final class PutBucketNotificationConfigurationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.PUT_BUCKET_NOTIFICATION_CONFIGURATION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_NOTIFICATION_CONFIGURATION = $SCHEMA.member("NotificationConfiguration");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_SKIP_DESTINATION_VALIDATION = $SCHEMA.member("SkipDestinationValidation");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient NotificationConfiguration notificationConfiguration;
    private final transient String expectedBucketOwner;
    private final transient Boolean skipDestinationValidation;

    private PutBucketNotificationConfigurationInput(Builder builder) {
        this.bucket = builder.bucket;
        this.notificationConfiguration = builder.notificationConfiguration;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.skipDestinationValidation = builder.skipDestinationValidation;
    }

    /**
     * The name of the bucket.
     */
    public String getBucket() {
        return bucket;
    }

    public NotificationConfiguration getNotificationConfiguration() {
        return notificationConfiguration;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * Skips validation of Amazon SQS, Amazon SNS, and Lambda destinations. True or false value.
     */
    public Boolean isSkipDestinationValidation() {
        return skipDestinationValidation;
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
        PutBucketNotificationConfigurationInput that = (PutBucketNotificationConfigurationInput) other;
        return Objects.equals(this.skipDestinationValidation, that.skipDestinationValidation)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.notificationConfiguration, that.notificationConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(notificationConfiguration);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(skipDestinationValidation);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (notificationConfiguration != null) {
            serializer.writeStruct($SCHEMA_NOTIFICATION_CONFIGURATION, notificationConfiguration);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (skipDestinationValidation != null) {
            serializer.writeBoolean($SCHEMA_SKIP_DESTINATION_VALIDATION, skipDestinationValidation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NOTIFICATION_CONFIGURATION, member, notificationConfiguration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SKIP_DESTINATION_VALIDATION, member, skipDestinationValidation);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutBucketNotificationConfigurationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.notificationConfiguration(this.notificationConfiguration);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.skipDestinationValidation(this.skipDestinationValidation);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutBucketNotificationConfigurationInput}.
     */
    public static final class Builder implements ShapeBuilder<PutBucketNotificationConfigurationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private NotificationConfiguration notificationConfiguration;
        private String expectedBucketOwner;
        private Boolean skipDestinationValidation;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = Objects.requireNonNull(bucket, "bucket cannot be null");
            tracker.setMember($SCHEMA_BUCKET);
            return this;
        }

        /**
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder notificationConfiguration(NotificationConfiguration notificationConfiguration) {
            this.notificationConfiguration = Objects.requireNonNull(notificationConfiguration, "notificationConfiguration cannot be null");
            tracker.setMember($SCHEMA_NOTIFICATION_CONFIGURATION);
            return this;
        }

        /**
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        /**
         * Skips validation of Amazon SQS, Amazon SNS, and Lambda destinations. True or false value.
         *
         * @return this builder.
         */
        public Builder skipDestinationValidation(Boolean skipDestinationValidation) {
            this.skipDestinationValidation = skipDestinationValidation;
            return this;
        }

        @Override
        public PutBucketNotificationConfigurationInput build() {
            tracker.validate();
            return new PutBucketNotificationConfigurationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> notificationConfiguration((NotificationConfiguration) SchemaUtils.validateSameMember($SCHEMA_NOTIFICATION_CONFIGURATION, member, value));
                case 2 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 3 -> skipDestinationValidation((Boolean) SchemaUtils.validateSameMember($SCHEMA_SKIP_DESTINATION_VALIDATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutBucketNotificationConfigurationInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_NOTIFICATION_CONFIGURATION)) {
                tracker.setMember($SCHEMA_NOTIFICATION_CONFIGURATION);
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
                    case 0 -> builder.bucket(de.readString(member));
                    case 1 -> builder.notificationConfiguration(NotificationConfiguration.builder().deserializeMember(de, member).build());
                    case 2 -> builder.expectedBucketOwner(de.readString(member));
                    case 3 -> builder.skipDestinationValidation(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
