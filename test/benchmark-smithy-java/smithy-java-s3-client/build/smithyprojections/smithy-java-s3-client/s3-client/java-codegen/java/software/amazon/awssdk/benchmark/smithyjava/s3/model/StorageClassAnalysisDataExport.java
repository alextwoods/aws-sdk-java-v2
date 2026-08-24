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
 * Container for data related to the storage class analysis for an Amazon S3 bucket for export.
 */
@SmithyGenerated
public final class StorageClassAnalysisDataExport implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.STORAGE_CLASS_ANALYSIS_DATA_EXPORT;
    private static final Schema $SCHEMA_OUTPUT_SCHEMA_VERSION = $SCHEMA.member("OutputSchemaVersion");
    private static final Schema $SCHEMA_DESTINATION = $SCHEMA.member("Destination");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient StorageClassAnalysisSchemaVersion outputSchemaVersion;
    private final transient AnalyticsExportDestination destination;

    private StorageClassAnalysisDataExport(Builder builder) {
        this.outputSchemaVersion = builder.outputSchemaVersion;
        this.destination = builder.destination;
    }

    /**
     * The version of the output schema to use when exporting data. Must be <code>V_1</code>.
     */
    public StorageClassAnalysisSchemaVersion getOutputSchemaVersion() {
        return outputSchemaVersion;
    }

    /**
     * The place to store the data for an analysis.
     */
    public AnalyticsExportDestination getDestination() {
        return destination;
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
        StorageClassAnalysisDataExport that = (StorageClassAnalysisDataExport) other;
        return Objects.equals(this.outputSchemaVersion, that.outputSchemaVersion)
               && Objects.equals(this.destination, that.destination);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(outputSchemaVersion);
        $hc = 31 * $hc + Objects.hashCode(destination);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_OUTPUT_SCHEMA_VERSION, outputSchemaVersion.getValue());
        if (destination != null) {
            serializer.writeStruct($SCHEMA_DESTINATION, destination);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_SCHEMA_VERSION, member, outputSchemaVersion);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION, member, destination);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link StorageClassAnalysisDataExport}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.outputSchemaVersion(this.outputSchemaVersion);
        builder.destination(this.destination);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StorageClassAnalysisDataExport}.
     */
    public static final class Builder implements ShapeBuilder<StorageClassAnalysisDataExport> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private StorageClassAnalysisSchemaVersion outputSchemaVersion;
        private AnalyticsExportDestination destination;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The version of the output schema to use when exporting data. Must be <code>V_1</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder outputSchemaVersion(StorageClassAnalysisSchemaVersion outputSchemaVersion) {
            this.outputSchemaVersion = Objects.requireNonNull(outputSchemaVersion, "outputSchemaVersion cannot be null");
            tracker.setMember($SCHEMA_OUTPUT_SCHEMA_VERSION);
            return this;
        }

        /**
         * The place to store the data for an analysis.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder destination(AnalyticsExportDestination destination) {
            this.destination = Objects.requireNonNull(destination, "destination cannot be null");
            tracker.setMember($SCHEMA_DESTINATION);
            return this;
        }

        @Override
        public StorageClassAnalysisDataExport build() {
            tracker.validate();
            return new StorageClassAnalysisDataExport(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> outputSchemaVersion((StorageClassAnalysisSchemaVersion) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_SCHEMA_VERSION, member, value));
                case 1 -> destination((AnalyticsExportDestination) SchemaUtils.validateSameMember($SCHEMA_DESTINATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<StorageClassAnalysisDataExport> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_OUTPUT_SCHEMA_VERSION)) {
                outputSchemaVersion(StorageClassAnalysisSchemaVersion.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_DESTINATION)) {
                tracker.setMember($SCHEMA_DESTINATION);
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
                    case 0 -> builder.outputSchemaVersion(StorageClassAnalysisSchemaVersion.builder().deserializeMember(de, member).build());
                    case 1 -> builder.destination(AnalyticsExportDestination.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
