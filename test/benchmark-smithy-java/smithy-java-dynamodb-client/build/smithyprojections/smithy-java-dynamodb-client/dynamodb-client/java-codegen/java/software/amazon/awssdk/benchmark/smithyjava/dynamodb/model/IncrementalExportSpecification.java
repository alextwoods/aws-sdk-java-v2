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
 * Optional object containing the parameters specific to an incremental export.
 */
@SmithyGenerated
public final class IncrementalExportSpecification implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.INCREMENTAL_EXPORT_SPECIFICATION;
    private static final Schema $SCHEMA_EXPORT_FROM_TIME = $SCHEMA.member("ExportFromTime");
    private static final Schema $SCHEMA_EXPORT_TO_TIME = $SCHEMA.member("ExportToTime");
    private static final Schema $SCHEMA_EXPORT_VIEW_TYPE = $SCHEMA.member("ExportViewType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant exportFromTime;
    private final transient Instant exportToTime;
    private final transient ExportViewType exportViewType;

    private IncrementalExportSpecification(Builder builder) {
        this.exportFromTime = builder.exportFromTime;
        this.exportToTime = builder.exportToTime;
        this.exportViewType = builder.exportViewType;
    }

    /**
     * Time in the past which provides the inclusive start range for the export table's data, counted in seconds from
     * the start of the Unix epoch. The incremental export will reflect the table's state including and after this point
     * in time.
     */
    public Instant getExportFromTime() {
        return exportFromTime;
    }

    /**
     * Time in the past which provides the exclusive end range for the export table's data, counted in seconds from the
     * start of the Unix epoch. The incremental export will reflect the table's state just prior to this point in time.
     * If this is not provided, the latest time with data available will be used.
     */
    public Instant getExportToTime() {
        return exportToTime;
    }

    /**
     * The view type that was chosen for the export. Valid values are <code>NEW_AND_OLD_IMAGES</code> and <code>
     * NEW_IMAGES</code>. The default value is <code>NEW_AND_OLD_IMAGES</code>.
     *
     * <p><code>NEW_AND_OLD_IMAGES</code> exports both the new and old images of each changed item, while <code>
     * NEW_IMAGES</code> exports only the new (latest) image. The view type you choose determines the structure of each
     * item in the output for <code>insert</code>, <code>update</code>, and <code>delete</code> operations. For details
     * and examples of how each view type shapes the export output, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/S3DataExport.Output.html">DynamoDB table export output format</a> in
     * the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ExportViewType getExportViewType() {
        return exportViewType;
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
        IncrementalExportSpecification that = (IncrementalExportSpecification) other;
        return Objects.equals(this.exportViewType, that.exportViewType)
               && Objects.equals(this.exportFromTime, that.exportFromTime)
               && Objects.equals(this.exportToTime, that.exportToTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exportFromTime);
        $hc = 31 * $hc + Objects.hashCode(exportToTime);
        $hc = 31 * $hc + Objects.hashCode(exportViewType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (exportFromTime != null) {
            serializer.writeTimestamp($SCHEMA_EXPORT_FROM_TIME, exportFromTime);
        }
        if (exportToTime != null) {
            serializer.writeTimestamp($SCHEMA_EXPORT_TO_TIME, exportToTime);
        }
        if (exportViewType != null) {
            serializer.writeString($SCHEMA_EXPORT_VIEW_TYPE, exportViewType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_FROM_TIME, member, exportFromTime);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TO_TIME, member, exportToTime);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_VIEW_TYPE, member, exportViewType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link IncrementalExportSpecification}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exportFromTime(this.exportFromTime);
        builder.exportToTime(this.exportToTime);
        builder.exportViewType(this.exportViewType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link IncrementalExportSpecification}.
     */
    public static final class Builder implements ShapeBuilder<IncrementalExportSpecification> {
        private Instant exportFromTime;
        private Instant exportToTime;
        private ExportViewType exportViewType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Time in the past which provides the inclusive start range for the export table's data, counted in seconds from
         * the start of the Unix epoch. The incremental export will reflect the table's state including and after this point
         * in time.
         *
         * @return this builder.
         */
        public Builder exportFromTime(Instant exportFromTime) {
            this.exportFromTime = exportFromTime;
            return this;
        }

        /**
         * Time in the past which provides the exclusive end range for the export table's data, counted in seconds from the
         * start of the Unix epoch. The incremental export will reflect the table's state just prior to this point in time.
         * If this is not provided, the latest time with data available will be used.
         *
         * @return this builder.
         */
        public Builder exportToTime(Instant exportToTime) {
            this.exportToTime = exportToTime;
            return this;
        }

        /**
         * The view type that was chosen for the export. Valid values are <code>NEW_AND_OLD_IMAGES</code> and <code>
         * NEW_IMAGES</code>. The default value is <code>NEW_AND_OLD_IMAGES</code>.
         *
         * <p><code>NEW_AND_OLD_IMAGES</code> exports both the new and old images of each changed item, while <code>
         * NEW_IMAGES</code> exports only the new (latest) image. The view type you choose determines the structure of each
         * item in the output for <code>insert</code>, <code>update</code>, and <code>delete</code> operations. For details
         * and examples of how each view type shapes the export output, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/S3DataExport.Output.html">DynamoDB table export output format</a> in
         * the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder exportViewType(ExportViewType exportViewType) {
            this.exportViewType = exportViewType;
            return this;
        }

        @Override
        public IncrementalExportSpecification build() {
            return new IncrementalExportSpecification(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exportFromTime((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPORT_FROM_TIME, member, value));
                case 1 -> exportToTime((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPORT_TO_TIME, member, value));
                case 2 -> exportViewType((ExportViewType) SchemaUtils.validateSameMember($SCHEMA_EXPORT_VIEW_TYPE, member, value));
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
                    case 0 -> builder.exportFromTime(de.readTimestamp(member));
                    case 1 -> builder.exportToTime(de.readTimestamp(member));
                    case 2 -> builder.exportViewType(ExportViewType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
