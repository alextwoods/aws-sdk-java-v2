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
 * The inventory table configuration for an S3 Metadata configuration.
 */
@SmithyGenerated
public final class InventoryTableConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_TABLE_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_CONFIGURATION_STATE = $SCHEMA.member("ConfigurationState");
    private static final Schema $SCHEMA_TABLE_STATUS = $SCHEMA.member("TableStatus");
    private static final Schema $SCHEMA_ERROR = $SCHEMA.member("Error");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient InventoryConfigurationState configurationState;
    private final transient String tableStatus;
    private final transient ErrorDetails error;
    private final transient String tableName;
    private final transient String tableArn;

    private InventoryTableConfigurationResult(Builder builder) {
        this.configurationState = builder.configurationState;
        this.tableStatus = builder.tableStatus;
        this.error = builder.error;
        this.tableName = builder.tableName;
        this.tableArn = builder.tableArn;
    }

    /**
     * The configuration state of the inventory table, indicating whether the inventory table is enabled or disabled.
     */
    public InventoryConfigurationState getConfigurationState() {
        return configurationState;
    }

    /**
     * The status of the inventory table. The status values are:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The inventory table is in the process of being created in the specified Amazon
     *     Web Services managed table bucket.
     *   </li>
     *   <li>
     *     <code>BACKFILLING</code> - The inventory table is in the process of being backfilled. When you enable the
     *     inventory table for your metadata configuration, the table goes through a process known as backfilling,
     *     during which Amazon S3 scans your general purpose bucket to retrieve the initial metadata for all objects
     *     in the bucket. Depending on the number of objects in your bucket, this process can take several hours.
     *     When the backfilling process is finished, the status of your inventory table changes from <code>
     *     BACKFILLING</code> to <code>ACTIVE</code>. After backfilling is completed, updates to your objects are
     *     reflected in the inventory table within one hour.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The inventory table has been created successfully, and records are being delivered
     *     to the table.
     *   </li>
     *   <li>
     *     <code>FAILED</code> - Amazon S3 is unable to create the inventory table, or Amazon S3 is unable to
     *     deliver records.
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
     * The name of the inventory table.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The Amazon Resource Name (ARN) for the inventory table.
     */
    public String getTableArn() {
        return tableArn;
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
        InventoryTableConfigurationResult that = (InventoryTableConfigurationResult) other;
        return Objects.equals(this.tableStatus, that.tableStatus)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.configurationState, that.configurationState)
               && Objects.equals(this.error, that.error);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(configurationState);
        $hc = 31 * $hc + Objects.hashCode(tableStatus);
        $hc = 31 * $hc + Objects.hashCode(error);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(tableArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_CONFIGURATION_STATE, configurationState.getValue());
        if (tableStatus != null) {
            serializer.writeString($SCHEMA_TABLE_STATUS, tableStatus);
        }
        if (error != null) {
            serializer.writeStruct($SCHEMA_ERROR, error);
        }
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION_STATE, member, configurationState);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, tableStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, error);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryTableConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.configurationState(this.configurationState);
        builder.tableStatus(this.tableStatus);
        builder.error(this.error);
        builder.tableName(this.tableName);
        builder.tableArn(this.tableArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryTableConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<InventoryTableConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private InventoryConfigurationState configurationState;
        private String tableStatus;
        private ErrorDetails error;
        private String tableName;
        private String tableArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The configuration state of the inventory table, indicating whether the inventory table is enabled or disabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder configurationState(InventoryConfigurationState configurationState) {
            this.configurationState = Objects.requireNonNull(configurationState, "configurationState cannot be null");
            tracker.setMember($SCHEMA_CONFIGURATION_STATE);
            return this;
        }

        /**
         * The status of the inventory table. The status values are:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The inventory table is in the process of being created in the specified Amazon
         *     Web Services managed table bucket.
         *   </li>
         *   <li>
         *     <code>BACKFILLING</code> - The inventory table is in the process of being backfilled. When you enable the
         *     inventory table for your metadata configuration, the table goes through a process known as backfilling,
         *     during which Amazon S3 scans your general purpose bucket to retrieve the initial metadata for all objects
         *     in the bucket. Depending on the number of objects in your bucket, this process can take several hours.
         *     When the backfilling process is finished, the status of your inventory table changes from <code>
         *     BACKFILLING</code> to <code>ACTIVE</code>. After backfilling is completed, updates to your objects are
         *     reflected in the inventory table within one hour.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The inventory table has been created successfully, and records are being delivered
         *     to the table.
         *   </li>
         *   <li>
         *     <code>FAILED</code> - Amazon S3 is unable to create the inventory table, or Amazon S3 is unable to
         *     deliver records.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder tableStatus(String tableStatus) {
            this.tableStatus = tableStatus;
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
         * The name of the inventory table.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) for the inventory table.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        @Override
        public InventoryTableConfigurationResult build() {
            tracker.validate();
            return new InventoryTableConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> configurationState((InventoryConfigurationState) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION_STATE, member, value));
                case 1 -> tableStatus((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, value));
                case 2 -> error((ErrorDetails) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, value));
                case 3 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 4 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventoryTableConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_CONFIGURATION_STATE)) {
                configurationState(InventoryConfigurationState.unknown(""));
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
                    case 0 -> builder.configurationState(InventoryConfigurationState.builder().deserializeMember(de, member).build());
                    case 1 -> builder.tableStatus(de.readString(member));
                    case 2 -> builder.error(ErrorDetails.builder().deserializeMember(de, member).build());
                    case 3 -> builder.tableName(de.readString(member));
                    case 4 -> builder.tableArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
