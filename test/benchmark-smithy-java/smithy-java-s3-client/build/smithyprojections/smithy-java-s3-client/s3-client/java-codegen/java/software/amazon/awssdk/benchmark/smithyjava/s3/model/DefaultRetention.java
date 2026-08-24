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
 * The container element for optionally specifying the default Object Lock retention settings for new objects placed in
 * the specified bucket.
 *
 * <ul>
 *   <li>
 *     The <code>DefaultRetention</code> settings require both a mode and a period.
 *   </li>
 *   <li>
 *     The <code>DefaultRetention</code> period can be either <code>Days</code> or <code>Years</code> but you must
 *     select one. You cannot specify <code>Days</code> and <code>Years</code> at the same time.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class DefaultRetention implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.DEFAULT_RETENTION;
    private static final Schema $SCHEMA_MODE = $SCHEMA.member("Mode");
    private static final Schema $SCHEMA_DAYS = $SCHEMA.member("Days");
    private static final Schema $SCHEMA_YEARS = $SCHEMA.member("Years");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectLockRetentionMode mode;
    private final transient Integer days;
    private final transient Integer years;

    private DefaultRetention(Builder builder) {
        this.mode = builder.mode;
        this.days = builder.days;
        this.years = builder.years;
    }

    /**
     * The default Object Lock retention mode you want to apply to new objects placed in the specified bucket. Must be
     * used with either <code>Days</code> or <code>Years</code>.
     */
    public ObjectLockRetentionMode getMode() {
        return mode;
    }

    /**
     * The number of days that you want to specify for the default retention period. Must be used with
     * <code>Mode</code>.
     */
    public Integer getDays() {
        return days;
    }

    /**
     * The number of years that you want to specify for the default retention period. Must be used with
     * <code>Mode</code>.
     */
    public Integer getYears() {
        return years;
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
        DefaultRetention that = (DefaultRetention) other;
        return Objects.equals(this.days, that.days)
               && Objects.equals(this.years, that.years)
               && Objects.equals(this.mode, that.mode);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(mode);
        $hc = 31 * $hc + Objects.hashCode(days);
        $hc = 31 * $hc + Objects.hashCode(years);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (mode != null) {
            serializer.writeString($SCHEMA_MODE, mode.getValue());
        }
        if (days != null) {
            serializer.writeInteger($SCHEMA_DAYS, days);
        }
        if (years != null) {
            serializer.writeInteger($SCHEMA_YEARS, years);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MODE, member, mode);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, days);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_YEARS, member, years);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DefaultRetention}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.mode(this.mode);
        builder.days(this.days);
        builder.years(this.years);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DefaultRetention}.
     */
    public static final class Builder implements ShapeBuilder<DefaultRetention> {
        private ObjectLockRetentionMode mode;
        private Integer days;
        private Integer years;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The default Object Lock retention mode you want to apply to new objects placed in the specified bucket. Must be
         * used with either <code>Days</code> or <code>Years</code>.
         *
         * @return this builder.
         */
        public Builder mode(ObjectLockRetentionMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * The number of days that you want to specify for the default retention period. Must be used with
         * <code>Mode</code>.
         *
         * @return this builder.
         */
        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        /**
         * The number of years that you want to specify for the default retention period. Must be used with
         * <code>Mode</code>.
         *
         * @return this builder.
         */
        public Builder years(Integer years) {
            this.years = years;
            return this;
        }

        @Override
        public DefaultRetention build() {
            return new DefaultRetention(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> mode((ObjectLockRetentionMode) SchemaUtils.validateSameMember($SCHEMA_MODE, member, value));
                case 1 -> days((Integer) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, value));
                case 2 -> years((Integer) SchemaUtils.validateSameMember($SCHEMA_YEARS, member, value));
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
                    case 0 -> builder.mode(ObjectLockRetentionMode.builder().deserializeMember(de, member).build());
                    case 1 -> builder.days(de.readInteger(member));
                    case 2 -> builder.years(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
