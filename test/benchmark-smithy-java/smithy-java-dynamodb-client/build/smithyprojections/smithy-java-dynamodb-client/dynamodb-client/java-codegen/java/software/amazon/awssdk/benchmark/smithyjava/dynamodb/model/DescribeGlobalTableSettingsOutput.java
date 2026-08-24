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
public final class DescribeGlobalTableSettingsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_GLOBAL_TABLE_SETTINGS_OUTPUT;
    private static final Schema $SCHEMA_GLOBAL_TABLE_NAME = $SCHEMA.member("GlobalTableName");
    private static final Schema $SCHEMA_REPLICA_SETTINGS = $SCHEMA.member("ReplicaSettings");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String globalTableName;
    private final transient List<ReplicaSettingsDescription> replicaSettings;

    private DescribeGlobalTableSettingsOutput(Builder builder) {
        this.globalTableName = builder.globalTableName;
        this.replicaSettings = builder.replicaSettings == null ? null : Collections.unmodifiableList(builder.replicaSettings);
    }

    /**
     * The name of the global table.
     */
    public String getGlobalTableName() {
        return globalTableName;
    }

    /**
     * The Region-specific settings for the global table.
     */
    public List<ReplicaSettingsDescription> getReplicaSettings() {
        if (replicaSettings == null) {
            return Collections.emptyList();
        }
        return replicaSettings;
    }

    public boolean hasReplicaSettings() {
        return replicaSettings != null;
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
        DescribeGlobalTableSettingsOutput that = (DescribeGlobalTableSettingsOutput) other;
        return Objects.equals(this.globalTableName, that.globalTableName)
               && Objects.equals(this.replicaSettings, that.replicaSettings);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTableName);
        $hc = 31 * $hc + Objects.hashCode(replicaSettings);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (globalTableName != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_NAME, globalTableName);
        }
        if (replicaSettings != null) {
            serializer.writeList($SCHEMA_REPLICA_SETTINGS, replicaSettings, replicaSettings.size(), SharedSerde.ReplicaSettingsDescriptionListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, globalTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_SETTINGS, member, replicaSettings);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeGlobalTableSettingsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTableName(this.globalTableName);
        builder.replicaSettings(this.replicaSettings);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeGlobalTableSettingsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeGlobalTableSettingsOutput> {
        private String globalTableName;
        private List<ReplicaSettingsDescription> replicaSettings;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global table.
         *
         * @return this builder.
         */
        public Builder globalTableName(String globalTableName) {
            this.globalTableName = globalTableName;
            return this;
        }

        /**
         * The Region-specific settings for the global table.
         *
         * @return this builder.
         */
        public Builder replicaSettings(List<ReplicaSettingsDescription> replicaSettings) {
            this.replicaSettings = replicaSettings;
            return this;
        }

        @Override
        public DescribeGlobalTableSettingsOutput build() {
            return new DescribeGlobalTableSettingsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTableName((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, value));
                case 1 -> replicaSettings((List<ReplicaSettingsDescription>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_SETTINGS, member, value));
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
                    case 0 -> builder.globalTableName(de.readString(member));
                    case 1 -> builder.replicaSettings(SharedSerde.deserializeReplicaSettingsDescriptionList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
