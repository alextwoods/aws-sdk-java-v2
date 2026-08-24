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
 * Describes how an uncompressed comma-separated values (CSV)-formatted input object is formatted.
 */
@SmithyGenerated
public final class CSVInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.CSV_INPUT;
    private static final Schema $SCHEMA_FILE_HEADER_INFO = $SCHEMA.member("FileHeaderInfo");
    private static final Schema $SCHEMA_COMMENTS = $SCHEMA.member("Comments");
    private static final Schema $SCHEMA_QUOTE_ESCAPE_CHARACTER = $SCHEMA.member("QuoteEscapeCharacter");
    private static final Schema $SCHEMA_RECORD_DELIMITER = $SCHEMA.member("RecordDelimiter");
    private static final Schema $SCHEMA_FIELD_DELIMITER = $SCHEMA.member("FieldDelimiter");
    private static final Schema $SCHEMA_QUOTE_CHARACTER = $SCHEMA.member("QuoteCharacter");
    private static final Schema $SCHEMA_ALLOW_QUOTED_RECORD_DELIMITER = $SCHEMA.member("AllowQuotedRecordDelimiter");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient FileHeaderInfo fileHeaderInfo;
    private final transient String comments;
    private final transient String quoteEscapeCharacter;
    private final transient String recordDelimiter;
    private final transient String fieldDelimiter;
    private final transient String quoteCharacter;
    private final transient Boolean allowQuotedRecordDelimiter;

    private CSVInput(Builder builder) {
        this.fileHeaderInfo = builder.fileHeaderInfo;
        this.comments = builder.comments;
        this.quoteEscapeCharacter = builder.quoteEscapeCharacter;
        this.recordDelimiter = builder.recordDelimiter;
        this.fieldDelimiter = builder.fieldDelimiter;
        this.quoteCharacter = builder.quoteCharacter;
        this.allowQuotedRecordDelimiter = builder.allowQuotedRecordDelimiter;
    }

    /**
     * Describes the first line of input. Valid values are:
     *
     * <ul>
     *   <li>
     *     <code>NONE</code>: First line is not a header.
     *   </li>
     *   <li>
     *     <code>IGNORE</code>: First line is a header, but you can't use the header values to indicate the column
     *     in an expression. You can use column position (such as _1, _2, …) to indicate the column (<code>SELECT
     *     s._1 FROM OBJECT s</code>).
     *   </li>
     *   <li>
     *     <code>Use</code>: First line is a header, and you can use the header value to identify a column in an
     *     expression (<code>SELECT "name" FROM OBJECT</code>).
     *   </li>
     * </ul>
     */
    public FileHeaderInfo getFileHeaderInfo() {
        return fileHeaderInfo;
    }

    /**
     * A single character used to indicate that a row should be ignored when the character is present at the start of
     * that row. You can specify any character to indicate a comment line. The default character is <code>#</code>.
     *
     * <p>Default: <code>#</code>
     */
    public String getComments() {
        return comments;
    }

    /**
     * A single character used for escaping the quotation mark character inside an already escaped value. For example,
     * the value <code>""" a , b """</code> is parsed as <code>" a , b "</code>.
     */
    public String getQuoteEscapeCharacter() {
        return quoteEscapeCharacter;
    }

    /**
     * A single character used to separate individual records in the input. Instead of the default value, you can
     * specify an arbitrary delimiter.
     */
    public String getRecordDelimiter() {
        return recordDelimiter;
    }

    /**
     * A single character used to separate individual fields in a record. You can specify an arbitrary delimiter.
     */
    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    /**
     * A single character used for escaping when the field delimiter is part of the value. For example, if the value is <code>
     * a, b</code>, Amazon S3 wraps this field value in quotation marks, as follows: <code>" a , b "</code>.
     *
     * <p>Type: String
     *
     * <p>Default: <code>"</code>
     *
     * <p>Ancestors: <code>CSV</code>
     */
    public String getQuoteCharacter() {
        return quoteCharacter;
    }

    /**
     * Specifies that CSV field values may contain quoted record delimiters and such records should be allowed. Default
     * value is FALSE. Setting this value to TRUE may lower performance.
     */
    public Boolean isAllowQuotedRecordDelimiter() {
        return allowQuotedRecordDelimiter;
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
        CSVInput that = (CSVInput) other;
        return Objects.equals(this.allowQuotedRecordDelimiter, that.allowQuotedRecordDelimiter)
               && Objects.equals(this.comments, that.comments)
               && Objects.equals(this.quoteEscapeCharacter, that.quoteEscapeCharacter)
               && Objects.equals(this.recordDelimiter, that.recordDelimiter)
               && Objects.equals(this.fieldDelimiter, that.fieldDelimiter)
               && Objects.equals(this.quoteCharacter, that.quoteCharacter)
               && Objects.equals(this.fileHeaderInfo, that.fileHeaderInfo);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(fileHeaderInfo);
        $hc = 31 * $hc + Objects.hashCode(comments);
        $hc = 31 * $hc + Objects.hashCode(quoteEscapeCharacter);
        $hc = 31 * $hc + Objects.hashCode(recordDelimiter);
        $hc = 31 * $hc + Objects.hashCode(fieldDelimiter);
        $hc = 31 * $hc + Objects.hashCode(quoteCharacter);
        $hc = 31 * $hc + Objects.hashCode(allowQuotedRecordDelimiter);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (fileHeaderInfo != null) {
            serializer.writeString($SCHEMA_FILE_HEADER_INFO, fileHeaderInfo.getValue());
        }
        if (comments != null) {
            serializer.writeString($SCHEMA_COMMENTS, comments);
        }
        if (quoteEscapeCharacter != null) {
            serializer.writeString($SCHEMA_QUOTE_ESCAPE_CHARACTER, quoteEscapeCharacter);
        }
        if (recordDelimiter != null) {
            serializer.writeString($SCHEMA_RECORD_DELIMITER, recordDelimiter);
        }
        if (fieldDelimiter != null) {
            serializer.writeString($SCHEMA_FIELD_DELIMITER, fieldDelimiter);
        }
        if (quoteCharacter != null) {
            serializer.writeString($SCHEMA_QUOTE_CHARACTER, quoteCharacter);
        }
        if (allowQuotedRecordDelimiter != null) {
            serializer.writeBoolean($SCHEMA_ALLOW_QUOTED_RECORD_DELIMITER, allowQuotedRecordDelimiter);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILE_HEADER_INFO, member, fileHeaderInfo);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMMENTS, member, comments);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUOTE_ESCAPE_CHARACTER, member, quoteEscapeCharacter);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECORD_DELIMITER, member, recordDelimiter);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_FIELD_DELIMITER, member, fieldDelimiter);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUOTE_CHARACTER, member, quoteCharacter);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALLOW_QUOTED_RECORD_DELIMITER, member, allowQuotedRecordDelimiter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CSVInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.fileHeaderInfo(this.fileHeaderInfo);
        builder.comments(this.comments);
        builder.quoteEscapeCharacter(this.quoteEscapeCharacter);
        builder.recordDelimiter(this.recordDelimiter);
        builder.fieldDelimiter(this.fieldDelimiter);
        builder.quoteCharacter(this.quoteCharacter);
        builder.allowQuotedRecordDelimiter(this.allowQuotedRecordDelimiter);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CSVInput}.
     */
    public static final class Builder implements ShapeBuilder<CSVInput> {
        private FileHeaderInfo fileHeaderInfo;
        private String comments;
        private String quoteEscapeCharacter;
        private String recordDelimiter;
        private String fieldDelimiter;
        private String quoteCharacter;
        private Boolean allowQuotedRecordDelimiter;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Describes the first line of input. Valid values are:
         *
         * <ul>
         *   <li>
         *     <code>NONE</code>: First line is not a header.
         *   </li>
         *   <li>
         *     <code>IGNORE</code>: First line is a header, but you can't use the header values to indicate the column
         *     in an expression. You can use column position (such as _1, _2, …) to indicate the column (<code>SELECT
         *     s._1 FROM OBJECT s</code>).
         *   </li>
         *   <li>
         *     <code>Use</code>: First line is a header, and you can use the header value to identify a column in an
         *     expression (<code>SELECT "name" FROM OBJECT</code>).
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder fileHeaderInfo(FileHeaderInfo fileHeaderInfo) {
            this.fileHeaderInfo = fileHeaderInfo;
            return this;
        }

        /**
         * A single character used to indicate that a row should be ignored when the character is present at the start of
         * that row. You can specify any character to indicate a comment line. The default character is <code>#</code>.
         *
         * <p>Default: <code>#</code>
         *
         * @return this builder.
         */
        public Builder comments(String comments) {
            this.comments = comments;
            return this;
        }

        /**
         * A single character used for escaping the quotation mark character inside an already escaped value. For example,
         * the value <code>""" a , b """</code> is parsed as <code>" a , b "</code>.
         *
         * @return this builder.
         */
        public Builder quoteEscapeCharacter(String quoteEscapeCharacter) {
            this.quoteEscapeCharacter = quoteEscapeCharacter;
            return this;
        }

        /**
         * A single character used to separate individual records in the input. Instead of the default value, you can
         * specify an arbitrary delimiter.
         *
         * @return this builder.
         */
        public Builder recordDelimiter(String recordDelimiter) {
            this.recordDelimiter = recordDelimiter;
            return this;
        }

        /**
         * A single character used to separate individual fields in a record. You can specify an arbitrary delimiter.
         *
         * @return this builder.
         */
        public Builder fieldDelimiter(String fieldDelimiter) {
            this.fieldDelimiter = fieldDelimiter;
            return this;
        }

        /**
         * A single character used for escaping when the field delimiter is part of the value. For example, if the value is <code>
         * a, b</code>, Amazon S3 wraps this field value in quotation marks, as follows: <code>" a , b "</code>.
         *
         * <p>Type: String
         *
         * <p>Default: <code>"</code>
         *
         * <p>Ancestors: <code>CSV</code>
         *
         * @return this builder.
         */
        public Builder quoteCharacter(String quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Specifies that CSV field values may contain quoted record delimiters and such records should be allowed. Default
         * value is FALSE. Setting this value to TRUE may lower performance.
         *
         * @return this builder.
         */
        public Builder allowQuotedRecordDelimiter(Boolean allowQuotedRecordDelimiter) {
            this.allowQuotedRecordDelimiter = allowQuotedRecordDelimiter;
            return this;
        }

        @Override
        public CSVInput build() {
            return new CSVInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> fileHeaderInfo((FileHeaderInfo) SchemaUtils.validateSameMember($SCHEMA_FILE_HEADER_INFO, member, value));
                case 1 -> comments((String) SchemaUtils.validateSameMember($SCHEMA_COMMENTS, member, value));
                case 2 -> quoteEscapeCharacter((String) SchemaUtils.validateSameMember($SCHEMA_QUOTE_ESCAPE_CHARACTER, member, value));
                case 3 -> recordDelimiter((String) SchemaUtils.validateSameMember($SCHEMA_RECORD_DELIMITER, member, value));
                case 4 -> fieldDelimiter((String) SchemaUtils.validateSameMember($SCHEMA_FIELD_DELIMITER, member, value));
                case 5 -> quoteCharacter((String) SchemaUtils.validateSameMember($SCHEMA_QUOTE_CHARACTER, member, value));
                case 6 -> allowQuotedRecordDelimiter((Boolean) SchemaUtils.validateSameMember($SCHEMA_ALLOW_QUOTED_RECORD_DELIMITER, member, value));
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
                    case 0 -> builder.fileHeaderInfo(FileHeaderInfo.builder().deserializeMember(de, member).build());
                    case 1 -> builder.comments(de.readString(member));
                    case 2 -> builder.quoteEscapeCharacter(de.readString(member));
                    case 3 -> builder.recordDelimiter(de.readString(member));
                    case 4 -> builder.fieldDelimiter(de.readString(member));
                    case 5 -> builder.quoteCharacter(de.readString(member));
                    case 6 -> builder.allowQuotedRecordDelimiter(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
