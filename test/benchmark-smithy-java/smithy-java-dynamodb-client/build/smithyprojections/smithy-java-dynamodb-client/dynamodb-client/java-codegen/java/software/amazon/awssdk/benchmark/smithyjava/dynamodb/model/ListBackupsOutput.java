package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
public final class ListBackupsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_BACKUPS_OUTPUT;
    private static final Schema $SCHEMA_BACKUP_SUMMARIES = $SCHEMA.member("BackupSummaries");
    private static final Schema $SCHEMA_LAST_EVALUATED_BACKUP_ARN = $SCHEMA.member("LastEvaluatedBackupArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<BackupSummary> backupSummaries;
    private final transient String lastEvaluatedBackupArn;

    private ListBackupsOutput(Builder builder) {
        this.backupSummaries = builder.backupSummaries == null ? null : Collections.unmodifiableList(builder.backupSummaries);
        this.lastEvaluatedBackupArn = builder.lastEvaluatedBackupArn;
    }

    /**
     * List of <code>BackupSummary</code> objects.
     */
    public List<BackupSummary> getBackupSummaries() {
        if (backupSummaries == null) {
            return Collections.emptyList();
        }
        return backupSummaries;
    }

    public boolean hasBackupSummaries() {
        return backupSummaries != null;
    }

    /**
     * The ARN of the backup last evaluated when the current page of results was returned, inclusive of the current
     * page of results. This value may be specified as the <code>ExclusiveStartBackupArn</code> of a new <code>
     * ListBackups</code> operation in order to fetch the next page of results.
     *
     * <p> If <code>LastEvaluatedBackupArn</code> is empty, then the last page of results has been processed and there
     * are no more results to be retrieved.
     *
     * <p> If <code>LastEvaluatedBackupArn</code> is not empty, this may or may not indicate that there is more data to
     * be returned. All results are guaranteed to have been returned if and only if no value for <code>
     * LastEvaluatedBackupArn</code> is returned.
     */
    public String getLastEvaluatedBackupArn() {
        return lastEvaluatedBackupArn;
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
        ListBackupsOutput that = (ListBackupsOutput) other;
        return Objects.equals(this.lastEvaluatedBackupArn, that.lastEvaluatedBackupArn)
               && Objects.equals(this.backupSummaries, that.backupSummaries);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(backupSummaries);
        $hc = 31 * $hc + Objects.hashCode(lastEvaluatedBackupArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (backupSummaries != null) {
            serializer.writeList($SCHEMA_BACKUP_SUMMARIES, backupSummaries, backupSummaries.size(), SharedSerde.BackupSummariesSerializer.INSTANCE);
        }
        if (lastEvaluatedBackupArn != null) {
            serializer.writeString($SCHEMA_LAST_EVALUATED_BACKUP_ARN, lastEvaluatedBackupArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKUP_SUMMARIES, member, backupSummaries);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_BACKUP_ARN, member, lastEvaluatedBackupArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListBackupsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.backupSummaries(this.backupSummaries);
        builder.lastEvaluatedBackupArn(this.lastEvaluatedBackupArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListBackupsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListBackupsOutput> {
        private List<BackupSummary> backupSummaries;
        private String lastEvaluatedBackupArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * List of <code>BackupSummary</code> objects.
         *
         * @return this builder.
         */
        public Builder backupSummaries(List<BackupSummary> backupSummaries) {
            this.backupSummaries = backupSummaries;
            return this;
        }

        /**
         * The ARN of the backup last evaluated when the current page of results was returned, inclusive of the current
         * page of results. This value may be specified as the <code>ExclusiveStartBackupArn</code> of a new <code>
         * ListBackups</code> operation in order to fetch the next page of results.
         *
         * <p> If <code>LastEvaluatedBackupArn</code> is empty, then the last page of results has been processed and there
         * are no more results to be retrieved.
         *
         * <p> If <code>LastEvaluatedBackupArn</code> is not empty, this may or may not indicate that there is more data to
         * be returned. All results are guaranteed to have been returned if and only if no value for <code>
         * LastEvaluatedBackupArn</code> is returned.
         *
         * @return this builder.
         */
        public Builder lastEvaluatedBackupArn(String lastEvaluatedBackupArn) {
            this.lastEvaluatedBackupArn = lastEvaluatedBackupArn;
            return this;
        }

        @Override
        public ListBackupsOutput build() {
            return new ListBackupsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> backupSummaries((List<BackupSummary>) SchemaUtils.validateSameMember($SCHEMA_BACKUP_SUMMARIES, member, value));
                case 1 -> lastEvaluatedBackupArn((String) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_BACKUP_ARN, member, value));
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
                    case 0 -> builder.backupSummaries(SharedSerde.deserializeBackupSummaries(member, de));
                    case 1 -> builder.lastEvaluatedBackupArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
