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
 * The specified updates to the S3 Metadata inventory table configuration.
 */
@SmithyGenerated
public final class InventoryTableConfigurationUpdates implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.INVENTORY_TABLE_CONFIGURATION_UPDATES;
    private static final Schema $SCHEMA_CONFIGURATION_STATE = $SCHEMA.member("ConfigurationState");
    private static final Schema $SCHEMA_ENCRYPTION_CONFIGURATION = $SCHEMA.member("EncryptionConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient InventoryConfigurationState configurationState;
    private final transient MetadataTableEncryptionConfiguration encryptionConfiguration;

    private InventoryTableConfigurationUpdates(Builder builder) {
        this.configurationState = builder.configurationState;
        this.encryptionConfiguration = builder.encryptionConfiguration;
    }

    /**
     * The configuration state of the inventory table, indicating whether the inventory table is enabled or disabled.
     */
    public InventoryConfigurationState getConfigurationState() {
        return configurationState;
    }

    /**
     * The encryption configuration for the inventory table.
     */
    public MetadataTableEncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
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
        InventoryTableConfigurationUpdates that = (InventoryTableConfigurationUpdates) other;
        return Objects.equals(this.configurationState, that.configurationState)
               && Objects.equals(this.encryptionConfiguration, that.encryptionConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(configurationState);
        $hc = 31 * $hc + Objects.hashCode(encryptionConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_CONFIGURATION_STATE, configurationState.getValue());
        if (encryptionConfiguration != null) {
            serializer.writeStruct($SCHEMA_ENCRYPTION_CONFIGURATION, encryptionConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION_STATE, member, configurationState);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, encryptionConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryTableConfigurationUpdates}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.configurationState(this.configurationState);
        builder.encryptionConfiguration(this.encryptionConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryTableConfigurationUpdates}.
     */
    public static final class Builder implements ShapeBuilder<InventoryTableConfigurationUpdates> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private InventoryConfigurationState configurationState;
        private MetadataTableEncryptionConfiguration encryptionConfiguration;

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
         * The encryption configuration for the inventory table.
         *
         * @return this builder.
         */
        public Builder encryptionConfiguration(MetadataTableEncryptionConfiguration encryptionConfiguration) {
            this.encryptionConfiguration = encryptionConfiguration;
            return this;
        }

        @Override
        public InventoryTableConfigurationUpdates build() {
            tracker.validate();
            return new InventoryTableConfigurationUpdates(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> configurationState((InventoryConfigurationState) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION_STATE, member, value));
                case 1 -> encryptionConfiguration((MetadataTableEncryptionConfiguration) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventoryTableConfigurationUpdates> errorCorrection() {
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
                    case 1 -> builder.encryptionConfiguration(MetadataTableEncryptionConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
