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
 * Specifies the configuration for the annotation table associated with a bucket's Amazon S3 Metadata configuration. The
 * annotation table is an Iceberg table that records annotation events for objects in the bucket.
 */
@SmithyGenerated
public final class AnnotationTableConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ANNOTATION_TABLE_CONFIGURATION;
    private static final Schema $SCHEMA_CONFIGURATION_STATE = $SCHEMA.member("ConfigurationState");
    private static final Schema $SCHEMA_ENCRYPTION_CONFIGURATION = $SCHEMA.member("EncryptionConfiguration");
    private static final Schema $SCHEMA_ROLE = $SCHEMA.member("Role");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient AnnotationConfigurationState configurationState;
    private final transient MetadataTableEncryptionConfiguration encryptionConfiguration;
    private final transient String role;

    private AnnotationTableConfiguration(Builder builder) {
        this.configurationState = builder.configurationState;
        this.encryptionConfiguration = builder.encryptionConfiguration;
        this.role = builder.role;
    }

    /**
     * The state of the annotation table. Valid values are <code>ENABLED</code> and <code>DISABLED</code>.
     */
    public AnnotationConfigurationState getConfigurationState() {
        return configurationState;
    }

    public MetadataTableEncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
    }

    /**
     * The ARN of the IAM role used to manage the annotation table.
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
        AnnotationTableConfiguration that = (AnnotationTableConfiguration) other;
        return Objects.equals(this.role, that.role)
               && Objects.equals(this.configurationState, that.configurationState)
               && Objects.equals(this.encryptionConfiguration, that.encryptionConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(configurationState);
        $hc = 31 * $hc + Objects.hashCode(encryptionConfiguration);
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
        if (encryptionConfiguration != null) {
            serializer.writeStruct($SCHEMA_ENCRYPTION_CONFIGURATION, encryptionConfiguration);
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
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, encryptionConfiguration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ROLE, member, role);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnnotationTableConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.configurationState(this.configurationState);
        builder.encryptionConfiguration(this.encryptionConfiguration);
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
     * Builder for {@link AnnotationTableConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<AnnotationTableConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private AnnotationConfigurationState configurationState;
        private MetadataTableEncryptionConfiguration encryptionConfiguration;
        private String role;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The state of the annotation table. Valid values are <code>ENABLED</code> and <code>DISABLED</code>.
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
         * @return this builder.
         */
        public Builder encryptionConfiguration(MetadataTableEncryptionConfiguration encryptionConfiguration) {
            this.encryptionConfiguration = encryptionConfiguration;
            return this;
        }

        /**
         * The ARN of the IAM role used to manage the annotation table.
         *
         * @return this builder.
         */
        public Builder role(String role) {
            this.role = role;
            return this;
        }

        @Override
        public AnnotationTableConfiguration build() {
            tracker.validate();
            return new AnnotationTableConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> configurationState((AnnotationConfigurationState) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION_STATE, member, value));
                case 1 -> encryptionConfiguration((MetadataTableEncryptionConfiguration) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, value));
                case 2 -> role((String) SchemaUtils.validateSameMember($SCHEMA_ROLE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AnnotationTableConfiguration> errorCorrection() {
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
                    case 1 -> builder.encryptionConfiguration(MetadataTableEncryptionConfiguration.builder().deserializeMember(de, member).build());
                    case 2 -> builder.role(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
