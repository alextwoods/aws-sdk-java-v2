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
public final class DeleteObjectsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE_OBJECTS_OUTPUT;
    private static final Schema $SCHEMA_DELETED = $SCHEMA.member("Deleted");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_ERRORS = $SCHEMA.member("Errors");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<DeletedObject> deleted;
    private final transient RequestCharged requestCharged;
    private final transient List<Error> errors;

    private DeleteObjectsOutput(Builder builder) {
        this.deleted = builder.deleted == null ? null : Collections.unmodifiableList(builder.deleted);
        this.requestCharged = builder.requestCharged;
        this.errors = builder.errors == null ? null : Collections.unmodifiableList(builder.errors);
    }

    /**
     * Container element for a successful delete. It identifies the object that was successfully deleted.
     */
    public List<DeletedObject> getDeleted() {
        if (deleted == null) {
            return Collections.emptyList();
        }
        return deleted;
    }

    public boolean hasDeleted() {
        return deleted != null;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * Container for a failed delete action that describes the object that Amazon S3 attempted to delete and the error
     * it encountered.
     */
    public List<Error> getErrors() {
        if (errors == null) {
            return Collections.emptyList();
        }
        return errors;
    }

    public boolean hasErrors() {
        return errors != null;
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
        DeleteObjectsOutput that = (DeleteObjectsOutput) other;
        return Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.deleted, that.deleted)
               && Objects.equals(this.errors, that.errors);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(deleted);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(errors);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (deleted != null) {
            serializer.writeList($SCHEMA_DELETED, deleted, deleted.size(), SharedSerde.DeletedObjectsSerializer.INSTANCE);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (errors != null) {
            serializer.writeList($SCHEMA_ERRORS, errors, errors.size(), SharedSerde.ErrorsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETED, member, deleted);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERRORS, member, errors);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteObjectsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.deleted(this.deleted);
        builder.requestCharged(this.requestCharged);
        builder.errors(this.errors);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteObjectsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteObjectsOutput> {
        private List<DeletedObject> deleted;
        private RequestCharged requestCharged;
        private List<Error> errors;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Container element for a successful delete. It identifies the object that was successfully deleted.
         *
         * @return this builder.
         */
        public Builder deleted(List<DeletedObject> deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        /**
         * Container for a failed delete action that describes the object that Amazon S3 attempted to delete and the error
         * it encountered.
         *
         * @return this builder.
         */
        public Builder errors(List<Error> errors) {
            this.errors = errors;
            return this;
        }

        @Override
        public DeleteObjectsOutput build() {
            return new DeleteObjectsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> deleted((List<DeletedObject>) SchemaUtils.validateSameMember($SCHEMA_DELETED, member, value));
                case 1 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 2 -> errors((List<Error>) SchemaUtils.validateSameMember($SCHEMA_ERRORS, member, value));
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
                    case 0 -> builder.deleted(SharedSerde.deserializeDeletedObjects(member, de));
                    case 1 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 2 -> builder.errors(SharedSerde.deserializeErrors(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
