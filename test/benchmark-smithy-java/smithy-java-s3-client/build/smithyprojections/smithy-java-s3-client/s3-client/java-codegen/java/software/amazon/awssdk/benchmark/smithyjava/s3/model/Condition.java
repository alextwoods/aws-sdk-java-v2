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
 * A container for describing a condition that must be met for the specified redirect to apply. For example, 1. If
 * request is for pages in the <code>/docs</code> folder, redirect to the <code>/documents</code> folder. 2. If request
 * results in HTTP error 4xx, redirect request to another host where you might process the error.
 */
@SmithyGenerated
public final class Condition implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.CONDITION;
    private static final Schema $SCHEMA_HTTP_ERROR_CODE_RETURNED_EQUALS = $SCHEMA.member("HttpErrorCodeReturnedEquals");
    private static final Schema $SCHEMA_KEY_PREFIX_EQUALS = $SCHEMA.member("KeyPrefixEquals");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String httpErrorCodeReturnedEquals;
    private final transient String keyPrefixEquals;

    private Condition(Builder builder) {
        this.httpErrorCodeReturnedEquals = builder.httpErrorCodeReturnedEquals;
        this.keyPrefixEquals = builder.keyPrefixEquals;
    }

    /**
     * The HTTP error code when the redirect is applied. In the event of an error, if the error code equals this value,
     * then the specified redirect is applied. Required when parent element <code>Condition</code> is specified and
     * sibling <code>KeyPrefixEquals</code> is not specified. If both are specified, then both must be true for the
     * redirect to be applied.
     */
    public String getHttpErrorCodeReturnedEquals() {
        return httpErrorCodeReturnedEquals;
    }

    /**
     * The object key name prefix when the redirect is applied. For example, to redirect requests for <code>
     * ExamplePage.html</code>, the key prefix will be <code>ExamplePage.html</code>. To redirect request for all pages
     * with the prefix <code>docs/</code>, the key prefix will be <code>/docs</code>, which identifies all objects in
     * the <code>docs/</code> folder. Required when the parent element <code>Condition</code> is specified and sibling <code>
     * HttpErrorCodeReturnedEquals</code> is not specified. If both conditions are specified, both must be true for the
     * redirect to be applied.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getKeyPrefixEquals() {
        return keyPrefixEquals;
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
        Condition that = (Condition) other;
        return Objects.equals(this.httpErrorCodeReturnedEquals, that.httpErrorCodeReturnedEquals)
               && Objects.equals(this.keyPrefixEquals, that.keyPrefixEquals);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(httpErrorCodeReturnedEquals);
        $hc = 31 * $hc + Objects.hashCode(keyPrefixEquals);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (httpErrorCodeReturnedEquals != null) {
            serializer.writeString($SCHEMA_HTTP_ERROR_CODE_RETURNED_EQUALS, httpErrorCodeReturnedEquals);
        }
        if (keyPrefixEquals != null) {
            serializer.writeString($SCHEMA_KEY_PREFIX_EQUALS, keyPrefixEquals);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_HTTP_ERROR_CODE_RETURNED_EQUALS, member, httpErrorCodeReturnedEquals);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_PREFIX_EQUALS, member, keyPrefixEquals);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Condition}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.httpErrorCodeReturnedEquals(this.httpErrorCodeReturnedEquals);
        builder.keyPrefixEquals(this.keyPrefixEquals);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Condition}.
     */
    public static final class Builder implements ShapeBuilder<Condition> {
        private String httpErrorCodeReturnedEquals;
        private String keyPrefixEquals;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The HTTP error code when the redirect is applied. In the event of an error, if the error code equals this value,
         * then the specified redirect is applied. Required when parent element <code>Condition</code> is specified and
         * sibling <code>KeyPrefixEquals</code> is not specified. If both are specified, then both must be true for the
         * redirect to be applied.
         *
         * @return this builder.
         */
        public Builder httpErrorCodeReturnedEquals(String httpErrorCodeReturnedEquals) {
            this.httpErrorCodeReturnedEquals = httpErrorCodeReturnedEquals;
            return this;
        }

        /**
         * The object key name prefix when the redirect is applied. For example, to redirect requests for <code>
         * ExamplePage.html</code>, the key prefix will be <code>ExamplePage.html</code>. To redirect request for all pages
         * with the prefix <code>docs/</code>, the key prefix will be <code>/docs</code>, which identifies all objects in
         * the <code>docs/</code> folder. Required when the parent element <code>Condition</code> is specified and sibling <code>
         * HttpErrorCodeReturnedEquals</code> is not specified. If both conditions are specified, both must be true for the
         * redirect to be applied.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * @return this builder.
         */
        public Builder keyPrefixEquals(String keyPrefixEquals) {
            this.keyPrefixEquals = keyPrefixEquals;
            return this;
        }

        @Override
        public Condition build() {
            return new Condition(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> httpErrorCodeReturnedEquals((String) SchemaUtils.validateSameMember($SCHEMA_HTTP_ERROR_CODE_RETURNED_EQUALS, member, value));
                case 1 -> keyPrefixEquals((String) SchemaUtils.validateSameMember($SCHEMA_KEY_PREFIX_EQUALS, member, value));
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
                    case 0 -> builder.httpErrorCodeReturnedEquals(de.readString(member));
                    case 1 -> builder.keyPrefixEquals(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
