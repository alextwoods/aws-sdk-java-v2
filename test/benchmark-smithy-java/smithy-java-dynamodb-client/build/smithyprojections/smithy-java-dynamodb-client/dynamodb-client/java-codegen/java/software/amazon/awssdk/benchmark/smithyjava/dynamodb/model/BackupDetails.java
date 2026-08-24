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
 * Contains the details of the backup created for the table.
 */
@SmithyGenerated
public final class BackupDetails implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BACKUP_DETAILS;
    private static final Schema $SCHEMA_BACKUP_ARN = $SCHEMA.member("BackupArn");
    private static final Schema $SCHEMA_BACKUP_NAME = $SCHEMA.member("BackupName");
    private static final Schema $SCHEMA_BACKUP_SIZE_BYTES = $SCHEMA.member("BackupSizeBytes");
    private static final Schema $SCHEMA_BACKUP_STATUS = $SCHEMA.member("BackupStatus");
    private static final Schema $SCHEMA_BACKUP_TYPE = $SCHEMA.member("BackupType");
    private static final Schema $SCHEMA_BACKUP_CREATION_DATE_TIME = $SCHEMA.member("BackupCreationDateTime");
    private static final Schema $SCHEMA_BACKUP_EXPIRY_DATE_TIME = $SCHEMA.member("BackupExpiryDateTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String backupArn;
    private final transient String backupName;
    private final transient Long backupSizeBytes;
    private final transient BackupStatus backupStatus;
    private final transient BackupType backupType;
    private final transient Instant backupCreationDateTime;
    private final transient Instant backupExpiryDateTime;

    private BackupDetails(Builder builder) {
        this.backupArn = builder.backupArn;
        this.backupName = builder.backupName;
        this.backupSizeBytes = builder.backupSizeBytes;
        this.backupStatus = builder.backupStatus;
        this.backupType = builder.backupType;
        this.backupCreationDateTime = builder.backupCreationDateTime;
        this.backupExpiryDateTime = builder.backupExpiryDateTime;
    }

    /**
     * ARN associated with the backup.
     */
    public String getBackupArn() {
        return backupArn;
    }

    /**
     * Name of the requested backup.
     */
    public String getBackupName() {
        return backupName;
    }

    /**
     * Size of the backup in bytes. DynamoDB updates this value approximately every six hours. Recent changes might not
     * be reflected in this value.
     */
    public Long getBackupSizeBytes() {
        return backupSizeBytes;
    }

    /**
     * Backup can be in one of the following states: CREATING, ACTIVE, DELETED.
     */
    public BackupStatus getBackupStatus() {
        return backupStatus;
    }

    /**
     * BackupType:
     *
     * <ul>
     *   <li>
     *     <code>USER</code> - You create and manage these using the on-demand backup feature.
     *   </li>
     *   <li>
     *     <code>SYSTEM</code> - If you delete a table with point-in-time recovery enabled, a <code>SYSTEM</code>
     *     backup is automatically created and is retained for 35 days (at no additional cost). System backups allow
     *     you to restore the deleted table to the state it was in just before the point of deletion.
     *   </li>
     *   <li>
     *     <code>AWS_BACKUP</code> - On-demand backup created by you from Backup service.
     *   </li>
     * </ul>
     */
    public BackupType getBackupType() {
        return backupType;
    }

    /**
     * Time at which the backup was created. This is the request time of the backup.
     */
    public Instant getBackupCreationDateTime() {
        return backupCreationDateTime;
    }

    /**
     * Time at which the automatic on-demand backup created by DynamoDB will expire. This <code>SYSTEM</code> on-demand
     * backup expires automatically 35 days after its creation.
     */
    public Instant getBackupExpiryDateTime() {
        return backupExpiryDateTime;
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
        BackupDetails that = (BackupDetails) other;
        return Objects.equals(this.backupSizeBytes, that.backupSizeBytes)
               && Objects.equals(this.backupArn, that.backupArn)
               && Objects.equals(this.backupName, that.backupName)
               && Objects.equals(this.backupStatus, that.backupStatus)
               && Objects.equals(this.backupType, that.backupType)
               && Objects.equals(this.backupCreationDateTime, that.backupCreationDateTime)
               && Objects.equals(this.backupExpiryDateTime, that.backupExpiryDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(backupArn);
        $hc = 31 * $hc + Objects.hashCode(backupName);
        $hc = 31 * $hc + Objects.hashCode(backupSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(backupStatus);
        $hc = 31 * $hc + Objects.hashCode(backupType);
        $hc = 31 * $hc + Objects.hashCode(backupCreationDateTime);
        $hc = 31 * $hc + Objects.hashCode(backupExpiryDateTime);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BACKUP_ARN, backupArn);
        serializer.writeString($SCHEMA_BACKUP_NAME, backupName);
        if (backupSizeBytes != null) {
            serializer.writeLong($SCHEMA_BACKUP_SIZE_BYTES, backupSizeBytes);
        }
        serializer.writeString($SCHEMA_BACKUP_STATUS, backupStatus.getValue());
        serializer.writeString($SCHEMA_BACKUP_TYPE, backupType.getValue());
        serializer.writeTimestamp($SCHEMA_BACKUP_CREATION_DATE_TIME, backupCreationDateTime);
        if (backupExpiryDateTime != null) {
            serializer.writeTimestamp($SCHEMA_BACKUP_EXPIRY_DATE_TIME, backupExpiryDateTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_ARN, member, backupArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_NAME, member, backupName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_STATUS, member, backupStatus);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_TYPE, member, backupType);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_CREATION_DATE_TIME, member, backupCreationDateTime);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_SIZE_BYTES, member, backupSizeBytes);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_EXPIRY_DATE_TIME, member, backupExpiryDateTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BackupDetails}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.backupArn(this.backupArn);
        builder.backupName(this.backupName);
        builder.backupSizeBytes(this.backupSizeBytes);
        builder.backupStatus(this.backupStatus);
        builder.backupType(this.backupType);
        builder.backupCreationDateTime(this.backupCreationDateTime);
        builder.backupExpiryDateTime(this.backupExpiryDateTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BackupDetails}.
     */
    public static final class Builder implements ShapeBuilder<BackupDetails> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String backupArn;
        private String backupName;
        private Long backupSizeBytes;
        private BackupStatus backupStatus;
        private BackupType backupType;
        private Instant backupCreationDateTime;
        private Instant backupExpiryDateTime;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * ARN associated with the backup.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupArn(String backupArn) {
            this.backupArn = Objects.requireNonNull(backupArn, "backupArn cannot be null");
            tracker.setMember($SCHEMA_BACKUP_ARN);
            return this;
        }

        /**
         * Name of the requested backup.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupName(String backupName) {
            this.backupName = Objects.requireNonNull(backupName, "backupName cannot be null");
            tracker.setMember($SCHEMA_BACKUP_NAME);
            return this;
        }

        /**
         * Size of the backup in bytes. DynamoDB updates this value approximately every six hours. Recent changes might not
         * be reflected in this value.
         *
         * @return this builder.
         */
        public Builder backupSizeBytes(Long backupSizeBytes) {
            this.backupSizeBytes = backupSizeBytes;
            return this;
        }

        /**
         * Backup can be in one of the following states: CREATING, ACTIVE, DELETED.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupStatus(BackupStatus backupStatus) {
            this.backupStatus = Objects.requireNonNull(backupStatus, "backupStatus cannot be null");
            tracker.setMember($SCHEMA_BACKUP_STATUS);
            return this;
        }

        /**
         * BackupType:
         *
         * <ul>
         *   <li>
         *     <code>USER</code> - You create and manage these using the on-demand backup feature.
         *   </li>
         *   <li>
         *     <code>SYSTEM</code> - If you delete a table with point-in-time recovery enabled, a <code>SYSTEM</code>
         *     backup is automatically created and is retained for 35 days (at no additional cost). System backups allow
         *     you to restore the deleted table to the state it was in just before the point of deletion.
         *   </li>
         *   <li>
         *     <code>AWS_BACKUP</code> - On-demand backup created by you from Backup service.
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupType(BackupType backupType) {
            this.backupType = Objects.requireNonNull(backupType, "backupType cannot be null");
            tracker.setMember($SCHEMA_BACKUP_TYPE);
            return this;
        }

        /**
         * Time at which the backup was created. This is the request time of the backup.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupCreationDateTime(Instant backupCreationDateTime) {
            this.backupCreationDateTime = Objects.requireNonNull(backupCreationDateTime, "backupCreationDateTime cannot be null");
            tracker.setMember($SCHEMA_BACKUP_CREATION_DATE_TIME);
            return this;
        }

        /**
         * Time at which the automatic on-demand backup created by DynamoDB will expire. This <code>SYSTEM</code> on-demand
         * backup expires automatically 35 days after its creation.
         *
         * @return this builder.
         */
        public Builder backupExpiryDateTime(Instant backupExpiryDateTime) {
            this.backupExpiryDateTime = backupExpiryDateTime;
            return this;
        }

        @Override
        public BackupDetails build() {
            tracker.validate();
            return new BackupDetails(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> backupArn((String) SchemaUtils.validateSameMember($SCHEMA_BACKUP_ARN, member, value));
                case 1 -> backupName((String) SchemaUtils.validateSameMember($SCHEMA_BACKUP_NAME, member, value));
                case 2 -> backupStatus((BackupStatus) SchemaUtils.validateSameMember($SCHEMA_BACKUP_STATUS, member, value));
                case 3 -> backupType((BackupType) SchemaUtils.validateSameMember($SCHEMA_BACKUP_TYPE, member, value));
                case 4 -> backupCreationDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_BACKUP_CREATION_DATE_TIME, member, value));
                case 5 -> backupSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_BACKUP_SIZE_BYTES, member, value));
                case 6 -> backupExpiryDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_BACKUP_EXPIRY_DATE_TIME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<BackupDetails> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_ARN)) {
                backupArn("");
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_NAME)) {
                backupName("");
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_STATUS)) {
                backupStatus(BackupStatus.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_TYPE)) {
                backupType(BackupType.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_CREATION_DATE_TIME)) {
                backupCreationDateTime(Instant.EPOCH);
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
                    case 0 -> builder.backupArn(de.readString(member));
                    case 1 -> builder.backupName(de.readString(member));
                    case 2 -> builder.backupStatus(BackupStatus.builder().deserializeMember(de, member).build());
                    case 3 -> builder.backupType(BackupType.builder().deserializeMember(de, member).build());
                    case 4 -> builder.backupCreationDateTime(de.readTimestamp(member));
                    case 5 -> builder.backupSizeBytes(de.readLong(member));
                    case 6 -> builder.backupExpiryDateTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
