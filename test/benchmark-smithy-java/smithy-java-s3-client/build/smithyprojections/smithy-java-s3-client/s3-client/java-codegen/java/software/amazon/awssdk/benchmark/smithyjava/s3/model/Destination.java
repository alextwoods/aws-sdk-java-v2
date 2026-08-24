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
 * Specifies information about where to publish analysis or configuration results for an Amazon S3 bucket and S3
 * Replication Time Control (S3 RTC).
 */
@SmithyGenerated
public final class Destination implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.DESTINATION;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_ACCOUNT = $SCHEMA.member("Account");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_ACCESS_CONTROL_TRANSLATION = $SCHEMA.member("AccessControlTranslation");
    private static final Schema $SCHEMA_ENCRYPTION_CONFIGURATION = $SCHEMA.member("EncryptionConfiguration");
    private static final Schema $SCHEMA_REPLICATION_TIME = $SCHEMA.member("ReplicationTime");
    private static final Schema $SCHEMA_METRICS = $SCHEMA.member("Metrics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String account;
    private final transient StorageClass storageClass;
    private final transient AccessControlTranslation accessControlTranslation;
    private final transient EncryptionConfiguration encryptionConfiguration;
    private final transient ReplicationTime replicationTime;
    private final transient Metrics metrics;

    private Destination(Builder builder) {
        this.bucket = builder.bucket;
        this.account = builder.account;
        this.storageClass = builder.storageClass;
        this.accessControlTranslation = builder.accessControlTranslation;
        this.encryptionConfiguration = builder.encryptionConfiguration;
        this.replicationTime = builder.replicationTime;
        this.metrics = builder.metrics;
    }

    /**
     * The Amazon Resource Name (ARN) of the bucket where you want Amazon S3 to store the results.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Destination bucket owner account ID. In a cross-account scenario, if you direct Amazon S3 to change replica
     * ownership to the Amazon Web Services account that owns the destination bucket by specifying the <code>
     * AccessControlTranslation</code> property, this is the account ID of the destination bucket owner. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-change-owner.html">Replication Additional Configuration: Changing the Replica Owner</a> in the <i>Amazon S3 User
     * Guide</i>.
     */
    public String getAccount() {
        return account;
    }

    /**
     * The storage class to use when replicating objects, such as S3 Standard or reduced redundancy. By default, Amazon
     * S3 uses the storage class of the source object to create the object replica.
     *
     * <p>For valid values, see the <code>StorageClass</code> element of the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTreplication.html">PUT Bucket replication</a> action in the
     * <i>Amazon S3 API Reference</i>.
     *
     * <p><code>FSX_OPENZFS</code> is not an accepted value when replicating objects.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * Specify this only in a cross-account scenario (where source and destination bucket owners are not the same), and
     * you want to change replica ownership to the Amazon Web Services account that owns the destination bucket. If this
     * is not specified in the replication configuration, the replicas are owned by same Amazon Web Services account
     * that owns the source object.
     */
    public AccessControlTranslation getAccessControlTranslation() {
        return accessControlTranslation;
    }

    /**
     * A container that provides information about encryption. If <code>SourceSelectionCriteria</code> is specified, you
     * must specify this element.
     */
    public EncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
    }

    /**
     * A container specifying S3 Replication Time Control (S3 RTC), including whether S3 RTC is enabled and the time
     * when all objects and operations on objects must be replicated. Must be specified together with a
     * <code>Metrics</code> block.
     */
    public ReplicationTime getReplicationTime() {
        return replicationTime;
    }

    /**
     * A container specifying replication metrics-related settings enabling replication metrics and events.
     */
    public Metrics getMetrics() {
        return metrics;
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
        Destination that = (Destination) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.account, that.account)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.accessControlTranslation, that.accessControlTranslation)
               && Objects.equals(this.encryptionConfiguration, that.encryptionConfiguration)
               && Objects.equals(this.replicationTime, that.replicationTime)
               && Objects.equals(this.metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(account);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(accessControlTranslation);
        $hc = 31 * $hc + Objects.hashCode(encryptionConfiguration);
        $hc = 31 * $hc + Objects.hashCode(replicationTime);
        $hc = 31 * $hc + Objects.hashCode(metrics);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (account != null) {
            serializer.writeString($SCHEMA_ACCOUNT, account);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (accessControlTranslation != null) {
            serializer.writeStruct($SCHEMA_ACCESS_CONTROL_TRANSLATION, accessControlTranslation);
        }
        if (encryptionConfiguration != null) {
            serializer.writeStruct($SCHEMA_ENCRYPTION_CONFIGURATION, encryptionConfiguration);
        }
        if (replicationTime != null) {
            serializer.writeStruct($SCHEMA_REPLICATION_TIME, replicationTime);
        }
        if (metrics != null) {
            serializer.writeStruct($SCHEMA_METRICS, metrics);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT, member, account);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_CONTROL_TRANSLATION, member, accessControlTranslation);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, encryptionConfiguration);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_TIME, member, replicationTime);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, metrics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Destination}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.account(this.account);
        builder.storageClass(this.storageClass);
        builder.accessControlTranslation(this.accessControlTranslation);
        builder.encryptionConfiguration(this.encryptionConfiguration);
        builder.replicationTime(this.replicationTime);
        builder.metrics(this.metrics);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Destination}.
     */
    public static final class Builder implements ShapeBuilder<Destination> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String account;
        private StorageClass storageClass;
        private AccessControlTranslation accessControlTranslation;
        private EncryptionConfiguration encryptionConfiguration;
        private ReplicationTime replicationTime;
        private Metrics metrics;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the bucket where you want Amazon S3 to store the results.
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
         * Destination bucket owner account ID. In a cross-account scenario, if you direct Amazon S3 to change replica
         * ownership to the Amazon Web Services account that owns the destination bucket by specifying the <code>
         * AccessControlTranslation</code> property, this is the account ID of the destination bucket owner. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-change-owner.html">Replication Additional Configuration: Changing the Replica Owner</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * @return this builder.
         */
        public Builder account(String account) {
            this.account = account;
            return this;
        }

        /**
         * The storage class to use when replicating objects, such as S3 Standard or reduced redundancy. By default, Amazon
         * S3 uses the storage class of the source object to create the object replica.
         *
         * <p>For valid values, see the <code>StorageClass</code> element of the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTreplication.html">PUT Bucket replication</a> action in the
         * <i>Amazon S3 API Reference</i>.
         *
         * <p><code>FSX_OPENZFS</code> is not an accepted value when replicating objects.
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * Specify this only in a cross-account scenario (where source and destination bucket owners are not the same), and
         * you want to change replica ownership to the Amazon Web Services account that owns the destination bucket. If this
         * is not specified in the replication configuration, the replicas are owned by same Amazon Web Services account
         * that owns the source object.
         *
         * @return this builder.
         */
        public Builder accessControlTranslation(AccessControlTranslation accessControlTranslation) {
            this.accessControlTranslation = accessControlTranslation;
            return this;
        }

        /**
         * A container that provides information about encryption. If <code>SourceSelectionCriteria</code> is specified, you
         * must specify this element.
         *
         * @return this builder.
         */
        public Builder encryptionConfiguration(EncryptionConfiguration encryptionConfiguration) {
            this.encryptionConfiguration = encryptionConfiguration;
            return this;
        }

        /**
         * A container specifying S3 Replication Time Control (S3 RTC), including whether S3 RTC is enabled and the time
         * when all objects and operations on objects must be replicated. Must be specified together with a
         * <code>Metrics</code> block.
         *
         * @return this builder.
         */
        public Builder replicationTime(ReplicationTime replicationTime) {
            this.replicationTime = replicationTime;
            return this;
        }

        /**
         * A container specifying replication metrics-related settings enabling replication metrics and events.
         *
         * @return this builder.
         */
        public Builder metrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        @Override
        public Destination build() {
            tracker.validate();
            return new Destination(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> account((String) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT, member, value));
                case 2 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 3 -> accessControlTranslation((AccessControlTranslation) SchemaUtils.validateSameMember($SCHEMA_ACCESS_CONTROL_TRANSLATION, member, value));
                case 4 -> encryptionConfiguration((EncryptionConfiguration) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, value));
                case 5 -> replicationTime((ReplicationTime) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_TIME, member, value));
                case 6 -> metrics((Metrics) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Destination> errorCorrection() {
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
                    case 1 -> builder.account(de.readString(member));
                    case 2 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 3 -> builder.accessControlTranslation(AccessControlTranslation.builder().deserializeMember(de, member).build());
                    case 4 -> builder.encryptionConfiguration(EncryptionConfiguration.builder().deserializeMember(de, member).build());
                    case 5 -> builder.replicationTime(ReplicationTime.builder().deserializeMember(de, member).build());
                    case 6 -> builder.metrics(Metrics.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
