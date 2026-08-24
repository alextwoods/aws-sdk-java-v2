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
 * Contains the bucket name, file format, bucket owner (optional), and prefix (optional) where S3 Inventory results are
 * published.
 */
@SmithyGenerated
public final class InventoryS3BucketDestination implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_S3_BUCKET_DESTINATION;
    private static final Schema $SCHEMA_ACCOUNT_ID = $SCHEMA.member("AccountId");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_FORMAT = $SCHEMA.member("Format");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_ENCRYPTION = $SCHEMA.member("Encryption");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String accountId;
    private final transient String bucket;
    private final transient InventoryFormat format;
    private final transient String prefix;
    private final transient InventoryEncryption encryption;

    private InventoryS3BucketDestination(Builder builder) {
        this.accountId = builder.accountId;
        this.bucket = builder.bucket;
        this.format = builder.format;
        this.prefix = builder.prefix;
        this.encryption = builder.encryption;
    }

    /**
     * The account ID that owns the destination S3 bucket. If no account ID is provided, the owner is not validated
     * before exporting data.
     *
     * <p> Although this value is optional, we strongly recommend that you set it to help prevent problems if the
     * destination bucket ownership changes.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * The Amazon Resource Name (ARN) of the bucket where inventory results will be published.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Specifies the output format of the inventory results.
     */
    public InventoryFormat getFormat() {
        return format;
    }

    /**
     * The prefix that is prepended to all inventory results.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Contains the type of server-side encryption used to encrypt the inventory results.
     */
    public InventoryEncryption getEncryption() {
        return encryption;
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
        InventoryS3BucketDestination that = (InventoryS3BucketDestination) other;
        return Objects.equals(this.accountId, that.accountId)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.format, that.format)
               && Objects.equals(this.encryption, that.encryption);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(accountId);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(format);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(encryption);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (accountId != null) {
            serializer.writeString($SCHEMA_ACCOUNT_ID, accountId);
        }
        serializer.writeString($SCHEMA_BUCKET, bucket);
        serializer.writeString($SCHEMA_FORMAT, format.getValue());
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (encryption != null) {
            serializer.writeStruct($SCHEMA_ENCRYPTION, encryption);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_FORMAT, member, format);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_ID, member, accountId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION, member, encryption);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryS3BucketDestination}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.accountId(this.accountId);
        builder.bucket(this.bucket);
        builder.format(this.format);
        builder.prefix(this.prefix);
        builder.encryption(this.encryption);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryS3BucketDestination}.
     */
    public static final class Builder implements ShapeBuilder<InventoryS3BucketDestination> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String accountId;
        private String bucket;
        private InventoryFormat format;
        private String prefix;
        private InventoryEncryption encryption;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The account ID that owns the destination S3 bucket. If no account ID is provided, the owner is not validated
         * before exporting data.
         *
         * <p> Although this value is optional, we strongly recommend that you set it to help prevent problems if the
         * destination bucket ownership changes.
         *
         * @return this builder.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the bucket where inventory results will be published.
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
         * Specifies the output format of the inventory results.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder format(InventoryFormat format) {
            this.format = Objects.requireNonNull(format, "format cannot be null");
            tracker.setMember($SCHEMA_FORMAT);
            return this;
        }

        /**
         * The prefix that is prepended to all inventory results.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Contains the type of server-side encryption used to encrypt the inventory results.
         *
         * @return this builder.
         */
        public Builder encryption(InventoryEncryption encryption) {
            this.encryption = encryption;
            return this;
        }

        @Override
        public InventoryS3BucketDestination build() {
            tracker.validate();
            return new InventoryS3BucketDestination(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> format((InventoryFormat) SchemaUtils.validateSameMember($SCHEMA_FORMAT, member, value));
                case 2 -> accountId((String) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_ID, member, value));
                case 3 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 4 -> encryption((InventoryEncryption) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventoryS3BucketDestination> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_FORMAT)) {
                format(InventoryFormat.unknown(""));
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
                    case 1 -> builder.format(InventoryFormat.builder().deserializeMember(de, member).build());
                    case 2 -> builder.accountId(de.readString(member));
                    case 3 -> builder.prefix(de.readString(member));
                    case 4 -> builder.encryption(InventoryEncryption.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
