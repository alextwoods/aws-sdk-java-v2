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

/**
 * Represents the properties of a global table.
 */
@SmithyGenerated
public final class GlobalTable implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_TABLE;
    private static final Schema $SCHEMA_GLOBAL_TABLE_NAME = $SCHEMA.member("GlobalTableName");
    private static final Schema $SCHEMA_REPLICATION_GROUP = $SCHEMA.member("ReplicationGroup");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String globalTableName;
    private final transient List<Replica> replicationGroup;

    private GlobalTable(Builder builder) {
        this.globalTableName = builder.globalTableName;
        this.replicationGroup = builder.replicationGroup == null ? null : Collections.unmodifiableList(builder.replicationGroup);
    }

    /**
     * The global table name.
     */
    public String getGlobalTableName() {
        return globalTableName;
    }

    /**
     * The Regions where the global table has replicas.
     */
    public List<Replica> getReplicationGroup() {
        if (replicationGroup == null) {
            return Collections.emptyList();
        }
        return replicationGroup;
    }

    public boolean hasReplicationGroup() {
        return replicationGroup != null;
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
        GlobalTable that = (GlobalTable) other;
        return Objects.equals(this.globalTableName, that.globalTableName)
               && Objects.equals(this.replicationGroup, that.replicationGroup);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTableName);
        $hc = 31 * $hc + Objects.hashCode(replicationGroup);
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
        if (replicationGroup != null) {
            serializer.writeList($SCHEMA_REPLICATION_GROUP, replicationGroup, replicationGroup.size(), SharedSerde.ReplicaListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, globalTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_GROUP, member, replicationGroup);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalTable}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTableName(this.globalTableName);
        builder.replicationGroup(this.replicationGroup);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalTable}.
     */
    public static final class Builder implements ShapeBuilder<GlobalTable> {
        private String globalTableName;
        private List<Replica> replicationGroup;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The global table name.
         *
         * @return this builder.
         */
        public Builder globalTableName(String globalTableName) {
            this.globalTableName = globalTableName;
            return this;
        }

        /**
         * The Regions where the global table has replicas.
         *
         * @return this builder.
         */
        public Builder replicationGroup(List<Replica> replicationGroup) {
            this.replicationGroup = replicationGroup;
            return this;
        }

        @Override
        public GlobalTable build() {
            return new GlobalTable(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTableName((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, value));
                case 1 -> replicationGroup((List<Replica>) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_GROUP, member, value));
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
                    case 1 -> builder.replicationGroup(SharedSerde.deserializeReplicaList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
