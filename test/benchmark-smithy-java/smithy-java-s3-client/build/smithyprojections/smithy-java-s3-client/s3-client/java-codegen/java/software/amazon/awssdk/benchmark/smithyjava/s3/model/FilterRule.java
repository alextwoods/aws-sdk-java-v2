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
 * Specifies the Amazon S3 object key name to filter on. An object key name is the name assigned to an object in your
 * Amazon S3 bucket. You specify whether to filter on the suffix or prefix of the object key name. A prefix is a
 * specific string of characters at the beginning of an object key name, which you can use to organize objects. For
 * example, you can start the key names of related objects with a prefix, such as <code>2023-</code> or
 * <code>engineering/</code>. Then, you can use <code>FilterRule</code> to find objects in a bucket with key names that
 * have the same prefix. A suffix is similar to a prefix, but it is at the end of the object key name instead of at the
 * beginning.
 */
@SmithyGenerated
public final class FilterRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.FILTER_RULE;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_VALUE = $SCHEMA.member("Value");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient FilterRuleName name;
    private final transient String value;

    private FilterRule(Builder builder) {
        this.name = builder.name;
        this.value = builder.value;
    }

    /**
     * The object key name prefix or suffix identifying one or more objects to which the filtering rule applies. The
     * maximum length is 1,024 characters. Overlapping prefixes and suffixes are not supported. For more information,
     * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Configuring Event Notifications</a> in the <i>Amazon S3 User Guide</i>.
     */
    public FilterRuleName getName() {
        return name;
    }

    /**
     * The value that the filter searches for in object key names.
     */
    public String getValue() {
        return value;
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
        FilterRule that = (FilterRule) other;
        return Objects.equals(this.value, that.value)
               && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(value);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name.getValue());
        }
        if (value != null) {
            serializer.writeString($SCHEMA_VALUE, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link FilterRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.value(this.value);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FilterRule}.
     */
    public static final class Builder implements ShapeBuilder<FilterRule> {
        private FilterRuleName name;
        private String value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The object key name prefix or suffix identifying one or more objects to which the filtering rule applies. The
         * maximum length is 1,024 characters. Overlapping prefixes and suffixes are not supported. For more information,
         * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Configuring Event Notifications</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder name(FilterRuleName name) {
            this.name = name;
            return this;
        }

        /**
         * The value that the filter searches for in object key names.
         *
         * @return this builder.
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        public FilterRule build() {
            return new FilterRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((FilterRuleName) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> value((String) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value));
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
                    case 0 -> builder.name(FilterRuleName.builder().deserializeMember(de, member).build());
                    case 1 -> builder.value(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
