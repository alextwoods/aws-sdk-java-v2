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
 * Contains the type of server-side encryption used to encrypt the S3 Inventory results.
 */
@SmithyGenerated
public final class InventoryEncryption implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_ENCRYPTION;
    private static final Schema $SCHEMA_SSES3 = $SCHEMA.member("SSES3");
    private static final Schema $SCHEMA_SSEKMS = $SCHEMA.member("SSEKMS");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient SSES3 sses3;
    private final transient SSEKMS ssekms;

    private InventoryEncryption(Builder builder) {
        this.sses3 = builder.sses3;
        this.ssekms = builder.ssekms;
    }

    /**
     * Specifies the use of SSE-S3 to encrypt delivered inventory reports.
     */
    public SSES3 getSses3() {
        return sses3;
    }

    /**
     * Specifies the use of SSE-KMS to encrypt delivered inventory reports.
     */
    public SSEKMS getSsekms() {
        return ssekms;
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
        InventoryEncryption that = (InventoryEncryption) other;
        return Objects.equals(this.sses3, that.sses3)
               && Objects.equals(this.ssekms, that.ssekms);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sses3);
        $hc = 31 * $hc + Objects.hashCode(ssekms);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sses3 != null) {
            serializer.writeStruct($SCHEMA_SSES3, sses3);
        }
        if (ssekms != null) {
            serializer.writeStruct($SCHEMA_SSEKMS, ssekms);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSES3, member, sses3);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS, member, ssekms);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryEncryption}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sses3(this.sses3);
        builder.ssekms(this.ssekms);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryEncryption}.
     */
    public static final class Builder implements ShapeBuilder<InventoryEncryption> {
        private SSES3 sses3;
        private SSEKMS ssekms;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the use of SSE-S3 to encrypt delivered inventory reports.
         *
         * @return this builder.
         */
        public Builder sses3(SSES3 sses3) {
            this.sses3 = sses3;
            return this;
        }

        /**
         * Specifies the use of SSE-KMS to encrypt delivered inventory reports.
         *
         * @return this builder.
         */
        public Builder ssekms(SSEKMS ssekms) {
            this.ssekms = ssekms;
            return this;
        }

        @Override
        public InventoryEncryption build() {
            return new InventoryEncryption(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> sses3((SSES3) SchemaUtils.validateSameMember($SCHEMA_SSES3, member, value));
                case 1 -> ssekms((SSEKMS) SchemaUtils.validateSameMember($SCHEMA_SSEKMS, member, value));
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
                    case 0 -> builder.sses3(SSES3.builder().deserializeMember(de, member).build());
                    case 1 -> builder.ssekms(SSEKMS.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
