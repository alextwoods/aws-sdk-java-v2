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
public final class GetBucketAclOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.GET_BUCKET_ACL_OUTPUT;
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_GRANTS = $SCHEMA.member("Grants");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Owner owner;
    private final transient List<Grant> grants;

    private GetBucketAclOutput(Builder builder) {
        this.owner = builder.owner;
        this.grants = builder.grants == null ? null : Collections.unmodifiableList(builder.grants);
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
        GetBucketAclOutput that = (GetBucketAclOutput) other;
        return Objects.equals(this.owner, that.owner)
               && Objects.equals(this.grants, that.grants);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(grants);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANTS, member, grants);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketAclOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.owner(this.owner);
        builder.grants(this.grants);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketAclOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketAclOutput> {
        private Owner owner;
        private List<Grant> grants;

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

        @Override
        public GetBucketAclOutput build() {
            return new GetBucketAclOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 1 -> grants((List<Grant>) SchemaUtils.validateSameMember($SCHEMA_GRANTS, member, value));
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
