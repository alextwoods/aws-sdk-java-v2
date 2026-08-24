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
 * Specifies the configuration and any analyses for the analytics filter of an Amazon S3 bucket.
 */
@SmithyGenerated
public final class AnalyticsConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.ANALYTICS_CONFIGURATION;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");
    private static final Schema $SCHEMA_STORAGE_CLASS_ANALYSIS = $SCHEMA.member("StorageClassAnalysis");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient AnalyticsFilter filter;
    private final transient StorageClassAnalysis storageClassAnalysis;

    private AnalyticsConfiguration(Builder builder) {
        this.id = builder.id;
        this.filter = builder.filter;
        this.storageClassAnalysis = builder.storageClassAnalysis;
    }

    /**
     * The ID that identifies the analytics configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * The filter used to describe a set of objects for analyses. A filter must have exactly one prefix, one tag, or one
     * conjunction (AnalyticsAndOperator). If no filter is provided, all objects will be considered in any analysis.
     */
    public AnalyticsFilter getFilter() {
        return filter;
    }

    /**
     * Contains data related to access patterns to be collected and made available to analyze the tradeoffs between
     * different storage classes.
     */
    public StorageClassAnalysis getStorageClassAnalysis() {
        return storageClassAnalysis;
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
        AnalyticsConfiguration that = (AnalyticsConfiguration) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.filter, that.filter)
               && Objects.equals(this.storageClassAnalysis, that.storageClassAnalysis);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(filter);
        $hc = 31 * $hc + Objects.hashCode(storageClassAnalysis);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ID, id);
        if (filter != null) {
            serializer.writeStruct($SCHEMA_FILTER, filter);
        }
        if (storageClassAnalysis != null) {
            serializer.writeStruct($SCHEMA_STORAGE_CLASS_ANALYSIS, storageClassAnalysis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS_ANALYSIS, member, storageClassAnalysis);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnalyticsConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.filter(this.filter);
        builder.storageClassAnalysis(this.storageClassAnalysis);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AnalyticsConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<AnalyticsConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private AnalyticsFilter filter;
        private StorageClassAnalysis storageClassAnalysis;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ID that identifies the analytics configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            tracker.setMember($SCHEMA_ID);
            return this;
        }

        /**
         * The filter used to describe a set of objects for analyses. A filter must have exactly one prefix, one tag, or one
         * conjunction (AnalyticsAndOperator). If no filter is provided, all objects will be considered in any analysis.
         *
         * @return this builder.
         */
        public Builder filter(AnalyticsFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Contains data related to access patterns to be collected and made available to analyze the tradeoffs between
         * different storage classes.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder storageClassAnalysis(StorageClassAnalysis storageClassAnalysis) {
            this.storageClassAnalysis = Objects.requireNonNull(storageClassAnalysis, "storageClassAnalysis cannot be null");
            tracker.setMember($SCHEMA_STORAGE_CLASS_ANALYSIS);
            return this;
        }

        @Override
        public AnalyticsConfiguration build() {
            tracker.validate();
            return new AnalyticsConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 1 -> storageClassAnalysis((StorageClassAnalysis) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS_ANALYSIS, member, value));
                case 2 -> filter((AnalyticsFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AnalyticsConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ID)) {
                id("");
            }
            if (!tracker.checkMember($SCHEMA_STORAGE_CLASS_ANALYSIS)) {
                tracker.setMember($SCHEMA_STORAGE_CLASS_ANALYSIS);
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
                    case 0 -> builder.id(de.readString(member));
                    case 1 -> builder.storageClassAnalysis(StorageClassAnalysis.builder().deserializeMember(de, member).build());
                    case 2 -> builder.filter(AnalyticsFilter.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
