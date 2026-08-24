package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
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
 * The PublicAccessBlock configuration that you want to apply to this Amazon S3 bucket. You can enable the configuration
 * options in any combination. Bucket-level settings work alongside account-level settings (which may inherit from
 * organization-level policies). For more information about when Amazon S3 considers a bucket or object public, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/access-control-block-public-access.html#access-control-block-public-access-policy-status">
 * The Meaning of "Public"</a> in the <i>Amazon S3 User Guide</i>.
 */
@SmithyGenerated
public final class PublicAccessBlockConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.PUBLIC_ACCESS_BLOCK_CONFIGURATION;
    private static final Schema $SCHEMA_BLOCK_PUBLIC_ACLS = $SCHEMA.member("BlockPublicAcls");
    private static final Schema $SCHEMA_IGNORE_PUBLIC_ACLS = $SCHEMA.member("IgnorePublicAcls");
    private static final Schema $SCHEMA_BLOCK_PUBLIC_POLICY = $SCHEMA.member("BlockPublicPolicy");
    private static final Schema $SCHEMA_RESTRICT_PUBLIC_BUCKETS = $SCHEMA.member("RestrictPublicBuckets");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean blockPublicAcls;
    private final transient Boolean ignorePublicAcls;
    private final transient Boolean blockPublicPolicy;
    private final transient Boolean restrictPublicBuckets;

    private PublicAccessBlockConfiguration(Builder builder) {
        this.blockPublicAcls = builder.blockPublicAcls;
        this.ignorePublicAcls = builder.ignorePublicAcls;
        this.blockPublicPolicy = builder.blockPublicPolicy;
        this.restrictPublicBuckets = builder.restrictPublicBuckets;
    }

    /**
     * Specifies whether Amazon S3 should block public access control lists (ACLs) for this bucket and objects in this
     * bucket. Setting this element to <code>TRUE</code> causes the following behavior:
     *
     * <ul>
     *   <li>
     *     PUT Bucket ACL and PUT Object ACL calls fail if the specified ACL is public.
     *   </li>
     *   <li>
     *     PUT Object calls fail if the request includes a public ACL.
     *   </li>
     *   <li>
     *     PUT Bucket calls fail if the request includes a public ACL.
     *   </li>
     * </ul>
     *
     * <p>Enabling this setting doesn't affect existing policies or ACLs.
     */
    public Boolean isBlockPublicAcls() {
        return blockPublicAcls;
    }

    /**
     * Specifies whether Amazon S3 should ignore public ACLs for this bucket and objects in this bucket. Setting this
     * element to <code>TRUE</code> causes Amazon S3 to ignore all public ACLs on this bucket and objects in this
     * bucket.
     *
     * <p>Enabling this setting doesn't affect the persistence of any existing ACLs and doesn't prevent new public ACLs
     * from being set.
     */
    public Boolean isIgnorePublicAcls() {
        return ignorePublicAcls;
    }

    /**
     * Specifies whether Amazon S3 should block public bucket policies for this bucket. Setting this element to <code>
     * TRUE</code> causes Amazon S3 to reject calls to PUT Bucket policy if the specified bucket policy allows public
     * access.
     *
     * <p>Enabling this setting doesn't affect existing bucket policies.
     */
    public Boolean isBlockPublicPolicy() {
        return blockPublicPolicy;
    }

    /**
     * Specifies whether Amazon S3 should restrict public bucket policies for this bucket. Setting this element to <code>
     * TRUE</code> restricts access to this bucket to only Amazon Web Services service principals and authorized users
     * within this account if the bucket has a public policy.
     *
     * <p>Enabling this setting doesn't affect previously stored bucket policies, except that public and cross-account
     * access within any public bucket policy, including non-public delegation to specific accounts, is blocked.
     */
    public Boolean isRestrictPublicBuckets() {
        return restrictPublicBuckets;
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
        PublicAccessBlockConfiguration that = (PublicAccessBlockConfiguration) other;
        return Objects.equals(this.blockPublicAcls, that.blockPublicAcls)
               && Objects.equals(this.ignorePublicAcls, that.ignorePublicAcls)
               && Objects.equals(this.blockPublicPolicy, that.blockPublicPolicy)
               && Objects.equals(this.restrictPublicBuckets, that.restrictPublicBuckets);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(blockPublicAcls);
        $hc = 31 * $hc + Objects.hashCode(ignorePublicAcls);
        $hc = 31 * $hc + Objects.hashCode(blockPublicPolicy);
        $hc = 31 * $hc + Objects.hashCode(restrictPublicBuckets);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (blockPublicAcls != null) {
            serializer.writeBoolean($SCHEMA_BLOCK_PUBLIC_ACLS, blockPublicAcls);
        }
        if (ignorePublicAcls != null) {
            serializer.writeBoolean($SCHEMA_IGNORE_PUBLIC_ACLS, ignorePublicAcls);
        }
        if (blockPublicPolicy != null) {
            serializer.writeBoolean($SCHEMA_BLOCK_PUBLIC_POLICY, blockPublicPolicy);
        }
        if (restrictPublicBuckets != null) {
            serializer.writeBoolean($SCHEMA_RESTRICT_PUBLIC_BUCKETS, restrictPublicBuckets);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BLOCK_PUBLIC_ACLS, member, blockPublicAcls);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_IGNORE_PUBLIC_ACLS, member, ignorePublicAcls);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BLOCK_PUBLIC_POLICY, member, blockPublicPolicy);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTRICT_PUBLIC_BUCKETS, member, restrictPublicBuckets);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PublicAccessBlockConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.blockPublicAcls(this.blockPublicAcls);
        builder.ignorePublicAcls(this.ignorePublicAcls);
        builder.blockPublicPolicy(this.blockPublicPolicy);
        builder.restrictPublicBuckets(this.restrictPublicBuckets);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PublicAccessBlockConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<PublicAccessBlockConfiguration> {
        private Boolean blockPublicAcls;
        private Boolean ignorePublicAcls;
        private Boolean blockPublicPolicy;
        private Boolean restrictPublicBuckets;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether Amazon S3 should block public access control lists (ACLs) for this bucket and objects in this
         * bucket. Setting this element to <code>TRUE</code> causes the following behavior:
         *
         * <ul>
         *   <li>
         *     PUT Bucket ACL and PUT Object ACL calls fail if the specified ACL is public.
         *   </li>
         *   <li>
         *     PUT Object calls fail if the request includes a public ACL.
         *   </li>
         *   <li>
         *     PUT Bucket calls fail if the request includes a public ACL.
         *   </li>
         * </ul>
         *
         * <p>Enabling this setting doesn't affect existing policies or ACLs.
         *
         * @return this builder.
         */
        public Builder blockPublicAcls(Boolean blockPublicAcls) {
            this.blockPublicAcls = blockPublicAcls;
            return this;
        }

        /**
         * Specifies whether Amazon S3 should ignore public ACLs for this bucket and objects in this bucket. Setting this
         * element to <code>TRUE</code> causes Amazon S3 to ignore all public ACLs on this bucket and objects in this
         * bucket.
         *
         * <p>Enabling this setting doesn't affect the persistence of any existing ACLs and doesn't prevent new public ACLs
         * from being set.
         *
         * @return this builder.
         */
        public Builder ignorePublicAcls(Boolean ignorePublicAcls) {
            this.ignorePublicAcls = ignorePublicAcls;
            return this;
        }

        /**
         * Specifies whether Amazon S3 should block public bucket policies for this bucket. Setting this element to <code>
         * TRUE</code> causes Amazon S3 to reject calls to PUT Bucket policy if the specified bucket policy allows public
         * access.
         *
         * <p>Enabling this setting doesn't affect existing bucket policies.
         *
         * @return this builder.
         */
        public Builder blockPublicPolicy(Boolean blockPublicPolicy) {
            this.blockPublicPolicy = blockPublicPolicy;
            return this;
        }

        /**
         * Specifies whether Amazon S3 should restrict public bucket policies for this bucket. Setting this element to <code>
         * TRUE</code> restricts access to this bucket to only Amazon Web Services service principals and authorized users
         * within this account if the bucket has a public policy.
         *
         * <p>Enabling this setting doesn't affect previously stored bucket policies, except that public and cross-account
         * access within any public bucket policy, including non-public delegation to specific accounts, is blocked.
         *
         * @return this builder.
         */
        public Builder restrictPublicBuckets(Boolean restrictPublicBuckets) {
            this.restrictPublicBuckets = restrictPublicBuckets;
            return this;
        }

        @Override
        public PublicAccessBlockConfiguration build() {
            return new PublicAccessBlockConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> blockPublicAcls((Boolean) SchemaUtils.validateSameMember($SCHEMA_BLOCK_PUBLIC_ACLS, member, value));
                case 1 -> ignorePublicAcls((Boolean) SchemaUtils.validateSameMember($SCHEMA_IGNORE_PUBLIC_ACLS, member, value));
                case 2 -> blockPublicPolicy((Boolean) SchemaUtils.validateSameMember($SCHEMA_BLOCK_PUBLIC_POLICY, member, value));
                case 3 -> restrictPublicBuckets((Boolean) SchemaUtils.validateSameMember($SCHEMA_RESTRICT_PUBLIC_BUCKETS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
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
                    case 0 -> builder.blockPublicAcls(de.readBoolean(member));
                    case 1 -> builder.ignorePublicAcls(de.readBoolean(member));
                    case 2 -> builder.blockPublicPolicy(de.readBoolean(member));
                    case 3 -> builder.restrictPublicBuckets(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
