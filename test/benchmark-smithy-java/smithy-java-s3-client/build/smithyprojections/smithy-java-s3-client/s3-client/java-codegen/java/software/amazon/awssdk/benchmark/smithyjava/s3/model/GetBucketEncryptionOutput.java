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

@SmithyGenerated
public final class GetBucketEncryptionOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_ENCRYPTION_OUTPUT;
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION_CONFIGURATION = $SCHEMA.member("ServerSideEncryptionConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ServerSideEncryptionConfiguration serverSideEncryptionConfiguration;

    private GetBucketEncryptionOutput(Builder builder) {
        this.serverSideEncryptionConfiguration = builder.serverSideEncryptionConfiguration;
    }

    public ServerSideEncryptionConfiguration getServerSideEncryptionConfiguration() {
        return serverSideEncryptionConfiguration;
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
        GetBucketEncryptionOutput that = (GetBucketEncryptionOutput) other;
        return Objects.equals(this.serverSideEncryptionConfiguration, that.serverSideEncryptionConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(serverSideEncryptionConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (serverSideEncryptionConfiguration != null) {
            serializer.writeStruct($SCHEMA_SERVER_SIDE_ENCRYPTION_CONFIGURATION, serverSideEncryptionConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION_CONFIGURATION, member, serverSideEncryptionConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketEncryptionOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.serverSideEncryptionConfiguration(this.serverSideEncryptionConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketEncryptionOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketEncryptionOutput> {
        private ServerSideEncryptionConfiguration serverSideEncryptionConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration serverSideEncryptionConfiguration) {
            this.serverSideEncryptionConfiguration = serverSideEncryptionConfiguration;
            return this;
        }

        @Override
        public GetBucketEncryptionOutput build() {
            return new GetBucketEncryptionOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> serverSideEncryptionConfiguration((ServerSideEncryptionConfiguration) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION_CONFIGURATION, member, value));
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
                    case 0 -> builder.serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
