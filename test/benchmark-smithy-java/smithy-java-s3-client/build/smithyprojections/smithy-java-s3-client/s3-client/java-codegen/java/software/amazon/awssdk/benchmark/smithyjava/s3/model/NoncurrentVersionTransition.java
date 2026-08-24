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
 * Container for the transition rule that describes when noncurrent objects transition to the <code>STANDARD_IA</code>,
 * <code>ONEZONE_IA</code>, <code>INTELLIGENT_TIERING</code>, <code>GLACIER_IR</code>, <code>GLACIER</code>, or
 * <code>DEEP_ARCHIVE</code> storage class. If your bucket is versioning-enabled (or versioning is suspended), you can
 * set this action to request that Amazon S3 transition noncurrent object versions to the <code>STANDARD_IA</code>,
 * <code>ONEZONE_IA</code>, <code>INTELLIGENT_TIERING</code>, <code>GLACIER_IR</code>, <code>GLACIER</code>, or
 * <code>DEEP_ARCHIVE</code> storage class at a specific period in the object's lifetime.
 */
@SmithyGenerated
public final class NoncurrentVersionTransition implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.NONCURRENT_VERSION_TRANSITION;
    private static final Schema $SCHEMA_NONCURRENT_DAYS = $SCHEMA.member("NoncurrentDays");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_NEWER_NONCURRENT_VERSIONS = $SCHEMA.member("NewerNoncurrentVersions");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer noncurrentDays;
    private final transient TransitionStorageClass storageClass;
    private final transient Integer newerNoncurrentVersions;

    private NoncurrentVersionTransition(Builder builder) {
        this.noncurrentDays = builder.noncurrentDays;
        this.storageClass = builder.storageClass;
        this.newerNoncurrentVersions = builder.newerNoncurrentVersions;
    }

    /**
     * Specifies the number of days an object is noncurrent before Amazon S3 can perform the associated action. For
     * information about the noncurrent days calculations, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/intro-lifecycle-rules.html#non-current-days-calculations">How Amazon S3 Calculates How Long an Object Has Been
     * Noncurrent</a> in the <i>Amazon S3 User Guide</i>.
     */
    public Integer getNoncurrentDays() {
        return noncurrentDays;
    }

    /**
     * The class of storage used to store the object.
     */
    public TransitionStorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * Specifies how many noncurrent versions Amazon S3 will retain in the same storage class before transitioning
     * objects. You can specify up to 100 noncurrent versions to retain. Amazon S3 will transition any additional
     * noncurrent versions beyond the specified number to retain. For more information about noncurrent versions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intro-lifecycle-rules.html">
     * Lifecycle configuration elements</a> in the <i>Amazon S3 User Guide</i>.
     */
    public Integer getNewerNoncurrentVersions() {
        return newerNoncurrentVersions;
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
        NoncurrentVersionTransition that = (NoncurrentVersionTransition) other;
        return Objects.equals(this.noncurrentDays, that.noncurrentDays)
               && Objects.equals(this.newerNoncurrentVersions, that.newerNoncurrentVersions)
               && Objects.equals(this.storageClass, that.storageClass);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(noncurrentDays);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(newerNoncurrentVersions);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (noncurrentDays != null) {
            serializer.writeInteger($SCHEMA_NONCURRENT_DAYS, noncurrentDays);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (newerNoncurrentVersions != null) {
            serializer.writeInteger($SCHEMA_NEWER_NONCURRENT_VERSIONS, newerNoncurrentVersions);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NONCURRENT_DAYS, member, noncurrentDays);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEWER_NONCURRENT_VERSIONS, member, newerNoncurrentVersions);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link NoncurrentVersionTransition}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.noncurrentDays(this.noncurrentDays);
        builder.storageClass(this.storageClass);
        builder.newerNoncurrentVersions(this.newerNoncurrentVersions);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link NoncurrentVersionTransition}.
     */
    public static final class Builder implements ShapeBuilder<NoncurrentVersionTransition> {
        private Integer noncurrentDays;
        private TransitionStorageClass storageClass;
        private Integer newerNoncurrentVersions;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the number of days an object is noncurrent before Amazon S3 can perform the associated action. For
         * information about the noncurrent days calculations, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/intro-lifecycle-rules.html#non-current-days-calculations">How Amazon S3 Calculates How Long an Object Has Been
         * Noncurrent</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder noncurrentDays(Integer noncurrentDays) {
            this.noncurrentDays = noncurrentDays;
            return this;
        }

        /**
         * The class of storage used to store the object.
         *
         * @return this builder.
         */
        public Builder storageClass(TransitionStorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * Specifies how many noncurrent versions Amazon S3 will retain in the same storage class before transitioning
         * objects. You can specify up to 100 noncurrent versions to retain. Amazon S3 will transition any additional
         * noncurrent versions beyond the specified number to retain. For more information about noncurrent versions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intro-lifecycle-rules.html">
         * Lifecycle configuration elements</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder newerNoncurrentVersions(Integer newerNoncurrentVersions) {
            this.newerNoncurrentVersions = newerNoncurrentVersions;
            return this;
        }

        @Override
        public NoncurrentVersionTransition build() {
            return new NoncurrentVersionTransition(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> noncurrentDays((Integer) SchemaUtils.validateSameMember($SCHEMA_NONCURRENT_DAYS, member, value));
                case 1 -> storageClass((TransitionStorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 2 -> newerNoncurrentVersions((Integer) SchemaUtils.validateSameMember($SCHEMA_NEWER_NONCURRENT_VERSIONS, member, value));
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
                    case 0 -> builder.noncurrentDays(de.readInteger(member));
                    case 1 -> builder.storageClass(TransitionStorageClass.builder().deserializeMember(de, member).build());
                    case 2 -> builder.newerNoncurrentVersions(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
