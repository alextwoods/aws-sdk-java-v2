package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * Contains the configuration that determines how a PromQL alarm evaluates its contributors, including the query to run
 * and the durations that define when contributors transition between states.
 */
@SmithyGenerated
public final class AlarmPromQLCriteria implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ALARM_PROM_QL_CRITERIA;
    private static final Schema $SCHEMA_QUERY = $SCHEMA.member("Query");
    private static final Schema $SCHEMA_PENDING_PERIOD = $SCHEMA.member("PendingPeriod");
    private static final Schema $SCHEMA_RECOVERY_PERIOD = $SCHEMA.member("RecoveryPeriod");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String query;
    private final transient Integer pendingPeriod;
    private final transient Integer recoveryPeriod;

    private AlarmPromQLCriteria(Builder builder) {
        this.query = builder.query;
        this.pendingPeriod = builder.pendingPeriod;
        this.recoveryPeriod = builder.recoveryPeriod;
    }

    /**
     * The PromQL query that the alarm evaluates. The query must return a result of vector type. Each entry in the
     * vector result represents an alarm contributor.
     */
    public String getQuery() {
        return query;
    }

    /**
     * The duration, in seconds, that a contributor must be continuously breaching before it transitions to the
     * <code>ALARM</code> state.
     */
    public Integer getPendingPeriod() {
        return pendingPeriod;
    }

    /**
     * The duration, in seconds, that a contributor must continuously not be breaching before it transitions back to the
     * <code>OK</code> state.
     */
    public Integer getRecoveryPeriod() {
        return recoveryPeriod;
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
        AlarmPromQLCriteria that = (AlarmPromQLCriteria) other;
        return Objects.equals(this.pendingPeriod, that.pendingPeriod)
               && Objects.equals(this.recoveryPeriod, that.recoveryPeriod)
               && Objects.equals(this.query, that.query);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(query);
        $hc = 31 * $hc + Objects.hashCode(pendingPeriod);
        $hc = 31 * $hc + Objects.hashCode(recoveryPeriod);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (query != null) {
            serializer.writeString($SCHEMA_QUERY, query);
        }
        if (pendingPeriod != null) {
            serializer.writeInteger($SCHEMA_PENDING_PERIOD, pendingPeriod);
        }
        if (recoveryPeriod != null) {
            serializer.writeInteger($SCHEMA_RECOVERY_PERIOD, recoveryPeriod);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY, member, query);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PENDING_PERIOD, member, pendingPeriod);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECOVERY_PERIOD, member, recoveryPeriod);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AlarmPromQLCriteria}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.query(this.query);
        builder.pendingPeriod(this.pendingPeriod);
        builder.recoveryPeriod(this.recoveryPeriod);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AlarmPromQLCriteria}.
     */
    public static final class Builder implements ShapeBuilder<AlarmPromQLCriteria> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String query;
        private Integer pendingPeriod;
        private Integer recoveryPeriod;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_QUERY);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The PromQL query that the alarm evaluates. The query must return a result of vector type. Each entry in the
         * vector result represents an alarm contributor.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder query(String query) {
            this.query = Objects.requireNonNull(query, "query cannot be null");
            tracker.setMember($SCHEMA_QUERY);
            return this;
        }

        /**
         * The duration, in seconds, that a contributor must be continuously breaching before it transitions to the
         * <code>ALARM</code> state.
         *
         * @return this builder.
         */
        public Builder pendingPeriod(Integer pendingPeriod) {
            this.pendingPeriod = pendingPeriod;
            return this;
        }

        /**
         * The duration, in seconds, that a contributor must continuously not be breaching before it transitions back to the
         * <code>OK</code> state.
         *
         * @return this builder.
         */
        public Builder recoveryPeriod(Integer recoveryPeriod) {
            this.recoveryPeriod = recoveryPeriod;
            return this;
        }

        @Override
        public AlarmPromQLCriteria build() {
            tracker.validate();
            return new AlarmPromQLCriteria(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> query((String) SchemaUtils.validateSameMember($SCHEMA_QUERY, member, value));
                case 1 -> pendingPeriod((Integer) SchemaUtils.validateSameMember($SCHEMA_PENDING_PERIOD, member, value));
                case 2 -> recoveryPeriod((Integer) SchemaUtils.validateSameMember($SCHEMA_RECOVERY_PERIOD, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AlarmPromQLCriteria> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_QUERY)) {
                query("");
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
                    case 0 -> builder.query(de.readString(member));
                    case 1 -> builder.pendingPeriod(de.readInteger(member));
                    case 2 -> builder.recoveryPeriod(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
