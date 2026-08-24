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
 * Contains details for the backup.
 */
@SmithyGenerated
public final class BackupSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BACKUP_SUMMARY;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TABLE_ID = $SCHEMA.member("TableId");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_BACKUP_ARN = $SCHEMA.member("BackupArn");
    private static final Schema $SCHEMA_BACKUP_NAME = $SCHEMA.member("BackupName");
    private static final Schema $SCHEMA_BACKUP_CREATION_DATE_TIME = $SCHEMA.member("BackupCreationDateTime");
    private static final Schema $SCHEMA_BACKUP_EXPIRY_DATE_TIME = $SCHEMA.member("BackupExpiryDateTime");
    private static final Schema $SCHEMA_BACKUP_STATUS = $SCHEMA.member("BackupStatus");
    private static final Schema $SCHEMA_BACKUP_TYPE = $SCHEMA.member("BackupType");
    private static final Schema $SCHEMA_BACKUP_SIZE_BYTES = $SCHEMA.member("BackupSizeBytes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String tableId;
    private final transient String tableArn;
    private final transient String backupArn;
    private final transient String backupName;
    private final transient Instant backupCreationDateTime;
    private final transient Instant backupExpiryDateTime;
    private final transient BackupStatus backupStatus;
    private final transient BackupType backupType;
    private final transient Long backupSizeBytes;

    private BackupSummary(Builder builder) {
        this.tableName = builder.tableName;
        this.tableId = builder.tableId;
        this.tableArn = builder.tableArn;
        this.backupArn = builder.backupArn;
        this.backupName = builder.backupName;
        this.backupCreationDateTime = builder.backupCreationDateTime;
        this.backupExpiryDateTime = builder.backupExpiryDateTime;
        this.backupStatus = builder.backupStatus;
        this.backupType = builder.backupType;
        this.backupSizeBytes = builder.backupSizeBytes;
    }

    /**
     * Name of the table.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Unique identifier for the table.
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * ARN associated with the table.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * ARN associated with the backup.
     */
    public String getBackupArn() {
        return backupArn;
    }

    /**
     * Name of the specified backup.
     */
    public String getBackupName() {
        return backupName;
    }

    /**
     * Time at which the backup was created.
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
     * Size of the backup in bytes.
     */
    public Long getBackupSizeBytes() {
        return backupSizeBytes;
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
        BackupSummary that = (BackupSummary) other;
        return Objects.equals(this.backupSizeBytes, that.backupSizeBytes)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableId, that.tableId)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.backupArn, that.backupArn)
               && Objects.equals(this.backupName, that.backupName)
               && Objects.equals(this.backupStatus, that.backupStatus)
               && Objects.equals(this.backupType, that.backupType)
               && Objects.equals(this.backupCreationDateTime, that.backupCreationDateTime)
               && Objects.equals(this.backupExpiryDateTime, that.backupExpiryDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(tableId);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(backupArn);
        $hc = 31 * $hc + Objects.hashCode(backupName);
        $hc = 31 * $hc + Objects.hashCode(backupCreationDateTime);
        $hc = 31 * $hc + Objects.hashCode(backupExpiryDateTime);
        $hc = 31 * $hc + Objects.hashCode(backupStatus);
        $hc = 31 * $hc + Objects.hashCode(backupType);
        $hc = 31 * $hc + Objects.hashCode(backupSizeBytes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (tableId != null) {
            serializer.writeString($SCHEMA_TABLE_ID, tableId);
        }
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (backupArn != null) {
            serializer.writeString($SCHEMA_BACKUP_ARN, backupArn);
        }
        if (backupName != null) {
            serializer.writeString($SCHEMA_BACKUP_NAME, backupName);
        }
        if (backupCreationDateTime != null) {
            serializer.writeTimestamp($SCHEMA_BACKUP_CREATION_DATE_TIME, backupCreationDateTime);
        }
        if (backupExpiryDateTime != null) {
            serializer.writeTimestamp($SCHEMA_BACKUP_EXPIRY_DATE_TIME, backupExpiryDateTime);
        }
        if (backupStatus != null) {
            serializer.writeString($SCHEMA_BACKUP_STATUS, backupStatus.getValue());
        }
        if (backupType != null) {
            serializer.writeString($SCHEMA_BACKUP_TYPE, backupType.getValue());
        }
        if (backupSizeBytes != null) {
            serializer.writeLong($SCHEMA_BACKUP_SIZE_BYTES, backupSizeBytes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, tableId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_ARN, member, backupArn);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_NAME, member, backupName);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_CREATION_DATE_TIME, member, backupCreationDateTime);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_EXPIRY_DATE_TIME, member, backupExpiryDateTime);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_STATUS, member, backupStatus);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_TYPE, member, backupType);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_SIZE_BYTES, member, backupSizeBytes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BackupSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.tableId(this.tableId);
        builder.tableArn(this.tableArn);
        builder.backupArn(this.backupArn);
        builder.backupName(this.backupName);
        builder.backupCreationDateTime(this.backupCreationDateTime);
        builder.backupExpiryDateTime(this.backupExpiryDateTime);
        builder.backupStatus(this.backupStatus);
        builder.backupType(this.backupType);
        builder.backupSizeBytes(this.backupSizeBytes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BackupSummary}.
     */
    public static final class Builder implements ShapeBuilder<BackupSummary> {
        private String tableName;
        private String tableId;
        private String tableArn;
        private String backupArn;
        private String backupName;
        private Instant backupCreationDateTime;
        private Instant backupExpiryDateTime;
        private BackupStatus backupStatus;
        private BackupType backupType;
        private Long backupSizeBytes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Name of the table.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Unique identifier for the table.
         *
         * @return this builder.
         */
        public Builder tableId(String tableId) {
            this.tableId = tableId;
            return this;
        }

        /**
         * ARN associated with the table.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * ARN associated with the backup.
         *
         * @return this builder.
         */
        public Builder backupArn(String backupArn) {
            this.backupArn = backupArn;
            return this;
        }

        /**
         * Name of the specified backup.
         *
         * @return this builder.
         */
        public Builder backupName(String backupName) {
            this.backupName = backupName;
            return this;
        }

        /**
         * Time at which the backup was created.
         *
         * @return this builder.
         */
        public Builder backupCreationDateTime(Instant backupCreationDateTime) {
            this.backupCreationDateTime = backupCreationDateTime;
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

        /**
         * Backup can be in one of the following states: CREATING, ACTIVE, DELETED.
         *
         * @return this builder.
         */
        public Builder backupStatus(BackupStatus backupStatus) {
            this.backupStatus = backupStatus;
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
         * @return this builder.
         */
        public Builder backupType(BackupType backupType) {
            this.backupType = backupType;
            return this;
        }

        /**
         * Size of the backup in bytes.
         *
         * @return this builder.
         */
        public Builder backupSizeBytes(Long backupSizeBytes) {
            this.backupSizeBytes = backupSizeBytes;
            return this;
        }

        @Override
        public BackupSummary build() {
            return new BackupSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> tableId((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ID, member, value));
                case 2 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 3 -> backupArn((String) SchemaUtils.validateSameMember($SCHEMA_BACKUP_ARN, member, value));
                case 4 -> backupName((String) SchemaUtils.validateSameMember($SCHEMA_BACKUP_NAME, member, value));
                case 5 -> backupCreationDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_BACKUP_CREATION_DATE_TIME, member, value));
                case 6 -> backupExpiryDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_BACKUP_EXPIRY_DATE_TIME, member, value));
                case 7 -> backupStatus((BackupStatus) SchemaUtils.validateSameMember($SCHEMA_BACKUP_STATUS, member, value));
                case 8 -> backupType((BackupType) SchemaUtils.validateSameMember($SCHEMA_BACKUP_TYPE, member, value));
                case 9 -> backupSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_BACKUP_SIZE_BYTES, member, value));
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.tableId(de.readString(member));
                    case 2 -> builder.tableArn(de.readString(member));
                    case 3 -> builder.backupArn(de.readString(member));
                    case 4 -> builder.backupName(de.readString(member));
                    case 5 -> builder.backupCreationDateTime(de.readTimestamp(member));
                    case 6 -> builder.backupExpiryDateTime(de.readTimestamp(member));
                    case 7 -> builder.backupStatus(BackupStatus.builder().deserializeMember(de, member).build());
                    case 8 -> builder.backupType(BackupType.builder().deserializeMember(de, member).build());
                    case 9 -> builder.backupSizeBytes(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
