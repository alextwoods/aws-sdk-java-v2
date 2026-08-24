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
 * Specifies how requests are redirected. In the event of an error, you can specify a different error code to return.
 */
@SmithyGenerated
public final class Redirect implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REDIRECT;
    private static final Schema $SCHEMA_HOST_NAME = $SCHEMA.member("HostName");
    private static final Schema $SCHEMA_HTTP_REDIRECT_CODE = $SCHEMA.member("HttpRedirectCode");
    private static final Schema $SCHEMA_PROTOCOL = $SCHEMA.member("Protocol");
    private static final Schema $SCHEMA_REPLACE_KEY_PREFIX_WITH = $SCHEMA.member("ReplaceKeyPrefixWith");
    private static final Schema $SCHEMA_REPLACE_KEY_WITH = $SCHEMA.member("ReplaceKeyWith");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String hostName;
    private final transient String httpRedirectCode;
    private final transient Protocol protocol;
    private final transient String replaceKeyPrefixWith;
    private final transient String replaceKeyWith;

    private Redirect(Builder builder) {
        this.hostName = builder.hostName;
        this.httpRedirectCode = builder.httpRedirectCode;
        this.protocol = builder.protocol;
        this.replaceKeyPrefixWith = builder.replaceKeyPrefixWith;
        this.replaceKeyWith = builder.replaceKeyWith;
    }

    /**
     * The host name to use in the redirect request.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * The HTTP redirect code to use on the response. Not required if one of the siblings is present.
     */
    public String getHttpRedirectCode() {
        return httpRedirectCode;
    }

    /**
     * Protocol to use when redirecting requests. The default is the protocol that is used in the original request.
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * The object key prefix to use in the redirect request. For example, to redirect requests for all pages with prefix
     * <code>docs/</code> (objects in the <code>docs/</code> folder) to <code>documents/</code>, you can set a condition
     * block with <code>KeyPrefixEquals</code> set to <code>docs/</code> and in the Redirect set <code>
     * ReplaceKeyPrefixWith</code> to <code>/documents</code>. Not required if one of the siblings is present. Can be
     * present only if <code>ReplaceKeyWith</code> is not provided.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getReplaceKeyPrefixWith() {
        return replaceKeyPrefixWith;
    }

    /**
     * The specific object key to use in the redirect request. For example, redirect request to <code>error.html</code>.
     * Not required if one of the siblings is present. Can be present only if <code>ReplaceKeyPrefixWith</code> is not
     * provided.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getReplaceKeyWith() {
        return replaceKeyWith;
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
        Redirect that = (Redirect) other;
        return Objects.equals(this.hostName, that.hostName)
               && Objects.equals(this.httpRedirectCode, that.httpRedirectCode)
               && Objects.equals(this.replaceKeyPrefixWith, that.replaceKeyPrefixWith)
               && Objects.equals(this.replaceKeyWith, that.replaceKeyWith)
               && Objects.equals(this.protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(hostName);
        $hc = 31 * $hc + Objects.hashCode(httpRedirectCode);
        $hc = 31 * $hc + Objects.hashCode(protocol);
        $hc = 31 * $hc + Objects.hashCode(replaceKeyPrefixWith);
        $hc = 31 * $hc + Objects.hashCode(replaceKeyWith);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (hostName != null) {
            serializer.writeString($SCHEMA_HOST_NAME, hostName);
        }
        if (httpRedirectCode != null) {
            serializer.writeString($SCHEMA_HTTP_REDIRECT_CODE, httpRedirectCode);
        }
        if (protocol != null) {
            serializer.writeString($SCHEMA_PROTOCOL, protocol.getValue());
        }
        if (replaceKeyPrefixWith != null) {
            serializer.writeString($SCHEMA_REPLACE_KEY_PREFIX_WITH, replaceKeyPrefixWith);
        }
        if (replaceKeyWith != null) {
            serializer.writeString($SCHEMA_REPLACE_KEY_WITH, replaceKeyWith);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_HOST_NAME, member, hostName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_HTTP_REDIRECT_CODE, member, httpRedirectCode);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROTOCOL, member, protocol);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLACE_KEY_PREFIX_WITH, member, replaceKeyPrefixWith);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLACE_KEY_WITH, member, replaceKeyWith);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Redirect}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.hostName(this.hostName);
        builder.httpRedirectCode(this.httpRedirectCode);
        builder.protocol(this.protocol);
        builder.replaceKeyPrefixWith(this.replaceKeyPrefixWith);
        builder.replaceKeyWith(this.replaceKeyWith);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Redirect}.
     */
    public static final class Builder implements ShapeBuilder<Redirect> {
        private String hostName;
        private String httpRedirectCode;
        private Protocol protocol;
        private String replaceKeyPrefixWith;
        private String replaceKeyWith;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The host name to use in the redirect request.
         *
         * @return this builder.
         */
        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        /**
         * The HTTP redirect code to use on the response. Not required if one of the siblings is present.
         *
         * @return this builder.
         */
        public Builder httpRedirectCode(String httpRedirectCode) {
            this.httpRedirectCode = httpRedirectCode;
            return this;
        }

        /**
         * Protocol to use when redirecting requests. The default is the protocol that is used in the original request.
         *
         * @return this builder.
         */
        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * The object key prefix to use in the redirect request. For example, to redirect requests for all pages with prefix
         * <code>docs/</code> (objects in the <code>docs/</code> folder) to <code>documents/</code>, you can set a condition
         * block with <code>KeyPrefixEquals</code> set to <code>docs/</code> and in the Redirect set <code>
         * ReplaceKeyPrefixWith</code> to <code>/documents</code>. Not required if one of the siblings is present. Can be
         * present only if <code>ReplaceKeyWith</code> is not provided.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * @return this builder.
         */
        public Builder replaceKeyPrefixWith(String replaceKeyPrefixWith) {
            this.replaceKeyPrefixWith = replaceKeyPrefixWith;
            return this;
        }

        /**
         * The specific object key to use in the redirect request. For example, redirect request to <code>error.html</code>.
         * Not required if one of the siblings is present. Can be present only if <code>ReplaceKeyPrefixWith</code> is not
         * provided.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * @return this builder.
         */
        public Builder replaceKeyWith(String replaceKeyWith) {
            this.replaceKeyWith = replaceKeyWith;
            return this;
        }

        @Override
        public Redirect build() {
            return new Redirect(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> hostName((String) SchemaUtils.validateSameMember($SCHEMA_HOST_NAME, member, value));
                case 1 -> httpRedirectCode((String) SchemaUtils.validateSameMember($SCHEMA_HTTP_REDIRECT_CODE, member, value));
                case 2 -> protocol((Protocol) SchemaUtils.validateSameMember($SCHEMA_PROTOCOL, member, value));
                case 3 -> replaceKeyPrefixWith((String) SchemaUtils.validateSameMember($SCHEMA_REPLACE_KEY_PREFIX_WITH, member, value));
                case 4 -> replaceKeyWith((String) SchemaUtils.validateSameMember($SCHEMA_REPLACE_KEY_WITH, member, value));
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
                    case 0 -> builder.hostName(de.readString(member));
                    case 1 -> builder.httpRedirectCode(de.readString(member));
                    case 2 -> builder.protocol(Protocol.builder().deserializeMember(de, member).build());
                    case 3 -> builder.replaceKeyPrefixWith(de.readString(member));
                    case 4 -> builder.replaceKeyWith(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
