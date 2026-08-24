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
public final class DeleteBackupInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_BACKUP_INPUT;
    private static final Schema $SCHEMA_BACKUP_ARN = $SCHEMA.member("BackupArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String backupArn;

    private DeleteBackupInput(Builder builder) {
        this.backupArn = builder.backupArn;
    }

    /**
     * The ARN associated with the backup.
     */
    public String getBackupArn() {
        return backupArn;
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
        DeleteBackupInput that = (DeleteBackupInput) other;
        return Objects.equals(this.backupArn, that.backupArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(backupArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BACKUP_ARN, backupArn);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_ARN, member, backupArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteBackupInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.backupArn(this.backupArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteBackupInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteBackupInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String backupArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ARN associated with the backup.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder backupArn(String backupArn) {
            this.backupArn = Objects.requireNonNull(backupArn, "backupArn cannot be null");
            tracker.setMember($SCHEMA_BACKUP_ARN);
            return this;
        }

        @Override
        public DeleteBackupInput build() {
            tracker.validate();
            return new DeleteBackupInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> backupArn((String) SchemaUtils.validateSameMember($SCHEMA_BACKUP_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteBackupInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BACKUP_ARN)) {
                backupArn("");
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
