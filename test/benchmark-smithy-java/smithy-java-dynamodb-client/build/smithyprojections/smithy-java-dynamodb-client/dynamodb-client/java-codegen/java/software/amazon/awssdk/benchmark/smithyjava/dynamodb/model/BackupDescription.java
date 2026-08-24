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
 * Contains the description of the backup created for the table.
 */
@SmithyGenerated
public final class BackupDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BACKUP_DESCRIPTION;
    private static final Schema $SCHEMA_BACKUP_DETAILS = $SCHEMA.member("BackupDetails");
    private static final Schema $SCHEMA_SOURCE_TABLE_DETAILS = $SCHEMA.member("SourceTableDetails");
    private static final Schema $SCHEMA_SOURCE_TABLE_FEATURE_DETAILS = $SCHEMA.member("SourceTableFeatureDetails");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BackupDetails backupDetails;
    private final transient SourceTableDetails sourceTableDetails;
    private final transient SourceTableFeatureDetails sourceTableFeatureDetails;

    private BackupDescription(Builder builder) {
        this.backupDetails = builder.backupDetails;
        this.sourceTableDetails = builder.sourceTableDetails;
        this.sourceTableFeatureDetails = builder.sourceTableFeatureDetails;
    }

    /**
     * Contains the details of the backup created for the table.
     */
    public BackupDetails getBackupDetails() {
        return backupDetails;
    }

    /**
     * Contains the details of the table when the backup was created.
     */
    public SourceTableDetails getSourceTableDetails() {
        return sourceTableDetails;
    }

    /**
     * Contains the details of the features enabled on the table when the backup was created. For example, LSIs, GSIs,
     * streams, TTL.
     */
    public SourceTableFeatureDetails getSourceTableFeatureDetails() {
        return sourceTableFeatureDetails;
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
        BackupDescription that = (BackupDescription) other;
        return Objects.equals(this.backupDetails, that.backupDetails)
               && Objects.equals(this.sourceTableDetails, that.sourceTableDetails)
               && Objects.equals(this.sourceTableFeatureDetails, that.sourceTableFeatureDetails);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(backupDetails);
        $hc = 31 * $hc + Objects.hashCode(sourceTableDetails);
        $hc = 31 * $hc + Objects.hashCode(sourceTableFeatureDetails);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (backupDetails != null) {
            serializer.writeStruct($SCHEMA_BACKUP_DETAILS, backupDetails);
        }
        if (sourceTableDetails != null) {
            serializer.writeStruct($SCHEMA_SOURCE_TABLE_DETAILS, sourceTableDetails);
        }
        if (sourceTableFeatureDetails != null) {
            serializer.writeStruct($SCHEMA_SOURCE_TABLE_FEATURE_DETAILS, sourceTableFeatureDetails);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_DETAILS, member, backupDetails);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_DETAILS, member, sourceTableDetails);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_FEATURE_DETAILS, member, sourceTableFeatureDetails);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BackupDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.backupDetails(this.backupDetails);
        builder.sourceTableDetails(this.sourceTableDetails);
        builder.sourceTableFeatureDetails(this.sourceTableFeatureDetails);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BackupDescription}.
     */
    public static final class Builder implements ShapeBuilder<BackupDescription> {
        private BackupDetails backupDetails;
        private SourceTableDetails sourceTableDetails;
        private SourceTableFeatureDetails sourceTableFeatureDetails;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Contains the details of the backup created for the table.
         *
         * @return this builder.
         */
        public Builder backupDetails(BackupDetails backupDetails) {
            this.backupDetails = backupDetails;
            return this;
        }

        /**
         * Contains the details of the table when the backup was created.
         *
         * @return this builder.
         */
        public Builder sourceTableDetails(SourceTableDetails sourceTableDetails) {
            this.sourceTableDetails = sourceTableDetails;
            return this;
        }

        /**
         * Contains the details of the features enabled on the table when the backup was created. For example, LSIs, GSIs,
         * streams, TTL.
         *
         * @return this builder.
         */
        public Builder sourceTableFeatureDetails(SourceTableFeatureDetails sourceTableFeatureDetails) {
            this.sourceTableFeatureDetails = sourceTableFeatureDetails;
            return this;
        }

        @Override
        public BackupDescription build() {
            return new BackupDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> backupDetails((BackupDetails) SchemaUtils.validateSameMember($SCHEMA_BACKUP_DETAILS, member, value));
                case 1 -> sourceTableDetails((SourceTableDetails) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_DETAILS, member, value));
                case 2 -> sourceTableFeatureDetails((SourceTableFeatureDetails) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_FEATURE_DETAILS, member, value));
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
                    case 0 -> builder.backupDetails(BackupDetails.builder().deserializeMember(de, member).build());
                    case 1 -> builder.sourceTableDetails(SourceTableDetails.builder().deserializeMember(de, member).build());
                    case 2 -> builder.sourceTableFeatureDetails(SourceTableFeatureDetails.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
