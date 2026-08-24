package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * Contains details about the global table.
 */
@SmithyGenerated
public final class GlobalTableDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_TABLE_DESCRIPTION;
    private static final Schema $SCHEMA_REPLICATION_GROUP = $SCHEMA.member("ReplicationGroup");
    private static final Schema $SCHEMA_GLOBAL_TABLE_ARN = $SCHEMA.member("GlobalTableArn");
    private static final Schema $SCHEMA_CREATION_DATE_TIME = $SCHEMA.member("CreationDateTime");
    private static final Schema $SCHEMA_GLOBAL_TABLE_STATUS = $SCHEMA.member("GlobalTableStatus");
    private static final Schema $SCHEMA_GLOBAL_TABLE_NAME = $SCHEMA.member("GlobalTableName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ReplicaDescription> replicationGroup;
    private final transient String globalTableArn;
    private final transient Instant creationDateTime;
    private final transient GlobalTableStatus globalTableStatus;
    private final transient String globalTableName;

    private GlobalTableDescription(Builder builder) {
        this.replicationGroup = builder.replicationGroup == null ? null : Collections.unmodifiableList(builder.replicationGroup);
        this.globalTableArn = builder.globalTableArn;
        this.creationDateTime = builder.creationDateTime;
        this.globalTableStatus = builder.globalTableStatus;
        this.globalTableName = builder.globalTableName;
    }

    /**
     * The Regions where the global table has replicas.
     */
    public List<ReplicaDescription> getReplicationGroup() {
        if (replicationGroup == null) {
            return Collections.emptyList();
        }
        return replicationGroup;
    }

    public boolean hasReplicationGroup() {
        return replicationGroup != null;
    }

    /**
     * The unique identifier of the global table.
     */
    public String getGlobalTableArn() {
        return globalTableArn;
    }

    /**
     * The creation time of the global table.
     */
    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    /**
     * The current state of the global table:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The global table is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The global table is being updated.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The global table is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The global table is ready for use.
     *   </li>
     * </ul>
     */
    public GlobalTableStatus getGlobalTableStatus() {
        return globalTableStatus;
    }

    /**
     * The global table name.
     */
    public String getGlobalTableName() {
        return globalTableName;
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
        GlobalTableDescription that = (GlobalTableDescription) other;
        return Objects.equals(this.globalTableArn, that.globalTableArn)
               && Objects.equals(this.globalTableName, that.globalTableName)
               && Objects.equals(this.globalTableStatus, that.globalTableStatus)
               && Objects.equals(this.creationDateTime, that.creationDateTime)
               && Objects.equals(this.replicationGroup, that.replicationGroup);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(replicationGroup);
        $hc = 31 * $hc + Objects.hashCode(globalTableArn);
        $hc = 31 * $hc + Objects.hashCode(creationDateTime);
        $hc = 31 * $hc + Objects.hashCode(globalTableStatus);
        $hc = 31 * $hc + Objects.hashCode(globalTableName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (replicationGroup != null) {
            serializer.writeList($SCHEMA_REPLICATION_GROUP, replicationGroup, replicationGroup.size(), SharedSerde.ReplicaDescriptionListSerializer.INSTANCE);
        }
        if (globalTableArn != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_ARN, globalTableArn);
        }
        if (creationDateTime != null) {
            serializer.writeTimestamp($SCHEMA_CREATION_DATE_TIME, creationDateTime);
        }
        if (globalTableStatus != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_STATUS, globalTableStatus.getValue());
        }
        if (globalTableName != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_NAME, globalTableName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_GROUP, member, replicationGroup);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_ARN, member, globalTableArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE_TIME, member, creationDateTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_STATUS, member, globalTableStatus);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, globalTableName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalTableDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.replicationGroup(this.replicationGroup);
        builder.globalTableArn(this.globalTableArn);
        builder.creationDateTime(this.creationDateTime);
        builder.globalTableStatus(this.globalTableStatus);
        builder.globalTableName(this.globalTableName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalTableDescription}.
     */
    public static final class Builder implements ShapeBuilder<GlobalTableDescription> {
        private List<ReplicaDescription> replicationGroup;
        private String globalTableArn;
        private Instant creationDateTime;
        private GlobalTableStatus globalTableStatus;
        private String globalTableName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Regions where the global table has replicas.
         *
         * @return this builder.
         */
        public Builder replicationGroup(List<ReplicaDescription> replicationGroup) {
            this.replicationGroup = replicationGroup;
            return this;
        }

        /**
         * The unique identifier of the global table.
         *
         * @return this builder.
         */
        public Builder globalTableArn(String globalTableArn) {
            this.globalTableArn = globalTableArn;
            return this;
        }

        /**
         * The creation time of the global table.
         *
         * @return this builder.
         */
        public Builder creationDateTime(Instant creationDateTime) {
            this.creationDateTime = creationDateTime;
            return this;
        }

        /**
         * The current state of the global table:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The global table is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The global table is being updated.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The global table is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The global table is ready for use.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder globalTableStatus(GlobalTableStatus globalTableStatus) {
            this.globalTableStatus = globalTableStatus;
            return this;
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

        @Override
        public GlobalTableDescription build() {
            return new GlobalTableDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> replicationGroup((List<ReplicaDescription>) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_GROUP, member, value));
                case 1 -> globalTableArn((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_ARN, member, value));
                case 2 -> creationDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE_TIME, member, value));
                case 3 -> globalTableStatus((GlobalTableStatus) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_STATUS, member, value));
                case 4 -> globalTableName((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, value));
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
                    case 0 -> builder.replicationGroup(SharedSerde.deserializeReplicaDescriptionList(member, de));
                    case 1 -> builder.globalTableArn(de.readString(member));
                    case 2 -> builder.creationDateTime(de.readTimestamp(member));
                    case 3 -> builder.globalTableStatus(GlobalTableStatus.builder().deserializeMember(de, member).build());
                    case 4 -> builder.globalTableName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
