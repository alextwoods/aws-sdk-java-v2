package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class ImportTableInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.IMPORT_TABLE_INPUT;
    private static final Schema $SCHEMA_CLIENT_TOKEN = $SCHEMA.member("ClientToken");
    private static final Schema $SCHEMA_S3_BUCKET_SOURCE = $SCHEMA.member("S3BucketSource");
    private static final Schema $SCHEMA_INPUT_FORMAT = $SCHEMA.member("InputFormat");
    private static final Schema $SCHEMA_INPUT_FORMAT_OPTIONS = $SCHEMA.member("InputFormatOptions");
    private static final Schema $SCHEMA_INPUT_COMPRESSION_TYPE = $SCHEMA.member("InputCompressionType");
    private static final Schema $SCHEMA_TABLE_CREATION_PARAMETERS = $SCHEMA.member("TableCreationParameters");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String clientToken;
    private final transient S3BucketSource s3BucketSource;
    private final transient InputFormat inputFormat;
    private final transient InputFormatOptions inputFormatOptions;
    private final transient InputCompressionType inputCompressionType;
    private final transient TableCreationParameters tableCreationParameters;

    private ImportTableInput(Builder builder) {
        this.clientToken = builder.clientToken;
        this.s3BucketSource = builder.s3BucketSource;
        this.inputFormat = builder.inputFormat;
        this.inputFormatOptions = builder.inputFormatOptions;
        this.inputCompressionType = builder.inputCompressionType;
        this.tableCreationParameters = builder.tableCreationParameters;
    }

    /**
     * Providing a <code>ClientToken</code> makes the call to <code>ImportTableInput</code> idempotent, meaning that
     * multiple identical calls have the same effect as one single call.
     *
     * <p>A client token is valid for 8 hours after the first request that uses it is completed. After 8 hours, any
     * request with the same client token is treated as a new request. Do not resubmit the same request with the same
     * client token for more than 8 hours, or the result might not be idempotent.
     *
     * <p>If you submit a request with the same client token but a change in other parameters within the 8-hour
     * idempotency window, DynamoDB returns an <code>IdempotentParameterMismatch</code> exception.
     */
    public String getClientToken() {
        return clientToken;
    }

    /**
     * The S3 bucket that provides the source for the import.
     */
    public S3BucketSource getS3BucketSource() {
        return s3BucketSource;
    }

    /**
     * The format of the source data. Valid values for <code>ImportFormat</code> are <code>CSV</code>,
     * <code>DYNAMODB_JSON</code> or <code>ION</code>.
     */
    public InputFormat getInputFormat() {
        return inputFormat;
    }

    /**
     * Additional properties that specify how the input is formatted,
     */
    public InputFormatOptions getInputFormatOptions() {
        return inputFormatOptions;
    }

    /**
     * Type of compression to be used on the input coming from the imported table.
     */
    public InputCompressionType getInputCompressionType() {
        return inputCompressionType;
    }

    /**
     * Parameters for the table to import the data into.
     */
    public TableCreationParameters getTableCreationParameters() {
        return tableCreationParameters;
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
        ImportTableInput that = (ImportTableInput) other;
        return Objects.equals(this.clientToken, that.clientToken)
               && Objects.equals(this.inputFormat, that.inputFormat)
               && Objects.equals(this.inputCompressionType, that.inputCompressionType)
               && Objects.equals(this.s3BucketSource, that.s3BucketSource)
               && Objects.equals(this.inputFormatOptions, that.inputFormatOptions)
               && Objects.equals(this.tableCreationParameters, that.tableCreationParameters);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(clientToken);
        $hc = 31 * $hc + Objects.hashCode(s3BucketSource);
        $hc = 31 * $hc + Objects.hashCode(inputFormat);
        $hc = 31 * $hc + Objects.hashCode(inputFormatOptions);
        $hc = 31 * $hc + Objects.hashCode(inputCompressionType);
        $hc = 31 * $hc + Objects.hashCode(tableCreationParameters);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (clientToken != null) {
            serializer.writeString($SCHEMA_CLIENT_TOKEN, clientToken);
        }
        if (s3BucketSource != null) {
            serializer.writeStruct($SCHEMA_S3_BUCKET_SOURCE, s3BucketSource);
        }
        serializer.writeString($SCHEMA_INPUT_FORMAT, inputFormat.getValue());
        if (inputFormatOptions != null) {
            serializer.writeStruct($SCHEMA_INPUT_FORMAT_OPTIONS, inputFormatOptions);
        }
        if (inputCompressionType != null) {
            serializer.writeString($SCHEMA_INPUT_COMPRESSION_TYPE, inputCompressionType.getValue());
        }
        if (tableCreationParameters != null) {
            serializer.writeStruct($SCHEMA_TABLE_CREATION_PARAMETERS, tableCreationParameters);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_SOURCE, member, s3BucketSource);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT, member, inputFormat);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CREATION_PARAMETERS, member, tableCreationParameters);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, clientToken);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT_OPTIONS, member, inputFormatOptions);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_COMPRESSION_TYPE, member, inputCompressionType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ImportTableInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.clientToken(this.clientToken);
        builder.s3BucketSource(this.s3BucketSource);
        builder.inputFormat(this.inputFormat);
        builder.inputFormatOptions(this.inputFormatOptions);
        builder.inputCompressionType(this.inputCompressionType);
        builder.tableCreationParameters(this.tableCreationParameters);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ImportTableInput}.
     */
    public static final class Builder implements ShapeBuilder<ImportTableInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String clientToken;
        private S3BucketSource s3BucketSource;
        private InputFormat inputFormat;
        private InputFormatOptions inputFormatOptions;
        private InputCompressionType inputCompressionType;
        private TableCreationParameters tableCreationParameters;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Providing a <code>ClientToken</code> makes the call to <code>ImportTableInput</code> idempotent, meaning that
         * multiple identical calls have the same effect as one single call.
         *
         * <p>A client token is valid for 8 hours after the first request that uses it is completed. After 8 hours, any
         * request with the same client token is treated as a new request. Do not resubmit the same request with the same
         * client token for more than 8 hours, or the result might not be idempotent.
         *
         * <p>If you submit a request with the same client token but a change in other parameters within the 8-hour
         * idempotency window, DynamoDB returns an <code>IdempotentParameterMismatch</code> exception.
         *
         * @return this builder.
         */
        public Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
            return this;
        }

        /**
         * The S3 bucket that provides the source for the import.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder s3BucketSource(S3BucketSource s3BucketSource) {
            this.s3BucketSource = Objects.requireNonNull(s3BucketSource, "s3BucketSource cannot be null");
            tracker.setMember($SCHEMA_S3_BUCKET_SOURCE);
            return this;
        }

        /**
         * The format of the source data. Valid values for <code>ImportFormat</code> are <code>CSV</code>,
         * <code>DYNAMODB_JSON</code> or <code>ION</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder inputFormat(InputFormat inputFormat) {
            this.inputFormat = Objects.requireNonNull(inputFormat, "inputFormat cannot be null");
            tracker.setMember($SCHEMA_INPUT_FORMAT);
            return this;
        }

        /**
         * Additional properties that specify how the input is formatted,
         *
         * @return this builder.
         */
        public Builder inputFormatOptions(InputFormatOptions inputFormatOptions) {
            this.inputFormatOptions = inputFormatOptions;
            return this;
        }

        /**
         * Type of compression to be used on the input coming from the imported table.
         *
         * @return this builder.
         */
        public Builder inputCompressionType(InputCompressionType inputCompressionType) {
            this.inputCompressionType = inputCompressionType;
            return this;
        }

        /**
         * Parameters for the table to import the data into.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableCreationParameters(TableCreationParameters tableCreationParameters) {
            this.tableCreationParameters = Objects.requireNonNull(tableCreationParameters, "tableCreationParameters cannot be null");
            tracker.setMember($SCHEMA_TABLE_CREATION_PARAMETERS);
            return this;
        }

        @Override
        public ImportTableInput build() {
            tracker.validate();
            return new ImportTableInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s3BucketSource((S3BucketSource) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_SOURCE, member, value));
                case 1 -> inputFormat((InputFormat) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT, member, value));
                case 2 -> tableCreationParameters((TableCreationParameters) SchemaUtils.validateSameMember($SCHEMA_TABLE_CREATION_PARAMETERS, member, value));
                case 3 -> clientToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, value));
                case 4 -> inputFormatOptions((InputFormatOptions) SchemaUtils.validateSameMember($SCHEMA_INPUT_FORMAT_OPTIONS, member, value));
                case 5 -> inputCompressionType((InputCompressionType) SchemaUtils.validateSameMember($SCHEMA_INPUT_COMPRESSION_TYPE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ImportTableInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_S3_BUCKET_SOURCE)) {
                tracker.setMember($SCHEMA_S3_BUCKET_SOURCE);
            }
            if (!tracker.checkMember($SCHEMA_INPUT_FORMAT)) {
                inputFormat(InputFormat.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_TABLE_CREATION_PARAMETERS)) {
                tracker.setMember($SCHEMA_TABLE_CREATION_PARAMETERS);
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
                    case 0 -> builder.s3BucketSource(S3BucketSource.builder().deserializeMember(de, member).build());
                    case 1 -> builder.inputFormat(InputFormat.builder().deserializeMember(de, member).build());
                    case 2 -> builder.tableCreationParameters(TableCreationParameters.builder().deserializeMember(de, member).build());
                    case 3 -> builder.clientToken(de.readString(member));
                    case 4 -> builder.inputFormatOptions(InputFormatOptions.builder().deserializeMember(de, member).build());
                    case 5 -> builder.inputCompressionType(InputCompressionType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
