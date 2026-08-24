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
 * The evaluation window that an alarm uses to select the range of metric data that it evaluates each time it runs. This
 * is a union type. Set exactly one of its members, <code>SlidingWindow</code> or <code>WallClockWindow</code>. If you
 * don't set <code>EvaluationWindow</code>, the alarm uses a <code>SlidingWindow</code> by default.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-evaluation-window.html">Alarm evaluation windows</a> in the <i>CloudWatch User Guide</i>.
 */
@SmithyGenerated
public sealed interface EvaluationWindow extends SerializableStruct {
    Schema $SCHEMA = Schemas.EVALUATION_WINDOW;

    ShapeId $ID = $SCHEMA.id();

    <T> T getValue();

    @Override
    default Schema schema() {
        return $SCHEMA;
    }

    @Override
    default <T> T getMemberValue(Schema member) {
        return SchemaUtils.validateMemberInSchema($SCHEMA, member, getValue());
    }

    /**
     * A wall clock window, which aligns the evaluated range to fixed clock boundaries that match the alarm's period, such
     * as the top of the hour, midnight, or the start of the calendar week.
     */
    @SmithyGenerated
    record WallClockWindowMember(WallClockWindow wallClockWindow) implements EvaluationWindow {
        private static final Schema $SCHEMA_WALL_CLOCK_WINDOW = $SCHEMA.member("WallClockWindow");
        public WallClockWindowMember {
            Objects.requireNonNull(wallClockWindow, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_WALL_CLOCK_WINDOW, wallClockWindow);
        }

        /**
         * A wall clock window, which aligns the evaluated range to fixed clock boundaries that match the alarm's period,
         * such as the top of the hour, midnight, or the start of the calendar week.
         */
        @Override
        public WallClockWindow getValue() {
            return wallClockWindow;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * A sliding window, which advances each time the alarm is evaluated, forming a rolling time window. This is the default
     * evaluation window.
     */
    @SmithyGenerated
    record SlidingWindowMember(SlidingWindow slidingWindow) implements EvaluationWindow {
        private static final Schema $SCHEMA_SLIDING_WINDOW = $SCHEMA.member("SlidingWindow");
        public SlidingWindowMember {
            Objects.requireNonNull(slidingWindow, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_SLIDING_WINDOW, slidingWindow);
        }

        /**
         * A sliding window, which advances each time the alarm is evaluated, forming a rolling time window. This is the
         * default evaluation window.
         */
        @Override
        public SlidingWindow getValue() {
            return slidingWindow;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String memberName) implements EvaluationWindow {
        @Override
        public void serialize(ShapeSerializer serializer) {
            throw new UnsupportedOperationException("Cannot serialize union with unknown member " + this.memberName);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {}

        @Override
        public String getValue() {
            return memberName;
        }

        private record $Hidden() implements EvaluationWindow {
            @Override
            public void serializeMembers(ShapeSerializer serializer) {}

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getValue() {
                return null;
            }
        }
    }

    interface BuildStage {
        EvaluationWindow build();
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EvaluationWindow}.
     */
    final class Builder implements ShapeBuilder<EvaluationWindow>, BuildStage {
        private EvaluationWindow value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        public BuildStage wallClockWindow(WallClockWindow value) {
            return setValue(new WallClockWindowMember(value));
        }

        public BuildStage slidingWindow(SlidingWindow value) {
            return setValue(new SlidingWindowMember(value));
        }

        public BuildStage $unknownMember(String memberName) {
            return setValue(new $Unknown(memberName));
        }

        private BuildStage setValue(EvaluationWindow value) {
            if (this.value != null) {
                throw new IllegalArgumentException("Only one value may be set for unions");
            }
            this.value = value;
            return this;
        }

        @Override
        public EvaluationWindow build() {
            return Objects.requireNonNull(value, "no union value set");
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> wallClockWindow((WallClockWindow) SchemaUtils.validateSameMember(WallClockWindowMember.$SCHEMA_WALL_CLOCK_WINDOW, member, value));
                case 1 -> slidingWindow((SlidingWindow) SchemaUtils.validateSameMember(SlidingWindowMember.$SCHEMA_SLIDING_WINDOW, member, value));
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
                    case 0 -> builder.wallClockWindow(WallClockWindow.builder().deserializeMember(de, member).build());
                    case 1 -> builder.slidingWindow(SlidingWindow.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }

            @Override
            public void unknownMember(Builder builder, String memberName) {
                builder.$unknownMember(memberName);
            }
        }
    }
}
