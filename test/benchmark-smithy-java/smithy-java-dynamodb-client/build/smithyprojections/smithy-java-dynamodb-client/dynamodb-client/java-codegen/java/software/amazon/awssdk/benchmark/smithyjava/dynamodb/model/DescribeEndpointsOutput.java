package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class DescribeEndpointsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ENDPOINTS_OUTPUT;
    private static final Schema $SCHEMA_ENDPOINTS = $SCHEMA.member("Endpoints");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Endpoint> endpoints;

    private DescribeEndpointsOutput(Builder builder) {
        this.endpoints = Collections.unmodifiableList(builder.endpoints);
    }

    /**
     * List of endpoints.
     */
    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public boolean hasEndpoints() {
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
        DescribeEndpointsOutput that = (DescribeEndpointsOutput) other;
        return Objects.equals(this.endpoints, that.endpoints);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(endpoints);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_ENDPOINTS, endpoints, endpoints.size(), SharedSerde.EndpointsSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENDPOINTS, member, endpoints);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeEndpointsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.endpoints(this.endpoints);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeEndpointsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeEndpointsOutput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<Endpoint> endpoints;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * List of endpoints.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder endpoints(List<Endpoint> endpoints) {
            this.endpoints = Objects.requireNonNull(endpoints, "endpoints cannot be null");
            tracker.setMember($SCHEMA_ENDPOINTS);
            return this;
        }

        @Override
        public DescribeEndpointsOutput build() {
            tracker.validate();
            return new DescribeEndpointsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> endpoints((List<Endpoint>) SchemaUtils.validateSameMember($SCHEMA_ENDPOINTS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeEndpointsOutput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ENDPOINTS)) {
                endpoints(Collections.emptyList());
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
                    case 0 -> builder.endpoints(SharedSerde.deserializeEndpoints(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
