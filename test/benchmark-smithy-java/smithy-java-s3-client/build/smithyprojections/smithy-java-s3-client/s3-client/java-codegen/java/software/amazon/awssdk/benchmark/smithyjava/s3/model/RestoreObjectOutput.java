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
public final class RestoreObjectOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.RESTORE_OBJECT_OUTPUT;
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_RESTORE_OUTPUT_PATH = $SCHEMA.member("RestoreOutputPath");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient RequestCharged requestCharged;
    private final transient String restoreOutputPath;

    private RestoreObjectOutput(Builder builder) {
        this.requestCharged = builder.requestCharged;
        this.restoreOutputPath = builder.restoreOutputPath;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * Indicates the path in the provided S3 output location where Select results will be restored to.
     */
    public String getRestoreOutputPath() {
        return restoreOutputPath;
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
        RestoreObjectOutput that = (RestoreObjectOutput) other;
        return Objects.equals(this.restoreOutputPath, that.restoreOutputPath)
               && Objects.equals(this.requestCharged, that.requestCharged);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(restoreOutputPath);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (restoreOutputPath != null) {
            serializer.writeString($SCHEMA_RESTORE_OUTPUT_PATH, restoreOutputPath);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_OUTPUT_PATH, member, restoreOutputPath);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RestoreObjectOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.requestCharged(this.requestCharged);
        builder.restoreOutputPath(this.restoreOutputPath);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RestoreObjectOutput}.
     */
    public static final class Builder implements ShapeBuilder<RestoreObjectOutput> {
        private RequestCharged requestCharged;
        private String restoreOutputPath;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        /**
         * Indicates the path in the provided S3 output location where Select results will be restored to.
         *
         * @return this builder.
         */
        public Builder restoreOutputPath(String restoreOutputPath) {
            this.restoreOutputPath = restoreOutputPath;
            return this;
        }

        @Override
        public RestoreObjectOutput build() {
            return new RestoreObjectOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 1 -> restoreOutputPath((String) SchemaUtils.validateSameMember($SCHEMA_RESTORE_OUTPUT_PATH, member, value));
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
                    case 0 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 1 -> builder.restoreOutputPath(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
