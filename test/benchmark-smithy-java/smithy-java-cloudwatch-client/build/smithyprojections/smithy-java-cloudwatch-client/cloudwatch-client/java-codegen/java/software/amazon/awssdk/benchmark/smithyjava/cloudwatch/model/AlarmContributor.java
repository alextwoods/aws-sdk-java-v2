package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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
 * Represents an individual contributor to a multi-timeseries alarm, containing information about a specific time series
 * and its contribution to the alarm's state.
 */
@SmithyGenerated
public final class AlarmContributor implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ALARM_CONTRIBUTOR;
    private static final Schema $SCHEMA_CONTRIBUTOR_ID = $SCHEMA.member("ContributorId");
    private static final Schema $SCHEMA_CONTRIBUTOR_ATTRIBUTES = $SCHEMA.member("ContributorAttributes");
    private static final Schema $SCHEMA_STATE_REASON = $SCHEMA.member("StateReason");
    private static final Schema $SCHEMA_STATE_TRANSITIONED_TIMESTAMP = $SCHEMA.member("StateTransitionedTimestamp");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String contributorId;
    private final transient Map<String, String> contributorAttributes;
    private final transient String stateReason;
    private final transient Instant stateTransitionedTimestamp;

    private AlarmContributor(Builder builder) {
        this.contributorId = builder.contributorId;
        this.contributorAttributes = builder.contributorAttributes == null ? null : Collections.unmodifiableMap(builder.contributorAttributes);
        this.stateReason = builder.stateReason;
        this.stateTransitionedTimestamp = builder.stateTransitionedTimestamp;
    }

    /**
     * The unique identifier for this alarm contributor.
     */
    public String getContributorId() {
        return contributorId;
    }

    /**
     * A map of attributes that describe the contributor, such as metric dimensions and other identifying
     * characteristics.
     */
    public Map<String, String> getContributorAttributes() {
        if (contributorAttributes == null) {
            return Collections.emptyMap();
        }
        return contributorAttributes;
    }

    public boolean hasContributorAttributes() {
        return contributorAttributes != null;
    }

    /**
     * An explanation for the contributor's current state, providing context about why it is in its current condition.
     */
    public String getStateReason() {
        return stateReason;
    }

    /**
     * The timestamp when the contributor last transitioned to its current state.
     */
    public Instant getStateTransitionedTimestamp() {
        return stateTransitionedTimestamp;
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
        AlarmContributor that = (AlarmContributor) other;
        return Objects.equals(this.contributorId, that.contributorId)
               && Objects.equals(this.stateReason, that.stateReason)
               && Objects.equals(this.stateTransitionedTimestamp, that.stateTransitionedTimestamp)
               && Objects.equals(this.contributorAttributes, that.contributorAttributes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(contributorId);
        $hc = 31 * $hc + Objects.hashCode(contributorAttributes);
        $hc = 31 * $hc + Objects.hashCode(stateReason);
        $hc = 31 * $hc + Objects.hashCode(stateTransitionedTimestamp);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (contributorId != null) {
            serializer.writeString($SCHEMA_CONTRIBUTOR_ID, contributorId);
        }
        if (contributorAttributes != null) {
            serializer.writeMap($SCHEMA_CONTRIBUTOR_ATTRIBUTES, contributorAttributes, contributorAttributes.size(), SharedSerde.ContributorAttributesSerializer.INSTANCE);
        }
        if (stateReason != null) {
            serializer.writeString($SCHEMA_STATE_REASON, stateReason);
        }
        if (stateTransitionedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, stateTransitionedTimestamp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_ID, member, contributorId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_ATTRIBUTES, member, contributorAttributes);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, stateReason);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, stateTransitionedTimestamp);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AlarmContributor}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.contributorId(this.contributorId);
        builder.contributorAttributes(this.contributorAttributes);
        builder.stateReason(this.stateReason);
        builder.stateTransitionedTimestamp(this.stateTransitionedTimestamp);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AlarmContributor}.
     */
    public static final class Builder implements ShapeBuilder<AlarmContributor> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String contributorId;
        private Map<String, String> contributorAttributes;
        private String stateReason;
        private Instant stateTransitionedTimestamp;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_CONTRIBUTOR_ID);
            tracker.setMember($SCHEMA_CONTRIBUTOR_ATTRIBUTES);
            tracker.setMember($SCHEMA_STATE_REASON);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The unique identifier for this alarm contributor.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder contributorId(String contributorId) {
            this.contributorId = Objects.requireNonNull(contributorId, "contributorId cannot be null");
            tracker.setMember($SCHEMA_CONTRIBUTOR_ID);
            return this;
        }

        /**
         * A map of attributes that describe the contributor, such as metric dimensions and other identifying
         * characteristics.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder contributorAttributes(Map<String, String> contributorAttributes) {
            this.contributorAttributes = Objects.requireNonNull(contributorAttributes, "contributorAttributes cannot be null");
            tracker.setMember($SCHEMA_CONTRIBUTOR_ATTRIBUTES);
            return this;
        }

        /**
         * An explanation for the contributor's current state, providing context about why it is in its current condition.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder stateReason(String stateReason) {
            this.stateReason = Objects.requireNonNull(stateReason, "stateReason cannot be null");
            tracker.setMember($SCHEMA_STATE_REASON);
            return this;
        }

        /**
         * The timestamp when the contributor last transitioned to its current state.
         *
         * @return this builder.
         */
        public Builder stateTransitionedTimestamp(Instant stateTransitionedTimestamp) {
            this.stateTransitionedTimestamp = stateTransitionedTimestamp;
            return this;
        }

        @Override
        public AlarmContributor build() {
            tracker.validate();
            return new AlarmContributor(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> contributorId((String) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_ID, member, value));
                case 1 -> contributorAttributes((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_ATTRIBUTES, member, value));
                case 2 -> stateReason((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, value));
                case 3 -> stateTransitionedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AlarmContributor> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_CONTRIBUTOR_ID)) {
                contributorId("");
            }
            if (!tracker.checkMember($SCHEMA_CONTRIBUTOR_ATTRIBUTES)) {
                contributorAttributes(Collections.emptyMap());
            }
            if (!tracker.checkMember($SCHEMA_STATE_REASON)) {
                stateReason("");
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
                    case 0 -> builder.contributorId(de.readString(member));
                    case 1 -> builder.contributorAttributes(SharedSerde.deserializeContributorAttributes(member, de));
                    case 2 -> builder.stateReason(de.readString(member));
                    case 3 -> builder.stateTransitionedTimestamp(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
