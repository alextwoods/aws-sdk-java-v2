package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
public final class RestoreTableToPointInTimeInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.RESTORE_TABLE_TO_POINT_IN_TIME_INPUT;
    private static final Schema $SCHEMA_SOURCE_TABLE_ARN = $SCHEMA.member("SourceTableArn");
    private static final Schema $SCHEMA_SOURCE_TABLE_NAME = $SCHEMA.member("SourceTableName");
    private static final Schema $SCHEMA_TARGET_TABLE_NAME = $SCHEMA.member("TargetTableName");
    private static final Schema $SCHEMA_USE_LATEST_RESTORABLE_TIME = $SCHEMA.member("UseLatestRestorableTime");
    private static final Schema $SCHEMA_RESTORE_DATE_TIME = $SCHEMA.member("RestoreDateTime");
    private static final Schema $SCHEMA_BILLING_MODE_OVERRIDE = $SCHEMA.member("BillingModeOverride");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEX_OVERRIDE = $SCHEMA.member("GlobalSecondaryIndexOverride");
    private static final Schema $SCHEMA_LOCAL_SECONDARY_INDEX_OVERRIDE = $SCHEMA.member("LocalSecondaryIndexOverride");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE = $SCHEMA.member("ProvisionedThroughputOverride");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE = $SCHEMA.member("OnDemandThroughputOverride");
    private static final Schema $SCHEMA_SSE_SPECIFICATION_OVERRIDE = $SCHEMA.member("SSESpecificationOverride");
    private static final Schema $SCHEMA_VECTOR_INDEX_OVERRIDE = $SCHEMA.member("VectorIndexOverride");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String sourceTableArn;
    private final transient String sourceTableName;
    private final transient String targetTableName;
    private final transient Boolean useLatestRestorableTime;
    private final transient Instant restoreDateTime;
    private final transient BillingMode billingModeOverride;
    private final transient List<GlobalSecondaryIndex> globalSecondaryIndexOverride;
    private final transient List<LocalSecondaryIndex> localSecondaryIndexOverride;
    private final transient ProvisionedThroughput provisionedThroughputOverride;
    private final transient OnDemandThroughput onDemandThroughputOverride;
    private final transient SSESpecification sseSpecificationOverride;
    private final transient List<VectorIndex> vectorIndexOverride;

    private RestoreTableToPointInTimeInput(Builder builder) {
        this.sourceTableArn = builder.sourceTableArn;
        this.sourceTableName = builder.sourceTableName;
        this.targetTableName = builder.targetTableName;
        this.useLatestRestorableTime = builder.useLatestRestorableTime;
        this.restoreDateTime = builder.restoreDateTime;
        this.billingModeOverride = builder.billingModeOverride;
        this.globalSecondaryIndexOverride = builder.globalSecondaryIndexOverride == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexOverride);
        this.localSecondaryIndexOverride = builder.localSecondaryIndexOverride == null ? null : Collections.unmodifiableList(builder.localSecondaryIndexOverride);
        this.provisionedThroughputOverride = builder.provisionedThroughputOverride;
        this.onDemandThroughputOverride = builder.onDemandThroughputOverride;
        this.sseSpecificationOverride = builder.sseSpecificationOverride;
        this.vectorIndexOverride = builder.vectorIndexOverride == null ? null : Collections.unmodifiableList(builder.vectorIndexOverride);
    }

    /**
     * The DynamoDB table that will be restored. This value is an Amazon Resource Name (ARN).
     */
    public String getSourceTableArn() {
        return sourceTableArn;
    }

    /**
     * Name of the source table that is being restored.
     */
    public String getSourceTableName() {
        return sourceTableName;
    }

    /**
     * The name of the new table to which it must be restored to.
     */
    public String getTargetTableName() {
        return targetTableName;
    }

    /**
     * Restore the table to the latest possible time. <code>LatestRestorableDateTime</code> is typically 5 minutes
     * before the current time.
     */
    public Boolean isUseLatestRestorableTime() {
        return useLatestRestorableTime;
    }

    /**
     * Time in the past to restore the table to.
     */
    public Instant getRestoreDateTime() {
        return restoreDateTime;
    }

    /**
     * The billing mode of the restored table.
     */
    public BillingMode getBillingModeOverride() {
        return billingModeOverride;
    }

    /**
     * List of global secondary indexes for the restored table. The indexes provided should match existing secondary
     * indexes. You can choose to exclude some or all of the indexes at the time of restore.
     *
     * <p>The <code>WarmThroughput</code> setting is not supported on global secondary indexes when you use <code>
     * RestoreTableToPointInTime</code>. Although <code>WarmThroughput</code> appears in the shared index definition,
     * including it in a <code>GlobalSecondaryIndexOverride</code> entry causes the request to fail with a validation
     * error.
     */
    public List<GlobalSecondaryIndex> getGlobalSecondaryIndexOverride() {
        if (globalSecondaryIndexOverride == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexOverride;
    }

    public boolean hasGlobalSecondaryIndexOverride() {
        return globalSecondaryIndexOverride != null;
    }

    /**
     * List of local secondary indexes for the restored table. The indexes provided should match existing secondary
     * indexes. You can choose to exclude some or all of the indexes at the time of restore.
     */
    public List<LocalSecondaryIndex> getLocalSecondaryIndexOverride() {
        if (localSecondaryIndexOverride == null) {
            return Collections.emptyList();
        }
        return localSecondaryIndexOverride;
    }

    public boolean hasLocalSecondaryIndexOverride() {
        return localSecondaryIndexOverride != null;
    }

    /**
     * Provisioned throughput settings for the restored table.
     */
    public ProvisionedThroughput getProvisionedThroughputOverride() {
        return provisionedThroughputOverride;
    }

    public OnDemandThroughput getOnDemandThroughputOverride() {
        return onDemandThroughputOverride;
    }

    /**
     * The new server-side encryption settings for the restored table.
     */
    public SSESpecification getSseSpecificationOverride() {
        return sseSpecificationOverride;
    }

    /**
     * The vector indexes for the restored table. If not specified, all vector indexes from the source table are
     * restored. The indexes provided must match existing vector indexes from the source table. You can choose to
     * exclude some or all of the vector indexes at the time of restore.
     */
    public List<VectorIndex> getVectorIndexOverride() {
        if (vectorIndexOverride == null) {
            return Collections.emptyList();
        }
        return vectorIndexOverride;
    }

    public boolean hasVectorIndexOverride() {
        return vectorIndexOverride != null;
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
        RestoreTableToPointInTimeInput that = (RestoreTableToPointInTimeInput) other;
        return Objects.equals(this.useLatestRestorableTime, that.useLatestRestorableTime)
               && Objects.equals(this.sourceTableArn, that.sourceTableArn)
               && Objects.equals(this.sourceTableName, that.sourceTableName)
               && Objects.equals(this.targetTableName, that.targetTableName)
               && Objects.equals(this.billingModeOverride, that.billingModeOverride)
               && Objects.equals(this.restoreDateTime, that.restoreDateTime)
               && Objects.equals(this.provisionedThroughputOverride, that.provisionedThroughputOverride)
               && Objects.equals(this.onDemandThroughputOverride, that.onDemandThroughputOverride)
               && Objects.equals(this.sseSpecificationOverride, that.sseSpecificationOverride)
               && Objects.equals(this.globalSecondaryIndexOverride, that.globalSecondaryIndexOverride)
               && Objects.equals(this.localSecondaryIndexOverride, that.localSecondaryIndexOverride)
               && Objects.equals(this.vectorIndexOverride, that.vectorIndexOverride);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sourceTableArn);
        $hc = 31 * $hc + Objects.hashCode(sourceTableName);
        $hc = 31 * $hc + Objects.hashCode(targetTableName);
        $hc = 31 * $hc + Objects.hashCode(useLatestRestorableTime);
        $hc = 31 * $hc + Objects.hashCode(restoreDateTime);
        $hc = 31 * $hc + Objects.hashCode(billingModeOverride);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexOverride);
        $hc = 31 * $hc + Objects.hashCode(localSecondaryIndexOverride);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(sseSpecificationOverride);
        $hc = 31 * $hc + Objects.hashCode(vectorIndexOverride);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sourceTableArn != null) {
            serializer.writeString($SCHEMA_SOURCE_TABLE_ARN, sourceTableArn);
        }
        if (sourceTableName != null) {
            serializer.writeString($SCHEMA_SOURCE_TABLE_NAME, sourceTableName);
        }
        serializer.writeString($SCHEMA_TARGET_TABLE_NAME, targetTableName);
        if (useLatestRestorableTime != null) {
            serializer.writeBoolean($SCHEMA_USE_LATEST_RESTORABLE_TIME, useLatestRestorableTime);
        }
        if (restoreDateTime != null) {
            serializer.writeTimestamp($SCHEMA_RESTORE_DATE_TIME, restoreDateTime);
        }
        if (billingModeOverride != null) {
            serializer.writeString($SCHEMA_BILLING_MODE_OVERRIDE, billingModeOverride.getValue());
        }
        if (globalSecondaryIndexOverride != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEX_OVERRIDE, globalSecondaryIndexOverride, globalSecondaryIndexOverride.size(), SharedSerde.GlobalSecondaryIndexListSerializer.INSTANCE);
        }
        if (localSecondaryIndexOverride != null) {
            serializer.writeList($SCHEMA_LOCAL_SECONDARY_INDEX_OVERRIDE, localSecondaryIndexOverride, localSecondaryIndexOverride.size(), SharedSerde.LocalSecondaryIndexListSerializer.INSTANCE);
        }
        if (provisionedThroughputOverride != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, provisionedThroughputOverride);
        }
        if (onDemandThroughputOverride != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, onDemandThroughputOverride);
        }
        if (sseSpecificationOverride != null) {
            serializer.writeStruct($SCHEMA_SSE_SPECIFICATION_OVERRIDE, sseSpecificationOverride);
        }
        if (vectorIndexOverride != null) {
            serializer.writeList($SCHEMA_VECTOR_INDEX_OVERRIDE, vectorIndexOverride, vectorIndexOverride.size(), SharedSerde.VectorIndexListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_TABLE_NAME, member, targetTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_ARN, member, sourceTableArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_NAME, member, sourceTableName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_USE_LATEST_RESTORABLE_TIME, member, useLatestRestorableTime);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_DATE_TIME, member, restoreDateTime);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE_OVERRIDE, member, billingModeOverride);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEX_OVERRIDE, member, globalSecondaryIndexOverride);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEX_OVERRIDE, member, localSecondaryIndexOverride);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, provisionedThroughputOverride);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, onDemandThroughputOverride);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION_OVERRIDE, member, sseSpecificationOverride);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEX_OVERRIDE, member, vectorIndexOverride);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RestoreTableToPointInTimeInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sourceTableArn(this.sourceTableArn);
        builder.sourceTableName(this.sourceTableName);
        builder.targetTableName(this.targetTableName);
        builder.useLatestRestorableTime(this.useLatestRestorableTime);
        builder.restoreDateTime(this.restoreDateTime);
        builder.billingModeOverride(this.billingModeOverride);
        builder.globalSecondaryIndexOverride(this.globalSecondaryIndexOverride);
        builder.localSecondaryIndexOverride(this.localSecondaryIndexOverride);
        builder.provisionedThroughputOverride(this.provisionedThroughputOverride);
        builder.onDemandThroughputOverride(this.onDemandThroughputOverride);
        builder.sseSpecificationOverride(this.sseSpecificationOverride);
        builder.vectorIndexOverride(this.vectorIndexOverride);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RestoreTableToPointInTimeInput}.
     */
    public static final class Builder implements ShapeBuilder<RestoreTableToPointInTimeInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String sourceTableArn;
        private String sourceTableName;
        private String targetTableName;
        private Boolean useLatestRestorableTime;
        private Instant restoreDateTime;
        private BillingMode billingModeOverride;
        private List<GlobalSecondaryIndex> globalSecondaryIndexOverride;
        private List<LocalSecondaryIndex> localSecondaryIndexOverride;
        private ProvisionedThroughput provisionedThroughputOverride;
        private OnDemandThroughput onDemandThroughputOverride;
        private SSESpecification sseSpecificationOverride;
        private List<VectorIndex> vectorIndexOverride;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The DynamoDB table that will be restored. This value is an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder sourceTableArn(String sourceTableArn) {
            this.sourceTableArn = sourceTableArn;
            return this;
        }

        /**
         * Name of the source table that is being restored.
         *
         * @return this builder.
         */
        public Builder sourceTableName(String sourceTableName) {
            this.sourceTableName = sourceTableName;
            return this;
        }

        /**
         * The name of the new table to which it must be restored to.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder targetTableName(String targetTableName) {
            this.targetTableName = Objects.requireNonNull(targetTableName, "targetTableName cannot be null");
            tracker.setMember($SCHEMA_TARGET_TABLE_NAME);
            return this;
        }

        /**
         * Restore the table to the latest possible time. <code>LatestRestorableDateTime</code> is typically 5 minutes
         * before the current time.
         *
         * @return this builder.
         */
        public Builder useLatestRestorableTime(Boolean useLatestRestorableTime) {
            this.useLatestRestorableTime = useLatestRestorableTime;
            return this;
        }

        /**
         * Time in the past to restore the table to.
         *
         * @return this builder.
         */
        public Builder restoreDateTime(Instant restoreDateTime) {
            this.restoreDateTime = restoreDateTime;
            return this;
        }

        /**
         * The billing mode of the restored table.
         *
         * @return this builder.
         */
        public Builder billingModeOverride(BillingMode billingModeOverride) {
            this.billingModeOverride = billingModeOverride;
            return this;
        }

        /**
         * List of global secondary indexes for the restored table. The indexes provided should match existing secondary
         * indexes. You can choose to exclude some or all of the indexes at the time of restore.
         *
         * <p>The <code>WarmThroughput</code> setting is not supported on global secondary indexes when you use <code>
         * RestoreTableToPointInTime</code>. Although <code>WarmThroughput</code> appears in the shared index definition,
         * including it in a <code>GlobalSecondaryIndexOverride</code> entry causes the request to fail with a validation
         * error.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexOverride(List<GlobalSecondaryIndex> globalSecondaryIndexOverride) {
            this.globalSecondaryIndexOverride = globalSecondaryIndexOverride;
            return this;
        }

        /**
         * List of local secondary indexes for the restored table. The indexes provided should match existing secondary
         * indexes. You can choose to exclude some or all of the indexes at the time of restore.
         *
         * @return this builder.
         */
        public Builder localSecondaryIndexOverride(List<LocalSecondaryIndex> localSecondaryIndexOverride) {
            this.localSecondaryIndexOverride = localSecondaryIndexOverride;
            return this;
        }

        /**
         * Provisioned throughput settings for the restored table.
         *
         * @return this builder.
         */
        public Builder provisionedThroughputOverride(ProvisionedThroughput provisionedThroughputOverride) {
            this.provisionedThroughputOverride = provisionedThroughputOverride;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder onDemandThroughputOverride(OnDemandThroughput onDemandThroughputOverride) {
            this.onDemandThroughputOverride = onDemandThroughputOverride;
            return this;
        }

        /**
         * The new server-side encryption settings for the restored table.
         *
         * @return this builder.
         */
        public Builder sseSpecificationOverride(SSESpecification sseSpecificationOverride) {
            this.sseSpecificationOverride = sseSpecificationOverride;
            return this;
        }

        /**
         * The vector indexes for the restored table. If not specified, all vector indexes from the source table are
         * restored. The indexes provided must match existing vector indexes from the source table. You can choose to
         * exclude some or all of the vector indexes at the time of restore.
         *
         * @return this builder.
         */
        public Builder vectorIndexOverride(List<VectorIndex> vectorIndexOverride) {
            this.vectorIndexOverride = vectorIndexOverride;
            return this;
        }

        @Override
        public RestoreTableToPointInTimeInput build() {
            tracker.validate();
            return new RestoreTableToPointInTimeInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> targetTableName((String) SchemaUtils.validateSameMember($SCHEMA_TARGET_TABLE_NAME, member, value));
                case 1 -> sourceTableArn((String) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_ARN, member, value));
                case 2 -> sourceTableName((String) SchemaUtils.validateSameMember($SCHEMA_SOURCE_TABLE_NAME, member, value));
                case 3 -> useLatestRestorableTime((Boolean) SchemaUtils.validateSameMember($SCHEMA_USE_LATEST_RESTORABLE_TIME, member, value));
                case 4 -> restoreDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_RESTORE_DATE_TIME, member, value));
                case 5 -> billingModeOverride((BillingMode) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE_OVERRIDE, member, value));
                case 6 -> globalSecondaryIndexOverride((List<GlobalSecondaryIndex>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEX_OVERRIDE, member, value));
                case 7 -> localSecondaryIndexOverride((List<LocalSecondaryIndex>) SchemaUtils.validateSameMember($SCHEMA_LOCAL_SECONDARY_INDEX_OVERRIDE, member, value));
                case 8 -> provisionedThroughputOverride((ProvisionedThroughput) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, value));
                case 9 -> onDemandThroughputOverride((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, value));
                case 10 -> sseSpecificationOverride((SSESpecification) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION_OVERRIDE, member, value));
                case 11 -> vectorIndexOverride((List<VectorIndex>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEX_OVERRIDE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RestoreTableToPointInTimeInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TARGET_TABLE_NAME)) {
                targetTableName("");
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
                    case 0 -> builder.targetTableName(de.readString(member));
                    case 1 -> builder.sourceTableArn(de.readString(member));
                    case 2 -> builder.sourceTableName(de.readString(member));
                    case 3 -> builder.useLatestRestorableTime(de.readBoolean(member));
                    case 4 -> builder.restoreDateTime(de.readTimestamp(member));
                    case 5 -> builder.billingModeOverride(BillingMode.builder().deserializeMember(de, member).build());
                    case 6 -> builder.globalSecondaryIndexOverride(SharedSerde.deserializeGlobalSecondaryIndexList(member, de));
                    case 7 -> builder.localSecondaryIndexOverride(SharedSerde.deserializeLocalSecondaryIndexList(member, de));
                    case 8 -> builder.provisionedThroughputOverride(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 9 -> builder.onDemandThroughputOverride(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 10 -> builder.sseSpecificationOverride(SSESpecification.builder().deserializeMember(de, member).build());
                    case 11 -> builder.vectorIndexOverride(SharedSerde.deserializeVectorIndexList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
