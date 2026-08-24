package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
 * This structure contains the configuration information about one metric stream.
 */
@SmithyGenerated
public final class MetricStreamEntry implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_STREAM_ENTRY;
    private static final Schema $SCHEMA_ARN = $SCHEMA.member("Arn");
    private static final Schema $SCHEMA_CREATION_DATE = $SCHEMA.member("CreationDate");
    private static final Schema $SCHEMA_LAST_UPDATE_DATE = $SCHEMA.member("LastUpdateDate");
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_FIREHOSE_ARN = $SCHEMA.member("FirehoseArn");
    private static final Schema $SCHEMA_STATE = $SCHEMA.member("State");
    private static final Schema $SCHEMA_OUTPUT_FORMAT = $SCHEMA.member("OutputFormat");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String arn;
    private final transient Instant creationDate;
    private final transient Instant lastUpdateDate;
    private final transient String name;
    private final transient String firehoseArn;
    private final transient String state;
    private final transient MetricStreamOutputFormat outputFormat;

    private MetricStreamEntry(Builder builder) {
        this.arn = builder.arn;
        this.creationDate = builder.creationDate;
        this.lastUpdateDate = builder.lastUpdateDate;
        this.name = builder.name;
        this.firehoseArn = builder.firehoseArn;
        this.state = builder.state;
        this.outputFormat = builder.outputFormat;
    }

    /**
     * The ARN of the metric stream.
     */
    public String getArn() {
        return arn;
    }

    /**
     * The date that the metric stream was originally created.
     */
    public Instant getCreationDate() {
        return creationDate;
    }

    /**
     * The date that the configuration of this metric stream was most recently updated.
     */
    public Instant getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * The name of the metric stream.
     */
    public String getName() {
        return name;
    }

    /**
     * The ARN of the Kinesis Firehose devlivery stream that is used for this metric stream.
     */
    public String getFirehoseArn() {
        return firehoseArn;
    }

    /**
     * The current state of this stream. Valid values are <code>running</code> and <code>stopped</code>.
     */
    public String getState() {
        return state;
    }

    /**
     * The output format of this metric stream. Valid values are <code>json</code>, <code>opentelemetry1.0</code>, and
     * <code>opentelemetry0.7</code>.
     */
    public MetricStreamOutputFormat getOutputFormat() {
        return outputFormat;
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
        MetricStreamEntry that = (MetricStreamEntry) other;
        return Objects.equals(this.arn, that.arn)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.firehoseArn, that.firehoseArn)
               && Objects.equals(this.state, that.state)
               && Objects.equals(this.outputFormat, that.outputFormat)
               && Objects.equals(this.creationDate, that.creationDate)
               && Objects.equals(this.lastUpdateDate, that.lastUpdateDate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(arn);
        $hc = 31 * $hc + Objects.hashCode(creationDate);
        $hc = 31 * $hc + Objects.hashCode(lastUpdateDate);
        $hc = 31 * $hc + Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(firehoseArn);
        $hc = 31 * $hc + Objects.hashCode(state);
        $hc = 31 * $hc + Objects.hashCode(outputFormat);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (arn != null) {
            serializer.writeString($SCHEMA_ARN, arn);
        }
        if (creationDate != null) {
            serializer.writeTimestamp($SCHEMA_CREATION_DATE, creationDate);
        }
        if (lastUpdateDate != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATE_DATE, lastUpdateDate);
        }
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
        if (firehoseArn != null) {
            serializer.writeString($SCHEMA_FIREHOSE_ARN, firehoseArn);
        }
        if (state != null) {
            serializer.writeString($SCHEMA_STATE, state);
        }
        if (outputFormat != null) {
            serializer.writeString($SCHEMA_OUTPUT_FORMAT, outputFormat.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARN, member, arn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE, member, creationDate);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE, member, lastUpdateDate);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_FIREHOSE_ARN, member, firehoseArn);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE, member, state);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, outputFormat);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricStreamEntry}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.arn(this.arn);
        builder.creationDate(this.creationDate);
        builder.lastUpdateDate(this.lastUpdateDate);
        builder.name(this.name);
        builder.firehoseArn(this.firehoseArn);
        builder.state(this.state);
        builder.outputFormat(this.outputFormat);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricStreamEntry}.
     */
    public static final class Builder implements ShapeBuilder<MetricStreamEntry> {
        private String arn;
        private Instant creationDate;
        private Instant lastUpdateDate;
        private String name;
        private String firehoseArn;
        private String state;
        private MetricStreamOutputFormat outputFormat;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ARN of the metric stream.
         *
         * @return this builder.
         */
        public Builder arn(String arn) {
            this.arn = arn;
            return this;
        }

        /**
         * The date that the metric stream was originally created.
         *
         * @return this builder.
         */
        public Builder creationDate(Instant creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        /**
         * The date that the configuration of this metric stream was most recently updated.
         *
         * @return this builder.
         */
        public Builder lastUpdateDate(Instant lastUpdateDate) {
            this.lastUpdateDate = lastUpdateDate;
            return this;
        }

        /**
         * The name of the metric stream.
         *
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The ARN of the Kinesis Firehose devlivery stream that is used for this metric stream.
         *
         * @return this builder.
         */
        public Builder firehoseArn(String firehoseArn) {
            this.firehoseArn = firehoseArn;
            return this;
        }

        /**
         * The current state of this stream. Valid values are <code>running</code> and <code>stopped</code>.
         *
         * @return this builder.
         */
        public Builder state(String state) {
            this.state = state;
            return this;
        }

        /**
         * The output format of this metric stream. Valid values are <code>json</code>, <code>opentelemetry1.0</code>, and
         * <code>opentelemetry0.7</code>.
         *
         * @return this builder.
         */
        public Builder outputFormat(MetricStreamOutputFormat outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        @Override
        public MetricStreamEntry build() {
            return new MetricStreamEntry(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> arn((String) SchemaUtils.validateSameMember($SCHEMA_ARN, member, value));
                case 1 -> creationDate((Instant) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE, member, value));
                case 2 -> lastUpdateDate((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE, member, value));
                case 3 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 4 -> firehoseArn((String) SchemaUtils.validateSameMember($SCHEMA_FIREHOSE_ARN, member, value));
                case 5 -> state((String) SchemaUtils.validateSameMember($SCHEMA_STATE, member, value));
                case 6 -> outputFormat((MetricStreamOutputFormat) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, value));
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
                    case 0 -> builder.arn(de.readString(member));
                    case 1 -> builder.creationDate(de.readTimestamp(member));
                    case 2 -> builder.lastUpdateDate(de.readTimestamp(member));
                    case 3 -> builder.name(de.readString(member));
                    case 4 -> builder.firehoseArn(de.readString(member));
                    case 5 -> builder.state(de.readString(member));
                    case 6 -> builder.outputFormat(MetricStreamOutputFormat.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
