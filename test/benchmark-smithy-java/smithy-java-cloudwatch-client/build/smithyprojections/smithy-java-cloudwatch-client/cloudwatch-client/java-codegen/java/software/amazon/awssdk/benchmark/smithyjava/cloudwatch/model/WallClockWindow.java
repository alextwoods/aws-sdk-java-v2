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
 * An evaluation window that aligns the evaluated range to fixed clock boundaries that match the alarm's period, such as
 * the top of the hour, midnight, or the start of the calendar week, optionally in a specific time zone.
 *
 * <p>When you use a wall clock window, the alarm's period must be 1 minute (60 seconds), 5 minutes (300 seconds), 1
 * hour (3,600 seconds), 1 day (86,400 seconds), or 1 week (604,800 seconds). Other period values aren't supported with
 * a wall clock window.
 *
 * <p>Choose a wall clock window when your monitoring is tied to a business or calendar period, such as daily reports,
 * batch jobs, or backups, or when you want alarm evaluations to match the periods shown on a metric dashboard.
 */
@SmithyGenerated
public final class WallClockWindow implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.WALL_CLOCK_WINDOW;
    private static final Schema $SCHEMA_TIMEZONE = $SCHEMA.member("Timezone");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String timezone;

    private WallClockWindow(Builder builder) {
        this.timezone = builder.timezone;
    }

    /**
     * The time zone to use when the alarm aligns the evaluation window to clock boundaries. You can specify an IANA
     * time zone name (for example, <code>America/New_York</code>), a fixed UTC offset (for example, <code>+05:30</code>
     * ), or an offset-prefixed identifier (for example, <code>UTC+05:30</code>). The offset must be aligned to a
     * multiple of 5 minutes. If you don't specify a time zone, CloudWatch uses <code>UTC</code>.
     *
     * <p>The time zone affects window alignment for all periods, including periods of one hour or shorter.
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
        WallClockWindow that = (WallClockWindow) other;
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
     * @return a builder for {@link WallClockWindow}.
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
     * Builder for {@link WallClockWindow}.
     */
    public static final class Builder implements ShapeBuilder<WallClockWindow> {
        private String timezone;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The time zone to use when the alarm aligns the evaluation window to clock boundaries. You can specify an IANA
         * time zone name (for example, <code>America/New_York</code>), a fixed UTC offset (for example, <code>+05:30</code>
         * ), or an offset-prefixed identifier (for example, <code>UTC+05:30</code>). The offset must be aligned to a
         * multiple of 5 minutes. If you don't specify a time zone, CloudWatch uses <code>UTC</code>.
         *
         * <p>The time zone affects window alignment for all periods, including periods of one hour or shorter.
         *
         * @return this builder.
         */
        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        @Override
        public WallClockWindow build() {
            return new WallClockWindow(this);
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
