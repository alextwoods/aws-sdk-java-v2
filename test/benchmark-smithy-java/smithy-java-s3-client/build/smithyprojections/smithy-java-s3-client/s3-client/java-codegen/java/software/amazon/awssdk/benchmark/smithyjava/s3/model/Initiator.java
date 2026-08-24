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
 * Container element that identifies who initiated the multipart upload.
 */
@SmithyGenerated
public final class Initiator implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.INITIATOR;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("ID");
    private static final Schema $SCHEMA_DISPLAY_NAME = $SCHEMA.member("DisplayName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient String displayName;

    private Initiator(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
    }

    /**
     * If the principal is an Amazon Web Services account, it provides the Canonical User ID. If the principal is an IAM
     * User, it provides a user ARN value.
     *
     * <p><b>Directory buckets</b> - If the principal is an Amazon Web Services account, it provides the Amazon Web
     * Services account ID. If the principal is an IAM User, it provides a user ARN value.
     */
    public String getId() {
        return id;
    }

    /**
     * This functionality is not supported for directory buckets.
     */
    public String getDisplayName() {
        return displayName;
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
        Initiator that = (Initiator) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(displayName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (displayName != null) {
            serializer.writeString($SCHEMA_DISPLAY_NAME, displayName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISPLAY_NAME, member, displayName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Initiator}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.displayName(this.displayName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Initiator}.
     */
    public static final class Builder implements ShapeBuilder<Initiator> {
        private String id;
        private String displayName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the principal is an Amazon Web Services account, it provides the Canonical User ID. If the principal is an IAM
         * User, it provides a user ARN value.
         *
         * <p><b>Directory buckets</b> - If the principal is an Amazon Web Services account, it provides the Amazon Web
         * Services account ID. If the principal is an IAM User, it provides a user ARN value.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Initiator build() {
            return new Initiator(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 1 -> displayName((String) SchemaUtils.validateSameMember($SCHEMA_DISPLAY_NAME, member, value));
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
                    case 0 -> builder.id(de.readString(member));
                    case 1 -> builder.displayName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
