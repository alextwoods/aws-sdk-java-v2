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
 * The S3 Metadata configuration for a general purpose bucket.
 */
@SmithyGenerated
public final class MetadataConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.METADATA_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_DESTINATION_RESULT = $SCHEMA.member("DestinationResult");
    private static final Schema $SCHEMA_JOURNAL_TABLE_CONFIGURATION_RESULT = $SCHEMA.member("JournalTableConfigurationResult");
    private static final Schema $SCHEMA_INVENTORY_TABLE_CONFIGURATION_RESULT = $SCHEMA.member("InventoryTableConfigurationResult");
    private static final Schema $SCHEMA_ANNOTATION_TABLE_CONFIGURATION_RESULT = $SCHEMA.member("AnnotationTableConfigurationResult");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient DestinationResult destinationResult;
    private final transient JournalTableConfigurationResult journalTableConfigurationResult;
    private final transient InventoryTableConfigurationResult inventoryTableConfigurationResult;
    private final transient AnnotationTableConfigurationResult annotationTableConfigurationResult;

    private MetadataConfigurationResult(Builder builder) {
        this.destinationResult = builder.destinationResult;
        this.journalTableConfigurationResult = builder.journalTableConfigurationResult;
        this.inventoryTableConfigurationResult = builder.inventoryTableConfigurationResult;
        this.annotationTableConfigurationResult = builder.annotationTableConfigurationResult;
    }

    /**
     * The destination settings for a metadata configuration.
     */
    public DestinationResult getDestinationResult() {
        return destinationResult;
    }

    /**
     * The journal table configuration for a metadata configuration.
     */
    public JournalTableConfigurationResult getJournalTableConfigurationResult() {
        return journalTableConfigurationResult;
    }

    /**
     * The inventory table configuration for a metadata configuration.
     */
    public InventoryTableConfigurationResult getInventoryTableConfigurationResult() {
        return inventoryTableConfigurationResult;
    }

    /**
     * The annotation table configuration result, if an annotation table is configured.
     */
    public AnnotationTableConfigurationResult getAnnotationTableConfigurationResult() {
        return annotationTableConfigurationResult;
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
        MetadataConfigurationResult that = (MetadataConfigurationResult) other;
        return Objects.equals(this.destinationResult, that.destinationResult)
               && Objects.equals(this.journalTableConfigurationResult, that.journalTableConfigurationResult)
               && Objects.equals(this.inventoryTableConfigurationResult, that.inventoryTableConfigurationResult)
               && Objects.equals(this.annotationTableConfigurationResult, that.annotationTableConfigurationResult);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(destinationResult);
        $hc = 31 * $hc + Objects.hashCode(journalTableConfigurationResult);
        $hc = 31 * $hc + Objects.hashCode(inventoryTableConfigurationResult);
        $hc = 31 * $hc + Objects.hashCode(annotationTableConfigurationResult);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (destinationResult != null) {
            serializer.writeStruct($SCHEMA_DESTINATION_RESULT, destinationResult);
        }
        if (journalTableConfigurationResult != null) {
            serializer.writeStruct($SCHEMA_JOURNAL_TABLE_CONFIGURATION_RESULT, journalTableConfigurationResult);
        }
        if (inventoryTableConfigurationResult != null) {
            serializer.writeStruct($SCHEMA_INVENTORY_TABLE_CONFIGURATION_RESULT, inventoryTableConfigurationResult);
        }
        if (annotationTableConfigurationResult != null) {
            serializer.writeStruct($SCHEMA_ANNOTATION_TABLE_CONFIGURATION_RESULT, annotationTableConfigurationResult);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_RESULT, member, destinationResult);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION_RESULT, member, journalTableConfigurationResult);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_TABLE_CONFIGURATION_RESULT, member, inventoryTableConfigurationResult);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_TABLE_CONFIGURATION_RESULT, member, annotationTableConfigurationResult);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetadataConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.destinationResult(this.destinationResult);
        builder.journalTableConfigurationResult(this.journalTableConfigurationResult);
        builder.inventoryTableConfigurationResult(this.inventoryTableConfigurationResult);
        builder.annotationTableConfigurationResult(this.annotationTableConfigurationResult);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetadataConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<MetadataConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private DestinationResult destinationResult;
        private JournalTableConfigurationResult journalTableConfigurationResult;
        private InventoryTableConfigurationResult inventoryTableConfigurationResult;
        private AnnotationTableConfigurationResult annotationTableConfigurationResult;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The destination settings for a metadata configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder destinationResult(DestinationResult destinationResult) {
            this.destinationResult = Objects.requireNonNull(destinationResult, "destinationResult cannot be null");
            tracker.setMember($SCHEMA_DESTINATION_RESULT);
            return this;
        }

        /**
         * The journal table configuration for a metadata configuration.
         *
         * @return this builder.
         */
        public Builder journalTableConfigurationResult(JournalTableConfigurationResult journalTableConfigurationResult) {
            this.journalTableConfigurationResult = journalTableConfigurationResult;
            return this;
        }

        /**
         * The inventory table configuration for a metadata configuration.
         *
         * @return this builder.
         */
        public Builder inventoryTableConfigurationResult(InventoryTableConfigurationResult inventoryTableConfigurationResult) {
            this.inventoryTableConfigurationResult = inventoryTableConfigurationResult;
            return this;
        }

        /**
         * The annotation table configuration result, if an annotation table is configured.
         *
         * @return this builder.
         */
        public Builder annotationTableConfigurationResult(AnnotationTableConfigurationResult annotationTableConfigurationResult) {
            this.annotationTableConfigurationResult = annotationTableConfigurationResult;
            return this;
        }

        @Override
        public MetadataConfigurationResult build() {
            tracker.validate();
            return new MetadataConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> destinationResult((DestinationResult) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_RESULT, member, value));
                case 1 -> journalTableConfigurationResult((JournalTableConfigurationResult) SchemaUtils.validateSameMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION_RESULT, member, value));
                case 2 -> inventoryTableConfigurationResult((InventoryTableConfigurationResult) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_TABLE_CONFIGURATION_RESULT, member, value));
                case 3 -> annotationTableConfigurationResult((AnnotationTableConfigurationResult) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_TABLE_CONFIGURATION_RESULT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetadataConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DESTINATION_RESULT)) {
                tracker.setMember($SCHEMA_DESTINATION_RESULT);
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
                    case 0 -> builder.destinationResult(DestinationResult.builder().deserializeMember(de, member).build());
                    case 1 -> builder.journalTableConfigurationResult(JournalTableConfigurationResult.builder().deserializeMember(de, member).build());
                    case 2 -> builder.inventoryTableConfigurationResult(InventoryTableConfigurationResult.builder().deserializeMember(de, member).build());
                    case 3 -> builder.annotationTableConfigurationResult(AnnotationTableConfigurationResult.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
