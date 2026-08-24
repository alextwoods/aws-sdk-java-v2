package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
public final class UpdateGlobalTableInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_GLOBAL_TABLE_INPUT;
    private static final Schema $SCHEMA_GLOBAL_TABLE_NAME = $SCHEMA.member("GlobalTableName");
    private static final Schema $SCHEMA_REPLICA_UPDATES = $SCHEMA.member("ReplicaUpdates");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String globalTableName;
    private final transient List<ReplicaUpdate> replicaUpdates;

    private UpdateGlobalTableInput(Builder builder) {
        this.globalTableName = builder.globalTableName;
        this.replicaUpdates = Collections.unmodifiableList(builder.replicaUpdates);
    }

    /**
     * The global table name.
     */
    public String getGlobalTableName() {
        return globalTableName;
    }

    /**
     * A list of Regions that should be added or removed from the global table.
     */
    public List<ReplicaUpdate> getReplicaUpdates() {
        return replicaUpdates;
    }

    public boolean hasReplicaUpdates() {
        return true;
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
        UpdateGlobalTableInput that = (UpdateGlobalTableInput) other;
        return Objects.equals(this.globalTableName, that.globalTableName)
               && Objects.equals(this.replicaUpdates, that.replicaUpdates);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTableName);
        $hc = 31 * $hc + Objects.hashCode(replicaUpdates);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_GLOBAL_TABLE_NAME, globalTableName);
        serializer.writeList($SCHEMA_REPLICA_UPDATES, replicaUpdates, replicaUpdates.size(), SharedSerde.ReplicaUpdateListSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, globalTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_UPDATES, member, replicaUpdates);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateGlobalTableInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTableName(this.globalTableName);
        builder.replicaUpdates(this.replicaUpdates);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateGlobalTableInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateGlobalTableInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String globalTableName;
        private List<ReplicaUpdate> replicaUpdates;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The global table name.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder globalTableName(String globalTableName) {
            this.globalTableName = Objects.requireNonNull(globalTableName, "globalTableName cannot be null");
            tracker.setMember($SCHEMA_GLOBAL_TABLE_NAME);
            return this;
        }

        /**
         * A list of Regions that should be added or removed from the global table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder replicaUpdates(List<ReplicaUpdate> replicaUpdates) {
            this.replicaUpdates = Objects.requireNonNull(replicaUpdates, "replicaUpdates cannot be null");
            tracker.setMember($SCHEMA_REPLICA_UPDATES);
            return this;
        }

        @Override
        public UpdateGlobalTableInput build() {
            tracker.validate();
            return new UpdateGlobalTableInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTableName((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, value));
                case 1 -> replicaUpdates((List<ReplicaUpdate>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_UPDATES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateGlobalTableInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_GLOBAL_TABLE_NAME)) {
                globalTableName("");
            }
            if (!tracker.checkMember($SCHEMA_REPLICA_UPDATES)) {
                replicaUpdates(Collections.emptyList());
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
                    case 0 -> builder.globalTableName(de.readString(member));
                    case 1 -> builder.replicaUpdates(SharedSerde.deserializeReplicaUpdateList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
