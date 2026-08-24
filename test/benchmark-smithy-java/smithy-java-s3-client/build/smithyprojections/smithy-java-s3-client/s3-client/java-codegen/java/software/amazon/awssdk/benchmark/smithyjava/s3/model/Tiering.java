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
 * The S3 Intelligent-Tiering storage class is designed to optimize storage costs by automatically moving data to the
 * most cost-effective storage access tier, without additional operational overhead.
 */
@SmithyGenerated
public final class Tiering implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.TIERING;
    private static final Schema $SCHEMA_DAYS = $SCHEMA.member("Days");
    private static final Schema $SCHEMA_ACCESS_TIER = $SCHEMA.member("AccessTier");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient int days;
    private final transient IntelligentTieringAccessTier accessTier;

    private Tiering(Builder builder) {
        this.days = builder.days;
        this.accessTier = builder.accessTier;
    }

    /**
     * The number of consecutive days of no access after which an object will be eligible to be transitioned to the
     * corresponding tier. The minimum number of days specified for Archive Access tier must be at least 90 days and
     * Deep Archive Access tier must be at least 180 days. The maximum can be up to 2 years (730 days).
     */
    public int getDays() {
        return days;
    }

    /**
     * S3 Intelligent-Tiering access tier. See <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html#sc-dynamic-data-access">Storage
     * class for automatically optimizing frequently and infrequently accessed objects</a> for a list of access tiers in
     * the S3 Intelligent-Tiering storage class.
     */
    public IntelligentTieringAccessTier getAccessTier() {
        return accessTier;
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
        Tiering that = (Tiering) other;
        return this.days == that.days
               && Objects.equals(this.accessTier, that.accessTier);
    }

    @Override
    public int hashCode() {
        int $hc = Integer.hashCode(days);
        $hc = 31 * $hc + Objects.hashCode(accessTier);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeInteger($SCHEMA_DAYS, days);
        serializer.writeString($SCHEMA_ACCESS_TIER, accessTier.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, days);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_TIER, member, accessTier);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Tiering}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.days(this.days);
        builder.accessTier(this.accessTier);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Tiering}.
     */
    public static final class Builder implements ShapeBuilder<Tiering> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private int days;
        private IntelligentTieringAccessTier accessTier;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The number of consecutive days of no access after which an object will be eligible to be transitioned to the
         * corresponding tier. The minimum number of days specified for Archive Access tier must be at least 90 days and
         * Deep Archive Access tier must be at least 180 days. The maximum can be up to 2 years (730 days).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder days(int days) {
            this.days = days;
            tracker.setMember($SCHEMA_DAYS);
            return this;
        }

        /**
         * S3 Intelligent-Tiering access tier. See <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html#sc-dynamic-data-access">Storage
         * class for automatically optimizing frequently and infrequently accessed objects</a> for a list of access tiers in
         * the S3 Intelligent-Tiering storage class.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder accessTier(IntelligentTieringAccessTier accessTier) {
            this.accessTier = Objects.requireNonNull(accessTier, "accessTier cannot be null");
            tracker.setMember($SCHEMA_ACCESS_TIER);
            return this;
        }

        @Override
        public Tiering build() {
            tracker.validate();
            return new Tiering(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> days((int) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, value));
                case 1 -> accessTier((IntelligentTieringAccessTier) SchemaUtils.validateSameMember($SCHEMA_ACCESS_TIER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Tiering> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DAYS)) {
                tracker.setMember($SCHEMA_DAYS);
            }
            if (!tracker.checkMember($SCHEMA_ACCESS_TIER)) {
                accessTier(IntelligentTieringAccessTier.unknown(""));
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
                    case 0 -> builder.days(de.readInteger(member));
                    case 1 -> builder.accessTier(IntelligentTieringAccessTier.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
