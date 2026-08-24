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

@SmithyGenerated
public final class ListObjectAnnotationsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_OBJECT_ANNOTATIONS_OUTPUT;
    private static final Schema $SCHEMA_ANNOTATIONS = $SCHEMA.member("Annotations");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_OBJECT_VERSION_ID = $SCHEMA.member("ObjectVersionId");
    private static final Schema $SCHEMA_ANNOTATION_PREFIX = $SCHEMA.member("AnnotationPrefix");
    private static final Schema $SCHEMA_MAX_ANNOTATION_RESULTS = $SCHEMA.member("MaxAnnotationResults");
    private static final Schema $SCHEMA_ANNOTATION_COUNT = $SCHEMA.member("AnnotationCount");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_NEXT_CONTINUATION_TOKEN = $SCHEMA.member("NextContinuationToken");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AnnotationEntry> annotations;
    private final transient String bucket;
    private final transient String key;
    private final transient String objectVersionId;
    private final transient String annotationPrefix;
    private final transient Integer maxAnnotationResults;
    private final transient Integer annotationCount;
    private final transient String continuationToken;
    private final transient String nextContinuationToken;
    private final transient RequestCharged requestCharged;

    private ListObjectAnnotationsOutput(Builder builder) {
        this.annotations = builder.annotations == null ? null : Collections.unmodifiableList(builder.annotations);
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.objectVersionId = builder.objectVersionId;
        this.annotationPrefix = builder.annotationPrefix;
        this.maxAnnotationResults = builder.maxAnnotationResults;
        this.annotationCount = builder.annotationCount;
        this.continuationToken = builder.continuationToken;
        this.nextContinuationToken = builder.nextContinuationToken;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * The list of annotations attached to the object.
     */
    public List<AnnotationEntry> getAnnotations() {
        if (annotations == null) {
            return Collections.emptyList();
        }
        return annotations;
    }

    public boolean hasAnnotations() {
        return annotations != null;
    }

    /**
     * The bucket name.
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
    public String getObjectVersionId() {
        return objectVersionId;
    }

    /**
     * The prefix used to filter the response.
     */
    public String getAnnotationPrefix() {
        return annotationPrefix;
    }

    /**
     * The maximum number of annotations returned in the response.
     */
    public Integer getMaxAnnotationResults() {
        return maxAnnotationResults;
    }

    /**
     * The number of annotations returned.
     */
    public Integer getAnnotationCount() {
        return annotationCount;
    }

    /**
     * The continuation token used in this request.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * The continuation token to use to retrieve the next page of results.
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
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
        ListObjectAnnotationsOutput that = (ListObjectAnnotationsOutput) other;
        return Objects.equals(this.maxAnnotationResults, that.maxAnnotationResults)
               && Objects.equals(this.annotationCount, that.annotationCount)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.objectVersionId, that.objectVersionId)
               && Objects.equals(this.annotationPrefix, that.annotationPrefix)
               && Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.nextContinuationToken, that.nextContinuationToken)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(annotations);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(objectVersionId);
        $hc = 31 * $hc + Objects.hashCode(annotationPrefix);
        $hc = 31 * $hc + Objects.hashCode(maxAnnotationResults);
        $hc = 31 * $hc + Objects.hashCode(annotationCount);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(nextContinuationToken);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (annotations != null) {
            serializer.writeList($SCHEMA_ANNOTATIONS, annotations, annotations.size(), SharedSerde.AnnotationListSerializer.INSTANCE);
        }
        if (bucket != null) {
            serializer.writeString($SCHEMA_BUCKET, bucket);
        }
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (objectVersionId != null) {
            serializer.writeString($SCHEMA_OBJECT_VERSION_ID, objectVersionId);
        }
        if (annotationPrefix != null) {
            serializer.writeString($SCHEMA_ANNOTATION_PREFIX, annotationPrefix);
        }
        if (maxAnnotationResults != null) {
            serializer.writeInteger($SCHEMA_MAX_ANNOTATION_RESULTS, maxAnnotationResults);
        }
        if (annotationCount != null) {
            serializer.writeInteger($SCHEMA_ANNOTATION_COUNT, annotationCount);
        }
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (nextContinuationToken != null) {
            serializer.writeString($SCHEMA_NEXT_CONTINUATION_TOKEN, nextContinuationToken);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATIONS, member, annotations);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_VERSION_ID, member, objectVersionId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_PREFIX, member, annotationPrefix);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_ANNOTATION_RESULTS, member, maxAnnotationResults);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_COUNT, member, annotationCount);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, nextContinuationToken);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectAnnotationsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.annotations(this.annotations);
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.objectVersionId(this.objectVersionId);
        builder.annotationPrefix(this.annotationPrefix);
        builder.maxAnnotationResults(this.maxAnnotationResults);
        builder.annotationCount(this.annotationCount);
        builder.continuationToken(this.continuationToken);
        builder.nextContinuationToken(this.nextContinuationToken);
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
     * Builder for {@link ListObjectAnnotationsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectAnnotationsOutput> {
        private List<AnnotationEntry> annotations;
        private String bucket;
        private String key;
        private String objectVersionId;
        private String annotationPrefix;
        private Integer maxAnnotationResults;
        private Integer annotationCount;
        private String continuationToken;
        private String nextContinuationToken;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The list of annotations attached to the object.
         *
         * @return this builder.
         */
        public Builder annotations(List<AnnotationEntry> annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * The bucket name.
         *
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
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
         * The version ID of the object.
         *
         * @return this builder.
         */
        public Builder objectVersionId(String objectVersionId) {
            this.objectVersionId = objectVersionId;
            return this;
        }

        /**
         * The prefix used to filter the response.
         *
         * @return this builder.
         */
        public Builder annotationPrefix(String annotationPrefix) {
            this.annotationPrefix = annotationPrefix;
            return this;
        }

        /**
         * The maximum number of annotations returned in the response.
         *
         * @return this builder.
         */
        public Builder maxAnnotationResults(Integer maxAnnotationResults) {
            this.maxAnnotationResults = maxAnnotationResults;
            return this;
        }

        /**
         * The number of annotations returned.
         *
         * @return this builder.
         */
        public Builder annotationCount(Integer annotationCount) {
            this.annotationCount = annotationCount;
            return this;
        }

        /**
         * The continuation token used in this request.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * The continuation token to use to retrieve the next page of results.
         *
         * @return this builder.
         */
        public Builder nextContinuationToken(String nextContinuationToken) {
            this.nextContinuationToken = nextContinuationToken;
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
        public ListObjectAnnotationsOutput build() {
            return new ListObjectAnnotationsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> annotations((List<AnnotationEntry>) SchemaUtils.validateSameMember($SCHEMA_ANNOTATIONS, member, value));
                case 1 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 2 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 3 -> objectVersionId((String) SchemaUtils.validateSameMember($SCHEMA_OBJECT_VERSION_ID, member, value));
                case 4 -> annotationPrefix((String) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_PREFIX, member, value));
                case 5 -> maxAnnotationResults((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_ANNOTATION_RESULTS, member, value));
                case 6 -> annotationCount((Integer) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_COUNT, member, value));
                case 7 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 8 -> nextContinuationToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, value));
                case 9 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.annotations(SharedSerde.deserializeAnnotationList(member, de));
                    case 1 -> builder.bucket(de.readString(member));
                    case 2 -> builder.key(de.readString(member));
                    case 3 -> builder.objectVersionId(de.readString(member));
                    case 4 -> builder.annotationPrefix(de.readString(member));
                    case 5 -> builder.maxAnnotationResults(de.readInteger(member));
                    case 6 -> builder.annotationCount(de.readInteger(member));
                    case 7 -> builder.continuationToken(de.readString(member));
                    case 8 -> builder.nextContinuationToken(de.readString(member));
                    case 9 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
