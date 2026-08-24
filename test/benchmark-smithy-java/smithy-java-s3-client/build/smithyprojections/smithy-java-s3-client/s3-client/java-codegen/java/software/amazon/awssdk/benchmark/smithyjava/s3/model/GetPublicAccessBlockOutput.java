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
public final class GetPublicAccessBlockOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.GET_PUBLIC_ACCESS_BLOCK_OUTPUT;
    private static final Schema $SCHEMA_PUBLIC_ACCESS_BLOCK_CONFIGURATION = $SCHEMA.member("PublicAccessBlockConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient PublicAccessBlockConfiguration publicAccessBlockConfiguration;

    private GetPublicAccessBlockOutput(Builder builder) {
        this.publicAccessBlockConfiguration = builder.publicAccessBlockConfiguration;
    }

    /**
     * The <code>PublicAccessBlock</code> configuration currently in effect for this Amazon S3 bucket.
     */
    public PublicAccessBlockConfiguration getPublicAccessBlockConfiguration() {
        return publicAccessBlockConfiguration;
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
        GetPublicAccessBlockOutput that = (GetPublicAccessBlockOutput) other;
        return Objects.equals(this.publicAccessBlockConfiguration, that.publicAccessBlockConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(publicAccessBlockConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (publicAccessBlockConfiguration != null) {
            serializer.writeStruct($SCHEMA_PUBLIC_ACCESS_BLOCK_CONFIGURATION, publicAccessBlockConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PUBLIC_ACCESS_BLOCK_CONFIGURATION, member, publicAccessBlockConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetPublicAccessBlockOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.publicAccessBlockConfiguration(this.publicAccessBlockConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetPublicAccessBlockOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetPublicAccessBlockOutput> {
        private PublicAccessBlockConfiguration publicAccessBlockConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The <code>PublicAccessBlock</code> configuration currently in effect for this Amazon S3 bucket.
         *
         * @return this builder.
         */
        public Builder publicAccessBlockConfiguration(PublicAccessBlockConfiguration publicAccessBlockConfiguration) {
            this.publicAccessBlockConfiguration = publicAccessBlockConfiguration;
            return this;
        }

        @Override
        public GetPublicAccessBlockOutput build() {
            return new GetPublicAccessBlockOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> publicAccessBlockConfiguration((PublicAccessBlockConfiguration) SchemaUtils.validateSameMember($SCHEMA_PUBLIC_ACCESS_BLOCK_CONFIGURATION, member, value));
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
                    case 0 -> builder.publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
