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

@SmithyGenerated
public final class DeleteObjectAnnotationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE_OBJECT_ANNOTATION_OUTPUT;
    private static final Schema $SCHEMA_OBJECT_VERSION_ID = $SCHEMA.member("ObjectVersionId");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String objectVersionId;
    private final transient RequestCharged requestCharged;

    private DeleteObjectAnnotationOutput(Builder builder) {
        this.objectVersionId = builder.objectVersionId;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * The version ID of the object that the annotation was deleted from.
     */
    public String getObjectVersionId() {
        return objectVersionId;
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
        DeleteObjectAnnotationOutput that = (DeleteObjectAnnotationOutput) other;
        return Objects.equals(this.objectVersionId, that.objectVersionId)
               && Objects.equals(this.requestCharged, that.requestCharged);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(objectVersionId);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (objectVersionId != null) {
            serializer.writeString($SCHEMA_OBJECT_VERSION_ID, objectVersionId);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_VERSION_ID, member, objectVersionId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteObjectAnnotationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.objectVersionId(this.objectVersionId);
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
     * Builder for {@link DeleteObjectAnnotationOutput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteObjectAnnotationOutput> {
        private String objectVersionId;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The version ID of the object that the annotation was deleted from.
         *
         * @return this builder.
         */
        public Builder objectVersionId(String objectVersionId) {
            this.objectVersionId = objectVersionId;
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
        public DeleteObjectAnnotationOutput build() {
            return new DeleteObjectAnnotationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> objectVersionId((String) SchemaUtils.validateSameMember($SCHEMA_OBJECT_VERSION_ID, member, value));
                case 1 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.objectVersionId(de.readString(member));
                    case 1 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
