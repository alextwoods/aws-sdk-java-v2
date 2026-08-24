package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
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

/**
 * Specifies a cross-origin access rule for an Amazon S3 bucket.
 */
@SmithyGenerated
public final class CORSRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.CORS_RULE;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("ID");
    private static final Schema $SCHEMA_ALLOWED_HEADERS = $SCHEMA.member("AllowedHeaders");
    private static final Schema $SCHEMA_ALLOWED_METHODS = $SCHEMA.member("AllowedMethods");
    private static final Schema $SCHEMA_ALLOWED_ORIGINS = $SCHEMA.member("AllowedOrigins");
    private static final Schema $SCHEMA_EXPOSE_HEADERS = $SCHEMA.member("ExposeHeaders");
    private static final Schema $SCHEMA_MAX_AGE_SECONDS = $SCHEMA.member("MaxAgeSeconds");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient List<String> allowedHeaders;
    private final transient List<String> allowedMethods;
    private final transient List<String> allowedOrigins;
    private final transient List<String> exposeHeaders;
    private final transient Integer maxAgeSeconds;

    private CORSRule(Builder builder) {
        this.id = builder.id;
        this.allowedHeaders = builder.allowedHeaders == null ? null : Collections.unmodifiableList(builder.allowedHeaders);
        this.allowedMethods = Collections.unmodifiableList(builder.allowedMethods);
        this.allowedOrigins = Collections.unmodifiableList(builder.allowedOrigins);
        this.exposeHeaders = builder.exposeHeaders == null ? null : Collections.unmodifiableList(builder.exposeHeaders);
        this.maxAgeSeconds = builder.maxAgeSeconds;
    }

    /**
     * Unique identifier for the rule. The value cannot be longer than 255 characters.
     */
    public String getId() {
        return id;
    }

    /**
     * Headers that are specified in the <code>Access-Control-Request-Headers</code> header. These headers are allowed
     * in a preflight OPTIONS request. In response to any preflight OPTIONS request, Amazon S3 returns any requested
     * headers that are allowed.
     */
    public List<String> getAllowedHeaders() {
        if (allowedHeaders == null) {
            return Collections.emptyList();
        }
        return allowedHeaders;
    }

    public boolean hasAllowedHeaders() {
        return allowedHeaders != null;
    }

    /**
     * An HTTP method that you allow the origin to execute. Valid values are <code>GET</code>, <code>PUT</code>,
     * <code>HEAD</code>, <code>POST</code>, and <code>DELETE</code>.
     */
    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public boolean hasAllowedMethods() {
        return true;
    }

    /**
     * One or more origins you want customers to be able to access the bucket from.
     */
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public boolean hasAllowedOrigins() {
        return true;
    }

    /**
     * One or more headers in the response that you want customers to be able to access from their applications (for
     * example, from a JavaScript <code>XMLHttpRequest</code> object).
     */
    public List<String> getExposeHeaders() {
        if (exposeHeaders == null) {
            return Collections.emptyList();
        }
        return exposeHeaders;
    }

    public boolean hasExposeHeaders() {
        return exposeHeaders != null;
    }

    /**
     * The time in seconds that your browser is to cache the preflight response for the specified resource.
     */
    public Integer getMaxAgeSeconds() {
        return maxAgeSeconds;
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
        CORSRule that = (CORSRule) other;
        return Objects.equals(this.maxAgeSeconds, that.maxAgeSeconds)
               && Objects.equals(this.id, that.id)
               && Objects.equals(this.allowedHeaders, that.allowedHeaders)
               && Objects.equals(this.allowedMethods, that.allowedMethods)
               && Objects.equals(this.allowedOrigins, that.allowedOrigins)
               && Objects.equals(this.exposeHeaders, that.exposeHeaders);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(allowedHeaders);
        $hc = 31 * $hc + Objects.hashCode(allowedMethods);
        $hc = 31 * $hc + Objects.hashCode(allowedOrigins);
        $hc = 31 * $hc + Objects.hashCode(exposeHeaders);
        $hc = 31 * $hc + Objects.hashCode(maxAgeSeconds);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (allowedHeaders != null) {
            serializer.writeList($SCHEMA_ALLOWED_HEADERS, allowedHeaders, allowedHeaders.size(), SharedSerde.AllowedHeadersSerializer.INSTANCE);
        }
        serializer.writeList($SCHEMA_ALLOWED_METHODS, allowedMethods, allowedMethods.size(), SharedSerde.AllowedMethodsSerializer.INSTANCE);
        serializer.writeList($SCHEMA_ALLOWED_ORIGINS, allowedOrigins, allowedOrigins.size(), SharedSerde.AllowedOriginsSerializer.INSTANCE);
        if (exposeHeaders != null) {
            serializer.writeList($SCHEMA_EXPOSE_HEADERS, exposeHeaders, exposeHeaders.size(), SharedSerde.ExposeHeadersSerializer.INSTANCE);
        }
        if (maxAgeSeconds != null) {
            serializer.writeInteger($SCHEMA_MAX_AGE_SECONDS, maxAgeSeconds);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALLOWED_METHODS, member, allowedMethods);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALLOWED_ORIGINS, member, allowedOrigins);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALLOWED_HEADERS, member, allowedHeaders);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPOSE_HEADERS, member, exposeHeaders);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_AGE_SECONDS, member, maxAgeSeconds);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CORSRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.allowedHeaders(this.allowedHeaders);
        builder.allowedMethods(this.allowedMethods);
        builder.allowedOrigins(this.allowedOrigins);
        builder.exposeHeaders(this.exposeHeaders);
        builder.maxAgeSeconds(this.maxAgeSeconds);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CORSRule}.
     */
    public static final class Builder implements ShapeBuilder<CORSRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private List<String> allowedHeaders;
        private List<String> allowedMethods;
        private List<String> allowedOrigins;
        private List<String> exposeHeaders;
        private Integer maxAgeSeconds;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Unique identifier for the rule. The value cannot be longer than 255 characters.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Headers that are specified in the <code>Access-Control-Request-Headers</code> header. These headers are allowed
         * in a preflight OPTIONS request. In response to any preflight OPTIONS request, Amazon S3 returns any requested
         * headers that are allowed.
         *
         * @return this builder.
         */
        public Builder allowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
            return this;
        }

        /**
         * An HTTP method that you allow the origin to execute. Valid values are <code>GET</code>, <code>PUT</code>,
         * <code>HEAD</code>, <code>POST</code>, and <code>DELETE</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder allowedMethods(List<String> allowedMethods) {
            this.allowedMethods = Objects.requireNonNull(allowedMethods, "allowedMethods cannot be null");
            tracker.setMember($SCHEMA_ALLOWED_METHODS);
            return this;
        }

        /**
         * One or more origins you want customers to be able to access the bucket from.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder allowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = Objects.requireNonNull(allowedOrigins, "allowedOrigins cannot be null");
            tracker.setMember($SCHEMA_ALLOWED_ORIGINS);
            return this;
        }

        /**
         * One or more headers in the response that you want customers to be able to access from their applications (for
         * example, from a JavaScript <code>XMLHttpRequest</code> object).
         *
         * @return this builder.
         */
        public Builder exposeHeaders(List<String> exposeHeaders) {
            this.exposeHeaders = exposeHeaders;
            return this;
        }

        /**
         * The time in seconds that your browser is to cache the preflight response for the specified resource.
         *
         * @return this builder.
         */
        public Builder maxAgeSeconds(Integer maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
            return this;
        }

        @Override
        public CORSRule build() {
            tracker.validate();
            return new CORSRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> allowedMethods((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALLOWED_METHODS, member, value));
                case 1 -> allowedOrigins((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALLOWED_ORIGINS, member, value));
                case 2 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 3 -> allowedHeaders((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALLOWED_HEADERS, member, value));
                case 4 -> exposeHeaders((List<String>) SchemaUtils.validateSameMember($SCHEMA_EXPOSE_HEADERS, member, value));
                case 5 -> maxAgeSeconds((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_AGE_SECONDS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CORSRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALLOWED_METHODS)) {
                allowedMethods(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_ALLOWED_ORIGINS)) {
                allowedOrigins(Collections.emptyList());
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
                    case 0 -> builder.allowedMethods(SharedSerde.deserializeAllowedMethods(member, de));
                    case 1 -> builder.allowedOrigins(SharedSerde.deserializeAllowedOrigins(member, de));
                    case 2 -> builder.id(de.readString(member));
                    case 3 -> builder.allowedHeaders(SharedSerde.deserializeAllowedHeaders(member, de));
                    case 4 -> builder.exposeHeaders(SharedSerde.deserializeExposeHeaders(member, de));
                    case 5 -> builder.maxAgeSeconds(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
