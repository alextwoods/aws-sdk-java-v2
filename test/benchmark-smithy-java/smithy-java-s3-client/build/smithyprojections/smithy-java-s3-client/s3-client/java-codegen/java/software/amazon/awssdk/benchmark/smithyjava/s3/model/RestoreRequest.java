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
 * Container for restore job parameters.
 */
@SmithyGenerated
public final class RestoreRequest implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.RESTORE_REQUEST;
    private static final Schema $SCHEMA_DAYS = $SCHEMA.member("Days");
    private static final Schema $SCHEMA_GLACIER_JOB_PARAMETERS = $SCHEMA.member("GlacierJobParameters");
    private static final Schema $SCHEMA_TYPE = $SCHEMA.member("Type");
    private static final Schema $SCHEMA_TIER = $SCHEMA.member("Tier");
    private static final Schema $SCHEMA_DESCRIPTION = $SCHEMA.member("Description");
    private static final Schema $SCHEMA_SELECT_PARAMETERS = $SCHEMA.member("SelectParameters");
    private static final Schema $SCHEMA_OUTPUT_LOCATION = $SCHEMA.member("OutputLocation");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer days;
    private final transient GlacierJobParameters glacierJobParameters;
    private final transient RestoreRequestType type;
    private final transient Tier tier;
    private final transient String description;
    private final transient SelectParameters selectParameters;
    private final transient OutputLocation outputLocation;

    private RestoreRequest(Builder builder) {
        this.days = builder.days;
        this.glacierJobParameters = builder.glacierJobParameters;
        this.type = builder.type;
        this.tier = builder.tier;
        this.description = builder.description;
        this.selectParameters = builder.selectParameters;
        this.outputLocation = builder.outputLocation;
    }

    /**
     * Lifetime of the active copy in days. Do not use with restores that specify <code>OutputLocation</code>.
     *
     * <p>The Days element is required for regular restores, and must not be provided for select requests.
     */
    public Integer getDays() {
        return days;
    }

    /**
     * S3 Glacier related parameters pertaining to this job. Do not use with restores that specify
     * <code>OutputLocation</code>.
     */
    public GlacierJobParameters getGlacierJobParameters() {
        return glacierJobParameters;
    }

    /**
     * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to
     * use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
     *
     * <p>Type of restore request.
     */
    public RestoreRequestType getType() {
        return type;
    }

    /**
     * Retrieval tier at which the restore will be processed.
     */
    public Tier getTier() {
        return tier;
    }

    /**
     * The optional description for the job.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to
     * use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
     *
     * <p>Describes the parameters for Select job types.
     */
    public SelectParameters getSelectParameters() {
        return selectParameters;
    }

    /**
     * Describes the location where the restore job's output is stored.
     */
    public OutputLocation getOutputLocation() {
        return outputLocation;
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
        RestoreRequest that = (RestoreRequest) other;
        return Objects.equals(this.days, that.days)
               && Objects.equals(this.description, that.description)
               && Objects.equals(this.type, that.type)
               && Objects.equals(this.tier, that.tier)
               && Objects.equals(this.glacierJobParameters, that.glacierJobParameters)
               && Objects.equals(this.selectParameters, that.selectParameters)
               && Objects.equals(this.outputLocation, that.outputLocation);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(days);
        $hc = 31 * $hc + Objects.hashCode(glacierJobParameters);
        $hc = 31 * $hc + Objects.hashCode(type);
        $hc = 31 * $hc + Objects.hashCode(tier);
        $hc = 31 * $hc + Objects.hashCode(description);
        $hc = 31 * $hc + Objects.hashCode(selectParameters);
        $hc = 31 * $hc + Objects.hashCode(outputLocation);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (days != null) {
            serializer.writeInteger($SCHEMA_DAYS, days);
        }
        if (glacierJobParameters != null) {
            serializer.writeStruct($SCHEMA_GLACIER_JOB_PARAMETERS, glacierJobParameters);
        }
        if (type != null) {
            serializer.writeString($SCHEMA_TYPE, type.getValue());
        }
        if (tier != null) {
            serializer.writeString($SCHEMA_TIER, tier.getValue());
        }
        if (description != null) {
            serializer.writeString($SCHEMA_DESCRIPTION, description);
        }
        if (selectParameters != null) {
            serializer.writeStruct($SCHEMA_SELECT_PARAMETERS, selectParameters);
        }
        if (outputLocation != null) {
            serializer.writeStruct($SCHEMA_OUTPUT_LOCATION, outputLocation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, days);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLACIER_JOB_PARAMETERS, member, glacierJobParameters);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, type);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIER, member, tier);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESCRIPTION, member, description);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SELECT_PARAMETERS, member, selectParameters);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_LOCATION, member, outputLocation);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RestoreRequest}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.days(this.days);
        builder.glacierJobParameters(this.glacierJobParameters);
        builder.type(this.type);
        builder.tier(this.tier);
        builder.description(this.description);
        builder.selectParameters(this.selectParameters);
        builder.outputLocation(this.outputLocation);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RestoreRequest}.
     */
    public static final class Builder implements ShapeBuilder<RestoreRequest> {
        private Integer days;
        private GlacierJobParameters glacierJobParameters;
        private RestoreRequestType type;
        private Tier tier;
        private String description;
        private SelectParameters selectParameters;
        private OutputLocation outputLocation;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Lifetime of the active copy in days. Do not use with restores that specify <code>OutputLocation</code>.
         *
         * <p>The Days element is required for regular restores, and must not be provided for select requests.
         *
         * @return this builder.
         */
        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        /**
         * S3 Glacier related parameters pertaining to this job. Do not use with restores that specify
         * <code>OutputLocation</code>.
         *
         * @return this builder.
         */
        public Builder glacierJobParameters(GlacierJobParameters glacierJobParameters) {
            this.glacierJobParameters = glacierJobParameters;
            return this;
        }

        /**
         * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to
         * use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
         *
         * <p>Type of restore request.
         *
         * @return this builder.
         */
        public Builder type(RestoreRequestType type) {
            this.type = type;
            return this;
        }

        /**
         * Retrieval tier at which the restore will be processed.
         *
         * @return this builder.
         */
        public Builder tier(Tier tier) {
            this.tier = tier;
            return this;
        }

        /**
         * The optional description for the job.
         *
         * @return this builder.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Amazon S3 Select is no longer available to new customers. Existing customers of Amazon S3 Select can continue to
         * use the feature as usual. <a href="http://aws.amazon.com/blogs/storage/how-to-optimize-querying-your-data-in-amazon-s3/">Learn more</a>
         *
         * <p>Describes the parameters for Select job types.
         *
         * @return this builder.
         */
        public Builder selectParameters(SelectParameters selectParameters) {
            this.selectParameters = selectParameters;
            return this;
        }

        /**
         * Describes the location where the restore job's output is stored.
         *
         * @return this builder.
         */
        public Builder outputLocation(OutputLocation outputLocation) {
            this.outputLocation = outputLocation;
            return this;
        }

        @Override
        public RestoreRequest build() {
            return new RestoreRequest(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> days((Integer) SchemaUtils.validateSameMember($SCHEMA_DAYS, member, value));
                case 1 -> glacierJobParameters((GlacierJobParameters) SchemaUtils.validateSameMember($SCHEMA_GLACIER_JOB_PARAMETERS, member, value));
                case 2 -> type((RestoreRequestType) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, value));
                case 3 -> tier((Tier) SchemaUtils.validateSameMember($SCHEMA_TIER, member, value));
                case 4 -> description((String) SchemaUtils.validateSameMember($SCHEMA_DESCRIPTION, member, value));
                case 5 -> selectParameters((SelectParameters) SchemaUtils.validateSameMember($SCHEMA_SELECT_PARAMETERS, member, value));
                case 6 -> outputLocation((OutputLocation) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_LOCATION, member, value));
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
                    case 0 -> builder.days(de.readInteger(member));
                    case 1 -> builder.glacierJobParameters(GlacierJobParameters.builder().deserializeMember(de, member).build());
                    case 2 -> builder.type(RestoreRequestType.builder().deserializeMember(de, member).build());
                    case 3 -> builder.tier(Tier.builder().deserializeMember(de, member).build());
                    case 4 -> builder.description(de.readString(member));
                    case 5 -> builder.selectParameters(SelectParameters.builder().deserializeMember(de, member).build());
                    case 6 -> builder.outputLocation(OutputLocation.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
