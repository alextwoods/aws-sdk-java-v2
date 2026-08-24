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
public final class GetObjectLegalHoldOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_OBJECT_LEGAL_HOLD_OUTPUT;
    private static final Schema $SCHEMA_LEGAL_HOLD = $SCHEMA.member("LegalHold");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectLockLegalHold legalHold;

    private GetObjectLegalHoldOutput(Builder builder) {
        this.legalHold = builder.legalHold;
    }

    /**
     * The current legal hold status for the specified object.
     */
    public ObjectLockLegalHold getLegalHold() {
        return legalHold;
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
        GetObjectLegalHoldOutput that = (GetObjectLegalHoldOutput) other;
        return Objects.equals(this.legalHold, that.legalHold);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(legalHold);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (legalHold != null) {
            serializer.writeStruct($SCHEMA_LEGAL_HOLD, legalHold);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LEGAL_HOLD, member, legalHold);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetObjectLegalHoldOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.legalHold(this.legalHold);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetObjectLegalHoldOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetObjectLegalHoldOutput> {
        private ObjectLockLegalHold legalHold;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The current legal hold status for the specified object.
         *
         * @return this builder.
         */
        public Builder legalHold(ObjectLockLegalHold legalHold) {
            this.legalHold = legalHold;
            return this;
        }

        @Override
        public GetObjectLegalHoldOutput build() {
            return new GetObjectLegalHoldOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> legalHold((ObjectLockLegalHold) SchemaUtils.validateSameMember($SCHEMA_LEGAL_HOLD, member, value));
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
                    case 0 -> builder.legalHold(ObjectLockLegalHold.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
