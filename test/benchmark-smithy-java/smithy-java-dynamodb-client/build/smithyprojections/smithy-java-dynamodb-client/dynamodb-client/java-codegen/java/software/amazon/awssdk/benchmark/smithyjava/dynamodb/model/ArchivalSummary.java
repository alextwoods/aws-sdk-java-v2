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
 * Contains details of a table archival operation.
 */
@SmithyGenerated
public final class ArchivalSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ARCHIVAL_SUMMARY;
    private static final Schema $SCHEMA_ARCHIVAL_DATE_TIME = $SCHEMA.member("ArchivalDateTime");
    private static final Schema $SCHEMA_ARCHIVAL_REASON = $SCHEMA.member("ArchivalReason");
    private static final Schema $SCHEMA_ARCHIVAL_BACKUP_ARN = $SCHEMA.member("ArchivalBackupArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant archivalDateTime;
    private final transient String archivalReason;
    private final transient String archivalBackupArn;

    private ArchivalSummary(Builder builder) {
        this.archivalDateTime = builder.archivalDateTime;
        this.archivalReason = builder.archivalReason;
        this.archivalBackupArn = builder.archivalBackupArn;
    }

    /**
     * The date and time when table archival was initiated by DynamoDB, in UNIX epoch time format.
     */
    public Instant getArchivalDateTime() {
        return archivalDateTime;
    }

    /**
     * The reason DynamoDB archived the table. Currently, the only possible value is:
     *
     * <ul>
     *   <li>
     *     <code>INACCESSIBLE_ENCRYPTION_CREDENTIALS</code> - The table was archived due to the table's KMS key
     *     being inaccessible for more than seven days. An On-Demand backup was created at the archival time.
     *   </li>
     * </ul>
     */
    public String getArchivalReason() {
        return archivalReason;
    }

    /**
     * The Amazon Resource Name (ARN) of the backup the table was archived to, when applicable in the archival reason.
     * If you wish to restore this backup to the same table name, you will need to delete the original table.
     */
    public String getArchivalBackupArn() {
        return archivalBackupArn;
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
        ArchivalSummary that = (ArchivalSummary) other;
        return Objects.equals(this.archivalReason, that.archivalReason)
               && Objects.equals(this.archivalBackupArn, that.archivalBackupArn)
               && Objects.equals(this.archivalDateTime, that.archivalDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(archivalDateTime);
        $hc = 31 * $hc + Objects.hashCode(archivalReason);
        $hc = 31 * $hc + Objects.hashCode(archivalBackupArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (archivalDateTime != null) {
            serializer.writeTimestamp($SCHEMA_ARCHIVAL_DATE_TIME, archivalDateTime);
        }
        if (archivalReason != null) {
            serializer.writeString($SCHEMA_ARCHIVAL_REASON, archivalReason);
        }
        if (archivalBackupArn != null) {
            serializer.writeString($SCHEMA_ARCHIVAL_BACKUP_ARN, archivalBackupArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_DATE_TIME, member, archivalDateTime);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_REASON, member, archivalReason);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_BACKUP_ARN, member, archivalBackupArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ArchivalSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.archivalDateTime(this.archivalDateTime);
        builder.archivalReason(this.archivalReason);
        builder.archivalBackupArn(this.archivalBackupArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ArchivalSummary}.
     */
    public static final class Builder implements ShapeBuilder<ArchivalSummary> {
        private Instant archivalDateTime;
        private String archivalReason;
        private String archivalBackupArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The date and time when table archival was initiated by DynamoDB, in UNIX epoch time format.
         *
         * @return this builder.
         */
        public Builder archivalDateTime(Instant archivalDateTime) {
            this.archivalDateTime = archivalDateTime;
            return this;
        }

        /**
         * The reason DynamoDB archived the table. Currently, the only possible value is:
         *
         * <ul>
         *   <li>
         *     <code>INACCESSIBLE_ENCRYPTION_CREDENTIALS</code> - The table was archived due to the table's KMS key
         *     being inaccessible for more than seven days. An On-Demand backup was created at the archival time.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder archivalReason(String archivalReason) {
            this.archivalReason = archivalReason;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the backup the table was archived to, when applicable in the archival reason.
         * If you wish to restore this backup to the same table name, you will need to delete the original table.
         *
         * @return this builder.
         */
        public Builder archivalBackupArn(String archivalBackupArn) {
            this.archivalBackupArn = archivalBackupArn;
            return this;
        }

        @Override
        public ArchivalSummary build() {
            return new ArchivalSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> archivalDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_DATE_TIME, member, value));
                case 1 -> archivalReason((String) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_REASON, member, value));
                case 2 -> archivalBackupArn((String) SchemaUtils.validateSameMember($SCHEMA_ARCHIVAL_BACKUP_ARN, member, value));
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
                    case 0 -> builder.archivalDateTime(de.readTimestamp(member));
                    case 1 -> builder.archivalReason(de.readString(member));
                    case 2 -> builder.archivalBackupArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
