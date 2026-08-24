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
 * Represents the properties of the table being imported into.
 */
@SmithyGenerated
public final class ImportTableDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.IMPORT_TABLE_DESCRIPTION;
    private static final Schema $SCHEMA_IMPORT_ARN = $SCHEMA.member("ImportArn");
    private static final Schema $SCHEMA_IMPORT_STATUS = $SCHEMA.member("ImportStatus");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_TABLE_ID = $SCHEMA.member("TableId");
    private static final Schema $SCHEMA_CLIENT_TOKEN = $SCHEMA.member("ClientToken");
    private static final Schema $SCHEMA_S3_BUCKET_SOURCE = $SCHEMA.member("S3BucketSource");
    private static final Schema $SCHEMA_ERROR_COUNT = $SCHEMA.member("ErrorCount");
    private static final Schema $SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN = $SCHEMA.member("CloudWatchLogGroupArn");
    private static final Schema $SCHEMA_INPUT_FORMAT = $SCHEMA.member("InputFormat");
    private static final Schema $SCHEMA_INPUT_FORMAT_OPTIONS = $SCHEMA.member("InputFormatOptions");
    private static final Schema $SCHEMA_INPUT_COMPRESSION_TYPE = $SCHEMA.member("InputCompressionType");
    private static final Schema $SCHEMA_TABLE_CREATION_PARAMETERS = $SCHEMA.member("TableCreationParameters");
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");
    private static final Schema $SCHEMA_PROCESSED_SIZE_BYTES = $SCHEMA.member("ProcessedSizeBytes");
    private static final Schema $SCHEMA_PROCESSED_ITEM_COUNT = $SCHEMA.member("ProcessedItemCount");
    private static final Schema $SCHEMA_IMPORTED_ITEM_COUNT = $SCHEMA.member("ImportedItemCount");
    private static final Schema $SCHEMA_FAILURE_CODE = $SCHEMA.member("FailureCode");
    private static final Schema $SCHEMA_FAILURE_MESSAGE = $SCHEMA.member("FailureMessage");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String importArn;
    private final transient ImportStatus importStatus;
    private final transient String tableArn;
    private final transient String tableId;
    private final transient String clientToken;
    private final transient S3BucketSource s3BucketSource;
    private final transient long errorCount;
    private final transient String cloudWatchLogGroupArn;
    private final transient InputFormat inputFormat;
    private final transient InputFormatOptions inputFormatOptions;
    private final transient InputCompressionType inputCompressionType;
    private final transient TableCreationParameters tableCreationParameters;
    private final transient Instant startTime;
    private final transient Instant endTime;
    private final transient Long processedSizeBytes;
    private final transient long processedItemCount;
    private final transient long importedItemCount;
    private final transient String failureCode;
    private final transient String failureMessage;

    private ImportTableDescription(Builder builder) {
        this.importArn = builder.importArn;
        this.importStatus = builder.importStatus;
        this.tableArn = builder.tableArn;
        this.tableId = builder.tableId;
        this.clientToken = builder.clientToken;
        this.s3BucketSource = builder.s3BucketSource;
        this.errorCount = builder.errorCount;
        this.cloudWatchLogGroupArn = builder.cloudWatchLogGroupArn;
        this.inputFormat = builder.inputFormat;
        this.inputFormatOptions = builder.inputFormatOptions;
        this.inputCompressionType = builder.inputCompressionType;
        this.tableCreationParameters = builder.tableCreationParameters;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.processedSizeBytes = builder.processedSizeBytes;
        this.processedItemCount = builder.processedItemCount;
        this.importedItemCount = builder.importedItemCount;
        this.failureCode = builder.failureCode;
        this.failureMessage = builder.failureMessage;
    }

    /**
     * The Amazon Resource Number (ARN) corresponding to the import request.
     */
    public String getImportArn() {
        return importArn;
    }

    /**
     * The status of the import.
     */
    public ImportStatus getImportStatus() {
        return importStatus;
    }

    /**
     * The Amazon Resource Number (ARN) of the table being imported into.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * The table id corresponding to the table created by import table process.
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * The client token that was provided for the import task. Reusing the client token on retry makes a call to
     * <code>ImportTable</code> idempotent.
     */
    public String getClientToken() {
        return clientToken;
    }

    /**
     * Values for the S3 bucket the source file is imported from. Includes bucket name (required), key prefix (optional)
     * and bucket account owner ID (optional).
     */
    public S3BucketSource getS3BucketSource() {
        return s3BucketSource;
    }

    /**
     * The number of errors occurred on importing the source file into the target table.
     */
    public long getErrorCount() {
        return errorCount;
    }

    /**
     * The Amazon Resource Number (ARN) of the Cloudwatch Log Group associated with the target table.
     */
    public String getCloudWatchLogGroupArn() {
        return cloudWatchLogGroupArn;
    }

    /**
     * The format of the source data going into the target table.
     */
    public InputFormat getInputFormat() {
        return inputFormat;
    }

    /**
     * The format options for the data that was imported into the target table. There is one value, CsvOption.
     */
    public InputFormatOptions getInputFormatOptions() {
        return inputFormatOptions;
    }

    /**
     * The compression options for the data that has been imported into the target table. The values are NONE, GZIP, or
     * ZSTD.
     */
    public InputCompressionType getInputCompressionType() {
        return inputCompressionType;
    }

    /**
     * The parameters for the new table that is being imported into.
     */
    public TableCreationParameters getTableCreationParameters() {
        return tableCreationParameters;
    }

    /**
     * The time when this import task started.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The time at which the creation of the table associated with this import task completed.
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * The total size of data processed from the source file, in Bytes.
     */
    public Long getProcessedSizeBytes() {
        return processedSizeBytes;
    }

    /**
     * The total number of items processed from the source file.
     */
    public long getProcessedItemCount() {
        return processedItemCount;
    }

    /**
     * The number of items successfully imported into the new table.
     */
    public long getImportedItemCount() {
        return importedItemCount;
    }

    /**
     * The error code corresponding to the failure that the import job ran into during execution.
     */
    public String getFailureCode() {
        return failureCode;
    }

    /**
     * The error message corresponding to the failure that the import job ran into during execution.
     */
    public String getFailureMessage() {
        return failureMessage;
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
        ImportTableDescription that = (ImportTableDescription) other;
        return this.errorCount == that.errorCount
               && this.processedItemCount == that.processedItemCount
               && this.importedItemCount == that.importedItemCount
               && Objects.equals(this.processedSizeBytes, that.processedSizeBytes)
               && Objects.equals(this.importArn, that.importArn)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.tableId, that.tableId)
               && Objects.equals(this.clientToken, that.clientToken)
               && Objects.equals(this.cloudWatchLogGroupArn, that.cloudWatchLogGroupArn)
               && Objects.equals(this.failureCode, that.failureCode)
               && Objects.equals(this.failureMessage, that.failureMessage)
               && Objects.equals(this.importStatus, that.importStatus)
               && Objects.equals(this.inputFormat, that.inputFormat)
               && Objects.equals(this.inputCompressionType, that.inputCompressionType)
               && Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime)
               && Objects.equals(this.s3BucketSource, that.s3BucketSource)
               && Objects.equals(this.inputFormatOptions, that.inputFormatOptions)
               && Objects.equals(this.tableCreationParameters, that.tableCreationParameters);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(importArn);
        $hc = 31 * $hc + Objects.hashCode(importStatus);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(tableId);
        $hc = 31 * $hc + Objects.hashCode(clientToken);
        $hc = 31 * $hc + Objects.hashCode(s3BucketSource);
        $hc = 31 * $hc + Long.hashCode(errorCount);
        $hc = 31 * $hc + Objects.hashCode(cloudWatchLogGroupArn);
        $hc = 31 * $hc + Objects.hashCode(inputFormat);
        $hc = 31 * $hc + Objects.hashCode(inputFormatOptions);
        $hc = 31 * $hc + Objects.hashCode(inputCompressionType);
        $hc = 31 * $hc + Objects.hashCode(tableCreationParameters);
        $hc = 31 * $hc + Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
        $hc = 31 * $hc + Objects.hashCode(processedSizeBytes);
        $hc = 31 * $hc + Long.hashCode(processedItemCount);
        $hc = 31 * $hc + Long.hashCode(importedItemCount);
        $hc = 31 * $hc + Objects.hashCode(failureCode);
        $hc = 31 * $hc + Objects.hashCode(failureMessage);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (importArn != null) {
            serializer.writeString($SCHEMA_IMPORT_ARN, importArn);
        }
        if (importStatus != null) {
            serializer.writeString($SCHEMA_IMPORT_STATUS, importStatus.getValue());
        }
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (tableId != null) {
            serializer.writeString($SCHEMA_TABLE_ID, tableId);
        }
        if (clientToken != null) {
            serializer.writeString($SCHEMA_CLIENT_TOKEN, clientToken);
        }
        if (s3BucketSource != null) {
            serializer.writeStruct($SCHEMA_S3_BUCKET_SOURCE, s3BucketSource);
        }
        serializer.writeLong($SCHEMA_ERROR_COUNT, errorCount);
        if (cloudWatchLogGroupArn != null) {
            serializer.writeString($SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN, cloudWatchLogGroupArn);
        }
        if (inputFormat != null) {
            serializer.writeString($SCHEMA_INPUT_FORMAT, inputFormat.getValue());
        }
        if (inputFormatOptions != null) {
            serializer.writeStruct($SCHEMA_INPUT_FORMAT_OPTIONS, inputFormatOptions);
        }
        if (inputCompressionType != null) {
            serializer.writeString($SCHEMA_INPUT_COMPRESSION_TYPE, inputCompressionType.getValue());
        }
        if (tableCreationParameters != null) {
            serializer.writeStruct($SCHEMA_TABLE_CREATION_PARAMETERS, tableCreationParameters);
        }
        if (startTime != null) {
            serializer.writeTimestamp($SCHEMA_START_TIME, startTime);
        }
        if (endTime != null) {
            serializer.writeTimestamp($SCHEMA_END_TIME, endTime);
        }
        if (processedSizeBytes != null) {
            serializer.writeLong($SCHEMA_PROCESSED_SIZE_BYTES, processedSizeBytes);
        }
        serializer.writeLong($SCHEMA_PROCESSED_ITEM_COUNT, processedItemCount);
        serializer.writeLong($SCHEMA_IMPORTED_ITEM_COUNT, importedItemCount);
        if (failureCode != null) {
            serializer.writeString($SCHEMA_FAILURE_CODE, failureCode);
        }
        if (failureMessage != null) {
            serializer.writeString($SCHEMA_FAILURE_MESSAGE, failureMessage);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_ARN, member, importArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_STATUS, member, importStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, tableId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, clientToken);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_SOURCE, member, s3BucketSource);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR_COUNT, member, errorCount);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN, member, cloudWatchLogGroupArn);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT, member, inputFormat);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT_OPTIONS, member, inputFormatOptions);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_COMPRESSION_TYPE, member, inputCompressionType);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CREATION_PARAMETERS, member, tableCreationParameters);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROCESSED_SIZE_BYTES, member, processedSizeBytes);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROCESSED_ITEM_COUNT, member, processedItemCount);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORTED_ITEM_COUNT, member, importedItemCount);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_CODE, member, failureCode);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_MESSAGE, member, failureMessage);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ImportTableDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.importArn(this.importArn);
        builder.importStatus(this.importStatus);
        builder.tableArn(this.tableArn);
        builder.tableId(this.tableId);
        builder.clientToken(this.clientToken);
        builder.s3BucketSource(this.s3BucketSource);
        builder.errorCount(this.errorCount);
        builder.cloudWatchLogGroupArn(this.cloudWatchLogGroupArn);
        builder.inputFormat(this.inputFormat);
        builder.inputFormatOptions(this.inputFormatOptions);
        builder.inputCompressionType(this.inputCompressionType);
        builder.tableCreationParameters(this.tableCreationParameters);
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        builder.processedSizeBytes(this.processedSizeBytes);
        builder.processedItemCount(this.processedItemCount);
        builder.importedItemCount(this.importedItemCount);
        builder.failureCode(this.failureCode);
        builder.failureMessage(this.failureMessage);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ImportTableDescription}.
     */
    public static final class Builder implements ShapeBuilder<ImportTableDescription> {
        private String importArn;
        private ImportStatus importStatus;
        private String tableArn;
        private String tableId;
        private String clientToken;
        private S3BucketSource s3BucketSource;
        private long errorCount = 0L;
        private String cloudWatchLogGroupArn;
        private InputFormat inputFormat;
        private InputFormatOptions inputFormatOptions;
        private InputCompressionType inputCompressionType;
        private TableCreationParameters tableCreationParameters;
        private Instant startTime;
        private Instant endTime;
        private Long processedSizeBytes;
        private long processedItemCount = 0L;
        private long importedItemCount = 0L;
        private String failureCode;
        private String failureMessage;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Number (ARN) corresponding to the import request.
         *
         * @return this builder.
         */
        public Builder importArn(String importArn) {
            this.importArn = importArn;
            return this;
        }

        /**
         * The status of the import.
         *
         * @return this builder.
         */
        public Builder importStatus(ImportStatus importStatus) {
            this.importStatus = importStatus;
            return this;
        }

        /**
         * The Amazon Resource Number (ARN) of the table being imported into.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * The table id corresponding to the table created by import table process.
         *
         * @return this builder.
         */
        public Builder tableId(String tableId) {
            this.tableId = tableId;
            return this;
        }

        /**
         * The client token that was provided for the import task. Reusing the client token on retry makes a call to
         * <code>ImportTable</code> idempotent.
         *
         * @return this builder.
         */
        public Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
            return this;
        }

        /**
         * Values for the S3 bucket the source file is imported from. Includes bucket name (required), key prefix (optional)
         * and bucket account owner ID (optional).
         *
         * @return this builder.
         */
        public Builder s3BucketSource(S3BucketSource s3BucketSource) {
            this.s3BucketSource = s3BucketSource;
            return this;
        }

        /**
         * The number of errors occurred on importing the source file into the target table.
         *
         * @return this builder.
         */
        public Builder errorCount(long errorCount) {
            this.errorCount = errorCount;
            return this;
        }

        /**
         * The Amazon Resource Number (ARN) of the Cloudwatch Log Group associated with the target table.
         *
         * @return this builder.
         */
        public Builder cloudWatchLogGroupArn(String cloudWatchLogGroupArn) {
            this.cloudWatchLogGroupArn = cloudWatchLogGroupArn;
            return this;
        }

        /**
         * The format of the source data going into the target table.
         *
         * @return this builder.
         */
        public Builder inputFormat(InputFormat inputFormat) {
            this.inputFormat = inputFormat;
            return this;
        }

        /**
         * The format options for the data that was imported into the target table. There is one value, CsvOption.
         *
         * @return this builder.
         */
        public Builder inputFormatOptions(InputFormatOptions inputFormatOptions) {
            this.inputFormatOptions = inputFormatOptions;
            return this;
        }

        /**
         * The compression options for the data that has been imported into the target table. The values are NONE, GZIP, or
         * ZSTD.
         *
         * @return this builder.
         */
        public Builder inputCompressionType(InputCompressionType inputCompressionType) {
            this.inputCompressionType = inputCompressionType;
            return this;
        }

        /**
         * The parameters for the new table that is being imported into.
         *
         * @return this builder.
         */
        public Builder tableCreationParameters(TableCreationParameters tableCreationParameters) {
            this.tableCreationParameters = tableCreationParameters;
            return this;
        }

        /**
         * The time when this import task started.
         *
         * @return this builder.
         */
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * The time at which the creation of the table associated with this import task completed.
         *
         * @return this builder.
         */
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * The total size of data processed from the source file, in Bytes.
         *
         * @return this builder.
         */
        public Builder processedSizeBytes(Long processedSizeBytes) {
            this.processedSizeBytes = processedSizeBytes;
            return this;
        }

        /**
         * The total number of items processed from the source file.
         *
         * @return this builder.
         */
        public Builder processedItemCount(long processedItemCount) {
            this.processedItemCount = processedItemCount;
            return this;
        }

        /**
         * The number of items successfully imported into the new table.
         *
         * @return this builder.
         */
        public Builder importedItemCount(long importedItemCount) {
            this.importedItemCount = importedItemCount;
            return this;
        }

        /**
         * The error code corresponding to the failure that the import job ran into during execution.
         *
         * @return this builder.
         */
        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        /**
         * The error message corresponding to the failure that the import job ran into during execution.
         *
         * @return this builder.
         */
        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        @Override
        public ImportTableDescription build() {
            return new ImportTableDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> importArn((String) SchemaUtils.validateSameMember($SCHEMA_IMPORT_ARN, member, value));
                case 1 -> importStatus((ImportStatus) SchemaUtils.validateSameMember($SCHEMA_IMPORT_STATUS, member, value));
                case 2 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 3 -> tableId((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, value));
                case 4 -> clientToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, value));
                case 5 -> s3BucketSource((S3BucketSource) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_SOURCE, member, value));
                case 6 -> errorCount((long) SchemaUtils.validateSameMember($SCHEMA_ERROR_COUNT, member, value));
                case 7 -> cloudWatchLogGroupArn((String) SchemaUtils.validateSameMember($SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN, member, value));
                case 8 -> inputFormat((InputFormat) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT, member, value));
                case 9 -> inputFormatOptions((InputFormatOptions) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT_OPTIONS, member, value));
                case 10 -> inputCompressionType((InputCompressionType) SchemaUtils.validateSameMember($SCHEMA_INPUT_COMPRESSION_TYPE, member, value));
                case 11 -> tableCreationParameters((TableCreationParameters) SchemaUtils.validateSameMember($SCHEMA_TABLE_CREATION_PARAMETERS, member, value));
                case 12 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 13 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
                case 14 -> processedSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_PROCESSED_SIZE_BYTES, member, value));
                case 15 -> processedItemCount((long) SchemaUtils.validateSameMember($SCHEMA_PROCESSED_ITEM_COUNT, member, value));
                case 16 -> importedItemCount((long) SchemaUtils.validateSameMember($SCHEMA_IMPORTED_ITEM_COUNT, member, value));
                case 17 -> failureCode((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_CODE, member, value));
                case 18 -> failureMessage((String) SchemaUtils.validateSameMember($SCHEMA_FAILURE_MESSAGE, member, value));
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
                    case 0 -> builder.importArn(de.readString(member));
                    case 1 -> builder.importStatus(ImportStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.tableArn(de.readString(member));
                    case 3 -> builder.tableId(de.readString(member));
                    case 4 -> builder.clientToken(de.readString(member));
                    case 5 -> builder.s3BucketSource(S3BucketSource.builder().deserializeMember(de, member).build());
                    case 6 -> builder.errorCount(de.readLong(member));
                    case 7 -> builder.cloudWatchLogGroupArn(de.readString(member));
                    case 8 -> builder.inputFormat(InputFormat.builder().deserializeMember(de, member).build());
                    case 9 -> builder.inputFormatOptions(InputFormatOptions.builder().deserializeMember(de, member).build());
                    case 10 -> builder.inputCompressionType(InputCompressionType.builder().deserializeMember(de, member).build());
                    case 11 -> builder.tableCreationParameters(TableCreationParameters.builder().deserializeMember(de, member).build());
                    case 12 -> builder.startTime(de.readTimestamp(member));
                    case 13 -> builder.endTime(de.readTimestamp(member));
                    case 14 -> builder.processedSizeBytes(de.readLong(member));
                    case 15 -> builder.processedItemCount(de.readLong(member));
                    case 16 -> builder.importedItemCount(de.readLong(member));
                    case 17 -> builder.failureCode(de.readString(member));
                    case 18 -> builder.failureMessage(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
