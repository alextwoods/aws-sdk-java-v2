package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * The journal table configuration for the S3 Metadata configuration.
 */
@SmithyGenerated
public final class JournalTableConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.JOURNAL_TABLE_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_TABLE_STATUS = $SCHEMA.member("TableStatus");
    private static final Schema $SCHEMA_ERROR = $SCHEMA.member("Error");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_RECORD_EXPIRATION = $SCHEMA.member("RecordExpiration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableStatus;
    private final transient ErrorDetails error;
    private final transient String tableName;
    private final transient String tableArn;
    private final transient RecordExpiration recordExpiration;

    private JournalTableConfigurationResult(Builder builder) {
        this.tableStatus = builder.tableStatus;
        this.error = builder.error;
        this.tableName = builder.tableName;
        this.tableArn = builder.tableArn;
        this.recordExpiration = builder.recordExpiration;
    }

    /**
     * The status of the journal table. The status values are:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The journal table is in the process of being created in the specified table
     *     bucket.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The journal table has been created successfully, and records are being delivered to
     *     the table.
     *   </li>
     *   <li>
     *     <code>FAILED</code> - Amazon S3 is unable to create the journal table, or Amazon S3 is unable to deliver
     *     records.
     *   </li>
     * </ul>
     */
    public String getTableStatus() {
        return tableStatus;
    }

    public ErrorDetails getError() {
        return error;
    }

    /**
     * The name of the journal table.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The Amazon Resource Name (ARN) for the journal table.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * The journal table record expiration settings for the journal table.
     */
    public RecordExpiration getRecordExpiration() {
        return recordExpiration;
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
        JournalTableConfigurationResult that = (JournalTableConfigurationResult) other;
        return Objects.equals(this.tableStatus, that.tableStatus)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.error, that.error)
               && Objects.equals(this.recordExpiration, that.recordExpiration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableStatus);
        $hc = 31 * $hc + Objects.hashCode(error);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(recordExpiration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_STATUS, tableStatus);
        if (error != null) {
            serializer.writeStruct($SCHEMA_ERROR, error);
        }
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (recordExpiration != null) {
            serializer.writeStruct($SCHEMA_RECORD_EXPIRATION, recordExpiration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, tableStatus);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECORD_EXPIRATION, member, recordExpiration);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, error);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link JournalTableConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableStatus(this.tableStatus);
        builder.error(this.error);
        builder.tableName(this.tableName);
        builder.tableArn(this.tableArn);
        builder.recordExpiration(this.recordExpiration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link JournalTableConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<JournalTableConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableStatus;
        private ErrorDetails error;
        private String tableName;
        private String tableArn;
        private RecordExpiration recordExpiration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The status of the journal table. The status values are:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The journal table is in the process of being created in the specified table
         *     bucket.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The journal table has been created successfully, and records are being delivered to
         *     the table.
         *   </li>
         *   <li>
         *     <code>FAILED</code> - Amazon S3 is unable to create the journal table, or Amazon S3 is unable to deliver
         *     records.
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableStatus(String tableStatus) {
            this.tableStatus = Objects.requireNonNull(tableStatus, "tableStatus cannot be null");
            tracker.setMember($SCHEMA_TABLE_STATUS);
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder error(ErrorDetails error) {
            this.error = error;
            return this;
        }

        /**
         * The name of the journal table.
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
         * The Amazon Resource Name (ARN) for the journal table.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * The journal table record expiration settings for the journal table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder recordExpiration(RecordExpiration recordExpiration) {
            this.recordExpiration = Objects.requireNonNull(recordExpiration, "recordExpiration cannot be null");
            tracker.setMember($SCHEMA_RECORD_EXPIRATION);
            return this;
        }

        @Override
        public JournalTableConfigurationResult build() {
            tracker.validate();
            return new JournalTableConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableStatus((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, value));
                case 1 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 2 -> recordExpiration((RecordExpiration) SchemaUtils.validateSameMember($SCHEMA_RECORD_EXPIRATION, member, value));
                case 3 -> error((ErrorDetails) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, value));
                case 4 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<JournalTableConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_STATUS)) {
                tableStatus("");
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_RECORD_EXPIRATION)) {
                tracker.setMember($SCHEMA_RECORD_EXPIRATION);
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
                    case 0 -> builder.tableStatus(de.readString(member));
                    case 1 -> builder.tableName(de.readString(member));
                    case 2 -> builder.recordExpiration(RecordExpiration.builder().deserializeMember(de, member).build());
                    case 3 -> builder.error(ErrorDetails.builder().deserializeMember(de, member).build());
                    case 4 -> builder.tableArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
