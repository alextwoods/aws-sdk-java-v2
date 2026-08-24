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

/**
 * A set of metrics that are associated with an entity, such as a specific service or resource. Contains the entity and
 * the list of metric data associated with it.
 */
@SmithyGenerated
public final class EntityMetricData implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ENTITY_METRIC_DATA;
    private static final Schema $SCHEMA_ENTITY = $SCHEMA.member("Entity");
    private static final Schema $SCHEMA_METRIC_DATA = $SCHEMA.member("MetricData");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Entity entity;
    private final transient List<MetricDatum> metricData;

    private EntityMetricData(Builder builder) {
        this.entity = builder.entity;
        this.metricData = builder.metricData == null ? null : Collections.unmodifiableList(builder.metricData);
    }

    /**
     * The entity associated with the metrics.
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * The metric data.
     */
    public List<MetricDatum> getMetricData() {
        if (metricData == null) {
            return Collections.emptyList();
        }
        return metricData;
    }

    public boolean hasMetricData() {
        return metricData != null;
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
        EntityMetricData that = (EntityMetricData) other;
        return Objects.equals(this.entity, that.entity)
               && Objects.equals(this.metricData, that.metricData);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(entity);
        $hc = 31 * $hc + Objects.hashCode(metricData);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (entity != null) {
            serializer.writeStruct($SCHEMA_ENTITY, entity);
        }
        if (metricData != null) {
            serializer.writeList($SCHEMA_METRIC_DATA, metricData, metricData.size(), SharedSerde.MetricDataSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENTITY, member, entity);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA, member, metricData);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link EntityMetricData}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.entity(this.entity);
        builder.metricData(this.metricData);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EntityMetricData}.
     */
    public static final class Builder implements ShapeBuilder<EntityMetricData> {
        private Entity entity;
        private List<MetricDatum> metricData;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The entity associated with the metrics.
         *
         * @return this builder.
         */
        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        /**
         * The metric data.
         *
         * @return this builder.
         */
        public Builder metricData(List<MetricDatum> metricData) {
            this.metricData = metricData;
            return this;
        }

        @Override
        public EntityMetricData build() {
            return new EntityMetricData(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> entity((Entity) SchemaUtils.validateSameMember($SCHEMA_ENTITY, member, value));
                case 1 -> metricData((List<MetricDatum>) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA, member, value));
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
                    case 0 -> builder.entity(Entity.builder().deserializeMember(de, member).build());
                    case 1 -> builder.metricData(SharedSerde.deserializeMetricData(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
