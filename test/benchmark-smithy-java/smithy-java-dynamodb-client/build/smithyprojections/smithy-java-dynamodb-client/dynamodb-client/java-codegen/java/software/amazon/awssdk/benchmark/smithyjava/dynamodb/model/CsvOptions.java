package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

/**
 * Processing options for the CSV file being imported.
 */
@SmithyGenerated
public final class CsvOptions implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CSV_OPTIONS;
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_HEADER_LIST = $SCHEMA.member("HeaderList");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String delimiter;
    private final transient List<String> headerList;

    private CsvOptions(Builder builder) {
        this.delimiter = builder.delimiter;
        this.headerList = builder.headerList == null ? null : Collections.unmodifiableList(builder.headerList);
    }

    /**
     * The delimiter used for separating items in the CSV file being imported.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * List of the headers used to specify a common header for all source CSV files being imported. If this field is
     * specified then the first line of each CSV file is treated as data instead of the header. If this field is not
     * specified the the first line of each CSV file is treated as the header.
     */
    public List<String> getHeaderList() {
        if (headerList == null) {
            return Collections.emptyList();
        }
        return headerList;
    }

    public boolean hasHeaderList() {
        return headerList != null;
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
        CsvOptions that = (CsvOptions) other;
        return Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.headerList, that.headerList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(delimiter);
        $hc = 31 * $hc + Objects.hashCode(headerList);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (delimiter != null) {
            serializer.writeString($SCHEMA_DELIMITER, delimiter);
        }
        if (headerList != null) {
            serializer.writeList($SCHEMA_HEADER_LIST, headerList, headerList.size(), SharedSerde.CsvHeaderListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_HEADER_LIST, member, headerList);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CsvOptions}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.delimiter(this.delimiter);
        builder.headerList(this.headerList);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CsvOptions}.
     */
    public static final class Builder implements ShapeBuilder<CsvOptions> {
        private String delimiter;
        private List<String> headerList;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The delimiter used for separating items in the CSV file being imported.
         *
         * @return this builder.
         */
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * List of the headers used to specify a common header for all source CSV files being imported. If this field is
         * specified then the first line of each CSV file is treated as data instead of the header. If this field is not
         * specified the the first line of each CSV file is treated as the header.
         *
         * @return this builder.
         */
        public Builder headerList(List<String> headerList) {
            this.headerList = headerList;
            return this;
        }

        @Override
        public CsvOptions build() {
            return new CsvOptions(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 1 -> headerList((List<String>) SchemaUtils.validateSameMember($SCHEMA_HEADER_LIST, member, value));
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
                    case 0 -> builder.delimiter(de.readString(member));
                    case 1 -> builder.headerList(SharedSerde.deserializeCsvHeaderList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
