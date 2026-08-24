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
 * Contains the current state of the annotation table associated with a bucket's Amazon S3 Metadata configuration,
 * including its provisioning status and identifiers.
 */
@SmithyGenerated
public final class AnnotationTableConfigurationResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.ANNOTATION_TABLE_CONFIGURATION_RESULT;
    private static final Schema $SCHEMA_CONFIGURATION_STATE = $SCHEMA.member("ConfigurationState");
    private static final Schema $SCHEMA_TABLE_STATUS = $SCHEMA.member("TableStatus");
    private static final Schema $SCHEMA_ERROR = $SCHEMA.member("Error");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_ROLE = $SCHEMA.member("Role");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient AnnotationConfigurationState configurationState;
    private final transient String tableStatus;
    private final transient ErrorDetails error;
    private final transient String tableName;
    private final transient String tableArn;
    private final transient String role;

    private AnnotationTableConfigurationResult(Builder builder) {
        this.configurationState = builder.configurationState;
        this.tableStatus = builder.tableStatus;
        this.error = builder.error;
        this.tableName = builder.tableName;
        this.tableArn = builder.tableArn;
        this.role = builder.role;
    }

    /**
     * The current configuration state of the annotation table.
     */
    public AnnotationConfigurationState getConfigurationState() {
        return configurationState;
    }

    /**
     * The provisioning status of the annotation table. Possible values: <code>CREATING</code>,
     * <code>BACKFILLING</code>, <code>ACTIVE</code>, <code>FAILED</code>.
     */
    public String getTableStatus() {
        return tableStatus;
    }

    public ErrorDetails getError() {
        return error;
    }

    /**
     * The name of the annotation table.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The ARN of the annotation table.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * The ARN of the IAM role associated with the annotation table.
     */
    public String getRole() {
        return role;
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
        AnnotationTableConfigurationResult that = (AnnotationTableConfigurationResult) other;
        return Objects.equals(this.tableStatus, that.tableStatus)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.role, that.role)
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
        $hc = 31 * $hc + Objects.hashCode(role);
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
        if (role != null) {
            serializer.writeString($SCHEMA_ROLE, role);
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
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_ROLE, member, role);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnnotationTableConfigurationResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.configurationState(this.configurationState);
        builder.tableStatus(this.tableStatus);
        builder.error(this.error);
        builder.tableName(this.tableName);
        builder.tableArn(this.tableArn);
        builder.role(this.role);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AnnotationTableConfigurationResult}.
     */
    public static final class Builder implements ShapeBuilder<AnnotationTableConfigurationResult> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private AnnotationConfigurationState configurationState;
        private String tableStatus;
        private ErrorDetails error;
        private String tableName;
        private String tableArn;
        private String role;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The current configuration state of the annotation table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder configurationState(AnnotationConfigurationState configurationState) {
            this.configurationState = Objects.requireNonNull(configurationState, "configurationState cannot be null");
            tracker.setMember($SCHEMA_CONFIGURATION_STATE);
            return this;
        }

        /**
         * The provisioning status of the annotation table. Possible values: <code>CREATING</code>,
         * <code>BACKFILLING</code>, <code>ACTIVE</code>, <code>FAILED</code>.
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
         * The name of the annotation table.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The ARN of the annotation table.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * The ARN of the IAM role associated with the annotation table.
         *
         * @return this builder.
         */
        public Builder role(String role) {
            this.role = role;
            return this;
        }

        @Override
        public AnnotationTableConfigurationResult build() {
            tracker.validate();
            return new AnnotationTableConfigurationResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> configurationState((AnnotationConfigurationState) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION_STATE, member, value));
                case 1 -> tableStatus((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, value));
                case 2 -> error((ErrorDetails) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, value));
                case 3 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 4 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 5 -> role((String) SchemaUtils.validateSameMember($SCHEMA_ROLE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AnnotationTableConfigurationResult> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_CONFIGURATION_STATE)) {
                configurationState(AnnotationConfigurationState.unknown(""));
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
                    case 0 -> builder.configurationState(AnnotationConfigurationState.builder().deserializeMember(de, member).build());
                    case 1 -> builder.tableStatus(de.readString(member));
                    case 2 -> builder.error(ErrorDetails.builder().deserializeMember(de, member).build());
                    case 3 -> builder.tableName(de.readString(member));
                    case 4 -> builder.tableArn(de.readString(member));
                    case 5 -> builder.role(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
