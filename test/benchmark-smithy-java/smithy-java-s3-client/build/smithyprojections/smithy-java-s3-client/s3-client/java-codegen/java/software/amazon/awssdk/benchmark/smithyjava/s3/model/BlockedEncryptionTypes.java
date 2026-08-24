package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
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
 * A bucket-level setting for Amazon S3 general purpose buckets used to prevent the upload of new objects encrypted with
 * the specified server-side encryption type. For example, blocking an encryption type will block <code>PutObject</code>
 * , <code>CopyObject</code>, <code>PostObject</code>, multipart upload, and replication requests to the bucket for
 * objects with the specified encryption type. However, you can continue to read and list any pre-existing objects
 * already encrypted with the specified encryption type. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/blocking-unblocking-s3-c-encryption-gpb.html">Blocking or unblocking SSE-C for a
 * general purpose bucket</a>.
 *
 * <p>This data type is used with the following actions:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketEncryption.html">PutBucketEncryption</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketEncryption.html">GetBucketEncryption</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketEncryption.html">DeleteBucketEncryption</a>
 *   </li>
 * </ul>
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>You must have the <code>s3:PutEncryptionConfiguration</code> permission to block or unblock an encryption
 *     type for a bucket.
 *
 *     <p>You must have the <code>s3:GetEncryptionConfiguration</code> permission to view a bucket's encryption
 *     type.
 *   </dd>
 * </dl>
 */
@SmithyGenerated
public final class BlockedEncryptionTypes implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.BLOCKED_ENCRYPTION_TYPES;
    private static final Schema $SCHEMA_ENCRYPTION_TYPE = $SCHEMA.member("EncryptionType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<EncryptionType> encryptionType;

    private BlockedEncryptionTypes(Builder builder) {
        this.encryptionType = builder.encryptionType == null ? null : Collections.unmodifiableList(builder.encryptionType);
    }

    /**
     * The object encryption type that you want to block or unblock for an Amazon S3 general purpose bucket.
     *
     * <p>Currently, this parameter only supports blocking or unblocking server side encryption with customer-provided
     * keys (SSE-C). For more information about SSE-C, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html">Using server-side encryption with customer-provided keys
     * (SSE-C)</a>.
     */
    public List<EncryptionType> getEncryptionType() {
        if (encryptionType == null) {
            return Collections.emptyList();
        }
        return encryptionType;
    }

    public boolean hasEncryptionType() {
        return encryptionType != null;
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
        BlockedEncryptionTypes that = (BlockedEncryptionTypes) other;
        return Objects.equals(this.encryptionType, that.encryptionType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(encryptionType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (encryptionType != null) {
            serializer.writeList($SCHEMA_ENCRYPTION_TYPE, encryptionType, encryptionType.size(), SharedSerde.EncryptionTypeListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_TYPE, member, encryptionType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BlockedEncryptionTypes}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.encryptionType(this.encryptionType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BlockedEncryptionTypes}.
     */
    public static final class Builder implements ShapeBuilder<BlockedEncryptionTypes> {
        private List<EncryptionType> encryptionType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The object encryption type that you want to block or unblock for an Amazon S3 general purpose bucket.
         *
         * <p>Currently, this parameter only supports blocking or unblocking server side encryption with customer-provided
         * keys (SSE-C). For more information about SSE-C, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html">Using server-side encryption with customer-provided keys
         * (SSE-C)</a>.
         *
         * @return this builder.
         */
        public Builder encryptionType(List<EncryptionType> encryptionType) {
            this.encryptionType = encryptionType;
            return this;
        }

        @Override
        public BlockedEncryptionTypes build() {
            return new BlockedEncryptionTypes(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> encryptionType((List<EncryptionType>) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_TYPE, member, value));
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
                    case 0 -> builder.encryptionType(SharedSerde.deserializeEncryptionTypeList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
