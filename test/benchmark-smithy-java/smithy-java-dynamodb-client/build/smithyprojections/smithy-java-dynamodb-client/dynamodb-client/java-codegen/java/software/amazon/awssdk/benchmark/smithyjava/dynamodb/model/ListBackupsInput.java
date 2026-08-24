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

@SmithyGenerated
public final class ListBackupsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_BACKUPS_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_LIMIT = $SCHEMA.member("Limit");
    private static final Schema $SCHEMA_TIME_RANGE_LOWER_BOUND = $SCHEMA.member("TimeRangeLowerBound");
    private static final Schema $SCHEMA_TIME_RANGE_UPPER_BOUND = $SCHEMA.member("TimeRangeUpperBound");
    private static final Schema $SCHEMA_EXCLUSIVE_START_BACKUP_ARN = $SCHEMA.member("ExclusiveStartBackupArn");
    private static final Schema $SCHEMA_BACKUP_TYPE = $SCHEMA.member("BackupType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient Integer limit;
    private final transient Instant timeRangeLowerBound;
    private final transient Instant timeRangeUpperBound;
    private final transient String exclusiveStartBackupArn;
    private final transient BackupTypeFilter backupType;

    private ListBackupsInput(Builder builder) {
        this.tableName = builder.tableName;
        this.limit = builder.limit;
        this.timeRangeLowerBound = builder.timeRangeLowerBound;
        this.timeRangeUpperBound = builder.timeRangeUpperBound;
        this.exclusiveStartBackupArn = builder.exclusiveStartBackupArn;
        this.backupType = builder.backupType;
    }

    /**
     * Lists the backups from the table specified in <code>TableName</code>. You can also provide the Amazon Resource
     * Name (ARN) of the table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Maximum number of backups to return at once.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Only backups created after this time are listed. <code>TimeRangeLowerBound</code> is inclusive.
     */
    public Instant getTimeRangeLowerBound() {
        return timeRangeLowerBound;
    }

    /**
     * Only backups created before this time are listed. <code>TimeRangeUpperBound</code> is exclusive.
     */
    public Instant getTimeRangeUpperBound() {
        return timeRangeUpperBound;
    }

    /**
     * <code>LastEvaluatedBackupArn</code> is the Amazon Resource Name (ARN) of the backup last evaluated when the
     * current page of results was returned, inclusive of the current page of results. This value may be specified as
     * the <code>ExclusiveStartBackupArn</code> of a new <code>ListBackups</code> operation in order to fetch the next
     * page of results.
     */
    public String getExclusiveStartBackupArn() {
        return exclusiveStartBackupArn;
    }

    /**
     * The backups from the table specified by <code>BackupType</code> are listed.
     *
     * <p>Where <code>BackupType</code> can be:
     *
     * <ul>
     *   <li>
     *     <code>USER</code> - On-demand backup created by you. (The default setting if no other backup types are
     *     specified.)
     *   </li>
     *   <li>
     *     <code>SYSTEM</code> - On-demand backup automatically created by DynamoDB.
     *   </li>
     *   <li>
     *     <code>ALL</code> - All types of on-demand backups (USER and SYSTEM).
     *   </li>
     * </ul>
     */
    public BackupTypeFilter getBackupType() {
        return backupType;
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
        ListBackupsInput that = (ListBackupsInput) other;
        return Objects.equals(this.limit, that.limit)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.exclusiveStartBackupArn, that.exclusiveStartBackupArn)
               && Objects.equals(this.backupType, that.backupType)
               && Objects.equals(this.timeRangeLowerBound, that.timeRangeLowerBound)
               && Objects.equals(this.timeRangeUpperBound, that.timeRangeUpperBound);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(limit);
        $hc = 31 * $hc + Objects.hashCode(timeRangeLowerBound);
        $hc = 31 * $hc + Objects.hashCode(timeRangeUpperBound);
        $hc = 31 * $hc + Objects.hashCode(exclusiveStartBackupArn);
        $hc = 31 * $hc + Objects.hashCode(backupType);
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
        if (limit != null) {
            serializer.writeInteger($SCHEMA_LIMIT, limit);
        }
        if (timeRangeLowerBound != null) {
            serializer.writeTimestamp($SCHEMA_TIME_RANGE_LOWER_BOUND, timeRangeLowerBound);
        }
        if (timeRangeUpperBound != null) {
            serializer.writeTimestamp($SCHEMA_TIME_RANGE_UPPER_BOUND, timeRangeUpperBound);
        }
        if (exclusiveStartBackupArn != null) {
            serializer.writeString($SCHEMA_EXCLUSIVE_START_BACKUP_ARN, exclusiveStartBackupArn);
        }
        if (backupType != null) {
            serializer.writeString($SCHEMA_BACKUP_TYPE, backupType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, limit);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_RANGE_LOWER_BOUND, member, timeRangeLowerBound);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_RANGE_UPPER_BOUND, member, timeRangeUpperBound);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_BACKUP_ARN, member, exclusiveStartBackupArn);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_TYPE, member, backupType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListBackupsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.limit(this.limit);
        builder.timeRangeLowerBound(this.timeRangeLowerBound);
        builder.timeRangeUpperBound(this.timeRangeUpperBound);
        builder.exclusiveStartBackupArn(this.exclusiveStartBackupArn);
        builder.backupType(this.backupType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListBackupsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListBackupsInput> {
        private String tableName;
        private Integer limit;
        private Instant timeRangeLowerBound;
        private Instant timeRangeUpperBound;
        private String exclusiveStartBackupArn;
        private BackupTypeFilter backupType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Lists the backups from the table specified in <code>TableName</code>. You can also provide the Amazon Resource
         * Name (ARN) of the table in this parameter.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Maximum number of backups to return at once.
         *
         * @return this builder.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Only backups created after this time are listed. <code>TimeRangeLowerBound</code> is inclusive.
         *
         * @return this builder.
         */
        public Builder timeRangeLowerBound(Instant timeRangeLowerBound) {
            this.timeRangeLowerBound = timeRangeLowerBound;
            return this;
        }

        /**
         * Only backups created before this time are listed. <code>TimeRangeUpperBound</code> is exclusive.
         *
         * @return this builder.
         */
        public Builder timeRangeUpperBound(Instant timeRangeUpperBound) {
            this.timeRangeUpperBound = timeRangeUpperBound;
            return this;
        }

        /**
         * <code>LastEvaluatedBackupArn</code> is the Amazon Resource Name (ARN) of the backup last evaluated when the
         * current page of results was returned, inclusive of the current page of results. This value may be specified as
         * the <code>ExclusiveStartBackupArn</code> of a new <code>ListBackups</code> operation in order to fetch the next
         * page of results.
         *
         * @return this builder.
         */
        public Builder exclusiveStartBackupArn(String exclusiveStartBackupArn) {
            this.exclusiveStartBackupArn = exclusiveStartBackupArn;
            return this;
        }

        /**
         * The backups from the table specified by <code>BackupType</code> are listed.
         *
         * <p>Where <code>BackupType</code> can be:
         *
         * <ul>
         *   <li>
         *     <code>USER</code> - On-demand backup created by you. (The default setting if no other backup types are
         *     specified.)
         *   </li>
         *   <li>
         *     <code>SYSTEM</code> - On-demand backup automatically created by DynamoDB.
         *   </li>
         *   <li>
         *     <code>ALL</code> - All types of on-demand backups (USER and SYSTEM).
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder backupType(BackupTypeFilter backupType) {
            this.backupType = backupType;
            return this;
        }

        @Override
        public ListBackupsInput build() {
            return new ListBackupsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> limit((Integer) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, value));
                case 2 -> timeRangeLowerBound((Instant) SchemaUtils.validateSameMember($SCHEMA_TIME_RANGE_LOWER_BOUND, member, value));
                case 3 -> timeRangeUpperBound((Instant) SchemaUtils.validateSameMember($SCHEMA_TIME_RANGE_UPPER_BOUND, member, value));
                case 4 -> exclusiveStartBackupArn((String) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_BACKUP_ARN, member, value));
                case 5 -> backupType((BackupTypeFilter) SchemaUtils.validateSameMember($SCHEMA_BACKUP_TYPE, member, value));
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
                    case 1 -> builder.limit(de.readInteger(member));
                    case 2 -> builder.timeRangeLowerBound(de.readTimestamp(member));
                    case 3 -> builder.timeRangeUpperBound(de.readTimestamp(member));
                    case 4 -> builder.exclusiveStartBackupArn(de.readString(member));
                    case 5 -> builder.backupType(BackupTypeFilter.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
