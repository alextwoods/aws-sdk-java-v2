package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * Container for grant information.
 */
@SmithyGenerated
public final class Grant implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GRANT;
    private static final Schema $SCHEMA_GRANTEE = $SCHEMA.member("Grantee");
    private static final Schema $SCHEMA_PERMISSION = $SCHEMA.member("Permission");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Grantee grantee;
    private final transient Permission permission;

    private Grant(Builder builder) {
        this.grantee = builder.grantee;
        this.permission = builder.permission;
    }

    /**
     * The person being granted permissions.
     */
    public Grantee getGrantee() {
        return grantee;
    }

    /**
     * Specifies the permission given to the grantee.
     */
    public Permission getPermission() {
        return permission;
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
        Grant that = (Grant) other;
        return Objects.equals(this.permission, that.permission)
               && Objects.equals(this.grantee, that.grantee);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(grantee);
        $hc = 31 * $hc + Objects.hashCode(permission);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (grantee != null) {
            serializer.writeStruct($SCHEMA_GRANTEE, grantee);
        }
        if (permission != null) {
            serializer.writeString($SCHEMA_PERMISSION, permission.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANTEE, member, grantee);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERMISSION, member, permission);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Grant}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.grantee(this.grantee);
        builder.permission(this.permission);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Grant}.
     */
    public static final class Builder implements ShapeBuilder<Grant> {
        private Grantee grantee;
        private Permission permission;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The person being granted permissions.
         *
         * @return this builder.
         */
        public Builder grantee(Grantee grantee) {
            this.grantee = grantee;
            return this;
        }

        /**
         * Specifies the permission given to the grantee.
         *
         * @return this builder.
         */
        public Builder permission(Permission permission) {
            this.permission = permission;
            return this;
        }

        @Override
        public Grant build() {
            return new Grant(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> grantee((Grantee) SchemaUtils.validateSameMember($SCHEMA_GRANTEE, member, value));
                case 1 -> permission((Permission) SchemaUtils.validateSameMember($SCHEMA_PERMISSION, member, value));
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
                    case 0 -> builder.grantee(Grantee.builder().deserializeMember(de, member).build());
                    case 1 -> builder.permission(Permission.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
