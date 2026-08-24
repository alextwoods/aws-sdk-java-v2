package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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

@SmithyGenerated
public final class ListPartsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.LIST_PARTS_OUTPUT;
    private static final Schema $SCHEMA_ABORT_DATE = $SCHEMA.member("AbortDate");
    private static final Schema $SCHEMA_ABORT_RULE_ID = $SCHEMA.member("AbortRuleId");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
    private static final Schema $SCHEMA_PART_NUMBER_MARKER = $SCHEMA.member("PartNumberMarker");
    private static final Schema $SCHEMA_NEXT_PART_NUMBER_MARKER = $SCHEMA.member("NextPartNumberMarker");
    private static final Schema $SCHEMA_MAX_PARTS = $SCHEMA.member("MaxParts");
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_PARTS = $SCHEMA.member("Parts");
    private static final Schema $SCHEMA_INITIATOR = $SCHEMA.member("Initiator");
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant abortDate;
    private final transient String abortRuleId;
    private final transient String bucket;
    private final transient String key;
    private final transient String uploadId;
    private final transient String partNumberMarker;
    private final transient String nextPartNumberMarker;
    private final transient Integer maxParts;
    private final transient Boolean isTruncated;
    private final transient List<Part> parts;
    private final transient Initiator initiator;
    private final transient Owner owner;
    private final transient StorageClass storageClass;
    private final transient RequestCharged requestCharged;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient ChecksumType checksumType;

    private ListPartsOutput(Builder builder) {
        this.abortDate = builder.abortDate;
        this.abortRuleId = builder.abortRuleId;
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.uploadId = builder.uploadId;
        this.partNumberMarker = builder.partNumberMarker;
        this.nextPartNumberMarker = builder.nextPartNumberMarker;
        this.maxParts = builder.maxParts;
        this.isTruncated = builder.isTruncated;
        this.parts = builder.parts == null ? null : Collections.unmodifiableList(builder.parts);
        this.initiator = builder.initiator;
        this.owner = builder.owner;
        this.storageClass = builder.storageClass;
        this.requestCharged = builder.requestCharged;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.checksumType = builder.checksumType;
    }

    /**
     * If the bucket has a lifecycle rule configured with an action to abort incomplete multipart uploads and the prefix
     * in the lifecycle rule matches the object name in the request, then the response includes this header indicating
     * when the initiated multipart upload will become eligible for abort operation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html#mpu-abort-incomplete-mpu-lifecycle-config">
     * Aborting Incomplete Multipart Uploads Using a Bucket Lifecycle Configuration</a>.
     *
     * <p>The response will also include the <code>x-amz-abort-rule-id</code> header that will provide the ID of the
     * lifecycle configuration rule that defines this action.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Instant getAbortDate() {
        return abortDate;
    }

    /**
     * This header is returned along with the <code>x-amz-abort-date</code> header. It identifies applicable lifecycle
     * configuration rule that defines the action to abort incomplete multipart uploads.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getAbortRuleId() {
        return abortRuleId;
    }

    /**
     * The name of the bucket to which the multipart upload was initiated. Does not return the access point ARN or
     * access point alias if used.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Object key for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * Upload ID identifying the multipart upload whose parts are being listed.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Specifies the part after which listing should begin. Only parts with higher part numbers will be listed.
     */
    public String getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * When a list is truncated, this element specifies the last part in the list, as well as the value to use for the
     * <code>part-number-marker</code> request parameter in a subsequent request.
     */
    public String getNextPartNumberMarker() {
        return nextPartNumberMarker;
    }

    /**
     * Maximum number of parts that were allowed in the response.
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Indicates whether the returned list of parts is truncated. A true value indicates that the list was truncated. A
     * list can be truncated if the number of parts exceeds the limit returned in the MaxParts element.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * Container for elements related to a particular part. A response can contain zero or more <code>Part</code>
     * elements.
     */
    public List<Part> getParts() {
        if (parts == null) {
            return Collections.emptyList();
        }
        return parts;
    }

    public boolean hasParts() {
        return parts != null;
    }

    /**
     * Container element that identifies who initiated the multipart upload. If the initiator is an Amazon Web Services
     * account, this element provides the same information as the <code>Owner</code> element. If the initiator is an IAM
     * User, this element provides the user ARN.
     */
    public Initiator getInitiator() {
        return initiator;
    }

    /**
     * Container element that identifies the object owner, after the object is created. If multipart upload is initiated
     * by an IAM user, this element provides the parent account ID.
     *
     * <p><b>Directory buckets</b> - The bucket owner is returned as the object owner for all the parts.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * The class of storage used to store the uploaded object.
     *
     * <p><b>Directory buckets</b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
     * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
     * class) in Dedicated Local Zones.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * The algorithm that was used to create a checksum of the object.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * The checksum type, which determines how part-level checksums are combined to create an object-level checksum for
     * multipart objects. You can use this header response to verify that the checksum type that is received is the same
     * checksum type that was specified in <code>CreateMultipartUpload</code> request. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
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
        ListPartsOutput that = (ListPartsOutput) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.maxParts, that.maxParts)
               && Objects.equals(this.abortRuleId, that.abortRuleId)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.uploadId, that.uploadId)
               && Objects.equals(this.partNumberMarker, that.partNumberMarker)
               && Objects.equals(this.nextPartNumberMarker, that.nextPartNumberMarker)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.abortDate, that.abortDate)
               && Objects.equals(this.initiator, that.initiator)
               && Objects.equals(this.owner, that.owner)
               && Objects.equals(this.parts, that.parts);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(abortDate);
        $hc = 31 * $hc + Objects.hashCode(abortRuleId);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(uploadId);
        $hc = 31 * $hc + Objects.hashCode(partNumberMarker);
        $hc = 31 * $hc + Objects.hashCode(nextPartNumberMarker);
        $hc = 31 * $hc + Objects.hashCode(maxParts);
        $hc = 31 * $hc + Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(parts);
        $hc = 31 * $hc + Objects.hashCode(initiator);
        $hc = 31 * $hc + Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (abortDate != null) {
            serializer.writeTimestamp($SCHEMA_ABORT_DATE, abortDate);
        }
        if (abortRuleId != null) {
            serializer.writeString($SCHEMA_ABORT_RULE_ID, abortRuleId);
        }
        if (bucket != null) {
            serializer.writeString($SCHEMA_BUCKET, bucket);
        }
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (uploadId != null) {
            serializer.writeString($SCHEMA_UPLOAD_ID, uploadId);
        }
        if (partNumberMarker != null) {
            serializer.writeString($SCHEMA_PART_NUMBER_MARKER, partNumberMarker);
        }
        if (nextPartNumberMarker != null) {
            serializer.writeString($SCHEMA_NEXT_PART_NUMBER_MARKER, nextPartNumberMarker);
        }
        if (maxParts != null) {
            serializer.writeInteger($SCHEMA_MAX_PARTS, maxParts);
        }
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (parts != null) {
            serializer.writeList($SCHEMA_PARTS, parts, parts.size(), SharedSerde.PartsSerializer.INSTANCE);
        }
        if (initiator != null) {
            serializer.writeStruct($SCHEMA_INITIATOR, initiator);
        }
        if (owner != null) {
            serializer.writeStruct($SCHEMA_OWNER, owner);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ABORT_DATE, member, abortDate);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ABORT_RULE_ID, member, abortRuleId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER_MARKER, member, partNumberMarker);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_PART_NUMBER_MARKER, member, nextPartNumberMarker);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_PARTS, member, maxParts);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTS, member, parts);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_INITIATOR, member, initiator);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListPartsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.abortDate(this.abortDate);
        builder.abortRuleId(this.abortRuleId);
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.uploadId(this.uploadId);
        builder.partNumberMarker(this.partNumberMarker);
        builder.nextPartNumberMarker(this.nextPartNumberMarker);
        builder.maxParts(this.maxParts);
        builder.isTruncated(this.isTruncated);
        builder.parts(this.parts);
        builder.initiator(this.initiator);
        builder.owner(this.owner);
        builder.storageClass(this.storageClass);
        builder.requestCharged(this.requestCharged);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumType(this.checksumType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListPartsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListPartsOutput> {
        private Instant abortDate;
        private String abortRuleId;
        private String bucket;
        private String key;
        private String uploadId;
        private String partNumberMarker;
        private String nextPartNumberMarker;
        private Integer maxParts;
        private Boolean isTruncated;
        private List<Part> parts;
        private Initiator initiator;
        private Owner owner;
        private StorageClass storageClass;
        private RequestCharged requestCharged;
        private ChecksumAlgorithm checksumAlgorithm;
        private ChecksumType checksumType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the bucket has a lifecycle rule configured with an action to abort incomplete multipart uploads and the prefix
         * in the lifecycle rule matches the object name in the request, then the response includes this header indicating
         * when the initiated multipart upload will become eligible for abort operation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html#mpu-abort-incomplete-mpu-lifecycle-config">
         * Aborting Incomplete Multipart Uploads Using a Bucket Lifecycle Configuration</a>.
         *
         * <p>The response will also include the <code>x-amz-abort-rule-id</code> header that will provide the ID of the
         * lifecycle configuration rule that defines this action.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder abortDate(Instant abortDate) {
            this.abortDate = abortDate;
            return this;
        }

        /**
         * This header is returned along with the <code>x-amz-abort-date</code> header. It identifies applicable lifecycle
         * configuration rule that defines the action to abort incomplete multipart uploads.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder abortRuleId(String abortRuleId) {
            this.abortRuleId = abortRuleId;
            return this;
        }

        /**
         * The name of the bucket to which the multipart upload was initiated. Does not return the access point ARN or
         * access point alias if used.
         *
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Object key for which the multipart upload was initiated.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Upload ID identifying the multipart upload whose parts are being listed.
         *
         * @return this builder.
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * Specifies the part after which listing should begin. Only parts with higher part numbers will be listed.
         *
         * @return this builder.
         */
        public Builder partNumberMarker(String partNumberMarker) {
            this.partNumberMarker = partNumberMarker;
            return this;
        }

        /**
         * When a list is truncated, this element specifies the last part in the list, as well as the value to use for the
         * <code>part-number-marker</code> request parameter in a subsequent request.
         *
         * @return this builder.
         */
        public Builder nextPartNumberMarker(String nextPartNumberMarker) {
            this.nextPartNumberMarker = nextPartNumberMarker;
            return this;
        }

        /**
         * Maximum number of parts that were allowed in the response.
         *
         * @return this builder.
         */
        public Builder maxParts(Integer maxParts) {
            this.maxParts = maxParts;
            return this;
        }

        /**
         * Indicates whether the returned list of parts is truncated. A true value indicates that the list was truncated. A
         * list can be truncated if the number of parts exceeds the limit returned in the MaxParts element.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * Container for elements related to a particular part. A response can contain zero or more <code>Part</code>
         * elements.
         *
         * @return this builder.
         */
        public Builder parts(List<Part> parts) {
            this.parts = parts;
            return this;
        }

        /**
         * Container element that identifies who initiated the multipart upload. If the initiator is an Amazon Web Services
         * account, this element provides the same information as the <code>Owner</code> element. If the initiator is an IAM
         * User, this element provides the user ARN.
         *
         * @return this builder.
         */
        public Builder initiator(Initiator initiator) {
            this.initiator = initiator;
            return this;
        }

        /**
         * Container element that identifies the object owner, after the object is created. If multipart upload is initiated
         * by an IAM user, this element provides the parent account ID.
         *
         * <p><b>Directory buckets</b> - The bucket owner is returned as the object owner for all the parts.
         *
         * @return this builder.
         */
        public Builder owner(Owner owner) {
            this.owner = owner;
            return this;
        }

        /**
         * The class of storage used to store the uploaded object.
         *
         * <p><b>Directory buckets</b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
         * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
         * class) in Dedicated Local Zones.
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        /**
         * The algorithm that was used to create a checksum of the object.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * The checksum type, which determines how part-level checksums are combined to create an object-level checksum for
         * multipart objects. You can use this header response to verify that the checksum type that is received is the same
         * checksum type that was specified in <code>CreateMultipartUpload</code> request. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        @Override
        public ListPartsOutput build() {
            return new ListPartsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> abortDate((Instant) SchemaUtils.validateSameMember($SCHEMA_ABORT_DATE, member, value));
                case 1 -> abortRuleId((String) SchemaUtils.validateSameMember($SCHEMA_ABORT_RULE_ID, member, value));
                case 2 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 3 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 4 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 5 -> partNumberMarker((String) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER_MARKER, member, value));
                case 6 -> nextPartNumberMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_PART_NUMBER_MARKER, member, value));
                case 7 -> maxParts((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_PARTS, member, value));
                case 8 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 9 -> parts((List<Part>) SchemaUtils.validateSameMember($SCHEMA_PARTS, member, value));
                case 10 -> initiator((Initiator) SchemaUtils.validateSameMember($SCHEMA_INITIATOR, member, value));
                case 11 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 12 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 13 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 14 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 15 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
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
                    case 0 -> builder.abortDate(de.readTimestamp(member));
                    case 1 -> builder.abortRuleId(de.readString(member));
                    case 2 -> builder.bucket(de.readString(member));
                    case 3 -> builder.key(de.readString(member));
                    case 4 -> builder.uploadId(de.readString(member));
                    case 5 -> builder.partNumberMarker(de.readString(member));
                    case 6 -> builder.nextPartNumberMarker(de.readString(member));
                    case 7 -> builder.maxParts(de.readInteger(member));
                    case 8 -> builder.isTruncated(de.readBoolean(member));
                    case 9 -> builder.parts(SharedSerde.deserializeParts(member, de));
                    case 10 -> builder.initiator(Initiator.builder().deserializeMember(de, member).build());
                    case 11 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 12 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 13 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 14 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 15 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
