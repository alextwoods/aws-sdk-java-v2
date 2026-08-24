package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
public final class DeleteObjectAnnotationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE_OBJECT_ANNOTATION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_ANNOTATION_NAME = $SCHEMA.member("AnnotationName");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_OBJECT_IF_MATCH = $SCHEMA.member("ObjectIfMatch");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String annotationName;
    private final transient String versionId;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;
    private final transient String objectIfMatch;

    private DeleteObjectAnnotationInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.annotationName = builder.annotationName;
        this.versionId = builder.versionId;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.objectIfMatch = builder.objectIfMatch;
    }

    /**
     * The name of the bucket that contains the object.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * The name of the annotation to delete. Annotation names are UTF-8 encoded and cannot start with <code>aws</code>
     * or <code>s3</code> (case-insensitive).
     *
     * <p>Length Constraints: Minimum length of 1. Maximum length of 512 bytes.
     */
    public String getAnnotationName() {
        return annotationName;
    }

    /**
     * The version ID of the object.
     */
    public String getVersionId() {
        return versionId;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The account ID of the expected bucket owner.
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * If specified, the operation only succeeds if the object's ETag matches the provided value.
     */
    public String getObjectIfMatch() {
        return objectIfMatch;
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
        DeleteObjectAnnotationInput that = (DeleteObjectAnnotationInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.annotationName, that.annotationName)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.objectIfMatch, that.objectIfMatch)
               && Objects.equals(this.requestPayer, that.requestPayer);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(annotationName);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(objectIfMatch);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        serializer.writeString($SCHEMA_KEY, key);
        serializer.writeString($SCHEMA_ANNOTATION_NAME, annotationName);
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (objectIfMatch != null) {
            serializer.writeString($SCHEMA_OBJECT_IF_MATCH, objectIfMatch);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, annotationName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_IF_MATCH, member, objectIfMatch);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteObjectAnnotationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.annotationName(this.annotationName);
        builder.versionId(this.versionId);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.objectIfMatch(this.objectIfMatch);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteObjectAnnotationInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteObjectAnnotationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String annotationName;
        private String versionId;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;
        private String objectIfMatch;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket that contains the object.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = Objects.requireNonNull(bucket, "bucket cannot be null");
            tracker.setMember($SCHEMA_BUCKET);
            return this;
        }

        /**
         * The object key.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * The name of the annotation to delete. Annotation names are UTF-8 encoded and cannot start with <code>aws</code>
         * or <code>s3</code> (case-insensitive).
         *
         * <p>Length Constraints: Minimum length of 1. Maximum length of 512 bytes.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder annotationName(String annotationName) {
            this.annotationName = Objects.requireNonNull(annotationName, "annotationName cannot be null");
            tracker.setMember($SCHEMA_ANNOTATION_NAME);
            return this;
        }

        /**
         * The version ID of the object.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestPayer(RequestPayer requestPayer) {
            this.requestPayer = requestPayer;
            return this;
        }

        /**
         * The account ID of the expected bucket owner.
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        /**
         * If specified, the operation only succeeds if the object's ETag matches the provided value.
         *
         * @return this builder.
         */
        public Builder objectIfMatch(String objectIfMatch) {
            this.objectIfMatch = objectIfMatch;
            return this;
        }

        @Override
        public DeleteObjectAnnotationInput build() {
            tracker.validate();
            return new DeleteObjectAnnotationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> annotationName((String) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, value));
                case 3 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 4 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 5 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 6 -> objectIfMatch((String) SchemaUtils.validateSameMember($SCHEMA_OBJECT_IF_MATCH, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteObjectAnnotationInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_ANNOTATION_NAME)) {
                annotationName("");
            }
            return this;
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
                    case 0 -> builder.bucket(de.readString(member));
                    case 1 -> builder.key(de.readString(member));
                    case 2 -> builder.annotationName(de.readString(member));
                    case 3 -> builder.versionId(de.readString(member));
                    case 4 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 5 -> builder.expectedBucketOwner(de.readString(member));
                    case 6 -> builder.objectIfMatch(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
