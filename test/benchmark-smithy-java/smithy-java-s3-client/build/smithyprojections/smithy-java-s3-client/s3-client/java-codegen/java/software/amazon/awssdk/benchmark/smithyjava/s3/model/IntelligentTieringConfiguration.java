package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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

/**
 * Specifies the S3 Intelligent-Tiering configuration for an Amazon S3 bucket.
 *
 * <p>For information about the S3 Intelligent-Tiering storage class, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html#sc-dynamic-data-access">Storage class for automatically optimizing
 * frequently and infrequently accessed objects</a>.
 */
@SmithyGenerated
public final class IntelligentTieringConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INTELLIGENT_TIERING_CONFIGURATION;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_TIERINGS = $SCHEMA.member("Tierings");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient IntelligentTieringFilter filter;
    private final transient IntelligentTieringStatus status;
    private final transient List<Tiering> tierings;

    private IntelligentTieringConfiguration(Builder builder) {
        this.id = builder.id;
        this.filter = builder.filter;
        this.status = builder.status;
        this.tierings = Collections.unmodifiableList(builder.tierings);
    }

    /**
     * The ID used to identify the S3 Intelligent-Tiering configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * Specifies a bucket filter. The configuration only includes objects that meet the filter's criteria.
     */
    public IntelligentTieringFilter getFilter() {
        return filter;
    }

    /**
     * Specifies the status of the configuration.
     */
    public IntelligentTieringStatus getStatus() {
        return status;
    }

    /**
     * Specifies the S3 Intelligent-Tiering storage class tier of the configuration.
     */
    public List<Tiering> getTierings() {
        return tierings;
    }

    public boolean hasTierings() {
        return true;
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
        IntelligentTieringConfiguration that = (IntelligentTieringConfiguration) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.filter, that.filter)
               && Objects.equals(this.tierings, that.tierings);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(filter);
        $hc = 31 * $hc + Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(tierings);
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
        serializer.writeString($SCHEMA_STATUS, status.getValue());
        serializer.writeList($SCHEMA_TIERINGS, tierings, tierings.size(), SharedSerde.TieringListSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIERINGS, member, tierings);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link IntelligentTieringConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.filter(this.filter);
        builder.status(this.status);
        builder.tierings(this.tierings);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link IntelligentTieringConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<IntelligentTieringConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private IntelligentTieringFilter filter;
        private IntelligentTieringStatus status;
        private List<Tiering> tierings;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ID used to identify the S3 Intelligent-Tiering configuration.
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
         * Specifies a bucket filter. The configuration only includes objects that meet the filter's criteria.
         *
         * @return this builder.
         */
        public Builder filter(IntelligentTieringFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Specifies the status of the configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(IntelligentTieringStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        /**
         * Specifies the S3 Intelligent-Tiering storage class tier of the configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tierings(List<Tiering> tierings) {
            this.tierings = Objects.requireNonNull(tierings, "tierings cannot be null");
            tracker.setMember($SCHEMA_TIERINGS);
            return this;
        }

        @Override
        public IntelligentTieringConfiguration build() {
            tracker.validate();
            return new IntelligentTieringConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 1 -> status((IntelligentTieringStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 2 -> tierings((List<Tiering>) SchemaUtils.validateSameMember($SCHEMA_TIERINGS, member, value));
                case 3 -> filter((IntelligentTieringFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<IntelligentTieringConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ID)) {
                id("");
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(IntelligentTieringStatus.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_TIERINGS)) {
                tierings(Collections.emptyList());
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
                    case 1 -> builder.status(IntelligentTieringStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.tierings(SharedSerde.deserializeTieringList(member, de));
                    case 3 -> builder.filter(IntelligentTieringFilter.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
