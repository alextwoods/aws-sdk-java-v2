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
public final class GetBucketWebsiteOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_BUCKET_WEBSITE_OUTPUT;
    private static final Schema $SCHEMA_REDIRECT_ALL_REQUESTS_TO = $SCHEMA.member("RedirectAllRequestsTo");
    private static final Schema $SCHEMA_INDEX_DOCUMENT = $SCHEMA.member("IndexDocument");
    private static final Schema $SCHEMA_ERROR_DOCUMENT = $SCHEMA.member("ErrorDocument");
    private static final Schema $SCHEMA_ROUTING_RULES = $SCHEMA.member("RoutingRules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient RedirectAllRequestsTo redirectAllRequestsTo;
    private final transient IndexDocument indexDocument;
    private final transient ErrorDocument errorDocument;
    private final transient List<RoutingRule> routingRules;

    private GetBucketWebsiteOutput(Builder builder) {
        this.redirectAllRequestsTo = builder.redirectAllRequestsTo;
        this.indexDocument = builder.indexDocument;
        this.errorDocument = builder.errorDocument;
        this.routingRules = builder.routingRules == null ? null : Collections.unmodifiableList(builder.routingRules);
    }

    /**
     * Specifies the redirect behavior of all requests to a website endpoint of an Amazon S3 bucket.
     */
    public RedirectAllRequestsTo getRedirectAllRequestsTo() {
        return redirectAllRequestsTo;
    }

    /**
     * The name of the index document for the website (for example <code>index.html</code>).
     */
    public IndexDocument getIndexDocument() {
        return indexDocument;
    }

    /**
     * The object key name of the website error document to use for 4XX class errors.
     */
    public ErrorDocument getErrorDocument() {
        return errorDocument;
    }

    /**
     * Rules that define when a redirect is applied and the redirect behavior.
     */
    public List<RoutingRule> getRoutingRules() {
        if (routingRules == null) {
            return Collections.emptyList();
        }
        return routingRules;
    }

    public boolean hasRoutingRules() {
        return routingRules != null;
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
        GetBucketWebsiteOutput that = (GetBucketWebsiteOutput) other;
        return Objects.equals(this.redirectAllRequestsTo, that.redirectAllRequestsTo)
               && Objects.equals(this.indexDocument, that.indexDocument)
               && Objects.equals(this.errorDocument, that.errorDocument)
               && Objects.equals(this.routingRules, that.routingRules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(redirectAllRequestsTo);
        $hc = 31 * $hc + Objects.hashCode(indexDocument);
        $hc = 31 * $hc + Objects.hashCode(errorDocument);
        $hc = 31 * $hc + Objects.hashCode(routingRules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (redirectAllRequestsTo != null) {
            serializer.writeStruct($SCHEMA_REDIRECT_ALL_REQUESTS_TO, redirectAllRequestsTo);
        }
        if (indexDocument != null) {
            serializer.writeStruct($SCHEMA_INDEX_DOCUMENT, indexDocument);
        }
        if (errorDocument != null) {
            serializer.writeStruct($SCHEMA_ERROR_DOCUMENT, errorDocument);
        }
        if (routingRules != null) {
            serializer.writeList($SCHEMA_ROUTING_RULES, routingRules, routingRules.size(), SharedSerde.RoutingRulesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REDIRECT_ALL_REQUESTS_TO, member, redirectAllRequestsTo);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_DOCUMENT, member, indexDocument);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR_DOCUMENT, member, errorDocument);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ROUTING_RULES, member, routingRules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketWebsiteOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.redirectAllRequestsTo(this.redirectAllRequestsTo);
        builder.indexDocument(this.indexDocument);
        builder.errorDocument(this.errorDocument);
        builder.routingRules(this.routingRules);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketWebsiteOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketWebsiteOutput> {
        private RedirectAllRequestsTo redirectAllRequestsTo;
        private IndexDocument indexDocument;
        private ErrorDocument errorDocument;
        private List<RoutingRule> routingRules;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the redirect behavior of all requests to a website endpoint of an Amazon S3 bucket.
         *
         * @return this builder.
         */
        public Builder redirectAllRequestsTo(RedirectAllRequestsTo redirectAllRequestsTo) {
            this.redirectAllRequestsTo = redirectAllRequestsTo;
            return this;
        }

        /**
         * The name of the index document for the website (for example <code>index.html</code>).
         *
         * @return this builder.
         */
        public Builder indexDocument(IndexDocument indexDocument) {
            this.indexDocument = indexDocument;
            return this;
        }

        /**
         * The object key name of the website error document to use for 4XX class errors.
         *
         * @return this builder.
         */
        public Builder errorDocument(ErrorDocument errorDocument) {
            this.errorDocument = errorDocument;
            return this;
        }

        /**
         * Rules that define when a redirect is applied and the redirect behavior.
         *
         * @return this builder.
         */
        public Builder routingRules(List<RoutingRule> routingRules) {
            this.routingRules = routingRules;
            return this;
        }

        @Override
        public GetBucketWebsiteOutput build() {
            return new GetBucketWebsiteOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> redirectAllRequestsTo((RedirectAllRequestsTo) SchemaUtils.validateSameMember($SCHEMA_REDIRECT_ALL_REQUESTS_TO, member, value));
                case 1 -> indexDocument((IndexDocument) SchemaUtils.validateSameMember($SCHEMA_INDEX_DOCUMENT, member, value));
                case 2 -> errorDocument((ErrorDocument) SchemaUtils.validateSameMember($SCHEMA_ERROR_DOCUMENT, member, value));
                case 3 -> routingRules((List<RoutingRule>) SchemaUtils.validateSameMember($SCHEMA_ROUTING_RULES, member, value));
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
                    case 0 -> builder.redirectAllRequestsTo(RedirectAllRequestsTo.builder().deserializeMember(de, member).build());
                    case 1 -> builder.indexDocument(IndexDocument.builder().deserializeMember(de, member).build());
                    case 2 -> builder.errorDocument(ErrorDocument.builder().deserializeMember(de, member).build());
                    case 3 -> builder.routingRules(SharedSerde.deserializeRoutingRules(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
