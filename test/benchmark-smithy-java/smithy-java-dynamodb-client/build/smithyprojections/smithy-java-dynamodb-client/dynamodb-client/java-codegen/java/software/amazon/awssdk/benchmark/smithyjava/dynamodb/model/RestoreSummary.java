package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * Contains details for the restore.
 */
@SmithyGenerated
public final class RestoreSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.RESTORE_SUMMARY;
    private static final Schema $SCHEMA_SOURCE_BACKUP_ARN = $SCHEMA.member("SourceBackupArn");
    private static final Schema $SCHEMA_SOURCE_TABLE_ARN = $SCHEMA.member("SourceTableArn");
    private static final Schema $SCHEMA_RESTORE_DATE_TIME = $SCHEMA.member("RestoreDateTime");
    private static final Schema $SCHEMA_RESTORE_IN_PROGRESS = $SCHEMA.member("RestoreInProgress");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String sourceBackupArn;
    private final transient String sourceTableArn;
    private final transient Instant restoreDateTime;
    private final transient boolean restoreInProgress;

    private RestoreSummary(Builder builder) {
        this.sourceBackupArn = builder.sourceBackupArn;
        this.sourceTableArn = builder.sourceTableArn;
        this.restoreDateTime = builder.restoreDateTime;
        this.restoreInProgress = builder.restoreInProgress;
    }

    /**
     * The Amazon Resource Name (ARN) of the backup from which the table was restored.
     */
    public String getSourceBackupArn() {
        return sourceBackupArn;
    }

    /**
     * The ARN of the source table of the backup that is being restored.
     */
    public String getSourceTableArn() {
        return sourceTableArn;
    }

    /**
     * Point in time or source backup time.
     */
    public Instant getRestoreDateTime() {
        return restoreDateTime;
    }

    /**
     * Indicates if a restore is in progress or not.
     */
    public boolean isRestoreInProgress() {
        return restoreInProgress;
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
        RestoreSummary that = (RestoreSummary) other;
        return this.restoreInProgress == that.restoreInProgress
               && Objects.equals(this.sourceBackupArn, that.sourceBackupArn)
               && Objects.equals(this.sourceTableArn, that.sourceTableArn)
               && Objects.equals(this.restoreDateTime, that.restoreDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sourceBackupArn);
        $hc = 31 * $hc + Objects.hashCode(sourceTableArn);
        $hc = 31 * $hc + Objects.hashCode(restoreDateTime);
        $hc = 31 * $hc + Boolean.hashCode(restoreInProgress);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sourceBackupArn != null) {
            serializer.writeString($SCHEMA_SOURCE_BACKUP_ARN, sourceBackupArn);
        }
        if (sourceTableArn != null) {
            serializer.writeString($SCHEMA_SOURCE_TABLE_ARN, sourceTableArn);
        }
        serializer.writeTimestamp($SCHEMA_RESTORE_DATE_TIME, restoreDateTime);
        serializer.writeBoolean($SCHEMA_RESTORE_IN_PROGRESS, restoreInProgress);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_DATE_TIME, member, restoreDateTime);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_IN_PROGRESS, member, restoreInProgress);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_BACKUP_ARN, member, sourceBackupArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_ARN, member, sourceTableArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RestoreSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sourceBackupArn(this.sourceBackupArn);
        builder.sourceTableArn(this.sourceTableArn);
        builder.restoreDateTime(this.restoreDateTime);
        builder.restoreInProgress(this.restoreInProgress);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RestoreSummary}.
     */
    public static final class Builder implements ShapeBuilder<RestoreSummary> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String sourceBackupArn;
        private String sourceTableArn;
        private Instant restoreDateTime;
        private boolean restoreInProgress;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the backup from which the table was restored.
         *
         * @return this builder.
         */
        public Builder sourceBackupArn(String sourceBackupArn) {
            this.sourceBackupArn = sourceBackupArn;
            return this;
        }

        /**
         * The ARN of the source table of the backup that is being restored.
         *
         * @return this builder.
         */
        public Builder sourceTableArn(String sourceTableArn) {
            this.sourceTableArn = sourceTableArn;
            return this;
        }

        /**
         * Point in time or source backup time.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder restoreDateTime(Instant restoreDateTime) {
            this.restoreDateTime = Objects.requireNonNull(restoreDateTime, "restoreDateTime cannot be null");
            tracker.setMember($SCHEMA_RESTORE_DATE_TIME);
            return this;
        }

        /**
         * Indicates if a restore is in progress or not.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder restoreInProgress(boolean restoreInProgress) {
            this.restoreInProgress = restoreInProgress;
            tracker.setMember($SCHEMA_RESTORE_IN_PROGRESS);
            return this;
        }

        @Override
        public RestoreSummary build() {
            tracker.validate();
            return new RestoreSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> restoreDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_RESTORE_DATE_TIME, member, value));
                case 1 -> restoreInProgress((boolean) SchemaUtils.validateSameMember($SCHEMA_RESTORE_IN_PROGRESS, member, value));
                case 2 -> sourceBackupArn((String) SchemaUtils.validateSameMember($SCHEMA_SOURCE_BACKUP_ARN, member, value));
                case 3 -> sourceTableArn((String) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RestoreSummary> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESTORE_DATE_TIME)) {
                restoreDateTime(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_RESTORE_IN_PROGRESS)) {
                tracker.setMember($SCHEMA_RESTORE_IN_PROGRESS);
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
                    case 0 -> builder.restoreDateTime(de.readTimestamp(member));
                    case 1 -> builder.restoreInProgress(de.readBoolean(member));
                    case 2 -> builder.sourceBackupArn(de.readString(member));
                    case 3 -> builder.sourceTableArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
