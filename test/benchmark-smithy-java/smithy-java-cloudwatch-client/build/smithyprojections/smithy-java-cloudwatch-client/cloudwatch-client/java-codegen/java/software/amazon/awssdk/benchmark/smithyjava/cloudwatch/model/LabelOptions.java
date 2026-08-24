package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * This structure includes the <code>Timezone</code> parameter, which you can use to specify your time zone so that the
 * labels that are associated with returned metrics display the correct time for your time zone.
 *
 * <p>The <code>Timezone</code> value affects a label only if you have a time-based dynamic expression in the label. For
 * more information about dynamic expressions in labels, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/graph-dynamic-labels.html">Using Dynamic Labels</a>.
 */
@SmithyGenerated
public final class LabelOptions implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LABEL_OPTIONS;
    private static final Schema $SCHEMA_TIMEZONE = $SCHEMA.member("Timezone");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String timezone;

    private LabelOptions(Builder builder) {
        this.timezone = builder.timezone;
    }

    /**
     * The time zone to use for metric data return in this operation. The format is <code>+</code> or <code>-</code>
     * followed by four digits. The first two digits indicate the number of hours ahead or behind of UTC, and the final
     * two digits are the number of minutes. For example, +0130 indicates a time zone that is 1 hour and 30 minutes
     * ahead of UTC. The default is +0000.
     */
    public String getTimezone() {
        return timezone;
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
        LabelOptions that = (LabelOptions) other;
        return Objects.equals(this.timezone, that.timezone);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timezone);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (timezone != null) {
            serializer.writeString($SCHEMA_TIMEZONE, timezone);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMEZONE, member, timezone);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LabelOptions}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timezone(this.timezone);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LabelOptions}.
     */
    public static final class Builder implements ShapeBuilder<LabelOptions> {
        private String timezone;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The time zone to use for metric data return in this operation. The format is <code>+</code> or <code>-</code>
         * followed by four digits. The first two digits indicate the number of hours ahead or behind of UTC, and the final
         * two digits are the number of minutes. For example, +0130 indicates a time zone that is 1 hour and 30 minutes
         * ahead of UTC. The default is +0000.
         *
         * @return this builder.
         */
        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        @Override
        public LabelOptions build() {
            return new LabelOptions(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timezone((String) SchemaUtils.validateSameMember($SCHEMA_TIMEZONE, member, value));
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
                    case 0 -> builder.timezone(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
