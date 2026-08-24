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
public final class CreateBucketInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_BUCKET_INPUT;
    private static final Schema $SCHEMA_ACL = $SCHEMA.member("ACL");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CREATE_BUCKET_CONFIGURATION = $SCHEMA.member("CreateBucketConfiguration");
    private static final Schema $SCHEMA_GRANT_FULL_CONTROL = $SCHEMA.member("GrantFullControl");
    private static final Schema $SCHEMA_GRANT_READ = $SCHEMA.member("GrantRead");
    private static final Schema $SCHEMA_GRANT_READAC_P = $SCHEMA.member("GrantReadACP");
    private static final Schema $SCHEMA_GRANT_WRITE = $SCHEMA.member("GrantWrite");
    private static final Schema $SCHEMA_GRANT_WRITEAC_P = $SCHEMA.member("GrantWriteACP");
    private static final Schema $SCHEMA_OBJECT_LOCK_ENABLED_FOR_BUCKET = $SCHEMA.member("ObjectLockEnabledForBucket");
    private static final Schema $SCHEMA_OBJECT_OWNERSHIP = $SCHEMA.member("ObjectOwnership");
    private static final Schema $SCHEMA_BUCKET_NAMESPACE = $SCHEMA.member("BucketNamespace");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BucketCannedACL acl;
    private final transient String bucket;
    private final transient CreateBucketConfiguration createBucketConfiguration;
    private final transient String grantFullControl;
    private final transient String grantRead;
    private final transient String grantReadacP;
    private final transient String grantWrite;
    private final transient String grantWriteacP;
    private final transient Boolean objectLockEnabledForBucket;
    private final transient ObjectOwnership objectOwnership;
    private final transient BucketNamespace bucketNamespace;

    private CreateBucketInput(Builder builder) {
        this.acl = builder.acl;
        this.bucket = builder.bucket;
        this.createBucketConfiguration = builder.createBucketConfiguration;
        this.grantFullControl = builder.grantFullControl;
        this.grantRead = builder.grantRead;
        this.grantReadacP = builder.grantReadacP;
        this.grantWrite = builder.grantWrite;
        this.grantWriteacP = builder.grantWriteacP;
        this.objectLockEnabledForBucket = builder.objectLockEnabledForBucket;
        this.objectOwnership = builder.objectOwnership;
        this.bucketNamespace = builder.bucketNamespace;
    }

    /**
     * The canned ACL to apply to the bucket.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public BucketCannedACL getAcl() {
        return acl;
    }

    /**
     * The name of the bucket to create.
     *
     * <p><b>General purpose buckets</b> - For information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">Bucket naming rules</a>
     * in the <i>Amazon S3 User Guide</i>.
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
     * The configuration information for the bucket.
     */
    public CreateBucketConfiguration getCreateBucketConfiguration() {
        return createBucketConfiguration;
    }

    /**
     * Allows grantee the read, write, read ACP, and write ACP permissions on the bucket.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getGrantFullControl() {
        return grantFullControl;
    }

    /**
     * Allows grantee to list the objects in the bucket.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getGrantRead() {
        return grantRead;
    }

    /**
     * Allows grantee to read the bucket ACL.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getGrantReadacP() {
        return grantReadacP;
    }

    /**
     * Allows grantee to create new objects in the bucket.
     *
     * <p>For the bucket and object owners of existing objects, also allows deletions and overwrites of those objects.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getGrantWrite() {
        return grantWrite;
    }

    /**
     * Allows grantee to write the ACL for the applicable bucket.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getGrantWriteacP() {
        return grantWriteacP;
    }

    /**
     * Specifies whether you want S3 Object Lock to be enabled for the new bucket.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Boolean isObjectLockEnabledForBucket() {
        return objectLockEnabledForBucket;
    }

    public ObjectOwnership getObjectOwnership() {
        return objectOwnership;
    }

    /**
     * Specifies the namespace where you want to create your general purpose bucket. When you create a general purpose
     * bucket, you can choose to create a bucket in the shared global namespace or you can choose to create a bucket in
     * your account regional namespace. Your account regional namespace is a subdivision of the global namespace that
     * only your account can create buckets in. For more information on bucket namespaces, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/gpbucketnamespaces.html">Namespaces for general
     * purpose buckets</a>.
     *
     * <p>General purpose buckets in your account regional namespace must follow a specific naming convention. These
     * buckets consist of a bucket name prefix that you create, and a suffix that contains your 12-digit Amazon Web
     * Services Account ID, the Amazon Web Services Region code, and ends with <code>-an</code>. Bucket names must
     * follow the format <code>bucket-name-prefix-accountId-region-an</code> (for example, <code>
     * amzn-s3-demo-bucket-111122223333-us-west-2-an</code>). For information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html#account-regional-naming-rules">
     * Account regional namespace naming rules</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public BucketNamespace getBucketNamespace() {
        return bucketNamespace;
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
        CreateBucketInput that = (CreateBucketInput) other;
        return Objects.equals(this.objectLockEnabledForBucket, that.objectLockEnabledForBucket)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.grantFullControl, that.grantFullControl)
               && Objects.equals(this.grantRead, that.grantRead)
               && Objects.equals(this.grantReadacP, that.grantReadacP)
               && Objects.equals(this.grantWrite, that.grantWrite)
               && Objects.equals(this.grantWriteacP, that.grantWriteacP)
               && Objects.equals(this.acl, that.acl)
               && Objects.equals(this.objectOwnership, that.objectOwnership)
               && Objects.equals(this.bucketNamespace, that.bucketNamespace)
               && Objects.equals(this.createBucketConfiguration, that.createBucketConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(acl);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(createBucketConfiguration);
        $hc = 31 * $hc + Objects.hashCode(grantFullControl);
        $hc = 31 * $hc + Objects.hashCode(grantRead);
        $hc = 31 * $hc + Objects.hashCode(grantReadacP);
        $hc = 31 * $hc + Objects.hashCode(grantWrite);
        $hc = 31 * $hc + Objects.hashCode(grantWriteacP);
        $hc = 31 * $hc + Objects.hashCode(objectLockEnabledForBucket);
        $hc = 31 * $hc + Objects.hashCode(objectOwnership);
        $hc = 31 * $hc + Objects.hashCode(bucketNamespace);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (acl != null) {
            serializer.writeString($SCHEMA_ACL, acl.getValue());
        }
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (createBucketConfiguration != null) {
            serializer.writeStruct($SCHEMA_CREATE_BUCKET_CONFIGURATION, createBucketConfiguration);
        }
        if (grantFullControl != null) {
            serializer.writeString($SCHEMA_GRANT_FULL_CONTROL, grantFullControl);
        }
        if (grantRead != null) {
            serializer.writeString($SCHEMA_GRANT_READ, grantRead);
        }
        if (grantReadacP != null) {
            serializer.writeString($SCHEMA_GRANT_READAC_P, grantReadacP);
        }
        if (grantWrite != null) {
            serializer.writeString($SCHEMA_GRANT_WRITE, grantWrite);
        }
        if (grantWriteacP != null) {
            serializer.writeString($SCHEMA_GRANT_WRITEAC_P, grantWriteacP);
        }
        if (objectLockEnabledForBucket != null) {
            serializer.writeBoolean($SCHEMA_OBJECT_LOCK_ENABLED_FOR_BUCKET, objectLockEnabledForBucket);
        }
        if (objectOwnership != null) {
            serializer.writeString($SCHEMA_OBJECT_OWNERSHIP, objectOwnership.getValue());
        }
        if (bucketNamespace != null) {
            serializer.writeString($SCHEMA_BUCKET_NAMESPACE, bucketNamespace.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACL, member, acl);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATE_BUCKET_CONFIGURATION, member, createBucketConfiguration);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, grantFullControl);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, grantRead);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, grantReadacP);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITE, member, grantWrite);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, grantWriteacP);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_ENABLED_FOR_BUCKET, member, objectLockEnabledForBucket);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_OWNERSHIP, member, objectOwnership);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_NAMESPACE, member, bucketNamespace);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateBucketInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.acl(this.acl);
        builder.bucket(this.bucket);
        builder.createBucketConfiguration(this.createBucketConfiguration);
        builder.grantFullControl(this.grantFullControl);
        builder.grantRead(this.grantRead);
        builder.grantReadacP(this.grantReadacP);
        builder.grantWrite(this.grantWrite);
        builder.grantWriteacP(this.grantWriteacP);
        builder.objectLockEnabledForBucket(this.objectLockEnabledForBucket);
        builder.objectOwnership(this.objectOwnership);
        builder.bucketNamespace(this.bucketNamespace);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateBucketInput}.
     */
    public static final class Builder implements ShapeBuilder<CreateBucketInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private BucketCannedACL acl;
        private String bucket;
        private CreateBucketConfiguration createBucketConfiguration;
        private String grantFullControl;
        private String grantRead;
        private String grantReadacP;
        private String grantWrite;
        private String grantWriteacP;
        private Boolean objectLockEnabledForBucket;
        private ObjectOwnership objectOwnership;
        private BucketNamespace bucketNamespace;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The canned ACL to apply to the bucket.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder acl(BucketCannedACL acl) {
            this.acl = acl;
            return this;
        }

        /**
         * The name of the bucket to create.
         *
         * <p><b>General purpose buckets</b> - For information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">Bucket naming rules</a>
         * in the <i>Amazon S3 User Guide</i>.
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
         * The configuration information for the bucket.
         *
         * @return this builder.
         */
        public Builder createBucketConfiguration(CreateBucketConfiguration createBucketConfiguration) {
            this.createBucketConfiguration = createBucketConfiguration;
            return this;
        }

        /**
         * Allows grantee the read, write, read ACP, and write ACP permissions on the bucket.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder grantFullControl(String grantFullControl) {
            this.grantFullControl = grantFullControl;
            return this;
        }

        /**
         * Allows grantee to list the objects in the bucket.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder grantRead(String grantRead) {
            this.grantRead = grantRead;
            return this;
        }

        /**
         * Allows grantee to read the bucket ACL.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder grantReadacP(String grantReadacP) {
            this.grantReadacP = grantReadacP;
            return this;
        }

        /**
         * Allows grantee to create new objects in the bucket.
         *
         * <p>For the bucket and object owners of existing objects, also allows deletions and overwrites of those objects.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder grantWrite(String grantWrite) {
            this.grantWrite = grantWrite;
            return this;
        }

        /**
         * Allows grantee to write the ACL for the applicable bucket.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder grantWriteacP(String grantWriteacP) {
            this.grantWriteacP = grantWriteacP;
            return this;
        }

        /**
         * Specifies whether you want S3 Object Lock to be enabled for the new bucket.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder objectLockEnabledForBucket(Boolean objectLockEnabledForBucket) {
            this.objectLockEnabledForBucket = objectLockEnabledForBucket;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder objectOwnership(ObjectOwnership objectOwnership) {
            this.objectOwnership = objectOwnership;
            return this;
        }

        /**
         * Specifies the namespace where you want to create your general purpose bucket. When you create a general purpose
         * bucket, you can choose to create a bucket in the shared global namespace or you can choose to create a bucket in
         * your account regional namespace. Your account regional namespace is a subdivision of the global namespace that
         * only your account can create buckets in. For more information on bucket namespaces, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/gpbucketnamespaces.html">Namespaces for general
         * purpose buckets</a>.
         *
         * <p>General purpose buckets in your account regional namespace must follow a specific naming convention. These
         * buckets consist of a bucket name prefix that you create, and a suffix that contains your 12-digit Amazon Web
         * Services Account ID, the Amazon Web Services Region code, and ends with <code>-an</code>. Bucket names must
         * follow the format <code>bucket-name-prefix-accountId-region-an</code> (for example, <code>
         * amzn-s3-demo-bucket-111122223333-us-west-2-an</code>). For information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html#account-regional-naming-rules">
         * Account regional namespace naming rules</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder bucketNamespace(BucketNamespace bucketNamespace) {
            this.bucketNamespace = bucketNamespace;
            return this;
        }

        @Override
        public CreateBucketInput build() {
            tracker.validate();
            return new CreateBucketInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> acl((BucketCannedACL) SchemaUtils.validateSameMember($SCHEMA_ACL, member, value));
                case 2 -> createBucketConfiguration((CreateBucketConfiguration) SchemaUtils.validateSameMember($SCHEMA_CREATE_BUCKET_CONFIGURATION, member, value));
                case 3 -> grantFullControl((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, value));
                case 4 -> grantRead((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, value));
                case 5 -> grantReadacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, value));
                case 6 -> grantWrite((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITE, member, value));
                case 7 -> grantWriteacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, value));
                case 8 -> objectLockEnabledForBucket((Boolean) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_ENABLED_FOR_BUCKET, member, value));
                case 9 -> objectOwnership((ObjectOwnership) SchemaUtils.validateSameMember($SCHEMA_OBJECT_OWNERSHIP, member, value));
                case 10 -> bucketNamespace((BucketNamespace) SchemaUtils.validateSameMember($SCHEMA_BUCKET_NAMESPACE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateBucketInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
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
                    case 1 -> builder.acl(BucketCannedACL.builder().deserializeMember(de, member).build());
                    case 2 -> builder.createBucketConfiguration(CreateBucketConfiguration.builder().deserializeMember(de, member).build());
                    case 3 -> builder.grantFullControl(de.readString(member));
                    case 4 -> builder.grantRead(de.readString(member));
                    case 5 -> builder.grantReadacP(de.readString(member));
                    case 6 -> builder.grantWrite(de.readString(member));
                    case 7 -> builder.grantWriteacP(de.readString(member));
                    case 8 -> builder.objectLockEnabledForBucket(de.readBoolean(member));
                    case 9 -> builder.objectOwnership(ObjectOwnership.builder().deserializeMember(de, member).build());
                    case 10 -> builder.bucketNamespace(BucketNamespace.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
