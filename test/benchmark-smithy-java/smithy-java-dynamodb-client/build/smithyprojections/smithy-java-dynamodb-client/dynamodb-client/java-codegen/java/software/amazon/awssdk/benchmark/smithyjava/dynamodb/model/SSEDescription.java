package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * The description of the server-side encryption status on the specified table.
 */
@SmithyGenerated
public final class SSEDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SSE_DESCRIPTION;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_SSE_TYPE = $SCHEMA.member("SSEType");
    private static final Schema $SCHEMA_KMS_MASTER_KEY_ARN = $SCHEMA.member("KMSMasterKeyArn");
    private static final Schema $SCHEMA_INACCESSIBLE_ENCRYPTION_DATE_TIME = $SCHEMA.member("InaccessibleEncryptionDateTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient SSEStatus status;
    private final transient SSEType sseType;
    private final transient String kmsMasterKeyArn;
    private final transient Instant inaccessibleEncryptionDateTime;

    private SSEDescription(Builder builder) {
        this.status = builder.status;
        this.sseType = builder.sseType;
        this.kmsMasterKeyArn = builder.kmsMasterKeyArn;
        this.inaccessibleEncryptionDateTime = builder.inaccessibleEncryptionDateTime;
    }

    /**
     * Represents the current state of server-side encryption. The only supported values are:
     *
     * <ul>
     *   <li>
     *     <code>ENABLED</code> - Server-side encryption is enabled.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - Server-side encryption is being updated.
     *   </li>
     * </ul>
     */
    public SSEStatus getStatus() {
        return status;
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
     * The KMS key ARN used for the KMS encryption.
     */
    public String getKmsMasterKeyArn() {
        return kmsMasterKeyArn;
    }

    /**
     * Indicates the time, in UNIX epoch date format, when DynamoDB detected that the table's KMS key was inaccessible.
     * This attribute will automatically be cleared when DynamoDB detects that the table's KMS key is accessible again.
     * DynamoDB will initiate the table archival process when table's KMS key remains inaccessible for more than seven
     * days from this date.
     */
    public Instant getInaccessibleEncryptionDateTime() {
        return inaccessibleEncryptionDateTime;
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
        SSEDescription that = (SSEDescription) other;
        return Objects.equals(this.kmsMasterKeyArn, that.kmsMasterKeyArn)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.sseType, that.sseType)
               && Objects.equals(this.inaccessibleEncryptionDateTime, that.inaccessibleEncryptionDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(sseType);
        $hc = 31 * $hc + Objects.hashCode(kmsMasterKeyArn);
        $hc = 31 * $hc + Objects.hashCode(inaccessibleEncryptionDateTime);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (status != null) {
            serializer.writeString($SCHEMA_STATUS, status.getValue());
        }
        if (sseType != null) {
            serializer.writeString($SCHEMA_SSE_TYPE, sseType.getValue());
        }
        if (kmsMasterKeyArn != null) {
            serializer.writeString($SCHEMA_KMS_MASTER_KEY_ARN, kmsMasterKeyArn);
        }
        if (inaccessibleEncryptionDateTime != null) {
            serializer.writeTimestamp($SCHEMA_INACCESSIBLE_ENCRYPTION_DATE_TIME, inaccessibleEncryptionDateTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_TYPE, member, sseType);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ARN, member, kmsMasterKeyArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_INACCESSIBLE_ENCRYPTION_DATE_TIME, member, inaccessibleEncryptionDateTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SSEDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.status(this.status);
        builder.sseType(this.sseType);
        builder.kmsMasterKeyArn(this.kmsMasterKeyArn);
        builder.inaccessibleEncryptionDateTime(this.inaccessibleEncryptionDateTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SSEDescription}.
     */
    public static final class Builder implements ShapeBuilder<SSEDescription> {
        private SSEStatus status;
        private SSEType sseType;
        private String kmsMasterKeyArn;
        private Instant inaccessibleEncryptionDateTime;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the current state of server-side encryption. The only supported values are:
         *
         * <ul>
         *   <li>
         *     <code>ENABLED</code> - Server-side encryption is enabled.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - Server-side encryption is being updated.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder status(SSEStatus status) {
            this.status = status;
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
         * The KMS key ARN used for the KMS encryption.
         *
         * @return this builder.
         */
        public Builder kmsMasterKeyArn(String kmsMasterKeyArn) {
            this.kmsMasterKeyArn = kmsMasterKeyArn;
            return this;
        }

        /**
         * Indicates the time, in UNIX epoch date format, when DynamoDB detected that the table's KMS key was inaccessible.
         * This attribute will automatically be cleared when DynamoDB detects that the table's KMS key is accessible again.
         * DynamoDB will initiate the table archival process when table's KMS key remains inaccessible for more than seven
         * days from this date.
         *
         * @return this builder.
         */
        public Builder inaccessibleEncryptionDateTime(Instant inaccessibleEncryptionDateTime) {
            this.inaccessibleEncryptionDateTime = inaccessibleEncryptionDateTime;
            return this;
        }

        @Override
        public SSEDescription build() {
            return new SSEDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((SSEStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 1 -> sseType((SSEType) SchemaUtils.validateSameMember($SCHEMA_SSE_TYPE, member, value));
                case 2 -> kmsMasterKeyArn((String) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ARN, member, value));
                case 3 -> inaccessibleEncryptionDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_INACCESSIBLE_ENCRYPTION_DATE_TIME, member, value));
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
                    case 0 -> builder.status(SSEStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.sseType(SSEType.builder().deserializeMember(de, member).build());
                    case 2 -> builder.kmsMasterKeyArn(de.readString(member));
                    case 3 -> builder.inaccessibleEncryptionDateTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
