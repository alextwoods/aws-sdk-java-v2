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
public final class ListMetricStreamsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_METRIC_STREAMS_OUTPUT;
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_ENTRIES = $SCHEMA.member("Entries");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String nextToken;
    private final transient List<MetricStreamEntry> entries;

    private ListMetricStreamsOutput(Builder builder) {
        this.nextToken = builder.nextToken;
        this.entries = builder.entries == null ? null : Collections.unmodifiableList(builder.entries);
    }

    /**
     * The token that marks the start of the next batch of returned results. You can use this token in a subsequent
     * operation to get the next batch of results.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * The array of metric stream information.
     */
    public List<MetricStreamEntry> getEntries() {
        if (entries == null) {
            return Collections.emptyList();
        }
        return entries;
    }

    public boolean hasEntries() {
        return entries != null;
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
        ListMetricStreamsOutput that = (ListMetricStreamsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.entries, that.entries);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(entries);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (entries != null) {
            serializer.writeList($SCHEMA_ENTRIES, entries, entries.size(), SharedSerde.MetricStreamEntriesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENTRIES, member, entries);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListMetricStreamsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.nextToken(this.nextToken);
        builder.entries(this.entries);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListMetricStreamsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListMetricStreamsOutput> {
        private String nextToken;
        private List<MetricStreamEntry> entries;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The token that marks the start of the next batch of returned results. You can use this token in a subsequent
         * operation to get the next batch of results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * The array of metric stream information.
         *
         * @return this builder.
         */
        public Builder entries(List<MetricStreamEntry> entries) {
            this.entries = entries;
            return this;
        }

        @Override
        public ListMetricStreamsOutput build() {
            return new ListMetricStreamsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 1 -> entries((List<MetricStreamEntry>) SchemaUtils.validateSameMember($SCHEMA_ENTRIES, member, value));
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
                    case 0 -> builder.nextToken(de.readString(member));
                    case 1 -> builder.entries(SharedSerde.deserializeMetricStreamEntries(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
