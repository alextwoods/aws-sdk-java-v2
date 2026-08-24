package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the settings used to enable server-side encryption.
 */
@SmithyGenerated
public final class SSESpecification implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SSE_SPECIFICATION;
    private static final Schema $SCHEMA_ENABLED = $SCHEMA.member("Enabled");
    private static final Schema $SCHEMA_SSE_TYPE = $SCHEMA.member("SSEType");
    private static final Schema $SCHEMA_KMS_MASTER_KEY_ID = $SCHEMA.member("KMSMasterKeyId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean enabled;
    private final transient SSEType sseType;
    private final transient String kmsMasterKeyId;

    private SSESpecification(Builder builder) {
        this.enabled = builder.enabled;
        this.sseType = builder.sseType;
        this.kmsMasterKeyId = builder.kmsMasterKeyId;
    }

    /**
     * Indicates whether server-side encryption is done using an Amazon Web Services managed key or an Amazon Web
     * Services owned key. If enabled (true), server-side encryption type is set to <code>KMS</code> and an Amazon Web
     * Services managed key is used (KMS charges apply). If disabled (false) or not specified, server-side encryption is
     * set to Amazon Web Services owned key.
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Server-side encryption type. The only supported value is:
     *
     * <ul>
     *   <li>
     *     <code>KMS</code> - Server-side encryption that uses Key Management Service. The key is stored in your
     *     account and is managed by KMS (KMS charges apply).
     *   </li>
     * </ul>
     */
    public SSEType getSseType() {
        return sseType;
    }

    /**
     * The KMS key that should be used for the KMS encryption. To specify a key, use its key ID, Amazon Resource Name
     * (ARN), alias name, or alias ARN. Note that you should only provide this parameter if the key is different from
     * the default DynamoDB key <code>alias/aws/dynamodb</code>.
     */
    public String getKmsMasterKeyId() {
        return kmsMasterKeyId;
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
        SSESpecification that = (SSESpecification) other;
        return Objects.equals(this.enabled, that.enabled)
               && Objects.equals(this.kmsMasterKeyId, that.kmsMasterKeyId)
               && Objects.equals(this.sseType, that.sseType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(enabled);
        $hc = 31 * $hc + Objects.hashCode(sseType);
        $hc = 31 * $hc + Objects.hashCode(kmsMasterKeyId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (enabled != null) {
            serializer.writeBoolean($SCHEMA_ENABLED, enabled);
        }
        if (sseType != null) {
            serializer.writeString($SCHEMA_SSE_TYPE, sseType.getValue());
        }
        if (kmsMasterKeyId != null) {
            serializer.writeString($SCHEMA_KMS_MASTER_KEY_ID, kmsMasterKeyId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENABLED, member, enabled);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_TYPE, member, sseType);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ID, member, kmsMasterKeyId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SSESpecification}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.enabled(this.enabled);
        builder.sseType(this.sseType);
        builder.kmsMasterKeyId(this.kmsMasterKeyId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SSESpecification}.
     */
    public static final class Builder implements ShapeBuilder<SSESpecification> {
        private Boolean enabled;
        private SSEType sseType;
        private String kmsMasterKeyId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether server-side encryption is done using an Amazon Web Services managed key or an Amazon Web
         * Services owned key. If enabled (true), server-side encryption type is set to <code>KMS</code> and an Amazon Web
         * Services managed key is used (KMS charges apply). If disabled (false) or not specified, server-side encryption is
         * set to Amazon Web Services owned key.
         *
         * @return this builder.
         */
        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Server-side encryption type. The only supported value is:
         *
         * <ul>
         *   <li>
         *     <code>KMS</code> - Server-side encryption that uses Key Management Service. The key is stored in your
         *     account and is managed by KMS (KMS charges apply).
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder sseType(SSEType sseType) {
            this.sseType = sseType;
            return this;
        }

        /**
         * The KMS key that should be used for the KMS encryption. To specify a key, use its key ID, Amazon Resource Name
         * (ARN), alias name, or alias ARN. Note that you should only provide this parameter if the key is different from
         * the default DynamoDB key <code>alias/aws/dynamodb</code>.
         *
         * @return this builder.
         */
        public Builder kmsMasterKeyId(String kmsMasterKeyId) {
            this.kmsMasterKeyId = kmsMasterKeyId;
            return this;
        }

        @Override
        public SSESpecification build() {
            return new SSESpecification(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> enabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_ENABLED, member, value));
                case 1 -> sseType((SSEType) SchemaUtils.validateSameMember($SCHEMA_SSE_TYPE, member, value));
                case 2 -> kmsMasterKeyId((String) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ID, member, value));
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
                    case 0 -> builder.enabled(de.readBoolean(member));
                    case 1 -> builder.sseType(SSEType.builder().deserializeMember(de, member).build());
                    case 2 -> builder.kmsMasterKeyId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
