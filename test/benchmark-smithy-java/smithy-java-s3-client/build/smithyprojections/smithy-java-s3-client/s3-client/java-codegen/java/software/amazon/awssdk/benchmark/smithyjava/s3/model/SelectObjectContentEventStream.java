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
 * The container for selecting objects from a content event stream.
 */
@SmithyGenerated
public sealed interface SelectObjectContentEventStream extends SerializableStruct {
    Schema $SCHEMA = Schemas6.SELECT_OBJECT_CONTENT_EVENT_STREAM;

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
     * The Records Event.
     */
    @SmithyGenerated
    record RecordsMember(RecordsEvent records) implements SelectObjectContentEventStream {
        private static final Schema $SCHEMA_RECORDS = $SCHEMA.member("Records");
        public RecordsMember {
            Objects.requireNonNull(records, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_RECORDS, records);
        }

        /**
         * The Records Event.
         */
        @Override
        public RecordsEvent getValue() {
            return records;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * The Stats Event.
     */
    @SmithyGenerated
    record StatsMember(StatsEvent stats) implements SelectObjectContentEventStream {
        private static final Schema $SCHEMA_STATS = $SCHEMA.member("Stats");
        public StatsMember {
            Objects.requireNonNull(stats, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_STATS, stats);
        }

        /**
         * The Stats Event.
         */
        @Override
        public StatsEvent getValue() {
            return stats;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * The Progress Event.
     */
    @SmithyGenerated
    record ProgressMember(ProgressEvent progress) implements SelectObjectContentEventStream {
        private static final Schema $SCHEMA_PROGRESS = $SCHEMA.member("Progress");
        public ProgressMember {
            Objects.requireNonNull(progress, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_PROGRESS, progress);
        }

        /**
         * The Progress Event.
         */
        @Override
        public ProgressEvent getValue() {
            return progress;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * The Continuation Event.
     */
    @SmithyGenerated
    record ContMember(ContinuationEvent cont) implements SelectObjectContentEventStream {
        private static final Schema $SCHEMA_CONT = $SCHEMA.member("Cont");
        public ContMember {
            Objects.requireNonNull(cont, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_CONT, cont);
        }

        /**
         * The Continuation Event.
         */
        @Override
        public ContinuationEvent getValue() {
            return cont;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * The End Event.
     */
    @SmithyGenerated
    record EndMember(EndEvent end) implements SelectObjectContentEventStream {
        private static final Schema $SCHEMA_END = $SCHEMA.member("End");
        public EndMember {
            Objects.requireNonNull(end, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_END, end);
        }

        /**
         * The End Event.
         */
        @Override
        public EndEvent getValue() {
            return end;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String memberName) implements SelectObjectContentEventStream {
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

        private record $Hidden() implements SelectObjectContentEventStream {
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
        SelectObjectContentEventStream build();
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SelectObjectContentEventStream}.
     */
    final class Builder implements ShapeBuilder<SelectObjectContentEventStream>, BuildStage {
        private SelectObjectContentEventStream value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        public BuildStage records(RecordsEvent value) {
            return setValue(new RecordsMember(value));
        }

        public BuildStage stats(StatsEvent value) {
            return setValue(new StatsMember(value));
        }

        public BuildStage progress(ProgressEvent value) {
            return setValue(new ProgressMember(value));
        }

        public BuildStage cont(ContinuationEvent value) {
            return setValue(new ContMember(value));
        }

        public BuildStage end(EndEvent value) {
            return setValue(new EndMember(value));
        }

        public BuildStage $unknownMember(String memberName) {
            return setValue(new $Unknown(memberName));
        }

        private BuildStage setValue(SelectObjectContentEventStream value) {
            if (this.value != null) {
                throw new IllegalArgumentException("Only one value may be set for unions");
            }
            this.value = value;
            return this;
        }

        @Override
        public SelectObjectContentEventStream build() {
            return Objects.requireNonNull(value, "no union value set");
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> records((RecordsEvent) SchemaUtils.validateSameMember(RecordsMember.$SCHEMA_RECORDS, member, value));
                case 1 -> stats((StatsEvent) SchemaUtils.validateSameMember(StatsMember.$SCHEMA_STATS, member, value));
                case 2 -> progress((ProgressEvent) SchemaUtils.validateSameMember(ProgressMember.$SCHEMA_PROGRESS, member, value));
                case 3 -> cont((ContinuationEvent) SchemaUtils.validateSameMember(ContMember.$SCHEMA_CONT, member, value));
                case 4 -> end((EndEvent) SchemaUtils.validateSameMember(EndMember.$SCHEMA_END, member, value));
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
                    case 0 -> builder.records(RecordsEvent.builder().deserializeMember(de, member).build());
                    case 1 -> builder.stats(StatsEvent.builder().deserializeMember(de, member).build());
                    case 2 -> builder.progress(ProgressEvent.builder().deserializeMember(de, member).build());
                    case 3 -> builder.cont(ContinuationEvent.builder().deserializeMember(de, member).build());
                    case 4 -> builder.end(EndEvent.builder().deserializeMember(de, member).build());
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
