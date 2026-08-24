package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * Represents the properties of the exported table.
 */
@SmithyGenerated
public final class ExportDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXPORT_DESCRIPTION;
    private static final Schema $SCHEMA_EXPORT_ARN = $SCHEMA.member("ExportArn");
    private static final Schema $SCHEMA_EXPORT_STATUS = $SCHEMA.member("ExportStatus");
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");
    private static final Schema $SCHEMA_EXPORT_MANIFEST = $SCHEMA.member("ExportManifest");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_TABLE_ID = $SCHEMA.member("TableId");
    private static final Schema $SCHEMA_EXPORT_TIME = $SCHEMA.member("ExportTime");
    private static final Schema $SCHEMA_CLIENT_TOKEN = $SCHEMA.member("ClientToken");
    private static final Schema $SCHEMA_S3_BUCKET = $SCHEMA.member("S3Bucket");
    private static final Schema $SCHEMA_S3_BUCKET_OWNER = $SCHEMA.member("S3BucketOwner");
    private static final Schema $SCHEMA_S3_PREFIX = $SCHEMA.member("S3Prefix");
    private static final Schema $SCHEMA_S3_SSE_ALGORITHM = $SCHEMA.member("S3SseAlgorithm");
    private static final Schema $SCHEMA_S3_SSE_KMS_KEY_ID = $SCHEMA.member("S3SseKmsKeyId");
    private static final Schema $SCHEMA_FAILURE_CODE = $SCHEMA.member("FailureCode");
    private static final Schema $SCHEMA_FAILURE_MESSAGE = $SCHEMA.member("FailureMessage");
    private static final Schema $SCHEMA_EXPORT_FORMAT = $SCHEMA.member("ExportFormat");
    private static final Schema $SCHEMA_BILLED_SIZE_BYTES = $SCHEMA.member("BilledSizeBytes");
    private static final Schema $SCHEMA_ITEM_COUNT = $SCHEMA.member("ItemCount");
    private static final Schema $SCHEMA_EXPORT_TYPE = $SCHEMA.member("ExportType");
    private static final Schema $SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION = $SCHEMA.member("IncrementalExportSpecification");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String exportArn;
    private final transient ExportStatus exportStatus;
    private final transient Instant startTime;
    private final transient Instant endTime;
    private final transient String exportManifest;
    private final transient String tableArn;
    private final transient String tableId;
    private final transient Instant exportTime;
    private final transient String clientToken;
    private final transient String s3Bucket;
    private final transient String s3BucketOwner;
    private final transient String s3Prefix;
    private final transient S3SseAlgorithm s3SseAlgorithm;
    private final transient String s3SseKmsKeyId;
    private final transient String failureCode;
    private final transient String failureMessage;
    private final transient ExportFormat exportFormat;
    private final transient Long billedSizeBytes;
    private final transient Long itemCount;
    private final transient ExportType exportType;
    private final transient IncrementalExportSpecification incrementalExportSpecification;

    private ExportDescription(Builder builder) {
        this.exportArn = builder.exportArn;
        this.exportStatus = builder.exportStatus;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.exportManifest = builder.exportManifest;
        this.tableArn = builder.tableArn;
        this.tableId = builder.tableId;
        this.exportTime = builder.exportTime;
        this.clientToken = builder.clientToken;
        this.s3Bucket = builder.s3Bucket;
        this.s3BucketOwner = builder.s3BucketOwner;
        this.s3Prefix = builder.s3Prefix;
        this.s3SseAlgorithm = builder.s3SseAlgorithm;
        this.s3SseKmsKeyId = builder.s3SseKmsKeyId;
        this.failureCode = builder.failureCode;
        this.failureMessage = builder.failureMessage;
        this.exportFormat = builder.exportFormat;
        this.billedSizeBytes = builder.billedSizeBytes;
        this.itemCount = builder.itemCount;
        this.exportType = builder.exportType;
        this.incrementalExportSpecification = builder.incrementalExportSpecification;
    }

    /**
     * The Amazon Resource Name (ARN) of the table export.
     */
    public String getExportArn() {
        return exportArn;
    }

    /**
     * Export can be in one of the following states: IN_PROGRESS, COMPLETED, or FAILED.
     */
    public ExportStatus getExportStatus() {
        return exportStatus;
    }

    /**
     * The time at which the export task began.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The time at which the export task completed.
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * The name of the manifest file for the export task.
     */
    public String getExportManifest() {
        return exportManifest;
    }

    /**
     * The Amazon Resource Name (ARN) of the table that was exported.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * Unique ID of the table that was exported.
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Point in time from which table data was exported.
     */
    public Instant getExportTime() {
        return exportTime;
    }

    /**
     * The client token that was provided for the export task. A client token makes calls to
     * <code>ExportTableToPointInTimeInput</code> idempotent, meaning that multiple identical calls have the same effect
     * as one single call.
     */
    public String getClientToken() {
        return clientToken;
    }

    /**
     * The name of the Amazon S3 bucket containing the export.
     */
    public String getS3Bucket() {
        return s3Bucket;
    }

    /**
     * The ID of the Amazon Web Services account that owns the bucket containing the export.
     */
    public String getS3BucketOwner() {
        return s3BucketOwner;
    }

    /**
     * The Amazon S3 bucket prefix used as the file name and path of the exported snapshot.
     */
    public String getS3Prefix() {
        return s3Prefix;
    }

    /**
     * Type of encryption used on the bucket where export data is stored. Valid values for <code>S3SseAlgorithm</code>
     * are:
     *
     * <ul>
     *   <li>
     *     <code>AES256</code> - server-side encryption with Amazon S3 managed keys
     *   </li>
     *   <li>
     *     <code>KMS</code> - server-side encryption with KMS managed keys
     *   </li>
     * </ul>
     */
    public S3SseAlgorithm getS3SseAlgorithm() {
        return s3SseAlgorithm;
    }

    /**
     * The ID of the KMS managed key used to encrypt the S3 bucket where export data is stored (if applicable).
     */
    public String getS3SseKmsKeyId() {
        return s3SseKmsKeyId;
    }

    /**
     * Status code for the result of the failed export.
     */
    public String getFailureCode() {
        return failureCode;
    }

    /**
     * Export failure reason description.
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * The format of the exported data. Valid values for <code>ExportFormat</code> are <code>DYNAMODB_JSON</code> or
     * <code>ION</code>.
     */
    public ExportFormat getExportFormat() {
        return exportFormat;
    }

    /**
     * The billable size of the table export.
     */
    public Long getBilledSizeBytes() {
        return billedSizeBytes;
    }

    /**
     * The number of items exported.
     */
    public Long getItemCount() {
        return itemCount;
    }

    /**
     * The type of export that was performed. Valid values are <code>FULL_EXPORT</code> or
     * <code>INCREMENTAL_EXPORT</code>.
     */
    public ExportType getExportType() {
        return exportType;
    }

    /**
     * Optional object containing the parameters specific to an incremental export.
     */
    public IncrementalExportSpecification getIncrementalExportSpecification() {
        return incrementalExportSpecification;
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
        ExportDescription that = (ExportDescription) other;
        return Objects.equals(this.billedSizeBytes, that.billedSizeBytes)
               && Objects.equals(this.itemCount, that.itemCount)
               && Objects.equals(this.exportArn, that.exportArn)
               && Objects.equals(this.exportManifest, that.exportManifest)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.tableId, that.tableId)
               && Objects.equals(this.clientToken, that.clientToken)
               && Objects.equals(this.s3Bucket, that.s3Bucket)
               && Objects.equals(this.s3BucketOwner, that.s3BucketOwner)
               && Objects.equals(this.s3Prefix, that.s3Prefix)
               && Objects.equals(this.s3SseKmsKeyId, that.s3SseKmsKeyId)
               && Objects.equals(this.failureCode, that.failureCode)
               && Objects.equals(this.failureMessage, that.failureMessage)
               && Objects.equals(this.exportStatus, that.exportStatus)
               && Objects.equals(this.s3SseAlgorithm, that.s3SseAlgorithm)
               && Objects.equals(this.exportFormat, that.exportFormat)
               && Objects.equals(this.exportType, that.exportType)
               && Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime)
               && Objects.equals(this.exportTime, that.exportTime)
               && Objects.equals(this.incrementalExportSpecification, that.incrementalExportSpecification);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exportArn);
        $hc = 31 * $hc + Objects.hashCode(exportStatus);
        $hc = 31 * $hc + Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
        $hc = 31 * $hc + Objects.hashCode(exportManifest);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(tableId);
        $hc = 31 * $hc + Objects.hashCode(exportTime);
        $hc = 31 * $hc + Objects.hashCode(clientToken);
        $hc = 31 * $hc + Objects.hashCode(s3Bucket);
        $hc = 31 * $hc + Objects.hashCode(s3BucketOwner);
        $hc = 31 * $hc + Objects.hashCode(s3Prefix);
        $hc = 31 * $hc + Objects.hashCode(s3SseAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(s3SseKmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(failureCode);
        $hc = 31 * $hc + Objects.hashCode(failureMessage);
        $hc = 31 * $hc + Objects.hashCode(exportFormat);
        $hc = 31 * $hc + Objects.hashCode(billedSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(itemCount);
        $hc = 31 * $hc + Objects.hashCode(exportType);
        $hc = 31 * $hc + Objects.hashCode(incrementalExportSpecification);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exportArn != null) {
            serializer.writeString($SCHEMA_EXPORT_ARN, exportArn);
        }
        if (exportStatus != null) {
            serializer.writeString($SCHEMA_EXPORT_STATUS, exportStatus.getValue());
        }
        if (startTime != null) {
            serializer.writeTimestamp($SCHEMA_START_TIME, startTime);
        }
        if (endTime != null) {
            serializer.writeTimestamp($SCHEMA_END_TIME, endTime);
        }
        if (exportManifest != null) {
            serializer.writeString($SCHEMA_EXPORT_MANIFEST, exportManifest);
        }
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (tableId != null) {
            serializer.writeString($SCHEMA_TABLE_ID, tableId);
        }
        if (exportTime != null) {
            serializer.writeTimestamp($SCHEMA_EXPORT_TIME, exportTime);
        }
        if (clientToken != null) {
            serializer.writeString($SCHEMA_CLIENT_TOKEN, clientToken);
        }
        if (s3Bucket != null) {
            serializer.writeString($SCHEMA_S3_BUCKET, s3Bucket);
        }
        if (s3BucketOwner != null) {
            serializer.writeString($SCHEMA_S3_BUCKET_OWNER, s3BucketOwner);
        }
        if (s3Prefix != null) {
            serializer.writeString($SCHEMA_S3_PREFIX, s3Prefix);
        }
        if (s3SseAlgorithm != null) {
            serializer.writeString($SCHEMA_S3_SSE_ALGORITHM, s3SseAlgorithm.getValue());
        }
        if (s3SseKmsKeyId != null) {
            serializer.writeString($SCHEMA_S3_SSE_KMS_KEY_ID, s3SseKmsKeyId);
        }
        if (failureCode != null) {
            serializer.writeString($SCHEMA_FAILURE_CODE, failureCode);
        }
        if (failureMessage != null) {
            serializer.writeString($SCHEMA_FAILURE_MESSAGE, failureMessage);
        }
        if (exportFormat != null) {
            serializer.writeString($SCHEMA_EXPORT_FORMAT, exportFormat.getValue());
        }
        if (billedSizeBytes != null) {
            serializer.writeLong($SCHEMA_BILLED_SIZE_BYTES, billedSizeBytes);
        }
        if (itemCount != null) {
            serializer.writeLong($SCHEMA_ITEM_COUNT, itemCount);
        }
        if (exportType != null) {
            serializer.writeString($SCHEMA_EXPORT_TYPE, exportType.getValue());
        }
        if (incrementalExportSpecification != null) {
            serializer.writeStruct($SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION, incrementalExportSpecification);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_ARN, member, exportArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_STATUS, member, exportStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_MANIFEST, member, exportManifest);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, tableId);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TIME, member, exportTime);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, clientToken);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET, member, s3Bucket);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_OWNER, member, s3BucketOwner);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_PREFIX, member, s3Prefix);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_ALGORITHM, member, s3SseAlgorithm);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_KMS_KEY_ID, member, s3SseKmsKeyId);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_CODE, member, failureCode);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_MESSAGE, member, failureMessage);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_FORMAT, member, exportFormat);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLED_SIZE_BYTES, member, billedSizeBytes);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, itemCount);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TYPE, member, exportType);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION, member, incrementalExportSpecification);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExportDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exportArn(this.exportArn);
        builder.exportStatus(this.exportStatus);
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        builder.exportManifest(this.exportManifest);
        builder.tableArn(this.tableArn);
        builder.tableId(this.tableId);
        builder.exportTime(this.exportTime);
        builder.clientToken(this.clientToken);
        builder.s3Bucket(this.s3Bucket);
        builder.s3BucketOwner(this.s3BucketOwner);
        builder.s3Prefix(this.s3Prefix);
        builder.s3SseAlgorithm(this.s3SseAlgorithm);
        builder.s3SseKmsKeyId(this.s3SseKmsKeyId);
        builder.failureCode(this.failureCode);
        builder.failureMessage(this.failureMessage);
        builder.exportFormat(this.exportFormat);
        builder.billedSizeBytes(this.billedSizeBytes);
        builder.itemCount(this.itemCount);
        builder.exportType(this.exportType);
        builder.incrementalExportSpecification(this.incrementalExportSpecification);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ExportDescription}.
     */
    public static final class Builder implements ShapeBuilder<ExportDescription> {
        private String exportArn;
        private ExportStatus exportStatus;
        private Instant startTime;
        private Instant endTime;
        private String exportManifest;
        private String tableArn;
        private String tableId;
        private Instant exportTime;
        private String clientToken;
        private String s3Bucket;
        private String s3BucketOwner;
        private String s3Prefix;
        private S3SseAlgorithm s3SseAlgorithm;
        private String s3SseKmsKeyId;
        private String failureCode;
        private String failureMessage;
        private ExportFormat exportFormat;
        private Long billedSizeBytes;
        private Long itemCount;
        private ExportType exportType;
        private IncrementalExportSpecification incrementalExportSpecification;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the table export.
         *
         * @return this builder.
         */
        public Builder exportArn(String exportArn) {
            this.exportArn = exportArn;
            return this;
        }

        /**
         * Export can be in one of the following states: IN_PROGRESS, COMPLETED, or FAILED.
         *
         * @return this builder.
         */
        public Builder exportStatus(ExportStatus exportStatus) {
            this.exportStatus = exportStatus;
            return this;
        }

        /**
         * The time at which the export task began.
         *
         * @return this builder.
         */
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * The time at which the export task completed.
         *
         * @return this builder.
         */
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * The name of the manifest file for the export task.
         *
         * @return this builder.
         */
        public Builder exportManifest(String exportManifest) {
            this.exportManifest = exportManifest;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the table that was exported.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * Unique ID of the table that was exported.
         *
         * @return this builder.
         */
        public Builder tableId(String tableId) {
            this.tableId = tableId;
            return this;
        }

        /**
         * Point in time from which table data was exported.
         *
         * @return this builder.
         */
        public Builder exportTime(Instant exportTime) {
            this.exportTime = exportTime;
            return this;
        }

        /**
         * The client token that was provided for the export task. A client token makes calls to
         * <code>ExportTableToPointInTimeInput</code> idempotent, meaning that multiple identical calls have the same effect
         * as one single call.
         *
         * @return this builder.
         */
        public Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
            return this;
        }

        /**
         * The name of the Amazon S3 bucket containing the export.
         *
         * @return this builder.
         */
        public Builder s3Bucket(String s3Bucket) {
            this.s3Bucket = s3Bucket;
            return this;
        }

        /**
         * The ID of the Amazon Web Services account that owns the bucket containing the export.
         *
         * @return this builder.
         */
        public Builder s3BucketOwner(String s3BucketOwner) {
            this.s3BucketOwner = s3BucketOwner;
            return this;
        }

        /**
         * The Amazon S3 bucket prefix used as the file name and path of the exported snapshot.
         *
         * @return this builder.
         */
        public Builder s3Prefix(String s3Prefix) {
            this.s3Prefix = s3Prefix;
            return this;
        }

        /**
         * Type of encryption used on the bucket where export data is stored. Valid values for <code>S3SseAlgorithm</code>
         * are:
         *
         * <ul>
         *   <li>
         *     <code>AES256</code> - server-side encryption with Amazon S3 managed keys
         *   </li>
         *   <li>
         *     <code>KMS</code> - server-side encryption with KMS managed keys
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder s3SseAlgorithm(S3SseAlgorithm s3SseAlgorithm) {
            this.s3SseAlgorithm = s3SseAlgorithm;
            return this;
        }

        /**
         * The ID of the KMS managed key used to encrypt the S3 bucket where export data is stored (if applicable).
         *
         * @return this builder.
         */
        public Builder s3SseKmsKeyId(String s3SseKmsKeyId) {
            this.s3SseKmsKeyId = s3SseKmsKeyId;
            return this;
        }

        /**
         * Status code for the result of the failed export.
         *
         * @return this builder.
         */
        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        /**
         * Export failure reason description.
         *
         * @return this builder.
         */
        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        /**
         * The format of the exported data. Valid values for <code>ExportFormat</code> are <code>DYNAMODB_JSON</code> or
         * <code>ION</code>.
         *
         * @return this builder.
         */
        public Builder exportFormat(ExportFormat exportFormat) {
            this.exportFormat = exportFormat;
            return this;
        }

        /**
         * The billable size of the table export.
         *
         * @return this builder.
         */
        public Builder billedSizeBytes(Long billedSizeBytes) {
            this.billedSizeBytes = billedSizeBytes;
            return this;
        }

        /**
         * The number of items exported.
         *
         * @return this builder.
         */
        public Builder itemCount(Long itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        /**
         * The type of export that was performed. Valid values are <code>FULL_EXPORT</code> or
         * <code>INCREMENTAL_EXPORT</code>.
         *
         * @return this builder.
         */
        public Builder exportType(ExportType exportType) {
            this.exportType = exportType;
            return this;
        }

        /**
         * Optional object containing the parameters specific to an incremental export.
         *
         * @return this builder.
         */
        public Builder incrementalExportSpecification(IncrementalExportSpecification incrementalExportSpecification) {
            this.incrementalExportSpecification = incrementalExportSpecification;
            return this;
        }

        @Override
        public ExportDescription build() {
            return new ExportDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exportArn((String) SchemaUtils.validateSameMember($SCHEMA_EXPORT_ARN, member, value));
                case 1 -> exportStatus((ExportStatus) SchemaUtils.validateSameMember($SCHEMA_EXPORT_STATUS, member, value));
                case 2 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 3 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
                case 4 -> exportManifest((String) SchemaUtils.validateSameMember($SCHEMA_EXPORT_MANIFEST, member, value));
                case 5 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 6 -> tableId((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, value));
                case 7 -> exportTime((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TIME, member, value));
                case 8 -> clientToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, value));
                case 9 -> s3Bucket((String) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET, member, value));
                case 10 -> s3BucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_OWNER, member, value));
                case 11 -> s3Prefix((String) SchemaUtils.validateSameMember($SCHEMA_S3_PREFIX, member, value));
                case 12 -> s3SseAlgorithm((S3SseAlgorithm) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_ALGORITHM, member, value));
                case 13 -> s3SseKmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_KMS_KEY_ID, member, value));
                case 14 -> failureCode((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_CODE, member, value));
                case 15 -> failureMessage((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_MESSAGE, member, value));
                case 16 -> exportFormat((ExportFormat) SchemaUtils.validateSameMember($SCHEMA_EXPORT_FORMAT, member, value));
                case 17 -> billedSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_BILLED_SIZE_BYTES, member, value));
                case 18 -> itemCount((Long) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, value));
                case 19 -> exportType((ExportType) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TYPE, member, value));
                case 20 -> incrementalExportSpecification((IncrementalExportSpecification) SchemaUtils.validateSameMember($SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION, member, value));
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
                    case 0 -> builder.exportArn(de.readString(member));
                    case 1 -> builder.exportStatus(ExportStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.startTime(de.readTimestamp(member));
                    case 3 -> builder.endTime(de.readTimestamp(member));
                    case 4 -> builder.exportManifest(de.readString(member));
                    case 5 -> builder.tableArn(de.readString(member));
                    case 6 -> builder.tableId(de.readString(member));
                    case 7 -> builder.exportTime(de.readTimestamp(member));
                    case 8 -> builder.clientToken(de.readString(member));
                    case 9 -> builder.s3Bucket(de.readString(member));
                    case 10 -> builder.s3BucketOwner(de.readString(member));
                    case 11 -> builder.s3Prefix(de.readString(member));
                    case 12 -> builder.s3SseAlgorithm(S3SseAlgorithm.builder().deserializeMember(de, member).build());
                    case 13 -> builder.s3SseKmsKeyId(de.readString(member));
                    case 14 -> builder.failureCode(de.readString(member));
                    case 15 -> builder.failureMessage(de.readString(member));
                    case 16 -> builder.exportFormat(ExportFormat.builder().deserializeMember(de, member).build());
                    case 17 -> builder.billedSizeBytes(de.readLong(member));
                    case 18 -> builder.itemCount(de.readLong(member));
                    case 19 -> builder.exportType(ExportType.builder().deserializeMember(de, member).build());
                    case 20 -> builder.incrementalExportSpecification(IncrementalExportSpecification.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
