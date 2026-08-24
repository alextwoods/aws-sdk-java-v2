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
 * Summary information about the source file for the import.
 */
@SmithyGenerated
public final class ImportSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.IMPORT_SUMMARY;
    private static final Schema $SCHEMA_IMPORT_ARN = $SCHEMA.member("ImportArn");
    private static final Schema $SCHEMA_IMPORT_STATUS = $SCHEMA.member("ImportStatus");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_S3_BUCKET_SOURCE = $SCHEMA.member("S3BucketSource");
    private static final Schema $SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN = $SCHEMA.member("CloudWatchLogGroupArn");
    private static final Schema $SCHEMA_INPUT_FORMAT = $SCHEMA.member("InputFormat");
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String importArn;
    private final transient ImportStatus importStatus;
    private final transient String tableArn;
    private final transient S3BucketSource s3BucketSource;
    private final transient String cloudWatchLogGroupArn;
    private final transient InputFormat inputFormat;
    private final transient Instant startTime;
    private final transient Instant endTime;

    private ImportSummary(Builder builder) {
        this.importArn = builder.importArn;
        this.importStatus = builder.importStatus;
        this.tableArn = builder.tableArn;
        this.s3BucketSource = builder.s3BucketSource;
        this.cloudWatchLogGroupArn = builder.cloudWatchLogGroupArn;
        this.inputFormat = builder.inputFormat;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
    }

    /**
     * The Amazon Resource Number (ARN) corresponding to the import request.
     */
    public String getImportArn() {
        return importArn;
    }

    /**
     * The status of the import operation.
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
     * The path and S3 bucket of the source file that is being imported. This includes the S3Bucket (required),
     * S3KeyPrefix (optional) and S3BucketOwner (optional if the bucket is owned by the requester).
     */
    public S3BucketSource getS3BucketSource() {
        return s3BucketSource;
    }

    /**
     * The Amazon Resource Number (ARN) of the Cloudwatch Log Group associated with this import task.
     */
    public String getCloudWatchLogGroupArn() {
        return cloudWatchLogGroupArn;
    }

    /**
     * The format of the source data. Valid values are <code>CSV</code>, <code>DYNAMODB_JSON</code> or <code>ION</code>.
     */
    public InputFormat getInputFormat() {
        return inputFormat;
    }

    /**
     * The time at which this import task began.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The time at which this import task ended.
     */
    public Instant getEndTime() {
        return endTime;
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
        ImportSummary that = (ImportSummary) other;
        return Objects.equals(this.importArn, that.importArn)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.cloudWatchLogGroupArn, that.cloudWatchLogGroupArn)
               && Objects.equals(this.importStatus, that.importStatus)
               && Objects.equals(this.inputFormat, that.inputFormat)
               && Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime)
               && Objects.equals(this.s3BucketSource, that.s3BucketSource);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(importArn);
        $hc = 31 * $hc + Objects.hashCode(importStatus);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(s3BucketSource);
        $hc = 31 * $hc + Objects.hashCode(cloudWatchLogGroupArn);
        $hc = 31 * $hc + Objects.hashCode(inputFormat);
        $hc = 31 * $hc + Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
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
        if (s3BucketSource != null) {
            serializer.writeStruct($SCHEMA_S3_BUCKET_SOURCE, s3BucketSource);
        }
        if (cloudWatchLogGroupArn != null) {
            serializer.writeString($SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN, cloudWatchLogGroupArn);
        }
        if (inputFormat != null) {
            serializer.writeString($SCHEMA_INPUT_FORMAT, inputFormat.getValue());
        }
        if (startTime != null) {
            serializer.writeTimestamp($SCHEMA_START_TIME, startTime);
        }
        if (endTime != null) {
            serializer.writeTimestamp($SCHEMA_END_TIME, endTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_ARN, member, importArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_STATUS, member, importStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_SOURCE, member, s3BucketSource);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN, member, cloudWatchLogGroupArn);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT, member, inputFormat);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ImportSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.importArn(this.importArn);
        builder.importStatus(this.importStatus);
        builder.tableArn(this.tableArn);
        builder.s3BucketSource(this.s3BucketSource);
        builder.cloudWatchLogGroupArn(this.cloudWatchLogGroupArn);
        builder.inputFormat(this.inputFormat);
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ImportSummary}.
     */
    public static final class Builder implements ShapeBuilder<ImportSummary> {
        private String importArn;
        private ImportStatus importStatus;
        private String tableArn;
        private S3BucketSource s3BucketSource;
        private String cloudWatchLogGroupArn;
        private InputFormat inputFormat;
        private Instant startTime;
        private Instant endTime;

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
         * The status of the import operation.
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
         * The path and S3 bucket of the source file that is being imported. This includes the S3Bucket (required),
         * S3KeyPrefix (optional) and S3BucketOwner (optional if the bucket is owned by the requester).
         *
         * @return this builder.
         */
        public Builder s3BucketSource(S3BucketSource s3BucketSource) {
            this.s3BucketSource = s3BucketSource;
            return this;
        }

        /**
         * The Amazon Resource Number (ARN) of the Cloudwatch Log Group associated with this import task.
         *
         * @return this builder.
         */
        public Builder cloudWatchLogGroupArn(String cloudWatchLogGroupArn) {
            this.cloudWatchLogGroupArn = cloudWatchLogGroupArn;
            return this;
        }

        /**
         * The format of the source data. Valid values are <code>CSV</code>, <code>DYNAMODB_JSON</code> or <code>ION</code>.
         *
         * @return this builder.
         */
        public Builder inputFormat(InputFormat inputFormat) {
            this.inputFormat = inputFormat;
            return this;
        }

        /**
         * The time at which this import task began.
         *
         * @return this builder.
         */
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * The time at which this import task ended.
         *
         * @return this builder.
         */
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        @Override
        public ImportSummary build() {
            return new ImportSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> importArn((String) SchemaUtils.validateSameMember($SCHEMA_IMPORT_ARN, member, value));
                case 1 -> importStatus((ImportStatus) SchemaUtils.validateSameMember($SCHEMA_IMPORT_STATUS, member, value));
                case 2 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 3 -> s3BucketSource((S3BucketSource) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_SOURCE, member, value));
                case 4 -> cloudWatchLogGroupArn((String) SchemaUtils.validateSameMember($SCHEMA_CLOUD_WATCH_LOG_GROUP_ARN, member, value));
                case 5 -> inputFormat((InputFormat) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT, member, value));
                case 6 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 7 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
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
                    case 3 -> builder.s3BucketSource(S3BucketSource.builder().deserializeMember(de, member).build());
                    case 4 -> builder.cloudWatchLogGroupArn(de.readString(member));
                    case 5 -> builder.inputFormat(InputFormat.builder().deserializeMember(de, member).build());
                    case 6 -> builder.startTime(de.readTimestamp(member));
                    case 7 -> builder.endTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
