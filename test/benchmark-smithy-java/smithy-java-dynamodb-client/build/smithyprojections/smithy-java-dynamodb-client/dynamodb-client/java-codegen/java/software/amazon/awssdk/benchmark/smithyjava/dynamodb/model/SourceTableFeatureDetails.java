package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

/**
 * Contains the details of the features enabled on the table when the backup was created. For example, LSIs, GSIs,
 * streams, TTL.
 */
@SmithyGenerated
public final class SourceTableFeatureDetails implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SOURCE_TABLE_FEATURE_DETAILS;
    private static final Schema $SCHEMA_LOCAL_SECONDARY_INDEXES = $SCHEMA.member("LocalSecondaryIndexes");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_STREAM_DESCRIPTION = $SCHEMA.member("StreamDescription");
    private static final Schema $SCHEMA_TIME_TO_LIVE_DESCRIPTION = $SCHEMA.member("TimeToLiveDescription");
    private static final Schema $SCHEMA_SSE_DESCRIPTION = $SCHEMA.member("SSEDescription");
    private static final Schema $SCHEMA_VECTOR_INDEXES = $SCHEMA.member("VectorIndexes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<LocalSecondaryIndexInfo> localSecondaryIndexes;
    private final transient List<GlobalSecondaryIndexInfo> globalSecondaryIndexes;
    private final transient StreamSpecification streamDescription;
    private final transient TimeToLiveDescription timeToLiveDescription;
    private final transient SSEDescription sseDescription;
    private final transient List<VectorIndexInfo> vectorIndexes;

    private SourceTableFeatureDetails(Builder builder) {
        this.localSecondaryIndexes = builder.localSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.localSecondaryIndexes);
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.streamDescription = builder.streamDescription;
        this.timeToLiveDescription = builder.timeToLiveDescription;
        this.sseDescription = builder.sseDescription;
        this.vectorIndexes = builder.vectorIndexes == null ? null : Collections.unmodifiableList(builder.vectorIndexes);
    }

    /**
     * Represents the LSI properties for the table when the backup was created. It includes the IndexName, KeySchema and
     * Projection for the LSIs on the table at the time of backup.
     */
    public List<LocalSecondaryIndexInfo> getLocalSecondaryIndexes() {
        if (localSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return localSecondaryIndexes;
    }

    public boolean hasLocalSecondaryIndexes() {
        return localSecondaryIndexes != null;
    }

    /**
     * Represents the GSI properties for the table when the backup was created. It includes the IndexName, KeySchema,
     * Projection, and ProvisionedThroughput for the GSIs on the table at the time of backup.
     */
    public List<GlobalSecondaryIndexInfo> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    /**
     * Stream settings on the table when the backup was created.
     */
    public StreamSpecification getStreamDescription() {
        return streamDescription;
    }

    /**
     * Time to Live settings on the table when the backup was created.
     */
    public TimeToLiveDescription getTimeToLiveDescription() {
        return timeToLiveDescription;
    }

    /**
     * The description of the server-side encryption status on the table when the backup was created.
     */
    public SSEDescription getSseDescription() {
        return sseDescription;
    }

    /**
     * The vector index properties for the table at the time the backup was created, including the index name, vector
     * attribute, dimensions, distance function, search schema, and projection.
     */
    public List<VectorIndexInfo> getVectorIndexes() {
        if (vectorIndexes == null) {
            return Collections.emptyList();
        }
        return vectorIndexes;
    }

    public boolean hasVectorIndexes() {
        return vectorIndexes != null;
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
        SourceTableFeatureDetails that = (SourceTableFeatureDetails) other;
        return Objects.equals(this.streamDescription, that.streamDescription)
               && Objects.equals(this.timeToLiveDescription, that.timeToLiveDescription)
               && Objects.equals(this.sseDescription, that.sseDescription)
               && Objects.equals(this.localSecondaryIndexes, that.localSecondaryIndexes)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes)
               && Objects.equals(this.vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(localSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(streamDescription);
        $hc = 31 * $hc + Objects.hashCode(timeToLiveDescription);
        $hc = 31 * $hc + Objects.hashCode(sseDescription);
        $hc = 31 * $hc + Objects.hashCode(vectorIndexes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (localSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_LOCAL_SECONDARY_INDEXES, localSecondaryIndexes, localSecondaryIndexes.size(), SharedSerde.LocalSecondaryIndexesSerializer.INSTANCE);
        }
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.GlobalSecondaryIndexesSerializer.INSTANCE);
        }
        if (streamDescription != null) {
            serializer.writeStruct($SCHEMA_STREAM_DESCRIPTION, streamDescription);
        }
        if (timeToLiveDescription != null) {
            serializer.writeStruct($SCHEMA_TIME_TO_LIVE_DESCRIPTION, timeToLiveDescription);
        }
        if (sseDescription != null) {
            serializer.writeStruct($SCHEMA_SSE_DESCRIPTION, sseDescription);
        }
        if (vectorIndexes != null) {
            serializer.writeList($SCHEMA_VECTOR_INDEXES, vectorIndexes, vectorIndexes.size(), SharedSerde.VectorIndexesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, localSecondaryIndexes);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_DESCRIPTION, member, streamDescription);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_DESCRIPTION, member, timeToLiveDescription);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_DESCRIPTION, member, sseDescription);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, vectorIndexes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SourceTableFeatureDetails}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.localSecondaryIndexes(this.localSecondaryIndexes);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.streamDescription(this.streamDescription);
        builder.timeToLiveDescription(this.timeToLiveDescription);
        builder.sseDescription(this.sseDescription);
        builder.vectorIndexes(this.vectorIndexes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SourceTableFeatureDetails}.
     */
    public static final class Builder implements ShapeBuilder<SourceTableFeatureDetails> {
        private List<LocalSecondaryIndexInfo> localSecondaryIndexes;
        private List<GlobalSecondaryIndexInfo> globalSecondaryIndexes;
        private StreamSpecification streamDescription;
        private TimeToLiveDescription timeToLiveDescription;
        private SSEDescription sseDescription;
        private List<VectorIndexInfo> vectorIndexes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the LSI properties for the table when the backup was created. It includes the IndexName, KeySchema and
         * Projection for the LSIs on the table at the time of backup.
         *
         * @return this builder.
         */
        public Builder localSecondaryIndexes(List<LocalSecondaryIndexInfo> localSecondaryIndexes) {
            this.localSecondaryIndexes = localSecondaryIndexes;
            return this;
        }

        /**
         * Represents the GSI properties for the table when the backup was created. It includes the IndexName, KeySchema,
         * Projection, and ProvisionedThroughput for the GSIs on the table at the time of backup.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<GlobalSecondaryIndexInfo> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * Stream settings on the table when the backup was created.
         *
         * @return this builder.
         */
        public Builder streamDescription(StreamSpecification streamDescription) {
            this.streamDescription = streamDescription;
            return this;
        }

        /**
         * Time to Live settings on the table when the backup was created.
         *
         * @return this builder.
         */
        public Builder timeToLiveDescription(TimeToLiveDescription timeToLiveDescription) {
            this.timeToLiveDescription = timeToLiveDescription;
            return this;
        }

        /**
         * The description of the server-side encryption status on the table when the backup was created.
         *
         * @return this builder.
         */
        public Builder sseDescription(SSEDescription sseDescription) {
            this.sseDescription = sseDescription;
            return this;
        }

        /**
         * The vector index properties for the table at the time the backup was created, including the index name, vector
         * attribute, dimensions, distance function, search schema, and projection.
         *
         * @return this builder.
         */
        public Builder vectorIndexes(List<VectorIndexInfo> vectorIndexes) {
            this.vectorIndexes = vectorIndexes;
            return this;
        }

        @Override
        public SourceTableFeatureDetails build() {
            return new SourceTableFeatureDetails(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> localSecondaryIndexes((List<LocalSecondaryIndexInfo>) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEXES, member, value));
                case 1 -> globalSecondaryIndexes((List<GlobalSecondaryIndexInfo>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 2 -> streamDescription((StreamSpecification) SchemaUtils.validateSameMember($SCHEMA_STREAM_DESCRIPTION, member, value));
                case 3 -> timeToLiveDescription((TimeToLiveDescription) SchemaUtils.validateSameMember($SCHEMA_TIME_TO_LIVE_DESCRIPTION, member, value));
                case 4 -> sseDescription((SSEDescription) SchemaUtils.validateSameMember($SCHEMA_SSE_DESCRIPTION, member, value));
                case 5 -> vectorIndexes((List<VectorIndexInfo>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEXES, member, value));
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
                    case 0 -> builder.localSecondaryIndexes(SharedSerde.deserializeLocalSecondaryIndexes(member, de));
                    case 1 -> builder.globalSecondaryIndexes(SharedSerde.deserializeGlobalSecondaryIndexes(member, de));
                    case 2 -> builder.streamDescription(StreamSpecification.builder().deserializeMember(de, member).build());
                    case 3 -> builder.timeToLiveDescription(TimeToLiveDescription.builder().deserializeMember(de, member).build());
                    case 4 -> builder.sseDescription(SSEDescription.builder().deserializeMember(de, member).build());
                    case 5 -> builder.vectorIndexes(SharedSerde.deserializeVectorIndexes(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
