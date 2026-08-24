package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
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

@SmithyGenerated
public final class DeleteInsightRulesOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_INSIGHT_RULES_OUTPUT;
    private static final Schema $SCHEMA_FAILURES = $SCHEMA.member("Failures");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<PartialFailure> failures;

    private DeleteInsightRulesOutput(Builder builder) {
        this.failures = builder.failures == null ? null : Collections.unmodifiableList(builder.failures);
    }

    /**
     * An array listing the rules that could not be deleted. You cannot delete built-in rules.
     */
    public List<PartialFailure> getFailures() {
        if (failures == null) {
            return Collections.emptyList();
        }
        return failures;
    }

    public boolean hasFailures() {
        return failures != null;
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
        DeleteInsightRulesOutput that = (DeleteInsightRulesOutput) other;
        return Objects.equals(this.failures, that.failures);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(failures);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (failures != null) {
            serializer.writeList($SCHEMA_FAILURES, failures, failures.size(), SharedSerde.BatchFailuresSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURES, member, failures);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteInsightRulesOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.failures(this.failures);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteInsightRulesOutput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteInsightRulesOutput> {
        private List<PartialFailure> failures;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array listing the rules that could not be deleted. You cannot delete built-in rules.
         *
         * @return this builder.
         */
        public Builder failures(List<PartialFailure> failures) {
            this.failures = failures;
            return this;
        }

        @Override
        public DeleteInsightRulesOutput build() {
            return new DeleteInsightRulesOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> failures((List<PartialFailure>) SchemaUtils.validateSameMember($SCHEMA_FAILURES, member, value));
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
                    case 0 -> builder.failures(SharedSerde.deserializeBatchFailures(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
