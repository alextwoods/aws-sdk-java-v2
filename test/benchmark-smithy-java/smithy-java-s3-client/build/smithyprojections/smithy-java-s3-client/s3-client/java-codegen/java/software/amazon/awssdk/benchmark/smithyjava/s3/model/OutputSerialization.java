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
 * Describes how results of the Select job are serialized.
 */
@SmithyGenerated
public final class OutputSerialization implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.OUTPUT_SERIALIZATION;
    private static final Schema $SCHEMA_CSV = $SCHEMA.member("CSV");
    private static final Schema $SCHEMA_JSON = $SCHEMA.member("JSON");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient CSVOutput csv;
    private final transient JSONOutput json;

    private OutputSerialization(Builder builder) {
        this.csv = builder.csv;
        this.json = builder.json;
    }

    /**
     * Describes the serialization of CSV-encoded Select results.
     */
    public CSVOutput getCsv() {
        return csv;
    }

    /**
     * Specifies JSON as request's output serialization format.
     */
    public JSONOutput getJson() {
        return json;
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
        OutputSerialization that = (OutputSerialization) other;
        return Objects.equals(this.csv, that.csv)
               && Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(csv);
        $hc = 31 * $hc + Objects.hashCode(json);
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
        if (json != null) {
            serializer.writeStruct($SCHEMA_JSON, json);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CSV, member, csv);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_JSON, member, json);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link OutputSerialization}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.csv(this.csv);
        builder.json(this.json);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OutputSerialization}.
     */
    public static final class Builder implements ShapeBuilder<OutputSerialization> {
        private CSVOutput csv;
        private JSONOutput json;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Describes the serialization of CSV-encoded Select results.
         *
         * @return this builder.
         */
        public Builder csv(CSVOutput csv) {
            this.csv = csv;
            return this;
        }

        /**
         * Specifies JSON as request's output serialization format.
         *
         * @return this builder.
         */
        public Builder json(JSONOutput json) {
            this.json = json;
            return this;
        }

        @Override
        public OutputSerialization build() {
            return new OutputSerialization(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> csv((CSVOutput) SchemaUtils.validateSameMember($SCHEMA_CSV, member, value));
                case 1 -> json((JSONOutput) SchemaUtils.validateSameMember($SCHEMA_JSON, member, value));
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
                    case 0 -> builder.csv(CSVOutput.builder().deserializeMember(de, member).build());
                    case 1 -> builder.json(JSONOutput.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
