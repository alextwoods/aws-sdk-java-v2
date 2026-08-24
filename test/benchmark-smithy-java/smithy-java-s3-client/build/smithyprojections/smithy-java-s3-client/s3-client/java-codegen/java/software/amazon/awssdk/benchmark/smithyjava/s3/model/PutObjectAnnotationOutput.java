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
public final class PutObjectAnnotationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.PUT_OBJECT_ANNOTATION_OUTPUT;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_ANNOTATION_NAME = $SCHEMA.member("AnnotationName");
    private static final Schema $SCHEMA_OBJECT_VERSION_ID = $SCHEMA.member("ObjectVersionId");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_CHECKSUMCR_C32 = $SCHEMA.member("ChecksumCRC32");
    private static final Schema $SCHEMA_CHECKSUMCR_C32_C = $SCHEMA.member("ChecksumCRC32C");
    private static final Schema $SCHEMA_CHECKSUMCR_C64NVM_E = $SCHEMA.member("ChecksumCRC64NVME");
    private static final Schema $SCHEMA_CHECKSUMSH_A1 = $SCHEMA.member("ChecksumSHA1");
    private static final Schema $SCHEMA_CHECKSUMSH_A256 = $SCHEMA.member("ChecksumSHA256");
    private static final Schema $SCHEMA_CHECKSUMSH_A512 = $SCHEMA.member("ChecksumSHA512");
    private static final Schema $SCHEMA_CHECKSUMM_D5 = $SCHEMA.member("ChecksumMD5");
    private static final Schema $SCHEMA_CHECKSUMXXHAS_H64 = $SCHEMA.member("ChecksumXXHASH64");
    private static final Schema $SCHEMA_CHECKSUMXXHAS_H3 = $SCHEMA.member("ChecksumXXHASH3");
    private static final Schema $SCHEMA_CHECKSUMXXHAS_H128 = $SCHEMA.member("ChecksumXXHASH128");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String key;
    private final transient String annotationName;
    private final transient String objectVersionId;
    private final transient String eTag;
    private final transient String checksumcrC32;
    private final transient String checksumcrC32C;
    private final transient String checksumcrC64nvmE;
    private final transient String checksumshA1;
    private final transient String checksumshA256;
    private final transient String checksumshA512;
    private final transient String checksummD5;
    private final transient String checksumxxhasH64;
    private final transient String checksumxxhasH3;
    private final transient String checksumxxhasH128;
    private final transient ChecksumType checksumType;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient RequestCharged requestCharged;

    private PutObjectAnnotationOutput(Builder builder) {
        this.key = builder.key;
        this.annotationName = builder.annotationName;
        this.objectVersionId = builder.objectVersionId;
        this.eTag = builder.eTag;
        this.checksumcrC32 = builder.checksumcrC32;
        this.checksumcrC32C = builder.checksumcrC32C;
        this.checksumcrC64nvmE = builder.checksumcrC64nvmE;
        this.checksumshA1 = builder.checksumshA1;
        this.checksumshA256 = builder.checksumshA256;
        this.checksumshA512 = builder.checksumshA512;
        this.checksummD5 = builder.checksummD5;
        this.checksumxxhasH64 = builder.checksumxxhasH64;
        this.checksumxxhasH3 = builder.checksumxxhasH3;
        this.checksumxxhasH128 = builder.checksumxxhasH128;
        this.checksumType = builder.checksumType;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * The object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * The name of the annotation.
     */
    public String getAnnotationName() {
        return annotationName;
    }

    /**
     * The version ID of the object that the annotation was attached to.
     */
    public String getObjectVersionId() {
        return objectVersionId;
    }

    /**
     * The entity tag of the annotation.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * The CRC32 checksum of the stored annotation.
     */
    public String getChecksumcrC32() {
        return checksumcrC32;
    }

    /**
     * The CRC32C checksum of the stored annotation.
     */
    public String getChecksumcrC32C() {
        return checksumcrC32C;
    }

    /**
     * The CRC64NVME checksum of the stored annotation.
     */
    public String getChecksumcrC64nvmE() {
        return checksumcrC64nvmE;
    }

    /**
     * The SHA1 checksum of the stored annotation.
     */
    public String getChecksumshA1() {
        return checksumshA1;
    }

    /**
     * The SHA256 checksum of the stored annotation.
     */
    public String getChecksumshA256() {
        return checksumshA256;
    }

    /**
     * The SHA512 checksum of the stored annotation.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * The MD5 checksum of the stored annotation.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * The XXHASH64 checksum of the stored annotation.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * The XXHASH3 checksum of the stored annotation.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * The XXHASH128 checksum of the stored annotation.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
    }

    /**
     * The type of checksum used.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
    }

    /**
     * The server-side encryption algorithm used to encrypt the annotation.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
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
        PutObjectAnnotationOutput that = (PutObjectAnnotationOutput) other;
        return Objects.equals(this.key, that.key)
               && Objects.equals(this.annotationName, that.annotationName)
               && Objects.equals(this.objectVersionId, that.objectVersionId)
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.checksumcrC32, that.checksumcrC32)
               && Objects.equals(this.checksumcrC32C, that.checksumcrC32C)
               && Objects.equals(this.checksumcrC64nvmE, that.checksumcrC64nvmE)
               && Objects.equals(this.checksumshA1, that.checksumshA1)
               && Objects.equals(this.checksumshA256, that.checksumshA256)
               && Objects.equals(this.checksumshA512, that.checksumshA512)
               && Objects.equals(this.checksummD5, that.checksummD5)
               && Objects.equals(this.checksumxxhasH64, that.checksumxxhasH64)
               && Objects.equals(this.checksumxxhasH3, that.checksumxxhasH3)
               && Objects.equals(this.checksumxxhasH128, that.checksumxxhasH128)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.requestCharged, that.requestCharged);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(annotationName);
        $hc = 31 * $hc + Objects.hashCode(objectVersionId);
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(checksumcrC32);
        $hc = 31 * $hc + Objects.hashCode(checksumcrC32C);
        $hc = 31 * $hc + Objects.hashCode(checksumcrC64nvmE);
        $hc = 31 * $hc + Objects.hashCode(checksumshA1);
        $hc = 31 * $hc + Objects.hashCode(checksumshA256);
        $hc = 31 * $hc + Objects.hashCode(checksumshA512);
        $hc = 31 * $hc + Objects.hashCode(checksummD5);
        $hc = 31 * $hc + Objects.hashCode(checksumxxhasH64);
        $hc = 31 * $hc + Objects.hashCode(checksumxxhasH3);
        $hc = 31 * $hc + Objects.hashCode(checksumxxhasH128);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (annotationName != null) {
            serializer.writeString($SCHEMA_ANNOTATION_NAME, annotationName);
        }
        if (objectVersionId != null) {
            serializer.writeString($SCHEMA_OBJECT_VERSION_ID, objectVersionId);
        }
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (checksumcrC32 != null) {
            serializer.writeString($SCHEMA_CHECKSUMCR_C32, checksumcrC32);
        }
        if (checksumcrC32C != null) {
            serializer.writeString($SCHEMA_CHECKSUMCR_C32_C, checksumcrC32C);
        }
        if (checksumcrC64nvmE != null) {
            serializer.writeString($SCHEMA_CHECKSUMCR_C64NVM_E, checksumcrC64nvmE);
        }
        if (checksumshA1 != null) {
            serializer.writeString($SCHEMA_CHECKSUMSH_A1, checksumshA1);
        }
        if (checksumshA256 != null) {
            serializer.writeString($SCHEMA_CHECKSUMSH_A256, checksumshA256);
        }
        if (checksumshA512 != null) {
            serializer.writeString($SCHEMA_CHECKSUMSH_A512, checksumshA512);
        }
        if (checksummD5 != null) {
            serializer.writeString($SCHEMA_CHECKSUMM_D5, checksummD5);
        }
        if (checksumxxhasH64 != null) {
            serializer.writeString($SCHEMA_CHECKSUMXXHAS_H64, checksumxxhasH64);
        }
        if (checksumxxhasH3 != null) {
            serializer.writeString($SCHEMA_CHECKSUMXXHAS_H3, checksumxxhasH3);
        }
        if (checksumxxhasH128 != null) {
            serializer.writeString($SCHEMA_CHECKSUMXXHAS_H128, checksumxxhasH128);
        }
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, annotationName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_VERSION_ID, member, objectVersionId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutObjectAnnotationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.annotationName(this.annotationName);
        builder.objectVersionId(this.objectVersionId);
        builder.eTag(this.eTag);
        builder.checksumcrC32(this.checksumcrC32);
        builder.checksumcrC32C(this.checksumcrC32C);
        builder.checksumcrC64nvmE(this.checksumcrC64nvmE);
        builder.checksumshA1(this.checksumshA1);
        builder.checksumshA256(this.checksumshA256);
        builder.checksumshA512(this.checksumshA512);
        builder.checksummD5(this.checksummD5);
        builder.checksumxxhasH64(this.checksumxxhasH64);
        builder.checksumxxhasH3(this.checksumxxhasH3);
        builder.checksumxxhasH128(this.checksumxxhasH128);
        builder.checksumType(this.checksumType);
        builder.serverSideEncryption(this.serverSideEncryption);
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
     * Builder for {@link PutObjectAnnotationOutput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectAnnotationOutput> {
        private String key;
        private String annotationName;
        private String objectVersionId;
        private String eTag;
        private String checksumcrC32;
        private String checksumcrC32C;
        private String checksumcrC64nvmE;
        private String checksumshA1;
        private String checksumshA256;
        private String checksumshA512;
        private String checksummD5;
        private String checksumxxhasH64;
        private String checksumxxhasH3;
        private String checksumxxhasH128;
        private ChecksumType checksumType;
        private ServerSideEncryption serverSideEncryption;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The object key.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * The name of the annotation.
         *
         * @return this builder.
         */
        public Builder annotationName(String annotationName) {
            this.annotationName = annotationName;
            return this;
        }

        /**
         * The version ID of the object that the annotation was attached to.
         *
         * @return this builder.
         */
        public Builder objectVersionId(String objectVersionId) {
            this.objectVersionId = objectVersionId;
            return this;
        }

        /**
         * The entity tag of the annotation.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * The CRC32 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumcrC32(String checksumcrC32) {
            this.checksumcrC32 = checksumcrC32;
            return this;
        }

        /**
         * The CRC32C checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumcrC32C(String checksumcrC32C) {
            this.checksumcrC32C = checksumcrC32C;
            return this;
        }

        /**
         * The CRC64NVME checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumcrC64nvmE(String checksumcrC64nvmE) {
            this.checksumcrC64nvmE = checksumcrC64nvmE;
            return this;
        }

        /**
         * The SHA1 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumshA1(String checksumshA1) {
            this.checksumshA1 = checksumshA1;
            return this;
        }

        /**
         * The SHA256 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumshA256(String checksumshA256) {
            this.checksumshA256 = checksumshA256;
            return this;
        }

        /**
         * The SHA512 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumshA512(String checksumshA512) {
            this.checksumshA512 = checksumshA512;
            return this;
        }

        /**
         * The MD5 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksummD5(String checksummD5) {
            this.checksummD5 = checksummD5;
            return this;
        }

        /**
         * The XXHASH64 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH64(String checksumxxhasH64) {
            this.checksumxxhasH64 = checksumxxhasH64;
            return this;
        }

        /**
         * The XXHASH3 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH3(String checksumxxhasH3) {
            this.checksumxxhasH3 = checksumxxhasH3;
            return this;
        }

        /**
         * The XXHASH128 checksum of the stored annotation.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH128(String checksumxxhasH128) {
            this.checksumxxhasH128 = checksumxxhasH128;
            return this;
        }

        /**
         * The type of checksum used.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        /**
         * The server-side encryption algorithm used to encrypt the annotation.
         *
         * @return this builder.
         */
        public Builder serverSideEncryption(ServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
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
        public PutObjectAnnotationOutput build() {
            return new PutObjectAnnotationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> annotationName((String) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, value));
                case 2 -> objectVersionId((String) SchemaUtils.validateSameMember($SCHEMA_OBJECT_VERSION_ID, member, value));
                case 3 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 4 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 5 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 6 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 7 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 8 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 9 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 10 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 11 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 12 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 13 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
                case 14 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                case 15 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 16 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.key(de.readString(member));
                    case 1 -> builder.annotationName(de.readString(member));
                    case 2 -> builder.objectVersionId(de.readString(member));
                    case 3 -> builder.eTag(de.readString(member));
                    case 4 -> builder.checksumcrC32(de.readString(member));
                    case 5 -> builder.checksumcrC32C(de.readString(member));
                    case 6 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 7 -> builder.checksumshA1(de.readString(member));
                    case 8 -> builder.checksumshA256(de.readString(member));
                    case 9 -> builder.checksumshA512(de.readString(member));
                    case 10 -> builder.checksummD5(de.readString(member));
                    case 11 -> builder.checksumxxhasH64(de.readString(member));
                    case 12 -> builder.checksumxxhasH3(de.readString(member));
                    case 13 -> builder.checksumxxhasH128(de.readString(member));
                    case 14 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    case 15 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 16 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
