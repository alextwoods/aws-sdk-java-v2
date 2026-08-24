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
 * Container for the owner's display name and ID.
 */
@SmithyGenerated
public final class Owner implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.OWNER;
    private static final Schema $SCHEMA_DISPLAY_NAME = $SCHEMA.member("DisplayName");
    private static final Schema $SCHEMA_ID = $SCHEMA.member("ID");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String displayName;
    private final transient String id;

    private Owner(Builder builder) {
        this.displayName = builder.displayName;
        this.id = builder.id;
    }

    /**
     *
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Container for the ID of the owner.
     */
    public String getId() {
        return id;
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
        Owner that = (Owner) other;
        return Objects.equals(this.displayName, that.displayName)
               && Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(displayName);
        $hc = 31 * $hc + Objects.hashCode(id);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (displayName != null) {
            serializer.writeString($SCHEMA_DISPLAY_NAME, displayName);
        }
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISPLAY_NAME, member, displayName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Owner}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.displayName(this.displayName);
        builder.id(this.id);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Owner}.
     */
    public static final class Builder implements ShapeBuilder<Owner> {
        private String displayName;
        private String id;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         *
         *
         * @return this builder.
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Container for the ID of the owner.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Owner build() {
            return new Owner(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> displayName((String) SchemaUtils.validateSameMember($SCHEMA_DISPLAY_NAME, member, value));
                case 1 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
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
                    case 0 -> builder.displayName(de.readString(member));
                    case 1 -> builder.id(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
