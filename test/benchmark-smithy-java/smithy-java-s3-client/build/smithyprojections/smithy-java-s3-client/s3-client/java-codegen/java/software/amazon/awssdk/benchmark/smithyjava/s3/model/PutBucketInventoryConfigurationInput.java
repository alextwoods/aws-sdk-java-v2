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
public final class PutBucketInventoryConfigurationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.PUT_BUCKET_INVENTORY_CONFIGURATION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_INVENTORY_CONFIGURATION = $SCHEMA.member("InventoryConfiguration");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String id;
    private final transient InventoryConfiguration inventoryConfiguration;
    private final transient String expectedBucketOwner;

    private PutBucketInventoryConfigurationInput(Builder builder) {
        this.bucket = builder.bucket;
        this.id = builder.id;
        this.inventoryConfiguration = builder.inventoryConfiguration;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The name of the bucket where the inventory configuration will be stored.
     *
     * <p><b>Directory buckets </b> - When you use this operation with a directory bucket, you must use path-style
     * requests in the format <code>https://s3express-control.<i>region-code</i>.amazonaws.com/<i>bucket-name</i></code>
     * . Virtual-hosted-style requests aren't supported. Directory bucket names must be unique in the chosen Zone
     * (Availability Zone or Local Zone). Bucket names must also follow the format <code><i>bucket-base-name</i>--<i>
     * zone-id</i>--x-s3</code> (for example, <code><i>DOC-EXAMPLE-BUCKET</i>--<i>usw2-az1</i>--x-s3</code>). For
     * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
     * Guide</i>
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The ID used to identify the inventory configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * Specifies the inventory configuration.
     */
    public InventoryConfiguration getInventoryConfiguration() {
        return inventoryConfiguration;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     *
     * <p>For directory buckets, this header is not supported in this API operation. If you specify this header, the
     * request fails with the HTTP status code <code>501 Not Implemented</code>.
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
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
        PutBucketInventoryConfigurationInput that = (PutBucketInventoryConfigurationInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.id, that.id)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.inventoryConfiguration, that.inventoryConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(inventoryConfiguration);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        serializer.writeString($SCHEMA_ID, id);
        if (inventoryConfiguration != null) {
            serializer.writeStruct($SCHEMA_INVENTORY_CONFIGURATION, inventoryConfiguration);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_CONFIGURATION, member, inventoryConfiguration);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutBucketInventoryConfigurationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.id(this.id);
        builder.inventoryConfiguration(this.inventoryConfiguration);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutBucketInventoryConfigurationInput}.
     */
    public static final class Builder implements ShapeBuilder<PutBucketInventoryConfigurationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String id;
        private InventoryConfiguration inventoryConfiguration;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket where the inventory configuration will be stored.
         *
         * <p><b>Directory buckets </b> - When you use this operation with a directory bucket, you must use path-style
         * requests in the format <code>https://s3express-control.<i>region-code</i>.amazonaws.com/<i>bucket-name</i></code>
         * . Virtual-hosted-style requests aren't supported. Directory bucket names must be unique in the chosen Zone
         * (Availability Zone or Local Zone). Bucket names must also follow the format <code><i>bucket-base-name</i>--<i>
         * zone-id</i>--x-s3</code> (for example, <code><i>DOC-EXAMPLE-BUCKET</i>--<i>usw2-az1</i>--x-s3</code>). For
         * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
         * Guide</i>
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
         * The ID used to identify the inventory configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            tracker.setMember($SCHEMA_ID);
            return this;
        }

        /**
         * Specifies the inventory configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder inventoryConfiguration(InventoryConfiguration inventoryConfiguration) {
            this.inventoryConfiguration = Objects.requireNonNull(inventoryConfiguration, "inventoryConfiguration cannot be null");
            tracker.setMember($SCHEMA_INVENTORY_CONFIGURATION);
            return this;
        }

        /**
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * <p>For directory buckets, this header is not supported in this API operation. If you specify this header, the
         * request fails with the HTTP status code <code>501 Not Implemented</code>.
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        @Override
        public PutBucketInventoryConfigurationInput build() {
            tracker.validate();
            return new PutBucketInventoryConfigurationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 2 -> inventoryConfiguration((InventoryConfiguration) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_CONFIGURATION, member, value));
                case 3 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutBucketInventoryConfigurationInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_ID)) {
                id("");
            }
            if (!tracker.checkMember($SCHEMA_INVENTORY_CONFIGURATION)) {
                tracker.setMember($SCHEMA_INVENTORY_CONFIGURATION);
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
                    case 1 -> builder.id(de.readString(member));
                    case 2 -> builder.inventoryConfiguration(InventoryConfiguration.builder().deserializeMember(de, member).build());
                    case 3 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
