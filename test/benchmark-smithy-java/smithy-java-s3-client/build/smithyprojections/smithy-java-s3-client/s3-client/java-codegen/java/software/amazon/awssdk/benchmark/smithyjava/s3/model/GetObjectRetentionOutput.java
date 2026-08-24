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
public final class GetObjectRetentionOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_OBJECT_RETENTION_OUTPUT;
    private static final Schema $SCHEMA_RETENTION = $SCHEMA.member("Retention");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectLockRetention retention;

    private GetObjectRetentionOutput(Builder builder) {
        this.retention = builder.retention;
    }

    /**
     * The container element for an object's retention settings.
     */
    public ObjectLockRetention getRetention() {
        return retention;
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
        GetObjectRetentionOutput that = (GetObjectRetentionOutput) other;
        return Objects.equals(this.retention, that.retention);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(retention);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (retention != null) {
            serializer.writeStruct($SCHEMA_RETENTION, retention);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETENTION, member, retention);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetObjectRetentionOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.retention(this.retention);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetObjectRetentionOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetObjectRetentionOutput> {
        private ObjectLockRetention retention;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The container element for an object's retention settings.
         *
         * @return this builder.
         */
        public Builder retention(ObjectLockRetention retention) {
            this.retention = retention;
            return this;
        }

        @Override
        public GetObjectRetentionOutput build() {
            return new GetObjectRetentionOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> retention((ObjectLockRetention) SchemaUtils.validateSameMember($SCHEMA_RETENTION, member, value));
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
                    case 0 -> builder.retention(ObjectLockRetention.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
