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
public final class MetadataConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METADATA_CONFIGURATION;
    private static final Schema $SCHEMA_JOURNAL_TABLE_CONFIGURATION = $SCHEMA.member("JournalTableConfiguration");
    private static final Schema $SCHEMA_INVENTORY_TABLE_CONFIGURATION = $SCHEMA.member("InventoryTableConfiguration");
    private static final Schema $SCHEMA_ANNOTATION_TABLE_CONFIGURATION = $SCHEMA.member("AnnotationTableConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient JournalTableConfiguration journalTableConfiguration;
    private final transient InventoryTableConfiguration inventoryTableConfiguration;
    private final transient AnnotationTableConfiguration annotationTableConfiguration;

    private MetadataConfiguration(Builder builder) {
        this.journalTableConfiguration = builder.journalTableConfiguration;
        this.inventoryTableConfiguration = builder.inventoryTableConfiguration;
        this.annotationTableConfiguration = builder.annotationTableConfiguration;
    }

    /**
     * The journal table configuration for a metadata configuration.
     */
    public JournalTableConfiguration getJournalTableConfiguration() {
        return journalTableConfiguration;
    }

    /**
     * The inventory table configuration for a metadata configuration.
     */
    public InventoryTableConfiguration getInventoryTableConfiguration() {
        return inventoryTableConfiguration;
    }

    /**
     * Optional annotation table configuration to include with the metadata configuration.
     */
    public AnnotationTableConfiguration getAnnotationTableConfiguration() {
        return annotationTableConfiguration;
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
        MetadataConfiguration that = (MetadataConfiguration) other;
        return Objects.equals(this.journalTableConfiguration, that.journalTableConfiguration)
               && Objects.equals(this.inventoryTableConfiguration, that.inventoryTableConfiguration)
               && Objects.equals(this.annotationTableConfiguration, that.annotationTableConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(journalTableConfiguration);
        $hc = 31 * $hc + Objects.hashCode(inventoryTableConfiguration);
        $hc = 31 * $hc + Objects.hashCode(annotationTableConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (journalTableConfiguration != null) {
            serializer.writeStruct($SCHEMA_JOURNAL_TABLE_CONFIGURATION, journalTableConfiguration);
        }
        if (inventoryTableConfiguration != null) {
            serializer.writeStruct($SCHEMA_INVENTORY_TABLE_CONFIGURATION, inventoryTableConfiguration);
        }
        if (annotationTableConfiguration != null) {
            serializer.writeStruct($SCHEMA_ANNOTATION_TABLE_CONFIGURATION, annotationTableConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION, member, journalTableConfiguration);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_TABLE_CONFIGURATION, member, inventoryTableConfiguration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_TABLE_CONFIGURATION, member, annotationTableConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetadataConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.journalTableConfiguration(this.journalTableConfiguration);
        builder.inventoryTableConfiguration(this.inventoryTableConfiguration);
        builder.annotationTableConfiguration(this.annotationTableConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetadataConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<MetadataConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private JournalTableConfiguration journalTableConfiguration;
        private InventoryTableConfiguration inventoryTableConfiguration;
        private AnnotationTableConfiguration annotationTableConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The journal table configuration for a metadata configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder journalTableConfiguration(JournalTableConfiguration journalTableConfiguration) {
            this.journalTableConfiguration = Objects.requireNonNull(journalTableConfiguration, "journalTableConfiguration cannot be null");
            tracker.setMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION);
            return this;
        }

        /**
         * The inventory table configuration for a metadata configuration.
         *
         * @return this builder.
         */
        public Builder inventoryTableConfiguration(InventoryTableConfiguration inventoryTableConfiguration) {
            this.inventoryTableConfiguration = inventoryTableConfiguration;
            return this;
        }

        /**
         * Optional annotation table configuration to include with the metadata configuration.
         *
         * @return this builder.
         */
        public Builder annotationTableConfiguration(AnnotationTableConfiguration annotationTableConfiguration) {
            this.annotationTableConfiguration = annotationTableConfiguration;
            return this;
        }

        @Override
        public MetadataConfiguration build() {
            tracker.validate();
            return new MetadataConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> journalTableConfiguration((JournalTableConfiguration) SchemaUtils.validateSameMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION, member, value));
                case 1 -> inventoryTableConfiguration((InventoryTableConfiguration) SchemaUtils.validateSameMember($SCHEMA_INVENTORY_TABLE_CONFIGURATION, member, value));
                case 2 -> annotationTableConfiguration((AnnotationTableConfiguration) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_TABLE_CONFIGURATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetadataConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION)) {
                tracker.setMember($SCHEMA_JOURNAL_TABLE_CONFIGURATION);
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
                    case 0 -> builder.journalTableConfiguration(JournalTableConfiguration.builder().deserializeMember(de, member).build());
                    case 1 -> builder.inventoryTableConfiguration(InventoryTableConfiguration.builder().deserializeMember(de, member).build());
                    case 2 -> builder.annotationTableConfiguration(AnnotationTableConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
