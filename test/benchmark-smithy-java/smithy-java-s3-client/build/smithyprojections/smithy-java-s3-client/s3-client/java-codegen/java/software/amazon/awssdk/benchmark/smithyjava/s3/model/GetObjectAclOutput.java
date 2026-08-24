package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public final class GetObjectAclOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_OBJECT_ACL_OUTPUT;
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_GRANTS = $SCHEMA.member("Grants");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Owner owner;
    private final transient List<Grant> grants;
    private final transient RequestCharged requestCharged;

    private GetObjectAclOutput(Builder builder) {
        this.owner = builder.owner;
        this.grants = builder.grants == null ? null : Collections.unmodifiableList(builder.grants);
        this.requestCharged = builder.requestCharged;
    }

    /**
     * Container for the bucket owner's ID.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * A list of grants.
     */
    public List<Grant> getGrants() {
        if (grants == null) {
            return Collections.emptyList();
        }
        return grants;
    }

    public boolean hasGrants() {
        return grants != null;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
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
        GetObjectAclOutput that = (GetObjectAclOutput) other;
        return Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.owner, that.owner)
               && Objects.equals(this.grants, that.grants);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(grants);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (owner != null) {
            serializer.writeStruct($SCHEMA_OWNER, owner);
        }
        if (grants != null) {
            serializer.writeList($SCHEMA_GRANTS, grants, grants.size(), SharedSerde.GrantsSerializer.INSTANCE);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANTS, member, grants);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetObjectAclOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.owner(this.owner);
        builder.grants(this.grants);
        builder.requestCharged(this.requestCharged);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetObjectAclOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetObjectAclOutput> {
        private Owner owner;
        private List<Grant> grants;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Container for the bucket owner's ID.
         *
         * @return this builder.
         */
        public Builder owner(Owner owner) {
            this.owner = owner;
            return this;
        }

        /**
         * A list of grants.
         *
         * @return this builder.
         */
        public Builder grants(List<Grant> grants) {
            this.grants = grants;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        @Override
        public GetObjectAclOutput build() {
            return new GetObjectAclOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 1 -> grants((List<Grant>) SchemaUtils.validateSameMember($SCHEMA_GRANTS, member, value));
                case 2 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 1 -> builder.grants(SharedSerde.deserializeGrants(member, de));
                    case 2 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
