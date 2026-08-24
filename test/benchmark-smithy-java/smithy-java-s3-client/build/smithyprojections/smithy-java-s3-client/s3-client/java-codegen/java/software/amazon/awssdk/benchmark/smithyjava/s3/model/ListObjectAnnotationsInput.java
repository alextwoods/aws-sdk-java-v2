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
public final class ListObjectAnnotationsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_OBJECT_ANNOTATIONS_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_MAX_ANNOTATION_RESULTS = $SCHEMA.member("MaxAnnotationResults");
    private static final Schema $SCHEMA_ANNOTATION_PREFIX = $SCHEMA.member("AnnotationPrefix");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String versionId;
    private final transient Integer maxAnnotationResults;
    private final transient String annotationPrefix;
    private final transient String continuationToken;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;

    private ListObjectAnnotationsInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.maxAnnotationResults = builder.maxAnnotationResults;
        this.annotationPrefix = builder.annotationPrefix;
        this.continuationToken = builder.continuationToken;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
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
     * The version ID of the object.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * The maximum number of annotations to return in the response. Maximum is 1,000.
     */
    public Integer getMaxAnnotationResults() {
        return maxAnnotationResults;
    }

    /**
     * Filter results to annotations whose name begins with the specified prefix.
     */
    public String getAnnotationPrefix() {
        return annotationPrefix;
    }

    /**
     * Continuation token returned by a previous request to retrieve the next page.
     */
    public String getContinuationToken() {
        return continuationToken;
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
        ListObjectAnnotationsInput that = (ListObjectAnnotationsInput) other;
        return Objects.equals(this.maxAnnotationResults, that.maxAnnotationResults)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.annotationPrefix, that.annotationPrefix)
               && Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.requestPayer, that.requestPayer);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(maxAnnotationResults);
        $hc = 31 * $hc + Objects.hashCode(annotationPrefix);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
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
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (maxAnnotationResults != null) {
            serializer.writeInteger($SCHEMA_MAX_ANNOTATION_RESULTS, maxAnnotationResults);
        }
        if (annotationPrefix != null) {
            serializer.writeString($SCHEMA_ANNOTATION_PREFIX, annotationPrefix);
        }
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_ANNOTATION_RESULTS, member, maxAnnotationResults);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_PREFIX, member, annotationPrefix);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectAnnotationsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.maxAnnotationResults(this.maxAnnotationResults);
        builder.annotationPrefix(this.annotationPrefix);
        builder.continuationToken(this.continuationToken);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListObjectAnnotationsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectAnnotationsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String versionId;
        private Integer maxAnnotationResults;
        private String annotationPrefix;
        private String continuationToken;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;

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
         * The version ID of the object.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * The maximum number of annotations to return in the response. Maximum is 1,000.
         *
         * @return this builder.
         */
        public Builder maxAnnotationResults(Integer maxAnnotationResults) {
            this.maxAnnotationResults = maxAnnotationResults;
            return this;
        }

        /**
         * Filter results to annotations whose name begins with the specified prefix.
         *
         * @return this builder.
         */
        public Builder annotationPrefix(String annotationPrefix) {
            this.annotationPrefix = annotationPrefix;
            return this;
        }

        /**
         * Continuation token returned by a previous request to retrieve the next page.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
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

        @Override
        public ListObjectAnnotationsInput build() {
            tracker.validate();
            return new ListObjectAnnotationsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 3 -> maxAnnotationResults((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_ANNOTATION_RESULTS, member, value));
                case 4 -> annotationPrefix((String) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_PREFIX, member, value));
                case 5 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 6 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 7 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListObjectAnnotationsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
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
                    case 2 -> builder.versionId(de.readString(member));
                    case 3 -> builder.maxAnnotationResults(de.readInteger(member));
                    case 4 -> builder.annotationPrefix(de.readString(member));
                    case 5 -> builder.continuationToken(de.readString(member));
                    case 6 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 7 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
