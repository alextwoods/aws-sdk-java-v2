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

/**
 * The container for the completed multipart upload details.
 */
@SmithyGenerated
public final class CompletedMultipartUpload implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.COMPLETED_MULTIPART_UPLOAD;
    private static final Schema $SCHEMA_PARTS = $SCHEMA.member("Parts");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<CompletedPart> parts;

    private CompletedMultipartUpload(Builder builder) {
        this.parts = builder.parts == null ? null : Collections.unmodifiableList(builder.parts);
    }

    /**
     * Array of CompletedPart data types.
     *
     * <p>If you do not supply a valid <code>Part</code> with your request, the service sends back an HTTP 400 response.
     */
    public List<CompletedPart> getParts() {
        if (parts == null) {
            return Collections.emptyList();
        }
        return parts;
    }

    public boolean hasParts() {
        return parts != null;
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
        CompletedMultipartUpload that = (CompletedMultipartUpload) other;
        return Objects.equals(this.parts, that.parts);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(parts);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (parts != null) {
            serializer.writeList($SCHEMA_PARTS, parts, parts.size(), SharedSerde.CompletedPartListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTS, member, parts);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CompletedMultipartUpload}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.parts(this.parts);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CompletedMultipartUpload}.
     */
    public static final class Builder implements ShapeBuilder<CompletedMultipartUpload> {
        private List<CompletedPart> parts;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Array of CompletedPart data types.
         *
         * <p>If you do not supply a valid <code>Part</code> with your request, the service sends back an HTTP 400 response.
         *
         * @return this builder.
         */
        public Builder parts(List<CompletedPart> parts) {
            this.parts = parts;
            return this;
        }

        @Override
        public CompletedMultipartUpload build() {
            return new CompletedMultipartUpload(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> parts((List<CompletedPart>) SchemaUtils.validateSameMember($SCHEMA_PARTS, member, value));
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
                    case 0 -> builder.parts(SharedSerde.deserializeCompletedPartList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
