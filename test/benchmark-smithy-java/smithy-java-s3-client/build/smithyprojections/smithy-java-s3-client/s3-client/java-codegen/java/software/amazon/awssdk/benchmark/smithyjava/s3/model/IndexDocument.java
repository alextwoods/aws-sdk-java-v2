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

/**
 * Container for the <code>Suffix</code> element.
 */
@SmithyGenerated
public final class IndexDocument implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.INDEX_DOCUMENT;
    private static final Schema $SCHEMA_SUFFIX = $SCHEMA.member("Suffix");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String suffix;

    private IndexDocument(Builder builder) {
        this.suffix = builder.suffix;
    }

    /**
     * A suffix that is appended to a request that is for a directory on the website endpoint. (For example, if the
     * suffix is <code>index.html</code> and you make a request to <code>samplebucket/images/</code>, the data that is
     * returned will be for the object with the key name <code>images/index.html</code>.) The suffix must not be empty
     * and must not include a slash character.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getSuffix() {
        return suffix;
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
        IndexDocument that = (IndexDocument) other;
        return Objects.equals(this.suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(suffix);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_SUFFIX, suffix);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SUFFIX, member, suffix);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link IndexDocument}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.suffix(this.suffix);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link IndexDocument}.
     */
    public static final class Builder implements ShapeBuilder<IndexDocument> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String suffix;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A suffix that is appended to a request that is for a directory on the website endpoint. (For example, if the
         * suffix is <code>index.html</code> and you make a request to <code>samplebucket/images/</code>, the data that is
         * returned will be for the object with the key name <code>images/index.html</code>.) The suffix must not be empty
         * and must not include a slash character.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder suffix(String suffix) {
            this.suffix = Objects.requireNonNull(suffix, "suffix cannot be null");
            tracker.setMember($SCHEMA_SUFFIX);
            return this;
        }

        @Override
        public IndexDocument build() {
            tracker.validate();
            return new IndexDocument(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> suffix((String) SchemaUtils.validateSameMember($SCHEMA_SUFFIX, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<IndexDocument> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_SUFFIX)) {
                suffix("");
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
                    case 0 -> builder.suffix(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
