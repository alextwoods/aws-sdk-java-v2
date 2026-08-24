package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

@SmithyGenerated
public final class DescribeExportOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_EXPORT_OUTPUT;
    private static final Schema $SCHEMA_EXPORT_DESCRIPTION = $SCHEMA.member("ExportDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ExportDescription exportDescription;

    private DescribeExportOutput(Builder builder) {
        this.exportDescription = builder.exportDescription;
    }

    /**
     * Represents the properties of the export.
     */
    public ExportDescription getExportDescription() {
        return exportDescription;
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
        DescribeExportOutput that = (DescribeExportOutput) other;
        return Objects.equals(this.exportDescription, that.exportDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exportDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exportDescription != null) {
            serializer.writeStruct($SCHEMA_EXPORT_DESCRIPTION, exportDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_DESCRIPTION, member, exportDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeExportOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exportDescription(this.exportDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeExportOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeExportOutput> {
        private ExportDescription exportDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the properties of the export.
         *
         * @return this builder.
         */
        public Builder exportDescription(ExportDescription exportDescription) {
            this.exportDescription = exportDescription;
            return this;
        }

        @Override
        public DescribeExportOutput build() {
            return new DescribeExportOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exportDescription((ExportDescription) SchemaUtils.validateSameMember($SCHEMA_EXPORT_DESCRIPTION, member, value));
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
                    case 0 -> builder.exportDescription(ExportDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
