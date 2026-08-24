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
public final class PutMetricDataInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_METRIC_DATA_INPUT;
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_DATA = $SCHEMA.member("MetricData");
    private static final Schema $SCHEMA_ENTITY_METRIC_DATA = $SCHEMA.member("EntityMetricData");
    private static final Schema $SCHEMA_STRICT_ENTITY_VALIDATION = $SCHEMA.member("StrictEntityValidation");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String namespace;
    private final transient List<MetricDatum> metricData;
    private final transient List<EntityMetricData> entityMetricData;
    private final transient Boolean strictEntityValidation;

    private PutMetricDataInput(Builder builder) {
        this.namespace = builder.namespace;
        this.metricData = builder.metricData == null ? null : Collections.unmodifiableList(builder.metricData);
        this.entityMetricData = builder.entityMetricData == null ? null : Collections.unmodifiableList(builder.entityMetricData);
        this.strictEntityValidation = builder.strictEntityValidation;
    }

    /**
     * The namespace for the metric data. You can use ASCII characters for the namespace, except for control characters
     * which are not supported.
     *
     * <p>To avoid conflicts with Amazon Web Services service namespaces, you should not specify a namespace that begins
     * with <code>AWS/</code>
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The data for the metrics. Use this parameter if your metrics do not contain associated entities. The array can
     * include no more than 1000 metrics per call.
     *
     * <p>The limit of metrics allowed, 1000, is the sum of both <code>EntityMetricData</code> and <code>MetricData</code>
     * metrics.
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

    /**
     * Data for metrics that contain associated entity information. You can include up to two <code>EntityMetricData</code>
     * objects, each of which can contain a single <code>Entity</code> and associated metrics.
     *
     * <p>The limit of metrics allowed, 1000, is the sum of both <code>EntityMetricData</code> and <code>MetricData</code>
     * metrics.
     */
    public List<EntityMetricData> getEntityMetricData() {
        if (entityMetricData == null) {
            return Collections.emptyList();
        }
        return entityMetricData;
    }

    public boolean hasEntityMetricData() {
        return entityMetricData != null;
    }

    /**
     * Whether to accept valid metric data when an invalid entity is sent.
     *
     * <ul>
     *   <li>
     *     When set to <code>true</code>: Any validation error (for entity or metric data) will fail the entire
     *     request, and no data will be ingested. The failed operation will return a 400 result with the error.
     *   </li>
     *   <li>
     *     When set to <code>false</code>: Validation errors in the entity will not associate the metric with the
     *     entity, but the metric data will still be accepted and ingested. Validation errors in the metric data
     *     will fail the entire request, and no data will be ingested.In the case of an invalid entity, the
     *     operation will return a <code>200</code> status, but an additional response header will contain
     *     information about the validation errors. The new header, <code>X-Amzn-Failure-Message</code> is an
     *     enumeration of the following values:
     *
     *     <ul>
     *       <li>
     *         <code>InvalidEntity</code> - The provided entity is invalid.
     *       </li>
     *       <li>
     *         <code>InvalidKeyAttributes</code> - The provided <code>KeyAttributes</code> of an entity is
     *         invalid.
     *       </li>
     *       <li>
     *         <code>InvalidAttributes</code> - The provided <code>Attributes</code> of an entity is invalid.
     *       </li>
     *       <li>
     *         <code>InvalidTypeValue</code> - The provided <code>Type</code> in the <code>KeyAttributes</code>
     *         of an entity is invalid.
     *       </li>
     *       <li>
     *         <code>EntitySizeTooLarge</code> - The number of <code>EntityMetricData</code> objects allowed is
     *         2.
     *       </li>
     *       <li>
     *         <code>MissingRequiredFields</code> - There are missing required fields in the <code>KeyAttributes</code>
     *         for the provided <code>Type</code>.
     *       </li>
     *     </ul>For details of the requirements for specifying an entity, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/adding-your-own-related-telemetry.html">How to add related information to
     *     telemetry</a> in the <i>CloudWatch User Guide</i>.
     *   </li>
     * </ul>
     *
     * <p>This parameter is <i>required</i> when <code>EntityMetricData</code> is included.
     */
    public Boolean isStrictEntityValidation() {
        return strictEntityValidation;
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
        PutMetricDataInput that = (PutMetricDataInput) other;
        return Objects.equals(this.strictEntityValidation, that.strictEntityValidation)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricData, that.metricData)
               && Objects.equals(this.entityMetricData, that.entityMetricData);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricData);
        $hc = 31 * $hc + Objects.hashCode(entityMetricData);
        $hc = 31 * $hc + Objects.hashCode(strictEntityValidation);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (metricData != null) {
            serializer.writeList($SCHEMA_METRIC_DATA, metricData, metricData.size(), SharedSerde.MetricDataSerializer.INSTANCE);
        }
        if (entityMetricData != null) {
            serializer.writeList($SCHEMA_ENTITY_METRIC_DATA, entityMetricData, entityMetricData.size(), SharedSerde.EntityMetricDataListSerializer.INSTANCE);
        }
        if (strictEntityValidation != null) {
            serializer.writeBoolean($SCHEMA_STRICT_ENTITY_VALIDATION, strictEntityValidation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA, member, metricData);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENTITY_METRIC_DATA, member, entityMetricData);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_STRICT_ENTITY_VALIDATION, member, strictEntityValidation);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutMetricDataInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.namespace(this.namespace);
        builder.metricData(this.metricData);
        builder.entityMetricData(this.entityMetricData);
        builder.strictEntityValidation(this.strictEntityValidation);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutMetricDataInput}.
     */
    public static final class Builder implements ShapeBuilder<PutMetricDataInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String namespace;
        private List<MetricDatum> metricData;
        private List<EntityMetricData> entityMetricData;
        private Boolean strictEntityValidation;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAMESPACE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The namespace for the metric data. You can use ASCII characters for the namespace, except for control characters
         * which are not supported.
         *
         * <p>To avoid conflicts with Amazon Web Services service namespaces, you should not specify a namespace that begins
         * with <code>AWS/</code>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
            tracker.setMember($SCHEMA_NAMESPACE);
            return this;
        }

        /**
         * The data for the metrics. Use this parameter if your metrics do not contain associated entities. The array can
         * include no more than 1000 metrics per call.
         *
         * <p>The limit of metrics allowed, 1000, is the sum of both <code>EntityMetricData</code> and <code>MetricData</code>
         * metrics.
         *
         * @return this builder.
         */
        public Builder metricData(List<MetricDatum> metricData) {
            this.metricData = metricData;
            return this;
        }

        /**
         * Data for metrics that contain associated entity information. You can include up to two <code>EntityMetricData</code>
         * objects, each of which can contain a single <code>Entity</code> and associated metrics.
         *
         * <p>The limit of metrics allowed, 1000, is the sum of both <code>EntityMetricData</code> and <code>MetricData</code>
         * metrics.
         *
         * @return this builder.
         */
        public Builder entityMetricData(List<EntityMetricData> entityMetricData) {
            this.entityMetricData = entityMetricData;
            return this;
        }

        /**
         * Whether to accept valid metric data when an invalid entity is sent.
         *
         * <ul>
         *   <li>
         *     When set to <code>true</code>: Any validation error (for entity or metric data) will fail the entire
         *     request, and no data will be ingested. The failed operation will return a 400 result with the error.
         *   </li>
         *   <li>
         *     When set to <code>false</code>: Validation errors in the entity will not associate the metric with the
         *     entity, but the metric data will still be accepted and ingested. Validation errors in the metric data
         *     will fail the entire request, and no data will be ingested.In the case of an invalid entity, the
         *     operation will return a <code>200</code> status, but an additional response header will contain
         *     information about the validation errors. The new header, <code>X-Amzn-Failure-Message</code> is an
         *     enumeration of the following values:
         *
         *     <ul>
         *       <li>
         *         <code>InvalidEntity</code> - The provided entity is invalid.
         *       </li>
         *       <li>
         *         <code>InvalidKeyAttributes</code> - The provided <code>KeyAttributes</code> of an entity is
         *         invalid.
         *       </li>
         *       <li>
         *         <code>InvalidAttributes</code> - The provided <code>Attributes</code> of an entity is invalid.
         *       </li>
         *       <li>
         *         <code>InvalidTypeValue</code> - The provided <code>Type</code> in the <code>KeyAttributes</code>
         *         of an entity is invalid.
         *       </li>
         *       <li>
         *         <code>EntitySizeTooLarge</code> - The number of <code>EntityMetricData</code> objects allowed is
         *         2.
         *       </li>
         *       <li>
         *         <code>MissingRequiredFields</code> - There are missing required fields in the <code>KeyAttributes</code>
         *         for the provided <code>Type</code>.
         *       </li>
         *     </ul>For details of the requirements for specifying an entity, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/adding-your-own-related-telemetry.html">How to add related information to
         *     telemetry</a> in the <i>CloudWatch User Guide</i>.
         *   </li>
         * </ul>
         *
         * <p>This parameter is <i>required</i> when <code>EntityMetricData</code> is included.
         *
         * @return this builder.
         */
        public Builder strictEntityValidation(Boolean strictEntityValidation) {
            this.strictEntityValidation = strictEntityValidation;
            return this;
        }

        @Override
        public PutMetricDataInput build() {
            tracker.validate();
            return new PutMetricDataInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 1 -> metricData((List<MetricDatum>) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA, member, value));
                case 2 -> entityMetricData((List<EntityMetricData>) SchemaUtils.validateSameMember($SCHEMA_ENTITY_METRIC_DATA, member, value));
                case 3 -> strictEntityValidation((Boolean) SchemaUtils.validateSameMember($SCHEMA_STRICT_ENTITY_VALIDATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutMetricDataInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAMESPACE)) {
                namespace("");
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
                    case 0 -> builder.namespace(de.readString(member));
                    case 1 -> builder.metricData(SharedSerde.deserializeMetricData(member, de));
                    case 2 -> builder.entityMetricData(SharedSerde.deserializeEntityMetricDataList(member, de));
                    case 3 -> builder.strictEntityValidation(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
