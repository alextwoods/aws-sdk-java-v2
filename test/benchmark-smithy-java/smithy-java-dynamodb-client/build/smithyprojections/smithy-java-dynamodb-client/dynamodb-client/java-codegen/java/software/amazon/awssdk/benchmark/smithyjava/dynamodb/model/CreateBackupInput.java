package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

@SmithyGenerated
public final class CreateBackupInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_BACKUP_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_BACKUP_NAME = $SCHEMA.member("BackupName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String backupName;

    private CreateBackupInput(Builder builder) {
        this.tableName = builder.tableName;
        this.backupName = builder.backupName;
    }

    /**
     * The name of the table. You can also provide the Amazon Resource Name (ARN) of the table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Specified name for the backup.
     */
    public String getBackupName() {
        return backupName;
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
        CreateBackupInput that = (CreateBackupInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.backupName, that.backupName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(backupName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        serializer.writeString($SCHEMA_BACKUP_NAME, backupName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_NAME, member, backupName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateBackupInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.backupName(this.backupName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateBackupInput}.
     */
    public static final class Builder implements ShapeBuilder<CreateBackupInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String backupName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table. You can also provide the Amazon Resource Name (ARN) of the table in this parameter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * Specified name for the backup.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupName(String backupName) {
            this.backupName = Objects.requireNonNull(backupName, "backupName cannot be null");
            tracker.setMember($SCHEMA_BACKUP_NAME);
            return this;
        }

        @Override
        public CreateBackupInput build() {
            tracker.validate();
            return new CreateBackupInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> backupName((String) SchemaUtils.validateSameMember($SCHEMA_BACKUP_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateBackupInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_NAME)) {
                backupName("");
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.backupName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
