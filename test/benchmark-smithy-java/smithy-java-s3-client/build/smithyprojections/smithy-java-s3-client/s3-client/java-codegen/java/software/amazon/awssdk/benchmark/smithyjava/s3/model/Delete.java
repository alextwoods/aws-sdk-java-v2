package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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

/**
 * Container for the objects to delete.
 */
@SmithyGenerated
public final class Delete implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE;
    private static final Schema $SCHEMA_OBJECTS = $SCHEMA.member("Objects");
    private static final Schema $SCHEMA_QUIET = $SCHEMA.member("Quiet");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ObjectIdentifier> objects;
    private final transient Boolean quiet;

    private Delete(Builder builder) {
        this.objects = Collections.unmodifiableList(builder.objects);
        this.quiet = builder.quiet;
    }

    /**
     * The object to delete.
     *
     * <p><b>Directory buckets</b> - For directory buckets, an object that's composed entirely of whitespace characters
     * is not supported by the <code>DeleteObjects</code> API operation. The request will receive a <code>400 Bad
     * Request</code> error and none of the objects in the request will be deleted.
     */
    public List<ObjectIdentifier> getObjects() {
        return objects;
    }

    public boolean hasObjects() {
        return true;
    }

    /**
     * Element to enable quiet mode for the request. When you add this element, you must set its value to
     * <code>true</code>.
     */
    public Boolean isQuiet() {
        return quiet;
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
        Delete that = (Delete) other;
        return Objects.equals(this.quiet, that.quiet)
               && Objects.equals(this.objects, that.objects);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(objects);
        $hc = 31 * $hc + Objects.hashCode(quiet);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_OBJECTS, objects, objects.size(), SharedSerde.ObjectIdentifierListSerializer.INSTANCE);
        if (quiet != null) {
            serializer.writeBoolean($SCHEMA_QUIET, quiet);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECTS, member, objects);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUIET, member, quiet);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Delete}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.objects(this.objects);
        builder.quiet(this.quiet);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Delete}.
     */
    public static final class Builder implements ShapeBuilder<Delete> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<ObjectIdentifier> objects;
        private Boolean quiet;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The object to delete.
         *
         * <p><b>Directory buckets</b> - For directory buckets, an object that's composed entirely of whitespace characters
         * is not supported by the <code>DeleteObjects</code> API operation. The request will receive a <code>400 Bad
         * Request</code> error and none of the objects in the request will be deleted.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder objects(List<ObjectIdentifier> objects) {
            this.objects = Objects.requireNonNull(objects, "objects cannot be null");
            tracker.setMember($SCHEMA_OBJECTS);
            return this;
        }

        /**
         * Element to enable quiet mode for the request. When you add this element, you must set its value to
         * <code>true</code>.
         *
         * @return this builder.
         */
        public Builder quiet(Boolean quiet) {
            this.quiet = quiet;
            return this;
        }

        @Override
        public Delete build() {
            tracker.validate();
            return new Delete(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> objects((List<ObjectIdentifier>) SchemaUtils.validateSameMember($SCHEMA_OBJECTS, member, value));
                case 1 -> quiet((Boolean) SchemaUtils.validateSameMember($SCHEMA_QUIET, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Delete> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_OBJECTS)) {
                objects(Collections.emptyList());
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
                    case 0 -> builder.objects(SharedSerde.deserializeObjectIdentifierList(member, de));
                    case 1 -> builder.quiet(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
