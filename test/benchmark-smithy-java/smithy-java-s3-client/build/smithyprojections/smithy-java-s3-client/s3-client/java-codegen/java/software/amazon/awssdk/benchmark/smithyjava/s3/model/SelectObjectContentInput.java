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
 * Learn Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue
 * to use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
 *
 * <p>Request to filter the contents of an Amazon S3 object based on a simple Structured Query Language (SQL) statement.
 * In the request, along with the SQL expression, you must specify a data serialization format (JSON or CSV) of the
 * object. Amazon S3 uses this to parse object data into records. It returns only records that match the specified SQL
 * expression. You must also specify the data serialization format for the response. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectSELECTContent.html">
 * S3Select API Documentation</a>.
 */
@SmithyGenerated
public final class SelectObjectContentInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.SELECT_OBJECT_CONTENT_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_EXPRESSION = $SCHEMA.member("Expression");
    private static final Schema $SCHEMA_EXPRESSION_TYPE = $SCHEMA.member("ExpressionType");
    private static final Schema $SCHEMA_REQUEST_PROGRESS = $SCHEMA.member("RequestProgress");
    private static final Schema $SCHEMA_INPUT_SERIALIZATION = $SCHEMA.member("InputSerialization");
    private static final Schema $SCHEMA_OUTPUT_SERIALIZATION = $SCHEMA.member("OutputSerialization");
    private static final Schema $SCHEMA_SCAN_RANGE = $SCHEMA.member("ScanRange");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;
    private final transient String expression;
    private final transient ExpressionType expressionType;
    private final transient RequestProgress requestProgress;
    private final transient InputSerialization inputSerialization;
    private final transient OutputSerialization outputSerialization;
    private final transient ScanRange scanRange;
    private final transient String expectedBucketOwner;

    private SelectObjectContentInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.expression = builder.expression;
        this.expressionType = builder.expressionType;
        this.requestProgress = builder.requestProgress;
        this.inputSerialization = builder.inputSerialization;
        this.outputSerialization = builder.outputSerialization;
        this.scanRange = builder.scanRange;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The S3 bucket.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * The server-side encryption (SSE) algorithm used to encrypt the object. This parameter is needed only when the
     * object was created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a>
     * in the <i>Amazon S3 User Guide</i>.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * The server-side encryption (SSE) customer managed key. This parameter is needed only when the object was created
     * using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>Amazon S3
     * User Guide</i>.
     */
    public String getSseCustomerKey() {
        return sseCustomerKey;
    }

    /**
     * The MD5 server-side encryption (SSE) customer managed key. This parameter is needed only when the object was
     * created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>
     * Amazon S3 User Guide</i>.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
    }

    /**
     * The expression that is used to query the object.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * The type of the provided expression (for example, SQL).
     */
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    /**
     * Specifies if periodic request progress information should be enabled.
     */
    public RequestProgress getRequestProgress() {
        return requestProgress;
    }

    /**
     * Describes the format of the data in the object that is being queried.
     */
    public InputSerialization getInputSerialization() {
        return inputSerialization;
    }

    /**
     * Describes the format of the data that you want Amazon S3 to return in response.
     */
    public OutputSerialization getOutputSerialization() {
        return outputSerialization;
    }

    /**
     * Specifies the byte range of the object to get the records from. A record is processed when its first byte is
     * contained by the range. This parameter is optional, but when specified, it must not be empty. See RFC 2616,
     * Section 14.35.1 about how to specify the start and end of the range.
     *
     * <p><code>ScanRange</code>may be used in the following ways:
     *
     * <ul>
     *   <li>
     *     <code>50100</code> - process only the records starting between the bytes 50 and 100 (inclusive, counting
     *     from zero)
     *   </li>
     *   <li>
     *     <code>50</code> - process only the records starting after the byte 50
     *   </li>
     *   <li>
     *     <code>50</code> - process only the records within the last 50 bytes of the file.
     *   </li>
     * </ul>
     */
    public ScanRange getScanRange() {
        return scanRange;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
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
        SelectObjectContentInput that = (SelectObjectContentInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.expression, that.expression)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.expressionType, that.expressionType)
               && Objects.equals(this.requestProgress, that.requestProgress)
               && Objects.equals(this.inputSerialization, that.inputSerialization)
               && Objects.equals(this.outputSerialization, that.outputSerialization)
               && Objects.equals(this.scanRange, that.scanRange);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(expression);
        $hc = 31 * $hc + Objects.hashCode(expressionType);
        $hc = 31 * $hc + Objects.hashCode(requestProgress);
        $hc = 31 * $hc + Objects.hashCode(inputSerialization);
        $hc = 31 * $hc + Objects.hashCode(outputSerialization);
        $hc = 31 * $hc + Objects.hashCode(scanRange);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        serializer.writeString($SCHEMA_KEY, key);
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (sseCustomerKey != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEY, sseCustomerKey);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
        }
        serializer.writeString($SCHEMA_EXPRESSION, expression);
        serializer.writeString($SCHEMA_EXPRESSION_TYPE, expressionType.getValue());
        if (requestProgress != null) {
            serializer.writeStruct($SCHEMA_REQUEST_PROGRESS, requestProgress);
        }
        if (inputSerialization != null) {
            serializer.writeStruct($SCHEMA_INPUT_SERIALIZATION, inputSerialization);
        }
        if (outputSerialization != null) {
            serializer.writeStruct($SCHEMA_OUTPUT_SERIALIZATION, outputSerialization);
        }
        if (scanRange != null) {
            serializer.writeStruct($SCHEMA_SCAN_RANGE, scanRange);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, expression);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_TYPE, member, expressionType);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_SERIALIZATION, member, inputSerialization);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_SERIALIZATION, member, outputSerialization);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PROGRESS, member, requestProgress);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCAN_RANGE, member, scanRange);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SelectObjectContentInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.expression(this.expression);
        builder.expressionType(this.expressionType);
        builder.requestProgress(this.requestProgress);
        builder.inputSerialization(this.inputSerialization);
        builder.outputSerialization(this.outputSerialization);
        builder.scanRange(this.scanRange);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SelectObjectContentInput}.
     */
    public static final class Builder implements ShapeBuilder<SelectObjectContentInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;
        private String expression;
        private ExpressionType expressionType;
        private RequestProgress requestProgress;
        private InputSerialization inputSerialization;
        private OutputSerialization outputSerialization;
        private ScanRange scanRange;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The S3 bucket.
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
         * The object key.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * The server-side encryption (SSE) algorithm used to encrypt the object. This parameter is needed only when the
         * object was created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a>
         * in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder sseCustomerAlgorithm(String sseCustomerAlgorithm) {
            this.sseCustomerAlgorithm = sseCustomerAlgorithm;
            return this;
        }

        /**
         * The server-side encryption (SSE) customer managed key. This parameter is needed only when the object was created
         * using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>Amazon S3
         * User Guide</i>.
         *
         * @return this builder.
         */
        public Builder sseCustomerKey(String sseCustomerKey) {
            this.sseCustomerKey = sseCustomerKey;
            return this;
        }

        /**
         * The MD5 server-side encryption (SSE) customer managed key. This parameter is needed only when the object was
         * created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        /**
         * The expression that is used to query the object.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expression(String expression) {
            this.expression = Objects.requireNonNull(expression, "expression cannot be null");
            tracker.setMember($SCHEMA_EXPRESSION);
            return this;
        }

        /**
         * The type of the provided expression (for example, SQL).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expressionType(ExpressionType expressionType) {
            this.expressionType = Objects.requireNonNull(expressionType, "expressionType cannot be null");
            tracker.setMember($SCHEMA_EXPRESSION_TYPE);
            return this;
        }

        /**
         * Specifies if periodic request progress information should be enabled.
         *
         * @return this builder.
         */
        public Builder requestProgress(RequestProgress requestProgress) {
            this.requestProgress = requestProgress;
            return this;
        }

        /**
         * Describes the format of the data in the object that is being queried.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder inputSerialization(InputSerialization inputSerialization) {
            this.inputSerialization = Objects.requireNonNull(inputSerialization, "inputSerialization cannot be null");
            tracker.setMember($SCHEMA_INPUT_SERIALIZATION);
            return this;
        }

        /**
         * Describes the format of the data that you want Amazon S3 to return in response.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder outputSerialization(OutputSerialization outputSerialization) {
            this.outputSerialization = Objects.requireNonNull(outputSerialization, "outputSerialization cannot be null");
            tracker.setMember($SCHEMA_OUTPUT_SERIALIZATION);
            return this;
        }

        /**
         * Specifies the byte range of the object to get the records from. A record is processed when its first byte is
         * contained by the range. This parameter is optional, but when specified, it must not be empty. See RFC 2616,
         * Section 14.35.1 about how to specify the start and end of the range.
         *
         * <p><code>ScanRange</code>may be used in the following ways:
         *
         * <ul>
         *   <li>
         *     <code>50100</code> - process only the records starting between the bytes 50 and 100 (inclusive, counting
         *     from zero)
         *   </li>
         *   <li>
         *     <code>50</code> - process only the records starting after the byte 50
         *   </li>
         *   <li>
         *     <code>50</code> - process only the records within the last 50 bytes of the file.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder scanRange(ScanRange scanRange) {
            this.scanRange = scanRange;
            return this;
        }

        /**
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        @Override
        public SelectObjectContentInput build() {
            tracker.validate();
            return new SelectObjectContentInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> expression((String) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, value));
                case 3 -> expressionType((ExpressionType) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_TYPE, member, value));
                case 4 -> inputSerialization((InputSerialization) SchemaUtils.validateSameMember($SCHEMA_INPUT_SERIALIZATION, member, value));
                case 5 -> outputSerialization((OutputSerialization) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_SERIALIZATION, member, value));
                case 6 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 7 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 8 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 9 -> requestProgress((RequestProgress) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PROGRESS, member, value));
                case 10 -> scanRange((ScanRange) SchemaUtils.validateSameMember($SCHEMA_SCAN_RANGE, member, value));
                case 11 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SelectObjectContentInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_EXPRESSION)) {
                expression("");
            }
            if (!tracker.checkMember($SCHEMA_EXPRESSION_TYPE)) {
                expressionType(ExpressionType.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_INPUT_SERIALIZATION)) {
                tracker.setMember($SCHEMA_INPUT_SERIALIZATION);
            }
            if (!tracker.checkMember($SCHEMA_OUTPUT_SERIALIZATION)) {
                tracker.setMember($SCHEMA_OUTPUT_SERIALIZATION);
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
                    case 1 -> builder.key(de.readString(member));
                    case 2 -> builder.expression(de.readString(member));
                    case 3 -> builder.expressionType(ExpressionType.builder().deserializeMember(de, member).build());
                    case 4 -> builder.inputSerialization(InputSerialization.builder().deserializeMember(de, member).build());
                    case 5 -> builder.outputSerialization(OutputSerialization.builder().deserializeMember(de, member).build());
                    case 6 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 7 -> builder.sseCustomerKey(de.readString(member));
                    case 8 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 9 -> builder.requestProgress(RequestProgress.builder().deserializeMember(de, member).build());
                    case 10 -> builder.scanRange(ScanRange.builder().deserializeMember(de, member).build());
                    case 11 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
