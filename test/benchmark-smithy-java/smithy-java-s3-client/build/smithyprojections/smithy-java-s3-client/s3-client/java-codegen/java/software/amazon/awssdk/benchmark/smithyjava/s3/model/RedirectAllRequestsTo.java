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
 * Specifies the redirect behavior of all requests to a website endpoint of an Amazon S3 bucket.
 */
@SmithyGenerated
public final class RedirectAllRequestsTo implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REDIRECT_ALL_REQUESTS_TO;
    private static final Schema $SCHEMA_HOST_NAME = $SCHEMA.member("HostName");
    private static final Schema $SCHEMA_PROTOCOL = $SCHEMA.member("Protocol");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String hostName;
    private final transient Protocol protocol;

    private RedirectAllRequestsTo(Builder builder) {
        this.hostName = builder.hostName;
        this.protocol = builder.protocol;
    }

    /**
     * Name of the host where requests are redirected.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Protocol to use when redirecting requests. The default is the protocol that is used in the original request.
     */
    public Protocol getProtocol() {
        return protocol;
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
        RedirectAllRequestsTo that = (RedirectAllRequestsTo) other;
        return Objects.equals(this.hostName, that.hostName)
               && Objects.equals(this.protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(hostName);
        $hc = 31 * $hc + Objects.hashCode(protocol);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_HOST_NAME, hostName);
        if (protocol != null) {
            serializer.writeString($SCHEMA_PROTOCOL, protocol.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_HOST_NAME, member, hostName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROTOCOL, member, protocol);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RedirectAllRequestsTo}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.hostName(this.hostName);
        builder.protocol(this.protocol);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RedirectAllRequestsTo}.
     */
    public static final class Builder implements ShapeBuilder<RedirectAllRequestsTo> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String hostName;
        private Protocol protocol;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Name of the host where requests are redirected.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder hostName(String hostName) {
            this.hostName = Objects.requireNonNull(hostName, "hostName cannot be null");
            tracker.setMember($SCHEMA_HOST_NAME);
            return this;
        }

        /**
         * Protocol to use when redirecting requests. The default is the protocol that is used in the original request.
         *
         * @return this builder.
         */
        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public RedirectAllRequestsTo build() {
            tracker.validate();
            return new RedirectAllRequestsTo(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> hostName((String) SchemaUtils.validateSameMember($SCHEMA_HOST_NAME, member, value));
                case 1 -> protocol((Protocol) SchemaUtils.validateSameMember($SCHEMA_PROTOCOL, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<RedirectAllRequestsTo> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_HOST_NAME)) {
                hostName("");
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
                    case 0 -> builder.hostName(de.readString(member));
                    case 1 -> builder.protocol(Protocol.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
