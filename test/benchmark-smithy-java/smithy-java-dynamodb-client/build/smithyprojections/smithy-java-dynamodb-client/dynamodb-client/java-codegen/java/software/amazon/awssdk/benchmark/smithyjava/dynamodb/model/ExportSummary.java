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

/**
 * Summary information about an export task.
 */
@SmithyGenerated
public final class ExportSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXPORT_SUMMARY;
    private static final Schema $SCHEMA_EXPORT_ARN = $SCHEMA.member("ExportArn");
    private static final Schema $SCHEMA_EXPORT_STATUS = $SCHEMA.member("ExportStatus");
    private static final Schema $SCHEMA_EXPORT_TYPE = $SCHEMA.member("ExportType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String exportArn;
    private final transient ExportStatus exportStatus;
    private final transient ExportType exportType;

    private ExportSummary(Builder builder) {
        this.exportArn = builder.exportArn;
        this.exportStatus = builder.exportStatus;
        this.exportType = builder.exportType;
    }

    /**
     * The Amazon Resource Name (ARN) of the export.
     */
    public String getExportArn() {
        return exportArn;
    }

    /**
     * Export can be in one of the following states: IN_PROGRESS, COMPLETED, or FAILED.
     */
    public ExportStatus getExportStatus() {
        return exportStatus;
    }

    /**
     * The type of export that was performed. Valid values are <code>FULL_EXPORT</code> or
     * <code>INCREMENTAL_EXPORT</code>.
     */
    public ExportType getExportType() {
        return exportType;
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
        ExportSummary that = (ExportSummary) other;
        return Objects.equals(this.exportArn, that.exportArn)
               && Objects.equals(this.exportStatus, that.exportStatus)
               && Objects.equals(this.exportType, that.exportType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exportArn);
        $hc = 31 * $hc + Objects.hashCode(exportStatus);
        $hc = 31 * $hc + Objects.hashCode(exportType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exportArn != null) {
            serializer.writeString($SCHEMA_EXPORT_ARN, exportArn);
        }
        if (exportStatus != null) {
            serializer.writeString($SCHEMA_EXPORT_STATUS, exportStatus.getValue());
        }
        if (exportType != null) {
            serializer.writeString($SCHEMA_EXPORT_TYPE, exportType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_ARN, member, exportArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_STATUS, member, exportStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TYPE, member, exportType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExportSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exportArn(this.exportArn);
        builder.exportStatus(this.exportStatus);
        builder.exportType(this.exportType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ExportSummary}.
     */
    public static final class Builder implements ShapeBuilder<ExportSummary> {
        private String exportArn;
        private ExportStatus exportStatus;
        private ExportType exportType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the export.
         *
         * @return this builder.
         */
        public Builder exportArn(String exportArn) {
            this.exportArn = exportArn;
            return this;
        }

        /**
         * Export can be in one of the following states: IN_PROGRESS, COMPLETED, or FAILED.
         *
         * @return this builder.
         */
        public Builder exportStatus(ExportStatus exportStatus) {
            this.exportStatus = exportStatus;
            return this;
        }

        /**
         * The type of export that was performed. Valid values are <code>FULL_EXPORT</code> or
         * <code>INCREMENTAL_EXPORT</code>.
         *
         * @return this builder.
         */
        public Builder exportType(ExportType exportType) {
            this.exportType = exportType;
            return this;
        }

        @Override
        public ExportSummary build() {
            return new ExportSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exportArn((String) SchemaUtils.validateSameMember($SCHEMA_EXPORT_ARN, member, value));
                case 1 -> exportStatus((ExportStatus) SchemaUtils.validateSameMember($SCHEMA_EXPORT_STATUS, member, value));
                case 2 -> exportType((ExportType) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TYPE, member, value));
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
                    case 0 -> builder.exportArn(de.readString(member));
                    case 1 -> builder.exportStatus(ExportStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.exportType(ExportType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
