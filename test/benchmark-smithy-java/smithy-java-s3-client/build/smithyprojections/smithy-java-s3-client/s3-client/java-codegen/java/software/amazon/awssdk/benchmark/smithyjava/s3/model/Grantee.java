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
 * Container for the person being granted permissions.
 */
@SmithyGenerated
public final class Grantee implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GRANTEE;
    private static final Schema $SCHEMA_DISPLAY_NAME = $SCHEMA.member("DisplayName");
    private static final Schema $SCHEMA_EMAIL_ADDRESS = $SCHEMA.member("EmailAddress");
    private static final Schema $SCHEMA_ID = $SCHEMA.member("ID");
    private static final Schema $SCHEMA_URI = $SCHEMA.member("URI");
    private static final Schema $SCHEMA_TYPE = $SCHEMA.member("Type");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String displayName;
    private final transient String emailAddress;
    private final transient String id;
    private final transient String uri;
    private final transient Type type;

    private Grantee(Builder builder) {
        this.displayName = builder.displayName;
        this.emailAddress = builder.emailAddress;
        this.id = builder.id;
        this.uri = builder.uri;
        this.type = builder.type;
    }

    /**
     *
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     *
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * The canonical user ID of the grantee.
     */
    public String getId() {
        return id;
    }

    /**
     * URI of the grantee group.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Type of grantee
     */
    public Type getType() {
        return type;
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
        Grantee that = (Grantee) other;
        return Objects.equals(this.displayName, that.displayName)
               && Objects.equals(this.emailAddress, that.emailAddress)
               && Objects.equals(this.id, that.id)
               && Objects.equals(this.uri, that.uri)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(displayName);
        $hc = 31 * $hc + Objects.hashCode(emailAddress);
        $hc = 31 * $hc + Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(uri);
        $hc = 31 * $hc + Objects.hashCode(type);
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
        if (emailAddress != null) {
            serializer.writeString($SCHEMA_EMAIL_ADDRESS, emailAddress);
        }
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (uri != null) {
            serializer.writeString($SCHEMA_URI, uri);
        }
        serializer.writeString($SCHEMA_TYPE, type.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, type);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISPLAY_NAME, member, displayName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EMAIL_ADDRESS, member, emailAddress);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_URI, member, uri);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Grantee}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.displayName(this.displayName);
        builder.emailAddress(this.emailAddress);
        builder.id(this.id);
        builder.uri(this.uri);
        builder.type(this.type);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Grantee}.
     */
    public static final class Builder implements ShapeBuilder<Grantee> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String displayName;
        private String emailAddress;
        private String id;
        private String uri;
        private Type type;

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
         *
         *
         * @return this builder.
         */
        public Builder emailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        /**
         * The canonical user ID of the grantee.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * URI of the grantee group.
         *
         * @return this builder.
         */
        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Type of grantee
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder type(Type type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            tracker.setMember($SCHEMA_TYPE);
            return this;
        }

        @Override
        public Grantee build() {
            tracker.validate();
            return new Grantee(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> type((Type) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, value));
                case 1 -> displayName((String) SchemaUtils.validateSameMember($SCHEMA_DISPLAY_NAME, member, value));
                case 2 -> emailAddress((String) SchemaUtils.validateSameMember($SCHEMA_EMAIL_ADDRESS, member, value));
                case 3 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 4 -> uri((String) SchemaUtils.validateSameMember($SCHEMA_URI, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Grantee> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TYPE)) {
                type(Type.unknown(""));
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
                    case 0 -> builder.type(Type.builder().deserializeMember(de, member).build());
                    case 1 -> builder.displayName(de.readString(member));
                    case 2 -> builder.emailAddress(de.readString(member));
                    case 3 -> builder.id(de.readString(member));
                    case 4 -> builder.uri(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
