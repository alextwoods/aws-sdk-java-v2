package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
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

@SmithyGenerated
public final class StartMetricStreamsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.START_METRIC_STREAMS_INPUT;
    private static final Schema $SCHEMA_NAMES = $SCHEMA.member("Names");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> names;

    private StartMetricStreamsInput(Builder builder) {
        this.names = builder.names == null ? null : Collections.unmodifiableList(builder.names);
    }

    /**
     * The array of the names of metric streams to start streaming.
     *
     * <p>This is an "all or nothing" operation. If you do not have permission to access all of the metric streams that
     * you list here, then none of the streams that you list in the operation will start streaming.
     */
    public List<String> getNames() {
        if (names == null) {
            return Collections.emptyList();
        }
        return names;
    }

    public boolean hasNames() {
        return names != null;
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
        StartMetricStreamsInput that = (StartMetricStreamsInput) other;
        return Objects.equals(this.names, that.names);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(names);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (names != null) {
            serializer.writeList($SCHEMA_NAMES, names, names.size(), SharedSerde.MetricStreamNamesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMES, member, names);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link StartMetricStreamsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.names(this.names);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StartMetricStreamsInput}.
     */
    public static final class Builder implements ShapeBuilder<StartMetricStreamsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<String> names;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAMES);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The array of the names of metric streams to start streaming.
         *
         * <p>This is an "all or nothing" operation. If you do not have permission to access all of the metric streams that
         * you list here, then none of the streams that you list in the operation will start streaming.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder names(List<String> names) {
            this.names = Objects.requireNonNull(names, "names cannot be null");
            tracker.setMember($SCHEMA_NAMES);
            return this;
        }

        @Override
        public StartMetricStreamsInput build() {
            tracker.validate();
            return new StartMetricStreamsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> names((List<String>) SchemaUtils.validateSameMember($SCHEMA_NAMES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<StartMetricStreamsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAMES)) {
                names(Collections.emptyList());
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
                    case 0 -> builder.names(SharedSerde.deserializeMetricStreamNames(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
