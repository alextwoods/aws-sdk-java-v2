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

@SmithyGenerated
public final class CreateBackupOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_BACKUP_OUTPUT;
    private static final Schema $SCHEMA_BACKUP_DETAILS = $SCHEMA.member("BackupDetails");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BackupDetails backupDetails;

    private CreateBackupOutput(Builder builder) {
        this.backupDetails = builder.backupDetails;
    }

    /**
     * Contains the details of the backup created for the table.
     */
    public BackupDetails getBackupDetails() {
        return backupDetails;
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
        CreateBackupOutput that = (CreateBackupOutput) other;
        return Objects.equals(this.backupDetails, that.backupDetails);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(backupDetails);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_DETAILS, member, backupDetails);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateBackupOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.backupDetails(this.backupDetails);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateBackupOutput}.
     */
    public static final class Builder implements ShapeBuilder<CreateBackupOutput> {
        private BackupDetails backupDetails;

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

        @Override
        public CreateBackupOutput build() {
            return new CreateBackupOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> backupDetails((BackupDetails) SchemaUtils.validateSameMember($SCHEMA_BACKUP_DETAILS, member, value));
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
