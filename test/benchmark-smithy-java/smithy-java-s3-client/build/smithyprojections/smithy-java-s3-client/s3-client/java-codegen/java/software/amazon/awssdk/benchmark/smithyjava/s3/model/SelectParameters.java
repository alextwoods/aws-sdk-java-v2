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
 * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to use
 * the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
 *
 * <p>Describes the parameters for Select job types.
 *
 * <p>Learn <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">How to optimize querying your data in Amazon S3</a> using <a href="https://docs.aws.amazon.com/athena/latest/ug/what-is.html">Amazon Athena</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/transforming-objects.html">S3 Object Lambda</a>,
 * or client-side filtering.
 */
@SmithyGenerated
public final class SelectParameters implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.SELECT_PARAMETERS;
    private static final Schema $SCHEMA_INPUT_SERIALIZATION = $SCHEMA.member("InputSerialization");
    private static final Schema $SCHEMA_EXPRESSION_TYPE = $SCHEMA.member("ExpressionType");
    private static final Schema $SCHEMA_EXPRESSION = $SCHEMA.member("Expression");
    private static final Schema $SCHEMA_OUTPUT_SERIALIZATION = $SCHEMA.member("OutputSerialization");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient InputSerialization inputSerialization;
    private final transient ExpressionType expressionType;
    private final transient String expression;
    private final transient OutputSerialization outputSerialization;

    private SelectParameters(Builder builder) {
        this.inputSerialization = builder.inputSerialization;
        this.expressionType = builder.expressionType;
        this.expression = builder.expression;
        this.outputSerialization = builder.outputSerialization;
    }

    /**
     * Describes the serialization format of the object.
     */
    public InputSerialization getInputSerialization() {
        return inputSerialization;
    }

    /**
     * The type of the provided expression (for example, SQL).
     */
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    /**
     * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to
     * use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
     *
     * <p>The expression that is used to query the object.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Describes how the results of the Select job are serialized.
     */
    public OutputSerialization getOutputSerialization() {
        return outputSerialization;
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
        SelectParameters that = (SelectParameters) other;
        return Objects.equals(this.expression, that.expression)
               && Objects.equals(this.expressionType, that.expressionType)
               && Objects.equals(this.inputSerialization, that.inputSerialization)
               && Objects.equals(this.outputSerialization, that.outputSerialization);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(inputSerialization);
        $hc = 31 * $hc + Objects.hashCode(expressionType);
        $hc = 31 * $hc + Objects.hashCode(expression);
        $hc = 31 * $hc + Objects.hashCode(outputSerialization);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (inputSerialization != null) {
            serializer.writeStruct($SCHEMA_INPUT_SERIALIZATION, inputSerialization);
        }
        serializer.writeString($SCHEMA_EXPRESSION_TYPE, expressionType.getValue());
        serializer.writeString($SCHEMA_EXPRESSION, expression);
        if (outputSerialization != null) {
            serializer.writeStruct($SCHEMA_OUTPUT_SERIALIZATION, outputSerialization);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INPUT_SERIALIZATION, member, inputSerialization);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_TYPE, member, expressionType);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, expression);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_SERIALIZATION, member, outputSerialization);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SelectParameters}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.inputSerialization(this.inputSerialization);
        builder.expressionType(this.expressionType);
        builder.expression(this.expression);
        builder.outputSerialization(this.outputSerialization);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SelectParameters}.
     */
    public static final class Builder implements ShapeBuilder<SelectParameters> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private InputSerialization inputSerialization;
        private ExpressionType expressionType;
        private String expression;
        private OutputSerialization outputSerialization;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Describes the serialization format of the object.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder inputSerialization(InputSerialization inputSerialization) {
            this.inputSerialization = Objects.requireNonNull(inputSerialization, "inputSerialization cannot be null");
            tracker.setMember($SCHEMA_INPUT_SERIALIZATION);
            return this;
        }

        /**
         * The type of the provided expression (for example, SQL).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expressionType(ExpressionType expressionType) {
            this.expressionType = Objects.requireNonNull(expressionType, "expressionType cannot be null");
            tracker.setMember($SCHEMA_EXPRESSION_TYPE);
            return this;
        }

        /**
         * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to
         * use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
         *
         * <p>The expression that is used to query the object.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expression(String expression) {
            this.expression = Objects.requireNonNull(expression, "expression cannot be null");
            tracker.setMember($SCHEMA_EXPRESSION);
            return this;
        }

        /**
         * Describes how the results of the Select job are serialized.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder outputSerialization(OutputSerialization outputSerialization) {
            this.outputSerialization = Objects.requireNonNull(outputSerialization, "outputSerialization cannot be null");
            tracker.setMember($SCHEMA_OUTPUT_SERIALIZATION);
            return this;
        }

        @Override
        public SelectParameters build() {
            tracker.validate();
            return new SelectParameters(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> inputSerialization((InputSerialization) SchemaUtils.validateSameMember($SCHEMA_INPUT_SERIALIZATION, member, value));
                case 1 -> expressionType((ExpressionType) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_TYPE, member, value));
                case 2 -> expression((String) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, value));
                case 3 -> outputSerialization((OutputSerialization) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_SERIALIZATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SelectParameters> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_INPUT_SERIALIZATION)) {
                tracker.setMember($SCHEMA_INPUT_SERIALIZATION);
            }
            if (!tracker.checkMember($SCHEMA_EXPRESSION_TYPE)) {
                expressionType(ExpressionType.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_EXPRESSION)) {
                expression("");
            }
            if (!tracker.checkMember($SCHEMA_OUTPUT_SERIALIZATION)) {
                tracker.setMember($SCHEMA_OUTPUT_SERIALIZATION);
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
                    case 0 -> builder.inputSerialization(InputSerialization.builder().deserializeMember(de, member).build());
                    case 1 -> builder.expressionType(ExpressionType.builder().deserializeMember(de, member).build());
                    case 2 -> builder.expression(de.readString(member));
                    case 3 -> builder.outputSerialization(OutputSerialization.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
