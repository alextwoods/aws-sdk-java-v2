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
 * Describes how uncompressed comma-separated values (CSV)-formatted results are formatted.
 */
@SmithyGenerated
public final class CSVOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.CSV_OUTPUT;
    private static final Schema $SCHEMA_QUOTE_FIELDS = $SCHEMA.member("QuoteFields");
    private static final Schema $SCHEMA_QUOTE_ESCAPE_CHARACTER = $SCHEMA.member("QuoteEscapeCharacter");
    private static final Schema $SCHEMA_RECORD_DELIMITER = $SCHEMA.member("RecordDelimiter");
    private static final Schema $SCHEMA_FIELD_DELIMITER = $SCHEMA.member("FieldDelimiter");
    private static final Schema $SCHEMA_QUOTE_CHARACTER = $SCHEMA.member("QuoteCharacter");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient QuoteFields quoteFields;
    private final transient String quoteEscapeCharacter;
    private final transient String recordDelimiter;
    private final transient String fieldDelimiter;
    private final transient String quoteCharacter;

    private CSVOutput(Builder builder) {
        this.quoteFields = builder.quoteFields;
        this.quoteEscapeCharacter = builder.quoteEscapeCharacter;
        this.recordDelimiter = builder.recordDelimiter;
        this.fieldDelimiter = builder.fieldDelimiter;
        this.quoteCharacter = builder.quoteCharacter;
    }

    /**
     * Indicates whether to use quotation marks around output fields.
     *
     * <ul>
     *   <li>
     *     <code>ALWAYS</code>: Always use quotation marks for output fields.
     *   </li>
     *   <li>
     *     <code>ASNEEDED</code>: Use quotation marks for output fields when needed.
     *   </li>
     * </ul>
     */
    public QuoteFields getQuoteFields() {
        return quoteFields;
    }

    /**
     * The single character used for escaping the quote character inside an already escaped value.
     */
    public String getQuoteEscapeCharacter() {
        return quoteEscapeCharacter;
    }

    /**
     * A single character used to separate individual records in the output. Instead of the default value, you can
     * specify an arbitrary delimiter.
     */
    public String getRecordDelimiter() {
        return recordDelimiter;
    }

    /**
     * The value used to separate individual fields in a record. You can specify an arbitrary delimiter.
     */
    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    /**
     * A single character used for escaping when the field delimiter is part of the value. For example, if the value is
     * <code>a, b</code>, Amazon S3 wraps this field value in quotation marks, as follows: <code>" a , b "</code>.
     */
    public String getQuoteCharacter() {
        return quoteCharacter;
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
        CSVOutput that = (CSVOutput) other;
        return Objects.equals(this.quoteEscapeCharacter, that.quoteEscapeCharacter)
               && Objects.equals(this.recordDelimiter, that.recordDelimiter)
               && Objects.equals(this.fieldDelimiter, that.fieldDelimiter)
               && Objects.equals(this.quoteCharacter, that.quoteCharacter)
               && Objects.equals(this.quoteFields, that.quoteFields);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(quoteFields);
        $hc = 31 * $hc + Objects.hashCode(quoteEscapeCharacter);
        $hc = 31 * $hc + Objects.hashCode(recordDelimiter);
        $hc = 31 * $hc + Objects.hashCode(fieldDelimiter);
        $hc = 31 * $hc + Objects.hashCode(quoteCharacter);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (quoteFields != null) {
            serializer.writeString($SCHEMA_QUOTE_FIELDS, quoteFields.getValue());
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUOTE_FIELDS, member, quoteFields);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUOTE_ESCAPE_CHARACTER, member, quoteEscapeCharacter);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECORD_DELIMITER, member, recordDelimiter);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_FIELD_DELIMITER, member, fieldDelimiter);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUOTE_CHARACTER, member, quoteCharacter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CSVOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.quoteFields(this.quoteFields);
        builder.quoteEscapeCharacter(this.quoteEscapeCharacter);
        builder.recordDelimiter(this.recordDelimiter);
        builder.fieldDelimiter(this.fieldDelimiter);
        builder.quoteCharacter(this.quoteCharacter);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CSVOutput}.
     */
    public static final class Builder implements ShapeBuilder<CSVOutput> {
        private QuoteFields quoteFields;
        private String quoteEscapeCharacter;
        private String recordDelimiter;
        private String fieldDelimiter;
        private String quoteCharacter;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether to use quotation marks around output fields.
         *
         * <ul>
         *   <li>
         *     <code>ALWAYS</code>: Always use quotation marks for output fields.
         *   </li>
         *   <li>
         *     <code>ASNEEDED</code>: Use quotation marks for output fields when needed.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder quoteFields(QuoteFields quoteFields) {
            this.quoteFields = quoteFields;
            return this;
        }

        /**
         * The single character used for escaping the quote character inside an already escaped value.
         *
         * @return this builder.
         */
        public Builder quoteEscapeCharacter(String quoteEscapeCharacter) {
            this.quoteEscapeCharacter = quoteEscapeCharacter;
            return this;
        }

        /**
         * A single character used to separate individual records in the output. Instead of the default value, you can
         * specify an arbitrary delimiter.
         *
         * @return this builder.
         */
        public Builder recordDelimiter(String recordDelimiter) {
            this.recordDelimiter = recordDelimiter;
            return this;
        }

        /**
         * The value used to separate individual fields in a record. You can specify an arbitrary delimiter.
         *
         * @return this builder.
         */
        public Builder fieldDelimiter(String fieldDelimiter) {
            this.fieldDelimiter = fieldDelimiter;
            return this;
        }

        /**
         * A single character used for escaping when the field delimiter is part of the value. For example, if the value is
         * <code>a, b</code>, Amazon S3 wraps this field value in quotation marks, as follows: <code>" a , b "</code>.
         *
         * @return this builder.
         */
        public Builder quoteCharacter(String quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        @Override
        public CSVOutput build() {
            return new CSVOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> quoteFields((QuoteFields) SchemaUtils.validateSameMember($SCHEMA_QUOTE_FIELDS, member, value));
                case 1 -> quoteEscapeCharacter((String) SchemaUtils.validateSameMember($SCHEMA_QUOTE_ESCAPE_CHARACTER, member, value));
                case 2 -> recordDelimiter((String) SchemaUtils.validateSameMember($SCHEMA_RECORD_DELIMITER, member, value));
                case 3 -> fieldDelimiter((String) SchemaUtils.validateSameMember($SCHEMA_FIELD_DELIMITER, member, value));
                case 4 -> quoteCharacter((String) SchemaUtils.validateSameMember($SCHEMA_QUOTE_CHARACTER, member, value));
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
                    case 0 -> builder.quoteFields(QuoteFields.builder().deserializeMember(de, member).build());
                    case 1 -> builder.quoteEscapeCharacter(de.readString(member));
                    case 2 -> builder.recordDelimiter(de.readString(member));
                    case 3 -> builder.fieldDelimiter(de.readString(member));
                    case 4 -> builder.quoteCharacter(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
