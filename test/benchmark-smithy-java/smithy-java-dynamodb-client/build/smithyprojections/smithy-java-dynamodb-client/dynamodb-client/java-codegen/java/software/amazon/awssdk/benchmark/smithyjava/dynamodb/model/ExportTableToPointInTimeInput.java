package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
public final class ExportTableToPointInTimeInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXPORT_TABLE_TO_POINT_IN_TIME_INPUT;
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_EXPORT_TIME = $SCHEMA.member("ExportTime");
    private static final Schema $SCHEMA_CLIENT_TOKEN = $SCHEMA.member("ClientToken");
    private static final Schema $SCHEMA_S3_BUCKET = $SCHEMA.member("S3Bucket");
    private static final Schema $SCHEMA_S3_BUCKET_OWNER = $SCHEMA.member("S3BucketOwner");
    private static final Schema $SCHEMA_S3_PREFIX = $SCHEMA.member("S3Prefix");
    private static final Schema $SCHEMA_S3_SSE_ALGORITHM = $SCHEMA.member("S3SseAlgorithm");
    private static final Schema $SCHEMA_S3_SSE_KMS_KEY_ID = $SCHEMA.member("S3SseKmsKeyId");
    private static final Schema $SCHEMA_EXPORT_FORMAT = $SCHEMA.member("ExportFormat");
    private static final Schema $SCHEMA_EXPORT_TYPE = $SCHEMA.member("ExportType");
    private static final Schema $SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION = $SCHEMA.member("IncrementalExportSpecification");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableArn;
    private final transient Instant exportTime;
    private final transient String clientToken;
    private final transient String s3Bucket;
    private final transient String s3BucketOwner;
    private final transient String s3Prefix;
    private final transient S3SseAlgorithm s3SseAlgorithm;
    private final transient String s3SseKmsKeyId;
    private final transient ExportFormat exportFormat;
    private final transient ExportType exportType;
    private final transient IncrementalExportSpecification incrementalExportSpecification;

    private ExportTableToPointInTimeInput(Builder builder) {
        this.tableArn = builder.tableArn;
        this.exportTime = builder.exportTime;
        this.clientToken = builder.clientToken;
        this.s3Bucket = builder.s3Bucket;
        this.s3BucketOwner = builder.s3BucketOwner;
        this.s3Prefix = builder.s3Prefix;
        this.s3SseAlgorithm = builder.s3SseAlgorithm;
        this.s3SseKmsKeyId = builder.s3SseKmsKeyId;
        this.exportFormat = builder.exportFormat;
        this.exportType = builder.exportType;
        this.incrementalExportSpecification = builder.incrementalExportSpecification;
    }

    /**
     * The Amazon Resource Name (ARN) associated with the table to export.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * Time in the past from which to export table data, counted in seconds from the start of the Unix epoch. The table
     * export will be a snapshot of the table's state at this point in time.
     */
    public Instant getExportTime() {
        return exportTime;
    }

    /**
     * Providing a <code>ClientToken</code> makes the call to <code>ExportTableToPointInTimeInput</code> idempotent,
     * meaning that multiple identical calls have the same effect as one single call.
     *
     * <p>A client token is valid for 8 hours after the first request that uses it is completed. After 8 hours, any
     * request with the same client token is treated as a new request. Do not resubmit the same request with the same
     * client token for more than 8 hours, or the result might not be idempotent.
     *
     * <p>If you submit a request with the same client token but a change in other parameters within the 8-hour
     * idempotency window, DynamoDB returns an <code>ExportConflictException</code>.
     */
    public String getClientToken() {
        return clientToken;
    }

    /**
     * The name of the Amazon S3 bucket to export the snapshot to.
     */
    public String getS3Bucket() {
        return s3Bucket;
    }

    /**
     * The ID of the Amazon Web Services account that owns the bucket the export will be stored in.
     *
     * <p>S3BucketOwner is a required parameter when exporting to a S3 bucket in another account.
     */
    public String getS3BucketOwner() {
        return s3BucketOwner;
    }

    /**
     * The Amazon S3 bucket prefix to use as the file name and path of the exported snapshot.
     */
    public String getS3Prefix() {
        return s3Prefix;
    }

    /**
     * Type of encryption used on the bucket where export data will be stored. Valid values for <code>S3SseAlgorithm</code>
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
     * The ID of the KMS managed key used to encrypt the S3 bucket where export data will be stored (if applicable).
     */
    public String getS3SseKmsKeyId() {
        return s3SseKmsKeyId;
    }

    /**
     * The format for the exported data. Valid values for <code>ExportFormat</code> are <code>DYNAMODB_JSON</code> or
     * <code>ION</code>.
     */
    public ExportFormat getExportFormat() {
        return exportFormat;
    }

    /**
     * Choice of whether to execute as a full export or incremental export. Valid values are FULL_EXPORT or
     * INCREMENTAL_EXPORT. The default value is FULL_EXPORT. If INCREMENTAL_EXPORT is provided, the
     * IncrementalExportSpecification must also be used.
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
        ExportTableToPointInTimeInput that = (ExportTableToPointInTimeInput) other;
        return Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.clientToken, that.clientToken)
               && Objects.equals(this.s3Bucket, that.s3Bucket)
               && Objects.equals(this.s3BucketOwner, that.s3BucketOwner)
               && Objects.equals(this.s3Prefix, that.s3Prefix)
               && Objects.equals(this.s3SseKmsKeyId, that.s3SseKmsKeyId)
               && Objects.equals(this.s3SseAlgorithm, that.s3SseAlgorithm)
               && Objects.equals(this.exportFormat, that.exportFormat)
               && Objects.equals(this.exportType, that.exportType)
               && Objects.equals(this.exportTime, that.exportTime)
               && Objects.equals(this.incrementalExportSpecification, that.incrementalExportSpecification);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(exportTime);
        $hc = 31 * $hc + Objects.hashCode(clientToken);
        $hc = 31 * $hc + Objects.hashCode(s3Bucket);
        $hc = 31 * $hc + Objects.hashCode(s3BucketOwner);
        $hc = 31 * $hc + Objects.hashCode(s3Prefix);
        $hc = 31 * $hc + Objects.hashCode(s3SseAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(s3SseKmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(exportFormat);
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
        serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        if (exportTime != null) {
            serializer.writeTimestamp($SCHEMA_EXPORT_TIME, exportTime);
        }
        if (clientToken != null) {
            serializer.writeString($SCHEMA_CLIENT_TOKEN, clientToken);
        }
        serializer.writeString($SCHEMA_S3_BUCKET, s3Bucket);
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
        if (exportFormat != null) {
            serializer.writeString($SCHEMA_EXPORT_FORMAT, exportFormat.getValue());
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET, member, s3Bucket);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TIME, member, exportTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, clientToken);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_OWNER, member, s3BucketOwner);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_PREFIX, member, s3Prefix);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_ALGORITHM, member, s3SseAlgorithm);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_KMS_KEY_ID, member, s3SseKmsKeyId);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_FORMAT, member, exportFormat);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TYPE, member, exportType);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION, member, incrementalExportSpecification);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExportTableToPointInTimeInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableArn(this.tableArn);
        builder.exportTime(this.exportTime);
        builder.clientToken(this.clientToken);
        builder.s3Bucket(this.s3Bucket);
        builder.s3BucketOwner(this.s3BucketOwner);
        builder.s3Prefix(this.s3Prefix);
        builder.s3SseAlgorithm(this.s3SseAlgorithm);
        builder.s3SseKmsKeyId(this.s3SseKmsKeyId);
        builder.exportFormat(this.exportFormat);
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
     * Builder for {@link ExportTableToPointInTimeInput}.
     */
    public static final class Builder implements ShapeBuilder<ExportTableToPointInTimeInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableArn;
        private Instant exportTime;
        private String clientToken;
        private String s3Bucket;
        private String s3BucketOwner;
        private String s3Prefix;
        private S3SseAlgorithm s3SseAlgorithm;
        private String s3SseKmsKeyId;
        private ExportFormat exportFormat;
        private ExportType exportType;
        private IncrementalExportSpecification incrementalExportSpecification;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) associated with the table to export.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = Objects.requireNonNull(tableArn, "tableArn cannot be null");
            tracker.setMember($SCHEMA_TABLE_ARN);
            return this;
        }

        /**
         * Time in the past from which to export table data, counted in seconds from the start of the Unix epoch. The table
         * export will be a snapshot of the table's state at this point in time.
         *
         * @return this builder.
         */
        public Builder exportTime(Instant exportTime) {
            this.exportTime = exportTime;
            return this;
        }

        /**
         * Providing a <code>ClientToken</code> makes the call to <code>ExportTableToPointInTimeInput</code> idempotent,
         * meaning that multiple identical calls have the same effect as one single call.
         *
         * <p>A client token is valid for 8 hours after the first request that uses it is completed. After 8 hours, any
         * request with the same client token is treated as a new request. Do not resubmit the same request with the same
         * client token for more than 8 hours, or the result might not be idempotent.
         *
         * <p>If you submit a request with the same client token but a change in other parameters within the 8-hour
         * idempotency window, DynamoDB returns an <code>ExportConflictException</code>.
         *
         * @return this builder.
         */
        public Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
            return this;
        }

        /**
         * The name of the Amazon S3 bucket to export the snapshot to.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder s3Bucket(String s3Bucket) {
            this.s3Bucket = Objects.requireNonNull(s3Bucket, "s3Bucket cannot be null");
            tracker.setMember($SCHEMA_S3_BUCKET);
            return this;
        }

        /**
         * The ID of the Amazon Web Services account that owns the bucket the export will be stored in.
         *
         * <p>S3BucketOwner is a required parameter when exporting to a S3 bucket in another account.
         *
         * @return this builder.
         */
        public Builder s3BucketOwner(String s3BucketOwner) {
            this.s3BucketOwner = s3BucketOwner;
            return this;
        }

        /**
         * The Amazon S3 bucket prefix to use as the file name and path of the exported snapshot.
         *
         * @return this builder.
         */
        public Builder s3Prefix(String s3Prefix) {
            this.s3Prefix = s3Prefix;
            return this;
        }

        /**
         * Type of encryption used on the bucket where export data will be stored. Valid values for <code>S3SseAlgorithm</code>
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
         * The ID of the KMS managed key used to encrypt the S3 bucket where export data will be stored (if applicable).
         *
         * @return this builder.
         */
        public Builder s3SseKmsKeyId(String s3SseKmsKeyId) {
            this.s3SseKmsKeyId = s3SseKmsKeyId;
            return this;
        }

        /**
         * The format for the exported data. Valid values for <code>ExportFormat</code> are <code>DYNAMODB_JSON</code> or
         * <code>ION</code>.
         *
         * @return this builder.
         */
        public Builder exportFormat(ExportFormat exportFormat) {
            this.exportFormat = exportFormat;
            return this;
        }

        /**
         * Choice of whether to execute as a full export or incremental export. Valid values are FULL_EXPORT or
         * INCREMENTAL_EXPORT. The default value is FULL_EXPORT. If INCREMENTAL_EXPORT is provided, the
         * IncrementalExportSpecification must also be used.
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
        public ExportTableToPointInTimeInput build() {
            tracker.validate();
            return new ExportTableToPointInTimeInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 1 -> s3Bucket((String) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET, member, value));
                case 2 -> exportTime((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TIME, member, value));
                case 3 -> clientToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, value));
                case 4 -> s3BucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_OWNER, member, value));
                case 5 -> s3Prefix((String) SchemaUtils.validateSameMember($SCHEMA_S3_PREFIX, member, value));
                case 6 -> s3SseAlgorithm((S3SseAlgorithm) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_ALGORITHM, member, value));
                case 7 -> s3SseKmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_S3_SSE_KMS_KEY_ID, member, value));
                case 8 -> exportFormat((ExportFormat) SchemaUtils.validateSameMember($SCHEMA_EXPORT_FORMAT, member, value));
                case 9 -> exportType((ExportType) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TYPE, member, value));
                case 10 -> incrementalExportSpecification((IncrementalExportSpecification) SchemaUtils.validateSameMember($SCHEMA_INCREMENTAL_EXPORT_SPECIFICATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ExportTableToPointInTimeInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_ARN)) {
                tableArn("");
            }
            if (!tracker.checkMember($SCHEMA_S3_BUCKET)) {
                s3Bucket("");
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
                    case 0 -> builder.tableArn(de.readString(member));
                    case 1 -> builder.s3Bucket(de.readString(member));
                    case 2 -> builder.exportTime(de.readTimestamp(member));
                    case 3 -> builder.clientToken(de.readString(member));
                    case 4 -> builder.s3BucketOwner(de.readString(member));
                    case 5 -> builder.s3Prefix(de.readString(member));
                    case 6 -> builder.s3SseAlgorithm(S3SseAlgorithm.builder().deserializeMember(de, member).build());
                    case 7 -> builder.s3SseKmsKeyId(de.readString(member));
                    case 8 -> builder.exportFormat(ExportFormat.builder().deserializeMember(de, member).build());
                    case 9 -> builder.exportType(ExportType.builder().deserializeMember(de, member).build());
                    case 10 -> builder.incrementalExportSpecification(IncrementalExportSpecification.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
