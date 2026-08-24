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
 * Describes the serialization format of the object.
 */
@SmithyGenerated
public final class InputSerialization implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.INPUT_SERIALIZATION;
    private static final Schema $SCHEMA_CSV = $SCHEMA.member("CSV");
    private static final Schema $SCHEMA_COMPRESSION_TYPE = $SCHEMA.member("CompressionType");
    private static final Schema $SCHEMA_JSON = $SCHEMA.member("JSON");
    private static final Schema $SCHEMA_PARQUET = $SCHEMA.member("Parquet");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient CSVInput csv;
    private final transient CompressionType compressionType;
    private final transient JSONInput json;
    private final transient ParquetInput parquet;

    private InputSerialization(Builder builder) {
        this.csv = builder.csv;
        this.compressionType = builder.compressionType;
        this.json = builder.json;
        this.parquet = builder.parquet;
    }

    /**
     * Describes the serialization of a CSV-encoded object.
     */
    public CSVInput getCsv() {
        return csv;
    }

    /**
     * Specifies object's compression format. Valid values: NONE, GZIP, BZIP2. Default Value: NONE.
     */
    public CompressionType getCompressionType() {
        return compressionType;
    }

    /**
     * Specifies JSON as object's input serialization format.
     */
    public JSONInput getJson() {
        return json;
    }

    /**
     * Specifies Parquet as object's input serialization format.
     */
    public ParquetInput getParquet() {
        return parquet;
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
        InputSerialization that = (InputSerialization) other;
        return Objects.equals(this.compressionType, that.compressionType)
               && Objects.equals(this.csv, that.csv)
               && Objects.equals(this.json, that.json)
               && Objects.equals(this.parquet, that.parquet);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(csv);
        $hc = 31 * $hc + Objects.hashCode(compressionType);
        $hc = 31 * $hc + Objects.hashCode(json);
        $hc = 31 * $hc + Objects.hashCode(parquet);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (csv != null) {
            serializer.writeStruct($SCHEMA_CSV, csv);
        }
        if (compressionType != null) {
            serializer.writeString($SCHEMA_COMPRESSION_TYPE, compressionType.getValue());
        }
        if (json != null) {
            serializer.writeStruct($SCHEMA_JSON, json);
        }
        if (parquet != null) {
            serializer.writeStruct($SCHEMA_PARQUET, parquet);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CSV, member, csv);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPRESSION_TYPE, member, compressionType);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_JSON, member, json);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARQUET, member, parquet);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InputSerialization}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.csv(this.csv);
        builder.compressionType(this.compressionType);
        builder.json(this.json);
        builder.parquet(this.parquet);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InputSerialization}.
     */
    public static final class Builder implements ShapeBuilder<InputSerialization> {
        private CSVInput csv;
        private CompressionType compressionType;
        private JSONInput json;
        private ParquetInput parquet;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Describes the serialization of a CSV-encoded object.
         *
         * @return this builder.
         */
        public Builder csv(CSVInput csv) {
            this.csv = csv;
            return this;
        }

        /**
         * Specifies object's compression format. Valid values: NONE, GZIP, BZIP2. Default Value: NONE.
         *
         * @return this builder.
         */
        public Builder compressionType(CompressionType compressionType) {
            this.compressionType = compressionType;
            return this;
        }

        /**
         * Specifies JSON as object's input serialization format.
         *
         * @return this builder.
         */
        public Builder json(JSONInput json) {
            this.json = json;
            return this;
        }

        /**
         * Specifies Parquet as object's input serialization format.
         *
         * @return this builder.
         */
        public Builder parquet(ParquetInput parquet) {
            this.parquet = parquet;
            return this;
        }

        @Override
        public InputSerialization build() {
            return new InputSerialization(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> csv((CSVInput) SchemaUtils.validateSameMember($SCHEMA_CSV, member, value));
                case 1 -> compressionType((CompressionType) SchemaUtils.validateSameMember($SCHEMA_COMPRESSION_TYPE, member, value));
                case 2 -> json((JSONInput) SchemaUtils.validateSameMember($SCHEMA_JSON, member, value));
                case 3 -> parquet((ParquetInput) SchemaUtils.validateSameMember($SCHEMA_PARQUET, member, value));
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
                    case 0 -> builder.csv(CSVInput.builder().deserializeMember(de, member).build());
                    case 1 -> builder.compressionType(CompressionType.builder().deserializeMember(de, member).build());
                    case 2 -> builder.json(JSONInput.builder().deserializeMember(de, member).build());
                    case 3 -> builder.parquet(ParquetInput.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
