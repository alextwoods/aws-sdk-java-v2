package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Specifies the action to remove a witness Region from a MRSC global table. You cannot delete a single witness from a
 * MRSC global table - you must delete both a replica and the witness together. The deletion of both a witness and
 * replica converts the remaining replica to a single-Region DynamoDB table.
 */
@SmithyGenerated
public final class DeleteGlobalTableWitnessGroupMemberAction implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_GLOBAL_TABLE_WITNESS_GROUP_MEMBER_ACTION;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;

    private DeleteGlobalTableWitnessGroupMemberAction(Builder builder) {
        this.regionName = builder.regionName;
    }

    /**
     * The witness Region name to be removed from the MRSC global table.
     */
    public String getRegionName() {
        return regionName;
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
        DeleteGlobalTableWitnessGroupMemberAction that = (DeleteGlobalTableWitnessGroupMemberAction) other;
        return Objects.equals(this.regionName, that.regionName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_REGION_NAME, regionName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteGlobalTableWitnessGroupMemberAction}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteGlobalTableWitnessGroupMemberAction}.
     */
    public static final class Builder implements ShapeBuilder<DeleteGlobalTableWitnessGroupMemberAction> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String regionName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The witness Region name to be removed from the MRSC global table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder regionName(String regionName) {
            this.regionName = Objects.requireNonNull(regionName, "regionName cannot be null");
            tracker.setMember($SCHEMA_REGION_NAME);
            return this;
        }

        @Override
        public DeleteGlobalTableWitnessGroupMemberAction build() {
            tracker.validate();
            return new DeleteGlobalTableWitnessGroupMemberAction(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteGlobalTableWitnessGroupMemberAction> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_REGION_NAME)) {
                regionName("");
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
                    case 0 -> builder.regionName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
