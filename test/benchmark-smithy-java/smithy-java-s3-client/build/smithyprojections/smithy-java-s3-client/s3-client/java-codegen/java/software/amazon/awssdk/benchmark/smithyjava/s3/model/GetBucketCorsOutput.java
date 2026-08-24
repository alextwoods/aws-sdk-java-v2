package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public final class GetBucketCorsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_CORS_OUTPUT;
    private static final Schema $SCHEMA_CORS_RULES = $SCHEMA.member("CORSRules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<CORSRule> corsRules;

    private GetBucketCorsOutput(Builder builder) {
        this.corsRules = builder.corsRules == null ? null : Collections.unmodifiableList(builder.corsRules);
    }

    /**
     * A set of origins and methods (cross-origin access that you want to allow). You can add up to 100 rules to the
     * configuration.
     */
    public List<CORSRule> getCorsRules() {
        if (corsRules == null) {
            return Collections.emptyList();
        }
        return corsRules;
    }

    public boolean hasCorsRules() {
        return corsRules != null;
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
        GetBucketCorsOutput that = (GetBucketCorsOutput) other;
        return Objects.equals(this.corsRules, that.corsRules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(corsRules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (corsRules != null) {
            serializer.writeList($SCHEMA_CORS_RULES, corsRules, corsRules.size(), SharedSerde.CORSRulesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CORS_RULES, member, corsRules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketCorsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.corsRules(this.corsRules);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketCorsOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketCorsOutput> {
        private List<CORSRule> corsRules;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A set of origins and methods (cross-origin access that you want to allow). You can add up to 100 rules to the
         * configuration.
         *
         * @return this builder.
         */
        public Builder corsRules(List<CORSRule> corsRules) {
            this.corsRules = corsRules;
            return this;
        }

        @Override
        public GetBucketCorsOutput build() {
            return new GetBucketCorsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> corsRules((List<CORSRule>) SchemaUtils.validateSameMember($SCHEMA_CORS_RULES, member, value));
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
                    case 0 -> builder.corsRules(SharedSerde.deserializeCORSRules(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
