package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
public final class RenameObjectInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.RENAME_OBJECT_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_RENAME_SOURCE = $SCHEMA.member("RenameSource");
    private static final Schema $SCHEMA_DESTINATION_IF_MATCH = $SCHEMA.member("DestinationIfMatch");
    private static final Schema $SCHEMA_DESTINATION_IF_NONE_MATCH = $SCHEMA.member("DestinationIfNoneMatch");
    private static final Schema $SCHEMA_DESTINATION_IF_MODIFIED_SINCE = $SCHEMA.member("DestinationIfModifiedSince");
    private static final Schema $SCHEMA_DESTINATION_IF_UNMODIFIED_SINCE = $SCHEMA.member("DestinationIfUnmodifiedSince");
    private static final Schema $SCHEMA_SOURCE_IF_MATCH = $SCHEMA.member("SourceIfMatch");
    private static final Schema $SCHEMA_SOURCE_IF_NONE_MATCH = $SCHEMA.member("SourceIfNoneMatch");
    private static final Schema $SCHEMA_SOURCE_IF_MODIFIED_SINCE = $SCHEMA.member("SourceIfModifiedSince");
    private static final Schema $SCHEMA_SOURCE_IF_UNMODIFIED_SINCE = $SCHEMA.member("SourceIfUnmodifiedSince");
    private static final Schema $SCHEMA_CLIENT_TOKEN = $SCHEMA.member("ClientToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String renameSource;
    private final transient String destinationIfMatch;
    private final transient String destinationIfNoneMatch;
    private final transient Instant destinationIfModifiedSince;
    private final transient Instant destinationIfUnmodifiedSince;
    private final transient String sourceIfMatch;
    private final transient String sourceIfNoneMatch;
    private final transient Instant sourceIfModifiedSince;
    private final transient Instant sourceIfUnmodifiedSince;
    private final transient String clientToken;

    private RenameObjectInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.renameSource = builder.renameSource;
        this.destinationIfMatch = builder.destinationIfMatch;
        this.destinationIfNoneMatch = builder.destinationIfNoneMatch;
        this.destinationIfModifiedSince = builder.destinationIfModifiedSince;
        this.destinationIfUnmodifiedSince = builder.destinationIfUnmodifiedSince;
        this.sourceIfMatch = builder.sourceIfMatch;
        this.sourceIfNoneMatch = builder.sourceIfNoneMatch;
        this.sourceIfModifiedSince = builder.sourceIfModifiedSince;
        this.sourceIfUnmodifiedSince = builder.sourceIfUnmodifiedSince;
        this.clientToken = builder.clientToken;
    }

    /**
     * The bucket name of the directory bucket containing the object.
     *
     * <p> You must use virtual-hosted-style requests in the format <code>
     * Bucket-name.s3express-zone-id.region-code.amazonaws.com</code>. Path-style requests are not supported. Directory
     * bucket names must be unique in the chosen Availability Zone. Bucket names must follow the format <code>
     * bucket-base-name--zone-id--x-s3 </code> (for example, <code>amzn-s3-demo-bucket--usw2-az1--x-s3</code>). For
     * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
     * Guide</i>.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Key name of the object to rename.
     */
    public String getKey() {
        return key;
    }

    /**
     * Specifies the source for the rename operation. The value must be URL encoded.
     */
    public String getRenameSource() {
        return renameSource;
    }

    /**
     * Renames the object only if the ETag (entity tag) value provided during the operation matches the ETag of the
     * object in S3. The <code>If-Match</code> header field makes the request method conditional on ETags. If the ETag
     * values do not match, the operation returns a <code>412 Precondition Failed</code> error.
     *
     * <p>Expects the ETag value as a string.
     */
    public String getDestinationIfMatch() {
        return destinationIfMatch;
    }

    /**
     * Renames the object only if the destination does not already exist in the specified directory bucket. If the
     * object does exist when you send a request with <code>If-None-Match:&#42;</code>, the S3 API will return a <code>412
     * Precondition Failed</code> error, preventing an overwrite. The <code>If-None-Match</code> header prevents
     * overwrites of existing data by validating that there's not an object with the same key name already in your
     * directory bucket.
     *
     * <p> Expects the <code>&#42;</code> character (asterisk).
     */
    public String getDestinationIfNoneMatch() {
        return destinationIfNoneMatch;
    }

    /**
     * Renames the object if the destination exists and if it has been modified since the specified time.
     */
    public Instant getDestinationIfModifiedSince() {
        return destinationIfModifiedSince;
    }

    /**
     * Renames the object if it hasn't been modified since the specified time.
     */
    public Instant getDestinationIfUnmodifiedSince() {
        return destinationIfUnmodifiedSince;
    }

    /**
     * Renames the object if the source exists and if its entity tag (ETag) matches the specified ETag.
     */
    public String getSourceIfMatch() {
        return sourceIfMatch;
    }

    /**
     * Renames the object if the source exists and if its entity tag (ETag) is different than the specified ETag. If an
     * asterisk (<code>&#42;</code>) character is provided, the operation will fail and return a <code>412 Precondition
     * Failed</code> error.
     */
    public String getSourceIfNoneMatch() {
        return sourceIfNoneMatch;
    }

    /**
     * Renames the object if the source exists and if it has been modified since the specified time.
     */
    public Instant getSourceIfModifiedSince() {
        return sourceIfModifiedSince;
    }

    /**
     * Renames the object if the source exists and hasn't been modified since the specified time.
     */
    public Instant getSourceIfUnmodifiedSince() {
        return sourceIfUnmodifiedSince;
    }

    /**
     * A unique string with a max of 64 ASCII characters in the ASCII range of 33 - 126.
     *
     * <p><code>RenameObject</code> supports idempotency using a client token. To make an idempotent API request using <code>
     * RenameObject</code>, specify a client token in the request. You should not reuse the same client token for other
     * API requests. If you retry a request that completed successfully using the same client token and the same
     * parameters, the retry succeeds without performing any further actions. If you retry a successful request using
     * the same client token, but one or more of the parameters are different, the retry fails and an <code>
     * IdempotentParameterMismatch</code> error is returned.
     */
    public String getClientToken() {
        return clientToken;
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
        RenameObjectInput that = (RenameObjectInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.renameSource, that.renameSource)
               && Objects.equals(this.destinationIfMatch, that.destinationIfMatch)
               && Objects.equals(this.destinationIfNoneMatch, that.destinationIfNoneMatch)
               && Objects.equals(this.sourceIfMatch, that.sourceIfMatch)
               && Objects.equals(this.sourceIfNoneMatch, that.sourceIfNoneMatch)
               && Objects.equals(this.clientToken, that.clientToken)
               && Objects.equals(this.destinationIfModifiedSince, that.destinationIfModifiedSince)
               && Objects.equals(this.destinationIfUnmodifiedSince, that.destinationIfUnmodifiedSince)
               && Objects.equals(this.sourceIfModifiedSince, that.sourceIfModifiedSince)
               && Objects.equals(this.sourceIfUnmodifiedSince, that.sourceIfUnmodifiedSince);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(renameSource);
        $hc = 31 * $hc + Objects.hashCode(destinationIfMatch);
        $hc = 31 * $hc + Objects.hashCode(destinationIfNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(destinationIfModifiedSince);
        $hc = 31 * $hc + Objects.hashCode(destinationIfUnmodifiedSince);
        $hc = 31 * $hc + Objects.hashCode(sourceIfMatch);
        $hc = 31 * $hc + Objects.hashCode(sourceIfNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(sourceIfModifiedSince);
        $hc = 31 * $hc + Objects.hashCode(sourceIfUnmodifiedSince);
        $hc = 31 * $hc + Objects.hashCode(clientToken);
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
        serializer.writeString($SCHEMA_RENAME_SOURCE, renameSource);
        if (destinationIfMatch != null) {
            serializer.writeString($SCHEMA_DESTINATION_IF_MATCH, destinationIfMatch);
        }
        if (destinationIfNoneMatch != null) {
            serializer.writeString($SCHEMA_DESTINATION_IF_NONE_MATCH, destinationIfNoneMatch);
        }
        if (destinationIfModifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_DESTINATION_IF_MODIFIED_SINCE, destinationIfModifiedSince);
        }
        if (destinationIfUnmodifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_DESTINATION_IF_UNMODIFIED_SINCE, destinationIfUnmodifiedSince);
        }
        if (sourceIfMatch != null) {
            serializer.writeString($SCHEMA_SOURCE_IF_MATCH, sourceIfMatch);
        }
        if (sourceIfNoneMatch != null) {
            serializer.writeString($SCHEMA_SOURCE_IF_NONE_MATCH, sourceIfNoneMatch);
        }
        if (sourceIfModifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_SOURCE_IF_MODIFIED_SINCE, sourceIfModifiedSince);
        }
        if (sourceIfUnmodifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_SOURCE_IF_UNMODIFIED_SINCE, sourceIfUnmodifiedSince);
        }
        if (clientToken != null) {
            serializer.writeString($SCHEMA_CLIENT_TOKEN, clientToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RENAME_SOURCE, member, renameSource);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_MATCH, member, destinationIfMatch);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_NONE_MATCH, member, destinationIfNoneMatch);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_MODIFIED_SINCE, member, destinationIfModifiedSince);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_UNMODIFIED_SINCE, member, destinationIfUnmodifiedSince);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_MATCH, member, sourceIfMatch);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_NONE_MATCH, member, sourceIfNoneMatch);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_MODIFIED_SINCE, member, sourceIfModifiedSince);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_UNMODIFIED_SINCE, member, sourceIfUnmodifiedSince);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, clientToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RenameObjectInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.renameSource(this.renameSource);
        builder.destinationIfMatch(this.destinationIfMatch);
        builder.destinationIfNoneMatch(this.destinationIfNoneMatch);
        builder.destinationIfModifiedSince(this.destinationIfModifiedSince);
        builder.destinationIfUnmodifiedSince(this.destinationIfUnmodifiedSince);
        builder.sourceIfMatch(this.sourceIfMatch);
        builder.sourceIfNoneMatch(this.sourceIfNoneMatch);
        builder.sourceIfModifiedSince(this.sourceIfModifiedSince);
        builder.sourceIfUnmodifiedSince(this.sourceIfUnmodifiedSince);
        builder.clientToken(this.clientToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RenameObjectInput}.
     */
    public static final class Builder implements ShapeBuilder<RenameObjectInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String renameSource;
        private String destinationIfMatch;
        private String destinationIfNoneMatch;
        private Instant destinationIfModifiedSince;
        private Instant destinationIfUnmodifiedSince;
        private String sourceIfMatch;
        private String sourceIfNoneMatch;
        private Instant sourceIfModifiedSince;
        private Instant sourceIfUnmodifiedSince;
        private String clientToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket name of the directory bucket containing the object.
         *
         * <p> You must use virtual-hosted-style requests in the format <code>
         * Bucket-name.s3express-zone-id.region-code.amazonaws.com</code>. Path-style requests are not supported. Directory
         * bucket names must be unique in the chosen Availability Zone. Bucket names must follow the format <code>
         * bucket-base-name--zone-id--x-s3 </code> (for example, <code>amzn-s3-demo-bucket--usw2-az1--x-s3</code>). For
         * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
         * Guide</i>.
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
         * Key name of the object to rename.
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
         * Specifies the source for the rename operation. The value must be URL encoded.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder renameSource(String renameSource) {
            this.renameSource = Objects.requireNonNull(renameSource, "renameSource cannot be null");
            tracker.setMember($SCHEMA_RENAME_SOURCE);
            return this;
        }

        /**
         * Renames the object only if the ETag (entity tag) value provided during the operation matches the ETag of the
         * object in S3. The <code>If-Match</code> header field makes the request method conditional on ETags. If the ETag
         * values do not match, the operation returns a <code>412 Precondition Failed</code> error.
         *
         * <p>Expects the ETag value as a string.
         *
         * @return this builder.
         */
        public Builder destinationIfMatch(String destinationIfMatch) {
            this.destinationIfMatch = destinationIfMatch;
            return this;
        }

        /**
         * Renames the object only if the destination does not already exist in the specified directory bucket. If the
         * object does exist when you send a request with <code>If-None-Match:&#42;</code>, the S3 API will return a <code>412
         * Precondition Failed</code> error, preventing an overwrite. The <code>If-None-Match</code> header prevents
         * overwrites of existing data by validating that there's not an object with the same key name already in your
         * directory bucket.
         *
         * <p> Expects the <code>&#42;</code> character (asterisk).
         *
         * @return this builder.
         */
        public Builder destinationIfNoneMatch(String destinationIfNoneMatch) {
            this.destinationIfNoneMatch = destinationIfNoneMatch;
            return this;
        }

        /**
         * Renames the object if the destination exists and if it has been modified since the specified time.
         *
         * @return this builder.
         */
        public Builder destinationIfModifiedSince(Instant destinationIfModifiedSince) {
            this.destinationIfModifiedSince = destinationIfModifiedSince;
            return this;
        }

        /**
         * Renames the object if it hasn't been modified since the specified time.
         *
         * @return this builder.
         */
        public Builder destinationIfUnmodifiedSince(Instant destinationIfUnmodifiedSince) {
            this.destinationIfUnmodifiedSince = destinationIfUnmodifiedSince;
            return this;
        }

        /**
         * Renames the object if the source exists and if its entity tag (ETag) matches the specified ETag.
         *
         * @return this builder.
         */
        public Builder sourceIfMatch(String sourceIfMatch) {
            this.sourceIfMatch = sourceIfMatch;
            return this;
        }

        /**
         * Renames the object if the source exists and if its entity tag (ETag) is different than the specified ETag. If an
         * asterisk (<code>&#42;</code>) character is provided, the operation will fail and return a <code>412 Precondition
         * Failed</code> error.
         *
         * @return this builder.
         */
        public Builder sourceIfNoneMatch(String sourceIfNoneMatch) {
            this.sourceIfNoneMatch = sourceIfNoneMatch;
            return this;
        }

        /**
         * Renames the object if the source exists and if it has been modified since the specified time.
         *
         * @return this builder.
         */
        public Builder sourceIfModifiedSince(Instant sourceIfModifiedSince) {
            this.sourceIfModifiedSince = sourceIfModifiedSince;
            return this;
        }

        /**
         * Renames the object if the source exists and hasn't been modified since the specified time.
         *
         * @return this builder.
         */
        public Builder sourceIfUnmodifiedSince(Instant sourceIfUnmodifiedSince) {
            this.sourceIfUnmodifiedSince = sourceIfUnmodifiedSince;
            return this;
        }

        /**
         * A unique string with a max of 64 ASCII characters in the ASCII range of 33 - 126.
         *
         * <p><code>RenameObject</code> supports idempotency using a client token. To make an idempotent API request using <code>
         * RenameObject</code>, specify a client token in the request. You should not reuse the same client token for other
         * API requests. If you retry a request that completed successfully using the same client token and the same
         * parameters, the retry succeeds without performing any further actions. If you retry a successful request using
         * the same client token, but one or more of the parameters are different, the retry fails and an <code>
         * IdempotentParameterMismatch</code> error is returned.
         *
         * @return this builder.
         */
        public Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
            return this;
        }

        @Override
        public RenameObjectInput build() {
            tracker.validate();
            return new RenameObjectInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> renameSource((String) SchemaUtils.validateSameMember($SCHEMA_RENAME_SOURCE, member, value));
                case 3 -> destinationIfMatch((String) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_MATCH, member, value));
                case 4 -> destinationIfNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_NONE_MATCH, member, value));
                case 5 -> destinationIfModifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_MODIFIED_SINCE, member, value));
                case 6 -> destinationIfUnmodifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_IF_UNMODIFIED_SINCE, member, value));
                case 7 -> sourceIfMatch((String) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_MATCH, member, value));
                case 8 -> sourceIfNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_NONE_MATCH, member, value));
                case 9 -> sourceIfModifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_MODIFIED_SINCE, member, value));
                case 10 -> sourceIfUnmodifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_SOURCE_IF_UNMODIFIED_SINCE, member, value));
                case 11 -> clientToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_TOKEN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RenameObjectInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_RENAME_SOURCE)) {
                renameSource("");
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
                    case 2 -> builder.renameSource(de.readString(member));
                    case 3 -> builder.destinationIfMatch(de.readString(member));
                    case 4 -> builder.destinationIfNoneMatch(de.readString(member));
                    case 5 -> builder.destinationIfModifiedSince(de.readTimestamp(member));
                    case 6 -> builder.destinationIfUnmodifiedSince(de.readTimestamp(member));
                    case 7 -> builder.sourceIfMatch(de.readString(member));
                    case 8 -> builder.sourceIfNoneMatch(de.readString(member));
                    case 9 -> builder.sourceIfModifiedSince(de.readTimestamp(member));
                    case 10 -> builder.sourceIfUnmodifiedSince(de.readTimestamp(member));
                    case 11 -> builder.clientToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
